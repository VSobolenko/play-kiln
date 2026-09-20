# PlayKiln

Завод по производству Android APK/AAB для экспериментов в Google Play.

Один общий агентский харнес (Cursor ∥ Codex, OpenSpec, skills, MCP, `/forge`).
Много изолированных попыток в `apps/`. Улучшили харнес один раз — все новые
слоты получают это сразу.

## Старт

```bash
node scripts/setup-mcp.mjs
npx skills add obra/superpowers --skill '*' -a cursor --copy -y
```

В Cursor: `/forge простой офлайн-календарь с напоминаниями`

Подробности для агентов — в `AGENTS.md`.
