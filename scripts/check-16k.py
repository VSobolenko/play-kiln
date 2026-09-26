#!/usr/bin/env python3
"""Measure 16 KB page-size readiness on a built artifact, from the artifact itself.

`scripts/prepare-release.mjs` writes `pageSizeReadiness.status = "confirmation-required"` whenever
the app ships any native library, because the factory never wrote the check that would confirm it:
the OpenSpec task for slot 1 says "if no native library is shipped, ..." and stops there. So an app
with `.so` files - this one, through AndroidX graphics-path and datastore - carries an honestly
unconfirmed Play requirement in its release manifest. This is that check.

Two independent conditions have to hold, and they fail separately:

1. **The ELF is built for 16 KB pages.** Every `PT_LOAD` segment's `p_align` must be at least 16384.
   A library built for 4 KB (`p_align` 4096) cannot be mapped on a 16 KB-page kernel; this is the
   condition Play reports as "not 16 KB compatible".
2. **The container keeps it that way.** The `.so` must be stored UNCOMPRESSED and page-aligned
   inside the archive, because the loader maps the file directly. A perfectly aligned ELF that the
   packager deflated is not loadable-aligned, which is why `zipalign -P 16` exists as a separate
   check. For an APK that is the entry's `header_offset % 16384`; an AAB is not required to be
   aligned itself, since Play regenerates the APKs, so for the AAB only condition 1 is meaningful.

Usage:
    python scripts/check-16k.py apps/<slot>/dist/<artifact>.apk [more artifacts...]
    python scripts/check-16k.py --selftest        # prove the sensor can fail

Exit code is 0 when every library passes, 1 when any fails, so it cannot be misread as a pass.
"""

from __future__ import annotations

import struct
import sys
import tempfile
import zipfile
from pathlib import Path

PAGE_16K = 16384
ELF_MAGIC = b"\x7fELF"


def elf_load_alignments(blob: bytes) -> list[int]:
    """p_align of every PT_LOAD program header in a 32- or 64-bit ELF."""
    if blob[:4] != ELF_MAGIC:
        raise ValueError("not an ELF file")
    is_64 = blob[4] == 2
    endian = "<" if blob[5] == 1 else ">"
    if is_64:
        e_phoff, = struct.unpack_from(endian + "Q", blob, 0x20)
        e_phentsize, e_phnum = struct.unpack_from(endian + "HH", blob, 0x36)
        # ELF64 program header: p_type(4) p_flags(4) p_offset(8) p_vaddr(8) p_paddr(8)
        #                        p_filesz(8) p_memsz(8) p_align(8)
        align_offset, entry_size = 0x30, 56
    else:
        e_phoff, = struct.unpack_from(endian + "I", blob, 0x1C)
        e_phentsize, e_phnum = struct.unpack_from(endian + "HH", blob, 0x2A)
        # ELF32 program header: p_type(4) p_offset(4) p_vaddr(4) p_paddr(4)
        #                       p_filesz(4) p_memsz(4) p_flags(4) p_align(4)
        align_offset, entry_size = 0x1C, 32
    aligns = []
    for index in range(e_phnum):
        base = e_phoff + index * (e_phentsize or entry_size)
        p_type, = struct.unpack_from(endian + "I", blob, base)
        if p_type != 1:  # PT_LOAD
            continue
        p_align, = struct.unpack_from(endian + "I" if not is_64 else endian + "Q",
                                      blob, base + align_offset)
        aligns.append(p_align)
    return aligns


def is_native_library_entry(name: str) -> bool:
    """True for a packaged native library, in either container's path shape.

    The two containers disagree, and assuming one silently produced a *false PASS* here: an APK
    stores `lib/<abi>/<name>.so` at the archive root, while an AAB nests it under
    `base/lib/<abi>/<name>.so`. A predicate written as `"/lib/" in name` - which is what the first
    version of this file used - matched the AAB and matched nothing in the APK, so the artifact that
    is actually loaded on the device was reported as shipping no native libraries at all. The
    measurement looked like a pass and was measuring an empty set.
    """
    if not name.endswith(".so"):
        return False
    return name.startswith("lib/") or "/lib/" in name


