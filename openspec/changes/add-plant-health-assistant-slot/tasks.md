# Tasks — add-plant-health-assistant-slot

Role tags: **[Build]** Cursor/Codex code work, **[Store]** listing and pack work,
**[Verify]** independent gate that must be run, not reasoned about.

Tasks in group 1 are **human decisions**. Groups 3–5 are gated on them: do not start a
gated group until the named decision is recorded in this file.

## 1. Human decisions (gate — no code before these are recorded)

- [ ] 1.1 **[Human]** Record the **O1 distinctness sentence** — one sentence naming what a
  user gets from this app that they do not get from the incumbents in
  `docs/market-enrichment-2026-09-23.md`. Acceptance: the sentence is written into this file
  under *Recorded decisions*, and it names a job, not a colour or a name. If it cannot be
  written, stop and do not scaffold (`docs/monetization-strategy.md` §1.2).
- [ ] 1.2 **[Human]** Record the **O2 engine path** — `on-device/deterministic` or `hosted`.
  Acceptance: the choice is written into *Recorded decisions*, together with whether it
  requires a server. If `hosted`, the change is re-scoped before group 3 because the slot then
  needs a server component and the task list is no longer sufficient.
- [ ] 1.3 **[Human]** Record the **O3 monetization archetype and product list** — the model
  and the exact product ids, per `docs/monetization-strategy.md` §3.6. Acceptance: archetype,
  product ids, and (if a pack is sold) its `Fulfillment` class and `C_unit` assumption are
  written into *Recorded decisions*.
- [ ] 1.4 **[Human]** Confirm whether the §1.1 production-access gate
  (`≥ 14 days` / 12 testers) applies to this account, checked in Play Console
  (`docs/monetization-strategy.md` §1.1). Acceptance: yes/no recorded; if yes, the slot joins
  a batch rather than being serialized.

### Recorded decisions

<!-- Filled by the human at task 1.1-1.4. Implementation reads this section. -->

- **O1 / O2 RE-DECIDED by the human (2026-09-23, after the engine research landed):** the
  **photo-diagnosis step is removed**. The measured ceiling (~31% on real-world images;
  re-verified against the primary source) means a photo diagnosis cannot carry a food-safety
  instruction, and the harvest interval inherits whatever feeded it. The slot keeps its
  harvest-interval and treatment value, but the **user supplies the inputs** — the crop, and
  the disease or the product they intend to use — instead of the app guessing them from a
  photograph.

  Consequences that must be applied before task group 3:
  - The `Photo-first capture-to-plan workflow` requirement in `spec.md` **no longer describes
    this product** and must be re-scoped. The pivot changes the slot's whole observable
    behaviour, so the proposal, spec, design and tasks all need revision as one coherent unit,
    not a patch.
  - The engine becomes a **deterministic lookup, not inference**. That is precisely what makes
    the O3 choice (`Fulfillment.LOCAL`, no per-unit cost) correct rather than optimistic.
  - The remaining accuracy question moves to the **symptom/disease selection** path: if the
    user self-identifies from a structured key, an error is the user's own observation rather
    than the app's confident guess — a materially different liability and honesty position.

  **The slot is now gated on exactly one thing:** that the interval data is licensable and that
  restating it is permissible, per
  `docs/research/2026-09-23-harvest-interval-differentiator.md`. If that evidence fails, this
  slot has **no differentiator** and must be closed and replaced, not shipped with a softened
  claim.

