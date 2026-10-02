#!/usr/bin/env node
/**
 * preToolUse (all tools) — record the tool name, so delegation is observable rather than assumed.
 *
 * Why this exists: the factory's stated model is that the main chat orchestrates and the work runs
 * in subagents and workers (`.cursor/rules/parallel-execution.mdc`). Nothing measured that. The
 * rule could be violated turn after turn and the only evidence was the owner noticing, which is how
 * he came to ask for it repeatedly. A rule with no sensor behind it is this repository's
 * most-recorded failure mode, so this writes the one fact that makes the question answerable: which
 * tools a session actually used, and therefore whether it ever dispatched a worker at all.
 *
 * What is deliberately NOT attempted: attribution. This build's hook payloads carry no usable
 * `conversation_id` — `.cursor/hooks/after-edit-ledger.cjs` records `timestamp \t - \t ` for every
 * edit, which `stop-gate.cjs` documents at length — so there is no way to say *which* agent made a
 * call, and equally no way to tell a subagent's own Shell from the orchestrator's. A per-agent
 * ledger would be a fabrication. A session-level count is what can be measured, so a session-level
 * count is what is written, and `stop-gate.cjs` reads it against the same watermark it uses for
 * file changes.
 *
 * Failure modes, and why this is built to stay silent rather than to guess:
 * - If this hook never fires, the log has no rows, and the reader treats that as "not observable" and
 *   says nothing, rather than as "no delegation".
 * - If it fires for ordinary tools but never for a dispatch, the reader would accuse a delegating
 *   session. That case is why the reader is **armed only by positive proof** — one dispatch row in the
 *   log's whole history (`everDispatched` in `stop-gate.cjs`) — and why this hook is wired to
 *   **two** events: `preToolUse`, which sees the `Task` call, and `subagentStart`, which Cursor's own
 *   hook documentation names as the event for subagent lifecycle and which carries the subagent's
 *   type instead of a tool name. Reading both means the sensor does not depend on which of the two a
 *   given build actually fires.
 * - Unconfirmed when this was written: whether either event fires for a dispatch in this build. The
 *   `stop` hook demonstrably fires (its reminder has been observed), so `hooks.json` itself is live;
 *   a `preToolUse` entry added mid-session was not observed firing, which is why the matcher was
 *   dropped and `subagentStart` added — Cursor's guidance is to remove a matcher and confirm the base
 *   hook before tightening it. The reader's arming condition is the honest response to that gap: it
 *   cannot accuse while it is unproven.
 *
 * Observing only: it records and always allows, so it can never block a tool call. Liveness is
 * unbounded by design — the log is inside the repository (`.superpowers/`, gitignored) and the
 * workspace-hygiene rule forbids writing anywhere outside it, so growth is the repository's problem
 * and not the machine's.
 */
const fs = require('fs')
const path = require('path')

const LOG_PARTS = ['.superpowers', 'parallel', 'tools.log']

/**
 * Payload keys that identify a worker dispatch, across the events this hook is wired to.
 *
 * `preToolUse` sees the *tool* (`Task`), while `subagentStart` sees the *subagent* and carries no
 * tool name at all — it is the event Cursor's own hook documentation names for this, and it is the
 * more reliable of the two because it fires for the thing being measured rather than for the call
 * that requested it. Both are read, so the sensor does not depend on which one a given build fires.
 */
const DISPATCH_TOOL_NAMES = new Set(['Task'])
const DISPATCH_TYPE_KEYS = ['subagent_type', 'agent_type', 'subagentType']

function readInput() {
  try {
    return JSON.parse(fs.readFileSync(0, 'utf8') || '{}')
  } catch {
    return {}
  }
}

const allow = () => process.stdout.write(JSON.stringify({ permission: 'allow' }))

function main() {
  const input = readInput()
  const root = (input.workspace_roots && input.workspace_roots[0]) || process.cwd()
  const name = input.tool_name || ''

  // A `subagentStart` payload has no tool name; the subagent's own type is what identifies it.
  const subagentType = DISPATCH_TYPE_KEYS.map((key) => input[key]).find((value) => typeof value === 'string' && value)
  const isDispatch = DISPATCH_TOOL_NAMES.has(name) || Boolean(subagentType)
  const label = name || (subagentType ? `subagent:${subagentType}` : '')

  if (!label) return allow()

  const file = path.join(root, ...LOG_PARTS)
  try {
    fs.mkdirSync(path.dirname(file), { recursive: true })
    // One row per call: when, what ran, and whether it counts as a dispatch. The third column is
    // written here rather than derived by the reader, so the definition of "a dispatch" has exactly
    // one home.
    fs.appendFileSync(file, `${new Date().toISOString()}\t${label}\t${isDispatch ? '1' : '0'}\n`)
  } catch {
    // A read-only workspace must not turn an observation into a failure.
  }

  return allow()
}

main()
