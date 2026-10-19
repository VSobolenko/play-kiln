CREATE TABLE integrity_challenge(
  id TEXT PRIMARY KEY,
  challenge_hash TEXT NOT NULL,
  install_hash TEXT NOT NULL,
  app_version TEXT NOT NULL,
  locale TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL,
  consumed_at INTEGER
);
CREATE INDEX integrity_challenge_expiry ON integrity_challenge(expires_at);

ALTER TABLE data_deletion_receipt ADD COLUMN provider_status TEXT NOT NULL DEFAULT 'UNKNOWN';
ALTER TABLE data_deletion_receipt ADD COLUMN provider_retention_until INTEGER;
ALTER TABLE report ADD COLUMN operation_id TEXT;
ALTER TABLE purchase ADD COLUMN intent_id TEXT;

CREATE TABLE moderation_case(
  id TEXT PRIMARY KEY,
  result_fingerprint TEXT NOT NULL UNIQUE,
  report_count INTEGER NOT NULL DEFAULT 0,
  status TEXT NOT NULL CHECK(status IN ('OPEN','ESCALATED','REVIEWED','ACTIONED','DISMISSED')),
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);
CREATE TABLE moderation_case_report(
  case_id TEXT NOT NULL REFERENCES moderation_case(id),
  wallet_id TEXT NOT NULL,
  report_id INTEGER NOT NULL UNIQUE REFERENCES report(id),
  PRIMARY KEY(case_id, wallet_id)
);
