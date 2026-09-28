package com.gexetr.pipesizing.ui.tables

import com.gexetr.pipesizing.sizing.TableApplicability
import com.gexetr.pipesizing.sizing.TableCitation
import com.gexetr.pipesizing.sizing.TableRow
import com.gexetr.pipesizing.sizing.USER_RECORDED_MARKER
import com.gexetr.pipesizing.sizing.UserCapacityTable
import com.gexetr.pipesizing.sizing.validationRefusal
import java.util.UUID

/** The editable header cells of one table. Every cell starts empty; nothing is defaulted (D1). */
enum class TableHeaderField {
    NAME,
    FUEL,
    MATERIAL_OR_SYSTEM,
    INLET_PRESSURE_CLASS,
    PERMITTED_PRESSURE_DROP,
    GAS_SPECIFIC_GRAVITY,
    CODE_AND_EDITION,
    TABLE_IDENTIFIER,
}

/** The editable cells of one recorded row. */
enum class TableRowField {
    NOMINAL_SIZE,
    INSIDE_DIAMETER,
    LENGTH_FEET,
    CAPACITY_MBH,
}

/**
 * One row exactly as the user is typing it: every cell is text and nothing is prefilled.
 *
 * Text that cannot be parsed is turned into `Double.NaN` rather than dropped, so the domain's own
 * `validationRefusal()` is the single judge of whether a row is usable. Parsing to `null` here would
 * be a second, quieter copy of that rule.
 */
data class TableRowDraft(
    val nominalSize: String = "",
    val insideDiameter: String = "",
    val lengthFeet: String = "",
    val capacityMbh: String = "",
) {

    /** The row as the domain stores it. Length and capacity are canonical (feet, MBH). */
    fun toRow(): TableRow = TableRow(
        nominalSize = nominalSize.trim(),
        insideDiameter = if (insideDiameter.isBlank()) null else insideDiameter.trim().toDoubleOrNull() ?: Double.NaN,
        lengthFeet = lengthFeet.trim().toDoubleOrNull() ?: Double.NaN,
        capacityMbh = capacityMbh.trim().toDoubleOrNull() ?: Double.NaN,
    )

    fun withField(field: TableRowField, value: String): TableRowDraft = when (field) {
        TableRowField.NOMINAL_SIZE -> copy(nominalSize = value)
        TableRowField.INSIDE_DIAMETER -> copy(insideDiameter = value)
        TableRowField.LENGTH_FEET -> copy(lengthFeet = value)
        TableRowField.CAPACITY_MBH -> copy(capacityMbh = value)
    }
}

/**
 * The table being typed.
 *
 * No cell has a default and no row exists until the user adds one, because the app owns no table
 * content (`docs/research/2026-09-25-pipe-sizing-code-source.md`). The marker is not a field: it is
 * the shared `USER_RECORDED_MARKER` constant, so the label the result view renders cannot drift.
 */
