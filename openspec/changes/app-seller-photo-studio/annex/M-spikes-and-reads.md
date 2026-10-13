# M — Spikes and reads (measure before building)

All spikes run in scratch (`.kiln/scratch/`), write their numbers into
`docs/research/2026-10-12-seller-platform-evidence.md` §Spikes, and leave a **default decision** the builder
uses if the spike is skipped or inconclusive. A spike is not development: nothing it produces ships.

## S0 — The server host (task 2.1)

Read: the Caddy configuration, hostnames pointing at `78.111.89.46`, certificate state, disk, free RAM,
how the agent reaches the host (SSH user and key), whether outbound HTTPS to `queue.fal.run` and
`androidpublisher.googleapis.com` works.
Pass: `GET https://<host>/health` answers over a valid certificate, or the host list shows exactly what is
missing.
Default if no domain/TLS exists: raise **one** `AskQuestion` — "which hostname may point at this VPS?" —
and wait. Everything in section 4 of the tasks that does not need the public URL (server code, tests)
proceeds meanwhile.

## S1 — Cut-out benchmark (task 2.2)

Set: ~30 photos, ≥ 8 hard (glass, jewellery, fur/knit, white on white, shiny metal, a hand holding the
item, a transparent bag), the rest plain. Engines: `fal-ai/bria/background/remove` (reference) and each
**stable** on-device candidate found by reading the artifact's release notes (beta artifacts are excluded
by `dependency-freshness.mdc`).
Metrics: mask IoU against the reference and against a hand-corrected mask for 10 photos; latency on the A40
for local engines; cost per call for the server engine; the interior-RGB invariant check on composites.
Pass for shipping a local engine: stable artifact **and** median IoU ≥ 0.95, hard-subset median ≥ 0.90,
p95 latency ≤ 3 s on `SM-A405FM`. Default otherwise: cut-out is server-only at 1 credit and the product
makes no free-cut-out claim.
Artifacts: table + 100 % crops of the six worst masks under `.kiln/scratch/spike-s1/`.

## S2 — Fidelity benchmark (task 2.3)

For each Enhanced operation candidate (`O-08`..`O-14`, `O-07`) found on the provider's own model page: run
the same 30 photos, compute annex E's metrics. **Controls:** (a) identity (re-encode only) must score
`dE ≈ 0`; (b) a cross-photo swap (result of a different item) must be rejected 100 %.
Thresholds: set at the 95th percentile of the metric over results a reviewer labels "same product", with
the swap controls proving the threshold rejects wrong products. Record per-op cost per call; assign each
op to its tier by cost ceiling (a route above the ceiling is rejected). Replace the `null` endpoints in
`models.json` only with ids read from the provider page.
Default if inconclusive: the op stays `enabled:false` in `/config`; Original mode is unaffected.

## S3 — Vision and OCR benchmark (task 2.4)

~50 items across the 21 categories; measure category accuracy, attribute usefulness (does it name
material/colour correctly), scale-hint kind accuracy, SKU/ruler/label text reading, cost per call, and one
`O-15` text call for the six languages including limits (WB title 60).
Pass: category ≥ 85 %; scale/SKU exact read ≥ 80 % on photos where the text is legible to a human.
Default if below: recognition remains (category chip, editable), and scale/SKU become suggestions the
seller types or confirms, with the feature described in the UI as "reads what it can". OCR engine choice
(on-device unbundled ML Kit vs server vision) is recorded with its artifact version and stable status.

## S4 — Walking skeleton (task 2.5)

One request from the phone over HTTPS to the server → one purchase-token verification with the Play
Developer API (a **real test product** if the console allows one, otherwise the `products.get` 400-on-fake
and 200-on-voided-list checks already observed) → one ledger reserve/settle → one model call via
`FalProvider` → result back on the phone. Pass: server request log lines, a phone screenshot via
`adb -s R58MB3SGZVL exec-out screencap -p`, and `node scripts/check-client-secrets.mjs` clean on the
skeleton. Artifacts only in scratch; the real server and client are built in sections 4–5.

## S5 — Reads for facts the specs treat as secondary (task 2.6)

First-party reads: Ozon, Avito, eBay, Etsy, Depop, Amazon, Shopify help (convert `secondary` → `first-party`
in `marketplaces.json`), Poshmark and Mercari (currently unread). Provider price pages for each chosen
endpoint; Photoroom and Pixelcut pricing pages; `docs/monetization-strategy.md` §2.2 and §3.7 re-read for
the net factor and the cost multiple; the Play Billing Library release notes for the pin; current ML Kit
and AndroidX release notes for the stable versions used. Each price gets a source and a read date in the
evidence document, then the **money owner** confirms or adjusts the pack prices (annex J).

## S6 — Policy and standards read (task 2.7)

`play-auditor` reads and records: AI-generated content policy and the Help Center best-practice article it
links; Data safety rules for photos sent to a server and a processor; the Families/children and content
rating consequences of an image-generation app; the provider's retention and training terms for the chosen
endpoints; the IPTC digital-source-type vocabulary (exact URIs for `ProvenanceMap`); the current C2PA
conformance programme (read-only, to decide whether a later checkpoint is useful); and a verdict on
enabling `O-14` (virtual model). Output: findings saved to `apps/<slot>/docs/play-audit-s6.md` once the
slot exists, and quoted in the evidence document before.

## Order and parallelism

S0, S1, S2, S3, S5, S6 are independent and are dispatched to separate workers in one message with disjoint
output files; S4 starts once S0 returns a host and S1 returns a cut-out route. Gradle is not involved in
any spike.
