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
        jcenter()
        google()
        maven { setUrl("https://www.jitpack.io") }
    }
}

subprojects {
    apply(plugin = "org.jmailen.kotlinter")

    kotlinter {
        experimentalRules = true
    }
}

buildscript {
    dependencies {
        classpath("com.github.zellius:android-shortcut-gradle-plugin:0.1.2")
        classpath("com.google.gms:google-services:4.3.3")
        classpath("com.mikepenz.aboutlibraries.plugin:aboutlibraries-plugin:8.3.0")
    }
    repositories {
        google()
        jcenter()
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
