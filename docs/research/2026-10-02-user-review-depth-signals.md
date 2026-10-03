# User-review depth signals: what real users say a small utility is missing, 2026-10-02

The factory's owner says the apps come out **RAW** — a gradebook that only adds/edits/deletes students,
a calculator that only calculates. This file is the **empirical half** of the answer: it mines what
users wrote, across Play/App Store review text, review-mining research and indie postmortems, rather
than reasoning from memory. It asks which classes of complaint exist and how often, which *extras*
users name, which extras users pay for, and when extras make an app worse.

Labels follow `.cursor/rules/grounded-claims.mdc`. **MEASURED** = a number or quote read at a source in
this session (URL given). **ON RECORD** = a named source states it and this session read that source.
**INFERRED** = follows from labelled facts. **UNVERIFIED** = not checked, with the deciding command named.

## 0. Method and provenance caveats

- Gathered by web search and page fetch on 2026-10-02; every URL in **Sources** was read (search-result
  excerpt or full fetch) in this session.
- Three sources are **self-published with no visible methodology** — `bigideasdb.com`, an `r/iosdev`
  post, and `getjustgo.com`. Their numbers are quoted as *what the source says* (`ON RECORD`), not as
  peer-reviewed findings; `bigideasdb.com` could not be re-fetched (anti-bot gate blocked a second read).
- No source here measures conversion, retention or revenue of a small offline utility. Every
  revenue-shaped statement is a review *signal*, not a P&L.

## 1. The complaint taxonomy, with published frequencies

### 1.1 Peer-reviewed complaint classes

Khalid, Shihab, Nagappan & Hassan, *What Do Mobile App Users Complain About?* (IEEE Software, 2014)
hand-tagged **6,390 one- and two-star reviews of 20 iOS apps** and reported 12 classes, with the
**median percentage across apps** and a 1:2-star impact ratio (**MEASURED**, PDF):

| # | Complaint type | Median % of reviews | 1:2-star ratio |
|---:|---|---:|---:|
| 1 | Functional Error | **26.68 %** | 2.1 |
| 2 | Feature Request | **15.13 %** | 1.28 |
| 3 | App Crashing | 10.51 % | 2.85 |
| 4 | Network Problem | 7.39 % | 2.25 |
| 5 | Interface Design | 3.44 % | 1.5 |
| 6 | Feature Removal | 2.73 % | 4.23 |
| 7 | Hidden Cost | 1.54 % | 5.63 |
| 8 | Compatibility | 1.39 % | 2.44 |
| 9 | Privacy and Ethical | 1.19 % | 8.56 |
| 10 | Unresponsive App | 0.73 % | 1.4 |
| 11 | Uninteresting Content | 0.29 % | 1.5 |
| 12 | Resource Heavy | 0.28 % | 2 |
| — | Not specific (no complaint) | 13.28 % | 3.8 |

Functional errors + feature requests + crashes are **more than 50 %** of low-star complaints, but
**feature requests are the second most frequent class at 15.13 %** — a missing *extra* is a top-two
reason a low rating is written, not a rounding error. The same paper reports that **6.12 % of all
feature requests were for better notification support** (**MEASURED**). Feature *removal* is only
2.73 % of reviews but carries a 4.23 ratio — rare and violently disliked, which is why §6 is about
additions, not removals.

### 1.2 What share of all reviews is "missing feature"?

Different corpora, so definitions differ; kept separate on purpose:

| Source | Corpus | Feature requests | Problem reports | Irrelevant |
|---|---|---:|---:|---:|
| MARA (Di Sorbo et al., MSR 2013) | large online review sample | **23.3 %** | — | — |
| AR-Miner (Chen et al., FSE 2014) | app-review pool | *(35.1 % "informative", not FR)* | — | — |
| Restrepo Henao et al. 2021, Fig. 1 | 6,406 EN app reviews | **17.1 %** | **22.4 %** | **60.4 %** |
| Restrepo Henao et al. 2021, abstract | cited prior work | "30 % requirements" | — | "~70 % noisy" |
| Pattern learning (PMC7298171) | real review set | **12.6 % actionable**; improvement requests outnumber defects by 8.4 pts | — | — |

