package com.gexetr.laneset.domain

/**
 * The stroke a set is swum in.
 *
 * The set is deliberately small and closed: a pool practice is swum in these five strokes, and no
 * free-text label belongs in the model. Anything a coach writes that is not one of them (a kick, a
 * drill, a pull) is an *equipment* or *intent* variation on a stroke, not a separate stroke, which
 * keeps [SetBlock.stroke] answerable at a glance.
 */
enum class Stroke {
    FREESTYLE,
    BACKSTROKE,
    BREASTSTROKE,
    BUTTERFLY,
    INDIVIDUAL_MEDLEY,
}

/**
 * What the swimmer is training today.
 *
 * The focus is an input to the generator (it selects the distances, repetitions, rest and pace
 * factor) rather than a label the UI puts on the result.
 */
enum class Focus {
    SPEED,
    THRESHOLD,
    ENDURANCE,
    TECHNIQUE,
    RECOVERY,
}

/**
 * The equipment actually on the pool deck.
 *
 * The generator may only program a piece of gear that is declared here, so a swimmer who owns no
 * fins never sees a fins set. Like [Stroke] this is a closed set — the enum cannot name a piece of
 * kit the app has no rule for.
 */
enum class Gear {
    KICKBOARD,
    PULL_BUOY,
    FINS,
    PADDLES,
    SNORKEL,
}

/**
 * The swimmer's own constraints for one session, exactly as the UI collected them.
 *
 * `minutesAvailable` is the wall-clock budget for the whole session; the generator fits the warm-up,
 * the main set and the cool-down inside it and reports the total it actually used. `strokesToAvoid`
 * is honoured absolutely: a stroke in it is never emitted, even for a warm-up.
 */
data class SessionConstraints(
    val poolLengthMeters: Int,
    val minutesAvailable: Int,
    val focus: Focus,
    val strokesToAvoid: Set<Stroke>,
    val gearOnDeck: Set<Gear>,
)

/**
 * One repeatable interval, e.g. `8 x 100 free on 1:45`.
 *
 * [targetTimeSeconds] is the time to swim **one** repetition, derived from the threshold pace, and
 * [restSeconds] is the rest after each repetition, so the send-off the swimmer leaves on is
 * `targetTimeSeconds + restSeconds`. Keeping the target and the rest apart is what lets the UI print
 * a time on every line instead of only a distance.
 *
 * [gear] is empty for a body-only set and otherwise names the declared equipment the set uses.
 */
data class SetBlock(
    val distanceMeters: Int,
    val repetitions: Int,
    val stroke: Stroke,
    val targetTimeSeconds: Int,
    val restSeconds: Int,
    val gear: Set<Gear> = emptySet(),
) {
    /** The whole block's wall-clock cost: every repetition's target plus its rest. */
    val totalSeconds: Int get() = repetitions * (targetTimeSeconds + restSeconds)

    /** The whole block's distance: every repetition's distance. */
    val totalDistanceMeters: Int get() = repetitions * distanceMeters
}

/**
 * The interval type the rest of the app speaks about.
 *
 * It is the same value as [SetBlock] under a second name, kept because "interval" is the word a
 * swimmer uses for one line of a set list.
 */
typealias Interval = SetBlock

/** Which third of the session a group of blocks belongs to. */
enum class SectionKind {
    WARM_UP,
    MAIN_SET,
    COOL_DOWN,
}

/** A run of blocks with the role it plays in the session. */
data class SessionSection(
    val kind: SectionKind,
    val blocks: List<SetBlock>,
) {
    val totalSeconds: Int get() = blocks.sumOf { it.totalSeconds }

    val totalDistanceMeters: Int get() = blocks.sumOf { it.totalDistanceMeters }
}

/**
 * A whole session: a warm-up, a main set and a cool-down, every interval carrying a target time.
 *
 * [estimatedTotalSeconds] and [totalDistanceMeters] are stated in the model rather than left for the
 * caller to re-add, so "does this fit the minutes I have?" is answerable without walking the sections,
 * and the UI never recomputes a number the domain already knows.
 */
data class Session(
    val constraints: SessionConstraints,
    val pacePer100Seconds: Double,
    val sections: List<SessionSection>,
    val estimatedTotalSeconds: Int,
    val totalDistanceMeters: Int,
) {
    /** Every block in the session, in swim order. */
    val blocks: List<SetBlock> get() = sections.flatMap { it.blocks }

    /** The section of the given kind, or `null` when the session has none. */
    fun section(kind: SectionKind): SessionSection? = sections.firstOrNull { it.kind == kind }

    /**
     * The main set's blocks.
     *
     * This is the value the contract's negative control compares: it must change when the constraints
     * change, because a main set sliced out of a fixed library would not.
     */
    val mainSetBlocks: List<SetBlock> get() = section(SectionKind.MAIN_SET)?.blocks.orEmpty()

    /** The session's cost in whole minutes, rounded up so a partial minute is not hidden. */
    val estimatedTotalMinutes: Int get() = (estimatedTotalSeconds + 59) / 60
}
