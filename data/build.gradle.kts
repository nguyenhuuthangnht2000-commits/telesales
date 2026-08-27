val telesalesApiKey = providers.gradleProperty("TELESALES_API_KEY")
    .orElse(providers.environmentVariable("TELESALES_API_KEY"))
    .getOrElse("")
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.nhakhoaquangninh.telesales.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
        buildConfigField("String", "TELESALES_API_KEY", "\"$telesalesApiKey\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core"))
    implementation(libs.androidx.core.ktx)

    // Retrofit & OkHttp
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)

    api(libs.room.runtime)
    api(libs.room.ktx)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)
}
