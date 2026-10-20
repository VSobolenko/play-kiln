import assert from 'node:assert/strict';
import { fileURLToPath } from 'node:url';
import { resolve, dirname } from 'node:path';
import { pathToFileURL } from 'node:url';

// Negative contract reproduction only: no credentials, provider calls or image results.
const slot = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..', '..', '..', '..', '..', '..');
const { createFalProvider } = await import(pathToFileURL(resolve(slot, 'server', 'src', 'registry.ts')).href);
const originalFetch = globalThis.fetch;
const calls = [];
try {
  globalThis.fetch = async (url) => {
    calls.push(String(url));
    return new Response(JSON.stringify({
      status: 'IN_QUEUE',
      request_id: 'documented-queue-acknowledgement',
      response_url: 'https://queue.fal.run/result-not-fetched',
    }), { status: 200 });
  };
  const provider = createFalProvider({ falKey: 'test-only-not-a-secret' });
  for (const op of ['O-07', 'O-08', 'O-09', 'O-10', 'O-12', 'O-14']) {
    await assert.rejects(
      () => provider.run(op, { image: Buffer.from([0xff, 0xd8, 0xff, 0xd9]) }, { prompt: 'contract test' }),
      { message: 'PROVIDER_RESPONSE_INVALID' },
    );
    assert.equal(calls.length, 1, 'Adapter did not poll or retrieve the queue result');
    assert.ok(calls[0].startsWith('https://queue.fal.run/'));
    calls.length = 0;
  }
  console.log('REPRODUCED: all six generation adapters reject a provider queue acknowledgement; none fetches its result.');
  const recognition = await provider.recognize(
    Buffer.from([0xff, 0xd8, 0xff, 0xd9]), { responseSchema: {} },
  );
  assert.equal(recognition.status, 'IN_QUEUE');
  assert.equal(recognition.category, undefined);
  console.log('REPRODUCED: recognition returns the queue acknowledgement, not item-recognition JSON.');

  globalThis.fetch = async () => new Response(JSON.stringify({
    image: { url: 'https://example.invalid/never-downloaded.png' },
  }), { status: 200 });
  for (const op of ['O-09', 'O-12', 'O-14']) {
    await assert.rejects(
      () => provider.run(op, { image: Buffer.from([0xff, 0xd8, 0xff, 0xd9]) }, {}),
      { message: 'PROVIDER_RESPONSE_INVALID' },
    );
  }
  console.log('REPRODUCED: expand, erase and try-on single-image result objects are rejected by the adapter.');
} finally {
  globalThis.fetch = originalFetch;
}
