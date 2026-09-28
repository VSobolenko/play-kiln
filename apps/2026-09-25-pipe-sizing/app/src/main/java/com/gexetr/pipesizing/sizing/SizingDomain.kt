package com.gexetr.pipesizing.sizing

/**
 * Pure arithmetic for user-recorded fuel-gas sizing inputs; this is not a code-compliance engine.
 *
 * Physical quantities are stored in canonical imperial units (feet and MBH). Presentation conversion
 * uses IEEE-754 doubles and standard exact conversion factors. Callers round only for display; tests
 * compare within 1e-6 so conversion round trips are not mistaken for a change in physical value.
 * No capacity, equivalent-length, gravity, pressure-drop, or diameter value is supplied here.
 */
enum class UnitSystem { IMPERIAL, METRIC }

enum class CapacityLikeKind {
    FITTING_EQUIVALENT_LENGTH,
    GAS_SPECIFIC_GRAVITY,
    PRESSURE_DROP_ALLOWANCE,
    PIPE_INSIDE_DIAMETER,
}

enum class SizingMethod(val requiredInputs: Set<CapacityLikeKind>) {
    CAPACITY_COMPARISON(emptySet()),
    CAPACITY_COMPARISON_WITH_PHYSICAL_INPUTS(
        setOf(
            CapacityLikeKind.GAS_SPECIFIC_GRAVITY,
            CapacityLikeKind.PRESSURE_DROP_ALLOWANCE,
            CapacityLikeKind.PIPE_INSIDE_DIAMETER,
        ),
    ),
    RETURN_FROM_USER_TABLE(emptySet()),
    SMALLEST_SIZE_FOR_LOAD(emptySet()),
}

val SizingMethod.returnsFromUserTable: Boolean
    get() = this == SizingMethod.RETURN_FROM_USER_TABLE || this == SizingMethod.SMALLEST_SIZE_FOR_LOAD

sealed interface UserProvenance {
    data object UserSupplied : UserProvenance

    data class CodeCitation(
        val codeAndEdition: String,
        val table: String,
        val row: String,
    ) : UserProvenance

    data class RecordedTableCitation(
        val tableId: String,
        val codeAndEdition: String,
        val tableIdentifier: String,
    ) : UserProvenance
}

/** Draft input may lack provenance, but [SizingEvaluation.Computed] never can. */
data class SourcedValue(val value: Double, val provenance: UserProvenance?)

data class Load(val label: String, val mbh: Double)

data class DevelopedLength(val label: String, val feet: Double)

data class FittingAllowance(
    val fittingLabel: String,
    val quantity: Int,
    val equivalentLengthFeet: SourcedValue,
)

data class CapacityEntry(
    val value: Double,
    val provenance: UserProvenance?,
    val confirmedForMethod: SizingMethod,
    /** Optional user-entered applicability limit; no limit is inferred when it is absent. */
    val statedMaximumTotalLengthFeet: Double? = null,
)

data class TableSelection(
    val tableId: String? = null,
    val nominalSize: String? = null,
)

data class SizingCase(
    val materialOrSystem: String,
    val pressureClass: String,
    val method: SizingMethod,
    val unitSystem: UnitSystem,
    val loads: List<Load>,
    val developedLengths: List<DevelopedLength>,
    val fittingAllowances: List<FittingAllowance>,
    val capacity: CapacityEntry?,
    val gasSpecificGravity: SourcedValue? = null,
    val pressureDropAllowance: SourcedValue? = null,
    val pipeInsideDiameter: SourcedValue? = null,
    val tableSelection: TableSelection = TableSelection(),
)

fun SizingCase.requireCapacity(): CapacityEntry = requireNotNull(capacity)

/*
 * The one definition of the case's load and length sums. The arithmetic evaluator and the
 * user-table lookup both consume these; a second copy is what `verification-discipline.mdc`
 * forbids, because the two would be free to drift.
 */
internal fun SizingCase.requiredLoadMbh(): Double = loads.sumOf { it.mbh }

internal fun SizingCase.developedLengthFeet(): Double = developedLengths.sumOf { it.feet }

internal fun SizingCase.fittingAllowanceFeet(): Double =
    fittingAllowances.sumOf { it.quantity.toDouble() * it.equivalentLengthFeet.value }

internal fun SizingCase.totalLengthFeet(): Double = developedLengthFeet() + fittingAllowanceFeet()

enum class RefusalKind {
    MISSING_INPUT,
    OUT_OF_RANGE,
    INCONSISTENT_INPUT,
    MISSING_PROVENANCE,
    NO_TABLE_SELECTED,
    TABLE_INCOMPLETE,
    APPLICABILITY_MISMATCH,
    SIZE_NOT_IN_TABLE,
    NOMINAL_SIZE_MISSING,
    LENGTH_ABOVE_EVERY_ENTERED_ROW,
    LOAD_ABOVE_EVERY_ENTERED_CAPACITY,
    SIZE_ORDER_INDETERMINATE,
    TABLE_CHANGED,
}

sealed interface SizingEvaluation {
    data class Computed internal constructor(
        val requiredLoadMbh: Double,
        val developedLengthFeet: Double,
        val fittingAllowanceFeet: Double,
        val totalLengthFeet: Double,
        val supportsRequiredLoad: Boolean,
        val capacityProvenance: UserProvenance,
    ) : SizingEvaluation

