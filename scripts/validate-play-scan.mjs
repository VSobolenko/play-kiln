/**
 * Validates the market-scan sensor against verdicts obtained by hand in a browser.
 *
 * A screen nobody has calibrated is worse than no screen: it produces confident verdicts with no
 * evidence that they correspond to anything. Seven queries were read manually on Play and their
 * verdicts recorded (docs/niche-selection.md) before this mechanism existed. Where the mechanism
 * disagrees, the disagreement is either a real defect in the rule or a stated blind spot — never
 * silently absorbed.
 *
 * Since 2026-09-23 the scan result also carries `relevance`, the guard against Play's fuzzy text
 * match (see `scripts/lib/play-scan.mjs`). It is printed here rather than gated, because the
 * verdict contract is deliberately unchanged: a polluted query still reports the verdict its
 * (polluted) numbers produce, with the warning beside it.
 *
 * Run: node scripts/validate-play-scan.mjs
 */
import { scanQueries, formatTable } from './lib/play-scan.mjs';

/**
 * Expected verdicts under the definitions stated in scripts/lib/play-scan.mjs.
 *
 * One entry is a **revision of the earlier manual reading**:
 * `electrician calculator` was called `candidate` by hand, but the data shows 10 of 16 rated
 * incumbents at >= 4.5 with a 5M+ install leader. Six weak incumbents out of sixteen is not a
 * majority, so by definition the field is `mixed`: demand is proven and a pocket exists, but the
 * strong players dominate and entry needs a specific angle rather than a better general app.
 * The earlier label was too optimistic, and the mechanism is right.
 */
const EXPECTED = {
  'electrician calculator': 'mixed',
  'visual schedule autism': 'candidate',
  'habit tracker': 'fortress',
  'shift work calendar': 'fortress',
  'fasting tracker': 'fortress',
  'bjj training log': 'dead-zone',
};

/**
 * Fields where this sensor is blind by construction and a human must decide.
 *
 * Two classes live here, and they fail differently:
 *
 * - `reputation` — the verdict is well founded on the wrong axis. `first aid guide` is held by the
 *   American Red Cross (4.8), the IFRC, the UN and the National Safety Council. Ratings alone read
 *   the field as `mixed`; reputation makes it a fortress. The mechanism cannot see reputation, so it
 *   reports the organisation-like publisher names for a human instead of pretending to judge them.
 *   The verdict itself still reproduces, so a change there IS a regression.
 *
 * - `query-token-pollution` — the verdict is not well founded at all. Play search is a fuzzy TEXT
 *   match, so a query can come back answering a fragment of itself, and the ratings arithmetic then
 *   runs over apps the query never described. For this class the verdict is NOT the thing to check:
 *   it is computed from off-topic apps, so it can read `candidate` today and `dead-zone` tomorrow
 *   with no change in any market. What must reproduce is the relevance guard flagging the query,
 *   which is why these entries carry `guardMust` instead of a verdict comparison.
 *
 * `mechanismSays` is therefore an OBSERVATION for the pollution class — recorded from a real run and
 * printed, never compared. Honestly: **`late blight` currently returns `candidate`**, and the
 * `candidate` label is the dangerous part of this defect, because the factory acts on a GO.
 */
const BLIND_SPOTS = {
  'first aid guide': {
    kind: 'reputation',
    mechanismSays: 'mixed',
    humanSays: 'fortress',
    why: 'institutional publishers (Red Cross, IFRC, UN, National Safety Council) are invisible to a ratings-only screen',
  },
  'late blight': {
    kind: 'query-token-pollution',
    mechanismSays: 'candidate',
    humanSays: 'unusable — the query does not name a niche',
    guardMust: 'polluted',
    why: 'returned `Lands of Blight`, `BE LIGHT: Calm & Focus Boost`, `Belight Community` and `L&S LITE BT`; `blight` shares the letters of `light` with all of them, so this shelf answers the query\'s spelling rather than its topic',
  },
  'tomato blight': {
    kind: 'query-token-pollution',
    mechanismSays: 'mixed',
    humanSays: 'unusable — only "tomato" survives the match',
    guardMust: 'flagged',
    why: 'returned `Snappy Tomato Pizza`, `Tomato Focus: Pomodoro Timer` and a Harry Potter game; the query\'s disease token "blight" appears in no visible title',
  },
  'tomato leaf curl': {
    kind: 'query-token-pollution',
    mechanismSays: 'mixed',
    humanSays: 'unusable — only "tomato" survives the match',
    guardMust: 'flagged',
    why: 'returned `Tomato Wallpaper`, `Tomato Lite - Secure VPN Proxy` and `Tomato Manga`; every title says "Tomato" yet "curl" is in none, which is why a share-only metric cannot be the guard',
  },
  'whitefly treatment': {
    kind: 'query-token-pollution',
    mechanismSays: 'dead-zone',
    humanSays: 'unusable — the pest is absent from the result set',
    guardMust: 'polluted',
    why: 'returned `WhiteFlag: Mental Health Help`, `Vtalix: Therapy for Everyone` and `Whitecafe`: every title starts with `White`, which is all the pest name shares with them, so this dead zone is a dead zone of white-named apps',
  },
};

