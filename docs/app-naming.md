# App naming contract — `naming.json`

Every slot states **what it is called and how that name was derived**, in a file a
program reads. This document is the specification; it is enforced by the naming check
whose slot half is `slot.naming`, and `scripts/new-slot.mjs` refuses to create a slot
without a valid record. Both facts are stated here because a name is worthless as a
convention — it is the one field a builder picks from their own head in thirty seconds
and every later stage then treats as a given.

## Why this file exists

The failure mode is a name invented from the builder's head. The owner's complaint, recorded
2026-10-01: an agent building a utility "that I already hate" will call it simply **`Calculator`** —
because the job is a calculator and the category word is the first word that comes to mind. Nothing
about that decision was recorded, nothing was rejected, and nothing about it derived anything.

That is a **commercial** defect first, and it is worth naming separately from any policy question,
because the policy half is survivable and this half is not.

### The commercial defect, precisely

1. **Search navigation hands the query to the incumbent.** The app title is the strongest
   text-ranking field on Play — **ON RECORD, not a Google claim**: AppTweak states titles "carry the
   strongest weight in terms of keyword indexation", Appmetra that Play "indexes the app name for
   keyword ranking, with more weight than any other field", PlayAudit that it is the "[h]ighest
   ranking weight". A generic title competes for the *head term* — the shortest, highest-volume, most
   contested phrase — and a new app with no ratings and no install velocity does not rank there.
   **MEASURED**, RespectASO's 309-search study: a brand-new title lands around **#37 on a very easy
   keyword, ~#99 on a middling one, ~#123 at difficulty 75 and ~#135 at 90**; at difficulty 90 fewer
   than **1 in 100** new apps reach the top 10. **MEASURED**, Applyra's 800,000-keyword analysis: the
   share of keywords that are both winnable *and* genuinely searched falls from ~**20 %** at one word
   to under **4 %** at three. Typing the bare category word is the one query a title of the bare
   category word can never win, and it is the only query that name was built for.
2. **The leader is unreachable and enormous.** Third-party install trackers — **estimates, not Play's
   own counts** — put *Calculator* (Google) at **1 B+** Play-certified installs (est. ~2.5 B, 4.31★
   over ~792 K ratings) and *Samsung Calculator* at **3.2 B+** installs. A slot with 0 installs and 0
   ratings does not appear on that query's visible page at all. **INFERRED:** "nobody finds it" is not
   a feeling; it is the previous point applied to the exact query the name was chosen from.
3. **Zero differentiation.** A category noun carries no claim, no audience and no wedge — it is the
   one field that could have made a one-line argument and instead spent itself on the category
   everyone already owns. The factory already refuses this for design
   (`.cursor/rules/visual-identity.mdc`: an unstyled Material app is a failed recipe) and for money
   (no plan, no scaffold). A generic name is the same defect one field earlier.
4. **No brand can compound.** Word-of-mouth needs a token a person can repeat and a listener can
   type. *Calculator* is not a brand; it is a query. Play **does not enforce unique titles** — several
   live apps may share a name, and it is the `applicationId`, not the name, that is globally unique
   and permanent — so a generic name is simultaneously unprotected and unownable: the only asset in
   the pack that never accumulates.

**The rule in one sentence:** a name must give a user a reason to prefer *this* app over the leader
of a crowded query, or it has spent the single most valuable ranking field on the category everyone
else already owns. The name is the first thing the pack says, and it is decided **at recon**, before
the UI exists, alongside the promise contract — never during `/release`.

### What is measurement and what is not

Everything above that cites a number is from the two research documents behind this contract:
`docs/research/2026-10-01-play-title-policy-and-aso.md` (policy wording, limits, enforcement) and
`docs/research/2026-10-01-app-naming-methodology.md` (the archetypes, the derivation procedure, the
scoring rubric). Their labels are preserved here: **MEASURED** is a number a source published,
**ON RECORD** is a named source asserting it without independent reproduction, **INFERRED** is this
document's reading, and **UNVERIFIED** is not checked. The install figures are third-party
estimates, not Play's. Where a claim rests on a single vendor study — the Incipia **+109 %** exact
title match, the ASOgency **+20 %** CVR (which is an **iOS** sample, so its transfer to Play is
**INFERRED**), the n=1 Medium **+26 %** impressions — it is labelled as such and is not treated as a
law. Nothing here proves a name *causes* installs; no controlled experiment exists. §"The bar" is a
discipline for making the decision recorded and arguable, not a guarantee of rank.

