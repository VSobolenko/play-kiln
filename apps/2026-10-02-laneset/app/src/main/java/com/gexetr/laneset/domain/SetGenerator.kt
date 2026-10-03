package com.gexetr.laneset.domain

/**
 * Derives a whole session from the swimmer's own constraints.
 *
 * This is not a rotation over a fixed library. Cutting a set out of a fixed list is what the shelf's
 * own reviewers complain about ("the sets the app generates are very repetitive"), and it is exactly
 * what this generator does **not** do: the arrangement of distances, strokes, repetitions and rests
 * is a function of the constraints, driven by a deterministic seeded sequence, so a different pool
 * length, minute budget, focus, set of avoided strokes or set of gear on the deck produces a
 * different main set ([Session.mainSetBlocks]).
 *
 * The generator is also **deterministic**: the same constraints and the same pace always yield the
 * same session. That is what makes the promise testable — a test can assert that two different
 * constraint sets never agree on a main set without the arrangement changing under it.
 *
 * Invariants it holds:
 * - every emitted [SetBlock] carries a [SetBlock.targetTimeSeconds] derived from the threshold pace;
 * - a stroke named in [SessionConstraints.strokesToAvoid] is never emitted;
 * - only gear named in [SessionConstraints.gearOnDeck] is used;
 * - the estimated total is fitted inside [SessionConstraints.minutesAvailable] and stated on the
 *   returned [Session].
 */
object SetGenerator {

