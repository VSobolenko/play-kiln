// The naming decision, as a slot check: a slot records the name it ships, derived from its job,
// audience and market, beside its money plan and its promise list — or it can say no such decision
// was made.
//
// Why this exists. `scripts/lib/slot-economics.mjs` forced the monetization decision to be recorded
// and gated before the UI, and `scripts/lib/slot-differentiators.mjs` did the same for the reasons
// the app was chosen. The **name** had no contract at all: `scripts/new-slot.mjs --name "<Title>"`
// took free text and nothing read it, so an agent building a utility could call it `Calculator` and
// spend Play's strongest ranking field on the head term the incumbent already owns. The measured
// Play rules are `docs/research/2026-10-01-play-title-policy-and-aso.md`; the methodology is
// `docs/research/2026-10-01-app-naming-methodology.md`; the contract is
// `scripts/lib/slot-naming.mjs`, and this module imports it rather than restating a rule
// (`.cursor/rules/verification-discipline.mdc`, "One source of truth, imported — never restated").
//
// What it refuses to claim: it reads JSON on disk, never Play and never the pixels. It cannot say a
// name is good, memorable or free of trademark conflict; it can say the decision was recorded, that
// the name satisfies the mechanical rules Play enforces, that it was derived from candidates rather
// than invented, and that the pack ships the same name that was approved. The judgement that
// separates a distinctive name from an ugly one stays with the owner, at the phase-1 review.
import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { applicationIdForSlug } from '../lib/namespaces.mjs'
import { parseNaming, validateNamingAgainstListing } from '../lib/slot-naming.mjs'
import { expectTrue, fail, guard, pass, ROOT, runFixtures, unverified } from './lib.mjs'

/** Slot-relative, POSIX-shaped path, for a message a human reads. */
const rel = (file) => path.relative(ROOT, file).split(path.sep).join('/')

const readText = (file) => readFileSync(file, 'utf8')

/**
 * The naming facts about one slot.
 *
 * Two rules the check obeys, both recorded failures elsewhere in this repository:
 *
 *   * **A missing input is never a pass.** A slot with no `naming.json` reports `UNVERIFIED` naming
 *     the path, because measuring nothing must not read as a green line — and the message says in
 *     the same breath that deleting the file must not switch the check off, mirroring
 *     `checkDifferentiators`'s wording of the same hazard.
 *   * **A pack that does not exist yet is not a defect.** A slot in development has no
 *     `store/listing.json`; the check says so in its PASS message instead of failing the slot for a
 *     stage it has not reached.
 */
export function namingChecks(slotDir) {
  return [
    guard('slot.naming', () => {
      const file = path.join(slotDir, 'naming.json')
      if (!existsSync(file)) {
        return unverified(
          'slot.naming',
          `no ${rel(file)}; this slot records no naming decision, so the name it ships is unaccounted ` +
            'for. A missing contract is not a pass, and deleting this file must not switch the check off'
        )
      }

      let parsed
      try {
        parsed = parseNaming(readText(file))
      } catch (error) {
        return fail('slot.naming', `${rel(file)} could not be read: ${error.message}`)
      }
      if (!parsed.ok) return fail('slot.naming', parsed.errors.join('; '))

      const { title, archetype, candidates } = parsed.value
      const archetypes = new Set(candidates.map((candidate) => candidate.archetype)).size
      const summary =
        `${rel(file)}: title "${title}" via archetype ${archetype}, ` +
        `${candidates.length} candidate(s) across ${archetypes} archetype(s)`

      const listingFile = path.join(slotDir, 'store', 'listing.json')
      if (!existsSync(listingFile)) {
        return pass(
          'slot.naming',
          `${summary}; no ${rel(listingFile)} yet, so there is no pack title to compare with it — a ` +
            'slot in development has no pack, and the comparison is made once one exists'
        )
      }

      let listing
      try {
        listing = JSON.parse(readText(listingFile).replace(/^\uFEFF/, ''))
      } catch (error) {
        return fail('slot.naming', `${rel(listingFile)} is not valid JSON: ${error.message}`)
      }

      const mismatches = validateNamingAgainstListing(parsed.value, listing)
      if (mismatches.length > 0) return fail('slot.naming', `${summary}; ${mismatches.join('; ')}`)

      return pass('slot.naming', `${summary}; the pack's titles agree with it`)
    })
  ]
}

