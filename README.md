# PlayKiln

PlayKiln is an Android/Google Play app factory: one idea becomes one isolated,
publishable app with signed APK/AAB artifacts and a complete store pack. It is not a
single app repository.

## Two-minute path

```bash
node scripts/kiln-status.mjs
# Then ask the agent:
# /ship <your app idea>
```

| Stage | Owner |
|---|---|
| Approve the plan | Human checkpoint |
| `/ship` → `scripts/new-slot.mjs` | Factory |
| Codex workers implement | Factory |
| `store-kit.py` → `prepare-release.mjs` → `verify-slot.mjs` → `/eval` | Factory |
| Legal declarations, first upload, publish | Human checkpoint |

New slots come from [`scaffold/android-compose/`](scaffold/android-compose/) and are
created under `apps/<date>-<slug>/` with a unique `com.gexetr.<slug>`
`applicationId` — the developer account's existing Play namespace.

The factory tests itself, so a broken script cannot reach a slot:

```bash
node scripts/selftest.mjs   # runs new-slot.mjs against a temp sandbox and asserts the contract
```

It also runs automatically inside `scripts/verify-slot.mjs`.

Read the agent contract in [`AGENTS.md`](AGENTS.md), the Russian operator brief in
[`MISSION.md`](MISSION.md), and the live-release handoff in
[`docs/play-publishing-playbook.md`](docs/play-publishing-playbook.md).
