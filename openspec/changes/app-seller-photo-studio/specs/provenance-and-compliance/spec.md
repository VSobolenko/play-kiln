## Purpose

Keeps AI-processed results honest with buyers and with Play: every output carries a provenance label,
Enhanced results are reportable without leaving the app, photos that leave the phone are described in the
privacy policy, and no client secret ships. Detail: `annex/H-compliance-and-provenance.md`.

## ADDED Requirements

### Requirement: Provenance is recorded and shown

Every output SHALL carry one of four provenance values — original pixels, manually adjusted, composite
with a generated background, or AI-enhanced — and the app SHALL show that value as a visible badge (`ProofRing`, annex J).
Exported files SHALL declare the provenance in metadata once spike S6 has recorded the exact IPTC
DigitalSourceType URIs; until then the export manifest SHALL carry the provenance in plain text and the
in-app badge SHALL still appear (annex H).

#### Scenario: Procedural background preserves original-pixels provenance

- **WHEN** a card is composed in Original mode over a deterministic solid, gradient or bundled procedural background
- **THEN** the result shows `ORIGINAL_PIXELS`

#### Scenario: Trained-model background is labelled as a generated composite

- **WHEN** untouched item pixels are composed over a background produced by a trained model
- **THEN** the result shows `COMPOSITE_GENERATED_BACKGROUND` and not `ORIGINAL_PIXELS` or `ENHANCED`

#### Scenario: Enhanced is labelled

- **WHEN** an Enhanced operation delivers a result
- **THEN** the result shows the AI-enhanced badge and the export manifest records `ENHANCED`

### Requirement: Provenance distinguishes procedural and generated work

Deterministic procedural backgrounds SHALL preserve `ORIGINAL_PIXELS` when item pixels are untouched.
Trained-model backgrounds SHALL use `COMPOSITE_GENERATED_BACKGROUND`; tonal item edits SHALL use
`MANUALLY_ADJUSTED`; generated/restorative item edits SHALL use `ENHANCED`.

#### Scenario: Tonal edit cannot claim original pixels

- **WHEN** a tonal edit changes the item region
- **THEN** the badge and manifest record `MANUALLY_ADJUSTED`

### Requirement: In-app report without leaving the app

The app SHALL let the seller report an offensive, wrong, unsafe or rights-related generative result from
the result screen without leaving the app, and the report SHALL reach the server as a reviewable moderation
case stored with its operation/provider/model metadata. Three offensive or unsafe reports for one endpoint
within 24 hours SHALL disable that endpoint pending operator review (Play AI-Generated Content policy,
ON RECORD 2026-08-26; API-11).

#### Scenario: Report is recorded

- **WHEN** the seller reports a result with a category and an optional note
- **THEN** the server stores the report under the job id and the seller sees a confirmation

#### Scenario: Repeated safety reports stop generation

- **WHEN** a third qualifying offensive or unsafe report reaches one endpoint within 24 hours
- **THEN** `/config` reports that endpoint disabled and no new job is sent to it until review

### Requirement: No free-text generative prompts in v1

Scene generation SHALL use fixed recipe templates from data (`scene-recipes.json`) and SHALL NOT accept
a free-text prompt from the seller in v1. A free-text scene field SHALL NOT ship until a server-side
moderation path and a refusal path exist (annex H).

#### Scenario: Scene is chosen from a list

- **WHEN** the seller picks a generated scene
- **THEN** the choice is one of the recipe ids and no free-text prompt is sent

### Requirement: Virtual model is gated

The virtual-model operation SHALL remain disabled in server configuration until the play-auditor spike
(S6) clears it, and the client SHALL show it as unavailable with the reason when it is disabled
(annex H, O-14).

#### Scenario: Disabled op is unavailable

- **WHEN** `/config` lists virtual model as disabled
- **THEN** the studio screen shows the tool unavailable and no job is created for it

### Requirement: Privacy policy describes what leaves the phone

The published privacy policy and the in-app policy entry SHALL state which photos leave the phone, that
they are sent to the app's server and a processing provider, the retention limit, the deletion path, and
that location, contacts, microphone and accounts are not used (annex H). The policy URL SHALL be reachable
from settings.

#### Scenario: Policy opens from settings

- **WHEN** the seller opens the privacy policy in settings
- **THEN** the published HTTPS policy opens

### Requirement: EXIF location is stripped from outputs

Exported images SHALL NOT retain EXIF GPS coordinates from the source (E-cases annex G; annex H).

#### Scenario: GPS stripped

- **WHEN** a source photo carries GPS EXIF and is exported
- **THEN** the exported file contains no GPS EXIF tags
