package com.omerkaya.sperrmuellfinder.ui.premium

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.core.ui.theme.SperrmullPrimary
import com.omerkaya.sperrmuellfinder.domain.model.premium.PremiumProduct
import com.omerkaya.sperrmuellfinder.domain.model.premium.PremiumType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumStatusScreen(
    onNavigateBack: () -> Unit,
    onManageSubscription: () -> Unit,
    onUpgrade: () -> Unit,
    viewModel: PremiumViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    val allProducts = remember(uiState.premiumProducts) {
        uiState.premiumProducts
            .filter { it.type in setOf(PremiumType.PREMIUM_WEEK, PremiumType.PREMIUM_MONTH, PremiumType.PREMIUM_YEAR) }
            .sortedBy { planOrder(it.type) }
    }

    val effectivePlanType = if (uiState.isPremium) {
        uiState.premiumEntitlement?.type ?: uiState.firebasePremiumType
    } else {
        null
    }

    val visibleProducts = remember(allProducts, effectivePlanType) {
        getVisibleUpgradeProducts(allProducts, effectivePlanType)
    }

    var selectedProductId by remember(visibleProducts, effectivePlanType) {
        mutableStateOf(
            visibleProducts.firstOrNull { !isCurrentPlanType(it.type, effectivePlanType) }?.id
                ?: visibleProducts.firstOrNull()?.id
        )
    }

    LaunchedEffect(visibleProducts, effectivePlanType) {
        if (visibleProducts.isNotEmpty() && selectedProductId == null) {
            selectedProductId = visibleProducts.firstOrNull { !isCurrentPlanType(it.type, effectivePlanType) }?.id
                ?: visibleProducts.first().id
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PremiumEvent.PurchaseSuccess -> Toast.makeText(context, context.getString(R.string.premium_purchase_success), Toast.LENGTH_LONG).show()
                is PremiumEvent.PurchaseCancelled -> Toast.makeText(context, context.getString(R.string.purchase_cancelled_message), Toast.LENGTH_SHORT).show()
                is PremiumEvent.PurchaseError -> Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                is PremiumEvent.NetworkError -> Toast.makeText(context, context.getString(R.string.revenuecat_network_error), Toast.LENGTH_LONG).show()
                is PremiumEvent.PaymentPending -> Toast.makeText(context, context.getString(R.string.payment_pending_message), Toast.LENGTH_LONG).show()
                is PremiumEvent.AlreadyOwned -> Toast.makeText(context, context.getString(R.string.product_already_owned_message), Toast.LENGTH_LONG).show()
                is PremiumEvent.ProductNotAvailable -> Toast.makeText(context, context.getString(R.string.product_not_available_message), Toast.LENGTH_LONG).show()
                is PremiumEvent.RestoreSuccess -> Toast.makeText(context, context.getString(R.string.restore_successful_items, event.restoredCount), Toast.LENGTH_LONG).show()
                is PremiumEvent.NoRestorablePurchases -> Toast.makeText(context, context.getString(R.string.no_restorable_purchases_message), Toast.LENGTH_LONG).show()
                is PremiumEvent.RestoreError -> Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                is PremiumEvent.PostExtensionSuccess -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.premium_status),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color(0xFFF4F0FB)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                PremiumHeroSection(isPremium = uiState.isPremium)
            }

            item {
                MembershipInfoCard(
                    effectivePlanType = effectivePlanType,
                    renewalDate = uiState.displayPlanEndDate,
                    isPremium = uiState.isPremium,
                    autoRenewEnabled = uiState.autoRenewEnabled,
                    onManageSubscription = onManageSubscription
                )
            }

            item {
                PlansCard(
                    products = visibleProducts,
                    selectedProductId = selectedProductId,
                    currentPlanType = effectivePlanType,
                    isPurchasing = uiState.isPurchasing,
                    onSelect = { selectedProductId = it },
                    onPurchase = {
                        val selectedProduct = visibleProducts
                            .firstOrNull { it.id == selectedProductId }
                            ?.takeIf { !isCurrentPlanType(it.type, effectivePlanType) }

                        if (selectedProduct != null && activity != null) {
                            viewModel.purchasePremium(activity, selectedProduct)
                        } else {
                            onUpgrade()
                        }
                    }
                )
            }

            item {
                Text(
                    text = stringResource(R.string.premium_features_included_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E2A5A)
                )
            }

            items(viewModel.getPremiumFeaturesSummary()) { feature ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (feature.isAvailable) Color(0xFFEFF8F1) else Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (feature.isAvailable) Color(0xFF2F8B3A) else Color(0xFFB0B7C9),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(feature.titleResId),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF18224D),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(feature.descriptionResId),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6D7390)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun PremiumHeroSection(isPremium: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE9F9)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Bubble(Modifier.align(Alignment.TopCenter).padding(top = 8.dp), Color(0xFF94A3FF), 66.dp)
                Bubble(Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 56.dp), Color(0xFFC6D7FF), 54.dp)
                Bubble(Modifier.align(Alignment.TopEnd).padding(end = 24.dp, top = 56.dp), Color(0xFFBBDCF7), 54.dp)
                Bubble(Modifier.align(Alignment.CenterStart).padding(start = 8.dp, top = 70.dp), Color(0xFFF9CCD7), 48.dp)
                Bubble(Modifier.align(Alignment.CenterEnd).padding(end = 8.dp, top = 70.dp), Color(0xFFF5D1B4), 48.dp)

                Box(
                    modifier = Modifier
                        .size(138.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF9DB8FF), Color(0xFFE2B8F4))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(78.dp)
                    )
                }
            }

            Text(
                text = if (isPremium) {
                    stringResource(R.string.premium_thank_you_title)
                } else {
                    stringResource(R.string.premium_upgrade_headline)
                },
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF1E2A5A),
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isPremium) {
                    stringResource(R.string.premium_thank_you_subtitle)
                } else {
                    stringResource(R.string.premium_upgrade_manage_subtitle)
                },
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF6D7390),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.premium_editors_choice),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1E2A5A),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun Bubble(modifier: Modifier, color: Color, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(size / 2)
        )
    }
}

