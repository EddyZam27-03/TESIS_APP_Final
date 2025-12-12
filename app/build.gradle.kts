plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.ensenando"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.ensenando"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        
        // ✅ CORRECCIÓN: MediaPipe solo soporta ARM, NO x86/x86_64
        // Si usas emulador, debe ser ARM64-v8a
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
            // NO incluir "x86" ni "x86_64" - MediaPipe no tiene librerías para estas arquitecturas
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
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

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/*.kotlin_module"
            )
        }
        jniLibs {
            // ✅ CRÍTICO: useLegacyPackaging = true asegura que las librerías .so se extraigan del APK
            useLegacyPackaging = true
            // ✅ Asegurar que TODAS las librerías nativas de MediaPipe se incluyan
            // pickFirsts resuelve conflictos cuando hay múltiples versiones de la misma librería
            pickFirsts += listOf(
                "**/libc++_shared.so",
                "**/libmediapipe_tasks_vision_jni.so",
                "**/libmediapipe_jni.so",
                "**/libtensorflowlite_jni.so",
                "**/libtensorflowlite_jni_*.so",
                "**/libmediapipe_tasks_core_jni.so",
                "**/libmediapipe*.so",
                "**/libtensorflow*.so"
            )
            // ✅ NO excluir ninguna librería nativa
            // Mantener todas las librerías .so en el APK
        }
    }

    splits {
        abi {
            // ✅ CORRECCIÓN: Solo habilitar splits si es necesario
            // MediaPipe solo soporta ARM, así que solo incluimos ARM
            isEnable = false // Deshabilitado para generar un APK universal con solo ARM
            // Si necesitas splits, descomenta y ajusta:
            // isEnable = true
            // reset()
            // include("armeabi-v7a", "arm64-v8a") // Solo ARM, NO x86/x86_64
            // isUniversalApk = true
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)

    // CameraX
    implementation(libs.bundles.camerax)

    implementation("com.google.android.material:material:1.11.0")
    // MediaPipe Tasks
    // ✅ ACTUALIZADO: Versión más reciente con mejor soporte de librerías nativas
    implementation("com.google.mediapipe:tasks-core:0.10.14") { isTransitive = true }
    implementation("com.google.mediapipe:tasks-vision:0.10.14") { isTransitive = true }

    // Room (KSP)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Lifecycle
    implementation(libs.bundles.lifecycle)
    implementation(libs.bundles.lifecycle)


    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // TensorFlow Lite
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.gpu)
    implementation(libs.tensorflow.lite.support)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)

    // Corrutinas - Versión estable y compatible
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Security Crypto
    implementation(libs.androidx.security.crypto)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
