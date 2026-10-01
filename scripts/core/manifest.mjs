// The slot's AndroidManifest.xml and the source paths around it.
//
// Ported from `scripts/verify.mjs` before that gate is deleted, because these ledger rows had
// no other sensor anywhere: a search for
// `dataExtractionRules|allowBackup|uses-permission|usesCleartextTraffic|android:exported|foregroundServiceType`
// across `scripts/core/` and `scripts/verify.mjs` returned zero hits, and a search for
// `AndroidManifest` across `scripts/` matched only `verify.mjs`. Deleting the legacy gate
// without this module would have deleted the rules with it.
//
// Ledger row -> check id -> the legacy implementation each one was ported from:
//
//   §9.6, §9.8  -> slot.manifest       `checkDangerousPermissions` (verify.mjs 1170-1198),
//                                      `validateBackup` (1141-1169), the
//                                      `usesCleartextTraffic` branch (1524-1529)
//   §9.7        -> slot.backup         `validateBackup` (1141-1169)
//   §15.7       -> slot.capabilities   `verifyCapabilityCompliance` (1234-1344)
//   §16.11      -> slot.secretPaths    `checkSecrets` / `isGitIgnored` / `matchesIgnorePattern`
//                                      (1386-1432)
//   (none)      -> slot.tokens         `checkUnresolvedTokens` (1372-1385)
//
// Four differences from the legacy gate, each deliberate and each printed rather than implied:
//
//   1. `build/` is never read (§11.7's rule, and this change's own instruction): a stale generated
//      copy of a source file must not produce a finding. The legacy capability check read the
//      *merged* manifest under `app/build/` and FAILed when the build had not run; the direction
//      this module can measure from the sources is the one that makes the merge carry a capability
//      at all — the declaration in `app/build.gradle.kts` / `gradle/libs.versions.toml` /
//      `build.gradle.kts`. That is the build-side half of §15.7; the archetype half is already
//      `slot.economics` (`scripts/core/build.mjs`), which imports `validateEconomicsAgainstCapabilities`
//      rather than restating it, so nothing here duplicates it.
//   2. A missing input is `UNVERIFIED`, never `FAIL`: the legacy gate reported a missing
//      `capabilities.json` as `FAIL`. A file that is not there has not been measured, in either
//      direction.
//   3. The permission half of §15.7 keeps the legacy scope, `['ads', 'billing']`. Widening it to
//      every registered capability would report `android.permission.INTERNET`, because
//      `capabilities/remoteconfig` owns that name and every slot declares it for the ads and Play
//      Billing SDKs on purpose — a finding about the wrong thing is worse than no finding.
//   4. §9.7's "and excludes secrets" is measured here and was not measured by the legacy gate: a
//      backup section that includes a whole domain while excluding no secret-bearing path is a
//      finding. A section that includes named files only cannot leak a secret it does not name, so
//      the reference slot's `recordbook.json` / `billing_state.xml` rules are not affected.
//
// Every finding names the file and the line or the attribute, so it is actionable without a build.
import { existsSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs'
import { SCRATCH_DIR } from '../lib/scratch.mjs'
import path from 'node:path'
import { aggregateSlotCapabilities, discoverCapabilities } from '../lib/capabilities.mjs'
// Imported, never re-implemented: `decodeSource` is the one place that decides a file is binary
// (`bytes.includes(0)`), which is the rule a text sweep must obey (§16.3).
import { decodeSource } from './build.mjs'
import { expectEqual, expectTrue, fail, guard, pass, ROOT, runFixtures, unverified } from './lib.mjs'

export const MANIFEST_REL = 'app/src/main/AndroidManifest.xml'
const DATA_SAFETY_REL = 'store/data-safety.md'
const BASE_PERMISSION = 'android.permission.'

/**
 * The gated permissions, verbatim from `checkDangerousPermissions` (verify.mjs 1170-1188).
 * Declaring one is not forbidden; declaring one without `store/data-safety.md` naming it is.
 */
export const GATED_PERMISSIONS = Object.freeze([
  'CAMERA',
  'RECORD_AUDIO',
  'ACCESS_FINE_LOCATION',
  'ACCESS_COARSE_LOCATION',
  'READ_CONTACTS',
  'READ_SMS',
  'READ_CALL_LOG',
  'MANAGE_EXTERNAL_STORAGE',
  'QUERY_ALL_PACKAGES',
  'POST_NOTIFICATIONS'
])

/** Restricted without a declared use case (Play's exact-alarm policy; ledger §9.6). */
export const RESTRICTED_PERMISSIONS = Object.freeze(['SCHEDULE_EXACT_ALARM', 'USE_EXACT_ALARM'])

/** The legacy capability-permission scope, kept narrow on purpose — see difference 3 in the header. */
export const PERMISSION_SCOPE = Object.freeze(['ads', 'billing'])

/** Paths a backup rule must exclude before it may include a whole domain (ledger §9.7). */
export const SECRET_PATH_HINTS = Object.freeze([/secret/i, /token/i, /keystore/i, /credential/i, /\.env$/i, /\.key$/i])

/** Directories a source-only check must not descend into. `build/` is the one the rule names. */
const SKIP_SEGMENTS = new Set(['build', '.gradle', '.kotlin', '.idea', '.git', '.cxx', '__pycache__', 'node_modules'])
const SOURCE_CODE = /\.(?:kt|java)$/i
const TOKEN = /__[A-Z_]+__/

const relPath = (file) => path.relative(ROOT, file).split(path.sep).join('/')
const lineOf = (text, index) => text.slice(0, index).split(/\r?\n/).length
const escapeRegExp = (value) => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

// ---------------------------------------------------------------------------------------------
// Primitives: reading the manifest and the source tree
// ---------------------------------------------------------------------------------------------

/** Every file under `dir`, skipping build output and toolchain state. Never throws. */
export function walkSlotFiles(dir, acc = []) {
  let entries
  try {
    entries = readdirSync(dir, { withFileTypes: true })
  } catch {
    return acc
  }
  for (const entry of entries) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      if (!SKIP_SEGMENTS.has(entry.name)) walkSlotFiles(full, acc)
    } else if (entry.isFile()) acc.push(full)
  }
  return acc
}

/**
 * Reads the slot's own manifest. The three states are distinct and never collapsed:
 * `{ missing: true }`, `{ error }`, or `{ text }`.
 */
