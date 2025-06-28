plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.github.luposolitario.stdf"
    compileSdk = 35

    defaultConfig {
        minSdk = 34

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        externalNativeBuild {
            cmake {

            }
        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += "arm64-v8a"
            }
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
    val buildLlama = rootProject.extra["buildStdf"] as? Boolean ?: false
    if (buildLlama) {
        externalNativeBuild {
            cmake {
                path("src/main/cpp/CMakeLists.txt")
                version = "3.22.1"
            }
        }
    } else {
        println("Compilazione di Llama disabilitata (buildLlama = false)")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    // --- 👇 QUESTA È LA CONFIGURAZIONE CHIAVE 👇 ---
    // Diciamo a Gradle dove trovare le nostre librerie pre-compilate
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
