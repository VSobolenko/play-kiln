// Configuration files the factory depends on, and the rule that keeps them honest.
//
// This module exists because of a measured defect: openspec/config.yaml carried the factory's
// `context` and its proposal/tasks rules, and was invalid YAML from the commit that introduced it
// until 2026-09-30. The OpenSpec CLI printed `Warning: could not parse ... ignoring it` and carried
// on, so every change in openspec/changes/ was planned without the project's own rules and nothing
// noticed. A config file is code; the only thing that keeps it honest is something that reads it
// and reports when it cannot.
//
// This is a VALIDATOR, not a parser: the core consumes no configuration value, so reconstructing
// one would be dead code that could itself be wrong. What it decides is the narrow, useful
// question — can a YAML reader accept this file? — and it answers with three outcomes:
//
//   valid          the file uses only the subset below and no prohibited construct
//   invalid        a specific line is malformed; the file would be ignored by a real reader
//   UNVERIFIED     the file uses a construct outside the subset, so this validator refuses to
//                  claim validity rather than guessing. A validator that silently skips what it
//                  does not understand would reproduce exactly the defect it replaces.
//
// Subset: mappings, nested mappings, block sequences, single- and double-quoted scalars, block
// scalars (`|`, `>`, with chomping indicators), comments, blank lines, CRLF or LF.
// Outside the subset: anchors and aliases (`&`, `*`), flow collections (`{`, `[`), explicit tags
// (`!`), complex keys (`? `), and directives (`%`).
import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { fail, guard, pass, ROOT, unverified } from './lib.mjs'

// Every entry here is a file the factory still reads. `scripts/lib/console-form.json` was one until
// the reduce-factory-to-core change archived it as `docs/play-console-observations.json`, which is a
// reading no program reads, so listing it here would turn the deletion into a permanent UNVERIFIED
// row rather than a clean one.
const JSON_CONFIGS = [
  'registry/apps.json',
  'scripts/lib/play-assets.json'
]

const YAML_CONFIGS = ['openspec/config.yaml']

