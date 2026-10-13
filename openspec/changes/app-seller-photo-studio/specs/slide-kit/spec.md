## Purpose

Produces an infographic slide series for a listing: one saved style across every slide, text chosen by
the seller and typeset on the device (never by a generative model), checked against the marketplace's
prohibited-text categories before render. Detail: `annex/I-slide-kit.md` (F-21…F-25).

## ADDED Requirements

### Requirement: Slide text is typeset on the device

The app SHALL render every text block on a slide with fonts bundled in the application and SHALL NOT
ask a generative model to draw letters, digits or punctuation on a slide (`D13`, annex I).

#### Scenario: No generative text on a slide

- **WHEN** the seller exports a slide series
- **THEN** every glyph on every slide was drawn by the device typesetter and no generative text endpoint was called for the slide

### Requirement: One style across the series

A look SHALL bind the slide style (colours, fonts, plate shapes, spacing and background asset). Changing
the style SHALL update every slide of the project. The background asset SHALL be the same file across the
series and SHALL NOT be regenerated per slide (F-23, WildCard's documented gap).

#### Scenario: Background is identical across slides

- **WHEN** a series of three slides is exported from one look
- **THEN** the background image bytes of the three slides are identical

### Requirement: Seller-controlled text with live limits

The app SHALL let the seller edit each text slot (title, specs, badge, caption), SHALL show a live
character count where the marketplace declares a limit, and SHALL refuse to render a slide whose text
matches a prohibited category of the selected marketplace (annex L rule L11, annex I).

#### Scenario: Prohibited price text is refused

- **WHEN** the selected marketplace prohibits price and discount text and the seller types a price with a currency symbol into a slide
- **THEN** the block is marked with the category and the slide does not render until the text is changed

#### Scenario: Empty prohibited list is honest

- **WHEN** the selected marketplace has an empty prohibited-text list
- **THEN** the app shows that the marketplace's slide-text rules were not read and raises no matcher finding

### Requirement: Contrast and preview legibility

The app SHALL check text colour against its plate or background at WCAG ratios (default ≥ 4.5 small /
≥ 3 large, annex L L12) and SHALL show a live preview of the slide at the catalogue thumbnail width
(246 px) so the seller can see whether the title stays legible (annex I defaults).

#### Scenario: Low contrast is flagged

- **WHEN** the text colour against its plate scores below the threshold
- **THEN** a contrast finding is shown before render

### Requirement: Templates and MAIN-safe slide

The app SHALL ship a closed set of slide templates (annex I: hero, spec3, size, detail, kit, use,
variants, clean) and SHALL include a clean template that puts only the item on the canvas for
marketplaces whose main photo forbids overlays.

#### Scenario: Clean template has no text

- **WHEN** the seller chooses the clean template
- **THEN** the rendered slide carries no text block and no overlay shape
