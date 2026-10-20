package com.gexetr.plinthproductphotos.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gexetr.plinthproductphotos.R

enum class PlinthSection { STUDIO, LIBRARY, PROFILE, MORE }

@Composable
fun PlinthTopBar(section: PlinthSection, balance: Int?, onOpenWallet: () -> Unit) {
    Box(Modifier.fillMaxWidth().plinthTonalGlass(PlinthGlassEdge.BOTTOM).statusBarsPadding().heightIn(min = 64.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(32.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .68f), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(27.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            AnimatedContent(targetState = sectionLabel(section), modifier = Modifier.weight(1f), transitionSpec = { (fadeIn(tween(180)) + slideInHorizontally(tween(180)) { it / 8 }) togetherWith (fadeOut(tween(120)) + slideOutHorizontally(tween(120)) { -it / 8 }) }, label = "plinth-section-title") { label ->
                Text(stringResource(label), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BalanceChip(balance, onOpenWallet)
        }
    }
}
