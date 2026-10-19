import { createSign } from 'node:crypto';
import type { ServerConfig } from './config.js';
import type { DeviceRecall, IntegrityConfig, IntegrityPayload, IntegrityVerifier, SessionInput } from './integrity.js';

type Warn = (message: string) => void;
const scope = 'https://www.googleapis.com/auth/playintegrity';
export const devIntegrityBanner =
  '*** PLINTH DEV INTEGRITY ACTIVE: Google attestation and Device Recall are SIMULATED; ' +
  'no device/license verification; challenge/hash/replay/expiry and starter budget remain enforced. NOT FOR PRODUCTION. ***';

export function sessionIntegrityConfig(config: ServerConfig): IntegrityConfig | null {
  const versionCode = config.devIntegrity ? 'LOCAL' : config.playVersionCode;
  const certificateSha256 = config.devIntegrity ? '0'.repeat(64) : config.playSigningCertSha256;
  if (!versionCode || !certificateSha256) return null;
  return { packageName: config.playPackageName, versionCode, certificateSha256,
    starterCredits: config.starterCredits, starterDailyBudgetMicroUsd: config.starterDailyBudgetMicroUsd };
}

export class StarterDeviceRecall implements DeviceRecall {
  constructor(
    private readonly write: (token: string) => Promise<void>,
    private readonly warn: Warn,
  ) {}

  async claimStarter(input: SessionInput, payload: IntegrityPayload): Promise<boolean | null> {
    if (payload.recallBitFirst !== false) {
      if (payload.recallBitFirst === undefined) this.warn('Device Recall unavailable/unevaluated: no starter grant.');
      return payload.recallBitFirst === true ? false : null;
    }
    try {
      await this.write(input.integrityToken);
      return true;
    } catch {
      // Do not log provider errors: they may contain the bearer token or request body.
      this.warn('Device Recall write failed: no starter grant.');
      return null;
    }
  }
}

// Separate transport because GooglePlayClient's API/OAuth helpers are private and it discards recall values.
// Decode exactly once: decoding a Standard token again can clear its verdicts through Google's replay protection.
export class PlayRecallIntegrityVerifier implements IntegrityVerifier {
  readonly recall: DeviceRecall;
  private readonly account: { client_email: string; private_key: string };
  private accessToken: { value: string; expiresAt: number } | undefined;

  constructor(accountJson: string, private readonly packageName: string, warn: Warn) {
    let parsed: unknown;
    try {
      parsed = JSON.parse(accountJson);
    } catch {
      throw new Error('PLAY_SERVICE_ACCOUNT_JSON is not valid JSON');
    }
    if (!isRecord(parsed) || typeof parsed['client_email'] !== 'string' || typeof parsed['private_key'] !== 'string') {
      throw new Error('PLAY_SERVICE_ACCOUNT_JSON must contain client_email and private_key');
    }
    this.account = { client_email: parsed['client_email'], private_key: parsed['private_key'] };
    this.recall = new StarterDeviceRecall(async (integrityToken) => {
      await this.api('/deviceRecall:write', { integrityToken, newValues: { bitFirst: true } });
    }, warn);
  }

  async decode(token: string, packageName: string): Promise<IntegrityPayload> {
    if (packageName !== this.packageName) throw new Error('INTEGRITY_PACKAGE_MISMATCH');
    const response = await this.api(':decodeIntegrityToken', { integrityToken: token });
    const payload = response['tokenPayloadExternal'];
    if (!isRecord(payload)) throw new Error('PLAY_INTEGRITY_RESPONSE_INVALID');
    const request = record(payload['requestDetails']);
    const app = record(payload['appIntegrity']);
    const account = record(payload['accountDetails']);
    const device = record(payload['deviceIntegrity']);
    const bit = record(record(device['deviceRecall'])['values'])['bitFirst'];
    return {
      requestHash: text(request['requestHash']),
      packageName: text(request['requestPackageName']),
      versionCode: text(app['versionCode']),
      certificateSha256: strings(app['certificateSha256Digest']),
      appRecognitionVerdict: text(app['appRecognitionVerdict']),
      appLicensingVerdict: text(account['appLicensingVerdict']),
      deviceRecognitionVerdicts: strings(device['deviceRecognitionVerdict']),
      recallBitFirst: typeof bit === 'boolean' ? bit : undefined,
    };
  }

