#!/usr/bin/env node

import { spawnSync } from 'node:child_process'
import {
  copyFileSync,
  existsSync,
  mkdirSync,
  readFileSync,
  readdirSync,
  statSync,
  writeFileSync
} from 'node:fs'
import path from 'node:path'
import process from 'node:process'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { bytesToHuman, sha256 } from './lib/play-assets.mjs'
import { ensureSigning } from './sign-release.mjs'

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const line = (status, message) => console.log(`  ${status.padEnd(11)} ${message}`)

class ReleaseError extends Error {}

function argument(name) {
  const index = process.argv.indexOf(name)
  return index === -1 ? null : process.argv[index + 1] || null
}

function listAppDirs() {
  const appsDir = path.join(ROOT, 'apps')
  if (!existsSync(appsDir)) return []
  return readdirSync(appsDir)
    .map((name) => path.join(appsDir, name))
    .filter((candidate) => {
      try {
        return statSync(candidate).isDirectory()
      } catch {
        return false
      }
    })
}

function resolveSlotDir(slug) {
  const matches = listAppDirs().filter((candidate) => {
    const name = path.basename(candidate)
    return name === slug || name.endsWith(`-${slug}`)
  })
  if (matches.length === 0) throw new ReleaseError(`slot not found under apps/: ${slug}`)
  if (matches.length > 1) throw new ReleaseError(`slot is ambiguous: ${matches.map((item) => path.basename(item)).join(', ')}`)
  return matches[0]
}

