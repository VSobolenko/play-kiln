// 16 KB page-size readiness, measured from the built artifact itself.
//
// Ported from scripts/check-16k.py, which exists because `scripts/prepare-release.mjs` could only
// ever write `pageSizeReadiness.status = "confirmation-required"` for an app that ships any native
// library: nothing in the factory read the artifact to confirm the requirement. The Python sensor
// stays in place until this one is proven equivalent, and its docstrings record the two defects
// this port must not reintroduce:
//
//   1. A path predicate written as `"/lib/" in name` matched an AAB's `base/lib/...` and matched
//      NOTHING in an APK's `lib/...`, so the artifact a device actually loads was reported as
//      shipping no native libraries — a green row from measuring an empty set. The predicate below
//      tests both container shapes explicitly, and `apk.fixtures` re-asserts it.
//   2. Measuring `header_offset % 16384` instead of the local header's data offset failed a
//      correctly aligned APK, because zipalign pads the extra field and the central directory does
//      not show that padding. The offset is read from the LOCAL header (30 + nameLength +
//      extraLength), never from `headerOffset`.
//
// Two independent conditions hold, and they fail separately:
//
//   1. **The ELF is built for 16 KB pages.** Every `PT_LOAD` segment's `p_align` must be at least
//      16384. One 4 KB segment among 16 KB ones makes the library unloadable on a 16 KB kernel, so
//      the check is the MINIMUM over every segment, and an ELF with no `PT_LOAD` is not ready
//      either — `Math.min([])` is Infinity, which is exactly how an empty set reads as a pass.
//   2. **The container keeps it that way, for an APK only.** The `.so` entry must be STORED and its
//      data offset must be a multiple of 16384. An AAB is repackaged by Play, so only condition 1
//      applies to it; applying the container rule there would fail a legal bundle.
//
// ZIP reading is pure JS (EOCD, central directory, local headers, `zlib.inflateRawSync`) so the one
// process contract holds: `scripts/verify.mjs` spawns nothing, not even a Python interpreter.
import { existsSync, readFileSync, readdirSync } from 'node:fs'
import path from 'node:path'
import zlib from 'node:zlib'
import { expectEqual, expectTrue, fail, guard, pass, skip, unverified } from './lib.mjs'

const PAGE_16K = 16384
const ELF_MAGIC = Buffer.from([0x7f, 0x45, 0x4c, 0x46])
const PT_LOAD = 1
const ZIP_LOCAL = 0x04034b50
const ZIP_CENTRAL = 0x02014b50
const ZIP_EOCD = 0x06054b50
const ARTIFACT = /\.(apk|aab)$/i

const viewOf = (bytes) => new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength)

// ---------------------------------------------------------------------------------------------
// The ELF half: is every PT_LOAD segment aligned for a 16 KB kernel?
// ---------------------------------------------------------------------------------------------

/**
 * `p_align` of every `PT_LOAD` program header in a 32- or 64-bit ELF, either endianness.
 *
 * Refuses rather than guesses: an ELF class or endianness this reader does not know, or a program
 * header that runs past the end of the file, throws. A misread alignment would report a 4 KB
 * library as ready, which is the one answer this check must never produce.
 */
export function elfLoadAlignments(blob) {
  if (blob.length < 6) throw new Error('truncated ELF identification')
  if (!blob.subarray(0, 4).equals(ELF_MAGIC)) throw new Error('not an ELF file')
  const elfClass = blob[4]
  const dataEncoding = blob[5]
  if (elfClass !== 1 && elfClass !== 2) throw new Error(`unsupported ELF class ${elfClass}`)
  if (dataEncoding !== 1 && dataEncoding !== 2) {
    throw new Error(`unsupported ELF data encoding ${dataEncoding}`)
  }
  const is64 = elfClass === 2
  const little = dataEncoding === 1
  if (blob.length < (is64 ? 64 : 52)) throw new Error('truncated ELF header')
  const view = viewOf(blob)

  let phoff
  let phentsize
  let phnum
  let alignOffset
  let defaultEntrySize
  if (is64) {
    phoff = Number(view.getBigUint64(0x20, little))
    phentsize = view.getUint16(0x36, little)
    phnum = view.getUint16(0x38, little)
    // ELF64 program header: p_type(4) p_flags(4) p_offset(8) p_vaddr(8) p_paddr(8)
    //                        p_filesz(8) p_memsz(8) p_align(8)
    alignOffset = 0x30
    defaultEntrySize = 56
  } else {
    phoff = view.getUint32(0x1c, little)
    phentsize = view.getUint16(0x2a, little)
    phnum = view.getUint16(0x2c, little)
    // ELF32 program header: p_type(4) p_offset(4) p_vaddr(4) p_paddr(4)
    //                       p_filesz(4) p_memsz(4) p_flags(4) p_align(4)
    alignOffset = 0x1c
    defaultEntrySize = 32
  }
  const entrySize = phentsize || defaultEntrySize
  const aligns = []
  for (let index = 0; index < phnum; index += 1) {
    const base = phoff + index * entrySize
    if (base < 0 || base + entrySize > blob.length) {
      throw new Error(`program header ${index} runs past the end of the file`)
    }
    if (view.getUint32(base, little) !== PT_LOAD) continue
    aligns.push(is64 ? Number(view.getBigUint64(base + alignOffset, little)) : view.getUint32(base + alignOffset, little))
  }
  return aligns
}

