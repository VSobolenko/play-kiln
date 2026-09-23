# Tasks — add-visual-schedule-slot

`[Human]` is a decision or Play Console action the factory must not perform. `[Build]` creates slot
behaviour. `[Store]` creates release-facing evidence. `[Verify]` is an independent gate.

**Status: do not commit this slot.** The twelve-phrasing evidence gate failed to establish either a
feature gap or an observable execution gap. Groups 2–9 are a dormant implementation plan and SHALL
NOT start unless task 1.1 obtains direct installed-product evidence and a human explicitly records
that the do-not-commit decision has been reversed.

## 1. Human gates — before scaffolding

- [ ] 1.1 **[Human] Reverse the do-not-commit decision only from direct product evidence.** Install
  and use the current versions of the 4.9-star / 500K+ direct incumbent, the 4.8-star specialist
  ranked first on the target query, and the relevant low-rated incumbents. Record the date, locale,
  versions, flows exercised, and one sentence naming a capability or observable execution quality
  this board provides that those products do not. Ratings, listing copy, an unrated app, or a
  general promise to execute better is insufficient. If direct use cannot support the sentence,
  leave this change unimplemented. **Proof artifact:** replace the `UNVERIFIED / blocking` item in
  `proposal.md` only with the installed-product evidence, the approved sentence, and an explicit
  human reversal of the do-not-commit decision.
- [ ] 1.2 **[Human] Approve the children-audience declaration.** Record the exact Play target age
  groups, acknowledging that the child directly uses the board and that “parents/carers only” is
  not a safe shortcut (policy research §1). **Proof artifact:** a dated target-audience decision in
  this file's *Recorded decisions* section, reviewed against the live Console wording.
- [ ] 1.3 **[Human] Approve claim-free store text in both locales.** Review title, short description,
  full description, screenshots, and translations for literal utility wording only; the
  health-claim path is out of scope. **Proof artifact:** approved `store/listing.md` and
  `store/listing.json`, plus a dated approval line in `store/checklist.md`; later verified by
  `node scripts/verify-slot.mjs --slot visual-schedule --strict-store`.
- [ ] 1.4 **[Human] Approve the expected Data safety and SDK surface.** Record that routine content
  stays on-device, identify every SDK and data type, and choose either a free launch or the optional
  one-time non-consumable product. **Proof artifact:** a dated decision in *Recorded decisions* and
  reviewed `store/data-safety.md`; if monetised, include the exact Play product id, otherwise record
  `billing omitted`.

### Recorded decisions

- **THE EVIDENCE GATE FAILED — do not commit the slot (recorded 2026-09-24).** The twelve-phrasing
  re-measurement in `docs/research/2026-09-23-visual-schedule-evidence.md` supersedes the earlier
  single-query reading. A 4.9-star / 500K+ direct incumbent and a 4.8-star specialist ranked first
  on the target query mean this slot is a bet to out-execute two already-good apps. The largest
  direct bucket observed across the run is 500K+ and the largest bucket on the exact target query
  is 100K+, materially below the plant vertical's 10M+ and 50M+ owners. Thirteen of 17 direct apps
  are unrated at no more than 1K+ installs; that tail is equally consistent with an open niche and
  a graveyard. Store pages show no feature gap and cannot verify why the weak apps are weak, so the
  recommendation changed from proceed to the human gate to **do not commit**. Only task 1.1 can
  reopen this plan.
- **Artifact qualification (recorded 2026-09-24):** spot checks of
  `.kiln/visual-schedule/screen.json`, `.kiln/visual-schedule/enrich-full.json`, and
  `.kiln/visual-schedule/listing.txt` support the ratings, target-query rank, install buckets, IAP
  badges, RoutinePals subscription listing, and 13-of-17 tail. They do not support the evidence
  document's stronger wording that RoutinePals and Little Planner *launched* in August–September
  2026 or that both shipped subscriptions then. The artifacts record 100+ installs, update dates of
  Sep 11 and Aug 14, 2026, and IAP badges; only RoutinePals' listing identifies a subscription.
- **What the new evidence did not change:** the product definition, child-inclusive audience,
  claim-free/local-only boundary, human-owned Play declarations, and reserved
  `com.gexetr.visualschedule` identity remain the implementation contract if task 1.1 ever reopens
  the change. Paying demand remains UNVERIFIED.
