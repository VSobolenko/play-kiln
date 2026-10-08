// Soft In-App Review: every product slot offers moderate Play ReviewManager prompts.
//
// Contract: `.cursor/rules/in-app-review.mdc`. Baseline date 2026-10-05 so older slots are notes.
//
// Legs:
//   review.dependency  — review-ktx declared in the version catalog or app build file
//   review.api         — ReviewManagerFactory.create + requestReviewFlow + launchReviewFlow
//   review.throttle    — DataStore persistence of last-prompted / completed state
//   review.settings    — a Rate affordance that calls the Review API before any market:// fallback

import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { SCRATCH_DIR } from '../lib/scratch.mjs'
import { baselineVerdict, FACTORY_BASELINE } from './baseline.mjs'
import { decodeSource } from './build.mjs'
import { expectTrue, ROOT, runFixtures, pass, fail, unverified, resolveSlotDir, guard } from './lib.mjs'
import { walkSlotFiles } from './manifest.mjs'
import { stripKotlinComments } from './localization.mjs'

/** The ids this module reports. The registry in `scripts/core/index.mjs` holds the same list. */
export const REVIEW_IDS = Object.freeze([
  'review.dependency',
  'review.api',
  'review.throttle',
  'review.settings'
])

export const REVIEW_RULE_REL = '.cursor/rules/in-app-review.mdc'
export const SCAFFOLD_DIR = path.join(ROOT, 'scaffold', 'android-compose')

const BUILD_RELS = Object.freeze(['app/build.gradle.kts', 'gradle/libs.versions.toml', 'build.gradle.kts'])
const KOTLIN_FILE = /\.kt$/i

const REVIEW_DEP = /com\.google\.android\.play:review(?:-ktx)?|libs\.play\.review|androidx-play-review|play-review-ktx|review-ktx/i
const REVIEW_MANAGER_FACTORY = /\bReviewManagerFactory\s*\.\s*create\b/
const REQUEST_REVIEW = /\brequestReviewFlow\b/
const LAUNCH_REVIEW = /\blaunchReviewFlow\b/
const DATASTORE = /\bpreferencesDataStore\b|\bDataStore\b/
const THROTTLE_KEYS =
  /lastPrompt|last_prompt|reviewPrompt|review_prompt|reviewCompleted|review_completed|promptedAt|prompted_at|lastReview/i
const RATE_AFFORDANCE = /\b[Rr]ate\b|settings_rate|review_app|requestReview|launchInAppReview|InAppReview/
const MARKET_FALLBACK = /market:\/\/details|play\.google\.com\/store\/apps\/details/

const relOf = (dir, file) => path.relative(dir, file).split(path.sep).join('/')
const lineAt = (text, index) => text.slice(0, index).split('\n').length

export function resolveReviewDir(input) {
  if (typeof input !== 'string' || input.trim() === '') {
    throw new Error('a slot directory or slug is required')
  }
  const candidate = path.isAbsolute(input) ? input : path.join(ROOT, input)
  if (existsSync(candidate)) return candidate
  return resolveSlotDir(input)
}

function kotlinSources(dir) {
  const sources = []
  const root = path.join(dir, 'app', 'src', 'main')
  if (!existsSync(root)) return sources
  for (const file of walkSlotFiles(root)) {
    if (!KOTLIN_FILE.test(file)) continue
    let text = null
    try {
      text = decodeSource(readFileSync(file))
    } catch {
      continue
    }
    if (text === null) continue
    sources.push({ rel: relOf(dir, file), code: stripKotlinComments(text) })
  }
  return sources.sort((a, b) => a.rel.localeCompare(b.rel))
}

function matchSites(sources, pattern) {
  const flags = pattern.flags.includes('g') ? pattern.flags : `${pattern.flags}g`
  const regex = new RegExp(pattern.source, flags)
  const sites = []
  for (const file of sources) {
    for (const match of file.code.matchAll(regex)) {
      sites.push({ rel: file.rel, line: lineAt(file.code, match.index), text: match[0] })
    }
  }
  return sites
}

function buildTexts(dir) {
  return BUILD_RELS.map((rel) => {
    const file = path.join(dir, ...rel.split('/'))
    if (!existsSync(file)) return null
    try {
      return { rel, text: readFileSync(file, 'utf8') }
    } catch {
      return null
    }
  }).filter(Boolean)
}

function dependencyCheck(dir) {
  const builds = buildTexts(dir)
  for (const file of builds) {
    if (REVIEW_DEP.test(file.text)) {
      const match = REVIEW_DEP.exec(file.text)
      return {
        findings: [],
        summary: `Play review dependency declared in ${file.rel}` + (match ? ` (${match[0]})` : '')
      }
    }
  }
  return {
    findings: [
      'no Play In-App Review dependency: expected com.google.android.play:review-ktx (or a catalog ' +
        'alias that resolves to it) in app/build.gradle.kts or gradle/libs.versions.toml'
    ],
    summary: ''
  }
}

