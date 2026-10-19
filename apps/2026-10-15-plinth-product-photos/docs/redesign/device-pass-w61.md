# W61 - device pass after W58/W59/W60

## Revision and artifact

**MEASURED:** `adb devices -l` listed `R58MB3SGZVL` as `device`;
`adb -s R58MB3SGZVL shell getprop ro.product.model` returned `SM-A405FM`,
and Android release was `11`. All device commands addressed that serial.

**MEASURED:** `node scripts/verify.mjs --slot plinth-product-photos --build`
ran `assembleDebug` successfully. The first invocation and a later stable
rebuild both reported the then-stale `device-verification.json` as the only
FAIL; the build row was PASS in both. Before the final rebuild,
`releaseSourceDigest('apps/2026-10-15-plinth-product-photos')` returned
`c5cc05bdbbde1123ef205f4e82c2744870584c36f2f189c0c030963ed2e37702` over
527 inputs; the same digest and count were read after the build.

**MEASURED:** The rebuilt debug APK was installed as
`com.gexetr.plinthproductphotos.debug`. `Get-FileHash -Algorithm SHA256` on
the local APK and the pulled installed `base.apk` returned the same hash:
`c36873d22413042150280be6da77377c938830dce88aa54f4d251e00103344e1`.
The on-device package reported `versionName=0.1.0-debug`,
`versionCode=1`, `targetSdk=36`. This is debug-device evidence only.

## Screen observations

**MEASURED:** After launching the installed build with `monkey`, the Studio
landing screen displayed a modern rounded credits pill (`+`, `Credits · 10`)
and the five bottom-navigation destinations on one line, in this order:
**Studio | Library | Create | Batch | More**. The UI hierarchy labels the pill
`Credits · 10. Opens Wallet to top up.` Evidence:
`.kiln/scratch/copilot-plinth-w61-device-pass/final-studio.png` and
`final-studio.xml`.

**MEASURED:** The hierarchy gave Library clickable bounds
`[231,1986][438,2110]`; tapping their center `(330,2048)` opened Library.
The screen showed `Your projects`, search, Filters, Select projects and
`No saved projects yet`. Evidence:
`.kiln/scratch/copilot-plinth-w61-device-pass/final-library.png`.

**MEASURED:** From Library, the hierarchy gave Studio clickable bounds
`[24,1986][231,2110]`; tapping their center `(127,2048)` returned to Studio.
The H.264 capture decoded to 96 frames at 25 fps (3.84 seconds). Sampled
frames show Library through about 0.8 seconds, then Studio rendered by about
0.9 seconds and stayed rendered; no transient fully blank transition frame
was observed. The no-photo silhouette on Studio is the stable empty-input
placeholder, not a blank transition frame. Evidence:
`.kiln/scratch/copilot-plinth-w61-device-pass/final-library-to-studio.h264`
and `final-motion-sequential.png`.

## Limits and verification

**UNVERIFIED:** The W56 record described a brief empty photo area while
entering the photo-backed Smart card editor. This device currently has no
saved projects, so this pass exercised the Studio landing destination, not
that photo-backed editor transition. No photo was imported and no user
gallery content was selected; the earlier editor glitch is not declared
fixed by this pass.

**MEASURED:** The refreshed record is
`apps/2026-10-15-plinth-product-photos/device-verification.json`. The
post-record `node scripts/verify.mjs --slot plinth-product-photos` output
records `slot.device PASS` (123 PASS, 3 UNVERIFIED, 4 SKIP; exit 2). The
unverified rows are pack inventory/media, which have no store assets, and the
source-only build row. This run is not an overall PASS and is not build
evidence; the build evidence is the `--build` run above. Output:
`.kiln/scratch/copilot-plinth-w61-device-pass/1792232669655-copilot-tool-output-130904-7b15e771-74d3-46ca-bb80-d52ab5c753a6.txt`.

No product UI code was edited. No release approval, signing, store-pack work,
or publishing was performed. Wall-clock elapsed time from the request at
13:15:40 +03:00 through the final slot check at 13:24:16 +03:00 was about
8 minutes 36 seconds.

| Claim | Label | Evidence |
|---|---|---|
| Correct phone and Android version | MEASURED | `adb -s R58MB3SGZVL shell getprop ro.product.model` → `SM-A405FM`; `ro.build.version.release` → `11` |
| Current revision built | MEASURED | `node scripts/verify.mjs --slot plinth-product-photos --build`; `assembleDebug` PASS; matching before/after digest over 527 inputs |
| Installed APK equals built APK | MEASURED | Local and pulled `base.apk` SHA-256 both `c36873d2…03344e1` |
| Five-tab navigation and credits pill | MEASURED | `final-studio.png`, `final-studio.xml` |
| Library-to-Studio landing transition showed no fully blank frame | MEASURED | `final-library-to-studio.h264`, sampled contact sheet `final-motion-sequential.png` |
| Photo-backed editor transition is glitch-free | UNVERIFIED | No saved project was available to reproduce the W56 path; no gallery photo was selected |
| Current device record agrees with the slot revision | MEASURED | Post-record `node scripts/verify.mjs --slot plinth-product-photos`: `slot.device PASS` |
