# factory-core Specification

## Purpose
Defines the deterministic surface of the factory: the facts a program can settle without reading
prose, the single command that settles them, and the budget that keeps that surface from growing
back into the harness this change exists to remove.

## Requirements

### Requirement: One entry point runs every fact check

The factory SHALL provide exactly one verification entry point that runs every fact check the core
owns, in one process, and prints one row per fact with an explicit verdict. The verdict vocabulary
SHALL distinguish `PASS`, `FAIL`, `SKIP` and `UNVERIFIED`, and `UNVERIFIED` SHALL never be printed
as `PASS`. The entry point SHALL NOT invoke another verification script or another test suite: it
SHALL import the checks it runs. The only subprocess it may spawn is the build tool, and only when
a build is requested.

#### Scenario: A clean run is one command and one process

- **WHEN** the verification entry point runs against a slot without the build flag
- **THEN** it executes every fact check in-process, prints each fact's verdict, prints no `PASS` for
  any fact it did not actually evaluate, and exits non-zero if any fact is `FAIL`

#### Scenario: An unbuildable revision is not a pass

- **WHEN** the entry point is run without the build flag
- **THEN** every fact that requires a compiled artifact reports `UNVERIFIED` and the run is not
  reported as passing

#### Scenario: No nested runners

- **WHEN** the repository is searched for a verification or test script that invokes another
  verification or test script
- **THEN** the search finds none, because checks are functions the entry point calls

### Requirement: The verification run reports its own cost

The entry point SHALL print the wall-clock duration of the run and the number of subprocesses it
spawned, and a build it performs SHALL report the build duration and the resolved JDK. A claimed
change in run cost SHALL be reported from this output rather than asserted.

#### Scenario: Cost is visible without a stopwatch

- **WHEN** any verification run finishes
- **THEN** its output states the elapsed duration and the subprocess count

### Requirement: The Play asset contract is enforced with the console's own windows

The core SHALL enforce, per asset kind and independently of one another:

- app icon: 512×512, 32-bit PNG with alpha, at most 1024 KB, with no drawn rounded mask or shadow;
- feature graphic: exactly 1024×500, JPEG or 24-bit PNG without alpha, at most 15 MB;
- every screenshot: an aspect ratio of 16:9 or 9:16, decided by integer cross-multiplication;
- phone screenshots: 2–8 accepted, at least 4 required, each side 320–3840 px, at most 8 MB each;
- 7-inch screenshots: 4–8 required, each side 320–3840 px, at most 8 MB each;
- 10-inch screenshots: 4–8 required, each side 1080–7680 px, at most 8 MB each.

The core SHALL also enforce the pack's declared inventory counts, so that a pack that ships fewer or
more screenshots than it is composed for is a failure rather than a smaller pack.

#### Scenario: A legal tablet asset is not rejected

- **WHEN** a pack contains a 1080×1920 image in the 7-inch field
- **THEN** the 7-inch size window accepts it, because that field's window is 320–3840 and the
  1080 px floor belongs to the 10-inch field

#### Scenario: An aspect ratio outside the console's sentence fails

- **WHEN** a screenshot is 1200×1920
- **THEN** the check reports a failure naming the image, its dimensions and both accepted ratios

#### Scenario: A wrong count fails

- **WHEN** a composed pack declares six phone screenshots and ships five
- **THEN** the check fails and names the expected count

### Requirement: Composed media carry real pixels

Every composed image the pack ships SHALL have a luminance standard deviation of at least 6, and
every screenshot SHALL additionally carry at least 12 distinct quantised colours. A solid fill, a
one-colour placeholder or a failed render SHALL fail, at any size.

#### Scenario: A solid colour is refused

- **WHEN** a pack contains a uniform 1080×1920 PNG in a screenshot field
- **THEN** the check fails, naming the image and the measured statistic

#### Scenario: A flat two-tone mark is not refused

