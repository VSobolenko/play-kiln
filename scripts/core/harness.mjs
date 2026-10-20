// Two facts about the harness itself that `scripts/selftest.mjs` used to assert and that nothing has
// asserted since it was retired (`openspec/changes/reduce-factory-to-core/deletions.md` §7).
//
// Both are *copies*: a value that exists in two files, where the two are read by different programs
// and nothing compared them. `deletions.md` §7 named them as the change's most concrete loss and said
// each is cheap to restore inside the core, in the module that owns the rule. This is that restore.
//
// The rule is `.cursor/rules/verification-discipline.mdc`'s "One source of truth, imported" — and the
// reason it is a written rule is that the identical defect already shipped once: a hardcoded message
// naming `com.playkiln.*` while the regex required `com.gexetr.*`. A second copy of a list is not
// wrong until one half moves, and then nothing fails.
import { existsSync, readFileSync, readdirSync } from 'node:fs'
import path from 'node:path'
import { expectEqual, expectTrue, fail, guard, pass, playAssets, ROOT, unverified } from './lib.mjs'
import {
  EXCLUDED_DIRECTORY_PATH_REASONS,
  EXCLUDED_FILE_REASONS,
  EXCLUSION_CRITERION,
  EXCLUSION_REASONS
} from '../lib/release-approval.mjs'
import { PLACEHOLDER_HOSTS } from '../lib/in-app-policy.mjs'

// ---------------------------------------------------------------------------------------------
// The approval digest's exclusion list
// ---------------------------------------------------------------------------------------------

/**
 * Problems in the approval digest's exclusion vocabulary, as sentences.
 *
 * Three assertions lived only in the deleted `selftest.mjs`: every excluded name carries a reason,
 * every reason is a key of the closed set, and the criterion sentence names every key. The third is
 * the one that matters most — the sentence is printed to a human deciding whether an approval covers
 * a file, so a reason missing from it is an exclusion nobody was told about.
 *
 * @param {Map<string, string>} fileReasons every excluded file name or directory path -> its reason key
 * @param {Record<string, string>} reasons the closed set of reason keys -> the sentence each carries
 * @param {string} criterion the human-readable sentence built from the set
 */
export function exclusionVocabularyProblems(fileReasons, reasons, criterion) {
  const problems = []
  const declared = new Set(Object.keys(reasons))

  for (const [name, reason] of fileReasons) {
    if (!declared.has(reason)) {
      problems.push(`'${name}' cites the reason '${reason}', which ${'EXCLUSION_REASONS'} does not define`)
    }
  }

  const used = new Set(fileReasons.values())
  for (const reason of declared) {
    if (!used.has(reason)) {
      problems.push(`the reason '${reason}' is defined but no excluded name uses it, so it is a claim about nothing`)
    }
  }

  for (const reason of declared) {
    if (!criterion.includes(`\`${reason}\``)) {
      problems.push(`EXCLUSION_CRITERION does not name the reason '${reason}', so a reader of the criterion is not told about it`)
    }
  }

  // A blank reason is the shape a copy-paste leaves behind, and `''` is a valid Set key.
  for (const [name, reason] of fileReasons) {
    if (!String(reason).trim()) problems.push(`'${name}' carries an empty reason`)
  }

  return problems
}

export function checkApprovalExclusions() {
  return guard('harness.approvalExclusions', () => {
    const problems = exclusionVocabularyProblems(
      new Map([...EXCLUDED_FILE_REASONS, ...EXCLUDED_DIRECTORY_PATH_REASONS]),
      EXCLUSION_REASONS,
      EXCLUSION_CRITERION
    )
    const detail =
      `${EXCLUDED_FILE_REASONS.size} excluded file name(s) and ${EXCLUDED_DIRECTORY_PATH_REASONS.size} slot-relative directory path(s) ` +
      `across ${Object.keys(EXCLUSION_REASONS).length} reason(s), ` +
      'each reason used and named in the criterion'
    return problems.length > 0 ? fail('harness.approvalExclusions', problems.join('; ')) : pass('harness.approvalExclusions', detail)
  })
}

// ---------------------------------------------------------------------------------------------
// The reserved-host list
// ---------------------------------------------------------------------------------------------

