# AGENTS.md — PlayKiln

> L1 contract for Cursor and Codex. Read this file before changing the factory or
> a slot. Cursor-only rules may add detail but may not contradict it.

## Mission

PlayKiln is a factory that turns one app idea into one isolated, publishable
Android/Google Play release; it is not a single-product repository. The human-facing
factory brief is [`MISSION.md`](./MISSION.md) and must be honored in every session.

## Factory pipeline

| Order | Stage | Owner | Gate or output |
|---:|---|---|---|
| 0 | `<idea>` and plan | Human + Cursor | **Human checkpoint 1:** approve the plan |
| 1 | `/ship <idea>` | Cursor | Automatic after approval |
| 2 | `scripts/new-slot.mjs` | Factory | Automatic isolated `apps/<date>-<slug>/` slot |
| 3 | Implement | Codex workers, orchestrated by Cursor | Automatic code, tests, and app behavior |
| 4 | `scripts/store-kit.py` | Factory | Automatic complete store pack |
| 5 | `scripts/prepare-release.mjs` | Factory | Automatic signed APK, signed AAB, and release manifest |
| 6 | `scripts/verify-slot.mjs` | Factory + Cursor | Automatic objective gates plus a read diff |
| 7 | `/eval` | Cursor evaluator | Automatic independent verdict; repair until green |
| 8 | Play Console legal steps, first upload, publish | Human | **Human checkpoint 2:** complete declarations, upload once, press publish |

One idea must take one command through this pipeline. Never silently upload, commit
an edit, or publish on the human's behalf.

## Published namespace

Every slot's `applicationId` is `com.gexetr.<slug>`, because `com.gexetr` is the
developer account's existing Play namespace and already-published apps use it. An
applicationId is permanent after the first upload, so it is never changed for a
shipped slot. The rule is defined once in `scripts/lib/namespaces.mjs` and imported
by `new-slot.mjs`, `verify-slot.mjs`, and `kiln-status.mjs`; do not restate it.

## Required toolchain

| Component | Factory contract |
|---|---|
| Gradle | **9.7.1** |
| Android Gradle Plugin | **9.4.1** with built-in Kotlin |
| Kotlin / Compose Compiler plugin | **2.3.21**; versions must match |
| Compose BOM | **2026.09.00** (`compose.ui` 1.12.0, Material 3 1.4.0) |
| SDK | `compileSdk = 37`, `targetSdk = 36`, `minSdk = 26` |
| JDK | **17 minimum** |
| SDK Build Tools | **36.0.0** |
| R8 | Full mode defaults; release minification and resource shrinking; refined keep rules; `proguard-android-optimize.txt` |
| Baseline profiles | `androidx.baselineprofile` 1.5.0, Profile Installer 1.4.1, Macrobenchmark 1.4.1; generation uses a device/emulator and `com.android.test` module |
| 16 KB pages | Pure Kotlin/Java is ready; native code requires AGP 8.5.1+ and NDK r28+; never use legacy JNI packaging |

## Agent model

Cursor is the orchestrator and verifier. Codex is a co-lead engineer with its own
worker channel and writes production code; it is not merely a reviewer.

- Run long Codex tasks with `node scripts/codex-run.mjs`; the MCP `codex` tool is
  only for short dialogue because long MCP requests time out.
- Give each worker a bounded role and file list. Parallel code writers must have
  disjoint files or separate worktrees.
- If a worker reports `RATE_LIMITED`, hand the task to the Cursor `generator`
  subagent. Follow `harness/dual-agent.md` for other failure signals.
- Never accept Cursor or Codex self-reporting as proof. Run
  `scripts/verify-slot.mjs` and read the diff before a PASS.
- Adjudicate disagreements against OpenSpec, this file, and observable evidence.

Operator details: [`harness/codex-workers.md`](harness/codex-workers.md).

## Store-pack contract

Every publishable slot contains this complete, truthful pack:

```text
store/listing.json
store/listing.md
store/privacy.md
store/data-safety.md
store/review-notes.md
store/graphics.md
store/checklist.md
store/assets/icon-512.png
store/assets/feature-graphic-1024x500.png
store/assets/<locale>/icon-512.png
store/assets/<locale>/feature-graphic-1024x500.png
store/assets/<locale>/phone-01.png ... phone-06.png
store/assets/<locale>/tablet-7-01.png ... tablet-7-04.png
store/assets/<locale>/tablet-10-01.png ... tablet-10-04.png
```

| Item | Contract |
|---|---|
| Listing text | Title ≤ 30, short description ≤ 80, full description ≤ 4000 characters |
| App icon | 512×512, 32-bit PNG with alpha, sRGB, ≤ 1024 KB; no drawn rounded mask or shadow |
| Feature graphic | Exactly 1024×500, JPEG or 24-bit PNG without alpha |
| Phone set | Exactly 6 at 1080×1920; Play permits 2–8, no alpha, ≤ 8 MB each, sides 320–3840, long side ≤ 2× short side |
| 7-inch set | Exactly 4 at 1200×1920; Play permits 4–8, short edge ≥ 1080, long edge ≤ 7680 |
| 10-inch set | Exactly 4 at 1600×2560; Play permits 4–8, short edge ≥ 1080, long edge ≤ 7680 |

