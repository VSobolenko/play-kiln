import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import type { FastifyInstance } from 'fastify';
import { createApp } from '../src/app.js';
import { loadConfig } from '../src/config.js';
import { createDatabase } from '../src/db.js';

describe('API error handler', () => {
  let app: FastifyInstance;

  beforeEach(async () => {
    app = await createApp({
      db: createDatabase(':memory:'),
      config: loadConfig({ JWT_SECRET: 'x'.repeat(32) }),
      provider: null,
    });
  });

  afterEach(async () => {
    await app?.close();
  });

  it.each(['/v1/integrity/challenge', '/v1/session'])(
    'returns 400 with the API envelope for malformed JSON at %s',
    async (url) => {
      const response = await app.inject({
        method: 'POST', url,
        headers: { 'content-type': 'application/json' },
        payload: '{',
      });
      console.log(`malformed JSON ${url}: HTTP=${response.statusCode} ${response.body}`);
      expect(response.statusCode).toBe(400);
      expect(response.json()).toEqual({
        error: {
          code: 'INVALID_REQUEST', message: 'The request could not be read.',
          retryAfterSec: null, details: {},
        },
      });
    },
  );

  it('preserves 415 for an unsupported media type with the API envelope', async () => {
    const response = await app.inject({
      method: 'POST', url: '/v1/integrity/challenge',
      headers: { 'content-type': 'application/xml' },
      payload: '<request/>',
    });
    expect(response.statusCode).toBe(415);
    expect(response.json()).toEqual({
      error: {
        code: 'INVALID_REQUEST', message: 'The request could not be read.',
        retryAfterSec: null, details: {},
      },
    });
  });

  it.each([400, 401, 403, 404, 409, 422, 429, 499])(
    'preserves a thrown client error status of %i without exposing internals',
    async (statusCode) => {
      app.get('/test/client-error', async () => {
        throw Object.assign(new Error('Private implementation detail'), { statusCode });
      });
      const response = await app.inject({ method: 'GET', url: '/test/client-error' });
      expect(response.statusCode).toBe(statusCode);
      expect(response.json()).toEqual({
        error: {
          code: 'INVALID_REQUEST', message: 'The request could not be read.',
          retryAfterSec: null, details: {},
        },
      });
    },
  );

  it('preserves 404 for an unknown route', async () => {
    const response = await app.inject({ method: 'GET', url: '/not-a-route' });
    expect(response.statusCode).toBe(404);
  });

  it.each([undefined, 500, 503, 302])(
    'returns 500 INTERNAL for a fault with statusCode %s without exposing internals',
    async (statusCode) => {
      app.get('/test/server-error', async () => {
        throw Object.assign(new Error('Private implementation detail'), { statusCode });
      });
      const response = await app.inject({ method: 'GET', url: '/test/server-error' });
      expect(response.statusCode).toBe(500);
      expect(response.json()).toEqual({
        error: {
          code: 'INTERNAL', message: 'The request could not be completed.',
          retryAfterSec: null, details: {},
        },
      });
    },
  );

  it('preserves the schema validation response', async () => {
    const response = await app.inject({
      method: 'POST', url: '/v1/integrity/challenge', payload: {},
    });
    expect(response.statusCode).toBe(400);
    expect(response.json()).toEqual({
      error: {
        code: 'INVALID_REQUEST', message: 'The request does not match the API schema.',
        retryAfterSec: null, details: {},
      },
    });
  });

  it('preserves the body limit response', async () => {
    const response = await app.inject({
      method: 'POST', url: '/v1/integrity/challenge',
      headers: { 'content-type': 'application/json' },
      payload: JSON.stringify('x'.repeat(5 * 1024 * 1024)),
    });
    expect(response.statusCode).toBe(413);
    expect(response.json()).toEqual({
      error: {
        code: 'TOO_LARGE', message: 'The request body exceeds the 5 MiB server limit.',
        retryAfterSec: null, details: {},
      },
    });
  });
});
