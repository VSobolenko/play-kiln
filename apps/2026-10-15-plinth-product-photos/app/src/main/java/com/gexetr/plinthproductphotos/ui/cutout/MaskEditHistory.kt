package com.gexetr.plinthproductphotos.ui.cutout

internal class MaskEditHistory(
    private val limit: Int = 10,
) {
    private val undo = ArrayDeque<ByteArray>()
    private val redo = ArrayDeque<ByteArray>()

    val canUndo: Boolean
        get() = undo.isNotEmpty()

    val canRedo: Boolean
        get() = redo.isNotEmpty()

    fun record(previous: ByteArray) {
        undo.addLast(previous.copyOf())
        while (undo.size > limit) undo.removeFirst()
        redo.clear()
    }

    fun undo(current: ByteArray): ByteArray? {
        if (undo.isEmpty()) return null
        redo.addLast(current.copyOf())
        return undo.removeLast()
    }

    fun redo(current: ByteArray): ByteArray? {
        if (redo.isEmpty()) return null
        undo.addLast(current.copyOf())
        while (undo.size > limit) undo.removeFirst()
        return redo.removeLast()
    }

    fun clear() {
        undo.clear()
        redo.clear()
    }
}
