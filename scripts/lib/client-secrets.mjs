/**
 * Does anything secret reach the **shipped client**?
 *
 * The factory already protects the repository: `secret-guard` blocks token patterns in git and
 * `.cursor/rules/security-secrets.mdc` forbids committed keys, keystores and service-account JSON.
 * Nothing protected the APK. A slot that compiled a provider key into `app/src/main/`, or that
 * depended on a provider SDK whose only authentication is a server-side secret, passed every gate
 * this factory had and shipped the credential to whoever unzipped the artifact.
 *
 * The external research behind the rule this module enforces is recorded in
 * `docs/research/2026-09-29-app-factory-prior-art.md` §3 row 8 (change **B1**, the highest-value
 * change in that document) and its source table: of 206,867 real Android apps scanned, 1,207 were
 * LLM-enabled and **66.3 % carried at least one LLM-API misuse** — 41.0 % leaked API credentials,
 * 24.3 % exposed system prompts; an independent scan found secrets in **35 % of 50,000 Play apps**
 * collected Aug–Oct 2025 (18,908 secrets, 1,802 verified live — Stripe keys, GitHub tokens, AI API
 * keys); and roughly **1 in 4 AI-generated code samples** carries a confirmed OWASP vulnerability.
 * The rule is `.cursor/rules/client-side-secrets.mdc` and the gate is
 * `scripts/check-client-secrets.mjs`.
 *
 * What this module claims, and what it cannot
 * -------------------------------------------
 * It claims: no key-shaped literal, no paid-provider SDK, no paid-provider host and no keyed
 * `google-services.json` reaches the client sources of a slot. It cannot claim an APK is
 * secret-free — a key assembled at runtime from three variables, fetched at launch, or hidden in a
 * dependency's own resources is beyond a line-oriented text scan, and this module says so rather
 * than implying completeness.
 *
 * Why the escape hatch is a file, not a comment
 * --------------------------------------------
 * A scanner that a `// client-secrets: ignore` comment can silence is a scanner the agent it judges
 * can silence — the same agent writes the comment. So a match is never suppressed by a marker in the
 * scanned text. Instead:
 *   - a *value* that is a documented placeholder (`YOUR_API_KEY`, `TODO`, `xxxxxxxx`, `example`,
 *     `placeholder`, `<...>`) is **downgraded, not dropped**: it becomes a printed note and never a
 *     failing finding, because it is visibly not a secret while still being visible;
 *   - a **whole file** (and one specific rule in it) may only be exempted through a slot-owned,
 *     reviewable file — `apps/<slot>/client-secrets-exempt.json` — whose entries must carry a
 *     `reason` and a `decidedBy`; an exemption without either is a failure, not an exemption, and
 *     `scanSlot` reports every exemption it applied so a suppression is never invisible.
 *
 * This module is pure: it reads files under a slot directory it is given and returns data. It never
 * resolves a slot, prints, or exits, so `scripts/test-client-secrets.mjs` can drive it against
 * throwaway sandboxes and a CLI failure and a test failure cannot drift apart.
 */
import fs from 'node:fs'
import path from 'node:path'

/**
 * The four rule ids. They are the vocabulary of the exemption file and of the CLI output, so they are
 * exported rather than spelled out at each use site: an exemption naming a rule id the detector does
 * not know could never suppress anything, and the mismatch would be invisible.
 */
export const RULES = Object.freeze({
  /** A key-shaped literal inside the client sources. */
  KEY_LITERAL: 'client-key-literal',
  /** A paid provider's SDK declared on the client classpath. */
  PROVIDER_SDK: 'paid-provider-sdk',
  /** A paid provider's host called from Kotlin/Java. */
  PROVIDER_HOST: 'paid-provider-host',
  /** A keyed `google-services.json` inside the slot. */
  GOOGLE_SERVICES_KEY: 'google-services-key'
})

/** Every declared rule id, so validation messages can be built from the check instead of hand-written. */
export const RULE_IDS = Object.freeze(Object.values(RULES))

