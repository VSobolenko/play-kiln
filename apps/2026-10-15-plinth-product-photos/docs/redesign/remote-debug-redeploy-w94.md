# W96a remote DEBUG redeploy of the W94 Scene fix — 2026-10-18

## Verdict

- Deploy: **done** (debug only). Production untouched.
- Scene with an **English** prompt (`tent campsite in a pine forest`): **works** — the output is a visibly different
  forest/mountain background.
- Scene with the **Russian** prompt (`джунгли`): **NOT fixed** — the provider returned a re-rendered product on a pale
  grey studio backdrop, not a jungle. The input bytes were not echoed, but the requested scene was ignored.
  Do not report Scene as fully fixed for Russian prompts.

## Deploy

- **MEASURED** — `cd server && npx tsc --noEmit` exit 0; `npx vitest run` → 15 files, 189 tests passed.
- **MEASURED** — `npm run build` exit 0; archive `release-20261018-022958.tgz` SHA-256
  `16beaf0476ba50f219584c234c5b2e36c433b8ebbb63308fcd9ebf64257d771a`; the VPS `sha256sum` printed the same value.
- Commands (host `root@78.111.89.46`, recorded in W73b/W82): `scp` the archive, then over ssh: extract to
  `/opt/plinth-product-photos/releases/20261018-022958`, move `scripts/debug-set-balance.mjs` to the release root
  (W82 location), `npm ci --omit=dev --ignore-scripts=false`, presence checks, `ln -sfn <release>
  /opt/plinth-product-photos/current-debug`, `systemctl restart plinth-product-photos-debug`. The env file was never read
  or printed. Temp files lived under `.kiln/scratch/w96a/`.
- **MEASURED** — VPS after restart: `is-active` → `active`; `readlink -f current-debug` →
  `/opt/plinth-product-photos/releases/20261018-022958`; `readlink -f current` (production) →
  `/opt/plinth-product-photos/releases/20261015-211618` (unchanged).
- **MEASURED** — `curl https://pikupo.com/plinth-debug/health` → HTTP 200,
  `{"ok":true,...,"databaseReachable":true}`.
- **Build marker (new, not just up):** on the VPS `dist/src/registry.js` SHA-256
  `fd00abd5f467d9b3446d4886285a71861022668262061a4abc1d932d51a48171` (W73b was
  `a6051e88…`), `dist/src/providers/fal.js`
  `f8837415ef8dd31afa01c3c4ad91f4e2da3bd4b7c7d1a3c64e10dbfa9dc4295f` (W73b was `02caae8f…`); both equal the local
  build. `grep -c PROVIDER_ECHOED_INPUT dist/src/registry.js` on the VPS → `1` (the new echoed-input rejection).
  The journal showed a fresh start at 23:30:26–27 (VPS time) with the DEV INTEGRITY banner.

## Scene live calls (real, against `https://pikupo.com/plinth-debug`)

