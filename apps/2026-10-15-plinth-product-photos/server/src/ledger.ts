import type Database from 'better-sqlite3';
import apiSchema from '../schema/api.schema.json' with { type: 'json' };
import { Ajv } from 'ajv';

const ajv = new Ajv();
const validateReceipt = ajv.compile(apiSchema.definitions.DeliveryReceipt);

function objectRecord(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error('DELIVERY_RECEIPT_MISMATCH');
  }
  return value as Record<string, unknown>;
}

export class CreditLedger {
  constructor(private readonly db: Database.Database) {}

  reserve(walletId: string, jobId: string, credits: number): Record<string, unknown> {
    if (!Number.isSafeInteger(credits) || credits < 1) throw new Error('INVALID_CREDIT_AMOUNT');
    return this.db.transaction(() => {
      const prior = this.db.prepare(
        `SELECT ref, delta FROM ledger WHERE wallet_id = ? AND kind = 'RESERVE' AND idempotency_key = ?`,
      ).get(walletId, jobId) as { ref: string; delta: number } | undefined;
      if (prior) {
        if (prior.ref !== jobId || prior.delta !== -credits) throw new Error('IDEMPOTENCY_CONFLICT');
        return this.reservationResult(walletId, true);
      }

      const job = this.db.prepare('SELECT wallet_id FROM job WHERE id = ?').get(jobId) as
        { wallet_id: string } | undefined;
      if (!job || job.wallet_id !== walletId) throw new Error('JOB_NOT_FOUND');

      const wallet = this.db.prepare(
        'SELECT balance, refund_debt, purchase_hold, deleted_at FROM wallet WHERE id = ?',
      ).get(walletId) as {
        balance: number; refund_debt: number; purchase_hold: number; deleted_at: number | null;
      } | undefined;
      if (!wallet || wallet.deleted_at !== null) throw new Error('WALLET_NOT_FOUND');
      if (wallet.refund_debt > 0 || wallet.purchase_hold > 0 || wallet.balance < credits) {
        return this.reservationResult(walletId, false);
      }

      const now = Date.now();
      this.db.prepare('UPDATE wallet SET balance = balance - ? WHERE id = ?').run(credits, walletId);
      this.db.prepare(`INSERT INTO ledger(wallet_id, kind, delta, ref, idempotency_key, created_at)
        VALUES (?, 'RESERVE', ?, ?, ?, ?)`).run(walletId, -credits, jobId, jobId, now);
      this.db.prepare('UPDATE job SET reserved = ? WHERE id = ?').run(credits, jobId);
      return this.reservationResult(walletId, true);
    }).immediate();
  }

  settle(walletId: string, jobId: string, receiptValue: unknown): Record<string, unknown> {
    const receipt = objectRecord(receiptValue);
    if (!validateReceipt(receipt)) throw new Error('DELIVERY_RECEIPT_MISMATCH');
    const attemptId = receipt.deliveryAttemptId as string;
    const bytes = receipt.bytes as number;
    const sha256 = receipt.sha256 as string;

    return this.db.transaction(() => {
      const stored = this.db.prepare(
        `SELECT dr.bytes, dr.sha256, r.job_id
           FROM delivery_receipt dr
           JOIN result r ON r.id = dr.result_id AND r.wallet_id = dr.wallet_id
          WHERE dr.attempt_id = ? AND dr.wallet_id = ?`,
      ).get(attemptId, walletId) as { bytes: number; sha256: string; job_id: string } | undefined;
      if (stored) {
        // A receipt proves delivery of one result. Without this check a valid
        // receipt for a cheap job could be replayed against a different,
        // undelivered job in the same wallet and report it as settled.
        if (stored.job_id !== jobId || stored.bytes !== bytes || stored.sha256 !== sha256) {
          throw new Error('DELIVERY_RECEIPT_MISMATCH');
        }
        const job = this.db.prepare('SELECT charged FROM job WHERE id = ? AND wallet_id = ?')
          .get(jobId, walletId) as { charged: number } | undefined;
        if (!job) throw new Error('JOB_NOT_FOUND');
        return { charged: job.charged, state: 'SETTLED' as const };
      }

      const expected = this.db.prepare(`SELECT r.id AS result_id, r.bytes, r.sha256, r.expires_at,
          a.id AS attempt_id, a.expires_at AS attempt_expires_at, j.reserved, j.state
        FROM job j
        JOIN result r ON r.job_id = j.id AND r.wallet_id = j.wallet_id
        JOIN delivery_attempt a ON a.result_id = r.id AND a.wallet_id = r.wallet_id
        WHERE j.id = ? AND j.wallet_id = ? AND a.id = ?`)
        .get(jobId, walletId, attemptId) as {
          result_id: string; bytes: number; sha256: string; expires_at: number;
          attempt_id: string; attempt_expires_at: number; reserved: number; state: string;
        } | undefined;
      const now = Date.now();
      if (!expected || expected.state !== 'SUCCEEDED' ||
          expected.bytes !== bytes || expected.sha256 !== sha256 ||
          expected.expires_at < now || expected.attempt_expires_at < now) {
        throw new Error('DELIVERY_RECEIPT_MISMATCH');
      }

      const competing = this.db.prepare(
        'SELECT attempt_id, bytes, sha256 FROM delivery_receipt WHERE result_id = ? AND wallet_id = ?',
      ).get(expected.result_id, walletId) as { attempt_id: string; bytes: number; sha256: string } | undefined;
      if (competing) {
        if (competing.bytes !== bytes || competing.sha256 !== sha256) {
          throw new Error('DELIVERY_RECEIPT_MISMATCH');
        }
        const charged = this.db.prepare('SELECT charged FROM job WHERE id = ?').get(jobId) as
          { charged: number } | undefined;
        return { charged: charged?.charged ?? 0, state: 'SETTLED' as const };
      }

      this.chargeDelivery(walletId, jobId, now);
      this.db.prepare(`INSERT INTO delivery_receipt
        (attempt_id, result_id, wallet_id, bytes, sha256, accepted_at)
        VALUES (?, ?, ?, ?, ?, ?)`).run(attemptId, expected.result_id, walletId, bytes, sha256, now);
      this.db.prepare('UPDATE result SET delivered_at = ? WHERE id = ?').run(now, expected.result_id);
      this.db.prepare('UPDATE delivery_token SET used_at = ? WHERE result_id = ? AND wallet_id = ?')
        .run(now, expected.result_id, walletId);
      return { charged: expected.reserved, state: 'SETTLED' as const };
    }).immediate();
  }

