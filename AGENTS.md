# AGENTS.md — PlayKiln

> L1 contract for Cursor and Codex. Read this file before changing the factory or
> a slot. Cursor-only rules may add detail but may not contradict it.

## Mission

PlayKiln is a factory that turns one app idea into one isolated, publishable
Android/Google Play release; it is not a single-product repository. The human-facing
factory brief is [`MISSION.md`](./MISSION.md) and must be honored in every session.

## Factory pipeline

Two phases, separated by the owner's approval. Phase 1 produces an app the owner can install
and review, and deliberately produces **no** signed artifacts, no store pack and no screenshots,
because all of those are wasted work while the app can still change. Phase 2 produces them and
refuses to run without the approval. The rule is `.cursor/rules/release-gate.mdc`.

**Phase 1 — development, `/ship <idea>`**

| Order | Stage | Owner | Gate or output |
|---:|---|---|---|
| 0 | `<idea>` from measured demand | Cursor, per `.cursor/rules/ideation-gate.mdc` | A proposal in `docs/ideation/` with measured demand evidence; never a topic carried over from an example |
| 0.5 | Economic plan | Cursor + the **money owner** role (`.cursor/agents/money-owner.md`) | `economics.json` passing `scripts/check-economics.mjs`, plus **Human checkpoint 1:** approve the plan and its price |
| 0.6 | Promise contract (recon's reason list) | Cursor | `differentiators.json` passing `scripts/check-differentiators.mjs`, authored from recon **before** the UI, so the reasons the app was chosen cannot be lost during the build; every promise starts `deferred` |
| 1 | `/ship <idea>` | Cursor | Automatic after approval |
| 2 | `scripts/new-slot.mjs --economics <path> --differentiators <path>` | Factory | Automatic isolated `apps/<date>-<slug>/` slot; **refuses to scaffold without a validated plan and a validated promise contract** |
| 3 | Implement | Codex workers, orchestrated by Cursor | Automatic code, tests, and app behavior; each promise moves from `deferred` to `shipped` with a proof, or to `narrowed`/`dropped` in writing |
| 4 | Development verification | Factory + Cursor | `scripts/run-slot-tests.mjs --rerun` plus `scripts/verify-slot.mjs --build`, which compiles `assembleDebug` and does not sign; **fails while any promise is still deferred**; a readable diff |
| 5 | **Human checkpoint 1.5: approve the release** | Human | The owner installs the debug APK, reviews the UI and behavior, and states approval in his own words. Recorded with `scripts/approve-release.mjs --slot <slug> --note "<owner words>"`, bound to a digest of the app's build inputs |

**Phase 2 — release, `/release <slug>`** — every step below refuses to run without the approval
from stage 5, and the approval lapses by itself if the app's sources change.

| Order | Stage | Owner | Gate or output |
|---:|---|---|---|
| 6 | `scripts/prepare-release.mjs` | Factory | signed APK, signed AAB, and release manifest |
| 7 | `scripts/store-kit.py` | Factory | complete store pack |
| 8 | `scripts/media-capture.mjs` | Factory | real-device screenshots, when a device is available |
| 9 | `scripts/verify-slot.mjs --strict-store --build` and `/eval` | Factory + Cursor | Objective gates plus an independent verdict; repair until green |
| 10 | Play Console submission, walked from the dashboard to `Изменения на проверке` | Cursor, driven under the owner's observation; **Human checkpoint 2** covers two steps only | The ladder is `docs/play-publishing-playbook.md` §5.1 and was measured live 2026-09-29. Every declaration answer, the release form, the save, the submit and the pre-review check are performed by the agent. Exactly two steps stay human: **uploading the five store-graphic fields** (the sandboxed browser cannot set a `file` input — an unsolved tooling limitation, deliberately recorded as such and not as a policy) and **the two first-publication legal declarations**, which are the owner's own attestation, once per app. Submitting at all still requires the stage-5 approval record, and the ladder is a recorded runbook, **not yet code** — no script performs those clicks today |
| 11 | Earn | Human + the **money owner** role | `node scripts/check-outcomes.mjs --slot <slug>` verdicts the plan's own `killCriteria` against a recorded reading from `apps/<slot>/store/outcomes.json`; `NO-DATA` and `TOO-EARLY` are honest states, `MISSED` names the kill command, and `node scripts/kiln-status.mjs --kill <slug> --reason "<why>"` records the decision. A slot that misses its own thresholds is archived, and the archive is a decision with the numbers in it rather than an abandoned directory |

One idea must take one command through this pipeline. Never silently upload, commit
an edit, or act in the console without the owner's direction and his ability to watch. An
approval is recorded only from the owner's own
words, never inferred from an agent's confidence, and never recorded to unblock a step. The
distinction the first real submission measured on 2026-09-29: an agent **may** walk the console ladder
when the owner has directed it and is observing, and it may not do any of it silently, on its own
initiative, or in place of the approval stage.

**The factory's product is revenue, not green gates.** Both phases measure form: that the
APK builds, that the store pack is complete and truthfully described, that the assets exist and
match a recorded build. None of them can tell you whether anyone will pay, and none of them can tell
you whether the app is still the app recon chose. Stages 0, 0.5, 0.6 and 11 are the money stages, and
only stage 11 produces the answer. A slot that passes every gate and earns
nothing is a failed slot, and the failure is in stage 0, not in verification.

Stage 0 exists because the factory's first real slot inverted it: a topic was inherited from an
example, the market was chosen on that inheritance, and the shipped app had
`capabilities.json` of `{"schemaVersion": 1, "enabled": []}` — no billing, no ads, no revenue
mechanism — while every gate returned PASS. That post-mortem is
`docs/reviews/2026-09-26-slot-1-economics-postmortem.md`. The economics contract that now blocks
it is `docs/slot-economics.md`.

**Recon's reason list is a contract too.** Recon answers two questions, not one: whether an app
can earn, and *why anyone would install it*. The second answer is a list of promises, and it is the
first thing a build loses — the easy promises ship, the hard ones get a `// TODO`, and the end of
the work is declared by whoever is tired. Stage 0.6 writes that list into `differentiators.json`
before the UI exists; every promise starts `deferred`, and stage 4 fails while any promise is still
deferred, so a slot cannot be called done with ten of twenty reasons delivered. Losing or weakening
a `core` promise requires the owner's own words, exactly as the release approval does. The contract
is `docs/slot-differentiators.md` and the gate is `scripts/check-differentiators.mjs`.

**The earn stage is a sensor now, not a paragraph.** Stage 11 used to be the one part of the pipeline
with nothing behind it: `killCriteria` were validated for shape and then read by no program, so the one
stage the mission calls the product could not answer whether a slot had earned anything. Two things
changed. `apps/<slot>/store/outcomes.json` carries dated readings from Play Console, and
`scripts/check-outcomes.mjs` judges them against the thresholds the slot declared **before** it was
built — `ON-TRACK`, `TOO-EARLY`, `NO-DATA`, or `MISSED`. `scripts/kiln-status.mjs` prints that verdict
for every slot and `--kill <slug> --reason "<why>"` records the decision, refusing a kill with no
reason and refusing one for a slot that meets its own criteria unless `--force`. The role that owns the
numbers, the price, the paywall and the reading is **the money owner** (`.cursor/agents/money-owner.md`,
contract in `docs/monetization-owner.md`) — a sibling to the scout, which finds demand, and to the
visual-identity work, which owns the look. Its rule is that an unmeasured claim is `UNVERIFIED`:
invented revenue is the defect it exists to prevent, and the factory has no revenue API, so a
hand-recorded reading with an `observedAt` is the current mechanism and is labelled as such.

The wall-clock target is **about one hour of agent time per slot**, and buildability is therefore a
first-class selection criterion rather than a detail discovered during the build. That target is a
direction, not a measurement: the first slot's scaffold-to-last-build span is on record as
**4 h 49 m as a lower bound** (`docs/reviews/2026-09-26-slot-1-economics-postmortem.md` §1), and
every recipe in `docs/ideation/2026-09-26-five-recipes.md` estimates 3.5–6 hours. Never cite the
target as if it had been met; report the elapsed wall clock instead. The concrete cuts that would
make one hour reachable are in `.cursor/rules/parallel-execution.mdc`.

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
store/release-notes.md
store/console-answers.md
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
| Feature graphic | Exactly 1024×500, JPEG or 24-bit PNG without alpha, ≤ 15 MB |
| Phone set | Exactly 6 at 1080×1920; the console's phone field requires 2–8, 16:9 or 9:16, each side 320–3840, ≤ 8 MB each |
| 7-inch set | Exactly 4 at 1080×1920; the console's 7-inch field requires 2–8, 16:9 or 9:16, each side 320–3840, ≤ 8 MB each — measured live in the RU console 2026-09-29 |
| 10-inch set | Exactly 4 at 1440×2560; the console's 10-inch field requires 2–8, 16:9 or 9:16, each side 1080–7680, ≤ 8 MB each — measured live in the RU console 2026-09-29 |
| Release name | An internal Play Console label, never shown to a user; it exists so the console's release list can tell one build from another. Play prefills it from the bundle's `versionName`, so `store/release-notes.md` states it as `<versionName> (<versionCode>)` and `store-kit.py` checks it against `dist/release-manifest.json` — a name that disagrees with the upload makes the list lie about which build is which |
| Release notes | `store/release-notes.md`, carrying a fenced block in **Play's own multi-language format** — one `<locale>` tag per language, tags on their own lines, the whole block pasted into the console's single text box. Play **does** show these on the store page and caps each language at **500 characters**, truncating silently rather than refusing, so every note in the block is measured at pack validation (`MAX_RELEASE_NOTE_CHARS` in `scripts/store-kit.py`) — including one for a language the listing does not declare, because text in the block is text a human may paste. The block is the note rather than a rendering of it, so the measured string and the pasted string are the same characters |
| Console answers | `store/console-answers.md`, the Ctrl-C/Ctrl-V sheet for a first upload: for every console field, the exact string to paste, in a fenced block, derived from the slot's own pack so it cannot drift from it. The owner performed the first upload of `teacher-gradebook` by hand, answering page after page in chat — the privacy-policy URL, ten App-content declarations, the credentials dialog — and nothing carried those answers, so each was re-derived by a human. The sheet is **written, not preserved**: it is a copy of what the pack already says, so a hand edit could only make it disagree. It carries a machine-readable claims block, and `validate_console_answers` fails a sheet whose answer contradicts the pack, names a product the slot's Kotlin does not declare, or carries a reserved documentation host. Generated by `write_documents` after `release-notes.md` (it embeds that file's paste block and release name), from the shared model `scripts/lib/console-form.json` |

Every screenshot set is 9:16 — phone and 7-inch at `1080×1920`, 10-inch at `1440×2560` — because that is
the aspect the console states in **every** screenshot field ("Соотношение сторон изображения должно
составлять 16:9 или 9:16"), not merely what its Large screens section recommends. That sentence and the
per-field size windows above were measured live in the RU console on 2026-09-29, from each field's own
inline strings rather than from the documentation; the two tablet windows genuinely differ
(320–3840 against 1080–7680) and the targets sit inside both. The factory previously shipped 5:8 tablet
canvases (`1200×1920`, `1600×2560`) on the reading that the ratio only affected large-screen surface
selection. The console did accept a 5:8 upload, but the factory's decision is to satisfy the stated
prose, because it cannot be refused either — and `screenshot_rule_failures` (`scripts/store-kit.py`)
now asserts the ratio for every kind, so a `max_ratio: None` kind is no longer size-only.

The **Desktop** and **Android XR** screenshot fields are deliberately **not produced**. They carry no
asterisk in the console — they are optional — and the owner's instruction on 2026-09-29 is that the pack
does not fill them. No target, no asset and no check exists for them, and adding one is a product
decision rather than a completeness fix.

Default locale is `en-US`; supported additional locale is `ru-RU`. If login gates
content, `review-notes.md` must provide usable demo access. Claims and screenshots
must describe the shipped app; synthetic captures must be identified as synthetic.

**Never enable Play Console's automatic string translation for a language the slot already
translates.** A first upload shows three rows of one list — the bundle *signed by Google Play*,
*automatic protection*, and *automatic strings translation* — because Play App Signing unlocks all
three as bundle enhancements; the third is a feature being offered, not a finding about the app, and
declining it blocks nothing. Enabling it for a translated language **overrides that language's
existing translations** ("Any existing translations for these languages will be overridden", <https://support.google.com/googleplay/android-developer/answer/9844778?hl=en>), replacing
git-tracked, diffable, reviewable `values-<locale>/` resources with machine output that lives only in
Console — where the next upload replaces it and switching the feature off discards it. The
repository is the source of truth for a translation. Using the feature as a bootstrap for a language
the slot does not translate at all is coherent only if the output is reviewed and frozen into
`values-<locale>/` afterwards. No Play requirement links the app's resource languages to the
languages declared on the listing, so a single-language listing over a multi-language app is
legitimate and needs no "fix".

`listing.json.contactEmail` must be the developer account's monitored address,
`v.sobolenko.vs@gmail.com` — defined once as `DEFAULT_CONTACT_EMAIL` in
`scripts/store-kit.py` and applied as the default for every new slot, so no pack
ships the generator's `support@example.com`. It is a public support address, not a
secret, and does not belong in `.env`. `privacyPolicyUrl` has no default: it must be
a hosted HTTPS URL the owner controls. A pack still pointing at a reserved
documentation host is unfinished, and the generated human-checks list says so.

## Play compliance floor

Before a slot is `ready`:

- Data safety is complete even when no data is collected, matches every SDK, and
  matches a public privacy policy.
- The privacy policy exists in **both** places Play requires it: the listing's public
  HTTPS URL, and **inside the app**. A slot that declares a `privacyPolicyUrl` must let
  the user open the policy from the app; privacy prose in an About screen is not the
  policy. Only the store URL was ever checked here, and `teacher-gradebook` shipped
  without the in-app half.
- Apps with accounts provide deletion in-app and through an external web resource.
- Permissions are minimized; Photo Picker is preferred; foreground-service types
  are declared; exact alarms are treated as restricted.
- Backup rules separate cloud backup from device transfer and exclude secrets.
- Edge-to-edge and predictive back behavior work at the target SDK.
- Ads, content rating, target audience, review access, and AI-content declarations
  are truthful and complete.
- The two first-publication declarations — **Developer Program Policies** and **US export
  laws** — are acknowledged by the owner when the app is created, once per app rather than
  per release. The export declaration is a certification about encryption the app uses, and
  every slot uses encryption whether or not it imports crypto code; the factory records the
  annual BIS self-classification question as an open obligation rather than settled law.
- "Collect" in Data safety means **transmitting data off the device**, so an app whose whole
  record stays in local storage declares none of it — but it must not under-declare a bundled
  SDK that does transmit (Play Billing's `datatransport` put a Financial-info row in
  `teacher-gradebook`).
- An in-app disclosure-and-consent gate is required only when handling may fall outside the
  user's reasonable expectation — background or unexpected collection — and **not** for a
  foreground-only app handling only what the user just typed into it. Do not add a
  launch-blocking consent screen to a slot that does not trigger this.
- Native libraries have confirmed 16 KB readiness, measured from the built artifact by
 `scripts/check-16k.py` (ELF `p_align` per `PT_LOAD`, plus the stored-and-aligned container
 offset in the APK). `prepare-release.mjs` cannot tell you this — it only knows that `.so` files
 exist, so it records `confirmation-required` until this script is run and read.
- **No shipped slot calls a paid API from the client.** A key compiled into `app/src/main/`, or a
 provider SDK whose only useful form needs a server-side secret, is the signature defect of this era:
 41% of LLM-enabled apps leaked API credentials in one 206,867-app scan, and secrets were found in 35%
 of 50,000 Play apps. `scripts/check-client-secrets.mjs` (rule
 `.cursor/rules/client-side-secrets.mdc`) scans the shipped client for key-shaped literals, paid-provider
 SDKs, direct calls to paid hosts and a keyed `google-services.json`. A slot that wants a paid capability
 must be offline by construction, or carry nothing it cannot serve without a secret. A slot-owned
 `apps/<slot>/client-secrets-exempt.json` may exempt a finding, but each entry needs a reason and a
 decider, and every applied exemption is printed.

Use [`docs/play-publishing-playbook.md`](docs/play-publishing-playbook.md) for the
human/agent handoff,
[`docs/research/2026-09-22-android-play-requirements.md`](docs/research/2026-09-22-android-play-requirements.md)
for the requirements brief, and
[`docs/research/2026-09-23-google-play-automation-surface.md`](docs/research/2026-09-23-google-play-automation-surface.md)
for what the Play API can and cannot automate (no official Play MCP exists; the API
cannot create an app; Data safety can be written from a reviewed CSV).

## Visual identity

Every slot ships its own pronounced visual identity; the factory's edge is design, not only
correctness. A working app that looks like an unstyled Material demo is a failed recipe even when
every gate is green. The binding rule is `.cursor/rules/visual-identity.mdc` and the human-facing
summary is [`DESIGN.md`](./DESIGN.md).

- The identity is declared in `store/studio.json` (`mark`, `palette`, per-shot `headlines` and
  `kickers`, `featureTagline`, `featurePill`) **before** the UI is styled, and the launcher icon,
  adaptive-icon vectors, feature graphic and store compositions are rendered from it.
- The declared palette is the slot's own `ui/theme/Color.kt` palette, so the pack cannot drift from
  the app, and the slot owns its theme and its component vocabulary rather than shipping library
  defaults.
- The identity is per slot: two slots never share a mark, a palette or a component kit, for the same
  reason they never share a job (`docs/monetization-strategy.md` §1.2).
- Evidence: `verify-slot --strict-store` requires `store/provenance.json` for a composed pack,
  `scripts/visual-qa.py` measures the pixels, and `media-capture.mjs` photographs the real signed
  build with asserted readings. `apps/2026-09-26-teacher-gradebook` is the reference slot.

## Self-learning loop

The factory changes Google Play policy, the Android toolchain and its own harness continuously, so
noticing a change is part of the pipeline rather than an occasional clean-up. Three ledgers carry it,
each with a program that reads it (contract: [`docs/self-learning.md`](docs/self-learning.md)):

| Ledger | Says | Read by |
|---|---|---|
| `docs/research/measured-facts.json` | every external rule the factory encodes, when it was measured, and **which program enforces it** | `scripts/check-freshness.mjs` — a fact past its window is `UNVERIFIED`, and a fact naming an enforcing script or probe that does not exist is a failure |
| `harness/lessons.md` | every defect that already cost this factory time, with the sensor that stops it recurring | `scripts/check-lessons.mjs` — a lesson whose named sensor has been deleted or renamed fails |
| `scripts/test-*.mjs`, `test-*.py` | the regression suites those sensors live in | `scripts/selftest.mjs`, which **runs every suite it finds on disk** and fails when one is neither executed nor given a reason |

Two rules keep that loop honest, and both were paid for. **A fact with no enforcing program is
reported as `UNENFORCED` (exit 2), never as fresh** — the factory follows some of its rules on prose
alone, and the ledger names which, instead of implying a check exists. **A check that exists and is
never run cannot be told apart from a check that does not exist**: six suites were written, cited in
this file as release evidence, and never executed by the one command a session actually runs, which is
why the self-test now discovers and runs all of them. Adding a suite to `scripts/` needs no edit in the
self-test — the safe direction, because a hand-maintained runner list fails by omission.

What the loop still cannot do, stated plainly in `docs/self-learning.md`: it does not browse Google
Play by itself, so a policy change is noticed when an agent or the owner reads the page and re-dates the
fact; and the two known unenforced positions — Play Console's automatic string translation overriding a
git-tracked translation, and the AGP/Kotlin/Compose BOM version pins — are recorded there as gaps
rather than closed.

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
| An unstyled Material 3 app — default seed, stock components as the whole look | A slot-owned identity declared in `store/studio.json` and carried into the slot's theme and components |
| The same mark, palette or component kit in two slots | A new identity per slot, like a new job |
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
scripts/new-slot.mjs              create a slot (requires a validated --economics plan and promise contract)
scripts/lib/namespaces.mjs        single source of truth for `com.gexetr.*`
scripts/lib/slot-lock.mjs         the per-slot build lease: a second Gradle consumer reports the holder
scripts/lib/gradle.mjs            the only Gradle route; fails closed when no supported JDK resolves
scripts/lib/client-secrets.mjs    the detector behind the no-client-side-paid-API rule
scripts/check-client-secrets.mjs  gate: a key-shaped literal or paid-provider SDK in the shipped client
scripts/lib/outcomes.mjs          the post-ship reading contract: schema, verdicts, kill instruction
scripts/check-outcomes.mjs        earn gate: dated readings vs the plan's own kill criteria
scripts/lib/freshness.mjs         measured-fact ledger: expiry plus the program enforcing each fact
scripts/lib/lessons.mjs           lesson-to-sensor contract: a named check must still exist on disk
scripts/lib/pack-approval.mjs     the authored pack inputs the approval binds, separate from the build
scripts/check-in-app-policy.mjs   gate: the privacy policy is reachable from inside the app
docs/self-learning.md             the improvement loop: inputs, cadence, and its honest limits
docs/monetization-owner.md        the money owner's contract: KPIs, cadence, verdicts
scripts/lib/slot-economics.mjs    the slot economics contract: parsing, validation, capability pairing
scripts/check-economics.mjs       money gate: does the plan name a real revenue mechanism
scripts/lib/slot-differentiators.mjs the promise contract: parsing, proof typing, delivery resolution
scripts/check-differentiators.mjs promise gate: is every reason this app was chosen delivered or dropped
scripts/check-locales.mjs         checks declared app languages against resource coverage
scripts/test-locale-dedup.mjs     regression test: a non-translatable key is declared once
scripts/check-adaptive-icon.py    checks launcher-mark fit inside adaptive-icon masks
scripts/check-16k.py              measures 16 KB page-size readiness from the built APK/AAB itself
docs/slot-economics.md            the economics.json contract and its archetype/capability pairing
docs/slot-differentiators.md      the differentiators.json contract: promises, proofs, owner decisions
docs/ideation/                    idea proposals awaiting the owner's decision
scripts/selftest.mjs              regression tests for the factory scripts
scripts/codex-run.mjs             durable Codex worker runner
scripts/lib/release-approval.mjs  source digest + owner approval: the two-phase gate
scripts/approve-release.mjs       record, check, or revoke the owner's release approval
scripts/store-kit.py              generate and validate store packs
scripts/lib/console-form.json     the console form: the ten declarations, page paths, field caps, release and billing rules
scripts/console-walkthrough.mjs   render and drift-check docs/play-console-walkthrough.md from that model
scripts/test-console-answers.py   regression suite for the generated store/console-answers.md sheet
scripts/visual-qa.py              report pixel measurements for store-pack visual identity
scripts/test-visual-qa-discovery.py regression test for visual-QA theme discovery
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
# phase 1 — development
node scripts/check-economics.mjs --slot <slug>           # money stage: plan vs enabled capabilities
node scripts/check-outcomes.mjs --slot <slug>            # earn stage: dated readings vs the plan's own kill criteria
node scripts/check-client-secrets.mjs --slot <slug>      # no client-side paid-API credential in the shipped app
node scripts/check-differentiators.mjs --slot <slug>     # promise gate: is every reason delivered or dropped
node scripts/check-differentiators.mjs --slot <slug> --allow-deferred  # builder progress read, never completion
node scripts/check-locales.mjs --slot <slug>             # declared app languages have resources
node scripts/test-locale-dedup.mjs                       # a non-translatable key is declared once
node scripts/check-freshness.mjs                         # a measured fact is in date and names a program that enforces it
node scripts/check-lessons.mjs                           # every lesson's named sensor still exists on disk
python scripts/check-adaptive-icon.py --slot <slug>      # launcher mark fits adaptive-icon masks
python scripts/visual-qa.py --slot <slug>                # visual-identity report; read it, exit 0 is not approval
python scripts/test-visual-qa-discovery.py               # visual-QA theme-discovery regression test
python scripts/test-console-answers.py                   # generated console sheet: generation, drift, and the model
node scripts/console-walkthrough.mjs --check             # the walkthrough still matches the shared console model
node scripts/selftest.mjs                         # factory scripts vs a sandbox slot, plus every scripts/test-* suite
node scripts/verify-slot.mjs                      # runs the self-test too
node scripts/verify-slot.mjs --slot <slug>
node scripts/verify-slot.mjs --slot <slug> --build   # phase-1 evidence: source gates + assembleDebug
node scripts/kiln-status.mjs                      # factory health + each slot's phase

# the approval boundary
node scripts/approve-release.mjs --check --slot <slug>    # is this revision approved for release?
node scripts/approve-release.mjs --slot <slug> --note "<the owner's own words>"
node scripts/approve-release.mjs --revoke --slot <slug>   # always safe, always allowed

# phase 2 — release (each of these refuses without the approval above)
node scripts/prepare-release.mjs --slot <slug>
python scripts/store-kit.py --slot <slug>
node scripts/media-capture.mjs --slot <slug>
python scripts/check-16k.py apps/<slot>/dist/*.apk apps/<slot>/dist/*.aab  # 16 KB page-size readiness
node scripts/verify-slot.mjs --slot <slug> --strict-store --build   # release evidence
node scripts/verify-slot.mjs --base main
node scripts/verify-slot.mjs --no-selftest        # skip the self-test (rare)
# factory health and the portfolio's money
node scripts/kiln-status.mjs                      # every slot's phase, every slot's earnings verdict
node scripts/kiln-status.mjs --kill <slug> --reason "<why>"  # record a kill; refuses without a reason
node scripts/test-capture-publish.mjs             # capture-publish failure injection (offline, fast)
node scripts/test-apk-provenance.mjs              # APK provenance failure injection (offline, fast)
node scripts/test-play-reviews.mjs                # review-sensor unit tests (offline, fast)
node scripts/test-slot-lock.mjs                   # per-slot lease: holder named, no deadlock
node scripts/test-client-secrets.mjs              # client-secret detector, with negative controls
node scripts/test-outcomes.mjs                    # post-ship verdicts and the kill boundary
node scripts/test-pack-approval.mjs               # approval covers the authored pack, not just the build
python scripts/test-store-studio-completeness.py  # composed media: needs local store/captures (gitignored)
node scripts/verify-reviews-sensor.mjs            # review sensor vs live Play pages
```

`scripts/lib/release-approval.mjs` is the single source of truth for the phase boundary. An
approval record (`apps/<slot>/release-approval.json`, gitignored) stores the owner's own words, a
timestamp, and **two** SHA-256 digests: one of the slot's build inputs, and one — `pack-approval.mjs` —
of the authored pack inputs the owner actually reviewed (`store/listing.json`, `store/journey.json`,
`store/studio.json`, `store/review-notes.md`). Editing any of those invalidates the approval on the
next check, so "approve, then keep coding" cannot silently ship an unreviewed revision, and neither can
"approve the app, then rewrite the listing to claim something the app does not do" — that second half
was outside the digest entirely, and the demonstration was a one-file edit that both `store-kit.py` and
`media-capture.mjs` accepted. Pipeline outputs (`store/assets/**`, `provenance.json`, `console-answers.md`)
are deliberately **not** in the pack digest: a digest over the pipeline's own output would void its own
approval every time the pipeline ran. `prepare-release.mjs`, `store-kit.py`'s rendering paths,
`media-capture.mjs` and now `play-upload.mjs` all refuse without it — the uploader was the one script
that talked to Google Play and never asked whether the revision was approved. `--revoke` is always safe
and never needs permission; recording one never does. When a release script refuses, the fix is to ask
the owner, never to edit or delete the record.

Two honest limits, stated rather than implied. First, neither digest covers `scripts/**`,
`.cursor/rules/**` or this file, so an agent could in principle weaken the check that judges it after
approval; widening the digest to `scripts/**` would void every slot's approval on every harness
improvement, which is the opposite of what this factory is, so the boundary is made **auditable**
(one command reports exactly which bytes the owner approved) rather than tamper-proof. The real defence
is that recorded approvals are read back and the harness changes are reviewed as diffs. Second,
`approvedBy: 'owner'` is a constant in the record: nothing can distinguish the owner's words from an
agent's. Only recording an approval from the owner's own words, never inferred, keeps that true.
`store-kit.py --init` and `--validate-only` stay usable during development.

`scripts/test-capture-publish.mjs` injects the interruptions that publish's fail-closed claim
depends on — a swap killed between its two renames, a throwing rename, a leftover backup beside a
surviving `raw/`, and a stale file from a previous run. The happy path proves none of those, so a
change to `scripts/lib/capture-publish.mjs` needs this suite, and the suite is expected to fail
against the previous implementation.

`media-capture.mjs` publishes nothing until the installed package is proven to be the release
artifact. It walks HEAD → `dist/release-manifest.json` → the recorded sha256 → the release APK file
on disk → the installed `base.apk`, and a missing or mismatched link is fatal.

When the manifest's commit is behind HEAD the chain would fail on its own, because committing a capture
necessarily advances HEAD past the build it records and exact equality would make the record unable to
outlive itself. That case is allowed only when every file changed between the two commits is on an
allowlist of paths that cannot alter the built APK (`NON_BUILD_PATHS` in
`scripts/lib/apk-provenance.mjs`). Anything unlisted forces a rebuild, so a new build input is
conservative by default: the previous version compared only the slot's `app/`, which missed the slot's
own Gradle config, the version catalog, the wrapper, and any shared `capabilities/` module the slot
includes.

It then writes `store/provenance.json`, which is committed beside the assets because `captures/` and
`dist/` are not, and is the only durable record tying the committed images to a build. `store-kit.py`
checks that record on every validation: each committed asset must hash to what it says, so an image
edited or replaced without a fresh capture fails instead of keeping the previous claim. The record is
required for any slot that composes its media, since those are the packs whose inventory claims captured
screenshots, and an absent record is a failure rather than a note.

The render inside a capture run passes `--skip-provenance`, because the record on disk then describes
the run being replaced. That flag does not yield a pass: it reports `UNVERIFIED` and exits 2, so the
deferred state can never be read as `STORE-KIT: OK`, and the capture ends with a real `--validate-only`
pass against the record it just wrote.

`scripts/test-apk-provenance.mjs` covers that chain offline. Its first case is the false pass that
motivated it: a stale manifest and the matching stale installed APK agree with each other about a
build that is neither of them, which a hash-string-only comparison cannot see. Both suites are
expected to fail against the implementation they replaced.

`--build` compiles the slot. Without it the gate reports `UNVERIFIED` (exit 2), not
PASS, because source-only evidence cannot prove the app builds; never cite a
build-less run as release evidence.

`run-slot-tests.mjs` reports `STALE` (exit 2) when Gradle did not execute the test task in that
invocation — the XML under `build/test-results/` is then a *previous* run's output. A Kotlin change
forces re-execution and hides this; a change confined to `res/` does not, because a test that reads
`strings.xml` off disk is an input Gradle cannot see. `STALE` is never a PASS, and `--rerun` is what
an evidence run must use.

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
- Development and release are separate phases: `/ship` stops at a debug build and a review, and
  signing, store packs and screenshots happen in `/release` only after the owner's recorded
  approval. Never record an approval, edit the record, or work around a refusing release script.
- Slots never depend on each other; the shared harness remains at repository root.
- No secret may be committed or printed into a worker prompt or log.
- Improve the root harness when the same failure recurs across slots.
- Archive or kill failed attempts; do not turn PlayKiln into one product repo.
- Human approval is mandatory for the legal declarations and for releasing at all: the stage-5
 approval record is the owner's own words and is never inferred. What is **not** human any more is
 pressing the console's controls — `docs/play-publishing-playbook.md` §5.1 records the submission
 ladder as walked by the agent on 2026-09-29, and only the graphics upload and the two
 first-publication attestations stay with the owner.
- A slot is never scaffolded without a validated `economics.json`; the money stage is not optional.
- A slot is never scaffolded without a validated `differentiators.json` either, and it is authored
  from recon **before** the UI. Every promise starts `deferred`, and `verify-slot.mjs` fails while
  any promise is deferred: shipping ten of twenty reasons is not shipping the app that was chosen.
  Losing or weakening a `core` promise needs the owner's own words — see
  `.cursor/rules/differentiator-contract.mdc`.
- An example the owner gives is a shape, never a topic. Topics are chosen from measured demand.
- Never generate or rotate a signing key without the owner's explicit permission in the current
  conversation; a signing key is the app's permanent identity and its password is the owner's to
  choose and store. See `.cursor/rules/authorization.mdc`.
- Target roughly one hour of agent wall-clock per slot, and report the actual time taken.
