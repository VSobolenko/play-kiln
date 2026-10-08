# Kerfwise: Cut Planner — woodworking project suite

## Decision

**INFERRED (market evidence below):** Kerfwise is a paid, fully on-device woodworking project suite that turns a finished furniture size into grain-aware parts, optimized stock layouts, joint settings, sensor-checked bevels, and a shareable cut packet.

**INFERRED (economics plan):** The paying user is a hobby woodworker or small-shop cabinet maker; they buy one permanent $9.99 unlock after a free small-project preview and before another export or the full template library.

**MEASURED (.kiln/scratch/woodwork-enrich-2026-10-08.md):** The highest-install directly relevant cut-list incumbent is CutList Optimizer at 1M+ installs and 9,530 displayed reviews, free with ads and IAP.

**MEASURED (.kiln/scratch/woodwork-enrich-2026-10-08.md):** The highest-install adjacent project-planning incumbent in the enriched slice is Moblo - 3D Furniture Design at 1M+ installs and 6,720 displayed reviews, free with IAP.

**MEASURED (.kiln/scratch/woodwork-enrich-2026-10-08.md):** The highest-priced observed paid comparator is Miter Angle Calculator at $8.99 with 10+ installs; Miter Angle: Angle Calculator is $2.99 with 1K+ installs.

**INFERRED (the paid-comparator gap):** $9.99 is a premium single-unlock test justified by an integrated six-feature workflow, not proven willingness to pay, because the high-install leaders expose no listing price and the observed paid apps are small.

**UNVERIFIED (conversion has not been measured):** At a modeled $6.80 net per sale, the required target is $1,000 ÷ $6.80 = 147 sales/month; this is reachable only if the 1M+-install adjacent shelf produces enough qualified discovery and conversion, which this recon cannot establish.

**INFERRED (ceiling arithmetic):** At 1% of the 1M+ lower-bound CutList Optimizer install base paying $6.80 net, annual net would be at least $68,000, or about $5,667/month; this is a theoretical addressable-install ceiling, not a sales forecast.

## Coherent job and feature areas

**INFERRED (job flow):** The coherent job is design → parts → cut layout → joint setup → workshop verification → handoff, rather than a bag of unrelated calculators.

1. **INFERRED (product design):** A project planner derives named parts from supported furniture templates using finished dimensions, stock thickness, kerf, and grain direction.
2. **INFERRED (product design):** A cut-list optimiser packs those parts onto the maker's real sheet and board sizes, labels a cutting diagram, and shows waste and usable offcuts.
3. **INFERRED (product design):** A joinery and angle solver gives mitre, bevel, compound-angle, and dado-placement settings tied to the selected part and reference edge.
4. **INFERRED (product design):** A material estimator totals sheet area, rough-hardwood board-feet, waste, and entered material cost from the same derived parts.
5. **INFERRED (product design):** A digital bevel gauge compares a planned angle with the phone's rotation-vector reading as an indicative workshop check.
6. **INFERRED (product design):** A PDF/share export carries the accepted parts, diagram, material estimate, and joint settings outside the app.

**MEASURED (.kiln/scratch/woodwork-complaints-2026-10-08.md):** A Cutting Optimizer review complains that it cannot define two-dimensional sheet size or scraps, while woodworking-plan reviews complain of “single pictures” with “little reference or detail”; the proposed workflow answers those discrete failures rather than copying a list-only app.

## Wedge

**INFERRED (functional comparison):** Kerfwise is the woodworking app that converts finished dimensions into grain-aware parts and real-stock cut diagrams, then carries the exact joint angle and a phone-checked bevel into one exportable workshop packet.

**ON RECORD (brief scope):** This differs from `mesh-mend`, which repairs imported STL/3D-print mesh files; Kerfwise computes a woodworking project's physical parts and cuts, and creates no 3D-file repair workflow.

## Market evidence

**MEASURED (.kiln/scratch/woodwork-market-2026-10-08.json):** The twelve requested woodworking queries yielded three candidate screens, two mixed screens, one fortress, and six dead zones; the candidate result is a screen, not proof by itself.

**MEASURED (.kiln/scratch/woodwork-titles-2026-10-08.md):** Hand classification found 21/30 woodworking/furniture cards for “woodworking project planner” versus the relevance guard's partial 16/30, and 13/13 cut-related cards for “cut list optimizer” versus its partial 7/13 guard.

**MEASURED (.kiln/scratch/woodwork-titles-2026-10-08.md):** Hand classification found 28/30 digital-angle cards for “digital angle finder app,” close to the guard's 26/30 on-topic count.

**MEASURED (.kiln/scratch/woodwork-enrich-2026-10-08.md):** Six of eight enriched cut-list listings declared ads and six declared IAP; every enriched listing was free at install.

**MEASURED (.kiln/scratch/woodwork-complaints-2026-10-08.md):** In the six-app cut-list complaint sample, 7 of 15 critical reviews mentioned ads and 2 mentioned missing capability.

**MEASURED (.kiln/scratch/woodwork-complaints-2026-10-08.md):** One 1-star angle-finder review says, “Shows ads even before you are able to use it once.”

