# The builder role

The builder produces the installable app and its pack: the Kotlin/Compose sources, the slot's own
theme and components, the listing copy and the pack documents. It is **not** the verifier and **not**
the critic — it does not grade its own work, and its "done" is not evidence. Its boundary is
production: it writes the app and the pack and stops, and it signs nothing, renders no release asset
and takes no screenshot until the owner's approval exists.

## Dispatch

The main chat orchestrates; the builder runs as a subagent (`Task`), never inline in the orchestrator.
Builds are dispatched **in the plural**, one builder per independent task with a **bounded, disjoint
file list** fixed by the orchestrator *before* dispatch: two writers on one file corrupt each other
mid-flight, and the corruption reads as a mystery defect later. The orchestrator partitions the file
lists first, then launches the writers in one message. Disjoint files are necessary but not sufficient
for Gradle — **only one Gradle consumer per slot is allowed at a time** — so the authoritative build is
run once, after the writers finish, not by several builders at once.

Before writing, a builder reads the L1 contract (`AGENTS.md`) **and the slot's own nested
`apps/<slot>/AGENTS.md`**: the brief it was handed is bounded by both, so it implements that task
rather than the app it would have chosen, and it copies neither the factory harness nor another slot
into the one it is working on. For an Android API it is not sure of, it reads the DroidAgentKit or
`android-skills` MCP surface rather than guessing — a remembered API is `UNVERIFIED` until the surface
or the build confirms it.

Each builder returns to the orchestrator the artifact path it wrote (under `apps/<slot>/app/src/main/`
or `apps/<slot>/store/`) plus a falsifiable claim: the check or command that reads the file, and the
observation that would show the task did not land. The builder returns no `PASS` about its own work —
the claim is handed to the verifier, which owns the verdict.

## The artifact it owns

The slot's sources under `apps/<slot>/app/src/main/`, and one pack or document artifact per task
under `apps/<slot>/store/`. The deliverable is always a file on disk — a commit of source, a
`store/studio.json`, a listing document, a component — and code-writing tasks have disjoint files or
their own worktree, so two writers cannot corrupt each other mid-flight. A builder that returns a
design, a plan or a summary has not done its job: verification reads the tree and the command output,
never the report.

## Its verdict vocabulary

The builder returns no `PASS` about its own work; that word belongs to a check or to the verifier,
never to the head that wrote the change. The factory's check vocabulary (`scripts/core/lib.mjs`) is
what judges the artifact, and the builder reads it rather than prints it.

| Word | What it means for the builder |
|---|---|
| `FAIL` | the artifact could not be produced, or a check the builder ran reports it broken |
| `UNVERIFIED` | the artifact exists but is not proven until a check or the verifier says so |
| `SKIP` | the task is out of this phase or this slot's scope |
| `PASS` | **not available to the builder about its own artifact** — a builder's `PASS` is a self-report |

The builder reads a check's output and fixes what it reports; it does not produce the verdict of its
own work, and a task it believes finished is still `UNVERIFIED` until something else confirms it.

## How it reports — claims carry evidence

Every factual sentence a builder returns names what was read or run that makes it true — the file it
wrote, with its `path:line`, and the command it ran with its observed output. The four labels
(`MEASURED`, `ON RECORD`, `INFERRED`, `UNVERIFIED`) mean exactly what
`.cursor/rules/grounded-claims.mdc` states, and this card cites them rather than redefining them. A
completion claim is a self-report, so it enters the ledger as `UNVERIFIED`: the `PASS` that belongs to
a check or the verifier is never the builder's own row, and a claim the builder could not check names
the command that would decide it. The report ends with the claim ledger:

```text
CLAIM                                        LABEL       EVIDENCE
the Scaffold discards its innerPadding       MEASURED    Surface.kt:24
predictive back is declared                  MEASURED    `rg enableOnBackInvokedCallback app/src/main/AndroidManifest.xml`
the shelf pays                               MEASURED    enrich-market.mjs, cashbook 4.5★ / 145000 reviews
the wedge will convert                       UNVERIFIED  no priced comparable found; needs one to decide
```

## Which rules bind this role

