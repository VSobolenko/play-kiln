## 0. Spec discipline (do not invent)

Normative: `docs/research/2026-10-21-coverkit-competitor-matrix.md` + `specs/social-cover-studio/spec.md`.

- [ ] 0.1 Before any UI file: confirm primary screens are only Destinations / Templates / Editor / Projects / Brand / Settings
- [ ] 0.2 Primary chrome labels use matrix nouns only (`Templates` `Photo` `Text` `Stickers` `Crop` `Export` `Share` `Save` `Brand` `Sizes` `Projects`)
- [ ] 0.3 REFUSE audit: no subscription SKU, no mid-edit interstitial, no AI tool, no feed planner / bio / scheduler / video timeline as primary
- [ ] 0.4 Complaint checklist from spec is part of device verification (blur, lost projects, paywall-to-save, unlock lag, non-Picker, unscalable text, irreplaceable template photo)

## 1. Scaffold and contracts

- [ ] 1.1 OpenSpec `app-coverkit` validates (`openspec validate app-coverkit`)
- [ ] 1.2 Scaffold slot with `node scripts/new-slot.mjs` using `docs/ideation/plans/coverkit.{economics,differentiators,naming}.json` and `--spec app-coverkit`; verify `apps/<date>-coverkit/` exists with matching `applicationId`
- [ ] 1.3 Move ideation proposal + plans into `apps/<slot>/docs/` and clear root queue leftovers; verify `ideas.orphanPlans` clean for `coverkit`
- [ ] 1.4 Enable `billing` in `capabilities.json`; add Fragment floor constraint; verify `node scripts/check-economics.mjs --slot coverkit`
- [ ] 1.5 Declare `store/studio.json` identity + `Color.kt` alignment; verify `palette` / studio presence before styling screens

## 2. Happy path (Destination → Templates/Photo → Export)

- [ ] 2.1 Destination home: YouTube / Instagram / TikTok / WhatsApp catalog cards; launch shows destinations, not blank canvas / marketing hub
- [ ] 2.2 Size catalog module with pixels from spec; unit-test exact dimensions for each destination id
- [ ] 2.3 Template grid + blank+Photo entry; free path reaches Editor; template photo slot is replaceable
- [ ] 2.4 System Photo Picker (not app-private gallery); no broad storage permission
- [ ] 2.5 Text layer add/edit + free pinch-scale; verify text visible and scales
- [ ] 2.6 Native-resolution PNG export + Share/Save; unit-test bitmap size equals catalog; no screenshot-only export
- [ ] 2.7 Free watermark when unlocked=false; draft Save into Projects works while locked

## 3. Editor depth (ABSORB maker tools only)

- [ ] 3.1 Primary tool row: Photo | Text | Stickers | Crop; Layers + Safe zone secondary — nothing else on the bar
- [ ] 3.2 Layer list: reorder, delete, select; verify draw order in export
- [ ] 3.3 Text fill + stroke (InstaSize-class border); stroke visible in PNG fixture
- [ ] 3.4 Stickers (YT callouts + story frames); sticker in export
- [ ] 3.5 Drag / pinch / rotate; transform persists in project JSON
- [ ] 3.6 Safe-zone overlay optional; does not bake into export unless user-placed layer

## 4. Durability and brand kit

- [ ] 4.1 Project serialize/deserialize; kill mid-edit → reopen same layers
- [ ] 4.2 Duplicate project (Social Media Post Maker pattern); original intact
- [ ] 4.3 Corrupt project fails soft; recoverable state
- [ ] 4.4 Brand kit (logo, ≤4 colors, 2 fonts) local only; defaults when set; Export works with empty Brand

## 5. Multi-size pack and unlock

- [ ] 5.1 Sizes pack UI after first success; single-size Export still one tap from Editor
- [ ] 5.2 Pack render non-stretch fit; two sizes → correct dimensions
- [ ] 5.3 Play Billing non-consumable only; no subscription SKU
- [ ] 5.4 Unlock: watermark off, pack + full templates + Brand gates; same-session apply + Restore purchase
- [ ] 5.5 Mark differentiator promises shipped with proofs; `check-differentiators.mjs --slot coverkit` (no deferred)

## 6. Settings, locales, policy, polish

- [ ] 6.1 Settings: language, theme, one behaviour preference; `settings.*`
- [ ] 6.2 en + ru resources; matrix nouns translated; no hardcoded prose; `locale.*`
- [ ] 6.3 Privacy + `publish-privacy.mjs` + in-app policy; `pack.placeholders` / `slot.inAppPolicy`
- [ ] 6.4 About + Rate; `review.*`
- [ ] 6.5 Navigation 3, insets, content colour; `uiux.*`

## 7. Verification and device

- [ ] 7.1 Unit tests: catalog, export dimensions, project round-trip, unlock gating; `run-slot-tests.mjs --slot coverkit --rerun`
- [ ] 7.2 `node scripts/verify.mjs --slot coverkit --build` green
- [ ] 7.3 A40 `R58MB3SGZVL`: happy path + pack + unlock restore + complaint checklist; write `device-verification.json`
- [ ] 7.4 Checkpoint 1.5 handoff with observed evidence — do not ask owner to test
