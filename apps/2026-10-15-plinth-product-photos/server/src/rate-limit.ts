/**
 * In-process request limiting.
 *
 * One Fastify process per host means an in-memory limiter is the whole
 * deployment surface: there is no second replica for a counter to disagree
 * with, and no Redis to keep alive. If a second replica is ever added, this
 * module must move to a shared store first; the honest limit is stated in the
 * comment rather than discovered by an attacker who opens two connections.
 *
 * Two keys are used, for two different attacks:
 *
 *  - the network key (`ip:<address>:<route>`) bounds an unauthenticated flood.
 *    Behind Caddy front it is the real client address, which needs
 *    `TRUST_PROXY=loopback` (the default) and nothing wider. IPv6 addresses are
 *    collapsed to their /64, because one delegated prefix otherwise buys 2^64
 *    fresh buckets.
 *  - the device key (`dev:<route>:<deviceId>`) bounds an authenticated caller.
 *    It is the JWT `sub`, so a caller cannot choose it, and it survives
 *    source-address rotation that the network key cannot see.
 *
 * Both keys name the route. Without it a read endpoint's polling spends the
 * allowance a purchase confirmation or a recovery code needs, and two unrelated
 * budgets collide even though neither caller misbehaved.
 *
 * The bucket map is capped (`MAX_BUCKETS`) and evicts oldest-first, so a flood
 * from many addresses costs bounded memory rather than growing until the
 * process is killed. Eviction is not a perfect defence — an attacker can churn
 * the map and evict an honest bucket — and that limit is documented rather than
 * described as protection it does not provide.
 *
 * A token bucket is used rather than a fixed window because a fixed window lets
 * a caller spend its whole allowance twice in two adjacent windows; the bucket
 * smooths that without a schedule.
 */

import type { FastifyInstance, FastifyReply, FastifyRequest } from 'fastify';

export interface LimitRule {
  /** Burst size, in requests. */
  readonly capacity: number;
  /** Time to refill an empty bucket. */
  readonly windowMs: number;
}

export interface LimitResult {
  readonly allowed: boolean;
  /** Whole requests left after this one (0 when refused). */
  readonly remaining: number;
  /** Seconds until one request is available again; 0 when allowed. */
  readonly retryAfterSec: number;
}

interface Bucket {
  tokens: number;
  updatedAt: number;
  readonly windowMs: number;
}

/**
 * Upper bound on live buckets. One address that probes every route can create
 * (routes × 1) entries, so the cap is generous for real traffic and finite
 * under a flood from many addresses. When it is reached the oldest entry is
 * evicted: an honest caller loses at most one refill, while an attacker cannot
 * grow the map without bound. Stated as a limitation rather than a perfect
 * defence in `docs/security-model.md`.
 */
export const MAX_BUCKETS = 20_000;

export class TokenBucketLimiter {
  private readonly buckets = new Map<string, Bucket>();
  private lastSweep = 0;

  constructor(
    private readonly sweepIntervalMs = 60_000,
    private readonly maxBuckets = MAX_BUCKETS,
  ) {
    if (!Number.isSafeInteger(maxBuckets) || maxBuckets < 1) {
      throw new Error('maxBuckets must be a positive integer');
    }
  }

  take(key: string, rule: LimitRule, now = Date.now()): LimitResult {
    this.sweep(now);
    const refillPerMs = rule.capacity / rule.windowMs;
    const bucket = this.buckets.get(key);
    let tokens: number;
    if (bucket === undefined) {
      this.makeRoom();
      tokens = rule.capacity;
      this.buckets.set(key, { tokens, updatedAt: now, windowMs: rule.windowMs });
    } else {
      const elapsed = Math.max(0, now - bucket.updatedAt);
      tokens = Math.min(rule.capacity, bucket.tokens + elapsed * refillPerMs);
      bucket.tokens = tokens;
      bucket.updatedAt = now;
    }
    if (tokens < 1) {
      const missing = 1 - tokens;
      return {
        allowed: false,
        remaining: 0,
        retryAfterSec: Math.max(1, Math.ceil(missing / refillPerMs / 1000)),
      };
    }
    const stored = this.buckets.get(key)!;
    stored.tokens = tokens - 1;
    return {
      allowed: true,
      remaining: Math.floor(stored.tokens),
      retryAfterSec: 0,
    };
  }

