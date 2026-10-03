// Reines Java ohne Bibliotheken: hält die APK sehr klein.
plugins {
    id("com.android.application")
}

android {
    namespace = "com.flitz.igel"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.flitz.igel"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"
    }

    signingConfigs {
        // Gleicher fester Debug-Key wie die DnB-App, damit neue APKs als Update installiert werden können.
        getByName("debug") {
            storeFile = rootProject.file("app/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    // Das Spiel selbst liegt als eine HTML-Datei in flitz-game/ und wird offline mitgeliefert.
    sourceSets {
        getByName("main").assets.srcDirs(rootProject.file("flitz-game"))
    }
    androidResources {
        // Nur index.html wird gebraucht.
        ignoreAssetsPatterns.addAll(listOf("!game.html", "!README.md"))
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

