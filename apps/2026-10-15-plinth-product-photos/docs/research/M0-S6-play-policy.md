# Plinth M0 S6 — Play policy and metadata audit

Read-only design review, observed 2026-10-14. No Plinth app exists yet; no compliance verdict is made.

## Findings

1. Before real-photo transmission, select and verify the actual model route and its terms, payload/media expiration settings, training use, and processor/controller status. The design's proposed 1-hour upload and 24-hour result retention do not establish provider-side retention. fal documents default request input/output storage for 30 days; `X-Fal-Store-IO: 0` does not itself remove CDN media. Terms have an Excluded Models exception to the Client Content training restriction; the selected route/model status is unresolved. Data Safety must declare photos as collected because they are transmitted off-device. Whether processing constitutes sharing depends on the actual provider arrangement.
2. The in-app report action for offensive, unsafe, wrong-product, or rights-related generated output is required behavior in the spec, but implementation and moderation handling remain unverified. Test the delivered app and server path before claiming this is satisfied.
3. Target audience and content rating cannot be set from this design review. fal's terms state an 18-or-age-of-majority requirement for customers and end users of a Customer Solution; verify applicability to the selected service. The actual intended audience must be truthfully declared, and IARC must be completed against the actual app.
4. Keep O-14 virtual model disabled pending a selected route and safeguards. The audit does not clear it.
5. IPTC DigitalSourceType vocabulary identifiers read:
   - `http://cv.iptc.org/newscodes/digitalsourcetype/trainedAlgorithmicMedia`
   - `http://cv.iptc.org/newscodes/digitalsourcetype/compositeWithTrainedAlgorithmicMedia`
   - `http://cv.iptc.org/newscodes/digitalsourcetype/algorithmicallyEnhanced`
   Map by what actually happened to the output; do not use a blanket Enhanced mapping.
6. No C2PA conformance claim is supportable. The design excludes C2PA from v1; revisit only if signing/validation enters scope.

## Claim ledger and sources

| Claim | Label | Evidence |
|---|---|---|
| M0 S6 requires AI policy, Data Safety/provider processing, audience/rating, provider terms, IPTC, C2PA, and O-14 verdict. | ON RECORD | `openspec/changes/app-seller-photo-studio/annex/M-spikes-and-reads.md:73-80` |
| Plinth's proposed server retention is 1 hour for uploads and 24 hours for results; actual provider retention remains separate. | ON RECORD / UNVERIFIED provider configuration | `openspec/changes/app-seller-photo-studio/annex/H-compliance-and-provenance.md:20-38`; https://fal.ai/docs/documentation/model-apis/media-expiration |
| fal terms distinguish Excluded Models, third-party model API transfer, and de-identified service improvement. | ON RECORD | https://fal.ai/legal/api-services ; https://fal.ai/legal/data-processing-addendum (updated 2026-07-31) |
| fal terms state minimum age 18 or age of majority for a Customer Solution's users. | ON RECORD | https://fal.ai/legal/terms-of-service (updated 2026-09-08) |
| Google requires reporting/flagging for offensive AI-generated content; implementation is unverified. | ON RECORD / UNVERIFIED implementation | https://support.google.com/googleplay/android-developer/answer/14094294?hl=en-GB ; https://support.google.com/googleplay/android-developer/answer/16353813?hl=en-GB |
| Data Safety treats off-device photo transmission as collection; sharing needs actual processor facts. | ON RECORD / UNVERIFIED sharing classification | https://support.google.com/googleplay/android-developer/answer/10787469?hl=en |
| Exact IPTC terms are as listed; output-specific mapping is an implementation decision based on the content type. | MEASURED / INFERRED | https://cv.iptc.org/newscodes/digitalsourcetype ; https://iptc.org/news/new-digital-source-type-term-added-to-support-inpainting-outpainting-in-generative-ai/ ; `annex/H-compliance-and-provenance.md:44-54` |
| No Plinth implementation or C2PA state was available to audit; O-14 remains gated. | MEASURED / ON RECORD | No app directory; `annex/E-pipeline-and-operations.md:151-158`; `annex/H-compliance-and-provenance.md:59-74` |
| Current IARC rating, actual Data Safety answers, provider-side deletion, and implemented report path remain unknown. | UNVERIFIED | Requires built app, selected route/configuration, and Play Console declarations |

Policy references also checked 2026-10-14: https://support.google.com/googleplay/android-developer/answer/9867159?hl=en ; https://support.google.com/googleplay/android-developer/answer/9898843?hl=en ; https://android-developers.googleblog.com/2024/06/enabling-safe-ai-experiences.html ; https://c2pa.org/conformance/
