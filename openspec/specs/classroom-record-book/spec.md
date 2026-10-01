# classroom-record-book Specification

## Purpose
Give a teacher a private, offline record book they own outright: classes, students, assignments,
grades, attendance, participation, missing work, parent contacts and seating. Every record is
editable and deletable, the whole record backs up and restores from a file the teacher chooses,
and the app is bought once rather than rented.

## Requirements

### Requirement: Every class is creatable, editable and deletable

The app SHALL let a teacher create a class, rename it and edit its subject, period and term, and
SHALL let a teacher delete a class, including the class currently open. A deletion SHALL be
permanent: after deleting a class, student, assignment or grade cell, the app SHALL NOT display
or restore that record. A destructive action that removes more than one stored record SHALL
require an explicit confirmation that names what will be removed.

#### Scenario: A class is created and renamed

- **WHEN** the teacher creates a class and then renames it
- **THEN** the class list shows exactly one class under the new name and the old name appears
  nowhere in the app

#### Scenario: A class is deleted while it is open

- **WHEN** the teacher deletes the class currently open, confirming the prompt
- **THEN** the class disappears from the list, the app navigates away from it, and reopening the
  app does not bring it back

#### Scenario: A grade cell is deleted

- **WHEN** the teacher clears a grade that was previously entered
- **THEN** the cell is empty on the next screen open and the student's average is recomputed
  without it

### Requirement: The roster is editable and importable

The app SHALL let a teacher add a student, edit a student's name and external identifier, remove
a student from a class, and mark a student inactive while keeping their history. It SHALL import
a roster from a CSV file chosen through the system file picker, SHALL report how many rows it
read and how many it skipped, and SHALL NOT silently drop or overwrite an existing student.

#### Scenario: A student is removed and does not reappear

- **WHEN** the teacher removes a student from the roster
- **THEN** the student is absent from the roster, from the grade grid, and from attendance on the
  next screen open

#### Scenario: A roster CSV imports

- **WHEN** the teacher imports a CSV whose first column holds names
- **THEN** the app adds one student per readable row, reports the counts of imported and skipped
  rows, and names any row it skipped and why

#### Scenario: An unreadable import is refused

- **WHEN** the chosen file is not a readable CSV roster
- **THEN** the app adds no student, keeps the existing roster unchanged, and states the reason

### Requirement: Assignments and weighted categories are editable

The app SHALL let a teacher create, edit, reorder and delete categories with a numeric weight,
and create, edit and delete assignments within a category, including the points an assignment is
worth. It SHALL reject a non-positive points value with a stated reason instead of storing it.

#### Scenario: An assignment is edited

- **WHEN** the teacher changes an assignment's points from 10 to 20 and a student's earned points
  stay the same
- **THEN** that student's percentage for the assignment halves and the running average reflects
  the change

#### Scenario: A non-positive points value is entered

- **WHEN** the teacher enters zero or a negative value as an assignment's points
- **THEN** the app stores no change and states that points must be greater than zero

### Requirement: A grade cell records score, missing or exempt

The app SHALL record for each student and assignment one of: an earned-points value, `Missing`,
or `Exempt`. It SHALL present these three as distinct user choices and SHALL NOT store an empty
cell as zero.

#### Scenario: A cell is marked exempt

- **WHEN** the teacher marks a grade cell exempt
- **THEN** the app shows it as exempt, excludes it from that assignment's distribution, and does
  not count it in the denominator of any average

#### Scenario: A cell is marked missing

- **WHEN** the teacher marks a grade cell missing
- **THEN** the app shows it as missing and applies the classroom's stored missing-work rule to
  the average

### Requirement: The running average is explainable and never fabricated

The app SHALL compute the running average as: per-assignment percent from earned points divided
by the assignment's points; optional removal of the n lowest percentages in a category; a
category average as the mean of surviving percentages; and an overall value as the
weight-weighted mean over categories that have at least one surviving grade. `Exempt` cells SHALL
be excluded. A category with no surviving grade SHALL be excluded from the overall value and
named as excluded in the UI. When no category has a surviving grade, the app SHALL display a
not-enough-data state and SHALL NOT display `0`, `NaN`, or any other numeric placeholder. The app
SHALL state the rounding mode and decimals it applied beside every displayed average.

#### Scenario: A category has no grades yet

- **WHEN** one category has assignments but no student has a grade in any of them
- **THEN** the app excludes that category from the overall average and identifies it as excluded

#### Scenario: No grade exists anywhere

- **WHEN** a class has students and assignments but no grade cell holds a value
- **THEN** the app displays the not-enough-data state and no numeric average

#### Scenario: Every cell in a category is exempt

- **WHEN** all of a student's cells in one category are exempt
- **THEN** that student's overall average excludes the category rather than counting it as zero

#### Scenario: The lowest grade is dropped

- **WHEN** the teacher sets a category to drop its single lowest percentage
- **THEN** the category average is computed from the remaining percentages and the drop is named
  in the UI

### Requirement: Attendance, notes and contacts are recorded per student and date

