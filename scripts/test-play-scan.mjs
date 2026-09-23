/**
 * Offline regression tests for scripts/lib/play-scan.mjs.
 *
 * Each case below exists because the defect was observed on live Play data, reproduced against
 * this module, and then fixed. A synthetic fixture alone only proves the parser agrees with my own
 * assumption about Play's markup, so these tests are paired with `verify-play-scan-defects.mjs`,
 * which re-runs the same checks against real pages. The synthetic run is the fast, deterministic
 * sensor; the live run is the proof that the fixture still resembles reality.
 *
 * Run: node scripts/test-play-scan.mjs
 */
import { parseSearchHtml, analyzeResults, verdictFor, organisationLikeDevelopers, analyzeRelevance, scanQuery, formatTable, POLLUTED_SHARE } from './lib/play-scan.mjs';

let failures = 0;

function check(name, condition, detail) {
  if (condition) {
    console.log(`  PASS  ${name}`);
  } else {
    failures += 1;
    console.log(`  FAIL  ${name}`);
    if (detail) console.log(`        ${detail}`);
  }
}

/**
 * Builds one card shaped like Play's search results.
 *
 * `attrsAfterHref` exists because that is what broke the parser: Play puts further attributes
 * after `href`, so a segment starting at the link match began mid-attribute.
 */
function card({ id, title, developer, rating = null, attrsAfterHref = '' }) {
  const ratingBlock =
    rating === null
      ? ''
      : `<span aria-label="Rated ${rating} stars out of five stars">${rating}</span><span>star</span>`;
  return (
    `<div class="cell"><div class="wrap">` +
    `<a class="Si6A0c" href="/store/apps/details?id=${id}"${attrsAfterHref}>` +
    `<div class="Vc0mnc"><img src="https://x/y.jpg" alt="Screenshot image" />` +
    `<div class="meta"><button aria-label="Play ${title}" class="FN1l2">${title}</button>` +
    `<div class="dev">${developer}</div>${ratingBlock}</div></div></a></div></div>`
  );
}

const html = [
  // Ordinary card, with the exact attribute order that produced the leaked-title defect.
  card({
    id: 'com.whoop.android',
    title: 'WHOOP',
    developer: 'WHOOP',
    rating: '4.5',
    attrsAfterHref: ' aria-label="WHOOP" class="Qfxief"',
  }),
  card({ id: 'com.cube.arc.fa', title: 'First Aid: American Red Cross', developer: 'American Red Cross', rating: '4.8' }),
  card({ id: 'com.renz.firstaid', title: 'Offline First Aid Guide', developer: 'UnknownUser', rating: null }),
  // Duplicate link, as Play renders for the image and the title.
  `<div class="cell"><a href="/store/apps/details?id=com.cube.arc.fa"><img src="x" alt="Screenshot image" /></a></div>`,
].join('');

console.log('=== D1: card text must not contain markup or a store path ===');

const parsed = parseSearchHtml(html);

check('parses four links into three unique apps', parsed.length === 3, `got ${parsed.length}: ${parsed.map((r) => r.packageId).join(', ')}`);

const whoop = parsed.find((r) => r.packageId === 'com.whoop.android');
check('whoop card found', Boolean(whoop));
if (whoop) {
  check(
    'title is clean text, not an href fragment',
    !/\/store\/|="|</.test(whoop.title),
    `title=${JSON.stringify(whoop.title)}`,
  );
  check('title equals the Play label', whoop.title === 'WHOOP', `title=${JSON.stringify(whoop.title)}`);
  check('developer is not the leaked attribute tail', whoop.developer === 'WHOOP', `developer=${JSON.stringify(whoop.developer)}`);
}

const redCross = parsed.find((r) => r.packageId === 'com.cube.arc.fa');
if (redCross) {
  check('title parsed for a labelled card', redCross.title === 'First Aid: American Red Cross', `title=${JSON.stringify(redCross.title)}`);
  check('developer parsed for a labelled card', redCross.developer === 'American Red Cross', `developer=${JSON.stringify(redCross.developer)}`);
  check('rating parsed', redCross.rating === 4.8, `rating=${redCross.rating}`);
}

const unrated = parsed.find((r) => r.packageId === 'com.renz.firstaid');
if (unrated) {
  check('an unrated card stays unrated rather than defaulting', unrated.rating === null && unrated.rated === false, `rating=${unrated.rating} rated=${unrated.rated}`);
}

