// Reads the dependency coordinates an Android App Bundle declares about ITSELF.
//
// Why this module exists (measured 2026-10-08). Google Play's "outdated SDK" notices are about the
// SHIPPED ARTIFACT, not about the build script that produced it. Every AAB carries
// `BUNDLE-METADATA/com.android.tools.build.libraries/dependencies.pb` — a protobuf listing every
// maven coordinate (group, name, version) that went into the build, and that file is exactly what
// the console reads when it decides to send the notice. The factory had been building these for
// weeks and had never opened one: it enforced freshness by regexing `app/build.gradle.kts` for a
// `constraints { }` block, which is a *proxy* for the artifact and a weak one. On 2026-10-08 Play
// flagged `2026-10-02-laneset` for `androidx.fragment:fragment:1.1.0` while the source-level gate
// read green, because the shipped bundle had been built before the constraint existed and no
// program had ever asked the bundle what it contained.
//
// Two decoders, both pure and both exercised by fixtures in `scripts/core/deps.mjs`:
//   - a minimal ZIP central-directory reader, because Node ships no zip API;
//   - a protobuf walker that descends only into chunks that parse EXACTLY, because a length-delimited
//     field may be a digest or a dex blob, and a walker that guesses invents fields out of hash bytes
//     (the first version of this reader did exactly that, and crashed on it).
//
// The schema it depends on, measured from real bundles rather than assumed:
//   AppDependencies { repeated Library libraries = 1; ... }
//   Library         { MavenLibrary maven_library = 1; ... }
//   MavenLibrary    { string group = 1; string name = 2; string version = 5; }
// The version really is field 5 and not 3 — verified against coordinates whose values the factory
// already pins (Kotlin 2.3.21, Compose BOM 2026.09.00). Fields 2/3/4 of the top-level message are
// per-file digests, the module name and the maven repositories; the library list is field 1 alone.

import { inflateRawSync } from 'node:zlib'

/** The AAB entry that carries the bundle's own dependency inventory. */
export const DEPENDENCY_METADATA_ENTRY = 'BUNDLE-METADATA/com.android.tools.build.libraries/dependencies.pb'

const EOCD_SIG = 0x06054b50
const CD_SIG = 0x02014b50
const LOCAL_SIG = 0x04034b50
const ZIP64_EOCD_LOCATOR_SIG = 0x07064b50
const ZIP64_EOCD_SIG = 0x06064b50
const ZIP64_EXTRA_ID = 0x0001
const U32_MAX = 0xffffffff

function findEndOfCentralDirectory(buf) {
  const floor = Math.max(0, buf.length - 66_000)
  for (let i = buf.length - 22; i >= floor; i--) {
    if (buf.readUInt32LE(i) === EOCD_SIG) return i
  }
  return -1
}

/**
 * The central directory's entry count and offset, resolving the ZIP64 records when the 32-bit
 * fields are saturated. Returns null when the archive is not readable as a zip.
 */
function readCentralDirectoryLocation(buf, eocd) {
  let count = buf.readUInt16LE(eocd + 10)
  let offset = buf.readUInt32LE(eocd + 16)
  if (count === 0xffff || offset === U32_MAX) {
    const locator = eocd - 20
    if (locator < 0 || buf.readUInt32LE(locator) !== ZIP64_EOCD_LOCATOR_SIG) return null
    const z64 = Number(buf.readBigUInt64LE(locator + 8))
    if (z64 < 0 || z64 + 56 > buf.length || buf.readUInt32LE(z64) !== ZIP64_EOCD_SIG) return null
    count = Number(buf.readBigUInt64LE(z64 + 32))
    offset = Number(buf.readBigUInt64LE(z64 + 48))
  }
  if (offset < 0 || offset >= buf.length) return null
  return { count, offset }
}

/** The 64-bit uncompressed size from an entry's ZIP64 extra field, or null. */
function zip64CompressedSize(buf, extraStart, extraLen) {
  let p = extraStart
  const end = extraStart + extraLen
  while (p + 4 <= end) {
    const id = buf.readUInt16LE(p)
    const size = buf.readUInt16LE(p + 2)
    if (id === ZIP64_EXTRA_ID) {
      // Fields present only when the matching 32-bit field was saturated; order is uncompressed,
      // then compressed. We need the compressed one, so walk past the sizes that are present.
      return { uncompressed: buf.readBigUInt64LE(p + 4), compressed: buf.readBigUInt64LE(p + 12) }
    }
    p += 4 + size
  }
  return null
}

/**
 * The bytes of one entry in a zip archive, or null when the entry is absent, structurally
 * unreadable, or uses a compression method this reader does not implement. Null is reported as
 * UNVERIFIED by the caller and is never read as a pass.
 */