/**
 * The guard's side of a pollution declaration.
 *
 * `flagged` accepts either level, because whether a polluted set lands in `polluted` or `partial`
 * depends on how many cards Play happened to render — a threshold artefact, not a market fact.
 * Naming an exact level where the exact level is what matters (the two `polluted` cases) still works.
 */
function guardSatisfies(relevance, guardMust) {
  if (!relevance) return false;
  if (guardMust === 'flagged') return relevance.suspect === true;
  return relevance.level === guardMust;
}

/** Renders the guard's verdict on one query for the printed declaration. */
function relevanceSummary(relevance) {
  if (!relevance) return 'not measured';
  const echoes = `${relevance.onTopicCount}/${relevance.total} titles echo the query`;
  const missing = relevance.unsupportedTokens.length > 0 ? `, no title says ${relevance.unsupportedTokens.join('/')}` : '';
  return `${relevance.level} (${echoes}${missing})`;
}

const queries = [...Object.keys(EXPECTED), ...Object.keys(BLIND_SPOTS)];
const scans = await scanQueries(queries, { concurrency: 4 });

console.log(formatTable(scans));
console.log('');

let failures = 0;

for (const scan of scans) {
  if (scan.error) {
    console.log(`ERROR   ${scan.query.padEnd(26)} ${scan.error}`);
    failures += 1;
    continue;
  }

  if (EXPECTED[scan.query]) {
    const expected = EXPECTED[scan.query];
    const ok = scan.verdict === expected;
    if (!ok) failures += 1;
    console.log(`${ok ? 'MATCH  ' : 'DIFFER '} ${scan.query.padEnd(26)} expected=${expected.padEnd(10)} got=${scan.verdict}`);
  }
}

console.log('\n--- declared blind spots (not counted as passes) ---');
for (const [query, spot] of Object.entries(BLIND_SPOTS)) {
  const scan = scans.find((s) => s.query === query);
  const got = scan?.error ? 'ERROR' : scan?.verdict;
  const relevance = scan?.relevance ?? null;

  if (spot.kind === 'reputation') {
    const asExpected = got === spot.mechanismSays;
    console.log(`${asExpected ? 'AS DECLARED' : 'CHANGED    '} ${query}  mechanism=${got} human=${spot.humanSays}`);
    if (!asExpected) failures += 1;
  } else {
    const asExpected = guardSatisfies(relevance, spot.guardMust);
    console.log(
      `${asExpected ? 'AS DECLARED' : 'NOT FLAGGED'} ${query}  mechanism=${got} (observed, not compared)  guard=${relevanceSummary(relevance)}`,
    );
    if (!asExpected) failures += 1;
  }

  console.log(`             kind: ${spot.kind} — ${spot.why}`);
  if (scan?.organisations?.length) {
    console.log(`             surfaced publishers: ${scan.organisations.join('; ')}`);
  }
}

// Informational, not a gate: the EXPECTED verdicts above still have to reproduce, but one that is
// computed from a polluted result set is worth seeing rather than hiding. A definitional verdict on
// a polluted query is arithmetic over the wrong apps.
const warnedExpected = scans.filter((scan) => !scan.error && EXPECTED[scan.query] && scan.relevance?.suspect);
if (warnedExpected.length > 0) {
  console.log('\n--- note: definitional verdicts carrying a relevance warning (informational, not a failure) ---');
  for (const scan of warnedExpected) {
    console.log(`${scan.query.padEnd(26)} verdict=${scan.verdict.padEnd(10)} ${relevanceSummary(scan.relevance)}`);
    console.log(`                           ${scan.relevance.warning}`);
  }
}

console.log('');
const spotCounts = Object.values(BLIND_SPOTS).reduce((counts, spot) => {
  counts[spot.kind] = (counts[spot.kind] ?? 0) + 1;
  return counts;
}, {});
console.log(
  failures === 0
    ? `SENSOR VALIDATED: ${Object.keys(EXPECTED).length} definitional verdicts reproduce, and ${Object.keys(BLIND_SPOTS).length} blind spot(s) behave as declared (${spotCounts.reputation} reputation, ${spotCounts['query-token-pollution']} query-token-pollution)`
    : `SENSOR FAILED: ${failures} disagreement(s)`,
);
process.exit(failures === 0 ? 0 : 1);
