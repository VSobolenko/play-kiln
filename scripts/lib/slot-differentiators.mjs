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
 * The only `decidedBy` a refusal on the `after` stage may name.
 *
 * Refusing `after` gives up the handoff — the capability that makes the user's job finishable, and
 * the one this contract's own research ranks first (`export / backup / data portability`) — so it is
 * a product decision rather than a scheduling note, and it is the owner's to make in his own words,
 * exactly as weakening a `core` promise is. `before` stays the scout's to refuse with its own reason;
 * `during` is never refusable. The normative text is `.cursor/rules/app-depth.mdc`.
 */
export const OWNER_DECIDER = 'owner'

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
 * Where a promise sits in the user's job. The job is not the act the app is named for; it is
 * everything the act sits inside — what the user brings to it, the act itself, and what they do
 * with the result. A contract that names only the middle of that job is the raw-MVP defect this
 * field makes visible, and the normative text is `.cursor/rules/app-depth.mdc`.
 */
export const JOB_STAGES = Object.freeze(['before', 'during', 'after'])

/**
 * The closed set of depth families a promise may declare.
 *
 * `core_operation` is the app's central act — the middle of the job, never a differentiator by
 * itself, which is why it can never satisfy the handoff floor. In the middle of the list sit the
 * **connective** families, the handoffs that let a user finish with the job: bringing their own
 * data in, getting the result off the device, being sure the work cannot be lost, being reached,
 * capturing without opening the app, finding it again, acting on many at once. Below them are the
 * families the research rates presence-only (`insight`) or that are reportable and never floored.
 * The set is closed on purpose: an unknown family is a typo a gate can name, not a new capability.
 */
export const DEPTH_FAMILIES = Object.freeze([
  'core_operation',
  'data_in',
  'data_out',
  'durability',
  'reminder',
  'widget',
  'find',
  'bulk',
  'insight',
  'speed',
  'fit',
  'access',
  'collaboration',
  'trust'
])

/**
 * The families that count toward the handoff floor — the *connective* ones.
 *
 * Each names an artifact or a platform API that cannot be faked by prose, so a presence check
 * against the proof artifact means something. `insight` is deliberately absent: the research rates
 * a chart or a statistic presence-only, and a slot cannot satisfy the floor by declaring one. The
 * last five families are reportable but never floored.
 */
export const CONNECTIVE_FAMILIES = Object.freeze([
  'data_in',
  'data_out',
  'durability',
  'reminder',
  'widget',
  'find',
  'bulk'
])

/**
 * The token an artifact must contain to prove each family, copied from the table in `.cursor/rules/app-depth.mdc`.
 * A promise may say "exports a CSV"; the floor asks whether the artifact that proves it actually contains a way
 * to get the file off the phone. Tokens are matched literally, because a gate that guessed at semantics would be
 * the false positive `docs/research/2026-10-02-depth-encoding-and-detectability.md` measured — the repository's
 * own `edit-every-record` promise survives a lexical predicate because it carries a contrast clause. What this
 * cannot claim: a token is satisfiable by a single call site that never runs, so it raises the floor from "no
 * output path at all" to "a named output path exists", and it does not measure depth. `data_in` takes the user's
 * own content through the picker or the camera alike — the camera is the same handoff through a different door.
 */
export const FAMILY_TOKENS = Object.freeze({
  data_in: Object.freeze([
    'ACTION_OPEN_DOCUMENT',
    'ACTION_GET_CONTENT',
    'GetContent',
    'OpenDocument',
    'PickVisualMedia',
    'ACTION_IMAGE_CAPTURE',
    'TakePicture',
    'ImageCapture',
    'ClipData',
    'ClipboardManager'
  ]),
  data_out: Object.freeze(['ACTION_SEND', 'ACTION_CREATE_DOCUMENT', 'FileProvider', 'PdfDocument', 'share']),
  durability: Object.freeze([
    'dataExtractionRules',
    'fullBackupContent',
    'UndoState',
    'undo',
    'rememberSaveable',
    'SavedStateHandle'
  ]),
  reminder: Object.freeze(['NotificationChannel', 'WorkManager', 'NotificationManagerCompat']),
  widget: Object.freeze(['GlanceAppWidget', 'AppWidgetProvider']),
  find: Object.freeze(['SearchBar', 'Fts4', 'Fts5', 'MATCH', 'searchQuery']),
  bulk: Object.freeze(['selectAll', 'toggleSelection', 'multiSelect', 'selection']),
  insight: Object.freeze(['Canvas', 'Vico', 'chart', 'statistics', 'aggregate'])
})

