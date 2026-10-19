import type Database from 'better-sqlite3';
import type { FastifyReply, FastifyRequest } from 'fastify';

declare module '@fastify/jwt' {
  interface FastifyJWT {
    /**
     * `ver` is the app version the client sent at session creation. It is inside
     * the signature, so the minimum-version gate cannot be escaped by editing a
     * request header on a later call.
     */
    payload: { sub: string; wid: string; gen: number; ver?: string };
    user: { sub: string; wid: string; gen: number; ver?: string; iat: number; exp: number };
  }
}

export function authenticate(db: Database.Database) {
  return async (request: FastifyRequest, reply: FastifyReply): Promise<void> => {
    try {
      await request.jwtVerify();
    } catch {
      reply.code(401).send({
        error: { code: 'UNAUTHORIZED', message: 'A valid device token is required.', retryAfterSec: null, details: {} },
      });
      return;
    }

    const claims = request.user;
    const row = db.prepare(`SELECT d.token_generation, d.blocked, d.deleted_at AS device_deleted,
        w.deleted_at AS wallet_deleted
      FROM device d
      JOIN device_wallet dw ON dw.device_id = d.id
      JOIN wallet w ON w.id = dw.wallet_id
      WHERE d.id = ? AND w.id = ?`)
      .get(claims.sub, claims.wid) as {
        token_generation: number; blocked: number; device_deleted: number | null; wallet_deleted: number | null;
      } | undefined;
    if (!row || row.device_deleted !== null || row.wallet_deleted !== null ||
        row.token_generation !== claims.gen) {
      reply.code(401).send({
        error: { code: 'UNAUTHORIZED', message: 'The device token is no longer valid.', retryAfterSec: null, details: {} },
      });
      return;
    }
    if (row.blocked) {
      reply.code(403).send({
        error: { code: 'DEVICE_BLOCKED', message: 'This device is blocked.', retryAfterSec: null, details: {} },
      });
      return;
    }
  };
}
