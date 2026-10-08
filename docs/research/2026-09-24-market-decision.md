# Market decision — what to launch next (2026-09-24)

> **Why this run happened.** Before it, the factory had **no product slot**. `registry/apps.json`
> holds exactly two entries and both are explicitly throwaway pipeline proofs — `smoke`
> (*"Pipeline smoke test: proves new-slot → signed APK + AAB → store pack → verify PASS"*) and
> `pipe-proof` (*"Throwaway pipeline proof … with no hand edits"*), both `scaffolded`. Neither is an
> app anyone would publish. Both changes in `openspec/changes/` are also closed:
> `add-plant-health-assistant-slot` is marked **ABANDONED 2026-09-23 — do not implement** (its
> differentiator asserted a safety verdict a pre-harvest interval cannot support), and
> `add-visual-schedule-slot` carries **"Status: do not commit this slot"** because its twelve-phrasing
> evidence gate failed. The question "what goes in the kiln next" was therefore genuinely open, not a
> re-litigation.
>
> **Method note.** This run also built the factory's first **review-text sensor**
> (`scripts/lib/play-reviews.mjs`, `scripts/scan-complaints.mjs`), because every prior screen could
> say *that* an incumbent was rated 3.8 and never *why*. That distinction is the whole deliverable:
> a pocket rated 3.8 for a missing feature is a different opportunity from one rated 3.8 because it
> loses the user's photos.

---

## 1. What was measured

| Run | Command | Result |
|---|---|---|
| Broad screen | `node scripts/scan-market.mjs --query-set scripts/market-queries-2026-09-24.json --concurrency 4` | **66 queries in 8.0 s** — 19 `candidate`, 29 `mixed`, 1 `fortress`, 17 `dead-zone`, 0 errors |
| Enrichment | `node scripts/enrich-market.mjs --top 8 --concurrency 3 --query …` (14 queries) | **112 app pages** fetched and parsed, **0 failures** |
| Complaints | `node scripts/scan-complaints.mjs --query … --top 6` (4 pockets) | **72 reviews read, 59 critical**, across 24 incumbents |

Raw artifacts (gitignored, cited rather than copied): `.kiln/deep/scan-2026-09-24.md` (screen),
`.kiln/deep/enrich-2026-09-24.md` (per-app facts), `.kiln/deep/complaints-2026-09-24.md`
(before the classifier revision) and `.kiln/deep/complaints-2026-09-24-after.md` (after).

The query set is new and separate from the calibrated archetype set on purpose: it explores eight
occupational families the factory has never screened (trades-unmeasured, field-service logs, animal
professionals, collections, caregiver records, life-admin, food craft, outdoors record-keeping), and
carries its **own control query** with a hand-read verdict so a run of it can be checked for sensor
drift without touching `scripts/market-queries.json`.

### 1.1 The control query drifted, and what that means

| Date | Query | seen | rated | weak | median | verdict |
|---|---|---:|---:|---:|---:|---|
| 2026-09-23 (hand-read) | `mushroom foraging guide` | 9 | 9 | 4 | 4.20 | `mixed` |
| 2026-09-24 (this run) | `mushroom foraging guide` | 25 | 18 | 10 | 4.05 | `candidate` |

The **visible result count moved from 9 to 25** for an identical query in the identical storefront.
That is not a market move. It means Play's result set varies between runs (surface
personalisation, session, and time), so **a single verdict is a spot check, not a measurement of a
market** — which is exactly what `docs/market-scan-2026-09-23.md` warns about, now demonstrated on
the factory's own control rather than asserted. Consequence adopted for this document: **no
conclusion below rests on a verdict alone**; every one is anchored to enriched per-app install and
review counts, or to review text.

### 1.2 A floor-level blocker: the factory cannot currently run its own gate

`node scripts/kiln-status.mjs` on this machine returns **`VERDICT: NOT READY`**:

| Check | Result |
|---|---|
| node v24.19.0 | PASS |
| python 3.13.14 | PASS |
| JDK 17+ | **FAIL** — *"no JDK 17+ found; set PLAYKILN_JAVA_HOME or JAVA_HOME"* |
| `ANDROID_HOME` | **FAIL** — missing or does not exist |
| Pillow | **FAIL** — *"Pillow unavailable to python"* |
| keytool | **FAIL** — not discoverable |
| factory self-test | **FAIL** |

The self-test failure is a **symptom of the Pillow gap, not a code defect**: `scripts/store-kit.py`
imports `from PIL import Image, ImageDraw, ImageFont` at line 25 and cannot start without it, so the
sandbox's `store-kit --render-only` exits non-zero, `capabilities.json` is never bootstrapped, and
the self-test then crashes at `scripts/selftest.mjs:669` reading a `store/listing.json` that was
never written. `scripts/kiln-status.mjs` already checks for exactly this dependency, so the factory
knows it is required; it is simply absent here.

**Verified as pre-existing, not introduced by this session:** the identical failure reproduces in a
clean `git worktree` at `HEAD` (`578ec72`), where none of this session's files exist.

Consequence: **no slot can be built or verified on this machine until the environment is repaired.**
Four of the six items are one-time setup — point `JAVA_HOME` at a JDK 17, set `ANDROID_HOME`,
`pip install pillow`, and keytool arrives with the JDK. The self-test failure should then be
confirmed gone by re-running it, not assumed gone.

---

## 2. The two live candidates

### Candidate 1 — Pipe sizing for the trades — **DECIDED: this is the next slot (US; NFPA 54 as the anchor code)**

**Demand is proven and the weak pocket is specific.**

