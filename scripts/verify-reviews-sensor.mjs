/**
 * Verifies the review-complaint sensor (`scripts/lib/play-reviews.mjs`) against real Play pages.
 *
 * A sensor that cannot fail is not a sensor. Every check below is written so that it *does* fail
 * when the thing it guards is broken, and each one names the defect it is guarding against:
 *
 *  - A page that is not an app page must throw. A parser that returns `[]` for garbage makes
 *    "this app has no reviews" and "the markup changed" the same answer.
 *  - The rating must be read **inside a review card**. Counting `aria-label="Rated…"` across the
 *    reviews section is the defect this file exists to catch, and check C2 proves the trap is real
 *    on a live page rather than asserting it from memory.
 *  - A review's rating must belong to that review's text. C3 re-reads each card and confirms the
 *    aligned text is present, so a mis-split cannot pass silently.
 *  - The histogram must be *derived*, not stored. C5 recomputes every count independently.
 *  - The threshold must actually be applied. C6 asserts no counted review is above it.
 *  - The taxonomy must not be dead code. C7 fails if no class ever fires in the sample.
 *
 * Run: node scripts/verify-reviews-sensor.mjs
 * Exit code 0 = every check passed; 1 = at least one check failed.
 */
import {
  COMPLAINT_CLASSES,
  COMPLAINT_CLASS_IDS,
  CRITICAL_RATING,
  PlayReviewsError,
  classifyComplaint,
  fetchReviews,
  parseReviewsHtml,
  summariseComplaints,
} from './lib/play-reviews.mjs';

// Chosen to span the shapes this sensor must survive: a hated low-install app, a niche leader, a
// big consumer app whose page carries a star histogram (the trap), and an unrated small app.
const PACKAGES = [
  'com.mushroomhunters',
  'it.Ettore.calcolielettrici',
  'com.spotify.music',
  'com.plant.disease.identifier',
  'com.mts.visualscheduleandstorymaker',
];

let failures = 0;
let checksRun = 0;

function check(id, passed, detail) {
  checksRun += 1;
  const status = passed ? 'PASS' : 'FAIL';
  if (!passed) failures += 1;
  console.log(`  ${status}  ${id}  ${detail}`);
}

console.log(`=== fetching ${PACKAGES.length} app pages ===`);
const pages = [];
for (const pkg of PACKAGES) {
  try {
    const page = await fetchReviews(pkg);
    pages.push(page);
    const summary = summariseComplaints(page.reviews);
    console.log(
      `  ${pkg}: cards=${page.cardsSeen ?? 0} reviews=${page.reviews.length} ` +
        `critical(<=${CRITICAL_RATING})=${summary.criticalRead} ` +
        `top=${summary.ranked[0] ? `${summary.ranked[0].id}(${summary.ranked[0].count})` : 'none'} ` +
        `unclassified=${summary.unclassified}`,
    );
  } catch (err) {
    console.log(`  ${pkg}: FETCH FAILED — ${err.code ?? err.name}: ${err.message}`);
    failures += 1;
  }
}

const withReviews = pages.filter((p) => p.sectionFound && p.reviews.length > 0);
check('C1a', withReviews.length > 0, `${withReviews.length} of ${pages.length} pages yielded at least one review`);

// ---------------------------------------------------------------- C1: garbage must throw
console.log('\n=== C1: a non-app page must throw, not return an empty list ===');
let threw = null;
try {
  parseReviewsHtml('<html><body><h1>Not an app</h1></body></html>', 'com.example.nope');
} catch (err) {
  threw = err;
}
check('C1b', threw instanceof PlayReviewsError && threw.code === 'PARSE_FAILED', `garbage HTML threw ${threw?.code ?? 'nothing'}`);

// A real 404 body is the other shape of "not an app page".
try {
  await fetchReviews('com.heavycodeworks.essentialkit');
  check('C1c', false, 'a 404 package did not throw');
} catch (err) {
  check('C1c', err instanceof PlayReviewsError, `404 package threw ${err.code ?? err.name}`);
}

