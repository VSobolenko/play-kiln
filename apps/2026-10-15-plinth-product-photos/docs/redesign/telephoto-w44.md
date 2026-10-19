# W44: Telephoto product inspection

Telephoto `me.saket.telephoto:zoomable:0.19.0` is used only by Card's full-screen
inspection dialog. Its `Modifier.zoomable()` wraps the already displayed bitmap
so users can pinch to zoom and pan; the normal Card preview and product thumbnails
remain unchanged. The bitmap is still produced by the existing load/composition
flow, and Telephoto does not participate in export or alter export pixels.

Coil remains the image-loading dependency for screens and thumbnails that use it.
This integration adds no icon pack and no client-side secret. Telephoto is
licensed under Apache-2.0.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| `0.19.0` is the latest stable version; Maven metadata lists only `1.0.0-alpha01` and `1.0.0-alpha02` after it, and reports alpha02 as the latest/release version. | MEASURED | `GET https://repo.maven.apache.org/maven2/me/saket/telephoto/zoomable/maven-metadata.xml` read 2026-10-17; the metadata lists `0.19.0`, then the two alpha versions, with `<latest>` and `<release>` set to `1.0.0-alpha02`. |
| The `zoomable` artifact's `Modifier.zoomable()` supports pan and zoom and can wrap an `Image`. | MEASURED | Telephoto v0.19.0 `docs/zoomable/index.md` and `zoomable/src/commonMain/kotlin/me/saket/telephoto/zoomable/Zoomable.kt`; upstream source at <https://github.com/saket/telephoto/tree/0.19.0>. |
| Telephoto is licensed under Apache-2.0. | MEASURED | Telephoto v0.19.0 `readme.md` license section and `license.txt`; <https://github.com/saket/telephoto/blob/0.19.0/license.txt>. |
| Pinch/pan improves product-photo inspection enough to justify the dependency. | UNVERIFIED | Requires a device review and user/product measurement; no device or Gradle run was performed for W44. |
