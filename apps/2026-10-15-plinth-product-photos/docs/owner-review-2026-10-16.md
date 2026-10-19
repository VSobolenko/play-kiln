# Owner review — 2026-10-16, checkpoint 1.5

**Outcome: NOT approved.** The owner reviewed the app on the test phone (Samsung SM-A405FM, serial
`R58MB3SGZVL`) after clearing app data, and rejected it. This is the record of that decision and of his
words, kept verbatim because the release-gate rule requires a rejection to be as legible as an approval.

The gate itself is unchanged: no `release-approval.json` was written, and phase 2 (signed build, store pack,
screenshots) did not run.

## His words (verbatim, Russian)

> «Приложение до сих пор какое-то очень плохое. То есть, это как будто MVP. Мы сейчас строим полноценное
> приложение... Я выбрал фотографию, предок просмотра того, что я выбрал, тоже нету. Одни кнопки и текст. То
> есть опять то же самое нарушение всего проекта. Кнопки, текст, кнопки, текст, кнопки, текст. Не юзерфрендли
> вообще. Приложение сплошное, какое-то просто доисторическое. Я полностью недоволен, это полностью не
> утверждено.»

> «Здесь одна фото целая студия, дальше какие-то категории, потом опять мне что-то предлагается. Ничего
> неинтуитивно, непонятно, текст местами тоже поехал. Никаких ни менюшек, ничего, только одно сплошное текст.
> Текст, кнопки, текст, кнопки.»

> «Открыть библиотеку проектов. Вообще всё, текст поехал, я не могу нажать на вернуться в студию, так как
> вверху плашка уведомления мне перекрывает. Экспортировать набор тоже какой-то кривой, пишется впритык к
> плашке.»

> «Захожу сгенерировать фон сцены, опять поехал, конечно, ползунок загрузки, он крутится не относительно
> центра. И меня вообще выбросило, короче... Я не смог ничего сгенерировать.»

> «Выберите категорию товары... Банальный самый встроенный какой-то механизм Android, который выглядит просто
> ужасно. Допустим, кухонная посуда, выбрать фон, выбрать фон, выбрать фон. Четыре кнопки. Что это за бред?»

> «Я, допустим, умная карточка, а как мне начать генерировать? А, вот кнопка, она оказывается в самом низу
> была, которая как обычно крутится, непонятно что крутится. Ну крутится и всё, и ничего, короче, ничего не
> получилось у меня сделать.»

> «Зашёл в инструменты студии, тут всё поехало какое-то, разметка просто ужасная. Мы вроде бы договаривались до
> пяти каких-то генераций, почему на каждый пункт по два кредита, по пять кредитов? Мы же договаривались, что
> какой-то один баланс какой-то будет у человека, который он будет пополнять, либо использовать.»

> «Потом я дошёл в этой вкладке в самый конец, можно выбрать товар, а там не могу прокрутить. Вернуться в
> студию не могу, потому что плашка перекрывает уведомления. Плашка уведомлений нижней кнопки — это вообще
> постоянные проблемы, я сколько раз говорил про это.»

> «Листаю влево, а оно ничего. Думал назад вернуться, нет, такого, конечно же, мы не работаем.»

Про настройки:

> «По поводу настроек. Тебе не кажется, что настройки как будто бы смешались? Потому что здесь и какие-то
> вертикальные, горизонтальные позиции, и доставка, и язык. И светлая, тёмная тема. Как будто бы настройки
> смешались всё в одно. Возможно, тут подразумевалось типа настройки приложения; настройки каких-то картинок;
> и просто какие-то типа rate up, about us... **Чтобы решить правильно эту проблему, сам ничего не придумывай,
> изучи в интернете, как советуют маркетологи, профессионалы, и как сделаны другие, и сделай примерно то же
> самое у нас.**»

Про способ работы:

> «Сам ничего не выполняю, поручаю все задачи... к пилоту. Пускай изучает, как всё правильно делать, отдаёт тебе
> результат, запускает это всё на изменения... Сам ты ничего не делаешь, только оркестрируешь, я повторяю. Это
> даже не дебаг, это точно не к релизу.»

## The defect classes, extracted from those words

| # | Class | Where it was assigned |
|---|---|---|
| 1 | Edge-to-edge insets not applied — system bars cover the top-bar back action and the bottom primary button; content runs into the bar; the last list item cannot be scrolled to | prompt `w1-insets.md` |
| 2 | Settings are one undifferentiated list mixing app concerns with per-image defaults; the owner asked for a **researched** professional structure, not an invented one | prompt `w2-settings-ia.md` |
| 3 | A picked photo has **no preview** — the flow is "buttons and text"; the primary action is hidden at the bottom; the progress spinner is not centred in a bounded container and the flow threw him out; the category picker is a raw platform list | prompt `w3-autocard-ux.md` |
| 4 | Studio tools layout broken and unscrollable; the credit model presents several per-item prices with no visible balance, against the agreed **one balance**, and there is no visible wallet entry point | prompt `w4-studio-credits.md` |
| 5 | Home reads as a pile of entries rather than a guided path ("one photo, a whole studio, then some categories, then something is offered again"); swipe-left does nothing | prompt `w3-autocard-ux.md` |

## What this review does not say

It does not say the performance work was wasted, and it does not reopen it: the defects the previous run fixed
are measured and were not in his report — the smart-card screen's 72 s main-thread freeze is gone (10.09 s,
zero `Davey!`), pack preparation went from ~3.5 min to 22.2 s, and the exported packs carry the correct
per-marketplace pixel policy. He reviewed the app and judged the **product**, which is exactly what checkpoint
1.5 is for.
