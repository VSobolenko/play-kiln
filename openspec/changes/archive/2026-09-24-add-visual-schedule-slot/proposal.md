# Add a visual schedule slot

> **ABANDONED 2026-09-24 — do not commit the slot.** The evidence gate failed: the twelve-phrasing
> re-measurement found a 4.9★/500K+ direct incumbent and a 4.8★ specialist ranked first on the target
> query, so this was a bet to out-execute two already-good apps rather than a reachable pocket. The
> reasoning is in the *Recorded decisions* section of `tasks.md` and in
> `docs/research/2026-09-23-visual-schedule-evidence.md` (sections 1, 2, 5 and *Bottom line*).
>
> **Archived 2026-09-30 without merging its delta spec.** `openspec archive` would have written
> `openspec/specs/visual-schedule-board/` as though that capability existed, and
> `.cursor/commands/ship.md` tells an agent to read `openspec/specs/**` first. A capability killed by
> its own evidence must not be advertised to the next session, so this change was moved here as a
> record instead of being merged.

## Why

The recorded goal is to ship one isolated picture-based routine-board slot while making the
differentiator and the children-audience policy surface explicit human gates before implementation
(`README.md`; `.openspec.yaml`). The prior plant-health attempt demonstrates why those gates must
carry evidence: that change was abandoned after its differentiator asserted a safety conclusion its
data could not support (`openspec/changes/add-plant-health-assistant-slot/tasks.md`, *Recorded
decisions*).

The later twelve-phrasing re-measurement supersedes the single-query interpretation that motivated
this change. It reproduces the target query's ratings screen (30 visible results, 14 rated, 11 of
the 14 below 4.2, and a 3.85 median), but manual classification and enrichment show a market that
is smaller and more occupied than that score implied
(`docs/research/2026-09-23-visual-schedule-evidence.md`, sections 1, 2, 5, and *Bottom line*).

The direct cohort includes a 4.9-star / 500K+ app and a 4.8-star / 10K+ dedicated specialist that
ranks first on the exact target query. Building this slot would therefore be a bet that the factory
can out-execute two already-good apps, not an entry into an empty or generic field. The observed
ceiling is also materially below the plant vertical: the largest direct app across the run is
500K+, and the largest visible app on the exact target query is 100K+, versus 10M+ and 50M+ owners
on the plant vertical's flat queries (evidence sections 2 and 5).

The long tail does not resolve the opportunity. Thirteen of 17 manually classified direct apps are
both unrated and at no more than the 1K+ install bucket. That is equally consistent with an open
niche and a graveyard of attempts that did not gain traction; a store page cannot distinguish
those explanations (evidence sections 1, 6, and *Bottom line*).

**Distinctness sentence: UNVERIFIED — the store evidence does not compare shipped behaviour, and
the low-rated incumbents advertise substantial on-job feature sets. It therefore supports neither
a feature gap nor a verified execution gap.** A human would need a direct, dated, installed-product
comparison before recording which capability or observable quality users get here that the two
strong incumbents do not. A persuasive sentence without that evidence would repeat the failure
class of the abandoned change (`docs/niche-selection.md` §4 Candidate B and §5; task 1.1).

**Recommendation: do not commit this slot on the evidence now available.** The original draft's
"proceed to the human gate" posture is withdrawn. The change may be reconsidered only if direct
product use verifies a distinct capability or execution advantage; ratings, listing copy, and the
ambiguous unrated tail are not enough to authorize scaffolding.

## App idea, target user, non-goals, and slot identity

**Idea.** An offline picture routine board. A parent or carer creates a routine, selects one picture
and writes one short label for each step, orders the steps, and starts the board. The child sees the
authored cards in order, with one current step. Completing a step adds a persistent checkmark and
spoken completed state, then makes the next incomplete step current. With no routine, the app shows
an adult-facing create-routine empty state; it never fabricates child data.

**Target users.** The adult is the author and controls editing, deletion, and any commercial
surface. The child is an intended user of the presentation board. Because the child directly uses
the shipped surface, the implementation and declarations must be planned for an audience that
includes children; calling the product “parents/carers only” would not describe its observable
use.

**Non-goals.** No ML, generative AI, server, account, cloud sync, social feature, camera capture,
notification, exact alarm, behaviour tracking, free-form child profile, or child-facing commerce.
The product is not a health, diagnostic, treatment, developmental, educational, communication, or
outcome service. It makes no claim about a condition or about what following a routine does.

