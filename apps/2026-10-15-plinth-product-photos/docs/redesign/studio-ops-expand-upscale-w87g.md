# W87g — Expand / Upscale / Erase in live Studio

## Change
- `StudioViewModel.REQUEST_OPERATIONS` now includes `EXPAND` (O-09), `UPSCALE` (O-11), `ERASE` (O-12); `start()` uses the same quote/charge/pending-receipt path as Relight.
- `studioRows` lists the three with existing strings (`studio_operation_expand_canvas`, `studio_tool_title_upscale`, `studio_operation_erase_region`; purposes `w6_studio_purpose_*`). Credits come from `config.ops[wireId]`; a row is hidden when the op is disabled.
- Tool form is the shared workflow panel: photo chip, optional short instruction (required only for scene), Generate · N credits. No new strings.
- `VIRTUAL_MODEL` is not in `StudioOperationCatalog`'s live path and is left out.

## Claim ledger
| Claim | Status | Evidence |
|---|---|---|
| The three ops pass the `require(operation in REQUEST_OPERATIONS)` gate | MEASURED by source | StudioViewModel.kt REQUEST_OPERATIONS |
| Rows appear in live Studio | UNVERIFIED on device | no device run in this task |
| Server accepts empty/optional `prompt` params for O-09/O-11/O-12 | UNVERIFIED | server contract not read here |
| Upscale resolution (2K/4K) and erase mask are not sent | Known limit | only optional prompt is in `params` |
| Compile | see handoff | `:app:compileDebugKotlin` |
