# Design: app-teacher-gradebook

## Product boundary

`teacher-gradebook` is a private, offline classroom record book. It has no account, server,
cloud sync, analytics, camera, ML or AI surface. It is a record the teacher keeps *beside* the
school's official system: it does not sync with, replace, or claim certification from any SIS or
LMS, and it bundles no grade-scale, standard, or curriculum data.

The recorded product decision that shapes this design: the measured complaints against the
incumbents are about **create/update/delete, crashes, a stranded purchase and a resent
subscription** — none are about maths (evidence file §2.3). So the record surfaces a
grade-only competitor does not ship — **attendance, participation and behaviour notes,
missing-work tracking, a parent-contact log and a seating chart** — are first-class features
here, not add-ons. They also make the product's purpose a *record system* rather than a
calculation, which is the boundary `.cursor/rules/ideation-gate.mdc` draws.

## Data model

One classroom store, serialised as a single JSON document (see "Persistence" for why).

| Entity | Fields |
|---|---|
| `Classroom` | id, name, subject, period, term, gradeScale, categories, students, assignments, grades, attendance, notes, contacts, seating |
| `Category` | id, name, weight (percent), dropLowest (0..n) |
| `Assignment` | id, categoryId, name, points (nullable → not yet graded), date |
| `Student` | id, firstName, lastName, externalId (the SIS id the teacher already has), active |
| `GradeCell` | studentId, assignmentId, value: `Score(points)` \| `Missing` \| `Exempt` |
| `AttendanceMark` | studentId, date, status (`PRESENT`, `TARDY`, `ABSENT_EXCUSED`, `ABSENT_UNEXCUSED`) |
| `RecordNote` | studentId, date, kind (`PARTICIPATION`, `BEHAVIOUR`, `MISSING_WORK`, `GENERAL`), text |
| `ParentContact` | studentId, date, channel, summary, followUpOn (nullable) |
| `SeatingPlan` | rows, columns, seats: desk index → studentId or null |
| `GradeScale` | ordered bands (minPercent, letter), user-defined, no bundled default |

Every entity has an id so a rename is an edit rather than a rewrite, and a delete is a real
delete. Deletion is a first-class tested operation precisely because "says deleted but
reappeared" and "no option to add or delete classes" are two of the measured complaints.

## Grade arithmetic contract

The only arithmetic is the running average, and it is fully specified so no value is ever
fabricated:

1. Per assignment, percent = earned ÷ points. `points <= 0` is rejected at input.
2. `Exempt` is excluded from the denominator; `Missing` counts as 0 unless the teacher turns
   "treat missing as zero" off, in which case it is excluded. The choice is stored per
   classroom and shown on every average so the number is explainable.
3. Optional `dropLowest` per category removes the n lowest percentages in that category.
4. Category average = mean of the surviving percentages.
5. Overall = Σ(weightᵢ × categoryAvgᵢ) ÷ Σ(weightᵢ), over categories that have ≥1 surviving
   grade. A category with no usable grade is **excluded and named in the UI**, never treated
   as zero.
6. If no category has usable data, the app shows **"Not enough data yet"** — never `0`, never
   `NaN`.
7. Rounding is an explicit classroom setting: mode (`HALF_UP`, `HALF_EVEN`, `FLOOR`) and
   decimals (0–2). The UI states the mode beside the number.

## Persistence, export and backup

**Persistence.** One `recordbook.json` in app-private storage, written atomically (temp file
then rename) and versioned with `schemaVersion`. Kotlin serialization is added as a dependency
for exactly this reason: it makes the internal store, the backup file and the test fixtures one
format, so the "back up and restore the whole record" promise is a single code path rather than
three converters. A teacher's data (a few hundred students × a few dozen assignments) stays far
below a megabyte, so a document store is honest here and a database would add migration code
without adding a capability. The reason is recorded because the repo requires one for a new
dependency.

**Export.** CSV via the system create-document picker, drawn by hand with RFC 4180 quoting
(a name containing a comma or a quote must not corrupt the file — tested). PDF via the
framework's `android.graphics.pdf.PdfDocument`, so no heavyweight PDF library is bundled.

**Backup and restore.** The same JSON document, written to and read from a file the teacher
chooses through the picker. Restore validates `schemaVersion` and refuses a document it cannot
read, with a stated reason, instead of partially applying it. This is the answer to the measured
complaint of a user who was "scared to uninstall and reinstall" because the purchase and the
data might not survive.

## Permission and privacy posture

- The app's own manifest declares **no** permission. Import and export go through the Storage
  Access Framework, which needs none; sharing uses `ACTION_SEND` through the Sharesheet.
- **The merged manifest is read, not assumed.** The billing capability's manifest contributes
  nothing, but a transitive SDK can still add one, so the merged release manifest's requested
  permissions are inspected after the first build and the observed list is recorded in
  `store/checklist.md`. This is written as a check with a recorded observation rather than as a
  claim, because "the library needs no permission" is a statement about the library, not about
  the artifact we ship.
- The only network exchange in the product is the Play Billing purchase or restore call, which
  carries a product ID and a purchase token and no student data. All record work — including
  offline — happens locally.
