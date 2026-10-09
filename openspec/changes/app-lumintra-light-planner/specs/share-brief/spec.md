# Share the session brief

## Purpose

Give the user the end of the job: the finished session brief leaves the phone, as plain text through
the system share sheet or as a postcard composed from the very scene on screen, so a night's plan can
be read in the field, handed to a trip companion, or posted — without a second app, without a storage
permission and without a server. Nothing about the send is inferred, promised or guessed: the payload
carries the facts the app computed for the chosen spot, night and lens, it states the age of every
forecast and feed input, and the user sees exactly what will leave before it does.

## ADDED Requirements

### Requirement: The text brief carries the whole session off the device

The app SHALL hand the finished session brief off the device as plain text through the Android share
sheet, by building an `Intent.ACTION_SEND` with `type` `text/plain` and the brief in
`Intent.EXTRA_TEXT`, so the recipient is chosen by the user in the system chooser.

The payload SHALL carry, in this order:

- the app's name `Lumintra`;
- the spot: its name, and its latitude and longitude in decimal degrees to four decimals with an
  N/S and E/W hemisphere, and the spot's zone id;
- the date: the spot-local calendar date of sunset, named as the night spanning two days;
- the dark window and its kind, from `DarkWindowReport` — when `DarkWindowKind` is `DARK`, the
  moonless astronomical-dark interval as spot-local clock times to the minute; otherwise the
  sentence the kind requires (requirement below);
- the twilight boundaries: civil (−6°), nautical (−12°) and astronomical (−18°) as spot-local clock
  times to the minute;
- the Moon's state from `MoonState`: its illuminated fraction to the whole percent and its named
  phase, and its state during the dark window (`up all night`, `below the horizon all night`,
  `rises at hh:mm` or `sets at hh:mm`), with its lowest altitude in whole degrees and its time when
  it is up all night;
- each chosen target, from `TargetWindow`: its designation, its common name from the localised
  catalogue, the interval it is above the spot's drawn horizon during the dark window as spot-local
  clock times to the minute, and its peak altitude in degrees to one decimal;
- each satellite pass, from `SatellitePass`: its start time in spot-local clock to the minute, its
  maximum altitude in whole degrees, its duration in minutes or seconds, its observability state
  (`visible`, or `occurs, not visible` with its reason), and the element-set age from `ElementSetAge`
  as `elements <N> h old`;
- the cloud outlook, from `CloudReport`: the hourly total cloud cover for the dark window as whole
  percentages against local hours, labelled a forecast, with its source, its retrieval or model time
  and its age from `ForecastAge`;
- the lens used, from `LensPreset` and `FieldOfView`: the focal length in millimetres, the sensor
  size, and the horizontal and vertical field of view in degrees to one decimal.

Every line SHALL be a complete sentence naming its quantity, its unit and, where it comes from a
model or a feed, its source and the age of that input; the payload SHALL NOT carry a bare number with
no unit and no label. The only abbreviations permitted are the object designations (`M31`, `NGC224`)
and unit symbols.

#### Scenario: The shared text holds every fact

- **WHEN** the user shares the brief for a spot with a chosen target, a satellite pass and a forecast
- **THEN** the payload contains the spot and its coordinates, the date, the dark window, the three
  twilight boundaries, the Moon's state, the target with its window, the pass with its element-set
  age, the cloud outlook with its model time, and the lens — each naming its unit

#### Scenario: Every forecast input states its age

- **WHEN** the brief carries a cloud outlook and a satellite pass
- **THEN** the cloud line names its retrieval time and its age, and the pass line names its
  element-set age, and neither value appears without its age

#### Scenario: No bare number leaves the app

- **WHEN** the payload is read
- **THEN** every numeric value is followed by its unit, and no forecast- or feed-derived value appears
  without its source and its age

#### Scenario: The payload names the app

- **WHEN** the payload is read
- **THEN** it carries the app's name `Lumintra`