**Slot identity.** The slug is `visual-schedule`. `scripts/lib/namespaces.mjs` derives
`com.gexetr.visualschedule` by applying the owned `com.gexetr` prefix and removing the dash. The
dated slot directory is `apps/2026-09-23-visual-schedule/`. The display name is **Picture Routine
Board**. The package name becomes permanent after the first upload (`AGENTS.md`, *Published
namespace*).

## Children-audience and claim gate

All Play-policy statements in this change come from
`docs/research/2026-09-23-autism-app-policy.md`, read on 2026-09-23. Its UNVERIFIED items remain
unverified here.

- **Families trigger.** Any declared target age group that includes children triggers the Families
  Policy Requirements. Google may assess the declaration against the app and listing imagery and
  terminology and may reject or enforce against a mismatch (policy research §1.1–§1.3). This
  board has a child-facing use mode, so an adult-only declaration is not a safe shortcut. The human
  must confirm the exact age groups and resulting answers in Play Console before implementation and
  again before submission.
- **Literal utility line.** “Shows the picture steps you chose, in the order you chose” describes
  functionality. A statement about a condition, development, behaviour, communication, learning,
  calming, treatment, or another result crosses into health-related claims under the research's
  applied boundary (policy research §2.1–§2.4). No policy page enumerating every permitted phrase
  was found; that phrasing boundary is explicitly UNVERIFIED in the research.
- **Recorded scope decision.** The health-claim path is ruled out for this slot. The app and store
  metadata will remain a claim-free routine utility. The mandatory Health apps declaration must be
  answered from the shipped build and the live form; whether Play Console classifies a particular
  claim-free schedule utility as health-related is UNVERIFIED and is a Console review decision
  (policy research §2.3 and §7.2).
- **Human gate.** Before code, the human approves the target-audience choice, the claim-free store
  wording, and the expected Data safety answers. Before submission, the human re-reads the live
  pages linked by the policy research and reconciles any change. These are legal/declaration acts
  and are never silently submitted by the factory (`docs/play-publishing-playbook.md` §§6–7, 11).

## Observable behaviour

The adult authors all routine content: a title, selected pictures, short labels, and step order.
The app copies only the selected picture into app-owned local storage. The child-facing board shows
the ordered cards, highlights and announces one current step, and exposes manual completion and
back controls. Completion is a checkmark plus a semantic completed label; it does not erase the
card or alter the saved routine. Finishing the last step shows explicit restart and exit actions.

All state transitions are deterministic functions of saved content and direct taps. There is no
model, recommendation, generated content, server response, automatic interpretation, or automatic
advance. The exact scenarios live in `specs/visual-schedule-board/spec.md`.

## Data and permissions

The local-only posture follows policy research §5.1–§5.4: on-device-only processing is not
collection for the Data safety form, while sending a photo, name, routine, or SDK payload off the
device would change the declaration and, for a children audience, the allowed SDK/API surface.

Concrete consequence: routine titles, labels, selected pictures, order, and completion state may
never leave the device. They are excluded from cloud backup and device transfer as well as upload,
analytics, logs, sync, and AI calls. Billing, if approved, communicates purchase data through Play
Billing but receives no routine content. The privacy policy and Data safety draft must be derived
from the final merged manifest and dependencies, not from this intention alone.

The initial behaviour needs no dangerous permission. Existing images come through the system
Photo Picker, so the app requests neither broad media access nor camera access. Location,
microphone, contacts, notifications, exact alarms, and `AD_ID` have no observable job and are ruled
out. Any future feature that needs one requires a separate change and a new policy/declaration
review (policy research §5.3–§5.4).

## Monetisation

The shape is **free core, no ads, optional one-time permanent authoring unlock through Google Play
Billing**. The one-time product is non-consumable: it may raise the limit on creating new routines,
is acknowledged rather than consumed, and is restorable. It may never hide, delete, or disable a
routine the user already created.

The initial release has **no consumable**. A persistent picture collection or permanent feature is
ownership and must not be mislabeled as a consumable; no honest spendable unit has been specified.
Adding a consumable later requires a separate product decision that names what is spent and how it
is restored. There is no subscription, virtual currency, or price in this proposal.

