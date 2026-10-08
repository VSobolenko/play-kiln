# Proposal — Rook: Encounter Generator (generator & creative-maker lane)

**Date:** 2026-10-02 · **Lane:** GENESOMETHING — generators and creative/maker tools
**Slug:** `rook-encounter-generator` · **Status:** proposed, awaiting the owner's decision at checkpoint 1
**Evidence:** [`docs/research/2026-10-02-generator-and-creative-evidence.md`](../research/2026-10-02-generator-and-creative-evidence.md)

> This proposal is written in the order `docs/ideation/README.md` requires, and every number in it is
> labelled per `.cursor/rules/grounded-claims.mdc`: `MEASURED` (a command was run in this session —
> it is named), `ON RECORD` (a named document states it), `INFERRED`, or `UNVERIFIED` (with the
> command that would decide it).

---

## 1. Category and the job

**Category:** tabletop-RPG game-master tooling — the offline scene/encounter generator.
**The job, in one sentence:** a game master sits down to run a session, needs a place to put the party
*right now*, and wants a page of runnable material rather than a blank page or a table result they
have to interpret themselves.

The app **makes** the content. The user is a creator/player leaving with material they did not have
to author. Nothing in this recipe is "stores what the user typed and shows it back as a list": there
is no record book, log, ledger, register, journal, checklist or CRUD table in it, and no arithmetic
over user-typed numbers. The one optional text field is the campaign *name* used to seed a generator;
it is an input to generation, not a record the app keeps.

## 2. The paying user, what is bought, the price, the moment

- **Paying user:** a game master who already pays for tabletop tooling. **8 of 8** enriched
  `worldbuilding generator` incumbents declare in-app purchases and **0 declare ads** (**MEASURED**,
  `node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "worldbuilding generator"`), the
  category leader at 500K+ installs carries both ads and IAP (`com.lionsden.gamemaster5`, 4.1★,
  1,610 reviews, **MEASURED**), and the adjacent name generator `FaNG` earns 4.8★ over **7,300**
  reviews at **500K+** with ads + IAP (**MEASURED**). This is a hobby that spends money.
- **What is bought:** a **permanent themed content pack** — an authored library of scene parts
  (locations, inhabitants, hooks, complications, rewards, names) in one genre. The base app ships
  with a full, genuinely usable core library; packs add *more worlds*, not *access to the feature*.
- **Price:** **$2.99** for the base unlock of the full library, with additional themed packs at
  **$1.99** each.
- **Moment of payment:** after the first generated scene, gated on **keeping** it — the user presses
  *Keep* (pin the scene to the session board) and that is the purchase moment. Generation itself is
  never interrupted; the pack is what is sold.

## 3. Archetype, and the three rejected with a revenue reason

**Chosen archetype:** `purchase_led_repeat_sales`, capabilities `["billing"]`.

| Archetype | Verdict | Revenue reason |
|---|---|---|
| `purchase_led_repeat_sales` | **chosen** | The product decomposes into permanent themed packs, and the shelf's own money is purchases: **8/8** enriched worldbuilding incumbents declare IAP and **0** declare ads (**MEASURED**). A GM who runs one genre buys a second. |
| `purchase_led_single_unlock` | rejected | One unlock caps the ceiling at one sale per user, and the packs are where the *repeat* is; the base library is deliberately complete, so this would sell a feature the user already has. |
| `ads_led_utility` | rejected | The shelf's users complain about exactly this: **2 of 24** critical reviews across the two RPG shelves are about *"Full screen ads every few rolls"* (`com.rpgautomails.fatedice`, 2.6★), and while 8/8 worldbuilding incumbents monetize by purchase, **0** run ads. An interstitial landing on the roll is the recorded complaint. |
| `purchase_only_no_ads` | rejected | Viable but strictly worse here: it loses the second sale, and the audience is not ad-hostile enough to justify giving up the pack ladder — 3 of 8 encounter incumbents and 2 of 8 DM incumbents do run ads, so ads are tolerable on this shelf. |
| `subscription` | **banned** | Machine-enforced — `subscription` is not in `ARCHETYPES`, so no plan naming it parses. |

**Honest limit of the repeat claim.** The evidence proves the shelf monetizes by IAP (`MEASURED`);
it does **not** prove any incumbent sells *genre packs* specifically. That the repeat unit is a pack
is this proposal's design, labelled **INFERRED**, and it is the one thing the post-ship outcome
reading would confirm or kill.

