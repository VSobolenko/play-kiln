# Session audit — 2026-09-24

Independent verification of work that landed in the repo in the hour before this run, made by
three writers who ended without a usable handoff (one exited 1 with no final message; two replied
only `DONE <file>`). Their deliverables are treated here as **claims**, not verified work.

- **Method:** read the code, then run only read-only commands (`node scripts/selftest.mjs`,
  `node scripts/test-codex-signals.mjs`, `node --input-type=module -e`, `python -c` / `python -`,
  `python scripts/store-kit.py --slot … --validate-only`, `node scripts/verify-slot.mjs --slot …`,
  `git status/diff/log`). No Gradle, no git mutations, no installs, no writes except this file.
- **Rule followed:** where a claim could only be tested by writing or by running Gradle, it is
  marked `unverified:` rather than endorsed.
- The single most serious residual risk is in §1(b): the listing-`idea` fix does not reach either
  existing slot (details and fix at the end).

## Verdicts

| # | Subject | Verdict |
|---:|---|---|
| 1 | `scripts/store-kit.py` — (a) icon mask predicate, (b) `recorded_idea`, (c) `truncate_chars` | **PASS** (with residuals) |
| 2 | `scripts/verify-slot.mjs` — build step now via `runGradleTasks` | **PASS** |
| 3 | `scripts/selftest.mjs` — "gradle invocation contract" wrapper sensor | **FAIL** (sensor is defeatable) |
| 4 | `AGENTS.md` + `harness/codex-workers.md` — Codex-first rule and ledger verdicts | **PASS** |
| 5 | `docs/research/2026-09-22-android-play-requirements.md` + store-pack table | **PASS** (residual: `play-assets.mjs` still says 7680) |
| 6 | `openspec/changes/add-visual-schedule-slot/` | **PASS** (residual: `README.md`/`.openspec.yaml` stale) |
| 7 | `docs/research/2026-09-23-store-asset-contract-audit.md` — "both slots fail" claim | **FAIL** (claim refuted for `pipe-proof`) |

---

## 1. `scripts/store-kit.py` — three changes

### (a) Full-bleed icon + `require_nontransparent_corners` — **PASS**, predicate can fail

`render_icon()` (`scripts/store-kit.py:611-620`) now builds an opaque `gradient(...).convert("RGBA")`
and saves it directly; the only drawing is the glyph (`draw_abstract_mark`, `:597-608`). The old
`mask = Image.new("L", base.size, 0)` + `rounded_rectangle(..., 112, fill=255)` + `canvas.paste(base,
(0,0), mask)` composite is gone.

`inspect_image()` gained the predicate at `scripts/store-kit.py:1095-1099`:

```1095:1099:scripts/store-kit.py
            if require_nontransparent_corners and image.mode == "RGBA":
                alpha = image.getchannel("A")
                corners = ((0, 0), (image.width - 1, 0), (0, image.height - 1), (image.width - 1, image.height - 1))
                if any(alpha.getpixel(corner) == 0 for corner in corners):
                    failures.append("contains fully transparent corner pixels; drawn rounded masks are forbidden")
```

It is wired to **both** icon specs, root and per-locale (`scripts/store-kit.py:1127` and `:1133`,
5th tuple element `True`).

**Measured that it can fail, on existing files (no repo mutation):** the older
`apps/2026-09-22-smoke` icon carries the drawn mask; the new `pipe-proof` icon does not.

```
apps/2026-09-22-smoke/store/assets/icon-512.png    plain: []    nontransparent: ['contains fully transparent corner pixels; drawn rounded masks are forbidden']   corners alpha: [0, 0, 0, 0]  alpha==0 px 41683
apps/2026-09-23-pipe-proof/store/assets/icon-512.png plain: []  nontransparent: []                                                                             corners alpha: [255, 255, 255, 255] alpha==0 px 0
```

The 41 683 zero-alpha pixels independently reproduce the earlier audit's number for the smoke icon,
and the predicate is the only check in `store-kit.py` that fires on it.

