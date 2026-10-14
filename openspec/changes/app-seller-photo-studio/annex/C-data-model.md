# C — Data model

`DECISION` throughout unless a row says otherwise. Package root is
`com.gexetr.<slug-without-dashes>` (derived by the scaffold; `com.gexetr.plinthproductphotos` for the
chosen name). All domain types are pure Kotlin in `domain/` and carry no Android import.

## Client domain types (`domain/model`)

```kotlin
enum class Provenance { ORIGINAL_PIXELS, MANUALLY_ADJUSTED, COMPOSITE_GENERATED_BACKGROUND, ENHANCED }
//  ORIGINAL_PIXELS: item pixels untouched; surroundings from solid/gradient/bundled procedural scene.
//  COMPOSITE_GENERATED_BACKGROUND: item pixels untouched; surroundings from a generated scene (O-07/O-09).
//  ENHANCED: a generative or restoring model changed item pixels (O-08, O-10..O-14, O-11).

enum class SlotRole { MAIN, EXTRA, REFERENCE, SLIDE }       // REFERENCE = scale/ruler/label photo, never exported
enum class ItemState { IMPORTED, CUTTING, CUT, COMPOSED, FAILED }
enum class JobState { QUEUED, RESERVING, RUNNING, SUCCEEDED, FAILED, CANCELLED }

@JvmInline value class Mask(val alpha: ByteArray)           // width*height, 0..255, same size as source
data class Canvas(val width: Int, val height: Int)           // output canvas in px

data class BackgroundSpec(
  val kind: Kind,                                            // SOLID, GRADIENT, PROCEDURAL_SCENE, GENERATED_SCENE
  val colorArgb: Int?, val color2Argb: Int?, val angleDeg: Int?,
  val sceneId: String?,                                      // procedural scene id or generated asset id
  val sceneAssetPath: String?,                               // files/scenes/<id>.jpg for GENERATED_SCENE
  val prompt: String?, val seed: Long?
) { enum class Kind { SOLID, GRADIENT, PROCEDURAL_SCENE, GENERATED_SCENE } }

data class ShadowSpec(val kind: Kind, val opacity: Float, val blurPct: Float, val offsetPct: Float) {
  enum class Kind { NONE, CONTACT, DROP }
}                                                            // defaults: CONTACT, 0.35, 0.02, 0.010

data class Look(
  val id: String, val name: String, val background: BackgroundSpec, val shadow: ShadowSpec,
  val marginPct: Float = 0.06f,                              // of min(canvas w,h)
  val fillTarget: Float = 0.80f,                             // bbox long-axis occupancy; see E §Fill
  val slideStyleId: String? = null,                          // annex I
  val createdAt: Long
)

data class Item(
  val id: String, val projectId: String, val sourcePath: String, val sku: String?, val groupId: String?,
  val role: SlotRole, val category: String?, val categoryConfirmed: Boolean,
  val attributesJson: String?, val scaleHintJson: String?,
  val maskPath: String?, val maskRevision: Int, val state: ItemState, val lastError: String?
)

data class Output(
  val id: String, val itemId: String, val path: String, val width: Int, val height: Int,
  val bytes: Long, val format: String, val provenance: Provenance, val operations: List<String>,
  val marketplaceId: String?, val role: SlotRole, val createdAt: Long
)
```

## Client persistence

| Store | Contents | Notes |
|---|---|---|
| Room (KSP) `app.db`, version 1 | tables `project`, `item`, `look`, `job`, `output`, `export_record`, `group` | one DAO per table; `exportSchema = true`, schema committed; migrations tested |
| Files `files/sources/<itemId>.jpg` | the **copied** source (the picker URI grant is not trusted to outlive the session) (E04) | JPEG re-encode is forbidden: the copy is the original bytes; format preserved |
| Files `files/masks/<itemId>-<rev>.png` | 8-bit alpha masks | last 5 revisions kept for undo |
| Files `files/outputs/<outputId>.<ext>` | rendered outputs | deletable from Library |
| Files `files/scenes/<id>.jpg` | generated scene assets bound to a Look | |
| Files `files/backup/looks.json` | the Looks table serialised, rewritten on every Look change | the only user content that enters cloud backup |
| DataStore Preferences `settings` | keys below | |
| DataStore Preferences `wallet` | `installId`, `deviceToken`, `walletId`, `balanceCache`, `unlockCache`, `tokenGeneration` | **excluded from cloud backup and device transfer**; transfer uses recovery code only |

DataStore `settings` keys (each one changes behaviour; `settings.*` check reads them):
`default_marketplaces: Set<String>`, `output_format: jpg|png|webp`, `jpeg_quality: 70..95` (default 90),
`strip_exif: Boolean = true`, `confirm_spend_over: Int = 1` (credits; the cost is always shown, the extra
confirm dialog appears above this), `notify_batch_done: Boolean = true`, `wifi_only_uploads: Boolean = false`,
`keep_sources: Boolean = true`, `purge_outputs_days: Int = 0` (0 = never), `theme: system|light|dark`,
`coach_grid: Boolean = true`, `batch_requires_charging: Boolean = false`. Per-app locale selection is
owned and persisted by `AppCompatDelegate`; it is not duplicated in this DataStore.

