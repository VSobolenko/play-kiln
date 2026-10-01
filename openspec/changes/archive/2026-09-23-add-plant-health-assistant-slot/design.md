# Design — plant health assistant

## Context

See `proposal.md` — *Why* and *The competitive picture*. The facts that shape this design:

- The niche is owned by a consumer-scale incumbent and the reachable weak pocket is small
  (`docs/market-enrichment-2026-09-23.md`). The slot must therefore earn its place through a
  **distinct job**, not through better execution of the incumbent's job.
- The factory has never shipped a photo-input app or a hosted-model app. There is no
  capability module for either; photo handling and the engine seam are slot-local until a
  second slot needs them (`capabilities/README.md`, *When to add a capability*).
- The factory ships **no server component** today, and the scaffold is a single Android
  module (`scaffold/android-compose/`, `openspec/specs/factory-slot-isolation/spec.md`).
- The three decisions in `proposal.md` (*O1/O2/O3*) are open and change the build. This
  design is written so that the **spec is invariant** under all three: the observable
  behavior in `specs/plant-health-assistant/spec.md` does not change whether the engine is
  on-device or hosted, or whether the app sells an unlock or a pack.

## Goals / Non-Goals

**Goals:**

- A single-module Compose app whose user-visible behavior is fully described by the spec and
  testable without a live model.
- An engine seam that lets a deterministic, on-device engine and a hosted engine be swapped
  without touching UI or domain code.
- A photo-input path that adds the smallest possible permission surface.
- Correct, sourced treatment content — not model improvisation — because
  `docs/niche-selection.md` §4 records that *"correctness is the product"*.
- A build that compiles and passes the factory gates with the engine faked.

**Non-Goals:**

- Deciding O1/O2/O3. The design states what each option costs; it does not choose.
- Any server, backend, or credential brokerage in this change. If O2 lands on hosted, that is
  a **separate project shape**, not a task in this task list.
- Plant identification of healthy plants, plant-care reminders, social features.
- Subscriptions (`docs/monetization-strategy.md` §3.3 defers them).

## Decisions

### D1 — Single `:app` module first, UDF, Compose + Material 3

Per `.cursor/skills/android-architecture`, start with one `:app` module: a Compose navigation
host, one `ViewModel` per stage, and unidirectional data flow (events in, `UiState` out).
Grow into `:core:*`/`:feature:*` only if a later OpenSpec change says so.

**Alternatives considered:** a multi-module split up front. Rejected: it slows the factory's
inner loop (`build-speed.mdc`) for an app whose shape is not yet proven.

### D2 — The engine is a seam, and the credential rule is a hard constraint

Define one interface (illustratively `DiagnosisEngine`) whose input is the photo reference
plus the structured intake, and whose output is the diagnosis record + plan + confidence band
+ provenance. Two candidate implementations exist behind it:

| Path | Where it runs | Cost consequence | Compliance consequence |
|---|---|---|---|
| **On-device / deterministic** | Device | No per-request cost; no server; larger APK if a bundled model is used | Data stays on device; simplest Data safety; no credential |
| **Hosted model** | A server we operate | Per-request cost; prices from `docs/monetization-strategy.md` §2.2 | A `SERVER_COST` fulfilment needs a `PurchaseAuthority` and an authoritative balance (§3.7); privacy policy must name the processor |

**Hard constraint stated as a consequence, not a preference:** a hosted provider API key
cannot ship inside an APK — it is extractable and is a credential leak. Therefore a hosted
path requires a server component. That makes it a different project than this factory has
built so far, and it is why O2 is a human decision that gates task group 3.

**Alternatives considered:** an on-device model bundled via a native runtime. Possible, but
any native `.so` triggers the 16 KB-page verification duty in
`openspec/specs/play-compliance/spec.md`, so the exact runtime is **UNVERIFIED** until the
human picks a path.

### D3 — Photo input: system Photo Picker first, no broad media permission

Use the Android Photo Picker (`ActivityResultContracts.PickVisualMedia`) as the default.
`AGENTS.md` *Play compliance floor* prefers the Photo Picker, and it is the **only** way to
read a user-selected image without holding a broad library permission on modern Android. If
the chosen workflow needs in-app capture, use a user-initiated capture and declare only the
camera permission that action needs — and treat that declaration as a reviewable decision,
not a default.

