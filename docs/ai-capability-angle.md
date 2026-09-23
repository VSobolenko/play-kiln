# AI-capability angle for PlayKiln

## 1. What is actually scarce

An LLM API key, including the owner's DeepSeek key, is **not** a durable advantage.
It is a purchasable input. Thousands of developers can buy access to an inference
provider, switch providers, and put a chat screen in an Android app. A nominal
input-cost markup is a pricing hypothesis, not a moat.

The existing `Fulfillment.SERVER_COST` billing seam is also **not yet an advantage**.
It is useful factory infrastructure: its code requires a `PurchaseAuthority` before
it grants a server-cost pack, and it distinguishes an authoritative balance from a
device cache. But the repository deliberately ships no authority implementation,
so no real slot has yet demonstrated purchase verification, credit settlement, or
cost control in production.

Things that might become scarce, but are not scarce merely because we say they are:

1. **Access to a specific workflow and its feedback loop.** A repeatable job flow,
   domain artefacts, and a way to measure whether its output is accepted can make a
   model useful where a general chatbot is not. This is only scarce when PlayKiln
   has obtained it lawfully and can keep improving it.
2. **Trusted domain correctness.** Curated source material, rule sets, expert review,
   and auditable outputs can be difficult to reproduce. In regulated or safety
   relevant work they are mandatory quality work, not marketing decoration.
3. **Distribution into a proven, reachable pocket.** The niche screen requires
   demand evidence and a weak incumbent pocket. It is an opportunity only after a
   concrete query and listing read prove both; it cannot be assumed from an AI
   feature.
4. **An economical, abuse-resistant service operation.** A server that verifies Play
   purchases, makes spend decisions atomically, applies rate/abuse controls, and
   observes cost per completed job can be operationally valuable. It is still
   replicable engineering, not an exclusive asset.

**Honest current conclusion:** no genuinely scarce AI advantage has been evidenced
for PlayKiln yet. The key is commodity access; the tested billing primitives and the
factory's niche discipline are capabilities to validate, not proven moats. A future
slot earns a stronger claim only with evidence that a particular workflow is used,
valued, and harder to replace than generic prompting.

## 2. Google Play policy constraints on AI apps

### Verified from official Play Help