| App | Rating | Reviews | Installs | Ads / IAP | Price |
|---|---:|---:|---|---|---|
| `com.pipeflowcalculations.android` — Pipe Diameter Calculator | 3.9★ | 311 | 100K+ | ads, no IAP | free |
| `ru.sotnikov.offsetpipe` — Offset Pipe calculator | 3.6★ | 888 | 100K+ | ads + IAP | free |
| `se.itwelding.pipeguidecalculator` — Pipe Guide Calculator | 3.6★ | — | 100K+ | ads | free |
| `pipe.support.calculator` — Pipe Support Calculator | 3.8★ | — | 5K+ | — | free |
| `com.mrcombi.pipesizing` — Gas Pipe Sizing Calculator | **2.0★** | 38 | 5K+ | neither | **paid $4.99** |
| `com.calculated.josephine` — Pipe Trades Pro (Calculated Industries) | 4.8★ | 1,590 | 50K+ | no ads, IAP | free |

The pocket is the **weak mid-tier at 100K+ installs with 300–900 reviews** — not the loved
incumbent. Calculated Industries is the strong player and is explicitly *not* the target.

**Why the users are unhappy** (verbatim, `scripts/scan-complaints.mjs`, 18 reviews / 13 critical):

- `com.mrcombi.pipesizing` (2.0★, paid $4.99) — *"Results are in metric"* · *"would not return a
  value for a long length high demand run."* · *"Doesnt calculate gas tite and only gives pipe size
  in metric."*
- `com.pipeflowcalculations.android` — *"sucks won't let me input a different pipe size about half
  of the calculations fields are locked so unless you need a 2" pipe every time useless"*
- `se.itwelding.pipeguidecalculator` — *"ads fall over input fields making it unusable. unistalled.
  what a shame"*
- `pipe.support.calculator` — *"Deceptive pics. None of the screens showing lengths from bottom of
  pipe to throat, long point, etc even exist in the app."* · *"New version it is different?"*

**Distinctness sentence (each clause answers a recorded complaint, sources in §2.2):** *a pipe sizing
tool that returns a result for long, high-demand runs, gives it in the unit system you chose, leaves
every input editable, cites the edition of the code table it used, uses the actual multipliers rather
than an opined one, puts no ad over an input, and is bought once instead of rented.*

**Factory fit:** the best of any candidate here. Deterministic arithmetic plus a published code
table; **no server, no ML, no account, no camera, no dangerous permission, offline by construction**.
This is also the archetype the factory has the strongest commercial evidence for — the measured
`electrician calculator` field has a 5M+ install leader selling ads + IAP, and its developer ships a
portfolio of *different* trade tools.

**Monetization:** purchase-led, per `docs/monetization-strategy.md` §3.6 — a one-time unlock for the
full code table plus the sizing workflows, ads absent or secondary. That matches the owner's recorded
commercial history (apps without ads, with in-app purchases of content).

**Risks, stated not softened:**

- **Correctness is the product.** A gas pipe sizing number that is wrong is dangerous. Every formula
  must come from a named, cited standard (NFPA 54, US), and the app must show which edition it used.
  This is quality work, not decoration.
- **Repetitive Content policy — narrower than an archetype claim, and this correction matters.** The
  policy is *"We don't allow apps that merely provide the same experience as other apps already on
  Google Play. Apps should provide value to users through the creation of unique content or
  services.* … *Creating multiple apps with highly similar functionality, content, and user
  experience."* — <https://support.google.com/googleplay/android-developer/answer/9899034>. It
  constrains **near-duplicate apps**, and it does not restrict a developer from holding a portfolio
  across categories: the factory builds a full range of apps and this slot commits it to nothing. The
  part that does bind: the factory must not later ship a second near-identical calculator under a new
  name.
- **Jurisdiction — DECIDED: United States, NFPA 54.** The UK code-table branch (BS 6891 / IGE-UP/2) is
  dropped. Note the consequence recorded in §2.1: the compliance evidence this decision rests on is
  now weaker than it looked, and that is a finding, not a footnote.
- **UNVERIFIED:** that a trade calculator converts at all at this price point — the store page shows
  a badge, not a P&L.

### 2.1 The US pocket is undefended but its demand verdict was an artifact — measured, not suspected

Choosing the US/NFPA 54 branch made this the decisive question, so it was measured before the
decision was recorded rather than after.

**The direct US incumbent does not exist in practice:**

| Package | Name | Rating | Reviews | Installs |
|---|---|---:|---:|---|
| `com.plumbtools.gas_pipe_sizing` | Gas Pipe Sizing - NFPA 54 | **unrated** | **0** | 100+ |

A US app named for the exact standard, with zero reviews. Nobody has taken this space.

**But the screen that suggested US demand was polluted, and the guard could not see it.** Running
`node scripts/scan-complaints.mjs --query "gas pipe sizing" --query "natural gas pipe sizing"` returned
`candidate` for both — and the "weak incumbents" it selected were **not pipe-sizing tools at all**:

| Selected as a weak incumbent | What it actually is | Installs |
|---|---|---|
| `gbis.gbandroid` (GasBuddy) | fuel-price finder | 10M+ |
| `com.app.gng` | Georgia Natural Gas — utility billing | 10K+ |
| `com.gassouth.gs.mobile` | Gas South — utility billing | 50K+ |
| `com.sew.swg` | Southwest Gas — utility billing | 100K+ |
| **every complaint harvested from all of them** | login, autopay, account state | — |

