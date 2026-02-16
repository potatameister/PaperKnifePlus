plugins {
    alias(libs.plugins.androidapplication)
    alias(libs.plugins.kotlinandroid)
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
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
    implementation(libs.androidxcorektx)
    implementation(libs.androidxlifecycleruntime)
    implementation(libs.androidxactivitycompose)
    implementation(platform(libs.androidxcomposebom))
    implementation(libs.androidxui)
    implementation(libs.androidxuigraphics)
    implementation(libs.androidxuitoolingpreview)
    implementation(libs.androidxmaterial3)
    implementation(libs.pdfboxandroid)
    implementation(libs.coilcompose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidxjunit)
    androidTestImplementation(libs.androidxespressocore)
    androidTestImplementation(platform(libs.androidxcomposebom))
    androidTestImplementation(libs.androidxuitestjunit4)
    debugImplementation(libs.androidxuitooling)
    debugImplementation(libs.androidxuitestmanifest)
}
