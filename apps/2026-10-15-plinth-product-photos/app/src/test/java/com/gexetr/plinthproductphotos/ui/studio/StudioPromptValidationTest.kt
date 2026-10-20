package com.gexetr.plinthproductphotos.ui.studio

import com.gexetr.plinthproductphotos.data.api.OperationId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioPromptValidationTest {
    @Test
    fun generatedSceneRequiresNonBlankDirection() {
        assertFalse(studioPromptIsValid(OperationId.GENERATED_SCENE, "  "))
        assertTrue(studioPromptIsValid(OperationId.GENERATED_SCENE, "forest by the sea"))
    }

    @Test
    fun otherGenerativeToolsKeepTheirPromptOptional() {
        assertTrue(studioPromptIsValid(OperationId.RELIGHT, ""))
        assertTrue(studioPromptIsValid(OperationId.RECOLOUR, ""))
    }

    @Test
    fun instructionCannotExceedTheStudioLimit() {
        assertFalse(studioPromptIsValid(OperationId.RELIGHT, "x".repeat(STUDIO_INSTRUCTION_MAX_LENGTH + 1)))
    }

    @Test
    fun liveCatalogueListsEveryAdvertisedOperationTheViewModelCanStart() {
        // Expand (O-09) is on the catalogue now that the client supplies a real target canvas
        // and the server enables it; every requestable operation must be reachable from the UI.
        assertEquals(
            StudioViewModel.REQUEST_OPERATIONS,
            studioRows.map { it.operation }.toSet(),
        )
    }
}
