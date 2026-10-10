# Review notes — Lumintra: Light Planner

**App:** Lumintra: Light Planner (`com.gexetr.lumintralightplanner`)
**What it is:** an offline-first planner for a photography session — it joins the dark window, the
twilight boundaries, the Moon, ~250 deep-sky targets, satellite passes and an hourly cloud forecast
into one brief, a scrubbable night scene, and a share handoff.
**Price:** paid once, no in-app purchase, no ads, no subscription.

## There is no login, no account and no demo credentials

Lumintra has **no account system, no sign-in, no sign-up and no paywall inside the app**. The purchase
is the Play price; once installed, every screen is reachable immediately. There is **nothing to unlock**,
so **no test account, no username and no password exist** and none is required. Reviewers do not need
credentials, a promo code, a redemption link or any special configuration.

There is also **no region gate, no server dependency and no feature flag**. The app works on a fresh
install in airplane mode for everything except the two optional network fetches (see below).

## The 60-second path to the core value

Everything below is reachable on a fresh install with no data and no permissions granted.

1. **Cold start.** The app opens on the single-purpose **Welcome** screen, which offers exactly one
   action: **Set a spot**. (Welcome appears once, only while no spot is saved.)
2. **Tap "Set a spot".** The spot editor opens with three ways to choose a coordinate:
   **manual entry**, **pasted coordinates**, or the app's **own map pin** (a locally drawn surface — no
   map SDK, no tile provider, no key). Enter or paste a coordinate, give the spot a name, and save.
   *No location permission is requested or needed*; a place name is filled in from the platform
   geocoder when it can be, and degrades to the name you typed when it cannot.
3. **Land on the Night scene.** With a spot saved, the app shows the **scrubbable sky** for tonight:
   your coordinate and time zone, the Sun and Moon where they are, the Milky Way as it crosses, and a
   **timeline strip** along the bottom. **Drag across the scene** — the sky moves one-to-one with your
   finger and the readout changes. This is the core value in one gesture.
4. **Read the Brief.** Open the session brief: the **dark window** (the true moonless astronomical-dark
   interval, with the golden, blue, civil and nautical twilight boundaries to the minute), the **Moon**
   as a state with both its numbers, and the **conditions with the age of every external input**. On a
   night that never goes fully dark the app says so in words instead of drawing an empty interval.
5. **Plan a shot.** Open the target list (~250 Messier/NGC objects) — each target carries its altitude
   window above your horizon for the chosen night, and a **lens fit** statement (fits / marginal / too
   large) from your focal length and sensor. Filter by brightness, search by name or catalogue number.
6. **Take it with you.** Open **Share**: the exact text payload and the postcard image are shown
   **before anything leaves the device**, so a reviewer can read precisely what would be sent, then
   choose a destination through the Android share sheet. Nothing is sent unless you tap a share target.
7. **Optional surfaces.** A home-screen **night-glance widget** and one **opt-in local reminder** at the
   start of the dark window. The reminder asks for the notification permission **only when you turn it
   on** — the app is complete without it.

## What the two network calls do, and what a reviewer should see

The app never sends anything anywhere on its own. Two optional, keyless calls exist:

- **Cloud forecast — `api.met.no`** (MET Norway). Opening a spot's forecast sends that **spot's
  coordinate, truncated to 4 decimal places (~11 m)**, and the app's identifying `User-Agent`. It
  happens **only while you are viewing that spot's forecast**, never in the background, and the app
  works fully without it. The data is labelled a **forecast with its source and its age**, never a
  measurement. Credit line: `Data from MET Norway (met.no) — CC BY 4.0`.
- **Satellite element sets — CelesTrak** (`celestrak.org`). To predict passes the app fetches current
  orbital element sets by satellite identifier. This request carries **no coordinate and no personal
  data**. If it fails, the app shows the last cached elements or an honest empty state.

If the review device is offline, both degrade to honest empty states and nothing else changes — that is
the intended behaviour, and the local product is complete without either call.

## Permissions

The app requests exactly two:

- **`INTERNET`** — the two keyless fetch paths above. Protection level *normal*, no prompt.
- **`POST_NOTIFICATIONS`** — Android 13+ only, requested **only when the reminder is switched on**. The
  app ships and works fully without it.

The app requests **no location permission of any kind** (no `ACCESS_FINE_LOCATION`, no
`ACCESS_COARSE_LOCATION`, no background location), no camera, microphone, contacts, storage, phone or
body-sensor permission. A *spot's* coordinate is a value the user enters or picks — it is not the
device's location. The Welcome screen states this in one line, on purpose: buyers arrive from apps that
draw a live compass and Lumintra is a planner.

## Data safety, in one line

One collected row — **Location → Approximate location**, purpose **App functionality**, **optional**,
**encrypted in transit** — because the saved spot's coordinate is transmitted to `api.met.no`. No data
is sent to the developer: there is no server of ours, no analytics, no crash reporter and no ad SDK.
Full detail: `store/data-safety.md`; public policy text: `store/privacy.md`.

## Things a reviewer might otherwise read as defects

- **No compass and no camera overlay.** The app computes where objects *will be* from the coordinate and
  the clock; it never asks the phone to be pointed. This is stated on Welcome.
- **No light-pollution map and no Bortle class.** Deliberate: the app plans the frame, it does not rate
  the sky's darkness. Bortle is on the "what it does not do" list in the listing.
- **The coordinate picker is the app's own drawing, not a map.** There is no map SDK and no tile
  provider by design — no key, no network, no third-party map terms — so the pin surface is drawn
  locally.
- **The forecast can be absent.** When it is, the brief says the forecast is unavailable and states why,
  rather than showing a fabricated number.

## Contact

`v.sobolenko.vs@gmail.com` — the developer account's monitored public support address.
