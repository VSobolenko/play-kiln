// Dependency freshness — two checks over one contract.
//
// Measured incident (2026-10-05, repeated 2026-10-08). Google Play flagged Mark Book for Teachers,
// then Laneset, for shipping `androidx.fragment:fragment:1.1.0`. It reaches the APK only as a
// transitive of `play-services-basement` via `com.android.billingclient:billing`. Billing is already
// at the latest stable; Compose apps do not use Fragment UI. The fix is a dependency *constraint*
// that forces the transitive to the factory floor, never an app feature dependency and never a
// Billing downgrade. Rule: `.cursor/rules/dependency-freshness.mdc`.
//
// The second notice is why there are two checks and not one. `build.fragmentConstraint` reads
// `app/build.gradle.kts` and asks "is the constraint declared?" — a proxy, and a weak one: laneset's
// constraint was added to the source *after* its bundle had already shipped, so the proxy was true
// while the artifact was wrong, and Play's notice arrived anyway. `deps.artifact` reads what the
// bundle says about itself (`BUNDLE-METADATA/.../dependencies.pb`, the same file the console reads)
// and asks the question that actually matters. A source constraint is not evidence about an artifact.
import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { fail, guard, pass, ROOT, runFixtures, skip, unverified, expectEqual, expectTrue } from './lib.mjs'
import { baselineVerdict } from './baseline.mjs'
import { DEPENDENCY_METADATA_ENTRY, decodeDependencyList, aabDependencyCoordinates } from '../lib/aab-metadata.mjs'
import {
  compareVersions,
  floorFor,
  outdatedCoordinates,
  parseFloors,
  FLOORS_PATH
} from '../lib/dependency-floors.mjs'

const FRAGMENT_COORDINATE = 'androidx.fragment:fragment'

/**
 * Factory floor for the Fragment constraint, read from the one contract that declares it.
 * Bump it in `scripts/lib/play-dependency-floors.json` when Play's latest stable moves.
 */
export const FRAGMENT_FLOOR = floorFor(FRAGMENT_COORDINATE)

const BILLING_MARKERS = [
  /libs\.capability\.billing\b/,
  /com\.gexetr\.capability:billing\b/,
  /libs\.billing\.ktx\b/,
  /com\.android\.billingclient:billing/
]

function readText(file) {
  try { return readFileSync(file, 'utf8') } catch { return null }
}

/** True when the build script depends on Play Billing (capability or raw SDK). */
export function buildDeclaresBilling(gradleText) {
  if (!gradleText) return false
  return BILLING_MARKERS.some((re) => re.test(gradleText))
}

/** True when capabilities.json enables the billing capability. */
export function capabilitiesEnableBilling(capabilitiesText) {
  if (!capabilitiesText) return false
  try {
    const json = JSON.parse(capabilitiesText)
    return Array.isArray(json.enabled) && json.enabled.includes('billing')
  } catch {
    return false
  }
}

/**
 * Reads `fragment = "x.y.z"` from a version catalog. Returns null when absent or unparseable.
 */
export function catalogFragmentVersion(catalogText) {
  if (!catalogText) return null
  const match = catalogText.match(/^\s*fragment\s*=\s*"([^"]+)"/m)
  return match ? match[1] : null
}

/**
 * Extracts the Fragment version a constraints block pins, or null when none is present.
 *
 * Accepted shapes (constraint only — an `implementation(libs.androidx.fragment)` outside
 * `constraints { }` is deliberately NOT a pass; Compose apps must not take Fragment as a feature):
 *   - `api("androidx.fragment:fragment:1.9.1") { because(...) }`
 *   - `api("androidx.fragment:fragment") { version { strictly("1.9.1") } }`
 *   - `api(libs.androidx.fragment) { because(...) }` with `fragment = "1.9.1"` in the catalog
 */
