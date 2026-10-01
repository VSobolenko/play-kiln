#!/usr/bin/env node
/**
 * Report whether the PlayKiln factory has the files and local tools needed to build and ship,
 * and what each slot has earned.
 * Usage: node scripts/kiln-status.mjs
 *        node scripts/kiln-status.mjs --kill <slug> --reason "<why>" [--force]
 *
 * The earnings section exists because the factory's product is revenue, and until now nothing
 * in this file — or in the repository — ever related a shipped slot to a number. `economics.json`
 * carried `killCriteria` and `check-economics.mjs` validated that they were present, and no
 * program read `minInstalls` or `minRevenueUsd` afterwards; the post-mortem for the first slot
 * called this out in its own words ("the kill criteria will remain prose that nothing
 * evaluates"). A portfolio only works when attempts that miss are stopped on a clock, so the
 * verdict is computed here for every slot, and `--kill` is the path that records the decision.
 *
 * `--kill` refuses without a reason, because the prior-art failure it replaces is a slot that is
 * silently abandoned rather than archived with its numbers. It also refuses to kill a slot whose
 * latest reading meets its own criteria unless `--force` is passed: the threshold it enforces is
 * the one the plan itself declared, and a rule that can be fired at anything is not a rule.
 */

import { existsSync, readFileSync, readdirSync, statSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'
import { applicationIdPattern, scaffoldPackage } from './lib/namespaces.mjs'
import { resolveJavaHome } from './lib/java.mjs'
import { checkReleaseApproval, slotSlug } from './lib/release-approval.mjs'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
let hasFailure = false

function line(status, message) {
  console.log(`${status.padEnd(8)} ${message}`)
  if (status === 'FAIL') hasFailure = true
}

function section(title) {
  console.log(`\n## ${title}`)
}

function run(command, args) {
  const result = spawnSync(command, args, {
    cwd: root,
    encoding: 'utf8',
    shell: false,
    timeout: 30_000,
    windowsHide: true
  })
  return {
    ok: result.status === 0,
    status: result.status,
    output: `${result.stdout || ''}${result.stderr || ''}`.trim(),
    error: result.error
  }
}

function firstLine(value) {
  return String(value || '').split(/\r?\n/).map((item) => item.trim()).find(Boolean) || ''
}

function readText(relativePath) {
  const file = join(root, relativePath)
  if (!existsSync(file)) return null
  try {
    return readFileSync(file, 'utf8').replace(/^\uFEFF/, '')
  } catch (error) {
    line('FAIL', `${relativePath}: ${error.message}`)
    return null
  }
}

function javaMajor(output) {
  const match = output.match(/version\s+"(?:1\.)?(\d+)/i) || output.match(/openjdk\s+(\d+)/i)
  return match ? Number(match[1]) : null
}

function versionParts(value) {
  return value.split('.').map((part) => Number(part.match(/^\d+/)?.[0] || 0))
}

function compareVersions(left, right) {
  const a = versionParts(left)
  const b = versionParts(right)
  for (let index = 0; index < Math.max(a.length, b.length); index += 1) {
    const difference = (a[index] || 0) - (b[index] || 0)
    if (difference !== 0) return difference
  }
  return 0
}

console.log('# PlayKiln status')

const ARGV = process.argv.slice(2)

/** The value that follows `flag`, or null. A flag with no value is not a value. */
function flagValue(flag) {
  const index = ARGV.indexOf(flag)
  if (index === -1) return null
  const value = ARGV[index + 1]
  return value && !value.startsWith('--') ? value : null
}

const KILL_SLOT = flagValue('--kill')
const KILL_REASON = flagValue('--reason')
const KILL_FORCE = ARGV.includes('--force')

/**
 * The money stage: what each slot has actually earned, and whether it is time to stop.
 *
 * The verdict comes from `scripts/check-outcomes.mjs`, which reads the slot's own
 * `economics.json` thresholds and its `store/outcomes.json` readings — so the number that
 * decides a slot's fate is the number the slot declared before it was built, not one chosen
 * after the fact. `MISSED` is the only failure: `NO-DATA` and `TOO-EARLY` are honest states, and
 * making them fail would teach the operator to silence the check, which is the failure mode
 * this replaces.
 */
function portfolioEarnings() {
  const script = join(root, 'scripts', 'check-outcomes.mjs')
  if (!existsSync(script)) {
    line('FAIL', 'scripts/check-outcomes.mjs is missing — the revenue plan has no sensor')
    return
  }
  const apps = Array.isArray(registry?.apps) ? registry.apps : []
  if (apps.length === 0) {
    line('INFO', 'no registered slots')
    return
  }
  for (const app of apps) {
    const killable = typeof app?.killReason === 'string' && app.killReason
    if (app.status === 'killed') {
      line('INFO', `${app.id}: killed ${app.killedAt || ''} — ${firstLine(killable)}`)
      continue
    }
    const result = run(process.execPath, [script, '--slot', app.id])
    const verdict = /VERDICT:\s*([A-Z-]+)/.exec(result.output)?.[1] || 'UNKNOWN'
    const detail = /(no readings|missing|elapsed|meets|window)[^\n]*/i.exec(result.output)
    if (verdict === 'ON-TRACK') {
      line('PASS', `${app.id}: ${verdict} — ${firstLine(detail)}`)
    } else if (verdict === 'MISSED') {
      line('FAIL', `${app.id}: ${verdict} — ${firstLine(detail)}`)
      console.log(`         node scripts/kiln-status.mjs --kill ${app.id} --reason "<why it did not earn>"`)
    } else {
      line('INFO', `${app.id}: ${verdict} — ${firstLine(detail)}`)
    }
  }
}

/**
 * Record a kill decision, or refuse to.
 *
 * The slot's own thresholds decide whether this is allowed: a slot whose latest reading meets
 * its declared `minInstalls` and `minRevenueUsd` is `ON-TRACK`, and killing it needs an explicit
 * `--force` rather than a moment of impatience. The registry is the durable record and is written
 * with the file's own line endings, so a kill does not reformat an unrelated file into a
 * repository-wide diff.
 */
function killSlot(slug, reason, force) {
  const apps = Array.isArray(registry?.apps) ? registry.apps : []
  const app = apps.find((entry) => entry?.id === slug || String(entry?.dir || '').endsWith(`-${slug}`))
  if (!app) {
    line('FAIL', `--kill ${slug}: no registry entry matches that id or directory`)
    return false
  }
  if (!reason || reason.trim().length < 10) {
    line(
      'FAIL',
      '--kill needs --reason "<what the numbers were and why the slot stops>"; a kill with no recorded reason is the silent abandonment this path replaces'
    )
    return false
  }
  const slotDir = join(root, app.dir || '')
  const outcome = existsSync(join(root, 'scripts', 'check-outcomes.mjs'))
    ? run(process.execPath, [join(root, 'scripts', 'check-outcomes.mjs'), '--slot', app.id])
    : null
  const verdict = outcome ? /VERDICT:\s*([A-Z-]+)/.exec(outcome.output)?.[1] || 'UNKNOWN' : 'UNKNOWN'
  if (verdict === 'ON-TRACK' && !force) {
    line(
      'FAIL',
      `${app.id} meets its own kill criteria (${verdict}) — pass --force only if the decision is about something the numbers do not measure`
    )
    return false
  }

  app.status = 'killed'
  app.killedAt = new Date().toISOString()
  app.killReason = reason.trim()
  app.killVerdict = verdict
  app.updatedAt = app.killedAt
  const registryPath = join(root, 'registry', 'apps.json')
  const original = readFileSync(registryPath, 'utf8')
  const eol = original.includes('\r\n') ? '\r\n' : '\n'
  writeFileSync(
    registryPath,
    JSON.stringify(registry, null, 2).split('\n').join(eol) + eol,
    'utf8'
  )
  line('PASS', `${app.id}: killed (${verdict}) — ${app.killReason}`)
  console.log(`         the slot directory is left in place so its evidence survives; archive it with:`)
  console.log(`         git mv ${app.dir} apps/_archive/${app.dir.split('/').pop()}`)
  if (slotDir && !existsSync(slotDir)) line('WARN', `the recorded directory ${app.dir} does not exist`)
  return true
}

section('Harness files')

const requiredHarnessFiles = [
  'AGENTS.md',
  'MISSION.md',
  'DESIGN.md',
  '.cursor/mcp.json',
  '.cursor/hooks.json',
  '.cursor/commands/forge.md', '.cursor/commands/ship.md',
  '.cursor/commands/release.md',
  '.cursor/commands/eval.md',
  '.cursor/rules/release-gate.mdc',
  '.cursor/agents/planner.md',
  '.cursor/agents/generator.md',
  '.cursor/agents/evaluator.md',
  '.cursorignore',
  'scripts/verify.mjs',
  'scripts/new-slot.mjs',
  'scripts/verify.mjs',
  'scripts/core/index.mjs',
  'scripts/core/facts.mjs',
  'scripts/lib/namespaces.mjs',
  'scripts/lib/gradle.mjs',
  'scripts/lib/java.mjs',
  'scripts/codex-run.mjs',
  'scripts/prepare-release.mjs',
  'scripts/approve-release.mjs',
  'scripts/lib/release-approval.mjs',
  'scripts/lib/pack-approval.mjs',
  'scripts/sign-release.mjs',
  'scripts/store/model.py',
  'scripts/store/render.py',
  'scripts/store/validate.py',
  'scripts/lib/play-assets.mjs',
  'scripts/check-in-app-policy.mjs',
  'scripts/lib/in-app-policy.mjs',
  'scripts/check-client-secrets.mjs',
  'scripts/lib/client-secrets.mjs',
  'scripts/check-outcomes.mjs',
  'scripts/lib/outcomes.mjs',
  'scripts/lib/slot-lock.mjs',
  'harness/ambition.md',
  'harness/cursor-surface.md',
  'harness/lessons.md',
  'docs/research/measured-facts.json',
  'docs/self-learning.md',
  'docs/monetization-owner.md',
  '.cursor/agents/money-owner.md',
  'docs/research/2026-09-20-android-play-ai-agents.md',
  'docs/research/2026-09-22-android-play-requirements.md',
  'docs/research/2026-09-23-google-play-automation-surface.md',
  'docs/play-publishing-playbook.md',
  'docs/monetization-strategy.md',
  '.cursor/rules/verification-discipline.mdc',
  '.cursor/rules/build-speed.mdc',
  '.cursor/rules/visual-identity.mdc',
  '.cursor/rules/client-secrets.mdc',
  '.cursor/rules/differentiator-contract.mdc',
  '.cursor/rules/authorization.mdc'
]
for (const relativePath of requiredHarnessFiles) {
  line(existsSync(join(root, relativePath)) ? 'PASS' : 'FAIL', relativePath)
}

section('Scaffold integrity')
const scaffoldFiles = [
  'scaffold/android-compose/gradlew',
  'scaffold/android-compose/gradlew.bat',
  'scaffold/android-compose/gradle/wrapper/gradle-wrapper.jar',
  'scaffold/android-compose/app/build.gradle.kts',
  'scaffold/android-compose/lint.xml',
  'scaffold/android-compose/keystore.properties.example',
  'scaffold/android-compose/.gitignore',
  'scaffold/android-compose/baselineprofile/build.gradle.kts'
]
for (const relativePath of scaffoldFiles) {
  line(existsSync(join(root, relativePath)) ? 'PASS' : 'FAIL', relativePath)
}

section('Scaffold correctness')
const rootBuild = readText('scaffold/android-compose/build.gradle.kts')
const appBuild = readText('scaffold/android-compose/app/build.gradle.kts')
const wrapper = readText('scaffold/android-compose/gradle/wrapper/gradle-wrapper.properties')

if (rootBuild === null) {
  line('FAIL', 'cannot probe scaffold root build file')
} else {
  line(
    !rootBuild.includes('org.jetbrains.kotlin.android') ? 'PASS' : 'FAIL',
    'root build omits org.jetbrains.kotlin.android'
  )
}

const appProbes = [
  [/\bcompileSdk\s*=\s*37\b/, 'app compileSdk = 37'],
  [/\btargetSdk\s*=\s*36\b/, 'app targetSdk = 36'],
  [/\bisMinifyEnabled\s*=\s*true\b/, 'release minification enabled'],
  [/\bisShrinkResources\s*=\s*true\b/, 'release resource shrinking enabled']
]
for (const [pattern, label] of appProbes) {
  line(appBuild !== null && pattern.test(appBuild) ? 'PASS' : 'FAIL', label)
}
line(
  wrapper !== null && /gradle-9\.7\.1-bin\.zip(?:\s|$)/.test(wrapper) ? 'PASS' : 'FAIL',
  'Gradle wrapper uses gradle-9.7.1-bin.zip'
)

// The scaffold's package must satisfy the same rule the verify gate enforces.
const scaffoldApplicationId = appBuild?.match(/\bapplicationId\s*=\s*["']([^"']+)["']/)?.[1]
line(
  Boolean(scaffoldApplicationId) && applicationIdPattern.test(scaffoldApplicationId)
    ? 'PASS'
    : 'FAIL',
  `scaffold applicationId satisfies the slot rule (found ${scaffoldApplicationId || '?'})`
)
line(
  existsSync(join(root, 'scaffold', 'android-compose', 'app', 'src', 'main', 'java', ...scaffoldPackage.split('.')))
    ? 'PASS'
    : 'FAIL',
  'scaffold package directory matches its namespace'
)

section('Environment')
const nodeResult = run(process.execPath, ['--version'])
line(nodeResult.ok ? 'PASS' : 'FAIL', `node ${firstLine(nodeResult.output) || 'unavailable'}`)

/**
 * The factory pins one JDK so Gradle reuses a single daemon. A build under a
 * different JDK starts a second daemon and reconfigures from scratch (measured:
 * 2.4s pinned vs 70.1s unpinned), so report the JDK the factory will actually use
 * rather than whatever happens to be on PATH.
 */
const resolvedJava = resolveJavaHome()
let javaHomeBin = null
if (!resolvedJava) {
  line(
    'FAIL',
    'no JDK 17+ found; set PLAYKILN_JAVA_HOME or JAVA_HOME (the factory pins a JDK to keep the Gradle daemon warm)'
  )
} else {
  javaHomeBin = join(resolvedJava.home, 'bin')
  line('PASS', `factory JDK ${resolvedJava.major} (${resolvedJava.source}): ${resolvedJava.home}`)
  const pathResult = run('java', ['-version'])
  const pathMajor = javaMajor(pathResult.output)
  if (pathMajor === null) {
    line('WARN', 'java on PATH is unavailable; factory runs are unaffected because the JDK is pinned')
  } else if (pathMajor !== resolvedJava.major) {
    line(
      'WARN',
      `java on PATH is JDK ${pathMajor} but the factory builds with JDK ${resolvedJava.major}; a build that skips the pin starts a second Gradle daemon and rebuilds cold`
    )
  } else {
    line('PASS', `java on PATH matches the pinned JDK ${pathMajor}`)
  }
}

const androidHome = process.env.ANDROID_HOME
if (!androidHome || !existsSync(androidHome)) {
  line('FAIL', 'ANDROID_HOME is missing or does not exist')
} else {
  line('PASS', `ANDROID_HOME: ${androidHome}`)
  line(existsSync(join(androidHome, 'platforms', 'android-37')) ? 'PASS' : 'FAIL', 'SDK platform android-37')

  const buildToolsRoot = join(androidHome, 'build-tools')
  let buildTools = []
  if (existsSync(buildToolsRoot)) {
    try {
      buildTools = readdirSync(buildToolsRoot)
        .filter((name) => statSync(join(buildToolsRoot, name)).isDirectory())
        .sort(compareVersions)
    } catch (error) {
      line('FAIL', `cannot inspect SDK build-tools: ${error.message}`)
    }
  }
  const qualifyingBuildTools = buildTools.filter((version) => compareVersions(version, '36.0.0') >= 0)
  line(
    qualifyingBuildTools.length > 0 ? 'PASS' : 'FAIL',
    qualifyingBuildTools.length > 0
      ? `SDK build-tools >= 36: ${qualifyingBuildTools.join(', ')}`
      : 'SDK build-tools >= 36 not found'
  )

  const adbNames = process.platform === 'win32' ? ['adb.exe', 'adb'] : ['adb']
  const adb = adbNames.map((name) => join(androidHome, 'platform-tools', name)).find(existsSync)
  line(adb ? 'PASS' : 'FAIL', 'platform-tools/adb')
}

const pythonResult = run('python', ['--version'])
line(pythonResult.ok ? 'PASS' : 'FAIL', `python ${firstLine(pythonResult.output) || 'unavailable'}`)
const pillowResult = pythonResult.ok ? run('python', ['-c', 'import PIL; print(PIL.__version__)']) : null
line(
  pillowResult?.ok ? 'PASS' : 'FAIL',
  pillowResult?.ok ? `Pillow ${firstLine(pillowResult.output)}` : 'Pillow unavailable to python'
)

let keytoolResult = run('keytool', ['-help'])
let keytoolSource = 'PATH'
if (!keytoolResult.ok && javaHomeBin) {
  const executable = join(javaHomeBin, process.platform === 'win32' ? 'keytool.exe' : 'keytool')
  if (existsSync(executable)) {
    keytoolResult = run(executable, ['-help'])
    keytoolSource = 'JAVA_HOME/bin'
  }
}
line(keytoolResult.ok ? 'PASS' : 'FAIL', `keytool ${keytoolResult.ok ? `via ${keytoolSource}` : 'not discoverable'}`)

section('Codex channel')
const launcherPath = join(root, '.cursor', 'mcp', 'codex-launcher.cjs')
if (!existsSync(launcherPath)) {
  line('WARN', '.cursor/mcp/codex-launcher.cjs is missing')
} else {
  const codexResult = run(process.execPath, [launcherPath, '--version'])
  line(
    codexResult.ok ? 'PASS' : 'WARN',
    codexResult.ok
      ? `Codex CLI ${firstLine(codexResult.output)}`
      : `Codex CLI unresolved: ${firstLine(codexResult.output) || codexResult.error?.message || 'unknown error'}`
  )
}

const workerDirectory = join(root, '.kiln', 'codex')
let workerCount = 0
if (existsSync(workerDirectory)) {
  try {
    workerCount = readdirSync(workerDirectory).filter((name) => name.endsWith('.meta.json')).length
  } catch (error) {
    line('WARN', `cannot count Codex worker records: ${error.message}`)
  }
}
line('INFO', `Codex worker records: ${workerCount}`)

section('MCP config')
let mcp = null
const mcpText = readText('.cursor/mcp.json')
if (mcpText !== null) {
  try {
    mcp = JSON.parse(mcpText)
  } catch (error) {
    line('FAIL', `.cursor/mcp.json is invalid JSON: ${error.message}`)
  }
}
const requiredServers = ['context7', 'playwright', 'codex', 'android-skills', 'droidagentkit']
for (const name of requiredServers) {
  line(mcp?.mcpServers?.[name] ? 'PASS' : 'FAIL', `mcp:${name}`)
}
const optionalServers = ['firebase']
for (const name of optionalServers) {
  line('INFO', `mcp:${name} (optional): ${mcp?.mcpServers?.[name] ? 'configured' : 'not configured'}`)
}

section('Registry')
let registry = null
const registryText = readText('registry/apps.json')
if (registryText === null) {
  line('FAIL', 'registry/apps.json is missing')
} else {
  try {
    registry = JSON.parse(registryText)
    if (!Array.isArray(registry.apps)) throw new Error('apps must be an array')
  } catch (error) {
    registry = null
    line('FAIL', `registry/apps.json is invalid: ${error.message}`)
  }
}
if (registry) {
  const counts = new Map()
  for (const app of registry.apps) {
    const status = typeof app?.status === 'string' && app.status ? app.status : 'unknown'
    counts.set(status, (counts.get(status) || 0) + 1)
  }
  const summary = [...counts.entries()]
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([status, count]) => `${status}=${count}`)
    .join(', ')
  line('INFO', `registry apps: ${registry.apps.length}; statuses: ${summary || 'none'}`)
}

/**
 * A kill is an action, not a report: it runs before the status report so it cannot be half-done
 * by a later failing check, and it exits with its own verdict.
 */
if (KILL_SLOT) {
  section('Kill decision')
  process.exitCode = killSlot(KILL_SLOT, KILL_REASON, KILL_FORCE) ? 0 : 1
  console.log(process.exitCode === 0 ? '\nVERDICT: KILLED' : '\nVERDICT: NOT KILLED')
  process.exit(process.exitCode)
}

section('Portfolio earnings')
portfolioEarnings()

/**
 * Which slots are still in development and which the owner has released.
 *
 * The phase is derived from the approval record, not from a field someone remembers to
 * update: a slot is "released" only while its `.kiln-state.json` and its sources still hash
 * to what the owner approved. An approval that a later edit voided reports as development
 * again, which is the honest answer.
 */
section('Slot release state')
const appsRoot = join(root, 'apps')
let slotNames = []
if (existsSync(appsRoot)) {
  try {
    slotNames = readdirSync(appsRoot)
      .filter((name) => statSync(join(appsRoot, name)).isDirectory())
      .sort()
  } catch (error) {
    line('WARN', `cannot inspect apps/: ${error.message}`)
  }
}
if (slotNames.length === 0) {
  line('INFO', 'no slots under apps/')
}
for (const name of slotNames) {
  const slotDir = join(appsRoot, name)
  if (!existsSync(join(slotDir, '.kiln-state.json'))) {
    line('INFO', `${name}: not a scaffolded slot (.kiln-state.json absent)`)
    continue
  }
  const approval = checkReleaseApproval(slotDir)
  if (approval.ok) {
    line(
      'PASS',
      `${name}: release approved by the owner at ${approval.record.approvedAt} over ${approval.record.sourceFiles} build input(s)`
    )
  } else {
    line(
      'INFO',
      `${name}: in development — release gated until the owner approves: node scripts/approve-release.mjs --slot ${slotSlug(slotDir)} --note "<the owner's words>"`
    )
  }
}

section('Factory checks')
// The self-test was retired with the meta-runner chain: one entry point runs every check in one
// process, so the health summary asks that entry point instead of spawning a runner of runners.
const verifyFile = join(root, 'scripts', 'verify.mjs')
if (!existsSync(verifyFile)) {
  line('FAIL', 'scripts/verify.mjs is missing')
} else {
  const verify = spawnSync(process.execPath, [verifyFile], {
    cwd: root,
    encoding: 'utf8',
    shell: false,
    timeout: 300_000,
    windowsHide: true
  })
  const verifyOutput = `${verify.stdout || ''}${verify.stderr || ''}`
  const verifyRows = verifyOutput.split(/\r?\n/).map((item) => item.replace(/^\uFEFF/, '').trim())
  const passes = verifyRows.filter((item) => item.startsWith('PASS ')).length
  if (verify.status === 0) {
    line('PASS', `the factory's checks hold (${passes} row(s) PASS)`)
  } else {
    // Exit 2 is not a pass either: an UNVERIFIED row is a check that could not decide.
    line('FAIL', `factory checks did not pass (exit ${verify.status}) — run \`node scripts/verify.mjs\` for detail`)
    const interesting = verifyRows.filter((item) => item.startsWith('FAIL ') || item.startsWith('UNVERIFIED '))
    for (const row of interesting.slice(0, 6)) console.log(`         ${row}`)
  }
}

console.log(`\nVERDICT: ${hasFailure ? 'NOT READY' : 'READY'}`)
process.exitCode = hasFailure ? 1 : 0
