# Proposal: app-pipe-sizing

## Why this slot, why now

The owner selected the US fuel-gas pipe-sizing workflow in the market decision's §6. Demand evidence is for the broader on-job pipe-tool category: §2.2 records the hand-filtered incumbents and complaints; §7.5 preserves the remaining gas-specific measurement. The utility can be offline, account-free, server-free, camera-free, and free of dangerous permissions (market decision §§2, 6).

The 2026-09-25 owner decision narrows v1 to a sizing workflow and calculation aid. The app supplies no ICC, NFPA, AGA, or other code capacity data; users consult their own code book or free online code, enter or confirm the values they use, and record their citation. The app performs only surrounding arithmetic and bookkeeping. This follows the settled ICC 2024 IFGC front-matter facts: free viewing at `codes.iccsafe.org` is not redistribution permission; ICC requires separate written permission for reproduction and identifies IFGS material as AGA-copyrighted.

The recorded monetization is a one-time Play Billing unlock; subscriptions are deferred and ads are absent. This responds to the former-lifetime-price objection recorded in market decision §2.2 and `docs/monetization-strategy.md` §3.6.

## Demand evidence

This proposal relies on the decision memo rather than repeating its market study:

- §2.2 records the hand-filtered on-job evidence, including rated apps in the 50K+ and 100K+ install buckets, and why the raw gas query could not prove demand.
- §2.2 records complaints about locked inputs, metric-only outputs, failed long/high-demand runs, intrusive ads, and subscription migration.
- §6 records the owner's slot and jurisdiction selection. Historic broader-scope wording is superseded by the 2026-09-25 workflow-only decision.

## Distinctness

*A pipe-sizing calculation aid that handles long, high-demand runs when the user supplies the required capacity, gives the arithmetic in the unit system the user chose, leaves every input editable, records and shows the user's own source citation with every result, puts no ad over an input, and is bought once instead of rented.*

The clauses answer §2.2 complaints respectively: long/high-demand no-result and metric-only output (`com.mrcombi.pipesizing`); locked fields (`com.pipeflowcalculations.android`); ads over inputs (`se.itwelding.pipeguidecalculator`); and subscription migration (`com.calculated.josephine`). The citation is user-recorded provenance, not a claim that the app supplies a table or determines compliance.

## Non-goals

- Water sizing under IPC and sprinkler pipe under NFPA 13 are deferred from v1.
- Subscriptions and ads are not part of the v1 commercial surface.
- Accounts, cloud sync, server processing, ML/camera diagnosis, and any AI wrapper are out of scope.
- The app does not embed, bundle, download, reproduce, or fabricate ICC, NFPA, AGA, or other code-table capacities, clause text, table rows, or constants.
- The app does not claim code compliance, code approval, standards certification, or that every US jurisdiction adopts the same code, edition, amendment, material branch, or calculation method.
