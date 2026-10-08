# Rental unit walk

## Purpose

Give a self-managing landlord a private, offline record of the condition of his units:
a walk he runs room by room, photographing condition and writing a note in each room, a
small portfolio of named units he owns outright on the phone, a move-out walk compared
room by room against the prior move-in report, and a signed PDF he can share or file
without a vendor cloud. Every walk is editable and deletable, survives a process kill,
and works with no account and no network; the app is bought once rather than rented.

## ADDED Requirements

### Requirement: A walk is a room-by-room condition record that survives an interruption

The app SHALL present a walk as an ordered sequence of rooms that the landlord controls, SHALL let him
add a room, attach one or more photos to it, write a note for it and record its condition, and SHALL
let him finish one room and open the next. It SHALL write every photo and every room note to durable
local draft storage **before the next room opens**, so a crash, a mis-tapped next action or a process
kill cannot delete a completed walk. Reopening the app after a process kill SHALL restore the same
walk, the same position in it and every photo already taken. A missing or unreadable draft SHALL read
as "nothing to resume" rather than crashing.

#### Scenario: A photo and a note survive a process kill

- **WHEN** the landlord photographs two rooms and writes a note, and the process is then killed
  before the report is finished
- **THEN** reopening the app shows the same walk with both rooms, both photos and both notes intact

#### Scenario: The draft is restored at the same room

- **WHEN** the landlord is part way through a walk, the process is killed and the app is reopened
- **THEN** the walk resumes at the same room he was on, with no room already finished presented as
  unfinished

#### Scenario: A corrupt draft costs the draft, not the app

- **WHEN** the stored draft payload is truncated or otherwise unreadable
- **THEN** the app opens normally and reports that there is nothing to resume

### Requirement: The walk is offline and account-free by construction

The app SHALL open, show the portfolio, run a complete walk with photos and notes, save the draft and
produce the finished report with the device in **airplane mode** and with no account created. It SHALL
NOT contain a login, a sign-up, a sync or an account concept anywhere in its user interface, SHALL NOT
require an email address, and SHALL keep every walk and every photo on the device.

#### Scenario: A walk finishes in airplane mode

- **WHEN** the device has no network and no account exists, and the landlord completes a full walk and
  opens the report
- **THEN** the walk, its photos and the report are produced with no error and no retry prompt

#### Scenario: There is no account anywhere

- **WHEN** the app's screens and strings are read
- **THEN** no screen offers or requires a login, a sign-up, an email or a sync

#### Scenario: The app's own manifest asks only for the camera

- **WHEN** the app's own `AndroidManifest.xml` is read
- **THEN** `CAMERA` is the only runtime `uses-permission` it declares

#### Scenario: The merged manifest is measured, not asserted

- **WHEN** the merged manifest of the built release application is read
- **THEN** any permission present that the app's own manifest did not declare is one the Play
  purchase path requires, the exact list is read from the built artifact, and the walk, its photos
  and the report are unaffected by whether the device is online

**Honest boundary, recorded rather than glossed.** The launch copy claims *no account and no network
in the daily path*, not "this app is incapable of using the network". The `billing` capability's Play
Billing dependency can contribute `INTERNET` / `ACCESS_NETWORK_STATE` and an internal
`TransportBackendDiscovery` service to the **merged** manifest even when the app declares none, so
the merged permission list is read from the built artifact and the claim is corrected if the artifact
disagrees.

### Requirement: The portfolio holds named units, each with its own walks

The app SHALL keep a local portfolio of named units. It SHALL let the landlord create a unit, rename
it and delete it, and SHALL start a walk from a named unit. Each unit SHALL keep its own walks, and a
walk SHALL NOT appear under a unit it does not belong to. The portfolio and its walks SHALL persist
across restarts.

#### Scenario: Two units keep independent walks

- **WHEN** the landlord creates two units and starts a walk from each
- **THEN** each unit shows its own walk and neither unit shows the other's walk

#### Scenario: A walk starts from a named unit

- **WHEN** the landlord starts a walk
- **THEN** the walk records the named unit it started from, and the unit's own list shows it

#### Scenario: A unit is renamed

- **WHEN** the landlord renames a unit
- **THEN** the new name is shown for that unit and survives a restart, and its walks stay with it

### Requirement: A move-out walk compares room by room against the prior move-in report

The app SHALL link a move-out walk to that unit's prior move-in walk and SHALL present the prior
report beside the new walk so the landlord records a difference for each room. A unit with no prior
walk SHALL report that there is no prior report rather than inventing a baseline.

#### Scenario: The prior report opens beside the new walk

- **WHEN** the landlord opens a move-out walk for a unit that has a finished move-in walk
- **THEN** the app shows the prior move-in report for that unit beside the new walk

#### Scenario: A per-room difference is recorded

- **WHEN** the landlord records that a room is worse than the prior report and adds a note
- **THEN** that room's difference is stored on the move-out walk and shown in the finished report

#### Scenario: A unit with no prior walk says so

- **WHEN** the landlord opens a move-out walk for a unit that has no prior move-in walk
- **THEN** the app states that there is no prior report, and does not show an empty report as a
  baseline

### Requirement: Photos come from the camera or the gallery without a storage permission

The app SHALL let the landlord attach a condition photo by taking one with the device camera and by
picking an existing one through the Android Photo Picker. Both paths SHALL store the photo in the
app's own private storage. Neither path SHALL request a storage permission, and the Photo Picker path
SHALL request no runtime permission at all.

#### Scenario: A condition photo is taken with the camera

- **WHEN** the landlord captures a photo with the device camera for a room
- **THEN** the photo is attached to that room and is present in the report

