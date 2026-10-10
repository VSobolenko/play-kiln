# G — Android build + device proof: Orekit 13.1.5 + Hipparchus 4.0.3 under R8 full mode

**Closes the gap:** `.kiln/scratch/positioning-2026-10-08/F-engine-gap-map.md` §7 risk #4, whose claim ledger
ended with the row `Orekit + Hipparchus build, shrink and run correctly on the target Android configuration |
UNVERIFIED | No Android/Gradle/D8/R8 build was run`.

**Verdict**

```
ANDROID_BUILD=PASS
SIZE_MB=4.18
```

The row is now **MEASURED PASS**. Orekit 13.1.5 + Hipparchus 4.0.3 compile, shrink under R8 full mode with
**zero Orekit-specific keep rules**, install and run on the factory's test phone, producing numerics that are
**byte-identical to the JVM reference run already on disk**.

| Artifact | Value |
|---|---|
| APK on disk | `4,379,153` bytes = **4.176 MiB** = 4.379 MB (decimal) |
| APK sha256 | `39FAE7995EDD1952AABD8EB71BC0CA6E07D0D276ECF2D19AE797AD2E50BFE25A` |
| `classes.dex` | **1,753,644** uncompressed / **900,383** compressed — **one** dex, no multidex |
| Compressed payload | 4,307,526 bytes across 173 entries |
| Device | serial `R58MB3SGZVL`, model `SM-A405FM` (Galaxy A40), Android 11 |
| Device verdict | `OREKITSMOKE_OK`, exit clean, **zero** linkage failures |

---

## 1. Route taken — why no slot was created

**Route: standalone throwaway Gradle project at `.kiln/scratch/android-orekit-smoke/`, built through
`scripts/lib/gradle.mjs`.** `scripts/new-slot.mjs` was **not** used and no slot was created.

The reason is mechanical, not a preference. `new-slot.mjs` refuses to scaffold without four validated
contracts — `--economics`, `--differentiators`, `--naming` and an OpenSpec change (`AGENTS.md` stage 2,
`.cursor/rules/spec-first.mdc`). None exists for "does Orekit work on Android", because that is not a product
idea: it is a packaging measurement with no revenue plan, no promise list, no name and no spec. Writing four
fake contracts to unblock a build would have been the exact defect `spec-first.mdc` was written from — a slot
directory that exists outside the specification the owner requires. So the measurement lives in scratch, and
the Gradle invocation still goes through the factory's own single entry point so the build honours the same
three contracts a slot build does.

`tools/build.mjs` calls `runGradleTasks(projectDir, tasks)` from `scripts/lib/gradle.mjs`, which is what gives
this build:

- the **JDK 17 pin** resolved by `scripts/lib/java.mjs` (`GRADLE_JAVA_HOME=C:\Program Files\Java\jdk-17 major=17 source=JAVA_HOME`) — `.cursor/rules/build-speed.mdc`;
- **`TEMP`/`TMP`/`TMPDIR` redirected into `.kiln/scratch/gradle/`** by `scripts/lib/scratch.mjs`, so the build wrote nothing to the machine's system temp directory — `.cursor/rules/workspace-hygiene.mdc`. Confirmed: `rg --no-ignore` over the scratch tree found the Kotlin daemon log under
  `.kiln/scratch/gradle/kotlin-daemon.2026-10-08.21-00-27-543.00.log.lck`, not under `%TEMP%`;
- the per-slot lease (`scripts/lib/slot-lock.mjs`).

**Toolchain pins** (`app/build.gradle.kts`, mirroring `AGENTS.md` § "Required toolchain"): `compileSdk = 37`,
`buildToolsVersion = "36.0.0"`, `minSdk = 26`, `targetSdk = 36`, Kotlin 2.3.21 + Compose compiler plugin,
AGP 9.4.1, Gradle 9.7.1, `sourceCompatibility`/`targetCompatibility = JavaVersion.VERSION_17`. The merged
release manifest confirms `android:minSdkVersion="26"` and `android:targetSdkVersion="36"`.

**Release build config — the half that matters:**

```kotlin
release {
    isMinifyEnabled = true
    isShrinkResources = true
    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
}
```

`app/proguard-rules.pro` is **comments only** — it contains no `-keep` directive at all. That file is the
measurement, exactly as its own comment says:

> `# Deliberately starts EMPTY apart from the AndroidX/Material3 defaults.`
> `# This is the measurement: does Orekit 13.1.5 + Hipparchus 4.0.3 survive R8 full mode`
> `# with no Orekit-specific keep rule? A keep rule is only added after a reproduced failure.`

