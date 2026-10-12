# DoseFold Clinic Log — proposal (2026-10-10)

One recipe. Primitive solo recon, no subagents, no factory scan scripts — the
owner asked for that shape. The measured shelf and the refused hype of the same
week live in `docs/research/2026-10-10-glp1-tracker-shelf.md`. This file is the
human decision document.

## Recommendation

**Build `dosefold-clinic-log` if the owner wants a late-2026 consumer app that
is not another astronomy pack and not a thin AI wrapper.**

The week of 2026-10-08 the Play charts are AI video, AI rooms, and glass
widgets. Those pay. They are refused below. The job that is *on the hype of
2026* and that this factory can ship without a client secret is the GLP-1
weekly routine: a drug class that hit primary care in 2024, that MyFitnessPal
bolted a module onto in **April 2026**, and that still puts a dedicated tracker
at **US Health & Fitness Grossing #56** in October 2026.

## The wedge, in one sentence

A person on a weekly GLP-1 shot keeps every dose, injection site, side-effect
and plate photo on the phone without an account, and hands the clinician a
dated PDF — without a subscription and without an AI that guesses the meal.

That is a difference in **functionality and content**, not a rename. Shotsy
loses the log when an account hiccups. MeAgain and Pep sell the same log as a
subscription and then guess the plate wrong.

## The arithmetic

| Line | Amount | Label |
|---|---|---|
| Price | **$5.99** one-time Play Billing unlock | proposed |
| Play fee | 15% (first-million bracket) | ON RECORD factory default; whether this account is still in that bracket is UNVERIFIED |
| Tax / refund haircut | 8% | assumption, same as recent ideation plans |
| Net per sale | $5.99 × 0.85 × 0.92 = **$4.68** | INFERRED |
| Target used for the decision | **$200 net / month** | owner information, not a gate |
| Sales to clear $200 | $200 ÷ $4.68 ≈ **43 unlocks / month** | INFERRED |
| Sales to clear $1,000 | ≈ **214 unlocks / month** | INFERRED; shown so the stretch is visible |

Day-1 path: the first shot, one side-effect tag and one plate photo are free.
The dated clinic PDF and the restorable backup file sit behind the unlock.
Google Play Billing is the collector.

Shotsy is already on a US **grossing** chart (AppInsider, 2026-10-10). That is
not our revenue. It is proof the shelf takes money. Conversion at $5.99 against
a free-ish leader is **UNVERIFIED**.

## Job, who pays, what is bought

- **Category:** Health & Fitness — GLP-1 shot companion, not a generic calorie
  counter and not a medical device.
- **Job:** Before the next injection and before the next clinic visit, keep a
  crash-safe record of what was injected, where, what it felt like, and what
  the person managed to eat, then leave with a file a clinician can read.
- **Paying user:** An adult already paying for a branded or compounded GLP-1
  (Wegovy, Zepbound, Mounjaro, Ozempic, or a compounded equivalent) who will
  not add a $10/month tracker bill on top of the drug.
- **Paid unit:** A permanent on-device unlock: unlimited history, the clinic
  PDF/CSV, and the restorable backup file.
- **Moment of payment:** After the first free shot + side-effect + plate photo,
  when the user taps "Make the clinic packet" or "Keep more than one month".
- **Archetype:** `purchase_led_single_unlock` (`billing`).

### Archetypes considered and rejected

| Archetype | Verdict | Revenue reason |
|---|---|---|
| `purchase_led_single_unlock` | **chosen** | One durable packet. The user is already paying hundreds per month for the drug (Lauren Freeman, MeAgain, 2026-08-30). A second monthly bill is the complaint. A $5.99 unlock is a single decision. |
| `purchase_led_repeat_sales` | rejected | There is no consumable to re-sell. A plate photo and a shot row do not cost us money to serve. Invented "pack of 10 PDFs" would be a fake store. |
| `ads_led_utility` | rejected | The same review says there is no money left after the medication. Injection-site and body photos next to a banner are ad-hostile and a trust defect. A rewarded ad does not buy a clinic PDF. |
| `purchase_only_no_ads` | rejected | A paid download throws away the free-first funnel every measured incumbent uses (Shotsy, MeAgain, Pep are free-to-install). Discovery dies. |
| `subscription` | banned | Not an option. It is also the exact shape the featured MeAgain review refuses. |

