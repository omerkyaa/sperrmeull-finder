plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.ksp) // KSP instead of kapt
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.omerkaya.sperrmuellfinder.domain"
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
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = libs.versions.jvmTarget.get()
    }
}

dependencies {
    // Module dependencies
    implementation(project(":core"))

    // Java 8+ API desugaring for older Android versions
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler) // KSP instead of kapt

    // Paging
    implementation(libs.paging.runtime)

    // Testing
    testImplementation(libs.bundles.testing)
}

// KSP configuration for domain module
ksp {
    // Hilt optimizations
    arg("dagger.enableGradleIncremental", "true")
    arg("dagger.formatGeneratedSource", "disabled")
}
