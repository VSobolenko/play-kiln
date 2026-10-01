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
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { expectEqual, expectTrue, fail, guard, pass, playAssets, ROOT, unverified } from './lib.mjs'
import {
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
 * @param {Map<string, string>} fileReasons every excluded name -> the reason key it cites
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
      EXCLUDED_FILE_REASONS,
      EXCLUSION_REASONS,
      EXCLUSION_CRITERION
    )
    const detail =
      `${EXCLUDED_FILE_REASONS.size} excluded name(s) across ${Object.keys(EXCLUSION_REASONS).length} reason(s), ` +
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
  return [checkApprovalExclusions(), checkPlaceholderHosts()]
}

/** Exported so a fixture can read the file without a second path literal. */
export const PLAY_ASSETS_PATH = path.join(ROOT, 'scripts', 'lib', 'play-assets.json')

/** Read the raw JSON, for a fixture that wants the file rather than the parsed export. */
export function rawPlayAssetsHosts() {
  return JSON.parse(readFileSync(PLAY_ASSETS_PATH, 'utf8')).placeholderHosts
}
