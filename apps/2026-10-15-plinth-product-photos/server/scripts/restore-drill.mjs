#!/usr/bin/env node
/**
 * Restore drill: prove that the backup used by `deploy/backup.sh` is one
 * self-contained file that reads back with every committed row.
 *
 *   npm run build
 *   npm run drill:restore
 *
 * The drill writes only under `.kiln/scratch/infra/restore-drill/`, never the
 * system temporary directory, and leaves the sealed snapshot there for a
 * human to inspect. It also takes a blind copy of the live WAL database and
 * shows what that copy reads, which is the failure the seal prevents.
 *
 * Exit codes: 0 verified, 1 setup/import failure, 2 verification mismatch.
 */
import { copyFileSync, existsSync, mkdirSync, rmSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import Database from 'better-sqlite3';

function fail(message) {
  console.error(`restore-drill: ${message}`);
  process.exit(1);
}

let backupDatabase;
let verifyDatabaseFile;
try {
  ({ backupDatabase, verifyDatabaseFile } = await import('../dist/src/backup.js'));
} catch {
  fail('dist/src/backup.js is missing — run `npm run build` in server/ first');
}

// server/scripts -> server -> slot -> apps -> repository root
const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../../..');
const work = join(repoRoot, '.kiln', 'scratch', 'infra', 'restore-drill');
rmSync(work, { recursive: true, force: true });
mkdirSync(work, { recursive: true });

const source = join(work, 'source.sqlite');
const destination = join(work, 'backup.sqlite');
const blind = join(work, 'blind-copy.sqlite');

const db = new Database(source);
db.pragma('journal_mode = WAL');
db.pragma('wal_autocheckpoint = 0');
db.exec(`
  CREATE TABLE wallet(id TEXT PRIMARY KEY, balance INTEGER NOT NULL);
  CREATE TABLE ledger(id INTEGER PRIMARY KEY, wallet_id TEXT NOT NULL, delta INTEGER NOT NULL);
`);
const insertWallet = db.prepare('INSERT INTO wallet(id, balance) VALUES (?, ?)');
const insertLedger = db.prepare('INSERT INTO ledger(wallet_id, delta) VALUES (?, ?)');
db.transaction(() => {
  insertWallet.run('w-1', 9);
  insertWallet.run('w-2', 4);
  for (let index = 0; index < 25; index += 1) insertLedger.run('w-1', -1);
})();

const live = {
  wallets: 2,
  ledger: 25,
  balance: 13,
};

// A plain file copy of a WAL database that has not been checkpointed is not a
// backup. Read it the way restore.sh would read a single installed file.
copyFileSync(source, blind);
let blindError = null;
try {
  const copy = new Database(blind, { readonly: true, fileMustExist: true });
  try {
    copy.prepare('SELECT count(*) AS n FROM wallet').get();
  } finally {
    copy.close();
  }
} catch (error) {
  blindError = error instanceof Error ? error.message : String(error);
}
for (const suffix of ['', '-wal', '-shm']) rmSync(`${blind}${suffix}`, { force: true });

let report;
try {
  report = await backupDatabase(db, destination);
} finally {
  db.close();
}

const restored = new Database(destination, { readonly: true, fileMustExist: true });
let restoredRows;
let journalMode;
try {
  journalMode = restored.pragma('journal_mode', { simple: true });
  restoredRows = {
    wallets: restored.prepare('SELECT count(*) AS n FROM wallet').get().n,
    ledger: restored.prepare('SELECT count(*) AS n FROM ledger').get().n,
    balance: restored.prepare('SELECT sum(balance) AS n FROM wallet').get().n,
  };
} finally {
  restored.close();
}

const sealedIsSingleFile = !existsSync(`${destination}-wal`) && !existsSync(`${destination}-shm`);
const matchesLive = JSON.stringify(restoredRows) === JSON.stringify(live);

console.log(JSON.stringify({
  live,
  blindCopyError: blindError,
  backup: report,
  journalModeAfterSeal: journalMode,
  sealedIsSingleFile,
  restoredRows,
  matchesLive,
  backupPath: destination,
}, null, 2));

if (report.integrity !== 'ok' || !sealedIsSingleFile || !matchesLive || journalMode !== 'delete') {
  console.error('restore-drill: verification failed');
  process.exit(2);
}
