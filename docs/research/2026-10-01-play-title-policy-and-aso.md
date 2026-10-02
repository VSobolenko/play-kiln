# Play app titles — policy constraints and ASO evidence

**Measured 2026-10-01.** Every external claim carries one of four labels: **MEASURED** (read on the cited source), **ON RECORD** (asserted by a named secondary source), **INFERRED** (this document's reasoning), **UNVERIFIED** (could not confirm). Policy wording is quoted verbatim; where a page is paraphrased the label says so.

## What this measures

The hard limits and exact policy text governing what a Google Play app title may contain, what enforcement a bad title actually attracts, every text field a listing has and which of them Play indexes, the ASO evidence for whether a *name* can change installs for a developer with no brand, the title's display/truncation behaviour, and whether a title may differ per locale. It measures the external facts only — nothing here is a recommendation or a restatement of a factory rule.

---

## 1. Title rules — the limit and the prohibitions

### The character limit

- **MEASURED**, Play Console Help, *Create and set up your app*: the Console field is **App name**, **`30 character limit`**, with the note *"You can add one localized name per language."* The page's footnote: *"Character limits apply to both full-width and half-width characters — the numbers listed above are the maximum limits regardless of what type of characters you are using."* — <https://support.google.com/googleplay/android-developer/answer/9859152>
- **MEASURED**, Metadata policy: *"Your app title must be 30 characters or less."* — <https://support.google.com/googleplay/android-developer/answer/9898842>
- **MEASURED**, Best practices for your store listing: *"Your app title must be 30 characters or less."* — <https://support.google.com/googleplay/android-developer/answer/13393723?hl=en>
- **ON RECORD** (AppTweak): the limit was **50 characters until 29 September 2021**, then reduced to 30. — <https://www.apptweak.com/en/aso-blog/how-to-prepare-for-new-google-metadata-policy-changes>

### The prohibitions, quoted

Metadata policy (*Store Listing and Promotion* group), the governing paragraph — **MEASURED**:

> "Users depend on descriptions of your app to help them understand its functionality and purpose. We don't allow apps with misleading, improperly formatted, non-descriptive, irrelevant, excessive, or inappropriate metadata, including but not limited to the app's description, developer name, title, icon, screenshots, and promotional images."
> "Your app title, icon, and developer name are particularly helpful for users to find and learn about your app. Don't use emojis, emoticons, or repeated special characters in these metadata elements. Avoid ALL CAPS unless it is part of your brand name. … Your app title must be 30 characters or less. Don't use text or image in the app title, icon, or developer name that indicate store performance or ranking, price or promotional information, or that suggests relations to existing Google Play programs."

Also on that page — **MEASURED**, verbatim: *"Avoid using repetitive or unrelated keywords or references."* and, in the Do/Don't table, *"Don't use repetitive keywords or deceptive claims like 'best game ever.'"* / *"Don't use special characters or all caps to manipulate search ranking."* — <https://support.google.com/googleplay/android-developer/answer/9898842>

Best practices page, **App title** section — **MEASURED**, verbatim fragments: *"Make sure your title is unique, accessible, and avoid common terms."* · *"Don't use text elements to promote deals. Words like 'Free' and 'No Ads' promote deals and don't belong in app titles or developer names."* · *"Don't use CAPS unless your brand is capitalized. Avoid using emojis, emoticons, and special character sequences that are irrelevant to your app."*

Google's own documented counter-example on that page — **MEASURED**: *"App title: Crane: travel PLANNER!! 🌞"*. The same page lists disallowed ranking/price text: *"'App of the year,' '#1,' 'Best of Play 20XX,' 'Popular,' award icons … '10% off,' '$50 cash back,' 'free for a limited time only'"*. — <https://support.google.com/googleplay/android-developer/answer/13393723?hl=en>

The **App Promotion** policy (Monetization and Ads group) repeats the title restriction as a promotion practice — **MEASURED**: *"Text or image in the app title, icon, or developer name that indicate store performance or ranking, price or promotional information, or that suggests relations to existing Google Play programs."* — <https://support.google.com/googleplay/android-developer/answer/9899004?hl=en>

**Competitor / other-entity names** are governed by the **Impersonation** policy, not the Metadata policy — **MEASURED**: *"Don't use another app or entity's title or name in a manner that may mislead users."* and *"Don't falsely claim to be the 'official' app of an established entity."*

**Repetitive / duplicate catalogue** is the **Spam** policy's *Repetitive Content* section — **MEASURED**: *"We don't allow apps that merely provide the same experience as other apps already on Google Play."* and *"Creating multiple apps with highly similar functionality, content, and user experience."* — <https://support.google.com/googleplay/android-developer/answer/9899034>

**INFERRED:** "keyword spam" and "irrelevant keywords" in a *title* have no separate named section; they fall under the Metadata policy's *"irrelevant, excessive … metadata"* sentence plus the best-practices guidance, and under *App Promotion* when used to boost visibility.

---

## 2. Enforcement — what is cited, and the escalation

**Which policy names the violation.** **MEASURED:** the Metadata policy is the page a title violation is decided under; *Enforcement Process* states Google considers *"app metadata (for example, app title, description)"* when reviewing. The **App Promotion** policy names the title restriction independently; the **Spam** policy names duplicate-catalogue behaviour. — <https://support.google.com/googleplay/android-developer/answer/9899234>

**Escalation ladder — MEASURED, quoted from *Enforcement Process*:** *"Repeated or serious violations (such as malware, fraud, and apps that may cause user or device harm) of these policies or the Developer Distribution Agreement (DDA) will result in termination of individual or related Google Play Developer accounts."*

- **Rejection:** *"A new app or app update submitted for review will not be made available on Google Play. … Rejections don't impact the standing of your Google Play Developer account."*
- **Removal:** *"The app, along with any previous versions of that app, are removed from Google Play … Removals don't immediately impact the standing of your Google Play Developer account, but multiple removals may result in a suspension."*
- **Suspension:** *"Suspension can occur as the result of egregious or multiple policy violations, as well as repeated app rejections or removals. … Suspensions count as strikes against the good standing of your Google Play Developer account. Multiple strikes can result in the termination of individual and related Google Play Developer accounts."*
- **Account Termination:** *"When your developer account is terminated, all apps in your catalog will be removed from Google Play and you will no longer be able to publish new apps. This also means that any related Google Play developer accounts will also be permanently suspended."*
- **Limited Visibility** (a distinct action): *"Your app's discoverability on Google Play is restricted."* — <https://support.google.com/googleplay/android-developer/answer/9899234>

**MEASURED**, *Create and set up your app*, the Console's own warning for exactly this risk: *"Repetitive or irrelevant use of keywords in the app name, description, or promotional description can create an unpleasant user experience and result in an app being suspended on Google Play."* — <https://support.google.com/googleplay/android-developer/answer/9859152>

**MEASURED**, account-help page: *"App removals, suspensions, and warnings all count as strikes against the good standing of your Play Console account. Multiple or egregious policy violations may result in the termination of your Play Console account."* and *"Google is not required to send you a warning prior to suspending or terminating your account."* — <https://support.google.com/googleplay/android-developer/answer/9023898?hl=en>

**MEASURED**, the termination troubleshooter offers *"Misleading developer name"* as a selectable termination reason — a named public category adjacent to title rules. — <https://support.google.com/googleplay/android-developer/troubleshooter/2993242>

**Documented examples.** **ON RECORD:** the 2021 change was reported as accompanied by Play Console warnings about title character count and content, and the announcement said titles not meeting the policy *"will not be allowed on Google Play"* (<https://www.apptweak.com/en/aso-blog/how-to-prepare-for-new-google-metadata-policy-changes>; <https://www.businessofapps.com/insights/google-play-policy-changes/>). **UNVERIFIED:** a named app publicly removed or terminated specifically for title/metadata keyword spam — none was found.

---

## 3. The store-listing metadata surface

**MEASURED**, *Create and set up your app* (Product details table) and *Prepare and roll out a release*:

| Field | Limit | Notes from the source | Indexed for Play search? |
|---|---|---|---|
| App name (title) | **30** incl. spaces; full-width = half-width | *"You can add one localized name per language."* | **Yes** — MEASURED as a metadata signal (§4); relative weight unpublished by Google |
| Short description | **80** | *"The first text users see when looking at your app's detail page"* | **Yes** — MEASURED it appears in search results (§5); ON RECORD it is indexed |
| Full description | **4000** | Detail page; the short description expands into it | **On Record, not stated by Google** — secondary sources agree Play indexes it (unlike iOS) |
| Release notes ("What's new") | **500 Unicode characters per language** | *"You can enter release notes using up to 500 Unicode characters per language."* | **UNVERIFIED** — sources conflict (§4) |
| Developer name | no documented limit found | Restriction *"don't falsely imply a relationship"* applies | **On Record** (indexed for brand) |
| Promotional description | **80** (same text as short description) | Best-practices page states 80 chars and *"It appears in places like search results"* | **On Record** — **INFERRED** this is the short-description field under another name |

Sources: <https://support.google.com/googleplay/android-developer/answer/9859152>, <https://support.google.com/googleplay/android-developer/answer/9859348?hl=en>, <https://support.google.com/googleplay/android-developer/answer/13393723?hl=en>.

**MEASURED, and the load-bearing point for whether keywords belong in a name at all** — *Add preview assets*, on the short description: *"Unnecessary keywords in an attempt to improve search results; it will not impact ranking and provides a bad user experience."* — <https://support.google.com/googleplay/android-developer/answer/9866151>

**INFERRED:** Google publishes **no** field-by-field indexing specification and no weights. Every "title is the heaviest field" statement below is a third-party inference, not a Google claim.

---

## 4. ASO mechanics a NAME can affect

**How search uses text — MEASURED**, *App Discovery and Ranking*: intent is derived from the query, then *"metadata (for example, title, description, category) and other signals are used to determine which apps best address the user's query."* Ranking is multi-factor: *"User relevance … Quality of the app experience … Editorial value … Ads … User experience"*, and *"apps shown in search results are heavily influenced by relevance to the user's query."* The same page lists the pre-install *"icon, title, screenshots, videos, description"* as part of app quality. — <https://support.google.com/googleplay/android-developer/answer/9958766?hl=en>

**Title carries the most weight — ON RECORD (not Google):**

- AppTweak: *"App titles on the App Store and Google Play carry the strongest weight in terms of keyword indexation"*, and *"the title holds the most weight for the indexing algorithm on both stores."* — <https://www.apptweak.com/en/aso-blog/optimizing-your-app-title-keywords-vs-brand-name>
- Appmetra: *"Google Play indexes the app name for keyword ranking, with more weight than any other field."* — <https://appmetra.com/guides/google-play-title-and-description-character-limits>
- PlayAudit: *"Title — Highest ranking weight. This is universally agreed upon by every ASO study and practitioner."* — <https://playaudit.app/blog/google-play-aso-guide>

**Keyword-heavy vs brandable titles — ON RECORD, with the small-developer read:**

- AppTweak, top-100 free apps in 7 countries: *"on average, 41% of apps on the App Store and 47% of apps on Google Play include additional generic keywords in their titles."* Their stated criterion is **brand notoriety**: *"The more recognizable and standalone your brand name is, the less you might need generic keywords."* They add that on Google Play *"the short description and screenshots do not appear in search results (for generic keyword searches), meaning that the title and icon have to be more self-explanatory."* — <https://www.apptweak.com/en/aso-blog/optimizing-your-app-title-keywords-vs-brand-name>
- PlayAudit, citing a yellowHEAD study: keywords in the **short description** had an **87%** chance of ranking, average position **~29.5** — indexed but weak. — <https://playaudit.app/blog/google-play-aso-guide>
- Incipia, 70 top-10 apps in Health & Fitness: an **exact title match** for the keyword produced **+109% average improvement** in rank versus apps without one. **ON RECORD** (single study, one category). — <https://incipia.co/post/app-marketing/aso-study-how-to-earn-a-top-10-keyword-rank/>
- ASOgency, from Play Console client data: *"the conversion rate (CVR) is +20% higher on average for brand searches that have keywords besides the brand name"*, and *"adding keywords to the app title increases the CVR by +20% for 17% of the traffic coming from searches for your brand, resulting in +3.4% more downloads."* **ON RECORD**, but the sample is **iOS** (US Top 200, four categories) — treat the Google Play transfer as **INFERRED**. — <https://www.asogency.com/case-studies/secret-behind-search-suggestions-on-app-store-google-play>
- One Google Play experiment (Medium, app "Hide All", Spanish keywords): moving the search term from after the brand to the **front of the title** raised related keyword impressions **+26%**, total impressions **+18%**, conversion flat. **ON RECORD**, n=1, self-reported, no control. — <https://medium.com/@youyoulapuda/i-moved-one-word-in-my-app-title-and-got-26-more-impressions-c58a17fb09ef>

**What does NOT work — MEASURED (Google):** unnecessary keywords *"will not impact ranking"* (<https://support.google.com/googleplay/android-developer/answer/9866151>), and keyword-stuffed titles/descriptions are a policy violation (§1). PlayAudit adds that the commonly cited "3–5% keyword density" target has no empirical backing. — <https://playaudit.app/blog/google-play-aso-guide>

**Ranking is not text alone — ON RECORD:** a bi-hourly scraping study (865 apps, 104,849 entries) found *"high download volumes, stable high review rates, and upload video introductions significantly contribute to a top-ten ranking."* — <https://exa.ai/library/publication/6j3rd5l33js>

**Contradiction to flag:** MobileAction still states *"Google Play Store allows for up to 50 characters in your app title"* — **ON RECORD and outdated/incorrect** against the 30-character policy measured above. — <https://www.mobileaction.co/guide/android-google-play-aso-guide/>

**Release-notes indexing — conflicting, UNVERIFIED:** one reference table marks "What's new" **not indexed** (<https://www.applyra.io/blog/app-store-character-limits>); another asserts *"Google has confirmed … release notes are part of the indexed metadata pool"* with no citation (<https://theionproject.com/blog/google-play-release-notes-aso-best-practices-2026/>). Google's own pages do not say.

---

## 5. Where the title is displayed, and truncation

- **MEASURED**, *Get discovered on Google Play search*: *"Keep your title focused. **Long titles may be shortened on some devices** that your users are browsing on."* — <https://support.google.com/googleplay/android-developer/answer/4448378>
- **MEASURED**, *Create and set up your app*: the app name is *"Your app's name on Google Play"*; the icon is used *"in various locations on Google Play, including your store listing, search results, and top charts"* (the title sits next to it). — <https://support.google.com/googleplay/android-developer/answer/9859152>
- **ON RECORD**, AppTweak: on Google Play *"the short description and screenshots do not appear in search results (for generic keyword searches)"* — so in a **keyword** search result the title and icon are the only text cues. — <https://www.apptweak.com/en/aso-blog/optimizing-your-app-title-keywords-vs-brand-name>
- **ON RECORD**, Appmetra: the name appears *"on search results cards, in the launcher label after install, and in the app switcher"*, and *"Search cards and the launcher truncate long names … write for the front of the string, not the end."* — <https://appmetra.com/guides/google-play-title-and-description-character-limits>
- **INFERRED:** Google publishes no fixed character count for the search-card/launcher cut, so the exact truncation point is device- and locale-dependent and cannot be machine-checked from the limit alone.

---

## 6. Multilingual constraints — can the title differ per locale?

**Yes, and it can be a different string, not a translation.** **MEASURED:**

- *Create and set up your app*: the App name field note is *"You can add one localized name per language."* — <https://support.google.com/googleplay/android-developer/answer/9859152>
- *Translate and localize your app*: *"If a user's language preference matches the translation languages you've added, they'll see the translated version of your app."* The store-listing translation unit is explicitly *"the title, short description, and full description of your app as shown to users on Google Play"* — i.e. the title is per-language and hand-authored. — <https://support.google.com/googleplay/android-developer/answer/9844778?hl=en>
- Each localized title is bound by the **same** 30-character limit and the **same** Metadata policy: the best-practices page repeats, in the App title and description sections, *"Remember that the Metadata policy and our other policies apply to all translations of your Google Play store listing."* — <https://support.google.com/googleplay/android-developer/answer/13393723?hl=en>
- If a locale is not translated, users may see a machine translation of the listing, clearly labelled, *"along with an option to view the store listing in its default language instead."* — <https://support.google.com/googleplay/android-developer/answer/9844778?hl=en>
- For a **country-specific** title rather than a language-specific one, Google points to **custom store listings** (default language chosen per listing, not auto-translated). — <https://support.google.com/googleplay/android-developer/answer/9867158?hl=en>

**INFERRED:** because the listing title and the installed app's launcher label are independent (the listing title is a Console field; the launcher label comes from the app's `strings.xml`/manifest), a slot can carry a keyword-bearing store title and a different in-app launcher name. This is observed behaviour on Play, not a documented permission — **UNVERIFIED** against a Google statement.

---

## Sources

Every URL fetched for this document:

**Google Play (primary policy and Help):**
- Metadata policy — <https://support.google.com/googleplay/android-developer/answer/9898842>
- Best practices for your store listing — <https://support.google.com/googleplay/android-developer/answer/13393723?hl=en>
- Create and set up your app — <https://support.google.com/googleplay/android-developer/answer/9859152>
- Add preview assets to showcase your app — <https://support.google.com/googleplay/android-developer/answer/9866151>
- Prepare and roll out a release (release-notes limit) — <https://support.google.com/googleplay/android-developer/answer/9859348?hl=en>
- Get discovered on Google Play search — <https://support.google.com/googleplay/android-developer/answer/4448378>
- App Discovery and Ranking — <https://support.google.com/googleplay/android-developer/answer/9958766?hl=en>
- Spam policy — <https://support.google.com/googleplay/android-developer/answer/9899034>
- App Promotion policy — <https://support.google.com/googleplay/android-developer/answer/9899004?hl=en>
- Functionality, Content, and User Experience — <https://support.google.com/googleplay/android-developer/answer/9898783?hl=en>
- Enforcement Process — <https://support.google.com/googleplay/android-developer/answer/9899234>
- Account, registration, and payment issues — <https://support.google.com/googleplay/android-developer/answer/9023898?hl=en>
- Contact Google Play about a termination/removal — <https://support.google.com/googleplay/android-developer/troubleshooter/2993242>
- Translate and localize your app — <https://support.google.com/googleplay/android-developer/answer/9844778?hl=en>
- Create custom store listings — <https://support.google.com/googleplay/android-developer/answer/9867158?hl=en>
- Google Play Developer Policy Center — <https://play.google.com/about/developer-content-policy/>

**ASO / secondary:**
- AppTweak — title keywords vs brand name — <https://www.apptweak.com/en/aso-blog/optimizing-your-app-title-keywords-vs-brand-name>
- AppTweak — Play metadata policy changes (2021) — <https://www.apptweak.com/en/aso-blog/how-to-prepare-for-new-google-metadata-policy-changes>
- Business of Apps — Google Play policy changes — <https://www.businessofapps.com/insights/google-play-policy-changes/>
- PlayAudit — Google Play ASO guide — <https://playaudit.app/blog/google-play-aso-guide>
- Appmetra — Play title and description character limits — <https://appmetra.com/guides/google-play-title-and-description-character-limits>
- Incipia — ASO keyword ranking study — <https://incipia.co/post/app-marketing/aso-study-how-to-earn-a-top-10-keyword-rank/>
- ASOgency — search suggestions study — <https://www.asogency.com/case-studies/secret-behind-search-suggestions-on-app-store-google-play>
- Medium — moving one word in an app title — <https://medium.com/@youyoulapuda/i-moved-one-word-in-my-app-title-and-got-26-more-impressions-c58a17fb09ef>
- Dynamic Monitoring of Google Play Store Ranking Changes — <https://exa.ai/library/publication/6j3rd5l33js>
- MobileAction — Play ASO guide (outdated 50-char claim) — <https://www.mobileaction.co/guide/android-google-play-aso-guide/>
- Applyra — character limits reference — <https://www.applyra.io/blog/app-store-character-limits>
- The IOn Project — release-notes ASO (uncited indexing claim) — <https://theionproject.com/blog/google-play-release-notes-aso-best-practices-2026/>
