import { createCipheriv, createDecipheriv, createHash, createHmac, randomBytes, randomUUID } from 'node:crypto';
import type Database from 'better-sqlite3';

const intentLifetimeMs = 15 * 60 * 1000;
const tokenHash = (token: string): string => createHash('sha256').update(token).digest('hex');

export type PlayPurchase = {
  packageName: string;
  productId: string;
  state: 'PURCHASED' | 'PENDING' | 'CANCELED' | 'VOIDED';
  quantity: number;
  obfuscatedAccountId: string | null;
  obfuscatedProfileId: string | null;
  orderId: string | null;
  consumptionState: 'CONSUMED' | 'UNCONSUMED';
}

export interface PlayPurchaseStore {
  getPurchase(productId: string, purchaseToken: string): Promise<PlayPurchase>;
  consume(productId: string, purchaseToken: string): Promise<void>;
  acknowledge(productId: string, purchaseToken: string): Promise<void>;
  listVoidedPurchases(startTime: number, pageToken?: string): Promise<{
    purchases: Array<{ productId: string; purchaseToken: string; voidedQuantity: number }>;
    pageToken?: string;
  }>;
}

export type RtdnPurchaseEvent = {
  messageId: string;
  purchaseToken: string;
  productId: string;
  type: 'PURCHASED' | 'CANCELED' | 'VOIDED';
  eventTime: number;
};

export class PurchaseService {
  constructor(
    private readonly db: Database.Database,
    private readonly store: PlayPurchaseStore,
    private readonly bindingSecret: string,
    private readonly packageName: string,
    private readonly products: Readonly<Record<string, number>>,
    private readonly tokenKek: Buffer,
    private readonly entitlements: Readonly<Record<string, string>> = {},
  ) {
    if (bindingSecret.length < 32) throw new Error('BILLING_BINDING_SECRET must contain at least 32 characters');
    if (tokenKek.length !== 32) throw new Error('PURCHASE_TOKEN_KEK must contain 32 bytes');
  }

  createIntent(walletId: string, productId: string, now = Date.now()): {
    intentId: string; obfuscatedAccountId: string; obfuscatedProfileId: string; expiresAt: number;
  } {
    const credits = this.products[productId];
    const known = this.isEntitlement(productId) ||
      (typeof credits === 'number' && Number.isSafeInteger(credits) && credits > 0);
    if (!known) throw new Error('PURCHASE_PRODUCT_UNKNOWN');
    const wallet = this.db.prepare('SELECT id FROM wallet WHERE id = ? AND deleted_at IS NULL').get(walletId);
    if (!wallet) throw new Error('WALLET_NOT_FOUND');
    const intentId = randomUUID();
    const accountId = this.binding('wallet:', walletId);
    const profileId = this.binding('intent:', intentId);
    const expiresAt = now + intentLifetimeMs;
    this.db.prepare(`INSERT INTO purchase_intent
      (id, wallet_id, product_id, account_hash, profile_hash, expires_at)
      VALUES (?, ?, ?, ?, ?, ?)`).run(intentId, walletId, productId, accountId, profileId, expiresAt);
    return { intentId, obfuscatedAccountId: accountId, obfuscatedProfileId: profileId, expiresAt };
  }

  intentBinding(intentId: string): { obfuscatedAccountId: string; obfuscatedProfileId: string } {
    const intent = this.db.prepare('SELECT wallet_id FROM purchase_intent WHERE id = ?').get(intentId) as
      { wallet_id: string } | undefined;
    if (!intent) throw new Error('PURCHASE_INTENT_INVALID');
    return {
      obfuscatedAccountId: this.binding('wallet:', intent.wallet_id),
      obfuscatedProfileId: this.binding('intent:', intentId),
    };
  }

