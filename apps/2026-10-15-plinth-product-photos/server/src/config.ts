import { resolve } from 'node:path';

function requiredSecret(env: NodeJS.ProcessEnv, name: string): string {
  const value = env[name];
  if (!value || value.trim().length < 32) {
    throw new Error(`${name} must be set to at least 32 characters`);
  }
  return value;
}

function positiveInteger(env: NodeJS.ProcessEnv, name: string, fallback: number): number {
  const raw = env[name];
  if (raw === undefined) return fallback;
  const value = Number(raw);
  if (!Number.isSafeInteger(value) || value <= 0) throw new Error(`${name} must be a positive integer`);
  return value;
}

/**
 * How the socket address becomes `request.ip`.
 *
 * The service listens on loopback behind Caddy on the same host, so the trusted
 * hop is `loopback` and nothing more: trusting a wider range would let a remote
 * caller forge `X-Forwarded-For` and step out of its rate-limit bucket. A
 * deployment that terminates elsewhere sets `TRUST_PROXY=<address or CIDR>`.
 */
function trustProxySetting(env: NodeJS.ProcessEnv): boolean | string {
  const raw = (env.TRUST_PROXY ?? 'loopback').trim();
  const lowered = raw.toLowerCase();
  if (lowered === '' || lowered === 'loopback') return 'loopback';
  if (lowered === '0' || lowered === 'false' || lowered === 'off') return false;
  if (lowered === '1' || lowered === 'true') return true;
  return raw;
}

function positiveMicroUsd(env: NodeJS.ProcessEnv, name: string, fallbackUsd: number): number {
  const raw = env[name];
  const value = raw === undefined ? fallbackUsd : Number(raw);
  if (!Number.isFinite(value) || value <= 0) throw new Error(`${name} must be a positive USD amount`);
  return Math.round(value * 1_000_000);
}

/**
 * The version codes Play may attest, from `PLAY_VERSION_CODES` (comma-separated)
 * or the single-value `PLAY_VERSION_CODE` kept for older deployments.
 *
 * An empty result disables the session route rather than accepting anything:
 * `sessionIntegrityConfig` returns null on an empty list, so a missing setting
 * fails closed instead of attesting every build.
 */
function parseVersionCodes(env: NodeJS.ProcessEnv): string[] {  const listed = (env.PLAY_VERSION_CODES ?? '').split(',').map((value) => value.trim()).filter(Boolean);
  const single = (env.PLAY_VERSION_CODE ?? '').trim();
  const codes = listed.length > 0 ? listed : single ? [single] : [];
  for (const code of codes) {
    if (!/^[0-9]{1,10}$/.test(code)) {
      throw new Error(`PLAY_VERSION_CODES must be comma-separated integers, got '${code}'`);
    }
  }
  return [...new Set(codes)];
}

/** The deployment's own name, reported by `/v1/config`; see `environmentName` on the config. */
function environmentName(env: NodeJS.ProcessEnv): string {
  const value = (env.PLINTH_ENVIRONMENT ?? 'production').trim();
  if (!/^[a-z][a-z0-9-]{0,31}$/.test(value)) {
    throw new Error(`PLINTH_ENVIRONMENT must match [a-z][a-z0-9-]{0,31}, got '${value}'`);
  }
  return value;
}

export interface ServerConfig {
  jwtSecret: string;
  falKey?: string;
  recoveryPepper?: string;
  databasePath: string;
  dataDir: string;
  jobConcurrency: number;
  dailySpendCapMicroUsd: number;
  starterDailyBudgetMicroUsd: number;
  starterCredits: number;
  /**
   * `true` grants the starter credits when Play's core verdicts and device
   * integrity pass but Device Recall returned no value — the state that shipped
   * with 0 free credits on 2026-10-21 because Device Recall is not enabled in
   * Play Console yet. The daily starter budget and the one-grant-per-install
   * rule still bound the liability; what is lost is only reinstall resistance.
   */
  starterWithoutRecall: boolean;
  /**
   * A short name for this deployment (`production`, `debug`, `staging`). It is
   * reported in `/v1/config` so the public prefixes can be told apart by what
   * they answer: before this existed, prod and debug returned byte-identical
   * config payloads and the separation audit's S8c leg could not pass.
   */
  environmentName: string;
  devIntegrity?: boolean;
  minAppVersion: string;
  /** Present -> the service is in `maintenance`: new paid work is refused, reads stay open. */
  killSwitchFile: string;
  /** Present -> the service is `closed`: final, and it outranks the maintenance flag. */
  serviceClosedFile: string;
  /** Fastify's `trustProxy`: `loopback` by default, because nginx is the only trusted hop. */
  trustProxy: boolean | string;
  playPackageName: string;
  /**
   * Every version code the server will attest. A release adds one entry; it must
   * not replace the previous one, because a staged rollout leaves users on the
   * older build and they would otherwise be refused with a valid Play install.
   */
  playVersionCodes: readonly string[];
  playSigningCertSha256?: string;
  playServiceAccountJson?: string;
  billingBindingSecret?: string;
  purchaseTokenKek?: Buffer;
  playCreditProducts: Readonly<Record<string, number>>;
  playEntitlementProducts: Readonly<Record<string, string>>;
  pubsubAudience?: string;
  pubsubServiceAccountEmail?: string;
}

