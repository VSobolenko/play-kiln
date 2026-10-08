#!/usr/bin/env node
import { spawn, spawnSync } from 'node:child_process'
import { appendFileSync, existsSync, mkdirSync, readFileSync, readdirSync, unlinkSync, writeFileSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { randomUUID } from 'node:crypto'
import { buildRunArgs, finalAnswerFrom, resolveCopilot, sessionIdFromOutput } from './lib/copilot-cli.mjs'
import { diagnose, DIAGNOSIS_WINDOW_LINES } from './lib/codex-signals.mjs'
import { classifyWorkerFromDisk, isProcessAlive, LOST_NOTE, LOST_STATE, readJsonSafe } from './lib/codex-ledger.mjs'
import { scratchEnv } from './lib/scratch.mjs'

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const STATE = join(ROOT, '.kiln', 'copilot')
const argv = process.argv.slice(2); const cmd = argv[0]
const arg = (name, fallback = null) => { const i = argv.indexOf(name); return i < 0 ? fallback : (argv[i + 1] ?? fallback) }
const paths = (id) => ({ meta: join(STATE, `${id}.meta.json`), log: join(STATE, `${id}.log`), err: join(STATE, `${id}.err`), last: join(STATE, `${id}.last.md`), exit: join(STATE, `${id}.exit.json`) })
function fail(message) { console.error(`copilot-run: ${message}`); process.exit(1) }
function ensure() { mkdirSync(STATE, { recursive: true }); mkdirSync(join(STATE, 'prompts'), { recursive: true }) }
function tail(file, lines) { return existsSync(file) ? readFileSync(file, 'utf8').split(/\r?\n/).slice(-lines).join('\n') : '' }
function ids() { return existsSync(STATE) ? readdirSync(STATE).filter((f) => f.endsWith('.meta.json')).map((f) => f.replace(/\.meta\.json$/, '')) : [] }
function readMeta(id) { const p = paths(id).meta; const r = readJsonSafe(p); if (!r.ok) fail(`unreadable meta for ${id}: ${p} (${r.error})`); return r.value }
function validateRun(id, promptFile) { if (!id || !promptFile) fail('run requires --id and --prompt-file'); const pf = resolve(ROOT, promptFile); if (!existsSync(pf)) fail(`prompt file not found: ${pf}`); return pf }
function finish(id, meta, exitCode, stderr, stdout) {
  const p = paths(id); const diagnostic = `${String(stderr || '').split(/\r?\n/).slice(-DIAGNOSIS_WINDOW_LINES).join('\n')}\n${tail(p.log, DIAGNOSIS_WINDOW_LINES)}`
  const signal = diagnose(diagnostic); const endedAt = new Date().toISOString()
  writeFileSync(p.exit, JSON.stringify({ exitCode, signal, endedAt }, null, 2) + '\n')
  meta.status = signal ? 'blocked' : exitCode === 0 ? 'done' : 'failed'; meta.endedAt = endedAt; meta.exitCode = exitCode; if (signal) meta.signal = signal
  const sessionId = sessionIdFromOutput(`${stdout || ''}\n${stderr || ''}`); if (sessionId) meta.sessionId = sessionId
  writeFileSync(p.meta, JSON.stringify(meta, null, 2) + '\n')
  if (exitCode === 0) writeFileSync(p.last, finalAnswerFrom(stdout))
  console.log(`copilot-run: ${id} exit=${exitCode}${signal ? ` signal=${signal}` : ''}`)
  if (existsSync(p.last)) console.log(finalAnswerFrom(readFileSync(p.last, 'utf8')))
  process.exitCode = exitCode === 0 ? 0 : 1
}
function launch({ id, promptFile, resume = false }) {
  ensure(); const pf = resume ? (arg('--prompt-file') || fail('resume requires --id and --prompt-file')) : validateRun(id, promptFile)
  const resolvedPrompt = resolve(ROOT, pf); if (!existsSync(resolvedPrompt)) fail(`prompt file not found: ${resolvedPrompt}`)
  const old = resume ? readMeta(id) : null; const p = paths(id)
  const cwd = resolve(ROOT, resume ? old.cwd : arg('--cwd', ROOT)); if (!existsSync(cwd)) fail(`cwd not found: ${cwd}`)
  const meta = resume ? { ...old, status: 'running', pid: process.pid, promptFile: resolvedPrompt, startedAt: new Date().toISOString() } : { id, label: arg('--label', ''), status: 'running', pid: process.pid, startedAt: new Date().toISOString(), promptFile: resolvedPrompt, cwd, model: arg('--model'), effort: arg('--effort'), permissions: arg('--permissions', 'tools'), sessionId: randomUUID(), agent: arg('--agent') }
  if (resume && !meta.sessionId) fail(`no session id recorded for ${id} — cannot resume safely`)
  if (!resume && existsSync(p.last)) unlinkSync(p.last)
  writeFileSync(p.meta, JSON.stringify(meta, null, 2) + '\n'); appendFileSync(p.log, `\n=== ${resume ? 'resume' : 'copilot-run'} ${id} @ ${meta.startedAt} ===\n`)
  let binary; try { binary = resolveCopilot() } catch (e) { fail(e.message) }
  let child; try { child = spawn(binary.command, [...binary.prefix, ...buildRunArgs({ prompt: readFileSync(resolvedPrompt, 'utf8'), cwd, permissions: meta.permissions, model: meta.model, effort: meta.effort, agent: meta.agent ?? arg('--agent'), sessionId: meta.sessionId, resume })], { cwd, env: scratchEnv(process.env, `copilot-${id}`), stdio: ['ignore', 'pipe', 'pipe'], windowsHide: true }) } catch (e) { finish(id, meta, 1, `spawn error: ${e.message}`, ''); return }
  const out = []; const err = []; child.stdout.on('data', (d) => { out.push(d); appendFileSync(p.log, d) }); child.stderr.on('data', (d) => { err.push(d); appendFileSync(p.err, d) })
  let timer = null; const timeout = Number(resume ? null : arg('--timeout-min'))
  if (arg('--timeout-min') !== null && (!Number.isInteger(timeout) || timeout <= 0)) fail('--timeout-min must be a positive integer')
  if (Number.isInteger(timeout) && timeout > 0) timer = setTimeout(() => child.kill('SIGKILL'), timeout * 60_000)
  child.on('error', (e) => { if (timer) clearTimeout(timer); finish(id, meta, 1, `spawn error: ${e.message}`, Buffer.concat(out).toString()) })
  child.on('close', (code) => { if (timer) clearTimeout(timer); finish(id, meta, code ?? 1, Buffer.concat(err).toString(), Buffer.concat(out).toString()) })
}
function status() { ensure(); const wanted = arg('--id'); const list = wanted ? [wanted] : ids(); if (!list.length) return console.log('no workers found'); for (const id of list) { const p = paths(id); const r = readJsonSafe(p.meta); if (!r.ok) { console.log(`${'UNREADABLE'.padEnd(18)} ${id}`); continue }; let exit = null; if (existsSync(p.exit)) { const er = readJsonSafe(p.exit); if (!er.ok) { console.log(`${'UNREADABLE'.padEnd(18)} ${id}`); continue }; exit = er.value }; const state = classifyWorkerFromDisk(r.value, exit); console.log(`${String(state).padEnd(18)} ${id.padEnd(24)} last=${existsSync(p.last) ? 'yes' : 'no'} ${r.value.label || ''}${state === LOST_STATE ? ` ${LOST_NOTE}` : ''}`) } }
function probe() { const binary = resolveCopilot(); const result = spawnSync(binary.command, [...binary.prefix, ...buildRunArgs({ prompt: 'Reply with exactly: PONG', cwd: ROOT, model: arg('--model') })], { cwd: ROOT, env: scratchEnv(process.env, 'copilot-probe'), encoding: 'utf8', windowsHide: true }); console.log(`copilot-run probe: exit=${result.status}`); console.log(result.stdout || ''); process.exit(result.status === 0 ? 0 : 1) }
switch (cmd) { case 'run': launch({ id: arg('--id'), promptFile: arg('--prompt-file') }); break; case 'resume': { const id = arg('--id'); if (!id || !arg('--prompt-file')) fail('resume requires --id and --prompt-file'); launch({ id, resume: true }); break }; case 'status': case 'list': status(); break; case 'last': { const id = arg('--id'); if (!id) fail('--id is required'); console.log(tail(paths(id).last, Number(arg('--chars', '4000')))); break }; case 'log': { const id = arg('--id'); if (!id) fail('--id is required'); const n = Number(arg('--lines', '60')); console.log(tail(paths(id).log, n)); console.log('--- stderr ---'); console.log(tail(paths(id).err, n)); break }; case 'probe': probe(); break; default: console.log('Usage: copilot-run.mjs run|status|list|last|log|resume|probe'); process.exit(cmd ? 1 : 0) }
