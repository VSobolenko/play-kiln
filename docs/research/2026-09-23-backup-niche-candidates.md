# Backup niche candidates — hand-verified store evidence (2026-09-23)

> **Question this run tests.** The factory is one decision away from having nothing: the current
> front-runner (`visual schedule autism`) carries an open children's-audience policy question and a
> possible inadequate-differentiation question. This document widens the shortlist with candidates
> that a **small, honest, deterministic app** could serve — a utility with **no ML engine, no
> server, and no regulated claim**, which is exactly the set of properties whose absence killed the
> generic plant idea and then the crop vertical
> (`docs/research/2026-09-23-plant-vertical-evidence.md`).
>
> The query set below was chosen **new**, not repeated from `docs/market-scan-2026-09-23.md`, and
> spread across five archetypes: trade/professional, hobby/enthusiast, narrow accessibility,
> household/measurement, and personal organisation.
>
> **Play search is a fuzzy text match and this repo has already measured it producing garbage**
> (`late blight` → `BE LIGHT: Calm & Focus Boost`, `whitefly treatment` → `WhiteFlag: Mental Health
> Help`). A verdict computed over off-topic cards is not a weak signal, it is a signal about a
> different market. So **every visible card title in this run was read and classified by hand**,
> and every query where the shelf answered a different job than the query is labelled and excluded
> from the conclusions. Two of the fifteen queries failed that test (**section 4**), and a third
> and fourth passed the library's own relevance guard while failing it (**section 2**).

## 1. How this was produced

```bash
# Run 1 — the 12-query screen (every visible card enriched, not just the top 8)
node scripts/scan-plant-vertical.mjs --query "sewing measurements app" --query "nail salon client notes" --query "fence post spacing calculator" --query "board game collection tracker" --query "fishing knot guide" --query "yarn stash inventory" --query "large print crossword puzzles" --query "big button voice recorder" --query "recipe portion calculator" --query "wallpaper quantity calculator" --query "packing list organizer" --query "wedding seating chart planner" --top 30 --concurrency 3 --out .kiln\backup-niches\scan-2026-09-23.md

# Run 2 — confirmation: the three most promising pockets, different phrasing
node scripts\scan-plant-vertical.mjs --query "tailor measurement book" --query "recipe scaling calculator" --query "seating chart maker" --top 30 --concurrency 3 --out .kiln\backup-niches\confirm-2026-09-23.md
```

- **Run 1 exit code: 0.** 12 queries, **328 app detail pages** fetched and parsed, `Failures: None`,
  82,194 ms wall clock, generated `2026-09-23T20:31:25.548Z`.
- **Run 2 exit code: 0.** 3 queries, **87 app detail pages**, `Failures: None`, 49,956 ms,
  generated `2026-09-23T20:35:29.030Z`.
- Raw output (gitignored, cited rather than copied): `.kiln/backup-niches/scan-2026-09-23.md` and
  `.kiln/backup-niches/scan-2026-09-23.log` (run 1, full stdout); `.kiln/backup-niches/confirm-2026-09-23.md`
  and `.kiln/backup-niches/confirm-2026-09-23.log` (run 2).
- Signed-out Play search (`?c=apps`) plus per-app detail pages, `hl=en`, `gl=US`, one session.
  Search cards from `scripts/lib/play-scan.mjs`; per-app facts from `scripts/lib/play-app-detail.mjs`.
  Both libraries and the CLI were used **unmodified**.
- **Every number below is from those two runs.** No figure was carried over from
  `docs/market-scan-2026-09-23.md` or from memory. Every judgement about whether a card answers the
  query is mine and is stated as such.

**Coverage advantage over the reference run.** The plant-vertical run enriched its top 8 of up to 50
visible cards. This run requested `--top 30`, and **for all 15 queries the enriched count equals the
visible count** (30/30, 14/14, 28/28, 29/29), so the hand-classification below covers the **complete
visible result set** of every query, not a top slice. That is the main reason the on-topic shares
here can be trusted further than a spot check of eight cards.

**Reading the columns.** `installs` is Play's rounded bucket (`5M+`), so it is a **floor, not a
count**. `ads` / `iap` are three-valued: `true` = Play showed the badge, `false` = the badge region
was present and did not show that badge, `unknown` = the badge region itself was absent. In both
runs **0 of 415 pages** had a missing badge region, so every `unknown` count below is a genuine `0` —
an observed fact, not a default.

## 2. The guard's lexical share versus a job-level read — the finding that shapes everything below

`play-scan.mjs` attaches an **annotation** (`relevance`) that measures how much of a result set
lexically echoes the query, and states its own directionality claim: token misses push the share
down, so *"`on-topic` is trustworthy while a low share is evidence of pollution rather than proof of
it"*. This run tested that claim by hand on all fifteen queries. **It does not hold in both
directions.**

| query | guard level | guard's own share | my job-level read (hand) | verdict |
| --- | --- | --- | --- | --- |
| `wallpaper quantity calculator` | partial | **29/30 (0.97)** | **4/30 (0.13)** on the wallpapering job | guard's word "part" understates 87% off-job |
| `seating chart maker` | **ok** | ≥0.65, all tokens echoed | **13/29 (0.45)** seating; 16/29 are bar/pie/org chart makers | guard's most-trusted label, half the shelf is another job |
| `nail salon client notes` | partial | 26/30 (0.87) | **4/30 (0.13)** professional salon tools; 18/30 are nail-salon **games** | guard's word "part" understates 60% off-job |
| `fence post spacing calculator` | **ok** | ≥0.65, all tokens echoed | **9/30 (0.30)** fence/spacing; 12/30 are other trade calculators | guard's most-trusted label on a 30% shelf |
| `sewing measurements app` | partial | 16/30 (0.53) | **29/30 (0.97)** same sewing/tailoring trade | guard *under*-alarms a shelf that is almost all the trade |
| `recipe scaling calculator` | ok | ≥0.65 | 13/30 (0.43) portion/scale; 10/30 recipe costing | guard passes, job share under half |
| `recipe portion calculator` | ok | ≥0.65 | 14/30 (0.47) | same |
| `board game collection tracker` | ok | ≥0.65 | 17/30 (0.57) collection trackers | same |
| `fishing knot guide` | ok | ≥0.65 | 18/30 (0.60) knot guides | same |
| `wedding seating chart planner` | ok | ≥0.65 | 11/30 (0.37) seating-specific | same |
| `packing list organizer` | ok | ≥0.65 | **30/30 (1.00)** | guard and hand agree |
| `tailor measurement book` | ok | ≥0.65 | **28/28 (1.00)** | guard and hand agree |
| `yarn stash inventory` | partial | 13/30 (0.43) | 8/30 (0.27) stash-specific | close |
| `large print crossword puzzles` | partial | 14/14 | 14/14 crosswords, **0/14** name large print | guard sees "crossword", hand sees the missing word |
| `big button voice recorder` | partial | 14/14 | 14/14 recorders, **0/14** name big button | same |

**Mechanism, stated from the library's own code.** Two generic shelf tokens are **not** in
`GENERIC_QUERY_TOKENS`: `calculator` and `maker`. Because a query is "on-topic" when **each**
distinctive token is echoed **somewhere** and a card counts when it echoes **any** token, the token
`calculator` makes an entire shelf of unrelated calculators count — which is how
`fence post spacing calculator` earns `ok` at a 30% job share, and how `wallpaper quantity
calculator` reaches a 0.97 lexical share while 77% of the shelf is phone-wallpaper and toy
calculators. Likewise `chart maker` made `seating chart maker` look `ok`.

