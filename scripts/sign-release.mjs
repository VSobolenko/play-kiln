#!/usr/bin/env node

import { randomBytes } from 'node:crypto'
import { spawnSync } from 'node:child_process'
import {
  appendFileSync,
  existsSync,
  mkdirSync,
  readFileSync,
  readdirSync,
  statSync,
  writeFileSync
} from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import process from 'node:process'
import { fileURLToPath, pathToFileURL } from 'node:url'

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

const defaultLog = (status, message) => console.log(`  ${status.padEnd(11)} ${message}`)

class SigningError extends Error {
  constructor(message, exitCode = 2) {
    super(message)
    this.exitCode = exitCode
  }
}

export function ensureSigning(slotDir, { generate = false, log = defaultLog } = {}) {
  const resolvedSlot = path.resolve(slotDir)
  const propertiesFile = path.join(resolvedSlot, 'keystore.properties')
  const applicationId = readApplicationId(resolvedSlot)
  const keytool = findKeytool()

  if (existsSync(propertiesFile)) {
    const properties = readProperties(propertiesFile)
    const alias = properties.keyAlias
    const storePassword = properties.storePassword
    const storeFile = resolveStoreFile(resolvedSlot, properties.storeFile)
    if (!alias || !storePassword || !properties.keyPassword || !storeFile) {
      log('BLOCKED', 'keystore.properties must define storeFile, storePassword, keyAlias, and keyPassword')
      throw new SigningError('Incomplete keystore.properties')
    }
    if (!existsSync(storeFile)) {
      log('BLOCKED', `keystore file not found: ${storeFile}`)
      throw new SigningError('Keystore file is missing')
    }
    if (!keytool) {
      log('BLOCKED', 'keytool not found; set JAVA_HOME or add a JDK bin directory to PATH')
      throw new SigningError('keytool is required to verify signing')
    }

    const fingerprint = readFingerprintOrBlock(keytool, storeFile, alias, storePassword, log)
    log('PASS', `signing alias: ${alias}`)
    log('PASS', `upload certificate SHA-256: ${fingerprint}`)
    return { alias, applicationId, fingerprint, keystorePath: storeFile, propertiesFile }
  }

  const keystorePath = path.join(os.homedir(), '.playkiln', 'keystores', `${applicationId}.jks`)
  const command = printableKeytoolCommand(keytool || 'keytool', keystorePath, applicationId)

  if (!generate) {
    log('BLOCKED', 'keystore.properties is missing; generate the upload key explicitly or run:')
    log('BLOCKED', command)
    throw new SigningError('Signing configuration is missing')
  }
  if (!keytool) {
    log('BLOCKED', 'keytool not found; install a JDK, set JAVA_HOME, then run:')
    log('BLOCKED', command)
    throw new SigningError('keytool is required to generate signing credentials')
  }

  if (isInsideGitRepository(keystorePath)) {
    log('WARN', `keystore destination is inside a Git repository: ${keystorePath}`)
  }

  mkdirSync(path.dirname(keystorePath), { recursive: true, mode: 0o700 })
  if (existsSync(keystorePath)) {
    log('BLOCKED', `refusing to overwrite existing keystore: ${keystorePath}`)
    throw new SigningError('Keystore already exists without a matching properties file')
  }

  const password = randomBytes(24).toString('base64url').slice(0, 32)
  const alias = 'upload'
  const passwordEnvironmentName = 'PLAYKILN_KEYTOOL_PASSWORD'
  ensureGitIgnored(resolvedSlot, 'keystore.properties')
  const result = spawnSync(
    keytool,
    [
      '-genkeypair',
      '-v',
      '-keystore',
      keystorePath,
      '-storepass:env',
      passwordEnvironmentName,
      '-keypass:env',
      passwordEnvironmentName,
      '-alias',
      alias,
      '-keyalg',
      'RSA',
      '-keysize',
      '4096',
      '-validity',
      '10000',
      '-dname',
      `CN=${applicationId}, OU=Upload, O=PlayKiln, C=US`,
      '-J-Duser.language=en',
      '-J-Duser.country=US'
    ],
    {
      cwd: resolvedSlot,
      encoding: 'utf8',
      env: { ...process.env, [passwordEnvironmentName]: password },
      shell: false,
      windowsHide: true
    }
  )
  if (result.error || result.status !== 0) {
    log('BLOCKED', `keytool generation failed: ${safeToolError(result)}`)
    throw new SigningError('Could not generate upload keystore')
  }

  const content = [
    `storeFile=${keystorePath.replaceAll('\\', '/')}`,
    `storePassword=${password}`,
    `keyAlias=${alias}`,
    `keyPassword=${password}`,
    ''
  ].join('\n')
  writeFileSync(propertiesFile, content, { encoding: 'utf8', mode: 0o600, flag: 'wx' })

  const fingerprint = readFingerprintOrBlock(keytool, keystorePath, alias, password, log)
  log('PASS', `generated upload keystore outside repository: ${keystorePath}`)
  log('PASS', `signing alias: ${alias}`)
  log('PASS', `upload certificate SHA-256: ${fingerprint}`)
  return { alias, applicationId, fingerprint, keystorePath, propertiesFile }
}

