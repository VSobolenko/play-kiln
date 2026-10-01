/**
 * Is the privacy policy reachable from inside the app, not only from the store page?
 *
 * Play requires the privacy policy in **two** places. The console says it in its own words on
 * `.../app-content/privacy-policy`:
 *
 *   "Следует разместить ссылку на политику конфиденциальности на странице приложения в Google Play
 *    и в самом приложении, даже если оно не предназначено для детей и не получает..."
 *
 * `AGENTS.md` has stated that rule for a while, and the factory shipped a slot without the second
 * half anyway: `apps/2026-09-26-teacher-gradebook` declares `privacyPolicyUrl` in its store pack and
 * renders privacy *prose* plus a support e-mail in `AboutScreen.kt`, but nothing in its Kotlin can
 * open the policy. The prose was right and nothing measured it — the same shape of defect as a rule
 * that lives only in a document, so the fix is a sensor rather than a better paragraph.
 *
 * The four claims, in the order they are checked
 * ---------------------------------------------
 *   1. The store pack declares a `privacyPolicyUrl`. A pack that declares none has nothing to mirror
 *      and is reported as inapplicable — not as a pass, and not as a failure. `scripts/store/render.py`
 *      owns whether a pack must carry one.
 *   2. That exact URL is baked into the app as a **default-locale string resource** under `values/`.
 *      A URL typed into Kotlin fails, because a translated app cannot localise an address and a
 *      literal is invisible to `check-locales.mjs`. A URL only in `values-<locale>/` fails, because
 *      then the app's policy address depends on which language the phone speaks.
 *   3. The resource that holds it is **referenced from Kotlin**, so it is used rather than declared
 *      and forgotten.
 *   4. The same Kotlin file that references it can open a URI — `Intent(ACTION_VIEW)`,
 *      `CustomTabsIntent`, or Compose's `LocalUriHandler`. The reference and the opener must be in one
 *      file: an opener somewhere else in the app is not evidence that *this* string reaches the user.
 *
 * What this cannot prove, and does not claim: that the control is reachable, visible or well
 * labelled, that a device with no browser is handled at runtime, or that the policy text satisfies a
 * reviewer. Those need the app on a device. This sensor exists to make the *missing half*
 * unshippable, which is the defect that actually occurred.
 *
 * This module is pure: it reads files under a slot directory it is given and returns data. It never
 * resolves a slot, prints, or exits, so `scripts/test-in-app-policy.mjs` can drive it against
 * throwaway sandboxes and a CLI failure and a test failure cannot drift apart.
 */
import fs from 'node:fs'
import path from 'node:path'

/**
 * The ways this app family opens a web URL, named as code so a reader can see exactly what is accepted
 * rather than trusting a prose description of it.
 *
 * The Compose case is two patterns rather than one, and that is a correction this suite's own first
 * run produced. `LocalUriHandler.current` is one idiomatic form; the other is a screen that receives a
 * `UriHandler` and calls `openUri` on it — hoisting the handler is the recommended Compose shape, and
 * the first version of this list only recognised the first form, so it failed a correct
 * implementation. `openUri` is not a platform method: it belongs to Compose's `UriHandler` alone (and
 * to the test-only `TestUriHandler` double), so matching the call is specific rather than loose.
 */
export const OPENERS = Object.freeze([
  { id: 'Intent.ACTION_VIEW', pattern: /\bIntent\s*\.\s*ACTION_VIEW\b/ },
  { id: 'CustomTabsIntent', pattern: /\bCustomTabsIntent\b/ },
  { id: 'UriHandler.openUri', pattern: /\bLocalUriHandler\b|\.\s*openUri\s*\(/ }
])

/**
 * Hosts reserved for documentation by RFC 2606 / RFC 6761.
 *
 * A pack whose policy URL still points at one of these has no hosted policy yet: the pack renderer
 * (`scripts/store/render.py`) seeds every new `listing.json` with `https://example.com/privacy` and
 * reports such a value as *unfinished* in the generated human-checks list. So a placeholder is not a
 * policy the app can be required to mirror, and this module answers `UNVERIFIED` rather than `FAIL`
 * for one — which also means it can never be read as green.
 *
 * The Python half of the factory holds the same list — `scripts/store/model.py` reads
 * `placeholderHosts` from `scripts/lib/play-assets.json`, the one source of truth for every Play
 * asset rule, and `scripts/store/render.py` and `validate.py` use it. Two holders of one list is the
 * shape that has bitten this factory before, so the agreement between this literal and the JSON
 * **must** be asserted; since `scripts/selftest.mjs` was deleted (2026-09-30) nothing asserts it any
 * more. That is a live hole, recorded as a finding in
 * `openspec/changes/reduce-factory-to-core/deletions.md` §7 rather than trusted: adding a host here
 * and not in the JSON — or the reverse — is now a silent divergence.
 */
export const PLACEHOLDER_HOSTS = Object.freeze([
  'example.com',
  'example.org',
  'example.net',
  'example.edu',
  'localhost',
  'invalid'
])

/**
 * True when a URL cannot yet be a real hosted policy: a reserved documentation host, an unparseable
 * address, or a scheme that is not http(s). Mirrors `scripts/store/render.py`'s own predicate, including
 * its treatment of a non-https scheme as unfinished.
 */
export function isPlaceholderPolicyUrl(url) {
  let parsed
  try {
    parsed = new URL(url)
  } catch {
    return true
  }
  if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') return true
  const host = parsed.hostname.toLowerCase()
  return PLACEHOLDER_HOSTS.some((reserved) => host === reserved || host.endsWith(`.${reserved}`))
}

function readText(file) {
  try {
    return fs.readFileSync(file, 'utf8')
  } catch {
    return null
  }
}

/** Every `.kt` file under `app/src/main/java`, which is where this scaffold keeps its Kotlin. */
export function kotlinFiles(slotDir) {
  const base = path.join(slotDir, 'app', 'src', 'main', 'java')
  const found = []
  const walk = (dir) => {
    if (!fs.existsSync(dir)) return
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
      const full = path.join(dir, entry.name)
      if (entry.isDirectory()) walk(full)
      else if (entry.name.endsWith('.kt')) found.push(full)
    }
  }
  walk(base)
  return found
}

