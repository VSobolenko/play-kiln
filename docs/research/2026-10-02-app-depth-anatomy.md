# The anatomy of app depth: what separates a complete utility from an MVP

**Measured 2026-10-02.** The complaint this document serves: small utility apps ship as working
demos of one operation — a gradebook that only adds/edits/deletes, a calculator that only
calculates — with no import/export, search, templates, stats, share, automation, or saved work.
This is a **reading of published sources**, not an opinion piece and not a restatement of any
internal rule.

**Labels.** **MEASURED** — the cited page's own words were returned to this document in this run
(search snippet or direct fetch); the source tag resolves to a URL in **Sources**. **ON RECORD** —
a named source states it, not independently reproduced here. **INFERRED** — this document's
reasoning. **UNVERIFIED** — not checked. Where a page is a practitioner blog rather than a study,
that is stated in the row. Every MEASURED claim carries its `[tag]`; **Sources** lists the URL for
every tag.

---

## What this measures

Three questions, answered from external evidence: (1) which concrete feature *families* a
professional treats as the difference between a complete tool and a demo, with shipped-app
examples and the bad-review wording each family's absence produces; (2) what product-management
literature says about narrowing *surface* while completing the *job*; (3) which of those families
are achievable in an offline, no-account Android app and which genuinely require a server.
A fourth section records the opposite failure — bloat — and a fifth names the defect precisely.

### The strongest empirical anchor, stated first

Review-mining corpora put **broken basics** far above missing features: one analysis of **67.7 M**
reviews across 8,000 iOS/Android apps found users complain about broken experiences **6×** more
often than they request new features, with ad complaints the #1 quality issue (**MEASURED**,
`[unitq]`). A separate extraction from **136,000+** reviews (7,758 apps) ranked the top pain
themes: crashes/bugs/freezes 4,797, intrusive ads 3,819, pricing/billing 3,475, **missing features
2,599**, login 1,857, **sync/backup/data loss 1,707** (**MEASURED**, `[bigideasdb]`). A keyword
classification of 6,634 audited listings found payment 28.6 %, device compatibility 28.4 %, UI/UX
25.4 %, and **missing features** in 10.3 % of apps (**MEASURED**, `[brightappdata]`). Read
together: **the family that most reliably produces a 1-star review is not a missing feature; it is
work the user lost, or work they cannot get back out.** That reorders the families in §1.

---

## 1. The feature families

A "family" is a cluster of *outcomes* a user expects from a tool, not a screen. Only some have a
measured retention or rating link; each row says which. The table is ordered by how often the
sources read here tie the family to retention or to ratings (§1.2 ranks them explicitly).

### 1.1 The ten families

