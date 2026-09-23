import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

export class CapabilityConfigError extends Error {
  constructor(message) {
    super(message)
    this.name = 'CapabilityConfigError'
  }
}

function readJson(file, label) {
  let value
  try {
    value = JSON.parse(readFileSync(file, 'utf8'))
  } catch (error) {
    throw new CapabilityConfigError(`${label} is not valid JSON: ${error.message}`)
  }
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new CapabilityConfigError(`${label} must contain a JSON object`)
  }
  return value
}

export function discoverCapabilities(rootDir) {
  const directory = path.join(rootDir, 'capabilities')
  if (!existsSync(directory)) {
    throw new CapabilityConfigError(`capability registry directory is missing: ${directory}`)
  }
  const manifests = new Map()
  for (const name of readdirSync(directory).sort()) {
    const moduleDir = path.join(directory, name)
    if (!statSync(moduleDir).isDirectory()) continue
    const manifestFile = path.join(moduleDir, 'capability.json')
    if (!existsSync(manifestFile)) continue
    const manifest = readJson(manifestFile, `capabilities/${name}/capability.json`)
    if (manifest.id !== name) {
      throw new CapabilityConfigError(
        `capabilities/${name}/capability.json id must be ${name} (found ${manifest.id ?? 'missing'})`
      )
    }
    manifests.set(name, manifest)
  }
  if (manifests.size === 0) throw new CapabilityConfigError('no capability manifests were found')
  return manifests
}

function validateSlotConfig(config, validIds, label) {
  if (config.schemaVersion !== 1) {
    throw new CapabilityConfigError(`${label}.schemaVersion must be 1`)
  }
  if (!Array.isArray(config.enabled) || !config.enabled.every((id) => typeof id === 'string')) {
    throw new CapabilityConfigError(`${label}.enabled must be an array of capability ids`)
  }
  const duplicates = config.enabled.filter((id, index) => config.enabled.indexOf(id) !== index)
  if (duplicates.length > 0) {
    throw new CapabilityConfigError(`${label}.enabled contains duplicate ids: ${[...new Set(duplicates)].join(', ')}`)
  }
  const unknown = config.enabled.filter((id) => !validIds.includes(id))
  if (unknown.length > 0) {
    throw new CapabilityConfigError(
      `unknown capability id(s): ${unknown.join(', ')}; valid ids: ${validIds.join(', ')}`
    )
  }

  if (config.billing !== undefined) {
    if (!config.billing || typeof config.billing !== 'object' || Array.isArray(config.billing)) {
      throw new CapabilityConfigError(`${label}.billing must be an object`)
    }
    if (!config.enabled.includes('billing')) {
      throw new CapabilityConfigError(`${label}.billing is configured but billing is not enabled`)
    }
    const consumables = config.billing.consumables ?? []
    if (!Array.isArray(consumables)) {
      throw new CapabilityConfigError(`${label}.billing.consumables must be an array`)
    }
    for (const [index, pack] of consumables.entries()) {
      if (!pack || typeof pack !== 'object' || Array.isArray(pack)) {
        throw new CapabilityConfigError(`${label}.billing.consumables[${index}] must be an object`)
      }
      if (typeof pack.id !== 'string' || !pack.id.trim()) {
        throw new CapabilityConfigError(`${label}.billing.consumables[${index}].id must be a non-empty string`)
      }
      if (!['LOCAL', 'SERVER_COST'].includes(pack.fulfillment)) {
        throw new CapabilityConfigError(
          `${label}.billing.consumables[${index}].fulfillment must be LOCAL or SERVER_COST`
        )
      }
    }
    if (
      config.billing.purchaseAuthority !== undefined &&
      (typeof config.billing.purchaseAuthority !== 'string' || !config.billing.purchaseAuthority.trim())
    ) {
      throw new CapabilityConfigError(`${label}.billing.purchaseAuthority must be a non-empty string`)
    }
  }
}

function uniqueObjects(items, keyOf) {
  const unique = new Map()
  for (const item of items) unique.set(keyOf(item), item)
  return [...unique.values()]
}

export function aggregateSlotCapabilities({ rootDir, slotDir }) {
  const manifests = discoverCapabilities(rootDir)
  const validIds = [...manifests.keys()].sort()
  const configFile = path.join(slotDir, 'capabilities.json')
  if (!existsSync(configFile)) {
    throw new CapabilityConfigError(
      `missing ${path.relative(rootDir, configFile)}; declare enabled capabilities (use an empty array when none are enabled)`
    )
  }
  const config = readJson(configFile, path.relative(rootDir, configFile))
  validateSlotConfig(config, validIds, path.relative(rootDir, configFile))

  const selected = config.enabled.map((id) => manifests.get(id))
  const permissions = uniqueObjects(
    selected.flatMap((manifest) => manifest.permissions ?? []),
    (permission) => permission.name
  ).sort((left, right) => left.name.localeCompare(right.name))
  const dataCollected = uniqueObjects(
    selected.flatMap((manifest) => manifest.dataCollected ?? []),
    (item) => `${item.type}\u0000${item.purpose}\u0000${item.shared}\u0000${item.optional}`
  ).sort((left, right) =>
    left.type.localeCompare(right.type) || left.purpose.localeCompare(right.purpose)
  )
  const sdkDependencies = uniqueObjects(
    selected.flatMap((manifest) => manifest.sdkDependencies ?? []),
    (sdk) => `${sdk.name}\u0000${sdk.version}`
  ).sort((left, right) => left.name.localeCompare(right.name))
  const requiresPurchaseAuthority = Boolean(
    config.billing?.consumables?.some((pack) => pack.fulfillment === 'SERVER_COST')
  )

  return {
    schemaVersion: 1,
    enabled: [...config.enabled].sort(),
    permissions,
    dataCollected,
    sdkDependencies,
    adsPresent: selected.some((manifest) => manifest.adsPresent === true),
    requiresConsent: selected.some((manifest) => manifest.requiresConsent === true),
    requiresPurchaseAuthority,
    purchaseAuthority: config.billing?.purchaseAuthority?.trim() || null
  }
}

const invokedFile = process.argv[1] ? path.resolve(process.argv[1]) : null
if (invokedFile === fileURLToPath(import.meta.url)) {
  const args = process.argv.slice(2)
  const rootIndex = args.indexOf('--root')
  const slotIndex = args.indexOf('--slot')
  const rootDir = rootIndex >= 0 ? path.resolve(args[rootIndex + 1] ?? '') : process.cwd()
  const slotDir = slotIndex >= 0 ? path.resolve(args[slotIndex + 1] ?? '') : null
  try {
    if (!slotDir) throw new CapabilityConfigError('usage: capabilities.mjs --root <repo> --slot <slot-dir>')
    process.stdout.write(`${JSON.stringify(aggregateSlotCapabilities({ rootDir, slotDir }))}\n`)
  } catch (error) {
    process.stderr.write(`FAIL: ${error.message}\n`)
    process.exitCode = 1
  }
}
