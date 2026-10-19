#!/usr/bin/env node
import Database from 'better-sqlite3';
import { resolve } from 'node:path';

function fail(code) {
  throw new Error(code);
}

function parseArgs(args) {
  let acknowledged = false;
  let latest = false;
  let walletId;
  let balance;

  for (let index = 0; index < args.length; index += 1) {
    const argument = args[index];
    if (argument === '--debug') acknowledged = true;
    else if (argument === '--latest') latest = true;
    else if (argument === '--wallet') walletId = args[++index];
    else if (argument === '--balance') balance = Number(args[++index]);
    else fail(`UNKNOWN_ARGUMENT:${argument}`);
  }

  if (!acknowledged) fail('EXPLICIT_DEBUG_FLAG_REQUIRED');
  if (process.env.NODE_ENV === 'production') fail('PRODUCTION_NODE_ENV_REFUSED');
  if (!Number.isSafeInteger(balance) || balance < 0) fail('VALID_NON_NEGATIVE_BALANCE_REQUIRED');
  if ((latest && walletId) || (!latest && !walletId)) fail('SELECT_EXACTLY_ONE_OF_LATEST_OR_WALLET');
  return { latest, walletId, balance };
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

function setBalance(db, walletId, balance) {
  return db.transaction(() => {
    const before = db.prepare(`
      SELECT balance, refund_debt AS refundDebt, purchase_hold AS purchaseHold
      FROM wallet WHERE id = ? AND deleted_at IS NULL
    `).get(walletId);
    if (!before) fail('ACTIVE_WALLET_NOT_FOUND');
    db.prepare(`
      UPDATE wallet SET balance = ?, refund_debt = 0, purchase_hold = 0
      WHERE id = ? AND deleted_at IS NULL
    `).run(balance, walletId);
    const after = db.prepare(`
      SELECT balance, refund_debt AS refundDebt, purchase_hold AS purchaseHold
      FROM wallet WHERE id = ?
    `).get(walletId);
    return { walletId, before, after, ledgerEntry: 'not-recorded-schema-does-not-allow-DEBUG_TOPUP' };
  }).immediate();
}

try {
  const { latest, walletId, balance } = parseArgs(process.argv.slice(2));
  const databasePath = resolve(process.env.DATABASE_PATH || 'data/server.sqlite');
  requireDebugDatabase(databasePath);
  const db = new Database(databasePath);
  db.pragma('foreign_keys = ON');
  db.pragma('busy_timeout = 5000');
  try {
    const target = latest ? latestWallet(db)?.id : walletId;
    if (!target) fail('ACTIVE_DEBUG_WALLET_NOT_FOUND');
    console.log(JSON.stringify(setBalance(db, target, balance)));
  } finally {
    db.close();
  }
} catch (error) {
  console.error(error instanceof Error ? error.message : String(error));
  process.exitCode = 1;
}
