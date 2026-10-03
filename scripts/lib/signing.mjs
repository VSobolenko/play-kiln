#!/usr/bin/env node
/**
 * The upload-signing contract — single source of truth.
 *
 * Why this module exists
 * ----------------------
 * The factory had two answers to "where does the upload key live and how is it made", and they
 * disagreed. `scripts/sign-release.mjs --generate` writes the keystore to
 * `<homedir>/.playkiln/keystores/<applicationId>.jks` and writes a `keystore.properties` beside the
 * slot; the `keystore.properties.example` that every slot inherited from the scaffold told the
 * reader to run a manual `keytool -genkeypair` into `../upload.jks`, a path inside the slot that the
 * generator never uses. The owner's report, recorded 2026-10-01, is that at signing time the agent
 * had to ask him how to proceed — *"нужно донастроить завод, чтобы сам уже знал, как генерировать
 * ключи"*. A template that names a different path from the generator is exactly why the factory did
 * not know.
 *
 * So the keystore directory, the filename, the alias, the key algorithm, its size and validity, the
 * `-dname` shape and the text of the example file all live here, once. `sign-release.mjs` generates
 * from these values, `new-slot.mjs` writes a new slot's example from `keystorePropertiesExample()`,
 * and `scripts/core/signing.mjs` fails when a slot's or the scaffold's example has drifted from what
 * this module produces. The module imports nothing from `sign-release.mjs` — that would be a cycle;
 * the direction is one way, and `sign-release.mjs` imports from here.
 *
 * JDK discovery is deliberately **not** here. `scripts/lib/java.mjs` is the only place that decides
 * which Java the factory uses (`.cursor/rules/build-speed.mdc`), and `keytool` is found by its
 * caller next to that decision rather than by a second discovery routine — a duplicate one already
 * built `<jdk>\keytool.exe` instead of `<jdk>\bin\keytool.exe` and broke signing whenever
 * `JAVA_HOME` was unset.
 *
 * The honest limit, stated rather than implied: nothing here can prove a keystore exists, that its
 * password is recoverable, or that the key belongs to this app. It is the recorded *shape* of the
 * factory's signing decision, and `sign-release.mjs` is what actually mints and inspects a key —
 * under the owner's explicit authorization (`.cursor/rules/authorization.mdc`), because a signing
 * key is the app's permanent identity.
 */
import { existsSync, readFileSync } from 'node:fs'
import os from 'node:os'
import path from 'node:path'

/**
 * Where the factory keeps upload keys: outside the repository, under the operator's home.
 *
 * Deliberately outside `apps/`: a keystore is the app's permanent identity and must never be
 * committed, and `apps/**` is copied by tooling that would otherwise carry it along.
 */
export const KEYSTORE_DIR_NAME = '.playkiln'
export const KEYSTORE_SUBDIR = 'keystores'
export const KEYSTORE_DIR = path.join(os.homedir(), KEYSTORE_DIR_NAME, KEYSTORE_SUBDIR)

/**
 * The home directory, as the committed `keystore.properties.example` spells it.
 *
 * `<home>` is chosen over `~` and `${HOME}` deliberately. Those two are shell expansions that
 * Gradle does not perform: it resolves `storeFile` relative to the slot, so a copy-pasted
 * `~/.playkiln/…` would be read as a literal directory named `~` and silently point at nothing —
 * the exact "template that contradicts the generator" defect this module exists to stop, wearing a
 * plausible path's clothes. An angle-bracket token is unmistakably a value to substitute, it uses
 * the convention the generated command already uses with `<slot>`, and it keeps the committed bytes
 * identical on every machine. The real, absolute path the generator writes stays
 * `keystorePathFor()` / `storeFilePathFor()`.
 */
export const HOME_PLACEHOLDER = '<home>'

/** The keystore entry alias the factory creates and then reads back. */
export const KEY_ALIAS = 'upload'