**What I do with that, and what I did not do.** The guard is an annotation and the CLI was not
modified (as instructed). I used the guard as a *prompt to look harder*, and I treat a query as
usable **only if my hand read of the complete visible shelf says the shelf answers the query's job**.
Two queries failed that test outright (`nail salon client notes`, `wallpaper quantity calculator`);
two more passed the guard but fail it on the job (`fence post spacing calculator`,
`seating chart maker`). **No conclusion below rests on any of those four as a market for the queried
job** — their verdicts are reported because the library computed them, and labelled as describing a
different shelf.

## 3. Per-query table (run 1 = queries 1–12, run 2 = queries 13–15)

Verdict, `seen`, `rated/total`, `weak` (< 4.2★) and median are the ratings-only screen's own output
for these runs. "On-topic (hand)" is my classification of every visible title. "Biggest incumbent"
is the largest install bucket among the enriched cards, with the app at it.

| # | query | verdict | seen | rated/total | weak | median | on-topic (hand) | biggest incumbent |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `sewing measurements app` | **candidate** | 30 | 15/30 | 12 | 3.70 | 29/30 same trade; **11/30 exact job** | 1M+ `j2kapps.patronesbase` **JSK Patrones** (patterns, 3.5★) |
| 2 | `nail salon client notes` | mixed ⚠ | 30 | 15/30 | 7 | 4.20 | **4/30** professional tools; 18/30 games | 10M+ `com.tintash.nailsalon` **Nail Salon 3D** (a game) |
| 3 | `fence post spacing calculator` | mixed ⚠ | 30 | 13/30 | 6 | 4.20 | **9/30** fence/spacing | 10M+ `all.in.one.calculator` **All-In-One Calculator** |
| 4 | `board game collection tracker` | mixed | 30 | 22/30 | 6 | 4.50 | 17/30 collection trackers | 500K+ `com.tuyware.mygamecollection` **My Game Collection** (4.3★) |
| 5 | `fishing knot guide` | mixed | 30 | 19/30 | 4 | 4.40 | 18/30 knot guides | 10M+ `com.fishbrain.app` **Fishbrain** (fishing, off-job) |
| 6 | `yarn stash inventory` | **fortress** | 30 | 18/30 | 3 | 4.55 | 8/30 stash-specific | 1M+ `com.yarnpal.ai` **YarnPal** / `com.row_counter` **My Row Counter** |
| 7 | `large print crossword puzzles` | **fortress** | 14 | 14/14 | 0 | 4.70 | 14/14 crosswords; 0/14 large print | 10M+ Daily Themed Crossword / NYT Games / Crossword Jam / Crossword Master |
| 8 | `big button voice recorder` | mixed | 14 | 14/14 | 1 | 4.45 | 14/14 recorders; 0/14 big button | **1B+** `com.sec.android.app.voicenote` **Samsung Voice Recorder** |
| 9 | `recipe portion calculator` | mixed | 30 | 15/30 | 5 | 4.20 | 14/30 portion/scale | 1M+ `com.supercook.app` **SuperCook** (off-job); in-job 100K+ `Calculation of recipes` (3.4★) |
| 10 | `wallpaper quantity calculator` | mixed ⚠ | 30 | 19/30 | 8 | 4.30 | **4/30** wallpapering | **1B+** `com.miui.calculator` **Calculator** (Xiaomi) |
| 11 | `packing list organizer` | **candidate** | 30 | 11/30 | 7 | 4.00 | **30/30 packing lists** | 1M+ `com.YRH.PackPoint` **PackPoint** (4.4★) |
| 12 | `wedding seating chart planner` | mixed | 30 | 12/30 | 3 | 4.25 | 11/30 seating-specific | 1M+ The Knot / WeddingWire / Joy / Bridebook (planners, off-job) |
| 13 | `tailor measurement book` | **dead-zone** | 28 | 3/28 | 3 | 3.00 | **28/28 tailor job** | 100K+ `com.tailormate` **TailorMate** (3.7★) |
| 14 | `recipe scaling calculator` | mixed | 30 | 11/30 | 4 | 4.20 | 13/30 portion/scale | 100K+ `Calculation of recipes` (3.4★) / `com.exorp.food_cost_calculator` (3.3★) |
| 15 | `seating chart maker` | **dead-zone** ⚠ | 29 | 8/29 | 5 | 3.70 | **13/29** seating; 16/29 chart makers | 500K+ chart makers (off-job); seating: 50K+ `toptable.matthewalner.com` **Table Tailor** (2.6★) |

⚠ = the verdict was computed over a shelf that does not answer the query's job (see section 4).
On-topic (hand) counts are mine; every other column is the run's output.

**Verdict counts, run 1:** 2 `candidate`, 8 `mixed`, 1 `fortress`, 0 `dead-zone`, 0 `no-results`.
**Run 2:** 1 `mixed`, 2 `dead-zone`. Across both runs **only four labels are `candidate` or
`fortress`**, and all four are in-job shelves: query 1 `candidate` (which flips to `dead-zone` under
a rephrase — query 13), query 6 `fortress`, query 7 `fortress`, query 11 `candidate`. Note what that
means for the guard: **none of the four polluted queries produced a strong label** — they came back
`mixed`/`mixed`/`mixed`/`dead-zone` (queries 2, 3, 10, 15), so a strong verdict was not the way the
pollution announced itself. Only the hand read found it, and in two of those four the library's own
text called the shelf `ok` or "partly" relevant.

## 4. Hand-verification: what Play actually returned, query by query

Each query: what Play served, the cards I classified off-job or ambiguous **by name**, and what
conclusion (if any) the query supports.

### 4.1 `sewing measurements app` — the sewing/tailoring shelf (run 1)

Play returned a **genuinely on-trade shelf**: 29 of 30 cards are sewing, tailoring, dressmaking or
pattern-making apps. Only `com.bcimageapp` "Upscale Patterns" (House of Bernard LLC, 10K+) is
unverifiable from its title. Play's fuzzy match worked *in my favour* here — the guard's `partial`
(16/30) is a lexical artefact, since "measurement(s)" appears in few titles while the whole shelf is
the same trade.

But the shelf has two halves, and they matter:
- **Exact job (client measurement records / tailor shop tools), 11 cards:** `com.aeeiee.dressmeasurement`
  Tailor Body Measurements by DM (3.0★, 10K+), `org.tailored` Tailored: Measure clothes (**1.7★**,
  1K+), `com.tailormate` TailorMate - App for Tailors (3.7★, **100K+**), `com.ejastech.tailoric`
  Tailoric: Measurement Records (unrated, 10K+), `com.blaarrow.tackspilormesurment` BS : Tailor
  Measurement App (unrated, 1K+), `com.kaytek.tailor` Tailor Measure (unrated, 10K+),
  `com.taxasworld.fitmeter` Fit Meter (unrated, 500+), `com.ontrails.measurenote` MeasureNote (unrated,
  5K+), `com.indiandevelopershailendra.tailorbookpro` Tailor Book (unrated, 10K+),
  `com.sewtrack.sewtrackapp` SewTrack (unrated, 100+), `com.hydroponicglass.circle_skirt_calculator`
  Circle Skirt Sewing Calculator (unrated, 500+).