function readApplicationId(slotDir) {
  const gradleFile = path.join(slotDir, 'app', 'build.gradle.kts')
  if (!existsSync(gradleFile)) throw new SigningError(`Missing ${gradleFile}`, 1)
  const text = readFileSync(gradleFile, 'utf8')
  const match = text.match(/\bapplicationId\s*=\s*["']([^"']+)["']/)
  if (!match) throw new SigningError('applicationId not found in app/build.gradle.kts', 1)
  return match[1]
}

function readProperties(file) {
  const result = {}
  for (const rawLine of readFileSync(file, 'utf8').replace(/^\uFEFF/, '').split(/\r?\n/)) {
    const line = rawLine.trim()
    if (!line || line.startsWith('#') || line.startsWith('!')) continue
    const separator = line.search(/(?<!\\)[=:]/)
    if (separator === -1) continue
    const key = line.slice(0, separator).trim()
    const value = line.slice(separator + 1).trim().replaceAll('\\:', ':').replaceAll('\\\\', '\\')
    result[key] = value
  }
  return result
}

function resolveStoreFile(slotDir, configuredPath) {
  if (!configuredPath) return null
  return path.resolve(slotDir, configuredPath)
}

function readFingerprint(keytool, keystoreFile, alias, password) {
  const passwordEnvironmentName = 'PLAYKILN_KEYTOOL_PASSWORD'
  const result = spawnSync(
    keytool,
    [
      '-list',
      '-v',
      '-keystore',
      keystoreFile,
      '-alias',
      alias,
      '-storepass:env',
      passwordEnvironmentName,
      '-J-Duser.language=en',
      '-J-Duser.country=US'
    ],
    {
      encoding: 'utf8',
      env: { ...process.env, [passwordEnvironmentName]: password },
      shell: false,
      windowsHide: true
    }
  )
  if (result.error || result.status !== 0) {
    throw new SigningError(`keytool could not inspect the upload certificate: ${safeToolError(result)}`)
  }
  const output = `${result.stdout || ''}\n${result.stderr || ''}`
  const match = output.match(/SHA-?256:\s*([0-9A-F]{2}(?::[0-9A-F]{2})+)/i)
  if (!match) throw new SigningError('keytool output did not contain a SHA-256 fingerprint')
  return match[1].toUpperCase()
}

function readFingerprintOrBlock(keytool, keystoreFile, alias, password, log) {
  try {
    return readFingerprint(keytool, keystoreFile, alias, password)
  } catch (error) {
    log('BLOCKED', error.message)
    throw error
  }
}

function safeToolError(result) {
  if (result.error) return result.error.message
  const output = `${result.stderr || ''}\n${result.stdout || ''}`.trim()
  return output.split(/\r?\n/).filter(Boolean).slice(-3).join(' | ') || `exit ${result.status}`
}

