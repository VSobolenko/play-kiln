# Play publishing playbook

This is the ordered handoff for taking a verified PlayKiln slot live. **Factory**
means automated by the harness; **Human** means a Play Console or legal action that
the factory must not perform silently.

## 1. Confirm account prerequisites — Human

1. Complete developer verification and use the intended personal or organization
   developer account.
2. If the personal account was created after **2023-11-13**, recruit at least **12**
   testers and keep them opted into a closed test continuously for **14 days** before
   applying for production access. Plan about three weeks for the first launch.
3. Confirm the account can register the intended package name.

The closed-test rule above is verified only for new personal accounts. Organization-
account-specific prerequisites are **NEEDS-VERIFICATION** because `FACTS.md` does not
state them.

Sources: **NEEDS-VERIFICATION** — `FACTS.md` supplies no official account, verification, or closed-test URLs.

## 2. Ship and verify the slot — Factory, then Human checkpoint

Run `/ship <idea>`. After plan approval, the factory creates the slot, implements
it, generates the store pack, signs the releases, verifies the slot, and runs
`/eval`.

Required handoff evidence:

```text
dist/<slug>-<version>-<code>-release.apk
dist/<slug>-<version>-<code>-release.aab
dist/release-manifest.json
store/listing.json and listing.md
store/assets/icon-512.png
store/assets/feature-graphic-1024x500.png
6 phone + 4 seven-inch + 4 ten-inch screenshots per locale
green verify-slot --strict-store
accepted /eval
```

The human reads the final summary and diff. A worker's “done” is not evidence.

Sources: factory contract in `AGENTS.md`; no external citation required.

## 3. Create the Play app — Human

Create the app in Play Console and register the exact `applicationId` from
`release-manifest.json`. Do not change a slot's package name during upload.

The Play Developer API cannot create a new app, so this cannot be automated.

Sources: **NEEDS-VERIFICATION** — `FACTS.md` supplies no official API-limitation or package-registration URL.

## 4. Establish signing — Factory + Human

**Factory:** `scripts/sign-release.mjs` configures or inspects the local upload key,
keeps the keystore outside Git, reads ignored `keystore.properties`, and reports the
upload-certificate SHA-256 fingerprint. `scripts/prepare-release.mjs` builds the
release APK/AAB and verifies the APK signature.

**Human:** enroll the app in Play App Signing during the Console flow and compare the
expected upload certificate with the factory's recorded fingerprint before upload.

Factory upload-key material and Play App Signing are separate control surfaces. The
exact external distinction between the upload key and Google's app-signing key is
**NEEDS-VERIFICATION** because `FACTS.md` provides no signing documentation.

For key rotation or reset:

1. Stop release automation; do not replace a key silently.
2. Follow the current Play Console key rotation/reset flow. The exact eligible flow,
   waiting periods, and Console labels are **NEEDS-VERIFICATION**.
3. Store the approved replacement key outside Git and update the ignored local
   signing configuration.
4. Re-run `node scripts/sign-release.mjs --slot <slug>` and compare the new SHA-256
   fingerprint with the Console record.
5. Re-run release preparation and verification before any upload.

Sources: **NEEDS-VERIFICATION** — no official Play App Signing or key-rotation URL is present in `FACTS.md`.

## 5. Perform the first upload — Human

Upload the signed AAB manually in Play Console. Complete any first-upload setup the
Console requires and confirm that the package name and upload certificate are
accepted. The factory must not attempt `play-upload.mjs` before this succeeds.

Sources: **NEEDS-VERIFICATION** — `FACTS.md` states the first AAB must be uploaded manually but provides no official URL.

## 6. Complete the store listing — Factory prepares, Human confirms

**Factory:** generates listing text within 30/80/4000-character limits, a 512×512
icon, a 1024×500 feature graphic, 6 phone screenshots, 4 seven-inch screenshots,
and 4 ten-inch screenshots per locale. It validates format, dimensions, alpha, byte
size, and count.

**Human:** confirm every claim and screenshot matches the uploaded build; review every
asset explicitly marked synthetic for truthful representation of shipped behavior;
provide demo credentials and review instructions when the app is gated.

Sources: **NEEDS-VERIFICATION** — `FACTS.md` names Play Console Help article 9866151 but provides no official URL.

## 7. Complete legal and audience declarations — Human

Use the generated files as evidence, not as authorization to guess:

- Submit Data safety even if no data is collected; reconcile every SDK and the
  public privacy-policy URL.
- If accounts exist, verify deletion works in-app and through the declared external
  web resource.
- Complete the content-rating questionnaire.
- Declare whether the app contains ads.
- Complete target-audience declarations.
- Confirm permissions, foreground-service types, exact-alarm use, and reviewer access.

The human owns the truth and legal effect of these declarations. If code, policy,
and form disagree, stop and fix the app or the draft before submission.

Sources: **NEEDS-VERIFICATION** — no official Data safety, privacy, deletion, ads, rating, or audience URLs are present in `FACTS.md`.

## 8. Complete AI-content declarations when relevant — Factory + Human

**Factory:** verify that generative-AI features include in-app reporting or flagging,
do not produce prohibited content, and are more than a thin API wrapper.

**Human:** complete the applicable Console declarations and review the shipped safety
behavior. Do not publish if the reporting path cannot be demonstrated.

Sources: **NEEDS-VERIFICATION** — no official AI-generated-content policy URL is present in `FACTS.md`.

## 9. Test and request production access — Human

For an affected new personal account, maintain the 12-tester/14-day closed test and
then apply for production access. For other account types, follow the Console path
shown for that account; those account-specific rules are **NEEDS-VERIFICATION**.

Sources: **NEEDS-VERIFICATION** — no official testing or production-access URL is present in `FACTS.md`.

## 10. Roll out — Human approval, Factory may automate updates

Choose the production release in Play Console and use a staged rollout when desired.
The exact staged-rollout controls and percentages are **NEEDS-VERIFICATION** because
they are absent from `FACTS.md`.

After the first manual upload, `scripts/play-upload.mjs` may automate an approved
update through Android Publisher v3: create edit, upload bundle, patch listings,
upload images, update track, and commit. It uses service-account credentials from an
environment-provided local path. Never commit the JSON; never commit an edit without
explicit human approval.

Sources: **NEEDS-VERIFICATION** — no official edits/tracks/staged-rollout URL is present in `FACTS.md`.

## 11. Publish — Human

Review Console warnings, legal declarations, countries, price, and rollout choice.
Press **Publish** only when they match the approved release. This is the second human
checkpoint: the human presses Publish, and the action is never delegated to an agent.

Sources: human-control rule in `MISSION.md`; no external citation required.

## 12. Monitor after launch — Human + Factory

Monitor Android vitals and user feedback. Treat these as bad-behavior ceilings:

| Metric | Overall | Per device model |
|---|---:|---:|
| User-perceived crash rate | < 1.09% | < 8% |
| User-perceived ANR rate | < 0.47% | < 8% |
| Excessive partial wake locks | < 5% | — |

Also inspect p90 anonymous RSS plus swap, bitmap memory, and jank. Pause or repair a
rollout when evidence shows a release is unsafe; do not hide failures with listing
copy. Feed recurring faults back into `harness/lessons.md` and the root harness.

Sources: **NEEDS-VERIFICATION** — no official Android vitals URL is present in `FACTS.md`.