  /** Exposed for tests and for an operator that wants to clear a false positive. */
  reset(): void {
    this.buckets.clear();
    this.lastSweep = 0;
  }

  get size(): number {
    return this.buckets.size;
  }

  /** Evicts oldest-first until a new key may be inserted without exceeding the cap. */
  private makeRoom(): void {
    while (this.buckets.size >= this.maxBuckets) {
      const oldest = this.buckets.keys().next();
      if (oldest.done === true) return;
      this.buckets.delete(oldest.value);
    }
  }

  /** Drops buckets that have refilled to full and can no longer be distinguished from absent. */
  private sweep(now: number): void {
    if (now - this.lastSweep < this.sweepIntervalMs) return;
    this.lastSweep = now;
    for (const [key, bucket] of this.buckets) {
      if (now - bucket.updatedAt >= bucket.windowMs) this.buckets.delete(key);
    }
  }
}

/**
 * Device-keyed allowances. Values are deliberately far below what a human can
 * generate and far above what the worst legitimate burst needs (a batch retry
 * after a network drop), so a real user never sees a 429 and a loop does.
 */
export const DEVICE_LIMITS = {
  walletRead: { capacity: 60, windowMs: 60_000 },
  recoveryCode: { capacity: 3, windowMs: 3_600_000 },
  upload: { capacity: 30, windowMs: 60_000 },
  recognize: { capacity: 30, windowMs: 60_000 },
  jobCreate: { capacity: 20, windowMs: 60_000 },
  jobRead: { capacity: 120, windowMs: 60_000 },
  jobResult: { capacity: 30, windowMs: 60_000 },
  deliveryReceipt: { capacity: 60, windowMs: 60_000 },
  jobCancel: { capacity: 30, windowMs: 60_000 },
  purchaseIntent: { capacity: 10, windowMs: 600_000 },
  purchaseConfirm: { capacity: 10, windowMs: 600_000 },
  reportCreate: { capacity: 20, windowMs: 3_600_000 },
  reportRead: { capacity: 60, windowMs: 60_000 },
  dataDelete: { capacity: 3, windowMs: 3_600_000 },
} as const satisfies Record<string, LimitRule>;

export type DeviceLimitName = keyof typeof DEVICE_LIMITS;

export const PUBLIC_LIMITS = {
  config: { capacity: 120, windowMs: 60_000 },
  challenge: { capacity: 12, windowMs: 300_000 },
  session: { capacity: 12, windowMs: 300_000 },
  deletionStatus: { capacity: 30, windowMs: 60_000 },
  providerNotification: { capacity: 600, windowMs: 60_000 },
} as const satisfies Record<string, LimitRule>;

/**
 * The network ceiling on an authenticated route. It is a flood guard, not the
 * caller's allowance: a household behind one NAT address has to fit several
 * apps into it, while a single device's own bucket (above) is what shapes
 * legitimate use. 600/min is 10 req/s sustained, far above any screen's poll.
 */
const AUTHENTICATED_IP_FLOOD: LimitRule = { capacity: 600, windowMs: 60_000 };

interface RouteLimit {
  /** Stable bucket name, so one route's spending cannot exhaust another's. */
  readonly name: string;
  readonly method: 'GET' | 'POST' | 'PUT' | 'DELETE';
  readonly pattern: RegExp;
  readonly ip: LimitRule;
  readonly device?: DeviceLimitName;
}

