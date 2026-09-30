# Device capture readiness — can this machine produce real Play screenshots?

Date: 2026-09-23
Host: Windows 10.0.19045, `d:\Python\PlayKiln`
Author: capture-readiness probe (one-off investigation, no repo sources changed)

## Verdict

**Yes — real device captures are possible on this machine right now.** An Android
emulator is installed, hardware-accelerated, and boots; `adb` can install an APK and
`screencap` returns real, readable PNGs of the shipped app. The existing factory
tooling (`scripts/media-capture.mjs` → `scripts/store-kit.py`) produced a **complete,
validated store pack from real device captures** — 6 phone, 4 seven-inch and 4
ten-inch screenshots in both locales, all `PASS`.

That proof was run in a **temporary sandbox** (`%TEMP%\pk-capture-sandbox`) containing
copies of the scripts and of the `2026-09-22-smoke` slot, so no file under
`apps/**`, `scripts/**`, `capabilities/**` or `openspec/**` was touched. The emulator
was **stopped** before this report was written (`adb devices` is empty again).

Four real gaps remain between "captures work" and "a store pack that honestly shows
the shipped app":

1. **One resolution only.** Capture happens once at whatever the device screen is, and
   the framing step `cover()`s those same 6 images into the phone *and* both tablet
   sets (`store-kit.py:679-692`). The tablet assets are therefore real pixels at the
   wrong device size, not tablet renderings.
2. **Both locales share one capture pass.** The same 6 raw PNGs are framed into
   `en-US/` and `ru-RU/` with different captions; only the caption band differs,
   so `ru-RU` screenshots show the **English** UI. Measured with PIL: the difference
   bounding box between `en-US/phone-01.png` and `ru-RU/phone-01.png` is
   `(83, 116, 872, 168)` — a ~52 px caption strip, nothing else.
3. **The debug APK cannot be driven by the current script as-is.** The debug APK
   installs as `com.gexetr.smoke.debug`, but `media-capture.mjs` launches
   `listing.json → applicationId`, which is `com.gexetr.smoke`.
4. **No slot has a `store/journey.json`.** The script silently writes a placeholder
   journey ("Edit coordinates/text for the real app") whose taps are hard-coded for a
   1080×1920-ish screen, so the first run captures whatever those coordinates hit.

None of these is a blocker for a phone-only, English-first capture; all four need a
small change to `scripts/**` (which this task explicitly forbade), so they are reported,
not fixed.

---

## 1. Android SDK and platform tooling

Environment (real output):

```text
=== ANDROID_HOME ===
E:\Programm\Android\SDK
=== ANDROID_SDK_ROOT ===
E:\Programm\Android\SDK
=== JAVA_HOME ===
C:\Program Files\Java\jdk-17
=== PLAYKILN_JAVA_HOME ===
(empty)
=== ANDROID_AVD_HOME ===
E:\Programm\Android\avd
```

`adb` is on `PATH`:

```text
$ adb version
Android Debug Bridge version 1.0.41
Version 37.0.1-15733141
Installed as E:\Programm\Android\SDK\platform-tools\adb.exe
Running on Windows 10.0.19045
```

`emulator`, `sdkmanager` and `avdmanager` are **not** on `PATH`:

```text
$ emulator -version
emulator : The term 'emulator' is not recognized as the name of a cmdlet, function, script file, or operable program.
$ sdkmanager --version
sdkmanager : The term 'sdkmanager' is not recognized ...
$ avdmanager --version
avdmanager : The term 'avdmanager' is not recognized ...
```

The emulator binary exists inside the SDK, and it works when called by full path:

```text
$ E:\Programm\Android\SDK\emulator\emulator.exe -version
Android emulator version 37.1.11.0 (build_id 15917651) (CL:N/A)
...
INFO         | Android emulator version 37.1.11.0 (build_id 15917651) (CL:N/A)
INFO         | Graphics backend: gfxstream
```

SDK root contents (`E:\Programm\Android\SDK`):

```text
.downloadIntermediates  .sdk  .temp  build-tools  cmake  emulator  fonts
licenses  ndk  platform-tools  platforms  skins  sources  system-images
.knownPackages
```

