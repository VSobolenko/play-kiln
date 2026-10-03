# Tasks: app-laneset

Slot: `apps/2026-10-02-laneset` — **Laneset: Swim Set Builder** — `com.gexetr.laneset`.
Contracts: `economics.json` (`purchase_led_single_unlock`, `billing` required, no ads),
`differentiators.json` (7 promises, 3 core), `naming.json` (name/slug/applicationId).

**One build policy.** Gradle is serial per slot (`.cursor/rules/build-speed.mdc`): all tasks for one
build go to **one** invocation, and the orchestrator runs it, not this document. The one authoritative
phase-1 build is `node scripts/verify.mjs --slot laneset --build` (which runs `assembleDebug`). No
build has been run yet for this slot at the time of writing — every "done" mark below is about a file
on disk, not about a compiled or tested artifact.

Legend: `[x]` done and MEASURED on disk in this session; `[ ]` pending; `[~]` in flight (files on disk,
not integrated); `[BLOCKED]` requires the owner's recorded release approval
(`.cursor/rules/release-gate.mdc`) or a device.

## 0. Recon and contracts (stage 0 – 0.7)

- [x] Recon proposal authored: `docs/ideation/2026-10-02-planners-and-optimisers.md`.
- [x] Measured evidence recorded: `docs/research/2026-10-02-planners-and-optimisers-evidence.md`.
- [x] Money plan authored: `apps/2026-10-02-laneset/economics.json`
      (`purchase_led_single_unlock`, $9.99, `billing` required, ads and subscriptions refused with
      reasons).
- [x] Promise contract authored from recon, before the UI: `differentiators.json` — 7 promises, 3
      `core`, each with an incumbent, a limitation, an evidence strength and a source.
- [x] Naming contract authored: `naming.json`; slug `laneset`, applicationId `com.gexetr.laneset`,
      titles present in `en-US` and `ru-RU`.
- [x] Slot scaffolded from the template with the three contracts
      (`registry/apps.json:63-72`, lifecycle `scaffolded`).
- [x] This OpenSpec change consolidated (`.openspec.yaml`, `proposal.md`, `design.md`, `tasks.md`,
      `specs/swim-set-builder/spec.md`).
- [ ] Capability declaration `apps/2026-10-02-laneset/capabilities.json` enabling `billing` — required
      by `check-economics.mjs`; **not on disk** (MEASURED `Test-Path capabilities.json` → `False`).
- [ ] `store/studio.json` identity declaration (see the Identity workstream) — required before the UI
      is styled.

## 1. Domain (exists, complete)

- [x] `domain/Session.kt` — `Stroke`, `Focus`, `Gear`, `SessionConstraints`, `SetBlock`, `Interval`,
      `SectionKind`, `SessionSection`, `Session` with derived totals.
- [x] `domain/PaceModel.kt` — `thresholdPacePer100` (CSS `(t400-t200)/2`), `intervalTargetSeconds`,
      guarded by `require` so an impossible pair throws instead of producing a negative target.
- [x] `domain/SetGenerator.kt` — `generateSession(constraints, pacePer100Seconds)`; FNV-1a seed over
      every constraint, explicit `DeterministicRandom` (xorshift32), bookends fitted first, per-focus
      and per-gear programming rules, `fitBlock`/`trimToBudget`/`snapToStep`.
- [x] `domain/CoachSetParser.kt` — `parse(text, pacePer100Seconds, constraints)` over section headers,
      `8x100`/`300`, stroke aliases, `on 1:45`/`@1:10` send-offs and `rest 20`.
- [x] Unit tests for all three: `app/src/test/java/com/gexetr/laneset/domain/` (`SetGeneratorTest.kt`,
      `PaceModelTest.kt`, `CoachSetParserTest.kt`).
- [ ] Run them and read the result — `node scripts/run-slot-tests.mjs --slot laneset --rerun`
      **UNVERIFIED**; no run has happened.

## 2. Data and billing

- [x] `data/SettingsStore.kt` — interface + DataStore implementation; theme mode, dynamic colour,
      haptics; corrupt/unreadable preferences fall back to defaults; `SharingStarted.WhileSubscribed(5_000)`
      on the reading side.
- [x] `data/PaceStore.kt` — interface + DataStore implementation; both trials **and** the derived pace,
      `hasTrials` requiring all three, `setTimeTrials` delegating the arithmetic to `PaceModel`.
- [x] `data/SessionStore.kt` — interface + DataStore implementation; `SessionPosition` (indices, not a
      wall clock), `PersistedSession`, `SessionSerializer` v1 (framework-free, never throws, refuses an
      unknown version), `clampedTo` on restore.
