#!/usr/bin/env node
/**
 * Operator CLI for the database backup described in
 * `apps/2026-10-15-plinth-product-photos/docs/release-operations.md`.
 *
 *   node scripts/backup-db.mjs                     # snapshot to backups/plinth-<stamp>.sqlite
 *   node scripts/backup-db.mjs --out <dir>         # snapshot to another directory
 *   node scripts/backup-db.mjs --verify <file>     # read a backup back and print its counts
 *   node scripts/backup-db.mjs --keep 14           # delete snapshots older than the newest 14
 *
 * The backup logic lives in `src/backup.ts` and is imported from `dist/`, so
 * this script and the test suite cannot drift. Run `npm run build` first; the
 * script says so if `dist/` is missing.
 */
import { existsSync, readdirSync, rmSync, statSync } from 'node:fs';
import { resolve } from 'node:path';
import Database from 'better-sqlite3';

function fail(message) {
  console.error(`backup-db: ${message}`);
  process.exit(1);
}

function option(name, fallback) {
  const index = process.argv.indexOf(name);
  if (index === -1) return fallback;
  const value = process.argv[index + 1];
  if (value === undefined || value.startsWith('--')) fail(`${name} requires a value`);
  return value;
}

let backupDatabase;
let verifyDatabaseFile;
try {
  ({ backupDatabase, verifyDatabaseFile } = await import('../dist/src/backup.js'));
} catch {
  fail('dist/src/backup.js is missing — run `npm run build` in server/ first');
}

const verifyTarget = option('--verify', null);
if (verifyTarget !== null) {
  const target = resolve(verifyTarget);
  if (!existsSync(target)) fail(`no such backup: ${target}`);
  const report = verifyDatabaseFile(target);
  console.log(JSON.stringify({ path: target, ...report }, null, 2));
  if (report.integrity !== 'ok') process.exit(2);
  process.exit(0);
}

const databasePath = resolve(process.env.DATABASE_PATH ?? 'data/server.sqlite');
if (!existsSync(databasePath)) {
  fail(`database not found: ${databasePath} (set DATABASE_PATH or run from the server directory)`);
}

const outDir = resolve(option('--out', process.env.BACKUP_DIR ?? 'backups'));
const stamp = new Date().toISOString().replaceAll(':', '-').replaceAll('.', '-');
const destination = resolve(outDir, `plinth-${stamp}.sqlite`);

const db = new Database(databasePath);
let report;
try {
  report = await backupDatabase(db, destination);
} finally {
  db.close();
}

const keep = Number(option('--keep', '0'));
if (Number.isSafeInteger(keep) && keep > 0) {
  const snapshots = readdirSync(outDir)
    .filter((name) => name.startsWith('plinth-') && name.endsWith('.sqlite'))
    .map((name) => ({ name, mtime: statSync(resolve(outDir, name)).mtimeMs }))
    .sort((a, b) => b.mtime - a.mtime);
  for (const stale of snapshots.slice(keep)) {
    rmSync(resolve(outDir, stale.name), { force: true });
  }
}

// Prints counts only: a path, a size and totals. Never a wallet id, a token or a row.
console.log(JSON.stringify(report, null, 2));
