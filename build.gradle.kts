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

    buildFeatures {
        buildConfig = true
    }

    // একই কোড থেকে দুইটা APK: trial (সময় শেষে বন্ধ) ও paid (লাইসেন্স কোড লাগবে)
    flavorDimensions += "tier"
    productFlavors {
        create("trial") {
            dimension = "tier"
            applicationIdSuffix = ".trial"
            versionNameSuffix = "-trial"
            buildConfigField("boolean", "IS_PAID", "false")
        }
        create("paid") {
            dimension = "tier"
            buildConfigField("boolean", "IS_PAID", "true")
        }
    }

    // ম্যাক্সিমাম R8 অবফাসকেশন/শ্রিংকিং চালু — নেটিভ কোড নেই, তাই ফাইল ছোট থাকে
    buildTypes {
        getByName("debug") {
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

// রুটে adnan.jpg (বা .png/.jpeg) রাখলে বিল্ডের আগে নিজে res-এ কপি হয়ে অ্যাপে ঢুকবে
tasks.register<Copy>("copyProfilePhoto") {
    from(fileTree(".") { include("adnan.jpg", "adnan.jpeg", "adnan.png") })
    into("res/drawable-nodpi")
}
tasks.matching { it.name == "preBuild" }.configureEach { dependsOn("copyProfilePhoto") }
