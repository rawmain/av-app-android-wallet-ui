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
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(files("libs/jj2000_imageutil.jar"))

    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation(libs.material)

    // ML Kit dependencies
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // MRZ (logging replaced slf4j with Timber)
    implementation(libs.timber)

    // JSON processing
    implementation(libs.gson)
    implementation("com.jayway.jsonpath:json-path:2.9.0")
    implementation("com.github.wnameless.json:json-flattener:0.17.3")

    // Image processing
    implementation("com.github.bumptech.glide:glide:5.0.4")
    implementation("jp.wasabeef:glide-transformations:4.3.0")
    annotationProcessor("com.github.bumptech.glide:compiler:5.0.5")

    // NFC dependencies
    implementation("org.jmrtd:jmrtd:0.8.3")
    implementation("com.madgag.spongycastle:prov:1.58.0.0")
    implementation("net.sf.scuba:scuba-sc-android:0.0.26")
    implementation(group = "org.ejbca.cvc", name = "cert-cvc", version = "1.4.13")

    // WSQ
    implementation("com.github.mhshams:jnbis:2.1.2")

    // Utilities
    implementation("commons-codec:commons-codec:1.19.0")

    // RxJava
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")

    // WorkManager
    implementation(libs.androidx.work.ktx)

    // JWT
    api("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-orgjson:0.13.0") {
        exclude(group = "org.json", module = "json") // provided by Android natively
    }
    implementation("io.jsonwebtoken:jjwt-gson:0.13.0")
}
