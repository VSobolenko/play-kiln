/**
 * Route guards for the two server-side refusals that are not authentication:
 * the service-state gate and the minimum-version gate.
 *
 * Both run after `authenticate`, because the version comes from the signed JWT
 * (bound at session creation) and the state refusal should name the client it
 * refused. Both fail closed: an unreadable client version is below any
 * enforceable minimum.
 */

import type { FastifyReply, FastifyRequest } from 'fastify';

import type { ServerConfig } from './config.js';
import { readOperatorState } from './service-state.js';
import { isBelowMinimum } from './versions.js';

export interface ErrorBody {
  error: {
    code: string;
    message: string;
    details: Record<string, unknown>;
  };
}

export function refusal(
  reply: FastifyReply,
  status: number,
  code: string,
  message: string,
  details: Record<string, unknown> = {},
): FastifyReply {
  const body: ErrorBody = { error: { code, message, details } };
  return reply.code(status).send(body);
}

/** The client version bound into this session's JWT, when the token carries one. */
export function clientAppVersion(request: FastifyRequest): string | undefined {
  const user = request.user as { ver?: unknown } | undefined;
  return typeof user?.ver === 'string' && user.ver.length > 0 ? user.ver : undefined;
}

/**
 * Refuses new paid work while the operator has paused or closed the service.
 * Reads stay open (status, result download, wallet balance, data deletion), so
 * credits already spent are still collectable and a deletion request still works.
 */
export function requirePaidWorkOpen(config: ServerConfig) {
  return async (request: FastifyRequest, reply: FastifyReply): Promise<void> => {
    const operator = readOperatorState(config);
    if (operator === 'ok') return;
    request.log.warn({ code: operator === 'closed' ? 'SERVICE_CLOSED' : 'SERVICE_MAINTENANCE' }, 'paid work refused');
    if (operator === 'closed') {
      refusal(
        reply,
        503,
        'SERVICE_CLOSED',
        'Plinth is closed: new jobs, purchases and recognition are not accepted.',
      );
      return;
    }
    refusal(
      reply,
      503,
      'SERVICE_MAINTENANCE',
      'Plinth is paused for maintenance: new jobs, purchases and recognition are not accepted. Nothing was charged.',
    );
  };
}

/**
 * Refuses every mutating route for a client older than `MIN_APP_VERSION`.
 * `426 Upgrade Required` is a distinct status and a distinct code, so the client
 * can tell "update the app" from "come back later" without parsing prose.
 */
export function requireMinimumVersion(config: ServerConfig) {
  return async (request: FastifyRequest, reply: FastifyReply): Promise<void> => {
    const version = clientAppVersion(request);
    if (!isBelowMinimum(version, config.minAppVersion)) return;
    request.log.warn(
      { code: 'UPDATE_REQUIRED', clientVersion: version ?? 'unknown' },
      'outdated client refused',
    );
    refusal(
      reply,
      426,
      'UPDATE_REQUIRED',
      'This build is below the minimum supported version. Update the app before spending credits.',
      { minAppVersion: config.minAppVersion },
    );
  };
}
