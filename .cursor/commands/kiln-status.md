---
name: "/kiln-status"
id: "kiln-status"
category: "Workflow"
description: "Show factory health: kiln-status + harness inventory"
---

# /kiln-status

```bash
node scripts/kiln-status.mjs
node scripts/harness-inventory.mjs
```

Report PASS/WARN/FAIL plainly. If NOT READY, route to `/diagnose`.
