#!/usr/bin/env node
/**
 * Create an isolated Android Compose app slot under apps/.
 *
 * Usage:
 *   node scripts/new-slot.mjs --slug <slug> --name "<Title>" --idea "<one line>" [--date YYYY-MM-DD] [--package com.gexetr.<slug>] [--force]
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
  'dist'
])
const excludedFiles = new Set(['local.properties', 'keystore.properties', '.DS_Store'])
const excludedSuffixes = ['.apk', '.aab', '.ap_', '.jks', '.keystore', '.iml']
const usage =
  'node scripts/new-slot.mjs --slug <slug> --name "<Title>" --idea "<one line>" [--date YYYY-MM-DD] [--package com.gexetr.<slug>] [--force]'

function line(status, message) {
  console.log(`${status.padEnd(8)} ${message}`)
}

function parseArgs(argv) {
  const values = new Set(['--slug', '--name', '--idea', '--date', '--package'])
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

function registerSlot({ slug, dirName, name, applicationId, idea, createdAt }) {
  const registry = readRegistry()
  const index = registry.apps.findIndex((app) => app?.id === slug)
  const previous = index === -1 ? null : registry.apps[index]
  const entry = {
    id: slug,
    dir: `apps/${dirName}`,
    name,
    applicationId,
    idea,
    status: 'scaffolded',
    createdAt: previous?.createdAt || createdAt,
    updatedAt: createdAt
  }

  if (index === -1) registry.apps.push(entry)
  else registry.apps[index] = entry

  mkdirSync(dirname(registryPath), { recursive: true })
  writeFileSync(registryPath, `${JSON.stringify(registry, null, 2)}\n`, 'utf8')
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
  if (!existsSync(scaffold)) throw new Error(`Missing scaffold: ${scaffold}`)

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
  const createdAt = new Date().toISOString()
  const state = {
    slug,
    applicationId,
    appName: name,
    idea,
    createdAt,
    scaffoldVersion,
    status: 'scaffolded'
  }
  writeFileSync(join(destination, '.kiln-state.json'), `${JSON.stringify(state, null, 2)}\n`, 'utf8')
  line('PASS', 'created dist/ and .kiln-state.json')

  registerSlot({ slug, dirName, name, applicationId, idea, createdAt })
  line('PASS', `registered ${slug} in registry/apps.json`)
  line('INFO', `applicationId: ${applicationId}`)

  console.log('\nNext:')
  line('INFO', 'implement the app')
  line('INFO', `python scripts/store-kit.py --slot ${slug}`)
  line('INFO', `node scripts/sign-release.mjs --slot ${slug} --generate`)
  line('INFO', `node scripts/prepare-release.mjs --slot ${slug}`)
  line('INFO', `node scripts/verify-slot.mjs --slot ${slug} --strict-store`)
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

// Exported so scripts/selftest.mjs can exercise a real slot in a sandbox and reuse
// the exact exclusion rules instead of re-implementing (and drifting from) them.
export { isExcludedScaffoldPath, parseArgs, walkFiles }

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) runCli()