/**
 * Ready only when there is at least one `PT_LOAD` and the MINIMUM alignment clears 16384.
 *
 * The `length > 0` guard is the whole point: `Math.min(...[])` is `Infinity`, so an ELF whose
 * segments the reader failed to enumerate would otherwise read as perfectly aligned.
 */
export function isElf16kReady(aligns) {
  return aligns.length > 0 && Math.min(...aligns) >= PAGE_16K
}

// ---------------------------------------------------------------------------------------------
// The path predicate: which entries are native libraries, in both container shapes
// ---------------------------------------------------------------------------------------------

/**
 * True for a packaged native library, in either container's path shape.
 *
 * This is the exact predicate whose first version caused a false PASS: `"/lib/" in name` matched
 * the AAB's `base/lib/<abi>/<name>.so` and matched nothing at all in the APK's `lib/<abi>/<name>.so`,
 * so the APK — the artifact loaded on the device — was reported as shipping no native libraries.
 * Both shapes are therefore named explicitly, and `apk.fixtures` asserts each one.
 */
export function isNativeLibraryEntry(name) {
  if (!name.endsWith('.so')) return false
  return name.startsWith('lib/') || name.includes('/lib/')
}

// ---------------------------------------------------------------------------------------------
// The container half: a minimal, dependency-free ZIP reader
// ---------------------------------------------------------------------------------------------

function findEndOfCentralDirectory(view, length) {
  const earliest = Math.max(0, length - 22 - 0xffff)
  for (let offset = length - 22; offset >= earliest; offset -= 1) {
    if (view.getUint32(offset, true) === ZIP_EOCD) return offset
  }
  throw new Error('no End Of Central Directory record, so this file is not a ZIP archive')
}

/**
 * Walks the central directory and returns one record per entry.
 *
 * A ZIP64 archive is refused rather than partially walked: its real entry count and directory
 * offset live outside the fields read here, so a partial list would under-report the libraries and
 * a shortened list reads exactly like a smaller app. Refusing becomes UNVERIFIED upstream, which is
 * the honest state; guessing would become a PASS.
 */
export function readZipEntries(bytes) {
  if (bytes.length < 22) throw new Error('the file is too short to be a ZIP archive')
  const view = viewOf(bytes)
  const eocd = findEndOfCentralDirectory(view, bytes.length)
  const totalEntries = view.getUint16(eocd + 10, true)
  const centralSize = view.getUint32(eocd + 12, true)
  const centralOffset = view.getUint32(eocd + 16, true)
  if (totalEntries === 0xffff || centralSize === 0xffffffff || centralOffset === 0xffffffff) {
    throw new Error('ZIP64 central directory: refusing to walk it rather than report a partial list')
  }
  const entries = []
  let cursor = centralOffset
  for (let index = 0; index < totalEntries; index += 1) {
    if (cursor + 46 > bytes.length) {
      throw new Error(`central directory entry ${index} runs past the end of the file`)
    }
    if (view.getUint32(cursor, true) !== ZIP_CENTRAL) {
      throw new Error(`central directory entry ${index} has no signature at 0x${cursor.toString(16)}`)
    }
    const nameLength = view.getUint16(cursor + 28, true)
    const extraLength = view.getUint16(cursor + 30, true)
    const commentLength = view.getUint16(cursor + 32, true)
    entries.push({
      name: bytes.subarray(cursor + 46, cursor + 46 + nameLength).toString('utf8'),
      method: view.getUint16(cursor + 10, true),
      compressedSize: view.getUint32(cursor + 20, true),
      uncompressedSize: view.getUint32(cursor + 24, true),
      headerOffset: view.getUint32(cursor + 42, true)
    })
    cursor += 46 + nameLength + extraLength + commentLength
  }
  return entries
}

