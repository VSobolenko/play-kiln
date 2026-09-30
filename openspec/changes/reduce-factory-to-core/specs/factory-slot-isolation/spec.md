# Factory capability: slot isolation

## MODIFIED Requirements

### Requirement: Unique application identity

Each slot SHALL use a unique `applicationId` of the form `com.gexetr.<slug>`
and a unique directory `apps/<YYYY-MM-DD>-<slug>/`. The factory's registry SHALL record every slot
with a lifecycle state whose vocabulary distinguishes at least a scaffolded slot, a slot with a
release published on Play, and a slot with a recorded post-ship reading. An `applicationId` SHALL
NOT change once its slot is recorded as published, because an `applicationId` is permanent after the
first upload.

#### Scenario: New idea creates a new slot

- **WHEN** a human requests a new app via `/ship`
- **THEN** the factory creates a new directory under `apps/` and a registry entry recording that
  slot's identity and its scaffolded lifecycle state

#### Scenario: A shipped app is not reported as scaffolded

- **WHEN** the factory reports the portfolio
- **THEN** a slot whose release exists on Play is reported in its published state, and a slot with a
  dated post-ship reading is reported in its earning state, so the distinction between an app that
  was built and an app that earned is representable

#### Scenario: A published identity is frozen

- **WHEN** a slot recorded as published carries an `applicationId` different from the recorded value
- **THEN** the check fails and names both values

#### Scenario: A kill carries a reason

- **WHEN** a slot is killed
- **THEN** the reason is recorded with the state, and a kill with no reason is refused
