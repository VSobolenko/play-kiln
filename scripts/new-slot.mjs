#!/usr/bin/env node
/**
 * Create an isolated Android Compose app slot under apps/.
 *
 * Usage:
 *   node scripts/new-slot.mjs --slug <slug> --name "<Title>" --idea "<one line>" --economics <plan.json> --differentiators <contract.json> [--date YYYY-MM-DD] [--package com.gexetr.<slug>] [--force]
 *
 * A slot cannot be scaffolded without a validated `economics.json` plan: the first
 * slot shipped with no revenue mechanism and every gate still returned PASS, so the
 * plan is now a precondition rather than a convention.
 *
 * It cannot be scaffolded without a `differentiators.json` contract either, and for the
 * same reason: recon states why the app can win, and a reason list written after the
 * build only contains what was built. Both files are written before the first line of
 * UI, and `scripts/check-differentiators.mjs` refuses to call the slot done while any
 * promise in it is still deferred.
 */

import {
  cpSync,
  existsSync,
  mkdirSync,
  readFileSync,
  readdirSync,
  renameSync,
  rmSync,
  writeFileSync
} from 'node:fs'
import { basename, dirname, extname, join, relative, resolve, sep } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import {
  applicationIdForSlug,
  applicationIdPattern,
  scaffoldPackage
} from './lib/namespaces.mjs'
import { parseEconomics } from './lib/slot-economics.mjs'
import { parseDifferentiators } from './lib/slot-differentiators.mjs'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const scaffold = join(root, 'scaffold', 'android-compose')
const appsDir = join(root, 'apps')
const registryPath = join(root, 'registry', 'apps.json')
const scaffoldVersion = 'android-compose-v1'
const textExtensions = new Set([
  '.kt',
  '.kts',
  '.xml',
  '.md',
  '.toml',
  '.properties',
  '.example',
  '.pro',
  '.json',
  '.txt',
  '.gitignore'
])

// A slot must be created from the scaffold *sources*, never from its build state.
// Copying `build/` drags ~145 MB of binary artifacts into every new slot and makes
// token substitution impossible (see shouldCopyFromScaffold / verifyScaffoldCopy).
const excludedDirectories = new Set([
  'build',
  '.gradle',
  '.kotlin',
  '.idea',
  '.git',
  'captures',
  '.cxx',
  'dist',
  // A Python bytecode cache is a build output: `scripts/__pycache__/*.pyc` is machine-local, binary
  // and gitignored. It is listed here rather than in one caller's filter because the predicate is the
  // single answer to "what must not be copied", and the `build.copyFilter` check
  // (`scripts/core/build.mjs`) sweeps the repository through it and fails a recursive copy that
  // decides for itself what must not be copied.
  '__pycache__'
])
const excludedFiles = new Set(['local.properties', 'keystore.properties', '.DS_Store'])
const excludedSuffixes = ['.apk', '.aab', '.ap_', '.jks', '.keystore', '.iml']
// `scripts/codex-run.mjs` writes a run's stdout beside the durable ledger under `.kiln/`, and a
// worker started with the slot as its working directory leaves that output in the slot root instead.
// Two such logs sat in a slot root and were committed by hand, because `.gitignore` covered `dist/`
// and `build/` but not this. Both the scaffold and the slot ignore them now, and the copy filter
// refuses them too, so a slot can neither receive one from the scaffold nor be reported clean while
// carrying one.
const workerLogSegment = /^\.kiln-.*\.log$/i
const usage =
  'node scripts/new-slot.mjs --slug <slug> --name "<Title>" --idea "<one line>" --economics <plan.json> --differentiators <contract.json> [--date YYYY-MM-DD] [--package com.gexetr.<slug>] [--force]'

function line(status, message) {
  console.log(`${status.padEnd(8)} ${message}`)
}

