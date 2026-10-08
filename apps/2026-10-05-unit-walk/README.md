# UnitWalk

**Slug:** `unit-walk`  
**applicationId:** `com.gexetr.unitwalk`  
**Idea:** UnitWalk — a crash-safe, no-account rental unit walk for self-managing landlords: room-by-room photo condition records that survive a process kill, move-in versus move-out compare per unit, and a signed PDF shared from the phone.

PlayKiln slot — isolated Android attempt. Factory harness lives at the repo root.

## Build

```bash
# from this directory (JDK 17+ and Android SDK required)
./gradlew assembleDebug
./gradlew bundleRelease
```

Windows: `.\gradlew.bat assembleDebug`

If the Gradle wrapper JAR is missing, from a machine with Gradle installed:

```bash
gradle wrapper --gradle-version 8.11.1
```

## Store pack

See `store/` for Play listing drafts.
