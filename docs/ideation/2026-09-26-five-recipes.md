# Five recipes — ideation proposal, 2026-09-26

**What this is.** A decision-grade proposal of five app ideas for the owner to choose between, one
of which I recommend refusing. Each recipe states its demand evidence, its unit arithmetic, its
marketing, its timing argument (or admits it has none), its build cost, and the cheap way to prove
it wrong. Plans are in `docs/ideation/plans/`, one `.economics.json` per recipe, all five verified
against `scripts/check-economics.mjs`.

**What this is not.** It is not a build instruction, and none of these recipes is a slot. The
factory's first product slot failed commercially with no revenue mechanism at all
(`docs/reviews/2026-09-26-slot-1-economics-postmortem.md`); this document exists so that the next
slot is chosen on measured demand instead of on an example the owner happened to use to describe a
shape. No calculators. No unit converters. No generic arithmetic tools.

**How to read the labels.** Every factual claim below carries one of four labels:

| Label | Means |
|---|---|
| MEASURED | I ran a command in this session and this is what it printed. The command is named. |
| ON RECORD | A file in this repository states it. The path is given. |
| INFERRED | My reasoning from MEASURED or ON RECORD facts. |
| UNVERIFIED | A number or claim I am carrying without evidence. It is listed again in §7. |

No number in this document was invented. Where a figure is absent I say it is absent.

---

## 1. Method, and exactly what was run

### 1.1 Commands and observed results

All commands were run from the repository root on 2026-09-26. Network access was available.

| # | Command | Observed result |
|---|---|---|
| 1 | `node scripts/kiln-status.mjs` | Exit 1, `VERDICT: NOT READY`. Environment rows all PASS: `node v24.11.1`, JDK 17 via `JAVA_HOME` at `C:\Program Files\Java\jdk-17`, `ANDROID_HOME` at `E:\Programm\Android\SDK`, SDK platform `android-37`, build-tools `36.0.0`, `adb`, `python 3.13.7`, `Pillow 11.3.0`, `keytool` via `JAVA_HOME/bin`, `codex-cli 0.146.0-alpha.3`, MCP `context7`/`playwright`/`codex`/`android-skills`/`droidagentkit`. The failure is entirely in the factory self-test: `FAIL a run that skips the build reports UNVERIFIED` and `FAIL an UNVERIFIED verdict exits 2 (got 1)`. 143 s wall clock. |
| 2 | `node scripts/scan-market.mjs --query-set scripts/market-queries.json --concurrency 4` | 17 queries in 4,851 ms: **3 candidate, 6 mixed, 3 fortress, 5 dead-zone, 0 no-results, 0 errored**. Per-query rows quoted in §1.3. |
| 3 | `node scripts/scan-titles.mjs --query "amateur radio logbook"` | `verdict=mixed guard=on-topic (22/29, share 0.76) visible=29`. Full title list in §3.1. |
| 4 | `node scripts/scan-titles.mjs --query "scuba dive log"` | `verdict=mixed guard=on-topic (24/30, share 0.80) visible=30`. |
| 5 | `node scripts/scan-titles.mjs --query "hair color formula"` | `verdict=candidate guard=on-topic (11/12, share 0.92) visible=12`. |
| 6 | `node scripts/scan-titles.mjs --query "salon client record book"` | `verdict=fortress guard=on-topic (9/11, share 0.82) visible=11`. |
| 7 | `node scripts/scan-titles.mjs --query "student driving log"` | `verdict=mixed guard=polluted (6/20, share 0.30) visible=20`. |
| 8 | `node scripts/scan-titles.mjs --query "packing checklist"` | `verdict=candidate guard=on-topic (30/30, share 1.00) visible=30`. |
| 9 | `node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "amateur radio logbook"` | 8 apps enriched, 0 failures, 4,548 ms. Ads declared on 4 of 8, absent on 4; IAP declared on 5, absent on 3; largest install bucket `100K+`. All 8 free. |
| 10 | `node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "salon client record book"` | 8 apps enriched, 0 failures, 5,050 ms. Ads declared on 2 of 8; IAP declared on 5, absent on 3; largest bucket `10M+`. All 8 free. |
| 11 | `node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "packing checklist"` | 8 apps enriched, 0 failures, 5,400 ms. Ads declared on 0 of 8; IAP on 5, absent on 3; largest bucket `1M+`. Two of the 8 are paid: `$1.09` and `$2.99`. |
| 12 | `node scripts/scan-complaints.mjs --package com.talixa.hamradiolog --top 6` | `3.7★ 10K+ reviews=3 critical=2 top=broken(1) unclassified=1`. |
| 13 | `node scripts/scan-complaints.mjs --package com.pelagicnet.diverlogplus --top 6` | `1.9★ 50K+ reviews=3 critical=3 top=data-loss(3) unclassified=0`. |
| 14 | `node scripts/scan-complaints.mjs --package com.stayincharm --top 6` | `4.4★ 10K+ reviews=3 critical=0 top=none unclassified=0`. |
| 15 | `node scripts/scan-complaints.mjs --package dev.hairyotter07.studentdrivinglog --top 6` | `4.2★ 10K+ reviews=3 critical=2 top=ads(1) unclassified=0`. |
| 16 | `node scripts/scan-complaints.mjs --package com.YRH.PackPoint --top 6` | `4.4★ 1M+ reviews=3 critical=3 top=account(1) unclassified=1`. |
| 17 | `node scripts/check-economics.mjs --economics <each of the five plans>` | All five `VERDICT: PASS`, exit 0, with `SKIP capability pairing is enforced later by node scripts/check-economics.mjs --slot <slug>` and `PASS evidenceRef resolves on disk: docs/research/2026-09-24-market-decision.md`. |
| 18 | `node scripts/scan-titles.mjs --query "hair color formula app" --query "dog walking client log"` — run as background task `993427` in this session, read from its captured output | Exit 0, 151,796 ms, `2 queries: 2 ok, 0 errored`. `hair color formula app`: `candidate`, guard on-topic 28/30 = 0.93, 30 visible; hand formula-job share 3/30 = 0.10. `dog walking client log`: `candidate`, guard on-topic 20/30 = 0.67, 30 visible; three jobs in one shelf, recorded in §2 with the reason it is not taken. |
| 19 | `node scripts/kiln-status.mjs` — run as background task `993428` in this session, read from its captured output | Exit 1, 144,389 ms, `VERDICT: NOT READY`. Every harness, scaffold-integrity, scaffold-correctness, environment, Codex, MCP and registry row PASS or INFO; `WARN java on PATH is JDK 24 but the factory builds with JDK 17`; `INFO registry apps: 3; statuses: scaffolded=3`; `INFO Codex worker records: 51`. The only failure is the same factory self-test pair as run #1. This is the second independent confirmation of that failure; my read of its mechanism is in §6.6. |

Every sensor I invoked answered. Nothing in this run was blocked, and no demand claim in this
document is UNMEASURED because of a sensor failure. There is one honest gap, stated in §1.5.

### 1.2 What the sensors cannot tell you

Four limits, all of them observed in this run rather than assumed:

- **A verdict is not a job.** The guard measures word and category overlap, not whether the visible
  apps do the job a recipe is about. §1.4 documents two collisions, one of which returned
  `candidate` at 0.92 for a shelf that is 75 % a different job.
- **A result count is display-capped and geo-dependent** (ON RECORD, `docs/niche-selection.md`).
  "29 visible results" means the page showed 29 cards, not that the market has 29 apps.
- **An unrated competitor is a failure signal, not a vacancy** (ON RECORD, `docs/niche-selection.md`,
  and it is why the three `dead-zone` hobby logs in §1.3 were rejected).
- **The sensors cannot see revenue, conversion or install quality.** `enrich-market` reports a
  declared price, a declared `ads` flag and a declared `IAP` flag as the store page states them, and
  an install *bucket*. Nothing in the toolchain reads what an app actually earns. That is why §1.5
  is the largest uncertainty in this document.

### 1.3 The calibrated broad screen, as measured today

`node scripts/scan-market.mjs --query-set scripts/market-queries.json --concurrency 4`, verbatim
columns `verdict / relev / seen / rated / weak / median`:

| Group | Query | Verdict | Seen | Rated | Weak | Median |
|---|---|---|---:|---|---:|---:|---:|
| trade-calculators | electrician calculator | mixed | 30 | 15/30 | 5 | 4.50 |
| trade-calculators | hvac load calculator | mixed | 29 | 11/29 | 5 | 4.20 |
| trade-calculators | concrete calculator | fortress | 9 | 7/9 | 1 | 4.70 |
| trade-calculators | welding calculator | dead-zone | 30 | 9/30 | 3 | 4.30 |
| trade-calculators | bolt torque calculator | candidate | 30 | 12/30 | 7 | 4.00 |
| hobby-tools | knitting counter | dead-zone | 30 | 9/30 | 4 | 4.50 |
| hobby-tools | guitar practice log | mixed | 30 | 27/30 | 6 | 4.40 |
| hobby-tools | mushroom foraging guide | candidate | 29 | 21/29 | 12 | 4.00 |
| hobby-tools | beekeeping log | dead-zone | 30 | 5/30 | 3 | 3.80 |
| hobby-tools | aquarium water log | dead-zone | 30 | 10/30 | 3 | 4.45 |
| narrow-health | migraine tracker | mixed | 30 | 13/30 | 4 | 4.30 |
| narrow-health | baby feeding tracker | fortress | 16 | 14/16 | 1 | 4.75 |
| narrow-health | pet medication reminder | mixed | 12 | 9/12 | 2 | 4.50 |
| narrow-health | visual schedule autism | candidate | 30 | 12/30 | 9 | 3.75 |
| ai-engine | plant disease identifier | mixed | 15 | 15/15 | 3 | 4.40 |
| ai-engine | language pronunciation practice | fortress | 30 | 26/30 | 6 | 4.50 |
| controls | bjj training log | dead-zone | 30 | 9/30 | 4 | 4.30 |

