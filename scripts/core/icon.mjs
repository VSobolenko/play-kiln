// Adaptive-icon geometry: does the shipped launcher mark survive a launcher mask?
//
// Ported from scripts/check-adaptive-icon.py, which stays in place until its ledger rows (§5.11 and
// §5.13-5.15) are proven honoured. Two properties are load-bearing and are carried over exactly:
//
//   1. The path data is read out of the SHIPPED foreground resource, never retyped, so this check
//      cannot pass on a stale copy of the geometry.
//   2. Relative path commands are resolved against the current point. Treating them as absolute
//      was the original defect: it returned a plausible point list measured from the origin, so a
//      relative-path generator change kept the check reporting PASS while the shipped mark was
//      clipped.
//
// The subset is straight lines and circular arcs, matching what the generator emits. An arc whose
// rx != ry is REFUSED rather than measured as if it were circular: a silently wrong reach is worse
// than a refused check.
import { existsSync, readFileSync, readdirSync } from 'node:fs'
import path from 'node:path'
import { expectTrue, fail, guard, pass, playAssets, unverified } from './lib.mjs'

const TOKEN = /([MmLlHhVvAaZz])|(-?\d*\.?\d+)/g
const ARC_SAMPLES = 64
const ARC_RADIUS_TOLERANCE = 1e-6
const ANDROID_DRAWABLE = 'android:drawable'
const ANDROID_PATH_DATA = /android:pathData="([^"]+)"/g

/** Flattens the supported path subset into sampled points. Throws on a construct it cannot measure. */
export function pointsFrom(pathData, samples = ARC_SAMPLES) {
  const tokens = []
  for (const match of pathData.matchAll(TOKEN)) {
    tokens.push(match[1] ? match[1] : Number(match[2]))
  }
  const points = []
  let x = 0
  let y = 0
  let start = [0, 0]
  let command = ''
  let i = 0

  while (i < tokens.length) {
    const token = tokens[i]
    if (typeof token === 'string') {
      command = token
      i += 1
      if (command === 'Z' || command === 'z') {
        ;[x, y] = start
      }
      continue
    }
    const relative = command === command.toLowerCase()
    const kind = command.toUpperCase()

    if (kind === 'M') {
      const nx = tokens[i]
      const ny = tokens[i + 1]
      ;[x, y] = relative ? [x + nx, y + ny] : [nx, ny]
      start = [x, y]
      points.push([x, y])
      i += 2
      // Extra coordinate pairs after a moveto are implicit linetos, in the same command's case.
      command = relative ? 'l' : 'L'
      continue
    }
    if (kind === 'L') {
      const nx = tokens[i]
      const ny = tokens[i + 1]
      ;[x, y] = relative ? [x + nx, y + ny] : [nx, ny]
      points.push([x, y])
      i += 2
      continue
    }
    if (kind === 'H') {
      const nx = tokens[i]
      x = relative ? x + nx : nx
      points.push([x, y])
      i += 1
      continue
    }
    if (kind === 'V') {
      const ny = tokens[i]
      y = relative ? y + ny : ny
      points.push([x, y])
      i += 1
      continue
    }
    if (kind === 'A') {
      const rx = tokens[i]
      const ry = tokens[i + 1]
      const large = tokens[i + 3]
      const sweep = tokens[i + 4]
      const ax = tokens[i + 5]
      const ay = tokens[i + 6]
      i += 7
      if (Math.abs(rx - ry) > ARC_RADIUS_TOLERANCE) {
        throw new Error(
          `elliptical arc (rx=${rx}, ry=${ry}) is outside this checker's subset: it treats ry and ` +
            'x-axis-rotation as ignored, so it cannot measure this arc. Refusing to report a reach ' +
            'rather than reporting a wrong one.'
        )
      }
      const [x2, y2] = relative ? [x + ax, y + ay] : [ax, ay]
      points.push(...arcPoints(x, y, x2, y2, rx, large, sweep, samples))
      x = x2
      y = y2
      continue
    }
    // An unknown command token with numeric operands: skip the number.
    i += 1
  }
  return points
}

