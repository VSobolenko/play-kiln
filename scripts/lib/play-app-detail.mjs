/**
 * Play app detail — the per-app facts a ratings-only scan cannot see.
 *
 * Why this exists
 * ---------------
 * `play-scan.mjs` reads a search results page, which carries no install count and no
 * monetisation. That leaves two questions the niche screen in `docs/niche-selection.md` asks and
 * cannot answer from the result list alone:
 *
 * - **How big is the incumbent?** A 4.4★ app with 50K installs and a 4.4★ app with 5M installs
 *   look identical in a result card. `docs/niche-selection.md` §3 step 4 resolves this by opening
 *   the listing; this module does that reading.
 * - **Does the incumbent monetise?** The "Contains ads · In-app purchases" badges are the only
 *   on-store evidence of a working business model, and they are not in the result card at all.
 *
 * The rating on an incumbent is a signal about *demand*; the install count is a signal about its
 * *size*, and the badges are a signal about whether anyone pays. All three are needed before a
 * pocket is judged reachable, which is why this is a separate module rather than a change to the
 * scanner (that scanner is validated against hand-read verdicts; it is not touched).
 *
 * Scope and honesty
 * -----------------
 * - Signed-out, geo-parameterised: one storefront, `gl`/`hl` chosen by the caller.
 * - Server-rendered HTML only. Fields Play ships in a deferred JSON blob are deliberately not
 *   parsed: an undocumented internal payload changes without warning, while the microdata below
 *   is the same markup the page renders to a human.
 * - **Absence is information.** A missing rating means the app is genuinely unrated (which is the
 *   `dead-zone` signal), and a missing ad badge means Play declares no ads. Neither is ever
 *   defaulted to a plausible value: a default would silently destroy the signal the screen
 *   exists to read.
 * - Anything whose anchor is not found while the page is otherwise intact is reported as `null`
 *   **and** named in `unparsed`, so a structure change is visible instead of masquerading as a
 *   real "no ads" or a real "unrated".
 *
 * Field availability, as measured on real pages
 * --------------------------------------------
 * Verified 2026-09-23 against `com.cube.arc.fa`, `it.Ettore.calcolielettrici`,
 * `net.dinglisch.android.taskerm` and `com.pando.bjjtraininglog`:
 *
 * | Field | Server-rendered marker | Reliable |
 * |---|---|---|
 * | title | `<h1><span itemprop="name">` | yes |
 * | developer | `<div class="Vbfug ..."><a ...><span>` in the header | yes |
 * | rating | `aria-label="Rated X stars out of five stars"` inside `itemprop="starRating"` | yes |
 * | ratingCount | `N reviews` immediately after the star rating block | yes — but Play rounds it (see below) |
 * | installs | `ClM7O` div paired with the `Downloads` label | yes |
 * | updated | `Updated on` label + `xg1aie` date | yes |
 * | free / price | `<meta itemprop="price" content="...">` in the app's own offer | yes |
 * | containsAds | `UIuSk` badge text in the header | yes, when the badge is declared |
 * | inAppPurchases | `UIuSk` badge text in the header | yes, when the badge is declared |
 *
 * Two caveats that are recorded rather than hidden:
 * - Play shows `1M+`, `5M+`, `11.2K` — rounded magnitudes. `ratingCount` is therefore an integer
 *   derived from the *displayed* number (11.2K -> 11200), because the displayed number is what the
 *   store actually shows. `ratingCountExact` is the unrounded schema.org twin (11176 for the same
 *   page), served in `<script type="application/ld+json" nonce="...">`. Note the `nonce`: a regex
 *   demanding a bare `<script type="application/ld+json">` matches nothing on any page, which is
 *   how the exact count first read as permanently null. An unrated app still ships the JSON-LD
 *   block but without `aggregateRating`, so `ratingCountExact` is null there — same signal as
 *   `ratingCount`, read twice.
 * - `containsAds`/`inAppPurchases` are `false` only when the header's badge region was found and
 *   did not declare that badge. If the badge region itself is missing, they are `null` and listed
 *   in `unparsed`, because then "no ads" and "the page changed" are indistinguishable.
 */

const DETAIL_ENDPOINT = 'https://play.google.com/store/apps/details';

/** Same browser-ish headers as the search scanner: Play serves the rendered page to this. */
const BROWSER_HEADERS = {
  'user-agent':
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36',
  'accept-language': 'en-US,en;q=0.9',
};

/**
 * How far after the title the header (developer, badges) is searched. Measured header spans were
 * 295–410 chars; the window is generous so a longer developer line does not fall outside it, but
 * still far short of the first similar-app card, so a later card can never be mistaken for this app.
 */
const HEADER_WINDOW = 3000;

