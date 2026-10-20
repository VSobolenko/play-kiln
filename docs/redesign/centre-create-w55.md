# W55 - Centre Create

## Change

`NavBar` below means
`apps\2026-10-15-plinth-product-photos\app\src\main\java\com\gexetr\plinthproductphotos\ui\components\PlinthNavBar.kt`.

- Added `androidx.compose.runtime.getValue` for the two read-only animated-state
  delegates. No `setValue` is needed: neither delegate is assigned (`NavBar:28,255-264`).
- The bottom bar now has a full-width outer `Box`. Its glass `Surface` reserves
  20 dp above itself; the 56 dp Create FAB is a sibling aligned `TopCenter`, not
  a child trailing the destination row (`NavBar:138-193`).
- Studio and Library share the leading weighted group; More occupies the equally
  weighted trailing group. An 80 dp centre spacer reserves the FAB's horizontal
  lane (`NavBar:153-175`). Thus Create's horizontal centre is independent of the
  uneven destination count. At the minimum 76 dp bar height, Create's centre is
  30 dp above the destination row's centre (28 dp versus 20 + 38 dp).
- Kept `plinthTonalGlass(TOP)`, `onClick = onCreate`, the localized accessible
  Create name, and the wide rail's header FAB (`NavBar:142,179-190,196-221`).
  The public signatures did not change; no `PlayKilnApp.kt` edit was needed.

Chosen over merely inserting the FAB between weighted items: that would place
Create off-centre with three destinations. The sibling alignment and equal side
groups explicitly fix the geometric centre. The reserved top space keeps the
raised control inside the bottom bar's measured bounds rather than translating
it into the content above.

## Observed verification

- Before the layout edit, a Node source assertion requiring
  `align(Alignment.TopCenter)` failed with
  `Create must use parent-centred top alignment, not be a trailing Row child`.
  After the edit, source assertions for that alignment, the import, 20 dp rise,
  80 dp gap, weights, callback and tonal glass passed. These are structural
  assertions, not rendered-UI evidence.
- `rg -n 'import androidx.compose.runtime.getValue' <NavBar>` found the import;
  the final line is 28.
- The first compile attempt used a relative slot directory and exited 1 with
  `The system cannot find the path specified.` Retrying through the same
  factory helper with an absolute slot directory succeeded:

```powershell
@'
import path from 'node:path';
import { runGradleTasks } from './scripts/lib/gradle.mjs';
const r = runGradleTasks(
  path.resolve(String.raw`apps\2026-10-15-plinth-product-photos`),
  [':app:compileDebugKotlin'],
);
console.log(r.result.stdout ?? '');
console.error(r.result.stderr ?? '');
if (r.result.error) console.error(r.result.error);
process.exitCode = r.result.status ?? 1;
'@ | node --input-type=module
```

Observed: `BUILD SUCCESSFUL in 8s`, `:app:compileDebugKotlin` executed, exit 0,
helper elapsed 8,631 ms. Node also emitted the helper's `DEP0190` shell warning.
No release approval was recorded and no release pipeline was run.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Animated-state delegate import is present | MEASURED | `NavBar:28`; final `rg` output |
| Create is structurally raised and horizontally centred | MEASURED | `NavBar:138-182`; source assertions failed before and passed after |
| Glass, callbacks and prominent wide-rail Create remain | MEASURED | `NavBar:142,179-190,196-221` |
| Debug Kotlin compiles | MEASURED | Absolute-path `runGradleTasks` command above: exit 0, `BUILD SUCCESSFUL in 8s` |
| Actual on-device appearance and touch interaction | UNVERIFIED | Install a debug APK and inspect/tap the bottom bar and wide rail; the orchestrator can run this device check |

Elapsed wall-clock at the implementation/compile checkpoint: approximately
3 minutes (12:15:57 to 12:18:33 +03:00); documentation followed.
