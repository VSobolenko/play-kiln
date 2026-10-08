// The device-capability declaration every proposal carries, read as a check.
//
// WHY THIS EXISTS. The owner's observation after reading recon for two slots: the factory only ever
// proposes apps that use "a couple of buttons", while the phone in his hand has a camera, a photo
// picker, a gallery, media, sensors. Two read-only workers measured it in the session this was
// written. Of 20 proposed ideas across 13 documents, 16 are "the user types something and the app
// stores it"; the measured zeros are camera 0, sensors 0, media playback 0, location 0, widget 0.
// The steering sentence was the buildability line of `.cursor/rules/ideation-gate.mdc` ("prefer no
// server, no account, no dangerous permission, no camera, no ML, offline by construction"), and a
// scout refused a camera-shaped job citing a brief that "explicitly excludes" it
// (`docs/ideation/2026-10-02-assistive-and-communication.md`).
//
// The deeper cause is STRUCTURAL rather than a preference, and that is the defect this check is
// written against: no recon contract had any field for a device capability. `docs/slot-economics.md`,
// `docs/slot-differentiators.md`, `docs/app-naming.md` and their `scripts/lib/slot-*.mjs` parse no
// such field, and the whole "capability" vocabulary is five non-device ids under `capabilities/`. A
// scout therefore had **nowhere to write "this job needs the camera"** even if it wanted to. So this
// check does not loosen the surface — how far to open it (camera? sensors? location? games?) is the
// owner's decision and is deliberately not made here. It makes the existing answer DECLARED rather
// than arrived at by silence: a proposal that uses no device capability must say so one line at a
// time, so "none" is a finding the owner can read instead of a default nobody chose. The field is
// stated in `docs/ideation/README.md`, bound to the scout in `harness/roles/scout.md`, asked at
// checkpoint 1 in `docs/ideation-and-money-checklist.md`, and read here.
//
// The shape, one rule: for each of the twelve capabilities below a proposal says `used` or
// `not used`; when it is `used` the line names the API the slot would call and what the job loses
// without it. A capability that requires a runtime permission must also name that permission, so a
// declaration cannot contradict the proposal's own permissions section. The mobile-platform detail
// that makes this a declaration problem rather than a cost problem is stated in the contract: the
// Android Photo Picker (`ActivityResultContracts.PickVisualMedia`) needs no runtime permission at
// all, so "uses the photo picker" costs nothing against the permission-minimisation bar — which is
// exactly why a scout that silently avoided it was not making a permission decision.
//
// TWO HONEST LIMITS, stated rather than implied, because a gate that overclaims is worse than none.
// (1) The contradiction check detects two spellings and only two: an explicit `no <capability>` claim
// in another section of the same document, and the absence of the capability's manifest permission
// token (`CAMERA`, `RECORD_AUDIO`, ...) when one is required. A contradiction written without either
// — a permission named only in an `AndroidManifest` fragment, or a capability used but never
// mentioned in the permissions section and never negated in prose — is invisible here, and a reader
// still has to open the proposal. (2) The check reads prose, so it can say a declaration is present,
// complete and internally consistent; it cannot say the named API is the right one, that the app will
// really call it, or that the job would in fact miss it. That judgement stays with the owner at
// checkpoint 1, exactly as the naming contract's honest limit does.
//
// APPLICABILITY IS DERIVED, NOT HAND-LISTED, and here that is load-bearing rather than tidy. Every
// proposal already on disk was authored before this contract existed and none of them can be
// retrofitted, so `PROPOSAL_CAPABILITY_BASELINE` in `scripts/core/baseline.mjs` makes the finding a
// printed NOTE for a proposal authored before it and a FAIL for one authored at or after it. A gate
// that is red across the tree it was written into teaches the operator to read past it — the defect
// this repository has corrected twice, and the reason `baseline.mjs` exists at all. The date is one
// day later than the newest proposal on disk, because those had already been written and presented
// before the rule existed; the property that matters is that not one existing proposal is gated.
//
// And a missing input is never a pass: `docs/ideation/` holding no proposal document is `UNVERIFIED`,
// not a green line about declarations for nothing, following `scripts/core/spec.mjs`.
//
// ORPHAN PLANS. Successful apps may later leave the factory (`Ctrl+X`) with their own tree, so a
// plan that still sits under `docs/ideation/plans/<slug>.*` after `apps/<date>-<slug>/` exists is
// trash the cut-out developer never sees. `ideas.orphanPlans` fails that leftover. The open queue
// under `docs/ideation/` keeps only proposals that have not been scaffolded yet.
import { existsSync, mkdirSync, mkdtempSync, readdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { SCRATCH_DIR } from '../lib/scratch.mjs'
import { PROPOSAL_CAPABILITY_BASELINE, proposalBaselineApplies } from './baseline.mjs'
import { expectEqual, expectTrue, fail, guard, pass, ROOT, runFixtures, unverified, VERDICT } from './lib.mjs'

/** A plan leftover filename: `<slug>.{economics|differentiators|naming}.json`. */
const PLAN_FILE = /^([a-z0-9]+(?:-[a-z0-9]+)*)\.(economics|differentiators|naming)\.json$/

/** A living product slot directory: `apps/<YYYY-MM-DD>-<slug>/`. */
const SLOT_DIR = /^(\d{4}-\d{2}-\d{2})-(.+)$/

/**
 * Slugs that already have a slot directory under `apps/`, so their ideation plans must have moved.
 *
 * Driven over an arbitrary root so the fixture can inject a leftover without touching the real tree.
 */
export function livingSlotSlugs(root = ROOT) {
  const appsDir = path.join(root, 'apps')
  const slugs = new Set()
  if (!existsSync(appsDir)) return slugs
  for (const entry of readdirSync(appsDir, { withFileTypes: true })) {
    if (!entry.isDirectory()) continue
    const match = entry.name.match(SLOT_DIR)
    if (match) slugs.add(match[2])
  }
  return slugs
}

/**
 * Plan files still under `docs/ideation/plans/` for a slug that already has `apps/<date>-<slug>/`.
 *
 * Each problem names the leftover path and the remedy, so a FAIL is an instruction rather than a
 * label. An absent plans directory is not a problem — there is nothing left to orphan.
 */
export function orphanPlanProblems(root = ROOT) {
  const plansDir = path.join(root, 'docs', 'ideation', 'plans')
  if (!existsSync(plansDir)) return []
  const slugs = livingSlotSlugs(root)
  if (slugs.size === 0) return []
  const problems = []
  for (const name of readdirSync(plansDir)) {
    const match = name.match(PLAN_FILE)
    if (!match) continue
    const slug = match[1]
    if (!slugs.has(slug)) continue
    problems.push(
      `docs/ideation/plans/${name} remains after apps/*-${slug}/ was scaffolded — move it to apps/<slot>/docs/`
    )
  }
  return problems.sort()
}

/**
 * The twelve capabilities every proposal answers, with the API/permission facts a declaration is
 * judged against.
 *
 * `aliases` are the labels a scout may write; the declaration's own label is matched against them,
 * so the wording is free while the SET is closed. `noun` is the word an explicit `no <noun>` claim
 * must use to contradict a `used` answer. `permissions` are the Android manifest tokens the
 * permission-minimisation bar needs to see when the capability is used — an EMPTY list is not an
 * oversight: the Photo Picker, SAF, media playback, share/export and widgets need no runtime
 * permission, and that is the measured platform fact the contract asks the scout to reason with.
 * Motion sensors carry no runtime permission either (an accelerometer or gyroscope is permission
 * free); only activity recognition would need one, and that is a subset this set does not claim.
 */
export const DEVICE_CAPABILITIES = Object.freeze([
  { key: 'camera', label: 'camera', aliases: ['camera'], noun: 'camera', permissions: ['CAMERA'] },
  {
    key: 'photo-picker',
    label: 'photo picker / gallery',
    aliases: ['photo picker', 'gallery', 'photo library'],
    noun: 'photo picker',
    permissions: []
  },
  {
    key: 'media',
    label: 'media playback or capture',
    aliases: ['media playback', 'media capture', 'audio playback', 'video playback', 'media'],
    noun: 'media',
    permissions: []
  },
  { key: 'microphone', label: 'microphone', aliases: ['microphone', 'mic'], noun: 'microphone', permissions: ['RECORD_AUDIO'] },
  { key: 'location', label: 'location', aliases: ['location', 'gps', 'geolocation'], noun: 'location', permissions: ['ACCESS_FINE_LOCATION', 'ACCESS_COARSE_LOCATION'] },
  {
    key: 'motion-sensors',
    label: 'motion sensors',
    aliases: ['motion sensor', 'sensor', 'accelerometer', 'gyroscope', 'step counter'],
    noun: 'motion sensor',
    permissions: []
  },
  {
    key: 'contacts-calendar',
    label: 'contacts / calendar',
    aliases: ['contacts', 'calendar'],
    noun: 'contacts',
    permissions: ['READ_CONTACTS', 'READ_CALENDAR', 'WRITE_CALENDAR']
  },
  {
    key: 'notifications',
    label: 'notifications / reminder',
    aliases: ['notification', 'reminder'],
    noun: 'notification',
    permissions: ['POST_NOTIFICATIONS']
  },
  { key: 'widget', label: 'home-screen widget', aliases: ['widget', 'home screen widget'], noun: 'widget', permissions: [] },
  { key: 'share-export', label: 'share / export', aliases: ['share', 'export'], noun: 'share', permissions: [] },
  { key: 'bluetooth-usb', label: 'Bluetooth / USB', aliases: ['bluetooth', 'usb'], noun: 'bluetooth', permissions: ['BLUETOOTH'] },
  {
    key: 'files-saf',
    label: 'files / SAF',
    aliases: ['file', 'saf', 'storage access framework', 'document picker'],
    noun: 'file picker',
    permissions: []
  }
])

/** The heading a declaration section must carry, matched case-insensitively. */
const DECLARATION_HEADING = /^#{2,6}\s+.*\bdevice capabilit/i

/** Any markdown heading, used to find where a declaration section ends. */
const ANY_HEADING = /^#{1,6}\s+/

/**
 * Documents under `docs/ideation/` that are proposal sets: `<YYYY-MM-DD>-<topic>.md`.
 *
 * `README.md` is the contract, not a proposal, and does not match the date prefix.
 */
export const PROPOSAL_FILE = /^\d{4}-\d{2}-\d{2}-.+\.md$/

/**
 * Dated documents that are deliberately NOT proposals, each with a reason.
 *
 * A proposal set states a paying user; these documents are analyses OF proposals — pricing
 * worksheets, verification reports and head-to-heads — and none proposes a recipe of its own. The
 * list exists rather than a heuristic because the repo's established shape for a scope decision is
 * an escape hatch that is *a file with a reason* (`ADAPTERS_WITHOUT_A_ROLE` in `project.mjs`, the
 * client-secrets exemption file): silent omission is the failure mode, so every entry must name a
 * file that exists and carry a reason, and `ideasChecks` fails if one does not. A future worksheet
 * therefore becomes a visible decision (add it with a reason) instead of a silent skip.
 */
export const NON_PROPOSAL_DOCUMENTS = Object.freeze({
  '2026-10-01-pricing-and-archetype-worksheet.md': 'a pricing worksheet over the archetype table; it proposes no recipe of its own',
  '2026-10-01-verification-of-evidence.md': "a verification report on another document's claims; it proposes no recipe",
  '2026-10-02-lane-pricing-worksheet.md': 'a pricing worksheet over the three lanes; it proposes no recipe',
  '2026-10-02-leader-head-to-head.md': 'a head-to-head comparison of recipes proposed elsewhere; it proposes no recipe',
  '2026-10-02-leader-head-to-head-tap-to-say.md': 'a head-to-head comparison of one recipe proposed elsewhere; it proposes no recipe',
  '2026-10-02-target-vs-kill-floor.md': "an analysis of the plans' kill floors; it proposes no recipe",
  '2026-10-02-verification-lane-b.md': 'a verification report on another lane; it proposes no recipe',
  '2026-10-02-verification-lane-c.md': 'a verification report on another lane; it proposes no recipe',
  '2026-10-05-scout-b-hobby-health-refusal.md': 'a measured refusal of the hobby/narrow-health shelf; it proposes no recipe of its own',
  '2026-10-05-scout-b-money.md': "a money reading of another document's refused candidates; it proposes no recipe"
})

/** Escapes a literal for a `RegExp`, so a capability noun can never be read as a pattern. */
const escapeRegex = (text) => text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

/** The line range a declaration section occupies, and the text outside it. */
export function splitDeclaration(text) {
  const lines = String(text ?? '').split('\n')
  let start = -1
  for (let index = 0; index < lines.length; index += 1) {
    if (DECLARATION_HEADING.test(lines[index])) {
      start = index
      break
    }
  }
  if (start === -1) return { found: false, section: '', body: lines.join('\n') }
  const level = (lines[start].match(/^#+/) || [''])[0].length
  let end = lines.length
  for (let index = start + 1; index < lines.length; index += 1) {
    const heading = lines[index].match(/^(#+)\s/)
    if (heading && heading[1].length <= level) {
      end = index
      break
    }
  }
  const body = [...lines.slice(0, start), ...lines.slice(end)].join('\n')
  return { found: true, section: lines.slice(start + 1, end).join('\n'), body }
}

/**
 * The bullet items of a declaration section, with wrapped continuation lines rejoined.
 *
 * A `used` line is often longer than one source line, and a parser that read only the first would
 * report a real justification as missing — a false positive in the direction that makes an author
 * switch the check off.
 */
export function declarationItems(section) {
  const items = []
  let current = null
  for (const raw of String(section ?? '').split('\n')) {
    const line = raw.replace(/\s+$/, '')
    if (ANY_HEADING.test(line)) {
      if (current !== null) items.push(current)
      current = null
      continue
    }
    const bullet = line.match(/^\s*[-*]\s+(.*)$/)
    if (bullet) {
      if (current !== null) items.push(current)
      current = bullet[1].trim()
      continue
    }
    if (current !== null && line.trim() !== '') current += ` ${line.trim()}`
  }
  if (current !== null) items.push(current)
  return items
}

/** The capability a declaration label names, or null if the label matches none of the closed set. */
export function matchCapability(label) {
  const normalised = String(label ?? '').toLowerCase()
  return DEVICE_CAPABILITIES.find((capability) => capability.aliases.some((alias) => normalised.includes(alias))) ?? null
}

/**
 * The findings for one proposal's text.
 *
 * Pure over its input so the fixture suite can drive every leg without a document on disk. The
 * findings are whole sentences without a file name; the caller prefixes the path, so the same
 * sentence reads correctly in a per-document note and in a gated FAIL.
 */
export function deviceCapabilityFindings(text) {
  const { found, section, body } = splitDeclaration(text)
  if (!found) {
    return [
      `no device-capability declaration: add a '## Device capabilities' section answering ` +
        `'used' or 'not used' for all ${DEVICE_CAPABILITIES.length} capabilities`
    ]
  }

  const findings = []
  const answers = new Map()
  for (const item of declarationItems(section)) {
    const colon = item.indexOf(':')
    if (colon === -1) continue
    const capability = matchCapability(item.slice(0, colon))
    if (!capability) continue
    const rest = item.slice(colon + 1).trim()
    if (answers.has(capability.key)) {
      findings.push(`capability '${capability.label}' is answered more than once`)
      continue
    }
    if (/\bnot\s+used\b/i.test(rest)) {
      answers.set(capability.key, { capability, status: 'not used' })
      continue
    }
    if (!/\bused\b/i.test(rest)) {
      findings.push(`capability '${capability.label}' does not say 'used' or 'not used'`)
      continue
    }
    answers.set(capability.key, { capability, status: 'used' })
    if (!/`[^`]+`/.test(rest)) {
      findings.push(`capability '${capability.label}' is used but names no API in backticks`)
    }
    // The justification is what is left once the status, the backticked API and the separator are
    // removed. A bare "used" with an API and nothing else does not say what the job loses.
    const justification = rest
      .replace(/`[^`]*`/g, ' ')
      .replace(/\bnot\s+used\b/i, ' ')
      .replace(/\bused\b/i, ' ')
      .replace(/^[\s:—–-]+/, '')
      .trim()
    if (justification.length < 20) {
      findings.push(`capability '${capability.label}' is used but does not say what the job loses without it`)
    }
  }

  for (const capability of DEVICE_CAPABILITIES) {
    if (!answers.has(capability.key)) {
      findings.push(`capability '${capability.label}' is not answered used or not used`)
    }
  }

  // Contradiction 1: a `used` answer while ANOTHER section claims the capability is absent. The scan
  // reads the body outside the declaration, so a used line's own prose ("without it there is no
  // camera-based way") cannot contradict itself.
  for (const { capability, status } of answers.values()) {
    if (status !== 'used') continue
    const negation = new RegExp(`\\bno\\s+${escapeRegex(capability.noun)}\\b`, 'i')
    if (negation.test(body)) {
      findings.push(
        `capability '${capability.label}' is declared used while another section says 'no ${capability.noun}'`
      )
    }
  }

  // Contradiction 2: a `used` capability that requires a runtime permission, with none of its
  // manifest tokens named anywhere. The token is matched case-sensitively because Android's
  // permissions are uppercase; the lowercase word "camera" in the declaration is not a manifest
  // permission and must not satisfy this.
  for (const { capability, status } of answers.values()) {
    if (status !== 'used' || capability.permissions.length === 0) continue
    const named = capability.permissions.some((token) => new RegExp(`\\b${escapeRegex(token)}\\b`).test(text))
    if (!named) {
      findings.push(
        `capability '${capability.label}' is used but its manifest permission is not named ` +
          `(expected ${capability.permissions.join(' or ')})`
      )
    }
  }

  return findings
}

/**
 * One proposal document resolved to gated findings or to a pre-baseline note.
 *
 * The baseline is what keeps the check useful in the tree it was written into: none of the proposals
 * already on disk could have carried the field, so they are reported and not gated.
 */
export function evaluateProposalDocument({ name, date, text }) {
  const raw = deviceCapabilityFindings(text)
  if (raw.length === 0) return { findings: [], notes: [] }
  const state = proposalBaselineApplies(date)
  if (state.applies) return { findings: raw.map((finding) => `${name}: ${finding}`), notes: [] }
  return {
    findings: [],
    notes: [`${name}: ${raw.length} finding(s), first: ${raw[0]} (${state.reason})`]
  }
}

/**
 * The allowlist's own integrity, over an injected file list.
 *
 * The escape hatch is a file with a reason, so an entry that names a file which does not exist is a
 * stale exemption that must be removed, and an entry with no reason is a silent one.
 */
export function nonProposalProblems(names, reasonFor = (name) => NON_PROPOSAL_DOCUMENTS[name]) {
  const problems = []
  for (const name of Object.keys(NON_PROPOSAL_DOCUMENTS)) {
    if (!names.includes(name)) problems.push(`the non-proposal allowlist names ${name}, which does not exist — remove the entry`)
    const reason = reasonFor(name)
    if (!reason || !String(reason).trim()) problems.push(`the allowlist entry for ${name} carries no reason`)
  }
  return problems
}

/** The proposal documents under `docs/ideation/`, the non-proposal allowlist excluded. */
export function proposalDocuments(dir) {
  const documents = []
  const stale = []
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    if (!entry.isFile() || !PROPOSAL_FILE.test(entry.name)) continue
    if (NON_PROPOSAL_DOCUMENTS[entry.name]) continue
    documents.push({ name: entry.name, date: entry.name.slice(0, 10), file: path.join(dir, entry.name) })
  }
  documents.sort((a, b) => (a.name < b.name ? -1 : a.name > b.name ? 1 : 0))
  return { documents, stale }
}

/**
 * The project-level check: every proposal document carries the declaration, or says in its own
 * pre-baseline note why it does not.
 */
export function ideasChecks() {
  return [
    guard('ideas.capabilities', () => {
      const dir = path.join(ROOT, 'docs', 'ideation')
      if (!existsSync(dir)) {
        return unverified('ideas.capabilities', 'no docs/ideation directory, so no proposal declaration could be read')
      }

      const names = readdirSync(dir, { withFileTypes: true })
        .filter((entry) => entry.isFile())
        .map((entry) => entry.name)
      const problems = nonProposalProblems(names)
      const { documents } = proposalDocuments(dir)
      if (documents.length === 0) {
        return unverified(
          'ideas.capabilities',
          'docs/ideation/ holds no proposal document, so no declaration could be read — an empty input is not a pass'
        )
      }

      const gated = []
      const notes = []
      for (const document of documents) {
        const text = readFileSync(document.file, 'utf8')
        const result = evaluateProposalDocument({ name: document.name, date: document.date, text })
        gated.push(...result.findings)
        notes.push(...result.notes)
      }

      const summary =
        `${documents.length} proposal document(s) read, ${gated.length} gated finding(s)` +
        (notes.length > 0
          ? `; note: ${notes.length} predate the ${PROPOSAL_CAPABILITY_BASELINE} baseline and carry the ` +
            `finding as a note rather than a FAIL — ${notes.join(' | ')}`
          : '')

      if (problems.length > 0) return fail('ideas.capabilities', `${problems.join('; ')} — ${summary}`)
      if (gated.length > 0) return fail('ideas.capabilities', gated.join('; '))
      return pass('ideas.capabilities', summary)
    }),
    guard('ideas.orphanPlans', () => {
      const problems = orphanPlanProblems(ROOT)
      if (problems.length > 0) {
        return fail(
          'ideas.orphanPlans',
          `${problems.length} leftover plan file(s) for scaffolded slot(s): ${problems.join('; ')}`
        )
      }
      const slugs = livingSlotSlugs(ROOT)
      return pass(
        'ideas.orphanPlans',
        `${slugs.size} living slot slug(s) checked; docs/ideation/plans/ holds no leftover for any of them`
      )
    })
  ]
}

/**
 * The sensor driven with proposals whose verdict is known.
 *
 * Every fixture below is a leg of the rule, and the `MUST FAIL` legs are the point: a check that
 * cannot fail is not a check. The negative controls keep it honest in the other direction — the
 * Photo Picker's zero-permission fact must not fire, and a `not used` capability must not be read as
 * a contradiction with a permissions section that also says "no camera".
 */
export function ideasFixtures() {
  return guard('ideas.fixtures', () => {
    const cases = []
    const CAPABILITIES = [
      '- camera: not used — the job is text entry; no step in it is a photograph.',
      '- photo picker / gallery: not used',
      '- media playback or capture: not used',
      '- microphone: not used',
      '- location: not used',
      '- motion sensors: not used',
      '- contacts / calendar: not used',
      '- notifications / reminder: not used',
      '- home-screen widget: not used',
      '- share / export: not used',
      '- Bluetooth / USB: not used',
      '- files / SAF: not used'
    ]
    const complete = (lines = CAPABILITIES) => `# A proposal\n\n## Device capabilities\n\n${lines.join('\n')}\n\n## Permissions\n\nNone.\n`

    cases.push([
      'a complete all-not-used declaration has no findings',
      () => expectEqual(deviceCapabilityFindings(complete()), [], 'findings')
    ])

    cases.push([
      'MUST FAIL — a proposal with no declaration is reported, so the field cannot be omitted by silence',
      () => {
        const findings = deviceCapabilityFindings('# A proposal\n\n## Build\n\nNo permissions.\n')
        expectEqual(findings.length, 1, 'findings')
        expectTrue(findings[0].includes('no device-capability declaration'), findings[0])
      }
    ])

    cases.push([
      'MUST FAIL — a declaration missing one capability names it',
      () => {
        const findings = deviceCapabilityFindings(complete(CAPABILITIES.filter((line) => !line.startsWith('- photo picker'))))
        expectTrue(findings.some((finding) => finding.includes('photo picker / gallery')), JSON.stringify(findings))
      }
    ])

    cases.push([
      'MUST FAIL — a used capability with no API named is reported',
      () => {
        const lines = [...CAPABILITIES]
        lines[0] = '- camera: used — the reading is a photograph and typing it loses the point.'
        const findings = deviceCapabilityFindings(complete(lines))
        expectTrue(findings.some((finding) => finding.includes('names no API')), JSON.stringify(findings))
      }
    ])

    cases.push([
      'MUST FAIL — a used capability with no justification is reported',
      () => {
        const lines = [...CAPABILITIES]
        lines[3] = '- microphone: used — `RECORD_AUDIO`'
        const findings = deviceCapabilityFindings(complete(lines))
        expectTrue(findings.some((finding) => finding.includes('what the job loses without it')), JSON.stringify(findings))
      }
    ])

    cases.push([
      'MUST FAIL — used camera while another section says "no camera" is a contradiction',
      () => {
        const lines = [...CAPABILITIES]
        lines[0] = '- camera: used — `ActivityResultContracts.TakePicture`, `android.permission.CAMERA`; without it the reading is retyped.'
        const text = `# A proposal\n\n## Device capabilities\n\n${lines.join('\n')}\n\n## Build\n\nNo camera, no location, no storage.\n`
        const findings = deviceCapabilityFindings(text)
        expectTrue(findings.some((finding) => finding.includes("says 'no camera'")), JSON.stringify(findings))
      }
    ])

    cases.push([
      'MUST FAIL — used camera with its manifest permission nowhere named is reported',
      () => {
        const lines = [...CAPABILITIES]
        lines[0] = '- camera: used — `ActivityResultContracts.TakePicture`; without it the reading is retyped.'
        const findings = deviceCapabilityFindings(complete(lines))
        expectTrue(findings.some((finding) => finding.includes('CAMERA')), JSON.stringify(findings))
      }
    ])

    cases.push([
      'NEGATIVE CONTROL — a not-used capability is not contradicted by a permissions section saying "no camera"',
      () => {
        const text = complete()
        const findings = deviceCapabilityFindings(`${text}\n## Build\n\nNo camera, no location, no storage.\n`)
        expectEqual(findings, [], 'findings')
      }
    ])

    cases.push([
      'NEGATIVE CONTROL — the Photo Picker needs no runtime permission, so a used picker with no permission named is not a finding',
      () => {
        const lines = [...CAPABILITIES]
        lines[1] = '- photo picker / gallery: used — `ActivityResultContracts.PickVisualMedia`; without it the user retypes a reading the phone already shows.'
        expectEqual(deviceCapabilityFindings(complete(lines)), [], 'findings')
      }
    ])

    cases.push([
      'RULE baseline — a proposal authored before the baseline carries the finding as a note, never a FAIL',
      () => {
        const result = evaluateProposalDocument({ name: '2026-10-02-x.md', date: '2026-10-02', text: '# x\n' })
        expectEqual(result.findings, [], 'gated findings')
        expectEqual(result.notes.length, 1, 'notes')
        expectTrue(result.notes[0].includes(PROPOSAL_CAPABILITY_BASELINE), result.notes[0])
      }
    ])

    cases.push([
      'RULE baseline — a proposal authored at the baseline turns the same finding into a FAIL',
      () => {
        const result = evaluateProposalDocument({ name: '2026-10-03-x.md', date: '2026-10-03', text: '# x\n' })
        expectTrue(result.findings.length === 1 && result.findings[0].startsWith('2026-10-03-x.md:'), JSON.stringify(result))
      }
    ])

    cases.push([
      'the allowlist refuses an entry that names a file which does not exist or carries no reason',
      () => {
        const problems = nonProposalProblems(['docs-not-a-real-name.md'])
        expectTrue(problems.some((problem) => problem.includes('does not exist')), JSON.stringify(problems))
        const real = Object.keys(NON_PROPOSAL_DOCUMENTS)
        expectEqual(nonProposalProblems(real, () => '').length, real.length, 'entries with no reason reported')
        expectEqual(nonProposalProblems(real).length, 0, 'a complete allowlist has no problems')
      }
    ])

    cases.push([
      'the real docs/ideation proposal set is read and, in its current state, gated nowhere',
      () => {
        const verdict = ideasChecks()[0]
        expectEqual(verdict.verdict, VERDICT.PASS, `verdict (${verdict.detail})`)
        expectTrue(verdict.detail.includes('proposal document(s) read'), verdict.detail)
        expectTrue(verdict.detail.includes(`${PROPOSAL_CAPABILITY_BASELINE} baseline`), verdict.detail)
      }
    ])

    cases.push([
      'MUST FAIL — a leftover docs/ideation/plans/<slug>.economics.json beside apps/<date>-<slug>/ is reported',
      () => {
        mkdirSync(SCRATCH_DIR, { recursive: true })
        const root = mkdtempSync(path.join(SCRATCH_DIR, 'orphan-plans-'))
        try {
          mkdirSync(path.join(root, 'apps', '2026-10-04-demo-slug'), { recursive: true })
          mkdirSync(path.join(root, 'docs', 'ideation', 'plans'), { recursive: true })
          writeFileSync(path.join(root, 'docs', 'ideation', 'plans', 'demo-slug.economics.json'), '{}\n')
          const problems = orphanPlanProblems(root)
          expectEqual(problems.length, 1, 'problems')
          expectTrue(problems[0].includes('docs/ideation/plans/demo-slug.economics.json'), problems[0])
          expectTrue(problems[0].includes('apps/*-demo-slug/'), problems[0])
        } finally {
          rmSync(root, { recursive: true, force: true })
        }
      }
    ])

    cases.push([
      'NEGATIVE CONTROL — a plan for a slug with no apps/<date>-<slug>/ directory is not an orphan',
      () => {
        mkdirSync(SCRATCH_DIR, { recursive: true })
        const root = mkdtempSync(path.join(SCRATCH_DIR, 'orphan-plans-clean-'))
        try {
          mkdirSync(path.join(root, 'apps', '2026-10-04-other'), { recursive: true })
          mkdirSync(path.join(root, 'docs', 'ideation', 'plans'), { recursive: true })
          writeFileSync(path.join(root, 'docs', 'ideation', 'plans', 'unchosen-recipe.economics.json'), '{}\n')
          expectEqual(orphanPlanProblems(root), [], 'problems')
        } finally {
          rmSync(root, { recursive: true, force: true })
        }
      }
    ])

    cases.push([
      'the real tree has no leftover plans for any living slot slug',
      () => {
        const verdict = ideasChecks().find((row) => row.id === 'ideas.orphanPlans')
        expectTrue(Boolean(verdict), 'ideas.orphanPlans present')
        expectEqual(verdict.verdict, VERDICT.PASS, `verdict (${verdict.detail})`)
      }
    ])

    return runFixtures('ideas.fixtures', cases)
  })
}
