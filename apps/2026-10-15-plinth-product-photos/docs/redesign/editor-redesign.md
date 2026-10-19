# W28 - Editor: the product is the hero

Implements item 9 of `modern-ai-android-2026.md`, section 3. This is a
screen-local hierarchy change, not a new editing engine.

## Layout and interaction

The composed image is the default view. Its square canvas fits inside a separate
preview stage rather than sharing a long scrolling list with every control.
At an available content height of at least 600 dp and font scale up to 1.3,
compact windows reserve 1.6 shares for the preview and one for independently
scrolling tools. From 840 dp available width, preview and tools sit side by side
at a 1.5:1 ratio. These measurements are after scaffold insets and screen padding,
not physical device dimensions.

Shorter windows and larger font scales use one scrolling workspace instead of
crushing either panel. In that fallback the image stage stays 320 dp tall while
the filename, comparison/history actions and tool content grow naturally. The
preview can scroll out of view in this accessibility/short-window fallback.

Four wrapping, single-selection tool families replace the flat control list:

| Family | Existing controls retained |
|---|---|
| Background | Paper, Graphite, Travertine |
| Shadow | None, Contact, Soft drop |
| Framing | Photo fill, margin, item scale |
| Position | Horizontal and vertical position |

Only the selected family's controls appear. Each family has a heading and a short
instruction; chips expose Material selection semantics. Comparison is explicitly
labelled Original / Composed, with unavailable original comparison disabled.
Undo and redo are outlined, labelled actions beside comparison, outside the
scrolling tool panel, enabled by `canUndo` / `canRedo`. An edit, undo or redo
returns comparison to Composed so the user sees its effect rather than an
unchanged original. Comparison and tool selection use saveable screen state.
All chips/buttons retain at least 48 dp height and labels can wrap.

No dependency, palette, public callback signature, slider range, command key,
composition session, persistence or export behavior was changed. Existing
`PlinthScaffold` migration changes were retained. Position still uses the
screen's existing preview translation; this task does not make it a new
pipeline/export feature. Existing command-key history coalescing is unchanged.

## Verification and claim ledger

Paths below are relative to the slot unless stated otherwise.

| Claim | Status | Evidence |
|---|---|---|
| Preview/tool split, responsive ratios and short/large-text fallback are implemented in source | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/ui/editor/EditorScreen.kt:118-170`, `:187-230` |
| Four selected tool families retain all five slider controls | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/ui/editor/EditorScreen.kt:179-184`, `:263-353` |
| Comparison and history use selected/enabled state; edits and history return to composition | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/ui/editor/EditorScreen.kt:119-141`, `:231-251` |
| Eight new editor strings have English and Russian resources | MEASURED | `app/src/main/res/values/strings_plinth.xml:42-49`; `app/src/main/res/values-ru/strings_plinth.xml:42-49` |
| Source-contract and locale checks pass, and the staged old editor fails the new hierarchy predicate | MEASURED | Repository-root command: `python .kiln\scratch\copilot-plinth-w28-editor\verify_editor.py`, exit 0; four PASS lines. Checks callback signature/ranges/wiring, source layout predicates, XML parsing/string resolution and delimiter balance only |
| Bounded code/resource diff has no whitespace errors | MEASURED | Repository-root `git diff --check --` with the editor screen and both `strings_plinth.xml` files, exit 0 |
| Compilation / Compose runtime / rendered hierarchy / accessibility behavior | UNVERIFIED | No Gradle, device, screenshot or instrumentation commands run, as required by W28 |

The scratch checker is disposable source inspection, not an Android test and not
a Kotlin parser. Passing it does not establish compilation or pixel correctness.

## Runtime acceptance still required

Use a debug build supplied by the orchestrator. In English and Russian, inspect
a normal phone, a wide window, a short landscape window, and large system text.
Confirm the image stays visible while scrolling tools in bounded layouts; in the
fallback confirm the whole page scrolls and labels/actions are not clipped.
Switch every family, exercise every control, compare Original / Composed, and
confirm each edit returns to Composed. Check disabled undo/redo at the initial
state, enabled undo after a change, redo after undo, and redo clearing after a new
edit. Confirm loading, initial load failure and composition failure remain
readable, and verify chip selection/button names with TalkBack.
