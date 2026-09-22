# PlayKiln Smoke

**Slug:** `smoke`  
**applicationId:** `com.gexetr.smoke`  
**Idea:** Pipeline smoke test: proves new-slot -> signed APK + AAB -> store pack -> verify PASS.

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
