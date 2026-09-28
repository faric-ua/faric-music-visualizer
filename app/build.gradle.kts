plugins {
    id("com.android.application")
}
android {
    namespace = "com.saney.musicvisualizer"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.saney.musicvisualizer"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { buildConfig = true }
}
dependencies {
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.media3:media3-common:1.11.1")
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    testImplementation("junit:junit:4.13.2")
}
