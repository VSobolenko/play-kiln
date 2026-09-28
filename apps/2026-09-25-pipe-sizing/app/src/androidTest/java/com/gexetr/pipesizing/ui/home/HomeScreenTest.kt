package com.gexetr.pipesizing.ui.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.gexetr.pipesizing.MainActivity
import com.gexetr.pipesizing.R
import com.gexetr.pipesizing.monetization.MonetizationConfig
import com.gexetr.pipesizing.sizing.SizingEvaluation
import com.gexetr.pipesizing.sizing.SizingMethod
import com.gexetr.pipesizing.sizing.TableApplicability
import com.gexetr.pipesizing.sizing.TableCitation
import com.gexetr.pipesizing.sizing.TableRow
import com.gexetr.pipesizing.sizing.USER_RECORDED_MARKER
import com.gexetr.pipesizing.sizing.UserCapacityTable
import com.gexetr.pipesizing.ui.tables.TablesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun sizingFormAndRefusalAreVisible() {
        val title = composeRule.activity.getString(R.string.app_name)
        val primaryAction = composeRule.activity.getString(R.string.evaluate_action)

        // The app name now lives in the app-level top bar only, and the calculator is the start tab.
        composeRule.onAllNodesWithText(title)[0].assertIsDisplayed()
        // The form is a scrolling column and the primary action sits below the fold on a phone now
        // that the method control lists four methods, so reaching it means scrolling to it - which is
        // what the user does - rather than it being on screen the moment the tab opens.
        composeRule.onNodeWithText(primaryAction).performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Enter material or system.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theFourthTabOpensTheRecordedTablesSurface() {
        // Step 5.1 — the table surface is reachable in the shipped app, not only in a test that
        // renders it directly. Four tabs, every label on screen, and choosing the new one lands on
        // the table surface the calculator returns values from.
        val activity = composeRule.activity
        listOf(R.string.nav_calculator, R.string.nav_cases, R.string.nav_guide, R.string.nav_tables)
            .forEach { labelRes ->
                composeRule.onAllNodesWithText(activity.getString(labelRes))[0].assertIsDisplayed()
            }

        composeRule.onAllNodesWithText(activity.getString(R.string.nav_tables))[0].performClick()
        composeRule.onNodeWithText(activity.getString(R.string.tables_title)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(activity.getString(R.string.tables_new)).performScrollTo().assertIsDisplayed()
    }
}

/**
 * Step 4.7 — the returned-table journey on a device: what the user sees when the app hands back a
 * value out of their own recorded table.
 *
 * The screen is content-only (the app's single `Scaffold` lives in `ui/PlayKilnApp.kt`), so this
 * suite renders it over a real `HomeViewModel`. The method choice and the table picker are driven
 * through the controls the user actually taps; the numeric readings are set through the same
 * `HomeEvent.FieldChanged` path the fields use, because `HomeScreen` tags its structural controls and
 * not each field, and a test that located a field by its English label would break the first time a
 * translation moved the words.
 *
 * Every value below is invented synthetic filler ("synthetic table", "synthetic-size-a", 1.0, 2.0).
 * The app supplies no table content and holds no capacity value of its own; the declared gas
 * specific gravity of 21.5 is no gas's and is read from no code or standard.
 */
class HomeScreenTableResultTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aReturnedValueShowsItsRowItsSourceLabelAndTheDisclaimer() {
        val table = syntheticTable()
        val viewModel = viewModel(TablesRepository.InMemory(listOf(table)))
        composeRule.setContent { HomeSurface(viewModel) }
        // The premise of everything below: the state holder can see the recorded table.
        assertEquals(1, viewModel.uiState.value.availableTables.size)

        // The user chooses the method and the table through the controls, not through the state.
        composeRule.onNodeWithTag(HomeScreenTags.method(SizingMethod.RETURN_FROM_USER_TABLE)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(HomeScreenTags.tableOption(table.id)).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(HomeScreenTags.TABLE_NOMINAL_SIZE).performScrollTo().performTextInput("synthetic-size-a")
        composeRule.waitForIdle()

        enterReadings(viewModel, load = "1", length = "2")
        composeRule.onNodeWithText(text(R.string.evaluate_action)).performScrollTo().performClick()
        composeRule.waitForIdle()

        // The app returned a value from the recorded table, and says where it came from.
        val evaluation = viewModel.uiState.value.evaluation
        assertTrue("expected a returned value, saw $evaluation", evaluation is SizingEvaluation.ReturnedFromUserTable)
        composeRule.onNodeWithText(text(R.string.returned_table_result_title)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(
            text(R.string.returned_table_row, "synthetic-size-a", "synthetic-size-a"),
        ).performScrollTo().assertIsDisplayed()
        // The substitution is shown, because the case's own length is not one of the recorded lengths.
        composeRule.onNodeWithText(
            text(R.string.returned_table_length_substitution_imperial, "2.00", "3.00"),
        ).performScrollTo().assertIsDisplayed()
        assertSourceLabelAndDisclaimerAreVisible()
    }

    /**
     * Step 5.1 — the Report action is reachable for a value returned from the user's recorded table,
     * and what it produces is that value's own report.
     *
     * This is the sensor for the defect §4.8 recorded: the returned-table report was implemented and
     * tested while no screen could reach it, because the Report action was gated on
     * `SizingEvaluation.Computed`. The journey below is driven through the controls the user taps and
     * the same `HomeEvent.FieldChanged` path the fields use, so it fails if the gate is restored to a
     * computed-only test, not merely if the report function is broken.
     */
    @Test
    fun theReportActionIsOfferedForAReturnedValueAndCarriesTheReturnedTableReport() {
        val table = syntheticTable()
        val viewModel = viewModel(TablesRepository.InMemory(listOf(table)))
        var report: String? = null
        composeRule.setContent {
            HomeSurface(viewModel) { state ->
                report = buildReportFor(
                    ApplicationProvider.getApplicationContext<Context>(),
                    state,
                    isPro = true,
                )
            }
        }

        chooseReturnFromTable(table, nominalSize = "synthetic-size-a")
        enterReadings(viewModel, load = "1", length = "2")
        composeRule.onNodeWithText(text(R.string.evaluate_action)).performScrollTo().performClick()
        composeRule.waitForIdle()

        val evaluation = viewModel.uiState.value.evaluation
        assertTrue("expected a returned value, saw $evaluation", evaluation is SizingEvaluation.ReturnedFromUserTable)

        // The action is offered, and it is the one the user taps.
        composeRule.onNodeWithTag(HomeScreenTags.REPORT_ACTION).performScrollTo().assertIsEnabled().performClick()
        composeRule.waitForIdle()

        // What the action produced is the returned-table report, not the arithmetic one and not the
        // "a report needs a result on screen" message.
        val shared = report
        assertNotNull("the report action produced no report", shared)
        assertTrue(
            "the returned-table report must carry its own title, saw: $shared",
            shared!!.contains(text(R.string.returned_table_result_title)),
        )
        assertTrue(
            "the app must state that it did not verify the value, saw: $shared",
            shared.contains(text(R.string.returned_table_unverified)),
        )
        assertFalse(
            "the arithmetic report is not the one the action chose, saw: $shared",
            shared.contains(text(R.string.result_title)),
        )
    }

    /**
     * The other half of the same decision: a refusal has no report, so the action stays disabled and
     * the sensor above cannot pass by leaving the button enabled for everything.
     */
    @Test
    fun aRefusalOffersNoReport() {
        val viewModel = viewModel(TablesRepository.InMemory(listOf(syntheticTable())))
        composeRule.setContent { HomeSurface(viewModel) {} }

        // A table method with nothing entered yet: the app refuses and names what to correct.
        composeRule.onNodeWithTag(HomeScreenTags.method(SizingMethod.RETURN_FROM_USER_TABLE)).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(text(R.string.evaluate_action)).performScrollTo().performClick()
        composeRule.waitForIdle()

        val evaluation = viewModel.uiState.value.evaluation
        assertTrue("expected a refusal, saw $evaluation", evaluation is SizingEvaluation.Refused)
        composeRule.onNodeWithTag(HomeScreenTags.REPORT_ACTION).performScrollTo().assertIsNotEnabled()
    }

    /** Chooses the method and the table through the controls, the way a user does. */
    private fun chooseReturnFromTable(table: UserCapacityTable, nominalSize: String) {
        composeRule.onNodeWithTag(HomeScreenTags.method(SizingMethod.RETURN_FROM_USER_TABLE)).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(HomeScreenTags.tableOption(table.id)).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(HomeScreenTags.TABLE_NOMINAL_SIZE).performScrollTo().performTextInput(nominalSize)
        composeRule.waitForIdle()
    }

    /** The source label, the no-verification statement and the disclaimer, in one place for reuse. */
    private fun assertSourceLabelAndDisclaimerAreVisible() {
        // The user's own recording is labelled as such - the same marker the entity requires, rendered
        // from the shared constant rather than from hand-written copy (design D2).
        composeRule.onNodeWithText(text(R.string.returned_table_user_recorded, USER_RECORDED_MARKER))
            .performScrollTo()
            .assertIsDisplayed()
        // And the app states plainly that it did neither transcribe nor verify the value.
        composeRule.onNodeWithText(text(R.string.returned_table_unverified))
            .performScrollTo()
            .assertIsDisplayed()
        // The calculation-aid disclaimer is on the result itself, asserted by its own tag because the
        // same words stand at the top of the screen and a text query would match either.
        composeRule.onNodeWithTag(HomeScreenTags.RESULT_DISCLAIMER).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theTableMethodsControlsAreReachableAtLargeFontScale() {
        val table = syntheticTable()
        val viewModel = viewModel(TablesRepository.InMemory(listOf(table)))
        // Twice the system font scale, the size at which a control sized to an English label breaks.
        composeRule.setContent { HomeSurface(viewModel, fontScale = 2f) }

        // The method control is a vertical list of all four methods: every label keeps the full width
        // instead of a quarter of one, so a longer locale wraps rather than clipping.
        composeRule.onNodeWithTag(HomeScreenTags.METHOD_LIST).performScrollTo().assertIsDisplayed()
        SizingMethod.entries.forEach { method ->
            composeRule.onNodeWithTag(HomeScreenTags.method(method)).performScrollTo().assertIsDisplayed()
        }

        composeRule.onNodeWithTag(HomeScreenTags.method(SizingMethod.RETURN_FROM_USER_TABLE)).performScrollTo().performClick()
        composeRule.waitForIdle()

        // The table picker and the nominal-size field are reachable at this font scale, which is what
        // makes the new method usable on a device with large text rather than merely present.
        composeRule.onNodeWithTag(HomeScreenTags.TABLE_PICKER).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(HomeScreenTags.tableOption(table.id)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(HomeScreenTags.TABLE_NOMINAL_SIZE).performScrollTo().assertIsDisplayed()
        // The method that needs no nominal size still shows the picker.
        composeRule.onNodeWithTag(HomeScreenTags.method(SizingMethod.SMALLEST_SIZE_FOR_LOAD)).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(HomeScreenTags.TABLE_PICKER).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(HomeScreenTags.TABLE_NOMINAL_SIZE).assertDoesNotExist()
    }

    private fun enterReadings(viewModel: HomeViewModel, load: String, length: String) {
        listOf(
            HomeField.MATERIAL to "synthetic system",
            HomeField.PRESSURE to "synthetic class",
            HomeField.LOAD to load,
            HomeField.LENGTH to length,
            HomeField.GAS_SPECIFIC_GRAVITY to "21.5",
        ).forEach { (field, value) -> viewModel.onEvent(HomeEvent.FieldChanged(field, value)) }
        composeRule.waitForIdle()
    }

    private fun text(resId: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(resId, *args)

    @Composable
    private fun HomeSurface(
        viewModel: HomeViewModel,
        fontScale: Float = 1f,
        onReport: (HomeUiState) -> Unit = {},
    ) {
        val state by viewModel.uiState.collectAsState()
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            Surface {
                HomeScreen(
                    state = state,
                    onEvent = viewModel::onEvent,
                    isPro = true,
                    canSaveCase = true,
                    savedCaseCount = 0,
                    freeCaseLimit = MonetizationConfig.FREE_CASE_LIMIT,
                    onSaveCase = {},
                    onLimitReached = {},
                    // The screen owns the gate; the caller owns the report, exactly as
                    // `PlayKilnApp` does with `buildReportFor`.
                    onShareReport = { onReport(state) },
                    onOpenPaywall = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    private fun viewModel(repository: TablesRepository) = HomeViewModel(
        persistence = CasePersistence.None,
        tables = repository,
        scope = CoroutineScope(Dispatchers.Unconfined),
    )

    /**
     * Invented synthetic readings: two rows for one size, so the case's own length falls between
     * them and the substitution path is the one on screen. The app supplies no table content.
     */
    private fun syntheticTable(): UserCapacityTable = UserCapacityTable(
        id = "table-1",
        name = "synthetic table",
        applicability = TableApplicability(
            fuel = "synthetic fuel",
            materialOrSystem = "synthetic system",
            inletPressureClass = "synthetic class",
            permittedPressureDrop = "synthetic drop",
            gasSpecificGravity = "21.5",
        ),
        citation = TableCitation(codeAndEdition = "synthetic edition", tableIdentifier = "synthetic identifier"),
        marker = USER_RECORDED_MARKER,
        rows = listOf(
            TableRow("synthetic-size-a", null, 1.0, 2.0),
            TableRow("synthetic-size-a", null, 3.0, 1.0),
        ),
    )
}