// A live run once reported a suspiciously short result set (10 cards for one query against 30 for
// others), which could have meant either a small niche or a parser dropping cards. Reproducing it
// against real pages showed `parsed == distinct` for every query, so de-duplication is not
// over-aggressive. This pins that property offline: repeated links must collapse without losing a
// card. It cannot prove every card Play renders carries a link the parser matches - that is what
// `verify-play-scan-defects.mjs` checks against live pages.
check('three unique apps survive duplicate links', parsed.length === 3, `got ${parsed.length}`);

console.log('\n=== D3: a fortress reason must not contradict its own weakCount ===');

// 7 strong, 2 weak of 10 rated -> strongShare 0.7 (>= 0.6), weakShare 0.2 (< 0.3): fortress with
// a non-zero weakCount, which is exactly the case the old wording denied.
const fortressResults = [
  ...Array.from({ length: 7 }, (_, i) => ({ packageId: `strong${i}`, rating: 4.6, rated: true })),
  ...Array.from({ length: 2 }, (_, i) => ({ packageId: `weak${i}`, rating: 3.5, rated: true })),
  { packageId: 'unrated', rating: null, rated: false },
];
const fortressAnalysis = analyzeResults(fortressResults);
const fortressVerdict = verdictFor(fortressAnalysis);

check('verdict is fortress', fortressVerdict.verdict === 'fortress', `got ${fortressVerdict.verdict}`);
check('weakCount is non-zero in this fixture', fortressAnalysis.weakCount === 2, `weakCount=${fortressAnalysis.weakCount}`);
check('ratedCount is the 9 rated cards, not all 10', fortressAnalysis.ratedCount === 9, `ratedCount=${fortressAnalysis.ratedCount}`);
check(
  'reason does not claim there is no weak pocket',
  !/no weak pocket/.test(fortressVerdict.reason),
  `reason=${JSON.stringify(fortressVerdict.reason)}`,
);
check(
  'reason states the real weak share out of the rated count',
  fortressVerdict.reason.includes('2/9'),
  `reason=${JSON.stringify(fortressVerdict.reason)}`,
);

console.log('\n=== D2: organisation heuristic needs word boundaries ===');

const orgResults = [
  { packageId: 'a', title: 'WHO app', developer: 'World Health Organization', rating: 4.5, rated: true },
  { packageId: 'b', title: 'WHOOP', developer: 'WHOOP', rating: 4.5, rated: true },
  { packageId: 'c', title: 'healow', developer: 'eClinicalWorks LLC', rating: 4.0, rated: true },
  { packageId: 'd', title: 'Evite', developer: 'Whova', rating: 4.0, rated: true },
  { packageId: 'e', title: 'First Aid', developer: 'American Red Cross', rating: 4.8, rated: true },
  { packageId: 'f', title: 'Notes', developer: 'Solo Developer', rating: 4.2, rated: true },
];
const flagged = organisationLikeDevelopers(orgResults);
const flaggedNames = flagged.map((f) => f.split(' (')[0]);

check('flags World Health Organization', flaggedNames.includes('World Health Organization'), `flagged=${flaggedNames.join(' | ')}`);
check('flags American Red Cross', flaggedNames.includes('American Red Cross'), `flagged=${flaggedNames.join(' | ')}`);
check('does not flag WHOOP', !flaggedNames.includes('WHOOP'), `flagged=${flaggedNames.join(' | ')}`);
check('does not flag eClinicalWorks LLC (substring "clinic")', !flaggedNames.includes('eClinicalWorks LLC'), `flagged=${flaggedNames.join(' | ')}`);
check('does not flag Whova', !flaggedNames.includes('Whova'), `flagged=${flaggedNames.join(' | ')}`);
check('does not flag an ordinary solo developer', !flaggedNames.includes('Solo Developer'), `flagged=${flaggedNames.join(' | ')}`);

console.log('\n=== D5: fuzzy-match pollution — a verdict computed from apps the query never described ===');

