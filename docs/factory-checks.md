# Every check that survives, and the command that runs it

This file exists because `scripts/selftest.mjs` was deleted on 2026-09-30 (task 5.1 of
`openspec/changes/reduce-factory-to-core`). The self-test had one job worth keeping — it knew which
suites existed and it ran them — and one job worth losing: it was a test runner running test runners
inside a verification script, so a one-line fix paid for a chain of processes and a green run could
not be read as a statement about anything narrower than the whole factory. What is kept is the
**knowledge**: this page, checked in, listing every surviving check and suite with the exact command
that runs it, so the list cannot be lost when a runner is.

Two rules for reading it:

- **A command listed here was run.** The `observed` column is a real run's result on 2026-09-30 in
  this working tree, not a description of one. Where a command needs a device, a Gradle build, a
  signing key or a Play credential, it is marked and was **not** run, because running it is either
  forbidden (`prepare-release.mjs`, `media-capture.mjs`, `play-upload.mjs`) or belongs to another
  worker's lease.
- **A check that is in no list is a check that does not exist.** Adding a check means adding it to
  the one verification command, or naming it here with the reason it is a procedure rather than a
  gate.

## 1. The one entry point

`scripts/verify.mjs` imports every fact check from `scripts/core/index.mjs` and runs them in **one
process**. It spawns at most one subprocess, and only when `--build` is asked for.

| Command | What it decides | Observed 2026-09-30 |
|---|---|---|
| `node scripts/verify.mjs` | every project-wide invariant, including the fact ledger | see §8 |
| `node scripts/verify.mjs --only invariants` | the invariants alone | see §8 |
| `node scripts/verify.mjs --slot <slug>` | the slot's pack, icon, apk, build, manifest, policy, project **and ui** checks | see §8 |
| `node scripts/verify.mjs --slot <slug> --build` | the same, plus `assembleDebug` | **`[needs Gradle]` — not run** |
| `node scripts/verify.mjs --json` | the same report, machine-readable | not run separately; covered by the rows above |

The checks it runs, as `scripts/core/index.mjs` declares them:

| id | module | needs a slot |
|---|---|---|
| `invariants` | `assets.mjs`, `apk.mjs`, `build.mjs`, `config.mjs`, `icon.mjs`, `project.mjs`, `facts.mjs`, `uiux.mjs`, `settings.mjs`, `localization.mjs` | no |
| `assets` | `assets.mjs` (`packChecks`) | yes |
| `icon` | `icon.mjs` (`checkAdaptiveIcon`) | yes |
| `apk` | `apk.mjs` (`apkChecks`) | yes |
| `build` | `build.mjs` (`buildChecks`) | yes |
| `manifest` | `manifest.mjs` (`manifestChecks`) | yes |
| `policy` | `policy.mjs` (`policyChecks`) | yes |
| `project` | `project.mjs` (`slotChecks`) | yes |
| `ui` | `uiux.mjs`, `settings.mjs`, `localization.mjs` | yes |

### The `ui` group, added 2026-09-30

The owner reviewed the factory's first live app and found a pronounced identity over a crude
interface: a hand-rolled back stack, no settings, text wrapping into a column of two words. Three
baselines were written from that review, and each is a program rather than prose — the failure mode
this repository records most often is a rule only a document asserts. All of them run in the `ui`
group and print `file:line` findings.

| Module | Check ids | What it decides |
|---|---|---|
| `scripts/core/uiux.mjs` | `uiux.navigation`, `uiux.adaptive`, `uiux.text-layout`, `uiux.motion`, `uiux.a11y`, `uiux.insets`, `uiux.platform`, `uiux.content-colour`, `uiux.scaffold`, `uiux.rule`, `uiux.fixtures` | Navigation 3 hosts its own back stack; a multi-destination slot reads the window size class; text can wrap and grow; screens animate; every interactive element has a name and a 48 dp target; the keyboard is placed; content is not drawn under the status bar, the camera cutout or the gesture bar; and the app behaves like a modern Android app — predictive back opted in, no alpha Material 3 Expressive artifact on a pinned BOM; default-coloured text is visible against the colour it is drawn on rather than falling back to Compose's black on a dark background; and the scaffold carries all of it before any slot does |
| `scripts/core/settings.mjs` | `settings.surface`, `settings.reachable`, `settings.language`, `settings.theme`, `settings.behaviour`, `settings.strings`, `settings.persistence`, `settings.documented` | a settings surface exists, is reachable from the app, and every preference in it reaches the code that acts on it — a toggle that changes nothing is theatre |
| `scripts/core/localization.mjs` | `locale.multilingual`, `locale.placeholder`, `locale.hardcoded`, `locale.pseudolocale` | a slot is born multilingual; a declared tree is a translation rather than a copy or a sentinel; no user-facing prose is compiled into Kotlin; no pseudolocale ships |
| `scripts/core/baseline.mjs` | — (shared) | **which slots a new rule gates.** The creation date comes from `registry/apps.json` and "is this an app at all" from the presence of an `economics.json`, so the four slots that already existed carry a finding as a printed note instead of a permanent red row, while every slot created from 2026-09-30 is gated. A finding that exists and is not reported would read exactly like a finding that is not there, so the note is printed either way |

Three deliberate ownership splits, recorded so the modules cannot drift into duplicates:
`locale.multilingual` is **not** a restatement of `slot.locales` — `scripts/core/policy.mjs`
`localeProblems()` already fails both directions of `locales_config.xml` ⟷ `values-<locale>/` and
fails per key on incompleteness, and `localization.mjs` covers only the two gaps that were open (a
slot born monolingual, and a tree that is a copy or a placeholder). `uiux.text-layout` owns the
statically visible causes of forced wrapping, never the layout question of whether the longest
locale's words fit; `localization.mjs` owns the translation pipeline and hands that finding over.
And `uiux.scaffold`, `settings.*`'s template rows and `locale.scaffold.*` judge
`scaffold/android-compose/`, because a finding there is the count of slots that would be born with
the defect.

`scripts/core/facts.mjs` exports the successor of the two deleted ledger gates — `checkFactLedger`
(verdict id `facts.ledger`) and `factFixtures` (verdict id `facts.fixtures`) — and the registry lists
it, so `node scripts/verify.mjs --only invariants` runs both. They are also callable alone:

| Command | What it decides | Observed 2026-09-30 |
|---|---|---|
| `node -e "import('./scripts/core/facts.mjs').then(m=>console.log(m.checkFactLedger()))"` | every measured fact inside its window, enforced, or honestly `UNENFORCED` | `PASS` — `12 of 18 fact(s) name an enforcement that exists … 6 UNENFORCED` |
| `node -e "import('./scripts/core/facts.mjs').then(m=>console.log(m.factFixtures()))"` | the ledger check can still fail (18 inline fixtures) | `PASS — 18 fixtures` |

### The workspace boundary, added 2026-10-01

`build.scratch` (in `scripts/core/build.mjs`, so it runs in every `invariants` pass) is the one check
that is about the *host machine* rather than about the factory or a slot. It encodes the owner's
instruction after a Gradle build filled his system temp directory and slowed the machine: a factory
process writes its temporary files under `.kiln/scratch/` in this repository and writes **nothing**
outside it. The rule is `.cursor/rules/workspace-hygiene.mdc`; `scripts/lib/scratch.mjs` is the single
module that decides the location, and `scripts/lib/gradle.mjs` routes its spawn environment through it.

