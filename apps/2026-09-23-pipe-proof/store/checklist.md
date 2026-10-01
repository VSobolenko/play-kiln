# Google Play upload checklist — PlayKiln Pipe Proof

- [ ] Run `python scripts/store/validate.py --slot pipe-proof` and resolve every FAIL.
- [ ] Replace synthetic screenshots with real captures when `graphics.md` marks them synthetic.
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
- [ ] Use `node scripts/play-upload.mjs --slot pipe-proof --track internal --dry-run` before any live upload.
- [ ] Obtain explicit human approval before committing a Play edit.