/**
 * Hosts the in-app privacy-policy check treats as unfinished.
 *
 * The Python half moved to `scripts/store/model.py`, which reads `placeholderHosts` from
 * `scripts/lib/play-assets.json`. The Node half kept a literal array in `scripts/lib/in-app-policy.mjs`.
 * Two holders of one list: if the Python half gains a host and the Node half does not, a pack whose
 * policy URL points at the new reserved host is read as hosting a real policy by the check that
 * decides whether the app can open one — while `store/validate.py` reports it as unfinished. Neither
 * gate is wrong on its own; the disagreement is invisible.
 */
export function placeholderHostProblems(nodeHosts, jsonHosts) {
  const node = new Set(nodeHosts.map((host) => String(host).toLowerCase()))
  const json = new Set(jsonHosts.map((host) => String(host).toLowerCase()))
  const problems = []
  for (const host of json) {
    if (!node.has(host)) problems.push(`play-assets.json declares '${host}' but the in-app check does not reserve it`)
  }
  for (const host of node) {
    if (!json.has(host)) problems.push(`the in-app check reserves '${host}' but play-assets.json does not declare it`)
  }
  return problems
}

export function checkPlaceholderHosts() {
  return guard('harness.placeholderHosts', () => {
    const jsonHosts = playAssets().placeholderHosts
    if (!Array.isArray(jsonHosts) || jsonHosts.length === 0) {
      return unverified('harness.placeholderHosts', 'scripts/lib/play-assets.json declares no placeholderHosts, so there is nothing to compare')
    }
    const problems = placeholderHostProblems(PLACEHOLDER_HOSTS, jsonHosts)
    const detail = `${PLACEHOLDER_HOSTS.length} reserved host(s), identical in both halves`
    return problems.length > 0 ? fail('harness.placeholderHosts', problems.join('; ')) : pass('harness.placeholderHosts', detail)
  })
}

// ---------------------------------------------------------------------------------------------
// Fixtures
// ---------------------------------------------------------------------------------------------

export function harnessFixtures() {
  const reasons = { 'machine-local': 'a path only this machine has', 'build-output': 'a build wrote it' }
  const criterion = 'each is stated in `machine-local`, `build-output`'
  const fileReasons = new Map([['local.properties', 'machine-local'], ['build', 'build-output']])

  return {
    id: 'harness.fixtures',
    ...(() => {
      const cases = []
      const add = (label, ok) => cases.push({ label, ok })

      add('a consistent vocabulary has no problems', exclusionVocabularyProblems(fileReasons, reasons, criterion).length === 0)
      add(
        'a name citing an undefined reason fails',
        exclusionVocabularyProblems(new Map([...fileReasons, ['x', 'no-such-reason']]), reasons, criterion).length === 1
      )
      add(
        'a slot-relative directory path citing an undefined reason fails',
        exclusionVocabularyProblems(
          new Map([...fileReasons, ['server/data', 'no-such-reason']]),
          reasons,
          criterion
        ).length === 1
      )
      add(
        'a defined but unused reason fails twice, for being unused and for being unnamed in the criterion',
        exclusionVocabularyProblems(fileReasons, { ...reasons, 'never-used': 'x' }, criterion).length === 2
      )
      add(
        'a reason missing from the criterion sentence fails',
        exclusionVocabularyProblems(fileReasons, reasons, 'each is stated in `machine-local`').length === 1
      )
      add(
        'an empty reason fails',
        exclusionVocabularyProblems(new Map([['y', '   ']]), { '   ': 'x' }, criterion).length >= 1
      )

      add(
        'two identical host lists have no problems',
        placeholderHostProblems(['example.com', 'localhost'], ['localhost', 'example.com']).length === 0
      )
      add(
        'a host the Python half gained and the Node half lacks fails',
        placeholderHostProblems(['example.com'], ['example.com', 'example.internal']).length === 1
      )
      add(
        'a host the Node half reserves and the Python half lacks fails',
        placeholderHostProblems(['example.com', 'old.example'], ['example.com']).length === 1
      )

      // The publication assist ladder: the three points and the one-click download protocol.
      const synthPoints = [{ id: 'p', label: 'the synthetic point', pattern: /alpha/ }]
      const synthExpected = [{ path: 'a.md', what: 'the synthetic doc' }]
      const synthProtocol = [{ label: 'does the thing', pattern: /gamma/ }]
      const synthLadder = (docs) =>
        publishAssistProblems(docs, synthPoints, synthExpected, ['a.md'], synthProtocol)

      add(
        'a document naming the point has no problems',
        synthLadder([{ path: 'a.md', text: 'alpha gamma' }]).length === 0
      )
      add(
        'a document missing the point\u2019s token fails',
        synthLadder([{ path: 'a.md', text: 'beta gamma' }]).length === 1
      )
      add(
        'an expected document that is absent fails',
        synthLadder([]).length === 1
      )
      add(
        'a protocol document that omits the protocol token fails',
        synthLadder([{ path: 'a.md', text: 'alpha beta' }]).length === 1
      )
      add(
        'a protocol document outside the protocol list is not asked for the protocol',
        publishAssistProblems([{ path: 'a.md', text: 'alpha beta' }], synthPoints, synthExpected, [], synthProtocol).length === 0
      )

      const realDocs = PUBLISH_ASSIST_DOCS.filter(({ path: rel }) => existsSync(path.join(ROOT, rel)))
      const realText = () =>
        realDocs.map(({ path: rel }) => ({ path: rel, text: readFileSync(path.join(ROOT, rel), 'utf8') }))
      add('the real repository states the ladder somewhere', realDocs.length > 0)
      add('every declared holder of the ladder exists on disk', realDocs.length === PUBLISH_ASSIST_DOCS.length)
      add('the real documents carry no drift', publishAssistProblems(realText()).length === 0)

      // The two below are the same comparison read the other way: they exist so that a fixture set
      // which cannot fail is noticable as one. Both must be true for the inverted predicates above
      // to be meaningful.
      const json = playAssets().placeholderHosts
      add('the real repository host list is non-empty', Array.isArray(json) && json.length > 0)
      add('the real in-app host list is non-empty', PLACEHOLDER_HOSTS.length > 0)

      const failed = cases.filter((entry) => !entry.ok)
      return {
        verdict: failed.length ? 'FAIL' : 'PASS',
        detail: failed.length
          ? `${failed.length} of ${cases.length} fixture(s) failed: ${failed.map((entry) => entry.label).join('; ')}`
          : `${cases.length} fixtures`
      }
    })()
  }
}

