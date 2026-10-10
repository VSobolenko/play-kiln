# Store pack — Lumintra: Light Planner

`store/listing.json` is the single source of truth for this pack's copy and declarations. Every other
document here either explains a decision, records an answer for the console, or is the pack the release
render produces from it. If a human-readable document and `listing.json` ever disagree, `listing.json`
wins.

**Slot:** `apps/2026-10-09-lumintra-light-planner/`
**App:** Lumintra: Light Planner · `com.gexetr.lumintralightplanner`
**Model:** paid once, **$9.99**, no in-app product, no ads, no subscription (`purchase_only_no_ads`)
**Job:** an offline-first planner that joins the dark window, twilight boundaries, the Moon, ~250
deep-sky targets, satellite passes and an hourly cloud forecast into one brief, a scrubbable night
scene, and a share handoff.

## What each file is

| File | Role |
|---|---|
| `listing.json` | **The source of truth.** Identity, both locales' copy, data-safety declaration, permissions, ads/content-rating answers, `graphicsSource`. |
| `listing.md` | Human-readable rendering of `listing.json` (en + ru), with the measured character counts beside each field. Regenerated from the JSON, never a competing source. |
| `data-safety.md` | The Play Console Data safety answers, row by row, with the reason each exists and the rows deliberately absent. |
| `privacy.md` | The public privacy policy text for **this** app: local storage, the two keyless network calls, no accounts, no ads, no analytics, how to delete data, contact. |
| `review-notes.md` | Reviewer access: there is no login gate, the 60-second path to the core value, what the two network calls do, and why no credentials are needed. |
| `release-notes.md` | The release name (`1.0.0 (1)`) and Play's `<locale>`-tagged multi-language paste block (en-US, ru-RU). |
| `graphics.md` | The store asset inventory: exact canvases, counts, formats and caps, plus the fields deliberately **not** produced. |
| `checklist.md` | The console walk for this app: the merged permission list to read, the two first-publication declarations, the in-app review rules, and the placeholder rule. |
| `studio.json` | The app's declared visual identity (mark, palette, per-shot headlines, tagline). **Written by the identity work; not edited here.** The icon, adaptive-icon vectors, feature graphic and store compositions are rendered from it. |

## What the app's declarations say, in one line each

- **No location permission of any kind.** A *spot's* coordinate is a value the user types or picks; it
  is not the device's location. The only permission besides `INTERNET` is `POST_NOTIFICATIONS`, and only
  when the reminder is switched on.
- **Two keyless network calls** — `api.met.no` (MET Norway) for the hourly cloud forecast, which receives
  the saved spot's coordinate **truncated to 4 decimal places (~11 m)**, and **CelesTrak** for orbital
  element sets, which carries **no coordinate**. Everything else computes on the device.
- **One collected row** in Data safety — Location → **Approximate** location, purpose **App
  functionality**, **optional**, **encrypted in transit**. No account, no analytics, no crash reporter,
  no ad SDK. The pack never claims the app collects nothing.

## Deliberately absent

- **Desktop and Android XR screenshots** — the console fields are optional and the pack does not fill
  them (owner's instruction, recorded 2026-09-29). No target, asset or check exists for them.
- **`privacyPolicyUrl` is empty today** — this is the honest unfinished state, not a placeholder. The
  factory hosts the policy and writes the real public HTTPS URL with
  `node scripts/publish-privacy.mjs --slot lumintra-light-planner`. No reserved host
  (`example.com`, `localhost`, …) is ever written here.

## Assets the release render produces

```text
store/assets/icon-512.png
store/assets/feature-graphic-1024x500.png
store/assets/<locale>/icon-512.png
store/assets/<locale>/feature-graphic-1024x500.png
store/assets/<locale>/phone-01.png ... phone-06.png
store/assets/<locale>/tablet-7-01.png ... tablet-7-04.png
store/assets/<locale>/tablet-10-01.png ... tablet-10-04.png
store/provenance.json
```

## How the pack is produced and checked

Phase 1 (now) authors the pack by hand and validates it source-only. Phase 2 — the store render, the
device captures and the signed build — runs **only after the owner's recorded release approval**
(`.cursor/rules/release-gate.mdc`); each of these refuses without it:

```bash
# independent cross-check of the pack on disk (safe in phase 1)
python scripts/store/validate.py --slot lumintra-light-planner

# phase 2, after approval
python scripts/store/render.py --slot lumintra-light-planner
node scripts/media-capture.mjs --slot lumintra-light-planner
```

`scripts/lib/play-assets.json` is the one source of every numeric limit (title 30, short description 80,
full description 4000, release note 500, icon 512×512, feature graphic exactly 1024×500, the three
screenshot sets and their caps); no value is restated in this pack. `store/release-notes.md` carries the
release name as `<versionName> (<versionCode>)` so it agrees with `dist/release-manifest.json`, and the
whole `<locale>` block is pasted into the console's single release-notes box as one piece.