/**
 * Titles copied VERBATIM from the real run artifact
 * `.kiln/plant-vertical/scan-2026-09-23.json` (the top-8 enriched card of each query, 2026-09-23).
 *
 * The artifact is gitignored disposable state, so the durable copy of the evidence lives here as a
 * literal fixture - a test that read `.kiln/` would silently stop testing anything the day that
 * state was cleaned. Developer names are carried over because they were in the artifact, but the
 * guard reads titles only.
 *
 * Each fixture mirrors one row of the defect report:
 * - `late blight` is the dangerous one: it returned `candidate` ("build this") out of an app named
 *   `Lands of Blight`, a calm/focus app, a community app, and four lighting/Bluetooth apps. What
 *   these cards share with the query is text, not topic - `blight` contains the letters of `light` -
 *   and Play search is a fuzzy text match, so the shelf answered the query's spelling.
 * - `whitefly treatment` returned mental-health and therapy apps whose names start `White`.
 * - `tomato blight` returned pizza, a pomodoro timer and a Harry Potter game.
 * - `tomato leaf curl` returned a wallpaper, a VPN, a simulator and a manga reader.
 * - `tomato disease` is the clean control: a crop query whose results really are disease tools.
 */
const LATE_BLIGHT = [
  { title: 'Lands of Blight', developer: '7 Spot Games', rating: '4.5' },
  { title: 'BE LIGHT: Calm & Focus Boost', developer: 'BE LIGHT Now GmbH', rating: '3.8' },
  { title: 'Belight Community', developer: 'Tomorrow - Services', rating: null },
  { title: 'L&S LITE BT', developer: 'L&S Italia S.P.A.', rating: null },
  { title: 'B\u00b7T LIGHTING', developer: 'CXW Technology (Shenzhen) Company Limited', rating: null },
  { title: 'BLE Light', developer: 'Ningbo Xinyilong Electronic Technology Co., Ltd.', rating: null },
  { title: 'Dustlight', developer: 'Latai GmbH', rating: null },
  { title: 'BT Light', developer: 'nikey', rating: '2.5' },
];

const WHITEFLY = [
  { title: 'WhiteFlag: Mental Health Help', developer: 'whiteflagapp', rating: '3.9' },
  { title: 'Vtalix: Therapy for Everyone', developer: 'vtalix', rating: null },
  { title: 'Whitecaf\u00e9', developer: 'xMenu srl', rating: null },
  { title: 'White Feathers', developer: 'Yixuan Information Technology (Shanghai) Co., Ltd.', rating: null },
  { title: 'Whitecroft Sense', developer: 'Whitecroft Lighting Limited', rating: null },
  { title: 'White Feather', developer: "That's My AI Guy LLC", rating: null },
  { title: 'WHITE ICY', developer: 'eFolix SARL', rating: null },
  { title: 'White Collar Professional', developer: 'White Collar Professional', rating: null },
];

const TOMATO_BLIGHT = [
  { title: 'Tomato Cultivation', developer: 'plant-apps', rating: null },
  { title: 'iPlant with Brigitte Mars', developer: 'Lundgren Consulting LLC', rating: '4' },
  { title: 'Snappy Tomato Pizza', developer: 'Snappy Tomato Pizza', rating: '3.6' },
  { title: 'Tomato Focus: Pomodoro Timer', developer: 'danielelpsy', rating: null },
  { title: 'Harry Potter: Hogwarts Mystery', developer: 'Jam City, Inc.', rating: '4.6' },
  { title: 'Blinkit: Groceries & more', developer: 'Blinkit', rating: '4.8' },
  { title: 'Fandango: Movies & Series', developer: 'Fandango Media', rating: '4.4' },
  { title: 'Tomato Clinic', developer: 'npckc', rating: '4.8' },
];

const TOMATO_LEAF_CURL = [
  { title: 'Tomato Clinic', developer: 'npckc', rating: '4.8' },
  { title: 'Tomato Cultivation', developer: 'plant-apps', rating: null },
  { title: 'Tomato Wallpaper', developer: 'small wave', rating: null },
  { title: 'Tomato simulator', developer: 'Strategimws', rating: '4.4' },
  { title: 'Tomato Focus: Pomodoro Timer', developer: 'danielelpsy', rating: null },
  { title: 'Tomato Lite - Secure VPN Proxy', developer: 'IronMeta Studio', rating: '4' },
  { title: 'Tomato - Official Game', developer: 'Bong Chul Kim', rating: null },
  { title: 'Tomato Manga', developer: 'OLEXK APPS STUDIOS', rating: null },
];

