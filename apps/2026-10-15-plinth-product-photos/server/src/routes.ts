import { Ajv } from 'ajv';
import { createHash, createHmac, randomBytes, randomUUID } from 'node:crypto';
import { mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import type Database from 'better-sqlite3';
import type { FastifyInstance, FastifyReply, FastifyRequest } from 'fastify';
import sharp from 'sharp';
import apiSchema from '../schema/api.schema.json' with { type: 'json' };
import type { ServerConfig } from './config.js';
import { authenticate } from './auth.js';
import { validateExpandImageSize } from './providers/fal.js';
import type { CreditLedger } from './ledger.js';
import { onCompletedSend } from './delivery.js';
import { JobQueue } from './queue.js';
import type { ProviderConcurrency } from './provider-concurrency.js';
import { MODEL_REGISTRY, operationConfig, type OperationId, type ProviderAdapter } from './registry.js';
import { createChallenge, createIntegritySession, type IntegrityVerifier } from './integrity.js';
import { sessionIntegrityConfig } from './device-recall.js';
import type { PurchaseService } from './purchases.js';
import { recordOutputReport, reportStatus, removeWalletReports, type ReportCategory } from './moderation.js';
import { clientAppVersion, requireMinimumVersion, requirePaidWorkOpen } from './guards.js';
import { deviceLimit, type TokenBucketLimiter } from './rate-limit.js';
import { noticeFor, readOperatorState, resolveServiceState } from './service-state.js';

const validateReceipt = new Ajv().compile(apiSchema.definitions.DeliveryReceipt);
const validateRecognition = new Ajv().compile(apiSchema.definitions.ItemRecognitionResponse);
const apiRef = (name: string): string =>
  `https://plinth.invalid/server-api.schema.json#/definitions/${name}`;

type RoutesOptions = {
  db: Database.Database;
  config: ServerConfig;
  ledger: CreditLedger;
  queue: JobQueue | null;
  providerConcurrency: ProviderConcurrency;
  provider: ProviderAdapter | null;
  integrityVerifier: IntegrityVerifier | null;
  purchaseService: PurchaseService | null;
  rtdnIdentityVerifier: ((authorization: string | undefined, audience: string, email: string) => Promise<boolean>) | null;
  /** Shared with the network hook installed in `createApp`, so both keys live in one bucket table. */
  limiter: TokenBucketLimiter;
};

type AuthClaims = { sub: string; wid: string; gen: number; ver?: string; iat: number; exp: number };
type RequestBody = Record<string, unknown>;

/** A bounded, safe client-version header read. Anything else is `undefined` (which fails closed). */
function headerAppVersion(request: FastifyRequest): string | undefined {
  const raw = request.headers['x-app-version'];
  const value = Array.isArray(raw) ? raw[0] : raw;
  if (typeof value !== 'string') return undefined;
  const trimmed = value.trim();
  return /^[A-Za-z0-9.+_-]{1,64}$/.test(trimmed) ? trimmed : undefined;
}

function bodyRecord(request: FastifyRequest): RequestBody {
  return request.body as RequestBody;
}

function replyError(
  reply: FastifyReply,
  status: number,
  code: string,
  message: string,
  details: Record<string, unknown> = {},
): FastifyReply {
  return reply.code(status).send({ error: { code, message, retryAfterSec: null, details } });
}

function dailyCost(db: Database.Database): number {
  const day = new Date().toISOString().slice(0, 10);
  const row = db.prepare('SELECT cost_micro_usd FROM spend_day WHERE day = ?').get(day) as
    { cost_micro_usd: number } | undefined;
  return row?.cost_micro_usd ?? 0;
}

function reserveDailyCost(db: Database.Database, costMicroUsd: number, capMicroUsd: number): boolean {
  return db.transaction(() => {
    const day = new Date().toISOString().slice(0, 10);
    const current = db.prepare('SELECT cost_micro_usd FROM spend_day WHERE day = ?')
      .get(day) as { cost_micro_usd: number } | undefined;
    if ((current?.cost_micro_usd ?? 0) + costMicroUsd > capMicroUsd) return false;
    db.prepare(`INSERT INTO spend_day(day, cost_micro_usd, starter_cost_micro_usd)
      VALUES (?, ?, 0)
      ON CONFLICT(day) DO UPDATE SET cost_micro_usd = cost_micro_usd + excluded.cost_micro_usd`)
      .run(day, costMicroUsd);
    return true;
  }).immediate();
}

function generateRecoveryCode(): string {
  const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
  const bytes = randomBytes(10);
  let bits = 0;
  let value = 0;
  let output = '';
  for (const byte of bytes) {
    value = (value << 8) | byte;
    bits += 8;
    while (bits >= 5) {
      output += alphabet[(value >>> (bits - 5)) & 31];
      bits -= 5;
    }
  }
  return output;
}

function hashRecoveryCode(code: string, pepper: string): string {
  return createHmac('sha256', pepper).update(code.toUpperCase()).digest('hex');
}

export async function registerRoutes(app: FastifyInstance, options: RoutesOptions): Promise<void> {
  const { db, config, ledger, queue } = options;
  const auth = authenticate(db);
  const purchaseService = options.purchaseService;
  const minimumVersion = requireMinimumVersion(config);
  const paidWork = requirePaidWorkOpen(config);

  /**
   * One builder for `/config` and the session's embedded copy, so the two can
   * never disagree about the state. `appVersion` is undefined on `/config` when
   * the caller sent no header: the service then reports what the operator flags
   * say, and the signed `ver` claim still refuses an outdated build on every
   * route that can move credits.
   */
  const buildConfig = (appVersion: string | undefined) => {
    const operator = readOperatorState(config);
    const serviceState = resolveServiceState(operator, appVersion, config.minAppVersion);
    const dailyCapReached = dailyCost(db) >= config.dailySpendCapMicroUsd;
    return {
      ops: operationConfig(config, serviceState !== 'ok' || dailyCapReached),
      // Names this deployment. The separation audit compares the two public
      // prefixes through this field; without it both answered identically.
      environment: config.environmentName,
      starterCredits: 0,
      killSwitch: serviceState !== 'ok',
      minAppVersion: config.minAppVersion,
      dailyCapReached,
      serviceState,
      notice: noticeFor(serviceState),
    };
  };

  const health = async (_request: FastifyRequest, reply: FastifyReply) => {
    /**
     * A dead database must make the probe fail, not just a dead process. A real
     * read of the schema file is the cheapest query that fails on a corrupt or
     * unreachable database, and it does not take a write lock on every probe.
     * A read-only filesystem still answers: that failure is left to the write
     * path and is stated as a limit in docs/release-operations.md.
     */
    let databaseReachable = true;
    try {
      db.prepare('SELECT count(*) AS tables FROM sqlite_master').get();
    } catch {
      databaseReachable = false;
    }
    return reply.code(databaseReachable ? 200 : 503).send({
      ok: databaseReachable,
      service: 'plinth-product-photos-server',
      version: '1.0.0',
      time: Date.now(),
      databaseReachable,
      serviceState: readOperatorState(config),
    });
  };
  for (const path of ['/health', '/v1/health']) {
    app.get(path, {
      schema: {
        response: {
          200: { $ref: apiRef('HealthResponse') },
          503: { $ref: apiRef('HealthUnavailableResponse') },
        },
      },
    }, health);
  }

  const configHandler = async (request: FastifyRequest, reply: FastifyReply) => {
    reply.header('Cache-Control', 'public, max-age=60');
    return reply.send(buildConfig(headerAppVersion(request)));
  };
  for (const path of ['/config', '/v1/config']) app.get(path, configHandler);

  app.post('/v1/integrity/challenge', {
    schema: {
      body: { $ref: apiRef('IntegrityChallengeRequest') },
      response: { 200: { $ref: apiRef('IntegrityChallengeResponse') } },
    },
  }, async (request, reply) => {
    if (!options.integrityVerifier) {
      return replyError(reply, 503, 'NOT_CONFIGURED', 'Play Integrity verification is not configured.');
    }
    const body = bodyRecord(request);
    try {
      const challenge = createChallenge(db, body['installId'] as string, body['appVersion'] as string,
        (body['locale'] as string | undefined) ?? 'en-US');
      return reply.code(200).send(challenge);
    } catch {
      return replyError(reply, 400, 'INVALID_SESSION_FIELDS', 'The session fields are invalid.');
    }
  });
  app.post('/v1/session', {
    schema: {
      body: { $ref: apiRef('SessionRequest') },
      response: { 200: { $ref: apiRef('SessionResponse') } },
    },
  }, async (request, reply) => {
    const integrityConfig = sessionIntegrityConfig(config);
    if (!options.integrityVerifier || !integrityConfig) {
      return replyError(reply, 503, 'NOT_CONFIGURED', 'Play Integrity verification is not configured.');
    }
    const body = bodyRecord(request);
    try {
      const result = await createIntegritySession(db, {
        challengeId: body['challengeId'] as string,
        challenge: body['challenge'] as string,
        installId: body['installId'] as string,
        appVersion: body['appVersion'] as string,
        locale: body['locale'] as string,
        integrityToken: body['integrityToken'] as string,
      }, options.integrityVerifier, options.integrityVerifier.recall ?? null, integrityConfig);
      const deviceToken = app.jwt.sign(
        { sub: result.deviceId, wid: result.walletId, gen: 0, ver: result.appVersion },
        { expiresIn: '30d' },
      );
      const sessionConfig = buildConfig(result.appVersion);
      return reply.send({
        deviceToken,
        walletId: result.walletId,
        balance: result.balance,
        starterGranted: result.starterGranted,
        config: sessionConfig,
      });
    } catch (error) {
      const code = error instanceof Error ? error.message : 'INTEGRITY_VERIFICATION_FAILED';
      if (code.startsWith('INTEGRITY_CHALLENGE_') || code === 'INTEGRITY_REQUEST_HASH_MISMATCH' ||
          code.startsWith('INTEGRITY_PACKAGE_') || code.startsWith('INTEGRITY_VERSION_') ||
          code.startsWith('INTEGRITY_CERTIFICATE_') || code.startsWith('INTEGRITY_APP_') ||
          code.startsWith('INTEGRITY_LICENSE_')) {
        // The client receives one generic code, but the operator needs to know which
        // claim failed: on 2026-10-21 three session attempts answered 400 and the log
        // named none of them, which cost a diagnosis cycle that a single line answers.
        request.log.warn({ requestId: request.id, integrityCode: code }, 'integrity claim rejected');
        return replyError(reply, code === 'INTEGRITY_CHALLENGE_REPLAYED' ? 409 : 400,
          'INTEGRITY_INVALID', 'The Play Integrity response could not be verified.');
      }
      request.log.error({ requestId: request.id, code: 'INTEGRITY_PROVIDER_ERROR' }, 'integrity verification failed');
      return replyError(reply, 503, 'INTEGRITY_UNAVAILABLE', 'Play Integrity verification is temporarily unavailable.');
    }
  });

  app.get('/v1/wallet', {
    preHandler: [auth, deviceLimit(options.limiter, 'walletRead')],
    schema: { response: { 200: { $ref: apiRef('WalletResponse') } } },
  },
    async (request, reply) => {
      const claims = request.user as AuthClaims;
      const wallet = db.prepare(
        'SELECT balance, refund_debt, purchase_hold, unlocked_at FROM wallet WHERE id = ? AND deleted_at IS NULL',
      ).get(claims.wid) as
        { balance: number; refund_debt: number; purchase_hold: number; unlocked_at: number | null } | undefined;
      if (!wallet) return replyError(reply, 404, 'WALLET_NOT_FOUND', 'Wallet is not available.');
      const reserved = db.prepare(
        `SELECT COALESCE(SUM(reserved - charged), 0) AS value
         FROM job WHERE wallet_id = ? AND state IN ('QUEUED', 'RUNNING', 'SUCCEEDED')`,
      ).get(claims.wid) as { value: number };
      const entries = db.prepare(
        'SELECT kind, delta, created_at AS createdAt FROM ledger WHERE wallet_id = ? ORDER BY created_at DESC LIMIT 50',
      ).all(claims.wid);
      return reply.send({
        balance: wallet.balance, reserved: reserved.value, refundDebt: wallet.refund_debt,
        purchaseHold: wallet.purchase_hold, unlocked: wallet.unlocked_at !== null, entries,
      });
    });

  app.post('/v1/purchases/intents', {
    preHandler: [auth, minimumVersion, paidWork, deviceLimit(options.limiter, 'purchaseIntent')],
    schema: {
      body: { $ref: apiRef('PurchaseIntentRequest') },
      response: { 201: { $ref: apiRef('PurchaseIntentResponse') } },
    },
  }, async (request, reply) => {
    if (!purchaseService) return replyError(reply, 503, 'NOT_CONFIGURED', 'The Play product catalogue is not configured.');
    const claims = request.user as AuthClaims;
    try {
      return reply.code(201).send(purchaseService.createIntent(claims.wid, bodyRecord(request)['productId'] as string));
    } catch (error) {
      if (error instanceof Error && error.message === 'PURCHASE_PRODUCT_UNKNOWN') {
        return replyError(reply, 400, 'PURCHASE_PRODUCT_UNKNOWN', 'The requested Play product is not available.');
      }
      throw error;
    }
  });
  app.post('/v1/purchases/confirm', {
    preHandler: [auth, minimumVersion, paidWork, deviceLimit(options.limiter, 'purchaseConfirm')],
    schema: {
      body: { $ref: apiRef('PurchaseConfirmRequest') },
      response: { 200: { $ref: apiRef('PurchaseConfirmResponse') } },
    },
  }, async (request, reply) => {
    if (!purchaseService) return replyError(reply, 503, 'NOT_CONFIGURED', 'Play purchase verification is not configured.');
    const claims = request.user as AuthClaims;
    const body = bodyRecord(request);
    try {
      return reply.send(await purchaseService.confirm(claims.wid,
        body['intentId'] as string, body['productId'] as string, body['purchaseToken'] as string));
    } catch (error) {
      const code = error instanceof Error ? error.message : 'PURCHASE_INVALID';
      if (code === 'PURCHASE_PENDING') return replyError(reply, 409, code, 'The Play purchase is still pending.');
      if (code === 'PURCHASE_FINALIZATION_PENDING') {
        return replyError(reply, 503, code, 'The purchase is verified and will be finalized by the server.');
      }
      if (code.startsWith('PURCHASE_')) return replyError(reply, 400, 'PURCHASE_INVALID', 'The purchase could not be verified.');
      request.log.error({ requestId: request.id, code: 'PURCHASE_PROVIDER_ERROR' }, 'purchase processing failed');
      return replyError(reply, 503, 'PURCHASE_UNAVAILABLE', 'Play purchase verification is temporarily unavailable.');
    }
  });

  app.post('/v1/wallet/recover', {
    schema: { body: { $ref: apiRef('RecoveryRequest') } },
  }, async (_request, reply) => replyError(reply, 503, 'NOT_CONFIGURED', 'Recovery requires a verified device session.'));

  app.post('/v1/wallet/recovery-code', {
    preHandler: [auth, minimumVersion, paidWork, deviceLimit(options.limiter, 'recoveryCode')],
  }, async (request, reply) => {
    const recoveryPepper = config.recoveryPepper;
    if (!recoveryPepper) {
      return replyError(reply, 503, 'NOT_CONFIGURED', 'Recovery-code support is not configured.');
    }
    const claims = request.user as AuthClaims;
    const code = generateRecoveryCode();
    db.prepare('UPDATE wallet SET recovery_hash = ? WHERE id = ? AND deleted_at IS NULL')
      .run(hashRecoveryCode(code, recoveryPepper), claims.wid);
    return reply.send({ code });
  });

  app.put('/v1/uploads', {
    preHandler: [auth, minimumVersion, paidWork, deviceLimit(options.limiter, 'upload')],
  }, async (request, reply) => {
    const claims = request.user as AuthClaims;
    const contentType = String(request.headers['content-type'] ?? '').split(';')[0]?.trim().toLowerCase() || '';
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(contentType)) {
      return replyError(reply, 415, 'UNSUPPORTED_MEDIA', 'Upload must be JPEG, PNG, or WebP.');
    }
    const bytes = request.body as Buffer;
    if (!Buffer.isBuffer(bytes) || bytes.length === 0 || bytes.length > 4 * 1024 * 1024) {
      return replyError(reply, 413, 'TOO_LARGE', 'Upload must be a non-empty image no larger than 4 MiB.');
    }
    const declared = request.headers['x-client-sha256'];
    const sha256 = createHash('sha256').update(bytes).digest('hex');
    if (typeof declared !== 'string' || declared.toLowerCase() !== sha256) {
      return replyError(reply, 400, 'UPLOAD_HASH_MISMATCH', 'The upload digest does not match its bytes.');
    }
    let metadata: sharp.Metadata;
    try {
      metadata = await sharp(bytes, { limitInputPixels: 40_000_000 }).metadata();
    } catch {
      return replyError(reply, 415, 'UNSUPPORTED_MEDIA', 'The upload bytes are not a supported image.');
    }
    const formats: Record<string, string> = { jpeg: 'image/jpeg', png: 'image/png', webp: 'image/webp' };
    if (!metadata.format || formats[metadata.format] !== contentType) {
      return replyError(reply, 415, 'UNSUPPORTED_MEDIA', 'The image signature does not match Content-Type.');
    }
    const id = randomUUID();
    const expiresAt = Date.now() + 60 * 60 * 1000;
    const uploadDir = join(config.dataDir, 'uploads');
    const path = join(uploadDir, id);
    await mkdir(uploadDir, { recursive: true });
    await writeFile(path, bytes, { flag: 'wx' });
    try {
      db.prepare(`INSERT INTO upload(id, wallet_id, path, sha256, bytes, created_at, expires_at)
        VALUES (?, ?, ?, ?, ?, ?, ?)`)
        .run(id, claims.wid, path, sha256, bytes.length, Date.now(), expiresAt);
    } catch (error) {
      await rm(path, { force: true });
      throw error;
    }
    return reply.code(201).send({ uploadId: id, expiresAt });
  });

  app.post('/v1/recognize', {
    preHandler: [auth, minimumVersion, paidWork, deviceLimit(options.limiter, 'recognize')],
    schema: { response: { 200: { $ref: apiRef('ItemRecognitionResponse') } } },
  }, async (request, reply) => {
    /**
     * A second read of the operator state, after the `paidWork` guard and before
     * the provider call. The guard is the normal refusal; this narrows the window
     * in which a pause can land between the guard and a billed request. It reads
     * the same function, so the two cannot disagree about which flags mean what.
     */
    const operator = readOperatorState(config);
    if (operator !== 'ok') {
      return replyError(reply, 503, operator === 'closed' ? 'SERVICE_CLOSED' : 'SERVICE_MAINTENANCE',
        'Model operations are not accepted while the service is paused or closed.');
    }
    if (!config.falKey) {
      return replyError(reply, 503, 'NOT_CONFIGURED', 'The model provider is not configured.');
    }
    const model = MODEL_REGISTRY['O-01'];
    if (!model.enabled || !model.endpointId || !options.provider?.recognize) {
      return replyError(reply, 503, 'NOT_CONFIGURED', 'Item recognition is not configured.');
    }
    const contentType = String(request.headers['content-type'] ?? '').split(';')[0]?.trim().toLowerCase() || '';
    if (contentType !== 'image/jpeg') {
      return replyError(reply, 415, 'UNSUPPORTED_MEDIA', 'Recognition requires a JPEG image.');
    }
    const image = request.body as Buffer;
    if (!Buffer.isBuffer(image) || image.length === 0 || image.length > 4 * 1024 * 1024) {
      return replyError(reply, 413, 'TOO_LARGE', 'Recognition requires a non-empty JPEG no larger than 4 MiB.');
    }
    const declared = request.headers['x-client-sha256'];
    const imageHash = createHash('sha256').update(image).digest('hex');
    if (typeof declared !== 'string' || declared.toLowerCase() !== imageHash) {
      return replyError(reply, 400, 'UPLOAD_HASH_MISMATCH', 'The image digest does not match its bytes.');
    }
    let metadata: sharp.Metadata;
    try {
      metadata = await sharp(image, { limitInputPixels: 40_000_000 }).metadata();
    } catch {
      return replyError(reply, 415, 'UNSUPPORTED_MEDIA', 'The image bytes are not a supported JPEG.');
    }
    if (metadata.format !== 'jpeg' || !metadata.width || !metadata.height ||
        Math.max(metadata.width, metadata.height) !== 1024) {
      return replyError(reply, 415, 'UNSUPPORTED_MEDIA', 'Recognition requires a JPEG with a 1024 px longest side.');
    }
    if (!options.providerConcurrency.tryAcquire()) {
      return replyError(reply, 429, 'RATE_LIMITED', 'Recognition is temporarily at capacity.');
    }
    if (model.costMicroUsd === null ||
        !reserveDailyCost(db, model.costMicroUsd, config.dailySpendCapMicroUsd)) {
      options.providerConcurrency.release();
      return replyError(reply, 503, 'CAP_REACHED', 'The daily provider spending limit has been reached.');
    }

    try {
      const params = { temperature: 0, responseSchema: apiSchema.definitions.ItemRecognitionResponse };
      for (let attempt = 0; attempt < 2; attempt += 1) {
        const answer = await options.provider.recognize(image, params);
        if (validateRecognition(answer)) return reply.code(200).send(answer);
        // Annex E O-01 permits one schema-only retry; it is the sole exception to the one-call-per-item budget.
      }
      return reply.code(200).send({
        category: 'other',
        confidence: 0,
        attributes: [],
        visibleText: [],
        scaleHint: { kind: 'none' },
      });
    } catch {
      request.log.error({ requestId: request.id, code: 'RECOGNITION_PROVIDER_FAILED' },
        'item recognition provider failed');
      return replyError(reply, 503, 'PROVIDER_FAILED', 'Item recognition is temporarily unavailable.');
    } finally {
      options.providerConcurrency.release();
    }
  });

  app.post('/v1/jobs', {
    preHandler: [auth, minimumVersion, paidWork, deviceLimit(options.limiter, 'jobCreate')],
    schema: {
      body: { $ref: apiRef('JobRequest') },
      response: { 202: { $ref: apiRef('JobAccepted') } },
    },
  }, async (request, reply) => {
    const claims = request.user as AuthClaims;
    const body = bodyRecord(request);
    const op = body.op as OperationId;
    const model = MODEL_REGISTRY[op];
    if (op === 'O-01') {
      return replyError(reply, 400, 'INVALID_OPERATION_INPUT', 'Recognition uses POST /v1/recognize.');
    }
    if (body.estimateCredits !== model.credits) {
      return replyError(reply, 409, 'PRICE_CHANGED', 'The current operation cost differs from the estimate.',
        { credits: model.credits });
    }
    if (!model.enabled) return replyError(reply, 503, 'OPERATION_DISABLED', 'This operation is not currently available.');
    const operator = readOperatorState(config);
    if (operator !== 'ok') {
      return replyError(reply, 503, operator === 'closed' ? 'SERVICE_CLOSED' : 'SERVICE_MAINTENANCE',
        'Paid operations are not accepted while the service is paused or closed.');
    }
    if (!options.provider || !queue) {
      return replyError(reply, 503, 'NOT_CONFIGURED', 'The model provider is not configured.');
    }
    if (model.costMicroUsd === null ||
        dailyCost(db) + model.costMicroUsd > config.dailySpendCapMicroUsd) {
      return replyError(reply, 503, 'CAP_REACHED', 'The daily provider spending limit has been reached.');
    }

    const uploadIds = body.uploadIds as string[];
    /**
     * Schema-independent ceilings. `params` is an open object because each model
     * names its own keys, so its size is bounded here (8 KiB is far above the
     * largest documented parameter set and far below anything that strains the
     * queue or the provider). A prompt is client-supplied text forwarded to a
     * billed provider call, so it is capped at 2 000 characters — the Annex E
     * templates are all well under that, and an unbounded one is a cost lever.
     */
    const params = body.params as Record<string, unknown>;
    if (JSON.stringify(params).length > 8 * 1024) {
      return replyError(reply, 400, 'INVALID_OPERATION_INPUT', 'The operation parameters are too large.');
    }
    if (Object.hasOwn(params, 'prompt')) {
      const prompt = params['prompt'];
      if (typeof prompt !== 'string' || prompt.length > 2_000) {
        return replyError(reply, 400, 'INVALID_OPERATION_INPUT', 'A prompt must be a string of at most 2000 characters.');
      }
    }
    const requiredUploads = op === 'O-12' ? 2 : 1;
    if (uploadIds.length !== requiredUploads) {
      return replyError(reply, 400, 'INVALID_OPERATION_INPUT',
        op === 'O-12' ? 'Erase requires a product image and a mask.' : 'This tool requires exactly one selected product image.');
    }
    if (op === 'O-02' && Object.hasOwn(params, 'prompt')) {
      return replyError(reply, 400, 'INVALID_OPERATION_INPUT', 'Cut-out requires exactly one image and no prompt.');
    }
    const placeholders = uploadIds.map(() => '?').join(',');
    const uploads = db.prepare(`SELECT id, path, expires_at FROM upload
      WHERE wallet_id = ? AND deleted_at IS NULL AND expires_at > ? AND id IN (${placeholders})`)
      .all(claims.wid, Date.now(), ...uploadIds) as Array<{ id: string; path: string; expires_at: number }>;
    if (uploads.length !== uploadIds.length) {
      return replyError(reply, 400, 'UPLOAD_NOT_AVAILABLE', 'One or more uploads are unavailable or expired.');
    }
    const orderedUploads = uploadIds.map((id) => uploads.find((upload) => upload.id === id));
    if (orderedUploads.some((upload) => !upload)) {
      return replyError(reply, 400, 'UPLOAD_NOT_AVAILABLE', 'One or more uploads are unavailable or expired.');
    }
    const image = await readFile(orderedUploads[0]!.path);
    if (op === 'O-09') {
      try {
        await validateExpandImageSize(image, params);
      } catch (error) {
        if (error instanceof Error &&
            ['PROVIDER_CANVAS_SIZE_REQUIRED', 'PROVIDER_IMAGE_GEOMETRY_INVALID',
              'PROVIDER_ORIGINAL_IMAGE_SIZE_MISMATCH'].includes(error.message)) {
          return replyError(reply, 400, 'INVALID_OPERATION_INPUT', 'Expand requires valid canvas and source-image geometry.');
        }
        throw error;
      }
    }
    let maskBase64: string | undefined;
    if (op === 'O-12') {
      // Bria mask polarity: white (255) erases; black (0) keeps the source pixel.
      const maskBytes = await readFile(orderedUploads[1]!.path);
      let maskMetadata: sharp.Metadata;
      try {
        maskMetadata = await sharp(maskBytes, { limitInputPixels: 40_000_000 }).metadata();
      } catch {
        return replyError(reply, 415, 'UNSUPPORTED_MEDIA', 'The eraser mask is not a supported image.');
      }
      const imageMetadata = await sharp(image, { limitInputPixels: 40_000_000 }).metadata();
      if (!maskMetadata.width || !maskMetadata.height ||
          maskMetadata.width !== imageMetadata.width || maskMetadata.height !== imageMetadata.height) {
        return replyError(reply, 400, 'INVALID_OPERATION_INPUT', 'The eraser mask must match the product image dimensions.');
      }
      maskBase64 = await sharp(maskBytes, { limitInputPixels: 40_000_000 })
        .greyscale().png().toBuffer().then((bytes) => bytes.toString('base64'));
    }
    const jobId = randomUUID();
    try {
      const accepted = await queue.enqueue({
        jobId,
        walletId: claims.wid,
        clientRequestId: body.clientRequestId,
        op,
        imageBase64: image.toString('base64'),
        ...(maskBase64 ? { maskBase64 } : {}),
        uploadIds,
        params,
        credits: model.credits,
      });
      return reply.code(202).send({
        jobId: accepted['jobId'] as string,
        reserved: accepted['reserved'] as number,
        state: accepted['state'] as string,
      });
    } catch (error) {
      const code = (error as { code?: string }).code;
      if (code === 'INSUFFICIENT_CREDITS') {
        return replyError(reply, 402, code, 'The wallet does not have enough credits.');
      }
      if (code === 'CAP_REACHED') return replyError(reply, 503, code, 'The daily provider spending limit has been reached.');
      if (code === 'SERVICE_MAINTENANCE') {
        return replyError(reply, 503, code, 'Paid operations are paused for maintenance.');
      }
      if (code === 'SERVICE_CLOSED') {
        return replyError(reply, 503, code, 'The service is closed: new paid operations are not accepted.');
      }
      if (code === 'OPERATION_DISABLED') return replyError(reply, 503, code, 'This operation is not currently available.');
      throw error;
    }
  });

  app.get('/v1/jobs/:id', {
    preHandler: [auth, deviceLimit(options.limiter, 'jobRead')],
  }, async (request, reply) => {
    const claims = request.user as AuthClaims;
    const { id } = request.params as { id: string };
    const job = db.prepare('SELECT id, state, charged, fail_reason FROM job WHERE id = ? AND wallet_id = ?')
      .get(id, claims.wid) as { id: string; state: string; charged: number; fail_reason: string | null } | undefined;
    if (!job) return replyError(reply, 404, 'JOB_NOT_FOUND', 'Job was not found.');
    reply.header('Retry-After', '2');
    if (job.state !== 'SUCCEEDED') {
      const response: Record<string, unknown> = { state: job.state, charged: job.charged };
      if (job.fail_reason) response.error = { code: job.fail_reason, message: 'The operation did not complete.' };
      return reply.send(response);
    }
    const result = db.prepare(`SELECT r.id, r.sha256, r.bytes, r.expires_at, r.delivered_at,
        m.width, m.height, m.provenance, m.guard_json
      FROM result r JOIN result_metadata m ON m.result_id = r.id
      WHERE r.job_id = ? AND r.wallet_id = ? AND r.deleted_at IS NULL`)
      .get(id, claims.wid) as {
        id: string; sha256: string; bytes: number; expires_at: number; delivered_at: number | null;
        width: number; height: number; provenance: string; guard_json: string | null;
      } | undefined;
    if (!result || result.expires_at <= Date.now()) {
      return replyError(reply, 410, 'RESULT_EXPIRED', 'The result is no longer available.');
    }
    let resultPayload: Record<string, unknown> | undefined;
    if (!result.delivered_at) {
      const token = randomBytes(32).toString('base64url');
      const tokenHash = createHash('sha256').update(token).digest('hex');
      db.transaction(() => {
        /**
         * At most three live tokens per result. A polling client asks for a
         * status and gets a fresh token each time until the result is delivered;
         * without this bound the table grows with the poll rate instead of with
         * the number of results. Two newest survive, then the new one is added.
         */
        db.prepare(`DELETE FROM delivery_token WHERE result_id = ? AND used_at IS NULL
          AND token_hash NOT IN (SELECT token_hash FROM delivery_token
            WHERE result_id = ? AND used_at IS NULL ORDER BY expires_at DESC LIMIT 2)`)
          .run(result.id, result.id);
        db.prepare(`INSERT INTO delivery_token(token_hash, result_id, wallet_id, expires_at)
          VALUES (?, ?, ?, ?)`).run(tokenHash, result.id, claims.wid, result.expires_at);
      }).immediate();
      resultPayload = {
        resultToken: token,
        expiresAt: result.expires_at,
        width: result.width,
        height: result.height,
        bytes: result.bytes,
        sha256: result.sha256,
        provenance: result.provenance,
        ...(result.guard_json ? { guard: JSON.parse(result.guard_json) } : {}),
      };
    }
    return reply.send({ state: job.state, ...(resultPayload ? { result: resultPayload } : {}), charged: job.charged });
  });

  app.get('/v1/jobs/:id/result', {
    preHandler: [auth, deviceLimit(options.limiter, 'jobResult')],
  }, async (request, reply) => {
    const claims = request.user as AuthClaims;
    const { id } = request.params as { id: string };
    const token = request.headers['x-result-token'];
    if (typeof token !== 'string') return replyError(reply, 401, 'UNAUTHORIZED', 'A result token is required.');
    const tokenHash = createHash('sha256').update(token).digest('hex');
    const result = db.prepare(`SELECT r.path, r.bytes, r.sha256, r.expires_at, t.token_hash
      FROM result r
      JOIN delivery_token t ON t.result_id = r.id AND t.wallet_id = r.wallet_id
      JOIN job j ON j.id = r.job_id
      WHERE j.id = ? AND r.wallet_id = ? AND t.token_hash = ? AND t.used_at IS NULL
        AND t.expires_at > ? AND r.expires_at > ? AND r.deleted_at IS NULL`)
      .get(id, claims.wid, tokenHash, Date.now(), Date.now()) as {
        path: string; bytes: number; sha256: string; expires_at: number; token_hash: string;
      } | undefined;
    if (!result) return replyError(reply, 404, 'RESULT_NOT_AVAILABLE', 'The result token is invalid or expired.');
    const attemptId = randomUUID();
    db.prepare(`INSERT INTO delivery_attempt(id, result_id, wallet_id, created_at, expires_at)
      SELECT ?, id, wallet_id, ?, expires_at FROM result WHERE path = ?`)
      .run(attemptId, Date.now(), result.path);
    const bytes = await readFile(result.path);
    if (bytes.length !== result.bytes || createHash('sha256').update(bytes).digest('hex') !== result.sha256) {
      return replyError(reply, 500, 'RESULT_INTEGRITY_FAILED', 'The stored result failed integrity verification.');
    }
    reply.header('Content-Length', String(result.bytes));
    reply.header('Digest', `sha-256=${Buffer.from(result.sha256, 'hex').toString('base64')}`);
    reply.header('X-Delivery-Attempt', attemptId);
    reply.type('application/octet-stream');
    onCompletedSend(reply.raw, () => {
      try {
        ledger.deliver(claims.wid, id);
      } catch (error) {
        // Never log the error object itself: its message can carry SQL text.
        request.log.error({ jobId: id, code: error instanceof Error ? error.name : 'UNKNOWN' }, 'delivery charge failed');
      }
    });
    return reply.send(bytes);
  });

  app.post('/v1/jobs/:id/delivery-receipts', {
    preHandler: [auth, deviceLimit(options.limiter, 'deliveryReceipt')],
    schema: { body: { $ref: apiRef('DeliveryReceipt') } },
  }, async (request, reply) => {
    const claims = request.user as AuthClaims;
    const { id } = request.params as { id: string };
    const body = bodyRecord(request);
    if (!validateReceipt(body)) return replyError(reply, 400, 'DELIVERY_RECEIPT_MISMATCH', 'The receipt is invalid.');
    try {
      return reply.send(ledger.settle(claims.wid, id, body));
    } catch (error) {
      if (error instanceof Error && error.message === 'DELIVERY_RECEIPT_MISMATCH') {
        return replyError(reply, 400, error.message, 'The delivery receipt does not match the complete result.');
      }
      if (error instanceof Error && error.message === 'RESERVATION_NOT_ACTIVE') {
        return replyError(reply, 409, error.message, 'The job reservation is no longer active.');
      }
      throw error;
    }
  });

  app.post('/v1/jobs/:id/cancel', {
    preHandler: [auth, deviceLimit(options.limiter, 'jobCancel')],
  }, async (request, reply) => {
    const claims = request.user as AuthClaims;
    const { id } = request.params as { id: string };
    const state = db.prepare('SELECT state FROM job WHERE id = ? AND wallet_id = ?')
      .get(id, claims.wid) as { state: string } | undefined;
    if (!state) return replyError(reply, 404, 'JOB_NOT_FOUND', 'Job was not found.');
    const cancelled = queue?.cancelBeforeProvider(id, claims.wid);
    if (cancelled) return reply.send(cancelled);
    if (state.state === 'QUEUED') {
      const released = ledger.release(claims.wid, id)['released'] as number;
      db.prepare(`UPDATE job SET state = 'CANCELLED', fail_reason = 'CANCELLED', finished_at = ?
        WHERE id = ? AND wallet_id = ?`).run(Date.now(), id, claims.wid);
      return reply.send({ state: 'CANCELLED', released });
    }
    return reply.send({ state: state.state, released: 0 });
  });

  app.post('/v1/reports', {
    preHandler: [auth, deviceLimit(options.limiter, 'reportCreate')],
    schema: {
      body: { $ref: apiRef('ReportRequest') },
      response: { 201: { $ref: apiRef('ReportResponse') } },
    },
  }, async (request, reply) => {
    const claims = request.user as AuthClaims;
    const body = bodyRecord(request);
    const jobId = body.jobId as string;
    try {
      const report = recordOutputReport(db, claims.wid, jobId, body.category as ReportCategory, body.note as string | undefined);
      return reply.code(201).send({ id: report.id, status: 'OPEN' });
    } catch (error) {
      if (error instanceof Error && error.message === 'JOB_NOT_FOUND') {
        return replyError(reply, 404, 'JOB_NOT_FOUND', 'A completed output for this wallet was not found.');
      }
      if (error instanceof Error && error.message === 'REPORT_INVALID') {
        return replyError(reply, 400, 'REPORT_INVALID', 'The report is invalid.');
      }
      throw error;
    }
  });

  app.get('/v1/reports/:id', {
    preHandler: [auth, deviceLimit(options.limiter, 'reportRead')],
  }, async (request, reply) => {
    const claims = request.user as AuthClaims;
    const rawId = (request.params as { id: string }).id;
    const id = Number(rawId);
    if (!Number.isSafeInteger(id) || id < 1) return replyError(reply, 400, 'REPORT_NOT_FOUND', 'Report was not found.');
    const status = reportStatus(db, claims.wid, id);
    if (!status) return replyError(reply, 404, 'REPORT_NOT_FOUND', 'Report was not found.');
    return reply.send(status);
  });

  app.delete('/v1/data', {
    preHandler: [auth, deviceLimit(options.limiter, 'dataDelete')],
    schema: {
      body: { $ref: apiRef('DeleteRequest') },
      response: {
        200: { $ref: apiRef('DeleteResponse') },
        202: { $ref: apiRef('DeleteResponse') },
      },
    },
  }, async (request, reply) => {
    const claims = request.user as AuthClaims;
    const body = bodyRecord(request);
    if (body.confirm !== true || claims.iat * 1000 < Date.now() - 15 * 60 * 1000) {
      return replyError(reply, 400, 'CONFIRMATION_REQUIRED', 'Recent authentication and explicit confirmation are required.');
    }
    const token = randomBytes(32).toString('base64url');
    const tokenHash = createHash('sha256').update(token).digest('hex');
    const now = Date.now();
    const providerActivity = db.prepare(`SELECT MAX(COALESCE(finished_at, created_at)) AS last_call FROM job
      WHERE wallet_id = ? AND provider_endpoint IS NOT NULL`).get(claims.wid) as { last_call: number | null };
    const providerRetentionUntil = providerActivity.last_call === null
      ? null
      : providerActivity.last_call + 30 * 24 * 60 * 60 * 1000;
    const providerStatus = providerRetentionUntil === null ? 'NO_PROVIDER_DATA' :
      providerRetentionUntil > now ? 'RETENTION_WINDOW' : 'RETENTION_UNCONFIRMED';
    const deletionStatus = providerStatus === 'NO_PROVIDER_DATA' ? 'COMPLETE' : 'PENDING';
    const linkedDevices = db.prepare(`SELECT d.id AS device_id, d.install_hash FROM device d
      JOIN device_wallet dw ON dw.device_id = d.id WHERE dw.wallet_id = ?`)
      .all(claims.wid) as Array<{ device_id: string; install_hash: string }>;
    const paths = db.prepare(`SELECT path FROM upload WHERE wallet_id = ?
      UNION SELECT path FROM result WHERE wallet_id = ?`).all(claims.wid, claims.wid) as Array<{ path: string }>;
    for (const item of paths) await rm(item.path, { force: true });
    const surrogateId = randomUUID();
    db.transaction(() => {
      db.prepare(`INSERT INTO wallet(id, balance, refund_debt, purchase_hold, starter_granted, created_at, deleted_at)
        VALUES (?, 0, 0, 0, 0, ?, ?)`).run(surrogateId, now, now);
      db.prepare('UPDATE ledger SET wallet_id = ? WHERE wallet_id = ?').run(surrogateId, claims.wid);
      db.prepare(`UPDATE purchase SET wallet_id = ?, token_ciphertext = NULL, token_wrapped_key = NULL,
        intent_id = NULL, state = CASE WHEN state = 'PLAY_FINALIZING' THEN 'INVALID' ELSE state END
        WHERE wallet_id = ?`).run(surrogateId, claims.wid);
      db.prepare('DELETE FROM delivery_token WHERE wallet_id = ?').run(claims.wid);
      db.prepare('DELETE FROM delivery_receipt WHERE wallet_id = ?').run(claims.wid);
      db.prepare('DELETE FROM delivery_attempt WHERE wallet_id = ?').run(claims.wid);
      db.prepare('DELETE FROM result_metadata WHERE result_id IN (SELECT id FROM result WHERE wallet_id = ?)')
        .run(claims.wid);
      db.prepare('DELETE FROM result WHERE wallet_id = ?').run(claims.wid);
      db.prepare('DELETE FROM job_upload WHERE job_id IN (SELECT id FROM job WHERE wallet_id = ?)')
        .run(claims.wid);
      db.prepare('DELETE FROM upload WHERE wallet_id = ?').run(claims.wid);
      removeWalletReports(db, claims.wid);
      db.prepare('DELETE FROM job WHERE wallet_id = ?').run(claims.wid);
      db.prepare('DELETE FROM purchase_intent WHERE wallet_id = ?').run(claims.wid);
      for (const device of linkedDevices) {
        db.prepare('DELETE FROM integrity_challenge WHERE install_hash = ?').run(device.install_hash);
      }
      db.prepare('DELETE FROM starter_grant WHERE wallet_id = ?').run(claims.wid);
      db.prepare('DELETE FROM device_wallet WHERE wallet_id = ?').run(claims.wid);
      for (const device of linkedDevices) db.prepare('DELETE FROM device WHERE id = ?').run(device.device_id);
      db.prepare('DELETE FROM wallet WHERE id = ?').run(claims.wid);
      db.prepare(`INSERT INTO data_deletion_receipt
        (token_hash, status, controlled_systems_complete, completed_at, expires_at, provider_status,
         provider_retention_until)
        VALUES (?, ?, 1, ?, ?, ?, ?)`)
        .run(tokenHash, deletionStatus, now, now + 7 * 24 * 60 * 60 * 1000, providerStatus,
          providerRetentionUntil);
    }).immediate();
    return reply.code(deletionStatus === 'COMPLETE' ? 200 : 202).send({
      receiptToken: token, status: deletionStatus, providerStatus,
      ...(providerRetentionUntil ? { providerRetentionUntil } : {}),
    });
  });

  app.get('/v1/data/deletions/:receiptToken', async (request, reply) => {
    const { receiptToken } = request.params as { receiptToken: string };
    const hash = createHash('sha256').update(receiptToken).digest('hex');
    const row = db.prepare(`SELECT status, controlled_systems_complete AS controlledSystemsComplete,
        provider_status AS providerStatus, provider_retention_until AS providerRetentionUntil,
        completed_at AS completedAt
      FROM data_deletion_receipt WHERE token_hash = ? AND expires_at > ?`)
      .get(hash, Date.now()) as {
        status: string; controlledSystemsComplete: number; providerStatus: string;
        providerRetentionUntil: number | null; completedAt: number;
      } | undefined;
    if (!row) return replyError(reply, 404, 'DELETION_RECEIPT_NOT_FOUND', 'The deletion receipt is invalid or expired.');
    const providerStatus = row.providerStatus === 'UNKNOWN' ? 'RETENTION_UNCONFIRMED' : row.providerStatus;
    return reply.send({
      status: row.providerStatus === 'UNKNOWN' ? 'PENDING' : row.status,
      controlledSystemsComplete: row.controlledSystemsComplete === 1,
      providerStatus,
      ...(row.providerRetentionUntil !== null ? { providerRetentionUntil: row.providerRetentionUntil } : {}),
      ...(row.status === 'COMPLETE' ? { completedAt: row.completedAt } : {}),
    });
  });

  app.post('/v1/play/rtdn', {
    schema: { body: { $ref: apiRef('PubSubPush') } },
  }, async (request, reply) => {
    if (!purchaseService || !options.rtdnIdentityVerifier || !config.pubsubAudience ||
        !config.pubsubServiceAccountEmail) {
      return replyError(reply, 503, 'NOT_CONFIGURED', 'Pub/Sub identity verification and Play reconciliation are not configured.');
    }
    const authorized = await options.rtdnIdentityVerifier(
      request.headers.authorization, config.pubsubAudience, config.pubsubServiceAccountEmail,
    );
    if (!authorized) return replyError(reply, 401, 'UNAUTHORIZED', 'The Pub/Sub identity is invalid.');
    const body = bodyRecord(request);
    const message = body['message'] as { messageId: string; data: string };
    let notification: Record<string, unknown>;
    try {
      const parsed: unknown = JSON.parse(Buffer.from(message.data, 'base64').toString('utf8'));
      if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) throw new Error('INVALID');
      notification = parsed as Record<string, unknown>;
    } catch {
      return replyError(reply, 400, 'RTDN_INVALID', 'The Pub/Sub message payload is invalid.');
    }
    if (notification['packageName'] !== config.playPackageName) return reply.code(204).send();
    const product = notification['oneTimeProductNotification'];
    if (!product || typeof product !== 'object' || Array.isArray(product)) return reply.code(204).send();
    const oneTime = product as Record<string, unknown>;
    const type = oneTime['notificationType'] === 1 ? 'PURCHASED' :
      oneTime['notificationType'] === 2 ? 'CANCELED' : null;
    if (!type || typeof oneTime['purchaseToken'] !== 'string' || typeof oneTime['sku'] !== 'string') {
      return reply.code(204).send();
    }
    try {
      await purchaseService.processRtdn({
        messageId: message.messageId,
        purchaseToken: oneTime['purchaseToken'],
        productId: oneTime['sku'],
        type,
        eventTime: Number(notification['eventTimeMillis']),
      });
      return reply.code(204).send();
    } catch (error) {
      if (error instanceof Error && error.message.startsWith('PURCHASE_')) {
        return replyError(reply, 400, 'RTDN_INVALID', 'The purchase notification could not be verified.');
      }
      if (error instanceof Error && error.message === 'RTDN_INVALID') {
        return replyError(reply, 400, 'RTDN_INVALID', 'The purchase notification is invalid.');
      }
      request.log.error({ requestId: request.id, code: 'RTDN_PROCESSING_FAILED' }, 'RTDN processing failed');
      return replyError(reply, 503, 'RTDN_UNAVAILABLE', 'Purchase notification processing is temporarily unavailable.');
    }
  });
}