### Requirement: The brief tells the truth about a night with no dark window

When the `DarkWindowKind` is not `DARK`, the app SHALL NOT print a dark window. It SHALL print one
sentence naming the deepest twilight level the night actually reaches where the kind has one, the
Sun's altitude at that instant in degrees to one decimal, and the instant in spot-local clock to the
minute. For `NO_ASTRO_DARK` the sentence names nautical twilight; for `POLAR_DAY` the sentence states
that the Sun does not set tonight and that there is no darkness, as `session-brief`'s `NoSunsetTonight`
states for that kind; for `NO_DARK` the sentence states that no darkness occurs because the Sun stays
above civil twilight, and gives the Sun's lowest altitude and its time, as `session-brief`'s `NoDark`
states for that kind; for `POLAR_NIGHT` it states that the Sun does not rise and gives the deepest
dark interval of the civil day. The app SHALL NOT collapse `POLAR_DAY` and `NO_DARK` into one
statement: `POLAR_DAY` is a night whose Sun never sets, while `NO_DARK` is a night whose Sun does
cross the horizon but never reaches civil twilight, and the shared image SHALL state each state's own
sentence so the two are distinguishable from the text alone. The app SHALL
NOT print a zero-length interval, a `00:00–00:00` window, or a black sky with stars for a night that
never gets dark. The same sentence SHALL be used by the text brief, the postcard overlay, the `Brief`
screen and the `Tonight` summary, so the four cannot disagree.

#### Scenario: No astronomical darkness is stated, not faked

- **WHEN** the brief is composed for a spot and night whose `DarkWindowKind` is `NO_ASTRO_DARK`
- **THEN** the payload carries the sentence naming the deepest level reached and the Sun's altitude at
  its time, and carries no dark-window interval

#### Scenario: A `POLAR_DAY` night states that the Sun does not set

- **WHEN** the brief is composed for a night whose `DarkWindowKind` is `POLAR_DAY`, a night whose Sun
  never sets
- **THEN** the payload's sentence states that the Sun does not set and that there is no darkness, and
  no dark-window interval and no night-shaped line appear

#### Scenario: A `NO_DARK` night states that the Sun never reaches civil twilight

- **WHEN** the brief is composed for a night whose `DarkWindowKind` is `NO_DARK`, a night whose Sun does
  cross the horizon but whose lowest altitude stays above civil twilight
- **THEN** the payload's sentence states that no darkness occurs because the Sun stays above civil
  twilight, gives the Sun's lowest altitude and its time, and carries no statement that the Sun stays
  above the horizon or never sets

#### Scenario: The postcard overlay agrees with the words

- **WHEN** the postcard is composed for the same night
- **THEN** its overlay names the same level sentence and no dark window, so the picture and the words
  agree

### Requirement: The postcard is a real rendering of the scene the app shows, and it carries ESO's credit on the image

The app SHALL compose the postcard from the rendered scene at the chosen instant, by rendering the
same `SceneFrame` the `Night` screen draws for the same spot, night, instant and lens, through the
same twilight LUT, the same tonemap and the same slot palette. The postcard SHALL NOT be a mock-up, a
stock image, a stored screenshot of a different revision, or a hand-composed frame around a plain
screen. It SHALL be **1080 × 1350 px**, PNG, sRGB. It SHALL carry an overlay reading the spot name,
the spot-local date, the instant to the minute, the lens as its focal length in millimetres and its
horizontal field of view in degrees to one decimal, and the Moon as its illuminated fraction in whole
percent and its named phase. Because the Milky Way band is rendered from the ESO panorama `eso0932a`,
the string **`ESO/S. Brunier`** SHALL appear as text **on the image itself or immediately beside the
Milky Way image on the postcard**, unaltered and never separated from that image, at a WCAG contrast
ratio of at least 4.5:1 against its immediate background and legible when the image is shown 90 px
wide; it SHALL NOT be removable or obscurable by the user, and SHALL NOT be cropped out. The postcard
SHALL also carry the source link `https://www.eso.org/public/images/eso0932a/`, SHALL NOT state or imply
that ESO endorses the app, and SHALL, when the Milky Way image or the postcard composition has been
modified — including AI-modification — state that modification on the card.

