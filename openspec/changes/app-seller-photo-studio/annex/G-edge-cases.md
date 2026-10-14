# G — Edge-case register

Each row is a state or failure the app must handle, with the required behaviour and where it is tested.
Specs cite these ids. "T:" names the test class or journey step (annex K).

## Import and sources

| Id | Case | Required behaviour | Test |
|---|---|---|---|
| E01 | Picker returns 0 photos | Return to the screen, no error toast | T:ImportFlow |
| E02 | Selection > 100 photos | Import in chunks of 25 with progress; the first chunk is usable immediately | T:ImportFlow |
| E03 | HEIC / WEBP / 16-bit PNG / animated GIF | Decode what `ImageDecoder` supports; animated → first frame with a notice; undecodable → per-item error, the rest continue | T:Decoding |
| E04 | The picker URI permission expires | Sources are copied into `files/sources` at import; nothing reads the URI after the copy | T:SourceCopy |
| E05 | Storage full during copy | Stop importing, keep the items copied so far, show the shortfall in MB | T:Storage |
| E06 | Source > 48 MP | Decode with `inSampleSize` for processing; keep the original file; mask computed at ≤ 4096 px longest side and upscaled with edge-aware refinement | T:LargeSource |
| E07 | Duplicate photo (same SHA-256) in a project | Offer "skip duplicates"; default skip | T:Duplicates |
| E08 | Mask empty or covers < 2 % / > 98 % of the frame | Item marked `FAILED(NOT_FOUND)` with "we couldn't find the item"; offer manual brush, retry, or a different photo; credit for that cut-out is released | T:MaskSanity |
| E09 | Source small: needed scale `s > 2.0` | Compose anyway, mark "small source", offer Upscale (O-11) with its cost | T:SmallSource |
| E10 | Recognised category wrong | The category chip is editable at any time; changing it re-picks the scene recipe without a server call | T:AutoCard |
| E11 | Several items in one photo | Cut-out returns the largest connected component by default; a "pick the item" tap selects another component; "keep all" keeps them as one item | T:MultiObject |
| E12 | Item touches the frame edge | Composition keeps the cut edge; shadow is suppressed on cut edges; warn "item is cut off by the frame" (WB requires the item fully visible) | T:EdgeContact |
| E13 | Transparent or glassy item | Mask may be partial; show the "glass" hint: keep Original mode and use a darker solid; Enhanced is discouraged for it | T:Glass |
| E14 | White item on white background | Cut-out confidence low → mask preview overlay is mandatory before composing; hint to shoot on a contrasting surface | T:WhiteOnWhite |
| E15 | Hand or person in the photo | Cut-out may include them; the brush removes them; the coach advises | T:Hand |
| E16 | Rotated / mirrored EXIF | Orientation applied at import; stored copy keeps the original bytes | T:Orientation |
| E17 | Non-sRGB source | Convert to sRGB at compose time using the embedded ICC; if the profile is unreadable assume sRGB and say so in details | T:ColourProfile |
| E18 | Photo has EXIF GPS | Never read for any feature; stripped from outputs by default | T:Exif |

## Cut-out, composition, outputs