function apiCheck(sources) {
  const factory = matchSites(sources, REVIEW_MANAGER_FACTORY)
  const request = matchSites(sources, REQUEST_REVIEW)
  const launch = matchSites(sources, LAUNCH_REVIEW)
  const findings = []
  if (factory.length === 0) {
    findings.push(
      'no ReviewManagerFactory.create: the In-App Review flow must create a ReviewManager before ' +
        'requestReviewFlow / launchReviewFlow'
    )
  }
  if (request.length === 0) {
    findings.push('no requestReviewFlow call: the Review API was never asked for a ReviewInfo')
  }
  if (launch.length === 0) {
    findings.push('no launchReviewFlow call: a ReviewInfo was never launched against an Activity')
  }
  return {
    findings,
    summary:
      findings.length === 0
        ? `ReviewManager flow present (${factory.length} create, ${request.length} request, ${launch.length} launch)`
        : ''
  }
}

function throttleCheck(sources) {
  const stores = matchSites(sources, DATASTORE)
  const keys = matchSites(sources, THROTTLE_KEYS)
  const findings = []
  if (stores.length === 0) {
    findings.push(
      'no DataStore in the review throttle path: soft prompts need a persisted last-prompted / ' +
        'completed record so they stay moderate'
    )
  }
  if (keys.length === 0) {
    findings.push(
      'no review throttle keys: expected a DataStore preference naming last-prompted or ' +
        'review-completed (e.g. lastPromptedAt, reviewCompleted)'
    )
  }
  return {
    findings,
    summary:
      findings.length === 0
        ? `review throttle persisted (${keys.length} key site(s) over DataStore)`
        : ''
  }
}

function settingsCheck(sources) {
  const findings = []
  const rateSites = matchSites(sources, RATE_AFFORDANCE)
  const apiSites = [
    ...matchSites(sources, REVIEW_MANAGER_FACTORY),
    ...matchSites(sources, REQUEST_REVIEW),
    ...matchSites(sources, LAUNCH_REVIEW)
  ]
  const marketOnly = matchSites(sources, MARKET_FALLBACK)

  if (rateSites.length === 0 && apiSites.length === 0) {
    findings.push(
      'no Rate / In-App Review entry point: settings should offer Rate app (or an equivalent) that ' +
        'calls the Review API, and/or a soft prompt after meaningful use'
    )
    return { findings, summary: '' }
  }

  if (apiSites.length === 0 && marketOnly.length > 0) {
    findings.push(
      `${marketOnly[0].rel}:${marketOnly[0].line}: opens the Play Store listing without attempting ` +
        'ReviewManager first; settings Rate and soft prompts must call requestReviewFlow / ' +
        'launchReviewFlow, and only fall back to market:// when the flow fails to start'
    )
  }

  if (apiSites.length === 0) {
    findings.push(
      'a Rate / review affordance exists but nothing calls ReviewManagerFactory / requestReviewFlow / ' +
        'launchReviewFlow'
    )
  }

  return {
    findings,
    summary:
      findings.length === 0
        ? 'Rate / review path uses the In-App Review API before any store fallback'
        : ''
  }
}

/** `{ [id]: { findings, summary } }` for one module or slot. */
export function reviewAudit(input) {
  const dir = resolveReviewDir(input)
  const sources = kotlinSources(dir)
  return {
    'review.dependency': dependencyCheck(dir),
    'review.api': apiCheck(sources),
    'review.throttle': throttleCheck(sources),
    'review.settings': settingsCheck(sources)
  }
}

export function reviewFindings(input) {
  const audit = reviewAudit(input)
  return REVIEW_IDS.flatMap((id) => audit[id].findings)
}

export function hasKotlinSources(dir) {
  return kotlinSources(dir).length > 0
}

/**
 * Slot-scoped review facts. Predates `IN_APP_REVIEW_BASELINE` → note inside PASS.
 */
export function reviewChecks(slotDir) {
  const dir = resolveReviewDir(slotDir)
  if (!hasKotlinSources(dir)) {
    return REVIEW_IDS.map((id) =>
      unverified(id, 'no Kotlin source under app/src/main/ to measure In-App Review wiring')
    )
  }
  const audit = reviewAudit(dir)
  return REVIEW_IDS.map((id) => {
    const { findings, summary } = audit[id]
    return baselineVerdict(id, dir, findings, summary || `${id} has findings for this slot`)
  })
}

