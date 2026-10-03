# Swim set builder

## Purpose

Give a pool swimmer a private, offline set builder he owns outright: he says how long he has, how long
the pool is, what he is training, which strokes to avoid and what gear is actually on the deck, and the
app computes the whole session from authored programming rules, puts a target time on every interval
from a guided pace test, and hands the set list over as text or a printable card. Every session is
computed on the device with no account and no network, and the app is bought once rather than rented.

## ADDED Requirements

### Requirement: The session is computed from the swimmer's constraints

The app SHALL compute each session from the swimmer's own constraints — pool length, minutes available,
training focus, strokes to avoid and gear on the deck — against programming rules the app ships, and
SHALL NOT serve a session from a fixed rotating library. The computation SHALL be deterministic: the
same constraints and the same pace SHALL produce the same session. The generated session SHALL NOT use
a stroke listed among the strokes to avoid, SHALL use no gear that is not on the deck, and SHALL state
its own estimated total so that "does this fit the minutes I have?" is answerable without re-adding the
sections.

#### Scenario: Two different constraint sets produce different main sets

- **WHEN** the swimmer generates a session for 45 minutes in a 25 m pool and then a session that differs
  only in the pool length
- **THEN** the two main sets are not the same arrangement of distances, repetitions and strokes

#### Scenario: An avoided stroke never appears

- **WHEN** the swimmer lists a stroke among the strokes to avoid
- **THEN** no interval in the generated session uses that stroke

#### Scenario: Gear that is not on the deck is never used

- **WHEN** the swimmer declares only a kickboard as the gear on the deck
- **THEN** every interval that uses gear uses only the kickboard

#### Scenario: The session fits the minutes offered

- **WHEN** the swimmer asks for a 45-minute session
- **THEN** the session's own estimated total is at most 45 minutes

#### Scenario: The same constraints produce the same session

- **WHEN** the swimmer generates twice with identical constraints and the same pace
- **THEN** both sessions are identical

### Requirement: The guided pace test drives the target on every interval

The app SHALL walk the swimmer through a guided 400 m and 200 m time trial, SHALL compute a threshold
pace per 100 m from the two recorded times, and SHALL print a target time on every interval of every
session generated afterwards, so no interval is a bare distance with no time to swim it in. Before the
trials are completed the app SHALL print a target from a shipped default and SHALL label it as a default
rather than the swimmer's own pace. The computed pace SHALL persist across restarts and SHALL be reused
in later sessions without asking again. A recorded pair that cannot yield a positive pace SHALL be
refused rather than turned into a target time.

#### Scenario: The two trials produce a threshold pace

- **WHEN** the swimmer records a 400 m time and a 200 m time through the guided test
- **THEN** the app states a threshold pace per 100 m derived from those two times

#### Scenario: Every interval carries its target

- **WHEN** a session is generated after the pace is known
- **THEN** every interval shows a target time, and no interval shows a bare distance with no time

#### Scenario: The pace survives a restart

- **WHEN** the swimmer completes the test, closes the app and reopens it to build another session
- **THEN** the target times use the same threshold pace that was computed before the restart

#### Scenario: An impossible trial pair is refused

- **WHEN** the recorded 400 m time is not slower than the recorded 200 m time
- **THEN** the app refuses the pace instead of printing a target time

**Honest boundary, recorded rather than glossed.** The free focus is a real session, not a preview: the
`paidUnit` of the plan lists the guided trials among what is bought, while the moment of payment is
after a session has been swum. The app therefore ships a conservative default pace so the free session
carries a target on every interval, and the guided trials replace that default with the swimmer's own
measurement behind the single unlock. The default value is authored from the programming rules and is
stated in the UI as a default; this change does not name a number.

### Requirement: A coach's written workout is parsed into the same session

The app SHALL accept a workout written as text the way a coach writes a practice and SHALL parse it into
the same structured, paced session a generated one is — sections, intervals, and a target time derived
from the swimmer's own pace. The parse SHALL read section headers, repetitions and distances, stroke
words, send-offs and explicit rests, and SHALL skip a line with no readable distance rather than
guessing at it.

#### Scenario: A pasted practice becomes a structured session

- **WHEN** the swimmer pastes a practice with a warm-up, a main set and a cool-down, carrying
  repetitions, distances, strokes and send-offs
- **THEN** each readable line becomes an interval in the matching section with a target time

#### Scenario: An unreadable line is skipped, not guessed

- **WHEN** the pasted text contains a line with no readable distance
- **THEN** that line produces no interval and the rest of the practice still parses

### Requirement: The finished session leaves the phone

The app SHALL give the swimmer two ways to take a finished session with him, both through system intents
and neither requiring a storage permission: SHALL share the whole set list as plain text through a
send action, and SHALL write a one-page printable card through the system file picker using the
framework PDF writer. Both SHALL carry every section, every interval, and on every interval its
distance, repetitions, stroke, target time and rest, together with the pace the targets derive from.

#### Scenario: Share as text carries the whole set list

- **WHEN** the swimmer shares a session through the share action
- **THEN** the shared payload contains every section and every interval with its target time