## The bar

A name is not accepted until all of the following are true.

- **It is derived from the app's job and audience.** The job sentence and the paying audience exist
  by stage 0; the candidate job nouns come from real user phrasing (critical reviews of weak
  incumbents, Play search autocomplete, the incumbent titles the category already spends its title
  on), not from the builder's vocabulary. Each candidate job noun is tagged `MEASURED` (found in
  user text or a scan) or `UNVERIFIED` (an agent guess).
- **It is recorded together with its scored candidates and its rejected alternatives.** The record
  names the winner, every serious candidate with its seven-dimension score, and at least one
  rejected alternative with its reason. A field is generated and eliminated; one name is not written
  and defended. The bare category word is included as a **negative control** and must lose to every
  serious candidate.
- **It satisfies Play's title rules** (below) — 30 characters including spaces, no emoji, no ALL
  CAPS, no keyword spam, no competitor names, no ranking or price text, no stylised misspelling.
- **It passes the radio test.** Said once with no visual aid, a listener spells the carrier
  correctly from sound alone. Reject dropped vowels, silent letters, doubled consonants, `-er`/`-re`/
  `-ar` ambiguity and homophone traps; two to three syllables; one canonical spelling, exactly one
  obvious way to type it. (**ON RECORD** — brand-naming practice, DN.org's radio-test model.)
- **It fits the 30-character title alongside its descriptor.** The recommended shape is
  `<carrier>: <job noun>`, and the *whole string* is measured — the cap applies to the localised
  name too, not to the carrier alone. Keep **≥ 5 characters of slack** for a longer locale.
- **It survives localisation into every declared locale.** en and ru are the minimum this factory
  ships.

### Why the name clears the same localisation bar the layout does

`.cursor/rules/visual-identity.mdc` already fails a style whose meaning depends on English string
lengths, and `docs/localization-standards.md` requires a slot to be born multilingual with a
pseudolocale (`en-XA`, ~30 % longer strings) as the layout test. **A name must meet the same bar,
for the same reason, and this is not an analogy.** The title is a user-visible string with a hard
30-character cap and a **per-language variant** — Play's own field note says *"You can add one
localized name per language"*, and the best-practices page repeats that *"the Metadata policy and
our other policies apply to all translations of your Google Play store listing"* (**MEASURED**). So
a carrier that only works because English happens to be short is the name-shaped version of the
defect that rule already names. Concretely: measure the title in en **and** ru, keep slack, decide
per locale whether the carrier stays Latin (transliterated) or is transcreated, and **never let a
machine translation name the app** — `AGENTS.md` forbids enabling Play's automatic string
translation over a translation the slot already ships, and the reasoning applies one level up.
Rename-one-locale-later is not an option either: the *carrier* is the brand, and a brand that is
different in each language is not one brand.

## Play's actual rules

These are the measured limits and the exact policy text. Every quotation below was read on the
cited source on 2026-10-01; nothing is paraphrased into a rule it does not support.

### The character limit

- **MEASURED**, Play Console Help, *Create and set up your app*: the Console field is **App name**,
  **30 character limit**, with the note *"You can add one localized name per language."* The page's
  footnote is the part that catches non-Latin scripts: *"Character limits apply to both full-width
  and half-width characters — the numbers listed above are the maximum limits regardless of what
  type of characters you are using."* — <https://support.google.com/googleplay/android-developer/answer/9859152>
- **MEASURED**, Metadata policy and *Best practices for your store listing*: *"Your app title must
  be 30 characters or less."* — <https://support.google.com/googleplay/android-developer/answer/9898842>,
  <https://support.google.com/googleplay/android-developer/answer/13393723?hl=en>
- **ON RECORD** (third-party, AppTweak): the limit was **50 characters until 29 September 2021**,
  then reduced to 30. This is why older ASO guidance is wrong: MobileAction still states *"Google
  Play Store allows for up to 50 characters in your app title"* — **ON RECORD and outdated** against
  the policy measured above. — <https://www.apptweak.com/en/aso-blog/how-to-prepare-for-new-google-metadata-policy-changes>

### What a title may not contain

Metadata policy (*Store Listing and Promotion* group), the governing paragraph — **MEASURED,
quoted**:

> "Users depend on descriptions of your app to help them understand its functionality and purpose.
> We don't allow apps with misleading, improperly formatted, non-descriptive, irrelevant, excessive,
> or inappropriate metadata, including but not limited to the app's description, developer name,
> title, icon, screenshots, and promotional images."
> "Your app title, icon, and developer name are particularly helpful for users to find and learn
> about your app. Don't use emojis, emoticons, or repeated special characters in these metadata
> elements. Avoid ALL CAPS unless it is part of your brand name. … Your app title must be 30
> characters or less. Don't use text or image in the app title, icon, or developer name that indicate
> store performance or ranking, price or promotional information, or that suggests relations to
> existing Google Play programs."

Also on that page, **MEASURED, verbatim**: *"Avoid using repetitive or unrelated keywords or
references."*, and in the Do/Don't table *"Don't use repetitive keywords or deceptive claims like
'best game ever.'"* and *"Don't use special characters or all caps to manipulate search ranking."* —
<https://support.google.com/googleplay/android-developer/answer/9898842>

The best-practices page, **MEASURED**: *"Make sure your title is unique, accessible, and avoid
common terms."*; *"Don't use text elements to promote deals. Words like 'Free' and 'No Ads' promote
deals and don't belong in app titles or developer names."*; *"Don't use CAPS unless your brand is
capitalized."* Its own documented counter-example: *"App title: Crane: travel PLANNER!! 🌞"*, and
the ranking/price text it lists as disallowed — *"'App of the year,' '#1,' 'Best of Play 20XX,'
'Popular,' award icons … '10% off,' '$50 cash back,' 'free for a limited time only'"*. —
<https://support.google.com/googleplay/android-developer/answer/13393723?hl=en>

Three prohibitions live in **other** policies, and it matters which page names the violation:

- **Competitor names — Impersonation, not Metadata. MEASURED:** *"Don't use another app or entity's
  title or name in a manner that may mislead users."* and *"Don't falsely claim to be the 'official'
  app of an established entity."*
- **Repetitive/duplicate catalogue — the Spam policy's Repetitive Content section. MEASURED:**
  *"We don't allow apps that merely provide the same experience as other apps already on Google
  Play."* and *"Creating multiple apps with highly similar functionality, content, and user
  experience."* — <https://support.google.com/googleplay/android-developer/answer/9899034>
- **Promotional/ranking text — the App Promotion policy repeats the title restriction. MEASURED:**
  *"Text or image in the app title, icon, or developer name that indicate store performance or
  ranking, price or promotional information, or that suggests relations to existing Google Play
  programs."* — <https://support.google.com/googleplay/android-developer/answer/9899004?hl=en>

**INFERRED:** "keyword spam" and "irrelevant keywords" have no separate named section in a *title*;
they fall under the Metadata policy's *"irrelevant, excessive … metadata"* sentence plus the
best-practices guidance, and under App Promotion when used to boost visibility.

### Keyword stuffing does not buy ranking — Google says so itself

The load-bearing fact for whether keywords belong in a name at all is Google's own, and it is the
inverse of what a keyword-stuffed title assumes. **MEASURED**, *Add preview assets*, on the short
description: *"Unnecessary keywords in an attempt to improve search results; it will not impact
ranking and provides a bad user experience."* —
<https://support.google.com/googleplay/android-developer/answer/9866151>

So a stuffed title is not a clever ranking hack: it is a bad user experience, it does not rank, and
it is a policy violation (§ Metadata, App Promotion). The Console itself says so, **MEASURED**,
*Create and set up your app*: *"Repetitive or irrelevant use of keywords in the app name,
description, or promotional description can create an unpleasant user experience and result in an
app being suspended on Google Play."*

**The only defensible reason to put words in the title is the job noun a real user types** — the
two-to-three-word mid-tail phrase — not a pile of related category words. The strongest available
evidence for the *useful* version of this is the AppTweak read in the archetypes section below.

### Enforcement, and it escalates to the whole catalogue

**Which page names the violation — MEASURED:** a title violation is decided under the **Metadata**
policy; the **App Promotion** policy names the title restriction independently; the **Spam** policy
names duplicate-catalogue behaviour. *Enforcement Process* states Google considers *"app metadata
(for example, app title, description)"* when reviewing. —
<https://support.google.com/googleplay/android-developer/answer/9899234>

Escalation ladder — **MEASURED, quoted from *Enforcement Process***:

> "Repeated or serious violations (such as malware, fraud, and apps that may cause user or device
> harm) of these policies or the Developer Distribution Agreement (DDA) will result in termination
> of individual or related Google Play Developer accounts."

