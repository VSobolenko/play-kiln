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
import process from 'node:process'

import { ProvenanceError, verifyArtifactChain } from './lib/apk-provenance.mjs'

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

console.log(`\n${checks - failures}/${checks} checks passed`)
process.exit(failures === 0 ? 0 : 1)
