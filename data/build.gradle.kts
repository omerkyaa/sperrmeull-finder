plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.ksp) // KSP instead of kapt
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.omerkaya.sperrmuellfinder.data"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room schema export is configured in kapt block below
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
}

dependencies {
    // Module dependencies
    implementation(project(":core"))
    implementation(project(":domain"))

    // Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler) // KSP instead of kapt

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)

    // Networking
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)

    // Database
    implementation(libs.bundles.room)
    ksp(libs.room.compiler) // KSP instead of kapt

    // Camera & ML Kit
    implementation(libs.bundles.camerax)

    // Maps
    implementation(libs.bundles.maps)

    // Revenue Cat
    implementation(libs.revenuecat)

    // Paging
    implementation(libs.paging.runtime)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Testing
    testImplementation(libs.bundles.testing)
}

// KSP configuration for data module
ksp {
    // Hilt optimizations
    arg("dagger.enableGradleIncremental", "true")
    arg("dagger.formatGeneratedSource", "disabled")
    
    // Room schema export
    arg("room.schemaLocation", "$projectDir/schemas")
    
    // Room compiler options
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}
