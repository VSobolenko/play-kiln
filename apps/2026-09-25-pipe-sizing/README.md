# Pipe Sizing: NFPA 54

**Slug:** `pipe-sizing`  
**applicationId:** `com.gexetr.pipesizing`  
**Idea:** Standards-cited US fuel gas pipe sizing per NFPA 54: every input editable, results in the unit system you chose, the cited code edition shown in-app, offline, bought once.

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
