package eu.kanade.tachiyomi.extension.model

import eu.kanade.tachiyomi.source.Source

sealed class Extension {

    abstract val name: String
    abstract val pkgName: String
    abstract val versionName: String
    abstract val versionCode: Int
    abstract val lang: String?

    data class Installed(override val name: String,
                         override val pkgName: String,
                         override val versionName: String,
                         override val versionCode: Int,
                         val sources: List<Source>,
                         override val lang: String,
                         val hasUpdate: Boolean = false) : Extension()

    data class Available(override val name: String,
                         override val pkgName: String,
                         override val versionName: String,
                         override val versionCode: Int,
                         override val lang: String,
                         val apkName: String,
                         val iconUrl: String) : Extension()

    data class Untrusted(override val name: String,
                         override val pkgName: String,
                         override val versionName: String,
                         override val versionCode: Int,
                         val signatureHash: String,
                         override val lang: String? = null) : Extension()

}
