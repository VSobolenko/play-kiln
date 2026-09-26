#!/usr/bin/env node
/**
 * Report whether the PlayKiln factory has the files and local tools needed to build and ship.
 * Usage: node scripts/kiln-status.mjs
 */

import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs'
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

section('Harness files')
const requiredHarnessFiles = [
  'AGENTS.md',
  'MISSION.md',
  'DESIGN.md',
  '.cursor/mcp.json',
  '.cursor/hooks.json',
  '.cursor/commands/ship.md',
  '.cursor/commands/release.md',
  '.cursor/commands/eval.md',
  '.cursor/rules/release-gate.mdc',
  '.cursor/agents/planner.md',
  '.cursor/agents/generator.md',
  '.cursor/agents/evaluator.md',
  '.cursorignore',
  'scripts/verify-slot.mjs',
  'scripts/new-slot.mjs',
  'scripts/selftest.mjs',
  'scripts/lib/namespaces.mjs',
  'scripts/lib/gradle.mjs',
  'scripts/lib/java.mjs',
  'scripts/codex-run.mjs',
  'scripts/prepare-release.mjs',
  'scripts/approve-release.mjs',
  'scripts/lib/release-approval.mjs',
  'scripts/sign-release.mjs',
  'scripts/store-kit.py',
  'scripts/lib/play-assets.mjs',
  'harness/ambition.md',
  'harness/cursor-surface.md',
  'harness/lessons.md',
  'docs/research/2026-09-20-android-play-ai-agents.md',
  'docs/research/2026-09-22-android-play-requirements.md',
  'docs/research/2026-09-23-google-play-automation-surface.md',
  'docs/play-publishing-playbook.md',
  'docs/monetization-strategy.md',
  '.cursor/rules/verification-discipline.mdc',
  '.cursor/rules/build-speed.mdc',
  '.cursor/rules/visual-identity.mdc'
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

section('Factory self-test')
const selftestFile = join(root, 'scripts', 'selftest.mjs')
if (!existsSync(selftestFile)) {
  line('FAIL', 'scripts/selftest.mjs is missing')
} else {
  const selftest = spawnSync(process.execPath, [selftestFile], {
    cwd: root,
    encoding: 'utf8',
    shell: false,
    timeout: 120_000,
    windowsHide: true
  })
  const selftestOutput = `${selftest.stdout || ''}${selftest.stderr || ''}`
  if (selftest.status === 0) {
    const passes = selftestOutput.split(/\r?\n/).filter((item) => item.trim().startsWith('PASS')).length
    line('PASS', `factory scripts honour their contract (${passes} checks)`)
  } else {
    line('FAIL', 'factory self-test failed — run `node scripts/selftest.mjs` for detail')
    for (const failure of selftestOutput.split(/\r?\n/).filter((item) => item.trim().startsWith('FAIL')).slice(0, 6)) {
      console.log(`         ${failure.trim()}`)
    }
  }
}

console.log(`\nVERDICT: ${hasFailure ? 'NOT READY' : 'READY'}`)
process.exitCode = hasFailure ? 1 : 0
