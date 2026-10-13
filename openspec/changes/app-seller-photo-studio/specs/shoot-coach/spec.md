## Purpose

Helps a seller take a usable first photo and a complete set of shots for the item type, on the device and
without spending a credit, because the quality of the source photo decides the quality of every card made
from it. Detail: screen S-16 in `annex/F-screens-and-flows.md`; edge cases E63–E64.

## ADDED Requirements

### Requirement: Camera with live framing guidance

The app SHALL provide an in-app camera that shows a frame for the item, warns when the image is blurred,
over-exposed or under-exposed, warns when the device is not level, and warns when the background is not
uniform. Guidance SHALL be computed on the device and SHALL work without network access.

#### Scenario: Blur warning

- **WHEN** the live image is blurred beyond a stated threshold
- **THEN** the camera shows a blur warning and offers to retake

#### Scenario: Works offline

- **WHEN** the device has no network
- **THEN** the camera and all its guidance work normally

### Requirement: Shot list by item type

The app SHALL offer a shot list for the selected item type (for example clothing, shoes, jewellery,
electronics) naming each required shot (front, back, label, flaw close-up, measurement), SHALL show which
shots are done, and SHALL let the seller skip a shot.

#### Scenario: Progress is shown

- **WHEN** the seller has taken two of five shots for a garment
- **THEN** the list shows two shots done and three remaining

### Requirement: Quality check on imported photos

The app SHALL evaluate imported photos for blur and exposure before processing and SHALL tell the seller
which photos are weak and why, without blocking the seller from proceeding.

#### Scenario: Weak photo is flagged but allowed

- **WHEN** an imported photo is too dark
- **THEN** it is flagged with the reason and the seller can still process it

### Requirement: Import without permissions where possible

The app SHALL import photos from the gallery through the system picker without requesting a storage
permission, and SHALL request the camera permission only when the seller opens the camera, with an
explanation of what is lost without it.

#### Scenario: Gallery import needs no permission

- **WHEN** the seller imports from the gallery
- **THEN** no storage permission prompt is shown

#### Scenario: Camera refusal degrades gracefully

- **WHEN** the seller denies the camera permission
- **THEN** the seller can still import from the gallery and the app states what the camera would have added
