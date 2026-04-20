plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.ksp) // KSP instead of kapt
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt) // Re-enabled with KSP
    // NOTE: google-services and firebase-crashlytics plugins removed from core module
    // These plugins should only be applied to application modules (:app)
    // Firebase dependencies are still available via implementation declarations below
}


android {
    namespace = "com.omerkaya.sperrmuellfinder.core"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Debug build type for development
            isMinifyEnabled = false
        }
        
        create("staging") {
            // Staging build type to match app module staging variant
            initWith(getByName("debug"))
            isMinifyEnabled = false
        }
        
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = libs.versions.jvmTarget.get()
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.kotlinCompilerExtensionVersion.get()
    }
}

dependencies {
    // Core Android
    api(libs.core.ktx)
    api(libs.bundles.lifecycle)

    // Compose
    api(platform(libs.compose.bom))
    api(libs.bundles.compose)

    // Dependency Injection with KSP
    api(libs.hilt.android)
    api(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler) // KSP instead of kapt

    // Coroutines
    api(libs.coroutines.core)
    api(libs.coroutines.android)

    // Serialization
    api(libs.kotlinx.serialization.json)

    // Firebase (for error handling and services)
    api(platform(libs.firebase.bom))
    api(libs.firebase.crashlytics)
    api(libs.firebase.auth)
    api(libs.firebase.firestore)
    api(libs.firebase.storage)
    api(libs.firebase.messaging)

    // Google Play Services (for GooglePlayServicesChecker)
    api(libs.play.services.base)
    api(libs.play.services.auth)

    // Security
    api(libs.security.crypto)

    // ExifInterface for image processing
    api(libs.exifinterface)

    // Testing
    testImplementation(libs.bundles.testing)
}

// Annotation processor configuration for core module
// KSP configuration for core module
ksp {
    arg("dagger.enableGradleIncremental", "true")
    arg("dagger.formatGeneratedSource", "disabled")
}
