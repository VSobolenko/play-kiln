#!/usr/bin/env node
/**
 * Serialized MCP launcher — avoids concurrent `npx -y` races (npm ECOMPROMISED).
 *
 * Usage (from mcp.json):
 *   node .cursor/mcp/run-mcp.cjs <alias> [...extraArgs]
 *
 * Resolution:
 *   1. Local node_modules package bin (node <js>) — preferred, no npx race
 *   2. npx -y under a cross-process lock
 */
const { spawn } = require('child_process')
const fs = require('fs')
const os = require('os')
const path = require('path')

const root = path.resolve(__dirname, '..', '..')
const alias = {
  context7: '@upstash/context7-mcp',
  playwright: '@playwright/mcp',
  'android-skills': 'android-skills-mcp',
  droidagentkit: '@droidagentkit/launcher',
  firebase: 'firebase-tools'
}

const raw = process.argv[2]
if (!raw) {
  console.error('usage: node run-mcp.cjs <alias|package> [...args]')
  process.exit(1)
}

const pkg = alias[raw] || raw
const extraArgs = process.argv.slice(3)
const lockPath = path.join(os.tmpdir(), 'playkiln-mcp-npx.lock')

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms))
}

async function withLock(fn) {
  const start = Date.now()
  for (;;) {
    try {
      const fd = fs.openSync(lockPath, 'wx')
      fs.writeFileSync(fd, String(process.pid))
      fs.closeSync(fd)
      try {
        return await fn()
      } finally {
        try {
          fs.unlinkSync(lockPath)
        } catch {}
      }
    } catch {
      if (Date.now() - start > 180000) {
        console.error('run-mcp: timed out waiting for npx lock')
        process.exit(1)
      }
      try {
        const st = fs.statSync(lockPath)
        if (Date.now() - st.mtimeMs > 300000) fs.unlinkSync(lockPath)
      } catch {}
      await sleep(400)
    }
  }
}

function resolveLocalEntry() {
  const pkgJsonPath = path.join(root, 'node_modules', ...pkg.split('/'), 'package.json')
  if (!fs.existsSync(pkgJsonPath)) return null
  let pj
  try {
    pj = JSON.parse(fs.readFileSync(pkgJsonPath, 'utf8'))
  } catch {
    return null
  }
  const bin = pj.bin
  let rel = null
  if (typeof bin === 'string') rel = bin
  else if (bin && typeof bin === 'object') {
    rel = bin['context7-mcp'] || bin[Object.keys(bin)[0]]
  }
  if (!rel && pj.main) rel = pj.main
  if (!rel) return null
  const abs = path.join(path.dirname(pkgJsonPath), rel)
  if (!fs.existsSync(abs)) return null
  return abs
}

function run(cmd, args, { shell = false } = {}) {
  const child = spawn(cmd, args, {
    stdio: 'inherit',
    shell,
    cwd: root,
    env: process.env,
    windowsHide: true
  })
  child.on('exit', (code, signal) => {
    if (signal) process.exit(1)
    process.exit(code === null ? 1 : code)
  })
  child.on('error', (err) => {
    console.error('run-mcp: failed to start', cmd, err.message)
    process.exit(1)
  })
}

;(async () => {
  // firebase special: `firebase mcp --dir <root>`
  if (raw === 'firebase' || pkg === 'firebase-tools') {
    const localFirebase = path.join(root, 'node_modules', 'firebase-tools')
    if (fs.existsSync(localFirebase)) {
      run(process.execPath, [
        path.join(localFirebase, 'lib', 'bin', 'firebase.js'),
        'mcp',
        '--dir',
        root,
        ...extraArgs
      ])
      return
    }
    await withLock(() =>
      run(process.platform === 'win32' ? 'npx.cmd' : 'npx', [
        '-y',
        'firebase-tools@latest',
        'mcp',
        '--dir',
        root,
        ...extraArgs
      ], { shell: true })
    )
    return
  }

  const entry = resolveLocalEntry()
  if (entry) {
    run(process.execPath, [entry, ...extraArgs])
    return
  }

  await withLock(() =>
    run(
      process.platform === 'win32' ? 'npx.cmd' : 'npx',
      ['-y', pkg, ...extraArgs],
      { shell: true }
    )
  )
})()