data class TableDraft(
    val id: String,
    val name: String = "",
    val fuel: String = "",
    val materialOrSystem: String = "",
    val inletPressureClass: String = "",
    val permittedPressureDrop: String = "",
    val gasSpecificGravity: String = "",
    val codeAndEdition: String = "",
    val tableIdentifier: String = "",
    val rows: List<TableRowDraft> = emptyList(),
) {

    /**
     * The size order the app will use, taken from the order the user recorded the sizes in.
     *
     * Computed from the draft text rather than from a parsed table on purpose: a half-typed table
     * must still show the order it is going to use, so a mis-ordered transcription is visible before
     * a case depends on it (D4/D11). `theShownOrderAgreesWithTheDomainOrderOnceTheDraftIsComplete`
     * pins it to `UserCapacityTable.derivedSizeOrder()` so the two cannot drift.
     */
    val derivedSizeOrder: List<String>
        get() = rows
            .map { it.nominalSize.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }

    fun withHeader(field: TableHeaderField, value: String): TableDraft = when (field) {
        TableHeaderField.NAME -> copy(name = value)
        TableHeaderField.FUEL -> copy(fuel = value)
        TableHeaderField.MATERIAL_OR_SYSTEM -> copy(materialOrSystem = value)
        TableHeaderField.INLET_PRESSURE_CLASS -> copy(inletPressureClass = value)
        TableHeaderField.PERMITTED_PRESSURE_DROP -> copy(permittedPressureDrop = value)
        TableHeaderField.GAS_SPECIFIC_GRAVITY -> copy(gasSpecificGravity = value)
        TableHeaderField.CODE_AND_EDITION -> copy(codeAndEdition = value)
        TableHeaderField.TABLE_IDENTIFIER -> copy(tableIdentifier = value)
    }

    fun withRow(index: Int, field: TableRowField, value: String): TableDraft {
        val row = rows.getOrNull(index) ?: return this
        return copy(rows = rows.toMutableList().also { it[index] = row.withField(field, value) })
    }

    fun plusRow(): TableDraft = copy(rows = rows + TableRowDraft())

    fun minusRow(index: Int): TableDraft =
        if (index in rows.indices) copy(rows = rows.filterIndexed { position, _ -> position != index }) else this

    /** The entity the domain validates. Unparseable cells arrive as NaN so the refusal names them. */
    fun buildTable(): UserCapacityTable = UserCapacityTable(
        id = id,
        name = name.trim(),
        applicability = TableApplicability(
            fuel = fuel.trim(),
            materialOrSystem = materialOrSystem.trim(),
            inletPressureClass = inletPressureClass.trim(),
            permittedPressureDrop = permittedPressureDrop.trim(),
            gasSpecificGravity = gasSpecificGravity.trim(),
        ),
        citation = TableCitation(
            codeAndEdition = codeAndEdition.trim(),
            tableIdentifier = tableIdentifier.trim(),
        ),
        marker = USER_RECORDED_MARKER,
        rows = rows.map(TableRowDraft::toRow),
    )

    /**
     * The draft as a complete table, or null while the domain would refuse it.
     *
     * `validationRefusal()` is the whole completeness rule, imported rather than restated, so an
     * incomplete header cannot become a usable table no matter what the screen does.
     */
    fun toTableOrNull(): UserCapacityTable? = buildTable().takeIf { it.validationRefusal() == null }

    companion object {
        fun new(id: String = UUID.randomUUID().toString()): TableDraft = TableDraft(id = id)

        fun from(table: UserCapacityTable): TableDraft = TableDraft(
            id = table.id,
            name = table.name,
            fuel = table.applicability.fuel,
            materialOrSystem = table.applicability.materialOrSystem,
            inletPressureClass = table.applicability.inletPressureClass,
            permittedPressureDrop = table.applicability.permittedPressureDrop,
            gasSpecificGravity = table.applicability.gasSpecificGravity,
            codeAndEdition = table.citation.codeAndEdition,
            tableIdentifier = table.citation.tableIdentifier,
            rows = table.rows.map { row ->
                TableRowDraft(
                    nominalSize = row.nominalSize,
                    insideDiameter = row.insideDiameter?.toString().orEmpty(),
                    lengthFeet = row.lengthFeet.toString(),
                    capacityMbh = row.capacityMbh.toString(),
                )
            },
        )
    }
}

/**
 * What the table surface renders.
 *
 * The list is the repository's own flow and the draft is the one table open for editing; everything
 * derived (`derivedSizeOrder`, `editingIsUsable`, `editingProblem`) is computed here rather than in
 * the composable, so the screen cannot disagree with the domain about what is usable.
 */
data class TablesUiState(
    val tables: List<UserCapacityTable> = emptyList(),
    val editing: TableDraft? = null,
) {
    /** The order the app will use, in the order the user recorded the sizes. */
    val derivedSizeOrder: List<String> get() = editing?.derivedSizeOrder.orEmpty()

    /** True only when the domain accepts the draft as a complete, usable table. */
    val editingIsUsable: Boolean get() = editing?.toTableOrNull() != null

    /** The correction the domain names while the draft is unusable, or null when it is usable. */
    val editingProblem: String? get() = editing?.buildTable()?.validationRefusal()?.correctionRequired
}
