# H — Compliance, AI labelling and provenance

Labels per `.cursor/rules/grounded-claims.mdc`. Everything marked `S6` is read and recorded by the
`play-auditor` spike before the Data safety form or the privacy policy is written (task 2.7).

## Play policy facts the build must satisfy

| Requirement | Source | Build consequence |
|---|---|---|
| Apps that generate images with AI "must contain in-app user reporting or flagging features … without needing to exit the app"; and must prevent Restricted Content and Deceptive Behavior | ON RECORD: Developer Program Policy, effective 2026-08-26 (fetched 2026-10-12), section "AI-Generated Content" | `Report` action on every generative result (S-14), `POST /reports` (API-11), the server stores the report; `slot.inAppReport` is a task-8 manual check until a project check exists |
| Subscriptions are not used | owner's rule (`ideation-gate.mdc`) | the app queries only `INAPP` products; a test asserts no `SUBS` query |
| Latest stable libraries; Fragment floor | `dependency-freshness.mdc`, `scripts/lib/play-dependency-floors.json` | slot declares the Fragment constraint at or above the floor, `deps.artifact` runs on the release bundle |
| Privacy policy: public URL and in-app | `privacy-policy.mdc` | `node scripts/publish-privacy.mjs` during development; policy opened from S-17/S-18 |
| Permissions minimised | AGENTS.md | picker needs none; `CAMERA` and `POST_NOTIFICATIONS` requested at point of use only |

## What leaves the phone (the privacy facts the policy and Data safety must state)

| Data | Leaves the phone? | To whom | Retention | Note |
|---|---|---|---|---|
| Photos for cut-out, recognition, generative operations | yes, only for operations the user starts, downscaled to ≤ 2048 px | our server → the model provider (fal) | uploads ≤ 1 h, results ≤ 24 h on our server; provider retention `S6` | never for local-only operations |
| Text typed for listing text | yes | server → text model | not stored beyond the request | |
| Purchase token | yes | server → Google Play Developer API | token stored hashed | |
| `installId`, wallet id | yes | server | until `DELETE /data` | random identifiers, not hardware IDs |
| Report note | yes | server | until `DELETE /data` | |
| Location | never read | — | — | EXIF GPS stripped from outputs |
| Contacts, microphone, accounts | never used | — | — | |

Data safety rows to draft (S6 confirms the category wording and the "shared vs processed on behalf" rule):
Photos and videos — collected (processed ephemerally), not shared with third parties for their own use
(`S6`: provider terms); Purchase history — collected via Play Billing; Device or other IDs — collected
(`installId`); App activity — none; Crash logs — none unless a crash reporter is added (it is not).
**UNVERIFIED** until read: whether a provider's no-training/retention terms exist for the chosen
endpoints; the policy text states only what the app does and names the provider category, and `S6`
records the provider terms before any "not used for training" sentence is allowed.

## Provenance model

`Provenance` is in the data model (annex C). The mapping to what the user and the file show:

| Provenance | In-app label | File metadata |
|---|---|---|
| `ORIGINAL_PIXELS` | badge "Original pixels" with the solid proof ring | `xmp:CreatorTool = "<App name> <version>"` only |
| `COMPOSITE_GENERATED_BACKGROUND` | badge "Item untouched · background generated" with a split ring | `Iptc4xmpExt:DigitalSourceType` set to the IPTC term for a composite containing trained-model content (`S6`) + `CreatorTool` |
| `ENHANCED` | badge "AI-enhanced" with the dashed ring and the guard summary | `DigitalSourceType` set to the IPTC term for trained-algorithmic media (`S6`) + `CreatorTool` |

- `UNVERIFIED`: the exact IPTC URIs. The names `compositeWithTrainedAlgorithmicMedia` and
  `trainedAlgorithmicMedia` are recalled from the IPTC NewsCodes digital-source-type vocabulary and were not
  read in this session. **S6 reads `cv.iptc.org` and records the exact URIs; the builder never types one
  from memory.** If S6 cannot read the vocabulary the metadata field is not written and the UI badge
  still is; the export manifest records `provenance` in plain text regardless.
- **XMP writing is own code** (`domain/export/XmpWriter`): JPEG gets an APP1 segment with the
  `http://ns.adobe.com/xap/1.0/` header; PNG an `iTXt` chunk keyed `XML:com.adobe.xmp`; WebP an `XMP `
  RIFF chunk. Tests: write → parse back; the file still decodes; a file with an existing XMP is merged, not
  duplicated. Stated as a design fact: `androidx.exifinterface` is not relied on for XMP writing.
- **C2PA content credentials are out of scope for v1.** Signing requires a certificate chain the factory
  does not have; no claim of C2PA is made anywhere. `UNVERIFIED` current C2PA conformance requirements —
  recorded as a follow-up decision for the owner.
- `UNVERIFIED`: whether any target marketplace requires disclosure of AI-generated images or whether EU or
  other law requires a machine-readable marker for these outputs. The app labels and records regardless;
  whether a duty exists is a lawyer's question and the policy text does not assert it.

## Generative safety and abuse

- **No free-text prompts in v1.** Scenes come from recipes with fixed prompt templates that never mention
  the item (`scene-recipes.json`); the user picks from a list. A free-text scene field is an M3 option that
  requires a server-side text moderation call and a refusal path before it ships.
- **Virtual model (O-14) is `enabled:false` in `/config` until `play-auditor` clears it.** It generates a
  person, which touches Deceptive Behavior and consent questions the other operations do not.
- Provider content filters are honoured (`MODERATION_BLOCKED`, E51); the server never retries around a
  filter.
- Reports (`API-11`) are stored and reviewed by the operator; a `WRONG_PRODUCT` or `UNSAFE` report on an
  operation increments a per-op counter in the server log used by the operator to disable that op via
  `/config`.

## Intellectual property

Sellers upload product photos. The Terms line (shown at S-01 and S-18): "You confirm you have the rights
to the photos you use." The app cannot detect counterfeit goods; Play's Intellectual Property policy is
ON RECORD in the same policy document. A report category `OTHER` is available for a rights complaint and
the operator contact is the listing's `contactEmail`.

## Billing facts

- ON RECORD (Play Billing Library release notes, fetched 2026-10-12): newest release listed is 9.1.0
  (2026-06-18). The slot pins the latest stable at scaffold time; the builder reads the release notes page
  again then and records the version.
- Consumable packs are consumed by the server after crediting, the one-time unlock is acknowledged by the
  server, and the client refreshes with `queryPurchasesAsync`; Play refunds an unacknowledged purchase
  after 3 days, so E37 forbids letting a confirmed-but-uncredited purchase sit unretried.
- The client holds no Play credential; `purchases.products.get` and consume/acknowledge use the server's
  service account only.

## Native code and 16 KB

The client uses no bundled native libraries of its own; ML Kit text recognition and barcode scanning are
taken as the Google Play services (unbundled) variants where available (S3 decides), so
`scripts/check-16k.py` has nothing to measure beyond the platform's libraries. If a bundled-model variant
is chosen at S3, the builder runs the 16 KB check and records it.