export function harnessChecks() {
  return [
    checkApprovalExclusions(),
    checkPlaceholderHosts(),
    checkPublishAssists(),
    checkRuleFrontmatter(),
    checkGroundedClaims()
  ]
}

// ---------------------------------------------------------------------------------------------
// The publication `[assist]` ladder — the three points where the owner's hand is needed
// ---------------------------------------------------------------------------------------------
//
// The defect this was written from, recorded by the owner on 2026-10-07: the *list* of human-hand
// points existed in seven documents while the *procedure* for the third one existed in none, so the
// agent pressed `Скачать коды` repeatedly and the owner dismissed a stack of identical save windows.
// Two failures with one shape — a list that is a copy in seven places drifts, and a point named
// without its protocol is a point the next session improvises. Both halves are compared here, and
// the comparison is only possible because the canonical statement is `.cursor/rules/release-gate.mdc`
// § "Tooling assists" and every other holder cites it.

/**
 * The three points, as the token a document must carry to count as naming each one.
 *
 * One pattern per point, and each is the phrase the factory actually uses for the point. A stricter
 * pair was tried first and rejected on measurement: `release.md` and `forge.md` name the store
 * assist as "store graphics" without the word "input", so the second pattern was testing a house
 * style rather than the presence of the point — and a check that fails on correct prose is the
 * false positive this repository's own fixtures exist to prevent. What carries the real weight is
 * not this table but `PROMO_DOWNLOAD_PROTOCOL` below: naming the download was never the defect.
 */
export const PUBLISH_ASSIST_POINTS = [
  {
    id: 'aab-chooser',
    label: 'the AAB/APK file chooser',
    pattern: /AAB\s*\/\s*APK[^.\n]{0,20}chooser|AAB\s+chooser|bundle chooser|chooser for the (bundle|AAB)/i
  },
  {
    id: 'store-graphics',
    label: 'the store-graphic file inputs',
    pattern: /store[-\s]graphic/i
  },
  {
    id: 'promo-download',
    label: 'the promo-code download',
    pattern: /promo[-\s]codes?[^.\n]{0,40}download|download[^.\n]{0,40}promo[-\s]codes?/i
  }
]

/** Every document that states the ladder. A missing one is a drift, so it is a problem, not a skip. */
export const PUBLISH_ASSIST_DOCS = [
  { path: 'AGENTS.md', what: 'the L1 contract' },
  { path: '.cursor/rules/release-gate.mdc', what: 'the rule' },
  { path: '.cursor/rules/authorization.mdc', what: 'the authorization rule' },
  { path: '.cursor/commands/release.md', what: 'the release command' },
  { path: '.cursor/commands/forge.md', what: 'the forge command' },
  { path: 'docs/play-publishing-playbook.md', what: 'the runbook' },
  { path: 'docs/play-console-checklist.md', what: 'the console checklist' }
]

