/**
 * Offline regression suite for `scripts/lib/client-secrets.mjs`.
 *
 * The sensor exists because the factory protected the repository and not the APK: `secret-guard`
 * blocks keys in git, and a slot that compiled a provider key into `app/src/main/` passed every gate
 * the factory had. The measured scale of the defect is in
 * `docs/research/2026-09-29-app-factory-prior-art.md` §3 row 8 (change B1): 66.3 % of 1,207
 * LLM-enabled Android apps carried at least one LLM-API misuse, 41.0 % leaked API credentials, and
 * secrets were found in 35 % of 50,000 Play apps.
 *
 * A check is only worth having if it fails when the defect is present, so every positive case below is
 * a defect that would ship, and every negative case is a shape a naive detector gets wrong — a scanner
 * that fires on `val apiKey = "YOUR_API_KEY"` or on the word "secret" in a sentence is a scanner whose
 * output nobody reads. The split-key case is the one the obvious implementation (one regex per line
 * over the raw text) misses.
 *
 * The sandboxes are real directories under the system temp dir, so the suite exercises the same file
 * reading the gate does, and it never touches `apps/`.
 *
 * Run: node scripts/test-client-secrets.mjs
 */
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { RULE_IDS, RULES, scanSlot } from './lib/client-secrets.mjs'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
let failures = 0
let checks = 0
const sandboxes = []
const seenRules = new Set()

function check(name, condition, detail) {
  checks += 1
  if (condition) {
    console.log(`  PASS  ${name}`)
  } else {
    failures += 1
    console.log(`  FAIL  ${name}`)
    if (detail) console.log(`        ${detail}`)
  }
}

/**
 * Builds a throwaway slot from a map of slot-relative path → text. Nothing else is created, so a case
 * that leaves `app/src/main/` out on purpose is the UNVERIFIED case.
 */
function makeSlot(files = {}) {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'kiln-client-secrets-'))
  sandboxes.push(dir)
  for (const [relative, body] of Object.entries(files)) {
    const target = path.join(dir, relative.split('/').join(path.sep))
    fs.mkdirSync(path.dirname(target), { recursive: true })
    fs.writeFileSync(target, body, 'utf8')
  }
  return dir
}

const run = (slotDir) => {
  const result = scanSlot(slotDir)
  for (const finding of result.findings) seenRules.add(finding.rule)
  return result
}

/** A value that looks like a credential: long, alphanumeric, mixed case, no placeholder marker. */
const ALNUM = 'SyDdEfGhIjKlMnOpQrStUvWxYz0123456789AbCdEfGh'
const GOOGLE_KEY = `AIza${ALNUM.slice(0, 35)}` // Google/Firebase shape: AIza + exactly 35 characters
const STRIPE_KEY = `sk_live_${ALNUM.slice(0, 24)}`
const OPENAI_KEY = `sk-proj-${ALNUM.slice(0, 30)}`
const ANTHROPIC_KEY = `sk-ant-api03-${ALNUM.slice(0, 24)}`

const kotlin = (relative, body) => ({ [`app/src/main/java/com/example/${relative}`]: body })

console.log('=== T1: the defect this gate exists for — a key compiled into the client ===')
{
  const slot = makeSlot(kotlin('Net.kt', `package com.example\n\nval credential = "${GOOGLE_KEY}"\n`))
  const { verdict, rows, findings } = run(slot)
  check('a Google key literal in Kotlin FAILS', verdict === 'FAIL', `verdict ${verdict}`)
  check(
    'the failure names the rule and the file, so the fix has a location',
    rows.some((row) => row.status === 'FAIL' && row.message.includes(RULES.KEY_LITERAL) && row.message.includes('Net.kt')),
    JSON.stringify(rows)
  )
  check('the finding carries the provider shape that matched', findings.some((f) => f.provider === 'google-api-key'))
}

