// Slot-level facts the core owns: the shipped client, the in-app policy, the registry's lifecycle,
// the post-ship reading, and the core's own size budget.
//
// Each of these delegates to the module that is already the single source of truth for its fact
// rather than re-implementing it, because `.cursor/rules/verification-discipline.mdc` records what
// a second implementation of one rule costs: a duplicate Java-home discovery built
// `<jdk>\keytool.exe` instead of `<jdk>\bin\keytool.exe`, and signing failed whenever JAVA_HOME was
// unset. The core imports; it does not restate.
import { existsSync, readFileSync, readdirSync } from 'node:fs'
import path from 'node:path'
import { evaluateInAppPolicy } from '../lib/in-app-policy.mjs'
import { scanClientImports, scanClientText, scanGoogleServices, scanSlot } from '../lib/client-secrets.mjs'
import { evaluateOutcomes, parseOutcomes } from '../lib/outcomes.mjs'
import { expectEqual, expectTrue, fail, guard, pass, ROOT, runFixtures, skip, unverified } from './lib.mjs'

/** The registry's lifecycle vocabulary. Adding a state is a product decision, not a convenience. */
export const LIFECYCLE = Object.freeze([
  'scaffolded', // an isolated project exists and nothing has shipped
  'building', // implementation in progress
  'published', // a release exists on Google Play
  'earning', // a dated post-ship reading exists
  'killed', // a recorded decision to stop, carrying a reason
  'retired' // removed from Play
])

/** States that imply the app's public identity is frozen. */
export const IDENTITY_FROZEN_STATES = Object.freeze(['published', 'earning', 'retired'])

// ---------------------------------------------------------------------------------------------
// The shipped client
// ---------------------------------------------------------------------------------------------

export function checkClientSecrets(slotDir) {
  return guard('slot.secrets', () => {
    const result = scanSlot(slotDir)
    const detail = result.rows
      .filter((row) => row.status !== 'PASS' || result.verdict !== 'PASS')
      .map((row) => row.message)
      .join('; ')
    if (result.verdict === 'PASS') {
      return pass('slot.secrets', `${result.scannedFiles} client file(s) scanned, no paid-provider credential`)
    }
    if (result.verdict === 'FAIL') {
      return fail('slot.secrets', detail || 'a paid-provider credential was found in the shipped client')
    }
    return unverified('slot.secrets', detail || 'the client could not be scanned')
  })
}

/**
 * The client-secret sensor's own controls, run against the sensor the check above delegates to.
 *
 * `scripts/lib/client-secrets.mjs` is the detector behind the "no paid API is called from the client"
 * rule, and it is only worth trusting if it can be made to fire on a real credential **and** to stay
 * silent on the things a naive detector flags. Both negative controls below are named by the rule,
 * and they matter for a reason that is not cosmetic: a detector that fires on a documented
 * placeholder is a detector the first annoyed author switches off, and a rule that has been switched
 * off is a rule that is no longer enforced. The positive controls are the ones a line-oriented
 * pattern match misses — a key assembled from two literals, a key in an XML resource, a keyed
 * `google-services.json`, and a plaintext call to a paid host.
 *
 * Measured exposure behind the rule (`docs/research/2026-09-29-app-factory-prior-art.md` §3 row 8):
 * of 206,867 scanned Android apps, 41.0 % of the LLM-enabled ones leaked API credentials, and
 * secrets were found in 35 % of 50,000 Play apps.
 */
