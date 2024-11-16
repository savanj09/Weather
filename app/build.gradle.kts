plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.weatherapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.weatherapp"
        minSdk = 24
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    // The Google Material Dependency
    //implementation 'com.google.android.material:material:1.1.0'

    // This is for getting the location
    //implementation 'com.google.android.gms:play-services-location:17.0.0'

    // Dexter runtime permissions
    // https://github.com/Karumi/Dexter
   // implementation 'com.karumi:dexter:6.0.1'
    implementation(libs.service.location)
    implementation(libs.karumi.dexter)
    implementation(libs.rerofit)
   implementation(libs.retrofit.gson)

    //implementation 'com.squareup.retrofit2:retrofit:2.6.2'
    //implementation 'com.squareup.retrofit:converter-gson:2.0.0-beta2'
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}