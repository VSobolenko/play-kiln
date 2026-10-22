package com.gexetr.coverkit.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gexetr.coverkit.R
import com.gexetr.coverkit.ui.theme.Cyan
import com.gexetr.coverkit.ui.theme.Paper

/**
 * Canva-style custom colour: SV square + rainbow hue strip + hex + presets.
 * Vertical drags are consumed here so a parent ModalBottomSheet cannot steal them.
 */
@Composable
fun HsvColorPicker(
    argb: Int,
    onArgb: (Int) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val seed = remember(argb) {
        FloatArray(3).also { AndroidColor.colorToHSV(argb or (0xFF shl 24), it) }
    }
    var hue by remember(argb) { mutableFloatStateOf(seed[0]) }
    var sat by remember(argb) { mutableFloatStateOf(seed[1]) }
    var value by remember(argb) { mutableFloatStateOf(seed[2].coerceAtLeast(0.05f)) }
    var hexDraft by remember(argb) {
        mutableStateOf(String.format("%06X", argb and 0xFFFFFF))
    }

    fun emit(h: Float, s: Float, v: Float) {
        hue = h
        sat = s
        value = v
        val next = AndroidColor.HSVToColor(floatArrayOf(h, s, v)) or (0xFF shl 24)
        hexDraft = String.format("%06X", next and 0xFFFFFF)
        onArgb(next)
    }

    val squareH = if (compact) 112.dp else 148.dp

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.editor_color_custom), color = Paper)
        Box(
            Modifier
                .fillMaxWidth()
                .height(squareH)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Paper.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
        ) {
            val hueColor = Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f)))
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(hue) {
                        fun at(x: Float, y: Float) {
                            val s = (x / size.width.toFloat()).coerceIn(0f, 1f)
                            val v = 1f - (y / size.height.toFloat()).coerceIn(0f, 1f)
                            emit(hue, s, v)
                        }
                        detectTapGestures { offset -> at(offset.x, offset.y) }
                        detectDragGestures { change, _ ->
                            change.consume()
                            at(change.position.x, change.position.y)
                        }
                    },
            ) {
                drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor)))
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                val cx = sat * size.width
                val cy = (1f - value) * size.height
                drawCircle(Color.White, radius = 12f, center = Offset(cx, cy))
                drawCircle(Color.Black, radius = 12f, center = Offset(cx, cy), style = Stroke(width = 3f))
            }
        }
        Text(stringResource(R.string.editor_color_hue), color = Paper.copy(alpha = 0.8f))
        // Rainbow strip — not a Material Slider (sheet drag steals vertical/horizontal scrub).
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    fun at(x: Float) {
                        val h = (x / size.width.toFloat()).coerceIn(0f, 1f) * 360f
                        emit(h, sat, value)
                    }
                    detectTapGestures { offset -> at(offset.x) }
                    detectDragGestures { change, _ ->
                        change.consume()
                        at(change.position.x)
                    }
                },
        ) {
            val stops = listOf(
                Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red,
            )
            drawRect(Brush.horizontalGradient(stops))
            val cx = (hue / 360f) * size.width
            drawCircle(Color.White, radius = 14f, center = Offset(cx, size.height / 2f))
            drawCircle(Color.Black, radius = 14f, center = Offset(cx, size.height / 2f), style = Stroke(width = 3f))
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(argb or (0xFF shl 24)))
                    .border(2.dp, Cyan, CircleShape),
            )
            OutlinedTextField(
                value = hexDraft,
                onValueChange = { raw ->
                    val cleaned = raw.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }.take(6)
                    hexDraft = cleaned
                    if (cleaned.length == 6) {
                        val parsed = cleaned.toIntOrNull(16) ?: return@OutlinedTextField
                        val next = parsed or (0xFF shl 24)
                        val out = FloatArray(3)
                        AndroidColor.colorToHSV(next, out)
                        emit(out[0], out[1], out[2].coerceAtLeast(0.05f))
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(stringResource(R.string.editor_color_hex)) },
                prefix = { Text("#") },
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFFE63946.toInt(), 0xFF2DE2E6.toInt(),
                0xFFFFBE0B.toInt(), 0xFF8338EC.toInt(), 0xFF06D6A0.toInt(), 0xFF3A86FF.toInt(),
            ).forEach { c ->
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(c))
                        .border(1.dp, Paper.copy(alpha = 0.35f), CircleShape)
                        .clickable {
                            val out = FloatArray(3)
                            AndroidColor.colorToHSV(c, out)
                            emit(out[0], out[1], out[2].coerceAtLeast(0.05f))
                        },
                )
            }
        }
    }
}
