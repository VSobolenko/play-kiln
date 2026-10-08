# Ideation and money checklist

This is the checklist the owner reads at **human checkpoint 1** — before an app is built — and again
**after an app ships**, when its readings are in. Every line is a question the owner can answer
without reading code, and every line names the artifact that answers it.

**Checkpoint 1 is one decision:** *approve this plan and its price, or reject it.* Nothing is
scaffolded before it, and the proposal is written to be accepted or rejected from this page.

**Where the questions point.** This page asks the question and names the artifact and the values to
check. The judgement rules themselves live in `.cursor/rules/*.mdc` (or the contract document named
beside the question); this page points at them rather than restating them. The machine-checkable part
of the plan is `node scripts/check-economics.mjs --slot <slug>`.

---

## Part 1 — At checkpoint 1, before the app is built

### Q1. Is this the topic I would have chosen, or the example I gave?

- [ ] The proposal's `Job` line names a subject, and the demand evidence behind it was measured. If
      the job is a paraphrase of an example the owner gave — its size, its offline-ness, its build
      estimate — the answer is no. Rule: `.cursor/rules/ideation-gate.mdc`.

### Q2. Does the proposal sit in a category the factory has burned?

- [ ] Check the proposed job against the burned categories, including the one exception that lets an
      owner name the app himself. Rule: `.cursor/rules/ideation-gate.mdc`.

### Q3. Is this a job we already have an app for?

- [ ] Compare the proposal's job against `registry/apps.json` and every `apps/*/store/listing.json`.
      Rule: `.cursor/rules/ideation-gate.mdc`; the catalogue-wide risk is
      `docs/monetization-strategy.md` §1.2.

### Q4. Was this already killed with a recorded reason?

- [ ] Check the proposal's slug and job against the dead zones in `docs/niche-selection.md`, §3 of
      `docs/research/2026-09-24-market-decision.md`, and `registry/apps.json`. Rule:
      `.cursor/rules/ideation-gate.mdc`.

### Q5. Is the demand measured, and is every claim labelled?

- [ ] Every statement in `## Demand evidence` carries one of `MEASURED` / `ON RECORD` / `INFERRED` /
      `UNVERIFIED`.
- [ ] The competitor titles were hand-classified with `node scripts/scan-titles.mjs`, not taken from
      the relevance guard's verdict.
- [ ] No conclusion rests on an unrated competitor. Rule: `.cursor/rules/ideation-gate.mdc`.

### Q6. Does the archetype pair with a capability the app will actually enable?

- [ ] The `archetype` is one of the five named in `docs/slot-economics.md`, and the capability it
      needs is enabled in `apps/<slot>/capabilities.json`.
- [ ] `node scripts/check-economics.mjs --slot <slug>` does not report the plan as blocked. Rule and
      vocabulary: `docs/slot-economics.md`.

### Q7. Are the kill criteria numbers, and are they non-zero?

- [ ] `windowDays` is in 7..180; `minInstalls` is at least 1; `minRevenueUsd` is at least 1. Rule:
      `docs/slot-economics.md`.

### Q8. What do I, as the user, buy?

- [ ] The plan's `paidUnit`, `momentOfPayment` and day-one revenue path answer it, and a price-only
      plan says so out loud and argues for it rather than leaving the question unanswered. Rule:
      `.cursor/rules/ideation-gate.mdc`; shape: `docs/slot-economics.md`.

### Q9. Is there a server, and is its cost stated rather than discovered?

- [ ] A server, cloud service, account/backend, ML service or paid API is admissible. The plan states
      its service cost, whether it is per-user-per-month or one-off, and how price funds it; for
      recurring per-user cost it states near-zero cost, repeat purchases, or paid-upfront lifetime
      margin. A money-costing credit service also records its server-side authority before approval.
      Rules: `.cursor/rules/ideation-gate.mdc`, `.cursor/rules/client-secrets.mdc`.

### Q10. Is the build estimate stated as information — and are device capabilities declared?

- [ ] The proposal gives a build estimate in agent hours and names the shape it chose on purpose.
- [ ] The proposal declares, one line per capability in a `## Device capabilities` section, which of
      the twelve device capabilities the job uses and which it does not — so a job that needs nothing
      from the phone says so on purpose rather than by silence, and a job that needs the camera, the
      microphone, the sensors, location or a widget is a stated choice rather than an omission.