- **Rejection:** *"A new app or app update submitted for review will not be made available on Google
  Play. … Rejections don't impact the standing of your Google Play Developer account."*
- **Removal:** *"The app, along with any previous versions of that app, are removed from Google
  Play … multiple removals may result in a suspension."*
- **Suspension:** *"Suspension can occur as the result of egregious or multiple policy violations,
  as well as repeated app rejections or removals. … Suspensions count as strikes against the good
  standing of your Google Play Developer account. Multiple strikes can result in the termination of
  individual and related Google Play Developer accounts."*
- **Account Termination:** *"When your developer account is terminated, all apps in your catalog will
  be removed from Google Play and you will no longer be able to publish new apps. This also means
  that any related Google Play developer accounts will also be permanently suspended."*
- **Limited Visibility** (a distinct action): *"Your app's discoverability on Google Play is
  restricted."*

Two further **MEASURED** facts from account help, because "we would get a warning first" is not true:
*"App removals, suspensions, and warnings all count as strikes against the good standing of your Play
Console account. Multiple or egregious policy violations may result in the termination of your Play
Console account."* and *"Google is not required to send you a warning prior to suspending or
terminating your account."* — <https://support.google.com/googleplay/android-developer/answer/9023898?hl=en>

**This is why the rejection list below is enforced rather than advised.** The factory already treats
the internal-near-duplicate half of the Spam policy as an existential risk to the whole catalogue
(`docs/monetization-strategy.md` §1.2, `.cursor/rules/ideation-gate.mdc`). A naming rule is that
same risk one field earlier: a name that copies an incumbent's job, or that duplicates another slot's
head term, is not a taste problem, it is an account problem.

