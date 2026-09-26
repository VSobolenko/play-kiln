#!/usr/bin/env node
/**
 * Give a slot a real, publicly reachable privacy-policy URL.
 *
 * Google Play requires a privacy policy URL that anyone can open, and the factory's first two slots
 * both pointed at `https://example.com/privacy` — a placeholder that is not a policy, not reachable
 * as one, and that a reviewer would fail. The store pack validates the shape of the listing and
 * never the reachability of that URL, so the placeholder passed every gate.
 *
 * The owner of this repo has no hosted site and no server, so the policy is published to
 * **Telegraph** (`telegra.ph`), a free, anonymous, HTTPS publishing service: it needs no account, no
 * domain and no deployment step, it returns a permanent public URL, and a token created once lets
 * the page be updated in place instead of accumulating a new URL on every edit. The reachability of
 * the result is verified here rather than assumed, because "the API returned ok" is not evidence that
 * a reviewer can read the page.
 *
 * The page is generated *from* `store/privacy.md`, so the published policy and the committed file
 * cannot drift: re-running after an edit updates the same URL.
 *
 * Usage:
 *   node scripts/publish-privacy.mjs --slot <slug>
 *   node scripts/publish-privacy.mjs --slot <slug> --check    # verify the recorded URL, publish nothing
 */
import { existsSync, readFileSync, readdirSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import process from 'node:process'
import { fileURLToPath } from 'node:url'

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const API = 'https://api.telegra.ph'

function fail(message) {
  console.error(`BLOCKED privacy - ${message}`)
  process.exit(2)
}

function argument(name) {
  const index = process.argv.indexOf(name)
  return index === -1 ? null : process.argv[index + 1]
}

const slug = argument('--slot')
if (!slug) fail('usage: node scripts/publish-privacy.mjs --slot <slug> [--check]')
const checkOnly = process.argv.includes('--check')

const slotDir = (() => {
  const apps = path.join(ROOT, 'apps')
  const matches = readdirSync(apps).filter(
    (name) =>
      (name === slug || name.endsWith(`-${slug}`)) &&
      existsSync(path.join(apps, name, 'store', 'listing.json'))
  )
  if (matches.length !== 1) fail(`expected one slot matching '${slug}', found ${matches.length}`)
  return path.join(apps, matches[0])
})()

const listingPath = path.join(slotDir, 'store', 'listing.json')
const privacyPath = path.join(slotDir, 'store', 'privacy.md')
const envPath = path.join(slotDir, '.env')

/** Inline markdown -> Telegraph inline nodes. */
function inline(text) {
  const nodes = []
  const pattern = /\[([^\]]+)\]\(([^)]+)\)|\*\*([^*]+)\*\*|`([^`]+)`/g
  let cursor = 0
  for (const match of text.matchAll(pattern)) {
    if (match.index > cursor) nodes.push(text.slice(cursor, match.index))
    if (match[1]) nodes.push({ tag: 'a', attrs: { href: match[2] }, children: [match[1]] })
    else if (match[3]) nodes.push({ tag: 'strong', children: [match[3]] })
    else nodes.push({ tag: 'code', children: [match[4]] })
    cursor = match.index + match[0].length
  }
  if (cursor < text.length) nodes.push(text.slice(cursor))
  return nodes.filter((node) => node !== '')
}

/** The policy markdown -> Telegraph block nodes. Tables become definition-like paragraphs, because
 * Telegraph has no table tag and dropping the data would lose the disclosures that matter most. */
function blocks(markdown) {
  const out = []
  const lines = markdown.split(/\r?\n/)
  let index = 0
  while (index < lines.length) {
    const line = lines[index]
    const trimmed = line.trim()
    if (trimmed === '') {
      index += 1
      continue
    }
    if (/^---+$/.test(trimmed)) {
      out.push({ tag: 'hr' })
      index += 1
      continue
    }
    const heading = trimmed.match(/^(#{1,6})\s+(.*)$/)
    if (heading) {
      out.push({ tag: heading[1].length <= 2 ? 'h3' : 'h4', children: inline(heading[2]) })
      index += 1
      continue
    }
    // A table: consume the whole block, keep every cell.
    if (trimmed.startsWith('|')) {
      const rows = []
      while (index < lines.length && lines[index].trim().startsWith('|')) {
        const cells = lines[index]
          .trim()
          .replace(/^\||\|$/g, '')
          .split('|')
          .map((cell) => cell.trim())
        if (!/^:?-{2,}:?$/.test(cells[0])) rows.push(cells)
        index += 1
      }
      const [header, ...body] = rows
      for (const row of body) {
        const children = []
        row.forEach((cell, position) => {
          if (position > 0) children.push(' — ')
          if (header?.[position]) children.push({ tag: 'strong', children: [header[position]] }, ': ')
          children.push(...inline(cell))
        })
        out.push({ tag: 'p', children })
      }
      continue
    }
    if (/^[-*]\s+/.test(trimmed)) {
      const items = []
      while (index < lines.length && /^[-*]\s+/.test(lines[index].trim())) {
        items.push({ tag: 'li', children: inline(lines[index].trim().replace(/^[-*]\s+/, '')) })
        index += 1
      }
      out.push({ tag: 'ul', children: items })
      continue
    }
    if (/^\d+\.\s+/.test(trimmed)) {
      const items = []
      while (index < lines.length && /^\d+\.\s+/.test(lines[index].trim())) {
        items.push({ tag: 'li', children: inline(lines[index].trim().replace(/^\d+\.\s+/, '')) })
        index += 1
      }
      out.push({ tag: 'ol', children: items })
      continue
    }
    const paragraph = [trimmed]
    index += 1
    while (
      index < lines.length &&
      lines[index].trim() !== '' &&
      !/^(#{1,6}\s|[-*]\s|\d+\.\s|\||---+$)/.test(lines[index].trim())
    ) {
      paragraph.push(lines[index].trim())
      index += 1
    }
    out.push({ tag: 'p', children: inline(paragraph.join(' ')) })
  }
  return out
}

/** Read the token recorded for this slot, if any. Never printed. */
function recordedToken() {
  if (!existsSync(envPath)) return null
  const match = readFileSync(envPath, 'utf8').match(/^TELEGRAPH_ACCESS_TOKEN=(.+)$/m)
  return match ? match[1].trim() : null
}

function recordToken(token) {
  const line = `TELEGRAPH_ACCESS_TOKEN=${token}\n`
  const existing = existsSync(envPath) ? readFileSync(envPath, 'utf8') : ''
  if (/^TELEGRAPH_ACCESS_TOKEN=/m.test(existing)) return
  writeFileSync(envPath, `${existing}${existing.endsWith('\n') || existing === '' ? '' : '\n'}${line}`)
}

async function call(method, params) {
  const response = await fetch(`${API}/${method}`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(params)
  })
  const payload = await response.json().catch(() => null)
  if (!payload || payload.ok !== true) {
    fail(`${method} refused: ${JSON.stringify(payload?.error || payload || response.status)}`)
  }
  return payload.result
}

async function reachable(url) {
  const response = await fetch(url, { redirect: 'follow' })
  const body = await response.text()
  return { status: response.status, body }
}

const listing = JSON.parse(readFileSync(listingPath, 'utf8'))
// The listing keeps the human title in the locale block, not at the top level, so reading
// `listing.title` silently produced a page named after the internal slot slug.
const locale = listing.locales?.[listing.defaultLocale] || Object.values(listing.locales || {})[0] || {}
const pageName = listing.appName || locale.title || slug
const existing = String(listing.privacyPolicyUrl || '')
const title = `${pageName} — privacy policy`

if (checkOnly) {
  if (!/^https:\/\//.test(existing)) fail(`privacyPolicyUrl is not an https URL: ${existing}`)
  const { status, body } = await reachable(existing)
  console.log(`URL:  ${existing}\nHTTP: ${status}\nbytes: ${body.length}`)
  const marker = 'privacy'
  console.log(`mentions the policy: ${body.toLowerCase().includes(marker)}`)
  process.exit(status === 200 && body.toLowerCase().includes(marker) ? 0 : 1)
}

const markdown = readFileSync(privacyPath, 'utf8')
const content = blocks(markdown)
if (content.length < 10) fail(`converted only ${content.length} block(s) from store/privacy.md - refusing to publish a stub`)

let token = recordedToken()
if (!token) {
  const account = await call('createAccount', {
    short_name: String(pageName).slice(0, 32),
    author_name: String(listing.contactEmail || '')
  })
  token = account.access_token
  recordToken(token)
  console.log(`PASS        created a publishing account (token stored in the slot's .env, not printed)`)
} else {
  console.log(`PASS        reusing the publishing account already recorded in the slot's .env`)
}

