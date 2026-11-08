package app.enk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ManifestTest {

    private val forbiddenPermissions = listOf(
        "android.permission.INTERNET",
        "android.permission.ACCESS_NETWORK_STATE",
        "android.permission.ACCESS_WIFI_STATE",
        "android.permission.CHANGE_NETWORK_STATE",
        "android.permission.CHANGE_WIFI_STATE",
        "android.permission.CHANGE_WIFI_MULTICAST_STATE",
        "android.permission.NEARBY_WIFI_DEVICES"
    )

    private fun findMergedManifestFiles(): List<File> {
        val candidateDirs = listOf(
            File("build/intermediates/merged_manifests/debug"),
            File("app/build/intermediates/merged_manifests/debug"),
            File("../app/build/intermediates/merged_manifests/debug")
        )

        return candidateDirs
            .filter { it.exists() }
            .flatMap { dir ->
                dir.walkTopDown().filter { it.isFile && it.name == "AndroidManifest.xml" }.toList()
            }
    }

    @Test
    fun noNetworkPermissions() {
        val manifestFiles = findMergedManifestFiles()

        assertTrue(
            "Merged AndroidManifest.xml not found. Ensure :app:processDebugManifest has run.",
            manifestFiles.isNotEmpty()
        )

        for (manifestFile in manifestFiles) {
            val content = manifestFile.readText()
            for (permission in forbiddenPermissions) {
                assertFalse(
                    "Forbidden network permission '$permission' found in ${manifestFile.path}",
                    content.contains(permission)
                )
            }
        }
    }

    @Test
    fun packageIdIsEnkNotes() {
        val manifestFiles = findMergedManifestFiles()
        assertTrue("Merged AndroidManifest.xml not found", manifestFiles.isNotEmpty())

        for (manifestFile in manifestFiles) {
            val content = manifestFile.readText()
            assertTrue(
                "Expected package 'app.enk.notes' in ${manifestFile.path}",
                content.contains("package=\"app.enk.notes\"")
            )
        }
    }

    @Test
    fun minSdkIs31() {
        val manifestFiles = findMergedManifestFiles()
        assertTrue("Merged AndroidManifest.xml not found", manifestFiles.isNotEmpty())

        for (manifestFile in manifestFiles) {
            val content = manifestFile.readText()
            assertTrue(
                "Expected minSdkVersion 31 in ${manifestFile.path}",
                content.contains("android:minSdkVersion=\"31\"")
            )
        }
    }

    @Test
    fun rtlIsDisabled() {
        val manifestFiles = findMergedManifestFiles()
        assertTrue("Merged AndroidManifest.xml not found", manifestFiles.isNotEmpty())

        for (manifestFile in manifestFiles) {
            val content = manifestFile.readText()
            assertTrue(
                "Expected supportsRtl=\"false\" in ${manifestFile.path}",
                content.contains("android:supportsRtl=\"false\"")
            )
        }
    }

    @Test
    fun allowBackupIsFalse() {
        val manifestFiles = findMergedManifestFiles()
        assertTrue("Merged AndroidManifest.xml not found", manifestFiles.isNotEmpty())

        for (manifestFile in manifestFiles) {
            val content = manifestFile.readText()
            assertTrue(
                "Expected allowBackup=\"false\" in ${manifestFile.path}",
                content.contains("android:allowBackup=\"false\"")
            )
        }
    }
}