**Honest gap:** **UNVERIFIED** — a named app publicly removed or terminated *specifically* for
title/metadata keyword spam was not found. The escalation ladder is documented and unambiguous; a
worked public example of a title-only enforcement was not located. The rule does not rest on such an
example existing.

### Display and truncation

**MEASURED**, *Get discovered on Google Play search*: *"Keep your title focused. Long titles may be
shortened on some devices that your users are browsing on."* (**ON RECORD**, Appmetra: names are
truncated on search cards and the launcher, so *"write for the front of the string, not the end."*) —
<https://support.google.com/googleplay/android-developer/answer/4448378>. **INFERRED:** Google
publishes no fixed cut point, so it is device- and locale-dependent and is not machine-checkable
from the limit alone — which is one more reason the carrier goes first.

## The archetypes

Nine shapes, adapted from the methodology research. "The example" is a **real, live Google Play
listing** unless marked otherwise. "Fits when" is the decision line; shapes combine — that is what
the recommended shape below already is.

| # | `archetype` id | Shape | Real Play example | What it buys | What it costs | Fits when |
|---:|---|---|---|---|---|---|
| 1 | `descriptive` | names the specific job | `Mark Book for Teachers`; `Gas Pipe Sizing Workbook` (repo slots) | instant clarity; carries the query words | generic if it names the *category*, not the job; weak trademark | the job itself is specific enough to be the wedge |
| 2 | `evocative` | hints at the benefit, not the function | `Calm` (meditation) | memorable, brandable, room to extend | teaches nothing; the descriptor must carry search | the feeling is the product |
| 3 | `coined` | a new, pronounceable word | `Spotify`, `Zillow`, `Venmo`, `Hulu` | highest distinctiveness and trademark headroom | zero inherent meaning; discovery must be bought or earned | the slot can afford marketing and has a descriptor after a separator |
| 4 | `compound` | two real words fused | `CamScanner`; `Evernote` | meaning from the parts, distinctiveness from the join | can look spammy; pronunciation can wobble | one half is the job and the other a differentiator |
| 5 | `metaphorical` | a concrete noun borrowed to suggest a quality | `Forest: Focus for Productivity`; `Anchor` | strong story in one word | the metaphor must be explainable in one sentence or it is noise | the job has one vivid, honest image |
| 6 | `eponymous` | a person's / character's name | `Appu`; `Khan Academy` (**ON RECORD**) | warmth, story, authority | weak for a utility with no known person | a named audience or credible persona exists |
| 7 | `acronym` | initials of a longer phrase | `YNAB: You Need A Budget` | short token plus a full phrase in the descriptor | opaque until explained; can look cold | the expansion is the wedge and fits the descriptor slot |
| 8 | `borrowed_foreign_word` | a word from another language | `Anki` / `AnkiDroid` — Japanese 暗記 *anki*, "memorization" (**ON RECORD**) | distinctive, meaningful, usually free | meaning invisible to most users; cultural risk | the word is pronounceable in en **and** ru and means nothing bad |
| 9 | `carrier_job_noun` **(default)** | an ownable carrier + a real descriptive noun | `Sleep Cycle: Sleep Tracker`; `PictureThis - Plant Identifier`; `Forest: Focus for Productivity`; `Water Tracker: WaterMinder app`; `Yuka - Food & Cosmetic Scanner` | findable **and** ownable in one 30-char title | needs discipline to stay inside 30 chars and avoid stuffing | **the default for a small utility** |

