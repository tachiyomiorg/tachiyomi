plugins {
    id("com.android.application") version BuildPluginsVersion.AGP apply false
    id("com.android.library") version BuildPluginsVersion.AGP apply false
    kotlin("android") version BuildPluginsVersion.KOTLIN apply false
    id("org.jmailen.kotlinter") version BuildPluginsVersion.KOTLINTER
    id("com.github.ben-manes.versions") version BuildPluginsVersion.VERSIONS_PLUGIN
}

allprojects {
    repositories {
        mavenCentral()
        google()
        maven { setUrl("https://www.jitpack.io") }
        jcenter()
    }
}

subprojects {
    apply(plugin = "org.jmailen.kotlinter")

    kotlinter {
        experimentalRules = true

        // Doesn't play well with Android Studio
        disabledRules = arrayOf("experimental:argument-list-wrapping")
    }
}

buildscript {
    dependencies {
        classpath("com.github.zellius:android-shortcut-gradle-plugin:0.1.2")
        classpath("com.google.gms:google-services:4.3.8")
        classpath("com.mikepenz.aboutlibraries.plugin:aboutlibraries-plugin:${BuildPluginsVersion.ABOUTLIB_PLUGIN}")
        classpath(kotlin("serialization", version = BuildPluginsVersion.KOTLIN))
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
