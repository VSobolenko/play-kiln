# Google Play listing — Lawn Round Log (phase-1 draft)

This is the review copy. `store/listing.json` is the machine-readable source; this file renders it for
a human. Character caps are Play's own, measured by the pack's limit resolver.

## Identity

| Field | Value |
|---|---|
| App name | Lawn Round Log |
| Package | `com.gexetr.lawnroundlog` |
| Default locale | `en-US` |
| Additional locale | `ru-RU` |
| Category | Business |
| Tags | lawn care, round tracker, offline, small business |
| Contact email | v.sobolenko.vs@gmail.com |
| Privacy policy URL | `https://lawn-round-log.invalid/privacy` — **PLACEHOLDER, must be replaced** (see below) |
| Contains ads | No |
| Content rating | Adults only branch (`adults-only`), no child-directed content |

### The privacy policy URL is unfinished, on purpose

`https://lawn-round-log.invalid/privacy` is not a policy and cannot be mistaken for one: `.invalid`
is a reserved TLD that never resolves. `store/privacy.md` is the policy text; it has to be hosted at
an HTTPS URL the owner controls, and that URL then replaces the placeholder here and in
`store/listing.json`. Nothing in this phase may invent a host.

---

## en-US

### Title (14/30)

Lawn Round Log

### Short description (71/80)

Offline round book for a solo mower: visits, balances, one-time unlock.

### Full description (2301/4000)

Lawn Round Log is the round book a solo mower actually owns.

It is not an account and not a cloud dashboard. Your customers, the order you drive them in, every
visit you log and every payment you take live on your phone, in one record you keep. Open it at the
kerb in airplane mode and it works: no login, no sync, no server that has to be up before you can
start.

THE ROUND, IN YOUR ORDER
Add each customer once and move the list into the order you actually drive. Nothing asks you for an
appointment, a time slot or a calendar entry. The round is the worklist, and it stays in the order you
set it.

ONE TAP AT THE KERB
Mark a visit mowed, skipped or rained off with a single tap. Every control is at least 48 dp, and each
status is written out as a word as well as coloured, so you can read it one-handed in the sun.

RAINED OFF? PUSH THE DAY
One action moves every customer due today forward exactly one day. Each one moves once, so nobody is
moved twice and nobody is left behind, and pushing again moves them one further day.

WHO OWES YOU
Each customer carries a running balance built from his billable visits minus the payments you record.
Mowed visits count; skipped and rained-off visits never do. Record a payment in one tap and remove it
if you mistyped. The total owed across the whole round is on screen, and every figure is recomputed
from the stored visits and payments, so it is identical after a restart.

KEEP YOUR OWN COPY
Export the stored record to a CSV file through the system file picker, and the app tells you how many
rows it wrote. Edit or delete a customer, a visit or a payment; deleting more than one record asks
first and names what will be removed.

WHAT YOU PAY
The app is free for up to eight customers, with no time limit and no trial clock. One purchase
unlocks the whole round for good: unlimited customers, the complete visit history, the owed balances
and the export. One price, paid once. There is no subscription, no renewal and no advertising
anywhere in the app, and the purchase can be restored after a reinstall.

PRIVATE BY CONSTRUCTION
The record stays on your phone. No account is created, no analytics run, and nothing you type is
uploaded. The only party involved beyond you and your customers is Google Play, and only to take the
one-time payment.

### Screenshot captions (six)

1. The round in your order: move a customer to match the drive
2. One tap at the kerb: mowed, skipped or rained off
3. Rained off? Push the whole round one day
4. See who still owes you, customer by customer
5. Keep your own copy: export the record to CSV
6. Free for eight customers, unlocked once for the whole round

### Feature bullets

- Works offline, in airplane mode, with no account
- One tap records a visit as mowed, skipped or rained off
- A rain push moves every customer due today exactly one day
- A running owed balance per customer, plus the total for the round
- CSV export through the system file picker, with a row count
- One-time unlock: free up to eight customers, no subscription

---

## ru-RU

### Title (14/30)

Lawn Round Log

### Short description (62/80)

