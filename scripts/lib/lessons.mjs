/**
 * Every lesson must leave a sensor behind, or say out loud that it cannot.
 *
 * `harness/lessons.md` is the factory's memory: symptom, root cause, fix class, and what the fix left
 * behind. The rule that keeps it honest is in `.cursor/rules/verification-discipline.mdc` — "every
 * repeated defect earns a regression check" and "a check that cannot fail is not a check" — and until
 * now that rule was itself only prose in the journal. A lesson whose fix left no check is a lesson the
 * factory can repeat, and nothing could tell the difference between an entry that closed its defect
 * and an entry that merely described it.
 *
 * So the journal now carries a claim the repository can test: a bullet that begins with `Sensor`,
 * stating what fails today if this defect comes back. Three shapes are honest, and the check accepts
 * exactly those:
 *
 *   1. `- Sensor: scripts/test-x.mjs` — one or more backticked repository paths that **exist**, at
 *      least one of them outside `.kiln/`. A path that is not there is a belief about a check rather
 *      than a check, which is the defect this check exists to catch. **A sensor that names no
 *      repository path at all fails too**: a bullet that says only "measured before it was trusted,
 *      the suite went red" cannot fail when the suite is deleted or renamed, and that silent hole is
 *      exactly what this contract closes.
 *   2. `- Sensor measured before it was trusted: ... \`scripts/test-x.mjs\` ...` — the form the
 *      entries dated 2026-09-29 use, because it states the path *and* how it was shown to fail. Accepted
 *      unchanged rather than rewritten: the journal's history is evidence, not a style guide. The
 *      path is what the check reads; the prose beside it is what a reader reads.
 *   3. `- Sensor: none - <reason>` — an explicit "no sensor is possible", with a reason. A console
 *      sequence or a one-off UI observation legitimately leaves nothing to run, and saying so is a
 *      decision a reader can challenge. Silence is not: an entry that simply omits the bullet hides
 *      which of the three cases it is, and that is the defect.
 *
 * A `.kiln/` path does not satisfy the contract on its own, because `.kiln/` is disposable local state
 * by design (`AGENTS.md`) and a sensor that lives only there disappears with the next cleanup. It is
 * reported rather than failed: citing a throwaway injection harness beside a durable check is
 * legitimate and useful.
 *
 * The contract applies from `SENSOR_CONTRACT_FROM` forward. The earlier entries are not rewritten:
 * they are counted and the count is printed, so the gap is visible without pretending the history was
 * written under a rule that did not exist yet.
 *
 * This module is pure — it takes the journal text and an `exists` predicate — so it can be driven
 * against a synthetic journal as well as against the real one. The contract's own demonstration is
 * recorded in `docs/self-learning.md`: a `Sensor` bullet pointed at a file that is not there fails
 * and names both the lesson and the missing path.
 */

/** The first date the contract binds. Earlier entries are counted, never rewritten. */
export const SENSOR_CONTRACT_FROM = '2026-09-29'

/**
 * A path is only worth calling a sensor if it can be opened. Tokens that are not paths are ignored, so
 * a bullet quoting a number (`\`456 -> 459\``) or a flag (`\`--prove\``) never counts as a citation.
 */
const PATH_TOKEN = /[\w.@-]+(?:\/[\w.@-]+)*\.(?:mjs|cjs|js|py|json|md|kt|kts|toml|xml|ya?ml)\b/

const SENSOR_BULLET = /^-\s*Sensor\b/i
/**
 * Shape 3 is a *declaration*, not the word: the bullet has to open `- Sensor: none` and then give a
 * reason. Testing for `none` anywhere in the bullet was too loose to be a check — a long sensor
 * bullet that quoted a FAIL row containing the word ("carries no screen with that id and none whose
 * console label is ...") was counted as an explicit "no sensor is possible", which silently turned a
 * real 431-leg suite into a `none`. A quoted row is prose about a check; this is a statement that
 * there is none.
 */
const NONE_CLAIM = /^-\s*Sensor\s*:\s*none\b/i

