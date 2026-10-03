# Privacy-policy hosting — where the policy lives, how it is published, and who does what

This is the runbook a builder reads when a slot needs a privacy-policy URL. It exists because the
factory had no answer to a question Play asks **every** app: the first two slots both pointed at
`https://example.com/privacy` — a placeholder that is not a policy, not reachable as one, and that no
gate looked at — and `apps/2026-10-02-laneset` declares no URL at all. Nothing here is a policy of
its own: the binding boundary is `.cursor/rules/privacy-policy.mdc`, and the requirement it rests on
is Play's own, quoted below.

The one sentence a reader needs: **the factory publishes the policy itself; the owner is never asked
for a host.**

## What Play requires, and where the requirement is recorded

Play requires the policy in **two** places: a public HTTPS URL on the store listing, and a link the
user can open **from inside the app**. The console states both on
`Контент приложения → Политика конфиденциальности` (`.../app-content/privacy-policy`); its prose,
read live in the RU console on 2026-09-29 and archived in `docs/play-publishing-playbook.md:183`, is:

> Следует разместить ссылку на политику конфиденциальности на странице приложения в Google Play и в
> самом приложении, даже если оно не предназначено для детей и не получает…

The public URL satisfies one half and the app satisfies the other, which is why this runbook couples
publishing to the app's own strings rather than to the listing alone. The store half is the field
`URL политики конфиденциальности`; the in-app half is measured by `slot.inAppPolicy`
(`scripts/check-in-app-policy.mjs`). The floor is also restated in `AGENTS.md` § "Play compliance
floor".

## Why Telegraph

The URL is hosted on **Telegraph** (`telegra.ph`), used through its anonymous API (the `API` constant
in `scripts/publish-privacy.mjs`). The owner has no server and no domain, and the factory is
deliberately not a web-hosting project, so the choice is about the smallest thing that satisfies
Play:

- **free and anonymous** — no account, no login, no payment, no card;
- **no domain and no deployment step** — nothing to buy, nothing to configure, nothing to keep alive;
- **HTTPS** — the scheme Play requires;
- **a permanent public URL** — the page is not a temporary upload;
- **updatable in place** — a token created once lets a later run edit the same page, so the URL a
  reviewer may already have seen does not change when the text is corrected.

**What depending on a third party means, stated rather than hidden.** The page lives on
`telegra.ph`, not on a host the owner controls: Telegraph could delete it, change its terms, or go
away, and this factory has no service contract with it. That is the real cost of taking the ready-made
service instead of standing up a site, and it is accepted because the alternative — a server, a
domain and a deployment — is a project the factory is not. The committed source of truth is
`store/privacy.md`, so a lost page can be republished to a new URL, and re-running the publisher
after that rewrites the URL in both the listing and the app.

## The command, end to end

```bash
node scripts/publish-privacy.mjs --slot <slug>
node scripts/publish-privacy.mjs --slot <slug> --check   # verify the recorded URL, publish nothing
```

What the publishing run does, in order:

Each step below names the function it is or the message it prints rather than a line number, because a
line number in prose is a copy that drifts — these moved twice inside a single session while this
section was being written, which is exactly how a citation stops being evidence.

1. Resolves the slot by slug and reads `store/listing.json` and `store/privacy.md`.
2. Converts the policy markdown to Telegraph's node format — `inline()` and `blocks()`; tables become
   definition-like paragraphs because Telegraph has no table tag and dropping the cells would lose the
   disclosures that matter most.
3. Reuses the token already recorded for the slot, or creates an anonymous publishing account and
   records its token — `recordedToken()` and `recordToken()`.
4. **Updates the existing page when the recorded URL is already one of ours** — the `editPage` call, so
   a correction keeps the same URL — and creates a page only for a slot that has none.
5. Fetches the new URL **from the outside** with `reachable()` before recording it.
6. Writes the URL into `store/listing.json` as `privacyPolicyUrl` and prints it.

**The two things it refuses to do**, and each refusal is deliberate:

- **It will not publish a stub.** If `store/privacy.md` converts to fewer than 10 blocks, it stops with
  `refusing to publish a stub` rather than put an empty or near-empty page behind a URL Play will show
  a reviewer. A policy that is not there is worse than a missing URL, because it looks finished.
- **It will not record a URL it could not fetch.** The write is accepted only when the page answers
  HTTP 200 **and** its body contains the word "privacy" — the guard that prints `is not readable`; the
  API saying `ok` proves the write succeeded, not that a reviewer can read the page.

`--check` publishes nothing and only re-verifies the URL already recorded, exiting non-zero when the
page is unreachable or the listing does not carry an https URL.

## Where the token lives, and why it is gitignored

