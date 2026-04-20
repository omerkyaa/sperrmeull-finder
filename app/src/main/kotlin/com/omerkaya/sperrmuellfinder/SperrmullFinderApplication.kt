package com.omerkaya.sperrmuellfinder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.NativeLibraryManager
import com.omerkaya.sperrmuellfinder.core.util.GoogleServicesChecker
import com.omerkaya.sperrmuellfinder.core.util.ResourceMigrationManager
import com.omerkaya.sperrmuellfinder.core.util.CrashReportingManager
import com.omerkaya.sperrmuellfinder.core.util.BinderProxyManager
import com.omerkaya.sperrmuellfinder.data.manager.AnalyticsManager
import com.omerkaya.sperrmuellfinder.data.manager.FirebaseManager
import com.omerkaya.sperrmuellfinder.data.manager.RemoteConfigManager
import com.omerkaya.sperrmuellfinder.data.manager.RevenueCatManager
import com.omerkaya.sperrmuellfinder.manager.OneSignalManager
import com.omerkaya.sperrmuellfinder.ui.ads.AdManager
import com.omerkaya.sperrmuellfinder.ui.ads.ConsentManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SperrmullFinderApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var logger: Logger
    
    @Inject
    lateinit var firebaseManager: FirebaseManager
    
    @Inject
    lateinit var analyticsManager: AnalyticsManager
    
    @Inject
    lateinit var remoteConfigManager: RemoteConfigManager
    
    @Inject
    lateinit var revenueCatManager: RevenueCatManager
    
    @Inject
    lateinit var nativeLibraryManager: NativeLibraryManager
    
    @Inject
    lateinit var googleServicesChecker: GoogleServicesChecker
    
    @Inject
    lateinit var resourceMigrationManager: ResourceMigrationManager
    
    @Inject
    lateinit var crashReportingManager: CrashReportingManager
    
    @Inject
    lateinit var binderProxyManager: BinderProxyManager
    
    @Inject
    lateinit var purchaseManager: com.omerkaya.sperrmuellfinder.core.manager.PurchaseManagerInterface

    @Inject
    lateinit var oneSignalManager: OneSignalManager
    
    @Inject
    lateinit var adManager: AdManager
    
    @Inject
    lateinit var consentManager: ConsentManager

    private val applicationScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        
        // CRASH FIX: Initialize crash reporting FIRST to catch any startup issues
        initializeCrashReporting()
        
        // CRASH FIX: Resource migration MUST be early to prevent resource ID crashes
        performResourceMigration()
        
        // CRASH FIX: Initialize BinderProxy cleanup EARLY to prevent leaks
        initializeBinderProxyManager()
        
        // CRASH FIX: Initialize native libraries (defensive approach)
        initializeNativeLibraries()
        
        // CRASH FIX: Check Google Services before Firebase initialization
        checkGoogleServices()
        
        // Initialize Firebase services
        initializeFirebase()
        
        // Initialize RevenueCat
        initializeRevenueCat()

        // Initialize OneSignal
        initializeOneSignal()

        // Initialize AdMob SDK (ads are gated in UI for basic users only)
        initializeAds()
        
        // Create notification channels
        createNotificationChannels()
        
        // Initialize Remote Config asynchronously
        initializeRemoteConfig()
        
        logger.i(Logger.TAG_FIREBASE, "Application started successfully")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun initializeFirebase() {
        try {
            // Initialize Firebase Manager (includes App Check)
            firebaseManager.initialize()
            
            // Initialize Analytics
            analyticsManager.initialize()
            
            // Setup analytics configuration callback from Remote Config
            remoteConfigManager.setAnalyticsConfigCallback { enabled ->
                analyticsManager.updateConfiguration(enabled)
            }
            
            logger.i(Logger.TAG_FIREBASE, "Firebase services initialized successfully")
            
        } catch (e: Exception) {
            logger.e(Logger.TAG_FIREBASE, "Failed to initialize Firebase services", e)
        }
    }
    
    private fun initializeRevenueCat() {
        try {
            // CRITICAL: Use bulletproof PurchaseManager for initialization
            val apiKey = BuildConfig.REVENUECAT_SDK_API_KEY
            
            // Initialize both managers for backwards compatibility
            purchaseManager.initialize(apiKey)
            revenueCatManager.initialize(apiKey)
            
            logger.i(Logger.TAG_PREMIUM, "Purchase systems initialized - Status: ${purchaseManager.getStatus()}")
        } catch (e: Exception) {
            logger.e(Logger.TAG_PREMIUM, "Failed to initialize purchase systems", e)
            // CRITICAL: App continues without crashing
        }
    }

    private fun initializeOneSignal() {
        try {
            oneSignalManager.initialize(BuildConfig.ONESIGNAL_APP_ID)
            logger.i(Logger.TAG_DEFAULT, "OneSignal initialized successfully")
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to initialize OneSignal", e)
        }
    }

    private fun initializeAds() {
        try {
            consentManager.init(this)
            adManager.init(this)
            logger.i(Logger.TAG_DEFAULT, "AdMob initialized successfully")
        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Failed to initialize AdMob", e)
            crashReportingManager.reportNonFatalError(
                error = e,
                context = "Application.initializeAds",
                additionalData = mapOf("ads_component" to "application_init")
            )
        }
    }
    
    private fun initializeRemoteConfig() {
        applicationScope.launch {
            try {
                val success = remoteConfigManager.initialize()
                if (success) {
                    logger.i(Logger.TAG_FIREBASE, "Remote Config initialized successfully")
                } else {
                    logger.w(Logger.TAG_FIREBASE, "Remote Config initialization failed")
                }
            } catch (e: Exception) {
                logger.e(Logger.TAG_FIREBASE, "Error initializing Remote Config", e)
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // SperrmüllFinder notifications channel (main channel for FCM)
            val mainChannel = NotificationChannel(
                com.omerkaya.sperrmuellfinder.service.SperrmuellFirebaseMessagingService.CHANNEL_ID,
                com.omerkaya.sperrmuellfinder.service.SperrmuellFirebaseMessagingService.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Benachrichtigungen für neue Likes, Kommentare und Aktivitäten"
                enableVibration(true)
                enableLights(true)
                lightColor = 0xFF9C27B0.toInt() // Purple color
                setShowBadge(true)
            }

            // Legacy channels for backward compatibility
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "Allgemeine Benachrichtigungen",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Allgemeine App-Benachrichtigungen"
                enableVibration(true)
                setShowBadge(true)
            }

            val postsChannel = NotificationChannel(
                CHANNEL_POSTS,
                "Post-Benachrichtigungen",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Benachrichtigungen über neue Posts und Aktivitäten"
                enableVibration(true)
                setShowBadge(true)
            }

            val socialChannel = NotificationChannel(
                CHANNEL_SOCIAL,
                "Soziale Benachrichtigungen",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Benachrichtigungen über Likes, Kommentare und Follower"
                enableVibration(true)
                setShowBadge(true)
            }

            val premiumChannel = NotificationChannel(
                CHANNEL_PREMIUM,
                "Premium-Benachrichtigungen",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Wichtige Premium-Benachrichtigungen und Angebote"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(
                listOf(mainChannel, generalChannel, postsChannel, socialChannel, premiumChannel)
            )
            
            logger.i(Logger.TAG_FIREBASE, "Notification channels created successfully")
        }
    }
    
    private fun initializeNativeLibraries() {
        try {
            // CRASH FIX: Proactively check and handle native library issues
            nativeLibraryManager.initializeCommonLibraries()
            logger.i(Logger.TAG_DEFAULT, "Native library initialization completed")
            } catch (e: Exception) {
                // CRITICAL: Never let native library issues crash the app
                logger.e(Logger.TAG_DEFAULT, "Native library initialization failed - app continues", e)
                crashReportingManager.reportCrashFixEvent(
                    com.omerkaya.sperrmuellfinder.core.util.CrashFixType.NATIVE_LIBRARY_ERROR,
                    e,
                    "Native library initialization failed but app continues"
                )
            }
    }
    
    private fun checkGoogleServices() {
        try {
            // CRASH FIX: Comprehensive Google Services diagnostic
            val diagnostic = googleServicesChecker.performDiagnostic()
            
            when (val status = diagnostic.status) {
                is com.omerkaya.sperrmuellfinder.core.util.GoogleServicesStatus.Available -> {
                    logger.i(Logger.TAG_DEFAULT, "Google Services are available and ready")
                }
                is com.omerkaya.sperrmuellfinder.core.util.GoogleServicesStatus.SecurityError -> {
                    logger.e(Logger.TAG_DEFAULT, "Google Services security error - package name mismatch?", status.exception)
                    logger.w(Logger.TAG_DEFAULT, "App package: ${diagnostic.appPackage.packageName}")
                    logger.w(Logger.TAG_DEFAULT, "Google Services will be disabled for this session")
                    
                    crashReportingManager.reportCrashFixEvent(
                        com.omerkaya.sperrmuellfinder.core.util.CrashFixType.GOOGLE_SERVICES_ERROR,
                        status.exception,
                        "Google Services SecurityException handled - package: ${diagnostic.appPackage.packageName}"
                    )
                }
                else -> {
                    logger.w(Logger.TAG_DEFAULT, "Google Services issue: ${status.message}")
                    logger.i(Logger.TAG_DEFAULT, "App will continue with limited Google Services functionality")
                }
            }
            
        } catch (e: Exception) {
            // CRITICAL: Never let Google Services check crash the app
            logger.e(Logger.TAG_DEFAULT, "Google Services check failed - app continues", e)
            crashReportingManager.reportCrashFixEvent(
                com.omerkaya.sperrmuellfinder.core.util.CrashFixType.GOOGLE_SERVICES_ERROR,
                e,
                "Google Services check failed but app continues"
            )
        }
    }
    
    private fun performResourceMigration() {
        // CRITICAL: This must be synchronous and complete before any UI is shown
        applicationScope.launch {
            try {
                logger.i(Logger.TAG_DEFAULT, "Starting critical resource migration...")
                
                val result = resourceMigrationManager.performResourceMigration()
                
                when (result) {
                    is com.omerkaya.sperrmuellfinder.core.util.ResourceMigrationResult.Success -> {
                        logger.i(Logger.TAG_DEFAULT, "Resource migration completed successfully in ${result.durationMs}ms")
                        if (result.migrationsPerformed.isNotEmpty()) {
                            logger.i(Logger.TAG_DEFAULT, "Migrations performed: ${result.migrationsPerformed.size}")
                            result.migrationsPerformed.forEach { migration ->
                                logger.d(Logger.TAG_DEFAULT, "Migration: $migration")
                            }
                        }
                    }
                    is com.omerkaya.sperrmuellfinder.core.util.ResourceMigrationResult.AlreadyUpToDate -> {
                        logger.d(Logger.TAG_DEFAULT, "Resource migration already up to date")
                    }
                    is com.omerkaya.sperrmuellfinder.core.util.ResourceMigrationResult.Failed -> {
                        logger.e(Logger.TAG_DEFAULT, "Resource migration failed - app continues with risk", result.exception)
                    }
                }
                
            } catch (e: Exception) {
                // CRITICAL: Never let migration failure crash the app
                logger.e(Logger.TAG_DEFAULT, "Critical error in resource migration - app continues", e)
            }
        }
    }
    
    private fun initializeCrashReporting() {
        try {
            // CRASH FIX: Initialize crash reporting to track all subsequent errors
            crashReportingManager.initialize()
            
            // Add startup breadcrumb
            crashReportingManager.addBreadcrumb("Application onCreate started", "startup")
            
            logger.i(Logger.TAG_DEFAULT, "Crash reporting initialized")
        } catch (e: Exception) {
            // CRITICAL: Never let crash reporting initialization crash the app
            logger.e(Logger.TAG_DEFAULT, "Crash reporting initialization failed - app continues", e)
        }
    }
    
    private fun initializeBinderProxyManager() {
        try {
            // CRASH FIX: Initialize BinderProxy cleanup to prevent death recipient leaks
            binderProxyManager.initialize()
            
            logger.i(Logger.TAG_DEFAULT, "BinderProxy cleanup manager initialized")
        } catch (e: Exception) {
            // CRITICAL: Never let BinderProxy manager initialization crash the app
            logger.e(Logger.TAG_DEFAULT, "BinderProxy manager initialization failed - app continues", e)
        }
    }

    companion object {
        const val CHANNEL_GENERAL = "general"
        const val CHANNEL_POSTS = "posts"
        const val CHANNEL_SOCIAL = "social"
        const val CHANNEL_PREMIUM = "premium"
    }
}