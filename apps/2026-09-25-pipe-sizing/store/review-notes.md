# Reviewer notes — Gas Pipe Sizing Workbook

No account, login, invitation, or demo credentials are required. The release is paid once through the store listing; the app contains no ads, subscription, in-app purchase, or billing capability.

## Review journey

1. Launch the app.
2. Enter a material or system, a pressure class, a load, a developed length, and a fitting allowance.
3. Choose whether the capacity-like reading is user-supplied or comes from a code citation, and if cited record the edition, table, and row.
4. Confirm the capacity, then evaluate to review the arithmetic and the calculation-aid disclaimer.
5. Evaluate with a required input missing, for example an unconfirmed capacity, to see the guidance name what must be corrected rather than showing a partial number.

The app performs arithmetic and bookkeeping only. It supplies no code-table data and does not determine compliance or certify work. It restores the current case after the process ends by reading it back from local storage, and keeps no history of previous cases.

## Permissions, data, and media

The manifest declares no permissions. No data is collected, transmitted, or shared; the entered case is local to the device.

The phone screenshots are real captures of the signed release build, driven by `store/journey.json` on a phone AVD. The tablet screenshots are that same phone capture framed at tablet dimensions rather than captures taken on a tablet device, so the large-screen eligibility they support should be re-confirmed on real large-screen hardware before upload.

## Capability compliance

- Enabled: none
- Consent required: **No**
- Purchase authority: Not required — no SERVER_COST consumables are declared.

## Human checks required before upload

- TODO-HUMAN: Replace `support@example.com` with a monitored support address if it is still a placeholder.
- TODO-HUMAN: Host `store/privacy.md` publicly and replace `https://example.com/privacy` with its final HTTPS URL.
- TODO-HUMAN: Confirm the data-safety answers and permission list against the release AAB and all bundled SDKs.
- TODO-HUMAN: Replace the phone-derived tablet screenshots with captures from a real large-screen device.
- TODO-HUMAN: Complete the Play Console legal declarations and first-app setup.

Version reviewed: `0.1.0` (code `1`)
