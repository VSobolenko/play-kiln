#!/usr/bin/env python3
"""Regression test for the two slot assumptions `scripts/visual-qa.py` used to bake in.

`visual-qa.py` is the tool `.cursor/rules/visual-identity.mdc` names as the independent
measurement behind "the palette in the pack is the palette in the app". Both of its reads of a
slot were written against the first slot literally:

* the theme directory was `app/src/main/java/com/gexetr/pipesizing/ui/theme`, so the palette
  section died with a `FileNotFoundError` for every other slot - the gate the rule tells an
  operator to run could not be run at all.
* the Kotlin palette regex required `internal val`, which is how the first slot declared its
  colours. A slot that groups them in one named palette object with plain `val` - the shape the
  rule asks for - matched nothing, which emptied the table and crashed the report on
  `NoneType is not subscriptable` while printing it.

Neither defect was visible by reading the rule that recommends the tool, and both appeared the
first time the tool was pointed at a second slot. So this test points it at a synthetic slot whose
package and colour declarations are deliberately unlike the reference slot's, and at the reference
slot's own declaration style, because a regex widened to fix the first must not stop matching the
second.

    python scripts/test-visual-qa-discovery.py     # exits 0 when every expectation holds
"""

from __future__ import annotations

import importlib.util
import pathlib
import sys
import tempfile

HERE = pathlib.Path(__file__).resolve().parent
VISUAL_QA = pathlib.Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else HERE / "visual-qa.py"

failures: list[str] = []


def check(ok: bool, message: str) -> None:
    print(f"{'PASS' if ok else 'FAIL'}     {message}")
    if not ok:
        failures.append(message)


def load_visual_qa():
    spec = importlib.util.spec_from_file_location("playkiln_visual_qa", VISUAL_QA)
    if spec is None or spec.loader is None:
        raise SystemExit(f"BLOCKED: cannot load {VISUAL_QA}")
    module = importlib.util.module_from_spec(spec)
    sys.modules["playkiln_visual_qa"] = module
    spec.loader.exec_module(module)
    return module


def write_palette(slot: pathlib.Path, package: str, source_root: str = "java") -> pathlib.Path:
    """A Color.kt shaped like the reference slot's second revision, not its first."""
    theme = slot / "app" / "src" / "main" / source_root
    for part in package.split("."):
        theme = theme / part
    theme = theme / "ui" / "theme"
    theme.mkdir(parents=True, exist_ok=True)
    (theme / "Color.kt").write_text(
        f"package {package}.ui.theme\n"
        "\n"
        "import androidx.compose.ui.graphics.Color\n"
        "\n"
        "internal object Chalk {\n"
        "    val Indigo = Color(0xFF1E1B4B)\n"
        "    val Amber = Color(0xFFD97706)\n"
        "}\n",
        encoding="utf-8",
    )
    return theme


def main() -> int:
    module = load_visual_qa()

    # Named first, so a version that has gone back to hardcoding a slot path reports a readable
    # FAIL row here instead of an AttributeError traceback from the first use below.
    missing = [name for name in ("theme_dir", "KOTLIN_COLOR") if not hasattr(module, name)]
    if missing:
        check(False, f"visual-qa.py exposes {', '.join(missing)} for this test to drive")
        print(f"\nVERDICT: {'FAIL' if failures else 'PASS'}")
        return 1

    with tempfile.TemporaryDirectory(prefix="pk-visual-qa-discovery-") as tmp:
        slot = pathlib.Path(tmp)
        expected = write_palette(slot, "com.gexetr.zetaslot")
        check(
            module.theme_dir(slot) == expected,
            "theme_dir finds a slot whose package is not com.gexetr.pipesizing",
        )

        names = [
            match.group(1)
            for match in module.KOTLIN_COLOR.finditer(
                (expected / "Color.kt").read_text(encoding="utf-8")
            )
        ]
        check(
            names == ["Indigo", "Amber"],
            "the palette regex reads plain `val`s declared inside a palette object",
        )

        hostile_source = '''val Brand = Color(0xFF112233)
// val Brand = Color(0xFFFF0000)
val Text = "val Ghost = Color(0xFF00FF00)"
val Raw = """val RawGhost = Color(0xFFABCDEF)"""
val Character = '/'
/* val BlockGhost = Color(0xFF010203) */
'''
        parsed = [(match.group(1), match.group(2)) for match in module.KOTLIN_COLOR.finditer(hostile_source)]
        check(
            parsed == [("Brand", "FF112233")],
            "palette discovery ignores declarations in comments and string literals",
        )
        unsupported_source = hostile_source + (
            "val Wrapped = Color(0xFF445566.toInt())\n"
            "var Mutable = Color(0xFF778899)\n"
            "val Six = Color(0x112233)\n"
            "val Unsigned = Color(0xFFAABBCCu)\n"
        )
        if hasattr(module, "unparsed_kotlin_color_declarations"):
            unreadable = module.unparsed_kotlin_color_declarations(unsupported_source)
            check(
                [item["declaration"].split("=")[0].strip() for item in unreadable]
                == ["val Wrapped", "var Mutable", "val Six", "val Unsigned"],
                "unsupported named Color declarations are reported instead of silently omitted",
            )
        else:
            check(False, "unsupported named Color declarations are reported instead of silently omitted")

        write_palette(slot, "com.gexetr.other")
        try:
            module.theme_dir(slot)
        except SystemExit:
            check(True, "two candidate themes fail closed instead of grading an arbitrary one")
        else:
            check(False, "two candidate themes fail closed instead of grading an arbitrary one")

    with tempfile.TemporaryDirectory(prefix="pk-visual-qa-empty-") as tmp:
        try:
            module.theme_dir(pathlib.Path(tmp))
        except SystemExit as error:
            check(
                "Color.kt" in str(error),
                "a slot with no Color.kt fails closed and names the file it wanted",
            )
        else:
            check(False, "a slot with no Color.kt fails closed and names the file it wanted")

    with tempfile.TemporaryDirectory(prefix="pk-visual-qa-kotlin-") as tmp:
        slot = pathlib.Path(tmp)
        expected = write_palette(slot, "com.example.kotlinroot", "kotlin")
        try:
            kotlin_result = module.theme_dir(slot)
        except SystemExit:
            check(False, "theme_dir discovers Color.kt under src/main/kotlin")
        else:
            check(
                kotlin_result == expected,
                "theme_dir discovers Color.kt under src/main/kotlin",
            )

        java_theme = write_palette(slot, "com.example.javaroot", "java")
        try:
            module.theme_dir(slot)
        except SystemExit as error:
            message = str(error)
            check(
                expected.relative_to(slot).as_posix() + "/Color.kt" in message
                and java_theme.relative_to(slot).as_posix() + "/Color.kt" in message,
                "java/kotlin ambiguity fails closed and names every candidate",
            )
        else:
            check(False, "java/kotlin ambiguity fails closed and names every candidate")

    reference_style = (
        "internal val Slate90 = Color(0xFFFAFAFA)\n"
        "private val LocalAccent = Color(0x80112233)\n"
    )
    names = [match.group(1) for match in module.KOTLIN_COLOR.finditer(reference_style)]
    check(
        names == ["Slate90", "LocalAccent"],
        "the widened regex still reads the reference slot's internal and private vals",
    )

    print(f"\nVERDICT: {'FAIL' if failures else 'PASS'}")
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
