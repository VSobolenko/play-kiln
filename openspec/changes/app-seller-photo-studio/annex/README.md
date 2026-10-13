# Annex — the implementation reference for `app-seller-photo-studio`

Read order for the building chat: `proposal.md` → `design.md` → this README → the annex file named by the
task you are on → the capability spec. **Nothing in this package is left for the builder to decide except
what a spike (annex M) is explicitly assigned to measure.** Where a value is a decision it says
`DECISION`; where it is a fact it carries a label from `.cursor/rules/grounded-claims.mdc` and a source in
`docs/research/2026-10-12-seller-platform-evidence.md`; where it is genuinely unknown it says `SPIKE <id>`
and gives the default the builder uses until the spike reports.

| File | What it fixes | Cited by |
|---|---|---|
| `A-competitor-parity.md` | every competitor feature found, mapped to a requirement, a milestone or a stated non-goal (ids `F-nn`) | specs, tasks |
| `B-marketplace-presets.md` + `marketplaces.seed.json` | the preset schema, linter rule types and the seeded data with sources (ids `M-<key>`) | `marketplace-presets`, tasks 5.4–5.5, 6.7 |
| `C-data-model.md` | Kotlin domain types, the Room schema, DataStore keys, backup rules, the server SQLite schema | all client/server tasks |
| `D-server-api.md` | every HTTP endpoint, request/response bodies, error codes, idempotency, limits (ids `API-nn`) | `credits-wallet-server`, tasks 4.x |
| `E-pipeline-and-operations.md` | the composition maths, the invariant, the operation catalogue with credit tiers, model-routing seed, fidelity metrics (ids `O-nn`) | `card-pipeline`, `studio-ai` |
| `F-screens-and-flows.md` | navigation graph, every screen with states and smart buttons, the first-run journey (ids `S-nn`) | UI tasks |
| `G-edge-cases.md` | the register of states and failures with the required behaviour (ids `E-nn`) | all specs |
| `H-compliance-and-provenance.md` | Play policy, Data safety rows, privacy facts, AI labelling, retention, abuse | `provenance-and-compliance` |
| `I-slide-kit.md` | the infographic/slide series: layout engine, templates, seller-controlled text, text safety | `slide-kit` |
| `J-identity-and-money.md` | visual direction, the name derivation, price ladder, unit arithmetic, kill criteria | scaffold, economics |
| `K-package-and-test-plan.md` | module/package layout, dependencies, the named test classes, worker file partition | tasks, dispatch |
| `L-linter-rules.md` | every linter rule with its predicate, severity, fix action and fixtures | `marketplace-presets` |
| `M-spikes-and-reads.md` | the measurements that precede code, their pass numbers and default decisions | tasks section 2 |

Identifiers are stable. Specs cite `(E12)`, `(F-11)`, `(O-04)`, `(API-07)`, `(S-09)`, `(M-wb)` so a
reader can follow a requirement to the behaviour that defines it.

Two limits, stated rather than implied. The annex is a specification, not evidence that the app works;
nothing here has been built or run. And marketplace numbers are as old as their read date — the preset's
`verifiedAt` is part of the data because marketplaces change their rules without notice.
