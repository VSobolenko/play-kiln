# Proposal: reduce-factory-to-core

## Why

PlayKiln's product is revenue; the harness is a means to it. After four scaffolded slots and no
earnings, the means has become the work.

**Measured on 2026-09-30** (a directory walk of `scripts/`, excluding `__pycache__`):

| Surface | Files | Lines |
|---|---:|---:|
| All `scripts/` sources | 88 | 47,358 |
| Regression suites (`test-*`) | 22 | 10,641 |
| The two meta-runners (`selftest.mjs` 3,507 + `verify-slot.mjs` 1,674) | 2 | 5,181 |
| Everything else | 64 | 31,536 |

**33 % of the harness verifies the other 67 %.** `selftest.mjs` discovers and runs the 22 suites;
`verify-slot.mjs` runs `selftest.mjs`; three suites assert that other scripts call `runGradleTasks`
or that a named sensor still exists. The three largest files are `store-kit.py` (6,192),
`test-console-answers.py` (4,158) and `selftest.mjs` (3,507) — one program that both renders a
store pack and validates it, one suite that tests the generator of a document, and one runner that
runs everything.

The cause is not size. It is that the harness tries to do two jobs a program cannot do.

1. **Judgement enforced by code.** Whether a niche is real, whether a price is right, whether a
   listing is truthful, whether an icon is beautiful — these are read-and-decide questions. The
   harness encodes them as regexes over prose and nested gates, so a rule change needs a code
   change, a rule the code cannot express is faked with a shape check, and the rule that mattered
   is enforced by nothing. The factory's own `check-*` gates report `UNENFORCED` for exactly this
   reason.
2. **A gate trusted rather than exercised.** The repository's own rules say it: *a check that
   cannot fail is not a check*, and *a gate that has never run on a slot it was not written for is
   unverified*. Both rules were written because both were violated.

**A second, independent defect, measured in this session.** `openspec/config.yaml` — the file that
carries the factory's `context` and its `proposal` and `tasks` rules for every OpenSpec change — is
**invalid YAML**. `python -c "yaml.safe_load(open('openspec/config.yaml',encoding='utf-8'))"`
reports `mapping values are not allowed here, line 17, column 34`, caused by a `: ` inside an
unquoted plain scalar (`...not merely thin: the factory shipped...`). The OpenSpec CLI therefore
prints `Warning: could not parse ... ignoring it` and proceeds. **Every change in
`openspec/changes/` was planned without the project's own rules**, and nothing in the harness ever
noticed, because nothing ran the parser on the config.

## What Changes

- **A program core is defined and bounded** (`factory-core`): the facts a machine can measure
  deterministically — Play asset geometry and byte caps, published package identity, build/toolchain
  configuration, absence of a paid-provider credential in the shipped client, the privacy policy's
  in-app reachability, 16 KB ELF readiness, the owner's approval digests, capture provenance, and
  the slot registry. One command per fact. **No gate runs another gate's suite.**
- **Judgement moves to agent roles** (`factory-agent-pipeline`): niche selection, the commercial
  plan, copy, icon creativity and pricing become role prompts plus artifacts the owner reads, not
  regexes. Each role's rules live in exactly one prompt, so changing a judgement rule is a prompt
  edit, not a code edit, and there is no test suite to re-derive.
- **A gate must earn admission** (`factory-gate-admission`): a check is admitted only for a fact a
  machine measures, only if it names the recorded incident that justified it, only with one owner
  command, and only if the single verify command actually runs it. A check with no recorded
  incident is removed.
- **BREAKING — the meta-runner chain is removed.** `selftest.mjs` stops discovering and running
  suites; a suite is run by whoever needs it, from a checked-in command list. `verify-slot.mjs`
  stops invoking `selftest.mjs`. The per-slot lock is kept as a lease, not as a substitute for
  scheduling.
- **BREAKING — `store-kit.py` is split** into one renderer, one validator and one fixture-loading
  model, so the program that writes a store pack is not the program that grades it.
- **MODIFIED — `slot-economics`**: the economics gate keeps only the part a machine can measure
  (archetype vocabulary, capability pairing, shape of the kill criteria); the demand evidence and
  the price become an owner-approved plan the money owner authors.
- **MODIFIED — `factory-slot-isolation`**: the registry records a lifecycle with `published` and
  `earning` states, so an app that shipped and an app that earned are representable. Today only
  `scaffolded` and `killed` exist, which is why the factory has never been able to state a result.
- **A written preservation contract** — `requirements-preservation.md` in this change — enumerates
  every requirement accumulated in the harness with its exact values and its current source, so the
  refactor is provably lossless rather than hopefully lossless.
- **The invalid `openspec/config.yaml` is repaired** and a check is added that fails when the file
  does not parse, since a config that is silently ignored is worse than no config.

## Capabilities

### New Capabilities

- `factory-core`: the deterministic program surface — what the factory must measure in code, the
  asset and artifact contract it enforces, the identity and approval boundaries, and its size
  budget.
- `factory-agent-pipeline`: the roles (scout, money owner, builder, verifier, critic), the artifacts
  each must leave on disk, the two human checkpoints, and the rule that a judgement has no machine
  gate.
- `factory-gate-admission`: the admission rule for a new check, the evidence bar, the
  one-command/one-fact structure, the program-surface budget, and the removal default.

### Modified Capabilities

- `factory-slot-isolation`: the registry gains lifecycle states, so `published`, `earning` and
  `retired` are representable and the kill decision binds to a state rather than to a directory.
- `slot-economics`: the plan stops being a program gate and becomes an owner-approved document; the
  machine keeps only the archetype vocabulary, the capability pairing and the shape of the kill
  criteria.

## Impact

- **Deleted or reduced**: `scripts/selftest.mjs` (3,507 → a checked-in command list),
  `scripts/verify-slot.mjs` (1,674 → one command per fact), the 22 `scripts/test-*` suites except
  those whose sensors stay in the core, `scripts/store-kit.py` (6,192 → three smaller programs),
  `scripts/test-console-answers.py` (4,158 → removed with the document it tests),
  `scripts/check-freshness.mjs`, `scripts/check-lessons.mjs`, `scripts/check-outcomes.mjs`'s gate
  role, and the `console-form.json`-driven console sheets.
- **Kept and hardened**: `new-slot.mjs`, `lib/gradle.mjs`, `lib/java.mjs`, `prepare-release.mjs`,
  `lib/release-approval.mjs`, `approve-release.mjs`, `check-client-secrets.mjs`,
  `lib/client-secrets.mjs`, `check-16k.py`, `check-adaptive-icon.py`, `lib/play-assets.mjs`,
  `media-capture.mjs`, `lib/apk-provenance.mjs`, `play-upload.mjs`, `lib/namespaces.mjs`,
  `check-in-app-policy.mjs`, `check-locales.mjs`.
- **Preserved as data, not as prose at risk**: the exact Play asset windows, the icon construction
  grid and mask geometry, the console form's field caps and page paths, the EU Regulation
  2018/302 position, the client-secret pattern set, the toolchain pin table and the build-speed
  measurements — all enumerated in `requirements-preservation.md`.
- **Not changed by this proposal**: no slot is rebuilt, no `.cursor/rules/**` behaviour is
  discarded, and no measured fact is removed without its replacement being named in
  `requirements-preservation.md`.