#### Scenario: The postcard is the scene, not a mock-up

- **WHEN** a postcard is composed for a spot, night and instant, and the same `SceneFrame` is rendered
  on the `Night` screen
- **THEN** the postcard's sky, sampled at the same angular positions, matches the on-screen scene to
  within a CIELAB ΔE00 of 2, and the postcard's horizon line is drawn from the same `HorizonProfile`
  the scene drew

#### Scenario: The postcard has the declared size and overlay

- **WHEN** the composed postcard bitmap is measured
- **THEN** it is 1080 × 1350 px PNG in sRGB, and its overlay carries the spot, the date, the instant
  to the minute, the lens and the Moon

#### Scenario: The ESO credit is on the image itself

- **WHEN** the composed postcard bitmap is read
- **THEN** the string `ESO/S. Brunier` is present as pixels on the image, and no user control removes
  or obscures it

#### Scenario: The credit is legible at thumbnail size

- **WHEN** the postcard is scaled so its width is 90 px
- **THEN** the `ESO/S. Brunier` text is still legible, and its contrast against its immediate
  background is at least 4.5:1

#### Scenario: The credit is not separated from the image, and no endorsement is implied

- **WHEN** the composed postcard bitmap is read
- **THEN** the `ESO/S. Brunier` credit sits on or immediately beside the Milky Way image and is never
  separated from it, and the source link `https://www.eso.org/public/images/eso0932a/` is present
- **AND** no ESO endorsement of the app is stated or implied

#### Scenario: A modified image is declared on the card

- **WHEN** the Milky Way image or the postcard composition has been modified
- **THEN** the card states that it has been modified, as CC BY 4.0 requires

### Requirement: The postcard goes out through FileProvider with a temporary grant

The app SHALL write the postcard PNG into its own cache directory and expose it to the share sheet
only through a `FileProvider` `content://` URI, granted with
`Intent.FLAG_GRANT_READ_URI_PERMISSION` on the `ACTION_SEND` intent. The provider SHALL be declared
`android:exported="false"` with `android:grantUriPermissions="true"`, and its `file_paths.xml` SHALL
expose only a `<cache-path>` subdirectory — never `<root-path>`, never an `<external-path>`. The app
SHALL NOT share a `file://` URI. The postcard intent SHALL set `type` to `image/png` and
`Intent.EXTRA_STREAM` to the content URI, and MAY carry a one-line `Intent.EXTRA_TEXT` naming the
spot and the date.

#### Scenario: The shared postcard is a content URI

- **WHEN** the postcard share intent is inspected
- **THEN** its `EXTRA_STREAM` is a `content://` URI whose authority is the app's `FileProvider`, and
  the intent carries `FLAG_GRANT_READ_URI_PERMISSION`

#### Scenario: The provider is not world-readable

- **WHEN** the app's own `AndroidManifest.xml` and its `file_paths.xml` are read
- **THEN** the provider is `android:exported="false"` with `android:grantUriPermissions="true"`, and
  no `root-path` or `external-path` is exposed

#### Scenario: No file URI is ever shared

- **WHEN** the source of the share path is read
- **THEN** no `file://` URI is constructed for either handoff

### Requirement: The Share screen shows the exact payload before it leaves

The app SHALL provide a `Share` screen (`ui/share/ShareScreen.kt`) that previews the exact payload
before it is sent and offers the choice between the text brief and the postcard. The text preview
SHALL render the exact string that will be placed in `EXTRA_TEXT`, selectable and scrollable; the
postcard preview SHALL render the exact bitmap that will be sent. The send control SHALL be disabled
until the preview for the selected option has been rendered. The payload previewed SHALL equal the
payload sent: for the text, the same characters; for the postcard, the same pixels.

