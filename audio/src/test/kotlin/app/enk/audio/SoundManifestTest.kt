package app.enk.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SoundManifestTest {

    private val sampleFourSetsJson = """
    [
      {
        "id": "Classic",
        "name": "Classic Royal",
        "keys": ["sounds/classic/key1.ogg", "sounds/classic/key2.ogg", "sounds/classic/key3.ogg"],
        "space": "sounds/classic/space.ogg",
        "enter": "sounds/classic/enter.ogg",
        "backspace": "sounds/classic/backspace.ogg",
        "bell": "sounds/classic/bell.ogg"
      },
      {
        "id": "Antique",
        "name": "Antique Underwood",
        "keys": [
          {"file": "sounds/antique/key1.ogg", "gain": 0.9},
          {"file": "sounds/antique/key2.ogg", "gain": 1.1}
        ],
        "space": {"file": "sounds/antique/space.ogg", "gain": 0.8},
        "enter": "sounds/antique/enter.ogg",
        "backspace": "sounds/antique/backspace.ogg",
        "bell": {"file": "sounds/antique/bell.ogg", "gain": 1.0}
      },
      {
        "id": "Soft",
        "name": "Soft Touch",
        "keys": ["sounds/soft/key1.ogg"],
        "space": "sounds/soft/space.ogg",
        "enter": "sounds/soft/enter.ogg",
        "backspace": "sounds/soft/backspace.ogg",
        "bell": "sounds/soft/bell.ogg"
      },
      {
        "id": "Keyboard",
        "name": "Mechanical Keyboard",
        "keys": ["sounds/keyboard/key1.ogg", "sounds/keyboard/key2.ogg"],
        "space": "sounds/keyboard/space.ogg",
        "enter": "sounds/keyboard/enter.ogg",
        "backspace": "sounds/keyboard/backspace.ogg",
        "bell": "sounds/keyboard/bell.ogg"
      }
    ]
    """.trimIndent()

    @Test
    fun parsesFourSets() {
        val sets = SoundManifest.parse(sampleFourSetsJson)
        assertEquals(4, sets.size)

        val classic = sets[0]
        assertEquals("Classic", classic.id)
        assertEquals("Classic Royal", classic.name)
        assertEquals(3, classic.keys.size)
        assertEquals("sounds/classic/key1.ogg", classic.keys[0].file)
        assertEquals(1f, classic.keys[0].gain, 0.001f)
        assertEquals("sounds/classic/space.ogg", classic.space.file)
        assertEquals("sounds/classic/enter.ogg", classic.enter.file)
        assertEquals("sounds/classic/backspace.ogg", classic.backspace.file)
        assertEquals("sounds/classic/bell.ogg", classic.bell.file)

        val antique = sets[1]
        assertEquals("Antique", antique.id)
        assertEquals("Antique Underwood", antique.name)
        assertEquals(2, antique.keys.size)
        assertEquals("sounds/antique/key1.ogg", antique.keys[0].file)
        assertEquals(0.9f, antique.keys[0].gain, 0.001f)
        assertEquals(0.8f, antique.space.gain, 0.001f)

        assertEquals("Soft", sets[2].id)
        assertEquals("Keyboard", sets[3].id)
    }

    @Test
    fun parsesRootObjectWithSetsProperty() {
        val wrappedJson = """
        {
          "sets": [
            {
              "id": "Classic",
              "name": "Classic Royal",
              "keys": ["sounds/classic/key1.ogg"],
              "space": "sounds/classic/space.ogg",
              "enter": "sounds/classic/enter.ogg",
              "backspace": "sounds/classic/backspace.ogg",
              "bell": "sounds/classic/bell.ogg"
            }
          ]
        }
        """.trimIndent()

        val sets = SoundManifest.parse(wrappedJson)
        assertEquals(1, sets.size)
        assertEquals("Classic", sets[0].id)
    }

    @Test
    fun parseRejectsMissingBell() {
        val json = """
        [
          {
            "id": "Classic",
            "name": "Classic Royal",
            "keys": ["sounds/classic/key1.ogg"],
            "space": "sounds/classic/space.ogg",
            "enter": "sounds/classic/enter.ogg",
            "backspace": "sounds/classic/backspace.ogg"
          }
        ]
        """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            SoundManifest.parse(json)
        }
    }

    @Test
    fun parseRejectsMissingSpaceEnterOrBackspace() {
        val missingSpace = """
        [
          {
            "id": "Classic",
            "name": "Classic Royal",
            "keys": ["sounds/classic/key1.ogg"],
            "enter": "sounds/classic/enter.ogg",
            "backspace": "sounds/classic/backspace.ogg",
            "bell": "sounds/classic/bell.ogg"
          }
        ]
        """.trimIndent()
        assertThrows(IllegalArgumentException::class.java) {
            SoundManifest.parse(missingSpace)
        }

        val missingEnter = """
        [
          {
            "id": "Classic",
            "name": "Classic Royal",
            "keys": ["sounds/classic/key1.ogg"],
            "space": "sounds/classic/space.ogg",
            "backspace": "sounds/classic/backspace.ogg",
            "bell": "sounds/classic/bell.ogg"
          }
        ]
        """.trimIndent()
        assertThrows(IllegalArgumentException::class.java) {
            SoundManifest.parse(missingEnter)
        }

        val missingBackspace = """
        [
          {
            "id": "Classic",
            "name": "Classic Royal",
            "keys": ["sounds/classic/key1.ogg"],
            "space": "sounds/classic/space.ogg",
            "enter": "sounds/classic/enter.ogg",
            "bell": "sounds/classic/bell.ogg"
          }
        ]
        """.trimIndent()
        assertThrows(IllegalArgumentException::class.java) {
            SoundManifest.parse(missingBackspace)
        }
    }

    @Test
    fun parseRejectsEmptyKeys() {
        val json = """
        [
          {
            "id": "Classic",
            "name": "Classic Royal",
            "keys": [],
            "space": "sounds/classic/space.ogg",
            "enter": "sounds/classic/enter.ogg",
            "backspace": "sounds/classic/backspace.ogg",
            "bell": "sounds/classic/bell.ogg"
          }
        ]
        """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            SoundManifest.parse(json)
        }
    }

    @Test
    fun parseRejectsDuplicateSetIds() {
        val json = """
        [
          {
            "id": "Classic",
            "name": "Classic 1",
            "keys": ["k1.ogg"],
            "space": "s.ogg",
            "enter": "e.ogg",
            "backspace": "b.ogg",
            "bell": "bell.ogg"
          },
          {
            "id": "Classic",
            "name": "Classic 2",
            "keys": ["k2.ogg"],
            "space": "s.ogg",
            "enter": "e.ogg",
            "backspace": "b.ogg",
            "bell": "bell.ogg"
          }
        ]
        """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            SoundManifest.parse(json)
        }
    }

    @Test
    fun parseRejectsMalformedJsonOrBlankFile() {
        assertThrows(IllegalArgumentException::class.java) {
            SoundManifest.parse("{ not valid json }")
        }

        val blankFileJson = """
        [
          {
            "id": "Classic",
            "name": "Classic",
            "keys": ["   "],
            "space": "s.ogg",
            "enter": "e.ogg",
            "backspace": "b.ogg",
            "bell": "bell.ogg"
          }
        ]
        """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            SoundManifest.parse(blankFileJson)
        }
    }

    @Test
    fun parseRejectsNegativeOrInvalidGain() {
        val negativeGain = """
        [
          {
            "id": "Classic",
            "name": "Classic",
            "keys": [{"file": "k.ogg", "gain": -0.5}],
            "space": "s.ogg",
            "enter": "e.ogg",
            "backspace": "b.ogg",
            "bell": "bell.ogg"
          }
        ]
        """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            SoundManifest.parse(negativeGain)
        }
    }

    @Test
    fun parseIgnoresExtraUnknownJsonProperties() {
        val jsonWithExtras = """
        {
          "version": "1.0",
          "author": "Enk Team",
          "sets": [
            {
              "id": "Classic",
              "name": "Classic Royal",
              "extra_info": "ignored",
              "keys": [
                {"file": "sounds/classic/key1.ogg", "gain": 1.0, "pitch": 440}
              ],
              "space": {"file": "sounds/classic/space.ogg", "unknown": true},
              "enter": "sounds/classic/enter.ogg",
              "backspace": "sounds/classic/backspace.ogg",
              "bell": "sounds/classic/bell.ogg"
            }
          ]
        }
        """.trimIndent()

        val sets = SoundManifest.parse(jsonWithExtras)
        assertEquals(1, sets.size)
        assertEquals("Classic", sets[0].id)
        assertEquals("sounds/classic/key1.ogg", sets[0].keys[0].file)
        assertEquals(1.0f, sets[0].keys[0].gain, 0.001f)
    }

    @Test
    fun parseAcceptsIntegerGain() {
        val json = """
        [
          {
            "id": "Classic",
            "name": "Classic",
            "keys": [
              {"file": "k1.ogg", "gain": 1},
              {"file": "k2.ogg", "gain": 2}
            ],
            "space": "s.ogg",
            "enter": "e.ogg",
            "backspace": "b.ogg",
            "bell": "bell.ogg"
          }
        ]
        """.trimIndent()

        val sets = SoundManifest.parse(json)
        assertEquals(1, sets.size)
        assertEquals(1.0f, sets[0].keys[0].gain, 0.001f)
        assertEquals(2.0f, sets[0].keys[1].gain, 0.001f)
    }

    @Test
    fun parseHandlesWhitespaceInIdNameAndFile() {
        val json = """
        [
          {
            "id": "  Classic  ",
            "name": "  Classic Royal  ",
            "keys": ["  sounds/classic/key1.ogg  "],
            "space": "  sounds/classic/space.ogg  ",
            "enter": "  sounds/classic/enter.ogg  ",
            "backspace": "  sounds/classic/backspace.ogg  ",
            "bell": "  sounds/classic/bell.ogg  "
          }
        ]
        """.trimIndent()

        val sets = SoundManifest.parse(json)
        assertEquals(1, sets.size)
        assertEquals("Classic", sets[0].id)
        assertEquals("Classic Royal", sets[0].name)
        assertEquals("sounds/classic/key1.ogg", sets[0].keys[0].file)
        assertEquals("sounds/classic/space.ogg", sets[0].space.file)
        assertEquals("sounds/classic/enter.ogg", sets[0].enter.file)
        assertEquals("sounds/classic/backspace.ogg", sets[0].backspace.file)
        assertEquals("sounds/classic/bell.ogg", sets[0].bell.file)
    }

    @Test
    fun parseEmptyArrayReturnsEmptyList() {
        val sets = SoundManifest.parse("[]")
        assertEquals(0, sets.size)
    }

    @Test
    fun parseRejectsInvalidRootFormats() {
        // Empty string
        assertThrows(IllegalArgumentException::class.java) {
            SoundManifest.parse("")
        }

        // Whitespace only
        assertThrows(IllegalArgumentException::class.java) {
            SoundManifest.parse("    ")
        }

        // "null"
        assertThrows(IllegalArgumentException::class.java) {
            SoundManifest.parse("null")
        }

        // Empty object without sets
        assertThrows(IllegalArgumentException::class.java) {
            SoundManifest.parse("{}")
        }

        // Root is a scalar number
        assertThrows(IllegalArgumentException::class.java) {
            SoundManifest.parse("123")
        }
    }
}
