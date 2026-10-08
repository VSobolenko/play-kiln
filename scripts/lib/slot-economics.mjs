/**
 * The slot economics contract: the plan a slot must state before it is allowed to
 * exist, and the capability pairing that gives the plan teeth.
 *
 * Why this exists: the factory's first real product slot shipped with
 * `capabilities.json = {"schemaVersion":1,"enabled":[]}` — no billing, no ads, no
 * revenue mechanism at all — and every existing gate still returned PASS.
 * `docs/monetization-strategy.md` §3.6 already required an archetype before UI
 * design and §5 item 5 already required numeric kill criteria before shipping, but
 * both were prose that no program read. This module is that prose made executable:
 * `scripts/new-slot.mjs` refuses to scaffold without a valid plan and
 * `scripts/check-economics.mjs` re-checks it at verify time.
 *
 * `requiredCapabilitiesFor` is the teeth. A plan that claims it will earn is only
 * credible if the capability that can collect the money is actually enabled; an
 * archetype whose revenue mechanism is disabled is a plan that cannot pay, which is
 * exactly the state the first slot shipped in.
 */

export const SCHEMA_VERSION = 1

/** Minimum length of a free-text plan field; a one-word answer is not a plan. */
export const MIN_PLAN_TEXT_LENGTH = 12

/** The four archetypes the factory offers. `subscription` is banned (owner, 2026-10-01): recurring
 *  billing needs server-side validation and drags refund, cancellation and grace-period duties into
 *  the slot. Its absence here is the enforcement — see docs/slot-economics.md. */
export const ARCHETYPES = Object.freeze([
  'purchase_led_single_unlock',
  'purchase_led_repeat_sales',
  'ads_led_utility',
  'purchase_only_no_ads'
])

/**
 * Which capability each archetype needs enabled at verify time.
 *
 * `purchase_only_no_ads` needs none: the price of a paid app is set in Play
 * Console, not by an in-app capability, so the gate's only demand of it is
 * `priceUsd > 0`.
 */
const ARCHETYPE_CAPABILITIES = Object.freeze({
  purchase_led_single_unlock: ['billing'],
  purchase_led_repeat_sales: ['billing'],
  ads_led_utility: ['ads'],
  purchase_only_no_ads: []
})

/** The free-text plan fields, each required to read as a sentence rather than a label. */
export const REQUIRED_STRING_FIELDS = Object.freeze([
  'payingUser',
  'paidUnit',
  'momentOfPayment',
  'distinctnessSentence',
  'day1RevenuePath'
])

/**
 * Text that means "not written yet". A field that matches any of these is rejected
 * even when it is long enough to pass the length rule, because a plan nobody filled
 * in is the prose-that-checks-nothing defect this whole gate exists to close.
 */
export const PLACEHOLDER_PATTERNS = Object.freeze([
  /^(?:tbd|todo|n\/a|none|unknown|placeholder)$/i,
  /example\.com/i,
  /<[^<>]+>/,
  /lorem/i,
  /\bx{3,}\b/i
])

/**
 * Where a plan may point for evidence. `docs/research/` holds factory-wide measured
 * facts; `docs/ideation/` holds an open proposal still in the queue; and after scaffold
 * the proposal moves into `apps/<date>-<slug>/docs/`, so that path is accepted too.
 * A plan that cites only a *method* record — as all five plans in
 * `docs/ideation/2026-09-26-five-recipes.md` first did — points at a file that documents
 * how the market was measured rather than what was measured for that niche, and that is
 * a weaker link than this gate implies.
 *
 * Accepting these prefixes cannot create a dangling reference: `check-economics.mjs`
 * resolves the cited file on disk and BLOCKS when it is gone, so deleting the proposal
 * that justifies a slot breaks that slot's money gate.
 */
