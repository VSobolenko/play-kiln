# Tasks: app-pipe-sizing

## Recorded decisions

- [x] The existing slot is `apps/2026-09-25-pipe-sizing`; do not create or alter any other slot.
- [x] v1 is a United States fuel-gas sizing workflow and calculation aid; IPC water and NFPA 13 sprinkler work are deferred.
- [x] The commercial direction is a one-time Play Billing unlock; subscriptions and ads are absent in v1.
- [x] The app is offline, account-free, server-free, camera-free, and has no ML or AI wrapper.
- [x] **2026-09-25 — v1 ships the sizing workflow, not code data.** It supplies no ICC/NFPA/AGA capacity data; users read capacities from their own code book or free online code and record their citation. **Reason:** ICC permits free viewing but not reproduction without separate written permission, and IFGS material has an additional AGA copyright holder.
- [x] **2026-09-25 — v1 makes no code-compliance claim.** It is a calculation aid that directs users to verify against the applicable adopted code. **Reason:** the owner has no qualified fuel-gas engineer/code professional to review the maths.

## 1. User-supplied data and arithmetic core

- [ ] **[Build] Model only user-supplied or user-confirmed capacity-like values.** Require a visible source affordance and capture code/edition, table, row, or “user-supplied.” Do not embed, bundle, download, reproduce, or fake a code capacity, clause, table row, or table-derived constant. **Verification:** unit and UI tests reject a result without citation.
- [ ] **[Build] Implement arithmetic and bookkeeping only.** Sum loads/MBH and developed lengths, add user-supplied fitting allowances, convert units, and compare computed required load with user-confirmed capacity. Treat equivalent lengths, specific gravity, pressure-drop allowances, and inside diameters as user inputs when required; compute only conversions and pure arithmetic identities. **Verification:** independently calculated fixtures assert arithmetic and method selection.
- [ ] **[Build] Implement explicit missing, range, and consistency states.** Missing, out-of-range, or inconsistent required input or citation produces no result and names the correction. Never estimate, default, interpolate, fabricate, or carry a capacity-like value from another row. **Verification:** unit and UI tests cover each state, including long/high-demand cases and a previous-row value that must not be reused.
- [ ] **[Build] Verify arithmetic without a code-capacity oracle.** No external oracle for code capacities exists because v1 supplies none. Independently compute expected developed-length sums, fitting-allowance addition, unit-conversion round trips, MBH/load summation, method selection, and missing-input handling; never generate expected values with the production implementation or test embedded code capacities. **Verification:** fixtures document their independent arithmetic calculation and tests fail if production arithmetic changes.

## 2. User workflow and safety states

- [ ] **[Build] Implement editable sizing inputs.** Let users edit material, pressure class, method, loads, lengths, capacity inputs, citations, and every method-required input. **Verification:** UI tests edit each field and prove no conflicting value is silently retained.
- [ ] **[Build] Implement output, provenance, and units.** Display arithmetic result, editable input summary, and attached user citation on every result view; display imperial ft/in and MBH and provide metric without changing physical value, method, or provenance. **Verification:** conversion and semantics/UI tests assert no result surface without provenance.
- [ ] **[Build] Display the required disclaimer.** Show “This is a calculation aid, not a code-compliance determination. You are responsible for verifying this work against the applicable adopted code.” on first run and every result view. **Verification:** UI tests assert exact text in both locations; store-copy review rejects compliance or safety-critical claims.

## 3. Offline, privacy, billing, and compliance

- [ ] **[Build] Keep the app offline and account-free.** Omit server, analytics, cloud sync, camera, ML, AI, and unnecessary permissions; configure backup rules consistently with local-data posture. **Verification:** manifest, dependency report, network inspection, and backup-rule read are recorded in `store/checklist.md`.
- [ ] **[Build] Implement one-time Play Billing.** Use one non-consumable, acknowledged, restorable entitlement; show real price/effect; preserve work on failure; include no subscription or ad surface. **Verification:** billing tests cover purchase, restore, pending, cancellation, and failure.
- [ ] **[Verify] Exercise accessibility, edge-to-edge, and predictive back.** Test editable fields and unavailable-result states at large font scale and with screen reader semantics. **Proof:** dated device/emulator observations in `store/checklist.md`.

## 4. Store, release, and factory gates

- [ ] **[Store] Generate and complete the truthful store pack.** Run `python scripts/store-kit.py` for `pipe-sizing`; supply copy and media showing only calculation-aid behaviour, user-recorded provenance, and disclaimer. **Verification:** `node scripts/verify-slot.mjs --slot pipe-sizing --strict-store` passes; this build-less check is not release evidence.
- [ ] **[Human] Approve Play declarations.** Review Data safety, privacy policy, billing, ads, content rating, audience, reviewer access, and no-compliance-claim posture. **Proof:** dated approvals in `store/checklist.md`; do not submit forms automatically.
- [ ] **[Verify] Prepare signed release artifacts.** Run `node scripts/prepare-release.mjs --slot pipe-sizing`. **Verification:** signed APK, signed AAB, and `dist/release-manifest.json` exist and signature checks pass.
- [ ] **[Verify] Run the release evidence gate.** Run `node scripts/verify-slot.mjs --slot pipe-sizing --strict-store --build`. **Verification:** exit 0 and PASS; read complete output.
- [ ] **[Verify] Read the complete change and slot diff, then run `/eval`.** Repair until the independent evaluator accepts the implementation. **Proof:** recorded evaluator verdict and reviewer diff findings.
- [ ] **[Human] Perform first upload and publish.** The human compares signing fingerprints, completes declarations, uploads the first AAB, and publishes only after the above evidence agrees.
