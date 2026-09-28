package com.gexetr.pipesizing.ui.tables

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gexetr.pipesizing.sizing.TableApplicability
import com.gexetr.pipesizing.sizing.TableCitation
import com.gexetr.pipesizing.sizing.TableRow
import com.gexetr.pipesizing.sizing.UserCapacityTable
import com.gexetr.pipesizing.ui.storage.ENTRY_SEPARATOR
import com.gexetr.pipesizing.ui.storage.FIELD_SEPARATOR
import com.gexetr.pipesizing.ui.storage.KEY_VALUE_SEPARATOR
import com.gexetr.pipesizing.ui.storage.RECORD_SEPARATOR
import com.gexetr.pipesizing.ui.storage.escapeField
import com.gexetr.pipesizing.ui.storage.unescapeField
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.capacityTablesStore by preferencesDataStore(name = "capacity_tables")

/**
 * The user's recorded tables, stored on this device only.
 *
 * Deliberately the same triple as `CasesRepository` (`interface` + `InMemory` + `DataStore`) over a
 * second key space, not a second persistence technology: a table outlives the cases read against it,
 * so it gets its own blob, and a corrupt case blob can never cost the user a table (design D8).
 */
interface TablesRepository {

    val tables: Flow<List<UserCapacityTable>>

    suspend fun upsert(table: UserCapacityTable)

    suspend fun delete(id: String)

    suspend fun get(id: String): UserCapacityTable?

    suspend fun clear()

    /** In-memory implementation for previews and tests; never a production backend. */
    class InMemory(initial: List<UserCapacityTable> = emptyList()) : TablesRepository {
        private val state = MutableStateFlow(initial)

        override val tables: Flow<List<UserCapacityTable>> = state

        override suspend fun upsert(table: UserCapacityTable) {
            state.value = state.value.filterNot { it.id == table.id } + table
        }

        override suspend fun delete(id: String) {
            state.value = state.value.filterNot { it.id == id }
        }

        override suspend fun get(id: String): UserCapacityTable? = state.value.firstOrNull { it.id == id }

        override suspend fun clear() {
            state.value = emptyList()
        }
    }
}

class DataStoreTablesRepository(private val context: Context) : TablesRepository {

    override val tables: Flow<List<UserCapacityTable>> =
        context.capacityTablesStore.data.map { preferences ->
            TableCodec.decode(preferences[KEY_TABLES].orEmpty())
        }

    override suspend fun upsert(table: UserCapacityTable) {
        context.capacityTablesStore.edit { preferences ->
            val current = TableCodec.decode(preferences[KEY_TABLES].orEmpty())
            preferences[KEY_TABLES] = TableCodec.encode(current.filterNot { it.id == table.id } + table)
        }
    }

    override suspend fun delete(id: String) {
        context.capacityTablesStore.edit { preferences ->
            val current = TableCodec.decode(preferences[KEY_TABLES].orEmpty())
            preferences[KEY_TABLES] = TableCodec.encode(current.filterNot { it.id == id })
        }
    }

    override suspend fun get(id: String): UserCapacityTable? =
        tables.first().firstOrNull { it.id == id }

    override suspend fun clear() {
        context.capacityTablesStore.edit { preferences -> preferences.remove(KEY_TABLES) }
    }

    private companion object {
        val KEY_TABLES = stringPreferencesKey("tables")
    }
}

/**
 * The tolerant storage codec for the user's recorded tables.
 *
 * Deliberately not JSON (no JSON dependency in this slot) and deliberately tolerant in one
 * direction: one unreadable record is dropped rather than throwing, so a killed write cannot cost
 * the user every table. Inside a record the opposite rule holds — a single unreadable *row* drops
 * the whole record, because a table that silently lost one of its rows is not a smaller table, it
 * is a wrong one.
 *
 * The escaping rule is imported from `ui/storage/LineCodec.kt` and never restated here; a second
 * hand-written escape table is the defect `.cursor/rules/verification-discipline.mdc` names under
 * "One source of truth, imported".
 */
internal object TableCodec {

    private const val HEADER_FIELD_COUNT = 10
    private const val RECORD_FIELD_COUNT = HEADER_FIELD_COUNT + 1
    private const val ROW_FIELD_COUNT = 4

    fun encode(tables: List<UserCapacityTable>): String =
        tables.joinToString(RECORD_SEPARATOR.toString(), transform = ::encodeRecord)

    fun decode(text: String): List<UserCapacityTable> =
        if (text.isBlank()) emptyList() else text.split(RECORD_SEPARATOR).mapNotNull(::decodeRecord)

    private fun encodeRecord(table: UserCapacityTable): String {
        val header = listOf(
            table.id,
            table.name,
            table.applicability.fuel,
            table.applicability.materialOrSystem,
            table.applicability.inletPressureClass,
            table.applicability.permittedPressureDrop,
            table.applicability.gasSpecificGravity,
            table.citation.codeAndEdition,
            table.citation.tableIdentifier,
            table.marker,
        ).joinToString(FIELD_SEPARATOR.toString(), transform = ::escapeField)

        val rows = table.rows.joinToString(ENTRY_SEPARATOR.toString()) { row ->
            listOf(
                row.nominalSize,
                row.insideDiameter?.toString().orEmpty(),
                row.lengthFeet.toString(),
                row.capacityMbh.toString(),
            ).joinToString(KEY_VALUE_SEPARATOR.toString(), transform = ::escapeField)
        }

        return header + FIELD_SEPARATOR + rows
    }

    private fun decodeRecord(record: String): UserCapacityTable? {
        val fields = record.split(FIELD_SEPARATOR)
        if (fields.size != RECORD_FIELD_COUNT) return null

        val header = fields.take(HEADER_FIELD_COUNT).map(::unescapeField)
        val rows = decodeRows(fields[HEADER_FIELD_COUNT]) ?: return null

        return UserCapacityTable(
            id = header[0],
            name = header[1],
            applicability = TableApplicability(
                fuel = header[2],
                materialOrSystem = header[3],
                inletPressureClass = header[4],
                permittedPressureDrop = header[5],
                gasSpecificGravity = header[6],
            ),
            citation = TableCitation(codeAndEdition = header[7], tableIdentifier = header[8]),
            marker = header[9],
            rows = rows,
        )
    }

    /** Null when any single row is unreadable, so a truncated write drops the table, not a row. */
    private fun decodeRows(encoded: String): List<TableRow>? {
        if (encoded.isEmpty()) return emptyList()
        return encoded.split(ENTRY_SEPARATOR).map { decodeRow(it) ?: return null }
    }

    private fun decodeRow(encoded: String): TableRow? {
        val fields = encoded.split(KEY_VALUE_SEPARATOR)
        if (fields.size != ROW_FIELD_COUNT) return null

        val lengthFeet = fields[2].toDoubleOrNull() ?: return null
        val capacityMbh = fields[3].toDoubleOrNull() ?: return null
        val insideDiameter = if (fields[1].isEmpty()) null else fields[1].toDoubleOrNull() ?: return null

        return TableRow(
            nominalSize = unescapeField(fields[0]),
            insideDiameter = insideDiameter,
            lengthFeet = lengthFeet,
            capacityMbh = capacityMbh,
        )
    }
}
