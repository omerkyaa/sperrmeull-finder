package com.omerkaya.sperrmuellfinder.ui.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import com.omerkaya.sperrmuellfinder.core.util.CrashReportingManager
import com.omerkaya.sperrmuellfinder.core.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConsentManager @Inject constructor(
    private val logger: Logger,
    private val crashReportingManager: CrashReportingManager
) {

    private var consentInformation: ConsentInformation? = null
    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()
    private var initialized = false
    private var isRequestInFlight = false

    fun init(context: Context) {
        if (initialized) return
        try {
            consentInformation = UserMessagingPlatform.getConsentInformation(context.applicationContext)
            _canRequestAds.value = consentInformation?.canRequestAds() == true
            initialized = true
        } catch (e: Exception) {
            logger.e(TAG, "Failed to initialize UMP consent manager", e)
            crashReportingManager.reportNonFatalError(
                error = e,
                context = "ConsentManager.init",
                additionalData = mapOf("ads_component" to "ump_init")
            )
            _canRequestAds.value = false
        }
    }

    fun requestConsentIfRequired(activity: Activity) {
        if (isRequestInFlight) return
        if (_canRequestAds.value) return
        try {
            init(activity.applicationContext)
            val info = consentInformation ?: return
            isRequestInFlight = true

            // TODO(Omer): Add test device hashes and region simulation via debug settings when needed.
            val debugSettings = ConsentDebugSettings.Builder(activity).build()
            val params = ConsentRequestParameters.Builder()
                .setConsentDebugSettings(debugSettings)
                .build()

            info.requestConsentInfoUpdate(
                activity,
                params,
                {
                    _canRequestAds.value = info.canRequestAds()
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError: FormError? ->
                        if (formError != null) {
                            logger.w(TAG, "UMP form error: ${formError.errorCode} ${formError.message}")
                            crashReportingManager.reportNonFatalError(
                                error = RuntimeException("UMP form error ${formError.errorCode}: ${formError.message}"),
                                context = "ConsentManager.loadAndShowConsentFormIfRequired",
                                additionalData = mapOf("ads_component" to "ump_form")
                            )
                        }
                        _canRequestAds.value = info.canRequestAds()
                        isRequestInFlight = false
                    }
                },
                { requestError ->
                    logger.w(TAG, "UMP consent info update failed: ${requestError.errorCode} ${requestError.message}")
                    crashReportingManager.reportNonFatalError(
                        error = RuntimeException(
                            "UMP consent info update failed ${requestError.errorCode}: ${requestError.message}"
                        ),
                        context = "ConsentManager.requestConsentInfoUpdate",
                        additionalData = mapOf("ads_component" to "ump_update")
                    )
                    _canRequestAds.value = false
                    isRequestInFlight = false
                }
            )
        } catch (e: Exception) {
            logger.e(TAG, "Unexpected UMP consent flow failure", e)
            crashReportingManager.reportNonFatalError(
                error = e,
                context = "ConsentManager.requestConsentIfRequired",
                additionalData = mapOf("ads_component" to "ump_flow")
            )
            _canRequestAds.value = false
            isRequestInFlight = false
        }
    }

    companion object {
        private const val TAG = "ConsentManager"
    }
}
