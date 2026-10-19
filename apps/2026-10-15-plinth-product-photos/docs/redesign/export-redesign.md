# Export redesign: finished seller handoff

Export now opens with an explicit ready-to-deliver summary: marketplace destination count, exported-image count, and the formats actually present in the generated packs. Each destination is a distinct card with its file count, ready state, and a direct ZIP action; the existing save-all action remains available for multiple destinations.

Inside each destination, per-file dimensions, format, quality and byte size are kept together, while the processing/provenance label is surfaced as a separate visual tag. The rules source and manifest explanation sit in a tinted processing-record panel, so provenance is visible without suggesting the manifest contains more than its explicit rule overrides. The empty state remains separate and does not claim readiness.

The screen keeps its existing share, gallery-save, document-picker and ZIP delivery behavior. It uses the existing PlinthScaffold for system insets and Compose Material surfaces; no preview images or dependency changes were needed. English and Russian resources carry the new UI copy.

**Evidence status:** implementation structure and localized resources are inspectable in `app/src/main/java/com/gexetr/plinthproductphotos/ui/export/ExportScreen.kt` and `app/src/main/res/values*/strings_export.xml`. No Gradle build or on-device review was run for this bounded worker task; visual fit and compilation remain **UNVERIFIED**.
