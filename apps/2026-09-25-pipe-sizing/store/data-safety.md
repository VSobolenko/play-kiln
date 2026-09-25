# Play Console data safety answers — Gas Pipe Sizing Workbook

## Data collection and sharing

- Does the app collect user data? **No.**
- Does the app share user data? **No.**
- Does the app transmit user-entered values? **No.** Everything stays on the device.
- Does the app declare permissions? **No.** The shipped manifest contains no `<uses-permission>` entries.

## Data types

No data types are collected or shared: no personal information, financial information, files, photos, location, device identifiers, app activity, diagnostics, or other user data.

## Third-party SDKs

None. The capability declaration is `{"schemaVersion": 1, "enabled": []}`; there are no ads, analytics, account, cloud, or billing capabilities.

## Security and deletion

Network encryption is not applicable because the app has no network capability. Account deletion is not applicable because the app has no accounts. Users can clear local app data or uninstall the app.
