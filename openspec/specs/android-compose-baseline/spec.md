# Factory capability: Android Compose baseline

## Purpose

All new PlayKiln apps share a modern Android baseline so agents do not invent
conflicting stacks.

## Requirements

### Requirement: Compose-first Kotlin apps

New slots SHALL be Kotlin projects using Jetpack Compose and Material 3, built
with Gradle Kotlin DSL and a version catalog, with `compileSdk`/`targetSdk` ≥ 36.

#### Scenario: Scaffold

- **WHEN** `scripts/new-slot.mjs` runs
- **THEN** the slot contains an `:app` module with Compose enabled, placeholder
  `MainActivity` using `enableEdgeToEdge`, and backup extraction rules XML

### Requirement: Play-oriented packaging

Release distribution for Google Play SHALL prefer Android App Bundle (`bundleRelease`).
Compliance details live in `play-compliance` spec.

#### Scenario: Release packaging

- **WHEN** a slot is prepared for release
- **THEN** the release route produces a signed AAB alongside the APK, and the slot's
  `dist/release-manifest.json` records both artifacts

### Requirement: Navigation hosted by Navigation 3

The navigation host SHALL be `NavDisplay` driven by the back stack Navigation 3 owns
(`rememberNavBackStack`), with one `@Serializable` `NavKey` per destination. A hand-rolled
`SnapshotStateList` / `listSaver` back stack SHALL NOT be passed to `NavDisplay`, because
Navigation 3 keys its transitions, predictive-back continuity, scene strategies and
deep-link handling off the back stack it is given, so a list it does not own makes all four
inert at once.

#### Scenario: The scaffold and every new slot

- **WHEN** `node scripts/verify.mjs --slot <slug>` runs the `ui` group
- **THEN** `uiux.navigation` reports no `mutableStateListOf` / `listSaver` back stack behind
  `NavDisplay`, and `uiux.scaffold` reports none in `scaffold/android-compose/` either

### Requirement: A multi-destination slot reads the form factor

A slot with more than one destination SHALL name at least one adaptive API — a window size
class (`currentWindowAdaptiveInfo`, `WindowSizeClass`), a scene strategy
(`rememberListDetailSceneStrategy`, `rememberSupportingPaneSceneStrategy`), or an adaptive
grid (`GridCells.Adaptive`). A list destination that opens a detail destination SHALL state
its two-pane policy rather than rendering one stretched column on a tablet.

#### Scenario: A list and its detail on a wide window

- **WHEN** the slot is rendered on a window wide enough for two panes
- **THEN** the list and the opened destination are shown side by side, and
  `uiux.adaptive` reports no finding

### Requirement: Text can wrap and can grow

No user-facing text SHALL be placed in a fixed `Modifier.width` / `height` / `size` extent,
and no `Text` SHALL clamp with `maxLines = 1` without an `overflow` — which defaults to
`TextOverflow.Clip` and cuts a glyph in half. A container SHALL grow around its label, and
the Material 3 type scale SHALL set the size rather than a smaller `fontSize` chosen to make
a string fit a box the layout chose. The failure this prevents is named by WCAG 2.2 SC 1.4.4,
whose own failure text describes a sentence rendered as a vertical column of words.

#### Scenario: A long locale and a large font scale

- **WHEN** `node scripts/verify.mjs --slot <slug>` runs the `ui` group
- **THEN** `uiux.text-layout` reports no fixed extent around a text-bearing element and no
  `maxLines` clamp without an `overflow`

### Requirement: A settings surface with preferences that act

Every slot SHALL ship a settings destination that is reachable from inside the app, offering
at least the app language, a light/dark/system theme choice wired into the theme host, and one
behaviour preference that app logic outside the settings surface reads. Every preference SHALL
survive the process through `preferencesDataStore`. A control that changes nothing SHALL be
treated as a defect rather than padding; the contract is
`.cursor/rules/app-settings.mdc`.

#### Scenario: A preference is changed

- **WHEN** a user changes a preference on the settings screen
- **THEN** the change is persisted, and the code that acts on it reads it on the next
  composition; `settings.reachable`, `settings.theme`, `settings.behaviour` and
  `settings.persistence` report no finding

### Requirement: A slot is born multilingual

Every slot SHALL declare at least one language besides the default in
`res/xml/locales_config.xml` and SHALL ship a matching `res/values-<locale>/` tree from its
first commit. Every user-facing string SHALL be a resource, and a translated tree SHALL carry
real translations rather than placeholder tokens or copies of the default. A pseudolocale
SHALL NOT ship.

#### Scenario: Creating a slot

- **WHEN** `scripts/new-slot.mjs` copies `scaffold/android-compose/`
- **THEN** the new slot declares and ships the scaffold's additional language, and
  `locale.multilingual`, `locale.placeholder` and `locale.hardcoded` report no finding

### Requirement: Applicability is derived, not hand-maintained

A baseline rule that takes effect on a given date SHALL gate only the slots that exist from
that date. A slot created earlier, a pipeline proof with no `economics.json`, and a killed slot
SHALL carry a finding as a printed note inside a PASS rather than as a red row, because a gate
that stays red on a shipped app teaches the operator to read past it — and because a finding
that exists and is not reported would read exactly like a finding that is not there.

#### Scenario: A slot predating the baseline

- **WHEN** `node scripts/verify.mjs --slot <old-slug>` runs the `ui` group
- **THEN** each finding is printed with `reported but not gating` and the verdict is PASS
