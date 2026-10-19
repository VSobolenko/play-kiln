# W103 — Final device pass: progress banner, then wallet at 10

Device `R58MB3SGZVL` read `SM-A405FM`. Gradle not run. Debug APK sha256
`F9C4BB33DDDB70F276FD654DC66C9725E61FEFA753CDBBB5EEDCB399AB91138D`
(`app/build/outputs/apk/debug/app-debug.apk`, built 05:49, after the W101 source edits at 05:34), installed with
`install -r` (Success). Screenshots are in `.kiln/scratch/w103/` (disposable).

## P1 — progress banner: PASS (banner); button in place but below the fold

Relight, "warm sunset light from the left", IME dismissed, fresh dump, tap `Relight · 5 credits`. No scrolling.
Polled with `uiautomator dump` (each poll ~1 s of dump time, then 3 s sleep ≈ 5.4 s period).

| Time since tap | Banner text seen | Button state | In viewport? |
|---|---|---|---|
| 1.1 s (first poll) | "Relighting…" + "This can take a minute or more. Keep the app open — your result will appear here." | not in viewport (below the fold, LazyColumn does not compose it) | banner YES, bounds [48,308][1032,482] |
| 7.6, 13.9, 20.2 … 144.7 s (26 polls, every ~5.4 s) | identical on every poll | not in viewport | banner YES at every poll |
| ~150–225 s | still shown at each further poll | — | YES |
| ≈ 220–230 s total | banner gone, Before/After shown | — | — |

The banner was on screen at every poll from the first (1.1 s) until the result; it never left the viewport.
The relight took roughly 3.5–4 min this time (last pass ≈ 65 s) — a service-side duration, not an app change.

**Button state** (second Relight, "cool moonlight from the right"): tap, then ONE deliberate scroll (after the
no-scroll check) → the primary button sits at [428,1824][653,1884] labelled "Relighting…", greyed (screenshot
`p1_btn2.png`: dimmed pill, grey text), in place, not removed. `uiautomator` reports `enabled=true` on that
node, so "disabled" is judged from the greyed pixels and not from the accessibility flag. Second run took 232 s.
Without scrolling, the button is NOT in the viewport (the preview card is tall) — only the banner is. The brief's
"button showing the progress label … in place" is therefore true but not visible without a scroll; the banner is
what answers the user's "is something happening".

**Auto-scroll: YES.** On both Relight results Before/After was in the viewport without my scrolling
(`Before` [48,639][522,688], `After` [558,639][1032,688]; the second run had been scrolled by me and still
jumped to the result). The banner disappeared when the result arrived.

## P2 — Scene: PASS
Chip "Tent campsite" → `Create background · 2 credits`. Banner "Creating the background…" at 1.1, 6.5, 10.9 s;
result at ≈15 s. After: the mug on a dirt path in a forest clearing, blurred trees, an orange tent at the upper
right — clearly different from the pale-grey input background (`s_result.png`).

## P3 — editor labels: PASS
Cutout → Select all → `Cut out item · 1 credit` → review → Edit edges. "Remove from selection" [90,1325][492,1469]
and "Restore to selection" [588,1325][990,1469] each render on two centred lines inside their pills; no character
is clipped at either edge (`editor2.png`).

## P4 — nothing else regressed: PASS
- Create: choose photo (picker: TAB ×11 to `plinth-cup.jpg`, ENTER), one primary action `Create product card`
  (plus a secondary link "Or create an AI background"), then `Save image` → system save dialog (Downloads,
  product-card.png) → SAVE → "Saved to Downloads." + Done. Exactly one save action; destination named.
- Recolour first visit: no Before/After; button `Recolour · 2 credits`.
- No credit/charge line under the Relight, Scene or product-card result (dump filter for charge/credit/Used found none).

## Wallet
Phone chip arithmetic: 30 → Relight 5, Scene 2, Relight 5, Cutout 1 → 17 read on the phone (one charge per action).
Wallet used by the phone: `57d9f223-de89-495d-89d4-a10c5348bc35` (`--latest` picked it; the phone read 30 after it
was set to 30 and 17 before the reset, so it is the phone's wallet).

```
ssh root@78.111.89.46 "set -a; . /etc/plinth-product-photos/server-debug.env; set +a; cd /opt/plinth-product-photos/current-debug; node debug-set-balance.mjs --debug --wallet 57d9f223-de89-495d-89d4-a10c5348bc35 --balance 10"
```
Output: `before.balance 17 → after.balance 10`. After force-stop + relaunch + Profile tab the top bar read
"Credits · 10" (`final_balance.png`).

## Gaps
- Dump `enabled` is true for the progress button; disabled state is visual only.
- Button not visible without a scroll while the banner shows.
- The Relight "After" (first run) was a green flat background with the mug, not obviously "warm sunset" — service quality, not checked further.
- Cyrillic UI / input still not covered.
- Nav bar taps at "Profile" report bounds [0,0]; used only as a wallet-refresh trigger.
