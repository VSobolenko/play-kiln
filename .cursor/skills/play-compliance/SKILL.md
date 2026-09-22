---
name: play-compliance
description: Enforce current Google Play / Android 15–16 compliance for PlayKiln slots (targetSdk 36, backup rules, 16KB pages, edge-to-edge, Data safety, AI policy). Use before marking a slot ready or during /ship Verify.
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
9. **Artifact** — `bundleRelease` AAB for Play
10. **Gate** — `node scripts/verify-slot.mjs --slot <slug>` PASS

## Do not invent tooling

Use official docs (Context7 MCP), Google android skills, Gradle, Play Console.
Do not add fictional Android MCPs.