def stored_data_offset(artifact: Path, info: zipfile.ZipInfo) -> int:
    """Where this entry's *data* begins in the file, not where its header begins.

    `ZipInfo.header_offset` points at the local file header, so it is not the alignment a loader
    cares about, and it is not the number `zipalign -P 16` aligns. The local header's own
    name-length and extra-length fields decide where the payload starts, and zipalign pads the extra
    field to land that payload on a page boundary - so the padding is invisible in the central
    directory and has to be read from the local header. Measuring `header_offset % 16384` instead
    would fail a correctly aligned APK, which is the opposite error and just as misleading.
    """
    with open(artifact, "rb") as handle:
        handle.seek(info.header_offset)
        header = handle.read(30)
    if len(header) != 30:
        raise ValueError("truncated local file header")
    name_length, extra_length = struct.unpack_from("<HH", header, 26)
    return info.header_offset + 30 + name_length + extra_length


def inspect(artifact: Path) -> tuple[list[dict], list[str]]:
    rows: list[dict] = []
    failures: list[str] = []
    suffix = artifact.suffix.lower()
    with zipfile.ZipFile(artifact) as archive:
        for info in archive.infolist():
            name = info.filename
            if not is_native_library_entry(name):
                continue
            blob = archive.read(name)
            row = {"entry": name, "bytes": len(blob)}
            try:
                aligns = elf_load_alignments(blob)
            except ValueError as error:
                row["error"] = str(error)
                failures.append(f"{name}: {error}")
                rows.append(row)
                continue
            row["load_alignments"] = aligns
            row["min_align"] = min(aligns) if aligns else None
            row["elf_16k_ok"] = bool(aligns) and min(aligns) >= PAGE_16K
            if not row["elf_16k_ok"]:
                failures.append(f"{name}: min PT_LOAD p_align={row['min_align']} < {PAGE_16K}")
            # Only an APK is loaded directly; an AAB is repackaged by Play.
            if suffix == ".apk":
                row["stored"] = info.compress_type == zipfile.ZIP_STORED
                data_offset = stored_data_offset(artifact, info)
                row["data_offset"] = data_offset
                row["offset_aligned"] = data_offset % PAGE_16K == 0
                if not row["stored"]:
                    failures.append(f"{name}: compressed in the APK, so it cannot be page-aligned")
                elif not row["offset_aligned"]:
                    failures.append(
                        f"{name}: stored at data offset {data_offset} (0x{data_offset:X}), "
                        f"not a multiple of {PAGE_16K}")
            rows.append(row)
    return rows, failures


def report(artifact: Path) -> list[str]:
    print(f"\n{artifact}")
    if not artifact.exists():
        print("  BLOCKED  artifact does not exist")
        return [f"{artifact}: missing"]
    rows, failures = inspect(artifact)
    if not rows:
        print("  PASS     no native libraries in this artifact")
        return []
    for row in rows:
        if "error" in row:
            print(f"  FAIL     {row['entry']}: {row['error']}")
            continue
        verdict = "PASS" if row["elf_16k_ok"] else "FAIL"
        detail = f"min p_align=0x{row['min_align']:X} ({row['min_align']})"
        if "stored" in row:
            detail += (f", container={'stored' if row['stored'] else 'COMPRESSED'}"
                       f" at data offset 0x{row['data_offset']:X}"
                       f"{' aligned' if row['offset_aligned'] else ' MISALIGNED'}")
        print(f"  {verdict:8} {row['entry']}: {row['bytes']} bytes, {detail}")
    if not failures:
        print(f"  PASS     16 KB ready: {len(rows)} native librar"
              f"{'y' if len(rows) == 1 else 'ies'}")
    return failures


