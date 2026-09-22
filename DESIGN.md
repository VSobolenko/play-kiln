# DESIGN.md — PlayKiln visual & product UI guidance

Always-on design guidance for agents (Factory.ai / modern harness practice).
App slots may override with their own `DESIGN.md` under `apps/<slot>/`.

## Default look (Material 3)

- Use Material 3 dynamic color when appropriate; otherwise a coherent seed color
  per app (not purple-on-white cliché unless brand demands it)
- Prefer clarity over decoration; one primary action per screen
- Edge-to-edge; respect system bars via Scaffold insets
- Typography: Material type scale; avoid tiny gray-on-gray
- Motion: short, purposeful (shared axis / fade); no gratuitous loops

## Factory UX for store screenshots

- Show real screens, not mock marketing mashups that the APK cannot do
- At least one screenshot of the core job-to-be-done

## Anti-patterns

- Card-soup dashboards for simple utilities
- Fake maps/social graphs in screenshots
- Illegible icon text
