# Tasks: app-unit-walk

## Recorded decisions

- [x] The slot is `apps/2026-10-05-unit-walk`; new and independent, built from its own plan,
  its own promise contract and the shared harness. No sibling slot's code is read
  (`.cursor/rules/ideation-gate.mdc` § "The factory is not the market").
- [x] **The owner approved the plan at checkpoint 1 on 2026-10-05.** The archetype
  `purchase_led_single_unlock`, the `$9.99` price, the moment of payment, the capabilities and the
  kill criteria are the ones in `apps/2026-10-05-unit-walk/docs/unit-walk.economics.json`; this change matches
  them and does not restate a different number.
- [x] **No account, no login and no server.** **Reason:** the promise that carries the slot is that a
  walk opens and finishes on the phone the landlord already owns, and the measured complaint classes
  on this shelf are forced account/login (Property Inspect) and a server blocking the handoff. The
  launch copy says *no account and no network in the daily path* — **not** "the app is incapable of
  using the network".
- [x] **No ads, and no subscription at any point.** The commercial surface is **one** non-consumable
  Play Billing unlock at **$9.99**, offered when the landlord starts a **second unit** or asks for a
  **PDF export without the watermark**. No trial clock, no renewal wording, no second paywall, and
  nothing consumed. The free tier is one unit walk that is fully usable.
- [x] **`CAMERA` is the app's own runtime permission; it is the only one.** The Photo Picker
  (`ActivityResultContracts.PickVisualMedia`) needs none, and share/SAF needs no storage permission on
  modern Android. The `billing` capability's Play Billing dependency may contribute `INTERNET` /
  `ACCESS_NETWORK_STATE` and an internal `TransportBackendDiscovery` service to the **merged**
  manifest, so the merged permission list is **measured from the built artifact**, never asserted
  (same disclosure pattern as the reference slot, measured there by `aapt2 dump permissions`).
- [x] **Persistence is one versioned JSON document plus the photo files, written atomically before the
  next room opens.** **Reason:** the draft store, the report and the unit-test fixtures become one
  shape, so "the walk survives a process kill" is one code path; a walk is far below a megabyte of
  notes plus its images.
- [ ] **[Human] Approve the product at checkpoint 1.5.** The agent installs and tests the debug APK
  on the connected device (`.cursor/rules/device-testing.mdc`); the owner's decision is the product,
  not the installation. Nothing in phase 2 runs before that: `node scripts/approve-release.mjs
  --slot unit-walk --note "<the owner's own words>"`.

## 1. Domain core, draft store and their tests — worker 1

Base package `com.gexetr.unitwalk`.

- [ ] **[Build]** `domain/Model.kt` — `Unit(id, name, address, notes, createdAt)`,
  `Walk(id, unitId, kind, startedAtIso, finishedAtIso, status, rooms, differences, signatures)`,
  `enum class WalkKind { MOVE_IN, MOVE_OUT, SINGLE }`,
  `enum class WalkStatus { DRAFT, FINISHED }`,
  `Room(id, name, note, photos)`, `Photo(id, fileName, capturedAtIso, source)`,
  `enum class PhotoSource { CAMERA, PICKER }`, `RoomDelta(roomId, condition, note)`.
- [ ] **[Build]** `domain/WalkDraft.kt` — pure functions that mutate a draft room by room and return
  the next durable snapshot: `openRoom`, `addPhoto`, `setNote`, `finishRoom`, `finishWalk`. Every
  mutation MUST produce a snapshot that is written before the next room is opened.
- [ ] **[Build]** `domain/Compare.kt` — pure functions that link a move-out walk to its unit's prior
  move-in walk and compute per-room deltas; a unit with no prior walk reports "no prior report"
  rather than fabricating one.
- [ ] **[Build]** `domain/Report.kt` — builds the report description (unit, kind, dates, rooms,
  photo references, notes, deltas, signatures) consumed by the PDF writer; framework-free so it is
  testable on the JVM.
- [ ] **[Build]** `data/WalkStore.kt` — JSON persistence in the slot's own private files directory,
  versioned document, atomic write (temp file + rename), photo bytes written to app-private files
  before the JSON row is committed. A tolerant read of a missing or corrupt file returns an empty
  state rather than crashing.
- [ ] **[Test]** `app/src/test/java/com/gexetr/unitwalk/domain/` — the fixtures that make each
  promise falsifiable: a completed room reloads after a simulated process kill; a half-written
  snapshot reads as the last good state; move-out compares to the linked move-in and reports a delta
  per room; a unit with no prior walk reports no prior report; report bytes are non-empty and carry
  every room.
- [ ] **[Gate]** `node scripts/run-slot-tests.mjs --slot unit-walk --rerun`.

## 2. Design system, shell and settings — worker 2

- [ ] **[Build]** `ui/theme/Color.kt`, `ui/theme/Shape.kt`, `ui/theme/Type.kt`, `ui/theme/Theme.kt` —
  the slot's own palette, taken from the slot's `store/studio.json` `palette` block so the store pack
  and the app cannot drift, with an explicit light and dark mapping and **no** dynamic colour and no
  default purple seed.
- [ ] **[Build]** `ui/components/Brand.kt` — the slot's own mark glyph drawn in Compose, plus the
  reusable vocabulary: a walk-progress rail, a room-condition chip that states condition in a word
  **and** a shape, a photo thumbnail row, a stat tile, an empty state, a lock/paywall row.
- [ ] **[Build]** `ui/UnitWalkApp.kt`, `MainActivity.kt` — the destination host and its own back
  stack, edge-to-edge, window size class read for the wide layout; predictive back enabled.