/**
 * The rough lexical predicate of `docs/research/2026-10-02-depth-encoding-and-detectability.md` §1.2:
 * a basic record operation whose head noun is an app-internal record and which names no exit.
 *
 * This is a **report**, never a gate. The document measured the repository's own worst case — the
 * gradebook's `edit-every-record` — and found it escapes the predicate because it carries a contrast
 * clause ("instead of the incumbent's name-only editing"), which may be a true and sourced
 * difference. Failing it would be a false positive on correct prose, so the predicate's verdict is
 * printed for a human to read and never becomes a finding. The table is a proxy: it measures
 * vocabulary, and depth is semantics plus market.
 */
const CRUD_VERB = Object.freeze([
  'add', 'adds', 'adding', 'create', 'creates', 'created', 'creating', 'edit', 'edits', 'edited',
  'editing', 'update', 'updates', 'updated', 'updating', 'delete', 'deletes', 'deleted', 'deleting',
  'remove', 'removes', 'removed', 'removing', 'list', 'lists', 'listed', 'listing', 'view', 'views',
  'viewed', 'viewing', 'show', 'shows', 'showed', 'showing', 'display', 'displays', 'displayed',
  'displaying', 'save', 'saves', 'saved', 'saving', 'store', 'stores', 'stored', 'storing', 'enter',
  'enters', 'entered', 'entering', 'record', 'records', 'recorded', 'recording', 'track', 'tracks',
  'tracked', 'tracking', 'change', 'changes', 'changed', 'changing', 'manage', 'manages', 'managed',
  'managing', 'calculate', 'calculates', 'calculated', 'calculating', 'compute', 'computes',
  'computed', 'computing'
])

const CRUD_NOUN = Object.freeze([
  'student', 'students', 'class', 'classes', 'mark', 'marks', 'grade', 'grades', 'entry', 'entries',
  'row', 'rows', 'item', 'items', 'record', 'records', 'note', 'notes', 'customer', 'customers',
  'visit', 'visits', 'field', 'fields', 'sale', 'sales', 'log', 'logs'
])

const EXIT_SIGNAL = Object.freeze([
  'csv', 'json', 'pdf', 'file', 'files', 'backup', 'backups', 'export', 'exports', 'import',
  'imports', 'print', 'prints', 'share', 'shares', 'shared', 'clipboard', 'picker', 'photo',
  'photos', 'undo', 'redo', 'merge', 'merges', 'convert', 'converts', 'derive', 'derives',
  'derived', 'sum', 'sums', 'owed', 'balance', 'balances', 'total', 'totals', 'average',
  'averages', 'difference', 'differences', 'parent', 'invoice', 'invoices', 'reminder', 'reminders',
  'widget', 'notification', 'notifications', 'scan', 'scans', 'chart', 'charts', 'statistics',
  'template', 'templates'
])

const CONTRAST_ONLY = /\b(?:instead of|rather than|without|no account|offline|unlike|never|cannot|does not|doesn't)\b/i

const wholeWord = (words) => new RegExp(`\\b(?:${words.join('|')})\\b`, 'i')

/**
 * The §1.2 verdict for one promise: `CRUD-shaped`, `AUDIT`, or `null` for a promise that names a
 * handoff. Exported so the report and its fixtures agree on one predicate rather than two.
 */
export function crudVerdict(promise) {
  const text = typeof promise === 'string' ? promise : ''
  if (!wholeWord(CRUD_VERB).test(text)) return null
  if (!wholeWord(CRUD_NOUN).test(text)) return null
  if (wholeWord(EXIT_SIGNAL).test(text)) return null
  return CONTRAST_ONLY.test(text) ? 'AUDIT' : 'CRUD-shaped'
}

/** The line number of each claim id in the raw JSON, so a note can name a `file:line`. */
export function claimLines(rawText) {
  const lines = new Map()
  if (typeof rawText !== 'string') return lines
  rawText.split(/\r?\n/).forEach((line, index) => {
    const match = line.match(/"id"\s*:\s*"([^"]+)"/)
    if (match && !lines.has(match[1])) lines.set(match[1], index + 1)
  })
  return lines
}

/**
 * The CRUD report: one line per promise the lexical predicate flags, with its `file:line`.
 *
 * This is the machine-reported, human-audited half the rule names. It is deliberately not a
 * finding: see the comment on the predicate above, and `.cursor/rules/app-depth.mdc`
 * "Human-audited, reported and never auto-passed".
 */
