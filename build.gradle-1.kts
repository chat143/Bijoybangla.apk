plugins {
    id("com.android.application") version "8.5.2"
    id("org.jetbrains.kotlin.android") version "1.9.24"
}

android {
    namespace = "com.bijoy.keyboard"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.taz.bijoykeyboard"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets {
        getByName("main") {
            manifest.srcFile("AndroidManifest.xml")
            res.setSrcDirs(listOf("res"))
        }
    }
}

kotlin {
    jvmToolchain(17)
    // রুট ফোল্ডারের সব .kt ফাইল কম্পাইল হবে
    sourceSets.getByName("main").kotlin.apply {
        setSrcDirs(listOf("."))
        include("*.kt")
    }
}