- **THE GATE FAILED — niche closed (2026-09-23).** The evidence landed and it kills the
  differentiator. Recorded from
  `docs/research/2026-09-23-harvest-interval-differentiator.md` (53 KB), reviewed before
  acceptance. Three independent findings, any one of which is fatal:

  1. **The claim logically overclaims, and no dataset can fix it.** A pre-harvest interval is a
     **legal use restriction**, not a residue measurement. It says nothing about under-run
     intervals, tank mixes, or MRLs. So "how long until the crop is safe to eat" asserts a
     **safety verdict** the data cannot support *even with a perfect licensed dataset*. The
     honest restatement is "your label's minimum interval has not yet elapsed". This is
     independent of jurisdiction and independent of data availability — it is a defect in the
     sentence, and it would have shipped as a food-safety claim.
  2. **There is no market for it as a searched job.** `pre harvest interval` and
     `harvest interval pesticide` fuzzy-match "harvest" and return time-tracking apps, banks,
     churches and farm games — zero on-topic cards; Play's `dead-zone` verdict there describes
     its own text match, not a market. `safe to eat after spraying` matched "eat" and returned
     food diaries.
  3. **The data is geo-limited and the advisory role is regulated.** Confirmed by reading the
     published files: **France E-Phy** (Licence Ouverte, commercial use permitted, 17,866 of
     81,615 usage rows populated) and **Germany BVL** (`dl-de/by-2-0`). The **EU has no PHI
     dataset** and the **US has no free structured one** (170,000+ label PDFs; structured PHI is
     sold commercially). Separately, "adviser" is a certified role in the EU and California
     licenses anyone who "offers a recommendation on any agricultural use" — §11408 excludes
     home use and §11411 carves out label-consistent summaries, but this is legal-read territory,
     not a detail.

  **Both fallbacks were checked against live data before recommending either, and both are weak:**
  - *Label-interval tracker* (user enters the interval from their own label). Clean of licence and
    overclaim risk, but the product **already exists**: a probe of `spray schedule garden` returned
    `Spraybook: Spray Record Diary` (4.0★, **500+ installs**) sitting behind garden planners at
    100K+–500K+ (Seed to Spoon, Seedtime, Planter). It is a small utility in an owned space with no
    demand evidence for the phrasing.
  - *Crop-specific spray calendar* reverts to competing with Plantix at 10M+ using the same
    ~31%-accurate diagnosis engine this proposal already rejected.

  **Conclusion:** this niche is closed. The cause of death is not the engine alone and not the
  data alone — it is that the product's entire differentiator was a **safety claim**, and a safety
  claim cannot rest on a use restriction. The change is left in place, unarchived, pending the
  human's decision on re-targeting; it must not be implemented.

- **RE-TARGETED by the human (2026-09-23):** this niche is closed and the factory moves to
  **`visual schedule autism`** (a visual-schedule / routine utility). The basis is a first-party
  probe read by the orchestrator rather than the ranked list alone: the query is **clean** (every
  visible card was an on-topic visual-schedule app - no fuzzy-match pollution), and the supply is
  **measurably bad against proven demand** - `Fun Routine - Autism` at **2.9 stars with 100K+
  installs**, `Visual Schedules and Social St` at **2.0 stars with 10K+ installs**, against only
  one respectable incumbent (`Lil Planner`, 4.8 stars, 10K+). The ranked-list score came from the
  **pre-fix** market sensor, so install/IAP enrichment is still outstanding and must be gathered
  before the slot is committed.

  **Why this idea suits the first real slot:** it is a **deterministic UI product** - schedules,
  pictures, checklists, timers - with **no ML engine, no server, and no regulated claim**. Every
  fatal property of the plant idea (a ~31%-accurate inference engine, a safety verdict, a licence
  question, a geo-limited dataset) is absent. It therefore exercises the factory end-to-end on
  honest ground instead of on a claim the factory cannot support.

  **Two risks that must be resolved before implementation, and they are not small:**
  1. **Audience and policy.** An autism/children's audience raises Google Play **Families**
     policy, restrictions on advertising to children, and the fact that "autism" is
     health-adjacent - apps in this space are criticised for making therapeutic claims. The
     product must be a scheduling *utility*, and its monetization must be defensible.
  2. **Differentiation must be real, not decorative.** The incumbents are bad, which is an
     opening, not a licence to ship a thin reskin. What a user gets that a 2.9-star app does not
     must be stated in one sentence before scaffolding, exactly as task 1.1 required here.

  The plant change stays in the repo as an unarchived, unimplemented record and is **not** to be
  built; its research documents under `docs/research/` remain the evidence for why.