Two readings worth carrying forward:

- **The sensor still works.** The `bjj training log` control is still `dead-zone` and the
  `visual schedule autism` row is still `candidate`, matching the manual readings recorded ON RECORD
  in `docs/research/2026-09-24-market-decision.md`. The guard has not obviously drifted since
  2026-09-24.
- **Three candidate rows are already closed.** `bolt torque calculator` is a calculator
  (`.cursor/rules/ideation-gate.mdc`), `visual schedule autism` is on the binding killed list
  (`docs/research/2026-09-24-market-decision.md` §3), and `mushroom foraging guide` is a content
  app whose subject can kill someone who follows it wrongly. `mushroom foraging guide` is the
  single strongest row in the whole screen — 21/29 rated, 12 weak, median 4.00 — and I am rejecting
  it on liability, not on demand. That is stated here so the rejection is a decision rather than an
  omission.
- **Both hobby logs adjacent to my ham-radio recipe are dead zones.** `knitting counter`,
  `beekeeping log` and `aquarium water log` are `dead-zone`, which per
  `docs/niche-selection.md` means unrated competitors — evidence of failure, not vacancy. The ham
  radio shelf I measured directly is not a dead zone (§3.1). Those two facts sit next to each other
  on purpose.

### 1.4 The homonym finding, which is why a guard verdict is not evidence

The rule in `.cursor/rules/ideation-gate.mdc` is that a `candidate` verdict is not evidence on its
own, because the guard is blind to homonym collisions. I found two, and one of them would have been
enough to select a bad idea:

| Query | Guard verdict | Hand job-share | The collision |
|---|---|---|---|
| `hair color formula` | **candidate**, on-topic 11/12 = 0.92 | **2/12 = 0.17** for the *formula* job | Nine of the twelve visible results are hair-*changer* photo editors (`com.ttech.aihairtryon` 4.6, `com.interstellarapps.hairstyleai` 4.2, `com.luckystars.haircolorchanger` 2.5, and five more). Only `com.stayincharm` (4.4) and `com.itbruderhofer.tintlog` (unrated) serve a professional formula record. The words matched; the job did not. |
| `student driving log` | **mixed**, guard *polluted* 6/20 = 0.30 | **7/20 = 0.35** for hour-logging | The shelf is dominated by permit *test-prep* apps (`com.aceable.aceablede_android` 4.7, `dto.ee.dmv.genius` 4.7, `com.driverlicenseapp` Zutobi 4.6, `com.brainsoft.dmv.permit.driving.test` 4.7, and more). The guard flagged the pollution itself, which is the sensor behaving correctly, but the hand count is what tells you the addressable shelf is seven apps, not twenty. |
| `hair color formula app` (longer query, separate run) | **candidate**, on-topic **28/30 = 0.93** | **3/30 = 0.10** for the *formula* job | Widening the query made the guard *more* confident (0.93) while the addressable share **fell by 40 %** (2/12 → 3/30). The three on-job results are `com.stayincharm` (4.4), `com.itbruderhofer.tintlog` (unrated) and `com.app.colorvault` (unrated). The other 27 are hair-changer and AI try-on editors. |

The `student driving log` row also shows the guard working as designed — it *reported* pollution
rather than calling it `candidate`. The two `hair color` rows show the guard failing in the way the
rule predicts, and the second is the more damning: **a broader query raised the guard's confidence
from 0.92 to 0.93 while cutting the hand-verified job share from 0.17 to 0.10.** A recipe chosen from
the guard's verdict alone would have shipped a formula app into a shelf that is nine tenths photo
filters.

MEASURED, in a separate run recorded in this session as background task `993427`
(`node scripts/scan-titles.mjs --query "hair color formula app" --query "dog walking client log"`,
151,796 ms, exit 0): the third on-job result, `com.app.colorvault`, is titled **"Color Formula
Vault"** — the same name as the title this proposal assigned to Recipe 3. That makes the name a
measured collision rather than a hypothetical one, and it is a second, independent reason to refuse
Recipe 3: a store search for the product's own name already splits between two apps.

### 1.5 The one gap I am not closing

I did not measure install-to-purchase conversion for any of these five categories, and no repository
file records it. Every install-to-revenue ladder in §3 therefore multiplies a **2 % conversion
assumption that is UNVERIFIED**. This is the single largest uncertainty in the document: it is the
difference between "1,000 installs a month earns $80" and "1,000 installs a month earns $8". The
commands that would settle it are on Play Console, not in the sensors:
`scripts/play-upload.mjs` and the Play Developer API can read an app's reports but cannot create an
app (`docs/research/2026-09-23-google-play-automation-surface.md`), and the factory has never had a
published app, so the measurement does not exist yet for any slot
(`docs/reviews/2026-09-26-slot-1-economics-postmortem.md` §6: "No revenue data exists at all").

### 1.6 The killed list, and confirmation that nothing here re-proposes it

ON RECORD: `docs/research/2026-09-24-market-decision.md` §3 binds the following as killed —
`process server log`, `dog groomer appointment book`, `field service work order app`,
`stuttering practice app`, `dementia caregiver log`, `farrier hoof record`, `livestock breeding
record`, `machinist feeds and speeds`, handloading/reloading data, plant-health diagnosis, and
visual-schedule-for-autism. None of the five recipes below is on that list, and none is a
calculator, converter, or generic arithmetic tool.

---

## 2. Why these five, and what was rejected on the way

The five differ in **category and job**, which is a hard constraint because Google Play's
Repetitive Content policy reaches the whole account
(`docs/monetization-strategy.md` §1.2, `.cursor/rules/ideation-gate.mdc`). No two of them can be
described by one sentence:

| # | Category | Job |
|---|---|---|
| 1 | Amateur radio / hobby field operations | Record contacts so another logging program can read them |
| 2 | Scuba diving / adventure record | Keep a dive logbook the diver owns |
| 3 | Beauty professional / client record | Keep a per-client colour formula with a photo |
| 4 | Driver licensing / supervised practice record | Certify supervised day and night driving hours |
| 5 | Travel / packing | Rebuild a trip packing list without starting from nothing |

Rejected along the way, with the reason, so a later session does not redo the work:

| Candidate | Why not |
|---|---|
| `mushroom foraging guide` | Strongest row in the whole screen (§1.3), rejected on liability: a wrong identification can kill the user, and Play policy plus common sense treat that as a content-safety problem the factory should not take on with a 4-hour build. |
| `beekeeping log`, `aquarium water log`, `knitting counter` | `dead-zone` verdicts — unrated competitors are evidence of failure, not vacancy (`docs/niche-selection.md`). |
| `visual schedule autism` | ON RECORD killed, `docs/research/2026-09-24-market-decision.md` §3. |
| `bolt torque calculator`, `electrician calculator` | Calculators. Burned. |
| `migraine tracker`, `pet medication reminder` | `mixed` and adjacent to health claims; both would need a medical-claims posture the factory has no appetite for. Not rejected as markets, rejected as *this* factory's next slot. |
| `baby feeding tracker` | `fortress` (14/16 rated, median 4.75) — the incumbents are good, so there is no wedge. |
| `language pronunciation practice` | `fortress`, and the strongest entrants are AI-voice products; a thin wrapper risk. |
| `dog walking client log` | **Measured on 2026-09-26 and deliberately not taken.** `node scripts/scan-titles.mjs --query "dog walking client log"` returned `candidate` at guard 20/30 = 0.67 over 30 visible results, so demand looked real. Hand-classified, the shelf is three jobs at once: the marketplaces (`com.rover.android` 4.5, `com.ionicframework.wagandroid554504` Wag! 4.4, `com.petbacker.android` 4.2), the walker-side client managers (`com.sweetss.timetopet` Time To Pet 3.7, `com.cloudypetcare.petcareapp` 3.8, `com.diddydashboard.android` 3.5, `com.scoutforpets.staff` 3.3, `io.gonative.android.zwnjm` Swifto unrated, `com.sofanaut.dogwalkinghq` unrated, `com.packplan.mobile.app` unrated), and the consumer trackers (`com.doggylogs.android` 4.1, `com.mobikode.dog` DogLog 4.5, `be.foxtastic.dogwalk` Amiko 4.4). **Two reasons not to take it:** it is the same *job* as `dog groomer appointment book`, which is ON RECORD killed (`docs/research/2026-09-24-market-decision.md` §3: "Polluted (3/11) and a fortress underneath: MoeGo 4.3★/309, TapGroom 4.1★/390, DaySmart 3.3★/413, all 10K+") — pet-service provider client bookkeeping, one vertical over — and this shelf shows the same fortress shape, with two marketplace apps at 4.4–4.5. It is recorded here so a later session does not re-derive it. |

**Note on the `dog walking client log` row.** I did not add it as a sixth recipe: Step 2 requires
exactly five, and it fails the same test the killed grooming idea failed. The reason to record the
measurement anyway is that a `candidate` verdict at 0.67 with a hand-verifiable demand shape is
exactly what a fresh session would find attractive, and the adjacency to a *killed* idea is only
visible if you have read §3 of the market decision. That adjacency is the finding, not the shelf.

---

## 3. The five recipes

### 3.1 Recipe 1 — Amateur radio logbook (`amateur-radio-logbook`)

**Category / job.** Hobby field operations / record radio contacts at a field site and hand the
result to another logging program.