1. AI-generated-content apps must comply with existing Play policies, prevent
   restricted output (the policy names child exploitation/abuse and deceptive
   behaviour), provide an **in-app** way to report or flag offensive generated
   content without leaving the app, and use reports to inform filtering/moderation.
   This applies to content generated from user prompts, including text chat and
   image/video generation. [AI-generated content policy](https://support.google.com/googleplay/android-developer/answer/13985936?hl=en)
2. Play's spam policy prohibits repetitive or low-quality apps. In particular, it
   prohibits multiple apps with highly similar functionality, content, and user
   experience, and says apps should provide value through unique content or
   services. [Spam policy](https://support.google.com/googleplay/android-developer/answer/9899034?hl=en)
   This is directly relevant to a factory: changing the prompt, theme, or niche
   label around the same AI screen is not evidence of a distinct product.
3. If a slot sends a user's prompt, uploaded material, or other personal/sensitive
   user data to a third-party inference provider, the developer remains responsible
   for that handling. Play says developers should share user data with a third party
   only when needed; data collected by an SDK is treated as collected directly by
   the app. [SDK responsibility guidance](https://support.google.com/googleplay/android-developer/answer/13326895?hl=en)
   The Data safety form must therefore be completed from the actual data flow,
   including inference-provider handling, not from the Android client alone.
4. Play's Data safety guidance says to answer that personal information is shared
   when user-provided personal information is shared with a third party other than
   the developer/publisher. Whether a particular prompt is personal information is
   a factual classification exercise; do not prefill "no data shared" merely because
   the app does not retain the prompt. [Shares Personal Information](https://support.google.com/googleplay/android-developer/answer/6161127?hl=en)
5. Separately, store-listing or promotional assets that are AI-generated or edited
   may require an asset-level self-declaration and can receive an AI label on Play
   surfaces. This is a store-asset rule, not a substitute for in-app reporting.
   [Declaring AI-generated content in Play Console](https://support.google.com/googleplay/android-developer/answer/17262077?hl=en)

### Clarification and unverified boundary

`AGENTS.md` correctly forbids a "thin AI chat wrapper" as a factory rule. I did
**not** verify an official Play page that uses that exact phrase or categorically
bans every API-wrapper app. The verified policy basis is the broader requirement for
unique value, non-repetitive content, and adequate functionality. Treat a generic
chat wrapper as a high rejection and low-demand risk, not as a policy quotation.

I also did **not** verify DeepSeek's current processing, retention, location,
subprocessor, or training terms. Those facts are needed before any slot can make a
truthful Data safety declaration, privacy-policy statement, or prompt-retention
claim. The exact Data safety answers depend on the selected provider, server design,
SDKs, account model, and every data type actually transmitted.

## 3. Cost arithmetic for selling inference

Use a pack with a named, bounded unit such as `jobs`, `credits`, or `tokens`; do not
sell an undefined promise of "AI access." The unit must map to a server-enforced
spend rule and an observable provider cost.

Let the following all be **assumptions to replace with evidence for a real slot**:

- `P` = customer price of one pack in the purchaser's currency, before sales taxes
  or refunds as handled by the store.
- `g` = applicable Google Play service-fee fraction for this transaction.
- `r` = expected refund/chargeback/fraud loss fraction of gross pack revenue.
- `F` = fixed per-pack costs outside the model (payment-adjacent, support, storage,
  server allocation, and any tax cost not already excluded from `P`).
- `C` = expected inference-provider cost to fulfil all credits in this pack.
- `m` = target contribution-margin fraction of gross price, where `0 < m < 1`.

Then expected contribution from the pack is:

```text
contribution = P * (1 - g - r) - F - C
```

For a target contribution margin `m` of gross price, require:

```text
P * (1 - g - r) - F - C >= m * P
P >= (F + C) / (1 - g - r - m)
```

The denominator must be positive. If `1 - g - r - m <= 0`, the requested margin is
mathematically impossible under those assumptions. Round the resulting price upward
only after checking available Play price points and local tax/fee treatment; neither
is established in this document.

For a pack granting `N` credits, if one credit has expected cost `c`, then `C = N *
c`. If credits represent heterogeneous work, replace `c` with the measured expected
mix, not a best-case request. Add a reserve for retries, long prompts, output size,
and provider failures only after measuring them. No DeepSeek price, usage rate,
Google fee, refund rate, fraud rate, or conversion rate is asserted here.

The owner's example, "cost us $10, sell for $20," is only a gross-markup example.
It omits `g`, `r`, `F`, and the target margin; it cannot establish profit. It is also
unsafe when the client is trusted to grant or spend credit. A modified client can
fabricate a purchase callback/token or edit a local balance, turning the actual
cost `C` into unbounded loss. For `SERVER_COST`, the server must verify the Play
purchase before crediting, reject duplicate grants, and atomically authorize/debit
each spend before calling inference. That is exactly the boundary represented by
`PurchaseAuthority`; its production backend is still unimplemented.

## 4. Where AI is the engine, not the product

The candidate is a tool when a user starts with a specific work artefact, follows a
structured job flow, receives a constrained deliverable, can inspect/correct it,
and completes a task that remains useful without continuing a conversation. AI can
perform the tedious transformation inside that flow; it should not be the whole
screen or the whole value proposition.

Potential shapes to investigate (not recommendations until the niche screen is run):

1. **Trade-job intake to a reviewable work pack.** A specific profession's notes,
   photos, measurements, and codes become a structured checklist, material list,
   client-facing draft, or exception list. The defensible work is domain constraints,
   source citations, and review/approval--not free-form answers.
2. **Caregiver or support-plan preparation.** A carer enters routines and constraints
   and gets editable, printable schedules or handover materials. This has heightened
   privacy, target-audience, and correctness risk; it must not make clinical claims
   without appropriate evidence.
3. **Document-to-workflow conversion for a narrow occupation.** A recurring form,
   inspection record, job log, or local procedure becomes structured fields, a
   checklist, and a traceable export. The app's value is completing and retaining
   the workflow; the model is one extraction/drafting component.
4. **Narrow language transformation with a final-use format.** For example, turning
   a specialist's rough note into an editable client explanation plus required
   follow-up questions. This is viable only if the exact occupation and demand pass
   the screen; generic rewriting is not a niche.

These shapes must still pass `docs/niche-selection.md` before a slot exists:

```text
Proven demand: rated incumbents and an opened listing with meaningful evidence.
Reachable pocket: weak mid-tier ratings or an unserved sub-workflow.
Distinctness: one sentence naming the materially different job/result.
Fit: a viable monetization and build/compliance shape.
```

Do not infer demand from a broad prompt such as "AI [profession]" or from low
competitor count. The factory's measured warning is that mostly unrated competitors
are a dead zone, not a vacancy. A proven parent category plus a weak, specific
workflow is the starting hypothesis to test.

Dead-on-arrival shapes are generic chat, generic summarisation, "ask AI anything,"
and a portfolio of prompt variations with the same interaction and output. They
fail the factory's distinctness standard, give users no reason to choose this app
over a general model, and are exposed to Play's repetitive-content/limited-value
assessment. This is a product and distribution conclusion; no revenue or rejection
rate is claimed.

## 5. Risks and kill criteria

Define the measurement window and thresholds in a slot plan before spending on
acquisition or inference. The following are observable retirement signals; numerical
thresholds remain **unverified and slot-specific** until baseline data exists.

| Risk | Observable signal | Kill or repair decision |
| --- | --- | --- |
| No demand | Niche screen cannot show rated, proven demand and a reachable weak pocket. | Do not create the slot. |
| Thin or repetitive value | The distinctness sentence reduces to "the same AI, for [label]"; a reviewer cannot demonstrate a task beyond prompting. | Stop before submission; redesign around a completed workflow. |
| Unit economics fail | Measured fulfilled-pack cost plus verified fees, refunds, fraud, and operating cost exceeds the planned price/margin. | Disable or reprice the pack; retire if a safe price destroys demand. |
| Fraud/abuse | Server logs show duplicate grants, unauthorized spend attempts, or cost incurred without an authorized debit. | Stop paid fulfilment until authoritative verification, idempotency, and spend control work. |
| Unsafe output | In-app reports, evaluation cases, or support tickets show restricted, materially harmful, or systematically incorrect output. | Disable the affected capability; only relaunch with tested mitigations. |
| Privacy/data-safety mismatch | Actual inference requests or SDK behavior cannot be reconciled with the privacy policy and Data safety answers. | Do not ship/update until declarations and handling are corrected. |
| No retained tool value | Users do not complete the defined workflow or do not return/use paid units after a predeclared observation period. | Retire rather than adding generic features or more prompt templates. |

Every production AI slot also needs: an in-app generated-content report/flag path;
logging that measures request class, authorized debit, provider cost, completion, and
report outcome without retaining more user content than necessary; a documented
provider/data-flow inventory; and a human review of the exact store declarations.
Those are release gates, not optional polish.
