import { createHash, randomUUID } from 'node:crypto';
import { EventEmitter } from 'node:events';
import { mkdir, rm, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import sharp from 'sharp';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { createApp } from '../src/app.js';
import { loadConfig } from '../src/config.js';
import { onCompletedSend } from '../src/delivery.js';
import { CreditLedger } from '../src/ledger.js';
import { JobQueue } from '../src/queue.js';
import type { ProviderAdapter } from '../src/registry.js';
import { sweepExpiredResources } from '../src/retention.js';
import { seedJob, testDatabase } from './helpers.js';

const sha = (b: Buffer) => createHash('sha256').update(b).digest('hex');
const RESERVED = 3;
const TOKEN = 'w121-result-token';

describe('W121-B fetch is delivery', () => {
  let root: string;
  let db: ReturnType<typeof testDatabase>;
  let ledger: CreditLedger;
  let app: Awaited<ReturnType<typeof createApp>>;
  let bytes: Buffer;
  let headers: Record<string, string>;

  beforeEach(async () => {
    root = join(process.cwd(), '.kiln', 'scratch', `w121-${randomUUID()}`);
    await mkdir(root, { recursive: true });
    db = testDatabase(10);
    ledger = new CreditLedger(db);
    bytes = await sharp({ create: { width: 4, height: 4, channels: 3, background: '#336699' } }).png().toBuffer();
    app = await createApp({
      db,
      config: loadConfig({ JWT_SECRET: 'x'.repeat(32), DATA_DIR: root, KILL_SWITCH_FILE: join(root, 'kill') }),
    });
    const now = Date.now();
    db.prepare(`INSERT INTO device(id,install_hash,created_at,integrity_state)
      VALUES('w121-device','w121-install',?,'PLAY_RECOGNIZED')`).run(now);
    db.prepare("INSERT INTO device_wallet(device_id,wallet_id) VALUES('w121-device','wallet-1')").run();
    headers = { authorization: `Bearer ${app.jwt.sign({ sub: 'w121-device', wid: 'wallet-1', gen: 0 })}` };
    seedJob(db, 'job-1');
    expect(ledger.reserve('wallet-1', 'job-1', RESERVED)).toMatchObject({ accepted: true, balance: 10 - RESERVED });
    const path = join(root, 'result-1');
    await writeFile(path, bytes);
    db.prepare(`UPDATE job SET state = 'SUCCEEDED' WHERE id = 'job-1'`).run();
    db.prepare(`INSERT INTO result(id,job_id,wallet_id,path,sha256,bytes,expires_at)
      VALUES('result-1','job-1','wallet-1',?,?,?,?)`).run(path, sha(bytes), bytes.length, now + 60_000);
    db.prepare(`INSERT INTO delivery_token(token_hash,result_id,wallet_id,expires_at)
      VALUES(?,?,?,?)`).run(sha(Buffer.from(TOKEN)), 'result-1', 'wallet-1', now + 60_000);
  });

  afterEach(async () => {
    await app.close();
    db.close();
    await rm(root, { recursive: true, force: true });
  });

  const fetchResult = () => app.inject({
    method: 'GET', url: '/v1/jobs/job-1/result', headers: { ...headers, 'x-result-token': TOKEN },
  });
  const receipt = (attempt: string) => app.inject({
    method: 'POST', url: '/v1/jobs/job-1/delivery-receipts', headers,
    payload: { deliveryAttemptId: attempt, bytes: bytes.length, sha256: sha(bytes) },
  });
  const job = () => db.prepare('SELECT state, charged, reserved, fail_reason FROM job WHERE id = ?').get('job-1');
  const balance = () => (db.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-1') as { balance: number }).balance;
  const count = (kind: string) => (db.prepare(
    `SELECT count(*) AS n FROM ledger WHERE kind = ? AND idempotency_key = 'job-1'`).get(kind) as { n: number }).n;
  const sweep = () => sweepExpiredResources(db, ledger, Date.now() + 3_600_000);

  it('charges at fetch with no receipt and expiry does not release', async () => {
    const res = await fetchResult();
    expect(res.statusCode).toBe(200);
    expect(Buffer.from(res.rawPayload).equals(bytes)).toBe(true);
    expect(job()).toMatchObject({ state: 'SUCCEEDED', charged: RESERVED, reserved: RESERVED });
    expect(balance()).toBe(10 - RESERVED);
    expect(count('SETTLE')).toBe(1);
    await sweep();
    expect(count('RELEASE')).toBe(0);
    expect(balance()).toBe(10 - RESERVED);
    expect(job()).toMatchObject({ state: 'SUCCEEDED', charged: RESERVED, fail_reason: null });
  });

  it('a valid receipt after the fetch-charge is idempotent confirmation', async () => {
    const res = await fetchResult();
    const attempt = res.headers['x-delivery-attempt'] as string;
    const confirmed = await receipt(attempt);
    expect(confirmed.statusCode).toBe(200);
    expect(confirmed.json()).toEqual({ charged: RESERVED, state: 'SETTLED' });
    expect((await receipt(attempt)).json()).toEqual({ charged: RESERVED, state: 'SETTLED' });
    expect(count('SETTLE')).toBe(1);
    expect(balance()).toBe(10 - RESERVED);
    expect(job()).toMatchObject({ charged: RESERVED });
  });

  it('rejects a receipt when no bytes were ever issued and leaves the wallet untouched', async () => {
    const res = await receipt('never-issued');
    expect(res.statusCode).toBe(400);
    expect(res.json().error.code).toBe('DELIVERY_RECEIPT_MISMATCH');
    expect(() => ledger.settle('wallet-1', 'job-1', { deliveryAttemptId: 'never-issued', bytes: bytes.length, sha256: sha(bytes) }))
      .toThrow('DELIVERY_RECEIPT_MISMATCH');
    expect(job()).toMatchObject({ charged: 0, reserved: RESERVED });
    expect(balance()).toBe(10 - RESERVED);
    expect(count('SETTLE')).toBe(0);
  });

  it('an aborted transfer does not charge and expiry releases', async () => {
    // Fake response: the socket closes without 'finish', as on a client abort or stream error.
    const aborted = Object.assign(new EventEmitter(), { statusCode: 200 });
    let charged = 0;
    onCompletedSend(aborted, () => { charged += 1; });
    aborted.emit('close');
    expect(charged).toBe(0);
    const completed = Object.assign(new EventEmitter(), { statusCode: 200 });
    onCompletedSend(completed, () => { charged += 1; });
    completed.emit('finish');
    expect(charged).toBe(1);
    const failed = Object.assign(new EventEmitter(), { statusCode: 500 });
    onCompletedSend(failed, () => { charged += 1; });
    failed.emit('finish');
    expect(charged).toBe(1);

    // An issued attempt whose transfer never completed leaves the job uncharged.
    db.prepare(`INSERT INTO delivery_attempt(id,result_id,wallet_id,created_at,expires_at)
      VALUES('attempt-aborted','result-1','wallet-1',?,?)`).run(Date.now(), Date.now() + 60_000);
    expect(job()).toMatchObject({ charged: 0, reserved: RESERVED });
    expect(count('SETTLE')).toBe(0);
    await sweep();
    expect(count('RELEASE')).toBe(1);
    expect(balance()).toBe(10);
    expect(job()).toMatchObject({ state: 'FAILED', fail_reason: 'RESULT_EXPIRED', charged: 0 });
  });

  it('repeated fetches charge once', async () => {
    expect((await fetchResult()).statusCode).toBe(200);
    expect((await fetchResult()).statusCode).toBe(200);
    expect((await fetchResult()).statusCode).toBe(200);
    expect(count('SETTLE')).toBe(1);
    expect(balance()).toBe(10 - RESERVED);
    expect(job()).toMatchObject({ charged: RESERVED });
  });

  it('a fidelity-guard failure is released and costs nothing', async () => {
    const dataDir = join(root, 'queue');
    const drift = {
      bytes, sha256: sha(bytes), provenance: 'ORIGINAL_PIXELS' as const, width: 4, height: 4, format: 'image/png',
      guard: { passed: false },
    };
    const queue = new JobQueue(db, ledger, { run: async () => drift } as unknown as ProviderAdapter, 1, dataDir,
      { dailySpendCapMicroUsd: 1_000_000 });
    await queue.enqueue({
      jobId: 'job-guard', walletId: 'wallet-1', op: 'O-02', clientRequestId: 'client-guard',
      imageBase64: Buffer.from('source').toString('base64'), uploadIds: [], params: {}, credits: 2,
    });
    await queue.waitForIdle();
    expect(db.prepare('SELECT state, charged FROM job WHERE id = ?').get('job-guard'))
      .toMatchObject({ state: 'FAILED', charged: 0 });
    expect(db.prepare(`SELECT count(*) AS n FROM ledger WHERE kind = 'RELEASE' AND idempotency_key = 'job-guard'`).get())
      .toEqual({ n: 1 });
    expect(balance()).toBe(10 - RESERVED);
  });
});
