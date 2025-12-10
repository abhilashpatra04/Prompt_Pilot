
plugins {
    id("com.android.application")
    id ("org.jetbrains.kotlin.android") version "2.2.0"
    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
    // id("org.jetbrains.kotlin.kapt")
    id ("com.google.dagger.hilt.android")
    id("kotlin-kapt")
    alias(libs.plugins.kotlin.compose.compiler)
}

android {
    namespace = "com.example.promptpilot"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.promptpilot"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
//    kotlinOptions {
//        jvmTarget = "11"
//    }
    buildFeatures {
        compose = true
    }
    kotlin {
        jvmToolchain(17)
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "2.2.0"
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
    }
    //    packaging{
//        resources {
//            excludes += "/META-INF/{AL2.0,LGPL2.1}"
//        }
//    }
    // build.gradle (Module level - app)
    kapt {
        correctErrorTypes = true
        arguments {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }
}

dependencies {
    // AndroidX Core & Lifecycle
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.activity:activity-compose:1.12.1")

    // Jetpack Compose UI
    implementation("androidx.compose.ui:ui:1.10.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.10.0")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.ui:ui-util:1.10.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.navigation:navigation-compose:2.9.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    // Coil (Image loading)
    implementation(libs.coil.compose)
    // System UI customization (Accompanist)
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.36.0")
    // View Binding
    implementation("androidx.compose.ui:ui-viewbinding:1.10.0")
    // Firebase (BoM handles versions for all Firebase libraries)
    implementation(platform("com.google.firebase:firebase-bom:34.0.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore-ktx:25.1.4")
    implementation("com.google.firebase:firebase-storage-ktx:21.0.2")
    // Hilt (Dependency Injection)
    implementation("com.google.dagger:hilt-android:2.57")
    implementation(libs.androidx.core.animation)
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    kapt("com.google.dagger:hilt-compiler:2.57")
    kapt("androidx.room:room-compiler:2.7.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")
    // Kotlinx
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.9.0")
    // Gson
    implementation("com.google.code.gson:gson:2.13.2")
    // Retrofit & Networking
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("com.squareup.okhttp3:logging-interceptor:5.3.2")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.10.0")
    debugImplementation("androidx.compose.ui:ui-tooling:1.10.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.10.0")
    implementation("com.github.jeziellago:compose-markdown:0.5.4")  // Upgraded for syntax highlighting

    // For permissions handling
    implementation ("androidx.activity:activity-compose:1.12.1")
    implementation ("com.google.accompanist:accompanist-permissions:0.37.3")
}

// Allow references to generated code
kapt {
    correctErrorTypes = true
}