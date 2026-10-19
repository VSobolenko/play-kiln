import { generateKeyPairSync } from 'node:crypto';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { GooglePlayClient } from '../src/google-play.js';

afterEach(() => vi.unstubAllGlobals());

describe('Google Play product purchase decoding', () => {
  it('reads package-scoped legacy purchase fields used for quantity and consumption checks', async () => {
    const { privateKey } = generateKeyPairSync('rsa', { modulusLength: 2048 });
    const fetchMock = vi.fn(async (input: string | URL | Request) => {
      const url = String(input);
      if (url === 'https://oauth2.googleapis.com/token') {
        return new Response(JSON.stringify({ access_token: 'test-access-token', expires_in: 3600 }));
      }
      expect(url).toContain(
        '/applications/com.gexetr.plinthproductphotos/purchases/products/credits.small/tokens/token-a',
      );
      return new Response(JSON.stringify({
        purchaseState: 0,
        productId: 'credits.small',
        quantity: 1,
        consumptionState: 0,
        obfuscatedExternalAccountId: 'account-binding',
        obfuscatedExternalProfileId: 'profile-binding',
        orderId: 'order-a',
      }));
    });
    vi.stubGlobal('fetch', fetchMock);
    const client = new GooglePlayClient(JSON.stringify({
      client_email: 'play-service@example.invalid',
      private_key: privateKey.export({ type: 'pkcs8', format: 'pem' }).toString(),
    }), 'com.gexetr.plinthproductphotos');

    await expect(client.getPurchase('credits.small', 'token-a')).resolves.toEqual({
      packageName: 'com.gexetr.plinthproductphotos',
      productId: 'credits.small',
      state: 'PURCHASED',
      quantity: 1,
      obfuscatedAccountId: 'account-binding',
      obfuscatedProfileId: 'profile-binding',
      orderId: 'order-a',
      consumptionState: 'UNCONSUMED',
    });
  });
});
