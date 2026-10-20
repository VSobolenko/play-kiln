package com.gexetr.plinthproductphotos.domain.autocard

import com.gexetr.plinthproductphotos.domain.model.BackgroundSpec

enum class AutoCardFallbackReason {
    OFFLINE,
    RECOGNITION_UNAVAILABLE,
}

data class AutoCardFlowState(
    val category: AutoCategory? = null,
    val recognition: AutoCardRecognition? = null,
    val attributes: List<String> = recognition?.attributes.orEmpty(),
    val fallbackReason: AutoCardFallbackReason? = null,
    val isRecognizing: Boolean = false,
    val categoryEdited: Boolean = false,
) {
    val isSuggested: Boolean
        get() = recognition != null && !categoryEdited

    fun recipes(catalog: AutoCardSceneCatalog): List<AutoCardSceneRecipe> =
        category?.let(catalog::recipesFor).orEmpty()
            .filter { it.background.kind != BackgroundSpec.Kind.GENERATED_SCENE }

    fun chooseCategory(value: AutoCategory?): AutoCardFlowState =
        copy(category = value, isRecognizing = false, categoryEdited = true)

    fun editAttributes(value: List<String>): AutoCardFlowState =
        copy(attributes = value)

    companion object {
        fun recognizing() = AutoCardFlowState(isRecognizing = true)

        fun recognized(value: AutoCardRecognition) =
            if (value.confidence <= 0f || !value.confidence.isFinite()) failed()
            else AutoCardFlowState(category = value.category, recognition = value)

        fun offline() = AutoCardFlowState(fallbackReason = AutoCardFallbackReason.OFFLINE)

        fun failed(reason: AutoCardFallbackReason = AutoCardFallbackReason.RECOGNITION_UNAVAILABLE) =
            AutoCardFlowState(fallbackReason = reason)
    }
}
