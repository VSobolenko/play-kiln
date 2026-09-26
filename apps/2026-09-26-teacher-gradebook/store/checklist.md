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
- [ ] Use `node scripts/play-upload.mjs --slot teacher-gradebook --track internal --dry-run` before any live upload.
- [ ] Obtain explicit human approval before committing a Play edit.
