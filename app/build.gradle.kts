plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "app.enk"
    compileSdk = 35

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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
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
}

tasks.matching { it.name.contains("preparePaparazzi") }.configureEach {
    doLast {
        val paparazziDir = layout.buildDirectory.dir("intermediates/paparazzi").orNull?.asFile
        paparazziDir?.walk()?.filter { it.name == "resources.json" }?.forEach { file ->
            val content = file.readText()
            if (content.contains("\"targetSdkVersion\": \"35\"")) {
                file.writeText(content.replace("\"targetSdkVersion\": \"35\"", "\"targetSdkVersion\": \"34\""))
            }
        }
    }
}

tasks.register("generateCredits") {
    val csvFile = rootProject.file("docs/provenance.csv")
    val jsonFile = file("src/main/assets/credits.json")
    val libCsvFile = rootProject.file("docs/libraries.csv")
    val libAssetFile = file("src/main/assets/libraries.csv")
    inputs.file(csvFile)
    inputs.file(libCsvFile)
    outputs.file(jsonFile)
    outputs.file(libAssetFile)
    doLast {
        val lines = csvFile.readLines().filter { it.isNotBlank() }
        val header = lines.first().split(",")
        val fileIdx = header.indexOf("file")
        val sourceIdx = header.indexOf("source_url")
        val authorIdx = header.indexOf("author")
        val licenseIdx = header.indexOf("license")
        val records = lines.drop(1).map { line ->
            val cols = line.split(",")
            mapOf(
                "title" to cols[fileIdx],
                "author" to cols[authorIdx],
                "source" to cols[sourceIdx],
                "license" to cols[licenseIdx]
            )
        }
        val json = groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(records))
        jsonFile.parentFile.mkdirs()
        jsonFile.writeText(json + "\n")

        libAssetFile.parentFile.mkdirs()
        libAssetFile.writeText(libCsvFile.readText())
    }
}

tasks.register("checkCredits") {
    mustRunAfter("generateCredits")
    val csvFile = rootProject.file("docs/provenance.csv")
    val jsonFile = file("src/main/assets/credits.json")
    val libCsvFile = rootProject.file("docs/libraries.csv")
    val libAssetFile = file("src/main/assets/libraries.csv")
    inputs.file(csvFile)
    inputs.file(jsonFile)
    inputs.file(libCsvFile)
    inputs.file(libAssetFile)
    doLast {
        if (!jsonFile.exists()) {
            throw GradleException("credits.json does not exist. Run ./gradlew generateCredits")
        }
        if (!libAssetFile.exists()) {
            throw GradleException("libraries.csv does not exist in assets. Run ./gradlew generateCredits")
        }
        val lines = csvFile.readLines().filter { it.isNotBlank() }
        val header = lines.first().split(",")
        val fileIdx = header.indexOf("file")
        val sourceIdx = header.indexOf("source_url")
        val authorIdx = header.indexOf("author")
        val licenseIdx = header.indexOf("license")
        val records = lines.drop(1).map { line ->
            val cols = line.split(",")
            mapOf(
                "title" to cols[fileIdx],
                "author" to cols[authorIdx],
                "source" to cols[sourceIdx],
                "license" to cols[licenseIdx]
            )
        }
        val expected = groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(records)) + "\n"
        val actual = jsonFile.readText().replace("\r\n", "\n")
        if (expected != actual) {
            throw GradleException("credits.json is out of date with docs/provenance.csv. Run ./gradlew generateCredits")
        }
        val expectedLibs = libCsvFile.readText().replace("\r\n", "\n")
        val actualLibs = libAssetFile.readText().replace("\r\n", "\n")
        if (expectedLibs != actualLibs) {
            throw GradleException("assets/libraries.csv is out of date with docs/libraries.csv. Run ./gradlew generateCredits")
        }
    }
}

tasks.named("check") {
    dependsOn("checkCredits")
}
