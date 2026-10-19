# W66 — starter-credit anti-abuse assessment

## Answer

**Production — MEASURED/INFERRED:** a normal second install on the same physical
device does not receive another starter grant, provided Play Integrity Device Recall
has been enabled for the app. The server decodes `deviceRecall.values.bitFirst`
([`server/src/device-recall.ts:66-78`](../../server/src/device-recall.ts)), refuses a
true or unavailable bit ([`server/src/device-recall.ts:25-35`](../../server/src/device-recall.ts)),
and, only for an explicit `false`, writes `bitFirst: true` before returning a successful
claim ([`server/src/device-recall.ts:48-50`](../../server/src/device-recall.ts)). The
wallet transaction grants the ten credits only when that claim was successful
([`server/src/integrity.ts:149-170`](../../server/src/integrity.ts)).

**ON RECORD:** Google documents that Device Recall data is stored on Google's servers
and can be recalled after an app reinstall or a device reset. It also documents that
writes can take up to 30 seconds to propagate and that Device Recall is beta.
[Google's Device Recall documentation](https://developer.android.com/google/play/integrity/device-recall)

Consequently, clearing app data, reinstalling, factory-resetting, or clearing Play
app data does **not** reset the production grant marker: it is neither the app's
install ID nor local Play app storage that is used as the marker. The server's
install-ID hash is a same-install race and budget check only
([`server/src/integrity.ts:32-45`](../../server/src/integrity.ts)); it is not the
anti-reinstall proof.

## Attacks and limits

| Case | Result | Evidence |
|---|---|---|
| Fresh physical device | A new device can receive one ten-credit grant. | The recalled bit is per device; the code only denies an already-true bit (`device-recall.ts:25-29`). |
| Rooted/tampered device | An ordinary rooted/tampered session is denied a grant unless it still receives Play's exact `PLAY_RECOGNIZED`, `LICENSED`, and `MEETS_DEVICE_INTEGRITY` verdicts. This is anti-abuse, not an absolute guarantee against an attacker who can defeat Play Integrity itself. | `integrity.ts:128-148`. |
| Sideload/outside Play | No starter grant: the exact recognized-and-licensed checks are required. | `integrity.ts:138-149`. |
| Clear app data, reinstall, factory reset, clear Play data | No new production grant for that physical device after Device Recall is configured. | Google documentation above; `device-recall.ts:25-35`. |
| DEV integrity | Re-grant is possible with a new install ID after app-data clear/reinstall because DEV always simulates `recallBitFirst: false`. It is prohibited in production. | `device-recall.ts:104-118`; `server/README.md:74-84`. |
| Two fresh sessions before recall propagation | A bounded duplicate-grant race remains possible if separate fresh integrity tokens both contain `bitFirst: false` before Google's write is visible. The daily budget still caps it. A complete close needs a fresh post-write client token/warm-up, not a server-side hardware ID. | Google documentation above; `integrity.ts:149-157`. |

## Liability cap

**MEASURED:** every grant reserves `starterCredits × 25,000` micro-USD in the UTC
`spend_day` row and the availability check refuses a grant that would exceed
`STARTER_DAILY_BUDGET_USD` ([`server/src/integrity.ts:30-45`](../../server/src/integrity.ts),
[`server/src/integrity.ts:166-170`](../../server/src/integrity.ts)). Defaults are ten
credits and a $10 daily starter budget ([`server/src/config.ts:54-56`](../../server/src/config.ts)).
At the design ceiling this reserves $0.25 per ten-credit grant, so the default admits
at most 40 grants in one UTC day. This caps daily starter liability; it is not a
permanent identity system and resets at the next UTC day.

## Owner-phone review

**Production: no.** Debug builds and data clearing on the owner's phone do not
produce a new production starter grant once `bitFirst` has been written and returned
as true by Play. **DEV review: yes.** Its documented simulator always returns false,
and a clear/reinstall gives a new install ID; the local database then has no matching
install-hash grant. That is intentionally not production-safe and is blocked when
`NODE_ENV=production` ([`server/src/device-recall.ts:101-118`](../../server/src/device-recall.ts)).

## Disposition

No server code was changed. The examined production path already checks bit polarity
and requires a successful recall write before the ledger grant. The remaining
propagation race cannot be truthfully fixed in `server/src/**` alone without changing
the client flow to obtain a new post-write verdict; inventing a hardware identifier
would not be a valid fix.