#### Scenario: Print writes a one-page card the swimmer keeps

- **WHEN** the swimmer chooses print and picks a location through the system file picker
- **THEN** a PDF card is written to that location containing the same set list

#### Scenario: The handoff needs no storage permission

- **WHEN** the app's own `AndroidManifest.xml` is read
- **THEN** it declares no storage permission for the share or the print path

### Requirement: The session survives an interruption

The app SHALL restore an in-progress session — the same set, the same target times and the same position
in the set — after the app is interrupted by a call, a rotation or a trip back to the locker, or after
the process is recreated. A stored position that no longer points inside the session SHALL be clamped to
a real interval rather than restoring a position past the end. A missing or unreadable stored session
SHALL read as "nothing to resume" rather than crashing.

#### Scenario: A session comes back where it was left

- **WHEN** the swimmer advances part way through a session, the process is recreated and the app is
  reopened
- **THEN** the same set, the same targets and the same position in it are restored

#### Scenario: A corrupt stored session costs the session, not the app

- **WHEN** the stored session payload is truncated or otherwise unreadable
- **THEN** the app opens normally and reports that there is nothing to resume

### Requirement: The app is offline by construction

The app SHALL compute a session, run the pace arithmetic and produce the share or print handoff with the
device in airplane mode and with no account created. It SHALL NOT declare an `INTERNET` permission in
its own manifest, SHALL NOT contain a login, a sign-up, a sync or an account concept anywhere in its
user interface, and SHALL keep every session and the pace on the device.

#### Scenario: A session is computed in airplane mode

- **WHEN** the device has no network and no account exists, and the swimmer builds a session
- **THEN** the session is computed and handed over with no error and no retry prompt

#### Scenario: The app's own manifest asks for no internet

- **WHEN** the app's own `AndroidManifest.xml` is read
- **THEN** it declares no `INTERNET` permission

#### Scenario: The purchase path is the only thing that ever needs the network

- **WHEN** the merged manifest of the built application is read
- **THEN** any network permission present is one the Play Billing path requires and the app's own
  manifest did not ask for, and the session, the pace and the handoff are unaffected by whether the
  device is online

**Honest boundary, recorded rather than glossed.** The promise is *no account and no network in the
daily path*, not "the app is incapable of using the network". Play Billing's `datatransport` dependency
contributes network permissions to the merged manifest even when the app declares none — measured on
the factory's reference slot — so the merged manifest is read from the built artifact and the claim is
corrected if the artifact disagrees.

### Requirement: The app is bought once, with no subscription and no ads

The app SHALL offer exactly one in-app product: a **non-consumable** unlock. The unlock SHALL be offered
at the first locked training focus and at the first use of the coach import or the share-and-print
handoff, SHALL state that it is a one-time purchase, and SHALL present no trial clock, no subscription
wording and no renewal. There SHALL be no advertising and no second paywall anywhere in the app. The
unlock SHALL be restorable after a reinstall with the same Play account. At least one training focus
SHALL be fully usable — a computed session with a target on every interval — before any purchase.

#### Scenario: The free session is a real session

- **WHEN** the swimmer has not purchased anything and builds a session in the free focus
- **THEN** the session is generated with a target time on every interval and is not cut off part way

#### Scenario: A locked focus offers the single unlock

- **WHEN** the swimmer selects a focus that is behind the unlock
- **THEN** one sheet is shown stating a one-time purchase, and the focus is available once the unlock
  completes

#### Scenario: There is no subscription anywhere

- **WHEN** the shipped strings and the billing product list are read
- **THEN** no string offers, implies or refers to a subscription, a renewal or a trial

#### Scenario: The unlock survives a reinstall

- **WHEN** the swimmer reinstalls the app on the same Play account that purchased the unlock
- **THEN** every locked focus, the coach import and the handoff are available again

### Requirement: The set list is usable one-handed on a pool deck

The app SHALL keep every control on the builder and the session screens reachable with a thumb and at
least 48 dp in its smallest dimension, SHALL state each fact that matters — a locked focus, a target
time, a completed interval — with a word or a shape in addition to colour alone, and SHALL keep text
legible against its background at a contrast ratio of at least 4.5:1.

#### Scenario: Lock state is not carried by colour alone

- **WHEN** a swimmer who cannot distinguish the palette's hues reads the builder
- **THEN** each locked focus still states that it is locked, because the state is written and shaped
  rather than only coloured

#### Scenario: Every control meets the target floor

- **WHEN** the builder and the session screens are measured
- **THEN** every interactive control is at least 48 dp in both dimensions

### Requirement: Every stored preference changes real behaviour

The app SHALL provide a settings surface reachable from the main screen, and every preference in it
SHALL change behaviour the swimmer can observe. The app SHALL NOT contain a setting that is stored and
then read by nothing.

#### Scenario: A setting has an effect

- **WHEN** the swimmer changes a preference offered in settings
- **THEN** the change is visible in the app's behaviour without a restart, and it survives one

## MODIFIED Requirements

None. This change adds a new capability and modifies no existing spec.
