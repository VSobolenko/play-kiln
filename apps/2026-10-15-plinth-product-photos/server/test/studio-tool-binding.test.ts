import { createHash, randomUUID } from 'node:crypto';
import { rm } from 'node:fs/promises';
import { join } from 'node:path';
import sharp from 'sharp';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { createApp } from '../src/app.js';
import { loadConfig } from '../src/config.js';
import { createFalProvider, MODEL_REGISTRY, type OperationId } from '../src/registry.js';
import { testDatabase } from './helpers.js';

let app: Awaited<ReturnType<typeof createApp>> | undefined;
let dataDir: string | undefined;

afterEach(async () => {
  if (app) await app.close();
  app = undefined;
  if (dataDir) await rm(dataDir, { recursive: true, force: true });
  dataDir = undefined;
  vi.unstubAllGlobals();
});

async function setup() {
  dataDir = join(process.cwd(), '.kiln', 'scratch', 'studio-tool-binding-w73', randomUUID());
  const config = loadConfig({
    JWT_SECRET: 'x'.repeat(32), FAL_KEY: 'fixture-key', DATA_DIR: dataDir,
  });
  const db = testDatabase(20);
  db.prepare(`INSERT INTO device(id,install_hash,created_at,integrity_state)
    VALUES('device-1','install-1',?,'PLAY_RECOGNIZED')`).run(Date.now());
  db.prepare(`INSERT INTO device_wallet(device_id,wallet_id) VALUES('device-1','wallet-1')`).run();
  app = await createApp({ db, config, provider: createFalProvider(config) });
  const headers = { authorization: `Bearer ${app.jwt.sign({ sub: 'device-1', wid: 'wallet-1', gen: 0 })}` };
  const selected = await sharp({
    create: { width: 3, height: 2, channels: 3, background: '#111111' },
  }).jpeg().toBuffer();
  const unrelated = await sharp({
    create: { width: 3, height: 2, channels: 3, background: '#ffffff' },
  }).jpeg().toBuffer();
  async function upload(bytes: Buffer) {
    const response = await app!.inject({
      method: 'PUT', url: '/v1/uploads',
      headers: { ...headers, 'content-type': 'image/jpeg',
        'x-client-sha256': createHash('sha256').update(bytes).digest('hex') },
      payload: bytes,
    });
    expect(response.statusCode).toBe(201);
    return response.json().uploadId as string;
  }
  // A pre-existing and a newer upload must not substitute for the selected one.
  const olderId = await upload(unrelated);
  const selectedId = await upload(selected);
  const newerId = await upload(unrelated);
  return { db, headers, selected, selectedId, olderId, newerId };
}

describe('selected Studio image through upload -> job -> queue -> fal', () => {
  // Each case builds a real app, a SQLite DB and runs sharp normalisation; under a loaded full-suite
  // run that measured 5.5 s against the 5 s default; alone it is ~0.9 s.
  vi.setConfig({ testTimeout: 30_000 });

  it.each(['O-02', 'O-07', 'O-08', 'O-10'] satisfies OperationId[])(
    '%s sends only the requested upload bytes, not another library upload', async (op) => {
      const { db, headers, selected, selectedId } = await setup();
      const generated = await sharp({
        create: { width: 3, height: 2, channels: 3, background: '#333333' },
      }).jpeg().toBuffer();
      const requestId = 'fixture-request';
      const responseUrl = `https://queue.fal.run/fal-ai/fixture/requests/${requestId}`;
      const outputUrl = 'https://fal.media/fixture.jpg';
      const fetchMock = vi.fn<typeof fetch>(async (url, init) => {
        if (init?.method === 'POST') return Response.json({
          request_id: requestId, status_url: `${responseUrl}/status`, response_url: responseUrl,
        });
        if (url === `${responseUrl}/status`) return Response.json({ request_id: requestId, status: 'COMPLETED' });
        if (url === responseUrl) return Response.json(op === 'O-02'
          ? { image: { url: outputUrl } } : { images: [{ url: outputUrl }] });
        if (url === outputUrl) return new Response(new Uint8Array(generated));
        throw new Error(`Unexpected fixture request: ${String(url)}`);
      });
      vi.stubGlobal('fetch', fetchMock);
      const accepted = await app!.inject({
        method: 'POST', url: '/v1/jobs', headers,
        payload: { clientRequestId: randomUUID(), op, uploadIds: [selectedId],
          params: {}, estimateCredits: MODEL_REGISTRY[op].credits },
      });
      expect(accepted.statusCode).toBe(202);
      const jobId = accepted.json().jobId as string;
      await vi.waitFor(() => {
        expect(db.prepare('SELECT state FROM job WHERE id = ?').get(jobId)).toEqual({ state: 'SUCCEEDED' });
      });
      expect(db.prepare('SELECT upload_id FROM job_upload WHERE job_id = ?').all(jobId))
        .toEqual([{ upload_id: selectedId }]);
      const [url, init] = fetchMock.mock.calls[0]!;
      expect(url).toBe(`https://queue.fal.run/${MODEL_REGISTRY[op].endpointId}`);
      const body = JSON.parse(String(init?.body));
      expect(body.image_url).toBe(`data:image/jpeg;base64,${selected.toString('base64')}`);
      if (op === 'O-02') expect(body).toEqual({ image_url: body.image_url });
      else expect(body.prompt).toContain('Never invent an unrelated subject');
      expect(fetchMock.mock.calls.filter(([, options]) => options?.method === 'POST')).toHaveLength(1);
    },
  );

  it.each(['O-02', 'O-07', 'O-08', 'O-10'] satisfies OperationId[])(
    '%s rejects ambiguous multiple-image input before reserving or calling fal', async (op) => {
      const { db, headers, selectedId, newerId } = await setup();
      const fetchMock = vi.fn<typeof fetch>();
      vi.stubGlobal('fetch', fetchMock);
      const response = await app!.inject({
        method: 'POST', url: '/v1/jobs', headers,
        payload: { clientRequestId: randomUUID(), op, uploadIds: [newerId, selectedId],
          params: {}, estimateCredits: MODEL_REGISTRY[op].credits },
      });
      expect(response.statusCode).toBe(400);
      expect(response.json().error.code).toBe('INVALID_OPERATION_INPUT');
      expect(db.prepare('SELECT count(*) AS n FROM job').get()).toEqual({ n: 0 });
      expect(fetchMock).not.toHaveBeenCalled();
    },
  );
});