@Composable
private fun MembershipInfoCard(
    effectivePlanType: PremiumType?,
    renewalDate: Date?,
    isPremium: Boolean,
    autoRenewEnabled: Boolean,
    onManageSubscription: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFBFF)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MembershipRow(
                label = stringResource(R.string.premium_membership_label),
                value = if (isPremium) {
                    effectivePlanType?.let { localizedPlanName(it) } ?: stringResource(R.string.premium)
                } else {
                    stringResource(R.string.premium_inactive)
                }
            )

            MembershipRow(
                label = stringResource(R.string.premium_renew_on_label),
                value = renewalDate?.let { formatPremiumDate(it) } ?: stringResource(R.string.not_available)
            )

            MembershipRow(
                label = stringResource(R.string.premium_auto_renew_label),
                value = if (autoRenewEnabled) {
                    stringResource(R.string.premium_auto_renew_enabled)
                } else {
                    stringResource(R.string.premium_auto_renew_disabled)
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onManageSubscription() }
                    .background(Color(0xFFF1F2FA))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.manage_subscription),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E2A5A)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF8A93B5)
                )
            }
        }
    }
}

@Composable
private fun MembershipRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF1E2A5A),
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF7C86AA),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlansCard(
    products: List<PremiumProduct>,
    selectedProductId: String?,
    currentPlanType: PremiumType?,
    isPurchasing: Boolean,
    onSelect: (String) -> Unit,
    onPurchase: () -> Unit
) {
    val hasUpgradeablePlan = remember(products, currentPlanType) {
        products.any { !isCurrentPlanType(it.type, currentPlanType) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE9F9)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.premium_upgrade_manage_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E2A5A)
            )

            products.forEach { product ->
                val isCurrent = isCurrentPlanType(product.type, currentPlanType)
                val isSelected = selectedProductId == product.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = !isCurrent) { onSelect(product.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) Color(0xFFEAF7EE) else Color.White
                    ),
                    border = if (isSelected || isCurrent) {
                        BorderStroke(
                            width = 2.dp,
                            color = if (isCurrent) Color(0xFF2F8B3A) else SperrmullPrimary
                        )
                    } else {
                        null
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = localizedPlanName(product.type),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E2A5A)
                            )
                            Text(
                                text = product.price,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF2F8B3A),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (isCurrent) {
                            Text(
                                text = stringResource(R.string.current_plan_label),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF2F8B3A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (!hasUpgradeablePlan && currentPlanType != null) {
                Text(
                    text = stringResource(R.string.premium_highest_plan_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6D7390),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Button(
                onClick = onPurchase,
                enabled = hasUpgradeablePlan && selectedProductId != null && !isPurchasing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F8B3A))
            ) {
                Text(
                    text = if (isPurchasing) {
                        stringResource(R.string.processing)
                    } else {
                        stringResource(R.string.premium_change_plan_cta)
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun localizedPlanName(type: PremiumType): String {
    return when (type) {
        PremiumType.PREMIUM_WEEK -> stringResource(R.string.premium_plan_weekly)
        PremiumType.PREMIUM_MONTH -> stringResource(R.string.premium_plan_monthly)
        PremiumType.PREMIUM_YEAR -> stringResource(R.string.premium_plan_yearly)
        PremiumType.PREMIUM_PLUS_MONTH -> stringResource(R.string.premium_plan_monthly)
    }
}

private fun getVisibleUpgradeProducts(
    orderedProducts: List<PremiumProduct>,
    currentPlanType: PremiumType?
): List<PremiumProduct> {
    if (currentPlanType == null) return orderedProducts

    val currentRank = planOrder(currentPlanType)
    val currentProduct = orderedProducts.firstOrNull { isCurrentPlanType(it.type, currentPlanType) }
    val higherTierProducts = orderedProducts.filter { planOrder(it.type) > currentRank }

    return buildList {
        if (currentProduct != null) add(currentProduct)
        addAll(higherTierProducts)
    }.ifEmpty { orderedProducts }
}

private fun isCurrentPlanType(candidate: PremiumType, current: PremiumType?): Boolean {
    if (current == null) return false
    return planOrder(candidate) == planOrder(current)
}

private fun planOrder(type: PremiumType): Int {
    return when (type) {
        PremiumType.PREMIUM_WEEK -> 0
        PremiumType.PREMIUM_MONTH, PremiumType.PREMIUM_PLUS_MONTH -> 1
        PremiumType.PREMIUM_YEAR -> 2
    }
}

private fun formatPremiumDate(date: Date): String {
    val formatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    return formatter.format(date)
}
