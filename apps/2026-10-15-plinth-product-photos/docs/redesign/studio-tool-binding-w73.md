# W73 - selected product binding and server-owned tool instructions

Date: 2026-10-17. Scope: W72 design section 4; no tool additions, naming changes,
release actions or Studio restyling.

## Measured trace and root cause

Paths below are relative to this slot. The owner-reported car / girl-and-dog
results are **ON RECORD** in `product-rewrite-design-w72.md:20-21`; this session did
not reproduce those exact outputs against a live model or identify the bytes of
the installed build that produced them.

| Tool | Client input and server route |
|---|---|
| Generated scene (`O-07`) | `StudioViewModel.start` captures `state.photoId` in `StudioPending.photoId`. `execute` loads that ID with `ProjectImageLoader`, scales its source pixels to at most 1536 px on the longest side, encodes JPEG quality 85, uploads it and persists the returned upload ID. The job sends exactly that ID. |
| Relight (`O-08`) | Same selected-source path, not a generated preview, sample image or unrelated library item. |
| Recolour (`O-10`) | Same selected-source path. The pending record retains photo ID, upload ID, request ID and operation across retry. |
| Expand canvas (`O-09`) | Not executable from Studio: absent from `REQUEST_OPERATIONS`, and server registry disabled because this client does not supply `canvas_size`. Its provider input builder nevertheless binds source bytes and outpainting constraints when given valid dimensions. No fallback generation was enabled. |
| Cut-out (`O-02`) | Separate `CutoutViewModel`: picker URI is imported, the returned photo ID is loaded, and `prepareUpload` encodes those source pixels. `ApiClient.submitCutout` sends the returned upload ID with empty params; the pending store retains request/job identity. It uses the background-removal endpoint, not text-to-image. |

Client evidence: `app/src/main/java/com/gexetr/plinthproductphotos/ui/studio/StudioViewModel.kt:160-190,237-269`,
`ui/studio/StudioPendingStore.kt:14-27`, `data/import/ProjectImageLoader.kt:16-28`
(the latter two paths use the same Kotlin source prefix),
`ui/cutout/CutoutViewModel.kt:175-184,450-461,879-900`, and
`data/api/ApiClient.kt:102-118`.

Server evidence: `server/src/routes.ts:417-446` validates the upload belongs to
the authenticated wallet and is neither deleted nor expired, reads its stored
bytes and passes `imageBase64` to the queue. `server/src/queue.ts:185-198` decodes
those bytes and reuses the same image for a guard retry.

**MEASURED provider defects in the pre-fix implementation:** `O-07` selected
`fal-ai/flux/dev` (text-to-image), and `falImageInput` explicitly omitted its
`image_url`. An uploaded product therefore never reached that generation.
For `O-08` and `O-10`, `image_url` was already populated, but the prompt was only
the caller's text; there was no server-owned lighting/colour purpose or
same-product constraint. A wrong client image ID was not established as the
cause of the owner's relight/recolour outputs. Prompt drift is **INFERRED**
from that missing constraint, not a measured explanation of those exact images.

## Fix on disk

`server/src/registry.ts:50-53` routes scenes to `fal-ai/flux-pro/kontext`,
an image editor already used for recolour. Its registry cost uses the existing
Kontext per-image cost; wallet price remains two credits.

`server/src/providers/fal.ts:97-159` now always binds nonempty source bytes,
rejects caller overrides of source/system fields, and constructs the prompt
from a server-owned per-tool purpose:

| Operation | Constraint |
|---|---|
| Scene | Generate surroundings around the supplied product; preserve product identity and details. |
| Relight | Change illumination, highlights and shadows only; preserve product and framing. |
| Expand | Extend outside the original image, not replace or crop the product; retain the canvas-dimension requirement. |
| Recolour | Change product surface colour only; preserve shape, material, text and background; white is the default when no colour is specified. |
| Cut-out | The fixed `bria/background/remove` model supplies the tool semantics. Empty params only; a prompt is rejected rather than sent as a nonexistent provider feature. |

The generative prompts start with the supplied-image constraint, state the
tool purpose, prohibit unrelated subjects, append JSON-quoted optional user
guidance subordinate to that purpose, and finish by restating tool priority.
Relight/expand also receive server-owned negative prompts.

**Provider limitation:** the fetched Kontext and IC-Light input schemas have a
single `prompt` field and no `system_prompt` role. The server-owned system
instructions are consequently compiled into that supported field; an invented
provider role would not enforce anything. Schemas read in this session:
<https://fal.ai/api/openapi/queue/openapi.json?endpoint_id=fal-ai%2Fflux-pro%2Fkontext>
and
<https://fal.ai/api/openapi/queue/openapi.json?endpoint_id=fal-ai%2Ficlight-v2>.
This is prompt conditioning, not a hard guarantee that every generated image
preserves identity; live visual fidelity remains unverified.

`server/src/routes.ts:419-424` rejects ambiguous multi-image requests rather than
silently choosing their first image. `StudioViewModel.start` allows empty
guidance and omits blank prompt params; Studio's prompt eligibility allows an
empty instruction. Existing durable selected-photo/upload binding is retained.
Matching implementation edits and the end-to-end binding suite arrived in the
shared worktree during this session; they were inspected and preserved.

## Evidence and limits

