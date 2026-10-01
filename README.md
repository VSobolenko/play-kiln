# PlayKiln

PlayKiln is an Android/Google Play app factory: one idea becomes one isolated,
publishable app with signed APK/AAB artifacts and a complete store pack. It is not a
single app repository.

It works in **two phases, separated by your approval**: development produces an app you can
install and judge, and only after you approve it does the factory build the signed artifacts,
compose the store pack and capture screenshots. Nothing expensive is produced twice.

## Two-minute path

```bash
node scripts/kiln-status.mjs
# Then ask the agent:
# /ship <your app idea>      phase 1 — build and test the app, stop for your review
# /release <slug>            phase 2 — sign, store pack, screenshots, after you approve
```

| Phase | Stage | Owner |
|---|---|---|
| 1 | Idea from measured demand, plus an economics plan and a promise contract | Agent, per `.cursor/rules/ideation-gate.mdc` |
| 1 | Approve the plan and its price | Human checkpoint 1 |
| 1 | `/ship` → `scripts/new-slot.mjs --economics <path> --differentiators <path>` | Factory |
| 1 | Codex workers implement | Factory |
| 1 | Unit tests plus `verify.mjs --build` (debug APK, no signing) | Factory |
| 1 | **Install the debug APK and approve the release** | Human checkpoint 1.5 |
| 2 | `/release` → `prepare-release.mjs` → `store/render.py` → `media-capture.mjs` | Factory |
| 2 | `verify.mjs --build` → `/eval` | Factory |
| 2 | Legal declarations, first upload, publish | Human checkpoint 2 |
| 2 | Earn, or hit the plan's kill criteria and get archived | Human checkpoint |

**The product of this factory is revenue, not green gates.** Stages after the plan check that
the APK builds and that the store pack is complete and truthful; none of them can tell you
whether anyone will pay. That is why a slot cannot be scaffolded without a validated
`economics.json`:

```bash
node scripts/check-economics.mjs --slot <slug>      # is there a real revenue mechanism
node scripts/check-economics.mjs --economics docs/ideation/plans/<slug>.economics.json
```

The phase-1 → phase-2 boundary is one command, and it is yours to run:

```bash
node scripts/approve-release.mjs --check  --slot <slug>   # is this revision approved?
node scripts/approve-release.mjs --slot <slug> --note "<your own words>"
node scripts/approve-release.mjs --revoke --slot <slug>   # always safe
```

The approval is bound to a digest of the app's build inputs, so editing the app after approving
it invalidates the approval automatically — `prepare-release.mjs`, `store/render.py`'s rendering
paths and `media-capture.mjs` all refuse until you approve the revision you actually reviewed.
The rule is [`.cursor/rules/release-gate.mdc`](.cursor/rules/release-gate.mdc).

The contract is [`docs/slot-economics.md`](docs/slot-economics.md), and candidate ideas
awaiting a decision live in [`docs/ideation/`](docs/ideation/).

An example the owner gives describes a **shape**, never a topic. The factory's first real slot
failed because a calculator offered as an illustration became the strategy:
[`docs/reviews/2026-09-26-slot-1-economics-postmortem.md`](docs/reviews/2026-09-26-slot-1-economics-postmortem.md).

New slots come from [`scaffold/android-compose/`](scaffold/android-compose/) and are
created under `apps/<date>-<slug>/` with a unique `com.gexetr.<slug>`
`applicationId` — the developer account's existing Play namespace.

The factory tests itself, so a broken script cannot reach a slot:

```bash
node scripts/verify.mjs              # every project-wide check, in one process
node scripts/verify.mjs --slot <slug> # the same, plus the slot's pack, icon, apk and project checks
```

Every check and every surviving regression suite, with the exact command that runs it, is listed in
[`docs/factory-checks.md`](docs/factory-checks.md). The knowledge lives there rather than in a runner:
the old `scripts/selftest.mjs` was itself a test runner running test runners, and it was retired on
2026-09-30 (`openspec/changes/reduce-factory-to-core/`).

Read the agent contract in [`AGENTS.md`](AGENTS.md), the Russian operator brief in
[`MISSION.md`](MISSION.md), and the live-release handoff in
[`docs/play-publishing-playbook.md`](docs/play-publishing-playbook.md).
