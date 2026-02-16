plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.paperknifeplus.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.paperknifeplus.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    // Modern Kotlin 2.0 Compiler Options
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    lint {
        abortOnError = false
    }
}

dependencies {
    implementation(libs.cmp.core.ktx)
    implementation(libs.cmp.lifecycle.runtime)
    implementation(libs.cmp.activity.compose)
    implementation(platform(libs.cmp.compose.bom))
    implementation(libs.cmp.ui)
    implementation(libs.cmp.ui.graphics)
    implementation(libs.cmp.ui.tooling.preview)
    implementation(libs.cmp.material3)
    implementation(libs.cmp.pdfbox.android)
    implementation(libs.cmp.coil.compose)
    testImplementation(libs.cmp.junit)
    androidTestImplementation(libs.cmp.androidx.junit)
    androidTestImplementation(libs.cmp.androidx.espresso.core)
    androidTestImplementation(platform(libs.cmp.compose.bom))
    androidTestImplementation(libs.cmp.ui.test.junit4)
    debugImplementation(libs.cmp.ui.tooling)
    debugImplementation(libs.cmp.ui.test.manifest)
}
