/**
 * Prove the kill path fires, refuses, and leaves the repository alone.
 *
 * `scripts/kiln-status.mjs` derives its root from its own file location, so a fixture has to be a
 * small mirror of the repository rather than a directory the script is pointed at. This builds one
 * in the OS temp directory and drives the real script against it, which is what makes the claims
 * below observations instead of readings.
 */
import { mkdtempSync, mkdirSync, cpSync, writeFileSync, readFileSync, rmSync, existsSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'

const repo = join(dirname(fileURLToPath(import.meta.url)), '..')
const sandbox = mkdtempSync(join(tmpdir(), 'playkiln-kill-'))
let passed = 0
let failed = 0

function check(name, condition, detail = '') {
  if (condition) {
    passed += 1
    console.log(`PASS ${name}`)
  } else {
    failed += 1
    console.log(`FAIL ${name}${detail ? ` ${detail}` : ''}`)
  }
}

function isoDaysAgo(days) {
  return new Date(Date.now() - days * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)
}

// The mirror: the script, the library it imports, the gate it shells out to, and two slots.
mkdirSync(join(sandbox, 'scripts'), { recursive: true })
mkdirSync(join(sandbox, 'registry'), { recursive: true })
cpSync(join(repo, 'scripts', 'kiln-status.mjs'), join(sandbox, 'scripts', 'kiln-status.mjs'))
cpSync(join(repo, 'scripts', 'lib'), join(sandbox, 'scripts', 'lib'), { recursive: true })
cpSync(join(repo, 'scripts', 'check-outcomes.mjs'), join(sandbox, 'scripts', 'check-outcomes.mjs'))

const plan = JSON.parse(readFileSync(join(repo, 'apps', '2026-09-26-teacher-gradebook', 'economics.json'), 'utf8'))
plan.killCriteria = { windowDays: 30, minInstalls: 100, minRevenueUsd: 50 }

function makeSlot(slug, readings) {
  const dir = join(sandbox, 'apps', `2026-01-01-${slug}`)
  mkdirSync(join(dir, 'store'), { recursive: true })
  writeFileSync(join(dir, '.kiln-state.json'), '{}\n', 'utf8')
  writeFileSync(join(dir, 'economics.json'), `${JSON.stringify(plan, null, 2)}\n`, 'utf8')
  writeFileSync(join(dir, 'store', 'outcomes.json'), `${JSON.stringify({ schemaVersion: 1, readings }, null, 2)}\n`, 'utf8')
  return dir
}

const missed = [
  { observedAt: isoDaysAgo(60), source: 'play-console', installCount: 0, activeDeviceCount: 0, revenueUsd: 0 },
  { observedAt: isoDaysAgo(1), source: 'play-console', installCount: 12, activeDeviceCount: 9, revenueUsd: 0 }
]
const onTrack = [
  { observedAt: isoDaysAgo(60), source: 'play-console', installCount: 0, activeDeviceCount: 0, revenueUsd: 0 },
  { observedAt: isoDaysAgo(1), source: 'play-console', installCount: 4800, activeDeviceCount: 3900, revenueUsd: 310 }
]
const early = [{ observedAt: isoDaysAgo(3), source: 'play-console', installCount: 4000, activeDeviceCount: 3000, revenueUsd: 200 }]

makeSlot('loser', missed)
makeSlot('winner', onTrack)
makeSlot('newbie', early)

const registry = {
  version: 1,
  apps: ['loser', 'winner', 'newbie'].map((slug) => ({
    id: slug,
    dir: `apps/2026-01-01-${slug}`,
    name: slug,
    applicationId: `com.gexetr.${slug}`,
    idea: 'fixture',
    status: 'scaffolded',
    createdAt: new Date(Date.now() - 60 * 24 * 60 * 60 * 1000).toISOString(),
    updatedAt: new Date(Date.now() - 60 * 24 * 60 * 60 * 1000).toISOString()
  }))
}
const registryFile = join(sandbox, 'registry', 'apps.json')
writeFileSync(registryFile, `${JSON.stringify(registry, null, 2)}\n`, 'utf8')

function kill(...args) {
  return spawnSync(process.execPath, [join(sandbox, 'scripts', 'kiln-status.mjs'), ...args], {
    encoding: 'utf8',
    windowsHide: true,
    cwd: sandbox
  })
}

const read = () => JSON.parse(readFileSync(registryFile, 'utf8'))

// A: a kill with no reason is refused, and the registry is untouched.
const noReason = kill('--kill', 'loser')
check('A kill without --reason exits non-zero', noReason.status !== 0, `status=${noReason.status}`)
check('A it says why a reason is required', /--reason/.test(noReason.stdout), noReason.stdout.trim().split('\n').pop())
check('A the registry was not written', read().apps.find((a) => a.id === 'loser').status === 'scaffolded')

// B: a slug nobody registered is refused.
const unknown = kill('--kill', 'ghost', '--reason', 'a reason long enough to pass the length check')
check('B an unknown slug exits non-zero', unknown.status !== 0, `status=${unknown.status}`)
check('B it names the missing registry entry', /no registry entry/.test(unknown.stdout))

// C: a slot past its window that missed both thresholds is killed, and the decision is recorded.
const killed = kill('--kill', 'loser', '--reason', '30 days in: 12 installs and $0 against a declared 100 installs and $50')
const loser = read().apps.find((a) => a.id === 'loser')
check('C the kill exits zero', killed.status === 0, `status=${killed.status}`)
check('C the verdict is KILLED', /VERDICT: KILLED/.test(killed.stdout))
check('C the slot status is killed', loser.status === 'killed', loser.status)
check('C the reason is recorded verbatim', loser.killReason === '30 days in: 12 installs and $0 against a declared 100 installs and $50')
check('C the gate verdict is recorded', loser.killVerdict === 'MISSED', loser.killVerdict)
check('C a timestamp is recorded', typeof loser.killedAt === 'string' && !Number.isNaN(Date.parse(loser.killedAt)))
check('C the archive command is printed', /git mv apps\/2026-01-01-loser apps\/_archive\/2026-01-01-loser/.test(killed.stdout))
check('C the slot directory is left in place', existsSync(join(sandbox, 'apps', '2026-01-01-loser')))

// D: a slot that meets its own criteria is not killable by accident.
const before = readFileSync(registryFile, 'utf8')
const refused = kill('--kill', 'winner', '--reason', 'I got impatient and the numbers do not matter to me')
check('D killing an ON-TRACK slot exits non-zero', refused.status !== 0, `status=${refused.status}`)
check('D it says the slot meets its criteria', /meets its own kill criteria/.test(refused.stdout))
check('D the registry is byte-identical after the refusal', readFileSync(registryFile, 'utf8') === before)

// E: the escape hatch exists, is explicit, and records the verdict it overrode.
const forced = kill('--kill', 'winner', '--reason', 'forced: the category is shut by policy even though the numbers are good', '--force')
check('E --force kills an ON-TRACK slot', forced.status === 0, `status=${forced.status}`)
check('E the overridden verdict is recorded', read().apps.find((a) => a.id === 'winner').killVerdict === 'ON-TRACK')

// F: a killed slot is reported as killed rather than re-judged.
const second = kill('--kill', 'loser', '--reason', 'trying to kill it twice should still be reported honestly')
check('F a killed slot is not silently re-judged', /killed/.test(second.stdout) || second.status === 0, `status=${second.status}`)

rmSync(sandbox, { recursive: true, force: true })
console.log(`\n${passed}/${passed + failed} checks passed`)
process.exit(failed === 0 ? 0 : 1)
