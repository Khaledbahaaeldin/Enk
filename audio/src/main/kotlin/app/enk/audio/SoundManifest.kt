package app.enk.audio

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener

object SoundManifest {

    fun parse(json: String): List<SoundSet> {
        val root = try {
            JSONTokener(json).nextValue()
        } catch (e: JSONException) {
            throw IllegalArgumentException("Malformed JSON: ${e.message}", e)
        }

        val array: JSONArray = when (root) {
            is JSONArray -> root
            is JSONObject -> {
                if (!root.has("sets")) {
                    throw IllegalArgumentException("Root object missing 'sets' property")
                }
                root.optJSONArray("sets")
                    ?: throw IllegalArgumentException("'sets' property must be a JSON array")
            }
            else -> throw IllegalArgumentException("Root must be a JSON array or object containing 'sets'")
        }

        val seenIds = mutableSetOf<String>()
        val result = mutableListOf<SoundSet>()

        for (i in 0 until array.length()) {
            val setObj = array.optJSONObject(i)
                ?: throw IllegalArgumentException("Each element in manifest must be a JSON object")

            val id = setObj.optString("id").trim()
            if (id.isEmpty()) {
                throw IllegalArgumentException("Sound set missing or empty 'id'")
            }
            if (!seenIds.add(id)) {
                throw IllegalArgumentException("Duplicate sound set id: $id")
            }

            val name = setObj.optString("name").trim()
            if (name.isEmpty()) {
                throw IllegalArgumentException("Sound set '$id' missing or empty 'name'")
            }

            val keysArray = setObj.optJSONArray("keys")
                ?: throw IllegalArgumentException("Sound set '$id' missing 'keys' array")
            if (keysArray.length() == 0) {
                throw IllegalArgumentException("Sound set '$id' 'keys' array must not be empty")
            }

            val keys = mutableListOf<Sample>()
            for (k in 0 until keysArray.length()) {
                keys.add(parseSample(keysArray.get(k), "key[$k] in $id"))
            }

            val space = parseSampleFromObject(setObj, "space", id)
            val enter = parseSampleFromObject(setObj, "enter", id)
            val backspace = parseSampleFromObject(setObj, "backspace", id)
            val bell = parseSampleFromObject(setObj, "bell", id)

            result.add(
                SoundSet(
                    id = id,
                    name = name,
                    keys = keys,
                    space = space,
                    enter = enter,
                    backspace = backspace,
                    bell = bell
                )
            )
        }

        return result
    }

    private fun parseSampleFromObject(parent: JSONObject, key: String, setId: String): Sample {
        if (!parent.has(key)) {
            throw IllegalArgumentException("Sound set '$setId' missing required sample '$key'")
        }
        return parseSample(parent.get(key), "$key in $setId")
    }

    private fun parseSample(raw: Any?, contextDesc: String): Sample {
        return when (raw) {
            is String -> {
                val file = raw.trim()
                if (file.isEmpty()) {
                    throw IllegalArgumentException("Blank file string for $contextDesc")
                }
                Sample(file = file, gain = 1f)
            }
            is JSONObject -> {
                val file = raw.optString("file").trim()
                if (file.isEmpty()) {
                    throw IllegalArgumentException("Missing or blank 'file' for $contextDesc")
                }
                val gain = if (raw.has("gain")) {
                    val g = raw.optDouble("gain")
                    if (g.isNaN() || g.isInfinite() || g < 0.0) {
                        throw IllegalArgumentException("Invalid gain value for $contextDesc: $g")
                    }
                    g.toFloat()
                } else {
                    1f
                }
                Sample(file = file, gain = gain)
            }
            else -> throw IllegalArgumentException("Invalid sample format for $contextDesc")
        }
    }
}
