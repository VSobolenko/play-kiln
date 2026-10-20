# Release notes — Plinth: Product Photos

The release screen asks for two things no other pack document carries.

## Release name

An internal Play Console label. It is never shown to a user; it exists so the console's release
list can tell one build from another. Play prefills it from the bundle's `versionName`, so it must
agree with what was uploaded, and this line is checked against `dist/release-manifest.json`.

**Use:** `0.1.1 (2)`

## What's new

Unlike the release name, this **is** shown on the store page. Play puts every language in a **single
text box** and separates them with a tag whose name is the language, the tags on their own lines:

```text
<en-US>
Plinth turns your product photos into listing-ready images. Cut out the item, build a clean or generated background, and check each image against marketplace rules before export.

Every result is labelled by how it was made: original, adjusted, AI-enhanced or generated background. Export a pack with the right sizes and file names, and see the credit cost before you spend it.

Credits, never a subscription. AI processing needs a connection.
</en-US>
<ru-RU>
Plinth превращает фотографии товара в готовые к публикации изображения. Вырежьте предмет, создайте чистый или сгенерированный фон и проверьте снимок по правилам площадки перед экспортом.

У каждого результата указано, как он сделан: оригинал, обработка, ИИ-улучшение или сгенерированный фон. Экспорт набора с нужными размерами и именами файлов; стоимость в кредитах видна до запуска.

Кредиты, без подписки. Для ИИ-обработки нужен интернет.
</ru-RU>
```

Select everything inside the block above and paste it into Play's release-notes box as one piece:
this is Play's own format, not a representation of it, so there is nothing to reassemble. Play caps
each language at 500 characters and truncates silently, so every note in the
block is measured when the pack is validated. Keep the `<en-US>` and `</en-US>` lines
exactly where they are; a language with no real note yet can simply keep a short one or be left out of
the block.
