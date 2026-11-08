package app.enk

import app.enk.audio.SoundManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(RobolectricTestRunner::class)
class ProvenanceTest {

    private val allowedLicenses = setOf("CC0", "CC-BY-4.0", "CC-BY-3.0", "CC-BY-2.0")

    private fun findRepoRoot(): File {
        var current: File? = File(".").canonicalFile
        while (current != null) {
            if (File(current, "settings.gradle.kts").exists()) {
                return current
            }
            current = current.parentFile
        }
        error("Could not find repository root")
    }

    @Test
    fun verifySoundProvenanceAndManifest() {
        val root = findRepoRoot()
        val provenanceFile = File(root, "docs/provenance.csv")
        assertTrue("docs/provenance.csv must exist", provenanceFile.exists())

        val lines = provenanceFile.readLines().map { it.trim() }.filter { it.isNotEmpty() }
        assertTrue("provenance.csv must not be empty", lines.isNotEmpty())

        val header = lines.first()
        assertEquals(
            "Header must match exactly",
            "file,source_url,author,license,retrieved,archived_copy",
            header
        )

        val rows = lines.drop(1).map { line ->
            val cols = line.split(",")
            assertEquals("Row must have exactly 6 columns: $line", 6, cols.size)
            ProvenanceRow(
                file = cols[0].trim(),
                sourceUrl = cols[1].trim(),
                author = cols[2].trim(),
                license = cols[3].trim(),
                retrieved = cols[4].trim(),
                archivedCopy = cols[5].trim()
            )
        }

        assertTrue("Must have at least one provenance row", rows.isNotEmpty())

        val soundsDir = File(root, "app/src/main/assets/sounds")
        assertTrue("app/src/main/assets/sounds must exist", soundsDir.exists())

        // (a) Every audio file under assets/sounds/** appears in the CSV
        val audioFiles = soundsDir.walkTopDown()
            .filter { it.isFile && (it.extension == "ogg" || it.extension == "wav") }
            .map { it.relativeTo(soundsDir).path.replace('\\', '/') }
            .toSet()

        val csvAudioFiles = rows.map { it.file.removePrefix("sounds/").removePrefix("/") }.toSet()

        for (audioFile in audioFiles) {
            assertTrue(
                "Audio file '$audioFile' under assets/sounds/ must appear in provenance.csv",
                csvAudioFiles.contains(audioFile) || rows.any { it.file == "sounds/$audioFile" || it.file == audioFile }
            )
        }

        // (b) Every CSV row's license is in the allowed list
        for (row in rows) {
            assertTrue(
                "Row license '${row.license}' for file '${row.file}' must be in allowed list $allowedLicenses",
                allowedLicenses.contains(row.license)
            )
        }

        // (c) Every archived_copy path exists under docs/archive/
        val archiveDir = File(root, "docs/archive")
        assertTrue("docs/archive must exist", archiveDir.exists())
        for (row in rows) {
            val archivedFile = File(archiveDir, row.archivedCopy.removePrefix("docs/archive/").removePrefix("/"))
            assertTrue(
                "archived_copy '${row.archivedCopy}' (resolved to ${archivedFile.path}) must exist",
                archivedFile.exists()
            )
        }

        // (d) manifest.json parses into exactly four sets and every sample file it names exists
        val manifestFile = File(soundsDir, "manifest.json")
        assertTrue("manifest.json must exist under assets/sounds/", manifestFile.exists())

        val manifestJson = manifestFile.readText()
        val sets = SoundManifest.parse(manifestJson)
        assertEquals("manifest.json must parse into exactly 4 sets", 4, sets.size)

        val assetsDir = File(root, "app/src/main/assets")
        for (set in sets) {
            val allSamples = set.keys + listOf(set.space, set.enter, set.backspace, set.bell)
            for (sample in allSamples) {
                val sampleFile = File(assetsDir, sample.file)
                assertTrue(
                    "Sample file '${sample.file}' referenced in set '${set.id}' must exist under assets/ (looked at ${sampleFile.path})",
                    sampleFile.exists()
                )
            }
        }

        // (e) No file or row mentions "Brother" or "Subtersonic"
        for (line in lines) {
            assertFalse(
                "provenance.csv must not mention 'Brother': $line",
                line.contains("Brother", ignoreCase = true)
            )
            assertFalse(
                "provenance.csv must not mention 'Subtersonic': $line",
                line.contains("Subtersonic", ignoreCase = true)
            )
        }

        assertFalse(
            "manifest.json must not mention 'Brother'",
            manifestJson.contains("Brother", ignoreCase = true)
        )
        assertFalse(
            "manifest.json must not mention 'Subtersonic'",
            manifestJson.contains("Subtersonic", ignoreCase = true)
        )

        for (file in soundsDir.walkTopDown().filter { it.isFile }) {
            assertFalse(
                "File name must not mention 'Brother': ${file.name}",
                file.name.contains("Brother", ignoreCase = true)
            )
            assertFalse(
                "File name must not mention 'Subtersonic': ${file.name}",
                file.name.contains("Subtersonic", ignoreCase = true)
            )
        }
    }