- **Content/pattern gallery, the rest:** JSK Patrones (3.5★, **1M+**), Clothing Pattern Designs
  (3.9★, **500K+**), Dress Patterns: Sewing Ideas (3.9★, 500K+), Complete Dress Pattern ideas (4.7★,
  100K+), Clothing, Sewing Studio (4.2★, 50K+), Dressmaking Academy (**1.0★**, 50K+), Sewpal (4.7★,
  10K+), Sewing Patterns (paid $5.99, 4.1★), Sewing Patterns Lite (3.5★), Sew Addict (3.3★),
  Patrondis Pattern Making (**1.1★**, 100K+), Sewing Street, SewCanShe, ShapesAndSense, My Sewing
  Patterns, Upscale Patterns.

**Conclusion — and this is where the `candidate` label needs care.** Of the 12 weak (<4.2★) apps the
verdict rests on, **5 are the actual job** (Tailor Body Measurements 3.0, Tailored 1.7, TailorMate
3.7, Sew Addict 3.3, Patrondis 1.1) and **7 are sewing-content apps** with large buckets
(500K+–1M+) and mediocre ratings (3.5–3.9). So the label is roughly **58% carried by the
pattern-gallery shelf**, not by the measurement job. Reported as-is, with the split named.

### 4.2 `nail salon client notes` — **polluted, no conclusion** (run 1)

Play matched `nail`/`salon` and served the **consumer nail-game shelf**. Only 4 of 30 cards are
professional tools: `com.akynoapps.nailappointment` Nail Tech Appointment: AppNail (4.0★, 10K+),
`net.tapnail` TapNail for Salon / Manicurist (3.9★, 100K+), `com.wowpia.nailshopmanager` Nailva -
Nail Salon Manager (unrated, 50+), `com.naiy.nailsaloncrm` Naly – Nail Salon Management (unrated,
10+).

Named off-job cards (18, all games/kids/try-on/lessons): `com.tripletapgames.nailsalonasmr` Nail
Salon Jam ASMR, `nail.salon.toddler.kids` Nail Salon Games for Kids 2-5 (500K+), `com.casual.nail.salon.simulator`
Nail Salon: ASMR Simulator (**1M+**), `pretty.nail.manicure.salon.makeover.ghaliagames.girlsgames`
Pretty nail & manicure salon (2.9★, 100K+), `com.tintash.nailsalon` Nail Salon 3D (**10M+**),
`com.MA.Magic.Manicure.Game.Your.Nail.Design` Nail Salon Manicure Game, `com.YovoGames.NailSalon3`
Kids nail salon, `abcgames.fun.nailartsalonforgirls` Nail Art Salon for Girls (3.1★),
`com.sz.nailsalon.nailart.diy.makeover` Nail Salon Makeover Games (4.6★, 1M+),
`com.gamesforgirls.nailart.nailsalongame` Nail Salon Game Kids Nail art (1M+),
`br.com.tapps.nailmakeover` My Nail Makeover (**1M+**), `com.casual.simulator.nails.asmr` Acrylic
Nails (100K+), `com.univn.naillessons` Nail Lessons, `com.naildesignstudio` Nail Design Studio (4.7★),
`com.ninevil.nailumi` Nailumi: Salon Nail Try-On, `dev.ehsandevops.nailprettify` NailPrettify,
`net.dsgl.nailpop` Nail POP (1.8★, 50K+). Eight more cannot be classified from title alone:
`com.nailcompany` Nail Company (1K+), `com.zenoti.nailstationz` Nail Station Salon (1K+),
`com.innova.nailitspa` Nail It Spa (1K+), `com.bookingnailist` The Nailist (100+),
`com.tywo.nails` Nails.Studio (100+), `com.nailstudiopro.app` Nail Studio Pro (500+),
`com.ordenygestion.noenails` Noenails (1K+), `com.rita.nailapp` NailApp (100+).

**No conclusion.** The `mixed` verdict (median 4.20) describes the nail-game shelf. It says nothing
about whether nail technicians want client notes — and the fact that only 4 cards even attempt the
professional job, at 50+–100K+ installs, is interesting but is not evidence the scan can compute a
verdict from.

### 4.3 `fence post spacing calculator` — guard `ok`, job share 30% (run 1)

Guard says `ok`; I count **9/30** cards that solve the queried job: `com.fencecalculator` Fence
Calculator (3.0★, 5 reviews, 1K+), `even.spacingcalculator` Even Spacing Calculator (unrated),
`com.fencecalculator.fence.cost` Fence Calculator: Cost & Posts, `com.buildtools.fence_calculator`
Fence Calculator - Estimator, `com.tooltoolnam.fence_calculator` Fence Calculator (10+),
`dev.damonbauer.evenspacing` Even Spacing Calculator (**paid $2.99**), `com.dixonappstudio.smartfencingcalculator`
Fence Calculator: Posts & Cost (5+), `com.sanca.fence` Fence Calculator (10+),
`com.beyondcode.spacing` Equal Spacing Calculator (**paid $2.99**).
Eleven more are other trade calculators (FSWizard ×2,
Construction Master Pro 4.7★/100K+, Feet & Inch 4.6★/1M+, CutOnce, Decking, RedX Roof,
Calculator For Land, Fence Mapster GPS, Fence Manager, Plant Spacing). Ten are unrelated (Fault
Current, Fraction SquareRoot, Off-Grid PV, DualCalc, Omni, All-In-One 10M+, Calculator with
parentheses, True Position GD&T, Dilution Ratio, SoapCalc).

**Conclusion: the on-job pocket is unproven.** Excluding one 3.0★ app with **5 reviews**, **every**
fence/spacing card is unrated — including 8 near-identical apps named "Fence Calculator" at 5+ to
1K+ installs, and two paid $2.99 spacing apps at 50+/100+ installs. Per the library's own rule that
is dead-zone-shaped (people tried, nothing reached rating scale). The `mixed` verdict came from the
calculator shelf. **Do not treat as a candidate.**

### 4.4 `board game collection tracker` — in-job, but entrenched (run 1)

On-job (17/30): `nl.eerko.boardgamestats` Board Game Stats (**4.8★, 100K+, paid $5.99**),
`com.tuyware.mygamecollection` My Game Collection (4.3★, **500K+**), `com.boardgamegeek.app`
BoardGameGeek (4.2★, 100K+), `net.minimap.minimapweb` Minimap (4.5★, 50K+),
`com.tuyware.myboardgamecollection` My Board Game Collection (4.4★, 50K+), `com.boardgame.tracker`,
`com.progrunning.boardgamescompanion` (4.2★, 10K+), `com.solruna.bg_log`, `com.boardgamenow`,
`com.boardgamestracker.app`, `com.tabletop.tools`, `fr.lehovezski.scores` Score Pad (3.5★, 1K+),
`com.lo2lol.BoardGamesCollection`, `com.boardgamecompanion.pro`, `com.trafford.boardrecord` (4.5★,
10K+), `com.ludoya.app` (5K+), `com.playboom.boardgames` Board Club (4.5★, 100K+).
Adjacent (8): board-game timers (`com.sublabgames.boardgametimer` 4.8★,
`com.ferrancatalan.countdowngames` 3.6★, `com.sharedgametimer.twa` 4.6★), video-game collection
trackers (`com.hairyharri.gameye` GAMEYE 4.9★/100K+, `com.collectorz.javamobile.android.games` CLZ
Games 4.5★/100K+), a generic collection tracker (`cn.edu.sdut.wwzmyapplication.release` AnyVault
5.0★), a single-game score counter (`com.digidiced.sxarelease` Forest Shuffle 4.8★) and a publisher
app (`com.boardgametables.ontour` Allplay 3.9★/10K+). Off-job (5, actual board games sold on Play):
`com.direwolfdigital.root` Root ($9.99), `com.handelabra.SpiritIsland` Spirit Island,
`com.digidiced.ixsrelease` Isle of Skye, `com.marmalade.cluedo2` Clue,
`com.jwhenriksen.boardgames` Board Games - Classics.