- Student names and grades never leave the device unless the teacher exports or shares them, and
  the listing says so plainly. Whether Play treats such a listing as child-directed, and therefore
  whether the Families Policy Requirements apply, is an open question recorded in the proposal
  and must be resolved against the live policy text before the store pack is written.

## Billing

One non-consumable product (`recordbook_pro`) unlock: unlimited classes and students, weighted
categories, CSV/PDF export, backup and restore. The free tier is genuinely usable — one class,
up to 20 students, unlimited grades — so a teacher can complete real work before paying. The
unlock is offered at the two moments the teacher actually hits a limit: creating a second class,
or the first export.

The implementation uses the `billing` capability and never a Billing SDK type directly, with a
DataStore-backed `BillingStateStore` so the entitlement survives process death — the failure
mode where a paying teacher offline would be dropped to the free tier. The release acknowledges
pending purchases on cold start, because an unacknowledged purchase is refunded by Play after
three days and reads as a lost sale.

### Deviation: the paywall states no price of its own

The requirement originally said the paywall would "show the real localized price before purchase".
That is not implementable against the capability as it stands: `BillingController` exposes
entitlements, wallet and availability, and no product-details or price surface, so the process has
no localized price to display. The three options were to hard-code a number, to extend the shared
capability, or to narrow the requirement, and the choice was made on honesty first:

- A hard-coded "$6.99" is wrong for every user outside the dollar zone, and a wrong price on a
  paywall is worse than no price.
- Extending `billing` with a price surface is the right long-term fix and is worth doing for the
  next slot that sells anything. It is not done here because it means a capability version bump and
  republish while this slot's first build is still running, and it does not change whether the app
  can be sold: Play's own confirmation sheet always shows the localized price before the user
  confirms, and the listing carries the price.

So the paywall says the unlock is a one-time purchase and that Google Play shows the exact price
on the confirmation sheet it opens, and the capability's own policy note — "provide the price and
renewal terms in the listing, not only inside the app" — is satisfied by the listing. Recorded here
rather than silently dropped, because a requirement quietly weakened is how the last slot shipped
with no revenue mechanism at all.

## Validation and release strategy

### Deviation: "no cloud" became "nothing sent to us"

The proposal's wedge #6 and the ideation brief both say **"No account, no cloud, nothing
transmitted."** The shipped app says something narrower, and the reason is worth recording because
the stronger claim was shippable and was rejected on purpose.

`allowBackup=true` plus the scaffold's inherited `<include domain="file" path="." />` put the
teacher's `recordbook.json` — pupil names and marks — into Android Auto Backup, which copies it to
the teacher's own Google Drive. Excluding it was a one-line change. It was not made, on the owner's
decision, because the promise that actually matters to the measured users is different from the
literal one: a teacher who drops their phone and reinstalls should get their term back, and gating
that behind the unlock would mean the free-tier teacher loses it. So the app keeps Auto Backup and
the positioning narrows to what is true and still differentiating — **no account, no ads, nothing
sent to us** — which is the claim the 500K+ leader sells on in the first place (evidence §2.2).

What this changed, and all of it had to change together:

- `store/privacy.md` states the Auto Backup path plainly instead of claiming no cloud, and names
  the two files that can be copied.
- `res/xml/data_extraction_rules.xml` and `res/xml/backup_rules.xml` now enumerate those two files
  explicitly instead of including the whole `file` domain, so the declaration and the rules are the
  same fact.
- The listing claims offline operation, not cloudlessness.

### Deviation: the merged manifest declares INTERNET

Reading the merged release manifest, rather than assuming, found four permissions where the slot's
own manifest declares none: `com.android.vending.BILLING`, a generated AndroidX permission, and —
unexpectedly — `android.permission.INTERNET` and `ACCESS_NETWORK_STATE`, contributed by
`com.google.android.datatransport`, a transitive dependency of Play Billing, along with a
`TransportBackendDiscovery` service.

This does not change any user-visible behaviour: the app's own code has no HTTP client and no URL,
and `network_security_config.xml` forbids cleartext. It does change what may be *written*: a privacy
policy claiming the app has no internet access would have been false, and a Data safety form
claiming nothing is collected while a Google SDK declares INTERNET is the kind of under-declaration
that costs an account. Both documents were rewritten to name the SDK and the permission, and the
comment in `AndroidManifest.xml` that asserted "the only entry is BILLING" was corrected — it was
the false source the wrong claim came from.

The domain layer is pure Kotlin with no Android dependency, so it is tested on the JVM:

- every clause of the arithmetic contract above, including the empty-category, all-exempt,
  drop-lowest and rounding-mode cases, and the "not enough data" state;
- deletion actually removing a class, student, assignment and grade cell;
- CSV quoting against commas, quotes, newlines and non-ASCII names;
- JSON backup round-trip, and a rejected restore for a wrong `schemaVersion` or malformed
  document;
- the free-tier boundary (second class blocked, 21st student blocked) and that a confirmed
  purchase lifts both.

Release evidence ends with the truthful store pack, green `verify-slot.mjs --strict-store`,
`check-economics.mjs --slot`, a complete diff read, and `/eval`. The human alone completes the
legal declarations, the first upload and publication.