export function loadConfig(env: NodeJS.ProcessEnv = process.env): ServerConfig {
  if (env.PLINTH_DEV_INTEGRITY === '1' && env.NODE_ENV === 'production') {
    throw new Error('PLINTH_DEV_INTEGRITY cannot be enabled in production');
  }
  const cwd = process.cwd();
  let playCreditProducts: Record<string, number> = {};
  let playEntitlementProducts: Record<string, string> = {};
  let purchaseTokenKek: Buffer | undefined;
  if (env.PLAY_ENTITLEMENT_PRODUCTS) {
    const parsed: unknown = JSON.parse(env.PLAY_ENTITLEMENT_PRODUCTS);
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed) ||
        Object.entries(parsed).some(([id, name]) => !id || name !== 'unlock')) {
      throw new Error('PLAY_ENTITLEMENT_PRODUCTS must be a JSON object of product ids to the entitlement name "unlock"');
    }
    playEntitlementProducts = parsed as Record<string, string>;
  }
  if (env.PLAY_CREDIT_PRODUCTS) {
    const parsed: unknown = JSON.parse(env.PLAY_CREDIT_PRODUCTS);
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed) ||
        Object.entries(parsed).some(([id, credits]) =>
          !id || typeof credits !== 'number' || !Number.isSafeInteger(credits) || credits <= 0)) {
      throw new Error('PLAY_CREDIT_PRODUCTS must be a JSON object of product ids to positive credit counts');
    }
    playCreditProducts = parsed as Record<string, number>;
  }
  if (env.PURCHASE_TOKEN_KEK) {
    purchaseTokenKek = Buffer.from(env.PURCHASE_TOKEN_KEK, 'base64');
    if (purchaseTokenKek.length !== 32) throw new Error('PURCHASE_TOKEN_KEK must be base64 for 32 random bytes');
  }
  return {
    jwtSecret: requiredSecret(env, 'JWT_SECRET'),
    falKey: env.FAL_KEY || undefined,
    recoveryPepper: env.RECOVERY_PEPPER || undefined,
    databasePath: resolve(cwd, env.DATABASE_PATH || 'data/server.sqlite'),
    dataDir: resolve(cwd, env.DATA_DIR || 'data'),
    jobConcurrency: positiveInteger(env, 'JOB_CONCURRENCY', 2),
    dailySpendCapMicroUsd: positiveMicroUsd(env, 'DAILY_SPEND_CAP_USD', 25),
    starterDailyBudgetMicroUsd: positiveMicroUsd(env, 'STARTER_DAILY_BUDGET_USD', 10),
    starterCredits: positiveInteger(env, 'STARTER_CREDITS', 10),
    starterWithoutRecall: env.STARTER_ALLOW_WITHOUT_RECALL === '1',
    environmentName: environmentName(env),
    devIntegrity: env.PLINTH_DEV_INTEGRITY === '1',
    minAppVersion: env.MIN_APP_VERSION || '0',
    killSwitchFile: resolve(cwd, env.KILL_SWITCH_FILE || 'KILL_SWITCH'),
    serviceClosedFile: resolve(cwd, env.SERVICE_CLOSED_FILE || 'SERVICE_CLOSED'),
    trustProxy: trustProxySetting(env),
    playPackageName: env.PLAY_PACKAGE_NAME || 'com.gexetr.plinthproductphotos',
    playVersionCodes: parseVersionCodes(env),
    playSigningCertSha256: env.PLAY_SIGNING_CERT_SHA256?.replaceAll(':', '').toUpperCase() || undefined,
    playServiceAccountJson: env.PLAY_SERVICE_ACCOUNT_JSON || undefined,
    billingBindingSecret: env.BILLING_BINDING_SECRET || undefined,
    purchaseTokenKek,
    playCreditProducts,
    playEntitlementProducts,
    pubsubAudience: env.PUBSUB_AUDIENCE || undefined,
    pubsubServiceAccountEmail: env.PUBSUB_SERVICE_ACCOUNT_EMAIL || undefined,
  };
}
