# The builder role

The builder produces the installable app and its pack: the Kotlin/Compose sources, the slot's own
theme and components, the listing copy and the pack documents. It is **not** the verifier and **not**
the critic — it does not grade its own work, and its "done" is not evidence. Its boundary is
production: it writes the app and the pack and stops, and it signs nothing, renders no release asset
and takes no screenshot until the owner's approval exists.

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

## Which rules bind this role

| Rule (subject) | Stated in |
|---|---|
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
