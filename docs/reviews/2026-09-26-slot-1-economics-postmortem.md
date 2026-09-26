# Slot 1 economics post-mortem — `apps/2026-09-25-pipe-sizing`

**Verdict:** this was the factory's first commercial failure, and it failed before it
shipped: the slot was built as a placeholder topic rather than as a product with a
revenue mechanism, so it reached a green gate while having nothing to sell and no
reason to be chosen over the unrated incumbents the factory's own research had
already identified as a graveyard.

This is a post-mortem of a slot that is `scaffolded` in `registry/apps.json`, with no
Play upload, no production access confirmation, and no revenue of any kind. Every
fact below is traceable to a file read while writing it; anything that could not be
verified is labelled UNVERIFIED.

---

## 1. What was built

| Item | Value | Source |
|---|---|---|
| Slot | `apps/2026-09-25-pipe-sizing` | `registry/apps.json` |
| Name | `Gas Pipe Sizing Workbook` | `apps/2026-09-25-pipe-sizing/store/listing.json` |
| `applicationId` | `com.gexetr.pipesizing` | `dist/release-manifest.json`, `store/listing.json` |
| Version | `versionName 0.1.0`, `versionCode 1` | `apps/2026-09-25-pipe-sizing/dist/release-manifest.json` |
| SDK levels | `compileSdk 37`, `targetSdk 36`, `minSdk 26` | `apps/2026-09-25-pipe-sizing/dist/release-manifest.json` |
| Debug APK | 13,219,048 bytes, sha256 `5f21d6f9…c8b9a` | `apps/2026-09-25-pipe-sizing/dist/release-manifest.json` |
| Release APK | 1,449,830 bytes, sha256 `f8706756…30884` | `apps/2026-09-25-pipe-sizing/dist/release-manifest.json` |
| Release AAB | 3,436,412 bytes, sha256 `258d42b6…c8cb` | `apps/2026-09-25-pipe-sizing/dist/release-manifest.json` |
| Signing | alias `upload`; SHA-256 `4B:34:F1:B1:…:D7:65`; `apkVerifiedByApksigner: true` | `apps/2026-09-25-pipe-sizing/dist/release-manifest.json` |
| Build | `2026-09-25T20:27:19.431Z`, commit `1b9836cb7d17082bf99b187fea48f22cad9800ab` | `apps/2026-09-25-pipe-sizing/dist/release-manifest.json` |
| Recorded idea | "US fuel gas pipe sizing workflow: sum loads and developed lengths, convert units both ways, keep every input editable, and record which code edition and table each reading came from. Ships no code tables; results verify against your own book." | `registry/apps.json` |
| Revenue capability | `{"schemaVersion": 1, "enabled": []}` — no billing, no ads | `apps/2026-09-25-pipe-sizing/capabilities.json` |
| Registry status | `scaffolded` | `registry/apps.json` |

**What the app does**, in its own copy: it is "a paid-once, offline calculation aid
for organizing gas pipe sizing inputs and arithmetic" — it "sums loads and lengths,
converts imperial and metric units both ways, compares required load with the value
you entered, and records your edition, table, and row citation"
(`apps/2026-09-25-pipe-sizing/store/listing.json`).

**What the app explicitly does not do**, in the same file:

- It "provides no code-table data and does not determine compliance, certify work,
  or endorse a source."
- It "asserts no result when a required input, citation, range, or consistency check
  is missing."
- "No account, ads, subscription, in-app purchase, or network connection is
  required."
- `permissions` is `[]`, `dataSafety.collectsData` is `false`, and
  `ads.containsAds` is `false`.

The recorded span from slot creation (`2026-09-25T15:37:50.949Z` in
`registry/apps.json`) to the last recorded build (`2026-09-25T20:27:19.431Z` in
`dist/release-manifest.json`) is roughly four hours and forty-nine minutes. That
figure is a lower bound: it excludes any evaluation, capture, or repair work that
left no timestamp in these two files.

---

## 2. The commercial failure, stated plainly

The failure is a chain. Each link is a decision or omission that a file records.

1. **An illustrative example was mistaken for a strategic niche.** The owner's
   stated intent, recorded 2026-09-26, is that a calculator was given as an
   *example of the shape* of a small offline tool, not as the niche to build. The
   factory treated that example as a topic and produced a calculator. This intent is
   the owner's statement to this post-mortem and is **UNVERIFIED against a
   repository file** — no committed artifact records it. What the repository does
   record is that a pipe-sizing calculator became the slot: the slot's own
   `AGENTS.md` states the idea as "Offline gas pipe sizing arithmetic using
   user-entered loads, lengths, allowances, units, and source citations; paid once."

