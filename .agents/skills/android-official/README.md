# Official Google Android skills (vendored)

Source: https://github.com/android/skills (Apache-2.0)

Curated subset used by PlayKiln agents. Refresh with Android CLI when available:

```bash
android skills add --all --project=.
```

| Folder | Purpose |
|--------|---------|
| `system-edge-to-edge` | Edge-to-edge / insets (API 35–36) |
| `play-play-policy-insights` | Pre-submit Play policy audit |
| `jetpack-compose-adaptive` | Large screens / foldables |
| `navigation-navigation-3` | Navigation 3 |
| `performance-r8-analyzer` | R8 / shrink |
| `security-android-intent-security` | Intent / export hygiene |
| `testing-testing-setup` | Test setup |
| `devtools-android-cli` | Android CLI skill |
| `build-system-agp-agp-9-upgrade` | AGP 9 migration |
| `identity-restore-credentials` | Credential restore |

Do not invent competing Android MCP servers — use these skills + Context7 + SDK.