  async confirm(
    walletId: string,
    intentId: string,
    productId: string,
    purchaseToken: string,
    now = Date.now(),
  ): Promise<{ credited: number; balance: number; state: 'CREDITED' }> {
    if (!purchaseToken || purchaseToken.length > 4096) throw new Error('PURCHASE_INVALID');
    const intent = this.db.prepare(`SELECT id, wallet_id, product_id, account_hash, profile_hash, expires_at, used_at
      FROM purchase_intent WHERE id = ?`).get(intentId) as {
      id: string; wallet_id: string; product_id: string; account_hash: string; profile_hash: string;
      expires_at: number; used_at: number | null;
    } | undefined;
    const digest = tokenHash(purchaseToken);
    const prior = this.db.prepare(`SELECT wallet_id, product_id, state FROM purchase WHERE token_hash = ?`)
      .get(digest) as { wallet_id: string; product_id: string; state: string } | undefined;
    if (!intent || intent.wallet_id !== walletId || intent.product_id !== productId ||
        (intent.expires_at <= now && !prior) ||
        (this.products[productId] === undefined && !this.isEntitlement(productId))) {
      throw new Error('PURCHASE_INTENT_INVALID');
    }

    const purchase = await this.store.getPurchase(productId, purchaseToken);
    if (purchase.packageName !== this.packageName || purchase.productId !== productId ||
        purchase.quantity !== 1 ||
        purchase.obfuscatedAccountId !== intent.account_hash ||
        purchase.obfuscatedProfileId !== intent.profile_hash) {
      throw new Error('PURCHASE_INVALID');
    }
    if (purchase.state === 'PENDING') throw new Error('PURCHASE_PENDING');
    if (purchase.state !== 'PURCHASED') throw new Error('PURCHASE_INVALID');

    let record = this.db.prepare('SELECT wallet_id, product_id, quantity, state FROM purchase WHERE token_hash = ?')
      .get(digest) as { wallet_id: string; product_id: string; quantity: number; state: string } | undefined;
    if (record) {
      if (record.wallet_id !== walletId || record.product_id !== productId) throw new Error('PURCHASE_INVALID');
    } else {
      if (intent.used_at !== null) throw new Error('PURCHASE_INTENT_USED');
      const entitlement = this.isEntitlement(productId);
      const credits = entitlement ? 0 : this.products[productId]!;
      this.db.transaction(() => {
        const currentIntent = this.db.prepare(`SELECT used_at FROM purchase_intent
          WHERE id = ? AND wallet_id = ? AND expires_at > ?`).get(intentId, walletId, now) as
          { used_at: number | null } | undefined;
        if (!currentIntent || currentIntent.used_at !== null) throw new Error('PURCHASE_INTENT_USED');
        this.db.prepare(`INSERT INTO purchase
          (token_hash, product_id, order_id, wallet_id, package_name, purchase_state,
           state, quantity, created_at, intent_id, token_ciphertext, token_wrapped_key, next_attempt_at)
          VALUES (?, ?, ?, ?, ?, 'PURCHASED', 'PLAY_FINALIZING', 1, ?, ?, ?, ?, ?)`)
          .run(digest, productId, purchase.orderId, walletId, this.packageName, now, intentId,
            ...this.encryptToken(purchaseToken), now);
        if (entitlement) this.applyUnlock(walletId, now);
        else this.applyPurchasedCredits(walletId, digest, credits, now);
        this.db.prepare('UPDATE purchase_intent SET used_at = ? WHERE id = ?').run(now, intentId);
      }).immediate();
      record = { wallet_id: walletId, product_id: productId, quantity: 1, state: 'PLAY_FINALIZING' };
    }

    if (record.state === 'VOIDED') throw new Error('PURCHASE_INVALID');
    const entitlementPurchase = this.isEntitlement(productId);
    if (record.state !== 'CONSUMED' && record.state !== 'ACKNOWLEDGED' &&
        !(entitlementPurchase && record.state === 'FULFILLED')) {
      try {
        if (entitlementPurchase) {
          await this.store.acknowledge(productId, purchaseToken);
        } else {
          const authoritative = await this.store.getPurchase(productId, purchaseToken);
          if (authoritative.consumptionState !== 'CONSUMED') await this.store.consume(productId, purchaseToken);
        }
      } catch (error) {
        this.db.prepare(`UPDATE purchase SET last_error = 'PLAY_FINALIZE_FAILED', next_attempt_at = ?
          WHERE token_hash = ?`).run(Date.now() + 60_000, digest);
        throw Object.assign(new Error('PURCHASE_FINALIZATION_PENDING'), { cause: error });
      }
      this.db.prepare(`UPDATE purchase SET state = ?, finalized_at = ?, token_ciphertext = NULL,
        token_wrapped_key = NULL, last_error = NULL WHERE token_hash = ? AND wallet_id = ?`)
        .run(entitlementPurchase ? 'FULFILLED' : 'CONSUMED', Date.now(), digest, walletId);
    }
    const wallet = this.db.prepare('SELECT balance FROM wallet WHERE id = ?').get(walletId) as
      { balance: number } | undefined;
    return { credited: entitlementPurchase ? 0 : this.products[productId]!, balance: wallet?.balance ?? 0, state: 'CREDITED' };
  }