export function checkSecretFixtures() {
  return guard('secrets.fixtures', () => {
    const KOTLIN = 'app/src/main/java/com/gexetr/demo/Main.kt'
    const STRINGS = 'app/src/main/res/values/strings.xml'
    const BUILD = 'app/build.gradle.kts'
    const fixtures = []
    const textFindings = (file, text) => scanClientText(file, text).findings
    const importFindings = (file, text, options) => scanClientImports(file, text, options).findings

    // ---- negative controls: these must NOT fire ----
    fixtures.push([
      'the rule\'s own negative control stays silent',
      () => expectEqual(scanClientText(KOTLIN, 'val apiKey = "YOUR_API_KEY"').findings, [], 'findings')
    ])
    fixtures.push([
      'a long documented placeholder is downgraded to a note, not reported as a credential',
      () => {
        // The first version of this fixture asserted a note for `"YOUR_API_KEY"`, and it was wrong in a
        // way worth keeping: that value is 12 characters, under the assignment rule's own 20-character
        // floor, so it never reaches the rule at all. A placeholder long enough to match is what
        // exercises the downgrade path — findings empty, note present — and that path is the one that
        // keeps the detector from being switched off by the first author it annoys.
        const result = scanClientText(KOTLIN, 'val apiKey = "YOUR_API_KEY_GOES_HERE_0123456789"')
        expectEqual(result.findings, [], 'findings')
        expectTrue(result.notes.length > 0, 'the placeholder is still visible as a note')
      }
    ])
    fixtures.push([
      'the word "secret" in prose is not a finding',
      () =>
        expectEqual(
          textFindings(KOTLIN, '// the secret is the scheduling algorithm, not the key'),
          [],
          'findings'
        )
    ])
    fixtures.push([
      'a comment naming a paid host is a note, not a call',
      () => {
        const result = scanClientImports(KOTLIN, '// see https://api.openai.com for the API shape')
        expectEqual(result.findings, [], 'findings')
        expectTrue(result.notes.length > 0, 'the comment is reported as a note')
      }
    ])

    // ---- positive controls a naive detector misses ----
    fixtures.push([
      'a key assembled from two literals is the same credential',
      () =>
        expectTrue(
          textFindings(KOTLIN, 'val apiKey = "AIza" + "01234567890123456789012345678901234"').length > 0,
          'the assembled key was not detected'
        )
    ])
    fixtures.push([
      'a key in an XML resource is found',
      () =>
        expectTrue(
          textFindings(STRINGS, '<string name="api_key">sk-ant-0123456789abcdefghijklmnop</string>').length > 0,
          'the XML key was not detected'
        )
    ])
    fixtures.push([
      'a paid-provider SDK on the client classpath is found',
      () =>
        expectTrue(
          importFindings(BUILD, 'dependencies { implementation("com.openai:openai:1.0") }', {
            dependencyFile: true
          }).length > 0,
          'the provider SDK was not detected'
        )
    ])
    fixtures.push([
      'a plaintext call to a paid host is found',
      () =>
        expectTrue(
          importFindings(KOTLIN, 'val url = "https://api.openai.com/v1/chat/completions"').length > 0,
          'the host call was not detected'
        )
    ])
    fixtures.push([
      'a keyed google-services.json is found, and a placeholder one is not',
      () => {
        const keyed = '{"client":[{"api_key":[{"current_key":"AIzaSyD0123456789abcdefghijklmnopqrstu"}]}]}'
        const placeholder = '{"client":[{"api_key":[{"current_key":"YOUR_API_KEY"}]}]}'
        expectTrue(
          scanGoogleServices('app/google-services.json', keyed).findings.length > 0,
          'the keyed file was not detected'
        )
        expectEqual(
          scanGoogleServices('app/google-services.json', placeholder).findings,
          [],
          'a placeholder key must not be a finding'
        )
      }
    ])

    const failures = []
    for (const [label, body] of fixtures) {
      try {
        body()
      } catch (error) {
        failures.push(`${label}: ${error.message}`)
      }
    }
    return failures.length === 0
      ? pass('secrets.fixtures', `${fixtures.length} fixtures`)
      : fail('secrets.fixtures', failures.join(' | '))
  })
}

export function checkInAppPolicy(slotDir) {
  return guard('slot.inAppPolicy', () => {
    const result = evaluateInAppPolicy(slotDir)
    const detail = result.rows
      .filter((row) => row.status !== 'PASS')
      .map((row) => row.message)
      .join('; ')
    if (result.verdict === 'PASS') {
      return pass('slot.inAppPolicy', `the declared policy is reachable in-app (${result.url})`)
    }
    if (result.verdict === 'FAIL') return fail('slot.inAppPolicy', detail || 'the policy is not reachable in-app')
    return unverified('slot.inAppPolicy', detail || 'the in-app policy could not be evaluated')
  })
}

