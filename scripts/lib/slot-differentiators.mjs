/**
 * The slot differentiator contract: the reasons this app is worth installing, stated
 * one by one, each with the recon evidence that says it is worth keeping and the proof
 * that says it actually shipped.
 *
 * Why this exists: recon finds why an app can win — a complaint every incumbent shares,
 * a job nobody serves, a number in a review — and that list is the product's actual
 * plan. It is also the first thing implementation quietly loses. The factory already
 * has a shape for this: `economics.json` states how the app earns and a program reads
 * it. This module is the same idea for *what the app is for*. A twelve-point reason
 * list becomes a twelve-row contract, every row starts `deferred`, and a row may only
 * leave that state by shipping with evidence on disk or by being dropped in writing —
 * with the owner's own words when the row was one of the core ones.
 *
 * The failure this prevents is not "we forgot". It is "we shipped ten of twenty and
 * called the slot done", which no existing gate could see, because every existing gate
 * measures form — the APK builds, the pack is complete, the plan parses — and none of
 * them knew what the app had promised to be.
 */

// A placeholder is a placeholder: the same patterns and the same minimum length that
// `docs/slot-economics.md` applies to a plan apply to a promise. Imported rather than
// restated, because a second copy of this rule is a second thing to drift.
import { MIN_PLAN_TEXT_LENGTH, PLACEHOLDER_PATTERNS } from './slot-economics.mjs'

export const SCHEMA_VERSION = 1

/** A promise shorter than this is a label, not a promise. */
export const MIN_CLAIM_TEXT_LENGTH = MIN_PLAN_TEXT_LENGTH

/**
 * Floors, not quality measures. An app whose reason list is one item is a one-trick
 * recipe, and an app with no core claim has no claim that could ever cost anything to
 * keep. These numbers stop a degenerate file from passing; they cannot make a file good.
 */
export const MIN_CLAIMS = 3
export const MIN_CORE_CLAIMS = 2

/** How much a promise costs to break. `core` promises need the owner to break them. */
export const WEIGHTS = Object.freeze(['core', 'supporting'])

/** The evidence labels `.cursor/rules/ideation-gate.mdc` already requires of a claim. */
export const STRENGTHS = Object.freeze(['MEASURED', 'ON RECORD', 'INFERRED', 'UNVERIFIED'])

/**
 * Where a promise can be. `deferred` is the state every promise starts in and the only
 * state that is not an answer: the gate fails while any promise is still deferred, so
 * "we ran out of time" cannot be mistaken for "we are done".
 */
export const STATUSES = Object.freeze(['shipped', 'narrowed', 'dropped', 'deferred'])
export const UNRESOLVED_STATUS = 'deferred'

/** Statuses that mean the promise has been accounted for, one way or another. */
export const RESOLVED_STATUSES = Object.freeze(['shipped', 'narrowed', 'dropped'])

/** Breaking or weakening a `core` promise is the owner's call, not the builder's. */
export const OWNER_DECISION_STATUSES = Object.freeze(['narrowed', 'dropped'])

/**
 * What kind of artifact proves a promise. The type is what makes the `ref` checkable:
 * a promise about behaviour is proved by a test in a test source set, a promise about
 * what the user sees is proved by a resource, and a promise about how it looks is
 * proved by a captured frame. Allowing a free-form ref would let a promise point at
 * any file in the slot, which is how a proof becomes a checkbox.
 */
export const PROOF_TYPES = Object.freeze(['test', 'code', 'resource', 'capture'])

/**
 * Where a proof of each type must live, as slot-relative prefixes. A test proof may be
 * a unit test or an instrumented test because some promises are only observable on a
 * device; a capture proof is a committed store asset, because `captures/` is not
 * committed and a proof that lives only on the machine that ran it is not a proof.
 */
export const PROOF_PREFIXES = Object.freeze({
  test: Object.freeze(['app/src/test/', 'app/src/androidTest/']),
  code: Object.freeze(['app/src/main/']),
  resource: Object.freeze(['app/src/main/res/']),
  capture: Object.freeze(['store/assets/'])
})

/** Proof types whose file must literally contain the named symbol. */
export const SYMBOL_PROOF_TYPES = Object.freeze(['test', 'code', 'resource'])

/**
 * Where a promise's evidence may come from: the measured research the factory keeps, or
 * the proposal the recipe came from. The same two roots `economics.json` accepts, for
 * the same reason — the per-niche measurement lives in the proposal, and the method
 * record alone is a weaker link than a plan implies.
 */