Офлайн-журнал обхода: клиенты, визиты, долги, разовая покупка.

### Full description (2400/4000)

Lawn Round Log — это журнал обхода, который принадлежит самому косильщику.

Это не аккаунт и не облачная панель. Клиенты, порядок объезда, каждый отмеченный визит и каждый
принятый платёж хранятся на вашем телефоне, в одной записи, которая остаётся у вас. Откройте
приложение у бордюра в режиме полёта — оно работает: без входа, без синхронизации и без сервера,
который должен быть доступен, прежде чем вы начнёте.

ОБХОД В ВАШЕМ ПОРЯДКЕ
Добавьте клиента один раз и расставьте список в том порядке, в котором вы действительно едете.
Ничего не спрашивает запись на приём, время или календарь. Обход — это рабочий список, и он
сохраняет заданный порядок.

ОДНО КАСАНИЕ У БОРДЮРА
Отметьте визит как «скошено», «пропущено» или «смыто дождём» одним касанием. Каждый элемент
управления — не меньше 48 dp, а состояние написано словом, а не только цветом, поэтому его можно
прочитать одной рукой на солнце.

СМЫЛО ДОЖДЁМ? СДВИНЬТЕ ДЕНЬ
Одно действие переносит каждого клиента, запланированного на сегодня, ровно на один день вперёд.
Каждый сдвигается один раз: никто не двигается дважды и никто не остаётся позади. Повторное нажатие
сдвигает их ещё на день.

КТО ВАМ ДОЛЖЕН
По каждому клиенту ведётся текущий баланс: оплачиваемые визиты минус записанные платежи. Скошенные
визиты считаются, пропущенные и смытые дождём — никогда. Запишите платёж одним касанием и удалите
его, если ошиблись. Общая сумма долга по всему обходу — на экране, и каждая цифра пересчитывается из
сохранённых визитов и платежей, поэтому после перезапуска она такая же.

ДЕРЖИТЕ СВОЮ КОПИЮ
Выгрузите запись в файл CSV через системный выбор файлов — приложение сообщит, сколько строк
записало. Клиента, визит и платёж можно изменить или удалить; удаление более одной записи сначала
спрашивает и называет то, что будет удалено.

ЧТО ВЫ ПЛАТИТЕ
Приложение бесплатно до восьми клиентов, без ограничения по времени и без пробного счётчика. Одна
покупка открывает весь обход навсегда: неограниченное число клиентов, полную историю визитов,
балансы долгов и выгрузку. Одна цена, один раз. Подписки, продления и рекламы в приложении нет, а
покупку можно восстановить после переустановки.

ПРИВАТНОСТЬ ПО УСТРОЙСТВУ
Запись остаётся на вашем телефоне. Аккаунт не создаётся, аналитика не работает, и ничего из
введённого вами не выгружается. Единственный участник помимо вас и ваших клиентов — Google Play, и
только чтобы принять разовый платёж.

### Screenshot captions (six)

1. Обход в вашем порядке: переставьте клиента под маршрут
2. Одно касание у бордюра: скошено, пропущено, смыто
3. Смыло дождём? Сдвиньте весь обход на день
4. Видно, кто ещё должен, по каждому клиенту
5. Держите свою копию: выгрузка записи в CSV
6. Бесплатно до восьми клиентов, разовая разблокировка

### Feature bullets

- Работает офлайн, в режиме полёта и без аккаунта
- Одно касание отмечает визит: скошено, пропущено, смыто
- Сдвиг из-за дождя переносит всех на сегодня ровно на день
- Текущий баланс долга по каждому клиенту и итог по обходу
- Выгрузка в CSV через системный выбор файлов, со счётчиком строк
- Разовая покупка: бесплатно до восьми клиентов, без подписки

---

## Claims check

Every sentence above describes behaviour the slot's spec requires. Nothing here promises a sync, a
map, an invoice, an account or a subscription, because the app has none of those. "Free up to eight
customers" is the free tier, not a trial: it has no expiry, and the app shows no clock. The purchase
is a single non-consumable unlock; the app contains no second paywall and no advertising.
