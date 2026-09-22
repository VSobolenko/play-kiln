#!/usr/bin/env node
/**
 * stop — one evidence follow-up for factory discipline (loop_count === 0 only).
 * Disable with env PLAYKILN_STOP_FOLLOWUP=0
 */
const fs = require('fs')

let input = {}
try {
  input = JSON.parse(fs.readFileSync(0, 'utf8') || '{}')
} catch {}

if (process.env.PLAYKILN_STOP_FOLLOWUP === '0') {
  process.stdout.write('{}')
  process.exit(0)
}

const loop = Number(input.loop_count || 0)
const status = input.status || 'completed'

if (status === 'completed' && loop === 0) {
  process.stdout.write(
    JSON.stringify({
      followup_message:
        'PlayKiln stop gate: if this session changed apps/ or harness, confirm evidence ' +
        '(`node scripts/verify-slot.mjs` and/or `/eval`). If already reported with stdout, reply DONE.'
    })
  )
} else {
  process.stdout.write('{}')
}
