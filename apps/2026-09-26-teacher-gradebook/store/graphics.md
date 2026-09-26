# Graphics inventory — Mark Book for Teachers

Screenshot source status: **real device captures, composed by `scripts/store-studio.py`**.

| Asset | Dimensions | Format / purpose |
|---|---:|---|
| `assets/icon-512.png` | 512×512 | RGBA Play store icon; full-bleed and opaque, no transparent padding |
| `assets/feature-graphic-1024x500.png` | 1024×500 | RGB feature graphic |
| `assets/<locale>/phone-01..06.png` | 1080×1920 | RGB phone screenshots |
| `assets/<locale>/tablet-7-01..04.png` | 1200×1920 | RGB 7-inch tablet screenshots |
| `assets/<locale>/tablet-10-01..04.png` | 1600×2560 | RGB 10-inch tablet screenshots |

## How these are produced

`scripts/store-studio.py` renders all of them. The screen inside every mock-up is a real capture, taken
by `scripts/media-capture.mjs` from `journey.json` and stored under `captures/raw/`; the studio adds the
branded field, the device bezel, and the headline. It invents no UI and never redraws the captured
screen. Re-run it after changing the palette, or after a new capture:
`python scripts/store-studio.py --slot teacher-gradebook`.

## What build these captures show

`scripts/media-capture.mjs` will not publish unless the installed package is the release artifact. It
walks HEAD -> `dist/release-manifest.json` -> the recorded sha256 of the release APK -> that APK file on
disk -> the installed `base.apk`, and requires every link to match; any missing or mismatched link stops
the run before anything is published or rendered.

The result is recorded in `store/provenance.json`, which is committed and lists the sha256 of each
composed asset above. That record is the durable evidence, because both `captures/` and `dist/` are
excluded from version control: the images here survive in git, their inputs do not. If
`store/provenance.json` is absent, or names a different commit, these images are **not attested to any
build** - re-run `node scripts/media-capture.mjs --slot teacher-gradebook` against a built release to
restore the link.

## Large-screen assets are framed phone captures

The tablet assets are framed from the **phone** captures rather than captured on a tablet device: no
tablet AVD exists on this machine. Play treats large-screen assets as a claim about the large-screen
experience, so the distinction is stated here rather than passed off as a tablet capture.

The tablet sizes are 5:8, which is neither Play's 16:9 nor 9:16 large-screen aspect recommendation.
That is a known factory-size choice, recorded in `AGENTS.md`.

Four screenshots are supplied for both tablet formats. This satisfies the large-format eligibility asset count; eligibility also depends on the app's actual large-screen quality and Play requirements at submission time.
