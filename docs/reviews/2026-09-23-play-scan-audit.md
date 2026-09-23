# Audit: `scripts/lib/play-scan.mjs` — 2026-09-23

Independent adversarial audit of the factory's market screen, commissioned to try to falsify it
rather than confirm it. This record exists because the auditor ran in a read-only sandbox and could
not write its report to disk; the findings below are therefore transcribed here alongside the
verification the orchestrator performed afterwards.

**Status: review complete — 3 defect classes confirmed and fixed, 2 disagreements recorded.**

## Why this audit happened

`play-scan.mjs` had been validated against six hand-read queries and passed. That is weak evidence:
a sensor that only reproduces the cases it was tuned on is overfitted, not validated. The audit
attacked it on fresh queries, on parser edge cases, on card segmentation, and on the organisation
heuristic.

## Auditor's method and its one limitation

The auditor could not execute the module: the sandbox was read-only, a Playwright fallback had no
`fs`/`require`, and a Windows sandbox for Codex failed. It therefore ran a **verbatim transcription**
of the module's functions against server-rendered Play HTML, across 20 queries and 23 fetches,
covering 251 rating-to-app pairs. Its transcription reproduced the author's tuned set 7/7.

That is corroboration, not proof, so every claim below was re-run afterwards **against the real
module** (`scripts/verify-play-scan-defects.mjs`). Nothing was changed on the auditor's word alone.

## Confirmed defects

### D1 — a card title could be raw markup (CONFIRMED, fixed)

Observed live, reproduced independently:

```text
pkg=com.whoop.android
title="/store/apps/details?id=com.whoop.android\" aria-label=\"WHOOP\" class=\"Qfxief\">"
```

**Root cause.** `parseSearchHtml` split the page on `/store/apps/details?id=` matches. Those matches
sit *inside* the anchor's `href` attribute, so a segment began mid-attribute; the attribute tail
survived tag-stripping and became the card's first "text". Whether it did depended only on the
80-character line filter — a title in the output was a function of an arbitrary threshold.

**Fix.** Card boundaries are now structural: a segment runs from just after the tag containing its
link to just before the tag containing the next link. Visible-text extraction additionally drops any
line containing `="` or starting with `/store/`, because Play card text never contains either.

**Verified after fix:** 12 live queries, zero titles containing markup or a store path.

### D2 — the organisation heuristic was actively harmful (CONFIRMED, fixed)

The auditor reported that `'who'` flagged `WHOOP` and `Whova` while matching none of the World
Health Organization apps. Independent reproduction found it is worse than that:

| Behaviour | Evidence |
|---|---|
| False positive on `WHOOP` | token `'who'` matched by substring |
| False positive on `eClinicalWorks LLC` | token `'clinic'` matched by substring |
| **Missed the actual WHO** | 14 `World Health Organization` apps, none flagged |

The heuristic was producing noise while missing the exact case it was introduced for (`first aid
guide`, held by the Red Cross and the UN).

**Fix.** Phrase matching on word boundaries, with ambiguous short tokens removed (`who`, `clinic`,
`agency`, `authority`, `department`) and `world health` added.

**Verified after fix:** the same live query now flags **20 `World Health Organization` apps**, and
flags neither `WHOOP` nor `eClinicalWorks LLC`.

### D3/D4 — a `fortress` reason contradicted its own data (CONFIRMED, fixed)

The auditor found 4 of 4 fortress verdicts printing "no weak pocket" while `weakCount` was 2–4.
Independent reproduction found **8 of 8**, with `weakCount` between 1 and 6:

```text
[step counter]     weakCount=6 weakShare=0.20  reason="18/30 ... with no weak pocket — entrenched"
[water reminder]   weakCount=2 weakShare=0.11  reason="13/19 ... with no weak pocket — entrenched"
```

**Root cause.** The fortress condition tests `weakShare < NO_POCKET_SHARE` (0.30), which permits a
non-zero `weakCount`; the message asserted "no weak pocket" unconditionally. The wording was
hand-written rather than derived from the check — the exact failure mode
`.cursor/rules/verification-discipline.mdc` describes.

**Fix.** The reason string is now built from the same numbers the condition tested.

**Verified after fix:** 12 live queries, zero contradictions.

## Disagreements (recorded, not acted on)

### Threshold change for `dead-zone` — rejected

The auditor argued `beekeeping log` should not be `dead-zone` and that `DEMAND_PROVEN_SHARE` should
move from 0.35 to ≤0.16. Measured: 5 of 30 visible incumbents are rated (0.17).

**Not adopted.** A niche where only 5 of 30 visible apps have ever earned enough installs to be
rated is thin evidence of demand, and 0.16 would make `dead-zone` nearly unreachable — the screen
would stop rejecting anything. More importantly, tuning a threshold to reclassify a single query is
precisely the overfitting this module's calibration exists to prevent. The measured `ratedShare` is
already printed in every row, so a reader who disagrees can see the number and overrule the label.

### `INADEQUATE_SHARE` for `child allowance tracker` — rejected for the same reason

The `mixed` verdict follows from the stated definition (8 weak, but only 0.15 of rated are strong).
Changing the threshold to produce a different label would be fitting the rule to one observation.

## Limitation that remains (not fixed)

A verdict can change with the number of cards Play renders. The auditor observed `morse code trainer`
scoring `fortress` on the 20 cards it read by hand and `mixed` on the 30 the page served.

The D3 fix incidentally corrects that specific case (`morse code trainer` now returns `mixed`, since
its `weakShare` of 0.40 fails the fortress gate regardless of page size), but the underlying
sensitivity is real: **every share is computed over a display-capped result set (~30 cards), not
over the market.** This is stated in the module header and must stay stated. Verdicts are triage,
not a verdict oracle.

## What was verified to be sound

- **Card segmentation held.** 251 rating-to-app pairs across 16 pages were checked against
  browser-DOM ground truth with zero misattributions.
- **Not overfitted to the author's six.** On fresh queries the verdicts were not tuned; the two
  disagreements above are definitional, not evidence of fitting.
- **Calibration survives the fixes.** All six definitional verdicts still reproduce and the declared
  blind spot still behaves as declared (`scripts/validate-play-scan.mjs`, exit 0).

## Sensors added

| File | Purpose | Offline? |
|---|---|---|
| `scripts/test-play-scan.mjs` | Synthetic regression tests for D1, D2, D3 | yes — fast, deterministic |
| `scripts/verify-play-scan-defects.mjs` | Re-runs the same checks against live Play pages | no — proves the fixture still matches reality |
| `scripts/validate-play-scan.mjs` | Calibration against six hand-read verdicts | no |

A synthetic fixture alone only proves the parser agrees with its author's assumption about Play's
markup, so the live script is kept alongside it. `test-play-scan.mjs` caught a wrong expectation in
the orchestrator's own test while writing it (9 rated cards asserted as 10), which is the intended
behaviour.

## Verified vs unverified

**Verified:** every defect above, by running the real module on live Play data before and after the
fix; the fixes' effect; the calibration holding.

**Not verified:** that the auditor's transcription was byte-identical to the module (it was never
executed in its sandbox); that rating share predicts revenue at all — the screen measures supply
quality, not demand volume, install magnitude, or willingness to pay.
