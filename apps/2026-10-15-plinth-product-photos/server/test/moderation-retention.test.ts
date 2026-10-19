import { afterEach, describe, expect, it } from 'vitest';
import { createHash } from 'node:crypto';
import { mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import Database from 'better-sqlite3';
import { createDatabase } from '../src/db.js';
import { sweepExpiredResources } from '../src/retention.js';
import { recordOutputReport, reportStatus } from '../src/moderation.js';
import { testDatabase } from './helpers.js';
import { createApp } from '../src/app.js';
import { loadConfig } from '../src/config.js';

describe('reporting and retention', () => {
  it('creates a moderation case at the repeated-report threshold and exposes its state', () => {
    const db = testDatabase();
    try {
      const now = Date.now();
      for (const [wallet, job] of [['wallet-1', 'job-report-1'], ['wallet-2', 'job-report-2'], ['wallet-3', 'job-report-3']]) {
        db.prepare('INSERT OR IGNORE INTO wallet(id,balance,created_at) VALUES(?,0,?)').run(wallet, now);
        db.prepare(`INSERT INTO job(id,wallet_id,op,state,reserved,client_request_id,created_at)
          VALUES(?,?,'O-02','SUCCEEDED',0,?,?)`).run(job, wallet, `client-${job}`, now);
        db.prepare(`INSERT INTO result(id,job_id,wallet_id,path,sha256,bytes,expires_at)
          VALUES(?,?,?,?,'a',1,?)`).run(`result-${job}`, job, wallet, `result-path-${job}`, now + 60000);
      }
      const first = recordOutputReport(db, 'wallet-1', 'job-report-1', 'OFFENSIVE');
      expect(first.caseStatus).toBe('OPEN');
      recordOutputReport(db, 'wallet-2', 'job-report-2', 'OFFENSIVE');
      const third = recordOutputReport(db, 'wallet-3', 'job-report-3', 'UNSAFE');
      expect(third.caseStatus).toBe('ESCALATED');
      expect(reportStatus(db, 'wallet-1', first.id)?.caseStatus).toBe('ESCALATED');
    } finally {
      db.close();
    }
  });

  it('denies cross-wallet report state reads', () => {
    const db = testDatabase();
    try {
      db.prepare(`INSERT INTO wallet(id,balance,created_at) VALUES('wallet-2',0,?)`).run(Date.now());
      db.prepare(`INSERT INTO job(id,wallet_id,op,state,reserved,client_request_id,created_at)
        VALUES('job-report','wallet-1','O-02','SUCCEEDED',0,'client-report',?)`).run(Date.now());
      db.prepare(`INSERT INTO result(id,job_id,wallet_id,path,sha256,bytes,expires_at)
        VALUES('result-report','job-report','wallet-1','result-path','a',1,?)`).run(Date.now() + 60000);
      const report = recordOutputReport(db, 'wallet-1', 'job-report', 'OTHER');
      expect(reportStatus(db, 'wallet-2', report.id)).toBeNull();
    } finally {
      db.close();
    }
  });

  it('denies cross-wallet results and expired opaque result tokens', async () => {
    const db = createDatabase(':memory:');
    const now = Date.now();
    for (const [device, wallet] of [['device-1', 'wallet-1'], ['device-2', 'wallet-2']]) {
      db.prepare(`INSERT INTO device(id,install_hash,created_at,integrity_state)
        VALUES(?,?,?,'PLAY_RECOGNIZED')`).run(device, device, now);
      db.prepare('INSERT INTO wallet(id,balance,created_at) VALUES(?,0,?)').run(wallet, now);
      db.prepare('INSERT INTO device_wallet(device_id,wallet_id) VALUES(?,?)').run(device, wallet);
    }
    const rawToken = 'opaque-result-token';
    const tokenHash = createHash('sha256').update(rawToken).digest('hex');
    db.prepare(`INSERT INTO job(id,wallet_id,op,state,reserved,client_request_id,created_at)
      VALUES('job-owned','wallet-1','O-02','SUCCEEDED',0,'owned-request',?)`).run(now);
    db.prepare(`INSERT INTO result(id,job_id,wallet_id,path,sha256,bytes,expires_at)
      VALUES('result-owned','job-owned','wallet-1','unused-result-path','a',1,?)`).run(now + 60000);
    db.prepare(`INSERT INTO delivery_token(token_hash,result_id,wallet_id,expires_at)
      VALUES(?,?,?,?)`).run(tokenHash, 'result-owned', 'wallet-1', now - 1);
    const app = await createApp({ db, config: loadConfig({ JWT_SECRET: 'x'.repeat(32) }), closeDatabaseOnExit: false });
    try {
      const foreignToken = app.jwt.sign({ sub: 'device-2', wid: 'wallet-2', gen: 0 });
      const crossWallet = await app.inject({
        method: 'GET', url: '/v1/jobs/job-owned', headers: { authorization: `Bearer ${foreignToken}` },
      });
      expect(crossWallet.statusCode).toBe(404);
      const ownerToken = app.jwt.sign({ sub: 'device-1', wid: 'wallet-1', gen: 0 });
      const expired = await app.inject({
        method: 'GET', url: '/v1/jobs/job-owned/result',
        headers: { authorization: `Bearer ${ownerToken}`, 'x-result-token': rawToken },
      });
      expect(expired.statusCode).toBe(404);
    } finally {
      await app.close();
    }
  }, 15_000);

  it('deletes expired upload files and rows during retention sweep', async () => {
    const db = createDatabase(':memory:');
    const root = join(process.cwd(), '.kiln', 'scratch', 'plinth-w4-server', 'retention-test');
    const path = join(root, 'expired-upload');
    try {
      await mkdir(root, { recursive: true });
      await writeFile(path, 'upload');
      db.prepare('INSERT INTO wallet(id,balance,created_at) VALUES(?,?,?)').run('wallet-1', 0, Date.now());
      db.prepare(`INSERT INTO upload(id,wallet_id,path,sha256,bytes,created_at,expires_at)
        VALUES(?,?,?,?,?,?,?)`).run('upload-1', 'wallet-1', path, 'abc', 6, Date.now() - 4000000, Date.now() - 1);
      await sweepExpiredResources(db, undefined, Date.now());
      expect(await readFile(path).catch(() => null)).toBeNull();
      expect(db.prepare('SELECT deleted_at FROM upload WHERE id = ?').get('upload-1')).toMatchObject({
        deleted_at: expect.any(Number),
      });
    } finally {
      db.close();
      await rm(root, { recursive: true, force: true });
    }
  });

  it('keeps delivery receipts durable after an expired result is removed', async () => {
    const db = testDatabase();
    const now = Date.now();
    const root = join(process.cwd(), '..', '..', '..', '.kiln', 'scratch', 'plinth-w4-server');
    try {
      db.prepare(`INSERT INTO job(id,wallet_id,op,state,reserved,charged,client_request_id,created_at)
        VALUES('job-receipted','wallet-1','O-02','SUCCEEDED',0,2,'receipted-request',?)`).run(now);
      db.prepare(`INSERT INTO result(id,job_id,wallet_id,path,sha256,bytes,expires_at)
        VALUES('result-receipted','job-receipted','wallet-1',?,'b',7,?)`)
        .run(join(root, 'expired-result'), now - 1);
      db.prepare(`INSERT INTO delivery_attempt(id,result_id,wallet_id,created_at,expires_at)
        VALUES('attempt-receipted','result-receipted','wallet-1',?,?)`).run(now - 1000, now - 1);
      db.prepare(`INSERT INTO delivery_receipt(attempt_id,result_id,wallet_id,bytes,sha256,accepted_at)
        VALUES('attempt-receipted','result-receipted','wallet-1',7,'b',?)`).run(now - 500);

      expect(db.prepare('SELECT count(*) AS n FROM delivery_receipt WHERE result_id = ?').get('result-receipted'))
        .toEqual({ n: 1 });
      await sweepExpiredResources(db, undefined, now);

      expect(db.prepare('SELECT count(*) AS n FROM result WHERE id = ?').get('result-receipted'))
        .toEqual({ n: 0 });
      expect(db.prepare('SELECT count(*) AS n FROM delivery_receipt WHERE result_id = ?').get('result-receipted'))
        .toEqual({ n: 1 });
      expect(db.prepare('SELECT count(*) AS n FROM delivery_attempt WHERE id = ?').get('attempt-receipted'))
        .toEqual({ n: 1 });
    } finally {
      db.close();
    }
  });

  it('preserves delivery receipts while upgrading a v2 database', async () => {
    const root = join(process.cwd(), '..', '..', '..', '.kiln', 'scratch', 'plinth-server2');
    const filename = join(root, 'legacy-receipt.sqlite');
    let db: Database.Database | undefined;
    try {
      await mkdir(root, { recursive: true });
      db = createDatabase(filename);
      const now = Date.now();
      db.prepare('INSERT INTO wallet(id,balance,created_at) VALUES(?,?,?)')
        .run('wallet-legacy', 0, now);
      db.prepare(`INSERT INTO job(id,wallet_id,op,state,reserved,charged,client_request_id,created_at)
        VALUES('job-legacy','wallet-legacy','O-02','SUCCEEDED',0,2,'legacy-request',?)`).run(now);
      db.prepare(`INSERT INTO result(id,job_id,wallet_id,path,sha256,bytes,expires_at)
        VALUES('result-legacy','job-legacy','wallet-legacy','legacy-result','b',7,?)`).run(now - 1);
      db.close();
      db = undefined;

      const legacy = new Database(filename);
      legacy.pragma('foreign_keys = OFF');
      legacy.exec(`
        ALTER TABLE wallet DROP COLUMN unlocked_at;
        DROP TABLE delivery_receipt;
        DROP TABLE delivery_attempt;
        CREATE TABLE delivery_attempt(
          id TEXT PRIMARY KEY, result_id TEXT NOT NULL REFERENCES result(id),
          wallet_id TEXT NOT NULL, created_at INTEGER NOT NULL, expires_at INTEGER NOT NULL
        );
        CREATE TABLE delivery_receipt(
          id INTEGER PRIMARY KEY, attempt_id TEXT NOT NULL UNIQUE REFERENCES delivery_attempt(id),
          result_id TEXT NOT NULL, wallet_id TEXT NOT NULL, bytes INTEGER NOT NULL,
          sha256 TEXT NOT NULL, accepted_at INTEGER NOT NULL, UNIQUE(result_id, wallet_id)
        );
      `);
      legacy.prepare(`INSERT INTO delivery_attempt(id,result_id,wallet_id,created_at,expires_at)
        VALUES('attempt-legacy','result-legacy','wallet-legacy',?,?)`).run(now - 1000, now - 1);
      legacy.prepare(`INSERT INTO delivery_receipt(attempt_id,result_id,wallet_id,bytes,sha256,accepted_at)
        VALUES('attempt-legacy','result-legacy','wallet-legacy',7,'b',?)`).run(now - 500);
      legacy.pragma('user_version = 2');
      legacy.close();

      db = createDatabase(filename);
      expect(db.prepare('SELECT count(*) AS n FROM delivery_receipt').get()).toEqual({ n: 1 });
      expect(db.prepare('SELECT count(*) AS n FROM delivery_attempt').get()).toEqual({ n: 1 });
      const deliveryAttemptForeignKeys = db.pragma('foreign_key_list(delivery_attempt)') as
        Array<{ table: string }>;
      expect(deliveryAttemptForeignKeys.map((key) => key.table))
        .not.toContain('result');
      expect(db.pragma('foreign_key_check')).toEqual([]);
    } finally {
      db?.close();
      await rm(root, { recursive: true, force: true });
    }
  });

  it('returns a durable, expiring deletion receipt without claiming provider deletion', async () => {
    const db = createDatabase(':memory:');
    const now = Date.now();
    db.prepare(`INSERT INTO device(id,install_hash,created_at,integrity_state)
      VALUES('device-1','hash',?,'PLAY_RECOGNIZED')`).run(now);
    db.prepare(`INSERT INTO wallet(id,balance,created_at) VALUES('wallet-1',0,?)`).run(now);
    db.prepare('INSERT INTO device_wallet(device_id,wallet_id) VALUES(?,?)').run('device-1', 'wallet-1');
    db.prepare(`INSERT INTO job(id,wallet_id,op,state,reserved,provider_endpoint,client_request_id,created_at)
      VALUES('job-provider','wallet-1','O-02','SUCCEEDED',0,'fal/model','request',?)`).run(now);
    const app = await createApp({ db, config: loadConfig({ JWT_SECRET: 'x'.repeat(32) }), closeDatabaseOnExit: false });
    try {
      const token = app.jwt.sign({ sub: 'device-1', wid: 'wallet-1', gen: 0 });
      const deleted = await app.inject({
        method: 'DELETE', url: '/v1/data',
        headers: { authorization: `Bearer ${token}` },
        payload: { confirm: true },
      });
      expect(deleted.statusCode).toBe(202);
      const body = deleted.json() as {
        receiptToken: string; status: string; providerStatus: string; providerRetentionUntil: number;
      };
      expect(body.status).toBe('PENDING');
      expect(body.providerStatus).toBe('RETENTION_WINDOW');
      expect(body.providerRetentionUntil).toBeGreaterThan(now);
      const status = await app.inject({ method: 'GET', url: `/v1/data/deletions/${body.receiptToken}` });
      expect(status.json()).toMatchObject({
        status: 'PENDING', controlledSystemsComplete: true, providerStatus: 'RETENTION_WINDOW',
      });
      db.prepare('UPDATE data_deletion_receipt SET expires_at = ?')
        .run(Date.now() - 1);
      const expired = await app.inject({ method: 'GET', url: `/v1/data/deletions/${body.receiptToken}` });
      expect(expired.statusCode).toBe(404);
    } finally {
      await app.close();
    }
  }, 15_000);
});