- **O1 distinctness sentence (recorded 2026-09-23):**

  > Photograph a sick **edible garden crop** (tomatoes, cucumbers, peppers) and get a
  > diagnosis, a treatment plan, and **how long until the crop is safe to eat**.

  The named job is *food-safety timing after treatment*, not plant identification. The
  incumbent identified in `docs/market-enrichment-2026-09-23.md` (`cn.danatech.xingseus`,
  PictureThis, 50M+ installs) answers "what plant is this"; this app answers "what is wrong
  with my crop, what do I do, and when may I eat it".

  **Crop-specificity alone is NOT sufficient, and evidence proves it.** The vertical run in
  `docs/research/2026-09-23-plant-vertical-evidence.md` found a **dedicated crop-diagnosis
  incumbent already at scale**: `com.peat.GartenBank` — *Plantix - your crop doctor*, **10M+
  installs, 4.2★, 132,000 reviews** — plus the mid-scale *Agrio* at 500K+. So "we diagnose
  edible crops" describes a job Plantix already performs at 10M+. The store data cannot say
  whether Plantix's gap is crop granularity or something else, and this scan cannot compare
  products. **The consequence is that the harvest-interval clause carries the differentiator
  essentially alone**, which is why the pending evidence for it is critical path rather than
  confirmation.

  Two further facts from that run that shape the slot:
  - The vertical is **fragmented at the crop-symptom level** (no single owner; nothing above
    500K+ for tomato or pepper specifically), so an entry point exists — it is just not
    defined by being crop-specific.
  - The only **paid-upfront** app in 112 enriched apps is `com.farmingai.tomato.ImageClassification`
    ("Tomato Disease Diagnosis", $7.49) and it sits at **1+ installs**. One observation, not a
    law, but it is evidence against paid-upfront and consistent with the O3 choice below.

  **This clause is the differentiator and is therefore a claim, not a settled fact.** It
  depends on two things that are being established rather than assumed, and task group 3 must
  not start until they are:
  1. whether the interval data exists in a form the app may ship (licence, jurisdiction), and
  2. whether restating an interval is permissible or is regulated advice.
  Evidence is being gathered in `docs/research/2026-09-23-harvest-interval-differentiator.md`;
  until that exists, this sentence is a **product intent, not a verified capability**. If the
  differentiator does not survive that evidence, O1 must be re-decided rather than the claim
  being softened.

- **O2 engine path — research returned a measured blocker (recorded 2026-09-23):**

  Neither path is currently supportable for *photo-based diagnosis*. Recorded from
  `docs/research/2026-09-23-on-device-plant-diagnosis.md`, whose load-bearing figures were
  re-verified against the primary source during review:

  | Fact | Value | Source (read) |
  |---|---|---|
  | PlantVillage authors' held-out accuracy | **99.35%** | Mohanty et al. 2016 |
  | Same models on real-world images | **31.40% / 31.69%** | same paper |
  | Accuracy when the crop is *given* to the model | **47.8%** (random ≈ 22.5%) | same paper |
  | Same-paper limitation | trained on "single leaves, facing up, on a homogeneous background" | same paper |
  | 2026 replication | **99.73% → 32.05%**, mean confidence still **79.76%** | Frontiers 2026 |

  The last row is the failure mode that matters: **wrong and confident**. A 32%-accurate
  diagnosis displayed at 79% confidence is not an imperfect tool, it is a misleading one.

  Two further constraints, both material:
  - **Training-data coverage does not match the chosen vertical.** The vertical is tomato,
    cucumber, pepper (`proposal.md`). PlantVillage covers 14 crops and **does not include
    cucumber**; pepper has only 2 classes; essentially only tomato has depth. A cucumber
    diagnosis would be an invented capability, not a weak one.
  - **Licence reaches the model.** PlantVillage is **CC BY-SA 3.0**, and its authors state
    that algorithms trained on it fall under the same licence. The GitHub repo carries no
    LICENSE file at all, so the term rests on paper prose — a share-alike obligation on a
    shipped model, and untested legal ground for this factory. UNVERIFIED pending counsel.

  **Consequence for the slot — this is the important part.** The harvest-interval clause is
  *downstream* of the diagnosis: the interval depends on which pesticide the treatment step
  names, which depends on the diagnosis. If the diagnosis is wrong two times in three, then
  the **harvest interval is wrong two times in three**, and the user is given a confident
  instruction about when to eat sprayed produce. The differentiator's value and its harm are
  the same property: it is safety-critical, and it inherits the accuracy of whatever feeds it.

  Therefore **O2 is not a choice between on-device and hosted** — both inherit the same
  quality ceiling unless the product stops guessing. The live question is whether the slot
  keeps a photo-diagnosis step at all. See the recorded options in the handoff; task group 3
  must not start while this is open.

  Evidence still pending in
  `docs/research/2026-09-23-harvest-interval-differentiator.md`: whether the interval data is
  licensable and whether restating it is permissible. That determines whether the
  harvest-interval angle survives *at all*, independent of the engine question.

