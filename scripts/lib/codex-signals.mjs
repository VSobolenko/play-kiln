/**
 * Failure-signal diagnosis for Codex workers — the single source of truth.
 *
 * Why this is its own module
 * --------------------------
 * This logic was wrong twice, in the same way, and both times the harness believed it.
 *
 * 1. It scanned the **whole** transcript. That transcript echoes the prompt and every file the
 *    worker read, and the repo's own documents name the keywords — `AGENTS.md` says
 *    `RATE_LIMITED`, `harness/lessons.md` names all three. So reading the harness made the harness
 *    report that the harness had failed. A worker that produced no files was reported `DONE`.
 * 2. Bounding the scan to the **tail** did not fix it, because the tail of a worker's output is
 *    often the document it just wrote. `ai-angle` was reported `BLOCKED(AUTH_ERROR)` on the
 *    strength of the phrase *"unauthorized spend attempts"* in its own fraud section, while the
 *    task had in fact been delivered. A false signal is not harmless: it discards finished work
 *    and, under the ladder in `AGENTS.md`, can hand a satisfied task to a different writer.
 *
 * Both defects share one root cause: **matching bare words against prose.** A token like `401`,
 * `compact`, or `unauthorized` occurs constantly in technical writing about authentication,
 * concurrency and fraud. The words are only meaningful inside an actual error line.
 *
 * The rule here
 * -------------
 * A signal counts only if it appears on a line that *looks like a diagnostic* — an explicit error
 * prefix, a stream/API/HTTP error, an HTTP status in an error context, or an auth instruction.
 * Signal keywords are deliberately NOT matched anywhere else. This is a narrower sensor by design:
 * a missed quota error is a nuisance, a missed real error is worse, but a signal invented from
 * prose throws away completed work, so the trade is made toward precision.
 *
 * Being importable matters: `scripts/codex-run.mjs` is a CLI with a top-level dispatch, so its
 * functions cannot be imported without running it. Keeping this in `scripts/lib` makes the
 * classifier testable — see `scripts/test-codex-signals.mjs`.
 */

/**
 * How much of the end of a run is scanned.
 *
 * Bounded on purpose. This is a second line of defence only; the primary one is that a signal must
 * land on a diagnostic-shaped line.
 */
export const DIAGNOSIS_WINDOW_LINES = 40;

/**
 * Lines that may carry a real failure signal.
 *
 * Matched on shape, not on topic: an error prefix, a transport/API error, an HTTP status where a
 * status is expected, or an explicit sign-in instruction. Ordinary prose never matches.
 */
const DIAGNOSTIC_LINE = new RegExp(
  [
    // "ERROR: ...", "error: ...", "FATAL", "panic:" — how CLIs actually report failure.
    String.raw`^\s*(?:ERROR|FATAL|PANIC|SEVERE)\b`,
    // Bare "error:" / "failed:" at the start of a line, which wrappers often emit.
    String.raw`^\s*(?:error|failed|failure)\s*:`,
    // Transport- or API-level failure phrasing.
    String.raw`stream error|api error|http error|internal error|unexpected status|request failed`,
    // A status code where a status is expected: "status 401", "status: 403", "code=429".
    String.raw`(?:status|code)\s*[:=]?\s*(?:4\d\d|5\d\d)\b`,
    // Explicit sign-in instruction, which is how an auth failure normally surfaces. Deliberately
    // narrow: `re-authenticate` and `log in again` were tried here and had to be removed, because
    // documentation *about* authentication uses them. A test caught that. These two forms are what
    // the CLI actually prints when the session is unusable.
    String.raw`not logged in|codex login`,
  ].join('|'),
  'i',
);

/**
 * Signal patterns, applied only to diagnostic lines.
 *
 * Each is anchored on wording a real error uses. `compact` was removed from the context test: it is
 * an everyday English word ("compact notation") and was a false-positive source on its own.
 */
const SIGNALS = [
  {
    signal: 'RATE_LIMITED',
    // "You've hit your usage limit. Upgrade to Pro" is the real phrasing observed in this repo.
    pattern: /usage limit|rate limit|too many requests|quota exceeded|quota exhausted|limit reached|\b429\b/,
  },
  {
    signal: 'CONTEXT_LIMIT',
    pattern: /context (?:window|length|limit)|too many tokens|token limit|conversation too long|context_?length_exceeded|compact(?:ed|ion)? the conversation/,
  },
  {
    signal: 'AUTH_ERROR',
    pattern: /not logged in|unauthori[sz]ed|invalid api key|api key (?:is )?(?:invalid|expired|missing)|refresh token|authentication failed|please (?:log|sign) in|\b401\b|\b403\b/,
  },
]

/**
 * Returns the failure signal for a run's output, or `null` when there is none.
 *
 * Only diagnostic-shaped lines are considered, so a document that discusses authentication,
 * concurrency or fraud cannot be mistaken for a worker that hit one of those limits.
 */
export function diagnose(text) {
  const lines = String(text || '').split(/\r?\n/);
  const diagnosticLines = lines.filter((line) => DIAGNOSTIC_LINE.test(line));
  if (diagnosticLines.length === 0) return null;

  const haystack = diagnosticLines.join('\n').toLowerCase();
  for (const { signal, pattern } of SIGNALS) {
    if (pattern.test(haystack)) return signal;
  }
  return null;
}

/**
 * Classifies a run from its exit code and its output.
 *
 * A diagnosed signal outranks a zero exit code: a worker that stopped on a limit has not delivered,
 * and calling that `done` is the false confidence this module exists to prevent. The reverse must
 * also hold, which is why `diagnose` errs toward silence.
 */
export function classifyRun(exitCode, output) {
  const signal = diagnose(output);
  if (signal) return 'blocked';
  return exitCode === 0 ? 'done' : 'failed';
}

/** Exposed for tests and for callers that want to explain why a classification happened. */
export function diagnosticLinesOf(text) {
  return String(text || '')
    .split(/\r?\n/)
    .filter((line) => DIAGNOSTIC_LINE.test(line));
}
