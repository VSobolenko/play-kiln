// The on-device run, as an artifact with a sensor: "I tested it on the phone" resolving to a file.
//
// Why this exists. `.cursor/rules/device-testing.mdc` states the factory's contract — the agent that
// built the app installs it on the attached phone, drives every screen itself, and only then hands
// the owner a product to judge — and its own "How it is verified" section records the gap this
// module closes: the rule is "enforced by **the run and its record**, not by a script", and nothing
// read the record. An agent then stopped at the checkpoint asking the owner to install and test the
// app, having tested nothing, and every gate still returned PASS, because no check could see that
// the run had not happened. This is the same shape as `slot.naming`, `slot.differentiators` and
// `slot.uiCoverage`: the evidence is a file, its absence is `UNVERIFIED`, and a defect is `FAIL`.
//
// Nothing here is invented. The record's fields are the evidence block that rule already asks a
// handoff to carry — the adb serial the run addressed, the artifact as installed and its sha256,
// the revision, and what was observed on screen — written as `apps/<slot>/device-verification.json`
// (schemaVersion 1). Field rules are the rule's own words: a run with no serial is not attributable
// to a device, so an empty serial is a defect and not a blank; an observation describes an action
// taken *and* what was observed.
//
// A missing record is `UNVERIFIED`, never `FAIL`. `.cursor/rules/device-testing.mdc` makes "no
// device attached" an honest gap that must be reported as such, and a check that failed on it would
// teach the operator to read past the row — the failure mode `harness/lessons.md` and
// `docs/reviews/2026-09-29-factory-review-decisions.md` D6 both record.
//
// The important half decides whether the app under review is still the app that was tested. The unit
// for that is the revision, not the artifact's bytes: a debug APK is not byte-reproducible across
// builds, so comparing hashes alone reported an identical-source rebuild as a stale install and forced
// a truthful record to be hand-edited after every rebuild. A record may therefore carry `buildDigest`
// — the digest `scripts/lib/release-approval.mjs` computes over the slot's build inputs, **imported
// here rather than re-implemented**, so a revision has one definition — and when it equals the current
// digest the revision is unchanged and the record PASSes, whatever the artifact's bytes hash to. When
// the digest differs, the app changed since the run and the record FAILs, naming the remedy. When the
// record carries no `buildDigest` the artifact cross-check is still made, and a disagreement remains
// the false pass `scripts/lib/apk-provenance.mjs` was written for: a stale install reported as
// evidence about the current app. The artifact absent is not a defect — a slot in development may
// record a run on a build that has since been cleaned — so that case PASSes with the cross-check named
// as not run, rather than failing a stage the slot has not reached.
//
// What this cannot claim, stated rather than implied: it reads JSON and one APK. It can say a run
// was recorded against a serial, that the record is well-formed, and that it names the build now on
// disk. It cannot say the observations are truthful, that the taps were read from the UI hierarchy
// rather than hand-guessed, or that the device was a phone rather than an emulator. The judgement
// that the app actually works stays with the run and with the owner at checkpoint 1.5; this
// sensor's job is to make the run's absence visible instead of silent.
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { sha256 } from '../lib/play-assets.mjs'
import { releaseSourceDigest } from '../lib/release-approval.mjs'
import { scratchDirFor } from '../lib/scratch.mjs'
import { expectEqual, expectTrue, fail, guard, pass, ROOT, runFixtures, unverified, VERDICT } from './lib.mjs'

/** Slot-relative, POSIX-shaped path, for a message a human reads. */
const rel = (file) => path.relative(ROOT, file).split(path.sep).join('/')

const readText = (file) => readFileSync(file, 'utf8')

/** The record this check reads, beside `naming.json` and `economics.json` in the slot root. */
const RECORD_NAME = 'device-verification.json'

/** The artifact the currently-built revision produces, per `.cursor/rules/release-gate.mdc` phase 1. */
const DEBUG_APK_PARTS = ['app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk']

