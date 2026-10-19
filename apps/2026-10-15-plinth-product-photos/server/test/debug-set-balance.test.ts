import { afterEach, describe, expect, it } from 'vitest';
import { mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { spawnSync } from 'node:child_process';
import { createDatabase } from '../src/db.js';

const script = join(process.cwd(), 'scripts', 'debug-set-balance.mjs');
const tempDirs: string[] = [];

function createDebugDatabase(): { directory: string; databasePath: string } {
  const directory = mkdtempSync(join(tmpdir(), 'plinth-product-photos-debug-test-'));
  tempDirs.push(directory);
  const databasePath = join(directory, 'server.sqlite');
  const db = createDatabase(databasePath);
  const now = Date.now();
  db.prepare('INSERT INTO wallet(id, balance, refund_debt, purchase_hold, created_at) VALUES (?, ?, ?, ?, ?)')
    .run('recent-wallet', 2, 3, 4, now);
  db.prepare('INSERT INTO device(id, install_hash, integrity_state, created_at) VALUES (?, ?, ?, ?)')
    .run('recent-device', 'a'.repeat(64), 'DEV', now);
  db.prepare('INSERT INTO device_wallet(device_id, wallet_id) VALUES (?, ?)')
    .run('recent-device', 'recent-wallet');
  db.prepare(`INSERT INTO ledger(wallet_id, kind, delta, ref, idempotency_key, created_at)
    VALUES (?, 'STARTER', ?, ?, ?, ?)`).run('recent-wallet', 2, 'seed', 'seed', now);
  db.close();
  return { directory, databasePath };
}

function runTool(databasePath: string, ...args: string[]) {
  return spawnSync(process.execPath, [script, ...args], {
    env: { ...process.env, NODE_ENV: 'development', DATABASE_PATH: databasePath },
    encoding: 'utf8',
  });
}

afterEach(() => {
  for (const directory of tempDirs.splice(0)) rmSync(directory, { recursive: true, force: true });
});

describe('debug-set-balance', () => {
  it('requires the explicit debug acknowledgement', () => {
    const { databasePath } = createDebugDatabase();
    const result = runTool(databasePath, '--latest', '--balance', '10');

    expect(result.status).not.toBe(0);
    expect(result.stderr).toContain('EXPLICIT_DEBUG_FLAG_REQUIRED');
  });

  it('sets the latest active debug wallet balance and clears blocking holds', () => {
    const { databasePath } = createDebugDatabase();
    const result = runTool(databasePath, '--debug', '--latest', '--balance', '10');

    expect(result.status).toBe(0);
    expect(JSON.parse(result.stdout)).toMatchObject({
      walletId: 'recent-wallet',
      before: { balance: 2, refundDebt: 3, purchaseHold: 4 },
      after: { balance: 10, refundDebt: 0, purchaseHold: 0 },
      ledgerEntry: 'not-recorded-schema-does-not-allow-DEBUG_TOPUP',
    });

    const db = createDatabase(databasePath);
    expect(db.prepare('SELECT balance, refund_debt, purchase_hold FROM wallet WHERE id = ?').get('recent-wallet'))
      .toEqual({ balance: 10, refund_debt: 0, purchase_hold: 0 });
    db.close();
  });
});
