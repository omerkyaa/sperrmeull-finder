package com.omerkaya.sperrmuellfinder.ui.ads

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.RequestConfiguration
import com.omerkaya.sperrmuellfinder.BuildConfig
import com.omerkaya.sperrmuellfinder.core.util.CrashReportingManager
import com.omerkaya.sperrmuellfinder.core.util.Logger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdManager @Inject constructor(
    private val logger: Logger,
    private val crashReportingManager: CrashReportingManager
) {
    fun shouldShowAds(inputs: AdEligibilityInputs): Boolean {
        return try {
            val isPremium = if (inputs.hasRevenueCatSignal) {
                inputs.isRevenueCatPremium
            } else {
                inputs.isRevenueCatPremium || inputs.isFirestorePremium
            }
            if (isPremium) {
                logger.d(TAG, "Ads disabled for premium user")
                return false
            }
            if (inputs.forceTestMode) {
                logger.d(TAG, "Ads enabled in debug test mode")
                return true
            }
            if (!inputs.adsEnabled) {
                logger.d(TAG, "Ads disabled by Remote Config")
                return false
            }
            if (!inputs.hasConsent) {
                logger.d(TAG, "Ads blocked until consent is granted")
                return false
            }
            true
        } catch (e: Exception) {
            logger.e(TAG, "Failed to evaluate ad eligibility", e)
            crashReportingManager.reportNonFatalError(
                error = e,
                context = "AdManager.shouldShowAds",
                additionalData = mapOf("ads_component" to "eligibility")
            )
            false
        }
    }


    private var adView: AdView? = null
    private var initialized = false
    private var lastLoadSignature: String? = null

    fun init(context: Context) {
        if (initialized) return
        try {
            if (BuildConfig.DEBUG) {
                MobileAds.setRequestConfiguration(
                    RequestConfiguration.Builder()
                        .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                        .build()
                )
            }
            MobileAds.initialize(context.applicationContext)
            initialized = true
            logger.i(TAG, "MobileAds initialized in AdManager")
        } catch (e: Exception) {
            logger.e(TAG, "Failed to initialize MobileAds", e)
            crashReportingManager.reportNonFatalError(
                error = e,
                context = "AdManager.init",
                additionalData = mapOf("ads_component" to "mobile_ads_init")
            )
        }
    }

    fun loadBanner(
        activity: Activity,
        container: ViewGroup,
        adUnitId: String,
        containerWidthPx: Int
    ) {
        if (adUnitId.isBlank()) return

        try {
            init(activity.applicationContext)

            val adaptiveSize = getAdaptiveBannerSize(activity, containerWidthPx)
            val loadSignature = "$adUnitId:${adaptiveSize.width}x${adaptiveSize.height}"

            val currentAdView = adView ?: createAdView(activity, adUnitId).also { adView = it }
            ensureAttachedToContainer(currentAdView, container)

            val adUnitChanged = currentAdView.adUnitId != adUnitId
            val sizeChanged = currentAdView.adSize != adaptiveSize

            if (adUnitChanged || sizeChanged) {
                currentAdView.adUnitId = adUnitId
                currentAdView.setAdSize(adaptiveSize)
                lastLoadSignature = null
            }

            if (lastLoadSignature == loadSignature) return

            currentAdView.loadAd(AdRequest.Builder().build())
            lastLoadSignature = loadSignature
        } catch (e: Exception) {
            logger.e(TAG, "Failed to load banner", e)
            crashReportingManager.reportNonFatalError(
                error = e,
                context = "AdManager.loadBanner",
                additionalData = mapOf(
                    "ads_component" to "banner_load",
                    "ad_unit_id" to adUnitId
                )
            )
        }
    }

    fun pauseBanner() {
        try {
            adView?.pause()
        } catch (e: Exception) {
            logger.e(TAG, "Failed to pause banner", e)
            crashReportingManager.reportNonFatalError(
                error = e,
                context = "AdManager.pauseBanner",
                additionalData = mapOf("ads_component" to "banner_pause")
            )
        }
    }

    fun resumeBanner() {
        try {
            adView?.resume()
        } catch (e: Exception) {
            logger.e(TAG, "Failed to resume banner", e)
            crashReportingManager.reportNonFatalError(
                error = e,
                context = "AdManager.resumeBanner",
                additionalData = mapOf("ads_component" to "banner_resume")
            )
        }
    }

    fun destroyBanner() {
        try {
            adView?.let { banner ->
                (banner.parent as? ViewGroup)?.removeView(banner)
                banner.destroy()
            }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to destroy banner", e)
            crashReportingManager.reportNonFatalError(
                error = e,
                context = "AdManager.destroyBanner",
                additionalData = mapOf("ads_component" to "banner_destroy")
            )
        } finally {
            adView = null
            lastLoadSignature = null
        }
    }

    private fun createAdView(activity: Activity, adUnitId: String): AdView {
        return AdView(activity).apply {
            this.adUnitId = adUnitId
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    logger.d(TAG, "Banner loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    logger.w(TAG, "Banner failed to load: ${error.message}")
                    crashReportingManager.reportNonFatalError(
                        error = RuntimeException("Banner failed: ${error.code} ${error.message}"),
                        context = "AdManager.onAdFailedToLoad",
                        additionalData = mapOf(
                            "ads_component" to "banner_listener_failure",
                            "ad_code" to error.code.toString()
                        )
                    )
                    lastLoadSignature = null
                }
            }
            onPaidEventListener = OnPaidEventListener { adValue: AdValue ->
                logger.d(TAG, "Paid event micros=${adValue.valueMicros}")
            }
        }
    }

    private fun ensureAttachedToContainer(banner: AdView, container: ViewGroup) {
        val currentParent = banner.parent as? ViewGroup
        if (currentParent != null && currentParent !== container) {
            currentParent.removeView(banner)
        }

        if (banner.parent == null) {
            container.removeAllViews()
            container.addView(
                banner,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private fun getAdaptiveBannerSize(activity: Activity, containerWidthPx: Int): AdSize {
        val metrics = activity.resources.displayMetrics
        val widthPx = if (containerWidthPx > 0) containerWidthPx else metrics.widthPixels
        val adWidthDp = (widthPx / metrics.density).toInt().coerceAtLeast(1)
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidthDp)
    }

    companion object {
        private const val TAG = "AdManager"
    }
}

data class AdEligibilityInputs(
    val adsEnabled: Boolean,
    val isRevenueCatPremium: Boolean,
    val isFirestorePremium: Boolean,
    val hasRevenueCatSignal: Boolean = true,
    val hasConsent: Boolean,
    val forceTestMode: Boolean = false
)
