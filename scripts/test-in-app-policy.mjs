/**
 * Offline regression suite for `scripts/lib/in-app-policy.mjs`.
 *
 * The sensor exists because a rule the factory had already written down was shipped broken anyway:
 * `apps/2026-09-26-teacher-gradebook` declares `privacyPolicyUrl` in its store pack, renders privacy
 * prose in `AboutScreen.kt`, and cannot open the policy from inside the app — while Play requires it
 * in both places. `AGENTS.md` stated the rule and nothing measured it. A rule that only a document
 * asserts is the defect, so the fix is a check, and a check is only worth having if it fails when the
 * defect is present. Every case below is a defect that actually recurred or that the naive
 * implementation of this check gets wrong.
 *
 * The sandboxes are real directories under the system temp dir, so the suite exercises the same file
 * reading the gate does, and it never touches `apps/`.
 *
 * Run: node scripts/test-in-app-policy.mjs
 */
import fs from 'node:fs'
import { SCRATCH_DIR } from './lib/scratch.mjs'
import path from 'node:path'
import { evaluateInAppPolicy } from './lib/in-app-policy.mjs'

let failures = 0
let checks = 0
const sandboxes = []

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

const POLICY_URL = 'https://telegra.ph/Mark-Book-for-Teachers--privacy-policy-09-26'
const OTHER_URL = 'https://example.invalid/some-other-policy'

/**
 * Builds a throwaway slot. Everything is optional so a case can leave a half out on purpose — that is
 * what most of these cases are.
 */
function makeSlot({ listing = null, values = {}, locales = {}, kotlin = {} } = {}) {
  const dir = fs.mkdtempSync(path.join(SCRATCH_DIR, 'kiln-in-app-policy-'))
  sandboxes.push(dir)
  if (listing !== null) {
    fs.mkdirSync(path.join(dir, 'store'), { recursive: true })
    fs.writeFileSync(
      path.join(dir, 'store', 'listing.json'),
      typeof listing === 'string' ? listing : JSON.stringify(listing, null, 2),
      'utf8'
    )
  }
  for (const [name, body] of Object.entries(values)) {
    const target = path.join(dir, 'app', 'src', 'main', 'res', 'values')
    fs.mkdirSync(target, { recursive: true })
    fs.writeFileSync(path.join(target, name), body, 'utf8')
  }
  for (const [locale, files] of Object.entries(locales)) {
    for (const [name, body] of Object.entries(files)) {
      const target = path.join(dir, 'app', 'src', 'main', 'res', `values-${locale}`)
      fs.mkdirSync(target, { recursive: true })
      fs.writeFileSync(path.join(target, name), body, 'utf8')
    }
  }
  for (const [relative, body] of Object.entries(kotlin)) {
    const target = path.join(dir, 'app', 'src', 'main', 'java', path.dirname(relative))
    fs.mkdirSync(target, { recursive: true })
    fs.writeFileSync(path.join(dir, 'app', 'src', 'main', 'java', relative), body, 'utf8')
  }
  return dir
}

function stringsXml(id, ...values) {
  const body = values.map((value) => `<string name="${value.id}">${value.text}</string>`).join('')
  return `<?xml version="1.0" encoding="utf-8"?>\n<resources>${body}</resources>\n`
}

/** A screen that references a string id, with an optional way to open a URI. */
function screen({ refs = [], opener = null } = {}) {
  const references = refs.map((id) => `Text(stringResource(R.string.${id}))`).join('\n')
  return `package com.example\n\nfun AboutScreen(uriHandler: UriHandler) {\n${references}\n${opener || ''}\n}\n`
}

/**
 * Four shapes, because the Compose case has two: reading `LocalUriHandler.current` inline, and the
 * recommended hoisted form where the screen receives a `UriHandler` and calls `openUri` on it. The
 * sensor's first version recognised only the first, so this fixture is what caught it — a suite that
 * only tested the form the author had in mind would have shipped a sensor that fails correct code.
 */
const OPENERS = {
  'Intent.ACTION_VIEW': 'context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(s)))',
  CustomTabsIntent: 'context.startActivity(CustomTabsIntent.Builder().build().intent)',
  'LocalUriHandler.current': 'LocalUriHandler.current.openUri(s)',
  'UriHandler.openUri (hoisted handler)': 'uriHandler.openUri(s)'
}

const run = (slotDir) => evaluateInAppPolicy(slotDir, { root: slotDir })
const listing = (url = POLICY_URL) => ({ contactEmail: 'a@b.example', privacyPolicyUrl: url })

console.log('=== T1: the shipped defect — a policy URL in the pack that the app cannot open ===')
{
  const slot = makeSlot({
    listing: listing(),
    values: { 'strings_misc.xml': stringsXml('x', { id: 'about_privacy_title', text: 'Private by design' }) },
    kotlin: { 'ui/about/AboutScreen.kt': screen({ refs: ['about_privacy_title'] }) }
  })
  const { verdict, rows } = run(slot)
  check('a pack with a privacy policy and no in-app link FAILS', verdict === 'FAIL', `verdict ${verdict}`)
  check(
    'the failure names the missing half rather than the store pack',
    rows.some((row) => row.status === 'FAIL' && /cannot open the policy/.test(row.message)),
    JSON.stringify(rows)
  )
}