**Archetype.** `purchase_led_single_unlock`, `$4.99`, requires the `billing` capability.

**Demand evidence (MEASURED, 2026-09-26).** `node scripts/scan-titles.mjs --query "amateur radio
logbook"` returned `verdict=mixed guard=on-topic (22/29, share 0.76)` over 29 visible results. My
hand classification of those 29, by job:

| Job in the shelf | Count | Examples with rating |
|---|---:|---|
| Contact logging / capture | **16/29 = 0.55** | `com.talixa.hamradiolog` 3.7, `com.talixa.hamradiolog.adfree` 3.3, `com.kimbrelk.android.hamlogger` 4.0, `app.hamrs` 4.3, `com.ham2k.polo.beta` 4.6, `com.worldradioleague.app` 4.6, `com.kviation.logbook` 4.7, `com.group427.netloggercompanion` 4.3, `com.hamradiolog.amateur` unrated, `com.unixeer.hamlogging` unrated, `com.mircules.HAM.QuickLog` unrated, `cz.ok2dds.log.twa` unrated, `nz.co.oppware.ultimate_logbook` unrated, `appinventor.ai_mattia_zomer.GYKLOG` unrated, `appinventor.ai_MzMd1494.HamLog` 4.1, `com.phlabbe94.hamradio` unrated |
| Examination and study | 4/29 = 0.14 | `com.criticalcommunicationsmedia.hamradioprep3` 4.6, `com.vemento.learning.radioham` 4.4, `com.edupass.hamradiotechnician` 4.6, `appinventor.ai_izzybella419.AmateurRadioQCodeQuiz` 5 |
| Reference and utility tools | 9/29 = 0.31 | `com.daveyhollenberg.amateurradiotoolkit` 4.3, `com.kimbrelk.ham` 4.1, `com.ft8.app` 5, `net.hamandmore.ham` 4.3, `appinventor.ai_izzybella419.AmateurHamRadioGridSquareCalculator` 4.8, and four unrated toolkits |

Overall, **18 of the 29 visible results are rated = 0.62**, which is above the 0.35 threshold that
`docs/niche-selection.md` uses to declare a dead zone. Within the logging subset, **9 of 16 are
rated = 0.56**, and **3 of those 9 are below 4.2** (`com.talixa.hamradiolog` 3.7,
`com.talixa.hamradiolog.adfree` 3.3, `com.kimbrelk.android.hamlogger` 4.0); the median of the rated
logging apps is 4.3. So the *narrow* job is thinner than the query's overall rated share, and this
is not a dead zone but it is not a well-served shelf either.

Installs and monetisation (MEASURED, `node scripts/enrich-market.mjs --top 8 --concurrency 3
--query "amateur radio logbook"`):

| App | Rating | Reviews (exact) | Installs | Ads | IAP | Price |
|---|---:|---:|---|---|---|---|
| `com.kimbrelk.android.hamlogger` Ham Radio Logger | 4.0 | 149 | `10K+` | true | true | free |
| `com.worldradioleague.app` World Radio League | 4.6 | 156 | `10K+` | false | false | free |
| `com.ham2k.polo.beta` Ham2K PoLo: Portable Logger | 4.6 | 199 | `10K+` | false | true | free |
| `com.talixa.hamradiolog` Ham Radio Log | 3.7 | 198 | `10K+` | true | false | free |
| `com.hamradiolog.amateur` Ham Radio Log (Sida Oy) | unrated | not shown | `100+` | false | false | free |
| `com.criticalcommunicationsmedia.hamradioprep3` Ham Radio Prep | 4.6 | 1,520 | `100K+` | false | true | free |
| `com.daveyhollenberg.amateurradiotoolkit` Amateur Radio Toolkit | 4.3 | 2,870 | `100K+` | true | true | free |
| `com.kimbrelk.ham` Ham Radio US Band Plan | 4.1 | 467 | `50K+` | true | true | free |

The `100K+` rows are a study app and a toolkit, not loggers. **The largest dedicated logger in this
sample is `10K+`.** That is the honest ceiling of the shelf: this is a small market. An unrated
competitor with `100+` installs (`com.hamradiolog.amateur`) is evidence of failure, not vacancy.

Verbatim complaints (MEASURED, `node scripts/scan-complaints.mjs --package
com.talixa.hamradiolog --top 6`, 3 reviews read, 2 critical, both classified `broken`):

> 2★ "I have problem with exporting adif file and database. It doesn't work!"

> 2★ "Seems stable which is nice but get your comments right the first time. Re-editing more than
> the single line width of your screen is impossible."

The export failure is the wedge. ADIF is the interchange format every other logging program, LoTW
and QRZ read (INFERRED from the format's role; I did not measure which downstream programs import
it).

**The math.** Net per sale at `$4.99`, using the factory's own assumption set
(`docs/monetization-strategy.md` §2.2): `F_billing = 0.15`, `F_refund = 0.02`, `F_fx = 0.02`.

```
net multiplier = (1−0.15)(1−0.02)(1−0.02) = 0.85 × 0.98 × 0.98 = 0.8163
net per sale   = 4.99 × 0.8163 = $4.07
sales for $1,000/month = 1000 / 4.07 = 245.7 → 246 sales/month (≈ 8.1/day)
```

`F_fraud` is excluded here, and this is the one judgement in the arithmetic. The factory's
`F_fraud = 0.20` default exists for products that grant credit on the client's word before payment
is confirmed (`docs/monetization-strategy.md` §2.2: "it is the share the app *allows* by granting
credit on the client's word"). This recipe serves nothing: the unlock is a boolean the slot flips
when Google Play reports the purchase, so there is no granted-then-unpaid unit. INFERRED, and the
reason it is safe to write the plan this way.

Conversion is UNVERIFIED at 2 % of installs. The ladder, so the owner can see what each level of
distribution is worth:

| Installs / month | Sales / month | Revenue / month |
|---:|---:|---:|
| 300 | 6 | $24 |
| 1,000 | 20 | $81 |
| 3,000 | 60 | $244 |
| 12,300 | 246 | $1,000 |

Reaching `$1,000/month` needs roughly 12,300 installs a month, which is more than the largest
dedicated logger in the shelf has ever accumulated. **This recipe is not a $1,000-a-month idea; it
is a $25–$250-a-month idea at plausible volumes.** A subscription would be worse, not better: there
is nothing recurring to serve, so a subscription would be rent for a static feature and would
convert far below a one-time unlock.

**The marketing.** The paying user is a licensed operator who is already logging on paper or in a
spreadsheet and has been burned by an export that did not work. Discovery is search (`ham radio
log`, `adif logbook`, `pota logger`) plus the communities where operators actually talk: QRZ forums,
`r/amateurradio`, POTA and SOTA activators, and local radio clubs. Three reasons they would not just
use a free web tool or a spreadsheet: a spreadsheet cannot emit ADIF that LoTW and QRZ import; a
web logger needs connectivity and this job happens on a hilltop with no signal; and the measured
incumbent that is ad-supported has an open export-bug complaint. The competing free option is
`app.hamrs` (HAMRS, 4.3★, IAP declared, install bucket not in the enriched top 8) — that is a real
competitor and the plan must beat it, not ignore it.

**The timing.** I have no timing argument. ADIF is a stable, long-lived format and Android's file
access APIs have been settled for a decade. The app is durable, which is not the same as timely,
and I am not going to invent a trend to fill this paragraph. The one timing fact that *is* real is
against starting another slot at all: ON RECORD, `docs/monetization-strategy.md` §1.1 records that
personal Play Console accounts created after 2023-11-13 must run a closed test with at least 12
testers for 14 days before production access, so every recipe in this document carries a 14-day
delay between "built" and "earning" (UNVERIFIED for this account; see §7).

**The build.** Estimated **3.5–5 agent hours** to a verified slot.

Screens: contact list with search; contact entry (callsign, band, mode, frequency, RST sent and
received, UTC date/time, notes); ADIF import via the system file picker; ADIF export via the system
file picker; settings (own callsign, grid square, station); one paywall sheet. Domain work: an ADIF
writer and a tolerant ADIF reader, band-from-frequency mapping, UTC handling. Permissions: **none**.
Capabilities: `billing`. Server: **none**. Why the estimate is 3.5–5 and not 1: ADIF is a
line-based record format with type codes for each field, so the reader/writer is real code with real
edge cases (contests emit formats that differ), and the previous slot's recorded scaffold-to-build
span was 4 hours 49 minutes for a *simpler* app
(`docs/reviews/2026-09-26-slot-1-economics-postmortem.md` §1).

**The kills.** The cheapest early proof that this is wrong is to put a two-screen prototype in front
of ten operators in `r/amateurradio` or a club meeting and ask which of them currently exports ADIF
from a phone at all. If fewer than three do, the export wedge is not a job. Kill criteria:
**60 days, fewer than 300 installs, or less than $50 revenue.**

**Scaffold command.**

```bash
node scripts/new-slot.mjs --slug amateur-radio-logbook --name "Ham Radio Logbook" --idea "Offline ADIF-native amateur radio contact log: log, import and export contacts with no account, unlocking unlimited logging and export once." --economics docs/ideation/plans/amateur-radio-logbook.economics.json
```

---

### 3.2 Recipe 2 — Scuba dive logbook (`scuba-dive-log`)

**Category / job.** Adventure record / keep a dive history the diver owns and can take with them.

**Archetype.** `purchase_led_single_unlock`, `$5.99`, requires the `billing` capability.

**Demand evidence (MEASURED, 2026-09-26).** `node scripts/scan-titles.mjs --query "scuba dive log"`
returned `verdict=mixed guard=on-topic (24/30, share 0.80)`. Hand classification of those 30:
**25/30 = 0.83** serve a logbook, logbook-and-social, or dive-planning job. The five that do not are
`com.duns.padiapp` (PADI, 4.7), `com.divessi.ssi` (SSI, 4.8), `com.garmin.android.apps.dive`
(Garmin Dive, 4.8), `com.lifeplus.diveplus` (Dive+, 4.5) and `ai.divesight` (a dive-forecast app,
unrated). The hand count is slightly *more* favourable than the guard's 24/30 — the opposite
direction from the collisions in §1.4, where the guard over-counted the job. Neither direction can
be trusted without the hand check.

Of the 30 visible, **12 are rated = 0.40**, and **5 of those 12 are below 4.2 = 0.42**. The floor is
low and the top is high:

| App | Rating | Reviews (exact) | Installs | Note |
|---|---:|---:|---|---|
| `com.shearwater.dive` Dive Shearwater | **1.7** | not measured | not measured | hardware companion |
| `com.pelagicnet.diverlogplus` DiverLog+ | **1.9** | not measured | `50K+` (MEASURED via `scan-complaints`) | hardware companion (Aqualung) |
| `com.wear_snorkeling.wear_snorkeling` Dive Watch | 2.8 | not measured | not measured | |
| `com.uwatec.logtrak2` SCUBAPRO LogTRAK 2.0 | 3.1 | not measured | not measured | hardware companion |
| `com.confitek.divemateusb` DiveMate | 3.6 | not measured | not measured | hardware sync |
| `com.divinglog.divelog` Diving Log | 4.5 | not measured | not measured | the strong incumbent |
| `com.duns.padiapp` PADI | 4.7 | not measured | not measured | training ecosystem |
| `com.divessi.ssi` SSI | 4.8 | not measured | not measured | training ecosystem |
| `com.garmin.android.apps.dive` Garmin Dive | 4.8 | not measured | not measured | hardware companion, good |

Verbatim complaints (MEASURED, `node scripts/scan-complaints.mjs --package
com.pelagicnet.diverlogplus --top 6`, 3 reviews read, **3 of 3 critical, all classified
`data-loss`**):

> 1★ "Terrible app, I had two different ways to login. The main was the Facebook login and backup
> was an email address. Have tried logging in with the Faceb"

> 1★ "I am running diverlog+ on a Pixel 9 Pro. My Oceanic Veo 4.0 has had trouble for the past
> month syncing via the app. I can change settings via the app,"

> 1★ "Very disappointed. While the app syncs well. The elapsed dive time is incorrect. However, the
> tank recorded bottom time is correct. For instance, on a"

**The honest read of that evidence, including the part that weakens this recipe.** The strongest
measured complaints are about **sync with a dive computer** and about a **forced second login**.
This recipe deliberately does not build sync. So it answers the login complaint by removing
accounts, and it answers the data-loss complaint by never using a cloud, but it *ignores* the sync
complaint — and sync is the feature the 1.9★ and 1.7★ incumbents are leaders in. A diver who wants
their computer's data in the log will not be served here, and the well-rated leaders (Garmin 4.8,
PADI 4.7, Diving Log 4.5) already cover the segment that wants a good app. The addressable user is
narrower than the shelf suggests: a diver who **manually logs** and wants a file they own. Whether
that user exists in numbers is UNVERIFIED.