const ROUTE_LIMITS: readonly RouteLimit[] = [
  { name: 'config', method: 'GET', pattern: /^\/(?:v1\/)?config$/, ip: PUBLIC_LIMITS.config },
  {
    name: 'challenge',
    method: 'POST',
    pattern: /^\/v1\/integrity\/challenge$/,
    ip: PUBLIC_LIMITS.challenge,
  },
  { name: 'session', method: 'POST', pattern: /^\/v1\/session$/, ip: PUBLIC_LIMITS.session },
  {
    name: 'deletionStatus',
    method: 'GET',
    pattern: /^\/v1\/data\/deletions\/[^/]+$/,
    ip: PUBLIC_LIMITS.deletionStatus,
  },
  {
    name: 'providerNotification',
    method: 'POST',
    pattern: /^\/v1\/play\/rtdn$/,
    ip: PUBLIC_LIMITS.providerNotification,
  },

  {
    name: 'walletRead',
    method: 'GET',
    pattern: /^\/v1\/wallet$/,
    ip: AUTHENTICATED_IP_FLOOD,
    device: 'walletRead',
  },
  {
    name: 'recoveryCode',
    method: 'POST',
    pattern: /^\/v1\/wallet\/recovery-code$/,
    ip: AUTHENTICATED_IP_FLOOD,
    device: 'recoveryCode',
  },
  {
    name: 'upload',
    method: 'PUT',
    pattern: /^\/v1\/uploads$/,
    ip: AUTHENTICATED_IP_FLOOD,
    device: 'upload',
  },
  {
    name: 'recognize',
    method: 'POST',
    pattern: /^\/v1\/recognize$/,
    ip: AUTHENTICATED_IP_FLOOD,
    device: 'recognize',
  },
  {
    name: 'jobCreate',
    method: 'POST',
    pattern: /^\/v1\/jobs$/,
    ip: AUTHENTICATED_IP_FLOOD,
    device: 'jobCreate',
  },
  {
    name: 'jobRead',
    method: 'GET',
    pattern: /^\/v1\/jobs\/[^/]+$/,
    ip: AUTHENTICATED_IP_FLOOD,
    device: 'jobRead',
  },
  {
    name: 'jobResult',
    method: 'GET',
    pattern: /^\/v1\/jobs\/[^/]+\/result$/,
    ip: AUTHENTICATED_IP_FLOOD,
    device: 'jobResult',
  },
  {
    name: 'deliveryReceipt',
    method: 'POST',
    pattern: /^\/v1\/jobs\/[^/]+\/delivery-receipts$/,
    ip: AUTHENTICATED_IP_FLOOD,
    device: 'deliveryReceipt',
  },
  {
    name: 'jobCancel',
    method: 'POST',
    pattern: /^\/v1\/jobs\/[^/]+\/cancel$/,
    ip: AUTHENTICATED_IP_FLOOD,
    device: 'jobCancel',
  },
  {
    name: 'purchaseIntent',
    method: 'POST',
    pattern: /^\/v1\/purchases\/intents$/,
    ip: AUTHENTICATED_IP_FLOOD,
    device: 'purchaseIntent',
  },
  {
    name: 'purchaseConfirm',
    method: 'POST',
    pattern: /^\/v1\/purchases\/confirm$/,
    ip: AUTHENTICATED_IP_FLOOD,
    device: 'purchaseConfirm',
  },
  {
    name: 'reportCreate',
    method: 'POST',
    pattern: /^\/v1\/reports$/,
    ip: AUTHENTICATED_IP_FLOOD,
    device: 'reportCreate',
  },
  {
    name: 'reportRead',
    method: 'GET',
    pattern: /^\/v1\/reports\/[^/]+$/,
    ip: AUTHENTICATED_IP_FLOOD,
    device: 'reportRead',
  },
  {
    name: 'dataDelete',
    method: 'DELETE',
    pattern: /^\/v1\/data$/,
    ip: AUTHENTICATED_IP_FLOOD,
    device: 'dataDelete',
  },
];

/** `/health` and `/healthz` are probe endpoints and are never limited. */
function pathOf(url: string): string {
  const query = url.indexOf('?');
  return query === -1 ? url : url.slice(0, query);
}

