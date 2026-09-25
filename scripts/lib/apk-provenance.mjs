/**
 * The chain that ties an installed package to a signed artifact.
 *
 * Kept separate from `media-capture.mjs` so it can be driven with fakes by
 * `scripts/test-apk-provenance.mjs`. The failure this exists to prevent is a *false pass*: a manifest
 * and an installed APK that agree with each other about a build neither of them is. Reproducing that
 * needs a stale manifest next to a rebuilt artifact, which is awkward to stage on a real device and
 * trivial with injected values.
 *
 * The chain is:
 *
 *   HEAD -> manifest.gitCommit -> manifest sha256 -> the artifact file on disk -> the installed base.apk
 *
 * Each link rejects a different staleness, and all of them are required:
 *
 *  - `gitCommit` ties the manifest to the sources being shipped, so a manifest built before them
 *    cannot vouch for a capture;
 *  - hashing the artifact *file* ties the recorded hash to bytes that exist. Comparing the recorded
 *    hash to the installed APK alone is the original defect: both can be old and still agree;
 *  - the installed hash ties what was photographed to that artifact.
 */

import { spawnSync } from 'node:child_process'
import process from 'node:process'

/** Thrown for any unproven identity. Publication must not continue. */
export class ProvenanceError extends Error {}

/**
 * True when `head` would still build the artifact recorded at `gitCommit`.
 *
 * Comparing commits for exact equality is wrong for this purpose: committing the captures advances
 * HEAD past the build they record, so the claim would fail the moment it was written down. What the
 * link actually needs to express is "HEAD would still produce this APK", so compare the app sources.
 *
 * The `ls-files` probe is not decoration. `git diff --quiet <a> <b> -- <path>` exits **0 when the
 * pathspec matches nothing**, so a renamed, absent, or misspelled app directory reads as "no changes"
 * and licenses precisely the staleness this exists to catch. Require tracked files to exist under the
 * path before trusting a clean diff.
 */
export function appSourcesUnchangedBetween(gitCommit, head, appDir, cwd = process.cwd()) {
  if (!gitCommit || !head) return false
  if (String(gitCommit) === String(head)) return true
  const tracked = spawnSync('git', ['ls-files', '--', appDir], { cwd, encoding: 'utf8' })
  if (tracked.status !== 0 || !String(tracked.stdout || '').trim()) return false
  const diff = spawnSync('git', ['diff', '--quiet', String(gitCommit), String(head), '--', appDir], {
    cwd, encoding: 'utf8'
  })
  return diff.status === 0
}

/**
 * @param {object} input
 * @param {string} input.manifestLabel   path shown in messages
 * @param {object|null} input.manifest   parsed release manifest, or null when absent
 * @param {string} input.head            current commit
 * @param {string|null} input.artifactPath   resolved path of the release APK, or null
 * @param {string|null} input.artifactHash   sha256 of that file, or null when it is unreadable
 * @param {string|null} input.installedHash  sha256 of the installed base.apk, or null
 * @param {string|null} input.installedApkPath  device path of the installed base.apk
 * @param {boolean} [input.appSourcesUnchanged] true when HEAD differs from `manifest.gitCommit` but the
 *   app sources are identical between them, so HEAD would still build this artifact. Committing the
 *   captures advances HEAD past the build commit, and without this the record could not outlive itself.
 * @returns {object} the record to persist with the captures
 */
export function verifyArtifactChain({
  manifestLabel,
  manifest,
  head,
  artifactPath,
  artifactHash,
  installedHash,
  installedApkPath,
  appSourcesUnchanged = false
}) {
  if (!manifest) {
    throw new ProvenanceError(
      `${manifestLabel} is absent, so the installed APK cannot be tied to a signed artifact. Build the ` +
      'release first (node scripts/prepare-release.mjs --slot <slug>): publishing a capture of an ' +
      'unidentified build would commit screenshots the documentation calls the release build.'
    )
  }
  const entry = (manifest.artifacts || []).find((item) => String(item.path || '').endsWith('release.apk'))
  if (!entry?.sha256) {
    throw new ProvenanceError(`${manifestLabel} records no release-APK sha256, so identity cannot be checked`)
  }
  const recorded = String(entry.sha256).toLowerCase()

  if (!manifest.gitCommit) {
    throw new ProvenanceError(`${manifestLabel} records no gitCommit, so it cannot be tied to the current sources`)
  }
  if (String(manifest.gitCommit) !== head && !appSourcesUnchanged) {
    throw new ProvenanceError(
      `${manifestLabel} was built from commit ${String(manifest.gitCommit).slice(0, 12)} but HEAD is ` +
      `${head.slice(0, 12)}, and the app sources differ between them. Rebuild the release so the ` +
      'manifest describes the sources being shipped, otherwise the capture would attest to a build ' +
      'the current tree does not produce.'
    )
  }

  // Manifest artifact paths are slot-relative, as `prepare-release.mjs` writes them.
  if (!artifactPath) {
    throw new ProvenanceError(
      `${manifestLabel} names ${entry.path}, which does not exist, so the artifact cannot be hashed. ` +
      'Comparing only the recorded hash string would let a stale manifest pass.'
    )
  }
  if (!artifactHash) {
    throw new ProvenanceError(`could not hash ${entry.path}, so the manifest cannot be tied to the artifact bytes`)
  }
  if (artifactHash !== recorded) {
    throw new ProvenanceError(
      `the release artifact does not match its own manifest: ${entry.path} hashes to ` +
      `${artifactHash.slice(0, 12)}… but ${manifestLabel} records ${recorded.slice(0, 12)}…. Rebuild the release.`
    )
  }

  if (!installedHash) {
    throw new ProvenanceError(
      'the installed base.apk could not be located or hashed, so it cannot be compared to the artifact'
    )
  }
  if (installedHash !== artifactHash) {
    throw new ProvenanceError(
      `the installed APK does not match the signed artifact. installed sha256=${installedHash} but ` +
      `${entry.path} is ${artifactHash}. Re-install the current release APK (adb install -r ` +
      'app/build/outputs/apk/release/app-release.apk), because a capture of a stale build would ' +
      'photograph a package that is not the one being uploaded.'
    )
  }

  return {
    artifactMatch: 'exact',
    gitCommit: head,
    manifestPath: manifestLabel,    artifactPath: entry.path,
    artifactSha256: artifactHash,
    installedSha256: installedHash,
    installedApkPath: installedApkPath ?? null
  }
}