| # | Family | The job it serves | A shipped example | When it is MISSING — the user's words | Evidence |
|---|---|---|---|---|---|
| 1 | **Not losing work** — autosave, undo/redo, version history, trash, backup/restore, transfer | Trust that a mis-tap, a crash or a lost phone does not destroy the record | Google Docs autosaves with no off switch and keeps restorable version history (**MEASURED**, `[gdocs-history]`); Notion keeps version history and a Trash (30-day default) (**MEASURED**, `[notion-trash]`); Android Auto Backup is on by default, 25 MB/app/user, restored at install (**MEASURED**, `[autobackup]`); Material's own guidance is an "Undo" snackbar action held until acted on (**MEASURED**, `[snackbar]`) | *"Data vanishes or won't sync across devices."* — the #6 pain theme, 1,707 documented mentions (**MEASURED**, `[bigideasdb]`). Data-loss clusters appear in 192 of 6,634 apps, sync in 162 (**MEASURED**, `[brightappdata]`) | MEASURED + ON RECORD |
| 2 | **Trusting the app** — offline reliability, data longevity, no lock-in | The tool still works, and the user's record is not held hostage by it | Google Takeout exports data in portable formats it chooses for longevity (e.g. contacts as vCard) to a download or to Drive/Dropbox/Box/OneDrive (**MEASURED**, `[takeout]`); QuickQR ships with **no `INTERNET` permission** as a stated property so "nothing can be uploaded even in principle" (**MEASURED**, `[quickqr]`) | Evernote is the measured counter-example: proprietary `.enex`, *"Good luck opening those ENEX files… vendor lock-in"*, import losses (formatting, attachments, dates), and users unable to export after a downgrade (**MEASURED**, `[evernote-xda]`, `[evernote-reddit]`); a 13-year, 6,453-note archive needed third-party extraction (**MEASURED**, `[beginefusion]`) | MEASURED |
| 3 | **Being reminded** — notifications, recurring reminders, calendar | The app acts when the job is due, not when the user remembers | WorkManager schedules persistent work that survives app exit and reboot (**MEASURED**, `[workmanager]`); daily reminders are built as `OneTimeWorkRequest` + `setInitialDelay` with **no exact-alarm permission** (**MEASURED**, `[zenn-wm]`, `[so-wm-exact]`); Calendar entry is handed to the Calendar app via `ACTION_INSERT` with **no permission** (**MEASURED**, `[calendar-provider]`) | *"Reminders and alerts silently fail."* — 465 documented mentions, ranked just below slow performance (**MEASURED**, `[bigideasdb]`); notification clusters in 388 of 6,634 apps / 1,259 reviews (**MEASURED**, `[brightappdata]`) | MEASURED |
| 4 | **Data in & out** — import CSV/Excel/clipboard, export CSV/PDF/image, share sheet, print | Get the user's existing data in, and their result into whatever tool they already use | Storage Access Framework `ACTION_OPEN_DOCUMENT` / `ACTION_CREATE_DOCUMENT` with **no system permissions** (**MEASURED**, `[saf]`, `[create-doc]`); the print framework renders to a PDF the user prints (**MEASURED**, `[print]`); the share sheet sends a `content://` file to any app (**MEASURED**, `[share]`) | *"There is no way to export your data — you can't email a list, share a task, or extract all of the data to a csv list. I can't use an app that I can't get my data out of."* (**MEASURED**, `[hitlist]`); *"There are no graphs and no reports… we can't export data"* (**MEASURED**, `[watchpower]`) | MEASURED |
| 5 | **Seeing the whole picture** — statistics, charts, trends, totals, reports | Turn a pile of records into a decision | Vico renders Cartesian charts offline in Compose, Apache-2.0 (**MEASURED**, `[vico]`); Notion surfaces per-page updates and analytics (**MEASURED**, `[notion-updates]`) | *"No graphs, no averages, no way to export data to a useful tool."* and *"I can't export data for my doctor. I cannot graph my results. It's useless."* (**MEASURED**, `[relion]`); the same review asks for *"graphs and exporting data"* as the two things that would earn 5 stars (`[relion]`) | MEASURED |
| 6 | **Finding things** — search, filter, sort, tags, saved views | Find one record among thousands without scrolling | Todoist filters are saved custom query views (date/project/label/priority), favouritable (**MEASURED**, `[todoist-filters]`); Gmail organises by labels plus search rather than folders (**MEASURED**, `[gmail-organize]`) | No single verbatim quote was measured for search alone; review-mining counts **search/organization** among the concrete capabilities users explicitly ask for and do not get (**MEASURED**, `[unitq]`); *"I can't find anything"* is **INFERRED** from that gap list | MEASURED (gap category) + INFERRED (wording) |
| 7 | **Fitting the person** — themes, custom fields, reordering, units, formats, accessibility | The same job done the user's way | Todoist labels carry colour and can be favourited, and the sidebar/Quick Add are user-configurable (**MEASURED**, `[todoist-labels]`, `[todoist-quickadd]`); Notion database properties and views are user-defined, and a database must keep at least one view (**MEASURED**, `[notion-views]`) | **Customization** is among the requested-gap categories (**MEASURED**, `[unitq]`); the reverse failure is also measured — ClickUp's *"overwhelming number of configuration decisions before they've even created a single task"* (**MEASURED**, `[clickup]`) and GoodNotes' *"complicated interface"* after it became an "everything app" (**MEASURED**, `[goodnotes]`) | MEASURED |
| 8 | **Getting faster over time** — templates, presets, defaults, recents, favourites, quick actions | The tenth use costs less than the first | Notion's template gallery spans 250+ categories (**MEASURED**, `[notion-templates]`); Todoist setup templates copy a whole project/filter/label set, and Quick Add parses natural-language dates, `%labels` and `p1–p3` (**MEASURED**, `[todoist-quickadd]`, `[todoist-templates]`) | No verbatim quote measured; **INFERRED** wording *"I retype the same thing every day"*. The measured lever behind the family is different: **< 5 % of users change any setting**, so defaults are the real product (**MEASURED**, `[uie-defaults]`; meta-analytic default effect d ≈ 0.68, `[defaults-meta]`) | MEASURED (mechanism) + INFERRED (wording) |
| 9 | **Working with others** — share, export-and-send, QR, integrations | The output must be able to leave the app | `ACTION_SEND` + `Intent.createChooser` + a `FileProvider` `content://` URI with no runtime storage permission (**MEASURED**, `[share]`, `[fileprovider]`); QR generation is fully offline via ZXing `core`, no internet and no permission (**MEASURED**, `[zxing]`); Google's Data Portability API moves a copy of data between services (**MEASURED**, `[data-portability]`) | *"You can't email a list, share a task, or extract all of the data"* (**MEASURED**, `[hitlist]`); data portability legislation is argued to relieve only the data-based part of lock-in, not network or workflow lock-in (**ON RECORD**, `[ssrn-portability]`) | MEASURED + ON RECORD |
| 10 | **Doing things in bulk** — multi-select, batch edit, duplicate, merge | Change a hundred records in one act | Gmail ships Select-all (50/page on Android in 2024) then bulk delete/archive/label (**MEASURED**, `[gmail-delete]`, `[gmail-selectall]`); Notion duplicates and trashes/restores pages (`in_trash`), with permanent delete not exposed (**MEASURED**, `[notion-trash]`) | No verbatim quote measured. The demand is documented by the product press: the Android Select-all, when it arrived in 2024, was *"long awaited"* and its absence meant *"you had to manually select each email"* (**MEASURED**, `[gmail-selectall]`, `[gmail-selectall2]`) | MEASURED (demand) |

### 1.2 Ranked by how often the literature read here ties the family to retention or rating

Ranking is by strength and volume of evidence **in the sources read for this document**, not by
aesthetic importance. The honest split: ranks 1–4 are backed by measured review volumes or
platform guidance; ranks 5–7 by review quotes and gap categories; ranks 8–10 by weaker or indirect
evidence, and the wording is marked INFERRED.