- **Spec reconciliation (recorded 2026-09-24):** the winning capability remains
  `specs/visual-schedule-board/spec.md`; the duplicate capability was merged forward and then
  removed. Requirement-by-requirement disposition:
  - **Routine authoring from picture steps — merged in part.** Persistence after reopening,
    independent multiple routines, and deletion isolation are stronger, consistent assertions.
    Its generic user wording was dropped in favour of the adult-author/child-board split.
  - **Deterministic full-screen presentation — merged in part.** Exact authored sequence and no
    addition, omission, or substitution strengthen determinism. Full-screen one-step-only display
    was dropped because the approved design shows the whole ordered board with one prominent current
    card.
  - **Step timer — dropped.** It contradicts `design.md` D3, which explicitly prohibits timers and
    requires direct-tap-only progression.
  - **Accessibility is required behaviour — merged.** Large-font and disabled-animation behaviour,
    explicit touch-target measurement, and labelled/operable controls strengthen the retained
    accessibility requirement and agree with `design.md` D9.
  - **Offline operation with no account and no collection — merged in part.** No account/sign-in and
    the transmitting-SDK inspection strengthen the local-only requirement. Timer language was
    dropped with the timer requirement.
  - **Pictures are chosen through the system picker — dropped as subsumed.** The retained spec is
    stricter: it requires Android's Photo Picker and names the forbidden broad permissions.
  - **The user's routine data is deletable — merged in part.** Deleting one routine while preserving
    the others is a stronger missing scenario. The remaining deletion behaviour was already present.
  - **No claim about a condition, a development, or an outcome — merged in part.** Explicit checks
    of in-app copy, bundled content, and store metadata strengthen the retained all-surface rule.
  - **Any commercial surface is truthful, non-coercive, and non-blocking — merged.** Visible price
    and effect, no pressure tactics, and preservation of existing routines fill observable gaps in
    the retained adult-only commercial requirement.
  - **No generated content — merged in part.** The no-prompt/no-generate/no-suggestion scenario is
    a stronger observable check; the authored-content prohibition was already present.
  - **Play compliance floor applies — dropped as subsumed.** The retained factory-gate requirement
    is stricter because it requires strict-store build verification, signed artifacts, and explicit
    human ownership of declarations.

- **Recorded 2026-09-23:** slot identity is `visual-schedule` /
  `com.gexetr.visualschedule` / `apps/2026-09-23-visual-schedule/`; display name is **Picture
  Routine Board** (`scripts/lib/namespaces.mjs`; `AGENTS.md`).
- **Recorded 2026-09-23:** an adult authors every picture, label, and order; a child directly uses
  the deterministic board. Audience submission remains human-owned and must include the child use
  honestly (policy research §1).
- **Recorded 2026-09-23:** health-claim metadata and functionality are out of scope. All copy is a
  literal description of a picture routine board; the Health apps answer comes from the shipped
  build and live form (policy research §2 and §7.2).
- **Recorded 2026-09-23:** routine data stays on-device and out of both backup modes; Photo Picker
  is the only image-acquisition path; camera, broad media, location, microphone, notification,
  exact-alarm, and advertising-ID permissions are out of scope (policy research §5).
- **Recorded 2026-09-23:** no ads. If task 1.4 approves monetisation, use Play Billing for one
  permanent non-consumable adult-authoring unlock. No consumable ships because no spendable unit is
  defined. No price or conversion assumption is recorded (policy research §§3–4;
  `docs/monetization-strategy.md` §§3.1–3.6).
- **UNVERIFIED / blocking:** feature-level distinctness and the asserted execution gap; the expanded
  store evidence cannot compare shipped behaviour or explain weak ratings
  (`docs/research/2026-09-23-visual-schedule-evidence.md`, sections 4 and 6).
- **UNVERIFIED / human-owned:** live Designed for Families relationship, Console classification of
  the claim-free utility, legal purchase-consent mechanics, paying demand, and any legal duty beyond
  the researched Play-policy text (policy research, *Open items marked UNVERIFIED*).

## 2. Create the isolated slot

- [ ] 2.1 **[Build] Create the slot only after group 1 passes.** Run:
  `node scripts/new-slot.mjs --slug visual-schedule --name "Picture Routine Board" --idea "An
  offline picture routine board authored by a carer and followed by a child." --date 2026-09-23`.
  **Verification:** `node scripts/verify-slot.mjs --slot visual-schedule` identifies
  `apps/2026-09-23-visual-schedule/` and applicationId `com.gexetr.visualschedule`; a build-less
  result is evidence for structure only, never release PASS.
- [ ] 2.2 **[Build] Confirm isolation and factory defaults.** Keep one `:app` module, Compose +
  Material 3, the AGP 9 built-in Kotlin setup, and no copied factory or machine-local state.
  **Verification:** `node scripts/verify-slot.mjs --slot visual-schedule` reports no isolation,
  namespace, SDK, or forbidden-default failure.

## 3. Domain and local persistence