#### Scenario: The preview is the payload

- **WHEN** the user previews the text brief and then sends it
- **THEN** the characters in `EXTRA_TEXT` are identical to the characters previewed

#### Scenario: The choice is offered before any send

- **WHEN** the `Share` screen is opened
- **THEN** both options are present, named and at least 48 dp, and the selected option's preview is
  shown before a send control is enabled

#### Scenario: Changing the plan updates the preview

- **WHEN** the user changes the lens or the chosen target and returns to `Share`
- **THEN** the preview shows the changed value, and the send control is re-enabled only after the
  preview is re-rendered

### Requirement: Neither handoff asks for a storage permission, and no temporary file survives it

Neither the text brief nor the postcard SHALL require a storage permission. The app's own
`AndroidManifest.xml` SHALL declare none of `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`,
`MANAGE_EXTERNAL_STORAGE`, `READ_MEDIA_IMAGES` or `READ_MEDIA_VISUAL_USER_SELECTED` for these paths.
Any temporary file SHALL be written inside the app's own cache directory and deleted when the share
returns, and any file left by a cancelled or interrupted share SHALL be deleted on the next app
start; the cache SHALL be the only location the share path writes.

#### Scenario: The manifest asks for no storage permission

- **WHEN** the app's own `AndroidManifest.xml` is read
- **THEN** it declares none of the storage permissions named above

#### Scenario: The temporary file is cleaned up

- **WHEN** a postcard share completes, or is cancelled, and the app is reopened
- **THEN** no postcard file remains in the app's cache directory

#### Scenario: The share path writes only to the cache

- **WHEN** the source of the share path is read
- **THEN** every temporary file it writes is created under `context.cacheDir`

### Requirement: The brief never states more than the app knows

The brief SHALL carry only values the app computed for the chosen spot, night and lens, and SHALL
state a limit in words wherever the app cannot know an outcome. It SHALL NOT promise clear skies: the
cloud line SHALL be labelled a forecast and SHALL NOT use language asserting an outcome. It SHALL NOT
present an unlabelled prediction: any forecast- or feed-derived value SHALL carry its source and its
age. It SHALL NOT claim that a satellite streak is certain: a pass SHALL be stated in the two-state
`occurs` / `occurs, not visible` form, and the app SHALL NOT assert that the satellite will appear in
the user's frame. Where a pass occurs but is not observable — the satellite is in Earth's shadow, or
the sky is too bright — the brief SHALL state that, rather than dropping the pass or offering it as
visible. The rule is the confident-wrong rule: a wrong "clean" or "clear" is worse than no statement
at all.

#### Scenario: No clear-sky promise

- **WHEN** the cloud line is read
- **THEN** it names the cover as a forecast with its source and its age, and contains no wording that
  promises clear skies or any other outcome

#### Scenario: A pass that is not observable is stated, not dropped

- **WHEN** a pass occurs while the satellite is in Earth's shadow
- **THEN** the payload contains the pass with the state `occurs, not visible` and the reason, and does
  not present it as visible

#### Scenario: No unlabelled prediction

- **WHEN** every forecast- or feed-derived value in the payload is read
- **THEN** each carries its source and its age

### Requirement: The handoff is available where the plan exists and refuses where it does not

The `Share` screen SHALL be reachable from the `Night`, `Brief` and `Share` entry points and from the
reminder notification and the widget without a configuration step, and the payload it composes SHALL
be for the same spot and night the entry surface displayed, never a silently different spot. The app
SHALL handle each of the following without emitting a false payload: no spot saved; no target chosen;
no forecast available; no frame rendered yet; a cancelled share; and a device with no share target.

- **No spot.** The `Share` screen SHALL show an empty state naming the reason and a control to open
  `Spots`, the send control SHALL be disabled, and the app SHALL NOT compose a payload for a default
  or `(0, 0)` coordinate.
