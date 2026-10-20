package com.gexetr.plinthproductphotos.domain.studio

import com.gexetr.plinthproductphotos.domain.model.Provenance
import com.gexetr.plinthproductphotos.domain.presets.EditPolicy
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePreset

enum class StudioOperationId(val wireValue: String) {
    RECOGNISE("O-01"),
    CUT_OUT("O-02"),
    REFINE_MASK("O-03"),
    COMPOSE_ORIGINAL("O-04"),
    SHADOW_MARGIN_FILL("O-05"),
    LOCAL_BACKGROUND("O-06"),
    GENERATED_SCENE("O-07"),
    RELIGHT("O-08"),
    EXPAND_CANVAS("O-09"),
    RECOLOUR("O-10"),
    UPSCALE("O-11"),
    ERASE_REGION("O-12"),
    GHOST_MANNEQUIN("O-13"),
    VIRTUAL_MODEL("O-14"),
    LISTING_TEXT("O-15"),
    SERIES("O-16"),
    SLIDE_RENDER("O-17"),
    EXPORT_PACK("O-18"),
}

enum class CreditTier(val credits: Int) {
    LIGHT(1),
    STANDARD(2),
    PREMIUM(5),
}

enum class UpscaleResolution {
    TWO_K,
    FOUR_K,
}

enum class OperationUnavailableReason {
    ORIGINAL_ONLY_MARKETPLACE,
    ORIGINAL_ONLY_OPERATION,
    FIDELITY_THRESHOLDS_UNMEASURED,
    OPERATION_DISABLED,
    SERVER_OPERATION_DISABLED,
    BACKGROUND_POLICY,
    WHITE_MAIN_BACKGROUND,
    REAL_PHOTO_REQUIRED,
    SLIDE_OVERLAY_NOT_ALLOWED,
    GHOST_MANNEQUIN_ROUTE_MISSING,
    VIRTUAL_MODEL_REVIEW_PENDING,
    SERIES_VARIANT_BLOCKED,
    PRESET_REQUIRED,
}

enum class MarketplaceGateRule {
    NONE,
    ORIGINAL_ONLY,
    ORIGINAL_ONLY_RETURNS_SOURCE,
    BACKGROUND_POLICY,
    GENERATED_BACKGROUND,
    ENHANCED_MAIN_POLICY,
    REAL_PHOTO_MAIN,
    PER_PRESET_OVERLAY,
    AS_VARIANT,
    PER_PRESET_EXPORT,
}

data class StudioOperation(
    val id: StudioOperationId,
    val nameResourceKey: String,
    val costRule: OperationCostRule,
    val outputProvenance: Provenance?,
    val requiresServer: Boolean = false,
    val marketplaceGates: Set<MarketplaceGateRule> = setOf(MarketplaceGateRule.NONE),
)

sealed interface OperationCostRule {
    data class Fixed(val credits: Int) : OperationCostRule
    data class Tier(val tier: CreditTier) : OperationCostRule
    data class PerScene(val tier: CreditTier) : OperationCostRule
    data class PerItem(val tier: CreditTier) : OperationCostRule
    data class PerVariant(val tier: CreditTier) : OperationCostRule
    data object Upscale : OperationCostRule
    data object Series : OperationCostRule
}

data class StudioOperationRequest(
    val operationId: StudioOperationId,
    val variantCount: Int = 1,
    val seriesOperation: StudioOperationId = StudioOperationId.RECOLOUR,
    val upscaleResolution: UpscaleResolution = UpscaleResolution.TWO_K,
) {
    init {
        require(variantCount > 0) { "Variant count must be positive" }
        require(operationId != StudioOperationId.SERIES ||
            seriesOperation == StudioOperationId.RECOLOUR ||
            seriesOperation == StudioOperationId.VIRTUAL_MODEL
        ) { "Series can contain only O-10 or O-14 variants" }
    }
}

data class OperationCost(
    val credits: Int,
    val unitCredits: Int,
    val quantity: Int,
)