/**
 * The `<string>` elements of the **default locale only**: `res/values/*.xml`, never `values-<locale>`.
 * A URL is not translatable, so its home is `values/`; a copy under `values-ru/` is a second source of
 * truth for the same address.
 */
export function defaultLocaleStrings(slotDir) {
  const valuesDir = path.join(slotDir, 'app', 'src', 'main', 'res', 'values')
  const entries = []
  if (!fs.existsSync(valuesDir)) return entries
  for (const name of fs.readdirSync(valuesDir)) {
    if (!name.endsWith('.xml')) continue
    const text = readText(path.join(valuesDir, name))
    if (text === null) continue
    for (const match of text.matchAll(/<string\b([^>]*)>([\s\S]*?)<\/string>/g)) {
      const nameMatch = match[1].match(/\bname\s*=\s*"([^"]+)"/)
      if (!nameMatch) continue
      entries.push({ file: `app/src/main/res/values/${name}`, id: nameMatch[1], text: match[2] })
    }
  }
  return entries
}

/** Where else the URL is declared, so a duplicated address is reported even though it is not fatal. */
export function otherLocaleDeclarations(slotDir, url) {
  const resDir = path.join(slotDir, 'app', 'src', 'main', 'res')
  const found = []
  if (!fs.existsSync(resDir)) return found
  for (const entry of fs.readdirSync(resDir, { withFileTypes: true })) {
    if (!entry.isDirectory() || !entry.name.startsWith('values-')) continue
    for (const name of fs.readdirSync(path.join(resDir, entry.name))) {
      if (!name.endsWith('.xml')) continue
      const text = readText(path.join(resDir, entry.name, name))
      if (text && text.includes(url)) found.push(`${entry.name}/${name}`)
    }
  }
  return found
}

/**
 * The verdict for one slot. `root` is only for shortening paths in messages; it defaults to the slot's
 * own parent-parent, so a sandbox prints sandbox-relative paths.
 *
 * Returns `{ verdict, rows, notes, url }` where `verdict` is `PASS`, `FAIL` or `UNVERIFIED`. `rows`
 * carry `status` and `message` exactly as the CLI prints them.
 */
