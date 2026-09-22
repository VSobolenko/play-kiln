# Factory capability: slot isolation

## Purpose

Every PlayKiln attempt is an isolated Android project under `apps/` so experiments
never corrupt each other or the shared harness.

## Requirements

### Requirement: Unique application identity

Each slot SHALL use a unique `applicationId` of the form `com.gexetr.<slug>`
and a unique directory `apps/<YYYY-MM-DD>-<slug>/`.

#### Scenario: New idea creates a new slot

- **WHEN** a human requests a new app via `/ship`
- **THEN** the factory creates a new directory under `apps/` and a registry entry
  with status `building`

### Requirement: Shared harness

Slots MUST NOT embed copies of `.cursor/`, `.agents/`, or factory OpenSpec config.
Harness improvements happen at the repository root and apply to future work.

#### Scenario: Harness upgrade

- **WHEN** a skill or MCP config is updated at the root
- **THEN** existing and new slots inherit the change without per-app migration
