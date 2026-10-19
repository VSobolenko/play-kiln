# W87 вЂ” Studio rewrite (owner rejection 2026-10-17)

**Status:** owner-approved and implemented; see the W87h closeout and subsequent W90/W91 follow-ups.

## What the owner said (source of truth)

The current Studio is unusable. Incremental fixes did not change the product
shape. Rewrite Studio end-to-end.

Measured complaints (paraphrase of owner words, not inventing new goals):

1. Expected flow: open Studio в†’ photo is chosen в†’ choose a **tool** from a
   clear catalogue в†’ each tool explains what it does в†’ run it.
2. Tool layout is broken: one full-width button, controls scattered, chrome
   that blocks reading (labels wrapping onto three lines).
3. After a photo is already selected, tools still ask to **choose photo again**.
4. Huge photo preview eats half the tool screen; client-facing **IDs / job
   ids** appear; a **refresh cost** control is nonsense.
5. **Generated scene / AI background:** user wants free description
   (вЂњseaвЂќ, вЂњforest for a tentвЂќ), not locked **Scene 1 / 2 / 3** with only
   two usable options. вЂњAI backgroundвЂќ should mean generate surroundings,
   not pick opaque scene tokens.
6. Opening Relight / Upscale / Cutout still shows вЂњStudio toolsвЂќ,
   вЂњreturn to StudioвЂќ, prior generations, and re-pick photo вЂ” orientation
   is lost.
7. вЂњOther ways to finish the jobвЂќ at the bottom is unexplained clutter.
8. Landing hard-sells **AI background** as if that were the only job.
9. Listing / marketplace packaging (Amazon / Wildberries / Ozon) does not
   belong inside generative Studio tools вЂ” sellers generate images freely;
   marketplace slide kits stay out of this rewriteвЂ™s Studio path.

## Competitor patterns we already measured (W71)

- Product photo first, then direction, then Generate/Create with cost
  shown before commit (Pebblely, Photoroom, Claid, Product Plus).
- Scene direction is prompt and/or meaningful templates вЂ” not fake padding
  and not opaque вЂњScene NвЂќ as the only path.

## Approaches considered

| # | Approach | Verdict |
|---|---|---|
| A | Patch `StudioRoute` in place (enablement, strings, chip) | **Rejected by owner** вЂ” вЂњРЅРµС‡РµРіРѕ РёСЃРїСЂР°РІР»СЏС‚СЊвЂќ |
| B | Replace Studio UX shell: compact photo в†’ 2-column tool grid в†’ dedicated tool screen (photo locked, freeform where needed, Generate+cost only) | **Recommended** |
| C | Navigate each tool as its own top-level destination from the app shell | Deferred вЂ” more nav churn; B can grow into C later |

## Recommended product contract (B)

### Screen 1 вЂ” Studio home (after optional import)

- Compact selected photo strip (thumbnail + change/replace). Not half the screen.
- **2-column tool grid** (equal cards): icon + short title + one-line purpose + credit cost.
- No primary tool forced full-bleed. No вЂњother ways to finishвЂќ footer.
- Default highlight is neutral (вЂњYour photoвЂќ) вЂ” not вЂњAI backgroundвЂќ as the hero.

### Screen 2 вЂ” Inside one tool

- Title = tool name only (e.g. Relight). No second вЂњStudio toolsвЂќ catalogue.
- Photo is **already bound** вЂ” show small chip вЂњUsing this photoвЂќ + Change.
  Never a mandatory full photo picker.
- Controls that belong to the tool only:
  - Cutout: refine / generate вЂ” no IDs, no refresh-cost.
  - Scene / AI background: **large freeform prompt** first (вЂњforest clearing at duskвЂ¦вЂќ).
    Optional suggestion chips are examples that fill the prompt, not exclusive Scene 1/2/3.
  - Relight / Recolour / Outpaint / Upscale: short instruction field if needed; otherwise Generate.
- Primary CTA: **Generate В· N credits** (or local Cutout equivalent). Nothing else competing.
- Prior results: link to Profile/Library, not a stack that looks like a second photo picker.

### Out of Studio scope (this rewrite)

- Marketplace slide kit / AmazonВ·WBВ·Ozon listing chrome: do not surface inside
  Studio tool forms. Keep under its own destination if it remains in the app.
- Server fal prompts: keep W73 truthfulness; extend scene prompt to pass the
  userвЂ™s freeform text as the direction (no invented Scene-N-only API).

### Success criteria (device)

1. Photo selected once on Studio home; opening any tool does not demand re-pick.
2. Tool grid readable at a glance; no full-screen single Choose button.
3. Scene tool accepts вЂњРЅР° С„РѕРЅРµ РјРѕСЂСЏ / Р»РµСЃР°вЂќ freeform and does not force Scene 1/2/3.
4. No UUID / job id in Studio chrome.
5. No вЂњrefresh costвЂќ control; cost only on Generate.
6. Inside a tool, chrome is that tool вЂ” not the whole Studio catalogue.

## Implementation partition (after owner yes)

| Worker | Owns (disjoint) |
|---|---|
| W87a | `StudioRoute.kt` shell: home grid + navigation into tool panel; kill footer clutter |
| W87b | Tool panel / `StudioWorkflowPanel`: locked photo chip, Generate+cost, hide IDs/refresh |
| W87c | Scene tool: freeform prompt primary; suggestion chips fill prompt; strings en+ru |
| W87d | Remove marketplace / slide packaging from Studio surfaces; string cleanup for tool titles |
| W87e | Unit/UI tests for photo-once + grid + scene freeform; device pass when green |

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Owner rejected current Studio as unusable and ordered a full rewrite | ON RECORD | Owner message 2026-10-17 (~21:26 +03) |
| Live Studio is `StudioRoute` (~685 lines); `StudioToolsScreen` is superseded | MEASURED | `StudioToolsScreen.kt:3`; `StudioRoute.kt` line count |
| Live catalogue rows currently Scene / Relight / Recolour | MEASURED | `StudioRoute.kt` `studioRows` |
| Competitors document photo в†’ direction в†’ Create with cost | ON RECORD | `docs/redesign/competitor-generative-ux-w71.md` |