console.log('\n=== T2: a pack that declares no policy has nothing to mirror ===')
{
  const slot = makeSlot({ listing: { contactEmail: 'a@b.example' }, kotlin: { 'Main.kt': screen() } })
  const { verdict } = run(slot)
  check('no privacyPolicyUrl is PASS (inapplicable), not FAIL', verdict === 'PASS', `verdict ${verdict}`)
}

console.log('\n=== T3: the URL must be a default-locale resource, not a Kotlin literal ===')
{
  const slot = makeSlot({
    listing: listing(),
    kotlin: { 'ui/about/AboutScreen.kt': `${screen({ opener: OPENERS['Intent.ACTION_VIEW'] })}\nval policy = "${POLICY_URL}"\n` }
  })
  const { verdict, rows } = run(slot)
  check('a hardcoded URL in Kotlin FAILS', verdict === 'FAIL', `verdict ${verdict}`)
  check(
    'and the failure says "hardcoded", so the fix is obvious',
    rows.some((row) => row.status === 'FAIL' && /hardcoded/.test(row.message)),
    JSON.stringify(rows)
  )
}

console.log('\n=== T4: an unrelated URL in the app does not satisfy the check ===')
{
  const slot = makeSlot({
    listing: listing(),
    values: { 'strings_misc.xml': stringsXml('x', { id: 'about_site', text: OTHER_URL }) },
    kotlin: { 'ui/about/AboutScreen.kt': screen({ refs: ['about_site'], opener: OPENERS['Intent.ACTION_VIEW'] }) }
  })
  const { verdict } = run(slot)
  check('a different URL than the pack declares FAILS', verdict === 'FAIL', `verdict ${verdict}`)
}

console.log('\n=== T5: declared and never referenced is the same defect in a different coat ===')
{
  const slot = makeSlot({
    listing: listing(),
    values: { 'strings_misc.xml': stringsXml('x', { id: 'about_privacy_link', text: POLICY_URL }) },
    kotlin: { 'ui/about/AboutScreen.kt': screen({ refs: ['about_privacy_title'], opener: OPENERS['Intent.ACTION_VIEW'] }) }
  })
  const { verdict, rows } = run(slot)
  check('a URL string that no Kotlin references FAILS', verdict === 'FAIL', `verdict ${verdict}`)
  check(
    'and the failure names the unreferenced string',
    rows.some((row) => row.status === 'FAIL' && /never referenced from Kotlin/.test(row.message)),
    JSON.stringify(rows)
  )
}

console.log('\n=== T6: the opener must be in the file that references the string ===')
{
  const slot = makeSlot({
    listing: listing(),
    values: { 'strings_misc.xml': stringsXml('x', { id: 'about_privacy_link', text: POLICY_URL }) },
    kotlin: {
      'ui/about/AboutScreen.kt': screen({ refs: ['about_privacy_link'] }),
      'util/Opener.kt': `fun open(context: Context, s: String) { ${OPENERS['Intent.ACTION_VIEW']} }\n`
    }
  })
  const { verdict, rows } = run(slot)
  check('an opener in an unrelated file does not count FAILS', verdict === 'FAIL', `verdict ${verdict}`)
  check(
    'and the message points at the referenced-but-unopenable case',
    rows.some((row) => row.status === 'FAIL' && /not in a file that can open a URI/.test(row.message)),
    JSON.stringify(rows)
  )
}

console.log('\n=== T7: every accepted opener opens the policy ===')
for (const [id, opener] of Object.entries(OPENERS)) {
  const slot = makeSlot({
    listing: listing(),
    values: { 'strings_misc.xml': stringsXml('x', { id: 'about_privacy_link', text: POLICY_URL }) },
    kotlin: { 'ui/about/AboutScreen.kt': screen({ refs: ['about_privacy_link'], opener }) }
  })
  const { verdict } = run(slot)
  check(`${id} is accepted`, verdict === 'PASS', `verdict ${verdict}`)
}

console.log('\n=== T8: the string id is not hardcoded in the sensor ===')
{
  const slot = makeSlot({
    listing: listing(),
    values: { 'strings_misc.xml': stringsXml('x', { id: 'zzz_policy', text: POLICY_URL }) },
    kotlin: { 'ui/about/AboutScreen.kt': screen({ refs: ['zzz_policy'], opener: OPENERS['LocalUriHandler.current'] }) }
  })
  const { verdict } = run(slot)
  check('an arbitrarily named resource id PASSES', verdict === 'PASS', `verdict ${verdict}`)
}

console.log('\n=== T9: the address must not depend on the phone language ===')
{
  const slot = makeSlot({
    listing: listing(),
    locales: { ru: { 'strings_misc.xml': stringsXml('x', { id: 'about_privacy_link', text: POLICY_URL }) } },
    kotlin: { 'ui/about/AboutScreen.kt': screen({ refs: ['about_privacy_link'], opener: OPENERS['LocalUriHandler.current'] }) }
  })
  const { verdict } = run(slot)
  check('a URL only under values-<locale>/ FAILS', verdict === 'FAIL', `verdict ${verdict}`)
}

