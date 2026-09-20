#!/usr/bin/env node
/**
 * Objective verification gate for PlayKiln.
 *
 *   node scripts/verify-slot.mjs
 *   node scripts/verify-slot.mjs --slot <slug>
 *   node scripts/verify-slot.mjs --base <ref>
 *   node scripts/verify-slot.mjs --build   # also Gradle assembleDebug for touched slots
 */

import { spawnSync } from 'node:child_process'
import { existsSync, readdirSync, statSync, readFileSync } from 'node:fs'
import path from 'node:path'
import process from 'node:process'

const ROOT = process.cwd()
const ARGV = process.argv.slice(2)
const baseIdx = ARGV.indexOf('--base')
const BASE_REF = baseIdx !== -1 ? ARGV[baseIdx + 1] : null
const slotIdx = ARGV.indexOf('--slot')
const SLOT = slotIdx !== -1 ? ARGV[slotIdx + 1] : null
const RUN_BUILD = ARGV.includes('--build')

function run(cmd, cmdArgs, cwd, { useShell = false } = {}) {
  const res = spawnSync(cmd, cmdArgs, {
    cwd,
    encoding: 'utf8',
    shell: useShell
  })
  const out = ((res.stdout || '') + (res.stderr || '')).trim()
  if (res.error) {
    return { ok: false, missing: res.error.code === 'ENOENT', out: String(res.error.message) }
  }
  return { ok: res.status === 0, status: res.status, out }
}

function gitLines(gitArgs, { trim = true } = {}) {
  const res = spawnSync('git', gitArgs, { cwd: ROOT, encoding: 'utf8' })
  if (res.status !== 0) {
    console.error('verify-slot: git failed:\n' + (res.stderr || ''))
    process.exit(1)
  }
  const lines = res.stdout.split('\n').filter(Boolean)
  return trim ? lines.map((s) => s.trim()) : lines
}

function changedFiles() {
  if (BASE_REF) {
    return gitLines(['diff', '--name-only', '--diff-filter=ACMR', `${BASE_REF}...HEAD`])
  }
  return gitLines(['status', '--porcelain'], { trim: false })
    .map((line) => {
      let p = line.slice(3)
      const arrow = p.indexOf(' -> ')
      if (arrow !== -1) p = p.slice(arrow + 4)
      return p.replace(/^"|"$/g, '').trim()
    })
    .filter(Boolean)
}

function listAppDirs() {
  const apps = path.join(ROOT, 'apps')
  if (!existsSync(apps)) return []
  return readdirSync(apps)
    .map((n) => path.join(apps, n))
    .filter((p) => statSync(p).isDirectory())
}

function resolveSlotDir(slug) {
  const apps = listAppDirs()
  const hit = apps.find((p) => path.basename(p).endsWith('-' + slug) || path.basename(p) === slug)
  return hit || null
}

function checkNodeSyntax(files) {
  let failed = false
  let blocked = false
  for (const f of files) {
    const abs = path.join(ROOT, f)
    if (!existsSync(abs)) continue
    const r = run(process.execPath, ['--check', abs], ROOT)
    if (r.missing) {
      console.log('  BLOCKED     node --check unavailable')
      blocked = true
      break
    }
    if (r.ok) console.log(`  PASS        syntax — ${f}`)
    else {
      console.log(`  FAIL        syntax — ${f}`)
      console.log('        ' + (r.out || '').split('\n').join('\n        '))
      failed = true
    }
  }
  return { failed, blocked }
}

function gradleAssemble(slotDir) {
  const isWin = process.platform === 'win32'
  const wrapper = path.join(slotDir, isWin ? 'gradlew.bat' : 'gradlew')
  if (!existsSync(wrapper)) {
    return { ok: false, missing: true, out: 'gradlew not found' }
  }
  return run(wrapper, ['assembleDebug', '--quiet'], slotDir, { useShell: isWin })
}

const indent = (s, pad = '        ') =>
  !s ? pad + '(no output)' : s.split('\n').map((l) => pad + l).join('\n')

const tracked = SLOT
  ? listAppDirs()
      .filter((p) => path.basename(p).includes(SLOT))
      .flatMap((p) => {
        // when --slot is set without git changes, still verify that slot
        return [`apps/${path.basename(p)}/README.md`]
      })
  : [...new Set(changedFiles())]

