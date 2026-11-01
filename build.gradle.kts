plugins {
    id("com.android.application") version "8.5.2"
    id("org.jetbrains.kotlin.android") version "1.9.24"
}
android {
    namespace = "com.bijoy.keyboard"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.bijoy.keyboard"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    // সব ফাইল রুটে: .kt ফাইল ও AndroidManifest.xml রুট থেকেই পড়বে
    sourceSets {
        getByName("main") {
            manifest.srcFile("AndroidManifest.xml")
            java.setSrcDirs(listOf("."))
            java.include("*.kt")
            kotlin.setSrcDirs(listOf("."))
            kotlin.include("*.kt")
            res.setSrcDirs(listOf("res"))
        }
    }
}
