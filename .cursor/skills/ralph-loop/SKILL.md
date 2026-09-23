---
name: ralph-loop
description: Filesystem-ledger grind loop for stubborn PlayKiln tasks — fresh context each iteration, state on disk. Use when a task keeps stalling mid-context.
---

# Ralph loop (adapted)

Sit on the loop, not in it:

1. Persist goal + acceptance in `.superpowers/sdd/<slug>/ralph.md`
2. Each iteration: fresh agent context; read only ledger + failing evidence
3. Do the smallest next action; update ledger; exit
4. Outer driver (`/ship` controller) re-invokes until acceptance or cap

Pair with computational sensors (`verify-slot`) as back-pressure. Do not infinite-loop
without a human-visible cap (default 8 iterations) and ledger Ruling on exit.
