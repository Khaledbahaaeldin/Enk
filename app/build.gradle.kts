import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "app.enk"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.enk.notes"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val properties = Properties()
                properties.load(keystorePropertiesFile.inputStream())
                storeFile = file(properties.getProperty("storeFile"))
                storePassword = properties.getProperty("storePassword")
                keyAlias = properties.getProperty("keyAlias")
                keyPassword = properties.getProperty("keyPassword")
            } else if (System.getenv("ENK_KEYSTORE_PATH") != null) {
                storeFile = file(System.getenv("ENK_KEYSTORE_PATH"))
                storePassword = System.getenv("ENK_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ENK_KEY_ALIAS")
                keyPassword = System.getenv("ENK_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            vcsInfo.include = false
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null && releaseSigning.storeFile!!.exists()) {
                signingConfig = releaseSigning
            }
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Pure helpers shared by unit tests and instrumented tests; never packaged in the app.
    sourceSets {
        getByName("test").kotlin.directories.add("src/sharedTest/kotlin")
        getByName("androidTest").kotlin.directories.add("src/sharedTest/kotlin")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// AGP 9 no longer creates the release unit tests unless asked. They are part of the gate: a class that
// compiles for debug but not for release must fail here, as it did before the upgrade.
androidComponents {
    beforeVariants(selector().withBuildType("release")) { variant ->
        variant.hostTests[com.android.build.api.variant.HostTestBuilder.UNIT_TEST_TYPE]?.enable = true
    }
}

dependencies {
    implementation(project(":text"))
    implementation(project(":design"))
    implementation(project(":audio"))
    implementation(project(":data"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.glance.appwidget)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
}

tasks.matching { it.name == "testDebugUnitTest" }.configureEach {
    inputs.dir(layout.buildDirectory.dir("intermediates/merged_manifests/debug"))
}

tasks.withType<Test>().matching { it.name == "testReleaseUnitTest" }.configureEach {
    // Compose UI tests rely on ui-test-manifest which is debugImplementation only
    exclude("**/MarkerGestureTest*")
    exclude("**/WriterScreenTest*")
    exclude("**/NightThemeTest*")
    exclude("**/GlassNavBarTest*")
    exclude("**/ShellScaffoldTest*")
    exclude("**/StartHandoffTest*")
    exclude("**/SystemBarIconsTest*")
    exclude("**/BoxInteractionTest*")
    exclude("**/SearchScreenTest*")
    exclude("**/SettingsScreenTest*")
    exclude("**/AppearanceGroupTest*")
    exclude("**/ShellScreensLayoutTest*")
    exclude("**/ShellControlsCompositionTest*")
    exclude("**/AccessibilityTest*")
    exclude("**/WriterAccessibilityTest*")
    exclude("**/LargeTextLayoutTest*")
    exclude("**/WriterCompactLayoutTest*")
    exclude("**/*GoldenTest*")
}


// credits.json, libraries.csv and the Apache licence text are generated from their sources so the
// Credits screen cannot drift from docs/provenance.csv, docs/libraries.csv and LICENSE.
val creditsCsv = rootProject.file("docs/provenance.csv")
val creditsJson = file("src/main/assets/credits.json")
val librariesCsv = rootProject.file("docs/libraries.csv")
val librariesAsset = file("src/main/assets/libraries.csv")
val licenseFile = rootProject.file("LICENSE")
val licenseAsset = file("src/main/assets/licenses/Apache-2.0.txt")

fun expectedCreditsJson(): String {
    val lines = creditsCsv.readLines().filter { it.isNotBlank() }
    val header = lines.first().split(",")
    val fileIdx = header.indexOf("file")
    val sourceIdx = header.indexOf("source_url")
    val authorIdx = header.indexOf("author")
    val licenseIdx = header.indexOf("license")
    val records = lines.drop(1).mapIndexed { row, line ->
        val cols = line.split(",")
        if (cols.size != header.size) {
            throw GradleException("docs/provenance.csv row ${row + 2} has ${cols.size} columns, expected ${header.size}")
        }
        mapOf(
            "title" to cols[fileIdx],
            "author" to cols[authorIdx],
            "source" to cols[sourceIdx],
            "license" to cols[licenseIdx]
        )
    }
    return groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(records)) + "\n"
}

fun normalised(file: File): String = file.readText().replace("\r\n", "\n")

tasks.register("generateCredits") {
    inputs.files(creditsCsv, librariesCsv, licenseFile)
    outputs.files(creditsJson, librariesAsset, licenseAsset)
    doLast {
        creditsJson.parentFile.mkdirs()
        creditsJson.writeText(expectedCreditsJson())
        librariesAsset.writeText(normalised(librariesCsv))
        licenseAsset.parentFile.mkdirs()
        licenseAsset.writeText(normalised(licenseFile))
    }
}

tasks.register("checkCredits") {
    mustRunAfter("generateCredits")
    inputs.files(creditsCsv, librariesCsv, licenseFile, creditsJson, librariesAsset, licenseAsset)
    doLast {
        for (generated in listOf(creditsJson, librariesAsset, licenseAsset)) {
            if (!generated.exists()) throw GradleException("${generated.name} is missing. Run ./gradlew generateCredits")
        }
        if (expectedCreditsJson() != normalised(creditsJson)) {
            throw GradleException("credits.json is out of date with docs/provenance.csv. Run ./gradlew generateCredits")
        }
        if (normalised(librariesCsv) != normalised(librariesAsset)) {
            throw GradleException("assets/libraries.csv is out of date with docs/libraries.csv. Run ./gradlew generateCredits")
        }
        if (normalised(licenseFile) != normalised(licenseAsset)) {
            throw GradleException("assets/licenses/Apache-2.0.txt is out of date with LICENSE. Run ./gradlew generateCredits")
        }
    }
}

tasks.named("check") {
    dependsOn("checkCredits")
}

// generateCredits writes into src/main/assets; when both run in one build, assets are merged after it.
tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
    mustRunAfter("generateCredits")
}