**Conclusion: in-job but fortress-shaped.** 0.55 of rated in-job apps are ≥4.5★, the reference
database (BoardGameGeek) has its own official app at 100K+, a paid $5.99 tracker holds 4.8★/100K+,
and the 500K+ leader is 4.3★. The `mixed` verdict is honest but the pocket is occupied by loved
apps. **Kill.**

### 4.5 `fishing knot guide` — in-job, but a loved fortress (run 1)

18/30 are knot guides, and they are strong: `com.MpFish.KnotsLite` Fishing Knots (**4.6★, 1M+**),
`de.perished.android.apps.fishingknots` Fishing Knots (**4.7★, 1M+**), `com.neptuns.usefulfishingknots`
Useful Fishing Knots (4.4★, 500K+), `com.neptuns.usefulknots` Useful Knots (4.4★, 1M+),
`org.vinczu.ultimatefishingknots` Ultimate Fishing Knots (4.4★, 100K+),
`com.bearessentials.knotIQ` Knot IQ (**4.9★, paid $4.99**, 10K+), `com.neptuns.usefulfishingknotspro`
(paid $1.99, 4.4★), `com.MpFish.Knots` (paid $11.99, **2.8★**), plus ten small/unrated guides.
The other 12 are fishing apps, not knots (`com.fishbrain.app` Fishbrain 10M+, `com.memsgroupinc.fishbox`
Fishbox 1M+, `com.fishangler.fishangler` 1M+, `com.antonnikitin.solunarforecast` 1M+, onWater,
Fishbuddy, Anglers' Log, SnapHatch, Fly Fishing Guide, Fishing Spots, Fishing Guide & Spots,
Fishing Guide: Tips & Gear).

**Conclusion: kill.** The exact job is served by **two 1M+ apps at 4.6–4.7★** plus a 4.9★ paid app,
and the content is illustration/video — content cost with a free ad-supported leader. A better knot
guide is not a plausible wedge.

### 4.6 `yarn stash inventory` — fortress shelf, and the job is owned off-Play (run 1)

Only 8/30 cards are stash/inventory apps, and they are **tiny**:
`com.blewettproductsystems.bpi_craft_stash_inventory` Yarn & Fabric Stash Inventory
(**1+ installs**), `com.yarnstash.tracker` Yarn Stash Tracker (10+), `com.mindstorm.yarncollect`
Yarn Collect (10+), `com.ciarakw.yarncat` YarnCat (100+), `com.YarnStasher.YarnStasher` Yarn Stasher
(500+), `com.itbruderhofer.skeincheck` SkeinCheck: Yarn Planner (**0+**),
`lcom.hookingcutecreations.YarnHoarderv1` Yarn Hoarder (5.0★ but **11 reviews**, 1K+),
`com.hugetoothstudios.stashhub` Stash Hub (4.1★, 10K+, sewing fabric), and **`com.luxx.Stash2GoII`
Stash2Go II – Ravelry App (4.5★, 10K+)** — a client for **Ravelry**.

The other 17 are the knitting/crochet tool shelf and it is a fortress (verdict: 13/18 rated ≥4.5★):
`com.row_counter` My Row Counter (4.8★, **1M+**), `com.yarnpal.ai` YarnPal (4.7★, 1M+),
`com.rfsp.pocketcrochet` Pocket Crochet (4.7★, 100K+), `com.ribblr.app` Ribblr (4.5★, 100K+),
`com.woolsyapp` Woolsy (4.5★, 10K+), `com.moniq.crochet.knit.rowcounter` (4.7★, 10K+), plus
`com.knitandnote.android` Knitandnote (**3.3★**, 50K+) and `com.knitandnote`-adjacent
`fr.crochetou` Crochet Genius (4.3★, 100K+). Five are off-job entirely (games/communities:
`com.unicogames.yarnation` Yarnation, `com.miquelcms.homeorganizer` Find my stuff, `com.stashapp`
Stash: Home Inventory, `tv.uscreen.yarnhivecommunity` TL Yarn Crafts, `com.idefforts.yarn_market`
Yarn Market).

**Conclusion: kill.** The broad shelf is a fortress, and the exact job — a yarn stash database — is
already served **off-Play** by Ravelry, whose client is the only in-job app with traction (4.5★,
10K+). **UNVERIFIED:** Ravelry itself was not fetched; the claim that Ravelry is the real incumbent
rests on the app's own title ("Stash2Go II – Ravelry App").

### 4.7 `large print crossword puzzles` — fortress, and the accessibility word is unserved (run 1)

All **14/14** cards are crossword apps and **0/14** mention large print: Daily Themed Crossword
(4.8★, **10M+**), NYT Games (4.8★, 10M+), Crossword Jam (4.6★, 10M+), Crossword Master (4.8★,
10M+), Crossword Quest (4.7★, 10M+), English Crossword Puzzle (4.7★, 5M+), Crossword Puzzle
Redstone (4.8★, 5M+), Crossword Puzzle Explorer (4.8★, 5M+), Wordgrams (4.6★, 1M+), Crossword Go!
(4.9★, 1M+), The Big Crossword (4.6★, 100K+), Crossword Puzzles (4.6★, 100K+), Daily POP (4.2★,
100K+), Easy Crossword for Beginner (4.5★, 50K+). Verdict: fortress, 13/14 ≥4.5★, **0/14 weak**,
and 14/14 declare in-app purchases.

**Conclusion: kill.** This is a daily-content business (server + editorial) owned by 10M+ publishers,
the opposite of a no-server utility, and it sits inside the accessibility aisle — a framing that
would itself need a careful claim.

### 4.8 `big button voice recorder` — fortress of OEM and 50M+ apps (run 1)

All **14/14** are voice recorders, **0/14** mention big buttons: **`com.sec.android.app.voicenote`
Samsung Voice Recorder (4.0★, 1B+ installs, preinstalled on Samsung devices)**, Voice Recorder by
"quality apps" (4.9★, **50M+**), Easy Voice Recorder (4.7★, 50M+), Smart Voice Recorder (4.6★,
10M+), Voice Recorder & Voice Memos ×2 (4.6★/4.8★, 10M+ each), Simple Voice Recorder (4.4★, 5M+),
Voice Recorder: Memos & Audio (4.3★, 5M+), plus 1M+ apps (Voice Recorder & Audio Editor 4.8★,
XVoice 4.3★, Voice Recorder & Transcriber 4.4★) and 100K+ apps.

**Conclusion: kill.** Recording voice is a preinstalled OS capability and the shelf is dominated by
1B+/50M+ apps: **two more at 50M+** (Voice Recorder by "quality apps" 4.9★, Easy Voice Recorder
4.7★) **and three at 10M+** (Smart Voice Recorder 4.6★, Voice Recorder & Voice Memos 4.6★ and 4.8★).
"Big button" is unserved in titles, but a button size is not a wedge against a preinstalled recorder.

### 4.9 `recipe portion calculator` (run 1) and `recipe scaling calculator` (run 2) — the same pocket, twice