- **No target chosen.** The brief SHALL omit the targets section and state that no target is chosen,
  and SHALL otherwise be complete.
- **No forecast available.** The cloud line SHALL state the sentence `session-brief` and
  `cloud-forecast` own — `No forecast available.` — with its reason, or, when a cached reading exists,
  the `OfflineReadingFrom` wording `No forecast available. Last reading {age} old: {value} % cloud.`;
  nothing else in the brief SHALL be blocked.
- **No frame rendered yet.** The postcard option SHALL render the scene for the chosen instant on
  demand; if the render fails, the screen SHALL state that the postcard is unavailable and keep the
  text option available, and SHALL NOT send an empty or partially drawn image.
- **Cancelled share.** The app SHALL return to `Share` unchanged, show no error, keep the preview and
  clean up the temporary file; a cancel SHALL NOT be reported as a failure.
- **No share target.** The app SHALL detect that no activity can handle the `ACTION_SEND` intent,
  SHALL state that no app is available, SHALL offer the text brief to the clipboard for the text
  option and `Intent.ACTION_CREATE_DOCUMENT` to save the PNG for the postcard option, and SHALL NOT
  show an empty chooser.

#### Scenario: No spot means no payload

- **WHEN** the `Share` screen is opened with no saved spot
- **THEN** it shows the empty state and disables the send control, and no payload is composed

#### Scenario: No target still sends a complete brief

- **WHEN** the user shares with no target chosen
- **THEN** the payload omits the targets section, states that no target is chosen, and carries every
  other fact

#### Scenario: No forecast does not block the brief

- **WHEN** no forecast is available
- **THEN** the cloud line states `No forecast available.` with its reason, and the rest of the payload
  is complete

#### Scenario: A cancelled share is not a failure

- **WHEN** the user dismisses the share sheet without sending
- **THEN** the app returns to `Share` with the preview intact, shows no error, and leaves no
  temporary file behind

#### Scenario: No share target falls back without an empty chooser

- **WHEN** the device can handle neither `ACTION_SEND` for text nor for an image
- **THEN** the app states that no app is available and offers the clipboard for the text and the
  system file picker for the postcard, rather than showing an empty chooser

#### Scenario: The entry point does not change the spot

- **WHEN** the app is opened from the reminder notification or the widget and the user opens `Share`
- **THEN** the payload's spot and night are the ones the entry surface displayed

### Requirement: The text payload is bounded, and its header is never truncated

The text brief SHALL NOT exceed **8 000 characters**. When the complete brief would exceed it, the app
SHALL drop target rows beyond the first twelve and pass rows beyond the first six, in that order,
appending `…and <N> more` for each list it shortened. The header block — the app's name, the spot and
its coordinates, the date, and the dark window or, in its absence, the sentence the kind requires —
SHALL NOT be dropped under any cap. The postcard SHALL NOT be truncated; if it cannot be composed,
the app SHALL state that (requirement above) rather than send a partial image.

#### Scenario: A short brief is sent whole

- **WHEN** the complete brief fits within 8 000 characters
- **THEN** the sent payload is the complete brief, and no `…and N more` line is added

#### Scenario: A long brief drops only list rows

- **WHEN** the brief would exceed 8 000 characters
- **THEN** at most the first twelve target rows and the first six pass rows are kept, each shortened
  list carries its `…and <N> more` line, and the header block is unchanged

### Requirement: Every string is a resource, and the brief is rendered in the user's language

Every user-facing string on the `Share` screen and in the brief SHALL be a resource in
`app/src/main/res/values/strings.xml` and `app/src/main/res/values-ru/strings.xml`; no user-facing
string SHALL be a literal in Kotlin. The text brief SHALL be rendered in the app's current language
at the moment of composition, and the names of objects SHALL come from the localised fields of the
bundled catalogue (`CatalogueRepository`), falling back to the designation when a locale has no name
for an object. Numbers SHALL be formatted for the app's current locale, and the focal length SHALL
always be in millimetres, never converted. The app's name SHALL be a non-translatable string
resource.

