# DESIGN.md — PlayKiln visual & product UI guidance

Always-on design guidance for agents (Factory.ai / modern harness practice).
App slots may override with their own `DESIGN.md` under `apps/<slot>/`.

## Unique visual identity (mandatory)

Every app gets its own pronounced style. The factory's edge is design, so a correct app that
looks like an unstyled Material demo is a failed recipe even when every gate is green. The
binding rule is `.cursor/rules/visual-identity.mdc`; this is its human-facing summary.

- **Declare the identity before styling UI**: `store/studio.json` names the `mark`, the full
  `palette`, the per-shot `headlines`/`kickers`, the `featureTagline` and the `featurePill`. The
  launcher icon, the adaptive-icon vectors, the feature graphic and the store compositions are
  rendered from it — it is a build input, not documentation.
- **One palette, shared**: the colours in `studio.json` are the colours in the slot's
  `ui/theme/Color.kt`, so the store pack cannot drift away from the app it describes.
- **The slot owns its theme and its components**: its own `Color.kt`, shapes and typography, and
  its own visual vocabulary (mark glyph, hero/stat panel, section strip, stat tiles, panels, empty
  states, status pill). `apps/2026-09-26-teacher-gradebook/app/src/main/java/.../ui/components/Brand.kt`
  is the reference *shape*, not a template to copy.
- **Depth and hierarchy are visible**: a gradient or tonal field, meaningful elevation, iconography,
  grouping. Not identical cards on a flat background.
- **Identity is per slot**, exactly like the job is: two slots never share a mark, palette or
  component kit (`docs/monetization-strategy.md` §1.2).
- **The style survives localisation and 48dp**: no layout whose style depends on English string
  lengths; no icon with a baked-in mask or shadow.

Still true and not optional: one primary action per screen; edge-to-edge with system-bar insets
via `Scaffold`; readable type (never tiny gray-on-gray); short purposeful motion; clarity over
decoration once the style is established.

## Product UI baseline (mandatory since 2026-09-30)

Looking distinctive is not the same as being usable. The factory's first live app had a pronounced
identity and was still rejected by the owner for being crude: a dated navigation pattern, no settings,
and text that wrapped into a column of two words. The three rules below were written from that
review, and each has a sensor that fails on a `file:line`. **They apply to every slot scaffolded from
2026-09-30 onward**; the slots that already existed carry their findings as a printed note
(`scripts/core/baseline.mjs`) because the owner's instruction was to prevent the next app, not to
repair that one.

- **Navigation is Navigation 3's, and the interface adapts.** `NavDisplay` gets the back stack
  Navigation 3 owns (`rememberNavBackStack`), so its transitions, predictive back and scene
  strategies are live. A slot with more than one destination reads the window size class or declares
  a scene strategy, so a list and its detail are two panes on a tablet instead of one stretched
  column. `.cursor/rules/ui-ux-baseline.mdc`, `docs/ui-ux-standards.md`.
- **Text can wrap and can grow.** No fixed `Modifier.width`/`height`/`size` around a label, and no
  `maxLines = 1` without an `overflow` — which defaults to `TextOverflow.Clip` and cuts a glyph in
  half. A container grows around its label; the Material 3 type scale sets the size, never a smaller
  `fontSize` chosen to make a string fit. This is the rule the owner's sentence bought: WCAG 2.2
  SC 1.4.4 names "a vertical column of text" as the failure mode.
- **A settings surface exists, and every preference in it does something.** Language, theme, and at
  least one real app-behaviour preference, persisted in DataStore. A toggle that changes nothing is
  theatre and the sensor fails it. `.cursor/rules/app-settings.mdc`.
- **A slot is born multilingual.** One language besides the default ships from the first commit, and
  every user-facing string is a resource from the first screen. Retrofitting locale trees onto a
  finished UI is the most expensive way to do the cheapest part of the work.
  `.cursor/rules/localization.mdc`.

## Factory UX for store screenshots

- Show real screens, not mock marketing mashups that the APK cannot do
- At least one screenshot of the core job-to-be-done

## Anti-patterns

- A screen whose whole visual language is default library components — stock `TopAppBar` +
  stock `TabRow` + default `Button`/`OutlinedTextField` in `Card`s on the default background.
- Card-soup dashboards for simple utilities
- The first palette you thought of, or the same palette in two slots
- Emoji, clipart or a stock icon as the app's mark
- Fake maps/social graphs in screenshots; a styled frame around an unstyled app
- Illegible icon text

## Verifying the identity

- `node scripts/verify.mjs --slot <slug>` — a pack that declares
  `store/studio.json` is composed and must bind its assets to a recorded build via
  `store/provenance.json`.
- `python scripts/visual-qa.py --slot <slug>` — independent pixel measurements: palette vs the
  app's Kotlin palette, caption ink and edge clipping, WCAG contrast, pairwise similarity of the six
  phone shots, adaptive-mask clipping of the icon, thumbnail legibility.
- `node scripts/media-capture.mjs --slot <slug>` — photographs the real signed build and refuses a
  frame that does not show the readings its shot declares.

These prove the identity is in the build, the pack and the pixels, and that two slots do not share
one. Whether the style is any good is the owner's call, at the phase-1 review and phase-2 approval.

## Verifying the product UI

`node scripts/verify.mjs --slot <slug>` runs a `ui` group beside the identity checks, and every
finding it prints is a `file:line` rather than an opinion:

- `uiux.*` — navigation, adaptive layout, text layout, motion, accessible names and touch targets,
  IME insets (`scripts/core/uiux.mjs`)
- `settings.*` — the surface exists, is reachable, and each preference reaches the code that acts on
  it (`scripts/core/settings.mjs`)
- `locale.*` — the slot is multilingual, a declared tree is a real translation, and no user-facing
  prose is compiled into Kotlin (`scripts/core/localization.mjs`)

What none of them can see is a rendered pixel: a proxy that guesses at wrapping produces false
positives, so `uiux.mjs` names the statically visible *causes* and the manual half stays with the
builder — a `@PreviewFontScales` render and a device at a 200 % font scale. Each rule lists its own
deliberate under-approximations, and a named gap is part of the rule rather than a hole in it.