/** A `none` claim needs a reason, not a shrug: the bullet has to say why nothing can be run. */
const MIN_NONE_REASON = 40
/** ...and a real sensor bullet has to say more than the word `Sensor:`. */
const MIN_SENSOR_LENGTH = 20

/**
 * Splits the journal into entries. An entry is `### <date> - <title>` followed by bullets; a `###`
 * heading that is not a dated entry (the journal's preamble has one) is skipped rather than guessed at.
 */
export function parseLessons(text) {
  const entries = []
  const chunks = String(text || '').split(/^### /m)
  let line = 1
  for (const chunk of chunks) {
    const lines = chunk.split(/\r?\n/)
    if (!/^\d{4}-\d{2}-\d{2}\b/.test(lines[0] || '')) {
      line += lines.length
      continue
    }
    entries.push({
      date: lines[0].match(/^(\d{4}-\d{2}-\d{2})/)[1],
      title: lines[0].trim(),
      body: lines.slice(1).join('\n'),
      startLine: line
    })
    line += lines.length
  }
  return entries
}

/**
 * Normalises a backtick token to a **repository-relative path**, or `null` when it is not one.
 *
 * A path must contain a `/`. That is not pedantry: the journal names bare filenames (`economics.json`,
 * `console-answers.md`) when it means a *generated artifact of some slot*, and those are not paths in
 * the repository, so resolving them against the root would report a missing sensor for an entry that
 * is correctly citing the pack's own document. A token without a separator is therefore not a citation
 * and is ignored — the same reasoning that makes `\`456 -> 459\`` and `\`--prove\`` not citations.
 */
export function asPath(token) {
  const candidate = String(token).replace(/^\.\//, '').trim()
  if (!candidate || /\s/.test(candidate) || candidate.includes('*') || candidate.includes('<')) return null
  if (!candidate.includes('/')) return null
  if (!/^[\w.@/-]+$/.test(candidate)) return null
  if (!/\.[a-z0-9]+$/i.test(candidate)) return null
  return PATH_TOKEN.test(candidate) ? candidate : null
}

/** The runners a sensor citation may name before the script it runs. */
const COMMAND_RUNNERS = new Set([
  'node',
  'npm',
  'npx',
  'python',
  'python3',
  'py',
  'pwsh',
  'powershell',
  'bash',
  'sh',
  'zsh',
  'cmd',
  'gradlew',
  'git',
  'java',
  'keytool',
  'sdkmanager'
])

/**
 * The repository path a sensor citation resolves to, or `null`.
 *
 * A citation is often a *command* rather than a bare path - `node scripts/test-x.mjs --prove`,
 * `python scripts/store-kit.py --observations`, `scripts/store-kit.py --observations` - and the file
 * that has to exist is the script the command runs, not the whole line. So a token that is not a path
 * on its own is read as a command when it *is* one, and only then: a named runner (`node`, `python`,
 * `gradlew`, ...) followed by a script, or a bare script path followed only by flags. A prose
 * fragment that merely contains a path - a leg label like `FAIL ... requires store/console-answers.md
 * of every pack` - is not a citation and resolves to `null`, so a sentence about a check is never
 * mistaken for a path this repository must have.
 */
export function asCommandPath(token) {
  const whole = asPath(token)
  if (whole) return whole
  const parts = String(token).trim().split(/\s+/).filter(Boolean)
  if (parts.length === 0) return null
  if (COMMAND_RUNNERS.has(parts[0].toLowerCase())) {
    for (const part of parts.slice(1)) {
      if (part.startsWith('-')) continue
      const candidate = asPath(part)
      if (candidate) return candidate
    }
    return null
  }
  if (parts.length > 1 && asPath(parts[0]) && parts.slice(1).every((part) => part.startsWith('-'))) {
    return asPath(parts[0])
  }
  return null
}

/**
 * The verdict for one journal.
 *
 * `exists` is injected so the module stays pure and the suite can hand it a sandbox filesystem.
 * Returns `{ verdict, rows, notes, stats }`, where `rows` carry `status` and `message` exactly as the
 * CLI prints them.
 *
 * `requirePath` is accepted and ignored: naming a repository path is now the default obligation
 * rather than an opt-in, so `--require-path` no longer changes the verdict. It stays in the option
 * shape so an existing caller does not break.
 */
export function evaluateLessons(text, exists, { from = SENSOR_CONTRACT_FROM, requirePath = false } = {}) {
  const entries = parseLessons(text)
  const bound = entries.filter((entry) => entry.date >= from)
  const grandfathered = entries.filter((entry) => entry.date < from)
  const rows = []
  const notes = []
  const covered = []
  let proseOnly = 0

  for (const entry of bound) {
    const bullet = entry.body.split(/\r?\n/).find((line) => SENSOR_BULLET.test(line))
    const where = `"${entry.title}"`

    if (!bullet) {
      rows.push({
        status: 'FAIL',
        message: `${where} leaves no sensor: add \`- Sensor: <path>\`, or \`- Sensor: none - <why nothing can be run>\``
      })
      continue
    }

    const paths = [...bullet.matchAll(/`([^`]+)`/g)].map((match) => asCommandPath(match[1])).filter(Boolean)
    const disposable = paths.filter((path) => path.startsWith('.kiln/'))
    const missing = paths.filter((path) => !path.startsWith('.kiln/') && !exists(path))
    const durable = paths.filter((path) => !path.startsWith('.kiln/') && exists(path))

    if (missing.length > 0) {
      rows.push({ status: 'FAIL', message: `${where} names a sensor that is not there: ${missing.join(', ')}` })
      continue
    }

    if (durable.length > 0) {
      covered.push({ title: entry.title, kind: 'sensor', paths: durable })
      if (disposable.length > 0) {
        notes.push(`${where} also cites disposable local state (${disposable.join(', ')}); the durable check is what counts`)
      }
      continue
    }

    const claimsNone = NONE_CLAIM.test(bullet)
    if (claimsNone && bullet.length >= MIN_NONE_REASON) {
      covered.push({ title: entry.title, kind: 'none' })
      continue
    }

    // A bullet that declares a sensor without naming a file that exists. This is the hole the
    // contract exists to close: `- Sensor: measured before it was trusted ...` reads exactly like a
    // check until the check is deleted or renamed, and a sensor that cannot be opened is a belief
    // about a check rather than a check. Naming the file is the whole obligation; prose beside it is
    // welcome and is what the entries dated 2026-09-29 already carry.
    if (durable.length === 0 && paths.length === 0 && !claimsNone && bullet.length >= MIN_SENSOR_LENGTH) {
      proseOnly += 1
      rows.push({
        status: 'FAIL',
        message:
          `${where} declares a sensor that names no repository path, so nothing here can fail when ` +
          'the check is deleted or renamed: name the check it drives in backticks, or say ' +
          '`- Sensor: none - <why nothing can be run>`'
      })
      continue
    }

    if (bullet.length < MIN_SENSOR_LENGTH) {
      rows.push({ status: 'FAIL', message: `${where} has a sensor bullet that says nothing: ${bullet.trim()}` })
      continue
    }

    rows.push({
      status: 'FAIL',
      message:
        claimsNone
          ? `${where} declares \`Sensor: none\` without a reason: a none-claim has to say why nothing can be run, in ${MIN_NONE_REASON}+ characters, or name the check it drives in backticks`
          : disposable.length > 0
            ? `${where} names only a .kiln/ harness (${disposable.join(', ')}), which is disposable local state; name the durable check it drove, or say \`- Sensor: none - <why>\``
            : `${where} names no repository path and does not claim \`none\`: ${bullet.trim()}`
    })
  }

  const hasSensor = (entry) => entry.body.split(/\r?\n/).some((line) => SENSOR_BULLET.test(line))
  const stats = {
    total: entries.length,
    bound: bound.length,
    covered: covered.length,
    none: covered.filter((entry) => entry.kind === 'none').length,
    prose: proseOnly,
    grandfathered: grandfathered.length,
    grandfatheredWithoutSensor: grandfathered.filter((entry) => !hasSensor(entry)).length
  }

  return { verdict: rows.some((row) => row.status === 'FAIL') ? 'FAIL' : 'PASS', rows, notes, stats }
}