// ---------------------------------------------------------------------------------------------
// The registry and its lifecycle
// ---------------------------------------------------------------------------------------------

export function readRegistry() {
  return JSON.parse(readFileSync(path.join(ROOT, 'registry', 'apps.json'), 'utf8').replace(/^\uFEFF/, ''))
}

/**
 * Validates the registry's shape and vocabulary, and reports each slot's lifecycle state.
 *
 * The failure this addresses is not a missing field: it is that no state meant "shipped" and none
 * meant "earned", so four slots read `scaffolded` while one of them was live on Google Play, and
 * the factory could not state a result. A state the vocabulary does not name is a defect, because
 * a typo would silently read as an unknown string to every consumer.
 */
export function checkRegistry(options = {}) {
  return guard('registry.lifecycle', () => {
    const registry = readRegistry()
    if (!Array.isArray(registry.apps)) return fail('registry.lifecycle', 'registry has no apps array')
    const problems = []
    const counts = {}
    for (const app of registry.apps) {
      const id = app.id || '<no id>'
      if (!LIFECYCLE.includes(app.status)) {
        problems.push(`${id}: status '${app.status}' is not one of ${LIFECYCLE.join(', ')}`)
        continue
      }
      counts[app.status] = (counts[app.status] || 0) + 1
      if (!app.dir || !existsSync(path.join(ROOT, app.dir))) {
        problems.push(`${id}: dir '${app.dir}' does not exist`)
      }
      if (app.status === 'killed' && !app.killReason) {
        problems.push(`${id}: killed without a recorded reason`)
      }
      if (IDENTITY_FROZEN_STATES.includes(app.status) && !app.publishedAt) {
        problems.push(`${id}: '${app.status}' implies a published identity but carries no publishedAt`)
      }
    }
    if (problems.length > 0) return fail('registry.lifecycle', problems.join('; '))
    const summary = Object.entries(counts)
      .map(([state, count]) => `${count} ${state}`)
      .join(', ')
    return pass('registry.lifecycle', `${registry.apps.length} slot(s): ${summary}`)
  })
}

// ---------------------------------------------------------------------------------------------
// The post-ship reading
// ---------------------------------------------------------------------------------------------

export function checkOutcomes(slotDir) {
  return guard('slot.outcomes', () => {
    const file = path.join(slotDir, 'store', 'outcomes.json')
    if (!existsSync(file)) {
      return skip('slot.outcomes', 'no recorded reading; NO-DATA is an honest state, not a failure')
    }
    const parsed = parseOutcomes(JSON.parse(readFileSync(file, 'utf8').replace(/^\uFEFF/, '')))
    if (parsed.error) return fail('slot.outcomes', parsed.error)
    const registry = readRegistry()
    const slug = path.basename(slotDir).replace(/^\d{4}-\d{2}-\d{2}-/, '')
    const created = registry.apps.find((app) => app.id === slug)
    const verdict = evaluateOutcomes({
      readings: parsed.readings,
      killCriteria: parsed.killCriteria,
      slug,
      createdAt: created ? created.createdAt : null
    })
    // The verdict vocabulary is deliberately not collapsed: TOO-EARLY and NO-DATA are honest
    // states, and MISSED names the kill command rather than being reported as a bare failure.
    if (verdict.verdict === 'MISSED') return fail('slot.outcomes', verdict.message)
    return pass('slot.outcomes', `${verdict.verdict}: ${verdict.message}`)
  })
}

// ---------------------------------------------------------------------------------------------
// The core's own size, so growth is visible instead of discovered
// ---------------------------------------------------------------------------------------------

const SKIP_DIRS = new Set(['node_modules', '.git', '__pycache__'])

export function walkFiles(dir, acc = []) {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    // A directory is descended into and a **regular file** is measured; every other entry kind is
    // skipped rather than collected. This clause is the whole of a 2026-10-07 hardening: the walk
    // used to push anything that was not a directory, so a symlink or a junction was handed to
    // `readFileSync` and the measurement died with `EISDIR: illegal operation on a directory, read`
    // — observed, not imagined, from a symlink-to-directory under the gitignored `.kiln/` scratch
    // tree. A walker that throws on an entry it cannot classify turns a measurement into a crash,
    // which is the opposite of what a size row is for.
    if (entry.isDirectory()) {
      if (SKIP_DIRS.has(entry.name)) continue
      walkFiles(full, acc)
    } else if (entry.isFile()) acc.push(full)
  }
  return acc
}

