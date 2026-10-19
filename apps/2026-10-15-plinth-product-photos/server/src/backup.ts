/**
 * Consistent, verified SQLite backups.
 *
 * The database runs in WAL mode and is written continuously, so copying the
 * `.sqlite` file with `cp` produces a file that may miss the WAL content — a
 * backup that looks fine and is silently stale. `better-sqlite3`'s `backup()`
 * uses SQLite's own online backup API instead, which takes a read lock for the
 * duration of each step and produces a transactionally consistent snapshot
 * while writers continue.
 *
 * Every backup is verified before it replaces the previous good copy:
 * `integrity_check` on a separate read-only connection, plus the table and row
 * counts an operator would use to decide the restore is usable. The write goes
 * to `<destination>.partial` and is renamed only after verification, so a
 * failed backup can never be mistaken for a successful one by its name.
 */

import Database from 'better-sqlite3';
import { mkdir, rename, rm, stat } from 'node:fs/promises';
import { dirname } from 'node:path';

export interface BackupVerification {
  integrity: string;
  tables: number;
  wallets: number;
  ledgerEntries: number;
  schemaVersion: number;
}

export interface BackupReport extends BackupVerification {
  path: string;
  bytes: number;
  createdAt: number;
}

/**
 * Reads a database file as a client would after a restore. Read-only, so a
 * verification can never repair or mutate the file it is judging.
 */
export function verifyDatabaseFile(path: string): BackupVerification {
  const db = new Database(path, { readonly: true, fileMustExist: true });
  try {
    const integrity = String(db.pragma('integrity_check', { simple: true }));
    const schemaVersion = Number(db.pragma('user_version', { simple: true }));
    const tables = (db.prepare(`SELECT count(*) AS n FROM sqlite_master WHERE type = 'table'`)
      .get() as { n: number }).n;
    // A Plinth database without these tables is not a Plinth backup; failing
    // here is better than restoring an empty file over a working one.
    const wallets = (db.prepare('SELECT count(*) AS n FROM wallet').get() as { n: number }).n;
    const ledgerEntries = (db.prepare('SELECT count(*) AS n FROM ledger').get() as { n: number }).n;
    return { integrity, tables, wallets, ledgerEntries, schemaVersion };
  } finally {
    db.close();
  }
}

/**
 * Writes a verified snapshot to `destination`, atomically.
 *
 * `db` must be the live connection: the online backup API reads through it, so
 * uncommitted WAL frames are included exactly as a committed reader would see
 * them. The caller keeps serving; nothing is locked for the whole copy.
 */
/**
 * Turns a WAL snapshot into one self-contained file.
 *
 * The online backup API copies the source's journal mode, so the result is
 * itself a WAL database: its newest committed frames can live in a `-wal`
 * sidecar, and a restore that copies only the `.sqlite` file silently loses
 * them. Checkpointing and switching to rollback-journal mode folds every frame
 * back into the main file and deletes the sidecars, so one file is the whole
 * backup.
 */
function sealSingleFile(path: string): void {
  const snapshot = new Database(path);
  try {
    snapshot.pragma('wal_checkpoint(TRUNCATE)');
    snapshot.pragma('journal_mode = DELETE');
    snapshot.pragma('optimize');
  } finally {
    snapshot.close();
  }
}

export async function backupDatabase(
  db: Database.Database,
  destination: string,
): Promise<BackupReport> {
  await mkdir(dirname(destination), { recursive: true });
  const partial = `${destination}.partial`;
  await rm(partial, { force: true });
  try {
    await db.backup(partial);
    /**
     * The snapshot inherits WAL mode from the source, and a WAL database is not
     * one file: a `cp` of it alone can miss the newest committed frames. Sealing
     * it into rollback-journal mode makes the snapshot self-contained, so the
     * restore step is a single copy — which is what the runbook tells an
     * operator to do under pressure.
     */
    sealSingleFile(partial);
    const verification = verifyDatabaseFile(partial);
    if (verification.integrity !== 'ok') {
      throw new Error(`BACKUP_INTEGRITY_FAILED: ${verification.integrity}`);
    }
    await rename(partial, destination);
    const info = await stat(destination);
    return {
      path: destination,
      bytes: info.size,
      createdAt: Date.now(),
      ...verification,
    };
  } catch (error) {
    await rm(partial, { force: true });
    await rm(`${partial}-wal`, { force: true });
    await rm(`${partial}-shm`, { force: true });
    throw error;
  }
}
