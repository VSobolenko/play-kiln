# Release notes — Mark Book for Teachers

Two fields Play asks for on the release screen, and where each one comes from.

## Release name

Play Console → *Release name* is an internal label. It is never shown to a user, it appears only in
Play Console's own release list, so it exists to tell one build from another. Play prefills it from
the bundle's `versionName`.

**Use:** `0.1.1 (2)`

The source of truth is `dist/release-manifest.json` (`versionName` + `versionCode`). Read it from
there rather than from memory — a release name that disagrees with the bundle makes the console list
lie about which build is which, which is how a rollout gets promoted by mistake.

## What's new

Unlike the release name, this **is** shown to users on the store page. Play does not give you a
separate box per language: it gives you **one** box and separates the languages with a tag whose
name is the language, the tags on their own lines —

> "Add the description for your app's release between the relevant language tags... make sure the
> language tags are on separate lines from the release notes."
> — <https://support.google.com/googleplay/android-developer/answer/9859348?hl=en>

So the block below is not a description of what to paste. It **is** the paste. Select everything
between the fence markers, put it into Play's release-notes box as one piece, and nothing needs
reassembling:

```text
<en-US>
Private offline grade book: classes, weighted marks with dropped lowest, attendance, notes, parent contacts and seating — on your device, no account. Every average shows its working. Export CSV or PDF; back up the whole record.

Free for one class up to 20 students; one purchase unlocks the rest. Want it better? Leave wishes and issues in the Play reviews — we read them and plan updates from what you write.
</en-US>
<ru-RU>
Приватный офлайн-журнал: классы, оценки с весами и отбрасыванием низшей, посещаемость, заметки, контакты родителей и рассадка — на устройстве, без аккаунта. Каждое среднее показывает расчёт. Экспорт CSV/PDF и резервная копия.

Бесплатно: один класс до 20 учеников; одна покупка открывает остальное. Хотите ещё круче? Напишите пожелания в отзывах Play — мы читаем их и планируем обновления.
</ru-RU>
```

Play allows **500 characters per language** and enforces it by truncating the paste rather than
refusing it, so a note that is too long ships as a half-sentence with nothing to say so. Both notes
above are measured when the pack is validated (`scripts/store/validate.py`), which is why they carry a
safety margin rather than sitting at the limit.

A language with no note yet can simply be left out of the block — Play then shows no release note
for it, which is not an error. What must not happen is English text sitting under a foreign tag:
that claims a translation the listing does not have.
