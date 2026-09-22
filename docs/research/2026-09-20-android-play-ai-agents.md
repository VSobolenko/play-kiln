# Research brief — Android + Play + AI agents (2026-09-20)

Sources: Google Play Console Help, developer.android.com (target SDK, Auto Backup,
16 KB pages, edge-to-edge, Android 15/16 behavior changes), github.com/android/skills,
Play AI-Generated Content / Data safety / User Data policies. Last checked ~2026-09.

## A. Google Play hard requirements (phone/tablet factory apps)

| Requirement | Rule (as of research date) | PlayKiln action |
|-------------|----------------------------|-----------------|
| **Target API** | From **2026-08-31**, new apps & updates must **target API 36** (Android 16). Existing apps need ≥ API 35 to stay discoverable on newer OS. Extension to 2026-11-01 may be available. | Scaffold `compileSdk`/`targetSdk` = **36** |
| **16 KB page size** | Apps targeting API 35+ must support 16 KB pages on 64-bit. Pure Kotlin/Java already OK. Native `.so` needs AGP ≥ 8.5.1 + NDK r28+; hard fail for updates from **2027-02-01**. | Prefer pure Kotlin; if NDK/SDK adds `.so`, enforce AGP 8.5.1+ and document check |
| **AAB** | Play accepts App Bundles for distribution (not raw APK for new listings). | `bundleRelease` before ready |
| **Data safety** | Required even if **no data collected**. Must match privacy policy + all SDKs. | `store/data-safety.md` + audit skill |
| **Privacy policy** | Public URL + in-app link/text. | `store/privacy.md` + listing field |
| **Backup / data transfer** | Auto Backup on by default. For API 31+: declare `android:dataExtractionRules`; keep `fullBackupContent` for older devices. Exclude secrets/tokens. Lint: `DataExtractionRules`. | Manifest + XML rules in scaffold |
| **Edge-to-edge** | Enforced when targeting API 35+ on Android 15+. API 36: **cannot opt out** on Android 16. | `enableEdgeToEdge` + Material3 Scaffold insets |
| **Predictive back** | API 36+: system predictive back on by default; migrate or temporary `enableOnBackInvokedCallback=false`. | Prefer Navigation predictive back; enable callback true |
| **Package registration** | Play Console package-name registration / developer verification (policy updates 2026). | Checklist item before submit |
| **Demo access** | Provide demo login / review instructions when gated. | `store/review-notes.md` |
| **Restricted permissions** | SMS/Call Log, All files, exact alarms, FGS types, photo/video → Photo Picker first. | Minimize permissions; policy skill audit |
| **GenAI apps** | If app *generates* AI content: in-app report/flag, no prohibited content, not a thin API wrapper. | Extra OpenSpec tasks when idea is AI |

Emails about “support backup / optimize memory” map mainly to: **Auto Backup + dataExtractionRules**, **16 KB pages**, **target API bumps**, and **background/memory behavior** on new OS versions — not a single obscure checkbox.

## B. How the industry builds Android with AI agents (do not invent)

1. **Official Google Android skills** (`github.com/android/skills`) — Agent Skills standard; install via Android CLI `android skills add`. Covers edge-to-edge, Navigation 3, Play policy insights, R8, intent security, AGP 9, etc. **Installed into** `.agents/skills/android-official/`.
2. **AGENTS.md as the Codex channel** — external agents don't see Cursor rules; durable policy must live in AGENTS.md (also OpenAI/Cursor harness practice).
3. **Harness engineering loop** — plan → execute → observe → independent verify → repair → feed failures back into skills (OpenAI / Cursor long-running agent patterns). PlayKiln already mirrors this with `/ship` + dual peers.
4. **Grounding** — use Context7 / official docs MCP for live API truth; do **not** invent Android MCPs. Real MCP set: context7, playwright (store assets), codex, optional firebase. Android CLI when installed locally.
5. **Failure modes others hit**
   - Stale `targetSdk` → cannot submit / lose discoverability
   - Missing Data safety / mismatched privacy → rejection
   - Thin AI wrappers → low-value / policy risk
   - Broken edge-to-edge / insets → “unusable” review UX
   - Native libs failing 16 KB → Play Console warning → later hard block
   - Over-permissioned manifests without declaration forms
   - Agents inventing APIs / wrong Gradle plugins — mitigate with official skills + verify gates

## C. Factory compliance gate (mechanical)

`scripts/verify-slot.mjs` must fail a slot that lacks:

- `targetSdk` / `compileSdk` ≥ 36 (configurable floor)
- `android:dataExtractionRules` in manifest
- backup rules XML present
- `applicationId` under `com.gexetr.`

Optional `--build` still runs `assembleDebug`.

## D. References (bookmark)

- https://support.google.com/googleplay/android-developer/answer/11926878
- https://developer.android.com/google/play/requirements/target-sdk
- https://developer.android.com/guide/practices/page-sizes
- https://developer.android.com/identity/data/autobackup
- https://developer.android.com/about/versions/15/behavior-changes-15
- https://developer.android.com/about/versions/16/behavior-changes-16
- https://developer.android.com/tools/agents/android-skills
- https://github.com/android/skills
- https://support.google.com/googleplay/android-developer/answer/10787469 (Data safety)
- https://support.google.com/googleplay/android-developer/answer/14094294 (AI-generated content)