  async retryFinalizations(now = Date.now()): Promise<number> {
    const pending = this.db.prepare(`SELECT token_hash, product_id, wallet_id, intent_id,
        token_ciphertext, token_wrapped_key
      FROM purchase WHERE state = 'PLAY_FINALIZING' AND next_attempt_at <= ?`)
      .all(now) as Array<{
        token_hash: string; product_id: string; wallet_id: string; intent_id: string;
        token_ciphertext: Buffer | null; token_wrapped_key: Buffer | null;
      }>;
    let finalized = 0;
    for (const row of pending) {
      if (!row.token_ciphertext || !row.token_wrapped_key) throw new Error('PURCHASE_TOKEN_CIPHERTEXT_MISSING');
      const purchaseToken = this.decryptToken(row.token_ciphertext, row.token_wrapped_key);
      const authoritative = await this.store.getPurchase(row.product_id, purchaseToken);
      const intent = this.db.prepare(`SELECT account_hash, profile_hash FROM purchase_intent WHERE id = ?
        AND wallet_id = ? AND product_id = ?`).get(row.intent_id, row.wallet_id, row.product_id) as
        { account_hash: string; profile_hash: string } | undefined;
      if (!intent || authoritative.packageName !== this.packageName ||
          authoritative.productId !== row.product_id || authoritative.state !== 'PURCHASED' ||
          authoritative.obfuscatedAccountId !== intent.account_hash ||
          authoritative.obfuscatedProfileId !== intent.profile_hash) {
        throw new Error('PURCHASE_FINALIZATION_OWNERSHIP_MISMATCH');
      }
      try {
        const entitlement = this.isEntitlement(row.product_id);
        if (entitlement) {
          await this.store.acknowledge(row.product_id, purchaseToken);
        } else if (authoritative.consumptionState !== 'CONSUMED') {
          await this.store.consume(row.product_id, purchaseToken);
        }
        this.db.prepare(`UPDATE purchase SET state = ?, finalized_at = ?, token_ciphertext = NULL,
          token_wrapped_key = NULL, next_attempt_at = NULL, last_error = NULL WHERE token_hash = ?`)
          .run(entitlement ? 'FULFILLED' : 'CONSUMED', now, row.token_hash);
        finalized += 1;
      } catch {
        this.db.prepare(`UPDATE purchase SET next_attempt_at = ?, last_error = 'PLAY_FINALIZE_FAILED'
          WHERE token_hash = ?`).run(now + 60_000, row.token_hash);
      }
    }
    return finalized;
  }

  async processRtdn(event: RtdnPurchaseEvent): Promise<{ duplicate: boolean }> {
    const seen = this.db.prepare('SELECT 1 FROM rtdn_message WHERE message_id = ?').get(event.messageId);
    if (seen) return { duplicate: true };
    if (!event.messageId || event.messageId.length > 256 || !Number.isSafeInteger(event.eventTime)) {
      throw new Error('RTDN_INVALID');
    }
    const authoritative = await this.store.getPurchase(event.productId, event.purchaseToken);
    if (authoritative.packageName !== this.packageName || authoritative.productId !== event.productId) {
      throw new Error('PURCHASE_INVALID');
    }
    const digest = tokenHash(event.purchaseToken);
    if (event.type === 'PURCHASED') {
      if (authoritative.state !== 'PURCHASED') throw new Error('PURCHASE_INVALID');
      const intent = this.db.prepare(`SELECT id, wallet_id FROM purchase_intent
        WHERE product_id = ? AND profile_hash = ? AND account_hash = ? AND expires_at > ?
        ORDER BY expires_at DESC LIMIT 1`).get(event.productId, authoritative.obfuscatedProfileId,
        authoritative.obfuscatedAccountId, Date.now()) as { id: string; wallet_id: string } | undefined;
      if (intent) await this.confirm(intent.wallet_id, intent.id, event.productId, event.purchaseToken);
    } else if (event.type === 'VOIDED' || event.type === 'CANCELED') {
      if (event.type === 'VOIDED' && authoritative.state !== 'VOIDED') throw new Error('PURCHASE_NOT_VOIDED');
      if (event.type === 'CANCELED' && authoritative.state !== 'CANCELED' && authoritative.state !== 'VOIDED') {
        throw new Error('PURCHASE_NOT_CANCELED');
      }
      if (authoritative.state === 'VOIDED' || authoritative.state === 'CANCELED') {
        this.clawBack(digest, 1, event.messageId, Date.now());
      }
    } else {
      throw new Error('RTDN_INVALID');
    }
    this.db.prepare(`INSERT INTO rtdn_message(message_id, event_time, notification_type, processed_at)
      VALUES (?, ?, ?, ?)`).run(event.messageId, event.eventTime, event.type, Date.now());
    return { duplicate: false };
  }

