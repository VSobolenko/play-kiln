package com.gexetr.plinthproductphotos.ui.components

import com.gexetr.plinthproductphotos.ui.theme.PlinthTheme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun ProofRing(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val ringColor = PlinthTheme.accentInk
    Box(
        modifier = modifier
            .semantics { contentDescription = label }
            .drawWithContent {
                drawContent()
                val inset = 2.dp.toPx()
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            .padding(4.dp),
    ) {
        content()
    }
}
