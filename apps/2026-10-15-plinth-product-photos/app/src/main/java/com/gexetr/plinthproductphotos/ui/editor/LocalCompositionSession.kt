package com.gexetr.plinthproductphotos.ui.editor

import com.gexetr.plinthproductphotos.data.StudioDefaults
import com.gexetr.plinthproductphotos.domain.model.BackgroundSpec
import com.gexetr.plinthproductphotos.domain.model.Canvas
import com.gexetr.plinthproductphotos.domain.model.Look
import com.gexetr.plinthproductphotos.domain.model.Mask
import com.gexetr.plinthproductphotos.domain.model.ShadowSpec
import com.gexetr.plinthproductphotos.domain.pipeline.CompositionResult
import com.gexetr.plinthproductphotos.domain.pipeline.Compositor
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer

data class CompositionSettings(
    val background: BackgroundSpec,
    val shadow: ShadowSpec = ShadowSpec(),
    val marginPct: Float = 0.06f,
    val fillTarget: Float = 0.80f,
    val itemScale: Float = 1f,
    val horizontalPosition: Float = 0f,
    val verticalPosition: Float = 0f,
)

fun StudioDefaults.toCompositionSettings(backgroundArgb: Int) = CompositionSettings(
    background = solidBackground(backgroundArgb),
    shadow = ShadowSpec(kind = ShadowSpec.Kind.entries[shadow]),
    marginPct = marginPct,
    fillTarget = fillTarget,
    itemScale = itemScale,
    horizontalPosition = horizontalPosition,
    verticalPosition = verticalPosition,
)

class LocalCompositionSession(
    private val source: PixelBuffer,
    mask: Mask,
    private val canvas: Canvas,
    initial: CompositionSettings,
) {
    private val mask = Mask(mask.alpha.copyOf())
    private val undoStack = ArrayDeque<CompositionSettings>()
    private val redoStack = ArrayDeque<CompositionSettings>()
    private var lastCommandKey: String? = null

    var settings: CompositionSettings = initial
        private set

    var composition: CompositionResult = compose(initial)
        private set

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun preview(next: CompositionSettings): CompositionResult = compose(next)

    fun apply(next: CompositionSettings, commandKey: String? = null) {
        if (next == settings) return
        val nextComposition = compose(next)
        if (commandKey == null || commandKey != lastCommandKey) undoStack.addLast(settings)
        redoStack.clear()
        settings = next
        composition = nextComposition
        lastCommandKey = commandKey
    }

    fun undo() {
        val previous = undoStack.lastOrNull() ?: return
        val previousComposition = compose(previous)
        redoStack.addLast(settings)
        undoStack.removeLast()
        settings = previous
        composition = previousComposition
        lastCommandKey = null
    }

    fun redo() {
        val next = redoStack.lastOrNull() ?: return
        val nextComposition = compose(next)
        undoStack.addLast(settings)
        redoStack.removeLast()
        settings = next
        composition = nextComposition
        lastCommandKey = null
    }

    private fun compose(value: CompositionSettings): CompositionResult = Compositor.compose(
        source = source,
        mask = mask,
        canvas = canvas,
        look = Look(
            id = "editor",
            name = "Editor",
            background = value.background,
            shadow = value.shadow,
            marginPct = value.marginPct,
            fillTarget = value.fillTarget * value.itemScale,
            createdAt = 0L,
        ),
    )
}

fun solidBackground(color: Int): BackgroundSpec = BackgroundSpec(
    kind = BackgroundSpec.Kind.SOLID,
    colorArgb = color,
    color2Argb = null,
    angleDeg = null,
    sceneId = null,
    sceneAssetPath = null,
    prompt = null,
    seed = null,
)
