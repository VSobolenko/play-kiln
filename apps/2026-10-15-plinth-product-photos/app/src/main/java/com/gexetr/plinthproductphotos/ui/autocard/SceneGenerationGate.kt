package com.gexetr.plinthproductphotos.ui.autocard

import com.gexetr.plinthproductphotos.data.api.OperationId
import com.gexetr.plinthproductphotos.ui.studio.StudioState

internal data class SceneGenerationGate(
    val credits: Int?,
    val serviceAvailable: Boolean,
    val needsCredits: Boolean,
    val conflictingPending: Boolean,
    val canGenerate: Boolean,
)

internal fun sceneGenerationGate(
    state: StudioState,
    photoId: String,
    guidance: String,
): SceneGenerationGate {
    val config = state.config
    val quote = config?.ops?.get("O-07")
    val credits = quote?.credits?.takeIf { it > 0 }
    val available = quote?.enabled == true && credits != null &&
        config?.killSwitch == false && config.dailyCapReached == false
    val funded = credits != null && state.balance?.let { it >= credits } == true
    return SceneGenerationGate(
        credits = credits,
        serviceAvailable = available,
        needsCredits = available && state.balance != null && !funded,
        conflictingPending = state.hasPending &&
            (state.photoId != photoId || state.operation != OperationId.GENERATED_SCENE),
        canGenerate = available && funded && state.photoId == photoId &&
            !state.busy && !state.hasPending && state.result == null &&
            state.errorCode == null && guidance.length <= 2000,
    )
}