- [ ] 3.1 **[Build] Implement the authored domain model and pure operations.** Add `Routine`,
  `RoutineStep`, and `BoardProgress`; validate content, normalize order, select current, complete,
  back, restart, and finish without mutating the saved definition. **Verification:** from
  `apps/2026-09-23-visual-schedule/`, run `./gradlew :app:testDebugUnitTest`; tests cover reorder,
  deletion, first-incomplete selection, bounds, completion, and restart.
- [ ] 3.2 **[Build] Implement the local repository and app-owned picture copies.** Persist routine
  metadata and progress locally; copy Photo Picker content to private storage; cascade deletion to
  records, progress, and image files. **Verification:** `./gradlew :app:testDebugUnitTest` passes a
  repository recreation test and a two-routine deletion test proving the untouched routine remains.
- [ ] 3.3 **[Build] Exclude routine content from cloud backup and device transfer.** Write separate
  `cloud-backup` and `device-transfer` rules covering database, preferences, and picture paths.
  **Verification:** `node scripts/verify-slot.mjs --slot visual-schedule --build` finds both rule
  sections and their referenced XML; inspect the XML artifact to confirm every routine-data path is
  excluded.
- [ ] 3.4 **[Verify] Prove there is no inference or remote routine path.** Inspect main source,
  dependencies, and manifests for model, generation, sync, analytics, and remote-config code.
  **Verification:** `rg -n -i "model|inference|generate|prompt|upload|sync|analytics|remote.?config"
  apps/2026-09-23-visual-schedule/app/src/main` produces no unexplained runtime path; record any
  matches and their disposition in `store/checklist.md`.

## 4. Adult authoring and empty states

- [ ] 4.1 **[Build] Implement routine list and no-data state.** Create, rename, select, and delete
  routines; when none exist, show adult instructions and a create action. **Verification:**
  `./gradlew :app:connectedDebugAndroidTest` passes tests for fresh install, two independent
  routines, deletion confirmation, and empty state.
- [ ] 4.2 **[Build] Implement step editing.** Add, label, replace, reorder, and remove picture
  steps; prevent presentation of zero-step routines and identify an unreadable picture.
  **Verification:** `./gradlew :app:connectedDebugAndroidTest` passes editor tests for each action,
  zero-step start, and unavailable-picture recovery.
- [ ] 4.3 **[Build] Use the system Photo Picker only.** Wire
  `ActivityResultContracts.PickVisualMedia`; do not add camera or media-read permission.
  **Verification:** `node scripts/verify-slot.mjs --slot visual-schedule --build` plus a merged
  manifest read shows no `CAMERA`, `READ_MEDIA_IMAGES`, or `READ_EXTERNAL_STORAGE`; the UI test
  attaches only the selected image.

## 5. Child-facing board and accessibility

- [ ] 5.1 **[Build] Implement deterministic board presentation.** Show all cards in authored order,
  one current step, checkmarked completed steps, remaining steps, explicit complete/back actions,
  and finished restart/exit actions. **Verification:** `./gradlew
  :app:connectedDebugAndroidTest` presents a five-step routine and asserts every state transition
  and that the saved definition remains unchanged.
- [ ] 5.2 **[Build] Keep commerce and editing out of the board.** The child-facing route contains no
  price, offer, paywall, ad, purchase action, rename, reorder, or delete control. **Verification:**
  `./gradlew :app:connectedDebugAndroidTest` inspects the board semantics tree for those actions;
  `rg -n -i "billing|purchase|price|paywall|adview"` over the board source has no unexplained match.
- [ ] 5.3 **[Build] Implement accessibility requirements.** Use ≥48dp touch targets; expose label,
  position, state, and action; provide checkmark/text/semantics in addition to colour; support large
  font scale and disabled animations. **Verification:** `./gradlew
  :app:connectedDebugAndroidTest` runs semantics, touch-bound, large-font, and animation-disabled
  tests; a human screen-reader pass is recorded in `store/checklist.md`.
- [ ] 5.4 **[Build] Verify edge-to-edge and predictive back.** Exercise authoring and board routes on
  API 36 with system bars, large font, and predictive-back preview. **Proof artifact:** a dated
  device/emulator observation in `store/checklist.md`; the later strict build gate must remain
  green.

## 6. Dependencies, permissions, and commercial surface

- [ ] 6.1 **[Build] Wire only approved capabilities.** Add required `core`; omit ads, telemetry,
  remote configuration, analytics, and every unapproved SDK. Add `billing` only if task 1.4 names
  the product id. **Verification:** `./gradlew :app:dependencies` is saved/read and the merged
  manifest contains no `AD_ID`, Firebase component, analytics component, or unexpected permission.
- [ ] 6.2 **[Build] Implement the approved billing branch or omit it.** For a monetised build,
  implement one acknowledged, restorable, never-consumed entitlement that affects only creation of
  future routines and never hides existing ones. For a free build, include no billing dependency or
  UI. **Verification:** `./gradlew :app:testDebugUnitTest` covers purchase, restore, pending/failure,
  and preservation of existing routines; or the dependency report proves billing absent.