const EVIDENCE_SOURCE_PATTERN = /^docs\/(?:research|ideation)\/[a-z0-9._-]+\.md(#.+)?$/

/** Claim ids are named in gate output, so they must be stable and greppable. */
const CLAIM_ID_PATTERN = /^[a-z0-9]+(?:-[a-z0-9]+)*$/

function isPlainObject(value) {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value)
}

function describe(value) {
  if (value === undefined) return 'missing'
  if (typeof value === 'string') return `"${value}"`
  return JSON.stringify(value)
}

function matchesPlaceholder(text) {
  return PLACEHOLDER_PATTERNS.some((pattern) => pattern.test(text))
}

/**
 * Validate one required free-text field. `what` names the field in the error so a
 * report of ten problems says which row and which field each one came from.
 */
function requireText(value, what, errors, { min = MIN_CLAIM_TEXT_LENGTH } = {}) {
  if (typeof value !== 'string') {
    errors.push(`${what} is required and must be a string (found ${describe(value)})`)
    return false
  }
  const trimmed = value.trim()
  if (!trimmed) {
    errors.push(`${what} must not be empty`)
    return false
  }
  if (matchesPlaceholder(trimmed)) {
    errors.push(`${what} must say the real thing, not a placeholder (found "${trimmed}")`)
    return false
  }
  if (trimmed.length < min) {
    errors.push(`${what} must be at least ${min} characters so it states a promise rather than a label (found ${trimmed.length})`)
    return false
  }
  return true
}

function validateEvidence(value, what, errors) {
  if (!isPlainObject(value)) {
    errors.push(`${what}.evidence must be an object with strength, source and finding`)
    return
  }
  const { strength, source, finding } = value
  if (!STRENGTHS.includes(strength)) {
    errors.push(`${what}.evidence.strength must be one of: ${STRENGTHS.join(', ')} (found ${describe(strength)})`)
  }
  if (typeof source !== 'string' || !EVIDENCE_SOURCE_PATTERN.test(source.trim())) {
    errors.push(
      `${what}.evidence.source must be docs/research/<file>.md or docs/ideation/<file>.md with an optional #section (found ${describe(source)})`
    )
  } else if (matchesPlaceholder(source.trim())) {
    errors.push(`${what}.evidence.source must point at real recon, not a placeholder (found "${source.trim()}")`)
  }
  // A MEASURED claim with no number in it is a claim about a measurement nobody took.
  const findingOk = requireText(finding, `${what}.evidence.finding`, errors)
  if (findingOk && strength === 'MEASURED' && !/\d/.test(finding)) {
    errors.push(
      `${what}.evidence.finding is labelled MEASURED but contains no number; quote the count, the price or the rating the recon actually saw, or lower the label`
    )
  }
}

function validateProof(proof, what, errors) {
  if (!isPlainObject(proof)) {
    errors.push(`${what}.proof must be an object with type, ref and assertion`)
    return null
  }
  const { type, ref, symbol, assertion } = proof
  if (!PROOF_TYPES.includes(type)) {
    errors.push(`${what}.proof.type must be one of: ${PROOF_TYPES.join(', ')} (found ${describe(type)})`)
    return null
  }
  if (typeof ref !== 'string' || !ref.trim()) {
    errors.push(`${what}.proof.ref must be a slot-relative path (found ${describe(ref)})`)
  } else if (ref.startsWith('/') || /^[a-z]:/i.test(ref) || ref.includes('..')) {
    errors.push(`${what}.proof.ref must be relative to the slot root and must not escape it (found "${ref}")`)
  }
  if (SYMBOL_PROOF_TYPES.includes(type)) {
    if (typeof symbol !== 'string' || !symbol.trim()) {
      errors.push(
        `${what}.proof.symbol is required for a "${type}" proof: name the test, composable or resource key that carries the promise, so deleting it fails this gate`
      )
    } else if (matchesPlaceholder(symbol.trim())) {
      errors.push(`${what}.proof.symbol must name a real symbol (found "${symbol.trim()}")`)
    }
  }
  requireText(assertion, `${what}.proof.assertion`, errors)
  return type
}

function validateOwnerDecision(value, what, errors) {
  if (!isPlainObject(value)) {
    errors.push(
      `${what} is a core promise, so narrowing or dropping it needs ownerDecision { words, recordedAt } — the owner's own words, never an agent's summary of them`
    )
    return
  }
  requireText(value.words, `${what}.ownerDecision.words`, errors)
  if (typeof value.recordedAt !== 'string' || Number.isNaN(Date.parse(value.recordedAt))) {
    errors.push(`${what}.ownerDecision.recordedAt must be an ISO date string (found ${describe(value.recordedAt)})`)
  }
}

