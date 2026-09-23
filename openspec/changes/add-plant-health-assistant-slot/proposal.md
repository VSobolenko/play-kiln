# Add a plant health assistant slot

> **ABANDONED 2026-09-23 - do not implement.** This change was written and validated, then
> killed by its own required evidence. The differentiator it depends on ("how long until the
> crop is safe to eat") **asserts a safety verdict that a pre-harvest interval cannot support**,
> because an interval is a legal use restriction, not a residue measurement - a defect no
> dataset can repair. The market for the phrasing is also unproven, and the only public interval
> datasets are national (France, Germany). Full reasoning, with the kill criteria and the
> verified sources, is recorded in the *Recorded decisions* section of `tasks.md` and in
> `docs/research/2026-09-23-harvest-interval-differentiator.md` and
> `docs/research/2026-09-23-on-device-plant-diagnosis.md`.
>
> The factory re-targeted to a `visual schedule autism` slot instead. This file is retained as
> an evidence record only.

## Why

`plant disease identifier` is a proven-demand niche with a **consumer-scale owner**, and
PlayKiln has never shipped a photo-input diagnosis tool. The ratings-only screen in
`docs/market-scan-2026-09-23.md` returned `mixed` for this query (9/10 visible apps rated,
4 weak incumbents, median 4.20, ranked #3 of the scored candidates) — but that screen reads
ratings only and **cannot see install magnitude**. `docs/market-enrichment-2026-09-23.md`
enriched the top 8 apps and supplies the missing fact: a single app sits a whole install
bucket above the rest. This proposal records that picture without softening it and puts the
entry decision in front of a human instead of inventing a differentiation angle.

## App idea, target user, non-goals

**Idea.** A plant health assistant: the user photographs a leaf or stem, answers a short
structured set of questions about the plant, and receives a **diagnosis record** — the likely
disease, pest, or nutrient deficiency, the visual cues it was read from, a confidence band,
and a **treatment plan with concrete, ordered steps**.

**Target user.** Edible-garden growers — someone with tomatoes, cucumbers or peppers in a bed,
a pot or a greenhouse — whose crop is sick *now* and who wants a specific next action and a
food-safety answer, not a conversation. *(Recorded 2026-09-23; the earlier draft said
"indoor-plant owners", which no longer matches the chosen angle. See O1 in `tasks.md`.)*

**Explicitly not.** Not a chatbot and not a thin AI wrapper — `AGENTS.md` ("Defaults to
suppress": *a thin AI chat wrapper → a differentiated workflow with required content
reporting*). The diagnosis is structured data the user fills in and the app reports back;
there is no free-form chat surface. Not a plant *identifier* for healthy plants (the
incumbent sells that). Not a social, marketplace, or encyclopedia app. Not a
general-purpose plant-care reminder.

**Slot identity.** slug `plant-health-assistant`;
applicationId `com.gexetr.planthealthassistant` (derived by `applicationIdForSlug` in
`scripts/lib/namespaces.mjs`, which strips dashes); slot directory
`apps/2026-09-23-plant-health-assistant/`.

## The competitive picture this proposal must confront

Every figure below is attributed; none is invented.

**One clear owner.** Per `docs/market-enrichment-2026-09-23.md`, the query
`plant disease identifier` is owned by `cn.danatech.xingseus` — *PictureThis - Plant
Identifier*, Glority Global Group Ltd., **50M+ install bucket, 4.5★, 800,000 ratings
(799,516 exact), in-app purchases declared, ads not declared**. The same document records
two more apps at the **10M+** bucket (`com.scaleup.plantid`, ScaleUp, 4.1★, 548,000 ratings;
`com.myplantin.app`, PlantIn, 4.0★, 136,000 ratings), and describes the 50M+ bucket as
"a whole bucket above the next tier". The enrichment explicitly states the ratings screen's
`mixed` verdict understated the situation because it cannot see install magnitude.

**The weak pocket is real but small.** The same source identifies the reachable weak
incumbents as `com.plant.disease.identifier` (AppCraftingMinds, **3.0★, 16 ratings, 5K+
bucket**) and `com.ai.identifier.plant` (Brahim Chouih, **unrated, 1K+ bucket**). Everything
else enriched sits between 10K+ and 500K+. `com.agrio` (Agrio, Saillog Ltd., 4.2★, 4,800
ratings, 500K+) is the strongest mid-tier app.

**Demand and willingness-to-pay are both proven.** Of the 8 enriched apps, **all 8 declare
in-app purchases** and only 3 declare ads (`docs/market-enrichment-2026-09-23.md`). Across
all 40 enriched apps in that run, 27/40 declare IAP against 9/40 declaring ads, and all 40
are free.

**What this does not prove.** The same document states plainly that install counts are Play's
rounded buckets, that revenue/retention/conversion are not observable from a store page, and
that it cannot tell us **whether users would pay for *this* app**. Treating the incumbent's
size as a revenue forecast would contradict the source.

**Consequence for this proposal.** Demand is proven and the niche is owned by a
consumer-scale incumbent that already monetises with IAP. Therefore the entry decision rests
on a **differentiation angle** and a **monetization archetype**, and both are human
decisions, not agent inventions (see *Open questions* below and `design.md`).

## What Changes

- Create **one** new isolated slot, `apps/2026-09-23-plant-health-assistant/`, via
  `scripts/new-slot.mjs` — Kotlin, Jetpack Compose, Material 3, Gradle KTS, per
  `AGENTS.md` and `openspec/specs/android-compose-baseline/spec.md`.
- Add a new product capability spec, `plant-health-assistant`, describing the observable
  behavior: capture → structured symptom intake → diagnosis record → treatment plan.
- Consume only the `capabilities/` modules the slot needs, pinned by version, and derive the
  Data safety answers from their `capability.json` files.
- Follow the Photo Picker preference from `AGENTS.md` for photo input.
- Produce the full store pack and a signed release, gated by the commands in `AGENTS.md`.

**BREAKING:** none. This is an additive slot; no existing slot, capability, or spec changes
behavior.

## Capabilities

### New Capabilities

- `plant-health-assistant`: the slot's observable product behavior — photo-first symptom
  capture, a structured diagnosis record with an advisory confidence band, and a concrete
  treatment plan, with the privacy and accuracy-claim boundaries that behavior must respect.

### Modified Capabilities

<!-- None. The factory specs (android-compose-baseline, play-compliance,
     factory-slot-isolation, capabilities) already cover this slot's obligations, and this
     change introduces no new factory-wide requirement. App-specific compliance surface is
     specified inside the new `plant-health-assistant` capability so the factory specs stay
     unchanged. -->

- None.

## Open questions (human decisions required before implementation)

These are decisions, not gaps to be filled by an agent. They are repeated in `design.md`
with the evidence each option would need.

1. **O1 — Differentiation angle.** A vertical angle (a specific crop, growing context, or
   workflow the incumbent serves generically) is **one candidate family**, stated as an
   option and not a conclusion. Without a distinctness sentence Play's repetitive-content
   policy and `AGENTS.md` both block the slot (`docs/monetization-strategy.md` §1.2,
   `docs/niche-selection.md` §2 step 6).
2. **O2 — Diagnostic engine path.** On-device/deterministic **or** hosted model. Hard
   constraint: **a hosted API key cannot ship inside an APK**, so a hosted path requires a
   server component and is a different project shape than this factory has built so far.
3. **O3 — Monetization archetype.** One-time unlock, repeatable consumable packs
   (`Fulfillment.LOCAL` vs `SERVER_COST`), subscription, or ads
   (`docs/monetization-strategy.md` §3, §3.6, §3.7). The archetype decides whether the slot
   is an app or a server project.

## Impact

- **New:** `apps/2026-09-23-plant-health-assistant/` (Kotlin/Compose slot), its store pack,
  and one `registry/apps.json` entry.
- **Consumes (unchanged):** `com.gexetr.capability:core` (required by every slot) and, subject
  to O3, `ads`, `billing`, `remoteconfig`, `telemetry` — each pinned by version and always by
  published artifact, never by source.
- **Unchanged:** `capabilities/**`, `scripts/**`, `scaffold/**`, and every existing slot.
- **Policy surface:** minimized photo/camera permissions, backup rules, edge-to-edge and
  predictive back, an advisory-accuracy boundary, and truthful Data safety / AI-content
  declarations.