const EVIDENCE_REF_PATTERN =
  /^(?:docs\/(?:research|ideation)|apps\/\d{4}-\d{2}-\d{2}-[a-z0-9]+(?:-[a-z0-9]+)*\/docs)\/[a-z0-9._-]+\.md(#.+)?$/

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

function validatePlanText(value, errors) {
  for (const field of REQUIRED_STRING_FIELDS) {
    const raw = value[field]
    if (raw === undefined || raw === null) {
      errors.push(`${field} is required and must be a string`)
      continue
    }
    if (typeof raw !== 'string') {
      errors.push(`${field} must be a string (found ${typeof raw})`)
      continue
    }
    const trimmed = raw.trim()
    if (!trimmed) {
      errors.push(`${field} must not be empty`)
      continue
    }
    if (matchesPlaceholder(trimmed)) {
      errors.push(`${field} must be a real plan, not a placeholder (found "${trimmed}")`)
    }
    if (trimmed.length < MIN_PLAN_TEXT_LENGTH) {
      errors.push(
        `${field} must be at least ${MIN_PLAN_TEXT_LENGTH} characters so it states a plan, not a label (found ${trimmed.length})`
      )
    }
  }
}

function validatePrice(value, errors) {
  const archetype = value.archetype
  // Only ads_led_utility may be free; every other archetype has to name a price it
  // can collect. A paid archetype at 0 is the "factory with no revenue mechanism"
  // state restated as a number.
  const adsLed = archetype === 'ads_led_utility'
  if (typeof value.priceUsd !== 'number' || !Number.isFinite(value.priceUsd)) {
    errors.push(`priceUsd must be a finite number (found ${describe(value.priceUsd)})`)
    return
  }
  if (!adsLed && value.priceUsd <= 0) {
    errors.push(
      `priceUsd must be greater than 0 for archetype "${archetype}"; a paid archetype needs a price, because ${value.priceUsd} cannot earn`
    )
  }
  if (adsLed && value.priceUsd < 0) {
    errors.push(`priceUsd must not be negative for ads_led_utility (found ${value.priceUsd})`)
  }
}

function validateEvidenceRef(value, errors) {
  const raw = value.evidenceRef
  if (typeof raw !== 'string') {
    errors.push(
      'evidenceRef is required and must be a string like docs/research/<file>.md#<section> or docs/ideation/<file>.md#<section>'
    )
    return
  }
  const trimmed = raw.trim()
  if (!EVIDENCE_REF_PATTERN.test(trimmed)) {
    errors.push(
      `evidenceRef must be docs/research/<file>.md, docs/ideation/<file>.md, or apps/<date>-<slug>/docs/<file>.md with an optional #section (found "${trimmed}")`
    )
    return
  }
  if (matchesPlaceholder(trimmed)) {
    errors.push(`evidenceRef must point at real research, not a placeholder (found "${trimmed}")`)
  }
}

function validateKillCriteria(value, errors) {
  const criteria = value.killCriteria
  if (!isPlainObject(criteria)) {
    errors.push('killCriteria must be an object with windowDays, minInstalls, and minRevenueUsd')
    return
  }
  const window = criteria.windowDays
  if (!Number.isInteger(window)) {
    errors.push(`killCriteria.windowDays must be an integer (found ${describe(window)})`)
  } else if (window < 7 || window > 180) {
    errors.push(`killCriteria.windowDays must be between 7 and 180 (found ${window})`)
  }

  const installs = criteria.minInstalls
  if (!Number.isInteger(installs)) {
    errors.push(`killCriteria.minInstalls must be an integer (found ${describe(installs)})`)
  } else if (installs < 1) {
    errors.push(
      `killCriteria.minInstalls must be at least 1: a kill criterion of zero is not a criterion, because it can never be missed (found ${installs})`
    )
  }

  const revenue = criteria.minRevenueUsd
  if (!Number.isInteger(revenue)) {
    errors.push(`killCriteria.minRevenueUsd must be an integer (found ${describe(revenue)})`)
  } else if (revenue < 1) {
    errors.push(
      `killCriteria.minRevenueUsd must be at least 1: a kill criterion of zero is not a criterion, because it can never be missed (found ${revenue})`
    )
  }
}

function validateAssumptions(value, errors) {
  const assumptions = value.assumptionsUnverified
  if (!Array.isArray(assumptions)) {
    errors.push(`assumptionsUnverified must be an array of strings (found ${describe(assumptions)})`)
    return
  }
  if (assumptions.length === 0) {
    errors.push(
      'assumptionsUnverified must contain at least one honest unknown; an empty array is not a plan'
    )
    return
  }
  let usable = 0
  assumptions.forEach((entry, index) => {
    if (typeof entry !== 'string' || !entry.trim()) {
      errors.push(`assumptionsUnverified[${index}] must be a non-empty string`)
      return
    }
    if (!matchesPlaceholder(entry.trim())) usable += 1
  })
  if (usable === 0) {
    errors.push('assumptionsUnverified must contain at least one entry that is not a placeholder')
  }
}

/**
 * Validate a parsed `economics.json`. Returns `{ ok: true, value }` or
 * `{ ok: false, errors }` and never throws for invalid input, so a caller can print
 * every problem at once instead of failing on the first.
 *
 * A string argument is accepted and JSON-parsed for convenience; a parse failure is
 * reported as an error rather than thrown.
 */
export function parseEconomics(json) {
  let value = json
  if (typeof value === 'string') {
    try {
      value = JSON.parse(value.replace(/^\uFEFF/, ''))
    } catch (error) {
      return { ok: false, errors: [`economics is not valid JSON: ${error.message}`] }
    }
  }
  if (!isPlainObject(value)) {
    return { ok: false, errors: ['economics must be a JSON object'] }
  }

  const errors = []
  if (value.schemaVersion !== SCHEMA_VERSION) {
    errors.push(`schemaVersion must be exactly ${SCHEMA_VERSION} (found ${describe(value.schemaVersion)})`)
  }
  if (!ARCHETYPES.includes(value.archetype)) {
    errors.push(`archetype must be one of: ${ARCHETYPES.join(', ')} (found ${describe(value.archetype)})`)
  }
  validatePlanText(value, errors)
  validatePrice(value, errors)
  validateEvidenceRef(value, errors)
  validateKillCriteria(value, errors)
  validateAssumptions(value, errors)

  return errors.length === 0 ? { ok: true, value } : { ok: false, errors }
}

/**
 * The capability ids an archetype needs enabled. Throwing on an unknown archetype is
 * deliberate: returning an empty list would silently under-enforce the gate, which
 * is the failure mode this module exists to remove.
 */
export function requiredCapabilitiesFor(archetype) {
  if (!Object.prototype.hasOwnProperty.call(ARCHETYPE_CAPABILITIES, archetype)) {
    throw new Error(
      `unknown archetype: ${describe(archetype)}; valid archetypes: ${ARCHETYPES.join(', ')}`
    )
  }
  return [...ARCHETYPE_CAPABILITIES[archetype]]
}

/**
 * Check a plan against a slot's parsed `capabilities.json`.
 *
 * Only a missing required capability is an error; enabled capabilities beyond what
 * the archetype demands are allowed, because ads and a remove-ads unlock coexist
 * legitimately (`docs/monetization-strategy.md` §3.6).
 */
export function validateEconomicsAgainstCapabilities(economics, capabilitiesJson) {
  const archetype = isPlainObject(economics) ? economics.archetype : undefined
  if (!ARCHETYPES.includes(archetype)) {
    return {
      ok: false,
      errors: [
        `archetype must be one of: ${ARCHETYPES.join(', ')} before capabilities can be paired (found ${describe(archetype)})`
      ]
    }
  }

  const required = requiredCapabilitiesFor(archetype)
  if (required.length === 0) return { ok: true, value: economics }

  const enabled = Array.isArray(capabilitiesJson?.enabled) ? capabilitiesJson.enabled : null
  if (enabled === null) {
    return {
      ok: false,
      errors: ['capabilities.json must contain an "enabled" array for the archetype pairing to be evaluated']
    }
  }

  const errors = []
  for (const capability of required) {
    if (!enabled.includes(capability)) {
      errors.push(
        `archetype "${archetype}" requires the "${capability}" capability; capabilities.json enables [${enabled.join(', ')}]; add "${capability}" to capabilities.json or choose another archetype`
      )
    }
  }
  return errors.length === 0 ? { ok: true, value: economics } : { ok: false, errors }
}