Every harvested complaint was about **mobile account management for a gas utility** — login failures,
autopay not applying, billing mismatches. None was about pipe sizing. The `candidate` verdict was
computed over a shelf that does not answer the query.

The relevance guard reported **`ok`** for those same queries, measured directly with a probe set
(`.kiln/probe/queries-gas-us.json`):

```text
gas pipe sizing                 candidate  ok     30    13/30  7     4.00
natural gas pipe sizing         candidate  ok     30    16/30  8     4.20
```

**Why the guard is blind here — and the provenance of this finding.** It counts a title as on-topic
when it contains a whole-token match for a distinctive query token. `Gas South`, `Georgia Natural Gas`
and `Southwest Gas` literally contain the token `gas`, so they satisfy the test while naming a
different job. This is a **homonym collision**, and it is a *sub-mechanism* of a limitation this repo
had **already measured twice**, not a new discovery:

- `docs/research/2026-09-23-backup-niche-candidates.md` §2 recorded the guard returning its
  most-trusted label `ok` on a **30%** job-share shelf (`fence post spacing calculator`, via the
  generic shelf token `calculator`) and a **45%** one (`seating chart maker`, via `maker`), with the
  mechanism spelled out from the library's own code: `calculator` and `maker` are absent from
  `GENERIC_QUERY_TOKENS`, a query is `on-topic` when **each** distinctive token is echoed
  **somewhere**, and a card counts when it echoes **any** token.
- `docs/research/2026-09-23-visual-schedule-evidence.md` independently recorded the same
  under-counting for `visual`/`schedule` and concluded the guard "is not sufficient here".

**Hand-classification of every visible card was already the adopted discipline** as a result — so
the filtering in §2.2 is that discipline being applied, not a method invented here.

What this instance adds is the sub-mechanism: both earlier cases turn on a **generic shelf token**
(a common word like `calculator`, `maker`, `visual` that belongs to many unrelated jobs), whereas here
the query's *distinctive* token `gas` is present and correct in the title and the title still names
another job. That matters for the fix, because the obvious repair for the earlier cases — extend
`GENERIC_QUERY_TOKENS` — does nothing here: `gas` is load-bearing for a gas-piping tool, and
suppressing it would silence the very query being measured.

**First reading — recorded, then tested and partly overturned.** Taken alone, the polluted screen implied
the US pocket was *undefended but unproven*. §2.2 tests that by filtering the incumbents by hand, and the
result is that **US demand is proven — but never by the `gas` screen**, and the wedge is sharper than
the polluted verdict ever showed.

### 2.2 The US pocket, filtered by hand — demand proven, and a live monetisation window

The guard cannot filter for job relevance (§2.1), so it was done by hand — now with a tool, so the
next session can repeat it instead of re-implementing it. `node scripts/scan-titles.mjs` prints the
visible result set and splits every card into "on the job" (names piping/plumbing work) versus "not the
job" (names fuel retail or utility billing) by regex, so the classification is auditable rather than
asserted. It also contrasts the two shares side by side and **flags the failure mode automatically**:

```text
### gas pipe sizing
verdict=candidate  guard=on-topic (28/30, share 0.93)  visible=30
  -- NOT THE JOB (4) --
    Southwest Gas  [com.sew.swg]  3.8  Smart Energy Water
    Gas South  [com.gassouth.gs.mobile]  3.7  Gas South, LLC
    Georgia Natural Gas  [com.app.gng]  3.5  Georgia Natural Gas
    GasBuddy: Find &amp; Pay for Gas  [gbis.gbandroid]  2.9  PDI Software, Inc.
  hand job-share: 19/30 (0.63) vs guard on-topic  <-- GUARD SAID on-topic, JOB SHARE IS LOW
```

The guard's lexical confidence was **0.93** on that shelf while only **63%** of cards named piping
work — so this is not a marginal case the guard nearly caught; it was maximally confident and wrong.

**Demand in the US category is proven, on rated incumbents that are actually pipe tools:**

| App | Rating | Reviews | Installs |
|---|---:|---:|---|
| `com.calculated.josephine` — Pipe Trades Pro Calculator | **4.8★** | **1,590** | 50K+ |
| `ru.sotnikov.offsetpipe` — Offset Pipe calculator | 3.6★ | 888 | 100K+ |
| `com.pipeflowcalculations.android` — Pipe Diameter Calculator | 3.9★ | 311 | 100K+ |
| `com.pipe.fittings.kevin` — Pipe Fitter Calculator | 4.4★ | — | 10K+ |

That is 1,590 + 888 + 311 rated reviews on on-job apps. The 50K+ and 100K+ buckets are *floor*
comparisons, and none of this needed the word `gas` to be true.

**Three complaints, verbatim, that point at the same opening** (`scan-complaints --package`, US-accessible on-job apps):

1. **The 4.8★ leader's users are in revolt, and the complaint is the pricing model.** All three critical
   reviews of `com.calculated.josephine` are `paywall`:
   - *"Every year we bought the apprentices the app for their android phones… not anymore. It was easy
     and affordable at **$25 one-time fee for life**."*
   - *"Purchased the app as well as the subscription. Used it for a few days and then it says i dont have
     a subscirption."*

   A repeat institutional buyer has stopped, and the reason is a move from a one-time price to a
   subscription. This is a **dated migration in progress**, and it is the cheapest kind of opening there
   is: the incumbent is shedding goodwill it spent years accumulating.

2. **A union steamfitter says the maths is wrong.** `com.pipe.fittings.kevin` (4.4★):
   - *"Union steamfitter here: The multipliers on this app are flat out wrong. For a 45 offset you want to
     multiply the rise/run by 1.4142 (or 1.4/1.41/etc)."*

   A professional publicly disputing the numbers in the fourth-highest-rated app in the pocket. Accuracy
   is not decoration in this niche; it is the thing users audit.

