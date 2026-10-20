#!/usr/bin/env node
/**
 * The only route to `adb` from a factory process.
 *
 * Why this module exists at all: `adb` resolves its state directory from
 * `ANDROID_USER_HOME`, falling back to `HOME`/`USERPROFILE`. A sandboxed worker (Codex, Copilot) has no
 * writable home, so adb tries `\.android` at the drive root and dies with
 * `adb_utils.cpp:315 Cannot mkdir '\.android': Permission denied` — before it ever talks to the phone.
 * That looked like "the device is unavailable" twice in one session (W23, W25 on 2026-10-18) when the
 * device was attached and healthy the whole time.
 *
 * The fix is the same shape as `scripts/lib/scratch.mjs`: the location is decided here, inside the
 * repository, and every caller inherits it instead of re-deriving it. The environment is passed
 * explicitly to the child, so the caller's shell — PowerShell, bash, cmd — cannot change the answer.
 *
 * Usage:
 *   node scripts/adb.mjs -s <serial> shell getprop ro.product.model
 *   node scripts/adb.mjs --out .kiln/scratch/x/shot.png -s <serial> exec-out screencap -p
 *
 * `--out <path>` writes the child's stdout to a file as BYTES. PowerShell's `>` is UTF-16 and mangles
 * a PNG; a screenshot must never be redirected through the shell.
 *
 * ## Why there are two strategies, measured on 2026-10-18
 *
 * A Codex worker received the environment correctly — `node -e 'console.log(process.env.HOME)'`
 * printed the repository path — and yet `adb version` still died with
 * `Cannot mkdir '\.android': Permission denied` and exit `3221226505` (`0xC0000022`,
 * STATUS_ACCESS_DENIED). The environment block handed to `spawn` did not survive into the adb process
 * in that sandbox, so adb built an empty home and tried the drive root.
 *
 * `shellset` therefore sets the variables **inside the shell that execs adb**, so they are inherited
 * by the process adb actually becomes, whatever stripped the outer block. It is tried when the direct
 * strategy fails with that specific signature, and `--strategy` forces either one for a diagnostic.
 */
import { spawn } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const REPO_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const STATE_ROOT = path.join(REPO_ROOT, '.kiln', 'scratch', 'android-home');

/**
 * The child environment. `ANDROID_USER_HOME` is the one adb actually reads; `HOME` and `USERPROFILE`
 * are its documented fallbacks and are set too, so an older or wrapped adb cannot pick another path.
 */
export function adbEnv(base = process.env) {
  const androidDir = path.join(STATE_ROOT, 'android');
  const homeDir = path.join(STATE_ROOT, 'home');
  fs.mkdirSync(androidDir, { recursive: true });
  fs.mkdirSync(homeDir, { recursive: true });
  return {
    ...base,
    ANDROID_USER_HOME: androidDir,
    HOME: homeDir,
    USERPROFILE: homeDir,
    // Keep adb's own output readable in a worker's log rather than wrapped at 80 columns.
    ADB_TRACE: base.ADB_TRACE ?? '',
  };
}

/** Where `adb` is: `PLAYKILN_ADB`, `ANDROID_HOME`, `ANDROID_SDK_ROOT`, then a bare `adb` on PATH. */
export function findAdb(env = process.env) {
  if (env.PLAYKILN_ADB && fs.existsSync(env.PLAYKILN_ADB)) return env.PLAYKILN_ADB;
  const exe = process.platform === 'win32' ? 'adb.exe' : 'adb';
  for (const root of [env.ANDROID_HOME, env.ANDROID_SDK_ROOT]) {
    if (!root) continue;
    const candidate = path.join(root, 'platform-tools', exe);
    if (fs.existsSync(candidate)) return candidate;
  }
  return exe;
}

function splitOut(argv) {
  const rest = [];
  let out = null;
  let strategy = null;
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    if (arg === '--out') {
      out = argv[i + 1];
      i += 1;
      continue;
    }
    if (arg.startsWith('--out=')) {
      out = arg.slice('--out='.length);
      continue;
    }
    if (arg === '--strategy') {
      strategy = argv[i + 1];
      i += 1;
      continue;
    }
    if (arg.startsWith('--strategy=')) {
      strategy = arg.slice('--strategy='.length);
      continue;
    }
    rest.push(arg);
  }
  return { out, rest, strategy };
}