/** Constructs this validator will not judge; seeing one produces UNVERIFIED, never validity. */
const OUT_OF_SUBSET = [
  [/^[&*!]\S/, 'an anchor, alias or tag'],
  [/^%/, 'a directive'],
  [/^\?\s/, 'a complex key'],
  [/^[-:\w][^\s]*\s*:[^\s].*[{[]/, 'a flow collection'],
  [/^[[{&*!]/, 'a flow collection, anchor or tag']
]

export const OUT_OF_SUBSET_NOTE = "outside this validator's documented subset"

/**
 * Validates the YAML subset. Returns `{ problems: string[], unsupported: string[] }`.
 *
 * `problems` name a malformed line; `unsupported` name a construct this reader will not judge.
 *
 * The two shapes the repo actually uses, and which a naive line scanner gets wrong:
 *
 *   a multi-line single-quoted scalar   `- 'First line` … `  last line.'`
 *   a multi-line plain scalar           `- First line` … `  last line.`
 *
 * A quoted scalar is scanned to its closing quote, and everything inside it is text — which is why
 * a colon in there is legal and why the earlier naive version reported the repaired config as
 * broken. A plain scalar is scanned to the last more-indented line that follows it, and the
 * prohibited `: ` sequence is tested over the whole thing, because YAML forbids it on a
 * continuation line just as much as on the first.
 */
export function validateYaml(text) {
  const problems = []
  const unsupported = []
  const lines = text.split(/\r?\n/)

  // Continuation state: which construct, at what indent, and where it started.
  let quoted = null // { quote, startLine }
  let plain = null // { indent }

  const stripComment = (value) => value.replace(/\s+#.*$/, '').replace(/\s+$/, '')

  for (let index = 0; index < lines.length; index += 1) {
    const raw = lines[index]
    const lineNumber = index + 1

    if (raw.trim() === '') {
      // A blank line ends a plain scalar but is legal inside a quoted one.
      if (plain) plain = null
      continue
    }
    if (/^\s*#/.test(raw)) continue
    if (raw.includes('\t')) {
      problems.push(`line ${lineNumber}: a tab character in indentation, which YAML forbids`)
      continue
    }
    if (/^\s*(---|\.\.\.)\s*$/.test(raw)) continue

    const indent = raw.length - raw.replace(/^\s+/, '').length
    const body = raw.slice(indent)

    // Inside a quoted scalar: only the closing quote matters, and it must be the last thing on the
    // line so that an apostrophe inside the text cannot be mistaken for the terminator.
    if (quoted) {
      const trimmed = stripComment(body)
      if (trimmed.endsWith(quoted.quote) && trimmed.length > 1) quoted = null
      continue
    }

    // A more-indented line is a continuation of the plain scalar above it.
    if (plain && indent > plain.indent) {
      if (/:\s/.test(body)) {
        problems.push(
          `line ${lineNumber}: a plain scalar contains ": ", which YAML forbids — ` +
            `quote the whole value`
        )
      }
      continue
    }
    plain = null

    const content = body.startsWith('- ') ? body.slice(2) : body === '-' ? '' : body
    if (content === '') continue
    const itemIndent = indent

    // A block scalar's body is literal text, so colons inside it are not errors.
    const keyed = /^([A-Za-z_][\w.-]*)\s*:\s*(.*)$/.exec(content)
    const scalar = keyed ? keyed[2].trim() : content.trim()
    if (/^[|>][-+]?\d*$/.test(scalar)) {
      let look = index + 1
      let bodyIndent = null
      while (look < lines.length) {
        const line = lines[look]
        if (line.trim() === '') {
          look += 1
          continue
        }
        const lineIndent = line.length - line.replace(/^\s+/, '').length
        if (bodyIndent === null) {
          if (lineIndent <= indent) break
          bodyIndent = lineIndent
        } else if (lineIndent < bodyIndent) break
        look += 1
      }
      index = look - 1
      continue
    }

    for (const [pattern, label] of OUT_OF_SUBSET) {
      if (pattern.test(scalar)) {
        unsupported.push(`line ${lineNumber}: ${label}`)
        break
      }
    }

    if (scalar.startsWith("'") || scalar.startsWith('"')) {
      const quote = scalar[0]
      const trimmed = stripComment(scalar)
      if (!(trimmed.endsWith(quote) && trimmed.length > 1)) {
        quoted = { quote, startLine: lineNumber }
        continue
      }
      // A quoted scalar must not be followed by anything but the closing quote.
      if (!/^(['"]).*\1$/.test(trimmed)) {
        problems.push(`line ${lineNumber}: text after the closing quote of a quoted scalar`)
      }
      continue
    }

    if (!keyed && !body.startsWith('- ') && body !== '-') {
      problems.push(`line ${lineNumber}: not a mapping entry, a sequence item or a comment`)
      continue
    }
    if (/:\s/.test(scalar)) {
      problems.push(
        `line ${lineNumber}: a plain scalar contains ": ", which YAML forbids — ` +
          `quote it: "${scalar.slice(0, 48)}${scalar.length > 48 ? '…' : ''}"`
      )
      continue
    }
    if (scalar !== '') plain = { indent: itemIndent }
  }

  if (quoted) {
    problems.push(`line ${quoted.startLine}: a quoted scalar is never closed (opened here)`)
  }
  return { problems, unsupported }
}

export function checkConfigs() {
  return guard('config.parse', () => {
    const problems = []
    const unsupported = []
    const validated = []

    for (const rel of JSON_CONFIGS) {
      const file = path.join(ROOT, rel)
      if (!existsSync(file)) {
        unsupported.push(`${rel}: absent`)
        continue
      }
      try {
        JSON.parse(readFileSync(file, 'utf8').replace(/^\uFEFF/, ''))
        validated.push(rel)
      } catch (error) {
        problems.push(`${rel}: ${error.message}`)
      }
    }

    for (const rel of YAML_CONFIGS) {
      const file = path.join(ROOT, rel)
      if (!existsSync(file)) {
        unsupported.push(`${rel}: absent`)
        continue
      }
      const { problems: found, unsupported: outside } = validateYaml(readFileSync(file, 'utf8'))
      for (const item of found) problems.push(`${rel}: ${item}`)
      for (const item of outside) unsupported.push(`${rel}: ${item} (${OUT_OF_SUBSET_NOTE})`)
      if (found.length === 0 && outside.length === 0) validated.push(rel)
    }

    if (problems.length > 0) return fail('config.parse', problems.join('; '))
    if (unsupported.length > 0) {
      return unverified('config.parse', `not evaluated: ${unsupported.join(', ')}`)
    }
    return pass('config.parse', `${validated.length} configuration file(s) accepted`)
  })
}

/**
 * The reader is exercised against the exact text of the measured defect, and against the repair
 * that fixed it, so this sensor cannot pass on a validator that accepts everything.
 */
export function checkConfigFixtures() {
  return guard('config.fixtures', () => {
    const cases = []
    const broken = [
      'rules:',
      '  proposal:',
      '    - A proposal with no revenue mechanism is',
      '      incomplete, not merely thin: the factory shipped `x` with'
    ].join('\n')
    const fixed = [
      'rules:',
      '  proposal:',
      "    - 'A proposal with no revenue mechanism is",
      "      incomplete, not merely thin: the factory shipped `x` with'"
    ].join('\n')

    const expectProblem = (label, text, needle) => {
      cases.push([
        label,
        () => {
          const { problems } = validateYaml(text)
          if (problems.length === 0) throw new Error('accepted, expected a problem')
          if (needle && !problems.join(' ').includes(needle)) {
            throw new Error(`problem did not mention ${needle}: ${problems.join(' ')}`)
          }
        }
      ])
    }
    const expectClean = (label, text) => {
      cases.push([
        label,
        () => {
          const { problems, unsupported } = validateYaml(text)
          if (problems.length > 0) throw new Error(`rejected a valid file: ${problems.join(' ')}`)
          if (unsupported.length > 0) throw new Error(`unexpected UNVERIFIED: ${unsupported.join(' ')}`)
        }
      ])
    }

    expectProblem('the measured defect is refused', broken, 'quote the whole value')
    expectClean('quoting the item repairs it', fixed)
    expectClean('a block scalar may contain colons', ['context: |', '  Note: a colon: here.', '  And: here.'].join('\n'))
    expectClean('CRLF is the same as LF', fixed.replace(/\n/g, '\r\n'))
    expectClean('comments and blank lines are ignored', ['# top', 'a: 1', '', '  # indented', 'b: 2'].join('\n'))
    expectClean('a sequence of items is accepted', ['rules:', '  tasks:', '    - one', '    - two'].join('\n'))
    expectClean('an inline mapping inside a sequence is accepted', ['rules:', "  - key: 'value'"].join('\n'))
    expectClean('a two-line plain scalar without a colon is accepted', ['rules:', '  - one two', '    three four'].join('\n'))
    expectProblem('a colon on a plain continuation line is refused', ['rules:', '  - one', '    two: three'].join('\n'), 'plain scalar')
    expectProblem('a tab in indentation is refused', ['a:', '\tb: 1'].join('\n'), 'tab character')
    expectProblem('an unterminated quote is refused', ["a: 'unclosed"].join('\n'), 'never closed')

    cases.push([
      'an anchor reports UNVERIFIED rather than validity',
      () => {
        const { problems, unsupported } = validateYaml(['a: &anchor 1'].join('\n'))
        if (problems.length > 0) throw new Error(`reported a problem instead: ${problems.join(' ')}`)
        if (unsupported.length === 0) throw new Error('claimed validity for a construct it cannot judge')
      }
    ])
    cases.push([
      'a flow collection reports UNVERIFIED rather than validity',
      () => {
        const { problems, unsupported } = validateYaml(['a: [1, 2]'].join('\n'))
        if (problems.length > 0) throw new Error(`reported a problem instead: ${problems.join(' ')}`)
        if (unsupported.length === 0) throw new Error('claimed validity for a construct it cannot judge')
      }
    ])

    const failures = []
    for (const [label, body] of cases) {
      try {
        body()
      } catch (error) {
        failures.push(`${label}: ${error.message}`)
      }
    }
    return failures.length === 0
      ? pass('config.fixtures', `${cases.length} fixtures`)
      : fail('config.fixtures', failures.join(' | '))
  })
}

export function checkConfig() {
  return [checkConfigFixtures(), checkConfigs()]
}
