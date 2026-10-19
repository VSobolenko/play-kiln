import { createHash, randomBytes, randomUUID } from 'node:crypto';
import type Database from 'better-sqlite3';

const challengeLifetimeMs = 5 * 60 * 1000;
const hash = (value: string): string => createHash('sha256').update(value).digest('hex');

export type SessionInput = {
  challengeId: string;
  challenge: string;
  installId: string;
  appVersion: string;
  locale: string;
  integrityToken: string;
};

export type IntegrityPayload = {
  requestHash?: string;
  packageName?: string;
  versionCode?: string;
  certificateSha256?: string[];
  appRecognitionVerdict?: string;
  appLicensingVerdict?: string;
  deviceRecognitionVerdicts?: string[];
  recallBitFirst?: boolean;
};

export interface IntegrityVerifier {
  decode(token: string, packageName: string): Promise<IntegrityPayload>;
  recall?: DeviceRecall;
}

export interface DeviceRecall {
  claimStarter(input: SessionInput, payload: IntegrityPayload): Promise<boolean | null>;
}

export type IntegrityConfig = {
  packageName: string;
  versionCode: string;
  certificateSha256: string;
  starterCredits: number;
  starterDailyBudgetMicroUsd?: number;
};

// Reserve the full liability at the design's $0.025/credit ceiling, not merely today's spend.
const starterCreditCostMicroUsd = 25_000;

function starterAvailable(db: Database.Database, installId: string, config: IntegrityConfig, now: number): boolean {
  const used = db.prepare(`SELECT 1 FROM device d
    JOIN device_wallet dw ON dw.device_id = d.id
    JOIN starter_grant sg ON sg.wallet_id = dw.wallet_id WHERE d.install_hash = ? LIMIT 1`)
    .get(hash(installId));
  if (used) return false;
  const day = new Date(now).toISOString().slice(0, 10);
  const spent = db.prepare('SELECT starter_cost_micro_usd AS cost FROM spend_day WHERE day = ?')
    .get(day) as { cost: number } | undefined;
  return config.starterDailyBudgetMicroUsd === undefined ||
    (spent?.cost ?? 0) + config.starterCredits * starterCreditCostMicroUsd <= config.starterDailyBudgetMicroUsd;
}

export type IntegritySession = {
  deviceId: string;
  walletId: string;
  balance: number;
  starterGranted: boolean;
  integrityState: string;
  /** The attested app version, echoed back so the route can bind it into the JWT. */
  appVersion: string;
};

export function sessionRequestHash(input: SessionInput): string {
  const canonical = [
    'session',
    input.challengeId,
    input.challenge,
    input.installId,
    input.appVersion,
    input.locale,
  ].join('\n');
  return createHash('sha256').update(canonical, 'utf8').digest('base64url');
}

export function createChallenge(
  db: Database.Database,
  installId: string,
  appVersion: string,
  locale: string,
  now = Date.now(),
): { challengeId: string; challenge: string; expiresAt: number } {
  if (!/^[A-Za-z0-9._-]{1,128}$/.test(installId) ||
      !/^[A-Za-z0-9.+_-]{1,64}$/.test(appVersion) ||
      !/^[A-Za-z0-9-]{2,35}$/.test(locale)) {
    throw new Error('INVALID_SESSION_FIELDS');
  }
  const challengeId = randomUUID();
  const challenge = randomBytes(32).toString('base64url');
  const expiresAt = now + challengeLifetimeMs;
  db.prepare(`INSERT INTO integrity_challenge
    (id, challenge_hash, install_hash, app_version, locale, created_at, expires_at)
    VALUES (?, ?, ?, ?, ?, ?, ?)`)
    .run(challengeId, hash(challenge), hash(installId), appVersion, locale, now, expiresAt);
  return { challengeId, challenge, expiresAt };
}

