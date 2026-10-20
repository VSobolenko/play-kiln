package com.gexetr.plinthproductphotos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.ui.theme.PlinthPalette

@Composable
fun PlinthCostChip(
    cost: Int,
    modifier: Modifier = Modifier,
) = PlinthCostChipLabel(
    label = pluralStringResource(R.plurals.home_cost_chip, cost, cost),
    modifier = modifier,
)

@Composable
fun PlinthCostChip(
    label: String,
    modifier: Modifier = Modifier,
) = PlinthCostChipLabel(label = label, modifier = modifier)

@Composable
private fun PlinthCostChipLabel(
    label: String,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        border = BorderStroke(1.dp, PlinthPalette.Spark.copy(alpha = 0.34f)),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
        )
    }
}
