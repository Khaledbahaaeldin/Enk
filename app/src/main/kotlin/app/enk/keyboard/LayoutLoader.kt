package app.enk.keyboard

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class LayoutException(message: String, cause: Throwable? = null) : Exception(message, cause)

object LayoutLoader {
    const val EN_ASSET = "keyboard/en.json"

    fun parse(json: String): KeyboardLayout {
        val root = try {
            JSONObject(json)
        } catch (e: JSONException) {
            throw LayoutException("root: invalid JSON", e)
        }

        if (!root.has("id")) {
            throw LayoutException("id: missing")
        }
        val idVal = root.opt("id")
        if (idVal !is String || idVal.isBlank()) {
            throw LayoutException("id: must be a non-blank string")
        }

        if (!root.has("name")) {
            throw LayoutException("name: missing")
        }
        val nameVal = root.opt("name")
        if (nameVal !is String || nameVal.isBlank()) {
            throw LayoutException("name: must be a non-blank string")
        }

        val popupsMap = mutableMapOf<String, List<String>>()
        if (root.has("popups")) {
            val popupsObj = root.optJSONObject("popups")
                ?: throw LayoutException("popups: must be an object")
            val keys = popupsObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val path = "popups.$key"
                if (key.isEmpty()) {
                    throw LayoutException("$path: key must not be empty")
                }
                val arr = popupsObj.optJSONArray(key)
                    ?: throw LayoutException("$path: must be an array")
                if (arr.length() !in 1..10) {
                    throw LayoutException("$path: must have 1 to 10 entries")
                }
                val alts = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val itemPath = "$path[$i]"
                    val item = arr.opt(i)
                    if (item !is String || item.isEmpty() || item.length > 8) {
                        throw LayoutException("$itemPath: must be non-empty string of at most 8 chars")
                    }
                    alts.add(item)
                }
                popupsMap[key] = alts
            }
        }

        if (!root.has("pages")) {
            throw LayoutException("pages: missing")
        }
        val pagesObj = root.optJSONObject("pages")
            ?: throw LayoutException("pages: must be an object")

        val expectedPages = setOf("letters", "numbers", "symbols")
        val pageKeys = mutableSetOf<String>()
        val pageKeyIter = pagesObj.keys()
        while (pageKeyIter.hasNext()) {
            pageKeys.add(pageKeyIter.next())
        }

        if (pageKeys != expectedPages) {
            val missing = expectedPages - pageKeys
            val extra = pageKeys - expectedPages
            if (missing.isNotEmpty()) {
                throw LayoutException("pages: missing page '${missing.first()}'")
            }
            if (extra.isNotEmpty()) {
                throw LayoutException("pages.${extra.first()}: unexpected page")
            }
        }

        val parsedPages = mutableMapOf<PageId, List<KeyRow>>()

        for (pageName in listOf("letters", "numbers", "symbols")) {
            val pageId = PageId.fromJson(pageName) ?: error("Unknown page $pageName")
            val pageArr = pagesObj.optJSONArray(pageName)
                ?: throw LayoutException("pages.$pageName: must be an array")
            if (pageArr.length() !in 1..6) {
                throw LayoutException("pages.$pageName: must have 1 to 6 rows (got ${pageArr.length()})")
            }

            val rows = mutableListOf<KeyRow>()
            for (r in 0 until pageArr.length()) {
                val rowPath = "pages.$pageName[$r]"
                val rowArr = pageArr.optJSONArray(r)
                    ?: throw LayoutException("$rowPath: must be an array")
                if (rowArr.length() !in 1..16) {
                    throw LayoutException("$rowPath: row must have 1 to 16 entries (got ${rowArr.length()})")
                }

                val keySpecs = mutableListOf<KeySpec>()
                for (c in 0 until rowArr.length()) {
                    val entryPath = "$rowPath[$c]"
                    val entry = rowArr.opt(c)
                    val spec = when (entry) {
                        is String -> {
                            if (entry.isEmpty() || entry.length > 8) {
                                throw LayoutException("$entryPath: string entry must be non-empty and at most 8 chars")
                            }
                            CharKey(
                                label = entry,
                                popup = popupsMap[entry] ?: emptyList(),
                                width = 1f
                            )
                        }
                        is JSONObject -> {
                            parseKeyObject(entry, entryPath, popupsMap)
                        }
                        else -> {
                            throw LayoutException("$entryPath: entry must be a string or object")
                        }
                    }
                    keySpecs.add(spec)
                }
                rows.add(KeyRow(keySpecs))
            }
            parsedPages[pageId] = rows
        }

        return KeyboardLayout(
            id = idVal,
            name = nameVal,
            pages = parsedPages
        )
    }

    private fun parseKeyObject(
        obj: JSONObject,
        path: String,
        popupsMap: Map<String, List<String>>
    ): KeySpec {
        if (!obj.has("type")) {
            throw LayoutException("$path.type: missing")
        }
        val type = obj.optString("type")
        val width = if (obj.has("width")) {
            val w = obj.optDouble("width", Double.NaN)
            if (w.isNaN() || w < 0.25 || w > 8.0) {
                throw LayoutException("$path.width: width must be in 0.25..8.0 (got $w)")
            }
            w.toFloat()
        } else {
            null
        }

        return when (type) {
            "char" -> {
                if (!obj.has("label")) {
                    throw LayoutException("$path.label: missing")
                }
                val label = obj.optString("label")
                if (label.isEmpty() || label.length > 8) {
                    throw LayoutException("$path.label: label must be non-empty and at most 8 chars")
                }
                CharKey(
                    label = label,
                    popup = popupsMap[label] ?: emptyList(),
                    width = width ?: 1f
                )
            }
            "shift" -> ShiftKey(width = width ?: 1.5f)
            "backspace" -> BackspaceKey(width = width ?: 1.5f)
            "space" -> SpaceKey(width = width ?: 4f)
            "enter" -> EnterKey(width = width ?: 2f)
            "globe" -> GlobeKey(width = width ?: 1f)
            "spacer" -> SpacerKey(width = width ?: 0.5f)
            "page" -> {
                if (!obj.has("target")) {
                    throw LayoutException("$path.target: missing")
                }
                val targetStr = obj.optString("target")
                val target = PageId.fromJson(targetStr)
                    ?: throw LayoutException("$path.target: invalid target '$targetStr'")
                if (!obj.has("label")) {
                    throw LayoutException("$path.label: missing")
                }
                val label = obj.optString("label")
                if (label.isBlank() || label.length > 8) {
                    throw LayoutException("$path.label: page label must be non-blank and at most 8 chars")
                }
                PageKey(target = target, label = label, width = width ?: 1.5f)
            }
            else -> {
                throw LayoutException("$path.type: unknown type '$type'")
            }
        }
    }

    fun load(context: Context, assetPath: String = EN_ASSET): KeyboardLayout {
        val text = try {
            context.assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: IOException) {
            throw LayoutException("cannot read $assetPath", e)
        }
        return parse(text)
    }
}
