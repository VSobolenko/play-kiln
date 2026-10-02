# Monetization owner contract — the reading that decides a slot

Every slot states **how it can earn** and **what would make us kill it** before it exists
(`docs/slot-economics.md`). This document is the other half: **what the app actually earned**, and
who is accountable for reading it. It is enforced by `scripts/check-outcomes.mjs`, the numbers live
in `apps/<slot>/store/outcomes.json`, and the role that owns them is the Money owner
(`.cursor/agents/money-owner.md`, and the same card under `.claude/agents/` and `.codex/agents/`).

## Why this file exists

The factory's money plan has had numeric `killCriteria` since the economics gate was written:
`windowDays`, `minInstalls`, `minRevenueUsd`, all validated by `scripts/lib/slot-economics.mjs`
before a slot is allowed to exist. **Nothing ever read them after the app shipped.** A repo-wide
search for a consumer of `minInstalls` / `minRevenueUsd` found the schema, the documents and one
test fixture — so the one stage `AGENTS.md` calls the factory's product ("The factory's product is
revenue, not green gates") had no program and no owner behind it.

The prior-art review reached the same conclusion from the outside and made it change **B4**
(`docs/research/2026-09-29-app-factory-prior-art.md` §2.6): *"Every portfolio source is unanimous:
the kill rule is what makes a portfolio a system."* The rule was prose with a validator in front of
it and nothing behind it. This document and its gate are the part behind it.

Two failures are prevented by having a role rather than a spreadsheet column:

- **The optimistic reading.** A number nobody is required to record is a number that gets estimated.
  An estimate presented as a measurement is the defect class this repository has recorded most.
- **The slot nobody kills.** An attempt that misses and is quietly left in `apps/` is not a neutral
  outcome; per `docs/monetization-strategy.md` §1.2 it is evidence of a repetitive catalogue, and it
  costs maintenance forever.

## The contract

`apps/<slot>/store/outcomes.json` lives inside the slot's store pack because it is committed
evidence about the shipped app, beside `listing.json` and the assets. Its shape is exactly:

```json
{
  "schemaVersion": 1,
  "readings": [
    {
      "observedAt": "2026-10-01",
      "source": "play-console",
      "installCount": 420,
      "activeDeviceCount": 305,
      "revenueUsd": 12.0,
      "conversionRate": 0.019,
      "refundCount": 1,
      "ratingCount": 7,
      "notes": "First payout statement; revenue is the net figure from Financial reports."
    }
  ]
}
```

| Field | Rule |
|---|---|
| `schemaVersion` | Exactly `1`. A bump is a deliberate migration, not a typo to tolerate. |
| `readings` | An array. **An empty array is valid**: a slot with no data yet is `NO-DATA`, not a broken file, and treating "not measured" as malformed would make the two states indistinguishable. |
| `readings[].observedAt` | An ISO-8601 calendar date (`YYYY-MM-DD`) that exists on the calendar. It must not be in the future — a reading cannot be observed tomorrow, and a future date is how a stale record hides. |
| `readings[].source` | A non-empty string naming where the number came from, e.g. `play-console`, `payout-statement`, `review-page`. Not restricted to a list, because refusing a legitimate source would force the record to be mislabelled instead of described. |
| `readings[].installCount` | Integer `>= 0`. Required. |
| `readings[].activeDeviceCount` | Integer `>= 0`. Required. |
| `readings[].revenueUsd` | Finite number `>= 0`. Required. A refund is recorded as `refundCount`, never as negative revenue. |
| `readings[].conversionRate` | Optional fraction between `0` and `1`. Play shows a percentage, so `1.9%` is `0.019`. |
| `readings[].refundCount` | Optional integer `>= 0`. |
| `readings[].ratingCount` | Optional integer `>= 0`. |
| `readings[].notes` | Optional non-empty string. |
| duplicate `observedAt` | Rejected. One observation per date, so a corrected re-read **replaces** the row rather than being added beside it — otherwise a bad early reading would be averaged into the verdict forever. |

## The KPI vocabulary

The vocabulary exists so one app's "conversion" is another app's "conversion". Every number is a
Play Console number; where a page is named, the **console's own definition of the metric wins over
this table**, because Play changes its reports and this document cannot.

