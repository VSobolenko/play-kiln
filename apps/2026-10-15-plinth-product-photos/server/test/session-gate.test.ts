import { afterEach, describe, expect, it, vi } from 'vitest';
import { generateKeyPairSync } from 'node:crypto';
import { createApp } from '../src/app.js';
import { loadConfig } from '../src/config.js';
import { createDatabase } from '../src/db.js';
import { createChallenge, createIntegritySession, sessionRequestHash, type SessionInput } from '../src/integrity.js';
import {
  PlayRecallIntegrityVerifier, StarterDeviceRecall, createSessionIntegrityVerifier, devIntegrityBanner,
} from '../src/device-recall.js';

const apps: Awaited<ReturnType<typeof createApp>>[] = [];
afterEach(async () => {
  for (const app of apps.splice(0)) await app.close();
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});

async function setup(env: NodeJS.ProcessEnv = { PLINTH_DEV_INTEGRITY: '1' }) {
  const db = createDatabase(':memory:');
  const config = loadConfig({ JWT_SECRET: 'test-only-session-secret-at-least-32-chars', ...env });
  const app = await createApp({ db, config, provider: null });
  apps.push(app);
  return { app, db };
}

async function challenge(app: Awaited<ReturnType<typeof createApp>>, installId = 'install-a') {
  const fields = { installId, appVersion: '0.1.0', locale: 'en-US' };
  const response = await app.inject({ method: 'POST', url: '/v1/integrity/challenge', payload: fields });
  expect(response.statusCode).toBe(200);
  const { challengeId, challenge: nonce } = response.json();
  const input: SessionInput = { ...fields, challengeId, challenge: nonce, integrityToken: '' };
  input.integrityToken = `dev:${sessionRequestHash(input)}`;
  return input;
}

