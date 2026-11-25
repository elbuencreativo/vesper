import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Clave de firma propia, fuera del repositorio (~/.vesper-signing). Sin ella se firma con la de depuración.
val signingProps = Properties().apply {
    val f = file(System.getProperty("user.home") + "/.vesper-signing/keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.minimal.tvlauncher"
    compileSdk {
        version = release(37) { minorApiLevel = 1 }
    }

    defaultConfig {
        applicationId = "com.minimal.tvlauncher"
        minSdk = 28
        targetSdk = 34
        versionCode = 15
        versionName = "0.6.2"
        // Dirección del update.json que consulta "Buscar actualización" (ver tools/publish_update.sh).
        val updateUrl = (project.findProperty("updateUrl") as String?).orEmpty()
        buildConfigField("String", "UPDATE_URL", "\"$updateUrl\"")
        // Clave de TMDB (logos). Vive en ~/.vesper-signing, fuera del repositorio; sin ella la opción de logos no se ofrece.
        buildConfigField("String", "TMDB_KEY", "\"${signingProps.getProperty("tmdbKey", "")}\"")
    }

    signingConfigs {
        if (signingProps.containsKey("storeFile")) {
            create("vesper") {
                storeFile = file(signingProps.getProperty("storeFile"))
                storePassword = signingProps.getProperty("storePassword")
                keyAlias = signingProps.getProperty("keyAlias")
                keyPassword = signingProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("vesper") ?: signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.11.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.datastore:datastore-preferences:1.3.0")
    // Instala el perfil de arranque (baseline-prof.txt) al primer uso, incluso con instalación por ADB.
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation("androidx.tvprovider:tvprovider:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    testImplementation("junit:junit:4.13.2")
    // org.json real para probar el parseo de art.json en JVM (el de android.jar son stubs).
    testImplementation("org.json:json:20240303")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}
