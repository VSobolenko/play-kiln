#!/usr/bin/env node
/**
 * PlayKiln Codex worker runner.
 *
 * Why this exists
 * ---------------
 * The Codex MCP server (`codex` / `codex-reply`) is excellent for short
 * back-and-forth, but MCP requests have a hard timeout: a real implementation
 * task runs for many minutes and the MCP call dies mid-flight, losing the work.
 *
 * This runner shells out to the Codex CLI (`codex exec`) that ships inside the
 * ChatGPT/Codex editor extension, keeps durable logs and the final message on
 * disk, and records an exit record. It runs in the foreground on purpose: the
 * orchestrator's shell tool is what backgrounds long jobs, so several workers can
 * run in parallel without any process-detaching tricks.
 *
 * Usage
 * -----
 *   node scripts/codex-run.mjs run    --id scaffold-build --prompt-file .kiln/codex/prompts/a.md
 *   node scripts/codex-run.mjs status [--id <id>]
 *   node scripts/codex-run.mjs last   --id <id> [--chars 4000]
 *   node scripts/codex-run.mjs log    --id <id> [--lines 60]
 *   node scripts/codex-run.mjs resume --id <id> --prompt-file <file>
 *   node scripts/codex-run.mjs list
 *
 * `run` options:
 *   --cwd <dir>          working root for Codex (default: repo root)
 *   --model <name>       default: from ~/.codex/config.toml
 *   --effort <level>     none|low|medium|high|xhigh|max  (default: medium)
 *   --sandbox <mode>     read-only|workspace-write|danger-full-access (default workspace-write)
 *   --timeout-min <n>    kill the worker after n minutes (default: none)
 *   --label <text>       human note stored in the meta file
 */

import { spawn, spawnSync } from 'node:child_process'
import {
  appendFileSync,
  existsSync,
  mkdirSync,
  readFileSync,
  readdirSync,
  writeFileSync
} from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const ROOT = resolve(HERE, '..')
const STATE = join(ROOT, '.kiln', 'codex')
const LAUNCHER = join(ROOT, '.cursor', 'mcp', 'codex-launcher.cjs')

const argv = process.argv.slice(2)
const cmd = argv[0]

function arg(name, fallback = null) {
  const i = argv.indexOf(name)
  if (i === -1) return fallback
  return argv[i + 1] ?? fallback
}

const metaPath = (id) => join(STATE, `${id}.meta.json`)
const logPath = (id) => join(STATE, `${id}.log`)
const errPath = (id) => join(STATE, `${id}.err`)
const lastPath = (id) => join(STATE, `${id}.last.md`)
const exitPath = (id) => join(STATE, `${id}.exit.json`)

function fail(msg) {
  console.error(`codex-run: ${msg}`)
  process.exit(1)
}

function ensureDirs() {
  mkdirSync(STATE, { recursive: true })
  mkdirSync(join(STATE, 'prompts'), { recursive: true })
}

function readMeta(id) {
  const p = metaPath(id)
  if (!existsSync(p)) fail(`unknown worker id: ${id} (no ${p})`)
  return JSON.parse(readFileSync(p, 'utf8'))
}

/** Detect Codex quota / auth problems so the orchestrator can switch writers. */
function diagnose(text) {
  const t = (text || '').toLowerCase()
  if (!t) return null
  if (/usage limit|rate limit|429|too many requests|quota exceeded|limit reached/.test(t))
    return 'RATE_LIMITED'
  if (/context (window|length)|too many tokens|token limit|compact/.test(t)) return 'CONTEXT_LIMIT'
  if (/not logged in|unauthorized|invalid api key|refresh token|401/.test(t)) return 'AUTH_ERROR'
  return null
}

function tail(file, lines) {
  if (!existsSync(file)) return ''
  return readFileSync(file, 'utf8').split(/\r?\n/).slice(-lines).join('\n')
}

