/**
 * Offline regression tests for scripts/lib/play-reviews.mjs.
 *
 * Paired with `verify-reviews-sensor.mjs`, exactly as `test-play-scan.mjs` is paired with
 * `verify-play-scan-defects.mjs`: the synthetic fixture here is the fast, deterministic sensor,
 * and the live run is the proof that the fixture still resembles Play's real markup.
 *
 * The first case is the reason this file exists. Play's reviews section contains a **star
 * histogram** whose rows carry the same `aria-label="Rated N stars out of five stars"` as a review
 * does — measured 4, 7, 10 and 15 anchors for 3 reviews on real pages. Counting anchors is the
 * obvious implementation and it is wrong; the fixture below encodes that trap so an implementation
 * that counts instead of splitting fails here rather than in a market verdict.
 *
 * Run: node scripts/test-play-reviews.mjs
 */
import {
  COMPLAINT_CLASS_IDS,
  CRITICAL_RATING,
  PlayReviewsError,
  classifyComplaint,
  parseReviewsHtml,
  summariseComplaints,
} from './lib/play-reviews.mjs';

let failures = 0;
let checks = 0;

function check(name, condition, detail) {
  checks += 1;
  if (condition) {
    console.log(`  PASS  ${name}`);
  } else {
    failures += 1;
    console.log(`  FAIL  ${name}`);
    if (detail) console.log(`        ${detail}`);
  }
}

/** One review card, shaped like Play's: rating anchor, date, body. */
function card({ rating, text = null, date = 'July 24, 2021', helpful = null, cardClass = 'Jx4nYe' }) {
  const body = text === null ? '' : `<div class="h3YV2d">${text}</div>`;
  const helpfulBlock = helpful === null ? '' : `<div>${helpful} people found this review helpful</div>`;
  return (
    `<div class="${cardClass}">` +
    `<div aria-label="Rated ${rating} stars out of five stars" role="img" class="iXRFPc">stars</div>` +
    `<span class="bp9Aid">${date}</span>` +
    body +
    helpfulBlock +
    `</div>`
  );
}

/** A star-histogram row: NOT a review, but carrying an identical rating aria-label. */
function histogramRow(stars, percent) {
  return (
    `<div aria-label="Rated ${stars} stars out of five stars" role="img" class="histoRow">` +
    `<div class="bar" style="width:${percent}%"></div></div>`
  );
}

const APP_TITLE = `<h1 class="Fd93Bb"><span itemprop="name">Mushroom Hunter's Guide</span></h1>`;

const fixture =
  APP_TITLE +
  `<div aria-label="Information badges"><span class="UIuSk">Contains ads</span></div>` +
  `<div class="F7XJmb" data-number="1">stars</div>` +
  `<div data-g-id="description">A guide.</div>` +
  `<div class="Jwxk6d" data-g-id="reviews">` +
  histogramRow(5, 18) +
  histogramRow(4, 4) +
  histogramRow(3, 6) +
  histogramRow(2, 8) +
  histogramRow(1, 64) +
  card({ rating: 1, text: 'Useless app, everything is behind a paywall. Avoid.', date: 'July 24, 2021', helpful: 8 }) +
  card({ rating: 5, text: 'Best app about mushrooms, love it!', date: 'October 11, 2022' }) +
  card({ rating: 2, text: null, date: 'September 9, 2021', helpful: 4 }) +
  `</div>`;

console.log('=== T1: the star histogram must not be read as reviews ===');
const parsed = parseReviewsHtml(fixture, 'com.mushroomhunters');

// Prove the trap is encoded in the fixture: a naive anchor count disagrees with the truth.
const sectionAt = fixture.indexOf('data-g-id="reviews"');
const naiveAnchorCount = [...fixture.slice(sectionAt).matchAll(/aria-label="Rated [0-9.]+ stars? out of five stars"/g)].length;
check(
  'fixture really encodes the trap',
  naiveAnchorCount > 3,
  `naive count would be ${naiveAnchorCount} for 3 reviews`,
);
check('parsed exactly the three review cards', parsed.reviews.length === 3, `got ${parsed.reviews.length}`);
check(
  'ratings come from the cards, not the histogram',
  parsed.reviews.map((r) => r.rating).join(',') === '1,5,2',
  `got ${parsed.reviews.map((r) => r.rating).join(',')}`,
);
check('cardsSeen is reported', parsed.cardsSeen === 3, `got ${parsed.cardsSeen}`);

console.log('\n=== T2: each rating stays with its own text ===');
check(
  'first card pairs 1 star with the paywall text',
  parsed.reviews[0].rating === 1 && /paywall/.test(parsed.reviews[0].text),
  JSON.stringify(parsed.reviews[0]),
);
check(
  'second card pairs 5 stars with the praise text',
  parsed.reviews[1].rating === 5 && /love it/.test(parsed.reviews[1].text),
  JSON.stringify(parsed.reviews[1]),
);
check('a rating-only review keeps its rating and a null text', parsed.reviews[2].rating === 2 && parsed.reviews[2].text === null, JSON.stringify(parsed.reviews[2]));
check('date and helpful count are read', parsed.reviews[0].date === 'July 24, 2021' && parsed.reviews[0].helpfulCount === 8, JSON.stringify(parsed.reviews[0]));