It fails when `.kiln/scratch/` is missing, resolves outside the repository or is not gitignored; when
`scratch.mjs` stops exporting `SCRATCH_DIR`/`scratchDirFor`/`scratchEnv`, stops redirecting `TEMP`,
`TMP` or `TMPDIR`, or derives its root from anything but its own file location; when `gradle.mjs` stops
building its environment from `scratchEnv()`; and when any module under `scripts/` names `tmpdir()`,
which is how the system temp directory gets chosen by accident. Fourteen fixture sandboxes did exactly
that — four of them in the checks that run on **every** `verify.mjs` invocation — and all fourteen now
build under `SCRATCH_DIR`.

| Command | What it decides | Observed 2026-10-01 |
|---|---|---|
| `node scripts/verify.mjs --only invariants` | the row above, plus every other in-process check | `PASS` — `build.scratch  every spawner writes under scripts/lib/scratch.mjs's SCRATCH_DIR, which is gitignored as .kiln/; no module under scripts/ names the system temp directory` |
| `node -e "import('./scripts/core/build.mjs').then(m=>console.log(m.systemTempProblems('mkdtempSync(path.join(os.tmpdi'+'r(),\"x\"))')))"` | the sensor that fires on a system-temp call | one finding, naming the system temp directory |

What it cannot see is stated in the rule rather than implied here: a **reused** Gradle daemon keeps the
environment it was started with, so a daemon older than the rule keeps writing to the old location
until it is stopped (`--no-daemon` is the forbidden fix), and a process the factory did not spawn —
a tool an agent runs by hand, a provider SDK's own cache, a browser's download directory — is outside
any sensor in this repository.

### The naming contract, added 2026-10-01

The app's name is a recorded product decision now, beside the money plan and the promise list. The
owner's correction is concrete: an agent building a utility names it after its category, and a title
that is the bare category noun spends Play's strongest ranking field on the head term the incumbent
already owns. `docs/app-naming.md` is the contract, `scripts/lib/slot-naming.mjs` is its parser
(`parseNaming`, `validateNamingAgainstListing`), and `scripts/check-naming.mjs` is the gate.
`scripts/core/naming.mjs` carries both halves of the check, so `slot.naming` runs in the `policy`
group beside `slot.differentiators`, and `naming.fixtures` runs in `invariants` on every verify.

| Command | What it decides | Observed 2026-10-01 |
|---|---|---|
| `node scripts/check-naming.mjs --slot <slug>` | the recorded name parses, is not a bare category noun / an empty modifier / ALL-CAPS / promotional text, fits Play's 30-character cap in every declared locale, is carried by at least 5 scored candidates spanning 3 archetypes with the chosen one at ≥ 70/100 beside a rejected alternative, and that `store/listing.json` ships the same title that was approved | `UNVERIFIED`, exit 2, on `teacher-gradebook` — it carries no `naming.json`, and `new-slot.mjs` no longer creates one without the contract |
| `node scripts/check-naming.mjs --naming <path> [--validate-only]` | a contract that is not in a slot yet, which is how `new-slot.mjs` validates it before anything is written | `PASS` — `naming schema — title "Sunstone: Lawn Round Log", archetype carrier_job_noun, 5 candidate(s), 1 rejected` |
| `node scripts/verify.mjs --only invariants`, row `naming.fixtures` | the sensor can still fail — 12 inline fixtures including `Calculator`, `My Calculator`, `CALCULATOR`, a 31-character title, and a pack whose title disagrees with the approved name | `PASS  naming.fixtures  12 fixtures` |

### The specification a slot is written inside, added 2026-10-02

The owner's rule is that every application is authored inside an OpenSpec change, and it was violated
in a way nothing could see: `apps/2026-10-02-laneset/` was scaffolded with code while
`openspec/changes/` held no change for it. `scripts/new-slot.mjs` required `--economics`,
`--differentiators` and `--naming` and asked for nothing about a specification, and no check under
`scripts/core/` read a slot's change — so the slot passed every gate while sitting outside the
specification the owner requires. `.cursor/rules/spec-first.mdc` states the boundary; the `slot.spec`
half runs in the `policy` group beside `slot.differentiators`, `slot.naming` and `slot.device`, and
its scope is **derived, not hand-listed**: `SPEC_BASELINE` in `scripts/core/baseline.mjs` is the day
the rule was measured, so a slot created before it carries the finding as a note while every slot
from that date is gated.

The **pre-creation half is stronger than the sensor**, and it is what makes the ungoverned slot
impossible to create rather than merely detected: `scripts/new-slot.mjs` now takes
`--spec <change-name>` (defaulting to `app-<slug>`) and **refuses before it writes anything** when
`openspec/changes/<spec>/` is missing or lacks `proposal.md`, `tasks.md` or a `specs/**/spec.md`,
naming the expected path and the remedy — authored with the `openspec-propose` skill and checked with
`openspec validate <change-name>`.

| Command | What it decides | Observed 2026-10-02 |
|---|---|---|
| `node scripts/verify.mjs --slot <slug>`, row `slot.spec` | the slot is written inside an OpenSpec change: `openspec/changes/app-<slug>/` — or a change whose `proposal.md` names the slot by a word-boundary token — carries `proposal.md`, `tasks.md` and at least one `specs/<capability>/spec.md`; a slot with no change is a finding, and one created before the baseline is a printed note | the sensor is `scripts/core/spec.mjs`, registered under `slot.spec` in the `policy` group (`scripts/core/index.mjs`); this pass did not run it |
| `node scripts/new-slot.mjs --economics <p> --differentiators <p> --naming <p> --slug <slug>` with no matching change | the scaffold refuses **before any file is created**, so the slot directory that was half of the original defect cannot exist | `FAIL` — `No OpenSpec change for this slot: expected openspec/changes/app-<slug>/ …`, exit 1, and `apps/` unchanged |

Its honest limit, stated rather than implied: the sensor proves a specification **exists and is
well-formed**, never that the app matches it — a slot can hold a valid, empty specification and pass.
That judgement stays with the owner at checkpoint 1, exactly as the naming and promise contracts'
own limits state.

### The device-run record, added 2026-10-02

The owner's correction: an agent ran `/forge`, wrote some code, and stopped asking him to install and
confirm the app, having tested nothing itself. `.cursor/rules/device-testing.mdc` already said the
connected phone is the test target and that reading the source is not a test, but **nothing read the
run**, and that rule said so out loud. The run is an artifact now —
`apps/<slot>/device-verification.json`, carrying the serial, the timestamp, the artifact path and its
sha256, and at least three observations of what was tapped and what appeared. `scripts/core/device.mjs`
carries both halves, so `slot.device` runs in the `policy` group beside `slot.differentiators` and
`device.fixtures` runs in `invariants` on every verify.