Final observed server checks (server directory, scratch-backed environment):
`node node_modules\vitest\vitest.mjs run` reported **14 files, 179 tests,
zero failures**, exit 0; `node node_modules\typescript\bin\tsc --noEmit`
returned exit 0. Full logs are at repository-root
`.kiln/scratch/plinth-w73-tests/server-tests.log` and `server-typecheck.log`.
These are command observations, not an independent product-fidelity verdict.

- `server/test/fal-provider.test.ts`: source bytes on every supported input,
  per-tool constraints, optional/conflicting guidance, override rejection,
  missing-source rejection and image-editor scene routing.
- `server/test/studio-tool-binding.test.ts`: real HTTP upload, job route, queue
  and fal adapter with mocked external HTTP. Older and newer unrelated uploads
  cannot replace the explicitly selected upload. Multi-image input is refused.
- `server/test/studio-upload-availability.test.ts`: missing, expired, deleted
  and foreign-wallet selected uploads are refused without fallback, job creation,
  reservation or provider invocation; expansion remains explicitly unavailable.
- Regression falsification used an isolated scratch snapshot, never the shared
  product sources. Replacing the selected read with the first stored upload
  produced four image-binding failures. Restoring the pre-fix provider and
  registry produced 13 failures in the selected provider regressions. Logs:
  `.kiln/scratch/plinth-w73-mutation/wrong-upload.log` and `old-prompts.log`
  at repository root.

The concurrent server-verification worker did not launch a second Gradle process:
it observed the slot lease held by PID 144132 at 18:31 local time. The worker
holding that lease subsequently observed **BUILD SUCCESSFUL**, 138395 ms, with
`:app:testDebugUnitTest` executed (not up-to-date). This was the one W73 Gradle
process, routed through `scripts/lib/gradle.mjs` / `runGradleTasks`, with:

```text
testDebugUnitTest
--tests com.gexetr.plinthproductphotos.data.api.ApiClientTest
--tests com.gexetr.plinthproductphotos.ui.studio.StudioConfirmationPreviewTest
--tests com.gexetr.plinthproductphotos.ui.studio.StudioToolPurposeTest
--rerun
```

Fresh XML under `app/build/test-results/testDebugUnitTest/` reports **14 tests,
zero failures/errors** (6 API, 5 confirmation, 3 purpose). The new API case
checks source bytes and the returned selected upload ID across all five tool
payloads, with empty guidance. The independent source-contract command
`python app\src\test\java\com\gexetr\plinthproductphotos\test_studio_tool_binding_w73.py`
(from this slot) observed **3 tests, OK**; it checks the actual pending-photo
load/upload/job wiring and optional-instruction gates, not runtime fidelity.
English/Russian instruction labels now identify the field as optional.

After adding explicit blank-guidance and all-tool override regressions, the
final focused server run at 18:35 local time observed **87 tests, 3 files,
zero failures**, followed by successful `npm run build`:

```powershell
npm test -- --reporter=dot test\fal-provider.test.ts test\studio-tool-binding.test.ts test\studio-upload-availability.test.ts
npm run build
```

The initial pre-fix provider regression run in the implementation worker observed
**14 failed / 40 passed**, including O-07's missing image URL, wrong endpoint,
missing default prompts, missing identity constraints and missing empty-source
rejection. The earlier 179-test full-suite measurement above predates the eight
additional blank-guidance/override cases; it is not a claim of a newer full run.

**UNVERIFIED:** live-device result fidelity and deployment of these changes.
W79 must exercise the owner's phone photo against a server running this revision,
inspect subject identity and each tool's lighting/colour/background behaviour,
and verify export. No paid live inference was requested. Prompt conditioning is
not an output-identity detector, and generated scene output remains `ENHANCED`
in the adapter, not a promise of original-pixel preservation.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Pre-fix scenes discarded the selected image at the provider boundary | MEASURED | Pre-fix registry/input builder inspected; isolated pre-fix provider run rejected by 13 selected regressions, `old-prompts.log` |
| Existing Studio requests upload their persisted selected source ID | MEASURED | `StudioViewModel.kt:166-190,237-269`; `ProjectImageLoader.kt:16-28` |
| Relight/recolour previously lacked server-owned tool semantics | MEASURED | Pre-fix `falImageInput` forwarded caller prompt; `old-prompts.log` |
| Exact owner-observed wrong subjects are explained conclusively | UNVERIFIED | Requires the original installed/deployed artifacts and live reproduction; not established here |
| A wrong-upload mutation is detectable | MEASURED | Four failures in isolated `wrong-upload.log` |
| Server suite and types were exercised | MEASURED | 179 tests across 14 files, exit 0; `tsc --noEmit`, exit 0; logs above |
| Latest binding/prompt regressions and server build executed | MEASURED | Final command above: 87 tests across 3 files, zero failures; `npm run build`, exit 0 |
| Focused Android tests compiled and executed in one leased Gradle process | MEASURED | `runGradleTasks` command above, BUILD SUCCESSFUL, 138395 ms; fresh XML: 14 tests / 0 failures / 0 errors |
| Selected-source and optional-guidance source contracts are exercised | MEASURED | `test_studio_tool_binding_w73.py`: 3 tests, OK |
| Live visual output now always preserves the phone | UNVERIFIED | Needs W79 device/live-provider observation; prompt assertions alone cannot establish it |
