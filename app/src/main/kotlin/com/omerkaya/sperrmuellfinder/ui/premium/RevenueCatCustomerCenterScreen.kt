package com.omerkaya.sperrmuellfinder.ui.premium

import androidx.compose.runtime.Composable

/**
 * Temporary Customer Center entry.
 * Until a dedicated Customer Center UI is wired, fallback to RevenueCat paywall.
 */
@Composable
fun RevenueCatCustomerCenterScreen(
    onNavigateBack: () -> Unit
) {
    RevenueCatPaywallScreen(
        onDismiss = onNavigateBack,
        onPurchaseSuccess = onNavigateBack
    )
}
