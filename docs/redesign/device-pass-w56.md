# W56 - final-revision device digest refresh

## Revision and device

MEASURED: `adb -s R58MB3SGZVL shell getprop ro.product.model` returned
`SM-A405FM`; Android release was `11`. All device commands explicitly addressed
that serial. No release approval, signing, publication or product UI edit was made.

Worker polling began at 12:04:45 +03:00 on 2026-10-17. W51, W52, W53 and W54
all reported `DONE` by 12:21:57. Evidence:
`.kiln\scratch\device-plinth-w56\preflight.txt`.
The first build succeeded, but later digest sampling detected source drift:
`add1426f3829...` became `788285975f6e...`. W57 was then reported running;
its `DONE` was observed at 12:28:07. W56 waited and rebuilt rather than
attaching the new digest to the older install. Evidence:
`preflight-extra.txt`, `verify-build-before-w57.txt`, `digest-final-build.txt`.
No other worker's source files were edited.

MEASURED: the final `node scripts\verify.mjs --slot plinth-product-photos --build`
ran exactly one subprocess, `assembleDebug`, which passed in 9367 ms.
The full run took 21696 ms and exited 1 solely for the old `slot.device` record;
store inventory/media were `UNVERIFIED` because no store assets exist.
Log: `.kiln\scratch\device-plinth-w56\verify-build.txt`.
The two builds were sequential, never concurrent Gradle consumers.

`adb ... install -r` returned `Success`. The installed debug package was
`com.gexetr.plinthproductphotos.debug`. `pm path` identified its `base.apk`,
which was pulled to `.kiln\scratch\device-plinth-w56\installed-base.apk`.
`Get-FileHash -Algorithm SHA256` returned the same value for built and installed APK:

```text
apkSha256:   a98dc668ee55f211ef5e252137b79378205a0b1691eb96418069b5622bc25550
buildDigest: e8c0599317ba5e1fb3ce120a366e156d7c2f705242eaabb57996c8031f3a1544
input files: 522
```

`releaseSourceDigest` from `scripts\lib\release-approval.mjs` returned that
digest before the final build at 12:28:27 and after the journey at 12:30:57.
The record itself is not a build input. This is development evidence, not
evidence about an unsuffixed release package.

## Final installed-build journey

MEASURED: screenshots were visually read, with actions located from the current
UI hierarchy rather than guessed. Every evidence filename below is under
`.kiln\scratch\device-plinth-w56\`; PNGs and XML dumps share the basename.
`actions.txt` records the actual taps. Files without `final-` belong to the
superseded first install and are not evidence for the final digest.

| Action | Observation | Evidence |
|---|---|---|
| Launch debug package using `monkey` | Studio Home, original glasses photo, Credits: 10, Continue editing, Choose a scene, central plus; teal actions and shaded header/bottom chrome | `final-01-home` |
| Continue editing at (540,1220) | Smart card, original 2344 x 2344 photo, Wildberries, recognition cost 0 and balance 10, teal recognition/manual-category actions | `final-02-studio` |
| Library at (324,2109) | Your projects, search, teal Filters/Select projects, saved plinth-cup.jpg | `final-03-library` |
| Saved preview at (285,1084) | Make a card, marketplace not selected, original image, teal ready-to-compose label | `final-04-project` |
| Make card at (540,1794) | Teal rendering spinner followed by composed image, Draft rendered and teal Save draft PNG | `final-05-draft.png`, `final-05b-draft-settled` |
| Centre create at (540,1992) | Import photos, own-product-photo explanation, up to five photos, teal Choose photos | `final-06-create` |

The first final-build launch dump returned `null root node`; capture was retried
after launch settled, replacing `final-01-home.png` with the actual Home.
`final-05-draft.png` caught the rendering overlay before the following XML dump
observed completion; the explicitly recaptured `final-05b-draft-settled` pair
is the evidence for the stable draft state.

## Motion and limits

MEASURED: a six-second `adb -s R58MB3SGZVL exec-out screenrecord
--output-format=h264 --time-limit 6 -` capture surrounds Home -> Smart card.
`final-home-to-studio.h264` decoded into 118 frames with existing OpenCV;
the selected frames were visually read in `final-motion-frames.jpg`.
Frames 12/14 show the photo changing placement/size while the destination opens;
the bottom navigation remains present. This establishes visible transition
states, not a frame-rate or predictive-back acceptance verdict.

**Observed caveat:** frame 115 shows a temporarily empty photo area, restored
in frames 116-117. Do not interpret `slot.device` PASS as certifying glitch-free
shared motion. No UI fix was attempted in this evidence-only task.

The UI accents on the exercised screens looked teal, not lime. The existing
Library thumbnail contains green-tinted image content; that is not an accent
claim. Light theme, dynamic colours, predictive back, AI recognition/generation,
the AutoCard generated-result screen, export and new-photo selection remain
UNVERIFIED in this bounded pass. The local Make card draft was rendered, not saved.

## Verification and claim ledger

The refreshed device record has six observations, the confirmed serial,
installed APK hash and final source digest. The requested source-only rerun
is retained as `.kiln\scratch\device-plinth-w56\verify-final.txt`.

MEASURED at 12:32:35 +03:00: the source-only rerun reported `slot.device PASS`,
including both matching buildDigest and matching APK hash: **123 PASS,
3 UNVERIFIED, 4 SKIP, zero FAIL**, exit 2, zero subprocesses, 12396 ms.
The three unverified rows are deliberately unproduced store inventory/media
and the build not rerun without `--build`; the preceding final-build log
provides the compilation evidence. This is not an overall factory PASS.
An additional `releaseSourceDigest` read at that time still returned the
full digest above, with `recordMatches: true` and six observations.

Wall-clock time from the request timestamp (12:02:03) to final verification
(12:32:35) was approximately **30 minutes 32 seconds**, including worker waits.

| Claim | Label | Evidence |
|---|---|---|
| Correct physical phone addressed | MEASURED | Explicit-serial getprop returned SM-A405FM / Android 11 |
| Final debug build compiled | MEASURED | `verify-build.txt`: build PASS, 9367 ms, one subprocess |
| Installed APK equals final built artifact | MEASURED | Get-FileHash on local APK and pulled installed-base.apk: full equal hash above |
| Final installed revision exercised on Home/Studio/Library/create | MEASURED | `final-01` through `final-06` PNG/XML pairs and `actions.txt` |
| Teal chrome/actions and visible photo transition states | MEASURED | Visually read final PNGs and `final-motion-frames.jpg` |
| Refreshed slot.device matches the current revision | MEASURED | `verify-final.txt`: slot.device PASS, digest and APK hash both match |
| Transition is free of glitches | UNVERIFIED | Empty photo area in frame 115 contradicts an unconditional acceptance |
| Release or paid-generation readiness | UNVERIFIED | Outside this task; no approval or paid action performed |
