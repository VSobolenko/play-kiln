# Generator & creative/maker tools — measured pocket evidence (2026-10-02)

Recon evidence behind the recipe proposed in
[`docs/ideation/2026-10-02-generator-and-creative-tools.md`](../ideation/2026-10-02-generator-and-creative-tools.md).
Every table below was produced by the command named beside it, in this session, on this machine.
Nothing here is copied from a prior run, and no quote below is reconstructed: each is printed
verbatim by the command in its section header.

The lane: apps that **make content** — a generated dungeon, encounter, name, world, tile set or
image. The hard rule for the lane is that the value must come from what the app *makes*, never from
a record the user typed in; a record book, log, ledger, register, journal, checklist or CRUD table
is out of scope, as are calculators and unit converters (a closed topic).

## 1. Sensor calibration — the two controls, same invocation as the lane

`node scripts/scan-market.mjs --concurrency 4 --query "dnd dm screen app" … --query "habit tracker" --query "bjj training log"`

| Control query | Expected | Observed (this invocation) | Verdict |
|---|---|---|---|
| `habit tracker` | `fortress` | `fortress` — 29/30 rated, 18/29 rated ≥ 4.5, 4 weak, median 4.50 | **matched** |
| `bjj training log` | `dead-zone` | `dead-zone` — only 9/29 incumbents rated (0.31 < 0.35) | **matched** |

Both controls behave exactly as the repository's recorded hand-readings say they should, so the
lane verdicts below are read as sensor output, not noise. The same invocation scanned **36 queries
in 8,923 ms** (concurrency 4): **10 candidate, 14 mixed, 6 fortress, 6 dead-zone, 0 no-results,
0 errored**.

**The guard is a spot check and the run says so.** Five of the eighteen lane verdicts came back
`partial` on the relevance guard — `dnd dm screen app`, `ttrpg encounter generator`,
`worldbuilding generator`, `character backstory generator` and `plot generator`. Hand classification
was therefore run (§3) before any verdict was believed, which is the discipline
`docs/research/2026-09-24-market-decision.md` §2.1 and §7.4 require.

## 2. The eighteen lane verdicts

`node scripts/scan-market.mjs …` (the same invocation as §1)

| Query | Verdict | Guard | seen | rated | weak | median |
|---|---|---|---:|---:|---:|---:|
| `dnd dm screen app` | candidate | partial | 30 | 22/30 | 13 | 4.10 |
| `random tables rpg` | mixed | ok | 29 | 23/29 | 8 | 4.30 |
| `ttrpg encounter generator` | mixed | partial | 30 | 21/30 | 9 | 4.30 |
| `dungeon map generator` | mixed | ok | 20 | 19/20 | 8 | 4.30 |
| `worldbuilding generator` | candidate | partial | 30 | 27/30 | 14 | 4.10 |
| `character backstory generator` | candidate | partial | 30 | 28/30 | 14 | 4.10 |
| `writing prompt generator` | mixed | ok | 30 | 25/30 | 7 | 4.40 |
| `plot generator` | mixed | partial | 30 | 24/30 | 9 | 4.30 |
| `fantasy name generator` | candidate | ok | 29 | 13/29 | 7 | 4.00 |
| `pixel art editor` | mixed | ok | 30 | 23/30 | 6 | 4.50 |
| `color palette generator` | mixed | ok | 30 | 17/30 | 6 | 4.40 |
| `font pairing tool` | candidate | ok | 30 | 23/30 | 14 | 4.00 |
| `cross stitch pattern maker` | mixed | ok | 30 | 21/30 | 10 | 4.20 |
| `embroidery pattern maker` | candidate | ok | 29 | 15/29 | 10 | 4.00 |
| `sticker maker` | **fortress** | ok | 30 | 25/30 | 3 | 4.60 |
| `logo maker` | **fortress** | ok | 30 | 30/30 | 5 | 4.50 |
| `poster maker` | **fortress** | ok | 30 | 20/30 | 2 | 4.55 |
| `pattern generator wallpaper` | candidate | ok | 30 | 16/30 | 8 | 4.20 |

**Reading, and the refusals it supports.** `logo maker`, `sticker maker` and `poster maker` are
fortresses with the *money* characteristics that make a shelf undislodgeable: 14–18 of the rated
incumbents sit at ≥ 4.5 and only 2–5 are weak (logo 18/30 at ≥ 4.5, 5 weak; sticker 17/25 at ≥ 4.5,
3 weak; poster 14/20 at ≥ 4.5, 2 weak). The image-editing lane (`pixel art editor`,
`color palette generator`, `cross stitch`/`embroidery pattern maker`) leaves a candidate verdict but
requires on-device raster authoring, photo import or a fabric-chart exporter — a materially heavier
build than a content generator. The tabletop-RPG generator lane is where a *candidate* verdict
survived hand classification and where the incumbents are provably paying; §3–§5 measure it.

