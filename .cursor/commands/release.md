---
name: "/release"
id: "release"
category: "Workflow"
description: "PlayKiln phase 2: after the owner approves — build, sign, store pack, screenshots, strict verify"
---

# /release — PlayKiln release phase

The second half of the factory. Development (`/forge` or `/ship`) ends with an app the owner can
look at; this phase turns an **approved** revision into a publish-ready slot. `/forge` chains into
this file automatically once the owner has approved the app — nothing here is skipped by arriving
that way, and the gate below is checked either way.

It runs only after the owner has explicitly approved the release in the current conversation.
If he has not, **stop** and ask — do not record an approval to unblock yourself.

Read `.cursor/rules/release-gate.mdc` before starting.

**Dispatch this phase too.** Release is several questions that do not depend on each other — the
pack's copy, the release notes, the policy and Data-safety answers, the screenshots, and the grading —
so run the writers in parallel over disjoint file lists, and put the grading (`evaluator`,
`play-auditor`) in a context other than the one that authored. Two serializations here are real and
were not chosen: step 0's gate must pass before anything is signed, and Gradle is one consumer per
slot (`.cursor/rules/parallel-execution.mdc`).

## 0. Confirm the gate

```bash
node scripts/approve-release.mjs --check --slot <slug>
```

Exit 0 means a current approval exists for this revision. Exit 2 means one of:

- no approval was ever recorded → ask the owner, then record it with his own words:
  `node scripts/approve-release.mjs --slot <slug> --note "<the owner's own words>"`;
- the app changed after approval → the approved revision no longer exists. Re-approve the
  revision the owner actually reviewed, or ask him to review the new one.

Never run `--revoke`, and never hand-edit `release-approval.json`, to get past this.

## 1. Build and sign

```bash
node scripts/prepare-release.mjs --slot <slug>
```

One Gradle invocation produces the debug APK, the release APK and the AAB
(`.cursor/rules/build-speed.mdc`); `apksigner` verifies the release certificate, and
`dist/release-manifest.json` records the artifacts, the signing fingerprint and the git
commit. Report the printed build time.

**Signing keys are the owner's decision — read `docs/signing.md` before touching one.** First
inspect what exists:

```bash
node scripts/sign-release.mjs --slot <slug>   # alias + upload-certificate SHA-256, or BLOCKED
```

If a key and its `keystore.properties` already exist, there is nothing to create. If none exists,
**stop and ask** before generating anything: `--generate` and `keytool -genkeypair` create the app's
permanent identity and need an explicit instruction in the current conversation, and `--generate`
itself refuses without the owner's recorded release approval (`.cursor/rules/authorization.mdc`,
`.cursor/rules/release-gate.mdc`, `.cursor/rules/signing-keys.mdc`). Never print key material or
`keystore.properties`; the key's backup is `docs/signing.md` § "The backup".

## 2. Store pack

```bash
python scripts/store/render.py --slot <slug>
```

Writes listing copy, privacy, data-safety, review notes, icon, feature graphic and both
locale screenshot sets. Review the copy against the shipped app — `play-auditor` before
marking ready.

The public privacy-policy URL was already published and recorded in phase 1 (`/ship` step 3), because
the app's own in-app policy needs it. Verify it rather than publishing here:

```bash
node scripts/publish-privacy.mjs --slot <slug> --check   # fetches the recorded URL, publishes nothing
```

If the policy text changed after approval, re-run the publisher (it updates the same page and the same
URL) — but that rewrites `store/listing.json`, which is in the approved pack, so the approval lapses
and must be re-recorded on the owner's own words before continuing. Runbook:
`docs/privacy-policy-hosting.md`.

## 3. Screenshots

```bash
node scripts/media-capture.mjs --slot <slug>   # real device journey, preferred
```

Without a device, the store-kit render in step 2 composes synthetic screens; any synthetic
capture must be labelled as such in `store/review-notes.md`. Real captures must come from the
installed **release** build — `media-capture.mjs` proves that chain and refuses otherwise.

## 4. Verify and grade

```bash
node scripts/verify.mjs --slot <slug> --build
node scripts/check-economics.mjs --slot <slug>
node scripts/kiln-status.mjs
```

Then `/eval` with the `evaluator` subagent and `play-auditor`. The generator never grades its
own work. On FIX → generator → re-eval. `UNVERIFIED` is not PASS — read the diff by hand.

## 5. Register, then CHECKPOINT 2 — publish authorization

Update `registry/apps.json`. Artifacts are ready. **Stop and ask** in the owner's own words:
«may I proceed to publish?» Checkpoint 1.5 authorised the release pipeline; this yes authorises
Console submission. Do not propose that the owner publish while you watch.

On yes, the agent walks `docs/play-publishing-playbook.md` §5.1 end to end: declarations, forms,
Data safety / content-rating answers, the release form, submit, and Publish. Three points are tooling
assists where the sandbox cannot act — they are not ownership of the release
(`.cursor/rules/release-gate.mdc` § "Tooling assists"; owner's decision, recorded 2026-10-04):

1. **Store graphics and the AAB/APK chooser** (hidden `input[type=file]`): open the page, name
   absolute paths, ask, wait, then continue.
2. **The promo-code download** (`Монетизация с помощью Play → Promotions` → `Скачать коды`): click
   the button **exactly once**, then **stop and ask** — «нажмите „Скачать“ в открывшемся окне и
   подтвердите, когда файл сохранится» — and **wait**. Do not press again, do not reload. On the
   confirmation, read `promotion_codes.csv` from where the browser saved it (on Windows usually
   `%USERPROFILE%\Downloads`) and continue. If no window opened, give the manual path (page
   `.../promotions`, control `Скачать коды`, file `promotion_codes.csv`) and wait for the owner to
   report the location. The file is never committed.

```bash
node scripts/play-upload.mjs --slot <slug>   # updates after the first Console upload exists
```

## Guardrails

- The gate is not a formality: if it refuses, ask the owner instead of working around it.
- Never commit, never push, never upload, never publish without an explicit instruction in
  the current conversation. A publish yes means **you** drive the console and press submit/Publish.
- Never generate or rotate a signing key on your own initiative.
- Evidence before "done": `verify.mjs` output plus a read diff, never a self-report.
- Report the release phase's elapsed wall clock separately from development time.

## Output

```
## /release complete — <title>

**Slot:** apps/<dir>
**Approval:** recorded <date>, over <N> build inputs
**Artifacts:** dist/ signed APK + AAB + release manifest
**Store pack:** apps/<dir>/store/
**Verification:** <stdout evidence + /eval verdict>
**Release time:** <elapsed wall clock>
**Next:** human checkpoint 2 — ask «may I proceed to publish?»; on yes, agent walks §5.1 and publishes
```
