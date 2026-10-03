package com.gexetr.laneset.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gexetr.laneset.domain.Focus
import com.gexetr.laneset.domain.Gear
import com.gexetr.laneset.domain.SectionKind
import com.gexetr.laneset.domain.Session
import com.gexetr.laneset.domain.SessionConstraints
import com.gexetr.laneset.domain.SessionSection
import com.gexetr.laneset.domain.SetBlock
import com.gexetr.laneset.domain.Stroke
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Where in a session the swimmer is, as indices rather than as a wall-clock stopwatch.
 *
 * A pool visit is interrupted — a phone call, a rotation, a walk back to the locker — and what has
 * to survive is the *position in the set*, not an elapsed timer that would have kept running. The
 * section/block/repetition triple is the smallest thing that can be resumed: it names the line the
 * swimmer was on and how many of its repetitions were already swum.
 *
 * Indices are clamped against the session they are restored with ([clampedTo]), so a position
 * stored against a different arrangement cannot point past the end of the one on screen.
 */
data class SessionPosition(
    val sectionIndex: Int = 0,
    val blockIndex: Int = 0,
    val repetitionIndex: Int = 0,
) {
    /**
     * This position, guaranteed to point at a real block and repetition of [session].
     *
     * The two ways a stored position can be wrong are treated differently, because they mean
     * different things. A position naming a **section or block the session does not have** is not a
     * position in this session at all — the record and the set are from different swims, and the
     * honest answer is [Start], not the last line of a workout the swimmer was never on. A position
     * naming a **repetition beyond its own block** is a line that does exist with a count that
     * drifted, which clamps to that block's last repetition without discarding where the swimmer was.
     */
    fun clampedTo(session: Session): SessionPosition {
        if (sectionIndex !in session.sections.indices) return Start
        val blocks = session.sections[sectionIndex].blocks
        if (blockIndex !in blocks.indices) return Start
        val repetitions = blocks[blockIndex].repetitions
        if (repetitions <= 0) return Start
        return SessionPosition(
            sectionIndex = sectionIndex,
            blockIndex = blockIndex,
            repetitionIndex = repetitionIndex.coerceIn(0, repetitions - 1),
        )
    }

    companion object {
        /** The top of a session, before anything has been swum. */
        val Start = SessionPosition()
    }
}

/** A session and the swimmer's place in it: the whole of what has to survive process death. */
data class PersistedSession(
    val session: Session,
    val position: SessionPosition = SessionPosition.Start,
)

/**
 * A stable, versioned, framework-free codec for [PersistedSession].
 *
 * A pure Kotlin/JVM function is chosen over a serialization framework on purpose: the format is the
 * contract the durability promise rests on, so it must be readable and writable with no Android type
 * on the classpath, which makes it directly unit-testable. Every field is an enum name, an integer
 * or a double, so the format needs no string escaping and cannot be broken by a stroke or gear name —
 * the vocabulary is closed by the domain enums.
 *
 * The format is line-oriented; `v` is the schema version and a payload whose version is not
 * [SCHEMA_VERSION] is refused rather than guessed at, so an old or future payload degrades to "no
 * session" instead of being read as this one. [decode] never throws: a malformed, truncated or
 * unreadable payload returns `null`, which the store surfaces as "nothing to resume".
 */
object SessionSerializer {

    /** Bump when the layout below changes incompatibly. */
    const val SCHEMA_VERSION: Int = 1

    private const val EMPTY_SET = "-"
    private const val SECTION_PREFIX = "s="
    private const val BLOCK_PREFIX = "b="
    private const val POSITION_PREFIX = "pos="

