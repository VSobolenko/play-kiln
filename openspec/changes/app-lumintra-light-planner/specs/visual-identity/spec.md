# Visual identity

## Purpose

Lumintra's declared look: the one identity that the launcher icon, the adaptive-icon layers, the
feature graphic, the store compositions and every screen of the app are rendered from, so that the app
a user installs is the app the store pack shows and the app the owner approved. It is a build input
authored before the UI exists, not decoration applied at `/release`: the pack is composed from
`store/studio.json`, the app's colours are the same colours declared in `ui/theme/Color.kt`, the mark
is drawn rather than borrowed, and the identity is this slot's own — two apps that look identical are
the near-duplicate evidence Google Play's Repetitive Content policy acts on. The binding rule is
`.cursor/rules/visual-identity.mdc`; this capability is its normative form for Lumintra.

## ADDED Requirements

### Requirement: The identity is declared in `store/studio.json` before any UI is styled, and the pack is rendered from it

The slot SHALL carry `store/studio.json` declaring the whole identity: the `mark` (the brand glyph the
launcher icon, the adaptive-icon vectors and the in-app brand mark are derived from), the full
`palette` (every colour role the renderer draws with), the per-shot `headlines` and `kickers`, the
`featureTagline` and the `featurePill`. The file SHALL exist and SHALL declare each of those six
members before any UI code exists — in phase 1, before `ui/theme/Color.kt` and the screens are
written — and it SHALL be a build input rather than documentation: the launcher icon, the adaptive-icon
foreground and themed/monochrome layers, the feature graphic and the store compositions SHALL all be
rendered from it in phase 2, not hand-authored independently of it.

A pack that renders without a declaration, or that falls back to a generic identity, SHALL be a
defect, not a shortcut. `scripts/store/render.py` SHALL refuse to render when `store/studio.json` is
absent or incomplete rather than silently producing the generic pack, and the absence of the
declaration SHALL NOT be concealed by a render that succeeds anyway. Deleting `store/studio.json`
after `store/assets/` has been populated SHALL still fail, so removing the declaration cannot switch a
check off.

#### Scenario: The declaration is authored in phase 1, before the theme exists

- **WHEN** the slot is read before any UI code is written
- **THEN** `store/studio.json` exists and declares the `mark`, the full `palette`, the per-shot `headlines` and `kickers`, the `featureTagline` and the `featurePill`, and the identity decision therefore predates the screens rather than following them

#### Scenario: The pack is rendered from the declaration in phase 2

- **WHEN** the store pack is composed in phase 2 after the owner's release approval
- **THEN** the launcher icon, the adaptive-icon foreground and monochrome layers, the feature graphic and the store compositions are all rendered from `store/studio.json`'s `mark`, `palette`, `headlines`, `kickers`, `featureTagline` and `featurePill`, not from values authored anywhere else

#### Scenario: A render without a declaration is refused, not degraded to the generic pack

- **WHEN** the pack renderer runs with `store/studio.json` absent or missing one of the six members
- **THEN** the render refuses and names the missing declaration, rather than producing the generic pack the identity exists to replace

#### Scenario: Deleting the declaration does not switch the requirement off

- **WHEN** `store/studio.json` is removed while `store/assets/` still holds composed media
- **THEN** the pack still fails for the absent declaration, so the requirement cannot be satisfied by deleting its input

### Requirement: The declared palette is the app's own theme, with one source of truth

Every colour declared in `store/studio.json`'s `palette` SHALL be the colour the app uses: the values
SHALL be the values in the slot's own `ui/theme/Color.kt`. No colour the renderer draws with SHALL be
written twice — a store pack whose palette has drifted from the app's own theme is a screenshot of a
different app, and the drift SHALL be treated as a defect rather than as two acceptable shades of the
same intent. A change to a colour role SHALL be made in one place and reflected in the other in the
same revision.

The measurement is owned elsewhere and is cited here rather than restated: `scripts/core/palette.mjs`
(the `palette` group) reads the **declared** `studio.json` palette against the slot's own `Color.kt`,
and `scripts/visual-qa.py`'s `palette` section measures the **shipped pixels** against the Kotlin
palette. Both SHALL agree that the declared identity is the installed identity.

#### Scenario: An edited `Color.kt` with a stale `studio.json` is a failure

- **WHEN** a colour role in `ui/theme/Color.kt` is edited while `store/studio.json` still declares the previous value
- **THEN** the drift is a failure named by `scripts/core/palette.mjs` and by `scripts/visual-qa.py`'s `palette` section, because the pack and the app no longer declare the same colour

#### Scenario: A colour is written once