export function readZipEntry(buf, entryName) {
  const eocd = findEndOfCentralDirectory(buf)
  if (eocd < 0) return null
  const location = readCentralDirectoryLocation(buf, eocd)
  if (!location) return null

  let p = location.offset
  for (let i = 0; i < location.count; i++) {
    if (p + 46 > buf.length || buf.readUInt32LE(p) !== CD_SIG) return null
    const method = buf.readUInt16LE(p + 10)
    let compressedSize = buf.readUInt32LE(p + 20)
    const nameLen = buf.readUInt16LE(p + 28)
    const extraLen = buf.readUInt16LE(p + 30)
    const commentLen = buf.readUInt16LE(p + 32)
    const localOffset = buf.readUInt32LE(p + 42)
    const name = buf.toString('utf8', p + 46, p + 46 + nameLen)
    const extraStart = p + 46 + nameLen

    if (name === entryName) {
      if (compressedSize === U32_MAX) {
        const z64 = zip64CompressedSize(buf, extraStart, extraLen)
        if (!z64) return null
        compressedSize = Number(z64.compressed)
      }
      if (localOffset === U32_MAX) return null
      if (localOffset + 30 > buf.length || buf.readUInt32LE(localOffset) !== LOCAL_SIG) return null
      const localNameLen = buf.readUInt16LE(localOffset + 26)
      const localExtraLen = buf.readUInt16LE(localOffset + 28)
      const start = localOffset + 30 + localNameLen + localExtraLen
      if (start + compressedSize > buf.length) return null
      const data = buf.subarray(start, start + compressedSize)
      if (method === 0) return Buffer.from(data)
      if (method === 8) {
        try {
          return inflateRawSync(data)
        } catch {
          return null
        }
      }
      return null
    }
    p = extraStart + extraLen + commentLen
  }
  return null
}

function readVarint(buf, pos) {
  let shift = 0
  let value = 0
  while (true) {
    if (pos >= buf.length) return null
    const byte = buf[pos]
    pos += 1
    value += (byte & 0x7f) * 2 ** shift
    if ((byte & 0x80) === 0) return { value, pos }
    shift += 7
    if (shift > 63) return null
  }
}

/**
 * A protobuf message as `{ field, wire, value }[]`, or null when `buf` is not EXACTLY one
 * well-formed message. The exactness is the point: it is what stops this walker descending into a
 * sha256 blob and reporting fields that were never there.
 */
export function parseMessage(buf) {
  if (!buf || buf.length === 0) return null
  const out = []
  let pos = 0
  while (pos < buf.length) {
    const key = readVarint(buf, pos)
    if (!key) return null
    pos = key.pos
    const field = key.value >>> 3
    const wire = key.value & 7
    if (field === 0) return null
    if (wire === 0) {
      const v = readVarint(buf, pos)
      if (!v) return null
      pos = v.pos
      out.push({ field, wire, value: v.value })
    } else if (wire === 2) {
      const len = readVarint(buf, pos)
      if (!len) return null
      pos = len.pos
      if (pos + len.value > buf.length) return null
      out.push({ field, wire, value: buf.subarray(pos, pos + len.value) })
      pos += len.value
    } else if (wire === 5) {
      if (pos + 4 > buf.length) return null
      pos += 4
      out.push({ field, wire, value: null })
    } else if (wire === 1) {
      if (pos + 8 > buf.length) return null
      pos += 8
      out.push({ field, wire, value: null })
    } else {
      return null
    }
  }
  return out
}

function stringField(fields, number) {
  const hit = fields.find((f) => f.field === number && f.wire === 2)
  if (!hit) return null
  const text = hit.value.toString('utf8')
  return text.length > 0 ? text : null
}

/**
 * Every maven coordinate the protobuf lists: `[{ group, name, version }]`, in file order.
 * An entry missing any of the three parts is skipped rather than guessed at.
 */
export function decodeDependencyList(buf) {
  const top = parseMessage(buf)
  if (!top) return []
  const out = []
  for (const libraryField of top) {
    if (libraryField.field !== 1 || libraryField.wire !== 2) continue
    const library = parseMessage(libraryField.value)
    if (!library) continue
    for (const mavenField of library) {
      if (mavenField.field !== 1 || mavenField.wire !== 2) continue
      const maven = parseMessage(mavenField.value)
      if (!maven) continue
      const group = stringField(maven, 1)
      const name = stringField(maven, 2)
      const version = stringField(maven, 5)
      if (group && name && version) out.push({ group, name, version })
    }
  }
  return out
}

/**
 * The coordinates declared by one AAB file's bytes, or null when the bundle carries no readable
 * dependency inventory. Null is a finding, not a pass: a bundle that cannot be asked the question
 * is not a bundle that answered it.
 */
export function aabDependencyCoordinates(aabBytes) {
  const blob = readZipEntry(aabBytes, DEPENDENCY_METADATA_ENTRY)
  if (!blob) return null
  return decodeDependencyList(blob)
}
