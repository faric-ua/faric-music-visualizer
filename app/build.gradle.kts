plugins {
    id("com.android.application")
}

val devKeystorePath = providers.environmentVariable("VISUALIZER_DEV_KEYSTORE_PATH").orNull
val devStorePassword = providers.environmentVariable("VISUALIZER_DEV_STORE_PASSWORD").orNull
val devKeyAlias = providers.environmentVariable("VISUALIZER_DEV_KEY_ALIAS").orNull
val devKeyPassword = providers.environmentVariable("VISUALIZER_DEV_KEY_PASSWORD").orNull

// V178 (faric 23.10 21:06: «збирати APK у себе на телефоні»): the phone has no Android NDK, so the
// C++ part (projectM) comes ready-made from the last GitHub APK — a folder with arm64-v8a/*.so —
// and only Kotlin and resources are built on the phone. GitHub builds keep compiling the C++ itself.
val prebuiltNative = providers.gradleProperty("faricPrebuiltNative").orNull?.takeIf { it.isNotBlank() }

// V180 (faric 23.10 21:54: «додай у звіт версію програми, щоб ти розумів, на якій версії, який код»):
// the commit the APK is built from — GitHub gives it, on the phone (menu 22) git tells it.
val gitShort: String =
    providers.environmentVariable("GITHUB_SHA").orNull?.take(7)
        ?: runCatching {
            val process = ProcessBuilder("git", "rev-parse", "--short=7", "HEAD").directory(rootDir).redirectErrorStream(true).start()
            process.inputStream.bufferedReader().readText().trim().also { process.waitFor() }
        }.getOrNull()?.takeIf { it.matches(Regex("[0-9a-f]{7}")) }
        ?: "local"

android {
    namespace = "com.saney.musicvisualizer"
    compileSdk = 36
    ndkVersion = "27.2.12479018"

    defaultConfig {
        applicationId = "com.saney.musicvisualizer"
        minSdk = 26
        targetSdk = 36
        versionCode = 233
        versionName = "0.21.37"
        // v0.19.64: role-specific PNG imports visible on Board/Themes; responsive labels and gaps.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GIT_SHA", "\"$gitShort\"")

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
            if (prebuiltNative != null) jniLibs.srcDir(prebuiltNative)
        }
    }

    if (prebuiltNative == null) {
        externalNativeBuild {
            cmake {
                path = file("src/main/cpp/CMakeLists.txt")
                version = "3.22.1"
            }
        }
    } else {
        // The ready-made libraries are already stripped (taken from a release APK); no NDK to strip again.
        packaging {
            jniLibs {
                keepDebugSymbols += "**/*.so"
            }
        }
    }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.media3:media3-common:1.11.1")
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-session:1.11.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.12.0")
}
