package com.gexetr.plinthproductphotos.ui.provenance

import com.gexetr.plinthproductphotos.ui.theme.PlinthTheme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.domain.model.Provenance

@Composable
fun ProvenanceBadge(
    provenance: Provenance,
    modifier: Modifier = Modifier,
    guardSummary: String? = null,
) {
    val ringColor = PlinthTheme.accentInk
    val label = when (provenance) {
        Provenance.ORIGINAL_PIXELS -> stringResource(R.string.provenance_original_label)
        Provenance.MANUALLY_ADJUSTED -> stringResource(R.string.provenance_manual_label)
        Provenance.COMPOSITE_GENERATED_BACKGROUND ->
            stringResource(R.string.provenance_composite_label)
        Provenance.ENHANCED -> stringResource(R.string.provenance_enhanced_label)
    }
    Row(
        modifier = modifier.semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(20.dp)) {
            val inset = 2.dp.toPx()
            val diameter = size.minDimension - inset * 2
            val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            when (provenance) {
                Provenance.COMPOSITE_GENERATED_BACKGROUND -> {
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 164f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                        size = androidx.compose.ui.geometry.Size(diameter, diameter),
                        style = stroke,
                    )
                    drawArc(
                        color = ringColor,
                        startAngle = 106f,
                        sweepAngle = 164f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                        size = androidx.compose.ui.geometry.Size(diameter, diameter),
                        style = stroke,
                    )
                }
                Provenance.ENHANCED -> drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(diameter, diameter),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                    ),
                )
                else -> drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(diameter, diameter),
                    style = stroke,
                )
            }
        }
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
    if (provenance == Provenance.ENHANCED && !guardSummary.isNullOrBlank()) {
        Text(
            text = stringResource(R.string.provenance_guard_summary, guardSummary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