#### Scenario: The brief follows the current language

- **WHEN** the app's language is changed from `en` to `ru` and the brief is shared
- **THEN** the brief's prose is in `ru`, and the app's name is unchanged

#### Scenario: Object names are localised

- **WHEN** a target is shared in a locale whose catalogue carries a name for it
- **THEN** the payload carries that locale's name, and in a locale that carries none it carries the
  designation

#### Scenario: No literal is shipped

- **WHEN** the `Share` screen and the brief formatter are read
- **THEN** every user-facing string is a resource reference, and every one exists in both `values/`
  and `values-ru/`

#### Scenario: Millimetres are never converted

- **WHEN** the app's units are set to imperial and the brief is shared
- **THEN** the focal length is still in millimetres

### Requirement: The handoff is usable with TalkBack and every control is at least 48 dp

Every interactive control on the `Share` screen SHALL be at least **48 dp** in both dimensions and
SHALL carry a TalkBack role and a name. The text preview SHALL have a content description summarising
what will be sent, and the postcard preview SHALL have a content description naming the spot, the
date, the instant, the lens and the Moon. The two options SHALL be presented with a selectable role
and SHALL state the selected option in words, not by colour alone.

#### Scenario: Every control meets the floor

- **WHEN** the `Share` screen is measured
- **THEN** every interactive control is at least 48 dp in both dimensions and has a name and a role

#### Scenario: The preview is announced

- **WHEN** TalkBack focuses the postcard preview
- **THEN** it announces the spot, the date, the instant, the lens and the Moon

#### Scenario: The choice is not carried by colour alone

- **WHEN** a user who cannot distinguish the palette's hues reads the choice
- **THEN** the selected option is stated in words

### Requirement: The share path transmits nothing to the app's author

The share path SHALL transmit nothing to the app's author: the user chose the recipient, and the app
SHALL NOT send the brief or the postcard anywhere by itself. The app SHALL contain no analytics, no
tracking and no server, and the share path SHALL make no network request. The text and postcard
handoffs SHALL both complete with the device in airplane mode.

#### Scenario: The handoff works in airplane mode

- **WHEN** the device has no network and the user shares the text brief and then the postcard
- **THEN** both complete with no error and no retry prompt

#### Scenario: No analytics or tracking is declared

- **WHEN** `app/build.gradle.kts` and `gradle/libs.versions.toml` are read
- **THEN** no analytics or tracking library is declared

#### Scenario: The share path makes no network call

- **WHEN** the source of the share path is read
- **THEN** it performs no network request and names no server

**Honest boundary, recorded rather than glossed.** The tokens this capability resolves on are textual:
a requirement names `Intent.ACTION_SEND` and `FileProvider`, and a test can assert the token is
present in `ui/share/ShareScreen.kt` and that the shared text holds the payload — it cannot prove the
recipient's app rendered that payload, that the user pressed send, or that the image arrived
unaltered. "A real rendering, not a mock-up" is provable only by comparing the composed postcard's
pixels to the same `SceneFrame` rendered on `Night` (the ΔE00 ≤ 2 scenario above); a postcard
composed from a stored screenshot could still pass every string check, so the deliberate pixel
comparison is the check that matters. The ESO credit is checked as pixels on the composed bitmap,
never in the recipient's view of it, and no program here can force a recipient's app to display the
credit. The no-storage-permission claim is read from the app's own `AndroidManifest.xml`; a library
could still contribute a permission to the merged manifest, so the merged manifest of the built
artifact is what must be read when the claim matters. The language of the brief is the sender's at
composition time, so a recipient in another language receives the sender's language — a stated limit
of a text handoff, not a defect to fix here.

## MODIFIED Requirements

None. This change adds a new capability and modifies no existing spec.
