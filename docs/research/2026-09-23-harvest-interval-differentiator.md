# Pre-harvest-interval differentiator — viability evidence — 2026-09-23

> **The claim under test.** The product's differentiator is fixed as:
> *"You photograph a sick edible crop, get a diagnosis, a treatment plan, and **how long until the crop is safe to eat**."*
>
> That last clause is load-bearing, so this document tests the three things that decide whether it
> can ship: **does anyone already do it (store evidence), does the data exist under a usable
> licence (primary sources), and is advising on it safe and permissible (primary sources).**

**Evidence rules applied to this document.** Every factual claim carries a URL. Anything I could not
read from a primary source is marked **UNVERIFIED** inline and listed again in §6. I did not invent a
regulation, a licence, a dataset, a Play policy, or an interval value. Where a conclusion is my
reasoning from a source rather than the source's own statement, it says so.

**No interval numbers as guidance.** I found real pre-harvest-interval values in one dataset and
**do not reproduce any per-product value here**. Where values exist, this document says which field
holds them and how many rows are populated, which is a statement about data availability, not advice.

---

## 1. Question 1 — Do incumbents already do this?

### 1.1 How this was produced

Five queries screened with the repo's own validated library, unmodified:

```bash
# ratings-only screen, full visible result set per query
node scripts/scan-market.mjs \
  --query "pre harvest interval" --query "harvest interval pesticide" \
  --query "pesticide calculator" --query "spray interval vegetables" \
  --query "safe to eat after spraying" --json

# per-app facts (installs, declared ads/IAP) for the first 8 cards of each query
node scripts/enrich-market.mjs \
  --query "pre harvest interval" --query "harvest interval pesticide" \
  --query "pesticide calculator" --query "spray interval vegetables" \
  --query "safe to eat after spraying" --top 8 --concurrency 2 --json
```

Both runs exited 0, every query returned a visible result set, and all 40 selected app-detail pages
were fetched and parsed (`Failures: None`). **Neither library was modified.** Signed-out Play,
`hl=en`, `gl=US`, one session, run 2026-09-23.

### 1.2 Verdicts, and a contamination warning that matters

| query | verdict | seen | rated/total | weak (<4.2★) | median | is the result set on-topic? |
| --- | --- | --- | --- | --- | --- | --- |
| `pre harvest interval` | mixed | 30 | 13/30 | 5 | 4.2 | **no** — Play fuzzy-matched "harvest" |
| `harvest interval pesticide` | dead-zone | 30 | 10/30 | 4 | 4.2 | **no** — same "harvest" match |
| `pesticide calculator` | mixed | 30 | 11/30 | 4 | 4.2 | **partly** — 5 of the first 6 cards are pesticide calculators |
| `spray interval vegetables` | mixed | 12 | 9/12 | 2 | 4.3 | **mostly no** — games dominate |
| `safe to eat after spraying` | mixed | 9 | 9/9 | 3 | 4.6 | **no** — Play matched "eat" |

The contamination is the finding, not a nuisance, and it is visible in the raw cards:

- **`pre harvest interval`** returned `FarmTRX Harvest`, `Harvest Loop 3D`, `Harvest Trail`,
  `Season Harvest`, `Rain Harvesting`, `HarvestYield`, `Harvest: Track Time & Invoice`,
  `I HARVEST`, `Farm Harvest Day`, `Harvest CrossFit`, `Intervals`, `Harvest Ministries`,
  `Harvest Right`, `Harvest Run!`, `Harvest Sort`, `Harvest Jam`, `Daily Harvest` (3.3★),
  `Final Harvest`, `Story Harvest`… **not one card is about a pre-harvest interval.** The 30 visible
  results are a time-tracking SaaS, a bank, churches, and farm games.
- **`harvest interval pesticide`** produced the same app set plus `Harvest Bank Mobile`,
  `Harvest Church Lubbock`, `Farm Harvest Path`. Verdict `dead-zone` describes Play's text match,
  not a market.
- **`safe to eat after spraying`** returned only food/nutrition apps: `Spoonful: Diet & Food Scanner`,
  `Food Diary See How You Eat`, `My Plate Coach`, `Eat This Much`, `CamScanner`, `Chewing Diet`,
  `Fig: Food Scanner & Guide`, `Calorie Counter - MyNetDiary`, `SkinSAFE: Ingredient Scanner`.
  **Zero** cards are about spraying or produce safety.
- **`spray interval vegetables`** returned `Brawl Stars`, `Plants vs. Zombies`, `Rise of Kingdoms`,
  `Flipkart`, `Arena Breakout`, `MARVEL SNAP`, `Lost Light`. Two of twelve cards are relevant.

**What this means.** Play's search is a fuzzy text match, so three of the five queries returned
nothing on-topic and two returned a mixture. No verdict above is evidence about a
pre-harvest-interval market; the usable evidence is the residual on-topic cards, listed next.

### 1.3 The on-topic incumbents the screen actually surfaced

| card | package id | title | developer | installs | rating | ads / IAP |
| --- | --- | --- | --- | --- | --- | --- |
| `pesticide calculator` #1 | `in.res.ccari.pesticidecalc` | Pesticide Calculator | **ICAR CCARI** | 10K+ | unrated | false / false |
| #2 | `fiverr.hnronline.calculatormixing.activity` | Oil Palm Pesticide Calculator- | Devarajen Rajagopal | 10K+ | unrated | false / false |
| #3 | `com.nanohawk.pestmath` | PestMath: Pesticide Calculator | Swamp Ventures LLC | 100+ | unrated | **true / true** |
| #5 | `com.hydroponicglass.pesticide_dilution_calculator` | Pesticide Mix Calculator & Log | HydroponicGlass | 10+ | unrated | true / false |
| #6 | `com.h3apps.pesticideformulas` | Pesticide Calculator & Formula | H3 Apps | 10+ | unrated | false / false ($2.99) |
| #30 (card only, not enriched) | `ca.gc.hcsc.pmra` | Pesticide Labels | **Health Canada \| Santé Canada** | — | unrated | — |
| `spray interval vegetables` #1 | `com.cabi.cabi_dosage_calc_android` | **Crop Sprayer** | **CABI** | **100K+** | unrated | false / false |
| `spray interval vegetables` | `cn.danatech.xingseus` | PictureThis - Plant Identifier | Glority Global Group Ltd. | 50M+ | 4.5 | false / true |
| `spray interval vegetables` | `com.growit.ai` | GrowIt: Vegetable Garden Care | Glority Global Group Ltd. | (not enriched) | 4.3 | — |

