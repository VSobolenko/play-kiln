import { createSign, createPublicKey, verify as verifySignature } from 'node:crypto';
import type { IntegrityPayload, IntegrityVerifier } from './integrity.js';
import type { PlayPurchase, PlayPurchaseStore } from './purchases.js';

type ServiceAccount = { client_email: string; private_key: string };
type GoogleResponse = Record<string, unknown>;

export class GooglePlayClient implements IntegrityVerifier, PlayPurchaseStore {
  private readonly account: ServiceAccount;
  private accessToken: { value: string; expiresAt: number } | undefined;

  constructor(accountJson: string, private readonly packageName: string) {
    let parsed: unknown;
    try {
      parsed = JSON.parse(accountJson);
    } catch {
      throw new Error('PLAY_SERVICE_ACCOUNT_JSON is not valid JSON');
    }
    if (!parsed || typeof parsed !== 'object' || !('client_email' in parsed) ||
        !('private_key' in parsed) || typeof parsed.client_email !== 'string' ||
        typeof parsed.private_key !== 'string') {
      throw new Error('PLAY_SERVICE_ACCOUNT_JSON must contain client_email and private_key');
    }
    this.account = parsed as ServiceAccount;
  }

  async decode(token: string, packageName: string): Promise<IntegrityPayload> {
    const response = await this.api(
      `https://playintegrity.googleapis.com/v1/${encodeURIComponent(packageName)}:decodeIntegrityToken`,
      'https://www.googleapis.com/auth/playintegrity',
      { method: 'POST', body: JSON.stringify({ integrityToken: token }) },
    );
    const payload = response['tokenPayloadExternal'];
    if (!isRecord(payload)) throw new Error('PLAY_INTEGRITY_RESPONSE_INVALID');
    const request = isRecord(payload['requestDetails']) ? payload['requestDetails'] : {};
    const app = isRecord(payload['appIntegrity']) ? payload['appIntegrity'] : {};
    const account = isRecord(payload['accountDetails']) ? payload['accountDetails'] : {};
    const device = isRecord(payload['deviceIntegrity']) ? payload['deviceIntegrity'] : {};
    return {
      requestHash: stringValue(request['requestHash']),
      packageName: stringValue(request['requestPackageName']),
      versionCode: stringValue(app['versionCode']),
      certificateSha256: stringArray(app['certificateSha256Digest']),
      appRecognitionVerdict: stringValue(app['appRecognitionVerdict']),
      appLicensingVerdict: stringValue(account['appLicensingVerdict']),
      deviceRecognitionVerdicts: stringArray(device['deviceRecognitionVerdict']),
    };
  }

