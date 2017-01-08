package eu.kanade.tachiyomi.data.source

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import dalvik.system.PathClassLoader
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.source.online.OnlineSource
import eu.kanade.tachiyomi.data.source.online.YamlOnlineSource
import eu.kanade.tachiyomi.data.source.online.english.*
import eu.kanade.tachiyomi.data.source.online.german.WieManga
import eu.kanade.tachiyomi.data.source.online.russian.Mangachan
import eu.kanade.tachiyomi.data.source.online.russian.Mintmanga
import eu.kanade.tachiyomi.data.source.online.russian.Readmanga
import eu.kanade.tachiyomi.util.hasPermission
import org.yaml.snakeyaml.Yaml
import timber.log.Timber
import java.io.File

open class SourceManager(private val context: Context) {

    private val sourcesMap = mutableMapOf<Long, Source>()

    init {
        createSources()
    }

    open fun get(sourceKey: Long): Source? {
        return sourcesMap[sourceKey]
    }

    fun getOnlineSources() = sourcesMap.values.filterIsInstance<OnlineSource>()

    fun getCatalogueSources() = sourcesMap.values.filterIsInstance<CatalogueSource>()

    private fun createSources() {
        createExtensionSources().forEach { registerSource(it) }
        createYamlSources().forEach { registerSource(it) }
        createInternalSources().forEach { registerSource(it) }
    }

    private fun registerSource(source: Source, overwrite: Boolean = false) {
        if (overwrite || !sourcesMap.containsKey(source.id)) {
            sourcesMap.put(source.id, source)
        }
    }

    private fun createInternalSources(): List<Source> = listOf(
            Batoto(),
            Mangahere(),
            Mangafox(),
            Kissmanga(),
            Readmanga(),
            Mintmanga(),
            Mangachan(),
            Readmangatoday(),
            Mangasee(),
            WieManga()
    )

    private fun createYamlSources(): List<Source> {
        val sources = mutableListOf<Source>()

        val parsersDir = File(Environment.getExternalStorageDirectory().absolutePath +
                File.separator + context.getString(R.string.app_name), "parsers")

        if (parsersDir.exists() && context.hasPermission(READ_EXTERNAL_STORAGE)) {
            val yaml = Yaml()
            for (file in parsersDir.listFiles().filter { it.extension == "yml" }) {
                try {
                    val map = file.inputStream().use { yaml.loadAs(it, Map::class.java) }
                    sources.add(YamlOnlineSource(map))
                } catch (e: Exception) {
                    Timber.e("Error loading source from file. Bad format?")
                }
            }
        }
        return sources
    }

    private fun createExtensionSources(): List<OnlineSource> {
        val pkgManager = context.packageManager
        val flags = PackageManager.GET_CONFIGURATIONS or PackageManager.GET_SIGNATURES
        val installedPkgs = pkgManager.getInstalledPackages(flags)
        val extPkgs = installedPkgs.filter { it.reqFeatures.orEmpty().any { it.name == FEATURE } }

        val sources = mutableListOf<OnlineSource>()
        for (pkgInfo in extPkgs) {
            val appInfo = pkgManager.getApplicationInfo(pkgInfo.packageName,
                    PackageManager.GET_META_DATA) ?: continue


            val data = appInfo.metaData
            val extName = data.getString(NAME)
            val version = data.getInt(VERSION)
            val sourceClass = extendClassName(data.getString(SOURCE), pkgInfo.packageName)

            val ext = Extension(extName, appInfo, version, sourceClass)
            if (!validateExtension(ext)) {
                continue
            }

            val instance = loadExtension(ext, pkgManager)
            if (instance == null) {
                Timber.e("Extension error: failed to instance $extName")
                continue
            }
            sources.add(instance)
        }
        return sources
    }

    private fun validateExtension(ext: Extension): Boolean {
        if (ext.version < LIB_VERSION_MIN || ext.version > LIB_VERSION_MAX) {
            Timber.e("Extension error: ${ext.name} has version ${ext.version}, while only versions "
                    + "$LIB_VERSION_MIN to $LIB_VERSION_MAX are allowed")
            return false
        }
        return true
    }

    private fun loadExtension(ext: Extension, pkgManager: PackageManager): OnlineSource? {
        return try {
            val classLoader = PathClassLoader(ext.appInfo.sourceDir, null, context.classLoader)
            val resources = pkgManager.getResourcesForApplication(ext.appInfo)

            Class.forName(ext.sourceClass, false, classLoader).newInstance() as? OnlineSource
        } catch (e: Exception) {
            null
        } catch (e: LinkageError) {
            null
        }
    }

    private fun extendClassName(className: String, packageName: String): String {
        return if (className.startsWith(".")) {
            packageName + className
        } else {
            className
        }
    }

    class Extension(val name: String,
                    val appInfo: ApplicationInfo,
                    val version: Int,
                    val sourceClass: String)

    private companion object {
        const val FEATURE = "tachiyomi.extension"
        const val NAME = "tachiyomi.extension.name"
        const val VERSION = "tachiyomi.extension.version"
        const val SOURCE = "tachiyomi.extension.source"
        const val LIB_VERSION_MIN = 1
        const val LIB_VERSION_MAX = 1
    }

}