Residual weaknesses (reasoned, not measured — measuring them would need a written file):
the predicate tests only the four 1×1 corner pixels for `alpha == 0`. A mask with a small radius,
or anti-aliasing that leaves a corner at alpha 1–254, passes; and the contract's "or shadow" is not
checked at all. It is a reasonable proxy, not a proof.

### (b) `recorded_idea()` on the CLI path, and its fallbacks — **PASS**, but it cannot reach an existing listing

`recorded_idea()` is defined at `scripts/store-kit.py:183-221` and is called by `slot_metadata()`
at `:251`, which `main()` calls at `:1172` **before** any listing is built (`:1197-1200`). So it is
on the CLI path. Because `store-kit.py` writes on every non-`--validate-only` form, I exercised the
exact function the create path calls rather than run generation; **the path I exercised was
`main → slot_metadata → recorded_idea`**, which is the same path that `default_listing()` consumes
when `existing is None`.

Every branch was exercised (read-only):

| Branch (store-kit.py) | How exercised | Result |
|---|---|---|
| `.kiln-state.json` `idea` (`:184-197`) | `recorded_idea()` on both real slots | `pipe-proof` → `"Throwaway pipeline proof: … no hand edits."` (its `.kiln-state.json` value), not the listing's stale string |
| `registry/apps.json` by `id == slug` (`:199-210`) | nonexistent slot dir + slug `pipe-proof`, no strings | returns the registry `idea` |
| `strings.xml` `app_idea` (`:212-221`) | nonexistent slot dir + unknown slug + real `apps/2026-09-23-pipe-proof/.../strings.xml` | returns `"Throwaway pipeline proof: … no hand edits."` |
| README/AGENTS prose (`:252-254`) | monkeypatched `recorded_idea` to return `""` and called `slot_metadata` | returns `"PlayKiln slot — isolated Android attempt. Factory harness lives at the repo root."` (README prose) |

So the function is live and every fallback still works.

**Residual / most serious finding.** The generator preserves an existing `listing.json`
(`listing = existing`, `scripts/store-kit.py:1200`) and never refreshes `idea` or locale copy from
the regenerated metadata. Both real slots already have a `listing.json`, so neither benefits:

```
apps/2026-09-23-pipe-proof/store/listing.json  "idea": "PlayKiln slot — isolated Android attempt. Factory harness lives at the repo root."
apps/2026-09-23-pipe-proof/.kiln-state.json    "idea": "Throwaway pipeline proof: scaffold to signed APK and AAB to store pack to verified gates, with no hand edits."
smoke en-US shortDescription len 80: 'PlayKiln slot … Factory harness lives at the repo roo…'
```

The store copy that would ship is still factory scaffolding, and the `en-US` short description is
still mid-word-truncated at 80/80 ("repo roo…") because that string was frozen before `truncate_chars`
was fixed. The new word-boundary `truncate_chars` also never re-runs against these listings.
**Fix for the owning agent:** on generation, when `listing.idea`/locale copy differs from
`meta`/`english_copy`, refresh it (or make `--init` the only writer of prose and have generation
re-derive `idea` when `.kiln-state.json` is newer). Do not rely on the `existing` short-circuit for a
copy fix.

### (c) `truncate_chars` — **PASS**

`scripts/store-kit.py:269-281`. Measured (`python -`, importing the module; the ellipsis rendered as
`?` only because of console encoding — `len()` is authoritative):

| input | limit | result | len |
|---|---:|---|---:|
| `hello world foo` | 10 | word boundary cut to `hello…` | 6 |
| `supercalifragilistic` | 5 | single word > limit → hard `supe…` | 5 |
| `abc` | 1 | `…` | 1 |
| `a` | 1 | `a` (already ≤ 1) | 1 |
| `abc` | 0 | `""` | 0 |
| `abc` | -5 | `""` | 0 |
| `two words here` | 8 | `two…` (drops the whole middle word) | 4 |
| `  hello   world  ` | 7 | normalized, `hello…` | 6 |
| `abcdef` | 6 | `abcdef` (no ellipsis when it fits) | 6 |
| `abcdef` | 5 | `abcd…` | 5 |
| `ab cdef` | 4 | `ab…` (never exceeds the limit) | 3 |
| `""` | 5 | `""` | 0 |

