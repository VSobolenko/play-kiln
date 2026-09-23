# Design — visual schedule slot

## Context

The slot is a small deterministic system with two intentionally separate surfaces: an adult
authors local content, and a child follows the resulting board. The measured market evidence says
the query passed a ratings screen, but it does not compare features; implementation remains blocked
until a human records an evidenced distinctness sentence (`proposal.md`, *Recorded decisions*).

The policy design is equally explicit. Because a child directly operates the board, declaring the
app as “parents/carers only” is not a safe way around Families review. The shipped product stays on
the literal-utility side of the claim boundary and excludes the health-claim path entirely
(`docs/research/2026-09-23-autism-app-policy.md` §§1–2).

## Goals / non-goals

**Goals:**

- Let an adult author ordered picture-and-label routines and delete them.
- Give the child a predictable board with a clear current step and durable completed state.
- Work without network access and prevent routine content from leaving the device.
- Use the smallest permission and dependency surface.
- Keep commercial content, if approved, entirely out of the child-facing board.
- Make the factory and human gates independently verifiable.

**Non-goals:**

- Generated, inferred, recommended, clinical, diagnostic, treatment, developmental,
  educational-outcome, or behavioural content.
- Accounts, sync, sharing, analytics, remote configuration, ads, social features, or a backend.
- Camera capture, reminders, notifications, exact alarms, child profiles, or free-text notes beyond
  routine and step labels.
- Consumables, subscriptions, virtual currency, or child-facing purchase prompts.

## Decisions

### D1 — One isolated Compose app with unidirectional state

Create the slot through `scripts/new-slot.mjs` and retain the scaffold's single `:app` module.
Compose screens receive immutable state and emit events; a ViewModel coordinates repositories and
pure routine operations. No new root capability is justified for behaviour used by one slot.

The navigation graph has an adult route (`RoutineList` → `RoutineEditor`) and a presentation route
(`RoutineBoard`). Starting the board is an explicit adult action; editing and commercial actions
are not reachable from the board without exiting it.

### D2 — Authored content is the only source of truth

The domain model is deliberately small:

- `Routine(id, title, steps)` owns an ordered list.
- `RoutineStep(id, picturePath, label, position)` stores authored content.
- `BoardProgress(routineId, completedStepIds, currentStepId)` stores presentation state separately
  from the routine definition.

Pure operations validate non-blank titles and labels, normalize positions after reorder/removal,
select the first incomplete step, complete the current step, move back, restart, and finish. They
never call a model or network. Keeping `BoardProgress` separate ensures presenting, completing, or
restarting cannot mutate the authored definition.

### D3 — Concrete board and empty states

The routine list owns the no-data state: it explains that an adult creates the picture steps and
offers one create action. An empty routine cannot start. An unavailable picture blocks presentation
and identifies the exact step to replace or remove.

The board shows every authored card in sequence so the child can see the routine, while the current
card is larger or otherwise prominent. Completed cards remain in place with a checkmark and a
semantic `completed` state; the current card has a semantic `current` state. Completion advances
only on the explicit completion action. Finishing shows restart and exit. No timer, clock, model,
or animation drives progression.

### D4 — App-owned local picture copies and local database

The system Photo Picker supplies one selected image at a time. The app copies the selected content
into its private storage and stores only that private path. This makes routine persistence and
deletion deterministic without broad media permission or a permanent dependency on the source
provider.

Room or DataStore stores routine metadata and progress; the implementer chooses the simpler option
that supports relational deletion without `kapt` (KSP if code generation is needed). Deleting a
routine removes its records, progress, and owned images in one repository operation.

Routine titles, labels, images, ordering, and progress are excluded from both cloud backup and
device transfer. This is stricter than merely relying on Data safety's local-processing exclusion
and makes “never leaves the device” testable. Policy basis: policy research §5.1–§5.4.

### D5 — No behaviour permission beyond the Photo Picker

The initial app declares no camera, broad media, location, microphone, contacts, notification,
exact-alarm, or advertising-ID permission. The Photo Picker needs no broad library read. There is
no reminder behaviour to justify notifications or alarms, and no capture behaviour to justify a
camera request.

Any proposal to add those behaviours is a scope change, because it changes the permission review,
Data safety story, tests, and potentially the Families obligations (policy research §5.3–§5.4).

