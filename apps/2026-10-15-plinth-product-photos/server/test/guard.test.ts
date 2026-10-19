import { afterEach, describe, expect, it, vi } from 'vitest';
import { createHash, randomUUID } from 'node:crypto';
import { rm } from 'node:fs/promises';
import sharp from 'sharp';
import { join } from 'node:path';
import { CreditLedger } from '../src/ledger.js';
import { JobQueue } from '../src/queue.js';
import { MODEL_REGISTRY, type ProviderAdapter, type ProviderResult } from '../src/registry.js';
import { testDatabase } from './helpers.js';

describe('fidelity guard ledger contract', () => {
  let db: ReturnType<typeof testDatabase> | undefined;
  let dataDir: string | undefined;

  afterEach(async () => {
    db?.close();
    db = undefined;
    if (dataDir) await rm(dataDir, { recursive: true, force: true });
    dataDir = undefined;
  });

  it('rejects a drifting result after one retry and releases the reservation', async () => {
    db = testDatabase(5);
    dataDir = join(process.cwd(), '.kiln', 'scratch', 'plinth-p-refund', randomUUID());
    const ledger = new CreditLedger(db);
    const result = guardedResult(false);
    const run = vi.fn<ProviderAdapter['run']>(async () => result);
    const queue = new JobQueue(db, ledger, { run }, 1, dataDir, { dailySpendCapMicroUsd: 36_000 });

    await queue.enqueue(job('job-drift'));
    await queue.waitForIdle();

    expect(run).toHaveBeenCalledTimes(2);
    expect(run.mock.calls[0]?.[2]['seed']).toBeTypeOf('number');
    expect(run.mock.calls[0]?.[2]).not.toEqual(run.mock.calls[1]?.[2]);
    expect(db.prepare('SELECT cost_micro_usd FROM spend_day').get())
      .toEqual({ cost_micro_usd: 36_000 });
    expect(db.prepare('SELECT state, fail_reason, charged FROM job WHERE id = ?').get('job-drift'))
      .toEqual({ state: 'FAILED', fail_reason: 'GUARD_REJECTED', charged: 0 });
    expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-1')).toEqual({ balance: 5 });
    expect(db.prepare(`SELECT count(*) AS n FROM ledger WHERE kind = 'RELEASE' AND idempotency_key = ?`)
      .get('job-drift')).toEqual({ n: 1 });
  });

  it('does not retry a rejected result when the retry would exceed the daily spend cap', async () => {
    db = testDatabase(5);
    dataDir = join(process.cwd(), '.kiln', 'scratch', 'plinth-p-refund', randomUUID());
    const run = vi.fn<ProviderAdapter['run']>(async () => guardedResult(false));
    const queue = new JobQueue(db, new CreditLedger(db), { run }, 1, dataDir, {
      dailySpendCapMicroUsd: MODEL_REGISTRY['O-02'].costMicroUsd ?? 0,
    });

    await queue.enqueue(job('job-guard-cap'));
    await queue.waitForIdle();

    expect(run).toHaveBeenCalledTimes(1);
    expect(db.prepare('SELECT cost_micro_usd FROM spend_day').get())
      .toEqual({ cost_micro_usd: MODEL_REGISTRY['O-02'].costMicroUsd });
    expect(db.prepare('SELECT state, fail_reason, charged FROM job WHERE id = ?').get('job-guard-cap'))
      .toEqual({ state: 'FAILED', fail_reason: 'CAP_REACHED', charged: 0 });
    expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-1')).toEqual({ balance: 5 });
    expect(db.prepare(`SELECT count(*) AS n FROM ledger WHERE kind = 'RELEASE' AND idempotency_key = ?`)
      .get('job-guard-cap')).toEqual({ n: 1 });
  });

  it('maps a provider moderation block without retrying and releases the reservation', async () => {
    db = testDatabase(5);
    dataDir = join(process.cwd(), '.kiln', 'scratch', 'plinth-p-refund', randomUUID());
    const run = vi.fn<ProviderAdapter['run']>(async () => {
      throw new Error('MODERATION_BLOCKED');
    });
    const queue = new JobQueue(db, new CreditLedger(db), { run }, 1, dataDir);

    await queue.enqueue(job('job-moderation'));
    await queue.waitForIdle();

    expect(run).toHaveBeenCalledTimes(1);
    expect(db.prepare('SELECT state, fail_reason, charged FROM job WHERE id = ?').get('job-moderation'))
      .toEqual({ state: 'FAILED', fail_reason: 'MODERATION_BLOCKED', charged: 0 });
    expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-1')).toEqual({ balance: 5 });
    expect(db.prepare(`SELECT count(*) AS n FROM ledger WHERE kind = 'RELEASE' AND idempotency_key = ?`)
      .get('job-moderation')).toEqual({ n: 1 });
  });

  it('delivers a faithful result and settles its verified receipt only once', async () => {
    db = testDatabase(5);
    dataDir = join(process.cwd(), '.kiln', 'scratch', 'plinth-p-refund', randomUUID());
    const ledger = new CreditLedger(db);
    const output = guardedResult(true);
    const queue = new JobQueue(db, ledger, { run: async () => output } as ProviderAdapter, 1, dataDir);
    await queue.enqueue(job('job-faithful'));
    await queue.waitForIdle();
    const result = db.prepare('SELECT id, bytes, sha256, expires_at FROM result WHERE job_id = ?')
      .get('job-faithful') as { id: string; bytes: number; sha256: string; expires_at: number };
    db.prepare(`INSERT INTO delivery_attempt(id, result_id, wallet_id, created_at, expires_at)
      VALUES ('attempt-1', ?, 'wallet-1', ?, ?)`).run(result.id, Date.now(), result.expires_at);
    const receipt = { deliveryAttemptId: 'attempt-1', bytes: result.bytes, sha256: result.sha256 };

    expect(guardedResult(true).guard?.passed).toBe(true);
    expect(ledger.settle('wallet-1', 'job-faithful', receipt)).toEqual({ charged: 2, state: 'SETTLED' });
    expect(ledger.settle('wallet-1', 'job-faithful', receipt)).toEqual({ charged: 2, state: 'SETTLED' });
    expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-1')).toEqual({ balance: 3 });
    expect(db.prepare(`SELECT count(*) AS n FROM ledger WHERE kind = 'SETTLE' AND idempotency_key = ?`)
      .get('job-faithful')).toEqual({ n: 1 });
  });
});

function job(jobId: string) {
  return {
    jobId,
    walletId: 'wallet-1',
    op: 'O-02',
    clientRequestId: `client-${jobId}`,
    imageBase64: Buffer.from('source').toString('base64'),
    uploadIds: [],
    params: {},
    credits: 2,
  };
}

const resultPng = await sharp({ create: { width: 1024, height: 1024, channels: 3, background: '#808080' } })
  .png().toBuffer();

function guardedResult(passed: boolean): ProviderResult {
  const bytes = resultPng;
  return {
    bytes,
    sha256: createHash('sha256').update(bytes).digest('hex'),
    provenance: 'ORIGINAL_PIXELS',
    width: 1,
    height: 1,
    format: 'image/png',
    guard: { passed, metrics: { dE: passed ? 0 : 12, ssimGrad: passed ? 1 : 0.2, iou: passed ? 1 : 0.2 } },
  };
}
