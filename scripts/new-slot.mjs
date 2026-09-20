#!/usr/bin/env node
/**
 * Create an isolated Android Compose app slot under apps/.
 *
 * Usage:
 *   node scripts/new-slot.mjs --slug my-calendar --name "My Calendar" --idea "offline calendar"
 */

import {
  cpSync,
  existsSync,
  mkdirSync,
  readFileSync,
  writeFileSync,
  readdirSync,
  statSync,
  rmSync
} from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'

const here = dirname(fileURLToPath(import.meta.url))
const root = join(here, '..')
const scaffold = join(root, 'scaffold', 'android-compose')
const appsDir = join(root, 'apps')
const registryPath = join(root, 'registry', 'apps.json')

function arg(name, fallback = null) {
  const idx = process.argv.indexOf(name)
  if (idx === -1) return fallback
  return process.argv[idx + 1] || fallback
}

function today() {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function slugify(s) {
  return String(s)
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 48)
}

function walk(dir, out = []) {
  for (const name of readdirSync(dir)) {
    const p = join(dir, name)
    if (statSync(p).isDirectory()) walk(p, out)
    else out.push(p)
  }
  return out
}

function replaceInFile(file, map) {
  let text = readFileSync(file, 'utf8')
  let changed = false
  for (const [from, to] of Object.entries(map)) {
    if (text.includes(from)) {
      text = text.split(from).join(to)
      changed = true
    }
  }
  if (changed) writeFileSync(file, text)
}

const rawSlug = arg('--slug')
const name = arg('--name', 'Untitled App')
const idea = arg('--idea', '')
if (!rawSlug) {
  console.error('Usage: node scripts/new-slot.mjs --slug <slug> --name "<Title>" --idea "<one line>"')
  process.exit(1)
}

const slug = slugify(rawSlug)
if (!slug) {
  console.error('Invalid --slug')
  process.exit(1)
}

const applicationId = `com.playkiln.${slug.replace(/-/g, '')}`
const dirName = `${today()}-${slug}`
const dest = join(appsDir, dirName)

if (!existsSync(scaffold)) {
  console.error('Missing scaffold at', scaffold)
  process.exit(1)
}
if (existsSync(dest)) {
  console.error('Slot already exists:', dest)
  process.exit(1)
}

mkdirSync(appsDir, { recursive: true })
cpSync(scaffold, dest, { recursive: true })

const map = {
  __APP_NAME__: name,
  __APP_SLUG__: slug,
  __APPLICATION_ID__: applicationId,
  __PACKAGE_PATH__: applicationId.replace(/\./g, '/'),
  __IDEA__: idea || name,
  'com.playkiln.scaffold': applicationId
}

for (const file of walk(dest)) {
  if (/\.(kt|kts|xml|md|toml|properties|example|json|pro)$/.test(file)) {
    replaceInFile(file, map)
  }
}

const srcPkg = join(dest, 'app', 'src', 'main', 'java', 'com', 'playkiln', 'scaffold')
const dstPkg = join(dest, 'app', 'src', 'main', 'java', ...applicationId.split('.'))
if (existsSync(srcPkg)) {
  mkdirSync(dirname(dstPkg), { recursive: true })
  cpSync(srcPkg, dstPkg, { recursive: true })
  rmSync(srcPkg, { recursive: true, force: true })
}

// Best-effort Gradle wrapper if system Gradle exists
const gradle = spawnSync('gradle', ['wrapper', '--gradle-version', '8.11.1'], {
  cwd: dest,
  encoding: 'utf8',
  shell: true
})
if (gradle.status === 0) {
  console.log('Gradle wrapper generated.')
} else {
  console.log(
    'Note: system Gradle not available — add wrapper later with: gradle wrapper --gradle-version 8.11.1'
  )
}

let registry = { version: 1, apps: [] }
if (existsSync(registryPath)) {
  registry = JSON.parse(readFileSync(registryPath, 'utf8'))
}
registry.apps.push({
  id: slug,
  dir: `apps/${dirName}`,
  name,
  applicationId,
  idea,
  status: 'building',
  createdAt: new Date().toISOString()
})
mkdirSync(dirname(registryPath), { recursive: true })
writeFileSync(registryPath, JSON.stringify(registry, null, 2) + '\n')

console.log('Created slot:', dest)
console.log('applicationId:', applicationId)
console.log('registry: updated', registryPath)
