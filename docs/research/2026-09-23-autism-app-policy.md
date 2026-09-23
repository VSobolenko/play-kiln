# Autism-adjacent visual-schedule utility — Google Play policy surface (2026-09-23)

This is a policy brief for one planned slot: a **visual-schedule / routine utility** used by
autistic children and their families, planned as free, **no ads**, monetised with repeatable
**consumable packs**.

It exists to stop one class of mistake before it reaches a store listing: describing a
scheduling *utility* in language that makes it a *therapeutic or medical claim*.

## How to read this document

- Every claim has a URL to the page it came from. Operative wording is quoted where it decides
  something.
- **UNVERIFIED** means: stated plainly, because a policy was expected, searched for, and
  **could not be read from a primary source**. UNVERIFIED items are *not* findings in either
  direction. Several are UNVERIFIED *absences* ("no such rule found"), which is not the same as
  "a rule exists and permits this".
- Nothing here is legal advice. Where a question is decided by law (COPPA/GDPR) or by a Play
  Console declaration form rather than by published policy text, that is flagged rather than
  answered.
- Pages were read on **2026-09-23** in `en-US` (Play Console Help) and at the URLs printed
  below. Live policy text changes; re-read before publishing.

---

## 1. Families: does this audience trigger Families requirements, and which regime applies?

### 1.1 The trigger is the **Target audience declaration**, and Google can override it

Google Play decides "is this for children?" primarily from the **Target audience and content**
section of Play Console, and the consequence is mechanical:

> "Your app is designed primarily for children under 13. → You must comply with Google Play's
> Families Policy Requirements…"
> "Your app is designed for everyone, including children. → Any apps that have at least one
> target audience age group that includes children must comply with Google Play's Families
> Policy Requirements…"
> "Your app is not designed for children. → You must still meet the requirements outlined in
> Google Play Developer Program policies and Developer Distribution Agreement."

Source: <https://support.google.com/googleplay/android-developer/answer/9867159>

The same page states the rule directly: "Any apps that include children in their target
audience must comply with Google Play's Families Policy Requirements."

So there are only **two regimes**, and they are not parallel programmes:

| Regime | Who it binds | Where it lives |
|---|---|---|
| **Families Policy Requirements** | Any app with at least one child age group in its target audience | Mandatory; <https://support.google.com/googleplay/android-developer/answer/9893335> |
| **Designed for Families** | An *opt-in programme* (badges, extra review, DDA addendum) | Opt-in; Addendum text at <https://play.google/intl/ALL_us/families/developer-distribution-agreement-addendum.html> |

The Designed for Families addendum is written as an opt-in: "**By opting into** the Designed for
Families program, You consent to be bound by these terms…" and "Your participation in the
Designed for Families program provides you with the ability to promote your app that is designed
for kids and family audiences." (same URL)