- **O3 archetype (recorded 2026-09-23):** Free, **no ads**, monetized with **repeatable
  consumable diagnosis packs** (`Fulfillment.LOCAL`). Chosen from data, not preference: 8 of 8
  enriched incumbents in this niche declare in-app purchases and only 3 declare ads, and across
  all 40 enriched apps 27 declare IAP against 9 declaring ads
  (`docs/market-enrichment-2026-09-23.md`).

  **The product ids are NOT recorded yet** and must not be invented here. They are a
  consequence of O2, for this reason: `Fulfillment.LOCAL` means the slot serves each unit
  itself with no per-unit cost, which is only true if the diagnosis runs on-device. If O2
  resolves to `hosted`, then every pack unit costs us real money and is reported to us by a
  client we do not control, so `LOCAL` becomes the wrong class and `SERVER_COST` with a
  `PurchaseAuthority` is required instead (`docs/monetization-strategy.md` §3.7). **O3 is
  therefore conditional on O2**, and both must be recorded before task group 3.

- **Production-access gate applies:** _pending_ - must be checked in Play Console by the human
  (task 1.4). Only the account owner can read this, and it decides whether this slot can be
  serialized or must join a batch.

## 2. Slot creation

- [ ] 2.1 **[Build]** Create the slot with `node scripts/new-slot.mjs` for slug
  `plant-health-assistant` and the O1 idea sentence. Acceptance: command exits 0 and
  `apps/2026-09-23-plant-health-assistant/` exists with applicationId
  `com.gexetr.planthealthassistant`; `registry/apps.json` gains one entry with status
  `building`.
- [ ] 2.2 **[Build]** Confirm the slot did not inherit factory config. Acceptance: the slot
  tree contains no `.cursor/`, `.agents/`, `openspec/`, or `dist/` copy, and no `build/`,
  `.gradle/`, `local.properties`, or keystore file (`openspec/specs/factory-slot-isolation/spec.md`).
- [ ] 2.3 **[Build]** Set the slot's display name, package-id strings, and locales
  (`en-US` default, `ru-RU` additional) per `AGENTS.md` *Store-pack contract*. Acceptance:
  `./gradlew :app:assembleDebug` is not required yet; instead grep the slot for the scaffold
  package name `com.gexetr.scaffold` and confirm zero matches.

## 3. Diagnosis domain and engine seam

- [ ] 3.1 **[Build]** Define the domain model: `PlantPhoto`, `SymptomIntake`,
  `DiagnosisCandidate` (class = disease | pest | nutrient deficiency), `ConfidenceBand`
  (low | medium | high), `TreatmentStep` (action, optional caution), `TreatmentPlan`,
  `DiagnosisRecord` (candidates, cues, band, plan, provenance). Acceptance: unit test asserts
  `DiagnosisCandidate.class` accepts only the three classes and that `ConfidenceBand` has no
  numeric member.