function parseArgs(argv) {
  const values = new Set(['--slug', '--name', '--idea', '--date', '--package', '--economics', '--differentiators'])
  const flags = new Set(['--force'])
  const parsed = { force: false }

  for (let index = 0; index < argv.length; index += 1) {
    const option = argv[index]
    if (flags.has(option)) {
      parsed.force = true
      continue
    }
    if (!values.has(option)) throw new Error(`Unknown option: ${option}\nUsage: ${usage}`)
    const value = argv[index + 1]
    if (!value || value.startsWith('--')) throw new Error(`Missing value for ${option}\nUsage: ${usage}`)
    parsed[option.slice(2)] = value
    index += 1
  }
  return parsed
}

function localDate() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function validateDate(value) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false
  const [year, month, day] = value.split('-').map(Number)
  const date = new Date(Date.UTC(year, month - 1, day))
  return (
    date.getUTCFullYear() === year &&
    date.getUTCMonth() === month - 1 &&
    date.getUTCDate() === day
  )
}

function walkFiles(directory, output = []) {
  for (const entry of readdirSync(directory, { withFileTypes: true })) {
    const path = join(directory, entry.name)
    if (entry.isDirectory()) walkFiles(path, output)
    else if (entry.isFile()) output.push(path)
  }
  return output
}

function isTextFile(file) {
  const name = basename(file).toLowerCase()
  return textExtensions.has(name) || textExtensions.has(extname(name))
}

/**
 * Decide whether a scaffold path belongs in a fresh slot.
 *
 * Gradle build outputs, IDE state, and machine-local files are excluded: they are
 * large, binary, and machine-specific. Every path segment is checked so that a
 * file nested under an excluded directory (for example `app/build/foo.txt`) is
 * caught too. The Gradle wrapper JAR under `gradle/wrapper/` is intentionally
 * *not* excluded — without it the slot cannot build, so verifyScaffoldCopy asserts
 * that it survived.
 */
function isExcludedScaffoldPath(relativePath) {
  const segments = relativePath.split(/[\\/]+/).filter(Boolean)
  return segments.some((segment, index) => {
    if (excludedDirectories.has(segment)) return true
    if (workerLogSegment.test(segment)) return true
    if (index !== segments.length - 1) return false
    if (excludedFiles.has(segment)) return true
    const lower = segment.toLowerCase()
    return excludedSuffixes.some((suffix) => lower.endsWith(suffix))
  })
}

function shouldCopyFromScaffold(source) {
  return !isExcludedScaffoldPath(relative(scaffold, source))
}

function findExcludedArtifacts(slot) {
  return walkFiles(slot).filter((file) => isExcludedScaffoldPath(relative(slot, file)))
}

function replaceText(file, replacements) {
  let text = readFileSync(file, 'utf8')
  let changed = false
  for (const [from, to] of replacements) {
    if (!text.includes(from)) continue
    text = text.split(from).join(to)
    changed = true
  }
  if (changed) writeFileSync(file, text, 'utf8')
}

function movePackageDirectories(slot, applicationId) {
  const sourceRoots = [
    join(slot, 'app', 'src', 'main', 'java'),
    join(slot, 'app', 'src', 'test', 'java'),
    join(slot, 'app', 'src', 'androidTest', 'java'),
    join(slot, 'baselineprofile', 'src', 'main', 'java')
  ]

  for (const sourceRoot of sourceRoots) {
    const from = join(sourceRoot, ...scaffoldPackage.split('.'))
    if (!existsSync(from)) continue
    const to = join(sourceRoot, ...applicationId.split('.'))
    mkdirSync(dirname(to), { recursive: true })
    renameSync(from, to)
  }
}

function verifySubstitution(slot) {
  const files = walkFiles(slot)
  const tokenFiles = []
  const packageFiles = []

  for (const file of files) {
    if (!isTextFile(file)) continue
    const text = readFileSync(file, 'utf8')
    if (/__[A-Z_]+__/.test(text)) tokenFiles.push(file)
    if (text.includes(scaffoldPackage)) packageFiles.push(file)
  }

  line(tokenFiles.length === 0 ? 'PASS' : 'FAIL', 'no residual __TOKEN__ placeholders')
  line(packageFiles.length === 0 ? 'PASS' : 'FAIL', `no residual ${scaffoldPackage} references`)
  if (tokenFiles.length > 0 || packageFiles.length > 0) {
    const affected = [...new Set([...tokenFiles, ...packageFiles])]
      .map((file) => relative(slot, file))
      .join(', ')
    throw new Error(`Substitution verification failed in: ${affected}`)
  }
}

