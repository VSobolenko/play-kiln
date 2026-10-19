import { afterEach, describe, expect, it } from 'vitest';
import { mkdirSync, rmSync } from 'node:fs';
import { join } from 'node:path';
import { spawnSync } from 'node:child_process';
import { createDatabase } from '../src/db.js';

const script = join(process.cwd(), 'scripts', 'debug-set-unlock.mjs');
// Inside the repository, never the system temp directory: the factory's temporary
// files belong to `.kiln/scratch` or a build directory, not to the owner's machine.
// The name carries `plinth-product-photos-debug` because the tool refuses any database
// path that does not mark itself as the debug database.
const scratchRoot = join(process.cwd(), 'build', 'plinth-product-photos-debug-test');
const directories: string[] = [];

function debugDatabasePath(name: string): string {
  const directory = join(scratchRoot, name);
  mkdirSync(directory, { recursive: true });
  directories.push(directory);
  return join(directory, 'server.sqlite');
}

/**
 * Two wallets that `--latest` and `--device-token` disagree about on purpose.
 *
 * `busy-wallet` carries the newest ledger entry, so `--latest` selects it. `phone-wallet`
 * has no activity at all and is the one the device token names. A tool that resolves the
 * token must therefore NOT pick the wallet `--latest` would, which is exactly the failure
 * a device run hit when it unlocked the wrong wallet and read the watermark as a defect.
 */
function createTwoWalletDatabase(databasePath: string): void {
  const db = createDatabase(databasePath);
  const now = Date.now();
  db.prepare('INSERT INTO wallet(id, balance, refund_debt, purchase_hold, created_at) VALUES (?, ?, ?, ?, ?)')
    .run('busy-wallet', 10, 0, 0, now - 60_000);
  db.prepare('INSERT INTO wallet(id, balance, refund_debt, purchase_hold, created_at) VALUES (?, ?, ?, ?, ?)')
    .run('phone-wallet', 1, 0, 0, now - 120_000);
  db.prepare('INSERT INTO device(id, install_hash, integrity_state, created_at) VALUES (?, ?, ?, ?)')
    .run('busy-device', 'a'.repeat(64), 'DEV', now - 60_000);
  db.prepare('INSERT INTO device_wallet(device_id, wallet_id) VALUES (?, ?)')
    .run('busy-device', 'busy-wallet');
  db.prepare(`INSERT INTO ledger(wallet_id, kind, delta, ref, idempotency_key, created_at)
    VALUES (?, 'STARTER', ?, ?, ?, ?)`).run('busy-wallet', 10, 'seed', 'seed', now - 1_000);
  db.close();
}

function deviceToken(walletId: string): string {
  const header = Buffer.from(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).toString('base64url');
  const payload = Buffer.from(JSON.stringify({ sub: 'device', wid: walletId, gen: 0 })).toString('base64url');
  return `${header}.${payload}.signature-not-verified-by-this-tool`;
}

function runTool(databasePath: string, ...args: string[]) {
  return spawnSync(process.execPath, [script, ...args], {
    env: { ...process.env, NODE_ENV: 'development', DATABASE_PATH: databasePath },
    encoding: 'utf8',
  });
}

function unlockState(databasePath: string, walletId: string): boolean {
  const db = createDatabase(databasePath);
  const row = db.prepare('SELECT unlocked_at FROM wallet WHERE id = ?').get(walletId) as
    { unlocked_at: number | null } | undefined;
  db.close();
  return row !== undefined && row.unlocked_at !== null;
}

afterEach(() => {
  for (const directory of directories.splice(0)) rmSync(directory, { recursive: true, force: true });
});

describe('debug-set-unlock', () => {
  it('requires the explicit debug acknowledgement', () => {
    const databasePath = debugDatabasePath('ack');
    createTwoWalletDatabase(databasePath);

    const result = runTool(databasePath, '--latest', '--unlocked', 'true');

    expect(result.status).not.toBe(0);
    expect(result.stderr).toContain('EXPLICIT_DEBUG_FLAG_REQUIRED');
  });

  it('refuses a non-debug database path', () => {
    const result = runTool(join(process.cwd(), 'build', 'production.sqlite'), '--debug', '--latest', '--unlocked', 'true');

    expect(result.status).not.toBe(0);
    expect(result.stderr).toContain('DEBUG_DATABASE_PATH_REQUIRED');
  });

  it('refuses two selectors at once', () => {
    const databasePath = debugDatabasePath('selectors');
    createTwoWalletDatabase(databasePath);

    const result = runTool(databasePath, '--debug', '--latest', '--wallet', 'phone-wallet', '--unlocked', 'true');

    expect(result.status).not.toBe(0);
    expect(result.stderr).toContain('SELECT_EXACTLY_ONE_OF_LATEST_WALLET_OR_DEVICE_TOKEN');
  });

  it('unlocks the wallet the device token names, not the one --latest would pick', () => {
    const databasePath = debugDatabasePath('token');
    createTwoWalletDatabase(databasePath);

    const result = runTool(databasePath, '--debug', '--device-token', deviceToken('phone-wallet'), '--unlocked', 'true');

    expect(result.status).toBe(0);
    expect(JSON.parse(result.stdout)).toMatchObject({
      walletId: 'phone-wallet',
      before: { unlocked: false },
      after: { unlocked: true },
      ledgerEntry: 'not-recorded-entitlement-grants-no-credits',
    });
    expect(unlockState(databasePath, 'phone-wallet')).toBe(true);
    // The control: the wallet `--latest` would have chosen is untouched.
    expect(unlockState(databasePath, 'busy-wallet')).toBe(false);
  });

  it('clears the unlock again without recording a ledger entry', () => {
    const databasePath = debugDatabasePath('revoke');
    createTwoWalletDatabase(databasePath);
    runTool(databasePath, '--debug', '--device-token', deviceToken('phone-wallet'), '--unlocked', 'true');

    const result = runTool(databasePath, '--debug', '--device-token', deviceToken('phone-wallet'), '--unlocked', 'false');

    expect(result.status).toBe(0);
    expect(JSON.parse(result.stdout)).toMatchObject({ walletId: 'phone-wallet', after: { unlocked: false } });
    expect(unlockState(databasePath, 'phone-wallet')).toBe(false);

    const db = createDatabase(databasePath);
    const entries = db.prepare('SELECT count(*) AS n FROM ledger WHERE wallet_id = ?').get('phone-wallet') as
      { n: number };
    db.close();
    expect(entries.n).toBe(0);
  });

  it('rejects a token with no wallet claim instead of falling back to --latest', () => {
    const databasePath = debugDatabasePath('no-wid');
    createTwoWalletDatabase(databasePath);
    const header = Buffer.from(JSON.stringify({ alg: 'HS256' })).toString('base64url');
    const payload = Buffer.from(JSON.stringify({ sub: 'device' })).toString('base64url');

    const result = runTool(databasePath, '--debug', '--device-token', `${header}.${payload}.sig`, '--unlocked', 'true');

    expect(result.status).not.toBe(0);
    expect(result.stderr).toContain('DEVICE_TOKEN_HAS_NO_WALLET_CLAIM');
    expect(unlockState(databasePath, 'busy-wallet')).toBe(false);
  });
});