## Android manifest facts

Permissions: `INTERNET`, `CAMERA` (coach only, requested at first coach use), `POST_NOTIFICATIONS`
(requested when the user turns on batch-done notifications). `android:allowBackup="true"` with
`dataExtractionRules` and `fullBackupContent`: cloud backup includes `files/backup/looks.json` and the
`settings` DataStore, excludes Room, sources, masks, outputs, scenes and the `wallet` DataStore; device
transfer adds Room, sources and outputs but excludes wallet ids, device/session tokens, recovery material
and purchase tokens. Wallet access moves only through explicit recovery. No `exported` component except
the launcher activity.

## Server SQLite (`server/db`, WAL, one writer)

```sql
CREATE TABLE device(id TEXT PRIMARY KEY, install_hash TEXT NOT NULL, created_at INTEGER NOT NULL,
  integrity_state TEXT NOT NULL, token_generation INTEGER NOT NULL DEFAULT 0,
  blocked INTEGER NOT NULL DEFAULT 0, deleted_at INTEGER);
CREATE TABLE wallet(id TEXT PRIMARY KEY, recovery_hash TEXT, balance INTEGER NOT NULL CHECK(balance >= 0),
  refund_debt INTEGER NOT NULL DEFAULT 0 CHECK(refund_debt >= 0), purchase_hold INTEGER NOT NULL DEFAULT 0,
  starter_granted INTEGER NOT NULL DEFAULT 0, created_at INTEGER NOT NULL, deleted_at INTEGER);
CREATE TABLE device_wallet(device_id TEXT PRIMARY KEY REFERENCES device(id), wallet_id TEXT NOT NULL REFERENCES wallet(id));
CREATE TABLE starter_grant(wallet_id TEXT PRIMARY KEY REFERENCES wallet(id), recall_bit INTEGER NOT NULL CHECK(recall_bit = 1),
  granted_at INTEGER NOT NULL);
CREATE TABLE ledger(id INTEGER PRIMARY KEY, wallet_id TEXT NOT NULL REFERENCES wallet(id),
  kind TEXT NOT NULL CHECK(kind IN ('STARTER','PURCHASE','RESERVE','SETTLE','RELEASE','REFUND_ADJUST')),
  delta INTEGER NOT NULL, ref TEXT NOT NULL, idempotency_key TEXT NOT NULL, created_at INTEGER NOT NULL,
  UNIQUE(wallet_id, kind, idempotency_key));
CREATE TABLE purchase_intent(id TEXT PRIMARY KEY, wallet_id TEXT NOT NULL, product_id TEXT NOT NULL,
  account_hash TEXT NOT NULL, profile_hash TEXT NOT NULL, expires_at INTEGER NOT NULL, used_at INTEGER);
CREATE TABLE purchase(token_hash TEXT PRIMARY KEY, product_id TEXT NOT NULL, order_id TEXT,
  wallet_id TEXT NOT NULL, package_name TEXT NOT NULL, purchase_state TEXT NOT NULL,
  token_ciphertext BLOB, token_wrapped_key BLOB,
  state TEXT NOT NULL CHECK(state IN ('RECEIVED','VERIFIED','FULFILLED','PLAY_FINALIZING','CONSUMED','ACKNOWLEDGED','VOIDED','INVALID')),
  quantity INTEGER NOT NULL DEFAULT 1, voided_quantity INTEGER NOT NULL DEFAULT 0,
  last_error TEXT, next_attempt_at INTEGER, finalized_at INTEGER, created_at INTEGER NOT NULL,
  UNIQUE(order_id, product_id));
CREATE TABLE job(id TEXT PRIMARY KEY, wallet_id TEXT NOT NULL, op TEXT NOT NULL,
  state TEXT NOT NULL CHECK(state IN ('QUEUED','RUNNING','SUCCEEDED','FAILED','CANCELLED')),
  reserved INTEGER NOT NULL, charged INTEGER NOT NULL DEFAULT 0, provider_endpoint TEXT,
  cost_micro_usd INTEGER, attempt INTEGER NOT NULL DEFAULT 0, fail_reason TEXT,
  client_request_id TEXT NOT NULL, created_at INTEGER NOT NULL, finished_at INTEGER,
  UNIQUE(wallet_id, client_request_id));
CREATE TABLE upload(id TEXT PRIMARY KEY, wallet_id TEXT NOT NULL REFERENCES wallet(id),
  path TEXT NOT NULL UNIQUE, sha256 TEXT NOT NULL, bytes INTEGER NOT NULL, created_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL, deleted_at INTEGER);
CREATE TABLE job_upload(job_id TEXT NOT NULL REFERENCES job(id), upload_id TEXT NOT NULL REFERENCES upload(id),
  PRIMARY KEY(job_id, upload_id));
CREATE TABLE result(id TEXT PRIMARY KEY, job_id TEXT NOT NULL UNIQUE REFERENCES job(id),
  wallet_id TEXT NOT NULL REFERENCES wallet(id), path TEXT NOT NULL UNIQUE, sha256 TEXT NOT NULL,
  bytes INTEGER NOT NULL, expires_at INTEGER NOT NULL, delivered_at INTEGER, deleted_at INTEGER);
CREATE TABLE delivery_token(token_hash TEXT PRIMARY KEY, result_id TEXT NOT NULL REFERENCES result(id),
  wallet_id TEXT NOT NULL REFERENCES wallet(id), expires_at INTEGER NOT NULL, used_at INTEGER);
CREATE TABLE delivery_attempt(id TEXT PRIMARY KEY, result_id TEXT NOT NULL REFERENCES result(id),
  wallet_id TEXT NOT NULL REFERENCES wallet(id), created_at INTEGER NOT NULL, expires_at INTEGER NOT NULL);
CREATE TABLE delivery_receipt(id INTEGER PRIMARY KEY, attempt_id TEXT NOT NULL UNIQUE REFERENCES delivery_attempt(id),
  result_id TEXT NOT NULL, wallet_id TEXT NOT NULL, bytes INTEGER NOT NULL, sha256 TEXT NOT NULL,
  accepted_at INTEGER NOT NULL, UNIQUE(result_id, wallet_id));
CREATE TABLE report(id INTEGER PRIMARY KEY, job_id TEXT, wallet_id TEXT, category TEXT NOT NULL,
  note TEXT, status TEXT NOT NULL CHECK(status IN ('OPEN','REVIEWED','ACTIONED','DISMISSED')),
  provider_endpoint TEXT, model_version TEXT, result_fingerprint TEXT, created_at INTEGER NOT NULL,
  reviewed_at INTEGER, action TEXT);
CREATE TABLE rtdn_message(message_id TEXT PRIMARY KEY, event_time INTEGER NOT NULL,
  notification_type TEXT NOT NULL, processed_at INTEGER NOT NULL);
CREATE TABLE play_reconcile(id INTEGER PRIMARY KEY CHECK(id = 1), page_token TEXT,
  overlap_from INTEGER NOT NULL, last_success_at INTEGER, last_error TEXT);
CREATE TABLE spend_day(day TEXT PRIMARY KEY, cost_micro_usd INTEGER NOT NULL, starter_cost_micro_usd INTEGER NOT NULL);
```

