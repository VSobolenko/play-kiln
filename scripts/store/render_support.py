"""The pack error type and the renderer's small text helpers.

Kept apart from the module that renders so that ``render_listing`` and ``render_documents`` can both use
them without importing each other, and so the error two suites catch by name (``StoreKitError``,
historically ``store-kit.py``'s) has one definition rather than two.
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Any, Callable
from urllib.parse import urlparse

# The sibling modules are files beside this one, not an installed package: the store directory goes
# on `sys.path` before they are imported, the line `scripts/store/validate.py` also uses.
sys.path.insert(0, str(Path(__file__).parent))

from render_limits import PLACEHOLDER_HOSTS




class RenderError(RuntimeError):
    """The pack cannot be written: an input is missing, malformed, or not approved."""




# The pre-split name `store-kit.py` raised. Kept as a compatibility alias for callers written
# against the monolith; nothing in the tree catches it by that name any more.
StoreKitError = RenderError




def _is_placeholder_host(host: str) -> bool:
    host = host.lower().strip(".")
    return not host or any(host == entry or host.endswith(f".{entry}") for entry in PLACEHOLDER_HOSTS)




def is_placeholder_email(value: str) -> bool:
    """True when an address still uses a reserved documentation domain."""
    return _is_placeholder_host(value.rpartition("@")[2])




def is_placeholder_url(value: str) -> bool:
    """True when a URL still points at a reserved documentation host, or is not an http(s) URL."""
    parsed = urlparse(value if "//" in value else f"//{value}")
    return parsed.scheme not in ("http", "https") or _is_placeholder_host(parsed.hostname or "")




def read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except OSError as exc:
        raise RenderError(f"cannot read {path}: {exc}") from exc




def write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")




def match_gradle(text: str, name: str, *, required: bool = True) -> str:
    match = re.search(rf"\b{re.escape(name)}\s*=\s*[\"']([^\"']+)[\"']", text)
    if match:
        return match.group(1).strip()
    if required:
        raise RenderError(f"app/build.gradle.kts is missing {name} = \"...\"")
    return ""




def prose_lines(path: Path) -> list[str]:
    if not path.is_file():
        return []
    lines = []
    fenced = False
    for raw in read_text(path).splitlines():
        line = raw.strip()
        if line.startswith("```"):
            fenced = not fenced
            continue
        if fenced or not line or line.startswith(("#", ">", "-", "*", "|", "<")):
            continue
        clean = re.sub(r"[`*_\[\]]", "", line).strip()
        if clean:
            lines.append(clean)
    return lines




def heading(path: Path) -> str:
    if not path.is_file():
        return ""
    for raw in read_text(path).splitlines():
        match = re.match(r"^#\s+(.+?)\s*$", raw)
        if match:
            return re.sub(r"[`*_]", "", match.group(1)).strip()
    return ""




# --------------------------------------------------------------------------------------------
# Draft listing copy
# --------------------------------------------------------------------------------------------


def truncate_chars(text: str, limit: int) -> str:
    text = re.sub(r"\s+", " ", text).strip()
    if limit <= 0:
        return ""
    if len(text) <= limit:
        return text
    if limit == 1:
        return "…"
    clipped = text[: limit - 1]
    if " " in clipped:
        clipped = clipped.rsplit(" ", 1)[0]
    clipped = clipped.rstrip(" ,.;:-")
    if not clipped:
        clipped = text[: limit - 1]
    return clipped + "…"
