# W11: shell syntax and create-to-result handoff

Implemented in `app/src/main/java/com/gexetr/plinthproductphotos/ui/PlayKilnApp.kt`.
Compilation is **UNVERIFIED**. No Gradle, build, lint, JUnit, installation, signing,
release approval, commit or git-state-changing command was run.

## Defect actually observed

The original Coach tail had equal brace/parenthesis totals but invalid nesting.
The scratch checker reported unexpected `}` tokens at original lines 619 and 620,
while `NavDisplay(` from original line 284 was still open, and unclosed outer
scopes from original lines 191 and 255. A count alone would have missed it.

Before, verbatim from the original region:

```kotlin
                            onUsePhoto = { handoff: CoachCaptureHandoff ->
                                backStack.push(AutoCardDestination(handoff.item.id))
                                    },
                                )
                            }
                        }
                    }
                }
            },
        )
    }
}
```

After (`PlayKilnApp.kt:631-643`):

```kotlin
                            onUsePhoto = { handoff: CoachCaptureHandoff ->
                                backStack.push(AutoCardDestination(handoff.item.id))
                            },
                        )
                    }
                }
                    },
                )
            }
        }
    }
}
```

The Coach call now closes its lambda, call, loaded-content lambda and entry in
the same order as the neighboring AutoCard entry (`PlayKilnApp.kt:373-397`).
The following `},` closes `entryProvider`, then `)` closes `NavDisplay`, then
the Row, outer scaffold content, Surface and app scopes close. All Coach
arguments and its photo handoff remain. This required correcting the second
shell-boundary imbalance too: two braces had been placed before the NavDisplay
closing parenthesis rather than after it. No navigation was restructured.
The Wallet sibling remains an ordinary call inside its entry
(`PlayKilnApp.kt:611-620`).

## Three integration edits

1. `PlayKilnApp.kt:383-388`: `onChangePhoto` pops AutoCard and pushes Import only
   when Import is not already on top. The existing result storage and explicit
   result navigation remain at lines 389-392.
2. `PlayKilnApp.kt:663-679`: `WithLoadedDestination` accepts optional
   `loadingContent` immediately before its trailing `content`, keeping the
   CircularProgressIndicator default for other callers. Loading invokes that
   callback. The AutoCard call supplies `FormingCardPreview` inside
   `PlinthScaffold`, using the loaded source's width divided by its height
   (`PlayKilnApp.kt:362-373`), not a guessed canvas.
3. `PlayKilnApp.kt:804-984`: the result screen loads the real source and decodes
   both source and result through the existing suspend preview extension.
   Bitmap/failure state and effects are keyed by the pixel buffer and
   `saveAttempt`; cancellation is rethrown. Decode errors use the existing
   save-error message and expose Retry, including when the stored output is
   already Ready. Retry also re-executes display decoding. Original/card chips
   use the real decoded source and result; the original chip is disabled until
   its bitmap exists. The aspect-bounded preview uses result geometry, then
   source geometry, and is not created when neither exists. Loading/Saving and
   pending decoding animate; Missing/Failed/decode failures stop the sweep and
   show the existing failure messages. No raw Image or bare loading spinner
   remains in this result screen. The Ready Share/Save-to-folder row is now in
   `PlinthScaffold.bottomBar`, inside a Surface with 20 dp content padding
   (`PlayKilnApp.kt:860-913`). Execution handlers are unchanged. No navigation-bar
   or IME padding was added.

The existing automatic `saveAutoCard` remains the only library-save call.
Missing/regenerate and save-failure/retry behavior remains; no completion effect
navigates, and no second library Save, Export pack or invented charge receipt
was added.

## Declarations read before integration

All paths below are relative to this slot.

| Symbol | Actual declaration read |
|---|---|
| `AutoCardRoute`, including defaulted `onChangePhoto` | `app/src/main/java/com/gexetr/plinthproductphotos/ui/autocard/AutoCardScreen.kt:79-92` |
| `FormingCardPreview(bitmap: Bitmap?, aspectRatio: Float, busy: Boolean, message: String, failed: Boolean, modifier: Modifier = Modifier)` | `app/src/main/java/com/gexetr/plinthproductphotos/ui/autocard/FormingCardPreview.kt:39-46` |
| `PixelBuffer.autoCardPreviewBitmap(): Bitmap` | `app/src/main/java/com/gexetr/plinthproductphotos/ui/autocard/AutoCardImages.kt:39-44` |
| Preview sizing bound (1024 maximum dimension before downscaling) | `app/src/main/java/com/gexetr/plinthproductphotos/ui/autocard/AutoCardImages.kt:8-13` |
| Off-main dispatcher and cancellation-aware serialized image work | `app/src/main/java/com/gexetr/plinthproductphotos/ui/autocard/AutoCardImageWork.kt:9-18` |
| `PlinthScaffold` content/bottomBar API and owned insets | `app/src/main/java/com/gexetr/plinthproductphotos/ui/components/PlinthScaffold.kt:18-49` |
| Existing source/card comparison labels | `app/src/main/res/values/strings_import_smartcard.xml:17-18`, `app/src/main/res/values-ru/strings_import_smartcard.xml:17-18` |