console.log('\n=== T2: a key split across literals is the same credential ===')
{
  const head = 'AIza'
  const tail = GOOGLE_KEY.slice(4)
  const rawLine = `val credential = "${head}" + "${tail}"`
  const slot = makeSlot(kotlin('Split.kt', `package com.example\n\n${rawLine}\n`))
  const { verdict, findings } = run(slot)
  check(
    'the fixture is a genuine split: its raw line carries no complete key',
    !new RegExp('AIza[0-9A-Za-z_-]{35}').test(rawLine),
    rawLine
  )
  check('a key concatenated from two literals FAILS', verdict === 'FAIL', `verdict ${verdict}`)
  check('and the finding is the key-literal rule', findings.some((f) => f.rule === RULES.KEY_LITERAL))
}

console.log('\n=== T3: a key in an Android string resource ===')
{
  const slot = makeSlot({
    ...kotlin('Home.kt', 'package com.example\n\nval title = "Records"\n'),
    'app/src/main/res/values/strings_keys.xml': [
      '<?xml version="1.0" encoding="utf-8"?>',
      '<resources>',
      `    <string name="openai_api_key" translatable="false">${OPENAI_KEY}</string>`,
      '</resources>',
      ''
    ].join('\n')
  })
  const { verdict, findings, rows } = run(slot)
  check('an OpenAI key in res/values/strings_keys.xml FAILS', verdict === 'FAIL', `verdict ${verdict}`)
  check('the finding is an OpenAI shape', findings.some((f) => f.provider === 'openai-project-key'))
  check(
    'and the failure names the resource file',
    rows.some((row) => row.status === 'FAIL' && row.message.includes('strings_keys.xml')),
    JSON.stringify(rows)
  )
}

console.log('\n=== T4: a key in the version catalog ===')
{
  const slot = makeSlot({
    ...kotlin('Home.kt', 'package com.example\n\nval title = "Records"\n'),
    'gradle/libs.versions.toml': `[versions]\nagp = "9.4.1"\nanthropicKey = "${ANTHROPIC_KEY}"\n`
  })
  const { verdict, rows } = run(slot)
  check('a key in gradle/libs.versions.toml FAILS', verdict === 'FAIL', `verdict ${verdict}`)
  check(
    'the failure names the catalog file',
    rows.some((row) => row.status === 'FAIL' && row.message.includes('gradle/libs.versions.toml')),
    JSON.stringify(rows)
  )
}

console.log('\n=== T5: a key in the R8/ProGuard rules ===')
{
  const slot = makeSlot({
    ...kotlin('Home.kt', 'package com.example\n\nval title = "Records"\n'),
    'app/proguard-rules.pro': `# R8 rules for the release build\n-keepattributes *Annotation*\n${STRIPE_KEY}\n`
  })
  const { verdict, findings } = run(slot)
  check('a Stripe secret key in app/proguard-rules.pro FAILS', verdict === 'FAIL', `verdict ${verdict}`)
  check('the finding is a Stripe shape', findings.some((f) => f.provider === 'stripe-secret-key'))
}

console.log('\n=== T6: google-services.json ===')
{
  const slot = makeSlot({
    ...kotlin('Home.kt', 'package com.example\n\nval title = "Records"\n'),
    'app/google-services.json': JSON.stringify(
      { project_info: { project_id: 'kiln' }, client: [{ api_key: [{ current_key: GOOGLE_KEY }] }] },
      null,
      2
    )
  })
  const { verdict, findings } = run(slot)
  check('a google-services.json with a real-shaped key FAILS', verdict === 'FAIL', `verdict ${verdict}`)
  check('and the finding is the google-services rule', findings.some((f) => f.rule === RULES.GOOGLE_SERVICES_KEY))

  const placeholder = makeSlot({
    ...kotlin('Home.kt', 'package com.example\n\nval title = "Records"\n'),
    'app/google-services.json': JSON.stringify({ client: [{ api_key: [{ current_key: 'YOUR_API_KEY' }] }] })
  })
  const soft = run(placeholder)
  check('a placeholder key is not a failing finding', soft.verdict === 'PASS', `verdict ${soft.verdict}`)
  check('but the file is still reported as a note', soft.notes.some((note) => note.includes('google-services.json')))
}

