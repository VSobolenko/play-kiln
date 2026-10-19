import { afterEach, describe, expect, it } from 'vitest';
import { createDatabase } from '../src/db.js';
import { CreditLedger } from '../src/ledger.js';
import { PurchaseService, type PlayPurchase, type PlayPurchaseStore } from '../src/purchases.js';

const databases: ReturnType<typeof createDatabase>[] = [];
afterEach(() => {
  for (const db of databases.splice(0)) db.close();
});

function fixture() {
  const db = createDatabase(':memory:');
  databases.push(db);
  db.prepare('INSERT INTO wallet (id, balance, created_at) VALUES (?, 0, ?)').run('wallet-1', Date.now());
  const purchases = new Map<string, PlayPurchase>();
  const finalized: string[] = [];
  let orderCount = 0;
  let consumeFailures = 0;
  const store: PlayPurchaseStore = {
    async getPurchase(_productId, token) {
      const purchase = purchases.get(token);
      if (!purchase) throw new Error('PURCHASE_INVALID');
      return purchase;
    },
    async consume(_productId, token) {
      if (consumeFailures > 0) {
        consumeFailures -= 1;
        throw new Error('PLAY_TEMPORARILY_UNAVAILABLE');
      }
      finalized.push(token);
    },
    async acknowledge(_productId, token) { finalized.push(token); },
    async listVoidedPurchases() { return { purchases: [] }; },
  };
  const service = new PurchaseService(db, store, 'b'.repeat(32), 'com.gexetr.plinthproductphotos',
    { 'credits.small': 10 }, Buffer.alloc(32, 3));
  const verified = (overrides: Partial<PlayPurchase> = {}): PlayPurchase => ({
    packageName: 'com.gexetr.plinthproductphotos',
    productId: 'credits.small',
    state: 'PURCHASED',
    quantity: 1,
    obfuscatedAccountId: '',
    obfuscatedProfileId: '',
    orderId: `order-${++orderCount}`,
    consumptionState: 'UNCONSUMED',
    ...overrides,
  });
  return {
    db, service, store, purchases, finalized, verified,
    failNextConsume() { consumeFailures += 1; },
  };
}