### D6 — Minimal dependencies and no routine-data network path

The shared `core` capability is required by the factory. `billing` is permitted only if the human
approves the one-time product. `ads`, `telemetry`, and `remoteconfig` are excluded. No other SDK may
be added without checking whether it transmits data and whether it is approved for child-directed
services, as required for a children audience (policy research §5.2–§5.3).

Billing is isolated behind an adult-only purchase repository. It may transmit the transaction data
needed for Google Play Billing but never receives routine content. The final Data safety answer is
derived from the actual SDK, merged manifest, and network behaviour; “local-only” is not accepted
from design intent alone.

### D7 — Claim-free copy is a structural constraint

All app and store copy uses literal verbs: create, choose, order, show, complete, restart, delete.
It does not name a condition or assert an effect on health, development, education, behaviour,
communication, emotion, or another outcome. Bundled examples, screenshots, accessibility labels,
and translations follow the same rule.

The Health apps declaration remains mandatory for all apps, but whether Play Console classifies a
particular claim-free schedule utility as health-related is UNVERIFIED. The human answers from the
shipped build and live form (policy research §2.3 and §7.2). The health-claim alternative is not an
implementation branch; it is out of scope.

### D8 — No ads; optional one-time product; no consumables

No ads is a product choice that reduces the SDK and data surface, not a statement that policy bans
ads (policy research §3). If approved, the only product is a permanent adult-authoring unlock. It
is a one-time non-consumable entitlement, acknowledged and restored, never consumed. A free build
simply omits billing.

Consumables are excluded because the product defines no honest spendable unit. A picture pack or
permanent feature would be owned content, not something consumed. This follows the ownership versus
balance distinction in `docs/monetization-strategy.md` §§3.1–3.2. The purchase surface states what
real money changes, has no pressure mechanic, and never appears on the child-facing board; policy
basis: policy research §4.1–§4.4. No price is chosen here.

### D9 — Accessibility describes state, not decoration

Every action has at least a 48dp by 48dp touch target and a screen-reader label. Each card exposes
its authored label, sequence position, current/completed state, and action. Checkmarks and semantic
state accompany colour, and layouts support large font scale. If motion is used, disabled system
animations must not block state changes. The research records accessibility here as published age
guidance and Android guidance, not a general Play policy mandate (policy research §6).

### D10 — Human gates precede scaffolding and submission

Before scaffolding, the human records:

1. an evidenced feature-level distinctness sentence;
2. target age groups that truthfully include the child user;
3. approved claim-free `en-US` and `ru-RU` listing text;
4. expected Data safety answers and SDK list; and
5. free launch or the one-time billing product, including its Play product id.

Before submission, the human re-reads every live primary policy page linked from policy research,
records changes, reconciles the target audience, Health apps, ads, Data safety, and content-rating
answers, and performs the first upload and publish actions. `docs/play-publishing-playbook.md`
reserves these acts to the human.

## Risks / trade-offs

- **Distinctness is not yet evidenced.** Mitigation: task 1.1 blocks `new-slot.mjs`; weak ratings
  alone do not authorize implementation.
- **An adult-only declaration could conflict with use.** Mitigation: specify the child-facing board
  and require a child-inclusive audience decision; policy research §1 says imagery and terminology
  can override a declaration.
- **Copy can drift into an outcome claim.** Mitigation: narrow verbs, automated vocabulary scan,
  human semantic read, and health-claim path excluded rather than conditionally implemented.
- **A third-party SDK can invalidate local-only claims.** Mitigation: minimal allowlist, dependency
  and merged-manifest inspection, network-off tests, and Data safety derived from shipped evidence.
- **Selected images can outlive their records.** Mitigation: app-owned copies, repository-level
  cascade deletion, and tests that inspect both database and files.
- **Commercial UI could reach the child surface.** Mitigation: separate navigation, UI tests, and a
  spec prohibition. A wholly free launch remains valid.
- **Policy pages change.** Mitigation: task 9.1 requires a dated live-page re-read immediately
  before submission; the 2026-09-23 research is not treated as permanent.

## Migration plan

This is a new isolated slot, so no user-data migration or existing app migration exists. Rollback
before first upload is to abandon the slot. After first upload the applicationId remains permanent;
changes proceed as updates and never reuse another slot's identity (`AGENTS.md`).
