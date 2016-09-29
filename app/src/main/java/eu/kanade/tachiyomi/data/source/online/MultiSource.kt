package eu.kanade.tachiyomi.data.source.online

import android.content.Context
import eu.kanade.tachiyomi.data.source.*

abstract class MultiSource(context: Context) : ParsedOnlineSource(context) {

    protected fun lang(langs: List<Language>) : Language {
        val codes : Set<String> = preferences.enabledLanguages().get() as Set<String>
        if (codes.contains("EN") && langs.contains(EN)) {
            return EN
        } else if (codes.contains("ES") && langs.contains(ES)) {
            return ES
        } else if (codes.contains("DE") && langs.contains(DE)) {
            return DE
        } else if (codes.contains("IT") && langs.contains(IT)) {
            return IT
        } else if (codes.contains("RU") && langs.contains(RU)) {
            return RU
        } else if (codes.contains("BR") && langs.contains(BR)) {
            return BR
        /*} else if (codes.contains("JA") && langs.contains(JA)) {
            return JA*/
        }
        return langs.first()
    }

    abstract fun baseURL(language: Language, url: String) : String

}