describe('purchase fulfilment', () => {
  it('does not credit a forged or unknown token', async () => {
    const { db, service } = fixture();
    const intent = service.createIntent('wallet-1', 'credits.small');
    await expect(service.confirm('wallet-1', intent.intentId, 'credits.small', 'forged-token'))
      .rejects.toThrow('PURCHASE_INVALID');
    expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-1')).toEqual({ balance: 0 });
  });

  it('rejects a Play product mismatch without credit', async () => {
    const { db, service, purchases, verified } = fixture();
    const intent = service.createIntent('wallet-1', 'credits.small');
    purchases.set('token-a', verified({ productId: 'credits.large' }));
    await expect(service.confirm('wallet-1', intent.intentId, 'credits.small', 'token-a'))
      .rejects.toThrow('PURCHASE_INVALID');
    expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-1')).toEqual({ balance: 0 });
  });

  it('rejects a purchase owned by a different wallet binding', async () => {
    const { db, service, purchases, verified } = fixture();
    const intent = service.createIntent('wallet-1', 'credits.small');
    purchases.set('wrong-owner', verified({ obfuscatedAccountId: 'other-wallet' }));
    await expect(service.confirm('wallet-1', intent.intentId, 'credits.small', 'wrong-owner'))
      .rejects.toThrow('PURCHASE_INVALID');
    expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-1')).toEqual({ balance: 0 });
  });

  it.each([
    ['package', { packageName: 'com.other.app' }],
    ['quantity', { quantity: 2 }],
  ])('rejects a Play %s mismatch', async (_name, change) => {
    const { db, service, purchases, verified } = fixture();
    const intent = service.createIntent('wallet-1', 'credits.small');
    purchases.set('mismatch-token', verified({
      ...service.intentBinding(intent.intentId),
      ...(change as Partial<PlayPurchase>),
    }));
    await expect(service.confirm('wallet-1', intent.intentId, 'credits.small', 'mismatch-token'))
      .rejects.toThrow('PURCHASE_INVALID');
    expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-1')).toEqual({ balance: 0 });
  });

  it('fulfils once, finalizes server-side, and returns the same result for replay', async () => {
    const { db, service, purchases, finalized, verified } = fixture();
    const intent = service.createIntent('wallet-1', 'credits.small');
    const binding = service.intentBinding(intent.intentId);
    purchases.set('token-a', verified(binding));
    const first = await service.confirm('wallet-1', intent.intentId, 'credits.small', 'token-a');
    const replay = await service.confirm('wallet-1', intent.intentId, 'credits.small', 'token-a');
    expect(first).toEqual(replay);
    expect(finalized).toEqual(['token-a']);
    expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-1')).toEqual({ balance: 10 });
    expect(db.prepare(`SELECT count(*) AS n FROM ledger WHERE kind = 'PURCHASE'`).get()).toEqual({ n: 1 });
  });

  it('holds a pending purchase without crediting', async () => {
    const { db, service, purchases, verified } = fixture();
    const intent = service.createIntent('wallet-1', 'credits.small');
    purchases.set('pending-token', verified({
      ...service.intentBinding(intent.intentId), state: 'PENDING',
    }));
    await expect(service.confirm('wallet-1', intent.intentId, 'credits.small', 'pending-token'))
      .rejects.toThrow('PURCHASE_PENDING');
    expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-1')).toEqual({ balance: 0 });
  });

  it('persists encrypted purchase state and retries server-side finalization after interruption', async () => {
    const { db, service, purchases, verified, failNextConsume } = fixture();
    const intent = service.createIntent('wallet-1', 'credits.small');
    purchases.set('retry-token', verified(service.intentBinding(intent.intentId)));
    failNextConsume();
    await expect(service.confirm('wallet-1', intent.intentId, 'credits.small', 'retry-token'))
      .rejects.toThrow('PURCHASE_FINALIZATION_PENDING');
    const pending = db.prepare(`SELECT state, token_ciphertext, token_wrapped_key FROM purchase`)
      .get() as { state: string; token_ciphertext: Buffer; token_wrapped_key: Buffer };
    expect(pending.state).toBe('PLAY_FINALIZING');
    expect(pending.token_ciphertext.toString('utf8')).not.toContain('retry-token');
    expect(pending.token_wrapped_key).toBeInstanceOf(Buffer);
    expect(await service.retryFinalizations(Date.now() + 60_001)).toBe(1);
    expect(db.prepare('SELECT state, token_ciphertext FROM purchase').get())
      .toEqual({ state: 'CONSUMED', token_ciphertext: null });
  });

  it('rejects cancelled purchases without credit', async () => {
    const { db, service, purchases, verified } = fixture();
    const intent = service.createIntent('wallet-1', 'credits.small');
    purchases.set('cancel-token', verified({ state: 'CANCELED' }));
    await expect(service.confirm('wallet-1', intent.intentId, 'credits.small', 'cancel-token'))
      .rejects.toThrow('PURCHASE_INVALID');
    expect(db.prepare('SELECT balance FROM wallet WHERE id = ?').get('wallet-1')).toEqual({ balance: 0 });
  });

  it('deduplicates RTDN and claws back voided credits into refund debt', async () => {
    const { db, service, purchases, verified } = fixture();
    const intent = service.createIntent('wallet-1', 'credits.small');
    purchases.set('token-a', verified(service.intentBinding(intent.intentId)));
    await service.confirm('wallet-1', intent.intentId, 'credits.small', 'token-a');
    purchases.set('token-a', { ...purchases.get('token-a')!, state: 'VOIDED' });
    db.prepare('UPDATE wallet SET balance = 2 WHERE id = ?').run('wallet-1');
    db.prepare(`INSERT INTO ledger(wallet_id,kind,delta,ref,idempotency_key,created_at)
      VALUES('wallet-1','RESERVE',-8,'job','job',?)`).run(Date.now());
    const event = {
      messageId: 'message-1', purchaseToken: 'token-a', productId: 'credits.small',
      type: 'VOIDED' as const, eventTime: Date.now(),
    };
    purchases.set('token-a', { ...purchases.get('token-a')!, state: 'VOIDED' });
    expect(await service.processRtdn(event)).toEqual({ duplicate: false });
    expect(await service.processRtdn(event)).toEqual({ duplicate: true });
    expect(db.prepare('SELECT balance, refund_debt FROM wallet WHERE id = ?').get('wallet-1'))
      .toEqual({ balance: 0, refund_debt: 8 });
    expect(db.prepare(`SELECT count(*) AS n FROM ledger WHERE kind = 'REFUND_ADJUST'`).get()).toEqual({ n: 1 });
    db.prepare(`INSERT INTO job(id,wallet_id,op,state,reserved,client_request_id,created_at)
      VALUES('blocked-job','wallet-1','O-02','QUEUED',0,'blocked-request',?)`).run(Date.now());
    expect(new CreditLedger(db).reserve('wallet-1', 'blocked-job', 1)).toMatchObject({ accepted: false });

    const nextIntent = service.createIntent('wallet-1', 'credits.small');
    purchases.set('token-b', verified(service.intentBinding(nextIntent.intentId)));
    await service.confirm('wallet-1', nextIntent.intentId, 'credits.small', 'token-b');
    expect(db.prepare('SELECT balance, refund_debt FROM wallet WHERE id = ?').get('wallet-1'))
      .toEqual({ balance: 2, refund_debt: 0 });
  });

  it('reconciles an authoritative voided-purchase page idempotently', async () => {
    const { db, service, purchases, store, verified } = fixture();
    const intent = service.createIntent('wallet-1', 'credits.small');
    purchases.set('token-voided', verified({ ...service.intentBinding(intent.intentId) }));
    await service.confirm('wallet-1', intent.intentId, 'credits.small', 'token-voided');
    db.prepare('UPDATE wallet SET balance = 0 WHERE id = ?').run('wallet-1');
    db.prepare(`INSERT INTO ledger(wallet_id,kind,delta,ref,idempotency_key,created_at)
      VALUES('wallet-1','RESERVE',-10,'spent','spent',?)`).run(Date.now());
    const mutableStore = store as PlayPurchaseStore & {
      listVoidedPurchases: () => Promise<{ purchases: Array<{ productId: string; purchaseToken: string; voidedQuantity: number }> }>;
    };
    mutableStore.listVoidedPurchases = async () => ({
      purchases: [{ productId: 'credits.small', purchaseToken: 'token-voided', voidedQuantity: 1 }],
    });
    const purchase = purchases.get('token-voided')!;
    purchases.set('token-voided', { ...purchase, state: 'VOIDED' });
    expect(await service.reconcileVoided()).toMatchObject({ processed: 1 });
    expect(await service.reconcileVoided()).toMatchObject({ processed: 0 });
    expect(db.prepare('SELECT balance, refund_debt FROM wallet WHERE id = ?').get('wallet-1'))
      .toEqual({ balance: 0, refund_debt: 10 });
  });
});
