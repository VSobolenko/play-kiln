#!/usr/bin/env node
/**
 * afterFileEdit — append a one-line ledger entry (observe-only).
 */
const fs = require('fs')
const path = require('path')

let input = {}
try {
  input = JSON.parse(fs.readFileSync(0, 'utf8') || '{}')
} catch {}

const root = (input.workspace_roots && input.workspace_roots[0]) || process.cwd()
const filePath = input.file_path || ''
const dir = path.join(root, '.superpowers', 'edit-ledger')
try {
  fs.mkdirSync(dir, { recursive: true })
  const day = new Date().toISOString().slice(0, 10)
  const line =
    new Date().toISOString() +
    '\t' +
    (input.conversation_id || '-') +
    '\t' +
    filePath +
    '\n'
  fs.appendFileSync(path.join(dir, day + '.log'), line)
} catch {}

process.stdout.write('{}')
