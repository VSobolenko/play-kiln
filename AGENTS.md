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
- A ledger `BLOCKED(...)` label, including a wall of historical labels, is not evidence
  that Codex is unavailable. Before assigning a Cursor `generator`, run a fresh
  read-only, trivial `tier-smoke`-shaped Codex probe in this session at the tier the
  task will use; record its observed exit in the handoff.
- Apply fallback per failed worker task, not to the channel globally: `RATE_LIMITED`
  does not retry and goes to `generator`; `CONTEXT_LIMIT` reduces scope and resumes
  once, then goes to `generator`; `AUTH_ERROR` records the blocker and goes to
  `generator`. A blocked worker does not disable Codex while the fresh probe answers.
- Historical exit records predate the current classifier and may preserve a classifier
  bug rather than the worker's fate. Check the deliverable on disk before trusting a
  label; see “2026-09-23 - the failure classifier reported finished work as blocked,
  because it matched prose” in `harness/lessons.md`.
- Never accept Cursor or Codex self-reporting as proof. Run
  `scripts/verify-slot.mjs` and read the diff before a PASS.
- Adjudicate disagreements against OpenSpec, this file, and observable evidence.

Worker ledger verdicts from `node scripts/codex-run.mjs list`:

| Verdict | Written when |
|---|---|
| `DONE` | an exit record exists with exit code 0 and no diagnosed signal |
| `FAILED(<code>)` | an exit record exists with a non-zero exit code and no signal |
| `BLOCKED(<signal>)` | the exit record diagnosed a signal; a signal outranks a zero exit code, because a worker that stopped on a limit has not delivered |
| `LOST` | the recorded status is `running`, no exit record exists, and the recorded pid is not alive (including no usable recorded pid) |

A `LOST` worker died without reporting, so its work is **unverified**: inspect its
files on disk and never assume the task landed. Liveness is a heuristic — a recycled
pid can leave a dead worker reported `running` — so `running` alone is not proof of
life.

Operator details: [`harness/codex-workers.md`](harness/codex-workers.md).

### The worker loop is the default, not an optimisation

Serial solo work is the exception and needs a reason. The default mode for any task bigger than a
one-line edit is to split it into bounded roles and run them as workers, because a single agent
working one step at a time is the slowest possible configuration of this factory and it is also the
least verified one: the same head that just wrote the code is the one judging it.

The standing loop, for any slot or harness change:

| Role | Owns | Must not |
|---|---|---|
| **Implementer** | the change, its tests, the diff | verify its own work |
| **Bug hunter** | adversarial search for defects in the implementer's artifacts | fix anything; report only |
| **Verifier** | independent reproduction of the claim from the artifact on disk | accept any self-report |
| **Fixer** | the defects the two above actually reproduced | close a finding it cannot reproduce |

Then run the loop again on what the fixer produced. One worker is the **standing critic**: it runs
continuously against whatever landed most recently, hunting defects, and it stays a separate context
from whoever produced that work. Keep the roles' file lists disjoint, or give them separate
worktrees, so the loop cannot corrupt itself mid-flight.

Rules that earn that loop its cost:

- **Dispatch first, then work.** If a task has more than one independent part, start the workers
  before touching the first part by hand. Waiting to "do the quick part myself" is how the loop
  never starts.
- **A worker must produce an artifact, not a plan.** A Codex worker handed a design brief will
  often answer with a proposed design and a request for approval, then exit — it looks like a
  worker failure but it is a prompt failure. Say explicitly in the prompt that approval is not
  being sought, that the worker is already authorised, and that it must run its commands and report
  observed output. Check the deliverable on disk before recording the verdict.
- **A verifier must be able to fail the thing it verifies.** State a falsifiable consequence: which
  observation would flip the verdict, and the exact command that produces it. A verdict that no
  evidence could contradict is not a verdict. See "2026-09-25 - the checksum that gated the capture
  existed only in the lesson that described it" in `harness/lessons.md`.
- **Bug hunting and verification are different jobs.** The hunter looks for defects nobody has
  claimed; the verifier tests claims that someone has. Both are needed; collapsing them into one
  pass loses the first.
