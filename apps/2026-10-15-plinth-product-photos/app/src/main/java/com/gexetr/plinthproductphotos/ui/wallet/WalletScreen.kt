package com.gexetr.plinthproductphotos.ui.wallet

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gexetr.capability.billing.ConsumablePack
import com.gexetr.capability.billing.ProductId
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.data.api.DeliveryReceiptResponse
import com.gexetr.plinthproductphotos.data.api.JobStatus
import com.gexetr.plinthproductphotos.data.wallet.WalletProductIds
import com.gexetr.plinthproductphotos.domain.studio.StudioOperationCatalog
import com.gexetr.plinthproductphotos.domain.studio.StudioOperationId
import com.gexetr.plinthproductphotos.domain.studio.StudioOperationRequest
import com.gexetr.plinthproductphotos.ui.components.AuroraField
import com.gexetr.plinthproductphotos.ui.components.PlinthGlassEdge
import com.gexetr.plinthproductphotos.ui.components.PlinthCostChip
import com.gexetr.plinthproductphotos.ui.components.PlinthScaffold
import com.gexetr.plinthproductphotos.ui.components.PlinthSurface
import com.gexetr.plinthproductphotos.ui.components.plinthTonalGlass
import com.gexetr.plinthproductphotos.ui.theme.PlinthTheme
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
data object WalletDestination

data class WalletHistoryRow(
    val label: String,
    val delta: Int,
)

data class WalletJobStatus(
    val jobId: String,
    val status: JobStatus,
    val receipt: DeliveryReceiptResponse? = null,
)

enum class WalletPurchaseUiStatus {
    IDLE,
    PENDING,
    CONFIRMING,
    FAILED_NOT_CHARGED,
    REJECTED_NOT_CHARGED,
    UNCONFIRMED,
    CONFIRMED,
}

enum class WalletRestoreUiStatus {
    IDLE,
    SUCCEEDED,
    FAILED,
}

enum class WalletRecoveryUiStatus {
    IDLE,
    SUCCEEDED,
    FAILED,
}

data class WalletScreenState(
    val balance: Int,
    val productIds: WalletProductIds,
    val reserved: Int = 0,
    val refundDebt: Int = 0,
    val offline: Boolean = false,
    val serverConfirmedUnlock: Boolean = false,
    val localizedPrices: Map<String, String> = emptyMap(),
    val purchaseStatus: WalletPurchaseUiStatus = WalletPurchaseUiStatus.IDLE,
    val purchaseAvailable: Boolean = true,
    val history: List<WalletHistoryRow> = emptyList(),
    val jobs: List<WalletJobStatus> = emptyList(),
    val restoreStatus: WalletRestoreUiStatus = WalletRestoreUiStatus.IDLE,
    val recoveryStatus: WalletRecoveryUiStatus = WalletRecoveryUiStatus.IDLE,
    val jobStatusUnavailable: Boolean = false,
    val jobHistoryUnavailable: Boolean = true,
) {
    val shouldOfferBalanceRefresh: Boolean
        get() = offline

    fun canPurchase(product: ProductId): Boolean =
        !offline && purchaseAvailable && productIds.contains(product) &&
            !localizedPrices[product.value].isNullOrBlank() &&
            purchaseStatus != WalletPurchaseUiStatus.CONFIRMING &&
            purchaseStatus != WalletPurchaseUiStatus.PENDING &&
            (product != productIds.unlock || !serverConfirmedUnlock)
}

