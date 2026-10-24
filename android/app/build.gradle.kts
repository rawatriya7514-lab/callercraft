plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.craftcaller.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.craftcaller.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 5
        versionName = "3.0.2"

        // Own lookup API (Retrofit) — not required; Firebase is the primary source.
        // Replace with your backend if you deploy one.
        buildConfigField("String", "API_BASE_URL", "\"https://api.craftcaller.app/v1/\"")
        buildConfigField("String", "API_KEY", "\"replace-with-your-key\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("../../keystore/craftcaller-release.keystore")
            storePassword = "craftcaller2026"
            keyAlias = "craftcaller"
            keyPassword = "craftcaller2026"
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "META-INF/{AL2.0,LGPL2.1}"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
        unitTests.all {
            it.maxParallelForks = 2
            it.systemProperty("robolectric.conscryptMode", "OFF")
            it.systemProperty("robolectric.nativeruntime.enable", "false")
            it.systemProperty("robolectric.sqliteMode", "LEGACY")
        }
    }
}

dependencies {
    // Jetpack Compose (BOM keeps versions in sync)
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.navigation:navigation-compose:2.8.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Coroutines (incl. Firebase Tasks -> suspend await)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Firebase (Auth + Firestore + Realtime Database)
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-database")

    // Retrofit + Gson
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // WorkManager (background contact sync)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Correct phone-number normalization/parsing (Google libphonenumber, Apache-2.0)
    implementation("com.googlecode.libphonenumber:libphonenumber:8.13.50")

    // JVM unit tests (JUnit4) + Robolectric for Context-level checks.
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