function validateClaim(claim, index, errors, seenIds) {
  const what = `claims[${index}]`
  if (!isPlainObject(claim)) {
    errors.push(`${what} must be an object`)
    return null
  }

  const id = claim.id
  let label = what
  if (typeof id !== 'string' || !CLAIM_ID_PATTERN.test(id)) {
    errors.push(`${what}.id must be a kebab-case identifier such as "cites-code-edition" (found ${describe(id)})`)
  } else {
    label = `claim "${id}"`
    if (seenIds.has(id)) errors.push(`${what}.id is duplicated: "${id}"`)
    seenIds.add(id)
  }

  const weight = claim.weight
  if (!WEIGHTS.includes(weight)) {
    errors.push(`${label}.weight must be one of: ${WEIGHTS.join(', ')} (found ${describe(weight)})`)
  }

  requireText(claim.promise, `${label}.promise`, errors)
  validateEvidence(claim.evidence, label, errors)

  const status = claim.status
  if (!STATUSES.includes(status)) {
    errors.push(`${label}.status must be one of: ${STATUSES.join(', ')} (found ${describe(status)})`)
    return { id, weight, status }
  }

  if (status === 'deferred') {
    if (typeof claim.deferredReason !== 'string' || !claim.deferredReason.trim()) {
      errors.push(`${label}.deferredReason is required while the promise is deferred, so an unfinished row says what is missing`)
    }
  } else {
    if (typeof claim.deferredReason === 'string' && claim.deferredReason.trim()) {
      errors.push(`${label}.deferredReason must be removed once the promise is resolved (status "${status}")`)
    }
  }

  if (status === 'shipped' || status === 'narrowed') {
    validateProof(claim.proof, label, errors)
  } else if (claim.proof !== undefined) {
    errors.push(
      `${label}.proof must not be present at status "${status}": a promise that did not ship has nothing to prove, and a stale proof reads as delivery`
    )
  }

  if (status === 'narrowed') {
    requireText(claim.narrowedTo, `${label}.narrowedTo`, errors)
    requireText(claim.lostCoverage, `${label}.lostCoverage`, errors)
  }
  if (status === 'dropped') {
    requireText(claim.droppedReason, `${label}.droppedReason`, errors)
  }

  // The teeth. A `core` promise is the reason the app was chosen; weakening or losing
  // it is a decision about what the product is, so it is the owner's to make and it is
  // recorded in his words. A `supporting` promise only has to be explained in writing.
  if (weight === 'core' && OWNER_DECISION_STATUSES.includes(status)) {
    validateOwnerDecision(claim.ownerDecision, label, errors)
  }

  return { id, weight, status }
}

/**
 * Validate a parsed `differentiators.json`. Returns `{ ok: true, value }` or
 * `{ ok: false, errors }`, and never throws for invalid input, so a caller can print
 * every problem at once instead of failing on the first.
 *
 * A string argument is accepted and JSON-parsed for convenience.
 */
export function parseDifferentiators(json) {
  let value = json
  if (typeof value === 'string') {
    try {
      value = JSON.parse(value.replace(/^\uFEFF/, ''))
    } catch (error) {
      return { ok: false, errors: [`differentiators is not valid JSON: ${error.message}`] }
    }
  }
  if (!isPlainObject(value)) {
    return { ok: false, errors: ['differentiators must be a JSON object'] }
  }

  const errors = []
  if (value.schemaVersion !== SCHEMA_VERSION) {
    errors.push(`schemaVersion must be exactly ${SCHEMA_VERSION} (found ${describe(value.schemaVersion)})`)
  }
  if (typeof value.slot !== 'string' || !CLAIM_ID_PATTERN.test(value.slot)) {
    errors.push(`slot must be the slot's kebab-case slug such as "pipe-sizing" (found ${describe(value.slot)})`)
  }
  const reconOk =
    typeof value.reconRef === 'string' && EVIDENCE_SOURCE_PATTERN.test(value.reconRef.trim())
  if (!reconOk) {
    errors.push(
      `reconRef must be docs/research/<file>.md or docs/ideation/<file>.md with an optional #section (found ${describe(value.reconRef)})`
    )
  }

  if (!Array.isArray(value.claims)) {
    errors.push(`claims must be an array (found ${describe(value.claims)})`)
    return { ok: false, errors }
  }

  const seenIds = new Set()
  const rows = []
  value.claims.forEach((claim, index) => {
    const row = validateClaim(claim, index, errors, seenIds)
    if (row) rows.push(row)
  })

  const core = rows.filter((row) => row.weight === 'core')
  if (value.claims.length < MIN_CLAIMS) {
    errors.push(
      `claims must contain at least ${MIN_CLAIMS} promises (found ${value.claims.length}); a reason list this short cannot describe why the app should win`
    )
  }
  if (core.length < MIN_CORE_CLAIMS) {
    errors.push(
      `claims must contain at least ${MIN_CORE_CLAIMS} with weight "core" (found ${core.length}); without one, no promise is load-bearing and none of them cost anything to break`
    )
  }

  return errors.length === 0 ? { ok: true, value } : { ok: false, errors }
}

