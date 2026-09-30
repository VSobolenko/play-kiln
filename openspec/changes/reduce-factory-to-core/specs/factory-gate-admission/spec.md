# Factory capability: check admission

## Purpose

Defines when a check may exist at all, so the program surface can only grow against a recorded
failure and cannot grow back into the harness this change removes.

## ADDED Requirements

### Requirement: A check is admitted only for a fact a machine settles deterministically

A program check SHALL exist only where a deterministic measurement decides the outcome, and the
measurement SHALL be reproducible from the artifact rather than from a log, a self-report or a
previous run's output. A check whose outcome depends on reading prose SHALL NOT be added, and where
one exists it SHALL be removed.

#### Scenario: A proposed check over prose is refused

- **WHEN** a check is proposed that would match patterns in listing text to judge its quality
- **THEN** it is not added, and the rule it would express is carried by the prompt that owns it

#### Scenario: A measurement read from a stale artifact is refused

- **WHEN** a check would read a result produced by an earlier invocation
- **THEN** it reports `UNVERIFIED` rather than a verdict

### Requirement: Every check names the incident that justified it

A check SHALL name the recorded defect, owner rejection or failed upload that motivated it, with a
reference to where that incident is recorded. A check that cannot name one SHALL be removed.

#### Scenario: An unmotivated check is removed

- **WHEN** a check's motivation cannot be found in the repository's records
- **THEN** the check is deleted rather than kept for safety

#### Scenario: A motivated check cites its source

- **WHEN** a check is added or kept
- **THEN** its definition names the incident and where it is recorded

### Requirement: One check, one fact, one command

A check SHALL evaluate one fact, take its constants from the single module that defines them, and
build its failure message from those constants rather than from a hand-written sentence. A check
SHALL NOT invoke another check or suite.

#### Scenario: A message cannot drift from its rule

- **WHEN** a check fails
- **THEN** the message is produced from the same constants the check evaluates, so the wording and
  the logic cannot disagree

#### Scenario: Constants are not restated

- **WHEN** two checks need the same limit
- **THEN** both import it from one module and neither repeats the value

### Requirement: A check that is not run is a defect

Every admitted check SHALL be invoked by the single verification entry point, and a check that the
entry point does not run SHALL fail the run rather than sit in the repository unexercised.

#### Scenario: An orphaned check fails the run

- **WHEN** a fact exists in the repository's fact ledger naming an enforcing function that the entry
  point does not run, or naming a function that does not exist
- **THEN** the verification run fails and names that fact

#### Scenario: An unenforced rule is reported honestly

- **WHEN** a recorded rule has no program enforcing it
- **THEN** the run reports it as unenforced rather than as satisfied or as absent

### Requirement: The program surface has a stated budget and is measured

The factory SHALL state a line-count budget for its program surface and for the test surface inside
it, and SHALL report both from a measurement. A measured value over budget SHALL be reported as such.

#### Scenario: Budget breach is visible

- **WHEN** the size report runs
- **THEN** it prints the measured line count, the budget, and whether the budget is met

### Requirement: A reduction claim is reported from a measurement

A change that claims to reduce the harness SHALL report the same measurement before and after, and
the comparison SHALL be recorded with the change rather than asserted in prose.

#### Scenario: Before and after are comparable

- **WHEN** a change claims a reduction
- **THEN** the record contains both measurements, produced by the same command, and the difference
  is readable without trusting the author

### Requirement: A configuration file is verified by something that reads it

A configuration file the factory depends on SHALL be parsed by the verification run, and a file that
does not parse SHALL fail the run rather than be silently ignored.

#### Scenario: An unparseable configuration fails the run

- **WHEN** a configuration file the factory depends on is invalid
- **THEN** the verification run fails and names the file and the parse error