/** Key algorithm, size and validity, in one place so the generator and the docs cannot disagree. */
export const KEY_ALGORITHM = 'RSA'
export const KEY_SIZE = 4096
export const KEY_VALIDITY = 10000

/**
 * The environment variable `keytool` reads the store and key password from.
 *
 * It is passed on the child process environment rather than on the command line so the password
 * cannot appear in a process listing; both the generation and the fingerprint read use this name.
 */
export const PASSWORD_ENVIRONMENT_NAME = 'PLAYKILN_KEYTOOL_PASSWORD'

/** The keystore file the generator would write for an applicationId. */
export function keystorePathFor(applicationId) {
  return path.join(KEYSTORE_DIR, `${applicationId}.jks`)
}

/** The X.500 name of the upload certificate the generator creates. */
export function keytoolDname(applicationId) {
  return `CN=${applicationId}, OU=Upload, O=PlayKiln, C=US`
}

/**
 * The applicationId declared by a slot's Gradle file, or null when it cannot be read.
 *
 * Returning null rather than throwing keeps this usable by the read-only core check, which must
 * report an unreadable slot honestly; `sign-release.mjs` keeps its own error messages around this.
 */
export function applicationIdFromSlot(slotDir) {
  const gradleFile = path.join(slotDir, 'app', 'build.gradle.kts')
  if (!existsSync(gradleFile)) return null
  const match = readFileSync(gradleFile, 'utf8').match(/\bapplicationId\s*=\s*["']([^"']+)["']/)
  return match ? match[1] : null
}

/** The command that would create an upload key, derived from this module's own values. */
export function printableKeytoolCommand(keytool, applicationId) {
  const quotedTool = `"${keytool}"`
  const quotedStore = `"${keystorePathFor(applicationId)}"`
  return (
    `${quotedTool} -genkeypair -v -keystore ${quotedStore} -alias ${KEY_ALIAS}` +
    ` -keyalg ${KEY_ALGORITHM} -keysize ${KEY_SIZE} -validity ${KEY_VALIDITY}` +
    ` -dname "${keytoolDname(applicationId)}"`
  )
}

/** The `storeFile` value the real, gitignored `keystore.properties` is written with. */
export function storeFilePathFor(applicationId) {
  return keystorePathFor(applicationId).replaceAll('\\', '/')
}

/**
 * The `storeFile` value the committed `keystore.properties.example` carries.
 *
 * The home directory is the placeholder rather than this machine's home, so the committed template
 * is byte-identical on every machine, in a fresh clone and in CI — the defect this function replaces
 * embedded one operator's Windows path in a file every slot inherits. The last three segments are
 * derived from `KEYSTORE_DIR`'s own names, so the template and the generator cannot disagree about
 * where a key lives, and the *shape* the core check asserts (`.playkiln/keystores/<applicationId>.jks`)
 * is exactly what the generator produces under any home.
 */
export function exampleStoreFilePathFor(applicationId) {
  return [
    HOME_PLACEHOLDER,
    KEYSTORE_DIR_NAME,
    KEYSTORE_SUBDIR,
    `${applicationId}.jks`
  ].join('/')
}

/**
 * The exact text of `keystore.properties.example` for a given applicationId.
 *
 * This is the load-bearing export: `new-slot.mjs` writes a slot's example from it, the scaffold's
 * committed file is generated from it, and `scripts/core/signing.mjs` compares both against it. The
 * `storeFile` line names the keystore the generator actually writes — the directory `.playkiln/keystores`
 * and the filename `<applicationId>.jks` are its shape, and only the home directory is a placeholder —
 * because a template pointing anywhere else is the defect this module was written for, not merely a
 * formatting drift, while an absolute home path in a committed file is a defect in the other direction
 * (it makes the file true on one machine only).
 */