console.log('\n=== T10: a duplicated address is reported, not failed ===')
{
  const slot = makeSlot({
    listing: listing(),
    values: { 'strings_misc.xml': stringsXml('x', { id: 'about_privacy_link', text: POLICY_URL }) },
    locales: { ru: { 'strings_misc.xml': stringsXml('x', { id: 'about_privacy_link', text: POLICY_URL }) } },
    kotlin: { 'ui/about/AboutScreen.kt': screen({ refs: ['about_privacy_link'], opener: OPENERS['LocalUriHandler.current'] }) }
  })
  const { verdict, notes } = run(slot)
  check('a copy in values-ru/ still PASSES', verdict === 'PASS', `verdict ${verdict}`)
  check(
    'but the duplication is reported as a note',
    notes.some((note) => /values-ru/.test(note)),
    JSON.stringify(notes)
  )
}

console.log('\n=== T11: an undecidable slot is UNVERIFIED, never PASS ===')
{
  const absent = makeSlot({ values: { 'strings_misc.xml': stringsXml('x', { id: 'a', text: 'b' }) } })
  check('no store/listing.json is UNVERIFIED', run(absent).verdict === 'UNVERIFIED', run(absent).verdict)
  const broken = makeSlot({ listing: '{ not json', kotlin: { 'Main.kt': screen() } })
  check('unparseable listing.json is UNVERIFIED', run(broken).verdict === 'UNVERIFIED', run(broken).verdict)
}

console.log('\n=== T12: the comparison is exact and trimmed, not fuzzy ===')
{
  const padded = makeSlot({
    listing: { contactEmail: 'a@b.example', privacyPolicyUrl: `  ${POLICY_URL}  ` },
    values: { 'strings_misc.xml': stringsXml('x', { id: 'about_privacy_link', text: POLICY_URL }) },
    kotlin: { 'ui/about/AboutScreen.kt': screen({ refs: ['about_privacy_link'], opener: OPENERS['LocalUriHandler.current'] }) }
  })
  check('surrounding whitespace in listing.json does not break it', run(padded).verdict === 'PASS', run(padded).verdict)

  const nearMiss = makeSlot({
    listing: { contactEmail: 'a@b.example', privacyPolicyUrl: `${POLICY_URL}/` },
    values: { 'strings_misc.xml': stringsXml('x', { id: 'about_privacy_link', text: POLICY_URL }) },
    kotlin: { 'ui/about/AboutScreen.kt': screen({ refs: ['about_privacy_link'], opener: OPENERS['LocalUriHandler.current'] }) }
  })
  check('a trailing-slash near miss FAILS rather than passing loosely', run(nearMiss).verdict === 'FAIL', run(nearMiss).verdict)
}

console.log('\n=== T13: a placeholder address is not a policy yet ===')
{
  const seeded = makeSlot({
    listing: { contactEmail: 'a@b.example', privacyPolicyUrl: 'https://example.com/privacy' },
    kotlin: { 'Main.kt': screen() }
  })
  const { verdict, rows } = run(seeded)
  check('the pack renderer\'s seeded placeholder is UNVERIFIED, not FAIL', verdict === 'UNVERIFIED', `verdict ${verdict}`)
  check(
    'and it says why, so nobody reads it as "policy handled"',
    rows.some((row) => row.status === 'UNVERIFIED' && /placeholder/.test(row.message)),
    JSON.stringify(rows)
  )

  for (const [label, url] of [
    ['localhost', 'http://localhost:8080/privacy'],
    ['a non-http scheme', 'ftp://files.example.test/privacy'],
    ['a subdomain of a reserved host', 'https://docs.example.com/privacy'],
    ['an address that is not a URL at all', 'not a url']
  ]) {
    const slot = makeSlot({
      listing: { contactEmail: 'a@b.example', privacyPolicyUrl: url },
      kotlin: { 'Main.kt': screen() }
    })
    check(`${label} is UNVERIFIED`, run(slot).verdict === 'UNVERIFIED', `verdict ${run(slot).verdict}`)
  }

  // The rule keys off the HOST, so a real host whose path merely mentions a reserved name is a real
  // policy. A substring test would get this backwards and quietly stop checking that slot.
  const lookalike = makeSlot({
    listing: { contactEmail: 'a@b.example', privacyPolicyUrl: 'https://telegra.ph/example.com/privacy' },
    kotlin: { 'Main.kt': screen() }
  })
  check(
    'a real host with "example.com" in the path still decides (FAIL here, because nothing opens it)',
    run(lookalike).verdict === 'FAIL',
    `verdict ${run(lookalike).verdict}`
  )
}

for (const dir of sandboxes) fs.rmSync(dir, { recursive: true, force: true })

console.log(`\n=== ${checks} checks, ${failures} failure(s) ===`)
console.log(failures === 0 ? 'IN-APP POLICY SENSOR PASSED' : 'IN-APP POLICY SENSOR FAILED')
process.exit(failures === 0 ? 0 : 1)
