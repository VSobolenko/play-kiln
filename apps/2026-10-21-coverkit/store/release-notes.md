# Release notes — Coverkit: Social Studio

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
Coverkit turns one layout into sharp, correctly sized covers for YouTube, Instagram, TikTok and WhatsApp — fully offline, no account. Pick a destination size, start from a template or your own photo, add text and stickers, then export a native-resolution PNG. Save drafts, reopen projects and keep a local brand kit. A one-time unlock removes the watermark and exports every size from the same design. Tell us in Play reviews which covers you need next.
</en-US>
<ru-RU>
Coverkit превращает один макет в чёткие обложки нужных размеров для YouTube, Instagram, TikTok и WhatsApp — полностью офлайн, без аккаунта. Выберите размер, начните с шаблона или своего фото, добавьте текст и стикеры и экспортируйте PNG в родном разрешении. Черновики, повторное открытие проектов и локальный бренд-кит. Разовая разблокировка убирает водяной знак и выгружает все размеры из одного макета. Напишите в отзывах Play, какие обложки нужны дальше.
</ru-RU>
```

Select everything inside the block above and paste it into Play's release-notes box as one piece —
this is Play's own format, not a representation of it, so there is nothing to reassemble. Play caps
each language at 500 characters and truncates silently, so every note in the
block is measured when the pack is validated.