export function matchRouteLimit(method: string, url: string): RouteLimit | null {
  const path = pathOf(url);
  for (const rule of ROUTE_LIMITS) {
    if (rule.method === method && rule.pattern.test(path)) return rule;
  }
  return null;
}

export function rateLimitedBody(result: LimitResult): Record<string, unknown> {
  return {
    error: {
      code: 'RATE_LIMITED',
      message: 'Too many requests. Retry after the number of seconds in retryAfterSec.',
      retryAfterSec: result.retryAfterSec,
      details: {},
    },
  };
}

export function sendRateLimited(reply: FastifyReply, result: LimitResult): FastifyReply {
  reply.header('Retry-After', String(result.retryAfterSec));
  return reply.code(429).send(rateLimitedBody(result));
}

/**
 * Collapses a source address into the key the network limiter counts against.
 *
 * An IPv6 client normally controls a whole /64, so keying by the full address
 * hands an attacker 2^64 fresh buckets for the price of one prefix. The first
 * four hextets are the /64 an ISP delegates; that is the smallest unit worth
 * counting. IPv4-mapped addresses (`::ffff:a.b.c.d`, what Node reports on a
 * dual-stack listener) collapse to the IPv4 form so a client cannot get a
 * second allowance by switching families.
 */
export function networkKey(address: string): string {
  const mapped = /^::ffff:(\d{1,3}(?:\.\d{1,3}){3})$/i.exec(address);
  if (mapped?.[1] !== undefined) return `ip:${mapped[1]}`;
  if (!address.includes(':')) return `ip:${address}`;
  return `ip:[${ipv6Prefix64(address)}/64]`;
}

function ipv6Prefix64(address: string): string {
  const bare = address.split('%')[0] ?? address;
  const [leftRaw = '', rightRaw = ''] = bare.split('::');
  const left = leftRaw === '' ? [] : leftRaw.split(':');
  const right = rightRaw === '' ? [] : rightRaw.split(':');
  const groups = bare.includes('::')
    ? [...left, ...Array<string>(Math.max(0, 8 - left.length - right.length)).fill('0'), ...right]
    : left;
  return groups
    .slice(0, 4)
    .map((group) => {
      const hex = group.toLowerCase();
      return hex.replace(/^0+(?=.)/, '');
    })
    .join(':');
}

/** Network-keyed hook: install once in `createApp`. */
export function installNetworkLimiter(app: FastifyInstance, limiter: TokenBucketLimiter): void {
  app.addHook('onRequest', async (request, reply) => {
    const route = matchRouteLimit(request.method, request.url);
    if (route === null) return;
    // The route name is part of the key: sharing one bucket across unrelated
    // routes would let a read endpoint spend the allowance a purchase needs.
    const result = limiter.take(`${networkKey(request.ip)}:${route.name}`, route.ip);
    if (!result.allowed) {
      request.log.warn(
        { code: 'RATE_LIMITED', key: 'ip', limit: `${route.ip.capacity}/${route.ip.windowMs}ms` },
        'network limit reached',
      );
      return sendRateLimited(reply, result);
    }
  });
}

/**
 * Device-keyed pre-handler. Placed after authentication so the key is a verified
 * claim; a caller that has not authenticated never reaches it.
 */
export function deviceLimit(limiter: TokenBucketLimiter, name: DeviceLimitName) {
  const rule = DEVICE_LIMITS[name];
  return async (request: FastifyRequest, reply: FastifyReply): Promise<void> => {
    const user = request.user as { sub?: unknown } | undefined;
    const subject = typeof user?.sub === 'string' ? user.sub : undefined;
    if (subject === undefined) return;
    // Per-route device keys, for the same reason as the network key: a polling
    // read must not consume the allowance a purchase confirmation needs.
    const result = limiter.take(`dev:${name}:${subject}`, rule);
    if (!result.allowed) {
      request.log.warn(
        { code: 'RATE_LIMITED', key: 'device', limit: `${rule.capacity}/${rule.windowMs}ms` },
        'device limit reached',
      );
      sendRateLimited(reply, result);
      return;
    }
  };
}
