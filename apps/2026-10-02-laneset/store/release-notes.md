# Release notes - Laneset: Swim Set Builder

The release screen asks for two things no other pack document carries.

## Release name

An internal Play Console label. It is never shown to a user; it exists so the console's release
list can tell one build from another. Play prefills it from the bundle's `versionName`, so it must
agree with what was uploaded, and this line is checked against `dist/release-manifest.json`.

**Use:** `0.1.0 (1)`

## What's new

Unlike the release name, this **is** shown on the store page. Play puts every language in a **single
text box** and separates them with a tag whose name is the language, the tags on their own lines:

```text
<en-US>
First release. Laneset builds a paced swim set from your minutes, pool length, focus, strokes to avoid and gear on the deck.

The 400 m and 200 m trials set your threshold pace per 100 m, so every interval has a target time. Share the set as text or print a card. Works offline; one purchase, no subscription, no ads.
</en-US>
<ru-RU>
Первый выпуск. Laneset собирает тренировку по минутам, бассейну, задаче дня, гребкам и инвентарю.

Два заплыва на время, 400 и 200, задают пороговый темп на 100 м, и у каждого отрезка есть целевое время. Тренировку можно отправить текстом или распечатать карточкой. Работает без сети, одна покупка, без подписки и рекламы.
</ru-RU>
```

Select everything inside the block above and paste it into Play's release-notes box as one piece:
this is Play's own format, not a representation of it, so there is nothing to reassemble. Play caps
each language at 500 characters and truncates silently, so every note in the
block is measured when the pack is validated. Keep the `<en-US>` and `</en-US>` lines
exactly where they are; a language with no real note yet can simply keep a short one or be left out of
the block.