export async function createIntegritySession(
  db: Database.Database,
  input: SessionInput,
  verifier: IntegrityVerifier,
  recall: DeviceRecall | null,
  config: IntegrityConfig,
  now = Date.now(),
): Promise<IntegritySession> {
  const challengeRow = db.prepare(`SELECT challenge_hash, install_hash, app_version, locale, expires_at, consumed_at
    FROM integrity_challenge WHERE id = ?`).get(input.challengeId) as {
    challenge_hash: string; install_hash: string; app_version: string; locale: string;
    expires_at: number; consumed_at: number | null;
  } | undefined;
  if (!challengeRow || challengeRow.expires_at <= now) throw new Error('INTEGRITY_CHALLENGE_EXPIRED');
  const consumed = db.prepare(`UPDATE integrity_challenge SET consumed_at = ?
    WHERE id = ? AND consumed_at IS NULL AND expires_at > ?`).run(now, input.challengeId, now);
  if (consumed.changes !== 1) throw new Error('INTEGRITY_CHALLENGE_REPLAYED');
  if (challengeRow.challenge_hash !== hash(input.challenge) ||
      challengeRow.install_hash !== hash(input.installId) ||
      challengeRow.app_version !== input.appVersion || challengeRow.locale !== input.locale) {
    throw new Error('INTEGRITY_CHALLENGE_MISMATCH');
  }

  const payload = await verifier.decode(input.integrityToken, config.packageName);
  if (payload.requestHash !== sessionRequestHash(input)) throw new Error('INTEGRITY_REQUEST_HASH_MISMATCH');
  if (payload.packageName !== config.packageName) throw new Error('INTEGRITY_PACKAGE_MISMATCH');
  if (payload.versionCode !== config.versionCode) throw new Error('INTEGRITY_VERSION_MISMATCH');
  if (!payload.certificateSha256?.some((value) => {
    // Google's wire format is base64url; configuration uses Play Console's SHA-256 hex.
    const normalized = /^[A-Fa-f0-9]{64}$/.test(value) ? value :
      /^[A-Za-z0-9_-]{43}=?$/.test(value) ? Buffer.from(value, 'base64url').toString('hex') : '';
    return normalized.toUpperCase() === config.certificateSha256.toUpperCase();
  })) {
    throw new Error('INTEGRITY_CERTIFICATE_MISMATCH');
  }
  if (payload.appRecognitionVerdict && payload.appRecognitionVerdict !== 'PLAY_RECOGNIZED' &&
      payload.appRecognitionVerdict !== 'UNEVALUATED') {
    throw new Error('INTEGRITY_APP_NOT_RECOGNIZED');
  }
  if (payload.appLicensingVerdict && payload.appLicensingVerdict !== 'LICENSED' &&
      payload.appLicensingVerdict !== 'UNEVALUATED') {
    throw new Error('INTEGRITY_LICENSE_REQUIRED');
  }
  const coreVerdictsPass = payload.appRecognitionVerdict === 'PLAY_RECOGNIZED' &&
    payload.appLicensingVerdict === 'LICENSED';
  const meetsDeviceIntegrity = payload.deviceRecognitionVerdicts?.includes('MEETS_DEVICE_INTEGRITY') ?? false;
  let recallClaimed = false;
  if (coreVerdictsPass && meetsDeviceIntegrity && recall && starterAvailable(db, input.installId, config, now)) {
    recallClaimed = (await recall.claimStarter(input, payload)) === true;
  }
  let starterGranted = coreVerdictsPass && meetsDeviceIntegrity && recallClaimed;
  const deviceId = randomUUID();
  const walletId = randomUUID();
  const integrityState = db.transaction(() => {
    // Recheck under the write lock: asynchronous recall calls can race on an install or daily budget.
    starterGranted = starterGranted && starterAvailable(db, input.installId, config, now);
    const state = !coreVerdictsPass ? 'UNEVALUATED' :
      !meetsDeviceIntegrity ? 'DEVICE_NOT_RECOGNIZED' :
        recall ? (starterGranted ? 'RECALL_CLAIMED' : 'RECALL_UNAVAILABLE_OR_USED') : 'RECALL_UNAVAILABLE';
    db.prepare(`INSERT INTO device(id, install_hash, created_at, integrity_state)
      VALUES (?, ?, ?, ?)`).run(deviceId, hash(input.installId), now, state);
    db.prepare(`INSERT INTO wallet(id, balance, created_at, starter_granted)
      VALUES (?, ?, ?, ?)`).run(walletId, starterGranted ? config.starterCredits : 0, now, starterGranted ? 1 : 0);
    db.prepare('INSERT INTO device_wallet(device_id, wallet_id) VALUES (?, ?)').run(deviceId, walletId);
    if (starterGranted) {
      db.prepare(`INSERT INTO spend_day(day, cost_micro_usd, starter_cost_micro_usd) VALUES (?, 0, ?)
        ON CONFLICT(day) DO UPDATE SET starter_cost_micro_usd = starter_cost_micro_usd + excluded.starter_cost_micro_usd`)
        .run(new Date(now).toISOString().slice(0, 10), config.starterCredits * starterCreditCostMicroUsd);
      db.prepare(`INSERT INTO starter_grant(wallet_id, recall_bit, granted_at) VALUES (?, 1, ?)`)
        .run(walletId, now);
      db.prepare(`INSERT INTO ledger(wallet_id, kind, delta, ref, idempotency_key, created_at)
        VALUES (?, 'STARTER', ?, ?, ?, ?)`)
        .run(walletId, config.starterCredits, input.challengeId, input.challengeId, now);
    }
    return state;
  }).immediate();
  return { deviceId, walletId, balance: starterGranted ? config.starterCredits : 0, starterGranted,
    integrityState, appVersion: input.appVersion };
}
