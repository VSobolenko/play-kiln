# W94c — Studio instruments (Scene / Relight / Recolour)

W95 integration verified the server and Android build. The O-09 registry failure was not pre-existing relative to the staged server baseline: `vitest run test/fal-provider.test.ts test/registry.test.ts` passed 47/47 there, where O-09 was disabled. The W94 worktree had enabled O-09 even though the client still supplies no `canvas_size`; W95 restored the disabled flag and its reason. The full server suite then passed 189/189.

## Scene: request payload

Endpoint `fal-ai/flux-pro/kontext` (O-07, an image *editor*; unchanged).

**Before** (client prompt, when a recognised category + marketplace preset with `white-255` policy exist):
```
Product category: X. Preserve the original product and its details.
Marketplace: …; main background policy: white-255.
Use a pure white (#FFFFFF) background.
Soft natural contact shadow. Product fill target: …
Seller background idea: in the jungle
```
→ server wrapped it as: purpose "…Change only its surroundings to a clean, realistic product-photography setting" + `Optional user guidance (use only when it agrees with the tool purpose…)`. Two instructions said "white / clean"; the jungle was the weakest, optional line. Result: white background.

**After**: client sends only the seller's words (`"in the jungle"`); engine text is used only when the field is empty. Server `image_url` + `num_images:1` + prompt:
purpose "Replace the whole background and surroundings with the setting the seller describes below … must be clearly different from the source background" + guidance line + "For this tool the described setting is the requested new background: render it visibly, and never leave a plain white or the original background unless the seller asked for that" + identity constraints.
Server also now throws `PROVIDER_ECHOED_INPUT` if the provider's output bytes equal the input (no charge for an echo).

Provider returns: its own generated image fetched from the fal output URL (`registry.ts` run → `falImage` → fetch); never the input unless byte-identical, now rejected.
UNVERIFIED: I did not call fal live; that jungle output differs visibly is not measured here. Run one live probe before release. Scene is NOT removed.

## Defects

| Defect | Root cause | Fix | User sees |
|---|---|---|---|
| E2 dead end | `GeneratedSceneRoute.kt` conflict branch showed text only; `StudioViewModel.kt` `choosePhoto`/`start` throw `JOB_UNRESOLVED` | `finishPending()` / `discardPending()` in VM; two buttons in the route | "You have an unfinished edit — Finish it / Discard and continue" |
| E2 invisible leftovers | `refresh()` restored any old delivered result as pending | delivered leftovers are auto-saved as labelled variant and cleared (`settleLeftover`) | no message at all in the usual case |
| E5a stale photo | `refresh()` set `photoId = pending?.photoId` even for finished jobs, and Scene `choosePhoto` was blocked while `hasPending` | only an unresolved job pins its photo; leftovers cleared first | Scene opens on the chosen photo |
| E5b white background | prompt composition above + server "clean setting" purpose; scene prompt >240 chars also failed `studioPromptIsValid` | see payload; scene limit 2000 in `studioPromptIsValid` | jungle prompt renders a jungle (unmeasured live) |
| E6 before/after | `StudioRoute` rendered comparison for any `state.result` | requires `state.result != null && state.operation == activeOperation`; restored results no longer exist | first visit: photo + action only |
| E7 button | panel label always "Generate · N"; save button was the primary filled one | `studioActionLabel`: "Recolour · N credits", "Create background · N", "Relight · N"; save is an `OutlinedButton` "Save this result to the item" | button names the action |
| E9 what is recoloured | nothing shown | photo preview with primary border, chip "Will be recoloured", title "Recolour the product", note that it recolours the main product and no object/part can be picked yet | visible target, honest limit |
| 7 chips | already filled the field (`StudioRoute` panel, `GeneratedSceneRoute`) | unchanged; field stays primary and editable | — |

Line anchors (post-edit, find by symbol): `StudioViewModel.refresh/settleLeftover/finishPending/discardPending/saveVariant`, `StudioRoute.studioActionLabel`, `StudioWorkflowPanel` preview block, `GeneratedSceneRoute` `val prompt` and `conflictingPending` branch, `fal.ts` `STUDIO_TOOL_PURPOSES['O-07']`/`studioToolPrompt`, `registry.ts` `PROVIDER_ECHOED_INPUT`.

## Tools removed
None. Recolour still whole-photo/model-chosen: copy says "main product", never a selection.

## Follow-ups (outside my files)
- `androidTest` w78/w83/w87 assert text `studio_live_generate_cost` for Scene/Relight/Recolour; they need `studio_live_action_*`.
- Studio still picks "current photo" = most recent import (`StudioRoute` has no photo argument from `PlayKilnApp.kt:768`); an explicit photoId param would need the app shell.

## Claim ledger
| Claim | Status | Evidence |
|---|---|---|
| Server compiles | MEASURED | `npx tsc --noEmit` in `server/`, exit 0 |
| Server tests | MEASURED | `npx vitest run` in `server/`, 15 files / 189 tests passed |
| O-09 disabled test was pre-existing | MEASURED: no | Staged baseline `vitest run test/fal-provider.test.ts test/registry.test.ts`: 47/47 passed; staged `server/src/registry.ts` declares O-09 disabled |
| Prompt override cause | MEASURED (read) | `AutoCardEngine.kt:325-336`, `fal.ts` old purpose line 138 |
| Pending pins photo | MEASURED (read) | `StudioViewModel.kt` old `refresh` |
| Jungle image differs | UNVERIFIED | no live fal call |
| Kotlin compiles | MEASURED | `node scripts/verify.mjs --slot plinth-product-photos --build`, assembleDebug passed |