/** Thrown for anything the caller must not mistake for a successful read. */
export class PlayAppDetailError extends Error {
  constructor(code, message, details = {}) {
    super(message);
    this.name = 'PlayAppDetailError';
    this.code = code;
    Object.assign(this, details);
  }
}

const NAMED_ENTITIES = { amp: '&', lt: '<', gt: '>', quot: '"', apos: "'", nbsp: ' ' };

function decodeEntities(text) {
  return text
    .replace(/&#x([0-9a-f]+);/gi, (whole, hex) => {
      const code = Number.parseInt(hex, 16);
      return Number.isFinite(code) ? String.fromCodePoint(code) : whole;
    })
    .replace(/&#(\d+);/g, (whole, dec) => {
      const code = Number.parseInt(dec, 10);
      return Number.isFinite(code) ? String.fromCodePoint(code) : whole;
    })
    .replace(/&(amp|lt|gt|quot|apos|nbsp);/g, (_, name) => NAMED_ENTITIES[name]);
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/**
 * Parses a displayed magnitude such as `5M+`, `11.2K`, `1,234` into a number.
 *
 * Play uses commas for thousands and a dotted decimal before a `K`/`M`/`B` suffix, and it never
 * mixes the two, so stripping commas and reading the dot as a decimal is unambiguous. Returns
 * `null` when no number is present — never a guess.
 */
export function parseDisplayedNumber(text) {
  if (typeof text !== 'string') return null;
  const match = text.match(/([\d.,]+)\s*([KMB])?/i);
  if (!match) return null;

  const magnitude = { k: 1e3, m: 1e6, b: 1e9 }[(match[2] || '').toLowerCase()] ?? 1;
  const value = Number.parseFloat(match[1].replace(/,/g, ''));
  if (!Number.isFinite(value)) return null;

  return Math.round(value * magnitude);
}

/**
 * Extracts one app's facts from a Play app-detail HTML page.
 *
 * Pure and network-free so the markers above can be re-verified against captured HTML whenever
 * Play changes its markup. Throws `PlayAppDetailError` when the document is not an app-detail
 * page at all, because returning a plausible-looking empty object there is the exact failure this
 * module must not have.
 */
