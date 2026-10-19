# W40 - AutoCard preview and device record

Run: 2026-10-17, started 02:04 +03:00. Device: `R58MB3SGZVL`,
Samsung `SM-A405FM`, Android 11. Debug only; no commit, release or purchase.
Evidence directory: `.kiln/scratch/device-plinth-w40/`.

## Cause and change

The W39 screen was not a permanently suspended coroutine. At the start of W40
its preview had eventually appeared (`00-stall.xml` / `.png`).
`AutoCardRoute` passes the source-sized canvas to `AutoCardEngine.compose`;
`Compositor.compose` calculates shadow sigma from canvas width.
`PixelBuffer.blur` previously scanned every Gaussian tap for every pixel in
both axes. For 2344 square pixels and sigma 46.88 this is about 3.1 billion
weighted samples. `AutoCardImageWork` runs off main but serializes this work
with preview bitmap creation, so the source/result bitmap also waits.
The results ViewModel is a completed-card cache, not the source of this stall.

The focused change replaces broad Gaussian shadows with six variance-matched
sliding box passes. Full halos preserve transparent-outside boundary behavior.
Small/thin/canvas-capped shadows remain exact. Product resolution and source
pixels are not reduced. Regression fixtures measure the approximation against
the old Gaussian, with maximum observed alpha error 4/255.
An existing misplaced closing brace in the AutoCard navigation branch was
relocated to close the surrounding SharedTransitionLayout; the compiler had
rejected the intermediate worktree.

Other workers changed UI/resources and authored documentation during this run.
Their edits were preserved. The final installed build includes the then-current
UI/resources. Later documentation-only changes affect the factory source digest
but not the compiled APK; the final digest is computed after this report exists.

## Device journey

- Final built and pulled installed APK both SHA-256:
  `8535b0e86f62f14fde1f3683ff71f06b19d94b0457a28f69e868f45fed72efd5`.
  `apk-hashes.json` and `installed-base.apk` are the byte-equality evidence.
  The install helper's immediate UI dump returned "null root node"; the install
  and hashes succeeded, and a separate subsequent dump recovered normally.
- Studio Continue editing opened the 2344 x 2344 product photo. Manual category
  Top showed "Preparing your card preview..." in `13-final-chosen.xml`
  at 02:33:56.430, then the real "Product photo card preview" in
  `14-final-preview.xml` at 02:34:08.125. This is a bounded observation, not an
  exact render duration: the category tap preceded the first dump by a few
  seconds. The preview completed within about 20 seconds, not instantly.
- Create card advanced to "Saving the AI card to this project...", then
  "Your AI smart card is saved as a project output" with Share AI card and
  Save to folder (`15-final-result.xml`, `16-saved-result.xml`).
- Save to folder opened Android DocumentsUI; SAVE wrote
  `/sdcard/Download/auto-card-977ed723-7f7d-48fc-b4e3-ada40828e7c2.png`.
  ADB observed 688302 bytes and pulled it to `exported-card.png`.
  PNG is 2344 x 2344, SHA-256
  `bf440b703472e9528c3132b2c6601e964724b7cd5e04ce157a91d2cec9bc00eb`.
  Visual caveat: the exported image has a green cast; file export is proven,
  colour fidelity is not accepted by this run. No unrelated encoder change
  was made.
- Library and More appeared after single taps; Settings opened, scrolling
  revealed About this app, About displayed version 0.1.0-debug (1), and Back
  returned to the same Settings position (`19-library` through `24-about-back`).
  Subjective feel: OK, no multi-second visible stall on these transitions.
  This is not a claim of jank-free animation: whole-session gfxinfo reports
  333/901 janky frames (36.96%), median 13 ms, p95 73 ms, p99 400 ms, including
  image processing/loading. ADB input itself took about 1.05 seconds and is not
  a measurement of app transition latency.
- Logcat: `preview-logcat.txt`, `final-logcat.txt`; no matching FATAL EXCEPTION,
  ANR-in or OutOfMemoryError in the captured final buffer. No `kill -3` used.

## Claim ledger

| Claim | Status | Evidence |
|---|---|---|
| Source-resolution Gaussian was excessively slow | MEASURED / code-backed | Worker RED regression 8614 ms against 2000 ms budget; `PixelBuffer.kt` blur loops; W39 delayed preview subsequently visible in `00-stall.xml` |
| New shadow preserves tested Gaussian profile within 4/255 alpha | MEASURED | `app/build/test-results/testDebugUnitTest/TEST-com.gexetr.plinthproductphotos.domain.PixelBufferShadowTest.xml`, six tests, full-resolution shadow 754 ms in final Gradle run |
| Slot unit suite passes | MEASURED | `node scripts/run-slot-tests.mjs --slot plinth-product-photos --rerun`; `unit-tests.log`: 280 tests, zero failures/errors/skips |
| Debug build compiles | MEASURED | `node scripts/verify.mjs --slot plinth-product-photos --build`; `verify-build-final.log`: build PASS, 6051 ms; at that point only stale device record failed |
| Final installed APK matches built bytes | MEASURED | ADB install/pull and `apk-hashes.json`, hash above |
| AutoCard preview, creation, persistence and file export complete | MEASURED | `13-final-chosen.xml` through `18-exported.xml`; pulled PNG; no claim that green cast is correct |
| W37 tab / Settings / About feel retained | OBSERVED, not frame-perfect | `19-library.xml` through `24-about-back.xml`; gfxinfo caveat above |
| Current device record matches final digest | Independently checked after writing record | Final `node scripts/verify.mjs --slot plinth-product-photos`; see `verify-final.log` |

Elapsed wall-clock for investigation, worker implementation, rebuilds and phone
journey: approximately 34 minutes. This is transparency, not a selection or kill
criterion. Signed artifacts and publish authorization remain outside this task.