console.log('\n=== T7: a raw call to a paid provider host ===')
{
  const slot = makeSlot(
    kotlin(
      'Client.kt',
      [
        'package com.example',
        '',
        'import java.net.URL',
        '',
        'fun summarise(text: String): String {',
        '    val endpoint = "https://api.openai.com/v1/chat/completions"',
        '    return URL(endpoint).readText()',
        '}'
      ].join('\n')
    )
  )
  const { verdict, findings } = run(slot)
  check('a plaintext api.openai.com call inside a Kotlin function FAILS', verdict === 'FAIL', `verdict ${verdict}`)
  check('the finding is the provider-host rule', findings.some((f) => f.rule === RULES.PROVIDER_HOST))

  const commented = makeSlot(
    kotlin(
      'Client.kt',
      [
        'package com.example',
        '',
        '// The paid endpoint api.openai.com is deliberately never called, and that is the point.',
        'fun summarise(text: String): String = text.trim()'
      ].join('\n')
    )
  )
  const soft = run(commented)
  check('a comment that mentions the host is NOT a finding', soft.verdict === 'PASS', `verdict ${soft.verdict}`)
  check('and it is reported as a note rather than dropped silently', soft.notes.some((note) => note.includes('comment')))
}

console.log('\n=== T8: a paid provider SDK on the client classpath ===')
{
  const slot = makeSlot({
    ...kotlin('Home.kt', 'package com.example\n\nval title = "Records"\n'),
    'app/build.gradle.kts': [
      'dependencies {',
      '    implementation("androidx.core:core-ktx:1.19.0")',
      '    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")',
      '}',
      ''
    ].join('\n')
  })
  const { verdict, findings } = run(slot)
  check('a provider SDK in app/build.gradle.kts FAILS', verdict === 'FAIL', `verdict ${verdict}`)
  check('the finding is the provider-SDK rule', findings.some((f) => f.rule === RULES.PROVIDER_SDK))

  const catalog = makeSlot({
    ...kotlin('Home.kt', 'package com.example\n\nval title = "Records"\n'),
    'gradle/libs.versions.toml':
      '[libraries]\nopenai = { module = "com.openai:openai-java", version.ref = "openai" }\n'
  })
  check('a provider declared in the version catalog FAILS', run(catalog).verdict === 'FAIL')

  const commented = makeSlot({
    ...kotlin('Home.kt', 'package com.example\n\nval title = "Records"\n'),
    'app/build.gradle.kts': [
      'dependencies {',
      '    // implementation("com.google.ai.client.generativeai:generativeai:0.9.0") until a key exists',
      '    implementation("androidx.core:core-ktx:1.19.0")',
      '}',
      ''
    ].join('\n')
  })
  check('a commented-out provider declaration is NOT a finding', run(commented).verdict === 'PASS')
}

console.log('\n=== T9: a generic long assignment ===')
{
  const slot = makeSlot(
    kotlin('Config.kt', `package com.example\n\nval signingSecret = "${ALNUM.slice(0, 27)}"\n`)
  )
  const { verdict, findings } = run(slot)
  check('a long non-placeholder value assigned to a secret-looking name FAILS', verdict === 'FAIL', `verdict ${verdict}`)
  check('the finding names the assignment', findings.some((f) => f.provider === 'generic-secret-assignment'))
}

console.log('\n=== T10: negative control — a documented placeholder is not a key ===')
{
  const slot = makeSlot(kotlin('Config.kt', 'package com.example\n\nval apiKey = "YOUR_API_KEY"\n'))
  const { verdict, findings } = run(slot)
  check('`val apiKey = "YOUR_API_KEY"` PASSES', verdict === 'PASS', `verdict ${verdict}`)
  check('and it is not a finding', findings.length === 0, JSON.stringify(findings))
}

console.log('\n=== T11: negative control — prose is not an assignment ===')
{
  const slot = makeSlot(
    kotlin(
      'About.kt',
      [
        'package com.example',
        '',
        '// The secret of this app is that it never needs a network call at all.',
        'val tagline = "Private by design, and no account is required to use it"',
        ''
      ].join('\n')
    )
  )
  const { verdict, findings } = run(slot)
  check('the word "secret" in prose PASSES', verdict === 'PASS', `verdict ${verdict}`)
  check('and it produces no finding', findings.length === 0, JSON.stringify(findings))
}

