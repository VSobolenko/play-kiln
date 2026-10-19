/**
 * Client-version arithmetic for the server-side minimum-version gate.
 *
 * The version string the server compares is the one the client sent in
 * `POST /v1/session`, where it is bound into the Play Integrity request hash
 * (`integrity.ts::sessionRequestHash`) and re-checked by the attestation
 * payload. It is not a request header a caller may choose per call.
 */

const VERSION_PATTERN = /^[vV]?(\d+(?:\.\d+){0,3})(?:[-+][0-9A-Za-z.-]+)?$/;

/** Parses `1`, `1.2`, `1.2.3`, `1.2.3.4` (optional `v` prefix and suffix). Null when unparseable. */
export function parseVersion(value: string): number[] | null {
  const match = VERSION_PATTERN.exec(value.trim());
  if (!match || match[1] === undefined) return null;
  return match[1].split('.').map((part) => Number(part));
}

function compareParsed(a: readonly number[], b: readonly number[]): number {
  const length = Math.max(a.length, b.length);
  for (let index = 0; index < length; index += 1) {
    const left = a[index] ?? 0;
    const right = b[index] ?? 0;
    if (left !== right) return left < right ? -1 : 1;
  }
  return 0;
}

/** Negative when `a < b`, zero when equal, positive when `a > b`; null when either is unparseable. */
export function compareVersions(a: string, b: string): number | null {
  const left = parseVersion(a);
  const right = parseVersion(b);
  if (!left || !right) return null;
  return compareParsed(left, right);
}

/** A minimum of `0`, `0.0.0`, empty or unparseable means "no minimum" (the machine default). */
export function minimumIsEnforceable(minimum: string): boolean {
  if (!minimum || minimum.trim() === '') return false;
  const parsed = parseVersion(minimum);
  if (!parsed) return false;
  return parsed.some((part) => part > 0);
}

/**
 * True when the client must be refused. An unknown client version is below any
 * enforceable minimum: failing closed is the only safe direction for money.
 */
export function isBelowMinimum(clientVersion: string | undefined, minimum: string): boolean {
  if (!minimumIsEnforceable(minimum)) return false;
  const minimumParsed = parseVersion(minimum)!;
  const clientParsed = clientVersion === undefined ? null : parseVersion(clientVersion);
  if (!clientParsed) return true;
  return compareParsed(clientParsed, minimumParsed) < 0;
}