Default locale is `en-US`; supported additional locale is `ru-RU`. If login gates
content, `review-notes.md` must provide usable demo access. Claims and screenshots
must describe the shipped app; synthetic captures must be identified as synthetic.

## Play compliance floor

Before a slot is `ready`:

- Data safety is complete even when no data is collected, matches every SDK, and
  matches a public privacy policy.
- Apps with accounts provide deletion in-app and through an external web resource.
- Permissions are minimized; Photo Picker is preferred; foreground-service types
  are declared; exact alarms are treated as restricted.
- Backup rules separate cloud backup from device transfer and exclude secrets.
- Edge-to-edge and predictive back behavior work at the target SDK.
- Ads, content rating, target audience, review access, and AI-content declarations
  are truthful and complete.
- Native libraries have confirmed 16 KB readiness.

Use [`docs/play-publishing-playbook.md`](docs/play-publishing-playbook.md) for the
human/agent handoff and
[`docs/research/2026-09-22-android-play-requirements.md`](docs/research/2026-09-22-android-play-requirements.md)
for the requirements brief.

## Defaults to suppress

| Do not use | Use instead |
|---|---|
| `org.jetbrains.kotlin.android` | AGP 9 built-in Kotlin |
| `android.kotlinOptions {}` | `android.compileOptions.targetCompatibility`; built-in Kotlin derives `jvmTarget` |
| `proguard-android.txt` | `proguard-android-optimize.txt` |
| `kapt` | KSP when annotation processing is required |
| XML View layouts for new screens | Jetpack Compose + Material 3 |
| Flutter, React Native, Expo, or Next.js | Native Kotlin + Compose |
| Invented MCP servers or APIs | Committed MCP configuration and verified documentation |
| `jniLibs.useLegacyPackaging = true` | 16 KB-compatible packaging |
| Keystores, passwords, service-account JSON, or keyed `google-services.json` in Git | Environment variables or ignored local paths |
| Hardcoded Material colors | `MaterialTheme.colorScheme` |
| Shared `applicationId` values | Unique `com.gexetr.<slug>` per slot |
| A thin AI chat wrapper | A differentiated workflow with required content reporting |

Do not set obsolete AGP flags such as `android.enableR8.fullMode`,
`android.builtInKotlin`, or `android.newDsl`. Use `androidComponents.onVariants`, not
the removed `applicationVariants` API.

## Layout

```text
AGENTS.md                         L1 contract for Cursor and Codex
MISSION.md                        Russian human-facing factory brief
DESIGN.md                         L1 visual guidance
.cursor/                          commands, rules, hooks, MCP, and Cursor agents
.agents/skills/                   shared and official Android skills
.codex/agents/                    Codex-compatible agent roles
.kiln/                            gitignored worker state, prompts, logs, and ledgers
openspec/                         factory specs and per-slot changes
harness/                          protocols, ambition, surface map, and lessons
apps/<date>-<slug>/               isolated Android projects
scaffold/android-compose/         new-slot template
registry/apps.json                slot registry
scripts/new-slot.mjs              create a slot
scripts/lib/namespaces.mjs        single source of truth for `com.gexetr.*`
scripts/selftest.mjs              regression tests for the factory scripts
scripts/codex-run.mjs             durable Codex worker runner
scripts/store-kit.py              generate and validate store packs
scripts/sign-release.mjs          configure and inspect local upload signing
scripts/prepare-release.mjs       build, sign, verify, and stage APK/AAB artifacts
scripts/media-capture.mjs         capture store media through ADB journeys
scripts/play-upload.mjs           update an existing Play app after its first manual upload
scripts/verify-slot.mjs           objective factory and slot gates
scripts/kiln-status.mjs           factory health summary
```

`.kiln/` is disposable local state and never a source of product secrets. Durable
factory conventions belong in this file, OpenSpec, or `harness/`.

## Verification

```bash
node scripts/selftest.mjs                         # factory scripts vs a sandbox slot
node scripts/verify-slot.mjs                      # runs the self-test too
node scripts/verify-slot.mjs --slot <slug>
node scripts/verify-slot.mjs --slot <slug> --strict-store
node scripts/verify-slot.mjs --slot <slug> --build
node scripts/verify-slot.mjs --base main
node scripts/verify-slot.mjs --no-selftest        # skip the self-test (rare)
node scripts/kiln-status.mjs
```

`UNVERIFIED` requires a manual diff/read; it is not PASS. For release preparation,
the required evidence is a verified signed release APK, signed release AAB, release
manifest, strict store-pack PASS, green slot verification, and `/eval` acceptance.

`scripts/selftest.mjs` exercises the factory's own scripts (real `new-slot.mjs`
against a temp scaffold sandbox) instead of trusting them by inspection. When a
factory defect is fixed, add a check there and confirm it fails before the fix —
see `.cursor/rules/verification-discipline.mdc`.

## Working rules

- One idea creates one slot unless the human explicitly asks to extend a slot.
- Slots never depend on each other; the shared harness remains at repository root.
- No secret may be committed or printed into a worker prompt or log.
- Improve the root harness when the same failure recurs across slots.
- Archive or kill failed attempts; do not turn PlayKiln into one product repo.
- Human approval is mandatory for legal declarations, first upload, and publish.