- [ ] 3.2 **[Build]** Define the engine seam `DiagnosisEngine` with
  `suspend fun diagnose(photo, intake): DiagnosisResult`, where `DiagnosisResult` is a
  sealed type covering a diagnosis and an explicit "cannot diagnose, need X" outcome.
  Acceptance: the interface compiles with no engine implementation on the classpath, and a
  test drives it through a fake.
- [ ] 3.3 **[Build]** Implement the `FakeDiagnosisEngine` used by tests and previews.
  Acceptance: unit test asserts a photo-only call (empty intake) returns the
  "cannot diagnose" outcome and never a candidate (spec: *Required observation content*).
- [ ] 3.4 **[Build]** Implement the engine selected at 1.2 behind the seam. Acceptance:
  (a) on-device path — a lint/`rg` check confirms no network call and no API-key string in the
  engine module; (b) hosted path — task list is re-scoped per 1.2 before this runs. Either way,
  `rg -n "api[_-]?key|Bearer " apps/2026-09-23-plant-health-assistant/app` returns no
  credential.
- [ ] 3.5 **[Build]** Record provenance on every `DiagnosisRecord` (engine id + knowledge
  base or model version). Acceptance: unit test asserts provenance is non-empty.

## 4. Treatment knowledge base

- [ ] 4.1 **[Build]** Author the local knowledge base as versioned data: for each cause, the
  cues, the ordered steps, a caution where a step carries a risk, the re-check timing, and a
  **source citation per entry**. Acceptance: a schema test fails if any entry is missing a
  citation, a caution for a risky step, or re-check timing.
- [ ] 4.2 **[Build]** Implement the plan builder that maps a diagnosis to a
  `TreatmentPlan`. Acceptance: unit test asserts steps are returned in declared order and a
  risky step's caution is attached to that step, not to the plan header.
- [ ] 4.3 **[Verify]** Independently read every entry for hazards and unsupported claims, and
  record findings. Acceptance: a reviewer reads the file and reports either "no unsupported
  claim found" or the specific defects; this is a read, not a green test, and is reported as
  such.

## 5. User workflow (Compose)

- [ ] 5.1 **[Build]** Build the photo stage using the system Photo Picker
  (`ActivityResultContracts.PickVisualMedia`). Acceptance: `AndroidManifest.xml` declares no
  `READ_MEDIA_IMAGES`/`READ_EXTERNAL_STORAGE`, and a UI test selects an image from the picker.
- [ ] 5.2 **[Build]** Build the structured intake stage as a required form. Acceptance: UI
  test asserts the "Diagnose" action stays blocked until required answers are present, and the
  blocked state names the missing answers.
- [ ] 5.3 **[Build]** Build the diagnosis-result screen: candidates with their class, the
  cues, the confidence **band**, the advisory statement **on this screen**, and no numeric
  probability anywhere. Acceptance: UI test asserts the band text is present and that no `%`
  appears in the result subtree.
- [ ] 5.4 **[Build]** Build the treatment-plan screen with ordered steps, per-step cautions,
  and re-check timing. Acceptance: UI test asserts steps render in order and a risky step's
  caution renders adjacent to it.
- [ ] 5.5 **[Build]** Implement the "cannot diagnose" state with the specific missing input.
  Acceptance: UI test drives the fake engine's failure outcome and asserts the screen asks for
  the extra photo or answer and shows no candidate.
- [ ] 5.6 **[Build]** Preserve the photo and answered intake across back navigation.
  Acceptance: UI test goes back from intake to photo and forward again and asserts answers are
  still present.
- [ ] 5.7 **[Build]** Confirm there is no chat surface. Acceptance: `rg` over the slot's UI
  package finds no free-form text composer / chat-message list; the result is recorded in the
  change's notes (spec: *No chat surface is offered*).
- [ ] 5.8 **[Build]** If 1.2 selected a generative/hosted engine, add the in-app report path
  on the result screen. Acceptance: UI test reaches the report action from the result screen.
  If the engine is deterministic, record that the path is not required, citing
  `openspec/specs/play-compliance/spec.md`.