| KPI | What it means | Where the number comes from | What it does **not** prove |
|---|---|---|---|
| **Installs** | Device install events, the console's headline acquisition figure for the period | Play Console → Statistics (acquisition) | Not use: an install is not an open, a retained user, or a user who ever saw the paywall. Re-installs on a new device can be counted, so it is not "people". |
| **Active devices** | Devices with at least one session in the period | Play Console → Statistics (active devices) | A device is not a user (shared tablets, reinstalls), activity is not engagement depth, and an active device has not bought anything. |
| **First-open** | The app was launched at least once after install | Play Console → Statistics / acquisition funnel | Only that launch succeeded. It does not prove the app was used for its job, nor that any paying screen was reached. |
| **Conversion to paid** | Share of a stated base that completed a purchase in the period (`buyers ÷ base`) | Play Console → Financial reports / monetization, with the base recorded in `notes` | Not profit: Play's fee, refunds and FX are deducted before the money arrives. Not attribution: it does not say which cohort or which paywall converted. A rate over a base of 20 is noise. |
| **ARPU** | Revenue per user, `revenueUsd ÷ the stated base` | Computed from a reading; the base must be named | Not a margin, and not a per-person average anyone experiences: it blends buyers and non-buyers, so a high ARPU can be one whale. Never report it without the base. |
| **ARPPU** | Revenue per **paying** user, `revenueUsd ÷ paying users` | Computed from a reading; the paying-user count must be recorded | Not a business. A high ARPPU over two buyers is a rounding error, and it says nothing about how many more will buy. |
| **Refund rate** | `refundCount ÷ transactions` over the period | Play Console → Financial reports (refunds/chargebacks) | Not satisfaction: some refunds are policy or accidental, and a flattering rate over three sales proves nothing at all. |
| **Churn** | The share of a recurring relationship that lapsed without renewal | Play Console → subscription reports | **Not applicable to any slot.** Subscriptions are banned (owner, 2026-10-01; `.cursor/rules/ideation-gate.mdc` § "There is no house monetization"), so no slot has a recurring relationship to churn. Record `n/a` in `notes`; do not invent a number to fill the row. |
| **D1 / D7 / D30 retention** | Share of an install cohort still active 1, 7, 30 days after install | Play Console → Retention (needs enough data to show a cohort) | Not paying intent: retention is measured over all installs, including the users who will never see the paywall. On a small cohort the percentage is too coarse to act on. |
| **Rating** | The average star rating | Play Console → Ratings | Not earnings, and not a sample of users: only people who choose to rate are counted. A 4.9 with forty installs is not a validated product. |
| **Review volume** | The number of ratings and written reviews, and how fast new ones arrive | Play Console → Ratings and reviews | Not revenue: reviews are not monetised. A rising count is one of the few public signals of install velocity, and it is a signal to investigate, never a result. |

## Observation cadence

The cadence is derived from `killCriteria.windowDays` in the slot's own `economics.json`, not from a
factory-wide interval, because a 30-day window and a 180-day window cannot be read on one schedule.

- **Record a reading at the start.** The window epoch is the **first reading** once there are two or
  more; with a single reading it is the slot's `createdAt` in `registry/apps.json`. Recording the
  first reading late shortens the window the slot is judged over.
- **Record at roughly the halfway point** and **on the decision day**. Two or three points show a
  trend; a single point at the end shows only whether the slot is already dead.
- **The decision day is `windowDays` after the epoch.** On that day the window is over: from it, a
  reading that misses either `minInstalls` or `minRevenueUsd` is `MISSED`, not `TOO-EARLY`.
- **Date the reading as the console reports it, not as you look.** Console reporting lags for hours
  to a day; recording today's date against yesterday's partial data inflates or deflates the last
  point of a series that exists to be compared.
- **Record the source.** A reading whose `source` is empty cannot be re-checked, so the gate refuses
  it.

## The verdicts

`node scripts/check-outcomes.mjs --slot <slug>` prints exactly one verdict, and the exit code carries
it too. Adding `--json` gives the same verdict as a machine-readable record.

