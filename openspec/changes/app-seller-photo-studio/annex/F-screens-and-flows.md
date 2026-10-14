# F — Screens, navigation and flows

Navigation 3 hosts the back stack (`ui-ux-baseline.mdc`). Window size class: compact = single pane with a
slot-owned bottom strip; medium/expanded = rail and list-detail (project list | project). Every screen
handles: empty, loading, error, offline, process death, 200 % font scale and the longest of the six
locales. Interactive elements are ≥ 48 dp with a content description. The wallet pill (balance) is in the
top bar of S-02, S-04, S-05, S-07, S-14 and opens S-15.

## Adaptive and accessibility contract (normative)

Navigation 3 owns one saved/restored back stack and predictive back pops it. Compact uses one pane;
medium uses list-detail; expanded may add a supporting preview pane. Scene choice follows current window
size/posture. Edge-to-edge scaffolds consume safe-drawing and IME insets; media may draw behind bars but
controls may not. Every action is keyboard reachable, semantically named and at least 48 dp; status is not
colour-only and `ProofRing` exposes its label to accessibility services.

## Destinations

Strip: **Studio** (S-02), **Library** (S-13), **Coach** (S-16), **Settings** (S-17).

| Id | Screen | Purpose | Key content and actions | States to build |
|---|---|---|---|---|
| S-01 | Welcome | first run only | one-sentence promise ("your item stays yours — everything around it is styled"), marketplace multi-select, language picker; ends at S-03 | skipped when a project exists |
| S-02 | Studio (home) | projects | project cards with thumbnails, marketplace chips, item counts, "New project"; recent Looks row; wallet pill | empty (first project invitation), loading, error |
| S-03 | Import | bring photos in | system picker (multi), camera shortcut to S-16, paste-from-clipboard image, import progress with chunking (E02), duplicates banner (E07) | empty, in progress, partial failure list (E03) |
| S-04 | Smart card | the one-tap flow | big result preview, category chip (editable, E10), marketplace chip(s), scene carousel (procedural first, "generate scene — 2 credits"), buttons: **Make card**, **Fix for <marketplace>**, **Save as Look**, **Apply to all**, **More** (→ S-05). Shows cost line before any server call | cutting, composing, failed (E08, E19), low-confidence mask preview (E14) |
| S-05 | Editor | manual control | tabs: Mask (brush add/remove, smart edge, undo/redo), Background (solid, gradient, procedural scene, generated), Shadow, Fill & margin, Crop/rotate/exposure; before/after toggle; stale-outputs banner (E28) | each tab empty, process-death restore (E69) |
| S-06 | Looks | saved looks | grid of Looks with mini-previews, rename, duplicate, delete, "apply to project"; backup state ("included in backup") | empty, in use |
| S-07 | Batch | many items, one Look | item grid with state per item (queued, cutting, composed, failed), progress, "Retry failed", "Pause", "Re-apply Look" (E27); notification toggle (E58, E59) | resume after restart (E35), large batch (E57) |
| S-08 | Group & SKU | structure | groups as rows with draggable items (E60), SKU pattern editor (`{base}-{n}`), reference-photo marking (E61), OCR suggestions with confirm (E62) | empty, regroup |
| S-09 | Marketplaces & check | rules | per-marketplace card: "meets the rules we read on <date> from <source>" or findings list from annex L with fix buttons, override with "Export anyway" (E30), badge for secondary/stale presets (E32) | all-pass, errors, original-only explanation (E31) |
| S-10 | Slide kit | infographic series | template picker, per-slide text fields with live character counters, prohibited-text feedback (L11), contrast indicator (L12), series preview at 246 px width, "apply look to all slides" (annex I) | empty, invalid text |
| S-11 | Listing text | titles/descriptions | language selector (6), marketplace limits counters, attributes confirm, Generate (1 credit), copy/share, edit-in-place | not generated, generating, over-limit (E65) |
| S-12 | Export | handoff | pack per marketplace (file names `{sku}_{role}_{n}`), format/size summary, share sheet, save to gallery, ZIP (Storage Access Framework create-document), manifest (overrides, provenance), "send to myself" shortcut = share | exporting, partial, too large |
| S-13 | Library | find again | search (SKU, category, marketplace, text), filters, multi-select with bulk actions (change marketplace, re-export, delete), project open | empty (E67), scale (E68) |
| S-14 | Studio tools | Enhanced operations | tool list with cost and provenance label: Relight, Expand, Erase, Upscale, Recolour, Ghost mannequin, Virtual model, Series; disabled-with-reason on gated marketplaces (E31, E53); result compare slider; "Report" (E54); guard details sheet | running, rejected (E49, E50), moderation (E51) |
| S-15 | Wallet | credits and unlock | balance, packs and the unlock with store prices (E48), restore purchases, recovery code (show/copy once, E40), ledger preview, "how credits work", free-vs-credit table | pending (E36), confirming (E37), failed |
| S-16 | Coach | shooting | CameraX preview with level (gravity sensor), framing overlay for the selected marketplace aspect, shot list for the chosen category (checklist), quality check after capture (blur, exposure, item fill), "use photo" → S-04 | permission denied (E63), offline (E64), no camera |
| S-17 | Settings | every setting acts | language, default marketplaces, output format/quality, strip location, confirm-spend threshold, notifications, Wi-Fi only, keep sources, purge outputs, theme, Looks backup note, privacy policy (in-app), delete my data, wallet recovery, about | — |
| S-18 | Privacy & data | compliance | policy opens through `CustomTabsIntent`, what leaves the phone, delete data action (API-14), report help | — |

## Smart buttons (the named one-tap actions)

| Button | Does | Where |
|---|---|---|
| Make card | cut-out + category scene + compose (annex E §Auto-card) | S-04 |
| Fix for <marketplace> | applies the linter's `fixAction`s for that marketplace in one step | S-04, S-09 |
| Make white main | composes MAIN on `#FFFFFF` at the preset's fill | S-04 |
| Apply to all | applies the current Look to every item in the project | S-04, S-07 |
| Square / 3:4 | re-composes at the aspect without touching the Look | S-05 |
| Slide kit | opens S-10 with the Look's slide style | S-04, S-12 |
| Export pack | renders every marketplace's pack and runs the linter | S-12 |

## First-run journey (the device test follows it)

S-01 → S-03 (import 1 photo) → S-04 (**Make card**, uses 1 starter credit if the cut-out is a server
operation; the cost line is shown first) → result with the linter verdict → **Save as Look** →
S-03 (import 5 more) → S-07 (**Apply to all**, batch runs) → S-09 → S-12 (**Export pack**, share sheet).
The journey passes with airplane mode off; steps that do not need the network (S-05 brush, S-16 coach,
S-12 export of already-composed outputs) are also exercised with airplane mode on.

## Visual identity hooks (annex J)

Slot-owned components required by `visual-identity.mdc`: a **mark glyph**, a **hero panel** on S-02 and
S-04 showing the item on its plinth with real item stats (items, credits, packs ready), a **section strip**
whose inactive pills are glyph-only (survives the longest locale), a **status pill** for item/preset state,
an **item tile** with a state ring, and **empty states** drawn with the mark. General glyphs (back, plus,
share, settings, trash, camera, search) come from one licensed set recorded in
`icons/provenance.json`.