Method: script `.kiln/scratch/w96a/scene-live.mjs` — challenge → `/v1/session` with the DEV token `dev:<request hash>`
(a fresh install id, so a new wallet, not the phone's) → `debug-set-balance` to 10 → `PUT /v1/uploads` →
`POST /v1/jobs` op `O-07`, `estimateCredits` 2 → poll → `GET /v1/jobs/:id/result`. Input: a synthetic 1024×1024 JPEG, a
red mug on pure white (`.kiln/scratch/w96a/input.jpg`).

| Prompt | Input sha256 | Output sha256 | Differs | Provenance | Background |
|---|---|---|---|---|---|
| `джунгли` | `51962e23fe7e5f0762439dd905b9305a85609fe6727cea408798304ef0b48c46` | `3a660843f3c2ec03fc8ce4a8f1323b97a77ee2ff5f8ddddc74109d3b6f8b33c4` | yes (hash) | ENHANCED | **Pale grey/off-white studio**, corner RGB ≈ (227,225,230)/(210,208,213)/(227,225,226)/(219,217,218); no jungle. The mug was also re-rendered (orange instead of red). |
| `tent campsite in a pine forest` | `51962e23fe7e5f0762439dd905b9305a85609fe6727cea408798304ef0b48c46` | `40e99d7e8f4c7757dda7ada04cb46eb1289516da92645e560974c5f33686d68f` | yes | ENHANCED | **Different, non-white**: blurred pine forest, mountain, rock; corner RGB (55,66,61)/(76,91,96)/(77,70,58)/(75,77,40). Viewed visually. No tent is visible, but the setting is clearly the requested outdoor scene. |

Both jobs: HTTP 202 → `SUCCEEDED` → result HTTP 200, 1024×1024 JPEG, delivered sha256 equal to the declared sha256.
Credits: 2 reserved per job on a throw-away debug wallet (`d3b9b3e6-…`/the second run's wallet); the phone wallet was
not touched. A second call was used because the first (Russian) failed visually.

Observations (not fixed here): the server prompt embeds the guidance verbatim and says to ignore guidance that disagrees
with the tool purpose; Kontext appears to ignore short Cyrillic guidance. A candidate fix, not tested, is a
server-side RU→EN scene translation or an explicit "background setting: <prompt>" line. Also, the product itself is
regenerated (ENHANCED), not composited from original pixels.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Server tsc and 189 tests were green before deploy. | MEASURED | `npx tsc --noEmit` exit 0; `npx vitest run` 15 files / 189 tests. |
| The debug service runs release `20261018-022958`. | MEASURED | VPS `readlink -f current-debug`; `systemctl is-active` → active. |
| The new code is deployed. | MEASURED | VPS sha256 of `registry.js`/`fal.js` equals local build and differs from W73b; `PROVIDER_ECHOED_INPUT` count 1. |
| Production was not switched. | MEASURED | VPS `readlink -f current` → `20261015-211618`. |
| Debug health is OK. | MEASURED | HTTP 200, `databaseReachable:true`. |
| English Scene prompt yields a different, non-white background. | MEASURED | Hashes above; output visually inspected (forest/mountain); corner RGB dark green/blue. |
| Russian Scene prompt `джунгли` yields a jungle background. | MEASURED (false) | Output hash differs but the image is a pale grey studio; no jungle. |
| Cause of the Russian failure is the model ignoring Cyrillic guidance. | UNVERIFIED | Only one Russian and one English sample; no controlled test. |

## W97a — Russian free-text Scene guidance (2026-10-18)

### Diagnosis

Ran the same O-07 input photo three times against the existing DEBUG service before the fix. All calls used input SHA-256 `792018cd6ca8458d604e39bea999ad1e721962a733c935068433fb963658962e`.

| Prompt | Output SHA-256 | What the returned image shows |
|---|---|---|
| `джунгли` (first call) | `8231e1edf4ad05260e3aacf9111b2ae1fa2226828de47bf99d89fe863f5def9b` | Pale grey studio wall and white surface; no jungle. |
| `джунгли` (repeat) | `7150d9fe1270aa131db8a5a530b9a7a9321eb9ea84099d1e5cf44022606b117a` | Dark grey studio backdrop/tabletop; no jungle. |
| `jungle` (English equivalent) | `63beb13b59f3100289e2466183619dec5243327e4b2ef58dcd38b91f9bad16ab` | Dark green foliage and a leafy/soil floor, visibly a jungle setting. |

I opened and visually compared all three outputs. This controlled sample confirms that the provider path failed to apply these short Cyrillic instructions while applying the equivalent English instruction; it supports language sensitivity for the tested prompts, not a universal claim about every Cyrillic prompt.

### Fix

The server now detects Cyrillic Scene guidance and translates the complete free-text prompt through fal.ai's text-only `fal-ai/any-llm` endpoint using the server's already-configured `FAL_KEY`. English guidance bypasses translation. Only the validated English translation is passed to the image editor; an empty, overlong, or still-Cyrillic translation fails the job instead of silently reverting to a studio default. No translator credential or client secret was added. [fal.ai's endpoint documentation](https://fal.ai/models/fal-ai/any-llm/llms.txt) lists the standard request at `$0.001`; O-07's spend reservation is now `$0.041` (`costMicroUsd: 41000`) to include that translation request.

- Cyrillic detection and translation-result validation: `server/src/prompt.ts:1-13`.
- Server-side translation request and English-only handoff to O-07: `server/src/registry.ts:161-175`.
- fal translation response validation: `server/src/providers/fal.ts:97-103`.
- O-07 translation cost included in spend cap: `server/src/registry.ts:52`.
- Identical-input rejection remains: `server/src/registry.ts:186`; exercised by `server/test/fal-provider.test.ts:244-250`.
- Free-form Russian provider-call coverage: `server/test/fal-provider.test.ts:208-242`; English bypass and invalid-translation coverage: `server/test/prompt.test.ts:4-25`.

### Verification and DEBUG deployment

- **MEASURED** — `cd server && npx tsc --noEmit && npx vitest run` exited 0: 16 test files and 195 tests passed. This includes the live-adapter request-shape tests, English bypass, Russian translation, and echoed-input rejection.
- **MEASURED** — `npm run build` exited 0 and copied all three migrations.
- **MEASURED** — DEBUG-only archive `release-20261018-024828.tgz`, SHA-256 `55371e7cb52ae10eb73c2ec42597da2da298f0161afb59bb8d35cdc5ec206561`; remote `sha256sum -c` returned `OK`.
- **MEASURED** — Remote `npm ci --omit=dev --ignore-scripts=false` completed; npm reported 3 dependency vulnerabilities (1 high, 2 critical). They were not investigated or changed in W97a.
- **MEASURED** — Deployed release id `20261018-024828`; `plinth-product-photos-debug` is `active` and `current-debug` resolves to `/opt/plinth-product-photos/releases/20261018-024828`.
- **MEASURED** — Deployed `registry.js`, `providers/fal.js`, and `prompt.js` SHA-256 values matched the local build: `f157da5c972d907ee9dc66e4a725d20029d5d19f4c8814f5c024a66efe34c37a`, `554937b8737afd35040e4241dadd2f236936a75e2484cbbeb7ef57e9b44fb4cc`, and `e78d51721c03b30e63029797c2722d52d8f60c07be537a5efc6111217d047bbf`.
- **MEASURED** — Post-deploy `https://pikupo.com/plinth-debug/health` returned HTTP 200 with `databaseReachable:true`; `systemctl is-active` returned `active`.
- **MEASURED** — Production was not switched: `/opt/plinth-product-photos/current` remained `/opt/plinth-product-photos/releases/20261015-211618`; production health also returned HTTP 200.

### Live Scene calls after deployment

All calls used the same input photo and SHA-256 `792018cd6ca8458d604e39bea999ad1e721962a733c935068433fb963658962e`. Each returned HTTP 202 → `SUCCEEDED` → result HTTP 200; each downloaded image hash matched the server-declared hash. I opened every returned image.

| Prompt | Input SHA-256 | Output SHA-256 | What the image actually shows / match |
|---|---|---|---|
| `в джунглях` | `792018cd6ca8458d604e39bea999ad1e721962a733c935068433fb963658962e` | `86a6327b78556911e0b8aa7ee5241794afc2cb5528b2a0ccdff04e586bb05730` | Multiple glass tumblers on a mossy rock, with dense dark-green foliage behind them. **Matches a jungle setting; not a pale studio backdrop.** |
| `палаточный лагерь` | `792018cd6ca8458d604e39bea999ad1e721962a733c935068433fb963658962e` | `f1dabada46eb593110b7441eb86bc9eecd4e93b7dbe504f91f398321293a5af5` | Glass tumblers against a dark woodland and bare forest floor. It is outdoors, but **no tent or clear campsite marker is visible; the campsite request is only a partial visual match.** |
| `tent campsite in a pine forest` (English path after deployment) | `792018cd6ca8458d604e39bea999ad1e721962a733c935068433fb963658962e` | `a6cff81109bbf5c2e11aeb85ea4a6ab0e75ba97ad50ab7ea7b133a2aba45ee0c` | Glass tumblers in a wooded clearing with a prominent orange tent behind them. **Matches the English request and confirms that path still works.** |

### W97a claim ledger

| Claim | Label | Evidence |
|---|---|---|
| The pre-fix provider path failed both repeated Cyrillic `джунгли` calls but produced a jungle for English `jungle`. | MEASURED | Three live DEBUG O-07 calls with identical input hash; output hashes and visual observations above. |
| Russian free text is translated server-side before reaching the image model, and English skips translation. | MEASURED | `npx vitest run test/fal-provider.test.ts test/prompt.test.ts`; live DEBUG `в джунглях` returned a visually recognizable jungle; English live call returned a tent scene. |
| Echoed provider bytes remain rejected. | MEASURED | `npx vitest run test/fal-provider.test.ts`; `rejects an image edit that echoes the original bytes` passes with `PROVIDER_ECHOED_INPUT`. |
| Required TypeScript and full server tests pass. | MEASURED | `npx tsc --noEmit && npx vitest run`: exit 0, 16 files / 195 tests. |
| DEBUG runs release `20261018-024828`; its build matches local output. | MEASURED | Remote release symlink, active service, archive checksum, and three remote/local JavaScript SHA-256 matches above. |
| DEBUG health is available and production remains unchanged. | MEASURED | Debug and production health both HTTP 200; production symlink stayed at `20261015-211618`. |
| The deployed `в джунглях` request visually matches. | MEASURED | Opened output SHA `86a6327b78556911e0b8aa7ee5241794afc2cb5528b2a0ccdff04e586bb05730`: dark green foliage and mossy rock. |
| The deployed `палаточный лагерь` request fully matches. | MEASURED (false) | Opened output SHA `f1dabada46eb593110b7441eb86bc9eecd4e93b7dbe504f91f398321293a5af5`: woodland backdrop, but no tent/campsite marker. |
