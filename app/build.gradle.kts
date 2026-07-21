import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// Seconds since 2020-01-01 UTC. Every new APK receives a larger versionCode,
// which Android requires when it updates an already installed app.
val versionCodeEpochMillis = 1_577_836_800_000L
val automaticVersionCode = ((System.currentTimeMillis() - versionCodeEpochMillis) / 1_000L).toInt()

// Release signing stays local: create app/keystore.properties from the
// checked-in example and keep the referenced keystore backed up securely.
val keystorePropertiesFile = file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}
val hasReleaseKeystore = keystorePropertiesFile.exists() &&
    keystoreProperties.getProperty("storeFile") != null

plugins {
    // AGP 9.0+ has built-in Kotlin support, so the standalone
    // org.jetbrains.kotlin.android plugin must NOT be applied. The Compose
    // and serialization compiler plugins still attach to the built-in Kotlin.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.verbum.launcher"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.verbum.launcher"
        minSdk = 26
        targetSdk = 36
        versionCode = automaticVersionCode
        versionName = "1.0.1"
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
