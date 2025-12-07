import java.util.Properties
import java.io.FileInputStream
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {

    namespace = "com.example.mood"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.mood"
        minSdk = 26
        targetSdk = 34 // Strongly recommend 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Read API key from local.properties
        val properties = Properties() // Correct: Use the simple name because of the import
        val localPropertiesFile = project.rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            try {
                FileInputStream(localPropertiesFile).use { fis -> // Correct: Use simple name and .use block
                    properties.load(fis)
                }
            } catch (e: Exception) {
                println("Warning: Could not load local.properties: ${e.message}")
            }
        }
        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${properties.getProperty("GEMINI_API_KEY", "NO_KEY_FOUND_IN_PROPERTIES")}\""
        )
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.04.01") // Or latest BOM
    implementation(composeBom)
    androidTestImplementation(composeBom) // For UI tests
    implementation("com.google.code.gson:gson:2.10.1") // Veya en son sürüm
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling") // For @Preview and Layout Inspector
    implementation("androidx.compose.material3:material3") // Material Design 3 components
    implementation("androidx.compose.material:material-icons-core")
    implementation("net.openid:appauth:0.11.1") // Veya en son sürüm
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("com.google.ai.client.generativeai:generativeai:0.3.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    // CameraX
    implementation("androidx.camera:camera-core:1.1.0")
    implementation("androidx.camera:camera-camera2:1.1.0")
    implementation("androidx.camera:camera-lifecycle:1.1.0")
    implementation("androidx.camera:camera-view:1.0.0-alpha32")
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation("androidx.compose.material:material-icons-core:1.6.7") // Or latest
    implementation("androidx.compose.material:material-icons-extended:1.6.7") // Or latest
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    


// ML Kit Face Detection
    implementation("com.google.mlkit:face-detection:16.1.7")

}