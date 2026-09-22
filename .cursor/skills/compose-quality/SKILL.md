---
name: compose-quality
description: Compose quality bar for PlayKiln — state hoisting, stability, lists, accessibility, previews. Use when writing or reviewing UI under apps/*/.
---

# Compose quality

- Hoist state; keep composables dumb where possible
- Stable keys in Lazy lists; avoid indexing as key
- Prefer `Modifier` semantics; content descriptions for icons
- Previews for non-trivial UI
- Collect flows safely (`collectAsStateWithLifecycle`)
- No business logic in composables — ViewModel / use-cases
- Edge-to-edge: Scaffold/padding insets (API 35–36)
- Load Google `system-edge-to-edge` / adaptive skills via android-skills MCP when unsure