    data class ReturnedFromUserTable internal constructor(
        val requiredLoadMbh: Double,
        val totalLengthFeet: Double,
        val returnedCapacityMbh: Double,
        val returnedSize: String,
        val rowLabel: String,
        val substitution: LengthSubstitution?,
        val capacityProvenance: UserProvenance.RecordedTableCitation,
    ) : SizingEvaluation

    data class Refused(val kind: RefusalKind, val correctionRequired: String) : SizingEvaluation
}

data class LengthSubstitution(val enteredLengthFeet: Double, val usedLengthFeet: Double)

object Units {
    // Pure unit identities, not pipe/code-table data.
    fun feetToMeters(feet: Double): Double = feet * 0.3048
    fun metersToFeet(meters: Double): Double = meters / 0.3048
    // 1 MBH = 1000 BTU_IT/hour = 1000 * 1055.05585262 J/hour = 0.2930710701722222 kW.
    private const val MBH_TO_KILOWATTS = 0.2930710701722222
    fun mbhToKilowatts(mbh: Double): Double = mbh * MBH_TO_KILOWATTS
    fun kilowattsToMbh(kilowatts: Double): Double = kilowatts / MBH_TO_KILOWATTS
}

object SizingEvaluator {
    fun evaluate(case: SizingCase, table: UserCapacityTable? = null): SizingEvaluation =
        if (case.method.returnsFromUserTable) {
            evaluateCaseAgainstUserTable(case, table)
        } else {
            evaluateArithmetically(case)
        }

    private fun evaluateArithmetically(case: SizingCase): SizingEvaluation {
        if (case.materialOrSystem.isBlank()) return missing("material or system")
        if (case.pressureClass.isBlank()) return missing("pressure class")
        if (case.loads.isEmpty()) return missing("at least one load")
        if (case.developedLengths.isEmpty()) return missing("at least one developed length")

        val capacity = case.capacity ?: return missing("user-confirmed capacity")
        if (capacity.provenance == null) return missingProvenance("capacity")
        if (capacity.confirmedForMethod != case.method) {
            return inconsistent("confirm capacity for the selected method")
        }
        if (!validPositive(capacity.value)) return outOfRange("user-confirmed capacity")

        case.loads.forEach {
            if (it.label.isBlank()) return missing("load label")
            if (!validNonNegative(it.mbh)) return outOfRange("load")
        }
        case.developedLengths.forEach {
            if (it.label.isBlank()) return missing("developed-length label")
            if (!validNonNegative(it.feet)) return outOfRange("developed length")
        }
        case.fittingAllowances.forEach {
            if (it.fittingLabel.isBlank()) return missing("fitting label")
            if (it.quantity <= 0) return outOfRange("fitting quantity")
            if (it.equivalentLengthFeet.provenance == null) return missingProvenance("fitting equivalent length")
            if (!validNonNegative(it.equivalentLengthFeet.value)) return outOfRange("fitting equivalent length")
        }

        val requiredMethodInputs = mapOf(
            CapacityLikeKind.GAS_SPECIFIC_GRAVITY to case.gasSpecificGravity,
            CapacityLikeKind.PRESSURE_DROP_ALLOWANCE to case.pressureDropAllowance,
            CapacityLikeKind.PIPE_INSIDE_DIAMETER to case.pipeInsideDiameter,
        )
        for (kind in case.method.requiredInputs) {
            val input = requiredMethodInputs.getValue(kind) ?: return missing(kind.displayName())
            if (input.provenance == null) return missingProvenance(kind.displayName())
            if (!validPositive(input.value)) return outOfRange(kind.displayName())
        }

        val requiredLoad = case.requiredLoadMbh()
        val developedLength = case.developedLengthFeet()
        val fittingAllowance = case.fittingAllowanceFeet()
        val totalLength = case.totalLengthFeet()
        val maximumLength = capacity.statedMaximumTotalLengthFeet
        if (maximumLength != null && (!validPositive(maximumLength) || totalLength > maximumLength)) {
            return outOfRange("total length for the user-confirmed capacity")
        }
        return SizingEvaluation.Computed(
            requiredLoadMbh = requiredLoad,
            developedLengthFeet = developedLength,
            fittingAllowanceFeet = fittingAllowance,
            totalLengthFeet = totalLength,
            supportsRequiredLoad = capacity.value >= requiredLoad,
            capacityProvenance = capacity.provenance,
        )
    }

    private fun validNonNegative(value: Double): Boolean = value.isFinite() && value >= 0.0
    private fun validPositive(value: Double): Boolean = value.isFinite() && value > 0.0
    private fun missing(field: String) = SizingEvaluation.Refused(RefusalKind.MISSING_INPUT, "Enter $field.")
    private fun outOfRange(field: String) = SizingEvaluation.Refused(RefusalKind.OUT_OF_RANGE, "Correct the out-of-range $field.")
    private fun inconsistent(field: String) = SizingEvaluation.Refused(RefusalKind.INCONSISTENT_INPUT, "Correct inconsistent $field.")
    private fun missingProvenance(field: String) = SizingEvaluation.Refused(RefusalKind.MISSING_PROVENANCE, "Record provenance for $field.")
}

private fun CapacityLikeKind.displayName(): String = when (this) {
    CapacityLikeKind.GAS_SPECIFIC_GRAVITY -> "gas specific gravity"
    CapacityLikeKind.PRESSURE_DROP_ALLOWANCE -> "pressure-drop allowance"
    CapacityLikeKind.PIPE_INSIDE_DIAMETER -> "pipe inside diameter"
    CapacityLikeKind.FITTING_EQUIVALENT_LENGTH -> "fitting equivalent length"
}
