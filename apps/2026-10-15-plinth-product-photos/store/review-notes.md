# Reviewer notes — Plinth: Product Photos

## Reviewer access

There is no app account or sign-in. Server-based paid operations require available wallet credits; a new wallet is not guaranteed free credits because the server gates its starter grant on Play Integrity and Device Recall (`server/src/integrity.ts`). The reviewer cannot be asked to buy with a personal account. The Play Console route for paid access is a promo code entered in App access instructions (`docs/play-console-checklist.md`, §§3 and 5); no code is stored here, and the current Console product/promo-code setup has not been verified. Until an active credit-pack code is provided and tested in Console, paid review access is unverified; the reviewer can still inspect the local workflow and the app's free/on-device tools.

## Not finished yet

- The privacy policy is published and reachable: `node scripts/publish-privacy.mjs --slot plinth-product-photos --check` returned HTTP 200, 12,589 bytes, and `mentions the policy: true` for the URL in `listing.json`.
- Listing claims still need a release-build comparison; no device review was performed in this task. The listing's contact address is `v.sobolenko.vs@gmail.com`, but whether it is currently monitored was not checked here.
- `journey.json` is authored but has not been replayed on a device. The store renderer and real-device screenshot capture have not run; both are phase-2 work gated on the owner's recorded release approval.
- The active Play Billing products and reviewer promo-code redemption remain unverified in Play Console; verify them before declaring full paid access to the reviewer.

## Photos and local work

Imported project photos and local edits stay in the app's private storage; they are not uploaded just by importing or viewing them. A user-started server operation sends the selected media to Plinth and its processing provider; session/wallet identifiers, purchase verification data, listing text requested for generation, and optional report notes also leave the device as described in [data-safety.md](data-safety.md). That document is the full data-use and retention account; this note does not replace it.