1. **Not losing work** — the single clearest rating/retention signal measured: 1,707 data-loss/sync
   mentions, plus data-loss and sync complaint clusters, plus Google's own version-history defaults.
2. **Trusting the app / no lock-in** — same evidence plus Evernote's documented migration failures;
   the 6:1 broken-vs-feature-request finding makes reliability, not novelty, the retention lever.
3. **Being reminded** — 465 mentions; 388 apps / 1,259 reviews. Notification RCTs show tailored,
   timed prompts raise proximal engagement (**ON RECORD**, `[mhealth-push]`), which is the only
   experimental support any family here has.
4. **Data in & out** — measured review quotes plus Kano guidance that **export** is an "obviously
   Basic" capability to ship before anything else (**ON RECORD**, `[rework-kano]`).
5. **Seeing the whole picture** — measured review quotes requesting graphs/reports as the condition
   for continued use.
6. **Finding things** — named among explicitly requested, unfulfilled capabilities.
7. **Fitting the person** — personalization raises engagement, strongest in week one and decaying
   after (**ON RECORD**, `[personalization]`).
8. **Getting faster over time** — defaults study is strong; the template half is not measured.
9. **Working with others** — the share sheet is platform-standard; no retention measurement found.
10. **Doing things in bulk** — the weakest evidence: product-press demand, no rating/retention study.

---

## 2. The "one job, done completely" principle

**The crux.** The two positions in the literature are not "few features vs many"; they are two
different definitions of *minimum*.

### Position A — viable means someone chooses it (surface narrow, job complete)

Marty Cagan defines the MVP as *"the smallest possible product that has three critical
characteristics: people choose to use it or buy it; people can figure out how to use it; and we can
deliver it when we need it with the resources available – also known as valuable, usable and
feasible."* (**MEASURED**, <https://www.svpg.com/minimum-viable-product/>). His named failure mode
is the app in question: *"I use the term 'Minimal Product' to refer to this smallest possible
product that actually works and is usable. Unfortunately, that's often a long way from a Viable
Product which not only works, but is something that people actually choose to buy and use."*
(**MEASURED**, <https://www.svpg.com/viable-product-vs-minimal-product/>). The load-bearing
question is *"not 'could you use it?' but rather 'would you use it?'"* (**MEASURED**, same URL).

### Position B — minimum viable is the smallest thing that teaches you (thin by design)

Eric Ries, quoted in InfoQ: *"The minimum viable product is that product which has just those
features and no more that allows you to ship a product that early adopters see and, at least some
of whom resonate with, pay you money for, and start to give you feedback on."* (**MEASURED**,
`[infoq]`). The same source's corrective is the sentence that matters for this document: *"Any
version of a product that does not deliver value is useless."* (**MEASURED**, `[infoq]`).

### The reconciliation the sources converge on: cut scope, not the job

- **Minimum Lovable Product.** *"An MVP is the minimum functionality a product needs to achieve
  viability in market. In contrast, MLP delivers the minimum a product needs to be loved by
  users… the bare minimum is rarely the right solution."* (**MEASURED**, `[aha-mlp]`). The
  operational difference: *"MVP optimizes for learning speed. MLP optimizes for retention from
  first use. MVP asks, 'Will anyone use this?' MLP asks, 'Will anyone come back?'"* (**MEASURED**,
  `[atta-mlp]`). And the discipline it demands is **sharper scope, not more time**: *"Do one thing,
  for one user, at a level of quality that produces genuine love."* (**MEASURED**, `[mlp-ss]`).
- **Single-feature MVP.** *"This is not a rough prototype… the constraint is scope, not quality."*
  (**MEASURED**, `[kromatic]`).
- **Depth over breadth / complete flows.** *"Partial flows are worse than missing features"* and
  *"Depth over breadth: 5 features at 60 % quality → 2 features at 100 % quality"* (**ON RECORD**,
  `[scope-skill]`). *"A narrow product can still feel complete. The test is whether a real person
  can finish the job you promised."* (**MEASURED**, `[saas-mvp-task]`).
- **Functionality vs feature.** *"Search is functionality. Collaboration is functionality… Choose
  depth over breadth when your core workflow still feels fragile."* (**MEASURED**, `[functionality]`).
- **Minimum Marketable Feature.** *"The Minimum Marketable Feature is the smallest unit of
  functionality with intrinsic market value"* (Denne & Cleland-Huang, 2004) (**MEASURED**,
  `[mmf]`, `[mmf-book]`); Agile Alliance adds that released work should let *"your customer
  accomplish something they weren't able to before"* (**MEASURED**, `[mmf-aa]`).

### Where the evidence is strong, and where it is opinion

| Claim class | Strength |
|---|---|
| Review volumes and complaint themes (data loss 1,707; missing features 2,599; notifications 465) | **MEASURED** corpus, large n, but aggregator methodology, not peer review |
| Retention benchmarks: first-session meaningful action → 2–3× day-30 retention | **ON RECORD**, cited to Amplitude/UXCam/Adjust (**MEASURED** secondary, `[uxcam]`, `[sonar]`) |
| "Any version that does not deliver value is useless" | **MEASURED** quote, but **opinion** (practitioner essay) |
| Cagan's Minimal vs Viable Product; MLP "love"; MMF "intrinsic market value" | Canonical practitioner literature — **ON RECORD**; the terms are established, the thresholds are not measured |
| "Partial flows are worse than missing features"; "30–50 % of the roadmap disappears" | **ON RECORD / opinion** — a consultant's rule of thumb, no experiment |
| "Depth over breadth" as a rule | **ON RECORD**; converged on by several practitioner sources, none experimental |

