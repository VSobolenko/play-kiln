# App slots

Each subdirectory is one Google Play attempt:

```
apps/YYYY-MM-DD-<slug>/
```

Create with:

```bash
node scripts/new-slot.mjs --slug my-app --name "My App" --idea "one-line pitch"
```

Or ask the agent: `/forge <idea>`

Do not put factory harness files here. Slots are disposable; the kiln is not.
