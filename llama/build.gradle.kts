plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "android.llama.cpp"
    compileSdk = 35

    defaultConfig {
        minSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        ndk {
            // Add NDK properties if wanted, e.g.
            //abiFilters += listOf("arm64-v8a")
            abiFilters.add("arm64-v8a")
        }
        externalNativeBuild {
            cmake {
                arguments += "-DLLAMA_CURL=OFF"
                arguments += "-DLLAMA_BUILD_COMMON=ON"
                arguments += "-DGGML_LLAMAFILE=OFF"
                arguments += "-DCMAKE_BUILD_TYPE=Release"

                // Argomenti per OpenCL che abbiamo aggiunto prima
                arguments += "-DLLAMA_CLBLAST=ON"
                arguments += "-DGGML_OPENCL_F16=1"

                // --- NUOVI ARGOMENTI PER OTTIMIZZAZIONE ANDROID/SNAPDRAGON ---

                // Incorpora i kernel OpenCL direttamente nella libreria .so
                // (Fortemente raccomandato per Android)
                arguments += "-DGGML_OPENCL_EMBED_KERNELS=ON"

                // Usa i kernel specifici e ottimizzati per le GPU Adreno (Snapdragon)
                // (Fondamentale per le performance su Snapdragon)
                arguments += "-DGGML_OPENCL_USE_ADRENO_KERNELS=ON"
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

    val buildLlama = rootProject.extra["buildLlama"] as? Boolean ?: false
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
