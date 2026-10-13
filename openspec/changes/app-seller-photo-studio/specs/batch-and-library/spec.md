## Purpose

Lets a seller process many photos and items in one go, reuse a saved look weeks later, keep every project
findable, and not lose work when the app is closed or the phone is changed. Detail: screens S-06, S-07,
S-08, S-13 in `annex/F-screens-and-flows.md`; edge cases E27, E35, E57–E62, E67–E68; backup rules in
`annex/C-data-model.md`.

## ADDED Requirements

### Requirement: Batch processing with a saved Look

The app SHALL let the seller select many photos, apply one Look (background, shadow, composition,
marketplace, mode) to all of them, and process them as a queue. The queue SHALL continue after the app is
closed and reopened, SHALL show per-item status, and SHALL let the seller cancel or retry individual items.

#### Scenario: One Look applies to every selected photo

- **WHEN** the seller selects 30 photos and applies a saved Look
- **THEN** all 30 outputs share that Look's background, shadow, canvas and marketplace

#### Scenario: Queue survives a restart

- **WHEN** the app is closed while 12 of 30 items are done and then reopened
- **THEN** the queue resumes and the 12 finished items are not processed again

#### Scenario: A failed item is retried alone

- **WHEN** one item of a batch fails
- **THEN** the seller can retry that item and the other items are unaffected

### Requirement: Looks are reusable and portable

The app SHALL save a Look under a name, apply it to any later project in one action, and include saved
Looks in the app's cloud and device-transfer backup.

#### Scenario: A Look is reapplied later

- **WHEN** the seller applies a Look saved weeks earlier to a new photo
- **THEN** the result uses exactly that Look's stored settings

#### Scenario: Looks survive a restore

- **WHEN** the app is restored on a new device from backup
- **THEN** the saved Looks are present

### Requirement: Grouping by item and SKU

The app SHALL group a batch's photos by item, let the seller correct the grouping by hand, preserve photo
order within an item, and number items with a SKU pattern the seller defines. The app SHALL read a scale
display, a SKU label or a ruler in a photo marked as such and SHALL place the reading in the item's fields
for the seller's confirmation, and SHALL exclude such reference photos from the exported set.

#### Scenario: Manual regrouping

- **WHEN** the seller moves a photo from item 3 to item 4
- **THEN** item 3 and item 4 reflect the move and the SKU numbering stays unique

#### Scenario: Scale reading is confirmed

- **WHEN** a photo of a scale is read
- **THEN** the weight appears in the item's weight field marked as unconfirmed until the seller accepts it

#### Scenario: Reference photos are not exported

- **WHEN** the seller exports an item that contains a scale photo
- **THEN** the scale photo is not in the exported set

### Requirement: Library and search

The app SHALL keep every project in a library with a preview, SKU, tags, marketplace and date, and SHALL
let the seller search and filter by any of them. The seller SHALL be able to delete projects from the
device.

#### Scenario: Search by SKU

- **WHEN** the seller types a SKU into the library search
- **THEN** only projects with that SKU are listed

#### Scenario: Deleting a project

- **WHEN** the seller deletes a project
- **THEN** its files are removed from the device and it no longer appears in the library

### Requirement: Bulk operations

The app SHALL let the seller select all or some items and change their marketplace, apply a Look, rename,
tag or delete them in one action.

#### Scenario: Select all and change marketplace

- **WHEN** the seller selects all items and changes the marketplace
- **THEN** every item's preset changes and the linter re-evaluates each one

### Requirement: Completion reminder

When a batch runs while the app is not in the foreground the app SHALL post a notification when the batch
finishes, and SHALL ask for the notification permission only when the seller starts a batch, with an
explanation. Without the permission the batch SHALL still run and its result SHALL wait in the app.

#### Scenario: Notification on completion

- **WHEN** a batch finishes while the app is in the background and the permission is granted
- **THEN** a notification states the number of finished and failed items

#### Scenario: Permission refused

- **WHEN** the seller refuses the notification permission
- **THEN** the batch runs normally and its result is shown when the seller opens the app
