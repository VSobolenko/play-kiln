// The factory's dependency-freshness contract: which coordinates Play has flagged, and the version
// each must be at or above.
//
// `play-dependency-floors.json` is the one source of truth. Two checks read it and neither restates
// it: `scripts/core/deps.mjs` uses it for the source-level constraint check (`build.fragmentConstraint`)
// and for the artifact-level check (`deps.artifact`), which is the half that would have caught the
// 2026-10-08 Play notice on `2026-10-02-laneset`. A floor written in two places drifts, and the drift
// is invisible until Play sends the notice again — which is the whole defect this file was written
// from.
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = path.dirname(fileURLToPath(import.meta.url))

/** The floors file, derived from this module's own location — never from a re-derived repo root. */
export const FLOORS_PATH = path.join(HERE, 'play-dependency-floors.json')

/** Compare dotted numeric versions; returns negative when a < b. Non-numeric tails sort lower. */
export function compareVersions(a, b) {
  const parts = (v) => String(v).split(/[.+-]/).map((p) => (/^\d+$/.test(p) ? Number(p) : p))
  const aa = parts(a)
  const bb = parts(b)
  const n = Math.max(aa.length, bb.length)
  for (let i = 0; i < n; i++) {
    const x = aa[i] ?? 0
    const y = bb[i] ?? 0
    if (typeof x === 'number' && typeof y === 'number') {
      if (x !== y) return x - y
    } else {
      const sx = String(x)
      const sy = String(y)
      if (sx !== sy) return sx < sy ? -1 : 1
    }
  }
  return 0
}

/**
 * Parse and validate the floors document. Returns `{ floors, problems }` — `problems` is non-empty
 * when the document itself is malformed, which is a failure of the contract rather than of a slot,
 * and is reported as one so a typo in a floor cannot silently disable the gate.
 */
export function parseFloors(text, rel = FLOORS_PATH) {
  const problems = []
  let json
  try {
    json = JSON.parse(text)
  } catch (err) {
    return { floors: [], problems: [`${rel}: not valid JSON (${err.message})`] }
  }
  if (!json || typeof json !== 'object' || !Array.isArray(json.floors)) {
    return { floors: [], problems: [`${rel}: expected an object with a "floors" array`] }
  }
  const floors = []
  json.floors.forEach((entry, i) => {
    const at = `${rel} floors[${i}]`
    if (!entry || typeof entry !== 'object') {
      problems.push(`${at}: not an object`)
      return
    }
    if (typeof entry.coordinate !== 'string' || !/^[^:\s]+:[^:\s]+$/.test(entry.coordinate)) {
      problems.push(`${at}: coordinate must be "group:name", got ${JSON.stringify(entry.coordinate)}`)
      return
    }
    if (typeof entry.floor !== 'string' || !/^\d/.test(entry.floor)) {
      problems.push(`${at}: floor must be a version starting with a digit, got ${JSON.stringify(entry.floor)}`)
      return
    }
    if (typeof entry.why !== 'string' || entry.why.trim().length < 10) {
      problems.push(`${at}: needs a "why" — a floor with no recorded reason is a floor nobody can audit`)
      return
    }
    floors.push(entry)
  })
  return { floors, problems }
}

/** The floors document as read from disk. Throws rather than returning empty, so a missing or
 *  malformed contract can never be read as "no coordinate is outdated". */
export function loadFloors(file = FLOORS_PATH) {
  const { floors, problems } = parseFloors(readFileSync(file, 'utf8'), file)
  if (problems.length > 0) throw new Error(problems.join('; '))
  if (floors.length === 0) throw new Error(`${file}: no floors declared`)
  return floors
}

const FLOORS = loadFloors()

/** The declared floor for one `group:name`, or null when the coordinate is not listed. */
export function floorFor(coordinate) {
  const hit = FLOORS.find((f) => f.coordinate === coordinate)
  return hit ? hit.floor : null
}

/**
 * Every coordinate in `coordinates` that is below its declared floor.
 *
 * `coordinates` is `[{ group, name, version }]` as produced by `aab-metadata.mjs`; a coordinate the
 * contract does not list is left alone, because inventing a floor for a library nobody flagged is
 * how a gate starts failing correct builds.
 */
export function outdatedCoordinates(coordinates, floors = FLOORS) {
  const out = []
  for (const coord of coordinates) {
    const coordinate = `${coord.group}:${coord.name}`
    const entry = floors.find((f) => f.coordinate === coordinate)
    if (!entry) continue
    if (compareVersions(coord.version, entry.floor) < 0) {
      out.push({ coordinate, version: coord.version, floor: entry.floor, why: entry.why })
    }
  }
  return out
}
