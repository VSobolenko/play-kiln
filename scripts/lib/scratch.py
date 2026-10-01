"""Where a Python factory process is allowed to write temporary files.

The rule is `.cursor/rules/workspace-hygiene.mdc`, and this module is the Python half of
`scripts/lib/scratch.mjs`: a factory process may make as much mess as it likes **inside** this
repository and must write nothing outside it. The machine's system temp directory - the default
parent of every `tempfile` scratch API - is outside it, so every Python call that needs a scratch
directory passes `dir=SCRATCH_DIR`.

The owner's instruction that paid for this, recorded 2026-10-01: a Gradle build filled his system
temp directory and his machine took 30 seconds to five minutes to become usable.

Deleting everything under `.kiln/scratch/` at any moment, including mid-run, is always safe.
"""

from __future__ import annotations

import tempfile
from pathlib import Path

#: Repository root, derived from this file's own location (`scripts/lib/scratch.py`), so the
#: directory cannot drift to something that resolves outside the repository.
ROOT = Path(__file__).resolve().parent.parent.parent

#: The one directory a factory process may treat as scratch. Gitignored with all of `.kiln/`.
SCRATCH_DIR = ROOT / ".kiln" / "scratch"

# Created at import time, so `SCRATCH_DIR` is always a usable `dir=` for either tempfile API. This is
# the module's only side effect, and it is deliberate: a scratch directory that might not exist is a
# scratch directory a caller skips in favour of the default.
SCRATCH_DIR.mkdir(parents=True, exist_ok=True)


def scratch_dir(prefix: str) -> Path:
    """A new, empty directory under the project scratch, named with `prefix`.

    Equivalent to `Path(tempfile.mkdtemp(prefix=prefix, dir=SCRATCH_DIR))`; prefer passing
    `dir=SCRATCH_DIR` directly where the call already reads clearly.
    """
    return Path(tempfile.mkdtemp(prefix=prefix, dir=SCRATCH_DIR))