export function crudReport(value, { file = 'differentiators.json', raw = '' } = {}) {
  const lines = claimLines(raw)
  const rows = []
  const claims = Array.isArray(value?.claims) ? value.claims : []
  for (const claim of claims) {
    const verdict = crudVerdict(claim?.promise)
    if (!verdict) continue
    const line = lines.get(claim?.id)
    rows.push(`${line ? `${file}:${line}` : file} claim "${claim?.id}" reads as ${verdict}`)
  }
  return rows
}

/**
 * Where a promise's evidence may come from: the measured research the factory keeps, or
 * the proposal the recipe came from. The same two roots `economics.json` accepts, for
 * the same reason — the per-niche measurement lives in the proposal, and the method
 * record alone is a weaker link than a plan implies.
 */
const EVIDENCE_SOURCE_PATTERN =
  /^(?:docs\/(?:research|ideation)|apps\/\d{4}-\d{2}-\d{2}-[a-z0-9]+(?:-[a-z0-9]+)*\/docs)\/[a-z0-9._-]+\.md(#.+)?$/

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
      `${what}.evidence.source must be docs/research/<file>.md, docs/ideation/<file>.md, or apps/<date>-<slug>/docs/<file>.md with an optional #section (found ${describe(source)})`
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

/**
 * The one shape in which "the owner said so" is written: `{ words, recordedAt }`.
 *
 * Two paths carry it — a narrowed or dropped `core` promise, and a refusal of the `after` stage —
 * and they share this function on purpose, so the two cannot drift apart
 * (`.cursor/rules/verification-discipline.mdc`, "one source of truth, imported — never restated").
 * `because` is the sentence that says why *this* holder needs the owner's words; the field errors
 * it produces are identical either way.
 */
function validateOwnerDecision(value, what, errors, because = 'is a core promise, so narrowing or dropping it') {
  if (!isPlainObject(value)) {
    errors.push(
      `${what} ${because} needs ownerDecision { words, recordedAt } — the owner's own words, never an agent's summary of them`
    )
    return
  }
  requireText(value.words, `${what}.ownerDecision.words`, errors)
  if (typeof value.recordedAt !== 'string' || Number.isNaN(Date.parse(value.recordedAt))) {
    errors.push(`${what}.ownerDecision.recordedAt must be an ISO date string (found ${describe(value.recordedAt)})`)
  }
}

/**
 * A `comparison` names the incumbent a promise is a difference from, so the claim carries evidence
 * that somebody else does not already do this. Its truth is recon's business and a human reads it
 * (`.cursor/rules/app-depth.mdc`); the schema only refuses a comparison that names nothing.
 */
function validateComparison(value, what, errors) {
  if (!isPlainObject(value)) {
    errors.push(`${what}.comparison must be { incumbent, limitation, source } (found ${describe(value)})`)
    return
  }
  for (const field of ['incumbent', 'limitation', 'source']) {
    const field_ = value[field]
    if (typeof field_ !== 'string' || !field_.trim()) {
      errors.push(`${what}.comparison.${field} is required and must be a non-empty string (found ${describe(field_)})`)
    } else if (matchesPlaceholder(field_.trim())) {
      errors.push(`${what}.comparison.${field} must name the real incumbent, not a placeholder (found "${field_.trim()}")`)
    }
  }
}

/**
 * The `jobCoverage` object: each of `before`, `during` and `after` resolves to a claim in this
 * contract or carries a written refusal.
 *
 * The shape is checked here because it is a fact about the file, not about a slot: a stage that
 * points at a claim id nobody declared, or a refusal with no reason, is a broken contract on any
 * slot. Whether the three stages are *present at all* is the baseline-scoped half
 * (`depthFindings` below), because a contract that predates the rule must carry that finding as a
 * note rather than a schema refusal. `during` is the act the app is named for, so it can never be
 * refused — refusing it would say there is no app. `after` may be refused, but only by the owner in
 * his own words, because refusing it gives up the handoff (`OWNER_DECIDER`); `before` is the scout's
 * to refuse with a reason.
 */
function validateJobCoverage(coverage, seenIds, errors) {
  if (coverage === undefined) return
  if (!isPlainObject(coverage)) {
    errors.push(
      `jobCoverage must be an object with before, during and after, each a { claimId } or a { refusal } (found ${describe(coverage)})`
    )
    return
  }

  for (const key of Object.keys(coverage)) {
    if (!JOB_STAGES.includes(key)) {
      errors.push(`jobCoverage.${key} is not a job stage; the stages are ${JOB_STAGES.join(', ')}`)
    }
  }

  for (const stage of JOB_STAGES) {
    const entry = coverage[stage]
    if (entry === undefined) continue
    const what = `jobCoverage.${stage}`
    if (!isPlainObject(entry)) {
      errors.push(`${what} must be { claimId } or { refusal: { reason, decidedBy, recordedAt } } (found ${describe(entry)})`)
      continue
    }
    const hasClaim = entry.claimId !== undefined
    const hasRefusal = entry.refusal !== undefined
    if (hasClaim === hasRefusal) {
      errors.push(`${what} must be exactly one of { claimId } or { refusal } (found ${describe(entry)})`)
      continue
    }

    if (hasClaim) {
      if (typeof entry.claimId !== 'string' || !entry.claimId.trim()) {
        errors.push(`${what}.claimId must be the id of a claim in this contract (found ${describe(entry.claimId)})`)
      } else if (!seenIds.has(entry.claimId)) {
        errors.push(
          `${what}.claimId "${entry.claimId}" does not name a claim in this contract; a stage that points at nothing is not coverage`
        )
      }
      continue
    }

    if (stage === 'during') {
      errors.push(
        `${what} is a refusal, but "during" is the act the app is named for and can never be refused; refusing it says there is no app`
      )
    }
    const refusal = entry.refusal
    if (!isPlainObject(refusal)) {
      errors.push(`${what}.refusal must be { reason, decidedBy, recordedAt } (found ${describe(refusal)})`)
      continue
    }
    // `reason` carries the "real text, not a label" rule every free-text field in this contract
    // carries — a bare `none` or `n/a` is a formality, not a reason — so it goes through the same
    // `requireText` helper as `promise` and `narrowedTo` rather than a second, weaker check. What
    // this cannot catch, stated rather than implied: a *sentence* that is not true. No predicate
    // reads sincerity; `.cursor/rules/app-depth.mdc`'s answer is that a human audits every refusal,
    // so this removes the placeholder token and not the bad-faith sentence.
    requireText(refusal.reason, `${what}.refusal.reason`, errors)
    for (const field of ['decidedBy', 'recordedAt']) {
      if (typeof refusal[field] !== 'string' || !refusal[field].trim()) {
        errors.push(
          `${what}.refusal.${field} is required: a refusal is a sentence with a reason, a decider and a date (found ${describe(refusal[field])})`
        )
      }
    }

    // `after` is the stage this contract exists for. The handoff is the most valuable capability it
    // promises — the research in `.cursor/rules/app-depth.mdc` ranks export/backup/data portability
    // first — so refusing it is not a scheduling note a scout writes; it is a decision to ship
    // without the handoff. It therefore needs the owner's own words, in the *same* `ownerDecision`
    // shape a narrowed or dropped `core` promise uses, validated by the same helper so the two paths
    // cannot drift apart. `before` remains the scout's to refuse with its own reason.
    if (stage === 'after') {
      if (refusal.decidedBy !== OWNER_DECIDER) {
        errors.push(
          `${what}.refusal.decidedBy must be "${OWNER_DECIDER}", not ${describe(refusal.decidedBy)}: refusing the after stage gives up the handoff the job is finished with, so the decision is the owner's, recorded in his own words (.cursor/rules/app-depth.mdc)`
        )
      }
      validateOwnerDecision(
        refusal.ownerDecision,
        `${what}.refusal`,
        errors,
        'refuses the after stage, which gives up the handoff the job is finished with, so it'
      )
    }
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

  // The depth fields are optional so every contract that parsed before this rule still parses; when
  // they are present they must name a real stage or a real family, because a typo would silently
  // excuse a promise from the floor rather than reporting it.
  if (claim.jobStage !== undefined && !JOB_STAGES.includes(claim.jobStage)) {
    errors.push(`${label}.jobStage must be one of: ${JOB_STAGES.join(', ')} (found ${describe(claim.jobStage)})`)
  }
  if (claim.depthFamily !== undefined && !DEPTH_FAMILIES.includes(claim.depthFamily)) {
    errors.push(
      `${label}.depthFamily must be one of the closed set: ${DEPTH_FAMILIES.join(', ')} (found ${describe(claim.depthFamily)})`
    )
  }
  if (claim.comparison !== undefined) validateComparison(claim.comparison, label, errors)

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

  return { id, weight, status, jobStage: claim.jobStage, depthFamily: claim.depthFamily, proof: claim.proof }
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
      `reconRef must be docs/research/<file>.md, docs/ideation/<file>.md, or apps/<date>-<slug>/docs/<file>.md with an optional #section (found ${describe(value.reconRef)})`
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

  // After every claim id is known, so a stage pointing at a claim declared later in the file still
  // resolves. A refusal is validated here too: it is a sentence with a reason, a decider and a date.
  validateJobCoverage(value.jobCoverage, seenIds, errors)

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

/**
 * The depth half of the contract: does the promise list cover the user's whole job, and does it
 * contain at least one handoff whose proof artifact actually carries the family's named token.
 *
 * This is separate from `validateDifferentiatorRefs` because its findings are **baseline-scoped**:
 * a slot that predates the rule carries them as a note rather than a red row
 * (`scripts/core/baseline.mjs`, whose derivation the core applies). The shape facts — a stage
 * pointing at nothing, a refused `during`, a refusal missing its reason — are *not* here; they are
 * schema errors in `parseDifferentiators`, because they are wrong on any slot, gated or not.
 *
 * Four findings are produced, and each names what it measured:
 *
 *   * no `jobCoverage` at all — the contract never said what the user does before, during and after;
 *   * a `jobCoverage` missing a stage — coverage is all three stages or it is not coverage;
 *   * no shipped claim in a connective family — the raw-MVP shape the rule exists to stop;
 *   * a connective claim whose proof artifact lacks that family's token — the measured
 *     `lawn-round-log` shape, where `domain/Csv.kt` builds a file and nothing carries it off the
 *     phone.
 *
 * `readFile`/`isFile` are injected so the fixtures can drive every leg without a slot on disk.
 */
export function depthFindings(value, { slotDir, isFile = () => false, readFile = () => '' } = {}) {
  const findings = []
  const claims = Array.isArray(value?.claims) ? value.claims : []
  const coverage = value?.jobCoverage

  if (coverage === undefined) {
    findings.push(
      'no jobCoverage: the contract does not say what the user does before, during and after the ' +
        "app's core act, so the handoffs the job actually has are unaccounted for. Author it at recon, " +
        'with each stage resolving to a claim or a written refusal (.cursor/rules/app-depth.mdc)'
    )
  } else if (isPlainObject(coverage)) {
    for (const stage of JOB_STAGES) {
      if (coverage[stage] === undefined) {
        findings.push(
          `jobCoverage.${stage} is absent: the contract must cover before, during and after the core act, each with a claim or a written refusal`
        )
      }
    }
  }

  const shippedConnective = claims.filter(
    (claim) => claim?.status === 'shipped' && CONNECTIVE_FAMILIES.includes(claim?.depthFamily)
  )
  if (shippedConnective.length === 0) {
    findings.push(
      `no shipped claim names a connective depth family (one of ${CONNECTIVE_FAMILIES.join(', ')}): ` +
        'the app has no handoff, so the job cannot be finished with — the middle of the job alone is ' +
        'the defect this rule exists to stop'
    )
  } else {
    for (const claim of shippedConnective) {
      const family = claim.depthFamily
      const tokens = FAMILY_TOKENS[family] || []
      const label = `claim "${claim.id}" (${family})`
      const proof = claim.proof
      if (!isPlainObject(proof) || typeof proof.ref !== 'string') {
        findings.push(
          `${label} is the handoff this contract rests on, but its proof does not name an artifact to read the ${family} token from`
        )
        continue
      }
      const ref = proof.ref.split('\\').join('/')
      const file = slotDir ? `${slotDir}/${ref}` : ref
      if (!isFile(file)) {
        findings.push(
          `${label}: the proof artifact ${ref} does not resolve inside the slot, so its ${family} token cannot have been observed`
        )
        continue
      }
      if (proof.type === 'capture') {
        findings.push(
          `${label}: a captured frame cannot carry the ${family} token this floor is proved by (${tokens.join(', ')}); point the handoff at the code, test or resource that performs it`
        )
        continue
      }
      const text = readFile(file)
      if (!tokens.some((token) => text.includes(token))) {
        findings.push(
          `${label}: ${ref} contains none of the ${family} token(s) ${tokens.join(', ')}, so the ` +
            'artifact exists while the handoff does not — the measured lawn-round-log shape'
        )
      }
    }
  }

  return findings
}
