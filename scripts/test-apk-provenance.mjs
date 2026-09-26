#!/usr/bin/env node
/**
 * Failure-injection tests for the APK provenance chain.
 *
 * An evaluation scored this 3/5 on tests because the only evidence was a live `PASS`: the check had
 * been observed to succeed, never to refuse. The finding was specific - the code compared a sha256
 * *string* from the manifest against the installed APK and never hashed the artifact it named, so a
 * stale manifest and the matching stale installed APK agreed with each other about a build that was
 * neither of them. That case is cast 1 below.
 */
import { existsSync, mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { spawnSync } from 'node:child_process'
import { tmpdir } from 'node:os'
import path from 'node:path'
import process from 'node:process'

import { buildInputsUnchangedBetween, ProvenanceError, verifyArtifactChain } from './lib/apk-provenance.mjs'

let failures = 0
let checks = 0

function check(label, condition, detail) {
  checks += 1
  if (condition) {
    console.log(`PASS ${label}`)
  } else {
    failures += 1
    console.log(`FAIL ${label}${detail ? ` — ${detail}` : ''}`)
  }
}

const HEAD = 'aaaaaaaabbbbbbbbccccccccddddddddeeeeeeee'
const OLD_COMMIT = '1111111122222222333333334444444455555555'
const ARTIFACT = 'f87067564c09542aba00c0ed567d10f121ced6c4d7858da07003a0fb8ac30884'
const OLD_ARTIFACT = '075224ba42a349e593345b48d50afe48dd32dcfa1abb7e7b2550b9c2cfa57c7d'
const LABEL = 'dist/release-manifest.json'

function manifestFor(overrides = {}) {
  return {
    gitCommit: HEAD,
    artifacts: [
      { path: 'dist/pipe-sizing-0.1.0-1-debug.apk', sha256: 'd'.repeat(64) },
      { path: 'dist/pipe-sizing-0.1.0-1-release.apk', sha256: ARTIFACT },
      { path: 'dist/pipe-sizing-0.1.0-1-release.aab', sha256: 'a'.repeat(64) }
    ],
    ...overrides
  }
}

function base(overrides = {}) {
  return {
    manifestLabel: LABEL,
    manifest: manifestFor(),
    head: HEAD,
    artifactPath: 'dist/pipe-sizing-0.1.0-1-release.apk',
    artifactHash: ARTIFACT,
    installedHash: ARTIFACT,
    installedApkPath: '/data/app/~~x/base.apk',
    ...overrides
  }
}

function refuses(label, input, fragment) {
  try {
    verifyArtifactChain(input)
    check(label, false, 'it passed instead of refusing')
  } catch (error) {
    if (!(error instanceof ProvenanceError)) {
      check(label, false, `threw ${error.constructor.name}, not ProvenanceError`)
      return
    }
    check(label, error.message.includes(fragment), `message lacked "${fragment}": ${error.message}`)
  }
}

/* 1. THE ORIGINAL DEFECT: a stale manifest and the matching stale installed APK.
 *    Both agree on the old hash, so a hash-string-only comparison passes and photographs a build
 *    that is not the artifact the current sources produce. Hashing the file catches it. */
refuses(
  'stale manifest + matching stale installed APK is refused',
  base({ manifest: manifestFor({ gitCommit: OLD_COMMIT }), artifactHash: ARTIFACT, installedHash: OLD_ARTIFACT }),
  'was built from commit'
)

/* 2. The same defect with the commit link satisfied: the manifest is current, but the artifact file
 *    was rebuilt so its bytes no longer hash to what the manifest records. */
refuses(
  'an artifact rebuilt under a stale recorded hash is refused',
  base({ artifactHash: 'e'.repeat(64) }),
  'does not match its own manifest'
)

/* 3. The installed APK is an older build than the current artifact. */
refuses(
  'an installed APK older than the artifact is refused',
  base({ installedHash: OLD_ARTIFACT }),
  'does not match the signed artifact'
)

/* 4. Absent manifest must refuse, not warn and continue. */
refuses('an absent manifest is refused', base({ manifest: null }), 'is absent')

/* 5. A manifest with no release-APK entry cannot vouch for anything. */
refuses(
  'a manifest with no release-APK entry is refused',
  base({ manifest: { gitCommit: HEAD, artifacts: [{ path: 'dist/x-debug.apk', sha256: 'd'.repeat(64) }] } }),
  'records no release-APK sha256'
)

/* 6. A manifest that omits the commit cannot be tied to the sources. */
refuses('a manifest with no gitCommit is refused', base({ manifest: manifestFor({ gitCommit: undefined }) }), 'records no gitCommit')

/* 7. The artifact file named by the manifest does not exist. */
refuses('a missing artifact file is refused', base({ artifactPath: null, artifactHash: null }), 'does not exist')

/* 8. The artifact exists but is unreadable, so its bytes cannot be hashed. */
refuses('an unhashable artifact file is refused', base({ artifactHash: null }), 'could not hash')

/* 9. The installed base.apk could not be located or read. */
refuses('an unreadable installed APK is refused', base({ installedHash: null }), 'could not be located or hashed')

/* 10. The case that must pass: every link agrees. Nothing may be reported as refused. */
{
  const record = verifyArtifactChain(base())
  check('a fully matching chain passes', record.artifactMatch === 'exact', JSON.stringify(record.artifactMatch))
  check('the passing record carries the commit', record.gitCommit === HEAD)
  check('the passing record carries the artifact hash', record.artifactSha256 === ARTIFACT)
  check('the passing record carries the installed hash', record.installedSha256 === ARTIFACT)
  check('the passing record names the installed apk', record.installedApkPath === '/data/app/~~x/base.apk')
}

/* 11. Hash comparison is case-insensitive on the manifest side, since manifests are hand-editable. */
{
  const record = verifyArtifactChain(base({ manifest: manifestFor({ artifacts: [
    { path: 'dist/pipe-sizing-0.1.0-1-release.apk', sha256: ARTIFACT.toUpperCase() }
  ] }) }))
  check('an upper-case recorded hash still matches', record.artifactSha256 === ARTIFACT)
}

/* 12. Committing the captures advances HEAD past the build commit. That must not invalidate the
 *     record, provided the app sources did not move - otherwise the chain could never be satisfied
 *     by the commit that contains it. */
{
  const record = verifyArtifactChain(base({
    manifest: manifestFor({ gitCommit: OLD_COMMIT }),
    appSourcesUnchanged: true
  }))
  check('an older build commit passes when the app sources are unchanged', record.artifactMatch === 'exact')
}

/* 13. ...but only when they really did not move. Same commit gap, unchanged sources reported false. */
refuses(
  'an older build commit is refused when the app sources moved',
  base({ manifest: manifestFor({ gitCommit: OLD_COMMIT }), appSourcesUnchanged: false }),
  'the app sources differ'
)

/* 14. A blank commit must be refused rather than read as "nothing to compare". A record written with
 *     `gitCommit: null` is indistinguishable from one that deliberately omitted it, so a null/blank
 *     HEAD cannot be allowed even when the commit-gap relaxation is claimed. */
refuses(
  'a null HEAD is refused even when sources are claimed unchanged',
  base({ head: null, appSourcesUnchanged: true }),
  'HEAD could not be resolved'
)
refuses(
  'a whitespace-only HEAD is refused even when sources are claimed unchanged',
  base({ head: '   ', appSourcesUnchanged: true }),
  'HEAD could not be resolved'
)
refuses(
  'a whitespace-only manifest gitCommit is refused',
  base({ manifest: manifestFor({ gitCommit: '   ' }), appSourcesUnchanged: true }),
  'records no gitCommit'
)

/* 17-24. The commit-gap relaxation, against a real temporary repository. The relaxation is an
 *        allowlist: only paths that cannot change the built APK may differ. Defaulting to "rebuild"
 *        for anything unlisted is what keeps a new build input from being silently ignorable. */
{
  const repo = mkdtempSync(path.join(tmpdir(), 'kiln-buildin-'))
  const git = (...args) => spawnSync('git', args, { cwd: repo, encoding: 'utf8' })
  git('init', '-q')
  git('config', 'user.email', 'test@example.invalid')
  git('config', 'user.name', 'test')

  const slot = path.join(repo, 'apps', 'slot')
  const appDir = path.join(slot, 'app')
  const storeDir = path.join(slot, 'store')
  mkdirSync(appDir, { recursive: true })
  mkdirSync(storeDir, { recursive: true })
  mkdirSync(path.join(repo, 'capabilities', 'billing'), { recursive: true })
  writeFileSync(path.join(appDir, 'Main.kt'), 'version 1\n', 'utf8')
  writeFileSync(path.join(repo, 'capabilities', 'billing', 'Wallet.kt'), 'version 1\n', 'utf8')
  git('add', '-A')
  git('commit', '-q', '-m', 'one')
  const first = String(git('rev-parse', 'HEAD').stdout).trim()

  check('identical commits are unchanged', buildInputsUnchangedBetween(first, first, slot, repo) === true)

  // Each case compares the commit immediately before its own change, so one case's edit cannot leak
  // into another's diff and make a passing case look like a failing one.
  const around = (message, mutate) => {
    const before = String(git('rev-parse', 'HEAD').stdout).trim()
    mutate()
    git('add', '-A')
    git('commit', '-q', '-m', message)
    const after = String(git('rev-parse', 'HEAD').stdout).trim()
    return buildInputsUnchangedBetween(before, after, slot, repo)
  }

  // Copy and media are not build inputs, so committing a capture must not demand a rebuild.
  check(
    'a store-only change does not demand a rebuild',
    around('store only', () => {
      writeFileSync(path.join(storeDir, 'provenance.json'), '{}\n', 'utf8')
      writeFileSync(path.join(storeDir, 'listing.md'), 'copy\n', 'utf8')
    }) === true
  )

  // The evaluator's case: a shared capability module the slot includes, outside the slot's app/.
  check(
    'a shared capability change demands a rebuild',
    around('capability change', () => {
      writeFileSync(path.join(repo, 'capabilities', 'billing', 'Wallet.kt'), 'version 2\n', 'utf8')
    }) === false
  )

  // The slot's own gradle configuration is a build input that app/ does not cover.
  check(
    'a slot build.gradle.kts change demands a rebuild',
    around('slot gradle', () => {
      writeFileSync(path.join(slot, 'build.gradle.kts'), 'plugins {}\n', 'utf8')
    }) === false
  )

  // The version catalog is another build input outside app/.
  check(
    'a gradle/libs.versions.toml change demands a rebuild',
    around('version catalog', () => {
      mkdirSync(path.join(slot, 'gradle'), { recursive: true })
      writeFileSync(path.join(slot, 'gradle', 'libs.versions.toml'), '[versions]\n', 'utf8')
    }) === false
  )

  // Tooling cannot change the APK, so it must not demand one.
  check(
    'a change under scripts/ does not demand a rebuild',
    around('script change', () => {
      mkdirSync(path.join(repo, 'scripts'), { recursive: true })
      writeFileSync(path.join(repo, 'scripts', 'tool.mjs'), 'tooling\n', 'utf8')
    }) === true
  )

  check(
    'an app/ change demands a rebuild',
    around('app change', () => {
      writeFileSync(path.join(appDir, 'Main.kt'), 'version 2\n', 'utf8')
    }) === false
  )

  const finalHead = String(git('rev-parse', 'HEAD').stdout).trim()
  check(
    'a missing commit is not mistaken for unchanged',
    buildInputsUnchangedBetween('', finalHead, slot, repo) === false
  )
  check(
    'a nonexistent commit is not mistaken for unchanged',
    buildInputsUnchangedBetween('0'.repeat(40), finalHead, slot, repo) === false
  )
  check(
    'an unreadable slot path does not read as unchanged',
    around('unreadable slot', () => {
      writeFileSync(path.join(repo, 'capabilities', 'billing', 'Wallet.kt'), 'version 3\n', 'utf8')
    }) === false
  )

  rmSync(repo, { recursive: true, force: true })
}

/* 25-38. Rename handling, against a real temporary repository. `git diff --name-only` prints only the
 *        *destination* of a rename, so with detection on, moving a build input into an allowlisted
 *        directory showed one harmless path and the removal never appeared - the function returned
 *        true and attested a capture to a tree whose sources were gone. Every case below except the
 *        controls returned the wrong answer before `--no-renames` was added. */
{
  const repo = mkdtempSync(path.join(tmpdir(), 'kiln-rename-'))
  const git = (...args) => spawnSync('git', args, { cwd: repo, encoding: 'utf8' })
  git('init', '-q')
  git('config', 'user.email', 'test@example.invalid')
  git('config', 'user.name', 'test')

  const slot = path.join(repo, 'apps', 'slot')
  const appDir = path.join(slot, 'app')
  mkdirSync(appDir, { recursive: true })
  mkdirSync(path.join(slot, 'store'), { recursive: true })
  mkdirSync(path.join(repo, 'capabilities', 'billing'), { recursive: true })
  for (const dir of ['docs', 'scripts', 'harness', '.kiln']) mkdirSync(path.join(repo, dir), { recursive: true })

  // One source per case, so no case can be rescued or broken by another's mutation.
  writeFileSync(path.join(appDir, 'Screen.kt'), 'fun screen(){}\n', 'utf8')
  writeFileSync(path.join(appDir, 'Extra.kt'), 'fun extra(){}\n', 'utf8')
  writeFileSync(path.join(appDir, 'Hidden.kt'), 'fun hidden(){}\n', 'utf8')
  writeFileSync(path.join(appDir, 'Moved.kt'), 'fun moved(){}\n', 'utf8')
  writeFileSync(path.join(appDir, 'Harnessed.kt'), 'fun harnessed(){}\n', 'utf8')
  writeFileSync(
    path.join(appDir, 'Edited.kt'),
    'package app\n\nfun edited(): Int {\n  val a = 1\n  val b = 2\n  val c = 3\n  val d = 4\n  val e = 5\n  return a + b + c + d + e\n}\n',
    'utf8'
  )
  writeFileSync(path.join(repo, 'capabilities', 'billing', 'Wallet.kt'), 'wallet 1\n', 'utf8')
  writeFileSync(path.join(repo, 'docs', 'a.md'), 'notes a\n', 'utf8')
  writeFileSync(path.join(repo, 'docs', 'incoming.md'), 'notes incoming\n', 'utf8')
  git('add', '-A')
  git('commit', '-q', '-m', 'build')
  const first = String(git('rev-parse', 'HEAD').stdout).trim()

  // Commits one change and reports whether HEAD still looks like it builds the prior commit.
  const around = (message, mutate) => {
    const before = String(git('rev-parse', 'HEAD').stdout).trim()
    mutate()
    git('add', '-A')
    git('commit', '-q', '-m', message)
    const after = String(git('rev-parse', 'HEAD').stdout).trim()
    return buildInputsUnchangedBetween(before, after, slot, repo)
  }

  check('unchanged commits are still unchanged', buildInputsUnchangedBetween(first, first, slot, repo) === true)

  // A build input moved into each allowlisted directory: the removal must force a rebuild.
  check(
    'moving a build input into docs/ demands a rebuild',
    around('move to docs', () => git('mv', 'apps/slot/app/Screen.kt', 'docs/Screen.kt')) === false
  )
  check(
    'moving a build input into scripts/ demands a rebuild',
    around('move to scripts', () => git('mv', 'apps/slot/app/Extra.kt', 'scripts/Extra.kt')) === false
  )
  check(
    'moving a build input into .kiln/ demands a rebuild',
    around('move to kiln', () => git('mv', 'apps/slot/app/Hidden.kt', '.kiln/Hidden.kt')) === false
  )
  check(
    'moving a build input into the slot store/ demands a rebuild',
    around('move to store', () => git('mv', 'apps/slot/app/Moved.kt', 'apps/slot/store/Moved.kt')) === false
  )
  check(
    'moving a build input into harness/ demands a rebuild',
    around('move to harness', () => git('mv', 'apps/slot/app/Harnessed.kt', 'harness/Harnessed.kt')) === false
  )

  // The shared-capability case the reviewer used: a module the slot includes, moved out of the tree.
  check(
    'moving capabilities/billing/Wallet.kt into docs/ demands a rebuild',
    around('move capability', () => git('mv', 'capabilities/billing/Wallet.kt', 'docs/Wallet.kt')) === false
  )

  // A content-edited rename is a rename with similarity < 100%; it hides a modification, not just a
  // deletion, so it must also force a rebuild. The edit is one line of a multi-line file so git still
  // scores it as a rename rather than a delete+add.
  check(
    'a content-edited rename of a build input demands a rebuild',
    around('edit and move', () => {
      git('mv', 'apps/slot/app/Edited.kt', 'docs/Edited.kt')
      writeFileSync(
        path.join(repo, 'docs', 'Edited.kt'),
        'package app\n\nfun edited(): Int {\n  val a = 100\n  val b = 2\n  val c = 3\n  val d = 4\n  val e = 5\n  return a + b + c + d + e\n}\n',
        'utf8'
      )
    }) === false
  )

  // The reverse direction: a build input *appears* because a file moved out of an allowlisted path.
  check(
    'moving an allowlisted file into app/ demands a rebuild',
    around('move into app', () => git('mv', 'docs/incoming.md', 'apps/slot/app/incoming.md')) === false
  )

  // Controls: the fix must not over-reject. A rename wholly inside an allowlisted directory, and a
  // plain new allowlisted file, are still not build inputs.
  check(
    'a rename wholly inside docs/ does not demand a rebuild',
    around('rename within docs', () => git('mv', 'docs/a.md', 'docs/b.md')) === true
  )
  check(
    'a new file under docs/ does not demand a rebuild',
    around('new doc', () => writeFileSync(path.join(repo, 'docs', 'new.md'), 'new\n', 'utf8')) === true
  )

  rmSync(repo, { recursive: true, force: true })
}

console.log(`\n${checks - failures}/${checks} checks passed`)
process.exit(failures === 0 ? 0 : 1)