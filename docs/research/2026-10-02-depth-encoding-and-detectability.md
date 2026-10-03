# Depth as a machine-checkable contract, and how far a static scan can honestly go

**Written 2026-10-02.** This is a research and design document, not a factory rule: it measures what an
offline app's *depth* looks like from source and proposes a contract shape. Sections 1, 2, 4 and 6 are
**engineering proposal** built on the measurements in §3 and the prior art in §5 — they are labelled as
proposal rather than sourced fact. External claims are labelled **MEASURED** (read at the cited URL this
session), **ON RECORD** (a named source states it), **INFERRED**, or **UNVERIFIED**. Repo-internal facts
name the file that was read.

## What this measures

`differentiators.json` records *why anyone would install this app*, one promise per row. Its own spec
already defines a promise as "what the user gets here that the existing apps in this category do not
give them" (**MEASURED**, `docs/slot-differentiators.md`) — so the contract's prose is not the gap.
The gap is that **nothing measures that sentence**: `scripts/lib/slot-differentiators.mjs` checks
schema, a proof `ref` that resolves, and a `symbol` that still appears (**MEASURED**). A row can satisfy
every check while describing an operation every incumbent already performs.

The owner's complaint is that apps ship **raw**. The suspicion this document tests is checkable and
narrower: that rows are written as the app's own **basic record operations** (create / edit / delete /
list / show) rather than as reasons to choose this app, so the gate is green while the product is an MVP.

The vocabulary used below is the one `docs/research/2026-10-02-modern-android-platform-surface.md` already
established for this factory: **DETECTABLE** / **WEAK** / **NOT-DETECTABLE** (§3), and it is extended, not
restated.

---

## 1. Is a CRUD-only promise detectable?

### 1.1 The measured state, before any theory

I read the three slot contracts and the ten design-stage plans under `docs/ideation/plans/`. The plainest
CRUD-shaped `core` promise in the repository is the first claim of the shipped gradebook slot
(**MEASURED**, `apps/2026-09-26-teacher-gradebook/differentiators.json`):

> `edit-every-record` (core, shipped) — *"Every class, student, category, assignment, mark, attendance
> record, note, contact and seat can be created, edited and deleted, instead of the incumbent's name-only
> editing."* Its proof is `deleteAssignment` in `RecordBookEditor.kt`.

The newer plans are, in the main, **not** CRUD-only: they name external artifacts (`moves in and out as
CSV`), derived quantities (`what is still owed`, `from their own prices`), authored content (a Part 107
item bank) or deletion semantics (`stays deleted and takes its dependents with it`) (**MEASURED**, the ten
plan files). So the suspicion is **partially** confirmed: the defect is real and recorded, and it is not
universal. A sensor is still worth building, because the one measured case shipped.

### 1.2 A textual predicate (proposal)

Define a promise as **CRUD-shaped** when all three hold:

1. **A basic-operation verb is its main predicate** —
   `add|create|edit|update|delete|remove|list|view|show|display|save|store|enter|record|track|change|manage`
   (any inflection), or the arithmetic pair `calculate|compute` for a calculator slot.
2. **Its head noun is an app-internal record** — the entities the slot itself stores: *student, class,
   mark, grade, entry, row, item, record, note, customer, visit, field, sale, log*.
3. **It names no strong exit signal** — an external artifact or transport (`CSV`, `JSON`, `PDF`, `file`,
   `backup`, `export`, `import`, `print`, `share`, `clipboard`, the system photo/file picker), a
   transformation (`undo`, `redo`, `merge`, `convert`, `derive`, `sum`), a derived quantity (`owed`,
   `balance`, `total`, `average`, `difference`), an external recipient (`a parent can read`), or an
   authored content library.

A fourth class, **contrast-only** (`instead of`, `rather than`, `without`, `no account`, `offline`),
does **not** clear the flag on its own. It downgrades the verdict to `AUDIT` — a candidate for a human
to read — because a contrast clause can be true and can also be decoration.