console.log('\n=== T12: negative control — a legitimate offline slot is clean ===')
{
  const slot = makeSlot({
    ...kotlin(
      'Home.kt',
      [
        'package com.example',
        '',
        'import androidx.compose.material3.Text',
        'import androidx.compose.runtime.Composable',
        '',
        '@Composable',
        'fun HomeScreen(records: Int) {',
        '    Text("Records: $records")',
        '}'
      ].join('\n')
    ),
    'app/build.gradle.kts': 'dependencies {\n    implementation(libs.androidx.core.ktx)\n}\n',
    'gradle/libs.versions.toml': '[libraries]\nandroidx-core-ktx = { module = "androidx.core:core-ktx" }\n',
    'app/proguard-rules.pro': '# nothing to keep\n'
  })
  const { verdict, rows, scannedFiles } = run(slot)
  check('an offline slot with no network calls PASSES', verdict === 'PASS', `verdict ${verdict}`)
  check('and the scan reports how much it read', scannedFiles >= 4, `scannedFiles ${scannedFiles}`)
  check('and the PASS row names its own scope', rows.some((row) => row.status === 'PASS'), JSON.stringify(rows))
}

console.log('\n=== T13: a placeholder-shaped key is downgraded, not dropped ===')
{
  const slot = makeSlot(kotlin('Config.kt', `package com.example\n\nval apiKey = "AIza${'x'.repeat(35)}"\n`))
  const { verdict, findings, notes } = run(slot)
  check('a Google-shaped placeholder PASSES', verdict === 'PASS', `verdict ${verdict}`)
  check('with no finding', findings.length === 0, JSON.stringify(findings))
  check('but explicitly reported as a note', notes.some((note) => note.includes('placeholder')), JSON.stringify(notes))
}

console.log('\n=== T14: nothing to scan is UNVERIFIED, never PASS ===')
{
  const empty = makeSlot({ 'README.md': '# nothing here\n' })
  check('a slot with no app/src/main/ is UNVERIFIED', run(empty).verdict === 'UNVERIFIED', run(empty).verdict)

  const buildOnly = makeSlot({ 'app/build.gradle.kts': 'dependencies { }\n' })
  check('build files without client sources are UNVERIFIED too', run(buildOnly).verdict === 'UNVERIFIED')
}

console.log('\n=== T15: an exemption is a declared, reviewed suppression ===')
{
  const path0 = 'app/src/main/java/com/example/Net.kt'
  const files = kotlin('Net.kt', `package com.example\n\nval credential = "${GOOGLE_KEY}"\n`)
  files['client-secrets-exempt.json'] = JSON.stringify(
    [
      {
        path: path0,
        rule: RULES.KEY_LITERAL,
        reason: 'fixture: the value is the documented sample credential from the provider quickstart',
        decidedBy: 'owner'
      }
    ],
    null,
    2
  )
  const slot = makeSlot(files)
  const { verdict, rows, findings, exemptions } = run(slot)
  check('an exemption with a reason and a decider suppresses the finding', verdict === 'PASS', `verdict ${verdict}`)
  check('and no finding survives', findings.length === 0, JSON.stringify(findings))
  check('and the exemption is reported by path and rule', exemptions.length === 1 && exemptions[0].path === path0 && exemptions[0].rule === RULES.KEY_LITERAL)
  check(
    'and it appears in the printed rows, so a suppression is never invisible',
    rows.some((row) => row.status === 'EXEMPT' && row.message.includes(path0) && row.message.includes(RULES.KEY_LITERAL)),
    JSON.stringify(rows)
  )
}

