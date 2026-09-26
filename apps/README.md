# App slots

Each subdirectory is one Google Play attempt:

```
apps/YYYY-MM-DD-<slug>/
```

Create with:

```bash
node scripts/new-slot.mjs --slug my-app --name "My App" --idea "one-line pitch" \
  --economics docs/ideation/plans/my-app.economics.json \
  --differentiators docs/ideation/plans/my-app.differentiators.json
```

`--economics` and `--differentiators` are both required. The script refuses to create a slot whose
plan does not
name a real revenue mechanism, because the factory's first real slot shipped with
`capabilities.json = {"enabled": []}` — no billing, no ads — and every gate passed.
The plan contract is `docs/slot-economics.md` and candidate ideas awaiting a decision
live in `docs/ideation/`.

Or ask the agent: `/ship <idea>` — the agent proposes the idea and its economics plan
from measured demand, and you approve both before any slot exists.

Do not put factory harness files here. Slots are disposable; the kiln is not.
