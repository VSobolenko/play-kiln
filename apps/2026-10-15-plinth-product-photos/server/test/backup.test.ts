/**
 * The backup and restore drill: a snapshot taken while the database is open is
 * consistent, verifies, and can be opened and written after being copied to a
 * new path. This is the mechanism `docs/release-operations.md` tells the
 * operator to run; the test proves it on the same code path.
 */
import { cp, mkdir, readdir, rm } from 'node:fs/promises';
import { join } from 'node:path';
import Database from 'better-sqlite3';
import { afterEach, describe, expect, it } from 'vitest';
import { backupDatabase, verifyDatabaseFile } from '../src/backup.js';
import { createDatabase } from '../src/db.js';

const root = join(process.cwd(), '.kiln', 'scratch', 'backup-drill');
const directories: string[] = [];
const databases: Database.Database[] = [];

afterEach(async () => {
  for (const db of databases.splice(0)) {
    try {
      db.close();
    } catch {
      // Already closed by the test.
    }
  }
  for (const dir of directories.splice(0)) await rm(dir, { recursive: true, force: true });
});

async function workspace(): Promise<string> {
  const dir = join(root, `${Date.now()}-${Math.random().toString(36).slice(2)}`);
  directories.push(dir);
  await mkdir(dir, { recursive: true });
  return dir;
}

function seed(filenames: string): Database.Database {
  const db = createDatabase(filenames);
  databases.push(db);
  db.prepare('INSERT INTO wallet(id, balance, created_at) VALUES (?, ?, ?)')
    .run('wallet-backup', 7, Date.now());
  db.prepare(`INSERT INTO ledger(wallet_id, kind, delta, ref, idempotency_key, created_at)
    VALUES ('wallet-backup', 'STARTER', 7, 'seed', 'seed', ?)`).run(Date.now());
  return db;
}

describe('verified SQLite backup', () => {
  it('snapshots an open WAL database consistently and leaves no partial file', async () => {
    const dir = await workspace();
    const livePath = join(dir, 'live', 'server.sqlite');
    await mkdir(join(dir, 'live'), { recursive: true });
    const db = seed(livePath);

    const destination = join(dir, 'backups', 'plinth-1.sqlite');
    const report = await backupDatabase(db, destination);

    expect(report.integrity).toBe('ok');
    expect(report.path).toBe(destination);
    expect(report.wallets).toBe(1);
    expect(report.ledgerEntries).toBe(1);
    expect(report.bytes).toBeGreaterThan(0);
    expect(await readdir(join(dir, 'backups'))).toEqual(['plinth-1.sqlite']);

    const verified = verifyDatabaseFile(destination);
    expect(verified).toMatchObject({ integrity: 'ok', wallets: 1, ledgerEntries: 1 });
    expect(verified.tables).toBeGreaterThan(10);
    expect(verified.schemaVersion).toBeGreaterThan(0);

    // The live database is still writable after the online backup.
    db.prepare(`INSERT INTO ledger(wallet_id, kind, delta, ref, idempotency_key, created_at)
      VALUES ('wallet-backup', 'PURCHASE', 5, 'after', 'after', ?)`).run(Date.now());
    expect((db.prepare('SELECT count(*) AS n FROM ledger').get() as { n: number }).n).toBe(2);
  });

  it('keeps earlier snapshots unchanged when a newer one is taken', async () => {
    const dir = await workspace();
    await mkdir(join(dir, 'live'), { recursive: true });
    const db = seed(join(dir, 'live', 'server.sqlite'));
    const first = join(dir, 'backups', 'plinth-1.sqlite');
    const second = join(dir, 'backups', 'plinth-2.sqlite');

    await backupDatabase(db, first);
    db.prepare(`INSERT INTO ledger(wallet_id, kind, delta, ref, idempotency_key, created_at)
      VALUES ('wallet-backup', 'PURCHASE', 5, 'newer', 'newer', ?)`).run(Date.now());
    await backupDatabase(db, second);

    expect(verifyDatabaseFile(first).ledgerEntries).toBe(1);
    expect(verifyDatabaseFile(second).ledgerEntries).toBe(2);
  });

  it('restores from the snapshot into a new file that accepts writes', async () => {
    const dir = await workspace();
    await mkdir(join(dir, 'live'), { recursive: true });
    const db = seed(join(dir, 'live', 'server.sqlite'));
    const backup = join(dir, 'backups', 'plinth.sqlite');
    await backupDatabase(db, backup);

    // The drill's restore step is a copy; the point is that the file is
    // self-contained, not a WAL pair, so a single file is enough to restore.
    const restoredPath = join(dir, 'restored', 'server.sqlite');
    await mkdir(join(dir, 'restored'), { recursive: true });
    await cp(backup, restoredPath);

    const restored = createDatabase(restoredPath);
    databases.push(restored);
    expect(restored.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-backup'))
      .toEqual({ balance: 7 });
    expect(restored.prepare('SELECT count(*) AS n FROM ledger').get()).toEqual({ n: 1 });
    restored.prepare('UPDATE wallet SET balance = 6 WHERE id = ?').run('wallet-backup');
    expect(restored.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-backup'))
      .toEqual({ balance: 6 });
  });
});