## 3. Hand classification — the RPG shelves are on the job

`node scripts/scan-titles.mjs --concurrency 3 --query "<q>" --on-job … --off-job …`

| Query | Guard said | Hand job-share | Split |
|---|---|---:|---|
| `ttrpg encounter generator` | partial (15/30, share 0.50) | **29/30 = 0.97** | 29 on the job; the single off-job card is a Free Fire banner, and 0 cards were left unmatched |
| `dnd dm screen app` | partial (15/30, share 0.50) | **21/30 = 0.70** | 21 on the job, 0 off-job, 9 unmatched (D&D Beyond, D&D Session, D&D Session Assistant, SessionKeeper, D&D Spellbook 5e, D&D Helper, D&D 5e Character Keep, and two screen-dimmer apps riding the word "screen") |

So the guard's `partial` on both is the known homonym mechanism (`screen`, `generator`) and not a
polluted shelf: the visible result sets genuinely answer the query. This is the opposite outcome to
the `gas pipe sizing` case in `2026-09-24-market-decision.md` §2.1, where a maximally confident guard
was wrong — and it is why the hand split, not the guard, is what is reported.

## 4. The pocket, enriched by hand — demand is proven, the generator tier is weak

`node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "<q>"` — 0 failures.

**`ttrpg encounter generator`** — 30 cards, top 8 enriched: 3 declare ads, 4 declare IAP, largest
bucket 100K+.

| Package | Title | Rating | Exact reviews | Installs | Ads | IAP | Price |
|---|---|---:|---:|---|---|---|---|
| `com.crixisvision.meb2024` | Encounter Builder D&D 2024 | unrated | — | 500+ | no | no | free |
| `gg.encounter.thelostcards` | Encounter: The Lost Cards | **3.8** | 157 | 10K+ | no | yes | free |
| `uk.co.maddwarf.randomencountergenerator` | Random Encounter Generator | **unrated** | — | 100+ | no | no | free |
| `uk.co.maddwarf.randomencountergeneratorpro` | Random Encounter Generator PRO | **unrated** | — | 5+ | no | no | **paid $1.99** |
| `th.majimesoft.dice_roller` | TTRPG Dice Roller Saving Throw | **unrated** | — | 5K+ | yes | no | free |
| `com.dmtools` | D&D 5e Encounter Keep | **2.5** | 23 | 10K+ | yes | yes | free |
| `com.Tomorrow.DnDEncounterGenerator` | Simple RPG Battle Generator | 4.3 | 13 | 1K+ | no | yes | free |
| `com.Tomorrow.Roll20TabletopRpgMaps` | Virtual Tabletop RPG Manager | 4.3 | 1,220 | 100K+ | yes | yes | free |

Other incumbents visible in the same result set (§3 printing), with the rating the card showed:
`com.alexanderwinn.DungeonRoller` (Dungeon Roller – RPG Generator) **4.2**, `com.axehigh.rpg.encounters`
(RPG Encounter) **3.3**, `com.nationssoftware.endlessrpg` (Endless RPG Solo Tools for D&D) **3.7**,
`social.petrol.playmat` (Playmat: TTRPG RPG VTT Sandbox) **2.1**,
`com.rpgautomails.fatedice` (Fate Dice: Fate RPG) **2.6**,
`com.michaelwu.gamemasterstoolkit5e` **4.5**, `air.com.trapStreetStudios.theCalamus` (Calamus – A TTRPG
Map-Maker) **2.9**.

**The unrated generator tail is evidence of failure, not vacancy:** `Encounter Builder D&D 2024`
(500+), `Random Encounter Generator` (100+, free), `Random Encounter Generator PRO` (5+, paid $1.99),
`TTRPG Dice Roller Saving Throw` (5K+), `Questweaver – D&D 5e DM App` (500+).

**`dnd dm screen app`** — 30 cards, top 8 enriched: 2 declare ads, 6 declare IAP, largest bucket 5M+.

| Package | Title | Rating | Exact reviews | Installs | Ads | IAP | Price |
|---|---|---:|---:|---|---|---|---|
| `fi.kieksi.questweaver` | Questweaver – D&D 5e DM App | **unrated** | — | 500+ | no | yes | free |
| `com.lionsden.gamemaster5` | Game Master 5th Edition | **4.1** | **1,610 (1,611)** | **500K+** | **yes** | yes | free |
| `com.michaelwu.gamemasterstoolkit5e` | Game Master's Toolkit 5e | 4.5 | 496 | 100K+ | no | yes | free |
| `com.fandom.playercompanion` | D&D Beyond (Wizards of the Coast) | **4.6** | **131,000 (130,628)** | **5M+** | no | yes | free |
| `com.bluckapps.donotdisturb` | Simple DND – Do Not Disturb | 4.3 | 308 | 10K+ | no | no | free |
| `com.random.ddnexuss` | DnD Nexus: Ultimate Guide | **3.9** | 21 | 1K+ | yes | yes | free |
| `com.jackischstefan.dndultima` | DnD Ultima | **3.5** | 243 | 50K+ | no | no | free |
| `com.dndcharactersheet.app` | DnD Character Sheet | **3.0** | 103 | 10K+ | no | yes | free |