export function evaluateInAppPolicy(slotDir, { root = path.dirname(path.dirname(slotDir)) } = {}) {
  const relative = (absolute) => path.relative(root, absolute).split(path.sep).join('/')
  const listingPath = path.join(slotDir, 'store', 'listing.json')
  const listingText = readText(listingPath)

  if (listingText === null) {
    return {
      verdict: 'UNVERIFIED',
      url: '',
      rows: [{ status: 'UNVERIFIED', message: 'no store/listing.json, so there is no declared privacy-policy URL to mirror' }],
      notes: []
    }
  }

  let listing
  try {
    listing = JSON.parse(listingText)
  } catch (error) {
    return {
      verdict: 'UNVERIFIED',
      url: '',
      rows: [{ status: 'UNVERIFIED', message: `store/listing.json is not parseable: ${error.message}` }],
      notes: []
    }
  }

  const url = typeof listing.privacyPolicyUrl === 'string' ? listing.privacyPolicyUrl.trim() : ''
  if (url === '') {
    return {
      verdict: 'PASS',
      url: '',
      rows: [{ status: 'PASS', message: 'the pack declares no privacyPolicyUrl, so there is no policy to reach from inside the app' }],
      notes: []
    }
  }

  // A placeholder address is not a policy yet. This branch exists because the factory's own scaffold
  // tripped the check: `scripts/store/render.py` seeds every new pack with `https://example.com/privacy`, so every
  // freshly scaffolded slot suddenly had a "policy" the app could not open. Failing there would have
  // been wrong twice over — the pack is unfinished rather than broken, and `scripts/store/render.py` already says
  // so. `UNVERIFIED` keeps the case out of both green and red, which is the honest verdict for it.
  if (isPlaceholderPolicyUrl(url)) {
    return {
      verdict: 'UNVERIFIED',
      url,
      rows: [
        {
          status: 'UNVERIFIED',
          message: `the pack's privacyPolicyUrl is still a placeholder (${url}), so there is no hosted policy for the app to open yet; scripts/store/render.py reports this pack unfinished`
        },
        { status: 'NOTE', message: 'a real hosted HTTPS URL in store/listing.json makes this check decide, and then the app must be able to open it' }
      ],
      notes: []
    }
  }

  const holders = defaultLocaleStrings(slotDir).filter((entry) => entry.text.includes(url))
  const kotlin = kotlinFiles(slotDir).map((file) => ({ file, text: readText(file) || '' }))

  // (2) The URL must be a default-locale resource — not a Kotlin literal, and not only a store field.
  if (holders.length === 0) {
    const hardcoded = kotlin.filter((entry) => entry.text.includes(url))
    const rows = hardcoded.length > 0
      ? [
          { status: 'FAIL', message: 'the privacy-policy URL is hardcoded in Kotlin instead of a `translatable="false"` string in res/values/' },
          ...hardcoded.map((entry) => ({ status: 'NOTE', message: relative(entry.file) }))
        ]
      : [
          { status: 'FAIL', message: 'the privacy-policy URL is declared in the store pack but nowhere in the app, so the app cannot open the policy' },
          { status: 'NOTE', message: `expected the exact URL in app/src/main/res/values/*.xml: ${url}` }
        ]
    return { verdict: 'FAIL', url, rows, notes: [] }
  }

  // (3) The holder must be referenced from Kotlin.
  const referenced = new Set()
  const wired = []
  for (const entry of kotlin) {
    const names = holders.filter((holder) => entry.text.includes(`R.string.${holder.id}`)).map((holder) => holder.id)
    if (names.length === 0) continue
    for (const id of names) referenced.add(id)
    const opener = OPENERS.find((candidate) => candidate.pattern.test(entry.text))
    if (opener) wired.push({ file: relative(entry.file), opener: opener.id, ids: names })
  }

  const unreferenced = holders.filter((holder) => !referenced.has(holder.id))
  if (unreferenced.length > 0) {
    return {
      verdict: 'FAIL',
      url,
      rows: [
        { status: 'FAIL', message: 'a string holding the privacy-policy URL is never referenced from Kotlin, so the app declares it and never opens it' },
        ...unreferenced.map((holder) => ({ status: 'NOTE', message: `${holder.file} → R.string.${holder.id}` }))
      ],
      notes: []
    }
  }

  // (4) The referencing file itself must be able to open a URI.
  if (wired.length === 0) {
    const anyOpener = kotlin.some((entry) => OPENERS.some((candidate) => candidate.pattern.test(entry.text)))
    return {
      verdict: 'FAIL',
      url,
      rows: [
        {
          status: 'FAIL',
          message: anyOpener
            ? 'the privacy-policy URL is referenced from Kotlin, but not in a file that can open a URI (`Intent.ACTION_VIEW`, `CustomTabsIntent` or `LocalUriHandler`)'
            : 'nothing in the app can open a URI, so the privacy-policy link cannot be opened by the user'
        },
        ...holders.map((holder) => ({ status: 'NOTE', message: `${holder.file} → R.string.${holder.id}` }))
      ],
      notes: []
    }
  }

  const notes = []
  const duplicates = otherLocaleDeclarations(slotDir, url)
  if (duplicates.length > 0) {
    // Reported, not failed: `check-locales.mjs` owns string parity, and a URL carried inside a
    // translated sentence is a drift risk rather than a broken state.
    notes.push(`the same URL is also declared outside the default locale: ${duplicates.join(', ')}`)
  }

  return {
    verdict: 'PASS',
    url,
    rows: [
      ...wired.map((entry) => ({
        status: 'PASS',
        message: `${entry.file} opens the policy declared in the store pack (${entry.opener}, ${entry.ids.map((id) => `R.string.${id}`).join(', ')})`
      })),
      { status: 'PASS', message: `the privacy-policy URL is a default-locale resource and the app can open it (${holders.map((holder) => `R.string.${holder.id}`).join(', ')})` }
    ],
    notes
  }
}
