// Dependency freshness — Fragment constraint for Billing slots.
//
// Measured incident (2026-10-05): Google Play flagged Mark Book for Teachers for shipping
// `androidx.fragment:fragment:1.1.0`, which reached the APK only as a transitive of
// `play-services-basement` via `com.android.billingclient:billing:9.1.0`. Billing is already at
// the latest stable; Compose apps do not use Fragment UI. The fix is a dependency *constraint*
// that forces the transitive to the factory floor (≥ 1.9.1), never an app feature dependency and
// never a Billing downgrade. Rule: `.cursor/rules/dependency-freshness.mdc`.
import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { fail, guard, pass, ROOT, runFixtures, skip, unverified, expectEqual, expectTrue } from './lib.mjs'

/** Factory floor for the Fragment constraint. Bump with the rule when Play's latest stable moves. */
export const FRAGMENT_FLOOR = '1.9.1'

const FRAGMENT_COORD = 'androidx.fragment:fragment'
const BILLING_MARKERS = [
  /libs\.capability\.billing\b/,
  /com\.gexetr\.capability:billing\b/,
  /libs\.billing\.ktx\b/,
  /com\.android\.billingclient:billing/
]

function readText(file) {
  try { return readFileSync(file, 'utf8') } catch { return null }
}

/** Compare dotted numeric versions; returns negative when a < b. Non-numeric tails sort lower. */
export function compareVersions(a, b) {
  const parts = (v) => String(v).split(/[.+-]/).map((p) => (/^\d+$/.test(p) ? Number(p) : p))
  const aa = parts(a)
  const bb = parts(b)
  const n = Math.max(aa.length, bb.length)
  for (let i = 0; i < n; i++) {
    const x = aa[i] ?? 0
    const y = bb[i] ?? 0
    if (typeof x === 'number' && typeof y === 'number') {
      if (x !== y) return x - y
    } else {
      const sx = String(x)
      const sy = String(y)
      if (sx !== sy) return sx < sy ? -1 : 1
    }
  }
  return 0
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

/** Per-slot gate: a billing slot must constrain Fragment to the factory floor. */
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

export function depsChecks(slotDir) {
  return [checkFragmentConstraint(slotDir)]
}

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
      ]
    ]
    return runFixtures('deps.fixtures', cases)
  })
}