def selftest() -> int:
    """Prove the sensor can fail, before trusting that it passes."""
    failures = 0

    def elf_with_load_align(align: int, extra_loads: list[int] | None = None) -> bytes:
        """A minimal but structurally valid 64-bit ELF whose PT_LOAD p_align values are as asked."""
        aligns = [align] + list(extra_loads or [])
        blob = bytearray(64 + 56 * len(aligns))
        blob[0:4] = ELF_MAGIC
        blob[4] = 2          # 64-bit
        blob[5] = 1          # little-endian
        struct.pack_into("<Q", blob, 0x20, 64)              # e_phoff
        struct.pack_into("<H", blob, 0x36, 56)              # e_phentsize
        struct.pack_into("<H", blob, 0x38, len(aligns))     # e_phnum
        for index, value in enumerate(aligns):
            base = 64 + index * 56
            struct.pack_into("<I", blob, base, 1)           # p_type = PT_LOAD
            struct.pack_into("<Q", blob, base + 0x30, value)
        return bytes(blob)

    # The false pass that motivated this rewrite: the APK path shape must be recognised. Before
    # this check existed, `"/lib/" in name` matched the AAB's `base/lib/...` and matched none of the
    # APK's `lib/...`, and the tool reported the APK as shipping no native libraries.
    cases = {
        "lib/arm64-v8a/libx.so": True,
        "base/lib/arm64-v8a/libx.so": True,
        "lib/x86_64/libdatastore_shared_counter.so": True,
        "res/raw/libx.so": False,
        "assets/jni/libx.so": False,
        "lib/arm64-v8a/libx.so.txt": False,
        "classes.dex": False,
    }
    wrong = {name: got for name, want in cases.items()
             if (got := is_native_library_entry(name)) != want}
    ok = not wrong
    print(f"{'PASS' if ok else 'FAIL'}     selftest: recognises both container path shapes"
          f"{'' if ok else ' -> ' + str(wrong)}")
    failures += 0 if ok else 1

    # The ELF half of the check, on its own.
    aligns = elf_load_alignments(elf_with_load_align(4096))
    ok = aligns == [4096] and min(aligns) < PAGE_16K
    print(f"{'PASS' if ok else 'FAIL'}     selftest: a 4 KB PT_LOAD is judged NOT 16 KB ready "
          f"(read {aligns})")
    failures += 0 if ok else 1
    aligns = elf_load_alignments(elf_with_load_align(16384, extra_loads=[16384]))
    ok = bool(aligns) and min(aligns) >= PAGE_16K
    print(f"{'PASS' if ok else 'FAIL'}     selftest: a 16 KB PT_LOAD is judged ready (read {aligns})")
    failures += 0 if ok else 1
    # One 4 KB segment among 16 KB ones is enough to make the library unloadable on a 16 KB kernel,
    # so the check must look at every segment rather than the first.
    aligns = elf_load_alignments(elf_with_load_align(16384, extra_loads=[4096]))
    ok = min(aligns) < PAGE_16K
    print(f"{'PASS' if ok else 'FAIL'}     selftest: a later 4 KB segment is still caught "
          f"(read {aligns})")
    failures += 0 if ok else 1

    # The container half, isolated: a valid 16 KB ELF stored at whatever offset a plain archive
    # writer chooses. The ELF passes and the alignment does not, so the only complaint the run may
    # produce is about the offset. That is what makes this check discriminate rather than merely
    # reject: if the alignment branch were dead, the expected failure would not appear.
    with tempfile.TemporaryDirectory(prefix="pk-check16k-") as tmp:
        archive_path = Path(tmp) / "probe.apk"
        with zipfile.ZipFile(archive_path, "w", zipfile.ZIP_STORED) as archive:
            archive.writestr("lib/arm64-v8a/libprobe.so", elf_with_load_align(16384))
        with zipfile.ZipFile(archive_path) as archive:
            info = archive.getinfo("lib/arm64-v8a/libprobe.so")
            offset = stored_data_offset(archive_path, info)
            expected = info.header_offset + 30 + len(info.filename.encode()) + len(info.extra)
            ok = offset == expected
            print(f"{'PASS' if ok else 'FAIL'}     selftest: data offset read from the local header "
                  f"(header=0x{info.header_offset:X} data=0x{offset:X})")
            failures += 0 if ok else 1
        rows, problems = inspect(archive_path)
        ok = (len(rows) == 1 and rows[0]["elf_16k_ok"] and not rows[0]["offset_aligned"]
              and len(problems) == 1 and "not a multiple of" in problems[0])
        print(f"{'PASS' if ok else 'FAIL'}     selftest: a valid ELF at a misaligned offset is "
              f"rejected for the offset alone -> {problems}")
        failures += 0 if ok else 1

    print(f"\nVERDICT: {'FAIL' if failures else 'PASS'}")
    return 1 if failures else 0


def main(argv: list[str]) -> int:
    if len(argv) == 1 or argv[1] == "--selftest":
        return selftest()
    all_failures: list[str] = []
    for argument in argv[1:]:
        all_failures += report(Path(argument))
    print()
    if all_failures:
        print(f"VERDICT: FAIL — {len(all_failures)} problem(s)")
        for problem in all_failures:
            print(f"  - {problem}")
        return 1
    print("VERDICT: PASS — every native library is 16 KB ready")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
