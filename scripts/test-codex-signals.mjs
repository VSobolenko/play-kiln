/**
 * Regression tests for the Codex worker failure classifier.
 *
 * Two defects motivated this file, both the same defect:
 *   - scanning the whole transcript made the harness report its own documents as failures;
 *   - bounding it to the tail still matched prose, so `ai-angle` was reported `BLOCKED(AUTH_ERROR)`
 *     because its deliverable contained the phrase "unauthorized spend attempts".
 *
 * So there are two things to prove, and the second matters as much as the first:
 *   1. prose must NOT produce a signal   (precision — a false signal discards finished work);
 *   2. a real error MUST still produce the right signal   (recall — missing one is the original bug).
 *
 * The second half of this file replays the classifier over every worker log this repo has actually
 * recorded, and prints the old label next to the new one. Real logs are the only evidence that the
 * fix does not simply suppress everything.
 *
 * Run: node scripts/test-codex-signals.mjs
 */
import { readFileSync, existsSync, readdirSync } from 'node:fs';
import { join, resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { diagnose, classifyRun, DIAGNOSIS_WINDOW_LINES } from './lib/codex-signals.mjs';

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const STATE = join(ROOT, '.kiln', 'codex');

let failures = 0;

function check(name, actual, expected) {
  const ok = actual === expected;
  if (!ok) failures += 1;
  console.log(`  ${ok ? 'PASS' : 'FAIL'}  ${name}`);
  if (!ok) console.log(`        expected ${JSON.stringify(expected)}, got ${JSON.stringify(actual)}`);
}

/**
 * The classifier as it existed before the fix, kept here as the reference "before" behaviour.
 *
 * Explicitly labelled a defect reference: this is what must NOT come back. It is duplicated rather
 * than imported because its whole point is that the real module no longer behaves this way.
 */
const DEFECTIVE_PATTERNS = [
  [/usage limit|rate limit|429|too many requests|quota exceeded|limit reached/, 'RATE_LIMITED'],
  [/context (window|length)|too many tokens|token limit|compact/, 'CONTEXT_LIMIT'],
  [/not logged in|unauthorized|invalid api key|refresh token|401/, 'AUTH_ERROR'],
];

function diagnoseDefective(text) {
  const t = String(text || '').toLowerCase();
  if (!t) return null;
  for (const [pattern, signal] of DEFECTIVE_PATTERNS) {
    if (pattern.test(t)) return signal;
  }
  return null;
}

console.log('=== precision: prose must not be read as a failure ===');

// Verbatim fragments from documents this factory's own workers wrote.
check(
  'a fraud section saying "unauthorized spend attempts" is not AUTH_ERROR',
  diagnose('| Fraud/abuse | Server logs show duplicate grants, unauthorized spend attempts, or cost incurred without an authorized debit. |'),
  null,
);
check(
  'prose saying "compact notation" is not CONTEXT_LIMIT',
  diagnose('Compact notation and small examples do not count as visualizations.'),
  null,
);
check(
  'a doc discussing 401 responses is not AUTH_ERROR',
  diagnose('A 401 from the gateway means the client must re-authenticate before retrying.'),
  null,
);
check(
  'a doc that merely mentions "rate limit" as a topic is not RATE_LIMITED',
  diagnose('3. **Rate limits / quota model**, and whether there are hard concurrency limits an app must handle.'),
  null,
);
check('empty input yields no signal', diagnose(''), null);
check('ordinary output yields no signal', diagnose('Wrote 3 files. All tests pass.'), null);
// The sandbox failure is the newest signal, and it has the newest failure mode to guard against: a
// document that *describes* the Windows sandbox must not be read as a worker that died in it, or
// every note about this very defect would discard the work of whoever wrote it.
check(
  'a doc describing the Windows sandbox problem is not a failure',
  diagnose('The Windows sandbox on this machine cannot spawn a child process, so the reviewer could not read the tree.'),
  null,
);
check(
  'a doc recommending danger-full-access is not a failure',
  diagnose('Run the worker with `--sandbox danger-full-access`; the sandboxed modes fail with Win32 error 1920.'),
  null,
);

console.log('\n=== recall: real errors must still be caught ===');

check(
  'the real usage-limit line is RATE_LIMITED',
  diagnose("ERROR: You've hit your usage limit. Upgrade to Pro (https://chatgpt.com/codex/pricing) or try again later."),
  'RATE_LIMITED',
);
check(
  'a stream 401 is AUTH_ERROR',
  diagnose('stream error: unexpected status 401 Unauthorized'),
  'AUTH_ERROR',
);
check(
  'status 429 is RATE_LIMITED',
  diagnose('request failed: status 429 too many requests'),
  'RATE_LIMITED',
);
check(
  'an update failure with 403 is AUTH_ERROR',
  diagnose('ERROR: unexpected status 403 Forbidden'),
  'AUTH_ERROR',
);
check(
  'context window exceeded is CONTEXT_LIMIT',
  diagnose('ERROR: context window exceeded, please start a new session'),
  'CONTEXT_LIMIT',
);
check(
  'a sign-in instruction is AUTH_ERROR',
  diagnose('ERROR: not logged in. Please run `codex login`.'),
  'AUTH_ERROR',
);
check(
  'an explicit auth instruction line is AUTH_ERROR',
  diagnose('You are not logged in. Run codex login to continue.'),
  'AUTH_ERROR',
);
// The false `DONE` of 2026-09-29, verbatim from the recorded probe transcript. Every command failed
// before it started, the run exited 0, and the ledger recorded `DONE` — because the failure line is
// timestamped (`2026-…Z ERROR …`) and the diagnostic gate only accepted a level at the line start.
check(
  'a timestamped sandbox spawn failure is SANDBOX_UNAVAILABLE',
  diagnose(
    '2026-09-29T20:25:55.590481Z ERROR codex_core::exec: exec error: windows sandbox: runner failed during SpawnChild: CreateProcessAsUserW failed: 1920 (Доступ к этому файлу из системы отсутствует.) | cwd=D:\\Python\\PlayKiln | creation_flags=525312 (Windows error 1920)',
  ),
  'SANDBOX_UNAVAILABLE',
);
check(
  'the router wrapping the same failure is SANDBOX_UNAVAILABLE',
  diagnose(
    '2026-09-29T20:25:55.591395Z ERROR codex_core::tools::router: error=execution error: Io(Custom { kind: Other, error: "windows sandbox: runner failed during SpawnChild: CreateProcessAsUserW failed: 1920" })',
  ),
  'SANDBOX_UNAVAILABLE',
);
check(
  'a bare CreateProcessAsUserW failure is SANDBOX_UNAVAILABLE',
  diagnose('ERROR: CreateProcessAsUserW failed: 1920 | cmd=pwsh.exe'),
  'SANDBOX_UNAVAILABLE',
);
// The gate was widened to consider `exec error:` lines, so it must not start inventing signals from
// them: a tool call that failed for an ordinary reason is a defect in the task, not a run that could
// not reach the repository. This is the precision half of the same change.
check(
  'an ordinary exec error is still not a signal',
  diagnose('ERROR: exec error: spawn git ENOENT'),
  null,
);

console.log('\n=== classification precedence ===');

check('signal beats a zero exit code', classifyRun(0, 'ERROR: You\'ve hit your usage limit.'), 'blocked');
check('a clean zero exit is done', classifyRun(0, 'Wrote 3 files.'), 'done');
check('a non-zero exit with no signal is failed', classifyRun(1, 'spawn error: ENOENT'), 'failed');
// The defect this signal exists for: a run that executed nothing, exited 0, and was recorded as
// delivery. `blocked` is the honest verdict, and it keeps the file-on-disk check that `AGENTS.md`
// demands in place instead of letting a label claim work that was never done.
check(
  'a run whose every command died in the sandbox is blocked, not done',
  classifyRun(
    0,
    'I\'ll run the two read-only commands exactly as requested.\n2026-09-29T20:25:55.590481Z ERROR codex_core::exec: exec error: windows sandbox: runner failed during SpawnChild: CreateProcessAsUserW failed: 1920 (Windows error 1920)',
  ),
  'blocked',
);

console.log('\n=== replay over every recorded worker log ===');

if (!existsSync(STATE)) {
  console.log('  (no recorded state at .kiln/codex — skipping replay)');
} else {
  const ids = [...new Set(
    readdirSync(STATE)
      .filter((f) => f.endsWith('.err') || f.endsWith('.log'))
      .map((f) => f.replace(/\.(err|log)(\.attempt\d+)?$/, '')),
  )].sort();

  console.log('  worker                     old(defective)   new(fixed)');
  console.log('  -------------------------  ---------------  ----------------');

  const changed = [];

  for (const id of ids) {
    let text = '';
    for (const suffix of ['.err', '.log']) {
      const file = join(STATE, `${id}${suffix}`);
      if (existsSync(file)) {
        text += readFileSync(file, 'utf8').split(/\r?\n/).slice(-DIAGNOSIS_WINDOW_LINES).join('\n');
      }
    }
    const before = diagnoseDefective(text) ?? '-';
    const after = diagnose(text) ?? '-';
    if (before !== after) changed.push(`${id}: ${before} -> ${after}`);
    console.log(`  ${id.padEnd(25)}  ${before.padEnd(15)}  ${after}`);
  }

  console.log('');
  if (changed.length === 0) {
    console.log('  no label changed on recorded logs — the fix is inert on real data, which would mean');
    console.log('  this test cannot demonstrate the defect. Treat that as a failure of the test.');
    failures += 1;
  } else {
    console.log(`  ${changed.length} label(s) changed on real recorded logs:`);
    for (const line of changed) console.log(`    - ${line}`);
  }
}

console.log('');
if (failures === 0) {
  console.log('CODEX SIGNAL REGRESSION: all checks passed');
} else {
  console.log(`CODEX SIGNAL REGRESSION: ${failures} check(s) failed`);
}
process.exit(failures === 0 ? 0 : 1);
