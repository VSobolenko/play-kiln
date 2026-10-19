CREATE TABLE delivery_receipt_migration_backup AS
  SELECT id, attempt_id, result_id, wallet_id, bytes, sha256, accepted_at FROM delivery_receipt;
DELETE FROM delivery_receipt;

CREATE TABLE delivery_attempt_migration_new(
  id TEXT PRIMARY KEY,
  result_id TEXT NOT NULL,
  wallet_id TEXT NOT NULL REFERENCES wallet(id),
  created_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL
);
INSERT INTO delivery_attempt_migration_new(id, result_id, wallet_id, created_at, expires_at)
  SELECT id, result_id, wallet_id, created_at, expires_at FROM delivery_attempt;
DROP TABLE delivery_attempt;
ALTER TABLE delivery_attempt_migration_new RENAME TO delivery_attempt;

INSERT INTO delivery_receipt(id, attempt_id, result_id, wallet_id, bytes, sha256, accepted_at)
  SELECT id, attempt_id, result_id, wallet_id, bytes, sha256, accepted_at
  FROM delivery_receipt_migration_backup;
DROP TABLE delivery_receipt_migration_backup;
