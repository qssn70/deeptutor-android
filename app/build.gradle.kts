plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "dev.qssn70.deeptutor"
    compileSdk = 35
    defaultConfig { applicationId = "dev.qssn70.deeptutor"; minSdk = 26; targetSdk = 35; versionCode = 2; versionName = "0.2.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }

    // GitHub Actions uses a persistent signing key stored in repository secrets.
    // Local builds retain the ordinary Android debug signing behavior.
    val keystorePath = System.getenv("DEEPTUTOR_KEYSTORE_PATH")
    if (!keystorePath.isNullOrBlank()) {
        signingConfigs {
            getByName("debug") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("DEEPTUTOR_STORE_PASSWORD")
                    ?: error("Missing DEEPTUTOR_STORE_PASSWORD")
                keyAlias = System.getenv("DEEPTUTOR_KEY_ALIAS")
                    ?: error("Missing DEEPTUTOR_KEY_ALIAS")
                keyPassword = System.getenv("DEEPTUTOR_KEY_PASSWORD")
                    ?: error("Missing DEEPTUTOR_KEY_PASSWORD")
            }
        }
    }
}
dependencies { implementation("androidx.webkit:webkit:1.17.1"); implementation("androidx.core:core-ktx:1.16.0") }