/**
 * ISO-8601 as `testedAt` must read: a full date and time, with an offset, `Z`, or nothing after the
 * seconds. A bare `2026-10-01` is a date, not a timestamp, and `October 1, 2026` parses but is not
 * ISO-8601, so `Date.parse` alone is not enough to accept it.
 */
const ISO_8601 = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d+)?)?(Z|[+-]\d{2}:?\d{2})?$/

/** A value's shape and size, so a failure can show what was found instead of only what was wanted. */
const truncate = (text, max = 64) => (text.length <= max ? text : `${text.slice(0, max)}…`)

function describeValue(value) {
  if (value === undefined) return 'nothing (the field is absent)'
  if (value === null) return 'null'
  if (typeof value === 'string') return `the string "${truncate(value)}" (${value.length} character(s))`
  if (typeof value === 'number' || typeof value === 'boolean') return `the ${typeof value} ${String(value)}`
  if (Array.isArray(value)) return `an array of ${value.length}`
  return `a ${typeof value}`
}

/**
 * The 64-lowercase-hex rule for a digest-shaped field, shared by `apkSha256` and `buildDigest` so the
 * two cannot drift apart. `noun` names what the digest is over, and every message is built from the
 * offending value, matching the other fields' failures rather than restating the rule in prose.
 */
function hexDigestProblem(field, value, noun) {
  if (typeof value !== 'string') {
    return `${field} must be the 64-character lowercase hex sha256 of ${noun}; found ${describeValue(value)}`
  }
  if (value.length !== 64) {
    return `${field} must be 64 lowercase hex characters; "${truncate(value)}" is ${value.length} character(s)`
  }
  if (!/^[0-9a-f]{64}$/.test(value)) {
    return `${field} "${truncate(value)}" is 64 characters but is not lowercase hex; a sha256 uses only 0-9 and a-f, in lowercase`
  }
  return undefined
}

/**
 * Every rule the record must satisfy, each message built from the offending value and the rule.
 *
 * Split out as a plain function over a parsed value so the fixtures can drive every leg without a
 * slot on disk, and so the same wording is what the check and the suite both report.
 */
function deviceRecordProblems(value) {
  if (value === null || typeof value !== 'object' || Array.isArray(value)) {
    return [`the record is ${describeValue(value)}, not a JSON object`]
  }

  const problems = []

  if (value.schemaVersion !== 1) {
    problems.push(
      `schemaVersion must be exactly 1; found ${describeValue(value.schemaVersion)}`
    )
  }

  if (typeof value.serial !== 'string' || value.serial.trim() === '') {
    problems.push(
      'serial must be a non-empty string naming the adb serial the run addressed; found ' +
        `${describeValue(value.serial)} — a run with no serial is not attributable to a device`
    )
  }

  if (typeof value.testedAt !== 'string' || value.testedAt.trim() === '') {
    problems.push(
      `testedAt must be a non-empty ISO-8601 timestamp string; found ${describeValue(value.testedAt)}`
    )
  } else if (Number.isNaN(Date.parse(value.testedAt))) {
    problems.push(
      `testedAt "${truncate(value.testedAt)}" is not a parseable timestamp (Date.parse returned NaN)`
    )
  } else if (!ISO_8601.test(value.testedAt)) {
    problems.push(
      `testedAt "${truncate(value.testedAt)}" parses but is not ISO-8601; ` +
        'expected a full date and time, e.g. 2026-10-01T21:58:00Z'
    )
  }

  const apkSha256Problem = hexDigestProblem('apkSha256', value.apkSha256, 'the installed artifact')
  if (apkSha256Problem) problems.push(apkSha256Problem)

  // `buildDigest` is optional: a record written before this field existed, or one whose run predates a
  // source change, may honestly carry none, and the artifact cross-check below still applies. When it
  // is present it must be a sha256 exactly as `apkSha256` must, so a malformed digest is reported as
  // the schema defect it is rather than being compared and read as a stale revision.
  if (value.buildDigest !== undefined) {
    const buildDigestProblem = hexDigestProblem(
      'buildDigest',
      value.buildDigest,
      "the slot's build inputs"
    )
    if (buildDigestProblem) problems.push(buildDigestProblem)
  }

  if (typeof value.artifact !== 'string' || value.artifact.trim() === '') {
    problems.push(
      'artifact must be a non-empty string naming the artifact as installed; found ' +
        describeValue(value.artifact)
    )
  }

  if (!Array.isArray(value.observations)) {
    problems.push(
      'observations must be an array of at least 3 strings, each describing an action taken and ' +
        `what was observed on screen; found ${describeValue(value.observations)}`
    )
  } else if (value.observations.length < 3) {
    problems.push(
      `observations holds ${value.observations.length} observation(s); at least 3 are required, ` +
        'each describing an action taken and what was observed on screen'
    )
  } else {
    value.observations.forEach((entry, index) => {
      if (typeof entry !== 'string' || entry.trim() === '') {
        problems.push(
          `observations[${index}] must be a non-empty string describing an action taken and what ` +
            `was observed on screen; found ${describeValue(entry)}`
        )
      }
    })
  }

  return problems
}