**Honest bottom line:** the *vocabulary* (viable vs minimal, lovable, MMF) is well-established but
practitioner-authored; the *numbers* that make it operational come from review corpora and retention
benchmarks, and the strongest of those say that **an app is judged on whether it finishes the job
without losing the user's work**, not on how few or many features it lists.

---

## 3. What depth is achievable without a server, an account, or a permission

All rows below are platform APIs read in this run; "Server?" is about **this app owning a service**,
not about the OS using Google's infrastructure.

| Capability | How it is done | Dependency | Permission / cost | Server? | Ref |
|---|---|---|---|---|---|
| Import a file (CSV/Excel/PDF) | Storage Access Framework: `ACTION_OPEN_DOCUMENT` / `OpenDocument`; user picks, app gets a URI | platform | **none** | no | `[saf]` |
| Import from clipboard | `ClipboardManager` reads `ClipData` the user copied | platform | none | no | `[clipboard]` |
| Export a file | `ACTION_CREATE_DOCUMENT` / `ActivityResultContracts.CreateDocument("text/csv")`, write via `ContentResolver.openOutputStream` | androidx.activity | **none** | no | `[create-doc]`, `[csv-pdf]` |
| Generate CSV | plain Kotlin string building + `OutputStream` | none | none | no | `[csv-pdf]` |
| Generate PDF | `PdfDocument` (render Compose to bitmap, draw onto page canvas) or `PrintedPdfDocument` | platform | none | no | `[csv-pdf]`, `[print]` |
| Print | `PrintManager.print(jobName, PrintDocumentAdapter, …)`; adapter lays out and writes requested pages | platform | **none** | no | `[print]`, `[print-adapter]` |
| Share out | `ACTION_SEND` + `Intent.createChooser` + `FileProvider.getUriForFile` (`FLAG_GRANT_READ_URI_PERMISSION`) | androidx.core | **none** for app-private files | no | `[share]`, `[fileprovider]` |
| Search / filter / sort | Room `@Fts4`, or `@Fts5` with `BundledSQLiteDriver`; external-content FTS keeps writes on the normal table | Room / SQLite | none | no | `[room-fts]`, `[room-fts5]`, `[fts5]` |
| Local persistence | Room/SQLite; DataStore for settings | Room, DataStore | none | no | `[quickqr]` |
| Scheduled reminders | `OneTimeWorkRequest` + `setInitialDelay`, `enqueueUniqueWork`, post a local notification; survives app exit and reboot | WorkManager | `POST_NOTIFICATIONS` on API 33+; **no exact-alarm permission** | no | `[workmanager]`, `[zenn-wm]`, `[post-notif]` |
| Exact-time alarm | `AlarmManager.setExact` | platform | `SCHEDULE_EXACT_ALARM`; Play grants `USE_EXACT_ALARM` only to alarm/timer apps | no | `[zenn-wm]` |
| Calendar entry | `ACTION_INSERT` with `Events.CONTENT_URI`; the Calendar app confirms | platform | **none** | no | `[calendar-provider]`, `[intents-common]` |
| Charts / trends | Vico Compose (`CartesianChartHost`, line/column/candlestick), Apache-2.0 | Vico | none | no | `[vico]` |
| QR generation | ZXing `core` `QRCodeWriter` → `BitMatrix` → `Bitmap` | ZXing | **none** (scanning needs `CAMERA`) | no | `[zxing]` |
| Backup / restore / new device | Android Auto Backup: on by default, 25 MB/app/user, restored at install, encrypted on Android 9+ | platform | none in-app | **auto-uploads to the user's Drive** | `[autobackup]`, `[backup-overview]` |
| Undo affordance | app-level command stack + Material snackbar action (`LENGTH_INDEFINITE` while an action is offered) | material3 | none | no | `[snackbar]` |
| Offline by construction | ship without the `INTERNET` permission (QuickQR's stated design) | none | none | no | `[quickqr]` |

**What genuinely needs a server** (each would turn the app into a server project, so each is a
stage-0 decision, not a discovery):

- **Cross-device sync with conflict resolution and multi-user collaboration.** Google Docs/Drive
  revisions are account-backed and server-side; nothing in the on-device list above merges two
  divergent copies. (**INFERRED** from `[gdrive-changes]`, `[gdocs-history]`.)
- **Account login, shared workspaces, permissioned sharing.** (**INFERRED**.)
- **Server-originated push** (e.g. FCM). Local reminders do **not** need it (**MEASURED**, `[workmanager]`).
- **Third-party data transfer** — Google's Data Portability API is an OAuth + server archive flow
  with signed URLs and app verification (**MEASURED**, `[data-portability]`).
- **Cloud backup beyond Auto Backup's 25 MB/app** or with its own restore UI across accounts
  (**MEASURED** limit, `[autobackup]`).

**One nuance worth writing down:** "no server" and "offline" are not the same claim. Auto Backup,
WorkManager scheduling and the SAF are all on-device APIs, but Auto Backup does upload to the
user's Drive account. An app that must not touch any network must opt out explicitly
(`android:allowBackup`), and then loses new-device restore (**MEASURED**, `[autobackup]`,
`[backupmanager]`).

---

## 4. The trap of adding features

The opposite error is measured too, and it is why "add everything" is not the fix for §1.

- **Bloat has four named costs:** a *choice* cost imposed on every user who meets an option, an
  *onboarding* cost ("how twelve-step tours are born"), a *support* cost that scales with a
  feature's existence rather than its usage, and a permanent *roadmap* cost. The definition is
  explicitly not a feature count: *"a product with a hundred well-organised capabilities can be
  simpler to use than one with twenty presented badly."* (**MEASURED**, `[kompassify-bloat]`).
- **Individually harmless, collectively fatal:** *"the settings toggle used by nobody, the
  integration built for one deal that closed anyway."* (**MEASURED**, `[kano-guide]`).
- **The settings screen nobody opens:** a repeated survey found **less than 5 % of users changed any
  setting**, and in that program a critical autosave feature was off by default because a file was
  initialised with zeroes — the default, not the settings screen, decided the experience
  (**MEASURED**, `[uie-defaults]`; restated `[uxmag-defaults]`). A meta-analysis of 58 default
  studies (n ≈ 73,675) puts the default effect at d ≈ 0.68 but warns it varies and must be tested,
  not assumed (**MEASURED**, `[defaults-meta]`).
- **Measured reverse failures.** GoodNotes: users report *"even simple note-taking tasks
  unnecessarily complicated and frustrating"*, lag between writing and ink, and battery drain after
  the app became an "everything app" (**MEASURED**, `[goodnotes]`). MyFitnessPal: *"Logging food
  now requires pointless extra clicks… The old diary let me see my whole day at a glance. Now
  everything is buried"* — from a decade-long paying user who cancelled (**MEASURED**, `[mfp]`).
  ClickUp: configuration decisions before the first task, a liability for a team that just wants to
  start (**MEASURED**, `[clickup]`). Onboarding tours confirm the direction: median completion 29 %,
  and tours of 9+ steps complete at 8 % vs 73 % for 1–2 steps (**MEASURED**, `[produktly]`).
- **The decision rules that were found:**
  - **Kano as a veto.** Classify candidates Basic / Performance / Excitement / **Indifferent**;
    *"a feature that tests indifferent is a feature you now have permission not to build, which is
    frequently the most profitable finding."* Skip Kano only when the solution is *"obviously
    Basic (auth, export, accessibility)"* (**MEASURED**, `[kano-guide]`, `[rework-kano]`).
  - **Shape Up's appetite and no-gos.** *"Fixed time, variable scope"*: set the appetite first
    (1–2 or 6 weeks), state the **no-gos** explicitly, and *"you don't cut quality, you cut scope"*
    (**MEASURED**, `[shapeup-boundaries]`, `[shapeup-pitch]`, `[shapeup-stop]`).
  - **Opportunity cost, stated.** Compare each feature's outcome, build and maintenance cost, and
    what is forgone; *"if you only talk about how great one option is without discussing
    alternatives, [stakeholders] don't have the full picture"* (**MEASURED**, `[oppcost]`).
  - **A clarity filter** (consultant blog): does it serve the one core customer, move the one
    metric, rest on real evidence, and is it cheap to remove? Claimed effect: *"30–50 % of the
    roadmap disappears"*. This is **opinion**, not a study (**MEASURED as a claim**, `[clarity]`).

**The honest rule the sources support.** None of them gives a numeric threshold, and only the
defaults study and the review corpora are empirical. What converges is: **(a)** the Basic families
in §1 — data out, not losing work, reminders, search — must exist before any Delight feature; and
**(b)** every addition must be justified by the one job it advances, because everything shipped
carries a permanent maintenance and attention cost. The depth families are "the job done
completely"; bloat is capability *outside* the job. The two rules are the two halves of one
sentence.

---

## 5. Naming the defect precisely

**The name.** **The minimal-product trap** — after Cagan's "Minimal Product" (works, is usable, is
not viable) — or, more descriptively for a bug tracker, **"a partial flow of the job, at
record-keeping depth."**