**Run 1** (`recipe portion calculator`): 14/30 cards do portioning/scaling/conversion
(`com.nancel.kitchenconverter` Kitchen converter 4.6★/10K+, `com.umitapp.cookingconverter` Cooking
Unit Converter 4.4★/10K+, `com.artemsuharev.timbu.myapplicationnew` **Calculation of recipes 3.4★/100K+**,
`com.mykitchencalculator.kitchencalculator` Recipe Calculator **paid $1.99**/4.2★/10K+,
`com.cookingconversions` Cooking Conversions 4.1★/10K+, `ch.tastelab.converter` Useful Units 3.3★/5K+,
plus eight unrated micro-apps: Recipe Portion Calculator **1+**, Recipe Measure Converter **10+**,
Baking Conversion Calculators 50+, Kitchen Calculator 1K+, Recipe Calculator 1K+, Kitchen Converter:
Bake & Cook 500+, Cooking Conversion Calculator 100+, Recipe Calculator (blackstar) 1K+).
13 cards are the adjacent recipe-**costing** job (Food Cost Calculator, Recipe Cost Calculator
100K+, Cookara, KitchenCost, Food Cost & Recipe, Simple Recipe Cost Calculator, Zenbyte, F98K,
costmydish, escandallo, Food Cost Calculator & Recipe, Recipe Price Calculator, Recipe Scaler).
3 are unrelated (SuperCook recipe generator 1M+, AI Recipe Genie, AI Recipe Generator).

**Run 2** (`recipe scaling calculator`) returned **largely the same pocket**: 13/30 portion/scale,
10/30 recipe costing, 7/30 unrelated (instrument scaling, model scale 100K+, RedX construction,
RedX stairs, pace calculator, concrete calc, "Scale calculator"). The same apps recur — Kitchen
converter 4.6★, Calculation of recipes 3.4★/100K+, Cooking Unit Converter 4.4★, Recipe Calculator
paid $1.99, Recipe Portion Calculator (1+), Kitchen Converter: Bake & Cook, Baking Conversion
Calculators, Kitchen Calculator, Recipe Scale Converter, Recipe Scaler $—.

**Conclusion — the most stable pocket in this document.** Two different phrasings return the same
small shelf, both `mixed`, with the same structure: **one weak app at 100K+ scale**
(`Calculation of recipes`, 3.4★) and in the adjacent cluster two more (`Food Cost Calculator`
3.3★/100K+, `Recipe Cost Calculator` 4.2★/100K+), a handful of 4.4–4.6★ apps stuck at only 10K+
(Kitchen converter, Cooking Unit Converter), a $1.99 paid incumbent at 4.2★/10K+ with **70 ratings**,
and ~20 unrated micro-apps including two at **1+ installs**. The job is pure arithmetic.

### 4.10 `wallpaper quantity calculator` — **polluted, no conclusion** (run 1)

Guard says `partial` with a **0.97 lexical share** (29/30 titles echo `wallpaper` or `calculator`).
My read: only **4/30** cards compute wallpaper quantities — `com.wallpapercalc` Wallpaper Calculator
(100+), `com.application.wallfashion.wallfashionwallpapercalculator` Wallpaper Roll Calculator (5K+),
`appinventor.ai_1954gellis.wallpaper_calculator` Wallpaper Calculator (1K+), `com.sanca.wallpaper`
Wallpaper Calculator (5+) — **all four unrated**.

Named off-job cards: phone-wallpaper apps (`io.wallpaperengine.weclient` Wallpaper Engine 5M+,
`de.j4velin.wallpaperChanger` Wallpaper Changer 5M+, `bazaart.me.patternator` Patternator 5M+,
`com.Buddy.countdown` Countdown Live Wallpaper, `com.my.life.calendar` / `ai.mimos.lifecalendar` /
`com.sumanmsoft.dotWallP` Life Calendar Wallpaper ×3) and **generic calculators**
(`com.miui.calculator` Calculator **1B+**, `all.in.one.calculator` 10M+, `com.tip.calculator.unit.converter.android`
Calculator Launcher 1M+, `info.woodsmall.calculator` 1M+, `com.tomhogenkamp.personalcalc`,
`com.stan.calculator`, `com.shilenkov.pink_calculator`, `com.syualikesapple.app.calculator`,
`jp.co.conduits.calcbas` CASIO Style 4.7★/500K+, `webfamous.mx...`, `com.applife.cutecalculator`,
`com.naladimarth.nala_calculator`, `ca.vetal.widgetcalculator`, `com.ratolab.skin.calculator`,
`com.calculadoracdk`, plus `com.omnicalculator.app`). Only 3 cards are surface-area calculators in
the right family (`com.whiteflower10001.Tile_Calculator` and `in.pravion.tilescalculator` Floor &
Wall Tiles, `evanwinograd.archcalc` Feet & Inch 1M+).

**No conclusion.** The `mixed` verdict (median 4.30) is a statement about phone wallpapers and toy
calculators. The four real wallpaper calculators being unrated is a fact, but four cards is not a
market.

### 4.11 `packing list organizer` — the cleanest query of the twelve (run 1)

**30/30 cards are travel packing-list apps.** No off-topic card at all. On-job ratings:
`com.YRH.PackPoint` PackPoint (4.4★, **1M+**), `com.packparty.app` PackParty (4.9★, 5K+),
`com.createo.packteo` PackTeo (4.2★, 100K+), `com.pixelbrewstudio.quickpack` QuickPack (4.4★, 10K+),
`com.ouji.packmate` PackMate (4.1★, 10K+), `com.dotnetideas.packinglist` Packing List (4.0★, 100K+),
`com.dotnetideas.packinglist2` Packing List 2 (3.9★, 1K+), `com.tnx.packed` Packlist (3.9★, 50K+),
`com.acquasys.smartpack` SmartPack (4.0★, 10K+), `com.slbdeveloper.packinglist` Simple Travel
Packing List (4.0★, 10K+), `com.dotnetideas.packinglistfull` Packing List - Full (**paid $2.99**,
3.7★, 10K+). The remaining 19 are unrated (5+ to 10K+ installs), including two "AI Packing List"
apps (`com.bitjust.packwise.app`, `com.cogniq.packpilot`).

**Conclusion: `candidate` verdict, fully verified-relevant — but on a knife edge.** `ratedShare` is
11/30 = **0.3667** against a `DEMAND_PROVEN_SHARE` threshold of **0.35**: one more unrated card and
the verdict flips to `dead-zone`. The weak apps are real and reachable (3.7–4.0★ at 10K+–100K+
installs, plus a $2.99 paid app at 3.7★), but 19/30 unrated apps of near-identical function is the
signature of a shelf of clones, not of unmet demand. Also note: the bare query is genuinely
zero-noise, which makes it the cheapest pocket here to re-measure.

### 4.12 `wedding seating chart planner` (run 1) and `seating chart maker` (run 2) — seating is unproven, twice

**Run 1**: 11/30 cards are seating-specific and **10 of those 11 are unrated**:
`toptable.matthewalner.com` Table Tailor: Seating Planner (**2.6★, 50K+** — the only rated one),
`com.anniefalkllc.seated` Seated (5K+), `com.weddingseat.app` Wedding Seat (100+),
`com.sidsoftsolutions.seatingplan` Seating Planner (500+), `pips.seatsychartplanner` Seatsy (50+),
`com.play3rs.seatingchart` Seating Chart Maker (10+), `com.syanac.trammel` Wedding Seating Chart:
Compare (5+), `app.brindis.seating` Brindis (100+), `com.snaptool.snapseat` SnapSeat (500+),
`com.seatplanner.app` Easy Seat Plan (1K+), `com.appfactory.chartseats` ChartSeats (50+). The other
19 are full wedding planners, i.e. a different (and much stronger) job: The Knot (4.7★, 1M+),
WeddingWire (4.7★, 1M+), Joy (4.4★, 1M+), Bridebook (4.2★, 1M+), MyWed (4.4★, 1M+), Wedsly (4.2★,
100K+), Weddi (4.1★, 100K+), plus 12 small/unrated planners and `com.wedding.planner.guest.list`
WedGuest (50K+, guest lists).

