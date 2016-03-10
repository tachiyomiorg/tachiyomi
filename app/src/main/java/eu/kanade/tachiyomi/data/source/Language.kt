package eu.kanade.tachiyomi.data.source

class Language(val lang: String, val code: String)

val EN = Language("English", "EN")
val RU = Language("Russian", "RU")

fun getLanguages(): List<Language> = listOf(EN, RU)