/**
 * Where this entry's *data* begins in the file, not where its header begins.
 *
 * `headerOffset` points at the local file header, which is not the alignment a loader cares about
 * and not the number `zipalign -P 16` aligns. The local header's own name-length and extra-length
 * fields decide where the payload starts, and zipalign pads the extra field to land that payload on
 * a page boundary — padding that is invisible in the central directory. Measuring
 * `headerOffset % 16384` instead would fail a correctly aligned APK, which is the mirror image of
 * the false pass the predicate above used to produce.
 */
export function storedDataOffset(bytes, headerOffset) {
  if (headerOffset + 30 > bytes.length) throw new Error('local file header runs past the end of the file')
  const view = viewOf(bytes)
  if (view.getUint32(headerOffset, true) !== ZIP_LOCAL) {
    throw new Error('no local file header at the offset the central directory records')
  }
  const nameLength = view.getUint16(headerOffset + 26, true)
  const extraLength = view.getUint16(headerOffset + 28, true)
  return headerOffset + 30 + nameLength + extraLength
}

function readEntryData(bytes, entry) {
  const dataOffset = storedDataOffset(bytes, entry.headerOffset)
  const end = dataOffset + entry.compressedSize
  if (end > bytes.length) throw new Error('entry data runs past the end of the file')
  const payload = bytes.subarray(dataOffset, end)
  if (entry.method === 0) return payload
  if (entry.method === 8) return zlib.inflateRawSync(payload)
  throw new Error(`compression method ${entry.method} is outside this reader's subset`)
}

const formatAlign = (value) => (value === null ? 'none' : `0x${value.toString(16).toUpperCase()} (${value})`)

/**
 * Measures one archive. `suffix` decides whether the container rule applies.
 *
 * Returned as `{ rows, failures, isApk }` rather than a printed report so a fixture can assert each
 * measurement, not just the final verdict — a finding that exists only as formatted text cannot be
 * cross-checked, only trusted.
 */
export function inspectArchive(bytes, suffix) {
  const isApk = String(suffix).toLowerCase() === '.apk'
  const rows = []
  const failures = []
  for (const entry of readZipEntries(bytes)) {
    if (!isNativeLibraryEntry(entry.name)) continue
    const row = { name: entry.name }
    let blob
    try {
      blob = readEntryData(bytes, entry)
    } catch (error) {
      row.error = error.message
      failures.push(`${entry.name}: ${error.message}`)
      rows.push(row)
      continue
    }
    row.bytes = blob.length
    try {
      row.loadAlignments = elfLoadAlignments(blob)
    } catch (error) {
      row.error = error.message
      failures.push(`${entry.name}: ${error.message}`)
      rows.push(row)
      continue
    }
    row.minAlign = row.loadAlignments.length > 0 ? Math.min(...row.loadAlignments) : null
    row.elf16kOk = isElf16kReady(row.loadAlignments)
    if (!row.elf16kOk) {
      failures.push(`${entry.name}: min PT_LOAD p_align=${formatAlign(row.minAlign)} < ${PAGE_16K}`)
    }
    // Only an APK is mapped directly by the loader; Play repackages an AAB into per-device APKs, so
    // the bundle's own entry alignment is not the alignment that ships.
    if (isApk) {
      row.stored = entry.method === 0
      row.dataOffset = storedDataOffset(bytes, entry.headerOffset)
      row.offsetAligned = row.dataOffset % PAGE_16K === 0
      if (!row.stored) {
        failures.push(`${entry.name}: compressed in the APK, so it cannot be page-aligned`)
      } else if (!row.offsetAligned) {
        failures.push(
          `${entry.name}: stored at data offset 0x${row.dataOffset.toString(16).toUpperCase()}, ` +
            `not a multiple of ${PAGE_16K}`
        )
      }
    }
    rows.push(row)
  }
  return { rows, failures, isApk }
}

export function inspectArtifact(file) {
  return inspectArchive(readFileSync(file), path.extname(file).toLowerCase())
}

// ---------------------------------------------------------------------------------------------
// Inline fixtures — the sensors this module depends on, made falsifiable
// ---------------------------------------------------------------------------------------------

