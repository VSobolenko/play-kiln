import { createHash, randomUUID } from 'node:crypto';
import { mkdir, rm, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import sharp from 'sharp';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { createApp } from '../src/app.js';
import { loadConfig } from '../src/config.js';
import { MODEL_REGISTRY, type ProviderAdapter } from '../src/registry.js';
import { testDatabase } from './helpers.js';

describe('W120 O-12 upload binding', () => {
  vi.setConfig({ testTimeout: 30_000 });

  it('requires exactly two uploads and passes the second upload to the provider as the mask', async () => {
    const root = join(process.cwd(), '.kiln', 'scratch', `w120-erase-${randomUUID()}`);
    await mkdir(root, { recursive: true });
    const db = testDatabase(20);
    const source = await sharp({
      create: { width: 2, height: 2, channels: 3, background: '#123456' },
    }).png().toBuffer();
    const mask = await sharp({
      create: { width: 2, height: 2, channels: 3, background: '#ffffff' },
    }).png().toBuffer();
    const mismatchedMask = await sharp({
      create: { width: 1, height: 1, channels: 3, background: '#000000' },
    }).png().toBuffer();
    const observed: Array<{ image?: Buffer; mask?: Buffer }> = [];
    const provider: ProviderAdapter = {
      async run(_op, inputs) {
        observed.push({ image: inputs.image, mask: inputs.mask });
        return {
          bytes: source,
          sha256: createHash('sha256').update(source).digest('hex'),
          provenance: 'ENHANCED',
          width: 2,
          height: 2,
          format: 'image/png',
        };
      },
    };
    const app = await createApp({
      db,
      config: loadConfig({
        JWT_SECRET: 'x'.repeat(32), DATA_DIR: root, KILL_SWITCH_FILE: join(root, 'kill'),
      }),
      provider,
    });
    try {
      const now = Date.now();
      db.prepare(`INSERT INTO device(id,install_hash,created_at,integrity_state)
        VALUES('w120-device','w120-install',?,'PLAY_RECOGNIZED')`).run(now);
      db.prepare("INSERT INTO device_wallet(device_id,wallet_id) VALUES('w120-device','wallet-1')").run();
      const ids: string[] = [];
      for (const [id, bytes] of [['w120-photo', source], ['w120-mask', mask]] as const) {
        const path = join(root, id);
        await writeFile(path, bytes);
        db.prepare(`INSERT INTO upload(id,wallet_id,path,sha256,bytes,created_at,expires_at)
          VALUES(?,?,?,?,?,?,?)`).run(id, 'wallet-1', path,
          createHash('sha256').update(bytes).digest('hex'), bytes.length, now, now + 60_000);
        ids.push(id);
      }
      const mismatchedPath = join(root, 'w120-mismatched-mask');
      await writeFile(mismatchedPath, mismatchedMask);
      db.prepare(`INSERT INTO upload(id,wallet_id,path,sha256,bytes,created_at,expires_at)
        VALUES(?,?,?,?,?,?,?)`).run('w120-mismatched-mask', 'wallet-1', mismatchedPath,
        createHash('sha256').update(mismatchedMask).digest('hex'), mismatchedMask.length, now, now + 60_000);
      const headers = {
        authorization: `Bearer ${app.jwt.sign({ sub: 'w120-device', wid: 'wallet-1', gen: 0 })}`,
      };
      const submit = (uploadIds: string[]) => app.inject({
        method: 'POST',
        url: '/v1/jobs',
        headers,
        payload: {
          clientRequestId: randomUUID(), op: 'O-12', uploadIds, params: {},
          estimateCredits: MODEL_REGISTRY['O-12'].credits,
        },
      });

      const one = await submit([ids[0]!]);
      expect(one.statusCode).toBe(400);
      expect(one.json().error.code).toBe('INVALID_OPERATION_INPUT');
      expect(observed).toEqual([]);

      const wrongSize = await submit([ids[0]!, 'w120-mismatched-mask']);
      expect(wrongSize.statusCode).toBe(400);
      expect(wrongSize.json().error.code).toBe('INVALID_OPERATION_INPUT');
      expect(observed).toEqual([]);

      const two = await submit(ids);
      expect(two.statusCode).toBe(202);
      await vi.waitFor(() => expect(observed).toHaveLength(1));
      await vi.waitFor(() => {
        const state = db.prepare('SELECT state FROM job WHERE id = ?').get(two.json().jobId) as
          { state: string } | undefined;
        expect(['SUCCEEDED', 'FAILED']).toContain(state?.state);
      });
      expect(observed[0]).toEqual({ image: source, mask: expect.any(Buffer) });
      expect(await sharp(observed[0]!.mask!).metadata()).toMatchObject({ width: 2, height: 2, format: 'png' });
      expect((db.prepare('SELECT upload_id FROM job_upload WHERE job_id = ?').all(two.json().jobId) as
        Array<{ upload_id: string }>).map(({ upload_id }) => upload_id).sort()).toEqual([...ids].sort());
    } finally {
      vi.unstubAllGlobals();
      await app.close();
      db.close();
      await rm(root, { recursive: true, force: true });
    }
  });
});