export function measureSources(dir) {
  const rows = walkFiles(dir).map((file) => ({
    file,
    name: path.basename(file),
    lines: readFileSync(file, 'utf8').split('\n').length
  }))
  const totals = { files: rows.length, lines: 0, tests: 0, meta: 0, other: 0 }
  for (const row of rows) {
    totals.lines += row.lines
    if (/^test-/.test(row.name) || /\.test\./.test(row.name)) totals.tests += row.lines
    else if (/^(selftest|verify.mjs)\.mjs$/.test(row.name)) totals.meta += row.lines
    else totals.other += row.lines
  }
  return { rows, totals }
}

/** Where the recorded baselines, the band and the revision history live. */
export const BUDGET_PATH = path.join('scripts', 'core', 'budget.json')

/**
 * The budget file: recorded baselines, the band, and the revision history.
 *
 * `budget` may be handed in for one reason only — so a fixture can drive `checkSize` against a
 * synthetic file, which is how the band is proved able to fail without editing the real one.
 */
export function budgets(budget) {
  if (budget) return budget
  return JSON.parse(readFileSync(path.join(ROOT, BUDGET_PATH), 'utf8'))
}

/**
 * The band a recorded baseline may move before `core.size` fails.
 *
 * A recorded number is a **baseline**, not an exact ceiling, and that distinction is the whole of
 * this check's 2026-10-07 redesign. The ceilings used to be set to the exact measurement of the
 * moment, so *every* edit under `scripts/` turned the row red and the remedy was a hand-written
 * justification in `budget.json`: nine consecutive raises are recorded there, and on 2026-10-07 a
 * correct change ended red because a **parallel session** had written 172 lines minutes earlier. A
 * number that fires on every edit — including edits someone else made — is not a measurement, it is
 * a toll, and the row it prints in every chat is noise the operator learns to read past.
 *
 * The band is 25%, and the number is measured rather than felt: the largest single change in the
 * file's own history moved the new surface by 1973 lines (8% of it) and the legacy half by 935 (3%),
 * so 25% clears every real change on record with room to spare while still catching what this
 * number exists for — bulk, unrecorded growth. `tolerancePercent` lives in `budget.json`, so this
 * module states no limit of its own.
 */
export function tolerancePercent(budget) {
  const percent = budget?.tolerancePercent
  return typeof percent === 'number' && Number.isFinite(percent) && percent > 0 ? percent : null
}

/** The limit a recorded baseline implies at a given band. */
export function sizeLimit(recorded, percent) {
  return Math.ceil(recorded * (1 + percent / 100))
}

/**
 * The size verdict, as problems. Pure, so the fixtures drive every edge of the band — including the
 * single line over it — and it names the command that records a measurement, because a refusal a
 * reader cannot act on is a refusal that gets worked around.
 */
export function sizeProblems({ surface, legacy, budget }) {
  const problems = []
  const percent = tolerancePercent(budget)
  if (percent === null) {
    problems.push(`\`tolerancePercent\` is missing from ${BUDGET_PATH}, so the recorded baselines have no band`)
    return problems
  }
  const rows = [
    { what: 'the core', measured: surface, recorded: budget?.coreBaseline, field: 'coreBaseline' },
    { what: 'the legacy harness', measured: legacy, recorded: budget?.legacyBaseline, field: 'legacyBaseline' }
  ]
  for (const row of rows) {
    if (typeof row.recorded !== 'number') {
      problems.push(`\`${row.field}\` is missing from ${BUDGET_PATH}`)
      continue
    }
    const limit = sizeLimit(row.recorded, percent)
    if (row.measured > limit) {
      problems.push(
        `${row.what} is ${row.measured} lines, past the ${percent}% band over its ` +
          `${row.recorded}-line recorded baseline (limit ${limit}) — record the measurement with ` +
          '`node scripts/core/budget.mjs --record`'
      )
    }
  }
  return problems
}

