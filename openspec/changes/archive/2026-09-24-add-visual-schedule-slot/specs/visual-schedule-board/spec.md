# Visual schedule board

## Purpose

Provide an offline picture routine board that a parent or carer authors and a child can follow,
without generated content, remote processing, or claims about health, development, learning, or
outcomes.

## ADDED Requirements

### Requirement: A parent or carer authors every routine

The app SHALL let an adult author create a routine, give it a title, and add an ordered set of
steps. For every step, the author SHALL choose a picture through the Android system Photo Picker
and SHALL write the displayed label. The app SHALL NOT infer, generate, or rewrite the picture,
label, order, or meaning of a step. The author SHALL be able to keep multiple routines and select
which one to present.

#### Scenario: An author creates a routine

- **WHEN** the author enters a routine title and adds picture-and-label steps
- **THEN** the saved routine contains exactly those steps in the authored order, and reopening the
  app shows the same content and order

#### Scenario: An author changes the order

- **WHEN** the author reorders, edits, or removes steps and saves the routine
- **THEN** the next board presentation reflects that saved order and content exactly

#### Scenario: Several routines remain independent

- **WHEN** the author creates a second routine and edits either routine
- **THEN** both routines remain available and editing one changes no content in the other

#### Scenario: No generation surface exists

- **WHEN** the app's authoring and board surfaces are inspected
- **THEN** there is no prompt entry, generate action, or model-derived suggestion

### Requirement: The child sees one deterministic board state

The board SHALL show the routine's ordered picture cards, SHALL make the current step visually and
semantically distinct, and SHALL expose only manual controls for completing the current step or
moving back. It SHALL NOT use a model, server response, score, or inferred behaviour to select or
advance a step. It SHALL NOT add, omit, substitute, merge, skip, or reorder authored steps.

#### Scenario: A routine starts

- **WHEN** the child-facing board opens a saved routine
- **THEN** the first incomplete step is current, completed steps remain identifiable, and the
  remaining steps retain their authored order

#### Scenario: The presented sequence equals the authored sequence

- **WHEN** a routine is followed from start to finish
- **THEN** every authored step appears in order with no addition, omission, substitution, merge,
  or skip

#### Scenario: The current step is completed

- **WHEN** the child activates the completion control for the current step
- **THEN** that card shows a persistent checkmark and a spoken completed-state label, and the next
  incomplete step becomes current

#### Scenario: The last step is completed

- **WHEN** the child completes the final incomplete step
- **THEN** the board shows that the routine is finished and offers explicit restart and exit
  actions without changing the saved routine definition

#### Scenario: A completed step is reopened

- **WHEN** the child uses the back control from a later step
- **THEN** the earlier card becomes current without deleting or reordering any authored step

### Requirement: Empty and invalid states are explicit

The app SHALL never fabricate sample child data or silently start a routine when no usable routine
exists.

#### Scenario: No routines exist

- **WHEN** the app has no saved routine
- **THEN** the adult-facing empty state explains that an adult must create a routine and offers a
  create action

#### Scenario: A routine has no steps

- **WHEN** an author attempts to start an empty routine
- **THEN** the app stays in authoring mode, states that at least one picture step is required, and
  does not show the child-facing board

#### Scenario: A selected picture is unavailable

- **WHEN** a saved picture cannot be read
- **THEN** the editor identifies the affected step and requires the author to replace or remove it
  before presentation

### Requirement: Routine content remains local

Routine titles, step labels, selected pictures, ordering, and completion state SHALL be processed
and stored only on the device. They SHALL NOT be uploaded, synced, logged, included in telemetry,
sent to an AI service, or placed in cloud backup or device-transfer backup. The app SHALL provide
in-app deletion of a routine and its owned picture copies. The app SHALL NOT require an account or
sign-in.

#### Scenario: A routine is used offline

- **WHEN** an author edits a routine or a child uses the board without network connectivity
- **THEN** all authoring, presentation, completion, restart, and deletion behaviour remains
  available

#### Scenario: A routine is deleted

- **WHEN** the author confirms deletion
- **THEN** the routine, its steps, completion state, and app-owned picture copies are removed from
  local storage

#### Scenario: Deleting one routine preserves the others

- **WHEN** the author deletes one of several routines
- **THEN** every other routine and its owned pictures remain present and usable

#### Scenario: Data safety is reviewed

- **WHEN** the merged manifest, dependencies, backup rules, and network behaviour are inspected
- **THEN** no routine content has an off-device path and the Data safety answer reflects the
  shipped SDK and billing surface, with no analytics or crash-reporting SDK that transmits data

### Requirement: Permissions are limited to observable behaviour

The app SHALL use the Android system Photo Picker for existing pictures and SHALL request no broad
photo or video permission. The initial release SHALL NOT request camera, microphone, contacts,
location, notification, advertising ID, or exact-alarm permission.