- **WHEN** a renderer colour role present in `store/studio.json` is searched for in the slot's Kotlin sources
- **THEN** its app counterpart resolves to the same value in `ui/theme/Color.kt`, and no second, independently authored copy of that value exists

#### Scenario: The pixels are the declared palette

- **WHEN** the shipped screenshots are measured against the app's own Kotlin palette
- **THEN** the palette section reports the pack and the app as one identity, so the pack cannot advertise a palette the app does not render

### Requirement: The slot owns its theme and its component vocabulary

The slot SHALL own its theme as slot code, not as library defaults: its own `ui/theme/Color.kt`,
`ui/theme/Type.kt`, `ui/theme/Shape.kt` and `ui/theme/Theme.kt`, its own shapes and typography, and
its own dark and light role mapping. The Material 3 defaults — including the default purple seed and
dynamic colour — SHALL be replaced, not shipped; a screen whose colours come from an unmodified default
seed SHALL be a failure.

The slot SHALL own its component vocabulary as slot code under `ui/components/`, containing at
minimum: a **mark glyph** (`ui/components/LumintraMark.kt`), a **hero / stat panel**
(`ui/components/SkyCard.kt`, with `ui/components/StatTile.kt` for the individual facts), a **section
strip or tabs** (`ui/components/SectionStrip.kt`), a **panel**, an **empty state**
(`ui/components/EmptyState.kt`) and a **status pill** (`ui/components/ConditionChip.kt`). Each
component SHALL encode a decision the app makes repeatedly — the darkness description, the
moonless-versus-moonlit state, a target's fit, a forecast's age — rather than being a thin wrapper
around a library `Card` or `Surface`. The depth the identity requires SHALL be visible: a gradient or
tonal field, elevation that means something, a glyph set used as iconography, and spacing that
separates groups, rather than a grid of identical cards on a flat background.

#### Scenario: A stock screen with the default seed is a failure

- **WHEN** a screen's entire visual language is stock `TopAppBar`, `TabRow`, `Button`, `OutlinedTextField` and `Card` components on the default background with the default Material 3 seed and dynamic colour enabled
- **THEN** it is a failure, because the slot has not replaced the defaults and its components encode no decision of its own

#### Scenario: The required component vocabulary exists as slot code

- **WHEN** `ui/components/` is read
- **THEN** the mark glyph, the hero / stat panel, the section strip or tabs, the panel, the empty state and the status pill each exist as slot-owned composables

#### Scenario: The theme is the slot's own, not the library's

- **WHEN** `ui/theme/Color.kt`, `Type.kt`, `Shape.kt` and `Theme.kt` are read
- **THEN** the palette, typography and shapes are declared by the slot and mapped across the app's dark and light states, and no screen renders from an unmodified Material 3 default seed or dynamic colour

#### Scenario: The background is not a flat grid of cards

- **WHEN** a main screen is rendered
- **THEN** depth and hierarchy are visible — a gradient or tonal field, meaningful elevation and grouped spacing — rather than a field of identical library cards on a flat background

### Requirement: The mark is drawn from the declaration, not borrowed, and it survives the adaptive-icon masks

The launcher icon, the adaptive-icon foreground, the themed/monochrome layer and any in-app brand mark
SHALL all be generated from the `mark` declared in `store/studio.json`, and SHALL NOT be a stock
library glyph, an emoji, clipart, or a stock icon. The mark SHALL read at **48 dp**, SHALL fit inside
the **66 dp safe zone**, and SHALL carry **no ink beyond the circle keyline**; the launcher icon and the
adaptive-icon layers SHALL carry **no baked-in rounded mask and no drop shadow**, because Play rejects
both. The Android 13+ monochrome/themed layer SHALL be the same mark flattened to a single ink colour
and SHALL remain legible when the system tints it, so hue is not carrying the mark's structure.

The geometry measurement is owned elsewhere and is cited here rather than restated:
`scripts/core/icon.mjs` (ink outside the 66 dp safe zone, beyond the circle keyline),
`python scripts/check-adaptive-icon.py`, and `scripts/visual-qa.py`'s `icon` section (adaptive-mask
clipping). The provenance half — that the mark is not a library glyph the set forbids as a brand — is
`scripts/core/icons.mjs`'s `icons.mark` and `icons.launcher` legs, and the licence reason is
`docs/icon-provenance.md` rule 13 (Remix Icon License v1.0 §3.3 forbids brand and app-icon use).

#### Scenario: The mark reads at 48 dp and inside the safe zone

- **WHEN** the launcher icon, the adaptive-icon foreground and the monochrome layer are measured
- **THEN** the mark reads at **48 dp** and carries no ink outside the **66 dp safe zone** or beyond the circle keyline

