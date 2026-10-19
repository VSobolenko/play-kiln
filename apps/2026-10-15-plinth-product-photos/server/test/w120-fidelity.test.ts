import { createHash, randomUUID } from 'node:crypto';
import { rm } from 'node:fs/promises';
import { join } from 'node:path';
import sharp from 'sharp';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { loadConfig } from '../src/config.js';
import { CreditLedger } from '../src/ledger.js';
import { falImageInput } from '../src/providers/fal.js';
import { JobQueue } from '../src/queue.js';
import { createFalProvider } from '../src/registry.js';
import { testDatabase } from './helpers.js';

afterEach(() => {
  vi.unstubAllGlobals();
});

function config() {
  return loadConfig({ JWT_SECRET: 'x'.repeat(32), FAL_KEY: 'fixture-key' });
}

function falFetch(output: Buffer) {
  const requestId = 'fixture-request';
  const responseUrl = `https://queue.fal.run/fal-ai/fixture/requests/${requestId}`;
  const outputUrl = 'https://fal.media/fixture.png';
  const fetchMock = vi.fn<typeof fetch>(async (url, init) => {
    if (init?.method === 'POST') {
      return Response.json({
        request_id: requestId, status_url: `${responseUrl}/status`, response_url: responseUrl,
      });
    }
    if (url === `${responseUrl}/status`) {
      return Response.json({ request_id: requestId, status: 'COMPLETED' });
    }
    if (url === responseUrl) return Response.json({ image: { url: outputUrl } });
    if (url === outputUrl) return new Response(new Uint8Array(output));
    throw new Error(`Unexpected fixture request: ${String(url)}`);
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

async function pixels(width: number, height: number, values: number[]) {
  return sharp(Buffer.from(values), { raw: { width, height, channels: 3 } }).png().toBuffer();
}

describe('W120 provider image fidelity', () => {
  vi.setConfig({ testTimeout: 30_000 });

  it('keeps rejecting O-09 when canvas_size is missing', () => {
    expect(() => falImageInput('O-09', Buffer.from('photo'), {}))
      .toThrow('PROVIDER_CANVAS_SIZE_REQUIRED');
  });

  it('rejects O-09 original_image_size that disagrees with the source', async () => {
    const photo = await pixels(3, 2, Array(18).fill(80));
    const provider = createFalProvider(config());
    await expect(provider.run('O-09', { image: photo }, {
      canvas_size: [5, 4], original_image_size: [2, 2], original_image_location: [1, 1],
    })).rejects.toThrow('PROVIDER_ORIGINAL_IMAGE_SIZE_MISMATCH');
  });

  it('rejects O-09 source placement that extends outside the canvas', () => {
    expect(() => falImageInput('O-09', Buffer.from('photo'), {
      canvas_size: [5, 4], original_image_size: [3, 2], original_image_location: [3, 2],
    })).toThrow('PROVIDER_IMAGE_GEOMETRY_INVALID');
  });

  it('restores the O-09 source rectangle at the declared canvas location', async () => {
    const source = await pixels(2, 1, [10, 20, 30, 40, 50, 60]);
    const generated = await pixels(4, 3, Array(36).fill(200));
    falFetch(generated);
    const output = await createFalProvider(config()).run('O-09', { image: source }, {
      canvas_size: [4, 3], original_image_size: [2, 1], original_image_location: [1, 1],
    });
    const { data } = await sharp(output.bytes).removeAlpha().raw().toBuffer({ resolveWithObject: true });

    expect([...data.subarray((1 * 4 + 1) * 3, (1 * 4 + 1) * 3 + 6)])
      .toEqual([10, 20, 30, 40, 50, 60]);
  });

  it('binds the O-12 mask and retains Bria mask_url override protection', () => {
    const photo = Buffer.from('photo');
    const mask = Buffer.from('mask');
    expect(falImageInput('O-12', photo, {}, mask)).toMatchObject({
      image_url: `data:image/jpeg;base64,${photo.toString('base64')}`,
      mask_url: `data:image/png;base64,${mask.toString('base64')}`,
      mask_type: 'manual',
    });
    expect(() => falImageInput('O-12', photo, { mask_url: 'data:image/png;base64,caller' }, mask))
      .toThrow('PROVIDER_INPUT_OVERRIDE_FORBIDDEN');
  });

  it('erases white mask pixels and restores black mask pixels from the source', async () => {
    const sourceValues = [10, 20, 30, 40, 50, 60];
    const photo = await pixels(2, 1, sourceValues);
    const mask = await pixels(2, 1, [255, 255, 255, 0, 0, 0]);
    const generated = await pixels(2, 1, [200, 10, 10, 40, 50, 60]);
    falFetch(generated);

    const output = await createFalProvider(config()).run('O-12', { image: photo, mask }, {});
    const decoded = await sharp(output.bytes).removeAlpha().raw().toBuffer();

    expect([...decoded.subarray(0, 3)]).toEqual([200, 10, 10]);
    expect([...decoded.subarray(3, 6)]).toEqual(sourceValues.slice(3, 6));
  });

  it('fails the job instead of storing an O-12 result changed outside the mask', async () => {
    const db = testDatabase(5);
    const dataDir = join(process.cwd(), '.kiln', 'scratch', randomUUID());
    const sourceValues = [10, 20, 30, 40, 50, 60];
    const photo = await pixels(2, 1, sourceValues);
    const mask = await pixels(2, 1, [255, 255, 255, 0, 0, 0]);
    const changedOutsideMask = await pixels(2, 1, [200, 10, 10, 200, 10, 10]);
    falFetch(changedOutsideMask);
    const queue = new JobQueue(db, new CreditLedger(db), createFalProvider(config()), 1, dataDir);

    try {
      await queue.enqueue({
        jobId: 'job-fidelity-failed', walletId: 'wallet-1', op: 'O-12',
        clientRequestId: 'client-fidelity-failed', imageBase64: photo.toString('base64'),
        maskBase64: mask.toString('base64'), uploadIds: [], params: {}, credits: 2,
      });
      await queue.waitForIdle();

      expect(db.prepare('SELECT state, fail_reason FROM job WHERE id = ?').get('job-fidelity-failed'))
        .toEqual({ state: 'FAILED', fail_reason: 'FIDELITY_GUARD_FAILED' });
      expect(db.prepare('SELECT count(*) AS n FROM result WHERE job_id = ?').get('job-fidelity-failed'))
        .toEqual({ n: 0 });
      expect(db.prepare("SELECT count(*) AS n FROM ledger WHERE kind = 'RELEASE' AND ref = ?")
        .get('job-fidelity-failed')).toEqual({ n: 1 });
    } finally {
      db.close();
      await rm(dataDir, { recursive: true, force: true });
    }
  });
});
