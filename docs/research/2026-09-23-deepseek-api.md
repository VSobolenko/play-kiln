# DeepSeek API — verified facts for selling inference as a product

**Read on:** 2026-09-23 (session date; operator timezone UTC+3). Every price, model
identifier, and limit below was read from the URL cited next to it on that date.
**Nothing here is remembered or inferred from training data.**

**Prices and model identifiers change.** The DeepSeek pricing page itself says
"Product prices may vary and DeepSeek reserves the right to adjust them" and asks
readers to check the page regularly. Re-read the two pricing URLs in §7 before any
pricing decision. Cite a date with every number you carry into a plan.

**Governing rule for this document:** if a fact could not be read from a source it
is marked **UNVERIFIED** in §8, with a statement of what could not be confirmed. No
gap is filled with a plausible number. This follows
`.cursor/rules/verification-discipline.mdc`.

Read together with [`../monetization-strategy.md`](../monetization-strategy.md) —
§3.2 (consumable packs), §3.7 (client-reported purchase tokens are
attacker-controlled), §5.7 (a `SERVER_COST` slot is a server project).

---

## 0. Verdict

1. **Two models are callable:** `deepseek-flash` and `deepseek-v4-pro`. The legacy
   names `deepseek-v4-flash` and `deepseek-v4-flash-vision-exp` still resolve, but
   the old models are retired and those names are billed at the Flash price.
   `deepseek-chat` and `deepseek-reasoner` are **dead** — retired 2026-07-24. Any
   plan or third-party Kotlin SDK that still uses them is stale.
2. **The API is OpenAI-compatible REST over HTTPS.** There is no official Android or
   Kotlin SDK. Any OpenAI-compatible HTTP client works.
3. **Shipping the key in the APK is forbidden by the provider's own terms**, not
   merely unwise. Terms of Service §2.2 says do not "expose it in browser or other
   client-side code". Selling inference therefore **requires a proxy server**. This
   is a hard requirement, not a hardening recommendation.
4. **Concurrency, not requests-per-minute, is the published limit:** 2500 concurrent
   for `deepseek-flash`, 500 for `deepseek-v4-pro`, per account, shared across all
   API keys. Exceeding it returns HTTP 429.
5. **The owner's "$10 cost → $20 sell" framing is off by orders of magnitude at the
   token level.** A blended `deepseek-v4-pro` token at peak costs about
   **$0.0000021**. $10 of provider spend buys roughly **4.7 million** blended tokens
   at that rate. The sellable unit must be an **app credit** (a pack), never one
   DeepSeek token.
6. **A 2× markup is not a business.** Under the assumptions in §6 (20 % unverified
   credit, 15 % Play fee, 2 % refunds, 2 % FX), 2× cost leaves a **~25 % margin**;
   pure break-even needs **1.53× cost**, and a 60 % margin target needs
   **≈ 3.83× cost**.
7. **Server-side verification of the purchase token is worth real money** — under the
   same assumptions it moves the required multiple from ≈ 3.83× to ≈ 3.09×, i.e. it
   pays for itself (§6.4).

---

## 1. Models and the identifiers actually passed to the API

Source: <https://api-docs.deepseek.com/quick_start/pricing> and
<https://api-docs.deepseek.com/api/create-chat-completion> — both read 2026-09-23.

| API `model` value | Underlying model | Vision | Concurrency limit |
|---|---|---|---|
| `deepseek-flash` | DeepSeek-V4.1-Flash | **Yes** | 2500 |
| `deepseek-v4-pro` | DeepSeek-V4-Pro-0813 | Not supported | 500 |
| `deepseek-v4-flash` (legacy name) | retired; requests served by V4.1-Flash, billed at Flash price | (as Flash) | (as Flash) |
| `deepseek-v4-flash-vision-exp` (legacy name) | retired; served by V4.1-Flash, billed at Flash price | (as Flash) | (as Flash) |

