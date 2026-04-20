package com.omerkaya.sperrmuellfinder.manager

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.omerkaya.sperrmuellfinder.BuildConfig
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.core.di.IoDispatcher
import com.omerkaya.sperrmuellfinder.core.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.inAppMessages.IInAppMessageDidDismissEvent
import com.onesignal.inAppMessages.IInAppMessageDidDisplayEvent
import com.onesignal.inAppMessages.IInAppMessageLifecycleListener
import com.onesignal.inAppMessages.IInAppMessageWillDismissEvent
import com.onesignal.inAppMessages.IInAppMessageWillDisplayEvent
import com.onesignal.user.subscriptions.IPushSubscriptionObserver
import com.onesignal.user.subscriptions.PushSubscriptionChangedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OneSignalManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val logger: Logger,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) {
    companion object {
        private const val PREFS_ONESIGNAL = "onesignal_prefs"
        private const val KEY_WELCOME_SHOWN = "welcome_shown"
        private const val KEY_PUSH_PROMPT_REQUESTED = "push_prompt_requested"
    }

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val pushApiClient = OkHttpClient()
    private val prefs by lazy { appContext.getSharedPreferences(PREFS_ONESIGNAL, Context.MODE_PRIVATE) }

    @Volatile
    private var initialized = false
    private var appId: String = ""
    private var activityRef: WeakReference<Activity>? = null
    private var hasShownWelcomeDialog = false
    private var hasPromptedForPushPermission = false

    fun bindActivity(activity: Activity) {
        activityRef = WeakReference(activity)
        maybeShowWelcomeDialogFromCurrentState()
    }

    fun unbindActivity(activity: Activity) {
        val current = activityRef?.get()
        if (current === activity) {
            activityRef = null
        }
    }

    fun initialize(oneSignalAppId: String) {
        if (initialized) return
        appId = oneSignalAppId

        try {
            OneSignal.Debug.logLevel = LogLevel.WARN
            OneSignal.initWithContext(appContext, oneSignalAppId)
            registerPushSubscriptionObserver()
            initialized = true
            logger.i(Logger.TAG_DEFAULT, "OneSignal initialized successfully")
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "OneSignal initialization failed", e)
        }

        hasShownWelcomeDialog = prefs.getBoolean(KEY_WELCOME_SHOWN, false)
        hasPromptedForPushPermission = prefs.getBoolean(KEY_PUSH_PROMPT_REQUESTED, false)
    }

    fun login(externalId: String) {
        if (!initialized || externalId.isBlank()) return
        try {
            OneSignal.login(externalId)
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "OneSignal login failed", e)
        }
    }

    fun logout() {
        if (!initialized) return
        try {
            OneSignal.logout()
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "OneSignal logout failed", e)
        }
    }

    fun setEmail(email: String) {
        if (!initialized || email.isBlank()) return
        try {
            OneSignal.User.addEmail(email)
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "OneSignal setEmail failed", e)
        }
    }

    fun setSmsNumber(number: String) {
        if (!initialized || number.isBlank()) return
        try {
            OneSignal.User.addSms(number)
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "OneSignal setSmsNumber failed", e)
        }
    }

    fun setTag(key: String, value: String) {
        if (!initialized || key.isBlank()) return
        try {
            OneSignal.User.addTag(key, value)
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "OneSignal setTag failed", e)
        }
    }

    fun setTags(tags: Map<String, String>) {
        if (!initialized || tags.isEmpty()) return
        tags.forEach { (key, value) -> setTag(key, value) }
    }

    fun trackEvent(name: String, properties: Map<String, Any>) {
        if (!initialized || name.isBlank()) return
        try {
            OneSignal.User.trackEvent(name, properties)
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "OneSignal trackEvent failed", e)
        }
    }

    fun setLogLevel(level: LogLevel) {
        OneSignal.Debug.logLevel = level
    }

    fun setConsentRequired(required: Boolean) {
        OneSignal.consentRequired = required
    }

    fun setConsentGiven(given: Boolean) {
        OneSignal.consentGiven = given
    }

    private fun registerPushSubscriptionObserver() {
        OneSignal.User.pushSubscription.addObserver(object : IPushSubscriptionObserver {
            override fun onPushSubscriptionChange(state: PushSubscriptionChangedState) {
                val previousId = state.previous.id
                val currentId = state.current.id
                if (previousId.isNullOrEmpty() && !currentId.isNullOrEmpty()) {
                    maybeShowWelcomeDialogFromCurrentState()
                }
            }
        })
    }

    private fun maybeShowWelcomeDialogFromCurrentState() {
        if (!BuildConfig.DEBUG) return
        if (hasShownWelcomeDialog) return
        val subscriptionId = OneSignal.User.pushSubscription.id
        if (subscriptionId.isNullOrEmpty()) return
        val activity = activityRef?.get() ?: return

        hasShownWelcomeDialog = true
        prefs.edit().putBoolean(KEY_WELCOME_SHOWN, true).apply()
        activity.runOnUiThread {
            showWelcomeDialog(activity)
        }
    }

    private fun showWelcomeDialog(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.onesignal_welcome_title))
            .setMessage(activity.getString(R.string.onesignal_welcome_message))
            .setPositiveButton(activity.getString(R.string.onesignal_welcome_trigger_button)) { _, _ ->
                OneSignal.InAppMessages.addTrigger("ai_implementation_campaign_email_journey", "true")
                setupIamDismissListener()
            }
            .setCancelable(false)
            .show()
    }

    private fun setupIamDismissListener() {
        OneSignal.InAppMessages.addLifecycleListener(object : IInAppMessageLifecycleListener {
            override fun onWillDisplay(event: IInAppMessageWillDisplayEvent) = Unit
            override fun onDidDisplay(event: IInAppMessageDidDisplayEvent) = Unit
            override fun onWillDismiss(event: IInAppMessageWillDismissEvent) = Unit

            override fun onDidDismiss(event: IInAppMessageDidDismissEvent) {
                OneSignal.InAppMessages.removeLifecycleListener(this)
                promptForPushPermission()
            }
        })
    }

    private fun promptForPushPermission() {
        if (!BuildConfig.DEBUG) return
        if (hasPromptedForPushPermission) return
        val activity = activityRef?.get() ?: return
        hasPromptedForPushPermission = true
        prefs.edit().putBoolean(KEY_PUSH_PROMPT_REQUESTED, true).apply()
        scope.launch {
            val granted = try {
                withContext(Dispatchers.Main) {
                    OneSignal.Notifications.requestPermission(true)
                }
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "OneSignal requestPermission failed", e)
                false
            }

            if (granted) {
                withContext(Dispatchers.Main) {
                    showSendNotificationDialog(activity)
                }
            }
        }
    }

    private fun showSendNotificationDialog(activity: Activity) {
        val messageInput = EditText(activity).apply {
            hint = activity.getString(R.string.onesignal_send_push_input_hint)
        }

        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.onesignal_send_push_title))
            .setMessage(activity.getString(R.string.onesignal_send_push_message))
            .setView(messageInput)
            .setPositiveButton(activity.getString(R.string.onesignal_send_button)) { _, _ ->
                val message = messageInput.text.toString().trim()
                if (message.isNotEmpty()) {
                    sendPushNotificationToSelf(message)
                }
            }
            .setNegativeButton(activity.getString(R.string.cancel), null)
            .setCancelable(false)
            .show()
    }

    private fun sendPushNotificationToSelf(message: String) {
        val subscriptionId = OneSignal.User.pushSubscription.id ?: return
        if (appId.isBlank()) return
        val restApiKey = BuildConfig.ONESIGNAL_REST_API_KEY
        if (restApiKey.isBlank()) {
            val activity = activityRef?.get() ?: return
            activity.runOnUiThread {
                Toast.makeText(
                    activity,
                    activity.getString(R.string.onesignal_rest_key_missing),
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }

        scope.launch {
            val requestJson = JSONObject().apply {
                put("app_id", appId)
                put("target_channel", "push")
                put("name", "SDK setup self-test")
                put("contents", JSONObject().put("en", message))
                put("headings", JSONObject().put("en", "OneSignal Demo"))
                put("include_subscription_ids", JSONArray().put(subscriptionId))
            }

            val requestBody = requestJson.toString()
                .toRequestBody("application/json; charset=UTF-8".toMediaType())

            val request = Request.Builder()
                .url("https://api.onesignal.com/notifications")
                .post(requestBody)
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Key $restApiKey")
                .build()

            val success = try {
                pushApiClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        logger.w(
                            Logger.TAG_DEFAULT,
                            "OneSignal self-push failed with code ${response.code}: ${response.body?.string()}"
                        )
                    }
                    response.isSuccessful
                }
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "OneSignal self-push request failed", e)
                false
            }

            val activity = activityRef?.get() ?: return@launch
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    activity,
                    activity.getString(
                        if (success) R.string.onesignal_send_push_success
                        else R.string.onesignal_send_push_failure
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }

            if (success) {
                mirrorOneSignalNotificationToInbox(message)
            }
        }
    }

    private suspend fun mirrorOneSignalNotificationToInbox(message: String) {
        val uid = auth.currentUser?.uid ?: return
        val notificationRef = firestore
            .collection("notifications")
            .document(uid)
            .collection("user_notifications")
            .document()

        val payload = mapOf(
            "id" to notificationRef.id,
            "toUserId" to uid,
            "type" to "system",
            "title" to "OneSignal",
            "message" to message,
            "deepLink" to "notifications",
            "createdAt" to FieldValue.serverTimestamp(),
            "isRead" to false,
            "meta" to mapOf("source" to "onesignal")
        )

        try {
            notificationRef.set(payload).await()
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to mirror OneSignal push to in-app inbox", e)
        }
    }
}
