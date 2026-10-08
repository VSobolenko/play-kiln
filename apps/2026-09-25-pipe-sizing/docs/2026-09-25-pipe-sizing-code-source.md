# Pipe-sizing code source: pre-slot safety and licence memo

## What this establishes / what it does not

This establishes that a US fuel-gas sizing product needs a *versioned source
dataset plus a selected sizing method*, not merely arithmetic: the publicly
indexed 2024 IFGC material expressly provides both capacity tables and separate
low-/high-pressure equations. [IFGC 2024, Chapter 4](https://codes.iccsafe.org/content/IFGC2024P1/chapter-4-gas-piping-installations)
It also establishes a realistic independent test source: IFGC Appendix A says it
contains multiple worked sizing examples. [IFGC 2024, Appendix A](https://codes.iccsafe.org/content/IFGC2024V1.0/appendix-a-sizing-and-capacities-of-gas-piping-ifgs)

It does **not** establish permission to copy NFPA or ICC tables into an app,
the current NFPA 54 edition on 2026-09-25, an NFPA table transcription, or a
specific jurisdiction's adoption. Direct HTTP requests from this environment to
NFPA, ICC and Play all failed with status `000`; these are safety/licence gates,
not gaps to fill from memory.

## 1. What the rules consist of

**Both tables and equations.** The accessible IFGC Chapter 4 search record says
that its sizing provisions use Tables 402.4(1)–(37), and separately specifies a
low-pressure equation and a high-pressure equation. It says the table route
applies to piping/tubing other than noncorrugated stainless steel tubing and
the equations apply to smooth-wall pipe/tubing. [IFGC 2024, Chapter 4](https://codes.iccsafe.org/content/IFGC2024P1/chapter-4-gas-piping-installations)
Therefore the product data model must retain at least: code/edition, fuel,
material/system, nominal size and inside diameter, inlet-pressure band,
permitted pressure drop, gas specific gravity, developed/equivalent length,
load/capacity units, sizing method, and table/row provenance. This is an
implementation inference from the variables and table headings described by
that source, not a copied table. [IFGC 2024, Chapter 4](https://codes.iccsafe.org/content/IFGC2024P1/chapter-4-gas-piping-installations)

The minimum real-tool branches visible in IFGC are low-pressure and elevated-
pressure sizing, and tables/equations; the cited table example is Schedule 40
metallic pipe. [IFGC 2024, Chapter 4](https://codes.iccsafe.org/content/IFGC2024P1/chapter-4-gas-piping-installations)
**UNVERIFIED:** a complete, edition-specific NFPA 54 inventory of Schedule 40,
CSST, semirigid copper and PE tables. Settling it requires a human with lawful
access to the chosen NFPA 54 edition to inventory the actual table list and any
manufacturer-system instructions it incorporates.

## 2. Edition position and changes

The plausible production editions are 2018, 2021, and 2024: ANSI's product
records identify 2018 and 2021 as historical, and a publication index describes
2018 as superseded by 2024. [ANSI: 2018](https://webstore.ansi.org/standards/nfpa/nfpa542018)
[ANSI: 2021](https://webstore.ansi.org/standards/nfpa/nfpa542021)
[NBS publication index](https://staging.web.pubs-search.thenbs.cloud/documents/details?DocId=334755&Pub=NFPA)
NFPA committee material also calls 2027 a *proposed* edition and says it was to
be published later in 2026. [NFPA Standards Council material](https://content.nfpa.org/-/media/Project/Storefront/Catalog/Files/Codes-and-standards/standards-council/April-2026/SC_Agenda_04_2026.pdf)

**UNVERIFIED:** which edition is current today. The official NFPA standard page
could not be fetched. A human must open NFPA's official NFPA 54 page/LiNK on the
release date, record the displayed current edition and any TIAs, and freeze that
edition in the product release manifest.

The public 2024 product description lists changes to definitions, purge
requirements and annex material, but does not document a changed sizing output.
[NFPA 54 2024 product description](https://nfpawebstore.com/product/nfpa-54-ansi-z223-1-pdf/)
NFPA public-input material proposes adding intended-use lines to some sizing
tables, but a proposal is not proof of adopted values or a production result.
[NFPA 54-2024 public-input record](https://policycommons.net/artifacts/16984175/public-input-no/17869094/)
**UNVERIFIED:** every recent change that changes a sizing result. Settle by a
licensed side-by-side comparison of the selected edition against its predecessor,
with a reviewer signing an edition-diff artifact for every shipped branch.

## 3. Legal access and incorporation by reference

NFPA publicly represents that its codes and standards can be viewed online for
free, while a fire-service association describes that path as read-only, with no
download or print. [NFPA interactive page](https://files.nfpa.org/nfpaorg/NFPA/index.html)
[IAFC description](https://www.iafc.org/topics-and-tools/resources/resource/free-access-to-nfpa-codes-and-standards)
NFPA also sells LiNK access, including current and legacy editions. [NFPA LiNK](https://products.nfpa.org/view/126122027)

Free reading is not a reproduction licence. **UNVERIFIED:** the applicable NFPA
Free Access click-through terms and whether they authorize copying a capacity
table into a commercial app. The official terms page could not be fetched;
settle only with the written licence/permission from NFPA (and counsel's review),
not an engineer's interpretation of the reader.

Standards incorporated by reference can have access paths; NIST maintains a
federal guide that links both NFPA free access and ICC free resources.
[NIST: standards incorporated by reference](https://www.nist.gov/standardsgov/accessing-standards-incorporated-reference)
**UNVERIFIED:** a state-by-state proposition that an adopting authority publishes
the relevant adopted NFPA 54/IFGC text free for reuse. Adoption, amendments,
edition, and copyright licence vary; settle it for each target jurisdiction with
the authority's adopted-code page and explicit reuse terms. Public availability
would still not itself prove permission to redistribute table data.

## 4. ICC/IFGC alternative

IFGC has a publicly indexed, directly addressable Digital Codes page for Chapter
4 and Appendix A. Those pages show tables plus equations and Appendix A's
guidance/examples, so it is a viable *technical* alternative/additional source,
not evidence that NFPA is inherently the better anchor. [IFGC Chapter 4](https://codes.iccsafe.org/content/IFGC2024P1/chapter-4-gas-piping-installations)
[IFGC Appendix A](https://codes.iccsafe.org/content/IFGC2024V1.0/appendix-a-sizing-and-capacities-of-gas-piping-ifgs)
ICC sells IFGC and describes a paid Digital Codes Premium offering; that is
evidence of a commercial access product, not a table-republication licence.
[ICC IFGC store page](https://shop.iccsafe.org/international-codes/ifgc-references.html)

**UNVERIFIED:** whether ICC's currently displayed online reader is free for the
complete current IFGC, its click-through reuse rights/cost for app embedding,
whether its capacities are numerically identical to any NFPA 54 edition, and
which code is more commonly adopted nationally. Settle each through (1) ICC's
current licence quote/terms, (2) a licensed table-by-table comparison, and (3) a
jurisdiction/adoption dataset scoped to the launch states.

## 5. Defensible shipping options

| Option | Human must provide before coding | Correctness risk | Licence risk |
|---|---|---|---|
| Embed code tables | A written NFPA/ICC licence explicitly allowing app reproduction; chosen edition/TIAs; a reviewer-signed transcription and diff. | Low only after full row/column comparison and regression tests. | High until written permission; free viewing is not enough. |
| Embed public-domain/freely reusable data | Source owner, edition, licence text permitting commercial redistribution, and a mapping to the adopted code. | High if it is merely a look-alike chart rather than an authoritative adopted source. | Low only if the actual licence is verified. **UNVERIFIED:** a suitable source was not found. |
| Implement equations | Written right to implement/copy the equation or a public-domain derivation; selected inputs and rounding rules; expert validation. | High: an equation can produce a different answer if constants, applicability, equivalent length or rounding are wrong. | **UNVERIFIED:** equation copyright/licence status was not established. |
| User-entered lookup | A UI that records the user's code/edition/table/row and clearly says the app does not supply the capacity; an expert review of the workflow. | Lower data-transcription risk, but still material UX/unit/method-selection risk. | Lower because the app does not redistribute the table; **UNVERIFIED:** obtain counsel's confirmation. |
| Licensed manufacturer-system data | Manufacturer permission, installation instructions/version, product scope and reviewer sign-off. | Material if the user selects the wrong system or document version. | High until permission is written; manufacturer instructions may have their own terms. |

## 6. Who checks it; worked-example test source

A credible release evidence pack is: (1) the exact licensed/free-reuse source
and edition/TIA snapshot; (2) a machine-readable dataset whose every cell maps
to source page/table/row/column; (3) independent double entry or automated
comparison; (4) sign-off by a qualified US fuel-gas engineer/code professional;
(5) regression tests for every supported method/material/pressure branch; and
(6) the launch-jurisdiction/adoption disclaimer reviewed by counsel. The
requirement for edition-specific evidence follows because official sources show
separate editions and revision processes. [ANSI editions](https://webstore.ansi.org/standards/nfpa/nfpa542018)
[NFPA proposed-2027 material](https://docinfofiles.nfpa.org/files/AboutTheCodes/54/FD_D26_1_SC26_4_9_d_TIA_1883_NFPA_54.pdf)

The best published lead is IFGC Appendix A: its official description says it
contains multiple examples of applying Chapter 4 tables and sizing methods.
[IFGC preface](https://codes.iccsafe.org/content/IFGC2024P1/preface)
**UNVERIFIED:** one worked example's full known inputs and answer, suitable for a
test assertion; this environment could not fetch/read the complete Appendix A
page. A human with lawful access must select an example, capture its citation and
inputs/output in a test-fixture approval record, and have the independent
reviewer recalculate it before it becomes a test.

## 7. Play prior art (lead only)

The required Play pages could not be fetched (`000`), and the repository's
previous market memo only records titles/market signals, not code-edition claims.
For example, it labels `com.plumbtools.gas_pipe_sizing` “Gas Pipe Sizing - NFPA
54”; that is a title lead, not confirmation of an edition or licence.
[prior market decision](./2026-09-24-market-decision.md)

| Package | Store-page code/edition claim verified now? | Conclusion |
|---|---|---|
| `com.calculated.josephine` | **UNVERIFIED** — Play fetch failed. | Do not infer a code source or licence. |
| `ru.sotnikov.offsetpipe` | **UNVERIFIED** — Play fetch failed. | Do not infer a code source or licence. |
| `com.pipeflowcalculations.android` | **UNVERIFIED** — Play fetch failed. | Do not infer a code source or licence. |
| `com.plumbtools.gas_pipe_sizing` | NFPA 54 appears in the recorded title, but edition is **UNVERIFIED**. | A store title cannot establish licensing. |

## Decisions this forces on the human

| Decision | Options | Consequence |
|---|---|---|
| Launch authority/edition | Freeze one NFPA 54 edition plus TIAs; use one IFGC edition; or scope launch to a named jurisdiction's adopted code. | The app must show this choice on every result and reject unsupported combinations. No “US code” generalization. |
| Data right | Obtain written NFPA licence; obtain written ICC licence; find verified reusable source; or ship user-entered lookup only. | No embedded numbers until a right is documented. User-entered mode has less copying risk but is a different product. |
| Calculation route | Table lookup; equation; licensed manufacturer system; or user-entered lookup. | Each route changes the data required, test suite, supported materials and error messaging. |
| First-release surface | One material/pressure/method branch; multiple branches; or defer gas sizing. | More branches multiply transcription, method-selection and review scope. |
| Independent checker | Qualified fuel-gas engineer/code professional; paid code consultant; or no launch. | A developer self-check is not credible safety evidence; no checker means do not claim standards-cited sizing. |
| Test oracle | Reviewer-approved worked example(s); a licensed table-cell comparison fixture; or no release. | Tests must assert a source-derived answer, not outputs generated by the same implementation. |
| Jurisdiction statement | Named launch states/AHJs; nationwide with exclusions; or generic educational tool. | Adoption/amendments must be researched for the claim made; generic language does not cure a false standards claim. |

## Fetch log (this run)

`curl -L` observed the following final status for every requested host: all were
connection failures (`000`), not successful verification. The URLs were:

| URL | HTTP status observed |
|---|---:|
| `https://www.nfpa.org/codes-and-standards/nfpa-54-standard-development/54` | `000` |
| `https://www.nfpa.org/for-professionals/codes-and-standards/nfpa-liNK` | `000` |
| `https://www.nfpa.org/legal/copyright-permissions` | `000` |
| `https://codes.iccsafe.org/content/IFGC2024P1/chapter-4-gas-piping-installations` | `000` |
| `https://codes.iccsafe.org/content/IFGC2024V1.0/appendix-a-sizing-and-capacities-of-gas-piping-ifgs` | `000` |
| `https://codes.iccsafe.org/content/IFGC2024P1/preface` | `000` |
| `https://shop.iccsafe.org/media/wysiwyg/material/3600S24-TOC.pdf` | `000` |
| `https://play.google.com/store/apps/details?id=com.calculated.josephine` | `000` |
| `https://play.google.com/store/apps/details?id=ru.sotnikov.offsetpipe` | `000` |
| `https://play.google.com/store/apps/details?id=com.pipeflowcalculations.android` | `000` |
| `https://play.google.com/store/apps/details?id=com.plumbtools.gas_pipe_sizing` | `000` |

The memo uses direct URLs for traceability and marks claims that need direct
official-text confirmation as `UNVERIFIED`; it deliberately contains no capacity
value, clause-number assertion, or copied table.