Two things follow from the install buckets alone. First, **every pesticide-timing-adjacent app found
here is unrated and small** (10+ to 10K+), except one: **CABI's `Crop Sprayer` at 100K+, unrated,
free, with no ads and no in-app purchases** — an institutional publisher shipping a crop-protection
tool at scale without monetising it. Second, **PictureThis (50M+) does appear in the
`spray interval vegetables` result set**, so the generalist is not absent from this phrasing.

### 1.4 What the incumbents' store pages actually say

`scripts/lib/play-app-detail.mjs` — the validated fetcher — **does not expose the listing
description**: it parses title, developer, rating, rating count, installs, updated date, price and
the ads/IAP badges, deliberately, and its module header says so. So the detail pass could not answer
the feature question. To answer it I read **seven store pages directly** and report what their own
listing text says. This is still *store-page* evidence: it is what the developer chose to advertise,
**not** what the app does. I did not install or use any of these apps.

| app | what its listing text advertises | any pre-harvest-interval / safe-to-eat claim? |
| --- | --- | --- |
| [PestMath: Pesticide Calculator](https://play.google.com/store/apps/details?id=com.nanohawk.pestmath) | tank-mix, broadcast/perimeter/crack-and-crevice rates, "units used on EPA labels and SDS documents", planned "SDS linking and digital documentation"; aimed at "pest control professionals, PCOs, and agricultural applicators" | **no** — nothing about harvest timing or residues |
| [Pesticide Mix Calculator & Log](https://play.google.com/store/apps/details?id=com.hydroponicglass.pesticide_dilution_calculator) | dilution ratios, tank presets, spray log, "order of addition" | **no** — and it ships an explicit label-first disclaimer (quoted in §3.6) |
| [ICAR-CCARI Pesticide Calculator](https://play.google.com/store/apps/details?id=in.res.ccari.pesticidecalc) | "Calculate how much pesticide you will need"; litres/hectares/acres | **no** — dosage only, plus a liability disclaimer |
| [CABI Crop Sprayer](https://play.google.com/store/apps/details?id=com.cabi.cabi_dosage_calc_android) | concentrate amount, total product, tanks required, sprayer-size adjustment; offline | **no** |
| [PictureThis](https://play.google.com/store/apps/details?id=cn.danatech.xingseus) | "Plant Disease Auto Diagnose & Cure … diagnose the disease and offer **treatment advice** … step-by-step treatment plan"; toxic-plant warning for pets/children | **no** — no harvest interval, no residue, no "safe to eat" |
| [Plantix](https://play.google.com/store/apps/details?id=com.peat.GartenBank) | "detect pests and diseases … get **recommended treatments**"; fertilizer calculator; "Agri Weather Forecast: Know the best time to **weed, spray and harvest**" | **no PHI claim** — but see the note below |
| [Agrio](https://play.google.com/store/apps/details?id=com.agrio) | "**pest life-cycle modeling** to help you optimize the timing of pesticide applications"; "Track intervention reminders"; IPM protocols | **no** — application timing, not eating safety |

**Genuinely close calls, stated rather than smoothed over** — I am not going to claim these apps lack
the feature, because a store page cannot show that:

- **Plantix's "best time to weed, spray and harvest"** is presented as a *weather forecast* feature
  alongside an "Agri Weather Forecast" heading. That is weather-driven advisory timing. Whether it
  internally computes a label pre-harvest interval is **not knowable from the listing**, and I am
  not asserting either way.
- **Agrio models "the timing of pesticide applications"** and sends "intervention reminders". Timing
  of application is the input to a pre-harvest interval, not the interval itself, but the two are one
  design decision apart.
- **Health Canada's `Pesticide Labels` app** (`ca.gc.hcsc.pmra`) appeared as card 30 of
  `pesticide calculator` and was **not** enriched (top-8 cut), so I can say only that a national
  regulator ships a label-looking-up app. Its store page was not read for this document.

### 1.5 Verdict on Question 1

**A pre-harvest-interval / food-safety-timing feature is not visible anywhere in the store presence
of the incumbents I could inspect.**

What I established: across 5 queries and 111 visible cards, **no title** referenced a pre-harvest
interval, a harvest interval, a spray-to-harvest wait, or whether produce is safe to eat; the queries
that should have surfaced such an app returned fuzzy matches instead; and the 7 listing descriptions
I read contain **no** such claim, while four of them (PestMath, Pesticide Mix Calculator, ICAR
calculator, CABI Crop Sprayer) are explicitly *dosage* calculators.

What I could **not** establish: whether any of these apps contains such a feature unadvertised.
Judged from the store page only — title, listing text, developer, badges, install bucket. **A store
page proves what an app advertises, never what it does.** Absence of the claim is evidence of
absence of *positioning*; it is not evidence of absence of the feature, and it is not evidence that
users want it.

---

## 2. Question 2 — Does the data exist, and under what licence?

The short answer: **yes, but nationally and unevenly, and not at EU or global level.**

### 2.1 What a pre-harvest interval actually is (so the dataset question is well-posed)

- US EPA, *OPP Label Review Manual*, Chapter 11 (Dec 2024): the PHI is a **regulatory interval** the
  label reviewer must require where a residue-chemistry assessment identified one, and it "should be
  indicated as numbers of weeks or days", with a "pre-harvest interval (PHI) for all uses".
  [epa.gov — LRM Ch. 11](https://www.epa.gov/system/files/documents/2024-12/lrm-chapter11_2024.pdf)
- US EPA glossary: "PHI (Pre-Harvest Interval) — The interval between the last application of
  pesticide and harvest of the crop."
  [archive.epa.gov — CARES Technical Manual, Appendix A](https://archive.epa.gov/scipoly/sap/meetings/web/pdf/tmanual.pdf)
- European Commission, MRL guideline appendix: pre-harvest intervals are "the time between the last
  application of a plant protection product and harvest or the earliest possible use of the treated
  product", are "the result primarily of the needs of agricultural practice and are no shorter than
  Good Agricultural Practice (GAP) requires", and are "set by an experienced expert" in the
  authorisation process.
  [food.ec.europa.eu — Appendix I](https://food.ec.europa.eu/system/files/2016-10/pesticides_mrl_guidelines_app-i.pdf)

Three consequences that shape the data question: the interval is **per product × per crop × per
use**, it is **set nationally**, and it is a **legal use restriction**, not a measured residue
outcome. See §3.7 for why that last distinction breaks the product's wording.

### 2.2 CONFIRMED: France — E-Phy (Anses), with the interval as a named, populated field

This is the strongest single finding in this document, because I verified it from the published
file itself rather than from documentation about it.

- **Publisher / portal:** Anses (the French agency for food, environmental and occupational health
  safety), via the [E-Phy catalogue](https://ephy.anses.fr).
- **Dataset page:** [data.gouv.fr — Données ouvertes du catalogue E-Phy](https://www.data.gouv.fr/datasets/donnees-ouvertes-du-catalogue-e-phy-des-produits-phytopharmaceutiques-matieres-fertilisantes-et-supports-de-culture-adjuvants-produits-mixtes-et-melanges)
- **File I downloaded and inspected** (2026-09-23): `decisionamm-intrant-format-csv-20260922-utf8.zip`
  from `static.data.gouv.fr`, dataset dated **2026-09-22**, updated **weekly** (Tuesday night).
- **Licence:** **Licence Ouverte / Open Licence** (Etalab), as declared in the dataset's own
  `Licence` field. The licence text grants, "à des fins commerciales ou non, dans le monde entier et
  pour une durée illimitée", the right to reproduce, adapt, transform, publish, redistribute and
  "l'exploiter à titre commercial … en l'incluant dans votre propre produit ou application", subject
  to **attribution** (source + last-update date) and **not altering the meaning** of the data.
  [data.gouv.fr — Licence Ouverte 2.0](https://www.data.gouv.fr/pages/legal/licences/etalab-2.0) ·
  [full text, PDF](https://www.justice.gouv.fr/sites/default/files/2023-04/ETALAB-Licence-Ouverte-v2.0.pdf)
  **Commercial use in a shipped app is permitted**, with attribution.
- **The field, verified by reading the CSV header and counting rows** — file
  `produits_usages_utf8.csv`, which I opened after unpacking:

  | column (as published) | meaning |
  | --- | --- |
  | `numero AMM` | national marketing authorisation number |
  | `identifiant usage` | the use (product × crop × target) |
  | **`delai avant recolte jour`** | **pre-harvest interval, in days** |
  | **`delai avant recolte bbch`** | pre-harvest interval expressed as a growth stage instead of days |
  | `nombre max d'application` | max applications |
  | `intervalle minimum entre applications (jour)` | minimum interval between applications |
  | `condition emploi`, `ZNT …` | conditions of use; buffer zones |

  Observed magnitudes: **81,615 usage rows**, of which **17,866 carry a value in
  `delai avant recolte jour`** and **139 carry the BBCH growth-stage notation instead** — so the
  interval is *not always a number of days* in the source. Across 15,151 product rows, the
  `gamme usage` field is `Professionnel` in 3,211 rows and
  `Amateur / emploi autorisé dans les jardins` in 360 rows (11,580 blank). **I did not export or
  reproduce a single per-product interval value into this document.**
- **API or bulk?** **Bulk download** (CSV and XML, weekly). No API is documented on the dataset page;
  third-party REST wrappers exist but are not official.

### 2.3 CONFIRMED: Germany — BVL PSM-API, with the waiting period as a table

- **Publisher:** BVL (Bundesamt für Verbraucherschutz und Lebensmittelsicherheit), the German
  federal office. Announcement: since April 2024 the authorisation data is served as a **REST API in
  OpenAPI**, updated **monthly**, and **"gebührenfrei" (free of charge)**.
  [bvl.bund.de — PSM-API announcement](https://www.bvl.bund.de/SharedDocs/Fachmeldungen/04_pflanzenschutzmittel/2024/2024_04_03_Fa_PSM_API.html)
- **Endpoint:** `https://psm-api.bvl.bund.de/`. The vendor-facing description states the target
  audience is "Unternehmen, Institutionen und versierte Privatpersonen, welche die Daten
  weiterverarbeiten" — companies and institutions that **further process** the data.
- **The interval field:** the official API explanation document specifies table **`AWG_WARTEZEIT`**
  and **`AWG_WARTEZEIT_AUSG_KULTUR`** — the assignment of an *application* (`awg_id`) to a **waiting
  period in days per culture** (`kultur`), with excepted cultures handled by the second table. The
  same document records that the `AWG` table carries `huk` (**whether the use is authorised for house
  and kitchen gardens**) and `anwenderkategorie` (`beruflich` / `nichtberuflich`).
  [bvl.bund.de — PSM-API Erläuterungen, PDF](https://www.bvl.bund.de/SharedDocs/Downloads/04_Pflanzenschutzmittel/PSM_API_Erlaeuterungen.pdf)
- **Licence:** **Datenlizenz Deutschland – Namensnennung – Version 2.0 (dl-de/by-2-0)**, listed with
  "freie Nutzung" (free use) for the PSM-API on the BMEL agricultural data platform, whose publishing
  organisation is named as BVL.
  [landwirtschaftsdaten.de — Online-Datenbank Pflanzenschutzmittel](https://www.landwirtschaftsdaten.de/detailansicht.html?cHash=19a301a919532a87a6d9032709fa794d&tx_epadpsearch_showdetails%5Baction%5D=show&tx_epadpsearch_showdetails%5Bcontroller%5D=Search&tx_epadpsearch_showdetails%5Bgeodata%5D=54)
  That licence text expressly permits commercial use and embedding in products and applications,
  subject to a source note.
  [govdata.de — DL-DE→BY-2.0](https://www.govdata.de/dl-de/by-2-0)
  **Caveat:** the licence is asserted in the **BMEL platform's metadata**, not on the BVL API page
  itself. I did not find a licence statement inside the BVL API documentation or on
  `psm-api.bvl.bund.de`. Treat "this specific API is dl-de/by-2-0" as **licensed per the platform
  metadata, but not confirmed by BVL on the API page itself** — verify before shipping.
- **API or bulk?** **API.** The MS-Access bulk file was **discontinued in January 2026** and is
  "vollständig von der PSM-Programmierschnittstelle (PSM-API) abgelöst" — fully replaced by the API.
  [bvl.bund.de — Zulassungsdaten im Datenbankformat](https://www.bvl.bund.de/DE/Arbeitsbereiche/04_Pflanzenschutzmittel/01_Aufgaben/02_ZulassungPSM/01_ZugelPSM/06_Datenbank/psm_ZugelPSM_Datenbank_basepage.html)

### 2.4 ESTABLISHED NEGATIVE: the EU has no central pre-harvest-interval dataset

The Commission's own description of the **EU Pesticides Database** scopes it to **active substances**
(approved / not approved / basic substances), **MRLs in food products**, and **emergency
authorisations** under Article 53 of Regulation (EC) No 1107/2009. **Pre-harvest intervals are not
among them.** The Commission states the database "has no legal value".
[food.ec.europa.eu — EU Pesticides Database](https://food.ec.europa.eu/plants/pesticides/eu-pesticides-database_en)

This is consistent with the EC's own guideline, which places the setting of PHIs in the national
authorisation process and in Good Agricultural Practice (§2.1). **The two confirmed datasets are
national implementations, not EU aggregations.**

> **UNVERIFIED:** the licence for the EU Pesticides Database itself. I did not read a licence
> statement on that page, and the CC BY 4.0 attribution I saw came from a third-party mirror, not
> from the Commission — so I am not repeating it as fact. Irrelevant to the PHI feature either way,
> since the database does not contain PHIs.

### 2.5 US: the interval is in the label, not in the structured register

- **The structured register does not carry it.** EPA's **Pesticide Product Information System
  (PPIS)** is the downloadable, machine-readable US register. Its own field list is: registrant name
  and address, chemical ingredients, toxicity category, product names, distributor brand names,
  site/pest uses, pesticidal type, formulation code, and registration status — **no pre-harvest
  interval field**.
  [epa.gov — PPIS](https://www.epa.gov/ingredients-used-pesticide-products/pesticide-product-information-system-ppis) ·
  [PPIS data dictionary, PDF](https://www.epa.gov/sites/default/files/2021-01/documents/ppis-data-dictionary.pdf)
  I verified the absence by searching that data dictionary's text for `harvest`, `interval`, `PHI`
  and `restricted`: the only match of interest is `RUP_FLAG`, the restricted-use-product flag. There
  is no PHI column.
- **Where it does live:** EPA's **Pesticide Product Label System (PPLS)**, "over 170,000 current and
  historical labels converted to text-searchable PDF files", linkable per registration number, with
  an application programming interface referenced by EPA.
  [epa.gov — PPLS: More Information](https://www.epa.gov/pesticide-labels/pesticide-product-label-system-ppls-more-information)
  A PHI is a line of prose inside a PDF, labelled per crop and use. Vendoring 170,000 PDFs is not a
  dataset; deriving a structured field means extracting text from free-form label prose.
- **Structured US PHI data is sold, not published.** A commercial crop-input data vendor advertises
  "Pre-harvest interval" among the structured fields in its API database, alongside re-entry
  intervals, crops, pests, rates and geography.
  [compassregulatory.com — crop input database](https://compassregulatory.com/blog/crop-input-database)
  This is a **vendor's marketing page**, used here only as evidence that a paid market for structured
  PHI exists — **not** as a licence offer or a price.
- **Licence, if we vendored labels:** US federal works created by government employees in the course
  of their duties are not protected by copyright (17 U.S.C. §105, as quoted in EPA's own publication
  review procedures, which also state that works created under contract or grant **may** be
  copyrighted).
  [nepis.epa.gov — Publication Review and Clearance Procedures](https://nepis.epa.gov/Exe/ZyPURL.cgi?Dockey=940006NS.TXT)
  **UNVERIFIED:** whether a pesticide label — drafted and submitted by a *registrant company* — is a
  "work of the United States Government" for reuse purposes. I did not read a licence statement
  covering label text, and EPA's guidance explicitly warns that contractor-created material may be
  protected.

### 2.6 UK: register confirmed, interval field UNVERIFIED

HSE publishes a pesticide register and a **garden** database (searchable by plant/area, product,
MAPP number, company, active substance, field of use) covering products authorised in the
home/garden/allotment.
[hse.gov.uk — Pesticides Register Database user guide](https://www.hse.gov.uk/pesticides/databases/new-pesticides-register-database-user-guide.htm) ·
[HSE Garden Pesticide Search](https://secure.pesticides.gov.uk/garden/)
**UNVERIFIED:** whether that register exposes a pre-harvest interval as a structured field. The user
guide's listed search fields do not include one, but HSE also routes use instructions to the label,
and I did not retrieve a product record to confirm what a details page shows. **Do not treat the UK
as a confirmed data source on this evidence.**

### 2.7 Summary table

| jurisdiction | dataset | interval present? | licence | API or bulk | confidence |
| --- | --- | --- | --- | --- | --- |
| **FR** | E-Phy (Anses) | **yes** — `delai avant recolte jour` / `…bbch`, 17,866 of 81,615 rows | **Licence Ouverte (Etalab)** — commercial use permitted, attribution required | bulk CSV+XML, weekly | **high** — read the field in the published file |
| **DE** | BVL PSM-API | **yes** — `AWG_WARTEZEIT` per application × culture | dl-de/by-2-0 per BMEL platform metadata; not stated on the API page itself | REST API, monthly, free | **high** for the field, **medium** for the licence |
| **EU** | EU Pesticides Database | **no** — substances, MRLs, emergency authorisations only | **UNVERIFIED** | download + APIs | high (scope) |
| **US** | PPIS | **no** — no PHI field | US federal works generally public domain; label-text status **UNVERIFIED** | ASCII files | high (absence) |
| **US** | PPLS labels | only as PDF prose across 170,000+ labels | **UNVERIFIED** for label text | per-label URLs + API | high (form) |
| **US** | commercial aggregators | **yes**, sold | commercial, terms unknown — no price read | API | medium (vendor claim) |
| **UK** | HSE registers | **UNVERIFIED** | not read | web DB | low |

**Jurisdiction conclusion.** The interval is **national**. There is no EU-level dataset and no global
one; France and Germany each required a separate national source with a different field name, a
different delivery mechanism and a different licence. A truthful feature therefore **cannot be
global** — it can only cover jurisdictions the app actually sources, and must name the jurisdiction
on every answer. France and Germany are the only two I confirmed with the interval as a
**structured, licensable field**; the US has the data but as label prose or a paid licence; the UK
is unverified.

---

## 3. Question 3 — Is advising on this safe and permissible?

### 3.1 The label is the legal instrument, in every jurisdiction I read

- **US.** "It is a violation to use a registered pesticide in a manner inconsistent with its
  labeling, FIFRA § 12(a)(2)(G)."
  [epa.gov — Pesticide Labeling Questions & Answers](https://www.epa.gov/pesticide-labels/pesticide-labeling-questions-answers)
- **EU.** The Commission places the PHI-setting and GAP analysis inside the national authorisation
  process (§2.1); the Sustainable Use Directive's point-of-sale duty is on the **distributor with a
  certificate**, who must "provide adequate information to customers as regards pesticide use,
  health and environmental risks and safety instructions", not on any third party.
  [eur-lex.europa.eu — Directive 2009/128/EC, Art. 6](https://eur-lex.europa.eu/eli/dir/2009/128/oj/eng)
- **UK.** "By law, all users of professional pesticides must be adequately trained and must follow
  the product label. **The product label provides appropriate guidance for use of amateur
  (non-professional) pesticides in the home and garden.**" Amateur products require no training
  because "the label will be worded so that the instructions are easy to follow".
  [hse.gov.uk — Pesticides guidance for the general public](https://www.hse.gov.uk/pesticides/using-pesticides/members-of-public.htm) ·
  [hse.gov.uk — Using pesticides in your garden, home or allotment](https://www.hse.gov.uk/pesticides/using-pesticides/garden-home.htm)

**Reading:** in all three, the duty to comply sits on the **user**, and the authoritative source is
the **label**; the registrant and the regulator produce that label. Restating a published interval
accurately is not itself an act of applying a pesticide. That is the favourable half.

### 3.2 The unfavourable half: "adviser" is a regulated or certified role in two of the three

- **EU — explicit.** Directive 2009/128/EC **Article 2(3)**: "'advisor' means any person who has
  acquired adequate knowledge and advises on pest management and the safe use of pesticides, **in
  the context of a professional capacity or commercial service**, including private self-employed and
  public advisory services, commercial agents, food producers and retailers where applicable."
  **Article 5** requires Member States to ensure all "professional users, distributors and advisors"
  have training and that certification systems exist; **Article 14(2)** requires them to ensure
  *professional users* have "advisory services on integrated pest management".
  [eur-lex.europa.eu — Directive 2009/128/EC](https://eur-lex.europa.eu/eli/dir/2009/128/oj/eng)
  Note the definition's own qualifier: **professional capacity or commercial service**. A consumer
  app is not a professional advisory service — but an app is unambiguously a **commercial service**,
  so the boundary is a legal reading, not a safe assumption.
- **US federal — no rule found, so marked UNVERIFIED.** I found **no** FIFRA provision licensing the
  act of advising a home grower. FIFRA's prohibition I read is on *using* a registered pesticide
  inconsistently with its label (§3.1), and its training/certification machinery attaches to
  **restricted-use** products and applicators. **UNVERIFIED:** that no federal rule constrains
  app-delivered advice — I did not read FIFRA in full, and "I did not find it" is not "it does not
  exist."
- **California — a licensing regime that keys on "agricultural use".** Food and Agricultural Code:
  - §11410: "'Agricultural pest control adviser' means **any person who offers a recommendation on
    any agricultural use**, who holds himself or herself forth as an authority on any agricultural
    use, or who solicits services or sales for any agricultural use."
  - §11411: "'Recommendation' means the giving of **any instruction or advice on any agricultural use
    as to any particular application on any particular piece of property**. 'Recommendation' does not
    include any summary that does not specify the use to be made for any designated pest, provided
    that such summary is not in conflict with any registered pesticide label…"
  - §12001: "No person shall act, or offer to act, as an agricultural pest control adviser without
    first having secured an agricultural pest control adviser license."
  - §11408: "'Agricultural use' means the use of any pesticide or method or device for the control of
    plant or animal pests… **It excludes the sale or use of pesticides in properly labeled packages or
    containers that are intended for … (a) Home use.**"
  [leginfo.legislature.ca.gov — FAC Division 6, Chapter 1](https://leginfo.legislature.ca.gov/faces/codes_displayText.xhtml?article=&chapter=1.&division=6.&lawCode=FAC&part=&title=)
  CDPR's own compendium confirms the licensing test and the three criteria.
  [cdpr.ca.gov — Compendium Vol. 1, Ch. 3](https://www.cdpr.ca.gov/wp-content/uploads/2026/04/volume_1_chapter_3.pdf)
  **Reasoning, marked as reasoning:** because "agricultural use" **expressly excludes home-use
  products**, a tool limited to **home edible gardens** sits outside a regime built on "agricultural
  use". But **UNVERIFIED:** whether a commercial app that a grower also uses on a market patch is
  captured, and whether an app that "holds itself forth as an authority" can be a "person". A
  California user with a farm plot could pull the product into §11410 on the plain text of "any
  person who … holds himself forth as an authority". This is a real, unresolved exposure, and §11411's
  carve-out is narrow: a "summary that does not specify the use … for any designated pest" and that
  "is not in conflict with any registered pesticide label".
  **UNVERIFIED:** whether any *other* US state licenses pesticide advice. I did not survey the states.

### 3.3 What Play policy requires — and what it does not

I searched the text of Google Play's **Developer Program Policy** for a provision specific to
pesticides, agriculture or food safety. **There is none.** The nearest applicable policies are:

1. **Health Content and Services → Health and Medical Apps.** "If your app offers health-related
   features or information as part of its functionality … it must comply with the existing Google
   Play Developer Policies, including Privacy, Deception and Device Abuse", plus: "We don't allow apps
   with health and medical related functionalities that are **misleading or potentially harmful**";
   and "Other health and medical apps must include a clear disclaimer in their app description
   indicating that the app is '**not a medical device and does not diagnose, treat, cure, or prevent
   any medical condition**.'" and "Apps must also remind users to consult a healthcare professional…"
   [support.google.com — Developer Program Policy](https://support.google.com/googleplay/android-developer/answer/17105854)
2. **The health apps declaration is mandatory for every app.** "All developers that have an app
   published on Google Play must complete the Health apps declaration … Even developers with apps that
   do not offer any health features must complete this form and certify that no health features are
   offered through the app."
   [support.google.com — Health apps declaration form](https://support.google.com/googleplay/android-developer/answer/14738291)
3. **Play's own scope for "health" includes nutrition, and lists `CAMERA`.** "Health apps encompass a
   wide range of applications that are designed to support and improve users' health, well-being
   (mental and physical)…"; "health and fitness apps … inform or let users track or sync information
   about their personal health and fitness … in areas such as fitness, **nutrition**, wellness, and
   sleep"; and the permissions "considered in scope of health related sensitive data" include
   **`CAMERA`**, alongside location, body sensors and activity recognition. Best practice for an app
   making a health claim is to disclose its purpose, its claimed benefits, **"the basis of claim made
   (for example, evidence, best practices, standards)"**, the intended users, the risks, and "all
   required disclaimers and warnings".
   [support.google.com — Health app categories and additional information](https://support.google.com/googleplay/android-developer/answer/13996367)
4. **Deceptive Behavior → Misleading Claims.** "We don't allow apps that contain false or misleading
   information or claims, including in the description, title, icon, and screenshots."
   [support.google.com — Developer Program Policy](https://support.google.com/googleplay/android-developer/answer/17105854)
5. **AI-Generated Content.** "Apps that generate content using AI must contain in-app user reporting
   or flagging features that allow users to report or flag offensive content to developers without
   needing to exit the app."
   [support.google.com — Developer Program Policy](https://support.google.com/googleplay/android-developer/answer/17105854)

**Reading, stated as reading.** A photo-based crop app that answers a food-safety question touches
Play's health framing on two independent hooks — **nutrition/well-being** and the **`CAMERA`**
permission — so the health declaration and the disclosure expectations look **likely to apply**, and
the app must be described honestly under Misleading Claims. But the disclaimer Play prescribes for
non-device health apps is written for **human medicine** ("not a medical device … does not diagnose,
treat, cure, or prevent any medical condition", "consult a healthcare professional"). Applied
literally to a crop app it is incoherent, and Play does not publish a food/pesticide-specific
disclaimer. **UNVERIFIED:** how Play reviewers treat an agriculture app that declares itself under
Health → "nutrition/wellness", and whether the medical disclaimer wording is enforced for a
non-medical claim. The **UNVERIFIED** here is a submission risk, not something I can settle from the
published policy.

### 3.4 No Play policy prohibits this — say it plainly

**UNVERIFIED:** any Google Play policy that specifically governs pesticide, agricultural or
food-safety advice. I found none, and I searched the Developer Program Policy text for the obvious
terms. "No specific policy found" is the honest statement; it is not a claim that none exists
elsewhere in Play's corpus (Help Center, review-notes requirements, AI-content declarations, or
region-specific rules I did not enumerate).

### 3.5 Restating an interval vs recommending a product

The distinction appears in the sources I read, and it is the crux of the risk:

| | restating a published interval | recommending a product |
| --- | --- | --- |
| what is being asserted | "the label for product X on crop Y states an interval of Z" — a citation of a regulatory artifact | "apply X to your crop, and here is the plan" — a pest-management recommendation |
| who owns the underlying decision | the registrant and the regulator who approved the label | the app, as adviser |
| the signals in the sources | CA §11411 carves out "any summary that does not specify the use … for any designated pest" that does not conflict with the label; US liability attaches to *use* inconsistent with the label; HSE routes the consumer to the label and says it "cannot give advice on which products control which pest or disease" | CA §11410 captures "any person who offers a recommendation on any agricultural use"; EU Art. 2(3) defines an "advisor" who advises "on pest management and the safe use of pesticides" in a professional/commercial capacity; EU Art. 5 certifies advisers |
| residual risk | mis-citation, staleness (labels change; the app may hold a superseded interval), and implying regulatory endorsement | the app becomes the adviser of record, and a wrong recommendation is a wrong instruction about a substance on food |

Two consequences worth stating because they are not obvious:

- **The licence terms constrain the *claim*, not just the download.** E-Phy requires that the data's
  meaning not be altered and that the source and last-update date be shown; BVL's licence requires a
  source note. An app that shows a bare number with no jurisdiction, no source and no date is
  arguably non-compliant with its own data licence *and* misleading under Play's Misleading Claims
  policy at the same time.
- **Even the regulator declines this advice.** HSE: "HSE cannot give advice on which products control
  which pest or disease. You must ask for this information when you're buying the product or check on
  the label of the container." That is the same posture §3.1 describes, stated by an authority.

### 3.6 This framing already ships, and I found real examples

The safest framing is not theoretical — it is what existing apps in this exact category already do.
Verbatim from store pages read for §1.4:

- `Pesticide Mix Calculator & Log`: *"**Results in this app are for reference only. Always follow the
  label on the pesticide container for the actual dose and method, and check your national pesticide
  safety information service.** This app does not judge whether products can be mixed together. …
  This is an independent app made by an individual developer and is not affiliated with any government
  body or agricultural organisation."*
- `ICAR-CCARI Pesticide Calculator`: *"Disclaimer: ICAR-CCARI does not accept responsibility for any
  loss which may arise from reliance on the software or materials published on this site."*

Note both are **calculators that take the user's own inputs** — neither supplies the interval. That is
the design that has reached the store in this category. It is evidence that the framing is accepted;
it is not evidence that it is legally sufficient, which is §6's problem.

### 3.7 The claim as worded is stronger than the data supports — a finding, not an aside

The owner's sentence says **"how long until the crop is safe to eat"**. The interval that exists in
the data is a **pre-harvest interval**: a legal *use restriction* on the label, defined by the
regulators as "the interval between the last application … and harvest" and as something "no shorter
than Good Agricultural Practice (GAP) requires" (§2.1). It is **not** a residue measurement, and it
says nothing about:

- what happens if the interval is under-run, a different product or rate was used, or the product was
  mixed with something else;
- the **MRL** — the actual residue standard, which is a *separate* instrument (the EU database that
  does carry MRLs carries no PHIs, §2.4);
- whether washing, peeling or cooking changes the exposure;
- the fact that the interval is a **per-product, per-crop, per-use** value that only exists for a
  product the user actually applied.

So an app that reports the interval and says "safe to eat" is **overclaiming**: it would be
converting a use restriction into a safety guarantee. Restated correctly, the app can answer
*"your label's minimum interval has not yet elapsed"*; it cannot answer *"it is safe to eat"* on the
strength of the interval alone. **This is the single most important product finding in this
document**, and it is why §4's fallbacks are built around the label rather than around a safety
verdict.

### 3.8 Framings, with their risk — I am not choosing for you

| # | framing | what the app asserts | risk profile |
| --- | --- | --- | --- |
| **A** | **Interval restated with provenance.** "Official interval for [product] on [crop] in [jurisdiction]: [value]. Source: [register], updated [date]. Your product label is authoritative; read it before you harvest." | a citation of a published regulatory fact | **Medium.** Needs a per-jurisdiction dataset, correct attribution, staleness handling, and no imperative language. Stays on the "restating" side of §3.5. Requires the licence attribution the data actually demands. |
| **B** | **Label-first tracker (no supplied number).** The user reads the interval off **their own label** and enters it; the app counts down, logs the spray, and tells them when *their own entered* interval has elapsed. | a calculator over the user's own input | **Lowest.** The app never supplies a pesticide value, so there is no dataset licence and no mis-citation to defend, and it is the pattern that already ships (§3.6). Weakens the differentiator only in that the user must do one lookup. |
| **C** | **Route to authority.** Photograph → diagnosis → treatment plan → "before you eat it, check the interval for your product on [official register] / on your label". No countdown, no number. | a pointer | **Low.** Closest to HSE's own posture. Yet it does **not** answer "how long until it is safe to eat", so it does not deliver the differentiator — it delivers the *promise* of the answer. |
| **D** | **The claim as worded.** "This crop is safe to eat in N days." | a safety verdict | **Highest.** Overclaims beyond a use restriction (§3.7); sits directly under Play's "misleading or potentially harmful" health clause (§3.3); and pulls the app toward the "adviser" role in CA §11410 and EU Art. 2(3) (§3.2). |

**A plausible combination:** B or A as the engine, capped by jurisdiction, with C's provenance
display mandatory. But the choice between "restate the number" and "only count the number the user
entered" is a legal and product decision, not a research one — I am presenting it, not settling it.

---

## 4. Question 4 — Kill criterion and fallback

### 4.1 The differentiator is dead if any of these hold

1. **No licensed source for the target market.** Target market = the storefronts the app ships to. If
   France and Germany are the only confirmed structured sources (§2.2–2.3), then for the US — the
   market the app would launch in — there is **no free structured dataset**, only 170,000+ label
   PDFs (§2.5). If a legal read concludes that deriving the field from labels is impractical or that
   the label text cannot be vendored, **and** the commercial licence is not affordable, then **the
   feature cannot be truthfully sourced for the launch market**. That alone kills the direct claim.
2. **The claim has to be "safe to eat".** §3.7 shows the interval is a use restriction, not a safety
   determination. If the product requires the literal sentence "how long until the crop is safe to
   eat", then the differentiator is dead **regardless of the data**, because no dataset in §2
   supports that assertion. This is the sharpest kill criterion, and it does not depend on any
   jurisdiction's law.
3. **A competent authority says the output is a recommendation.** A written answer from California
   DPR, a state agricultural commissioner, or an EU member state authority that an app which matches
   a product to a crop and returns an interval is giving a "recommendation on agricultural use"
   (CA §11410) or is advising "on pest management and the safe use of pesticides" in a commercial
   service (EU Art. 2(3)) kills framing A outright in that market.
4. **A licence constraint bites.** E-Phy requires attribution and no alteration of meaning
   (§2.2). If a legal read holds that a *derived* per-crop answer, or a paywalled presentation,
   alters the data's meaning or implies endorsement, the source is unusable as designed.
5. **Play review rejects it.** If a submission is rejected under the health/medical disclaimer
   requirement because the app cannot honestly carry the human-medical disclaimer text, and no
   acceptable wording is found (§3.3), the differentiator cannot reach users.

**Test each of these before writing code.** 1 and 2 are desk checks this document already half
answers; 3 needs a human to ask an authority; 4 and 5 need a legal read. All five are cheap relative
to a build.

### 4.2 Fallbacks if it dies, best first

1. **Label-interval tracker (framing B).** Photograph → diagnosis → treatment plan → **"enter the
   interval from your own label"** → countdown, spray log, and a reminder for when *the user's own
   entered* interval has elapsed. No dataset, no licence, no mis-citation, and it still answers the
   owner's question in the user's own terms — which is arguably the *more* useful product, because it
   works for whatever product the user actually bought. This is also, per §3.6, the pattern that
   already reaches the store in this category.
2. **Crop-specific spray calendar and resistance rotation.** Sourced from **extension services and
   national IPM advisory material** rather than a product register, this is a real differentiator
   versus PictureThis (which treats a tomato as "a plant") without touching a food-safety claim. It
   competes on the *crop* axis, which `docs/research/2026-09-23-plant-vertical-evidence.md` §4 already
   warned is contested only by Plantix.
3. **Residue-safety literacy, not a verdict.** Educate on washing, peeling, cooking, MRLs and why the
   label interval exists, sourced to public food-safety authorities, with **no per-product number
   supplied and no safety verdict**. Delivers the topic, not the answer.
4. **The vertical itself, minus the timing claim.** The differentiator then reverts to the
   crop-specific diagnosis and treatment workflow, which the vertical research already assessed as
   fragmented but with a real 10M+ incumbent (Plantix) — worth re-reading before assuming it is
   enough.

**Where this points.** Fallback 1 is the only option that preserves the owner's *user value* while
removing the two things that make the current wording unshippable: the unlicensed-provenance problem
and the overclaim. Fallback 2 changes the differentiator. Fallback 3 abandons it.

---

## 5. Bottom line

- **Incumbents.** No pre-harvest-interval or "safe to eat" claim is visible on any store page I
  could read — 5 queries, 111 visible cards, 7 listing texts. But the two phrasings closest to the
  job (`pre harvest interval`, `harvest interval pesticide`) do not **find** a market on Play at all:
  Play fuzzy-matches "harvest" and returns time-tracking apps, banks, churches and farm games. The
  demand for this *as a searched job* is therefore **unproven from store data**, and that is a market
  finding as well as a Query-1 finding. Judged from store pages only; a page shows what an app
  advertises, never what it does.
- **Data.** It exists, is licensable, and is **national**. Confirmed with the interval as a named,
  populated, structured field: **France's E-Phy** (`delai avant recolte jour`, Licence Ouverte,
  commercial use permitted, bulk CSV/XML, weekly) and **Germany's BVL PSM-API** (`AWG_WARTEZEIT`,
  dl-de/by-2-0 per the BMEL platform, REST, monthly, free). The **EU has no PHI dataset**; the **US
  has no free structured one** — PPIS has no PHI field and PPLS is 170,000+ label PDFs, with
  structured PHI sold commercially. A truthful feature is **geo-limited by construction**.
- **Risk.** In the US, EU and UK the duty to follow the label sits on the **user**, and restating a
  published interval is not applying a pesticide. But **"adviser" is a defined, certified role in the
  EU** (Art. 2(3), professional or commercial capacity; Art. 5 training), and **California licenses any
  person who "offers a recommendation on any agricultural use"** (FAC §11410, §12001) — though
  §11408's exclusion of home-use products, and §11411's carve-out for label-consistent summaries,
  both work in the app's favour, and whether either shields a commercial app is **UNVERIFIED**.
  Play has **no pesticide or food-safety policy**; the applicable ones are Health & Medical Apps
  (declaration mandatory for all apps; misleading-or-harmful bar; a **human-medical** disclaimer),
  Misleading Claims, and the AI-content reporting requirement — with `CAMERA` and "nutrition"
  explicitly in Play's own health scope.
- **The load-bearing defect.** The differentiator as worded overclaims. A pre-harvest interval is a
  **legal use restriction**, not a residue measurement; "safe to eat" is a **safety verdict** the data
  does not support. This is true even in a market with a perfect licensed dataset, so it must be
  fixed in the spec before it is fixed in the code.
- **Recommendation.** Do not build framing D. Treat **bounding the claim to the label's interval** (A)
  or — lower risk, no dataset, no licence — **counting down an interval the user enters from their own
  label** (B) as the candidates, and settle it with a legal read plus the five kill tests in §4.1.
  Nothing in this document licenses shipping the sentence as written.

---

## 6. Every claim marked UNVERIFIED

**Data / licence**
1. The EU Pesticides Database licence (I read its scope, not its licence text).
2. Whether the **BVL** PSM-API's own terms state the dl-de/by-2-0 licence. The licence is declared in
   the **BMEL platform metadata**, not on the API page I read.
3. The reuse/copyright status of **US pesticide label text** (registrant-authored, so not obviously a
   federal "work of the United States Government" under 17 U.S.C. §105).
4. Whether **HSE's** UK register exposes a pre-harvest interval as a structured field (I read the
   user guide's search fields, not a product record).
5. Any licence or price for the **commercial** US structured-PHI databases (I read a vendor's
   marketing page only).
6. Whether other national registers (Australia, Canada, Spain, Italy, other EU member states) expose
   PHI structurably — **not searched**; the confirmed set is exactly France and Germany.
7. Whether **France's 360 `Amateur / emploi autorisé dans les jardins` products** are sufficient home-
   garden coverage for a consumer app. I read the counts; I did not assess coverage.
8. Whether **E-Phy's** data provenance covers every product a French home grower could buy (the
   dataset page notes "120-day" derogations are **not** included).

**Legal / regulatory**
9. Whether any state other than California licenses pesticide **advice** to consumers.
10. That **no US federal rule** constrains app-delivered pesticide advice — I did not read FIFRA in
    full; this is "not found", not "does not exist".
11. Whether a **commercial app** is captured by California FAC §11410 / §12001, given §11408 excludes
    home-use products and §11411 carves out label-consistent summaries.
12. Whether a **consumer** app falls inside the EU's Article 2(3) "advisor" definition — the text
    requires "professional capacity or commercial service", and an app is a commercial service.
13. Whether **EU member states** require any registration or certification for a software publisher
    (as opposed to a person) giving interval information to non-professional users.
14. What a **disclaimer** must say, in any jurisdiction, for this specific claim. No source I read
    specifies one for food-safety advice to home growers.

**Play**
15. How Play reviewers treat an **agriculture** app that declares itself under Health →
    nutrition/wellness, and whether the **human-medical disclaimer wording** ("not a medical device
    … does not diagnose, treat, cure, or prevent any medical condition") is enforced against a claim
    that is about crops and food, not about a person.
16. Whether any Play policy **elsewhere** in its corpus (Help Center, review notes, region-specific
    rules, AI-content declarations) adds requirements for food-safety-adjacent claims. I searched the
    Developer Program Policy text and found none.

**Competitors**
17. Whether any incumbent **contains** a pre-harvest-interval feature without advertising it. Store
    pages show positioning, not capability; no app was installed or used.
18. What **Plantix's** "best time to weed, spray and harvest" and **Agrio's** "pest life-cycle
    modeling … timing of pesticide applications" actually compute. Both are adjacent to a PHI; neither
    listing claims one, and I did not test either.
19. Whether `ca.gc.hcsc.pmra` (Health Canada "Pesticide Labels") exposes an interval — it appeared as
    card 30 of `pesticide calculator`, **below the enrichment cut**, so its store page was not read.