/**
 * The file the exemption list lives in: `apps/<slot>/client-secrets-exempt.json`, an array of
 * `{ path, rule, reason, decidedBy }`. Slot-owned and reviewable, unlike a marker comment inside the
 * scanned text — see the module docstring.
 */
export const EXEMPTION_FILE = 'client-secrets-exempt.json'

/**
 * Text extensions the client tree is scanned in. `.toml` is here because `gradle/libs.versions.toml`
 * is where a dependency's coordinates live, and a version catalog can carry a literal just as
 * easily as a `.kt` file; `.pro` because R8/ProGuard rules are part of the shipped build config.
 */
export const SCANNED_EXTENSIONS = Object.freeze([
  '.kt',
  '.java',
  '.xml',
  '.json',
  '.properties',
  '.kts',
  '.gradle',
  '.toml',
  '.pro'
])

/**
 * Substrings that mark a matched value as a documented placeholder rather than a secret. The list is
 * deliberately the boring one: `YOUR_API_KEY`, `TODO`, `xxxxxxxx`, `example`, `placeholder` and the
 * angle-bracket form. A value matching one of these is reported as a note and never fails.
 */
export const PLACEHOLDER_MARKERS = Object.freeze([
  'your',
  'example',
  'placeholder',
  'changeme',
  'change_me',
  'change-me',
  'replace_me',
  'replaceme',
  'xxxx',
  'todo',
  'fixme',
  'dummy',
  'fake',
  'sample',
  'redacted',
  'not_a_real',
  'notreal',
  'insert_your',
  'api_key_here',
  '<',
  '>',
  '${',
  '...'
])

/**
 * Key shapes that only a real credential produces. Each entry is a non-global regex — `scanClientText`
 * compiles a global copy per call, because an exported global regex carries `lastIndex` state between
 * callers and turns a second scan into a silent miss.
 */
export const KEY_SHAPES = Object.freeze([
  {
    id: 'anthropic-api-key',
    provider: 'Anthropic',
    pattern: /\bsk-ant-[A-Za-z0-9_-]{20,}/
  },
  {
    id: 'openai-project-key',
    provider: 'OpenAI',
    pattern: /\bsk-proj-[A-Za-z0-9_-]{20,}/
  },
  {
    id: 'openai-api-key',
    provider: 'OpenAI',
    pattern: /\bsk-[A-Za-z0-9]{20,}/
  },
  {
    id: 'stripe-secret-key',
    provider: 'Stripe',
    pattern: /\b(?:sk_live_|sk_test_|rk_live_)[A-Za-z0-9]{16,}/
  },
  {
    id: 'google-api-key',
    provider: 'Google / Firebase',
    pattern: /\bAIza[0-9A-Za-z_-]{35}\b/
  },
  {
    id: 'aws-access-key-id',
    provider: 'AWS',
    pattern: /\b(?:AKIA|ASIA)[0-9A-Z]{16}\b/
  },
  {
    id: 'slack-token',
    provider: 'Slack',
    pattern: /\bxox[baprs]-[A-Za-z0-9-]{10,}/
  },
  {
    id: 'github-token',
    provider: 'GitHub',
    pattern: /\b(?:ghp_|gho_|github_pat_)[A-Za-z0-9_]{20,}/
  },
  {
    id: 'private-key-pem',
    provider: 'a private key',
    pattern: /-----BEGIN (?:RSA |EC |DSA |OPENSSH |PGP )?PRIVATE KEY-----/
  }
])

/**
 * A generic assignment whose value is long enough to be a credential. It is the rule that catches a
 * provider nobody listed (`val webhookSigningSecret = "..."`), and it is also the rule most likely to
 * be wrong, so it is narrow on purpose: an assignment-looking name, an optional type annotation, then
 * 20+ characters with no whitespace and no placeholder marker.
 */
export const GENERIC_ASSIGNMENT_SOURCE =
  String.raw`\b(?:api[_-]?key|apikey|secret|token|access[_-]?key|client[_-]?secret|signing[_-]?secret)\b` +
  String.raw`\s*(?::\s*[A-Za-z_][\w.<>?\[\]]*)?\s*[:=]\s*(["'])([^\s"']{20,})\1`