const TOMATO_DISEASE = [
  { title: 'Tomato Disease Diagnosis', developer: 'Farming AI', rating: null },
  { title: 'Agrio - Plant diagnosis app', developer: 'Saillog Ltd', rating: '4.2' },
  { title: 'Plant Disease Diagnosis App', developer: 'Artmvstd', rating: '3.9' },
  { title: 'Plant disease identifier', developer: 'AppCraftingMinds', rating: '3' },
  { title: 'Plant disease', developer: 'LeafMind Tech Limited', rating: '3.6' },
  { title: 'Tomato Clinic', developer: 'npckc', rating: '4.8' },
  { title: 'Kopal: Plant Disease Scanner', developer: 'Xorvanta Technologies', rating: null },
  { title: 'Plant Disease identifier', developer: 'Brahim Chouih', rating: null },
];

/** The guard reads titles only, so a fixture can be reduced to titles for these checks. */
const titlesOf = (entries) => entries.map(({ title }) => ({ title }));

const lateBlightRelevance = analyzeRelevance('late blight', titlesOf(LATE_BLIGHT));
const whiteflyRelevance = analyzeRelevance('whitefly treatment', titlesOf(WHITEFLY));
const tomatoBlightRelevance = analyzeRelevance('tomato blight', titlesOf(TOMATO_BLIGHT));
const tomatoCurlRelevance = analyzeRelevance('tomato leaf curl', titlesOf(TOMATO_LEAF_CURL));
const tomatoDiseaseRelevance = analyzeRelevance('tomato disease', titlesOf(TOMATO_DISEASE));

check(
  'late blight is flagged polluted, not read as a market',
  lateBlightRelevance.level === 'polluted' && lateBlightRelevance.suspect === true,
  `level=${lateBlightRelevance.level} suspect=${lateBlightRelevance.suspect} share=${lateBlightRelevance.share}`,
);
check(
  'late blight share is measured and below the pollution threshold',
  lateBlightRelevance.share !== null && lateBlightRelevance.share < POLLUTED_SHARE,
  `share=${lateBlightRelevance.share} threshold=${POLLUTED_SHARE}`,
);
check(
  'late blight echoes at 1/8: only the literal token "blight" in "Lands of Blight"',
  lateBlightRelevance.onTopicCount === 1 && lateBlightRelevance.total === 8,
  `onTopic=${lateBlightRelevance.onTopicCount}/${lateBlightRelevance.total}`,
);
check(
  'late blight warning names the query and the count',
  typeof lateBlightRelevance.warning === 'string' && lateBlightRelevance.warning.includes('"late blight"') && lateBlightRelevance.warning.includes('1/8'),
  `warning=${JSON.stringify(lateBlightRelevance.warning)}`,
);

// The tokenizer's whole job: `light` must never match `blight`, `Belight` or `LITE`. A naive
// substring test (either direction) fires here - `'late blight'.includes('light')` is true, which
// is precisely how this shelf received a `candidate` verdict.
check(
  '"BE LIGHT" does not echo the query "late blight"',
  analyzeRelevance('late blight', titlesOf([LATE_BLIGHT[1]])).onTopicCount === 0,
  `onTopic=${analyzeRelevance('late blight', titlesOf([LATE_BLIGHT[1]])).onTopicCount}`,
);
check(
  '"L&S LITE BT" does not echo the token "blight"',
  analyzeRelevance('blight', titlesOf([LATE_BLIGHT[3]])).onTopicCount === 0,
  `onTopic=${analyzeRelevance('blight', titlesOf([LATE_BLIGHT[3]])).onTopicCount}`,
);
check(
  'the same title DOES echo the query "light" - the comparison is equality, not a ban on short words',
  analyzeRelevance('light therapy', titlesOf([LATE_BLIGHT[1]])).onTopicCount === 1,
  `onTopic=${analyzeRelevance('light therapy', titlesOf([LATE_BLIGHT[1]])).onTopicCount}`,
);
check(
  '"WhiteFlag" does not echo "whitefly" (equality, not prefix)',
  whiteflyRelevance.onTopicCount === 0 && whiteflyRelevance.unsupportedTokens.includes('whitefly'),
  `onTopic=${whiteflyRelevance.onTopicCount} unsupported=${whiteflyRelevance.unsupportedTokens.join(',')}`,
);
check(
  'whitefly treatment is flagged polluted with a zero share',
  whiteflyRelevance.level === 'polluted' && whiteflyRelevance.share === 0,
  `level=${whiteflyRelevance.level} share=${whiteflyRelevance.share}`,
);