| Id | Case | Required behaviour | Test |
|---|---|---|---|
| E19 | Cut-out fails (network/provider) | Item `FAILED`, credit released, "Retry" per item and "Retry failed" per batch | T:CutoutRetry |
| E20 | Cut-out succeeds, compose fails (OOM) | Retry at half canvas then full; if still failing, mark with a clear message; no credit is affected (cut-out already settled) | T:ComposeOOM |
| E21 | Output exceeds `maxBytes` at the quality floor | Downscale in 5 % steps until it fits; report the final size | T:SizeFit |
| E22 | Export with `strip_exif = false` | Allowed; location is stripped regardless; only date and device model may remain | T:Exif |
| E23 | Preset says `white-255` and the Look has a scene | MAIN slot is forced to white; EXTRA slots keep the scene; the UI shows why | T:WhiteMain |
| E24 | Preset has `aspect = 3:4` and the Look canvas differs | Canvas follows the preset for that export; the Look is not modified | T:AspectExport |
| E25 | Output small enough but item fill below `minFillRatio` | L08 warns; one-tap `FillToRatio` | T:Fill |
| E26 | Two marketplaces selected with conflicting rules (Amazon white vs Depop square) | One export pack per marketplace with its own files; shared work is rendered once per distinct (canvas, background) pair | T:MultiMarketplace |
| E27 | Seller changes the Look after a batch | A "re-apply Look" action re-composes without re-cutting; masks are reused | T:ReapplyLook |
| E28 | Mask edited after outputs exist | Outputs are marked stale; re-compose offered | T:StaleOutputs |
| E29 | Same SKU number twice | SKU pattern guarantees uniqueness inside a project by suffixing `-2`, `-3`; manual duplicates are refused | T:SkuUnique |
| E30 | Seller exports despite an `ERROR` finding | Allowed only through "Export anyway"; the manifest records every overridden rule id | T:Override |
| E31 | `original-only` marketplace selected | Cut-out, background, generated and Enhanced actions are disabled with the reason; only crop/rotate/exposure/white-balance remain; export is the original bytes cropped/adjusted | T:OriginalOnly |
| E32 | Preset `status = secondary` or older than 120 days | Shown with "rules read from <source> on <date>; verify"; the copy never says "approved" | T:PresetBadge |

## Wallet, purchases, server

| Id | Case | Required behaviour | Test |
|---|---|---|---|
| E33 | Insufficient credits | Cost shown, balance shown, shortfall highlighted, wallet sheet opens; no job is created | T:Insufficient |
| E34 | Network lost mid-job | Job stays `RUNNING` on the server; the client re-polls on reconnect; if it never returns, `RUNNING > 180 s` becomes `FAILED` with release | T:NetworkLoss |
| E35 | App killed during a batch | The queue is durable; on next launch pending jobs resume; finished items are not reprocessed | T:QueueResume |
| E36 | Purchase pending (cash/slow payment) | Pack shows "Pending"; credits arrive when Play reports it purchased; no double credit | T:PendingPurchase |
| E37 | Purchase confirmed by Play but server unreachable | Purchase is not consumed or acknowledged on the client; retried on next launch and on resume; Play's 3-day acknowledge window is respected; UI shows "confirming" | T:ConfirmRetry |
| E38 | Forged or replayed token | Server `PURCHASE_INVALID` or the first result again; never credits twice | T:ForgedToken |
| E39 | Refund / voided purchase | Authenticated RTDN triggers an authoritative Play API re-read; a scheduled paginated Voided Purchases sweep with persisted cursor and overlap reconciles missed events. Apply one idempotent `REFUND_ADJUST` for the full/partial voided quantity: deduct available credits to zero and place the remainder in nonnegative `refundDebt`; block new paid model jobs while debt exists, and future purchased credits repay debt before becoming spendable. Local/free features remain usable. A voided unlock removes its entitlement. | T:Voided |
| E40 | Reinstall after buying credits | Settings → "Restore wallet" accepts the recovery code; the unlock is restored from Play (`queryPurchasesAsync`); credits are not recoverable without the code and the first purchase screen says so | T:Recovery |
| E41 | New phone | Device transfer may carry Room content but no wallet id/token; wallet access requires the recovery code and unlock restore uses Play; otherwise as E40 | T:Transfer |
| E42 | Starter credits farmed by reinstalls | `installId` is not anti-reinstall proof; a challenged supported integrity binding grants once, otherwise grant zero; daily budget is secondary containment | T:StarterCap |
| E43 | Daily spend cap reached | `CAP_REACHED`; Original-mode local work continues; the message offers "try again tomorrow" | T:SpendCap |
| E44 | Kill switch on | As E43 with `KILL_SWITCH` | T:KillSwitch |
| E45 | App too old | `VERSION_TOO_OLD` blocks paid operations; local work continues | T:MinVersion |
| E46 | Price changes between estimate and call | `PRICE_CHANGED`; the dialog shows the new cost | T:PriceChanged |
| E47 | Starter credits and Play country with no purchases (e.g. unsupported billing) | Local features and starter credits work; the wallet explains purchases are unavailable | T:NoBilling |
| E48 | Currency/price display | Prices come from `ProductDetails.formattedPrice`; the app never converts or hardcodes | T:StorePrices |

