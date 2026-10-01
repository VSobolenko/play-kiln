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

const mission = read('MISSION.md', 14000)

// Stamp the session boundary for `.cursor/hooks/stop-gate.cjs`. That hook decides whether to
// remind the agent about evidence by comparing `mtime`s under `apps/` and `harness/` against a
// watermark; without a stamp taken here, its first check has nothing to compare against and it
// writes one itself, so this is an improvement rather than a dependency — it just means changes
// made in the first moments of a session are attributed correctly instead of being missed.
try {
  const stamp = path.join(root, '.superpowers', 'stop-gate', 'session.json')
  fs.mkdirSync(path.dirname(stamp), { recursive: true })
  fs.writeFileSync(stamp, JSON.stringify({ since: new Date().toISOString(), root }, null, 2) + '\n')
} catch {
  // A read-only workspace must not stop the session from starting.
}

const extra = [
  '=== PLAYKILN SESSION (auto) ===',
  mission || 'MISSION.md missing — follow AGENTS.md.',
  '',
  'Two phases: phase 1 develops an app to review (debug build, tests, source gates) and is entered by',
  '/forge (finds the topic and builds it) or /ship (the idea is already in hand); phase 2 signs,',
  'packs and captures screenshots only after the owner approves the revision with',
  '`node scripts/approve-release.mjs --slot <slug> --note "<the owner\'s own words>"`.',
  'Commands: /forge [<brief>] (recon picks the topic, then builds and releases, stopping twice for you),',
  '/ship <idea> (develop from an idea you already have; also continues existing work), /release <slug>, /eval, /kiln-status, /improve-harness.',
  'Agents: planner, generator, evaluator, play-auditor, android-debugger.',
  'Gate: node scripts/verify.mjs — never self-PASS. Never record an approval yourself.',
  'Full contract: AGENTS.md · DESIGN.md · .cursor/rules/release-gate.mdc · harness/cursor-surface.md'
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
