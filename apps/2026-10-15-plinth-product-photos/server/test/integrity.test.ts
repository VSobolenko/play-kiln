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
import { StarterDeviceRecall } from '../src/device-recall.js';

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
  versionCodes: ['1'],
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
        versionCode: config.versionCodes[0],
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
      versionCode: config.versionCodes[0],
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

  it('accepts every listed version code, so a staged rollout is not refused', async () => {
    const db = database();
    const listed = { ...config, versionCodes: ['1', '2'] };
    const first = setup(db, 'install-rollout-a');
    await expect(createIntegritySession(db, first.input, first.verifier, null, listed)).resolves.toBeTruthy();

    const second = setup(db, 'install-rollout-b');
    second.verifier.decode = async () => ({
      requestHash: sessionRequestHash(second.input), packageName: listed.packageName,
      versionCode: '2', certificateSha256: [listed.certificateSha256],
      appRecognitionVerdict: 'PLAY_RECOGNIZED', appLicensingVerdict: 'LICENSED',
      deviceRecognitionVerdicts: ['MEETS_DEVICE_INTEGRITY'],
    });
    await expect(createIntegritySession(db, second.input, second.verifier, null, listed)).resolves.toBeTruthy();
  });

  it('names the accepted versions when a build is refused', async () => {
    const { db, input, verifier } = setup();
    verifier.decode = async () => ({
      requestHash: sessionRequestHash(input), packageName: config.packageName,
      versionCode: '9', certificateSha256: [config.certificateSha256],
      appRecognitionVerdict: 'PLAY_RECOGNIZED', appLicensingVerdict: 'LICENSED',
      deviceRecognitionVerdicts: ['MEETS_DEVICE_INTEGRITY'],
    });
    await expect(createIntegritySession(db, input, verifier, null, config))
      .rejects.toThrow('INTEGRITY_VERSION_MISMATCH: attested 9, accepted 1');
  });

  it('grants no starter credits for an unevaluated core verdict', async () => {
    const { db, input, verifier } = setup();
    verifier.decode = async () => ({
      requestHash: sessionRequestHash(input),
      packageName: config.packageName,
      versionCode: config.versionCodes[0],
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
      versionCode: config.versionCodes[0], certificateSha256: [config.certificateSha256],
      appRecognitionVerdict: 'PLAY_RECOGNIZED', appLicensingVerdict: 'LICENSED',
      deviceRecognitionVerdicts: ['MEETS_DEVICE_INTEGRITY'],
    });
    second.verifier.decode = async () => ({
      requestHash: sessionRequestHash(second.input), packageName: config.packageName,
      versionCode: config.versionCodes[0], certificateSha256: [config.certificateSha256],
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

  // Recorded 2026-10-21: the production build opened with 0 credits and the log said
  // "Device Recall unavailable/unevaluated: no starter grant." — Device Recall was not
  // enabled in Play Console, so every real install lost the 10 credits the listing promises.
  it('grants the starter credits when the bit is unevaluated and STARTER_ALLOW_WITHOUT_RECALL is set', async () => {
    const { db, input, verifier } = setup();
    const recall = new StarterDeviceRecall(async () => {}, () => {}, true);
    const result = await createIntegritySession(db, input, verifier, recall, config);
    expect(result.starterGranted).toBe(true);
    expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get(result.walletId)).toEqual({ balance: 10 });
    expect(result.integrityState).toBe('RECALL_CLAIMED');
  });

  it('still refuses a device whose recall bit is already set while the flag is on', async () => {
    const { db, input, verifier } = setup();
    const recall = new StarterDeviceRecall(async () => {}, () => {}, true);
    verifier.decode = async () => ({
      requestHash: sessionRequestHash(input), packageName: config.packageName,
      versionCode: config.versionCodes[0], certificateSha256: [config.certificateSha256],
      appRecognitionVerdict: 'PLAY_RECOGNIZED', appLicensingVerdict: 'LICENSED',
      deviceRecognitionVerdicts: ['MEETS_DEVICE_INTEGRITY'], recallBitFirst: true,
    });
    const result = await createIntegritySession(db, input, verifier, recall, config);
    expect(result.starterGranted).toBe(false);
    expect(result.integrityState).toBe('RECALL_UNAVAILABLE_OR_USED');
  });
});

describe('Device Recall gate', () => {
  const input = {
    challengeId: 'challenge-1', challenge: 'value', installId: 'install-1',
    appVersion: '0.1.1', locale: 'en-US', integrityToken: 'token-1',
  };
  const noop = () => {};

  it('withholds the grant when the bit is unevaluated and the flag is off', async () => {
    const recalls = new StarterDeviceRecall(async () => {}, noop);
    expect(await recalls.claimStarter(input, {})).toBeNull();
  });

  it('grants and still writes the bit when it is unevaluated and the flag is on', async () => {
    const written: string[] = [];
    const recalls = new StarterDeviceRecall(async (token) => { written.push(token); }, noop, true);
    expect(await recalls.claimStarter(input, {})).toBe(true);
    expect(written).toEqual(['token-1']);
  });

  it('grants without a write when the write fails for an unevaluated bit', async () => {
    const recalls = new StarterDeviceRecall(async () => { throw new Error('provider down'); }, noop, true);
    expect(await recalls.claimStarter(input, {})).toBe(true);
  });

  it('withholds the grant when a write fails for an evaluated bit', async () => {
    const recalls = new StarterDeviceRecall(async () => { throw new Error('provider down'); }, noop, true);
    expect(await recalls.claimStarter(input, { recallBitFirst: false })).toBeNull();
  });
});
