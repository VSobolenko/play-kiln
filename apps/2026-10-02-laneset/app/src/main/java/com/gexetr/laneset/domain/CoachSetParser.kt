package com.gexetr.laneset.domain

/**
 * Parses a coach's written practice into the same [Session] the generator produces.
 *
 * A swimmer who already has a club set should not retype it into a different shape, so a pasted
 * practice becomes exactly the value a generated session is: sections, blocks, and a
 * [SetBlock.targetTimeSeconds] on every interval derived from the swimmer's own threshold pace by
 * [PaceModel]. The parser owns no display text — it returns the [Session] model and the UI formats it.
 *
 * What it understands, because that is how practices are actually written:
 * - a bare section header (`Warm up`, `Main set`, `Cool down`) sets the section for the lines after
 *   it, while a header carrying its own set (`Warm up 300 free`) applies to that line alone and
 *   leaves the lines after it in the main set, which is how a shorthand practice reads;
 * - a set as `repetitions x distance` (`8x100`) or a bare distance (`300`);
 * - a stroke word after the distance (`free`, `back`, `breast`, `fly`, `IM`), defaulting to freestyle;
 * - a send-off (`on 1:45`, `@1:10`), from which the rest is derived as send-off minus target;
 * - an explicit rest (`rest 20`).
 *
 * Blank lines and `#`/`//` comments are ignored, and a line with no readable distance is skipped
 * rather than guessed at. When the text names no section headers, every set is the main set.
 */
object CoachSetParser {

    private const val DEFAULT_REST_SECONDS = 15

    private val REPETITIONS_AND_DISTANCE = Regex("""(\d+)\s*[x×]\s*(\d+)""")
    private val SINGLE_DISTANCE = Regex("""(?<![\d:])(\d{2,4})(?![\d:])""")
    private val SEND_OFF = Regex("""(?:on|@|at)\s*(\d+):(\d{2})""")
    private val REST = Regex("""rest\s*(\d+)""")
    private val WORD = Regex("""[a-z]+""")

    private val STROKE_ALIASES: Map<String, Stroke> = mapOf(
        "free" to Stroke.FREESTYLE,
        "freestyle" to Stroke.FREESTYLE,
        "fr" to Stroke.FREESTYLE,
        "back" to Stroke.BACKSTROKE,
        "backstroke" to Stroke.BACKSTROKE,
        "bk" to Stroke.BACKSTROKE,
        "breast" to Stroke.BREASTSTROKE,
        "breaststroke" to Stroke.BREASTSTROKE,
        "br" to Stroke.BREASTSTROKE,
        "fly" to Stroke.BUTTERFLY,
        "butterfly" to Stroke.BUTTERFLY,
        "fl" to Stroke.BUTTERFLY,
        "im" to Stroke.INDIVIDUAL_MEDLEY,
        "medley" to Stroke.INDIVIDUAL_MEDLEY,
    )

    private val COOL_DOWN_PHRASES = listOf("cool down", "cool-down", "cooldown", "warm down", "warmdown")
    private val WARM_UP_PHRASES = listOf("warm up", "warm-up", "warmup", "warm")
    private val MAIN_SET_PHRASES = listOf("main set", "main")

    /**
     * Parses [text] at [pacePer100Seconds] per 100 m.
     *
     * [constraints] supplies the pool length and the focus the session is recorded against; the
     * minute total is recomputed from the parsed sets, because the practice, not the caller, decides
     * how long it takes. The focus is taken from the text when it names one.
     */
    fun parse(
        text: String,
        pacePer100Seconds: Double,
        constraints: SessionConstraints = SessionConstraints(
            poolLengthMeters = 25,
            minutesAvailable = 1,
            focus = Focus.ENDURANCE,
            strokesToAvoid = emptySet(),
            gearOnDeck = emptySet(),
        ),
    ): Session {
        require(pacePer100Seconds > 0.0) {
            "pacePer100Seconds must be positive, was $pacePer100Seconds s"
        }
        require(constraints.poolLengthMeters > 0) {
            "poolLengthMeters must be positive, was ${constraints.poolLengthMeters}"
        }

        val sectionsByKind = linkedMapOf<SectionKind, MutableList<SetBlock>>()
        var currentSection = SectionKind.MAIN_SET

        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("#") || line.startsWith("//")) continue

            val header = sectionFor(line.lowercase())
            var section = currentSection
            val setPortion: String
            if (header != null) {
                setPortion = header.second
                if (setPortion.isBlank()) {
                    // A bare header opens a section the following lines belong to, and carries no
                    // set of its own.
                    currentSection = header.first
                    continue
                }
                // A header carrying its own set is a complete instruction: the set belongs to that
                // section, and the lines after it are the rest of the practice — the main set,
                // unless another header claims them.
                section = header.first
                currentSection = SectionKind.MAIN_SET
            } else {
                setPortion = line
            }