Three worked cases:

| Promise | Verdict | Why |
|---|---|---|
| "Adds a student and deletes a row." | **CRUD-shaped** | verb + internal noun, no exit signal |
| "Imports the class list from a CSV the teacher already has." | **Not CRUD** | external artifact + source |
| "Every field can be created, edited and deleted, instead of the incumbent's locked fields." | **AUDIT** | verb + noun + contrast clause only — this is the measured gradebook row |

### 1.3 Can a scan do this reliably? Confidence and failure modes

**Confidence that the plain form is flaggable: high. Confidence that the verdict "this is not a
differentiator" can be automated: low.** The distinction is the whole answer, and it is not a hedge —
it is the failure the measured case demonstrates.

- **False negative — the dressed CRUD (measured).** `edit-every-record` is, lexically, an edit/delete
  row; it survives because of `instead of`. Whether it is a *real* differentiator depends on a fact about
  a competitor that is not in the sentence — the incumbent only lets you rename a class, so "you can also
  delete" genuinely is a difference. A scanner that failed it would be firing on an honestly sourced
  promise.
- **False positive — the semantic differentiator.** "Every field is editable, unlike the incumbent's
  locked inputs" is the wedge of a shipped slot (**MEASURED**, `apps/2026-09-25-pipe-sizing`) and reads
  as CRUD+contrast. Same shape, opposite verdict.
- **Signal laundering.** "Exports a CSV" clears every exit signal and can still be a token feature the
  incumbent also has. Presence of an artifact noun is not evidence of a wedge.

**Failure mode to state plainly:** the predicate measures **vocabulary**, and depth is **semantics plus
market**. It can say "this row names no artifact, no transformation, no derived quantity and no external
recipient" — a useful, high-precision flag. It cannot say "this row is not a reason to install the app",
because that requires knowing what the incumbents do, which is recon's job and a human's judgement.

**Consequence for design (proposal):** the predicate must be a **report**, not a hard-fail gate, and the
row that fails should be *answered*, not deleted. §4 turns that into a field.

---

## 2. Is job coverage detectable? Can a stage be enumerated in a schema?

Yes — job coverage is **schema-enumerable**. This is the stronger half of the design, because it does not
try to read quality out of English.

**Proposal.** Add a `jobStage` to every claim and a small `jobCoverage` object beside it:

```json
{
  "jobStages": ["before", "during", "after"],
  "jobCoverage": {
    "before": { "claimId": "imports-the-register" },
    "during": { "claimId": "one-tap-marking" },
    "after":  { "refusal": { "reason": "...", "decidedBy": "owner", "recordedAt": "..." } }
  }
}
```

where *before* / *during* / *after* are the boundaries of the user's job around its core task — for a
gradebook, *get the class list in from the register* → *mark the work* → *produce something a parent or
the office can read*. Each stage must be either a `claimId` that resolves to a claim in the same file, or
a written refusal. `during` may not be refused: it is the app's job.

**What a machine can verify:** the three keys exist; each value is a claim id that resolves or a refusal
object with a non-empty reason; `during` is never refused; no two stages point at the same claim unless
the claim is genuinely shared; a refusal on a `core`-adjacent stage carries the owner's words, reusing
`scripts/lib/slot-differentiators.mjs`'s existing `ownerDecision` shape (**MEASURED**).

**What a machine cannot verify — stated, not implied:**

- **Whether the stage boundary is honest.** "Export the term" is *after* the core task for a teacher and
  *during* it for a tutor who invoices per session. That is a judgement about the job, not a parse.
- **Whether a declared stage is implemented.** The existing `proof` mechanism proves only that a file
  exists and still contains a symbol (**MEASURED**). A stage bound to a `proof` whose symbol is a `// TODO`
  named function passes.
- **Whether a stage was *needed*.** A refused stage is a sentence; nothing measures the sentence.

