# Release notes — Lawn Round Log

**Use:** `0.1.0 (1)`

The release name above is an internal Play Console label, never shown to a user. Play prefills it from
the bundle's `versionName` / `versionCode`, so it must stay `0.1.0 (1)` until the app's version
changes; a name that disagrees with the upload makes the console's release list lie about which build
is which.

The block below is the note itself, not a rendering of it. Paste it whole into the console's single
`Заметки о выпуске` field, tags on their own lines — Play reads every `<locale>` pair out of that one
box and shows the text on the store page. Each language is capped at 500 characters and Play
truncates silently rather than refusing, so the two notes below are measured.

```text
<en-US>
First release of Lawn Round Log. The round, one-tap visits (mowed, skipped or rained off), the rained-off day push, per-customer owed balances and CSV export all work offline with no account. Free for up to eight customers; one purchase unlocks the whole round for good, with no subscription.
</en-US>
<ru-RU>
Первый выпуск Lawn Round Log. Обход, визиты одним касанием (скошено, пропущено, смыто дождём), сдвиг дня из-за дождя, балансы долгов по клиентам и выгрузка в CSV работают офлайн и без аккаунта. Бесплатно до восьми клиентов; одна покупка открывает весь обход навсегда, без подписки.
</ru-RU>
```