console.log('## PlayKiln verification\n')
console.log(
  `Scope: ${SLOT ? `slot ${SLOT}` : BASE_REF ? `vs ${BASE_REF}` : 'uncommitted'} — ${tracked.length} path(s)`
)

if (!SLOT && tracked.length === 0) {
  // Still allow verifying harness scripts if nothing changed? Prefer fail empty.
  console.log('\nNothing changed. Nothing to verify.')
  process.exit(1)
}

let failed = false
let blocked = false
const unverified = []

// Harness JS
const harnessJs = tracked.filter(
  (f) =>
    (f.startsWith('scripts/') || f.startsWith('.cursor/hooks/') || f.startsWith('.cursor/mcp/')) &&
    /\.(js|mjs|cjs)$/.test(f)
)
if (harnessJs.length) {
  console.log(`\n### harness scripts (${harnessJs.length})`)
  const r = checkNodeSyntax(harnessJs)
  failed = failed || r.failed
  blocked = blocked || r.blocked
}

// OpenSpec / docs — UNVERIFIED (manual)
const docs = tracked.filter((f) => f.startsWith('openspec/') || f.startsWith('docs/') || f.endsWith('.md'))
if (docs.length) {
  console.log(`\n### docs/openspec (${docs.length})`)
  console.log('  UNVERIFIED  markdown/spec — read by hand')
  unverified.push('docs/openspec')
}

// Slots
const slotDirs = new Set()
for (const f of tracked) {
  const m = f.match(/^apps\/([^/]+)\//)
  if (m) slotDirs.add(m[1])
}
if (SLOT) {
  const d = resolveSlotDir(SLOT)
  if (d) slotDirs.add(path.basename(d))
  else {
    console.log(`\n### slot ${SLOT}`)
    console.log('  FAIL        slot directory not found under apps/')
    failed = true
  }
}

for (const name of slotDirs) {
  const dir = path.join(ROOT, 'apps', name)
  console.log(`\n### apps/${name}`)
  if (!existsSync(dir)) {
    console.log('  FAIL        missing directory')
    failed = true
    continue
  }

  const manifest = path.join(dir, 'app', 'src', 'main', 'AndroidManifest.xml')
  const gradle = path.join(dir, 'app', 'build.gradle.kts')
  if (!existsSync(manifest) || !existsSync(gradle)) {
    console.log('  FAIL        not a Compose slot (missing app module files)')
    failed = true
    continue
  }

  // applicationId sanity via build.gradle.kts
  const gradleText = readFileSync(gradle, 'utf8')
  if (!/applicationId\s*=\s*"com\.playkiln\./.test(gradleText)) {
    console.log('  FAIL        applicationId must be com.playkiln.*')
    failed = true
  } else {
    console.log('  PASS        applicationId namespace')
  }

  // Kotlin sources — no mechanical style linter yet
  const ktChanged = tracked.filter((f) => f.startsWith(`apps/${name}/`) && f.endsWith('.kt'))
  if (ktChanged.length) {
    console.log(`  UNVERIFIED  kotlin style (${ktChanged.length} file(s)) — read diff by hand`)
    unverified.push(name)
  }

  if (RUN_BUILD) {
    const r = gradleAssemble(dir)
    if (r.missing) {
      console.log('  BLOCKED     Gradle wrapper / SDK not available')
      console.log(indent(r.out))
      blocked = true
    } else if (r.ok) {
      console.log('  PASS        assembleDebug')
    } else {
      console.log('  FAIL        assembleDebug')
      console.log(indent(r.out.split('\n').slice(-40).join('\n')))
      failed = true
    }
  } else {
    console.log('  SKIP        assembleDebug (pass --build to run)')
  }
}

console.log('\n---')
if (unverified.length) {
  console.log(`UNVERIFIED: ${[...new Set(unverified)].join(', ')}`)
}
if (failed) {
  console.log('\nVERDICT: FAIL')
  process.exit(1)
}
if (blocked) {
  console.log('\nVERDICT: BLOCKED')
  process.exit(1)
}
console.log('\nVERDICT: PASS — available checks green.')
