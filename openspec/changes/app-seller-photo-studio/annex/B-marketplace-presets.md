# B — Marketplace presets: schema, status, seed

Seed data is `annex/marketplaces.seed.json`. The builder copies it to
`app/src/main/assets/marketplaces.json` unchanged at M1 and may only (a) raise a `status` after a
first-party read, (b) add an entry that carries `sources` with a read date, (c) correct a number to what a
first-party page says. Inventing, rounding or "fixing" a number from memory is forbidden.

## Schema (`MarketplacePreset`)

| Field | Type | Meaning |
|---|---|---|
| `id` | string | stable key; also the `M-<id>` citation |
| `name`, `regions[]`, `queue` | | display; ISO region codes; 1 = ships M1, 2 = ships M2 |
| `editPolicy` | `enhanced-allowed` \| `original-only` | `original-only` disables cut-out, background replacement, generated and Enhanced operations (E31) |
| `status` | `first-party` \| `secondary` \| `unread` | `unread` never reaches the UI |
| `verifiedAt` | ISO date | shown in the UI; older than 120 days shows "rules may have changed" (DECISION) |
| `sources[]` | `{title, site, printedDate, label, firstParty, quotes}` | the evidence; at least one per shown entry |
| `image` | see below | the technical limits |
| `main` | see below | the main-photo rules |
| `text` | `{prohibited[], contrastMin, avoidColourPairs[]}` | slide-text rules; `prohibited` uses ids from `textCategories` |
| `listing` | `{titleMax, descriptionMax, descriptionMin, ...}` | listing-text limits when read |
| `notes[]` | strings | behaviours worth showing the seller |

`image`: `formats[]`, `maxBytes`, `minWidth`, `minHeight`, `minLongSide`, `maxLongSide`, `maxSide`,
`maxSidesSum`, `maxCount`, `aspect` (`"3:4"`, `"1:1"` or null), `recommended {w,h,basis}`,
`minJpegQuality`, `noTransparentPng`.

`main`: `backgroundPolicy` (`white-255` \| `neutral` \| `neutral-only` \| `any`), `minFillRatio`,
`textOnMain` (`forbidden` \| `penalised-by-size` \| `forbidden-categories-only` \| `infographic-allowed-except-select` \| `not-stated` \| `n/a-original-only`),
`forbidWatermark`, `forbidBorders`, `forbidPackaging`, `itemMustBeFullyVisible`.

## Rules for the data

1. **A shown preset needs a source and a date.** `build.presets` (project check, task 5.4) fails when an
   entry with `status != unread` has no `sources` or no `verifiedAt`, and when a number field appears with
   no entry in `sources`. It is verified by deleting a source from a copy of the file and seeing it fail.
2. **Conflicts are data.** The WB minimum height is 900 in one help article and 933 in another; the entry
   states the larger in the field and the other in `minHeightAlternative`, and the linter message names both.
3. **Rejected sources are recorded** (`rejectedSources`) so a later session does not re-add the Ozon
   "1:1 / 1000×1000 / RGB 255" claim.
4. **Presets are bundled in v1.** No remote rule updates (no signing story for remote rules, and a remote
   rule file is another thing to secure). Staleness is shown instead of hidden. Revisit after S5.
5. **A seller may override an `error` rule** with an explicit labelled override ("export anyway"); the
   override is recorded in the export manifest and is never default (E30).
6. **The app never claims acceptance.** It says "meets the rules we read on <date> from <source>"
   (`S-09` Marketplaces & check, and repeated on `S-12` Export), never "approved by <marketplace>" — no
   marketplace's moderation is observable from here.

## First-party reads required (spike S5)

Ozon, Avito, eBay, Etsy, Depop, Amazon, Shopify, then Poshmark and Mercari (unread). A read converts
`secondary` to `first-party` and replaces the `sources` row; the seed's secondary rows stay as history in
the evidence document.
