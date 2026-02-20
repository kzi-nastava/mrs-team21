import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val secretsPropertiesFile = rootProject.file("secrets.properties")
val secretsProperties = Properties().apply {
    if (secretsPropertiesFile.exists()) {
        secretsPropertiesFile.inputStream().use { load(it) }
    }
}

val mapboxAccessToken = (
    secretsProperties.getProperty("MAPBOX_ACCESS_TOKEN")
        ?: secretsProperties.getProperty("MAPBOX_PUBLIC_TOKEN")
        ?: secretsProperties.getProperty("MAPBOX_SECRET_KEY")
        ?: "MAPBOX_API_KEY"
).trim()
val envApiBaseUrl = (System.getenv("API_BASE_URL") ?: "").trim()
val secretsApiBaseUrl = (secretsProperties.getProperty("API_BASE_URL") ?: "").trim()
val resolvedApiBaseUrl = when {
    envApiBaseUrl.isNotEmpty() -> envApiBaseUrl
    secretsApiBaseUrl.isNotEmpty() -> secretsApiBaseUrl
    else -> "http://localhost:8080/api"
}

android {
    namespace = "com.drumigo.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.drumigo.mobile"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "MAPBOX_ACCESS_TOKEN", "\"${mapboxAccessToken}\"")
        buildConfigField("String", "API_BASE_URL", "\"${resolvedApiBaseUrl}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // Core Android
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation("androidx.activity:activity:1.9.3")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    // Navigation Component
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Maps (Mapbox)
    implementation(libs.mapbox.maps)
    implementation(libs.play.services.location)

    // Image loading (profile picture)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