/**
 * Guard against the scaffold leaking its build state into a slot. Regression test
 * for the defect where `cpSync` had no filter and copied ~145 MB of `build/`.
 */
function verifyScaffoldCopy(slot) {
  const artifacts = findExcludedArtifacts(slot)
  line(artifacts.length === 0 ? 'PASS' : 'FAIL', 'no build outputs or machine-local files copied')
  if (artifacts.length > 0) {
    const shown = artifacts.slice(0, 5).map((file) => relative(slot, file)).join(', ')
    const more = artifacts.length > 5 ? ` (+${artifacts.length - 5} more)` : ''
    throw new Error(`Build state leaked into the slot: ${shown}${more}`)
  }

  const wrapper = join(slot, 'gradle', 'wrapper', 'gradle-wrapper.jar')
  const wrapperProperties = join(slot, 'gradle', 'wrapper', 'gradle-wrapper.properties')
  const wrapperOk = existsSync(wrapper) && existsSync(wrapperProperties)
  line(wrapperOk ? 'PASS' : 'FAIL', 'Gradle wrapper jar + properties present')
  if (!wrapperOk) throw new Error('Gradle wrapper is incomplete; the slot could not build')
}

function readRegistry() {
  if (!existsSync(registryPath)) return { version: 1, apps: [] }
  const registry = JSON.parse(readFileSync(registryPath, 'utf8').replace(/^\uFEFF/, ''))
  if (!registry || typeof registry !== 'object' || !Array.isArray(registry.apps)) {
    throw new Error('registry/apps.json must contain an apps array')
  }
  return registry
}

/**
 * Refuse an applicationId that another slot already owns.
 *
 * An applicationId is permanent after the first Play upload, and
 * `applicationIdForSlug` drops dashes, so distinct slugs can collide:
 * `my-app` and `myapp` both derive `com.gexetr.myapp`. Without this check the
 * second slot would be scaffolded against an ID the account can never publish,
 * and the collision would only surface at upload time.
 */
function assertApplicationIdIsFree(applicationId, slug) {
  const registry = readRegistry()
  const owner = registry.apps.find((app) => app?.applicationId === applicationId && app?.id !== slug)
  if (owner) {
    throw new Error(
      `applicationId ${applicationId} is already owned by slot '${owner.id}' ` +
        `(${owner.dir}); an applicationId is permanent after the first Play upload, ` +
        `so '${slug}' needs a different slug`
    )
  }
}

function registerSlot({ slug, dirName, name, applicationId, idea, createdAt, archetype, promises }) {
  const registry = readRegistry()
  const index = registry.apps.findIndex((app) => app?.id === slug)
  const previous = index === -1 ? null : registry.apps[index]
  const entry = {
    id: slug,
    dir: `apps/${dirName}`,
    name,
    applicationId,
    idea,
    archetype,
    promises,
    status: 'scaffolded',
    createdAt: previous?.createdAt || createdAt,
    updatedAt: createdAt
  }

  if (index === -1) registry.apps.push(entry)
  else registry.apps[index] = entry

  mkdirSync(dirname(registryPath), { recursive: true })
  writeFileSync(registryPath, `${JSON.stringify(registry, null, 2)}\n`, 'utf8')
}

/**
 * Load and validate the required economics plan before anything is written.
 *
 * The first slot shipped with `capabilities.json = {"schemaVersion":1,"enabled":[]}`
 * — no revenue mechanism — and every gate still returned PASS. Refusing here, before
 * the scaffold is copied, is what makes a planless slot impossible to create rather
 * than merely discouraged.
 */
