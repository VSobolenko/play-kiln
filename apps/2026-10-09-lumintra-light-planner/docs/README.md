# Annex — the authoring inputs this change cites

## What this directory is

The specification package for **Lumintra** (`openspec/changes/app-lumintra-light-planner/`) cites its
authoring inputs throughout. Those inputs were written during one working session on **2026-10-08** and
originally lived under `.kiln/scratch/spec-2026-10-08/`, which is **gitignored** (`.gitignore` →
`.kiln/`) — so a fresh clone of this repository would have had none of them, and every citation in the
package would have dangled.

This `annex/` directory is the fix: the cited inputs are **copied here, verbatim and byte-identical**,
so the change is self-contained and survives a clone.

**These files are working notes, not specifications.** Each is a reference annex written during the
session that produced the package. The specification — the thing that was reviewed and that a builder
is held to — is the **twelve files one level up** (`proposal.md`, `design.md`, `tasks.md` and the
`specs/**/spec.md` deltas). Treat the annex bodies as reference material, never as normative text.

## The letter map

The package references these files by a single letter plus a section, e.g. `N §2.1` or `O §4.3`. That
letter resolves here:

| Annex file | What it is |
|---|---|
| `CONTEXT.md` | The package map: shared types, paths and schemas the whole specification is written against. Cited across the package (~24 times in `design.md`). |
| `K-competitor-features.md` | Competitor teardown — the incumbent feature shelves the wedge is argued against. |
| `L-depth-retention-edges.md` | **The edge-case register, ids `E25`–`E111`.** The five specs cite it **123 times** as normative (`(E94)`, `(E111)`, …). This is the file a citation like `(E<n>)` resolves into. |
| `M-play-integrations-compliance.md` | Play integrations and the compliance reading (permissions, Data safety, policy surface). |
| `N-data-algorithms.md` | Data sources and algorithms (catalogues, ephemeris, cloud, light-pollution, the arithmetic). |
| `O-sky-scene-rendering.md` | The sky-rendering model and the LUT / tone-mapping approach. |

## The one rule that makes the citations resolvable

> A citation of the form **`<letter> §x`** resolves to **`annex/<that file> §x`** (for example `N §2.1`
> → `annex/N-data-algorithms.md §2.1`), and a citation of the form **`(E<n>)`** resolves to **the row
> whose id is `E<n>` in the edge-case register inside `annex/L-depth-retention-edges.md`**.

That is the whole mechanism: read the letter, open the file in this directory, find the section. If you
are a reader who landed on this file first, that sentence is what you needed.

## Provenance and caveat

- **Byte-identical copies.** All six files here were copied from `.kiln/scratch/spec-2026-10-08/` on
  **2026-10-08**, and each copy was verified byte-for-byte against its scratch original (equal byte
  count **and** equal SHA-256). Nothing in their bodies was rewritten, summarised or reformatted.
- **The copies are the durable ones.** `.kiln/` is disposable local state and may be deleted at any
  time; the copies in this `annex/` are now the ones that travel with the repository. If the scratch
  originals disappear, nothing here is lost.
- **These are the inputs, not verified conclusions.** The package was *written against* these notes;
  the notes are not themselves a reviewed artifact. Where the package and an annex disagree, **the
  package's specification is authoritative**, because the specification is what was reviewed and what a
  builder implements. Naming any such disagreement you find is welcome; silently trusting either side
  is not. Nothing here should be read as a claim that a given figure was independently re-verified —
  it is a record of what the authoring session wrote down.

## Note on `tai-utc.dat` (not part of this annex set)

The package requires the Orekit relativity file at `assets/orekit-data/tai-utc.dat`. A file of the
stated **3,321 bytes** exists in scratch — but **not** at the literal path the handoff brief named
(`.kiln/scratch/positioning-2026-10-08/tai-utc.dat`, which does **not** exist). The 3,321-byte file was
measured at:

- `.kiln/scratch/positioning-2026-10-08/minimal-data/orekit-data/tai-utc.dat` (3,321 B)
- `.kiln/scratch/positioning-2026-10-08/tiny-data/orekit-data/tai-utc.dat` (3,321 B)

(also present under `.kiln/scratch/android-orekit-smoke/app/src/main/assets/orekit-data/` and
`.kiln/scratch/nv-core-proof/data/orekit-data/`, both 3,321 B).

So its source is knowable, but it is **not** copied into this annex — this directory holds six
markdown inputs and this README, nothing else.

## What is deliberately not here

Only the six cited markdown inputs were copied. The large HTML/JSON captures, the `aar/` tree, the
Orekit Java probes and the other `.kiln/scratch/` directories are **not** part of this annex.
