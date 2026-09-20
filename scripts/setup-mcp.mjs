#!/usr/bin/env node

import { execFile } from 'node:child_process'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'

const here = dirname(fileURLToPath(import.meta.url))
const root = join(here, '..')
const mcpPath = join(root, '.cursor', 'mcp.json')

const MANUAL_NOTES = {
  firebase: 'optional — `firebase login` + a Firebase project when a slot needs it',
  codex: 'needs ChatGPT/Codex editor extension (or CODEX_BIN)'
}

function runNpx(pkg) {
  return new Promise((resolve) => {
    execFile('npx', ['-y', pkg, '--help'], { timeout: 180000 }, (err, stdout, stderr) => {
      if (err && err.killed) {
        resolve({ ok: false, output: 'timed out after 180s' })
        return
      }
      const out = (stdout || '') + (stderr || '')
      const failed = /could not find|not found|E404|ECONNREFUSED|ENOTFOUND|Cannot find module/i.test(out)
      const firstLine = out.trim().split(/\r?\n/)[0]
      resolve({ ok: !failed, output: firstLine || (err && err.message) || '' })
    })
  })
}

async function main() {
  let cfg
  try {
    cfg = JSON.parse(readFileSync(mcpPath, 'utf8'))
  } catch (e) {
    console.error('[FAIL] cannot read .cursor/mcp.json:', e.message)
    process.exit(1)
  }

  const servers = cfg.mcpServers || {}
  console.log('Checking', Object.keys(servers).length, 'MCP server(s)...\n')
  const needsManual = []

  for (const name of Object.keys(servers)) {
    const s = servers[name]
    if (s.command === 'node') {
      console.log('[SETUP]', name + ':', MANUAL_NOTES[name] || 'local node launcher')
      needsManual.push(name)
      continue
    }
    if (s.command === 'npx' && Array.isArray(s.args)) {
      const pkg = s.args.find((a) => !a.startsWith('-') && !a.startsWith('.')) || s.args[0]
      process.stdout.write('[  .. ] ' + name + ': resolving ' + pkg + ' ...')
      const r = await runNpx(pkg)
      console.log(r.ok ? ' [OK]' : ' [FAIL]')
      if (!r.ok) needsManual.push(name + ': ' + r.output)
      if (MANUAL_NOTES[name]) needsManual.push(name + ': ' + MANUAL_NOTES[name])
    }
  }

  console.log('\nDone. Enable servers in Cursor Settings → MCP, then reload.')
  if (needsManual.length) {
    console.log('\nManual steps:')
    for (const line of needsManual) console.log(' -', line)
  }
}

main()
