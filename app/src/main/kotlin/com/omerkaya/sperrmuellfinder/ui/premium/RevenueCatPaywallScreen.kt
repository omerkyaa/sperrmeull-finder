package com.omerkaya.sperrmuellfinder.ui.premium

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.omerkaya.sperrmuellfinder.R
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.Purchases

/**
 * Displays RevenueCat dashboard-managed published paywall (native UI).
 */
@Composable
fun RevenueCatPaywallScreen(
    onDismiss: () -> Unit,
    onPurchaseSuccess: () -> Unit
) {
    var retryToken by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedOffering by remember { mutableStateOf<Offering?>(null) }
    var configErrorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(retryToken) {
        isLoading = true
        configErrorMessage = null
        selectedOffering = null

        Purchases.sharedInstance.getOfferingsWith(
            onError = { error ->
                isLoading = false
                configErrorMessage = error.underlyingErrorMessage ?: error.message
            },
            onSuccess = { offerings ->
                val resolvedOffering = offerings.current
                    ?: offerings.all["default"]
                    ?: offerings.all.values.firstOrNull()

                if (resolvedOffering == null) {
                    isLoading = false
                    configErrorMessage = ""
                    return@getOfferingsWith
                }

                selectedOffering = resolvedOffering
                isLoading = false
            }
        )
    }

    when {
        isLoading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        }

        selectedOffering == null -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.revenuecat_paywall_config_error_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(
                        R.string.revenuecat_paywall_config_error_body,
                        configErrorMessage ?: "-"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp, bottom = 20.dp)
                )
                Button(onClick = { retryToken++ }) {
                    Text(text = stringResource(R.string.retry))
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(text = stringResource(R.string.close))
                }
            }
        }

        else -> {
            val options = PaywallOptions.Builder(dismissRequest = onDismiss)
                .setOffering(selectedOffering)
                .setShouldDisplayDismissButton(true)
                .setListener(
                    object : PaywallListener {
                        override fun onPurchaseCompleted(
                            customerInfo: CustomerInfo,
                            storeTransaction: StoreTransaction
                        ) {
                            onPurchaseSuccess()
                        }

                        override fun onRestoreCompleted(customerInfo: CustomerInfo) {
                            onPurchaseSuccess()
                        }

                        override fun onPurchaseError(error: PurchasesError) = Unit
                        override fun onPurchaseCancelled() = Unit
                        override fun onRestoreError(error: PurchasesError) = Unit
                    }
                )
                .build()

            Paywall(options = options)
        }
    }
}
