#!/usr/bin/env node
/**
 * Offline regression suite for the pack half of the release approval.
 *
 * The defect it was written against: `scripts/lib/release-approval.mjs` excludes the whole `store/`
 * directory from the approval digest, because the release pipeline writes a pack *after* approval.
 * That exclusion is right for pipeline output and wrong for the files a human authors. A review
 * demonstrated it on 2026-09-29 — approve a slot, rewrite `store/listing.json` to describe a feature
 * the build does not have and `store/journey.json` to photograph different readings, then run
 * `scripts/store/render.py` and `media-capture.mjs`; every gate returned ok, and the artifact nobody
 * re-approved was the one carrying every public claim.
 *
 * Every case below is a claim, and the suite is built so it cannot pass by refusing everything or by
 * hashing nothing: the pack digest must move when an authored input is edited (1, 2, 7) and must not
 * move when the pipeline rewrites its own output (3, 4), the approved bytes must re-open the gate
 * (6), and the uploader must refuse an unapproved slot whose pack is otherwise complete and
 * uploadable (8) — the last case runs the real `play-upload.mjs` against a fixture so the refusal
 * cannot be a side effect of some other missing input.
 */
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { SCRATCH_DIR } from './lib/scratch.mjs'
import path from 'node:path'
import process from 'node:process'

import {
  APPROVAL_SCHEMA_VERSION,
  approvalPath,
  checkReleaseApproval,
  releaseSourceDigest,
  writeReleaseApproval
} from './lib/release-approval.mjs'
import { AUTHORED_PACK_INPUTS, checkPackApproval, packDigest } from './lib/pack-approval.mjs'

const REPO = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const NOTE = 'Owner approved the release after reviewing the UI.'

let checks = 0
let failures = 0

function check(label, condition, detail) {
  checks += 1
  if (condition) {
    console.log(`PASS ${label}`)
  } else {
    failures += 1
    console.log(`FAIL ${label}${detail ? ` — ${detail}` : ''}`)
  }
}

function pad(value) {
  return String(value).padStart(2, '0')
}

/**
 * A fixture image. `play-upload.mjs` reads the IHDR fields and never decodes pixels, so a fixture
 * needs the signature, the `IHDR` tag and the width/height/bitDepth/colorType fields — not a
 * decodable PNG. That keeps the suite offline and instant.
 */
function pngBytes(width, height, colorType) {
  const bytes = Buffer.alloc(64)
  Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]).copy(bytes, 0)
  bytes.write('IHDR', 12, 'ascii')
  bytes.writeUInt32BE(width, 16)
  bytes.writeUInt32BE(height, 20)
  bytes[24] = 8
  bytes[25] = colorType
  return bytes
}

/**
 * A slot whose pack is complete enough that `play-upload.mjs --only release --dry-run` prints a
 * plan for it, so the unapproved refusal in case 8 is attributable to the gate and not to a missing
 * file. Every path is under a fresh temp directory, which is also the `cwd` the uploader is run
 * from — `play-upload.mjs` resolves `apps/` from the process cwd, so the fixture never needs a slot
 * inside the real `apps/` while other writers are active.
 */