- [ ] Wire the in-progress session's writer into the Session screen (`SessionStore.save(session,
      position)` on every position change) — the store exists, no caller does.
- [ ] Unit tests for the session codec: round-trip, refused version, malformed payload → `null`,
      position clamped to a shorter session. **Not-yet-built.**
- [ ] Billing entitlement store implementing the `billing` capability over DataStore; acknowledge
      pending purchases on cold start; surface `purchased` / `pending` / `unavailable` as a flow.
      **Not-yet-built.**
- [ ] Paywall state machine + product details (localized price from Play); no hard-coded price.
      **Not-yet-built.**
- [ ] Restore the non-consumable purchase after reinstall/clear-data; verify no subscription product is
      ever queried. **Not-yet-built.**

## 3. Identity and theme

- [ ] `store/studio.json`: `mark` (the recon's lane-line-and-turning-cross), full `palette`
      (deep teal-blue fields, one high-chroma accent reserved for target times), per-shot
      `headlines`/`kickers`, `featureTagline`, `featurePill`. **Not-yet-built** (MEASURED
      `Test-Path store/studio.json` → `False`).
- [ ] Reconcile `ui/theme/Color.kt` (green/teal/amber roles, `:5-24`) with the declared palette and
      with `store/listing.json:92-93`'s `brandColor` `#369b35` / `accentColor` `#977b47`, so
      `palette.*` and `visual-qa.py` see one source of truth. **Not-yet-built.**
- [ ] Slot-owned components: a session/stat panel, an interval row, a section strip, an empty state, a
      lock/paywall row — not stock `Card` arrangements. **Not-yet-built.**
- [ ] Launcher icon + adaptive foreground + monochrome layer derived from `mark`; replace or reconcile
      the current launcher vectors, which are not yet checked against a declared palette.
      **Not-yet-built.**
- [ ] `store/provenance.json` once assets are composed (phase 2). **[BLOCKED]** on approval.

## 4. Icons

- [x] Material Symbols set vendored as `VectorDrawable`s with `icons/provenance.json` (commit pinned,
      sha256 per icon, upstream `sourcePath`) and the Apache-2.0 text committed at
      `icons/LICENSES/Apache-2.0.txt`.
- [ ] Reference the vendored `share`, `print`, `timer` and `pool` icons from the new screens (they are
      vendored but unused today).
- [ ] Confirm each new glyph is tinted from the theme and carries no colour of its own
      (`icons.tint`).

## 5. Build and platform

- [x] Billing libraries declared and version-aligned
      (`app/build.gradle.kts:140-146`, `gradle/libs.versions.toml:21,54,55`).
- [x] `com.android.vending.BILLING` is the only `uses-permission`
      (`app/src/main/AndroidManifest.xml:9`).
- [x] Cleartext forbidden by `res/xml/network_security_config.xml`; backup rules split cloud from
      device transfer (`data_extraction_rules.xml`, `backup_rules.xml`).
- [x] `android:localeConfig="@xml/locales_config"` (`en`, `ru`) and
      `android:enableOnBackInvokedCallback="true"` declared
      (`app/src/main/AndroidManifest.xml:16,20`).
- [ ] The one authoritative single-invocation build:
      `node scripts/verify.mjs --slot laneset --build`. **Not run yet** — no `app/build/` output
      exists.
- [ ] Read the **merged** release manifest after that build and record the observed permission list
      (Play Billing's `datatransport` may contribute `INTERNET`/`ACCESS_NETWORK_STATE`, as it did for
      the reference teacher-gradebook). `store/checklist.md`. **UNVERIFIED.**
- [ ] Generate a baseline profile once the new screens exist (`:baselineprofile` module).
- [ ] `python scripts/check-16k.py` over the built artifacts (phase 2). **[BLOCKED]** on approval.

## 6. Screens and strings

- [x] Navigation 3 host already owns its back stack (`NavDisplay` + `rememberNavBackStack` +
      `rememberListDetailSceneStrategy`); edge-to-edge enabled; root `Surface` scrim.
- [x] Settings surface exists with Appearance / Language / Behaviour groups; the language row opens the
      platform per-app picker (no stored language field); `hapticsEnabled` is read in
      `MainActivity` → `HomeScreen`.
- [ ] Home screen: replace the scaffold starter with the one real action (build the free Endurance
      session) and the resume-session card when a session is stored. **Not-yet-built.**
- [ ] Builder screen: pool length, minutes, focus (Endurance selectable, others locked), avoided
      strokes, gear on deck → `SessionConstraints` → push Session. **Not-yet-built.**