function run() {
  ensureDirs()
  const id = arg('--id')
  const promptFile = arg('--prompt-file')
  if (!id || !promptFile) fail('run requires --id and --prompt-file')
  const pf = resolve(ROOT, promptFile)
  if (!existsSync(pf)) fail(`prompt file not found: ${pf}`)
  const prompt = readFileSync(pf, 'utf8')

  const meta = {
    id,
    label: arg('--label', ''),
    promptFile: pf,
    cwd: resolve(ROOT, arg('--cwd', ROOT)),
    model: arg('--model', null),
    effort: arg('--effort', 'medium'),
    sandbox: arg('--sandbox', 'workspace-write'),
    timeoutMin: arg('--timeout-min', null),
    log: logPath(id),
    err: errPath(id),
    lastMessage: lastPath(id),
    status: 'running',
    startedAt: new Date().toISOString(),
    pid: process.pid
  }
  writeFileSync(metaPath(id), JSON.stringify(meta, null, 2) + '\n')
  appendFileSync(meta.log, `\n=== codex-run ${id} @ ${meta.startedAt} ===\n`)

  const args = [
    LAUNCHER,
    'exec',
    '--cd',
    meta.cwd,
    '--sandbox',
    meta.sandbox,
    '--color',
    'never',
    '-o',
    meta.lastMessage,
    '-c',
    `model_reasoning_effort="${meta.effort}"`
  ]
  if (meta.model) args.push('-m', meta.model)
  args.push('-')

  const child = spawn(process.execPath, args, {
    cwd: meta.cwd,
    stdio: ['pipe', 'pipe', 'pipe'],
    windowsHide: true
  })

  const out = []
  const err = []
  child.stdout.on('data', (d) => {
    out.push(d)
    appendFileSync(meta.log, d)
  })
  child.stderr.on('data', (d) => {
    err.push(d)
    appendFileSync(meta.err, d)
  })

  let timer = null
  if (meta.timeoutMin) {
    timer = setTimeout(
      () => {
        console.error(`codex-run: ${id} exceeded --timeout-min ${meta.timeoutMin}, killing`)
        child.kill('SIGKILL')
      },
      Number(meta.timeoutMin) * 60_000
    )
  }

  child.stdin.end(prompt)

  child.on('error', (e) => {
    if (timer) clearTimeout(timer)
    finish(id, meta, null, `spawn error: ${e.message}`)
  })
  child.on('exit', (code) => {
    if (timer) clearTimeout(timer)
    finish(id, meta, code, err.join(''))
  })
}

function finish(id, meta, code, errText) {
  const outText = existsSync(meta.log) ? readFileSync(meta.log, 'utf8') : ''
  const signal = diagnose(String(errText || '') + '\n' + outText)
  const record = {
    id,
    exitCode: code,
    signal,
    finishedAt: new Date().toISOString()
  }
  writeFileSync(exitPath(id), JSON.stringify(record, null, 2) + '\n')
  meta.status = code === 0 ? 'done' : signal ? 'blocked' : 'failed'
  meta.exitCode = code
  meta.signal = signal
  writeFileSync(metaPath(id), JSON.stringify(meta, null, 2) + '\n')

  console.log('')
  console.log(`codex-run: ${id} exit=${code}${signal ? ` signal=${signal}` : ''}`)
  if (existsSync(meta.lastMessage)) {
    const text = readFileSync(meta.lastMessage, 'utf8')
    console.log('--- final message ---')
    console.log(text.length > 4000 ? '…' + text.slice(-4000) : text)
  } else {
    console.log('--- no final message; stderr tail ---')
    console.log(tail(meta.err, 30))
  }
  process.exit(code === 0 ? 0 : 1)
}