`model` is documented as a **required** body field with
`Possible values: [deepseek-flash, deepseek-v4-pro]`
(<https://api-docs.deepseek.com/api/create-chat-completion>, read 2026-09-23).

Both models, per the pricing page (read 2026-09-23):

- context length **1M** tokens;
- maximum output **384K** tokens;
- JSON output, tool calls, Responses API, Anthropic API, chat prefix completion
  (beta) supported by both;
- FIM completion supported in non-thinking mode only;
- **thinking mode is the default** (`thinking.type` default value `enabled`).

**Retired, do not use** — from the change log entry dated 2026-04-24
(<https://api-docs.deepseek.com/updates>, read 2026-09-23):

> "The two legacy API model names, `deepseek-chat` and `deepseek-reasoner`, will be
> discontinued in three months (2026-07-24)."

That date has passed. Third-party Kotlin SDKs found in the wild still default to
`deepseek-chat` / `deepseek-coder` (§5.2) — a slot that adopts one unmodified will
call a dead model name.

---

## 2. Pricing

Source: <https://api-docs.deepseek.com/quick_start/pricing> — **read 2026-09-23**.
Currency: **USD**, per **1,000,000 tokens**. Off-peak rates are exactly half of peak.

### 2.1 Published table (USD per 1M tokens)

| | `deepseek-flash` off-peak | `deepseek-flash` peak | `deepseek-v4-pro` off-peak | `deepseek-v4-pro` peak |
|---|---:|---:|---:|---:|
| Input, **cache hit** | $0.003 | $0.006 | $0.022 | $0.044 |
| Input, **cache miss** | $0.15 | $0.30 | $0.66 | $1.32 |
| Output | $0.60 | $1.20 | $1.98 | $3.96 |

The cache-hit / cache-miss distinction **does exist** and is large: cache-hit input
is **50× cheaper than cache-miss on Flash** ($0.003 vs $0.15 off-peak) and
**30× cheaper on V4-Pro** ($0.022 vs $0.66 off-peak).

### 2.2 Peak windows (official wording, with the operator's local time)

> "Peak hours are 01:00 - 04:00 and 06:00 - 10:00 UTC, Monday through Friday,
> excluding Chinese public holidays. All other hours are off-peak, including
> weekends and Chinese public holidays in full."
> — <https://api-docs.deepseek.com/quick_start/pricing>, read 2026-09-23

Converted for a UTC+3 operator: peak is **04:00–07:00** and **09:00–13:00** local,
Monday–Friday. Peak is 7 hours × 5 days = 35 of 168 hours/week = **20.8 % of the
week**.

Arithmetic consequence for budgeting: if traffic is uniform over the week, the
blended rate is `0.208×peak + 0.792×off-peak = 1.208 × off-peak`. **Budget at the
peak rate** as the conservative bound; do not assume off-peak.

### 2.3 What is billed

From the same pricing page: "The expense = number of tokens × price … We will bill
based on the total number of input and output tokens by the model." Fees are
deducted from topped-up balance or granted balance, granted balance first.

Two consequences that matter to a pack product:

- **Thinking tokens are a cost, and thinking is on by default.**
  `usage.completion_tokens_details.reasoning_tokens` is documented as a *breakdown of
  `completion_tokens`* ("Breakdown of tokens used in a completion",
  <https://api-docs.deepseek.com/api/create-chat-completion>, read 2026-09-23).
  Reasoning tokens are therefore part of the billed output token count. This is an
  inference from the two documented facts (pricing bills output tokens; reasoning
  tokens are a subset of completion tokens), not a sentence DeepSeek writes
  verbatim — treat it as a **high-confidence inference**, and confirm against a real
  `usage` block before it drives a price.
- **Default output budget is not small.** `max_tokens` defaults to **8K in
  non-thinking mode, 64K in thinking mode, 128K with `reasoning_effort: max`**
  (<https://api-docs.deepseek.com/api/create-chat-completion>, read 2026-09-23). A
  mobile slot that leaves thinking enabled inherits the 64K default. Setting
  `thinking: {type: "disabled"}` or a small `max_tokens` is a **cost control**, not
  a nicety.

### 2.4 Cache pricing mechanics

Source: <https://api-docs.deepseek.com/guides/kv_cache>, read 2026-09-23.

- Context caching is **enabled by default for all users**; no code change is needed.
- A hit requires **full** prefix-unit match: "A subsequent request can only hit the
  cache if it fully matches a cache prefix unit." A request that shares only part of
  a prefix misses, and the shared prefix is persisted for *later* requests.
- Cache is **best-effort**: "does not guarantee a 100 % cache hit rate"; cache
  construction takes seconds; entries clear "usually within a few hours to a few
  days."
- Per-request audit fields: `prompt_cache_hit_tokens` and `prompt_cache_miss_tokens`
  (also `prompt_tokens_details.cached_tokens`).

Design consequence: a **fixed system prompt placed first**, with user content last,
is the cheapest ordering and the one the cache is built around. Any per-request
prefix (a timestamp, a random nonce, a user id at the top) destroys the hit rate and
multiplies input cost by up to 50×.

### 2.5 Pricing facts NOT confirmed

- **Tax treatment of the listed prices is not stated** on the pricing page.
  **UNVERIFIED** — whether $0.15/1M is tax-inclusive is not established.
- **No minimum top-up amount is published on the pages read.** Third-party pages
  claim $2.00, but that is not an official page and is therefore **UNVERIFIED**
  (§8). The prepaid model itself *is* official: a 402 "Insufficient Balance" error
  exists (<https://api-docs.deepseek.com/quick_start/error_codes>, read 2026-09-23)
  and the ToS §6.1 describes prepayment.

---

## 3. Rate limits, quota model, and what an app must handle

Source: <https://api-docs.deepseek.com/quick_start/rate_limit>, read 2026-09-23.

### 3.1 The published limit is concurrency

| | `deepseek-flash` | `deepseek-v4-pro` |
|---|---:|---:|
| Concurrency limit | **2500** | **500** |

Official properties of that limit, quoted from the page:

- "A request counts as one concurrent connection from the time it is sent until the
  model response is complete."
- "Concurrency limits are calculated at the account level, regardless of which API
  Key is used."
- "when the concurrency limit is exceeded, you will receive an HTTP 429 error code."
- Higher limits are available by request ("capacity expansion request"), at **no
  additional cost**; approved accounts then also get per-`user_id` sub-limits
  (2500 Flash / 500 V4-Pro per `user_id`).

**No requests-per-minute or tokens-per-minute quota is published on that page.**
Do not assume one exists, and do not assume none exists either —
**UNVERIFIED:** the docs read describe concurrency only.

### 3.2 The client must handle these failures

Source: <https://api-docs.deepseek.com/quick_start/error_codes>, read 2026-09-23.

| Code | Official cause | What a mobile slot must do |
|---|---|---|
| 400 Invalid Format | malformed request body | bug; do not retry |
| 401 Authentication Fails | wrong API key | do not retry; alert (server-side only) |
| 402 Insufficient Balance | "you have run out of balance" | **our** operational failure. The business stops when the prepaid balance empties. Needs monitoring and a top-up runbook |
| 422 Invalid Parameters | bad parameters | bug; do not retry |
| 429 Rate Limit Reached | "sending requests too quickly" | hard concurrency cap hit. Queue and back off. The docs themselves advise switching to another provider temporarily |
| 500 Server Error | provider issue | retry after a brief wait |
| 503 Server Overloaded | provider overloaded | retry after a brief wait |

Also relevant: `finish_reason` can be `insufficient_system_resource` ("the request is
interrupted due to insufficient resource of the inference system") or `aborted`
(<https://api-docs.deepseek.com/api/create-chat-completion>, read 2026-09-23). A
generation can therefore be cut short by the provider mid-stream.

**UNVERIFIED:** the docs read do **not** state whether tokens generated before an
`aborted` / `insufficient_system_resource` ending are billed. Assume they may be
billed, and reconcile against the usage dashboard before trusting a cost model.

### 3.3 Quota shape

- Prepaid balance, decremented per request; **402** when exhausted.
- ToS §6.1 (read 2026-09-23): "we reserve the right to suspend or terminate Services
  if your balance is insufficient. We are not responsible for any impacts or losses
  resulting from your failure to recharge on time."
- ToS §6.3: refunds are reviewed, "do not support partial refunds, and amounts
  already spent will not be refunded."

**Operational consequence:** a pack-selling app has a **provider-side balance** that
is a business-critical single point of failure. Low-balance alerting is a launch
requirement, not a follow-up.

---

## 4. API shape a mobile app cares about

Sources: <https://api-docs.deepseek.com/> (Your First API Call),
<https://api-docs.deepseek.com/api/create-chat-completion>,
<https://api-docs.deepseek.com/api/deepseek-api>,
<https://api-docs.deepseek.com/quick_start/rate_limit>,
<https://api-docs.deepseek.com/guides/responses_api> — all read 2026-09-23. Full
`/chat/completions` reference was read in full (schema, streaming event list,
`usage` object).

### 4.1 Compatibility and endpoints

| PARAM | VALUE |
|---|---|
| base_url (OpenAI format) | `https://api.deepseek.com` |
| base_url (Anthropic format) | `https://api.deepseek.com/anthropic` |
| Auth | HTTP Bearer (`Authorization: Bearer <API key>`) |
| Chat endpoint | `POST https://api.deepseek.com/chat/completions` |

> "The DeepSeek API uses an API format compatible with OpenAI/Anthropic. By modifying
> the configuration, you can use the OpenAI/Anthropic SDK or softwares compatible
> with the OpenAI/Anthropic API to access the DeepSeek API."
> — <https://api-docs.deepseek.com/>, read 2026-09-23

So: **OpenAI-compatible, yes.** Authentication is documented as `http` /
`bearer` (<https://api-docs.deepseek.com/api/deepseek-api>, read 2026-09-23). A
Responses API is also offered on the same base URL
(<https://api-docs.deepseek.com/guides/responses_api>, read 2026-09-23); the
Responses API is **stateless** (`store` not supported; `previous_response_id` and
`conversation` not supported), as is Chat Completions. **The client owns the
conversation history** — every turn re-sends the prefix, which is exactly why prompt
caching (§2.4) dominates the input cost of a chat app.

### 4.2 Streaming

- Chat Completions: `stream: true` returns "data-only server-sent events (SSE) …
  with the stream terminated by a `data: [DONE]` message."
- Usage arrives on the **last chunk** before `[DONE]`, in `usage`
  (`completion_tokens`, `prompt_tokens`, `prompt_cache_hit_tokens`,
  `prompt_cache_miss_tokens`, `total_tokens`). With `stream_options.include_usage:
  true`, every chunk carries `usage`, `null` except the last. There is no separate
  usage-only chunk.
- Responses API streaming ends with `response.completed` /
  `response.incomplete` / `response.failed` — "there is no `data: [DONE]` message."
  A client written for one shape will not parse the other.

### 4.3 Timeouts — read this before writing any HTTP client

From <https://api-docs.deepseek.com/quick_start/rate_limit>, read 2026-09-23,
"Request Keep-Alive Mechanism":

- After a request is sent, the connection may stay open a while. Non-streaming
  requests "continuously return empty lines"; streaming requests "continuously return
  SSE keep-alive comments (`: keep-alive`)."
- These do not break JSON parsing, but a hand-rolled parser must tolerate empty lines
  and comment lines.
- **"If the request has not started inference after 10 minutes, the server will close
  the connection."**

Client requirements that follow: the read/idle timeout must be set deliberately and
comfortably above the provider's behaviour (a short default read timeout will abort
healthy requests), the client must tolerate keep-alive padding, and a 10-minute
no-inference close must be surfaced as a retryable provider delay, not as a crash.
**UNVERIFIED:** no official recommendation for a specific client timeout value was
found, and no official statement of a *total* request duration ceiling was found —
only the 10-minute pre-inference rule.

### 4.4 Request-size and modality limits (from `/chat/completions` and Vision)

- `max_tokens` 1 … 384K; defaults 8K non-thinking / 64K thinking / 128K at
  `reasoning_effort: max`.
- Deprecated and no-op if sent: `frequency_penalty`, `presence_penalty`.
- `response_format: {type: "json_object"}` is supported but "you must also instruct
  the model to produce JSON yourself"; without that instruction the model "may
  generate an unending stream of whitespace" — a cost and latency trap.
- Vision is `deepseek-flash` only; images are allowed in `user` messages only, and
  an image in a `system` or `assistant` message returns **400**. Supported formats
  JPEG/PNG/GIF/WebP, detected from content.
- Image token ceiling: images are resized before inference and there is "an upper
  bound of **1024 tokens per image**"
  (<https://api-docs.deepseek.com/guides/vision>, read 2026-09-23). A screenshot
  costs at most ~1024 input tokens — cheap. `detail: "low"` downscales to 512×512.
- Limits: request body **48 MiB**; 32 MiB per inline/URL image, 64 MiB per
  Files-API `file_id` image; 600 images/request; 8192 px per side (drops to 4096 at
  ≥15 images); external image download must finish within 60 s.
- `user_id` is optional, must match `[a-zA-Z0-9\-_]+`, max 512 chars, and the docs
  explicitly say **"Do not include user privacy information in `user_id`."**

---

## 5. Mobile integration: what is safe, and what is not

### 5.1 Direct-from-device calls with the factory's key are prohibited

This is not a hardening suggestion. The provider's terms forbid it:

> "**The API key you create through your account is a necessary credential for
> calling the open platform API interface. Please keep your created API key
> securely, prevent any form of leakage, do not share or publicly disclose your API
> key with others, and do not expose it in browser or other client-side code. You
> will be solely responsible for any fees incurred and losses caused by sharing or
> leaking the API key.**"
> — DeepSeek Open Platform Terms of Service §2.2, release 2026-04-22, effective
> 2026-04-29, read 2026-09-23 from
> <https://cdn.deepseek.com/policies/en-US/deepseek-open-platform-terms-of-service.html>

An APK is client-side code. A string in an APK is recoverable by anyone who downloads
it, and it is our credential, billed to our prepaid balance. Combined with §3.3
above (the account can be suspended for insufficient balance) and §6 below (a
provider bill that scales with an attacker's requests), the exposure is direct
financial loss.

**Security requirement, stated plainly: the DeepSeek API key must never be shipped
in an APK, a `BuildConfig` field, a per-slot `.env` that gets bundled, a `strings.xml`
entry, or a resource file. It lives only on a server we control.** This satisfies
`.cursor/rules/security-secrets.mdc` as well.

The same clause also means the *client* is the wrong place for the key even in a
"trusted users" app, and it removes "embed the key and add a rate limit in the app"
from the design space entirely.

### 5.2 Client-side library choices

- **There is no official DeepSeek Android/Kotlin SDK.** What the official docs
  document is HTTP REST with example scripts in `curl`, Python, and JavaScript
  (<https://api-docs.deepseek.com/>, read 2026-09-23). The official GitHub org
  (<https://github.com/deepseek-ai>, read 2026-09-23) lists model/agent repos
  (`deepseek-harness`, `DeepSeek-V3`, `DeepSeek-R1`, `awesome-deepseek-integration`,
  `DeepSeek-OCR`) — no Android or Kotlin client library.
- Third-party Kotlin clients exist (e.g. `deepseek-kotlin`,
  `openai-kotlin`, JetBrains Koog's DeepSeek provider). They are **not official**,
  and search results read on 2026-09-23 showed at least some of them still
  advertising `deepseek-chat` / `deepseek-coder` model ids — names retired on
  2026-07-24 (§1). **UNVERIFIED:** their maintenance status, release cadence, and
  compatibility with `deepseek-flash` were not established from their own
  repositories in this session.
- Because the API is OpenAI-compatible REST over HTTPS, a plain `OkHttp`/`Retrofit`
  client, or the official OpenAI-compatible Kotlin client pointed at
  `https://api.deepseek.com`, is sufficient — **on the server.**
- Transport: HTTPS only. The repo's rule is to pin certificates "only with a clear
  threat model"
  (`.cursor/rules/security-devsecops.mdc`); nothing read from DeepSeek requires
  pinning.

### 5.3 The proxy is now a product requirement

Per [`../monetization-strategy.md`](../monetization-strategy.md) §3.7, a slot selling
money-costing credit is a **server project**, and `BillingCatalog.requiresPurchaseAuthority()`
already declares that the billing capability refuses a `SERVER_COST` catalog with no
`PurchaseAuthority`. The DeepSeek key requirement makes the same conclusion
unavoidable from the provider side: **the proxy that holds the key is the server that
must also verify the purchase token and own the token balance.** One server, two
forced jobs. The factory does not ship one today (§5.7 of that document).

### 5.4 Play-side obligations triggered by selling AI inference

Read from the DeepSeek ToS (read 2026-09-23) — these are DeepSeek's requirements on
us, and they map onto Google Play declarations:

- **§8.1:** "You shall clearly disclose to your end users that the Output content is
  generated by AI, and may contain errors or omissions and are for reference only."
  → an in-app AI disclaimer is contractually required, and it lines up with Play's
  AI-content declaration duties listed in `AGENTS.md`.
- **§3.3:** we must inform end users of personal-information processing rules *before*
  providing the service and obtain consent for the transfer of their data to
  DeepSeek. → Data safety and privacy policy must name DeepSeek as a sub-processor of
  user prompts, and consent must gate the first inference.
- **§3.2 / §3.4:** we are the operator of the downstream app and are responsible for
  regulating end-user behaviour and for our own security measures.
- **§5.** Brand: without permission we may **not** use the "DeepSeek" name, logo, or
  model name prominently or "side by side with your own logo", and may not describe
  the relationship as "official cooperation / strategic partner / certified
  integrator". §5.4 makes a breach a **fundamental breach** with the right to
  terminate and **forfeit any unconsumed balance**. → slot store copy must not
  claim a DeepSeek partnership, and should describe the engine factually if at all.
- **§4.2:** "We assign any rights, title, and interests—if any—in the Outputs of the
  Services to you" and outputs may be used for "derivative product development".
  → reselling outputs is permitted as far as these terms go.
- **§1.4:** "We make no warranty that the Services are available or will continue to
  be available in certain jurisdictions."
- **§7.2:** DeepSeek may restrict or suspend an account on its own judgement.

**UNVERIFIED:** whether the current DeepSeek Terms of Use, the Model Principles
document, or the acceptable-use rules impose additional restrictions on selling
inference to end users (for example on specific content categories or on
jurisdictions) was not established — only the Open Platform ToS was read in full.
Re-read the Terms of Use before a slot's first paid release.

---

## 6. Cost arithmetic: the minimum sale price of a pack

The purpose of this section is to make the assumptions **named** so they can be
argued with, not to produce one invented number. Every input below is either sourced
(§2, §3) or explicitly labelled as an assumption to be set per slot.

### 6.1 Provider cost per request

All prices are USD per 1M tokens (`p`, from §2.1). For one request:

```
C_request = ( hit_tokens × p_in_hit
            + miss_tokens × p_in_miss
            + out_tokens × p_out ) / 1e6
```

`out_tokens` **includes** reasoning tokens (§2.3). Use `usage` from the API response
as the source of truth; `prompt_cache_hit_tokens` and `prompt_cache_miss_tokens`
separate the two input prices.

For a *pack unit* (one unit of credit the app sells), define
`C_unit` = provider cost in USD to fulfil one unit.

### 6.2 The formula

```
                 C_unit
P_min = ─────────────────────────────────────────────
        (1−F_fraud)(1−F_billing)(1−F_refund)(1−F_fx)(1−M_target)
```

Named assumptions — each one is a decision, not a fact:

| Symbol | Meaning | Note |
|---|---|---|
| `C_unit` | provider cost per credit unit sold (USD) | computed from §6.1 |
| `F_fraud` | share of **granted** credit for which no payment is ever collected | §3.7 of `monetization-strategy.md`; see §6.4 |
| `F_billing` | Google Play service fee | sourced in §6.3 |
| `F_refund` | refunds / chargebacks on paid transactions | per-slot assumption |
| `F_fx` | FX + payout rounding loss | per-slot assumption; Play prices are set per-currency |
| `M_target` | target gross margin on **net** revenue | per-slot assumption |

Derivation (per granted credit unit): of `Q` granted units, `u = 1 − F_fraud` are
paid; net revenue per granted unit is `u × P × (1−F_billing)(1−F_refund)(1−F_fx)`.
Requiring `M = (Net − C_unit)/Net` gives `C_unit = Net(1−M)`, which rearranges to the
formula above. Note that `P_min → ∞` as any factor → 1, which is why each factor
needs a *measured* value, not a hopeful one.

Practical variant: the **required multiple** is
`K = 1 / [(1−F_fraud)(1−F_billing)(1−F_refund)(1−F_fx)(1−M_target)]`, so
`P_min = K × C_unit`. Recompute `K` whenever an assumption changes, and quote `K`
alongside any pack price.

### 6.3 The Play service fee, as read

Source: <https://support.google.com/googleplay/android-developer/answer/112622>,
read 2026-09-23.

For transactions with users in the **EEA, UK, or US** starting **2026-06-30**, the
fee depends on whether the install is "new" or "existing":

| Service fee type | Auto-renewing subscriptions | Other transactions (new installs) | Other transactions (existing installs) |
|---|---|---|---|
| **First $1M (USD) of annual earnings** | **10 % + 5 % billing fee** | **10 % + 5 % billing fee** | **10 % + 5 % billing fee** |
| Standard | 10 % + 5 % | 20 % + 5 % | 25 % + 5 %, or 20 % via external web links |
| Play Games Level Up / Apps Experience programs | 10 % + 5 % | 15 % + 5 % | 20 % + 5 %, or 15 % via external web links |

For **all remaining markets**, until the updated fees roll out globally: **15 % for
the first $1M (USD)** of annual revenue, 30 % above it; subscriptions 15 %.
The billing fee footnote: "Billing fee applies when a user completes a purchase of
an in-app digital service or feature using Google Play Billing or for the initial
purchase of a paid app or game."

**Chosen default assumption: `F_billing = 0.15`** — 10 % + 5 % in the EEA/UK/US
under $1M/year, and 15 % in remaining markets. Both land on 15 % for a young
account, which makes 0.15 the defensible starting value for a consumable pack.

**UNVERIFIED:** the interaction of the service fee with VAT/tax on the transaction
price — whether the developer's payout is computed on the tax-inclusive or
tax-exclusive price — was **not** confirmed from the pages read. If VAT
applies and is not separately accounted for, the effective take rate is higher than
0.15 and `F_billing` must be raised. Confirm on the official service-fee page before
pricing a real product.

### 6.4 Working the numbers

Provider cost per **1M tokens**, assuming a **70 % input / 30 % output** blend and
**all input cache-miss** (conservative — caching can only improve it):

| Model / window | Input component | Output component | `C_unit` per 1M tokens |
|---|---:|---:|---:|
| `deepseek-v4-pro`, peak | 0.70 × $1.32 = $0.924 | 0.30 × $3.96 = $1.188 | **$2.112** |
| `deepseek-v4-pro`, off-peak | 0.70 × $0.66 = $0.462 | 0.30 × $1.98 = $0.594 | **$1.056** |
| `deepseek-flash`, peak | 0.70 × $0.30 = $0.210 | 0.30 × $1.20 = $0.360 | **$0.570** |
| `deepseek-flash`, off-peak | 0.70 × $0.15 = $0.105 | 0.30 × $0.60 = $0.180 | **$0.285** |

Assumption set for the multiplier (each labelled as an assumption):
`F_fraud = 0.20`, `F_billing = 0.15`, `F_refund = 0.02`, `F_fx = 0.02`.

| `M_target` | `K` with `F_fraud = 0.20` (granting credit without verification) | `K` with `F_fraud = 0.01` (purchase verified server-side) |
|---|---:|---:|
| 0 % (break-even) | **1.53×** | 1.24× |
| 30 % | 2.19× | 1.77× |
| 50 % | 3.06× | 2.47× |
| **60 %** | **3.83×** | **3.09×** |
| 70 % | 5.10× | 4.12× |
| 80 % | 7.66× | 6.19× |

Reading of that table:

- **The owner's 2× markup implies `M_target ≈ 25 %`** under these assumptions, not
  50 % — and if fraud is worse than 20 %, 2× goes underwater. Break-even alone needs
  **1.53× cost** when 20 % of granted credit is never paid for.
- **Server-side verification is worth ~$0.74 of price per $1.00 of provider cost at a
  60 % margin target** (`3.83 − 3.09 = 0.74`). On the 1M-token V4-Pro pack below that
  is **$1.56 per pack**. That is the quantified case for building the proxy rather
  than trusting the purchase callback.

### 6.5 Two worked packs

Using `K = 3.83` (60 % margin target, unverified granting) — the *worst* realistic
case, as the planning number:

**Pack A — 1,000,000 blended tokens on `deepseek-v4-pro`, priced at peak:**

```
C_unit   = $2.112
P_min    = 3.83 × $2.112 = $8.09
naive 2× = $4.22      ← under-prices by ~48 %
```

**Pack B — 1,000,000 blended tokens on `deepseek-flash`, priced off-peak:**

```
C_unit   = $0.285
P_min    = 3.83 × $0.285 = $1.09
naive 2× = $0.57
```

Then **round up to a price point you can actually set in Play Console** — the listed
price, not `P_min`, is what the user pays, and price points are discrete. Verifying
which points are available is a Play Console step, not something this document can
establish. **UNVERIFIED:** the current set of purchasable price points.

### 6.6 Why the unit must be an app credit, never one DeepSeek token

The owner's framing — "a token costs us $10, we sell it for $20" — does not survive
contact with §2.1:

- The most expensive token we can buy is one `deepseek-v4-pro` **output** token at
  peak: **$3.96 / 1,000,000 = $0.00000396.**
- At the §6.4 blended peak rate for V4-Pro ($2.112 per 1M), **$10 of provider spend
  buys ≈ 4.73 million tokens.**
- So the framing's "one token" is really a **~4.7 M-token bundle**, i.e. a *pack*,
  not a token. Calling it a token in the UI, the Play Console product name, or the
  store listing would be materially wrong and invites a Play pricing complaint.

Per-interaction sanity check, so pack sizes are chosen from real numbers:

| Interaction | Provider cost | `P_min` at `K = 3.83` |
|---|---:|---:|
| 500 input (cache-miss) + 300 output, `deepseek-v4-pro` peak | $0.001848 | **$0.00708** |
| 500 input (cache-miss) + 300 output, `deepseek-flash` off-peak | $0.000255 | **$0.000976** |

So a **$4.99** pack on V4-Pro peak at `K = 3.83` can fund about
`4.99 / 3.83 / 0.001848 ≈ 705` such interactions — before any of them hit thinking
mode. That is the honest shape of the product: **hundreds of interactions per pack,
sold on a 3–4× multiple of provider cost, with the multiplier driven by the Play fee
and the fraud allowance.**

### 6.7 Cheapest legitimate levers on `C_unit`

1. **Prompt caching** (§2.4): up to 50× off input on Flash. Keep everything static
   at the front of the prompt; never put a timestamp or per-user nonce ahead of it.
   Verify the hit rate from `prompt_cache_hit_tokens` per request rather than
   assuming it.
2. **Disable thinking mode** when the task does not need it (`thinking: {type:
   "disabled"}` or `reasoning_effort: "none"`), and cap `max_tokens` (§2.3). Default
   thinking costs output tokens at up to $3.96/1M.
3. **Model choice per feature**: `deepseek-flash` output is $1.20/1M at peak versus
   $3.96/1M on V4-Pro — 3.3× cheaper — and only Flash does vision. Reserve V4-Pro
   for what actually needs it.
4. **Off-peak scheduling** for anything deferrable: a batch job run off-peak pays
   half. Interactive mobile traffic cannot be rescheduled, so do not budget on it.

---

## 7. Re-check list before a pricing decision

Re-read these, and re-date every number carried into a plan:

- <https://api-docs.deepseek.com/quick_start/pricing> — models, prices, peak windows
- <https://api-docs.deepseek.com/quick_start/rate_limit> — concurrency limits
- <https://api-docs.deepseek.com/quick_start/error_codes> — error handling
- <https://api-docs.deepseek.com/updates> — model retirements and price changes
- <https://api-docs.deepseek.com/api/create-chat-completion> — request/response shape
- <https://support.google.com/googleplay/android-developer/answer/112622> — Play fee
- <https://cdn.deepseek.com/policies/en-US/deepseek-open-platform-terms-of-service.html>
  — key handling (§2.2), AI disclosure (§8.1), brand use (§5)

Also re-check, because they were **not** confirmed here: the minimum top-up amount,
tax treatment of listed prices, billing of aborted generations, and the available
Play price points (§8).

---

## 8. UNVERIFIED register

Each entry states what could not be confirmed and why. Do not fill these gaps with a
plausible value.

| # | Item | Status |
|---|---|---|
| 1 | **Minimum top-up amount.** No official page read states it. Third-party sites claim $2.00 → **not** an official source. | UNVERIFIED |
| 2 | **Whether listed prices are tax-inclusive.** The pricing page does not say. | UNVERIFIED |
| 3 | **Whether reasoning tokens are billed at the output rate.** Inferred from two documented facts (`reasoning_tokens` is a breakdown of `completion_tokens`; billing is on total input + output tokens). High confidence, not a verbatim statement. | INFERRED — confirm against a real `usage` block |
| 4 | **Billing of partially generated output** when `finish_reason` is `aborted` or `insufficient_system_resource`. | UNVERIFIED |
| 5 | **Requests-per-minute / tokens-per-minute quotas.** The rate-limit page documents concurrency only. Neither the existence nor the absence of an RPM quota was confirmed. | UNVERIFIED |
| 6 | **A recommended client timeout value**, and whether a total request-duration ceiling exists beyond the 10-minute pre-inference rule. | UNVERIFIED |
| 7 | **Whether the service fee is computed on the tax-inclusive price.** Affects `F_billing`. | UNVERIFIED |
| 8 | **The set of purchasable Play price points** (needed to round `P_min` up). | UNVERIFIED — Play Console step |
| 9 | **The DeepSeek Terms of Use, Model Principles, and any acceptable-use policy** beyond the Open Platform ToS (this session read the ToS only). Additional restrictions on selling inference to end users may exist. | UNVERIFIED |
| 10 | **Maintenance and correctness of third-party Kotlin SDKs**; at least some still advertise retired model ids (`deepseek-chat`, `deepseek-coder`). | UNVERIFIED |
| 11 | **The `/quick_start/limits` URL** did not serve a distinct page — it returned the "Your First API Call" content (read 2026-09-23). Any limits beyond those in §4.4 were not found there. | Page not distinct from landing page |
| 12 | **`platform.deepseek.com` billing pages** (top-up options, balance, expiry) were not read — they require an authenticated session. Only the documentation site, the ToS, and the GitHub org were read. | UNVERIFIED |

---

## 9. Secret scan

Requested check, performed 2026-09-23:

- Repo-wide search for a DeepSeek-style key pattern (`sk-` followed by 20+ token
  characters): **no matches**.
- Repo-wide search for `DEEPSEEK` / `deepseek`: **no matches** in this repository.

**No API key, token, or secret was found in the repository, and none has been
reproduced in this document.** If a key is later added for local testing, it belongs
in a gitignored per-slot `.env` with a `.env.example` placeholder, per
`.cursor/rules/security-secrets.mdc` — and never in a file that is bundled into an
APK (§5.1).

---

## 10. Sources read in this session (2026-09-23)

| Source | What it established |
|---|---|
| <https://api-docs.deepseek.com/> | OpenAI/Anthropic compatibility, base URLs, model parameter, `stream: true` |
| <https://api-docs.deepseek.com/quick_start/pricing> | Model table, all prices, cache hit/miss split, peak/off-peak windows, concurrency limits |
| <https://api-docs.deepseek.com/quick_start/rate_limit> | Concurrency limits, 429 behaviour, `user_id` isolation, keep-alive, 10-minute pre-inference close |
| <https://api-docs.deepseek.com/quick_start/error_codes> | 400/401/402/422/429/500/503 semantics |
| <https://api-docs.deepseek.com/quick_start/token_usage> | Token↔character ratios, offline tokenizer, image token calculator |
| <https://api-docs.deepseek.com/api/create-chat-completion> | Full request/response schema, streaming chunks, `usage`, `finish_reason`, `max_tokens` defaults, parameter support |
| <https://api-docs.deepseek.com/api/deepseek-api> | Bearer auth, MIT license, support contact, ToS link |
| <https://api-docs.deepseek.com/guides/kv_cache> | Context caching defaults, prefix-unit matching, hit/miss counters |
| <https://api-docs.deepseek.com/guides/vision> | Vision support on Flash, image limits, ≤1024 tokens per image |
| <https://api-docs.deepseek.com/guides/responses_api> | Responses API support, statelessness, streaming event list |
| <https://api-docs.deepseek.com/updates> | V4.1-Flash release 2026-09-10, price adjustment 2026-08-16, retirement of `deepseek-chat`/`deepseek-reasoner` on 2026-07-24 |
| <https://github.com/deepseek-ai> | Official org repositories — no Android/Kotlin SDK |
| <https://cdn.deepseek.com/policies/en-US/deepseek-open-platform-terms-of-service.html> | §2.2 key must not be in client-side code; §3.3 privacy/consent; §4.2 output rights; §5 brand limits; §6 prepaid balance and refunds; §7 suspension; §8.1 AI disclosure |
| <https://support.google.com/googleplay/android-developer/answer/112622> | Play service fee table, 5 % billing fee, first-$1M tier, remaining-markets 15 % |

Repo sources: [`../monetization-strategy.md`](../monetization-strategy.md) §3.2, §3.7,
§5.7; `.cursor/rules/verification-discipline.mdc`; `.cursor/rules/security-secrets.mdc`;
`AGENTS.md` store-pack and Play-compliance contracts.
