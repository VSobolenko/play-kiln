# Release notes — Lumintra: Light Planner

The release screen asks for two things no other pack document carries.

## Release name

An internal Play Console label. It is never shown to a user; it exists so the console's release
list can tell one build from another. Play prefills it from the bundle's `versionName`, so it must
agree with what was uploaded, and this line is checked against `dist/release-manifest.json`.

**Use:** `1.0.0 (1)`

## What's new

Unlike the release name, this **is** shown on the store page. Play puts every language in a **single
text box** and separates them with a tag whose name is the language, the tags on their own lines:

```text
<en-US>
Plan a whole night in one place: save a spot, draw your horizon, and read tonight on one scrubbable sky.

The true dark window sits beside the golden, blue, civil and nautical twilights to the minute, with the Moon as a state and both its numbers.

Browse about 250 deep-sky targets with altitude windows and a lens-fit answer, catch passes, and read hourly cloud.

Share the brief as text or a postcard, export and import your spots and horizons, and say what is missing in a review.
</en-US>
<ru-RU>
Спланируйте всю ночь в одном месте: сохраните точку, нарисуйте горизонт и прочитайте ночь на прокручиваемом небе.

Истинное тёмное окно рядом с границами золотого, синего, гражданского и навигационного сумерек до минуты, Луна как состояние и оба её числа.

Около 250 объектов глубокого космоса с окнами высоты и ответом по объективу, пролёты спутников и почасовая облачность.

Делитесь сводкой текстом или открыткой, экспортируйте точки и горизонты, а о нехватке напишите в отзыве Play.
</ru-RU>
```

Select everything inside the block above and paste it into Play's release-notes box as one piece —
this is Play's own format, not a representation of it, so there is nothing to reassemble. Play caps
each language at 500 characters and truncates silently, so every note in the block is measured when
the pack is validated: `release-notes:note` reports the code-point count per locale and
`release-notes:tone` refuses early/WIP/roadmap-apology language. The name above is what the console's
release list shows, and `scripts/store/validate.py` checks it against the bundle declared in
`dist/release-manifest.json`.

<!-- RELEASE_NOTES_MARKER -->
