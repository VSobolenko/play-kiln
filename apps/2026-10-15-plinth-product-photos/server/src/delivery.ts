import type { EventEmitter } from 'node:events';

export interface SendObservable extends EventEmitter {
  statusCode: number;
}

/**
 * Runs `onComplete` only when the response body was completely sent.
 * "Completed" means Node emitted 'finish' on the ServerResponse (end() called and every byte
 * flushed to the socket) with a 200 status. A client abort, socket error or stream failure
 * emits 'close' without 'finish' and never reaches `onComplete`.
 */
export function onCompletedSend(res: SendObservable, onComplete: () => void): void {
  res.once('finish', () => {
    if (res.statusCode === 200) onComplete();
  });
}
