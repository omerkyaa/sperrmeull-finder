package com.omerkaya.sperrmuellfinder.ui.ads

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.omerkaya.sperrmuellfinder.R
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@Composable
fun BannerAdHost(
    adUnitId: String,
    eligibilityInputs: AdEligibilityInputs,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val adManager = remember(context) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            BannerAdHostEntryPoint::class.java
        )
        entryPoint.adManager()
    }
    val consentManager = remember(context) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            BannerAdHostEntryPoint::class.java
        )
        entryPoint.consentManager()
    }
    val hasConsent by consentManager.canRequestAds.collectAsState()
    var container: ViewGroup? by remember { mutableStateOf(null) }
    var widthPx by remember { mutableIntStateOf(0) }
    val shouldShowAds = adManager.shouldShowAds(
        eligibilityInputs.copy(hasConsent = hasConsent)
    )

    AndroidView(
        modifier = modifier.onSizeChanged { size -> widthPx = size.width },
        factory = { viewContext ->
            val root = LayoutInflater.from(viewContext)
                .inflate(R.layout.view_banner_container, null, false)
            container = root.findViewById(R.id.banner_ad_container)
            root
        },
        update = { root ->
            container = root.findViewById(R.id.banner_ad_container)
        }
    )

    LaunchedEffect(adUnitId, widthPx, container, shouldShowAds) {
        val currentContainer = container ?: return@LaunchedEffect
        val hostActivity = context as? Activity ?: return@LaunchedEffect
        consentManager.requestConsentIfRequired(hostActivity)
        if (!shouldShowAds) {
            adManager.destroyBanner()
            return@LaunchedEffect
        }

        adManager.loadBanner(
            activity = hostActivity,
            container = currentContainer,
            adUnitId = adUnitId,
            containerWidthPx = widthPx
        )
    }

    DisposableEffect(lifecycleOwner, shouldShowAds) {
        val observer = LifecycleEventObserver { _, event ->
            if (!shouldShowAds) return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    val hostActivity = context as? Activity
                    if (hostActivity != null) {
                        consentManager.requestConsentIfRequired(hostActivity)
                    }
                    adManager.resumeBanner()
                }
                Lifecycle.Event.ON_PAUSE -> adManager.pauseBanner()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            adManager.destroyBanner()
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BannerAdHostEntryPoint {
    fun adManager(): AdManager
    fun consentManager(): ConsentManager
}
