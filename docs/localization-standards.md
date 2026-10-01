# Localization standards

The standard PlayKiln follows so that localization is a **design input from the first commit** rather
than a task discovered at `/release`. It is the detail behind
[`.cursor/rules/localization.mdc`](../.cursor/rules/localization.mdc); the executable half is
[`scripts/core/localization.mjs`](../scripts/core/localization.mjs).

The owner's instruction, recorded 2026-09-30 after reviewing the first live app,
`apps/2026-09-26-teacher-gradebook`: *"localization must always be there, from the start."* He then
directed that the app not be repaired and that the next apps must not be able to repeat the shape it
was built in.

## 1. What was already covered, measured before anything was written

The obvious hypothesis — "nothing checks that the language declaration and the resource tree agree" —
is false, and the gap had to be found rather than assumed. Measured 2026-09-30:

| Candidate gap | Verdict | What covers it |
|---|---|---|
| `locales_config.xml` agrees with `values-<locale>/` **in both directions** | **COVERED** | `scripts/core/policy.mjs` `localeProblems()` — `locales_config.xml offers '<lang>' but res/values-<lang>/ does not exist…` and `res/<dir>/ exists but locales_config.xml does not declare '<lang>'…`; the same rules in `scripts/check-locales.mjs` lines 247-268 |
| A translation is **complete**, not merely present | **COVERED** | `localeProblems()` fails per key — `is missing '<key>' (from values/<src>), so it falls back to English`, a stale key `which values/ no longer has`, a format-argument mismatch and a missing plural category. Measured: `node scripts/check-locales.mjs --slot teacher-gradebook` → `PASS locales - 11 translated language(s) x 518 key(s) complete` |
| A translation is **not a machine placeholder** | **OPEN** | `rg 'en-XA\|ar-XB\|pseudo\|machine.translat\|placeholder' scripts/` finds no locale-shaped sensor. The one plant-marker test, `HomeViewModelTest.shippedCopyCarriesNoScaffoldMarkers` (recorded in `harness/lessons.md`, 2026-09-27), is a slot's own unit test, never a factory check |
| A slot is **born multilingual** | **OPEN** | `localeProblems()` is explicitly honest about zero additional languages; its own fixture is *"a slot that declares no additional language is reported honestly, not as a defect"*. `scaffold/android-compose/` ships `res/values/` and `<locale android:name="en" />` alone, so every new slot starts monolingual |

The two open gaps are the defect (**born multilingual**) and the trap it leads to (**paste something
into `values-<locale>/` so the tree exists**). `scripts/core/localization.mjs` owns those two and
nothing else. A second implementation of an agreement or coverage rule would be a duplicate check,
which this repository treats as a defect rather than as extra safety.

## 2. The standard

### 2.1 A slot is born multilingual

- The locale set is chosen at stage 1, with the identity and the promise contract — not at `/release`,
  when the layout and every screen string are already fixed.
- At least **one language besides the default** is declared in `res/xml/locales_config.xml` and
  shipped as a `res/values-<locale>/` tree before the slot is called done. The `README.md` default
  additional locale is `ru-RU`.
- A locale is declared **only** together with its tree, in the same change. Declaring a language whose
  resources do not exist offers a per-app language setting that changes nothing.
- `values/` stays the fallback. Every locale resolves per string against `values/`, so a partial
  translation degrades to English words rather than to a blank — which is why a partial tree is a
  state to report, never a way to look finished.