/**
 * How the legacy number has moved across the recorded revisions.
 *
 * The budget file's own point is deletion — "the core replaced the harness, not sat beside it" — and
 * the count is what keeps that claim honest: a green row that says the number moved up nine times
 * and down once tells a reader more than the number itself does. Entries that carry no move (an
 * entry may record only the core half) are counted in neither direction.
 */
export function legacyMoves(budget) {
  const revisions = Array.isArray(budget?._revisions) ? budget._revisions : []
  let up = 0
  let down = 0
  for (const revision of revisions) {
    const raw = String(revision?.legacyBaseline ?? revision?.legacyCeiling ?? '')
    const move = raw.match(/(\d+)\D+(\d+)/)
    if (!move) continue
    if (Number(move[2]) > Number(move[1])) up += 1
    else if (Number(move[2]) < Number(move[1])) down += 1
  }
  return { up, down, revisions: revisions.length }
}

/** The one line every run prints, so growth is visible inside a green row rather than only in a red one. */
export function sizeReport({ surface, legacy, total, budget }) {
  const percent = tolerancePercent(budget) ?? 0
  const part = (what, measured, recorded) => {
    const delta = measured - (typeof recorded === 'number' ? recorded : 0)
    const movement =
      delta === 0
        ? 'at the recorded baseline'
        : `${delta > 0 ? '+' : ''}${delta}${delta > 0 ? ` within the ${percent}% band` : ' under the recorded baseline'}`
    return `${what} ${measured}/${sizeLimit(typeof recorded === 'number' ? recorded : 0, percent)} (recorded ${recorded}, ${movement})`
  }
  const moves = legacyMoves(budget)
  return (
    `${part('core', surface, budget?.coreBaseline)}; ${part('legacy', legacy, budget?.legacyBaseline)}; ` +
    `scripts total ${total} (target ${budget?._targets?.['scripts total'] ?? 'unrecorded'}); ` +
    `${moves.revisions} recorded revision(s), the legacy number moved up ${moves.up}x and down ${moves.down}x`
  )
}

export function checkSize(budget) {
  return guard('core.size', () => {
    const scripts = path.join(ROOT, 'scripts')
    if (!existsSync(scripts)) return unverified('core.size', 'no scripts/ directory')
    const file = budgets(budget)
    const total = measureSources(scripts).totals.lines

    // The new surface: what the refactor ADDED, which is what its budget governs.
    const surface = ['core', 'store']
      .map((name) => {
        const dir = path.join(scripts, name)
        return existsSync(dir) ? measureSources(dir).totals.lines : 0
      })
      .reduce((a, b) => a + b, 0)
    const legacy = total - surface

    const problems = sizeProblems({ surface, legacy, budget: file })
    const detail = sizeReport({ surface, legacy, total, budget: file })
    return problems.length === 0 ? pass('core.size', detail) : fail('core.size', problems.join('; ') + ` — ${detail}`)
  })
}

/**
 * The band's own controls, against the real tree and a synthetic budget.
 *
 * A band is the one thing in this file that could quietly become "no check at all", so the fixtures
 * drive it from both sides: the exact limit is green and one line past it is red, the missing
 * `tolerancePercent` is a problem rather than an infinite band, and the real check is driven against
 * a one-line budget whose band is zero — which must turn `core.size` FAIL on the tree it is reading,
 * proving the sensor can fail **in place** rather than only in arithmetic.
 */