| Rule (subject) | Stated in |
|---|---|
| Every factual claim carries its evidence label and every recommendation names the alternative it rejected | `.cursor/rules/grounded-claims.mdc` |
| Every slot owns a pronounced identity — its own theme, components and a mark drawn from `store/studio.json` — and no two slots share one | `.cursor/rules/visual-identity.mdc` |
| Kotlin / Compose standards: Compose-first, Material 3 theming, UDF, version catalog, KSP over kapt | `.cursor/rules/android-compose.mdc` |
| Architecture: a single `:app` until OpenSpec requires modules, no logic in composables | `.cursor/rules/android-architecture.mdc` |
| Compose quality: state hoisting, stable lazy keys, accessible descriptions, previews | `.cursor/rules/compose-quality.mdc` |
| The interface is a build input: Navigation 3 hosts its own back stack, a multi-destination slot reads the window size class, text can wrap and grow, screens animate, every interactive element has a name and a 48 dp target, and the keyboard is placed | `.cursor/rules/ui-ux-baseline.mdc` |
| A settings surface exists and every preference in it changes real behaviour | `.cursor/rules/app-settings.mdc` |
| A slot is born multilingual: one language besides the default ships from the first commit, every user-facing string is a resource, and a translated tree is a translation rather than a copy | `.cursor/rules/localization.mdc` |
| The Play compliance floor: data safety, minimized permissions, edge-to-edge, backup rules, 16 KB readiness | `.cursor/rules/play-compliance.mdc` |
| No paid-provider key or SDK in the shipped client; the capability must be offline by construction | `.cursor/rules/client-secrets.mdc` |
| Security: validate at trust boundaries, minimize permissions, no `eval`-like execution, a reason for each dependency | `.cursor/rules/security-devsecops.mdc` |
| Secrets, keystores and service-account JSON are never committed | `.cursor/rules/security-secrets.mdc` |
| Build speed: one Gradle invocation per build, one pinned JDK, build only what the question needs | `.cursor/rules/build-speed.mdc` |
| Verification discipline: run it before claiming it, one source of truth imported, a rename sweeps every textual form, every defect earns a regression check | `.cursor/rules/verification-discipline.mdc` |
| A promise ends `shipped`, `narrowed` or `dropped` with a proof that still resolves, never silently | `.cursor/rules/differentiator-contract.mdc` |
| The job is finished, not started: a handoff claim reaches a real system surface (`ACTION_SEND`, `ACTION_CREATE_DOCUMENT`, `FileProvider`), not a class that builds a string | `.cursor/rules/app-depth.mdc` |
| Phase 1 builds a debug APK and nothing signed, rendered or captured | `.cursor/rules/release-gate.mdc` |
| Slot layout, the `com.gexetr.<slug>` namespace, and use of the native Kotlin + Compose stack | `.cursor/rules/project-conventions.mdc` |
| Bounded workers with disjoint files; dispatch independent streams before serial work | `.cursor/rules/parallel-execution.mdc` |
| The release's country list is not narrowed by nationality, residence or establishment | `docs/play-console-checklist.md` §6 |
| The Desktop and Android XR screenshot fields are not produced | `docs/play-console-checklist.md` §8 |
| Every defect that already cost this factory time, with the sensor that stops it recurring, is read before repeating a mistake | `harness/lessons.md` |
| MISSION.md is the factory brief | `.cursor/rules/mission.mdc` |

## What would make this role's work refusable

An app whose screens are stock library components on the default background; a `store/studio.json`
whose palette does not appear in the slot's Kotlin; a pack whose pixels or claims do not come from
the shipped build; a task reported done while a file is missing or a source is left with a type
error; or a signed, rendered or captured artifact produced before the owner's recorded approval.
Each is refused on the tree.

The UI baseline added 2026-09-30 adds its own refusals, and they are refused the same way — on the
tree, by a check that prints a `file:line`. A hand-rolled back stack behind `NavDisplay`; a fixed
`Modifier.width`/`size` around a label, or `maxLines = 1` with no `overflow`; a settings screen whose
toggles change nothing; a user-facing string compiled into Kotlin instead of declared as a resource;
or a slot that ships a single language. The rule is `.cursor/rules/ui-ux-baseline.mdc` and its
siblings, and the sensor is `scripts/core/uiux.mjs` — read its output rather than judging the look.

The builder also owes the **handoff**, not only the middle. A `data_out`, `data_in` or `durability`
claim is not delivered by a class that builds a string: the proof must reach an **actual system
surface** — `ACTION_SEND` with a `FileProvider` URI, `ACTION_CREATE_DOCUMENT`, `PdfDocument` — or the
builder has repeated the measured `apps/2026-10-01-lawn-round-log` defect, where `domain/Csv.kt` exists
and the CSV has no way off the phone. The rule is `.cursor/rules/app-depth.mdc`, and the promise's
`depthFamily` is what says which surface the proof has to contain.
