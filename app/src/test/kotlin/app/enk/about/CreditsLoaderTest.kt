package app.enk.about

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CreditsLoaderTest {

    @Test
    fun everyProvenanceRowAppearsInCredits() {
        val csvFile = File("../docs/provenance.csv").let { if (it.exists()) it else File("docs/provenance.csv") }
        val jsonFile = File("src/main/assets/credits.json").let { if (it.exists()) it else File("app/src/main/assets/credits.json") }

        assertTrue(csvFile.exists())
        assertTrue(jsonFile.exists())

        val csvLines = csvFile.readLines().filter { it.isNotBlank() }
        val expectedCount = csvLines.size - 1 // minus header

        val credits = loadCredits(jsonFile.readText())
        assertEquals(expectedCount, credits.size)

        for (credit in credits) {
            assertFalse(credit.title.isBlank())
            assertFalse(credit.author.isBlank())
            assertFalse(credit.source.isBlank())
            assertFalse(credit.license.isBlank())
        }
    }

    @Test
    fun creditsHaveLicenseAndSource() {
        val jsonFile = File("src/main/assets/credits.json").let { if (it.exists()) it else File("app/src/main/assets/credits.json") }
        val credits = loadCredits(jsonFile.readText())

        for (c in credits) {
            assertTrue("Expected valid source URL: ${c.source}", c.source.startsWith("https://") || c.source.startsWith("http://"))
            assertTrue("Expected valid license: ${c.license}", c.license.isNotBlank())
        }
    }

    @Test
    fun malformedOrEmptyJsonReturnsEmptyListRatherThanCrash() {
        assertEquals(emptyList<Credit>(), loadCredits(""))
        assertEquals(emptyList<Credit>(), loadCredits("   "))
        assertEquals(emptyList<Credit>(), loadCredits("{ not valid json }"))
        assertEquals(emptyList<Credit>(), loadCredits("null"))
    }

    @Test
    fun everyRuntimeLibraryAppearsInLibrariesCsv() {
        val libsFile = File("../docs/libraries.csv").let { if (it.exists()) it else File("docs/libraries.csv") }
        assertTrue(libsFile.exists())

        val libraries = loadLibraries(libsFile.readText())
        assertTrue(libraries.isNotEmpty())

        val requiredLibraries = listOf(
            "androidx.core:core-ktx",
            "androidx.core:core-splashscreen",
            "androidx.lifecycle:lifecycle-runtime-ktx",
            "androidx.lifecycle:lifecycle-viewmodel-compose",
            "androidx.activity:activity-compose",
            "androidx.compose.material3:material3",
            "androidx.datastore:datastore-preferences",
            "androidx.room:room-runtime",
            "io.github.kyant0:backdrop",
            "androidx.glance:glance-appwidget"
        )

        val libraryNames = libraries.map { it.name }.toSet()
        for (required in requiredLibraries) {
            assertTrue("Missing runtime library in libraries.csv: $required", libraryNames.contains(required))
        }

        for (lib in libraries) {
            assertFalse(lib.name.isBlank())
            assertFalse(lib.version.isBlank())
            assertFalse(lib.license.isBlank())
            assertTrue("Expected valid URL: ${lib.sourceUrl}", lib.sourceUrl.startsWith("https://") || lib.sourceUrl.startsWith("http://"))
        }
    }

    @Test
    fun appLicenseTextIsApache2() {
        val model = getAppLicenseInfo()
        assertEquals("Apache License, Version 2.0", model.name)
        assertTrue(model.notice.contains("Apache"))
        assertNotNull(model.courierPrimeNotice)
        assertTrue(model.courierPrimeNotice.contains("SIL Open Font License"))
    }
}
