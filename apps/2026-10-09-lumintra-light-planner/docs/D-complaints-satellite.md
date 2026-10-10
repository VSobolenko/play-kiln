# PlayKiln complaint scan — 2026-10-08T17:29:25.754Z

Reads the review text Play server-renders on each incumbent's page, and sorts it into named complaint classes. Critical means a review rated 3★ or lower.

## Limitation, stated before the numbers

- Play server-renders a **sample** of its own choosing, measured at **3 reviews per page**. These counts describe the vocabulary of the pocket, not the frequency of complaints in it.
- The classifier is **lexical** (see `scripts/lib/play-reviews.mjs`). Every text it read is printed below so the counts can be audited, and the unclassified count is printed so its blind spot is visible.

## explicit packages

| package | title | rating | reviews read | critical | top complaint |
| --- | --- | --- | --- | --- | --- |
| `com.runar.issdetector` | ISS Detector Satellite Tracker | 4.8 | 3 | 1 | broken (1) |
| `com.vitotechnology.SatelliteTracker` | Satellite Tracker by Star Walk | 4.4 | 3 | 3 | paywall (2) |
| `com.rtbishop.look4sat` | Look4Sat: Satellite tracker | 3.8 | 3 | 0 | — |
| `com.findstarlink` | Find Starlink Satellites | 4.1 | 3 | 1 | notifications (1) |
| `com.satellite.chasers` | Satellite Chasers | 3.5 | 3 | 1 | broken (1) |

### Pocket complaint profile

15 reviews read, 6 critical (<= 3★), 0 critical without text, 0 critical not classified.

| complaint class | reviews | share of critical |
| --- | --- | --- |
| Missing feature or capability (`missing-feature`) | 3 | 50% |
| Notifications or email spam (`notifications`) | 3 | 50% |
| Paywall, price or subscription (`paywall`) | 3 | 50% |
| Broken feature or bug (`broken`) | 2 | 33% |
| Ads (`ads`) | 1 | 17% |
| Confusing or clunky interface (`confusing-ui`) | 1 | 17% |
| Slow, heavy or battery drain (`performance`) | 1 | 17% |
| Update made it worse (`regression`) | 1 | 17% |

### Verbatim critical reviews

**ISS Detector Satellite Tracker** (`com.runar.issdetector`, 4.8★)

- 3★ (January 14, 2021): I used to love this app. I bought all of the extension so I could see everything. Recently, notifications have stopped working and when I open it it is only showing the ISS. I need to refresh it to see all of the extensions I paid for. The extensions then work for maybe one more day, then they stop. It seems to have started after the most recent update. I have sent emails, but no response. Sure wish you would fix this issue.

**Satellite Tracker by Star Walk** (`com.vitotechnology.SatelliteTracker`, 4.4★)

- 2★ (August 28, 2026): the interface is good, but the fact that you have to add satellites to a list to track, and you can only add so many, which compared to how many satellites there are, is nothing, pretty much renders it pointless because you cant identify the numerous satellites going by that aren't in your list. every satellite should be pre installed and visible, especially since its a subscription. im literally paying for not knowing what the satellites I see are. canceling subscription.
- 1★ (July 14, 2026): It says that you need to enable location services to use the app which is fine and even though I did it still didn't work. It wouldn't let me use the app until I also allowed alarms and notifications. So it already doesn't let you prevent it from being intrusive if you don't like having lots of notifications. then I tried changing it to satellite view and it immediately showed me an ad. This is 10 seconds into trying to use the app. Not exaggerating. I uninstalled the app immediately.
- 1★ (June 5, 2025): How about telling me BEFORE I download the app that after a free trial period, I have to pay for it? Nope. Not a word about being a free trial of a paid app. A single mention all the way near the end of a 5 page product description is rather disingenuous don't you think? You are correct about it being in there so, my apologies, though you may want to consider saying it in the first line or two. Maybe even in the title.

**Find Starlink Satellites** (`com.findstarlink`, 4.1★)

- 3★ (August 24, 2026): it used to work well, but I'd now completely off. The ones last night went over 50 mins or so later than it said. I'm also not getting the notifications all the time, despite setting them.

**Satellite Chasers** (`com.satellite.chasers`, 3.5★)

- 3★ (August 19, 2021): It works, but it's reaaaaallllly slow. After 3 minutes it found my location and it did show me the satellites. I tried to do the tutorial but it wouldn't load more than the first screen. I tried to look when the ISS would come over next and gave up after 4 minutes of 'calculating'. My phone's not that old, but this seems like it needs a computer to run.

## Classes defined

- `crash` — Crashes or will not open
- `broken` — Broken feature or bug
- `ads` — Ads
- `paywall` — Paywall, price or subscription
- `account` — Forced account or login
- `privacy` — Permissions, privacy or tracking
- `accuracy` — Wrong or outdated information
- `units` — Wrong units or measurement system
- `regression` — Update made it worse
- `language` — Wrong or missing language
- `missing-feature` — Missing feature or capability
- `confusing-ui` — Confusing or clunky interface
- `performance` — Slow, heavy or battery drain
- `data-loss` — Sync, backup or lost data
- `offline` — Needs connection or offline failure
- `abandoned` — Abandoned or unsupported
- `notifications` — Notifications or email spam
- `value` — No value, useless or scam
