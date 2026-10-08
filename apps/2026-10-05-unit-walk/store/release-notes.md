# Release notes — UnitWalk

The release screen asks for two things no other pack document carries.

## Release name

An internal Play Console label. It is never shown to a user; it exists so the console's release
list can tell one build from another. Play prefills it from the bundle's `versionName`, so it must
agree with what was uploaded.

**Use:** `0.1.0 (1)`

## What's new

Unlike the release name, this **is** shown on the store page. Play puts every language in a **single
text box** and separates them with a tag whose name is the language, the tags on their own lines:

```text
<en-US>
UnitWalk is ready to use. Walk each room, take photos with the camera or pick them from your gallery, and add notes as you go — every photo is saved as you work, so closing the app loses nothing. Compare a move-in walk with its move-out walk room by room, then turn the report into a signed PDF and share or save it. Nothing is sent to us: no account, no sign-in, and the walk works offline. Tell us what to improve in a Play review.
</en-US>
<ru-RU>
UnitWalk готов к работе. Пройдите по каждой комнате, сделайте фото камерой или выберите их из галереи и добавьте заметки по ходу — каждое фото сохраняется сразу, поэтому закрытие приложения ничего не теряет. Сравните обход при заселении и при выселении по комнатам, затем сохраните отчёт в PDF и поделитесь им. Ничего не отправляется нам: без аккаунта, без входа, обход работает офлайн. Напишите в отзыве Play, что стоит улучшить.
</ru-RU>
```

Select everything inside the block above and paste it into Play's release-notes box as one piece —
this is Play's own format, not a representation of it, so there is nothing to reassemble. Play caps
each language at 500 characters and truncates silently, so both notes above were measured when the
pack was validated.