**The trap this schema introduces, named:** three invented claims satisfy it. Job coverage raises the cost
of a raw product from *zero* thought to *one sentence per stage*, and no higher. That is worth having, and
it is not depth.

---

## 3. Which depth capabilities are detectable from Kotlin/Compose source?

Every family below, with the exact API, artifact or dependency that would prove it and the honest
weakness of that proof. `app/src/main/` only, comments and string literals blanked before scanning
(the pattern `scripts/core/uiux.mjs` already uses — **ON RECORD**).

| Family | What proves it | Verdict | Honest weakness |
|---|---|---|---|
| **CSV / JSON import & export** | a serialiser by name — `@Serializable` + `Json.encodeToString`/`decodeFromString`, `org.json.JSONObject`, or a declared `Csv` parser — **plus** a read/write path (`openOutputStream(uri)`, `FileReader`) | **DETECTABLE** (presence) | proves a serialiser exists, not that the bytes match what the user's register actually exports; an internal-model round-trip named "export" is token-satisfiable |
| **PDF generation** | `android.graphics.pdf.PdfDocument` (`startPage` / `finishPage` / `writeTo`) or `PrintedPdfDocument` (**MEASURED**, API reference) | **DETECTABLE** (presence) | proves a PDF is written; whether a parent can read it — pagination, embedded font, Cyrillic — needs the file |
| **Share sheet** | `Intent.ACTION_SEND` + `Intent.createChooser` / `ShareCompat`, and an `<intent-filter>` carrying `ACTION_SEND` (**MEASURED**, platform-surface §5) | **DETECTABLE** | proves a chooser opens, not that the payload is complete or valid |
| **System file picker (SAF)** | `ActivityResultContracts.OpenDocument` / `CreateDocument` / `OpenDocumentTree`, or the intents `ACTION_OPEN_DOCUMENT` / `ACTION_CREATE_DOCUMENT` (**MEASURED**) | **DETECTABLE** | proves the picker is invoked; persistence (`takePersistableUriPermission`) and a correct concrete MIME type can still be absent |
| **Clipboard** | `LocalClipboard` + `Clipboard.setClipEntry`, or `ClipboardManager.setPrimaryClip(ClipData.newPlainText(…))` (**MEASURED**; `LocalClipboardManager` is deprecated) | **DETECTABLE** (presence) | presence only — "the user can copy the result" is not measured |
| **Search — backend** | `@Fts3` / `@Fts4` / `@Fts5` with `contentEntity` and a `MATCH` query; FTS5 additionally needs `BundledSQLiteDriver` because Android's own SQLite is built without it (**MEASURED**) | **DETECTABLE** | proves an index exists, not that it is ranked or useful |
| **Search — surface** | `TextField` + a `filter`, or Material 3 `SearchBar` / `SearchBarState` / `ExpandedDockedSearchBar` (**MEASURED**) | **WEAK** | a twelve-line in-memory `filter` and a real index satisfy the same token; `SearchBar` proves an affordance, not search |
| **Sort / filter as state** | `sortedBy` / `sortedWith` / `filter`, a persisted sort key in DataStore | **WEAK** | ubiquitous operators; cannot be told from a trivial implementation, and persisted-ness is not user value |
| **Multi-select & bulk operations** | a selection set (`Set<Long>`) plus `toggleable`, `Checkbox` / `TriStateCheckbox`, and a bulk-action function | **WEAK** | a `Checkbox` token is not bulk editing; the semantics live in the action, not the control |
| **Undo / redo — text** | `TextFieldState.undoState` → `UndoState.undo` / `redo` / `canUndo` / `canRedo` (`@ExperimentalFoundationApi`, foundation 1.7+) (**MEASURED**) | **DETECTABLE** | proves a text-field history, and only a text field's |
| **Undo / redo — domain** | a snapshot/history class around app state; no platform API exists (third-party `undoable-compose` is a normal dependency) | **NOT-DETECTABLE** | a stack of past states is a class, and the class is not the behaviour |
| **Autosave — state restore** | `rememberSaveable`, `rememberSerializable`, `SavedStateHandle.saveable` (**MEASURED**) | **DETECTABLE** (presence) | proves survival of configuration change / process death, not that an edit was written to disk |
| **Autosave — no explicit save** | a write on the edit path (`DataStore.edit`, file write in a ViewModel) | **NOT-DETECTABLE** | the property is "persisted on the right event"; a call site can exist and never fire |
| **Backup / restore — platform** | `android:dataExtractionRules` + `res/xml/data_extraction_rules.xml` (`<cloud-backup>`, `<device-transfer>`), `android:fullBackupContent` + `backup_rules.xml`, `android:allowBackup` (**MEASURED**) | **DETECTABLE** | proves the platform backup surface is configured (or excluded); not that restore works on a new device |
| **Backup / restore — app export** | the import/export path plus a restore test; `takePersistableUriPermission` for a lasting access grant | **DETECTABLE** as import/export | round-trip fidelity is a test's job; the gate can only require the test exists |
| **Templates / presets** | a `data class Preset` / `List<Template>` persisted in DataStore | **WEAK** | a preset is a data structure; whether it saves the user real work is invisible |
| **Statistics & charts — library** | a declared chart dependency and its composable (`com.patrykandpatrick.vico`, `co.yml:ycharts`, MPAndroidChart, …) | **DETECTABLE** (dependency + symbol) | proves a charting library is wired, not that a number is correct or legible |
| **Statistics & charts — custom** | Compose `Canvas` + `drawLine` / `drawPath` / `drawArc`, or a derived-statistics function | **WEAK** | a `Canvas` can draw one line; correctness needs a hand-checked value, i.e. a test |
| **Notifications & reminders** | `NotificationChannel`, `NotificationManagerCompat.notify`, `POST_NOTIFICATIONS` (API 33+), `PeriodicWorkRequest` / `WorkManager.enqueueUniquePeriodicWork` (**MEASURED**); exact alarms need `AlarmManager` + `SCHEDULE_EXACT_ALARM` | **DETECTABLE** (presence) | a channel and a worker prove the plumbing, not the reminder — the periodic interval is a *minimum*, not a time (**MEASURED**); exact alarms are a restricted Play policy cost |
| **Widgets (Glance)** | `androidx.glance:glance-appwidget` dependency, `GlanceAppWidget` subclass, `GlanceAppWidgetReceiver`, a receiver + `appwidget-provider` XML in the manifest (**MEASURED**) | **DETECTABLE** (presence) | glanceability is NOT-DETECTABLE; presence is not a reason the widget is worth having |
| **Dynamic / custom fields** | nothing — a user-defined field model is `Map<String, String>` or a `List<CustomField>` | **NOT-DETECTABLE** | no API and no convention; a schema check here would be inventing a dialect |
| **Accessibility** | already measured by `uiux.a11y` (accessible name; no declared extent below 48 dp) (**ON RECORD**, `scripts/core/uiux.mjs`) | **DETECTABLE** for the declared half | TalkBack order, correct semantics and real target size need the device |
| **Localisation of user data** | `java.text.Collator`, `NumberFormat` / `DateTimeFormatter` built with a locale, `Configuration.locales` — or the existing `locale.*` checks for copy | **WEAK** | a locale-aware formatter is a token; whether a name sort, a search or a PDF font handles the user's language is a runtime/media judgement |