**Run 2** (`seating chart maker`) — guard says `ok`, but **16/29 cards are bar/pie/org/Gantt chart
makers**: Chart Maker Pro (4.3★, 500K+), Chart Maker: Graphs and charts (3.5★, 500K+), EZ Charts
(3.9★, 100K+), Bar Chart Maker (4.9★, 100K+), Chart & Graph Maker (4.3★, 10K+), Fast chart (3.5★,
50K+), Easy Chart Maker (3.4★, 1K+), Org Chart Maker, Organization Chart Builder, Gantt Chart Maker,
Bar Chart Maker ×2, Chartik, Chart Maker ×3, Chart Maker: Pie Bar & Line. The **13 seating cards**
are: Table Tailor (2.6★, 50K+ — again the only rated one), SnapSeat, Seated, Seating Chart Maker
(10+), Seatsy, ChartSeats, `com.naoyaono.seat_arrangement` Easy Seating Arrangement (10K+),
`com.robcowen.msp` Seating Plan (500+), Easy Seat Plan, `com.seatuationship.twa` Seatuationship (10+),
`com.sidsoftsolutions.seatingplan` (500+), `info.camposha.seating` Seating Plan Studio (**1+**),
`com.biddulph.classroomplanner` Teacher Planner: Seating Chart (1K+).

**Conclusion: kill.** Two phrasings agree on the thing that matters: across ~24 distinct
seating-specific apps, **exactly one carries a rating — Table Tailor at 2.6★ (50K+)**. Per the
library's rule that is unproven demand (run 2 returned `dead-zone` outright), and the seating job is
an event-sized, one-shot need that four 1M+ wedding platforms appear to bundle for free.
**UNVERIFIED:** that The Knot / WeddingWire / Joy / Bridebook actually include a seating chart —
inferred from their "Wedding Planner" titles, not observed.

### 4.13 `tailor measurement book` (run 2) — the rephrase that flips the best candidate

**28/28 cards are tailor-job apps.** No pollution at all — and hardly any traction:
Tailor Book - Easy & Secure (10K+), Tailor Body Measurements by DM (**3.0★**, 10K+), Tailoric:
Measurement Records (10K+), TailorMate (**3.7★**, **100K+**), Tailor Assist (1K+), Tailor Book:
Tailor Diary (10+), Tailor Book (TWINSPIDER, 10K+), Tailored: Measure clothes (**1.7★**, 1K+),
My Tailor App (5K+), Tailor Master (1K+), BS : Tailor Measurement App (1K+), Fast Tailor (500+),
SewTrack (100+), Tailor Measure (10K+), Measurement Book (Akshay C P, 10+), TailorCare (50+),
TailorNote: Tailor Book & CRM (100+), Smart Tailor (1K+), Tailora (500+), Stylo Tailor (100+),
MJ Tailor - Order & Measurement (100+), Tailor Manager (10+), Smart Tailors (500+),
Measurement Book (Xorek, 10K+), Tailora - Smart Tailor Mgt (100+), Tailor bro (10K+),
TailorBook - Darzi Measurement (100+), ezyTailor (**50K+**).

**Verdict `dead-zone`: 3/28 rated (0.11).** And **all three rated apps are weak** (3.0, 3.7, 1.7 —
`weakShare` = 1.0). Monetisation is nearly absent: **4/28 declare ads, 7/28 declare IAP, 20/28
declare neither.**

**Conclusion: this contradicts query 1, and the contradiction is the finding.** Same job, same
shelf family; `sewing measurements app` returns `candidate` because it drags in rated
pattern-gallery apps with 100K+–1M+ buckets, while `tailor measurement book` isolates the job and
shows **25 of 28 apps below Play's rating threshold, and every rated one weak**. Read strictly, the
second query is stronger evidence about the job: many installs (10K+–100K+ on seven apps), almost no
ratings, and no app anyone rated highly. That is the "people tried and nobody got traction" pattern
the library calls a dead zone — see section 6.3.

## 5. Ads / IAP split

Totals are the runs' own output over every enriched card. "On-job, hand-counted" is my count from
the per-app `containsAds` / `inAppPurchases` columns restricted to the cards that actually do the
query's job — the figure that is attributable to the niche, which the query-level totals are **not**
when a query is polluted.

**Run 1 — all 12 queries combined (328 apps).** `unknown` is 0 for both fields.

| signal | declared | declared absent |
| --- | --- | --- |
| contains ads | **160 (48.8%)** | 168 |
| in-app purchases | **199 (60.7%)** | 129 |

- ads and no IAP: 58 · IAP and no ads: 97 · both: 102 · **neither: 71**
- free 314 / paid 14 (4.3%) / price unknown 0

**Run 2 — all 3 queries combined (87 apps).** ads declared 36 (41.4%), IAP declared 42 (48.3%);
both 23, ads-only 13, IAP-only 19, neither 32; free 86 / paid 1.

**Per query, and the attributable on-job figure.** Query-level numbers from the runs; on-job numbers
hand-counted by me from the per-app columns.

| query | ads (query) | IAP (query) | ads, on-job | IAP, on-job | what it says |
| --- | --- | --- | --- | --- | --- |
| `sewing measurements app` | 13/30 | 12/30 | **4/11 (36%)** | **6/11 (55%)** | exact-job tools lean IAP; 4/11 declare neither |
| `tailor measurement book` | 4/28 | 7/28 | 4/28 (14%) | 7/28 (25%) | **20/28 declare neither** — an unmonetised shelf |
| `packing list organizer` | 12/30 | 19/30 | 12/30 (40%) | **19/30 (63%)** | IAP is the shelf norm, ads secondary |
| `recipe portion calculator` | 14/30 | 22/30 | 9/14 (64%) | 9/14 (64%) | both, heavily — ad-heavy arithmetic shelf |
| `recipe scaling calculator` | 17/30 | 17/30 | **10/13 (77%)** | 8/13 (62%) | corroborates: ads dominate the on-job set |
| `fence post spacing calculator` | 15/30 | 14/30 | **6/9 (67%)** | 3/9 (33%) | on-job pocket is ad-monetised (+2 paid $2.99) |
| `wedding seating chart planner` | 3/30 | 19/30 | **1/11 (9%)** | **7/11 (64%)** | seating apps are almost ad-free, mostly IAP |
| `board game collection tracker` | 9/30 | 16/30 | — | — | IAP-leaning shelf |
| `fishing knot guide` | 18/30 | 18/30 | — | — | even split; leader is ad-supported |
| `yarn stash inventory` | 14/30 | 25/30 | — | — | IAP-leaning (25/30) |
| `large print crossword puzzles` | 13/14 | **14/14** | — | — | every crossword app declares IAP |
| `big button voice recorder` | 12/14 | 13/14 | — | — | both, on a fortress shelf |
| `nail salon client notes` ⚠ | 16/30 | 15/30 | — | — | **not attributable** (nail-game shelf) |
| `wallpaper quantity calculator` ⚠ | 21/30 | 12/30 | — | — | **not attributable** (wallpaper/calculator shelf) |
| `seating chart maker` ⚠ | 15/29 | 18/29 | — | — | **not attributable** (chart-maker shelf) |

