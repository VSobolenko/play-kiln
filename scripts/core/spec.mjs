// The specification every application is written inside, as a slot check.
//
// The owner's rule is that every app is authored inside an OpenSpec change — a proposal, a task
// breakdown and at least one capability spec — so the decisions a build is downstream of are
// readable before, during and after it. It was violated in a way nothing could see: an agent
// scaffolded `apps/2026-10-02-laneset/`, wrote code, and `openspec/changes/` contained only
// `app-lawn-round-log` and `archive`. `scripts/new-slot.mjs` requires `--economics`,
// `--differentiators` and `--naming` and does not require a specification, and no check under
// `scripts/core/` read a slot's change. The economics, promise and naming contracts each became a
// program because a rule only a document asserts is a rule nobody follows
// (`.cursor/rules/verification-discipline.mdc`, "One source of truth, imported"); this is that
// program for the specification.
//
// What a spec is here, measured from `openspec/changes/app-lawn-round-log/` rather than assumed:
// the convention is the change directory `openspec/changes/app-<slug>/`, holding `.openspec.yaml`
// (the schema/goal header), `proposal.md` (why this slot), `tasks.md` (the staged work), and at
// least one `specs/<capability>/spec.md` (the requirements and scenarios). A change missing any of
// those is a proposal that specifies nothing, so it is a FAIL naming the missing artifact.
//
// The core spawns no process, so the check does NOT shell out to `openspec validate`: the CLI exists
// and would be a second way to know the same facts, but a check that runs a subprocess is the chain
// `scripts/core/index.mjs` was written to replace, and the artifacts above are on disk to be read
// directly. The verdict is therefore about the shape of the change, never a re-run of the CLI.
//
// Two rules the port obeys. **Applicability is derived, not hand-listed**: `SPEC_BASELINE` in
// `scripts/core/baseline.mjs` is the day the rule was measured, so a slot created before it carries
// the finding as a note while a slot created at or after it is gated. And **a missing input is never
// a pass**: a slot with no `app/` and no `src/main` is not a product yet and reports `UNVERIFIED`
// rather than a green line about a specification for nothing.
import { existsSync, readdirSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { baselineVerdict } from './baseline.mjs'
import { expectEqual, expectTrue, guard, ROOT, runFixtures, unverified, VERDICT } from './lib.mjs'

/** Slot-relative, POSIX-shaped path, for a message a human reads. */
const rel = (file) => path.relative(ROOT, file).split(path.sep).join('/')

/** The slot's bare slug: the directory name without its `YYYY-MM-DD-` scaffold prefix. */
const slotSlug = (slotDir) => path.basename(slotDir).replace(/^\d{4}-\d{2}-\d{2}-/, '')

/** The change-directory convention: `openspec/changes/app-<slug>/`. */
export const CHANGE_PREFIX = 'app-'

/** `archive` holds changes already disposed of; it is not itself a change. */
const NOT_A_CHANGE = 'archive'

/**
 * Whether `text` contains `token` as a whole token.
 *
 * `\b` is not used, because every token here contains hyphens (`2026-10-02-laneset`, `laneset`) and
 * a hyphen is a non-word character: `\blaneset\b` would also match `laneset-two`. The rule that
 * matters is that the neighbours on both sides are not part of an identifier, so a proposal that
 * merely contains a longer word the token is embedded in is not accepted as naming the slot.
 */
export function wordBoundaryMatch(text, token) {
  const escaped = token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return new RegExp(`(?:^|[^A-Za-z0-9_])${escaped}(?:$|[^A-Za-z0-9_])`).test(text)
}

/**
 * The change that specifies a slot, or null.
 *
 * The convention directory is tried first, and only when it is absent does the search read
 * `openspec/changes/<name>/proposal.md` for a word-boundary token match on the slot's directory name
 * (`<date>-<slug>`) or its slug. The search exists because a change may legitimately be named for
 * the product rather than the directory; it is deliberately a search and not a guess, and the
 * `match` it returns is printed in the summary so a reader can see whether the convention or the
 * fallback was used.
 */
export function resolveSpecChange({ slug, slotName, changes, readProposal }) {
  const names = Array.isArray(changes) ? changes : []
  const conventional = `${CHANGE_PREFIX}${slug}`
  if (names.includes(conventional)) return { change: conventional, match: 'convention', token: null }

  const tokens = [slotName, slug].filter(Boolean)
  for (const name of names) {
    const proposal = readProposal(name)
    if (!proposal) continue
    const token = tokens.find((candidate) => wordBoundaryMatch(proposal, candidate))
    if (token) return { change: name, match: 'search', token }
  }
  return { change: null, match: null, token: null }
}

/**
 * The artifacts a change must carry to be a specification, over a list of files relative to the
 * change directory in POSIX form.
 *
 * A missing list rather than a thrown error, because the caller prints every absence at once: a
 * reader fixing one artifact at a time should not have to re-run to discover the next.
 */
export function specArtifacts(files) {
  const present = Array.isArray(files) ? files : []
  const missing = ['.openspec.yaml', 'proposal.md', 'tasks.md'].filter((name) => !present.includes(name))
  const specFiles = present.filter((file) => {
    const parts = file.split('/')
    return parts.length >= 2 && parts[0] === 'specs' && parts[parts.length - 1] === 'spec.md'
  })
  if (specFiles.length === 0) missing.push('specs/**/spec.md')
  return { missing, specFiles }
}

/**
 * The findings and summary for one slot, over an injected change set.
 *
 * Pure over `changes`, `readProposal` and `listChangeFiles` so the fixture suite can drive every
 * leg without a slot on disk; the verdict — FAIL here, NOTE for a pre-baseline slot — is applied by
 * `baselineVerdict` in `checkSpec`, so this function returns the finding and nothing about dates.
 */
export function specFindings({ slug, slotName, changes, readProposal, listChangeFiles }) {
  const resolution = resolveSpecChange({ slug, slotName, changes, readProposal })
  if (!resolution.change) {
    return {
      findings: [
        `no OpenSpec change specifies this slot: expected openspec/changes/${CHANGE_PREFIX}${slug}, ` +
          `and no openspec/changes/*/proposal.md names '${slotName}' or '${slug}' as a whole token`
      ],
      summary: `no change found (expected openspec/changes/${CHANGE_PREFIX}${slug})`
    }
  }

  const files = listChangeFiles(resolution.change)
  const { missing, specFiles } = specArtifacts(files)
  const via = resolution.match === 'search' ? `, resolved by proposal-text search naming '${resolution.token}'` : ''
  const summary =
    `change '${resolution.change}'${via}: ${specFiles.length} spec.md file(s), ${files.length} file(s) on disk`

  if (missing.length > 0) {
    return {
      findings: [
        `the change '${resolution.change}' is not a specification: ` +
          `openspec/changes/${resolution.change} is missing ${missing.join(', ')}`
      ],
      summary
    }
  }
  return { findings: [], summary }
}

/** Every file under `root`, as POSIX paths relative to it. A missing root is an empty list. */
function relativeFiles(root) {
  const out = []
  const walk = (dir, prefix) => {
    for (const entry of readdirSync(dir, { withFileTypes: true })) {
      const name = prefix ? `${prefix}/${entry.name}` : entry.name
      if (entry.isDirectory()) walk(path.join(dir, entry.name), name)
      else out.push(name)
    }
  }
  if (existsSync(root)) walk(root, '')
  return out
}

/** The change directories under `openspec/changes/`, the archive excluded, sorted for stable output. */
export function changeDirectories(changesRoot) {
  if (!existsSync(changesRoot)) return []
  return readdirSync(changesRoot, { withFileTypes: true })
    .filter((entry) => entry.isDirectory() && entry.name !== NOT_A_CHANGE)
    .map((entry) => entry.name)
    .sort()
}

/**
 * The specification facts about one slot.
 *
 * The product gate is checked before resolution because the two are different questions: a slot
 * with no `app/` and no `src/main` has no product for a change to specify, so it is `UNVERIFIED`
 * rather than a pass about a specification that describes nothing.
 */
export function checkSpec(slotDir) {
  return guard('slot.spec', () => {
    const slug = slotSlug(slotDir)
    const slotName = path.basename(slotDir)
    const appDir = path.join(slotDir, 'app')
    const srcMain = path.join(appDir, 'src', 'main')

    if (!existsSync(appDir) && !existsSync(srcMain)) {
      return unverified(
        'slot.spec',
        `no ${rel(appDir)} and no ${rel(srcMain)}, so this slot has no product yet for a change to ` +
          'specify — an empty scaffold is not a pass'
      )
    }

    const changesRoot = path.join(ROOT, 'openspec', 'changes')
    const changes = changeDirectories(changesRoot)
    const readProposal = (name) => {
      const file = path.join(changesRoot, name, 'proposal.md')
      try {
        return existsSync(file) ? readFileSync(file, 'utf8') : ''
      } catch {
        return ''
      }
    }
    const listChangeFiles = (name) => relativeFiles(path.join(changesRoot, name))

    const { findings, summary } = specFindings({ slug, slotName, changes, readProposal, listChangeFiles })
    return baselineVerdict('slot.spec', slotDir, findings, summary)
  })
}

/**
 * The sensor driven with changes whose verdict is known.
 *
 * One leg per rule the check depends on, so inverting the predicate that makes each one true turns
 * the suite red and names the leg: no change at all, a change missing `tasks.md`, a change missing
 * its `spec.md`, a change resolved by the proposal-text search, a word-boundary negative control,
 * the applicability boundary in both directions, and the real `app-lawn-round-log` change.
 */
export function specFixtures() {
  return guard('spec.fixtures', () => {
    const cases = []
    const SLUG = 'laneset'
    const SLOT = '2026-10-02-laneset'
    const complete = ['.openspec.yaml', 'proposal.md', 'tasks.md', 'specs/studio/spec.md']
    const run = (overrides) =>
      specFindings({
        slug: SLUG,
        slotName: SLOT,
        changes: [],
        readProposal: () => '',
        listChangeFiles: () => [],
        ...overrides
      })

    cases.push([
      'a slot with no change at all fails, naming the expected path openspec/changes/app-<slug>',
      () => {
        const { findings } = run({ changes: ['app-lawn-round-log'] })
        expectEqual(findings.length, 1, 'findings')
        expectTrue(findings[0].includes(`openspec/changes/app-${SLUG}`), findings[0])
      }
    ])

    cases.push([
      'a change with no tasks.md fails, naming tasks.md',
      () => {
        const { findings } = run({
          changes: [`app-${SLUG}`],
          listChangeFiles: () => ['.openspec.yaml', 'proposal.md', 'specs/studio/spec.md']
        })
        expectEqual(findings.length, 1, 'findings')
        expectTrue(findings[0].includes('tasks.md'), findings[0])
      }
    ])

    cases.push([
      'a change with no specs/**/spec.md fails, naming the missing spec',
      () => {
        const { findings } = run({
          changes: [`app-${SLUG}`],
          listChangeFiles: () => ['.openspec.yaml', 'proposal.md', 'tasks.md']
        })
        expectEqual(findings.length, 1, 'findings')
        expectTrue(findings[0].includes('specs/**/spec.md'), findings[0])
      }
    ])

    cases.push([
      'a change resolved by the proposal-text search passes, with the search named in the summary',
      () => {
        const proposal = '# Proposal\n\nThe slot is `apps/2026-10-02-laneset`, a swim set builder.'
        const result = run({
          changes: ['app-laneset-studio'],
          readProposal: (name) => (name === 'app-laneset-studio' ? proposal : ''),
          listChangeFiles: () => complete
        })
        expectEqual(result.findings, [], 'findings')
        expectTrue(result.summary.includes('proposal-text search'), result.summary)
        expectTrue(result.summary.includes('app-laneset-studio'), result.summary)
        expectTrue(result.summary.includes(SLOT), result.summary)
      }
    ])

    cases.push([
      'NEGATIVE CONTROL: a proposal that does not name the slot as a whole token is not accepted',
      () => {
        const { findings } = run({
          changes: ['app-other'],
          readProposal: () => '# Proposal\n\nThe slot is `apps/2026-10-02-lanesetter` and it is unrelated.'
        })
        expectEqual(findings.length, 1, 'findings')
        expectTrue(findings[0].includes('no OpenSpec change'), findings[0])
      }
    ])

    cases.push([
      'RULE baseline — a pre-baseline slot carries the no-change finding as a note, never a FAIL',
      () => {
        const dir = path.join(ROOT, 'apps', '2026-09-26-teacher-gradebook')
        const verdict = baselineVerdict('slot.spec', dir, ['no OpenSpec change specifies this slot'], 'summary')
        expectEqual(verdict.verdict, VERDICT.PASS, `verdict (${verdict.detail})`)
        expectTrue(verdict.detail.includes('note:'), `the finding was not reported as a note: ${verdict.detail}`)
        expectTrue(verdict.detail.includes('2026-10-02 baseline'), `the derivation was not named: ${verdict.detail}`)
      }
    ])

    cases.push([
      'RULE baseline — a slot created at the baseline turns the same finding into a FAIL',
      () => {
        const dir = path.join(ROOT, 'apps', '2026-10-02-laneset')
        const verdict = baselineVerdict('slot.spec', dir, ['no OpenSpec change specifies this slot'], 'summary')
        expectEqual(verdict.verdict, VERDICT.FAIL, `verdict (${verdict.detail})`)
        expectTrue(verdict.detail.includes('no OpenSpec change'), `the gated finding was not reported: ${verdict.detail}`)
      }
    ])

    cases.push([
      'the real app-lawn-round-log change resolves by the convention and passes',
      () => {
        const verdict = checkSpec(path.join(ROOT, 'apps', '2026-10-01-lawn-round-log'))
        expectEqual(verdict.verdict, VERDICT.PASS, `verdict (${verdict.detail})`)
        expectTrue(verdict.detail.includes("change 'app-lawn-round-log'"), verdict.detail)
        expectTrue(!verdict.detail.includes('note:'), `a complete change was reported as a note: ${verdict.detail}`)
      }
    ])

    cases.push([
      'a path with no app/ and no src/main is UNVERIFIED, never a pass for a spec that describes nothing',
      () => {
        const verdict = checkSpec(path.join(ROOT, 'registry'))
        expectEqual(verdict.verdict, VERDICT.UNVERIFIED, `verdict (${verdict.detail})`)
        expectTrue(verdict.detail.includes('no product'), verdict.detail)
      }
    ])

    return runFixtures('spec.fixtures', cases)
  })
}
