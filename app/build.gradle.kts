plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.luposolitario.immundanoctis"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.luposolitario.immundanoctis"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.runtime.livedata)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.tasks.genai)
    implementation(libs.androidx.concurrent.futures.ktx)
    implementation(libs.androidx.work.runtime.ktx) // O la versione più recente

    implementation(libs.androidx.lifecycle.viewmodel.compose) // O la versione più recente
    implementation(libs.gson)
    implementation(libs.translate)

    implementation(libs.jackson.module.kotlin) // Ultima versione stabile
    implementation(libs.jackson.databind)

    implementation(libs.language.id)


    implementation(project(":llama"))
    implementation(project(":stdf"))

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    testImplementation(kotlin("test"))

    // AGGIUNGI QUESTA LINEA per assicurare che le asserzioni JUnit siano disponibili nei test di strumentazione
    // libs.junit si riferisce a junit:junit:4.13.2 tramite il tuo file version catalogs.
    androidTestImplementation(libs.junit)

    // Opzionale: Se userai Mockito nei test di strumentazione per mockare classi Android.
    // Se non intendi mockare Context/AssetManager o altre classi Android in AssetLoadingTest, non è strettamente necessaria.
    // In caso contrario, aggiungila:
    // androidTestImplementation(libs.mockito.android) // Assicurati di averla definita in libs.versions.toml
}