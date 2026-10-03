# Laneset: Swim Set Builder

**Slug:** `laneset`  
**applicationId:** `com.gexetr.laneset`  
**Idea:** A swim set builder that computes each session from the swimmer's own constraints (minutes, pool length, focus, strokes to avoid, gear on deck) against authored programming rules, walks them through the two pace trials that put a target time on every interval, and prints or shares the set list; one-time unlock, offline, no account.

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