The publisher records `TELEGRAPH_ACCESS_TOKEN` in the slot's **`.env`** (`apps/<slot>/.env`, written by
`recordToken()`). The slot's `.gitignore` ignores `.env` and `.env.*`
(`scaffold/android-compose/.gitignore:40`) and un-ignores only the placeholder `.env.example`, so the
token never reaches git. It is a credential for the page: whoever holds it can edit the policy under
the app's name, so it is treated exactly like every other slot credential
(`.cursor/rules/security-secrets.mdc`) and is **never printed** — the publisher prints only whether
it created an account or reused one.

## The order of operations

The URL is a **build input**, not a release formality, and that fixes the order:

1. **During development (phase 1), before the app is reviewed.** The in-app policy is a separate Play
   requirement, so the app itself needs the URL: it is written into a `translatable="false"` string in
   `res/values/` and opened from the app (`slot.inAppPolicy` measures exactly that). The app cannot
   ship a link it does not have, so the policy is published while the app is still being built.
2. **The publisher writes the listing, and the builder writes the app.** `publish-privacy.mjs` records
   the URL in `store/listing.json`; the app's string resource is then set to the same URL by hand,
   because the in-app half is a resource, not a listing field.
3. **Do this before the release approval, not after.** `store/listing.json` is part of the pack the
   owner approves: editing it after an approval voids that approval and the release scripts refuse.
   Publishing during development keeps that from appearing as a mysterious gate failure in phase 2.

Phase 2 (`/release`) then validates the pack; it does not need to publish anything unless the policy
text changed after approval (below). This keeps a development step out of release work, exactly as
`.cursor/rules/release-gate.mdc` requires.

## When the policy text changes

Re-run the publisher. Because the recorded URL is already a Telegraph page of ours, it calls
`editPage`, so **the page and the URL stay the same** — a reviewer
who already has the link keeps a working link, and no second page accumulates.

Two consequences, both real:

- the published page and the committed `store/privacy.md` cannot drift, because the page is generated
  from the file on every run;
- the run rewrites `store/listing.json`, which **voids a recorded release approval** — the approval is
  bound to the bytes the owner reviewed. Re-record it with
  `node scripts/approve-release.mjs --slot <slug> --note "<the owner's own words>"`, after the owner
  has actually approved the change. Do not edit the approval record to get past the refusal.

The app's string resource must be updated to the same URL whenever the URL changes; with `editPage`
that is normally unnecessary, since the URL does not change on a text correction it is only necessary
for a slot that had no page, or if the page is ever republished to a new one.

## What is verified, and what is not

**Verified by the publisher, on the outside:** that the recorded URL answers HTTP 200 and that its
body mentions the policy — the `reachable()` fetch, whose failure prints `is not readable`. That is the
whole claim.

**Not verified, and not claimable from a green run:**

- that the page matches the shipped app's real data practices — it matches `store/privacy.md`, which
  is generated from the app's declarations by `scripts/store/render_documents.py`, but a declaration
  that is wrong makes a reachable page that is still wrong;
- that the markdown-to-Telegraph conversion preserved every table cell and every link;
- that the app's string resource carries the same URL — that half is `slot.inAppPolicy`, a different
  sensor, and it reads the app, not the page;
- that the policy is legally adequate or complete for the app's jurisdiction.

`--check` re-tests the same two things and nothing more.

## The residual human act

Hosting is no longer a question put to the owner — the factory publishes. The one thing a program
cannot do is **confirm that the policy still describes the app after the app changed**. A new SDK, a
new permission, a new network call, or a new kind of data collected can all make a previously true
policy false, and the publisher can only prove the URL is reachable and that `store/privacy.md` was
rendered, not that the prose is still accurate.

So the residual human act is a reading, not a task: when the app's data practices change, re-generate
`store/privacy.md`, re-run the publisher, and confirm the policy still says what the app does. The
owner does not host anything, buy anything, or copy a URL anywhere by hand — the factory writes the
URL into the listing and the app tells the builder to mirror it into the resources.

## How it is verified

```bash
node scripts/publish-privacy.mjs --slot <slug>          # publish and record the real URL
node scripts/publish-privacy.mjs --slot <slug> --check  # fetch the recorded URL, publish nothing
node scripts/check-in-app-policy.mjs --slot <slug>      # the app can open the declared policy
node scripts/approve-release.mjs --check --slot <slug>  # the gate the listing edit affects
```

The placeholder half is measured independently: `pack.placeholders` in `scripts/core/assets.mjs`
fails a `privacyPolicyUrl` on a reserved documentation host (`example.com`, `localhost`, a non-HTTPS
scheme), so a pack cannot reach a release pointing at `https://example.com/privacy`
(`scripts/core/assets.mjs:687-707`).
