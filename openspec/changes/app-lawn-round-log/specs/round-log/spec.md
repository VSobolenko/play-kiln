# Round log

## Purpose

Give a solo trade operator a private, offline record of his recurring round: the customers he visits,
in the order he visits them, what happened on each visit, and who still owes him. Every record is
editable and deletable, the whole record survives with no account and no network, and the app is
bought once rather than rented.

## ADDED Requirements

### Requirement: The round is an ordered recurring worklist

The app SHALL present the round as an ordered list of customers that the operator controls, and SHALL
let him move a customer up or down so the order matches the order he actually drives. The order SHALL
persist across restarts. The app SHALL NOT require a date, a time slot, or an appointment to be
attached to a customer before that customer can be visited.

#### Scenario: The operator reorders the round

- **WHEN** the operator moves a customer from the bottom of the round to the second position and
  returns to the list
- **THEN** the customer is shown second, and reopening the app keeps him second

#### Scenario: A customer is added and appears in the round

- **WHEN** the operator adds a customer named "Ada Reyes"
- **THEN** "Ada Reyes" appears in the round list without any appointment having been created for her

#### Scenario: The round is not an appointment book

- **WHEN** the operator opens the app on a day with no scheduled appointments
- **THEN** the day's list still shows the customers due on the round, and no empty state asks him to
  schedule an appointment

### Requirement: A visit is recorded in one tap and the round can be pushed

The app SHALL let the operator record a visit as **mowed**, **skipped** or **rained off** with a
single tap of a control that has a visible label and a target of at least 48 dp. It SHALL let him
push the round by one day in one action, and that action SHALL move each customer due that day
exactly once — never twice, and never leaving one behind. A recorded visit SHALL be editable and
deletable.

#### Scenario: A visit is marked mowed

- **WHEN** the operator taps the mowed control for a customer
- **THEN** that customer's row shows mowed and the visit is present in his history

#### Scenario: A rained-off morning pushes the round by one day

- **WHEN** the operator pushes the round by one day from a day with customers due
- **THEN** every customer due that day moves forward exactly one day, the count of customers due is
  unchanged, and pushing again moves each of them exactly one further day

#### Scenario: A push does not double-move a customer

- **WHEN** the operator pushes the round twice in a row
- **THEN** each affected customer has moved exactly two days and no customer appears twice in the
  round

#### Scenario: A mis-tapped visit is corrected

- **WHEN** the operator changes a visit recorded as skipped to mowed
- **THEN** the history shows the visit once, as mowed

### Requirement: Every customer carries a running owed balance

The app SHALL show, for each customer, a running balance computed from that customer's billable
visits minus the payments recorded against him, and SHALL show the total the operator is owed across
all customers. A payment SHALL be recordable in one tap and SHALL be removable. The balance SHALL be
derived from the stored visits and payments, so it is identical after a restart.

#### Scenario: Visits create a balance and a payment clears it

- **WHEN** a customer has two mowed visits at $40 each and a payment of $40 is recorded
- **THEN** that customer's balance is $40 and the operator's total owed includes $40

#### Scenario: The balance survives a restart

- **WHEN** the operator records visits and a payment, closes the app and reopens it
- **THEN** every balance is identical to the balance before the restart

#### Scenario: A removed payment restores the balance

- **WHEN** the operator removes a recorded payment of $40 from the customer above
- **THEN** that customer's balance is $80 again

#### Scenario: A skipped or rained-off visit is not billable

- **WHEN** a visit is recorded as skipped or rained off
- **THEN** that visit adds nothing to the customer's balance

### Requirement: The record is offline by construction

The app SHALL open, show the round, record a visit, record a payment and save, with the device in
airplane mode and with no account created. It SHALL NOT declare any `uses-permission` in its own
manifest, SHALL NOT contain a login, a sign-up, a sync or an account concept anywhere in its user
interface, and SHALL keep every record on the device.

#### Scenario: The round works in airplane mode