/** A minimal but structurally valid ELF whose `PT_LOAD` `p_align` values are exactly as asked. */
export function elfWithLoadAlign(align, extraLoads = [], options = {}) {
  const is64 = options.is64 !== false
  const little = options.little !== false
  const loadTypes = options.loadTypes || null
  const aligns = [align, ...extraLoads]
  const phoff = 64
  const entrySize = is64 ? 56 : 32
  const blob = Buffer.alloc(phoff + entrySize * aligns.length)
  ELF_MAGIC.copy(blob, 0)
  blob[4] = is64 ? 2 : 1
  blob[5] = little ? 1 : 2
  const view = viewOf(blob)
  if (is64) {
    view.setBigUint64(0x20, BigInt(phoff), little)
    view.setUint16(0x36, entrySize, little)
    view.setUint16(0x38, aligns.length, little)
  } else {
    view.setUint32(0x1c, phoff, little)
    view.setUint16(0x2a, entrySize, little)
    view.setUint16(0x2c, aligns.length, little)
  }
  aligns.forEach((value, index) => {
    const base = phoff + index * entrySize
    view.setUint32(base, loadTypes ? loadTypes[index] : PT_LOAD, little)
    if (is64) view.setBigUint64(base + 0x30, BigInt(value), little)
    else view.setUint32(base + 0x1c, value, little)
  })
  return blob
}

/**
 * Builds an in-memory archive so a fixture can control the one number that matters — the data
 * offset — without a temp file or a fixture-only code path. `align` pads the extra field the way
 * zipalign does, which is the only way to produce a STORED entry whose payload lands on (or off) a
 * page boundary; the reader then has to agree about where that boundary is.
 */
function buildZip(specs) {
  const parts = []
  const centrals = []
  let offset = 0
  for (const spec of specs) {
    const name = Buffer.from(spec.name, 'utf8')
    const method = spec.method || 0
    const payload = method === 8 ? zlib.deflateRawSync(spec.data) : spec.data
    let extraLength = 0
    if (spec.align === 'aligned' || spec.align === 'misaligned') {
      const base = offset + 30 + name.length
      const toBoundary = (PAGE_16K - (base % PAGE_16K)) % PAGE_16K
      extraLength = spec.align === 'aligned' ? toBoundary : toBoundary + 1
    }
    const local = Buffer.alloc(30 + name.length + extraLength)
    local.writeUInt32LE(ZIP_LOCAL, 0)
    local.writeUInt16LE(20, 4)
    local.writeUInt16LE(method, 8)
    local.writeUInt32LE(payload.length, 18)
    local.writeUInt32LE(spec.data.length, 22)
    local.writeUInt16LE(name.length, 26)
    local.writeUInt16LE(extraLength, 28)
    name.copy(local, 30)
    parts.push(local, payload)

    const central = Buffer.alloc(46 + name.length)
    central.writeUInt32LE(ZIP_CENTRAL, 0)
    central.writeUInt16LE(20, 4)
    central.writeUInt16LE(20, 6)
    central.writeUInt16LE(method, 10)
    central.writeUInt32LE(payload.length, 20)
    central.writeUInt32LE(spec.data.length, 24)
    central.writeUInt16LE(name.length, 28)
    central.writeUInt32LE(offset, 42)
    name.copy(central, 46)
    centrals.push(central)
    offset += local.length + payload.length
  }
  const central = Buffer.concat(centrals)
  const eocd = Buffer.alloc(22)
  eocd.writeUInt32LE(ZIP_EOCD, 0)
  eocd.writeUInt16LE(specs.length, 8)
  eocd.writeUInt16LE(specs.length, 10)
  eocd.writeUInt32LE(central.length, 12)
  eocd.writeUInt32LE(offset, 16)
  return Buffer.concat([...parts, central, eocd])
}

