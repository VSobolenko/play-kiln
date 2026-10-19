# Navigation hang — `selectSection` never terminated

Worker W15, 2026-10-17. Owns `ui/PlayKilnApp.kt` and `app/src/test/.../ui/BackStackOpsTest.kt`.
Compilation is **UNVERIFIED**: no Gradle was run (the orchestrator owns the build slot).

## The defect

`selectSection` (the section switch for the bottom bar and the navigation rail) drained the stack with:

```kotlin
while (backStack.isNotEmpty()) backStack.pop()
backStack.push(destination)
```

The file's own `pop()` keeps the root: `if (size > 1) removeLastOrNull()`. At `size == 1`,
`isNotEmpty()` is still true and `pop()` does nothing, so the loop spins forever on the main thread.
That is an ANR.

**How a user reaches it:** any call to `selectSection` hangs, because the stack always holds at
least the root (`rememberNavBackStack(HomeDestination)`). The callers are:
- tapping a tab in `PlinthBottomNavBar` (`onSelect = ::selectSection`) on a compact window;
- tapping a section in `PlinthNavigationRail` on a medium or expanded window;
- `onNavigateToLibrary` from Home;
- `openCreateFlow` (the bottom-bar and rail create action);
- `openLibraryWallet` (the wallet balance in the top bar);
- `navigateBack()` from a size-1 Library or More root.

## Why the suggested "drain to root, then push" shape was rejected

The obvious shape is `while (size > 1) pop(); push(destination)`. It terminates, but it produces
`[Home, Library]`. Two other parts of the file read the stack differently:
- `selectedSection` reads `backStack.firstOrNull()` (`PlayKilnApp.kt:215`), so Studio would stay
  highlighted while the user is on Library.
- `isTopLevel` requires `size == 1` (`PlayKilnApp.kt:220`), so the section top bar would vanish on
  Library and More.

`navigateBack()` (`PlayKilnApp.kt:235`) also assumes a section can be a size-1 root: it has a branch
for "size 1 and not Studio". The file's one notion of the root is therefore **the first entry**. The fix
keeps that notion and replaces the retained root instead of stacking on top of it.

## The fix

```kotlin
// selectSection, PlayKilnApp.kt:232
backStack.resetToRoot(destination)

// PlayKilnApp.kt:1133
internal fun <T> MutableList<T>.resetToRoot(root: T) {
    while (size > 1) popKeepingRoot()
    if (isEmpty()) add(root) else if (first() != root) set(0, root)
}
```

The loop terminates. Each iteration runs with `size > 1`, so `popKeepingRoot()` removes exactly one
entry, and the condition and the helper agree on the terminal state, `size == 1`. The stack is never
empty in between, which matters because `NavDisplay` cannot render an empty stack.

The new helpers are plain `MutableList` operations, so a JVM test can run them:
`popKeepingRoot`, `pushUnlessTop`, `resetToRoot` and `replaceDetail`. The private `NavBackStack.pop()`
and `NavBackStack.push()` now delegate to them, so the pop rule exists in one place.
`NavBackStack` is a `MutableList`; the file already calls `removeLastOrNull()`, a `MutableList`
extension, and `firstOrNull()` on it.

### Stack states the fix produces

| From | Action | Result | `selectedSection` / `isTopLevel` |
|---|---|---|---|
| `[Home]` | tap Library | `[Library]` | LIBRARY / true |
| `[Home]` | tap Studio | `[Home]` (no-op) | STUDIO / true |
| `[Home, Import, AutoCard]` | tap More | `[More]` | MORE / true |
| `[Library]` | back | `navigateBack` → `selectSection(STUDIO)` → `[Home]` | STUDIO / true |
| `[Library, Wallet]` | back | `[Library]` | LIBRARY / true |
| `[Home, Import]` | wallet chip | `[Library, Wallet]` | LIBRARY / false |
| any | create | `[Home, Import]` | STUDIO / false |

On a wide window, `[Library, Project(x)]` renders as list plus detail, because `LibraryDestination` is the only
`listPane()` entry (`PlayKilnApp.kt:434`). Every other root is a `detailPane()` and renders as a single pane.

## Sweep findings (`ui/PlayKilnApp.kt`, navigation region)

1. **Old `PlayKilnApp.kt:232` `while (isNotEmpty()) pop()`** — the loop's condition and its helper
   disagreed about the terminal state. It was reachable and hung forever. **Fixed** (above).
2. **`PlayKilnApp.kt:457` `onOpenProject` (previously `backStack.push(ProjectDestination(id))`)** —
   unbounded growth. On a medium or expanded window the Library list pane stays visible beside the
   project detail. `push` only refuses a duplicate of the *top*, so tapping project A, then B, then A
   stacks `[Library, P(A), P(B), P(A), …]` without bound, and Back must walk through every entry.
   Reachable on a wide window. **Fixed** with `replaceDetail`, which drains to the root and then
   pushes. On a compact window the list is only visible when Library is the whole stack, so there the
   drain is a no-op and behaviour is unchanged. Opening a project from the list now drops an open
   Group, Looks or Batch detail beneath it. That is the list-detail meaning of choosing another item.
3. **Root notions** — the start destination `HomeDestination` (`rememberNavBackStack`, line 194), the
   Studio mapping in `selectSection`, `selectedSection` reading `firstOrNull()` (215), and `isTopLevel`
   requiring `size == 1` (220). Before the fix, any drain-and-push shape made "first entry" and
   "current section" disagree (see above). After the fix, every section switch writes the first entry,
   so the four agree. This is now a design smell rather than a defect. **Left**, as instructed: no
   restructure.
4. **`PlayKilnApp.kt:649` `findActivity` `while (current is ContextWrapper)`** — this terminates. It
   returns on an `Activity`, breaks on a self-referencing `baseContext`, and otherwise walks a finite
   wrapper chain. No defect.
5. **`onChangePhoto` (~383) and `onRegenerate` (~403)** pop once, then push only if the target is not
   on top. They are bounded and **left**. On a size-1 stack the `pop()` is a no-op. That stack shape is
   not reachable for those entries, because they are always pushed above a root.
6. **The other `push` callbacks** (Home actions, Project → Group/Looks/Export, StudioTools → Wallet,
   Coach → AutoCard) are on `detailPane` entries. These render single-pane, so the callback's screen is
   only visible while its entry is on top. A double tap is absorbed by the duplicate-of-top guard.
   They are bounded and **left**.
7. **Design note, UNVERIFIED** — whether `NavDisplay` routes system Back to `onBack` when the stack has one
   entry was not checked against the library source. If it does not, the "size 1 and not Studio" branch of
   `navigateBack` is reachable only through the in-app back affordances. Left.

## Test

Path: `app/src/test/java/com/gexetr/plinthproductphotos/ui/BackStackOpsTest.kt` (JUnit 4, plain
JVM, no Android types). Every case has `@Test(timeout = 2_000)`, so the old `while (isNotEmpty())` shape
fails by timeout instead of hanging. The cases:
- `resetToRoot` drains `[home, import, autocard, result]` to exactly `[library]`;
- `[home]` → `resetToRoot("library")` has `first() == "library"` and size 1, which the
  drain-and-push shape fails;
- resetting to the same root is a no-op, and an empty stack is seeded;
- `popKeepingRoot` never empties the stack;
- `pushUnlessTop` refuses a duplicate of the top;
- 100 alternating `replaceDetail` taps leave `[library, project-b]`.

**Not executed.** `kotlinc` is not on PATH, and Gradle is reserved for the orchestrator. The orchestrator
runs it with `testDebugUnitTest --tests '*BackStackOpsTest'`.