// Reuse the existing page when the recorded URL is one of ours, so an edit updates the URL a
// reviewer may already have seen instead of replacing it.
const existingPath = (existing.match(/^https:\/\/telegra\.ph\/(.+)$/) || [])[1] || null
let url
if (existingPath) {
  const page = await call(`editPage/${existingPath}`, {
    access_token: token,
    title,
    content,
    author_name: String(listing.contactEmail || ''),
    return_content: false
  })
  url = page.url
  console.log(`PASS        updated the existing page`)
} else {
  const page = await call('createPage', {
    access_token: token,
    title,
    content,
    author_name: String(listing.contactEmail || ''),
    return_content: false
  })
  url = page.url
  console.log(`PASS        created the page`)
}

// Verify from the outside before recording it: the API's "ok" says the write succeeded, not that a
// reviewer can open the page.
const { status, body } = await reachable(url)
if (status !== 200 || !body.toLowerCase().includes('privacy')) {
  fail(`the published page is not readable (HTTP ${status}) - not recording it in the listing`)
}

listing.privacyPolicyUrl = url
writeFileSync(listingPath, `${JSON.stringify(listing, null, 2)}\n`)
console.log(`PASS        verified reachable (HTTP ${status}, ${body.length} bytes)`)
console.log(`\nprivacy policy URL: ${url}`)
console.log(`recorded in store/listing.json (store/ is outside the release-approval digest, so this does not void the approval)`)
