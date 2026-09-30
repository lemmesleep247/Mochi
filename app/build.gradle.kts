import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// CI (see .github/workflows/release.yml) passes these as -P properties derived
// from the pushed git tag; local/debug builds fall back to a fixed dev version.
val releaseVersionName = (project.findProperty("appVersionName") as String?) ?: "0.1.0-dev"
val releaseVersionCode = (project.findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 1

// CI passes the real client ID as -PmalClientId; local dev reads it from
// local.properties (see local.properties.copy) instead.
val malClientId = (project.findProperty("malClientId") as String?)
    ?: localProps.getProperty("MAL_CLIENT_ID", "")

android {
    namespace = "io.github.mochi"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.mochi"
        minSdk = 26
        targetSdk = 36
        versionCode = releaseVersionCode
        versionName = releaseVersionName

        buildConfigField("String", "MAL_CLIENT_ID", "\"$malClientId\"")
        buildConfigField("String", "OAUTH_REDIRECT", "\"mochi://oauth/callback\"")
    }

    // Only configured when RELEASE_KEYSTORE_PATH is set (the release workflow
    // decodes a repo secret into a keystore file and exports this); local
    // `assembleRelease` builds stay unsigned rather than failing outright.
    // Read via providers.environmentVariable (not System.getenv) so these are
    // tracked as proper configuration-cache inputs.
    val releaseKeystorePath = providers.environmentVariable("RELEASE_KEYSTORE_PATH").orNull
    signingConfigs {
        if (releaseKeystorePath != null) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = providers.environmentVariable("RELEASE_KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("RELEASE_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releaseKeystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    // Per-ABI release APKs (plus a universal one) for GitHub Releases, instead
    // of one large fat APK.
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Compose: BOM first, then override material3
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    // Images, paging, storage, login
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.browser)
    implementation(libs.jsoup)
}