# Google Play listing — Lumintra: Light Planner

Human-readable copy for `store/listing.json`, which is the single source of truth. If the two
disagree, `store/listing.json` wins and this file is regenerated from it. The title, slug and
`applicationId` are derived from `naming.json` at the slot root and are not re-authored here.

## Schema

`listing.json` schema version 1 requires identity fields (`appName`, `slug`, `applicationId`, `idea`,
`defaultLocale`, `category`, `tags`, `contactEmail`, `privacyPolicyUrl`), locale copy, safety
declarations, permissions, ads/content-rating declarations, and `graphicsSource`. Each locale contains
`title`, `shortDescription`, `fullDescription`, six `screenshotCaptions`, and at least three
`featureBullets`.

- Default locale: `en-US` (Play caps `title` at 30, `shortDescription` at 80, `fullDescription` at 4000 code points).
- Additional locale: `ru-RU`.
- `contactEmail`: `v.sobolenko.vs@gmail.com`.
- `privacyPolicyUrl`: intentionally empty — the factory hosts the policy and writes the real public
  HTTPS URL with `node scripts/publish-privacy.mjs --slot lumintra-light-planner`. An empty value is
  the honest unfinished state; no reserved-host placeholder is invented here.

## en-US

### App name (23/30)

Lumintra: Light Planner

### Short description (73/80)

A light-session planner: dark window, targets, passes, cloud, no account.

### Full description (3410/4000)

One paid, offline-first planner for the photographer who plans a shoot around the light — the Sun, the
Moon, the darkness between them, and the sky the frame will sit in front of.

Most light planners tell you where an object is. Lumintra answers what the night actually asks: when is
it truly dark, is the Moon in the way, what is up, will a satellite cross the frame, and what will the
clouds do — for one spot on one date, in one brief.

WHAT YOU CAN DO

• Save a spot — a name and a coordinate, plus a horizon line you draw yourself. Enter coordinates by
hand, paste them, or place the pin in the built-in coordinate picker; a place name is filled in through
the phone's own geocoder when it can. The app never asks for a location permission.
• Read the night in one scene — a scrubbable sky with your own horizon silhouette in the foreground,
the Sun and Moon where they will be, the Milky Way as it crosses, a star-trail mode along the timeline,
and a true red night mode that keeps your night vision.
• See the dark window — the true moonless astronomical-dark interval, computed from the Sun's altitude
at −18° and intersected with the Moon's rise, set and illuminated fraction, beside the golden, blue,
civil and nautical twilight boundaries to the minute. On a night that never goes fully dark, the app
says so in words instead of drawing an empty interval.
• Plan the targets — around 250 famous deep-sky objects (the Messier list and showpiece NGC/IC entries)
with magnitudes and apparent sizes, each with its altitude window above your horizon for the chosen
night. Search by name or catalogue number, or filter by brightness.
• Fit your lens — your focal length and sensor become an angular frame drawn on the scene, and a chosen
target is stated as fitting, marginal or too large for the shot before you drive anywhere.
• Catch a pass — visible satellite passes (the ISS, Tiangong, Hubble and other tracked objects) above
your drawn horizon, each with the age of the orbital element set it was computed from.
• See the cloud — hourly total cloud cover for the session window, drawn on the same time axis as the
dark window and the target's altitude curve, labelled a forecast with its source and its age.
• Take it with you — send the finished brief off the phone as plain-language text or as a postcard
rendered from the very scene on screen, through the Android share sheet. Export your spots, horizons
and equipment to a file you own, and import it on another phone.
• Glance at tonight — a home-screen widget states tonight's dark window, Moon and cloud for your saved
home spot, and one opt-in local notification can tell you when the dark window starts.

WHAT IT DOES NOT DO

No compass and no camera overlay, no light-pollution map and no Bortle class, no telescope control, no
eclipse or tide calculators, no quizzes. It plans the shot; it does not aim the phone or take the
picture.

OFFLINE, PAID ONCE, AND PRIVATE

Almost everything is computed on the device. Two keyless calls are the only times anything leaves it:
the hourly cloud forecast from api.met.no, which receives the saved spot's coordinate truncated to about
11 m, and a satellite element-set fetch from CelesTrak, which carries no coordinate at all. There is no
account, no sign-in, no advertising, no analytics and no subscription. One price, paid once, and the
app is yours.

Data from MET Norway (met.no) — CC BY 4.0.

### Screenshot captions (phone set order)

1. Tonight, read in one glance
2. When the sky is truly dark
3. Golden and blue hour, to the minute
4. Plan the shot before you leave
5. Satellite passes you can catch
6. Red mode keeps your night eyes

### Feature bullets

- One brief joins the dark window, twilight, Moon, deep-sky targets, satellite passes and hourly cloud
- A scrubbable night scene with your own drawn horizon, the Milky Way, star trails and a true red mode
- Around 250 famous deep-sky objects with per-target altitude windows and a lens frame
- Share the finished brief as text or a postcard; export and import your spots, horizons and equipment
- No account, no ads, no analytics, no subscription — one price, and the app is yours

## ru-RU

### Название (21/30)

Lumintra: план съёмки

### Краткое описание (70/80)

Планировщик съёмки: тёмное окно, цели, пролёты и облака, без аккаунта.