**Rejected:** `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE` as a convenience. It widens the
Data safety story and the permission surface for a photo the system picker can already hand us.

### D4 — Capability consumption and the Data safety obligations it creates

`core` is mandatory for every slot (`capabilities/core/capability.json`:
`optionalForSlots: false`) and collects nothing. Everything else is per-slot and depends on
O3/O2. The obligations below are quoted from the modules' own manifests, which are what the
store pack is generated from (`openspec/specs/capabilities/spec.md`).

| Module | When this slot takes it | Data safety it adds (from `capability.json`) |
|---|---|---|
| `core` | Always | None — `dataCollected: []`, no SDKs, no permissions |
| `telemetry` | Only if opted in | Default logger is `NoOpTelemetry`; keeping it means **"no data collected"** must stay the answer |
| `remoteconfig` | If a flag/experiment surface is wanted | **Device or other IDs** (Firebase Installations instance id), **Location** (coarse country), **App info and performance**; adds `INTERNET`. Firebase Installations is included even without Analytics |
| `billing` | If O3 is an unlock or a pack | **Financial info — App functionality** (purchase history). Payment instruments never reach the app |
| `ads` | Only if O3 lands on ads | **Device or other IDs** (advertising ID), **App activity**, **App info and performance**; adds `com.google.android.gms.permission.AD_ID`; `requiresConsent: true`, so no ad request before the UMP form resolves |

Two consequences that must not be designed away:

- **Declaring the ads module declares ads**, even if no ad is ever shown (`ads/capability.json`
  policy notes). The ads declaration and the Advertising ID declaration must be truthful.
- **A `SERVER_COST` pack without a `PurchaseAuthority` is `NotAuthorized`** and grants nothing
  (`docs/monetization-strategy.md` §3.7): the billing capability deliberately ships no default
  authority. So if O2 = hosted and O3 = money-costing credit, the server is not optional.

**Note on the app's own data.** Photos, intake answers, and diagnosis history are the slot's
own data, stored locally. They are not covered by a capability manifest, so the slot's own
`store/data-safety.md` must be written from the app's storage and send behavior — not copied
from a module.

### D5 — Treatment content is curated, versioned data

The diagnosis→treatment knowledge (cause, cues, steps, cautions, re-check timing) lives as a
**versioned local data file** with a source citation per entry, so that:

- the deterministic engine and a hosted engine return the same plan shape;
- a plan can be corrected without touching UI code;
- the provenance requirement in the spec has something real to record;
- a wrong plan is a data defect with one place to fix, which is how
  `docs/niche-selection.md` §4 says correctness must be handled.

**Rejected:** letting the model generate treatment text freely. That makes a wrong plan
untraceable and makes the spec's "risky step carries a caution" requirement unverifiable.

### D6 — Confidence is a band, and the boundary is part of the layout

Confidence is one of a small ordered set (low / medium / high) derived by the engine. No
percentage, no probability, no green "certain" state. The advisory statement sits **on the
result screen**, and the copy is plant-only so a reviewer cannot read it as medical advice.
This is a spec requirement (`spec.md`), not a copy preference.

### D7 — Navigation, edge-to-edge, and back behavior

Compose navigation host with a stage-based back stack; `enableEdgeToEdge`; predictive back
compatible (`enableOnBackInvokedCallback` true) so the Android 16 behavior in
`openspec/specs/play-compliance/spec.md` holds. Backing out mid-workflow preserves the photo
and answered intake (a spec scenario).

### D8 — AI content reporting only when the engine is generative

If O2 lands on a generative/hosted engine, the result screen must expose an in-app report
path (Play AI-Generated Content policy, recorded in `openspec/specs/play-compliance/spec.md`).
If the shipped engine is deterministic, the report path is not required — and the task list
records which case applies rather than assuming.

## Risks / Trade-offs

- **[Competing with a 50M+ incumbent]** → Mitigation: the slot is not scaffolded until a
  human supplies the O1 distinctness sentence. If the sentence is not specific, **do not
  build** (`docs/monetization-strategy.md` §1.2; `docs/niche-selection.md` §2 step 6).
