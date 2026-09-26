# Proposal: app-teacher-gradebook

## Why this slot, why now

The owner selected the teacher record-book shelf on 2026-09-26 from the measured scan in
`docs/research/2026-09-26-teacher-gradebook-evidence.md`. The slot is new and independent; it
does not touch `apps/2026-09-25-pipe-sizing`.

## Demand evidence

Every claim is labelled; the full record is `docs/research/2026-09-26-teacher-gradebook-evidence.md`.

- MEASURED: `node scripts/enrich-market.mjs --query "teacher gradebook" --top 10` returns a
  leader at **3.6★ with 4,510 reviews and a `500K+` install bucket**
  (`com.apolosoft.cuadernoprofesor`), with `100K+` at 4.1★ and `50K+` at 3.7★ behind it.
  In-app purchase is declared on **6 of 10** enriched apps and ads on **1 of 10**.
- MEASURED: `node scripts/scan-complaints.mjs` read 12 reviews across four on-job apps and
  found 6 critical ones naming **missing create/delete of classes**, **crashes on daily use**,
  **a purchase stranded in a dead store**, and **resentment of a subscription** where the user
  asks for a one-time purchase.
- MEASURED: `node scripts/scan-titles.mjs --query "markbook"` finds no app titled *Markbook*,
  so the proposed store title does not collide with an existing listing.
- ON RECORD: 3,232,905 US K-12 teachers (NEA 2026); **91 %** personally fund part of their work
  and **77.9 %** bought tools the school did not provide (4pt0, n = 1,000); out-of-pocket
  classroom spend is **≈ $600/year** (DonorsChoose 2026, n = 2,500).

## Economics

- **Archetype:** `purchase_led_single_unlock`; the slot must enable the `billing` capability.
- **Paying user:** a teacher who already keeps a private paper or spreadsheet record beside the
  school's official system and funds tools from their own pocket.
- **What is bought:** a one-time unlock of unlimited classes and students, weighted categories,
  CSV and PDF export, and a restorable backup file.
- **Price:** $6.99. **Moment:** creating a second class, or the first export.
- **Kill criteria:** 60 days, fewer than 1,500 installs, or under $100 of revenue.
- **Honest ceiling:** $100–$550/month at leader-scale install flow; $1,000/month would need
  ≈ 9,200 installs a month. The plan is
  `docs/ideation/plans/teacher-gradebook.economics.json` and it passes
  `node scripts/check-economics.mjs --economics`.

## Distinctness

*A classroom record book a teacher owns outright — every class, student, assignment, grade,
attendance mark, participation note and parent contact can be added, edited and deleted without
a crash, the whole record backs up and restores from the teacher's own storage, and it is bought
once instead of rented monthly.*

## Non-goals

- No account, server, cloud sync, analytics, camera, ML or AI surface; the release requests no
  network permission.
- No ads, and no subscription in v1.
- No claim to replace, sync with, or be certified by any school SIS or LMS.
- No grade-scale, standard, or curriculum data is bundled: the teacher defines the scale.