  async getPurchase(productId: string, purchaseToken: string): Promise<PlayPurchase> {
    const response = await this.api(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(this.packageName)}` +
        `/purchases/products/${encodeURIComponent(productId)}/tokens/${encodeURIComponent(purchaseToken)}`,
      'https://www.googleapis.com/auth/androidpublisher',
    );
    const packageName = this.packageName;
    const actualProductId = stringValue(response['productId']) ?? productId;
    const quantity = response['quantity'] === undefined ? 1 : response['quantity'];
    const rawState = response['purchaseState'];
    const state = rawState === 0 ? 'PURCHASED' :
      rawState === 1 ? 'CANCELED' :
        rawState === 2 ? 'PENDING' : null;
    if (!state) throw new Error('PURCHASE_INVALID');
    if (actualProductId !== productId || typeof quantity !== 'number' || !Number.isSafeInteger(quantity)) {
      throw new Error('PURCHASE_INVALID');
    }
    const consumptionState = response['consumptionState'] === 1 ? 'CONSUMED' :
      response['consumptionState'] === undefined || response['consumptionState'] === 0 ? 'UNCONSUMED' : null;
    if (!consumptionState) throw new Error('PURCHASE_INVALID');
    return {
      packageName,
      productId: actualProductId,
      state,
      quantity,
      obfuscatedAccountId: stringValue(response['obfuscatedExternalAccountId']) ?? null,
      obfuscatedProfileId: stringValue(response['obfuscatedExternalProfileId']) ?? null,
      orderId: stringValue(response['orderId']) ?? null,
      consumptionState,
    };
  }

  async consume(productId: string, purchaseToken: string): Promise<void> {
    await this.api(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(this.packageName)}` +
        `/purchases/products/${encodeURIComponent(productId)}/tokens/${encodeURIComponent(purchaseToken)}:consume`,
      'https://www.googleapis.com/auth/androidpublisher',
      { method: 'POST', body: '{}' },
    );
  }

  async acknowledge(productId: string, purchaseToken: string): Promise<void> {
    await this.api(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(this.packageName)}` +
        `/purchases/products/${encodeURIComponent(productId)}/tokens/${encodeURIComponent(purchaseToken)}:acknowledge`,
      'https://www.googleapis.com/auth/androidpublisher',
      { method: 'POST', body: '{}' },
    );
  }

  async listVoidedPurchases(startTime: number, pageToken?: string): Promise<{
    purchases: Array<{ productId: string; purchaseToken: string; voidedQuantity: number }>;
    pageToken?: string;
  }> {
    const url = new URL(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(this.packageName)}` +
      '/purchases/voidedpurchases',
    );
    url.searchParams.set('startTime', String(startTime));
    url.searchParams.set('type', '0');
    if (pageToken) url.searchParams.set('token', pageToken);
    const response = await this.api(url.toString(), 'https://www.googleapis.com/auth/androidpublisher');
    const rows = Array.isArray(response['voidedPurchases']) ? response['voidedPurchases'] : [];
    const purchases: Array<{ productId: string; purchaseToken: string; voidedQuantity: number }> = [];
    for (const row of rows) {
      if (!isRecord(row)) continue;
      const token = stringValue(row['purchaseToken']);
      if (!token) continue;
      const lineItems = await this.productDetailsForVoidedToken(token);
      if (lineItems.length === 0) throw new Error('VOIDED_PURCHASE_LOOKUP_EMPTY');
      for (const item of lineItems) purchases.push({
        productId: item.productId,
        purchaseToken: token,
        voidedQuantity: Number(row['voidedQuantity'] ?? item.quantity),
      });
    }
    return {
      purchases,
      ...(stringValue(response['tokenPagination'] && isRecord(response['tokenPagination'])
        ? response['tokenPagination']['nextPageToken'] : undefined)
        ? { pageToken: stringValue((response['tokenPagination'] as Record<string, unknown>)['nextPageToken'])! }
        : {}),
    };
  }

  async verifyRtdnIdentity(
    authorization: string | undefined,
    audience: string,
    serviceAccountEmail: string,
  ): Promise<boolean> {
    if (!authorization?.startsWith('Bearer ')) return false;
    const token = authorization.slice(7);
    const parts = token.split('.');
    if (parts.length !== 3) return false;
    try {
      const header = JSON.parse(Buffer.from(parts[0]!, 'base64url').toString('utf8')) as Record<string, unknown>;
      const claims = JSON.parse(Buffer.from(parts[1]!, 'base64url').toString('utf8')) as Record<string, unknown>;
      if (header['alg'] !== 'RS256' || typeof header['kid'] !== 'string' ||
          claims['aud'] !== audience ||
          (claims['iss'] !== 'accounts.google.com' && claims['iss'] !== 'https://accounts.google.com') ||
          claims['email'] !== serviceAccountEmail || claims['email_verified'] !== true ||
          typeof claims['exp'] !== 'number' || claims['exp'] <= Date.now() / 1000 ||
          typeof claims['iat'] !== 'number' || claims['iat'] < Date.now() / 1000 - 3600 ||
          claims['iat'] > Date.now() / 1000 + 60 || claims['exp'] - claims['iat'] > 3600) return false;
      const certs = await fetch('https://www.googleapis.com/oauth2/v3/certs', { signal: AbortSignal.timeout(10_000) });
      if (!certs.ok) return false;
      const keys = await certs.json() as { keys?: Array<Record<string, unknown>> };
      const key = keys.keys?.find((item) => item['kid'] === header['kid']);
      if (!key) return false;
      const publicKey = createPublicKey({
        key: key as import('node:crypto').JsonWebKey,
        format: 'jwk',
      });
      const signature = Buffer.from(parts[2]!, 'base64url');
      return verifySignature('RSA-SHA256', Buffer.from(`${parts[0]}.${parts[1]}`), publicKey, signature);
    } catch {
      return false;
    }
  }

  private async productDetailsForVoidedToken(token: string): Promise<Array<{ productId: string; quantity: number }>> {
    const response = await this.api(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(this.packageName)}` +
        `/purchases/productsv2/tokens/${encodeURIComponent(token)}`,
      'https://www.googleapis.com/auth/androidpublisher',
    );
    const rows = Array.isArray(response['productLineItem']) ? response['productLineItem'] : [];
    return rows.filter(isRecord).map((row) => ({
      productId: stringValue(row['productId']) ?? '',
      quantity: Number(row['quantity'] ?? 1),
    })).filter((row) => row.productId.length > 0 && Number.isSafeInteger(row.quantity));
  }

  private async api(url: string, scope: string, init?: RequestInit): Promise<GoogleResponse> {
    const token = await this.oauthToken(scope);
    const response = await fetch(url, {
      ...init,
      headers: { Authorization: `Bearer ${token}`, ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
        ...(init?.headers ?? {}) },
      signal: init?.signal ?? AbortSignal.timeout(15_000),
    });
    if (!response.ok) throw new Error(`GOOGLE_API_HTTP_${response.status}`);
    if (response.status === 204) return {};
    const payload: unknown = await response.json();
    if (!isRecord(payload)) throw new Error('GOOGLE_API_RESPONSE_INVALID');
    return payload;
  }

  private async oauthToken(scope: string): Promise<string> {
    if (this.accessToken && this.accessToken.expiresAt > Date.now() + 60_000) return this.accessToken.value;
    const issuedAt = Math.floor(Date.now() / 1000);
    const encode = (value: unknown): string => Buffer.from(JSON.stringify(value)).toString('base64url');
    const header = encode({ alg: 'RS256', typ: 'JWT' });
    const claims = encode({
      iss: this.account.client_email,
      scope,
      aud: 'https://oauth2.googleapis.com/token',
      iat: issuedAt,
      exp: issuedAt + 3600,
    });
    const signer = createSign('RSA-SHA256');
    signer.update(`${header}.${claims}`);
    const assertion = `${header}.${claims}.${signer.sign(this.account.private_key, 'base64url')}`;
    const response = await fetch('https://oauth2.googleapis.com/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
        assertion,
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

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}

function stringValue(value: unknown): string | undefined {
  return typeof value === 'string' ? value : undefined;
}

function stringArray(value: unknown): string[] | undefined {
  return Array.isArray(value) && value.every((item) => typeof item === 'string')
    ? value as string[] : undefined;
}
