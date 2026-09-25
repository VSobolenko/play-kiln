# Pre-upload checklist — Pipe Sizing: NFPA 54

Research baseline: `docs/research/2026-09-20-android-play-ai-agents.md`

## Build / platform

- [ ] `compileSdk` / `targetSdk` ≥ **36**
- [ ] `assembleDebug` passes
- [ ] `bundleRelease` AAB produced (Play artifact)
- [ ] `dataExtractionRules` + `backup_rules` present; secrets excluded
- [ ] Edge-to-edge + insets verified on API 35/36 emulator
- [ ] Predictive back OK (or temporary opt-out documented)
- [ ] If any `.so` / NDK / native SDK: 16 KB page-size check planned (hard block 2027-02-01)

## Play Console

- [ ] Privacy policy URL ready (`store/privacy.md` drafted)
- [ ] Data safety form answers (`store/data-safety.md`) — even if "no data collected"
- [ ] Ads declaration accurate
- [ ] Content rating questionnaire
- [ ] Target audience / Families policy if applicable
- [ ] Package name registered / developer verification done
- [ ] Review notes / demo account (`store/review-notes.md`)
- [ ] Listing matches real app behavior (no vaporware)
- [ ] If generative AI: in-app report/flag + not a thin wrapper

## Signing

- [ ] Upload keystore configured locally (not in git)
- [ ] Play App Signing enrolled
