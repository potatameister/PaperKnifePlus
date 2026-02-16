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
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures { compose = true }
}

dependencies {
    implementation(libs.corektx)
    implementation(libs.lifecycle)
    implementation(libs.activity)
    implementation(platform(libs.composebom))
    implementation(libs.ui)
    implementation(libs.graphics)
    implementation(libs.preview)
    implementation(libs.material3)
    implementation(libs.pdfbox)
    implementation(libs.coil)
    testImplementation(libs.testjunit)
    androidTestImplementation(libs.testext)
    androidTestImplementation(libs.testespresso)
    androidTestImplementation(platform(libs.composebom))
    androidTestImplementation(libs.junit4)
    debugImplementation(libs.tooling)
    debugImplementation(libs.manifest)
}