console.log('\n=== T16: an unqualified exemption is a failure, not an exemption ===')
{
  const path0 = 'app/src/main/java/com/example/Net.kt'
  const base = kotlin('Net.kt', `package com.example\n\nval credential = "${GOOGLE_KEY}"\n`)

  const noReason = makeSlot({
    ...base,
    'client-secrets-exempt.json': JSON.stringify([{ path: path0, rule: RULES.KEY_LITERAL, decidedBy: 'owner' }])
  })
  const noReasonResult = run(noReason)
  check('an exemption with no reason FAILS', noReasonResult.verdict === 'FAIL', `verdict ${noReasonResult.verdict}`)
  check(
    'and the message says which field is missing',
    noReasonResult.rows.some((row) => row.status === 'FAIL' && /reason/.test(row.message)),
    JSON.stringify(noReasonResult.rows)
  )

  const noDecider = makeSlot({
    ...base,
    'client-secrets-exempt.json': JSON.stringify([{ path: path0, rule: RULES.KEY_LITERAL, reason: 'because' }])
  })
  check('an exemption with no decidedBy FAILS too', run(noDecider).verdict === 'FAIL')

  const unknownRule = makeSlot({
    ...base,
    'client-secrets-exempt.json': JSON.stringify([
      { path: path0, rule: 'client-something-else', reason: 'because', decidedBy: 'owner' }
    ])
  })
  const unknownResult = run(unknownRule)
  check('an exemption naming an unknown rule FAILS', unknownResult.verdict === 'FAIL', `verdict ${unknownResult.verdict}`)
  check(
    'and the message lists the rules the detector knows',
    unknownResult.rows.some((row) => row.status === 'FAIL' && RULE_IDS.every((id) => row.message.includes(id))),
    JSON.stringify(unknownResult.rows)
  )

  const stale = makeSlot({
    ...kotlin('Home.kt', 'package com.example\n\nval title = "Records"\n'),
    'client-secrets-exempt.json': JSON.stringify([
      { path: 'app/src/main/java/com/example/Gone.kt', rule: RULES.PROVIDER_SDK, reason: 'stale', decidedBy: 'owner' }
    ])
  })
  const staleResult = run(stale)
  check('an exemption that matches nothing PASSES but is reported', staleResult.verdict === 'PASS', `verdict ${staleResult.verdict}`)
  check('and the note says it can be removed', staleResult.notes.some((note) => note.includes('matches no finding')))
}

console.log('\n=== T17: the CLI itself refuses what it cannot answer ===')
{
  const cli = path.join(root, 'scripts', 'check-client-secrets.mjs')
  const runCli = (args) => spawnSync(process.execPath, [cli, ...args], { cwd: root, encoding: 'utf8' })

  const noSlot = runCli([])
  check('no --slot is exit 2 (UNVERIFIED), not 0', noSlot.status === 2, `status ${noSlot.status}`)

  const badFlag = runCli(['--slot', 'teacher-gradebook', '--json', '--typo'])
  check('an unknown flag is exit 2 rather than ignored', badFlag.status === 2, `status ${badFlag.status}`)

  const missing = runCli(['--slot', 'no-such-slot-in-this-factory'])
  check('a slot that does not resolve is exit 2', missing.status === 2, `status ${missing.status}`)

  // Regression case: the first version of the arg parser consumed `--slot`'s two positions but not the
  // position of `--json`, so `--json` was refused as an unknown argument. A suite that only tested the
  // refusal paths could not see it.
  const jsonAccepted = runCli(['--slot', 'no-such-slot-in-this-factory', '--json'])
  check(
    '--json is an accepted flag, not an unknown argument',
    !jsonAccepted.stderr.includes('unknown argument'),
    jsonAccepted.stderr.trim()
  )
}

console.log('\n=== T18: the suite exercises every rule the detector declares ===')
check(
  `all ${RULE_IDS.length} rule ids fired in at least one fixture`,
  RULE_IDS.every((id) => seenRules.has(id)),
  `seen: ${[...seenRules].join(', ')}`
)

for (const dir of sandboxes) fs.rmSync(dir, { recursive: true, force: true })

console.log(`\n=== ${checks} checks, ${checks - failures} passed, ${failures} failure(s) ===`)
console.log(failures === 0 ? 'CLIENT-SECRETS SENSOR PASSED' : 'CLIENT-SECRETS SENSOR FAILED')
process.exit(failures === 0 ? 0 : 1)
