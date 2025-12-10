plugins {

    id ("com.android.application") version "8.13.1" apply false
    id ("com.android.library") version "8.13.1" apply false
    id ("org.jetbrains.kotlin.android") version "2.2.0" apply false
    id ("com.google.dagger.hilt.android") version "2.57" apply false
    id("com.google.gms.google-services") version "4.4.3" apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
//    alias(libs.plugins.kotlin.compose.compiler) apply false
}

//tasks.register('clean', Delete) {
//    delete rootProject.layout.buildDirectory
//}