export function arcPoints(x1, y1, x2, y2, radius, large, sweep, samples = ARC_SAMPLES) {
  const mx = (x1 + x2) / 2
  const my = (y1 + y2) / 2
  const dxr = (x1 - x2) / 2
  const dyr = (y1 - y2) / 2
  const chord = Math.hypot(dxr, dyr)
  let h = Math.sqrt(Math.max(0, radius * radius - chord * chord))
  if (large === sweep) h = -h
  const cx = chord ? mx + (h * dyr) / chord : mx
  const cy = chord ? my - (h * dxr) / chord : my
  let a1 = Math.atan2(y1 - cy, x1 - cx)
  let a2 = Math.atan2(y2 - cy, x2 - cx)
  if (sweep === 1 && a2 < a1) a2 += 2 * Math.PI
  if (sweep === 0 && a2 > a1) a2 -= 2 * Math.PI
  const out = []
  for (let step = 1; step <= samples; step += 1) {
    const angle = a1 + ((a2 - a1) * step) / samples
    out.push([cx + radius * Math.cos(angle), cy + radius * Math.sin(angle)])
  }
  return out
}

export function furthestReach(points, centre) {
  const [cx, cy] = centre
  let reach = 0
  for (const [px, py] of points) reach = Math.max(reach, Math.hypot(px - cx, py - cy))
  return reach
}

/** Resolves the shipped adaptive-icon foreground, refusing an ambiguous resource selection. */
export function resolveForeground(slotDir) {
  const res = path.join(slotDir, 'app', 'src', 'main', 'res')
  if (!existsSync(res)) throw new Error('no app/src/main/res')
  const launchers = []
  for (const dir of readdirSync(res, { withFileTypes: true })) {
    if (!dir.isDirectory() || !dir.name.startsWith('mipmap')) continue
    for (const name of ['ic_launcher.xml', 'ic_launcher_round.xml']) {
      const file = path.join(res, dir.name, name)
      if (existsSync(file)) launchers.push(file)
    }
  }
  if (launchers.length === 0) throw new Error('no adaptive ic_launcher XML found under res/')

  const references = new Map()
  for (const launcher of launchers) {
    const text = readFileSync(launcher, 'utf8')
    // The foreground element is the one carrying an android:drawable attribute.
    const match = /<foreground[^>]*android:drawable="([^"]+)"/.exec(text)
    if (!match) throw new Error(`${path.basename(launcher)} has no adaptive-icon foreground reference`)
    references.set(launcher, match[1])
  }
  const distinct = [...new Set(references.values())]
  if (distinct.length !== 1) {
    throw new Error(`adaptive-icon foreground references disagree: ${[...references.values()].join(', ')}`)
  }
  const reference = distinct[0]
  const parsed = /^@(drawable|mipmap)\/([A-Za-z0-9_]+)$/.exec(reference)
  if (!parsed) {
    throw new Error(`unsupported adaptive-icon foreground reference ${reference}; expected a local @drawable or @mipmap XML resource`)
  }
  const candidates = []
  for (const dir of readdirSync(res, { withFileTypes: true })) {
    if (!dir.isDirectory() || !dir.name.startsWith(parsed[1])) continue
    const file = path.join(res, dir.name, `${parsed[2]}.xml`)
    if (existsSync(file)) candidates.push(file)
  }
  if (candidates.length !== 1) {
    throw new Error(`${reference} resolved to ${candidates.length} files: ${candidates.map((c) => path.basename(c)).join(', ')}`)
  }
  return candidates[0]
}

