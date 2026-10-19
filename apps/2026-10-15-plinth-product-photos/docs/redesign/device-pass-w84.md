# Device pass W84 — Plinth Product Photos

## Outcome

The run used only `R58MB3SGZVL` (`SM-A405FM`). The installed artifact was the slot's debug APK; its configured debug package is `com.gexetr.plinthproductphotos.debug`. The Studio screen already had a product photo selected, so no import was needed.

Elapsed wall-clock time from the session start: **13m 26.9s**.

Relight was selectable at 9 credits for a displayed cost of 5. Its `Choose` control was enabled; after choosing it, `Generate · 5 credits` was also enabled. Tapping Generate reduced the Studio balance from 9 to 4. The request was accepted and charged; this run does **not** claim that a distinct success screen or final generation status was observed.

Profile and Library showed readable date/time or project/file titles, not UUIDs or job IDs. One UI discrepancy remains: Profile's body showed the current 4-credit balance after the request, while the shared balance chip in Profile and Library still showed 9.

`node scripts/verify.mjs --slot plinth-product-photos --build` exited 2: **124 PASS, 2 UNVERIFIED, 4 SKIP**. `slot.device` and `build` both PASS. There were no FAIL rows. The two UNVERIFIED rows were `pack.inventory` (no `store/assets` directory) and `pack.media` (no `store/assets`); neither is a device-run failure.

## Journey

| Step | Result | Evidence |
|---|---|---|
| Confirm target and install | `adb devices -l` listed `R58MB3SGZVL` as `device`; `getprop ro.product.model` returned `SM-A405FM`. `adb install -r` returned `Success`; the debug package launched and `MainActivity` resumed. Studio showed an already-selected product photo and 9 credits. | `w84-01-launch.png`, `w84-01-launch.xml` |
| Choose Relight | Studio listed **Изменить освещение** at 5 credits. With 9 available, the measured `Выбрать обработку` button's clickable parent was enabled. Tapped its center from bounds `[224,824][1032,968]`. | `w84-05-studio-more-tools.png/.xml`, `w84-06-relight.png/.xml`, `w84-10-operation-list.xml` |
| Generate | The Relight form displayed **Создать · 5 кредитов**; its clickable parent was enabled. Tapped its measured center at `[327,1736][753,1796]`. The subsequent Studio balance was 4. No error dialog was displayed; the delayed capture did not show a distinct completion message. | `w84-11-relight-chosen.png/.xml`, `w84-12-generate-tapped.png/.xml`, `w84-14-post-generate-panel.png/.xml` |
| Profile | Profile's recent result title was **17 окт. 2026 г. 17:53**. The body showed **4 кредита**. The top balance chip still showed 9. No UUID or job ID appeared in the visible recent-result label. | `w84-15-profile.png/.xml` |
| Library | Visible project titles included **images (1).jpeg**, **images (2).jpeg**, and a date/time project title, **17 окт. 2026 г. 19:59**. No raw UUID or job ID appeared in the visible titles. The top balance chip still showed 9. | `w84-16-library.png/.xml` |

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| The target was online and was the specified model. | MEASURED | `adb devices -l` → `R58MB3SGZVL device`; `adb -s R58MB3SGZVL shell getprop ro.product.model` → `SM-A405FM`. |
| The tested APK and build revision are recorded from measured values. | MEASURED | `Get-FileHash -Algorithm SHA256 apps\2026-10-15-plinth-product-photos\app\build\outputs\apk\debug\app-debug.apk` → `5e33681383965a97a26c64cf17d9349016e875194a9d6c3a9ac1a1f00752566b`. `node --input-type=module -e "import { releaseSourceDigest } from './scripts/lib/release-approval.mjs'; console.log(releaseSourceDigest('apps/2026-10-15-plinth-product-photos').digest)"` → `96f4ce87d13f80ce17c1345310a0fb13c5f41eb7d9fe36c79664f616d8199a7b`. |
| Relight's Choose control was enabled with balance greater than its cost. | MEASURED | `adb -s R58MB3SGZVL exec-out uiautomator dump /dev/tty`; `w84-10-operation-list.xml` shows the Relight Choose parent `clickable="true" enabled="true"`, balance 9, and 5-credit Relight cost. |
| Generate was enabled, displayed the 5-credit price, and was accepted. | MEASURED | Same UI-dump command; `w84-11-relight-chosen.xml` shows `Создать · 5 кредитов` under a clickable, enabled parent. After the measured tap, Studio showed 4 credits (`w84-14-post-generate-panel.xml`). Completion of the generated output is not claimed. |
| Profile and Library displayed human-readable titles instead of UUID/job IDs. | MEASURED | `adb -s R58MB3SGZVL exec-out uiautomator dump /dev/tty`; `w84-15-profile.xml` and `w84-16-library.xml` contain the titles listed above and no visible UUID/job identifier. |
| The shared balance chip refreshed after generation. | FAIL | The same Profile and Library UI dumps show the chip at 9 while Profile's body shows 4 (`w84-15-profile.xml`, `w84-16-library.xml`). |
| `slot.device` agrees with the tested revision and artifact. | MEASURED | `node scripts/verify.mjs --slot plinth-product-photos --build` → `PASS slot.device`; the row says the recorded build digest matches and the APK SHA-256 matches the APK as built. |

Evidence captures and UI hierarchy dumps are under `.kiln\scratch\device-plinth-product-photos\`.