export function sixteenKbFixtures() {
  return guard('apk.fixtures', () => {
    const cases = []

    // The false pass that motivated the whole check: the APK's root-level `lib/...` must be
    // recognised, and the AAB's nested `base/lib/...` alongside it. Before this existed,
    // `"/lib/" in name` matched only the second.
    const shapes = {
      'lib/arm64-v8a/libx.so': true,
      'base/lib/arm64-v8a/libx.so': true,
      'lib/x86_64/libdatastore_shared_counter.so': true,
      'res/raw/libx.so': false,
      'assets/jni/libx.so': false,
      'lib/arm64-v8a/libx.so.txt': false,
      'classes.dex': false
    }
    for (const [name, want] of Object.entries(shapes)) {
      cases.push([
        `path shape: ${name} ${want ? 'is' : 'is not'} a native library`,
        () => expectEqual(isNativeLibraryEntry(name), want, name)
      ])
    }

    cases.push([
      'a 4 KB PT_LOAD is judged NOT 16 KB ready',
      () => {
        const aligns = elfLoadAlignments(elfWithLoadAlign(4096))
        expectEqual(aligns, [4096], 'aligns')
        expectTrue(!isElf16kReady(aligns), 'a 4 KB segment must not be ready')
      }
    ])
    cases.push([
      'a 16 KB PT_LOAD is judged ready',
      () => {
        const aligns = elfLoadAlignments(elfWithLoadAlign(PAGE_16K, [PAGE_16K]))
        expectEqual(aligns, [PAGE_16K, PAGE_16K], 'aligns')
        expectTrue(isElf16kReady(aligns), 'a 16 KB segment must be ready')
      }
    ])
    cases.push([
      'an ELF with no PT_LOAD is not ready',
      () => {
        // The empty-set pass: `Math.min(...[])` is Infinity, so only an explicit length guard keeps
        // a library whose segments were not enumerated from reading as perfectly aligned.
        const aligns = elfLoadAlignments(elfWithLoadAlign(PAGE_16K, [], { loadTypes: [0] }))
        expectEqual(aligns, [], 'no PT_LOAD')
        expectTrue(!isElf16kReady(aligns), 'an empty alignment list must not be ready')
      }
    ])
    cases.push([
      'a later 4 KB segment among 16 KB ones is still caught',
      () => {
        const aligns = elfLoadAlignments(elfWithLoadAlign(PAGE_16K, [4096]))
        expectEqual(aligns, [PAGE_16K, 4096], 'aligns')
        expectTrue(!isElf16kReady(aligns), 'one 4 KB segment makes the library unloadable')
      }
    ])
    cases.push([
      'a 32-bit little-endian ELF is read',
      () => {
        const aligns = elfLoadAlignments(elfWithLoadAlign(4096, [], { is64: false }))
        expectEqual(aligns, [4096], 'aligns')
      }
    ])
    cases.push([
      'a 64-bit big-endian ELF is read',
      () => {
        const aligns = elfLoadAlignments(elfWithLoadAlign(PAGE_16K, [], { little: false }))
        expectEqual(aligns, [PAGE_16K], 'aligns')
        expectTrue(isElf16kReady(aligns), 'a big-endian ELF is still measured')
      }
    ])

    const elf = elfWithLoadAlign(PAGE_16K)

    cases.push([
      'a 16 KB ELF stored and page-aligned in an APK passes both conditions',
      () => {
        const report = inspectArchive(buildZip([{ name: 'lib/arm64-v8a/libprobe.so', data: elf, align: 'aligned' }]), '.apk')
        expectEqual(report.rows.length, 1, 'one native library measured')
        expectEqual(report.failures, [], 'failures')
        expectTrue(report.rows[0].stored === true, 'the entry is STORED')
        expectTrue(report.rows[0].offsetAligned === true, 'the data offset is a multiple of 16384')
      }
    ])
    cases.push([
      'a valid 16 KB ELF at a misaligned offset is rejected for the offset alone',
      () => {
        // The discriminating case: the ELF half passes and only the container half can complain, so
        // a dead alignment branch would leave `failures` empty and this fixture red.
        const report = inspectArchive(buildZip([{ name: 'lib/arm64-v8a/libprobe.so', data: elf, align: 'misaligned' }]), '.apk')
        expectTrue(report.rows[0].elf16kOk === true, 'the ELF half passes')
        expectTrue(report.rows[0].offsetAligned === false, 'the offset is misaligned')
        expectEqual(report.failures.length, 1, 'exactly one complaint')
        expectTrue(report.failures[0].includes('not a multiple of'), report.failures[0])
      }
    ])
    cases.push([
      'a deflated .so in an APK is rejected even at an aligned offset',
      () => {
        const report = inspectArchive(
          buildZip([{ name: 'lib/arm64-v8a/libprobe.so', data: elf, method: 8, align: 'aligned' }]),
          '.apk'
        )
        expectTrue(report.rows[0].offsetAligned === true, 'the fixture really is aligned')
        expectTrue(
          report.failures.some((item) => item.includes('compressed')),
          JSON.stringify(report.failures)
        )
      }
    ])
    cases.push([
      'the same misaligned offset is accepted in an AAB, which Play repackages',
      () => {
        const report = inspectArchive(
          buildZip([{ name: 'base/lib/arm64-v8a/libprobe.so', data: elf, align: 'misaligned' }]),
          '.aab'
        )
        expectEqual(report.failures, [], 'failures')
        expectTrue(report.rows[0].offsetAligned === undefined, 'the container half must not apply')
      }
    ])
    cases.push([
      'a 4 KB ELF inside an AAB is still rejected on the ELF half',
      () => {
        const report = inspectArchive(
          buildZip([{ name: 'base/lib/arm64-v8a/libbad.so', data: elfWithLoadAlign(4096), align: 'aligned' }]),
          '.aab'
        )
        expectEqual(report.failures.length, 1, 'one complaint')
        expectTrue(report.failures[0].includes('p_align'), report.failures[0])
      }
    ])
    cases.push([
      'a non-library entry is never measured',
      () => {
        const report = inspectArchive(buildZip([{ name: 'res/raw/libx.so', data: elfWithLoadAlign(4096) }]), '.apk')
        expectEqual(report.rows, [], 'rows')
        expectEqual(report.failures, [], 'failures')
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
      ? pass('apk.fixtures', `${cases.length} fixtures`)
      : fail('apk.fixtures', failures.join(' | '))
  })
}

// ---------------------------------------------------------------------------------------------
// The check
// ---------------------------------------------------------------------------------------------

/**
 * Measures 16 KB readiness for every `.apk` and `.aab` under the slot's `dist/`.
 *
 * `dist/` is gitignored and normally empty, so the absent case is reported as `skip` with its
 * reason rather than a pass: a green row that measured nothing is the exact defect this port exists
 * to prevent, and a genuinely absent artifact is not evidence of readiness in either direction.
 */
export function checkSixteenKb(slotDir) {
  return guard('apk.sixteenKb', () => {
    const dist = path.join(slotDir, 'dist')
    if (!existsSync(dist)) {
      return skip('apk.sixteenKb', 'no dist/: it is gitignored, so there is no built artifact to measure')
    }
    const artifacts = readdirSync(dist)
      .filter((name) => ARTIFACT.test(name))
      .sort()
    if (artifacts.length === 0) {
      return skip('apk.sixteenKb', 'dist/ holds no .apk and no .aab, so no native library was measured')
    }

    const failures = []
    const unreadable = []
    const summary = []
    const empty = []
    let libraries = 0
    let apks = 0
    let aabs = 0
    for (const name of artifacts) {
      const suffix = path.extname(name).toLowerCase()
      try {
        const { rows, failures: found } = inspectArtifact(path.join(dist, name))
        libraries += rows.length
        if (rows.length === 0) empty.push(name)
        if (suffix === '.apk') apks += 1
        else aabs += 1
        failures.push(...found.map((item) => `${name}: ${item}`))
        summary.push(`${name}: ${rows.length} librar${rows.length === 1 ? 'y' : 'ies'}`)
      } catch (error) {
        // An artifact that cannot be read is not a pass and not a finding about the app: it is a
        // measurement that did not happen, which is UNVERIFIED.
        unreadable.push(`${name}: ${error.message}`)
      }
    }

    if (failures.length > 0) {
      const detail = failures.join('; ') + (unreadable.length > 0 ? ` | not measurable: ${unreadable.join('; ')}` : '')
      return fail('apk.sixteenKb', detail)
    }
    if (unreadable.length > 0) {
      return unverified('apk.sixteenKb', `could not evaluate: ${unreadable.join('; ')}`)
    }

    const notes = [
      `${artifacts.length} artifact(s)`,
      `${libraries} native librar${libraries === 1 ? 'y' : 'ies'} measured`
    ]
    if (apks > 0) notes.push(`${apks} APK(s) with every .so STORED at a ${PAGE_16K}-byte data offset`)
    if (aabs > 0) notes.push(`${aabs} AAB(s) judged on ELF alignment only, because Play repackages the bundle`)
    if (empty.length > 0) {
      // Said out loud, per artifact, because "no library found" is exactly how the historical false
      // pass read: the predicate matched nothing in an APK and the row stayed green. An empty set is
      // only distinguishable from a missing library by `apk.fixtures`, so it must be visible here.
      notes.push(
        `no native library entry matched lib/ or base/lib/ in ${empty.join(', ')}, so ` +
          `${empty.length === 1 ? 'that artifact' : 'those artifacts'} measured an empty set`
      )
    }
    return pass('apk.sixteenKb', `${notes.join('; ')} (${summary.join(', ')})`)
  })
}

/** The Apk checks that need a slot, mirroring the other modules' `*Checks` aggregators. */
export function apkChecks(slotDir) {
  return [checkSixteenKb(slotDir)]
}