The app SHALL let a teacher record an attendance status for each student on a chosen date, add
participation, behaviour, missing-work and general notes to a student with a date, log a parent
contact with a date, channel and summary, and mark a follow-up date. Each of these records SHALL
be editable and deletable.

#### Scenario: Attendance is recorded for a date

- **WHEN** the teacher marks a student absent-unexcused on a date and reopens that date
- **THEN** the app shows that status for that student and that date

#### Scenario: A note is deleted

- **WHEN** the teacher deletes a note
- **THEN** the note is gone from the student's history on the next screen open

### Requirement: A seating chart assigns students to desks

The app SHALL let a teacher define a grid of desks for a class and assign a student to a desk,
and SHALL NOT place the same student at two desks at once.

#### Scenario: A student is moved

- **WHEN** the teacher assigns a student who already occupies a desk to a different desk
- **THEN** the app moves that student and leaves no duplicate seat occupied by them

### Requirement: The record exports to CSV and PDF

The app SHALL export a class to CSV through the system create-document picker, drawing a
students-by-assignments matrix of entered values plus the computed averages, with RFC 4180
quoting so a name containing a comma, a quote or a newline does not corrupt the file. It SHALL
export a printable PDF report of a class or of one student. An export SHALL contain only values
actually stored: no placeholder is invented for an empty cell.

#### Scenario: A name containing a comma is exported

- **WHEN** a student's name contains a comma or a double quote and the teacher exports CSV
- **THEN** the exported file remains readable and that name is preserved exactly

#### Scenario: An empty cell is exported

- **WHEN** a grade cell is empty and the teacher exports CSV
- **THEN** the exported cell is empty and is not written as zero

### Requirement: The whole record backs up and restores from a chosen file

The app SHALL write the complete record as a single versioned document to a file the teacher
chooses, and SHALL restore from such a file. Restore SHALL validate the document's schema version
and structure, and SHALL refuse — adding, changing and removing nothing — when the version is not
supported or the document is malformed, stating the reason. A successful restore SHALL reproduce
the classes, students, assignments, grades, attendance, notes, contacts and seating that were
saved.

#### Scenario: A backup round-trips

- **WHEN** the teacher backs up, deletes a class, and then restores that backup
- **THEN** the deleted class and its students, assignments and grades are present again

#### Scenario: An unsupported document is refused

- **WHEN** the chosen file is not a record-book backup or carries an unsupported schema version
- **THEN** the app changes nothing, keeps the current record intact, and states the reason

### Requirement: The app works offline, account-free, and requests no permission of its own

Every recording, averaging, export and backup feature SHALL operate without network connectivity
and without an account. The app's own manifest SHALL declare no permission at all; the only
permission in the merged release manifest may be one contributed by the Play Billing library's own
manifest, and the merged manifest SHALL be read and recorded as evidence rather than assumed. The
app SHALL NOT transmit stored records anywhere: the only network exchange in the product is the
Play Billing purchase and restore call, which sends no student data. File access SHALL go through
the Storage Access Framework, and sharing SHALL use the system Sharesheet.

#### Scenario: The device is offline

- **WHEN** the device has no network connection
- **THEN** a teacher can create a class, enter grades, mark attendance, export CSV and back up

#### Scenario: The merged manifest is inspected

- **WHEN** the merged release manifest's requested permissions are listed
- **THEN** the list contains no dangerous permission and nothing beyond what the Play Billing
  library contributes, and the observed list is recorded in `store/checklist.md`

### Requirement: The free tier is usable and the unlock is a one-time purchase

The app SHALL allow one class with up to 20 students and unlimited grades without payment, and
SHALL require a one-time, non-consumable, restorable Play Billing unlock for a second class,
more than 20 students in a class, weighted categories, export, and backup and restore. The paywall
SHALL NOT state a hard-coded price; it SHALL say that the unlock is a one-time purchase and that
Google Play shows the exact price in the user's currency on the confirmation sheet, and the
store listing SHALL carry the price. Billing SHALL contain no subscription and no
advertisement. A pending, cancelled or failed purchase SHALL leave all existing local work
readable and SHALL NOT grant the unlock. The app SHALL acknowledge a pending purchase on cold
start, and SHALL make the unlock survive process death and offline use by persisting the
entitlement locally.

#### Scenario: The free limit is reached

- **WHEN** the teacher attempts to create a second class, or add a 21st student to a class,
  without the unlock
- **THEN** the app offers the unlock, states that it is a one-time purchase whose price Google
  Play shows on the confirmation sheet, and creates nothing

#### Scenario: A purchase is restored on a new device

- **WHEN** the teacher restores purchases and Play reports the unlock
- **THEN** the app grants the unlock and the previously blocked actions succeed

#### Scenario: A purchase is cancelled

- **WHEN** the teacher cancels the purchase flow
- **THEN** the app remains on the free tier, keeps all local work, and reports no unlock

#### Scenario: The entitlement survives a restart

- **WHEN** an unlocked app is force-stopped, the device is taken offline, and the app is reopened
- **THEN** the unlock is still in effect and no blocked action is offered as locked
