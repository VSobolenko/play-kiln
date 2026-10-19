# W64 - Device pass after Studio scroll rewrite

## Result

**MEASURED:** Studio responds to repeated scrolling in both directions on
`R58MB3SGZVL` / `SM-A405FM`, Android 11. The current debug APK was installed and
its installed bytes matched the build. Three frames were captured and visually
inspected. **INFERRED: jank is not gone**: the capture-free repeat still reported
465/1064 janky frames (43.70%), with p95 40 ms. This is a device-evidence refresh,
not a smoothness acceptance or release approval.

**UNVERIFIED:** improvement versus the prior implementation, and subjective
human scroll feel. No comparable pre-W62/W63 frame-time baseline was measured in
this session. W62 and W63's reports explicitly left device/frame-time checks
unverified. Static frames cannot establish continuous motion quality.

## Artifact and procedure

- Package: `com.gexetr.plinthproductphotos.debug`.
- Artifact: `app\build\outputs\apk\debug\app-debug.apk` (slot-relative).
- Local and pulled installed APK SHA-256:
  `ff219a78585b7ea139c0c3f5b23531823e53e5f4dcb900c530e08264de5b0e6b`.
- Initial `releaseSourceDigest` from `scripts\lib\release-approval.mjs`:
  `81ceaf0b42b3c6d26b31b4102367fcacbb328881d66c7be044369ffdf1f558fb`,
  530 files; equal before and after the timed scroll runs. The final digest is
  in `device-verification.json` and `final-build-digest.json`: this report itself
  is included in the factory's digest, although it does not change APK bytes.
- Run date: 2026-10-17, approximately 13:35-13:41 +03:00 for build/install/scroll.

All evidence below is under `.kiln\scratch\device-plinth-w64\` at the repository
root. `device-commands.txt` records the serial-addressed commands and timestamps;
`run-device.cjs` preserves the repeatable gesture/capture procedure. No other
device was driven, no application data was cleared, and no device-wide settings
were changed.

Ran `node scripts\verify.mjs --slot plinth-product-photos --build`.
`initial-verify.txt` records build PASS, `assembleDebug` in 23894 ms, but overall
exit 1: the old `slot.device` digest was stale. The two separate pack checks were
UNVERIFIED because development has no store assets; no release pack was produced.

Installed with `adb -s R58MB3SGZVL install -r`, which returned `Success`;
force-stopped only the tested package and launched with `monkey`. The first
hierarchy dump immediately after launch returned a null root; a later dump
succeeded, and only that successful hierarchy informed the gestures.
Pulled the installed `base.apk` and compared SHA-256 to the built APK
(`device-commands.txt`, `installed-base.apk`).

The successful hierarchy gave the vertical list bounds
`[0,278][1080,1968]`. Gestures stayed within that viewport:

```text
adb -s R58MB3SGZVL shell input swipe 540 1750 540 550 600
adb -s R58MB3SGZVL shell input swipe 540 550 540 1750 600
```

Each timed pass used three cycles of three finger-up gestures and three finger-down
gestures: 18 swipes per pass, 36 total. The first pass included one intermediate
screenshot/hierarchy capture. The second pass had no captures in its measured
interval. Each pass reset package `gfxinfo` before scrolling and read
`dumpsys gfxinfo com.gexetr.plinthproductphotos.debug framestats` afterward.
These are debug-build Android aggregate counters, not a release benchmark or a
controlled causal comparison. The warm second pass differs from the first in
both warming and capture overhead, so their difference is not credited to W62/W63.

## Frames and observations

| Evidence | MEASURED observation |
|---|---|
| `01-studio-top.png` and `.xml` | Selected Studio, Credits pill, static empty-photo hero, import button, scene heading and Top/Bottoms gradient swatches; five navigation labels visible |
| `02-studio-bottom.png` and `.xml` | After three finger-up gestures: Recolour, Erase region, Upscale, Virtual model and Write listing text; fixed header and navigation remain visible |
| `03-studio-return-top.png` and `.xml` | After repeated bidirectional gestures: hero and scene swatches rendered again; Studio remains selected |

The second filename denotes lower content, **not proof of reaching the absolute
list end**. These samples show rendered content rather than blank screens at the
sampled positions; they do not exclude transient blank frames between samples.
The same app PID (1936) was returned after both passes.

## Timing

| Android gfxinfo counter | First pass, with intermediate capture | Clean repeat, no captures |
|---|---:|---:|
| Rendered frames | 913 | 1064 |
| Janky frames | 573 (62.76%) | 465 (43.70%) |
| p50 | 19 ms | 15 ms |
| p90 | 38 ms | 30 ms |
| p95 | 53 ms | 40 ms |
| p99 | 97 ms | 61 ms |
| Frame deadlines missed | 122 | 94 |

Sources: `gfx-scroll.txt` and `gfx-clean-scroll.txt`. A repeat with no
intermediate capture still has substantial missed-frame evidence. Therefore
**do not close the scroll-jank issue as fixed** on this run. No UI changes were
authorised or attempted.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Current debug APK builds | MEASURED | `initial-verify.txt`, build PASS |
| Tested install equals built APK | MEASURED | SHA-256 pair in `device-commands.txt`; `installed-base.apk` |
| Studio moves to lower tool rows and back to hero | MEASURED | Three PNG/XML pairs and 36 logged swipe commands |
| Residual jank remains | INFERRED | Capture-free `gfx-clean-scroll.txt`: 43.70% janky, p95 40 ms |
| Better than pre-W62/W63 | UNVERIFIED | Requires a same-device, same-scenario baseline; not established here |
| Human-perceived smoothness | UNVERIFIED | Requires continuous-motion/human observation; screenshots do not prove it |

## Record closure

The first refresh check (`refresh-before-closure.txt`) built successfully but
still failed `slot.device`: adding this report changed the digest from
`81ceaf0b42b3...` to `5e40919edc76...`. This was not an APK/source-code change;
the local APK retained the installed SHA-256 above. The digest implementation
includes slot documentation and excludes the device record itself.

Closure procedure: finish this report, read the final digest, replay one
additional down/up gesture pair on the unchanged APK, inspect the resulting
hierarchy, then write that digest into the device record. This adds two
untimed witness gestures to the 36 timed gestures; it is not another benchmark.
`closure.txt`, `closure.xml` and `final-build-digest.json` carry that evidence.
The final authoritative command is
`node scripts\verify.mjs --slot plinth-product-photos --build`;
its complete output and actual verdict are in `final-verify.txt`. The device
sensor verifies record currency, **not** smoothness; its PASS must not overrule
the residual-jank finding above.

Only the device record and this report are delivery edits. No release approval
was recorded or changed; no signed artifacts or store screenshots were produced.
