// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    val kotlin_version by extra("1.7.20")
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        // TODO: Remove JCenter
        @Suppress("JcenterRepositoryObsolete")
        jcenter {
            content {
                //  org.jetbrains.trove4j is only available in JCenter
                includeGroup("org.jetbrains.trove4j")
            }
        }
    }

    dependencies {
        classpath(Plugins.agp)
        classpath(Plugins.hilt)
        classpath(Plugins.kotlin)
        classpath(Plugins.gms)
        classpath(Plugins.crashlytics)
        classpath(Plugins.safeArgs)
        // classpath("in.navanatech.zabaan:zabaan-gradle-plugin:1.1.0")
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle.kts files
    }
}

val localProperties = java.util.Properties()
localProperties.load(java.io.FileInputStream(rootProject.file("local.properties")))

//apply(plugin = "in.navanatech.zabaan")
//
//configure<`in`.navanatech.zabaan.ZabaanExtension> {
//    apkPath = "/home/skrilltrax/Work/ruraFl-crowdsourcing-toolkit/client/app/release/app-release.aab"
//    upload {
//        releaseNumber = localProperties.getProperty("zbn.release") as String
//        releaseToken = localProperties.getProperty("zbn.token") as String
//    }
//}


allprojects {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://maven.pkg.github.com/navana-tech/zabaan-sdk") {
            credentials {
                username = localProperties.getProperty("gpr.user") ?: System.getenv("USERNAME")
                password = localProperties.getProperty("gpr.key") ?: System.getenv("PASSWORD")
            }
        }
        maven(url = "https://jitpack.io")
        // TODO: Remove JCenter
        @Suppress("JcenterRepositoryObsolete")
        jcenter {
            content {
                includeGroup("com.amitshekhar.android")
                includeGroup("com.kofigyan.stateprogressbar")
                includeGroup("org.jetbrains.trove4j")
            }
        }
    }
}

tasks {
    val clean by registering(Delete::class) {
        delete(rootProject.buildDir)
    }
}
