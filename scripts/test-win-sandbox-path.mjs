/**
 * Offline regression tests for sanitising the Windows MSIX execution-alias PATH entry.
 *
 * Run: node scripts/test-win-sandbox-path.mjs
 */
import { codexChildEnv, resolveWindowsAppsAliasDir, sandboxSafePath } from './lib/win-sandbox-path.mjs';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

let failures = 0;

function check(name, actual, expected) {
  const ok = JSON.stringify(actual) === JSON.stringify(expected);
  if (!ok) failures += 1;
  console.log(`  ${ok ? 'PASS' : 'FAIL'}  ${name}`);
  if (!ok) console.log(`        expected ${JSON.stringify(expected)}, got ${JSON.stringify(actual)}`);
}

function withoutLineComments(source) {
  return source.replace(/\/\/[^\r\n]*/g, '');
}

const aliasDir = 'C:\\Users\\vsobo\\AppData\\Local\\Microsoft\\WindowsApps';
const system32 = 'C:\\Windows\\System32';
const python = 'E:\\Programm\\Python\\Python313';
const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');

console.log('=== WindowsApps directory resolution ===');
const originalLocalAppData = process.env.LOCALAPPDATA;
process.env.LOCALAPPDATA = 'D:\\Redirected Local AppData';
check(
  'uses LOCALAPPDATA before the home-directory fallback',
  resolveWindowsAppsAliasDir(),
  'D:\\Redirected Local AppData\\Microsoft\\WindowsApps',
);
if (originalLocalAppData === undefined) delete process.env.LOCALAPPDATA;
else process.env.LOCALAPPDATA = originalLocalAppData;

console.log('=== win32 alias removal ===');
check(
  'drops the exact MSIX alias directory and records it verbatim',
  sandboxSafePath(`${system32};${aliasDir};${python}`, { platform: 'win32', aliasDir }),
  { path: `${system32};${python}`, dropped: [aliasDir] },
);
check(
  'does not drop a differently-pathed directory merely named WindowsApps',
  sandboxSafePath(`${system32};D:\\Tools\\WindowsApps;${python}`, { platform: 'win32', aliasDir }),
  { path: `${system32};D:\\Tools\\WindowsApps;${python}`, dropped: [] },
);
check(
  'drops a case-insensitive alias entry with repeated trailing separators',
  sandboxSafePath(`${system32};${String.raw`C:\USERS\VSOBO\APPDATA\LOCAL\MICROSOFT\WINDOWSAPPS\\`};${python}`, { platform: 'win32', aliasDir }),
  { path: `${system32};${python}`, dropped: [String.raw`C:\USERS\VSOBO\APPDATA\LOCAL\MICROSOFT\WINDOWSAPPS\\`] },
);

console.log('\n=== non-matches preserve PATH ===');
check(
  'returns an unchanged Windows PATH when the alias directory is absent',
  sandboxSafePath(`${system32};${python}`, { platform: 'win32', aliasDir }),
  { path: `${system32};${python}`, dropped: [] },
);
check(
  'does not filter an alias-shaped entry on POSIX',
  sandboxSafePath(`${system32};${aliasDir};${python}`, { platform: 'linux', aliasDir }),
  { path: `${system32};${aliasDir};${python}`, dropped: [] },
);

console.log('\n=== survivor order ===');
check(
  'keeps real directories in their original order',
  sandboxSafePath(`${python};${aliasDir};${system32}`, { platform: 'win32', aliasDir }),
  { path: `${python};${system32}`, dropped: [aliasDir] },
);

console.log('\n=== keep-PATH flag parsing ===');
for (const [value, expected] of [
  [undefined, false],
  ['0', false],
  ['false', false],
  ['FALSE', false],
  ['off', false],
  ['OFF', false],
  ['', false],
  ['1', true],
  ['true', true],
  ['yes', true],
]) {
  const baseEnv = { PATH: `${system32};${aliasDir}` };
  if (value !== undefined) baseEnv.PLAYKILN_CODEX_KEEP_PATH = value;
  check(
    `PLAYKILN_CODEX_KEEP_PATH=${value === undefined ? 'unset' : JSON.stringify(value)} sets skipped=${expected}`,
    codexChildEnv({ baseEnv, platform: 'win32', aliasDir }).skipped,
    expected,
  );
}

console.log('\n=== sandbox-mode PATH policy ===');
const dangerFullAccessEnv = { PATH: `${system32};${aliasDir}` };
check(
  'danger-full-access preserves aliases because it has no restricted-token sandbox',
  codexChildEnv({
    baseEnv: dangerFullAccessEnv,
    platform: 'win32',
    aliasDir,
    sandbox: 'danger-full-access',
  }),
  { env: dangerFullAccessEnv, dropped: [], rewrote: false, skipped: false, reason: 'danger-full-access' },
);

console.log('\n=== Codex launch environment sensor ===');
const launchSensorTargets = [
  {
    file: join(ROOT, 'scripts', 'codex-run.mjs'),
    label: 'Codex runner',
    expectedSpawnSites: 2,
  },
  {
    file: join(ROOT, '.cursor', 'mcp', 'codex-launcher.cjs'),
    label: 'Codex launcher',
    expectedSpawnSites: 1,
    sharedPathImport: /import\(['"]\.\.\/\.\.\/scripts\/lib\/win-sandbox-path\.mjs['"]\)/,
  },
];

function spawnSites(source) {
  return source.match(/(?:spawn|spawnSync)\([\s\S]*?^\s*}\)/gm) ?? [];
}

function everySpawnSiteSuppliesEnv(source) {
  const sites = spawnSites(source);
  return sites.length > 0 && sites.every((site) => /\benv\s*:/.test(withoutLineComments(site)));
}

function withoutFirstSpawnEnv(source) {
  const [firstSite] = spawnSites(source);
  if (!firstSite) return source;
  return source.replace(firstSite, firstSite.replace(/^\s*env\s*:[^\r\n]*\r?\n/m, ''));
}

for (const target of launchSensorTargets) {
  const source = readFileSync(target.file, 'utf8');
  check(`finds ${target.expectedSpawnSites} ${target.label} spawn site(s)`, spawnSites(source).length, target.expectedSpawnSites);
  check(`${target.label} spawn site(s) supply env`, everySpawnSiteSuppliesEnv(source), true);
  if (target.sharedPathImport) {
    check(`${target.label} imports the shared Windows sandbox PATH logic`, target.sharedPathImport.test(source), true);
  }

  const injectedSource = withoutFirstSpawnEnv(source);
  const injectedPass = everySpawnSiteSuppliesEnv(injectedSource);
  console.log(`  ${injectedPass ? 'PASS' : 'FAIL'}  injected missing env in ${target.label} (expected FAIL)`);
  check(`${target.label} injected missing env is rejected`, injectedPass, false);
}
check(
  'a commented-out env property does not satisfy the launch sensor',
  /\benv\s*:/.test(withoutLineComments('spawn(command, args, {\n  // env: childEnvironment.env\n})')),
  false,
);

console.log('');
if (failures === 0) {
  console.log('WIN SANDBOX PATH REGRESSION: all checks passed');
} else {
  console.log(`WIN SANDBOX PATH REGRESSION: ${failures} check(s) failed`);
}
process.exit(failures === 0 ? 0 : 1);