/** The measured signature of a sandbox that dropped the environment block before adb ran. */
const HOME_LOST = /Cannot mkdir '\\\.android'|3221226505|0xC0000022/i;

function cmdQuote(arg) {
  if (arg === '') return '""';
  if (!/[\s"&|<>^()%!]/.test(arg)) return arg;
  return `"${arg.replace(/"/g, '\\"')}"`;
}

function shellSetLine(adbPath, rest, env) {
  const scoped = adbEnv(env);
  const sets = ['HOME', 'USERPROFILE', 'ANDROID_USER_HOME']
    .map((name) => `set "${name}=${scoped[name]}"`)
    .join(' && ');
  return `${sets} && ${cmdQuote(adbPath)} ${rest.map(cmdQuote).join(' ')}`;
}

/**
 * One attempt with one strategy. `direct` spawns adb with an environment block; `shellset` lets
 * `cmd.exe` set the variables and then exec adb, so a sandbox that strips the block cannot lose them.
 */
function runOnce(strategy, rest, out, env) {
  const adbPath = findAdb(env);
  const command =
    strategy === 'shellset' && process.platform === 'win32'
      ? 'cmd.exe'
      : adbPath;
  const args =
    strategy === 'shellset' && process.platform === 'win32'
      ? ['/d', '/s', '/c', shellSetLine(adbPath, rest, env)]
      : rest;

  return new Promise((resolve) => {
    const child = spawn(command, args, {
      env: adbEnv(env),
      stdio: ['ignore', out ? 'pipe' : 'inherit', 'pipe'],
      windowsHide: true,
    });
    let bytes = 0;
    let stderrTail = '';
    let sink = null;
    if (out) {
      const target = path.resolve(REPO_ROOT, out);
      fs.mkdirSync(path.dirname(target), { recursive: true });
      sink = fs.createWriteStream(target);
      child.stdout.pipe(sink);
      sink.on('error', (error) => {
        process.stderr.write(`adb: cannot write ${target}: ${error.message}\n`);
      });
    }
    child.stdout?.on('data', (chunk) => {
      bytes += chunk.length;
    });
    child.stderr.on('data', (chunk) => {
      process.stderr.write(chunk);
      stderrTail = (stderrTail + chunk.toString('utf8')).slice(-4096);
    });
    const settle = (code) => {
      const result = {
        code: code ?? 0,
        out: out ? path.resolve(REPO_ROOT, out) : null,
        bytes,
        strategy,
        homeLost: HOME_LOST.test(stderrTail),
      };
      if (!sink) return resolve(result);
      sink.end(() => resolve(result));
    };
    child.on('error', (error) => {
      stderrTail += `cannot start ${command}: ${error.message}\n`;
      process.stderr.write(`adb: cannot start ${command}: ${error.message}\n`);
      settle(127);
    });
    child.on('close', (code) => settle(code));
  });
}

/**
 * Auto: try `direct`, and only when it fails with the sandbox signature retry as `shellset`. A real
 * adb error (no device, bad serial) is returned as-is, never retried — the retry is not a workaround
 * for device problems, and a silent second attempt would hide one.
 */
export async function runAdb(argv, options = {}) {
  const env = options.env ?? process.env;
  const { out, rest, strategy } = splitOut(argv);
  const chosen = options.strategy ?? strategy ?? 'auto';
  if (chosen !== 'auto') return runOnce(chosen, rest, out, env);

  const first = await runOnce('direct', rest, out, env);
  if (first.code === 0 || !first.homeLost) return first;
  process.stderr.write('adb: the sandbox lost the environment block; retrying via cmd.exe\n');
  return runOnce('shellset', rest, out, env);
}

function targetOf(out) {
  return out ? path.resolve(REPO_ROOT, out) : null;
}

const isMain = process.argv[1] && path.resolve(process.argv[1]) === path.resolve(fileURLToPath(import.meta.url));
if (isMain) {
  const result = await runAdb(process.argv.slice(2));
  if (result.out) {
    process.stderr.write(`adb: wrote ${result.bytes} bytes to ${result.out} (${result.strategy})\n`);
  }
  process.exit(result.code);
}
