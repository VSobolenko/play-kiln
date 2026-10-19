/**
 * Server-side service states.
 *
 * Two operator flags and one rule produce four client-visible states:
 *
 *   SERVICE_CLOSED file present  -> closed            (final; we stopped running it)
 *   otherwise kill switch file   -> maintenance       (temporary; ops paused)
 *   otherwise client < minimum   -> update_required   (this build must not spend)
 *   otherwise                    -> ok
 *
 * Order matters: a closed service stays closed even for a current build, and a
 * paused service does not pretend an update would help. The flags are files on
 * disk written by the operator script, so an accidental edit of one flag in a
 * config file cannot produce a half-state.
 */

import { existsSync } from 'node:fs';

import type { ServerConfig } from './config.js';
import { isBelowMinimum } from './versions.js';

export type ServiceState = 'ok' | 'maintenance' | 'closed' | 'update_required';

/** What the operator flags alone say, independent of the caller's client. */
export type OperatorState = 'ok' | 'maintenance' | 'closed';

export interface ServiceNotice {
  /** Stable machine code the client switches on. */
  code: 'SERVICE_CLOSED' | 'SERVICE_MAINTENANCE' | 'UPDATE_REQUIRED';
  /** Locale-tagged copy, so the server owns the wording in every declared language. */
  messageByLocale: { 'en-US': string; 'ru-RU': string };
  /** True only where the client may keep working after showing it. */
  dismissible: boolean;
}

const NOTICES: Record<Exclude<ServiceState, 'ok'>, ServiceNotice> = {
  closed: {
    code: 'SERVICE_CLOSED',
    messageByLocale: {
      'en-US': 'Plinth is closed. New jobs are not accepted; results you already made remain collectable.',
      'ru-RU': 'Plinth закрыт. Новые задания не принимаются; уже созданные результаты можно забрать.',
    },
    dismissible: false,
  },
  maintenance: {
    code: 'SERVICE_MAINTENANCE',
    messageByLocale: {
      'en-US': 'Plinth is paused for maintenance. New jobs are not accepted and nothing new is charged; try again later.',
      'ru-RU': 'Plinth приостановлен на обслуживание. Новые задания не принимаются, новые списания не производятся; попробуйте позже.',
    },
    dismissible: false,
  },
  update_required: {
    code: 'UPDATE_REQUIRED',
    messageByLocale: {
      'en-US': 'This build of Plinth is out of date. Update the app to keep working; nothing is charged here.',
      'ru-RU': 'Эта версия Plinth устарела. Обновите приложение, чтобы продолжить; здесь ничего не списывается.',
    },
    dismissible: false,
  },
};

/** Reads the operator flags. A closed service outranks a maintenance pause. */
export function readOperatorState(
  config: Pick<ServerConfig, 'serviceClosedFile' | 'killSwitchFile'>,
): OperatorState {
  if (existsSync(config.serviceClosedFile)) return 'closed';
  if (existsSync(config.killSwitchFile)) return 'maintenance';
  return 'ok';
}

/** Combines the operator flags with this caller's client version. */
export function resolveServiceState(
  operator: OperatorState,
  clientVersion: string | undefined,
  minimum: string,
): ServiceState {
  if (operator === 'closed') return 'closed';
  if (operator === 'maintenance') return 'maintenance';
  /**
   * An absent version is not a claimed one: `/config` is a read, and a caller
   * that sends no `X-App-Version` (a probe, an older build) gets the operator's
   * answer rather than a fabricated `update_required`. Every route that moves
   * credits decides from the signed `ver` claim instead, where an absent claim
   * fails closed.
   */
  if (clientVersion !== undefined && isBelowMinimum(clientVersion, minimum)) return 'update_required';
  return 'ok';
}

export function noticeFor(state: ServiceState): ServiceNotice | null {
  return state === 'ok' ? null : NOTICES[state];
}

/** New paid work (jobs, recognition, uploads, purchases, recovery codes) is open only in `ok`. */
export function paidWorkIsOpen(operator: OperatorState): boolean {
  return operator === 'ok';
}