export function iconFixtures() {
  return guard('icon.fixtures', () => {
    const cases = []
    const near = (a, b) => Math.abs(a - b) < 1e-9

    cases.push([
      'relative and absolute forms measure the same reach',
      () => {
        const centre = [54, 54]
        const absolute = furthestReach(pointsFrom('M50,50L60,50L60,60'), centre)
        const relative = furthestReach(pointsFrom('M50,50l10,0l0,10'), centre)
        expectTrue(near(absolute, relative), `absolute ${absolute} vs relative ${relative}`)
        expectTrue(near(absolute, Math.hypot(6, 6)), `absolute ${absolute} should be the corner distance`)
      }
    ])
    cases.push([
      'repeated moveto pairs are implicit linetos',
      () => {
        const points = pointsFrom('M0,0 10,10 20,20')
        expectTrue(points.length === 3 && points[2][0] === 20 && points[2][1] === 20, JSON.stringify(points))
      }
    ])
    cases.push([
      'repeated relative linetos accumulate',
      () => {
        const points = pointsFrom('M0,0l1,2 3,4')
        expectTrue(points.length === 3 && points[2][0] === 4 && points[2][1] === 6, JSON.stringify(points))
      }
    ])
    cases.push([
      'h and v are relative when lowercase',
      () => {
        const points = pointsFrom('M10,10h5v5')
        expectTrue(points.length === 3 && points[1][0] === 15 && points[2][1] === 15, JSON.stringify(points))
      }
    ])
    cases.push([
      'an elliptical arc is refused rather than measured as circular',
      () => {
        let threw = false
        try {
          pointsFrom('M0,0a10,5 0 0,1 10,0')
        } catch {
          threw = true
        }
        expectTrue(threw, 'rx != ry must throw')
      }
    ])
    cases.push([
      'a circular arc is measured and stays inside its radius',
      () => {
        const points = pointsFrom('M0,0a10,10 0 0,1 20,0')
        const reach = furthestReach(points, [10, 0])
        expectTrue(Math.abs(reach - 10) < 1e-6, `reach ${reach} should be the arc radius`)
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
      ? pass('icon.fixtures', `${cases.length} fixtures`)
      : fail('icon.fixtures', failures.join(' | '))
  })
}

export function checkAdaptiveIcon(slotDir) {
  return guard('slot.adaptiveIcon', () => {
    const geometry = playAssets().iconGeometry
    if (!existsSync(path.join(slotDir, 'app', 'src', 'main', 'res'))) {
      return unverified('slot.adaptiveIcon', 'no app/src/main/res to inspect')
    }
    let foreground
    try {
      foreground = resolveForeground(slotDir)
    } catch (error) {
      return fail('slot.adaptiveIcon', error.message)
    }
    const text = readFileSync(foreground, 'utf8')
    const pathData = [...text.matchAll(ANDROID_PATH_DATA)].map((match) => match[1])
    if (pathData.length === 0) {
      return fail('slot.adaptiveIcon', `${path.basename(foreground)} declares no android:pathData`)
    }
    let points
    try {
      points = pathData.flatMap((data) => pointsFrom(data))
    } catch (error) {
      return fail('slot.adaptiveIcon', error.message)
    }
    if (points.length === 0) {
      return unverified('slot.adaptiveIcon', `${path.basename(foreground)} declares paths but none sampled`)
    }
    const half = geometry.adaptiveViewportDp / 2
    const centre = [half, half]
    const reach = furthestReach(points, centre)
    const visible = geometry.visibleDp / 2
    const safe = geometry.safeRadiusDp

    const footprint = points.reduce(
      (acc, [px, py]) => ({
        minX: Math.min(acc.minX, px),
        maxX: Math.max(acc.maxX, px),
        minY: Math.min(acc.minY, py),
        maxY: Math.max(acc.maxY, py)
      }),
      { minX: Infinity, maxX: -Infinity, minY: Infinity, maxY: -Infinity }
    )
    const box = `x=[${footprint.minX.toFixed(2)}, ${footprint.maxX.toFixed(2)}] y=[${footprint.minY.toFixed(2)}, ${footprint.maxY.toFixed(2)}]`

    if (reach > safe) {
      return fail(
        'slot.adaptiveIcon',
        `reach ${reach.toFixed(2)}dp is outside the ${safe}dp safe zone ` +
          `(visible mask radius ${visible}dp); a launcher mask will crop the mark. ${box}`
      )
    }
    return pass(
      'slot.adaptiveIcon',
      `reach ${reach.toFixed(2)}dp inside the ${safe}dp safe zone, ${(safe - reach).toFixed(2)}dp of margin. ${box}`
    )
  })
}
