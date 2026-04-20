import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.ksp) // KSP instead of kapt
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}


android {
    namespace = "com.omerkaya.sperrmuellfinder"
    compileSdk = libs.versions.compileSdk.get().toInt()
    
    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(localPropertiesFile.inputStream())
    }
    
    fun getOptionalConfigValue(key: String): String? {
        return localProperties.getProperty(key)
            ?: providers.gradleProperty(key).orNull
            ?: System.getenv(key)
    }
    
    fun resolveAdValue(key: String, fallback: String, buildType: String): String {
        val resolved = getOptionalConfigValue(key)
        if (resolved.isNullOrBlank()) {
            println("WARNING: Missing $key for $buildType. Falling back to default configured value.")
            return fallback
        }
        return resolved
    }
    
    // Google's official AdMob test IDs — safe for public / debug builds.
    // See: https://developers.google.com/admob/android/test-ads
    val debugAdMobAppId = "ca-app-pub-3940256099942544~3347511713"
    val debugHomeBannerAdUnitId = "ca-app-pub-3940256099942544/6300978111"
    // Production IDs are NEVER hardcoded; they are supplied via local.properties / env vars.
    // The fallback for release/staging also uses Google's test IDs to prevent accidental
    // billing-account misuse if the env vars are missing.
    val productionAdMobAppId = debugAdMobAppId
    val productionHomeBannerAdUnitId = debugHomeBannerAdUnitId
    
    val releaseAdMobAppId = resolveAdValue(
        key = "ADMOB_APP_ID_RELEASE",
        fallback = productionAdMobAppId,
        buildType = "release"
    )
    val releaseHomeBannerAdUnitId = resolveAdValue(
        key = "ADMOB_HOME_BANNER_AD_UNIT_ID_RELEASE",
        fallback = productionHomeBannerAdUnitId,
        buildType = "release"
    )
    
    val stagingAdMobAppId = getOptionalConfigValue("ADMOB_APP_ID_STAGING")
        ?: releaseAdMobAppId
    val stagingHomeBannerAdUnitId = getOptionalConfigValue("ADMOB_HOME_BANNER_AD_UNIT_ID_STAGING")
        ?: releaseHomeBannerAdUnitId

    if (getOptionalConfigValue("ADMOB_APP_ID_STAGING").isNullOrBlank()) {
        println("WARNING: Missing ADMOB_APP_ID_STAGING. Staging uses release/test fallback app ID.")
    }
    if (getOptionalConfigValue("ADMOB_HOME_BANNER_AD_UNIT_ID_STAGING").isNullOrBlank()) {
        println("WARNING: Missing ADMOB_HOME_BANNER_AD_UNIT_ID_STAGING. Staging uses release/test fallback banner unit.")
    }

    defaultConfig {
        applicationId = "com.omerkaya.sperrmuellfinder"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()

        // Android Instrumentation Test Runner
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        // Room schema export is configured in kapt block below

        // Default manifest placeholders
        manifestPlaceholders["appName"] = "SperrmüllFinder"
        // Google Maps API key — injected into AndroidManifest. Sourced from local.properties / env.
        manifestPlaceholders["MAPS_API_KEY"] = getOptionalConfigValue("MAPS_API_KEY") ?: ""
        if (getOptionalConfigValue("MAPS_API_KEY").isNullOrBlank()) {
            println("WARNING: Missing MAPS_API_KEY in local.properties. Maps features will be disabled at runtime.")
        }
        
        // Build config fields
        buildConfigField("String", "APP_NAME", "\"SperrmuellFinder\"")
        buildConfigField("boolean", "ENABLE_ANALYTICS", "true")
        buildConfigField("boolean", "ENABLE_CRASHLYTICS", "true")
        
        buildConfigField("String", "REVENUECAT_SDK_API_KEY", "\"${localProperties.getProperty("REVENUECAT_SDK_API_KEY", "placeholder_key")}\"")
        buildConfigField("String", "ONESIGNAL_APP_ID", "\"193c741b-0242-4715-b3f3-d3aa74a5a380\"")
        buildConfigField("String", "ONESIGNAL_REST_API_KEY", "\"${localProperties.getProperty("ONESIGNAL_REST_API_KEY", "")}\"")
        buildConfigField("String", "ADMOB_APP_ID", "\"$debugAdMobAppId\"")
        buildConfigField("String", "ADMOB_HOME_BANNER_AD_UNIT_ID", "\"$debugHomeBannerAdUnitId\"")
        
        // Note: Secret API Key is for server-side use only, not included in app
    }

    buildTypes {
        debug {
            isDebuggable = true
            versionNameSuffix = "-debug"
            
            // Use manifestPlaceholders instead of resValue
            manifestPlaceholders["appName"] = "SperrmüllFinder Debug"
            manifestPlaceholders["FIREBASE_APP_CHECK_DEBUG_TOKEN"] = "debug-token-placeholder"
            manifestPlaceholders["admobAppId"] = debugAdMobAppId
            
            // Debug optimizations
            isMinifyEnabled = false
            isShrinkResources = false
            
            // Build config fields for debug
            buildConfigField("boolean", "ENABLE_ANALYTICS", "false")
            buildConfigField("boolean", "ENABLE_CRASHLYTICS", "false")
            buildConfigField("String", "BASE_URL", "\"https://api-dev.sperrmuell.finder.app/\"")
            buildConfigField("boolean", "IS_STAGING", "false")
            buildConfigField("String", "ADMOB_APP_ID", "\"$debugAdMobAppId\"")
            buildConfigField("String", "ADMOB_HOME_BANNER_AD_UNIT_ID", "\"$debugHomeBannerAdUnitId\"")
            
            // Compose compiler metrics enabled via task configuration below
        }

        create("staging") {
            // Staging is a production-like build without debugging capabilities
            initWith(getByName("release"))
            
            // Staging-specific configuration
            isDebuggable = false // Required for minification and optimizations
            // applicationIdSuffix removed to use same Firebase project as release
            versionNameSuffix = "-staging"
            
            // Note: Lint vital checks will be handled via task configuration
            
            // Override the app name for staging
            manifestPlaceholders["appName"] = "SperrmüllFinder Staging"
            manifestPlaceholders["FIREBASE_APP_CHECK_DEBUG_TOKEN"] = "staging-token-placeholder"
            manifestPlaceholders["admobAppId"] = stagingAdMobAppId
            
            // Production-level optimizations for staging
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Staging-specific config
            buildConfigField("String", "BASE_URL", "\"https://api-staging.sperrmuell.finder.app/\"")
            buildConfigField("boolean", "ENABLE_ANALYTICS", "true")
            buildConfigField("boolean", "ENABLE_CRASHLYTICS", "true")
            buildConfigField("boolean", "IS_STAGING", "true")
            buildConfigField("String", "ADMOB_APP_ID", "\"$stagingAdMobAppId\"")
            buildConfigField("String", "ADMOB_HOME_BANNER_AD_UNIT_ID", "\"$stagingHomeBannerAdUnitId\"")
            
            // Keep debug symbols for crash reporting
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Use manifestPlaceholders instead of resValue
            manifestPlaceholders["appName"] = "SperrmüllFinder"
            manifestPlaceholders["FIREBASE_APP_CHECK_DEBUG_TOKEN"] = "production-token-placeholder"
            manifestPlaceholders["admobAppId"] = releaseAdMobAppId
            
            buildConfigField("String", "BASE_URL", "\"https://api.sperrmuell.finder.app/\"")
            buildConfigField("boolean", "ENABLE_ANALYTICS", "true")
            buildConfigField("boolean", "ENABLE_CRASHLYTICS", "true")
            buildConfigField("boolean", "IS_STAGING", "false")
            buildConfigField("String", "ADMOB_APP_ID", "\"$releaseAdMobAppId\"")
            buildConfigField("String", "ADMOB_HOME_BANNER_AD_UNIT_ID", "\"$releaseHomeBannerAdUnitId\"")
            
            // Release optimizations
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        
        // Enable core library desugaring for older API levels
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = libs.versions.jvmTarget.get()
    }
    
    // Kotlin compiler optimizations - moved to tasks configuration
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-Xcontext-receivers"
        )
        
        // Add compose compiler metrics for debug builds
        if (name.contains("Debug")) {
            kotlinOptions.freeCompilerArgs += listOf(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" +
                        project.layout.buildDirectory.get().asFile.absolutePath + "/compose_metrics"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = false
        dataBinding = false
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.kotlinCompilerExtensionVersion.get()
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE",
                "/META-INF/LICENSE.txt",
                "/META-INF/LICENSE.md",
                "/META-INF/LICENSE-notice.md", // 🎯 FIX: JUnit Jupiter LICENSE-notice.md conflict
                "/META-INF/license.txt",
                "/META-INF/NOTICE",
                "/META-INF/NOTICE.txt",
                "/META-INF/notice.txt",
                "/META-INF/ASL2.0",
                "/META-INF/*.kotlin_module",
                "**/attach_hotspot_windows.dll",
                "META-INF/licenses/**",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1"
            )
            // Prevent R.jar conflicts
            pickFirsts += listOf("**/R.jar", "**/R\$*.class")
        }
    }

    // Resource processing optimizations to prevent file locks
    androidResources {
        generateLocaleConfig = false  // Disable to prevent resources.properties requirement
        noCompress += listOf("tflite", "lite", "json")
    }

    // Bundle configuration for better APK optimization
    bundle {
        storeArchive {
            enable = false
        }
        
        density {
            enableSplit = true
        }
        
        abi {
            enableSplit = true
        }
        
        language {
            enableSplit = false // Keep all languages in base APK
        }
    }

    // Lint configuration - keep quality gates enabled for release readiness
    lint {
        abortOnError = true
        checkReleaseBuilds = true
        disable += setOf("MissingTranslation", "ExtraTranslation")
        warningsAsErrors = false
        quiet = false
        checkDependencies = true
        disable += setOf("UnusedResources", "IconMissingDensityFolder", "GoogleAppIndexingWarning")
        baseline = file("lint-baseline.xml")
        ignoreWarnings = false
        checkAllWarnings = true
    }

    // Test configuration
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // Core Library Desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Core Android
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.bundles.lifecycle)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)

    // Dependency Injection
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler) // KSP instead of kapt

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)

    // Networking
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.kotlinx.serialization.json)

    // Image Loading
    implementation(libs.landscapist.glide)
    implementation(libs.glide)
    implementation("com.google.accompanist:accompanist-swiperefresh:0.33.2-alpha")

    // Camera
    implementation(libs.bundles.camerax)

    // ML Kit will be added in future updates

    // Maps & Location
    implementation(libs.bundles.maps)
    implementation(libs.play.services.ads)
    implementation(libs.google.ump)

    // Revenue Cat
    implementation(libs.revenuecat)
    implementation(libs.revenuecat.ui)
    implementation(libs.onesignal)

    // Database
    implementation(libs.bundles.room)
    ksp(libs.room.compiler) // KSP instead of kapt

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Paging
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Permissions
    implementation(libs.accompanist.permissions)
    
    // Lottie Animations
    implementation(libs.lottie.compose)

    // Splash Screen
    implementation(libs.core.splashscreen)

    // Module dependencies
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(project(":domain"))

    // Testing
    testImplementation(libs.bundles.testing)
    
    // Android Instrumentation Testing
    androidTestImplementation(libs.bundles.testing)
    androidTestImplementation(platform(libs.compose.bom))
    
    // Debug dependencies
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}

// KSP configuration
ksp {
    // Hilt optimizations
    arg("dagger.enableGradleIncremental", "true")
    arg("dagger.formatGeneratedSource", "disabled")
    
    // Room schema export
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}

// Build info is already available in build output, no custom task needed
// This avoids Configuration Cache issues while maintaining build information visibility
