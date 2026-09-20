# PlayKiln

Automated **Android APK/AAB factory** for Google Play experiments.

One shared agent harness (Cursor ∥ Codex, OpenSpec, skills, MCP, `/forge` / `/ship`).
Many isolated app attempts under `apps/`. Improve the kiln once — every future
attempt inherits it.

## Quick start

```bash
# 1. Open this folder in Cursor
# 2. Enable MCP servers (Settings → MCP), then:
node scripts/setup-mcp.mjs

# 3. Install methodology skills (once):
npx skills add obra/superpowers --skill '*' -a cursor --copy -y

# 4. Bring an idea to the agent, e.g.:
#    /forge simple offline calendar with reminders
```

## Layout

| Path | Role |
|------|------|
| `AGENTS.md` | Contract for Cursor **and** Codex |
| `.cursor/` | Commands, rules, hooks, MCP, project skills |
| `.agents/skills/` | Installed superpowers methodology |
| `openspec/` | Specs + changes |
| `apps/` | One folder per APK attempt |
| `scaffold/android-compose/` | Seed for `new-slot` (not a harness fork) |
| `registry/apps.json` | Attempt catalog |
| `harness/` | Dual-agent protocol |

## Stack (new apps)

Kotlin · Jetpack Compose · Material 3 · Gradle KTS · version catalogs.

## License

Private / unset — add one when you publish the factory itself.