    @Test
    fun everyAudioSampleIsValidMono44100OggVorbis() {
        val root = findRepoRoot()
        val soundsDir = File(root, "app/src/main/assets/sounds")
        assertTrue("app/src/main/assets/sounds must exist", soundsDir.exists())

        val oggFiles = soundsDir.walkTopDown()
            .filter { it.isFile && it.extension == "ogg" }
            .toList()

        assertTrue("Must find at least 30 Ogg files", oggFiles.size >= 30)

        for (file in oggFiles) {
            assertTrue("Sample ${file.name} must exist and be readable", file.exists() && file.canRead())
            assertTrue("Sample ${file.name} must be non-empty", file.length() > 0)

            val bytes = file.readBytes()
            assertTrue("Sample ${file.name} must be at least 64 bytes", bytes.size >= 64)

            // Magic: OggS
            val magic = String(bytes, 0, 4, Charsets.US_ASCII)
            assertEquals("File ${file.name} must start with 'OggS'", "OggS", magic)

            // Version
            assertEquals("Ogg version must be 0", 0, bytes[4].toInt())

            // Page segments
            val pageSegments = bytes[26].toInt() and 0xFF
            val packetOffset = 27 + pageSegments
            assertTrue("Packet offset within file bounds", packetOffset + 16 <= bytes.size)

            // Vorbis header packet type: 0x01
            assertEquals("File ${file.name} Vorbis packet type must be 1", 1, bytes[packetOffset].toInt())

            // Vorbis identifier: "vorbis"
            val vorbisMagic = String(bytes, packetOffset + 1, 6, Charsets.US_ASCII)
            assertEquals("File ${file.name} must contain 'vorbis' identifier", "vorbis", vorbisMagic)

            // Vorbis version: 0
            val vorbisVersion = ByteBuffer.wrap(bytes, packetOffset + 7, 4).order(ByteOrder.LITTLE_ENDIAN).int
            assertEquals("File ${file.name} Vorbis version must be 0", 0, vorbisVersion)

            // Audio channels: 1 (Mono)
            val channels = bytes[packetOffset + 11].toInt() and 0xFF
            assertEquals("File ${file.name} must be mono (1 channel)", 1, channels)

            // Audio sample rate: 44100
            val sampleRate = ByteBuffer.wrap(bytes, packetOffset + 12, 4).order(ByteOrder.LITTLE_ENDIAN).int
            assertEquals("File ${file.name} must have 44.1 kHz sample rate", 44100, sampleRate)
        }
    }

    @Test
    fun provenanceCsvHasNoDuplicateFileEntries() {
        val root = findRepoRoot()
        val provenanceFile = File(root, "docs/provenance.csv")
        val lines = provenanceFile.readLines().map { it.trim() }.filter { it.isNotEmpty() }
        val rows = lines.drop(1).map { it.split(",")[0].trim() }

        val seenFiles = mutableSetOf<String>()
        for (f in rows) {
            assertTrue("provenance.csv must not have duplicate entry for '$f'", seenFiles.add(f))
        }
    }

