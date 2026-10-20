# Studio UX (W78)

The live Studio route keeps the user's selected product photo visible while they
choose an available edit. Choosing an edit reveals a short, optional instruction
field and one **Generate · N credits** action; a blank instruction uses the
standard edit. The selected photo and operation remain visible while generation
is in progress, represented by one customer-facing spinner and message.

Credit details stay proportional to the decision: the action states its price,
and the shortfall dialog appears only after the user presses Generate with an
insufficient balance. A successful result shows the original and generated
image side by side. Internal request/job identifiers and server lifecycle names
are not part of the consumer UI.

The Compose behavior is covered by
`apps/2026-10-15-plinth-product-photos/app/src/androidTest/java/com/gexetr/plinthproductphotos/w78/StudioWorkflowTest.kt`:
optional instruction, price on the action, insufficient-balance timing, one
waiting indicator, tool selection without generation, and before/after layout.
