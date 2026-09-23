# Plant health assistant

## Purpose

Turns a photo of a sick **edible garden crop** plus a short structured symptom intake into a
reproducible diagnosis record, a concrete ordered treatment plan, and an indication of how
long until the treated crop is safe to eat.

## ADDED Requirements

### Requirement: Photo-first capture-to-plan workflow

The app SHALL provide a workflow that begins with a plant photo and ends with a written
treatment plan, without a free-form chat surface. The workflow SHALL have distinct,
resumable stages: (1) photo capture or selection, (2) structured symptom intake, (3)
diagnosis record, (4) treatment plan.

#### Scenario: A user completes the workflow

- **WHEN** the user provides a photo and answers the symptom intake
- **THEN** the app shows a diagnosis record and a treatment plan, and the user can navigate
  back to any earlier stage without losing the photo or the answers already given

#### Scenario: No chat surface is offered

- **WHEN** the user is anywhere in the workflow
- **THEN** the app exposes no free-form message composer and no conversational reply surface

### Requirement: Structured diagnosis record

A diagnosis SHALL be reported as a record, not as prose alone. The record SHALL name at
least one candidate cause, classify each candidate as a disease, a pest, or a nutrient
deficiency, and list the observable visual cues the diagnosis was read from.

#### Scenario: Diagnosis names a cause and its cues

- **WHEN** a diagnosis completes
- **THEN** the record lists at least one candidate with its class (disease, pest, or
  nutrient deficiency) and the cues observed in the photo and intake that support it

#### Scenario: Diagnosis cannot be made

- **WHEN** the photo is unusable, or the intake is insufficient to distinguish candidates
- **THEN** the app reports that it cannot diagnose, states what additional photo or answer it
  needs, and does not present a guessed cause as a diagnosis

### Requirement: Advisory confidence, never a guarantee

The app SHALL present every diagnosis as advisory. Confidence SHALL be expressed as an
ordered band (for example, low/medium/high) and SHALL NOT be presented as a percentage, a
probability, or any form that reads as a certainty of outcome. No diagnosis, plan, or store
copy SHALL promise a cure, a guaranteed fix, or an outcome.

#### Scenario: Confidence is shown as a band

- **WHEN** a diagnosis record is displayed
- **THEN** the confidence appears as one of the defined bands and no numeric probability is
  shown anywhere in the diagnosis, plan, or listing text

#### Scenario: Advisory boundary is visible with the result

- **WHEN** a diagnosis record is displayed
- **THEN** the advisory statement is visible on the same screen as the diagnosis, not only
  behind a link or in the app's About screen

#### Scenario: Plant diagnosis is not presented as medical advice

- **WHEN** any diagnosis or treatment text is displayed
- **THEN** it describes plants only, and no text presents the output as medical, veterinary,
  or human-health advice

### Requirement: Treatment plan with concrete steps

Every diagnosis SHALL be accompanied by a treatment plan of ordered, actionable steps. Each
step SHALL state what to do, and where a step carries a risk (for example, a chemical
treatment or a pruning cut) it SHALL state the caution. A plan SHALL state when the user
should re-check the plant.

#### Scenario: Plan steps are ordered and actionable

- **WHEN** the treatment plan is displayed
- **THEN** it shows steps in the order they are to be performed, each stating an action rather
  than a topic heading

#### Scenario: Risky step carries a caution

- **WHEN** a plan step involves a chemical, a hazard, or a destructive cut
- **THEN** the step states the caution next to that step

#### Scenario: Re-check timing is stated

- **WHEN** a treatment plan is displayed
- **THEN** it states when the user should re-check the plant and what a worsening result
  would mean

### Requirement: Required observation content

The app SHALL require structured content from the user about the plant and its conditions
before producing a diagnosis. The app SHALL NOT produce a diagnosis from a photo alone.

#### Scenario: Intake is required

- **WHEN** the user attempts to run a diagnosis without completing the required intake
- **THEN** the app blocks the diagnosis and indicates the missing answers

#### Scenario: Intake is stored with the diagnosis

