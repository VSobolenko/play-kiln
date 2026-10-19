import { afterEach, describe, expect, it } from 'vitest';
import { CreditLedger } from '../src/ledger.js';
import { seedDeliverable, seedJob, testDatabase } from './helpers.js';

const openDatabases: ReturnType<typeof testDatabase>[] = [];
afterEach(() => {
  for (const db of openDatabases.splice(0)) db.close();
});

function database(balance = 10) {
  const db = testDatabase(balance);
  openDatabases.push(db);
  return db;
}

describe('credit ledger', () => {
  it('refuses an insufficient reserve without changing the balance', () => {
    const db = database(3);
    seedJob(db, 'job-short');
    const ledger = new CreditLedger(db);

    expect(ledger.reserve('wallet-1', 'job-short', 5)).toEqual({
      accepted: false,
      balance: 3,
      reserved: 0,
    });
    expect(db.prepare(`SELECT count(*) AS n FROM ledger WHERE kind = 'RESERVE'`).get()).toEqual({ n: 0 });
  });

  it('serializes concurrent reserves so they cannot overspend', async () => {
    const db = database(4);
    seedJob(db, 'job-a');
    seedJob(db, 'job-b');
    const ledger = new CreditLedger(db);

    const results = await Promise.all([
      Promise.resolve().then(() => ledger.reserve('wallet-1', 'job-a', 3)),
      Promise.resolve().then(() => ledger.reserve('wallet-1', 'job-b', 3)),
    ]);

    expect(results.map((result) => result['accepted']).sort()).toEqual([false, true]);
    expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-1')).toEqual({ balance: 1 });
  });

  it('settles only an exact delivery receipt and safely replays a lost response', () => {
    const db = database(10);
    seedJob(db, 'job-receipt');
    const ledger = new CreditLedger(db);
    ledger.reserve('wallet-1', 'job-receipt', 2);
    seedDeliverable(db, 'job-receipt');

    expect(() => ledger.settle('wallet-1', 'job-receipt', {
      deliveryAttemptId: 'missing-attempt', bytes: 7, sha256: 'a'.repeat(64),
    })).toThrow('DELIVERY_RECEIPT_MISMATCH');
    expect(() => ledger.settle('wallet-1', 'job-receipt', {
      deliveryAttemptId: 'attempt-1', bytes: 8, sha256: 'a'.repeat(64),
    })).toThrow('DELIVERY_RECEIPT_MISMATCH');
    expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-1')).toEqual({ balance: 8 });
    expect(db.prepare(`SELECT count(*) AS n FROM ledger WHERE kind = 'SETTLE'`).get()).toEqual({ n: 0 });

    const accepted = { deliveryAttemptId: 'attempt-1', bytes: 7, sha256: 'a'.repeat(64) };
    expect(ledger.settle('wallet-1', 'job-receipt', accepted)).toEqual({ charged: 2, state: 'SETTLED' });
    expect(ledger.settle('wallet-1', 'job-receipt', accepted)).toEqual({ charged: 2, state: 'SETTLED' });
    expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-1')).toEqual({ balance: 8 });
    expect(db.prepare(`SELECT count(*) AS n FROM ledger WHERE kind = 'SETTLE'`).get()).toEqual({ n: 1 });
  });

  it('does not accept one job delivery receipt as proof for another job', () => {
    const db = database(10);
    seedJob(db, 'job-a');
    seedJob(db, 'job-b');
    const ledger = new CreditLedger(db);
    ledger.reserve('wallet-1', 'job-a', 2);
    ledger.reserve('wallet-1', 'job-b', 3);
    seedDeliverable(db, 'job-a', 'wallet-1', 'attempt-a');
    seedDeliverable(db, 'job-b', 'wallet-1', 'attempt-b');

    const receiptA = { deliveryAttemptId: 'attempt-a', bytes: 7, sha256: 'a'.repeat(64) };
    expect(ledger.settle('wallet-1', 'job-a', receiptA)).toEqual({ charged: 2, state: 'SETTLED' });
    expect(() => ledger.settle('wallet-1', 'job-b', receiptA)).toThrow('DELIVERY_RECEIPT_MISMATCH');
    expect(ledger.settle('wallet-1', 'job-b', {
      deliveryAttemptId: 'attempt-b', bytes: 7, sha256: 'a'.repeat(64),
    })).toEqual({ charged: 3, state: 'SETTLED' });
  });

  it('releases a failed job reservation idempotently', () => {
    const db = database(10);
    seedJob(db, 'job-failed');
    const ledger = new CreditLedger(db);
    ledger.reserve('wallet-1', 'job-failed', 4);

    expect(ledger.release('wallet-1', 'job-failed')).toEqual({ released: 4, state: 'RELEASED' });
    expect(ledger.release('wallet-1', 'job-failed')).toEqual({ released: 4, state: 'RELEASED' });
    expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-1')).toEqual({ balance: 10 });
    expect(db.prepare(`SELECT count(*) AS n FROM ledger WHERE kind = 'RELEASE'`).get()).toEqual({ n: 1 });
  });
});