**One-sentence definition.** *A product that reliably performs the record-keeping middle of a
user's job but never carries that job to a finished, keepable, shareable outcome, so it works and
is usable yet is not something a professional would choose to rely on.*

**Established terms in the literature, and why each is close but not exact:**

| Term | What it says | Source | Fit |
|---|---|---|---|
| **Minimal Product** (vs Viable Product) | Works and is usable, but people do not choose it | Cagan, **MEASURED** `[svpg-minimal]` | **Closest** to the owner's meaning; it names a complete *mechanism* without viability |
| **Thin value proposition** | A weak/undifferentiated promise that names a feature rather than a solved problem | **ON RECORD** `[thin-vp]` | Names the market-facing symptom, not the job-coverage cause |
| **CRUD app** | App whose core job is create/read/update/delete; used dismissively, but *"the CRUD is the easy 20 %; the rules and permissions are the other 80 %"* | **MEASURED** `[crud-glimfly]`, `[crud-dusko]`, `[crud-idiallo]` | Exactly describes the skeleton of an add/edit/delete gradebook; useful as a diagnosis, not a verdict |
| **Toy app** | Pejorative: shallow, throwaway, "not a tool for professionals" | **ON RECORD** `[hn-toys]`, `[hn-toys2]` | Implies the *purpose* is unserious, which is not the defect; a toy can be complete for a tiny niche `[hn-why-toys]` |
| **"A feature, not a product"** | Not standalone-defensible; can be absorbed by a bigger product | **ON RECORD** `[dropbox-feature]` | About defensibility, not job coverage |
| **Partial flow** | *"Partial flows are worse than missing features"* | **ON RECORD** `[scope-skill]` | Names the exact mechanism: the job is started but not finished |
| **No Minimum Marketable Feature** | The release contains no unit of functionality with "intrinsic market value" | **MEASURED** `[mmf]` | The money-facing restatement of the same defect |

