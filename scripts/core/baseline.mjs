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
//
// A later rule may need a *later* date than the 2026-09-30 family, because a slot created in the
// days between the two is already shipped and is not being repaired either. `uiux.content-colour`
// is the first of those: the invisible-text defect was measured on 2026-10-02, so a slot created on
// 2026-09-30 or 2026-10-01 carries the finding as a note while a slot created from 2026-10-02 is
// gated. The date is per check id rather than one global, because two rules added a week apart do
// not share a birthday and pretending they do would either redden a shipped app or hide the defect
// from a new one.
import path from 'node:path'
import { existsSync } from 'node:fs'
import { ROOT, fail, pass } from './lib.mjs'
import { readRegistry } from './project.mjs'

/** The date a baseline added in the 2026-09-30 session starts applying to newly created slots. */
export const FACTORY_BASELINE = '2026-09-30'

/**
 * The date the content-colour rule starts applying, which is the day the defect was measured.
 *
 * Read from `.cursor/rules/ui-ux-baseline.mdc`'s own account of the incident: a slot drew pure
 * `#000000` glyph strokes on a `#101410` window background, contrast 1.13:1, because nothing between
 * `NavDisplay` and the window provided a content colour. The rule is authored the day it was
 * measured, so every slot created from that date is gated and the ones already on the shelf are not.
 */
export const CONTENT_COLOUR_BASELINE = '2026-10-02'

/**
 * The date the In-App Review rule starts applying — the day the owner required soft rating prompts
 * via Play's ReviewManager (2026-10-05). Slots created before that date carry findings as notes.
 */
export const IN_APP_REVIEW_BASELINE = '2026-10-05'

/**
 * The date the specification rule starts applying, which is the day the defect was measured.
 *
 * The owner's rule is that every application is authored inside an OpenSpec change, and it was
 * violated without anything noticing: `apps/2026-10-02-laneset/` had code while
 * `openspec/changes/` held no change for it. The rule is authored the day it was measured, so every
 * slot created from that date is gated and the ones already on the shelf are not — the same shape as
 * `CONTENT_COLOUR_BASELINE`, and for the same reason a later rule must not redden a shipped app.
 */
export const SPEC_BASELINE = '2026-10-02'

/**
 * The date the device-capability declaration starts applying to a proposal.
 *
 * `scripts/core/ideas.mjs` requires every proposal under `docs/ideation/` to answer `used` or
 * `not used` for twelve device capabilities, so a job that needs nothing from the phone states that
 * on purpose rather than arriving at it by silence. The rule was authored on 2026-10-02, after that
 * day's proposals had already been written and presented, and NO proposal could be retro-fitted with
 * a field the contract did not yet ask for — so the baseline is the day AFTER the newest proposal on
 * disk, which is the first proposal date the rule can govern. The property that matters is the one
 * `FACTORY_BASELINE` exists for: not one existing proposal is gated, while every proposal authored
 * from this date is, because a gate that is red across the tree it was written into teaches the
 * operator to read past it.
 */
export const PROPOSAL_CAPABILITY_BASELINE = '2026-10-03'

/**
 * Whether the proposal baseline gates a proposal authored on `date`, with the reason either way.
 *
 * A proposal carries its own date in its filename, so applicability is derived from the artifact
 * rather than hand-listed, exactly as `baselineApplies` derives a slot's from `registry/apps.json`.
 * A date that cannot be read is treated as gated, never as exempt: an unreadable input must not
 * become a silent pass.
 */
export function proposalBaselineApplies(date) {
  const iso = typeof date === 'string' ? date.slice(0, 10) : ''
  if (!/^\d{4}-\d{2}-\d{2}$/.test(iso)) return { applies: true, reason: '' }
  if (iso < PROPOSAL_CAPABILITY_BASELINE) {
    return { applies: false, reason: `authored ${iso}, before the ${PROPOSAL_CAPABILITY_BASELINE} baseline` }
  }
  return { applies: true, reason: '' }
}

/**
 * Per-check baseline dates, for a rule whose birthday is later than `FACTORY_BASELINE`.
 *
 * A check id absent from this table uses `FACTORY_BASELINE`, which is what keeps every existing
 * caller's behaviour unchanged: the map is additive, and adding an entry is the only way a check
 * gets a date of its own.
 */
const CHECK_BASELINES = Object.freeze({
  'uiux.content-colour': CONTENT_COLOUR_BASELINE,
  'slot.spec': SPEC_BASELINE,
  'review.dependency': IN_APP_REVIEW_BASELINE,
  'review.api': IN_APP_REVIEW_BASELINE,
  'review.throttle': IN_APP_REVIEW_BASELINE,
  'review.settings': IN_APP_REVIEW_BASELINE
})

/** The date a check id's baseline starts applying, defaulting to the factory's own. */
export function baselineDateFor(id) {
  return CHECK_BASELINES[id] ?? FACTORY_BASELINE
}

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
 *
 * `id` is the check id whose baseline is being asked about, and it is optional so that a caller with
 * no id in hand — `check-differentiators.mjs` — keeps the factory-wide date it always used.
 */
export function baselineApplies(slotDir, id = null) {
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
  const baseline = baselineDateFor(id)
  const createdAt = typeof entry?.createdAt === 'string' ? entry.createdAt.slice(0, 10) : null
  if (!createdAt) {
    return {
      applies: true,
      reason: ''
    }
  }
  if (createdAt < baseline) {
    return {
      applies: false,
      reason: `created ${createdAt}, before the ${baseline} baseline`
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
  const state = baselineApplies(slotDir, id)
  if (findings.length === 0) return pass(id, summary)
  if (state.applies) return fail(id, findings.join('; '))
  return pass(
    id,
    `${summary}; note: ${state.reason} — ${findings.length} finding(s) reported but not gating: ${findings.join('; ')}`
  )
}
