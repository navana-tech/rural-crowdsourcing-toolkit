import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-android-extensions")
    id("kotlin-kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("dagger.hilt.android.plugin")
    id("androidx.navigation.safeargs.kotlin")
    id("com.ncorti.ktfmt.gradle") version "0.7.0"
    id("com.github.ben-manes.versions") version "0.38.0"
}

android {
  compileSdk = 31
    defaultConfig {
        applicationId = "com.navana.bolo"
        minSdkVersion(21)
        targetSdkVersion(31)
        multiDexEnabled = true
        versionCode = 24
        versionName = "1.2.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }
        val localProperties = Properties()
        localProperties.load(FileInputStream(rootProject.file("local.properties")))
        buildConfigField("String", "ZABAAN_ACCESS_TOKEN", localProperties.getProperty("zbn.token") as String)
    }
    buildTypes {
        named("release") {
            isMinifyEnabled = false
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    lintOptions {
        isAbortOnError = false
    }
    buildFeatures {
        dataBinding = true
        viewBinding = true
    }
    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/LICENSE")
        exclude("META-INF/LICENSE.txt")
        exclude("META-INF/license.txt")
        exclude("META-INF/NOTICE")
        exclude("META-INF/NOTICE.txt")
        exclude("META-INF/notice.txt")
        exclude("META-INF/ASL2.0")
        exclude("META-INF/*.kotlin_module")
    }
}

ktfmt {
    googleStyle()

    maxWidth.set(120)
    removeUnusedImports.set(true)
}

tasks.register<com.ncorti.ktfmt.gradle.tasks.KtfmtFormatTask>("ktfmtPrecommit") {
    source = project.fileTree(rootDir)
    include("**/*.kt")
}

dependencyLocking {
    lockAllConfigurations()
}

dependencies {

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    implementation(Dependencies.AndroidX.appcompat)
    implementation(Dependencies.AndroidX.constraintLayout)
    implementation(Dependencies.AndroidX.datastorePrefs)
    implementation(Dependencies.AndroidX.fragmentKtx)
    implementation(Dependencies.AndroidX.multidex)
    implementation(Dependencies.AndroidX.work_runtime)

    implementation(Dependencies.AndroidX.Lifecycle.common)
    implementation(Dependencies.AndroidX.Lifecycle.livedataKtx)
    implementation(Dependencies.AndroidX.Lifecycle.runtimeKtx)
    implementation(Dependencies.AndroidX.Lifecycle.saved_state)
    implementation(Dependencies.AndroidX.Lifecycle.viewModelKtx)

    implementation(Dependencies.AndroidX.Navigation.fragmentKtx)
    implementation(Dependencies.AndroidX.Navigation.uiKtx)

    implementation(Dependencies.AndroidX.Room.roomKtx)
    implementation(Dependencies.AndroidX.Room.roomRuntime)

    implementation(Dependencies.AndroidX.Navigation.fragmentKtx)
    implementation(Dependencies.AndroidX.Navigation.uiKtx)

    kapt(Dependencies.AndroidX.Room.roomCompiler)

    implementation(Dependencies.Google.gson)
    implementation(Dependencies.Google.material)

    implementation(platform(Dependencies.Google.Firebase.bom))
    implementation(Dependencies.Google.Firebase.crashlytics)
    implementation(Dependencies.Google.Firebase.analytics)

    implementation(Dependencies.AndroidX.Hilt.dagger)
    implementation(Dependencies.AndroidX.Hilt.hiltNavigationFragment)

    kapt(Dependencies.AndroidX.Hilt.daggerCompiler)
    kapt(Dependencies.AndroidX.Hilt.daggerHiltCompiler)

    implementation(Dependencies.Kotlin.Coroutines.core)
    implementation(Dependencies.Kotlin.Coroutines.coroutines)

    implementation(Dependencies.ThirdParty.circleImageView)
    implementation(Dependencies.ThirdParty.glide)
    implementation(Dependencies.ThirdParty.okhttp)
    implementation(Dependencies.ThirdParty.loggingInterceptor)
    implementation(Dependencies.ThirdParty.lottie)
    implementation(Dependencies.ThirdParty.stateProgressBar)

    implementation(Dependencies.ThirdParty.zabaan) {
        exclude(group = "androidx.room")
    }

    implementation(Dependencies.ThirdParty.Retrofit.retrofit)
    implementation(Dependencies.ThirdParty.Retrofit.gsonConverter)

    implementation("com.google.android.play:core-ktx:1.8.1")

    debugImplementation(Dependencies.ThirdParty.debugDB)

    implementation(project(":app-dropdown"))
    implementation(project(":app-bow"))
    implementation("com.mcxiaoke.volley:library:1.0.19")
}