check(
  'tomato blight is flagged partial: the disease token appears in no title',
  tomatoBlightRelevance.level === 'partial' && tomatoBlightRelevance.unsupportedTokens.includes('blight'),
  `level=${tomatoBlightRelevance.level} unsupported=${tomatoBlightRelevance.unsupportedTokens.join(',')}`,
);
// This is why the share alone is not the guard: every one of these eight titles says "Tomato", so a
// share-only metric reads the query as perfectly on-topic while the results are a wallpaper, a VPN
// and a manga reader.
check(
  'tomato leaf curl scores a full share yet is still flagged, because "curl" is in no title',
  tomatoCurlRelevance.share === 1 && tomatoCurlRelevance.level === 'partial' && tomatoCurlRelevance.unsupportedTokens.includes('curl'),
  `share=${tomatoCurlRelevance.share} level=${tomatoCurlRelevance.level} unsupported=${tomatoCurlRelevance.unsupportedTokens.join(',')}`,
);

// The control. A guard that flags everything is as useless as one that flags nothing, so a crop
// query whose results really are disease tools must come back clean and unwarned.
check(
  'tomato disease is NOT flagged (clean control)',
  tomatoDiseaseRelevance.level === 'on-topic' && tomatoDiseaseRelevance.suspect === false && tomatoDiseaseRelevance.warning === null,
  `level=${tomatoDiseaseRelevance.level} suspect=${tomatoDiseaseRelevance.suspect} warning=${JSON.stringify(tomatoDiseaseRelevance.warning)}`,
);
check(
  'tomato disease share is above the on-topic threshold',
  tomatoDiseaseRelevance.share >= 0.65 && tomatoDiseaseRelevance.unsupportedTokens.length === 0,
  `share=${tomatoDiseaseRelevance.share} unsupported=${tomatoDiseaseRelevance.unsupportedTokens.join(',')}`,
);

// A query of nothing but generic words has no evidence to measure, and "unmeasurable" must never be
// rendered as a pass: the guard cannot claim it checked something it could not check.
const genericRelevance = analyzeRelevance('plant guide app', titlesOf(TOMATO_DISEASE));
check(
  'a generic-only query reports unmeasurable rather than a fabricated share',
  genericRelevance.level === 'unmeasurable' && genericRelevance.share === null && genericRelevance.suspect === false,
  `level=${genericRelevance.level} share=${genericRelevance.share}`,
);
const emptyRelevance = analyzeRelevance('late blight', []);
check(
  'no visible results reports unmeasurable with a null share',
  emptyRelevance.level === 'unmeasurable' && emptyRelevance.share === null && emptyRelevance.total === 0,
  `level=${emptyRelevance.level} share=${emptyRelevance.share}`,
);

// The guard is an annotation, not a rewrite. This reproduces the exact defect end to end: the same
// eight cards, screened through the real `scanQuery`, must still produce the `candidate` verdict
// they produced the day the false GO was issued - with the pollution warning attached beside it.
const lateBlightHtml = LATE_BLIGHT.map((entry, index) =>
  card({
    id: `com.fixture.app${index}`,
    title: entry.title,
    developer: entry.developer,
    rating: entry.rating,
  }),
).join('');

const fakeResponse = { ok: true, status: 200, text: async () => lateBlightHtml };
const screenedLateBlight = await scanQuery('late blight', { fetchImpl: async () => fakeResponse });

check(
  'the verdict contract is unchanged: the polluted query still reports candidate',
  screenedLateBlight.verdict === 'candidate',
  `verdict=${screenedLateBlight.verdict} reason=${JSON.stringify(screenedLateBlight.reason)}`,
);
check(
  'the scan result carries the relevance guard',
  screenedLateBlight.relevance?.level === 'polluted' && screenedLateBlight.relevance?.suspect === true,
  `relevance=${JSON.stringify(screenedLateBlight.relevance)}`,
);
check(
  'formatTable surfaces the flag in the CLI table',
  formatTable([screenedLateBlight]).includes('POLLUTED'),
  formatTable([screenedLateBlight]),
);

console.log('');
if (failures === 0) {
  console.log('PLAY-SCAN REGRESSION: all checks passed');
} else {
  console.log(`PLAY-SCAN REGRESSION: ${failures} check(s) failed`);
}
process.exit(failures === 0 ? 0 : 1);