3. **A user of the 100K+ app asks for exactly the standards-based job.** `com.pipeflowcalculations.android`:
   - *"We know\knew the length and pipe diameter. What I am looking for is to enter those and get the
     others **based on industry standards**"*
   - *"…about half of the calculations fields are locked so unless you need a 2\" pipe every time useless"*

   The requested feature *is* the standards-cited sizing job — and the incumbent locks half its inputs.

**The counter-evidence, which is real and must not be buried.** The pocket is also a **graveyard of
unrated attempts**:

| App | Installs | Reviews |
|---|---|---:|
| `com.plumbtools.gas_pipe_sizing` — Gas Pipe Sizing - NFPA 54 | 100+ | **0** |
| `com.plumbtools.water_pipe_sizing` — Water Pipe Sizing IPC 2024 | — | unrated |
| `com.plumbtools.pipe_offset` — Pipe Offset Calculator | — | unrated |
| `com.hvactools.pipe_sizing_calculator` — HVAC Hydronic Pipe Calculator | 100+ | **0** |
| `com.pipefitpro.app` — PipeFit Pro: Offset Calculator | 1K+ | **0** |
| `com.AK2DSTUDIOS.gaspipesizingtool` — Pipe Sizing Tool | 10+ | **0** |

A repeat run through the promoted tool surfaced **more of the same tail**, which is why the graveyard
reading is the safe one: `com.matescorpcalchub.pipegasflow` (Pipe Gas Flow),
`com.carmelsoft.pipesizergaslowlib` (HVAC Pipe Sizer - Gas Low, from Carmel Software Corporation),
`de.mta.mta_app` (Size the pipe - by MTA), `com.coned.cegaspiping` (Gas Piping Inspections, from Con
Edison), `org.kgsmithengr.gaspipelinecalculator` (Gas Pipeline Calculator), and the UK pair
`couk.gb_gas.GBPipe` / `couk.gb_gas.lpgGBPipe`. All are **unrated**. Search cards carry no install
data, so unlike the table above these are unrated *and* unmeasured in size — a browser read would be
needed to size them, and this document does not claim to have done that.

Two conclusions follow, and both change the plan:

- **Publishing here is easy; winning is not.** `com.plumbtools.*` is one developer already shipping the
  exact multi-calculator portfolio this document first proposed — gas sizing to NFPA 54, water sizing to
  IPC 2024, offsets — and all three are unrated. The play is available, and someone doing it has not
  gained traction. That is a warning against assuming a volume portfolio of calculators wins, and it is
  independent confirmation of the correction in §2.
- **The wedge is not "another pipe calculator".** It is: *the sizing tool that cites the edition of the
  standard it used, shows the correct multipliers rather than an opined one, leaves every input
  editable, and is bought once instead of rented.* Every clause is a complaint a named user wrote, and
  the last one is the reason a repeat buyer left the 4.8★ leader.

**Net effect on the decision:** the US/NFPA 54 choice **survives**, on better evidence than it was made
with. The gas-specific demand remains unproven by any method used here — what is proven is the US
pipe-tool category and the complaint set. The honest label for the pocket is **proven category, unproven
specific job, unowned specific job.**


### Candidate 2 — Rental inspection for the self-managing landlord (recommended second slot, different category)

**Demand is proven at professional scale.**

| App | Rating | Reviews | Installs |
|---|---:|---:|---|
| `yardi.Android.Inspections` — Yardi Inspection | **1.5★** | — | 10K+ |
| `com.radweb.pi` — Property Inspect | **2.1★** | 42 | 10K+ |
| `com.yardi.maintiq` — Maintenance IQ by Yardi | **2.3★** | 19 | 5K+ |
| `com.yardi.Android.WorkOrder` | **2.9★** | — | 100K+ |
| `co.happy.property` — Happy Property | 3.0★ | 109 | 10K+ |
| `nz.co.myrent.myInspections` | 4.5★ | 181 | 10K+ |
| `com.zinspector3` — zInspector 3 | 4.8★ | 269 | 10K+ |

**This pocket produced the single strongest signal in the run: 18 of 18 critical reviews.** Verbatim:

- *"app closes out unexpectedly while you are inspecting and then you have to let it reload again.
  it's very time consuming especially when you have 25 inspections"* (`com.yardi.maintiq`, 2.3★)
- *"I was finalizing an inspection with photos, the app LOST THE PHOTOS in this process"*
  (`yardi.Android.Inspections`, 1.5★)
- *"just all around bad, for something that's supposed to save time it takes forever … just typing a
  unit number takes forever"* (`co.happy.property`, 3.0★)
- *"No support for this app whatsoever"* · *"Report upload failed due to validation error — but then
  no mention of what or where the error is? That is useless"*

Complaint profile: missing-feature 28%, data-loss 17%, performance 17%, crash 11%, abandoned 11%.

**Secondary corroboration (LEAD, not a first-party measurement).** A web search surfaced App Store
and aggregator reviews for the same vendor making the same claims — crashes mid-inspection, lost
photos/notes/timestamps, forced re-sign-in, submission taking five or six attempts — and one
aggregated comment that *"property managers have resorted to using third-party alternatives"*.
**These came from a search summary, not from my own fetch**, so they are recorded as a lead that
raises confidence and not as evidence this document rests on. Making them first-party is a
one-command follow-up, not a new project.

