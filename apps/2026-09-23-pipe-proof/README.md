# PlayKiln Pipe Proof

**Slug:** `pipe-proof`  
**applicationId:** `com.gexetr.pipeproof`  
**Idea:** Throwaway pipeline proof: scaffold to signed APK and AAB to store pack to verified gates, with no hand edits.

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