  /** Charge point: called once result bytes were completely sent. Idempotent; never refunds. */
  deliver(walletId: string, jobId: string): Record<string, unknown> {
    return this.db.transaction(() => {
      const job = this.db.prepare('SELECT state FROM job WHERE id = ? AND wallet_id = ?')
        .get(jobId, walletId) as { state: string } | undefined;
      if (!job) throw new Error('JOB_NOT_FOUND');
      if (job.state !== 'SUCCEEDED') throw new Error('RESERVATION_NOT_ACTIVE');
      this.chargeDelivery(walletId, jobId, Date.now());
      const after = this.db.prepare('SELECT charged FROM job WHERE id = ?').get(jobId) as { charged: number };
      return { charged: after.charged, state: 'SETTLED' as const };
    }).immediate();
  }

  private chargeDelivery(walletId: string, jobId: string, now: number): void {
    if (this.db.prepare(
      `SELECT 1 FROM ledger WHERE wallet_id = ? AND kind = 'SETTLE' AND idempotency_key = ?`,
    ).get(walletId, jobId)) return;
    const reservation = this.db.prepare(
      `SELECT delta FROM ledger WHERE wallet_id = ? AND kind = 'RESERVE' AND idempotency_key = ?`,
    ).get(walletId, jobId) as { delta: number } | undefined;
    if (!reservation || this.db.prepare(
      `SELECT 1 FROM ledger WHERE wallet_id = ? AND kind = 'RELEASE' AND idempotency_key = ?`,
    ).get(walletId, jobId)) throw new Error('RESERVATION_NOT_ACTIVE');
    this.db.prepare(`INSERT INTO ledger(wallet_id, kind, delta, ref, idempotency_key, created_at)
      VALUES (?, 'SETTLE', 0, ?, ?, ?)`).run(walletId, jobId, jobId, now);
    this.db.prepare('UPDATE job SET charged = reserved, finished_at = ? WHERE id = ?').run(now, jobId);
  }

  release(walletId: string, jobId: string, failReason = 'PROVIDER_FAILED'): Record<string, unknown> {
    return this.db.transaction(() => {
      const releasedBefore = this.db.prepare(
        `SELECT delta FROM ledger WHERE wallet_id = ? AND kind = 'RELEASE' AND idempotency_key = ?`,
      ).get(walletId, jobId) as { delta: number } | undefined;
      if (releasedBefore) return { released: releasedBefore.delta, state: 'RELEASED' as const };
      if (this.db.prepare(
        `SELECT 1 FROM ledger WHERE wallet_id = ? AND kind = 'SETTLE' AND idempotency_key = ?`,
      ).get(walletId, jobId)) return { released: 0, state: 'SETTLED' as const };

      const reservation = this.db.prepare(
        `SELECT delta FROM ledger WHERE wallet_id = ? AND kind = 'RESERVE' AND idempotency_key = ?`,
      ).get(walletId, jobId) as { delta: number } | undefined;
      if (!reservation) return { released: 0, state: 'NONE' as const };
      const amount = -reservation.delta;
      const now = Date.now();
      this.db.prepare('UPDATE wallet SET balance = balance + ? WHERE id = ?').run(amount, walletId);
      this.db.prepare(`INSERT INTO ledger(wallet_id, kind, delta, ref, idempotency_key, created_at)
        VALUES (?, 'RELEASE', ?, ?, ?, ?)`).run(walletId, amount, jobId, jobId, now);
      this.db.prepare(`UPDATE job SET state = 'FAILED', fail_reason = ?,
        finished_at = ? WHERE id = ? AND wallet_id = ?`).run(failReason, now, jobId, walletId);
      return { released: amount, state: 'RELEASED' as const };
    }).immediate();
  }

  private reservationResult(walletId: string, accepted: boolean): Record<string, unknown> {
    const wallet = this.db.prepare('SELECT balance FROM wallet WHERE id = ?').get(walletId) as
      { balance: number } | undefined;
    const reserved = this.db.prepare(
      `SELECT COALESCE(SUM(reserved - charged), 0) AS total
       FROM job WHERE wallet_id = ? AND state IN ('QUEUED', 'RUNNING', 'SUCCEEDED')`,
    ).get(walletId) as { total: number };
    return { accepted, balance: wallet?.balance ?? 0, reserved: reserved.total };
  }
}
