import { afterEach, describe, expect, it } from 'vitest';
import { Ajv } from 'ajv';
import apiSchema from '../schema/api.schema.json' with { type: 'json' };
import { createApp } from '../src/app.js';
import { loadConfig } from '../src/config.js';
import { createDatabase } from '../src/db.js';
import { PurchaseService, type PlayPurchase, type PlayPurchaseStore } from '../src/purchases.js';

const PACKAGE = 'com.gexetr.plinthproductphotos';
const databases: ReturnType<typeof createDatabase>[] = [];
afterEach(() => {
  for (const db of databases.splice(0)) db.close();
});

function fixture() {
  const db = createDatabase(':memory:');
  databases.push(db);
  db.prepare('INSERT INTO wallet (id, balance, created_at) VALUES (?, 5, ?)').run('wallet-1', Date.now());
  const purchases = new Map<string, PlayPurchase>();
  const acknowledged: string[] = [];
  const consumed: string[] = [];
  let orders = 0;
  const store: PlayPurchaseStore = {
    async getPurchase(_productId, token) {
      const purchase = purchases.get(token);
      if (!purchase) throw new Error('PURCHASE_INVALID');
      return purchase;
    },
    async consume(_productId, token) { consumed.push(token); },
    async acknowledge(_productId, token) { acknowledged.push(token); },
    async listVoidedPurchases() { return { purchases: [] }; },
  };
  const service = new PurchaseService(db, store, 'b'.repeat(32), PACKAGE,
    { 'credits.small': 10 }, Buffer.alloc(32, 3), { plinth_unlock: 'unlock' });
  async function buy(productId: string, token: string) {
    const intent = service.createIntent('wallet-1', productId);
    purchases.set(token, {
      packageName: PACKAGE, productId, state: 'PURCHASED', quantity: 1,
      ...service.intentBinding(intent.intentId),
      orderId: `order-${++orders}`, consumptionState: 'UNCONSUMED',
    });
    return service.confirm('wallet-1', intent.intentId, productId, token);
  }
  const unlockedAt = () => (db.prepare('SELECT unlocked_at FROM wallet WHERE id = ?').get('wallet-1') as
    { unlocked_at: number | null }).unlocked_at;
  const ledgerCount = () => (db.prepare('SELECT count(*) AS n FROM ledger').get() as { n: number }).n;
  const balance = () => (db.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-1') as
    { balance: number }).balance;
  return { db, service, store, purchases, acknowledged, consumed, buy, unlockedAt, ledgerCount, balance };
}