function findKeytool() {
  const executable = process.platform === 'win32' ? 'keytool.exe' : 'keytool'
  const candidates = []
  if (process.env.JAVA_HOME) {
    candidates.push(path.join(process.env.JAVA_HOME.replace(/^"|"$/g, ''), 'bin', executable))
  }
  for (const entry of (process.env.PATH || '').split(path.delimiter).filter(Boolean)) {
    candidates.push(path.join(entry.replace(/^"|"$/g, ''), executable))
  }

  if (process.platform === 'win32') {
    const programFiles = [process.env.ProgramFiles, process.env['ProgramFiles(x86)']].filter(Boolean)
    for (const root of programFiles) {
      candidates.push(path.join(root, 'Android', 'Android Studio', 'jbr', 'bin', executable))
      addJdkChildren(candidates, path.join(root, 'Java'), executable)
      addJdkChildren(candidates, path.join(root, 'Eclipse Adoptium'), executable)
    }
  } else if (process.platform === 'darwin') {
    addJdkChildren(candidates, '/Library/Java/JavaVirtualMachines', path.join('Contents', 'Home', 'bin', executable))
  } else {
    addJdkChildren(candidates, '/usr/lib/jvm', path.join('bin', executable))
  }

  return candidates.find((candidate) => isFile(candidate)) || null
}

function addJdkChildren(candidates, root, executableRelativePath) {
  if (!existsSync(root)) return
  try {
    for (const name of readdirSync(root)) candidates.push(path.join(root, name, executableRelativePath))
  } catch {
    // An unreadable optional JDK search location is equivalent to no candidate there.
  }
}

function isFile(file) {
  try {
    return statSync(file).isFile()
  } catch {
    return false
  }
}

function isInsideGitRepository(target) {
  let current = path.resolve(path.dirname(target))
  while (true) {
    if (existsSync(path.join(current, '.git'))) return true
    const parent = path.dirname(current)
    if (parent === current) return false
    current = parent
  }
}

function ensureGitIgnored(slotDir, entry) {
  const ignoreFile = path.join(slotDir, '.gitignore')
  const existing = existsSync(ignoreFile) ? readFileSync(ignoreFile, 'utf8') : ''
  let ignored = false
  for (const rawLine of existing.split(/\r?\n/)) {
    const line = rawLine.trim()
    if (line === entry || line === `/${entry}`) ignored = true
    if (line === `!${entry}` || line === `!/${entry}`) ignored = false
  }
  if (ignored) return
  const prefix = existing.length > 0 && !existing.endsWith('\n') ? '\n' : ''
  appendFileSync(ignoreFile, `${prefix}${entry}\n`, 'utf8')
}

function printableKeytoolCommand(keytool, keystorePath, applicationId) {
  const quotedTool = `"${keytool}"`
  const quotedStore = `"${keystorePath}"`
  return `${quotedTool} -genkeypair -v -keystore ${quotedStore} -alias upload -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=${applicationId}, OU=Upload, O=PlayKiln, C=US"`
}

function listAppDirs() {
  const appsDir = path.join(ROOT, 'apps')
  if (!existsSync(appsDir)) return []
  return readdirSync(appsDir)
    .map((name) => path.join(appsDir, name))
    .filter((candidate) => {
      try {
        return statSync(candidate).isDirectory()
      } catch {
        return false
      }
    })
}

function resolveSlotDir(slug) {
  const matches = listAppDirs().filter((candidate) => {
    const name = path.basename(candidate)
    return name === slug || name.endsWith(`-${slug}`)
  })
  if (matches.length === 0) throw new SigningError(`Slot not found under apps/: ${slug}`, 1)
  if (matches.length > 1) throw new SigningError(`Slot is ambiguous (${matches.map((item) => path.basename(item)).join(', ')}): ${slug}`, 1)
  return matches[0]
}

function cliArgument(name) {
  const index = process.argv.indexOf(name)
  return index === -1 ? null : process.argv[index + 1] || null
}

function runCli() {
  const slug = cliArgument('--slot')
  if (!slug) {
    console.error('Usage: node scripts/sign-release.mjs --slot <slug> [--generate]')
    process.exitCode = 1
    return
  }
  try {
    const slotDir = resolveSlotDir(slug)
    console.log(`## Signing ${path.basename(slotDir)}\n`)
    ensureSigning(slotDir, { generate: process.argv.includes('--generate') })
    console.log('\nVERDICT: PASS')
  } catch (error) {
    if (!(error instanceof SigningError)) defaultLog('BLOCKED', error.message)
    console.log('\nVERDICT: BLOCKED')
    process.exitCode = error.exitCode || 2
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) runCli()
