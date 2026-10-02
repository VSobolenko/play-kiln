# App naming methodology — 2026-10-01

> **Question asked (2026-10-01):** the factory has a visual-identity doctrine (a declared `mark` and palette) and a monetization doctrine (a validated `economics.json`), and nothing about **naming**. The owner's complaint, translated from Russian: an agent building a calculator "that I already hate" will call it simply *Calculator* — nobody finds it, nothing about it is memorable, and there is no reason to install it. He asked for the name to be **derived per app** from its job, its audience and its market, with real Google Play examples.

**Scope:** the *methodology* half of a naming doctrine — the failure mode, the archetypes, a derivation procedure a recon agent can execute before any code exists, the pre-commitment checks, the factory's slug/applicationId/title constraints, and a rejection list. It is input to a rule; it is not itself a rule, and it changes no code.

**Method:** web search plus direct fetches, 2026-10-01, plus the repository's own facts read on disk. This document **did not run Gradle and edited no repository file.** Every claim carries a label: **MEASURED** (a source published the number), **ON RECORD** (a named source states it, not independently reproduced here), **INFERRED** (my reading of the evidence), **UNVERIFIED** (not checked). Third-party install trackers are labelled as such — they are estimates, not Play.

---

## 1. The failure mode, named precisely

**A purely generic name — *Calculator*, *Notes*, *QR Scanner*, *Flashlight* — is a commercial defect for a small developer, independent of any Play policy.** Four mechanisms, each concrete:

1. **Search navigation hands the query to the incumbent.** The app title is the strongest text-ranking field on Google Play (**ON RECORD** — Play Console Help and every ASO study in §8). A generic title competes for the *head term* — the shortest, highest-volume, most-contested phrase — and a new app with no ratings or install velocity does not rank there. RespectASO's 309-search study measured where a brand-new title lands: **~#37 on a very easy keyword, ~#99 on a middling one, ~#123 at difficulty 75, ~#135 at 90**; on an easy keyword about 1 in 5 new apps reach the top 10, and at difficulty 90 **fewer than 1 in 100** do (**MEASURED**). Applyra's 800,000-keyword analysis shows the inverse curve: one-word terms have median difficulty ~**37** and three-word terms ~**20**, but the share of keywords that are both winnable *and* genuinely searched falls from ~**20 %** (one word) to under **4 %** (three words) (**MEASURED**). Typing the category word is the one query a title of the category word can never win.
2. **The leader is unreachable and enormous.** Third-party trackers: *Calculator* (Google) — **1 B+** Play-certified installs, est. ~2.5 B, 4.31★ over ~792 K ratings; *Samsung Calculator* — **3.2 B+** installs, ranking in 31 countries (**MEASURED**, third-party). A slot with 0 installs and 0 ratings does not appear in that query's visible page. **INFERRED:** "nobody finds it" is not a feeling; it is §1.1 applied to the exact query the name was built from.
3. **Zero differentiation — the title says nothing a user can prefer.** A category noun carries no claim, no audience, no wedge. Two apps called *Notes* differ only by who has more installs; the name has removed the one-line argument the slot could make. The factory already refuses this for design (`.cursor/rules/visual-identity.mdc`: an unstyled Material app is a failed recipe) and for monetization (no money plan, no scaffold). A generic name is the same defect one field earlier.
4. **No brand can accumulate.** Word-of-mouth needs a token a person can repeat and a listener can type. *Calculator* is not a brand; it is a query. Google Play will **not enforce a unique title** — several live apps may share a name, and it is the `applicationId`, not the name, that is globally unique and permanent (**ON RECORD** — Play Console Help; §5). A generic name is unprotected *and* unownable: the only asset in the pack that does not compound.

**The rule in one sentence:** a name must give a user a reason to prefer *this* app over the leader in a crowded query, or it has spent the single most valuable ranking field on the category everyone else already owns.

---

## 2. Naming archetypes

Nine shapes. "The example" is a **real, live Google Play listing** unless marked otherwise. "Fits when" is the decision line; shapes combine — that is what the carrier + job noun shape is.