  async reconcileVoided(now = Date.now()): Promise<{ processed: number; pageToken: string | null }> {
    this.db.prepare(`INSERT OR IGNORE INTO play_reconcile(id, overlap_from) VALUES (1, ?)`)
      .run(now - 48 * 60 * 60 * 1000);
    const state = this.db.prepare('SELECT page_token, overlap_from FROM play_reconcile WHERE id = 1')
      .get() as { page_token: string | null; overlap_from: number };
    const page = await this.store.listVoidedPurchases(state.overlap_from, state.page_token ?? undefined);
    let processed = 0;
    for (const item of page.purchases) {
      const digest = tokenHash(item.purchaseToken);
      const messageId = `voided:${digest}:${item.voidedQuantity}`;
      if (this.db.prepare('SELECT 1 FROM rtdn_message WHERE message_id = ?').get(messageId)) continue;
      const authoritative = await this.store.getPurchase(item.productId, item.purchaseToken);
      if (authoritative.packageName !== this.packageName || authoritative.productId !== item.productId ||
          !Number.isSafeInteger(item.voidedQuantity) ||
          item.voidedQuantity < 1 || item.voidedQuantity > authoritative.quantity) {
        throw new Error('VOIDED_PURCHASE_INVALID');
      }
      const stored = this.db.prepare('SELECT product_id, quantity FROM purchase WHERE token_hash = ?')
        .get(digest) as { product_id: string; quantity: number } | undefined;
      if (!stored || stored.product_id !== item.productId || item.voidedQuantity > stored.quantity) {
        throw new Error('VOIDED_PURCHASE_INVALID');
      }
      this.clawBack(digest, item.voidedQuantity, messageId, now);
      this.db.prepare(`INSERT INTO rtdn_message(message_id, event_time, notification_type, processed_at)
        VALUES (?, ?, 'VOIDED_SWEEP', ?)`).run(messageId, now, now);
      processed += 1;
    }
    this.db.prepare(`UPDATE play_reconcile SET page_token = ?, last_success_at = ?, last_error = NULL,
      overlap_from = CASE WHEN ? IS NULL THEN ? ELSE overlap_from END WHERE id = 1`)
      .run(page.pageToken ?? null, now, page.pageToken ?? null, now - 48 * 60 * 60 * 1000);
    return { processed, pageToken: page.pageToken ?? null };
  }

  private binding(prefix: string, id: string): string {
    return createHmac('sha256', this.bindingSecret).update(`${prefix}${id}`).digest('base64url');
  }

  private encryptToken(token: string): [Buffer, Buffer] {
    const dataKey = randomBytes(32);
    const dataIv = randomBytes(12);
    const dataCipher = createCipheriv('aes-256-gcm', dataKey, dataIv);
    const ciphertext = Buffer.concat([dataCipher.update(token, 'utf8'), dataCipher.final()]);
    const encryptedToken = Buffer.concat([dataIv, dataCipher.getAuthTag(), ciphertext]);
    const keyIv = randomBytes(12);
    const keyCipher = createCipheriv('aes-256-gcm', this.tokenKek, keyIv);
    const encryptedKey = Buffer.concat([keyCipher.update(dataKey), keyCipher.final()]);
    const wrappedKey = Buffer.concat([keyIv, keyCipher.getAuthTag(), encryptedKey]);
    dataKey.fill(0);
    return [encryptedToken, wrappedKey];
  }

  private decryptToken(ciphertext: Buffer, wrappedKey: Buffer): string {
    if (ciphertext.length < 29 || wrappedKey.length !== 60) throw new Error('PURCHASE_TOKEN_CIPHERTEXT_INVALID');
    const keyIv = wrappedKey.subarray(0, 12);
    const keyTag = wrappedKey.subarray(12, 28);
    const keyCipher = createDecipheriv('aes-256-gcm', this.tokenKek, keyIv);
    keyCipher.setAuthTag(keyTag);
    const dataKey = Buffer.concat([keyCipher.update(wrappedKey.subarray(28)), keyCipher.final()]);
    const dataIv = ciphertext.subarray(0, 12);
    const dataTag = ciphertext.subarray(12, 28);
    const dataCipher = createDecipheriv('aes-256-gcm', dataKey, dataIv);
    dataCipher.setAuthTag(dataTag);
    const token = Buffer.concat([dataCipher.update(ciphertext.subarray(28)), dataCipher.final()]).toString('utf8');
    dataKey.fill(0);
    return token;
  }

