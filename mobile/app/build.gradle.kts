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

val mapboxAccessToken = secretsProperties.getProperty("MAPBOX_ACCESS_TOKEN") ?: "MAPBOX_API_KEY"
val apiBaseUrl = secretsProperties.getProperty("API_BASE_URL") ?: "http://localhost:8080/api"

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

        val secretsFile = rootProject.file("secrets.properties")
        val secretsProps = Properties()
        if (secretsFile.exists()) {
            secretsFile.inputStream().use { secretsProps.load(it) }
        }
        val apiBaseUrl = secretsProps.getProperty("API_BASE_URL", "").trim()

        buildConfigField("String", "MAPBOX_ACCESS_TOKEN", "\"${mapboxAccessToken}\"")
        buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl}\"")
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
    implementation(libs.mapbox.annotation)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