    /** Serialises [value] to the versioned text form. */
    fun encode(value: PersistedSession): String = buildString {
        val session = value.session
        val constraints = session.constraints
        appendLine("v=$SCHEMA_VERSION")
        appendLine("pace=${session.pacePer100Seconds}")
        appendLine("pool=${constraints.poolLengthMeters}")
        appendLine("minutes=${constraints.minutesAvailable}")
        appendLine("focus=${constraints.focus.name}")
        appendLine("avoid=${encodeSet(constraints.strokesToAvoid.map { it.name })}")
        appendLine("gear=${encodeSet(constraints.gearOnDeck.map { it.name })}")
        appendLine("totalSeconds=${session.estimatedTotalSeconds}")
        appendLine("totalMeters=${session.totalDistanceMeters}")
        session.sections.forEach { section ->
            appendLine("$SECTION_PREFIX${section.kind.name}")
            section.blocks.forEach { block -> appendLine("$BLOCK_PREFIX${encodeBlock(block)}") }
        }
        appendLine(
            "$POSITION_PREFIX${value.position.sectionIndex}|" +
                "${value.position.blockIndex}|${value.position.repetitionIndex}",
        )
    }

    /**
     * Reads [payload] back, or `null` when it is absent, of another schema version, or unreadable.
     *
     * Returning `null` rather than throwing is the whole contract: the caller is a cold-start
     * restore, and a corrupt file must cost the swimmer the saved session, never the app.
     */
    fun decode(payload: String?): PersistedSession? {
        if (payload.isNullOrBlank()) return null
        return try {
            parse(payload)
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun encodeBlock(block: SetBlock): String = listOf(
        block.distanceMeters.toString(),
        block.repetitions.toString(),
        block.stroke.name,
        block.targetTimeSeconds.toString(),
        block.restSeconds.toString(),
        encodeSet(block.gear.map { it.name }),
    ).joinToString("|")

    private fun encodeSet(values: List<String>): String =
        if (values.isEmpty()) EMPTY_SET else values.sorted().joinToString(",")

    private fun decodeSet(raw: String): List<String> =
        if (raw == EMPTY_SET || raw.isBlank()) emptyList() else raw.split(",")

    private fun parse(payload: String): PersistedSession? {
        var version = -1
        var pace: Double? = null
        var pool: Int? = null
        var minutes: Int? = null
        var focus: Focus? = null
        var avoid: Set<Stroke>? = null
        var gear: Set<Gear>? = null
        var totalSeconds: Int? = null
        var totalMeters: Int? = null
        val sections = mutableListOf<SessionSection>()
        var currentKind: SectionKind? = null
        var currentBlocks = mutableListOf<SetBlock>()
        var position = SessionPosition.Start

        fun flushSection() {
            val kind = currentKind ?: return
            sections += SessionSection(kind, currentBlocks.toList())
            currentBlocks = mutableListOf()
        }

        for (rawLine in payload.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val separator = line.indexOf('=')
            if (separator <= 0) return null
            val key = line.substring(0, separator)
            val value = line.substring(separator + 1)
            when (key) {
                "v" -> version = value.toInt()
                "pace" -> pace = value.toDouble()
                "pool" -> pool = value.toInt()
                "minutes" -> minutes = value.toInt()
                "focus" -> focus = Focus.valueOf(value)
                "avoid" -> avoid = decodeSet(value).map { Stroke.valueOf(it) }.toSet()
                "gear" -> gear = decodeSet(value).map { Gear.valueOf(it) }.toSet()
                "totalSeconds" -> totalSeconds = value.toInt()
                "totalMeters" -> totalMeters = value.toInt()
                "s" -> {
                    flushSection()
                    currentKind = SectionKind.valueOf(value)
                }
                "b" -> {
                    // A block before any section header is a payload that cannot be trusted.
                    if (currentKind == null) return null
                    currentBlocks += parseBlock(value)
                }
                "pos" -> position = parsePosition(value)
                else -> return null
            }
        }
        flushSection()

        if (version != SCHEMA_VERSION) return null
        val paceValue = pace ?: return null
        val poolLength = pool ?: return null
        val minutesAvailable = minutes ?: return null
        val sessionFocus = focus ?: return null
        val strokesToAvoid = avoid ?: return null
        val gearOnDeck = gear ?: return null
        val estimatedTotalSeconds = totalSeconds ?: return null
        val totalDistanceMeters = totalMeters ?: return null
        if (paceValue <= 0.0 || poolLength <= 0 || minutesAvailable <= 0) return null
        if (sections.isEmpty()) return null

        val session = Session(
            constraints = SessionConstraints(
                poolLengthMeters = poolLength,
                minutesAvailable = minutesAvailable,
                focus = sessionFocus,
                strokesToAvoid = strokesToAvoid,
                gearOnDeck = gearOnDeck,
            ),
            pacePer100Seconds = paceValue,
            sections = sections,
            estimatedTotalSeconds = estimatedTotalSeconds,
            totalDistanceMeters = totalDistanceMeters,
        )
        return PersistedSession(session, position.clampedTo(session))
    }

    private fun parseBlock(value: String): SetBlock {
        val fields = value.split("|")
        require(fields.size >= 6) { "a block needs 6 fields, was ${fields.size}" }
        return SetBlock(
            distanceMeters = fields[0].toInt(),
            repetitions = fields[1].toInt(),
            stroke = Stroke.valueOf(fields[2]),
            targetTimeSeconds = fields[3].toInt(),
            restSeconds = fields[4].toInt(),
            gear = decodeSet(fields[5]).map { Gear.valueOf(it) }.toSet(),
        )
    }

    private fun parsePosition(value: String): SessionPosition {
        val fields = value.split("|")
        require(fields.size >= 3) { "a position needs 3 fields, was ${fields.size}" }
        return SessionPosition(
            sectionIndex = fields[0].toInt(),
            blockIndex = fields[1].toInt(),
            repetitionIndex = fields[2].toInt(),
        )
    }
}

/**
 * The current session, persisted so it survives the process being killed.
 *
 * This is the storage half of the contract promise `session-survives-interruption`: a generated
 * session comes back with the same set, the same interval targets and the same position rather than
 * being regenerated.
 *
 * The store is deliberately dumb — it saves what it is given and never decides how a session
 * advances. Advancing through repetitions is the runner's job, and a store that "helpfully" moved
 * the position would be a second place a session's progress is decided.
 */
interface SessionStore {
    /** The saved session and position, or `null` when there is none. */
    val saved: Flow<PersistedSession?>

    /** Saves a whole session at [position]; call when a new session starts. */
    suspend fun save(session: Session, position: SessionPosition = SessionPosition.Start)

    /** Moves the stored position without touching the stored session. A no-op when nothing is saved. */
    suspend fun savePosition(position: SessionPosition)

    /** Forgets the saved session; the swimmer finished it or chose another. */
    suspend fun clear()
}

/** A separate DataStore file: the in-progress session has nothing to do with preferences or pace. */
private val Context.sessionDataStore by preferencesDataStore(name = "session")

/**
 * The DataStore-backed implementation of [SessionStore].
 *
 * The session is stored as the [SessionSerializer] text under one string key: it is read whole or
 * not at all, so a partially-updated record is impossible, and an unreadable payload is `null`
 * rather than an exception thrown on cold start.
 */
class DataStoreSessionStore(private val context: Context) : SessionStore {

    override val saved: Flow<PersistedSession?> = context.sessionDataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }
        .map { preferences -> SessionSerializer.decode(preferences[SESSION_PAYLOAD]) }

    override suspend fun save(session: Session, position: SessionPosition) {
        val payload = SessionSerializer.encode(PersistedSession(session, position))
        context.sessionDataStore.edit { preferences -> preferences[SESSION_PAYLOAD] = payload }
    }

    override suspend fun savePosition(position: SessionPosition) {
        context.sessionDataStore.edit { preferences ->
            val current = SessionSerializer.decode(preferences[SESSION_PAYLOAD]) ?: return@edit
            preferences[SESSION_PAYLOAD] = SessionSerializer.encode(
                current.copy(position = position.clampedTo(current.session)),
            )
        }
    }

    override suspend fun clear() {
        context.sessionDataStore.edit { preferences -> preferences.remove(SESSION_PAYLOAD) }
    }

    private companion object {
        val SESSION_PAYLOAD = stringPreferencesKey("session_payload")
    }
}