**Distinctness sentence:** *an inspection app for the landlord who manages a few units themselves —
works with no account and no login, never loses a photo, and produces the finished report on the
device.*

**Factory fit:** moderate. Local-first checklist + photo capture + PDF export is achievable without a
server, but it needs camera/photo-picker permissions, file export, and careful backup rules.

**Risks, stated not softened:**

- **The market is smaller than Candidate 1.** Every independent incumbent sits at the 10K+ bucket;
  the 100K+ entries are enterprise apps whose users do not choose them.
- **The enterprise buyer is not reachable.** Yardi's users cannot switch — their employer chose it.
  The reachable user is the *self-managing landlord*, a different job from the one the complaints
  come from. That is a real caution: the loudest pain in the pocket is not necessarily the reachable
  customer.
- **B2B expectations** (sync, team, cloud) pull against the factory's no-backend shape; a
  subscription is the natural model and subscriptions are deferred (§3.3).
- **UNVERIFIED:** that a solo landlord pays for this rather than using paper or a spreadsheet.

---

## 3. Hard kills, with the reason

| Idea | Killed by |
|---|---|
| **Handloading / reloading load data** | **Play policy.** Play Console Help, *Dangerous Products*: *"We don't allow apps that provide instructions for the manufacture of explosives, firearms, ammunition, restricted firearm accessories, or other weapons."* Reloading data is instructions for manufacturing ammunition. The pocket had the best commercial shape of anything measured (Hornady 3.3★/1,840/100K+, Hodgdon 2.6★/349/50K+, Vihtavuori 3.9★/462/100K+, all corporate publishers) **and complaints that name the fix** (*"I paid for this and now it doesn't work anymore because you have a new edition"* · *"requires clunky sign in every time"* · *"it still displays all metric"* · *"The data should all be baked into the app, not online"*). None of that matters: an enforcement action is a strike, and strikes escalate to account termination, which removes **every** app in the catalogue. Incumbents existing on Play shows enforcement is inconsistent, not that the risk is acceptable. — <https://support.google.com/googleplay/android-developer/answer/9878810> |
| `process server log` | **Polluted shelf** (`5/30` titles carry a distinctive token). The enriched "leaders" were `com.apptiva.logviewer` (1M+, a *log-file viewer*) and `com.tortel.syslog` (100K+). The `candidate` verdict describes a different job. |
| `dog groomer appointment book` | **Polluted** (`3/11`) **and** a fortress underneath: MoeGo 4.3★/309, TapGroom 4.1★/390, DaySmart 3.3★/413, all 10K+. |
| `field service work order app` | **Fortress.** Jobber 1M+/4.4★/6,780, Housecall Pro 500K+/4.5★/6,790, Salesforce 500K+. No pocket. |
| `stuttering practice app` | **Polluted** (`10/30`) **and** owned: Stamurai 100K+/4.6★/6,860. |
| `dementia caregiver log` | **Thin, not weak.** Largest enriched incumbent is **5K+**, six of eight unrated. A `candidate` label over a field nobody has reached is the "people tried and nobody got traction" shape, not an opening. |
| `farrier hoof record` | **Polluted** (`7/29`) **and** thin — the on-job apps are at 1+, 10+, 50+, 1K+. |
| `livestock breeding record` | The 1M+ app in the shelf is **`Animal Breeding`, a game** with 4,110 ratings. The on-job field tops out at 10K+ with weak ratings. Confounded, not promising. |
| `machinist feeds and speeds` | Real field, but `FSWizard` holds 4.7★/850/100K+; the weak entries are corporate tool-maker apps. No reachable pocket. |
| `safety inspection checklist`, `forklift inspection checklist` | Real weak pockets, but the strong generalists (`checkbuster_v3` 4.4★/379/100K+, `com.inetsgi.safetyreports` 4.6★/255/50K+) serve the job generically; these are enterprise-flavoured and need a backend. Kept as backups, not recommendations. |
| Everything in `collection / life-admin / food craft / outdoors` groups | All `mixed`, `dead-zone` or fortress. `birding life list` scored as a `candidate` **while flagged POLLUTED**, which is exactly the trap the previous session documented — not treated as a candidate. |
| `coin collection catalog` — recon 2026-10-01 | **Refused by the owner, 2026-10-01.** Recorded so the same recipe is not re-proposed: the topic was selected by treating the `docs/niche-selection.md` §4 supply heuristic — a signal for *where to look* — as a requirement an idea must pass, and by applying it to a **hobby rather than a job** (the sentence is about a person doing work; hobbyists build for their own hobby). The proposal's premise was distinctness, not earnings. **The shelf being occupied is NOT the reason and must not be cited as one** (`.cursor/rules/ideation-gate.mdc` § "The objective is revenue, not novelty"): what is refused is this recipe and its reasoning, not the shelf's capacity to pay. |
| `pilot logbook` — recon 2026-10-01 | **Refused by the owner, 2026-10-01**, for the same reversal. The shelf is served by pilots themselves and by flight-school software vendors, which is the opposite of the supply phenomenon §4 describes, and the proposal argued novelty instead of revenue. Same caveat: the objection is to the recipe's reasoning, not to the market. |

---

## 4. What this run changed in the factory

1. **New sensor: `scripts/lib/play-reviews.mjs`** — reads the review text Play server-renders
   (`data-g-id="reviews"`, card `Jx4nYe`, per-review `iRFPc` rating anchor, body `h3YV2d`). It
   deliberately **never extracts a reviewer's name**, has no field for one, and that is asserted.
