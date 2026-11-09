import java.util.Properties

val localProps = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.vulcan"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.vulcan"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${localProps.getProperty("GEMINI_API_KEY", "")}\""
        )
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    // Retrofit: cliente REST
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    // Convierte JSON <-> clases de Kotlin
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    // Muestra en Logcat las peticiones (útil para depurar)
    implementation("com.squareup.okhttp3:logging-interceptor:5.5.1")
    // Corrutinas para no bloquear la interfaz
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    // ViewModel y lifecycleScope
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    // Para el Snackbar (normalmente ya viene en proyectos nuevos; si no, agrégala)
    implementation("com.google.android.material:material:1.14.0")
}