function status() {
  ensureDirs()
  const one = arg('--id')
  const ids = one ? [one] : listIds()
  if (!ids.length) {
    console.log('no workers found')
    return
  }
  for (const id of ids) {
    const mp = metaPath(id)
    if (!existsSync(mp)) {
      console.log(`${'MISSING'.padEnd(18)} ${id}`)
      continue
    }
    const meta = JSON.parse(readFileSync(mp, 'utf8'))
    let state = meta.status || 'unknown'
    if (existsSync(exitPath(id))) {
      const rec = JSON.parse(readFileSync(exitPath(id), 'utf8'))
      state =
        rec.exitCode === 0
          ? 'DONE'
          : rec.signal
            ? `BLOCKED(${rec.signal})`
            : `FAILED(${rec.exitCode})`
    }
    const mins = ((Date.now() - Date.parse(meta.startedAt)) / 60000).toFixed(1)
    const hasLast = existsSync(lastPath(id)) ? 'last=yes' : 'last=no'
    console.log(`${String(state).padEnd(18)} ${id.padEnd(24)} ${mins}m ${hasLast} ${meta.label || ''}`)
  }
}

function listIds() {
  if (!existsSync(STATE)) return []
  return readdirSync(STATE)
    .filter((f) => f.endsWith('.meta.json'))
    .map((f) => f.replace(/\.meta\.json$/, ''))
}

function showLast(id, chars) {
  if (!id) fail('--id is required')
  const p = lastPath(id)
  if (!existsSync(p)) {
    console.log('(no final message yet)')
    return
  }
  const text = readFileSync(p, 'utf8')
  console.log(text.length > chars ? '…' + text.slice(-chars) : text)
}

function sessionIdOf(id) {
  const text = existsSync(errPath(id)) ? readFileSync(errPath(id), 'utf8') : ''
  const m = text.match(/session id:\s*([0-9a-f-]{8,})/i)
  return m ? m[1] : null
}

function resume() {
  ensureDirs()
  const id = arg('--id')
  const promptFile = arg('--prompt-file')
  if (!id || !promptFile) fail('resume requires --id and --prompt-file')
  const meta = readMeta(id)
  const pf = resolve(ROOT, promptFile)
  if (!existsSync(pf)) fail(`prompt file not found: ${pf}`)

  // Resume by explicit session id: several workers share one cwd, so `--last`
  // would resume whichever Codex session finished most recently.
  const sid = sessionIdOf(id)
  if (!sid) fail(`no session id found in ${errPath(id)} — cannot resume safely`)

  appendFileSync(meta.log, `\n=== resume ${sid} @ ${new Date().toISOString()} ===\n`)
  const child = spawnSync(
    process.execPath,
    [
      LAUNCHER,
      'exec',
      'resume',
      sid,
      '-o',
      meta.lastMessage,
      '-c',
      `model_reasoning_effort="${meta.effort}"`,
      '-'
    ],
    {
      cwd: meta.cwd,
      input: readFileSync(pf, 'utf8'),
      encoding: 'utf8',
      windowsHide: true
    }
  )
  appendFileSync(meta.log, child.stdout || '')
  appendFileSync(meta.err, child.stderr || '')
  finish(id, meta, child.status, child.stderr || '')
}

switch (cmd) {
  case 'run':
    run()
    break
  case 'status':
    status()
    break
  case 'list':
    status()
    break
  case 'last':
    showLast(arg('--id'), Number(arg('--chars', '4000')))
    break
  case 'log':
    console.log(tail(logPath(arg('--id')), Number(arg('--lines', '60'))))
    console.log('--- stderr ---')
    console.log(tail(errPath(arg('--id')), Number(arg('--lines', '30'))))
    break
  case 'resume':
    resume()
    break
  default:
    console.log(
      [
        'PlayKiln Codex worker runner (foreground; let your shell tool background it)',
        '',
        '  run    --id <id> --prompt-file <file> [--cwd dir] [--model m] [--effort lvl] [--sandbox mode] [--timeout-min n]',
        '  status [--id <id>]',
        '  last   --id <id> [--chars n]',
        '  log    --id <id> [--lines n]',
        '  resume --id <id> --prompt-file <file>',
        '  list'
      ].join('\n')
    )
    process.exit(cmd ? 1 : 0)
}