- **WHEN** the device has no network and no account exists, and the operator records a visit
- **THEN** the visit is recorded and visible after a restart, with no error and no retry prompt

#### Scenario: The app requests no permission of its own

- **WHEN** the app's own `AndroidManifest.xml` is read
- **THEN** it declares no `uses-permission` element at all

#### Scenario: The purchase is the only thing that ever needs the network

- **WHEN** the merged manifest of the built application is read
- **THEN** any permission present is one the purchase path requires and the app's own manifest did
  not ask for, and the round, its history and its balances are unaffected by whether the device is
  online

**Honest boundary, recorded rather than glossed.** The launch copy claims *no account and no network
in the daily path*, not "this app is incapable of using the network". The `billing` capability's own
module contributes no permission and no component — its recorded position is that the billing service
belongs to the Play Store app and the library only binds to it — so the expectation is a merged
manifest with no permission at all; that expectation is **measured from the built artifact** and the
claim is corrected if the artifact disagrees.

### Requirement: The app is bought once, and the free tier is honest

The app SHALL be usable for up to **eight** customers with no purchase and with no time limit, and
SHALL offer exactly one in-app product: a **non-consumable** unlock. The unlock SHALL be offered when
the ninth customer is added and when the history or the export is first used, SHALL state one price
with no trial clock, no subscription and no renewal wording, and SHALL be restorable after a
reinstall. There SHALL be no advertising and no second paywall anywhere in the app.

#### Scenario: The free tier is not a trial

- **WHEN** the operator has eight customers and has not purchased anything
- **THEN** all eight are fully usable, including recording visits and seeing balances, with no expiry
  date shown

#### Scenario: The ninth customer offers the unlock

- **WHEN** the operator adds a ninth customer without having unlocked
- **THEN** the unlock sheet is shown with the price, and the ninth customer is kept once the unlock
  completes

#### Scenario: There is no subscription anywhere

- **WHEN** the shipped strings and the billing product list are read
- **THEN** no string offers, implies or refers to a subscription or a renewal

### Requirement: The day list is usable one-handed outdoors

The app SHALL keep every control on the day list reachable with a thumb and at least 48 dp in its
smallest dimension, SHALL state each visit status with a word or a shape in addition to colour alone,
and SHALL keep the status of a row legible against the row background at a contrast ratio of at
least 4.5:1.

#### Scenario: Status is not carried by colour alone

- **WHEN** a visitor who cannot distinguish the palette's hues reads the day list
- **THEN** each row still states whether it is mowed, skipped and due, because the state is written
  and shaped rather than only coloured

#### Scenario: Every day-list control meets the target floor

- **WHEN** the day list is measured
- **THEN** every interactive control is at least 48 dp in both dimensions

### Requirement: The record is exported, and the record is editable and deletable

The app SHALL export the stored record to a CSV file through the system file picker, SHALL report how
many rows it wrote, and SHALL NOT export silently to a location the operator did not choose. It SHALL
let the operator edit and delete a customer, a visit and a payment, and a deletion of more than one
record SHALL require a confirmation naming what will be removed.

#### Scenario: The export writes what is stored

- **WHEN** the operator exports with three customers, five visits and one payment
- **THEN** the file contains three customer rows, five visit rows and one payment row, and the app
  reports those counts

#### Scenario: A customer is deleted and stays deleted

- **WHEN** the operator deletes a customer, confirming the prompt that names the customer
- **THEN** the customer is gone from the round after a restart, and no visit or payment for him
  remains visible

### Requirement: Every stored preference changes real behaviour

The app SHALL provide a settings surface reachable from the main screen, and every preference in it
SHALL change behaviour the operator can observe. The app SHALL NOT contain a setting that is stored
and then read by nothing.

#### Scenario: A setting has an effect

- **WHEN** the operator changes a preference offered in settings
- **THEN** the change is visible in the app's behaviour without a restart, and it survives one

## MODIFIED Requirements

None. This change adds a new capability and modifies no existing spec.
