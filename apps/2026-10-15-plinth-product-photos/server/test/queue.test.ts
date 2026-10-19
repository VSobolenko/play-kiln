import { afterEach, describe, expect, it, vi } from 'vitest';
import { createHash, randomUUID } from 'node:crypto';
import { readFile, rm } from 'node:fs/promises';
import sharp from 'sharp';
import { join } from 'node:path';
import { CreditLedger } from '../src/ledger.js';
import { JobQueue } from '../src/queue.js';
import { createFakeProvider, fakeProvider } from '../src/registry.js';
import { testDatabase } from './helpers.js';

describe('job queue and provider seam', () => {
  let db: ReturnType<typeof testDatabase> | undefined;
  afterEach(() => {
    db?.close();
    db = undefined;
  });

  it('refuses insufficient balance before calling the provider', async () => {
    db = testDatabase(0);
    const provider = { run: vi.fn(fakeProvider.run) };
    const queue = new JobQueue(db, new CreditLedger(db), provider, 1);

    await expect(queue.enqueue({
      jobId: 'job-insufficient', walletId: 'wallet-1', op: 'O-02',
      clientRequestId: 'client-job-insufficient', imageBase64: Buffer.from('source').toString('base64'),
      uploadIds: [], params: {}, credits: 1,
    })).rejects.toMatchObject({ code: 'INSUFFICIENT_CREDITS' });
    expect(provider.run).not.toHaveBeenCalled();
  });

  it('runs the fake provider with a deterministic result and declared provenance', async () => {
    const output = await fakeProvider.run('O-02', { image: Buffer.from('fixture') }, {});

    expect(output).toMatchObject({
      bytes: expect.any(Buffer),
      provenance: 'ORIGINAL_PIXELS',
      sha256: expect.stringMatching(/^[a-f0-9]{64}$/),
    });
  });

  it('allows failure injection on the fake provider', async () => {
    const provider = createFakeProvider({ failure: true });
    await expect(provider.run('O-02', { image: Buffer.from('fixture') }, {}))
      .rejects.toThrow('FAKE_PROVIDER_FAILURE');
  });

  it('releases a reserved amount when the provider fails', async () => {
    db = testDatabase(5);
    const queue = new JobQueue(
      db,
      new CreditLedger(db),
      { run: async () => { throw new Error('provider failure'); } },
      1,
    );

    await queue.enqueue({
      jobId: 'job-provider-failure', walletId: 'wallet-1', op: 'O-02',
      clientRequestId: 'client-job-provider-failure', imageBase64: Buffer.from('source').toString('base64'),
      uploadIds: [], params: {}, credits: 2,
    });
    await queue.waitForIdle();

    expect(db.prepare('SELECT state, fail_reason, finished_at FROM job WHERE id = ?').get('job-provider-failure'))
      .toMatchObject({ state: 'FAILED', fail_reason: 'PROVIDER_FAILED', finished_at: expect.any(Number) });
    expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-1')).toEqual({ balance: 5 });
    expect(db.prepare(`SELECT count(*) AS n FROM ledger WHERE kind = 'RELEASE'`).get()).toEqual({ n: 1 });
  });

  it('terminally records a cap rejection so a duplicate request cannot return a queued orphan', async () => {
    db = testDatabase(5);
    const queue = new JobQueue(db, new CreditLedger(db), fakeProvider, 1, 'data', { dailySpendCapMicroUsd: 0 });
    const work = {
      jobId: 'job-cap-rejected', walletId: 'wallet-1', op: 'O-02' as const,
      clientRequestId: 'client-job-cap-rejected', imageBase64: Buffer.from('source').toString('base64'),
      uploadIds: [], params: {}, credits: 1,
    };

    await expect(queue.enqueue(work)).rejects.toMatchObject({ code: 'CAP_REACHED' });
    expect(db.prepare('SELECT state, fail_reason, finished_at FROM job WHERE id = ?').get(work.jobId))
      .toMatchObject({ state: 'FAILED', fail_reason: 'CAP_REACHED', finished_at: expect.any(Number) });
    await expect(queue.enqueue({ ...work, jobId: 'unused-retry-id' }))
      .resolves.toEqual({ jobId: work.jobId, reserved: 1, state: 'FAILED' });
    expect(db.prepare(`SELECT count(*) AS n FROM ledger WHERE kind = 'RELEASE' AND idempotency_key = ?`)
      .get(work.jobId)).toEqual({ n: 1 });
  });

  it('stores the normalised result and hashes those bytes, not the provider original', async () => {
    db = testDatabase(5);
    const dataDir = join(process.cwd(), '.kiln', 'scratch', randomUUID());
    const original = await sharp({ create: { width: 224, height: 224, channels: 3, background: '#808080' } })
      .png().toBuffer();
    const provider = {
      run: async () => ({
        bytes: original, sha256: createHash('sha256').update(original).digest('hex'),
        provenance: 'ENHANCED' as const, width: 224, height: 224, format: 'image/png',
      }),
    };
    const queue = new JobQueue(db, new CreditLedger(db), provider, 1, dataDir);
    await queue.enqueue({
      jobId: 'job-normalised', walletId: 'wallet-1', op: 'O-08',
      clientRequestId: 'client-job-normalised', imageBase64: Buffer.from('source').toString('base64'),
      uploadIds: [], params: {}, credits: 2,
    });
    await queue.waitForIdle();
    try {
      const row = db.prepare('SELECT path, sha256, bytes FROM result WHERE job_id = ?')
        .get('job-normalised') as { path: string; sha256: string; bytes: number };
      const stored = await readFile(row.path);
      const metadata = await sharp(stored).metadata();
      expect(Math.max(metadata.width ?? 0, metadata.height ?? 0)).toBe(1024);
      expect(row.sha256).toBe(createHash('sha256').update(stored).digest('hex'));
      expect(row.sha256).not.toBe(createHash('sha256').update(original).digest('hex'));
      expect(row.bytes).toBe(stored.length);
      expect(db.prepare('SELECT width, height FROM result_metadata WHERE result_id = (SELECT id FROM result WHERE job_id = ?)')
        .get('job-normalised')).toEqual({ width: metadata.width, height: metadata.height });
    } finally {
      await rm(dataDir, { recursive: true, force: true });
    }
  });

  it('fails the job with RESULT_UNUSABLE and releases credits when the result is not an image', async () => {
    db = testDatabase(5);
    const dataDir = join(process.cwd(), '.kiln', 'scratch', randomUUID());
    const queue = new JobQueue(db, new CreditLedger(db), fakeProvider, 1, dataDir);
    await queue.enqueue({
      jobId: 'job-unusable', walletId: 'wallet-1', op: 'O-08',
      clientRequestId: 'client-job-unusable', imageBase64: Buffer.from('source').toString('base64'),
      uploadIds: [], params: {}, credits: 2,
    });
    await queue.waitForIdle();
    try {
      expect(db.prepare('SELECT fail_reason FROM job WHERE id = ?').get('job-unusable'))
        .toEqual({ fail_reason: 'RESULT_UNUSABLE' });
      expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-1')).toEqual({ balance: 5 });
      expect(db.prepare('SELECT count(*) AS n FROM result WHERE job_id = ?').get('job-unusable')).toEqual({ n: 0 });
    } finally {
      await rm(dataDir, { recursive: true, force: true });
    }
  });

  it('releases an undelivered result after its retention expires', async () => {
    db = testDatabase(5);
    const dataDir = join(process.cwd(), '.kiln', 'scratch', randomUUID());
    const png = await sharp({ create: { width: 1024, height: 1024, channels: 3, background: '#808080' } })
      .png().toBuffer();
    const imageProvider = {
      run: async () => ({
        bytes: png, sha256: createHash('sha256').update(png).digest('hex'),
        provenance: 'ENHANCED' as const, width: 1024, height: 1024, format: 'image/png',
      }),
    };
    const queue = new JobQueue(db, new CreditLedger(db), imageProvider, 1, dataDir);
    await queue.enqueue({
      jobId: 'job-expired-result', walletId: 'wallet-1', op: 'O-02',
      clientRequestId: 'client-job-expired-result', imageBase64: Buffer.from('source').toString('base64'),
      uploadIds: [], params: {}, credits: 2,
    });
    await queue.waitForIdle();
    db.prepare('UPDATE result SET expires_at = ? WHERE job_id = ?').run(Date.now() - 1, 'job-expired-result');

    try {
      await queue.sweepExpired();
      expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-1')).toEqual({ balance: 5 });
      expect(db.prepare('SELECT count(*) AS n FROM result WHERE job_id = ?').get('job-expired-result'))
        .toEqual({ n: 0 });
      expect(db.prepare(`SELECT count(*) AS n FROM ledger WHERE kind = 'RELEASE'`).get()).toEqual({ n: 1 });
    } finally {
      await rm(dataDir, { recursive: true, force: true });
    }
  });
});