### `carrier_job_noun` is the default for a small utility

Spend the title in two halves: a **short carrier** — a coined, evocative, compound or metaphorical
word the slot owns — then a separator and the **real job noun** a user actually types
(`Sleep Tracker`, `Plant Identifier`). It is not findability versus brand; it is both, in the space
Play gives.

- **MEASURED, the best available support:** AppTweak's read of the **top 100 free apps in every
  category across seven countries found 47 % of Google Play titles (41 % of App Store titles) add
  generic descriptive keywords next to a brand name after a separator** — strongest in utility,
  photo/video and productivity, weaker in games. This is a description of what winning listings do,
  not proof of cause; it is the strongest evidence that exists for the pattern.
- **Supporting, weaker (ON RECORD, vendor studies):** Namify (3,579 high-download apps): 72 %
  keyword-led, 77 % literal/descriptive, 82 % ≥ 16 characters; Appbot (8,000 US top-chart apps):
  median 4 words / 27 characters, 32 % use a `-` separator.
- **The budget:** carrier under 12 characters (**ON RECORD**); form `<Carrier>: <Job noun>` or
  `<Carrier> - <Job noun>`; leave ≥ 5 characters of slack for a longer locale.
- **For this factory:** the carrier is **new per slot**, like the palette and the mark
  (`.cursor/rules/visual-identity.mdc`) — never the factory's name, never a head term another slot
  already carries.

The other eight are first-class options, and the choice is a **conclusion about this app, its
audience and its market**, not an inheritance. The factory's own history is the warning: every
visual identity, every economics plan and every recipe in this repository taught the same answer for
long enough that the doctrine had to say "there is no default" out loud
(`docs/slot-economics.md`). A record whose `archetype` is `carrier_job_noun` because the last
slot said so is the same defect in a different field.

## The rejection list

These must never be proposed. Items 1–6 are the hard bans; items 7–16 are rejections that each need
a reason in the record.

1. **A pure generic word or bare category noun** — *Calculator*, *Notes*, *QR Scanner*, *Flashlight*,
   *Converter*. It spends the ranking field on the head term the leader owns.