export function keystorePropertiesExample(applicationId) {
  const keystorePath = exampleStoreFilePathFor(applicationId)
  return (
    [
      '# GENERATED by scripts/lib/signing.mjs — do not hand-write this file.',
      '#',
      '# A new slot receives this file written from that module, and `signing.example`',
      '# (scripts/core/signing.mjs) fails when it drifts from the module\'s own text or when the',
      '# `storeFile` below names a keystore the generator would not use. A hand-edited template that',
      '# contradicted the generator is the defect this file exists to stop.',
      '#',
      '# Never commit the real keystore.properties file or any upload-key credentials; it is gitignored.',
      '#',
      `# The factory generates and owns the upload key for ${applicationId}. There is no manual keytool`,
      '# step. Generating a key mints the app\'s permanent identity together with the password that',
      '# unlocks it, so it needs the owner\'s explicit authorization in his own words —',
      '# `.cursor/rules/authorization.mdc` — and the procedure is the runbook `docs/signing.md`.',
      '#',
      '#   node scripts/sign-release.mjs --slot <slot> --generate',
      '#',
      '# The key is written outside this repository, to the factory\'s own keystore directory.',
      '# `<home>` is the operator\'s home directory: this committed template stays machine-independent,',
      '# and `sign-release.mjs` writes the real absolute path into the gitignored keystore.properties.',
      `#   ${keystorePath}`,
      '#',
      '# Play App Signing keeps the real app-signing key at Google. If the upload key is lost or',
      '# compromised, request an upload-key reset through Play Console.',
      '',
      `# storeFile=${keystorePath}`,
      '# storePassword=',
      `# keyAlias=${KEY_ALIAS}`,
      '# keyPassword='
    ].join('\n') + '\n'
  )
}

/**
 * The `storeFile` path a template declares, or null when it declares none.
 *
 * Commented and uncommented forms are both read: the example carries the line commented so a human
 * can see the location without it being active, and the real `keystore.properties` (gitignored)
 * carries it live. The example spells the home directory as `HOME_PLACEHOLDER`; the real file
 * spells the operator's actual home.
 */
export function storeFileLineValue(text) {
  for (const rawLine of String(text).split(/\r?\n/)) {
    const match = rawLine.match(/^\s*#?\s*storeFile\s*=\s*(.+?)\s*$/)
    if (match) return match[1]
  }
  return null
}

/**
 * Why a template's `storeFile` does not name this applicationId's keystore, or null when it does.
 *
 * This is the path half of the contract, kept separate from the whole-text comparison so the two
 * can be read and neutered independently: the text comparison catches formatting drift, and this
 * one catches the actual defect — a template pointing at a keystore the generator would not use.
 *
 * It asserts the path's **shape**, not its literal bytes, because the committed example carries
 * `HOME_PLACEHOLDER` where the real file carries the operator's home. The shape is machine
 * independent on purpose: the last directory is `KEYSTORE_SUBDIR` (`keystores`), the one before it
 * is `KEYSTORE_DIR_NAME` (`.playkiln`), and the filename is exactly `<applicationId>.jks`. That is
 * strictly stronger than comparing against one machine's absolute path — a template pointing at
 * `../upload.jks`, at another applicationId's key, or at `~/.playkiln/keys/…` is named where the old
 * literal comparison would have matched nothing and said only "some formatting drifted".
 */
export function storeFileProblem(text, applicationId) {
  const declared = storeFileLineValue(text)
  if (declared === null) return 'carries no storeFile line'
  const segments = String(declared)
    .replaceAll('\\', '/')
    .replace(/^\/+/, '')
    .split('/')
    .filter((segment) => segment.length > 0)
  const filename = segments.at(-1) ?? ''
  const parent = segments.at(-2) ?? ''
  const grandparent = segments.at(-3) ?? ''
  const expectedFilename = `${applicationId}.jks`
  if (filename !== expectedFilename) {
    return `storeFile is ${declared}, but the filename must be exactly ${expectedFilename}`
  }
  if (parent !== KEYSTORE_SUBDIR || grandparent !== KEYSTORE_DIR_NAME) {
    return `storeFile is ${declared}, but its directory must be ${KEYSTORE_DIR_NAME}/${KEYSTORE_SUBDIR}`
  }
  return null
}