Every returned length is ≤ limit; `limit <= 0` → `""`; `limit == 1` → `…`; a single word longer than
the limit is hard-cut rather than dropped to `""` (correct). No defect found.

---

## 2. `scripts/verify-slot.mjs` — build step via `runGradleTasks` — **PASS**

`gradleAssemble` (`scripts/verify-slot.mjs:131-146`) now:

```131:146:scripts/verify-slot.mjs
function gradleAssemble(slotDir) {
  let gradleRun
  try {
    gradleRun = runGradleTasks(slotDir, 'assembleDebug')
  } catch (error) {
    const out = error?.message || String(error)
    return { ok: false, missing: /^gradlew(?:\.bat)? not found$/.test(out), out }
  }
  const { result } = gradleRun
  const out = `${result.stdout || ''}${result.stderr || ''}`.trim()
  if (result.error) {
    return { ok: false, missing: result.error.code === 'ENOENT', out: result.error.message }
  }
  return { ok: result.status === 0, status: result.status, out }
}
```

Against `scripts/lib/gradle.mjs`:

- **Same task.** `normalizeTasks('assembleDebug')` → `['assembleDebug']`, then
  `gradleArgs` appends `--console=plain`. The task is still `assembleDebug`; the old invocation's
  `--quiet` is replaced by `--console=plain` (more output, same task). No new caller flag is
  required — `gradleAssemble` passes only `(slotDir, 'assembleDebug')`.
- **Wrapper-not-found preserved.** `runGradleTasks` throws `` `${path.basename(wrapper)} not found` ``
  (`scripts/lib/gradle.mjs:77`), i.e. `gradlew.bat not found` on win32 / `gradlew not found`
  elsewhere, which the catch regex matches exactly → `missing: true` → the `BLOCKED` branch at
  `verify-slot.mjs:705-707`. The self-test independently asserts "a missing Gradle wrapper fails
  loudly instead of reporting success". A JDK-resolution failure cannot masquerade as a build
  failure: `resolveJavaHome` **returns `null`**, it does not throw (`scripts/lib/java.mjs:108-137`),
  so it cannot make `runGradleTasks` throw.
- **Same PASS/FAIL/SKIP line and verdict.** Build-less run measured:

```
$ node scripts/verify-slot.mjs --slot pipe-proof
  ...
  PASS        en-US valid phone screenshots: 6 (required 4-8)
  SKIP        assembleDebug - pass --build to compile
---
UNVERIFIED: build
VERDICT: UNVERIFIED
VERIFY_EXIT=2
```

  (Also `PASS factory scripts honour their contract (143 checks)` in the same run.) It reports
  `UNVERIFIED` with exit 2, not PASS.

`unverified:` I did not execute the `--build` path (Gradle is forbidden here), so the actual
`PASS assembleDebug` / `BLOCKED` lines were verified by reading, not by running. The `--console=plain`
vs `--quiet` output difference is a real but benign behavior change.

---

## 3. `scripts/selftest.mjs` — wrapper sensor — **FAIL (defeatable)**

The sensor is `scripts/selftest.mjs:836-857`. `textFiles(join(ROOT, 'scripts'))` names entries
relative to `scripts/` (`textFiles` at `:72-90` uses `relative(directory, file)`), so the allow-list
entry `lib/gradle.mjs` genuinely matches and does hide `scripts/lib/gradle.mjs`. `node scripts/selftest.mjs`
passes (VERDICT: PASS, exit 0; the check line prints
`PASS Gradle wrappers are invoked only through scripts/lib/gradle.mjs`).

I replicated the sensor's exact regexes and fed them candidate snippets
(`node -` in-memory; no repo mutation):

```
FLAGGED  direct literal            spawnSync(path.join(dir, 'gradlew.bat'), args)
FLAGGED  direct literal var        const wrapper = path.join(dir, 'gradlew')\nspawnSync(wrapper, args)
MISSED   BYPASS wrapperFor         import { wrapperFor } from './lib/gradle.mjs'\nspawnSync(wrapperFor(dir), args)
MISSED   BYPASS split string       const p1 = 'grad'; const p2 = 'lew'\nspawnSync(p1 + p2, args)
MISSED   BYPASS aliased fn         const go = spawnSync\ngo(wrapper, args)
MISSED   BYPASS multi-line var     const base = 'gradlew'\nconst name = base + '.bat'\nspawnSync(name, args)
MISSED   indirect helper           function launch(cmd){ spawnSync(cmd, []) }\nlaunch(wrapperFor(dir))
```