Android's own guidance for the declaration is the per-app language feature, which reads
`android:localeConfig` from a `res/xml/locales_config.xml` listing one `<locale android:name="…" />`
per supported language (<https://developer.android.com/guide/topics/resources/app-languages>; the
attribute itself: <https://developer.android.com/reference/android/R.attr#localeConfig>). The platform
does **not** verify that a declared language has resources — a locale with no `values-<locale>/`
silently falls back — which is exactly why the factory checks it (in `slot.locales`, not twice).

### 2.2 Every user-facing string is a resource

- No prose literal reaches a composable. `Text("Starter action complete")` is copy no translator can
  reach and no pseudolocale can stretch; `stringResource(R.string.…)` is the only route.
- Accessibility text is copy: a `contentDescription` in a literal is read aloud in the wrong language.
- The sensor is deliberately conservative — it flags a literal of **two words or more** with at least
  one lowercase letter, and stays silent on tokens (`"OK"`, `"CSV"`), URLs and format strings. Single
  word prose is therefore missed, and that is stated rather than hidden: a detector that fires on
  correct code is a detector the next author switches off.
- Text **layout** under a longer language is a different rule. This standard owns where copy lives;
  the text-layout and typography rule owns whether it fits.

### 2.3 A `values-<locale>/` tree must contain a translation

Three mechanically provable shapes are refused:

| Shape | Example | Why it is a defect |
|---|---|---|
| A sentinel value | `TODO`, `[translate]`, `Lorem ipsum`, `U+FFFD` | a user in that language reads the placeholder — under the default locale this is a release-blocking string |
| A clone | every value in `values-ru/` byte-identical to `values/` | a copy of the English is not a translation; it is a tree created so a check would stop complaining |
| A wholesale paste | ≥ 80 % of shared values identical | reported as a **note** for a human to judge, not failed — this is the honest side of the boundary |

**What the sensor cannot do, said plainly:** a *real* machine translation — different words, wrong
idiom — is not mechanically detectable from the text alone. That is why the rule is procedural as
well as a sensor: the repository is the source of truth, `values-<locale>/` is git-tracked and
diffable, machine output is a starting point that a human reviews and freezes into
`values-<locale>/`, and **Play Console's automatic string translation stays off for every language the
slot already translates** (`AGENTS.md`; <https://support.google.com/googleplay/android-developer/answer/9844778?hl=en>).
Enabling it overrides the git-tracked translations, and switching it back off discards them.

### 2.4 Pseudolocales: the layout test that needs no translation

Android ships two pseudolocales for exactly this purpose
(<https://developer.android.com/guide/topics/resources/pseudolocales>):

- **`en-XA`** — right-to-left-expanded text: every resource string is accented and roughly 30 % longer,
  which is how a layout built around English word lengths breaks before a translator ever sees it.
- **`ar-XB`** — right-to-left mirrored: the test for a layout that assumes a left-to-right reading
  order, needed before Arabic is declared.

**How to run it** (device or emulator, API 18+): enable Developer options, then
**Settings → System → Developer options → Pseudolocales → English (XA) / Arabic (XB)**
(<https://developer.android.com/guide/topics/resources/pseudolocales#enable-pseudolocales>).

**What it proves, and why it belongs to this standard:**

- Every string that **stretches** is a resource. Every string that **stays English and unstretched** is
  a hardcoded literal — the pseudolocale is the cheapest way to find copy the translator will never
  see, and it is why §2.2 is a prerequisite for a meaningful pseudolocale test rather than a separate
  concern.
- It runs **before** the first translation exists, so the layout is fixed once instead of restyled
  per language later.

**What it does not prove:** it is not a real language, so it says nothing about idiom, grammar, plural
categories or string length in a specific locale. It is a layout-and-extraction test, not a
translation review.

**Shipped pseudolocales are banned.** A `values-en-rXA/` tree or an `en-XA` tag in `locales_config.xml`
offers a debug language to real users in the per-app language picker. The sensor fails both.

### 2.5 Store listing languages ≠ app resource languages

No Play requirement links the app's resource languages to the languages declared on the listing. A
single-language listing over a multi-language app is legitimate, and Play auto-translates the store
*listing* independently of the in-app strings — which is unrelated to §2.3 and must not be
"fixed" by turning on automatic in-app translation, or by deleting translations (`AGENTS.md`;
`docs/research/2026-09-22-android-play-requirements.md`).

## 3. Check inventory

| Check id | Artifact | Owns | Fails when |
|---|---|---|---|
| `locale.multilingual` | one slot | the born-multilingual shape | the slot declares no language besides its default; a declared language has no tree, or an empty one |
| `locale.placeholder` | one slot | translation integrity | a value is a sentinel, or the tree is a clone of `values/` |
| `locale.hardcoded` | one slot | copy extraction | user-facing prose is compiled into a Kotlin literal |
| `locale.pseudolocale` | one slot | shipped-language hygiene | an `en-XA`/`ar-XB` tag or `values-en-rXA/`-style tree is shipped |
| `locale.scaffold.multilingual` | **the factory** | the scaffold a new slot is copied from | `scaffold/android-compose/` declares only its default language (red until the scaffold ships a second language) |
| `locale.scaffold.hardcoded` | **the factory** | the scaffold's copy extraction | the scaffold compiles prose into a composable |
| `slot.locales` *(existing, not this standard's)* | one slot | declaration ⟷ tree agreement, key coverage, format arguments, plural categories | **do not duplicate** |

Applicability is derived by `scripts/core/baseline.mjs`, effective **2026-09-30**: a pipeline proof
(`apps/2026-09-22-smoke`, `apps/2026-09-23-pipe-proof` — no `economics.json`), a killed slot, and a
slot created before that date print their findings as a note instead of a red row, so the gate is not
noise on slots the owner has already decided not to repair. Every slot created from now on is gated.

## 4. How it is verified

```bash
node scripts/verify.mjs # invariants group: localizationFixtures() + localizationProjectChecks()
node scripts/verify.mjs --slot <slug> # ui group: the four locale.* slot checks
node --input-type=module -e "import('./scripts/core/localization.mjs').then(m=>console.log(JSON.stringify(m.localizationFindings('apps/<slot>'),null,2)))"
```

Every check id carries an inline fixture that fails against a deliberately wrong implementation, run
in the `invariants` group before any slot is read. The predicate that decides "monolingual" was
observed failing on real input before the module was trusted: with it inverted,
`apps/2026-09-26-teacher-gradebook` reports the monolingual finding and the scaffold reports clean,
and four fixtures go red — the demonstration is in the change record, not only in the fixtures.

## 5. Citations

All verified to resolve on 2026-09-30.

- Pseudolocales — <https://developer.android.com/guide/topics/resources/pseudolocales> and the
  enablement steps — <https://developer.android.com/guide/topics/resources/pseudolocales#enable-pseudolocales>
- Per-app language preferences and `res/xml/locales_config.xml` —
  <https://developer.android.com/guide/topics/resources/app-languages> and
  <https://developer.android.com/guide/topics/resources/app-languages#locale-config>
- The `android:localeConfig` attribute —
  <https://developer.android.com/reference/android/R.attr#localeConfig>
- Android 13 per-app language feature — <https://developer.android.com/about/versions/13/features/app-languages>
- Localize your app (general) — <https://developer.android.com/guide/topics/resources/localization>
- String resources, including `translatable` — <https://developer.android.com/guide/topics/resources/string-resource>
- Play Console automatic string translation overriding existing translations —
  <https://support.google.com/googleplay/android-developer/answer/9844778?hl=en>
- Play App Signing enhancements (the family the toggle belongs to) —
  <https://support.google.com/googleplay/android-developer/answer/9842756?hl=en>
- Repository rule on the three App Signing enhancements (`AGENTS.md`) and the recorded decision in
  `docs/research/2026-09-22-android-play-requirements.md`
- `harness/lessons.md` — 2026-09-26 (a `translatable="false"` value had nine copies and no gate),
  2026-09-26 (localizing a slot silently re-scoped the capture journey to the phone's language),
  2026-09-27 (a planted `lorem` marker found the runner, not the sensor)