// ------------------------------------------- C2: the star-histogram trap is real, and we avoid it
console.log('\n=== C2: rating anchors in the section vs. review cards ===');
let trapProven = 0;
for (const pkg of PACKAGES) {
  const res = await fetch(`https://play.google.com/store/apps/details?id=${pkg}&hl=en&gl=US`, {
    headers: {
      'user-agent':
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36',
      'accept-language': 'en-US,en;q=0.9',
    },
  });
  const html = await res.text();
  const sectionAt = html.indexOf('data-g-id="reviews"');
  if (sectionAt === -1) {
    console.log(`  ${pkg}: no review section`);
    continue;
  }
  const section = html.slice(sectionAt);
  const anchors = [...section.matchAll(/aria-label="Rated ([0-9.]+) stars? out of five stars"/g)].length;
  const cards = section.split('class="Jx4nYe"').length - 1;
  const parsed = parseReviewsHtml(html, pkg);
  const naiveWouldOvercount = anchors > cards;
  if (naiveWouldOvercount) trapProven += 1;
  console.log(
    `  ${pkg}: section rating-anchors=${anchors} cards=${cards} parsed=${parsed.reviews.length}` +
      `${naiveWouldOvercount ? '  <-- naive count would overcount' : ''}`,
  );
  check(
    `C2[${pkg}]`,
    parsed.reviews.length === cards,
    `parsed reviews (${parsed.reviews.length}) equals card count (${cards})`,
  );
}
check('C2-trap', trapProven > 0, `the naive-count trap is real on ${trapProven} page(s), so card scoping is load-bearing`);

// ------------------------------------------------- C3: each rating belongs to its own card's text
console.log('\n=== C3: rating/text alignment inside each card ===');
let alignmentChecked = 0;
let alignmentBad = 0;
for (const pkg of PACKAGES) {
  const res = await fetch(`https://play.google.com/store/apps/details?id=${pkg}&hl=en&gl=US`, {
    headers: {
      'user-agent':
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36',
      'accept-language': 'en-US,en;q=0.9',
    },
  });
  const html = await res.text();
  const sectionAt = html.indexOf('data-g-id="reviews"');
  if (sectionAt === -1) continue;
  const cards = html.slice(sectionAt).split('class="Jx4nYe"').slice(1);
  const parsed = parseReviewsHtml(html, pkg);

  for (const [i, review] of parsed.reviews.entries()) {
    const card = cards[i];
    alignmentChecked += 1;
    const ratingAt = card.search(/aria-label="Rated [0-9.]+ stars? out of five stars"/i);
    const bodyAt = card.search(/class="h3YV2d"/);
    // The rating must precede the body inside the card, and the body text must be that review's.
    const orderOk = ratingAt !== -1 && (bodyAt === -1 || ratingAt < bodyAt);
    const textOk =
      review.text === null ||
      decodeForCompare(card).includes(decodeForCompare(review.text).slice(0, 40));
    if (!orderOk || !textOk) {
      alignmentBad += 1;
      console.log(`  MISALIGNED ${pkg} card ${i + 1}: orderOk=${orderOk} textOk=${textOk}`);
    }
  }
}
check('C3', alignmentChecked > 0 && alignmentBad === 0, `${alignmentChecked} review(s) checked, ${alignmentBad} misaligned`);

