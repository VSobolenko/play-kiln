import type Database from 'better-sqlite3';
import { createDatabase } from '../src/db.js';

export function testDatabase(balance = 10): Database.Database {
  const db = createDatabase(':memory:');
  db.prepare(`INSERT INTO wallet (id, balance, created_at) VALUES (?, ?, ?)`)
    .run('wallet-1', balance, Date.now());
  if (balance > 0) {
    db.prepare(`INSERT INTO ledger (wallet_id, kind, delta, ref, idempotency_key, created_at)
      VALUES (?, 'STARTER', ?, 'test-seed', 'test-seed', ?)`)
      .run('wallet-1', balance, Date.now());
  }
  return db;
}

export function seedJob(db: Database.Database, id: string, walletId = 'wallet-1'): void {
  db.prepare(`INSERT INTO job
    (id, wallet_id, op, state, reserved, client_request_id, created_at)
    VALUES (?, ?, 'O-02', 'QUEUED', 0, ?, ?)`)
    .run(id, walletId, `client-${id}`, Date.now());
}

export function seedDeliverable(
  db: Database.Database,
  jobId: string,
  walletId = 'wallet-1',
  attemptId = 'attempt-1',
): void {
  const now = Date.now();
  db.prepare(`UPDATE job SET state = 'SUCCEEDED' WHERE id = ?`).run(jobId);
  db.prepare(`INSERT INTO result
    (id, job_id, wallet_id, path, sha256, bytes, expires_at)
    VALUES (?, ?, ?, ?, ?, ?, ?)`)
    .run(`result-${jobId}`, jobId, walletId, `/result/${jobId}`, 'a'.repeat(64), 7, now + 60_000);
  db.prepare(`INSERT INTO delivery_attempt (id, result_id, wallet_id, created_at, expires_at)
    VALUES (?, ?, ?, ?, ?)`)
    .run(attemptId, `result-${jobId}`, walletId, now, now + 60_000);
}