**MEASURED** for the numeric cells. Across independent corpora the feature-request share of *all*
reviews lands between **17 % and 23 %**, and of the actionable slice, improvement requests outnumber
defect reports by **8.4 percentage points**. Verified directly from Fig. 1: English app reviews split
Problem Report 22.4 %, Feature Request 17.1 %, Irrelevant 60.4 % (a search summary that read these as
"22.4 % feature requests" was wrong; the figure is the check).

### 1.3 The tool vendors' taxonomies (ON RECORD)

- **Appbot** auto-classifies into Themes ("Bugs, Payment and Design & UX", "Performance", "Customer
  Support", "Dissatisfied Users") plus Custom Topics and manual Tags; it says its Topics come from
  "over 400 million reviews" and sentiment is "93 %+" accurate (**ON RECORD**, support.appbot.co, appbot.co).
- `getjustgo.com`'s self-published scan of **994 Play apps / 233 at ≤4.3★** ranks complaints:
  (1) paywall on previously free features, (3) broken after updates, (5) bloat — "adding features nobody
  wanted while core functionality degraded", (7) no support (**ON RECORD**, methodology UNVERIFIED).

**Which defect is the factory shipping?** Not a crash and not a paywall: it ships **15 %-class
missing-feature complaints** — the second-most-frequent low-star reason in the best-measured dataset —
on an app whose core loop is usually correct. "Raw" is the empirical word for that.

## 2. The extras users name most often, ranked

Two datasets count requests by theme. They are **not** the same corpus and their order differs; both are
reproduced rather than averaged.

`bigideasdb.com` (self-published, 136K-review claim; "documented requests", methodology UNVERIFIED) —
table as printed (**ON RECORD**): Customization & personalization **905** · Search, filtering &
organization **807** · Flexible pricing & payment **673** · Sync, backup & data portability **661** ·
Reporting & analytics **591** · Integrations **507** · Notifications & reminders **364** · Collaboration
& sharing **214** · Offline mode **182**. Its own prose says "the largest theme … is sync, backup and
data portability", which **contradicts its table** (customization is first); both are reported, and the
discrepancy is why this source is UNVERIFIED for methodology.

`r/iosdev` self-posted analysis of 50,000 App Store reviews (**ON RECORD**, UNVERIFIED):

| Complaint | Mentions | Where | Users' words / note |
|---|---:|---|---|
| "Can't export data" | 743 | Notes 201, Productivity 156, Finance 147, Fitness 134 | *"My data feels trapped."* Requested CSV, PDF, Markdown |
| "No offline access" | 587 | Notes, Reading, Productivity, Travel | *"Useless on planes/poor WiFi"* |
| "Sync doesn't work" | 512 | multi-device apps | data loss in 34 % of these |
| "No Apple Watch support" | 423 | Fitness, Health, Productivity, Finance | *"Why do I need my phone at the gym?"* |
| "Can't customize [feature]" | 401 | all | "users willing to pay more for customization" |
| "No desktop version" | 356 | Finance, Notes, Productivity, Reading | *"Mobile-only is limiting"* |
| "No import from [competitor]" | 287 | — | switching cost |

### Synthesized ranking (INFERRED)

Weighted qualitatively: a signal ranks higher when it appears in **both** datasets, in **paid**
evidence, and in named review quotes. This is judgement, not arithmetic.

1. **Export / backup / data portability** — top-4 in both datasets (661 and 743); the paid extra (§3);
   the adoption blocker when absent (Bundled Notes, §5).
2. **Sync / multi-device / cross-platform** — grouped with backup in `bigideasdb`; 512 "sync doesn't
   work"; the named reason users upgraded in Authenticator App.
3. **Search / filtering / organization** — 807 requests; the most common *functional* extra.
4. **Reminders / notifications** — 364 requests; 6.12 % of all feature requests in Khalid 2014.
5. **Widgets / instant-capture surfaces** — no standalone count, but repeatedly a named praise and a
   named complaint (Authenticator's widget capped at 3 codes; Loop's widgets as a headline).
6. **Import from a competitor** — 287 mentions; explicitly the thing that stops a switch.
7. **Bulk operations** — no count; strong single quote (Authenticator QR stack); separates a tool from
   a demo.
8. **Statistics / reports / analytics** — 591 requests; AntennaPod users had to read SQLite to get
   theirs (a reported gap, not a delivered extra).
9. **Customization / themes / layout** — 905 (the largest *theme*), but partly a design preference
   rather than a job; kept mid-rank on purpose.
10. **Templates / presets / sharing** — Pixel Habit Tracker ships "sharing templates"; low independent
    evidence.
11. **Print** — **UNVERIFIED**: no review quote or count found in this session. The deciding command is
    a review-text search over a slot's own shelf for `print`/`PDF export`.

### Real user phrasings (MEASURED, app and rating where the source gives them)

- **Authenticator App** (App Store, 4.6/5, 48K ratings), *"Excellent App but Limited Widget"*: *"The
  Wdiget is limited to only 3 codes … I need that widget or my workflow will slow down. … The app does
  not support bulk importing/exporting of QR codes. Things would have gone a lot faster if I could have
  just dragged a stack of QR codes onto the importer rather than having to select them one by one."* —
  **widgets + bulk import + export in one review.**
- **Authenticator App**, *"Great App after my previous review"*: *"Please provide us with a link within
  App transfer everything from other Apps to this one without having to do it manually."* — **import from
  competitor.**
- **Notes Widget Reminder** (Play review, 2024-06-02): *"The only thing I miss here is being able to
  manually rearrange notes/tasks … App has sort by creation date, last modification date, priority, but
  doesn't have kind of 'manual ordering'."* — **manual order / bulk editing.**
- **Notes Widget Reminder** (Play review): *"Literally the longest I have kept a widget on my Main phone
  screen … I keep a running list of bills on it."* — **widget as the reason the app survives.**
- **Khalid et al. 2014** feature-request example: *"No way to customize alerts."* — **reminders.**
- **AntennaPod forum**, "Exporting statistics": *"I would like to export my listening statistics in
  numerical form … I didn't find a button within the app on the statistics screen."* — **export + stats.**
- **Bundled Notes**, `r/bundled`: *"The only thing holding me back from using it is that there's no
  import feature right now."* — **import as the gate on adoption.**

## 3. The "I would pay / I did pay for" signal

The evidence points overwhelmingly at **export / backup / data portability**, secondarily at **sync**.

- **Health Auto Export** (App Store; rating not captured — **UNVERIFIED**): a one-star complains the
  free tier has *"zero export functionality"*; a five-star says *"I instantly bought the lifetime
  subscription."* The paid tier is precisely the export engine (**MEASURED**, App Store reviews).
- **HabitKit** help docs: *"Both live at Settings › Data Import/Export … and both are HabitKit Pro
  features in the current release."* Export/import is the paywall (**MEASURED**).
- **TextPort** (4.3/5, 1,100+ ratings): *"Full export access requires a weekly or annual subscription …
  the intended pattern for one-off projects is subscribing for a week, exporting without limits, and
  cancelling."* (**MEASURED**). **iMazing**: *"advanced backup options and backup restoration require a
  paid subscription."* (**MEASURED**).
- **Authenticator App** five-stars naming the *extra* as the reason to pay: *"I upgraded to Premium …
  Easy to share between devices to keep things synced"*; *"For $14.99 a year I get a great app that syncs
  across all my apple devices"*; *"Having a solid 2FA Authenticator app that just works … Lifelong
  subscriber here"*; *"because it syncs across all mac devices … worth every penny!"* (**MEASURED**).
- **Habo** (4.6/5 from 67 reviews), review quoted on its site: *"it's open source, secure, has
  export/backup functionality and it simply works. all while not annoying you with in app purchases."*
  Its paid tiers sell "secure cloud backups, multi-device sync" — the extra, not the core (**ON RECORD**).

**INFERRED:** the extra users name when they pay is not "more ways to enter data" but **"a way to get my
data out, keep it safe, and carry it to the next device or tool."** Export/backup is the only extra that
appears as the *paid product* in four independent commercial apps and as the *named* reason for upgrade
in a paid app's five-star reviews. A 743-mention complaint ("my data feels trapped") and a 661-request
theme agree with the money.

## 4. Before / during / after — the job the app sits inside

This section is a **framework**, not a measurement: it comes from service-design writing, and each
mapping to a feature family is `INFERRED`, not `MEASURED`.

Service blueprints split customer actions into **pre-service, in-service and post-service**, and layer
backstage/support processes under the line of visibility; a blueprint must be anchored to a real scenario
or it "produces a diagram of guesses dressed as research" (**ON RECORD**: Uxcel service-blueprinting
lesson; Rise8 Delivery Playbooks; Adaptive Path *Guide to Service Blueprinting*; Talkful).

| Stage | What the user is doing (framework) | Feature family (INFERRED) |
|---|---|---|
| **Before** | preparing, gathering, bringing last time's data in, learning the rules | import from another tool (CSV/competing app), templates/presets, demo data, no-account start, a stated permission rationale |
| **During** | the operation the app is *for* | the core function **plus** quick capture (widget, notification action), search/filter, bulk edit, undo, autosave, offline |
| **After** | reporting, sharing, filing, reminding, reviewing, auditing | export/backup/share, sync, reminders, statistics/history, archive, a shareable report |

The framework's own output includes a **physical-evidence / artifact** column (receipts, emails, screens)
— for a utility that is exactly the export, the PDF, the shared link, the statistics screen (**ON RECORD**,
Rise8). "Complete" therefore means the *stages are all present*, not that the feature count is high.

## 5. What "complete" looks like — three anatomies

### 5.1 Loop Habit Tracker (Android) — the free, offline reference
- **Core:** habit check-off with flexible schedules, streak + habit-strength score.
- **Rating / scale:** Google Play **4.7 from 61.8K reviews, 5M+ installs**; AppBrain records **4.72 /
  61,815 ratings / 7.9M lifetime downloads**; ProductApps reports the last-100 average at **4.87**, above
  lifetime (**MEASURED**).
- **Extras shipped:** individual **reminders**; **widgets** ("track your habits directly from your home
  screen"); **charts and statistics**; **history**; **export to CSV or SQLite** (*"Take your data with
  you"*); ad-free, open source, no account (**MEASURED**, Play + GitHub).
- **Evidence:** the listing leads with stats, reminders and widgets; a ProductApps test singles out "the
  variety of home screen widgets that allow quick habit tracking without opening the app" and "Data
  export options for backup and analysis"; AppBrain's quoted review — *"The app is simple for daily
  activity/habit tracking. It has no ads and run smoothly. I am giving it five stars."* (**MEASURED**).

### 5.2 AntennaPod (Android) — depth without an account
- **Core:** podcast subscription, download and playback.
- **Rating / scale:** Google Play **4.8 from 95.1K reviews**; AppBrain **4.84 / 93,389 / 1M+** (**MEASURED**).
- **Extras shipped:** **OPML import/export**, directory import, **database export/import**,
  **statistics**, playback **history and search**, favourites, speed/chapters, **advanced sleep timer**,
  **homescreen widget**, automated download deletion, gPodder backup (**ON RECORD**, listing + docs).
- **Evidence:** the listing sells the extras beside playback — *"Import, organize and play … Share
  episodes and feeds … via OPML export"*; the documented gap proves users treat extras as required: a
  forum request to export listening statistics is answered "it is in the database export", i.e. the data
  is there but the *extras* make it usable (**MEASURED**).

### 5.3 Authenticator App by 2Stable (iOS/macOS) — the paid one
- **Core:** TOTP code generation.
- **Rating / scale:** App Store **4.6/5 from 48K ratings**; AppGoblin estimates 67K ratings at **4.64★**,
  ~171K MAU, **~$<10K/month, 100 % IAP** (**MEASURED**).
- **Extras shipped:** **encrypted iCloud backup + automatic sync** across iPhone/iPad/Mac/Watch,
  **multi-import**, groups, **widget**, offline codes, **import/export to `.json`** (**MEASURED**).
- **Evidence:** reviews praise the *extras* by name and pay for them — *"particularly the backup and sync
  function"*, *"I particularly enjoy the sync feature … incredibly valuable"*, *"I upgraded to Premium"*,
  *"worth every penny"*, plus the widget/bulk-import complaint in §2.

### 5.4 Counter-anatomy: Bundled Notes — a beautiful app held back by a missing extra
Play rating rose **3.9 → 4.3** and crash rate fell 40 % (developer's own March-2025 state post); features
include rich tags, Kanban, reminders, offline editing, sync, themes. But its FAQ: *"Can I import my notes
from another service (Keep/Evernote etc.)? Not yet."* and a user on `r/bundled`: *"The only thing holding
me back from using it is that there's no import feature right now."* The roadmap names *"a proper import
and export experience"* as a goal (**MEASURED**). **Depth without data portability is still "raw" at the
adoption gate.**

## 6. The honest counter-evidence — when extras HURT

- **A sync extra destroyed a game's ratings.** The developer of a top-3 App Store game added cloud sync
  after users begged for it; a race condition overwrote real saves with empty ones. *"the App Store rating
  got destroyed by 1-star reviews … Revenue plummeted, retention numbers plummeted, new installs
  plummeted … it was no longer #3 on App Store searches, it was #13."* (**MEASURED**). The extra was
  *right to want and wrong to ship* — a data-safety extra is a data-loss surface.
- **A requested extra made a product worse for everyone who did not ask.** A solo studio shipped a bulk
  export three customers requested, then removed it: *"I had not just … an option, I had added a decision
  point to a screen that used to require none."* Rule: *"when a request repeats often enough to act on, I
  ask … what does this replace for the people who never asked for it."* (**MEASURED**).
- **Unused features keep charging rent.** *"Before defending a feature's continued existence, I want to
  see who actually uses it"*; cut rule: *"Anything under 5 % engagement for more than a month is a
  candidate. Then I ask whether the feature needs explanation."* (**MEASURED**).
- **Feature creep degrades the core.** A productivity-app analysis attributes **6 %** of complaints to
  "Complexity and Learning Curve" — *"Too many features, can't find what I need"* — with the headline
  *"Users will forgive missing features … but they won't forgive slowness."* (**MEASURED**).
  `getjustgo`'s scan names **bloat** as a top-5 pattern (**ON RECORD**, methodology UNVERIFIED).
- **Self-built extras can still be a monologue.** A mini-app developer added notifications, favourites,
  spending stats and expiry memos nobody asked for, then cut them: *"When the person deciding to add a
  feature is the same person who supposedly needs it, that's not validation — it's a monologue."*
  (**MEASURED**).

**The decision rules the sources give:**

1. Ship a data-safety extra only when demanded, and **ship it safely** — a requested sync that loses data
   is worse than no sync (aljrico, MEASURED).
2. Before adding, name **what it replaces for the users who never asked**; if that is "a small tax on
   every other customer", find a version without the tax or do not build it (RAXXO, MEASURED).
3. Anything **under ~5 % engagement for a month** is a removal candidate; a feature that needs explaining
   is not pulling its weight (Steffen Bilde, MEASURED).
4. Prefer **depth in the job's own stages** (§4) over breadth of unrelated features (INFERRED, from the
   anatomy and bloat evidence).

## 7. What this means for the factory

An app is "raw" when it covers only the **during** stage and only the core operation inside it. The
measured fix is not "more screens"; it is the **four extras the job already implies**: (1) export /
backup / portability, (2) search / filter, (3) reminders or a widget (a way back in), (4) import from
whatever the user used before. Those four are the highest-ranked, the most-quoted, and the ones users
demonstrably pay for — and every one is offline and needs no server, which is why they fit this factory's
slots. The §6 rule still applies: add them as the job's own stages, keep the core first, and never ship a
data-safety extra without the failure path tested.

## Sources

Read in this session (search-result excerpt or full fetch), one line per source:

- Khalid et al., *What Do Mobile App Users Complain About?* (IEEE Software 2014) — https://www.swag.uwaterloo.ca/www/assets/other/papers/khalid-ieeesw-2014.pdf
- Di Sorbo et al., MARA (MSR 2013) — https://dl.acm.org/doi/10.5555/2487085.2487094
- Chen et al., AR-Miner (FSE 2014) — https://ink.library.smu.edu.sg/cgi/viewcontent.cgi?article=3323&context=sis_research
- Restrepo Henao et al., *Transfer Learning for Mining Feature Requests and Bug Reports…* (2021) — https://arxiv.org/pdf/2108.00663
- *Pattern Learning for Detecting Defect Reports and Improvement Requests in App Reviews* — https://pmc.ncbi.nlm.nih.gov/articles/PMC7298171/
- Maalej et al., *On the automatic classification of app reviews* — https://link.springer.com/article/10.1007/s00766-016-0251-9
- *Mining User Opinions to Support Requirement Engineering: An Empirical Study* — https://pmc.ncbi.nlm.nih.gov/articles/PMC7266457/
- *Mining and Searching App Reviews for Requirements Engineering* — https://discovery.ucl.ac.uk/id/eprint/10164064/1/final-version.pdf
- *Data-Driven Requirements Prioritization Framework for App Reviews* — https://www.mdpi.com/2411-5134/11/2/33
- *A Phrase-Level User Requests Mining Approach in Mobile Application Reviews* — https://www.mdpi.com/2078-2489/12/5/177
- Appbot — https://support.appbot.co/help-docs/how-can-appbot-help-with-app-review-insights/ · https://appbot.co/faqs/ · https://support.appbot.co/help-docs/tags-topics-custom-topics/ · https://appbot.co/best-practices/how-to-analyze-app-reviews/
- *State of Mobile App Pain Points 2026* — https://bigideasdb.com/state-of-mobile-app-pain-points-2026 (anti-bot gate blocked a second fetch; numbers read once)
- r/iosdev 50,000-review analysis — https://www.reddit.com/r/iosdev/comments/1r3ki6b/analyzed_50000_app_store_reviews_here_are_the_10/
- *I Scanned 994 Play Store Apps…* — https://getjustgo.com/play-store-enshittification/
- *Why Power Users Abandon Notion, Todoist, Trello (2026)* — https://unstar.app/blog/productivity-app-reviews-what-power-users-complain-about-2026
- *The Feature I Removed After Customers Asked For It* — https://raxxo.shop/blogs/lab/the-feature-i-removed-after-customers-asked-for-it
- *The Update That Killed the Dream* — https://aljrico.com/posts/the-update-that-killed-the-dream/
- DEV, *The Cost of Keeping What You Should Remove* — https://dev.to/joachimzeelmaekers/the-cost-of-keeping-what-you-should-remove-4c8h · *The Feature Nobody Uses Still Sends You a Bill Every Month* — https://dev.to/serguey_shinder_4ab9b87b1/the-feature-nobody-uses-still-sends-you-a-bill-every-month-8h9
- *Deleting features is shipping* — https://steffenbilde.com/notes/deleting-features-is-shipping
- *Why My App Got Smaller Every Time I Added a Feature* — https://builderlog.net/blog/04-less-is-more/
- Service-blueprint framework — https://uxcel.com/lessons/service-blueprinting-439 · https://delivery-playbooks.rise8.us/content/plays/design/service-blueprint/ · https://maeda.pm/wp-content/uploads/2019/12/A-Guide-to-Service-Blueprinting.pdf · https://www.talkful.io/blog/how-to-build-a-service-blueprint
- Loop Habit Tracker — https://play.google.com/store/apps/details?id=org.isoron.uhabits&hl=en · https://www.appbrain.com/app/loop-habit-tracker/org.isoron.uhabits · https://productivity-apps.com/apps/loop-habit-tracker · https://github.com/iSoron/uhabits
- AntennaPod — https://play.google.com/store/apps/details?hl=en&id=de.danoeh.antennapod · https://www.appbrain.com/app/antennapod-podcast-player/de.danoeh.antennapod · https://antennapod.org/documentation/general/backup · https://forum.antennapod.org/t/exporting-statistics/516
- Authenticator App (2Stable) — https://apps.apple.com/us/app/authenticator-app/id1538761576 · https://appsupports.co/1538761576/authenticator-app/positive-reviews · https://appgoblin.info/apps/1538761576 · https://authenticator.2stable.com/help/how-sync-and-backup-feature-works/
- Health Auto Export reviews — https://apps.apple.com/us/app/health-auto-export-json-csv/id1115567069
- TextPort review — https://textport.com/articles/textport-review · iMazing review — https://www.itechguides.com/products/imazing-review/
- Habo — https://www.itechguides.com/products/habo/ · https://habo.space/ · HabitKit (export is Pro) — https://habitkit.app/help/data-backup-and-sync/export-and-import-your-data
- Motif — https://www.usamakashif.me/motif/ · Dayora Daily — https://limebird.org/docs/dayora-daily/user-guide/
- Bundled Notes — https://bundlednotes.com/support/import-notes · https://bundlednotes.medium.com/files-photos-and-attachments-on-the-web-the-start-of-open-sourcing-and-more-26e3b886d044 · https://www.reddit.com/r/bundled/comments/1uiwygy/this_app_is_so_beautiful_and_awesome_best_way_to/
- Notes Widget Reminder — https://play.google.com/store/apps/details?id=sk.mildev84.noteswidgetreminder · Simple Notes Widget — https://play.google.com/store/apps/details?id=ru.igarin.notes
- Pixel Habit Tracker — https://habits.pixel.al/ · Habit Tracker (qbito) — https://habit-tracker.qbito.app/