**`cmdline-tools` does not exist** — `Test-Path E:\Programm\Android\SDK\cmdline-tools`
→ `False`, and `E:\Programm\Android\SDK\tools` → `False`. That is why `sdkmanager`
and `avdmanager` are missing.

Installed platforms:

```text
android-33
android-34
android-35
android-36
android-37
android-37.0
```

Installed build-tools (the contract's `36.0.0` is present):

```text
34.0.0
35.0.0
35.0.1
36.0.0
```

Installed NDK: `27.0.12077973`, `27.1.12297006`. Licenses: `android-sdk-license`.

Installed system images (all `x86_64`, all with `system.img` present):

```text
system-images\android-36\default
system-images\android-36\google_apis_playstore
system-images\android-36\google_apis_playstore_ps16k
system-images\android-37.1\google_apis_playstore_ps16k
system-images\android-37.1\google_apis_ps16k
```

So the system-image tooling is present as **installed images**; the *manager* tooling
(`sdkmanager`/`avdmanager`) is not.

## 2. Devices and emulators

```text
$ adb devices -l
List of devices attached
```

(section is empty — **no physical device and no running emulator** when the probe
started, and the emulator was killed before the probe ended; the final `adb devices -l`
after `adb emu kill` is again an empty list.)

Emulators: **exactly one AVD exists**, and it is a phone:

```text
$ E:\Programm\Android\SDK\emulator\emulator.exe -list-avds
Medium_Phone

$ dir /a /b E:\Programm\Android\avd
Medium_Phone.avd
Medium_Phone.ini
```

Its `config.ini` (excerpt):

```text
AvdId=Medium_Phone
avd.ini.displayname=Medium Phone
abi.type=x86_64
hw.device.name=medium_phone
hw.lcd.width=1080
hw.lcd.height=2400
hw.lcd.density=420
hw.ramSize=2048
hw.cpu.ncore=4
image.sysdir.1=system-images\android-37.1\google_apis_playstore_ps16k\x86_64\
target=android-37.1
PlayStore.enabled=true
fastboot.forceFastBoot=yes
```

**There is no tablet AVD.** A 7-inch (1080×1920) and a 10-inch (1440×2560) AVD would
have to be created, and the tool that normally does that (`avdmanager`) is absent.

## 3. Can an emulator be started here?

**Yes, it did start and boot.** HW acceleration is available:

```text
$ E:\Programm\Android\SDK\emulator\emulator-check.exe accel
accel:
0
WHPX(10.0.19045) is installed and usable.
accel
exit: 0

$ E:\Programm\Android\SDK\emulator\emulator-check.exe hyper-v
hyper-v:
2
Hyper-V is enabled
hyper-v
exit: 2
```

`systeminfo` agrees — `Hyper-V Requirements: A hypervisor has been detected.` So HAXM
is irrelevant here: the host uses **WHPX** (Windows Hypervisor Platform), and the
emulator log confirms it at runtime:

```text
WHPX on Windows 10.0.19045 detected.
Windows Hypervisor Platform accelerator is operational
```

Launch used (forced cold boot, headless):

```powershell
$env:ANDROID_AVD_HOME = "E:\Programm\Android\avd"
Start-Process -FilePath "E:\Programm\Android\SDK\emulator\emulator.exe" `
  -ArgumentList "-avd","Medium_Phone","-no-window","-no-audio","-no-boot-anim","-no-snapshot"
```

`START=2026-09-23T23:31:31.7013580+03:00`, `PID=21588`.

Outcome and timing:

```text
[+19 s]  adb devices -l
         emulator-5554          offline transport_id:1
[+138 s into an explicit poll loop]  getprop sys.boot_completed
         1
```

`adb wait-for-device`-style polling, at the end of which:

```text
$ adb devices -l
List of devices attached
emulator-5554          device product:sdk_gphone16k_x86_64 model:sdk_gphone16k_x86_64 device:emu64xa16k transport_id:1
```

Timing interpretation: the process started **23:31:31**; the adb transport appeared
within ~20 s but stayed `offline` for about the first minute; `sys.boot_completed=1`
was first observed 138 s into a poll loop that began at ≈23:33:35, i.e. roughly
**4 min 20 s from launch**. Treat it as **≈4–5 minutes for a cold boot on this host**.
Two caveats: `-no-snapshot` deliberately forced a cold boot (the AVD has
`fastboot.forceFastBoot=yes`, so a snapshot boot should be faster — not measured), and
another worker was running Gradle builds concurrently, so the machine was loaded.

Device properties once booted:

```text
$ adb -s emulator-5554 shell getprop ro.build.version.release
17
$ adb -s emulator-5554 shell getprop ro.build.version.sdk
37
$ adb -s emulator-5554 shell getprop ro.product.cpu.abi
x86_64
$ adb -s emulator-5554 shell wm size
Physical size: 1080x2400
$ adb -s emulator-5554 shell wm density
Physical density: 420
```

**Shutdown:** `adb -s emulator-5554 emu kill` → `OK: killing emulator, bye bye / OK`.
After that `adb devices -l` is empty and `Get-Process -Name "emulator*","qemu*"`
returns nothing. **No emulator was left running.**

## 4. Does the existing capture tooling work?

`scripts/media-capture.mjs` (read, not modified) requires:

- a slot resolvable under `apps/` whose basename is the slug or ends with `-<slug>`;
- `store/listing.json` with an `applicationId`;
- `store/journey.json` with at least 6 shots — **auto-written with placeholder
  coordinates if missing**;
- a Python interpreter (`python`/`py`) for the framing step;
- `adb` on `PATH` or under `$ANDROID_HOME/platform-tools`;
- **at least one device in `device` state** on `adb devices`.

It then runs `monkey -p <listing.applicationId> -c android.intent.category.LAUNCHER 1`,
executes each shot's steps (`tap`, `swipe`, `wait`, `text`, `back`), captures
`exec-out screencap -p` into `store/captures/raw/NN.png`, and finally shells out to
`python scripts/store-kit.py --slot <slug>` to frame the assets.

`scripts/store-kit.py` does the same capture via `--capture` and frames via
`render_screenshots`, which reads **one** raw set:

```text
679|    captures = store / "captures" / "raw"
680|    has_all_raw = all(load_raw_capture(captures, index) is not None for index in range(1, 7))
681|    for locale in locales:
...
687|            raw = load_raw_capture(captures, index)
688|            sources.append(raw or synthetic_screen((720, 1280), index, captions[index - 1], listing))
689|        for kind, (count, target) in SCREEN_TARGETS.items():
690|            for index in range(1, count + 1):
691|                out = frame_screen(sources[index - 1], target, captions[index - 1], listing)
```

with

```text
31|SCREEN_TARGETS = {
32|    "phone": (6, (1080, 1920)),
33|    "tablet-7": (4, (1080, 1920)),
34|    "tablet-10": (4, (1440, 2560)),
35|}
```

(The three targets became 9:16 on 2026-09-29, from the console's own aspect sentence; the
reason is in `AGENTS.md`. The measured blocks below are the 2026-09-23 records and keep the
sizes that were current when they were run.)

and `frame_screen` normalises whatever it is given with `cover()` (scale-to-cover +
centre crop). **Same 6 sources → phone, tablet-7 and tablet-10.**

### What was actually run

**(a) No device attached — the honest current-state run.** A sandbox copy of the
script against a sandbox slot with a minimal `listing.json`:

```text
PASS journey — wrote documented example ...\store\journey.json
BLOCKED: adb found no attached device in 'device' state
BLOCKED: start an emulator with:
  $ANDROID_HOME/emulator/emulator -list-avds
  $ANDROID_HOME/emulator/emulator -avd <AVD_NAME>
  adb wait-for-device
exit code: 2
```

(Note: at that moment `adb devices` showed `emulator-5554  offline`, i.e. the poll
loop was still booting; only `device` state counts.)

**(b) With the emulator booted and a real APK installed — full end-to-end run.**
`apps/2026-09-22-smoke/app/build/outputs/apk/debug/app-debug.apk` (an APK that already
existed on disk; **no Gradle was run**) was installed:

```text
$ adb -s emulator-5554 install -r ...\app-debug.apk
Performing Streamed Install
Success
install elapsed: 24s

$ adb -s emulator-5554 shell pm list packages | Select-String gexetr
package:com.gexetr.smoke.debug
```

Then, in the sandbox, with `listing.json`'s `applicationId` rewritten to
`com.gexetr.smoke.debug` (needed because of the `.debug` suffix):

```text
$ node scripts/media-capture.mjs --slot smoke
PASS journey — wrote documented example ...\store\journey.json
PASS capture — ...\store\captures\raw\01.png
PASS capture — ...\store\captures\raw\02.png
PASS capture — ...\store\captures\raw\03.png
PASS capture — ...\store\captures\raw\04.png
PASS capture — ...\store\captures\raw\05.png
PASS capture — ...\store\captures\raw\06.png
FAIL: missing ...\2026-09-22-smoke\app\build.gradle.kts
BLOCKED: store-kit framing failed with exit code 1
BLOCKED: start an emulator with: ...
exit code: 2   (elapsed 166 s)
```

The framing failure is a **sandbox artefact only**: the sandbox slot had no
`app/build.gradle.kts`, which `store-kit.py` validates. It also exposes a cosmetic
defect — `block()` prints "start an emulator" advice for *every* failure, including a
framing failure that has nothing to do with a device.

Once the sandbox slot was completed (slot files + `scripts/` + `capabilities/` copied),
the framing step rendered the full pack **from the six real captures**:

```text
PASS listing.json — schema=1 and Play character limits satisfied
PASS assets/en-US/phone-01.png — 1080x1920 RGB, 224441 bytes
... phone-02..06 ...
PASS assets/en-US/tablet-7-01.png — 1200x1920 RGB, 214632 bytes
... tablet-7-02..04 ...
PASS assets/en-US/tablet-10-01.png — 1600x2560 RGB, 290231 bytes
... tablet-10-02..04 ...
PASS assets/ru-RU/phone-01..06.png, tablet-7-01..04.png, tablet-10-01..04.png
STORE-KIT: OK apps/2026-09-22-smoke/store (icon=512x512 feature=1024x500 phone=6 tablet7=4 tablet10=4 locales=en-US,ru-RU)
exit: 0   (elapsed 33 s)
```

Captured raws were **real app pixels**, not a blank screen — the emulator was sitting
on the app's own activity, and the images are readable with broad tonal range:

```text
topResumedActivity=ActivityRecord{... u0 com.gexetr.smoke.debug/com.gexetr.smoke.MainActivity t92}

01.png (1080, 2400) RGBA mean= [58.9, 60.2, 66.9] extrema= ((2, 255), (2, 255), (5, 255))
02.png (1080, 2400) RGBA mean= [122.0, 120.3, 123.3] extrema= ((24, 255), (31, 255), (32, 255))
03.png (1080, 2400) RGBA mean= [122.0, 120.3, 123.3] extrema= ((24, 255), (31, 255), (32, 255))
04.png (1080, 2400) RGBA mean= [122.0, 120.3, 123.3] extrema= ((24, 255), (31, 255), (32, 255))
05.png (1080, 2400) RGBA mean= [122.0, 120.3, 123.3] extrema= ((24, 255), (31, 255), (32, 255))
06.png (1080, 2400) RGBA mean= [117.6, 117.0, 120.9] extrema= ((19, 255), (20, 255), (24, 255))
```

Note the dimensions: the emulator screen is **1080×2400**, not the store contract's
1080×1920 — `frame_screen` covers that by cropping. Shots 2–5 are byte-identical in
their statistics because the placeholder journey's taps hit nothing on this app, which
is exactly what gap 4 above means in practice.

`graphics.md` flipped to the truthful value once real captures were present:

```text
Screenshot source status: **framed from real device captures**.
```

### The resolution lever that does work (measured)

`wm size` gives the exact contract dimensions on a real device surface, and it is real
rendering, not resampling. All three were captured and read back:

```text
$ adb -s emulator-5554 shell wm size
Physical size: 1080x2400
Override size: 1080x1920
$ adb -s emulator-5554 shell screencap -p /sdcard/phone-1080x1920.png && adb pull ...
screencap size: (1080, 1920) RGBA mean= [120.4, 120.0, 124.6]

1200x1920 -> screencap (1200, 1920) RGBA
1600x2560 -> screencap (1600, 2560) RGBA
```

`wm size reset` restored `Physical size: 1080x2400` (verified). Two honest caveats:
the AVD's density stays 420 dpi, so a 1600×2560 capture is a **phone-density layout at
tablet pixel dimensions** — pixel-correct, layout-phone; and `wm size` is a debug
override on an emulator, not something a physical device would do.

**Windows-specific trap, verified:** `adb exec-out screencap -p` piped through a
PowerShell `>` redirect corrupts the PNG (PIL: `UnidentifiedImageError`). The
Node-based `media-capture.mjs` uses `spawnSync(..., {encoding: null})` and its PNGs
were valid, so the script path is fine — but any manual capture must use
`adb shell screencap -p <remote>` + `adb pull`, as above.

### Concrete gaps between what the tooling needs and what this machine has

| # | Gap | Evidence | Size of fix |
|---|---|---|---|
| 1 | One raw capture set feeds all three sizes; tablet assets are crops of phone captures | `store-kit.py:679-692` | Medium: per-size capture pass (3 raw dirs) + `render_screenshots` reading the matching set |
| 2 | One capture pass feeds both locales; `ru-RU` shows English UI | PIL diff bbox `(83,116,872,168)` between `en-US/phone-01.png` and `ru-RU/phone-01.png` | Medium: set device locale, re-run journey, keep two raw sets |
| 3 | Script launches `listing.applicationId`; debug APK installs as `...<slug>.debug` | `package:com.gexetr.smoke.debug` vs `"applicationId": "com.gexetr.smoke"` | Small: a `--package` override or install the release APK |
| 4 | No slot has `store/journey.json`; placeholder journey has hard-coded coordinates | `Get-ChildItem apps -Recurse -Filter journey.json` → nothing; `media-capture.mjs:90-122` | Small per slot: author real coordinates before capturing |
| 5 | No tablet AVD, and no `avdmanager`/`sdkmanager` to create one | `emulator -list-avds` → `Medium_Phone`; `Test-Path cmdline-tools` → `False` | Small download: `cmdline-tools;latest` (order of ~130 MB — **approximate, not measured here**), then `avdmanager create avd`. Workaround already proven: one phone AVD + `wm size` |
| 6 | `block()` prints emulator advice for non-device failures | observed on the sandbox framing failure | Cosmetic, small |
| 7 | No `ANDROID_EMULATOR_HOME`/`PATH` entry, so `emulator`/`sdkmanager` need full paths | see §1 | Small: export paths, or use absolute paths in the runbook (chosen) |

Nothing in gaps 1–4 was changed here; the task forbade touching `scripts/**`.

## 5. Runbook — from a built debug APK to the three screenshot sets

Contract sizes and counts (`AGENTS.md`, `scripts/store-kit.py`), per locale,
for both `en-US` and `ru-RU`. The three sets became 9:16 on 2026-09-29 — the aspect the
console states in every screenshot field; the sizes below are current, and the
`1200×1920` / `1600×2560` figures elsewhere in this 2026-09-23 report are the sizes that
were measured at the time:

| Set | Count | Size (W×H) | File names |
|---|---:|---|---|
| Phone | 6 | 1080×1920 | `store/assets/<locale>/phone-01.png` … `phone-06.png` |
| 7-inch | 4 | 1080×1920 | `store/assets/<locale>/tablet-7-01.png` … `tablet-7-04.png` |
| 10-inch | 4 | 1440×2560 | `store/assets/<locale>/tablet-10-01.png` … `tablet-10-04.png` |

### Step 0 — machine setup (once)

```powershell
# adb is on PATH; the emulator is not, so use the absolute path.
$env:ANDROID_AVD_HOME = "E:\Programm\Android\avd"
$EMU = "E:\Programm\Android\SDK\emulator\emulator.exe"

$EMU -list-avds            # expect: Medium_Phone
& "E:\Programm\Android\SDK\emulator\emulator-check.exe" accel   # expect: WHPX ... usable
```

### Step 1 — build the APK (other worker / normal pipeline)

```powershell
cd <slot>
.\gradlew assembleDebug      # do not run this concurrently with another Gradle build
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.
Debug `applicationId` is `com.gexetr.<slug>.debug`; the release APK's is
`com.gexetr.<slug>`. **This matters in step 5.**

### Step 2 — start the emulator (agent-scriptable, ~4–5 min cold boot)

```powershell
$env:ANDROID_AVD_HOME = "E:\Programm\Android\avd"
Start-Process -FilePath "E:\Programm\Android\SDK\emulator\emulator.exe" `
  -ArgumentList "-avd","Medium_Phone","-no-window","-no-audio","-no-boot-anim" `
  -RedirectStandardOutput "$env:TEMP\emu.out.log" -RedirectStandardError "$env:TEMP\emu.err.log"

# Wait: transport first, then boot completed. Do not gate on adb alone.
adb wait-for-device
do { Start-Sleep -Seconds 10 } while ((adb shell getprop sys.boot_completed).Trim() -ne "1")
adb devices            # expect: emulator-5554   device
```

Omit `-no-snapshot` to try the AVD's snapshot for a faster boot (not measured here).

### Step 3 — install and launch the app (scriptable)

```powershell
adb install -r "apps\<slot>\app\build\outputs\apk\debug\app-debug.apk"
adb shell monkey -p com.gexetr.<slug>.debug -c android.intent.category.LAUNCHER 1
# Confirm the app, not the launcher, is on screen:
adb shell dumpsys activity activities | Select-String topResumedActivity
```

### Step 4 — author the journey (**needs human/agent judgement**)

Create `apps\<slot>\store\journey.json` with **6 shots**, each `name`/`title`/`body`
and the `steps` that reach the screen you want (`tap`, `swipe`, `wait`, `text`,
`back`). If this file is missing, `media-capture.mjs` writes a placeholder whose taps
were measured to hit nothing on the smoke app — so author it, do not accept the stub.
This is the only step that genuinely needs human eyes on the app UI.

### Step 5 — capture the phone set (scriptable)

```powershell
adb shell wm size 1080x1920            # exact contract size on a real surface
node scripts/media-capture.mjs --slot <slug>
# -> store/captures/raw/01.png … 06.png (1080x1920)
# -> store/assets/{en-US,ru-RU}/phone-01..06.png, tablet-7-*.*, tablet-10-*.*
adb shell wm size reset
```

Caveat to keep visible: on a debug install, `running media-capture.mjs` will need
`listing.json`'s `applicationId` to be the installed one (`...debug`) or the release
APK installed instead — see gap 3. Until `scripts/**` changes, install the **release**
APK for the capture run, or expect `monkey` to find no launchable activity.

### Step 6 — real 7-inch and 10-inch sets (**partly manual today**)

Verified to work at the capture level, but `store-kit.py` cannot frame them yet
(gap 1), so this is the one part of the runbook that is **not fully scripted**:

```powershell
# 7-inch: 4 shots at 1080x1920 (9:16, the console's stated aspect)
adb shell wm size 1080x1920
# run the same 6-step journey, save the raw PNGs to a size-specific folder
#   adb shell screencap -p /sdcard/s.png   (NOT `adb exec-out ... > file` on PowerShell)
#   adb pull /sdcard/s.png  store/captures/raw-tablet7/NN.png
# 10-inch: 4 shots at 1440x2560 (9:16)
adb shell wm size 1440x2560
# ... same, into store/captures/raw-tablet10/
adb shell wm size reset
```

Then either frame them by hand at 1080×1920 / 1440×2560, or (preferred, and the fix
this report recommends) extend `render_screenshots` to read a per-size raw set.

### Step 7 — localized screenshots (**partly manual today**)

```powershell
# set device language to Russian, relaunch, re-run the journey into a second raw set
adb shell "am start -a android.settings.LOCALE_SETTINGS"   # or setprop on a rooted image
```

Confirmed defect if skipped: `en-US` and `ru-RU` assets differ **only** in the caption
band, so `ru-RU` ships English UI screenshots with Russian captions. Fixing this
properly needs a per-locale raw set (gap 2).

### Step 8 — validate the pack (scriptable)

```powershell
python scripts/store-kit.py --slot <slug>
node scripts/verify-slot.mjs --slot <slug> --strict-store
```

`store-kit.py` prints a `PASS` line per asset with dimensions, and `graphics.md` must
read `framed from real device captures` — if it still says `synthetic`, the raw set was
not found and the pack is fabricated again.

### Step 9 — stop the emulator (always)

```powershell
adb emu kill
adb devices        # must be empty
```

### Human vs scripted

| Step | Owner |
|---|---|
| 0–1 machine setup, APK build | Scripted (Gradle must not run concurrently with another build) |
| 2 emulator boot | Scripted, but slow (~4–5 min) and it competes with builds for CPU |
| 3 install/launch | Scripted |
| 4 authoring `journey.json` | **Human/agent judgement** — the only content-correctness step |
| 5 phone capture + framing | Scripted (see the debug-vs-release package caveat) |
| 6 tablet sizes | Capture verified manual; framing needs a scripts change |
| 7 localized UI | Needs a manual locale switch; framing needs a scripts change |
| 8 validation | Scripted |
| 9 shutdown | Scripted |
| Play Console login, upload, publish | **Human** — and *not needed for capture at all*; it is only required later, for the store listing upload |

---

## Everything marked UNVERIFIED

- **The full 6-phone-set capture path with the repo's real `scripts/media-capture.mjs`
  against a real repo slot.** It was proven with a *sandbox copy* of the script and a
  copy of the `2026-09-22-smoke` slot under `%TEMP%`, because the real path would have
  written `store/journey.json` and captures into `apps/**`, which this task forbade.
  The script itself was copied byte-for-byte and not modified.
- **Snapshot (fast) boot.** Every measurement above used `-no-snapshot` (cold boot).
  The AVD has `fastboot.forceFastBoot=yes`, but snapshot boot was not attempted.
- **Whether `monkey -p com.gexetr.<slug>` actually aborts on a debug-only install.**
  The package-name mismatch was measured (`package:com.gexetr.smoke.debug` vs
  `"applicationId": "com.gexetr.smoke"`); the capture run was made to work by
  rewriting `applicationId` in the sandbox. `monkey`'s failure mode was not observed.
- **`cmdline-tools;latest` download size** (~130 MB) — approximate, not measured; it
  was **not** installed, because a large download should be a human decision.
- **Creating tablet AVDs without `avdmanager`** (hand-written `.ini`/`config.ini`) —
  not attempted; the `wm size` route was used instead.
- **Android Studio.** `%APPDATA%\Google` contains `AndroidStudio2024.3` and
  `AndroidStudio2026.1.3` settings directories, but `studio64.exe` was **not found** at
  `C:\Program Files\Android\Android Studio\bin\`, `%LOCALAPPDATA%\Programs\Android Studio\bin\`,
  or a JetBrains Toolbox path, and a bounded search under `C:\Program Files` and
  `%LOCALAPPDATA%\Programs` found nothing. Whether the GUI Device Manager is available
  on this machine is **unknown**.
- **`emulator-check hyper-v` exit code 2** — reported as "Hyper-V is enabled" while
  `accel` reported WHPX usable. Both were run; the discrepancy was not investigated.
- **`-gpu` / rendering-backend choice** — the default (`gfxstream`) was used and worked;
  `swiftshader_indirect` was never tried.
- **Physical-device capture** — no USB device was ever attached; this report is
  emulator-only.
- **Play acceptance of `wm size`-derived tablet screenshots** — dimensions match the
  contract and the pixels are real, but Play's large-screen quality assessment is a
  human/policy judgement, not something measured here.

## Files touched

- Created: `docs/research/2026-09-23-device-capture-readiness.md` (this file).
- Nothing else in the repository was created or modified. All probing used temporary
  directories under `%TEMP%` (`pk-capture-sandbox`, `playkiln-emu`).
- The emulator was killed; `adb devices` is empty and no `emulator`/`qemu` process
  remains.