    /**
     * Builds the whole session for [constraints] at [pacePer100Seconds].
     *
     * Throws when the constraints are self-contradictory (no minutes, no pool, or every stroke
     * avoided), because a session cannot be invented from them and a silently empty main set would be
     * worse than a refusal the caller can act on.
     */
    fun generateSession(constraints: SessionConstraints, pacePer100Seconds: Double): Session {
        require(pacePer100Seconds > 0.0) {
            "pacePer100Seconds must be positive, was $pacePer100Seconds s"
        }
        require(constraints.poolLengthMeters > 0) {
            "poolLengthMeters must be positive, was ${constraints.poolLengthMeters}"
        }
        require(constraints.minutesAvailable > 0) {
            "minutesAvailable must be positive, was ${constraints.minutesAvailable}"
        }
        val allowedStrokes = Stroke.entries.filterNot { it in constraints.strokesToAvoid }
        require(allowedStrokes.isNotEmpty()) {
            "strokesToAvoid must leave at least one stroke: every stroke was excluded"
        }

        val context = GenerationContext(
            constraints = constraints,
            pacePer100Seconds = pacePer100Seconds,
            poolStepMeters = constraints.poolLengthMeters,
            budgetSeconds = constraints.minutesAvailable * 60,
            allowedStrokes = allowedStrokes,
            profile = profileFor(constraints.focus, constraints.poolLengthMeters),
            random = DeterministicRandom(seedFor(constraints, pacePer100Seconds)),
        )

        val warmUp = buildWarmUp(context).toMutableList()
        val coolDown = buildCoolDown(context).toMutableList()
        val mainBudgetSeconds = fitBookends(context.budgetSeconds, warmUp, coolDown)
        val mainSet = buildMainSet(context, mainBudgetSeconds)

        val sections = buildList {
            if (warmUp.isNotEmpty()) add(SessionSection(SectionKind.WARM_UP, warmUp.toList()))
            add(SessionSection(SectionKind.MAIN_SET, mainSet))
            if (coolDown.isNotEmpty()) add(SessionSection(SectionKind.COOL_DOWN, coolDown.toList()))
        }.let { trimToBudget(it, context.budgetSeconds) }

        return Session(
            constraints = constraints,
            pacePer100Seconds = pacePer100Seconds,
            sections = sections,
            estimatedTotalSeconds = sections.sumOf { it.totalSeconds },
            totalDistanceMeters = sections.sumOf { it.totalDistanceMeters },
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Bookends: a short easy swim in, a short easy swim out, and the main set between them.
    // ---------------------------------------------------------------------------------------------

    private fun buildWarmUp(context: GenerationContext): List<SetBlock> {
        val blocks = mutableListOf<SetBlock>()
        val baseDistance = when {
            context.budgetSeconds < 20 * 60 -> 100
            context.budgetSeconds < 35 * 60 -> 200
            else -> 300
        }
        val distance = snapToStep(baseDistance, context.poolStepMeters)
        val stroke = context.allowedStrokes.first()
        blocks += SetBlock(
            distanceMeters = distance,
            repetitions = 1,
            stroke = stroke,
            targetTimeSeconds = PaceModel.intervalTargetSeconds(
                context.pacePer100Seconds * WARM_UP_PACE_FACTOR,
                distance,
            ),
            restSeconds = 0,
        )

        // A kickboard on the deck earns an easy kick set *inside the warm-up*, never a stroke the
        // swimmer asked to avoid. The gear set is recorded on the block so the UI can label it.
        if (Gear.KICKBOARD in context.constraints.gearOnDeck) {
            val kickDistance = snapToStep(minOf(100, distance), context.poolStepMeters)
            val kickStroke = context.allowedStrokes[context.random.nextInt(context.allowedStrokes.size)]
            blocks += SetBlock(
                distanceMeters = kickDistance,
                repetitions = 4,
                stroke = kickStroke,
                targetTimeSeconds = PaceModel.intervalTargetSeconds(
                    context.pacePer100Seconds * WARM_UP_KICK_PACE_FACTOR,
                    kickDistance,
                ),
                restSeconds = 15,
                gear = setOf(Gear.KICKBOARD),
            )
        }
        return blocks
    }

    private fun buildCoolDown(context: GenerationContext): List<SetBlock> {
        val distance = snapToStep(if (context.budgetSeconds < 20 * 60) 100 else 200, context.poolStepMeters)
        val stroke = context.allowedStrokes[context.random.nextInt(context.allowedStrokes.size)]
        return listOf(
            SetBlock(
                distanceMeters = distance,
                repetitions = 1,
                stroke = stroke,
                targetTimeSeconds = PaceModel.intervalTargetSeconds(
                    context.pacePer100Seconds * COOL_DOWN_PACE_FACTOR,
                    distance,
                ),
                restSeconds = 0,
            ),
        )
    }

    /**
     * Shrinks the bookends until they take no more than half the budget, and returns what is left
     * for the main set. The main set is the reason the swimmer came, so it is never the part that
     * gives way first.
     */
    private fun fitBookends(
        budgetSeconds: Int,
        warmUp: MutableList<SetBlock>,
        coolDown: MutableList<SetBlock>,
    ): Int {
        fun durationOf(blocks: List<SetBlock>): Int = blocks.sumOf { it.totalSeconds }
        val maxBookends = budgetSeconds / 2

        while (durationOf(warmUp) + durationOf(coolDown) > maxBookends && coolDown.isNotEmpty()) {
            val last = coolDown.last()
            if (last.repetitions > 1) {
                coolDown[coolDown.lastIndex] = last.copy(repetitions = 1)
            } else {
                coolDown.removeAt(coolDown.lastIndex)
            }
        }
        while (durationOf(warmUp) + durationOf(coolDown) > maxBookends && warmUp.size > 1) {
            warmUp.removeAt(warmUp.lastIndex)
        }
        if (durationOf(warmUp) + durationOf(coolDown) > maxBookends && warmUp.isNotEmpty()) {
            val only = warmUp[0]
            if (only.repetitions > 1) warmUp[0] = only.copy(repetitions = 1)
        }
        return (budgetSeconds - durationOf(warmUp) - durationOf(coolDown)).coerceAtLeast(0)
    }

    // ---------------------------------------------------------------------------------------------
    // The main set: the part the negative control in the tests compares.
    // ---------------------------------------------------------------------------------------------

    private fun buildMainSet(context: GenerationContext, budgetSeconds: Int): List<SetBlock> {
        if (budgetSeconds <= 0) return emptyList()

        val blocks = mutableListOf<SetBlock>()
        var remaining = budgetSeconds

        // 1. One set per piece of gear actually on the deck, so declaring gear changes the session.
        for (gear in context.constraints.gearOnDeck.sortedBy { it.name }) {
            val fitted = fitBlock(gearBlock(context, gear), remaining)
                ?: continue
            blocks += fitted
            remaining -= fitted.totalSeconds
        }

        // 2. The stroke sets. The count falls out of the minute budget, and the distance, stroke,
        //    repetitions and rest all come from the seeded sequence, so the arrangement is derived
        //    and not drawn from a list.
        val targetBlockCount = (context.constraints.minutesAvailable / 12).coerceIn(2, 7)
        var attempts = 0
        while (blocks.size < targetBlockCount && remaining > 0 && attempts < targetBlockCount * 4) {
            attempts++
            val distance = context.profile.distances[context.random.nextInt(context.profile.distances.size)]
            val stroke = context.allowedStrokes[context.random.nextInt(context.allowedStrokes.size)]
            val repetitions = randomIn(context.random, context.profile.repetitions)
            val rest = randomIn(context.random, context.profile.restSeconds)
            val candidate = SetBlock(
                distanceMeters = distance,
                repetitions = repetitions,
                stroke = stroke,
                targetTimeSeconds = PaceModel.intervalTargetSeconds(
                    context.pacePer100Seconds * context.profile.paceFactor,
                    distance,
                ),
                restSeconds = rest,
            )
            val fitted = fitBlock(candidate, remaining) ?: continue
            blocks += fitted
            remaining -= fitted.totalSeconds
        }
        return blocks
    }

    private fun gearBlock(context: GenerationContext, gear: Gear): SetBlock {
        val recipe = gearRecipe(gear)
        val distance = snapToStep(recipe.distanceMeters, context.poolStepMeters)
        val stroke = if (gear == Gear.PULL_BUOY || gear == Gear.PADDLES) {
            // A pull set is freestyle by definition when freestyle is permitted; otherwise there is
            // no honest pull to program and the first allowed stroke stands in.
            Stroke.FREESTYLE.takeIf { it in context.allowedStrokes } ?: context.allowedStrokes.first()
        } else {
            context.allowedStrokes[context.random.nextInt(context.allowedStrokes.size)]
        }
        return SetBlock(
            distanceMeters = distance,
            repetitions = recipe.repetitions,
            stroke = stroke,
            targetTimeSeconds = PaceModel.intervalTargetSeconds(
                context.pacePer100Seconds * recipe.paceFactor,
                distance,
            ),
            restSeconds = recipe.restSeconds,
            gear = setOf(gear),
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Fitting, snapping and the deterministic sequence.
    // ---------------------------------------------------------------------------------------------

    /**
     * Returns [block] unchanged when it fits [remainingSeconds], otherwise the same block with fewer
     * repetitions; `null` when not even one repetition fits. Trimming repetitions — rather than
     * trimming the rest or the target — keeps the pace the swimmer is meant to hold intact.
     */
    private fun fitBlock(block: SetBlock, remainingSeconds: Int): SetBlock? {
        if (block.totalSeconds <= remainingSeconds) return block
        val perRepetition = block.targetTimeSeconds + block.restSeconds
        if (perRepetition <= 0) return null
        val repetitions = remainingSeconds / perRepetition
        return if (repetitions >= 1) block.copy(repetitions = repetitions) else null
    }

    /** Rounds a distance down to a whole number of pool lengths, never below one length. */
    private fun snapToStep(meters: Int, stepMeters: Int): Int {
        val snapped = (meters / stepMeters) * stepMeters
        return if (snapped < stepMeters) stepMeters else snapped
    }

    private fun randomIn(random: DeterministicRandom, range: IntRange): Int {
        val span = range.last - range.first + 1
        return range.first + random.nextInt(span)
    }

    /**
     * A last safety net: if the fitted sections still overflow the budget — only reachable with an
     * unusually small budget — the cool-down goes first and then the warm-up shrinks, block by block.
     * The main set is never touched: the promise is that a session fits the minutes honestly, and
     * trimming the part the swimmer came for would be the dishonest way to make it fit.
     */
    private fun trimToBudget(sections: List<SessionSection>, budgetSeconds: Int): List<SessionSection> {
        var result = sections
        fun totalSeconds(): Int = result.sumOf { it.totalSeconds }
        if (totalSeconds() <= budgetSeconds) return result

        for (kind in listOf(SectionKind.COOL_DOWN, SectionKind.WARM_UP)) {
            while (totalSeconds() > budgetSeconds) {
                val section = result.firstOrNull { it.kind == kind } ?: break
                if (section.blocks.isEmpty()) {
                    result = result.filterNot { it.kind == kind }
                    break
                }
                val reduced = reduceSection(section)
                result = if (reduced == null) {
                    result.filterNot { it.kind == kind }
                } else {
                    result.map { if (it.kind == kind) reduced else it }
                }
            }
        }
        return result
    }

    /** Removes one repetition from the longest block, or drops that block when it has one left. */
    private fun reduceSection(section: SessionSection): SessionSection? {
        val blocks = section.blocks
        if (blocks.isEmpty()) return null
        val index = blocks.indices.maxByOrNull { blocks[it].totalSeconds } ?: return null
        val block = blocks[index]
        val reduced = blocks.toMutableList()
        if (block.repetitions > 1) {
            reduced[index] = block.copy(repetitions = block.repetitions - 1)
        } else {
            reduced.removeAt(index)
        }
        return if (reduced.isEmpty()) null else section.copy(blocks = reduced)
    }

    /**
     * A canonical string of every constraint, hashed with FNV-1a into the seed of the sequence.
     * Two constraint sets that differ in any field get different seeds, which is how "the main set
     * changes when the constraints change" is made true rather than hoped for.
     */
    private fun seedFor(constraints: SessionConstraints, pacePer100Seconds: Double): Int {
        val key = buildString {
            append(constraints.poolLengthMeters).append('|')
            append(constraints.minutesAvailable).append('|')
            append(constraints.focus.name).append('|')
            append(constraints.strokesToAvoid.map { it.name }.sorted().joinToString(",")).append('|')
            append(constraints.gearOnDeck.map { it.name }.sorted().joinToString(",")).append('|')
            append((pacePer100Seconds * 100).toInt())
        }
        var hash = 0x811C9DC5.toInt()
        for (character in key) {
            hash = hash xor character.code
            hash *= 16777619
        }
        return hash
    }

    // ---------------------------------------------------------------------------------------------
    // Small immutable value types.
    // ---------------------------------------------------------------------------------------------

    private const val WARM_UP_PACE_FACTOR = 1.15
    private const val WARM_UP_KICK_PACE_FACTOR = 1.25
    private const val COOL_DOWN_PACE_FACTOR = 1.25

    private class GenerationContext(
        val constraints: SessionConstraints,
        val pacePer100Seconds: Double,
        val poolStepMeters: Int,
        val budgetSeconds: Int,
        val allowedStrokes: List<Stroke>,
        val profile: FocusProfile,
        val random: DeterministicRandom,
    )

    /** The shape of a main set for one focus, before it is snapped to the pool length. */
    private data class FocusProfile(
        val distances: List<Int>,
        val repetitions: IntRange,
        val restSeconds: IntRange,
        val paceFactor: Double,
    )

    private data class GearRecipe(
        val distanceMeters: Int,
        val repetitions: Int,
        val paceFactor: Double,
        val restSeconds: Int,
    )

    private fun profileFor(focus: Focus, poolStepMeters: Int): FocusProfile {
        val base = when (focus) {
            Focus.SPEED -> FocusProfile(
                distances = listOf(25, 50, 75, 100, 150),
                repetitions = 6..16,
                restSeconds = 15..40,
                paceFactor = 0.95,
            )
            Focus.THRESHOLD -> FocusProfile(
                distances = listOf(50, 75, 100, 150, 200, 300),
                repetitions = 3..8,
                restSeconds = 10..25,
                paceFactor = 1.00,
            )
            Focus.ENDURANCE -> FocusProfile(
                distances = listOf(100, 200, 300, 400, 500),
                repetitions = 2..6,
                restSeconds = 15..30,
                paceFactor = 1.05,
            )
            Focus.TECHNIQUE -> FocusProfile(
                distances = listOf(25, 50, 75, 100),
                repetitions = 4..10,
                restSeconds = 20..40,
                paceFactor = 1.20,
            )
            Focus.RECOVERY -> FocusProfile(
                distances = listOf(50, 100, 200, 300),
                repetitions = 2..5,
                restSeconds = 20..40,
                paceFactor = 1.25,
            )
        }
        val distances = base.distances
            .map { snapToStep(it, poolStepMeters) }
            .filter { it >= poolStepMeters }
            .distinct()
            .sorted()
        return base.copy(distances = distances)
    }

    private fun gearRecipe(gear: Gear): GearRecipe = when (gear) {
        Gear.KICKBOARD -> GearRecipe(distanceMeters = 50, repetitions = 6, paceFactor = 1.20, restSeconds = 15)
        Gear.PULL_BUOY -> GearRecipe(distanceMeters = 100, repetitions = 6, paceFactor = 1.00, restSeconds = 15)
        Gear.FINS -> GearRecipe(distanceMeters = 100, repetitions = 6, paceFactor = 0.90, restSeconds = 15)
        Gear.PADDLES -> GearRecipe(distanceMeters = 100, repetitions = 4, paceFactor = 0.97, restSeconds = 20)
        Gear.SNORKEL -> GearRecipe(distanceMeters = 50, repetitions = 8, paceFactor = 1.15, restSeconds = 20)
    }

    /**
     * A tiny xorshift32 sequence, written out rather than taken from `kotlin.random.Random`.
     *
     * The generator's contract is "identical input, identical output", and an explicit algorithm
     * makes that true across every Kotlin version and platform without relying on a library's
     * seeding guarantees. A seed of zero is remapped because xorshift32 is stuck at zero otherwise.
     */
    private class DeterministicRandom(seed: Int) {
        private var state: Int = if (seed == 0) 0x9E3779B9.toInt() else seed

        fun nextInt(bound: Int): Int {
            require(bound > 0) { "bound must be positive, was $bound" }
            state = state xor (state shl 13)
            state = state xor (state ushr 17)
            state = state xor (state shl 5)
            return (state and 0x7FFFFFFF) % bound
        }
    }
}
