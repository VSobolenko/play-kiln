# W94 — owner complaints, verbatim (2026-10-18, third rejection)

Owner's verdict: **«не работает приложение… Замечания я тебе сказал… ты сам должен с ними
разобраться уже»**. This file is the binding brief. Rows are his words.

## E1 — Create flow: "Черновик готов" / "Создать заново" is meaningless
> «нажал на плюсик создать, выбираю фотографию. Дальше тут есть пункт макет карточки.
> Черновик готовку, да, это черновик сохраняется. Карточка готовая до и что это за два пункта?
> Бред какой-то полнейший. Что за за создать заново? Какую нахуй мы, прости меня, тут карточку
> вообще сохраняем? Что здесь за бред происходит? Что за создать заново? Куда этот черновик
> идёт? Вообще ничего непонятно.»

Required: the Create flow must have ONE obvious next step. No "draft ready / saved draft /
create again" jargon. The user must always know: what is on screen, what the button does,
and where the result went.

## E2 — "ИИ фон" is a dead end
> «нажимаю на ИИ фон, и мне пишет, что в студии осталась работа с другим фото или инструментом,
> завершите её там, прежде чем создавать этот фон. Куда? Что? Какой бред творится? Ну я как
> пользователь, я не могу работать в этом приложении.»

Required: a tool must never be blocked by an invisible leftover job. Either finish/replace it
automatically, or offer one explicit action ("Finish the previous edit" / "Discard it and continue")
on the spot. Never a message that names an unknown place.

## E3 — Cutout does not let you choose WHAT to cut
> «Захожу, допустим, вырезы товар. Нажимаю вырезать. А что вырезать? Она что, само само как-то
> догадывается или что? То есть, ну, а что мне, прости меня, она щас вырезала? Само по себе.
> Может, я хотел другое вырезать?»

Required: the user selects the object before the cut — tap the subject, or an obvious
auto-selection that is visibly editable. Never an invisible guess presented as the only answer.

## E4 — Cutout editing is unusable; result is mixed with the editor; scrolling erases
> «Потом редактирование того, что мы вырезали, просто ужасное. Тут должен быть отдельный
> редактор, где есть вариант вырезать правильно и доделать то, что нужно. Где отменить? Нашёл
> отменить… я хочу вниз прогортать, а у меня стирается… Потому что ты смешал результат
> вырезания с редактором. Ну это бред полнейший, невозможно работать с вырезанием, полностью
> всё переделывать.»

Required: a REAL dedicated editor screen: brush size, add/remove mode, undo, and a scroll gesture
that does not erase. The result preview is separate from the editing surface. One-finger vertical
drag scrolls; erasing happens only in the mask-edit mode and says so.

## E5 — Scene shows a stale photo and does not change the background
> «Почему я нажимаю на сцену, а мне вообще показывается совсем какая-то левая карточка?…
> Почему там вообще какая-то старая? Почему тут написано, вообще используется это, если
> я вроде бы уже выбрал какую-то?»
> «я написал, что хочу, чтобы этот товар находился в джунглях. Он мне что, простите, сделал?
> Он мне просто вставил опять белый фон? Мы что, опять возвращаемся к тому, с чего начинали?»
> «Нажимаю платочный лагерь и нажимаю создать… И что, блять, прости меня опять на выходе?
> Опять, блять, та же самая карточка на белом фоне?»

Required: Scene opens with the CURRENT photo. The prompt "in the jungle" must produce a new
background. If the pipeline cannot, it must say so honestly instead of returning the input.
Returning the original image is the worst possible outcome — it is indistinguishable from a bug.

## E6 — Wrong "before/after" on a first visit
> «перекрасить… Опять какая-то чужая карточка. Опять пишется до, после, даже я хотя сюда не
> заходил. Какую оно будет генерировать?»

Required: before/after appears only after a generation in this session. A first visit shows the
input alone.

## E7 — Missing action button
> «я пишу перекрасить в красный цвет… А где, прости меня, кнопка? Перекрасить. Тут написано
> какой-то сохранить сгенерированный вид. Вариант товара.»
> «А, вот, надо было закрыть.»

Required: the primary action is labelled with what it does. Never "save generated variant" as a
substitute for the action the user came to perform.

## E8 — "used 2 credits" at the bottom is invented UI
> «Почему в самом конце пишется использовано кредитов два? У нас есть место, где пишутся все
> кредиты всегда, на верхней плашке. Всегда так писалось. Что ты опять начинаешь выдумывать?»

Required: the balance lives in the top bar only. A one-time "spent 2 credits" line at the bottom
of a result is removed.

## E9 — Recolour recolours the wrong object
> «а почему он перекрасил именно эту карточку? А может, я хотел стол перекрасить? То есть, как
> он решает, что перекрашивать? Тоже под вопросом.»

Required: the object being recoloured is visible or chosen, not assumed.

## E10 — 10 credits must be restored for testing
> «у меня токенов уже нету, поэтому ты мне пересоздашь опять, чтоб было 10 токенов.»

## E11 — the whole app must be obvious on first launch
> «представь, что ты пользователь, который первый раз зашёл… Люди, когда проектируют…
> не прячут кнопку какую-то большую или непонятную. Все редакторы сделаны по какому-то принципу,
> что каждый человек заходит, и он уже понимает, что где как уже нажимать.»

## E12 — study how Wildberries generates cards
> «здесь умная вещь должна быть, где каждый может генерировать себе фотографии для продажи.
> Изучи, как сейчас генерируются карточки в Валберес, вот примерно такое же, что есть.»

Required: research real product-photo editors and Wildberries' card generation, and encode the
patterns as concrete layout rules — never invented from imagination.

## Honest test the owner will run
First launch, no photo, no prior work: create a card, give a photo, open each tool, and get a
different picture back. If any step needs a second guess about what to tap, it fails.