2. **New CLI: `scripts/scan-complaints.mjs`** — screens a query, picks the weakest rated incumbents,
   reads their reviews, and prints a complaint histogram **plus every verbatim text it counted**.
3. **New tests, and they were proven able to fail.**
   - `scripts/test-play-reviews.mjs` — 39 offline checks. A temporary defect was introduced
     (splitting on the rating anchor instead of the review card) and **9 checks failed**, showing 8
     "reviews" built from the star-histogram rows; the defect was then reverted.
   - `scripts/verify-reviews-sensor.mjs` — 15 live checks. **C2 proves the trap is real on live
     pages**: Spotify's reviews section carries **15** `aria-label="Rated…"` anchors for **3**
     reviews, so any implementation that counts anchors is wrong by 5×.
4. **The complaint taxonomy was revised against the first real corpus, and the revision is measured.**
   Seeded before any data, then run over 59 critical reviews. **22 of 59 (37%) were unclassified** —
   the histogram was close to useless. Three classes were missing entirely (`units`, `regression`,
   `language`) and several patterns could not match real phrasing (`\blog ?in\b` cannot match
   "log into"). After the revision: **8 of 59 (14%)**. The before/after numbers, the exact review
   texts that exposed each gap, and the revision table all live in `scripts/lib/play-reviews.mjs`
   itself, so they cannot drift away from the code.
5. **`scan-market.mjs --query-set <path>`** was added, and a **restated path was removed**: the
   report header and the JSON payload both hard-coded `scripts/market-queries.json`, so a run
   against any other set would have named a file it never read. Both now derive from the path
   actually loaded.
6. **One known false positive is pinned in a test, not hidden:** a lexical classifier cannot tell
   *"no ads, love it"* from an anti-ads complaint. The test asserts the false positive exists so it
   is visible to whoever reads the ads count.
7. **`scan-complaints.mjs --package` was exercised for the first time** on the packages that decide
   the gas question, and it works: `com.plumbtools.gas_pipe_sizing` (100+, 0 reviews) and
   `com.mrcombi.pipesizing` (2.0★, 5K+, 3 critical, top class `units`). The earlier sessions had
   written that flag without ever running it.
8. **A guard gap was measured and reported, not patched (§2.1).** The relevance guard returns `ok`
   for a shelf of gas-utility billing apps, because `gas` is a whole-token match in their titles.
   **This is the third measurement of a known limitation, not a discovery** — the two earlier
   measurements and the hand-classification discipline that came out of them are cited in §2.1. The
   guard's own module header reserves input-contract changes as a human decision, so it was left
   alone; the new `homonym` sub-mechanism is recorded in `harness/lessons.md`.
9. **New CLI: `scripts/scan-titles.mjs` — print the visible titles so a human can classify them
   (§2.2).** It applies the discipline the 2026-09-23 research had already established, splits cards
   on-job versus off-job by regex, and **flags the guard failure automatically** when the guard says
   `on-topic` but the hand job-share is below 0.65 — which is how the §2.1 defect becomes visible in
   one command instead of a bespoke probe. It is versioned in `scripts/` rather than left in
   gitignored `.kiln/`, because the need has now recurred three times and a handoff instruction that
   points at an unpushed file is not an instruction. It has **no offline unit test**: it is a
   diagnostic printer whose output a human reads, its error paths (missing `--query`, invalid regex,
   bad `--concurrency`) were each exercised and exit non-zero, and the defect it detects is the one
   deliberately left unfixed in §2.1 — see the reasoning in `harness/lessons.md` for why a check
   pinning known-wrong behaviour is not a check.

---

## 5. What this does not establish

- **No revenue, retention or conversion.** The store page exposes a badge, not a P&L. A declared IAP
  proves a model was implemented, never that it earns.
- **Install counts are Play's rounded buckets.** `100K+` spans 100,000–999,999; every size
  comparison in this document is a floor comparison.
- **The review sample is Play's choice, three per page.** The complaint histograms describe the
  *vocabulary* of a pocket, not complaint frequency in its population. The classifier is lexical;
  the verbatim texts are printed so a human can audit every number.
- **Only the first 8 cards per query were enriched.** A larger incumbent can sit below that cut, so
  "the largest enriched app is X" is not "X is the largest app in the niche".
- **Both candidate sentences are hypotheses from complaint text.** They have not been tested against
  a shipped product, and the failure mode this factory already paid for once
  (`add-visual-schedule-slot`) is a persuasive distinctness sentence that a direct comparison does
  not support.
- **The off-Play corroboration for Candidate 2 is a search-derived lead**, not a fetch.

---

## 6. Decision recorded, and what remains open

### Decided by the owner on 2026-09-24

| Decision | Value |
|---|---|
| Next slot | **Pipe sizing for the trades** (Candidate 1) |
| Code jurisdiction | **United States** — NFPA 54 for fuel gas, plus the US codes the same job touches (IPC for water sizing, NFPA 13 for sprinkler pipe). The UK branch is dropped. |
| **Anchor job** | **Standards-cited pipe sizing, not gas-only.** Chosen because the proven demand is the *category* (1,590 + 888 + 311 rated reviews on on-job apps, §2.2), while gas-to-NFPA-54 alone is the one part that is unproven. NFPA 54 fuel gas is the first code table, not the whole product. |
| Archetype | **Not a commitment.** The factory builds a full range of apps; this slot commits it to nothing. The correction is recorded here because the first draft of this document wrongly framed the slot as locking the factory into trade calculators — see the Repetitive Content row in §2. |
| Build now? | **No.** No slot is created by this document. |

Candidate 2 (rental inspection) stays on the bench as the next slot, deliberately in a different
category.

