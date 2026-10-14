## Purpose

Sells and accounts for the credits that pay for server-side operations, with the server as the only
authority over purchases and balances, so that no client can mint credit, and keeps a seller's images and
data under stated limits. Detail: `annex/D-server-api.md` (API-00…API-15), `annex/C-data-model.md` (server
DDL), design D14 (settle after complete successful download), edge cases E36–E55.

## ADDED Requirements

### Requirement: Credits and the unlock are purchased through the store

The app SHALL sell consumable credit packs and one non-consumable unlock through the platform's billing,
and SHALL show each price in the seller's local currency from the store, never from a value in the app. The
app SHALL NOT offer a subscription. The unlock SHALL remove the watermark and the free batch limit, and
the app SHALL restore it after reinstall.

#### Scenario: Prices come from the store

- **WHEN** the purchase screen is shown in two different store regions
- **THEN** each price is the store's localized price for that region

#### Scenario: Unlock restored

- **WHEN** the app is reinstalled and the seller restores purchases
- **THEN** the unlock is active again with no new charge

#### Scenario: No subscription product exists

- **WHEN** the app queries the store for products
- **THEN** it requests no subscription product

### Requirement: Free and unlocked export limits are enforceable

Without the unlock, exported pixels SHALL carry a visible Plinth watermark and a batch SHALL contain at
most 5 items. The unlock SHALL remove both restrictions after server confirmation. If a marketplace
forbids watermarks, free export SHALL be blocked rather than falsely pass lint.

#### Scenario: Unlock removes both restrictions

- **WHEN** the server confirms the unlock entitlement
- **THEN** batches above 5 are allowed and exports contain no Plinth watermark

### Requirement: The server confirms every purchase

The system SHALL credit a purchase only after the server has verified the purchase token with the store's
developer interface, and SHALL do so exactly once per purchase. A purchase reported by the client without a
valid token SHALL NOT be credited. The client SHALL persist the token in an encrypted app-private Room
outbox before reporting it, retry with unique WorkManager work to a terminal response, and erase the raw
token afterwards. The server SHALL bind the Billing flow to a short-lived server purchase intent using
wallet-derived obfuscated account/profile ids and SHALL validate package, exact product/quantity, state,
token and both bindings. The server SHALL durably resume verification, fulfillment and Play finalization
across a crash and SHALL erase its envelope-encrypted token only after terminal consume/acknowledge.

#### Scenario: A forged purchase is refused

- **WHEN** a client reports a purchase with a token the store does not recognise
- **THEN** no credits are added

#### Scenario: A replayed purchase is credited once

- **WHEN** the same valid token is reported three times
- **THEN** the balance rises once

#### Scenario: A void exceeds the unspent balance

- **WHEN** Play voids a full or partial pack after some of its credits were spent
- **THEN** one idempotent adjustment reduces available credits to zero, records the remainder as nonnegative
  refund debt, blocks paid model jobs, and later purchased credits repay the debt before becoming spendable

### Requirement: The server holds the authoritative balance

The system SHALL keep the credit balance and its movement history on the server, SHALL reserve credits
before an operation, SHALL settle them only after the client has atomically stored the complete result,
verified its length and digest, and the server accepts an idempotent delivery receipt (design D14, E55), SHALL release them when the result expires without a valid receipt or the job fails, and
SHALL refuse an operation the balance cannot pay for. The client's display of the balance SHALL be derived
from the server.

#### Scenario: Insufficient balance

- **WHEN** an operation costs 5 credits and the balance is 3
- **THEN** the operation is refused before any model is called and the seller is offered the purchase screen

#### Scenario: Concurrent spends cannot overspend

- **WHEN** two operations that each cost 3 are requested at once against a balance of 4
- **THEN** one is accepted and the other is refused

### Requirement: Starter credits are small and bound to the device

When S4 proves Play Integrity Device Recall is enabled and read/write works for this Play app, the system
SHALL give a device the configured starter credits once using recall bit 1 and SHALL NOT give them again
after reinstall or device reset. If Device Recall is unavailable, unevaluated or cannot be written, the
session SHALL remain usable but SHALL grant zero starter credits.

#### Scenario: Reinstall does not refill

- **WHEN** the app is reinstalled on a device that already used its starter credits
- **THEN** no starter credits are granted again

#### Scenario: Process death does not lose fulfillment

- **WHEN** the client dies after Play returns a purchase but before server confirmation
- **THEN** the durable outbox retries the same token and fulfillment occurs at most once

### Requirement: Secrets never reach the client

The keys for the image models and for the store's developer interface SHALL exist only in the server's
environment. The application package SHALL contain no key, token or credential for either.

#### Scenario: The package is clean

- **WHEN** the project's client-secret check runs over the shipped client
- **THEN** it reports no finding

### Requirement: Cost caps and an emergency stop

The system SHALL enforce a per-device limit and a global daily spending limit on model calls, and SHALL
stop accepting paid operations, with a clear message to the seller, when the global limit is reached or an
operator switch is set.

#### Scenario: Global limit reached

- **WHEN** the day's model spending reaches the global limit
- **THEN** new operations are refused with a message and no model is called

### Requirement: Starter binding is not an install id

`installId` SHALL identify an installation only. Session creation SHALL use a Play Integrity Standard
request whose `requestHash` binds the server's single-use challenge and canonical session fields. Starter
grant SHALL use Device Recall bit 1 only after S4 proves it; without it the grant SHALL be zero and the app
SHALL make no reinstall-prevention claim.

#### Scenario: Binding unavailable

- **WHEN** no supported stable binding is available
- **THEN** the session works with zero starter grant and local capabilities remain available

### Requirement: Image retention and deletion

The system SHALL keep a seller's uploaded and generated images only as long as needed to deliver them, up to
a stated maximum, SHALL NOT record image contents in logs, and SHALL delete a device's server-side data on
the seller's request from within the app.

#### Scenario: Images expire

- **WHEN** the maximum retention time passes after delivery
- **THEN** the server no longer holds the images

#### Scenario: Delete my data

- **WHEN** the seller chooses to delete their data in settings
- **THEN** the server deletes images, reports, recovery/session/resource linkage and identifiable wallet
  data, revokes prior tokens, and retains only de-identified accounting and purchase replay rows that cannot
  restore access or balance

### Requirement: Delete revokes access

Delete SHALL revoke bearer and delivery tokens, invalidate recovery, delete owned resources and reports,
unlink devices and tombstone the wallet. Retained accounting/replay hashes SHALL be de-identified and
SHALL NOT restore balance or identify a device.

#### Scenario: Old token fails after delete

- **WHEN** deletion succeeds and an old token is used
- **THEN** authorization fails and no prior balance or image is recoverable

### Requirement: The privacy policy is reachable in the app

The app SHALL open the published privacy policy from settings, and the policy SHALL describe the photos
sent to the server, the processing providers, the retention limit and the deletion path.

#### Scenario: Policy opens from settings

- **WHEN** the seller opens the privacy policy in settings
- **THEN** the published policy opens
