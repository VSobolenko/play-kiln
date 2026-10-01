"""The limit vocabulary the renderer draws against, resolved from the pack model.

Every window, byte cap and character limit below is *called* from ``scripts/store/model.py``, which
loads ``scripts/lib/play-assets.json``; not one of them is a literal here, so the renderer and the
pack's verdict (``scripts/store/validate.py``) cannot measure different numbers. This module carries
the model's release-note helpers and the placeholder-host list for the same reason.

It also holds the import bootstrap -- the model is a file beside this one, not an installed package --
so the sibling modules take ``pack_model`` and the constants from here instead of each repeating
``sys.path`` surgery.
"""

from __future__ import annotations

import sys
from pathlib import Path



ROOT = Path(__file__).resolve().parents[2]


APPS = ROOT / "apps"



# The model is a file next to this one rather than an installed package, so its directory is added to
# `sys.path` exactly the way `scripts/store/validate.py` adds it.
sys.path.insert(0, str(ROOT / "scripts" / "store"))


import model as pack_model  # noqa: E402  (scripts/store/model.py, the pack's only limit resolver)



# Every Play limit this program renders against, resolved by the pack's own data model.  The names
# are kept because `scripts/visual-qa.py` and `scripts/store-studio.py` read the inventory through
# them; not one of them is a literal here.
SUPPORTED_LOCALES: tuple[str, ...] = pack_model.supported_locales()


SCREEN_TARGETS: dict[str, tuple[int, tuple[int, int]]] = pack_model.screenshot_targets()


SCREENSHOT_LIMITS: dict[str, dict[str, int | None]] = pack_model.screenshot_windows()


SCREENSHOT_ASPECT_RATIOS: tuple[tuple[int, int], ...] = pack_model.aspect_ratios()


MAX_PNG_BYTES: int = pack_model.screenshot_max_bytes()


MAX_ICON_BYTES: int = pack_model.icon_max_bytes()


MAX_FEATURE_BYTES: int = pack_model.feature_graphic_max_bytes()


ICON_CANVAS: tuple[int, int] = pack_model.icon_canvas()


FEATURE_GRAPHIC_CANVAS: tuple[int, int] = pack_model.feature_graphic_canvas()


MAX_RELEASE_NOTE_CHARS: int = pack_model.release_note_limit()


DEFAULT_CONTACT_EMAIL: str = pack_model.DEFAULT_CONTACT_EMAIL


PLACEHOLDER_HOSTS: tuple[str, ...] = pack_model.placeholder_hosts()



# The canvas rules the renderer draws against, read from the model so a limit has one definition.
aspect_ratio_failures = pack_model.aspect_ratio_failures


screenshot_rule_failures = pack_model.screenshot_rule_failures
