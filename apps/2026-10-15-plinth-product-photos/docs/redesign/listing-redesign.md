# W29 - Listing preview and editorial feedback

Implements backlog item 10 in `modern-ai-android-2026.md` section 3. Product
changes are confined to `ui/listing/ListingTextScreen.kt` and the English/Russian
`strings_listing.xml` files. No dependencies or route interfaces changed.

## Presentation and behavior

The listing is a composed surface containing its marketplace, an optional
product reference photo, the actual generated title/description, character
counts, editorial feedback, and confirmation/copy/share actions. Valid drafts
are visible before confirmation; copying and sharing still require confirmation.
Edits, marketplace changes, and language changes invalidate confirmation.
All existing composer refusal cases remain explicit: missing required details,
unrecorded limits, minimum/maximum length, and unsupported language.

The details editor sits beside the preview when its available width is at least
720 dp multiplied by the font scale. In narrower containers it follows the
preview in the same scrollable page. Language chips and action buttons wrap;
copy and description text are not ellipsized or given fixed heights. The
preview animates size changes, and editorial feedback is a polite live region.
Colors and shapes come from the existing slot theme; no new icon/font asset
or dependency was added.

The existing route provides only listing attributes, not a project or product
image. Rather than query unrelated saved projects, show a stock product, or
silently pretend to reuse the editor image, this screen offers an optional
system Photo Picker reference. It requests images only, displays the selected
local URI through the installed Coil integration, and provides replace/remove
actions. Cancellation preserves the current choice. A failed image load is
reported visibly with a recovery instruction. The URI is screen state, not a
new saved project association; restored state with unavailable URI access must
show that error rather than imply that a photo loaded.

The UI explicitly identifies the composition as a reference, not an exact
marketplace layout. The image is not uploaded and is not part of the copied or
shared payload: those remain title, two newlines, and short description, with
the existing `text/plain` share intent and title subject. This is not a renderer
for a project's finished marketplace card, an image exporter, or new AI text
generation. No marketplace acceptance guarantee is made.

## Observed checks and limits

- `node .kiln\scratch\copilot-plinth-w29-listing\check-listing.mjs` first exited
  1 against the old screen (missing Photo Picker source contract) and exited 0
  after implementation. This checks source tokens and referenced locale keys
  only; it does not execute Kotlin or establish UI behavior.
- `node scripts\check-locales.mjs --slot plinth-product-photos` exited 0:
  1013 keys in default/Russian resources, matching format arguments and plural
  categories at the time of the run. Other workers may add resources.
- `node scripts\verify.mjs --slot plinth-product-photos --json` exited 1:
  122 PASS, 1 FAIL, 3 UNVERIFIED, 4 SKIP. The failure is the stale `slot.device`
  build digest. `pack.inventory` and `pack.media` are UNVERIFIED because store
  assets are absent; `build` is UNVERIFIED because no build was requested.
  Report: `.kiln\scratch\copilot-plinth-w29-listing\verify.json` at repo root.
- Scoped `git diff --check` exited 0.

**Comp / rendered Compose behavior: UNVERIFIED.** No Gradle, compilation, device
run, screenshot, signing, release, staging, or commit was performed. Runtime
acceptance belongs to the orchestrator after its authorized build/device pass.
Exercise empty/missing marketplace data, all composer refusal states, drafts
before/after confirmation, edits after confirmation, marketplace/language
changes, exact copy/share payloads, Photo Picker cancellation/replace/remove,
unavailable/restored photo URIs, narrow and split-pane containers, Russian,
large fonts, IME reachability, TalkBack feedback, and resizing with a photo.
Visual quality, motion, and picker/Coil compatibility have not been observed.

Elapsed wall-clock time: approximately 5 minutes from task receipt to handoff.

## Claim ledger

Code paths below are relative to the slot's
`app/src/main/java/com/gexetr/plinthproductphotos/`.

| Claim | Label | Evidence |
|---|---|---|
| Preview and editor are separate file-local compositions in a responsive container | MEASURED | `ui/listing/ListingTextScreen.kt:171-215`, `:219`, `:276` |
| The product reference is explicitly selected with the system Photo Picker, not inferred from another project | MEASURED | `ui/listing/ListingTextScreen.kt:90-93`, `:182-184`, `:307-333` |
| Valid draft text appears before confirmation; copy/share remain gated | MEASURED | `ui/listing/ListingTextScreen.kt:370-420` |
| Refusal messages and character counts use the existing composer result | MEASURED | `ui/listing/ListingTextScreen.kt:375-399`, `:426-486` |
| Source-only checks are not a clean slot verification | MEASURED | Commands and actual verdicts above; repo-root `.kiln\scratch\copilot-plinth-w29-listing\verify.json` |
| Compilation, rendered layout, image-loading behavior, and interaction correctness | UNVERIFIED | Orchestrator must run its authorized build and the device cases above; no Gradle or device session in W29 |