- **WHEN** the app icon is a legitimate two-colour mark
- **THEN** the colour-count floor does not apply to it and it is not failed for low colour count

### Requirement: The launcher mark survives the adaptive-icon masks

The shipped adaptive-icon foreground SHALL be the same mark as the store icon, drawn on one
construction grid, and its furthest reach from the viewport centre SHALL stay inside the 33 dp safe
radius of the 108 dp viewport. The launcher icon and the store icon SHALL be rendered from one
declared mark, and their agreement SHALL be measured rather than assumed. A mark assembled from
absolute pixel constants, an emoji, or a stock image SHALL NOT be the app's mark.

#### Scenario: A mark outside the safe zone is refused

- **WHEN** the adaptive-icon foreground's path data reaches beyond the safe radius
- **THEN** the check reports the measured reach and fails, naming the mask that would crop it

#### Scenario: Store icon and launcher mark disagree

- **WHEN** the rendered store icon and the rasterised launcher foreground differ in the mark's
  coverage of the canvas beyond the declared tolerance
- **THEN** the check fails

#### Scenario: The geometry is read from the shipped file

- **WHEN** the check runs
- **THEN** it reads the path data out of the shipped foreground resource rather than from a copy, so
  it cannot pass on a stale geometry

### Requirement: The shipped client carries no paid-provider credential

The core SHALL scan the slot's client sources, its build files and its service-account file for
provider credential patterns, paid-provider SDK coordinates and direct calls to paid hosts, and
SHALL report a finding with the file and the rule. A slot with no client sources SHALL report
`UNVERIFIED`, never a pass. An exemption SHALL be a checked-in file naming the path, the rule, a
reason and the decider, and an exemption with no reason or no decider SHALL be a failure.

#### Scenario: A key in a resource file is found

- **WHEN** a provider key literal appears in a string resource, a version catalog or a ProGuard file
- **THEN** the scan reports a finding naming the file and the rule

#### Scenario: A documented placeholder does not fail

- **WHEN** a source file contains a value that is a documented placeholder
- **THEN** the scan reports a note and does not fail

#### Scenario: Nothing scanned is not a pass

- **WHEN** the slot has no client source directory
- **THEN** the scan reports `UNVERIFIED` and exits with the unverified status

### Requirement: The privacy policy is reachable from inside the app

A slot that declares a privacy policy URL SHALL let the user open that policy from inside the
application. Privacy prose in an informational screen SHALL NOT satisfy this requirement.

#### Scenario: The declared policy is reachable in-app

- **WHEN** the core inspects a slot that declares a privacy policy URL
- **THEN** it finds a reachable in-app path to that policy, or it fails and names the missing half

### Requirement: 16 KB page-size readiness is measured from the artifact

Readiness SHALL be measured from the built artifact — the ELF `p_align` of each loadable segment,
plus the stored-and-aligned container offset in the package — and SHALL NOT be inferred from build
output. A release manifest SHALL record the state as unconfirmed until this measurement has been run
and read.

#### Scenario: A manifest may not claim readiness it did not measure

- **WHEN** a release is staged without the page-size measurement
- **THEN** the manifest records the state as requiring confirmation rather than as ready

### Requirement: Nothing reaches Play without the owner's recorded approval, covering the build and the authored pack

Every step that produces a signed artifact, renders a store pack, captures media, or uploads to Play
SHALL refuse to run without a recorded approval. The record SHALL contain the owner's own words, a
timestamp, and two digests: one over the slot's build inputs and one over the authored pack inputs
the owner reviewed. Editing any digested input SHALL invalidate the approval on the next check.
Pipeline outputs SHALL NOT be part of either digest. Revoking an approval SHALL always be permitted.

#### Scenario: An unapproved revision cannot be uploaded

- **WHEN** the upload step runs against a revision with no recorded approval
- **THEN** it refuses, exits non-zero, and names the approval step

#### Scenario: Editing the reviewed pack invalidates the approval