- **WHEN** a diagnosis record is created
- **THEN** the record retains the answers that produced it, so the same record can be
  re-read later with its evidence intact

### Requirement: Photo input follows the Photo Picker preference

The app SHALL obtain photos through the Android Photo Picker (or an equivalent
system-mediated, user-initiated picker) as the default path. The app SHALL NOT request
broad media access (for example, a whole-library read permission) to satisfy its
photo-input requirement.

#### Scenario: Photo is chosen without broad media access

- **WHEN** the user attaches a plant photo
- **THEN** the photo is obtained through the system picker and the app holds no broad
  library-read permission

#### Scenario: In-app camera capture

- **WHEN** the chosen workflow includes taking a photo in the app
- **THEN** the camera is used only for that user-initiated capture, and the app requests no
  camera permission it does not need for that action

### Requirement: Photo and history data stay under the user's control

Diagnosis photos and intake answers SHALL be retained on the device by default and SHALL be
deletable by the user from inside the app. The app SHALL state what happens to retained
photos. Where a feature sends a photo or intake off the device, the app SHALL disclose that
before the send and SHALL NOT send on the user's behalf without that action.

#### Scenario: User deletes a diagnosis

- **WHEN** the user deletes a diagnosis record
- **THEN** the photo and its intake answers are removed from the app's storage

#### Scenario: Off-device processing is disclosed first

- **WHEN** any feature would send a photo or intake answer off the device
- **THEN** the app discloses what is sent and to what purpose before the first such send, and
  the user can decline

### Requirement: Replaceable diagnostic engine with no embedded credential

The diagnostic engine SHALL sit behind a single replaceable seam so the app builds and
behaves identically with an on-device engine and with a hosted engine. The APK SHALL NOT
contain a provider API key, token, or other credential for a hosted diagnostic service. A
hosted engine SHALL be reached through a component that keeps the credential off the device.

#### Scenario: Engine swap does not change the app's UI contract

- **WHEN** the engine implementation changes from on-device to hosted, or the reverse
- **THEN** the diagnosis record, plan, confidence band, and failure behavior seen by the user
  are unchanged

#### Scenario: No credential ships in the APK

- **WHEN** the release artifact is inspected
- **THEN** it contains no hosted-service API key, token, or credential, and the secret-guard
  check reports no secret

### Requirement: Diagnosis provenance

The app SHALL identify how a diagnosis was produced, so a result can be attributed to the
engine, the knowledge base version, or the model that produced it.

#### Scenario: Record identifies its engine

- **WHEN** a diagnosis record is created
- **THEN** it records which engine and which knowledge-base or model version produced it

### Requirement: AI-content reporting when generative output is shown

Where a diagnosis or plan is produced by a generative model, the app SHALL provide an
in-app way for the user to report the output, per the Play AI-Generated Content policy
already recorded in `openspec/specs/play-compliance/spec.md`.

#### Scenario: Reporting is reachable from the result

- **WHEN** a generative diagnosis or plan is displayed
- **THEN** the user can report that output from inside the app, and the report path is
  reachable from the result screen

#### Scenario: Non-generative output needs no report path

- **WHEN** the app ships only a deterministic, non-generative engine
- **THEN** the report path is not required, and the decision is recorded in the change's
  design notes rather than assumed

### Requirement: Play compliance floor applies

The slot SHALL meet the factory's Play compliance floor: `compileSdk`/`targetSdk` at or above
the `AGENTS.md` contract, separate cloud-backup and device-transfer rules excluding secrets,
working edge-to-edge and predictive back behavior, minimized permissions, and store
declarations that match the shipped app.

#### Scenario: Build and manifest gates pass

- **WHEN** `node scripts/verify-slot.mjs --slot plant-health-assistant --build` runs
- **THEN** the gate reports PASS on the SDK levels, backup rules, and manifest permissions

#### Scenario: Declarations match the shipped app

- **WHEN** the store pack is reviewed before `ready`
- **THEN** the Data safety answers, ads declaration, content rating, and AI-content
  declarations describe only what the app actually ships
