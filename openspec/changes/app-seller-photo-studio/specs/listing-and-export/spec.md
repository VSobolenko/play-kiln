## Purpose

Gets the seller's finished work out of the app in a form a marketplace accepts, together with the listing
text, so the job ends with something listed and not with a picture on the phone. Detail: screens S-11,
S-12; export and XMP in `annex/H-compliance-and-provenance.md`; slide series in capability `slide-kit`.

## ADDED Requirements

### Requirement: Export formats and parameters

The app SHALL export in JPEG, PNG and WebP with a chosen quality and long-side size, in the sRGB colour
profile, and SHALL name files from the item's SKU and photo order. Export SHALL be to the gallery, through
the system share sheet, or as a ZIP file saved to a location the seller chooses.

#### Scenario: Files are named by SKU

- **WHEN** an item with SKU A-014 and three photos is exported
- **THEN** the files are named from A-014 in photo order

#### Scenario: Chosen size is honoured

- **WHEN** the seller exports with a long side of 1600 px
- **THEN** the output's longer side is 1600 px

### Requirement: Pack per marketplace

The app SHALL export all selected items for a marketplace as one pack arranged as that marketplace expects
(one folder per item, ordered photos, listing text file), and SHALL run the rules linter on the whole pack
before export and show the failures.

#### Scenario: A pack carries photos and text

- **WHEN** the seller exports three items for a marketplace
- **THEN** the pack holds a folder per item with ordered photos and a text file with the listing

#### Scenario: Pack is linted

- **WHEN** one photo in the pack fails a rule
- **THEN** the seller is shown that failure before the pack is written and can fix or continue

### Requirement: Listing text in six languages

The app SHALL produce a title and description for an item in the language the seller chooses, independent
of the interface language, from the item's photos and fields, within the title and description limits of
the selected marketplace. The text SHALL be editable and SHALL be copyable to the clipboard and shareable.

#### Scenario: Text language is independent of the interface

- **WHEN** the interface is German and the seller chooses French for the listing text
- **THEN** the produced text is in French

#### Scenario: Marketplace limits are enforced

- **WHEN** the selected marketplace limits titles to a stated length
- **THEN** the produced title is within that length

#### Scenario: Copy and share

- **WHEN** the seller taps copy and then share on the listing text
- **THEN** the clipboard holds the text and the system share sheet carries the same text

### Requirement: Exported files declare AI processing

When an exported file was produced in Enhanced mode its metadata SHALL declare that it was processed
algorithmically, and the export screen SHALL tell the seller which files carry that declaration.

#### Scenario: Declaration is listed

- **WHEN** the seller exports a pack with one Enhanced file
- **THEN** the export screen names that file as AI-processed and its metadata carries the declaration

### Requirement: Settings change behaviour

The settings SHALL include the default marketplaces, the default Look, the default mode, the export
parameters, the listing-text language, the credit balance and purchases, data deletion and the privacy
policy, and each SHALL change the behaviour of the app it describes.

#### Scenario: Default mode is applied

- **WHEN** the seller sets the default mode to Original and starts a new project
- **THEN** the project starts in Original mode

#### Scenario: Default export size is applied

- **WHEN** the seller sets a default long side and exports
- **THEN** the export dialog starts with that value

### Requirement: Interface languages

The app SHALL ship its interface in English, Russian, German, French, Spanish and Brazilian Portuguese,
SHALL lay out correctly in the longest language, and SHALL take every user-visible string from resources.

#### Scenario: Longest language does not clip

- **WHEN** the interface is set to the language with the longest labels at a large font size
- **THEN** no label or button text is clipped or overlaps another control
