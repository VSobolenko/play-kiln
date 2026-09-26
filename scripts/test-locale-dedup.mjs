#!/usr/bin/env node
/**
 * Reproduce the defect that let a `translatable="false"` placeholder reach the About screen.
 *
 * `apps/2026-09-26-teacher-gradebook` shipped `about_support_email` as `support@example.com` in the
 * default resources and in eight locale directories at once, while `store/listing.json` advertised
 * the developer's real address. `check-locales.mjs` skipped non-translatable keys, so it could not
 * see any of them: the value had nine copies and no gate. `about_app_name` and
 * `common_date_placeholder` were duplicated the same way, and were found only after the first was
 * fixed.
 *
 * This drives the real `check-locales.mjs` against a synthetic slot, in four legs:
 *
 *   1. A correct slot passes. Without this the other legs prove nothing - a check that can never
 *      pass is not a check.
 *   2. The same slot with the non-translatable key copied into a locale fails, naming the key.
 *   3. A non-translatable key declared twice inside `values/` fails.
 *   4. A *translatable* key whose locale value happens to equal the default still passes. This is
 *      the discriminator: it proves the check keys off the attribute and not off "the value is the
 *      same in two files", which would fire on every untranslated-but-legal string.
 *
 * Expected to FAIL against the implementation before the guard existed.
 */
import { spawnSync } from 'node:child_process'
import { copyFileSync, mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import process from 'node:process'
import { fileURLToPath } from 'node:url'

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
// Point this at another revision to prove the legs can fail: a sensor that has never been observed
// failing is indistinguishable from one that always passes. Used as
// `PLAYKILN_CHECK_LOCALES=<old copy> node scripts/test-locale-dedup.mjs`.
const CHECKER = process.env.PLAYKILN_CHECK_LOCALES
  ? path.resolve(process.env.PLAYKILN_CHECK_LOCALES)
  : path.join(ROOT, 'scripts', 'check-locales.mjs')
const SANDBOX = mkdtempSync(path.join(tmpdir(), 'playkiln-locale-dedup-'))
const SLOT = path.join(SANDBOX, 'apps', 'fixture-slot')
const RES = path.join(SLOT, 'app', 'src', 'main', 'res')

const BASE = `<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="hello">Hello</string>
    <string name="brand" translatable="false">Mark Book for Teachers</string>
</resources>
`
const LOCALE_CLEAN = `<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="hello">Hallo</string>
</resources>
`
// Leg 2: the key that values/ declares non-translatable, copied into the locale anyway.
const LOCALE_DUPLICATE = `<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="hello">Hallo</string>
    <string name="brand" translatable="false">Mark Book for Teachers</string>
</resources>
`
// Leg 4: a *translatable* key whose value matches the default. Legal, and it must still pass.
const LOCALE_SAME_VALUE = `<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="hello">Hello</string>
</resources>
`
const BASE_DECLARED_TWICE = `<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="hello">Hello</string>
    <string name="brand" translatable="false">Mark Book for Teachers</string>
    <string name="brand" translatable="false">Mark Book for Teachers</string>
</resources>
`
const LOCALES_CONFIG = `<?xml version="1.0" encoding="utf-8"?>
<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
    <locale android:name="en" />
    <locale android:name="de" />
</locale-config>
`

function write(relative, contents) {
  const file = path.join(SANDBOX, relative)
  mkdirSync(path.dirname(file), { recursive: true })
  writeFileSync(file, contents, 'utf8')
}

function run() {
  const result = spawnSync(
    process.execPath,
    [path.join(SANDBOX, 'scripts', 'check-locales.mjs'), '--slot', 'fixture-slot'],
    { cwd: SANDBOX, encoding: 'utf8', windowsHide: true }
  )
  return { status: result.status, output: `${result.stdout || ''}${result.stderr || ''}` }
}

const rows = []
function leg(label, ok, detail) {
  rows.push({ label, ok, detail })
}

try {
  mkdirSync(path.join(SANDBOX, 'scripts'), { recursive: true })
  copyFileSync(CHECKER, path.join(SANDBOX, 'scripts', 'check-locales.mjs'))
  write('apps/fixture-slot/app/src/main/res/xml/locales_config.xml', LOCALES_CONFIG)
  write('apps/fixture-slot/app/src/main/res/values/strings.xml', BASE)
  write('apps/fixture-slot/app/src/main/res/values-de/strings.xml', LOCALE_CLEAN)

  const clean = run()
  leg(
    'a slot with no duplication passes',
    clean.status === 0,
    `exit ${clean.status}: ${clean.output.trim().split(/\r?\n/).slice(-1)[0] || ''}`
  )

  write('apps/fixture-slot/app/src/main/res/values-de/strings.xml', LOCALE_DUPLICATE)
  const duplicated = run()
  leg(
    'a non-translatable key copied into a locale fails and names the key',
    duplicated.status !== 0 && /redeclares 'brand'/.test(duplicated.output),
    `exit ${duplicated.status}; named=${/redeclares 'brand'/.test(duplicated.output)}`
  )

  write('apps/fixture-slot/app/src/main/res/values-de/strings.xml', LOCALE_CLEAN)
  write('apps/fixture-slot/app/src/main/res/values/strings.xml', BASE_DECLARED_TWICE)
  const twice = run()
  leg(
    'a non-translatable key declared twice in values/ fails',
    twice.status !== 0 && /declares a non-translatable key twice/.test(twice.output),
    `exit ${twice.status}; named=${/declares a non-translatable key twice/.test(twice.output)}`
  )

  write('apps/fixture-slot/app/src/main/res/values/strings.xml', BASE)
  write('apps/fixture-slot/app/src/main/res/values-de/strings.xml', LOCALE_SAME_VALUE)
  const sameValue = run()
  leg(
    'a translatable key equal to the default is not reported (the check keys off the attribute)',
    sameValue.status === 0,
    `exit ${sameValue.status}: ${sameValue.output.trim().split(/\r?\n/).slice(-1)[0] || ''}`
  )
} finally {
  rmSync(SANDBOX, { recursive: true, force: true })
}

for (const row of rows) {
  const status = row.ok ? 'PASS' : 'FAIL'
  console.log(`${status}     ${row.label} [${row.detail}]`)
}

const passed = rows.filter((row) => row.ok).length
console.log(`\nVERDICT: ${passed === rows.length ? 'PASS' : 'FAIL'}`)
process.exitCode = passed === rows.length ? 0 : 1