#### Scenario: No mask and no shadow is baked into the mark

- **WHEN** the launcher vector and the adaptive-icon layers are read
- **THEN** neither a baked rounded mask nor a drop shadow is present, because Play rejects a launcher icon that supplies its own

#### Scenario: A stock glyph or an emoji is never the mark

- **WHEN** the launcher icon, the adaptive-icon foreground, the monochrome layer or the in-app brand mark is read
- **THEN** each is derived from `store/studio.json`'s declared `mark`, and none is a stock library glyph, an emoji or clipart

### Requirement: UI iconography is one licence-cleared set, vendored with its licence and provenance; the mark is never one of its glyphs

Every general-purpose UI glyph — a back arrow, a plus, a bin, a settings gear, an empty-state symbol —
SHALL come from **one** licence-cleared professional icon set, vendored into the slot during
development and committed rather than fetched at build or run time, with its verbatim licence text
under `icons/LICENSES/<SPDX>.txt` and a per-icon provenance record at `apps/<slot>/icons/provenance.json`
(contract: `docs/icon-provenance.md`). A vendored UI icon SHALL be tinted from the theme — from
`LocalContentColor.current` or an explicit `MaterialTheme.colorScheme.<role>` — and SHALL carry **no
colour of its own**: a hard-coded fill in a vendored `VectorDrawable` or `ImageVector` is a defect. A
glyph whose meaning the app's own subject defines — the horizon line, the mark, the red-mode control —
SHALL stay slot-owned and SHALL be drawn in the library set's stroke weight and on its grid, rather
than bending a library icon to mean something it does not.

Hand-drawing a general-purpose glyph — a back arrow, a plus, a bin, a settings gear — as Compose
`Canvas` or path primitives SHALL be a defect. The measurement is `scripts/core/icons.mjs` (the `icon`
group), cited rather than restated: `icons.provenance`, `icons.licence`, `icons.offline`,
`icons.deprecated` and `icons.tint` decide the set, its licence text, its committed-only provenance and
its theme tint; `icons.mark` and `icons.launcher` decide that the mark is not a library glyph. Its
honest limit is stated there and not repeated here: it proves a licence was recorded, on an allowlist,
with its text committed — it cannot prove the bytes came from where the record says.

#### Scenario: A library glyph reused as the app's mark fails

- **WHEN** the launcher icon, the adaptive-icon foreground, the monochrome layer or the in-app brand mark is a glyph borrowed from the vendored UI set
- **THEN** it fails, both because the mark is slot-owned and because a set that forbids brand use (Remix Icon License v1.0 §3.3) may not represent the brand

#### Scenario: A hand-rolled back arrow fails

- **WHEN** a general-purpose glyph — a back arrow, a plus, a bin, a settings gear — is drawn as Compose `Canvas` or path primitives instead of taken from the vendored set
- **THEN** it is a failure, because these glyphs come from the licence-cleared professional set rather than from primitives one agent drew

#### Scenario: A domain glyph stays slot-owned, drawn in the set's grid

- **WHEN** a glyph whose meaning the app's subject defines — the horizon line, the mark, the red-mode control — is drawn
- **THEN** it is slot-owned and drawn in the vendored set's stroke weight and on its grid, rather than a library icon bent to mean something it does not

#### Scenario: The set is committed with its licence and provenance

- **WHEN** a vendored UI icon is used
- **THEN** `icons/provenance.json` records it against a licence on the allowlist, its verbatim licence text is committed under `icons/LICENSES/`, and the icon carries no colour of its own

### Requirement: The identity survives translation, landscape and large font scales

No layout's style SHALL depend on English string lengths, because a style that only fits the English
words breaks the moment the longest declared locale is loaded. The section strip SHALL render an
inactive tab as **its mark alone** when the longest declared locale's labels cannot fit — the same
mechanism the reference slot uses — rather than squeezing, truncating or overflowing its labels. A
fixed-width pill, a one-line headline that cannot wrap, or a label row whose height is fixed SHALL be a
defect that appears only after localisation. The identity SHALL hold in **landscape** and at the
largest system font scale: labels SHALL abbreviate rather than clip, and no styled container SHALL
truncate a value.

The layout response itself is owned elsewhere and is cited rather than restated: `specs/night-scene/spec.md`
owns the portrait/landscape and large-font behaviour of the `Night` screen, including the window size
class reading, the 200 % font scale reflow and the RTL rule. This requirement adds only that the
**identity** — the strip, the pills, the panels and the mark — survives the same conditions without a
style that depends on English.

#### Scenario: The section strip is mark-only when labels cannot fit