**Invariants** (each has a test in `server/test`): `wallet.balance` equals the sum of `ledger.delta`;
a single-result RESERVE has exactly one SETTLE or RELEASE; a series reservation equals the sum of its
per-variant SETTLE and RELEASE movements at terminal state; a `purchase.token_hash` fulfills once and its
ciphertext is erased only at terminal Play finalization; `job.charged` becomes non-zero only after an exact,
accepted delivery receipt; a result file is deleted ≤ 24 h after finish; no row stores image bytes or
a prompt that contains user text beyond the report `note` the user typed.

Retention: uploads ≤ 1 h; results ≤ 24 h; `ledger`, `purchase` and `spend_day` kept de-identified for
accounting after `DELETE /v1/data` (they carry no image reference); `report.note` deleted with the data.

## Wire/time conventions

All times are epoch milliseconds UTC; ids are UUIDv4 strings; money in server code is integer micro-USD;
credits are integers. Credit costs are server-authoritative (`GET /v1/config`).

## Persistence details

- Wallet DataStore additionally carries `tokenGeneration`; it and every wallet field are excluded from
  cloud backup and device transfer. Transfer of wallet access is represented only by the explicit recovery
  code flow; Room/sources/outputs may transfer, credentials and purchase tokens may not.
- `batch_free_limit=5` and `watermark_free_exports=true` are entitlement-derived observable values, not
  editable preferences. The locale tag is owned by `AppCompatDelegate` from an `AppCompatActivity`.
- Room additionally owns `purchase_outbox(id, productId, purchaseTokenCiphertext, state, attempt,
  nextAttemptAt, createdAt)` and durable job fields `workName`, `attempt`, `nextAttemptAt`, `terminalAt`.
  Raw tokens never enter logs/DataStore and are erased after terminal server confirmation.
- Required indexes: `item(projectId,state,sku)`, `job(state,nextAttemptAt,projectId)`,
  `output(itemId,createdAt)`, `project(updatedAt)`. Library/search DAOs expose Paging 3
  `PagingSource<Int, ProjectSummary>` and never load the whole catalogue.

Resource paths are resolved only through the ownership rows in the SQL schema above.

Deletion revokes tokens by incrementing `token_generation`, deletes resources/reports/recovery and all
device-wallet links, and tombstones the wallet. Ledger/purchase rows retained for accounting and replay
prevention lose wallet/device linkage through a non-reversible deletion surrogate; they cannot restore a
balance. The invariant suite proves no active resource or credential remains.
