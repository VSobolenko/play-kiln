#!/usr/bin/env node
import { buildRunArgs, finalAnswerFrom, sessionIdFromOutput } from './lib/copilot-cli.mjs'
import { diagnose } from './lib/codex-signals.mjs'
let failures = 0
function check(name, actual, expected) { const ok = JSON.stringify(actual) === JSON.stringify(expected); console.log(`  ${ok ? 'PASS' : 'FAIL'}  ${name}`); if (!ok) { console.log(`        expected ${JSON.stringify(expected)}, got ${JSON.stringify(actual)}`); failures++ } }
console.log('=== Copilot provider seam ===')
check('run includes safety flags', buildRunArgs({ prompt: 'x', cwd: 'D:/repo' }).slice(0, 5), ['-p', 'x', '--allow-all-tools', '--disallow-temp-dir', '-C'])
check('cost flags remain absent by default', buildRunArgs({ prompt: 'x', cwd: 'D:/repo' }).includes('--model'), false)
check('optional flags are forwarded', buildRunArgs({ prompt: 'x', cwd: 'D:/repo', model: 'auto', effort: 'low', permissions: 'tools+paths', agent: 'worker', sessionId: 'abc', resume: true }).slice(-9), ['--allow-all-paths', '--model', 'auto', '--reasoning-effort', 'low', '--agent', 'worker', '--resume', 'abc'])
check('stats are removed from the final answer', finalAnswerFrom('PONG\nChanges +0 -0\nAI Credits 1\nTokens ↑ 1 ↓ 2\nResume 12345678\n'), 'PONG\n')
check('resume line yields session id', sessionIdFromOutput('Resume 12345678-abcd-1234-abcd-123456789abc'), '12345678-abcd-1234-abcd-123456789abc')
console.log('\n=== shared signal classifier ===')
check('NativeCommandError success noise is not a signal', diagnose('NativeCommandError: node.exe copilot.ps1'), null)
check('Copilot sign-in error is AUTH_ERROR', diagnose('ERROR: not logged in. Please sign in.'), 'AUTH_ERROR')
console.log(failures ? `COPILOT SIGNAL REGRESSION: ${failures} failure(s)` : 'COPILOT SIGNAL REGRESSION: all checks passed')
process.exit(failures ? 1 : 0)