- **Report conflicts, do not average them.** When the hunter and the verifier disagree, the
  orchestrator adjudicates against the artifact, never by splitting the difference.

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
| 7-inch set | Exactly 4 at 1200×1920; Play permits 4–8, short edge ≥ 1080; Play's Large screens rule allows long edges to 7,680 but its general Requirements cap any dimension at 3,840, so the factory enforces the conservative 3,840 (`scripts/store-kit.py`) — <https://support.google.com/googleplay/android-developer/answer/9866151?hl=en> |
| 10-inch set | Exactly 4 at 1600×2560; Play permits 4–8, short edge ≥ 1080; Play's Large screens rule allows long edges to 7,680 but its general Requirements cap any dimension at 3,840, so the factory enforces the conservative 3,840 (`scripts/store-kit.py`) — <https://support.google.com/googleplay/android-developer/answer/9866151?hl=en> |

The tablet sets are 5:8 (`1200×1920`, `1600×2560`), not the 16:9/9:16 ratio Play's Large
screens section recommends. Accepted deviation recorded 2026-09-24: the hard Requirements
caps (320–3840 px, max dimension ≤ 2× min, short edge ≥ 1080) are met, and the ratio
affects only large-screen surface selection, not review eligibility — <https://support.google.com/googleplay/android-developer/answer/9866151?hl=en>

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
human/agent handoff,
[`docs/research/2026-09-22-android-play-requirements.md`](docs/research/2026-09-22-android-play-requirements.md)
for the requirements brief, and
[`docs/research/2026-09-23-google-play-automation-surface.md`](docs/research/2026-09-23-google-play-automation-surface.md)
for what the Play API can and cannot automate (no official Play MCP exists; the API
cannot create an app; Data safety can be written from a reviewed CSV).

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
scripts/scan-market.mjs           screen a query set for reachable pockets
scripts/enrich-market.mjs         per-app facts behind the top search cards
scripts/scan-ai-niches.mjs        fixed AI-utility query set over the same screen
scripts/scan-plant-vertical.mjs   screen the plant vertical and enrich its top cards
scripts/scan-complaints.mjs       read the weakest incumbents' critical reviews and classify them
scripts/scan-titles.mjs           print visible titles so a human can classify them by job
scripts/lib/play-scan.mjs         Play search screen: verdicts and a relevance guard
scripts/lib/play-app-detail.mjs   per-app installs, ads, IAP, and price
scripts/lib/play-reviews.mjs      review-text sensor and the complaint taxonomy
scripts/test-play-reviews.mjs     offline regression tests for the review sensor
scripts/verify-reviews-sensor.mjs live validation of the review sensor against Play
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
node scripts/verify-slot.mjs --slot <slug> --strict-store --build   # release evidence
node scripts/verify-slot.mjs --base main
node scripts/verify-slot.mjs --no-selftest        # skip the self-test (rare)
node scripts/kiln-status.mjs
node scripts/test-capture-publish.mjs             # capture-publish failure injection (offline, fast)
node scripts/test-apk-provenance.mjs              # APK provenance failure injection (offline, fast)
node scripts/test-play-reviews.mjs                # review-sensor unit tests (offline, fast)
node scripts/verify-reviews-sensor.mjs            # review sensor vs live Play pages
```

`scripts/test-capture-publish.mjs` injects the interruptions that publish's fail-closed claim
depends on — a swap killed between its two renames, a throwing rename, a leftover backup beside a
surviving `raw/`, and a stale file from a previous run. The happy path proves none of those, so a
change to `scripts/lib/capture-publish.mjs` needs this suite, and the suite is expected to fail
against the previous implementation.

`media-capture.mjs` publishes nothing until the installed package is proven to be the release
artifact. It walks HEAD → `dist/release-manifest.json` → the recorded sha256 → the release APK file
on disk → the installed `base.apk`, and a missing or mismatched link is fatal. It then writes
`store/provenance.json`, which is committed beside the assets because `captures/` and `dist/` are
not, and is the only durable record tying the committed images to a build.

`scripts/test-apk-provenance.mjs` covers that chain offline. Its first case is the false pass that
motivated it: a stale manifest and the matching stale installed APK agree with each other about a
build that is neither of them, which a hash-string-only comparison cannot see. Both suites are
expected to fail against the implementation they replaced.

`--build` compiles the slot. Without it the gate reports `UNVERIFIED` (exit 2), not
PASS, because source-only evidence cannot prove the app builds; never cite a
build-less run as release evidence.

`scripts/test-play-reviews.mjs` is the offline regression suite for the review-text sensor
(`scripts/lib/play-reviews.mjs`); `scripts/verify-reviews-sensor.mjs` checks the same sensor
against live Play pages, including the star-histogram trap where a page carries more
`aria-label="Rated…"` anchors than reviews. Run the offline suite after any taxonomy change —
it fails loudly on a class whose pattern no longer matches real phrasing.

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