- [ ] **[Build]** `data/SettingsStore.kt` + `ui/settings/SettingsScreen.kt` +
  `ui/settings/SettingsViewModel.kt` — a settings surface reachable from the main screen where
  **every** preference changes observable behaviour (candidate set: default walk kind, photo quality,
  keep-original photo after report, watermark on the free export, confirm-before-delete), persisted,
  no dead setting.
- [ ] **[Build]** `res/values/strings.xml` + `res/values-ru/strings.xml` — every user-facing string
  is a resource from the first screen; **no** hardcoded prose in Kotlin.
- [ ] **[Build]** `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml` — cloud backup and
  device transfer separated; the walk is the landlord's own data and no secret is stored.
- [ ] **[Gate]** no screen renders a hardcoded user-facing string; both locales carry every key.

## 3. Screens — worker 3

- [ ] **[Build]** `ui/units/UnitsScreen.kt` + `UnitsViewModel.kt` — the portfolio: create, rename and
  delete a named unit; each unit shows its own walks; **start a walk from a named unit**. Every
  control ≥ 48 dp; nothing carries meaning by colour alone.
- [ ] **[Build]** `ui/walk/WalkScreen.kt` + `WalkViewModel.kt` — the room-by-room walk: the room list,
  the current room, camera capture (`TakePicture` / `ACTION_IMAGE_CAPTURE`) and gallery attach
  (`PickVisualMedia`), the room note, finish-room and finish-walk. Each photo and note is committed to
  the draft store before the next room opens; a resumed draft restores the same position and photos.
- [ ] **[Build]** `ui/compare/CompareScreen.kt` + `CompareViewModel.kt` — move-in versus move-out for
  one unit: open the linked prior report beside the new walk and record a per-room delta.
- [ ] **[Build]** `ui/report/ReportScreen.kt` + `ReportViewModel.kt` — the finished report preview,
  the signature entry (landlord / tenant names), Share (`ACTION_SEND` + `FileProvider`) and Save
  (`ACTION_CREATE_DOCUMENT`), and the app's report of what it wrote. Delete of a unit or a walk
  requires a confirmation that names the item.
- [ ] **[Build]** `ui/unlock/UnlockScreen.kt` + `UnlockViewModel.kt` — one sheet, one price, no trial
  clock, no subscription wording, restore path.
- [ ] **[Gate]** no screen renders a hardcoded user-facing string.

## 4. Platform, capabilities and store drafts

- [ ] **[Build]** `app/build.gradle.kts` and `app/src/main/AndroidManifest.xml` — billing wired for
  one non-consumable product; `CAMERA` is the only `uses-permission` the app's own manifest declares;
  `FileProvider` declared with a narrow `file_paths` for the report PDF.
- [ ] **[Build]** `capabilities.json` — `billing` enabled, and only `billing`; ads off.
- [ ] **[Build]** `store/studio.json` — the slot's declared identity: `mark`, the full `palette` (the
  same colours as `ui/theme/Color.kt`), per-shot `headlines`/`kickers`, `featureTagline`,
  `featurePill`. Written **before** the UI is styled.
- [ ] **[Build]** `store/listing.json`, `store/listing.md`, `store/privacy.md`,
  `store/data-safety.md`, `store/review-notes.md`, `store/release-notes.md` — drafts. Data safety
  declares **no** collection, because the walk never leaves the device and the app's own code neither
  reads nor writes the network. The claim is about **the slot's code, not the merged manifest**: the
  Play Billing dependency can contribute `INTERNET` / `ACCESS_NETWORK_STATE` and an internal
  `TransportBackendDiscovery` service, so the merged release manifest is **read from the built
  artifact** (measured the same way the reference slot measured its own four permissions), and both
  the privacy policy and the data-safety sheet state this in the same words.
- [ ] **[Gate]** `node scripts/check-economics.mjs --slot unit-walk` — the plan, the archetype and the
  enabled `billing` capability agree; a plan the build cannot honour fails here.

## 5. Verification and the stop

- [ ] **[Gate]** `node scripts/check-economics.mjs --slot unit-walk` and
  `node scripts/check-differentiators.mjs --slot unit-walk` — the latter only after every promise
  leaves `deferred`; never with `--allow-deferred`.
- [ ] **[Gate]** `node scripts/run-slot-tests.mjs --slot unit-walk --rerun`.
- [ ] **[Gate]** `node scripts/verify.mjs --slot unit-walk --build` — compiles `assembleDebug`
  (`app/build/outputs/apk/debug/app-debug.apk`) and does not sign.
- [ ] **[Gate]** every promise in `differentiators.json` resolved — `shipped` with a proof that
  resolves on disk, or `narrowed`/`dropped` in writing. **`verify.mjs` fails while any is
  `deferred`**, and the five promises are the reason this slot was chosen.
- [ ] **[Gate]** `node scripts/check-client-secrets.mjs --slot unit-walk` and
  `node scripts/check-in-app-policy.mjs --slot unit-walk` (the latter once a policy URL exists).
- [ ] **[Human] CHECKPOINT 1.5** — the agent has installed and tested
  `app/build/outputs/apk/debug/app-debug.apk` on the connected device (serial, artifact hash and
  observed screens in the handoff); the owner approves the product, or asks for changes.

**Nothing in phase 2 runs here.** `prepare-release.mjs`, `scripts/store/render.py` in its rendering
modes and `media-capture.mjs` all refuse without a recorded approval, and working around that refusal
is the failure this phase exists to prevent.
