import { homedir } from 'node:os';
import { join } from 'node:path';

/**
 * Resolves the MSIX application-execution alias directory, with an override for
 * deterministic offline callers and tests.
 */
export function resolveWindowsAppsAliasDir({ aliasDir } = {}) {
  const localAppData = process.env.LOCALAPPDATA ?? join(homedir(), 'AppData', 'Local');
  return aliasDir ?? join(localAppData, 'Microsoft', 'WindowsApps');
}

function comparableWindowsDirectory(value) {
  return String(value)
    .replace(/\//g, '\\')
    .replace(/\\+/g, '\\')
    .replace(/\\+$/, '')
    .toLowerCase();
}

/**
 * Removes the Windows MSIX execution-alias directory from a worker PATH.
 * The helper has no filesystem or environment access, apart from its explicit
 * platform and alias-directory defaults, so callers can exercise it offline.
 */
export function sandboxSafePath(pathValue, options = {}) {
  const platform = options.platform ?? process.platform;
  if (platform !== 'win32' || typeof pathValue !== 'string') {
    return { path: pathValue, dropped: [] };
  }

  const aliasDir = resolveWindowsAppsAliasDir(options);
  const comparableAliasDir = comparableWindowsDirectory(aliasDir);
  const dropped = [];
  const survivors = [];

  for (const entry of pathValue.split(';')) {
    if (comparableWindowsDirectory(entry) === comparableAliasDir) {
      dropped.push(entry);
    } else {
      survivors.push(entry);
    }
  }

  return { path: survivors.join(';'), dropped };
}

/**
 * Parses the explicit opt-out for preserving a worker's inherited PATH.
 * `0`, `false`, and `off` match the factory's existing boolean-environment conventions.
 */
export function parseCodexKeepPath(value) {
  const normalized = String(value ?? '').toLowerCase();
  return normalized !== '' && normalized !== '0' && normalized !== 'false' && normalized !== 'off';
}

/**
 * Builds the environment for a Codex child from the environment explicitly
 * handed to it. Keeping this separate from the pure PATH filter lets both
 * launch paths use one policy without reading global process state in tests.
 */
export function codexChildEnv({ baseEnv, platform, aliasDir, sandbox = 'workspace-write' } = {}) {
  if (!baseEnv) throw new TypeError('codexChildEnv requires baseEnv');

  if (sandbox === 'danger-full-access') {
    return { env: baseEnv, dropped: [], rewrote: false, skipped: false, reason: 'danger-full-access' };
  }

  const skipped = parseCodexKeepPath(baseEnv.PLAYKILN_CODEX_KEEP_PATH);
  if (skipped) {
    return { env: baseEnv, dropped: [], rewrote: false, skipped: true, reason: 'keep-path' };
  }

  const safePath = sandboxSafePath(baseEnv.PATH, { platform, aliasDir });
  return {
    env: { ...baseEnv, PATH: safePath.path },
    dropped: safePath.dropped,
    rewrote: true,
    skipped: false,
    reason: 'filtered',
  };
}