@Composable
fun WalletScreen(
    state: WalletScreenState,
    onBack: () -> Unit,
    onRefreshBalance: () -> Unit,
    onPurchase: (ProductId) -> Unit,
    onRestorePurchases: () -> Unit,
    onRecoverWallet: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var recoveryCode by rememberSaveable { mutableStateOf("") }
    var restoreExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedProductValue by rememberSaveable { mutableStateOf<String?>(null) }

    val catalog = state.productIds.billingCatalog()
    val photoCost = StudioOperationCatalog.costFor(
        StudioOperationRequest(StudioOperationId.CUT_OUT),
    ).credits
    val packOffers = listOfNotNull(
        catalog.packFor(state.productIds.starter)?.let {
            walletPackOffer(
                pack = it,
                title = stringResource(R.string.wallet_pack_starter),
                price = state.localizedPrices[it.id.value],
            )
        },
        catalog.packFor(state.productIds.studio)?.let {
            walletPackOffer(
                pack = it,
                title = stringResource(R.string.wallet_pack_studio),
                price = state.localizedPrices[it.id.value],
            )
        },
        catalog.packFor(state.productIds.pro)?.let {
            walletPackOffer(
                pack = it,
                title = stringResource(R.string.wallet_pack_pro),
                price = state.localizedPrices[it.id.value],
            )
        },
    )
    val bestValueOffer = bestValueOffer(packOffers)
    val orderedOffers = packOffers.sortedBy { it.pack.id != bestValueOffer?.pack?.id }
    val selectedOffer = packOffers.firstOrNull { it.pack.id.value == selectedProductValue }
        ?: bestValueOffer
        ?: packOffers.firstOrNull()
    val photoCount = state.balance / photoCost
    val unlockPrice = state.localizedPrices[state.productIds.unlock.value]
        ?.takeIf { it.isNotBlank() }

    PlinthScaffold(
        modifier = modifier,
        topBar = {
            WalletHeader(onBack = onBack)
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth().plinthTonalGlass(PlinthGlassEdge.TOP),
                color = Color.Transparent,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PurchaseStatus(state.purchaseStatus)
                    Button(
                        onClick = {
                            selectedOffer?.let { onPurchase(it.pack.id) }
                        },
                        enabled = selectedOffer?.let { state.canPurchase(it.pack.id) } == true,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    ) {
                        Text(
                            text = purchaseActionLabel(
                                state = state,
                                selectedOffer = selectedOffer,
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = stringResource(R.string.wallet_purchase_trust),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                BalanceHero(state, photoCount, onRefreshBalance)
            }
            item {
                Text(
                    text = stringResource(R.string.wallet_choose_pack),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            item {
                Column(
                    modifier = Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    orderedOffers.forEach { offer ->
                        WalletPackCard(
                            offer = offer,
                            bestValue = offer.pack.id == bestValueOffer?.pack?.id,
                            selected = offer.pack.id == selectedOffer?.pack?.id,
                            photoCost = photoCost,
                            onSelect = { selectedProductValue = offer.pack.id.value },
                        )
                    }
                }
            }
            item {
                PlinthSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.wallet_unlock),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            if (state.serverConfirmedUnlock) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_lucide_check),
                                        contentDescription = stringResource(R.string.wallet_unlock_active),
                                        modifier = Modifier.padding(7.dp).size(18.dp),
                                    )
                                }
                            }
                        }
                        Text(
                            text = stringResource(
                                if (state.serverConfirmedUnlock) R.string.wallet_unlock_active
                                else R.string.wallet_free_limits,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (!state.serverConfirmedUnlock) {
                            Text(
                                text = stringResource(R.string.wallet_unlock_help),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(
                                onClick = { onPurchase(state.productIds.unlock) },
                                enabled = unlockPrice != null &&
                                    state.canPurchase(state.productIds.unlock),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            ) {
                                Text(
                                    text = unlockPrice?.let {
                                        stringResource(R.string.wallet_unlock_purchase, it)
                                    } ?: stringResource(R.string.wallet_price_unavailable),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
            item {
                PlinthSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val chevronRotation by animateFloatAsState(
                            targetValue = if (restoreExpanded) 90f else 0f,
                            animationSpec = walletSpring(),
                            label = "wallet-restore-chevron",
                        )
                        TextButton(
                            onClick = { restoreExpanded = !restoreExpanded },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            contentPadding = PaddingValues(vertical = 10.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.wallet_restore_section),
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Icon(
                                painter = painterResource(R.drawable.ic_lucide_chevron_right),
                                contentDescription = null,
                                modifier = Modifier.rotate(chevronRotation),
                            )
                        }
                        AnimatedVisibility(
                            visible = restoreExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            Column(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.wallet_recovery_help),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                OutlinedTextField(
                                    value = recoveryCode,
                                    onValueChange = { recoveryCode = it.trim().uppercase() },
                                    label = {
                                        Text(stringResource(R.string.wallet_recovery_code_label))
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                TextButton(
                                    onClick = { onRecoverWallet(recoveryCode) },
                                    enabled = recoveryCode.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                ) {
                                    Text(stringResource(R.string.wallet_recover))
                                }
                                TextButton(
                                    onClick = onRestorePurchases,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                ) {
                                    Text(stringResource(R.string.wallet_restore))
                                }
                                RecoveryStatus(state.recoveryStatus)
                                RestoreStatus(state.restoreStatus)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletHeader(onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().plinthTonalGlass(PlinthGlassEdge.BOTTOM),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_arrow_left),
                        contentDescription = stringResource(R.string.wallet_back),
                    )
                }
            }
            Text(
                text = stringResource(R.string.wallet_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
            )
            Icon(
                painter = painterResource(R.drawable.ic_lucide_wallet),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BalanceHero(
    state: WalletScreenState,
    photoCount: Int,
    onRefreshBalance: () -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
    ) {
        AuroraField(Modifier.matchParentSize(), intensity = 0.5f)
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.wallet_balance_heading),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.offline) {
                Text(
                    text = stringResource(R.string.wallet_balance_unavailable),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.wallet_offline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    AnimatedContent(
                        targetState = state.balance,
                        modifier = Modifier.alignByBaseline(),
                        label = "wallet-balance",
                    ) { balance ->
                        Text(
                            text = balance.toString(),
                            style = MaterialTheme.typography.displayMedium,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = pluralStringResource(
                            R.plurals.wallet_credit_unit,
                            state.balance,
                            state.balance,
                        ),
                        modifier = Modifier.alignByBaseline(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = pluralStringResource(
                        R.plurals.wallet_balance_uses,
                        photoCount,
                        photoCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.reserved > 0 || state.refundDebt > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.reserved > 0) {
                        WalletMetaPill(
                            text = stringResource(R.string.wallet_reserved, state.reserved),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (state.refundDebt > 0) {
                        WalletMetaPill(
                            text = stringResource(R.string.wallet_refund_debt, state.refundDebt),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
        if (state.shouldOfferBalanceRefresh) {
            IconButton(
                onClick = onRefreshBalance,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_refresh_cw),
                    contentDescription = stringResource(R.string.wallet_refresh),
                )
            }
        }
    }
}

@Composable
private fun WalletMetaPill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .82f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

private fun <T> walletSpring() = spring<T>(
    dampingRatio = 0.8f,
    stiffness = Spring.StiffnessMediumLow,
)

@Composable
private fun WalletPackCard(
    offer: WalletPackOffer,
    bestValue: Boolean,
    selected: Boolean,
    photoCost: Int,
    onSelect: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = walletSpring(),
        label = "wallet-pack-container",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            PlinthTheme.accentInk
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = walletSpring(),
        label = "wallet-pack-border",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 1.dp,
        animationSpec = walletSpring(),
        label = "wallet-pack-border-width",
    )
    val badgeScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = walletSpring(),
        label = "wallet-pack-badge",
    )
    val priceDescription = offer.price ?: stringResource(R.string.wallet_price_unavailable)
    val creditUnit = pluralStringResource(
        R.plurals.wallet_credit_unit,
        offer.pack.grants.toInt(),
        offer.pack.grants.toInt(),
    )
    val valueDescription = packPhotoCount(offer.pack, photoCost)?.let {
        pluralStringResource(R.plurals.wallet_pack_uses, it, it)
    } ?: stringResource(R.string.wallet_pack_too_small)
    val accessibleDescription = stringResource(
        if (bestValue) R.string.wallet_pack_best_accessibility
        else R.string.wallet_pack_accessibility,
        offer.title,
        offer.pack.grants,
        creditUnit,
        priceDescription,
        valueDescription,
    )
    val useCount = packPhotoCount(offer.pack, photoCost)
    val perPhotoPrice = if (useCount != null) {
        offer.parsedPrice?.perUnit(useCount)?.let {
            stringResource(R.string.wallet_pack_price_per_use, it)
        }
    } else {
        null
    }
    val comparison = perPhotoPrice ?: pluralStringResource(
        R.plurals.wallet_pack_credit_cost,
        photoCost,
        photoCost,
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 136.dp)
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton,
            )
            .semantics(mergeDescendants = true) {
                contentDescription = accessibleDescription
            }
            .animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        border = BorderStroke(borderWidth, borderColor),
    ) {
        Box {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = offer.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (bestValue) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Text(
                                text = stringResource(R.string.wallet_best_value),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    // The check badge sits in the card's top-end corner, so its width is reserved
                    // whether or not this pack is selected; otherwise selecting a pack shifts the row.
                    Spacer(Modifier.width(24.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = offer.pack.grants.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            text = creditUnit,
                            modifier = Modifier.padding(bottom = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (offer.price != null) {
                        Text(
                            text = offer.price,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ) {
                            Text(
                                text = stringResource(R.string.wallet_price_unavailable),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Text(
                    text = valueDescription,
                    style = MaterialTheme.typography.bodyMedium,
                )
                PlinthCostChip(label = comparison)
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = badgeScale
                        scaleY = badgeScale
                    },
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_check),
                    contentDescription = null,
                    modifier = Modifier.padding(4.dp),
                )
            }
        }
    }
}

@Composable
private fun purchaseActionLabel(
    state: WalletScreenState,
    selectedOffer: WalletPackOffer?,
): String {
    if (selectedOffer == null) return stringResource(R.string.wallet_purchase_unavailable)

    val credits = selectedOffer.pack.grants
    val price = selectedOffer.price
    return when {
        state.purchaseStatus == WalletPurchaseUiStatus.CONFIRMING ->
            stringResource(R.string.wallet_purchase_confirming)
        state.purchaseStatus == WalletPurchaseUiStatus.PENDING ->
            stringResource(R.string.wallet_purchase_pending)
        !state.purchaseAvailable || state.offline ->
            stringResource(R.string.wallet_cta_purchases_unavailable, credits)
        price == null ->
            stringResource(R.string.wallet_cta_price_unavailable, credits)
        state.purchaseStatus == WalletPurchaseUiStatus.FAILED_NOT_CHARGED ||
            state.purchaseStatus == WalletPurchaseUiStatus.REJECTED_NOT_CHARGED ||
            state.purchaseStatus == WalletPurchaseUiStatus.UNCONFIRMED ->
            stringResource(R.string.wallet_cta_retry, credits, price)
        else -> stringResource(R.string.wallet_cta_buy, credits, price)
    }
}

@Composable
private fun PurchaseStatus(status: WalletPurchaseUiStatus) {
    val message = when (status) {
        WalletPurchaseUiStatus.IDLE,
        WalletPurchaseUiStatus.PENDING,
        WalletPurchaseUiStatus.CONFIRMING -> null
        WalletPurchaseUiStatus.FAILED_NOT_CHARGED,
        WalletPurchaseUiStatus.REJECTED_NOT_CHARGED -> R.string.wallet_failed_not_charged
        WalletPurchaseUiStatus.UNCONFIRMED -> R.string.wallet_purchase_unconfirmed
        WalletPurchaseUiStatus.CONFIRMED -> R.string.wallet_purchase_success
    }
    if (message != null) {
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RecoveryStatus(status: WalletRecoveryUiStatus) {
    val message = when (status) {
        WalletRecoveryUiStatus.IDLE -> null
        WalletRecoveryUiStatus.SUCCEEDED -> R.string.wallet_recovery_success
        WalletRecoveryUiStatus.FAILED -> R.string.wallet_recovery_failed
    }
    if (message != null) {
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RestoreStatus(status: WalletRestoreUiStatus) {
    val message = when (status) {
        WalletRestoreUiStatus.IDLE -> null
        WalletRestoreUiStatus.SUCCEEDED -> R.string.wallet_restore_success
        WalletRestoreUiStatus.FAILED -> R.string.wallet_restore_failure
    }
    if (message != null) {
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class WalletPackOffer(
    val pack: ConsumablePack,
    val title: String,
    val price: String?,
    val parsedPrice: ParsedStorePrice?,
)

private data class ParsedStorePrice(
    val amount: BigDecimal,
    val prefix: String,
    val suffix: String,
    val decimalSeparator: Char?,
) {
    val currencySignature: String
        get() = (prefix + suffix).filterNot {
            it.isWhitespace() || it.isDigit() || it in ".,'’٬٫"
        }

    fun perUnit(units: Int): String {
        val amountPerUnit = amount.divide(
            BigDecimal.valueOf(units.toLong()),
            3,
            RoundingMode.HALF_UP,
        )
        val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 3
        }
        if (formatter is DecimalFormat && decimalSeparator != null) {
            val symbols = formatter.decimalFormatSymbols
            symbols.decimalSeparator = decimalSeparator
            formatter.decimalFormatSymbols = symbols
        }
        return prefix + formatter.format(amountPerUnit) + suffix
    }
}

private fun walletPackOffer(
    pack: ConsumablePack,
    title: String,
    price: String?,
): WalletPackOffer = WalletPackOffer(
    pack = pack,
    title = title,
    price = price?.takeIf { it.isNotBlank() },
    parsedPrice = price?.takeIf { it.isNotBlank() }?.let(::parseStorePrice),
)

private fun bestValueOffer(offers: List<WalletPackOffer>): WalletPackOffer? {
    if (offers.isEmpty()) return null
    val currencySignatures = offers.mapNotNull { it.parsedPrice?.currencySignature }.distinct()
    if (currencySignatures.size != 1 || currencySignatures.single().isBlank() ||
        offers.any { it.parsedPrice == null }
    ) {
        return null
    }

    return offers.mapNotNull { offer ->
        offer.parsedPrice?.let { price ->
            offer to price.amount.divide(
                BigDecimal.valueOf(offer.pack.grants.toLong()),
                MathContext.DECIMAL128,
            )
        }
    }.minWithOrNull(
        compareBy<Pair<WalletPackOffer, BigDecimal>> { it.second }
            .thenByDescending { it.first.pack.grants }
            .thenBy { it.first.pack.id.value },
    )?.first
}

private fun packPhotoCount(pack: ConsumablePack, photoCost: Int): Int? =
    (pack.grants / photoCost).toInt().takeIf { it > 0 }

private fun parseStorePrice(formattedPrice: String): ParsedStorePrice? {
    val numberMatch = Regex("[\\p{Nd}][\\p{Nd}.,\\u066B\\u066C'’\\s\\p{Z}]*").find(formattedPrice)
        ?: return null
    val rawNumber = numberMatch.value.trim { it.isWhitespace() || Character.isSpaceChar(it) }
    val normalizedDigits = rawNumber.map { character ->
        if (character.isDigit()) Character.digit(character, 10).toChar() else character
    }.joinToString("")
    val dotIndex = normalizedDigits.lastIndexOf('.')
    val commaIndex = normalizedDigits.lastIndexOf(',')
    val decimalSeparator = when {
        normalizedDigits.contains('\u066B') -> '\u066B'
        dotIndex >= 0 && commaIndex >= 0 -> if (dotIndex > commaIndex) '.' else ','
        dotIndex >= 0 || commaIndex >= 0 -> {
            val separator = if (dotIndex >= 0) '.' else ','
            val fractionDigits = normalizedDigits.substringAfterLast(separator)
                .count { it.isDigit() }
            val localeDecimal = DecimalFormatSymbols.getInstance(Locale.getDefault()).decimalSeparator
            if (separator == localeDecimal || fractionDigits in 1..2) separator else null
        }
        else -> null
    }
    val amountText = buildString {
        normalizedDigits.forEach { character ->
            when {
                character.isDigit() -> append(character)
                character == decimalSeparator -> append('.')
            }
        }
    }
    val amount = amountText.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: return null
    return ParsedStorePrice(
        amount = amount,
        prefix = formattedPrice.substring(0, numberMatch.range.first),
        suffix = formattedPrice.substring(numberMatch.range.first + rawNumber.length),
        decimalSeparator = decimalSeparator,
    )
}
