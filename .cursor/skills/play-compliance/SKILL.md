---
name: play-compliance
description: Enforce current Google Play / Android 15–16 compliance for PlayKiln slots (targetSdk 36, backup rules, 16KB pages, edge-to-edge, Data safety, AI policy). Use during /ship for the source-level checks and /release before marking a slot ready.
---

# Play compliance (PlayKiln)

Read first: `docs/research/2026-09-20-android-play-ai-agents.md` and
`openspec/specs/play-compliance/spec.md`.

Also activate Google skill when present:
`.agents/skills/android-official/play-play-policy-insights/`.

## Mandatory checklist per slot

1. **SDK** — `compileSdk`/`targetSdk` ≥ 36
2. **Backup** — `android:dataExtractionRules` + XML; exclude secrets; keep
   `fullBackupContent` if supporting API ≤ 30
3. **Edge-to-edge** — `enableEdgeToEdge()`; Material3 Scaffold insets; no API-36 opt-out
4. **Predictive back** — migrate or document temporary opt-out
5. **16 KB** — pure Kotlin OK; native deps → AGP 8.5.1+ / NDK r28+ verification
6. **Permissions** — minimize; Photo Picker before broad media; declare FGS types
7. **Store pack** — privacy, Data safety, ads, review notes, listing matches app
8. **GenAI** — if generative: reporting UI + not a thin wrapper
9. **Artifact** — `bundleRelease` AAB for Play, produced by `scripts/prepare-release.mjs` in the
   `/release` phase; the approved revision is required before any signed artifact exists
10. **Gate** — `node scripts/verify.mjs --slot <slug> --build` is the whole gate: the source
    checks plus `assembleDebug` in one process. A run without `--build` reports `UNVERIFIED`
    (exit 2), because source-only evidence cannot prove the app builds.

## Do not invent tooling

Use official docs (Context7 MCP), Google android skills, Gradle, Play Console.
Do not add fictional Android MCPs.