#### Scenario: An author chooses a picture

- **WHEN** the author adds or replaces a step picture
- **THEN** the system Photo Picker supplies only the selected item and no broad media permission is
  requested

#### Scenario: The manifest is inspected

- **WHEN** the release manifest is compared with the implemented behaviour
- **THEN** it contains none of the permissions ruled out by this requirement

### Requirement: Copy remains a literal utility description

In-app text, bundled content, listing text, and store graphics SHALL describe only the literal
picture-board behaviour. They SHALL NOT name or imply a therapeutic, diagnostic, clinical,
developmental, educational, behavioural, calming, communication, or other outcome.

#### Scenario: A user reads the product description

- **WHEN** any shipped description or graphic is reviewed
- **THEN** it describes authoring and following picture steps and contains no assertion about a
  condition, benefit, or outcome

#### Scenario: In-app and bundled content are reviewed

- **WHEN** user-visible strings, accessibility labels, and any bundled examples are inspected
- **THEN** they describe generic routine actions and contain no condition, benefit, or outcome
  claim

#### Scenario: Store metadata is reviewed

- **WHEN** listing text and store graphics are reviewed before release
- **THEN** they describe only shipped functionality and contain no condition, benefit, or outcome
  claim

#### Scenario: The Health apps declaration is prepared

- **WHEN** the human prepares the Play Console Health apps declaration
- **THEN** the answer is based on the shipped claim-free functionality and the current live form,
  not on the search niche or an assumed Console classification

### Requirement: Commercial content stays in the adult surface

The initial release SHALL contain no ads. If the approved product includes a permanent premium
authoring unlock, it SHALL use Google Play Billing as a one-time non-consumable product and SHALL
appear only in the adult authoring/settings surface. The initial release SHALL contain no
consumable product, subscription, virtual currency, purchase countdown, or purchase prompt on the
child-facing board. A purchase surface SHALL show the real-money price and the effect of the
purchase before confirmation, SHALL use no pressure tactic, and SHALL never hide or block a routine
the author already created.

#### Scenario: The permanent unlock is purchased

- **WHEN** the adult completes the approved one-time purchase
- **THEN** the permanent authoring entitlement is acknowledged, never consumed, and can be
  restored through Play

#### Scenario: Price and effect are visible before confirmation

- **WHEN** the adult reaches the purchase decision
- **THEN** the real-money price and the authoring capability unlocked are visible before
  confirmation, with no countdown or pressure prompt

#### Scenario: A purchase never blocks an existing routine

- **WHEN** an adult declines a purchase or the entitlement is unavailable
- **THEN** every routine already created remains present and presentable

#### Scenario: The child uses a routine

- **WHEN** the child-facing board is visible
- **THEN** no price, offer, paywall, ad, or purchase action is present

#### Scenario: No purchase is approved

- **WHEN** the human gate chooses a free initial release
- **THEN** the billing dependency and every commercial surface are absent

### Requirement: The board is accessible without relying on colour or text alone

Every interactive element SHALL have a touch target of at least 48dp by 48dp and a meaningful
screen-reader label. A picture card SHALL expose its authored label, position, current/completed
state, and action through semantics. Current and completed states SHALL use a non-colour indicator.
The board SHALL remain readable at the user's system font scale, and disabling system animations
SHALL NOT prevent or delay a state transition.

#### Scenario: A screen reader focuses a card

- **WHEN** focus reaches a picture card
- **THEN** its label, sequence position, state, and available action are announced

#### Scenario: Controls are labelled and measurable

- **WHEN** a screen reader traverses the controls and their touch bounds are measured
- **THEN** every control announces its purpose, remains operable without sighted assistance, and
  has a touch target of at least 48dp by 48dp

#### Scenario: Colour is unavailable

- **WHEN** colour differences cannot be perceived
- **THEN** the current step and completed steps remain distinguishable by checkmark, text, and
  semantics

#### Scenario: Large text and reduced motion are honoured

- **WHEN** system font scale is increased or system animations are disabled
- **THEN** text and controls remain readable without clipping and state changes complete without
  depending on animation

### Requirement: Factory release gates apply

The slot SHALL use the factory toolchain and Play compliance floor in `AGENTS.md`, including
edge-to-edge and predictive back, separate backup-rule sections, a truthful complete store pack,
and signed release artifacts. The human SHALL own the audience, health, Data safety, content
rating, first-upload, and publish decisions.

#### Scenario: Release evidence is produced

- **WHEN** `node scripts/verify-slot.mjs --slot visual-schedule --strict-store --build` runs after
  release preparation
- **THEN** it reports PASS and the signed APK, signed AAB, and release manifest exist

#### Scenario: Human declarations are submitted

- **WHEN** Play Console declarations are ready to submit
- **THEN** the human checks them against the shipped build, the live policy pages, and the approved
  recorded decisions before submitting