## Guard and generative

| Id | Case | Required behaviour | Test |
|---|---|---|---|
| E49 | Guard rejects the first result | Silent retry with a new seed; the user sees one spinner | T:GuardRetry |
| E50 | Guard rejects twice | `FAILED(GUARD_REJECTED)`, release, message recommends Original mode, "report" available | T:GuardFail |
| E51 | Provider moderation blocks an input | `MODERATION_BLOCKED`, release, no retry loop | T:Moderation |
| E52 | Result contains text/logo not in the source | Guard `text` fails; reject | T:TextDrift |
| E53 | Enhanced result on a marketplace gated `original-only` or white main | Action disabled before it can run | T:Gate |
| E54 | Report an output | `POST /reports`; confirmation; the output stays on the user's phone | T:Report |
| E55 | Result expired (> 24 h) before receipt | The client downloads immediately after `SUCCEEDED` into a temporary file, verifies length/digest, fsyncs where supported, atomically renames and durably retries API-09R. Failed/partial delivery is re-fetchable until `expiresAt`; only a valid idempotent receipt settles, otherwise expiry releases | T:ResultExpiry |

## Batch, library, shoot coach, text

| Id | Case | Required behaviour | Test |
|---|---|---|---|
| E56 | One batch item fails | The rest continue; failed items are listed with Retry | T:BatchIsolation |
| E57 | Batch with 300 items | Disk and memory bounded: ≤ 2 concurrent compositions on the device, ≤ 4 concurrent server jobs per device; progress notification updates at most once per 2 s | T:LargeBatch |
| E58 | Batch finished while the app is closed | A notification when the whole batch settles (if permitted); tap opens the batch | T:BatchNotify |
| E59 | Notification permission denied | Feature degrades to an in-app banner; Settings explains how to enable | T:NotifyDenied |
| E60 | Grouping wrong | Drag items between groups; changing a group's SKU base renumbers deterministically | T:Regroup |
| E61 | Reference photo (ruler/coin/label) included in export | `REFERENCE` role is excluded from every export by construction | T:ReferenceExcluded |
| E62 | OCR reads a wrong SKU/dimension | Every OCR value is a suggestion shown for confirmation; nothing is applied silently | T:OcrConfirm |
| E63 | Camera permission denied | Coach explains and offers the picker instead; no crash | T:CameraDenied |
| E64 | Coach in airplane mode | Guidance (level, framing, shot list, quality check) works with no network | T:CoachOffline |
| E65 | Listing text exceeds marketplace limit | L19 error; one-tap trim at a sentence boundary | T:TextLimit |
| E66 | Text language ≠ UI language | Text is generated per selected language independent of the UI language | T:TextLanguage |
| E67 | Search finds nothing | Empty state with filters cleared action | T:SearchEmpty |
| E68 | Library with 5 000 items | Paged queries; search by SKU/category/marketplace under 300 ms on the test phone | T:LibraryScale |

## Platform

| Id | Case | Required behaviour | Test |
|---|---|---|---|
| E69 | Process death during editing | Editor state (current mask revision, step list) restores from Room; unsaved brush stroke is dropped | T:ProcessDeath |
| E70 | Dark theme / font scale 200 % / RTL not in scope | Layouts hold at 200 % font scale on the longest-locale strings; dark theme implemented | T:UiScale |
| E71 | Per-app locale change | UI re-renders in the selected language; listing-text language remains independent | T:Locale |
| E72 | Backup restore | Looks and settings return; no tokens or balances return | T:Backup |
| E73 | Offline start | Local features work; wallet shows the last cached balance marked "offline" | T:Offline |
| E74 | Clock skew | JWT `iat/exp` checked with ±5 min tolerance; mismatch shows a clock message | T:ClockSkew |
