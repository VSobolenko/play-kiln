# L — Depth, retention and the edge-case register

Product: **Lumintra** (`lumintra-light-planner`, `com.gexetr.lumintralightplanner`), a $9.99 one-time
`purchase_only_no_ads` Android light-and-sky session planner. Kotlin + Compose + Material 3,
minSdk 26 / targetSdk 36 / compileSdk 37, Orekit 13.1.9 + Hipparchus 4.0.3 + SGP4 + the Astronomy
Engine Kotlin/JVM port.

Run 2026-10-08. Worker: spec/depth worker. Deliverable: this file only.

**Two questions, answered:** (1) which features give this app a *finished* job rather than a raw
middle, and which of them are not worth building; (2) the edge-case register — the situations that
*will* become bugs if the specification does not pin them, each with the exact behaviour required.

---

## 0. Method, and what I did not do

Read in this session, in the repository:

- `AGENTS.md` (the toolchain pins, the two-phase gate, the Play compliance floor, the working rules).
- `.cursor/rules/app-depth.mdc` — the three job stages, the closed `depthFamily` set, and the
  connective floor (read the family table and its token column directly at `:77`–`:87`).
- `.cursor/rules/grounded-claims.mdc`, `.cursor/rules/ideation-gate.mdc`,
  `.cursor/rules/verification-discipline.mdc`.
- `docs/ideation/plans/lumintra-light-planner.differentiators.json` — the seven existing claims.
- `docs/research/2026-10-02-user-review-depth-signals.md` — the measured complaint taxonomy and the
  ranked extras (this is the load-bearing evidence for every depth verdict below).
- `.kiln/scratch/positioning-2026-10-08/A-astrophotography-baseline.md`,
  `D-complaints-satellite.md`, `F-engine-gap-map.md`.
- `.kiln/scratch/spec-2026-10-08/metno-tos.html` (already on disk), and I **fetched**
  `https://celestrak.org/usage-policy.php` myself this session (HTTP 200, 13,980 bytes, saved to
  `.kiln/scratch/spec-2026-10-08/celestrak-usage-php.html`) because the recon's `HTTP 500` reading was
  taken from the wrong path (`/usage-policy/`, which returns a 404 page — measured, it is on disk as
  `celestrak-usage.html`).

Not done, deliberately: no Gradle run; no `apps/**` directory opened; nothing written outside
`.kiln/scratch/spec-2026-10-08/`; no sibling slot treated as a standard.

### 0.1 Two recon `UNVERIFIED` rows are now settled, and one is corrected

Both were flagged `UNVERIFIED` in `F-engine-gap-map.md` §4.4 and §5.3. I read both policies.

| Recon row | Status now | What the source actually says |
|---|---|---|
| "CelesTrak's terms of use permit a commercial app to poll the GP endpoint" — `UNVERIFIED`, page returned HTTP 500 | **Closes to a different question** | The page is reachable at `/usage-policy.php` and is a **usage policy**, not a licence. It contains **no licence grant, no redistribution clause and no commercial-use clause at all** — MEASURED: read the full 13,980-byte body; the only licence-shaped text is a link to GP documentation. So "permits commercial use" is not asserted by the source; the operational rules *are* stated and are binding on the client's behaviour (§0.2). |
| "MET Norway's terms permit a commercial app, and require an identifying User-Agent" — `UNVERIFIED` | **Settled** | The ToS states the attribution obligation (CC BY 4.0 verbatim), the User-Agent requirement, a hard coordinate-precision cap, a no-background-retrieval rule and a 20 req/s ceiling — quoted verbatim in §0.3. It states **no commercial-use restriction**; the CC BY 4.0 obligation is attribution, and CC BY 4.0 permits commercial use. |

**A new finding neither the recon nor the promise contract contains, and it changes the satellite
design** — MEASURED on the CelesTrak page:

> "URGENT: We ran out of 5-digit catalog numbers on **2026-07-11**. The official USSF SATCAT is now at
> 100954. All newly cataloged objects have **6-digit catalog numbers of 100000+ and GP data will not
> be available for them using the TLE format**. Please be sure to use one of CelesTrak's other formats
> which remove the 5-digit catalog number limitation (and finally fix the Y2K problem)…"

> "…the legacy formats of the SATCAT, GP, and SupGP data will change… Users need to update their
> software to use the **CSV formats**, which have been available for 6 years now. The CSV format has
> many advantages, including ease of use in a wide variety of applications, and is even **smaller
> (~10% smaller) than the TLE format** and far easier to parse."

The curated objects this app wants (ISS 25544, Tiangong 48274, Hubble 20580) are legacy 5-digit
catalog numbers and *do* still have TLE text, so a naive `TLE`-only parser would work today and break
the first time the curated list gains a newly-catalogued object. The specification must say **OMM/CSV
or JSON, not fixed-width TLE** (§2.4, E32–E34).

### 0.2 CelesTrak usage policy — the operator rules, verbatim (MEASURED)

Fetched `https://celestrak.org/usage-policy.php`, HTTP 200, 13,980 bytes this session:

| Rule | Verbatim |
|---|---|
| Cadence | "Only download the data you need, when you are going to use it, and **only download data once per update**. … For GP data, updates are **once every 2 hours**." |
| Fail-closed on any error | "we send HTTP error responses when users are exceeding limits or using incorrect (long-outdated) URLs (e.g., **HTTP 301, 403, 404, 50x**). **M2M (machine-to-machine) software should immediately stop querying when it receives any non-HTTP 200 responses** and report the results to a human for investigation. **Repeatedly ignoring them will end up sending your IP address to the firewall.**" |
| Format | CSV is "~10% smaller than the TLE format and far easier to parse"; TLE cannot carry 100000+ catalog numbers. |
| Directory queries | "Please don't query these more than once per hour." |
| Privacy | "We do not use cookies on CelesTrak and we do not collect any personal information, other than IP addresses, which are used to detect and block malicious activity… not used for tracking." |

The last row matters for Data safety: **the app sends no user data to CelesTrak** — a GP query carries
no coordinates. Weather is the *only* outbound user-data flow in this product (§0.3, E45).

### 0.3 api.met.no terms of service — the operator rules, verbatim (MEASURED)

Read on disk at `.kiln/scratch/spec-2026-10-08/metno-tos.html` (`https://api.met.no/doc/TermsOfService`,
2020-06-26, Geir Aalberg). The rows below are quoted; every one of them becomes a spec line.

| Rule | Verbatim | Spec consequence |
|---|---|---|
| Identification | "Valid User-Agents … e.g. `acmeweathersite.com support@acmeweathersite.com` … **fake or random strings as identification in the User-Agent header** … **will result in a permanent ban**." | A fixed, honest UA naming the app and a contact address; never per-install random. |
| Coordinate precision | "truncate all coordinates to **max 4 decimals**. … **For new products, requests with 5+ decimals will return a 403 Forbidden**." | Truncate lat/lon to 4 decimals **before** building the URL. 4 decimals of latitude ≈ 11 m. |
| Background retrieval | "**Applications on mobile devices must not retrieve new data as long as the application is not in use.** If you need push notification (e.g. for MetAlerts), don't make more than one poll every 10 mins." | Kills the background "good night alert" as designed (§1.3.B). |
| Caching | "don't repeat requests until the time indicated in the `Expires` response header"; "Cache data locally and use the `If-Modified-Since` request header"; "Don't use HEAD followed by GET". | Cache + conditional requests; the cache is the offline path. |
| Rate ceiling | "Anything over **20 requests/second per application** (total, not per client) requires special agreement." Throttling yields **HTTP 429**. | One request per spot per TTL, single attempt, no retry loop. |
| Attribution | "All open data require attribution as specified in the **CC BY 4.0** license: *You must give appropriate credit, provide a link to the license, and indicate if changes were made.*" | The credit line is a hard requirement, in-app and in the pack. |
| Transport | "We only support encrypted **HTTPS**"; HTTP "is not allowed over extended periods of time". | HTTPS only; an `http://` endpoint is refused by the client. |
| Personal data | "All api.met.no access logs are stored in our own data center in Oslo, Norway. Company email addresses (used in User-Agent headers) are not considered personal information for GDPR…" | The UA email is not a GDPR issue; the **coordinates are** a Play Data-safety item (§1.4, E53). |

**Measured detail worth keeping:** the endpoint answered **byte-identically with and without a
User-Agent** (`metno-compact-UA.json` and `metno-compact-noUA.json` are both 38,302 bytes,
`Compare-Object` difference count = 0). The endpoint's tolerance is not permission — the ToS requires
the header and a breach is a permanent ban. Say this in the spec so nobody "optimises" the header away.

---

# Part 1 — Depth and retention

## 1.0 The revenue logic of a purchase-only app — why retention is worth anything here

This has to be stated before any verdict, because the naive retention argument does not apply.

With `purchase_only_no_ads` at one price there is **no recurring revenue and no ad inventory**, so
retention does not pay directly. It pays through exactly two channels, and only these:

1. **Rating and review volume → store-rank discovery → more installs.** A user who is delighted on a
   clear, moonless night is a user who rates, and the ask lands best at that moment. A user who
   uninstalls in week one never rates and sometimes rates badly on the way out.
2. **Refund avoidance and complaint avoidance.** "My data feels trapped" and "it did nothing useful"
   are the two measured complaints that convert a sale into a refund plus a 1★.

`INFERRED` throughout this subsection — it is product reasoning over the measured complaint data, not
a measurement. The consequence is concrete and it shapes three verdicts below:

- A retention surface (widget, reminder, no-configuration default) is worth building **when it puts a
  good moment in front of the user**, and not merely because "retention is good".
- The rating prompt must fire **after delight, not at launch** (E88).
- Depth is spent on the **job's own handoffs** — export/import, offline, the brief — not on screens.

## 1.1 The feature table

`depthFamily` values are the closed set of `.cursor/rules/app-depth.mdc:77`–`:87`. **Connective** = the
family counts toward the handoff floor. Every `ALREADY SPECIFIED` row exists in
`lumintra-light-planner.differentiators.json` today and needs no new claim; every `NEW CLAIM` row would
need one, which is the **owner's decision at checkpoint 1**, not mine.

