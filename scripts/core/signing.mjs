// The signing contract's own sensor.
//
// What it measures, and why it exists: the factory had two answers to "where does the upload key
// live and how is it made". `scripts/sign-release.mjs --generate` writes the keystore to
// `<homedir>/.playkiln/keystores/<applicationId>.jks`; the `keystore.properties.example` every slot
// inherited from the scaffold named a manual `keytool -genkeypair` into `../upload.jks`, a path the
// generator never uses. The owner's report is that at signing time the agent had to ask him how to
// proceed. That is not a formatting drift — it is a template that contradicts the generator.
//
// So `scripts/lib/signing.mjs` owns the location and the parameters, and this check fails when the
// scaffold's or any slot's example differs from the text that module generates, naming the file and
// the first differing line; it fails separately when a template's `storeFile` does not have the
// keystore's shape — the directory `.playkiln/keystores` and the filename `<applicationId>.jks`. The
// committed example spells the home directory as a placeholder, so that shape half is machine
// independent and the whole check is green on any machine, in a fresh clone and in CI. A
// project-wide check, because the defect is "a slot was born with the wrong template" — the scaffold
// is the source and every slot is an instance.
//
// Honest limit, stated rather than implied: this proves the *recorded* shape is consistent. It
// cannot prove a key exists, that its password is recoverable, or that a keystore belongs to the app
// whose properties name it — `sign-release.mjs` mints and inspects the key, under the owner's
// authorization.
import { existsSync, readFileSync, readdirSync } from 'node:fs'
import path from 'node:path'
import { scaffoldPackage } from '../lib/namespaces.mjs'
import {
  applicationIdFromSlot,
  exampleStoreFilePathFor,
  HOME_PLACEHOLDER,
  keystorePropertiesExample,
  storeFileProblem,
  storeFilePathFor
} from '../lib/signing.mjs'
import { expectEqual, expectTrue, fail, guard, pass, ROOT, runFixtures } from './lib.mjs'

const SCAFFOLD_EXAMPLE = path.join(ROOT, 'scaffold', 'android-compose', 'keystore.properties.example')
const APPS_DIR = path.join(ROOT, 'apps')

const relativeToRoot = (file) => path.relative(ROOT, file).replaceAll('\\', '/')

/** The first line on which two texts disagree, in a form a reader can act on. */
export function firstDifferingLine(expected, actual) {
  const expectedLines = expected.split(/\r?\n/)
  const actualLines = actual.split(/\r?\n/)
  const length = Math.max(expectedLines.length, actualLines.length)
  for (let index = 0; index < length; index += 1) {
    const want = expectedLines[index]
    const got = actualLines[index]
    if (want !== got) {
      return (
        `line ${index + 1}: expected ${JSON.stringify(want ?? '<no line>')}, ` +
        `got ${JSON.stringify(got ?? '<no line>')}`
      )
    }
  }
  return null
}

/**
 * Every problem with one example file, given the text the module would generate for its slot.
 *
 * Two independent halves: the whole-text comparison catches any drift, and the `storeFile` check
 * catches the defect itself — a path the generator would not use — even if the surrounding prose
 * were somehow made to match.
 */
export function exampleProblems(file, expectedText, applicationId) {
  const problems = []
  if (!existsSync(file)) {
    problems.push(`${relativeToRoot(file)} is missing`)
    return problems
  }
  const actual = readFileSync(file, 'utf8')
  const drift = firstDifferingLine(expectedText, actual)
  if (drift) {
    problems.push(`${relativeToRoot(file)}: ${drift}`)
  }
  const pathProblem = storeFileProblem(actual, applicationId)
  if (pathProblem) {
    problems.push(`${relativeToRoot(file)}: ${pathProblem}`)
  }
  return problems
}

/** Slot directories under apps/, each with the applicationId its Gradle file declares. */
function slotExamples() {
  if (!existsSync(APPS_DIR)) return []
  const rows = []
  for (const entry of readdirSync(APPS_DIR, { withFileTypes: true })) {
    if (!entry.isDirectory()) continue
    const slotDir = path.join(APPS_DIR, entry.name)
    if (!existsSync(path.join(slotDir, 'app', 'build.gradle.kts'))) continue
    rows.push({
      slotDir,
      example: path.join(slotDir, 'keystore.properties.example'),
      applicationId: applicationIdFromSlot(slotDir)
    })
  }
  return rows
}

