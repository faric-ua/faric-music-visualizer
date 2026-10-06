plugins {
    id("com.android.application")
}

val devKeystorePath = providers.environmentVariable("VISUALIZER_DEV_KEYSTORE_PATH").orNull
val devStorePassword = providers.environmentVariable("VISUALIZER_DEV_STORE_PASSWORD").orNull
val devKeyAlias = providers.environmentVariable("VISUALIZER_DEV_KEY_ALIAS").orNull
val devKeyPassword = providers.environmentVariable("VISUALIZER_DEV_KEY_PASSWORD").orNull

android {
    namespace = "com.saney.musicvisualizer"
    compileSdk = 36
    ndkVersion = "27.2.12479018"

    defaultConfig {
        applicationId = "com.saney.musicvisualizer"
        minSdk = 26
        targetSdk = 36
        versionCode = 106
        versionName = "0.19.17"
        // v0.19.17 composites Cyber Shark glow on the encoder GPU surface.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17", "-fexceptions", "-frtti")
            }
        }
    }

    signingConfigs {
        if (
            !devKeystorePath.isNullOrBlank() &&
            !devStorePassword.isNullOrBlank() &&
            !devKeyAlias.isNullOrBlank() &&
            !devKeyPassword.isNullOrBlank()
        ) {
            create("stableDev") {
                storeFile = file(devKeystorePath)
                storePassword = devStorePassword
                keyAlias = devKeyAlias
                keyPassword = devKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            if (signingConfigs.names.contains("stableDev")) {
                signingConfig = signingConfigs.getByName("stableDev")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            // Skin Engine v1 reads the modular PNG pack directly from the
            // repository-level skin/ directory. No duplicate drawable copies.
            assets.srcDir("../skin")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.media3:media3-common:1.11.1")
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    testImplementation("junit:junit:4.13.2")
}