| # | Archetype | Shape | Real Google Play example | What it buys | What it costs | Fits when |
|---|---|---|---|---|---|---|
| 1 | **Descriptive** | names the specific job | `Mark Book for Teachers`; `Gas Pipe Sizing Workbook` (repo slots) | instant clarity; carries the query words | generic if it names the *category*, not the job; weak trademark | the job itself is specific enough to be the wedge (§2.1) |
| 2 | **Evocative / suggestive** | hints at the benefit, not the function | `Calm` (meditation) | memorable; brandable; room to extend | teaches nothing; the descriptor must carry search | the feeling is the product |
| 3 | **Coined / invented** | a new, pronounceable word | `Spotify`, `Zillow`, `Venmo`, `Hulu` | highest distinctiveness and trademark headroom | zero inherent meaning; discovery must be bought or earned | the slot can afford marketing and has a descriptor after a separator |
| 4 | **Compound** | two real words fused | `CamScanner` (cam + scanner); `Evernote` | meaning from the parts, distinctiveness from the join | can look spammy; pronunciation can wobble | one half is the job and the other a differentiator |
| 5 | **Metaphorical** | a concrete noun borrowed to suggest a quality | `Forest: Focus for Productivity` (a tree grows while you focus); `Anchor` | strong story in one word | the metaphor must be explainable in one sentence or it is noise | the job has one vivid, honest image |
| 6 | **Eponymous** | a person's / character's name | `Appu` (a tribute app); `Khan Academy` (founder) (**ON RECORD**) | warmth, story, authority | weak for a utility with no known person | a named audience or credible persona exists |
| 7 | **Acronym / initialism** | initials of a longer phrase | `YNAB: You Need A Budget` | short token + a full phrase in the descriptor | opaque until explained; can look cold | the expansion is the wedge and fits the descriptor slot |
| 8 | **Borrowed foreign word** | a word from another language | `Anki` / `AnkiDroid` — Japanese 暗記 *anki*, "memorization" (**ON RECORD**) | distinctive, meaningful, usually free | meaning is invisible to most users; cultural risk | the word is pronounceable in en **and** ru and means nothing bad (§4) |
| 9 | **Carrier + job noun** *(recommended)* | an ownable carrier + a real descriptive noun | `Sleep Cycle: Sleep Tracker`; `PictureThis - Plant Identifier`; `Forest: Focus for Productivity`; `Water Tracker: WaterMinder app`; `Yuka - Food & Cosmetic Scanner` | findable **and** ownable in one 30-char title | needs discipline to stay inside 30 chars and avoid stuffing | **the default for a small utility** — §2.1 |

### 2.1 The carrier + job noun pattern — the recommended shape

Spend the title in two halves: a **short carrier** (a coined/suggestive/compound word the slot owns) then a separator and the **real job noun or phrase** a user actually types (`Sleep Tracker`, `Plant Identifier`). It is not findability versus brand; it is both, in the space Play gives.

- **Evidence (MEASURED, best available):** AppTweak's read of the **top 100 free apps in every category across seven countries found 47 % of Google Play titles (41 % of App Store titles) add generic descriptive keywords next to a brand name after a separator** — strongest in utility, photo/video and productivity, weaker in games.
- **Supporting, weaker (ON RECORD, vendor studies):** Namify (3,579 high-download apps): 72 % keyword-led, 77 % literal/descriptive, 82 % ≥16 chars; Appbot (8,000 US top-chart apps): median 4 words / 27 characters, 32 % use a `-` separator.
- **Caveat:** the 47 % counts titles that *include* a keyword; it does not prove cause. It describes what winning listings do, consistent with the title being a ranking signal.
- **The budget:** carrier under 12 characters (**ON RECORD**); form `<Carrier>: <Job noun>` or `<Carrier> - <Job noun>`; leave **≥5 characters of slack** for a longer locale (§3 Step 3).
- **For this factory:** the carrier is *new per slot*, like the palette and the mark (`.cursor/rules/visual-identity.mdc`); never the factory's name, never a job another slot already carries.

---

## 3. The derivation procedure (normative — a recon agent executes these steps)

Inputs, available at stage 0 before any code exists: the **job sentence**, the **paying audience**, the **declared locales** (en + ru minimum), and the **exact phrases users type**. Produce a decision record (§5.3), not a preference.