/**
 * The documents that must carry the download's full protocol rather than only its name.
 *
 * `release-gate.mdc` is the canonical statement and `play-publishing-playbook.md` and
 * `play-console-checklist.md` are the two a session actually walks the console with; the commands
 * and the L1 contract name the three points and cite the rule. Requiring the protocol in all seven
 * would be seven copies to maintain, which is the drift this module exists to stop.
 */
export const PUBLISH_PROTOCOL_DOCS = [
  '.cursor/rules/release-gate.mdc',
  'docs/play-publishing-playbook.md',
  'docs/play-console-checklist.md'
]

/** The one-click protocol, as the tokens each protocol document must carry. */
export const PROMO_DOWNLOAD_PROTOCOL = [
  { label: 'name the console control `Скачать коды`', pattern: /Скачать коды/ },
  { label: 'name the downloaded file `promotion_codes.csv`', pattern: /promotion_codes\.csv/ },
  {
    label: 'say the button is pressed exactly once',
    pattern: /exactly once|clicks? once|only once|click it \*\*once\*\*|один раз/i
  },
  { label: 'say to stop and ask the owner', pattern: /stop and ask|stops?, asks? and waits?|ask the owner/i },
  { label: 'say to wait for the owner', pattern: /wait/i },
  {
    label: 'forbid pressing the button a second time',
    pattern: /never press|not press again|never pressed|repeated press|never a retry|повторн/i
  }
]

/**
 * Problems with a set of documents, as sentences. `docs` is `[{ path, text }]`.
 *
 * A document that is expected and absent is a problem: deleting a holder would otherwise make the
 * check quieter, which is the failure `.cursor/rules/verification-discipline.mdc` names when it says
 * a rule only a document asserts is a rule nobody follows.
 */
export function publishAssistProblems(
  docs,
  points = PUBLISH_ASSIST_POINTS,
  expected = PUBLISH_ASSIST_DOCS,
  protocolDocs = PUBLISH_PROTOCOL_DOCS,
  protocol = PROMO_DOWNLOAD_PROTOCOL
) {
  const problems = []
  const byPath = new Map(docs.map((doc) => [doc.path, String(doc.text)]))

  for (const { path: rel, what } of expected) {
    if (!byPath.has(rel)) {
      problems.push(`${rel} (${what}) is missing, so the ladder it states cannot be compared`)
      continue
    }
    const text = byPath.get(rel)
    for (const point of points) {
      if (!point.pattern.test(text)) {
        problems.push(`${rel} (${what}) does not name ${point.label}: no match for ${point.pattern.source}`)
      }
    }
    if (protocolDocs.includes(rel)) {
      for (const token of protocol) {
        if (!token.pattern.test(text)) problems.push(`${rel} (${what}) does not ${token.label}`)
      }
    }
  }

  return problems
}

export function checkPublishAssists() {
  return guard('harness.publishAssists', () => {
    const docs = []
    for (const { path: rel } of PUBLISH_ASSIST_DOCS) {
      const abs = path.join(ROOT, rel)
      if (existsSync(abs)) docs.push({ path: rel, text: readFileSync(abs, 'utf8') })
    }
    const problems = publishAssistProblems(docs)
    const detail =
      `${PUBLISH_ASSIST_POINTS.length} assist point(s) in ${PUBLISH_ASSIST_DOCS.length} document(s); ` +
      `${PUBLISH_PROTOCOL_DOCS.length} carry the one-click download protocol`
    return problems.length > 0
      ? fail('harness.publishAssists', problems.join('; '))
      : pass('harness.publishAssists', detail)
  })
}

// ---------------------------------------------------------------------------------------------
// Rule files that Cursor never loads
// ---------------------------------------------------------------------------------------------

/**
 * Parses the YAML frontmatter block of a `.cursor/rules/*.mdc` file.
 *
 * Deliberately minimal — the three keys Cursor's rule loader reads to decide *whether* a rule is
 * attached to a context: `alwaysApply`, `globs` and `description`. A general YAML parser would be a
 * dependency for scalars, and nothing here needs nesting or lists.
 *
 * Returns `null` when the file has no frontmatter at all, which is the whole point of the check.
 * A leading BOM is dropped rather than treated as content, because an editor can add one silently
 * and it would otherwise make every rule look unparseable.
 */