describe('server-side unlock entitlement', () => {
  it('accepts an intent for the entitlement product and rejects an unknown product', () => {
    const { service } = fixture();
    expect(service.createIntent('wallet-1', 'plinth_unlock').intentId).toBeTruthy();
    expect(() => service.createIntent('wallet-1', 'not_a_product')).toThrow('PURCHASE_PRODUCT_UNKNOWN');
  });

  it('reports unlocked through GET /v1/wallet only after the unlock is confirmed', async () => {
    const { db, buy } = fixture();
    const app = await createApp({
      db, config: loadConfig({ JWT_SECRET: 'x'.repeat(32) }), closeDatabaseOnExit: false,
    });
    try {
      db.prepare(`INSERT INTO device(id,install_hash,created_at,integrity_state)
        VALUES('d1','i1',?,'PLAY_RECOGNIZED')`).run(Date.now());
      db.prepare(`INSERT INTO device_wallet(device_id,wallet_id) VALUES('d1','wallet-1')`).run();
      const token = app.jwt.sign({ sub: 'd1', wid: 'wallet-1', gen: 0 });
      const read = async () => (await app.inject({
        method: 'GET', url: '/v1/wallet', headers: { authorization: `Bearer ${token}` },
      })).json() as { unlocked: boolean; balance: number };
      expect(await read()).toMatchObject({ unlocked: false, balance: 5 });
      await buy('plinth_unlock', 'unlock-token');
      expect(await read()).toMatchObject({ unlocked: true, balance: 5 });
    } finally {
      await app.close();
    }
  });

  it('writes no ledger row, leaves the balance alone, acknowledges and marks FULFILLED', async () => {
    const { db, buy, ledgerCount, balance, acknowledged, consumed } = fixture();
    const result = await buy('plinth_unlock', 'unlock-token');
    expect(result).toEqual({ credited: 0, balance: 5, state: 'CREDITED' });
    expect(ledgerCount()).toBe(0);
    expect(balance()).toBe(5);
    expect(acknowledged).toEqual(['unlock-token']);
    expect(consumed).toEqual([]);
    expect(db.prepare('SELECT state FROM purchase').get()).toEqual({ state: 'FULFILLED' });
  });

  it('is idempotent: confirming twice does not error or move the timestamp', async () => {
    const { service, db, unlockedAt, ledgerCount, purchases } = fixture();
    const intent = service.createIntent('wallet-1', 'plinth_unlock');
    purchases.set('t', {
      packageName: PACKAGE, productId: 'plinth_unlock', state: 'PURCHASED', quantity: 1,
      ...service.intentBinding(intent.intentId), orderId: 'o1', consumptionState: 'UNCONSUMED',
    });
    const first = await service.confirm('wallet-1', intent.intentId, 'plinth_unlock', 't', Date.now());
    const stamp = unlockedAt();
    const second = await service.confirm('wallet-1', intent.intentId, 'plinth_unlock', 't', Date.now() + 5_000);
    expect(second).toEqual(first);
    expect(unlockedAt()).toBe(stamp);
    expect(stamp).not.toBeNull();
    expect(ledgerCount()).toBe(0);
    expect(db.prepare('SELECT count(*) AS n FROM purchase').get()).toEqual({ n: 1 });
  });

  it('a second unlock purchase does not move an existing unlocked_at', async () => {
    const { buy, unlockedAt, db } = fixture();
    await buy('plinth_unlock', 'first');
    db.prepare('UPDATE wallet SET unlocked_at = 123 WHERE id = ?').run('wallet-1');
    await buy('plinth_unlock', 'second');
    expect(unlockedAt()).toBe(123);
  });

  it('voiding the unlock clears unlocked and leaves the ledger untouched', async () => {
    const { service, purchases, buy, unlockedAt, ledgerCount, balance, db } = fixture();
    await buy('plinth_unlock', 'unlock-token');
    expect(unlockedAt()).not.toBeNull();
    purchases.set('unlock-token', { ...purchases.get('unlock-token')!, state: 'VOIDED' });
    await service.processRtdn({
      messageId: 'm1', purchaseToken: 'unlock-token', productId: 'plinth_unlock',
      type: 'VOIDED', eventTime: Date.now(),
    });
    expect(unlockedAt()).toBeNull();
    expect(ledgerCount()).toBe(0);
    expect(balance()).toBe(5);
    expect(db.prepare('SELECT state FROM purchase').get()).toEqual({ state: 'VOIDED' });
  });

  it('still fulfils a credit product exactly as before', async () => {
    const { buy, balance, db, consumed, unlockedAt } = fixture();
    const result = await buy('credits.small', 'credit-token');
    expect(result).toEqual({ credited: 10, balance: 15, state: 'CREDITED' });
    expect(balance()).toBe(15);
    expect(db.prepare(`SELECT kind, delta FROM ledger`).all()).toEqual([{ kind: 'PURCHASE', delta: 10 }]);
    expect(consumed).toEqual(['credit-token']);
    expect(db.prepare('SELECT state FROM purchase').get()).toEqual({ state: 'CONSUMED' });
    expect(unlockedAt()).toBeNull();
  });

  it('retry finalization acknowledges an entitlement instead of consuming it', async () => {
    const { service, store, purchases, db, acknowledged, consumed } = fixture();
    const intent = service.createIntent('wallet-1', 'plinth_unlock');
    purchases.set('t', {
      packageName: PACKAGE, productId: 'plinth_unlock', state: 'PURCHASED', quantity: 1,
      ...service.intentBinding(intent.intentId), orderId: 'o1', consumptionState: 'UNCONSUMED',
    });
    const realAck = store.acknowledge;
    store.acknowledge = async () => { throw new Error('PLAY_TEMPORARILY_UNAVAILABLE'); };
    await expect(service.confirm('wallet-1', intent.intentId, 'plinth_unlock', 't'))
      .rejects.toThrow('PURCHASE_FINALIZATION_PENDING');
    expect(db.prepare('SELECT unlocked_at IS NOT NULL AS u FROM wallet').get()).toEqual({ u: 1 });
    store.acknowledge = realAck;
    expect(await service.retryFinalizations(Date.now() + 120_000)).toBe(1);
    expect(acknowledged).toEqual(['t']);
    expect(consumed).toEqual([]);
    expect(db.prepare('SELECT state FROM purchase').get()).toEqual({ state: 'FULFILLED' });
  });
});

describe('PLAY_ENTITLEMENT_PRODUCTS validation', () => {
  const base = { JWT_SECRET: 'x'.repeat(32) };
  it('accepts the unlock entitlement', () => {
    expect(loadConfig({ ...base, PLAY_ENTITLEMENT_PRODUCTS: '{"plinth_unlock":"unlock"}' })
      .playEntitlementProducts).toEqual({ plinth_unlock: 'unlock' });
  });
  it('defaults to no entitlement products', () => {
    expect(loadConfig(base).playEntitlementProducts).toEqual({});
  });
  it.each([
    ['unknown entitlement name', '{"plinth_unlock":"premium"}'],
    ['empty product id', '{"":"unlock"}'],
    ['array', '["unlock"]'],
    ['non-string value', '{"plinth_unlock":1}'],
  ])('rejects %s', (_name, raw) => {
    expect(() => loadConfig({ ...base, PLAY_ENTITLEMENT_PRODUCTS: raw }))
      .toThrow(/PLAY_ENTITLEMENT_PRODUCTS.*"unlock"/);
  });
});

describe('WalletResponse schema', () => {
  const validate = new Ajv().compile(apiSchema.definitions.WalletResponse);
  const body = { balance: 1, reserved: 0, refundDebt: 0, purchaseHold: 0, entries: [] };
  it('rejects a body without unlocked', () => {
    expect(validate(body)).toBe(false);
  });
  it('accepts a body with a boolean unlocked and rejects a non-boolean', () => {
    expect(validate({ ...body, unlocked: false })).toBe(true);
    expect(validate({ ...body, unlocked: 'yes' })).toBe(false);
  });
});