/** Scaffold has no grandfathering — a defect here is copied into every future slot. */
export function reviewProjectChecks() {
  if (!existsSync(SCAFFOLD_DIR)) {
    return REVIEW_IDS.map((id) =>
      unverified(id, 'no scaffold/android-compose to measure the factory review baseline against')
    )
  }
  const audit = reviewAudit(SCAFFOLD_DIR)
  const checks = REVIEW_IDS.map((id) => {
    const { findings, summary } = audit[id]
    return findings.length === 0
      ? pass(id, `template: ${summary}`)
      : fail(id, `template: ${findings.join('; ')}`)
  })
  const documented = documentationCheck()
  checks.push(
    documented.findings.length === 0
      ? pass('review.documented', documented.summary)
      : fail('review.documented', documented.findings.join('; '))
  )
  return checks
}

export function ruleDocProblems(text) {
  const problems = []
  if (!text.includes('scripts/core/review.mjs')) {
    problems.push(
      'the rule does not name scripts/core/review.mjs, so a reader cannot find the program that enforces it'
    )
  }
  const missing = REVIEW_IDS.filter((id) => !text.includes(id))
  if (missing.length > 0) {
    problems.push(`the rule does not name the check id(s) it is enforced by: ${missing.join(', ')}`)
  }
  return problems
}

function documentationCheck() {
  const file = path.join(ROOT, ...REVIEW_RULE_REL.split('/'))
  if (!existsSync(file)) {
    return { findings: [`${REVIEW_RULE_REL} does not exist`], summary: '' }
  }
  let text = null
  try {
    text = readFileSync(file, 'utf8')
  } catch (error) {
    return { findings: [`could not read ${REVIEW_RULE_REL}: ${error.message}`], summary: '' }
  }
  return {
    findings: ruleDocProblems(text),
    summary: `${REVIEW_RULE_REL} names ${REVIEW_IDS.length} check id(s) and the sensor (baseline ${FACTORY_BASELINE}+)`
  }
}

// ---------------------------------------------------------------------------------------------
// Fixtures
// ---------------------------------------------------------------------------------------------

const GOOD_BUILD = [
  '[versions]',
  'playReview = "2.0.2"',
  '',
  '[libraries]',
  'play-review-ktx = { module = "com.google.android.play:review-ktx", version.ref = "playReview" }',
  '',
  '// app/build.gradle.kts',
  'dependencies {',
  '    implementation(libs.play.review.ktx)',
  '}',
  ''
].join('\n')

const GOOD_REVIEW_KT = [
  'package com.gexetr.demo.ui.review',
  '',
  'import android.app.Activity',
  'import androidx.datastore.preferences.core.booleanPreferencesKey',
  'import androidx.datastore.preferences.core.edit',
  'import androidx.datastore.preferences.core.longPreferencesKey',
  'import androidx.datastore.preferences.preferencesDataStore',
  'import com.google.android.play.core.review.ReviewManagerFactory',
  'import kotlinx.coroutines.flow.first',
  '',
  'private val Context.reviewStore by preferencesDataStore(name = "review_prompts")',
  'private val lastPromptedAtKey = longPreferencesKey("last_prompted_at")',
  'private val reviewCompletedKey = booleanPreferencesKey("review_completed")',
  '',
  'class ReviewPromptStore(private val context: Context) {',
  '    suspend fun lastPromptedAt(): Long = context.reviewStore.data.first()[lastPromptedAtKey] ?: 0L',
  '    suspend fun markPrompted() {',
  '        context.reviewStore.edit { it[lastPromptedAtKey] = System.currentTimeMillis() }',
  '    }',
  '    suspend fun markCompleted() {',
  '        context.reviewStore.edit { it[reviewCompletedKey] = true }',
  '    }',
  '}',
  '',
  'fun launchInAppReview(activity: Activity, onFallback: () -> Unit) {',
  '    val manager = ReviewManagerFactory.create(activity)',
  '    manager.requestReviewFlow().addOnCompleteListener { task ->',
  '        if (task.isSuccessful) {',
  '            manager.launchReviewFlow(activity, task.result)',
  '        } else {',
  '            onFallback()',
  '        }',
  '    }',
  '}',
  '',
  'fun openPlayStoreFallback(activity: Activity) {',
  '    val uri = Uri.parse("market://details?id=" + activity.packageName)',
  '    runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, uri)) }',
  '}',
  ''
].join('\n')