| # | Feature | depthFamily | Permission (and what the job loses without it) | Proof artifact token | Revenue reason | Verdict |
|---|---|---|---|---|---|---|
| A1 | **Export the whole plan store** (spots, horizon profiles, equipment, favourites) to a file the user owns | `data_out` (connective) | none — SAF `ACTION_CREATE_DOCUMENT` asks for no runtime permission | `ACTION_CREATE_DOCUMENT` + `FileProvider` | Export/backup is the **#1 ranked extra in both independent datasets** (661 and 743 mentions) and the **paid product in four commercial apps**. A photographer's drawn horizon is the one irreplaceable artifact in the app. | **ADOPT** — new claim `carry-your-scout` |
| A2 | **Import a scout file back in** (own format; plus pasted/typed coordinates) | `data_in` (connective) | none — SAF `ACTION_OPEN_DOCUMENT` | `ACTION_OPEN_DOCUMENT` (+ `ClipboardManager` for pasted coordinates) | The contract's own `pin-the-session-scout` promises the plan "survives a reinstall or a move to another phone"; **no claim is `data_in` today, so that promise currently has no mechanism**. Also kills the measured "29 plans could not be transferred to a new tablet" complaint class that the incumbent already carries. | **ADOPT** — new claim `carry-your-scout` |
| A3 | **Android Auto Backup rules** for the plan store | `durability` (connective) | none | `dataExtractionRules` + `fullBackupContent` | Zero-UI, zero-permission insurance on the same artifact; **also a Play compliance floor item** (`AGENTS.md`: backup rules separate cloud backup from device transfer). | **ADOPT** — strengthens `pin-the-session-scout` |
| A4 | **Home-screen widget**: "Tonight at \<spot\>: dark 23:04–02:51 · Moon 34% · cloud 20%" | `widget` (connective) | none | `GlanceAppWidget` / `AppWidgetProvider` | A planning app is opened a few nights a month; the widget is the only surface that puts the product in front of the user *between* sessions. Measured: a widget is a named reason an app survives on the home screen ("Literally the longest I have kept a widget on my Main phone screen"). | **ADOPT** — new claim `glance-at-tonight` |
| A5 | **"Remind me tonight"** — one local notification at the start of tonight's dark window | `reminder` (connective) | `POST_NOTIFICATIONS` (runtime, API 33+; opt-in, default off). Without it: no reminder — every other feature is unaffected | `NotificationChannel` + `NotificationManagerCompat` | Reminders are the **#4 ranked extra** (364 requests) and **6.12 % of all feature requests** in Khalid 2014. | **ADOPT**, narrow — new claim `get-myself-outside` |
| A6 | **Background "a good night is coming" alert** (polls weather, pushes when clear + moonless + target up) | `reminder` | `POST_NOTIFICATIONS` + background work | `WorkManager` | — | **REJECT** — §1.3.B, three independent blockers |
| A7 | **"What's up tonight" with no configuration** — the brief opens pre-filled to tonight at the most recent spot | (behaviour inside `compose-the-light-and-dark-brief`; no new claim) | whatever the spot needs | — (the existing claim's own proof) | This is the whole second-launch experience. A buyer who opens the app twice in three months because it always asked them to configure first is the "raw" failure the owner named. | **ADOPT as a required default** |
| A8 | **Offline as a first-class state** | property of every claim (see §1.4) | none | — | "No offline access" is a top-5 ranked complaint (587 mentions). It costs almost nothing here: dark window, targets, scene, horizon, brief and export are already local. | **ADOPT as a required state** |
| A9 | **Favourites** — star a target, filter the list by favourites | `find` (connective — folds into the existing `search-the-deep-sky-catalogue`) | none | `SearchBar`, `searchQuery` (already the claim's token) | A photographer returns to the same five targets. Search is already promised; a favourite is the cheapest way to make a repeat visit one tap. | **ADOPT** — inside the existing claim |
| A10 | **The pasted-coordinates / GPX-ish paste path** (bring a point in from a message) | `data_in` (same claim as A2) | none | `ClipboardManager` | The honest substitute for "import from a competitor", which has no measured target format (§1.3.E). | **ADOPT** | 
| A11 | **The rate prompt, fired after a completed session** | `trust` (report-only) | none | — | The only rating ask that compounds discovery is the one at the moment of delight. | **ADOPT as a rule** (E88) |
| B1 | **Cloud sync of spots across devices** | — | would need an account and a server | — | — | **REJECT** — needs a server and an account, both excluded by the money plan; and the measured counter-anatomy is a sync extra that overwrote real saves and dropped a top-3 app to #13. |
| B2 | **Statistics dashboard** (nights planned, targets shot, charts) | `insight` | none | — | — | **REJECT** — `insight` may not satisfy the floor and this job has no analytics stage. The depth rule's own counter-evidence is a 6 % "complexity and learning curve" complaint class from feature creep. |
| B3 | **Bulk operations** (multi-select spots/targets, bulk edit) | `bulk` | none | — | — | **REJECT** — there is no bulk act in this job: you plan one night at one place for one target at a time. Building it to have a `bulk` claim is padding, which the depth rule bans by name. |
| B4 | **Import from a competitor's file** | `data_in` | none | — | — | **REJECT for now** — no competitor's export format was measured, so the interface would be invented. The measured 287-mention demand is real; the honest first step is A10. Settle with the check in §3.1. |
| B5 | **Light pollution / Bortle / SQM** | `insight` | none | — | — | **REJECT** — already deliberately excluded from the contract; needs a new bundled raster with its own licence question, and a 500K-install free incumbent already serves the number on its own. |
| B6 | **Aurora forecast** | — | would need a new data source and a licence | — | — | **REJECT** — out of the fixed product definition. |
| B7 | **Live AR sky view / camera overlay** | — | `CAMERA` + sensors | — | — | **REJECT** — that is a different app (a star map), it competes with 50M-install free incumbents, and it asks for the camera for a job this app does not have. |
| B8 | **A per-session history log** (which nights you actually planned) | `durability` | none | — | — | **REJECT for now** — no measured demand; the shareable brief already is the record. Named here as the first candidate if a later reading shows demand. |
| B9 | **Ads / subscription / account login** | — | — | — | — | **REJECT** — excluded by the money plan and the owner. |

## 1.2 The two permissions the app must *not* take

For the ledger, because "a runtime permission that buys nothing is a compliance and trust defect"
(`ideation-gate.mdc`), and because the adjacent shelf carries a *measured 1★ review for exactly this*:

> "It says that you need to enable location services to use the app which is fine and even though I
> did it still didn't work. **It wouldn't let me use the app until I also allowed alarms and
> notifications.** So it already doesn't let you prevent it from being intrusive…" — 1★,
> Satellite Tracker by Star Walk, `.kiln/scratch/positioning-2026-10-08/D-complaints-satellite.md:44`

| Never request | Why |
|---|---|
| `ACCESS_BACKGROUND_LOCATION` | No feature needs it. The reminder fires at a locally computed instant; nothing observes position in the background. |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | `AGENTS.md` treats exact alarms as restricted. A reminder for a dark window is precise to the minute at best (the window itself moves with the weather), so an **inexact** alarm is correct and loses nothing measurable. |
| `CAMERA`, `READ_MEDIA_IMAGES`, `ACTIVITY_RECOGNITION` | No feature uses them (B7 rejected). |
| `POST_NOTIFICATIONS` **as a gate** | The permission exists but must never block anything. Denial disables the reminder only. |

## 1.3 The named verdicts in detail

### A. The home-screen widget — **ADOPT** (`widget`)

The argument is not "widgets are popular". It is that a session planner is used *episodically*: opened
when the sky is clear, then not opened for a week. Between those sessions the app has no presence, and
an app with no presence is uninstalled when the phone runs out of space. The measured evidence for the
widget as a survival surface:

> "Literally the longest I have kept a widget on my Main phone screen … I keep a running list of bills
> on it." — Notes Widget Reminder, `docs/research/2026-10-02-user-review-depth-signals.md:149`

Pinned shape (the exact behaviour, E24-adjacent):

- One widget size that matters (4×2) plus a 2×2 variant; both show **one spot** — the primary one —
  and a configuration activity lets the user pick which spot a given widget shows.
- Content: `Tonight at <spot>` / the dark-window bounds / the Moon's illuminated fraction / the cloud
  cover **as last cached**, with its retrieval time. If there is no forecast: `no forecast` — never a
  blank and never a fabricated value.
- **The widget never performs a network request.** Its data is the app's cached state. A widget update
  is triggered by the platform's `updatePeriodMillis`, which is the one mechanism that needs neither
  a permission nor a boot receiver nor a periodic worker.
- Refresh cadence: `updatePeriodMillis = 3_600_000` (1 h — comfortably above the platform's 30-minute
  floor), plus an explicit `AppWidgetManager.updateAppWidget` whenever the app's own data changes.
- In the polar-day case the widget says `no astronomical darkness tonight at <spot>` (E02), and offline
  it says `no forecast` rather than showing a stale number as if it were current.
- Times are shown in the **spot's** zone with the zone abbreviation, because the dark window is a
  property of the place, not of the phone.

### B. The reminder, and the "good night alert" — **ADOPT one, REJECT the other**

**Adopt:** a single, opt-in, local reminder at the start of tonight's dark window.

- Created **only** by an explicit tap ("Remind me tonight") on the brief screen. Default state: off.
  Never requested at launch, never part of onboarding, never required (the 1★ review above).
- Scheduled with an **inexact** alarm; cancelled when the spot or the plan changes; **re-derived on
  every app open** so it can never fire for a plan the user has since changed.
- Contains: spot name, the dark window, the Moon's state, the leading target, and the cloud cover as of
  the **last cached retrieval, with its timestamp**, labelled forecast.
- **No network at fire time.** If the plan was computed offline, the notification says so.
- **Not offered at all when there is no dark window tonight** (polar day, E02) — the app must not
  schedule a "dark window" reminder for a night that has none.
- Does not claim the `reminder` floor by itself if A1/A2/A4 are built; it is a `supporting` claim.

**Reject:** the background "a good night is coming" push alert. Three independent blockers, any one of
which is sufficient:

1. **The weather source forbids it** — MEASURED, met.no ToS: "Applications on mobile devices must not
   retrieve new data as long as the application is not in use."
2. **Its cheapest implementation is a battery and maintenance tax** — a periodic worker polling every
   15+ minutes, on an app whose core value does not change, and a class of feature the depth rule cuts
   at ~5 % engagement.
3. **Its trust cost is higher than its value** — an alert that says "clear tonight" from a forecast
   fetched hours ago, for a user who then drives somewhere, is exactly the "it said clear and it rained"
   review. The forecast is already labelled a forecast; a *push* is a promise.

The honest substitute, if the owner wants the delight surface: the **widget** (A4) recomputes itself
from cached data and shows the same judgement — without a permission, without a server, without a
background fetch, and without the promise.

### C. "What's up tonight" — **ADOPT as a required default, not a feature**

Requirement: when a spot exists, the Session brief opens **already resolved** for tonight at the most
recently used spot, with no configuration step, and the scene is drawn for the current instant. The
user's first action is to scrub, not to answer questions. This is a property of
`compose-the-light-and-dark-brief`, not a new claim, and its test is trivial and worth writing: cold
start with one saved spot → the brief shows a computed dark window within one frame budget, with zero
dialogs.

`INFERRED` — this is a product decision about the second launch, not a measured demand signal. Its
evidence is the depth corpus's "a way back in" ranking plus the owner's "raw" complaint; it is not a
measurement of Lumintra users.

### D. Export / import / backup — **ADOPT** (the strongest verdict in this report)

Three separate mechanisms, deliberately layered so no single failure loses the user's drawn horizons:

| Layer | Mechanism | Family | Fails when |
|---|---|---|---|
| Automatic | Android Auto Backup (`dataExtractionRules` + `fullBackupContent`) | `durability` | the user disables backup, or is on a device without a Google account |
| Explicit, own-device | Export to a file the user picks, import from that file | `data_out` + `data_in` | the user never does it |
| Casual | `ACTION_SEND` of the JSON as an attachment | `data_out` | — |

The pinned format (this is the part that becomes a bug if it is left to the implementer):

- A single JSON document, `lumintra-scout-<yyyyMMdd>.json`, `schemaVersion` first, containing spots
  (name, latitude, longitude, IANA zone id, the horizon profile array, the equipment profile) and the
  favourite target ids. **No session data, no caches, no element sets** — caches are regenerable and
  must not bloat or date the file.
- **Targets and spots are referenced by stable string ids, never by list index** (M31, NGC224). An
  index is the classic way a "removed from the list" record silently points at a different object.
- Import is **all-or-nothing**: a file that fails validation imports nothing and reports the failing
  field and line. A partial import is how half-corrupt state is created.
- Import **never silently overwrites**: the user chooses `merge` (skip records whose name *and*
  coordinates match exactly) or `replace all`, and the count of each is shown before the commit.
- Hard caps validated **before** allocation: max file size, max spots, max samples per horizon array.
  A crafted or truncated file must produce a typed error, not an OOM (E106).

`ACTION_SEND` of the `.json` needs a `FileProvider` and a `content://` URI for a file written into the
app's own cache — the exact chain `app-depth.mdc:47` records `lawn-round-log` as *missing*.

### E. Import from a competitor — **REJECT for now**, for an honest reason

The measured demand is real (287 mentions, "explicitly the thing that stops a switch",
`2026-10-02-user-review-depth-signals.md:109`). What is missing is the **interface**: no competitor's
export format was measured in this session, and inventing a parser for a format nobody confirmed is
how a feature ships that imports nothing. The buildable substitute is A10 — paste coordinates, which is
the way a photographer actually receives a spot (a message, a shared link, a note). Settle the
possibility with the check in §3.1 and revisit after ship, not before.

## 1.4 Offline as a state, not a feature

Requirement, and it is a *state machine* rather than a screen:

| Capability | Offline | Online-only? |
|---|---|---|
| Dark window, twilight bands, sun and moon position | full | no |
| Deep-sky targets, altitude windows, framing | full | no |
| The scene and the drawn horizon | full | no |
| The session brief | full | no |
| Share the brief, export and import the store | full | no |
| Cloud cover | **unavailable, and says so**, with the last cached reading and its age if there is one | yes |
| Satellite passes | **unavailable, and says so**, with the element-set age if there is a cache | yes |

Two rules make that table true rather than aspirational:

1. **First run with no network is a supported state.** The bundled catalogue and the ephemeris are on
   disk; the app must reach a complete, useful brief with `INTERNET` never having been used, and the
   two network ribbons must render a typed "no data" state. No spinners that never resolve, no blocking
   dialogs, no crash.
2. **The app must not default to (0, 0).** First launch with no location and no saved spot shows a
   scoped-empty brief with a single call to action ("set a spot"), never a sky computed for the Gulf of
   Guinea (E110).

The Data-safety consequence of the weather half, stated plainly so the spec cannot drift: fetching a
forecast **transmits the spot's coordinates off the device** (truncated to 4 decimals, ≈ 11 m). Under
Play's definition — `AGENTS.md`: *"'Collect' in Data safety means transmitting data off the device"* —
that is a **Location (approximate)** declaration, and MET Norway must be named as a recipient in the
privacy policy. Nothing else leaves the device: the CelesTrak query carries no coordinates (§0.2), and
the share/export are user-initiated.

## 1.5 What this would mean for `differentiators.json` (the owner's decision, not mine)

Current contract: 7 claims, connective floor already met by `share-the-session-brief` (`data_out`,
`after`, `core`) and `pin-the-session-scout` (`durability`, `before`, `core`). Nothing here is
required to satisfy the gate. What the adopted features add is the *finished job*, and three of them
have no family in the contract at all:

| Proposed claim | Family | Stage | Weight | Why it is not redundant |
|---|---|---|---|---|
| `carry-your-scout` | `data_in` | `after` (export at the end of a trip; the import half serves `before`) | `core` | The app has **no `data_in` claim**, so "survives a move to another phone" is currently an assertion with no mechanism. |
| `glance-at-tonight` | `widget` | `during` | `supporting` | The only surface that exists between sessions. |
| `get-myself-outside` | `reminder` | `during` | `supporting` | The only surface that reaches the user at the moment the plan becomes actionable. |

`jobCoverage` would be unchanged (`before` → `pin-the-session-scout`, `during` →
`compose-the-light-and-dark-brief`, `after` → `share-the-session-brief`); the new claims strengthen
stages that already resolve. Adding claims **raises the ceiling** — every new promise must reach
`shipped` with a proof before `verify.mjs` will pass — so this is a scope decision the owner makes at
checkpoint 1, and it has the effect the contract rule predicts: it lapses any recorded approval.

---

# Part 2 — The edge-case register

## 2.0 How to read it

111 situations, each with four things: the **exact required behaviour**, **what the user sees**, and
**what is refused**. "Refused" means the app declines to produce a number or a screen, and says why —
the alternative (a zero, a blank, a plausible default) is what becomes a bug report.

One convention runs through the whole register and is worth stating once: **no event search may be a
single call over a long window.** The recon reproduced the trap for satellite passes — one
`propagate()` call over 3 days finds **1** AOS where a 60-second brute-force scan finds **15**, and
tightening `maxCheck` from 600 s to 10 s changes nothing (`F-engine-gap-map.md` §4.2, ON RECORD). The
same class of failure applies to **every** event search in this app: twilight crossings, moonrise,
target rise/set, and any window longer than a few hours. So: **every event search is segmented (or
stepped and refined) and the result is a list of crossings, verified against a brute-force scan at the
same step in a test.** E01, E43 and E95 are the same requirement seen from three directions.

The spherical-astronomy facts used repeatedly below, stated once so each row can be checked
(`INFERRED` — standard geometry, derived here rather than quoted):

- A body at declination δ, seen from latitude φ, has altitude at **lower culmination** (local
  midnight) of `alt_min = φ + δ − 90`.
- It is **circumpolar** when `δ > 90 − φ`.
- It **never rises** when `δ < −(90 − φ)`.
- The Sun never reaches altitude `h` at this latitude on this date when `δ > (h + 90) − φ`.

At Reine, φ = 67.93°: `alt_min = δ − 22.07`; circumpolar when δ > +22.07°; never-rises when
δ < −22.07°; no astronomical darkness (−18°) when δ > **+4.07°**; no nautical (−12°) when δ > +10.07°;
no civil (−6°) when δ > +16.07°. At the June solstice (δ = +23.44°) `alt_min = +1.37°`, i.e. **the Sun
does not set at all**.

### 2.1 Polar and high latitude — the product's home turf

| ID | Situation | Exact required behaviour | What the user sees | What is refused |
|---|---|---|---|---|
| E01 | Any event search requests a rise, set or twilight crossing that does not occur | The engine's `searchAltitude` / `searchRiseSet` return `Time?` — **null is an expected value, not an error**. The caller returns a typed `None`, never `!!`, never a substitute time. | The brief renders a typed sentence for the missing event. | A synthetic time, an exception, a zero-length interval presented as an interval. |
| E02 | **Polar day at Reine** — the Sun's minimum altitude is +1.37° on the solstice, so there is no sunset and no astronomical darkness | The dark-window result is a typed `NoAstronomicalDarkness(darkestLevel, atInstant, sunAltitude)` carrying the **deepest twilight actually reached** and the instant it occurs. The brief states it. The scene still renders the **true** sky, driven by the Sun's altitude through the same LUT as every other night — no special-case drawing. The dark-window ribbon greys the astronomical band and labels it. Share text says the same sentence. The widget says it. The reminder is **not offered**. | `No astronomical darkness at Lofoten tonight — the sky only reaches nautical twilight (Sun −14.6° at 01:04).` | A dark window of `00:00–00:00`, a black sky with stars, a "no data" screen, a reminder for a window that does not exist. |
| E03 | **Civil twilight also absent** (δ > +16.07° at Reine) | `darkestLevel` = `TWILIGHT_NAUTICAL` and the sentence names the level that *is* reached; the ribbon hides the bands that do not occur rather than drawing zero-width bands. | As E02, with the level named. | Drawing a civil-twilight band of zero width. |
| E04 | **No darkness of any kind** — the Sun stays above the horizon (midnight sun) | `darkestLevel = DAY`; the brief's first line becomes the *light* window: the Sun's minimum altitude and its time ("the Sun stays above the horizon; lowest 1.4° at 01:04"). The scene renders daylight through the LUT. | A true statement about the light, not an empty dark panel. | Any night-shaped output. |
| E05 | **Polar night** — the Sun never rises (Reine, ~early December to ~early January) | The night window is the whole civil day: `[00:00 local, 00:00 local next day)`, and the brief reports the Sun's **maximum** altitude of the day as a positive fact ("the Sun reaches −1.2° at 12:14"). Moon and target events still computed normally. | `The Sun does not rise today. The sky stays in twilight; deepest dark 09:41–14:52.` | `sunrise 00:00`, an empty light panel, a crash on the null rise. |
| E06 | The user asks for the brief on a date whose sunset is on the previous calendar day (asking at 01:30 local) | The scene's date parameter is the **local date of sunset**, not the device's current date. Asking at 01:30 on the 9th for the night in progress resolves to the 8th's night. An explicit "tonight / that night" affordance disambiguates. | The header names both calendar days: `the night of Tue 8 → Wed 9 Sep`. | A window starting after the instant the user is looking at it, or a night that silently became tomorrow. |
| E07 | Latitude ≥ the Arctic/Antarctic Circle, high sun | All of E02–E05 is reachable **also at mid-latitudes in June** (astronomical darkness is absent at 52.5 °N from roughly late May to late July). This is not an exotic branch: it is the ordinary summer case for most of Europe, so the typed-no-darkness path is on the main path, not behind a latitude test. | The same sentences, at Berlin as at Reine. | Treating "no dark window" as a polar-only special case; a code path per latitude band. |
| E08 | Latitude at or within a few degrees of a pole (input validation) | Latitude is validated to `[−90, +90]`; a name-only spot is not created. At exactly ±90° the azimuth of every body is undefined, so the app computes altitude only and states that the scene's azimuth axis is meaningless there. | An explicit note for |φ| = 90. | A NaN azimuth, a divide-by-zero on an azimuth scale, a silently wrong scene. |
| E09 | Longitude at ±180°, or a spot whose local date differs from the device's | The spot's calendar date comes from the spot's zone, computed from the spot's longitude-derived zone *id the user chose*, never from the device's zone. | The brief's date is the spot's date. | A spot one day off because the device is on the other side of the line. |
| E10 | The Sun's rise/set search is asked for a window that contains the event twice (a multi-day scene) | The search is segmented and returns a **list** of crossings in order; two sunsets in one window are legal and both render. | Two light/dark cycles in the scene. | Returning "the" sunset — the 1-vs-15 class of failure. |
| E11 | Twilight windows at high latitude touch or overlap (nautical ends after civil begins, in the same instant) | Windows are half-open intervals `[start, end)`; a zero-length window is dropped, not rendered as a hairline. | Only real windows appear. | A hairline band and a boundary label colliding with itself. |
| E12 | The user's drawn horizon blocks the whole sky (all samples at 90°) | Legal. Every target and pass is reported as "behind your horizon", the reason is stated once, and a single non-blocking confirmation is offered because an all-90° profile is usually a mis-draw. | `Your horizon blocks the whole sky.` | An empty target list with no reason; silently caching an unusable profile. |

### 2.2 The Moon

| ID | Situation | Exact required behaviour | What the user sees | What is refused |
|---|---|---|---|---|
| E13 | **Moon below the horizon for the whole dark window** | The moonless window equals the dark window; the Moon's altitude curve is drawn below zero for reference. | `Moonless all night.` | Inventing a moon-set time inside the window. |
| E14 | **Moon above the horizon for the whole dark window** | The moonless interval is **empty** and is reported as a distinct, typed outcome. The app also reports the Moon's **minimum altitude and its time** within the dark window, because that is the actionable instant when the Moon is least in the way. | `The Moon is up all night (72% lit); lowest 14° at 03:20 — there is no moonless interval.` | An empty panel, a `—`, or a fabricated moonless interval of zero length. |
| E15 | Moon rises inside the window | The moonless window is `[darkStart, moonrise)`; the brief names both boundaries and the Moon's illuminated fraction at the boundary instant. | Two windows, labelled. | One merged window. |
| E16 | Moon sets inside the window | The moonless window is `[moonset, darkEnd)`. | As above. | As above. |
| E17 | **Full moon crossing the antisolar point** | The Moon is by construction above the horizon all night and near maximum altitude at local midnight; the brief says so, and the moonless outcome is E14. | `Full moon, opposite the Sun: up all night, highest 58° at 01:04 — the sky is washed out.` | Reporting a moonless window that does not exist. |
| E18 | **New moon** | Illuminated fraction ≈ 0 is treated as moonless regardless of altitude, and the sentence says "new moon" rather than "0 %". | `New moon — the dark window is unobstructed.` | A "0 % Moon" panel that the user has to interpret. |
| E19 | Mapping a phase angle to a named phase, near 0°/360° | The phase angle is reduced `mod 360` and cut into **8 sectors of 45° centred on 0/90/180/270**, so waxing crescent near 359° does not fall into the wrong bucket. | Correct names around the wrap. | The wrap bug: `new` at both ends of the table. |
| E20 | The Moon's rise/set does not occur (polar) | `None`, per E01; the moonless result degrades to "the Moon does not rise" or "does not set", whichever is true. | `The Moon does not set tonight.` | A default rise time. |
| E21 | Which source defines the Moon — the port's `illumination()`/`searchRiseSet()`, or anything else | Exactly one source, and one convention for refraction, shared by the brief, the scene and the widget. The port's Moon path has **never been numerically verified** (ON RECORD, `F-engine-gap-map.md` §2.1: only the Sun case was checked, at 0.0248 arcmin; the three Moon cases were refused). That verification is a **build task**, not an assumption. | Nothing visible; this is a correctness requirement. | Mixing `illumination()` (solar-system bodies) with a catalogue's magnitude for a deep-sky object. |

### 2.3 Targets

| ID | Situation | Exact required behaviour | What the user sees | What is refused |
|---|---|---|---|---|
| E22 | **Target never rises at this latitude** (δ < −22.07° at Reine) | A typed `NeverRises`; the altitude curve is not drawn as a flat line at zero; the target is excluded from "what is up tonight" and from pass/target recommendations. | `M7 never rises from this spot.` | A flat curve, a rise time, membership in tonight's list. |
| E23 | **Target circumpolar, never sets** | A typed `Circumpolar(minAltitude, atInstant)`; the curve is drawn with its real minimum above zero. | `M31 is up all night; lowest 31° at 02:14.` | A rise/set pair, an empty curve. |
| E24 | Target rises and sets **twice** in one requested window | Returned as a list of crossings, in order (E10). | Both windows listed. | `the` rise. |
| E25 | **Target exactly at the horizon** | "Rise" is crossing of the **local drawn horizon** at that azimuth, not 0°. Where the profile blocks 12°, the event is "clears your horizon at 23:41", and the brief says so. Apparent altitude with refraction is the single convention. | `clears your horizon (12°) at 23:41`. | `rises at 23:12` (geometric, ignoring the user's ridge). |
| E26 | Target's maximum altitude **equals** the threshold (grazing tangency) | Classified `Marginal`: the app states the peak altitude and the duration above the threshold. | `grazes your horizon — peaks at 12.0° for under a minute.` | A rise and a set one second apart; a root-finder oscillating at the tangent. |
| E27 | Target with a **missing magnitude** in the compiled table | Unranked: sorted **after** everything with a known magnitude, and labelled `magnitude unknown`. | Unknowns at the end of "brightest first". | Parsing a missing value as 0 and putting all unknowns at the top of the list. |
| E28 | The catalogue's coordinate epoch vs the date (J2000 to 2026) | The table stores J2000 coordinates and states its epoch; whether the port precesses them internally is **UNVERIFIED** (§3.2) and must be settled by measuring one object against an independent ephemeris. If it does not, the residual is ~0.36° by 2026 (`INFERRED`: 50.3″/yr × 26 yr) — enough to change a framing answer. | Nothing visible; either the framing maths is right or a named caveat is printed. | Silently presenting J2000 coordinates as of-date. |
| E29 | The user's lens makes the target not fit | The FOV overlay is computed from focal length and sensor size **in millimetres, always** — focal length is never unit-converted (E71). | `M31 needs 2.6° × 1.0°; your 300 mm frame is 4.6° × 3.1° — fits.` | Converting millimetres under an imperial setting. |
| E30 | A target's name/number query returns many objects | Search matches on designation and on every common name; results are ordered by magnitude with unknowns last; the query itself is kept in `SavedStateHandle` so it survives process death. | A stable, restored result list. | Re-querying after rotation and clearing the user's input. |
| E31 | A stored reference to a target that the compiled table no longer contains | Targets are stored by **stable string id**; on load, a missing id is dropped with a note and the spot is unaffected (E104). | `1 saved reference could not be restored.` | Storing an index that now points at a different object. |

### 2.4 Satellites

| ID | Situation | Exact required behaviour | What the user sees | What is refused |
|---|---|---|---|---|
| E32 | **Element format** | Parse **OMM/CSV or JSON from CelesTrak, not fixed-width TLE**: since 2026-07-11 newly catalogued objects are 6-digit (100000+) and "GP data will not be available for them using the TLE format" (§0.1, MEASURED). A `TLE`-only parser works today for the curated legacy set and fails the first time the list grows. | Nothing visible. | A TLE parser with a 5-digit assumption; a silent skip of an object whose id is 6 digits. |
| E33 | Building the propagator from OMM/CSV fields | Map the measured OMM fields (`OBJECT_NAME`, `EPOCH`, `MEAN_MOTION`, `ECCENTRICITY`, `INCLINATION`, `RA_OF_ASC_NODE`, `ARG_OF_PERICENTER`, `MEAN_ANOMALY`, `NORAD_CAT_ID`, `BSTAR`) into the propagator's initial state. **Resolved 2026-10-08 (MEASURED, §3.3):** Orekit *does* ship an OMM parser that yields a `TLE` directly — `OmmParser.build()` → `Omm.generateTLE()` — so the parser is the **primary** path and this field mapping is the **fallback** for a record the parser refuses. | Nothing visible. | Assuming a parser exists because the package has a `files.ccsds` tree; **and**, now that it is confirmed to exist, hand-mapping every record instead of using it. |
| E34 | CelesTrak returns **any** non-200 (301, 403, 404, 429, 50x) | **Stop immediately, no retry loop**, surface a typed state, and do not re-attempt until the next permitted cadence (E35). This is the source's own rule for a machine client (§0.2, MEASURED). | `Element data unavailable (temporarily).` with the last-known ages if any. | A retry loop; a silent fallback to an old cache presented as current. |
| E35 | Fetch cadence | At most **one fetch per update**, and GP data "updates once every 2 hours" (§0.2) → cache TTL = 2 h per group; never refetch on every app open. | Nothing visible; the app is fast and the source is not hammered. | A fetch per screen entry. |
| E36 | **Stale element set** — the recon measured SGP4 diverging by **27,924 km after ≈3 weeks** of extrapolation (`F-engine-gap-map.md` §1.1, ON RECORD) | Age is always computed from the element set's own epoch and always **displayed** beside the prediction. Pinned policy (product decision, `INFERRED`): **≤ 24 h** normal; **24 h – 72 h** shown with an explicit "elements are N days old — times may be off by minutes" badge; **> 72 h** no pass **times** at all, only "element set expired; connect once to refresh". The 72 h line is chosen conservatively because the measured failure was observed at ~3 weeks and the penalty for a wrong minute (a missed pass) is asymmetric; the experiment that would replace it with a measured curve is in §4. | A badge, or a refusal with the reason. | Silently predicting from a week-old cache on an app that advertises offline use. |
| E37 | **Missing element set at first run, offline** | The satellite card renders a typed `unavailable` state naming the reason. The brief is otherwise complete and shares normally. | `Satellite passes need one connection to download element data.` | Shipping a bundled element set that was frozen at build time and presenting it as current. (Bundling the **object list** — names and catalog ids — is fine; bundling **elements** is not.) |
| E38 | **The pass occurs but is not observable** — satellite in Earth's shadow | Passes are reported in two distinct states: **occurs** and **observable**. A pass in umbra is `occurs, not visible (satellite in Earth's shadow)`; it is never silently dropped (silently dropping reads as a missed pass) and never offered as observable. | `A pass occurs at 23:47 but the satellite is in Earth's shadow.` | Dropping the pass; presenting it as visible. |
| E39 | **The pass occurs in daylight** (Sun above ≈ −6°) | Same two-state treatment: `occurs, not visible (the sky is too bright)`. In polar summer **every** pass is in daylight, and the card says that once, not per pass. | As above, at Reine in June in one sentence. | A list of "passes" the user cannot see. |
| E40 | Only part of the pass is sunlit | The **observable window** is the intersection of (above the horizon) ∧ (satellite sunlit) ∧ (observer dark), and it is a sub-interval that is displayed. In the hero scene the streak is drawn over the **same** sub-interval so the picture and the words agree. | A shortened, honest window; a matching streak. | A streak spanning the whole pass while the text says 40 seconds. |
| E41 | Eclipse model | One model, stated: umbra = not visible; penumbra = visible-but-fading (a product decision, `INFERRED`). | A consistent answer at the boundary. | Two different shadow rules in the pass finder and the scene. |
| E42 | **The user's drawn horizon hides the pass** | The detector uses the drawn profile as its elevation mask (the recon verified `ElevationMask(double[][])` exists but is **unexercised** — `F-engine-gap-map.md` §4.1, ON RECORD), so a pass behind the user's ridge is not "visible". | `rises behind your horizon`. | A generic 10° constant while the app draws a 25° mountain. |
| E43 | **The 1-vs-15 trap** | The pass search is **segmented into ≤1-hour propagation windows** (or stepped and refined); a test asserts that the 3-day / 10°-mask case with the fixture element set returns the **15 AOS / 30 events** the brute-force scan returns. This is the single most likely defect to ship looking correct. | Nothing visible; the app finds the passes. | A single multi-day `propagate()` call. |
| E44 | The scene asks for satellite streaks over a window that contains many objects | The number of drawn streaks is capped, and the cap is stated rather than silently truncating; the underlying pass list is complete even when the drawing is capped. | A stated cap in the layer legend. | Drawing 200 streaks and calling that "the" picture; truncating the data. |

### 2.5 Weather (`api.met.no`)

| ID | Situation | Exact required behaviour | What the user sees | What is refused |
|---|---|---|---|---|
| E45 | **The coordinate leaves the device** | The request carries the spot's latitude/longitude **truncated to 4 decimals** — the source's own rule and a 403 for 5+ (§0.3, MEASURED). Truncate, not round, and state the consequence: ≈ 11 m at 4 decimals. | Nothing visible; a privacy line in Settings. | Six-decimal "GPS precision" coordinates; sending coordinates for any spot the user is not looking at. |
| E46 | Data safety | Location (approximate) is declared as transmitted, and **MET Norway is named as a recipient** in the privacy policy, because the app transmits coordinates off the device. | The Data-safety form and the policy agree with the code. | A "no data collected" declaration while coordinates are in the query string. |
| E47 | **User-Agent** | A fixed, honest UA naming the app and a contact address, sent on every request. The measured detail that the endpoint answered identically without one (`Compare-Object` = 0 over two 38,302-byte probes) is **not** permission; the ToS requires it and a random string is a permanent ban (§0.3). | Nothing visible. | A per-install random or empty UA; removing the header to "simplify". |
| E48 | **Attribution** | The credit is rendered wherever the forecast is: the source name, a **tappable link to the CC BY 4.0 licence**, and a statement of modification (we truncate coordinates and re-render the data). It survives every declared locale. | `Cloud forecast · MET Norway · CC BY 4.0 · retrieved 21:14`. | The provider's name in the developer's own prose only; an unlinked credit; attribution dropped in a translation. |
| E49 | **Offline / unreachable** | The forecast card renders `no forecast`, and if a cached reading exists, that reading **with its retrieval time and an age label**. The rest of the brief is unaffected; nothing blocks. | `No forecast available. Last reading 6 h old: 40 % cloud.` | A spinner that never resolves; a stale number without an age; a blank panel with no reason. |
| E50 | **429 / throttled** | One attempt, then stop, back off past the `Expires` interval, and surface the state (§0.3, MEASURED: "Always check response `Status` headers and limit traffic immediately"). | `Forecast temporarily unavailable.` | A retry loop; a higher request rate. |
| E51 | The response has **no hours inside the requested window** (window beyond the model horizon, or a badly wrong device clock) | A typed `no hours cover this window`, naming the window and the range the response actually covered. | An exact sentence. | Interpolating the nearest hour as if it covered the window; showing the last row's value. |
| E52 | The response is malformed or the schema changed | A typed failure, never a crash; the card says the forecast is unavailable and the failure is logged without user data. | `Forecast unavailable.` | An exception propagating to the brief; a partially rendered ribbon. |
| E53 | Refresh policy | Cache on disk, send `If-Modified-Since`, honour `Expires`, never `HEAD` then `GET`, and **never fetch while the app is not in use** (§0.3, MEASURED). | A warm, fast forecast card. | A background worker fetching weather. |
| E54 | The source's request shape | HTTPS only; a plain-HTTP fallback is refused by the client rather than followed (§0.3, MEASURED). | Nothing visible. | An `http://` URL that "works" via redirect. |
| E55 | The forecast is presented | Always labelled a **forecast**, with the model/retrieval time on screen, and it is **total cloud only** — no low/mid/high split exists in this source, so no transparency or seeing model is promised anywhere, including the listing. | `Total cloud cover (forecast)`. | Any wording that reads as a guarantee; implying a layer split the data does not have. |

### 2.6 Location

| ID | Situation | Exact required behaviour | What the user sees | What is refused |
|---|---|---|---|---|
| E56 | **Permission denied** | The app is fully usable: the user sets a spot by search, by map/drag, or by pasted coordinates. Location is requested **only** on an explicit "use my location" action, never at launch, and denial is not an error state. | A picker, not a wall. | A blocking dialog; retry prompts; a feature that needs the permission. |
| E57 | **Coarse-only fix** | Accepted for the sky (a 3 km error moves a twilight boundary by ~1–2 s — `INFERRED`: ~0.0067°/km of latitude, ~4 s per minute of arc). It is **not** accepted silently for a drawn horizon, because the horizon belongs to a viewpoint: the app labels the spot `approximate` and lets the user nudge the coordinates. | `approximate location — adjust if needed`. | Presenting a coarse fix as exact; refusing to work with coarse only. |
| E58 | **Manual entry** | Decimal or DMS in, one canonical representation out; validated to lat `[−90, 90]`, lon `[−180, 180]`; a south/west sign error is caught by requiring a hemisphere control rather than a `-` the user can forget. | A validated, named spot. | Storing an out-of-range coordinate; silently swapping lat/lon for a comma-separated paste. |
| E59 | **A saved spot with no coordinates** | Impossible by schema: the save action requires a valid pair. A record already on disk that fails validation is quarantined (E103), not coerced. | `1 saved spot could not be read.` | A spot at (0, 0). |
| E60 | **A location in an unexpected timezone** | The spot carries an explicit IANA zone id, defaulted from the device at creation and editable; all times render in the **spot's** zone. Where the device zone differs, one line says so. | `Times in Europe/Oslo (spot) · your device is Europe/Moscow`. | Times in the device's zone for a spot abroad — the classic "the app shows tomorrow's night" defect. |
| E61 | Which permission, minimum | `ACCESS_COARSE_LOCATION` only. `ACCESS_FINE_LOCATION` is not needed: the astronomy is insensitive to 3 km and the horizon is the user's to anchor. No `ACCESS_BACKGROUND_LOCATION`, ever (§1.2). | One permission, asked once, refusable. | A fine-location request for a job that cannot use the extra precision. |
| E62 | Using the device's own position as the spot | "Use my location" creates a spot from the fix, and the spot is thereafter **independent** of the device's position — moving the phone does not move the spot. | A normal saved spot. | A spot that silently follows the phone, so yesterday's brief changes today. |

### 2.7 Time

| ID | Situation | Exact required behaviour | What the user sees | What is refused |
|---|---|---|---|---|
| E63 | **Spot zone vs device zone** | Every instant is stored and computed as an absolute instant; rendering uses the spot's zone (E60). The scene's time axis is an **absolute** axis, so nothing about the geometry depends on which zone is displayed. | Correct times for the place. | An axis labelled in local clock minutes of the device. |
| E64 | **DST spring-forward** — 02:00–03:00 local does not exist | The continuous axis simply never produces those labels: the display steps `01:59 → 03:00`. Computations are unaffected because they run on absolute time. | A gap, correctly. | `02:30` shown as a real instant; a wrong dark-window length. |
| E65 | **DST fall-back** — 01:00–02:00 local occurs twice | The axis is unambiguous (absolute), so the repeated hour is labelled twice and the offset is shown once per label. | Two 01:30s, distinguishable. | A duplicated label with no offset; a slider that jumps backwards. |
| E66 | **A night that spans midnight** | The window is `[sunset on D, sunrise on D+1]`; both calendar days are labelled. The scene's requested date is the date **of sunset** (E06). | `night of 8 → 9 Sep`. | A window cut at 00:00; a date control that flips to tomorrow at 23:00. |
| E67 | **Sunset and sunrise on different calendar days, in different months** (high latitude, E05) | The polar-night window is the civil day D (E05); the general rule still holds. | One labelled window. | A negative-length interval; an assertion that end > start. |
| E68 | **Multi-day scene** | The scene's maximum span is stated, and event lists over it are complete (E10). | A bounded, correct scrub range. | An unbounded slider that recomputes the whole ephemeris per frame. |
| E69 | **12 h vs 24 h** | One internal representation; the display follows the user's setting, defaulting from the platform's own 24-hour preference. The scene's axis labels use the same rule as every other time label in the app. | Consistent times everywhere. | A 24-hour axis with a 12-hour brief. |
| E70 | Which instant is "now" | `now` is captured once when the brief is opened and advanced by the UI, not re-read from the system clock on every recomputation. | A stable scene that does not twitch. | A scene that jumps because the recomputation raced the clock. |
| E71 | **Metric vs imperial** | Temperature, wind and any visibility distance convert. **Focal length and sensor dimensions do not** — they are millimetres everywhere, and the framing arithmetic (E29) never converts. | `300 mm · 10 °C · 5 m/s` or `300 mm · 50 °F · 11 mph`. | `11.8 in` focal length; a frame that "fits" only in one unit system. |

### 2.8 The horizon profile

| ID | Situation | Exact required behaviour | What the user sees | What is refused |
|---|---|---|---|---|
| E72 | **Storage and resolution** | The drawn silhouette is stored as a **quantised azimuth→elevation array**, never as a bitmap: **720 samples at 0.5° azimuth**, elevation in **tenths of a degree** as a signed integer, clamped to `[−10°, +90°]` (~1.4 kB per spot). Other azimuths interpolate linearly between samples, wrapping at 0/360. | Identical geometry at any screen size. | A bitmap whose meaning depends on the phone's resolution. |
| E73 | **Extreme aspect ratio** | The drawing canvas maps azimuth `0…360` linearly across the full width and elevation across a **declared vertical field** (default 0–30°, adjustable). The array is the truth; the canvas is a view of it. | The same profile on a 21:9 phone and a 4:3 tablet. | A profile that means 40° on one device and 25° on another. |
| E74 | **Drawn on a phone, opened on a tablet** | Because the source of truth is the degree array (E72), re-rendering is exact. | The same ridge, the same numbers. | Re-deriving the profile from the stored bitmap at a different size. |
| E75 | **A flat line** | Legal: a flat profile at 0° (or at the chosen value) is a horizon, used by every calculation. | A flat foreground, sensible maths. | Treating "flat" as "no profile"; rejecting the drawing. |
| E76 | **No profile at all** | A distinct state, `hasProfile = false`: calculations use 0° and every surface that depends on it says so once. | `No horizon profile for this spot — using a flat horizon.` | Treating "no profile" and "flat profile" as the same state. |
| E77 | **A horizon that blocks everything** | See E12. | As E12. | As E12. |
| E78 | The profile blocks the Moon's rise | The moon-rise event is masked: the app states that the Moon rises but stays behind the drawn horizon, and the moonless window is computed from the **apparent** (masked) visibility. | `The Moon rises at 21:10 but stays behind your horizon.` | A moonless window computed from an unobstructed horizon while the scene shows a blocking ridge. |
| E79 | One source of truth | The scene's foreground silhouette is a **rendering of the same array** the maths uses. | The picture matches the numbers. | A hand-drawn overlay layer that can disagree with the array. |
| E80 | Rotation or process death **while drawing** | The in-progress draft is held in saveable state and survives rotation; process death restores the draft or discards it with a prompt, never commits a half-drawing. | The drawing survives a rotate. | A drawing lost on rotation; a partially drawn profile committed. |
| E81 | Panning or zooming the scene | An independent view transform; the azimuth mapping is unaffected, and the currently visible azimuth range is stated. | A pan that does not change what the profile means. | A pan that shifts the 0° point. |

### 2.9 Rendering and accessibility

| ID | Situation | Exact required behaviour | What the user sees | What is refused |
|---|---|---|---|---|
| E82 | **Device rotation** | The scene is a pure function of (spot, instant, lens, layer flags); rotating recomputes nothing that is not already in the ViewModel, and it does not restart the animation. | An instant, unchanged rotation. | A rebuilt scene, a reset slider. |
| E83 | **Process death** | The chosen spot, date, instant, lens and the brief's scroll position are in `SavedStateHandle`/`rememberSaveable`; the app returns to the same frame. | The same frame. | Back to first launch. |
| E84 | **Low memory** | Star geometry is cached in a `drawWithCache` block keyed by (viewport, magnitude limit, rotation) and is **dropped and rebuilt** on `onTrimMemory` rather than thrashing. The drawn star count is capped by a magnitude limit. | A smooth scrub on a mid-range phone. | A per-frame star rebuild; a stall on a low-end device. |
| E85 | **Below the shader floor (API 26–32)** | The twilight gradient is a **baked LUT/`LinearGradient`** path for API 26–32 and may be an AGSL `RuntimeShader` on API 33+. The two paths produce the same sky colours within a stated tolerance, the selection is by `Build.VERSION.SDK_INT`, and a shader compile failure falls back to the LUT **for the session** rather than re-throwing. The A40 (Android 11) exercises the LUT path; the A16 (Android 16) is the shader-path check. | The same sky on both phones. | Two different skies; a probe that can throw; a crash on a device that cannot compile the shader. |
| E86 | **Dark mode and light mode** | The **Night scene is always rendered on the slot's dark surface** regardless of the system theme (a light-mode night sky is wrong); the surrounding chrome responds to dark/light. | A correct night sky in either theme. | A washed-out scene for a light-mode user. |
| E87 | **Very large system font** | The scene is graphics; the brief and every text surface reflow, with no fixed-height text container. Labels inside the scene may abbreviate but stay legible. | A readable brief at 200 % font scale. | Clipped labels; the ribbon collapsing. |
| E88 | **RTL** | The scene's horizontal axis is an **azimuth** axis and is **not mirrored** by layout direction — mirroring would put the user's west ridge on the wrong side and make the picture a lie. Text inside the scene is localised; the surrounding chrome mirrors normally. The scene's viewing azimuth (default south in the northern hemisphere, north in the southern) is stated, not inferred from layout direction. | A correctly oriented scene in Arabic/Hebrew. | A mirrored horizon; an RTL layout that flips the sky. |
| E89 | **TalkBack** | The hero scene is a graphic, so it needs a semantic summary: spot, date, the Sun's altitude, the Moon's state, the dark window, and the current instant — plus a `Seekable` time slider whose value is announced (`01:04, the Sun is 18° below the horizon`). Every interactive element has a name and a 48 dp target. | A usable hero screen with TalkBack. | A silent `Canvas` — the single most likely accessibility failure in this app. |
| E90 | Colour is the only signal | Every colour-coded band (twilight levels, moonless vs moonlit) also carries a label or a shape. Caption ink meets the WCAG contrast the palette gate measures. | Readable by a colour-blind user. | A red/green-only distinction. |
| E91 | **The rating prompt** | Fires only after a completed session — a saved spot plus a viewed brief plus either a share, an export or a launched reminder — and never on first launch, never more than once per version, and nothing is gated behind it. | A well-timed ask. | A launch-time dialog; a nag; a feature behind a rating. |
| E92 | Edge-to-edge and predictive back | The scene extends behind the system bars; toolbars respect insets; predictive back works at the target SDK (`AGENTS.md` compliance floor). | Clean insets, correct back gesture. | Content under the status bar; a back gesture that closes the app. |
| E93 | The system clock is hours wrong | Every computed instant is displayed **with its UTC instant** so the mismatch is visible, and where a network response carries a `Date` header, a difference greater than a few minutes raises a one-line warning. | `Your device clock looks 2 h 13 m off (vs the network).` | Silently computing a correct-looking brief for the wrong day. |
| E94 | The Sun's altitude is outside the LUT's range (below the darkest band) | Clamped to the darkest band; no divide-by-zero, no negative index. | A correct darkest sky. | A band index out of range. |
| E95 | The slider sits exactly on an event instant | The event's label is rendered beside the slider and the instant is attributed to the interval it opens (`23:04 · astronomical dark begins`). | An unambiguous boundary. | A label that flickers between the two sides of the boundary. |
| E96 | Screen off / long session | No wake lock; the reminder is a local alarm; the scene stops animating when not visible. | No battery drain. | A held wake lock; an off-screen animation loop. |

### 2.10 Data integrity

| ID | Situation | Exact required behaviour | What the user sees | What is refused |
|---|---|---|---|---|
| E97 | **The bundled catalogue asset is unreadable or missing** | The app starts; the Targets screen shows a typed empty state naming the problem; the brief omits targets and says so. A build-time test asserts the asset is packaged and parses. | `Target list unavailable.` | A crash; a brief with silently no targets; a fabricated catalogue. |
| E98 | **A corrupt saved spot** | The store is loaded **per record**: one bad record is quarantined, the rest load, and the quarantine is reported once. | `1 saved spot could not be read.` | The whole store failing to load; silent loss; a coerced default spot. |
| E99 | **A saved spot pointing at a removed target** | Targets are referenced by stable id; a missing id is dropped with a note and the spot survives (E31). | A note, and a working spot. | An index-based reference; a spot invalidated by a catalogue edit. |
| E100 | **Writes** | Atomic: write a temporary file, then rename. A crash mid-write must leave the previous good store intact. | Nothing. | A truncated store after a crash. |
| E101 | **Export/import schema** | `schemaVersion` first; unknown-field policy stated; a version newer than the app is refused with a message rather than half-imported. | A precise refusal. | Best-effort parsing of a future file. |
| E102 | **Backup rules** | `dataExtractionRules` (API 31+) and `fullBackupContent` (API ≤ 30) both present; the plan store is included; regenerable caches (element sets, forecasts) are **excluded**; there are no secrets to exclude, and the rules say so. | Restore works after a reinstall. | Caches restored as if fresh; the store excluded by omission. |
| E103 | **Quarantine is durable** | A quarantined record is not silently dropped on the next save: it is either kept aside or the user is told it will be discarded. | An explicit choice. | Silent data loss on the next write. |
| E104 | **A crafted or truncated import file** | Caps are validated **before** allocation: file size, spot count, horizon sample count, string lengths. A 10 000 000-sample array must produce a typed error, not an OOM. | `That file is not a Lumintra scout file.` | Allocating from the file's declared length; an OOM kill. |
| E105 | **A spot saved under one device zone, opened under another** | The zone id travels **in the spot record** (E60); re-opening under a new device zone shows the spot's zone and states the difference. | The spot's own times. | Re-deriving the zone from the device and silently re-labelling every time. |
| E106 | **Reinstall** | Auto Backup restores the store; where it does not (backup disabled), the user's exported file is the path (A1/A2). The app never claims a restore happened when it did not. | A truthful state. | A "restored" claim with an empty store. |

### 2.11 First run, force-stop, and the reminder's honest limits

| ID | Situation | Exact required behaviour | What the user sees | What is refused |
|---|---|---|---|---|
| E107 | **First launch, no network, no location, no saved spot** | A usable scoped-empty brief with one call to action ("set a spot"), the catalogue browsable offline, and no dialogs. | A short, honest empty state. | A sky computed for (0, 0); a mandatory permission; a login. |
| E108 | **First launch, network, no location** | The same, and the network ribbons say "no spot to forecast". Nothing is fetched before there is a spot. | As above. | Fetching weather for a default location. |
| E109 | **The device reboots before tonight's reminder** | The reminder must be re-armed. The mechanism (a `BOOT_COMPLETED` receiver vs re-arming on the next app open) is **UNVERIFIED** (§3.4) and must be settled by a boot test on the A40; whichever is chosen, the app must not depend on a receiver it cannot prove delivers. | The reminder fires, or the app states that it will re-arm on next open. | An alarm that silently evaporates — this is the measured 3★ "notifications have stopped working" failure mode. |
| E110 | **The user force-stops the app** | On modern Android a force-stopped app receives no broadcasts and cannot be restarted; **the reminder is not guaranteed until the app is opened again**. This is stated as a limitation rather than hidden. | Honest documentation in the app's own help. | A claim that the reminder "always works". |
| E111 | The reminder's data at fire time | The notification is built entirely from the plan already on the device; if that plan was computed offline, the notification says so, and the cloud figure — if any — is the cached one with its timestamp. | A truthful notification. | A network fetch at fire time (E53 forbids it); a fresh-looking cloud figure that is hours old. |

**Total: 111 situations (E01–E111).**

---

# Part 3 — What this analysis could not settle

Each row names the check that would settle it. Nothing here is a fact I have; all are `UNVERIFIED`.

### 3.1 Import from a competitor
Whether any incumbent on the light-planner shelf exports spots/horizons in a machine-readable form.
**Check:** install `com.photopills.android.photopills`, `com.yingwen.photographertoolspro` and
`com.astro.dsoplanner` on the A40, save a location, and enumerate every file the app writes under
`/sdcard/Android/data/<pkg>/` and any `ACTION_CREATE_DOCUMENT`/share intent it offers
(`adb shell dumpsys package <pkg>` for declared `ACTION_SEND`/`ACTION_CREATE_DOCUMENT` filters, then
drive the save flow and inspect the output). If a format exists and is documented, B4 becomes a real
option; if not, A10 is the whole answer.

### 3.2 Does the port precess a J2000 `defineStar` to the date?
**Check:** compute M31's altitude/azimuth at a fixed instant and place with the port, and compare
against an independent source (JPL Horizons, or Stellarium with the same time and place). Record the
arcminute error. Then read `defineStar` / `horizon` in `astronomy.kt` for a precession rotation. The
reason this matters: a J2000-as-of-date error is ~0.36° by 2026 (`INFERRED`), which changes a
"does it fit my frame" answer.

### 3.3 Does Orekit ship an OMM parser that yields a propagator?
**Question, and the check that was proposed:** `javap -public` over the Orekit jar for
`org.orekit.files.ccsds` and its `omm` package, and check whether an `Omm` →
`TLEPropagator`/`SpacecraftState` path exists. If it does not, the fallback is the measured field
mapping into the two fixed lines (E33), which is a field mapping and not a parser project.

**ANSWER, 2026-10-08 — it does, so this is closed rather than carried.** The check was run on the
pinned `orekit-13.1.9.jar` (8,538,135 B, sha256 `A1DBEC16…0FBE`; MEASURED). The package
`org.orekit.files.ccsds.ndm.odm.omm` contains `OmmParser`, `Omm`, `OmmData`, `OmmMetadata` and
`OmmTle`. `OmmParser` declares **`public Omm build()`**; `Omm` declares **`generateTLE()`**,
`generateKeplerianOrbit()` and `generateSpacecraftState()`; and `OmmTle` exposes the SGP4 element set —
`getEphemerisType()` with the constants `EPHEMERIS_TYPE_SGP`, `EPHEMERIS_TYPE_SGP4`,
`EPHEMERIS_TYPE_PPT3`, `EPHEMERIS_TYPE_SGP4_XP`, `EPHEMERIS_TYPE_SPECIAL_PERTURBATIONS`, plus
`getBStar()`, `getMeanMotionDot()`, `getMeanMotionDotDot()`, `getElementSetNo()`, `getRevAtEpoch()`,
`getNoradID()` and `getClassificationType()`. The `Omm` → `TLE` path is therefore **direct**. The
`TLE(int, char, int, int, String, int, int, AbsoluteDate, double×8, int, double)` field constructor was
re-confirmed present on the same revision and is the fallback. **What the check did not establish, and
therefore what remains a required test rather than a claim:** whether `generateTLE()` tolerates a
6-digit `NORAD_CAT_ID` (E32), and whether a non-SGP4 `EPHEMERIS_TYPE` is refused explicitly.

### 3.4 Is the reminder guaranteed after a reboot?
**Check:** on the A40, schedule a reminder, reboot the device, and observe whether the notification
fires with (a) a manifest `BOOT_COMPLETED` receiver at `android:exported="false"`, and (b) at
`android:exported="true"`. The API 31+ rule requires an explicit `android:exported`; which value
actually receives the protected system broadcast is the thing to measure, not to argue.

### 3.5 The real SGP4 timing-error-versus-age curve
The 72-hour stale-element floor in E36 is a product decision, not a measurement, and it is deliberately
conservative. **Check:** freeze one live ISS element set at T0, store it, and each day for ~21 days
propagate both the frozen set and that day's fresh set over the same next-24 h window; record the
maximum AOS-time difference and the maximum along-track distance. That one experiment replaces the
guess with a curve and a stated confidence interval.

### 3.6 The actual twilight dates at Reine
The §2 preamble's thresholds are derived geometry (`INFERRED`). **Check:** run the port's
`searchAltitude(Body.Sun, …, −18.0)` at (67.93, 13.09) for every date of one year and print the first
and last date on which the dark window exists; compare against a published twilight table for Lofoten
(e.g. timeanddate's). This is also the regression fixture for E02.

### 3.7 The Moon path's numerical accuracy
`F-engine-gap-map.md` §7 leaves this `UNVERIFIED` and it is load-bearing for the whole Moon half of the
brief. **Check:** `searchRiseSet(Body.Moon, …)` and `illumination(Body.Moon, …)` against a handful of
Horizons rows, recorded in arcminutes, exactly as `AstroCorpus.kt` already does for the Sun.

### 3.8 Does the AGSL and the LUT path agree?
**Check:** render the same (spot, instant) on the A40 (Android 11, LUT) and the A16 (Android 16,
shader) and compare sampled pixel colours per band. Until this is measured, E85's "same sky" is a
requirement, not a fact.

---

# Part 4 — The three edges most likely to become bugs if the specification is silent

These are the three where the *natural* implementation is wrong and the wrongness is invisible — the
defect ships looking correct, which is the failure class this factory keeps paying for.

1. **E43 / E01 / E10 — the single-call event search.** `ElevationDetector` over one multi-day
   `propagate()` found **1** AOS where a brute-force scan found **15**, and `maxCheck` from 600 s to
   10 s changed nothing (ON RECORD, `F-engine-gap-map.md` §4.2, reproduced 3×). The same shape exists
   for every other event search in this app: twilight, moonrise, and target rise/set over a multi-day
   or polar window. If the spec does not require **segmented (or stepped-and-refined) searches that
   return lists of crossings, with a brute-force cross-check in a test**, the app ships a satellite
   card that shows one pass where fifteen happen and a brief that misses a second sunset.
2. **E02 / E03 / E04 / E05 — no dark window at all.** The natural implementation returns an empty or
   zero-length interval, and every downstream surface renders "nothing" — or worse, a black sky with
   stars for a night whose sky never gets dark. This is not exotic: astronomical darkness is absent at
   **52.5 °N** for roughly two months of summer, so it is the ordinary June case for most of Europe,
   and it is the *permanent* case for Reine for much of the year. The spec must require a **typed
   result** carrying the deepest twilight actually reached, an instant it occurs, and one sentence the
   brief, the widget, the share text and the reminder all reuse — plus a scene that renders the true
   sky from the Sun's altitude through the ordinary LUT, with no special case.
3. **E60 / E63 / E64 / E65 — the spot's timezone and the DST night.** The natural implementation
   formats the device's local clock, which is wrong the moment the user plans a trip abroad, and it
   builds the scene's time axis out of local clock minutes, which is wrong twice a year when an hour
   does not exist or exists twice. Both errors are invisible at home and produce a brief that is off by
   an hour or a day exactly when the user is standing in the dark. The spec must require an **absolute
   time axis** with all labelling derived from the **spot's** zone, an explicit zone per spot, and a
   statement of the difference when the device's zone differs.

---

# Claim ledger

| CLAIM | LABEL | EVIDENCE |
|---|---|---|
| CelesTrak's usage policy is reachable at `/usage-policy.php` (HTTP 200, 13,980 bytes) and the recon's `HTTP 500` was taken from the wrong path, which returns a 404 page | MEASURED | `Invoke-WebRequest https://celestrak.org/usage-policy.php` this session → `STATUS=200 BYTES=13980`, saved to `.kiln/scratch/spec-2026-10-08/celestrak-usage-php.html`; the existing `.kiln/scratch/spec-2026-10-08/celestrak-usage.html` is a 404 page reading "File not found for celestrak.org/usage-policy/" |
| CelesTrak's policy page contains no licence grant, no redistribution clause and no commercial-use clause | MEASURED (absence) | Read the full 13,980-byte body this session; the only licence-shaped reference is a link to `/NORAD/documentation/gp-data-formats.php` |
| CelesTrak ran out of 5-digit catalog numbers on 2026-07-11; new objects are 100000+ and GP data is not available for them in TLE format | MEASURED | `celestrak-usage-php.html`, the "URGENT" banner, quoted verbatim in §0.1 |
| CelesTrak's CSV GP format is "~10% smaller than the TLE format and far easier to parse" and is what clients are told to move to | MEASURED | Same page, same list item, quoted verbatim |
| CelesTrak GP data updates once every 2 hours and clients must "only download data once per update" | MEASURED | Same page, "Only download the data you need…" list item, quoted verbatim |
| CelesTrak tells machine clients to stop immediately on any non-200 and says repeated ignoring sends the IP to the firewall | MEASURED | Same page, the "Most importantly…" item, quoted verbatim |
| CelesTrak collects only IP addresses and sends no user data beyond that | MEASURED | Same page footer, "CelesTrak's Simple Privacy Policy" popover text, quoted verbatim |
| api.met.no requires an identifying User-Agent, and a fake or random one risks a permanent ban | MEASURED | `.kiln/scratch/spec-2026-10-08/metno-tos.html`, "Abuse" section, quoted verbatim in §0.3 |
| api.met.no returns 403 for requests with 5+ decimal coordinates and requires truncation to 4 decimals | MEASURED | `metno-tos.html` line 220-221, quoted verbatim |
| api.met.no forbids mobile apps from retrieving new data while the app is not in use, and caps push-notification polling at one per 10 minutes | MEASURED | `metno-tos.html` line 224-226, quoted verbatim |
| api.met.no requires CC BY 4.0 attribution (credit, a link to the licence, and an indication of changes) | MEASURED | `metno-tos.html` lines 183-193, quoted verbatim |
| api.met.no's ceiling is 20 requests/second per application and throttling returns 429 | MEASURED | `metno-tos.html` lines 230-234 and 285-288, quoted verbatim |
| api.met.no answered byte-identically with and without a User-Agent (two probes, 38,302 bytes each, zero differences) | MEASURED | `Compare-Object (Get-Content metno-compact-noUA.json) (Get-Content metno-compact-UA.json)` → difference count `0`; both files are exactly 38,302 bytes |
| `ElevationDetector` over one multi-day `propagate()` finds 1 AOS where a 60 s brute-force scan finds 15, unchanged for `maxCheck` 600/300/60/10 s, reproducible 3× | ON RECORD | `.kiln/scratch/positioning-2026-10-08/F-engine-gap-map.md` §4.2, quoting `.kiln/scratch/positioning-2026-10-08/run-passvariants.txt` and `run-passcrosscheck.txt`; I did not re-run the probes |
| The same report records SGP4 diverging by 27,924.0197880 km on the `20413#2` block at +1,844,000 s (≈21.3 days) | ON RECORD | `F-engine-gap-map.md` §1.1 quoting `.kiln/scratch/nv-core-proof/RESULT.md` and `corpus-tinydata.txt` |
| `ElevationMask(double[][])` exists on Orekit 13.1.9 but no probe exercises it (all use `withConstantElevation`) | MEASURED | `F-engine-gap-map.md` §4.1, recorded as `MEASURED` by `javap`; re-confirmed 2026-10-08 on the pinned jar by `javap -classpath orekit-13.1.9.jar org.orekit.utils.ElevationMask` → `public org.orekit.utils.ElevationMask(double[][])`. **Note the package:** the class lives in `org.orekit.utils`, not `org.orekit.models.earth`, and the absence of a probe is still an absence |
| The port's Moon path has never been numerically verified; only `sun_dc_eclipse` was, at 0.024758 arcmin | ON RECORD | `F-engine-gap-map.md` §2.1 and §7, quoting `verbose-astro.txt`'s `ASTRO_AGGREGATE cases=4 verified=1 unverified=3` |
| The port exposes `defineStar`, `searchAltitude`, `searchRiseSet`, `horizon`, `moonPhase`, `illumination`, `angleFromSun` | ON RECORD | `F-engine-gap-map.md` §2.1 and §3.1, with `astronomy.kt` line numbers read by that report |
| Feature requests are the second-most-frequent low-star complaint class at 15.13 % (median), and 6.12 % of all feature requests were for better notification support | ON RECORD | `docs/research/2026-10-02-user-review-depth-signals.md` §1.1, citing Khalid et al., IEEE Software 2014 |
| Export/backup/portability ranks top-4 in both independent datasets (661 and 743 mentions) and is the paid product in four commercial apps | ON RECORD | `docs/research/2026-10-02-user-review-depth-signals.md` §2 and §3 |
| "No offline access" is a top-5 ranked complaint at 587 mentions | ON RECORD | Same document, §2 table (r/iosdev 50,000-review analysis) |
| Reminders rank #4 among named extras at 364 requests | ON RECORD | Same document, §2 (`bigideasdb.com` table) |
| A widget is a named reason an app survives on the home screen, and a capped widget is a named complaint | ON RECORD | Same document, §2 verbatim quotes from Notes Widget Reminder and Authenticator App |
| The depth rule ranks export/backup #1 and requires depth in the job's own stages rather than breadth | ON RECORD | Same document, §7 |
| An unused feature is a removal candidate below ~5 % engagement, and a requested sync extra destroyed a game's rating through a data-loss race | ON RECORD | Same document, §6 |
| The adjacent satellite shelf carries a measured 1★ review for being blocked on granting alarms and notifications, and a 3★ for notifications that stopped working | MEASURED | `.kiln/scratch/positioning-2026-10-08/D-complaints-satellite.md`, verbatim critical reviews for `com.vitotechnology.SatelliteTracker` and `com.runar.issdetector` (I read the file this session; it is a scan the positioning scout ran) |
| The same shelf's critical reviews split 50 % missing-feature, 50 % notifications, 50 % paywall | MEASURED | Same file, "Pocket complaint profile" table |
| The contract has 7 claims and no `data_in` claim; the connective floor is already met by `share-the-session-brief` (`data_out`) and `pin-the-session-scout` (`durability`) | MEASURED | `docs/ideation/plans/lumintra-light-planner.differentiators.json`, `claims[].depthFamily`; read this session |
| The depth family token lists are `ACTION_OPEN_DOCUMENT`/`ClipboardManager` for `data_in`, `ACTION_SEND`/`ACTION_CREATE_DOCUMENT`/`FileProvider` for `data_out`, `dataExtractionRules`/`fullBackupContent` for `durability`, `NotificationChannel`/`NotificationManagerCompat` for `reminder`, `GlanceAppWidget`/`AppWidgetProvider` for `widget`, `SearchBar`/`searchQuery` for `find` | MEASURED | `.cursor/rules/app-depth.mdc:77`–`:82`, read this session |
| The toolchain is minSdk 26 / targetSdk 36 / compileSdk 37, Kotlin+Compose+M3, and exact alarms are treated as restricted | MEASURED | `AGENTS.md` "Required toolchain" table and "Play compliance floor" |
| Data safety's "collect" means transmitting data off the device, so a weather fetch needs a Location declaration | MEASURED | `AGENTS.md`, "Play compliance floor", the Data-safety bullet |
| The app's `purchase_only_no_ads` shape and the excluded-monetization list | MEASURED | The brief and `docs/ideation/plans/lumintra-light-planner.economics.json` as cited in `differentiators.json`'s `share-the-session-brief` evidence |
| A body's altitude at lower culmination is `φ + δ − 90`; circumpolar when `δ > 90 − φ`; never rising when `δ < −(90 − φ)`; and the Sun misses altitude `h` when `δ > (h + 90) − φ` | INFERRED | Standard spherical astronomy; derived in §2.0, no source quoted. The consequences for Reine (no astronomical darkness when δ > +4.07°; no sunset at the solstice, `alt_min = +1.37°`) follow by substitution |
| Astronomical darkness is also absent at ~52.5 °N in midsummer, so the typed-no-darkness path is a main path, not an exotic branch | INFERRED | Same formula at φ = 52.5 → δ > +19.5°, which the Sun exceeds from roughly mid-May to late July |
| 4 decimals of latitude ≈ 11 m, and a 1–3 km location error moves a twilight boundary by roughly 1–2 s | INFERRED | 1° latitude = 111.32 km → 0.0001° = 11.1 m; 0.0067° of solar altitude per km of latitude, at ~0.004°/s of solar motion |
| A 50-minute pass-timing error corresponds to roughly 23,000 km of along-track displacement at 7.66 km/s | INFERRED | The measured complaint ("went over 50 mins or so later than it said", `D-complaints-satellite.md`) × the ISS's orbital speed |
| A J2000 coordinate presented as of-date is ~0.36° wrong by 2026 | INFERRED | General precession ≈ 50.3″/yr × 26 yr = 1,308″ = 0.363° |
| The 72-hour stale-element floor, the umbra/penumbra visible rule, the widget's 1-hour `updatePeriodMillis`, the `merge`/`replace-all` import policy and the "what's up tonight" default are product decisions | PRODUCT DECISION | Proposed here; each labelled `INFERRED` in place. None is a measurement, and each is the owner's to accept or replace at checkpoint 1 |
| Whether any incumbent exports spots/horizons in a machine-readable format | UNVERIFIED | §3.1 names the check |
| Whether the port precesses a J2000 `defineStar` to the date | UNVERIFIED | §3.2 names the check |
| Orekit ships an OMM parser yielding a propagator | MEASURED | javap -public over orekit-13.1.9.jar: OmmParser.build() -> Omm; Omm.generateTLE()/generateKeplerianOrbit()/generateSpacecraftState(); OmmTle exposes the SGP4 set (§3.3) |
| Whether generateTLE() tolerates a 6-digit NORAD_CAT_ID | UNVERIFIED | §3.3: not established by javap; the satellite-passes E32 unit test is the check |
| Whether the reminder survives a device reboot, and with which `android:exported` value | UNVERIFIED | §3.4 names the check |
| The real SGP4 timing-error-versus-element-age curve | UNVERIFIED | §3.5 names the experiment |
| The first and last date of the year on which Reine has a dark window | UNVERIFIED | §3.6 names the check |
| The Moon path's arcminute accuracy | UNVERIFIED | §3.7 names the check |
| Whether the AGSL and LUT sky paths produce the same colours | UNVERIFIED | §3.8 names the check |
| Whether `POST_NOTIFICATIONS` is the only runtime permission the reminder needs, and whether a `BOOT_COMPLETED` receiver can be `exported="false"` and still receive the broadcast | UNVERIFIED | §3.4's boot test on the A40; the platform rule is stated in `AGENTS.md`'s security baseline ("do not mark receivers exported unless required and protected") |