function loadEconomicsPlan(file) {
  const absolute = resolve(file)
  if (!existsSync(absolute)) throw new Error(`Economics plan not found: ${absolute}`)
  let raw
  try {
    raw = readFileSync(absolute, 'utf8')
  } catch (error) {
    throw new Error(`Economics plan could not be read: ${absolute} (${error.message})`)
  }
  let parsed
  try {
    parsed = JSON.parse(raw.replace(/^\uFEFF/, ''))
  } catch (error) {
    // Name the file: a JSON typo in a plan passed from the command line otherwise
    // reports only the parser's position with no clue which file it came from.
    throw new Error(`Economics plan is not valid JSON: ${absolute} (${error.message})`)
  }
  const result = parseEconomics(parsed)
  if (!result.ok) {
    for (const problem of result.errors) line('BLOCKED', problem)
    throw new Error(`Economics plan is invalid: ${absolute}`)
  }
  line('PASS', `economics plan valid: archetype ${result.value.archetype}`)
  return result.value
}

/**
 * Load and validate the required differentiator contract before anything is written.
 *
 * Every promise in a fresh contract is `deferred`, which is the honest state: nothing
 * has been built. `scripts/check-differentiators.mjs` accepts that at scaffold time and
 * refuses it at slot time, so the rows this function validates are the rows the builder
 * has to answer for before the app can be called done.
 */
function loadDifferentiatorsPlan(file) {
  const absolute = resolve(file)
  if (!existsSync(absolute)) throw new Error(`Differentiators contract not found: ${absolute}`)
  let raw
  try {
    raw = readFileSync(absolute, 'utf8')
  } catch (error) {
    throw new Error(`Differentiators contract could not be read: ${absolute} (${error.message})`)
  }
  let parsed
  try {
    parsed = JSON.parse(raw.replace(/^\uFEFF/, ''))
  } catch (error) {
    throw new Error(`Differentiators contract is not valid JSON: ${absolute} (${error.message})`)
  }
  const result = parseDifferentiators(parsed)
  if (!result.ok) {
    for (const problem of result.errors) line('BLOCKED', problem)
    throw new Error(`Differentiators contract is invalid: ${absolute}`)
  }
  const claims = result.value.claims.length
  const core = result.value.claims.filter((claim) => claim.weight === 'core').length
  line('PASS', `differentiators contract valid: ${claims} promise(s), ${core} core`)
  return result.value
}