export function parseAppDetailHtml(html, packageId) {
  if (typeof html !== 'string' || html.length === 0) {
    throw new PlayAppDetailError('EMPTY_HTML', 'no HTML to parse');
  }
  if (typeof packageId !== 'string' || packageId.trim().length === 0) {
    throw new PlayAppDetailError('INVALID_PACKAGE_ID', 'packageId must be a non-empty string');
  }

  const titleMatch = html.match(
    /<h1[^>]*>[\s\S]{0,200}?<span[^>]*itemprop="name"[^>]*>([^<]*)<\/span>/,
  );
  if (!titleMatch) {
    throw new PlayAppDetailError(
      'PARSE_FAILED',
      `no app title (itemprop="name" inside an h1) — this is not a Play app-detail page for ${packageId}`,
    );
  }

  const title = decodeEntities(titleMatch[1]).trim();
  // Header = the identity block only. Bounding it prevents a similar-app card later in the page
  // from being read as this app's developer or its ad badge.
  const header = html.slice(titleMatch.index, titleMatch.index + HEADER_WINDOW);
  const unparsed = [];

  const developerMatch = header.match(/class="Vbfug[^"]*"[\s\S]{0,200}?<span>([^<]*)<\/span>/);
  const developer = developerMatch ? decodeEntities(developerMatch[1]).trim() : null;
  if (developer === null) unparsed.push('developer');

  // Play renders "Contains ads" / "In-app purchases" as badge spans in the header, and omits a
  // span entirely when the app has neither. The `Information badges` region is the structural
  // anchor that tells us this is the normal header: without it, a missing badge is not evidence.
  const badgesDeclared = header.includes('aria-label="Information badges"');
  const badges = [...header.matchAll(/class="UIuSk"[^>]*>([^<]*)</g)].map((m) =>
    decodeEntities(m[1]).trim(),
  );
  let containsAds = null;
  let inAppPurchases = null;
  if (badgesDeclared) {
    containsAds = badges.some((badge) => /contains ads/i.test(badge));
    inAppPurchases = badges.some((badge) => /in-?app purchases/i.test(badge));
  } else {
    unparsed.push('containsAds', 'inAppPurchases');
  }

  // An absent starRating block is the signal itself (a genuinely unrated app), so it stays null
  // rather than becoming an error or a default.
  const ratingBlock = html.match(
    /<div itemprop="starRating">([\s\S]{0,500}?)<div class="g1rdde">([^<]*)<\/div>/,
  );
  let rating = null;
  let ratingCount = null;
  if (ratingBlock) {
    const ratingValue = ratingBlock[1].match(/aria-label="Rated ([0-9.]+) stars? out of five stars"/i);
    rating = ratingValue ? Number.parseFloat(ratingValue[1]) : null;
    ratingCount = parseDisplayedNumber(ratingBlock[2]);
    if (rating === null) unparsed.push('rating');
    if (ratingCount === null) unparsed.push('ratingCount');
  }

  // Exact count from schema.org JSON-LD. The tag carries a per-response `nonce` attribute
  // (`<script type="application/ld+json" nonce="...">`), so the attribute list must be tolerated —
  // matching a bare tag here silently yielded null on every page.
  let ratingCountExact = null;
  const jsonLdMatch = html.match(
    /<script[^>]*type="application\/ld\+json"[^>]*>([\s\S]*?)<\/script>/,
  );
  if (jsonLdMatch) {
    try {
      const parsed = JSON.parse(jsonLdMatch[1]);
      const node = Array.isArray(parsed) ? parsed[0] : parsed;
      const count = Number.parseInt(node?.aggregateRating?.ratingCount, 10);
      if (Number.isFinite(count)) ratingCountExact = count;
    } catch {
      // Malformed/absent JSON-LD is expected on some pages; `ratingCount` already answered.
    }
  }

  const installsMatch = html.match(
    /<div class="ClM7O">([^<]+)<\/div><div class="g1rdde">Downloads<\/div>/,
  );
  const installs = installsMatch ? decodeEntities(installsMatch[1]).trim() : null;
  if (installs === null) unparsed.push('installs');

  const updatedMatch = html.match(
    /<div class="lXlx5">Updated on<\/div><div class="xg1aie">([^<]*)<\/div>/,
  );
  const updated = updatedMatch ? decodeEntities(updatedMatch[1]).trim() : null;
  if (updated === null) unparsed.push('updated');

  // Price is scoped to this app's own offer via `rdid=<packageId>`, so an offer rendered for a
  // similar app cannot supply it.
  const priceMatch = html.match(
    new RegExp(
      `itemprop="offers"[\\s\\S]{0,300}?rdid=${escapeRegExp(packageId)}[\\s\\S]{0,200}?itemprop="price" content="([^"]*)"`,
    ),
  );
  let price = null;
  let free = null;
  if (priceMatch) {
    price = decodeEntities(priceMatch[1]).trim();
    // "0" is free; a currency amount is not. Anything else is left unknown rather than guessed.
    if (price === '0' || price === '0.00' || /^free$/i.test(price)) {
      free = true;
    } else if (/\d/.test(price)) {
      free = false;
    } else {
      unparsed.push('free');
    }
  } else {
    unparsed.push('price', 'free');
  }

  return {
    packageId,
    title,
    developer,
    rating,
    /** Displayed count rounded by Play (11.2K -> 11200). Use `ratingCountExact` when present. */
    ratingCount,
    /** Unrounded schema.org count; null when Play does not server-render JSON-LD. */
    ratingCountExact,
    /** Raw store display, e.g. "5M+", "5+". Truthful to the page rather than normalised. */
    installs,
    /** Lower bound the display implies: "5M+" -> 5000000. Null when `installs` is null. */
    installsMin: installs === null ? null : parseDisplayedNumber(installs),
    /** Raw store display, e.g. "Aug 24, 2026". Not re-formatted, so no locale guessing. */
    updated,
    containsAds,
    inAppPurchases,
    /** Raw offer amount as rendered ("0", "$4.49"). */
    price,
    free,
    /** Raw badge texts, so an unrecognised future badge is visible rather than silently ignored. */
    badges,
    /** Whether the header badge region existed; false means the booleans above are null, not "no". */
    badgesDeclared,
    /** Fields whose anchors were not found while the page was otherwise valid. Empty is normal. */
    unparsed,
  };
}

/**
 * Fetches one app's detail page and parses it.
 *
 * Throws `PlayAppDetailError` on a non-2xx response or an unparseable page instead of returning a
 * partially filled record: a caller deciding whether to enter a niche must be able to tell a real
 * "no ads" from a failed fetch, and a half-filled object makes those two identical.
 */
export async function fetchAppDetail(packageId, { gl = 'US', hl = 'en', fetchImpl = fetch } = {}) {
  if (typeof packageId !== 'string' || packageId.trim().length === 0) {
    throw new PlayAppDetailError('INVALID_PACKAGE_ID', 'packageId must be a non-empty string');
  }

  const url = `${DETAIL_ENDPOINT}?id=${encodeURIComponent(packageId)}&hl=${hl}&gl=${gl}`;
  const response = await fetchImpl(url, { headers: BROWSER_HEADERS });

  if (!response.ok) {
    throw new PlayAppDetailError('HTTP_ERROR', `Play returned HTTP ${response.status} for ${packageId}`, {
      status: response.status,
      url,
    });
  }

  const html = await response.text();
  return { ...parseAppDetailHtml(html, packageId), url };
}
