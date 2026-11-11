package app.enk.about

import org.json.JSONArray
import org.json.JSONObject

data class Credit(
    val title: String,
    val author: String,
    val source: String,
    val license: String
)

data class LibraryCredit(
    val name: String,
    val version: String,
    val license: String,
    val sourceUrl: String
)

/** One licence text shown from an asset: the app's Apache-2.0 text and the two font licences. */
data class LicenseDoc(val title: String, val subtitle: String, val assetPath: String)

object LicenseDocs {
    val App = LicenseDoc("Apache License, Version 2.0", "Enk", "licenses/Apache-2.0.txt")
    val CourierPrime = LicenseDoc("Courier Prime", "SIL Open Font License 1.1", "licenses/OFL-CourierPrime.txt")
    val Inter = LicenseDoc("Inter", "SIL Open Font License 1.1", "licenses/OFL-Inter.txt")
}

/** Recordings by one author under one licence from one source, and where Enk uses them. */
data class SoundCredit(
    val author: String,
    val license: String,
    val source: String,
    val usedIn: String
)

fun loadCredits(json: String): List<Credit> {
    if (json.isBlank()) return emptyList()
    return try {
        val array = JSONArray(json)
        val list = mutableListOf<Credit>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val title = obj.optString("title", "").trim()
            val author = obj.optString("author", "").trim()
            val source = obj.optString("source", "").trim()
            val license = obj.optString("license", "").trim()
            if (title.isNotEmpty()) {
                list.add(Credit(title = title, author = author, source = source, license = license))
            }
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}

fun loadLibraries(csv: String): List<LibraryCredit> {
    if (csv.isBlank()) return emptyList()
    val lines = csv.lines().filter { it.isNotBlank() }
    if (lines.size <= 1) return emptyList()

    val header = lines.first().split(",")
    val nameIdx = header.indexOf("name")
    val verIdx = header.indexOf("version")
    val licIdx = header.indexOf("license")
    val urlIdx = header.indexOf("source_url")

    val list = mutableListOf<LibraryCredit>()
    for (line in lines.drop(1)) {
        val cols = line.split(",")
        if (cols.size >= 4) {
            val name = cols.getOrElse(nameIdx) { "" }.trim()
            val version = cols.getOrElse(verIdx) { "" }.trim()
            val license = cols.getOrElse(licIdx) { "" }.trim()
            val url = cols.getOrElse(urlIdx) { "" }.trim()
            if (name.isNotEmpty()) {
                list.add(LibraryCredit(name = name, version = version, license = license, sourceUrl = url))
            }
        }
    }
    return list
}

/**
 * Collapses the per-file provenance rows into one row per author, source and licence. A row's file is
 * `sounds/<set>/<name>.ogg`; its use is listed as `Classic: keys, space, backspace`.
 */
fun groupSoundCredits(credits: List<Credit>): List<SoundCredit> {
    data class Key(val author: String, val license: String, val source: String)

    val order = LinkedHashMap<Key, LinkedHashMap<String, LinkedHashSet<String>>>()
    for (credit in credits) {
        val parts = credit.title.split("/")
        val soundSet = if (parts.size >= 3 && parts[0] == "sounds") parts[1] else ""
        val fileName = parts.last().substringBeforeLast('.')
        val kind = if (Regex("key_[0-9]+").matches(fileName)) "keys" else fileName
        order.getOrPut(Key(credit.author, credit.license, credit.source)) { LinkedHashMap() }
            .getOrPut(soundSet) { LinkedHashSet() }
            .add(kind)
    }
    return order.map { (key, sets) ->
        val usedIn = sets.entries.joinToString("; ") { (soundSet, kinds) ->
            val name = soundSet.replaceFirstChar { it.uppercase() }
            if (name.isEmpty()) kinds.joinToString(", ") else "$name: ${kinds.joinToString(", ")}"
        }
        SoundCredit(key.author, key.license, key.source, usedIn)
    }
}
