---
name: "/ui-verify"
id: "ui-verify"
category: "Review"
description: "Runtime UI sweep for Compose slots (emulator / screenshot evidence)"
---

# /ui-verify — runtime UI verifier

Static review and Gradle assemble miss blank screens and dead navigation.
This role drives (or screenshot-verifies) Compose UI for the **blast radius**
of the change.

## Scope

1. Diff → list changed files under `apps/<slot>/`
2. Map blast radius (navigation, theme, shared composables → wider sweep)
3. Checklist: one item per screen/flow with a concrete assertion

## Drivers (use what is available)

| Driver | When |
|--------|------|
| Android emulator + adb + screenshot | Preferred for Compose UI |
| Maestro (if installed for the slot) | Flow YAML under `apps/<slot>/.maestro/` |
| Playwright | Only for web helper pages / Play Console — not a substitute for native UI |

If no emulator is available, say so explicitly, capture compose previews /
build success, and mark UI runtime as **BLOCKED** (not PASS).

## Failure handling

Hand findings to the Build peer with repro steps; after fix, re-run only the
affected checklist items.