No server, no paid API, no account. Ongoing cost is near zero after the one
build. The price does not have to fund a generation.

## Incumbents as evidence, not as a warning

| Incumbent | What the card showed on 2026-10-10 | Why that helps |
|---|---|---|
| Shotsy (`com.shotsy.app`) | **4.7★ / 12.2K reviews**, Play **100K+**, US H&F **Grossing #56**, ~500 downloads/day (AppInsider) | A loved, paying leader. Occupied = demand. The featured 2026-09-17 review says the log vanishes after login. |
| MeAgain (`app.meagain.app`) | **4.6★ / 3.07K**, IAP, "subscription" on the listing; **50K+** (apppage.net) | Mid-tier that earns and is hated for the monthly bill. |
| Pep (`com.shredapps.glp1`) | **4.7★ / 1.96K**, IAP + Pep Pro subscription, AI food scan; Play **10K+**, AppBrain ~27K | Proves a newer entrant can still grow in 2026. Photos and the subscription are the complaints. |
| MyFitnessPal GLP-1 Support | Free module, April 2026, on a 100M+ fortress (Gila) | The fortress is moving *into* this job this year. We do not fight its food database. We do the clinic packet it will not care about. |

Hand job-share on the fetched `GLP-1 tracker` page: **14/15** titles on-job
(0.93). Detail and quotes: `docs/research/2026-10-10-glp1-tracker-shelf.md`.

The weak pocket is not a 3.1★ corpse. It is **subscription anger + data loss +
a missing file for the clinician**, sitting under a 4.7★ leader.

## Promise contract

At least two core, one connective. All start `deferred`. Machine form:
`docs/ideation/plans/dosefold-clinic-log.differentiators.json`.

| id | weight | stage / family | Promise |
|---|---|---|---|
| `import-the-paper-month` | core | before / `data_in` | The first week is not a blank app: the user photographs or picks the paper log or screenshot they already have, and those rows become the starting history. |
| `one-tap-shot-and-site` | core | during / `core_operation` | One tap logs the prescribed dose on a body-site map that remembers the last eight weeks, with side effects tagged in hours-since-shot — not a calorie diary wearing a syringe icon. |
| `plate-photo-not-guessed` | supporting | during / `data_in` | A plate is a photograph plus a caption and optional typed protein grams. No model guesses the meal. |
| `crash-safe-no-account` | core | during / `durability` | Killing the process, rotating the phone or never signing in cannot wipe the shot history. There is no login that can say "add your first shot" after month three. |
| `clinic-pdf-handoff` | core | after / `data_out` | A dated PDF (and CSV) of doses, sites, side effects, weights and plate thumbnails leaves through the system share sheet or SAF. |
| `shot-day-widget` | supporting | before / `widget` | A home-screen tile shows next shot day and the last site, so the routine is visible without opening a subscription app. |

## Name

Derived field, not an invented category noun. Machine form:
`docs/ideation/plans/dosefold-clinic-log.naming.json`.

**Chosen: `DoseFold Clinic Log`** (19 characters, `carrier_job_noun`, score 87).
Russian listing title: `DoseFold журнал доз` (19 characters). Slug
`dosefold-clinic-log`. `applicationId` `com.gexetr.dosefoldcliniclog`.

Rejected on the record: **GLP-1 Tracker** (bare category; the query Shotsy
already owns) and **VialDay Packet** (memorable, weaker search, understates the
clinic file).

## Device capabilities

Each capability is answered `used` or `not used`; a `used` line names the API and what the job
loses without it.

- camera: used — `ActivityResultContracts.TakePicture`, `android.permission.CAMERA`; without it a paper month and a plate that exists only in the room cannot enter the packet.
- photo picker / gallery: used — `ActivityResultContracts.PickVisualMedia`, which asks for no runtime permission; without it a screenshot of last month's paper log or a plate already in the gallery has to be retyped.
- media playback or capture: not used — the job stores stills, not video or audio.
- microphone: not used — nothing in the weekly routine is a voice note the job depends on.
- location: not used — a shot is not a place.
- motion sensors: not used — the body-site map is tapped, not inferred from the IMU.
- contacts / calendar: not used — shot day is a notification and a widget, not a calendar write.
- notifications / reminder: used — `NotificationChannel`, `android.permission.POST_NOTIFICATIONS`; without it shot day is forgotten between weekly visits.
- home-screen widget: used — `GlanceAppWidget`; without it next shot day and last site stay buried in an app the user is already paying a subscription not to open.
- share / export: used — `ACTION_SEND`, `FileProvider`; without it the clinic packet cannot leave the phone.
- Bluetooth / USB: not used — no pen or scale is paired in this recipe.
- files / SAF: used — `ACTION_CREATE_DOCUMENT`; without it the restorable backup file cannot be saved where the user keeps their own records.