### Bench survivors this run did not evaluate — the field is not binary

This document screened its own new query set; it did **not** re-evaluate pockets earlier research had
already left standing. Recording them here so the table above is not read as a complete field — each
is a **different category** from pipe sizing, which is the thing that matters for the Repetitive
Content constraint:

| Prior survivor | Evidence already on record | Recorded risk | Source |
|---|---|---|---|
| **Recipe portion / scaling calculator** | Called *"the most stable pocket in this document"* — two different phrasings return the same small shelf, `mixed`, with a weak incumbent at **100K+ on 3.4★** (`Calculation of recipes`), a `$1.99` paid incumbent at 4.2★/10K+, and ~20 unrated micro-apps including two at **1+ installs**. The job is pure arithmetic. | Absorption by a free web calculator | `docs/research/2026-09-23-backup-niche-candidates.md` §4.9 |
| **Packing list organizer** | *"Cleanest evidence, commodity risk"*: 30/30 on-job relevance, verified `candidate`, reachable 3.7–4.0★ weak tier; `PackPoint` at 1M+/4.4★ above it | Willingness to pay over ~30 free clones | `…backup-niche-candidates.md` §4.11 |
| **Tailor / sewing measurement records** | 28/28 on-job relevance, best retention shape, cheapest decisive test | Verdict flips `candidate` ↔ `dead-zone` on phrasing, so **not a GO** | `…backup-niche-candidates.md` §4.13 |

The recipe pocket is the one worth a second look if the pipe-sizing slot stalls: it is arithmetic, has
no server or ML requirement, and its 100K+/3.4★ weak incumbent is the same *shape* of opening that
made pipe sizing attractive.

### One factory-level gate that affects time-to-revenue, not the choice

`docs/monetization-strategy.md` §1.1: personal Play Console accounts created **after 2023-11-13** must
run a closed test with **≥12 testers opted in continuously for 14 days** before production access,
adding ~7 days of review — so each app costs ≥14 days before it can earn anything, and the correct
response would be to **batch** app creation rather than serialize it. **This almost certainly does not
apply to us**, because `AGENTS.md` records that `com.gexetr` already has published apps and production
access was therefore already granted — but that is an *inference from the namespace rule*, not a
Console reading, and the document flags it as unconfirmed. It costs one Console check to settle, and it
decides whether a second slot this quarter is serialized behind a 14-day window.


Rationale for Candidate 1 over Candidate 2: the larger proven market (100K+ incumbents with 300–900
reviews), the cheapest build the factory can do at all (no permissions, no camera, no server, no
ML), the archetype with the strongest existing commercial evidence, and a distinctness sentence
where every clause answers a complaint a real user wrote down. Candidate 2 has the more intense pain
but a smaller market, a heavier build, and its loudest pain belongs to users who cannot switch.

> **Corrected 2026-10-02 — the "no camera" clause is a dated reading, not the factory's position.**
> The measurement above is kept as written, because it is the record of why Candidate 1 was chosen.
> What changed on 2026-10-02 is what it is read as implying. The owner opened the phone's free, local
> capabilities, in his own words: *«есть камеры, есть фотопикер, есть галерея, картинки, музыка.
> Использовать телефон на полную, а мы используем только пару кнопок»*. The costs to avoid are now
> **a server, an account, a paid API, ML that needs a server, and a runtime permission that buys
> nothing**; a capability that is local and free — the Photo Picker
> (`ActivityResultContracts.PickVisualMedia`, which asks for **no runtime permission at all**), the
> gallery, the user's own media, the motion sensors, the camera — is not a cost and may be the job's
> own core. "No camera" stays a legitimate answer for a job with no photograph in it; it is no longer
> a factory-level refusal. Rule: `.cursor/rules/ideation-gate.mdc` § "Build cost is information, never
> a criterion".

**Prerequisite, not a decision:** the environment blockers in §1.2 must be repaired before any slot
can be built or verified. Market selection can continue in parallel; slot work cannot start while
`kiln-status.mjs` reports `NOT READY`.

### Open, and each has a named next measurement

1. **US demand: measured and resolved.** §2.1 found the verdict that first suggested US demand was an
   artifact of homonym pollution; §2.2 then filtered the incumbents by hand and found the demand proven
   on on-job apps — `com.calculated.josephine` (4.8★ / 1,590 / 50K+), `ru.sotnikov.offsetpipe`
   (3.6★ / 888 / 100K+), `com.pipeflowcalculations.android` (3.9★ / 311 / 100K+). The anchor job was
   then set to the proven category rather than the unproven single standard. **What remains open is
   scope, not demand:** how many code tables ship in the first release. NFPA 54 fuel gas is the anchor;
   IPC water sizing and NFPA 13 sprinkler pipe are the same job and are visibly served by dormant apps
   (`com.plumbtools.water_pipe_sizing` is unrated, `com.firetools.pipe_schedule_sprinkler` is unrated),
   but every table added is correctness surface that needs a sourced edition and a checker.
2. **The relevance guard is blind to homonym collisions (§2.1).** Measured, not suspected: `ok` on a
   shelf of utility-billing apps. A fix is a change to the verdict contract's inputs, which the
   module header reserves as a human decision, so nothing was changed silently. Recorded in
   `harness/lessons.md` with the proposed direction.
3. **Correctness source.** Which published NFPA 54 edition to cite, and who checks the formula table
   against it. For a gas tool this is a safety gate, not a nicety.