**What this decides.** Across both runs IAP-declared (60.7% / 48.3%) is **at least as common as
ads-declared** (48.8% / 41.4%), and 71 + 32 apps declare neither — so a free-with-IAP model matches
the majority of these shelves and a free-with-ads model only matches the arithmetic ones (recipe
on-job 64–77%, fence on-job 67%). Two hard caveats: a declared badge proves a model was
**implemented**, not that it earns (97 + 19 apps are IAP-only with, in several cases, 10+–500+
installs), and **the paid signal is tiny** — 14 paid apps in 328 on run 1, and in the exact-job
pockets the paid entrants are unrated and sub-1K+ (two $2.99 spacing calculators at 100+ and 50+;
a $2.99 packing list at 3.7★). The best-attested paid data point anywhere in these runs is
`nl.eerko.boardgamestats` (paid $5.99, 4.8★, 100K+) — and that is a fortress shelf.

## 6. Ranked shortlist (3, deliberately not padded)

Ranked by how much of the evidence survives hand-verification, then by how cheaply the thing that
would kill it can be tested. Two slots are left **empty on purpose**: eight of the twelve queries in
run 1 produced either a fortress, a polluted shelf, or an unproven pocket, and padding this list
would misstate the evidence (`AGENTS.md`: a false GO is the worst output in the system).

### 6.1 `recipe portion / scaling calculator` — **best-supported candidate** (queries 9 + 14)

One line: a deterministic kitchen-arithmetic utility whose largest on-job incumbent sits at
**100K+ installs on 3.4★**, with two corroborating phrasings, clear monetisation in the pocket, and
no ML, no server, no regulated claim.