            val block = parseBlock(setPortion, pacePer100Seconds) ?: continue
            val blocks = sectionsByKind.getOrPut(section) { mutableListOf() }
            blocks += block
        }

        val sections = SectionKind.entries.mapNotNull { kind ->
            val blocks = sectionsByKind[kind]
            if (blocks.isNullOrEmpty()) null else SessionSection(kind, blocks)
        }

        val estimatedTotalSeconds = sections.sumOf { it.totalSeconds }
        val totalDistanceMeters = sections.sumOf { it.totalDistanceMeters }

        return Session(
            constraints = constraints.copy(
                minutesAvailable = ((estimatedTotalSeconds + 59) / 60).coerceAtLeast(1),
                focus = detectFocus(text) ?: constraints.focus,
            ),
            pacePer100Seconds = pacePer100Seconds,
            sections = sections,
            estimatedTotalSeconds = estimatedTotalSeconds,
            totalDistanceMeters = totalDistanceMeters,
        )
    }

    /** Returns the section a header line opens, plus whatever set sits on the same line. */
    private fun sectionFor(loweredLine: String): Pair<SectionKind, String>? {
        val candidates = listOf(
            SectionKind.COOL_DOWN to COOL_DOWN_PHRASES,
            SectionKind.WARM_UP to WARM_UP_PHRASES,
            SectionKind.MAIN_SET to MAIN_SET_PHRASES,
        )
        for ((kind, phrases) in candidates) {
            for (phrase in phrases) {
                if (loweredLine == phrase) return kind to ""
                if (loweredLine.startsWith("$phrase ") || loweredLine.startsWith("$phrase:")) {
                    val remainder = loweredLine.substring(phrase.length).trimStart(' ', ':', '-').trim()
                    return kind to remainder
                }
            }
        }
        return null
    }

    private fun parseBlock(setPortion: String, pacePer100Seconds: Double): SetBlock? {
        val lowered = setPortion.lowercase()

        val repetitionsAndDistance = REPETITIONS_AND_DISTANCE.find(lowered)
        val repetitions: Int
        val distanceMeters: Int
        if (repetitionsAndDistance != null) {
            repetitions = repetitionsAndDistance.groupValues[1].toIntOrNull() ?: return null
            distanceMeters = repetitionsAndDistance.groupValues[2].toIntOrNull() ?: return null
        } else {
            val single = SINGLE_DISTANCE.find(lowered) ?: return null
            repetitions = 1
            distanceMeters = single.groupValues[1].toIntOrNull() ?: return null
        }
        if (repetitions <= 0 || distanceMeters <= 0) return null

        val stroke = STROKE_ALIASES.entries
            .firstOrNull { (alias, _) -> WORD.findAll(lowered).any { it.value == alias } }
            ?.value
            ?: Stroke.FREESTYLE

        val sendOffSeconds = SEND_OFF.find(lowered)?.let { match ->
            val minutes = match.groupValues[1].toIntOrNull() ?: return@let null
            val seconds = match.groupValues[2].toIntOrNull() ?: return@let null
            minutes * 60 + seconds
        }
        val explicitRest = REST.find(lowered)?.groupValues?.get(1)?.toIntOrNull()
        val targetTimeSeconds = PaceModel.intervalTargetSeconds(pacePer100Seconds, distanceMeters)
        val restSeconds = explicitRest
            ?: sendOffSeconds?.let { (it - targetTimeSeconds).coerceAtLeast(0) }
            ?: if (repetitions > 1) DEFAULT_REST_SECONDS else 0

        return SetBlock(
            distanceMeters = distanceMeters,
            repetitions = repetitions,
            stroke = stroke,
            targetTimeSeconds = targetTimeSeconds,
            restSeconds = restSeconds,
        )
    }

    private fun detectFocus(text: String): Focus? {
        val lowered = text.lowercase()
        return when {
            lowered.contains("sprint") || lowered.contains("speed") -> Focus.SPEED
            lowered.contains("threshold") || lowered.contains("css") -> Focus.THRESHOLD
            lowered.contains("technique") || lowered.contains("drill") -> Focus.TECHNIQUE
            lowered.contains("recovery") || lowered.contains("easy") -> Focus.RECOVERY
            lowered.contains("endurance") || lowered.contains("aerobic") -> Focus.ENDURANCE
            else -> null
        }
    }
}
