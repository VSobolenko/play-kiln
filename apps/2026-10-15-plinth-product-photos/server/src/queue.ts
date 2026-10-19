import { Ajv } from 'ajv';
import { createHash, randomInt, randomUUID } from 'node:crypto';
import { existsSync } from 'node:fs';
import { mkdir, rm, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import type Database from 'better-sqlite3';
import apiSchema from '../schema/api.schema.json' with { type: 'json' };
import type { CreditLedger } from './ledger.js';
import { ProviderConcurrency } from './provider-concurrency.js';
import { normaliseResult, ResultUnusableError } from './results.js';
import { sweepExpiredResources } from './retention.js';
import { MODEL_REGISTRY, type OperationId, type ProviderAdapter } from './registry.js';

const validateJobWork = new Ajv().compile(apiSchema.definitions.JobWork);
const opaqueResultLifetimeMs = 24 * 60 * 60 * 1000;

type WorkRecord = Record<string, unknown>;
type QueueEntry = { work: WorkRecord; cancelled: boolean };
type QueueOptions = {
  dailySpendCapMicroUsd?: number;
  killSwitchFile?: string;
  serviceClosedFile?: string;
  providerConcurrency?: ProviderConcurrency;
};

function workRecord(input: unknown): WorkRecord {
  if (!validateJobWork(input)) throw new Error('INVALID_JOB_PAYLOAD');
  return input as WorkRecord;
}

function stringField(record: WorkRecord, key: string): string {
  return record[key] as string;
}

function numberField(record: WorkRecord, key: string): number {
  return record[key] as number;
}

function stringArrayField(record: WorkRecord, key: string): string[] {
  return record[key] as string[];
}

function objectField(record: WorkRecord, key: string): Record<string, unknown> {
  return record[key] as Record<string, unknown>;
}

function providerErrorCode(error: unknown): string {
  if (error instanceof Error &&
      ['OPERATION_DISABLED', 'GUARD_REJECTED', 'MODERATION_BLOCKED', 'CAP_REACHED',
        'FIDELITY_GUARD_FAILED'].includes(error.message)) {
    return error.message;
  }
  if (error instanceof ResultUnusableError) return error.code;
  return 'PROVIDER_FAILED';
}

function newSeed(previous: unknown): number {
  const seed = randomInt(0, 2 ** 31);
  return seed === previous ? (seed + 1) % (2 ** 31) : seed;
}

export class JobQueue {
  private readonly pending: QueueEntry[] = [];
  private active = 0;
  private readonly idleWaiters: Array<() => void> = [];
  private readonly providerConcurrency: ProviderConcurrency;

  constructor(
    private readonly db: Database.Database,
    private readonly ledger: CreditLedger,
    private readonly provider: ProviderAdapter,
    private readonly concurrency: number,
    private readonly dataDir = 'data',
    private readonly options: QueueOptions = {},
  ) {
    if (!Number.isSafeInteger(concurrency) || concurrency < 1) throw new Error('INVALID_JOB_CONCURRENCY');
    this.providerConcurrency = options.providerConcurrency ?? new ProviderConcurrency(concurrency);
    this.providerConcurrency.onAvailable(() => this.pump());
  }

  async enqueue(input: unknown): Promise<Record<string, unknown>> {
    const work = workRecord(input);
    const jobId = stringField(work, 'jobId');
    const walletId = stringField(work, 'walletId');
    const clientRequestId = stringField(work, 'clientRequestId');
    const op = stringField(work, 'op') as OperationId;
    const credits = numberField(work, 'credits');
    const uploadIds = stringArrayField(work, 'uploadIds');
    /**
     * A replay of a request that already became a job returns that job, even
     * while the service is paused or closed: the caller must be able to learn
     * what happened to credits it already spent. Only new work is refused.
     */
    const existing = this.db.prepare(
      'SELECT id, reserved, state FROM job WHERE wallet_id = ? AND client_request_id = ?',
    ).get(walletId, clientRequestId) as { id: string; reserved: number; state: string } | undefined;
    if (existing) return { jobId: existing.id, reserved: existing.reserved, state: existing.state };
    if (this.options.serviceClosedFile && existsSync(this.options.serviceClosedFile)) {
      throw Object.assign(new Error('SERVICE_CLOSED'), { code: 'SERVICE_CLOSED' });
    }
    if (this.options.killSwitchFile && existsSync(this.options.killSwitchFile)) {
      throw Object.assign(new Error('SERVICE_MAINTENANCE'), { code: 'SERVICE_MAINTENANCE' });
    }
    const model = MODEL_REGISTRY[op];
    if (!model.enabled || model.costMicroUsd === null) {
      throw Object.assign(new Error('OPERATION_DISABLED'), { code: 'OPERATION_DISABLED' });
    }

    const now = Date.now();
    this.db.transaction(() => {
      this.db.prepare(`INSERT INTO job
        (id, wallet_id, op, state, reserved, provider_endpoint, cost_micro_usd, client_request_id, created_at)
        VALUES (?, ?, ?, 'QUEUED', 0, ?, ?, ?, ?)`)
        .run(jobId, walletId, op, model.endpointId, model.costMicroUsd, clientRequestId, now);
      for (const uploadId of uploadIds) {
        this.db.prepare('INSERT INTO job_upload(job_id, upload_id) VALUES (?, ?)').run(jobId, uploadId);
      }
    }).immediate();

    const reservation = this.ledger.reserve(walletId, jobId, credits);
    if (!reservation['accepted']) {
      this.db.prepare(`UPDATE job SET state = 'FAILED', fail_reason = 'INSUFFICIENT_CREDITS', finished_at = ?
        WHERE id = ?`).run(Date.now(), jobId);
      throw Object.assign(new Error('INSUFFICIENT_CREDITS'), { code: 'INSUFFICIENT_CREDITS' });
    }

    try {
      this.reserveDailyBudget(model.costMicroUsd);
    } catch (error) {
      this.ledger.release(walletId, jobId, providerErrorCode(error));
      throw error;
    }

    const entry: QueueEntry = { work, cancelled: false };
    this.pending.push(entry);
    this.pump();
    return { jobId, reserved: credits, state: 'QUEUED' };
  }

  cancelBeforeProvider(jobId: string, walletId: string): { state: string; released: number } | null {
    const index = this.pending.findIndex((entry) =>
      stringField(entry.work, 'jobId') === jobId && stringField(entry.work, 'walletId') === walletId,
    );
    if (index < 0) return null;
    const [entry] = this.pending.splice(index, 1);
    if (!entry) return null;
    entry.cancelled = true;
    const released = this.ledger.release(walletId, jobId)['released'] as number;
    this.db.prepare(`UPDATE job SET state = 'CANCELLED', fail_reason = 'CANCELLED', finished_at = ?
      WHERE id = ? AND wallet_id = ?`).run(Date.now(), jobId, walletId);
    this.notifyIdle();
    return { state: 'CANCELLED', released };
  }

  async waitForIdle(): Promise<void> {
    if (this.active === 0 && this.pending.length === 0) return;
    await new Promise<void>((resolve) => this.idleWaiters.push(resolve));
  }

  /**
   * Startup recovery, called before the process accepts work.
   *
   * Nothing can be RUNNING at this point: the only caller is `createApp`, before
   * `listen`, and a reservation is held in the ledger for a job no live process
   * is executing. The old query kept RUNNING jobs younger than three minutes
   * untouched, which leaks their reservation forever — a process that dies and
   * restarts within three minutes is exactly the restart this must repair.
   */
  async recoverInterruptedJobs(now = Date.now()): Promise<void> {
    const interrupted = this.db.prepare(`SELECT id, wallet_id, state FROM job
      WHERE state = 'QUEUED' OR state = 'RUNNING'`)
      .all() as Array<{ id: string; wallet_id: string; state: string }>;
    for (const job of interrupted) {
      this.ledger.release(job.wallet_id, job.id);
      this.db.prepare(`UPDATE job SET state = 'FAILED', fail_reason = 'PROVIDER_FAILED', finished_at = ?
        WHERE id = ?`).run(now, job.id);
    }
  }

  async sweepExpired(now = Date.now()): Promise<void> {
    await sweepExpiredResources(this.db, this.ledger, now);
  }

  private pump(): void {
    while (this.active < this.concurrency && this.pending.length > 0) {
      const entry = this.pending.shift();
      if (!entry || entry.cancelled) continue;
      if (!this.providerConcurrency.tryAcquire()) {
        this.pending.unshift(entry);
        break;
      }
      this.active += 1;
      void this.run(entry).finally(() => {
        this.providerConcurrency.release();
        this.active -= 1;
        this.pump();
        this.notifyIdle();
      });
    }
  }

  private async run(entry: QueueEntry): Promise<void> {
    const work = entry.work;
    const jobId = stringField(work, 'jobId');
    const walletId = stringField(work, 'walletId');
    const op = stringField(work, 'op') as OperationId;
    try {
      this.db.prepare(`UPDATE job SET state = 'RUNNING' WHERE id = ? AND wallet_id = ? AND state = 'QUEUED'`)
        .run(jobId, walletId);
      const image = Buffer.from(stringField(work, 'imageBase64'), 'base64');
      const mask = typeof work['maskBase64'] === 'string'
        ? Buffer.from(work['maskBase64'], 'base64')
        : undefined;
      const params = { ...objectField(work, 'params') };
      if (!Object.hasOwn(params, 'seed')) params['seed'] = newSeed(undefined);
      let output = await this.provider.run(op, { image, ...(mask ? { mask } : {}) }, params);
      if (output.guard?.passed === false) {
        const retryCost = MODEL_REGISTRY[op].costMicroUsd;
        if (retryCost === null) throw new Error('OPERATION_DISABLED');
        this.reserveDailyBudget(retryCost);
        const retryParams = { ...params, seed: newSeed(params['seed']) };
        output = await this.provider.run(op, { image, ...(mask ? { mask } : {}) }, retryParams);
        if (output.guard?.passed === false) throw new Error('GUARD_REJECTED');
      }
      const providerHash = createHash('sha256').update(output.bytes).digest('hex');
      if (providerHash !== output.sha256) throw new Error('PROVIDER_RESULT_HASH_MISMATCH');
      const normalised = await normaliseResult(output.bytes);
      if ((op === 'O-09' || op === 'O-12') &&
          (normalised.width !== output.width || normalised.height !== output.height)) {
        throw new Error('FIDELITY_GUARD_FAILED');
      }
      const actualHash = createHash('sha256').update(normalised.bytes).digest('hex');

      const resultsDir = join(this.dataDir, 'results');
      await mkdir(resultsDir, { recursive: true });
      const resultId = randomUUID();
      const path = join(resultsDir, resultId);
      await writeFile(path, normalised.bytes, { flag: 'wx' });
      try {
        const now = Date.now();
        this.db.transaction(() => {
          this.db.prepare(`INSERT INTO result
            (id, job_id, wallet_id, path, sha256, bytes, expires_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)`)
            .run(resultId, jobId, walletId, path, actualHash, normalised.bytes.length,
              now + opaqueResultLifetimeMs);
          this.db.prepare(`INSERT INTO result_metadata
            (result_id, width, height, format, provenance, guard_json)
            VALUES (?, ?, ?, ?, ?, ?)`)
            .run(resultId, normalised.width, normalised.height, `image/${normalised.format}`, output.provenance,
              output.guard ? JSON.stringify(output.guard) : null);
          this.db.prepare(`UPDATE job SET state = 'SUCCEEDED', finished_at = ?
            WHERE id = ? AND wallet_id = ?`).run(now, jobId, walletId);
        }).immediate();
      } catch (error) {
        await rm(path, { force: true });
        throw error;
      }
    } catch (error) {
      const code = providerErrorCode(error);
      this.ledger.release(walletId, jobId, code);
    }
  }

  private notifyIdle(): void {
    if (this.active !== 0 || this.pending.length !== 0) return;
    for (const resolve of this.idleWaiters.splice(0)) resolve();
  }

  private reserveDailyBudget(costMicroUsd: number): void {
    const cap = this.options.dailySpendCapMicroUsd ?? 25_000_000;
    this.db.transaction(() => {
      const day = new Date().toISOString().slice(0, 10);
      const current = this.db.prepare('SELECT cost_micro_usd FROM spend_day WHERE day = ?')
        .get(day) as { cost_micro_usd: number } | undefined;
      if ((current?.cost_micro_usd ?? 0) + costMicroUsd > cap) {
        throw Object.assign(new Error('CAP_REACHED'), { code: 'CAP_REACHED' });
      }
      this.db.prepare(`INSERT INTO spend_day(day, cost_micro_usd, starter_cost_micro_usd)
        VALUES (?, ?, 0)
        ON CONFLICT(day) DO UPDATE SET cost_micro_usd = cost_micro_usd + excluded.cost_micro_usd`)
        .run(day, costMicroUsd);
    }).immediate();
  }
}