  private isEntitlement(productId: string): boolean {
    return Object.hasOwn(this.entitlements, productId) && !Object.hasOwn(this.products, productId);
  }

  private applyUnlock(walletId: string, now: number): void {
    const result = this.db.prepare('UPDATE wallet SET unlocked_at = ? WHERE id = ? AND deleted_at IS NULL AND unlocked_at IS NULL')
      .run(now, walletId);
    if (result.changes === 0 &&
        !this.db.prepare('SELECT 1 FROM wallet WHERE id = ? AND deleted_at IS NULL').get(walletId)) {
      throw new Error('WALLET_NOT_FOUND');
    }
  }

  private applyPurchasedCredits(walletId: string, digest: string, credits: number, now: number): void {
    const wallet = this.db.prepare('SELECT balance, refund_debt FROM wallet WHERE id = ? AND deleted_at IS NULL')
      .get(walletId) as { balance: number; refund_debt: number } | undefined;
    if (!wallet) throw new Error('WALLET_NOT_FOUND');
    const repaid = Math.min(wallet.refund_debt, credits);
    const addedBalance = credits - repaid;
    this.db.prepare('UPDATE wallet SET balance = balance + ?, refund_debt = refund_debt - ? WHERE id = ?')
      .run(addedBalance, repaid, walletId);
    this.db.prepare(`INSERT INTO ledger(wallet_id, kind, delta, ref, idempotency_key, created_at)
      VALUES (?, 'PURCHASE', ?, ?, ?, ?)`)
      .run(walletId, addedBalance, digest, digest, now);
  }

  private clawBack(digest: string, voidedQuantity: number, idempotencyKey: string, now: number): void {
    const purchase = this.db.prepare(`SELECT wallet_id, product_id, quantity, voided_quantity, state
      FROM purchase WHERE token_hash = ?`).get(digest) as {
      wallet_id: string; product_id: string; quantity: number; voided_quantity: number; state: string;
    } | undefined;
    if (!purchase) return;
    const newVoidedQuantity = Math.max(purchase.voided_quantity, voidedQuantity);
    const quantityToClawBack = newVoidedQuantity - purchase.voided_quantity;
    if (quantityToClawBack <= 0) return;
    if (this.isEntitlement(purchase.product_id)) {
      this.db.transaction(() => {
        this.db.prepare('UPDATE wallet SET unlocked_at = NULL WHERE id = ?').run(purchase.wallet_id);
        this.db.prepare('UPDATE purchase SET voided_quantity = ?, state = ? WHERE token_hash = ?')
          .run(newVoidedQuantity, newVoidedQuantity >= purchase.quantity ? 'VOIDED' : purchase.state, digest);
      }).immediate();
      return;
    }
    const unitCredits = this.products[purchase.product_id];
    if (!unitCredits) throw new Error('PURCHASE_PRODUCT_UNKNOWN');
    const amount = unitCredits * quantityToClawBack;
    const wallet = this.db.prepare('SELECT balance FROM wallet WHERE id = ?').get(purchase.wallet_id) as
      { balance: number } | undefined;
    if (!wallet) throw new Error('WALLET_NOT_FOUND');
    const debit = Math.min(wallet.balance, amount);
    const debt = amount - debit;
    this.db.transaction(() => {
      this.db.prepare('UPDATE wallet SET balance = balance - ?, refund_debt = refund_debt + ? WHERE id = ?')
        .run(debit, debt, purchase.wallet_id);
      this.db.prepare(`INSERT INTO ledger(wallet_id, kind, delta, ref, idempotency_key, created_at)
        VALUES (?, 'REFUND_ADJUST', ?, ?, ?, ?)`)
        .run(purchase.wallet_id, -debit, digest, idempotencyKey, now);
      this.db.prepare(`UPDATE purchase SET voided_quantity = ?, state = ? WHERE token_hash = ?`)
        .run(newVoidedQuantity, newVoidedQuantity >= purchase.quantity ? 'VOIDED' : purchase.state, digest);
    }).immediate();
  }
}
