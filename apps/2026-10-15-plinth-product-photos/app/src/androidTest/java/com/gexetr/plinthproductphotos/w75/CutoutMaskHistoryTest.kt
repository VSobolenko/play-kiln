package com.gexetr.plinthproductphotos.w75

import com.gexetr.plinthproductphotos.ui.cutout.MaskEditHistory
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CutoutMaskHistoryTest {

    @Test
    fun undoAndRedoRestoreExactMaskSnapshots() {
        val history = MaskEditHistory()
        val original = byteArrayOf(0.toByte(), 32.toByte(), 255.toByte(), 128.toByte())
        val edited = byteArrayOf(255.toByte(), 32.toByte(), 128.toByte(), 0.toByte())
        history.record(original)

        assertArrayEquals(original, history.undo(edited))
        assertTrue(history.canRedo)
        assertArrayEquals(edited, history.redo(original))
        assertTrue(history.canUndo)
        assertFalse(history.canRedo)
    }

    @Test
    fun aNewStrokeAfterUndoDiscardsTheRedoBranch() {
        val history = MaskEditHistory()
        val original = byteArrayOf(0.toByte(), 255.toByte())
        val firstEdit = byteArrayOf(255.toByte(), 255.toByte())
        val secondEdit = byteArrayOf(0.toByte(), 0.toByte())
        history.record(original)

        assertArrayEquals(original, history.undo(firstEdit))
        history.record(original)

        assertFalse(history.canRedo)
        assertNull(history.redo(secondEdit))
    }
}
