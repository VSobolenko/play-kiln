#!/usr/bin/env node
// DEBUG-ONLY: set or clear the wallet's unlock entitlement so a device run can
// exercise both the free and the paid export path without a live Play purchase.
// The production grant path is `PurchaseService.applyUnlock`, covered by the
// server suite; this script only moves `unlocked_at` on a debug database and
// refuses everything else, mirroring `debug-set-balance.mjs`.
import Database from 'better-sqlite3';
import { resolve } from 'node:path';

function fail(code) {
  throw new Error(code);
}

function parseArgs(args) {
  let acknowledged = false;
  let latest = false;
  let walletId;
  let deviceToken;
  let unlocked;

  for (let index = 0; index < args.length; index += 1) {
    const argument = args[index];
    if (argument === '--debug') acknowledged = true;
    else if (argument === '--latest') latest = true;
    else if (argument === '--wallet') walletId = args[++index];
    else if (argument === '--device-token') deviceToken = args[++index];
    else if (argument === '--unlocked') unlocked = args[++index];
    else fail(`UNKNOWN_ARGUMENT:${argument}`);
  }

  if (!acknowledged) fail('EXPLICIT_DEBUG_FLAG_REQUIRED');
  if (process.env.NODE_ENV === 'production') fail('PRODUCTION_NODE_ENV_REFUSED');
  if (unlocked !== 'true' && unlocked !== 'false') fail('UNLOCKED_MUST_BE_TRUE_OR_FALSE');
  const selectors = [latest, Boolean(walletId), Boolean(deviceToken)].filter(Boolean).length;
  if (selectors !== 1) fail('SELECT_EXACTLY_ONE_OF_LATEST_WALLET_OR_DEVICE_TOKEN');
  return { latest, walletId, deviceToken, unlocked: unlocked === 'true' };
}

/**
 * The wallet a real install is using, read from the device token the app already holds.
 *
 * `--latest` is a guess: it orders by recent ledger/job/device activity, which answers
 * "which wallet did this server see last", not "which wallet is this phone using". A test
 * that unlocks the wrong wallet then reads an unchanged watermark and reports a broken
 * paywall, so a device run selects by token instead. The token is a JWT whose payload
 * carries the wallet id in `wid`.
 */
function walletFromDeviceToken(deviceToken) {
  const payload = deviceToken.split('.')[1];
  if (!payload) fail('DEVICE_TOKEN_IS_NOT_A_JWT');
  let claims;
  try {
    claims = JSON.parse(Buffer.from(payload, 'base64url').toString('utf8'));
  } catch {
    fail('DEVICE_TOKEN_PAYLOAD_IS_NOT_JSON');
  }
  if (typeof claims?.wid !== 'string' || claims.wid.length === 0) {
    fail('DEVICE_TOKEN_HAS_NO_WALLET_CLAIM');
  }
  return claims.wid;
}

function requireDebugDatabase(databasePath) {
  if (!databasePath.includes('plinth-product-photos-debug')) fail('DEBUG_DATABASE_PATH_REQUIRED');
}

function latestWallet(db) {
  return db.prepare(`
    SELECT dw.wallet_id AS id
    FROM device_wallet dw
    JOIN wallet w ON w.id = dw.wallet_id
    JOIN device d ON d.id = dw.device_id
    LEFT JOIN ledger l ON l.wallet_id = w.id
    LEFT JOIN job j ON j.wallet_id = w.id
    WHERE w.deleted_at IS NULL AND d.deleted_at IS NULL
    GROUP BY dw.wallet_id
    ORDER BY MAX(COALESCE(l.created_at, j.created_at, d.created_at)) DESC, w.created_at DESC
    LIMIT 1
  `).get();
}

function setUnlock(db, walletId, unlocked) {
  return db.transaction(() => {
    const before = db.prepare(`
      SELECT unlocked_at AS unlockedAt FROM wallet WHERE id = ? AND deleted_at IS NULL
    `).get(walletId);
    if (!before) fail('ACTIVE_WALLET_NOT_FOUND');
    const value = unlocked ? Date.now() : null;
    db.prepare(`
      UPDATE wallet SET unlocked_at = ? WHERE id = ? AND deleted_at IS NULL
    `).run(value, walletId);
    const after = db.prepare(`
      SELECT unlocked_at AS unlockedAt FROM wallet WHERE id = ?
    `).get(walletId);
    return {
      walletId,
      before: { unlocked: before.unlockedAt !== null },
      after: { unlocked: after.unlockedAt !== null },
      ledgerEntry: 'not-recorded-entitlement-grants-no-credits',
    };
  }).immediate();
}

try {
  const { latest, walletId, deviceToken, unlocked } = parseArgs(process.argv.slice(2));
  const databasePath = resolve(process.env.DATABASE_PATH || 'data/server.sqlite');
  requireDebugDatabase(databasePath);
  const db = new Database(databasePath);
  db.pragma('foreign_keys = ON');
  db.pragma('busy_timeout = 5000');
  try {
    const target = latest
      ? latestWallet(db)?.id
      : walletId ?? walletFromDeviceToken(deviceToken);
    if (!target) fail('ACTIVE_DEBUG_WALLET_NOT_FOUND');
    console.log(JSON.stringify(setUnlock(db, target, unlocked)));
  } finally {
    db.close();
  }
} catch (error) {
  console.error(error instanceof Error ? error.message : String(error));
  process.exitCode = 1;
}
