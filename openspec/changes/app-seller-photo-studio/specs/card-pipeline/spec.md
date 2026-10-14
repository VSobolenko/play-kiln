## Purpose

Turns the photos a seller took of an item into a finished product image on a chosen background and canvas,
without ever altering the item itself unless the seller asks for an enhanced result. This is the app's
central act. Detail: `annex/E-pipeline-and-operations.md` (O-nn), `annex/C-data-model.md`, screens S-03…S-05
in `annex/F-screens-and-flows.md`, edge cases E01–E28 in `annex/G-edge-cases.md`.

## ADDED Requirements

### Requirement: The item's pixels are untouched in Original mode

In Original mode the app SHALL produce the output by cutting the item out of the source photo and placing
it on a new background, and SHALL NOT change any pixel inside the item's mask, apart from an edge band of
a stated, fixed width. Everything outside the mask (background, shadow, canvas size, overlay) MAY be
generated or styled. Original mode SHALL be the default mode.

#### Scenario: Interior pixels are identical

- **WHEN** a photo is processed in Original mode and the output is compared with the source inside the
  item's mask eroded by the edge band
- **THEN** every compared pixel has identical colour values

#### Scenario: Original mode never calls a generative operation

- **WHEN** the seller exports a card in Original mode
- **THEN** no generative operation was applied to the item region and no credit was spent on one

### Requirement: Cut-out has a fast path and a studio path

The app SHALL offer a studio cut-out for finer edges (hair, fur, glass, thin straps), and SHALL additionally
offer a quick cut-out only when spike S1 selects an eligible stable on-device engine. When present, quick
cut-out SHALL work without network access and without spending a credit. When S1 selects no local engine,
the quick action SHALL be absent, every cut-out SHALL route through the studio path, and the cost SHALL be
shown before starting. The seller SHALL be
able to refine the mask by painting regions in or out, and to undo each refinement.

#### Scenario: Selected quick cut-out works offline

- **WHEN** S1 selected a local engine, the device has no network and the seller requests a quick cut-out
- **THEN** a mask is produced and no request leaves the device

#### Scenario: Quick cut-out is not selected

- **WHEN** S1 selected no eligible local engine
- **THEN** no quick-cut action or free-cut claim is shown and studio cut-out shows its credit cost before starting

#### Scenario: Mask refinement is reversible

- **WHEN** the seller paints a region out of the mask and then undoes the step
- **THEN** the mask equals the mask before the paint step

### Requirement: Backgrounds, shadows and composition

The app SHALL let the seller place the item on a solid colour, a gradient, a built-in studio scene or an
image from the gallery; SHALL render a shadow with adjustable direction, softness and density; and SHALL
compose the item onto a canvas with an adjustable fill ratio, centring and margin. The shadow and
composition SHALL be computed on the device without spending a credit.

#### Scenario: Shadow settings are applied

- **WHEN** the seller changes the shadow softness
- **THEN** the preview updates and the exported file reflects the same softness

#### Scenario: Fill ratio is respected

- **WHEN** the seller sets the item to fill 80% of the canvas on its longer side
- **THEN** the exported item's bounding box occupies 80% of the canvas on that side within one pixel

### Requirement: Manual editor

The app SHALL provide rotate, mirror and crop as non-destructive geometry steps, plus brightness, contrast,
saturation, sharpness and colour temperature as non-destructive tonal steps, each without spending a credit
and reversible through undo and redo. Geometry steps MAY retain `ORIGINAL_PIXELS` only when export merely
repositions/resamples the source under the Original invariant. Applying any tonal step to the item region
SHALL change provenance to `MANUALLY_ADJUSTED`; it SHALL never be labelled `ORIGINAL_PIXELS`. On an
`original-only` preset, only the preset's explicit `originalOnlyAllowed` steps are enabled. The editor
SHALL store recipes without overwriting the source bytes.

#### Scenario: Plain edits need no credit

- **WHEN** the seller rotates and crops a photo
- **THEN** no network request is made, the credit balance is unchanged, and the output is labelled manually adjusted

#### Scenario: Undo restores the previous state

- **WHEN** the seller applies three edits and undoes two
- **THEN** the image equals the image after the first edit

### Requirement: One-tap auto-card

The app SHALL offer a single action that identifies the item and its category, selects a suitable scene,
shadow and composition, checks the result against the selected marketplace's rules, and presents the
finished card for acceptance or adjustment. The identified item and category SHALL be shown to the seller
and SHALL be editable before the scene is chosen. The action SHALL use the marketplace's permitted mode:
where a marketplace allows no editing, it SHALL not alter the photo and SHALL show the coaching and the
rule check only.

#### Scenario: Category drives the scene

- **WHEN** the seller runs the auto-card on a shoe photo and then on a ring photo
- **THEN** the identified categories differ and the selected scenes are chosen from different sets

#### Scenario: Recognition can be corrected

- **WHEN** the identified category is wrong and the seller changes it
- **THEN** the scene, shadow and composition are re-selected for the corrected category

#### Scenario: A no-editing marketplace is respected

- **WHEN** the selected marketplace forbids image editing and the seller runs the auto-card
- **THEN** the output photo is the unaltered source and the result lists the rule findings and shooting advice

### Requirement: Results are provenance-labelled

The app SHALL record, for each output, whether it was produced in Original or Enhanced mode and SHALL show
that label to the seller. An Enhanced output SHALL carry an AI-processed label in the app and in the
exported file's metadata.

#### Scenario: Enhanced output is labelled

- **WHEN** the seller exports an Enhanced result
- **THEN** the in-app result shows an AI-processed label and the exported file's metadata declares it