- **WHEN** any authored pack input changes after an approval is recorded
- **THEN** the next check reports the approval as lapsed

#### Scenario: Editing the pipeline's own output does not lapse the approval

- **WHEN** a rendered asset or a generated sheet changes after an approval is recorded
- **THEN** the approval remains valid

#### Scenario: Recording an approval is never inferred

- **WHEN** no owner words are supplied to the recording step
- **THEN** no approval is recorded and the step refuses

### Requirement: Committed media are bound to a recorded build

A pack that composes its media SHALL ship a provenance record binding each committed asset to a
recorded build by hash, and the installed package SHALL be proven to be that build before any media
is published. A missing or mismatched link SHALL be fatal. A render performed while the record on
disk describes a previous run SHALL report `UNVERIFIED` rather than a pass.

#### Scenario: A self-consistent but stale pair is refused

- **WHEN** the release manifest and the installed package agree with each other about a build that
  is neither of them
- **THEN** the link is rejected

#### Scenario: An edited asset fails instead of keeping the previous claim

- **WHEN** a committed asset is replaced without a fresh capture
- **THEN** pack validation fails on the hash mismatch

### Requirement: Published package identity is stable

A slot's `applicationId` SHALL match the factory namespace rule, and once a slot is recorded as
published its `applicationId` SHALL NOT change. The namespace rule SHALL be defined in exactly one
module and imported by every program that needs it.

#### Scenario: A change to a shipped identity is refused

- **WHEN** a published slot's manifest carries a different `applicationId` from the recorded value
- **THEN** the check fails and names both values

### Requirement: Listing and release-note limits hold

Listing text SHALL satisfy the console's limits (title 30, short description 80, full description
4000 characters, counted as code points) and SHALL be non-empty in every required field. Every
release note in a multi-language paste block SHALL be at most 500 characters, measured per language,
including a language the listing does not declare. A release name SHALL agree with the staged
release manifest.

#### Scenario: An over-long note fails

- **WHEN** a paste block carries a note longer than the permitted per-language limit
- **THEN** the check fails and names the language

#### Scenario: A note for an undeclared language is still measured

- **WHEN** the paste block contains a language the listing does not declare
- **THEN** its note is measured and can fail the check

### Requirement: A pack carries no reserved documentation host

A store pack SHALL report as unfinished any declared URL pointing at a reserved documentation host
or using a non-secure scheme, and SHALL apply the developer account's monitored contact address as
the default so no pack ships a generator placeholder. A support address SHALL NOT be required to be
a secret and SHALL NOT be read from a credential file.

#### Scenario: A placeholder host is reported unfinished

- **WHEN** a pack declares a policy URL on a reserved documentation host
- **THEN** it is reported as unfinished and names the field

### Requirement: A slot's lifecycle state is recorded

The factory SHALL record each slot's lifecycle state, and the vocabulary SHALL include at least: a
scaffolded state, a published state meaning a release exists on Play, and an earning state meaning a
dated post-ship reading exists. A kill SHALL require a stated reason, and SHALL be refused for a
slot that meets its own declared criteria unless explicitly forced.

#### Scenario: A shipped app is distinguishable from a scaffolded one

- **WHEN** the factory status is printed for the portfolio
- **THEN** each slot's state is shown, and a slot with a release on Play is not shown as merely
  scaffolded

#### Scenario: A kill without a reason is refused

- **WHEN** a kill is requested with no reason
- **THEN** nothing is recorded and the command fails

### Requirement: The core's size is bounded and measured

The program core SHALL report its own size — the file count and line count of its sources — and its
test surface SHALL be a stated fraction of it. The measured values SHALL be reproducible by a single
command, so a claim of a reduction is reported from a measurement rather than asserted.

#### Scenario: Size is reproducible

- **WHEN** the size report is run twice on the same revision
- **THEN** it prints the same counts

#### Scenario: Growth is visible

- **WHEN** the core's line count exceeds its stated budget
- **THEN** the report says so rather than printing a bare number
