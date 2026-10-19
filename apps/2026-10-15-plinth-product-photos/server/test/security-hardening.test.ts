/**
 * Server-side protection of money and availability.
 *
 * Every leg here drives the HTTP surface (or the queue directly where the HTTP
 * surface would need a provider), and asserts the observable outcome: a status,
 * a refusal code, a row count, a wallet balance. Nothing asserts that a guard
 * "exists".
 */
import { createHash, randomUUID } from 'node:crypto';
import { mkdir, rm, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import type Database from 'better-sqlite3';
import type { FastifyInstance } from 'fastify';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { createApp } from '../src/app.js';
import { loadConfig, type ServerConfig } from '../src/config.js';
import { createChallenge, sessionRequestHash } from '../src/integrity.js';
import { CreditLedger } from '../src/ledger.js';
import { JobQueue } from '../src/queue.js';
import { MAX_BUCKETS, TokenBucketLimiter, networkKey } from '../src/rate-limit.js';
import { MODEL_REGISTRY, type ProviderAdapter } from '../src/registry.js';
import { isBelowMinimum } from '../src/versions.js';
import { seedDeliverable, seedJob, testDatabase } from './helpers.js';

const root = join(process.cwd(), '.kiln', 'scratch', 'security-hardening');
const apps: FastifyInstance[] = [];
const databases: Database.Database[] = [];
const directories: string[] = [];

afterEach(async () => {
  for (const app of apps.splice(0)) await app.close();
  for (const db of databases.splice(0)) {
    try {
      db.close();
    } catch {
      // Already closed by the test that wanted an unreachable database.
    }
  }
  for (const dir of directories.splice(0)) await rm(dir, { recursive: true, force: true });
  vi.unstubAllGlobals();
});

interface Fixture {
  app: FastifyInstance;
  db: Database.Database;
  config: ServerConfig;
  dir: string;
}

async function makeApp(
  env: NodeJS.ProcessEnv = {},
  options: { db?: Database.Database; provider?: ProviderAdapter | null } = {},
): Promise<Fixture> {
  const dir = join(root, randomUUID());
  directories.push(dir);
  await mkdir(dir, { recursive: true });
  const db = options.db ?? testDatabase();
  databases.push(db);
  const config = loadConfig({
    JWT_SECRET: 'x'.repeat(32),
    DATA_DIR: join(dir, 'data'),
    KILL_SWITCH_FILE: join(dir, 'KILL_SWITCH'),
    SERVICE_CLOSED_FILE: join(dir, 'SERVICE_CLOSED'),
    RECOVERY_PEPPER: 'test-recovery-pepper',
    ...env,
  });
  const app = await createApp({
    db,
    config,
    provider: options.provider ?? null,
    closeDatabaseOnExit: false,
  });
  apps.push(app);
  return { app, db, config, dir };
}

function linkDevice(db: Database.Database, deviceId: string, walletId: string): void {
  db.prepare(`INSERT INTO device(id, install_hash, created_at, integrity_state)
    VALUES (?, ?, ?, 'PLAY_RECOGNIZED')`).run(deviceId, `install-${deviceId}`, Date.now());
  db.prepare('INSERT INTO device_wallet(device_id, wallet_id) VALUES (?, ?)').run(deviceId, walletId);
}

function auth(app: FastifyInstance, deviceId = 'device-1', walletId = 'wallet-1', ver?: string) {
  return {
    authorization: `Bearer ${app.jwt.sign({
      sub: deviceId,
      wid: walletId,
      gen: 0,
      ...(ver === undefined ? {} : { ver }),
    })}`,
  };
}

function jobPayload(clientRequestId = 'req-1') {
  return {
    clientRequestId,
    op: 'O-02',
    uploadIds: ['upload-missing'],
    params: {},
    estimateCredits: MODEL_REGISTRY['O-02'].credits,
  };
}

describe('client version arithmetic', () => {
  it('compares numerically, not lexically, and fails closed when the version is unknown', () => {
    expect(isBelowMinimum('1.9.9', '1.10.0')).toBe(true);
    expect(isBelowMinimum('1.10.0', '1.10.0')).toBe(false);
    expect(isBelowMinimum('1.10.1', '1.10.0')).toBe(false);
    expect(isBelowMinimum('2', '1.10.0')).toBe(false);
    expect(isBelowMinimum(undefined, '1.0.0')).toBe(true);
    expect(isBelowMinimum('not-a-version', '1.0.0')).toBe(true);
    // A zero or unparseable minimum is the factory default and means "no gate".
    expect(isBelowMinimum(undefined, '0')).toBe(false);
    expect(isBelowMinimum('0.0.1', 'nonsense')).toBe(false);
  });
});

describe('rate limiting', () => {
  it('counts one IPv6 /64 as one caller, and IPv4-mapped addresses as their IPv4 form', () => {
    // A /64 is the block an ISP delegates; rotating inside it must not mint buckets.
    expect(networkKey('2001:db8:aaaa:bbbb:1:2:3:4')).toBe(networkKey('2001:db8:aaaa:bbbb:9:9:9:9'));
    expect(networkKey('2001:db8:aaaa:bbbb::1')).toBe('ip:[2001:db8:aaaa:bbbb/64]');
    expect(networkKey('2001:db8:aaaa:bbbb::1')).not.toBe(networkKey('2001:db8:aaaa:cccc::1'));
    // Node reports a dual-stack client this way; it must share the IPv4 bucket.
    expect(networkKey('::ffff:203.0.113.9')).toBe(networkKey('203.0.113.9'));
    expect(networkKey('203.0.113.9')).toBe('ip:203.0.113.9');
  });

  it('caps the bucket map, evicting oldest entries instead of growing without bound', () => {
    const limiter = new TokenBucketLimiter(60_000, 8);
    for (let index = 0; index < 100; index += 1) {
      limiter.take(`ip:198.51.100.${index}:config`, { capacity: 1, windowMs: 60_000 }, 1_000);
    }
    expect(limiter.size).toBeLessThanOrEqual(8);
    expect(limiter.size).toBe(8);
    // The default is finite too; a zero or fractional cap is a construction error.
    expect(MAX_BUCKETS).toBeGreaterThan(0);
    expect(() => new TokenBucketLimiter(60_000, 0)).toThrow(/maxBuckets/);
  });

  it('refuses the 13th unauthenticated challenge from one address within five minutes', async () => {
    const { app } = await makeApp();
    const payload = { installId: 'install-a', appVersion: '1.0.0', locale: 'en-US' };
    const accepted: number[] = [];
    for (let index = 0; index < 12; index += 1) {
      const response = await app.inject({ method: 'POST', url: '/v1/integrity/challenge', payload });
      accepted.push(response.statusCode);
    }
    const refused = await app.inject({ method: 'POST', url: '/v1/integrity/challenge', payload });
    // 503 is the unconfigured verifier; the point is that the limiter let 12 through.
    expect(accepted).toEqual(Array.from({ length: 12 }, () => 503));
    expect(refused.statusCode).toBe(429);
    expect(refused.json().error.code).toBe('RATE_LIMITED');
    expect(Number(refused.headers['retry-after'])).toBeGreaterThan(0);
  });

  it('keeps one network bucket per route, so challenge spending cannot refuse a session', async () => {
    const { app } = await makeApp();
    const payload = { installId: 'install-a', appVersion: '1.0.0', locale: 'en-US' };
    for (let index = 0; index < 12; index += 1) {
      const response = await app.inject({
        method: 'POST', url: '/v1/integrity/challenge', payload,
      });
      expect(response.statusCode).toBe(503); // unconfigured verifier; the limiter allowed it
    }
    const session = await app.inject({
      method: 'POST',
      url: '/v1/session',
      payload: {
        installId: 'install-a',
        appVersion: '1.0.0',
        locale: 'en-US',
        integrityToken: 'unused',
        challengeId: randomUUID(),
        challenge: 'unused',
      },
    });
    // The challenge bucket is empty; the session route has its own and reaches its handler.
    expect(session.statusCode).toBe(503);
    expect(session.json().error.code).toBe('NOT_CONFIGURED');
  });

  it('allows three recovery codes an hour per device, then refuses', async () => {
    const db = testDatabase();
    linkDevice(db, 'device-1', 'wallet-1');
    const { app } = await makeApp({}, { db });
    for (let index = 0; index < 3; index += 1) {
      const response = await app.inject({
        method: 'POST', url: '/v1/wallet/recovery-code', headers: auth(app),
      });
      expect(response.statusCode).toBe(200);
      expect(typeof response.json().code).toBe('string');
    }
    const refused = await app.inject({
      method: 'POST', url: '/v1/wallet/recovery-code', headers: auth(app),
    });
    expect(refused.statusCode).toBe(429);
    expect(refused.json().error.code).toBe('RATE_LIMITED');
    expect(Number(refused.headers['retry-after'])).toBeGreaterThan(0);
  });

  it('keeps a separate device bucket per device, so one caller cannot starve another', async () => {
    const db = testDatabase();
    db.prepare('INSERT INTO wallet(id, balance, created_at) VALUES (?, 0, ?)')
      .run('wallet-2', Date.now());
    linkDevice(db, 'device-1', 'wallet-1');
    linkDevice(db, 'device-2', 'wallet-2');
    const { app } = await makeApp({}, { db });
    for (let index = 0; index < 3; index += 1) {
      expect((await app.inject({
        method: 'POST', url: '/v1/wallet/recovery-code', headers: auth(app, 'device-1', 'wallet-1'),
      })).statusCode).toBe(200);
    }
    // device-2 still has its own three, even though device-1 is exhausted.
    expect((await app.inject({
      method: 'POST', url: '/v1/wallet/recovery-code', headers: auth(app, 'device-2', 'wallet-2'),
    })).statusCode).toBe(200);
  });

  it('keeps device buckets per route, so polling the wallet cannot refuse a recovery code', async () => {
    const db = testDatabase();
    linkDevice(db, 'device-1', 'wallet-1');
    const { app } = await makeApp({}, { db });
    // Wallet reads have a 60/min bucket; spend all of it.
    for (let index = 0; index < 60; index += 1) {
      expect((await app.inject({
        method: 'GET', url: '/v1/wallet', headers: auth(app),
      })).statusCode).toBe(200);
    }
    expect((await app.inject({
      method: 'GET', url: '/v1/wallet', headers: auth(app),
    })).statusCode).toBe(429);
    // The recovery-code route has its own bucket and is still available.
    expect((await app.inject({
      method: 'POST', url: '/v1/wallet/recovery-code', headers: auth(app),
    })).statusCode).toBe(200);
  });
});

describe('service states', () => {
  it('reports maintenance, disables every operation and keeps reads open', async () => {
    const db = testDatabase();
    linkDevice(db, 'device-1', 'wallet-1');
    const { app, dir } = await makeApp({}, { db });
    await writeFile(join(dir, 'KILL_SWITCH'), 'paused');

    const config = await app.inject({ url: '/config' });
    expect(config.statusCode).toBe(200);
    expect(config.json()).toMatchObject({
      serviceState: 'maintenance',
      killSwitch: true,
      notice: { code: 'SERVICE_MAINTENANCE', dismissible: false },
    });
    expect(Object.values(config.json().ops as Record<string, { enabled: boolean }>)
      .every((operation) => operation.enabled === false)).toBe(true);

    // A read still answers: the balance is how the user knows what is left.
    const wallet = await app.inject({ url: '/v1/wallet', headers: auth(app) });
    expect(wallet.statusCode).toBe(200);
    expect(wallet.json().balance).toBe(10);
  });

  it('lets closed outrank maintenance and report it in /health', async () => {
    const { app, dir } = await makeApp();
    await writeFile(join(dir, 'KILL_SWITCH'), 'paused');
    await writeFile(join(dir, 'SERVICE_CLOSED'), 'closed');
    const config = await app.inject({ url: '/config' });
    expect(config.json().serviceState).toBe('closed');
    expect(config.json().notice.code).toBe('SERVICE_CLOSED');
    const health = await app.inject({ url: '/health' });
    expect(health.statusCode).toBe(200);
    expect(health.json()).toMatchObject({ ok: true, serviceState: 'closed' });
  });

  it('answers update_required for a stated old build and ok for a current one', async () => {
    const { app } = await makeApp({ MIN_APP_VERSION: '1.2.0' });
    const state = async (version: string) => (await app.inject({
      url: '/config', headers: { 'x-app-version': version },
    })).json().serviceState;
    expect(await state('1.1.9')).toBe('update_required');
    expect(await state('1.2.0')).toBe('ok');
    expect(await state('1.10.0')).toBe('ok');
    // A stated but unparseable version is below any enforceable minimum.
    expect(await state('release-candidate')).toBe('update_required');
    // No claim at all is not a claim: the read reports the operator state.
    expect((await app.inject({ url: '/config' })).json().serviceState).toBe('ok');
  });

  it('refuses below the minimum and accepts a request exactly at the minimum', async () => {
    const db = testDatabase();
    linkDevice(db, 'device-1', 'wallet-1');
    const { app } = await makeApp({ MIN_APP_VERSION: '1.5.0' }, { db });

    const below = await app.inject({
      method: 'POST', url: '/v1/wallet/recovery-code', headers: auth(app, 'device-1', 'wallet-1', '1.4.9'),
    });
    expect(below.statusCode).toBe(426);
    expect(below.json().error.code).toBe('UPDATE_REQUIRED');

    const atMinimum = await app.inject({
      method: 'POST', url: '/v1/wallet/recovery-code', headers: auth(app, 'device-1', 'wallet-1', '1.5.0'),
    });
    expect(atMinimum.statusCode).toBe(200);
  });

  it('refuses a new job from an outdated signed session and does not reserve credits', async () => {
    const db = testDatabase();
    linkDevice(db, 'device-1', 'wallet-1');
    const { app } = await makeApp({ MIN_APP_VERSION: '1.5.0' }, { db });
    const response = await app.inject({
      method: 'POST', url: '/v1/jobs', headers: auth(app, 'device-1', 'wallet-1', '1.4.9'),
      payload: jobPayload(),
    });
    expect(response.statusCode).toBe(426);
    expect(response.json().error.code).toBe('UPDATE_REQUIRED');
    expect(response.json().error.details.minAppVersion).toBe('1.5.0');
    expect(db.prepare('SELECT count(*) AS n FROM job').get()).toEqual({ n: 0 });
    expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-1')).toEqual({ balance: 10 });
  });

  it('refuses a new job while closed, with a distinct code', async () => {
    const db = testDatabase();
    linkDevice(db, 'device-1', 'wallet-1');
    const { app, dir } = await makeApp({ MIN_APP_VERSION: '1.5.0' }, { db });
    await writeFile(join(dir, 'SERVICE_CLOSED'), 'closed');
    const response = await app.inject({
      method: 'POST', url: '/v1/jobs', headers: auth(app, 'device-1', 'wallet-1', '1.5.0'),
      payload: jobPayload(),
    });
    expect(response.statusCode).toBe(503);
    expect(response.json().error.code).toBe('SERVICE_CLOSED');
    expect(db.prepare('SELECT count(*) AS n FROM job').get()).toEqual({ n: 0 });
  });

  it('names a pause SERVICE_MAINTENANCE on the job route, not the old KILL_SWITCH', async () => {
    const db = testDatabase();
    linkDevice(db, 'device-1', 'wallet-1');
    const { app, dir } = await makeApp({}, { db });
    await writeFile(join(dir, 'KILL_SWITCH'), 'paused');
    const response = await app.inject({
      method: 'POST', url: '/v1/jobs', headers: auth(app), payload: jobPayload(),
    });
    expect(response.statusCode).toBe(503);
    expect(response.json().error.code).toBe('SERVICE_MAINTENANCE');
    expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-1')).toEqual({ balance: 10 });
  });

  it('lets the queue refuse a pause with the same code the HTTP guard uses', async () => {
    const db = testDatabase();
    const stateDir = join(root, 'queue-state');
    await mkdir(stateDir, { recursive: true });
    const killSwitchFile = join(stateDir, 'KILL_SWITCH');
    await writeFile(killSwitchFile, 'paused');
    const queue = new JobQueue(
      db,
      new CreditLedger(db),
      { run: vi.fn(), recognize: vi.fn() } as unknown as ProviderAdapter,
      1,
      join(stateDir, 'data'),
      { killSwitchFile },
    );
    await expect(queue.enqueue({
      jobId: 'job-state', walletId: 'wallet-1', clientRequestId: 'req-state', op: 'O-02',
      imageBase64: Buffer.from('x').toString('base64'), uploadIds: [], params: {},
      credits: MODEL_REGISTRY['O-02'].credits,
    })).rejects.toThrow('SERVICE_MAINTENANCE');
    expect(db.prepare('SELECT count(*) AS n FROM job').get()).toEqual({ n: 0 });
  });

  it('keeps job status and refunds available to an outdated build', async () => {
    const db = testDatabase();
    linkDevice(db, 'device-1', 'wallet-1');
    seedJob(db, 'job-old', 'wallet-1');
    const ledger = new CreditLedger(db);
    expect(ledger.reserve('wallet-1', 'job-old', 2)['accepted']).toBe(true);
    const { app } = await makeApp({ MIN_APP_VERSION: '1.5.0' }, { db });

    const read = await app.inject({
      url: '/v1/jobs/job-old', headers: auth(app, 'device-1', 'wallet-1', '1.0.0'),
    });
    expect(read.statusCode).toBe(200);
    expect(read.json().state).toBe('QUEUED');

    const refund = await app.inject({
      method: 'POST', url: '/v1/jobs/job-old/cancel', headers: auth(app, 'device-1', 'wallet-1', '1.0.0'),
    });
    expect(refund.json()).toMatchObject({ state: 'CANCELLED', released: 2 });
    expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-1')).toEqual({ balance: 10 });
  });

  it('binds the attested version into the session token and reports the state in the session config', async () => {
    const { app } = await makeApp({ PLINTH_DEV_INTEGRITY: '1', MIN_APP_VERSION: '2.0.0' });
    const fields = { installId: 'install-a', appVersion: '1.9.0', locale: 'en-US' };
    const challenge = await app.inject({
      method: 'POST', url: '/v1/integrity/challenge', payload: fields,
    });
    expect(challenge.statusCode).toBe(200);
    const { challengeId, challenge: nonce } = challenge.json();
    const input: { challengeId: string; challenge: string; installId: string; appVersion: string;
      locale: string; integrityToken: string } = {
      ...fields, challengeId, challenge: nonce, integrityToken: '',
    };
    input.integrityToken = `dev:${sessionRequestHash(input)}`;
    const session = await app.inject({ method: 'POST', url: '/v1/session', payload: input });
    expect(session.statusCode).toBe(200);
    expect(session.json().config).toMatchObject({
      serviceState: 'update_required',
      notice: { code: 'UPDATE_REQUIRED' },
    });
    // The token itself carries `ver`, so a later call is refused without any header.
    const blocked = await app.inject({
      method: 'POST', url: '/v1/purchases/intents',
      headers: { authorization: `Bearer ${session.json().deviceToken}` },
      payload: { productId: 'credits_20' },
    });
    expect(blocked.statusCode).toBe(426);
    expect(blocked.json().error.code).toBe('UPDATE_REQUIRED');
  });

  it('fails /health when the database is unreachable', async () => {
    const { app, db } = await makeApp();
    db.close();
    const response = await app.inject({ url: '/health' });
    expect(response.statusCode).toBe(503);
    expect(response.json()).toMatchObject({ ok: false, databaseReachable: false });
  });
});

describe('payload ceilings', () => {
  it('refuses parameters past 8 KiB before a job row or a provider call exists', async () => {
    const db = testDatabase();
    linkDevice(db, 'device-1', 'wallet-1');
    const run = vi.fn();
    const provider = { run, recognize: vi.fn() } as unknown as ProviderAdapter;
    const { app } = await makeApp({}, { db, provider });
    const response = await app.inject({
      method: 'POST', url: '/v1/jobs', headers: auth(app),
      payload: { ...jobPayload(), params: { prompt: 'x'.repeat(9000) } },
    });
    expect(response.statusCode).toBe(400);
    expect(response.json().error.code).toBe('INVALID_OPERATION_INPUT');
    expect(run).not.toHaveBeenCalled();
    expect(db.prepare('SELECT count(*) AS n FROM job').get()).toEqual({ n: 0 });
    expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-1')).toEqual({ balance: 10 });
  });

  it('refuses a prompt longer than 2000 characters', async () => {
    const db = testDatabase();
    linkDevice(db, 'device-1', 'wallet-1');
    const run = vi.fn();
    const provider = { run, recognize: vi.fn() } as unknown as ProviderAdapter;
    const { app } = await makeApp({}, { db, provider });
    const response = await app.inject({
      method: 'POST', url: '/v1/jobs', headers: auth(app),
      payload: { ...jobPayload(), params: { prompt: 'x'.repeat(2001) } },
    });
    expect(response.statusCode).toBe(400);
    expect(response.json().error.message).toContain('2000');
    expect(run).not.toHaveBeenCalled();
  });

  it('refuses a request body above the 5 MiB limit', async () => {
    const db = testDatabase();
    linkDevice(db, 'device-1', 'wallet-1');
    const { app } = await makeApp({}, { db });
    const response = await app.inject({
      method: 'PUT', url: '/v1/uploads',
      headers: { ...auth(app), 'content-type': 'image/jpeg', 'x-client-sha256': '0'.repeat(64) },
      payload: Buffer.alloc(5 * 1024 * 1024 + 16, 1),
    });
    expect(response.statusCode).toBe(413);
    expect(response.json().error.code).toBe('TOO_LARGE');
  });
});

describe('wallet isolation and refund idempotency', () => {
  it('does not let one wallet read another wallet job or result', async () => {
    const db = testDatabase();
    db.prepare('INSERT INTO wallet(id, balance, created_at) VALUES (?, 0, ?)').run('wallet-2', Date.now());
    linkDevice(db, 'device-1', 'wallet-1');
    linkDevice(db, 'device-2', 'wallet-2');
    seedJob(db, 'job-1', 'wallet-1');
    seedDeliverable(db, 'job-1', 'wallet-1');
    db.prepare(`INSERT INTO result_metadata(result_id, width, height, format, provenance, guard_json)
      VALUES ('result-job-1', 3, 2, 'image/jpeg', 'fal', NULL)`).run();
    const { app } = await makeApp({}, { db });

    const own = await app.inject({ url: '/v1/jobs/job-1', headers: auth(app, 'device-1', 'wallet-1') });
    expect(own.statusCode).toBe(200);
    const token = own.json().result.resultToken as string;

    const foreignStatus = await app.inject({
      url: '/v1/jobs/job-1', headers: auth(app, 'device-2', 'wallet-2'),
    });
    expect(foreignStatus.statusCode).toBe(404);
    expect(foreignStatus.json().error.code).toBe('JOB_NOT_FOUND');

    const foreignBytes = await app.inject({
      url: '/v1/jobs/job-1/result',
      headers: { ...auth(app, 'device-2', 'wallet-2'), 'x-result-token': token },
    });
    expect(foreignBytes.statusCode).toBe(404);
    expect(foreignBytes.json().error.code).toBe('RESULT_NOT_AVAILABLE');

    const foreignReceipt = await app.inject({
      method: 'POST', url: '/v1/jobs/job-1/delivery-receipts',
      headers: auth(app, 'device-2', 'wallet-2'),
      payload: { deliveryAttemptId: 'attempt-1', bytes: 7, sha256: 'a'.repeat(64) },
    });
    expect(foreignReceipt.statusCode).toBe(400);
    expect(foreignReceipt.json().error.code).toBe('DELIVERY_RECEIPT_MISMATCH');
    expect(db.prepare('SELECT charged FROM job WHERE id = ?').get('job-1')).toEqual({ charged: 0 });
  });

  it('releases a reservation once, so a repeated cancel cannot mint credits', async () => {
    const db = testDatabase(10);
    linkDevice(db, 'device-1', 'wallet-1');
    seedJob(db, 'job-refund', 'wallet-1');
    expect(new CreditLedger(db).reserve('wallet-1', 'job-refund', 3)['accepted']).toBe(true);
    const { app } = await makeApp({}, { db });

    const first = await app.inject({
      method: 'POST', url: '/v1/jobs/job-refund/cancel', headers: auth(app),
    });
    const second = await app.inject({
      method: 'POST', url: '/v1/jobs/job-refund/cancel', headers: auth(app),
    });
    expect(first.json()).toMatchObject({ state: 'CANCELLED', released: 3 });
    expect(second.json()).toMatchObject({ state: 'CANCELLED', released: 0 });
    expect(db.prepare(`SELECT count(*) AS n FROM ledger WHERE kind = 'RELEASE'`).get()).toEqual({ n: 1 });
    expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-1')).toEqual({ balance: 10 });
  });

  it('returns the existing job for a replayed clientRequestId and reserves only once', async () => {
    const db = testDatabase(10);
    const ledger = new CreditLedger(db);
    const provider = {
      run: async () => {
        throw new Error('provider down');
      },
      recognize: vi.fn(),
    } as unknown as ProviderAdapter;
    const queue = new JobQueue(db, ledger, provider, 1, join(root, 'queue-data'));
    const work = {
      jobId: 'job-a',
      walletId: 'wallet-1',
      clientRequestId: 'req-a',
      op: 'O-02',
      imageBase64: Buffer.from('x').toString('base64'),
      uploadIds: [],
      params: {},
      credits: 2,
    };
    const first = await queue.enqueue(work);
    const second = await queue.enqueue({ ...work, jobId: 'job-b' });
    await queue.waitForIdle();
    expect(second['jobId']).toBe(first['jobId']);
    expect(db.prepare(`SELECT count(*) AS n FROM ledger WHERE kind = 'RESERVE'`).get()).toEqual({ n: 1 });
    expect(db.prepare('SELECT count(*) AS n FROM job').get()).toEqual({ n: 1 });
    expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-1')).toEqual({ balance: 10 });
  });

  it('keeps at most three live delivery tokens per result', async () => {
    const db = testDatabase();
    linkDevice(db, 'device-1', 'wallet-1');
    seedJob(db, 'job-token', 'wallet-1');
    seedDeliverable(db, 'job-token', 'wallet-1');
    db.prepare(`INSERT INTO result_metadata(result_id, width, height, format, provenance, guard_json)
      VALUES ('result-job-token', 3, 2, 'image/jpeg', 'fal', NULL)`).run();
    const { app } = await makeApp({}, { db });

    const tokens: string[] = [];
    for (let index = 0; index < 5; index += 1) {
      const response = await app.inject({ url: '/v1/jobs/job-token', headers: auth(app) });
      expect(response.statusCode).toBe(200);
      tokens.push(response.json().result.resultToken as string);
    }
    expect(db.prepare(
      `SELECT count(*) AS n FROM delivery_token WHERE result_id = 'result-job-token' AND used_at IS NULL`,
    ).get()).toEqual({ n: 3 });
    const newest = createHash('sha256').update(tokens[4]!).digest('hex');
    expect(db.prepare(`SELECT 1 FROM delivery_token WHERE token_hash = ?`).get(newest)).toBeDefined();
  });
});