function createFixture() {
  const root = mkdtempSync(path.join(SCRATCH_DIR, 'kiln-pack-approval-'))
  const slug = 'fake-slot'
  const slot = path.join(root, 'apps', `2026-09-29-${slug}`)
  const store = path.join(slot, 'store')
  const assets = path.join(store, 'assets', 'en-US')
  mkdirSync(path.join(slot, 'app'), { recursive: true })
  mkdirSync(assets, { recursive: true })
  mkdirSync(path.join(slot, 'dist'), { recursive: true })

  writeFileSync(
    path.join(slot, 'app', 'build.gradle.kts'),
    'plugins { id("com.android.application") }\n\nandroid {\n  defaultConfig {\n' +
      '    versionCode = 7\n    versionName = "0.1.0"\n  }\n}\n',
    'utf8'
  )
  writeFileSync(path.join(slot, 'dist', `${slug}-0.1.0-7-release.aab`), 'aab-bytes\n', 'utf8')

  const listing = {
    schemaVersion: 1,
    appName: 'Fake Slot',
    slug,
    applicationId: 'com.gexetr.fakeslot',
    defaultLocale: 'en-US',
    contactEmail: 'v.sobolenko.vs@gmail.com',
    locales: {
      'en-US': {
        title: 'Fake Slot',
        shortDescription: 'A fixture slot for the pack-approval suite.',
        fullDescription: 'A fixture slot for the pack-approval suite.'
      }
    }
  }
  writeFileSync(path.join(store, 'listing.json'), `${JSON.stringify(listing, null, 2)}\n`, 'utf8')
  writeFileSync(path.join(store, 'journey.json'), `${JSON.stringify({ shots: [] }, null, 2)}\n`, 'utf8')
  writeFileSync(path.join(store, 'studio.json'), `${JSON.stringify({ mark: 'fixture' }, null, 2)}\n`, 'utf8')
  for (const document of ['listing.md', 'privacy.md', 'data-safety.md', 'review-notes.md', 'graphics.md', 'checklist.md']) {
    writeFileSync(path.join(store, document), `# ${document}\n`, 'utf8')
  }

  const icon = pngBytes(512, 512, 6)
  const feature = pngBytes(1024, 500, 2)
  writeFileSync(path.join(store, 'assets', 'icon-512.png'), icon)
  writeFileSync(path.join(store, 'assets', 'feature-graphic-1024x500.png'), feature)
  writeFileSync(path.join(assets, 'icon-512.png'), icon)
  writeFileSync(path.join(assets, 'feature-graphic-1024x500.png'), feature)
  const shots = [
    ...[...Array(6)].map((_, index) => [`phone-${pad(index + 1)}.png`, 1080, 1920]),
    ...[...Array(4)].map((_, index) => [`tablet-7-${pad(index + 1)}.png`, 1080, 1920]),
    ...[...Array(4)].map((_, index) => [`tablet-10-${pad(index + 1)}.png`, 1440, 2560])
  ]
  for (const [name, width, height] of shots) writeFileSync(path.join(assets, name), pngBytes(width, height, 2))

  return { root, slug, slot, store, listing: path.join(store, 'listing.json'), journey: path.join(store, 'journey.json') }
}

const fixture = createFixture()
const store = fixture.store