| Verdict | Exit | When | What it means |
|---|---|---|---|
| `NO-DATA` | `2` | No `store/outcomes.json`, or a file with no readings | Nothing has been measured. This is **never** a pass and **never** a kill: a portfolio rule that killed on missing data would kill every slot on its first day. The output names the missing file. |
| `TOO-EARLY` | `0` | The newest reading is younger than `windowDays` since the epoch | The window is still open. The output names the elapsed days, the window, and the remaining shortfall — it is a progress read, not an approval. |
| `ON-TRACK` | `0` | The newest reading meets **both** `minInstalls` and `minRevenueUsd` | The slot has already reached the numbers it was asked to reach. Hitting them early is `ON-TRACK` even while the window is open. |
| `MISSED` | `1` | The window has elapsed and the newest reading misses either threshold | The slot failed its own criterion. The output prints the miss, the elapsed window, and the exact `--kill` instruction an operator runs to archive the slot rather than silently abandon it. |

The order matters and is deliberate. `ON-TRACK` is tested **before** the window, so a slot that
reaches its numbers on day 5 is not reported `TOO-EARLY` merely because its window is still open.
`NO-DATA` is tested **before** the window clock, so an unmeasured slot is never killed on arithmetic
it never entered.

## Unmeasured claims are `UNVERIFIED`, never optimistic

- **Play Console is the source of truth.** Every number in a reading is a console number, and the
  console's own metric definition overrides this document.
- **The factory has no revenue API yet.** The Play Developer API surface the factory uses cannot
  serve these reports (`docs/research/2026-09-23-google-play-automation-surface.md`), so a reading is
  **hand-recorded today**. That is the current mechanism, and it is stated here so nobody mistakes
  the gate for an integration it does not have.
- **Every reading carries `observedAt` and `source`.** A number with no date and no source is a
  claim, and this repository's rule for a claim is that it is labelled `MEASURED`, `ON RECORD`,
  `INFERRED` or `UNVERIFIED` (`.cursor/rules/ideation-gate.mdc`) — never presented as more than it is.
- **A missing reading is `NO-DATA`, and `NO-DATA` is not a result.** It is the honest report of not
  knowing, and it exits `2` so it can never be read as green.
- **Never invent a number to complete a table.** A row with no source is worse than an empty row,
  because the empty row can still be filled by measuring.

## Enforcement

- **`scripts/check-outcomes.mjs --slot <slug> [--json]`** reads the slot's `economics.json` through
  `scripts/lib/slot-economics.mjs` and its `store/outcomes.json` through `scripts/lib/outcomes.mjs`,
  and prints the one verdict above. It refuses a plan it cannot validate (exit `1`, `BLOCKED`) and a
  slot or plan it cannot find (exit `2`, `UNVERIFIED`) rather than guessing a threshold.
- **`scripts/lib/outcomes.mjs`** is the single source of truth for the schema, the arithmetic and the
  `--kill` instruction, so a CLI run and a regression run cannot disagree.
- **`scripts/test-outcomes.mjs`** is the offline suite. It injects the defects a naive implementation
  would miss — a reading dated in the future, a negative count, a window that has not elapsed (which
  must be `TOO-EARLY`, not `MISSED`), a window that has elapsed with no data at all (which must be
  `NO-DATA`, not `MISSED`), and a `MISSED` case that must print the kill instruction.
- **The kill instruction names the archive path**, `node scripts/kiln-status.mjs --kill <slug>
  --reason "<reason>"` — change **B4** of `docs/research/2026-09-29-app-factory-prior-art.md`.
  Archiving is a real, recorded act, not a deletion: the slot's plan, readings and reason stay in the
  repository as the evidence that the criterion was applied.

## What this gate does not prove

It proves that a slot's own declared thresholds have been compared, on a stated date, against a
number someone recorded with a named source — and that the answer is `NO-DATA`, `TOO-EARLY`,
`ON-TRACK` or `MISSED`. It cannot prove the reading is faithful to the console: a hand-recorded
number can be wrong, mis-transcribed, or read from the wrong report, and the gate has no console
access to contradict it. It cannot prove the thresholds themselves were the right ones, that a
given revenue number is *good* rather than merely above a floor, or that a slot reported `ON-TRACK`
is worth continuing to invest in — those are judgements, and they belong to the owner, who reads
this file and the slot's own evidence. The gate exists so that a slot cannot pass its own kill
criterion by nobody ever looking.