- **[An advisory diagnosis still reads as a guarantee]** → Mitigation: bands not percentages,
  the advisory statement on the result screen, plant-only copy, and no cure language in the
  store listing (spec requirements; verified by task-level checks).
- **[Wrong treatment content]** → Mitigation: curated, cited, versioned data (D5) and a
  Store/Verify task that reads the plan for hazards before `ready`. This reduces the risk; it
  does **not** eliminate it.
- **[Hosted engine hidden cost]** → Mitigation: the cost arithmetic in
  `docs/monetization-strategy.md` §2.2 is applied before any pack price is set, and a
  `SERVER_COST` pack is blocked until a `PurchaseAuthority` exists (§3.7).
- **[Photo privacy]** → Mitigation: picker-based input, local retention by default, in-app
  deletion, and disclosure before any off-device send (spec requirements).
- **[Repetitive-content strike]** → Mitigation: one distinct job per slot, and this is a
  genuinely different job from the factory's existing slots (there is exactly one other slot,
  a pipeline smoke test — `registry/apps.json`).
- **[A bundled model's native code and the 16 KB rule]** → Mitigation: not chosen yet;
  if chosen, the change must add the 16 KB verification task required by
  `openspec/specs/play-compliance/spec.md`.
- **[Assuming the store page predicts revenue]** → Mitigation: every market figure is
  attributed, and `docs/market-enrichment-2026-09-23.md`'s own limitations ("What this still
  cannot tell us") are treated as binding on this design.

## Open Questions

These are the three human decisions from `proposal.md`, plus the operational unknowns that
must be answered before or during implementation. They are listed here rather than resolved
because each one changes the build, and this factory's contract reserves that call to a human.

1. **O1 — Differentiation angle.** Candidate families and the evidence each needs:
   - *Vertical crop / growing context* (for example one crop, one indoor setup, or one region):
     needs evidence that a specific sub-query is unserved — none was measured. **UNVERIFIED.**
   - *Diagnosis-to-action depth* (a plan tied to the exact cultivar and its conditions):
     needs a human judgement that the incumbent's generic plan is the weakness; the store
     data does not show it. **UNVERIFIED.**
   - *A different output artifact* (a record the user can act on over weeks): needs a
     hypothesis about the job, not a store fact. **UNVERIFIED.**
   The required evidence before implementation: a one-sentence distinctness statement that
   the human accepts, naming what a user gets here that the incumbent does not give.
2. **O2 — Engine path.** On-device/deterministic vs hosted. Hosted implies a server, a
   `PurchaseAuthority` if credit is sold, a named processor in the privacy policy, and a
   per-request cost model; on-device implies APK size and possible native code. **UNVERIFIED:**
   the on-device model's APK-size impact, runtime, and 16 KB readiness — none can be stated
   until a specific runtime is chosen.
3. **O3 — Monetization archetype.** One-time unlock (`docs/monetization-strategy.md` §3.1),
   consumable pack (`§3.2`, `Fulfillment.LOCAL` vs `SERVER_COST`), subscription (deferred,
   §3.3), or ads (§3.4). The niche's incumbents all declare IAP
   (`docs/market-enrichment-2026-09-23.md`), which is a fact worth weighing — it shows the
   shelf supports IAP, not that a new entrant converts.
4. **Operational — production access.** Does the §1.1 `≥ 14 days / 12 testers` gate apply to
   this Play account? `docs/monetization-strategy.md` §1.1 says this **must be checked in Play
   Console before planning a batch**. **UNVERIFIED.**
5. **Operational — Firebase and AdMob.** `remoteconfig` needs a Firebase project and a local
   `google-services.json`; `ads` needs an AdMob account and per-slot unit ids
   (`docs/monetization-strategy.md` §0). Neither exists until the human creates it, and both
   files are gitignored.
6. **Operational — Play price points.** The set of purchasable price points is
   **UNVERIFIED** (`docs/monetization-strategy.md` §2.2, research §8.8) and is a Play Console
   step. It bounds any pack price chosen under O3.

## Migration Plan

Not applicable — additive slot, no existing state to migrate. Rollback is deleting the slot
directory and its `registry/apps.json` entry before any Play upload; after a first upload the
`applicationId` is permanent (`AGENTS.md`, *Published namespace*), so the decision to upload
is the point of no return.