    @Test
    fun adversarialLicensesRejected() {
        val adversarialLicenses = listOf("GPL-3.0", "Proprietary", "CC-BY-NC", "CC-BY-NC-SA", "Apache-2.0", "")

        for (badLicense in adversarialLicenses) {
            assertFalse(
                "Adversarial license '$badLicense' must NOT be accepted in allowedLicenses",
                allowedLicenses.contains(badLicense)
            )
        }
    }

    @Test
    fun manifestSetsHaveValidIdsAndNonEmptyFields() {
        val root = findRepoRoot()
        val manifestFile = File(root, "app/src/main/assets/sounds/manifest.json")
        val manifestJson = manifestFile.readText()
        val sets = SoundManifest.parse(manifestJson)

        assertEquals("Must define exactly 4 sound sets", 4, sets.size)

        val expectedIds = setOf("classic", "antique", "soft", "keyboard")
        val actualIds = sets.map { it.id.lowercase() }.toSet()
        assertEquals("Sets must define classic, antique, soft, keyboard", expectedIds, actualIds)

        for (set in sets) {
            assertTrue("Set id must not be blank", set.id.isNotBlank())
            assertTrue("Set name must not be blank", set.name.isNotBlank())
            assertTrue("Set '${set.id}' keys must not be empty", set.keys.isNotEmpty())

            for ((idx, key) in set.keys.withIndex()) {
                assertTrue("Key $idx in '${set.id}' must have non-blank file", key.file.isNotBlank())
                assertTrue("Key $idx in '${set.id}' must have positive gain", key.gain > 0f)
            }

            assertTrue("Space in '${set.id}' must have non-blank file", set.space.file.isNotBlank())
            assertTrue("Space in '${set.id}' must have positive gain", set.space.gain > 0f)

            assertTrue("Enter in '${set.id}' must have non-blank file", set.enter.file.isNotBlank())
            assertTrue("Enter in '${set.id}' must have positive gain", set.enter.gain > 0f)

            assertTrue("Backspace in '${set.id}' must have non-blank file", set.backspace.file.isNotBlank())
            assertTrue("Backspace in '${set.id}' must have positive gain", set.backspace.gain > 0f)

            assertTrue("Bell in '${set.id}' must have non-blank file", set.bell.file.isNotBlank())
            assertTrue("Bell in '${set.id}' must have positive gain", set.bell.gain > 0f)
        }
    }

    @Test
    fun neitherBrotherNorSubtersonicMentionsExist() {
        val root = findRepoRoot()

        val filesToCheck = listOf(
            File(root, "docs/provenance.csv"),
            File(root, "app/src/main/assets/sounds/manifest.json"),
            File(root, "docs/sound-credits.md"),
            File(root, "CREDITS.md"),
            File(root, "tools/CutSamples.md")
        )

        for (file in filesToCheck) {
            assertTrue("File ${file.path} must exist", file.exists())
            val text = file.readText()
            assertFalse(
                "File ${file.name} must not contain 'Brother'",
                text.contains("Brother", ignoreCase = true)
            )
            assertFalse(
                "File ${file.name} must not contain 'Subtersonic'",
                text.contains("Subtersonic", ignoreCase = true)
            )
        }

        val soundsDir = File(root, "app/src/main/assets/sounds")
        for (file in soundsDir.walkTopDown().filter { it.isFile }) {
            assertFalse(
                "Sound file path must not contain 'Brother': ${file.name}",
                file.name.contains("Brother", ignoreCase = true)
            )
            assertFalse(
                "Sound file path must not contain 'Subtersonic': ${file.name}",
                file.name.contains("Subtersonic", ignoreCase = true)
            )
        }
    }

    private data class ProvenanceRow(
        val file: String,
        val sourceUrl: String,
        val author: String,
        val license: String,
        val retrieved: String,
        val archivedCopy: String
    )
}