Plausible bypasses it would miss, all realistic ways to reintroduce a direct launch:

1. **Indirect launch via the shared helper.** Any script may
   `import { wrapperFor } from './lib/gradle.mjs'` and `spawnSync(wrapperFor(dir), …)` without using
   `runGradleTasks`. `wrapperFor` never contains the literal `gradlew` on an assignment line, so no
   variable is captured and no literal is seen. (This is the exact pattern `gradle.mjs` itself uses.)
2. **Wrapper path assembled from pieces.** `'grad' + 'lew'`, or a multi-line
   `const base = 'gradlew'` / `const name = base + '.bat'`, breaks the single-line
   `const x = …gradlew` capture.
3. **Aliased launcher.** `const go = spawnSync; go(wrapper, …)` — `go` is not in
   `launcherPattern`, and `exec`/promisified `execAsync`/`child_process.exec` are absent from the
   list entirely (only `spawnSync|spawn|execFileSync|execFile|execSync|run`).
4. **Local helper.** `function launch(cmd){ spawnSync(cmd, []) }; launch(wrapperPath)`.

Also, the pattern is textual (comments and strings are not stripped), so it **can false-positive on
innocent data**:

```
FLAGGED  comment false-pos   // spawn('gradlew.bat') is forbidden
FLAGGED  string false-pos    const doc = "call run('gradlew') to build"
```

No such text exists in `scripts/` today (that is why the suite is green), but a docstring or comment
mentioning a launcher call would fail the suite for no real defect — a check that fails on prose is
the same failure class as the classifier bug in `harness/lessons.md:133`.

**Does the allow-list hide a real violation?** It does not hide one today — `scripts/lib/gradle.mjs`
launches only through `runGradleTasks`, and its `wrapperFor` uses a ternary inside `path.join`, so it
would not be flagged even without the allow-list. But allow-listing the **whole file** means any
future direct wrapper launch added to `gradle.mjs` is invisible.

**Fix for the owning agent:** make the rule positive instead of textual — require that every
`child_process` launch token in a non-allow-listed script is `runGradleTasks`, and that scripts
importing `wrapperFor` are also refused; or scan the AST. At minimum strip comments/strings before
matching, and flag imports of `wrapperFor` outside `gradle.mjs`.

---

## 4. `AGENTS.md` + `harness/codex-workers.md` — Codex-first rule and ledger verdicts — **PASS**

Every documented verdict and precedence claim matches `scripts/lib/codex-ledger.mjs` (measured with
`node --input-type=module -e`):

| Documented (`AGENTS.md:82-86`, `harness/codex-workers.md:88-92`) | `classifyWorkerState` / `classifyWorkerFromDisk` | Observed |
|---|---|---|
| `DONE` — exit 0, no signal | `record.exitCode === 0` after signal check (`:57-64`) | `DONE` |
| `FAILED(<code>)` — non-zero, no signal | `FAILED(${record.exitCode})` | `FAILED(2)` |
| `BLOCKED(<signal>)` — signal outranks any exit code | signal checked **first** (`:60`) | `{exitCode:0,signal:'RATE_LIMITED'}` → `BLOCKED(RATE_LIMITED)`; `{exitCode:3,signal:'AUTH_ERROR'}` → `BLOCKED(AUTH_ERROR)` |
| `LOST` — `running`, no exit record, pid not alive (incl. no usable pid) | only exact `status === 'running' && alive === false` (`:69`) | `running`+no record+dead → `LOST`; live → `running`; undetermined → `running`; `queued` → `queued` |
| (`LOST` note) | `LOST_NOTE` (`:42`) | `"(died without exit record — work unverified)"` |

`LOST` for a worker with **no recorded pid**, called directly:

```
classifyWorkerFromDisk({status:'running'}, null) => LOST
isProcessAlive(null) => false ; isProcessAlive(0) => false ; isProcessAlive('abc') => false
```

`codex-run.mjs` uses the classifier for the ledger it prints (`classifyWorkerFromDisk` at
`scripts/codex-run.mjs:322`; `LOST_NOTE` appended at `:327`). The referenced lesson exists
(`harness/lessons.md:133`, "the failure classifier reported finished work as blocked, because it
matched prose").

### Would the new rule have prevented the first error without blocking the second?

**Yes, on both counts — verified against the real ledger files.** The rule (`AGENTS.md:63-70`,
`harness/codex-workers.md:113-125`) says a `BLOCKED(...)` label is not evidence of unavailability,
that a fresh trivial probe must be run at the task's tier before falling back to a Cursor subagent,
and that fallback is per task.

- **First error (the misreading).** The event's own files confirm the facts the rule is aimed at:

```
.kiln/codex/tier-smoke.exit.json  {"exitCode": 0, "signal": null, "finishedAt": "2026-09-23T19:51:53.038Z"}
.kiln/codex/ai-angle.exit.json    {"exitCode": 0, "signal": "AUTH_ERROR", "finishedAt": "2026-09-23T19:55:17.844Z"}
.kiln/codex/ai-angle.meta.json    "status": "blocked"
```

  A fresh `tier-smoke`-shaped probe at 19:51Z exited **0** — the rule's required step would have
  answered "Codex is available", and the "check the deliverable on disk before trusting a label"
  clause covers `ai-angle` (exit 0, `AUTH_ERROR`, deliverable written). The rule would have stopped
  the global fallback.
- **Second event (the correct fallback).** `verify-session` genuinely stopped on the account limit:

```
.kiln/codex/verify-session.exit.json  {"exitCode": 1, "signal": "RATE_LIMITED", "finishedAt": "2026-09-23T21:57:02.700Z"}
.kiln/codex/verify-session.err (tail):
  ERROR: You've hit your usage limit. Upgrade to Pro (https://chatgpt.com/explore/pro), … or try again at 4:44 AM.
```

  A fresh probe in that window would also have hit the limit, and the rule's per-task table sends
  `RATE_LIMITED` to the Cursor `generator` without retry — so the rule does not block the correct
  second decision. `scripts/test-codex-signals.mjs` (exit 0) also reclassifies the two
  prose-contaminated historical records (`ai-angle: AUTH_ERROR -> -`, `vs-slot-change: RATE_LIMITED -> -`),
  which is the classifier bug the rule warns about.

`unverified:` I did not re-run a Codex probe (out of scope and would consume the limited quota), so
"a fresh probe would have answered" is inferred from `tier-smoke`'s recorded exit, not re-measured.

---

## 5. Play requirements doc + `AGENTS.md` store-pack table — **PASS** (with one residual)

- **No `7680` remains in either named file.** `rg -n "7680"` (text, excluding `.kiln`) returns
  matches only in `docs/research/2026-09-23-store-asset-contract-audit.md` (which quotes the old
  wording) and in `scripts/lib/play-assets.mjs:24-28`.
- **The marker is honest.** `AGENTS.md:122-123` now read "Play long-edge limit **UNVERIFIED**
  (enforced constraints are in `scripts/store-kit.py`)"; the requirements doc `:150-153` says the
  same and names the 3840 px cap `store-kit.py` applies.
- **Every numeric claim in the table matches the code.** Title/short/full → 30/80/4000
  (`store-kit.py:519`); icon 512×512 ≤ 1024 KB (`:1127,1144`); feature graphic 1024×500 (`:1128`);
  phone exactly 6 at 1080×1920 (`:34`), 320/3840/2×/8 MB (`:1102`, `:1144`); 7-inch exactly 4
  at 1200×1920; 10-inch exactly 4 at 1600×2560.
- **Official Play page — reached.** `https://support.google.com/googleplay/android-developer/answer/9866151`
  fetched (309 lines). It states the screenshot **Requirements** as "JPEG or 24-bit PNG (no alpha) ·
  Minimum dimension: 320px · Maximum dimension: 3840px · … can't be more than twice as long as the
  minimum dimension", and separately, under **Large screens** (Chromebook and tablets), "Upload
  screenshots between **1,080 and 7,680px**". So the page itself states both 3840 and 7,680, which is
  why 7,680 is not simply an invented number. Marking the long-edge limit `UNVERIFIED` and deferring
  to the enforced 3840 cap is honest and conservative, but it under-states what the official page
  actually says; the accurate phrasing would be "Play's own page is internally inconsistent (a
  3,840 px general maximum vs a 1,080–7,680 px large-screen range); the factory enforces 3,840."

