import { afterEach, describe, expect, it } from 'vitest';
import { createHash } from 'node:crypto';
import { createDatabase } from '../src/db.js';
import {
  createChallenge,
  createIntegritySession,
  sessionRequestHash,
  type DeviceRecall,
  type IntegrityPayload,
  type IntegrityVerifier,
} from '../src/integrity.js';

const databases: ReturnType<typeof createDatabase>[] = [];
afterEach(() => {
  for (const db of databases.splice(0)) db.close();
});

function database() {
  const db = createDatabase(':memory:');
  databases.push(db);
  return db;
}

const config = {
  packageName: 'com.gexetr.plinthproductphotos',
  versionCode: '1',
  certificateSha256: 'A'.repeat(64),
  starterCredits: 10,
};

function setup(db = database(), installId = 'install-a') {
  const challenge = createChallenge(db, installId, '1.0', 'en-US', Date.now());
  const input = { ...challenge, installId, appVersion: '1.0', locale: 'en-US', integrityToken: 'token-a' };
  const verifier: IntegrityVerifier = {
    async decode() {
      return {
        requestHash: sessionRequestHash(input),
        packageName: config.packageName,
        versionCode: config.versionCode,
        certificateSha256: [config.certificateSha256],
        appRecognitionVerdict: 'PLAY_RECOGNIZED',
        appLicensingVerdict: 'LICENSED',
        deviceRecognitionVerdicts: ['MEETS_DEVICE_INTEGRITY'],
      };
    },
  };
  return { db, input, verifier };
}

describe('Play Integrity session', () => {
  it('uses the canonical UTF-8 field order and binds locale', () => {
    const { input } = setup();
    const canonical = [
      'session', input.challengeId, input.challenge, input.installId, input.appVersion, input.locale,
    ].join('\n');
    expect(sessionRequestHash(input)).toBe(
      createHash('sha256').update(canonical, 'utf8').digest('base64url'),
    );
    expect(sessionRequestHash({ ...input, locale: 'ru-RU' })).not.toBe(sessionRequestHash(input));
  });

  it('rejects a token bound to an altered canonical request hash', async () => {
    const { db, input, verifier } = setup();
    verifier.decode = async () => ({
      requestHash: 'altered-hash',
      packageName: config.packageName,
      versionCode: config.versionCode,
      certificateSha256: [config.certificateSha256],
      appRecognitionVerdict: 'PLAY_RECOGNIZED',
      appLicensingVerdict: 'LICENSED',
      deviceRecognitionVerdicts: ['MEETS_DEVICE_INTEGRITY'],
    });
    await expect(createIntegritySession(db, input, verifier, null, config))
      .rejects.toThrow('INTEGRITY_REQUEST_HASH_MISMATCH');
  });

  it.each([
    ['package', { packageName: 'com.other.app' }, 'INTEGRITY_PACKAGE_MISMATCH'],
    ['version', { versionCode: '2' }, 'INTEGRITY_VERSION_MISMATCH'],
    ['certificate', { certificateSha256: ['B'.repeat(64)] }, 'INTEGRITY_CERTIFICATE_MISMATCH'],
    ['licensing', { appLicensingVerdict: 'UNLICENSED' }, 'INTEGRITY_LICENSE_REQUIRED'],
  ])('rejects a mismatched Play %s verdict', async (_name, changes, expectedError) => {
    const { db, input, verifier } = setup();
    const payload = await verifier.decode(input.integrityToken, config.packageName);
    verifier.decode = async () => ({ ...payload, ...(changes as Partial<IntegrityPayload>) });
    await expect(createIntegritySession(db, input, verifier, null, config)).rejects.toThrow(expectedError);
  });

  it('consumes a challenge once and rejects replay', async () => {
    const { db, input, verifier } = setup();
    await createIntegritySession(db, input, verifier, null, config);
    await expect(createIntegritySession(db, input, verifier, null, config))
      .rejects.toThrow('INTEGRITY_CHALLENGE_REPLAYED');
  });

  it('grants no starter credits for an unevaluated core verdict', async () => {
    const { db, input, verifier } = setup();
    verifier.decode = async () => ({
      requestHash: sessionRequestHash(input),
      packageName: config.packageName,
      versionCode: config.versionCode,
      certificateSha256: [config.certificateSha256],
      appRecognitionVerdict: 'UNEVALUATED',
      appLicensingVerdict: 'LICENSED',
      deviceRecognitionVerdicts: ['MEETS_DEVICE_INTEGRITY'],
    });
    const result = await createIntegritySession(db, input, verifier, null, config);
    expect(result.starterGranted).toBe(false);
    expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get(result.walletId)).toEqual({ balance: 0 });
  });

  it('grants no starter credits when Device Recall is unavailable', async () => {
    const { db, input, verifier } = setup();
    const result = await createIntegritySession(db, input, verifier, null, config);
    expect(result.starterGranted).toBe(false);
    expect(db.prepare('SELECT count(*) AS n FROM starter_grant').get()).toEqual({ n: 0 });
  });

  it('grants once only after Device Recall claims the bit', async () => {
    const { db, input, verifier } = setup();
    const recall: DeviceRecall = { async claimStarter() { return true; } };
    const result = await createIntegritySession(db, input, verifier, recall, config);
    expect(result.starterGranted).toBe(true);
    expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get(result.walletId)).toEqual({ balance: 10 });
    expect(db.prepare(`SELECT count(*) AS n FROM ledger WHERE kind = 'STARTER'`).get()).toEqual({ n: 1 });
  });

  it('does not use the install id as proof against reinstall or reset', async () => {
    const db = database();
    const recall: DeviceRecall = {
      async claimStarter() { return false; },
    };
    const first = setup(db, 'same-install-id');
    const second = setup(db, 'same-install-id');
    first.verifier.decode = async () => ({
      requestHash: sessionRequestHash(first.input), packageName: config.packageName,
      versionCode: config.versionCode, certificateSha256: [config.certificateSha256],
      appRecognitionVerdict: 'PLAY_RECOGNIZED', appLicensingVerdict: 'LICENSED',
      deviceRecognitionVerdicts: ['MEETS_DEVICE_INTEGRITY'],
    });
    second.verifier.decode = async () => ({
      requestHash: sessionRequestHash(second.input), packageName: config.packageName,
      versionCode: config.versionCode, certificateSha256: [config.certificateSha256],
      appRecognitionVerdict: 'PLAY_RECOGNIZED', appLicensingVerdict: 'LICENSED',
      deviceRecognitionVerdicts: ['MEETS_DEVICE_INTEGRITY'],
    });
    const a = await createIntegritySession(db, first.input, first.verifier, recall, config);
    const b = await createIntegritySession(db, second.input, second.verifier, recall, config);
    expect(a.walletId).not.toBe(b.walletId);
    expect(a.starterGranted).toBe(false);
    expect(b.starterGranted).toBe(false);
    expect(db.prepare('SELECT count(*) AS n FROM wallet WHERE balance > 0').get()).toEqual({ n: 0 });
  });
});