- **WHEN** the section strip is rendered in the longest declared locale and its labelled tabs cannot all fit at the minimum legible size
- **THEN** an inactive tab renders as its mark alone, and no label is squeezed, truncated or allowed to overflow

#### Scenario: A fixed-width, one-line, English-length style is a defect

- **WHEN** a styled element — a fixed-width pill, a one-line headline or a label row — is rendered with the longest declared locale's strings
- **THEN** it is a failure if its style depended on English string lengths, because the defect appears only after localisation

#### Scenario: The identity holds at 200 % font scale

- **WHEN** the system font scale is set to 200 %
- **THEN** the strip, the pills, the panels and their labels reflow without clipping a value and without a fixed-height container truncating it

#### Scenario: The identity holds in landscape

- **WHEN** a screen carrying the identity is rotated to landscape
- **THEN** the strip, the panels and the mark reflow within the window rather than being clipped or pushed outside it, in the arrangement `specs/night-scene/spec.md` requires of the `Night` screen

### Requirement: The identity belongs to this slot, and no two slots share it

Lumintra SHALL NOT share its mark, its palette or its component kit with any other slot in the
catalogue: the mark SHALL be derived from this slot's own subject, the palette SHALL be derived for
this slot, and the component vocabulary SHALL be this slot's own. The reason SHALL be recorded rather
than implied — Google Play's Repetitive Content policy treats near-duplicate apps as a terminating
offence, and a catalogue whose apps look identical is the same near-duplicate evidence as a catalogue
whose apps do the same job (`docs/monetization-strategy.md` §1.2; `.cursor/rules/visual-identity.mdc`).
"Our house style" SHALL be treated as a name for reusing a slot's identity, and reuse SHALL be a
defect.

**The honest boundary is recorded rather than glossed.** No program here can compare two slots' marks
or palettes and decide they are too alike, and no program can judge whether an identity is
**beautiful, distinctive or ugly** — a palette that passes the contrast and chroma arithmetic is a
legal palette, not a good one. What the gates prove is narrower and is cited rather than restated:
`scripts/core/palette.mjs` reads the declared `studio.json` palette against the slot's own
`Color.kt`; `scripts/core/icon.mjs` and `python scripts/check-adaptive-icon.py` measure the mark's
geometry; `scripts/core/icons.mjs` decides the icon set, its licence and its provenance; and
`python scripts/visual-qa.py --slot <slug>` measures the shipped pixels. Together they can say the
declared identity is the one in the build, the one in the pack and the one in the pixels. The
judgement that separates a distinctive identity from an ugly one SHALL stay with the owner, at the
phase-1 review and the phase-2 approval — this capability's job is to ensure there is something to
judge.

#### Scenario: The shipped pixels carry the declared identity

- **WHEN** `python scripts/visual-qa.py --slot lumintra-light-planner` is run against the shipped pack
- **THEN** its palette and icon sections measure the declared identity in the installed pixels, so the identity the owner reviewed is the identity a user sees

#### Scenario: Two slots sharing an identity is a defect

- **WHEN** two slots' `store/studio.json` declarations are read and their marks or palettes are the same or a renamed copy
- **THEN** the reuse is a defect for this slot's own identity, because a catalogue of apps that look identical is the near-duplicate evidence Play's Repetitive Content policy acts on

#### Scenario: The beauty of the identity is the owner's decision, not a gate

- **WHEN** the identity passes every objective gate — the declared palette matches `Color.kt`, the mark fits its masks, the icons are licence-cleared and tinted
- **THEN** the gates report only that the identity is **declared, shared with the app and measured in the pixels**, and the judgement that it is distinctive rather than ugly remains the owner's at the phase-1 review and the phase-2 approval

**Honest boundary, recorded rather than glossed.** This capability fixes what the identity is made of
and what the objective checks can read; it cannot prove a pack is beautiful. `scripts/visual-qa.py` is
a **report, not a gate** — it exits 0 whenever it finishes, so a clean run is evidence to read rather
than an approval — and the two objective halves that do gate are `scripts/core/palette.mjs` (declared
colours against `Color.kt`, honest that it reads declared colours and can never call a palette
beautiful) and `scripts/core/icons.mjs` (a licence recorded, on an allowlist, with its text committed,
honest that it cannot prove the bytes came from where the record says). No program here compares two
slots' marks or palettes, so the "no two slots share an identity" rule is enforced by reading the
declarations, not by a machine; and the store compositions that render from `store/studio.json` are
produced in phase 2, so in phase 1 this requirement is satisfiable as a declaration without a rendered
pack. What is proved today is that the identity is declared, that the app's theme is that declaration,
and that the shipped pixels agree with both.

## MODIFIED Requirements

None. This change adds a new capability and modifies no existing spec.
