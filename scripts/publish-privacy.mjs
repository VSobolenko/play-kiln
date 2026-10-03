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
 * Publishing is an **outward act** — it writes a public page and rewrites `store/listing.json`, which
 * is an authored pack input — so nothing here happens on a bare invocation. One slot is named with
 * `--slot`; the fleet is named with `--all`. `--all` exists because the owner's instruction is that
 * *every* app gets a real policy and nobody is asked again (`.cursor/rules/privacy-policy.mdc`; the
 * gate is `pack.placeholders` in `scripts/core/assets.mjs`), and because doing that one slot at a time
 * is how a fleet drifts. What `--all` publishes is the same question the gate answers: a slot whose
 * recorded URL is not yet a real hosted policy. The field has no baseline — the gate FAILs it
 * unconditionally — so the batch selects every slot carrying the defect, and `--slot <slug>` is the
 * single-slot door. `--dry-run` reports the batch without a network call.
 *
 * Usage:
 *   node scripts/publish-privacy.mjs --slot <slug>
 *   node scripts/publish-privacy.mjs --slot <slug> --check    # verify the recorded URL, publish nothing
 *   node scripts/publish-privacy.mjs --all [--check] [--dry-run]
 */
import { existsSync, readFileSync, readdirSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import process from 'node:process'
import { fileURLToPath } from 'node:url'
import { policyUrlProblem } from './lib/in-app-policy.mjs'

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const API = 'https://api.telegra.ph'

/**
 * A refusal this script must not swallow. `code` is the process exit for a single-slot run: 2 for a
 * block (nothing was published), 1 for a check that ran and found the recorded URL is not readable. A
 * batch run catches one per slot, so a single broken slot cannot hide the verdicts of the rest, and
 * still exits with the worst code it saw.
 */
class PrivacyFailure extends Error {
  constructor(message, code = 2) {
    super(message)
    this.code = code
  }
}

function fail(message, code = 2) {
  throw new PrivacyFailure(message, code)
}

function argument(name) {
  const index = process.argv.indexOf(name)
  return index === -1 ? null : process.argv[index + 1]
}

const USAGE = [
  'usage: node scripts/publish-privacy.mjs --slot <slug> [--check]',
  '       node scripts/publish-privacy.mjs --all [--check] [--dry-run]',
  '',
  '  --slot <slug>  publish one slot\'s store/privacy.md and record the URL (or, with --check,',
  '                 verify the URL already recorded, publishing nothing)',
  '  --all          every slot whose recorded URL is not yet a real hosted policy, because',
  '                 pack.placeholders fails the field with no baseline; --slot <slug> names one',
  '  --check        verify recorded URLs from the outside and publish nothing',
  '  --dry-run      with --all, print what would be published without any network call',
  '  --help         this text',
  '',
  'A bare invocation publishes nothing: it prints this text and exits 2.'
].join('\n')

const checkOnly = process.argv.includes('--check')
const dryRun = process.argv.includes('--dry-run')
const all = process.argv.includes('--all')
const help = process.argv.includes('--help') || process.argv.includes('-h')

/** The slot directory a slug names, refusing an ambiguous match. Requires a store/listing.json. */
function resolveSlotDir(slug) {
  const apps = path.join(ROOT, 'apps')
  const matches = readdirSync(apps).filter(
    (name) =>
      (name === slug || name.endsWith(`-${slug}`)) &&
      existsSync(path.join(apps, name, 'store', 'listing.json'))
  )
  if (matches.length !== 1) fail(`expected one slot matching '${slug}', found ${matches.length}`)
  return path.join(apps, matches[0])
}

/** Every directory under apps/, in a stable order. */
function allSlotDirs() {
  return readdirSync(path.join(ROOT, 'apps'), { withFileTypes: true })
    .filter((entry) => entry.isDirectory())
    .map((entry) => path.join(ROOT, 'apps', entry.name))
    .sort()
}

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
function recordedToken(envPath) {
  if (!existsSync(envPath)) return null
  const match = readFileSync(envPath, 'utf8').match(/^TELEGRAPH_ACCESS_TOKEN=(.+)$/m)
  return match ? match[1].trim() : null
}

function recordToken(envPath, token) {
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

/**
 * Publish one slot's policy page and record its URL. Names the slot in every line, because a batch
 * run's output is read as one log and an unattributed `PASS` tells the reader nothing.
 */
async function publishOne(slotDir) {
  const slug = path.basename(slotDir)
  const listingPath = path.join(slotDir, 'store', 'listing.json')
  const privacyPath = path.join(slotDir, 'store', 'privacy.md')
  const envPath = path.join(slotDir, '.env')

  if (!existsSync(privacyPath)) {
    fail(`no store/privacy.md in ${slug}, so there is no policy to publish`)
  }

  const listing = JSON.parse(readFileSync(listingPath, 'utf8'))
  // The listing keeps the human title in the locale block, not at the top level, so reading
  // `listing.title` silently produced a page named after the internal slot slug.
  const locale = listing.locales?.[listing.defaultLocale] || Object.values(listing.locales || {})[0] || {}
  const pageName = listing.appName || locale.title || slug
  const existing = String(listing.privacyPolicyUrl || '')
  const title = `${pageName} — privacy policy`

  const markdown = readFileSync(privacyPath, 'utf8')
  const content = blocks(markdown)
  if (content.length < 10) {
    fail(`converted only ${content.length} block(s) from store/privacy.md - refusing to publish a stub`)
  }

  if (dryRun) {
    console.log(`DRY-RUN     ${slug}: would render ${content.length} block(s) to ${API} and record the URL`)
    return existing || '(new page)'
  }

  let token = recordedToken(envPath)
  if (!token) {
    const account = await call('createAccount', {
      short_name: String(pageName).slice(0, 32),
      author_name: String(listing.contactEmail || '')
    })
    token = account.access_token
    recordToken(envPath, token)
    console.log(`PASS        ${slug}: created a publishing account (token stored in the slot's .env, not printed)`)
  } else {
    console.log(`PASS        ${slug}: reusing the publishing account already recorded in the slot's .env`)
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
    console.log(`PASS        ${slug}: updated the existing page`)
  } else {
    const page = await call('createPage', {
      access_token: token,
      title,
      content,
      author_name: String(listing.contactEmail || ''),
      return_content: false
    })
    url = page.url
    console.log(`PASS        ${slug}: created the page`)
  }

  // Verify from the outside before recording it: the API's "ok" says the write succeeded, not that a
  // reviewer can open the page.
  const { status, body } = await reachable(url)
  if (status !== 200 || !body.toLowerCase().includes('privacy')) {
    fail(`the published page for ${slug} is not readable (HTTP ${status}) - not recording it in the listing`)
  }

  listing.privacyPolicyUrl = url
  writeFileSync(listingPath, `${JSON.stringify(listing, null, 2)}\n`)
  console.log(`PASS        ${slug}: verified reachable (HTTP ${status}, ${body.length} bytes)`)
  console.log(`\nprivacy policy URL: ${url}`)
  console.log('recorded in store/listing.json — the listing is part of the approved pack, so any recorded release approval is now void: re-record it with scripts/approve-release.mjs before releasing')
  return url
}

/**
 * Verify one slot's recorded URL from the outside. Byte-identical output to the original
 * single-slot `--check`, including its refusal of a non-https field: 2 blocks, 1 reports a page that
 * is not readable, 0 passes.
 */
async function checkOne(slotDir) {
  const listing = JSON.parse(readFileSync(path.join(slotDir, 'store', 'listing.json'), 'utf8'))
  const existing = String(listing.privacyPolicyUrl || '')
  if (!/^https:\/\//.test(existing)) fail(`privacyPolicyUrl is not an https URL: ${existing}`)
  const { status, body } = await reachable(existing)
  console.log(`URL:  ${existing}\nHTTP: ${status}\nbytes: ${body.length}`)
  const marker = 'privacy'
  console.log(`mentions the policy: ${body.toLowerCase().includes(marker)}`)
  return { ok: status === 200 && body.toLowerCase().includes(marker), code: status === 200 && body.toLowerCase().includes(marker) ? 0 : 1 }
}

/**
 * The `--all --check` sweep: the recorded URL's shape first (so a placeholder is named rather than
 * fetched), then reachability. Never throws for a bad URL — the caller collects the worst code.
 */
async function sweepOne(slotDir) {
  const slug = path.basename(slotDir)
  let listing
  try {
    listing = JSON.parse(readFileSync(path.join(slotDir, 'store', 'listing.json'), 'utf8'))
  } catch (error) {
    console.log(`FAIL        ${slug}: store/listing.json is not parseable: ${error.message}`)
    return { ok: false, code: 2 }
  }
  const problem = policyUrlProblem(listing.privacyPolicyUrl)
  if (problem) {
    console.log(`FAIL        ${slug}: ${problem}`)
    return { ok: false, code: 1 }
  }
  const url = String(listing.privacyPolicyUrl)
  const { status, body } = await reachable(url)
  const mentions = body.toLowerCase().includes('privacy')
  const ok = status === 200 && mentions
  console.log(`${ok ? 'PASS       ' : 'FAIL       '} ${slug}: HTTP ${status}, ${body.length} bytes, mentions the policy: ${mentions}`)
  console.log(`            ${url}`)
  return { ok, code: ok ? 0 : 1 }
}

async function main() {
  if (help) {
    console.log(USAGE)
    return 0
  }
  const slug = argument('--slot')
  if (!slug && !all) {
    console.error(USAGE)
    console.error('refusing to run: pass --slot <slug> or --all; a bare invocation publishes nothing')
    return 2
  }
  if (slug && all) {
    console.error(USAGE)
    console.error('refusing to run: --slot and --all are alternatives')
    return 2
  }

  if (slug) {
    const slotDir = resolveSlotDir(slug)
    if (checkOnly) return (await checkOne(slotDir)).code
    await publishOne(slotDir)
    return 0
  }

  let worst = 0
  let published = 0
  let skipped = 0
  for (const slotDir of allSlotDirs()) {
    const name = path.basename(slotDir)
    const listingPath = path.join(slotDir, 'store', 'listing.json')
    const privacyPath = path.join(slotDir, 'store', 'privacy.md')
    if (!existsSync(listingPath)) {
      skipped += 1
      console.log(`SKIP        ${name}: no store/listing.json`)
      continue
    }
    if (!existsSync(privacyPath)) {
      skipped += 1
      console.log(`SKIP        ${name}: no store/privacy.md, so there is no policy to publish`)
      continue
    }
    if (checkOnly) {
      try {
        const result = await sweepOne(slotDir)
        worst = Math.max(worst, result.code)
      } catch (error) {
        console.log(`FAIL        ${name}: ${error.message}`)
        worst = Math.max(worst, error instanceof PrivacyFailure ? error.code : 2)
      }
      continue
    }
    let listing
    try {
      listing = JSON.parse(readFileSync(listingPath, 'utf8'))
    } catch (error) {
      console.log(`FAIL        ${name}: store/listing.json is not parseable: ${error.message}`)
      worst = Math.max(worst, 2)
      continue
    }
    const problem = policyUrlProblem(listing.privacyPolicyUrl)
    if (!problem) {
      skipped += 1
      console.log(`SKIP        ${name}: already a real hosted policy (${listing.privacyPolicyUrl})`)
      continue
    }
    try {
      await publishOne(slotDir)
      published += 1
    } catch (error) {
      console.log(`FAIL        ${name}: ${error.message}`)
      worst = Math.max(worst, error instanceof PrivacyFailure ? error.code : 2)
    }
  }
  console.log(`\n${published} published, ${skipped} skipped, exit ${worst}`)
  return worst
}

main()
  .then((code) => {
    if (code) process.exitCode = code
  })
  .catch((error) => {
    if (error instanceof PrivacyFailure) {
      console.error(`BLOCKED privacy - ${error.message}`)
      process.exitCode = error.code
    } else {
      console.error(`BLOCKED privacy - ${error?.stack || error}`)
      process.exitCode = 2
    }
  })
