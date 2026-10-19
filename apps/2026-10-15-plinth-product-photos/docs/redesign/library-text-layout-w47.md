# W47 — W41/W42 adjudication and Library text layout

## Adjudication

Navigation 3 is pinned to `1.2.0`; both runtime and UI artifacts reference the
same version in `gradle/libs.versions.toml`. W41's motion policy and
`SharedTransitionLayout` host are present in source. Its own report explicitly
leaves compilation, device transitions and gesture behavior unverified.

W42's Library polish is present. The project name uses a two-line ellipsis and
both single-line metadata labels use `TextOverflow.Ellipsis`
(`LibraryScreen.kt:345-365`). There are no unpaired `maxLines = 1` clamps in
the current Library source, so no further source edit was needed for the
reported text-layout failure.

## Verification boundary

`node scripts/verify.mjs --slot plinth-product-photos` was run source-only. It
reported `uiux.text-layout` **PASS** across 151 Kotlin files and ended with
122 PASS, 3 UNVERIFIED, 4 SKIP and 1 FAIL. The sole failure is `slot.device`:
the saved device record's build digest does not match the current source. The
build row is UNVERIFIED because `--build` was not used.

W40 was still running during this adjudication, so Gradle was not run. The
SharedTransition/NavDisplay wiring has no compile result in this pass; no
compile fix is claimed. The device digest was not edited or refreshed.

## Claim ledger

| Claim | Label | Evidence / limit |
|---|---|---|
| Navigation 3 runtime and UI use the `1.2.0` version reference. | **MEASURED** | `gradle/libs.versions.toml:11,60-61`. |
| W41 motion constants and shared-transition host are present in source. | **MEASURED** | `ui/theme/PlinthMotion.kt:9-10`; `ui/PlayKilnApp.kt:214,316-327`. |
| W41's SharedTransition/NavDisplay usage compiles and behaves correctly. | **UNVERIFIED** | No Gradle or device run; W40 owns the build/device work. |
| Every `maxLines = 1` label in Library shows truncation with ellipsis. | **MEASURED** | `ui/library/LibraryScreen.kt:356-357,364-365`; source verification reports `uiux.text-layout` PASS. |
| The full current slot passes source verification. | **NOT MET** | 122 PASS, 3 UNVERIFIED, 4 SKIP, 1 FAIL; the failure is the stale `slot.device` build digest, not text layout. |
