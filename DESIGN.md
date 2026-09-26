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

- `node scripts/verify-slot.mjs --slot <slug> --strict-store` — a pack that declares
  `store/studio.json` is composed and must bind its assets to a recorded build via
  `store/provenance.json`.
- `python scripts/visual-qa.py --slot <slug>` — independent pixel measurements: palette vs the
  app's Kotlin palette, caption ink and edge clipping, WCAG contrast, pairwise similarity of the six
  phone shots, adaptive-mask clipping of the icon, thumbnail legibility.
- `node scripts/media-capture.mjs --slot <slug>` — photographs the real signed build and refuses a
  frame that does not show the readings its shot declares.

These prove the identity is in the build, the pack and the pixels, and that two slots do not share
one. Whether the style is any good is the owner's call, at the phase-1 review and phase-2 approval.