/**
 * The sensor driven with contracts whose verdict is known, including the owner's own defect.
 *
 * The suite is deliberately able to fail: the anti-generic rule is exercised by `Calculator` — the
 * name the instruction names — and by `My Calculator` and `CALCULATOR`, while the negative control
 * asserts that a legitimately multi-word descriptive title is not caught by it. A sensor that fires
 * on everything is as useless as one that fires on nothing, and this set proves it does neither.
 */
export function namingFixtures() {
  return guard('naming.fixtures', () => {
    const VALID_TITLE = 'Sunstone: Lawn Round Log'
    const VALID_SLUG = 'lawn-round-log'

    const FILLER_CANDIDATES = [
      { name: 'Round Ledger', archetype: 'descriptive', score: 74, why: 'Names the record a mower keeps, in the words the job already uses.' },
      { name: 'Kerbstone', archetype: 'metaphorical', score: 71, why: 'A concrete roadside image for a round that is driven rather than browsed.' },
      { name: 'Mowpath', archetype: 'coined', score: 64, why: 'A short inventable token, but it teaches a new user nothing on its own.' },
      { name: 'LawnRoute', archetype: 'compound', score: 62, why: 'Two real words fused; clear, but the join reads like a stuffed keyword.' },
      { name: 'Rounds', archetype: 'evocative', score: 30, why: 'Evokes the round without saying what is done with it, so it cannot be searched.' }
    ]

    const candidatesFor = (title) => [
      {
        name: title,
        archetype: 'carrier_job_noun',
        score: 85,
        why: 'An ownable carrier plus the job noun a mower actually types, inside the 30-character cap.'
      },
      ...FILLER_CANDIDATES
    ]

    /** A complete, correct contract. Every failing fixture below is this object with one thing broken. */
    const validNaming = (overrides = {}) => ({
      schemaVersion: 1,
      title: VALID_TITLE,
      slug: VALID_SLUG,
      applicationId: applicationIdForSlug(VALID_SLUG),
      archetype: 'carrier_job_noun',
      audience: 'Solo mowers who keep their own round book on a phone rather than in a spreadsheet.',
      rationale: 'The carrier is ownable and the job noun carries the phrase a mower types into Play search.',
      searchPhrases: ['lawn round log', 'lawn care customer tracker'],
      localizedTitles: { 'ru-RU': 'Sunstone: журнал обхода' },
      candidates: candidatesFor(VALID_TITLE),
      rejected: [
        {
          name: 'Lawn Calculator',
          reason: 'It is the category noun plus a modifier, so it competes for the head term the leader owns.'
        }
      ],
      collisionCheck: 'Searched Play for "Sunstone" and "Lawn Round Log" on 2026-10-01; no live app shares the title.',
      localisationCheck: 'The carrier survives transliteration, and "журнал обхода" carries no unfortunate meaning in ru.',
      evidence: 'docs/research/2026-10-01-app-naming-methodology.md#3',
      ...overrides
    })

    const errorsOf = (value) => {
      const parsed = parseNaming(JSON.stringify(value))
      return parsed.ok ? [] : parsed.errors
    }
    const mentions = (errors, phrase) => errors.some((error) => error.includes(phrase))

    const cases = []

    cases.push([
      'a complete carrier + job-noun contract parses',
      () => {
        const parsed = parseNaming(JSON.stringify(validNaming()))
        expectTrue(parsed.ok, `a valid contract was refused: ${parsed.ok ? '' : parsed.errors.join(' | ')}`)
        expectTrue(parseNaming(`\uFEFF${JSON.stringify(validNaming())}`).ok, 'a BOM-prefixed contract was refused')
      }
    ])

    cases.push([
      'the owner\'s own defect: the bare category noun "Calculator" fails the anti-generic rule',
      () => {
        const errors = errorsOf(validNaming({ title: 'Calculator', candidates: candidatesFor('Calculator') }))
        expectTrue(mentions(errors, 'bare category noun'), `the generic rule did not fire: ${errors.join(' | ')}`)
      }
    ])

    cases.push([
      'an empty modifier — "My Calculator" — fails the banned-affix rule',
      () => {
        const errors = errorsOf(validNaming({ title: 'My Calculator', candidates: candidatesFor('My Calculator') }))
        expectTrue(mentions(errors, 'empty modifier'), `the affix rule did not fire: ${errors.join(' | ')}`)
      }
    ])

    cases.push([
      '"CALCULATOR" fails as an ALL-CAPS title',
      () => {
        const errors = errorsOf(validNaming({ title: 'CALCULATOR', candidates: candidatesFor('CALCULATOR') }))
        expectTrue(mentions(errors, 'ALL CAPS'), `the ALL-CAPS rule did not fire: ${errors.join(' | ')}`)
      }
    ])

    cases.push([
      'a 31-character title fails Play\'s 30-character cap',
      () => {
        const long = 'Round Log: Lawn Care Rounds'.padEnd(31, 'x')
        expectTrue(long.length === 31, `the fixture title is ${long.length} characters, not 31`)
        const errors = errorsOf(validNaming({ title: long, candidates: candidatesFor(long) }))
        expectTrue(mentions(errors, `at most ${30}`), `the length rule did not fire: ${errors.join(' | ')}`)
      }
    ])

    cases.push([
      'a candidates array of 2 fails the derivation rule',
      () => {
        const errors = errorsOf(validNaming({ candidates: candidatesFor(VALID_TITLE).slice(0, 2) }))
        expectTrue(mentions(errors, 'candidates'), `the candidates rule did not fire: ${errors.join(' | ')}`)
      }
    ])

    cases.push([
      'a chosen title absent from its own candidates fails',
      () => {
        const errors = errorsOf(validNaming({ candidates: FILLER_CANDIDATES }))
        expectTrue(mentions(errors, 'chosen name'), `the derivation rule did not fire: ${errors.join(' | ')}`)
      }
    ])

    cases.push([
      'an applicationId that disagrees with the slug fails',
      () => {
        const errors = errorsOf(validNaming({ applicationId: 'com.gexetr.wrong' }))
        expectTrue(mentions(errors, 'applicationId'), `the applicationId rule did not fire: ${errors.join(' | ')}`)
      }
    ])

    cases.push([
      'a localised title over the cap fails',
      () => {
        const errors = errorsOf(validNaming({ localizedTitles: { 'ru-RU': 'Солнечный камень: журнал обхода газона' } }))
        expectTrue(mentions(errors, 'localizedTitles'), `the localised-length rule did not fire: ${errors.join(' | ')}`)
      }
    ])

    cases.push([
      'a pack that ships a different title than the approved name fails',
      () => {
        const approved = validNaming()
        const listing = {
          defaultLocale: 'en-US',
          locales: {
            'en-US': { title: 'Round Ledger' },
            'ru-RU': { title: 'Sunstone: журнал обхода' }
          }
        }
        const problems = validateNamingAgainstListing(approved, listing)
        expectTrue(problems.length > 0, 'a listing with a different title was accepted')
        expectTrue(
          problems.some((problem) => problem.includes('approved name')),
          `the mismatch was not named as a pack/approval defect: ${problems.join(' | ')}`
        )
      }
    ])

    cases.push([
      'a pack whose titles agree produces no mismatch, including the localised one',
      () => {
        const approved = validNaming()
        const listing = {
          defaultLocale: 'en-US',
          locales: {
            'en-US': { title: VALID_TITLE },
            'ru-RU': { title: 'Sunstone: журнал обхода' }
          }
        }
        const problems = validateNamingAgainstListing(approved, listing)
        expectTrue(problems.length === 0, `an agreeing pack was refused: ${problems.join(' | ')}`)
      }
    ])

    cases.push([
      'NEGATIVE CONTROL: a multi-word descriptive title is not a bare category noun',
      () => {
        const descriptive = 'Round Log: Lawn Care Rounds'
        const errors = errorsOf(validNaming({ title: descriptive, candidates: candidatesFor(descriptive) }))
        expectTrue(
          !mentions(errors, 'bare category noun'),
          `the generic rule fired on a legitimate job title: ${errors.join(' | ')}`
        )
        expectTrue(
          parseNaming(JSON.stringify(validNaming({ title: descriptive, candidates: candidatesFor(descriptive) }))).ok,
          `a legitimate descriptive title was refused outright: ${errors.join(' | ')}`
        )
      }
    ])

    return runFixtures('naming.fixtures', cases)
  })
}