**Reading of the table.** Depth splits cleanly into two halves. The **connective** families — import,
export, PDF, share, file picker, clipboard, backup, reminders, widgets — are `DETECTABLE` as presence,
because each needs an artifact or a platform API that cannot be faked by prose. The **semantic** families
— search quality, sort/filter, bulk editing, undo, autosave, presets, charts, dynamic fields, data
localisation — are `WEAK` or `NOT-DETECTABLE`, because their value is in behaviour, not in a symbol.

---

## 4. What a contract can require without lying

The goal is not to invent a new dialect. `differentiators.json` already requires a promise, an evidence
source and a typed proof; §1–§3 add two fields and one project-level object. (`capabilities.json` is a
different thing entirely — it gates the *monetization* capability, `billing` / `ads`, per
`scripts/lib/slot-economics.mjs` — and must not be overloaded with depth.)

**Proposed shape (engineering proposal):**

```json
{
  "jobCoverage": { "before": {"claimId": "…"}, "during": {"claimId": "…"},
                   "after":  {"refusal": {"reason": "…", "decidedBy": "owner", "recordedAt": "…"}} },
  "claims": [
    {
      "id": "imports-the-register",
      "weight": "core",
      "jobStage": "before",
      "depthFamily": "import",
      "promise": "Imports the class list from the register CSV the teacher already has, …",
      "comparison": { "incumbent": "…", "limitation": "…", "source": "docs/research/<file>.md#<section>" },
      "evidence": { "…": "unchanged" },
      "status": "shipped",
      "proof": { "…": "unchanged" }
    }
  ]
}
```