export function checkSizeFixtures() {
  return guard('size.fixtures', () =>
    runFixtures('size.fixtures', [
      [
        'a measurement at its recorded baseline is green',
        () =>
          expectEqual(
            sizeProblems({ surface: 1000, legacy: 2000, budget: { tolerancePercent: 25, coreBaseline: 1000, legacyBaseline: 2000 } }),
            [],
            'problems'
          )
      ],
      [
        'the last line inside the band is green, and the next one is not',
        () => {
          const budget = { tolerancePercent: 10, coreBaseline: 1000, legacyBaseline: 1000 }
          expectEqual(sizeProblems({ surface: 1100, legacy: 1100, budget }), [], 'problems at the limit')
          expectEqual(sizeProblems({ surface: 1101, legacy: 1101, budget }).length, 2, 'problems past the limit')
        }
      ],
      [
        'a missing band is a problem, not an infinite band',
        () =>
          expectTrue(
            sizeProblems({ surface: 10, legacy: 10, budget: { coreBaseline: 1, legacyBaseline: 1 } }).some((p) =>
              p.includes('tolerancePercent')
            ),
            'the missing band was not reported'
          )
      ],
      [
        'a missing recorded baseline is a problem rather than a free pass',
        () =>
          expectTrue(
            sizeProblems({ surface: 10, legacy: 10, budget: { tolerancePercent: 25, legacyBaseline: 1 } }).some((p) =>
              p.includes('coreBaseline')
            ),
            'the missing baseline was not reported'
          )
      ],
      [
        'a deletion is reported as a direction, never as a problem',
        () => {
          const budget = {
            tolerancePercent: 25,
            coreBaseline: 1000,
            legacyBaseline: 2000,
            _revisions: [{ legacyCeiling: '1800 -> 2000' }, { legacyCeiling: '2000 -> 1800' }, { coreLines: '1 -> 2' }]
          }
          expectEqual(sizeProblems({ surface: 500, legacy: 500, budget }), [], 'problems after a deletion')
          expectEqual(legacyMoves(budget), { up: 1, down: 1, revisions: 3 }, 'moves')
          expectTrue(sizeReport({ surface: 500, legacy: 500, total: 1000, budget }).includes('target'), 'the target line')
        }
      ],
      [
        'the real check fails in place when its band is zero',
        () => {
          const verdict = checkSize({ tolerancePercent: 1, coreBaseline: 1, legacyBaseline: 1 })
          expectEqual(verdict.verdict, 'FAIL', 'a one-line budget must turn the real row red')
          expectTrue(verdict.detail.includes('budget.mjs --record'), 'the remedy is named')
        }
      ]
    ])
  )
}

// ---------------------------------------------------------------------------------------------
// Cross-references between the role cards and the agent adapters
// ---------------------------------------------------------------------------------------------

/** Files that legitimately name no role card, each for a stated reason. */
export const ADAPTERS_WITHOUT_A_ROLE = Object.freeze({
  'android-debugger.md':
    'a tool adapter for Gradle/crash triage, not a pipeline role: it owns no judgement rule and ' +
    'delegates to no role card'
})

/** A reference of the form `harness/roles/<name>.md`. */
const ROLE_REFERENCE = /harness\/roles\/([a-z][a-z-]*)\.md/g

/**
 * A role card cited by a rule id, e.g. ``harness/roles/verifier.md` V5 ``.
 *
 * The role cards define no rule ids — they carry a subject-to-file table instead — so this form is
 * always stale. It is the defect this check exists for: a worker rewrote the five cards from rule
 * documents into role cards, and six adapter files were left citing `S9`, `M4`, `V5`, `V11`, `B16`
 * and `B17`, ids that no longer existed anywhere. Nothing failed, because nothing read the pointers:
 * an adapter is only ever read by the agent that opens it, and a dangling pointer reads exactly like
 * a valid one until someone follows it.
 */