export function constrainedFragmentVersion(gradleText, catalogText) {
  if (!gradleText) return null
  const constraintsBlocks = [...gradleText.matchAll(/\bconstraints\s*\{([\s\S]*?)\n\s*\}/g)]
    .map((m) => m[1])
  if (constraintsBlocks.length === 0) return null

  for (const block of constraintsBlocks) {
    if (!block.includes('fragment')) continue
    const literal = block.match(/androidx\.fragment:fragment:([0-9][0-9A-Za-z.+_-]*)/)
    if (literal) return literal[1]
    const strictly = block.match(/androidx\.fragment:fragment["']?\s*\)[\s\S]*?strictly\s*\(\s*["']([^"']+)["']/)
    if (strictly) return strictly[1]
    if (/\blibs\.androidx\.fragment\b/.test(block) || /androidx\.fragment:fragment/.test(block)) {
      const fromCatalog = catalogFragmentVersion(catalogText)
      if (fromCatalog) return fromCatalog
    }
  }
  return null
}

/**
 * Problems for one slot's Fragment constraint. Empty array = clean (or billing not present).
 */
export function fragmentConstraintProblems({ gradleText, catalogText, capabilitiesText, rel = 'app/build.gradle.kts' }) {
  const needs = capabilitiesEnableBilling(capabilitiesText) || buildDeclaresBilling(gradleText)
  if (!needs) return []
  if (!gradleText) return [`${rel}: billing is enabled but the app build script is missing`]

  const pinned = constrainedFragmentVersion(gradleText, catalogText)
  if (pinned == null) {
    return [
      `${rel}: billing is on the classpath but androidx.fragment:fragment is not constrained to ` +
        `≥${FRAGMENT_FLOOR} (Play flagged 1.1.0 via billing→play-services-basement; Compose apps ` +
        `constrain the transitive only — do not add Fragment as UI)`
    ]
  }
  if (compareVersions(pinned, FRAGMENT_FLOOR) < 0) {
    return [
      `${rel}: Fragment constraint pins ${pinned}, below the factory floor ${FRAGMENT_FLOOR}`
    ]
  }
  return []
}

function slotFragmentInputs(slotDir) {
  const gradlePath = path.join(slotDir, 'app', 'build.gradle.kts')
  const catalogPath = path.join(slotDir, 'gradle', 'libs.versions.toml')
  const capsPath = path.join(slotDir, 'capabilities.json')
  return {
    gradleText: readText(gradlePath),
    catalogText: readText(catalogPath),
    capabilitiesText: readText(capsPath),
    hasGradle: existsSync(gradlePath)
  }
}

/** Per-slot gate (source): a billing slot must constrain Fragment to the factory floor. */
export function checkFragmentConstraint(slotDir) {
  const rel = path.relative(ROOT, slotDir).split(path.sep).join('/')
  return guard('build.fragmentConstraint', () => {
    const inputs = slotFragmentInputs(slotDir)
    if (!inputs.hasGradle) {
      return unverified('build.fragmentConstraint', `${rel}: no app/build.gradle.kts to inspect`)
    }
    const problems = fragmentConstraintProblems({ ...inputs, rel: `${rel}/app/build.gradle.kts` })
    if (problems.length === 0) {
      const needs = capabilitiesEnableBilling(inputs.capabilitiesText) || buildDeclaresBilling(inputs.gradleText)
      return needs
        ? pass('build.fragmentConstraint', `Fragment constrained ≥${FRAGMENT_FLOOR} (billing present)`)
        : skip('build.fragmentConstraint', 'billing not on the classpath')
    }
    return fail('build.fragmentConstraint', problems.join('; '))
  })
}

/**
 * The release bundle a slot has actually staged: the `-release.aab` named by
 * `dist/release-manifest.json`, or null when the slot has never been released.
 *
 * Deliberately *not* every `.aab` under `dist/`: that directory accumulates one bundle per version,
 * so a slot that fixed its dependency and shipped again still holds the old, outdated bundle on
 * disk, and failing on it would be a permanent false positive on a republished app. The manifest
 * names the artifact the factory would upload now, which is the one Play will read.
 */
export function stagedReleaseBundle(slotDir) {
  const manifestPath = path.join(slotDir, 'dist', 'release-manifest.json')
  if (!existsSync(manifestPath)) return { path: null, manifestPath, hasManifest: false }
  let manifest
  try {
    manifest = JSON.parse(readFileSync(manifestPath, 'utf8'))
  } catch {
    return { path: null, manifestPath, hasManifest: true, unreadable: true }
  }
  const artifacts = Array.isArray(manifest?.artifacts) ? manifest.artifacts : []
  const aab = artifacts.find((a) => typeof a?.path === 'string' && a.path.endsWith('-release.aab'))
  if (!aab) return { path: null, manifestPath, hasManifest: true }
  const relative = aab.path.split('/').join(path.sep)
  return { path: path.join(slotDir, relative), relative: aab.path, manifestPath, hasManifest: true }
}

/**
 * Problems for one slot's staged release bundle. Empty array = clean, or nothing staged to judge.
 * `unverified` names the bundles that could not be asked, so an unreadable artifact is reported
 * rather than silently counted as clean.
 */
export function artifactDependencyProblems({ bundlePath, bundleText = null, floors }) {
  let bytes = null
  if (bundleText !== null) {
    bytes = Buffer.isBuffer(bundleText) ? bundleText : Buffer.from(bundleText, 'binary')
  } else {
    try {
      bytes = readFileSync(bundlePath)
    } catch {
      return { problems: [], unverified: `could not read ${bundlePath}` }
    }
  }
  const coordinates = aabDependencyCoordinates(bytes)
  if (coordinates === null) {
    return {
      problems: [],
      unverified: `${bundlePath}: carries no readable ${DEPENDENCY_METADATA_ENTRY}`
    }
  }
  const hits = outdatedCoordinates(coordinates, floors)
  return {
    problems: hits.map(
      (hit) =>
        `ships ${hit.coordinate}:${hit.version}, below the Play-flagged floor ${hit.floor} ` +
        `— rebuild with the constraint raised (Play notice: ${hit.why.slice(0, 80)}…)`
    ),
    unverified: null,
    coordinateCount: coordinates.length
  }
}

/**
 * Per-slot gate (artifact): the staged release bundle must not declare a Play-outdated coordinate.
 *
 * This is the check that would have caught the 2026-10-08 notice. Applicability is the freshness
 * rule's own date through `baselineVerdict`, so the slots that shipped before the rule carry their
 * finding as a printed note while every slot created from that date is gated.
 */
export function checkArtifactDependencies(slotDir) {
  return guard('deps.artifact', () => {
    const staged = stagedReleaseBundle(slotDir)
    if (!staged.hasManifest) {
      return skip('deps.artifact', 'no dist/release-manifest.json — no release bundle has been staged')
    }
    if (staged.unreadable) {
      return unverified('deps.artifact', `${path.relative(ROOT, staged.manifestPath)}: not valid JSON`)
    }
    if (!staged.path) {
      return unverified('deps.artifact', `${path.relative(ROOT, staged.manifestPath)}: names no -release.aab`)
    }
    if (!existsSync(staged.path)) {
      return unverified('deps.artifact', `${staged.relative}: named by the manifest but missing on disk`)
    }
    const { problems, unverified: blind } = artifactDependencyProblems({ bundlePath: staged.path })
    if (blind) return unverified('deps.artifact', blind)
    return baselineVerdict(
      'deps.artifact',
      slotDir,
      problems,
      `${staged.relative} declares no Play-outdated coordinate`
    )
  })
}

export function depsChecks(slotDir) {
  return [checkFragmentConstraint(slotDir), checkArtifactDependencies(slotDir)]
}

// ---------------------------------------------------------------------------------------------
// Fixtures. The artifact legs need a zip and a protobuf to test against, so the encoders live
// here beside the legs that use them: a fixture that cannot build the artifact it claims to judge
// proves nothing about the decoder.
// ---------------------------------------------------------------------------------------------

let CRC_TABLE = null
function crc32(buf) {
  if (!CRC_TABLE) {
    CRC_TABLE = new Int32Array(256)
    for (let n = 0; n < 256; n++) {
      let c = n
      for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1
      CRC_TABLE[n] = c
    }
  }
  let crc = -1
  for (const byte of buf) crc = CRC_TABLE[(crc ^ byte) & 0xff] ^ (crc >>> 8)
  return (crc ^ -1) >>> 0
}

function varint(n) {
  const out = []
  let v = n
  while (v >= 0x80) {
    out.push((v & 0x7f) | 0x80)
    v = Math.floor(v / 128)
  }
  out.push(v)
  return Buffer.from(out)
}

function lenDelim(field, payload) {
  return Buffer.concat([varint((field << 3) | 2), varint(payload.length), payload])
}

function pbString(field, text) {
  return lenDelim(field, Buffer.from(text, 'utf8'))
}

/** Encode the AAB dependency protobuf: AppDependencies{Library{MavenLibrary{group,name,version}}}. */
export function encodeDependencyList(coordinates) {
  const libraries = coordinates.map((c) =>
    lenDelim(1, lenDelim(1, Buffer.concat([pbString(1, c.group), pbString(2, c.name), pbString(5, c.version)])))
  )
  return Buffer.concat(libraries)
}

/** A minimal, uncompressed zip archive — enough for the central-directory reader to be exercised. */
export function encodeZip(entries) {
  const localParts = []
  const centralParts = []
  let offset = 0
  for (const entry of entries) {
    const name = Buffer.from(entry.name, 'utf8')
    const data = Buffer.isBuffer(entry.data) ? entry.data : Buffer.from(entry.data, 'binary')
    const crc = crc32(data)

    const local = Buffer.alloc(30)
    local.writeUInt32LE(0x04034b50, 0)
    local.writeUInt16LE(20, 4)
    local.writeUInt16LE(0, 8)
    local.writeUInt32LE(crc, 14)
    local.writeUInt32LE(data.length, 18)
    local.writeUInt32LE(data.length, 22)
    local.writeUInt16LE(name.length, 26)
    localParts.push(local, name, data)

    const cd = Buffer.alloc(46)
    cd.writeUInt32LE(0x02014b50, 0)
    cd.writeUInt16LE(20, 4)
    cd.writeUInt16LE(20, 6)
    cd.writeUInt32LE(crc, 16)
    cd.writeUInt32LE(data.length, 20)
    cd.writeUInt32LE(data.length, 24)
    cd.writeUInt16LE(name.length, 28)
    cd.writeUInt32LE(offset, 42)
    centralParts.push(cd, name)

    offset += local.length + name.length + data.length
  }
  const central = Buffer.concat(centralParts)
  const eocd = Buffer.alloc(22)
  eocd.writeUInt32LE(0x06054b50, 0)
  eocd.writeUInt16LE(entries.length, 8)
  eocd.writeUInt16LE(entries.length, 10)
  eocd.writeUInt32LE(central.length, 12)
  eocd.writeUInt32LE(offset, 16)
  return Buffer.concat([...localParts, central, eocd])
}

/** A bundle whose dependency metadata declares exactly these coordinates. */
function fakeBundle(coordinates) {
  return encodeZip([{ name: DEPENDENCY_METADATA_ENTRY, data: encodeDependencyList(coordinates) }])
}

const FLOOR_FIXTURE = [
  {
    coordinate: FRAGMENT_COORDINATE,
    floor: FRAGMENT_FLOOR,
    why: 'fixture: Play flagged 1.1.0 via billing -> play-services-basement -> fragment'
  }
]

/**
 * Inline fixtures: each leg drives a known-verdict synthetic input so a sensor that can no longer
 * fail is visible in the same report as the slots it judges.
 */
export function depsFixtures() {
  return guard('deps.fixtures', () => {
    const goodConstraint = `
dependencies {
  implementation(libs.capability.billing)
  // Compose apps do not use Fragment; this only upgrades the transitive so Play stops flagging 1.1.0
  constraints {
    api("androidx.fragment:fragment:${FRAGMENT_FLOOR}") {
      because("Play notice: billing→GMS basement pulled fragment 1.1.0")
    }
  }
}`
    const catalogPin = `fragment = "${FRAGMENT_FLOOR}"`
    const cases = [
      [
        'a billing slot with no Fragment constraint fails',
        () => {
          const problems = fragmentConstraintProblems({
            gradleText: 'dependencies { implementation(libs.capability.billing) }',
            capabilitiesText: '{"schemaVersion":1,"enabled":["billing"]}',
            catalogText: ''
          })
          expectTrue(problems.length > 0, 'missing constraint was accepted')
          expectTrue(problems[0].includes('not constrained'), problems[0])
        }
      ],
      [
        'a billing slot that only implementation()s Fragment (no constraints block) fails',
        () => {
          const problems = fragmentConstraintProblems({
            gradleText: 'dependencies {\n  implementation(libs.capability.billing)\n  implementation(libs.androidx.fragment)\n}',
            capabilitiesText: '{"schemaVersion":1,"enabled":["billing"]}',
            catalogText: catalogPin
          })
          expectTrue(problems.length > 0, 'implementation-as-UI was accepted as a constraint')
        }
      ],
      [
        'a constraint older than the floor fails',
        () => {
          const problems = fragmentConstraintProblems({
            gradleText: `
dependencies {
  implementation(libs.capability.billing)
  constraints {
    api("androidx.fragment:fragment:1.1.0") { because("too old") }
  }
}`,
            capabilitiesText: '{"schemaVersion":1,"enabled":["billing"]}',
            catalogText: ''
          })
          expectTrue(problems.some((p) => p.includes('below the factory floor')), JSON.stringify(problems))
        }
      ],
      [
        'a constraint at the floor passes',
        () => {
          expectEqual(
            fragmentConstraintProblems({
              gradleText: goodConstraint,
              capabilitiesText: '{"schemaVersion":1,"enabled":["billing"]}',
              catalogText: ''
            }),
            [],
            'floor pin'
          )
        }
      ],
      [
        'a catalog-backed constraints { api(libs.androidx.fragment) } at the floor passes',
        () => {
          const gradle = `
dependencies {
  implementation(libs.capability.billing)
  constraints {
    api(libs.androidx.fragment) { because("Play notice via Billing→GMS") }
  }
}`
          expectEqual(
            fragmentConstraintProblems({
              gradleText: gradle,
              capabilitiesText: '{"schemaVersion":1,"enabled":[]}',
              catalogText: `[versions]\n${catalogPin}\n`
            }),
            [],
            'catalog constraint'
          )
        }
      ],
      [
        'a catalog pin below the floor fails even when the constraints block names the alias',
        () => {
          const gradle = `
dependencies {
  implementation(libs.capability.billing)
  constraints { api(libs.androidx.fragment) { because("x") } }
}`
          const problems = fragmentConstraintProblems({
            gradleText: gradle,
            capabilitiesText: null,
            catalogText: 'fragment = "1.8.0"'
          })
          expectTrue(problems.some((p) => p.includes('1.8.0') && p.includes(FRAGMENT_FLOOR)), JSON.stringify(problems))
        }
      ],
      [
        'a slot with no billing is silent',
        () => {
          expectEqual(
            fragmentConstraintProblems({
              gradleText: 'dependencies { implementation(libs.androidx.compose.ui) }',
              capabilitiesText: '{"schemaVersion":1,"enabled":[]}',
              catalogText: ''
            }),
            [],
            'no billing'
          )
        }
      ],
      [
        'compareVersions orders the floor above the Play-flagged antique',
        () => {
          expectTrue(compareVersions('1.1.0', FRAGMENT_FLOOR) < 0, '1.1.0 < floor')
          expectTrue(compareVersions(FRAGMENT_FLOOR, FRAGMENT_FLOOR) === 0, 'floor == floor')
          expectTrue(compareVersions('1.10.0', FRAGMENT_FLOOR) > 0, '1.10.0 > floor')
        }
      ],
      // ---- artifact legs: the half that reads the bundle rather than the build script ----
      [
        'a bundle declaring the Play-flagged fragment 1.1.0 fails',
        () => {
          const bundle = fakeBundle([{ group: 'androidx.fragment', name: 'fragment', version: '1.1.0' }])
          const { problems } = artifactDependencyProblems({ bundleText: bundle, floors: FLOOR_FIXTURE })
          expectTrue(problems.length > 0, 'an outdated shipped coordinate was accepted')
          expectTrue(problems[0].includes('androidx.fragment:fragment:1.1.0'), problems[0])
          expectTrue(problems[0].includes(FRAGMENT_FLOOR), problems[0])
        }
      ],
      [
        'a bundle at the floor passes',
        () => {
          const bundle = fakeBundle([
            { group: 'androidx.fragment', name: 'fragment', version: FRAGMENT_FLOOR },
            { group: 'androidx.compose', name: 'compose-bom', version: '2026.09.00' }
          ])
          const { problems, coordinateCount } = artifactDependencyProblems({ bundleText: bundle, floors: FLOOR_FIXTURE })
          expectEqual(problems, [], 'clean bundle')
          expectEqual(coordinateCount, 2, 'both coordinates read from the artifact')
        }
      ],
      [
        'a bundle above the floor passes even when unrelated coordinates are old',
        () => {
          const bundle = fakeBundle([
            { group: 'androidx.fragment', name: 'fragment', version: '1.12.0' },
            { group: 'com.example', name: 'ancient-thing', version: '0.0.1' }
          ])
          const { problems } = artifactDependencyProblems({ bundleText: bundle, floors: FLOOR_FIXTURE })
          expectEqual(problems, [], 'an unlisted coordinate must not be judged')
        }
      ],
      [
        'a bundle with no dependency metadata is UNVERIFIED, never a pass',
        () => {
          const bundle = encodeZip([{ name: 'base/manifest/AndroidManifest.xml', data: '<manifest/>' }])
          const { problems, unverified: blind } = artifactDependencyProblems({ bundleText: bundle, floors: FLOOR_FIXTURE })
          expectEqual(problems, [], 'nothing to report')
          expectTrue(typeof blind === 'string' && blind.includes('no readable'), String(blind))
        }
      ],
      [
        'a protobuf whose library field is truncated is rejected, not guessed at',
        () => {
          const good = encodeDependencyList([{ group: 'androidx.fragment', name: 'fragment', version: '1.1.0' }])
          const torn = good.subarray(0, good.length - 3)
          expectEqual(decodeDependencyList(torn), [], 'a truncated message yielded coordinates')
        }
      ],
      [
        'the decoder reads group/name/version, not whatever strings it finds first',
        () => {
          const coords = decodeDependencyList(
            encodeDependencyList([
              { group: 'com.google.android.gms', name: 'play-services-basement', version: '18.5.0' },
              { group: 'androidx.fragment', name: 'fragment', version: '1.1.0' }
            ])
          )
          expectEqual(coords.length, 2, 'both libraries decoded')
          expectEqual(coords[1].group, 'androidx.fragment', 'second group')
          expectEqual(coords[1].name, 'fragment', 'second name')
          expectEqual(coords[1].version, '1.1.0', 'second version')
        }
      ],
      [
        'the floors contract rejects a floor with no recorded reason',
        () => {
          const { problems } = parseFloors(
            JSON.stringify({ floors: [{ coordinate: 'a:b', floor: '1.0.0' }] }),
            'fixture'
          )
          expectTrue(problems.some((p) => p.includes('why')), JSON.stringify(problems))
        }
      ],
      [
        'the floors contract rejects a coordinate that is not group:name',
        () => {
          const { problems } = parseFloors(
            JSON.stringify({ floors: [{ coordinate: 'fragment', floor: '1.9.1', why: 'x'.repeat(20) }] }),
            'fixture'
          )
          expectTrue(problems.some((p) => p.includes('group:name')), JSON.stringify(problems))
        }
      ],
      [
        'the release builder is wired to refuse a Play-outdated bundle',
        () => {
          // Wiring, not behaviour: this asserts the one place a bundle is produced actually asks the
          // question. Without it, `deps.artifact` can only report a bundle that was already staged,
          // and the 2026-10-08 notice is reproducible by deleting four lines from the builder.
          const rel = 'scripts/prepare-release.mjs'
          const source = readText(path.join(ROOT, rel))
          expectTrue(source !== null, `${rel} is missing`)
          expectTrue(source.includes('aabDependencyCoordinates'), `${rel} never reads the bundle dependency inventory`)
          const call = source.indexOf('outdatedCoordinates(')
          expectTrue(call !== -1, `${rel} never compares the inventory against the declared floors`)
          expectTrue(
            /throw new ReleaseError/.test(source.slice(call, call + 900)),
            `${rel} reports a below-floor coordinate but does not refuse the release`
          )
        }
      ]
    ]
    return runFixtures('deps.fixtures', cases)
  })
}
