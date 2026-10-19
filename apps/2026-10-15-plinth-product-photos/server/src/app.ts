import fastify, { type FastifyError, type FastifyInstance } from 'fastify';
import fastifyJwt from '@fastify/jwt';
import type Database from 'better-sqlite3';
import apiSchema from '../schema/api.schema.json' with { type: 'json' };
import type { ServerConfig } from './config.js';
import { CreditLedger } from './ledger.js';
import { installNetworkLimiter, TokenBucketLimiter } from './rate-limit.js';
import { JobQueue } from './queue.js';
import { ProviderConcurrency } from './provider-concurrency.js';
import { registerRoutes } from './routes.js';
import { createFalProvider, type ProviderAdapter } from './registry.js';
import { sweepExpiredResources } from './retention.js';
import { GooglePlayClient } from './google-play.js';
import { PurchaseService } from './purchases.js';
import { createSessionIntegrityVerifier } from './device-recall.js';

type AppOptions = {
  db: Database.Database;
  config: ServerConfig;
  provider?: ProviderAdapter | null;
  closeDatabaseOnExit?: boolean;
};

export async function createApp(options: AppOptions): Promise<FastifyInstance> {
  const app = fastify({
    /**
     * The JSON limit is the API's payload limit: the largest legitimate body is
     * a recognition/edit request naming an uploaded file, measured in a few
     * kilobytes. 5 MiB is chosen only so the separate image parser below shares
     * one ceiling; `schema/api.schema.json` caps the nested fields.
     */
    bodyLimit: 5 * 1024 * 1024,
    /**
     * Caddy on the same host is the only trusted hop. Without this every caller
     * appears as 127.0.0.1 and the network limiter becomes one shared bucket;
     * with a wider setting a caller could forge `X-Forwarded-For` and escape it.
     */
    trustProxy: options.config.trustProxy,
    logger: {
      level: 'info',
      redact: ['req.url', 'req.headers.authorization', 'req.headers.x-result-token', 'req.body.integrityToken',
        'req.body.purchaseToken', 'req.body.note'],
    },
  });
  app.addSchema(apiSchema);
  /**
   * Responses are JSON for an Android client; no browser origin is ever a
   * legitimate caller, so no CORS plugin is registered and no
   * `Access-Control-Allow-Origin` is emitted. The headers below harden the
     * responses that Caddy does not add itself (HSTS is also set at the edge).
   */
  app.addHook('onSend', async (_request, reply, payload) => {
    reply.header('X-Content-Type-Options', 'nosniff');
    reply.header('X-Frame-Options', 'DENY');
    reply.header('Referrer-Policy', 'no-referrer');
    reply.header('Content-Security-Policy', "default-src 'none'; frame-ancestors 'none'");
    reply.header('Cross-Origin-Resource-Policy', 'same-origin');
    reply.header('Permissions-Policy', 'geolocation=(), microphone=(), camera=()');
    reply.header('Strict-Transport-Security', 'max-age=31536000; includeSubDomains');
    return payload;
  });
  const limiter = new TokenBucketLimiter();
  installNetworkLimiter(app, limiter);
  await app.register(fastifyJwt, { secret: options.config.jwtSecret });
  app.addContentTypeParser(['image/jpeg', 'image/png', 'image/webp'], { parseAs: 'buffer' },
    (_request, payload, done) => done(null, payload));
  app.setErrorHandler((error: FastifyError, request, reply) => {
    if (error.validation) {
      return reply.code(400).send({
        error: { code: 'INVALID_REQUEST', message: 'The request does not match the API schema.',
          retryAfterSec: null, details: {} },
      });
    }
    if (error.statusCode === 413) {
      return reply.code(413).send({
        error: { code: 'TOO_LARGE', message: 'The request body exceeds the 5 MiB server limit.',
          retryAfterSec: null, details: {} },
      });
    }
    if (error.statusCode !== undefined && error.statusCode >= 400 && error.statusCode < 500) {
      return reply.code(error.statusCode).send({
        error: { code: 'INVALID_REQUEST', message: 'The request could not be read.',
          retryAfterSec: null, details: {} },
      });
    }
    request.log.error({ requestId: request.id, code: error.code ?? 'INTERNAL' }, 'request failed');
    return reply.code(500).send({
      error: { code: 'INTERNAL', message: 'The request could not be completed.', retryAfterSec: null, details: {} },
    });
  });

  const provider = options.provider === undefined
    ? (options.config.falKey ? createFalProvider(options.config) : null)
    : options.provider;
  const ledger = new CreditLedger(options.db);
  const providerConcurrency = new ProviderConcurrency(options.config.jobConcurrency);
  const googlePlay = options.config.playServiceAccountJson
    ? new GooglePlayClient(options.config.playServiceAccountJson, options.config.playPackageName)
    : null;
  const integrityVerifier = createSessionIntegrityVerifier(options.config, (message) => app.log.warn(message));
  const purchaseService = googlePlay && options.config.billingBindingSecret && options.config.purchaseTokenKek
    ? new PurchaseService(options.db, googlePlay, options.config.billingBindingSecret,
      options.config.playPackageName, options.config.playCreditProducts, options.config.purchaseTokenKek,
    options.config.playEntitlementProducts)
    : null;
  const queue = provider
    ? new JobQueue(options.db, ledger, provider, options.config.jobConcurrency, options.config.dataDir, {
      dailySpendCapMicroUsd: options.config.dailySpendCapMicroUsd,
      killSwitchFile: options.config.killSwitchFile,
      serviceClosedFile: options.config.serviceClosedFile,
      providerConcurrency,
    })
    : null;
  await app.register(registerRoutes, {
    db: options.db, config: options.config, ledger, queue, providerConcurrency, provider, integrityVerifier,
    purchaseService, limiter,
    rtdnIdentityVerifier: googlePlay
      ? (authorization, audience, email) => googlePlay.verifyRtdnIdentity(authorization, audience, email)
      : null,
  });
  if (queue) {
    await queue.recoverInterruptedJobs();
  }
  await sweepExpiredResources(options.db, ledger);
  const maintenanceError = (error: unknown, label: string) => {
    app.log.error({ requestId: 'maintenance', code: error instanceof Error ? error.name : 'UNKNOWN' }, label);
  };
  if (purchaseService) {
    try {
      await purchaseService.retryFinalizations();
    } catch (error) {
      maintenanceError(error, 'purchase finalization recovery failed');
    }
    try {
      await purchaseService.reconcileVoided();
    } catch (error) {
      maintenanceError(error, 'voided purchase reconciliation failed');
    }
  }
  let maintenanceRunning = false;
  const maintenance = setInterval(() => {
    if (maintenanceRunning) return;
    maintenanceRunning = true;
    void (async () => {
      try {
        await sweepExpiredResources(options.db, ledger);
      } catch (error) {
        maintenanceError(error, 'retention maintenance failed');
      }
      if (purchaseService) {
        try {
          await purchaseService.retryFinalizations();
        } catch (error) {
          maintenanceError(error, 'purchase finalization recovery failed');
        }
        const reconciliation = options.db.prepare('SELECT last_success_at FROM play_reconcile WHERE id = 1')
          .get() as { last_success_at: number | null } | undefined;
        if (!reconciliation?.last_success_at || Date.now() - reconciliation.last_success_at >= 24 * 60 * 60 * 1000) {
          try {
            await purchaseService.reconcileVoided();
          } catch (error) {
            maintenanceError(error, 'voided purchase reconciliation failed');
          }
        }
      }
    })().finally(() => { maintenanceRunning = false; });
  }, 60_000);
  maintenance.unref();
  app.addHook('onClose', async () => clearInterval(maintenance));
  if (options.closeDatabaseOnExit !== false) {
    app.addHook('onClose', async () => options.db.close());
  }
  return app;
}