console.log('\n=== T3: escaped user text survives, nested tags cannot truncate ===');
const escaped = parseReviewsHtml(
  APP_TITLE + `<div data-g-id="reviews">` + card({ rating: 1, text: 'It says &quot;free&quot; but it is not &#39;free&#39; &lt;tag&gt;' }) + `</div>`,
  'com.example.escaped',
);
check(
  'entities are decoded',
  escaped.reviews[0].text === `It says "free" but it is not 'free' <tag>`,
  JSON.stringify(escaped.reviews[0].text),
);

console.log('\n=== T4: structural failure must be loud, absence must be quiet ===');
let threw = null;
try {
  parseReviewsHtml('<html><body><h1>Just a page</h1></body></html>', 'com.example.nope');
} catch (err) {
  threw = err;
}
check('a non-app page throws PARSE_FAILED', threw instanceof PlayReviewsError && threw.code === 'PARSE_FAILED', `threw ${threw?.code ?? 'nothing'}`);

const noSection = parseReviewsHtml(APP_TITLE + '<div data-g-id="description">no reviews here</div>', 'com.example.empty');
check('a real app page without reviews is an empty read, not an error', noSection.sectionFound === false && noSection.reviews.length === 0, JSON.stringify(noSection));
check('the missing section is named in unparsed', noSection.unparsed.includes('reviews'), JSON.stringify(noSection.unparsed));

console.log('\n=== T5: the histogram is derived, and the threshold is applied ===');
const summary = summariseComplaints(parsed.reviews);
check(
  'critical count uses the exported threshold',
  summary.maxRating === CRITICAL_RATING && summary.criticalRead === 2,
  `maxRating=${summary.maxRating} criticalRead=${summary.criticalRead}`,
);
check('the 5-star review contributes no complaint class', summary.counts.paywall === 1, JSON.stringify(summary.counts));
check('a rating-only review is counted as withoutText, not dropped', summary.withoutText === 1, `withoutText=${summary.withoutText}`);
check(
  'every class id appears in counts (zero included)',
  COMPLAINT_CLASS_IDS.every((id) => typeof summary.counts[id] === 'number'),
  Object.keys(summary.counts).join(','),
);
check('ranked list is sorted by count', summary.ranked.length === 0 || summary.ranked.every((entry, i, a) => i === 0 || a[i - 1].count >= entry.count), JSON.stringify(summary.ranked));

console.log('\n=== T6: the taxonomy fires on complaints and stays quiet on praise ===');
check('paywall text matches the paywall class', classifyComplaint('everything is behind a paywall').includes('paywall'));
check('crash text matches the crash class', classifyComplaint('It crashes every time I open it').includes('crash'));
check('praise matches nothing', classifyComplaint('Absolutely love it, works great, thank you!').length === 0, JSON.stringify(classifyComplaint('Absolutely love it, works great, thank you!')));
check('word boundaries: "download" is not an ad', !classifyComplaint('I download it from the store').includes('ads'), JSON.stringify(classifyComplaint('I download it from the store')));
check('word boundaries: "bad" is not an ad', !classifyComplaint('bad').includes('ads'), JSON.stringify(classifyComplaint('bad')));

console.log('\n=== T6b: the 2026-09-24 corpus revision — every text that exposed a gap ===');
// One case per revision row in the COMPLAINT_CLASSES KDoc. These are verbatim fragments from real
// reviews, so a future "simplification" of a pattern fails here rather than in a market verdict.
const revisionCases = [
  ['units', 'Results are in metric'],
  ['units', 'even after I set my prefs to us units, it still displays all metric'],
  ['regression', 'Old version was better!'],
  ['regression', 'New version it is different? I can see measures from the older one.'],
  ['regression', 'worse than the app it replaced'],
  ['language', "It's in a different language. And currency. Can't understand much."],
  ['account', 'as a tenet I cannot log into this app'],
  ['paywall', 'so now I would have to buy 38 on the new version'],
  ['paywall', 'Quick to take your money and locking you in without being able to CANCEL'],
  ['broken', 'would not return a value for a long length high demand run.'],
  ['broken', "after entering data, app doesn't show results"],
  ['missing-feature', "there's no way to save more than one recipe"],
  ['missing-feature', 'It currently only supports technicians'],
  ['value', 'Deceptive pics. None of the screens even exist in the app.'],
  ['offline', 'The data should all be baked into the app, not online.'],
];
for (const [expected, text] of revisionCases) {
  const got = classifyComplaint(text);
  check(`"${text.slice(0, 48)}" -> ${expected}`, got.includes(expected), `got [${got.join(', ') || 'nothing'}]`);
}

// Naming the false positive on purpose: a lexical classifier cannot tell "no ads" praise from an
// anti-ads complaint, and hiding that would make the ads count look more precise than it is.
check(
  'KNOWN FALSE POSITIVE: "no ads" praise matches the ads class (audited by eye, never silently "fixed")',
  classifyComplaint('great app, no ads, love it').includes('ads'),
  `got [${classifyComplaint('great app, no ads, love it').join(', ')}]`,
);

console.log('\n=== T7: no reviewer identity can be carried ===');
const keys = Object.keys(parsed.reviews[0]).sort().join(',');
check('review objects expose exactly [date, helpfulCount, rating, text]', keys === 'date,helpfulCount,rating,text', keys);

console.log(`\n=== ${checks} checks, ${failures} failure(s) ===`);
console.log(failures === 0 ? 'SENSOR UNIT TESTS PASSED' : 'SENSOR UNIT TESTS FAILED');
process.exit(failures === 0 ? 0 : 1);