> **UNVERIFIED (partial):** the current, live status of a page titled "Designed for Families
> Program Requirements" could not be established. The addendum still obliges participants to
> comply with "the Designed for Families Program Requirements", but that referenced document was
> not retrievable, and a Google Play policy announcement describes the 2022 change as
> "We've introduced a new Families policy and updated our Designed for Families program policy"
> (<https://support.google.com/googleplay/android-developer/announcements/13412212>). The
> *consolidated* policy mirror still asserts "Apps designed specifically for children must comply
> with the Designed for Families policy"
> (<https://support.google.com/googleplay/android-developer/answer/17517561>). Treat the
> relationship between the two names as **live text, unresolved**, and let the Play Console
> Target-audience flow decide what binds you in practice.

### 1.2 Declaring "for parents/carers" does **not** settle it

Two override rules matter, and both are quoted policy:

1. Imagery and terminology can override your declaration:

> "Regardless of what you identify in the Google Play Console, if you choose to include imagery
> and terminology in your app that could be considered targeting children, this may impact Google
> Play's assessment of your declared target audience. Google Play reserves the right to conduct
> its own review of the app information that you provide to determine whether the target audience
> that you disclose is accurate."

Source: <https://support.google.com/googleplay/android-developer/answer/9893335>

2. Child-appealing *store listing* elements can get the app rejected outright, with only two
   exits:

> "If your app is not primarily designed for children under 13 but your listing contains marketing
> elements that suggest otherwise (such as youthful animation or young characters in the graphic
> assets), Google Play may reject your app. If that happens, you'll have the following options:
> Remove any marketing elements in your store listing that may appeal to children… Change the
> target age group(s) of your app to only include children under 13 (which will require complying
> with Google Play Families Policy)."

Source: <https://support.google.com/googleplay/android-developer/answer/9867159>

And mis-declaring is itself an enforcement trigger:

> "Misrepresentation of any information about your app in the Play Console, including in the
> Target Audience and Content section, may result in removal or suspension of your app…"

Source: <https://support.google.com/googleplay/android-developer/answer/9893335>

### 1.3 Which regime applies to *this* product

The honest answer is **it is a product decision with a policy consequence, not a fact** — and the
consequence is asymmetric.

- If the slot ships a **child-facing** schedule surface (the child taps the picture cards, the
  child is the user, child-directed imagery/terminology in the app or listing), then a child age
  group is the accurate declaration, **Families Policy Requirements bind**, and that is
  inescapable: "Failure to satisfy these requirements may result in app removal or suspension."
  (<https://support.google.com/googleplay/android-developer/answer/9893335>)
- If the slot ships an **adult-facing** configuration tool for the parent/carer (adult copy,
  adult UI, no child-directed imagery or animated characters), the accurate declaration is an
  adult age group, and the Families Policy Requirements are not triggered *by declaration*. The
  residual risk is §1.2: Google may disagree, and a wrong answer is itself a violation.
- There is no third option where the child uses the app but the app is "not for children". If the
  child is a user, the strongest expectation is that families rules apply.

**Practical consequence:** the decision that must be made *before the slot is scaffolded* is
"who is the declared user of this app". It drives the data rules, the SDK/API allowlist, the ads
rules (moot here — see §3), the monetisation framing (§4), and the content string of the app and
the listing.

Also note, still on the target-audience page, that this is not only about a declared age: "if your
app targets any users who are under the age of 21, you must consider if they would be considered
children under their local laws. Apps that target children must comply with Google Play Families
Policy requirements."

---

## 2. Health claims: where the line between a utility and a therapeutic claim sits

### 2.1 The policy that decides it

**Health Content and Services** (<https://support.google.com/googleplay/android-developer/answer/16679511>):

> "We don't allow apps that expose users to harmful health content and services."
> "If your app offers health-related features or information as part of its functionality, or
> accesses health data to support non-health features, it must comply with the existing Google
> Play Developer Policies… in addition to the below requirements"
> "We don't allow apps with health and medical related functionalities that are misleading or
> potentially harmful."
> "Health Misinformation — We don't allow apps containing misleading health claims that contradict
> existing medical consensus, or can cause harm to users."
> (examples of violations) "Advocacy of harmful, unapproved treatments."

**Misleading Claims**, inside **Deceptive Behavior**
(<https://support.google.com/googleplay/android-developer/answer/9888077>):

> "We don't allow apps that contain false or misleading information or claims, including in the
> description, title, icon, and screenshots."
> (examples) "Apps that misrepresent or do not accurately and clearly describe their
> functionality".

And the metadata-accuracy rule in the same page: "Apps must provide an accurate disclosure,
description and images/video of their functionality in all parts of the metadata."

### 2.2 What that means if the app is in scope

Two obligations appear *only* when an app is inside the health policy. Both are quotable:

> "Apps that are regulated because they are a medical device must be declared as such… Other
> health and medical apps must include a clear disclaimer in their app description indicating that
> the app is 'not a medical device and does not diagnose, treat, cure, or prevent any medical
> condition.'"
> "Apps must also remind users to consult a healthcare professional for medical advice, diagnosis,
> or treatment."

Source: <https://support.google.com/googleplay/android-developer/answer/16679511>

> **UNVERIFIED:** a secondary source claims this disclaimer must appear in the *first paragraph*
> of the description. The policy text says only "in their app description". Do not build a
> requirement out of the secondary claim; read the Play Console field when the app is created.

### 2.3 Is "autism" itself a health declaration?

> **UNVERIFIED (absence):** no policy text read for this brief names autism, and no policy read
> makes a diagnosis word, by itself, a health declaration.

What the Console actually asks for is a declaration of *features*, not of audiences or conditions
(<https://support.google.com/googleplay/android-developer/answer/14738291>):

> "All developers that have an app published on Google Play must complete the Health apps
> declaration…"
> "Even developers with apps that do not offer any health features must complete this form and
> certify that no health features are offered through the app."
> "If your app doesn't provide any health features or access health data to support non-health
> features, you should declare: My app doesn't provide any health features."

The declarable feature categories are enumerated on the same page
(<https://support.google.com/googleplay/android-developer/answer/14738291>). The nearest
categories to an autism-adjacent schedule app, quoted:

- "Diseases and Conditions Management — Apps focusing on managing acute or chronic physical health
  conditions and diseases (like diabetes, asthma) …"
- "Mental and Behavioral Health — Tools for mental health support, counseling services and
  addiction recovery programs"
- "Physical Therapy and Rehabilitation — Apps supporting physical therapy exercises,
  rehabilitation programs… Includes tools for those with disabilities to improve accessibility
  and mobility."
- "Healthcare Services and Management — For scheduling medical appointments, reminders…"

A generic picture-sequence schedule that makes **no clinical claim** maps literally to none of
these. But the policy's own trigger is broader than the feature list:

> "If your app offers health-related features or information as part of its functionality, or
> accesses health data to support non-health features, it must comply with the existing Google
> Play Developer Policies… in addition to the below requirements"
> "If your app is not primarily a health app, but has health-related features and accesses health
> data, it is still in scope of the Health App policy."

Source: <https://support.google.com/googleplay/android-developer/answer/16679511>

A listing that says "for autistic children", or screenshots that promise developmental benefit, is
health-related *information* about a condition, and the reviewer can reasonably treat the app as
in scope.

> **UNVERIFIED (by design of the process):** whether Play Console's Health Apps review classifies
> a given schedule utility as health-related is a **Console review decision**, not something
> published policy resolves. The two honest declarations are "my app doesn't provide any health
> features" (only if that is true of the shipped functionality **and** the listing makes no health
> claim) or an in-scope declaration plus the disclaimer. A false answer is a violation: Families
> policy requirement 3 requires "accurate responses about your app in the Target Audience and
> Content section, Data safety section, and IARC Content Rating Questionnaire"
> (<https://support.google.com/googleplay/android-developer/answer/9893335>).

### 2.4 The boundary, stated as a rule the factory can apply

| Framing | Verdict | Why |
|---|---|---|
| "Shows what happens next, as pictures, in a sequence you choose." | **Utility — permitted** | Accurate description of functionality; no claim about a condition. Required by "Apps must provide an accurate disclosure, description… of their functionality in all parts of the metadata" (<https://support.google.com/googleplay/android-developer/answer/9888077>). |
| "A visual schedule for autistic children." | **Borderline — audience naming, no claim** | Names no benefit, but introduces condition-related information and moves the app toward health-policy scope (§2.3). Expect the Health apps declaration question. |
| "Supports autistic children", "helps with transitions", "reduces meltdowns", "improves communication", "therapy", "treatment", "clinically proven", "recommended by therapists" | **Health/medical claim — do not ship** | Falls under "misleading or potentially harmful" health functionality and/or "misleading health claims", and if untrue also under Misleading Claims. A claim of effect on a condition is exactly what the health policy reserves. |

The dividing line is **claims about a condition, a person's development, or an outcome**, versus
**a literal description of a picture-based reminder**. The first needs regulatory proof or the
disclaimer and invites Health Misinformation enforcement; the second is a utility.

> **UNVERIFIED:** no policy was found that enumerates permitted or prohibited *phrasings* for
> autism-adjacent apps. The boundary above is the application of the quoted rules, not a quoted
> rule.

---

## 3. Advertising: is "no ads" safe, or required?

**No-ads is safe, and it is not required.** Play permits ads in child-directed apps, under
conditions. Both the general and the Families framing are explicit:

> "To maintain a quality experience, we take into account your ad's content, audience, user
> experience, behavior, as well as security and privacy… **We also have additional requirements
> for ads if you're monetizing an app that targets children on Google Play.**"
> "If you're monetizing an app that targets children on Google Play, it's important that your app
> follows the Families Ads and Monetization Policy Requirements."

Source: <https://support.google.com/googleplay/android-developer/answer/9857753>

The Families conditions, if an app shows ads to children or users of unknown age
(<https://support.google.com/googleplay/android-developer/answer/9893335>):

> "Only use Google Play Families Self-Certified Ads SDKs to display ads to those users;"
> "Ensure ads displayed to those users do not involve interest-based advertising … or
> remarketing;"
> "Ensure ads displayed to those users present content that is appropriate for children;"
> "Ensure ads displayed to those users follow the Families ad format requirements"

plus the data rules that strip the usual ad plumbing:

> "Apps that solely target children must not transmit Android advertising identifier (AAID), SIM
> Serial, Build Serial, BSSID, MAC, SSID, IMEI, and/or IMSI."
> "Apps solely targeted to children should not request AD_ID permission when targeting Android API
> 33 or higher."

The self-certified-SDK programme ("Google Play Families Self-Certified Ads SDK Program",
<https://support.google.com/googleplay/android-developer/answer/12918983>) is currently closed to
new SDK applicants, which does not change the developer obligation to use only certified SDK
versions: "you must only use ads SDK versions that have self-certified compliance with Google Play
policies"
(<https://support.google.com/googleplay/android-developer/answer/9900633>).

**Conclusion for this slot:** the factory default of no ads is **the low-risk choice, not a policy
requirement**. It is also the only choice that keeps §5 (data), §4 (monetisation framing) and the
SDK allowlist simple. If ads were ever added, an app with any child age group in its audience
would immediately inherit the entire Families ads regime above — and, if declared solely
child-directed, an SDK allowlist restricted to child-approved, self-certified SDKs.

> **UNVERIFIED (absence):** no Play policy was found that *requires* an app aimed at this audience
> to be ad-free.

---

## 4. In-app purchases — the decision-relevant section

### 4.1 Play Billing is mandatory for these packs

Consumable packs are digital goods, so the Payments policy applies:

> "Google Play's billing system is required for developers offering in-app purchases of digital
> goods and services distributed on Google Play."
> (what must use Play Billing) "Digital items (such as virtual currencies, extra lives, additional
> playtime, add-on items, characters, or avatars)"
> "You can use Google Play's billing system to sell a one-time product or subscriptions on a
> recurring basis."
> "Within an app, developers may not lead users to a payment method other than Google Play's
> billing system unless Section 3, 8, or 9 of Payments policy applies."

Source: <https://support.google.com/googleplay/android-developer/answer/10281818>

### 4.2 There is **no** Play policy against IAP in a children's app

This was checked specifically, because it is the question the plan turns on.

> **The Families Policy Requirements do not prohibit in-app purchases.** Instead they *regulate
> how purchases may be presented.* The monetisation clauses are inside a section that explicitly
> reaches purchases:

> "The policies below apply to all monetization and advertising in your app, including ads,
> cross-promotions (for your apps and third party apps), **offers for in-app purchases**, or any
> other commercial content (such as paid product placement)."
> "Google Play reserves the right to reject, remove or suspend apps for overly aggressive
> commercial tactics."
> "Monetization and advertising in your app must not have deceptive content or be designed in a
> way that will result in inadvertent clicks from child users."
> Prohibited: "Use of shocking or emotionally manipulative tactics to encourage ads viewing or
> in-app purchases."
> Prohibited: "Not providing a distinction between the use of virtual game coins versus real-life
> money to make in-app purchases."

Source: <https://support.google.com/googleplay/android-developer/answer/9893335>

The target-audience guidance adds a design expectation rather than a ban: "Developmental
appropriateness: Consider the presence of sensitive or mature content, concepts, or themes, and
whether they are appropriate for the intended age. **This goes for ads and in-app purchases,
too.**" (<https://support.google.com/googleplay/android-developer/answer/9867159>)

> **UNVERIFIED (absence — high confidence of absence, and important):** no Google Play developer
> policy was found that (a) prohibits in-app purchases in child-directed apps, (b) requires
> parental consent as a precondition of an in-app purchase, or (c) requires a parental gate /
> adult action specifically for a purchase. The Families policy's "adult action" mechanism is
> defined only for **social** features and for children exchanging personal information, not for
> purchases. If a reviewer later requires a purchase gate, it will come from the Console
> declaration or from law, not from a policy sentence that could be cited today.

### 4.3 What Play *does* provide is a parent-side control — and it only works with Play Billing

> "If you're a parent in a family group, you can require family members to get your permission to
> purchase or download content on Google Play. **Purchase approval settings only apply to
> purchases made through Google Play's billing system.**"

Source: <https://support.google.com/families/answer/7039872>
</br>Also: <https://support.google.com/googleplay/answer/6294544>

The consequence is explicit for anything else:

> "Purchase approvals in Play won't apply if a child uses the billing system from the developer."

Source: <https://support.google.com/googleplay/answer/11174377>

So staying on Play's own billing is not only mandatory here — it is also the configuration in which
the parent's existing approval controls actually function. Steering children toward an alternative
payment path would remove the parent's control: a bad trade for this audience.

### 4.4 Verdict on the monetisation plan

**The planned model does not have to change on policy grounds — but it must be built to a specific
shape, and one legal question must be answered by a human.**

Supported by the policy text above:

- Free, no ads, with repeatable **consumable** packs, is **permitted**. There is no policy rule that
  makes it inappropriate for this audience.
- Those packs **must** be sold through Google Play Billing (§4.1).
- If the app includes children in its target audience, the purchase surface must be **non-coercive**
  and **truthful about what real money buys**: no emotionally manipulative purchase prompts, no
  blurring of virtual currency and real money, nothing that produces inadvertent taps, and no
  "overly aggressive commercial tactics" (all quoted in §4.2). This is a design constraint on the
  pack UI, not a ban on packs.
- If the app is declared child-directed, the *quality* of the IAP presentation is also evaluated by
  the Teacher Approved programme, whose published criteria include "Ads, in-app purchases, and
  cross-promotion, if present, must be appropriate and compliant with the Families ads and
  monetization policies. Teachers and specialists look at how ads, in-app purchases, and
  cross-promotion affect the overall quality of the experience for a child under 13."
  (<https://play.google.com/console/about/programs/teacherapproved/>) — participation is opt-in
  and not guaranteed, but the criteria state what "appropriate" looks like.

Not decidable from policy:

> **UNVERIFIED / needs a human:** whether purchases made by a child in this app require
> verifiable parental consent is a **legal** question (the Families policy requires compliance with
> "the U.S. Children's Online Privacy and Protection Act (COPPA), E.U. General Data Protection
> Regulation (GDPR), and any other applicable laws or regulations",
> <https://support.google.com/googleplay/android-developer/answer/9893335>) — the policy imposes
> the obligation to comply but does not itself define purchase-consent mechanics. This is the one
> item in this document that a lawyer or the Play Console declaration decides, not a policy page.

**What would actually force a change** is therefore not "packs are forbidden". It is the
combination of audience declaration plus purchase *design*: if the only way to monetise the packs
is a nag/energy/timer loop, that design is what the families monetisation rules prohibit, and the
model would have to become something else (for example a single non-consumable unlock). A
non-coercive, clearly-priced bundle of extra content is the shape the quoted policy permits.

---

## 5. Data safety, permissions, and the features that become inadvisable

### 5.1 Data safety: disclose what leaves the device — local-only processing is out of scope

> "All developers must complete a clear and accurate Data safety section for every app detailing
> collection, use, and sharing of user data."
> "Even developers with apps that do not collect any user data must complete this form and provide
> a link to their privacy policy."
> "**On-device access/processing:** User data accessed by your app that is only processed locally
> on the user's device and not sent off device does not need to be disclosed."
> "'Collect' means transmitting data from your app off a user's device."

Source: <https://support.google.com/googleplay/android-developer/answer/10787469>

This is the single most useful rule for this slot: **an offline app that keeps routine data and
photos of the child on the device declares no collection at all** — as long as nothing is
uploaded, not even to a first-party server. The moment a photo or a name is synced, backed up to a
developer server, or sent to an AI API, it becomes collected data and must be declared (the
declarable types include `Photos and videos — A user's photos`, `Personal info — Name`,
`Health info — Information about a user's health, such as medical records or symptoms`, and
`Identifiers`, same URL). The form's own warning applies: "You alone are responsible for making
complete and accurate declarations."

Note also that an on-device feature is not automatically cheap: if a third-party SDK in the app
transmits anything off device, that is your declaration, not the SDK's
(same URL: "You must reflect data collection or sharing carried out by such third-party code in
the Data safety form for your app").

### 5.2 User Data policy and a children's audience

Personal and sensitive data includes, per policy, "personally identifiable information … health
data, Health Connect data … microphone, camera, and other sensitive device or usage data"
(<https://support.google.com/googleplay/android-developer/answer/10144311>). Requirements that
matter here, quoted from the same page:

- Limit use "to app and service functionality and policy-conforming purposes reasonably expected by
  the user".
- "Handle all personal and sensitive user data securely, including transmitting it using modern
  cryptography (for example, over HTTPS)."
- "Do not sell personal and sensitive user data."
- "Have a valid privacy policy in both the app's store listing and within the app itself." (Privacy
  policy is required "even … apps that do not access any personal and sensitive user data", and must
  be at a "publicly accessible and non-geofenced URL (no PDFs)".)
- Applies to children's apps: "**Your app targets children → Your app must not include an SDK that
  is not approved for use in child-directed services.**"
- Prominent disclosure + affirmative consent is required when data access is "not within the
  reasonable expectation of the user"; the disclosure "Must be within the app itself", "Must be
  displayed in the normal usage of the app and not require the user to navigate into a menu or
  settings", and consent "Must require affirmative user action".

### 5.3 The Families data and SDK rules that decide features

Quoted from <https://support.google.com/googleplay/android-developer/answer/9893335>:

> "You must disclose the collection of any personal and sensitive information from children in your
> app, including through APIs and SDKs called or used in your app. Sensitive information from
> children includes, but is not limited to, authentication information, **microphone and camera
> sensor data**, device data, Android ID, and ad usage data."
> "Apps that solely target children must not transmit AAID, SIM Serial, Build Serial, BSSID, MAC,
> SSID, IMEI, and/or IMSI."
> "Apps that solely target children may not request location permission, or collect, use, and
> transmit precise location."
> "Apps that solely target children must not contain any APIs or SDKs that are not approved for use
> in primarily child-directed services."
> "Apps that target both children and older audiences must not implement APIs or SDKs that are not
> approved for use in child-directed services unless they are used behind a neutral age screen or
> implemented in a way that does not result in the collection of data from children."

Also, from the permission rules: "All user Photos are personal and sensitive data subject to the
User Data policy. Apps that target Android 13 or later (API level 33+) may only request the
`READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO` permissions if system pickers (like the Android Photo
Picker) are not sufficient for your app to provide core functionality… must submit a Play Console
declaration to demonstrate access needs"
(<https://support.google.com/googleplay/android-developer/answer/9888170>).

### 5.4 Features that are inadvisable or must be re-thought

| Feature in the planned product | Policy position |
|---|---|
| Photos of the child stored **on device only** | Not "collection" under Data safety (§5.1). Photo Picker avoids a broad media permission (§5.3). Camera use of a child is still "camera sensor data" that Families policy requires you to **disclose** if collected. |
| Free-text names of the child / family | "Personal info — Name" is a disclosed data type if it leaves the device; and per target-audience guidance, "Any collection of personal and sensitive information from children, such as name or e-mail address, must be disclosed and should be collected with parental consent if required" (<https://support.google.com/googleplay/android-developer/answer/9867159>). Keep identifiers local, or prefer non-identifying labels. |
| Photo **upload** / cloud sync / photo→schedule generation on a server | Turns on data collection and sharing: Photos and videos + possibly Identifiers, plus prominent disclosure and consent (§5.2). If the app has any child age group, the receiving SDK/API must also be one "approved for use in primarily child-directed services" (§5.3). |
| Sending a child's photo or routine to a third-party generative AI API | Same as above, **plus** the AI policy in §7.3. This is the most policy-loaded feature in the plan. |
| Analytics / crash SDKs | "Apps that solely target children must not contain any APIs or SDKs that are not approved for use in primarily child-directed services" (§5.3). Verify the SDK, or omit it. |
| Health Connect | Closed to this audience: "Do not use Health Connect to connect to applications, services, or features that solely target children." (<https://support.google.com/googleplay/android-developer/answer/9888170>) |
| Location | "Apps that solely target children may not request location permission" (§5.3). Not needed for a schedule. |
| Exact alarms for timed reminders | `USE_EXACT_ALARM` is restricted and Google reviews it: "App must use the `USE_EXACT_ALARM` functionality only when your app's core, user facing functionality requires precisely-timed actions, such as: The app is an alarm or timer app. The app is a calendar app that shows event notifications." Otherwise prefer `SCHEDULE_EXACT_ALARM`, which needs user grant. (<https://support.google.com/googleplay/android-developer/answer/9888170>) **UNVERIFIED:** whether a visual schedule with time-of-day notifications qualifies as "a calendar app that shows event notifications" is a review decision; plan for `SCHEDULE_EXACT_ALARM`. |

---

## 6. Accessibility

> **UNVERIFIED (absence):** no Google Play **developer policy** requiring general app
> accessibility was found. The only accessibility-related policy is the **AccessibilityService
> API** policy, which constrains a specific API rather than mandating accessibility
> (<https://support.google.com/googleplay/android-developer/answer/9888170>; help article at
> <https://support.google.com/googleplay/android-developer/answer/10964491>). That policy is only
> relevant if the slot uses `AccessibilityService`, in which case: only disability-support services
> may set `isAccessibilityTool=true`, and other uses require an accessibility declaration plus
> prominent disclosure and consent. **Not planned for a schedule utility.**

What Play *does* publish for this age group is expectation, not mandate, and it reads like the
accessibility brief for this app:

> (Ages 5 & under; app may be suitable if it) "Support non-readers, or early readers, with limited
> reliance on text; Have a simple design with large iconography and clear, consistent interactive
> elements"
> (quality best practices) "Ensure that your app is usable for the intended age, including
> navigation, visuals, and text or voice-over."

Source: <https://support.google.com/googleplay/android-developer/answer/9867159>

And Google's Android material states the concrete bar:

> "For touch interfaces, we recommend that each interactive UI element have a focusable area, or
> touch target size, of at least 48dp×48dp. Larger is even better."
> "For each UI element in your app, include a description that describes the element's purpose."

Source: <https://developer.android.com/guide/topics/ui/accessibility/apps>
</br>Compose specifics (minimum touch targets, `contentDescription`, semantics):
<https://developer.android.com/develop/ui/compose/accessibility/api-defaults>

**Build-in, not retrofit:** large touch targets (≥48dp, larger for this audience), picture-first
semantics with spoken labels, TalkBack-usable names for every card, no reliance on text or on
color alone, and a predictable layout. These are cheap while scaffolding a Compose UI and
expensive afterwards. Two of them are literal Play guidance for the age group above.

> **UNVERIFIED / out of scope:** accessibility as a **legal** obligation in particular markets
> (for example European accessibility legislation) is not a Play programme policy and was not
> verified here; that is for a lawyer, not this document.

---

## 7. Content rating and declarations

### 7.1 Content rating (IARC)

> "Your app's content ratings are assigned by separate rating authorities and determined by your
> questionnaire responses."
> "You need to complete the content rating questionnaire for … New apps submitted to Google Play
> Console … All app updates where there has been a change to your content or features that would
> affect the responses to the questionnaire"
> "Misrepresentation of your app's content may result in its removal or suspension."

Source: <https://support.google.com/googleplay/android-developer/answer/9859655>

Families policy requirement 3 also makes the answers themselves an obligation: "You must
accurately answer the questions in the Play Console … including, but not limited to, providing
accurate responses about your app in the Target Audience and Content section, Data safety section,
and IARC Content Rating Questionnaire" (<https://support.google.com/googleplay/android-developer/answer/9893335>).
If the app has any social/sharing feature, that must be disclosed in the questionnaire
(same page, requirement 7).

### 7.2 The declarations this product must make

| Declaration | Where | What it must say for this product |
|---|---|---|
| **Target audience and content** | Play Console → App content | The honest user of the app, per §1. This is the load-bearing declaration. "Before filling out the Target audience and content section, you must have declared whether or not your app contains ads, and provided instructions for app access. You must also have added a privacy policy." (<https://support.google.com/googleplay/android-developer/answer/9867159>) |
| **Ads** | Play Console | Truthful "contains ads" answer; for this plan: **No ads**. |
| **Data safety** | Play Console | Complete even if nothing is collected; must match the privacy policy and every SDK (<https://support.google.com/googleplay/android-developer/answer/10787469>). |
| **Health apps declaration** | Play Console → App content | Mandatory for **all** apps; "certify that no health features are offered through the app" or declare in-scope features (§2.3). |
| **Privacy policy** | Store listing + in-app | Required even with no data collection; public, non-geofenced, no PDF (<https://support.google.com/googleplay/android-developer/answer/10144311>). |
| **Content rating questionnaire** | Play Console | Accurate for the shipped content (§7.1). |
| **Families "Committed to follow the Play Families Policy" badge** | Data safety → Security practices | Optional opt-in, available to apps with children in the audience (<https://support.google.com/googleplay/android-developer/answer/10787469>). |
| **Significant changes / age signals** | Play Console → Age signals | Only if using Play Age Signals; "Google Play doesn't mandate the use of these features. The laws impose requirements…" (<https://support.google.com/googleplay/android-developer/answer/16569691>). **Not a Play policy requirement** — do not treat it as one. |
| **AI-generated assets in the listing** | Play Console content flows | Self-declaration per asset for AI-generated/edited store listing images and videos (<https://support.google.com/googleplay/android-developer/answer/17262077>). Relevant if listing art is AI-made. |

### 7.3 AI-generated content, if any AI feature ships

> "Apps that generate content using AI must contain in-app user reporting or flagging features that
> allow users to report or flag offensive content to developers without needing to exit the app."

Source: <https://support.google.com/googleplay/android-developer/answer/13985936>

Scope, quoted from the policy explainer:

> "Text-to-text AI chatbot apps… Text-to-image, voice-to-image, and image-to-image apps that use AI
> to generate images…"
> "The policy is not intended to cover the following types of limited-scope AI apps at this time:
> … Apps that summarize non-AI generated content… Productivity apps that use AI to improve an
> existing feature, such as email apps with AI-suggested email drafts."

Source: <https://support.google.com/googleplay/android-developer/answer/14094294>

So: an AI feature that **generates** cards/images/pictograms from a prompt is in scope → it needs
in-app reporting/flagging. A feature that merely improves an existing non-generative flow may fall
outside the policy's stated scope. Either way, the child-directed SDK/API restriction in §5.3 binds
separately and is stricter.

> **UNVERIFIED:** whether a specific planned AI feature (for example photo → schedule suggestion)
> falls inside or outside the quoted "limited-scope" carve-outs is not resolvable from policy text;
> the conservative reading is that it is in scope.

### 7.4 What the planned product would **misrepresent** if declared carelessly

1. **Target audience.** Declaring "not designed for children" while the app and listing use
   child-directed imagery/terminology. Quoted risk in §1.2, and misdeclaration is itself
   actionable ("may result in removal or suspension").
2. **Health features.** Declaring "my app doesn't provide any health features" while the listing
   claims a benefit for autistic children. That is both an inaccurate Console answer and a
   misleading claim.
3. **Ads.** Declaring "no ads" is *true* for this plan — keep it true; re-declaring is required if
   that ever changes ("We strongly recommend that you let your existing users know if you change
   the target age group for your app or start using ads or in-app purchases…"
   <https://support.google.com/googleplay/android-developer/answer/9893335>).
4. **Data safety.** Declaring "no data collected" while any SDK, sync or AI call transmits photos,
   names or routine data off device.
5. **AI content.** Shipping generated content or AI-made listing art without the flagging feature
   and the per-asset AI label where applicable.
6. **Content rating.** Answering the questionnaire for a "utility" while the listing and in-app
   copy imply a clinical tool.

---

## 8. Findings the slot build should carry

1. **Families Policy Requirements bind if any declared target age group includes children, and the
   declaration is reviewable — Google can override it from the app's imagery and terminology.** A
   child-facing visual schedule will very likely be assessed as targeting children; an adult-facing
   parent/carer tool has an argument for an adult declaration only if the app *and* the listing are
   genuinely adult-facing. Decide this before scaffolding (§1).
2. **The claim boundary is the real risk.** A literal description of a picture-based "what happens
   next" reminder is permitted; any claim of improvement, reduction of behaviours, therapy or
   treatment enters the health policy, invites the "not a medical device…" disclaimer requirement
   (and its "consult a healthcare professional" reminder), and risks Health Misinformation
   enforcement (§2).
3. **No ads is safe, not required**; adding ads later would import the entire Families ads regime,
   including a certified-SDK allowlist and an AAID/`AD_ID` prohibition (§3).
4. **The consumable-pack plan is not prohibited by any Play policy found.** It must use Google Play
   Billing; if children are in the target audience it must be non-manipulative, must not blur
   virtual currency and real money, and must avoid "overly aggressive commercial tactics"; and the
   COPPA/GDPR purchase-consent question is a legal one for a human (§4).
5. **Keep the child's data on the device.** Under Data safety, local-only processing is not
   collection; any upload flips the app into collection + prominent disclosure + consent, and under
   Families policy into SDKs "approved for use in primarily child-directed services" (§5).
6. **Accessibility is an expectation here, not a Play mandate** — but Play's own age guidance asks
   for non-reader support, large iconography and simple, consistent interaction, and Google's
   Android guidance gives the concrete bar (≥48dp targets, labelled elements). Build it in (§6).
7. **Two declarations must be right or the app is at risk on its own answers**: target audience and
   health features (§7.4).

### Open items marked UNVERIFIED

- The live/current status and content of a "Designed for Families Program Requirements" page, and
  its exact relationship to the current Families Policy text (§1.1).
- Whether a Play policy names autism or makes a diagnosis word, by itself, a health declaration —
  **no such text found** (§2.3).
- Whether Play Console's Health Apps review treats a claim-free schedule utility as health-related
  (§2.3).
- Any Play rule requiring parental consent, a parental gate, or a prohibition on IAP for
  child-directed apps — **none found** (§4.2).
- The claim that the health disclaimer must sit in the *first paragraph* of the description —
  secondary source only; policy says "in their app description" (§2.2).
- Whether exact-alarm use for a visual schedule qualifies as the "calendar app that shows event
  notifications" acceptable use (§5.4).
- Whether a general Play policy accessibility requirement exists — **none found** (§6).
- Any legal obligation beyond what the policy pages state (COPPA consent mechanics, EU
  accessibility legislation, medical-device classification) — explicitly out of scope and for a
  lawyer (§4.4, §6).

### Primary sources read (2026-09-23)

| Topic | URL |
|---|---|
| Families policies (target audience, requirements, ads & monetisation) | <https://support.google.com/googleplay/android-developer/answer/9893335> |
| Target audience and app content settings | <https://support.google.com/googleplay/android-developer/answer/9867159> |
| Health Content and Services | <https://support.google.com/googleplay/android-developer/answer/16679511> |
| Health apps declaration form | <https://support.google.com/googleplay/android-developer/answer/14738291> |
| Health app categories | <https://support.google.com/googleplay/android-developer/answer/13996367> |
| Deceptive Behavior / Misleading Claims | <https://support.google.com/googleplay/android-developer/answer/9888077> |
| Ads | <https://support.google.com/googleplay/android-developer/answer/9857753> |
| Families Self-Certified Ads SDK Program | <https://support.google.com/googleplay/android-developer/answer/12918983>, <https://support.google.com/googleplay/android-developer/answer/9900633> |
| Payments policy | <https://support.google.com/googleplay/android-developer/answer/10281818> |
| User Data | <https://support.google.com/googleplay/android-developer/answer/10144311> |
| Data safety form | <https://support.google.com/googleplay/android-developer/answer/10787469> |
| Permissions and APIs that Access Sensitive Information (incl. AccessibilityService API, photo permissions, Health Connect, exact alarms) | <https://support.google.com/googleplay/android-developer/answer/9888170> |
| Content rating requirements | <https://support.google.com/googleplay/android-developer/answer/9859655> |
| AI-Generated Content policy (+ explainer) | <https://support.google.com/googleplay/android-developer/answer/13985936>, <https://support.google.com/googleplay/android-developer/answer/14094294> |
| Declaring AI-generated content in Play Console | <https://support.google.com/googleplay/android-developer/answer/17262077> |
| Designed for Families DDA addendum | <https://play.google/intl/ALL_us/families/developer-distribution-agreement-addendum.html> |
| Teacher Approved criteria | <https://play.google.com/console/about/programs/teacherapproved/> |
| Purchase approvals (parent-side) | <https://support.google.com/families/answer/7039872>, <https://support.google.com/googleplay/answer/6294544>, <https://support.google.com/googleplay/answer/11174377> |
| Consolidated Developer Programme Policy (mirror used for cross-checking Families wording) | <https://support.google.com/googleplay/android-developer/answer/17517561> |
| Android accessibility guidance | <https://developer.android.com/guide/topics/ui/accessibility/apps>, <https://developer.android.com/develop/ui/compose/accessibility/api-defaults> |
| Age Signals (optional, law-driven) | <https://support.google.com/googleplay/android-developer/answer/16569691>, <https://developer.android.com/google/play/age-signals/v3/use-age-signals-api> |