**Residual (not in either named file):** `scripts/lib/play-assets.mjs:24-28` still exports

```24:28:scripts/lib/play-assets.mjs
// Google Play 7-inch tablet screenshot rule: 4-8 images, short edge at least 1080px, long edge at most 7680px.
export const TABLET_7 = Object.freeze({ min: 4, max: 8, minShortSide: 1080, maxLongSide: 7680, maxBytes: 8 * 1024 * 1024 })
// Google Play 10-inch tablet screenshot rule: 4-8 images, short edge at least 1080px, long edge at most 7680px.
export const TABLET_10 = Object.freeze({ min: 4, max: 8, minShortSide: 1080, maxLongSide: 7680, maxBytes: 8 * 1024 * 1024 })
```

with an uncited "Google Play … rule" comment. These constants are not currently consumed
(`verify-slot.mjs` uses only `isValidScreenshot`, `:292`), so the practical impact is low, but this
is the same value the docs now label UNVERIFIED, restated as a rule in code — the exact drift
`verification-discipline.mdc` forbids. **Fix for the owning agent:** either delete the unused
`TABLET_7`/`TABLET_10` constants (and the `maxLongSide` field) or import the enforced limit from one
place and delete the other.

Also pre-existing and still true: the icon row claims "sRGB" but nothing enforces it, and both icons
carry no `sRGB` chunk (`PIL` `image.info` keys `[]` for smoke and pipe-proof); and the feature-graphic
row says "JPEG or 24-bit PNG" while `inspect_image` hard-requires PNG (`store-kit.py:1093`).

---

## 6. `openspec/changes/add-visual-schedule-slot/` — **PASS** (with residual)

- **Exactly one capability spec directory:** `specs/visual-schedule-board/` (listed recursively;
  the change contains only `.openspec.yaml`, `design.md`, `proposal.md`, `README.md`, `tasks.md`,
  and that one `spec.md`). The duplicate capability was merged forward, as `tasks.md:60-92` records.
- **No therapeutic/outcome claim.** `proposal.md:56-59` and `:76-88`, `design.md:110-120`, and
  `spec.md:146-152` explicitly keep copy to literal utility wording and rule the health-claim path
  out. No figure is invented: every number traces to the evidence document (below).
- **Every in-repo path it cites exists** (checked with `Test-Path`): `README.md`, `.openspec.yaml`,
  `docs/research/2026-09-23-visual-schedule-evidence.md`, `docs/research/2026-09-23-autism-app-policy.md`,
  `docs/niche-selection.md`, `docs/monetization-strategy.md`, `docs/play-publishing-playbook.md`,
  `openspec/changes/add-plant-health-assistant-slot/tasks.md`, `scripts/lib/namespaces.mjs`,
  `AGENTS.md`, `specs/visual-schedule-board/spec.md`, and the three `.kiln/visual-schedule/*`
  artifacts named in `tasks.md:49-56`. The only non-existent path cited is
  `apps/2026-09-23-visual-schedule/`, which is the deliberately-not-created future slot.