- [ ] Session screen: sections and intervals with a target time on every line, the runner that advances
      `SessionPosition` and writes it through `SessionStore`, and the handoff actions.
      **Not-yet-built.**
- [ ] Coach-import screen: paste text → `CoachSetParser.parse` → the same Session surface, with the
      locked state for the free tier. **Not-yet-built.**
- [ ] Guided pace-test flow: the 400 and 200 trials with instructions, entry, `PaceModel` derivation,
      and the result written through `PaceStore.setTimeTrials`. **Not-yet-built.**
- [ ] Unlock screen/sheet: one non-consumable product, localized price from Play, no subscription
      wording, plus the restore path. **Not-yet-built.**
- [ ] Every new string added to **both** `values/` and `values-ru/` in the same change
      (`slot.locales`); replace the false scaffold strings `about_privacy_note`
      (`res/values/strings.xml:18`) and `about_body` (`:17`) in both locales.
- [ ] Adaptive layout kept: the new list/detail pairs read the host's size class and do not regress
      `uiux.adaptive`.
- [ ] Every interactive element named and ≥ 48 dp; the keyboard placed (the minutes field).

## 7. Tests

- [x] Domain JVM tests authored (see §1).
- [ ] Session-codec tests (see §2).
- [ ] Generator determinism + constraint-honouring test extended to assert the **negative control**
      (same constraints → same session; changed pool/minutes/gear/avoided-stroke → different main set).
- [ ] Coach-parser tests extended over send-offs, comments and the skip-unreadable rule.
- [ ] Share-payload test: the text handed to `ACTION_SEND` carries every section and every interval
      target.
- [ ] Print-path test: the `PdfDocument` card is generated for a fixed session.
- [ ] Run everything and read it: `node scripts/run-slot-tests.mjs --slot laneset --rerun`
      **UNVERIFIED** — not run.
- [ ] `node scripts/verify.mjs --slot laneset` (source gates, no build) and then the `--build` run.
- [ ] `node scripts/check-client-secrets.mjs --slot laneset` and
      `node scripts/check-in-app-policy.mjs --slot laneset` (the latter only once a policy URL exists).
- [ ] `node scripts/check-economics.mjs --slot laneset` and
      `node scripts/check-differentiators.mjs --slot laneset` (the latter only after every promise
      leaves `deferred` — never with `--allow-deferred`).

## 8. On-device verification (phase 1, agent-owned)

- [ ] Install the debug APK on the attached device and exercise the free Endurance session end to end
      (`.cursor/rules/device-testing.mdc`).
- [ ] Walk the two conversion moments and the guided trials; screenshot each.
- [ ] Import a real coach's workout and check the parsed targets.
- [ ] Share and print from the running app; confirm the set list leaves the app.
- [ ] Write `apps/2026-10-02-laneset/device-verification.json` with the serial, timestamp, artifact
      path and sha256, and at least three "what was tapped / what appeared" observations, so
      `slot.device` can cross-check the hash against the built APK.
- [ ] Confirm on-device that no network call happens in the daily path despite the merged manifest.

## 9. Phase 2 — release (BLOCKED until the owner's recorded approval)

Every item below refuses to run without `apps/2026-10-02-laneset/release-approval.json`
(`.cursor/rules/release-gate.mdc`). Nothing here may be started early.

- [BLOCKED] Record the owner's stage-5 approval in his own words:
      `node scripts/approve-release.mjs --slot laneset --note "<the owner's own words>"`.
- [BLOCKED] `node scripts/prepare-release.mjs --slot laneset` (signed APK + AAB + manifest).
- [BLOCKED] `python scripts/store/render.py --slot laneset` (listing, icon, feature graphic, the full
      screenshot inventory from `store/studio.json`).
- [BLOCKED] `node scripts/media-capture.mjs --slot laneset` with a device attached.
- [BLOCKED] `python scripts/store/validate.py --slot laneset` and `visual-qa.py --slot laneset`.
- [BLOCKED] Fill `store/privacy.md`, `store/data-safety.md`, `store/review-notes.md`,
      `store/release-notes.md` and `store/graphics.md` to match the shipped app.
- [BLOCKED] `scripts/check-16k.py` over the signed artifacts.
- [BLOCKED] Create the Play app, upload the graphics, acknowledge the two first-publication
      declarations, and publish — **human checkpoints** (`AGENTS.md` working rules).
- [BLOCKED] Update `registry/apps.json` lifecycle to `released` and record the first dated outcome
      reading for `node scripts/check-outcomes.mjs --slot laneset` (kill window 90 days).
