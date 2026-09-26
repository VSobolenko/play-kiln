# Mark Book for Teachers

**Slug:** `teacher-gradebook`  
**applicationId:** `com.gexetr.teachergradebook`  
**Idea:** Private offline classroom record book a teacher owns: create, edit and delete every class, student, assignment, grade, attendance mark, note, parent contact and seating position without an account or a server, see an explainable running average, export CSV and PDF, and keep a restorable backup file. Free for one class, unlocked once for unlimited classes and export.

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