Play Billing for digital goods and the non-coercive presentation rules come from policy research
§4.1–§4.4. No ads is a low-risk **product choice**, not a policy requirement (policy research §3).
The research found no Play-policy ban on child-directed IAP and no published Play rule requiring a
parental purchase gate, but those are UNVERIFIED absences; legal purchase-consent mechanics remain
a human/legal question. Therefore the human may choose a wholly free launch at the monetisation
gate, in which case billing is omitted.

## What changes

- Do not create `apps/2026-09-23-visual-schedule/` on the current evidence. If a later direct
  product comparison verifies distinctness and a human explicitly reopens the change, add one
  isolated Kotlin/Compose slot at that path only after the remaining human gates pass.
- Add the `visual-schedule-board` capability specification for the observable product behaviour.
- Use the factory toolchain, store-pack contract, verification gates, and human publishing
  checkpoints from `AGENTS.md`.
- Consume only the required shared `core` capability and, only if the one-time product is approved,
  the shared `billing` capability. Do not add ads, telemetry, remote configuration, or analytics.

**BREAKING:** none. This is an additive, isolated slot.

## Recorded decisions

- **THE EVIDENCE GATE FAILED — do not commit the slot (recorded 2026-09-24).** The twelve-phrasing
  re-measurement in `docs/research/2026-09-23-visual-schedule-evidence.md` replaced the earlier
  single-query interpretation. It found a 4.9-star / 500K+ direct incumbent, a 4.8-star specialist
  ranked first on the target query, no store-visible feature gap, and only an execution gap whose
  existence cannot be verified without using the apps. The slot would be a bet to out-execute two
  already-good products in a market whose observed direct ceiling is 500K+ and whose target-query
  ceiling is 100K+, materially below the plant vertical's 10M+ and 50M+ owners. Thirteen of 17
  direct apps are unrated at no more than 1K+ installs, which is equally consistent with "open
  niche" and "graveyard." The recommendation changed from proceed to the human gate to **do not
  commit unless direct installed-product evidence reverses this finding**.
- **Artifact qualification (recorded 2026-09-24):** the raw artifacts support the ratings, rank,
  install buckets, IAP badges, and the 13-of-17 count. They do **not** establish that RoutinePals
  and Little Planner *launched* in August–September 2026 or that both shipped subscriptions then.
  `.kiln/visual-schedule/enrich-full.json` records only 100+ installs, IAP badges, and update dates
  of Sep 11 and Aug 14, 2026; `.kiln/visual-schedule/listing.txt` identifies RoutinePals'
  subscription and prices, while Little Planner's listing plus badge establish IAP but not product
  type or launch date. Those stronger claims are not used by this change.
- **Unchanged by the market evidence:** the bounded product remains deterministic, local-only, and
  claim-free; the child is still an intended user; the Families/declaration gates remain
  human-owned; and the namespace and slot identity remain reserved if the change is ever reopened.

- **Decided:** slug `visual-schedule`, applicationId `com.gexetr.visualschedule`, directory
  `apps/2026-09-23-visual-schedule/`, display name **Picture Routine Board**. Evidence:
  `scripts/lib/namespaces.mjs` and `AGENTS.md`.
- **Decided:** the adult authors every picture, label, and order; the child directly uses the
  deterministic board; missing data produces an adult-facing empty/error state. Evidence: the
  recorded goal plus the bounded product definition in this change.
- **Decided:** no health-claim path, ML, server, account, cloud sync, ads, broad photo access,
  camera, notification, or exact alarm. Evidence: policy research §§1–5 and the requirement to keep
  the product literal, deterministic, and on-device.
- **Decided:** if monetised, use only a one-time non-consumable authoring unlock through Play
  Billing; no consumables ship because no honest spendable unit is defined. No-ads is a product
  choice, not a policy conclusion. Evidence: `docs/monetization-strategy.md` §§3.1–3.6 and policy
  research §§3–4.
- **UNVERIFIED / blocking:** the feature-level distinctness sentence and the claimed execution gap.
  Store pages cannot establish what incumbents lack or why the low-rated apps are weak. A direct,
  dated, installed-product comparison and explicit human reversal of the do-not-commit decision
  are required before scaffolding.
- **UNVERIFIED / human-owned:** the live relationship between Families/Designed for Families
  labels, Console classification of a claim-free utility, any purchase-consent obligation imposed
  by law or the live Console, and paying demand. These remain exactly within the research's stated
  limits.