What decides whether the record still describes the app under review is the **revision, not the
artifact's bytes** — and the reason is measured, not stylistic: a debug APK is not byte-reproducible
across builds, so an artifact-hash-only comparison read an identical-source rebuild as a stale install
and forced a truthful record to be hand-edited after every rebuild. A record may therefore carry
`buildDigest` — the digest `releaseSourceDigest` in `scripts/lib/release-approval.mjs` computes over
the slot's build inputs, **imported by `scripts/core/device.mjs` rather than copied** — and when it
equals the slot's current digest the row `PASS`es whatever the artifact's bytes hash to, the detail
naming the revision as the fact that carried the verdict. A differing digest `FAIL`s, naming the
changed app and the remedy (drive the journey again on the phone). A record with **no** `buildDigest`
falls back to the artifact cross-check, and a mismatch names **both** remedies. Because a record that
stores the revision's digest must not sit inside the digest it stores, `device-verification.json` is
one of the input list's exclusions (`.cursor/rules/release-gate.mdc`).

Its honest limit: it proves a record names **this revision** — or, with no `buildDigest`, agrees with
the built artifact — never that the run happened, that the taps were real, or that an observation
describes what was on the screen. A fabricated record passes it, and the rule says so rather than
implying more.

| Command | What it decides | Observed 2026-10-02 |
|---|---|---|
| `node scripts/verify.mjs --slot <slug>`, row `slot.device` | the record names a serial, a parseable timestamp, a 64-hex artifact sha256 and at least 3 observations; when it carries a 64-hex `buildDigest` the **revision** decides — a match `PASS`es even when the APK bytes differ, a mismatch `FAIL`s naming the changed app — and only with no `buildDigest` is the built APK's hash recomputed and a disagreement failed, naming both remedies | `PASS` on `lawn-round-log` — `on-device run on serial R58MB3SGZVL … with 9 observation(s); apkSha256 matches … as built` (the record carries no `buildDigest`, so the fallback ran) |
| `node scripts/verify.mjs --only invariants`, row `device.fixtures` | the sensor can still fail — 20 inline fixtures including a rebuild whose `buildDigest` matches but whose APK bytes differ (must `PASS`), a digest that no longer matches (must `FAIL` naming the changed app), a hash mismatch with no digest (must name both remedies), 2 observations, a non-hex `buildDigest`, a missing serial and a wrong `schemaVersion`, with "no record at all" asserted as `UNVERIFIED` rather than `FAIL` | `PASS  device.fixtures  20 fixtures` |

### The pack-provenance trigger, corrected 2026-10-02

`pack.provenance` used to treat the presence of `store/studio.json` as "the pack declares itself
composed" and demand `store/provenance.json`. The two files belong to different phases: the
declaration is authored before the UI is styled (`.cursor/rules/visual-identity.mdc`), while the
record is written only by `scripts/media-capture.mjs` in phase 2, which refuses without the owner's
recorded release approval — so every slot in development was permanently red on this row. The check
now keys on the pack **shipping composed media** — a `store/assets/` tree holding at least one file,
through `hasComposedMedia(slotDir)` in `scripts/core/assets.mjs` — so media with no record is `FAIL`,
no media is `SKIP`, and deleting the `store/studio.json` declaration cannot switch the check off
(the anti-evasion property the old line also held).

| Command | What it decides | Observed 2026-10-02 |
|---|---|---|
| `node scripts/verify.mjs --slot <slug>`, row `pack.provenance` | when `store/assets/` holds at least one file, `store/provenance.json` exists and every committed asset hashes to what the record says; no composed media is `SKIP` rather than a demand for a phase-2 record | `PASS` on `teacher-gradebook` — `18 committed asset(s) match the recorded build; 6 raw capture(s) under the gitignored captures/ not checked` |
| `node scripts/verify.mjs --only invariants`, row `assets.fixtures` | the sensor can still fail — the six provenance legs (`store/studio.json` with no media must `SKIP` naming no composed media, media with no record must `FAIL`, a matching hash must `PASS`, a disagreeing hash must `FAIL` printing the real hash, the declaration deleted with media present must still `FAIL`, and an empty `store/assets/` must `SKIP`) drive the real `checkProvenance` over a synthetic tree | `PASS  assets.fixtures  18 fixtures` |

Its honest limit: it proves the committed images are bound to a recorded build — never that the
screenshots show the shipped behaviour, because the record binds a hash, not a journey.

### The iconography split and its provenance record, added 2026-10-02

Two owner instructions arrived together after he installed a built app and looked at it: the icons are
hand-drawn imitations — a modern app **downloads ready-made, professional icon sets** — and the
palette reads as neon. The second is a separate change with its own sensor. For the first, the mark
and the UI icons are one concern with two different answers, so the ban was split rather than
softened: **the launcher mark and the brand mark stay slot-owned and custom**, and **UI iconography
must come from a licence-cleared, vendored set** rather than a Canvas imitation.
`.cursor/rules/visual-identity.mdc` carries the split, `docs/icon-provenance.md` is the machine
contract for `apps/<slot>/icons/provenance.json`, and `scripts/core/icons.mjs` is the sensor. Seven
legs run in the `icon` group beside the adaptive-icon check. Its `reports` list is written out
literally rather than derived, because the fact ledger reads the registry as *text* and can only name
an id that appears there as a literal `id:`; a load-time assertion fails if it ever drifts from the
sensor's own `ICON_IDS`.

The defect it was built from is measured, not stylistic: the live slot's three launcher vectors were
identical in geometry to `scaffold/android-compose`'s placeholder, and none of their fill colours was
a role of the slot's own palette — the app had no mark of its own at all.

| Command | What it decides | Observed 2026-10-02 |
|---|---|---|
| `node scripts/verify.mjs --slot <slug>`, row `icons.mark` | the mark `store/studio.json` declares resolves in the Python-owned `MARK_REGISTRY`, so the pack cannot declare an identity the studio cannot render | `PASS` on `lawn-round-log` — `3 registered: pipe, mark-book, mark-stripes` |
| `node scripts/verify.mjs --slot <slug>`, row `icons.launcher` | the launcher vectors are the slot's own rather than the scaffold's, judged by path geometry against `scaffold/android-compose` **and** by whether each fill colour is a role of the slot's palette | `FAIL` on `lawn-round-log` — all three vectors (`foreground`, `background`, `monochrome`) are the placeholder; **fires on none of them** on `teacher-gradebook`, whose vectors are its own |
| `node scripts/verify.mjs --slot <slug>`, rows `icons.provenance` / `icons.licence` | `icons/provenance.json` resolves — schema, SPDX allowlist, every file present and hashing to what it claims — and each declared SPDX has committed, non-empty licence text | `UNVERIFIED`, exit 2, on every slot: none carries the record yet, which is true rather than a false alarm |
| `node scripts/verify.mjs --slot <slug>`, rows `icons.offline` / `icons.deprecated` / `icons.tint` | no icon artifact is fetched at build or run time; no deprecated `material-icons-core`/`-extended` coordinate (the pinned Material 3 no longer carries it); UI iconography carries no literal colour, the mark excluded by design | `PASS` on `lawn-round-log` |
| `node scripts/verify.mjs --only invariants`, row `icons.fixtures` | the sensor can still fail — 23 inline fixtures across the seven legs | `PASS  icons.fixtures  23 fixtures` |