function main() {
  const options = parseArgs(process.argv.slice(2))
  const slug = options.slug
  const name = options.name?.trim()
  const idea = options.idea?.trim()
  const date = options.date || localDate()

  if (!slug) throw new Error(`Missing required --slug\nUsage: ${usage}`)
  if (slug.length < 2 || slug.length > 40 || !/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(slug)) {
    throw new Error('--slug must be lowercase kebab case and 2-40 characters long')
  }
  if (!name) throw new Error(`Missing required --name\nUsage: ${usage}`)
  if (!idea) throw new Error(`Missing required --idea\nUsage: ${usage}`)
  if (/\r|\n/.test(idea)) throw new Error('--idea must be one line')
  if (!validateDate(date)) throw new Error('--date must be a real date in YYYY-MM-DD format')

  const applicationId = options.package || applicationIdForSlug(slug)
  if (!applicationIdPattern.test(applicationId)) {
    throw new Error(`--package must match ${applicationIdPattern}`)
  }
  assertApplicationIdIsFree(applicationId, slug)
  if (!existsSync(scaffold)) throw new Error(`Missing scaffold: ${scaffold}`)

  if (!options.economics) throw new Error(`Missing required --economics <path>\nUsage: ${usage}`)
  const economics = loadEconomicsPlan(options.economics)

  if (!options.differentiators) {
    throw new Error(`Missing required --differentiators <path>\nUsage: ${usage}`)
  }
  const differentiators = loadDifferentiatorsPlan(options.differentiators)

  const dirName = `${date}-${slug}`
  const destination = resolve(appsDir, dirName)
  const resolvedApps = resolve(appsDir)
  if (!destination.startsWith(`${resolvedApps}${sep}`)) throw new Error('Refusing unsafe slot destination')

  if (existsSync(destination) && !options.force) {
    throw new Error(`Slot already exists: ${destination} (use --force to replace it)`)
  }
  if (existsSync(destination)) {
    rmSync(destination, { recursive: true, force: true })
    line('WARN', `replacing existing slot apps/${dirName}`)
  }

  mkdirSync(appsDir, { recursive: true })
  cpSync(scaffold, destination, { recursive: true, filter: shouldCopyFromScaffold })
  line('PASS', `copied scaffold sources to apps/${dirName}`)

  const replacements = [
    ['__APP_NAME__', name],
    ['__APP_SLUG__', slug],
    ['__APPLICATION_ID__', applicationId],
    ['__IDEA__', idea],
    [scaffoldPackage, applicationId]
  ]
  for (const file of walkFiles(destination)) {
    if (isTextFile(file)) replaceText(file, replacements)
  }
  movePackageDirectories(destination, applicationId)
  verifySubstitution(destination)
  verifyScaffoldCopy(destination)

  mkdirSync(join(destination, 'dist'), { recursive: true })
  writeFileSync(
    join(destination, 'economics.json'),
    `${JSON.stringify(economics, null, 2)}\n`,
    'utf8'
  )
  // Written before the UI exists, like the economics plan. The contract's `slot` field
  // is forced to this slot rather than trusted from the file: a contract copied from
  // another slot would otherwise pass schema validation and claim the wrong recon.
  const contract = { ...differentiators, slot: slug }
  writeFileSync(
    join(destination, 'differentiators.json'),
    `${JSON.stringify(contract, null, 2)}\n`,
    'utf8'
  )
  const createdAt = new Date().toISOString()
  const state = {
    slug,
    applicationId,
    appName: name,
    idea,
    createdAt,
    scaffoldVersion,
    status: 'scaffolded',
    economics: {
      archetype: economics.archetype,
      priceUsd: economics.priceUsd
    },
    differentiators: {
      promises: contract.claims.length,
      core: contract.claims.filter((claim) => claim.weight === 'core').length
    }
  }
  writeFileSync(join(destination, '.kiln-state.json'), `${JSON.stringify(state, null, 2)}\n`, 'utf8')
  line('PASS', 'created dist/, economics.json, differentiators.json, and .kiln-state.json')

  registerSlot({
    slug,
    dirName,
    name,
    applicationId,
    idea,
    createdAt,
    archetype: economics.archetype,
    promises: contract.claims.length
  })
  line('PASS', `registered ${slug} in registry/apps.json`)
  line('INFO', `applicationId: ${applicationId}`)

  console.log('\nNext:')
  line('INFO', 'implement the app')
  line('INFO', `python scripts/store/render.py --slot ${slug}`)
  line('INFO', `node scripts/check-economics.mjs --slot ${slug}`)
  line('INFO', `node scripts/check-differentiators.mjs --slot ${slug} --allow-deferred`)
  line('INFO', 'every promise in differentiators.json starts deferred; the slot is not done until each one is shipped, narrowed or dropped')
  line(
    'INFO',
    'signing requires the owner\'s explicit authorization; never run sign-release.mjs --generate without it'
  )
  line('INFO', `node scripts/prepare-release.mjs --slot ${slug}`)
  line('INFO', `node scripts/verify.mjs --slot ${slug}`)
}

function runCli() {
  try {
    main()
    console.log('\nVERDICT: PASS')
  } catch (error) {
    line('FAIL', error instanceof Error ? error.message : String(error))
    console.log('\nVERDICT: FAIL')
    process.exitCode = 1
  }
}

// Exported so the `build.copyFilter` check (`scripts/core/build.mjs`) can reuse the exact exclusion
// rules instead of re-implementing (and drifting from) them. That check is now the only consumer: the
// sandbox run of a real slot that `scripts/selftest.mjs` used to drive went with the runner, and
// `openspec/changes/reduce-factory-to-core/deletions.md` records it as an assertion with no successor.
export { isExcludedScaffoldPath, parseArgs, walkFiles }

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) runCli()
