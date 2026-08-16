val releaseStoreFile = providers.gradleProperty("TELESALES_RELEASE_STORE_FILE").orNull
val releaseStorePassword = providers.gradleProperty("TELESALES_RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.gradleProperty("TELESALES_RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.gradleProperty("TELESALES_RELEASE_KEY_PASSWORD").orNull
val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

val configuredApiKey = providers.gradleProperty("TELESALES_API_KEY")
    .orElse(providers.environmentVariable("TELESALES_API_KEY"))
    .getOrElse("")

val verifyReleaseApiKey by tasks.registering {
    inputs.property("apiKeyConfigured", configuredApiKey.isNotBlank())
    doLast {
        val apiKeyConfigured = inputs.properties["apiKeyConfigured"] as Boolean
        check(apiKeyConfigured) {
            "TELESALES_API_KEY bắt buộc phải được cấu hình cho release build"
        }
    }
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn(verifyReleaseApiKey)
}

android {
    namespace = "com.nhakhoaquangninh.telesales"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.nhakhoaquangninh.telesales"
        minSdk = 28
        targetSdk = 36
        versionCode = 3
        versionName = "1.2"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(requireNotNull(releaseStoreFile))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
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
    buildFeatures {
        compose = true
        aidl = false
        buildConfig = false
        shaders = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        // Giữ legacy JNI packaging để tương thích các native library trên thiết bị dùng page size 16 KB.
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)

    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":data"))

    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Arch Components
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // Tooling
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.json)

    // Material Icons Extended
    implementation(libs.androidx.compose.material.icons.extended)

    // Google Fonts
    implementation(libs.androidx.compose.ui.text.google.fonts)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)


    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Firebase
    val firebaseBom = platform(libs.firebase.bom)
    implementation(firebaseBom)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
}
