/** Provider-specific Copilot CLI mechanics, kept separate from the durable runner. */
import { spawnSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import { dirname, join } from 'node:path'

export const EFFORTS = Object.freeze(['none', 'minimal', 'low', 'medium', 'high', 'xhigh', 'max'])

export function resolveCopilot(binary = 'copilot') {
  if (process.platform === 'win32') {
    const lookup = spawnSync('powershell.exe', ['-NoProfile', '-Command', `(Get-Command ${binary} -ErrorAction Stop).Source`], { encoding: 'utf8', windowsHide: true })
    const source = String(lookup.stdout || '').trim()
    if (lookup.status !== 0 || !source) throw new Error(`could not resolve ${binary} on PATH: ${String(lookup.stderr || '').trim()}`)
    return resolveWindowsShim(source)
  }
  const probe = spawnSync(binary, ['--version'], { encoding: 'utf8', windowsHide: true })
  if (probe.error) throw new Error(`could not start ${binary}: ${probe.error.message}`)
  if (probe.status !== 0) throw new Error(`${binary} --version exited ${probe.status}`)
  return { command: binary, prefix: [] }
}

/**
 * The npm shims on Windows (`copilot.ps1` and `copilot.cmd`) both forward their arguments to
 * `node <npm-root>/node_modules/@github/copilot/npm-loader.js`. Driving the `.ps1` through
 * `powershell.exe -File` is not safe: the shim passes a bare `$args` array to a native command
 * and PowerShell re-splits it, so a prompt containing spaces or newlines arrives at the CLI as
 * several arguments and it answers "your prompt was not quoted". Spawning `node` with the loader
 * directly keeps every argument intact — one argv element in, one argv element out.
 *
 * Measured 2026-10-08: `-p "<multi-line prompt>"` via the `.ps1` shim failed with
 * `Invalid command format. It looks like your prompt was not quoted…`; the same prompt through
 * this route is delivered whole.
 */
export function resolveWindowsShim(shimPath) {
  const basedir = dirname(shimPath)
  const loader = join(basedir, 'node_modules', '@github', 'copilot', 'npm-loader.js')
  if (existsSync(loader)) {
    const localNode = join(basedir, 'node.exe')
    return { command: existsSync(localNode) ? localNode : 'node', prefix: [loader] }
  }
  // An unknown shim shape: `cmd.exe` forwards `%*` verbatim, which is the other safe route.
  const cmd = shimPath.replace(/\.ps1$/i, '.cmd')
  if (existsSync(cmd)) return { command: 'cmd.exe', prefix: ['/d', '/s', '/c', cmd] }
  return { command: 'powershell.exe', prefix: ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', shimPath] }
}

export function buildRunArgs({ prompt, cwd, permissions = 'tools', model = null, effort = null, agent = null, sessionId = null, resume = false }) {
  if (!['tools', 'tools+paths'].includes(permissions)) throw new Error(`unknown permissions mode: ${permissions}`)
  if (effort && !EFFORTS.includes(effort)) throw new Error(`unknown effort: ${effort}`)
  // Do not use --silent: Copilot prints its durable `Resume <uuid>` line only in normal text mode.
  const args = ['-p', prompt, '--allow-all-tools', '--disallow-temp-dir', '-C', cwd]
  if (permissions === 'tools+paths') args.push('--allow-all-paths')
  if (model) args.push('--model', model)
  if (effort) args.push('--reasoning-effort', effort)
  if (agent) args.push('--agent', agent)
  if (sessionId) args.push(resume ? '--resume' : '--session-id', sessionId)
  return args
}

/** Copilot's silent text response is the final answer; preserve it verbatim. */
export function finalAnswerFrom(stdout) {
  return String(stdout || '').replace(/\r?\n(?:Changes \+\d+ -\d+|AI Credits .*|Tokens .*|Resume [0-9a-f-]+)\s*$/gim, '').trimEnd() + '\n'
}

/** A session id may be printed in non-silent output; also accept JSONL session fields for future modes. */
export function sessionIdFromOutput(text) {
  const raw = String(text || '')
  const printed = raw.match(/\bResume\s+([0-9a-f]{8,}(?:-[0-9a-f]{4,}){0,4})\b/i)
  if (printed) return printed[1]
  for (const line of raw.split(/\r?\n/)) {
    try {
      const value = JSON.parse(line)
      const id = value?.sessionId ?? value?.session_id
      if (typeof id === 'string' && id) return id
    } catch { /* text output */ }
  }
  return null
}
