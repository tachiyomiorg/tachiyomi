package eu.kanade.tachiyomi.extension.api

import android.content.Context
import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.int
import com.github.salomonbrys.kotson.string
import com.google.gson.JsonArray
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.LoadResult
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.injectLazy

internal class ExtensionGithubApi {

    private val preferences: PreferencesHelper by injectLazy()

    suspend fun findExtensions(): List<Extension.Available> {
        val service: ExtensionGithubService = ExtensionGithubService.create()

        return withContext(Dispatchers.IO) {
            val response = service.getRepo()
            parseResponse(response)
        }
    }

    suspend fun checkForUpdates(context: Context): List<Extension.Installed> {
        val extensions = findExtensions()

        preferences.lastExtCheck().set(Date().time)

        val installedExtensions = ExtensionLoader.loadExtensions(context)
            .filterIsInstance<LoadResult.Success>()
            .map { it.extension }

        val extensionsWithUpdate = mutableListOf<Extension.Installed>()
        for (installedExt in installedExtensions) {
            val pkgName = installedExt.pkgName
            val availableExt = extensions.find { it.pkgName == pkgName } ?: continue

            val hasUpdate = availableExt.versionCode > installedExt.versionCode
            if (hasUpdate) {
                extensionsWithUpdate.add(installedExt)
            }
        }

        return extensionsWithUpdate
    }

    private fun parseResponse(json: JsonArray): List<Extension.Available> {
        return json
            .filter { element ->
                val versionName = element["version"].string
                val libVersion = versionName.substringBeforeLast('.').toDouble()
                libVersion >= ExtensionLoader.LIB_VERSION_MIN && libVersion <= ExtensionLoader.LIB_VERSION_MAX
            }
            .map { element ->
                val name = element["name"].string.substringAfter("Tachiyomi: ")
                val pkgName = element["pkg"].string
                val apkName = element["apk"].string
                val versionName = element["version"].string
                val versionCode = element["code"].int
                val lang = element["lang"].string
                val nsfw = element["nsfw"].int == 1
                val icon = "$REPO_URL_PREFIX/icon/${apkName.replace(".apk", ".png")}"

                Extension.Available(name, pkgName, versionName, versionCode, lang, nsfw, apkName, icon)
            }
    }

    fun getApkUrl(extension: Extension.Available): String {
        return "$REPO_URL_PREFIX/apk/${extension.apkName}"
    }

    companion object {
        const val BASE_URL = "https://raw.githubusercontent.com/"
        const val REPO_URL_PREFIX = "${BASE_URL}inorichi/tachiyomi-extensions/repo/"
    }
}