2. **The factory's own research called the pocket a graveyard, and the decision was
   made anyway.** `docs/research/2026-09-24-market-decision.md` §2.2 records the
   counter-evidence under the heading "The counter-evidence, which is real and must
   not be buried":

   | App | Installs | Reviews |
   |---|---:|---:|
   | `com.plumbtools.gas_pipe_sizing` — Gas Pipe Sizing - NFPA 54 | 100+ | **0** |
   | `com.plumbtools.water_pipe_sizing` — Water Pipe Sizing IPC 2024 | — | unrated |
   | `com.hvactools.pipe_sizing_calculator` | 100+ | **0** |
   | `com.pipefitpro.app` — PipeFit Pro: Offset Calculator | 1K+ | **0** |
   | `com.AK2DSTUDIOS.gaspipesizingtool` — Pipe Sizing Tool | 10+ | **0** |

   The same section states the reading plainly: "Publishing here is easy; winning is
   not. `com.plumbtools.*` is one developer already shipping the exact
   multi-calculator portfolio this document first proposed — gas sizing to NFPA 54,
   water sizing to IPC 2024, offsets — and all three are unrated." The document also
   records its own relevance guard returning `on-topic` on that same shelf.

3. **The wedge the research defined was abandoned in the build.** The same document
   states the wedge in bold: "**The wedge is not 'another pipe calculator'.** It is:
   *the sizing tool that cites the edition of the standard it used, shows the
   correct multipliers rather than an opined one, leaves every input editable, and
   is bought once instead of rented.*" The built app keeps only two of those clauses
   (editable inputs, citations of the user's own book) and drops the two that
   addressed the measured complaints: it ships "no code-table data" and therefore
   cannot show the correct multipliers or return a standards-cited result, and it is
   not "bought once instead of rented" because it is a *paid app with no purchase
   tier* — a different product from the unlock the research proposed. The one clause
   that answered the 4.8★ leader's pricing revolt is the clause the build removed.

4. **Zero revenue mechanism is enabled.** `capabilities.json` is
   `{"schemaVersion": 1, "enabled": []}`: no billing capability, no ads capability.
   The store copy confirms this as a product claim — "No account, ads,
   subscription, in-app purchase, or network connection is required." The app can be
   paid for once in the store, and nothing else. There is no second sale, no upsell,
   and no measurement surface.

5. **The planned unlock was never implemented.** `docs/research/2026-09-24-market-decision.md`
   §2 records the monetization plan as "purchase-led, per `docs/monetization-strategy.md`
   §3.6 — a one-time unlock for the full code table plus the sizing workflows, ads
   absent or secondary." No billing capability is enabled, no product id exists in a
   `BillingCatalog`, and the listing asserts the opposite of the plan. This is the
   plan-versus-artifact gap: the research prescribed a paid unlock and the artifact
   shipped a free-standing paid app whose own copy denies the unlock exists.

6. **The monetization requirements already existed in prose and were unenforced.**
   `docs/monetization-strategy.md` §3.6 names the requirement — "**Write the
   archetype sentence into the slot plan.** Same discipline as §1.2's distinctness
   sentence: if the archetype cannot be stated in one sentence with a named product
   list, the monetization is not designed yet." Section 5 item 5 names the second:
   "**Define kill criteria before shipping**, e.g. 'if after 30 days past production
   DAU < N and revenue < $X, the slot is archived.'" Neither requirement is checked
   by any artifact the factory ran on this slot. No archetype sentence with a named
   product list appears in the slot's `AGENTS.md` or `README.md`, and no kill
   criteria appear anywhere in the slot. Both rules were recorded on 2026-09-23 and
   neither bound the pipeline that produced this slot two days later.

7. **The store pack still carries placeholder contacts.** `store/listing.json` has
   `"contactEmail": "support@example.com"` and
   `"privacyPolicyUrl": "https://example.com/privacy"`. A pack in this state cannot
   pass a human Play submission, which is consistent with the slot never having been
   submitted.

8. **A signing key was generated by a script with a machine-chosen password.**
   `scripts/new-slot.mjs` ends by printing the operator's next commands, including
   `node scripts/sign-release.mjs --slot ${slug} --generate`. The `--generate` path
   in `scripts/sign-release.mjs` mints the password itself:
   `const password = randomBytes(24).toString('base64url').slice(0, 32)`, writes it
   to `keystore.properties` (gitignored, at `0o600`), and creates the keystore at
   `~/.playkiln/keystores/<applicationId>.jks` under alias `upload`. Whether the
   owner explicitly authorized that key generation is the orchestrator's statement,
   not a repository record; what the repository shows is that the tool path invites
   it and the manifest records the result. The key is outside the repository and the
   properties file is ignored, so no secret was committed — but a signing identity
   now exists for a slot whose commercial premise has failed.

**Net characterization.** A factory that had already measured the category as a
graveyard, had already written the rule that monetization must be designed before
UI, and had already defined the exact wedge that answered real user complaints,
produced an app with no code tables, no purchase tier, no revenue capability, and
placeholder contacts — and every automated gate passed it.

---

## 3. Root cause: a pipeline with no money stage

`AGENTS.md` defines the factory pipeline as eight stages, from the idea through
plan approval, scaffolding, implementation, store pack, release preparation,
verification, evaluation, and finally the human Play Console steps. Read the gates
at each stage and one property is uniform: **every gate measures form.** They check
that an APK and AAB are signed and hashed, that the release manifest agrees with
the artifacts on disk, that store assets exist at exact dimensions and counts, that
listing text is within character limits, that permissions are minimized, that a
privacy policy and data-safety declaration exist, that provenance binds the images
to the build. Not one stage asks whether the app can earn money, whether its
monetization matches the plan that justified the slot, or whether the plan was
followed at all.

That is why `{"schemaVersion": 1, "enabled": []}` could sit inside a slot that
reported a green verification: nothing in eight stages reads `capabilities.json` for
the presence of a revenue stream. It is why a store pack can state "no ... in-app
purchase" while the research that selected the slot promised "a one-time unlock for
the full code table plus the sizing workflows": no stage compares the artifact
against the plan it was built from. Form gates cannot detect an economic omission,
because an absent monetization is a perfectly well-formed artifact.

This is not a new defect class for this repository. It is the same one recorded
repeatedly in `harness/lessons.md`:

- "2026-09-25 — the checksum that gated the capture existed only in the lesson that
  described it": "a claim that a gate exists must name the gate's file and be
  reproducible by running something ... because prose is the one artifact in this
  repository that no gate reads."
- "2026-09-25 — two checks that looked like verification and could not fail": "both
  are the same defect as the fabricated checksum two entries above —
  **verification-shaped text that cannot be contradicted**."

The defect class is **a claim that no artifact can contradict**. In the earlier
instances the claim was a fabricated checksum or a static UI caption; here the claim
is the *plan itself*. The research document asserted a wedge, an archetype, and a
one-time unlock. All three were prose. Nothing in the pipeline could contradict
them, so none of them had to be true for the slot to pass. A green slot is exactly
what you get when a claim is unverifiable: the gates confirm the app is well-formed,
and the well-formed app quietly contradicts the plan that earned it a slot.

---

## 4. What actually has residual value

Honest accounting. The slot's *topic* is sunk; some of the *machinery* is not.

**Reusable engineering, independent of app category.** These are harness assets that
any slot — calculator, inspection tool, log, or otherwise — would exercise
unchanged. Their presence was read; their passing was not re-run for this
post-mortem, so what is claimed is that they exist and are generic, not that they
are proven green today.

- **The verification gates.** `scripts/verify-slot.mjs`, `scripts/selftest.mjs`,
  `scripts/store-kit.py`, and the offline suites (`scripts/test-capture-publish.mjs`,
  `scripts/test-apk-provenance.mjs`, `scripts/test-play-reviews.mjs`) encode the
  factory's build, store-pack, and provenance contracts. They are category-neutral:
  they check artifact form, not what the app is about.
- **The provenance chain.** `scripts/lib/apk-provenance.mjs` and
  `scripts/media-capture.mjs` bind committed store images to a specific signed build
  and write `store/provenance.json`; `store-kit.py` validates that record. The
  absence of this chain in any other project is a real gap only the factory has
  closed here.
- **The capture pipeline.** `scripts/media-capture.mjs` plus the slot's
  `store/journey.json` and `scripts/store-studio.py` compose real device captures
  into a store asset set with per-shot assertions. The slot's `README.md` records
  that the phone screenshots are real captures and warns that the tablet images are
  framed phone captures, not tablet captures — an accurate, self-flagged limitation
  that travels with the pack.
- **The signing and release path.** `scripts/sign-release.mjs` and
  `scripts/prepare-release.mjs` plus `scripts/lib/java.mjs` and
  `scripts/lib/gradle.mjs` produce a signed APK, a signed AAB, and a release
  manifest, and resolve the JDK consistently. These are generic to any Android slot.
- **The market sensors.** `scripts/lib/play-reviews.mjs`, `scripts/scan-complaints.mjs`,
  `scripts/scan-market.mjs`, and `scripts/scan-titles.mjs` were built to answer
  "why are incumbents weak" rather than only "how rated are they", and they produced
  the evidence that should have killed or reshaped this slot. The sensors worked;
  the decision process did not enforce their output.

**Sunk or wasted.**

- **The niche choice.** The selection rests on a pocket its own evidence called a
  graveyard, and on an unproven specific job the research labelled "proven category,
  unproven specific job, unowned specific job."
- **The listing copy and the app's own claims.** They describe an app that ships no
  code tables, which is precisely the differentiator the research identified and the
  build discarded.
- **The store assets.** Icon, feature graphic, phone and tablet sets are composed
  correctly but advertise a product whose premise has failed. The tablet set is
  additionally not genuine large-screen capture, and the slot flags that itself.
- **The slot's domain code and its tests.** They implement arithmetic the research
  warned was not the product ("Correctness is the product"), for a job nobody has
  shown they will buy.
- **The 4 hours 49 minutes** between scaffolding and the recorded build, as a lower
  bound, produced a green slot and no revenue-relevant asset. It was not an
  investment in the factory's goal. The reusable machinery listed above predates
  this slot in large part; what this slot *added* is the negative result.

---

## 5. Decisions this failure forces

Each decision below converts a previously-prose requirement into something an
artifact can contradict. The enforcement artifacts named in this table are the
intended homes for those checks. **None of them existed in the repository when this
post-mortem was written** — `.cursor/rules/ideation-gate.mdc`, `docs/slot-economics.md`,
`scripts/check-economics.mjs`, and `scripts/lib/slot-economics.mjs` were each
searched for by path and not found. They are being created alongside this
post-mortem, and until they exist this table is a plan, not evidence.

| Decision | Value | Where it is now enforced |
|---|---|---|
| An idea must name a revenue mechanism before it earns a slot | A slot may not be scaffolded without a declared archetype, a named product list, and a price | `.cursor/rules/ideation-gate.mdc` (created alongside this post-mortem) |
| The example-to-topic substitution is a named failure | An illustrative shape from the owner is not a niche; the niche must be chosen on the market evidence in `docs/niche-selection.md` | `.cursor/rules/ideation-gate.mdc` (created alongside this post-mortem) |
| The slot plan must carry its archetype sentence (§3.6) as reviewable text | `monetization-strategy.md` §3.6 moves from advice to a required slot artifact | `docs/slot-economics.md` (created alongside this post-mortem) |
| Kill criteria must exist before release (§5 item 5) | Numeric archive criteria are declared per slot, not described as a future practice | `docs/slot-economics.md` (created alongside this post-mortem) |
| Plan versus artifact is checkable | The enabled capabilities and the listing copy are compared against the recorded plan, so "no in-app purchase" cannot ship beside "one-time unlock" | `scripts/check-economics.mjs` (created alongside this post-mortem) |
| The economics check is a real sensor | It must fail on this slot's actual state — `{"schemaVersion": 1, "enabled": []}` with no declared unlock — and on a plan that promises an unlock the artifact does not enable | `scripts/lib/slot-economics.mjs`, `scripts/check-economics.mjs` (created alongside this post-mortem) |
| Placeholder contacts are a gate failure | `support@example.com` and `https://example.com/privacy` cannot pass a release pack | `scripts/check-economics.mjs` (created alongside this post-mortem) |
| A generated signing key needs explicit owner authorization | The keystore identity is created only on a recorded human decision, not as a routine step in a printed command list | decision recorded here; enforcement not yet located in the repository |

The last row is the weak one. The fix for a key minted by a script is a process
change in how `scripts/new-slot.mjs` prints its `Next:` hints and how
`scripts/sign-release.mjs --generate` is invoked, and no enforcement artifact for it
was found or is claimed here.

---

## 6. What remains unproven

- **No revenue data exists at all.** Not for this slot, not for any slot. There is
  no Play release for `com.gexetr.pipesizing`, and no report of installs, conversion,
  or earnings.
- **No slot has ever earned.** `registry/apps.json` contains three entries — `smoke`,
  `pipe-proof`, and `pipe-sizing` — and all three have `status: "scaffolded"`. The
  only measured thing the factory has produced across its lifetime is that it can
  scaffold and build a slot; it has never measured a sale.
- **Production access is unconfirmed.** `docs/monetization-strategy.md` §1.1 records
  that personal Play Console accounts created after 2023-11-13 must run a closed
  test with at least 12 testers for 14 days before production access. The same
  document and `docs/research/2026-09-24-market-decision.md` §6 both flag the
  question as open and note that the answer is inferred from the `com.gexetr`
  namespace rather than read from Play Console. Whether this factory can publish to
  production from day one is therefore UNVERIFIED.
- **That a green slot earns nothing-and-nothing is the only thing known.** A green
  slot proves the app is well-formed. Nothing links a green slot to a purchase,
  a retention curve, or a dollar. The failure documented here is the first evidence
  that a green slot can be economically worthless; it is not evidence about whether
  any *other* slot would earn.
- **That the new enforcement changes anything.** The decisions in §5 are plans.
  Until `scripts/check-economics.mjs` exists, fails on this slot's real state, and is
  confirmed to fail, the factory has not gained a money stage — it has gained a
  document saying it should have one, which is the exact defect class of §3.