## 6. Local persistence and history

- [ ] 6.1 **[Build]** Persist `DiagnosisRecord`s locally (Room or DataStore via KSP — not
  `kapt`) so a past diagnosis re-opens with its photo, intake, and plan intact. Acceptance:
  unit/instrumented test writes a record, recreates the store, and reads it back equal.
- [ ] 6.2 **[Build]** Implement in-app deletion of a record and its photo. Acceptance: test
  deletes a record and asserts the photo file and the intake rows are gone.
- [ ] 6.3 **[Build]** Implement the history list. Acceptance: UI test shows two records and
  opens one.

## 7. Capability wiring (only what §D4 of design.md authorises)

- [ ] 7.1 **[Build]** Add `implementation("com.gexetr.capability:core:1.0.0")` and confirm
  `mavenLocal()` resolves it. Acceptance: `./gradlew :app:dependencies` lists the artifact and
  the build resolves offline after publish.
- [ ] 7.2 **[Build]** Add `telemetry` only if the slot opts in; otherwise omit it and keep the
  Data safety answer "no data collected" where the slot itself collects nothing. Acceptance:
  a manifest-merge check shows no analytics SDK when omitted.
- [ ] 7.3 **[Build]** Add `billing` if 1.3 chose an unlock or a pack, with the slot's
  `BillingCatalog` declaring the product ids recorded at 1.3. Acceptance: unit test asserts
  each declared `ProductId` exists in the catalog and that entitlement state does not consume
  a non-consumable and does not credit `owned` for a pack.
- [ ] 7.4 **[Build]** Add `remoteconfig` only if a flag surface is wanted, and wire
  flags and defaults. Acceptance: no remote value can weaken the advisory boundary, delete-guard,
  or paid access below the reviewed defaults (`remoteconfig/capability.json` policy notes).
- [ ] 7.5 **[Build]** Add `ads` only if 1.3 chose ads. Acceptance: `canRequestAds()` is false
  before consent resolves and no ad request fires first (`ads/capability.json`).

## 8. Monetization (gated on 1.3)

- [ ] 8.1 **[Build]** Implement the paywall/offer exactly as recorded at 1.3, with price and
  terms shown in-app. Acceptance: UI test shows the price for each product id.
- [ ] 8.2 **[Build]** If a `SERVER_COST` pack was chosen, do **not** implement it in this
  slot; raise it as a new change, because `PlayBillingController` refuses a `SERVER_COST`
  catalog without a `PurchaseAuthority` and the factory ships no default one
  (`docs/monetization-strategy.md` §3.7). Acceptance: this file records the deferral and no
  `SERVER_COST` pack is present in the shipped catalog.
- [ ] 8.3 **[Build]** If a `LOCAL` pack was chosen, implement credit/spend so a repeat
  purchase is possible (consumable, not an entitlement). Acceptance: unit test buys the same
  pack twice and spends the balance correctly.
- [ ] 8.4 **[Store]** If ads were chosen, name the rewarded unlock in the store pack before UI
  sign-off; if there is nothing worth unlocking, record that a purchase leads instead
  (`docs/monetization-strategy.md` §3.4). Acceptance: the store pack states the ads archetype
  and the rewarded unlock, or states the purchase-led archetype.

## 9. Compliance surface (Build)

- [ ] 9.1 **[Build]** Confirm the manifest declares no permission the app does not use;
  camera only if task 5.1 added in-app capture. Acceptance: `node scripts/verify-slot.mjs --slot
  plant-health-assistant` reports no unexpected permission.
- [ ] 9.2 **[Build]** Keep `dataExtractionRules` with separate `cloud-backup` and
  `device-transfer` blocks, excluding any token/secret path added by `billing`/`remoteconfig`.
  Acceptance: the gate at 12.1 fails if either block or the referenced XML is missing.
- [ ] 9.3 **[Build]** Verify edge-to-edge and predictive back on the target SDK. Acceptance:
  manual check on an emulator at API 36 that the system bars do not overlap content and
  predictive back previews; record the observation.
