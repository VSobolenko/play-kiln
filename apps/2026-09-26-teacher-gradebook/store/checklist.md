# Google Play upload checklist — Mark Book for Teachers

- [ ] Run `python scripts/store-kit.py --slot teacher-gradebook --validate-only` and resolve every FAIL.
- [ ] Inspect every captured screen for personal or test data.
- [ ] Compare listing claims and captions with the release build.
- [ ] Replace placeholder contact email and privacy-policy URL in `listing.json`.
- [ ] Host `privacy.md` at the HTTPS URL declared in `listing.json`.
- [ ] Audit permissions and third-party SDK behavior against `data-safety.md`.
- [ ] Build and locally test the signed release AAB from `dist/`.
- [ ] Enrol in Play App Signing and keep upload credentials outside the repository.
- [ ] Complete Ads, Content rating, Target audience, App access, and Data safety declarations.
- [ ] Upload phone, 7-inch tablet, and 10-inch tablet assets for every locale.
- [ ] Verify edge-to-edge, orientation, text scale, and tablet layouts on representative devices.
- [ ] For a brand-new package, upload the first AAB and complete legal declarations manually in Play Console.

## Billing — create and activate what the app asks for

A purchase flow that references an unknown product fails in the reviewer's session, so every
step below has to be done, and confirmed with a real test purchase, before review submits.

- [ ] Create each of these in Play Console → Monetize with Play → Products → In-app products: `recordbook_pro`.
- [ ] Activate each product's purchase option. An option left in Draft *isn't returned by the Play Billing Library*, so the app behaves as if the product does not exist.
- [ ] Add the account that will test purchases to Play Console → Settings → License testing. An account added only as an internal tester on a track is **not** a license tester, and a purchase made by it is charged **real money**.
- [ ] Use `node scripts/play-upload.mjs --slot teacher-gradebook --track internal --dry-run` before any live upload.
- [ ] Obtain explicit human approval before committing a Play edit.
