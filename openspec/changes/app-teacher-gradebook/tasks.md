# Tasks: app-teacher-gradebook

## Recorded decisions

- [x] The slot is `apps/2026-09-26-teacher-gradebook`; it is new and independent, and it does not
  touch `apps/2026-09-25-pipe-sizing`.
- [x] v1 is offline, account-free, server-free, camera-free, ad-free and subscription-free, and
  requests **no** Android permission — not even `INTERNET`.
- [x] The commercial surface is one non-consumable Play Billing unlock at $6.99, offered when a
  second class is created or the first export is attempted.
- [x] **The record surfaces beyond grades are first-class, not extras.** Attendance, participation
  and behaviour notes, missing-work tracking, a parent-contact log and a seating chart ship in
  v1. **Reason:** the measured incumbent complaints are CRUD, crashes, a stranded purchase and
  subscription resentment (`docs/research/2026-09-26-teacher-gradebook-evidence.md` §2.3), and
  the record surfaces are what make the product a record system rather than arithmetic over typed
  numbers (`.cursor/rules/ideation-gate.mdc`).
- [x] **Persistence is one versioned JSON document, not a database.** **Reason:** the internal
  store, the backup file and the test fixtures become one format, so "back up and restore
  everything" is one code path; a teacher's data stays far below a megabyte, so a database would
  add migration code without adding capability.
- [x] The app makes no claim to replace, sync with, or be certified by any SIS or LMS, and bundles
  no grade-scale, standard or curriculum data.
- [ ] **[Human] Resolve the child-directed / Families Policy question** for a listing that stores
  student names and grades locally, against the live policy text, before the store pack is
  written.

## 1. Domain core and its tests

- [ ] **[Build] Implement the record model and store.** Classes, categories, assignments,
  students, grade cells (`Score` / `Missing` / `Exempt`), attendance, notes, contacts, seating and
  a user-defined grade scale, serialised as one versioned JSON document written atomically.
  **Verification:** unit tests round-trip a populated store and reject an unsupported
  `schemaVersion`.
- [ ] **[Build] Implement create, edit and delete for every entity.** Deletion is permanent; a
  multi-record destructive action requires a confirmation naming what is removed. **Verification:**
  tests delete a class, a student, an assignment and a grade cell and assert each is absent after
  a reload, and that a deleted grade cell does not return as zero.
- [ ] **[Build] Implement the grade arithmetic contract exactly as designed.** Per-assignment
  percent, `dropLowest`, category mean over surviving percentages, weight-weighted overall over
  categories with usable data, `Exempt` excluded, and the not-enough-data state. Non-positive
  points are rejected. **Verification:** independently computed fixtures cover empty category,
  all-exempt category, drop-lowest, each rounding mode and the no-data state; a fixture asserting
  `NaN` or `0` for no data fails.
- [ ] **[Build] Implement CSV import, CSV export and PDF export.** Roster import reports imported
  and skipped counts with reasons; export uses RFC 4180 quoting and writes no placeholder for an
  empty cell; PDF uses the framework `PdfDocument`. **Verification:** tests assert quoting for
  names containing a comma, a quote and a newline, and that an empty cell exports empty.
- [ ] **[Build] Implement backup and restore.** Validate version and structure before applying
  and refuse an unsupported or malformed document without changing anything. **Verification:**
  tests round-trip a full record and assert a refused restore leaves the existing record
  byte-identical.
- [ ] **[Build] Implement the free-tier boundary as a domain rule.** One class, up to 20 students,
  unlimited grades; the unlock lifts both limits. **Verification:** tests assert the blocked
  actions return a stated limit reason and that granting the entitlement lifts both.

## 2. UI, offline posture and billing

- [ ] **[Build] Implement the Compose screens.** Class list; class detail with roster and
  assignments; grade grid that does not fight the keyboard; student detail with average
  breakdown; category and weight editor; attendance sheet; notes; parent contacts; seating chart;
  export and backup; settings; one paywall sheet. Business logic stays in the ViewModel.
  **Verification:** `./gradlew assembleDebug` succeeds and the debug APK installs.
- [ ] **[Build] Keep the app offline and permission-free.** No `INTERNET` or other permission in
  the manifest; file access through the Storage Access Framework; sharing through the Sharesheet;
  backup rules separate cloud backup from device transfer. **Verification:** the merged manifest's
  requested permissions are read and recorded in `store/checklist.md` as empty.
- [ ] **[Build] Enable the `billing` capability the archetype requires.** Add `billing` to the
  slot's `capabilities.json`, wire the unlock through the capability with a persisted entitlement
  store, acknowledge pending purchases on cold start, and show the real localized price.
  **Verification:** `node scripts/check-economics.mjs --slot teacher-gradebook` prints
  `VERDICT: PASS`.
- [ ] **[Build] Exercise accessibility, edge-to-edge and predictive back.** Editable fields, the
  not-enough-data state and the paywall at large font scale with screen-reader semantics.
  **Verification:** dated device or emulator observations recorded in `store/checklist.md`.

## 3. Store, release and factory gates

- [ ] **[Store] Generate and complete the truthful store pack.** Run
  `python scripts/store-kit.py --slot teacher-gradebook`; write listing copy that claims only
  shipped behaviour, states the one-time price and what the free tier includes, and describes
  the app as a private local record that does not sync with a school system. **Verification:**
  `node scripts/verify-slot.mjs --slot teacher-gradebook --strict-store` passes; this build-less
  check is not release evidence.
- [ ] **[Human] Approve Play declarations.** Data safety, privacy policy, in-app purchase answer,
  content rating, target audience and reviewer access. **Proof:** dated approvals in
  `store/checklist.md`; no form is submitted automatically.
- [ ] **[Human] Authorize signing.** No keystore exists for this slot; generating one creates the
  app's permanent identity and its password is the owner's to choose and store. **Proof:** an
  explicit instruction in the conversation, then
  `node scripts/sign-release.mjs --slot teacher-gradebook`.
- [ ] **[Verify] Prepare signed release artifacts.**
  `node scripts/prepare-release.mjs --slot teacher-gradebook`. **Verification:** signed APK,
  signed AAB and `dist/release-manifest.json` exist and signature checks pass.
- [ ] **[Verify] Run the release evidence gate.**
  `node scripts/verify-slot.mjs --slot teacher-gradebook --strict-store --build` and
  `./gradlew assembleDebug` in the slot. **Verification:** exit 0 and PASS, with the complete
  output read rather than sampled.
- [ ] **[Verify] Read the complete change and slot diff, then run `/eval`.** Repair until the
  independent evaluator accepts the implementation. **Proof:** recorded evaluator verdict and
  reviewer diff findings.
- [ ] **[Human] Perform the first upload and publish.** The human compares signing fingerprints,
  completes the declarations, uploads the first AAB, and publishes only after the evidence above
  agrees.
