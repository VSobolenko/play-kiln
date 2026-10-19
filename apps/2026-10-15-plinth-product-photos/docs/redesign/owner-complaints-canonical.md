# Owner complaints — canonical, verbatim (2026-10-17)

This file is the **binding input** for every worker on the Studio rewrite. It is not a
summary and must not be summarised further. A worker brief that does not cite this file
row by row is an incomplete brief, and that defect has already cost this slot two rejected
product reviews.

Owner's verdict in one line: **«Оно всё так же ужасное… Переписывай всю полностью студию»**.

---

## C1 — tools are one full-screen button

> «под картинкой, под надписью "Make it studio worsely" находятся инструменты… а что это
> за хуйня такая творится там вообще? Что это за одна кнопка расположена на весь экран?
> Ты никогда не видел, как инструменты располагаются или что?»

Required: a **catalogue of tools** — a 2-column grid of equal cards. Never one full-width
button. Never one card that fills the screen.

## C2 — layout is a mess, controls are scattered

> «Что это за разметка просто ужасная? Что это за кнопки, которые разбросаны просто по
> всему? Это просто позор.»

Required: one clear vertical rhythm — photo header, then section title, then the grid. No
controls floating at arbitrary places. Nothing overlaps.

## C3 — the intended flow is photo first, then tool

> «Я предполагаю, что я зашёл, выбрал картинку и начинаю выбирать инструмент, что буду
> делать с ней.»

Required: choose the photo **once**. Every tool opened afterwards already has that photo.
A tool that asks to choose a photo again when one is bound is a defect (see C5, C10).

## C4 — every tool must say what it does

> «Где плюс еще каждом инструменте будет понятно, что он делает.»

Required: each card carries a name **and** a one-line purpose a non-expert understands, and
its credit cost. Not a bare verb.

## C5 — "Вырезать товар" asks for the photo again, and the photo eats half the screen

> «Допустим, вырезай товар. Что за выбрать фото опять, если я фото выбрал? Что за фото,
> которое на половину экрана показывается?»

Required: with a bound photo the cutter opens straight into the editing surface. The photo is
shown as a compact chip, not a half-screen preview. No "choose photo" control.

## C6 — client-facing IDs are shown

> «Что, прости меня, за айдишники клиенту показывается?»

Required: **zero** job ids, request ids, terminal ids or UUID substrings anywhere in a
user-visible string. Never a filename that is a UUID.

## C7 — "обновить стоимость" is nonsense

> «Зачем там кнопка обновить стоимость?»

Required: no refresh-cost and no refresh-wallet button anywhere in a tool. The price is
printed on the action button itself (`Generate · N credits`).

## C8 — "Сгенерировать фон сцены" is opaque Scene 1 / 2 / 3

> «Сгенерировать фон сцены. Почему я вообще не понимаю, что происходит здесь? При чём здесь
> фон макета сцена 1, сцена 2, сцена 3? Я хотел сделать фон на фоне моря, я этого сделать
> не могу… Почему только два фона доступны? …я буду продавать палатку, я хочу её сделать на
> фоне леса, а не на твоём … фоне 1, 2, 3, непонятно, что за фоны вообще такие.»

Required: a **free-text prompt is the primary control**. The user types «палатка на фоне
леса», «на фоне моря», «на пляже» and generates that. Suggestion chips may fill the prompt,
but they must never be the only choice, and there must never be a `Scene 1 / Scene 2 / Scene 3`
selector or an opaque `selectedSceneId` that gates generation.

## C9 — Relight shows studio chrome, wraps to three lines, re-asks photo, shows old pictures

> «Изменить освещение зашёл. Почему мне, блять, опять показывается инструменты студии,
> вернуться в студию, доступно два, для всех два кредитов? Почему, блять, эти три слова нахуй
> написаны на трёх строчках, блять? Что мне всё закрывает? Почему мне при изменении освещения
> опять показывается выбрать фото, если я вначале выбрал? Что мне, блять, показывается за ещё
> одни картинки, которые я генерировал до этого?»

Required: the Relight screen names **Relight** and nothing else. No "Studio tools" header, no
"back to Studio" banner, no prior-generations picker, no re-pick control, and no label that
wraps onto three lines. A header is one short line and never covers content.

## C10 — "Расширить холст" asks for the photo again

> «Расширить холст. Почему я выбрал до этого фотографию, тут опять нужно выбрать фотографию?»

Required: Expand / Outpaint opens with the bound photo. Same for Upscale and Recolour.

## C11 — Upscale screen says "инструменты студии"

> «Почему я зашёл в, допустим, увеличение раз решения, а там пишется инструменты студии?
> Куда я вообще зашёл?»

Required: every tool screen names its own tool. `Upscale` says Upscale.

## C12 — an unexplained footer "другие способы завершить работу"

> «Что это внизу за другие способы завершить работу? Я вообще первый раз их вижу. Почему
> они внизу?»

Required: remove it, or replace it with something a first-time user can act on. A mystery
footer under the tool is not allowed.

## C13 — landing hard-sells AI background

> «Почему ты на самом начале показываешь картинку и написано создать ИИ-фон? Я что, хочу
> ИИ-фон создавать? Я бы сюда пришёл за ИИ-фоном? …Нажимая на кнопку, мне сцена 1, сцена 2,
> сцена 3.»

Required: the landing states the product as a whole (a product-photo studio) and offers the
tool catalogue. It must not present one tool as the whole app, and no landing action may lead
straight into Scene 1/2/3.

## C14 — marketplace advertising inside a generative app

> «Текст объявления, бля, там вообще капец… Что это, прости меня, за реклама Амазона,
> Валбереса и Озона? Мы что, подстраиваемся под какой-то маркетплейс? …У нас генерация
> картина, каждый человек сам генерирует, как хочет.»

Required: Amazon / Wildberries / Ozon must not appear as a generative tool or as an advert
inside the Studio path. Listing text is out of this rewrite's Studio flow.

## C15 — a first-time user cannot tell how it works

> «Студия сделана просто ужасно, ты опять выдумал, ты не исправил вообще ничего…
> Это непонятно, как работает. Ты сделал наугад.»

Required: the first screen answers three questions without a tutorial — *what is this*,
*where do I start*, *what does each tool do*.

## C16 — the tools must actually run

> «Ни одним инструментом я не могу воспользоваться, потому что они ужасные… Почему я ничего
> не могу здесь делать абсолютно.»

Required: every tool the catalogue advertises must be able to reach `Generate` and return a
result. An advertised tool with no operation behind it is a lie and is a defect equal to a
crash.

---

## Definition of done

1. `:app:assembleDebug` SUCCESS.
2. Every row C1–C16 marked FIXED with a `path:line` and a phone observation.
3. A device pass on the A40 (`R58MB3SGZVL`, model `SM-A405FM`) driven by a worker, walking:
   photo selected once → grid → each tool entered with the photo already bound → free-text
   scene «палатка на фоне леса» → Generate → result visible; no id, no refresh, no Scene N.
4. `node scripts/verify.mjs --slot plinth-product-photos --build` green except rows that
   legitimately require a release artifact.
