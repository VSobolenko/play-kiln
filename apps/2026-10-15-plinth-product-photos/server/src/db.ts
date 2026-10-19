import Database from 'better-sqlite3';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const migrations = [
  readFileSync(new URL('../migrations/001_init.sql', import.meta.url), 'utf8'),
  readFileSync(new URL('../migrations/002_integrity_purchase_deletion.sql', import.meta.url), 'utf8'),
  readFileSync(new URL('../migrations/003_durable_delivery_receipts.sql', import.meta.url), 'utf8'),
  readFileSync(new URL('../migrations/004_unlock_entitlement.sql', import.meta.url), 'utf8'),
];

export function createDatabase(filename: string): Database.Database {
  const db = new Database(filename);
  db.pragma('foreign_keys = ON');
  db.pragma('busy_timeout = 5000');
  if (filename !== ':memory:') db.pragma('journal_mode = WAL');

  let version = db.pragma('user_version', { simple: true }) as number;
  while (version < migrations.length) {
    db.exec('BEGIN IMMEDIATE');
    try {
      const migration = migrations[version];
      if (!migration) throw new Error(`Missing database migration ${version + 1}`);
      db.exec(migration);
      version += 1;
      db.pragma(`user_version = ${version}`);
      db.exec('COMMIT');
    } catch (error) {
      db.exec('ROLLBACK');
      db.close();
      throw error;
    }
  }
  return db;
}

export function migrationPath(): string {
  return fileURLToPath(new URL('../migrations/001_init.sql', import.meta.url));
}
