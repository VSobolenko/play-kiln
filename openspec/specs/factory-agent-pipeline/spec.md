# factory-agent-pipeline Specification

## Purpose
Defines the factory as a sequence of agent roles that each leave an artifact on disk and two human
checkpoints, so that a rule needing judgement is changed by editing one prompt rather than by
changing a program and its tests.

## Requirements

### Requirement: The pipeline is roles producing artifacts, not programs enforcing prose

The factory SHALL define a fixed set of roles — a scout that finds and evidences demand, a money
owner that owns the commercial plan and the post-ship reading, a builder that produces the app and
its pack, a verifier that reproduces claims from the artifacts on disk, and a critic that runs
adversarially against the most recent landing. Each role SHALL be defined by one prompt document,
and each SHALL produce at least one artifact under version control. The pipeline SHALL be
resumable from the artifacts on disk alone, without depending on any agent's conversational state.

#### Scenario: A fresh session resumes from disk

- **WHEN** a new agent session starts against a slot that a previous session began
- **THEN** the role's next action is determined by reading the artifacts on disk, and no state is
  required from a previous conversation

#### Scenario: A role leaves an artifact

- **WHEN** any role finishes its work
- **THEN** a file exists under version control that records what the role concluded, and the next
  role reads that file rather than a self-report

### Requirement: A judgement rule is stated in exactly one place

A rule whose truth requires reading and judging prose SHALL be stated in exactly one prompt or
checklist document. It SHALL NOT also be encoded as a program check over text. The Cursor and Codex
agent definition directories SHALL point at the canonical role documents rather than duplicating
their text.

#### Scenario: One edit changes the behaviour

- **WHEN** a judgement rule is changed
- **THEN** exactly one file is edited, no program is changed, and no test suite needs updating

#### Scenario: Two copies cannot drift

- **WHEN** the agent definition directories are inspected
- **THEN** they reference the canonical role document rather than restating its rules

### Requirement: A machine check is never a substitute for a judgement

A requirement that cannot be decided from a deterministic measurement SHALL NOT be represented as a
program check. Where a program check exists over prose, it SHALL be removed and its intent SHALL be
carried by the prompt or checklist that owns it, so that a passing run is not read as coverage the
factory does not have.

#### Scenario: A passing run does not imply a judgement

- **WHEN** the verification entry point reports a passing run
- **THEN** no rule requiring judgement is reported as satisfied, because no such rule is a check

### Requirement: Two human checkpoints, and only two

The owner SHALL be the decider at exactly two points in the pipeline: approving the commercial plan
before the app is built, and approving the release before any signed artifact, store pack or upload
exists. Each decision SHALL be recorded from the owner's own words, SHALL NOT be inferred from any
agent's confidence, and SHALL NOT be recorded in order to unblock a step.

#### Scenario: An approval is not inferred

- **WHEN** an agent believes the app is ready
- **THEN** no approval is recorded, and the release steps keep refusing

#### Scenario: An approval is recorded from the owner's words

- **WHEN** the owner states his decision in his own words and the recording step is run with them
- **THEN** the record exists and the release steps proceed

#### Scenario: A lapsed approval stops the release

- **WHEN** any digested input changes after an approval was recorded
- **THEN** the release steps refuse again until a new approval is recorded

### Requirement: The owner's remaining manual work is limited to what cannot be automated

The manual steps SHALL be limited to the owner's own attestations and to a step whose automation is
blocked by a stated tooling limitation. Any other manual step SHALL be automated or removed rather
than documented as a human task.

#### Scenario: A human step carries its reason

- **WHEN** a checklist or runbook names a step as human
- **THEN** it states why, and the reason is either the owner's own attestation or a named tooling
  limitation

### Requirement: The verifier can fail the thing it verifies

A verification SHALL state, in advance, the observation that would flip its verdict and the exact
command that produces that observation. A verification whose verdict no evidence could contradict
SHALL NOT be recorded as a verification.

#### Scenario: A falsifiable verification

- **WHEN** a role reports a verification
- **THEN** the report names the command that was run, the observed output, and what would have made
  the verdict negative

### Requirement: Bug hunting and verification are separate passes

A defect search that looks for things no role has claimed SHALL be a separate pass from the
reproduction of claims another role has made. The two SHALL NOT be collapsed, and a disagreement
between them SHALL be adjudicated against the artifact rather than averaged.

#### Scenario: Both passes are required

- **WHEN** a change is completed
- **THEN** the record shows a claim-reproduction pass and an independent defect-search pass, each
  naming its own evidence

### Requirement: Bounded workers, one build consumer at a time

Every worker SHALL be given a bounded scope and a file list, and code-writing workers SHALL have
disjoint files or separate worktrees. At most one process SHALL run the build tool in a slot at a
time, and the mechanism SHALL report the holder of the slot instead of failing with a toolchain
error. A worker that died mid-write SHALL be detected from the tree rather than from a ledger label.

#### Scenario: A second build consumer is refused by name

- **WHEN** a build is attempted while another process holds the slot
- **THEN** it fails immediately and names the holder, rather than dying inside the build tool's own
  lock

#### Scenario: Work is inspected on disk

- **WHEN** a worker's ledger label says it was blocked
- **THEN** the files it was assigned are inspected before its scope is reassigned, because a worker
  that died mid-write leaves the tree, not the label, as the evidence

### Requirement: A role prompt states its rule together with the evidence that settles it

A rule in a role prompt SHALL state what observation settles it and where that observation is
recorded. A rule with no observable consequence SHALL NOT be added to a prompt.

#### Scenario: A rule is actionable

- **WHEN** a role prompt rule is read
- **THEN** it names what the role does, what artifact results, and what would show the rule was not
  followed
