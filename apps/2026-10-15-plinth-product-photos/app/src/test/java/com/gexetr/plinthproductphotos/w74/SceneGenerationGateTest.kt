package com.gexetr.plinthproductphotos.w74

import com.gexetr.plinthproductphotos.data.api.ApiConfigResponse
import com.gexetr.plinthproductphotos.data.api.OperationConfig
import com.gexetr.plinthproductphotos.data.api.OperationId
import com.gexetr.plinthproductphotos.ui.autocard.sceneGenerationGate
import com.gexetr.plinthproductphotos.ui.studio.StudioState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneGenerationGateTest {
    private val ready = StudioState(
        photoId = "chosen-phone",
        balance = 7,
        config = ApiConfigResponse(
            ops = mapOf("O-07" to OperationConfig(credits = 2, enabled = true)),
            starterCredits = 7,
            killSwitch = false,
            minAppVersion = "0.1.0",
            dailyCapReached = false,
        ),
    )

    @Test
    fun selectedPhotoCanGenerateWithoutCategoryOrRecognition() {
        val gate = sceneGenerationGate(ready, "chosen-phone", "")
        assertTrue(gate.canGenerate)
        assertEquals(2, gate.credits)
        assertFalse(gate.needsCredits)
    }

    @Test
    fun differentOrMissingSelectedPhotoCannotGenerate() {
        assertFalse(sceneGenerationGate(ready, "other-phone", "").canGenerate)
        assertFalse(sceneGenerationGate(ready.copy(photoId = null), "chosen-phone", "").canGenerate)
    }

    @Test
    fun pendingWorkForAnotherPhotoOrToolCannotMasqueradeAsThisScene() {
        val pending = ready.copy(hasPending = true, operation = OperationId.GENERATED_SCENE)
        val own = sceneGenerationGate(pending, "chosen-phone", "")
        assertFalse(own.canGenerate)
        assertFalse(own.conflictingPending)
        assertTrue(sceneGenerationGate(pending, "other-phone", "").conflictingPending)
        assertTrue(
            sceneGenerationGate(pending.copy(operation = OperationId.RELIGHT), "chosen-phone", "")
                .conflictingPending,
        )
    }

    @Test
    fun unavailableOrUnfundedGenerationNeverFallsBackToALocalCard() {
        val config = requireNotNull(ready.config)
        for (state in listOf(
            ready.copy(config = null),
            ready.copy(config = config.copy(killSwitch = true)),
            ready.copy(config = config.copy(dailyCapReached = true)),
            ready.copy(config = config.copy(ops = emptyMap())),
            ready.copy(config = config.copy(ops = mapOf("O-07" to OperationConfig(2, false)))),
            ready.copy(config = config.copy(ops = mapOf("O-07" to OperationConfig(0, true)))),
            ready.copy(balance = null),
            ready.copy(balance = 1),
            ready.copy(busy = true),
            ready.copy(errorCode = "PRICE_CHANGED"),
        )) {
            assertFalse(sceneGenerationGate(state, "chosen-phone", "").canGenerate)
        }
        assertTrue(sceneGenerationGate(ready.copy(balance = 1), "chosen-phone", "").needsCredits)
        assertTrue(sceneGenerationGate(ready.copy(balance = 2), "chosen-phone", "").canGenerate)
    }

    @Test
    fun guidanceIsOptionalButKeepsTheExistingStudioLengthLimit() {
        assertTrue(sceneGenerationGate(ready, "chosen-phone", " ").canGenerate)
        assertTrue(sceneGenerationGate(ready, "chosen-phone", "a".repeat(2000)).canGenerate)
        assertFalse(sceneGenerationGate(ready, "chosen-phone", "a".repeat(2001)).canGenerate)
    }
}