const GOOD_SETTINGS = [
  'package com.gexetr.demo.ui.settings',
  '',
  'import androidx.compose.material3.Text',
  'import androidx.compose.runtime.Composable',
  'import androidx.compose.ui.res.stringResource',
  'import com.gexetr.demo.R',
  'import com.gexetr.demo.ui.review.launchInAppReview',
  'import com.gexetr.demo.ui.review.openPlayStoreFallback',
  '',
  '@Composable',
  'fun SettingsScreen(onRateApp: () -> Unit) {',
  '    Text(text = stringResource(R.string.settings_rate_title))',
  '    // Rate app: Review API first, market fallback only if the flow fails to start.',
  '    launchInAppReview(activity) { openPlayStoreFallback(activity) }',
  '}',
  ''
].join('\n')

function writeTree(dir, files) {
  for (const [rel, body] of Object.entries(files)) {
    if (body === null) continue
    const target = path.join(dir, ...rel.split('/'))
    mkdirSync(path.dirname(target), { recursive: true })
    writeFileSync(target, body, 'utf8')
  }
}

export function reviewFixtures() {
  return [
    guard('review.fixtures', () => {
      const cases = []
      const sandbox = () => {
        mkdirSync(SCRATCH_DIR, { recursive: true })
        return mkdtempSync(path.join(SCRATCH_DIR, 'review-fix-'))
      }

      const withGood = (overrides, fn) => {
        const dir = sandbox()
        try {
          writeTree(dir, {
            'gradle/libs.versions.toml': GOOD_BUILD,
            'app/build.gradle.kts': 'dependencies {\n    implementation(libs.play.review.ktx)\n}\n',
            'app/src/main/java/com/gexetr/demo/ui/review/InAppReview.kt': GOOD_REVIEW_KT,
            'app/src/main/java/com/gexetr/demo/ui/settings/SettingsScreen.kt': GOOD_SETTINGS,
            ...overrides
          })
          return fn(dir)
        } finally {
          rmSync(dir, { recursive: true, force: true })
        }
      }

      const findingsFor = (dir, id) => reviewAudit(dir)[id].findings

      const expectFinding = (id, overrides, needle) => {
        expectTrue(
          withGood(overrides, (dir) => findingsFor(dir, id).some((f) => f.includes(needle))),
          `${id} should mention ${needle}`
        )
      }

      cases.push([
        'the good tree passes every review leg',
        () => {
          expectTrue(
            withGood({}, (dir) => REVIEW_IDS.every((id) => findingsFor(dir, id).length === 0)),
            'the good tree reported a review finding'
          )
        }
      ])

      cases.push([
        'missing review-ktx fails review.dependency',
        () => {
          expectFinding(
            'review.dependency',
            {
              'gradle/libs.versions.toml': '[versions]\n\n[libraries]\n',
              'app/build.gradle.kts': 'dependencies {\n}\n'
            },
            'review-ktx'
          )
        }
      ])

      cases.push([
        'missing ReviewManager flow fails review.api',
        () => {
          expectFinding(
            'review.api',
            {
              'app/src/main/java/com/gexetr/demo/ui/review/InAppReview.kt':
                'package com.gexetr.demo.ui.review\n\nfun noop() {}\n'
            },
            'ReviewManagerFactory'
          )
        }
      ])

      cases.push([
        'missing DataStore throttle keys fails review.throttle',
        () => {
          expectFinding(
            'review.throttle',
            {
              'app/src/main/java/com/gexetr/demo/ui/review/InAppReview.kt': [
                'package com.gexetr.demo.ui.review',
                '',
                'import com.google.android.play.core.review.ReviewManagerFactory',
                '',
                'fun launchInAppReview(activity: Activity) {',
                '    val manager = ReviewManagerFactory.create(activity)',
                '    manager.requestReviewFlow()',
                '    manager.launchReviewFlow(activity, reviewInfo)',
                '}',
                ''
              ].join('\n')
            },
            'DataStore'
          )
        }
      ])

      cases.push([
        'market:// without ReviewManager fails review.settings',
        () => {
          expectFinding(
            'review.settings',
            {
              'app/src/main/java/com/gexetr/demo/ui/review/InAppReview.kt':
                'package com.gexetr.demo.ui.review\n\nfun rate() { open("market://details?id=x") }\n',
              'app/src/main/java/com/gexetr/demo/ui/settings/SettingsScreen.kt': [
                'package com.gexetr.demo.ui.settings',
                '',
                '@Composable',
                'fun SettingsScreen() {',
                '    Text("Rate app")',
                '    open("market://details?id=demo")',
                '}',
                ''
              ].join('\n')
            },
            'without attempting ReviewManager'
          )
        }
      ])

      cases.push([
        'the real rule names the sensor and its ids',
        () => {
          const text = readFileSync(path.join(ROOT, ...REVIEW_RULE_REL.split('/')), 'utf8')
          expectTrue(ruleDocProblems(text).length === 0, ruleDocProblems(text).join(' | '))
        }
      ])

      return runFixtures('review.fixtures', cases)
    })
  ]
}