**The math.** Net per sale at `$5.99`:

```
net multiplier = (1−0.15)(1−0.02)(1−0.02) = 0.8163
net per sale   = 5.99 × 0.8163 = $4.89
sales for $1,000/month = 1000 / 4.89 = 204.5 → 205 sales/month (≈ 6.8/day)
```

At the UNVERIFIED 2 % conversion:

| Installs / month | Sales / month | Revenue / month |
|---:|---:|---:|
| 300 | 6 | $29 |
| 1,000 | 20 | $98 |
| 3,000 | 60 | $293 |
| 10,250 | 205 | $1,000 |

`F_fraud` is excluded for the same reason as Recipe 1: nothing is served, so there is no
granted-before-paid unit. The price is set above Recipe 1's because the measured leaders in this
category are free apps from hardware vendors, so the willingness to pay is signalled by a real paid
incumbent — `com.divinglog.divelog` at 4.5★ (INFERRED; I did not measure Diving Log's price, so the
justification for `$5.99` over `$4.99` is weak and this is listed in §7).

**The marketing.** The paying user dives recreationally several times a year, logs by hand, and does
not want a second account. Discovery is search (`dive log`, `dive logbook`, `scuba log`) plus
`r/scuba`, ScubaBoard, dive-shop counters, and dive-travel YouTube. The three reasons not to use a
free tool are: Subsurface is a desktop program, so it does not solve logging on a boat; a paper
logbook gets lost and cannot produce a per-year summary; and the free phone options are the
hardware vendors' own apps, which is precisely where the measured `50K+`-install app is rated 1.9.
The strongest counter-argument, stated plainly: **Subsurface is free, open-source, and excellent**,
and it caps what a paid phone logbook can charge.

**The timing.** The trend that is real is that dive computers have moved to Bluetooth companions
whose software is measured at 1.7–3.1★. But that trend argues for a *sync* product, which this
recipe is not. So the honest position is that this recipe has a **timing argument for a different
app than the one proposed**, and no timing argument for itself.

**The build.** Estimated **4–6 agent hours**.

Screens: dive list with search and a per-year summary; dive entry (date, site, max and average
depth, bottom time, surface interval, gas mix, start and end tank pressure, weight, exposure suit,
buddy, notes, photo); site list with GPS-free manual entry; gear locker; CSV export and a printable
PDF summary; one paywall sheet. Permissions: **none** (photos come through the Android Photo
Picker, which needs no permission). Capabilities: `billing`. Server: **none** — and this is the
point: the measured leaders lose logs in sync, so not syncing is a feature. The estimate is the
highest of the five because the dive record has the most fields, the yearly summary is real
aggregation, and PDF output must be produced without pulling in a heavyweight library.

**The kills.** The cheapest early proof is a single question in `r/scuba`: how many of you import
from a dive computer versus typing the dive in by hand. If the manual-logging share is under about a
third, the addressable user is too small and the recipe dies for the price of one post. Kill
criteria: **60 days, fewer than 300 installs, or less than $60 revenue.**

**Scaffold command.**

```bash
node scripts/new-slot.mjs --slug scuba-dive-log --name "Dive Logbook" --idea "Manual, offline dive logbook that a diver owns as a file: log every dive, attach a site and gear, export CSV and a printable summary, no account and no cloud sync." --economics docs/ideation/plans/scuba-dive-log.economics.json
```

---

### 3.3 Recipe 3 — Hair colour formula vault (`hair-color-formula-vault`)

**This is the recipe I recommend refusing.** It is included in full because the refusal is only
credible if the idea is stated properly.

**Category / job.** Beauty professional / keep one colour formula per client, with a photo, where
the colourist can find it next appointment.

**Archetype.** `purchase_led_single_unlock`, `$7.99`, requires the `billing` capability.

**Demand evidence — and this is where it fails.** `node scripts/scan-titles.mjs --query "hair color
formula"` returned `verdict=candidate guard=on-topic (11/12, share 0.92)`. That verdict is a
homonym artifact. Hand-classified, the 12 visible results are:

