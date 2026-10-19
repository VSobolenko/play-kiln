# W24 - Coach as guided capture

Implements backlog item 5 in `modern-ai-android-2026.md` section 3. Source-only
worker delivery: **Comp UNVERIFIED**. No Gradle command, device run, dependency
change, commit, signing or release action was performed.

## Interaction

The next shot and completed count now precede the camera. A larger viewfinder
retains the marketplace aspect/fill guides, adds a high-contrast hint specific
to the shot, and is followed by a distinct level-status surface. Label/mark,
measurement and detail shots have different hints; reference shots are identified
before capture.

The shutter lives in `PlinthScaffold.bottomBar`, rather than inside the scrolling
instructions. PlinthScaffold remains responsible for safe drawing, navigation-bar
and keyboard insets; Coach does not add a second inset layer. Capture/import locks
the shutter, category changes and skipping. The camera capture snapshots category
and role before its asynchronous callback; failures leave a visible retry message.

Review replaces the live preview and scrolls to the top. The saved image is fit
without cropping. Four compact check rows put attention-needed results first;
their remedies remain visible. Manual/unmeasured checks are not presented as
successful measurements. The full measurement explanation is expandable. Use and
retake stay in the bottom action area, and the use label no longer asserts that
the photo is good enough.

The full shot list/skip action and item-type/marketplace rules remain available
behind expandable controls. This rejects a full-screen document ahead of the
viewfinder, not the underlying guidance. It also rejects fake live sharpness,
exposure or subject-fill scores: the existing quality input still contains only
image dimensions and preset rules.

## Handoff boundary

`CoachRoute` retains its public signature. Use still calls
`onUsePhoto(photoReview.handoff)`, completes the current shot and clears review;
retake clears review without completing a shot. Camera and picker imports still
use `CoachCaptureRepository`. Reference roles are unchanged. The existing caller
in `ui/PlayKilnApp.kt:636-642` pushes
`AutoCardDestination(handoff.item.id)`; no navigation or repository file was edited.

The framing overlay remains guidance, not an assertion that the saved original
was cropped to that aspect or that item boundaries were measured.

## Evidence and remaining checks

Executed from the repository root:

```powershell
python .kiln\scratch\copilot-plinth-w24-coach\check_coach.py
```

Observed exit 0: 90 keys in each Coach locale, 87 screen references resolved,
placeholder parity, unique Kotlin import symbols and balanced delimiters.
Source assertions also found the camera-first ordering, busy guard, original
handoff callback, reference role and AutoCard destination. These are textual
checks, **not compilation or behavioral tests**. Scoped `git diff --check`
also exited 0. English and Russian strings were updated together.

Integrator verification remains necessary: compile the integrated slot, then
exercise camera permission grant/denial, picker fallback, capture/import failure,
rapid repeated shutter taps, every category, skip, reference capture, retake and
Use -> AutoCard. Check compact/landscape windows, large text, TalkBack, bottom
insets, dark/light preview legibility and device level-sensor availability.
In particular, failure to reach AutoCard with the captured item's ID contradicts
the handoff claim; a second capture while saving contradicts the busy guard.

## Claim ledger

Paths below are relative to the slot; `CoachScreen.kt` means
`app\src\main\java\com\gexetr\plinthproductphotos\ui\coach\CoachScreen.kt`.

| Claim | Label | Evidence |
|---|---|---|
| Next-shot camera-first hierarchy and pinned capture/review actions exist in source | MEASURED | `CoachScreen.kt:272-405`, `:592-624` |
| Camera capture fixes category/role at shutter time and blocks repeated taps in source | MEASURED | `CoachScreen.kt:206-258`, `:293-306` |
| Existing handoff callback and caller destination remain wired in source | MEASURED | `CoachScreen.kt:282-284`; `ui/PlayKilnApp.kt:636-642`; scratch source-contract check exit 0 |
| Review distinguishes manual checks, missing rules and attention-needed results | MEASURED | `CoachScreen.kt:657-772`; no new quality measurements were added |
| EN/RU Coach keys, format arguments and screen references agree | MEASURED | Scratch checker output: 90 keys per locale, 87 references, exit 0 |
| Compilation, camera behavior, inset rendering and usable on-device AutoCard handoff | UNVERIFIED | Integrator: `node scripts\verify.mjs --slot plinth-product-photos --build`, followed by the device cases above; worker must not run Gradle |

Wall-clock observation: started 2026-10-17 01:05:50 +03:00; source checks finished
01:09:25 +03:00 (3m35s). Documentation and final scope inspection followed.