- [ ] Every `used` capability names the API the slot would call, what the job loses without it, and
      the permission it needs. The Android Photo Picker (`ActivityResultContracts.PickVisualMedia`)
      needs no runtime permission, so using it costs nothing against permission minimisation while a
      capability that does need a permission must say so. Rules: `docs/ideation/README.md`;
      `.cursor/rules/ideation-gate.mdc` § "Build cost is information, never a criterion" — where a local and
      free capability is not a cost; infrastructure cost is declared in the arithmetic; and a runtime
      permission that buys nothing remains a defect.

### Q11. Is elapsed time reported transparently, without becoming a criterion?

- [ ] The proposal labels its build estimate and reports elapsed wall clock as information for the
      owner; neither number is a selection, ranking, refusal or kill criterion. Rules:
      `.cursor/rules/ideation-gate.mdc`, `.cursor/rules/parallel-execution.mdc`.

### Q12. Is this one slot, and was any earlier attempt closed properly?

- [ ] The proposal covers one job and one slot, extended only because the owner asked for it.
- [ ] Any earlier attempt is archived or killed with a recorded reason
      (`node scripts/kiln-status.mjs --kill <slug> --reason "<why>"`), not left as a directory in
      `apps/`. Rules: `.cursor/rules/ideation-gate.mdc`, `.cursor/rules/project-conventions.mdc`.

### Q13. Can I accept or reject this without reading code?

- [ ] The proposal carries the shape listed in `docs/ideation/README.md` — category and job, paying
      user, what is bought, price, demand evidence with its strength, promise contract, build estimate
      and kill criteria — and no capture or provenance work. Rule: `.cursor/rules/ideation-gate.mdc`.

### Q14. Do I approve this plan and its price?

- [ ] State the decision in your own words. Nothing is scaffolded before it, and the plan and its
      promise contract are validated before `new-slot.mjs` runs. Recording rule:
      `.cursor/rules/release-gate.mdc`; the step order is `docs/ideation/README.md`.

---

## Part 2 — After the app ships: the reading

`node scripts/check-outcomes.mjs --slot <slug>` prints exactly one verdict, and the exit code carries
it too. The reading is hand-recorded today, with an `observedAt` date and a `source` string — the
factory has no revenue API, and that is the mechanism rather than an integration. Contract:
`docs/monetization-owner.md`.

### The reading

- [ ] `apps/<slot>/store/outcomes.json` carries one row per observation, each with `observedAt` (an
      ISO date that is not in the future) and `source` (where the number came from, for example
      `play-console` or `payout-statement`). A number with no date and no source is a claim.
- [ ] An empty `readings` array is **valid**: a slot with no data yet is `NO-DATA`, not a broken file.
- [ ] Readings are recorded at the cadence the slot's own `killCriteria.windowDays` implies: at the
      start, at roughly halfway, and on the decision day. Date a reading as the console reports it,
      not as you look — console reporting lags.

### The vocabulary

Declared by `scripts/lib/outcomes.mjs`; the meanings below are how this page reads its output.

| Verdict | Exit | When | Read it as |
|---|---:|---|---|
| `NO-DATA` | 2 | No file, or no readings | Nothing measured. Never a pass and never a kill. |
| `TOO-EARLY` | 0 | The newest reading is younger than `windowDays` | The window is open. A progress read, not an approval. |
| `ON-TRACK` | 0 | The newest reading meets **both** `minInstalls` and `minRevenueUsd` | The numbers it was asked to reach. Reaching them early is still `ON-TRACK`. |
| `MISSED` | 1 | The window elapsed and the newest reading misses either threshold | The slot failed its own criterion. |

- [ ] On `MISSED`, the output prints the exact archive instruction —
      `node scripts/kiln-status.mjs --kill <slug> --reason "<why>"` — and a kill requires a non-empty
      reason. Contract: `docs/monetization-owner.md`.
- [ ] Success is yours to declare, over a measured reading, and never an agent's. Rule:
      `docs/monetization-owner.md`.