describe('session gate', () => {
  it('refuses the explicit development flag in production at startup', () => {
    expect(() => loadConfig({ NODE_ENV: 'production', PLINTH_DEV_INTEGRITY: '1' }))
      .toThrow('PLINTH_DEV_INTEGRITY cannot be enabled in production');
    console.info('production startup refused: PLINTH_DEV_INTEGRITY cannot be enabled in production');
  });

  it('also rejects programmatic development config when the running process is production', () => {
    const config = loadConfig({ JWT_SECRET: 'test-only-session-secret-at-least-32-chars',
      PLINTH_DEV_INTEGRITY: '1' });
    vi.stubEnv('NODE_ENV', 'production');
    expect(() => createSessionIntegrityVerifier(config, () => {}))
      .toThrow('PLINTH_DEV_INTEGRITY cannot be enabled in production');
  });

  it('announces exactly which Google checks it simulates', () => {
    const warn = vi.fn();
    createSessionIntegrityVerifier(loadConfig({ JWT_SECRET: 'test-only-session-secret-at-least-32-chars',
      PLINTH_DEV_INTEGRITY: '1' }), warn);
    expect(warn).toHaveBeenCalledExactlyOnceWith(devIntegrityBanner);
  });

  it.each(['0', 'true', 'yes', ''])('does not opt in with PLINTH_DEV_INTEGRITY=%s', async (flag) => {
    const { app } = await setup({ PLINTH_DEV_INTEGRITY: flag });
    const response = await app.inject({ method: 'POST', url: '/v1/integrity/challenge',
      payload: { installId: 'a', appVersion: '1', locale: 'en-US' } });
    expect(response.statusCode).toBe(503);
  });

  it('keeps the unconfigured production path closed with the flag absent', async () => {
    const { app } = await setup({ NODE_ENV: 'production' });
    const fields = { installId: 'install-a', appVersion: '0.1.0', locale: 'en-US' };
    const response = await app.inject({ method: 'POST', url: '/v1/integrity/challenge', payload: fields });
    expect(response.statusCode).toBe(503);
    expect(response.json().error.code).toBe('NOT_CONFIGURED');
    const session = await app.inject({ method: 'POST', url: '/v1/session', payload: {
      ...fields, challengeId: '00000000-0000-4000-8000-000000000000',
      challenge: 'unused', integrityToken: 'unused',
    } });
    expect(session.statusCode).toBe(503);
    expect(session.json().error.code).toBe('NOT_CONFIGURED');
    console.info('flag absent: challenge/session HTTP 503 NOT_CONFIGURED');
  });

  describe('Play Device Recall transport and grant', () => {
    const identity = { packageName: 'com.gexetr.plinthproductphotos', versionCodes: ['1'],
      certificateSha256: 'A'.repeat(64), starterCredits: 10, starterDailyBudgetMicroUsd: 10_000_000 };
    const privateKey = generateKeyPairSync('rsa', { modulusLength: 2048 }).privateKey
      .export({ type: 'pkcs8', format: 'pem' }).toString();
    const credentials = JSON.stringify({ client_email: 'test@example.invalid', private_key: privateKey });

    it.each(['false', 'true', 'absent', 'write-failure', 'core', 'device'])(
      'handles recall %s using a single decode and an acknowledged bit-1 write', async (kind) => {
        const db = createDatabase(':memory:');
        try {
          const nonce = createChallenge(db, 'install', '1.0', 'en-US');
          const input = { challengeId: nonce.challengeId, challenge: nonce.challenge,
            installId: 'install', appVersion: '1.0', locale: 'en-US', integrityToken: 'test-attestation' };
          const warn = vi.fn();
          let wroteBit = false;
          const fetchMock = vi.fn(async (url: string | URL | Request, init?: RequestInit) => {
            if (url === 'https://oauth2.googleapis.com/token') {
              return Response.json({ access_token: 'test-oauth', expires_in: 3600 });
            }
            if (String(url).endsWith(':decodeIntegrityToken')) {
              return Response.json({ tokenPayloadExternal: {
                requestDetails: { requestHash: sessionRequestHash(input), requestPackageName: identity.packageName },
                appIntegrity: { versionCode: '1', certificateSha256Digest: [Buffer.from('A'.repeat(64), 'hex').toString('base64url')],
                  appRecognitionVerdict: kind === 'core' ? 'UNEVALUATED' : 'PLAY_RECOGNIZED' },
                accountDetails: { appLicensingVerdict: 'LICENSED' },
                deviceIntegrity: {
                  deviceRecognitionVerdict: kind === 'device' ? [] : ['MEETS_DEVICE_INTEGRITY'],
                  ...(kind === 'absent' ? {} : { deviceRecall: { values: { bitFirst: kind === 'true' } } }),
                },
              } });
            }
            if (kind === 'write-failure') return new Response(null, { status: 403 });
            wroteBit = true;
            return Response.json({});
          });
          vi.stubGlobal('fetch', fetchMock);
          const verifier = new PlayRecallIntegrityVerifier(credentials, identity.packageName, warn);
          const result = await createIntegritySession(db, input, verifier, verifier.recall, identity);
          expect(result.balance).toBe(kind === 'false' ? 10 : 0);
          expect(result.starterGranted).toBe(kind === 'false');
          expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get(result.walletId))
            .toEqual({ balance: kind === 'false' ? 10 : 0 });
          expect(fetchMock.mock.calls.filter(([url]) => String(url).endsWith(':decodeIntegrityToken'))).toHaveLength(1);
          const writes = fetchMock.mock.calls.filter(([url]) => String(url).endsWith('/deviceRecall:write'));
          expect(writes).toHaveLength(kind === 'false' || kind === 'write-failure' ? 1 : 0);
          if (writes[0]) {
            expect(String(writes[0][0])).toBe(
              `https://playintegrity.googleapis.com/v1/${identity.packageName}/deviceRecall:write`,
            );
            expect(JSON.parse(String(writes[0][1]?.body))).toEqual({
              integrityToken: input.integrityToken, newValues: { bitFirst: true },
            });
          }
          expect(wroteBit).toBe(kind === 'false');
          expect(warn).toHaveBeenCalledTimes(kind === 'absent' || kind === 'write-failure' ? 1 : 0);
          if (kind === 'false') console.info('mocked Play recall false -> write acknowledged -> wallet balance 10');
        } finally {
          db.close();
        }
      },
    );

    it('never writes an unavailable or already-claimed bit', async () => {
      const write = vi.fn();
      const warn = vi.fn();
      const recall = new StarterDeviceRecall(write, warn);
      const input: SessionInput = { challengeId: 'unused', challenge: 'unused', installId: 'a',
        appVersion: '1', locale: 'en-US', integrityToken: 'unused' };
      expect(await recall.claimStarter(input, {})).toBeNull();
      expect(await recall.claimStarter(input, { recallBitFirst: true })).toBe(false);
      expect(write).not.toHaveBeenCalled();
      expect(warn).toHaveBeenCalledOnce();
    });
  });

  it('creates a local session with ten credits and rejects replay', async () => {
    const { app, db } = await setup();
    const address = await app.listen({ host: '127.0.0.1', port: 0 });
    expect((await fetch(`${address}/health`)).status).toBe(200);
    const fields = { installId: 'install-live-http', appVersion: '0.1.0', locale: 'en-US' };
    const challengeResponse = await fetch(`${address}/v1/integrity/challenge`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(fields),
    });
    expect(challengeResponse.status).toBe(200);
    const { challengeId, challenge: nonce } = await challengeResponse.json();
    const input: SessionInput = { ...fields, challengeId, challenge: nonce, integrityToken: '' };
    input.integrityToken = `dev:${sessionRequestHash(input)}`;
    const response = await fetch(`${address}/v1/session`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(input),
    });
    expect(response.status).toBe(200);
    const body = await response.json();
    expect(body.balance).toBe(10);
    expect(body.starterGranted).toBe(true);
    expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get(body.walletId)).toEqual({ balance: 10 });
    expect(typeof body.deviceToken).toBe('string');
    const wallet = await fetch(`${address}/v1/wallet`, {
      headers: { Authorization: `Bearer ${body.deviceToken}` },
    });
    expect(wallet.status).toBe(200);
    expect((await wallet.json()).balance).toBe(10);
    // Never print bearer credentials, even in local test evidence.
    console.info('local session HTTP 200', JSON.stringify({ ...body, deviceToken: '[REDACTED]' }));
    const replay = await app.inject({ method: 'POST', url: '/v1/session', payload: input });
    expect(replay.statusCode).toBe(409);
    expect(replay.json().error.code).toBe('INTEGRITY_INVALID');
  });

  it('grants once per install through the shared recall path, including concurrent sessions', async () => {
    const { app, db } = await setup();
    const inputs = await Promise.all([challenge(app), challenge(app)]);
    const responses = await Promise.all(inputs.map((input) =>
      app.inject({ method: 'POST', url: '/v1/session', payload: input })));
    expect(responses.map((r) => r.statusCode)).toEqual([200, 200]);
    expect(responses.map((r) => r.json().balance).sort()).toEqual([0, 10]);
    expect(db.prepare('SELECT count(*) AS n FROM starter_grant').get()).toEqual({ n: 1 });
    const later = await app.inject({ method: 'POST', url: '/v1/session', payload: await challenge(app) });
    expect(later.json().starterGranted).toBe(false);
  });

  it('enforces the UTC starter liability budget before granting', async () => {
    const { app, db } = await setup({ PLINTH_DEV_INTEGRITY: '1', STARTER_DAILY_BUDGET_USD: '0.25' });
    const first = await app.inject({ method: 'POST', url: '/v1/session', payload: await challenge(app, 'a') });
    const second = await app.inject({ method: 'POST', url: '/v1/session', payload: await challenge(app, 'b') });
    expect(first.json().balance).toBe(10);
    expect(second.json().balance).toBe(0);
    expect(db.prepare('SELECT starter_cost_micro_usd FROM spend_day').get())
      .toEqual({ starter_cost_micro_usd: 250_000 });
  });

  it.each(['challenge', 'installId', 'appVersion', 'locale', 'expired', 'missing', 'hash', 'garbage'])(
    'does not bypass %s validation locally', async (kind) => {
      const { app, db } = await setup();
      const input = await challenge(app);
      if (kind === 'challenge') input.challenge += 'changed';
      if (kind === 'installId') input.installId = 'other-install';
      if (kind === 'appVersion') input.appVersion = '9.0';
      if (kind === 'locale') input.locale = 'ru-RU';
      if (kind === 'expired') db.prepare('UPDATE integrity_challenge SET expires_at = 1').run();
      if (kind === 'missing') input.challengeId = '00000000-0000-4000-8000-000000000000';
      if (kind === 'hash') input.integrityToken = `dev:${'a'.repeat(43)}`;
      if (kind === 'garbage') input.integrityToken = 'garbage';
      const response = await app.inject({ method: 'POST', url: '/v1/session', payload: input });
      expect(response.statusCode).toBe(400);
      expect(response.json().error.code).toBe('INTEGRITY_INVALID');
      expect(db.prepare('SELECT count(*) AS n FROM wallet').get()).toEqual({ n: 0 });
    },
  );
});