2. **A `My` / `Simple` / `Easy` / `Pro` / `Free` / `Best` prefix or suffix** — *My Notes*, *Simple
   Budget*, *Pro Scanner*. Empty modifiers; *Free*/*Best* also run into Play's rules on promotional
   and ranking text.
3. **The factory's own name** — *PlayKiln* or any variant. The factory is not the product; a slot's
   identity is per-slot by rule.
4. **A number-only or emoji name** — *2048*, *Calculator 🧮*. Unsearchable by word, inaccessible to
   screen readers and voice control, against Play's metadata guidance.
5. **A name that promises a capability the app does not have** — *PDF Pro* on a reader that cannot
   edit, *AI Scanner* on an app that does not scan with AI. Misleading metadata is a policy risk and
   a refund.
6. **A name that collides with a competitor's** — an existing app, brand or registered mark. "It is
   empty on Play" is not a collision check.
7. **ALL CAPS, repeated symbols, or a stylised misspelling** of a common word — the metadata rule,
   and misspellings also cost the typos a user makes.
8. **Keyword stuffing** — two or three unrelated category words jammed into 30 characters. Play
   treats repetitive/irrelevant keywords as a suspension risk and users read it as spam.
9. **A name that fails the radio test** — a listener cannot type it after hearing it once.
10. **A name longer than the carrier + descriptor budget** — no room for the job noun, or it
    overflows 30 characters in a longer locale (ru minimum).
11. **A name that needs a pronunciation guide**, or has more than one plausible spelling.
12. **A homophone trap** in a target locale — it sounds like a different, unfortunate word.
13. **A name with an unexpected or culturally sensitive meaning in a declared locale** (en/ru
    minimum).
14. **A name that duplicates another slot's head term**, or that one sentence can describe as
    another slot — the Repetitive Content risk, one field earlier.
15. **A name that only works in English** — its meaning or its length depends on English, the naming
    form of the localisation defect `.cursor/rules/visual-identity.mdc` already fails.
16. **A name that copies an incumbent's title with one word swapped** — a rename plus a new palette
    without a new job is the shape Play's Spam policy targets
    (`.cursor/rules/ideation-gate.mdc`).

## The contract

`naming.json` is authored at recon under `docs/ideation/plans/<slug>.naming.json` — beside the
proposal, the economics plan and the promise contract — and `scripts/new-slot.mjs` writes the
validated object verbatim into the slot root as `naming.json`, beside `economics.json` and
`differentiators.json`. The order matters: the decision is made **before the first line of UI**, and
the record is what the scaffold reads. A record written after the build contains only what was
built; naming has the same survivorship bias as the promise list, in one field.

```json
{
  "schemaVersion": 1,
  "slot": "<the slot's kebab-case slug>",
  "reconRef": "docs/research/<file>.md#<section>",
  "chosen": {
    "archetype": "carrier_job_noun",
    "carrier": "<the short ownable half>",
    "jobNoun": "<the two-to-three-word phrase a user actually types>",
    "score": 82,
    "dimensions": {
      "jobFit": 4.5,
      "searchability": 4.0,
      "distinctiveness": 4.0,
      "radioTest": 5.0,
      "uniqueness": 3.5,
      "lengthFit": 4.0,
      "localeSurvival": 4.5
    },
    "titles": [
      { "locale": "en-US", "title": "<Carrier>: <Job noun>", "length": 24 },
      { "locale": "ru-RU", "title": "<Carrier>: <Job noun>", "length": 27 }
    ]
  },
  "candidates": [
    { "name": "<the bare category word>", "archetype": "descriptive", "score": 31, "note": "negative control; the head term the leader owns" },
    { "name": "<candidate>", "archetype": "coined", "score": 64, "note": "loses the search half" },
    { "name": "<candidate>", "archetype": "compound", "score": 71, "note": "carrier too long for ru" },
    { "name": "<candidate>", "archetype": "metaphorical", "score": 68, "note": "metaphor not honest to the job" },
    { "name": "<chosen>", "archetype": "carrier_job_noun", "score": 82 }
  ],
  "rejected": [
    { "name": "<a name that was seriously considered>", "reason": "<why it lost, in one line>" }
  ],
  "collisions": {
    "playSearch": "<what the exact title and its close variants surfaced on Play>",
    "trademarkScreen": ["USPTO", "EUIPO eSearch plus / TMview", "WIPO Global Brand Database", "<ru national register>"],
    "checkedAt": "2026-10-01"
  },
  "slug": "<the kebab-case slug the decision derives>",
  "applicationId": "com.gexetr.<slug>",
  "assumptionsUnverified": ["<at least one honest unknown>"]
}
```

| Field | Rule |
|---|---|
| `schemaVersion` | Exactly `1`. |
| `slot` | The slot's kebab-case slug. The gate checks it against the directory name, so a record copied from another slot fails instead of passing as this slot's decision. |
| `reconRef` | `docs/research/<file>.md` or `docs/ideation/<file>.md`, optionally with `#section`. The file must exist. |
| `chosen.archetype` | One of the nine ids in the table above. |
| `chosen.carrier` | The ownable half. For `carrier_job_noun`, ≤ 12 characters (**ON RECORD** budget). |
| `chosen.jobNoun` | The two-to-three-word phrase users actually type. |
| `chosen.score` | `≥ 70` out of 100 — the weighted rubric below. |
| `chosen.dimensions` | All seven dimensions, each `≥ 2.5`; the weighted sum must equal `chosen.score`. |
| `chosen.titles` | One entry per declared locale (en and ru minimum), each measured at `≤ 30` characters with `≥ 5` characters of slack. |
| `candidates` | At least **5**, spanning at least **3 distinct archetypes**, each with `name`, `archetype`, `score` and a note. Includes the bare category word as a **negative control** that scores below every serious candidate. |
| `rejected` | At least **1** candidate with its reason. A field that eliminated nothing was not generated. |
| `collisions.playSearch` | What the exact title and close variants surfaced on Play. The record states plainly that a clean Play search is **not** clearance — Play does not enforce unique titles. |
| `collisions.trademarkScreen` | The registers screened: **USPTO**, **EUIPO (eSearch plus / TMview)**, **WIPO Global Brand Database** and the ru-market national register. Class **9** (downloadable software) is the base class. A registry screen is not legal clearance. |
| `slug` | Lowercase kebab case, 2–40 characters — the same rule `scripts/new-slot.mjs` validates. |
| `applicationId` | `com.gexetr.<slug>` via `scripts/lib/namespaces.mjs` (`applicationIdForSlug`, which drops dashes). It must be free in `registry/apps.json`. |
| `assumptionsUnverified` | At least one non-empty string that is not a placeholder. An empty array claims a name has no unknowns, which is not a decision — it is a preference. |

### The scoring rubric

Each survivor is scored 0–5 on seven weighted dimensions (adapted from a published brand-name
rubric, **ON RECORD**). **Require ≥ 70/100 overall with no dimension below 2.5**; otherwise return
to the field and generate more candidates rather than defending the one that is left.

| Dimension | Weight | The question to answer with evidence |
|---|---:|---|
| Job fit / specificity | 20 | does the title tell a user this app's *specific* job, not its category? |
| Searchability | 15 | does it contain the two-to-three-word phrase users actually type? |
| Distinctiveness / memorability | 15 | does it stand apart from the top three incumbents' titles? |
| Radio test / spelling-on-hearing | 15 | can someone who heard it once type it correctly? |
| Uniqueness / trademark headroom | 15 | is the Play and register field clear enough to own? |
| Length fit | 10 | does `<Carrier>: <Job noun>` fit 30 characters in en **and** ru with ≥ 5 slack? |
| Locale survival | 10 | does it mean something acceptable in every declared locale? |

The score does not outrank a hard check: a high-scoring candidate that collides with a registered
mark, or that means something unfortunate in ru, is eliminated regardless of its total.

### One decision, three derived things

The name, the slug, the `applicationId` and the store title are **one recorded decision with three
derived outputs**, and they must not be independent:

```text
name → slug → applicationId (com.gexetr.<slug>) → store title (≤ 30 chars, per locale)
```

They drift apart today because the scaffold takes them as **separate flags** — `scripts/new-slot.mjs
--slug <slug> --name "<Title>"` — and two independent inputs are exactly how a title, a directory and
a package end up describing three different things. From this contract the scaffold and the listing
are **read from the record**: the title from its locale strings, the slug from its `slug`, the package
from `applicationIdForSlug(slug)` — so a name change is a change to one file every downstream consumer
derives from, exactly as `store/studio.json` is one source its theme and its store pack are both
rendered from.

Two of the three are irreversible in different ways, and this is the last moment a name is free:

- **The `applicationId` is permanent and cannot be reused.** **MEASURED** (developer.android.com):
  once published, the application ID should never change; changing it makes Play treat the upload as
  a different app, and it cannot be reused even after unpublishing. A rename after the first upload
  is therefore a **new app** — new listing, no way to move the installs, ratings or reviews.
- **The store title stays editable.** It **can** be changed after publishing without losing installs
  or reviews so long as the `applicationId` and signing certificate stay the same (**ON RECORD**).
  But by then the carrier is already the brand, and a brand changed mid-flight has thrown away
  whatever recognition it had accumulated.
- **Before the first upload everything is reversible** — name, slug, directory, title,
  `applicationId` — which is precisely why the decision is made at recon, before the UI, alongside
  the promise contract, and not during `/release` when layout and components are fixed.

None of the free-text fields may be a placeholder. The validator rejects the literal values `tbd`,
`todo`, `n/a`, `none`, `unknown` and `placeholder`; any text containing `example.com`; angle-bracket
text such as `<carrier>`; any text containing `lorem`; and three or more consecutive `x` characters
as a whole word. The patterns are **imported** from `scripts/lib/slot-economics.mjs` rather than
restated, because a second copy of a rule is a second thing to drift.

## Enforcement

- **`scripts/new-slot.mjs` requires a naming record**, alongside `--economics` and
  `--differentiators`. It validates the record, prints one `BLOCKED` line per problem, and refuses to
  copy the scaffold — a slot without a valid recorded name is never created. On success the validated
  object is written verbatim to the slot root as `naming.json`, and its `slug`, `name` and
  `applicationId` are recorded in `.kiln-state.json` and `registry/apps.json` so the three cannot
  disagree.
- **The naming check's slot half is `slot.naming`.** It re-validates the record at verify time: the
  schema, the score floor and the seven dimensions, the candidate count and archetype spread, the
  rejection list, the measured title lengths per locale against the 30-character cap and the 5-
  character slack, the `reconRef` file, the collision record, and that `slug` and `applicationId`
  agree with the directory and `registry/apps.json`. It prints `VERDICT: PASS` or `VERDICT: BLOCKED`
  and exits non-zero on failure.
- **The rejection list is the enforcement surface.** Items 1–16 above are checked patterns, not
  advice: a bare category noun, a `My`/`Simple`/`Pro`/`Free`/`Best` modifier, the factory's own name,
  a number-only or emoji name, a stolen incumbent title, keyword stuffing, and a name that does not
  fit 30 characters in a declared locale all fail the gate.

## How it is verified

```bash
node scripts/verify.mjs                 # the invariants group runs naming.fixtures
node scripts/verify.mjs --only invariants   # just the sensor's own fixtures, for a fast inner loop
node scripts/verify.mjs --slot <slug>   # runs slot.naming for the slot
node scripts/verify.mjs --json          # the same report as JSON
```

- **`naming.fixtures`** runs in the **`invariants`** group, in the same process as every other core
  check, before any slot is read. Its inline fixtures drive the checker over synthetic records whose
  verdict is known and must include the legs that **fail**: a bare category noun, a candidate field
  spanning fewer than three archetypes, a name that overflows 30 characters in ru, a title with no
  rejection recorded, and a missing negative control. A sensor that cannot fail is not a sensor, and
  the failing legs are what prove this one can.
- **`slot.naming`** is the per-slot check `verify.mjs --slot <slug>` runs.
- **A missing record is never a pass.** The check treats the decision as declared once it is on
  disk **or** in git, so deleting `naming.json` from the working tree does not switch the check off;
  a slot with no naming record at all is reported `UNVERIFIED` naming the path, and `UNVERIFIED` is
  not `PASS`.

## What this gate does not prove

It proves the decision was **derived** and **legal**: the name came from a scored field that spanned
several archetypes, at least one alternative was rejected in writing, the rejections are absent, the
title fits every declared locale inside Play's cap with slack, and the slug, package and title are
one decision rather than three. It does **not** prove the name is beautiful, clever, memorable or
right — no program can judge that, and a score of 82 is arithmetic, not taste. It cannot prove the
name *causes* installs; no controlled experiment exists, and the strongest facts here describe what
winning listings look like, not what a name will do for this one. And a clean Play search and a
registry screen are **not** trademark clearance. The judgement that separates a distinctive name from
an ugly one stays with the owner, at checkpoint 1, exactly as it does for the visual identity and the
promise list — this gate's job is to make sure he is asked, with the reasoning on the table instead
of a name an agent liked.