function decodeForCompare(value) {
  return value
    .replace(/&#x([0-9a-f]+);/gi, (w, h) => String.fromCodePoint(Number.parseInt(h, 16)))
    .replace(/&#(\d+);/g, (w, d) => String.fromCodePoint(Number.parseInt(d, 10)))
    .replace(/&(amp|lt|gt|quot|apos|nbsp);/g, (w, n) => ({ amp: '&', lt: '<', gt: '>', quot: '"', apos: "'", nbsp: ' ' })[n])
    .replace(/<[^>]*>/g, ' ')
    .replace(/\s+/g, ' ');
}

// ------------------------------------------------------------- C4: no reviewer identity is kept
console.log('\n=== C4: no reviewer identity in the output ===');
const EXPECTED_KEYS = ['date', 'helpfulCount', 'rating', 'text'];
let keyViolations = 0;
for (const page of pages) {
  for (const review of page.reviews) {
    const keys = Object.keys(review).sort();
    if (keys.join(',') !== EXPECTED_KEYS.join(',')) {
      keyViolations += 1;
      console.log(`  UNEXPECTED KEYS ${page.packageId}: ${keys.join(',')}`);
    }
  }
}
check(
  'C4',
  keyViolations === 0,
  `every review carries exactly [${EXPECTED_KEYS.join(', ')}] — no author field exists to leak`,
);

// ------------------------------------------------------ C5: the histogram is derived, not stored
console.log('\n=== C5: histogram recomputed independently ===');
let histogramMismatch = 0;
for (const page of pages) {
  const summary = summariseComplaints(page.reviews);
  const recomputed = Object.fromEntries(COMPLAINT_CLASS_IDS.map((id) => [id, 0]));
  for (const review of page.reviews) {
    if (review.rating > CRITICAL_RATING) continue;
    if (typeof review.text !== 'string' || review.text.length === 0) continue;
    for (const id of classifyComplaint(review.text)) recomputed[id] += 1;
  }
  const same = COMPLAINT_CLASS_IDS.every((id) => recomputed[id] === summary.counts[id]);
  if (!same) {
    histogramMismatch += 1;
    console.log(`  MISMATCH ${page.packageId}: summary=${JSON.stringify(summary.counts)} recomputed=${JSON.stringify(recomputed)}`);
  }
}
check('C5', histogramMismatch === 0, `${pages.length} page(s) cross-checked; ${histogramMismatch} mismatch(es)`);

// ------------------------------------------------ C6: the threshold is applied, not decorative
console.log('\n=== C6: no review above the threshold is counted ===');
let thresholdViolations = 0;
for (const page of pages) {
  const summary = summariseComplaints(page.reviews);
  const expectedCritical = page.reviews.filter((r) => r.rating <= CRITICAL_RATING).length;
  if (summary.criticalRead !== expectedCritical) {
    thresholdViolations += 1;
    console.log(`  MISMATCH ${page.packageId}: criticalRead=${summary.criticalRead} expected=${expectedCritical}`);
  }
}
check('C6', thresholdViolations === 0, `criticalRead matches the ${CRITICAL_RATING}★ boundary on every page`);

// ----------------------------------------------------------- C7: the taxonomy is not dead code
console.log('\n=== C7: the taxonomy fires on real text, and stays quiet on praise ===');
const fired = new Set();
for (const page of pages) {
  for (const review of page.reviews) {
    if (typeof review.text === 'string') for (const id of classifyComplaint(review.text)) fired.add(id);
  }
}
check('C7a', fired.size > 0, `${fired.size}/${COMPLAINT_CLASS_IDS.length} classes fired in the sample: ${[...fired].join(', ') || '(none)'}`);

const praise = 'Absolutely love it, works great, thank you to the developer!';
check('C7b', classifyComplaint(praise).length === 0, `praise classified as [${classifyComplaint(praise).join(', ') || 'nothing'}]`);
const unclassifiedShare = withReviews.length
  ? withReviews.reduce((sum, p) => sum + summariseComplaints(p.reviews).unclassified, 0) /
    withReviews.reduce((sum, p) => sum + summariseComplaints(p.reviews).criticalRead, 0)
  : 0;
console.log(`  INFO  unclassified share of critical reviews in this sample: ${(unclassifiedShare * 100).toFixed(1)}%`);
console.log(`  INFO  classes defined: ${COMPLAINT_CLASSES.length}`);

console.log(`\n=== ${checksRun} checks, ${failures} failure(s) ===`);
console.log(failures === 0 ? 'SENSOR VERIFIED' : 'SENSOR NOT VERIFIED');
process.exit(failures === 0 ? 0 : 1);