## Observed checks

Commands ran from repository root, without Gradle.

- `python .kiln\scratch\w11-shell-structure.py`: exit 0. Nested delimiters,
  ignoring comments and string literals: 529 opening/closing parentheses,
  249 opening/closing braces, 3 opening/closing brackets. Also checked the exact
  Coach/shell tail, Import replacement, original aspect, keyed decoding,
  cancellation propagation, retry, comparison, one automatic library-save call,
  the dock, and every shell string reference in both `values` and `values-ru`.
  Share and Save-to-folder execution bodies match the existing git index with
  whitespace ignored. This is a textual structural check, not a Kotlin parser
  or compilation.
- Read the edited AutoCard, Coach/shell, helper and entire result-screen regions
  line by line and compared the Coach entry to its AutoCard/Wallet siblings.
- `git --no-pager diff --check -- apps/2026-10-15-plinth-product-photos/app/src/main/java/com/gexetr/plinthproductphotos/ui/PlayKilnApp.kt`:
  exit 0, no output.
- `node scripts\verify.mjs --slot plinth-product-photos`: exit 1, **122 PASS,
  3 UNVERIFIED, 4 SKIP, 1 FAIL**, 23463 ms, **0 subprocesses**. Only failure:
  `slot.device` records an older build digest. `build` explicitly remains
  UNVERIFIED because `--build` was not passed; pack inventory/media also remain
  UNVERIFIED because no assets exist.
- The same source-only command before edits exited 1: **120 PASS,
  3 UNVERIFIED, 4 SKIP, 3 FAIL**, 17172 ms, 0 subprocesses. It already failed
  `slot.device`, `uiux.text-layout` and `uiux.a11y`. The latter two passed in the
  later reading while other workers were editing their files; W11 did not edit
  those files or claim those repairs. No new source/resource finding appeared.

Raw readings: `.kiln/scratch/w11-verify-before.txt` and
`.kiln/scratch/w11-verify-after.txt`. These are local scratch evidence, not
release evidence.

## Deliberately left and follow-up risk

Only the owned Kotlin host, this report and `.kiln/scratch/` were written.
Theme, Studio, navigation/cost components, Home, Wallet, AutoCard implementation,
resources and device/approval records were not edited.

**Pre-existing navigation defect, not repaired:** `PlayKilnApp.kt:232` executes
`while (backStack.isNotEmpty()) backStack.pop()`, but the helper at
`PlayKilnApp.kt:1114-1116` removes an item only when `size > 1`. With a nonempty
stack, `selectSection` cannot terminate. This is inferred directly from the
observed source, not reproduced on a phone. It can block section/create actions
at runtime. It was left because W11 explicitly forbids changing navigation
behavior beyond the documented integration; the orchestrator must assign it
before device acceptance.

The orchestrator must compile and exercise portrait/landscape previews,
comparison, failure/retry, delivery actions, large fonts and inset clearance.
This report does not claim runtime success.

## Claim ledger

| CLAIM | LABEL | EVIDENCE |
|---|---|---|
| The edited file has balanced, correctly nested delimiters | MEASURED | `python .kiln\scratch\w11-shell-structure.py`, exit 0; counts above |
| Coach and shell closing scopes match the expected structural shape | MEASURED | `PlayKilnApp.kt:621-643`; scratch exact-tail assertion |
| Import replacement and source-aspect loading handoff are wired | MEASURED | `PlayKilnApp.kt:362-392`, `PlayKilnApp.kt:663-679` |
| Result/source decoding, comparison and dock are present in source | MEASURED | `PlayKilnApp.kt:804-984`; scratch handoff assertions |
| Share/Save execution bodies were preserved | MEASURED | Scratch checker compares both handlers against the existing index using read-only `git show :<path>` |
| No new source/resource finding appeared in source-only verification | MEASURED | `node scripts\verify.mjs --slot plinth-product-photos`; before/after logs above; after has only stale `slot.device` failure |
| Existing section-selection loop cannot terminate on a nonempty stack | INFERRED | `PlayKilnApp.kt:232` loops until empty, but `PlayKilnApp.kt:1114-1116` preserves the last entry |
| The app compiles after this repair | UNVERIFIED | No compile run; only the orchestrator may run the authorized build |
| Device rendering and delivery work end to end | UNVERIFIED | Requires orchestrator build and fresh attached-device journey; current `slot.device` record is stale |
