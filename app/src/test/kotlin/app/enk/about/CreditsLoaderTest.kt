package app.enk.about

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.URI

/** Reads the real repository files (tests run in `app/`), so a stale or incomplete credit fails here. */
class CreditsLoaderTest {

    private fun repoFile(path: String): File =
        listOf(File("../$path"), File(path)).firstOrNull { it.exists() }
            ?: error("$path not found from ${File(".").absolutePath}")

    private fun creditsJson() = repoFile("app/src/main/assets/credits.json").readText()

    private fun validUrl(url: String): Boolean = try {
        val uri = URI(url)
        (uri.scheme == "https" || uri.scheme == "http") && !uri.host.isNullOrBlank()
    } catch (_: Exception) {
        false
    }

    @Test
    fun everyProvenanceRowAppearsInCreditsWithItsFields() {
        val lines = repoFile("docs/provenance.csv").readLines().filter { it.isNotBlank() }
        val header = lines.first().split(",")
        val expected = lines.drop(1).map { line ->
            val cols = line.split(",")
            Credit(
                title = cols[header.indexOf("file")],
                author = cols[header.indexOf("author")],
                source = cols[header.indexOf("source_url")],
                license = cols[header.indexOf("license")]
            )
        }

        assertEquals(expected, loadCredits(creditsJson()))
        for (credit in expected) {
            assertTrue("source ${credit.source}", validUrl(credit.source))
            assertFalse(credit.author.isBlank())
            assertFalse(credit.license.isBlank())
        }
    }

    @Test
    fun malformedOrEmptyJsonReturnsEmptyListRatherThanCrash() {
        assertEquals(emptyList<Credit>(), loadCredits(""))
        assertEquals(emptyList<Credit>(), loadCredits("   "))
        assertEquals(emptyList<Credit>(), loadCredits("{ not valid json }"))
        assertEquals(emptyList<Credit>(), loadCredits("null"))
        assertEquals(emptyList<Credit>(), loadCredits("[1, \"x\", null, {}]"))
    }

    @Test
    fun malformedLibrariesCsvReturnsWhatItCanAndNeverCrashes() {
        assertEquals(emptyList<LibraryCredit>(), loadLibraries(""))
        assertEquals(emptyList<LibraryCredit>(), loadLibraries("name,version,license,source_url"))
        assertEquals(emptyList<LibraryCredit>(), loadLibraries("a,b\nc,d"))
        val rows = loadLibraries("name,version,license,source_url\nx:y,1.0,MIT,https://e.org\n\nshort,row")
        assertEquals(listOf(LibraryCredit("x:y", "1.0", "MIT", "https://e.org")), rows)
    }

    // ---- libraries cannot drift from the version catalog ----