/**
 * Coordinates whose SDK is useless — or dangerous — without a secret that cannot live in an APK. This
 * is the list the spec names; `scanSlot` accepts an override so a future slot can add a provider
 * without editing a shared sensor, and the exemption file exists for the case where the dependency is
 * genuinely safe (Stripe's *publishable* key is client-safe; its secret key is not).
 */
export const PROVIDER_COORDINATES = Object.freeze([
  {
    id: 'google-generativeai',
    coordinate: 'com.google.ai.client.generativeai',
    pattern: /com\.google\.ai\.client\.generativeai/,
    why: 'the Generative AI SDK authenticates with a Google API key'
  },
  {
    id: 'firebase-ai',
    coordinate: 'com.google.firebase:firebase-ai',
    pattern: /com\.google\.firebase:firebase-ai/,
    why: 'Firebase AI Logic is a server-proxied API whose client SDK still needs a project key from google-services.json'
  },
  {
    id: 'openai',
    coordinate: 'com.openai',
    pattern: /com\.openai(?=[:"'])/,
    why: 'the official OpenAI SDK authenticates with a project secret key'
  },
  {
    id: 'anthropic',
    coordinate: 'com.anthropic',
    pattern: /com\.anthropic(?=[:"'])/,
    why: 'the Anthropic SDK authenticates with an API key that must stay server-side'
  },
  {
    id: 'stripe',
    coordinate: 'com.stripe',
    pattern: /com\.stripe(?=[:"'])/,
    why: 'Stripe charges money with a secret key; only the publishable key may ship, and a slot that needs the secret key is a server project'
  },
  {
    id: 'aws',
    coordinate: 'com.amazonaws',
    pattern: /com\.amazonaws(?=[:"'])/,
    why: 'an AWS SDK on the client authenticates with credentials that must not ship'
  },
  {
    id: 'mlkit',
    coordinate: 'com.google.android.gms:play-services-mlkit-',
    pattern: /com\.google\.android\.gms:play-services-mlkit-/,
    why: 'several ML Kit artifacts call a Google backend that requires an API key in the client'
  }
])

/**
 * Hosts that are only called by a paid API. A raw call to one from Kotlin/Java either carries a key
 * that ships with the APK, or is dead code that should not ship — both are defects this gate refuses.
 */
export const PROVIDER_HOSTS = Object.freeze([
  { id: 'google-generative-language', pattern: /generativelanguage\.googleapis\.com/ },
  { id: 'openai', pattern: /api\.openai\.com/ },
  { id: 'anthropic', pattern: /api\.anthropic\.com/ },
  { id: 'stripe', pattern: /api\.stripe\.com/ },
  { id: 'dashscope', pattern: /dashscope/ },
  { id: 'openrouter', pattern: /openrouter\.ai/ }
])

/**
 * The build files whose dependency declarations are read as client classpath. `app/src/main/` holds
 * the sources; these hold what those sources are linked against, and a slot cannot ship a provider SDK
 * without naming it here.
 */
export const BUILD_DEPENDENCY_FILES = Object.freeze([
  'app/build.gradle.kts',
  'gradle/libs.versions.toml',
  'build.gradle.kts'
])

/**
 * A line that declares a dependency. The trailing `[=(]` is what keeps this from firing on prose —
 * `# the api of com.openai...` is a comment, not a declaration.
 */
const DEPENDENCY_DECLARATION = /\b(?:implementation|api|compileOnly|runtimeOnly|classpath|kapt|ksp|module)\s*[=(]/

/**
 * Directories a walk must never descend into: build outputs and machine-local state. Scanning them
 * would report generated copies of the sources (a false positive whose fix is impossible), and
 * `build/` is large enough to make the walk pay for it.
 */
const SKIP_DIRECTORIES = new Set([
  'build',
  '.gradle',
  '.kotlin',
  '.cxx',
  '.idea',
  'node_modules',
  'dist',
  'captures'
])

/**
 * True when a value is a documented placeholder rather than a credential. Exported because the CLI and
 * the regression suite both need to agree with the detector about it, and one implementation is how
 * two callers stop drifting.
 */
export function isPlaceholderValue(value) {
  const lowered = String(value).toLowerCase()
  return PLACEHOLDER_MARKERS.some((marker) => lowered.includes(marker))
}

/**
 * True when a long assignment value looks like a credential rather than a sentence: at least one digit
 * or a mixed-case identifier. Without this, `val clientSecret = "a long prose placeholder here"` would
 * be scored as a key; with it, a genuinely random-looking value still is.
 */
function looksLikeSecretValue(value) {
  return /[0-9]/.test(value) || (/[a-z]/.test(value) && /[A-Z]/.test(value))
}

/**
 * Blank out `//` and block comments in Kotlin/Java while leaving every newline in place.
 *
 * This exists for the host rule only: a comment that *mentions* `api.openai.com` is not a call to it,
 * and reporting it would train a reader to ignore the rule. The `//` strip requires a leading
 * whitespace or start of line, because `https://api.openai.com/...` also contains `//` and removing
 * from there would hide the string this rule is for. Keys, by contrast, are scanned in raw text: a key
 * in a comment is still a committed credential.
 */
function stripSourceComments(text) {
  return text
    .replace(/\/\*[\s\S]*?\*\//g, (match) => match.replace(/[^\n]/g, ' '))
    .replace(/(^|\s)\/\/[^\n]*/g, '$1')
}

/**
 * Scan one file's text for key-shaped literals. Pure and side-effect free: the caller says which file
 * this is and what it contains, and gets back what it found.
 *
 * `relativePath` is slot-relative with forward slashes (`app/src/main/java/.../Foo.kt`) so a message
 * and an exemption entry name the same string. Returns `{ findings, notes }`:
 *   - `findings` are the failing rows — `{ rule, provider, file, line, match, message }`;
 *   - `notes` are the same match downgraded because its value is a documented placeholder, plus the
 *     split-key case where the shape was assembled from more than one literal.
 *
 * Two passes per line are deliberate. The raw line is matched so a key written straight into a string,
 * an XML resource or a JSON value is caught; the *dedecorated* line (quotes, `+` and whitespace
 * removed) is matched too, because `"AIza" + "SyDaBc..."` is the same credential as the joined literal
 * and a scanner that only reads the raw line misses it. The dedecoration keeps the line number exact.
 */
export function scanClientText(relativePath, text, options = {}) {
  const keyShapes = options.keyShapes ?? KEY_SHAPES
  const findings = []
  const notes = []
  const file = String(relativePath).split(path.sep).join('/')
  const lines = String(text).split(/\r?\n/)
  const shapes = keyShapes.map((shape) => ({ ...shape, global: new RegExp(shape.pattern.source, 'g') }))
  const generic = new RegExp(GENERIC_ASSIGNMENT_SOURCE, 'gi')

  lines.forEach((line, index) => {
    const lineNumber = index + 1
    // Quotes, apostrophes, `+` and whitespace out of the way, so a value split across literals reads as
    // one run of characters. Nothing else is removed, so an unrelated word cannot be glued onto a match.
    const dedecorated = line.replace(/["'+\s]/g, '')
    const haystacks = dedecorated === line ? [line] : [line, dedecorated]

    for (const shape of shapes) {
      const seen = new Set()
      for (const haystack of haystacks) {
        for (const match of haystack.matchAll(shape.global)) {
          const value = match[0]
          if (seen.has(value)) continue
          seen.add(value)
          if (isPlaceholderValue(value)) {
            notes.push(
              `${file}:${lineNumber} matches the ${shape.provider} key shape (${shape.id}) but its value is a documented placeholder, so it is not a finding`
            )
            continue
          }
          findings.push({
            rule: RULES.KEY_LITERAL,
            provider: shape.id,
            file,
            line: lineNumber,
            match: value,
            message: `${file}:${lineNumber} contains a ${shape.provider} key shape (${shape.id}); a key compiled into app/src/main/ ships inside the APK and anyone can unzip it — remove the call and route the capability through a server, or drop the paid feature (rule ${RULES.KEY_LITERAL})`
          })
        }
      }
    }

    for (const match of line.matchAll(generic)) {
      const value = match[2]
      if (isPlaceholderValue(value)) {
        notes.push(
          `${file}:${lineNumber} assigns a long value to \`${match[1]}\` but the value is a documented placeholder, so it is not a finding`
        )
        continue
      }
      if (!looksLikeSecretValue(value)) {
        notes.push(
          `${file}:${lineNumber} assigns a long value to \`${match[1]}\`; it does not look like a credential (no digit and no mixed case), so it is not a finding`
        )
        continue
      }
      findings.push({
        rule: RULES.KEY_LITERAL,
        provider: 'generic-secret-assignment',
        file,
        line: lineNumber,
        match: value,
        message: `${file}:${lineNumber} assigns a 20+ character value to \`${match[1]}\`; if that value is a credential it ships inside the APK, so move the capability behind a server or replace it with one that needs no secret (rule ${RULES.KEY_LITERAL})`
      })
    }
  })

  return { findings, notes }
}

/**
 * Scan one file for the two rules that are not about literals: a paid provider's SDK on the client
 * classpath, and a raw call to a paid provider's host. Kept separate from `scanClientText` because the
 * claim is different — this is not "a secret is here" but "this client is built to authenticate to a
 * paid API", which is the same defect one step earlier.
 */
export function scanClientImports(relativePath, text, options = {}) {
  const providerCoordinates = options.providerCoordinates ?? PROVIDER_COORDINATES
  const providerHosts = options.providerHosts ?? PROVIDER_HOSTS
  const findings = []
  const notes = []
  const file = String(relativePath).split(path.sep).join('/')
  const isDependencyFile = options.dependencyFile === true
  const isSource = /\.(?:kt|java)$/.test(file)
  const lines = String(text).split(/\r?\n/)
  const hostText = isSource ? stripSourceComments(String(text)) : null
  const hostLines = hostText === null ? null : hostText.split(/\r?\n/)
  // A commented-out dependency is not a dependency, and build files carry commented examples — the
  // scaffold's own ProGuard rules are mostly commented blocks. Reporting those would make the rule
  // unreadable. Kotlin/Gradle use `//`, TOML uses `#`, and the strip preserves newlines so line
  // numbers stay exact.
  const dependencyText = isDependencyFile
    ? file.endsWith('.toml')
      ? String(text).replace(/(^|\s)#[^\n]*/g, '$1')
      : stripSourceComments(String(text))
    : null
  const dependencyLines = dependencyText === null ? null : dependencyText.split(/\r?\n/)

  lines.forEach((line, index) => {
    const lineNumber = index + 1

    if (dependencyLines !== null && DEPENDENCY_DECLARATION.test(dependencyLines[index] ?? '')) {
      const declaration = dependencyLines[index]
      for (const provider of providerCoordinates) {
        if (!provider.pattern.test(declaration)) continue
        findings.push({
          rule: RULES.PROVIDER_SDK,
          provider: provider.id,
          file,
          line: lineNumber,
          match: provider.coordinate,
          message: `${file}:${lineNumber} declares ${provider.coordinate} on the client classpath, and ${provider.why}; a slot that needs it is a server project, so drop the dependency or move the call behind a server (rule ${RULES.PROVIDER_SDK})`
        })
      }
    }

    if (hostLines === null) return
    const hostLine = hostLines[index] ?? ''
    if (hostLine.trim() === '') {
      // Only a comment that actually names a provider host deserves a note; every other comment in the
      // file is blanked here for the same reason, and reporting each of them would drown the output.
      if (providerHosts.some((host) => host.pattern.test(line))) {
        notes.push(`${file}:${lineNumber} is a comment mentioning a provider host; a comment is not a call, so it is not a finding`)
      }
      return
    }
    for (const host of providerHosts) {
      if (!host.pattern.test(hostLine)) continue
      findings.push({
        rule: RULES.PROVIDER_HOST,
        provider: host.id,
        file,
        line: lineNumber,
        match: host.id,
        message: `${file}:${lineNumber} calls ${host.id} from the client; that host is paid and authenticates with a secret, so either the key ships with the APK or the call is dead code — route it through a server or remove it (rule ${RULES.PROVIDER_HOST})`
      })
    }
  })

  return { findings, notes }
}

/**
 * Read a `google-services.json` for a non-placeholder API key. Kept as its own rule because the defect
 * is different: the file is a credential *container*, gitignored for exactly that reason, and it is
 * present at all only when a slot was wired to a Firebase project the factory does not own.
 *
 * An unparseable file fails rather than passing: the rule's claim is "no non-placeholder key is here",
 * and a file that cannot be read cannot support that claim. A file with no key at all is a note — it is
 * still an unexplained artifact in the tree, which is worth seeing.
 */
export function scanGoogleServices(relativePath, text) {
  const file = String(relativePath).split(path.sep).join('/')
  let parsed
  try {
    parsed = JSON.parse(String(text).replace(/^\uFEFF/, ''))
  } catch (error) {
    return {
      findings: [
        {
          rule: RULES.GOOGLE_SERVICES_KEY,
          provider: 'firebase',
          file,
          line: 0,
          match: 'unparseable',
          message: `${file} is a google-services.json with a non-placeholder API key whose text cannot be parsed (${error.message}), so it cannot be proven key-free; remove the file from the slot (rule ${RULES.GOOGLE_SERVICES_KEY})`
        }
      ],
      notes: []
    }
  }

  const values = []
  const visit = (node, keyHint) => {
    if (typeof node === 'string') {
      if (keyHint && ['current_key', 'api_key', 'apikey'].includes(keyHint.toLowerCase())) values.push(node)
      return
    }
    if (Array.isArray(node)) {
      for (const item of node) visit(item, keyHint)
      return
    }
    if (node && typeof node === 'object') {
      for (const [key, value] of Object.entries(node)) visit(value, key)
    }
  }
  visit(parsed, null)

  const findings = []
  const notes = []
  const unique = [...new Set(values)]
  const real = unique.filter((value) => value.trim() !== '' && !isPlaceholderValue(value))
  for (const value of real) {
    findings.push({
      rule: RULES.GOOGLE_SERVICES_KEY,
      provider: 'firebase',
      file,
      line: 0,
      match: value,
      message: `${file} carries a non-placeholder API key; the file is gitignored because it is a credential for a Firebase project that is not part of this slot — remove it from the slot, or replace the capability with one that needs no key (rule ${RULES.GOOGLE_SERVICES_KEY})`
    })
  }
  for (const value of unique) {
    if (real.includes(value)) continue
    notes.push(`${file} declares an api_key whose value is a documented placeholder, so it is not a finding, but the file is still an unexplained client artifact`)
  }
  if (unique.length === 0) {
    notes.push(`${file} is present in the slot but declares no api_key; it is still an unexplained client artifact`)
  }
  return { findings, notes }
}

/**
 * Load and validate `apps/<slot>/client-secrets-exempt.json`.
 *
 * Returns `{ entries, errors }`. An entry must be `{ path, rule, reason, decidedBy }` with a non-empty
 * `reason` and `decidedBy` and a `rule` the detector actually knows; anything else is an error, because
 * an unqualified or misspelled exemption is a suppression nobody can review. Errors are returned rather
 * than thrown so the CLI can report all of them at once and still show the scan it ran.
 */
export function loadExemptions(slotDir) {
  const file = path.join(slotDir, EXEMPTION_FILE)
  if (!fs.existsSync(file)) return { entries: [], errors: [] }

  let raw
  try {
    raw = fs.readFileSync(file, 'utf8')
  } catch (error) {
    return { entries: [], errors: [`${EXEMPTION_FILE} exists but could not be read: ${error.message}`] }
  }

  let parsed
  try {
    parsed = JSON.parse(raw.replace(/^\uFEFF/, ''))
  } catch (error) {
    return { entries: [], errors: [`${EXEMPTION_FILE} is not valid JSON: ${error.message}`] }
  }
  if (!Array.isArray(parsed)) {
    return { entries: [], errors: [`${EXEMPTION_FILE} must be a JSON array of { path, rule, reason, decidedBy }`] }
  }

  const entries = []
  const errors = []
  parsed.forEach((entry, index) => {
    const label = `${EXEMPTION_FILE}[${index}]`
    if (!entry || typeof entry !== 'object' || Array.isArray(entry)) {
      errors.push(`${label} is not an object`)
      return
    }
    const { path: entryPath, rule, reason, decidedBy } = entry
    const missing = [
      ['path', entryPath],
      ['rule', rule],
      ['reason', reason],
      ['decidedBy', decidedBy]
    ].filter(([, value]) => typeof value !== 'string' || value.trim() === '')
    if (missing.length > 0) {
      errors.push(`${label} is missing ${missing.map(([name]) => name).join(', ')}; an exemption without a reason and a decider is not an exemption`)
      return
    }
    if (!RULE_IDS.includes(rule)) {
      errors.push(`${label} names rule "${rule}", which is not one of ${RULE_IDS.join(', ')}`)
      return
    }
    entries.push({
      path: entryPath.split('\\').join('/').replace(/^\.\//, ''),
      rule,
      reason: reason.trim(),
      decidedBy: decidedBy.trim()
    })
  })

  return { entries, errors }
}

/** Every file under `startDir` whose extension is listed, skipping build outputs and tool state. */
function walkFiles(startDir, extensions) {
  const found = []
  const visit = (dir) => {
    let entries
    try {
      entries = fs.readdirSync(dir, { withFileTypes: true })
    } catch {
      return
    }
    for (const entry of entries) {
      const full = path.join(dir, entry.name)
      if (entry.isDirectory()) {
        if (!SKIP_DIRECTORIES.has(entry.name)) visit(full)
        continue
      }
      if (entry.isFile() && extensions.includes(path.extname(entry.name).toLowerCase())) found.push(full)
    }
  }
  visit(startDir)
  return found
}

function readText(file) {
  try {
    return fs.readFileSync(file, 'utf8')
  } catch {
    return null
  }
}

/**
 * The set of files a slot scan reads, as `{ absolute, relative, dependencyFile }`. The client tree is
 * `app/src/main/`; the build files are read for the SDK rule; ProGuard rules are read because they are
 * build config that ships with the release. Walking skips `build/`, so a stale generated copy of a
 * source file can never produce a finding.
 */
export function clientScanTargets(slotDir) {
  const targets = new Map()
  const add = (absolute, dependencyFile) => {
    const relative = path.relative(slotDir, absolute).split(path.sep).join('/')
    const existing = targets.get(relative)
    if (existing) {
      existing.dependencyFile = existing.dependencyFile || dependencyFile
      return
    }
    targets.set(relative, { absolute, relative, dependencyFile })
  }

  for (const absolute of walkFiles(path.join(slotDir, 'app', 'src', 'main'), SCANNED_EXTENSIONS)) add(absolute, false)
  // ProGuard/R8 rules normally sit beside the module, not inside src/main; both locations ship.
  for (const absolute of walkFiles(path.join(slotDir, 'app'), ['.pro'])) add(absolute, false)
  for (const relative of BUILD_DEPENDENCY_FILES) {
    const absolute = path.join(slotDir, relative)
    if (fs.existsSync(absolute)) add(absolute, true)
  }

  return [...targets.values()].sort((left, right) => left.relative.localeCompare(right.relative))
}

/** Every `google-services.json` under the slot's `app/`, plus one at the slot root. */
export function googleServicesFiles(slotDir) {
  const found = walkFiles(path.join(slotDir, 'app'), ['.json']).filter(
    (file) => path.basename(file) === 'google-services.json'
  )
  const atRoot = path.join(slotDir, 'google-services.json')
  if (fs.existsSync(atRoot)) found.push(atRoot)
  return found.map((absolute) => path.relative(slotDir, absolute).split(path.sep).join('/'))
}

/**
 * The verdict for one slot.
 *
 * Returns `{ verdict, rows, notes, findings, exemptions, scannedFiles }` where `verdict` is `PASS`,
 * `FAIL` or `UNVERIFIED`, and `rows` carry `status` and `message` exactly as the CLI prints them.
 *
 * A slot with no `app/src/main/` is `UNVERIFIED`, never `PASS`: there is nothing to scan, and "nothing
 * was scanned" must not be readable as "nothing was found" — the same distinction `--build` makes in
 * `verify-slot.mjs`.
 */
export function scanSlot(slotDir, options = {}) {
  const { entries: exemptions, errors: exemptionErrors } = loadExemptions(slotDir)
  const rows = []
  const notes = []
  const findings = []
  const applied = []
  const usedExemptions = new Set()

  const mainDir = path.join(slotDir, 'app', 'src', 'main')
  if (!fs.existsSync(mainDir)) {
    return {
      verdict: 'UNVERIFIED',
      rows: [
        {
          status: 'UNVERIFIED',
          message: 'no app/src/main/, so there is no client source to scan; a slot with nothing to scan is never a pass'
        }
      ],
      notes,
      findings,
      exemptions: applied,
      scannedFiles: 0
    }
  }

  const targets = clientScanTargets(slotDir)
  let scannedFiles = 0

  for (const target of targets) {
    const text = readText(target.absolute)
    if (text === null) {
      notes.push(`${target.relative} could not be read, so it was not scanned`)
      continue
    }
    scannedFiles += 1
    const literal = scanClientText(target.relative, text, options)
    const imports = scanClientImports(target.relative, text, { ...options, dependencyFile: target.dependencyFile })
    for (const note of [...literal.notes, ...imports.notes]) notes.push(note)
    for (const finding of [...literal.findings, ...imports.findings]) findings.push(finding)
  }

  for (const relative of googleServicesFiles(slotDir)) {
    const text = readText(path.join(slotDir, relative))
    if (text === null) {
      notes.push(`${relative} could not be read, so it was not scanned`)
      continue
    }
    scannedFiles += 1
    const result = scanGoogleServices(relative, text)
    for (const note of result.notes) notes.push(note)
    for (const finding of result.findings) findings.push(finding)
  }

  // Exemptions are applied here, not inside the pure scanners, so the pure functions stay pure and the
  // CLI can report every suppression that actually happened.
  const surviving = []
  for (const finding of findings) {
    const match = exemptions.find((entry) => entry.path === finding.file && entry.rule === finding.rule)
    if (!match) {
      surviving.push(finding)
      continue
    }
    usedExemptions.add(`${match.path}::${match.rule}`)
    applied.push(match)
  }

  for (const error of exemptionErrors) rows.push({ status: 'FAIL', message: error })
  for (const finding of surviving) rows.push({ status: 'FAIL', message: finding.message })
  for (const entry of applied) {
    rows.push({
      status: 'EXEMPT',
      message: `${entry.rule} suppressed for ${entry.path} — ${entry.reason} (decided by ${entry.decidedBy})`
    })
  }
  for (const entry of exemptions) {
    if (!usedExemptions.has(`${entry.path}::${entry.rule}`)) {
      notes.push(`${EXEMPTION_FILE} exempts ${entry.rule} for ${entry.path}, which matches no finding — it can be removed`)
    }
  }
  for (const note of notes) rows.push({ status: 'NOTE', message: note })

  if (scannedFiles === 0) {
    rows.unshift({
      status: 'UNVERIFIED',
      message: 'app/src/main/ exists but no scannable client file could be read, so nothing was verified'
    })
    return { verdict: 'UNVERIFIED', rows, notes, findings: surviving, exemptions: applied, scannedFiles }
  }

  if (exemptionErrors.length === 0 && surviving.length === 0) {
    rows.unshift({
      status: 'PASS',
      message: `scanned ${scannedFiles} client file(s) under app/src/main/ and the slot's build files; no key-shaped literal, paid-provider SDK, paid-provider host or keyed google-services.json`
    })
    return { verdict: 'PASS', rows, notes, findings: surviving, exemptions: applied, scannedFiles }
  }

  if (surviving.length > 0) {
    rows.unshift({
      status: 'FAIL',
      message: `${surviving.length} client-side secret finding(s) across ${scannedFiles} scanned file(s); no shipped slot calls a paid API from the client`
    })
  }
  return { verdict: 'FAIL', rows, notes, findings: surviving, exemptions: applied, scannedFiles }
}
