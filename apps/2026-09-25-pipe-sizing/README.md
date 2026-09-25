# Gas Pipe Sizing Workbook

**Slug:** `pipe-sizing`  
**applicationId:** `com.gexetr.pipesizing`  
**Idea:** A paid-once, offline calculation aid for user-entered gas loads, developed lengths, fitting allowances, unit conversion, and recorded source citations. It performs arithmetic and does not provide code-table data or determine compliance.

PlayKiln slot — one isolated Android app. Factory harness lives at the repo root; this slot is
built and released on its own and shares no code with other slots.

## Build

```bash
# from this directory (JDK 17+ and Android SDK required)
./gradlew assembleDebug
./gradlew bundleRelease
```

Windows: `.\gradlew.bat assembleDebug`

The wrapper is committed and pins Gradle 9.7.1 in `gradle/wrapper/gradle-wrapper.properties`, so
no local Gradle installation is needed and this file should not be regenerated ad hoc.

## Store pack

See `store/` for the Play listing, privacy and data-safety declarations, reviewer notes, checklist,
and the graphics inventory.

The phone screenshots are real captures of the signed release build, driven by `store/journey.json`
on a `Medium_Phone` AVD and composed into marketing images by `scripts/store-studio.py`. The tablet
screenshots are that same phone capture framed at tablet dimensions, **not** captures taken on a
tablet device or emulator - no tablet AVD was available here. Replace them with genuine large-screen
captures before relying on the large-screen eligibility they are meant to support.

Run it with `python scripts/store-studio.py --slot pipe-sizing --preview-circle`. The studio owns the
icon, the feature graphic, the screenshots, and the adaptive-icon vector resources under
`app/src/main/res/drawable/`, so the launcher artwork and the store artwork cannot drift apart;
re-run it after changing the palette in `ui/theme/Color.kt`.