**`worldbuilding generator`** — 30 cards, top 8 enriched: **0 declare ads, 8 declare IAP**, largest
bucket 100K+.

| Package | Title | Rating | Exact reviews | Installs | Ads | IAP |
|---|---|---:|---:|---|---|---|
| `com.alexanderwinn.Genesis` | Genesis – World Building | **3.7** | 272 | 10K+ | no | yes |
| `com.pandawaystudios.WritersCompanion` | Writer Companion \| World-Building | 4.3 | 888 | 100K+ | no | yes |
| `com.avaton.fantasynamegenerator` | Fantasy World Generator | 4.0 | 88 | 10K+ | no | yes |
| `com.dungeonessmaximus.application` | DunMax: World Building | 4.1 | 14 | 1K+ | no | yes |
| `dev.gamemasterai.fwb` | Fantasy World Builder | **2.4** | 27 | 5K+ | no | yes |
| `com.gmacev.fairytales` | TaleCraft AI: Story Maker | 3.7 | 225 | 10K+ | no | yes |
| `com.lorebible.app` | LoreBible: Worldbuilding App | **unrated** | — | 100+ | no | yes |
| `com.alexanderwinn.Fantasy` | Fantasy Writer's Companion | **2.8** | 22 | 1K+ | no | yes |

**The adjacent, simpler generator proves the shape converts.**
`node scripts/enrich-market.mjs --top 8 --query "fantasy name generator"`:
`com.crystalpeak.fantasynamegenerator` (FaNG – Fantasy Name Generator) is **4.8★ over 7,300
(7,304) reviews at 500K+ installs**, ads + IAP; `com.wavefront.fantasynamegenerator` is 3.9★ over
815 at 100K+, ads + IAP; `com.silver.fantasynamegenerator` is 4.2★ over 605 at 50K+, ads + IAP.
5 of those 8 declare ads and 3 declare IAP.

**The two readings that decide the archetype.** Of the 8 enriched `worldbuilding generator`
incumbents, **8 declare in-app purchases and 0 declare ads** — this shelf's money is a purchase, not
a banner. And the generator tier is weak where it counts: `D&D 5e Encounter Keep` **2.5★/23**,
`Playmat` **2.1★**, `Fate Dice` **2.6★**, `Fantasy World Builder` **2.4★/27**, `Fantasy Writer's
Companion` **2.8★/22**, `RPG Encounter` **3.3★**, `Calamus` **2.9★** — against a proven category
whose tools are rated (`Game Master 5th Edition` 500K+/1,610 reviews at 4.1★,
`Game Master's Toolkit 5e` 100K+/496 at 4.5★).

## 5. What the incumbents' users actually complain about (MEASURED)

`node scripts/scan-complaints.mjs --query "<q>" --top 6 --concurrency 2` — 18 reviews read per shelf.

**`ttrpg encounter generator`** — 18 reviews, **12 critical**, 0 with no text, 3 unclassified.
Class shares of the critical set: **crash 3 (25 %), ads 2 (17 %), broken 2 (17 %), paywall 2 (17 %),
missing-feature 1 (8 %), performance 1 (8 %)**.

Verbatim, exactly as printed:

- `com.axehigh.rpg.encounters` (3.3★, 10K+, 2 critical) — 1★ *"Poorly set up app, very poorly set
  up!! I only see a few scene ideas. And when I refresh it crashes and I get nothing new. Not happy
  at all!!"*; 3★ *"This can be an incredibly useful app because all the tables and entries are there
  and they are awesome. please add a way to get an random result from "* **[the sentence Play
  truncates there is the ask: the tables exist and there is no generator on top of them.**
- `com.dmtools` (2.5★, 10K+, 2 critical) — 1★ *"extremely buggy. app kept crashing during game play,
  would remove player characters from the initiative order randomly, or would tell players that "th"*;
  3★ *"I really want to like your app. It's clean and it was running smooth, other than the fact that
  it kicks people out of the encounters. If you work on g"*
- `com.rpgautomails.fatedice` (2.6★, 10K+, 3 critical, **ads 2**) — 2★ *"Full screen ads every few
  rolls. The sounds are annoying and cannot be disabled."*; 2★ *"Full screen ads pop-up on bad rolls
  are pretty annoying since ads are present in app anyway. Clicked on those trying to reroll a few
  times and uninsta"*; 1★ *"Terrible. Non stop full screen video adds every few rolls."*
