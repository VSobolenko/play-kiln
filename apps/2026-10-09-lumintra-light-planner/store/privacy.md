# Privacy policy — Lumintra: Light Planner

**Last updated:** 2026-10-09

Lumintra: Light Planner (`com.gexetr.lumintralightplanner`) is an offline-first night-sky and
light-session planner published by Viktor Sobolenko. This policy describes exactly what the app does
with your information. It is short because the app does very little with it: there is no account, no
advertising, no analytics and no server of ours.

## In one paragraph

Everything you create in Lumintra — your observing spots, the horizons you draw, your settings, your
saved targets, your session plans — is stored **on your device only**. The app makes **two** network
requests, both to public keyless services, and one of them sends the coordinate of the spot you are
viewing so it can fetch a cloud forecast. Nothing goes to us. We have no server that receives your
data, no account system, and no way to see what you do in the app.

## What stays on your device

Stored locally in the app's private storage and never transmitted by us:

- **Spots** — the name you give a place, its latitude and longitude, its altitude, its time zone.
- **Horizons** — the silhouette you draw for a spot, kept as points and an optional elevation image.
- **Settings** — units, night-vision mode, theme, locale, notification and reminder preferences.
- **Targets and session plans** — the objects you select and the windows you plan.
- **Widget and reminder state** — what the night-glance widget shows and when a reminder is due.

This data leaves the device only when **you** ask it to, through the Android share sheet or a file you
choose to export (see "What you choose to send" below).

## The two network requests

Lumintra works fully offline. The only times it uses the network are:

### 1. Cloud forecast — MET Norway (`api.met.no`)

When you open a spot's forecast, the app asks the **Norwegian Meteorological Institute's Locationforecast
service** at `api.met.no` for the hourly **total cloud cover** in the session window. That request
contains:

- the spot's **latitude and longitude, truncated to 4 decimal places** (roughly 11 metres),
- the spot's altitude in whole metres, when known,
- the request time, and
- an identifying `User-Agent` string: `Lumintra/<version> (v.sobolenko.vs@gmail.com)`, which MET Norway
  requires so it can identify the client.

This is the **only** transmission that includes a coordinate. It happens **only while you are viewing
that spot's forecast**, never in the background, never at launch, and never from the widget or a
reminder. The forecast is **optional**: you can turn it off, and every other part of Lumintra — the sky
scene, the twilight and dark-window maths, the Moon, the target catalogue, the horizon and the satellite
passes — still works.

MET Norway's own terms govern what it does with the request — including its statement that it stores the
requesting address and any geocoordinates in its access logs — at <https://api.met.no/doc/TermsOfService>
and <https://www.met.no>.

### 2. Satellite element sets — CelesTrak (`celestrak.org`)

To predict satellite passes, Lumintra downloads current orbital **element sets** for named satellites
(by NORAD catalogue number) from **CelesTrak**. This request contains **no coordinate, no location and
no personal data** — only the satellite identifiers and the app's `User-Agent`. CelesTrak's practices
are described at <https://celestrak.org/>. If the download fails, the app simply shows the last cached
element sets or no passes.

We run no proxy and no server between you and these services, so neither request passes through
infrastructure we control.

## What you choose to send

When you tap **share** or **export**, Lumintra hands the content to whatever app you pick in the Android
share sheet, or writes a file to the location you choose. You decide the destination; the app sends it
nowhere by itself. This covers the text brief, the postcard image, and any export of your spots and
horizons. Once a file or a message leaves Lumintra, the receiving app's or service's own policy applies.

## What Lumintra does NOT do

- **No account.** There is no sign-up, no login and no profile. The app has no idea who you are.
- **No analytics and no telemetry.** No usage data, no crash reporting and no diagnostics are collected
  or transmitted.
- **No advertising.** There are no ads and no advertising SDK, so no advertising identifier is read.
- **No tracking, no profiling.** Nothing is used to build a profile of you.
- **No sale or sharing of your data.** We have no data to sell and no third party receives your
  information from us.
- **No background location, no device location.** The app requests **no location permission** of any
  kind and does not ask Android for your position. A *spot's* coordinate is a value you type or pick; it
  is not your device's location, and only the spot's coordinate may be sent, only as described above.

## Children

Lumintra is a planning tool intended for a general adult audience and is not directed at children.

## Permissions the app asks for

- **`INTERNET`** — required for the two requests above.
- **`POST_NOTIFICATIONS`** — required only if you turn on a session reminder; the app works without it.

The app requests no location, camera, microphone, contacts, storage, phone or body-sensor permission.

## How to delete your data

Because everything is on your device, you are always in control of it:

- **Delete one item** — remove a spot, a horizon or a plan inside the app.
- **Delete everything** — Android **Settings → Apps → Lumintra → Storage → Clear data** erases all
  spots, horizons, settings and cached forecasts; or simply **uninstall** the app.
- **Nothing to request from us** — there is no server-side copy of your data, so there is nothing for
  us to delete on your behalf.

## Your rights

Because Lumintra transmits no personal data to us and keeps no server-side records, we hold no personal
data about you to access, correct, export or erase. The data you created is on your device and under
your direct control. If you believe we hold information about you, write to us and we will tell you
plainly that we do not — or correct the record if we are wrong.

## Changes to this policy

If a future version of the app changes what it does with your information, this policy will be updated
before that version ships and the "Last updated" date above will change.

## Contact

Questions about this policy or the app:

**Viktor Sobolenko** — <v.sobolenko.vs@gmail.com>