/**
 * The device facts about one slot.
 *
 * Three rules the check obeys, each a recorded failure elsewhere:
 *
 *   * **A missing record is never a pass and never a failure.** No `device-verification.json`
 *     reports `UNVERIFIED` naming the path, because a slot with no device attached has an honest
 *     gap rather than a defect — and the message says in the same breath that deleting the file
 *     must not switch the check off.
 *   * **A record that does not satisfy its own schema is a defect, in full.** Every problem is
 *     reported, and each message is built from the offending value and the rule.
 *   * **A record about a different revision is a defect, and a rebuild of the same revision is not.**
 *     When the record carries a `buildDigest` it is compared to the slot's current build-input digest:
 *     a difference is a `FAIL` naming the changed app, and an equality is a `PASS` whose verdict the
 *     revision carries — even when the artifact's bytes differ, because a debug build is not
 *     byte-reproducible. With no `buildDigest`, the artifact sha256 cross-check stands, and a
 *     disagreement is the stale-install false pass `scripts/lib/apk-provenance.mjs` exists to prevent.
 */
export function deviceChecks(slotDir) {
  return [
    guard('slot.device', () => {
      const file = path.join(slotDir, RECORD_NAME)
      if (!existsSync(file)) {
        return unverified(
          'slot.device',
          `no ${rel(file)}; this slot carries no record of an on-device run, so the claim that the ` +
            'app was installed and driven on a phone is unaccounted for. A missing record is not a ' +
            'pass, and deleting this file must not switch the check off: the run recorded on the ' +
            'device is what makes the claim true (.cursor/rules/device-testing.mdc)'
        )
      }

      let value
      try {
        value = JSON.parse(readText(file).replace(/^\uFEFF/, ''))
      } catch (error) {
        return fail('slot.device', `${rel(file)} is not valid JSON: ${error.message}`)
      }

      const problems = deviceRecordProblems(value)
      if (problems.length > 0) {
        return fail('slot.device', `${rel(file)}: ${problems.join('; ')}`)
      }

      const summary =
        `${rel(file)}: on-device run on serial ${value.serial} at ${value.testedAt} with ` +
        `${value.observations.length} observation(s)`

      const apk = path.join(slotDir, ...DEBUG_APK_PARTS)
      const artifactBuilt = existsSync(apk)

      // The revision the record names, when it named one. This is deliberately not the artifact's
      // bytes: an identical-source rebuild produces a different debug APK, so the hash alone reported
      // a truthful record as a stale install. `releaseSourceDigest` is the same digest the release
      // approval is bound to, imported rather than copied, so the two cannot disagree about what a
      // revision is.
      if (value.buildDigest !== undefined) {
        const current = releaseSourceDigest(slotDir)
        if (current.digest !== value.buildDigest) {
          return fail(
            'slot.device',
            `${rel(file)} records buildDigest ${value.buildDigest.slice(0, 12)}… but the slot's ` +
              `build inputs now digest to ${current.digest.slice(0, 12)}…. The app changed since ` +
              'the run, so the record describes a revision that no longer exists. Drive the journey ' +
              'again on the attached phone against the current revision and record a new run ' +
              'against it.'
          )
        }
        const revisionFact =
          `buildDigest ${value.buildDigest.slice(0, 12)}… matches the slot's current build inputs, ` +
          'so the revision is the one that was tested'
        if (!artifactBuilt) {
          return pass(
            'slot.device',
            `${summary}; ${revisionFact}; no ${rel(apk)} is built, so the artifact's bytes were not ` +
              'cross-checked'
          )
        }
        const actual = sha256(apk)
        if (actual === value.apkSha256) {
          return pass(
            'slot.device',
            `${summary}; ${revisionFact}; apkSha256 also matches ${rel(apk)} as built`
          )
        }
        return pass(
          'slot.device',
          `${summary}; ${revisionFact}. This verdict is carried by the revision digest, not by the ` +
            `artifact-hash cross-check: the recorded apkSha256 ${value.apkSha256.slice(0, 12)}… ` +
            `differs from ${rel(apk)}, which hashes to ${actual.slice(0, 12)}…, because a debug ` +
            'build is not byte-reproducible. The bytes differ while the build inputs do not, so this ' +
            'is a rebuild of the revision that was tested rather than a stale install.'
        )
      }

      if (!artifactBuilt) {
        return pass(
          'slot.device',
          `${summary}; no ${rel(apk)} is built, so the recorded apkSha256 was not cross-checked ` +
            'against a build on disk — the check makes that comparison once the artifact exists. ' +
            "This record names no buildDigest, so an identical-source rebuild will still be read as " +
            "a stale install: record the revision's buildDigest to make a rebuild of the same " +
            'revision stop invalidating the record.'
        )
      }

      const actual = sha256(apk)
      if (actual !== value.apkSha256) {
        return fail(
          'slot.device',
          `${rel(file)} records apkSha256 ${value.apkSha256.slice(0, 12)}… but ${rel(apk)} hashes ` +
            `to ${actual.slice(0, 12)}…. Drive the journey again on the attached phone and record a ` +
            "new run against this build, or record the revision's buildDigest so a rebuild of " +
            'identical source stops invalidating the record. A record about a different build is a ' +
            'stale install reported as evidence about the current app, which is the defect ' +
            'scripts/lib/apk-provenance.mjs exists to prevent.'
        )
      }

      return pass('slot.device', `${summary}; apkSha256 matches ${rel(apk)} as built`)
    })
  ]
}

