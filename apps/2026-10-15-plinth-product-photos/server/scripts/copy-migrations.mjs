import { cpSync, mkdirSync, readdirSync } from 'node:fs';

// Copy every migration the server reads, discovered from the directory rather than
// from a hand-written list: a new migration that is not named here would be absent
// from dist/ and make `db.ts` fail on startup (its readFileSync resolves
// ../migrations/<name>.sql relative to dist/src/db.js). The list form is what
// dropped 003_durable_delivery_receipts.sql once already.
const migrationsDir = new URL('../migrations/', import.meta.url);
const outDir = new URL('../dist/migrations/', import.meta.url);

mkdirSync(outDir, { recursive: true });

const migrationNames = readdirSync(migrationsDir)
  .filter((name) => name.endsWith('.sql'))
  .sort();

for (const name of migrationNames) {
  cpSync(new URL(name, migrationsDir), new URL(name, outDir));
}

console.log(`copied ${migrationNames.length} migration(s): ${migrationNames.join(', ')}`);
