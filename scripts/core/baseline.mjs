// The factory baseline: when a new rule starts applying, and to which slots.
//
// A rule added after apps exist has two failure modes, and this module exists to avoid both.
// Manufacturing a permanent red row on a slot that is already shipped — or already killed — teaches
// the operator to read past the gate, and the gate then protects nothing. Grandfathering silently
// hides the defect in exactly the slots that have it. So applicability is *derived* rather than
// hand-maintained: the creation date comes from `registry/apps.json`, and "is this an app at all"
// comes from the economics plan the money gate already requires. A slot the baseline does not gate
// still prints its finding as a note, because a defect that exists and is not reported reads exactly
// like a defect that is not there.
//
// This is the first baseline of its kind, added 2026-09-30 after the owner's review of the first
// live app found a dated interface, no settings surface and late localization. The three rules that
// grew out of it each declare when they take effect and hand their findings here, so the four
// existing slots are not re-judged for defects the owner has already decided not to repair, while
// every slot created from now on is born into the baseline.
import path from 'node:path'
import { existsSync } from 'node:fs'
import { ROOT, fail, pass } from './lib.mjs'
import { readRegistry } from './project.mjs'

/** The date a baseline added in the 2026-09-30 session starts applying to newly created slots. */
export const FACTORY_BASELINE = '2026-09-30'

/** The registry entry for a slot directory, by its recorded `dir`. */
export function registryEntry(slotDir) {
  const relative = path.relative(ROOT, slotDir).split(path.sep).join('/')
  const apps = readRegistry().apps
  return Array.isArray(apps) ? apps.find((app) => app?.dir === relative) || null : null
}

/**
 * Whether a slot is an app rather than a pipeline proof.
 *
 * A proof slot exists to demonstrate that the pipeline can produce a signed artifact, and it has no
 * product behaviour to judge; the money gate makes the same distinction, reporting a proof slot as
 * `BLOCKED … economics plan could not be read` rather than as a pass. The marker is the plan itself,
 * which is the one thing every product slot must have.
 */
export function isProductSlot(slotDir) {
  return existsSync(path.join(slotDir, 'economics.json'))
}

/**
 * Whether a baseline gates this slot, with the sentence that explains the answer either way.
 *
 * The reason is returned rather than logged, because it is printed in the verdict detail: a reader
 * must be able to tell "this rule does not apply here" from "this rule applied and found nothing".
 */
export function baselineApplies(slotDir) {
  if (!isProductSlot(slotDir)) {
    return {
      applies: false,
      reason:
        'not a product slot: it declares no economics.json, so it is a pipeline proof rather than an app'
    }
  }
  const entry = registryEntry(slotDir)
  if (entry?.status === 'killed') {
    return {
      applies: false,
      reason: 'the slot is killed; its evidence is archived rather than gated'
    }
  }
  const createdAt = typeof entry?.createdAt === 'string' ? entry.createdAt.slice(0, 10) : null
  if (!createdAt) {
    return {
      applies: true,
      reason: ''
    }
  }
  if (createdAt < FACTORY_BASELINE) {
    return {
      applies: false,
      reason: `created ${createdAt}, before the ${FACTORY_BASELINE} baseline`
    }
  }
  return { applies: true, reason: '' }
}

/**
 * The one place a baseline check turns findings into a verdict.
 *
 * `applies` is the slot-scoped answer, so a finding is a FAIL only where the rule is in force; the
 * other slots carry it as a note inside a PASS. `summary` is what a passing check says when it found
 * nothing, and it must not be a claim the check did not measure.
 */
export function baselineVerdict(id, slotDir, findings, summary) {
  const state = baselineApplies(slotDir)
  if (findings.length === 0) return pass(id, summary)
  if (state.applies) return fail(id, findings.join('; '))
  return pass(
    id,
    `${summary}; note: ${state.reason} — ${findings.length} finding(s) reported but not gating: ${findings.join('; ')}`
  )
}