- [ ] 6.3 **[Verify] Audit the permission list against observable behaviour.** There must be no
  camera, broad media, location, microphone, contacts, notification, exact-alarm, or advertising-ID
  permission. **Verification:** `node scripts/verify-slot.mjs --slot visual-schedule --build`
  reports no unexpected permission, and the merged permission list is copied into
  `store/checklist.md`.

## 7. Claim, privacy, and declaration evidence

- [ ] 7.1 **[Store] Write claim-free app and store copy.** Describe only create/choose/order/show/
  complete/restart/delete behaviour in `en-US` and `ru-RU`. **Verification:** run `rg -n -i
  "autis|ASD|ADHD|special needs|therap|treat|diagnos|clin|develop|learn|educat|behavio|communicat|calm|anxi|meltdown|improv|reduc|help|support|cure"
  apps/2026-09-23-visual-schedule/app/src/main apps/2026-09-23-visual-schedule/store`; every match
  is read, and the approved shipped copy has no condition, benefit, or outcome assertion.
- [ ] 7.2 **[Store] Derive privacy and Data safety from the build.** Reconcile local storage,
  backup exclusions, dependencies, merged manifest, and billing before writing `store/privacy.md`
  and `store/data-safety.md`. **Proof artifact:** a trace table in `store/checklist.md` maps each
  SDK/data type to its source and states that routine content has no off-device path; human approval
  from task 1.4 is attached.
- [ ] 7.3 **[Human] Approve Play declarations.** Review target audience, Health apps, ads, Data
  safety, content rating, permissions, privacy-policy URL, and reviewer access against the shipped
  app. **Proof artifact:** dated approvals in `store/checklist.md`; the factory does not submit the
  forms (`docs/play-publishing-playbook.md` §7).

## 8. Store pack and release artifacts

- [ ] 8.1 **[Store] Generate and complete the store pack.** Run `python scripts/store-kit.py` for
  `visual-schedule`, then supply all listing, policy, review-note, icon, feature-graphic, phone, and
  tablet artifacts required by `AGENTS.md`. **Verification:** `node scripts/verify-slot.mjs --slot
  visual-schedule --strict-store` reports PASS; this build-less result validates the store pack but
  is not release evidence.
- [ ] 8.2 **[Store] Verify screenshots and review access.** Screenshots show only shipped UI and
  literal behaviour; synthetic captures are labelled; `review-notes.md` reaches a routine without
  login. **Proof artifact:** a human asset/copy read recorded in `store/checklist.md`.
- [ ] 8.3 **[Verify] Prepare signed artifacts.** Run `node scripts/prepare-release.mjs --slot
  visual-schedule`. **Verification:** signed release APK, signed release AAB, and
  `dist/release-manifest.json` exist and the signature check succeeds.

## 9. Submission and final gates

- [ ] 9.1 **[Human] Re-read live policy pages immediately before submission.** Open every primary
  page listed in policy research under *Primary sources read (2026-09-23)* that governs Families,
  target audience, health claims/declaration, payments, user data/Data safety, permissions, content
  rating, and privacy. **Proof artifact:** `store/checklist.md` records review date, URL, whether the
  operative text changed, and the resulting edit or `no change`; every research UNVERIFIED item
  remains labelled unless a primary source now resolves it.
- [ ] 9.2 **[Verify] Run release evidence gate.** Run `node scripts/verify-slot.mjs --slot
  visual-schedule --strict-store --build`. **Verification:** exit 0 and PASS; read the full output.
  A build-less run or exit 2 is `UNVERIFIED`, not PASS.
- [ ] 9.3 **[Verify] Run factory regression tests.** Run `node scripts/selftest.mjs`.
  **Verification:** exit 0 with every self-test passing.
- [ ] 9.4 **[Verify] Read the complete change and slot diff, then run `/eval`.** Check that only the
  intended isolated slot and approved OpenSpec/store evidence changed and repair until the
  independent verdict accepts it. **Proof artifact:** the evaluator verdict and the reviewer's
  written diff findings.
- [ ] 9.5 **[Verify] Check factory health.** Run `node scripts/kiln-status.mjs`. **Verification:**
  the slot is healthy and no factory regression is reported.
- [ ] 9.6 **[Human] Perform first upload and publish.** Register the exact applicationId, compare
  signing fingerprints, upload the first AAB, submit the reviewed declarations, and press Publish
  only after all evidence agrees. **Proof artifact:** Play Console confirmation retained by the
  human; these actions are never automated by this change (`docs/play-publishing-playbook.md`
  §§3–5, 7, 11).