sealed interface OperationAccess {
    data object Available : OperationAccess
    data class Unavailable(val reasons: List<OperationUnavailableReason>) : OperationAccess {
        init {
            require(reasons.isNotEmpty()) { "Unavailable operations need a reason" }
        }
    }

    data class InsufficientCredits(val shortfallCredits: Int) : OperationAccess {
        init {
            require(shortfallCredits > 0) { "Shortfall must be positive" }
        }
    }
}

data class OperationOffer(
    val operation: StudioOperation,
    val request: StudioOperationRequest,
    val cost: OperationCost,
    val balanceBeforeCredits: Int,
    val balanceAfterCredits: Int?,
    val access: OperationAccess,
    val returnOriginalSource: Boolean,
) {
    init {
        require(balanceBeforeCredits >= 0) { "Credit balance cannot be negative" }
        require(cost.credits >= 0) { "Operation cost cannot be negative" }
    }
}

data class StudioOperationConfig(
    val enabledOperationIds: Set<StudioOperationId> = emptySet(),
    val measuredFidelityOperationIds: Set<StudioOperationId> = emptySet(),
    val ghostMannequinRouteAvailable: Boolean = false,
    val virtualModelClearedByReview: Boolean = false,
)

sealed interface OperationExecution<out T> {
    data class Produced<T>(val result: T) : OperationExecution<T>
    data class ReturnedOriginal<T>(val source: T) : OperationExecution<T>
    data class Unavailable(val reasons: List<OperationUnavailableReason>) : OperationExecution<Nothing>
    data class InsufficientCredits(val shortfallCredits: Int) : OperationExecution<Nothing>
}

object StudioOperationCatalog {
    private val tierValues = CreditTier.entries.associateWith(CreditTier::credits)