/**
 * The sensor driven with records whose verdict is known, including the two states that must not be
 * confused with each other.
 *
 * The suite is deliberately able to fail, and each fixture names the rule it exercises. The one that
 * must not be a failure is the last: a slot with no record is `UNVERIFIED`, and a suite that
 * returned `FAIL` there would encode the opposite of `.cursor/rules/device-testing.mdc`, where a
 * device that is not attached is an honest gap rather than a defect.
 *
 * The cross-check fixtures write a real artifact and compare against the hash computed from its
 * bytes, so the matching case proves the comparison is a comparison rather than a rubber stamp and
 * the mismatching case proves the real hash is what the failure prints.
 */
export function deviceFixtures() {
  return guard('device.fixtures', () => {
    // `.kiln/scratch/` is the only place a factory process may write, and a run-unique directory
    // under it keeps two concurrent runs from reading each other's fixtures
    // (`.cursor/rules/workspace-hygiene.mdc`). A passing run removes its sandbox; a failing one
    // keeps it, because the tree is what tells a reader what happened.
    const runRoot = mkdtempSync(path.join(scratchDirFor('device-fixtures'), 'run-'))
    const freshDir = (name) => {
      const dir = path.join(runRoot, name)
      mkdirSync(dir, { recursive: true })
      return dir
    }
    const recordPath = (dir) => path.join(dir, RECORD_NAME)
    const writeRecord = (dir, value) => writeFileSync(recordPath(dir), JSON.stringify(value, null, 2))
    const writeArtifact = (dir, bytes) => {
      const apk = path.join(dir, ...DEBUG_APK_PARTS)
      mkdirSync(path.dirname(apk), { recursive: true })
      writeFileSync(apk, bytes)
      return apk
    }
    const verdictOf = (dir) => deviceChecks(dir)[0]
    const isPristineRecord = (dir) => existsSync(recordPath(dir))

    const OBSERVATIONS = [
      'Tapped "New round" on the round list; the new-round sheet opened with today already filled in.',
      'Typed 12 customers into the round and the header total updated to 12, matching the list.',
      'Pressed system back from the round screen; the list returned with the round still listed, not an empty state.'
    ]

    /** A complete, correct record. Every failing fixture below is this object with one thing broken. */
    const validRecord = (overrides = {}) => ({
      schemaVersion: 1,
      serial: 'R5CT30ABCDE',
      testedAt: '2026-10-01T21:58:00Z',
      apkSha256: 'a'.repeat(64),
      artifact: 'app/build/outputs/apk/debug/app-debug.apk',
      observations: [...OBSERVATIONS],
      ...overrides
    })

    const cases = []

    cases.push([
      'RULE record present, artifact absent — a well-formed record PASSes and the cross-check is named as not run',
      () => {
        const dir = freshDir('valid-no-apk')
        writeRecord(dir, validRecord())
        const verdict = verdictOf(dir)
        expectEqual(verdict.verdict, VERDICT.PASS, `verdict (${verdict.detail})`)
        expectTrue(verdict.detail.includes('R5CT30ABCDE'), `the serial is not named: ${verdict.detail}`)
        expectTrue(verdict.detail.includes('3 observation'), `the count is not named: ${verdict.detail}`)
        expectTrue(
          verdict.detail.includes('not cross-checked'),
          `the missing artifact was not reported: ${verdict.detail}`
        )
      }
    ])

    cases.push([
      'RULE apkSha256 names the installed build — a record whose hash disagrees with the artifact FAILs',
      () => {
        const dir = freshDir('hash-mismatch')
        const apk = writeArtifact(dir, Buffer.from('kiln fixture artifact bytes\n'))
        const actual = sha256(apk)
        // The hash is computed from the file on disk, not stubbed: this assertion is what makes the
        // fixture about the comparison rather than about two strings that were never read.
        expectTrue(actual !== 'b'.repeat(64), 'the fixture artifact already hashes to the wrong value')
        writeRecord(dir, validRecord({ apkSha256: 'b'.repeat(64) }))
        const verdict = verdictOf(dir)
        expectEqual(verdict.verdict, VERDICT.FAIL, `verdict (${verdict.detail})`)
        expectTrue(
          verdict.detail.includes(actual.slice(0, 12)),
          `the real hash was not computed and printed: ${verdict.detail}`
        )
        expectTrue(verdict.detail.includes('different build'), `the defect was not named: ${verdict.detail}`)
        expectTrue(
          verdict.detail.includes('stale install'),
          `the stale-install consequence was not named: ${verdict.detail}`
        )
      }
    ])

    cases.push([
      'RULE apkSha256 names the installed build — a record whose hash matches the artifact PASSes',
      () => {
        const dir = freshDir('hash-match')
        const apk = writeArtifact(dir, Buffer.from('kiln fixture artifact bytes, the matching copy\n'))
        writeRecord(dir, validRecord({ apkSha256: sha256(apk) }))
        const verdict = verdictOf(dir)
        expectEqual(verdict.verdict, VERDICT.PASS, `verdict (${verdict.detail})`)
        expectTrue(verdict.detail.includes('matches'), `the comparison was not reported: ${verdict.detail}`)
        expectTrue(
          verdict.detail.includes('app-debug.apk'),
          `the artifact compared against is not named: ${verdict.detail}`
        )
      }
    ])

    cases.push([
      'RULE buildDigest names the tested revision — a rebuild that changed the APK bytes but not the build inputs PASSes, and the byte difference is reported',
      () => {
        const dir = freshDir('digest-match-apk-diff')
        const apk = writeArtifact(dir, Buffer.from('kiln fixture artifact bytes, rebuilt from the same source\n'))
        // The digest is the shared function's own reading of this fixture tree, taken with the
        // artifact in place and the record absent (the record is excluded from the digest, so it does
        // not matter) — so a PASS proves the comparison is a comparison rather than a constant.
        const buildDigest = releaseSourceDigest(dir).digest
        const actual = sha256(apk)
        expectTrue(actual !== 'e'.repeat(64), 'the fixture artifact already hashes to the recorded value')
        writeRecord(dir, validRecord({ apkSha256: 'e'.repeat(64), buildDigest }))
        const verdict = verdictOf(dir)
        expectEqual(verdict.verdict, VERDICT.PASS, `verdict (${verdict.detail})`)
        expectTrue(
          verdict.detail.includes(buildDigest.slice(0, 12)),
          `the revision digest was not named: ${verdict.detail}`
        )
        expectTrue(
          verdict.detail.includes('carried by the revision digest'),
          `the verdict was not attributed to the revision: ${verdict.detail}`
        )
        expectTrue(
          verdict.detail.includes(actual.slice(0, 12)),
          `the differing artifact hash was not reported: ${verdict.detail}`
        )
      }
    ])

    cases.push([
      "RULE buildDigest names the tested revision — a digest that no longer matches the slot's build inputs FAILs, naming the changed app",
      () => {
        const dir = freshDir('digest-different')
        writeArtifact(dir, Buffer.from('kiln fixture artifact bytes\n'))
        writeRecord(dir, validRecord({ buildDigest: 'c'.repeat(64) }))
        const verdict = verdictOf(dir)
        expectEqual(verdict.verdict, VERDICT.FAIL, `verdict (${verdict.detail})`)
        expectTrue(
          verdict.detail.includes('The app changed since the run'),
          `the changed build inputs were not named: ${verdict.detail}`
        )
        expectTrue(
          verdict.detail.includes('Drive the journey again'),
          `the re-run remedy was not named: ${verdict.detail}`
        )
      }
    ])

    cases.push([
      'RULE no buildDigest leaves the artifact cross-check in force — a hash that disagrees names BOTH remedies, not one',
      () => {
        const dir = freshDir('no-digest-hash-mismatch')
        const apk = writeArtifact(dir, Buffer.from('kiln fixture artifact bytes, no digest recorded\n'))
        const actual = sha256(apk)
        expectTrue(actual !== 'd'.repeat(64), 'the fixture artifact already hashes to the recorded value')
        writeRecord(dir, validRecord({ apkSha256: 'd'.repeat(64) }))
        const verdict = verdictOf(dir)
        expectEqual(verdict.verdict, VERDICT.FAIL, `verdict (${verdict.detail})`)
        expectTrue(
          verdict.detail.includes('Drive the journey again on the attached phone'),
          `the on-device re-run remedy was not named: ${verdict.detail}`
        )
        expectTrue(
          verdict.detail.includes("record the revision's buildDigest"),
          `the buildDigest remedy was not named: ${verdict.detail}`
        )
      }
    ])

    cases.push([
      'RULE observations is an array of at least 3 non-empty strings — an empty array FAILs',
      () => {
        const dir = freshDir('observations-empty')
        writeRecord(dir, validRecord({ observations: [] }))
        const verdict = verdictOf(dir)
        expectEqual(verdict.verdict, VERDICT.FAIL, `verdict (${verdict.detail})`)
        expectTrue(
          verdict.detail.includes('observations holds 0 observation(s)'),
          `the count was not reported: ${verdict.detail}`
        )
      }
    ])

    cases.push([
      'RULE observations is an array of at least 3 non-empty strings — 2 observations FAIL',
      () => {
        const dir = freshDir('observations-two')
        writeRecord(dir, validRecord({ observations: OBSERVATIONS.slice(0, 2) }))
        const verdict = verdictOf(dir)
        expectEqual(verdict.verdict, VERDICT.FAIL, `verdict (${verdict.detail})`)
        expectTrue(
          verdict.detail.includes('observations holds 2 observation(s)'),
          `the count was not reported: ${verdict.detail}`
        )
        expectTrue(
          verdict.detail.includes('at least 3 are required'),
          `the rule was not stated: ${verdict.detail}`
        )
      }
    ])

    cases.push([
      'RULE observations is an array of at least 3 non-empty strings — a blank entry FAILs, naming its index',
      () => {
        const dir = freshDir('observations-blank')
        writeRecord(dir, validRecord({ observations: [OBSERVATIONS[0], '   ', OBSERVATIONS[2]] }))
        const verdict = verdictOf(dir)
        expectEqual(verdict.verdict, VERDICT.FAIL, `verdict (${verdict.detail})`)
        expectTrue(
          verdict.detail.includes('observations[1]'),
          `the offending index was not named: ${verdict.detail}`
        )
      }
    ])

    cases.push([
      'RULE apkSha256 is 64 lowercase hex characters — a non-hex value FAILs, showing what was found',
      () => {
        const dir = freshDir('sha-not-hex')
        writeRecord(dir, validRecord({ apkSha256: 'z'.repeat(64) }))
        const verdict = verdictOf(dir)
        expectEqual(verdict.verdict, VERDICT.FAIL, `verdict (${verdict.detail})`)
        expectTrue(verdict.detail.includes('not lowercase hex'), `the hex rule was not stated: ${verdict.detail}`)
        expectTrue(verdict.detail.includes('64 characters'), `the length found was not shown: ${verdict.detail}`)
      }
    ])

    cases.push([
      'RULE apkSha256 is 64 lowercase hex characters — a short value FAILs, showing its length',
      () => {
        const dir = freshDir('sha-short')
        writeRecord(dir, validRecord({ apkSha256: 'abc123' }))
        const verdict = verdictOf(dir)
        expectEqual(verdict.verdict, VERDICT.FAIL, `verdict (${verdict.detail})`)
        expectTrue(
          verdict.detail.includes('is 6 character(s)'),
          `the length found was not shown: ${verdict.detail}`
        )
      }
    ])

    cases.push([
      'RULE buildDigest, when present, is 64 lowercase hex characters like apkSha256 — a non-hex value FAILs, showing what was found',
      () => {
        const dir = freshDir('build-digest-not-hex')
        const found = 'z'.repeat(64)
        writeRecord(dir, validRecord({ buildDigest: found }))
        const verdict = verdictOf(dir)
        expectEqual(verdict.verdict, VERDICT.FAIL, `verdict (${verdict.detail})`)
        expectTrue(verdict.detail.includes('buildDigest'), `the field was not named: ${verdict.detail}`)
        expectTrue(
          verdict.detail.includes('not lowercase hex'),
          `the hex rule was not stated: ${verdict.detail}`
        )
        expectTrue(
          verdict.detail.includes(found),
          `the value found was not shown: ${verdict.detail}`
        )
      }
    ])

    cases.push([
      'RULE serial is a non-empty string — a missing serial FAILs, because the run is not attributable',
      () => {
        const dir = freshDir('serial-missing')
        const record = validRecord()
        delete record.serial
        writeRecord(dir, record)
        const verdict = verdictOf(dir)
        expectEqual(verdict.verdict, VERDICT.FAIL, `verdict (${verdict.detail})`)
        expectTrue(
          verdict.detail.includes('not attributable to a device'),
          `the reason was not stated: ${verdict.detail}`
        )
      }
    ])

    cases.push([
      'RULE serial is a non-empty string — an empty serial FAILs and the value found is shown',
      () => {
        const dir = freshDir('serial-empty')
        writeRecord(dir, validRecord({ serial: '   ' }))
        const verdict = verdictOf(dir)
        expectEqual(verdict.verdict, VERDICT.FAIL, `verdict (${verdict.detail})`)
        expectTrue(
          verdict.detail.includes('the string "   " (3 character(s))'),
          `the value found was not shown: ${verdict.detail}`
        )
      }
    ])

    cases.push([
      'RULE schemaVersion is exactly 1 — a schemaVersion of 2 FAILs showing the value found',
      () => {
        const dir = freshDir('schema-2')
        writeRecord(dir, validRecord({ schemaVersion: 2 }))
        const verdict = verdictOf(dir)
        expectEqual(verdict.verdict, VERDICT.FAIL, `verdict (${verdict.detail})`)
        expectTrue(
          verdict.detail.includes('schemaVersion must be exactly 1; found the number 2'),
          `the value found was not shown: ${verdict.detail}`
        )
      }
    ])

    cases.push([
      'RULE testedAt is a parseable ISO-8601 timestamp — "yesterday" FAILs',
      () => {
        const dir = freshDir('tested-not-parseable')
        writeRecord(dir, validRecord({ testedAt: 'yesterday' }))
        const verdict = verdictOf(dir)
        expectEqual(verdict.verdict, VERDICT.FAIL, `verdict (${verdict.detail})`)
        expectTrue(
          verdict.detail.includes('Date.parse returned NaN'),
          `the parse failure was not named: ${verdict.detail}`
        )
      }
    ])

    cases.push([
      'RULE testedAt is a parseable ISO-8601 timestamp — a parseable non-ISO date FAILs',
      () => {
        const dir = freshDir('tested-not-iso')
        writeRecord(dir, validRecord({ testedAt: 'October 1, 2026 21:58' }))
        const verdict = verdictOf(dir)
        expectEqual(verdict.verdict, VERDICT.FAIL, `verdict (${verdict.detail})`)
        expectTrue(verdict.detail.includes('is not ISO-8601'), `the shape rule was not stated: ${verdict.detail}`)
      }
    ])

    cases.push([
      'RULE artifact is a non-empty string — an empty artifact FAILs',
      () => {
        const dir = freshDir('artifact-empty')
        writeRecord(dir, validRecord({ artifact: '' }))
        const verdict = verdictOf(dir)
        expectEqual(verdict.verdict, VERDICT.FAIL, `verdict (${verdict.detail})`)
        expectTrue(
          verdict.detail.includes('artifact must be a non-empty string'),
          `the artifact rule was not stated: ${verdict.detail}`
        )
      }
    ])

    cases.push([
      'RULE the record is valid JSON — a malformed file FAILs rather than reporting an absent record',
      () => {
        const dir = freshDir('malformed-json')
        writeFileSync(recordPath(dir), '{ "schemaVersion": 1, ')
        const verdict = verdictOf(dir)
        expectEqual(verdict.verdict, VERDICT.FAIL, `verdict (${verdict.detail})`)
        expectTrue(verdict.detail.includes('not valid JSON'), `the parse failure was not named: ${verdict.detail}`)
      }
    ])

    cases.push([
      'RULE a missing record is UNVERIFIED, never FAIL — the state a suite must not encode backwards',
      () => {
        const dir = freshDir('no-record')
        expectTrue(!isPristineRecord(dir), 'the fixture directory already holds a record')
        const verdict = verdictOf(dir)
        expectEqual(verdict.verdict, VERDICT.UNVERIFIED, `verdict (${verdict.detail})`)
        expectTrue(
          verdict.detail.includes(RECORD_NAME),
          `the missing file was not named: ${verdict.detail}`
        )
        expectTrue(
          verdict.detail.includes('A missing record is not a pass'),
          `the absence was not stated as a gap rather than a pass: ${verdict.detail}`
        )
      }
    ])

    const result = runFixtures('device.fixtures', cases)
    if (result.verdict === VERDICT.PASS) rmSync(runRoot, { recursive: true, force: true })
    return result
  })
}
