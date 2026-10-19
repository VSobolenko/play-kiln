# W34 - Cutout text layout

Changed `CutoutSourceStage` in `ui/cutout/CutoutScreen.kt:460` from
`height(300.dp)` to `heightIn(min = 300.dp)`. The source stage retains its
300 dp minimum without fixing the height of its progress-text container.
The progress text remains multiline; no single-line clamp was introduced.
Existing worktree changes were preserved.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Original finding reproduced | MEASURED | `node scripts\verify.mjs --slot plinth-product-photos`: `FAIL uiux.text-layout`, `CutoutScreen.kt:460`, `height(300.dp)` on a text-bearing Surface. |
| Layout check passes after the change | MEASURED | Same source-only command: `PASS uiux.text-layout` - `no fixed extent on a text-bearing element and no maxLines clamp without an overflow in 150 Kotlin file(s)`. Log: `.kiln/scratch/copilot-plinth-w34-cutout-text/verify-after.txt`. |
| Full source-only run is not green | MEASURED | Exit 1: `122 PASS, 3 UNVERIFIED, 4 SKIP, 1 FAIL`. Remaining failure: `slot.device` records an outdated build digest (also failed before this change). Store inventory and media are UNVERIFIED because assets are absent. |
| Compilation and on-device layout | UNVERIFIED | No Gradle or `--build` command was run, per task boundary. Source-only verification reports `UNVERIFIED build`; an authorised build/device pass by the orchestrator is needed to establish runtime behaviour. |

No commits were made.
