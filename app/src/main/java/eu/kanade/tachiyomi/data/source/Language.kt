package eu.kanade.tachiyomi.data.source

class Language(val code: String, val lang: String)

val DE = Language("DE", "Deutsche")
val EN = Language("EN", "English")
val RU = Language("RU", "русский")
val IT = Language("IT", "Italiano")
val ES = Language("ES", "Español")
val JP = Language("JP", "日本語") //TODO: RAWS

fun getLanguages() = listOf(DE, EN, RU, IT, ES, JP)