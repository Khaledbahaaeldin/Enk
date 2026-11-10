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

data class AppLicenseInfo(
    val name: String,
    val notice: String,
    val courierPrimeNotice: String,
    val interNotice: String
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

fun getAppLicenseInfo(): AppLicenseInfo {
    return AppLicenseInfo(
        name = "Apache License, Version 2.0",
        notice = "Licensed under the Apache License, Version 2.0 (the \"License\"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0",
        courierPrimeNotice = "Courier Prime is licensed under the SIL Open Font License, Version 1.1. Copyright (c) 2013 Quote-Unquote Apps (http://quoteunquoteapps.com).",
        interNotice = "Inter is licensed under the SIL Open Font License, Version 1.1. Copyright (c) 2016-2024 The Inter Project Authors (https://github.com/rsms/inter)."
    )
}