const ROLE_CITED_BY_RULE_ID = /harness\/roles\/([a-z][a-z-]*)\.md`?\s+(?:and\s+)?([A-Z]{1,2}\d{1,2})\b/g

export function checkHarnessRefs() {
  return guard('harness.refs', () => {
    const groups = [
      { dir: '.cursor/agents', isAdapter: true },
      { dir: '.codex/agents', isAdapter: true },
      { dir: 'harness/roles', isAdapter: false }
    ]
    const files = []
    for (const group of groups) {
      const full = path.join(ROOT, group.dir)
      if (!existsSync(full)) continue
      for (const name of readdirSync(full)) {
        if (!name.endsWith('.md')) continue
        files.push({
          name,
          rel: `${group.dir}/${name}`,
          abs: path.join(full, name),
          isAdapter: group.isAdapter
        })
      }
    }
    if (files.length === 0) return unverified('harness.refs', 'no role card or adapter found to check')

    const problems = []
    let references = 0
    let adaptersPointing = 0

    // The escape hatch is a file, not a comment: an allowlist entry must name a file that exists
    // and must carry a reason, so an adapter cannot be exempted silently. This mirrors the
    // client-secrets exemption rule, and for the same reason.
    for (const [name, reason] of Object.entries(ADAPTERS_WITHOUT_A_ROLE)) {
      if (!files.some((file) => file.name === name)) {
        problems.push(`the allowlist names ${name}, which does not exist — remove the entry`)
      }
      if (!reason || !reason.trim()) {
        problems.push(`the allowlist entry for ${name} carries no reason`)
      }
    }

    for (const file of files) {
      const text = readFileSync(file.abs, 'utf8')
      let named = 0
      for (const match of text.matchAll(ROLE_REFERENCE)) {
        references += 1
        named += 1
        if (!existsSync(path.join(ROOT, 'harness', 'roles', `${match[1]}.md`))) {
          problems.push(`${file.rel}: names harness/roles/${match[1]}.md, which does not exist`)
        }
      }
      if (named > 0 && file.isAdapter) adaptersPointing += 1
      for (const match of text.matchAll(ROLE_CITED_BY_RULE_ID)) {
        problems.push(
          `${file.rel}: cites harness/roles/${match[1]}.md by the rule id ${match[2]}, but a role ` +
            `card defines no rule ids — name the card's section or the rule file instead`
        )
      }
      // An adapter's whole job is to be a pointer. One that points nowhere is a card that drifted
      // back into carrying its own rules, which is the duplication the role cards exist to remove.
      if (file.isAdapter && named === 0 && !ADAPTERS_WITHOUT_A_ROLE[file.name]) {
        problems.push(
          `${file.rel}: an adapter that names no role card, so it either carries its own rules or ` +
            `points at nothing`
        )
      }
    }

    if (problems.length > 0) return fail('harness.refs', problems.join('; '))
    if (references === 0) {
      return unverified('harness.refs', 'nothing references a role card, so no pointer was followed')
    }
    return pass(
      'harness.refs',
      `${references} role-card reference(s) resolve; ${adaptersPointing} adapter(s) point at a card ` +
        `and none cites a rule id`
    )
  })
}

/**
 * The repair this check guards, as a fixture: the exact dangling form the six adapters carried.
 * A pointer check that cannot fail is not a check, so the pattern is exercised here against the
 * text it was written for and against the repaired text.
 */
export function checkHarnessRefFixtures() {
  return guard('harness.refsFixtures', () => {
    const cases = []
    const stale = '- in `harness/roles/verifier.md` V5 and V11.'
    const repaired = '- under *Which rules bind this role* in `harness/roles/verifier.md`.'
    cases.push([
      'the stale form is detected',
      () => {
        if ([...stale.matchAll(ROLE_CITED_BY_RULE_ID)].length === 0) {
          throw new Error('the rule-id citation was not detected')
        }
      }
    ])
    cases.push([
      'the repaired form is not',
      () => {
        if ([...repaired.matchAll(ROLE_CITED_BY_RULE_ID)].length !== 0) {
          throw new Error('the repaired form was wrongly flagged')
        }
      }
    ])
    cases.push([
      'a role reference is still found in both forms',
      () => {
        for (const text of [stale, repaired]) {
          if ([...text.matchAll(ROLE_REFERENCE)].length !== 1) {
            throw new Error(`the role reference was not found in: ${text}`)
          }
        }
      }
    ])

    const failures = []
    for (const [label, body] of cases) {
      try {
        body()
      } catch (error) {
        failures.push(`${label}: ${error.message}`)
      }
    }
    return failures.length === 0
      ? pass('harness.refsFixtures', `${cases.length} fixtures`)
      : fail('harness.refsFixtures', failures.join(' | '))
  })
}

export function slotChecks(slotDir) {
  return [
    checkRegistry(),
    checkClientSecrets(slotDir),
    checkInAppPolicy(slotDir),
    checkOutcomes(slotDir)
  ]
}
