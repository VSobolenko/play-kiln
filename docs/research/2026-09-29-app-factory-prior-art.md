# AI app factories — prior art, and the problems already paid for

> **Question asked (2026-09-29):** an app factory — one idea → one command → a publishable
> Android release — is the thing PlayKiln is built to be. Are we first? Has someone already
> done this? What is good in their version, and which of their problems are we about to pay
> for again?

**Scope:** external prior art only. This document does not re-open any market question, and
it proposes no slot; it is input to the harness, not to ideation. Every claim carries a label:
**MEASURED** (someone published the number), **ON RECORD** (a named source states it, not
independently reproduced here), **INFERRED** (my reading of the evidence), **UNVERIFIED** (I
could not check it). Where a page could not be read directly, the label says so.

**Method:** web search plus direct fetches, 2026-09-29. Two fetches failed outright
(`github.com/Ice-Citron/New-Dejima` returned a near-empty page; the mirror of
*"What I Learned Shipping 90+ Mobile Apps with AI Coding Agents"* returned *"Site not
available"*), so those two are **ON RECORD via the search index**, not read from source.

---

## 0. Verdict

1. **We are not first at any single component.** Prompt → native Kotlin/Compose is Google's
   own feature. Play Console automation is a mature unofficial MCP category. Agent-discipline
   frameworks are a 2026 research topic with a named taxonomy. Running a *portfolio* of apps
   as one machine is a funded business model with a $1.2 B example.
2. **We are apparently first at the whole chain, and that is a weak claim.** The chain —
   money gate → promise contract → build → owner approval bound to a source digest → signed
   artifacts → store pack with provenance → submission walked by an agent — has no published
   equivalent. The nearest thing is a Paris studio that *claims* it, founded in 2026, with
   three people and 34 followers.
3. **The prior art's own answer to "why bother" is not the pipeline. It is the repeatability
   of the money loop.** HubX was funded at a $1.2 B pre-money valuation explicitly on "the
   repeatability of their model" with 370 employees and 40+ products; Bending Spoons buys
   product-market fit instead of searching for it. Nobody in this space is valued for
   generating code.
4. **Somebody already wrote down our failure mode, in their own repo, in one sentence:**
   *"These are projections, not measured revenue. We have not published apps to the Play Store
   yet."* — a 7-phase autonomous Android app factory with 12 built apps and zero revenue
   (**ON RECORD**).

---

## 0.5 If you are the agent sent to improve the factory, read this first

This document is **input, not authorisation**. Nothing below is a decision, and none of it has
been implemented. The owner approves; you change. Two rules for using it:

- **Verify before you build.** Two of these are readings, not measurements (§6.1, §6.2). Settle
  them by opening the file in question, not by writing code.
- **Do not turn §2 into a batch of new scripts.** The prior art's own lesson is that discipline
  enforced by code the agent cannot edit beats discipline enforced by prompts — and that most
  of these items are *checks that already have a place to live*, not new subsystems.

### Start with these two, before anything else — they are readings, not work

| Check | Question | Where | §  |
|---|---|---|---|
| **A1** | Does the approval digest cover the **gate scripts themselves** (`scripts/**`, `store/studio.json`, `.cursor/rules/**`)? If not, the agent that wrote the app can weaken the check that judges it *after* approval — the exact failure Consort's taxonomy predicts. | `scripts/lib/release-approval.mjs` (input list) | §6.1 |
| **A2** | How many human steps does a first publication really have? `AGENTS.md` says two; `play-upload.mjs`'s own header and `play-publishing-playbook.md` §7 imply more (content rating, ads and target-audience declarations), and the script already uploads all five graphic fields. Fix whichever document is wrong. | `AGENTS.md` stage 10 · `docs/play-publishing-playbook.md` §7 · `scripts/play-upload.mjs` | §6.2 |

### The change list, ordered by value per hour

| # | Change | Why — the evidence | Lands in | Cost | § |
|---|---|---|---|---|---|
| **B1** | **A new rule: no shipped slot calls a paid API from the client.** Plus a check that fails when a key-shaped literal or a provider SDK key reaches `app/src/main`. | 66.3 % of 1,207 LLM-enabled Android apps have ≥1 LLM-API misuse; 41 % leak API credentials; secrets found in 35 % of 50,000 Play apps (1,802 verified live); ~1 in 4 AI-generated samples carry an OWASP vuln. **We pass this defect today.** | new rule + a check in `verify-slot.mjs` / `selftest.mjs` | **highest value in this document** | §3.8 |
| **B2** | **Holdout scenarios the builder never sees**, authored at stage 0.6 beside `differentiators.json`, run by the verifier at stage 4. | StrongDM / dark factory: 3 runs, 2-of-3, ≥90 % gate, generator gets a one-line failure and never the scenario text. Our proof of a promise is written by the builder who made it. | `docs/ideation/<slot>.holdout.md` + `verify-slot.mjs` | low | §2.1 |
| **B3** | **Record an outcome on every approval, and compute an override rate.** `approved_clean` / `approved_after_repair` / `revoked`, surfaced in `kiln-status.mjs`. | The published promotion gate: enable autonomy only after 20 runs with pass >90 %, false-positive <5 %, **human override <10 %**. We have no number relating "gates green" to "owner satisfied". | `scripts/approve-release.mjs`, `scripts/kiln-status.mjs` | low | §2.2 |
| **B4** | **A kill rule that fires.** List slots past their decision date with no recorded verdict; add a `--kill <slot> --reason` path that archives. | Every portfolio source is unanimous: the kill rule is what makes a portfolio a system. `killCriteria` exist and are validated, but nothing evaluates them and no slot has ever been killed — our own post-mortem already called this out. | `scripts/kiln-status.mjs`, an archive path | low | §2.6 |
| **B5** | **One head term per slot**, registered, with a collision check at pack time. | "Every app should own one head term. No other app in your portfolio should target that term in any metadata field." Two own apps splitting 100 searches 60/40 rank #4 and #6. It is also machine-readable evidence in a Repetitive Content investigation. | `registry/head-terms.json` + `store-kit.py` | medium | §2.5 |
| **B6** | **A portfolio snapshot and an emergency rollout stop.** | StorePilot's `portfolio_overview` (all apps, both stores, one call) and `play_halt_rollout` — "stop a rollout immediately, no confirmation step". We have neither; a bad rollout has no documented stop. | `scripts/play-upload.mjs` (`--halt`), `kiln-status.mjs` | low | §2.7 |
| **B7** | **A per-slot platform-deadline register.** | "remembering which of ninety apps still uses a deprecated permission"; "skipping step eleven because you're tired". Survivable at two slots, not at ten. | `registry/apps.json` / `kiln-status.mjs` | medium | §3.2 |
| **B8** | **Make gate failures speak in one line.** | The dark factory gives the generator one line and no scenario text; our own `AGENTS.md` already applies that insight to the worker ledger. Apply it to `verify-slot.mjs`. | `scripts/verify-slot.mjs` | low | §2.4 |
| **B9** | **A decision about the single-account blast radius.** Record the conflict rather than discovering it: all slots ship under `com.gexetr`; Google's own white-label guidance says decentralise, and one account with 2,500 apps was terminated by a forced 30-day sweep. | **ON RECORD**, Play Console Help | `AGENTS.md` / `docs/` | low (a decision, not code) | §3.6 |

**Read §3 before changing anything in B1, B5 or B9**: those three map to live Play policy, and
the factory's per-slot identity rule (`visual-identity.mdc`) is account survival, not taste.

---

## 1. What already exists — five shapes

| Shape | Who | What they actually have | Same as us? |
|---|---|---|---|
| **Prompt-to-app SaaS** | a0.dev, Rork, Rocket.new, FlutterFlow, Draftbit, Bravo, Replit Agent, CatDoes, Lovable/Bolt | one prompt → React Native / Flutter / Swift project; some publish to both stores | No — a **tool**, the human still does the per-app work |
| **Agent-operated app studios** | Artenas, New-Dejima, HubX, Rocapine | portfolio logic + agents across build, publish, marketing; kill early, scale what works | **Yes, this is us**, at company scale |
| **Mature portfolio operator** | Bending Spoons | one shared stack (50+ services) running many acquired apps on lean dense teams | Partly — same *machine*, opposite sourcing (buys PMF) |
| **First-party platform tooling** | Google AI Studio, Android Studio, Gemini/Agent Mode | prompt → real Compose project → browser emulator → ADB install → **Play internal testing track, app record created automatically** | Commoditises our build+first-upload half |
| **Store-ops automation** | Android Publisher API + unofficial MCP servers (StorePilot, google-play-developer-mcp, playstore-release-mcp) | upload AAB, tracks, rollout %, listing copy, **graphics**, reviews, portfolio overview | Commoditises our release half |

### 1.1 Prompt-to-app SaaS (**ON RECORD** — ProductOS, VP0, Rork, CatDoes, 2026)

- **a0.dev** (Y Combinator W25), ~$20/mo: React Native/Expo, "one-click publishing to both
  the App Store and Google Play".
- **Rork** (a16z), from ~$20–25/mo plus **Rork Max ~$200/mo**: Rork Max generates real
  Swift/SwiftUI on a cloud Mac fleet; Apple-only, no Android.
- **CatDoes** claims the full end-to-end: subagents for design and code, real-device testing
  via QR, then AAB + signing + "OAuth-based submission to your Play Console". "Most other
  prompt-first builders stop at the AAB."
- **Lovable** is web-only; a Lovable-generated listing already turns up in Play search results
  in our own scan data (`app.lovable.cea97572b9aa46cfa6a028b683731f53`, **MEASURED**, in
  `docs/market-enrichment-2026-09-23.md` §4).

**Reading:** none of these is a factory. They sell velocity to a human who still chooses the
app, writes the listing, and answers the console. But they are the *reason* the store is
flooded, and they are the supply side of every number in §5.

### 1.2 Agent-operated app studios — the closest prior art

- **Artenas** (Paris, founded 2026, 3 employees, 34 LinkedIn followers, **ON RECORD**):
  *"the first end-to-end AI-Native App Studio & Publisher… an army of interconnected,
  self-improving AI agents across the full app lifecycle: development, optimization &
  publishing, and marketing & user acquisition… every app makes the next one smarter… run a
  portfolio logic that de-risks everything: iterate rapidly, kill early, scale what works."*
  That is PlayKiln's thesis, written by someone else, as a company pitch.
- **New-Dejima** (`github.com/Ice-Citron/New-Dejima`, 1 star, **ON RECORD via search index**,
  direct fetch returned a near-empty page): a 7-phase pipeline — scaffold → Claude Opus writes
  all Kotlin/Compose → `./gradlew assembleDebug` with up to 10 retries → APK verification
  (exists, >100 KB, package name) → ADB install on a Samsung Galaxy A16 → Gemini Flash
  screenshots and analyses the UI → JSON report. Claims 12 apps built, and states plainly that
  the revenue is a projection and nothing has been published.
- **HubX** (Izmir, 2022, **ON RECORD**): 40+ consumer products, 600 M users, 370 employees,
  profitable before raising; $75 M from Point72 Private Investments at $1.2 B pre-money. The
  stated investment thesis: **"the repeatability of their model"**.
- **Rocapine** ($13 M Series A led by Educapital, **ON RECORD**): AI-first *wellness* studio
  and publisher; an in-house stack (RocaStudio build, RocAI content, RocaBoost monetisation
  and marketing); **100+ concepts tested in under a year**, portfolio average rating >4.5;
  described as "the Voodoo of wellness". Their edge is stated as content velocity, not code.

**Reading:** the studios that got money are the ones whose *loop* repeats — content, launch,
monetisation, kill/scale. The one that published its pipeline as code built 12 apps and has no
revenue. Both facts should be uncomfortable.

### 1.3 The mature version of the same machine (**ON RECORD** — Acquiry, Sacra, TBPN, F-1)

Bending Spoons runs many apps on **one shared stack** ("Spoon Engine", 50+ services: auth,
payments, A/B, analytics, AI inference), with small high-talent teams, fluid redeployment of
people between products, and rigorous experimentation. Structurally the closest thing to a
factory that already works at scale:

- it **buys** product-market fit rather than searching for it: **6 closes from 2,500+ sourced
  opportunities ≈ 0.24 %**;
- M&A team ~8–9 people; ~95 % of staff are engineers/designers/growth/AI;
- it deliberately **refuses** cross-portfolio bundling (branding, cross-sell) — estimated
  ~5 % incremental revenue for a large cultural cost — and runs each unit with startup-level
  ownership, the shared platform delivered invisibly;
- it prefers **fewer, larger** deals because transformation effort does not scale linearly
  with revenue.

**Reading for PlayKiln:** "many apps, one machine" is proven — but the proven version
**centralises infrastructure, not identity**. That is the exact opposite of our per-slot
identity rule, and both are defensible for different reasons: they centralise to cut cost,
we differentiate to survive the Repetitive Content policy (§3).

### 1.4 Google is commoditising the half we are proudest of (**ON RECORD**)

**Google AI Studio → Android** (Build mode, Antigravity agent) generates a complete Kotlin +
Jetpack Compose project from a prompt, previews it in a browser emulator, installs it on a
physical device over ADB, and **publishes it to the Play Console internal testing track,
creating the app record automatically**. Documented constraints: client-side only (no Firebase,
no server runtime, no secret management), single-activity, single-module, Compose-only.
Production is explicitly out of scope; you export a ZIP and do it yourself. Android Studio adds
"Publish for Testing" from the signed-bundle flow; Gemini/Agent Mode with Firebase agent skills
sits alongside.

**Reading:** "an agent writes a native Android app and gets it into Play" is a free Google
feature in 2026. The defensible part of PlayKiln is therefore **not** code generation. It is
the part Google has no incentive to build: knowing *which* app to build, refusing to build
without a money plan, keeping the promise list honest, proving the pack matches the build, and
keeping the account alive.

### 1.5 Store-ops automation is a solved problem we re-solved (**ON RECORD**)

There is still **no official Play MCP**, which matches
`docs/research/2026-09-23-google-play-automation-surface.md`. But the unofficial surface is
rich:

- `OyaAIProd/google-play-developer-mcp` — 150 tools over Android Publisher API v3 and Play
  Developer Reporting API v1beta1, using only current endpoints
  (`monetization.onetimeproducts`, `purchases.subscriptionsv2`, 2026 fields like
  `offerPhase`, `cancel`/`defer`, price step-up consent).
- `Jeronimo0228/StorePilot` — an *orchestration* layer over the raw API:
  `get_release_snapshot` → `explain_release_blockers` → `execute_release_intent`, `dryRun: true`
  by default, `confirm: true` on destructive writes, a multi-app project registry
  (`storepilot.yaml` + `.storepilot/memory.json`), and EAS/GitHub webhooks for CI.
- `rabdulsal/playstore-release-mcp` — `play_doctor`, `play_upload_bundle`,
  `play_release_to_track`, `play_update_listing`, **`play_upload_graphics`**, `play_list_reviews`,
  `play_reply_review`.  It states the same fact our own research does: a Gradle-built `.aab`
  arrives signed, so the API can do the *entire* release — there is no Apple-style upload gap.
- `sonlenef/storepilot-mcp` — 34 tools with **`portfolio_overview`** (every app on both stores
  in one call), `parity_check`, `release_both`, and `play_halt_rollout` ("stop a rollout
  immediately, no confirmation step" — the right default for an emergency).

**Reading, corrected against our own code:** `scripts/play-upload.mjs` is closer to this
surface than expected — it already drives `edits.bundles`, `edits.listings`,
**`edits.images` for `icon`, `featureGraphic` and all three screenshot sets**,
`edits.details`, `edits.deobfuscationfiles`, `internalappsharingartifacts` and
`applications.dataSafety`, and it already has **`--dry-run`** ("print the plan, make no
network requests", `scripts/play-upload.mjs:70`). Two things it does not have, and the prior
art does: a **portfolio-level snapshot** (`portfolio_overview`: every app on both stores in one
call) and **`play_halt_rollout`** — an emergency rollout stop that deliberately asks for no
confirmation.

---

## 2. What is good in their stuff and cheap to import

Ordered by value per hour of work. Each is a harness change, not a slot.

### 2.1 A holdout set the builder cannot read

**Their version:** StrongDM's "dark factory" pattern (**ON RECORD** — env.dev, Pulumi): plain-
English Given/When/Then acceptance scenarios stored where the generating agent cannot reach
them; each scenario runs **3×, 2-of-3 must pass**; the overall gate is **≥90 %**; the generator
receives only a one-line failure — *"SQL Injection Detection failed: endpoint returned 500"* —
never the scenario text, so it cannot game the test.
**Ours:** `differentiators.json` is a promise list with proofs, but **the builder authors the
proofs**. `verify-slot.mjs` checks that every promise is resolved; it cannot check that the
proof is *the one recon would have demanded*, because nothing outside the builder's context
holds that answer.
**Cost to adopt:** low for one thing only — a `docs/ideation/<slot>.holdout.md` written at
stage 0.6 (when recon's reasons are already being frozen) that no generator prompt ever
includes, checked at stage 4 by the verifier.
**What it changes:** turns "the promises are resolved" into "the promises were tested".

### 2.2 A number for how often the owner rejects what the factory called ready

**Their version:** the exact promotion gate from the dark-factory write-ups: auto-merge is
enabled **only after 20 PRs** where scenario pass rate >90 %, false-positive rate <5 %, and
**human override rate <10 %**. Pulumi's version adds: humans approve every change until those
three hold.
**Ours:** we record approvals and revocations (`release-approval.json`,
`ci_signals` / `harness/lessons.md`), but nothing computes a rate, and `approve-release.mjs`
does not distinguish "approved as asked" from "approved after being sent back".
**Cost to adopt:** low — an `outcome` field on the approval record (`approved_clean` /
`approved_after_repair` / `revoked`) plus a `kiln-status.mjs` line.
**What it changes:** it is the only honest measure of whether the verification system can be
trusted with less supervision. Today "the gates are green" and "the owner was satisfied" are
not related by any number.

### 2.3 Enforcement the agent cannot edit, not persuasion

**Their version:** Consort (Databricks, arXiv **2609.09671**, Sep 2026) classifies agent
frameworks into three enforcement modes: **by persuasion** (prompt discipline the model may
ignore), **by front-loaded structure** (strong spec, then a trusted build), and **by controls
the agent cannot edit** — a deterministic orchestrator, human-approved gates that fail closed,
**immutable tests within a unit of work**, and "green" meaning a real run against real data
rather than an agent's say-so. It names GitHub Spec Kit, **`obra/superpowers`**, BMAD and GSD
as persuasion/structure examples, and argues they fail under pressure to go green because the
agent can set the rule aside.
**Ours:** `.agents/skills/` vendors `obra/superpowers` — so the prior art's critique lands
directly on our skills layer. Our *gates* are the other mode (`check-economics`,
`check-differentiators`, `verify-slot`, the approval digest), which is the strong side.
**Cost to adopt:** near zero, and it is a question rather than a feature: **does
`release-approval.mjs`'s digest cover the gate scripts themselves?** If a slot's approval
digest covers `app/**` but not `scripts/**`, then the agent that wrote the app can also weaken
the check that judges it, between approval and release. If it does cover them, the same
mechanism is also what stops a gate from being relaxed silently — and that deserves a sentence
in `AGENTS.md` instead of being an implementation detail.
**What it changes:** knowledge, and possibly a one-line widening of the digest.

### 2.4 A verifier output that names the failure, not the wall of history

**Their version:** one-line failure, no scenario text. Also: "A ledger `BLOCKED(...)` label,
including a wall of historical labels, is not evidence" is already *our* rule in `AGENTS.md` —
this is the same insight, applied to the verifier instead of the worker ledger.
**Ours:** `verify-slot.mjs` prints a structured report; a failing run is readable, a *partially*
failing run with context is long.
**Cost to adopt:** low. **What it changes:** fewer tokens spent re-deriving which check failed.

### 2.5 A portfolio-level head-term registry (and the cannibalisation rule)

**Their version:** **ON RECORD** (Sonar, MobileAction, App Radar): *"Every app should own one
head term. No other app in your portfolio should target that term in any metadata field."*
Two of your own apps splitting 100 searches 60/40 leaves you ranking #4 and #6 instead of #1;
the recommendation is to consolidate or sunset the weaker app, keeping the reviews of the
stronger one. App Radar frames the same problem as a market-tier matrix once one person runs
ten to forty apps.
**Ours:** `scan-market.mjs` + `scan-titles.mjs` screen *queries* per proposal; nothing prevents
two slots from being proposed on the same head term, and nothing checks it at pack time.
**Cost to adopt:** medium — a `registry/head-terms.json` and a check in `store-kit.py` that
fails when two slots' titles/short descriptions collide on a registered term.
**What it changes:** it protects the one asset that compounds (a slot's ranking) and it is
simultaneously **evidence** in a Repetitive Content investigation — two of our apps fighting
for one query is the machine-readable form of "highly similar functionality".

### 2.6 The kill rule as a system, not a spreadsheet column

**Their version:** the portfolio-math write-ups (**ON RECORD**, DEV/SquaredTech) put the whole
strategy on one condition — a portfolio of 20+ only wins when build cost is very low, the niches
are niche-of-niches, you own a channel, and **you are willing to kill aggressively**. Their
worked rule: archive anything that misses ~3 paying customers or ~$50 MRR within 30 days. The
stated outcome is a power law, not 30 winners: one app carries 35–50 % of revenue.
**Ours:** `economics.json` carries `killCriteria` and `check-economics.mjs` validates that they
exist — but nothing evaluates them after launch, and no slot has ever been killed. The
post-mortem for slot 1 already called this out (`docs/ideation/2026-09-26-five-recipes.md` §6.3:
*"the kill criteria will remain prose that nothing evaluates"*).
**Cost to adopt:** low at first — a `kill-review` line in `kiln-status.mjs` that lists slots
past their decision date with no recorded verdict, and a `--kill <slot> --reason` path that
archives rather than silently abandons.
**What it changes:** without it the factory is a graveyard with good intentions; the prior art
is unanimous that the kill rule is what makes a portfolio a system.

### 2.7 A portfolio-level snapshot, and an unconfirmed emergency stop

**Their version:** StorePilot defaults `dryRun: true` on workflows and requires `confirm: true`
on destructive writes — **we already have the dry-run half** (`--dry-run`,
`scripts/play-upload.mjs:70`). What we do not have is `portfolio_overview` ("every app on both
stores in one call") and `play_halt_rollout`, described by its author as stopping a rollout
**immediately, with no confirmation step** — the correct default for a bad release, and the
opposite of the confirm-on-everything posture.
**Ours:** `play-upload.mjs` is gated by the release approval and refuses without a recorded
one, which is a stronger gate at the boundary that matters most. But a bad rollout has no
documented stop at all.
**Cost to adopt:** low — a `--halt` path against `edits.tracks` and a `kiln-status.mjs` line
listing every app's current track state.
**What it changes:** an emergency has a script instead of an improvisation, and the factory
gains a cross-slot view it has never had.

---

## 3. Problems they already hit, and whether we are exposed

| # | Problem they hit | Evidence | Our exposure today |
|---|---|---|---|
| 1 | **The app is not the bottleneck; the console is.** *"Writing the app was never the bottleneck. App Store Connect and Google Play Console submissions were… dozens of small, easy-to-miss requirements that have nothing to do with whether your code works."* | **ON RECORD** — "90+ apps with AI coding agents", Aug 2026 (read via search index; mirror offline) | **Partially covered.** `console-form.json` + `console-answers.md` + `console-walkthrough.mjs` are exactly the fix for this. This is the one prior-art problem we have already solved better than they did. |
| 2 | **Catalogue-wide memory decay.** *"remembering which of ninety apps still uses a deprecated permission"*; *"re-running the same fifteen-step submission checklist without skipping step eleven because you're tired."* | same source | **Exposed, at small N.** Two slots — one policy deadline × two repos is survivable; ten is not. No per-slot "which platform deadlines are outstanding" register exists. |
| 3 | **Judgment stays human.** Money-touching config (IAP, prices, webhooks) gets a manual pass every time; irreversible operations are "the agent proposes, I approve"; *"it built successfully" is not a definition of done*. | same source | **Covered by design.** `release-gate.mdc` + the approval digest are that rule made structural, and the digest lapsing on source change is strictly stronger than "I approve". |
| 4 | **Repetitive Content / app-spam enforcement.** *"Creating multiple apps with highly similar functionality, content, and user experience."* Removals don't strike the account; **suspensions do**, and repeated ones terminate the account and **every app in it**, with no new accounts allowed. Google's own white-label guidance: use **decentralised** accounts, unique listings, unique graphic assets per app — *"a sea of identical looking apps"* is the named failure. | **ON RECORD** — Play Console Help (Spam; Enforcement Process; Best Practices for White Label Developers) | **Covered — and this is why `visual-identity.mdc` exists.** The per-slot mark/palette/component rule reads as taste; it is actually account survival. The residual exposure is *tonal*: two slots in the same category with the same job is the violation, and no gate measures "two slots are one recipe". |
| 5 | **Automated review cross-checks declarations against runtime behaviour.** Data safety mismatches with SDK behaviour; descriptions/screenshots cross-checked against what the app does; false rejections reportedly rose ~20 % during 2025 while Google's AI review struggled to contextualise. | **ON RECORD** — QAwerk, pushmyapp, 2026 | **Covered.** The pack-derives-from-the-build rule, `validate_console_answers`, and the provenance chain are the right shape. The risk we do not measure: an SDK added later that transmits (Play Billing's `datatransport` did this to `teacher-gradebook`). |
| 6 | **Blast radius of one account.** A developer with 2,500 published apps had the account terminated because Google forced a 30-day update sweep of all of them and it was impossible. Google also closes dormant accounts (<1,000 lifetime installs, no login 180 days). | **ON RECORD** — StackOverflow/Play Console support answer and account-requirements write-ups | **Exposed, deliberately.** Everything ships under one `com.gexetr`. That is correct while the count is small and the docs are right that a shipped `applicationId` is permanent. It becomes the factory's single largest risk at scale, and the prior art's answer (decentralised accounts) conflicts with the namespace rule — a conflict worth recording rather than discovering. |
| 7 | **The 12-tester / 14-day closed test**, per app, for personal accounts created after 2023-11-13; **organisation accounts skip it entirely** with a D-U-N-S number. Still in force in 2026. | **ON RECORD** — Play Console Help, multiple 2026 guides | **Already ON RECORD in the repo** (`docs/monetization-strategy.md` §1.1) and already inferred not to apply, because `com.gexetr` has published apps. The new fact is the escape hatch: an organisation account removes the gate. Worth recording for a future category that needs its own account. |
| 8 | **Client-side secret leakage is this era's signature defect.** Of 206,867 real Android apps scanned, 1,207 were LLM-enabled and **66.3 % had at least one LLM-API misuse**: 41.0 % leaked API credentials, 24.3 % exposed system prompts, 39.1 % failed input sanitisation. LM-Scout bypassed usage restrictions in **127 of 181 apps**, with **75 % of attacks causing direct financial damage**. Separately, LLMs found secrets in **35 % of 50,000 Play apps** collected Aug–Oct 2025 — 18,908 secrets, 1,802 verified live (Stripe keys, GitHub tokens, AI API keys). And ~**1 in 4 AI-generated code samples** contains a confirmed OWASP vulnerability. | **ON RECORD** — arXiv 2505.08204 (LM-Scout); arXiv LLM-API-misuse study; "Secrets Unlocked"; AppSec Santa via NowSecure | **Partially covered.** `secret-guard` and the no-keys-in-git rule cover the repo. Nothing covers *the APK*: a slot that called a paid API from the client would pass every gate we have. This is the single highest-value new rule the prior art produces: **no shipped slot calls a paid API from the client**, and a check that fails when a key-shaped literal or a provider SDK key reaches `app/src/main`. |
| 9 | **Apple's dynamic-code precedent (relevant to the next Android slot, not this one).** Guideline 2.5.2 enforcement: Replit and Vibecode updates blocked; "Anything" pulled 30 Mar 2026; Botify/Photify AI removed ($430 k/mo combined, $500 k+ withheld, lawsuit filed). Apple: *"no rules specifically against vibe coding apps"* — the enforcement is against **executing unreviewed code**. App Store submissions **+84 % in one quarter**; review times 24 h → up to 30 days. | **ON RECORD** — TNW, CNBC, asotxt, 2026 | **Structural, not active.** None of our slots downloads or executes code at runtime. The rule to hold is: the shipped APK stays self-contained, no runtime extension mechanism, no "the agent will fix it in the field". |
| 10 | **The money.** 58 % of new subscription apps earn under $1,000 in year 1 (**zero-earners are not even counted**); the top 10 % take **94.5 %** of subscription revenue; median revenue per new app **fell 22 %** while launches grew 31 %; monthly new subscription launches **7×** since 2022 (~2,000 → 14,700+); apps launched before 2020 still take **69 %** of revenue; only **4.6 %** of new apps reach $10 k/month MRR within two years. Play-side: **499,548** new releases in 2026 vs 203,635 in 2022, and only **12.2 %** of them ever earned a single star rating; **78 %** of recent releases got fewer than 1,000 downloads. | **ON RECORD** — RevenueCat SoSA 2026; Adapty; Testers Community (3,037,501 listings) | **This is the finding that matters.** It does not say the factory is wrong; it says the factory's *win condition* is a power law and the base rate of an unaimed attempt is near zero. Its operational consequence is §2.5 + §2.6: head-term discipline and a real kill rule are not hygiene, they are the strategy. |
| 11 | **AI apps monetise differently:** ~+41 % revenue per customer but ~30 % faster churn. | **ON RECORD** — RevenueCat SoSA 2026 | A churn fact for slot economics, not a harness change. |
| 12 | **Individual, honest post-mortems.** €0.20 lifetime (with AdMob, one ad per five questions). Three iOS apps with AI: 12 downloads, $37, ¥3 — *"finishing a product, monetizing it, and helping people discover it are three separate challenges."* A 90-day shutdown: 412 signups, ~11 payers, $209 MRR against ~$8,000/month burn. The one $25 k success was a **game licensed to web portals**, not a store listing. | **ON RECORD** — bmateas.com; dev.to; OperatorBook | Confirms that the store is not a distribution channel you get for free by shipping. |

---

## 4. What nobody else appears to have (PlayKiln's actual novelty)

Stated as narrowly as the evidence allows. **INFERRED** unless marked otherwise.

1. **An approval bound to a digest of the build inputs**, so that editing the app after approval
   invalidates the approval automatically (`scripts/lib/release-approval.mjs`). The prior art has
   human gates that fail closed (Consort) and an override-rate metric before auto-merge (the dark
   factory), but nothing that *expires a human decision when the artifact changes*. This is the
   best idea in the repo and it is not in any of the sources.
2. **A promise contract that fails the build while any reason is undelivered**
   (`differentiators.json` + `check-differentiators.mjs`), authored from recon *before* the UI.
   Consort freezes intent at a hashed gate, which is the nearest analogue; nobody appears to
   freeze *the reasons the app was chosen* and then refuse to call the work done.
3. **A money gate that refuses to scaffold a slot at all** (`check-economics.mjs`, mandatory
   `--economics`, capability/archetype pairing). The prior art treats monetisation as a doc
   (a0.dev, Rork), as a stack (Rocapine), or as underwriting (Bending Spoons). Nobody published
   a *refusal*.
4. **A store pack derived from a declared identity with a provenance chain** (`studio.json` →
   rendering → `provenance.json` → `visual-qa.py` measuring shipped pixels). Closest is
   Rocapine's RocaBoost (marketing/monetisation automation, no published mechanism). Honest
   caveat: `visual-qa.py` can prove *differentiation and truthfulness*, never beauty, and
   `visual-identity.mdc` already says so.
5. **The submission ladder written as a measured runbook** (`docs/play-publishing-playbook.md`
   §5.1, walked live 2026-09-29), with the human steps enumerated — though the count of those
   steps is contested inside the repo; see §6.2. No other source describes walking the console
   as an agent under observation.

**What this is not:** it is not a moat. Every item above is a *discipline*, and disciplines are
copyable in an afternoon by anyone holding the same models. Artenas is pitching the same
portfolio logic with no gate; the difference is that our version can *prove* it refused to ship
— which matters exactly once, in a policy investigation or in a post-mortem about whether the
factory was honest with itself.

---

## 5. The uncomfortable arithmetic, restated for the factory

**MEASURED** (RevenueCat SoSA 2026, Adapty, Testers Community — 3,037,501 Play listings):

- baseline probability that a new app earns $1,000 in year 1: **~42 %** (58 % earn less);
- probability that a new app reaches $10 k/month MRR within two years: **4.6 %**;
- probability that a 2026 release ever earns a single star rating: **12.2 %**;
- 69 % of subscription revenue belongs to apps launched **before 2020**;
- supply is growing ~7×/month-over-month-since-2022 while attention is not.

A factory is a **portfolio of draws from that distribution**, so the arithmetic only works if
three things are true: the build is nearly free (it now is), each attempt is *aimed* at a
measured wedge (that is §0 of `ideation-gate.mdc`), and attempts that miss are killed on a clock
(§2.6 — not implemented). PlayKiln currently enforces the aim before the build and the honesty
after it, and enforces nothing at all about **stopping**.

The prior art's bluntest sentence belongs here. The studio that shipped 90 apps: *"AI coding
agents didn't replace the parts of shipping software that require judgment. They replaced the
parts that required patience."* — **ON RECORD**.

---

## 6. Open questions this research creates

Each with the experiment that would settle it. None of these is a slot.

1. **Does the approval digest cover the gate scripts?** Read
   `scripts/lib/release-approval.mjs`'s input list and check whether `scripts/**`,
   `store/studio.json` and `.cursor/rules/**` are inside it. If they are, say so in `AGENTS.md`;
   if not, the agent can weaken the check that judges it after approval — the exact failure
   Consort's taxonomy predicts (**INFERRED**, cheap to settle).
2. **How many human steps does a first publication really have?** Two repo documents disagree,
   and the difference is not cosmetic. `AGENTS.md` stage 10 says *"Exactly two steps stay
   human: uploading the five store-graphic fields … and the two first-publication legal
   declarations"*, and it justifies the graphics half by *"the sandboxed browser cannot set a
   `file` input"*. But `scripts/play-upload.mjs` **already uploads all five graphic fields**
   through `edits.images` (`icon`, `featureGraphic`, `phoneScreenshots`,
   `sevenInchScreenshots`, `tenInchScreenshots`), and its own header states the real constraint:
   *"this API cannot create a brand-new Play application. A human must create the package,
   upload its first AAB, complete the legal consents **and the content rating questionnaire**,
   and press publish. Everything after the first upload can be automated."*
   `docs/play-publishing-playbook.md` §7 agrees with the script — *"Complete legal and audience
   declarations — Human"* — and lists the content-rating questionnaire, the ads declaration and
   the target-audience declarations inside that human section.
   So the browser's `file`-input limit explains why a *walkthrough* cannot set the graphics; it
   does not explain why the **factory's own uploader**, which can, is not the thing that does
   it. Two readings, both cheap to settle: (a) the graphics step is human only for the very
   first submission, before an app record with a live track exists, and `play-upload.mjs`
   covers every later one; (b) the first-publication human list is understated by one or more
   items (the content rating at minimum). **UNVERIFIED** — a reading of two documents against
   each other, not a measured console behaviour. Settling it is worth real time: it is the
   difference between "the ladder is nearly automated" and "the ladder has four human steps
   each time".
3. **What is our override rate?** Needs the `outcome` field from §2.2 and 10–20 completed
   runs before it means anything. Pulumi/StrongDM's thresholds (>90 % scenario pass, <5 % false
   positive, <10 % override) are the reference.
4. **Do two slots share a head term?** Check the two shipped slots' titles and short
   descriptions against each other and against `registry/apps.json` by hand. If they do, that is
   an ASO defect *and* a Repetitive Content data point (**ON RECORD** risk, cheap to check).
5. **Which of the 12 prior-art problems have we actually measured ourselves?** Only #4, #5 and
   #7 have repo-internal records. The rest are external evidence mapped onto us by reading —
   useful, but not the same thing as a measured exposure.

---

## 7. Sources

Retrieved 2026-09-29 unless noted.

| Source | Used for |
|---|---|
| `github.com/Ice-Citron/New-Dejima` (**ON RECORD via search index**; direct fetch returned a near-empty page) | 7-phase autonomous Android factory; "12 apps built"; revenue explicitly unmeasured |
| `linkedin.com/company/artenas` | Artenas: AI-native app studio & publisher, Paris, 3 employees, founded 2026 |
| `agencyadm.invest.gov.tr` — HubX unicorn announcement | HubX: $75 M, $1.2 B pre-money, 40+ products, 600 M users, 370 staff; "repeatability of their model" |
| Medium — Educapital, *Why we invested in Rocapine* | Rocapine: $13 M Series A, RocaStudio/RocAI/RocaBoost, 100+ concepts tested in a year |
| Acquiry, Sacra, TBPN Digest, Bending Spoons F-1 | Bending Spoons: Spoon Engine (50+ services), 6/2,500 sourcing, no cross-portfolio bundling, 60–80 % staff reductions |
| `productos.dev`, `vp0.com`, `rork.com`, `litmustools.com`, `catdoes.com` | Prompt-to-app builders: a0.dev, Rork/Rork Max, Rocket.new, FlutterFlow, Draftbit, Bravo, Replit, Lovable, Bolt, CatDoes |
| `ai.google.dev` (AI Studio Android; Build mode), `developer.android.com` blog, `code2native.com` | Google AI Studio prompt → Compose → Play internal testing; constraints; ZIP-export path |
| `github.com/OyaAIProd/google-play-developer-mcp`, `github.com/Jeronimo0228/StorePilot`, `github.com/rabdulsal/playstore-release-mcp`, `github.com/sonlenef/storepilot-mcp` | Play automation coverage: 150-tool Publisher v3, orchestration/dry-run, `play_upload_graphics`, `portfolio_overview` |
| `env.dev` dark-factory guides; `pulumi.com` dark-factory-for-infrastructure | Holdout scenarios, 3×/2-of-3, ≥90 % gate, >90 %/<5 %/<10 % promotion thresholds |
| arXiv **2609.09671** (Consort, Databricks) | Three enforcement modes; immutable tests; controls the agent cannot edit; names Spec Kit, `obra/superpowers`, BMAD, GSD |
| Play Console Help: *Spam* (Repetitive Content), *Enforcement Process*, *My app has been removed*, *Best Practices for White Label Developers*, *Play Console requirements* | Repetitive Content; strike → suspension → termination of the account and all apps; decentralised accounts; unique listings per app |
| Play Console Help — *Developer Program Policy* (eff. 2026-08-26) and *AI-Generated Content policy* | AI content policy scope (generation, not summarisation); in-app reporting; account deletion both in-app and on the web |
| QAwerk, pushmyapp, asotxt, TNW, CNBC | Automated review vs declared behaviour; ~20 % rise in false rejections in 2025; Apple Guideline 2.5.2 enforcement and the +84 % submission surge |
| arXiv **2505.08204** (LM-Scout); LLM-API-misuse study (206,867 apps → 1,207 LLM-enabled → 66.3 % misused); *Secrets Unlocked* (50,000 Play apps, 35 % with secrets); NowSecure/AppSec Santa | Client-side LLM/API security defects; ~1 in 4 AI-generated samples with an OWASP vulnerability |
| RevenueCat *State of Subscription Apps 2026*; Adapty; Testers Community (3,037,501 Play listings); Vester; TheVortiq/Appfigures | Revenue distribution, launch supply, install and rating base rates |
| `github.com/jasoncolapietro/suede-creator-skills` — `android-app-factory` | An existing agent-skill factory (Kotlin/Compose, Play policy baseline, release evidence gate, staged rollout, "it will not press publish") — the closest *skill-pack* prior art (**ON RECORD**; the raw SKILL.md fetch returned an empty body) |
| StackOverflow + Play Console support answers | No cap on apps per account; 15 new app uploads per 24 h; the 2,500-app account terminated by a forced 30-day sweep |
| bmateas.com; dev.to (`chii-ai`); OperatorBook; "90+ apps with AI coding agents" (Aug 2026, **via search index**) | Individual outcomes and the operational lessons: console is the bottleneck, judgment stays human, "it built successfully" is not done |
| **Repo-internal, checked while writing this** — `scripts/play-upload.mjs` (header contract, `--dry-run`, `edits.images` upload of `icon`/`featureGraphic`/three screenshot sets), `docs/play-publishing-playbook.md` §6–§7, `docs/research/2026-09-23-google-play-automation-surface.md` | Correcting §1.5 and §6.2: the graphics upload is already automated for an existing app record, and the human-step count differs between two repo documents |

---

## 8. What this document does not say

- It does not say the factory idea is wrong. It says the base rate of an *unaimed* attempt is
  near zero, and that the prior art's winners are the ones whose **loop** repeats.
- It does not claim we are first at anything except, weakly, the combination in §4.
- It does not propose a slot, a niche, or a category — and it does not touch anything on the
  killed list in `docs/research/2026-09-24-market-decision.md` §3 or the dead zones in
  `docs/niche-selection.md`.
- It does not convert any of §2 into a decision. Those are harness changes and each needs the
  owner's own instruction before it becomes work.
