plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// The FFmpeg extension AAR is built from source in CI
// (.github/workflows/build-apk.yml) and placed in app/libs/.

android {
    namespace = "com.captain.kplayer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.captain.kplayer"
        minSdk = 26
        targetSdk = 34
        versionCode = 6
        versionName = "2.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.txt"
            )
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    // FFmpeg audio extension (DTS / DTS-HD / TrueHD / AC3 / E-AC3): Google does
    // NOT publish media3-ffmpeg-decoder as an AAR (source-only extension), so
    // CI builds it from source (androidx/media tag 1.2.1, FFmpeg 6.0) and drops
    // the AAR into app/libs/ — see .github/workflows/build-apk.yml. A file
    // dependency carries no transitive deps, so media3-decoder is declared
    // explicitly below.
    implementation(files("libs/lib-decoder-ffmpeg-release.aar"))
    implementation("androidx.media3:media3-decoder:1.2.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-video:2.7.0")
    // SMB streaming (SmbBrowser / SmbDataSource): jcifs-ng for smb:// access.
    implementation("eu.agno3.jcifs:jcifs-ng:2.1.9")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
