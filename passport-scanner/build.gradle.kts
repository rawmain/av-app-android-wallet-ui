/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-parcelize")
}

android {
    namespace = "eu.europa.ec.passportscanner"
    compileSdk = 35

    defaultConfig {
        minSdk = 28
        vectorDrawables.useSupportLibrary = true
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    lint {
        abortOnError = false
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isJniDebuggable = true
        }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

configurations.all {
    exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
}

dependencies {
    implementation(files("libs/jj2000_imageutil.jar"))

    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)

    // ML Kit dependencies
    implementation(libs.mlkit.text.recognition)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // MRZ (logging replaced slf4j with Timber)
    implementation(libs.timber)

    // JSON processing
    implementation(libs.gson)
    implementation(libs.json.path)
    implementation(libs.json.flattener)

    // NFC dependencies
    implementation(libs.jmrtd)
    implementation(libs.spongycastle.prov)
    implementation(libs.scuba.sc.android)
    implementation(libs.cert.cvc)

    // WSQ
    implementation(libs.jnbis)

    // Utilities
    implementation("commons-codec:commons-codec:1.19.0")

    // RxJava
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")

    // WorkManager
    implementation(libs.androidx.work.ktx)

    // Browser (for Custom Tabs)
    implementation(libs.androidx.browser)

    // JWT
    api("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-orgjson:0.13.0") {
        exclude(group = "org.json", module = "json") // provided by Android natively
    }
    implementation("io.jsonwebtoken:jjwt-gson:0.13.0")
}
