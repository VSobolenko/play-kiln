import { afterEach, describe, expect, it, vi } from 'vitest';
import { createHash } from 'node:crypto';
import sharp from 'sharp';
import { createApp } from '../src/app.js';
import { loadConfig } from '../src/config.js';
import { createDatabase } from '../src/db.js';
import { createFakeProvider } from '../src/registry.js';

const databases: ReturnType<typeof createDatabase>[] = [];
const apps: Awaited<ReturnType<typeof createApp>>[] = [];

afterEach(async () => {
  for (const app of apps.splice(0)) await app.close();
  for (const db of databases.splice(0)) db.close();
});

const validAnswer = {
  category: 'shoes',
  confidence: 0.94,
  attributes: ['leather', 'lace-up'],
  visibleText: ['42'],
  scaleHint: { kind: 'label', valueMm: 270 },
  sku: 'SH-42',
};

async function setup(responses: unknown[], falKey = 'test-server-fal-key') {
  const db = createDatabase(':memory:');
  databases.push(db);
  const now = Date.now();
  db.prepare(`INSERT INTO device(id,install_hash,created_at,integrity_state)
    VALUES('device-1','install-1',?,'PLAY_RECOGNIZED')`).run(now);
  db.prepare('INSERT INTO wallet(id,balance,created_at) VALUES(?,?,?)').run('wallet-1', 10, now);
  db.prepare('INSERT INTO device_wallet(device_id,wallet_id) VALUES(?,?)').run('device-1', 'wallet-1');

  const provider = createFakeProvider({ recognitionResponses: responses });
  const recognize = vi.spyOn(provider, 'recognize');
  const config = loadConfig({
    JWT_SECRET: 'x'.repeat(32),
    ...(falKey ? { FAL_KEY: falKey } : {}),
  });
  const app = await createApp({ db, config, provider, closeDatabaseOnExit: false });
  apps.push(app);
  const token = app.jwt.sign({ sub: 'device-1', wid: 'wallet-1', gen: 0 });
  const image = await sharp({
    create: { width: 1024, height: 1024, channels: 3, background: '#ffffff' },
  }).jpeg().toBuffer();
  const response = await app.inject({
    method: 'POST',
    url: '/v1/recognize',
    headers: {
      authorization: `Bearer ${token}`,
      'content-type': 'image/jpeg',
      'x-client-sha256': createHash('sha256').update(image).digest('hex'),
    },
    payload: image,
  });
  return { app, db, provider: { ...provider, recognize }, response };
}

describe('O-01 item recognition', () => {
  it('returns a valid parsed answer from the fake provider', async () => {
    const { provider, response } = await setup([validAnswer]);

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual(validAnswer);
    expect(provider.recognize).toHaveBeenCalledTimes(1);
    expect(provider.recognize.mock.calls[0]?.[1]).toMatchObject({ temperature: 0 });
  });

  it('retries one schema-invalid answer and accepts the next valid answer', async () => {
    const { provider, response } = await setup([{ ...validAnswer, confidence: 2 }, validAnswer]);

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual(validAnswer);
    expect(provider.recognize).toHaveBeenCalledTimes(2);
  });

  it('returns the other fallback after two schema-invalid answers', async () => {
    const { provider, response } = await setup([{ ...validAnswer, confidence: 2 }, { category: 'bag' }]);

    expect(response.statusCode).toBe(200);
    expect(response.json()).toMatchObject({ category: 'other', confidence: 0 });
    expect(provider.recognize).toHaveBeenCalledTimes(2);
  });

  it('retries a category outside the closed set and falls back if still invalid', async () => {
    const { provider, response } = await setup([
      { ...validAnswer, category: 'unknown' },
      { ...validAnswer, category: 'unknown' },
    ]);

    expect(response.statusCode).toBe(200);
    expect(response.json()).toMatchObject({ category: 'other', confidence: 0 });
    expect(provider.recognize).toHaveBeenCalledTimes(2);
  });

  it('does not reserve or charge credits while recognizing', async () => {
    const { db, response } = await setup([validAnswer]);

    expect(response.statusCode).toBe(200);
    expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-1')).toEqual({ balance: 10 });
    expect(db.prepare('SELECT count(*) AS count FROM ledger').get()).toEqual({ count: 0 });
    expect(db.prepare('SELECT count(*) AS count FROM job').get()).toEqual({ count: 0 });
  });

  it('does not require a client-supplied key and refuses when the server key is absent', async () => {
    const { provider, response } = await setup([validAnswer], '');

    expect(response.statusCode).toBe(503);
    expect(response.json().error).toMatchObject({ code: 'NOT_CONFIGURED' });
    expect(provider.recognize).not.toHaveBeenCalled();
  });
});