#### Scenario: An existing photo is attached from the gallery

- **WHEN** the landlord picks an existing photo through the system Photo Picker
- **THEN** the photo is attached to the room with no permission dialog shown

#### Scenario: No storage permission is requested

- **WHEN** the app's own `AndroidManifest.xml` and its capture code are read
- **THEN** no storage permission is declared and the picker path requests no runtime permission

### Requirement: The finished report leaves the phone as a signed PDF

The app SHALL let the landlord share the finished report as a PDF through the system share action
(`ACTION_SEND` with a `FileProvider` URI) and save it through the system document picker
(`ACTION_CREATE_DOCUMENT`). The PDF SHALL carry the unit, the walk kind and dates, every room with
its note and its photo references, every recorded difference, and the names the landlord and tenant
entered as signatures. The app SHALL report what it wrote. The app SHALL NOT upload the report or any
photo to a vendor cloud.

#### Scenario: Share carries the whole walk

- **WHEN** the landlord shares a finished report through the share action
- **THEN** the shared PDF contains the unit, every room with its note, and the recorded signatures

#### Scenario: Save writes the report where the landlord chooses

- **WHEN** the landlord saves the report and picks a location through the system document picker
- **THEN** a PDF is written to that location containing the same report

#### Scenario: The app reports what it wrote

- **WHEN** the report is exported
- **THEN** the app states that the PDF was written and where it went, rather than exporting silently

#### Scenario: Nothing uploads to a vendor cloud

- **WHEN** the device is offline and the landlord exports a report
- **THEN** the export completes with no network call and no vendor account

**Honest boundary, recorded rather than glossed.** The signature is a pair of names the landlord and
tenant enter on the device, recorded into the document; it is a signed report the landlord keeps, not
a certified e-signature service, and the listing copy says so.

### Requirement: Every user-facing string is a resource

The app SHALL render every user-facing string from a string resource, SHALL declare at least `en` and
`ru`, and SHALL NOT render hardcoded user-facing prose in Kotlin. A string added for one locale SHALL
be added for the other in the same change.

#### Scenario: A Russian device shows Russian

- **WHEN** the device locale is `ru` and the app is opened
- **THEN** the screens render from the Russian string resources rather than from English

#### Scenario: No screen renders hardcoded prose

- **WHEN** the shipped sources are read
- **THEN** no screen renders a user-facing string that is not a resource

### Requirement: Every stored preference changes real behaviour

The app SHALL provide a settings surface reachable from the main screen, and every preference in it
SHALL change behaviour the landlord can observe. The app SHALL NOT contain a setting that is stored
and then read by nothing.

#### Scenario: A setting has an effect

- **WHEN** the landlord changes a preference offered in settings
- **THEN** the change is visible in the app's behaviour without a restart, and it survives one

### Requirement: The walk is usable one-handed on site

The app SHALL keep every control on the portfolio, the walk and the report reachable with a thumb and
at least 48 dp in its smallest dimension, SHALL state each fact that matters — a room's condition, a
saved/draft state, a locked export — with a word or a shape in addition to colour alone, and SHALL
keep text legible against its background at a contrast ratio of at least 4.5:1.

#### Scenario: Condition is not carried by colour alone

- **WHEN** a landlord who cannot distinguish the palette's hues reads a walk
- **THEN** each room's condition is still stated, because it is written and shaped rather than only
  coloured

#### Scenario: Every control meets the target floor

- **WHEN** the portfolio, the walk and the report screens are measured
- **THEN** every interactive control is at least 48 dp in both dimensions

### Requirement: The record is editable and deletable with a named confirmation

The app SHALL let the landlord edit and delete a unit, a room, a photo and a walk. A deletion SHALL
require a confirmation that names the item being removed, and a deleted record SHALL be gone after a
restart.

#### Scenario: Deleting a photo names the photo

- **WHEN** the landlord deletes a photo from a room
- **THEN** a confirmation is shown that names the photo and the room before it is removed

#### Scenario: An edit persists

- **WHEN** the landlord edits a room note, closes the app and reopens it
- **THEN** the edited note is shown

#### Scenario: A deleted walk stays deleted

- **WHEN** the landlord deletes a walk, confirming the prompt that names it
- **THEN** the walk is gone from its unit after a restart

### Requirement: The app is bought once, and the free tier is honest

The app SHALL be usable for **one complete unit walk** with no purchase and with no time limit. It
SHALL offer exactly one in-app product: a **non-consumable** unlock. The unlock SHALL be offered when
the landlord starts a **second unit** and when he requests a **PDF export without the watermark**,
SHALL state one price with no trial clock, no subscription and no renewal wording, and SHALL be
restorable after a reinstall. There SHALL be no advertising and no second paywall anywhere in the
app.

#### Scenario: The free walk is a real walk

- **WHEN** the landlord has not purchased anything and runs one unit walk
- **THEN** the walk records rooms, photos and notes and produces a shareable report, with no expiry
  date shown

#### Scenario: The second unit offers the unlock

- **WHEN** the landlord starts a walk on a second unit without having unlocked
- **THEN** the unlock sheet is shown with the one price, and the second unit is usable once the
  unlock completes

#### Scenario: There is no subscription anywhere

- **WHEN** the shipped strings and the billing product list are read
- **THEN** no string offers, implies or refers to a subscription, a renewal or a trial

#### Scenario: The unlock survives a reinstall

- **WHEN** the landlord reinstalls the app on the same Play account that purchased the unlock
- **THEN** unlimited units and watermark-free PDF export are available again

## MODIFIED Requirements

None. This change adds a new capability and modifies no existing spec.