export function readManifestState(slotDir) {
  const file = path.join(slotDir, MANIFEST_REL)
  if (!existsSync(file)) return { file, missing: true }
  try {
    return { file, text: readFileSync(file, 'utf8') }
  } catch (error) {
    return { file, error: error.message }
  }
}

/** The verdict for an input that could not be read. A missing input is never a pass. */
function manifestUnverified(id, state) {
  return state.missing
    ? unverified(id, `no ${relPath(state.file)} to read; a file that is not there has not been measured`)
    : unverified(id, `could not read ${relPath(state.file)}: ${state.error}`)
}

/**
 * Why this manifest cannot be read as a whole, or null when it is structurally complete.
 *
 * The checks below read the file with regular expressions rather than an XML parser, so this is the
 * one guard that keeps "the file does not parse" from reading as "the file declares nothing": a
 * truncated manifest must be `UNVERIFIED`, never `PASS` (and never a `FAIL` about rules that were
 * never read).
 */
export function manifestStructureProblem(text) {
  if (!/<manifest\b/.test(text)) return 'no <manifest> element'
  if (!/<\/manifest>/.test(text)) return 'no </manifest>, so the file is truncated'
  if (/<application\b/.test(text) && !/<\/application>/.test(text) && !/<application\b[^>]*\/>/.test(text)) {
    return 'the <application> element is never closed'
  }
  return null
}