### Полное описание (3496/4000)

Платный планировщик съёмки, который работает офлайн: для фотографа, который строит кадр вокруг
света — Солнца, Луны, темноты между ними и неба, на фоне которого будет снят кадр.

Большинство планировщиков света говорят, где находится объект. Lumintra отвечает на то, что ночь
спрашивает на самом деле: когда по-настоящему темно, не мешает ли Луна, что сейчас в небе, пройдёт ли
по кадру спутник и что сделают облака — для одной точки и одной даты, в одной сводке.

ЧТО МОЖНО СДЕЛАТЬ

• Сохранить точку — название и координаты, а также линию горизонта, которую вы рисуете сами. Введите
координаты вручную, вставьте их или поставьте метку во встроенном выборе точки; название места
подставляется через геокодер телефона, когда это возможно. Разрешение на геолокацию приложение не
запрашивает.
• Прочитать ночь одним полотном — прокручиваемое небо, где ваш собственный силуэт горизонта стоит на
переднем плане, Солнце и Луна — там, где они будут, Млечный Путь — так, как он проходит, режим звёздных
треков по таймлайну и настоящий красный ночной режим, который бережёт зрение.
• Увидеть тёмное окно — истинный безлунный интервал астрономической темноты, посчитанный по высоте
Солнца на −18° и пересечённый с восходом, заходом и фазой Луны, рядом с границами золотого, синего,
гражданского и навигационного сумерек с точностью до минуты. Если ночь не становится полностью тёмной,
приложение скажет это словами, а не нарисует пустой интервал.
• Спланировать цели — около 250 известных объектов глубокого космоса (список Мессье и заметные объекты
NGC/IC) со звёздными величинами и видимыми размерами, каждый — с окном высоты над вашим горизонтом для
выбранной ночи. Поиск по названию или номеру каталога, фильтр по яркости.
• Примерить объектив — фокусное расстояние и матрица становятся угловой рамкой на сцене, и про
выбранную цель сказано, помещается она, подходит на грани или слишком велика для кадра — до того, как
вы куда-то поедете.
• Поймать пролёт — видимые пролёты спутников (МКС, Тяньгунь, «Хаббл» и другие отслеживаемые объекты)
над нарисованным горизонтом, у каждого — возраст набора орбитальных элементов, по которым сделан
расчёт.
• Увидеть облака — почасовая общая облачность на окно сессии, построенная на той же оси времени, что и
тёмное окно, и кривая высоты цели, с пометкой «прогноз», источником и его возрастом.
• Забрать с собой — отправить готовую сводку с телефона обычным текстом или открыткой, отрисованной из
самой сцены на экране, через системное меню «Поделиться». Экспорт ваших точек, горизонтов и снаряжения
в файл, которым вы владеете, и импорт на другом телефоне.
• Взглянуть на сегодняшнюю ночь — виджет на главном экране показывает тёмное окно, Луну и облачность
для сохранённой точки, а одно уведомление по желанию сообщает начало тёмного окна.

ЧЕГО ПРИЛОЖЕНИЕ НЕ ДЕЛАЕТ

Ни компаса и камеры с наложением, ни карты светового загрязнения и класса Бортля, ни управления
телескопом, ни калькуляторов затмений и приливов, ни викторин. Оно планирует кадр; оно не наводит
телефон и не снимает.

ОФЛАЙН, ОДНА ЦЕНА, ПРИВАТНОСТЬ

Почти всё вычисляется на устройстве. Два запроса без ключей — единственное, что покидает телефон:
почасовой прогноз облачности от api.met.no, который получает координату сохранённой точки, усечённую
примерно до 11 м, и запрос наборов элементов спутников от CelesTrak, в котором координат нет вовсе. Ни
аккаунта, ни входа, ни рекламы, ни аналитики, ни подписки. Одна цена, один раз — и приложение ваше.

Данные MET Norway (met.no) — CC BY 4.0.

### Подписи к скриншотам (порядок набора для телефона)

1. Вся ночь с одного взгляда
2. Когда небо по-настоящему тёмное
3. Золотой и синий час до минуты
4. Спланируйте кадр заранее
5. Пролёты спутников, которые успеть
6. Красный режим бережёт глаза

### Ключевые пункты

- Одна сводка собирает тёмное окно, сумерки, Луну, цели глубокого космоса, пролёты спутников и почасовую облачность
- Прокручиваемая ночная сцена с вашим горизонтом, Млечным Путём, звёздными треками и красным режимом
- Около 250 известных объектов глубокого космоса с окнами высоты и рамкой объектива
- Поделитесь готовой сводкой текстом или открыткой; экспорт и импорт точек, горизонтов и снаряжения
- Без аккаунта, рекламы, аналитики и подписки — одна цена, и приложение ваше

## Notes for the console

- `category` is the token `PHOTOGRAPHY`; the console prints its own Russian word beside it, and the
  walk in `store/checklist.md` records the label the console shows.
- `contentRating.questionnaire = "notes"` (the console's "all other app types" branch) and
  `contentRating.targetAudience = "adults-only"` (not intended for children; the wizard skips
  `О приложении`, `Реклама` and `Страница приложения` on that branch).
- The first two lines of the full description are the only ones shown in most surfaces: they name the
  job and the buyer, so they are written to stand alone.