Falsifiability was observed rather than assumed. Neutering the `icons.launcher` leg so it returned no
problems turned `icons.fixtures` `FAIL` (`the scaffold copy is a finding: expected 1, got 0`), and the
sensor was then restored byte-identically with its sha256 compared before and after — a check that has
never been made to fail is not yet known to be able to.

**Adversarial audit and repair, 2026-10-02** — the first delivery was audited by a separate agent and
the audit was right about nine defects, each now fixed with a fixture that fails without the fix and a
mutation proof that restores the file byte-identically (sha256 `b07468e7…` before and after every
mutation). The one that mattered most was a gate that could not report at all: a provenance record
whose `icons[].file` resolved to a **directory** made the hash read throw `EISDIR` out of
`iconProblems`, so `iconsChecks` returned **all seven legs `UNVERIFIED`** and `iconsFindings()` threw
— a real `icons.launcher` `FAIL` produced no row, and a malformed record masked the defect the gate
exists to find. Two were evasions, where a scaffold copy renamed its namespace prefix
(`a:pathData=…`) or used single-quoted attributes and reported `PASS` with its geometry unchanged.
One had the shipped-source fetch list narrower than the build-file list, one accepted a **directory**
as a licence file, and two read comments as code — failing on a URL or a deprecated artifact *name* in
a comment. The leg that judges tint matched `android:color` where rule 10 is written about
`android:fillColor`, and failed an in-app brand mark although `docs/icon-provenance.md` §9 makes that
mark identity and it legitimately carries the palette; it now reads `fillColor` only and treats a
literal that **is** a role of the slot's own palette on a slot-owned drawable as identity, while
`ic_delete.xml` with `#FF0000` still `FAIL`s. The launcher leg's palette half became a real criterion
after the prose claimed it and the code only printed it, and a header that still said *not registered
yet* was corrected. Fixtures went 16 → 23, none weakened.

Its honest limit, stated rather than implied: it cannot tell a beautiful icon set from an ugly one.
It can say the set was licence-cleared, vendored, and tinted from the theme — the judgement of the
mark and the palette stays with the owner at checkpoint 1.

### The palette sensor, added 2026-10-02

The same owner review that produced the iconography split produced this half. The palette of a built
app read as **neon** — a high-chroma lime accent on a dark green field — and the diagnosis with
numbers is `docs/research/2026-10-02-color-systems-and-palette-derivation.md`. The factory had a
strong contract for *declaring* a palette and nothing that judged one, so a pack could ship the first
palette an agent thought of and every gate returned `PASS`.

`scripts/core/palette.mjs` runs in the `palette` group and reads the **declared** palette in
`store/studio.json` against the slot's own `ui/theme/Color.kt`. Like the icon group, its `reports`
list is written out literally because the fact ledger reads the registry as *text*, with a load-time
assertion against the sensor's own `PALETTE_IDS`. It imports `scripts/lib/hct.mjs` and nothing else —
the palette maths, implemented dependency-free so no npm package enters the factory, and **verified
rather than asserted**: its 19-row `selfTest` reproduces the research document's own measured values
(the accent's HCT `H123.4 C68.0 T89.5`, the 1.884 accent-to-container ratio the document rounds to
1.88, `maxChromaFor` at 100.0% for the in-gamut primaries, and the derived `CRITICAL_PLANES` matching
the reference implementation), and its WCAG half agrees with the factory's own Python `colours.py` to
six decimal places on every pair tried — 21, 10.951179, 1.308703, 1.441378, 14.331843.

| Command | What it decides | Observed 2026-10-02 |
|---|---|---|
| `node scripts/verify.mjs --slot <slug>`, rows `palette.declared` / `palette.agreement` | the record declares a palette whose every entry names a role that resolves to a hex, and every declared role is the colour the app's own `Color.kt` carries — `visual-identity.mdc`'s one-source-of-truth rule | `PASS` on `lawn-round-log` — 11 roles against 26 `Color.kt` constants; on `teacher-gradebook` it reports a real drift, `ink_soft` `#2A2568`, as a non-gating note |
| `node scripts/verify.mjs --slot <slug>`, row `palette.contrast` | WCAG 4.5:1 over every `on-X`/`X` pair across light and dark | `PASS` on `lawn-round-log` — 24 pairs, weakest 5.43:1 |
| `node scripts/verify.mjs --slot <slug>`, rows `palette.field-chroma` / `palette.hue-structure` / `palette.accent-chroma` | a field is neutral at C ≤ 12; the accent sits ≥ 40° from its fields; and the accent's chroma is within 1.5× what **Material 3's own algorithm** derives at the same hue and tone | `FAIL` on `lawn-round-log` — the accent `#C9F04A` is C68 = **1.88×** the C36.1 Material 3 derives, and its light field `#DCEBD3` sits **19.8°** away at a tone gap of 1.9 |
| `node scripts/verify.mjs --slot <slug>`, row `palette.duplicate` | two roles a user cannot tell apart — under 1.2:1 contrast **and** under 15° hue **and** under C20, all three | `FAIL` on `lawn-round-log` — `amber_pale` `#EAF9C6` and `paper` `#F6F4E4` differ by **1.010:1** |
| `node scripts/verify.mjs --only invariants`, row `palette.fixtures` | the sensor can still fail — 47 inline fixtures across the nine legs | `PASS  palette.fixtures  47 fixtures` |

Falsifiability was observed rather than assumed, and one mutation is worth reading twice: neutering
rule 13 turned **six** legs red, including `palette.accent-chroma` and `palette.hue-structure`, because
those legs classify a field as chromatic with the same predicate — so the mutation confirmed the legs
share a real dependency rather than each being independently green. The file was restored
byte-identically after every mutation (sha256 `FB2245AB…` before and after).

Its honest limits, stated rather than implied. It reads **declared** colours, so a dynamic-colour
palette chosen at run time, a colour inside a drawable and a colour in a `remember {}` block are
invisible to it — a green run is evidence about the declared system, never a certificate about every
pixel. Rule 15 is an **approximation** of a vendor algorithm rather than a call into one, and it was
not verified against Material's own output across a corpus of hues. The accent is **inferred** as the
highest-chroma chromatic role, because no slot declares which role is the accent. And it cannot say a
palette is beautiful: it says the palette is derived and legal, and the judgement stays with the owner
at checkpoint 1.

### The grounded-claims duty, added 2026-10-02

The owner's report — a scout said a pocket was good, the UI/UX work said the design was good, and
neither answered with facts — is the defect `.cursor/rules/grounded-claims.mdc` stops. The duty is
behavioural, so the sensor can only read the **declaration**: `harness.groundedClaims` in
`scripts/core/harness.mjs`, in the `invariants` group, checks that the rule loads (`alwaysApply: true`),
that every role card under `harness/roles/` names it, and that every agent card under `.cursor/agents/`,
`.codex/agents/` and `.claude/agents/` names it or delegates to a role card.
`docs/grounded-claims.md` is the contract, and `harness.groundedClaimsFixtures` is the inline set that
proves the sensor can fail.