/** The `<application ...>` element's attributes and the line it sits on, or null. */
export function applicationTag(text) {
  const match = /<application\b((?:[^>"']|"[^"]*"|'[^']*')*)(\/>|>)/.exec(text)
  return match ? { attributes: match[1], line: lineOf(text, match.index) } : null
}

/** The value of one attribute inside an attribute blob, or null when it is not declared. */
export function attributeValue(attributes, name) {
  const match = new RegExp(`${escapeRegExp(name)}(?=\\s*=)\\s*=\\s*"([^"]*)"`).exec(attributes)
  return match ? match[1] : null
}

/** Every `<uses-permission>` / `<uses-permission-sdk-23>` with its name and line. */
export function usesPermissions(text) {
  const rows = []
  for (const match of text.matchAll(/<uses-permission(?:-sdk-23)?\b((?:[^>"']|"[^"]*"|'[^']*')*)\/?>/g)) {
    rows.push({ name: attributeValue(match[1], 'android:name') || '', line: lineOf(text, match.index) })
  }
  return rows
}

/**
 * Every component element with the three attributes the export rule needs.
 *
 * `hasIntentFilter` is the manifest's own record that the export is required: a component the
 * launcher starts carries one, and a component exported with no filter is reachable by any app on
 * the device for no stated reason (ledger §9.6).
 */
export function manifestComponents(text) {
  const rows = []
  const pattern = /<(activity|activity-alias|service|receiver|provider)\b((?:[^>"']|"[^"]*"|'[^']*')*)(\/>|>([\s\S]*?)<\/\1>)/g
  for (const match of text.matchAll(pattern)) {
    const [, kind, attributes, , body] = match
    rows.push({
      kind,
      name: attributeValue(attributes, 'android:name'),
      exported: attributeValue(attributes, 'android:exported'),
      foregroundServiceType: attributeValue(attributes, 'android:foregroundServiceType'),
      hasIntentFilter: /<intent-filter\b/.test(body || ''),
      line: lineOf(text, match.index)
    })
  }
  return rows
}

/** `<file>:<line>` for every line of Kotlin/Java under `app/src/main/` matching `pattern`. */
export function codeSites(slotDir, pattern) {
  const sites = []
  for (const file of walkSlotFiles(path.join(slotDir, 'app', 'src', 'main'))) {
    if (!SOURCE_CODE.test(file)) continue
    let text = null
    try {
      text = decodeSource(readFileSync(file))
    } catch {
      continue
    }
    if (text === null) continue
    text.split(/\r?\n/).forEach((line, index) => {
      if (pattern.test(line)) sites.push(`${relPath(file)}:${index + 1}`)
    })
  }
  return sites
}

/** `minSdk` from `app/build.gradle.kts`, or null when it cannot be read. */
export function minSdkOf(slotDir) {
  const file = path.join(slotDir, 'app', 'build.gradle.kts')
  if (!existsSync(file)) return null
  try {
    const match = /\bminSdk\s*=\s*(\d+)/.exec(readFileSync(file, 'utf8'))
    return match ? Number(match[1]) : null
  } catch {
    return null
  }
}

function dataSafetyText(slotDir) {
  const file = path.join(slotDir, DATA_SAFETY_REL)
  if (!existsSync(file)) return null
  try {
    return readFileSync(file, 'utf8').toUpperCase()
  } catch {
    return null
  }
}

/** A `@xml/name` reference's name, or null. */
export function xmlReference(value) {
  const match = value === null ? null : /^@xml\/([A-Za-z0-9_]+)$/.exec(value.trim())
  return match ? match[1] : null
}

/**
 * The problems in one backup-rules XML, for the two sectioned shapes and the legacy one.
 *
 * `sections` is the list this file's root element defines: `['cloud-backup','device-transfer']` for
 * `data-extraction-rules`, `[]` for `full-backup-content`, which has no separate transfer policy.
 * Each section must exist and carry at least one include/exclude rule (the legacy rule), and a
 * section that includes a whole domain must exclude a secret-bearing path (ledger §9.7).
 */
export function backupSectionProblems(xml, relXml, sections = ['cloud-backup', 'device-transfer']) {
  const problems = []
  const stripped = xml.replace(/<!--[\s\S]*?-->/g, '')
  if (sections.length === 0) {
    const body = stripped
    if (!/<(?:include|exclude)\b/.test(body)) {
      return [`${relXml}: carries no include/exclude rule`]
    }
    return domainIncludeProblems(body, relXml, null)
  }
  for (const section of sections) {
    const match = new RegExp(`<${section}\\b[^>]*>([\\s\\S]*?)<\\/${section}>`).exec(stripped)
    if (!match) {
      problems.push(`${relXml}: no <${section}> section, so cloud backup and device transfer are not separated`)
      continue
    }
    if (!/<(?:include|exclude)\b/.test(match[1])) {
      problems.push(`${relXml}: <${section}> carries no include/exclude rule`)
      continue
    }
    problems.push(...domainIncludeProblems(match[1], relXml, section))
  }
  return problems
}

function domainIncludeProblems(body, relXml, section) {
  const where = section ? `<${section}>` : 'the file'
  const broad = [...body.matchAll(/<include\b[^>]*\bpath\s*=\s*"([^"]*)"/g)]
    .map((match) => match[1])
    .some((value) => value === '' || value === '.' || value === './')
  if (!broad) return []
  const excluded = [...body.matchAll(/<exclude\b[^>]*\bpath\s*=\s*"([^"]*)"/g)]
    .map((match) => match[1])
    .some((value) => SECRET_PATH_HINTS.some((hint) => hint.test(value)))
  return excluded
    ? []
    : [`${relXml}: ${where} includes a whole domain but excludes no secret-bearing path (a secrets, tokens, keystore, credential or .env name)`]
}

// ---------------------------------------------------------------------------------------------
// Primitives: .gitignore matching, ported verbatim from verify.mjs 1407-1432
// ---------------------------------------------------------------------------------------------

/** Last matching pattern wins, and `!` negates — git's own rule, as the legacy gate implemented it. */
export function isGitIgnored(relativePath, patterns) {
  let ignored = false
  for (const raw of patterns) {
    const trimmed = raw.trim()
    if (!trimmed || trimmed.startsWith('#')) continue
    const negated = trimmed.startsWith('!')
    const pattern = (negated ? trimmed.slice(1) : trimmed).replaceAll('\\', '/')
    if (matchesIgnorePattern(relativePath, pattern)) ignored = !negated
  }
  return ignored
}

export function matchesIgnorePattern(relativePath, rawPattern) {
  const anchored = rawPattern.startsWith('/')
  const directory = rawPattern.endsWith('/')
  let pattern = rawPattern.replace(/^\//, '').replace(/\/$/, '')
  if (!pattern) return false
  const globstarDirectory = '\u0001'
  const globstar = '\u0000'
  pattern = pattern.replaceAll('**/', globstarDirectory).replaceAll('**', globstar)
  const escaped = pattern.replace(/[.+^${}()|[\]\\]/g, '\\$&').replaceAll('*', '[^/]*').replaceAll('?', '[^/]')
  const glob = escaped.replaceAll(globstarDirectory, '(?:.*/)?').replaceAll(globstar, '.*')
  const prefix = anchored || rawPattern.includes('/') ? '^' : '(?:^|/)'
  const suffix = directory ? '(?:/.*)?$' : '$'
  return new RegExp(prefix + glob + suffix).test(relativePath)
}

/** The names §16.11 keeps out of git. `.env.example` is deliberately not one of them. */
export function isSecretFileName(name) {
  const lower = name.toLowerCase()
  return (
    lower.endsWith('.jks') ||
    lower.endsWith('.keystore') ||
    ['keystore.properties', '.env', 'google-services.json', 'play-service-account.json'].includes(lower)
  )
}

// ---------------------------------------------------------------------------------------------
// The capability build half
// ---------------------------------------------------------------------------------------------

const BUILD_FILES = ['app/build.gradle.kts', 'gradle/libs.versions.toml', 'build.gradle.kts']

function slotBuildText(slotDir) {
  const parts = []
  for (const rel of BUILD_FILES) {
    const file = path.join(slotDir, rel)
    if (!existsSync(file)) continue
    try {
      parts.push(readFileSync(file, 'utf8'))
    } catch {
      return null
    }
  }
  return parts.length === 0 ? null : parts.join('\n')
}

/** True when the build files name this capability, by catalog alias or by artifact coordinate. */
export function declaresCapability(buildText, id) {
  const escaped = escapeRegExp(id)
  return new RegExp(`libs\\.capability\\.${escaped}\\b|com\\.gexetr\\.capability:${escaped}\\b`).test(buildText)
}

// ---------------------------------------------------------------------------------------------
// The checks
// ---------------------------------------------------------------------------------------------

/**
 * Ledger §9.6 and §9.8: permissions minimized, no blanket permission, no unjustified exported
 * component, no cleartext traffic, a foreground-service type where a foreground service exists,
 * exact alarms treated as restricted, predictive back on.
 */
export function checkManifest(slotDir) {
  return guard('slot.manifest', () => {
    const state = readManifestState(slotDir)
    if (!state.text) return manifestUnverified('slot.manifest', state)
    const { text, file } = state
    const relFile = relPath(file)
    const structure = manifestStructureProblem(text)
    if (structure) return unverified('slot.manifest', `${relFile}: ${structure}`)
    const application = applicationTag(text)
    if (!application) {
      return unverified(
        'slot.manifest',
        `${relFile}: no <application> element, so the cleartext, foreground-service, back-callback and export rules cannot be read`
      )
    }
    const problems = []
    const permissions = usesPermissions(text)

    // Permissions: the minimum, and never a wildcard.
    const disclosure = dataSafetyText(slotDir)
    for (const row of permissions) {
      if (!row.name) {
        problems.push(`${relFile}:${row.line}: <uses-permission> declares no android:name`)
        continue
      }
      if (row.name.includes('*')) {
        problems.push(`${relFile}:${row.line}: blanket permission "${row.name}" — a wildcard is never the minimum set`)
        continue
      }
      const bare = row.name.startsWith(BASE_PERMISSION) ? row.name.slice(BASE_PERMISSION.length) : row.name
      if (!GATED_PERMISSIONS.includes(bare)) continue
      if (disclosure === null) {
        problems.push(
          `${relFile}:${row.line}: ${bare} is a gated permission and no ${DATA_SAFETY_REL} exists to document the need`
        )
      } else if (!disclosure.includes(bare)) {
        problems.push(`${relFile}:${row.line}: ${bare} is a gated permission and ${DATA_SAFETY_REL} does not mention it`)
      }
    }

    // A component is exported only where the manifest itself records the reason.
    for (const component of manifestComponents(text)) {
      if (component.exported === 'true' && !component.hasIntentFilter) {
        problems.push(
          `${relFile}:${component.line}: ${component.kind}${component.name ? ` ${component.name}` : ''} is ` +
            `android:exported="true" with no <intent-filter>, so nothing in the manifest records a requirement for the export`
        )
      }
    }

    // HTTPS by default: cleartext is never enabled in the application element.
    if (attributeValue(application.attributes, 'android:usesCleartextTraffic') === 'true') {
      problems.push(
        `${relFile}:${application.line}: android:usesCleartextTraffic="true" allows plaintext HTTP; HTTPS is the default and pinning needs a threat model`
      )
    }

    // A foreground service must declare its type.
    const foregroundPermission = permissions.some((row) => row.name.startsWith(`${BASE_PERMISSION}FOREGROUND_SERVICE`))
    const startForeground = codeSites(slotDir, /startForeground\s*\(/)
    const typed = manifestComponents(text).filter((c) => c.kind === 'service' && c.foregroundServiceType)
    if ((foregroundPermission || startForeground.length > 0) && typed.length === 0) {
      const trigger = startForeground.length > 0 ? startForeground[0] : `${BASE_PERMISSION}FOREGROUND_SERVICE`
      problems.push(
        `${relFile}: a foreground service is used (${trigger}) but no <service android:foregroundServiceType="…"> is declared, ` +
          `which Android 14+ requires and Play treats as an undeclared use case`
      )
    }

    // Exact alarms are restricted, so declaring one is a finding rather than a default.
    for (const restricted of RESTRICTED_PERMISSIONS) {
      const row = permissions.find((permission) => permission.name === `${BASE_PERMISSION}${restricted}`)
      if (row) {
        problems.push(
          `${relFile}:${row.line}: ${restricted} is a restricted permission reserved for alarm and calendar apps; ` +
            `a slot that needs it must say why in App content before it ships`
        )
      }
    }

    // Predictive back: no opt-out at the target SDK unless a change design records one.
    const backCallback = attributeValue(application.attributes, 'android:enableOnBackInvokedCallback')
    if (backCallback === 'false' && !backOptOut()) {
      problems.push(
        `${relFile}:${application.line}: android:enableOnBackInvokedCallback="false" opts out of predictive back, ` +
          `and no openspec change design records a temporary opt-out`
      )
    }

    if (problems.length > 0) return fail('slot.manifest', problems.join('; '))
    const back = backCallback === null ? 'not declared' : backCallback
    return pass(
      'slot.manifest',
      `${permissions.length} permission(s), none gated-and-undocumented, none a wildcard and none a restricted ` +
        `exact-alarm permission; no unjustified exported component; cleartext off; ` +
        `enableOnBackInvokedCallback ${back}`
    )
  })
}

/** A recorded temporary predictive-back opt-out in a change design, or null. */
function backOptOut() {
  const changes = path.join(ROOT, 'openspec', 'changes')
  for (const file of walkSlotFiles(changes)) {
    if (path.basename(file) !== 'design.md') continue
    let text = null
    try {
      text = decodeSource(readFileSync(file))
    } catch {
      continue
    }
    if (text === null) continue
    for (const line of text.split(/\r?\n/)) {
      if (!line.includes('enableOnBackInvokedCallback')) continue
      if (/false|opt[ -]?out/i.test(line)) return `${relPath(file)}: ${line.trim()}`
    }
  }
  return null
}

/**
 * Ledger §9.7: `dataExtractionRules` (plus the legacy `fullBackupContent` where `minSdk` <= 30)
 * points at an XML that separates cloud backup from device transfer and excludes secret material.
 */
export function checkBackup(slotDir) {
  return guard('slot.backup', () => {
    const state = readManifestState(slotDir)
    if (!state.text) return manifestUnverified('slot.backup', state)
    const { text, file } = state
    const relFile = relPath(file)
    const structure = manifestStructureProblem(text)
    if (structure) return unverified('slot.backup', `${relFile}: ${structure}`)
    const application = applicationTag(text)
    if (!application) {
      return unverified('slot.backup', `${relFile}: no <application> element, so the backup attributes cannot be read`)
    }
    const problems = []

    const allowBackup = attributeValue(application.attributes, 'android:allowBackup')
    if (allowBackup === null) {
      problems.push(`${relFile}:${application.line}: no android:allowBackup, so backup is decided by a platform default`)
    }

    const rulesValue = attributeValue(application.attributes, 'android:dataExtractionRules')
    const rulesName = xmlReference(rulesValue)
    if (!rulesName) {
      problems.push(
        `${relFile}:${application.line}: android:dataExtractionRules is missing or does not reference a @xml/ resource ` +
          `(found ${rulesValue === null ? 'no attribute' : `"${rulesValue}"`})`
      )
    } else {
      if (rulesName !== 'data_extraction_rules') {
        problems.push(
          `${relFile}:${application.line}: android:dataExtractionRules must reference @xml/data_extraction_rules, not @xml/${rulesName}`
        )
      }
      const rulesFile = path.join(slotDir, 'app', 'src', 'main', 'res', 'xml', `${rulesName}.xml`)
      if (!existsSync(rulesFile)) {
        problems.push(`${relFile}:${application.line}: dataExtractionRules points at @xml/${rulesName}, which ${relPath(rulesFile)} does not provide`)
      } else {
        problems.push(...backupSectionProblems(readFileSync(rulesFile, 'utf8'), relPath(rulesFile)))
      }
    }

    // The legacy half. `fullBackupContent` is the API <= 30 policy, so it is required exactly where
    // a device can still be running one of those APIs.
    const minSdk = minSdkOf(slotDir)
    const fullBackupValue = attributeValue(application.attributes, 'android:fullBackupContent')
    let legacyUnmeasured = false
    if (minSdk === null) {
      legacyUnmeasured = true
    } else if (minSdk <= 30) {
      const legacyName = xmlReference(fullBackupValue)
      if (!legacyName) {
        problems.push(
          `${relFile}:${application.line}: minSdk ${minSdk} is <= 30, so the legacy android:fullBackupContent rules are ` +
            `required alongside android:dataExtractionRules (found ${fullBackupValue === null ? 'no attribute' : `"${fullBackupValue}"`})`
        )
      } else {
        const legacyFile = path.join(slotDir, 'app', 'src', 'main', 'res', 'xml', `${legacyName}.xml`)
        if (!existsSync(legacyFile)) {
          problems.push(`${relFile}:${application.line}: fullBackupContent points at @xml/${legacyName}, which ${relPath(legacyFile)} does not provide`)
        } else {
          problems.push(...backupSectionProblems(readFileSync(legacyFile, 'utf8'), relPath(legacyFile), []))
        }
      }
    }

    if (problems.length > 0) return fail('slot.backup', problems.join('; '))
    if (legacyUnmeasured) {
      return unverified(
        'slot.backup',
        `${relFile}: minSdk could not be read from app/build.gradle.kts, so the API <= 30 fullBackupContent obligation was not decided`
      )
    }
    return pass(
      'slot.backup',
      `allowBackup=${allowBackup === null ? 'unset' : allowBackup}; dataExtractionRules separates cloud-backup from ` +
        `device-transfer and excludes secret material${minSdk !== null && minSdk <= 30 ? `; fullBackupContent covers API <= 30 (minSdk ${minSdk})` : ''}`
    )
  })
}

/**
 * Ledger §15.7, the build-side half: the capability plan the shipped build can honour.
 *
 * The archetype half is `slot.economics`; this is what the *build* declares — a capability the plan
 * names whose SDK is not on the classpath cannot be shipped, and a capability SDK on the classpath
 * that the plan does not name is a build carrying something nobody planned.
 */
export function checkCapabilities(slotDir) {
  return guard('slot.capabilities', () => {
    const configFile = path.join(slotDir, 'capabilities.json')
    if (!existsSync(configFile)) {
      return unverified(
        'slot.capabilities',
        `no ${relPath(configFile)}; the capability plan the shipped build would honour cannot be read, and a plan that is not there has not been measured`
      )
    }

    let registry
    let aggregate
    try {
      registry = discoverCapabilities(ROOT)
      aggregate = aggregateSlotCapabilities({ rootDir: ROOT, slotDir })
    } catch (error) {
      return /capability registry|no capability manifests/.test(error.message)
        ? unverified('slot.capabilities', error.message)
        : fail('slot.capabilities', error.message)
    }

    const problems = []
    const unmeasured = []
    const enabled = new Set(aggregate.enabled)
    const buildText = slotBuildText(slotDir)

    if (buildText === null) {
      unmeasured.push(`no ${BUILD_FILES.join(', ')} under ${relPath(slotDir)} to read the declared capability SDKs from`)
    } else {
      for (const id of aggregate.enabled) {
        if (!declaresCapability(buildText, id)) {
          problems.push(
            `capabilities.json enables '${id}', but the build files declare no '${id}' SDK — ` +
              `expected libs.capability.${id} in app/build.gradle.kts or com.gexetr.capability:${id}`
          )
        }
      }
      for (const [id, manifest] of registry) {
        // The baseline capability every slot consumes is not "enabled" in capabilities.json;
        // requiring it there would fail every slot on the registry's own baseline module.
        if (manifest.optionalForSlots !== true || enabled.has(id)) continue
        if (declaresCapability(buildText, id)) {
          problems.push(
            `the build declares the '${id}' capability, but capabilities.json does not enable it, ` +
              `so the shipped build carries a capability the plan does not name`
          )
        }
      }
    }

    // The permission half, in the legacy scope (see difference 3 in the header).
    const state = readManifestState(slotDir)
    if (!state.text) {
      unmeasured.push(`no ${relPath(path.join(slotDir, MANIFEST_REL))} to read the declared permissions from`)
    } else {
      const declared = new Set(usesPermissions(state.text).map((row) => row.name))
      for (const id of PERMISSION_SCOPE) {
        const manifest = registry.get(id)
        if (!manifest || enabled.has(id)) continue
        for (const permission of manifest.permissions ?? []) {
          if (declared.has(permission.name)) {
            problems.push(
              `${relPath(path.join(slotDir, MANIFEST_REL))}: declares ${permission.name}, which the '${id}' capability owns, ` +
                `but capabilities.json does not enable '${id}'`
            )
          }
        }
      }
    }

    if (problems.length > 0) return fail('slot.capabilities', problems.join('; '))
    if (unmeasured.length > 0) return unverified('slot.capabilities', unmeasured.join('; '))
    const declaredIds = aggregate.enabled.length > 0 ? aggregate.enabled.join(', ') : 'none'
    return pass(
      'slot.capabilities',
      `capabilities.json enables ${declaredIds}; every enabled capability's SDK is declared in the build, ` +
        `and no capability-owned permission (${PERMISSION_SCOPE.join('/')}) is declared without its capability`
    )
  })
}

/**
 * Ledger §16.11: a secret lives only where the slot's own `.gitignore` covers it.
 *
 * The names are the ones the legacy gate kept out of git, and `.env.example` is deliberately not
 * one of them: it is a placeholder file that must be committed.
 */
export function checkSecretPaths(slotDir) {
  return guard('slot.secretPaths', () => {
    const present = walkSlotFiles(slotDir).filter((file) => isSecretFileName(path.basename(file)))
    if (present.length === 0) return pass('slot.secretPaths', 'no in-slot secret file present')

    const ignoreFile = path.join(slotDir, '.gitignore')
    const relative = present.map((file) => path.relative(slotDir, file).split(path.sep).join('/')).sort()
    if (!existsSync(ignoreFile)) {
      return fail(
        'slot.secretPaths',
        `${relPath(ignoreFile)} does not exist, so git would track ${present.length} secret path(s): ${relative.join(', ')}`
      )
    }
    const patterns = readFileSync(ignoreFile, 'utf8').split(/\r?\n/)
    const problems = relative
      .filter((rel) => !isGitIgnored(rel, patterns))
      .map((rel) => `${rel}: not matched by ${relPath(ignoreFile)}, so git would track this secret`)
    return problems.length > 0
      ? fail('slot.secretPaths', problems.join('; '))
      : pass('slot.secretPaths', `${relative.length} secret path(s) present and each ignored by ${relPath(ignoreFile)}: ${relative.join(', ')}`)
  })
}

/**
 * No unresolved `__TOKEN__` placeholder survives in a slot's sources.
 *
 * `scripts/new-slot.mjs` sweeps the tokens once, at scaffold time, and nothing swept them again:
 * a template token that survives a later edit is invisible until a user reads `__APP_NAME__` on a
 * screen. Binary files are skipped by the shared `decodeSource` predicate (§16.3).
 */
export function checkTokens(slotDir) {
  return guard('slot.tokens', () => {
    const files = walkSlotFiles(slotDir)
    const findings = []
    const unreadable = []
    for (const file of files) {
      let text = null
      try {
        text = decodeSource(readFileSync(file))
      } catch (error) {
        unreadable.push(`${relPath(file)}: ${error.message}`)
        continue
      }
      if (text === null) continue
      const match = TOKEN.exec(text)
      if (match) findings.push(`${relPath(file)}:${lineOf(text, match.index)}: ${match[0]}`)
    }
    if (findings.length > 0) {
      return fail('slot.tokens', `unresolved template token(s): ${findings.slice(0, 20).join(', ')}`)
    }
    if (unreadable.length > 0) {
      return unverified('slot.tokens', `could not read: ${unreadable.join('; ')}`)
    }
    return pass('slot.tokens', `no unresolved __TOKEN__ placeholder in ${files.length} file(s)`)
  })
}

/** The manifest facts that need a slot, mirroring the other modules' `*Checks` aggregators. */
export function manifestChecks(slotDir) {
  return [
    checkManifest(slotDir),
    checkBackup(slotDir),
    checkCapabilities(slotDir),
    checkSecretPaths(slotDir),
    checkTokens(slotDir)
  ]
}

// ---------------------------------------------------------------------------------------------
// Inline fixtures — the sensors this module depends on, made falsifiable
// ---------------------------------------------------------------------------------------------

const GITIGNORE_OK = [
  '# Local SDK configuration and build-system state.',
  'local.properties',
  '.gradle/',
  '.kotlin/',
  '',
  '# Generated build and packaged artifacts.',
  'build/',
  '**/build/',
  '*.apk',
  '*.aab',
  'captures/',
  'dist/',
  '.cxx/',
  '',
  '# Signing material.',
  '*.jks',
  '*.keystore',
  'keystore.properties',
  '',
  '# Local environment and service credentials.',
  '.env',
  '.env.*',
  '!.env.example',
  'google-services.json',
  'play-service-account.json',
  ''
].join('\n')

const ACTIVITY_WITH_FILTER = [
  '        <activity android:name=".MainActivity" android:exported="true">',
  '            <intent-filter>',
  '                <action android:name="android.intent.action.MAIN" />',
  '                <category android:name="android.intent.category.LAUNCHER" />',
  '            </intent-filter>',
  '        </activity>'
].join('\n')

/** A minimal but real manifest, parameterised by the one attribute a fixture is testing. */
export function baseManifest(options = {}) {
  const rulesName = options.dataExtractionRules === undefined ? 'data_extraction_rules' : options.dataExtractionRules
  const legacyName = options.fullBackupContent === undefined ? 'backup_rules' : options.fullBackupContent
  const applications = [
    '        android:allowBackup="true"',
    rulesName === null ? null : `        android:dataExtractionRules="@xml/${rulesName}"`,
    `        android:enableOnBackInvokedCallback="${options.backCallback || 'true'}"`,
    legacyName === null ? null : `        android:fullBackupContent="@xml/${legacyName}"`,
    '        android:label="@string/app_name"',
    options.cleartext ? `        android:usesCleartextTraffic="${options.cleartext}"` : null
  ].filter((line) => line !== null)
  const permissions = (options.permissions || []).map((name) => `    <uses-permission android:name="${name}" />`)
  const lines = [
    '<?xml version="1.0" encoding="utf-8"?>',
    '<manifest xmlns:android="http://schemas.android.com/apk/res/android">',
    ...permissions,
    '    <application',
    ...applications.map((line, index) => (index === applications.length - 1 ? `${line}>` : line)),
    options.components === undefined ? (options.selfClosedActivity ? '        <activity android:name=".MainActivity" android:exported="true" />' : ACTIVITY_WITH_FILTER) : options.components,
    '    </application>',
    '</manifest>',
    ''
  ]
  return lines.join('\n')
}

const DATA_EXTRACTION_OK = [
  '<?xml version="1.0" encoding="utf-8"?>',
  '<data-extraction-rules>',
  '    <cloud-backup>',
  '        <include domain="sharedpref" path="." />',
  '        <exclude domain="sharedpref" path="secrets.xml" />',
  '    </cloud-backup>',
  '    <device-transfer>',
  '        <include domain="file" path="." />',
  '        <exclude domain="file" path="tokens/" />',
  '    </device-transfer>',
  '</data-extraction-rules>',
  ''
].join('\n')

const FULL_BACKUP_OK = [
  '<?xml version="1.0" encoding="utf-8"?>',
  '<full-backup-content>',
  '    <include domain="sharedpref" path="." />',
  '    <exclude domain="sharedpref" path="secrets.xml" />',
  '</full-backup-content>',
  ''
].join('\n')

/** The smallest slot tree a manifest check can be driven with: source files only, no build output. */
function fixtureFiles(overrides = {}) {
  return {
    'app/build.gradle.kts': 'android {\n    defaultConfig {\n        minSdk = 26\n    }\n}\n',
    [MANIFEST_REL]: baseManifest(),
    'app/src/main/res/xml/data_extraction_rules.xml': DATA_EXTRACTION_OK,
    'app/src/main/res/xml/backup_rules.xml': FULL_BACKUP_OK,
    'capabilities.json': '{\n  "schemaVersion": 1,\n  "enabled": []\n}\n',
    '.gitignore': GITIGNORE_OK,
    [DATA_SAFETY_REL]: '| Data type | Collected |\n|---|---|\n| App activity | No |\n',
    ...overrides
  }
}

function withTempSlot(files, body) {
  const dir = mkdtempSync(path.join(SCRATCH_DIR, 'kiln-manifest-'))
  try {
    for (const [rel, contents] of Object.entries(files)) {
      if (contents === null) continue
      const file = path.join(dir, ...rel.split('/'))
      mkdirSync(path.dirname(file), { recursive: true })
      if (Buffer.isBuffer(contents)) writeFileSync(file, contents)
      else writeFileSync(file, contents, 'utf8')
    }
    return body(dir)
  } finally {
    rmSync(dir, { recursive: true, force: true })
  }
}

/** Runs one check against a synthetic slot tree and asserts its verdict and a phrase in its detail. */
function expectVerdict(check, files, verdict, phrase) {
  const result = withTempSlot(files, (dir) => check(dir))
  expectEqual(result.verdict, verdict, `verdict of ${result.id}`)
  if (phrase) expectTrue(String(result.detail).includes(phrase), `detail "${result.detail}" does not name ${phrase}`)
  return result
}

export function manifestFixtures() {
  return guard('manifest.fixtures', () =>
    runFixtures('manifest.fixtures', [
      // ---- slot.manifest: cleartext (the predicate inverted for the recorded red observation) ----
      [
        'android:usesCleartextTraffic="true" is a finding, and "false" is not',
        () => {
          expectVerdict(
            checkManifest,
            fixtureFiles({ [MANIFEST_REL]: baseManifest({ cleartext: 'true' }) }),
            'FAIL',
            'usesCleartextTraffic'
          )
          expectVerdict(
            checkManifest,
            fixtureFiles({ [MANIFEST_REL]: baseManifest({ cleartext: 'false' }) }),
            'PASS',
            'cleartext off'
          )
        }
      ],
      // ---- slot.manifest: gated permission disclosed in store/data-safety.md ----
      [
        'CAMERA without a data-safety mention fails, with one passes',
        () => {
          expectVerdict(
            checkManifest,
            fixtureFiles({ [MANIFEST_REL]: baseManifest({ permissions: ['android.permission.CAMERA'] }) }),
            'FAIL',
            'CAMERA'
          )
          expectVerdict(
            checkManifest,
            fixtureFiles({
              [MANIFEST_REL]: baseManifest({ permissions: ['android.permission.CAMERA'] }),
              [DATA_SAFETY_REL]: '| Data type | Collected |\n|---|---|\n| Camera | Yes — CAMERA, for the scan the user starts |\n'
            }),
            'PASS',
            'permission(s)'
          )
          expectVerdict(
            checkManifest,
            fixtureFiles({
              [MANIFEST_REL]: baseManifest({ permissions: ['android.permission.CAMERA'] }),
              [DATA_SAFETY_REL]: null
            }),
            'FAIL',
            'no store/data-safety.md exists'
          )
        }
      ],
      [
        'a wildcard permission is a finding',
        () => {
          expectVerdict(
            checkManifest,
            fixtureFiles({ [MANIFEST_REL]: baseManifest({ permissions: ['android.permission.*'] }) }),
            'FAIL',
            'blanket permission'
          )
        }
      ],
      // ---- slot.manifest: exported component without a stated requirement ----
      [
        'exported="true" with no intent-filter fails, with one passes',
        () => {
          expectVerdict(
            checkManifest,
            fixtureFiles({ [MANIFEST_REL]: baseManifest({ selfClosedActivity: true }) }),
            'FAIL',
            'no <intent-filter>'
          )
          expectVerdict(checkManifest, fixtureFiles(), 'PASS', 'no unjustified exported component')
        }
      ],
      // ---- slot.manifest: foreground-service type ----
      [
        'startForeground without a foregroundServiceType fails, with one passes',
        () => {
          const service = [
            '        <service android:name=".SyncService" android:foregroundServiceType="dataSync" />'
          ].join('\n')
          expectVerdict(
            checkManifest,
            fixtureFiles({ 'app/src/main/java/com/gexetr/demo/Sync.kt': 'startForeground(1, notification)\n' }),
            'FAIL',
            'foregroundServiceType'
          )
          expectVerdict(
            checkManifest,
            fixtureFiles({
              'app/src/main/java/com/gexetr/demo/Sync.kt': 'startForeground(1, notification)\n',
              [MANIFEST_REL]: baseManifest({ components: `${ACTIVITY_WITH_FILTER}\n${service}` })
            }),
            'PASS',
            'no unjustified exported component'
          )
        }
      ],
      // ---- slot.manifest: restricted permissions, and predictive back ----
      [
        'SCHEDULE_EXACT_ALARM fails; removing it passes',
        () => {
          expectVerdict(
            checkManifest,
            fixtureFiles({ [MANIFEST_REL]: baseManifest({ permissions: ['android.permission.SCHEDULE_EXACT_ALARM'] }) }),
            'FAIL',
            'SCHEDULE_EXACT_ALARM'
          )
          expectVerdict(checkManifest, fixtureFiles(), 'PASS', 'restricted') // detail names neither restricted permission
        }
      ],
      [
        'enableOnBackInvokedCallback="false" fails; "true" passes',
        () => {
          expectVerdict(
            checkManifest,
            fixtureFiles({ [MANIFEST_REL]: baseManifest({ backCallback: 'false' }) }),
            'FAIL',
            'enableOnBackInvokedCallback'
          )
          expectVerdict(checkManifest, fixtureFiles(), 'PASS', 'enableOnBackInvokedCallback true')
        }
      ],
      [
        'a missing AndroidManifest.xml is UNVERIFIED, never PASS',
        () => {
          expectVerdict(checkManifest, fixtureFiles({ [MANIFEST_REL]: null }), 'UNVERIFIED', 'AndroidManifest.xml')
        }
      ],
      [
        'a manifest that does not parse is UNVERIFIED naming the path, never PASS and never a FAIL about unread rules',
        () => {
          const truncated = baseManifest().replace('</manifest>', '')
          expectVerdict(checkManifest, fixtureFiles({ [MANIFEST_REL]: truncated }), 'UNVERIFIED', 'AndroidManifest.xml')
          expectVerdict(checkManifest, fixtureFiles({ [MANIFEST_REL]: truncated }), 'UNVERIFIED', 'truncated')
          expectVerdict(checkBackup, fixtureFiles({ [MANIFEST_REL]: truncated }), 'UNVERIFIED', 'AndroidManifest.xml')
        }
      ],
      // ---- slot.backup ----
      [
        'no android:dataExtractionRules fails; both sections pass',
        () => {
          expectVerdict(
            checkBackup,
            fixtureFiles({ [MANIFEST_REL]: baseManifest({ dataExtractionRules: null }) }),
            'FAIL',
            'dataExtractionRules'
          )
          expectVerdict(checkBackup, fixtureFiles(), 'PASS', 'cloud-backup')
        }
      ],
      [
        'a rules file with no <device-transfer> section fails',
        () => {
          expectVerdict(
            checkBackup,
            fixtureFiles({
              'app/src/main/res/xml/data_extraction_rules.xml': DATA_EXTRACTION_OK.replace(
                /    <device-transfer>[\s\S]*?<\/device-transfer>\n/,
                ''
              )
            }),
            'FAIL',
            'device-transfer'
          )
          expectVerdict(checkBackup, fixtureFiles(), 'PASS', 'device-transfer')
        }
      ],
      [
        'a rules reference with no file behind it fails, and names the path',
        () => {
          expectVerdict(
            checkBackup,
            fixtureFiles({ 'app/src/main/res/xml/data_extraction_rules.xml': null }),
            'FAIL',
            'data_extraction_rules.xml'
          )
        }
      ],
      [
        'minSdk 26 without the legacy fullBackupContent fails; above 30 it is not required',
        () => {
          expectVerdict(
            checkBackup,
            fixtureFiles({ [MANIFEST_REL]: baseManifest({ fullBackupContent: null }) }),
            'FAIL',
            'fullBackupContent'
          )
          expectVerdict(
            checkBackup,
            fixtureFiles({
              [MANIFEST_REL]: baseManifest({ fullBackupContent: null }),
              'app/build.gradle.kts': 'android {\n    defaultConfig {\n        minSdk = 31\n    }\n}\n'
            }),
            'PASS',
            'allowBackup'
          )
        }
      ],
      [
        'a whole-domain include that excludes no secret path fails, and one that does passes',
        () => {
          const bare = DATA_EXTRACTION_OK.replace('        <exclude domain="sharedpref" path="secrets.xml" />\n', '').replace(
            '        <exclude domain="file" path="tokens/" />\n',
            ''
          )
          expectVerdict(
            checkBackup,
            fixtureFiles({ 'app/src/main/res/xml/data_extraction_rules.xml': bare }),
            'FAIL',
            'excludes no secret-bearing path'
          )
          expectVerdict(checkBackup, fixtureFiles(), 'PASS', 'excludes secret material')
        }
      ],
      // ---- slot.capabilities ----
      [
        "an enabled capability with no SDK in the build fails; declaring it passes",
        () => {
          const enabled = '{\n  "schemaVersion": 1,\n  "enabled": ["ads"]\n}\n'
          expectVerdict(
            checkCapabilities,
            fixtureFiles({ 'capabilities.json': enabled }),
            'FAIL',
            "enables 'ads'"
          )
          expectVerdict(
            checkCapabilities,
            fixtureFiles({
              'capabilities.json': enabled,
              'app/build.gradle.kts': 'android {\n    defaultConfig {\n        minSdk = 26\n    }\n}\n\ndependencies {\n    implementation(libs.capability.ads)\n}\n'
            }),
            'PASS',
            'enables ads'
          )
        }
      ],
      [
        'a capability SDK the plan does not name fails, and the baseline capability does not',
        () => {
          expectVerdict(
            checkCapabilities,
            fixtureFiles({
              'app/build.gradle.kts': 'dependencies {\n    implementation("com.gexetr.capability:ads:1.0.0")\n}\n'
            }),
            'FAIL',
            "does not enable it"
          )
          // `core` is every slot's baseline capability and is never listed in capabilities.json, so
          // requiring it there would fail every slot on the registry's own baseline module.
          expectVerdict(
            checkCapabilities,
            fixtureFiles({
              'app/build.gradle.kts': 'dependencies {\n    implementation(libs.capability.core)\n}\n'
            }),
            'PASS',
            'enables none'
          )
        }
      ],
      [
        'a capability-owned permission without its capability fails; enabling it passes',
        () => {
          const adId = baseManifest({ permissions: ['com.google.android.gms.permission.AD_ID'] })
          expectVerdict(checkCapabilities, fixtureFiles({ [MANIFEST_REL]: adId }), 'FAIL', 'AD_ID')
          expectVerdict(
            checkCapabilities,
            fixtureFiles({
              [MANIFEST_REL]: adId,
              'capabilities.json': '{\n  "schemaVersion": 1,\n  "enabled": ["ads"]\n}\n',
              'app/build.gradle.kts': 'dependencies {\n    implementation(libs.capability.ads)\n}\n'
            }),
            'PASS',
            'enables ads'
          )
        }
      ],
      [
        'no capabilities.json is UNVERIFIED, never PASS',
        () => {
          expectVerdict(checkCapabilities, fixtureFiles({ 'capabilities.json': null }), 'UNVERIFIED', 'capabilities.json')
        }
      ],
      // ---- slot.secretPaths ----
      [
        'a .env git would not ignore fails; adding the rule passes',
        () => {
          const withoutEnv = GITIGNORE_OK.replace('.env\n', '').replace('.env.*\n', '')
          expectVerdict(
            checkSecretPaths,
            fixtureFiles({ '.env': 'PLAYKILN_KEY=1\n', '.gitignore': withoutEnv }),
            'FAIL',
            '.env'
          )
          expectVerdict(checkSecretPaths, fixtureFiles({ '.env': 'PLAYKILN_KEY=1\n' }), 'PASS', '.env')
        }
      ],
      [
        'keystore.properties is ignored, and .env.example is not a secret at all',
        () => {
          expectVerdict(checkSecretPaths, fixtureFiles({ 'keystore.properties': 'storePassword=…\n' }), 'PASS', 'keystore.properties')
          expectVerdict(checkSecretPaths, fixtureFiles({ '.env.example': 'PLAYKILN_KEY=\n', '.gitignore': null }), 'PASS', 'no in-slot secret file')
        }
      ],
      // ---- slot.tokens ----
      [
        'a surviving __TOKEN__ fails and is repairable, and a binary is not read as text',
        () => {
          const file = 'app/src/main/res/values/strings.xml'
          expectVerdict(
            checkTokens,
            fixtureFiles({ [file]: '<resources><string name="app_name">__APP_NAME__</string></resources>\n' }),
            'FAIL',
            '__APP_NAME__'
          )
          expectVerdict(
            checkTokens,
            fixtureFiles({ [file]: '<resources><string name="app_name">Demo</string></resources>\n' }),
            'PASS',
            'no unresolved __TOKEN__'
          )
          // The same token behind a NUL byte is a binary, and a text sweep must not speak about it.
          const binary = Buffer.concat([Buffer.from([0x00, 0x01]), Buffer.from('__APP_NAME__', 'utf8')])
          expectVerdict(checkTokens, fixtureFiles({ 'app/src/main/assets/blob.bin': binary }), 'PASS', 'no unresolved __TOKEN__')
        }
      ]
    ])
  )
}
