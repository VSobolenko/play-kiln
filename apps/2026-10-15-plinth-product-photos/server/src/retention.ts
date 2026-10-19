import { rm } from 'node:fs/promises';
import type Database from 'better-sqlite3';
import type { CreditLedger } from './ledger.js';

export async function sweepExpiredResources(
  db: Database.Database,
  ledger?: CreditLedger,
  now = Date.now(),
): Promise<void> {
  const expiredUploads = db.prepare(
    'SELECT id, path FROM upload WHERE deleted_at IS NULL AND expires_at <= ?',
  ).all(now) as Array<{ id: string; path: string }>;
  for (const upload of expiredUploads) {
    await rm(upload.path, { force: true });
    db.prepare(`UPDATE upload SET path = ?, sha256 = '', bytes = 0, deleted_at = ?
      WHERE id = ?`).run(`expired:${upload.id}`, now, upload.id);
  }

  const expiredResults = db.prepare(`SELECT r.id AS result_id, r.job_id, r.wallet_id, r.path, j.charged
    FROM result r JOIN job j ON j.id = r.job_id WHERE r.expires_at <= ?`)
    .all(now) as Array<{ result_id: string; job_id: string; wallet_id: string; path: string; charged: number }>;
  for (const result of expiredResults) {
    // Expiry removes payload state, not delivery evidence: attempts and receipts outlive results.
    const receipt = db.prepare('SELECT 1 FROM delivery_receipt WHERE result_id = ?').get(result.result_id);
    if (!receipt && result.charged === 0 && ledger) {
      const released = ledger.release(result.wallet_id, result.job_id);
      if (released['state'] === 'RELEASED') {
        db.prepare(`UPDATE job SET fail_reason = 'RESULT_EXPIRED', state = 'FAILED', finished_at = ?
          WHERE id = ?`).run(now, result.job_id);
      }
    }
    await rm(result.path, { force: true });
    db.transaction(() => {
      db.prepare('DELETE FROM delivery_token WHERE result_id = ?').run(result.result_id);
      db.prepare('DELETE FROM result_metadata WHERE result_id = ?').run(result.result_id);
      // delivery_attempt and delivery_receipt deliberately remain as durable delivery history.
      db.prepare('DELETE FROM result WHERE id = ?').run(result.result_id);
    }).immediate();
  }
  db.prepare('DELETE FROM data_deletion_receipt WHERE expires_at <= ?').run(now);
  db.prepare('DELETE FROM integrity_challenge WHERE expires_at <= ?').run(now);
}