**Machine-verified (a program can fail on this):** the schema of every new field; `jobStage` is one of the
three and every stage resolves to a claim or a refusal; `during` is never refused; a `refusal` carries a
non-empty reason and, for a `core`-adjacent stage, `decidedBy: owner`; `depthFamily` is one of a closed
enum; and — the one behavioural check — **at least one claim whose `depthFamily` is a *connective* family
(import, export, pdf, share, file-picker, clipboard, backup, reminder, widget) is `shipped`, with a proof
whose artifact contains that family's named API from §3.** A CRUD-only app by construction has none of
these, so this is the floor the suspicion actually needs.

**Machine-reported, audited by a human (a program prints it, a person reads it):** the §1.2 predicate
verdict per promise, with `CRUD-shaped` and `AUDIT` rows printed by name; every `refusal`; every
`comparison`, since its truth is a claim about a competitor and only recon can source it.

**Must stay a judgement:** whether the app is deep rather than merely broad; whether a stage boundary is
drawn the way the user's job is shaped; whether a chart, a reminder, a search or a preset is *useful*;
and whether the depth is what makes anyone pay.

**What such a contract cannot prove — the honest limit, stated because a gate that overclaims is worse
than no gate:**

- it cannot prove the app is **good**, that the depth is **wanted**, or that anyone will **pay**;
- it cannot prove a `comparison` is **true** — it proves a source was named;
- it cannot prove a `proof` artifact **means** the promise (the existing contract already says this,
  **MEASURED**, and this adds no teeth to it);
- it cannot prove the family list is **complete** — a differentiator nobody thought of is invisible;
- it cannot prove a `refusal` was **justified**;
- and it **can be gamed**: three invented claims satisfy job coverage, and a single `ACTION_SEND`
  token satisfies the connective floor. The gate raises the floor from "no output path at all" to "an
  output path exists, is named, and is proved by a real API". It does not measure depth.

**One defence against the gaming, in the same spirit as the rest of the factory:** author the contract at
recon, before the UI, exactly as the promise list and the money plan are authored today, so the fields
describe the app that was chosen rather than the app that was built.

---

## 5. Prior art in requirements engineering

The schema above is not invented. Five established positions justify each of its load-bearing pieces.

- **A requirement must be singular, unambiguous and verifiable.** ISO/IEC/IEEE 29148:2018 lists the
  characteristics of a well-formed requirement: *"Unambiguous. The requirement is stated in such a way so
  that it can be interpreted in only one way"*; *"Singular. The requirement states a single capability,
  characteristic, constraint or quality factor"*; *"Verifiable. The requirement is structured and worded
  such that its realization can be proven … Verifiability is enhanced when the requirement is
  measurable"* (**MEASURED**, standard text as returned by search). That is exactly the shape of the
  existing one-promise-per-row contract, and the justification for adding a measurable field rather than
  more prose.