| Command | What it decides | Observed 2026-10-02 |
|---|---|---|
| `node scripts/verify.mjs`, row `harness.groundedClaims` (the `invariants` group) | the rule loads, every `harness/roles/*.md` card names it, and every agent card names it or delegates to a role card | `FAIL` in this working tree — the five role cards name the rule; the agent mirror cards (`android-debugger`, `evaluator`, `generator`, `money-owner`, `planner`, `play-auditor`) do not yet, and that pass is still in flight |
| `node scripts/verify.mjs --only invariants`, row `harness.groundedClaimsFixtures` | the sensor can still fail — 9 inline fixtures over synthetic card sets | `PASS  harness.groundedClaimsFixtures  9 fixtures` |

Its honest limit, stated rather than implied: it proves the duty is **declared**, never that a
particular sentence is true. A fabricated claim ledger passes it, and the rule says so rather than
implying more.

### The device-capability declaration, added 2026-10-02

Every proposal now declares, one line per capability, which device capabilities its job uses and which
it does not — the field that turns the factory's measured monoculture ("the user types something and
the app stores it", with the README's reading of camera 0, location 0, motion sensors 0, home-screen
widget 0 and media playback 0 across the proposals) from a silence into a decision.
`docs/ideation/README.md` is the contract, `harness/roles/scout.md` binds it to the scout, and
`scripts/core/ideas.mjs`'s `ideas.capabilities` is the sensor, in the `invariants` group. It reports a
proposal with no declaration, a capability left unanswered, a `used` line that names no API or no
loss, and a declaration that contradicts the document's own permissions prose or omits the manifest
permission a `used` capability needs. The platform fact at its centre is that the Android Photo Picker
(`ActivityResultContracts.PickVisualMedia`) asks for **no runtime permission at all**, so the
declaration is a statement about the job rather than a cost against permission minimisation. Its scope
is derived, not hand-listed: `PROPOSAL_CAPABILITY_BASELINE` in `scripts/core/ideas.mjs` is the day the
field was measured, so a proposal written before it carries the finding as a printed note while every
proposal from that date is gated.

| Command | What it decides | Observed 2026-10-02 |
|---|---|---|
| `node scripts/verify.mjs --only invariants`, row `ideas.capabilities` | every proposal carries a `## Device capabilities` section answering `used`/`not used` for all twelve capabilities, each `used` line naming an API and the loss without it, with no contradiction against the document's own permissions prose and none missing its manifest permission | `PASS` — `12 proposal document(s) read, 0 gated finding(s); note: 12 predate the 2026-10-03 baseline and carry the finding as a note rather than a FAIL` |
| `node scripts/verify.mjs --only invariants`, row `ideas.fixtures` | the sensor can still fail — 13 inline fixtures including a missing declaration, an unanswered capability, a `used` line with no API, a `used` line with no justification, and the negative control that a `not used` capability is not contradicted by a permissions section saying "no camera" | `PASS  ideas.fixtures  13 fixtures` |

## 2. The retired gate — `scripts/verify-slot.mjs`

**`scripts/verify-slot.mjs` is deleted.** It was the pre-core gate: a 1,600-line monolith that spawned
the per-slot check scripts, the pack validators, the factory self-test and the freshness gate, so a
one-line fix paid for a chain of runners and a green run could not be read as anything. It had already
become red for a reason that was not the app's — it still read the deleted `scripts/lib/console-form.json`
and still expected the deleted `scripts/store-kit.py`.

The replacement is `node scripts/verify.mjs`: one process, every check in `scripts/core/`, one row per
check. The removal was refused twice by this change and permitted on the third attempt, and the
condition is the point — **nothing was deleted until the successor reported the same fact**:

| Rule the gate held | Successor now | Evidence on the reference slot |
|---|---|---|
| manifest, permissions, exported components, cleartext | `slot.manifest` | `PASS — 0 permission(s), none gated-and-undocumented, none a wildcard` |
| backup rules vs device transfer | `slot.backup` | `PASS — dataExtractionRules separates cloud-backup from device-transfer` |
| capability compliance | `slot.capabilities` | `PASS — capabilities.json enables billing; every enabled capability's SDK is declared` |
| in-slot secret paths are gitignored | `slot.secretPaths` | `PASS — 2 secret path(s) present and each ignored by .gitignore` |
| no unresolved `__TOKEN__` | `slot.tokens` | `PASS — no unresolved __TOKEN__ placeholder in 248 file(s)` |
| declared languages have resources (row 7.6) | `slot.locales` | agrees with the gate on all four slots, same sentence and counts |
| the promise contract | `slot.differentiators` | `PASS — 7 promise(s), 5 core, 7 shipped, 0 deferred` |
| a listing claim has a control | `slot.uiCoverage` | agrees with the gate on all four slots, including both refusals |
| a build-less run is `UNVERIFIED`, exit 2 (row 12.11) | `build` | `UNVERIFIED build — assembleDebug was not compiled…`; the row was `SKIP`, which **exits 0** |

The flags went with it, and for a reason rather than by tidiness. `--strict-store` selected a stricter
branch of two checks; both branches are now unconditional, so keeping the flag would be a flag that no
longer selects anything. `--base <ref>` (a diff scoped to changed files) and `--no-selftest` (a stage
with no program) are gone, as is the self-test stage itself.

The command the factory runs instead is one line, and the entry point's own output is the report:

```bash
node scripts/verify.mjs --slot <slug> --build
```

**Two deviations from the gate are real and are stated in the successor's own account**, not only here:
the git half of the promise gate (an absent `differentiators.json` is `UNVERIFIED` rather than `FAIL`,
because reading git needs a subprocess the core may not spawn), and the end-to-end `new-slot.mjs`
scaffold assertion, which cannot be an in-process check. Neither turns a `FAIL` into a `PASS`.