export function parseRuleFrontmatter(text) {
  const lines = String(text).replace(/^\uFEFF/, '').split(/\r?\n/)
  if (lines.length === 0 || lines[0].trim() !== '---') return null
  const end = lines.findIndex((line, index) => index > 0 && line.trim() === '---')
  if (end === -1) return null
  const fields = {}
  for (const line of lines.slice(1, end)) {
    const match = /^([A-Za-z_][A-Za-z0-9_]*)\s*:\s*(.*)$/.exec(line)
    if (!match) continue
    fields[match[1]] = match[2].trim().replace(/^["']|["']$/g, '')
  }
  return fields
}

/**
 * Problems with a set of rule files, as sentences. `rules` is `[{ name, text }]`.
 *
 * The rule this enforces is not a style preference. A `.mdc` file with no frontmatter is attached to
 * nothing: Cursor's loader has no `description` to match on, no `globs` to scope by and no
 * `alwaysApply` to honour, so the file is never placed in any context. It is a rule that exists on
 * disk and is followed by nobody — which the factory keeps re-learning is the same thing as no rule
 * at all, except that its presence in the tree makes it *look* enforced.
 *
 * Measured before this check existed, on 2026-10-01: four rules were in that state, including
 * `parallel-execution.mdc` (the discipline the owner had to ask for repeatedly) and
 * `authorization.mdc` (which governs commits, signing keys and uploads). The ten rules that carried
 * `alwaysApply: true` were the ten that ever reached a session.
 *
 * The second clause is derived rather than hand-listed, because a list of "rules that must always
 * load" is a copy that drifts. A rule declaring `globs` is scoped to files; a rule declaring
 * `alwaysApply: true` is unscoped. A rule that declares *neither* is attached to no context at all
 * and is reachable only if an agent already knows to ask for it — which is precisely the state the
 * broken rules were in.
 */
export function ruleFrontmatterProblems(rules) {
  const problems = []
  for (const { name, text } of rules) {
    const fields = parseRuleFrontmatter(text)
    if (!fields) {
      problems.push(
        `${name} has no YAML frontmatter, so nothing attaches it to any context and it is never loaded`
      )
      continue
    }
    const description = (fields.description || '').trim()
    const globs = (fields.globs || '').trim()
    const alwaysApply = fields.alwaysApply === 'true'
    if (!description) {
      problems.push(`${name} declares no 'description', so neither the loader nor a reader is told what it governs`)
    }
    if (!globs && !alwaysApply) {
      problems.push(
        `${name} is neither 'alwaysApply: true' nor scoped by 'globs', so no context attaches it — ` +
          `it is reachable only by an agent that already knows to ask for it`
      )
    }
  }
  return problems
}

export function checkRuleFrontmatter() {
  return guard('harness.rules', () => {
    const dir = path.join(ROOT, '.cursor', 'rules')
    let names
    try {
      names = readdirSync(dir)
        .filter((name) => name.endsWith('.mdc'))
        .sort()
    } catch {
      return unverified('harness.rules', 'no .cursor/rules directory, so there is nothing to check')
    }
    if (names.length === 0) {
      return unverified('harness.rules', '.cursor/rules holds no .mdc file, which is not the same as every rule loading')
    }
    const rules = names.map((name) => ({ name, text: readFileSync(path.join(dir, name), 'utf8') }))
    const problems = ruleFrontmatterProblems(rules)
    const always = rules.filter(({ text }) => parseRuleFrontmatter(text)?.alwaysApply === 'true').length
    const detail = `${rules.length} rule(s), ${always} always-on, ${rules.length - always} scoped or attached by description`
    return problems.length > 0 ? fail('harness.rules', problems.join('; ')) : pass('harness.rules', detail)
  })
}

export function rulesFixtures() {
  const withFront = (body) => `---\n${body}\n---\n\n# A rule\n\nProse.\n`
  const cases = [
    ['a rule with alwaysApply: true and a description has no problems', () => {
      const problems = ruleFrontmatterProblems([
        { name: 'a.mdc', text: withFront('description: what it governs\nalwaysApply: true') }
      ])
      expectEqual(problems, [], 'problems')
    }],
    ['a rule scoped by globs has no problems', () => {
      const problems = ruleFrontmatterProblems([
        { name: 'a.mdc', text: withFront('description: what it governs\nglobs: apps/**/*.kt\nalwaysApply: false') }
      ])
      expectEqual(problems, [], 'problems')
    }],
    ['a rule with no frontmatter is reported as never loaded', () => {
      const problems = ruleFrontmatterProblems([{ name: 'a.mdc', text: '# No frontmatter\n\nProse.\n' }])
      expectEqual(problems.length, 1, 'problem count')
      expectTrue(problems[0].includes('never loaded'), 'the sentence names the consequence')
    }],
    ['a rule with frontmatter but no description is reported', () => {
      const problems = ruleFrontmatterProblems([{ name: 'a.mdc', text: withFront('alwaysApply: true') }])
      expectEqual(problems.length, 1, 'problem count')
      expectTrue(problems[0].includes("'description'"), 'the sentence names the missing key')
    }],
    ['a rule that is neither alwaysApply nor globbed is reported', () => {
      const problems = ruleFrontmatterProblems([
        { name: 'a.mdc', text: withFront('description: what it governs\nalwaysApply: false') }
      ])
      expectEqual(problems.length, 1, 'problem count')
      expectTrue(problems[0].includes('no context attaches it'), 'the sentence names the consequence')
    }],
    ['alwaysApply: false with globs is not a problem, so the check is not merely "alwaysApply must be true"', () => {
      const problems = ruleFrontmatterProblems([
        { name: 'a.mdc', text: withFront('description: scoped\nglobs: "**/*.md"\nalwaysApply: false') }
      ])
      expectEqual(problems, [], 'problems')
    }],
    ['a description carrying a colon and non-ASCII text survives parsing intact', () => {
      const fields = parseRuleFrontmatter(withFront('description: "the rule: why, per AGENTS.md"'))
      expectEqual(fields.description, 'the rule: why, per AGENTS.md', 'description')
    }],
    ['a body separator after the block does not extend the frontmatter', () => {
      const fields = parseRuleFrontmatter(withFront('description: x') + '\nBody.\n\n---\n\nMore.\n')
      expectEqual(fields.description, 'x', 'description')
    }],
    ['an unterminated frontmatter block is treated as no frontmatter', () => {
      expectEqual(parseRuleFrontmatter('---\ndescription: x\n\n# never closed\n'), null, 'parse result')
    }]
  ]

  const failures = []
  for (const [label, body] of cases) {
    try {
      body()
    } catch (error) {
      failures.push(`${label}: ${error.message}`)
    }
  }
  return {
    id: 'harness.rulesFixtures',
    verdict: failures.length ? 'FAIL' : 'PASS',
    detail: failures.length
      ? `${failures.length} of ${cases.length} fixture(s) failed: ${failures.join('; ')}`
      : `${cases.length} fixtures`
  }
}

// ---------------------------------------------------------------------------------------------
// The duty every dispensing card must declare: no claim without a source
// ---------------------------------------------------------------------------------------------

/**
 * A card that delegates rather than restates: a pointer at one of the `harness/roles/*.md` cards.
 * An agent adapter is allowed to carry no rule of its own, because the role card it names is where
 * the rule is stated — that thin-adapter shape is what `.cursor/agents/evaluator.md` already has.
 */
const ROLE_CARD_POINTER = /harness\/roles\//

/**
 * Problems with the declared duty, as sentences. `ruleText`/`ruleName` are the rule file under test;
 * `cards` is `[{ rel, text }]`, where a role card under `harness/roles/` is told apart from an agent
 * adapter by its `rel` and not by a flag, so a fixture needs no filesystem to place either.
 *
 * `.cursor/rules/grounded-claims.mdc` is a *behavioural* rule — no program can read one particular
 * sentence and say whether it was true — and its own "honest limit" section says the checkable half
 * is that the duty is *declared* everywhere it must be. Two declarations can silently fall out:
 * the rule stops being `alwaysApply: true`, so Cursor attaches it to no context (the failure
 * `harness.rules` exists for every rule), or a card that tells a worker how to report stops carrying
 * the duty, so the worker is never told (the failure `harness.refs` exists for a dangling pointer).
 * This is the sensor for those two, and only those two.
 *
 * It is a proxy, and it is honest about what it refuses to claim. It reads a token
 * (`grounded-claims`) and a delegation path (`harness/roles/`), never the meaning of the prose
 * around them: a card that names the token in a sentence *disowning* it passes, and a card that
 * states the whole duty in its own words without the token fails. That is deliberate, not a gap to
 * close — the token is what a later reader can follow, and rewording a card around it is a diff
 * someone reviews, which is the same defence `harness.rules` relies on. What it cannot do is prove
 * a worker obeyed the rule; it can only prove the duty reached the card that was meant to carry it.
 */
export function groundedClaimsProblems({ ruleText, ruleName, cards }) {
  const problems = []
  const fields = parseRuleFrontmatter(ruleText)
  if (!fields) {
    problems.push(
      `${ruleName} has no YAML frontmatter, so no context attaches it and the duty it states is followed by nobody`
    )
  } else if (fields.alwaysApply !== 'true') {
    problems.push(
      `${ruleName} is not 'alwaysApply: true', so the duty it states is attached to no context and is followed by nobody`
    )
  }
  for (const { rel, text } of cards) {
    const body = String(text)
    if (body.includes('grounded-claims')) continue
    // A role card is where the rule is *stated*; an agent adapter may instead delegate to one.
    if (!rel.startsWith('harness/roles/') && ROLE_CARD_POINTER.test(body)) continue
    problems.push(
      `${rel} neither names the grounded-claims rule nor delegates to a harness/roles/ card, ` +
        'so the duty is not declared to whoever reads it'
    )
  }
  return problems
}

/**
 * The verdict, from the rule text and the cards, with no filesystem.
 *
 * Separated from `checkGroundedClaims` precisely so the "nothing to check is not a pass" legs can
 * be exercised without a tree: an empty card set and an existing-but-empty card directory are both
 * `UNVERIFIED`, never `PASS`, because a directory with no cards is not evidence that every card is
 * clean. `emptyDirs` carries the directories that exist and hold no card, so their names reach the
 * sentence a reader sees.
 */
export function groundedClaimsVerdict({ ruleName, ruleText, cards, emptyDirs = [] }) {
  if (emptyDirs.length > 0) {
    return unverified(
      'harness.groundedClaims',
      `${emptyDirs.join(', ')} exists but holds no card, so the duty cannot be read for it`
    )
  }
  if (cards.length === 0) {
    return unverified('harness.groundedClaims', 'no role card or agent card exists, so nothing was read to bind the duty')
  }
  const problems = groundedClaimsProblems({ ruleText, ruleName, cards })
  const directories = new Set(cards.map((card) => card.rel.slice(0, card.rel.lastIndexOf('/'))))
  const detail = `${cards.length} card(s) across ${directories.size} directory(ies) bind grounded-claims`
  return problems.length > 0 ? fail('harness.groundedClaims', problems.join('; ')) : pass('harness.groundedClaims', detail)
}

/** The directories that hold a card the duty must reach. A missing directory is skipped, not failed. */
export const GROUNDED_CLAIMS_DIRS = Object.freeze([
  '.cursor/agents',
  '.codex/agents',
  '.claude/agents',
  'harness/roles'
])

export function checkGroundedClaims() {
  return guard('harness.groundedClaims', () => {
    const ruleName = '.cursor/rules/grounded-claims.mdc'
    const rulePath = path.join(ROOT, '.cursor', 'rules', 'grounded-claims.mdc')
    if (!existsSync(rulePath)) {
      return fail('harness.groundedClaims', `${ruleName} does not exist, so the duty has no rule file to load`)
    }
    const ruleText = readFileSync(rulePath, 'utf8')
    const cards = []
    const emptyDirs = []
    for (const rel of GROUNDED_CLAIMS_DIRS) {
      const dir = path.join(ROOT, rel)
      if (!existsSync(dir)) continue
      const names = readdirSync(dir)
        .filter((name) => name.endsWith('.md'))
        .sort()
      if (names.length === 0) {
        emptyDirs.push(rel)
        continue
      }
      for (const name of names) {
        cards.push({ rel: `${rel}/${name}`, text: readFileSync(path.join(dir, name), 'utf8') })
      }
    }
    return groundedClaimsVerdict({ ruleName, ruleText, cards, emptyDirs })
  })
}

export function groundedClaimsFixtures() {
  const withFront = (body) => `---\ndescription: no claim without a source\n${body}\n---\n\nThe rule.\n`
  const alwaysOn = withFront('alwaysApply: true')
  const agent = (name, text) => ({ rel: `.cursor/agents/${name}.md`, text })
  const role = (name, text) => ({ rel: `harness/roles/${name}.md`, text })

  const cases = [
    ['a rule that is alwaysApply: true with a card that names the token has no problems', () => {
      const problems = groundedClaimsProblems({
        ruleName: 'grounded-claims.mdc',
        ruleText: alwaysOn,
        cards: [agent('evaluator', 'Every finding carries a claim ledger per grounded-claims.')]
      })
      expectEqual(problems, [], 'problems')
    }],
    ['a rule that is not alwaysApply: true is reported, because an unattached rule is followed by nobody', () => {
      const problems = groundedClaimsProblems({
        ruleName: 'grounded-claims.mdc',
        ruleText: withFront('alwaysApply: false'),
        cards: [agent('evaluator', 'Follow the grounded-claims rule.')]
      })
      expectEqual(problems.length, 1, 'problem count')
      expectTrue(problems[0].includes('alwaysApply'), 'the sentence names the missing key')
    }],
    ['a rule file with no frontmatter at all is reported', () => {
      const problems = groundedClaimsProblems({
        ruleName: 'grounded-claims.mdc',
        ruleText: '# no frontmatter\n\nProse.\n',
        cards: []
      })
      expectEqual(problems.length, 1, 'problem count')
      expectTrue(problems[0].includes('no YAML frontmatter'), 'the sentence names the cause')
    }],
    ['a role card that does not name the token is a problem', () => {
      const problems = groundedClaimsProblems({
        ruleName: 'grounded-claims.mdc',
        ruleText: alwaysOn,
        cards: [role('builder', 'It writes the app and stops.')]
      })
      expectEqual(problems.length, 1, 'problem count')
      expectTrue(problems[0].includes('harness/roles/builder.md'), 'the sentence names the card')
    }],
    ['a card that names the token has no problem — the negative control that keeps a token sensor honest', () => {
      const problems = groundedClaimsProblems({
        ruleName: 'grounded-claims.mdc',
        ruleText: alwaysOn,
        cards: [agent('evaluator', 'The rule is `.cursor/rules/grounded-claims.mdc`.')]
      })
      expectEqual(problems, [], 'problems')
    }],
    ['a delegating card that names harness/roles/ but not the token has no problem', () => {
      const problems = groundedClaimsProblems({
        ruleName: 'grounded-claims.mdc',
        ruleText: alwaysOn,
        cards: [agent('evaluator', 'The canonical rules live in `harness/roles/verifier.md`.')]
      })
      expectEqual(problems, [], 'problems')
    }],
    ['a role card that delegates to another role card is still a problem — a role card is where the rule is stated', () => {
      const problems = groundedClaimsProblems({
        ruleName: 'grounded-claims.mdc',
        ruleText: alwaysOn,
        cards: [role('builder', 'See `harness/roles/verifier.md`.')]
      })
      expectEqual(problems.length, 1, 'problem count')
    }],
    ['an empty card set reports UNVERIFIED, never PASS', () => {
      const verdict = groundedClaimsVerdict({ ruleName: 'grounded-claims.mdc', ruleText: alwaysOn, cards: [] })
      expectEqual(verdict.verdict, 'UNVERIFIED', 'verdict')
    }],
    ['an existing but empty card directory reports UNVERIFIED rather than a pass on nothing', () => {
      const verdict = groundedClaimsVerdict({
        ruleName: 'grounded-claims.mdc',
        ruleText: alwaysOn,
        cards: [agent('evaluator', 'grounded-claims')],
        emptyDirs: ['.claude/agents']
      })
      expectEqual(verdict.verdict, 'UNVERIFIED', 'verdict')
    }]
  ]

  const failures = []
  for (const [label, body] of cases) {
    try {
      body()
    } catch (error) {
      failures.push(`${label}: ${error.message}`)
    }
  }
  return {
    id: 'harness.groundedClaimsFixtures',
    verdict: failures.length ? 'FAIL' : 'PASS',
    detail: failures.length
      ? `${failures.length} of ${cases.length} fixture(s) failed: ${failures.join('; ')}`
      : `${cases.length} fixtures`
  }
}

/** Exported so a fixture can read the file without a second path literal. */
export const PLAY_ASSETS_PATH = path.join(ROOT, 'scripts', 'lib', 'play-assets.json')

/** Read the raw JSON, for a fixture that wants the file rather than the parsed export. */
export function rawPlayAssetsHosts() {
  return JSON.parse(readFileSync(PLAY_ASSETS_PATH, 'utf8')).placeholderHosts
}
