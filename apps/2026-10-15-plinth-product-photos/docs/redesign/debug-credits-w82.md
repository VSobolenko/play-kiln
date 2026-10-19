# W82 — debug-credit reset procedure

## Scope

This procedure is for the debug service only:

- service: `plinth-product-photos-debug.service`
- release selector: `/opt/plinth-product-photos/current-debug`
- database: `/var/lib/plinth-product-photos-debug/server.sqlite`
- public prefix: `https://pikupo.com/plinth-debug`

Do not substitute `plinth-product-photos.service`, `/opt/plinth-product-photos/current`,
`/var/lib/plinth-product-photos/server.sqlite`, or `/plinth/v1`. They are production.

## Preferred owner reset: no SSH

With the debug build installed and the A40 connected, clear its debug application data:

```powershell
adb -s R58MB3SGZVL shell pm clear com.gexetr.plinthproductphotos.debug
```

Launch the debug app and complete its normal bootstrap. This works when the debug
service still runs `PLINTH_DEV_INTEGRITY=1`: DEV Device Recall simulates a false
recall bit and a fresh app-data clear creates a new install ID, so the regular
starter-grant path can issue its configured ten credits. It does **not** edit an
existing wallet, and it is not a production reset mechanism.

Use the SQL/script path below when the existing remote wallet itself must retain its
identity (for example, to reproduce a session or restore its remaining test budget),
or when a fresh bootstrap is unavailable.

## Repeatable SSH reset

The committed tool is `server/scripts/debug-set-balance.mjs`. It requires all of:

1. `--debug` as an explicit acknowledgement;
2. a non-production `NODE_ENV`; and
3. a database path containing `plinth-product-photos-debug`.

It also requires exactly one target selector: `--latest` or `--wallet <uuid>`.
It runs one immediate SQLite transaction, sets `balance` to the requested value,
and clears `refund_debt` and `purchase_hold` to zero so the credits are usable.

Copy the tool into the **debug** release and execute it with the debug environment:

```powershell
$slot = 'D:\Python\PlayKiln\apps\2026-10-15-plinth-product-photos'
scp "$slot\server\scripts\debug-set-balance.mjs" root@78.111.89.46:/opt/plinth-product-photos/current-debug/debug-set-balance.mjs
ssh root@78.111.89.46 "set -a; . /etc/plinth-product-photos/server-debug.env; set +a; cd /opt/plinth-product-photos/current-debug; node debug-set-balance.mjs --debug --latest --balance 10"
```

For a known wallet, replace `--latest` with (never add alongside it):

```powershell
ssh root@78.111.89.46 "set -a; . /etc/plinth-product-photos/server-debug.env; set +a; cd /opt/plinth-product-photos/current-debug; node debug-set-balance.mjs --debug --wallet <wallet-uuid> --balance 10"
```

The utility prints JSON with `before` and `after` values. Verify the isolated
debug surface afterwards:

```powershell
curl.exe -fsS https://pikupo.com/plinth-debug/health
```

The current `ledger.kind` CHECK constraint permits only `STARTER`, `PURCHASE`,
`RESERVE`, `SETTLE`, `RELEASE`, and `REFUND_ADJUST`; it does not permit
`DEBUG_TOPUP`. Therefore W82 deliberately records no fabricated ledger entry.
The database mutation is exactly:

```sql
UPDATE wallet
SET balance = 10, refund_debt = 0, purchase_hold = 0
WHERE id = :wallet_id AND deleted_at IS NULL;
```

It is executed within an immediate transaction by the tool. If a future schema
migration adds `DEBUG_TOPUP`, the tool must be changed in the same patch to add a
truthful ledger row; do not repurpose an existing accounting kind.

## W82 live action — 2026-10-17

The most-recent active debug wallet selected from `device_wallet`, `ledger`, `job`,
and `device` activity was `e6f7e2dd-04a4-4ff9-acd4-916b81dc680d`. An immediate
SQLite transaction on `/var/lib/plinth-product-photos-debug/server.sqlite` changed
its balance from 2 to 10; `refund_debt` and `purchase_hold` were already 0 and
remain 0. No production database or service was addressed.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Debug uses a separate development environment and database path. | MEASURED | 2026-10-17 SSH `systemctl show` plus `/etc/plinth-product-photos/server-debug.env`: `NODE_ENV=development`, `PLINTH_DEV_INTEGRITY=1`, `DATABASE_PATH=/var/lib/plinth-product-photos-debug/server.sqlite`. |
| The selected wallet was the most recent debug activity and started at 2 credits. | MEASURED | 2026-10-17 read-only SSH Node/SQLite query ordered wallet activity descending: wallet `e6f7e2dd-04a4-4ff9-acd4-916b81dc680d`, balance 2, debt 0, hold 0. |
| W82 changed that debug wallet to an available balance of 10. | MEASURED | 2026-10-17 SSH immediate transaction output: `before.balance=2`, `changes=1`, `after.balance=10`, `after.refund_debt=0`, `after.purchase_hold=0`. |
| The repeatable debug utility is deployed and can reset this wallet without a service restart. | MEASURED | 2026-10-17 copied `debug-set-balance.mjs` to `current-debug`; its live `--debug --wallet … --balance 10` output reported `before` and `after` as 10/0/0. |
| The repeatable utility refuses a production process environment. | MEASURED | 2026-10-17 live invocation with `NODE_ENV=production` returned `PRODUCTION_NODE_ENV_REFUSED`; the shell reported `production-guard-refused-as-expected`. |
| A `DEBUG_TOPUP` ledger record cannot be inserted under the present schema. | MEASURED | `server/migrations/001_init.sql:10-13` restricts `ledger.kind` to the six listed values. |
| DEV app-data clear can obtain a fresh starter grant. | ON RECORD | `docs/redesign/starter-credits-abuse-w66.md`, “Owner-phone review”: DEV simulates `recallBitFirst: false`; clear/reinstall creates a new install ID. |
| The debug health endpoint remained available after the reset. | MEASURED | 2026-10-17 `curl.exe -fsS -w "HTTP %{http_code}" https://pikupo.com/plinth-debug/health` returned `HTTP 200`, `ok=true`, and `databaseReachable=true`. |
