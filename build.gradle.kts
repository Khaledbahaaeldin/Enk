plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.paparazzi) apply false
    alias(libs.plugins.ksp) apply false
}

// Android modules target Java 17 bytecode. The build itself runs on JDK 21, which the Backdrop
// library needs for the JVM screenshot tests (its classes are compiled for Java 21).
subprojects {
    pluginManager.withPlugin("com.android.base") {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}