**Recommended phrasing.** For a product decision, use **"minimal-product trap"**. For a defect
report, use **"partial flow: the job's middle is implemented, its handoffs are not"** — because the
measured difference is not that the app does too little at the centre, but that it drops the user
before the job is finished and never hands the result out.

---

## Sources

Every tag used above. Pages marked *(fetch)* were retrieved whole in this run; the rest had their
own text returned by the search tool, and were read here. Where a site blocked a direct fetch, that
is noted.

**Review corpora and retention evidence** — `[unitq]` <https://www.unitq.com/blog/we-analyzed-67-7-million-app-reviews-heres-what-users-actually-want/> · `[bigideasdb]` <https://bigideasdb.com/state-of-mobile-app-pain-points-2026> (direct fetch blocked by bot check; the study's own tables were read via search) · `[brightappdata]` <https://brightappdata.com/blog/what-users-complain-about-most-in-mobile-apps> · `[glotier]` <https://glotier.com/apps> · `[arxiv-ai]` <https://arxiv.org/html/2506.10785v1> · `[uxcam]` <https://uxcam.com/blog/mobile-app-retention-benchmarks/> · `[sonar]` <https://trysonar.app/blog/app-onboarding-that-protects-day-1-retention> · `[produktly]` <https://produktly.com/research/saas-onboarding-benchmarks-2026> · `[getperspective]` <https://getperspective.ai/blog/consumer-app-onboarding-drop-off-2026-where-new-users-stall> · `[codexical-empty]` <https://www.codexical.com/posts/2026-05-08-empty-states-product-design> · `[mhealth-push]` <https://pmc.ncbi.nlm.nih.gov/articles/PMC6293241/> · `[personalization]` <https://questromworld.bu.edu/platformstrategy/wp-content/uploads/sites/49/2026/06/1054_Palikot_w34950-3-1.pdf>

**Review text quoted** — `[hitlist]` <https://justuseapp.com/en/app/948328681/the-hit-list/reviews> · `[watchpower]` <https://play.google.com/store/apps/details?hl=en_US&id=wifiapp.volfw.watchpower> · `[relion]` <https://allbestapps.net/android/relion-life> · `[evohome]` <http://lists.opentrv.org.uk/pipermail/opentrv-interest/2015-May/000235.html>

**Product-management literature** — `[svpg-mvp]` <https://www.svpg.com/minimum-viable-product/> · `[svpg-minimal]` <https://www.svpg.com/viable-product-vs-minimal-product/> (fetch) · `[svpg-lean]` <https://www.svpg.com/lean-thinking/> · `[infoq]` <https://www.infoq.com/articles/truth-minimum-viable-products/> · `[aha-mlp]` <https://www.aha.io/blog/minimum-viable-product-vs-minimum-lovable-product> · `[atta-mlp]` <https://blog.atta.systems/mvp-vs-mlp/> (fetch) · `[logrocket-mlp]` <https://blog.logrocket.com/product-management/what-is-a-minimum-lovable-product-mlp-how-to-build/> · `[mlp-ss]` <https://www.startupsuperschool.com/concepts/minimum-loveable-product> · `[mlp-mmp]` <https://www.tahirramzan.com/notes/how-to-choose-between-mvp-mlp-and-mmp-for-your-stage> · `[kromatic]` <https://kromatic.com/real-startup-book/4-evaluative-product-experiments/product-prototyping/single-feature-mvp/> · `[scope-skill]` <https://skillsmp.com/creators/kazdenc/builder-skills/claude-skills-product-prioritize-scope> · `[saas-mvp-task]` <https://dev.to/urielbitton/how-to-scope-a-saas-mvp-around-one-complete-user-task-1h15> · `[microsaas]` <https://microsaasbytes.com/building-single-feature-products> · `[functionality]` <https://www.aakashg.com/functionality-vs-feature/> · `[0toTraction]` <https://0-to-traction.com/lessons/product-strategy/ps-p282> · `[hld-scope]` <https://hld.handbook.academy/curriculum/interview-framework/requirements-scoping/> · `[mmf]` <https://buildd.co/product/minimum-marketable-feature> · `[mmf-aa]` <https://agilealliance.org/glossary/mmf/> · `[mmf-book]` <https://www.oreilly.com/library/view/software-by-numbers/0131407287/ch03.html> · `[mmf-untangle]` <https://agilecheetah.com/mvps-mmfs-and-mmps-untangled/> · `[thin-vp]` <https://venture-lab.org/2026/startup-pitch-deck-red-flags-invest/> · `[hbs-vp]` <https://online.hbs.edu/blog/post/creating-a-value-proposition> · `[dropbox-feature]` <https://philhsc.com/articles/how-to-respond-to-this-is-a-feature-not-a-product> · `[hn-dropbox]` <https://news.ycombinator.com/item?id=41996398> · `[gizmodo-dropbox]` <https://gizmodo.com/everything-apples-trying-to-kill-with-os-x-mountain-lio-5885691> · `[hn-toys]` <https://news.ycombinator.com/item?id=41929918> · `[hn-toys2]` <https://news.ycombinator.com/item?id=11674580> · `[hn-why-toys]` <https://news.ycombinator.com/item?id=16334035> · `[svn-toys]` <https://signalvnoise.com/posts/3873-programming-with-toys-and-magic-should-be-relished-not-scorned?9=> · `[crud-glimfly]` <https://glimfly.com/dictionary/crud> · `[crud-dusko]` <https://www.duskolicanin.com/glossary/what-is-crud-app> · `[crud-idiallo]` <https://idiallo.com/blog/crud> · `[crud-reddit]` <https://www.reddit.com/r/webdev/comments/15lwnym/what_do_people_mean_by_a_simple_crud_app/>