  private async api(suffix: string, body: unknown): Promise<Record<string, unknown>> {
    const token = await this.oauthToken();
    const response = await fetch(
      `https://playintegrity.googleapis.com/v1/${encodeURIComponent(this.packageName)}${suffix}`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
        signal: AbortSignal.timeout(15_000),
      });
    if (!response.ok) throw new Error(`GOOGLE_API_HTTP_${response.status}`);
    if (response.status === 204) return {};
    const payload: unknown = await response.json();
    if (!isRecord(payload)) throw new Error('GOOGLE_API_RESPONSE_INVALID');
    return payload;
  }

  private async oauthToken(): Promise<string> {
    if (this.accessToken && this.accessToken.expiresAt > Date.now() + 60_000) return this.accessToken.value;
    const issuedAt = Math.floor(Date.now() / 1000);
    const encode = (value: unknown) => Buffer.from(JSON.stringify(value)).toString('base64url');
    const unsigned = `${encode({ alg: 'RS256', typ: 'JWT' })}.${encode({
      iss: this.account.client_email, scope, aud: 'https://oauth2.googleapis.com/token',
      iat: issuedAt, exp: issuedAt + 3600,
    })}`;
    const signer = createSign('RSA-SHA256');
    signer.update(unsigned);
    const response = await fetch('https://oauth2.googleapis.com/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
        assertion: `${unsigned}.${signer.sign(this.account.private_key, 'base64url')}`,
      }),
      signal: AbortSignal.timeout(10_000),
    });
    if (!response.ok) throw new Error('GOOGLE_SERVICE_ACCOUNT_AUTH_FAILED');
    const payload: unknown = await response.json();
    if (!isRecord(payload) || typeof payload['access_token'] !== 'string') {
      throw new Error('GOOGLE_SERVICE_ACCOUNT_AUTH_INVALID');
    }
    const expiresIn = typeof payload['expires_in'] === 'number' ? payload['expires_in'] : 3600;
    this.accessToken = { value: payload['access_token'], expiresAt: Date.now() + expiresIn * 1000 };
    return this.accessToken.value;
  }
}

export function createSessionIntegrityVerifier(config: ServerConfig, warn: Warn): IntegrityVerifier | null {
  if (config.devIntegrity && process.env.NODE_ENV === 'production') {
    throw new Error('PLINTH_DEV_INTEGRITY cannot be enabled in production');
  }
  const identity = sessionIntegrityConfig(config);
  if (config.devIntegrity && identity) {
    warn(devIntegrityBanner);
    return {
      // This is a local stand-in, not device identity: reinstall/deletion can reset it.
      // integrity.ts checks existing DB grants and caps liability with starterDailyBudgetMicroUsd.
      recall: new StarterDeviceRecall(async () => {}, warn),
      async decode(token) {
        if (!/^dev:[A-Za-z0-9_-]{43}$/.test(token)) throw new Error('INTEGRITY_REQUEST_HASH_MISMATCH');
        return {
          requestHash: token.slice(4), packageName: identity.packageName, versionCode: identity.versionCode,
          certificateSha256: [identity.certificateSha256], appRecognitionVerdict: 'PLAY_RECOGNIZED',
          appLicensingVerdict: 'LICENSED', deviceRecognitionVerdicts: ['MEETS_DEVICE_INTEGRITY'],
          recallBitFirst: false,
        };
      },
    };
  }
  return config.playServiceAccountJson && identity
    ? new PlayRecallIntegrityVerifier(config.playServiceAccountJson, config.playPackageName, warn)
    : null;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}
function record(value: unknown): Record<string, unknown> { return isRecord(value) ? value : {}; }
function text(value: unknown): string | undefined { return typeof value === 'string' ? value : undefined; }
function strings(value: unknown): string[] | undefined {
  return Array.isArray(value) && value.every((item) => typeof item === 'string') ? value : undefined;
}
