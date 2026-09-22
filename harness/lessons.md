# Harness lessons (factory memory)

Append entries when `/diagnose` or `/improve-harness` finds a recurring failure.
Entries are append-only and remain in chronological insertion order.

## Template

```
### YYYY-MM-DD — short title
- Symptom:
- Root cause:
- Fix class: (AGENTS.md | skill | sensor | rule | spec)
- Change made:
```

---

### 2026-09-20 — Cursor surface completion
- Symptom: factory used skills/commands/MCP but missed Cursor **custom subagents** and full hook lifecycle
- Root cause: incomplete Cursor ecosystem map (docs: subagents, sessionStart, stop, .cursorignore)
- Fix class: `.cursor/agents/*`, expanded hooks.json, `.cursorignore`, `harness/cursor-surface.md`
- Change made: this entry

### 2026-09-22 — release-factory hardening
- Symptom: long Codex work vanished after MCP timeouts; invalid reasoning values failed; AGP 9 rejected the Kotlin plugin, `kotlinOptions`, and the old ProGuard file; `sdkmanager` crashed with `0xC0000409`; PowerShell copied from the wrong interpolated path; scaffold builds could not rely on system Gradle.
- Root cause: long work used a short-dialogue transport, stale AGP conventions and unverified effort values survived, machine-specific SDK/PowerShell behavior was assumed away, and the wrapper JAR was omitted.
- Fix class: runner protocol, AGP contract, machine setup, shell convention, scaffold completeness.
- Change made: use `node scripts/codex-run.mjs` for long work and reserve MCP `codex` for short dialogue; accept only `none|low|medium|high|xhigh|max`; omit `org.jetbrains.kotlin.android` and `android.kotlinOptions {}` and use `proguard-android-optimize.txt`; unpack the official platform zip when `sdkmanager` crashes; use `"$($obj.FullName)"` instead of `"$obj.FullName"`; ship `gradle/wrapper/gradle-wrapper.jar` because no system Gradle exists.

### 2026-09-22 — untested scripts shipped defects that reading did not catch
- Symptom: `new-slot.mjs` copied 145 MB of Gradle build output into every new slot and ran text substitution over binary `.so`/`.aab` files; the scaffold theme referenced `Theme.Material3.DayNight.NoActionBar`, a Material **Components View** style absent from a Compose-only app, so AAPT failed on first build; a `when` over a sealed interface carried a dead `else -> error(...)`; `new-slot.mjs` renamed the package only in `app/src/main`, leaving test and benchmark source roots stale.
- Root cause: scripts and sources were authored and reported as done without ever being executed. Boundary behaviour was assumed (what a recursive copy contains, which resources a Compose-only app packages, which source roots hold the package) instead of observed.
- Fix class: sensor + rule + single-source-of-truth module.
- Change made: new `scripts/selftest.mjs` runs the real `new-slot.mjs` against a temp scaffold sandbox and asserts the slot contract (no build state, wrapper intact, package moved in every source root, no token or old-package residue, registry correct, bad input refused, error text matches the actual rule) — wired into `verify-slot.mjs` so it runs on every gate; `scripts/lib/namespaces.mjs` becomes the single definition of `com.gexetr.*`, imported by `new-slot.mjs` and `verify-slot.mjs` after a hardcoded message drifted from its own regex; `.cursor/rules/verification-discipline.mdc` encodes the rules; sensor validated by injecting the defect and confirming FAIL, then PASS after removal.

### 2026-09-22 — published namespace is `com.gexetr.*`
- Symptom: slots were scaffolded as `com.playkiln.<slug>`, which is not the namespace the developer account publishes under.
- Root cause: the factory's internal name was used as the Play namespace; the account's existing convention was never recorded.
- Fix class: AGENTS.md + rule + shared module.
- Change made: `com.gexetr.<slug>` everywhere (scaffold, capability libraries, gates, docs); one-shot repo sweep covering plain, regex-escaped, string-escaped, and path forms plus package directory renames; convention recorded in `.cursor/rules/project-conventions.mdc` and `AGENTS.md`.