## 4. The wedge — one sentence, every clause answering a recorded complaint

> **One press returns a whole runnable scene rather than a single table result** — a location, who
> is in it, why the party is there, a complication, a reward and a name, composed from **a large
> authored library**; **every part names the table it came from and re-rolls in place without
> crashing or losing what is already there**; **the scene lives on the device, so nothing is
> uploaded and no server can delete it**; **no advertisement covers a roll**; and the library is
> **bought as permanent themed packs, never rented**.

Each clause is a complaint a named user actually wrote, printed verbatim by
`node scripts/scan-complaints.mjs --query "<q>" --top 6 --concurrency 2` in this session
(**MEASURED**, full text in evidence §5):

| Clause | The complaint it answers |
|---|---|
| "a whole runnable scene rather than a single table result" | *"all the tables and entries are there and they are awesome. please add a way to get an random result from "* (`com.axehigh.rpg.encounters`, 3.3★, 10K+) — the tables exist; there is no generator on top |
| "a large authored library" | *"this app is very unfinished there are only a small handful of races, classes and backgrounds"* (`com.dndcharactersheet.app`, 3.0★, 10K+) |
| "re-rolls in place without crashing or losing what is already there" | *"I only see a few scene ideas. And when I refresh it crashes and I get nothing new."* (same package); *"app kept crashing during game play, would remove player characters from the initiative order randomly"* (`com.dmtools`, 2.5★, 10K+). `crash` + `broken` = **8 of 24** critical across both shelves |
| "lives on the device … nothing is uploaded and no server can delete it" | *"Takes ages for you to upload items/tokens to the apps \"Inventory\", Only for the app to delete them after a few minutes."* (`social.petrol.playmat`, 2.1★, 10K+); *"when I logged in, my characters were gone"* (`com.vansteinengroentjes.apps.ddfive`, 3.3★, 500K+) |
| "no advertisement covers a roll" | *"Full screen ads every few rolls."* (`com.rpgautomails.fatedice`, 2.6★) — `ads` is 2 of 24 critical |
| "bought … never rented" | `paywall` is **5 of 24** critical; *"The purchase options are definitely not ready for purchase. I expect much more functionality for paying"* (`com.vansteinengroentjes.apps.ddfive`) |