| Job | Count | The apps |
|---|---:|---|
| Hair-*changer* photo editor | **9/12** | `com.ttech.aihairtryon` 4.6, `com.interstellarapps.hairstyleai` 4.2, `com.msaapps.morfio` unrated, `app.hairlabai` 3.9, `com.colorcoach` unrated, `haircolour.hairstyle.photoeditor.haircolorchanger` 3.5, `com.luckystars.haircolorchanger` 2.5, `com.photoeditor.games.hair.color.change` 3.3, `com.haircolor.ultimate` 2.8 |
| Professional formula record | **2/12** | `com.stayincharm` Charm (4.4), `com.itbruderhofer.tintlog` TintLog (unrated) |
| Brand tool | 1/12 | `com.redken.haircolor` Redken Style Station (4.5, published by L'Oréal) |

**A wider query made this worse, not better** (MEASURED in a separate run recorded in this session as
background task `993427`, 151,796 ms, exit 0). `node scripts/scan-titles.mjs --query "hair color
formula app"` returned **30** visible results with `verdict=candidate guard=on-topic (28/30, share
0.93)` — the guard's confidence *rose* — while the hand-classified formula job fell to **3/30 =
0.10**: `com.stayincharm` (4.4), `com.itbruderhofer.tintlog` (unrated) and `com.app.colorvault`
(unrated), against 27 hair-changer and AI try-on editors. The third on-job app, `com.app.colorvault`,
is titled **"Color Formula Vault"**, which collides with the title this proposal gave Recipe 3
("Colour Formula Vault"): the product's own name would already be split on the search page. That is a
measured collision, and it is a second independent reason to refuse this recipe.

To check whether the job exists at all I ran a second query with no homonym,
`node scripts/scan-titles.mjs --query "salon client record book"`, which returned
`verdict=fortress guard=on-topic (9/11, share 0.82)`. That shelf is 9/11 client-record or CRM apps —
and it is a **fortress**, held by booking platforms, not by formula tools (MEASURED,
`node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "salon client record book"`):

| App | Rating | Reviews (exact) | Installs | Ads | IAP | Price |
|---|---:|---:|---|---|---|---|
| `net.booksy.customer` Booksy for Customers | 4.9 | 1,029,068 | `10M+` | false | false | free |
| `net.booksy.business` Booksy Biz | 4.7 | 31,417 | `1M+` | false | true | free |
| `com.styleseat.promobile` StyleSeat | 4.8 | 61,284 | `1M+` | false | false | free |
| `com.clientnotepadmobilefrontend` Client Note Tracker | 4.7 | 115 | `5K+` | false | true | free |
| `com.lancerdog.hostess_note` ClientNote: Salon CRM | unrated | not shown | `10K+` | true | true | free |
| `com.jacksontempra.apps.hea` Salon Formulator | unrated | not shown | `100+` | false | true | free |
| `com.clientsrecords.app.android` Clients Records and Forms | unrated | not shown | `1K+` | false | true | free |
| `salonbook.salonappointments.salonstockmanager` SalonBook | unrated | not shown | `100+` | true | false | free |

So the formula job as a distinct product is served by exactly **two** apps I found
(`com.stayincharm` 4.4, 10K+; `com.jacksontempra.apps.hea` unrated, 100+), and the adjacent
client-record job is served by a fortress of booking platforms with millions of installs.

Complaint evidence: `node scripts/scan-complaints.mjs --package com.stayincharm --top 6` returned
`4.4★ 10K+ reviews=3 critical=0 top=none unclassified=0`. **Three reviews read, zero critical.** I
have no measured complaint to answer. The only measured weakness in the whole cluster is that
`com.clientnotepadmobilefrontend` is at `5K+` installs and `com.lancerdog.hostess_note` is unrated at
`10K+`, which per `docs/niche-selection.md` is a churn signal, not an opening.

**Verdict on evidence: UNMEASURED demand.** Two of twelve results serve the job, the one rated
incumbent has no critical reviews in the sample, and the shelf adjacent to it is a fortress. A
`candidate` verdict from the guard is the only thing that looked positive, and the guard was wrong
for the documented homonym reason.

**The math, for completeness.** Net per sale at `$7.99`:

```
net multiplier = (1−0.15)(1−0.02)(1−0.02) = 0.8163
net per sale   = 7.99 × 0.8163 = $6.52
sales for $1,000/month = 1000 / 6.52 = 153.4 → 154 sales/month (≈ 5.1/day)
```

At the UNVERIFIED 2 % conversion:

| Installs / month | Sales / month | Revenue / month |
|---:|---:|---:|
| 200 | 4 | $26 |
| 1,000 | 20 | $130 |
| 7,700 | 154 | $1,000 |

The arithmetic is the *best* of the five because `$7.99` is the highest price and a professional
tool from a professional audience can carry it. That is exactly why this recipe had to be refused on
evidence rather than on economics: a good price on a demand claim I cannot support is the
`pipe-sizing` failure with more decimal places.

**The marketing.** If it were built, the paying user is an independent colourist or a salon owner
who mixes colour and currently keeps formulas on a paper card or in a phone note. Discovery would be
search (which, as measured, is polluted by photo filters), `r/Hairstylist`, colourist educators on
Instagram and TikTok, and distributor representatives. The three reasons not to use a notebook are:
search across clients, a photo attached to the formula, and a printable card. But the measured
evidence gives me **no complaint to answer**, which means the "reason they would not use the free
alternative" argument is entirely INFERRED.

**The timing.** There is none. I will not invent one.

**The build.** Estimated **3–4 agent hours**. Screens: client list, client detail, formula card
editor (brand, shade lines, developer volume, mixing ratio, processing time), photo attach through
the Photo Picker, printable card, one paywall sheet. Permissions: **none**. Capabilities: `billing`.
Server: **none**.

**The kills.** Kill criteria: **45 days, fewer than 200 installs, or less than $100 revenue** — the
tightest revenue floor of the five, because a professional-tool price demands a professional-tool
conversion and any number below that floor means the audience is not there. The cheapest early
proof is one question in a stylist community: how many of you can find a client's formula from four
months ago in under thirty seconds today. If the answer is "yes, my notebook", the app has no job.

**Scaffold command (recorded so the refusal is a decision, not an omission).**

```bash
node scripts/new-slot.mjs --slug hair-color-formula-vault --name "Colour Formula Vault" --idea "Offline per-client hair colour formula record for independent colourists: formula, developer, ratio, processing time and a before/after photo on one card per client." --economics docs/ideation/plans/hair-color-formula-vault.economics.json
```

---

### 3.4 Recipe 4 — Supervised driving hours log (`student-driving-hours-log`)

**Category / job.** Licensing record / certify supervised day and night driving hours to a state
licensing office.

**Archetype.** `ads_led_utility`, `$2.99` remove-ads and export unlock. This requires the `ads`
capability; the slot should also enable `billing`, which the gate permits, because the unlock is a
purchase. This is the only ads-led recipe of the five and the only one that exercises the `ads`
capability, which no slot has ever enabled.

**Demand evidence (MEASURED, 2026-09-26).** `node scripts/scan-titles.mjs --query "student driving
log"` returned `verdict=mixed guard=polluted (6/20, share 0.30)`. The guard reported pollution
correctly. Hand-classified, the 20 visible results split:

| Job | Count | The apps |
|---|---:|---|
| Supervised-hours logger | **7/20 = 0.35** | `dev.hairyotter07.studentdrivinglog` 4.2 (`10K+`, MEASURED via `scan-complaints`), `com.akashcentral.studentdrivinglogger` 4.6, `com.saferoadsalliance.roadready` 3.9, `com.otso.prod` RoadReady Ohio 4.2, `com.gotimelog.app` GoTime 3.7, `com.beefpattyapps.drivinglog` unrated, `com.drivemint.app` unrated |
| Permit test prep / drivers ed | **12/20 = 0.60** | `com.aceable.aceablede_android` 4.7, `dto.ee.dmv.genius` 4.7, `com.driverlicenseapp` Zutobi 4.6, `com.brainsoft.dmv.permit.driving.test` 4.7, `com.rallsoftwares.dmvpracticetest` 4.6, and six more |
| State government app | 1/20 | `gov.ga.dds.gadds` DDS 2 GO, 3.0 |

So the addressable shelf is **seven apps**, of which three are rated and two of those are below 4.2
(`com.saferoadsalliance.roadready` 3.9, `com.gotimelog.app` 3.7), and one rated incumbent is
unrated-adjacent (`com.beefpattyapps.drivinglog`, unrated, no rating to compare). Three of the seven
are unrated, which per `docs/niche-selection.md` is a failure signal.

Verbatim complaints (MEASURED, `node scripts/scan-complaints.mjs --package
dev.hairyotter07.studentdrivinglog --top 6`, 3 reviews read, 2 critical, classified `ads` and
`crash`):

> 2★ "Could be useful please fix the export option in the app, app crashes when clicked export. nice
> app though"

> 3★ "simple, too simple. no timer option that you can do to start and stop driving. good otherwise,
> no ads"

Two things to take from those: **the export crashes**, which is the feature the licensing office
needs, and the reviewer treats **the absence of ads as a virtue**, which is a warning about this
recipe's archetype.

**The math.** This recipe has two layers and they must be computed separately, because they behave
very differently.

*The ad layer.* ON RECORD, `docs/monetization-strategy.md` §2.1: reaching `$1,000/month` needs
roughly 700–1,000 DAU rewarded-centric, 2,000–6,000 DAU with interstitials and banners, or 11,000+
DAU banner-only. This app is a banner-and-interstitial utility with nothing worth a rewarded video
(the thing being unlocked is the export, which is a purchase), so it sits in the middle band.
`DAU` for a utility that a family uses for about thirty days and then never again is UNVERIFIED,
and this is the weakest number in the document. The INFERRED approximation used here is
`DAU ≈ 0.5 × monthly installs` in steady state: a family that is active on roughly half the days it
owns the app contributes about half a daily active user per install. Converting the §2.1 band
(2,000–6,000 DAU = ~$1,000/month, i.e. **$0.17–$0.50 per DAU per month**) into installs:

| Installs / month | Approx. steady DAU | Ad revenue / month |
|---:|---:|---:|
| 500 | ~250 | ≈ $42–125 |
| 1,000 | ~500 | ≈ $83–250 |
| 3,000 | ~1,500 | ≈ $250–750 |
| 6,000 | ~3,000 | ≈ $500–1,500 |
| 12,000 | ~6,000 | ≈ $1,000–3,000 |

*The unlock layer.* Net per sale at `$2.99`:

```
net multiplier = (1−0.15)(1−0.02)(1−0.02) = 0.8163
net per sale   = 2.99 × 0.8163 = $2.44
sales for $1,000/month = 1000 / 2.44 = 409.8 → 410 sales/month (≈ 13.7/day)
```

At the UNVERIFIED 2 % conversion, the unlock returns `1,000 installs × 0.02 × $2.44 = $49` per month.
So at 1,000 installs a month the honest total is roughly **$130–300 a month**, of which the ads are
the larger line and the unlock is the smaller, more predictable one — an ad impression's price is
outside our control while a purchase is a fixed price. That is what makes `ads_led_utility` the
honest archetype for this recipe rather than a hedge, and it is also why the unlock must not be
described as the primary line.

Reaching `$1,000/month` in total needs about **4,000–10,000 installs a month, sustained**, for an app
whose users stop after thirty days. For scale, the best dedicated hours-logger measured carries a
`10K+` **lifetime** install bucket, so the monthly figure required is close to the category leader's
entire lifetime audience. `$1,000/month` is therefore not reachable here in the near term.

`F_fraud` is excluded for the same reason as Recipes 1 and 2, and for this one the reasoning is
stronger: an ad-supported app has no granted-credit unit at all, and the unlock is Play-confirmed.

**The marketing.** The paying user is a parent, because the parent is who fills in the form and who
is motivated to get the licence. Discovery is search that is state-shaped (`driving hours log`,
`40 hours driving log`, and the state's name), plus parenting forums, driving schools, and
`r/teenagers`-adjacent parent communities. The three reasons not to use a free state PDF: the state
PDF is a paper log that gets lost in a glovebox; the night-hours arithmetic is fiddly and different
per state; and both measured complaints — a crashing export and the absence of a start/stop timer —
are things a native app fixes and a PDF cannot. The strongest counter-argument: **the state's own
PDF is free, official, and already accepted by the office**, so the app has to be easier than paper,
not merely digital.

**The timing.** No timing argument. State supervised-hours requirements are long-standing. I will
not manufacture a trend.

**The build.** Estimated **3–4 agent hours**. Screens: drive timer (start and stop, day or night,
computed and overridable), drive list, running totals per requirement, state requirement settings,
printable export, a banner slot and one remove-ads sheet. Permissions: **none**. Capabilities: `ads`
plus `billing`. Server: **none**. The design constraint that matters: the timer must survive the app
being backgrounded without a foreground service — store the wall-clock start time and compute
elapsed on resume, because a foreground service would add a permission, a notification, and a Play
declaration for no user benefit.

**The kills.** The cheapest early proof is to name the three states with the largest learner
populations and check whether each office accepts a printed, self-reported summary or requires its
own form. If none of the three accepts a printed summary, the export has nothing to be accepted by
and the app is a note-taker. Kill criteria: **45 days, fewer than 1,000 installs, or less than $30
revenue** — the highest install floor and the lowest revenue floor of the five, because this is an
ads-led recipe and its revenue per install is the smallest.

**Scaffold command.**

```bash
node scripts/new-slot.mjs --slug student-driving-hours-log --name "Driving Hours Log" --idea "Supervised driving hours tracker for learner drivers: start and stop a drive, split day and night hours, track totals against the state requirement, and print a summary for the licensing office." --economics docs/ideation/plans/student-driving-hours-log.economics.json
```

---

### 3.5 Recipe 5 — Trip packing checklist (`smart-packing-checklist`)

**Category / job.** Travel / rebuild a trip packing list without starting from nothing.

**Archetype.** `purchase_led_repeat_sales`, `$3.99` per destination or season pack, requiring the
`billing` capability. This is the only repeat-sales recipe of the five, and it is the only one that
exercises the billing capability's consumable path rather than a single unlock.

**Demand evidence (MEASURED, 2026-09-26).** `node scripts/scan-titles.mjs --query "packing
checklist"` returned `verdict=candidate guard=on-topic (30/30, share 1.00)`. This is the only query
of the five where the guard's share and the hand classification agree exactly: **30/30 = 1.00** of
the visible results are packing-list apps. Demand is not in question.

The problem is the opposite of Recipe 3. Installs and monetisation (MEASURED,
`node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "packing checklist"`):

| App | Rating | Reviews (exact) | Installs | Ads | IAP | Price |
|---|---:|---:|---|---|---|---|
| `com.YRH.PackPoint` PackPoint | 4.4 | 42,087 | `1M+` | false | true | free |
| `com.pixelbrewstudio.quickpack` QuickPack | 4.4 | 177 | `10K+` | false | true | free |
| `com.ouji.packmate` PackMate | 4.1 | 278 | `10K+` | false | true | free |
| `com.packparty.app` PackParty | 4.9 | 204 | `5K+` | false | true | free |
| `com.bitjust.packwise.app` AI Packing List | unrated | not shown | `1K+` | false | true | free |
| `com.tripkit.trip_packing_list` Trip Packing List | unrated | not shown | `5+` | false | false | free |
| `com.AsadDev.MyPackingChecklist` | unrated | not shown | `1+` | false | false | **paid `$1.09`** |
| `com.adam.PackPoint` PackPoint Premium | **3.1** | 119 | `5K+` | false | false | **paid `$2.99`** |

Three facts from that table and the title list:

1. **The leader is enormous.** `com.YRH.PackPoint` has 42,087 exact reviews and `1M+` installs, and
   the query's 30 visible results include roughly 18 unrated apps — a churn signal, per
   `docs/niche-selection.md`, not an opening.
2. **Paid-up-front is measured dead here.** The paid edition of the leader is `$2.99` with `5K+`
   installs and **3.1★**, against the free edition's `1M+` and 4.4★. That is the factory's own
   position (`docs/monetization-strategy.md` §4: a paid app is "effectively dead outside ad-hostile
   audiences") reproduced in one category.
3. **In-app purchase is the norm.** Five of the eight declare IAP, none declare ads, and every one
   is free. So the competitive shape is "free app, sell something inside it" — which is what this
   recipe proposes, and which means the recipe is not differentiated by its *model* at all.

Verbatim complaints (MEASURED, `node scripts/scan-complaints.mjs --package com.YRH.PackPoint
--top 6`, 3 reviews read, 3 critical, classified `account`, `broken`, `data-loss`, `missing-feature`):

> 2★ "I tried this one because it has the highest rating (4.7) and is the editor's choice. I am
> uninstalling it because you can not edit the travel dates an"

> 2★ "Used to LOVE this app. Now it's BAD. Concept is so handy, but it's gotten worse over time.
> There's a bug where it won't let me add items to my list be"

Note the first quote cites 4.7★ while the sensor measures 4.4★ today: the review is older than the
current rating, which is itself a decay signal for the leader.

**The math.** Net per sale at `$3.99`, and repeat sales change the shape:

```
net multiplier = (1−0.15)(1−0.02)(1−0.02) = 0.8163
net per sale   = 3.99 × 0.8163 = $3.26
sales for $1,000/month = 1000 / 3.26 = 306.7 → 307 sales/month (≈ 10.2/day)
```

At the UNVERIFIED 2 % conversion **and one pack per buyer**:

| Installs / month | Pack buyers / month | Revenue / month |
|---:|---:|---:|
| 500 | 10 | $33 |
| 3,000 | 60 | $196 |
| 15,350 | 307 | $1,000 |

The repeat-sales archetype's advantage should be that a buyer buys more than once, and here it is
weak: a traveller who packs twice a year buys at most two packs a year, so the repeat factor is
closer to two than to ten (INFERRED). `F_fraud` is excluded, and for this recipe the reasoning is
strongest of all: the packs are static content shipped in the APK, so there is no served cost and
nothing to grant before payment.

`$1,000/month` needs about 15,350 installs a month in a category whose leader has 42,087 reviews
accumulated over roughly a decade. **This recipe is not a $1,000-a-month idea either.**

**The marketing.** The paying user travels two to four times a year and repacks the same list. The
three reasons not to use a free app or Google Keep: a per-bag weight budget that adds up the items
you ticked, packs that remember a whole trip so the next one starts half-done, and offline
functioning at the airport. Discovery is search (`packing list`, `travel checklist`) plus
`r/onebag`, `r/travel`, cruise forums and travel TikTok. The honest problem with this marketing
paragraph is that a free competitor with 42,087 reviews will appear above the app in the same search,
and a static pack list is copyable in a week, so nothing in the format defends the price.

**The timing.** There is no favourable trend, but there is a **decay signal in the leader**: a
measured 3.1★ paid edition, 2★ complaints about not being able to edit travel dates and not being
able to add items, and a review that quotes a higher rating than the app has today. A decaying
leader is a quality opening, not a market shift, and I am labelling it as exactly that.

**The build.** Estimated **3–4 agent hours**. Screens: trip list, trip detail with categories and
tickable items, item library with a per-item weight, weight totals per bag against a limit, a pack
store, one purchase sheet. Permissions: **none**. Capabilities: `billing`. Server: **none**.

**The kills.** Kill criteria: **60 days, fewer than 500 installs, or less than $40 revenue.** The
cheapest early proof is a single AdMob-free keyword test: publish nothing, but read the top twenty
results for `packing list` and check whether any paid pack product exists above 100 installs. If
none does, the pack model has no precedent in the category and the buyer does not exist. A second,
cheaper probe is one question in `r/onebag`: would you pay for a prebuilt destination list when the
free app's list is editable. If the answer is no, the recipe is dead before any code exists.

**Scaffold command.**

```bash
node scripts/new-slot.mjs --slug smart-packing-checklist --name "Trip Packing Packs" --idea "Offline packing checklist with reusable trip packs: tick items, attach a per-item weight, see each bag's total against its limit, and add destination packs as repeat purchases." --economics docs/ideation/plans/smart-packing-checklist.economics.json
```

---

## 4. Comparison

| # | Category | Job | Archetype | Price | Capability needed | Build hours | Demand evidence strength | Server | First expensive failure it risks | Score |
|---:|---|---|---|---:|---|---:|---|---|---|---:|
| 1 | Amateur radio | Log contacts other programs can read | `purchase_led_single_unlock` | `$4.99` | `billing` | 3.5–5 | **MEASURED** (29 titles hand-checked, 8 apps enriched, 2 complaints) | no | The market is too small: the biggest dedicated logger is `10K+` installs | **8/10** |
| 2 | Scuba diving | Own the dive logbook as a file | `purchase_led_single_unlock` | `$5.99` | `billing` | 4–6 | **MEASURED** (30 titles, 3 critical complaints on a 1.9★ `50K+` app) | no | The wedge I answer is sync, which this app does not build; Subsurface is free and good | **6/10** |
| 3 | Beauty professional | Per-client colour formula card | `purchase_led_single_unlock` | `$7.99` | `billing` | 3–4 | **UNMEASURED for the job** (3/30 hand-relevant at the wider query, guard verdict was a homonym artifact, 0 critical complaints measured, and the proposed name collides with an existing app) | no | Nobody pays for what a notebook already does, and there is no evidence they want it | **3/10** |
| 4 | Driver licensing | Certify supervised day and night hours | `ads_led_utility` + `billing`  | `$2.99` | `ads`, `billing` | 3–4 | **MEASURED** (20 titles, 7 hand-relevant, 2 complaints incl. a crashing export) | no | The volume the ad model needs: about 4,000–10,000 installs a month for $1,000, against a category leader holding a `10K+` lifetime bucket | **6/10** |
| 5 | Travel | Rebuild a packing list | `purchase_led_repeat_sales` | `$3.99` | `billing` | 3–4 | **MEASURED and the strongest of the five** (30/30 job-share, 8 apps enriched) | no | A 1M+ free leader with 42,087 reviews, and a static pack is copyable in a week | **4/10** |

**How the scores were assigned.** Each score starts at 5 and moves on four things: demand evidence
strength (Recipe 1 and 5 gained; Recipe 3 lost everything), distinctness of the job (Recipe 1 and 2
gained, Recipe 5 lost most of its gain), revenue per install (Recipe 2 and 3 gained on price; Recipe
4 lost), and the size of the addressable market against the build (Recipe 1's small market costs it
two points but its narrowness keeps demand clean; Recipe 5's large market is exactly why nothing is
distinct). Recipe 3's 3/10 is a demand failure, not a price failure.

**Note on the revenue ceiling.** Only Recipe 4 has an arithmetically visible path to `$1,000/month`,
and that path needs about 4,000–10,000 installs a month sustained against a category whose leader
holds a `10K+` lifetime bucket. The other four need roughly 7,700–15,400 installs a month at the
UNVERIFIED 2 % conversion. So the honest statement is: **this proposal set does not contain a
`$1,000/month` slot at plausible install volumes**, and saying so is more useful than adding a sixth
idea that pretends otherwise. What the set contains is five slots whose combined realistic ceiling is
a few hundred dollars a month, and whose first purpose is to make the factory measure a purchase at
all.

---

## 5. Recommendation

### 5.1 Build first: Recipe 1, the amateur radio logbook

It is the only recipe of the five where all three of the following are true at once: the demand was
measured on a shelf I hand-classified rather than accepted from the guard (16 of 29 results serve
the logging job, and of those 16, 9 are rated and 3 of the 9 are below 4.2); there is a **measured,
verbatim complaint
that the product answers directly** (`"I have problem with exporting adif file and database. It
doesn't work!"`), which is a job-shaped defect rather than a taste complaint; and it is the cheapest
of the five to build because it needs no camera, no timer, no aggregation, no permission and no
server — the whole app is a list, a form, and an ADIF reader and writer.

The reason this is the *right* first slot is not that it will earn the most; it will not. It is that
it is the cheapest slot that can **teach the factory what a purchase looks like**. Every slot to date
has been `scaffolded` with `capabilities.json = {"schemaVersion": 1, "enabled": []}`
(ON RECORD, `docs/reviews/2026-09-26-slot-1-economics-postmortem.md` §6), so the factory has never
observed an install, a purchase, a refund or a conversion rate. Recipe 1 exercises `billing`, ends
with an honest install-to-sale number, and costs the least to be wrong about.

### 5.2 Refuse: Recipe 3, the hair colour formula vault

I would refuse it. The guard called the query `candidate` at 0.92 on-topic; hand-classified, nine of
twelve results are hair-*changer* photo editors and only two serve the formula job. Widening the
query moved both numbers in the wrong direction: at `hair color formula app` the guard rose to 0.93
while the hand-verified job share fell to **3/30 = 0.10**, and one of those three is titled
"Color Formula Vault", colliding with the recipe's own proposed name. The one rated incumbent
returned **zero critical reviews out of three read**, so I have no complaint to answer, and the
adjacent client-record shelf is a fortress of booking platforms at `1M+` and `10M+` installs.
Building it would be selecting a niche on a label match rather than on a job — which is the same
class of mistake as turning the owner's illustrative calculator into a strategy, one level down. Its
economics are the best of the five (`$7.99`, 154 sales for `$1,000`), and that is precisely why it
has to be refused on evidence rather than kept on arithmetic.

### 5.3 If the owner wants a second slot now, and it is not Recipe 1

Recipe 4 (driving hours) is the second choice and the only genuinely different *monetisation
experiment*: it is the only recipe that enables `ads`, which no slot has ever done, so it is the only
one that would measure whether the factory's ad capability works in production. Its downside is
explicit: an ads-led utility earns on the order of `$130–300/month` at 1,000 installs a month, with
the ad line larger than the unlock, and its strongest measured complaint is a crashing export, which
the recipe fixes.

I would **not** choose Recipe 5 as that second slot despite its perfect demand measurement, because
its demand is measured in a category where a free app with 42,087 reviews already wins the same
search, and its repeat-sales model is weakened by a repeat factor near two.

---

## 6. What the factory should change

Each proposal names the file it belongs in and the failure it prevents. I did not implement any of
them: `AGENTS.md`, `.cursor/rules/*`, `scripts/`, and `apps/` are owned by another writer.

### 6.1 The economics gate should accept the evidence that justifies the plan

- **Fix.** Widen `EVIDENCE_REF_PATTERN` in `scripts/lib/slot-economics.mjs` to accept
  `docs/ideation/[a-z0-9._-]+\.md` as well as `docs/research/…`, or promote measured niche evidence
  into a `docs/research/` file as a required step of the ideation pipeline and say so in
  `docs/ideation/README.md`.
- **Failure it prevents.** All five plans in this proposal point at
  `docs/research/2026-09-24-market-decision.md#1-what-was-measured` — the method record — because
  the pattern refuses `docs/ideation/` and I may not write into `docs/research/`. The per-niche
  evidence that actually justifies each price and each wedge lives in this document. So the plan
  that a slot carries into the pipeline points at a file that documents *how* the market was
  measured rather than *what* was measured for that niche. That is a weaker evidence link than the
  gate implies, and it is exactly the "claim that no artifact can contradict" class recorded in
  `harness/lessons.md` and in the slot-1 post-mortem §3.
- **Sensor.** `node scripts/check-economics.mjs --economics <a plan whose evidenceRef is
  docs/ideation/…>` should return PASS after the change and BLOCKED before it. Verify the check
  fails first.

### 6.2 The one-page proposal, and where it lives

- **Fix.** Specify in `docs/ideation/README.md` that a recipe proposal is a single file
  `docs/ideation/<date>-<topic>.md` containing, in this order: a one-sentence idea; the paying user,
  the unit, the price and the archetype; a demand table with every claim labelled; the install-to-
  revenue ladder with the division shown; the build estimate in agent hours with permissions and
  capabilities; the kill criteria as three numbers; and the honest unknowns. Add a check to
  `scripts/selftest.mjs` that a proposal file under `docs/ideation/` does not contain an unlabelled
  demand figure, by requiring every numeric claim near the words rating, installs, reviews or price
  to sit within 200 characters of one of the four labels MEASURED / ON RECORD / INFERRED /
  UNVERIFIED.
- **Failure it prevents.** The owner cannot decide quickly from a document that mixes measured
  numbers with intuitions. This document had to be written the long way because no such template
  existed, and a fresh session with no context would not know it was needed.

### 6.3 Measure the slot after it ships, or the factory learns nothing

- **Fix.** Create `docs/operations/slot-metrics.md` defining the post-ship capture, and store one
  machine-readable file per slot per month at `apps/<slot>/metrics/<YYYY-MM>.json` with: installs,
  uninstall events, active devices, `DAU`, `MAU`, crash-free rate, ANR rate, the purchase count and
  gross revenue per product id, refund count, and the ad revenue if ads are enabled. Source it from
  the Play Developer API reports (which can read an app's reports but cannot create an app,
  ON RECORD `docs/research/2026-09-23-google-play-automation-surface.md`) plus Play Console for the
  figures the API does not expose, and add a reader to `scripts/kiln-status.mjs` that prints actual
  revenue per slot beside the plan's `killCriteria` and flags a slot that has missed them.
- **Failure it prevents.** The factory has never measured a sale
  (`docs/reviews/2026-09-26-slot-1-economics-postmortem.md` §6), so every price in this document
  rests on an UNVERIFIED 2 % conversion. Without this capture, the next ideation document will carry
  the same unverified number, and the kill criteria will remain prose that nothing evaluates.

### 6.4 Make "about one hour" real, or stop claiming it

- **Fix.** Record in `AGENTS.md` (or a rules file if the owner prefers) the measured trajectory that
  the one-hour budget must be timed against: ON RECORD,
  `docs/reviews/2026-09-26-slot-1-economics-postmortem.md` §1 puts the first slot's scaffold-to-last-
  build span at 4 h 49 m as a **lower bound** that excludes evaluation, capture and repair. Name the
  cuts that would have to happen for one hour to hold: skip `media-capture.mjs` and compose store
  images from a single debug-build screenshot set; skip the tablet asset sets for a first release and
  ship the required minimum phone count; and run `--skip-build` for staging. Then have
  `scripts/verify-slot.mjs --build` print the elapsed wall clock and the count of stages run, so a
  regression in slot cost is visible in the output rather than timed by hand.
- **Failure it prevents.** Every recipe in this document estimates 3–6 agent hours, and none of them
  is a one-hour slot. The budget as written is unachievable, and an unachievable budget is worse than
  no budget because it turns every honest estimate into a failure.

### 6.5 A dead-zone and fortress check at the ideation boundary

- **Fix.** Add `scripts/check-ideation.mjs` reading `docs/ideation/plans/*.economics.json`, running
  `scan-market`/`scan-titles` against a `query` recorded in each plan, and blocking a plan whose
  query verdict is `dead-zone` or `fortress`, or whose hand-classified job share is below a stated
  threshold (0.35 is the value implied by `docs/niche-selection.md`'s dead-zone rule). To make that
  possible, add one optional field to the plans (`query`), validated by `parseEconomics`.
- **Failure it prevents.** Recipe 3 exists in this document only so it can be refused, and the query
  that would have selected it returns `candidate` at 0.92 — so a later session running the same
  query without a hand-check would pick it. A gate that refuses `dead-zone` and `fortress` verdicts
  and demands a job-share number would have caught both that and the `hair color`/`hair changer`
  homonym at the same moment.

### 6.6 Two smaller observations from reading the route

- **`kiln-status.mjs` reports `NOT READY`, and the failure is now reproducible across two independent
  runs, 143 s and 144 s.** Two self-test checks fail (`a run that skips the build reports UNVERIFIED`,
  `an UNVERIFIED verdict exits 2 (got 1)`) while every environment row and every harness/scaffold row
  passes. This is exactly the contract `AGENTS.md` states ("Without it the gate reports `UNVERIFIED`
  (exit 2), not PASS"), so either the self-test or the `UNVERIFIED` exit path is wrong. **The
  mechanism is readable from the source, and it is worth handing to the file's owner rather than
  re-diagnosing:** `scripts/selftest.mjs` lines 711–720 run
  `verify-slot.mjs --slot selftest --no-selftest` and assert that the output contains
  `VERDICT: UNVERIFIED` and that the exit status is 2. In `scripts/verify-slot.mjs` the verdict order
  is `failed → exit 1`, then `blocked → exit 1`, then `unverified → exit 2` (lines 1077–1088), and
  `failed`/`blocked` are set by *any* printed `FAIL` or `BLOCKED` line (lines 62–69). The observed
  run printed no `UNVERIFIED` line and exited 1, so a check inside the sandbox slot printed `FAIL` or
  `BLOCKED` before the verdict was decided. INFERRED, not confirmed: the most likely such check is
  the new economics pairing, because the sandbox slot is asserted to enable **no** capabilities
  (see the comment at `selftest.mjs` lines 722–724) while `requiredCapabilitiesFor` demands
  `billing` for any purchase-led archetype — a sandbox plan paired with an empty capability list
  would print exactly this. Two warnings for whoever owns the fix: this is the "a check that cannot
  fail is not a check" class from `.cursor/rules/verification-discipline.mdc` in reverse — here the
  check *is* firing, but on the sandbox's own fixture rather than on a real defect — and the second
  independent confirmation means it is no longer dismissible as an in-flight edit. Either way the
  failure belongs in `harness/lessons.md` with the fix, because a factory that cannot report its own
  readiness is a factory whose readiness claims need a manual read.
- **A new WARN appeared in the second run that the first filtered out of my view:**
  `java on PATH is JDK 24 but the factory builds with JDK 17; a build that skips the pin starts a
  second Gradle daemon and rebuilds cold`. This is the measured 2.4 s vs 70.1 s lever from
  `.cursor/rules/build-speed.mdc`, and it is a warning rather than a failure by design. It deserves
  one line in the readiness output that names the environment variable to fix, because the cost of
  ignoring it is a factor of ~29 on every build in the loop.
- **`scripts/market-queries.json` no longer contains 66 queries.** It contains 17, in five groups,
  and its `trade-calculators` group is marked CALIBRATION GROUP ONLY with an explicit steering
  warning. That is good: the warning prevents a fresh session from choosing a calculator out of a
  file whose rows look like candidates. The group should nevertheless be moved out of the same file
  as the live groups into `scripts/market-queries.calibration.json`, so that "a query appearing here
  is not a recommendation" cannot be skimmed past — and so `scan-market`'s candidate count stops
  including `bolt torque calculator`, which today inflates the candidate total with a burned
  category.

---

## 7. Everything I could not verify, and every assumption I am carrying

**Sensors and infrastructure.**

1. Network was available and all seventeen commands in §1.1 ran; no claim in this document is
   UNMEASURED because a sensor was blocked. **However, `kiln-status.mjs` returned `VERDICT: NOT
   READY`** on the factory self-test, while all environment rows passed. I did not investigate
   whether that is a standing defect or an in-flight edit by another writer.
2. I did not re-run `scripts/verify-reviews-sensor.mjs` against live Play pages, so I am assuming the
   review sensor's taxonomy still matches live phrasing. ON RECORD, `AGENTS.md` requires that
   validation after a taxonomy change; I made no taxonomy change.

**Economics assumptions (all UNVERIFIED as applied to these recipes).**

3. **Install-to-purchase conversion is assumed at 2 %** of installs for every recipe. This is the
   largest single unknown in the document and it is not measured anywhere in the repository.
4. The `F_billing = 0.15`, `F_refund = 0.02`, `F_fx = 0.02` values are ON RECORD as **assumptions**
   in `docs/monetization-strategy.md` §2.2, not as measurements of this account. The same table warns
   that above the first `$1M` annual tier, or for existing EEA/UK/US installs at standard rates, the
   fee rises to 20–30 % plus a 5 % billing fee, and that whether the fee is computed on a
   tax-inclusive price is UNVERIFIED.
5. **Excluding `F_fraud` from a local unlock** is my INFERENCE from §2.2's definition of the term. If
   that reasoning is wrong, every net-per-sale figure in this document is overstated by roughly 20 %.
6. Play's actual purchasable price points are UNVERIFIED (ON RECORD,
   `docs/monetization-strategy.md` §2.2: "the set of purchasable Play price points (research §8.8)").
   So whether `$4.99`, `$5.99`, `$7.99`, `$3.99` and `$2.99` are all sellable in a given country is
   assumed.
7. The `DAU ≈ 0.5 × monthly installs` steady-state approximation used in Recipe 4 is INFERRED and
   unmeasured, and it is the weakest number in this document; a factor of two either way moves
   Recipe 4's ad line by a factor of two. The same is true of the assumption that a packing buyer
   buys about one pack (Recipe 5) rather than several.
8. I did not measure `com.divinglog.divelog`'s price, so Recipe 2's `$5.99` over `$4.99` is weakly
   justified.
9. Whether this account can publish to production on day one, or must run the 12-tester/14-day closed
   test, remains UNVERIFIED. ON RECORD, `docs/reviews/2026-09-26-slot-1-economics-postmortem.md` §6
   and `docs/research/2026-09-24-market-decision.md` §6 both leave this open.

**Market facts I did not measure.**

10. Install buckets are as printed by the sensors, and a bucket is a bound (`10K+` means at least
    10,000), not a point estimate. Several apps in §3.2 appear with a rating from `scan-titles` and no
    install bucket, because they were outside the enriched top 8; those cells read "not measured" and
    I did not fill them in.
11. I read **three reviews per app** (`--top 6` returned three each). Three reviews is a sample, not a
    rate; no complaint percentage in this document should be read as a population share. In
    particular, `com.stayincharm` returning zero critical reviews out of three is consistent with a
    well-loved app *and* with a small sample.
12. I did not measure whether LoTW, QRZ or contest software import a given ADIF variant (Recipe 1),
    whether state licensing offices accept a printed self-reported summary (Recipe 4), how many divers
    log by hand versus importing from a computer (Recipe 2), or whether any stylist would pay for a
    formula record (Recipe 3). Each is named in its recipe's kill criteria as the cheap command that
    would settle it.
13. The `hair color formula` hand-classification is my reading of titles, not a machine verdict. I
    probed this shelf three times with different wording (`hair color formula`, `hair color formula
    app`, and the homonym-free `salon client record book`) and reported all three; the three probes
    agree that the formula job is a small minority of the shelf, but a differently-punned query
    (`colour formula book`, `tint bar app`, `salon formula`) might surface a shelf I did not see.
14. I did not measure `com.shearwater.dive`'s install bucket, so the claim that the poorly-rated
    hardware companions are widely installed rests on `com.pelagicnet.diverlogplus` alone, which I did
    measure at `50K+`.
15. The `dog walking client log` shelf is measured at title level only. I did not run
    `enrich-market` or `scan-complaints` against it, so I have no exact review count, no install
    bucket and no complaint text for it. The claim that it is the same job as the killed
    `dog groomer appointment book` is my reading of the two shelves, and it is INFERRED.
16. The name collision between Recipe 3's proposed title and `com.app.colorvault` is MEASURED at the
    store-title level. Whether Play's search or the listing review would treat that as a problem is
    UNVERIFIED — I did not test a submission, and I am not claiming an enforcement outcome, only the
    measured fact that the name is already taken by an app in the same category.
17. The exit-1 cause in `kiln-status.mjs` is INFERRED from reading `selftest.mjs` lines 711–724 and
    `verify-slot.mjs` lines 62–69 and 1077–1088. I did not instrument or re-run
    `verify-slot.mjs --slot selftest --no-selftest` to confirm which check prints `FAIL` or
    `BLOCKED`, so the economics-pairing explanation in §6.6 is a hypothesis with a named next
    command, not a diagnosis. Fixing it is another writer's file: `scripts/selftest.mjs` and
    `scripts/verify-slot.mjs` were both modified in the working tree during this session, so the
    failure may already be resolved by the time this is read. Confirm before acting on it.
18. The two runs recorded as #18 and #19 in §1.1 were executed as background shell tasks in this
    session and I read their captured output files rather than their live streams. Their exit codes
    and timings are as the capture reports them; I did not re-run either command myself.