- [ ] 9.4 **[Build]** Write the advisory-boundary copy so it is plant-only and carries no
  cure/guarantee language, and apply it on the result screen. Acceptance: `rg` over slot
  strings and `store/` finds no `cure`, `guarantee`, `100%`, `always works`, `medical`, or
  `health advice` claim; the advisory string is present on the result screen.
- [ ] 9.5 **[Build]** Confirm 16 KB readiness. Acceptance: if no native library is shipped,
  record that; if one is shipped, add the verification task required by
  `openspec/specs/play-compliance/spec.md`.
- [ ] 9.6 **[Build]** Write the privacy policy draft and Data safety from the slot's actual
  storage/send behavior plus the merged capability manifests. Acceptance: every declared data
  type traces to a `capability.json` entry or to the slot's own storage, and nothing sends
  off-device without the disclosure required by the spec.

## 10. Store pack (Store)

- [ ] 10.1 **[Store]** Generate the pack with `python scripts/store-kit.py` for the slot.
  Acceptance: all paths in `AGENTS.md` *Store-pack contract* exist.
- [ ] 10.2 **[Store]** Write the listing: title ≤ 30, short description ≤ 80, full
  description ≤ 4000 characters, claims matching the shipped app, no cure/guarantee language.
  Acceptance: `node scripts/verify-slot.mjs --slot plant-health-assistant --strict-store`
  reports PASS.
- [ ] 10.3 **[Store]** Produce the assets: 512×512 icon, 1024×500 feature graphic, 6 phone
  screenshots at 1080×1920, 4 seven-inch at 1200×1920, 4 ten-inch at 1600×2560, each locale.
  Acceptance: the strict-store gate reports PASS with every asset present at its exact size.
- [ ] 10.4 **[Store]** Write `review-notes.md` with the path to reach a diagnosis without
  login gates and note that captured screenshots are synthetic if they are. Acceptance: a
  reviewer can follow the notes to a diagnosis; synthetic media is labelled as synthetic.

## 11. Release artifacts

- [ ] 11.1 **[Verify]** Build and stage signed artifacts:
  `node scripts/prepare-release.mjs --slot plant-health-assistant`. Acceptance: a signed
  release APK, a signed release AAB, and a release manifest are produced, and the command
  prints the build JDK and the build time (`build-speed.mdc`).
- [ ] 11.2 **[Verify]** Inspect the artifacts for a shipped credential. Acceptance: the
  secret-guard check reports no secret, and the APK contains no hosted-service API key
  (spec: *No credential ships in the APK*).

## 12. Verification gates (evidence, not self-report)

- [ ] 12.1 **[Verify]** Run `node scripts/verify-slot.mjs --slot plant-health-assistant
  --strict-store --build` and read its output. Acceptance: PASS. A build-less run is
  `UNVERIFIED` (exit 2) and must not be cited as release evidence (`AGENTS.md` *Verification*).
- [ ] 12.2 **[Verify]** Run `node scripts/selftest.mjs`. Acceptance: exit 0.
- [ ] 12.3 **[Verify]** Read the diff before accepting PASS; no worker self-report counts as
  proof (`AGENTS.md` *Agent model*). Acceptance: the reviewer states what they read.
- [ ] 12.4 **[Verify]** Run `/eval` and repair until green. Acceptance: an accepted
  independent verdict.
- [ ] 12.5 **[Verify]** Run `node scripts/kiln-status.mjs`. Acceptance: the slot reports
  healthy and the factory reports no regression.

## 13. Compile gate

- [ ] 13.1 **[Build]** End with the slot compiling — run `./gradlew assembleDebug` in
  `apps/2026-09-23-plant-health-assistant/` as a **single Gradle invocation** (do not spawn one
  process per task; `build-speed.mdc`). Acceptance: `BUILD SUCCESSFUL`, no configuration-cache
  or Kotlin-plugin error, and the elapsed time is recorded.