`openspec/changes/reduce-factory-to-core/deletions.md` §8.1 is the full ledger — the rule-by-rule
mapping, the 65 files retargeted, the two dated records a bug in the sweep's own filter caught and
which were restored, and the two consequences the deletion produced
(`build.copyFilter`'s stale guard list and a fact ledger entry naming the deleted script).

## 3. Per-slot checks and validators

Run in a session; none of them needs a device or a build.

| Command | What it decides | Observed 2026-09-30 (slot `teacher-gradebook`) |
|---|---|---|
| `node scripts/check-economics.mjs --slot <slug>` | the plan names a revenue mechanism its capabilities can honour | `PASS` |
| `node scripts/check-differentiators.mjs --slot <slug>` | every reason the app was chosen is delivered or dropped in writing | `PASS` |
| `node scripts/check-client-secrets.mjs --slot <slug>` | no key-shaped literal or paid-provider SDK in the shipped client | `PASS` |
| `node scripts/check-locales.mjs --slot <slug>` | declared languages have resources; a non-translatable key is declared once | `PASS` (11 languages × 518 keys; one `NOTE`) |
| `node scripts/check-in-app-policy.mjs --slot <slug>` | the app can open the privacy policy the pack declares | `PASS` |
| `pack.placeholders` (in the `assets` group of `node scripts/verify.mjs --slot <slug>`) | a `privacyPolicyUrl` that is absent, non-`https`, or on a reserved documentation host — the gate over the field, and it names `node scripts/publish-privacy.mjs --slot <slug>` as the remedy | `FAIL` on `2026-09-22-smoke` for `https://example.com/privacy`; `PASS` on the two published slots |
| `node scripts/check-outcomes.mjs --slot <slug>` | the plan's kill criteria against the recorded reading | `NO-DATA`, exit 2 — the honest state for a slot with no reading |
| `python scripts/check-adaptive-icon.py --slot <slug>` | the launcher mark fits the adaptive-icon masks | `PASS` — mark sits 4.08 dp inside the 33 dp safe zone |
| `python scripts/visual-qa.py --slot <slug>` | a **report** over the pack's pixels (palette, ink, clipping, contrast, similarity) | exit 0, 102 013 ms, `stability verdict: PINNED` |
| `python scripts/store/validate.py --slot <slug>` | the pack against the model, in pure Python — the slow independent cross-check | exit 1, 64 638 ms, 8 blocking rows (the reference slot's committed 5:8 tablet assets — a recorded, owner-accepted divergence) |
| `python scripts/store-kit.py --slot <slug> --validate-only` | the pack through the legacy renderer/validator | **retired**: the file is gone, split into `scripts/store/{model,render,validate}.py`; see §9 |

## 4. Factory and rule regression suites

Every command below was run on 2026-09-30 and passed.

| Command | Guards | Observed |
|---|---|---|
| `node scripts/test-slot-lock.mjs` | the per-slot lease: holder named, refusal by name, stale reclaim, JDK blocker | exit 0 — 49/49 |
| `node scripts/test-apk-provenance.mjs` | the chain from HEAD to the installed APK | exit 0 — 41/41 |
| `node scripts/test-client-secrets.mjs` | the secret detector, with negative controls | exit 0 — 55 checks, `CLIENT-SECRETS SENSOR PASSED` |
| `node scripts/test-locale-dedup.mjs` | a non-translatable key is declared once | exit 0 — `VERDICT: PASS` |
| `node scripts/test-in-app-policy.mjs` | the in-app policy sensor | exit 0 — 29 checks, `IN-APP POLICY SENSOR PASSED` |
| `node scripts/test-outcomes.mjs` | post-ship verdicts and the kill boundary | exit 0 — 70/70 |
| `node scripts/test-pack-approval.mjs` | the approval covers the authored pack, not just the build | exit 0 — 30/30 |
| `node scripts/test-kill-path.mjs` | `kiln-status --kill`: refuses without a reason, records the decision | exit 0 — 19/19 |
| `node scripts/test-stop-gate.mjs` | the `stop-gate` hook's quiet-turn behaviour | exit 0 — 13/13 legs, `VERDICT: PASS` |
| `node scripts/test-codex-signals.mjs` | the worker failure classifier | exit 0 — all checks passed |
| `node scripts/test-play-reviews.mjs` | the review-text sensor and its taxonomy | exit 0 — 39 checks, `SENSOR UNIT TESTS PASSED` |
| `node scripts/test-play-scan.mjs` | the Play search screen and its relevance guard | exit 0 — `PLAY-SCAN REGRESSION: all checks passed` |
| `node scripts/test-capture-assertions.mjs` | the capture journey's asserted readings | exit 0 — 26/26 |
| `node scripts/test-capture-locale.mjs` | the capture runs in the language its journey reads | exit 0 — `VERDICT: PASS (10 checks)` |
| `node scripts/test-capture-publish.mjs` | the capture publish step's fail-closed interruptions | exit 0 — 34/34 |
| `python scripts/test-visual-qa-discovery.py` | the visual-QA theme reader | exit 0 — `VERDICT: PASS` |
| `python scripts/test-icon-design.py` | the two icon renderers agree | exit 0 — `VERDICT: PASS (0 failing legs)` |
| `python scripts/test-checklist-products.py` | the generated console checklist against a billing slot's own Kotlin | exit 0 — 21/21 |
| `python scripts/test-release-notes.py` | the release-note format and its 500-character cap | exit 0 — 26/26 |
| `python scripts/test-console-answers.py` | the generated console answer sheet against the pack | **retired by the console port**: the file and its generator are gone; see §9 |

### Suites the 2026-09-30 deletion group did NOT retire, and the cases that stop them

Task 5.1 asked whether `scripts/test-client-secrets.mjs` and `scripts/test-release-notes.py` could go
now that their sensors have inline core fixtures. Both were kept, because the core's fixture sets
cover the sensor's *controls* and not the suite: `secrets.fixtures` (9 cases) covers the two negative
controls and the four shapes a line-oriented detector misses, `releaseNotes.fixtures` (5 cases) covers
the 500-character boundary and the code-point count. What neither covers is the half of each suite that
tests a **program** rather than a predicate. Deleting either would lose the only sensor for:

- **client secrets** — a long value assigned to a secret-shaped name (`provider:
  generic-secret-assignment`) at all; the version-catalog (`gradle/libs.versions.toml`) and
  `app/proguard-rules.pro` paths, and the commented-out provider declaration; `client-secrets-exempt.json`
  with and without a `reason`/`decidedBy`, and its unknown-rule and stale-entry cases; the CLI's exit 2
  on an unanswerable call and its `--json` regression; and the assertion that **every declared rule id
  fires in at least one fixture**.
- **release notes** — all of generation (`write_documents` writes the file, names it from the Gradle
  version, tags one note per listing language, marks it unfinished, and does not overwrite a hand-written
  note); `validate_release_notes`' release-name-versus-manifest rule, its missing-section rule, and its
  unbuilt-slot case; and the parser legs (backticked name, multi-line body, non-ASCII body, a body that
  does not absorb its tags, an inline tag that is not recognised, and prose mentioning a locale tag that
  is not a note).

## 5. Tools that are checks of the factory, not of a slot

| Command | What it prints | Observed 2026-09-30 |
|---|---|---|
| `node scripts/kiln-status.mjs` | every slot's phase, every slot's earnings verdict, the harness files that must exist, and the factory checks | see §8 |
| `node scripts/harness-inventory.mjs` | the factory's own surface, for a session that is about to change it | exit 0 |
| `node scripts/approve-release.mjs --check --slot <slug>` | whether this revision is approved for release | `NOT APPROVED`, exit 2 — correct: no approval is recorded for the current revision |
| `node scripts/console-walkthrough.mjs --check` | the walkthrough still matches the shared console model | **retired by the console port**: the file, its model and `docs/play-console-walkthrough.md` are gone; see §9 |

## 6. Commands that need a device, a build, a key or Play access

Listed so they are not forgotten, and **not run**: running them needs a Gradle lease, a device, a
signing key or an approval this session does not hold.

| Command | Why it is not run here |
|---|---|
| `node scripts/run-slot-tests.mjs --slot <slug> --rerun` | `[needs Gradle]` — executes the slot's unit tests |
| `node scripts/prepare-release.mjs --slot <slug>` | `[needs Gradle]` — builds, signs and stages the release; refuses without the owner's approval |
| `node scripts/sign-release.mjs` | needs the owner's signing key and explicit permission |
| `node scripts/media-capture.mjs --slot <slug>` | `[needs a device]` — replays journeys against the installed release build |
| `node scripts/play-upload.mjs` | needs Play credentials and an approved revision |
| `python scripts/check-16k.py <apk> <aab>` | `[needs built artifacts]` — measures the ELF alignment of a finished build |
| `python scripts/test-store-studio-completeness.py` | `[needs local captures]` — reads the gitignored `store/captures/raw/`, minutes-scale by design |
| `node scripts/publish-privacy.mjs --slot <slug> [--check]` | `[needs network]` — `--check` only fetches the already-recorded URL; publishing creates a real public page. The **factory** publishes (Telegraph), so the owner's host is not needed — `docs/privacy-policy-hosting.md`. Run it for one slot during development; `--all` exists for a deliberate sweep across the slots that need it and a bare invocation publishes nothing |
| `node scripts/new-slot.mjs --economics <p> --differentiators <p> --naming <p> [--spec <change>]` | creates a slot; refuses before writing anything without a validated plan, promise contract, naming contract and an existing OpenSpec change; run only after the owner picks an idea |

## 7. Live-network checks and scout tools

These read Google Play over the network. The three validators below were run on 2026-09-30 and
answered; the scout tools are **not checks** — they are the demand-measurement instruments the scout
uses, and nothing fails when they are not run.

| Command | Observed 2026-09-30 |
|---|---|
| `node scripts/validate-play-scan.mjs` | exit 0 — `SENSOR VALIDATED: 6 definitional verdicts reproduce, and 5 blind spot(s) behave as declared` |
| `node scripts/verify-play-scan-defects.mjs` | exit 0 — `confirmed defect classes reproduced: 0` |
| `node scripts/verify-reviews-sensor.mjs` | exit 0 — 15 checks, `SENSOR VERIFIED` |
| `node scripts/scan-market.mjs`, `enrich-market.mjs`, `scan-titles.mjs`, `scan-complaints.mjs`, `scan-ai-niches.mjs`, `scan-plant-vertical.mjs` | not run — instruments, not gates |
| `node scripts/codex-run.mjs list` | not run — the Codex worker channel, not a check |

## 8. The observed outputs §1, §2 and §5 point at

Run 2026-09-30 at ~20:35 +03:00 in this working tree (PowerShell, no Gradle, no device), **after every
deletion in §5 of the change and the retirement of the legacy gate**. The earlier block that stood here
recorded 16 PASS with one `UNVERIFIED config.parse`; that row is green now, because the console port
landed the replacement for the deleted `scripts/lib/console-form.json`.

`node scripts/verify.mjs --only invariants` — **22 PASS, exit 0** (5,234 ms, `subprocesses: 0`):

```text
PASS       facts.fixtures         18 fixtures
PASS       harness.fixtures       10 fixtures
PASS       harness.approvalExclusions 12 excluded name(s) across 3 reason(s), each reason used and
                                  named in the criterion
PASS       harness.placeholderHosts   6 reserved host(s), identical in both halves
PASS       manifest.fixtures      21 fixtures
PASS       policy.fixtures        24 fixtures
PASS       core.size              core 10492/10500; legacy 27070/51500; scripts total 37562 (target 9000)
verdict: PASS
```

`node scripts/verify.mjs` — **22 PASS, 7 SKIP**, verdict `SKIP`, exit 0 (3,603 ms). The seven `SKIP`
rows are the per-slot groups reporting `no --slot given, so this check did not run`, which is the one
state that is neither a pass nor a failure and says so; the closing line names all seven so the
`SKIP` verdict cannot be read as a full pass.

The rows worth reading are the ones that exist because something was lost. `facts.ledger` is the
successor of both deleted ledger gates and is falsifiable (18 fixtures); it proved itself on real data
in this pass by going **red** naming a fact that still claimed the deleted `scripts/verify.mjs` as its
enforcement. `harness.approvalExclusions` and `harness.placeholderHosts` are the two assertions that
lived only in the deleted `selftest.mjs` (§4 of the change's `deletions.md`).

`node scripts/verify.mjs --slot teacher-gradebook` — **43 PASS, 1 FAIL, 1 SKIP, 1 UNVERIFIED**
(7,056 ms, `subprocesses: 0`):

```text
FAIL       pack.media             en-US/tablet-7-01.png: tablet-7: 1200x1920 is not 16:9 or 9:16; … (8 rows)
PASS       pack.provenance        18 committed asset(s) match the recorded build
PASS       slot.manifest          0 permission(s), none gated-and-undocumented, none a wildcard
PASS       slot.backup            dataExtractionRules separates cloud-backup from device-transfer
PASS       slot.capabilities      capabilities.json enables billing; every enabled capability's SDK is declared
PASS       slot.locales           11 translated language(s) x 518 key(s) complete
PASS       slot.differentiators   contract schema — 7 promise(s), 5 core, 7 shipped, 0 narrowed, 0 dropped, 0 deferred
PASS       slot.uiCoverage        every operation the listing claims is reachable from a screen
SKIP       slot.outcomes          no recorded reading; NO-DATA is an honest state, not a failure
UNVERIFIED build                  assembleDebug was not compiled because --build was not passed
```

`pack.media` is the owner-accepted 5:8 divergence of the shipped listing (§9), not a regression from
these deletions. The `UNVERIFIED build` row is row 12.11 working: a build-less run says so and does
**not** exit 0 while claiming a pass — pass `--build` and it becomes `PASS build … subprocesses: 1`.

`node scripts/kiln-status.mjs` reports the factory from the same entry point rather than from the
presence of a file.

## 9. Open observations from this session

Recorded because a reader of §3 would otherwise see a red row and guess:

- **`scripts/store-kit.py`, `scripts/lib/console-form.json`, `scripts/console-walkthrough.mjs` and
  `scripts/test-console-answers.py` are gone** — deleted by the concurrent console/store port, not by
  this task (they are on this change's do-not-touch list). `scripts/store/{model,render,validate}.py`
  are the successors on disk and are listed in §3/§4. One consequence is still open: `config.parse`
  reads `scripts/lib/console-form.json` from `scripts/core/config.mjs` (a do-not-touch file here), so
  that row is `UNVERIFIED` until the port lands its replacement. (Resolved 2026-09-30: the
  deletion group removed `scripts/lib/console-form.json` from `JSON_CONFIGS` in
  `scripts/core/config.mjs`, so `config.parse` now reports `PASS 3 configuration file(s) accepted`
  and `node scripts/verify.mjs` carries no `UNVERIFIED` row. The pasted output above is this
  session's reading and keeps its wording.)
- **`scripts/store/validate.py` exits 1 on the reference slot** with 8 blocking rows, all
  `screenshot:aspect` on the committed `tablet-7-*`/`tablet-10-*` assets. This is the divergence the
  owner accepted on 2026-09-30 (`tasks.md`, run log): the live listing already carries images Play
  accepted, so the assets stay at 5:8 and the rule is not relaxed. The same divergence is the
  `pack.media` FAIL in §8.
- **`scripts/verify.mjs` is green** (resolved 2026-09-30). While the store port was in flight it read
  the deleted `scripts/lib/console-form.json` and expected the deleted `scripts/store-kit.py`, so it
  exited 1 with `FAIL store pack inventory`; the port repointed it to `scripts/store/validate.py`, so
  the platform check is `PASS` and the reading is in §8.
- **`scripts/check-outcomes.mjs` exits 2 (`NO-DATA`)** for every slot, because no slot has recorded an
  outcome reading. That is the defined honest state, not a failure.
- **`factFixtureCases()` and the `facts.ledger` check are the only sensors of the measured-fact
  ledger.** The 18 fixtures live in the same file as the check, so the ledger cannot be verified by a
  command that forgets to run them.
- **`scripts/test-core-equivalence.mjs` was deleted on 2026-09-30** (task 5.1's deletion group) and
  its `kiln:equivalence` npm key with it. It was the change's own migration scaffolding: it compared
  the core's per-asset measurements against `.kiln/golden/verify.mjs.txt`, which `.gitignore` line 4
  excludes (`.kiln/`), so in any clone but the author's working tree it can only print
  `not comparable: no golden report …` and exit 2 — a check that cannot pass is not a check. Its one
  green reading is recorded in this file's own history and in the change: `RESULT: equivalent`,
  16 assets compared, 8 `FAIL` assets in both the golden run and the core.
- **`scripts/harness-inventory.mjs` stays, and the reason is recorded rather than assumed** (task 5.5:
  the unmotivated checks). The search for the incident that produced it,
  `rg -n --hidden 'harness-inventory' harness/lessons.md docs/reviews .kiln`, returns **no hit in
  `harness/lessons.md` or `docs/reviews/**`** — only Codex worker logs under the gitignored
  `.kiln/codex/`, which are tool output and not a defect record — so no incident motivated it; its own
  header states its purpose, "so mirrored surfaces and specs cannot drift silently". It is kept anyway,
  because no surviving program does its job: the core's `harness.refs` check only *reads*
  `.cursor/agents` and `.codex/agents` to resolve the role cards they cite (`scripts/core/project.mjs`
  contains no `mirror`/`.claude` comparison at all), and `scripts/kiln-status.mjs` checks that required
  files exist and that `.cursor/mcp.json` parses. Neither asserts three-way agent mirror parity, counts
  the vendored official skills, or prints one description per script. A check whose job nothing else
  does is kept even when no incident wrote it.

## 10. The rows §2 was blocked on — honoured, with four residuals stated

The section that used to be here recorded nine rules that the single entry point did not carry, so
deleting the legacy gate was refused. **Every one of them is honoured now**, and the gate is deleted;
this table is the closure, kept so a reader can check the claim rather than trust it.

| Rule | Ledger row | Successor now | State |
|---|---|---|---|
| Backup rules separate cloud backup from device transfer | 9.7 `CORE` | `slot.backup` in `scripts/core/manifest.mjs` | **honoured** — reads the manifest and the XML it names |
| Permissions minimized; no gated dangerous permission; no exported component; no cleartext | 9.6 `CORE` | `slot.manifest` in the same module | **honoured** |
| Capability compliance (registry, permissions, ads declaration, purchase authority) | 15.7 `CORE` | `slot.capabilities` | **honoured** — the `--strict-store` branch is now unconditional, because a stricter branch behind a flag is a check that does not run by default |
| In-slot secret files are present only where `.gitignore` covers them | 16.11 `CORE` | `slot.secretPaths` | **honoured** |
| No unresolved `__TOKEN__` survives in a slot | — | `slot.tokens` | **honoured** (248 files of the reference slot swept) |
| A build-less run reports `UNVERIFIED` (exit 2), never a pass | 12.11 `CORE` | `scripts/verify.mjs`'s exit-code contract | **honoured** — the row used to be `SKIP`, which exits **0**; a caller reading only the exit code could not tell it from a full pass. That was a live defect in the successor, found by porting the row rather than inheriting it |
| The promise contract (`differentiators.json`) | — | `slot.differentiators` in `scripts/core/policy.mjs` | **honoured**, one half excepted: git is not read, so an absent contract is `UNVERIFIED` naming the path rather than `FAIL`. Extended 2026-10-02 with the depth contract (`.cursor/rules/app-depth.mdc`, the reason a slot's promise list *is* its scope): the job's `before`/`during`/`after` stages must each resolve to a claim or a written refusal, `during` never refusable, and at least one `shipped` claim must be a connective family whose proof artifact contains that family's named API. The CRUD report prints and never gates — the measured `edit-every-record` row escapes a lexical predicate because it carries a contrast clause — so these rows prove the handoff was **asked for**, never that it works |
| Declared app languages have resources | 7.6 `CORE` | `slot.locales` | **honoured** — agrees with the gate on all four slots, including the counting text |
| A listing feature has a control in some screen | — | `slot.uiCoverage` | **honoured**, including the two `UNVERIFIED` refusals (`harness/lessons.md` 2026-09-30, review decision D6) that stop it passing a slot it cannot check |
| The release name agrees with `dist/release-manifest.json`; a listing language with no note section fails | 7.4 `CORE` | `scripts/store/validate.py` | **honoured where it runs**: the Python validator is read-only, reproduces the golden pack verdict, and is a deliberately-run cross-check rather than a per-edit gate, because it decodes PNG pixels and 118 s is too slow for the inner loop |

**Four residuals, named rather than smoothed over.** Each is in the successor's own account with the
same wording, so a reader meets it where it matters:

1. **The git half of the promise gate.** An absent `differentiators.json` is `UNVERIFIED`, not `FAIL`:
   the core may spawn only the build tool, and reading git needs a subprocess. No slot is in that state.
2. **The end-to-end `new-slot.mjs` scaffold run.** `new-slot.mjs` exports the exclusion predicate and
   the walk but no scaffolding entry point, so an in-process check cannot run it. `build.copyFilter`
   sweeps and probes the predicate itself; the end-to-end half is unguarded.
3. **`test-store-studio-completeness.py` reports 27 of 28**: `FAIL the render is deterministic
   (re-render reproduces every asset) - 18 before, 18 after`. Two renders of the same input differ
   byte-for-byte. A composed pack whose render is not reproducible cannot have its provenance
   reproduced, so this is an open **finding**, and it appeared the moment the suite could run at all —
   see §11.
4. **`scripts/core/budget.json`'s `coreLines` was raised, 9000 → 10500.** The number was an estimate
   written before two ports the ledger requires existed; the revision records the measurement and its
   reason in the file, and `legacyCeiling` — the one whose comment forbids raising it to make a run
   pass — is untouched.

## 11. Suites that were cited as evidence and could not execute

Twelve files under `scripts/` are named as suites in `AGENTS.md`, and they were run one by one in the
closing pass. **Two of them could not run at all**, on Python 3.13:

```text
File "<stdlib>/dataclasses.py", line 757, in _is_type
    ns = sys.modules.get(cls.__module__).__dict__
AttributeError: 'NoneType' object has no attribute '__dict__'
```

`dataclasses` resolves a `KW_ONLY` field's type through `sys.modules.get(cls.__module__)`, and five
dynamic loaders never registered the module they loaded: `scripts/store-studio.py`,
`scripts/store/render.py`, `scripts/test-store-studio-completeness.py`,
`scripts/test-release-notes.py` and `scripts/test-checklist-products.py`. `scripts/visual-qa.py`
already had the correct form, which is the precedent the fix follows. All five now register before
`exec_module` and drop the entry if the load throws.

That is the whole argument for running a suite rather than listing one: `test-icon-design.py` now
reports `VERDICT: PASS (0 failing legs)` — and it never could have before — while
`test-store-studio-completeness.py` immediately reported the non-determinism in residual 3, which no
amount of reading would have surfaced.