    val operations: List<StudioOperation> = listOf(
        StudioOperation(
            StudioOperationId.RECOGNISE,
            "studio_operation_recognise",
            OperationCostRule.Fixed(0),
            null,
            true,
        ),
        StudioOperation(
            StudioOperationId.CUT_OUT,
            "studio_operation_cut_out",
            OperationCostRule.Tier(CreditTier.LIGHT),
            null,
            true,
            setOf(MarketplaceGateRule.ORIGINAL_ONLY),
        ),
        StudioOperation(
            StudioOperationId.REFINE_MASK,
            "studio_operation_refine_mask",
            OperationCostRule.Fixed(0),
            null,
            marketplaceGates = setOf(MarketplaceGateRule.ORIGINAL_ONLY),
        ),
        StudioOperation(
            StudioOperationId.COMPOSE_ORIGINAL,
            "studio_operation_compose_original",
            OperationCostRule.Fixed(0),
            Provenance.ORIGINAL_PIXELS,
            marketplaceGates = setOf(MarketplaceGateRule.ORIGINAL_ONLY_RETURNS_SOURCE),
        ),
        StudioOperation(
            StudioOperationId.SHADOW_MARGIN_FILL,
            "studio_operation_shadow_margin_fill",
            OperationCostRule.Fixed(0),
            Provenance.ORIGINAL_PIXELS,
            marketplaceGates = setOf(MarketplaceGateRule.ORIGINAL_ONLY_RETURNS_SOURCE),
        ),
        StudioOperation(
            StudioOperationId.LOCAL_BACKGROUND,
            "studio_operation_local_background",
            OperationCostRule.Fixed(0),
            Provenance.ORIGINAL_PIXELS,
            marketplaceGates = setOf(
                MarketplaceGateRule.ORIGINAL_ONLY,
                MarketplaceGateRule.BACKGROUND_POLICY,
            ),
        ),
        StudioOperation(
            StudioOperationId.GENERATED_SCENE,
            "studio_operation_generated_scene",
            OperationCostRule.PerScene(CreditTier.STANDARD),
            Provenance.COMPOSITE_GENERATED_BACKGROUND,
            true,
            setOf(MarketplaceGateRule.ORIGINAL_ONLY, MarketplaceGateRule.GENERATED_BACKGROUND),
        ),
        StudioOperation(
            StudioOperationId.RELIGHT,
            "studio_operation_relight",
            OperationCostRule.Tier(CreditTier.PREMIUM),
            Provenance.ENHANCED,
            true,
            setOf(MarketplaceGateRule.ORIGINAL_ONLY, MarketplaceGateRule.ENHANCED_MAIN_POLICY),
        ),
        StudioOperation(
            StudioOperationId.EXPAND_CANVAS,
            "studio_operation_expand_canvas",
            OperationCostRule.Tier(CreditTier.STANDARD),
            Provenance.COMPOSITE_GENERATED_BACKGROUND,
            true,
            setOf(MarketplaceGateRule.ORIGINAL_ONLY, MarketplaceGateRule.GENERATED_BACKGROUND),
        ),
        StudioOperation(
            StudioOperationId.RECOLOUR,
            "studio_operation_recolour",
            OperationCostRule.PerVariant(CreditTier.STANDARD),
            Provenance.ENHANCED,
            true,
            setOf(MarketplaceGateRule.ORIGINAL_ONLY, MarketplaceGateRule.REAL_PHOTO_MAIN),
        ),
        StudioOperation(
            StudioOperationId.UPSCALE,
            "studio_operation_upscale",
            OperationCostRule.Upscale,
            Provenance.ENHANCED,
            true,
            setOf(MarketplaceGateRule.ORIGINAL_ONLY),
        ),
        StudioOperation(
            StudioOperationId.ERASE_REGION,
            "studio_operation_erase_region",
            OperationCostRule.Tier(CreditTier.STANDARD),
            Provenance.ENHANCED,
            true,
            setOf(MarketplaceGateRule.ORIGINAL_ONLY, MarketplaceGateRule.ENHANCED_MAIN_POLICY),
        ),
        StudioOperation(
            StudioOperationId.GHOST_MANNEQUIN,
            "studio_operation_ghost_mannequin",
            OperationCostRule.Tier(CreditTier.PREMIUM),
            Provenance.ENHANCED,
            true,
            setOf(MarketplaceGateRule.ORIGINAL_ONLY, MarketplaceGateRule.ENHANCED_MAIN_POLICY),
        ),
        StudioOperation(
            StudioOperationId.VIRTUAL_MODEL,
            "studio_operation_virtual_model",
            OperationCostRule.PerVariant(CreditTier.PREMIUM),
            Provenance.ENHANCED,
            true,
            setOf(MarketplaceGateRule.ORIGINAL_ONLY),
        ),
        StudioOperation(
            StudioOperationId.LISTING_TEXT,
            "studio_operation_listing_text",
            OperationCostRule.PerItem(CreditTier.LIGHT),
            null,
            true,
        ),
        StudioOperation(
            StudioOperationId.SERIES,
            "studio_operation_series",
            OperationCostRule.Series,
            null,
            true,
            setOf(MarketplaceGateRule.AS_VARIANT),
        ),
        StudioOperation(
            StudioOperationId.SLIDE_RENDER,
            "studio_operation_slide_render",
            OperationCostRule.Fixed(0),
            null,
            marketplaceGates = setOf(
                MarketplaceGateRule.ORIGINAL_ONLY,
                MarketplaceGateRule.PER_PRESET_OVERLAY,
            ),
        ),
        StudioOperation(
            StudioOperationId.EXPORT_PACK,
            "studio_operation_export_pack",
            OperationCostRule.Fixed(0),
            null,
            marketplaceGates = setOf(MarketplaceGateRule.PER_PRESET_EXPORT),
        ),
    )

    private val byId = operations.associateBy(StudioOperation::id)

    val enhancedOperations: List<StudioOperation> =
        operations.filter { it.outputProvenance == Provenance.ENHANCED }

    fun provenanceFor(request: StudioOperationRequest): Provenance? =
        operation(if (request.operationId == StudioOperationId.SERIES) request.seriesOperation else request.operationId)
            .outputProvenance