**Step 1 — Mine real user phrasing.** Take candidate job nouns from real users, not the agent's own vocabulary: the critical reviews of weak incumbents (`scripts/scan-complaints.mjs`); Play's search autocomplete for head and mid-tail phrases; the top ~10 incumbent titles (`scripts/scan-titles.mjs`), which show which words the category already spends its title on. Keep only **two- to three-word phrases that carry real traffic** — the band that is both winnable and searched (§1.1). Output: 3–6 ranked job-noun phrases, each tagged `MEASURED` (found in user text / a scan) or `UNVERIFIED` (an agent guess).

**Step 2 — Generate 20–30 candidates across at least four archetypes.** Using §2, make for each job-noun phrase at least one descriptive, one evocative/coined, one compound and one metaphor candidate, then build **carrier + job noun** candidates from the best carriers. Generate a field and eliminate; do not write one name and defend it. Include a **negative control** — the bare category word — which must lose to every serious candidate.

**Step 3 — Screen mechanics before meaning.** Eliminate on hard tests first, so taste never decides:
- **Radio test** (**ON RECORD** — brand-naming practice; DN.org's model): say the carrier once with no visual aid; a listener must spell it correctly from sound alone. Reject dropped vowels, silent letters, doubled consonants, `-er`/`-re`/`-ar` ambiguity, homophone traps.
- **Pronounceability:** 2–3 syllables; a stranger says it right on the first try.
- **Length inside Play's cap:** **30 characters for the title, including spaces and any localised name** (**MEASURED** — Play Console Help). Measure `<Carrier>: <Job noun>` in **en and ru**; require ≥5 characters of slack.
- **Character set:** Latin letters and a plain space/`:`/`-`. Play bans emojis, ALL CAPS, repeated special characters and devised misspellings (**ON RECORD**).
- **One canonical spelling** — exactly one obvious way to type it.

**Step 4 — Check collisions and searchability (before selecting).** Search the exact title and close variants in Play and note how many live apps surface (**Play does not enforce unique titles**, so "taken" is a judgement — §4). Search the head term; if a 1 B+-install incumbent owns it, the carrier must not try to own it alone. Check the candidate against the **head-term register** so two of our own slots do not split one query (head-term discipline is **ON RECORD**). Confirm the derived `applicationId` (`com.gexetr.<slug>`, dashes dropped) is free in the registry (§5).

**Step 5 — Score, so the choice is arguable on evidence.** Score each survivor 0–5 on seven dimensions and weight them (adapted from a published brand-name rubric, **ON RECORD**). Require **≥70/100 overall with no dimension below 2.5**; else return to Step 2.

| Dimension | Weight | The question to answer with evidence |
|---|---:|---|
| Job fit / specificity | 20 | does the title tell a user this app's *specific* job, not its category? |
| Searchability | 15 | does it contain the two-to-three-word phrase users actually type? |
| Distinctiveness / memorability | 15 | does it stand apart from the top three incumbents' titles? |
| Radio test / spelling-on-hearing | 15 | can someone who heard it once type it correctly? |
| Uniqueness / trademark headroom | 15 | is the Play + register field clear enough to own (§4)? |
| Length fit | 10 | does `<Carrier>: <Job noun>` fit 30 chars in en **and** ru with ≥5 slack? |
| Locale survival | 10 | does it mean something acceptable in every declared locale (§4)? |

**Step 6 — Run the pre-commitment checks (§4).** A high-scoring candidate that collides, or means something unfortunate in ru, is eliminated; the score does not outrank a hard check.

**Step 7 — Record the decision (§5.3).** Write the naming record: chosen name, rejected alternatives with reasons, the derived slug, applicationId and per-locale titles. The record is the single source the slot is scaffolded from.

---

## 4. Pre-commitment checks (verify before the name is committed)

- **Collision with existing apps and brands.** Search Play for the exact title and close variants; search the wider web and the relevant markets for existing brands. **A clean Play search is not clearance:** Play does not enforce unique titles, and passing review is not evidence a name is clear (**ON RECORD** — Play Console Help; Namecheckly).
- **Trademark risk.** Screen the registers an examiner would cite — **USPTO**, **EUIPO (eSearch plus / TMview)**, **WIPO Global Brand Database**, plus the ru-market national register. The class is **Class 9 (downloadable software / mobile apps)**; a hosted service layer adds **Class 42**, and adjacent categories can add others (**ON RECORD** — USPTO; EUIPO; class-9 guidance). Both stores remove live listings over IP complaints, so this is an account risk, not a nicety. A registry screen is **not** legal clearance — for a serious brand it says "get advice", not "you are clear".
- **Unfortunate / ridiculous meaning in every declared locale (en + ru minimum).** A name neutral in English can be vulgar, comic or unlucky elsewhere. Documented: a name ordinary in English was Quebec street slang in Canadian French; Microsoft transcreated Bing for China because soundalike characters could read as "illness"/"sickness" (**ON RECORD** — Alconost; Creative Translation). Check pronunciation, slang, taboo and religious/cultural association with a **native in-market reader**, not an in-house guess.
- **Does the name survive translation?** This factory already holds that a **layout must survive localisation** — `.cursor/rules/visual-identity.mdc` fails a style whose meaning depends on English string lengths, and `docs/localization-standards.md` requires a slot to be born multilingual with the pseudolocale (`en-XA`, ~30 % longer strings) as the layout test. **A name must meet the same bar, for the same reason.** The name is a user-visible string with a hard cap (30 characters) and a per-language variant; if it only works because English is short, it is the name-shaped version of the defect that rule already names. Concretely: measure the title in en and ru, keep slack, decide per locale whether the carrier stays Latin (transliterated) or is transcreated, and **never let a machine translation name the app** — `AGENTS.md` forbids enabling Play's automatic string translation over a translation the slot already ships, and the reasoning applies one level up.

---

## 5. The factory's own constraints, connected

The name is upstream of two permanent things and one capped thing, and the scaffold takes them as **separate flags** — `scripts/new-slot.mjs --slug <slug> --name "<Title>"` — which is exactly how they drift apart.

### 5.1 The chain
`name` → `slug` → `applicationId` (`com.gexetr.<slug>`) → store `title` (≤30 chars, per locale).
- **The slug** is the directory and the package stem; lowercase kebab case, 2–40 characters (`new-slot.mjs` validates exactly that).
- **The applicationId** is permanent and cannot be reused. `applicationIdForSlug` builds `com.gexetr.<slug>` and **drops dashes**, so two distinct slugs can collide — `new-slot.mjs` already refuses a taken applicationId for this reason. Android is explicit: **once published, the application ID should never change; changing it makes Play treat the upload as a different app, and it cannot be reused even after unpublishing** (**MEASURED** — developer.android.com).
- **The store title** is capped at **30 characters, including spaces and the localised name** (**MEASURED** — Play Console Help; `scripts/lib/play-assets.json` resolves it to `title: 30`). It **can** be changed after publishing without losing installs or reviews so long as the applicationId and signing certificate stay the same (**ON RECORD**).

### 5.2 Reversible before first upload, irreversible after
- **Reversible (development):** the name, the slug, the directory, the title and the applicationId — nothing has shipped, nothing is bound. That is why naming is decided **at recon, before the UI**, alongside the promise contract, not during `/release`.
- **Irreversible (after the first upload):** the **applicationId**. It cannot be changed or reused; a rename means a new app with a new listing, and the old installs, ratings and reviews cannot be moved. Everything downstream of the slug is fixed by the naming decision the moment the first AAB is published.

### 5.3 How to record it, so the three are derived and not guessed
Write **one** naming record at recon, beside the other plan artifacts, e.g. `docs/ideation/plans/<slug>.naming.json`, holding: the chosen `name`, the `carrier`, the `jobNoun`, the `archetype`, the `rejected` candidates with reasons, the per-locale `title` strings with measured lengths, the `slug`, and the derived `applicationId`. The store listing and the scaffold are then **read from that record** — the title from its locale strings, the slug from its `slug`, the package from `applicationIdForSlug(slug)` — so a name change is a change to one file every downstream consumer derives from, exactly as the palette in `store/studio.json` is one source its theme and its store pack are both rendered from.

---

## 6. The rejection list

**Never propose a name that is:**

1. **A pure generic word or bare category noun** — *Calculator*, *Notes*, *QR Scanner*, *Flashlight*, *Converter*. (§1: it spends the ranking field on the head term the leader owns.)
2. **A `My` / `Simple` / `Easy` / `Pro` / `Free` / `Best` prefix or suffix** — *My Notes*, *Simple Budget*, *Pro Scanner*. Empty modifiers; *Free*/*Best* also run into Play's rules on promotional and ranking text.
3. **The factory's own name** — *PlayKiln* or any variant. The factory is not the product; a slot's identity is per-slot by rule (`.cursor/rules/visual-identity.mdc`).
4. **A number-only or emoji name** — *2048*, *Calculator 🧮*. Unsearchable by word, inaccessible to screen readers and voice control, against Play's metadata guidance.
5. **A name that promises a capability the app does not have** — *PDF Pro* on a reader that cannot edit, *AI Scanner* on an app that does not scan with AI. Misleading metadata is a policy risk and a refund.
6. **A name that collides with a competitor's** — an existing app, brand or registered mark (§4). "It is empty on Play" is not a collision check.

**Also rejected, each with a reason:**

7. **ALL CAPS, repeated symbols, or a stylised misspelling** of a common word (Play metadata rule; misspellings also cost the typos a user makes).
8. **Keyword stuffing** — two or three unrelated category words jammed into 30 characters; Play treats repetitive/irrelevant keywords as a suspension risk and users read it as spam.
9. **A name that fails the radio test** — a listener cannot type it after hearing it once.
10. **A name longer than the carrier + descriptor budget** — no room for the job noun, or it overflows 30 characters in a longer locale (ru minimum).
11. **A name that needs a pronunciation guide**, or has more than one plausible spelling.
12. **A homophone trap** in a target locale — sounds like a different, unfortunate word.
13. **A name with an unexpected / culturally sensitive meaning in a declared locale** (en/ru minimum; §4).
14. **A name that duplicates another slot's head term**, or that one sentence can describe as another slot — the Repetitive Content risk the visual-identity rule exists to prevent, one field earlier.
15. **A name that only works in English** — its meaning or its length depends on English, the naming form of the localisation defect `.cursor/rules/visual-identity.mdc` already fails.
16. **A name that copies an incumbent's title with one word swapped** — a rename plus a new palette without a new job is the shape Play's Spam policy targets (`.cursor/rules/ideation-gate.mdc`).

---

## 7. What this document cannot say

It cannot prove a name **causes** installs. The strongest available facts (AppTweak 47 %; the install leadership of *Calculator*) describe what winning listings look like and why a generic title cannot win a head term; none is a controlled experiment, because one does not exist. §3 is a **discipline for making a decision defensible and bounded**, not a guarantee of installs. The *Calculator* and *Samsung Calculator* figures are **third-party estimates**, not Play's own counts. And it cannot judge beauty — only collisions, meaning, length and fit; the judgement that separates a distinctive name from an ugly one stays with the owner, exactly as it does for the visual identity.

---

## 8. Sources

Retrieved 2026-10-01 unless noted.

| Source | Used for |
|---|---|
| Play Console Help — *Best practices for your store listing* (<https://support.google.com/googleplay/android-developer/answer/13393723?hl=en>), *Create and set up your app* (<https://support.google.com/googleplay/android-developer/answer/9859152>), *Metadata* (<https://support.google.com/googleplay/android-developer/answer/9898842>), *Get discovered on Google Play search* (<https://support.google.com/googleplay/android-developer/answer/4448378>), *Content quality guidelines* (<https://support.google.com/googleplay/android-developer/answer/12929944>) | the 30-character title cap (incl. localised names); no unique titles enforced; banned shapes (emoji, ALL CAPS, misspellings, stuffed keywords, "Free"/"#1"/"Best"); title as ranking field |
| Play Console Help — *Intellectual Property* (<https://support.google.com/googleplay/android-developer/answer/9888072>) and *Advance notice* (<https://support.google.com/googleplay/android-developer/answer/6320428>) | trademark/impersonation removal; permission documentation |
| developer.android.com — *Configure the app module* (<https://developer.android.com/build/configure-app-module>) | applicationId is permanent; changing it makes Play treat the upload as a new app |
| RespectASO — *Methodology* (<https://respectaso.com/docs/methodology/>) | where a brand-new title lands by keyword difficulty (309 real searches; 5,846 apps) |
| Applyra — *State of ASO 2026* (<https://www.applyra.io/blog/state-of-aso-2026>) | difficulty falls 37→~20 as phrases lengthen while winnable-and-searched share falls ~20 %→<4 % (800,000 keywords) |
| MWM — *Keyword Difficulty* (<https://mwm.ai/glossary/keyword-difficulty>); ASO World (<https://asoworld.com/blog/your-target-aso-keyword-is-too-competitive-how-to-find-app-store-keywords-you-can-actually-rank-for/>) | a new app cannot rank on a head term without ratings/velocity; target mid-tail first |
| AppTweak — *Optimizing Your App Title: Keywords vs. Brand Name* (<https://www.apptweak.com/en/aso-blog/optimizing-your-app-title-keywords-vs-brand-name>) | **47 % of Google Play top-100-free titles add generic keywords next to a brand name** (top 100, all categories, 7 countries) |
| Namify — pattern analysis (<https://namify.tech/app-name-generator>); Appbot — *How to name your mobile app* (<https://appbot.co/blog/how-to-name-your-mobile-app-or-game/>) | 3,579-app and 8,000-app pattern reads: keyword-led/literal majorities, median 4 words / 27 chars, 32 % `-` separator (vendor studies) |
| Appilot (<https://appilot.ai/blog/how-to-choose-app-name>); BrandAuditor (<https://www.brandauditor.ai/blog/how-to-name-a-brand>); Namilio (<https://namilio.com/blog/how-iconic-brands-got-their-names>); NameSpark (<https://namespark.tools/tools/brand-name-scorer/>) | archetype taxonomies and examples; brand ≤12 chars; the seven-dimension weighted rubric adapted for §3 Step 5 |
| Radiotest.me (<https://radiotest.me/>); DN.org — *The Radio Test Model* (<https://dn.org/the-radio-test-model-for-spoken-brandables/>) | the radio test: spell-from-hearing, rhythm, memory encoding |
| USPTO (<https://www.uspto.gov/trademarks/search>); EUIPO (<https://www.euipo.europa.eu/en/help-centre/tm/faq-search-availability>); surge.law — *Trademark Class 9* (<https://surge.law/trademark-class-9-software-technology/>); Namecheckly (<https://namecheckly.com/google-play-name-checker>) | Class 9 (+42 etc.); USPTO/EUIPO/TMview/WIPO registers; clean Play search ≠ clearance; application ID permanent |
| Alconost — *Cultural analysis* / *Four cultural traps* (<https://alconost.com/en/blog/cultural-traps-localization>); Creative Translation (<https://creativetranslation.com/is-your-brand-name-global-nailing-brand-name-translation/>); Lokalise (<https://lokalise.com/blog/transcreation-vs-localization/>); RuStore developer blog (<https://www.rustore.ru/developer/en/blog/how-to-localize-an-app-for-the-russian-market-in-2026>) | unfortunate-meaning risk; Quebec-slang and Bing-China cases; transcreation vs transliteration; ru-market naming practice |
| Play listings cited: `Sleep Cycle: Sleep Tracker` (`com.northcube.sleepcycle`); `PictureThis - Plant Identifier` (`cn.danatech.xingseus`); `Forest: Focus for Productivity` (`cc.forestapp`); `Water Tracker: WaterMinder app` (`com.funnmedia.waterminder`); `YNAB: You Need A Budget`; `CamScanner`; `Appu`; `Calculator` (`com.google.android.calculator`) | the archetype examples in §2 |
| Wikipedia — *Anki* and Wiktionary 暗記 (<https://en.wiktionary.org/wiki/%E6%9A%97%E8%A8%98>) | Anki's borrowed-foreign-word origin (Japanese *anki*, "memorization") |
| Third-party install trackers: androidrank.org, AppstoreSpy, AppDashboard | Calculator / Samsung Calculator install figures (estimates, not Play) |
| **Repo-internal, read on disk:** `scripts/new-slot.mjs` (slug validation, `applicationIdForSlug`, dashes dropped, free-applicationId check); `scripts/lib/namespaces.mjs`; `scripts/lib/play-assets.json` (`title: 30`, `shortDescription: 80`, `fullDescription: 4000`); `registry/apps.json`; `.cursor/rules/visual-identity.mdc`; `.cursor/rules/ideation-gate.mdc`; `docs/localization-standards.md` | the factory chain in §5 and the localisation bar in §4 |