**MEASURED (.kiln/scratch/woodwork-complaints-2026-10-08.md):** One 3-star Miter Angle review says, “Almost great! If the goal of the app is simplicity, it would be nice to be able to type the length in just inches.”

## Monetization choice

**INFERRED (economics plan):** The selected archetype is `purchase_led_single_unlock`, requiring Play Billing and deliberately excluding ads.

**MEASURED (.kiln/scratch/woodwork-complaints-2026-10-08.md):** Ads are rejected because the sampled cut-list and angle slices contain repeated ad complaints.

**INFERRED (durable local job):** Repeat-sale purchases are rejected because a local project-solving capability is not an honest consumable.

**MEASURED (.kiln/scratch/woodwork-enrich-2026-10-08.md):** Upfront purchase-only is rejected because the observed cut-list funnel is free-to-install with IAP, including the 1M+ leader.

**ON RECORD (owner instruction):** Subscriptions are banned.

## Promise contract

**ON RECORD (docs/ideation/plans/kerfwise-cut-planner.differentiators.json):** The contract carries five deferred promises, three core promises, all job stages, and the `data_out` connective claim `exports-cut-packet`.

## Device capabilities

- camera: not used — **INFERRED:** a finished-size-to-cut-packet workflow does not need a photograph.
- photo picker / gallery: not used — **INFERRED:** the first version derives projects from dimensions, not images.
- media playback or capture: not used — **INFERRED:** no sound or video contributes to the woodworking computation.
- microphone: not used — **INFERRED:** no audio input contributes to the woodworking computation.
- location: not used — **INFERRED:** workshop location does not alter a local cut plan.
- motion sensors: used — **INFERRED:** `Sensor.TYPE_ROTATION_VECTOR` or `TYPE_GAME_ROTATION_VECTOR` compares a planned bevel with the phone's orientation; without it the app loses its digital bevel-check area; manifest permission: none.
- contacts / calendar: not used — **INFERRED:** the job does not require another person's identity or calendar event.
- notifications / reminder: used — **INFERRED:** `NotificationManagerCompat` can issue a user-enabled local resume-cut-session reminder; without it the maker loses an optional workshop handoff; manifest permission: `POST_NOTIFICATIONS` on Android 13+.
- home-screen widget: not used — **INFERRED:** the live project packet is not useful as a static home-screen control in the first release.
- share / export: used — **INFERRED:** `PdfDocument`, `FileProvider`, and `Intent.ACTION_SEND` deliver the cut packet; without them the finished plan remains trapped on the phone; manifest permission: none.
- Bluetooth / USB: not used — **INFERRED:** the first release does not control a saw, measure, or CNC device.
- files / SAF: used — **INFERRED:** `Intent.ACTION_CREATE_DOCUMENT` saves the PDF to user-selected storage; without it the maker cannot keep the cut packet; manifest permission: none.

## Implementation envelope

**INFERRED (feature count and local algorithms):** The honest build estimate is 32 agent hours for templates, deterministic packing, joint solvers, material estimate, sensor comparison, PDF/share output, Billing, tests, and accessible UI.

**INFERRED (technical boundary):** The project needs no server, account, paid API, keyed client API, camera, microphone, or location service; its optimisation and geometry run locally.

**INFERRED (permission minimisation):** The only optional runtime permission is `POST_NOTIFICATIONS`, requested only after the user enables a reminder; motion sensors, SAF, PDF creation, sharing, and Billing need no runtime permission.

**INFERRED (visual direction):** Use pale birch stock, graphite tool marks, and a restrained safety-orange kerf line; cutting diagrams should use a workshop-drafting grid and animate the selected part from plan row to stock rectangle.

## Kill criteria and unknowns

**ON RECORD (economics plan):** At 90 days, kill or revise if installs are below 1,000, recognized net revenue is below $1,000, or fewer than 147 modeled net-equivalent unlocks have occurred.

**UNVERIFIED (decisive future evidence):** Actual IAP prices and conversion rates for CutList Optimizer and Moblo remain unknown.

**UNVERIFIED (decisive future evidence):** Trademark availability of Kerfwise in class 9, exact-title collision, and Russian searchability require dedicated screens.

**UNVERIFIED (decisive future evidence):** Real-device performance and diagram readability on large cabinet plans require benchmark fixtures and usability exercise.

## Naming

**ON RECORD (docs/ideation/plans/kerfwise-cut-planner.naming.json):** `Kerfwise: Cut Planner` scores 82.5/100, has five candidates spanning five archetypes, and records `CutList Optimizer` as the rejected near-incumbent title.

## Reproduction

**MEASURED (commands run 2026-10-08):** `node scripts/scan-market.mjs` ran all twelve requested queries with concurrency 3 and wrote `.kiln/scratch/woodwork-market-2026-10-08.json`.

**MEASURED (commands run 2026-10-08):** `node scripts/scan-titles.mjs`, `node scripts/enrich-market.mjs`, and `node scripts/scan-complaints.mjs` wrote the three named scratch reports cited above.