    fun costFor(request: StudioOperationRequest): OperationCost {
        val operation = operation(request.operationId)
        val unitCredits = when (val rule = operation.costRule) {
            is OperationCostRule.Fixed -> rule.credits
            is OperationCostRule.Tier -> tierValues.getValue(rule.tier)
            is OperationCostRule.PerScene -> tierValues.getValue(rule.tier)
            is OperationCostRule.PerItem -> tierValues.getValue(rule.tier)
            is OperationCostRule.PerVariant -> tierValues.getValue(rule.tier)
            OperationCostRule.Upscale -> when (request.upscaleResolution) {
                UpscaleResolution.TWO_K -> tierValues.getValue(CreditTier.LIGHT)
                UpscaleResolution.FOUR_K -> tierValues.getValue(CreditTier.STANDARD)
            }
            OperationCostRule.Series -> costFor(
                StudioOperationRequest(
                    operationId = request.seriesOperation,
                    variantCount = 1,
                    upscaleResolution = request.upscaleResolution,
                ),
            ).unitCredits
        }
        val quantity = when (operation.costRule) {
            is OperationCostRule.PerVariant -> request.variantCount
            OperationCostRule.Series -> request.variantCount
            else -> 1
        }
        return OperationCost(
            credits = Math.multiplyExact(unitCredits, quantity),
            unitCredits = unitCredits,
            quantity = quantity,
        )
    }

    fun offer(
        request: StudioOperationRequest,
        balanceCredits: Int,
        preset: MarketplacePreset? = null,
        config: StudioOperationConfig = StudioOperationConfig(),
        isMainSlot: Boolean = true,
        mainRequiresRealPhotograph: Boolean = false,
        backgroundPolicySatisfied: Boolean = false,
    ): OperationOffer {
        require(balanceCredits >= 0) { "Credit balance cannot be negative" }
        val operation = operation(request.operationId)
        val cost = costFor(request)
        val reasons = unavailableReasons(
            request = request,
            preset = preset,
            config = config,
            isMainSlot = isMainSlot,
            mainRequiresRealPhotograph = mainRequiresRealPhotograph,
            backgroundPolicySatisfied = backgroundPolicySatisfied,
        )
        val access = when {
            reasons.isNotEmpty() -> OperationAccess.Unavailable(reasons)
            balanceCredits < cost.credits -> OperationAccess.InsufficientCredits(cost.credits - balanceCredits)
            else -> OperationAccess.Available
        }
        return OperationOffer(
            operation = operation,
            request = request,
            cost = cost,
            balanceBeforeCredits = balanceCredits,
            balanceAfterCredits = if (balanceCredits >= cost.credits) balanceCredits - cost.credits else null,
            access = access,
            returnOriginalSource = preset?.editPolicy == EditPolicy.ORIGINAL_ONLY &&
                MarketplaceGateRule.ORIGINAL_ONLY_RETURNS_SOURCE in operation.marketplaceGates,
        )
    }

    fun <T> execute(
        offer: OperationOffer,
        source: T,
        transform: (T) -> T,
    ): OperationExecution<T> {
        if (offer.returnOriginalSource && offer.operation.outputProvenance == Provenance.ORIGINAL_PIXELS) {
            return OperationExecution.ReturnedOriginal(source)
        }
        return when (val access = offer.access) {
            OperationAccess.Available -> OperationExecution.Produced(transform(source))
            is OperationAccess.Unavailable -> OperationExecution.Unavailable(access.reasons)
            is OperationAccess.InsufficientCredits -> OperationExecution.InsufficientCredits(access.shortfallCredits)
        }
    }