- **The quality model decomposes "good" into named, measurable characteristics.** ISO/IEC 25010:2023
  defines nine product-quality characteristics — *functional suitability*, *performance efficiency*,
  *compatibility*, *interaction capability*, *reliability*, *security*, *maintainability*, *flexibility*,
  *safety* — and states the model *"can be used to establish requirements, their criteria for satisfaction
  and the corresponding measures"* (**MEASURED**, ISO preview + standard description). §3's family list
  is a small, domain-specific instance of the same move: name the characteristic, then name what
  measures it.
- **A controlled natural-language grammar is a legitimate constraint.** EARS (Mavin et al., RE'09) allows
  every requirement to be written in one of five templates — *ubiquitous*, *event-driven* (`When …`),
  *state-driven* (`While …`), *unwanted behaviour* (`If … then …`), *optional feature* (`Where …`) — and
  constrains clause order and vocabulary without leaving English (**MEASURED**). This is the precedent
  for adding a `jobStage` enum and a `comparison` object rather than a narrative field.
- **An outcome must be observable, not a database row.** The Gherkin reference states that a `Then` step
  *"should use an assertion to compare the actual outcome … to the expected outcome"* and that *"[a]n
  outcome should be on an observable output … and not a behaviour deeply buried inside the system (like a
  record in a database)"* (**MEASURED**, Cucumber reference). That sentence is the CRUD defect named
  fifteen years early: "a row was written" is not an outcome a user can observe. It is the strongest
  single citation for requiring a *connective* family rather than another CRUD proof.
- **Traceability has two directions, and both must be recorded.** Gotel and Finkelstein define
  requirements traceability as *"the ability to describe and follow the life of a requirement, in both a
  forwards and backwards direction"*, splitting **pre-RS** (its origins, before the specification) from
  **post-RS** (its deployment into design, code and tests) (**MEASURED**). `differentiators.json` already
  implements both halves — `evidence.source` is pre-RS, `proof` is post-RS — which is why the proposed
  fields attach to the existing rows rather than to a parallel document.
- **Do not build a trained classifier for this.** The requirements-engineering literature classifies
  functional vs non-functional requirements with NLP/ML, including transformer models (**ON RECORD**,
  systematic mappings), but those reviews report the same limits: few public datasets, class imbalance,
  inconsistent evaluation metrics and weak real-world validation (**MEASURED** from the review
  abstracts). §1.3's conclusion — a high-precision lexical **report** plus a human audit, not a model and
  not a hard gate — follows from that evidence as much as from the measured `edit-every-record` case.

---

## 6. What I could not verify, and the honest boundary

- **No claim here was validated by running a sensor.** The predicate in §1.2 has not been implemented and
  has no measured precision/recall. Its three worked cases are hand-evaluated against the definitions,
  which is an argument, not a measurement.
- **The family list in §3 is a reading of published API documentation, not a device test.** Nothing here
  was reproduced on hardware.
- **Material 3 `SearchBar` availability at the factory's pin** rests on the API reference's own
  deprecation notes (**MEASURED** as text) and is **UNVERIFIED** against the pinned Compose BOM
  `2026.09.00`.
- **The vendor paths are examples, not an allowlist.** Any named charting dependency decides a toolchain
  question that is the factory's, not this document's.
- **The design half is a proposal.** §1.2, §2 and §4 are engineering judgement built on the measurements
  and the prior art; they are not findings, and a program that enforces them would be new work with its
  own fixtures required (`verification-discipline.mdc`).

---

## Sources

Every URL the search/fetch tool returned content from for this document (2026-10-02).

- **Quality and requirement constructs:** <https://www.iso.org/standard/78176.html> · <https://previewnorm.com/iec/ISO%20IEC%2025010-2023%20PDF.pdf> · <https://cdn.standards.iteh.ai/samples/78176/13ff8ea97048443f99318920757df124/ISO-IEC-25010-2023.pdf> · <https://webstore.ansi.org/preview-pages/ISO/preview_ISO+IEC+25010-2023.pdf> · <https://www.iso.org/standard/72089.html> · <https://standards.ieee.org/ieee/29148/6937/> · <https://webstore.ansi.org/preview-pages/ISO/preview_ISO+IEC+IEEE+29148-2018.pdf> · <https://studylib.net/doc/27936341/iso-29148-2018---systems-and-software-engineering---life-...> · <https://drkasbokar.com/wp-content/uploads/2024/09/29148-2018-ISOIECIEEE.pdf>
- **EARS / BDD / traceability:** <https://alistairmavin.com/ears/> · <https://en.wikipedia.org/wiki/Easy_Approach_to_Requirements_Syntax> · <https://www.researchgate.net/publication/224079416_Easy_approach_to_requirements_syntax_EARS> · <https://cucumber.netlify.app/docs/gherkin/reference/> · <https://cucumber.netlify.app/docs/bdd/better-gherkin/> · <https://github.com/cucumber/website/blob/main/docs/gherkin/reference.md> · <http://csis.pace.edu/~ogotel/research/GOTEL94%20An%20Analysis%20of%20the%20Requirements%20Traceability%20Problem.pdf> · <https://discovery.ucl.ac.uk/id/eprint/749/1/2.2_rtprob.pdf> · <https://link.springer.com/article/10.1007/s00766-023-00412-z>
- **Requirements classification:** <https://link.springer.com/article/10.1007/s10462-023-10667-1> · <https://pure.manchester.ac.uk/ws/portalfiles/portal/183513868/2020_12_16_NLP4RE_Accepted.pdf> · <https://www.mdpi.com/2079-9292/13/11/2055> · <https://arxiv.org/pdf/2303.13365>
- **Android — files, documents, PDF, share, clipboard:** <https://developer.android.com/identity/data/autobackup> · <https://developer.android.com/identity/data/backup> · <https://developer.android.com/privacy-and-security/risks/backup-best-practices> · <https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/app/backup/FullBackup.java> · <https://developer.android.com/guide/topics/providers/document-provider> · <https://developer.android.com/training/data-storage/shared/documents-files> · <https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.OpenDocument> · <https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.CreateDocument> · <https://developer.android.com/reference/android/graphics/pdf/PdfDocument> · <https://developer.android.com/reference/kotlin/android/graphics/pdf/PdfDocument> · <https://developer.android.com/develop/ui/views/touch-and-input/copy-paste> · <https://developer.android.com/reference/android/content/ClipData> · <https://developer.android.com/reference/kotlin/androidx/compose/ui/platform/ClipboardManager>
- **Android — search, state, scheduling, widgets:** <https://developer.android.com/reference/androidx/room/Fts4> · <https://developer.android.com/reference/androidx/room3/Fts5> · <https://developer.android.com/training/data-storage/room/defining-data> · <https://www.sqlite.org/fts5.html> · <https://developer.android.com/reference/kotlin/androidx/compose/material3/SearchBar.composable> · <https://developer.android.com/reference/kotlin/androidx/compose/material3/SearchBarState> · <https://developer.android.com/reference/kotlin/androidx/compose/foundation/text/input/UndoState> · <https://developer.android.com/develop/ui/compose/state-saving> · <https://developer.android.com/develop/ui/compose/state-lifespans> · <https://developer.android.com/reference/androidx/lifecycle/SavedStateHandle> · <https://developer.android.com/develop/ui/compose/notifications/notification-permission> · <https://developer.android.com/reference/androidx/core/app/NotificationManagerCompat> · <https://developer.android.com/develop/background-work/background-tasks/persistent> · <https://developer.android.com/develop/ui/compose/glance/create-app-widget> · <https://developer.android.com/reference/kotlin/androidx/glance/appwidget/GlanceAppWidgetReceiver>