Distinctness is stated as **functionality and content**, never naming or colour. There is no
near-duplicate inside our own catalogue: the recorded jobs taken are `teacher-gradebook` (a class
record book) and `lawn-round-log` (a mower's round log), neither of which is a generator, and no slot
in `registry/apps.json` produces generated content (**MEASURED**, `registry/apps.json`).

## 5. Demand evidence

The measured pocket is in full in the evidence file. The four readings the decision rests on:

1. **The category pays, and it is a paying category, not a hole.** `com.fandom.playercompanion`
   (D&D Beyond) is 4.6★ over **131,000** reviews at 5M+ installs; `com.lionsden.gamemaster5` is 500K+
   at 4.1★ over **1,610** reviews; `com.michaelwu.gamemasterstoolkit5e` is 100K+ at 4.5★ over 496
   (**MEASURED**, `enrich-market.mjs`). An occupied shelf is evidence of demand
   (`.cursor/rules/ideation-gate.mdc`) — this is the demand reading, not a warning.
2. **The generator tier inside that paying category is weak enough to displace.** `D&D 5e Encounter
   Keep` **2.5★/23**, `Playmat` **2.1★**, `Fate Dice` **2.6★**, `Calamus` **2.9★**, `Fantasy World
   Builder` **2.4★/27**, `Fantasy Writer's Companion` **2.8★/22**, `RPG Encounter` **3.3★**
   (**MEASURED**). The unrated tail — `Encounter Builder D&D 2024` (500+), `Random Encounter
   Generator` free at 100+, its `PRO` at **$1.99** and 5+, `Questweaver` at 500+ — is an **unrated
   incumbent: evidence of failure, not vacancy** (**MEASURED**).
3. **The hand-classified shelves are on the job.** `scan-titles.mjs` puts `ttrpg encounter generator`
   at **29/30 = 0.97** on-the-job and `dnd dm screen app` at **21/30 = 0.70**, against a guard that
   said `partial` for both (**MEASURED**) — the homonym mechanism, not a polluted shelf.
4. **The adjacent, simpler generator proves the shape converts.** `FaNG – Fantasy Name Generator` is
   **4.8★ over 7,300 (7,304) reviews at 500K+ installs**, ads + IAP (**MEASURED**) — a generator of
   names, a fraction of a scene, on the same wall.

## 6. Unit arithmetic — the division shown

| Line | Value |
|---|---:|
| Base unlock price | $2.99 |
| Play's fee (15% band) | −$0.45 |
| Refund/dispute allowance (~3%) | −$0.09 |
| **Net per base sale** | **$2.45** |
| Additional themed pack | $1.99 → **$1.63** net |
| **Monthly target** | **$200** |
| Sales/month to clear it (base only) | $200 ÷ $2.45 = **82 sales/month** |
| With one pack per three buyers (1.33 units/buyer, blended net) | $2.45 + (0.33 × $1.63) = **$2.99/buyer** → **67 buyers/month** |

**The reading.** 82 sales a month at $2.99 is **≈ 2.7 sales/day**. For contrast, the shelf this
enters contains a 100K+-install app whose `PRO` edition is priced at **$1.99**, and a name generator
with **7,300 reviews at 500K+** (**INFERRED** from the MEASURED install/review figures).
**ON RECORD** at this point: there is no conversion reading for this or any slot — no revenue API
exists — so the target is a threshold the plan is judged against, not a forecast
(`docs/monetization-owner.md`). **UNVERIFIED:** realised conversion; the command that would decide it
is `node scripts/check-outcomes.mjs --slot rook-encounter-generator` against a Play Console reading.

## 7. The promise contract

The full list is [`plans/rook-encounter-generator.differentiators.json`](plans/rook-encounter-generator.differentiators.json)
— **5 promises, 2 `core`, 3 `supporting`, every one starting `deferred`** so `verify.mjs` cannot
pass until each is delivered or dropped in writing. The two `core` promises are *one press returns a
whole runnable scene, every part re-rollable in place* and *a large authored library, offline by
construction*. Nothing in the list is "the app remembers what you typed".

## 8. The name — derived, recorded, rejected alternatives on the record

Full record: [`plans/rook-encounter-generator.naming.json`](plans/rook-encounter-generator.naming.json).
Chosen: **“Rook: Encounter Generator”** — a carrier (*Rook*, a chess piece that holds a line, and a
real bird) plus the typed job noun the shelf actually ranks for. The scored field put **“Rook” at
82/100** across the seven weighted dimensions, ahead of `Crypt: Encounter Generator` (74) and
`Lair: Encounter Generator` (71). The bare head phrase **“Encounter Generator” scored 33/100** and the
bare device noun **“Generator” scored 22/100** as the negative controls, because they spend the
strongest text-ranking field on the exact term the category leader already owns.

Rejected on the record: *Scenario Forge* (a crowded carrier, no search value), *Tablewyrd*
(a stylised misspelling that fails the radio test and looks like a typo), *Rollfire* (evocative but
owns no query), *Crypt* (diluted by crypto apps), *Lair* (homophone risk with "layer"), and the bare
nouns above. Title length is **25/30** in `en` and **20/30** in `ru` ("Rook: Генератор сцен"),
leaving 5 and 10 characters of slack respectively (**MEASURED** by `check-naming.mjs`). A Play
search found **no title that is the exact string "Rook: Encounter Generator"** and no major tabletop
app using the carrier (**MEASURED**, Play search; this is not trademark clearance — **UNVERIFIED**,
and the action that would decide it is a trademark search in the owner's jurisdiction, which no
script here performs).

## 9. Build estimate, permissions, capabilities, server

| Item | Value |
|---|---|
| **Build estimate** | **5–6 agent hours**. The generator is small; the cost is the authored library, and that is the point of the app. This is an estimate for owner information, not a selection criterion. |
| **Permissions** | **None.** No network, no location, no camera, no accounts. Export of a scene goes through `ACTION_SEND` / the Storage Access Framework, which require no runtime permission. |
| **Capabilities** | `["billing"]` — Play Billing for the one-time base unlock and the themed packs. No `ads`, no server capability. |
| **Server** | **None required.** The whole library is authored content shipped in the APK; on-device PRNG only. This is explicitly not a server project. |
| **minSdk / targetSdk** | 26 / 36, per the factory toolchain. |

## 10. Visual direction (declared here, rendered later)

Per `.cursor/rules/visual-identity.mdc` the identity is a **build input**, declared in
`store/studio.json` before the UI is styled. Direction to be settled at stage 1:

- **Mark:** a drawn **standing stone / keep silhouette** — a slot-owned glyph, never a stock icon and
  never a library glyph; it must read at 48 dp and survive the 66 dp adaptive safe zone.
- **Palette:** a **candlelit-dungeon** field — near-black ink blue, warm parchment, ember amber
  reserved for danger/complication, and lichen green for reward — carried into the slot's own
  `Color.kt`, so the store pack cannot drift from the app.
- **Slot-owned components:** a **scene card** whose parts are dealt in as separate strips, a
  **table-provenance chip** naming the table a part came from, a **re-roll affordance per part**, and
  a **dice tray** roll control. The one thing these do that stock Material does not: they render
  promise #4 — *every part is independently re-rollable and shows its source* — as a visible object
  rather than a settings row.
- **UI glyphs** (back arrow, plus, bin, gear) come from **one** licence-cleared professional set
  vendored with provenance; nothing general-purpose is hand-drawn.

## 11. Kill criteria (three numbers)

Declared in the economics record and judged by `scripts/check-outcomes.mjs`: **≥ 2,500 installs**,
**≥ $200 revenue**, and a rating floor, measured over a **90-day** window. Missing them names the kill
command — `node scripts/kiln-status.mjs --kill rook-encounter-generator --reason "<why>"`.

## 12. Candidates refused, and the revenue reason for each

| Candidate | Refused because |
|---|---|
| Logo / sticker / poster maker | **Fortress**, and the fortress is the *money*: `logo maker` 18/30 rated at ≥ 4.5 with only 5 weak; `sticker maker` 17/25 at ≥ 4.5 with 3 weak; `poster maker` 14/20 at ≥ 4.5 with 2 weak — no disposable incumbent and no reachable price pocket (**MEASURED**, §2). |
| Pixel art editor / colour palette generator / embroidery / cross-stitch | Verdicts are candidate-to-mixed, but the value would be craft-workflow-specific rather than a generator; this lane's measured demand and price evidence do not establish a revenue case for it (**MEASURED**, §2). |
| Writing prompt / plot generator | 24–25 of 30 rated, median 4.3–4.4, no weak tier to displace, and prompts compete with free LLM apps at $0 — **no price anyone would pay** (**MEASURED**, §2). |
| Fantasy name generator | `FaNG` at 4.8★/7,300 reviews/500K+ is a strong incumbent and a name is one field of a scene, so the app would be a weaker name generator; the *scene* is the wedge instead (**MEASURED**, §4). |
| Generic "dungeon map generator" | `Calamus` at 2.9★ is weak, but that weakness alone does not establish a paying wedge; it is used here only as evidence that this shelf's generators disappoint (**MEASURED**, §4). |
| A record/log tool of any kind | Explicitly refused by the owner's brief and by this lane's hard rule — the value is not what the user typed. |

## 13. Honest unknowns

- **Conversion is UNVERIFIED.** No revenue reading exists for any slot; the arithmetic in §6 is a
  threshold, not a forecast. Deciding command: `node scripts/check-outcomes.mjs --slot <slug>`.
- **The repeat unit is INFERRED.** The shelf monetizes by IAP (MEASURED); nothing measured says it
  sells *genre packs*. The base unlock plus one pack is the assumption to test first.
- **The name is not cleared.** Play search found no exact-string title and no major tabletop carrier;
  that is not trademark clearance. Deciding action: a trademark search in the owner's jurisdiction.
- **Install buckets are floors** (`100K+` spans 100,000–999,999), so every size comparison is
  conservative in the same direction.
- **The complaint table describes vocabulary, not incidence** — `scan-complaints` selected the six
  weakest-rated incumbents and read three reviews each, so the wedge answers real complaints without
  claiming they are the most common.
- **Build estimate is an estimate.** The library authorship is the dominant cost and the risk is
  that a "large" library is under-delivered; that risk is what the `core` promise and its proof
  artifact exist to catch.