4. **Monetization.** One-time unlock is now evidence-backed, not just consistent with the recorded
   commercial history: the 4.8★ leader's critical reviews are *entirely* about moving away from a
   "$25 one-time fee for life" (§2.2), and a repeat institutional buyer has already left over it. Ads-led
   plus an unlock is the alternative; no subscriptions under the current deferral.
5. **Whether Candidate 2's off-Play corroboration should be made first-party** before it competes for
   a slot — one `scan-complaints --package` run against the Yardi packages. **The `--package` path is
   now verified to work**, exercised in this run against `com.plumbtools.gas_pipe_sizing` and
   `com.mrcombi.pipesizing`.

---

## 7. Handoff — how to reproduce this run and where to start

Written for the next session. Read §1.2 before running anything: **the factory cannot currently pass
its own gate, and that is not this run's doing.**

### 7.1 Exact commands

```bash
# Unit tests for the new sensor — offline, ~1s, 39 checks. Start here.
node scripts/test-play-reviews.mjs

# Live validation of the same sensor against Play pages (15 checks, incl. the histogram trap).
node scripts/verify-reviews-sensor.mjs

# The broad screen behind §1 and §3 (66 queries, ~8s).
node scripts/scan-market.mjs --query-set scripts/market-queries-2026-09-24.json --concurrency 4

# Read the weakest incumbents' complaints for one pocket.
node scripts/scan-complaints.mjs --query "gas pipe sizing" --top 6 --concurrency 2

# Read specific packages directly (this is the path that decided §2.2).
node scripts/scan-complaints.mjs --package com.calculated.josephine --package com.pipeflowcalculations.android

# The hand-classification tool behind §2.2 — prints visible titles split by job.
# It flags the guard failure automatically when the guard says on-topic but the job share is low.
node scripts/scan-titles.mjs --query "gas pipe sizing" --query "nfpa 54 gas pipe sizing" \
  --on-job "\b(pipe|piping|plumb|sizing|diameter)\b" \
  --off-job "\b(gasbuddy|gas south|southwest gas|natural gas|utility|bill|fuel|price)\b"
```

### 7.2 Artifacts (all gitignored, cited in this document rather than copied)

| Path | What it holds |
|---|---|
| `.kiln/deep/scan-2026-09-24.md` | The 66-query screen |
| `.kiln/deep/enrich-2026-09-24.md` | Per-app installs, badges, price |
| `.kiln/deep/complaints-2026-09-24.md` | Complaints **before** the taxonomy revision |
| `.kiln/deep/complaints-2026-09-24-after.md` | Complaints **after** the taxonomy revision (37% → 14% unclassified) |
| `.kiln/probe/queries-gas-us.json` | Probe set for the homonym measurement (§2.1) |

The hand-classification tool started as `.kiln/probe/pipe-us-titles.mjs` and has been **promoted to
`scripts/scan-titles.mjs`** so this handoff is executable after a clone — `.kiln/` is gitignored and
would not have travelled.

### 7.3 The blocker to clear first, and how to prove it is clear

`node scripts/kiln-status.mjs` returns `VERDICT: NOT READY` with four missing items — **JDK 17+**
(`PLAYKILN_JAVA_HOME`/`JAVA_HOME`), **`ANDROID_HOME`**, **Pillow**, and **keytool** (which arrives with
the JDK).

The `FAIL` in `verify-slot.mjs` and `selftest.mjs` is a **symptom of the Pillow gap, not a code defect**:
`scripts/store-kit.py:25` does `from PIL import Image, ImageDraw, ImageFont`, so without Pillow the
sandbox's `store-kit --render-only` exits non-zero, `capabilities.json` is never bootstrapped, and
`selftest.mjs:669` then dies reading a `store/listing.json` that was never written.

**Verified pre-existing, twice, in separate clean `git worktree`s at `HEAD` (`578ec72`) where none of
this session's files exist** — identical two `FAIL` lines and an identical `ENOENT`.

```bash
python -m pip install pillow      # clears the selftest failure
```

**Do not assume that fixed it — re-run `node scripts/selftest.mjs` and read the output.** Per
`harness/lessons.md`, an unverified "should be fixed now" is exactly the failure mode this repo has
already paid for.

### 7.4 What the next session should NOT redo

- **Do not re-screen these** — they are killed with reasons in §3: `process server log`,
  `dog groomer appointment book`, `field service work order app`, `stuttering practice app`,
  `dementia caregiver log`, `farrier hoof record`, `livestock breeding record`,
  `machinist feeds and speeds`, and the whole `collection / life-admin / food craft / outdoors` groups.
- **Do not re-litigate the plant-health or visual-schedule slots** — both are dead by their own
  recorded gates (`openspec/changes/`), and the plant vertical's `Photo-based crop diagnosis` was
  rejected on measured field accuracy (~31% real-world vs 99.35% lab), not on taste.
- **Do not "fix" the relevance guard by adding `gas` to `GENERIC_QUERY_TOKENS`** — §2.1 and
  `harness/lessons.md` explain why that silences the very query it would be meant to protect.
- **Do not report `seen` as a competitor count** and do not treat an unrated incumbent as vacancy —
  §2.2's graveyard (`com.plumbtools.*`, all unrated) is the counter-example.

### 7.5 The one open measurement that would change the plan

The anchor job is set to the **proven category** (§6), precisely because gas-to-NFPA-54 alone is
unproven by anything measured here. If someone wants to settle that specific job, the measurement is a
hand-classified read of `nfpa 54 gas pipe sizing` and `black iron pipe sizing` for pockets whose
incumbents are *actually* pipe tools — **not** another rating-only screen, which §2.1 shows cannot
answer it.