    /** `alias -> (module, version or null for BOM-managed artifacts)` from gradle/libs.versions.toml. */
    private fun catalog(): Map<String, Pair<String, String?>> {
        val toml = repoFile("gradle/libs.versions.toml").readLines()
        val versions = toml.dropWhile { !it.startsWith("[versions]") }.drop(1).takeWhile { !it.startsWith("[") }
            .mapNotNull { Regex("""^(\S+)\s*=\s*"([^"]+)"""").find(it)?.destructured }
            .associate { (name, value) -> name to value }
        return toml.dropWhile { !it.startsWith("[libraries]") }.drop(1).takeWhile { !it.startsWith("[") }
            .mapNotNull { line ->
                val m = Regex("""^(\S+)\s*=\s*\{(.*)}""").find(line) ?: return@mapNotNull null
                val body = m.groupValues[2]
                val module = Regex("""module\s*=\s*"([^"]+)"""").find(body)?.groupValues?.get(1) ?: return@mapNotNull null
                val ref = Regex("""version\.ref\s*=\s*"([^"]+)"""").find(body)?.groupValues?.get(1)
                m.groupValues[1] to (module to ref?.let { versions[it] })
            }.toMap()
    }

    /** Catalog aliases used by a runtime configuration (`implementation`/`api`) in any module. */
    private fun runtimeAliases(): Set<String> =
        listOf("app", "design", "data", "audio", "text").flatMap { module ->
            repoFile("$module/build.gradle.kts").readLines()
                .map { it.trim() }
                .filter { it.startsWith("implementation(") || it.startsWith("api(") }
                .flatMap { Regex("""libs\.([A-Za-z0-9.]+)""").findAll(it).map { m -> m.groupValues[1] }.toList() }
        }.map { it.replace('.', '-') }.toSet()

    @Test
    fun everyRuntimeDependencyInTheVersionCatalogHasAMatchingLibrariesRow() {
        val catalog = catalog()
        val rows = loadLibraries(repoFile("docs/libraries.csv").readText()).associateBy { it.name }
        val aliases = runtimeAliases()
        assertTrue("found no runtime dependencies: the build file scan is broken", aliases.size >= 10)

        for (alias in aliases) {
            val (module, version) = catalog[alias] ?: error("$alias is used in a build file but not in the catalog")
            val row = rows[module] ?: error("runtime dependency $module has no row in docs/libraries.csv")
            if (version != null) assertEquals("version of $module", version, row.version)
        }
    }

    @Test
    fun noLibrariesRowIsStaleOrIncomplete() {
        val catalog = catalog()
        val modules = runtimeAliases().map { catalog.getValue(it).first }.toSet()
        val rows = loadLibraries(repoFile("docs/libraries.csv").readText())

        assertEquals("no duplicate rows", rows.size, rows.map { it.name }.toSet().size)
        for (row in rows) {
            assertTrue("${row.name} is listed but no module uses it at runtime", row.name in modules)
            assertFalse("${row.name} has no version", row.version.isBlank())
            assertFalse("${row.name} has no license", row.license.isBlank())
            assertTrue("${row.name}: bad URL ${row.sourceUrl}", validUrl(row.sourceUrl))
        }
    }

    @Test
    fun theLibrariesAssetIsTheCsv() {
        assertEquals(
            repoFile("docs/libraries.csv").readText().replace("\r\n", "\n"),
            repoFile("app/src/main/assets/libraries.csv").readText().replace("\r\n", "\n")
        )
    }

    // ---- sounds are grouped, not listed per file ----

    @Test
    fun theRealSoundCreditsCollapseToOneRowPerSourceAndNameNoFilePaths() {
        val credits = loadCredits(creditsJson())
        val groups = groupSoundCredits(credits)

        assertEquals(credits.map { Triple(it.author, it.license, it.source) }.toSet().size, groups.size)
        assertTrue("grouped list should be short, was ${groups.size}", groups.size in 1..10)
        for (g in groups) {
            assertFalse(g.author.contains("/") || g.usedIn.contains(".ogg") || g.usedIn.contains("sounds/"))
            assertTrue(g.usedIn.isNotBlank())
        }
        // Every sound set in the files is mentioned somewhere.
        val sets = credits.map { it.title.split("/")[1].replaceFirstChar(Char::uppercase) }.toSet()
        for (set in sets) assertTrue("$set missing", groups.any { it.usedIn.contains("$set:") })
    }

    @Test
    fun groupingMergesKeysAndKeepsOrderAndSurvivesOddPaths() {
        val groups = groupSoundCredits(
            listOf(
                Credit("sounds/classic/key_01.ogg", "ann", "https://a.org/1", "CC0"),
                Credit("sounds/classic/key_02.ogg", "ann", "https://a.org/1", "CC0"),
                Credit("sounds/classic/space.ogg", "ann", "https://a.org/1", "CC0"),
                Credit("sounds/soft/key_01.ogg", "ann", "https://a.org/1", "CC0"),
                Credit("sounds/classic/bell.ogg", "bo", "https://b.org/2", "CC0"),
                Credit("weird", "cy", "https://c.org/3", "CC-BY"),
                Credit("", "di", "https://d.org/4", "CC0")
            )
        )
        assertEquals("Classic: keys, space; Soft: keys", groups[0].usedIn)
        assertEquals("Classic: bell", groups[1].usedIn)
        assertEquals("weird", groups[2].usedIn)
        assertEquals(4, groups.size)
        assertTrue(groupSoundCredits(emptyList()).isEmpty())
    }

    // ---- licence texts ----

    @Test
    fun theApacheTextIsTheRepositoryLicenseAndTheFontLicencesCarryTheirCopyrightLine() {
        val apache = repoFile("app/src/main/assets/licenses/Apache-2.0.txt").readText().replace("\r\n", "\n")
        assertEquals(repoFile("LICENSE").readText().replace("\r\n", "\n"), apache)
        assertTrue(apache.contains("Apache License"))

        for (doc in listOf(LicenseDocs.CourierPrime, LicenseDocs.Inter)) {
            val text = repoFile("app/src/main/assets/${doc.assetPath}").readText()
            assertTrue("${doc.title} must start with its copyright line", text.trimStart().startsWith("Copyright"))
            assertTrue(text.contains("SIL OPEN FONT LICENSE Version 1.1"))
        }
        assertEquals("Apache License, Version 2.0", LicenseDocs.App.title)
    }
}