## Permissions

- `android.permission.CAMERA` — only for the in-app capture of a paper log or a plate. The gallery path uses the Photo Picker and does not need it.
- `android.permission.POST_NOTIFICATIONS` — shot-day reminder. Revocable; the log still works without it.
- No `ACCESS_FINE_LOCATION`, no contacts, no microphone, no Health Connect in v1 (a later Health Connect read would add Data-safety rows we do not need for the clinic packet).

## Capabilities the slot must enable

- `billing` — required by `purchase_led_single_unlock`.
- No `ads`.
- No server capability. No paid-API client.

## Visual direction

A folded clinic card, not a syringe and not a pharmacy-green Material demo.
Palette: warm clay / dried-blood terracotta on paper-ivory, with ink-black
type and one sharp teal only on "packet ready". The mark is a folded dose
card with a single notch — readable at 48 dp, no needle, no capsule, no
stock Remix glyph as the brand. Slot-owned components: the body-site map,
the hours-since-shot side-effect strip, and the packet preview that looks
like a printed visit sheet, not a `Card` of numbers.

## Build estimate

About **90 agent-hours** of phase-1 work (domain log, site map, camera/picker,
PDF, widget, billing, tests, on-device journey). This is owner information.
It is not a reason to take or refuse the idea.

## Kill criteria

`windowDays` 45, `minInstalls` 300, `minRevenueUsd` 80. If 45 days after
publish we have fewer than 300 installs or less than $80 recorded Play
revenue, the slot is a miss and is archived with those numbers. The $200
decision target is higher than the kill floor on purpose: a kill floor that
equals the dream number never fires.

## What this recipe refuses, in revenue terms

- **Astronomy / night-sky / stars.** The owner closed that attempt on
  2026-10-10 (`lumintra-light-planner`). Not re-proposed.
- **RoomGPT / Hailuo / Kling clones.** Charts are hot this week. Serving cost
  plus a banned subscription plus a client secret is a losing division, not a
  taste refusal.
- **Liquid Glass widget packs.** #1 paid personalization this week. No durable
  job; Play spam shape.
- **Another calorie / macro calculator.** Burned category. The plate is a
  photograph, not an arithmetic toy.
- **A Shotsy clone with a new palette.** A rename is the repetitive-content
  pattern. The clinic PDF, the no-account durability, and the refusal to guess
  the meal are the wedge; without those three this recipe is not distinct.
- **Migraine / baby / asthma / pet-med logs.** Already refused as fortress or
  thin on 2026-10-05. Different jobs.

## Honest unknowns

- Whether 43 people a month will pay $5.99 when Shotsy's core log is free-ish.
- Whether Play's Health declaration for a GLP-1 *log* (no advice, no levels
  chart, no dose recommendation) stays inside Health & Fitness without extra
  medical-app friction.
- Whether "DoseFold" collides with a live Play title or a class-9 mark.
- Whether featured reviews overstate data-loss frequency.
- Whether a fortress MFP module, six months after launch, already satisfies
  the clinic-visit user. Unverified; MFP's job is food, not a visit packet.

## Scaffold command, only after the owner says yes

```bash
node scripts/new-slot.mjs --spec app-dosefold-clinic-log \
  --economics docs/ideation/plans/dosefold-clinic-log.economics.json \
  --differentiators docs/ideation/plans/dosefold-clinic-log.differentiators.json \
  --naming docs/ideation/plans/dosefold-clinic-log.naming.json
```

`--slug` and `--name` come from the naming contract. Do not run this until
checkpoint 1 is the owner's own words. The OpenSpec change
`app-dosefold-clinic-log` is authored *after* that yes, not before.