try {
  /* 1. The digest covers the authored inputs that exist and nothing else. */
  const first = packDigest(fixture.slot)
  check(
    'the pack digest covers exactly the authored store inputs',
    JSON.stringify(first.inputs) === JSON.stringify([...AUTHORED_PACK_INPUTS]),
    JSON.stringify(first.inputs)
  )
  check(
    'the pack digest never names pipeline-owned output',
    !first.inputs.some((input) => input.startsWith('store/assets/') || input === 'store/provenance.json'),
    JSON.stringify(first.inputs)
  )
  check('an unapproved slot has no pack to check', checkPackApproval(fixture.slot).ok === false)

  /* The control the rest of the suite rests on: a real recorded approval, in this fixture's root. */
  const record = writeReleaseApproval(fixture.slot, { note: NOTE, root: fixture.root })
  check(
    'a recorded approval binds both halves',
    typeof record.packDigest === 'string' && record.packDigest.length === 64 && record.packDigest !== record.sourceDigest,
    `packDigest=${record.packDigest} sourceDigest=${record.sourceDigest}`
  )
  check('the recorded approval opens the gate', checkReleaseApproval(fixture.slot, { root: fixture.root }).ok === true)

  /* 3. Re-reading unchanged inputs is idempotent — the digest must not be a nonce. */
  check('re-reading unchanged inputs returns the same pack digest', packDigest(fixture.slot).digest === first.digest)
  check('re-running the pack check does not void the approval it just verified', checkPackApproval(fixture.slot).ok === true)

  /* 4. The render path rewrites these files during a release run. None of them may move the digest,
   *    or the pipeline would void its own approval every time it ran — the trap the build half of
   *    this contract already documents. */
  writeFileSync(path.join(store, 'provenance.json'), '{"assets": {}}\n', 'utf8')
  writeFileSync(path.join(store, 'assets', 'en-US', 'phone-01.png'), pngBytes(1080, 1920, 2))
  writeFileSync(path.join(store, 'console-answers.md'), '# regenerated console-answers.md\n', 'utf8')
  for (const document of ['checklist.md', 'graphics.md', 'release-notes.md', 'listing.md', 'privacy.md', 'data-safety.md']) {
    writeFileSync(path.join(store, document), `# regenerated ${document}\n`, 'utf8')
  }
  check('rewriting pipeline-owned pack output does not move the digest', packDigest(fixture.slot).digest === first.digest)
  check(
    'rewriting pipeline-owned pack output does not void the approval',
    checkReleaseApproval(fixture.slot, { root: fixture.root }).ok === true
  )

  /* 2. THE DEFECT: an authored input edited after approval. The build digest must stay put, so the
   *    refusal can only be the new pack half. */
  const approvedListing = readFileSync(fixture.listing, 'utf8')
  writeFileSync(
    fixture.listing,
    approvedListing.replace('A fixture slot for the pack-approval suite.', 'A claim the build cannot honour.'),
    'utf8'
  )
  check(
    'an edit to listing.json leaves the build digest untouched',
    releaseSourceDigest(fixture.slot, { root: fixture.root }).digest === record.sourceDigest
  )
  const packRefusal = checkPackApproval(fixture.slot)
  check('an edit to listing.json refuses the pack check', packRefusal.ok === false)
  check(
    'the pack refusal names the pack, not the app',
    String(packRefusal.reason).includes('store pack') && !String(packRefusal.reason).includes('the app changed'),
    String(packRefusal.reason)
  )
  const gate = checkReleaseApproval(fixture.slot, { root: fixture.root })
  check('the full gate refuses when only the pack moved', gate.ok === false)
  check('and its message says which half moved', String(gate.reason).includes('store pack'), String(gate.reason))
  const digestAfterListingEdit = packDigest(fixture.slot).digest

  /* The opposing control: an authored input this module does cover must move the digest, so the
   * previous case cannot be passing because the digest ignores everything. */
  const approvedJourney = readFileSync(fixture.journey, 'utf8')
  writeFileSync(fixture.journey, `${JSON.stringify({ shots: [{ name: 'different reading' }] }, null, 2)}\n`, 'utf8')
  check('an edit to journey.json does move the pack digest', packDigest(fixture.slot).digest !== record.packDigest)
  writeFileSync(fixture.journey, approvedJourney, 'utf8')
  check(
    'restoring journey.json returns the pack digest to the edited-listing value',
    packDigest(fixture.slot).digest === digestAfterListingEdit
  )

  /* 6. Restoring the approved bytes must re-open the gate — content, not a timestamp. */
  writeFileSync(fixture.listing, approvedListing, 'utf8')
  check('restoring the approved listing re-opens the pack gate', checkPackApproval(fixture.slot).ok === true)
  check('and the whole approval', checkReleaseApproval(fixture.slot, { root: fixture.root }).ok === true)

  /* 5. A record without a pack digest was written before this contract. It must be reported as
   *    needing re-approval, never read as "no pack to check" — that silent downgrade is the one
   *    way this fix could make the gate weaker than no fix at all. */
  const sources = releaseSourceDigest(fixture.slot, { root: fixture.root })
  writeFileSync(
    approvalPath(fixture.slot),
    `${JSON.stringify(
      {
        schemaVersion: APPROVAL_SCHEMA_VERSION,
        slot: fixture.slug,
        approvedBy: 'owner',
        approvedAt: new Date().toISOString(),
        note: 'Owner approved before the authored pack was part of the contract.',
        sourceDigest: sources.digest,
        sourceFiles: sources.files
      },
      null,
      2
    )}\n`,
    'utf8'
  )
  const legacy = checkPackApproval(fixture.slot)
  check('a record with no pack digest is stale', legacy.ok === false && String(legacy.reason).includes('packDigest'), String(legacy.reason))
  check(
    'its build half still matches, so only the pack half is stale',
    releaseSourceDigest(fixture.slot, { root: fixture.root }).digest === sources.digest
  )
  const legacyGate = checkReleaseApproval(fixture.slot, { root: fixture.root })
  check('the full gate refuses a record with no pack digest', legacyGate.ok === false)
  check(
    'without claiming the app changed',
    !String(legacyGate.reason).includes('the app changed'),
    String(legacyGate.reason)
  )

  /* 8. THE UPLOADER. `play-upload.mjs` is the script that talks to Play and it had no approval gate
   *    at all. It is run here for real, from the fixture as cwd, with a credential file that exists
   *    but cannot be read as JSON: a pre-fix uploader reaches the plan and then the credentials, so
   *    the refusal before any plan — and before the credential file is opened — is the claim. The
   *    approved re-run is what makes the refusal attributable to the gate: the same fixture, with
   *    the same files, reaches a plan as soon as a record exists. */
  rmSync(approvalPath(fixture.slot), { force: true })
  const credentialPath = path.join(fixture.root, 'creds.json')
  writeFileSync(credentialPath, 'not a service account\n', 'utf8')
  const uploader = path.join(REPO, 'scripts', 'play-upload.mjs')
  const runUploader = (...extra) =>
    spawnSync(
      process.execPath,
      [uploader, '--slot', fixture.slug, '--only', 'release', ...extra],
      {
        cwd: fixture.root,
        encoding: 'utf8',
        windowsHide: true,
        env: { ...process.env, PLAY_SERVICE_ACCOUNT_JSON: credentialPath }
      }
    )
  const outputOf = (result) => `${result.stdout || ''}${result.stderr || ''}`

  const refused = runUploader('--dry-run')
  const refusedOutput = outputOf(refused)
  check('play-upload --dry-run refuses an unapproved slot', refused.status === 2, `status ${refused.status}: ${refusedOutput}`)
  check(
    'it refuses before printing a plan',
    !refusedOutput.includes('DRY-RUN') && !refusedOutput.includes('PASS preflight'),
    refusedOutput
  )
  check('it refuses before reading the credential path', !refusedOutput.includes('creds.json'), refusedOutput)
  check('it names the command that records approval', refusedOutput.includes('approve-release.mjs'), refusedOutput)

  const refusedUpload = runUploader()
  const refusedUploadOutput = outputOf(refusedUpload)
  check('a real upload of the same unapproved slot is refused too', refusedUpload.status === 2, `status ${refusedUpload.status}`)
  check(
    'and it never opens the credential file or builds a plan',
    !refusedUploadOutput.includes('creds.json') && !refusedUploadOutput.includes('service-account') &&
      !refusedUploadOutput.includes('PASS preflight'),
    refusedUploadOutput
  )

  writeReleaseApproval(fixture.slot, { note: NOTE })
  const approvedRun = runUploader('--dry-run')
  const approvedOutput = outputOf(approvedRun)
  check('the same fixture prints a plan once approved', approvedRun.status === 0, `status ${approvedRun.status}: ${approvedOutput}`)
  check('and the plan is what the refusal withheld', approvedOutput.includes('DRY-RUN'), approvedOutput)
} finally {
  rmSync(fixture.root, { recursive: true, force: true })
}

console.log(`\n${checks - failures}/${checks} checks passed`)
process.exit(failures === 0 ? 0 : 1)