function parseGradleMetadata(slotDir) {
  const file = path.join(slotDir, 'app', 'build.gradle.kts')
  if (!existsSync(file)) throw new ReleaseError('missing app/build.gradle.kts')
  const text = readFileSync(file, 'utf8')
  const applicationId = capture(text, /\bapplicationId\s*=\s*["']([^"']+)["']/, 'applicationId')
  const versionName = capture(text, /\bversionName\s*=\s*["']([^"']+)["']/, 'versionName')
  const versionCode = Number(capture(text, /\bversionCode\s*=\s*(\d+)/, 'versionCode'))
  const targetSdk = Number(capture(text, /\btargetSdk\s*=\s*(\d+)/, 'targetSdk'))
  const compileSdk = Number(capture(text, /\bcompileSdk\s*=\s*(\d+)/, 'compileSdk'))
  const minSdk = Number(capture(text, /\bminSdk\s*=\s*(\d+)/, 'minSdk'))
  const gradleName = text.match(/resValue\s*\(\s*["']string["']\s*,\s*["']app_name["']\s*,\s*["']([^"']+)["']\s*\)/)?.[1]
  const appName = gradleName || readAppName(slotDir) || path.basename(slotDir).replace(/^\d{4}-\d{2}-\d{2}-/, '')
  return { appName, applicationId, compileSdk, file, minSdk, targetSdk, text, versionCode, versionName }
}

function capture(text, pattern, label) {
  const value = text.match(pattern)?.[1]
  if (!value) throw new ReleaseError(`${label} not found in app/build.gradle.kts`)
  return value
}

function readAppName(slotDir) {
  const stringsFile = path.join(slotDir, 'app', 'src', 'main', 'res', 'values', 'strings.xml')
  if (!existsSync(stringsFile)) return null
  const value = readFileSync(stringsFile, 'utf8').match(/<string\s+name=["']app_name["'][^>]*>([\s\S]*?)<\/string>/)?.[1]
  return value ? decodeXml(value.trim()) : null
}

function decodeXml(value) {
  return value
    .replaceAll('&quot;', '"')
    .replaceAll('&apos;', "'")
    .replaceAll('&lt;', '<')
    .replaceAll('&gt;', '>')
    .replaceAll('&amp;', '&')
}

function rewriteVersionCode(metadata, requested) {
  if (requested === null) return metadata
  if (!/^\d+$/.test(requested) || Number(requested) < 1) throw new ReleaseError('--version-code must be a positive integer')
  const matches = [...metadata.text.matchAll(/\bversionCode\s*=\s*\d+/g)]
  if (matches.length !== 1) throw new ReleaseError(`expected exactly one versionCode assignment, found ${matches.length}`)
  const updated = metadata.text.replace(/\bversionCode\s*=\s*\d+/, `versionCode = ${Number(requested)}`)
  writeFileSync(metadata.file, updated, 'utf8')
  line('PASS', `versionCode updated to ${Number(requested)}`)
  return parseGradleMetadata(path.dirname(path.dirname(metadata.file)))
}

function runGradle(slotDir, task) {
  const windows = process.platform === 'win32'
  const wrapper = path.join(slotDir, windows ? 'gradlew.bat' : 'gradlew')
  if (!existsSync(wrapper)) throw new ReleaseError(`${path.basename(wrapper)} not found`)
  const result = spawnSync(wrapper, [task, '--console=plain'], {
    cwd: slotDir,
    encoding: 'utf8',
    shell: windows,
    windowsHide: true
  })
  const output = `${result.stdout || ''}\n${result.stderr || ''}`.trim()
  if (result.error || result.status !== 0) {
    line('FAIL', task)
    console.log(indent((result.error?.message || output || `exit ${result.status}`).split(/\r?\n/).slice(-60).join('\n')))
    throw new ReleaseError(`${task} failed`)
  }
  line('PASS', task)
}

function indent(value) {
  return value.split(/\r?\n/).map((entry) => `        ${entry}`).join('\n')
}

function locateApksigner() {
  const executable = process.platform === 'win32' ? 'apksigner.bat' : 'apksigner'
  const sdkRoot = (process.env.ANDROID_HOME || process.env.ANDROID_SDK_ROOT || '').replace(/^"|"$/g, '')
  if (sdkRoot) {
    const buildTools = path.join(sdkRoot, 'build-tools')
    if (existsSync(buildTools)) {
      const versions = readdirSync(buildTools)
        .map((name) => ({ name, parts: name.split(/[.-]/).map((part) => Number(part) || 0) }))
        .sort((a, b) => compareVersions(b.parts, a.parts))
      for (const version of versions) {
        const candidate = path.join(buildTools, version.name, executable)
        if (isFile(candidate)) return candidate
      }
    }
  }
  for (const entry of (process.env.PATH || '').split(path.delimiter).filter(Boolean)) {
    const candidate = path.join(entry.replace(/^"|"$/g, ''), executable)
    if (isFile(candidate)) return candidate
  }
  return null
}

function compareVersions(left, right) {
  for (let index = 0; index < Math.max(left.length, right.length); index += 1) {
    const difference = (left[index] || 0) - (right[index] || 0)
    if (difference !== 0) return difference
  }
  return 0
}

function isFile(file) {
  try {
    return statSync(file).isFile()
  } catch {
    return false
  }
}

function verifyApkSignature(apk, expectedFingerprint) {
  const apksigner = locateApksigner()
  if (!apksigner) {
    line('UNVERIFIED', 'release APK signature; apksigner was not found in Android SDK build-tools or PATH')
    return false
  }
  const result = spawnSync(apksigner, ['verify', '--print-certs', apk], {
    encoding: 'utf8',
    shell: process.platform === 'win32',
    windowsHide: true
  })
  if (result.error || result.status !== 0) {
    line('FAIL', 'release APK signature verification')
    console.log(indent(`${result.stderr || ''}\n${result.stdout || ''}`.trim() || result.error?.message || `exit ${result.status}`))
    return false
  }
  const output = `${result.stdout || ''}\n${result.stderr || ''}`
  const fingerprint = output.match(/certificate SHA-256 digest:\s*([0-9a-f:]+)/i)?.[1]
  if (!fingerprint) {
    line('FAIL', 'apksigner did not report the release certificate SHA-256 digest')
    return false
  }
  if (normalizeFingerprint(fingerprint) !== normalizeFingerprint(expectedFingerprint)) {
    line('FAIL', 'release APK certificate does not match the configured upload certificate')
    return false
  }
  line('PASS', 'release APK signature and upload certificate verified by apksigner')
  return true
}

function normalizeFingerprint(value) {
  return String(value).replace(/[^0-9a-f]/gi, '').toLowerCase()
}

function listZipEntries(file) {
  const bytes = readFileSync(file)
  const minimumEocd = 22
  if (bytes.length < minimumEocd) throw new ReleaseError('AAB is not a readable ZIP archive')
  const searchStart = Math.max(0, bytes.length - 0xffff - minimumEocd)
  let eocd = -1
  for (let offset = bytes.length - minimumEocd; offset >= searchStart; offset -= 1) {
    if (
      bytes.readUInt32LE(offset) === 0x06054b50 &&
      offset + minimumEocd + bytes.readUInt16LE(offset + 20) === bytes.length
    ) {
      eocd = offset
      break
    }
  }
  if (eocd === -1) throw new ReleaseError('AAB is not a readable ZIP archive')

  const entries = bytes.readUInt16LE(eocd + 10)
  const centralDirectorySize = bytes.readUInt32LE(eocd + 12)
  let offset = bytes.readUInt32LE(eocd + 16)
  const centralDirectoryEnd = offset + centralDirectorySize
  if (entries === 0xffff || centralDirectorySize === 0xffffffff || offset === 0xffffffff || centralDirectoryEnd > eocd) {
    throw new ReleaseError('AAB uses unsupported or malformed ZIP64 central-directory metadata')
  }
  const names = []
  for (let index = 0; index < entries; index += 1) {
    if (offset + 46 > bytes.length || bytes.readUInt32LE(offset) !== 0x02014b50) {
      throw new ReleaseError('AAB central directory is malformed or uses unsupported ZIP64 metadata')
    }
    const nameLength = bytes.readUInt16LE(offset + 28)
    const extraLength = bytes.readUInt16LE(offset + 30)
    const commentLength = bytes.readUInt16LE(offset + 32)
    names.push(bytes.toString('utf8', offset + 46, offset + 46 + nameLength))
    offset += 46 + nameLength + extraLength + commentLength
  }
  if (offset !== centralDirectoryEnd) throw new ReleaseError('AAB central-directory size does not match its entries')
  return names
}

function gitCommitHash() {
  const result = spawnSync('git', ['rev-parse', 'HEAD'], { cwd: ROOT, encoding: 'utf8', shell: false, windowsHide: true })
  return result.status === 0 ? result.stdout.trim() || null : null
}

function artifactRecord(slotDir, file) {
  const bytes = statSync(file).size
  return {
    path: path.relative(slotDir, file).split(path.sep).join('/'),
    sha256: sha256(file),
    bytes
  }
}

function runCli() {
  const slug = argument('--slot')
  if (!slug) {
    console.error('Usage: node scripts/prepare-release.mjs --slot <slug> [--skip-build] [--version-code N]')
    process.exitCode = 1
    return
  }

  console.log('## PlayKiln release preparation\n')
  try {
    const slotDir = resolveSlotDir(slug)
    let metadata = parseGradleMetadata(slotDir)
    line('PASS', `slot ${path.basename(slotDir)} (${metadata.applicationId})`)
    const requestedVersionCode = argument('--version-code')
    if (process.argv.includes('--version-code') && requestedVersionCode === null) {
      throw new ReleaseError('--version-code requires a positive integer')
    }
    if (process.argv.includes('--skip-build') && requestedVersionCode !== null) {
      throw new ReleaseError('--version-code cannot be combined with --skip-build because existing artifacts would be stale')
    }
    metadata = rewriteVersionCode(metadata, requestedVersionCode)
    if (!/^[A-Za-z0-9][A-Za-z0-9._-]*$/.test(metadata.versionName)) {
      throw new ReleaseError('versionName must be filename-safe (letters, digits, dot, underscore, or hyphen)')
    }
    line('PASS', `${metadata.appName} ${metadata.versionName} (${metadata.versionCode})`)

    const signing = ensureSigning(slotDir, { generate: false, log: line })

    const tasks = ['assembleDebug', 'assembleRelease', 'bundleRelease']
    if (process.argv.includes('--skip-build')) {
      for (const task of tasks) line('SKIP', `${task} (--skip-build)`)
    } else {
      for (const task of tasks) runGradle(slotDir, task)
    }

    const sourceArtifacts = [
      { kind: 'debug.apk', file: path.join(slotDir, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk') },
      { kind: 'release.apk', file: path.join(slotDir, 'app', 'build', 'outputs', 'apk', 'release', 'app-release.apk') },
      { kind: 'release.aab', file: path.join(slotDir, 'app', 'build', 'outputs', 'bundle', 'release', 'app-release.aab') }
    ]
    const missing = sourceArtifacts.filter((artifact) => !isFile(artifact.file))
    if (missing.length > 0) {
      for (const artifact of missing) line('FAIL', `missing artifact: ${path.relative(slotDir, artifact.file)}`)
      throw new ReleaseError('required build artifacts are missing')
    }
    for (const artifact of sourceArtifacts) line('PASS', `artifact exists: ${path.relative(slotDir, artifact.file)}`)

    const signatureVerified = verifyApkSignature(sourceArtifacts[1].file, signing.fingerprint)
    if (!signatureVerified) throw new ReleaseError('release APK signature is not verified')

    const aabEntries = listZipEntries(sourceArtifacts[2].file)
    const nativeLibraries = aabEntries.filter((entry) => /(?:^|\/)lib\/.*\.so$/i.test(entry))
    const pageSizeReadiness = nativeLibraries.length === 0
      ? { status: 'ready', note: 'pure Kotlin/Java — no native libs', nativeLibraries: [] }
      : {
          status: 'confirmation-required',
          note: '16 KB alignment must be confirmed for native libraries',
          nativeLibraries
        }
    const distDir = path.join(slotDir, 'dist')
    mkdirSync(distDir, { recursive: true })
    const prefix = `${slug}-${metadata.versionName}-${metadata.versionCode}`
    const destinations = [
      path.join(distDir, `${prefix}-debug.apk`),
      path.join(distDir, `${prefix}-release.apk`),
      path.join(distDir, `${prefix}-release.aab`)
    ]
    for (let index = 0; index < sourceArtifacts.length; index += 1) copyFileSync(sourceArtifacts[index].file, destinations[index])

    const artifacts = destinations.map((file) => artifactRecord(slotDir, file))
    for (const artifact of artifacts) line('PASS', `${artifact.path} — ${bytesToHuman(artifact.bytes)} — sha256 ${artifact.sha256}`)
    if (nativeLibraries.length === 0) line('PASS', '16 KB page-size readiness: pure Kotlin/Java — no native libs')
    else line('WARN', `16 KB alignment must be confirmed for ${nativeLibraries.length} native librar${nativeLibraries.length === 1 ? 'y' : 'ies'}`)

    const manifest = {
      appName: metadata.appName,
      slug,
      applicationId: metadata.applicationId,
      versionName: metadata.versionName,
      versionCode: metadata.versionCode,
      targetSdk: metadata.targetSdk,
      compileSdk: metadata.compileSdk,
      minSdk: metadata.minSdk,
      artifacts,
      signingFingerprint: signing.fingerprint,
      signing: {
        alias: signing.alias,
        fingerprintSha256: signing.fingerprint,
        apkVerifiedByApksigner: true
      },
      pageSizeReadiness,
      buildTimestamp: new Date().toISOString(),
      gitCommit: gitCommitHash()
    }
    const manifestFile = path.join(distDir, 'release-manifest.json')
    writeFileSync(manifestFile, `${JSON.stringify(manifest, null, 2)}\n`, 'utf8')
    line('PASS', path.relative(slotDir, manifestFile))
    console.log('\nVERDICT: READY FOR UPLOAD')
  } catch (error) {
    if (!(error instanceof ReleaseError) && typeof error?.exitCode !== 'number') line('FAIL', error.message)
    else if (error instanceof ReleaseError) line('FAIL', error.message)
    console.log('\nVERDICT: FAIL')
    process.exitCode = 1
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) runCli()
