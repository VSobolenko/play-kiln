#!/usr/bin/env node
/**
 * sessionStart — inject PlayKiln mission + L1 pointers into every new agent chat.
 */
const fs = require('fs')
const path = require('path')

let input = {}
try {
  input = JSON.parse(fs.readFileSync(0, 'utf8') || '{}')
} catch {}

const root = (input.workspace_roots && input.workspace_roots[0]) || process.cwd()

function read(rel, max = 12000) {
  const p = path.join(root, rel)
  try {
    if (!fs.existsSync(p)) return ''
    const t = fs.readFileSync(p, 'utf8')
    return t.length > max ? t.slice(0, max) + '\n\n[…truncated — see ' + rel + ']' : t
  } catch {
    return ''
  }
}

const mission = read('MISSION.md', 10000)
const extra = [
  '=== PLAYKILN SESSION (auto) ===',
  mission || 'MISSION.md missing — follow AGENTS.md.',
  '',
  'Commands: /ship (idea → publish-ready app; also continues existing work), /eval, /kiln-status, /improve-harness.',
  'Agents: planner, generator, evaluator, play-auditor, android-debugger.',
  'Gate: node scripts/verify-slot.mjs — never self-PASS.',
  'Full contract: AGENTS.md · DESIGN.md · harness/cursor-surface.md'
].join('\n')

process.stdout.write(
  JSON.stringify({
    additional_context: extra,
    env: {
      PLAYKILN_ROOT: root,
      PLAYKILN_FACTORY: '1'
    }
  })
)
