import { createHash, randomUUID } from 'node:crypto';
import { mkdir, rm, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { createApp } from '../src/app.js';
import { loadConfig } from '../src/config.js';
import { MODEL_REGISTRY, type OperationId, type ProviderAdapter } from '../src/registry.js';
import { testDatabase } from './helpers.js';

const enabledImageTools: OperationId[] = ['O-02', 'O-07', 'O-08', 'O-10'];

async function fixture() {
  const root = join(process.cwd(), '.kiln', 'scratch', `studio-binding-${randomUUID()}`);
  await mkdir(root, { recursive: true });
  const db = testDatabase(20);
  const observed: Array<{ op: OperationId; image: Buffer | undefined }> = [];
  const provider: ProviderAdapter = {
    async run(op, inputs) {
      observed.push({ op, image: inputs.image });
      const bytes = Buffer.from(`result-${op}`);
      return { bytes, sha256: createHash('sha256').update(bytes).digest('hex'),
        width: 512, height: 512, format: 'image/png', provenance: 'ENHANCED' };
    },
  };
  const app = await createApp({
    db,
    config: loadConfig({ JWT_SECRET: 'x'.repeat(32), DATA_DIR: root, KILL_SWITCH_FILE: join(root, 'kill') }),
    provider,
  });
  const now = Date.now();
  db.prepare(`INSERT INTO device(id,install_hash,created_at,integrity_state)
    VALUES('studio-device','studio-install',?,'PLAY_RECOGNIZED')`).run(now);
  db.prepare(`INSERT INTO device_wallet(device_id,wallet_id) VALUES('studio-device','wallet-1')`).run();
  // Insert the unrelated image first: a query-order or "latest photo" fallback must not pass.
  for (const [id, bytes] of [
    ['unrelated-upload', Buffer.from('car-not-selected')],
    ['selected-upload', Buffer.from('selected-black-phone')],
  ] as const) {
    const path = join(root, id);
    await writeFile(path, bytes);
    db.prepare(`INSERT INTO upload(id,wallet_id,path,sha256,bytes,created_at,expires_at)
      VALUES(?,?,?,?,?,?,?)`).run(id, 'wallet-1', path,
      createHash('sha256').update(bytes).digest('hex'), bytes.length, now, now + 60_000);
  }
  const token = app.jwt.sign({ sub: 'studio-device', wid: 'wallet-1', gen: 0 });
  return {
    app, db, root, observed,
    submit(op: OperationId, uploadIds = ['selected-upload']) {
      return app.inject({
        method: 'POST', url: '/v1/jobs', headers: { authorization: `Bearer ${token}` },
        payload: { clientRequestId: randomUUID(), op, uploadIds, params: {},
          estimateCredits: MODEL_REGISTRY[op].credits },
      });
    },
    async close() {
      await app.close();
      await rm(root, { recursive: true, force: true });
    },
  };
}

describe('Studio unavailable selected upload never falls back to another image', () => {
  it.each(enabledImageTools.flatMap((op) =>
    ['missing', 'expired', 'deleted', 'foreign-wallet'].map((reason) => ({ op, reason })),
  ))('$op rejects a $reason selected upload before reserving or invoking a provider', async ({ op, reason }) => {
    const f = await fixture();
    try {
      if (reason === 'expired') {
        f.db.prepare("UPDATE upload SET expires_at = ? WHERE id = 'selected-upload'").run(Date.now() - 1);
      } else if (reason === 'deleted') {
        f.db.prepare("UPDATE upload SET deleted_at = ? WHERE id = 'selected-upload'").run(Date.now());
      } else if (reason === 'foreign-wallet') {
        f.db.prepare("INSERT INTO wallet(id,balance,created_at) VALUES('foreign-wallet',0,?)").run(Date.now());
        f.db.prepare("UPDATE upload SET wallet_id = 'foreign-wallet' WHERE id = 'selected-upload'").run();
      }
      const response = await f.submit(op, [reason === 'missing' ? 'missing-selected-upload' : 'selected-upload']);
      expect(response.statusCode).toBe(400);
      expect(response.json().error.code).toBe('UPLOAD_NOT_AVAILABLE');
      expect(f.observed).toEqual([]);
      expect(f.db.prepare('SELECT count(*) AS n FROM job').get()).toEqual({ n: 0 });
      expect(f.db.prepare("SELECT count(*) AS n FROM ledger WHERE kind = 'RESERVE'").get()).toEqual({ n: 0 });
    } finally {
      await f.close();
    }
  });

  it('rejects expansion with missing canvas geometry as invalid input', async () => {
    const f = await fixture();
    try {
      const response = await f.submit('O-09');
      expect(response.statusCode).toBe(400);
      expect(response.json().error.code).toBe('INVALID_OPERATION_INPUT');
      expect(f.observed).toEqual([]);
    } finally {
      await f.close();
    }
  });
});