/**
 * Counts for a one-line summary: how many promises shipped, how many were weakened,
 * and how many are still unfinished. The unresolved count is the one that matters —
 * it is the number the owner wants to see before saying the app is done.
 */
export function summarizeDifferentiators(value) {
  const claims = Array.isArray(value?.claims) ? value.claims : []
  const summary = { total: claims.length, core: 0, shipped: 0, narrowed: 0, dropped: 0, deferred: 0 }
  for (const claim of claims) {
    if (claim?.weight === 'core') summary.core += 1
    if (Object.prototype.hasOwnProperty.call(summary, claim?.status)) summary[claim.status] += 1
  }
  return summary
}

/**
 * Resolve the promises against the file system: the recon a claim cites must exist, the
 * artifact that proves a shipped promise must exist *in the slot* and must still contain
 * the symbol the claim named, and nothing may be left deferred.
 *
 * This half is separate from `parseDifferentiators` because it needs the slot, and
 * because a plan is parsed before its slot exists: `new-slot.mjs` validates the shape,
 * and this validates the delivery.
 *
 * `readFile`/`isFile` are injected so the checks are exercisable without a slot on disk.
 */
export function validateDifferentiatorRefs(
  value,
  {
    slotDir,
    repoRoot,
    slug,
    allowDeferred = false,
    isFile = () => false,
    readFile = () => ''
  } = {}
) {
  const errors = []
  const claims = Array.isArray(value?.claims) ? value.claims : []

  if (slug && value?.slot !== slug) {
    errors.push(
      `slot is ${describe(value.slot)} but this is the "${slug}" slot; a copied reason list from another slot is not this slot's recon`
    )
  }

  const reconRef = typeof value?.reconRef === 'string' ? value.reconRef.split('#')[0] : null
  if (reconRef) {
    const reconFile = repoRoot ? `${repoRoot}/${reconRef}` : reconRef
    if (!isFile(reconFile)) {
      errors.push(`reconRef does not resolve to a file: ${reconRef} (relative to the repo root)`)
    }
  }

  for (const claim of claims) {
    const label = typeof claim?.id === 'string' ? `claim "${claim.id}"` : 'claim'
    const source = claim?.evidence?.source
    if (typeof source === 'string' && EVIDENCE_SOURCE_PATTERN.test(source.trim())) {
      const sourceFile = repoRoot ? `${repoRoot}/${source.split('#')[0]}` : source.split('#')[0]
      if (!isFile(sourceFile)) {
        errors.push(`${label}.evidence.source does not resolve to a file: ${source.split('#')[0]}`)
      }
    }

    if (claim?.status === UNRESOLVED_STATUS && !allowDeferred) {
      errors.push(
        `${label} is still deferred (${claim.deferredReason || 'no reason given'}); every promise must end shipped, narrowed or dropped before the slot is called done`
      )
    }

    const proof = claim?.proof
    if (!proof || typeof proof !== 'object') continue
    if (!PROOF_TYPES.includes(proof.type) || typeof proof.ref !== 'string') continue

    const ref = proof.ref.split('\\').join('/')
    const prefixes = PROOF_PREFIXES[proof.type]
    if (!prefixes.some((prefix) => ref.startsWith(prefix))) {
      errors.push(
        `${label}.proof.ref "${ref}" is not in a location a "${proof.type}" proof may live in (expected one of: ${prefixes.join(', ')})`
      )
      continue
    }
    const proofFile = slotDir ? `${slotDir}/${ref}` : ref
    if (!isFile(proofFile)) {
      errors.push(
        `${label}.proof.ref does not resolve inside the slot: ${ref} — a promise proved by a file that is not there is not proved`
      )
      continue
    }
    if (SYMBOL_PROOF_TYPES.includes(proof.type) && typeof proof.symbol === 'string' && proof.symbol.trim()) {
      const text = readFile(proofFile)
      const symbol = proof.symbol.trim()
      // A resource promise names a key, not prose: `name="key"` is what makes the
      // promise breakable by editing the resource rather than by editing a comment.
      const present = proof.type === 'resource' ? text.includes(`name="${symbol}"`) : text.includes(symbol)
      if (!present) {
        errors.push(
          `${label}.proof.symbol "${symbol}" no longer appears in ${ref}; the promise and the artifact have drifted apart`
        )
      }
    }
  }

  return errors.length === 0 ? { ok: true, value } : { ok: false, errors }
}