**Android platform APIs (offline / permission surface)** — `[saf]` <https://developer.android.com/training/data-storage/shared/documents-files> · `[create-doc]` <https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.CreateDocument> · `[documents-sample]` <https://android.googlesource.com/platform/development/+/master/samples/ApiDemos/src/com/example/android/apis/content/DocumentsSample.java> · `[csv-pdf]` <https://dev.to/sanjayajoseph/offline-first-challenge-making-csv-pdf-reports-right-on-android-3p4c> · `[saf-commons]` <https://commonsware.com/Jetpack/pages/chap-content-001.html> · `[print]` <https://developer.android.com/training/printing/custom-docs> · `[print-adapter]` <https://developer.android.com/reference/android/print/PrintDocumentAdapter> · `[print-manager]` <https://developer.android.com/reference/android/print/PrintManager> · `[share]` <https://developer.android.com/training/sharing/send> · `[fileprovider]` <https://developer.android.com/reference/androidx/core/content/FileProvider> · `[share-setup]` <https://developer.android.com/training/secure-file-sharing/setup-sharing> · `[share-clipdata]` <https://commonsware.com/blog/2021/01/07/action_send-share-sheet-clipdata.html> · `[workmanager]` <https://developer.android.com/develop/background-work/background-tasks/persistent> · `[zenn-wm]` <https://zenn.dev/nawa_10half/articles/android-workmanager-notification-migration?locale=en> · `[so-wm-exact]` <https://stackoverflow.com/questions/50945529/workmanager-api-for-exact-time-execution> · `[post-notif]` <https://developer.android.com/develop/ui/views/notifications/time-sensitive> · `[room-fts]` <https://developer.android.com/training/data-storage/room/defining-data> · `[room-fts5]` <https://developer.android.com/reference/androidx/room3/Fts5> · `[fts5]` <https://www.sqlite.org/fts5.html> · `[fts-blog]` <https://pallocchi.dev/posts/offline-full-text-search-in-android-and-ios/> · `[calendar-provider]` <https://developer.android.com/identity/providers/calendar-provider> · `[intents-common]` <https://developer.android.com/guide/components/intents-common> · `[vogella-calendar]` <https://www.vogella.com/tutorials/AndroidCalendar/article.html> · `[clipboard]` <https://developer.android.com/develop/ui/views/touch-and-input/copy-paste> · `[vico]` <https://github.com/patrykandpatrick/vico> · `[vico-rel]` <https://github.com/patrykandpatrick/vico/releases/tag/v3.0.0> · `[vico-api]` <https://api.vico.patrykandpatrick.com/vico/compose/com.patrykandpatrick.vico.compose.cartesian/-cartesian-chart-host> · `[zxing]` <https://stackoverflow.com/questions/64443791/android-qr-generator-api> · `[zxing-gen]` <https://www.codelessgenie.com/blog/how-to-generate-a-qr-code-for-an-android-application/> · `[zxing-offline]` <https://thestreamliners.in/blog/implement-a-offline-qr-generation-and-scanning-in-android/> · `[quickqr]` <https://github.com/mustafaer/quickqr> · `[autobackup]` <https://developer.android.com/identity/data/autobackup> · `[backup-overview]` <https://developer.android.com/identity/data/backup> · `[backupmanager]` <https://developer.android.com/reference/kotlin/android/app/backup/BackupManager> · `[backup-kv]` <https://developer.android.com/identity/data/keyvaluebackup> · `[backup-security]` <https://developer.android.com/privacy-and-security/risks/backup-best-practices> · `[snackbar]` <https://m3.material.io/components/snackbar/guidelines> · `[snackbar-api]` <https://developer.android.com/reference/kotlin/androidx/compose/material3/Snackbar.composable> · `[snackbar-android]` <https://github.com/material-components/material-components-android/blob/master/docs/components/Snackbar.md> · `[photo-picker]` <https://developer.android.com/training/data-storage/shared/photo-picker>

