#!/usr/bin/env node
/**
 * Print a compact PlayKiln agent-harness inventory so mirrored surfaces and specs cannot drift silently.
 * Usage: node scripts/harness-inventory.mjs
 */

import { existsSync, readFileSync, readdirSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
let readFailure = false

function line(status, message) {
  console.log(`${status.padEnd(8)} ${message}`)
  if (status === 'FAIL') readFailure = true
}

function section(title) {
  console.log(`\n## ${title}`)
}

function entries(relativePath) {
  const absolutePath = join(root, relativePath)
  if (!existsSync(absolutePath)) {
    line('FAIL', `required directory missing: ${relativePath}`)
    return []
  }
  try {
    return readdirSync(absolutePath, { withFileTypes: true }).sort((left, right) =>
      left.name.localeCompare(right.name)
    )
  } catch (error) {
    line('FAIL', `cannot read ${relativePath}: ${error.message}`)
    return []
  }
}

function files(relativePath, extension) {
  return entries(relativePath)
    .filter((entry) => entry.isFile() && (!extension || entry.name.endsWith(extension)))
    .map((entry) => entry.name)
}

function directories(relativePath) {
  return entries(relativePath).filter((entry) => entry.isDirectory()).map((entry) => entry.name)
}

function joinNames(names) {
  return names.length > 0 ? names.join(', ') : '(none)'
}

function readRequired(relativePath) {
  const absolutePath = join(root, relativePath)
  if (!existsSync(absolutePath)) {
    line('FAIL', `required file missing: ${relativePath}`)
    return null
  }
  try {
    return readFileSync(absolutePath, 'utf8').replace(/^\uFEFF/, '')
  } catch (error) {
    line('FAIL', `cannot read ${relativePath}: ${error.message}`)
    return null
  }
}

function description(text) {
  const block = text.match(/\/\*\*?([\s\S]*?)\*\//)
  if (block) {
    const first = block[1]
      .split(/\r?\n/)
      .map((value) => value.replace(/^\s*\*?\s?/, '').trim())
      .find(Boolean)
    if (first) return first
  }

  const pythonDocstring = text.match(/^(?:#![^\r\n]*\r?\n)?\s*(?:"""|''')\s*([^\r\n]+)/)
  return pythonDocstring?.[1]?.trim() || '(no block description)'
}

console.log('# PlayKiln harness inventory')

const commands = files('.cursor/commands', '.md')
const cursorSkillDirectories = directories('.cursor/skills')
const cursorSkillManifests = cursorSkillDirectories.filter((name) =>
  existsSync(join(root, '.cursor', 'skills', name, 'SKILL.md'))
)
const cursorAgents = files('.cursor/agents', '.md')
const rules = files('.cursor/rules', '.mdc')
const hooks = files('.cursor/hooks', '.cjs')

let mcpServers = []
const mcpText = readRequired('.cursor/mcp.json')
if (mcpText !== null) {
  try {
    const config = JSON.parse(mcpText)
    mcpServers = Object.keys(config.mcpServers || {}).sort((left, right) => left.localeCompare(right))
  } catch (error) {
    line('FAIL', `.cursor/mcp.json is invalid JSON: ${error.message}`)
  }
}

section('Cursor surface')
line('INFO', `.cursor/commands/*.md: ${commands.length}`)
line('INFO', `.cursor/skills/*/SKILL.md: ${cursorSkillManifests.length}`)
line('INFO', `.cursor/agents/*.md: ${cursorAgents.length}`)
line('INFO', `.cursor/rules/*.mdc: ${rules.length}`)
line('INFO', `.cursor/hooks/*.cjs: ${hooks.length}`)
line('INFO', `MCP servers (${mcpServers.length}): ${joinNames(mcpServers)}`)

section('Mirror parity')
for (const agent of cursorAgents) {
  const missing = ['.claude/agents', '.codex/agents'].filter(
    (directory) => !existsSync(join(root, directory, agent))
  )
  line(
    missing.length === 0 ? 'PASS' : 'WARN',
    missing.length === 0 ? `${agent}: Claude and Codex mirrors present` : `${agent}: missing ${missing.join(', ')}`
  )
}

section('Skills')
line('INFO', `Cursor skills (${cursorSkillDirectories.length}): ${joinNames(cursorSkillDirectories)}`)
for (const skill of cursorSkillDirectories) {
  if (!cursorSkillManifests.includes(skill)) line('WARN', `.cursor/skills/${skill}/SKILL.md is missing`)
}
const officialSkillDirectories = directories('.agents/skills/android-official')
const officialSkills = officialSkillDirectories.filter((name) =>
  existsSync(join(root, '.agents', 'skills', 'android-official', name, 'SKILL.md'))
)
line('INFO', `Android official vendored skills: ${officialSkills.length}`)

section('OpenSpec')
const specs = directories('openspec/specs')
const changes = directories('openspec/changes').filter((name) => name !== 'archive')
line('INFO', `specs (${specs.length}): ${joinNames(specs)}`)
line('INFO', `active changes (${changes.length}): ${joinNames(changes)}`)
for (const change of changes) {
  const tasks = join(root, 'openspec', 'changes', change, 'tasks.md')
  line(existsSync(tasks) ? 'PASS' : 'WARN', `${change}: ${existsSync(tasks) ? 'tasks.md present' : 'tasks.md missing'}`)
}

section('Scripts')
const scriptEntries = entries('scripts')
for (const entry of scriptEntries) {
  if (entry.isDirectory()) {
    line('INFO', `${entry.name}/ — directory`)
    continue
  }
  if (!entry.isFile()) continue
  const relativePath = `scripts/${entry.name}`
  const text = readRequired(relativePath)
  if (text !== null) line('INFO', `${entry.name} — ${description(text)}`)
}

console.log(`\nVERDICT: ${readFailure ? 'FAIL' : 'PASS'}`)
process.exitCode = readFailure ? 1 : 0
