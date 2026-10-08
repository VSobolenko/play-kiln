# Release notes — Lawn Round Log

**Use:** `0.1.0 (1)`

The release name above is an internal Play Console label, never shown to a user. Play prefills it from
the bundle's `versionName` / `versionCode`, so it must stay `0.1.0 (1)` until the app's version
changes; a name that disagrees with the upload makes the console's release list lie about which build
is which.

The block below is the note itself, not a rendering of it. Paste it whole into the console's single
`Заметки о выпуске` field, tags on their own lines — Play reads every `<locale>` pair out of that one
box and shows the text on the store page. Each language is capped at 500 characters and Play
truncates silently rather than refusing, so the note below is measured.

One locale only, deliberately. The listing declares `en-US` alone, so a second note would put text on
the store page in a language the listing does not carry and imply support the store presence does not
provide. The app itself is bilingual (`res/values-ru/`) — that is its own decision and is unaffected,
because Play links no requirement between an app's resource languages and the languages declared on
its listing.

```text
<en-US>
Lawn Round Log is ready to use. Run the round with one-tap visits (mowed, skipped or rained off), get the rained-off day push, track per-customer owed balances and export CSV — all offline, no account. Free for up to eight customers; one purchase unlocks the whole round for good, with no subscription. Have a wish or hit a snag? Leave it in a Play review — we read those and plan from them.
</en-US>
```