    private fun unavailableReasons(
        request: StudioOperationRequest,
        preset: MarketplacePreset?,
        config: StudioOperationConfig,
        isMainSlot: Boolean,
        mainRequiresRealPhotograph: Boolean,
        backgroundPolicySatisfied: Boolean,
    ): List<OperationUnavailableReason> = buildList {
        val operation = operation(request.operationId)
        val id = operation.id

        if (id == StudioOperationId.SERIES) {
            val child = operation(request.seriesOperation)
            if (child.outputProvenance != Provenance.ENHANCED) {
                add(OperationUnavailableReason.SERIES_VARIANT_BLOCKED)
            } else {
                addAll(
                    unavailableReasons(
                        request.copy(operationId = child.id, variantCount = 1),
                        preset,
                        config,
                        isMainSlot,
                        mainRequiresRealPhotograph,
                        backgroundPolicySatisfied,
                    ).filterNot { it == OperationUnavailableReason.SERVER_OPERATION_DISABLED },
                )
            }
        }

        if (operation.outputProvenance == Provenance.ENHANCED) {
            if (id !in config.measuredFidelityOperationIds) {
                add(OperationUnavailableReason.FIDELITY_THRESHOLDS_UNMEASURED)
            }
            if (id !in config.enabledOperationIds) add(OperationUnavailableReason.OPERATION_DISABLED)
            if (id == StudioOperationId.GHOST_MANNEQUIN && !config.ghostMannequinRouteAvailable) {
                add(OperationUnavailableReason.GHOST_MANNEQUIN_ROUTE_MISSING)
            }
            if (id == StudioOperationId.VIRTUAL_MODEL && !config.virtualModelClearedByReview) {
                add(OperationUnavailableReason.VIRTUAL_MODEL_REVIEW_PENDING)
            }
        } else if (operation.requiresServer && id !in config.enabledOperationIds) {
            add(OperationUnavailableReason.SERVER_OPERATION_DISABLED)
        }

        if (preset?.editPolicy == EditPolicy.ORIGINAL_ONLY &&
            MarketplaceGateRule.ORIGINAL_ONLY in operation.marketplaceGates
        ) {
            if (operation.outputProvenance == Provenance.ENHANCED) {
                add(OperationUnavailableReason.ORIGINAL_ONLY_MARKETPLACE)
            } else {
                add(OperationUnavailableReason.ORIGINAL_ONLY_OPERATION)
            }
        }
        if (preset?.editPolicy == EditPolicy.LIMITED_OVERLAY &&
            MarketplaceGateRule.PER_PRESET_OVERLAY in operation.marketplaceGates &&
            "slide" !in preset.allowedOverlays.orEmpty()
        ) {
            add(OperationUnavailableReason.SLIDE_OVERLAY_NOT_ALLOWED)
        }
        if (preset == null && operation.marketplaceGates.any {
                it == MarketplaceGateRule.PER_PRESET_OVERLAY || it == MarketplaceGateRule.PER_PRESET_EXPORT
            }
        ) {
            add(OperationUnavailableReason.PRESET_REQUIRED)
        }

        if (isMainSlot && preset?.main?.backgroundPolicy == "white-255" &&
            MarketplaceGateRule.GENERATED_BACKGROUND in operation.marketplaceGates
        ) {
            add(OperationUnavailableReason.WHITE_MAIN_BACKGROUND)
        }
        if (isMainSlot && preset?.main?.backgroundPolicy == "white-255" &&
            MarketplaceGateRule.ENHANCED_MAIN_POLICY in operation.marketplaceGates
        ) {
            add(OperationUnavailableReason.WHITE_MAIN_BACKGROUND)
        }
        if (isMainSlot && mainRequiresRealPhotograph &&
            MarketplaceGateRule.REAL_PHOTO_MAIN in operation.marketplaceGates
        ) {
            add(OperationUnavailableReason.REAL_PHOTO_REQUIRED)
        }
        if (MarketplaceGateRule.BACKGROUND_POLICY in operation.marketplaceGates &&
            preset?.main?.backgroundPolicy != null &&
            preset.main.backgroundPolicy != "any" &&
            !backgroundPolicySatisfied
        ) {
            add(OperationUnavailableReason.BACKGROUND_POLICY)
        }
    }.distinct()

    private fun operation(id: StudioOperationId): StudioOperation =
        byId[id] ?: error("No catalogue operation for ${id.wireValue}")
}
