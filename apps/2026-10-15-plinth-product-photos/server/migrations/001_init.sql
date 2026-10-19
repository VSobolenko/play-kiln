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
CREATE TABLE delivery_attempt(id TEXT PRIMARY KEY, result_id TEXT NOT NULL,
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
CREATE TABLE data_deletion_receipt(token_hash TEXT PRIMARY KEY, status TEXT NOT NULL,
  controlled_systems_complete INTEGER NOT NULL, completed_at INTEGER NOT NULL, expires_at INTEGER NOT NULL);
CREATE TABLE result_metadata(result_id TEXT PRIMARY KEY REFERENCES result(id), width INTEGER NOT NULL,
  height INTEGER NOT NULL, format TEXT NOT NULL, provenance TEXT NOT NULL, guard_json TEXT);

CREATE INDEX ledger_wallet_created ON ledger(wallet_id, created_at DESC);
CREATE INDEX jobs_wallet_created ON job(wallet_id, created_at DESC);
CREATE INDEX jobs_state_created ON job(state, created_at);
CREATE INDEX uploads_wallet_expires ON upload(wallet_id, expires_at);
