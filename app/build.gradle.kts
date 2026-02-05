import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

// Aplica google-services SOLO cuando se compile el flavor online
val isOnlineBuild = gradle.startParameter.taskNames.any { it.contains("Online", ignoreCase = true) }
if (isOnlineBuild) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.ludiary.android"

    compileSdk = 36

    defaultConfig {
        applicationId = "com.ludiary.android"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 👇 Flavors
    flavorDimensions += "mode"
    productFlavors {
        create("local") {
            dimension = "mode"
            applicationIdSuffix = ".local"
            versionNameSuffix = "-local"
        }
        create("online") {
            dimension = "mode"
            // sin suffix para la app “real”
        }
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

    buildFeatures {
        compose = true
        viewBinding = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    // --- comunes (local + online) ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.mpandroidchart)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // --- Firebase SOLO en ONLINE ---
    add("onlineImplementation", platform(libs.firebase.bom))
    add("onlineImplementation", libs.firebase.auth)
    add("onlineImplementation", libs.firebase.firestore)
    add("onlineImplementation", libs.firebase.functions)
    add("onlineImplementation", libs.firebase.auth.ktx)
    add("onlineImplementation", libs.firebase.functions.ktx)
    add("onlineImplementation", libs.kotlinx.coroutines.play.services)
    add("onlineImplementation", libs.firebase.messaging.ktx)
}