Evidence: two phrasings return the same pocket (`mixed` both times, 14/30 and 13/30 on-job);
`Calculation of recipes` 3.4★/100K+ and, adjacently, `Food Cost Calculator` 3.3★/100K+ and
`Recipe Cost Calculator` 4.2★/100K+ are reachable weak incumbents; the decent apps are stuck small
(Kitchen converter 4.6★/**10K+**, Cooking Unit Converter 4.4★/10K+); the paid incumbent is $1.99 at
4.2★ with **70 ratings**; ~20 unrated micro-apps include two at **1+ installs**; the on-job pocket
declares ads 64–77% and IAP 62–64%.

- **Riskiest assumption (single):** that the job is **not already absorbed by a free web
  calculator** — i.e. that a user will install and keep an app for arithmetic that
  `mykitchencalculator.com` (whose own app is one of the incumbents) or a Google answer does
  instantly. If that is false, no app wins here regardless of quality.
- **Cheapest test (hours, no code):** (1) check whether a free web recipe scaler is the top organic
  result for the query; (2) read the 1–3★ reviews of `Calculation of recipes` (263 reviews) and
  `Food Cost Calculator` (454 reviews) and classify the complaints as *fixable-feature* or
  *"this is just arithmetic"*. Both are already public and need no build.
- Honest caveat: even if it passes, the ceiling looks small — the best on-job app in the pocket is a
  10K+ app.

### 6.2 `packing list organizer` — **cleanest evidence, commodity risk** (query 11)

One line: the only query of the twelve with **100% on-job relevance**, a verified `candidate`
verdict, and a reachable weak tier (3.7–4.0★ at 10K+–100K+, plus a $2.99 paid app at 3.7★) — on a
job that is only a list.

Evidence: 30/30 packing-list apps, zero noise; `candidate` at 7/11 rated weak, median 4.00; leader
PackPoint 4.4★/1M+; strongest is 4.9★ with only 5K+ installs; 19/30 declare IAP.
**Caveats that shrink it:** `ratedShare` is 11/30 = 0.3667 against a 0.35 threshold (one card from
`dead-zone`), 19/30 apps are unrated near-identical clones, and the whole job is achievable with the
OS notes app.

- **Riskiest assumption (single):** that a packing checklist can **hold paying users** when 30 free
  clones and a free notes app already do the same thing — i.e. willingness to pay, not demand.
- **Cheapest test:** the paid data point already exists in the run (`com.dotnetideas.packinglistfull`,
  $2.99, 10K+, 3.7★) — read whether its paying reviewers name what the free version lacks; and read
  PackPoint's 1–3★ reviews for the same. A price-point/paywall check on the two paid apps is
  cheaper than a build.

### 6.3 `tailor / sewing measurement records` — **highest upside shape, lowest confidence** (queries 1 + 13)

One line: a repeat-use, offline record book (clients × measurements) for tailors — the best *shape*
in this document (records beat one-shot calculators on retention; no ML, no server, no regulated
claim) — but its store verdict **flips with phrasing**, so it is not a GO.

Evidence **for**: `sewing measurements app` returned `candidate` (12/15 rated weak, median 3.70) on
a shelf that is 29/30 the same trade, with the exact job containing real weak apps (Tailored 1.7★,
Tailor Body Measurements 3.0★, TailorMate 3.7★/100K+, Patrondis 1.1★/100K+).
Evidence **against**: `tailor measurement book` returned `dead-zone` (3/28 rated, 0.11) on a
**28/28 on-job shelf** — seven apps at 10K+–100K+ installs, only three ratings, **all weak**
(1.7/3.0/3.7), and **20/28 declaring neither ads nor IAP**. Two of the three legs of a business
(demand, payment) are unverified here, and the first query's label is ~58% carried by
pattern-gallery apps that are not the job.

- **Riskiest assumption (single):** that the weak ratings mean **bad tooling a new app can fix**,
  rather than that tailors do not want to keep measurement records on a phone at all (paper is free,
  durable and already in the shop). If it is the latter, the whole job is a dead zone — which is
  exactly what the second query's rule says.
- **Cheapest test — the cheapest in this document:** read the 1–3★ reviews of `com.tailormate`
  TailorMate (3.7★, **490 reviews**, 100K+) and `org.tailored` Tailored (1.7★, 14 reviews), and
  classify each complaint as *missing/broken feature* versus *"we keep this on paper"* /
  *"abandoned"*. 490 public reviews decide this, with no build and no query.
- Note: this candidate is the one most likely to move **up** if that read comes back
  "fixable-feature complaints" — the unmonetised, 28-app shelf then looks like an opportunity with
  no one trying to serve it well, not like a dead zone.

**Slots 4 and 5: intentionally empty.**

## 7. Considered and rejected (with the reason and the run)

| query(s) | label from the run | why it is not on the shortlist |
| --- | --- | --- |
| `board game collection tracker` | mixed, 22/30 rated, median 4.50 | In-job but entrenched: Board Game Stats **4.8★ paid $5.99**, My Game Collection 4.3★/500K+, BoardGameGeek's own app 100K+, Minimap 4.5★. Nothing to displace. |
| `fishing knot guide` | mixed, median 4.40 | In-job and loved: **two 1M+ apps at 4.6★/4.7★**, Useful Fishing Knots 4.4★/500K+, Knot IQ 4.9★. Content business with a free ad-supported leader. |
| `yarn stash inventory` | **fortress**, 13/18 ≥4.5★ | Broad shelf is a fortress (My Row Counter 4.8★/1M+) and the exact job is owned **off-Play** by Ravelry (Stash2Go II 4.5★/10K+ is its client). Kill. |
| `large print crossword puzzles` | **fortress**, 0/14 weak | Daily-content business (server + editorial) held by four 10M+ publishers; 14/14 declare IAP. Also an accessibility claim atop a content app. Kill. |
| `big button voice recorder` | mixed, 14/14 recorders | `com.sec.android.app.voicenote` is preinstalled at **1B+**; three more at 50M+/10M+. Button size is not a wedge. Kill. |
| `fence post spacing calculator` | mixed, but guard `ok` at a 30% job share | The on-job pocket is **unproven**: 8 near-identical "Fence Calculator" apps at 5+–1K+ installs, all unrated, one 3.0★ app with 5 reviews, two unrated paid $2.99 spacing apps. Dead-zone-shaped for the job. |
| `wedding seating chart planner` + `seating chart maker` | mixed / **dead-zone** | Two phrasings agree: across ~24 seating apps, **one** has a rating (Table Tailor, **2.6★**, 50K+); a one-shot event job seemingly bundled by four 1M+ planners. Kill. |
| `nail salon client notes` | mixed ⚠ | **Polluted:** 18/30 cards are nail-salon **games** (Nail Salon 3D 10M+, My Nail Makeover 1M+), only 4/30 are professional tools. No conclusion drawn. |
| `wallpaper quantity calculator` | mixed ⚠ | **Polluted:** 0.97 lexical share but only **4/30** cards do wallpapering; the rest are phone wallpapers and toy calculators (Xiaomi Calculator **1B+**). No conclusion drawn. |

## 8. What the scan cannot tell you

- **Revenue, retention and conversion are invisible.** A declared ads/IAP badge proves a model was
  implemented, not that it earns; the page shows the *app* price, never the in-app price points, and
  nothing here says how many of PackPoint's 42,100 raters pay.
- **Whether users would pay for a NEW app is not a store-visible fact.** 199/328 declaring IAP shows
  the shelf supports it; it says nothing about a new entrant's conversion, churn or acquisition cost.
- **A rating is also a hard gate, not only a quality signal.** `tailor measurement book` has 25/28
  apps *unrated*; the scan cannot tell whether those apps are abandoned, broken, too new, or simply
  below Play's rating threshold — and that distinction is the whole difference between "dead zone"
  and "vacant shelf with seven 10K+ apps". Only review reading resolves it.
- **Titles are not capabilities.** Every classification above is title-level. An app named
  "Recipe Scale Converter" may or may not scale correctly; `Calculation of recipes` (3.4★/100K+)
  might be a recipe browser rather than a portion tool. **UNVERIFIED** for every app named here.
- **The pocket is judged from a display-capped shelf.** Play showed ~14–30 cards per query and
  everything visible was enriched, but a larger incumbent can sit below the cap. The two 14-card
  queries (`large print crossword puzzles`, `big button voice recorder`) are the weakest samples.
- **Install buckets are floors, not counts.** `100K+` spans 100,000–499,999, and two apps in the
  same bucket are indistinguishable.
- **One storefront, one signed-out session.** `hl=en`, `gl=US`, no account, at the two timestamps in
  section 1. Play search is personalised and geo-dependent; this is a reproducible spot check, not a
  market study, and the demo/teen audiences Play serves differently are exactly the audience for the
  accessibility archetype.
- **Query text decides the shelf.** This run proves it twice in each direction (section 2): the two
  `candidate` labels rest on 14–30-card shelves, and two `ok` labels describe shelves that are half
  or three-quarters a different job.
- **Nothing here is a Play policy finding.** The children's-audience question on the front-runner,
  and any content-rating or Data safety consequence of these niches (e.g. a recorder app that holds
  audio), are **not** assessed in this document.

## 9. UNVERIFIED (explicit)

1. **Ravelry as the real yarn-stash incumbent.** Inferred from the title "Stash2Go II – Ravelry
   App" (4.5★, 10K+); Ravelry itself was not fetched.
2. **That The Knot / WeddingWire / Joy / Bridebook bundle a seating chart.** Inferred from their
   "Wedding Planner" titles; no app was opened.
3. **That `Calculation of recipes` (3.4★/100K+) really does portion scaling**, and that
   `Food Cost Calculator` (3.3★/100K+) is a weak incumbent for the same job. Title-level only.
4. **Why 25/28 tailor apps are unrated** (abandoned vs. new vs. below threshold). Not observable
   from Play's search or detail pages.
5. **Whether `com.blewettproductsystems.*` apps** (e.g. "Recipe Scaling Calculator" at **1+**
   installs, "Yarn & Fabric Stash Inventory" at **1+**) are automated filler or real products.
   Relevant because they inflate the "on-job but unrated" counts.
6. **Whether the four real wallpaper calculators or the eight fence calculators have been
   discontinued.** The store page does not say.
7. **Whether `sewing measurements app`'s 7 rated weak *pattern-gallery* apps share a job with
   tailors.** I treated them as a different job (content) than measurement records; that is my read
   of the titles, not an observed fact.
8. **The result of the two review-sentiment reads proposed in section 6.1 and 6.3.** Not performed
   here (the task scoped this work to the store scan); they are named as the next cheap step.

## 10. Bottom line

- **Only three candidates survived hand-verification, and only one of those is confident.**
  `recipe portion/scaling calculator` has the same pocket under two phrasings, a weak incumbent at
  **100K+ on 3.4★**, and clear monetisation; `packing list organizer` has the cleanest relevance of
  the twelve (30/30) and a `candidate` verdict that is one unrated card from `dead-zone`; the
  tailor/sewing measurement job has the best *shape* (repeat-use offline records) and the **cheapest
  decisive test**, but its verdict flips between `candidate` and `dead-zone` on phrasing, so it is
  **not** a GO. Slots 4–5 are empty by choice.
- **The single most important finding is a query-sensitivity, not a niche.** `sewing measurements
  app` = `candidate` (12/15 weak) and `tailor measurement book` = `dead-zone` (3/28 rated) describe
  the same job; the first label is ~58% carried by pattern-gallery apps that are not the job. Any
  GO taken from a single query in this system is unsafe.
- **Two queries are unusable as markets and are excluded**: `nail salon client notes` (18/30 cards
  are nail-salon games) and `wallpaper quantity calculator` (only 4/30 cards do wallpapering). For
  both, the library's own relevance text said "part of this result set is not about the query" when
  the true figure was 60% and 87% — and for `fence post spacing calculator` and
  `seating chart maker` the guard's most-trusted label, `ok`, was issued at 30% and 45% job share.
  The lexically-echoed token `calculator` (and `maker`) is not in the library's generic-token list,
  which is the mechanism. **Reported, not fixed — `scripts/**` was not modified.**
- **Monetisation fits free-with-IAP better than free-with-ads on these shelves** (IAP declared
  60.7% / 48.3% vs ads 48.8% / 41.4% across the two runs; 71 and 32 apps declare neither), with the
  arithmetic pockets being the ad-heavy exception (recipe on-job 64–77%, fence on-job 67%). The
  paid-uptake evidence is thin everywhere: 14 paid apps out of 328, and in-job paid entrants unrated
  and sub-1K+.
- **Cheapest next step if the front-runner falls:** read the 1–3★ reviews of `com.tailormate`
  (490 reviews, 3.7★, 100K+) and `Calculation of recipes` (263 reviews, 3.4★, 100K+). Two public
  review pages separate "fixable bad tooling" from "the job does not retain phone users" for the two
  best-shaped candidates — no code, no build, no further query.