**Shipped-app examples** — `[takeout]` <https://support.google.com/accounts/answer/3024190> · `[takeout-thirdparty]` <https://support.google.com/accounts/answer/14452558> · `[data-portability]` <https://developers.google.com/data-portability/user-guide/introduction> · `[gdocs-history]` <https://support.google.com/docs/answer/190843> · `[gdrive-versions]` <https://support.google.com/drive/answer/2409045> · `[gdrive-restore]` <https://support.google.com/drive/answer/16482799> · `[gdrive-changes]` <https://developers.google.com/workspace/drive/api/guides/change-overview> · `[notion-trash]` <https://www.notion.com/help/duplicate-delete-and-restore-content> · `[notion-templates]` <https://www.notion.com/help/guides/the-ultimate-guide-to-notion-templates> · `[notion-updates]` <https://www.notion.com/help/guides/tips-to-keep-your-teams-notion-pages-up-to-date> · `[notion-views]` <https://developers.notion.com/guides/data-apis/working-with-views> · `[notion-api-trash]` <https://developers.notion.com/reference/trash-page> · `[todoist-filters]` <https://www.todoist.com/help/todoist/features/introduction-to-filters-V98wIH> · `[todoist-quickadd]` <https://www.todoist.com/help/todoist/features/use-task-quick-add-in-todoist-va4Lhpzz> · `[todoist-labels]` <https://www.todoist.com/help/articles/introduction-to-labels-dSo2eE> · `[todoist-templates]` <https://www.todoist.com/help/articles/use-todoist-setup-templates-bL0Jkmbwo> · `[gmail-delete]` <https://support.google.com/mail/answer/7401?hl=en-GB&co=GENIE.Platform%3DAndroid> · `[gmail-organize]` <https://support.google.com/mail/answer/9259770?hl=en> · `[gmail-selectall]` <https://www.sammobile.com/news/gmail-android-select-all-button/> · `[gmail-selectall2]` <https://www.androidcentral.com/apps-software/gmail-for-android-widely-rolling-out-select-all-option> · `[mass-delete]` <https://www.howtogeek.com/854071/how-to-mass-delete-emails-in-gmail/>

**Lock-in / portability** — `[evernote-xda]` <https://www.xda-developers.com/stop-using-evernote-for-taking-notes/> · `[evernote-reddit]` <https://www.reddit.com/r/Evernote/comments/18pwspl/plot_twist_no_exporting_for_me_any_ideas/> · `[cyanote]` <https://cyanote.app/blog/leaving-evernote/> · `[beginefusion]` <https://www.beginefusion.com/post/case-study-evernote-to-markdown-migration> · `[ssrn-portability]` <https://papers.ssrn.com/sol3/papers.cfm?abstract_id=4728972>

**Feature bloat / defaults / decision rules** — `[kompassify-bloat]` <https://kompassify.com/blog/feature-bloat-guide> · `[ttpsc-bloat]` <https://ttpsc.com/en/blog/feature-bloat-in-software-development/> · `[kano-guide]` <https://kompassify.com/blog/kano-model-guide> · `[rework-kano]` <https://resources.rework.com/guides/product-manager-playbooks/roadmap-prioritization-rice-kano-ost> · `[oppcost]` <https://www.productplan.com/learn/justify-opportunity-cost> · `[clarity]` <https://justinmckelvey.com/blog/feature-creep-what-to-stop-building> · `[forbes-creep]` <https://www.forbes.com/councils/forbestechcouncil/2026/04/16/how-to-stop-feature-creep-and-prioritize-product-value/> · `[uie-defaults]` <https://archive.uie.com/brainsparks/2011/09/14/do-users-change-their-settings/> · `[uxmag-defaults]` <https://uxmag.com/articles/the-ux-of-default-settings-in-a-product> · `[defaults-meta]` <https://www.cambridge.org/core/services/aop-cambridge-core/content/view/67AF6972CFB52698A60B6BD94B70C2C0/S2398063X1800043Xa.pdf/div-class-title-when-and-why-defaults-influence-decisions-a-meta-analysis-of-default-effects-div.pdf> · `[defaults-product]` <https://www.researchgate.net/publication/325870050_The_Effect_of_Default_Options_on_Consumer_Decisions_in_the_Product_Configuration_Process> · `[shapeup-boundaries]` <https://basecamp.com/shapeup/1.2-chapter-03> · `[shapeup-pitch]` <https://basecamp.com/shapeup/1.5-chapter-06> · `[shapeup-bets]` <https://basecamp.com/shapeup/2.3-chapter-09> · `[shapeup-pdf]` <https://basecamp.com/shapeup/shape-up.pdf> · `[shapeup-stop]` <https://37signals.com/podcast/shape-up-principle-decide-when-to-stop/> · `[goodnotes]` <https://blog.planner-apps.com/has-goodnotes-lost-its-way-the-cost-of-becoming-an-everything-app/> · `[mfp]` <https://www.reddit.com/r/Myfitnesspal/comments/1tfrckb/1star_review_absolute_garbage_overhaul/> · `[clickup]` <https://apps4review.com/clickup-review-2026-has-it-become-too-complex/>

---

## What this document cannot say

It is a reading of published pages, not a measurement of any app: no screen was tested, no build
was run, no user was interviewed. The review-aggregator corpora (`[bigideasdb]`, `[brightappdata]`,
`[glotier]`) are commercial analyses whose sampling is not published in full, and one of them
blocked a direct fetch — its own numbers were read through the search tool. Retention and
onboarding benchmarks are **ON RECORD** to their publishers, not peer-reviewed here. The MMF
definition and Cagan's Minimal/Viable distinction are canonical but practitioner-authored; no
controlled experiment establishes a numeric depth threshold. The Android API facts are the most
reliable class here: they are read from the platform's own documentation and reference pages, and
each row's dependency/permission claim is stated by the page cited.