- `social.petrol.playmat` (2.1★, 10K+, 3 critical) — 2★ *"Takes ages for you to upload items/tokens to
  the apps \"Inventory\", Only for the app to delete them after a few minutes. No notes/sticky notes
  function"*
- `com.LodestarTeam.rolegateapp` (3.4★, 50K+, 2 critical, **paywall 1**) — 2★ *"Has some cool concepts
  but frustrated how things like macros and sending images, functions considered basic default
  functions in similar apps, cost a"*

**`dnd dm screen app`** — 18 reviews, **12 critical**, 0 with no text, 3 unclassified. Class shares:
**paywall 3 (25 %), account 2 (17 %), broken 2 (17 %), crash 2 (17 %), missing-feature 2 (17 %)**,
plus abandoned 1, notifications 1, regression 1, value 1.

Verbatim, exactly as printed:

- `com.dndcharactersheet.app` (3.0★, 10K+, 3 critical) — 1★ *"this app is very unfinished there are
  only a small handful of races, classes and backgrounds. There are no subclasses whatsoever for any
  of the races "*; 3★ *"Looks great but needs content. I am having issues importing XML's..."*
- `com.vansteinengroentjes.apps.ddfive` (3.3★, 500K+, 3 critical) — 2★ *"I bought the full version and
  it worked for many years. I haven't gotten to game since 2023 and when I logged in, my characters
  were gone, character c"*; 3★ *"As a free application it works well for a quick reference guide. The
  paid portions leave a lot to be desired. The dungeon master's tools seem complete"*
- `com.gnsampayan.dnd5eplayeraio` (2.6★, 1K+, 1 critical) — 1★ *"0/10 cant even create a character it
  might be good if I could make a character but I can barely get past puting in a name for a character
  and then I c"*

**The complaint the wedge must answer, in order of frequency across both shelves (12 + 12 critical
reviews):**

1. **Generation that breaks or loses content** — `crash` and `broken` are **8 of 24** critical
   reviews, and the verbatim failures are specifically on refresh/re-roll (*"when I refresh it crashes
   and I get nothing new"*), on the session clock (*"kept crashing during game play, would remove
   player characters from the initiative order randomly"*), on cloud round-trips (*"Only for the app
   to delete them after a few minutes"*) and on state loss (*"my characters were gone"*).
2. **Thin content and no way to get a result *out* of the tables that exist** — `missing-feature` is
   3 of 24 critical, and the single most useful sentence in either read is the request to *"add a way
   to get an random result from"* the tables that are already there — the tables exist, the generator
   does not. `value` adds another.
3. **Purchase friction** — `paywall` is **5 of 24** critical (*"The purchase options are definitely
   not ready for purchase. I expect much more functionality for paying"*; *"functions considered
   basic default functions in similar apps, cost a"*).
4. **Ads on the play surface** — `ads` is **2 of 24** critical, and both are *"Full screen ads every
   few rolls"* on a **2.6★** dice app.

**One honest exclusion.** `com.vivian.xkspirit` (1.7★) is the only card carrying `account`
complaints on the DM shelf, and its verbatim text — *"i have a vin for nothing , no updates for the
obd"* — shows it is a **car OBD scanner** riding the letters "DND", not a DM tool. Those two
`account` counts are a homonym collision and are **not** used as evidence for any clause. There is
therefore **no measured "forced account" complaint from a genuine tabletop incumbent** in this read.

## 6. The giant, stated rather than buried

`com.fandom.playercompanion` (D&D Beyond) is 4.6★ over **131,000** reviews at 5M+ installs and is the
official platform. It is not the target and it is not a reason to refuse the shelf: it *is* the
demand reading, and it is a cloud account app whose job (rules reference, character management and
publishing) is not the job this recipe takes (offline scene generation). The recipe competes with the
weak generator tier, not with D&D Beyond.

## 7. Honest limits of this evidence

- Install counts are Play's rounded buckets (`100K+` spans 100,000–999,999); every size comparison
  is a floor comparison.
- The review sample is Play's own choice of three per page and `scan-complaints` selected the six
  *weakest-rated* incumbents, so the complaint table describes the *vocabulary* of the pocket, not
  complaint frequency in its population.
- Only the first 8 cards per query were enriched, so a larger incumbent can sit below that cut.
- `seen` is a display-capped page count (~30), never a competitor count.
- `unrated` is Play's own state for an app with too few ratings to show one — treated here as
  evidence of failure, never as vacancy.
- No revenue, retention or conversion reading exists for this or any slot; a declared IAP proves a
  model was implemented, never that it earns.
- The name collision check in §8 of the proposal is a Play **search**, not an exact-title query and
  not trademark clearance.