export function checkSigningExample() {
  return guard('signing.example', () => {
    const problems = []
    let checked = 0

    problems.push(...exampleProblems(SCAFFOLD_EXAMPLE, keystorePropertiesExample(scaffoldPackage), scaffoldPackage))
    checked += 1

    for (const row of slotExamples()) {
      checked += 1
      if (!row.applicationId) {
        problems.push(`${relativeToRoot(row.example)}: no applicationId could be read from app/build.gradle.kts`)
        continue
      }
      problems.push(...exampleProblems(row.example, keystorePropertiesExample(row.applicationId), row.applicationId))
    }

    if (problems.length > 0) return fail('signing.example', problems.join('; '))
    return pass(
      'signing.example',
      `${checked} keystore.properties.example file(s) match the module the generator reads`
    )
  })
}

/**
 * The sensor made falsifiable.
 *
 * Each leg drives the pure functions the check is built from against a known verdict, so a sensor
 * that has stopped being able to fail is visible in the same report as the files it judges. The
 * "wrong path" legs are the ones that keep the path half load-bearing: neuter `storeFileProblem()`
 * and this suite goes red, which is the point — a whole-text comparison alone would report the
 * defect as a formatting diff and lose the fact that it is a wrong location.
 *
 * Two legs cover the other half of this check's purpose, added when the template stopped embedding
 * the machine's home directory: the committed example must carry `HOME_PLACEHOLDER` and must not
 * contain this machine's real absolute keystore path. Without them a future change could re-embed a
 * home path and still pass every other leg, because the whole-text comparison would agree with the
 * module it was rendered from.
 */
export function checkSigningFixtures() {
  return guard('signing.fixtures', () =>
    runFixtures('signing.fixtures', [
      [
        'the generated example names the keystore\'s shape for this applicationId',
        () => {
          const applicationId = 'com.gexetr.demo'
          const text = keystorePropertiesExample(applicationId)
          expectTrue(
            text.includes(exampleStoreFilePathFor(applicationId)),
            'the example text does not carry the placeholder keystore path the generator\'s shape implies'
          )
          expectEqual(storeFileProblem(text, applicationId), null, 'the generated text is self-consistent')
        }
      ],
      [
        'the committed example is machine-independent: placeholder home, not this machine\'s real path',
        () => {
          const applicationId = 'com.gexetr.demo'
          const text = keystorePropertiesExample(applicationId)
          expectTrue(
            text.includes(HOME_PLACEHOLDER),
            `the example must spell the home directory as ${HOME_PLACEHOLDER}`
          )
          const realPath = storeFilePathFor(applicationId)
          expectTrue(
            !text.includes(realPath),
            'the committed example must not embed this machine\'s absolute keystore path'
          )
        }
      ],
      [
        'a template pointing at ../upload.jks is reported as a wrong path, not as a formatting diff',
        () => {
          const applicationId = 'com.gexetr.demo'
          const problem = storeFileProblem('# storeFile=../upload.jks\n# storePassword=\n', applicationId)
          expectTrue(problem !== null, 'the wrong path was not detected')
          expectTrue(problem.includes('../upload.jks'), `the finding must name the bad value: ${problem}`)
        }
      ],
      [
        'a template naming another applicationId\'s keystore is reported, naming the value',
        () => {
          const problem = storeFileProblem(
            '# storeFile=<home>/.playkiln/keystores/com.gexetr.other.jks\n',
            'com.gexetr.demo'
          )
          expectTrue(problem !== null, 'another applicationId\'s keystore was not detected')
          expectTrue(problem.includes('com.gexetr.other.jks'), `the finding must name the bad value: ${problem}`)
        }
      ],
      [
        'a template whose directory is not .playkiln/keystores is reported, naming the value',
        () => {
          const problem = storeFileProblem('# storeFile=<home>/.playkiln/keys/com.gexetr.demo.jks\n', 'com.gexetr.demo')
          expectTrue(problem !== null, 'the wrong directory was not detected')
          expectTrue(
            problem.includes('.playkiln/keys/com.gexetr.demo.jks'),
            `the finding must name the bad value: ${problem}`
          )
        }
      ],
      [
        'a template with no storeFile line is reported',
        () => {
          expectTrue(storeFileProblem('# nothing here\n', 'com.gexetr.demo') !== null, 'a missing path was not detected')
        }
      ],
      [
        'a single changed comment is reported with its line number',
        () => {
          const applicationId = 'com.gexetr.demo'
          const expected = keystorePropertiesExample(applicationId)
          const drifted = expected.replace('do not hand-write this file', 'feel free to hand-write this file')
          const detail = firstDifferingLine(expected, drifted)
          expectTrue(detail !== null, 'the drift was not detected')
          expectTrue(detail.startsWith('line 1:'), `the first differing line must be named: ${detail}`)
        }
      ]
    ])
  )
}