R8 full mode is confirmed from the generated `build/outputs/mapping/release/configuration.txt`, which carries
`-allowaccessmodification` and the full-mode annotation-removal notes.

The release APK is signed with the debug key *because no factory keystore exists for a scratch project* — it is
installable evidence, not a Play upload, and no signing key was generated (`authorization.mdc`).

---

## 2. What the app actually does — the shortest path over the whole chain

Not a hello-world. `OrekitChain.kt` exercises, in order, on the device:

1. **Asset extraction + runtime data discovery.** `assets/orekit-data/tai-utc.dat` (3,321 bytes, the same file
   the JVM proof used) is copied out of the APK into `context.filesDir/orekit-data/`, and registered with
   `DataContext.getDefault().getDataProvidersManager().addProvider(new DirectoryCrawler(dir))`. This is the
   question "how does Orekit find its data on Android, and does that work" — it does, via the filesystem,
   because the asset is not readable in place.
2. **TLE parse** of satellite `06251` (Vallado's canonical case) → epoch
   `2006-06-25T19:46:43.980096Z`.
3. **Propagation to T+600 s** via `TLEPropagator.selectExtrapolator(tle)`, reading a TEME `PVCoordinates`.
4. **Frame transform TEME → ITRF** with `FramesFactory.getITRF(IERS_2010, true)` (`simpleEOP = true`), which is
   the path that pulls the nutation/precession machinery.
5. **Topocentric ground station** `TopocentricFrame` at Moscow (55.7558 N, 37.6173 E, 150 m) → elevation and
   azimuth.
6. **Segmented event detection** over one day at a 10° elevation mask → AOS/LOS, 24 segments.

That covers class loading, resource loading, the data-provider machinery, the frame/nutation stack and the
numerics — the whole chain, in one screen.

---

## 3. Build command and result

```
D:\Python\PlayKiln\.kiln\scratch\android-orekit-smoke\gradlew.bat assembleDebug assembleRelease --console=plain
```

Invoked through `node tools/build.mjs` → `runGradleTasks()`.

| | attempt 1 | attempt 2 |
|---|---|---|
| Exit code | **1** (FAILED) | **0** (`BUILD SUCCESSFUL in 3m 45s`) |
| Elapsed (`GRADLE_ELAPSED_MS`) | 271,362 ms | **226,809 ms** |
| Cause / tasks | `compileReleaseKotlin` + `compileDebugKotlin FAILED` — a `logger.getLoggedEvents().size()` Kotlin error in my own smoke code | 86 actionable tasks: 24 executed, 62 up-to-date; configuration cache **reused** |
| JDK | `C:\Program Files\Java\jdk-17` major=17 source=`JAVA_HOME` | same |

Total build wall clock across both attempts: **498,171 ms ≈ 8 min 18 s** (one failed compile pass, then the
fix and the real build).

**The one R8 warning, reported rather than buried:**

```
WARNING: ...\merged_java_res\release\mergeReleaseJavaResource\base.jar:
R8: Unexpected reference to missing service class: META-INF/services/com.sun.source.util.Plugin.
```

This is precisely the `ServiceLoader` hazard the JVM proof's constant-pool scan flagged
(`DefaultDataContextPlugin` naming `com.sun.source.util.Plugin`). **It is a warning, not an error**, it did not
fail the build, and the app ran to completion on the device. It is a dangling service descriptor for an
annotation-processor plugin Orekit ships but never needs at runtime. It costs nothing and needs **no keep
rule** — but it is real, it is now measured on Android (not inferred from a class-file scan), and a future
release that starts `ServiceLoader`-ing that path would surface it.

---

## 4. R8 outcome — no keep rule was needed

Measured from `build/outputs/mapping/release/mapping.txt` (19,222,501 bytes, 159,144 lines):

| Package | Classes listed | **Surviving** | Removed by R8 |
|---|---:|---:|---:|
| `org.orekit.*` | 249 | **220** | 29 |
| `org.hipparchus.*` | 59 | **49** | 10 |
| `com.gexetr.*` (app) | 2 | 2 | 0 |

R8 retained exactly the Orekit/Hipparchus classes the call path reaches — `org.orekit.data.DirectoryCrawler`,
`DataProvider`, `DataLoader`, `DataSource$StreamOpener`, `AbstractSelfFeedingLoader`,
`FundamentalNutationArguments`, `bodies.OneAxisEllipsoid`, `bodies.GeodeticPoint`,
`attitudes.Attitude` — and deleted the rest, **with no `-keep` rule and no `-keepattributes`**. Reflection and
`ServiceLoader` use in Orekit/Hipparchus did **not** produce an R8 failure on this call path. This directly
answers the F-map's open question about keep rules: **none are required for this chain.**

---

## 5. Size — the real finding, and it is not the dex

The number the F-map expected to matter (DEX size after shrinking) turns out to be the **small** half.

| Component | Uncompressed | Compressed | Share of compressed APK payload |
|---|---:|---:|---:|
| `classes.dex` (app + Compose + Orekit + Hipparchus code) | 1,753,644 | 900,383 | **20.9 %** |
| `assets/org/orekit/**` — **88 entries** (ITU-R-P.834 tables, IERS data) | 10,187,109 | 2,558,110 | 59.4 % |
| `assets/org/hipparchus/**` — **7 entries** (`new-joe-kuo-6.21201`, 1,887,612 B) | 1,911,942 | 702,604 | 16.3 % |
| — the two together | 12,099,051 | **3,260,714** | **75.7 %** |
| `assets/orekit-data/tai-utc.dat` (the data the app actually loads) | 3,321 | 511 | 0.01 % |
| Total payload (173 entries) | 14,080,761 | 4,307,526 | 100 % |

Two things follow, and both are actionable:

- **R8 shrinks the code well and cannot touch this.** `isShrinkResources = true` removes unused *Android*
  resources; jar-embedded resources are merged into `assets/` and are invisible to it. So **75.7 % of the
  shipped payload is Orekit/Hipparchus data files that R8 will never remove**, while the entire shrunk dex —
  app, Compose and both libraries — is 900 kB compressed.
- **Most of that baggage is irrelevant to an SGP4 job.** `ITU-R-P.834/*` is the ITU atmospheric-attenuation
  model (≈2.56 MB compressed across 88 files) and `hipparchus/random/new-joe-kuo-6.21201` is a 1.89 MB
  random-generator table. An SGP4 + frames core has no reason to load either. Excluding them is a
  `packaging { resources { excludes += … } }` entry — **cheap, not fundamental**, but it is a change that must
  be followed by re-running this proof, because the files are loaded lazily and their absence only shows when
  a code path asks for them. That follow-up has not been run.

For the record, the comparison the F-map could not make: the **unshrunk debug** APK of the identical project
is **18,545,325 bytes (17.686 MiB)** against the release APK's 4,379,153 bytes — R8 removed **76.4 %** of the
artifact. A debug build would have proved nothing about the shrinker, which is why the release build is the one
measured.

**Honest limit on the size claim:** this is a *smoke* APK, not a product APK. It carries the Compose baseline
and nothing else — no icons, no launcher vectors, no store assets, no Real app UI. The **Orekit-attributable
delta is not isolated by a baseline build**: I did not build the same app with the jars removed, so
"Orekit costs X MB" is not measured here. What *is* measured is the full inventory above, from which the
Orekit/Hipparchus share is exact. For scale, the factory's shipped release APKs run 1.1–4.0 MB
(`.kiln/scratch/positioning-2026-10-08/F-engine-gap-map.md`), so this smoke APK already sits **above** the top
of that range, on assets rather than code.

---

## 6. Device run — the reading, not a claim

Every command carried `-s R58MB3SGZVL`.

**Precondition, checked first:** `adb -s R58MB3SGZVL shell getprop ro.product.model` → **`SM-A405FM`** ✅
(`ro.build.version.release` → `11`). This is the factory's designated test phone
(`.cursor/rules/device-testing.mdc`), not the Galaxy A16 that must not be driven without the owner's word.

**Install:** `adb -s R58MB3SGZVL install -r app-release.apk` → `Performing Streamed Install / Success`,
exit 0 (11,504 ms).

**Launch:** `adb -s R58MB3SGZVL shell monkey -p com.gexetr.orekitsmoke -c android.intent.category.LAUNCHER 1`
→ `Events injected: 1`.

**Logcat, verbatim** (app pid 29731, tag `OrekitSmoke`; captured twice, ~6 s apart, **identical both times** —
the Compose recomposition re-ran the chain and produced the same bits):

```
I OrekitSmoke: DATA_DIR=/data/user/0/com.gexetr.orekitsmoke/files/orekit-data
I OrekitSmoke: DATA_BYTES=3321
I OrekitSmoke: PROVIDER=DirectoryCrawler
I OrekitSmoke: TLE_EPOCH_ISO=2006-06-25T19:46:43.980096Z
I OrekitSmoke: PROPAGATOR=selectExtrapolator ok
I OrekitSmoke: TEME_AT_T+600s pos_km=(1280.749619,5592.447086,3605.982869) vel_kms=(-5.388644115,-2.061975140,5.053811958)
I OrekitSmoke: ITRF_OK simpleEOP=true name=CIO/2010-based ITRF simple EOP
I OrekitSmoke: TOPO_AT_T+600s elevation_deg=-44.136279 azimuth_deg=354.524835
I OrekitSmoke: EVENT AOS iso=2006-06-26T09:53:47.897267186936432736Z elevation_deg=10.000000
I OrekitSmoke: EVENT LOS iso=2006-06-26T09:59:48.027921281203328768Z elevation_deg=10.000000
I OrekitSmoke: EVENT AOS iso=2006-06-26T11:29:18.304781434194478272Z elevation_deg=10.000000
I OrekitSmoke: EVENT LOS iso=2006-06-26T11:35:37.878154921931607312Z elevation_deg=10.000000
I OrekitSmoke: EVENT AOS iso=2006-06-26T13:05:20.97958659463764911Z elevation_deg=10.000000
I OrekitSmoke: EVENT LOS iso=2006-06-26T13:11:34.356202888963839478Z elevation_deg=10.000000
I OrekitSmoke: EVENT AOS iso=2006-06-26T14:41:11.385781447084213248Z elevation_deg=10.000000
I OrekitSmoke: EVENT LOS iso=2006-06-26T14:47:21.951915097744294424Z elevation_deg=10.000000
I OrekitSmoke: EVENT AOS iso=2006-06-26T16:17:52.146195545079537952Z elevation_deg=10.000000
I OrekitSmoke: EVENT LOS iso=2006-06-26T16:21:08.397038821904156032Z elevation_deg=10.000000
I OrekitSmoke: SEGMENTS=24 EVENTS=10 AOS_1D_10DEG=5
I OrekitSmoke: OREKITSMOKE_OK
```

**Cross-check against the JVM reference — the same bits, not the same shape.** The device printed
`elevation_deg=-44.136279 azimuth_deg=354.524835` and
`pos_km=(1280.749619,5592.447086,3605.982869) vel_kms=(-5.388644115,-2.061975140,5.053811958)`. The prior
JVM run already on disk, `.kiln/scratch/positioning-2026-10-08/run-tinydata.txt` lines 3 and 5, reads
**character-for-character identical** values. Same for the F-map's own copy of that run
(`F-engine-gap-map.md:281,283`). Verified by exact, case-sensitive string equality between the two sets of
lines, not by eye. So this is not "an Android run produced a plausible number" — it is the **same number, to
every printed digit**, from a different toolchain (ART vs HotSpot) and a different architecture (arm64 vs
x86-64).

**The event grid is NOT reproduced, and the difference is deliberate.** The JVM probe reports
`PASSES_3D_10DEG=1 EVENTS=2` with `PROBE_EXIT_OK`; the device reports `SEGMENTS=24 EVENTS=10
AOS_1D_10DEG=5` with `OREKITSMOKE_OK`. That is not a disagreement about physics — the two programs run
**different propagation windows on purpose**, and reading both sources settles it:

| | JVM probe | Device smoke |
|---|---|---|
| Window | **3 days**, one unsegmented `propagate()` call (`PassProbe.java:90`) | **1 day**, walked in 24 × 1-hour windows (`OrekitChain.kt:110-118`) |
| Detector | `ElevationDetector(60.0, 1.0e-6, topo).withConstantElevation(10°)` | **identical** (`PassProbe.java:88-89` / `OrekitChain.kt:105-107`) |
| Events found | 2 (1 AOS) | 10 (5 AOS) |

The device smoke segments precisely *because* the F-map measured that one multi-day `propagate()` call finds
1 AOS where ~15 exist — its own comment says so (`OrekitChain.kt:102-103`). So the device's 5 AOS in one day
for a 15.56 rev/day LEO is the **segmented** result, and the JVM probe's 1 AOS is the **unsegmented** result
the F-map recorded as a defect. The first AOS agrees to 18 decimal places across both
(`…09:53:47.897267186936432736Z`); the first LOS agrees to ~9 significant decimals and then diverges in the
deep fractional digits (`…281078647898Z` vs `…281203328768Z`), which is consistent with root-finding against a
differently-segmented state history rather than with a numeric disagreement — but the **cause of that specific
divergence is `INFERRED`, not measured**, because the two programs do not report a common event-count to
compare. The load-bearing cross-check is the TEME/TOPO pair, and that is exact.

**Error scan across the full 3,908-line logcat.** Searched for `AndroidRuntime.*FATAL`, `FATAL EXCEPTION`,
`NoClassDefFoundError`, `NoSuchMethodError`, `VerifyError`, `LinkageError`, and every ` E ` line from the app's
pid. **Zero matches.** The single `E` line attributed to the process is:

```
10-08 21:06:10.739 29731 29731 E etr.orekitsmok: Not starting debugger since process cannot load the jdwp agent.
```

— which is the expected message for a *non-debuggable release* APK, and is itself corroboration that this is a
release build and not a debug one. `android:debuggable` is absent from the merged release manifest. The only
runtime warnings on the app's threads are Samsung `Gralloc3/4: mapper … is not supported` and Compose
`Choreographer: Skipped N frames!` during first frame — neither is Orekit-related.

**On-screen evidence.** `uiautomator dump` produced a `TextView` whose `text` attribute contains **the same
chain output, string-for-string**, under the heading `Orekit on Android` (see
`.kiln/scratch/android-orekit-smoke/ui-dump.xml`) — i.e. the numbers are rendered in the Compose UI, not only
in the log. `adb exec-out screencap` → `screen.png`, 340,289 bytes, magic `89 50 4e 47 0d 0a 1a 0a` (a real
PNG). Screenshot was taken via device-side `screencap` + `adb pull`, never a text pipeline, per
`device-testing.mdc`'s Windows trap.

**Elapsed for the device phase:** install 11.5 s, launch→logcat capture 26.6 s, evidence capture (screenshot +
UI dump + error scan) 7.6 s — **≈ 45.7 s total**.

---

## 7. Vallado/CSSI Java subset — fallback status

**Location:** `.kiln/scratch/nv-core-proof/data/vallado-java-src/` — `SGP4unit.java` (95,061 B / 2,442 lines),
`SGP4utils.java` (37,862 B / 1,109 lines), `SGP4SatData.java` (2,141 B / 50 lines) = **135,064 bytes (~132 KiB),
3,601 lines** — the figure the F-map quoted is confirmed on disk.

**Compilation: DONE, artifacts on disk.** `.kiln/scratch/nv-core-proof/out/vallado/` holds five class files —
`SGP4unit.class` (26,307 B), `SGP4utils.class` (10,455 B), `SGP4unit$Gravconsttype.class` (1,095 B),
`SGP4SatData.class` (2,225 B), `SGP4utils$MDHMS.class` (446 B) = **40,528 bytes** of bytecode. Plain `javac`,
no third-party dependency, no `java.beans`, no AWT, no XML — it compiles and the output is on disk.

**Corpus results: NOT on disk — they would need re-running.** The only corpus output in that tree is
`orekit-corpus-output.txt`, which is the **Orekit** run. There is no Vallado verification output anywhere: a
search for `vallado` across `.kiln/scratch/nv-core-proof/` returns the sources, the C++ reference tree
(`data/vallado-cpp/`, including `TestSGP4/`), the `.class` files and `RESULT.md` — but no run log. `RESULT.md`
claims only `VALLADO_JAVAC=PASS`, i.e. compilation, never execution against `SGP4-VER.TLE` / `TCPPVER.OUT`.
Compiling the *reference implementation of the corpus's own source* is also a weaker result than it looks: the
2006 AIAA corpus exists to validate SGP4, and the CelesTrak Java port is a direct translation of the same
Vallado C++ code the corpus was built around, so re-running the corpus on it is a self-consistency check more
than an independent one. **Status: a viable fallback on packaging and dependency grounds, UNVERIFIED on
numerical grounds — and now less necessary, because Orekit itself is a measured PASS.**

**Licence: UNVERIFIED, unchanged.** `RESULT.md` records no standalone license file in the inspected archive
entries. Vendoring it into a shipped slot needs independent confirmation of shipping rights. This is the one
part of the fallback that is a real blocker rather than a work item.

---

## 8. What this changes in the F-map's risk #4

The F-map's closing `UNVERIFIED` row —

> `Orekit + Hipparchus build, shrink and run correctly on the target Android configuration | UNVERIFIED | No Android/Gradle/D8/R8 build was run`

— is now **MEASURED PASS**, and three of the specific hazards it named are settled by measurement rather than
by a class-file scan:

| F-map hazard | Verdict now |
|---|---|
| `java.beans` / AWT absent | Confirmed absent **and** irrelevant — the app built and ran; nothing linked them |
| `javax.xml` references untested in-app | **Not exercised by this chain** — no XML path was hit. Still open for any core that parses `tai-utc.dat` via an XML loader, e.g. `JPLEphemeridesLoader` or EOP `XML` files |
| reflection / `ServiceLoader` vs R8 | **Survived with zero keep rules.** One genuine R8 `WARNING` about `META-INF/services/com.sun.source.util.Plugin`, non-fatal |
| R8 keep rules | **None needed** for TLE → TEME → ITRF → topocentric → events |
| runtime data files | **Solved and measured**: asset extracted to `filesDir`, `DirectoryCrawler` registered, `DATA_BYTES=3321`, `PROVIDER=DirectoryCrawler` |
| final APK / DEX size | **4.18 MiB APK, 900,383 B compressed dex** — and the surprise is that 75.7 % of the payload is Orekit/Hipparchus *assets*, not code |

**The F-map's recommendation should be revisited, not silently kept.** It concluded
`OREKIT_RISKY_USE_VALLADO_SUBSET` on a *risk* basis — "the smaller subset avoids unnecessary packaging and
shrinker surface" — while explicitly stating "no Android build was run". The Android build has now been run
and it passed. The packaging concern is real but it is **asset weight, not a build or runtime failure**, and it
is addressable with an `excludes` entry plus a re-run. The decision the owner now faces is a product one:
11.6 MB of jars and a 4.18 MiB smoke APK against a 132 KiB fallback whose licence is unconfirmed and whose
numbers are unproven — versus 220 live Orekit classes that are proven to run and produce JVM-identical output.

---

## 9. Claim ledger

| LABEL | CLAIM | EVIDENCE |
|---|---|---|
| MEASURED | Standalone throwaway Gradle project, no slot created, built through `scripts/lib/gradle.mjs` | `.kiln/scratch/android-orekit-smoke/tools/build.mjs:20` (`runGradleTasks`); no `economics.json`/`differentiators.json`/`naming.json`/OpenSpec change exists for it |
| MEASURED | JDK 17 pinned; system temp redirected into `.kiln/scratch/gradle/` | `logs/gradle-build2.log`: `GRADLE_JAVA_HOME=C:\Program Files\Java\jdk-17 major=17 source=JAVA_HOME`; `rg --no-ignore` found `kotlin-daemon.…log.lck` under `.kiln/scratch/gradle/` |
| MEASURED | Toolchain pins: compileSdk 37, buildTools 36.0.0, minSdk 26, targetSdk 36, JVM 17 | `app/build.gradle.kts`; merged release manifest `android:minSdkVersion="26"`, `android:targetSdkVersion="36"` |
| MEASURED | Release build ran with `isMinifyEnabled=true`, `isShrinkResources=true`, R8 full mode | `app/build.gradle.kts` buildTypes.release; task `:app:minifyReleaseWithR8` in `logs/gradle-build2.log`; `-allowaccessmodification` + full-mode notes in `build/outputs/mapping/release/configuration.txt` |
| MEASURED | `proguard-rules.pro` contains **no** keep rule — comments only | `Get-Content app/proguard-rules.pro` → no non-comment line |
| MEASURED | Build command, attempt 2 exit code **0**, `BUILD SUCCESSFUL in 3m 45s`, 226,809 ms | `logs/gradle-build2.log`: `GRADLE_COMMAND=…\gradlew.bat assembleDebug assembleRelease --console=plain`, `GRADLE_EXIT=0`, `GRADLE_ELAPSED_MS=226809` |
| MEASURED | Attempt 1 failed at `compileReleaseKotlin`/`compileDebugKotlin`, exit 1, 271,362 ms — my own smoke code, not Orekit | `logs/gradle-build.log`: `GRADLE_EXIT=1`, `GRADLE_ELAPSED_MS=271362`, `> Task :app:compileReleaseKotlin FAILED` |
| MEASURED | R8 emitted one non-fatal `WARNING` about `META-INF/services/com.sun.source.util.Plugin` | `logs/gradle-build2.log`: `R8: Unexpected reference to missing service class: META-INF/services/com.sun.source.util.Plugin.` |
| MEASURED | No Orekit-specific R8 keep rule was needed; 220/249 `org.orekit.*` and 49/59 `org.hipparchus.*` classes survived; nothing Orekit failed to link | `build/outputs/mapping/release/mapping.txt` (159,144 lines) counted by package, `R8$$REMOVED$$CLASS` rows excluded; zero linkage errors in logcat |
| MEASURED | APK = 4,379,153 B = 4.176 MiB; single `classes.dex` = 1,753,644 uncompressed / 900,383 compressed; 173 entries / 4,307,526 B compressed payload | `ZipFile` enumeration of `app/build/outputs/apk/release/app-release.apk`; `DEX_COUNT=1`; sha256 `39FAE799…E25A` |
| MEASURED | Orekit+Hipparchus embedded assets = 3,260,714 B compressed = **75.7 %** of the payload; dex = 20.9 % | Zip enumeration: `assets/org/orekit` 88 entries 10,187,109/2,558,110; `assets/org/hipparchus` 7 entries 1,911,942/702,604 |
| MEASURED | Unshrunk debug APK = 18,545,325 B, i.e. R8 removed 76.4 % of the artifact | `app/build/outputs/apk/debug/app-debug.apk` size vs release |
| MEASURED | Device serial `R58MB3SGZVL` reports model `SM-A405FM`, Android 11 | `adb -s R58MB3SGZVL shell getprop ro.product.model` → `SM-A405FM`; `…ro.build.version.release` → `11` |
| MEASURED | Release APK installed successfully | `adb -s R58MB3SGZVL install -r …` → `Success`, exit 0 |
| MEASURED | App ran the full chain on device and printed `OREKITSMOKE_OK`; identical output on two consecutive runs | `logcat-full.txt` lines 1989/2202 and 3046/3048 (tag `OrekitSmoke`, pid 29731) |
| MEASURED | On-device numerics are byte-identical to the JVM reference | device `TOPO_AT_T+600s elevation_deg=-44.136279 azimuth_deg=354.524835`; `.kiln/scratch/positioning-2026-10-08/run-tinydata.txt:5` carries the same string |
| MEASURED | The equality is exact, case-sensitive string equality — not a visual comparison | `$jset -ceq $dset` over the `TEME_AT`+`TOPO_AT` lines of both sources → `CROSSCHECK=IDENTICAL` |
| MEASURED | The event-detector parameters are identical in both programs | `probe/orekit/PassProbe.java:88-89` and `app/src/main/java/com/gexetr/orekitsmoke/OrekitChain.kt:105-107` — both `ElevationDetector(60.0, 1.0e-6, topo).withConstantElevation(radians(10))` |
| MEASURED | The propagation windows deliberately differ: 3 days in one unsegmented call vs 1 day in 24 one-hour windows | `PassProbe.java:90` (`propagate(epoch, epoch.shiftedBy(3*86400))`); `OrekitChain.kt:110-118` (the 3600 s walk loop); device `SEGMENTS=24` |
| MEASURED | The device's 5 AOS in one day is the **segmented** result, not a reproduction of the JVM probe's 1 AOS | device `AOS_1D_10DEG=5` vs JVM `PASSES_3D_10DEG=1`; `OrekitChain.kt:102-103` states segmentation exists because the unsegmented multi-day call finds 1 AOS where ~15 exist |
| INFERRED | The first-LOS divergence (deep fractional digits only) is root-finding against a differently-segmented state history, not a numeric disagreement | derived from the measured identical detector parameters plus the measured different windows; the first AOS still matches to 18 decimals. **Not** confirmed by a common event count, because the two programs report different windows |
| MEASURED | No `NoClassDefFoundError`, `NoSuchMethodError`, `VerifyError`, `LinkageError` or `FATAL EXCEPTION` anywhere in the capture | scan of all 3,908 logcat lines; only app `E` line is the benign `Not starting debugger … jdwp agent` |
| MEASURED | The chain output is rendered on screen, not only logged | `ui-dump.xml` `TextView` `text=` carries the same string under `Orekit on Android`; `screen.png` 340,289 B with PNG magic `89 50 4e 47 0d 0a 1a 0a` |
| MEASURED | Vallado subset = 135,064 B / 3,601 lines across 3 files, and compiles to 40,528 B of class files | `.kiln/scratch/nv-core-proof/data/vallado-java-src/` sizes; `.kiln/scratch/nv-core-proof/out/vallado/*.class` sizes |
| MEASURED | **No Vallado corpus run exists on disk** — only the Orekit corpus output | recursive filename search for `vallado` in `nv-core-proof` returns sources, C++ reference tree and `.class` files, no run log; the only corpus output is `orekit-corpus-output.txt` |
| MEASURED | Build wall clock: 271,362 + 226,809 = 498,171 ms ≈ 8 m 18 s; device phase ≈ 45.7 s | `GRADLE_ELAPSED_MS` in both logs; per-command `Command completed in` readings 11,504 / 26,634 / 7,583 ms |
| ON RECORD | The `20413#2` far-range block and the three error-code fixtures behave as `RESULT.md` states | `.kiln/scratch/nv-core-proof/RESULT.md` § SGP4 corpus output and per-case table |
| ON RECORD | `com.gexetr` is the developer account's namespace; the factory's test phone is the A40 | `AGENTS.md` § "Published namespace"; `.cursor/rules/device-testing.mdc` serial table |
| INFERRED | Orekit/Hipparchus' embedded assets could be excluded with a `packaging.resources.excludes` entry to remove most of the 3.11 MiB | The 88 `ITU-R-P.834` files and `new-joe-kuo-6.21201` total 95 of the 95 asset entries and are loaded lazily, not by this chain — but this exclusion was **not built or tested**, and doing so requires re-running this proof |
| INFERRED | The F-map's `OREKIT_RISKY_USE_VALLADO_SUBSET` recommendation rested on unmeasured risk and is now weakened | It states "no Android build was run"; the Android build has now been run and passed with no keep rule |
| UNVERIFIED | The `javax.xml` paths in Orekit work on Android | No XML path was exercised by this chain. Settling it needs a smoke that loads EOP/frame data through an XML `DataSource` — e.g. `FilesManager`/`XMLParser`-backed `EopHistory` |
| UNVERIFIED | The Vallado Java subset passes the SGP4 corpus | No run output exists on disk; the command that would settle it is the CelesTrak `SGP4verification` runner over `SGP4-VER.TLE` / `TCPPVER.OUT` |
| UNVERIFIED | The Vallado Java conversion may be vendored into a shipped app | No standalone license file was found in the inspected archive; `RESULT.md` records the same gap |
| UNVERIFIED | The device's segmented event grid (`SEGMENTS=24 EVENTS=10 AOS_1D_10DEG=5`) has a JVM counterpart | No JVM run used the same 24 × 1-hour window; the JVM probe used one 3-day call. Settling it needs the identical segmentation run under `javac`/HotSpot — the device is the only place this grid has been produced so far, so it is a measured *device* result with no cross-toolchain twin |
| UNVERIFIED | The exact Orekit-attributable MB cost in a product APK | No baseline build without the jars was made; what is measured is the full inventory in §5, from which Orekit/Hipparchus' 3,260,714 compressed bytes are exact but their *incremental* cost over a no-Orekit app is not |

---

## 10. Reproduction

```powershell
# build (JDK 17 pinned, TEMP redirected to .kiln/scratch/gradle by scripts/lib/gradle.mjs)
cd D:\Python\PlayKiln\.kiln\scratch\android-orekit-smoke
node tools/build.mjs assembleDebug assembleRelease

# install + run, addressed by serial on every command
adb -s R58MB3SGZVL shell getprop ro.product.model            # must read SM-A405FM
adb -s R58MB3SGZVL install -r app\build\outputs\apk\release\app-release.apk
adb -s R58MB3SGZVL logcat -c
adb -s R58MB3SGZVL shell monkey -p com.gexetr.orekitsmoke -c android.intent.category.LAUNCHER 1
adb -s R58MB3SGZVL logcat -d -s OrekitSmoke:* AndroidRuntime:*
adb -s R58MB3SGZVL shell screencap -p /sdcard/s.png; adb -s R58MB3SGZVL pull /sdcard/s.png screen.png
adb -s R58MB3SGZVL exec-out uiautomator dump /dev/tty > ui-dump.xml
```

Evidence on disk under `.kiln/scratch/android-orekit-smoke/`: `logcat-full.txt` (3,908 lines), `screen.png`
(340,289 B), `ui-dump.xml`, `logs/gradle-build.log`, `logs/gradle-build2.log`,
`app/build/outputs/mapping/release/mapping.txt`.

---

## 11. The two limits this report does not paper over

1. **Not a product APK.** This is a Compose smoke app carrying the chain and nothing else. It proves the
   library's build/shrink/run chain; it says nothing about a slot's own UI, and its 4.18 MiB is not a forecast
   for a real app — a real app's own code and iconography come on top, and the 3.11 MiB of Orekit assets could
   come off.
2. **One chain, one TLE, one device.** A single satellite (06251) at a single epoch on one arm64 API-30 phone.
   It does not exercise the XML data path, the `20413#2` corpus anomaly, EOP file loading, or any arm32 device.
   It converts the F-map's `UNVERIFIED` row into a measured PASS **for the TLE → TEME → ITRF → topocentric →
   event chain on the target configuration** — precisely the scope it claims, and no wider.