- **The distinctness sentence is still marked unverified.** `proposal.md:31-35` ("**Distinctness
  sentence: UNVERIFIED …**") and `tasks.md:108-111` ("**UNVERIFIED / blocking:** feature-level
  distinctness and the asserted execution gap").
- **Numbers repeated from the evidence document are in it, and the artifacts back them.**

| Number in the change | Evidence document | Raw artifact (measured) |
|---|---|---|
| 30 visible, 14 rated, 11 weak, median 3.85 | §1 table row `visual schedule autism` | `.kiln/visual-schedule/screen.json`: `analysis {total:30, ratedCount:14, weakCount:11, medianRating:3.85}`, `verdict "candidate"` |
| 4.9★ / 500K+ direct incumbent | §2 / Bottom line | `enrich-full.json` `com.phonegap.matraquinha {installs:"500K+", rating:4.9}` |
| 4.8★ / 10K+ specialist ranked first on target query | §2 / Bottom line | `enrich-full.json` `app.littleplanner {installs:"10K+", rating:4.8}`; `screen.json` incumbents[0] = `app.littleplanner` |
| largest direct 500K+, target-query largest 100K+ | §5 | exact-query bucket set includes `100K+` and no larger; `derived5.txt` "PER-QUERY LARGEST BUCKET … visual schedule autism: 100K+" |
| 13 of 17 direct apps unrated and ≤ 1K+ | §2 | `derived5.txt`: "direct rated=4 unrated=13", "direct with installs ≤ 1K+: 13"; the 4 rated are exactly the 4 at ≥ 10K+, so the intersection is 13 |
| RoutinePals $6.99/mo, $49.99/yr, 14-day trial | §3/§4 | `.kiln/visual-schedule/listing.txt:290` |
| artifact qualification: update dates Sep 11 / Aug 14 | `tasks.md:48-56` | `enrich-full.json` `com.routinepals.app` updated Sep 11 2026; `co.littlelabs.littleplanner` updated Aug 14 2026 |

  No number the change repeats is unsupported by the evidence document or by the raw artifacts.
  The change's own qualification (`tasks.md:48-56`) correctly refuses the evidence document's
  stronger "launched in Aug–Sep 2026 / both shipped subscriptions then" wording — the artifacts show
  update dates and badges, not launch dates. (`unverified:` the "17 direct apps" partition is a
  human classification, not stored in the JSON; I reproduced the counts from `derived5.txt` and the
  raw JSON, but the label itself is a judgement the evidence document already declares as such.)

**Residual.** `README.md:3` and `.openspec.yaml` were not reconciled with the do-not-commit decision
that `proposal.md`/`tasks.md` record. The README still reads
"Visual schedule / routine utility for **autistic children** and their families - … ; no ML, no
server, no therapeutic claim", and `.openspec.yaml` still has the goal "**Ship** one isolated
PlayKiln slot …". The proposal now says "**do not commit this slot**". The README is not shipped
copy, so it does not violate the spec's literal-copy requirement, but it markets a condition-named
audience and a shipping goal the change has withdrawn — an internal contradiction the next reader
will trip over. **Fix for the owning agent:** update `README.md` and `.openspec.yaml` to the
do-not-commit status.

---

## 7. `docs/research/2026-09-23-store-asset-contract-audit.md` — **FAIL** (claim refuted)

The audit claims (`:118-129`) that `--validate-only` "aborts … on both existing slots: neither
`apps/2026-09-22-smoke/capabilities.json` nor `apps/2026-09-23-pipe-proof/capabilities.json` exists",
and that the failure "is real … on both slots". Measured:

```
=== pipe-proof --validate-only ===
PASS listing.json — schema=1 and Play character limits satisfied
PASS assets/icon-512.png — 512x512 RGBA, 4597 bytes
... (all 38 assets PASS) ...
STORE-KIT: OK apps/2026-09-23-pipe-proof/store (icon=512x512 feature=1024x500 phone=6 tablet7=4 tablet10=4 locales=en-US,ru-RU)
EXIT=0

=== smoke --validate-only ===
FAIL: missing apps\2026-09-22-smoke\capabilities.json; declare enabled capabilities (use an empty array when none are enabled)
EXIT=1

Test-Path apps/2026-09-22-smoke/capabilities.json    -> False
Test-Path apps/2026-09-23-pipe-proof/capabilities.json -> True
```

**Confirmed:** the failure is real for `2026-09-22-smoke`. **Refuted:** it is not true for
`pipe-proof` — that slot now has a `capabilities.json` and exits 0 with every asset PASS. The
audit's blanket "both slots" claim is therefore stale/wrong; do not cite that audit as evidence that
the factory's store gate is inoperative. Other parts of that audit I spot-checked still hold: the
smoke icon's rounded mask (41 683 zero-alpha pixels, corners `[0,0,0,0]`, reproduced exactly), the
missing `sRGB` chunk (`info` keys `[]`), and the mid-word-truncated `en-US` short description.

`unverified:` I did not re-measure all 34 smoke assets or the feature-graphic/JPEG validator path; I
confirmed the specific claims above and the one that mattered (the exit-code claim).

---

## Single most serious defect / residual risk

**The store-kit `idea` fix cannot reach any existing store pack.** `recorded_idea()` is correct and
on the CLI path (§1(b)), but the generator keeps `listing = existing` (`scripts/store-kit.py:1200`)
and never re-derives `idea` or locale copy, so both real slots still carry the placeholder sentence
`"PlayKiln slot — isolated Android attempt. Factory harness lives at the repo root."` as their store
`idea` and an 80/80 mid-word-truncated short description (`"…repo roo…"`). The improvement only
affects a slot whose `listing.json` does not yet exist, i.e. none in this repo — the copy that would
ship is unchanged and violates `AGENTS.md`'s "claims must describe the shipped app". Owning agent:
refresh `idea`/locale copy from metadata during generation (or have `--init` be the only prose
writer), and regenerate both slots' packs.

Runner-up: the `selftest.mjs` wrapper sensor (§3) is bypassable four ways while printing
"Gradle wrappers are invoked only through scripts/lib/gradle.mjs".

## Claims I could not verify (9)

1. The `verify-slot.mjs --build` PASS/BLOCKED path — Gradle is forbidden here; verified by reading
   `gradleAssemble` against `gradle.mjs`, and the build-less `UNVERIFIED`/exit-2 path was run.
2. `require_nontransparent_corners` rejecting a near-transparent (alpha 1–254) or small-radius mask
   — reasoned from the `== 0` test at `store-kit.py:1098`; proving it needs a written PNG.
3. That the icon contains no drawn **shadow** — the predicate checks alpha corners only; shadow is
   not inspected anywhere.
4. The full CLI **create** path for `store-kit.py` (`existing is None → default_listing`) — running
   it writes files; I called the identical `slot_metadata`/`recorded_idea` functions instead.
5. Whether Play requires an actual `sRGB` chunk for app icons — I reached the preview-assets page,
   which says only "Meets Google Play's icon design specifications", not what that entails.
6. That the wrapper sensor misses a regression in the **real** repo — bypasses were demonstrated
   in-memory against the sensor's regexes, not by planting a file.
7. That a fresh Codex probe in the second window (21:57Z) would have failed on the limit — inferred
   from `verify-session.err`; I did not spend quota to re-probe.
8. The "17 direct apps" human classification behind §6's 13-of-17 — the counts are reproducible
   from `derived5.txt`/`enrich-full.json`, but the `direct` label is a declared human judgement.
9. The audit's full 34-asset re-measurement (subject 7) — I reproduced the load-bearing exit-code,
   mask, sRGB and truncation claims only.

## Commands that produced evidence

```
node scripts/selftest.mjs                                  # PASS, exit 0, 143 checks incl. the wrapper sensor
node scripts/test-codex-signals.mjs                        # "CODEX SIGNAL REGRESSION: all checks passed", exit 0
node scripts/verify-slot.mjs --slot pipe-proof             # UNVERIFIED, exit 2, SKIP assembleDebug
python scripts/store-kit.py --slot pipe-proof --validate-only   # all PASS, STORE-KIT: OK, exit 0
python scripts/store-kit.py --slot smoke --validate-only        # FAIL missing capabilities.json, exit 1
python -  (import scripts/store-kit.py: truncate_chars, recorded_idea, slot_metadata, inspect_image)
node -    (replicated the selftest sensor regexes against crafted text)
node --input-type=module -e '… import ./scripts/lib/codex-ledger.mjs …'
rg -n "7680" ; Test-Path … ; git status --porcelain ; git diff -- scripts/verify-slot.mjs
```
