# Proposal: app-coverkit

**Product:** Coverkit: Social Studio — `com.gexetr.coverkit`  
**Contracts (pre-scaffold):** `docs/ideation/plans/coverkit.{economics,differentiators,naming}.json`  
**Evidence:** `docs/research/2026-10-21-social-cover-studio-shelf.md`  
**Competitor assembly (normative UI/features):** `docs/research/2026-10-21-coverkit-competitor-matrix.md`  
**Owner approval:** checkpoint 1, 2026-10-21 — build Coverkit (multi-network covers, no AI keys).

## Why

Creators already pay for YouTube thumbnail makers (5M–10M+ installs, ads+IAP) and Instagram story makers (10M+ each), but they bounce between apps and hit the same failures: **subscription to save**, **blurry export**, **lost projects**, **one network per app**. Coverkit closes one need — **a cover/post ready for the network they chose** — with a guided first path and a deeper studio for those who stay. No AI provider keys; no account; one-time unlock.

## What Changes

- New PlayKiln slot `apps/<date>-coverkit/` with Kotlin/Compose app.
- Capability **social-cover-studio**: happy-path cover creation, layered editor copied from working makers, platform size pack, brand kit, durable projects, share/export, one-time billing unlock.
- Factory contracts: economics `purchase_led_single_unlock` at $9.99, differentiators (4 promises, all start `deferred`), naming Coverkit.
- **No** AI generation, **no** subscription, **no** server, **no** inventing novel UI chrome — assemble ABSORB rows from the competitor matrix only; REFUSE/DEFER rows need a new OpenSpec delta + owner words.

## Capabilities

### New Capabilities

- `social-cover-studio`: End-to-end social cover/post studio — attention-guided happy path, platform sizes, templates, layers, brand kit, multi-export, unlock.

### Modified Capabilities

- (none)

## Impact

- New isolated Android slot only; harness unchanged.
- Dependencies: Compose canvas/graphics, Photo Picker, billing capability, DataStore for brand kit/projects/settings — no paid AI SDKs, no client secrets.
- Device testing on A40 (`R58MB3SGZVL`) before checkpoint 1.5.
