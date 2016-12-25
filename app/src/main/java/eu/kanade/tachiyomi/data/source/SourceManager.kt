package eu.kanade.tachiyomi.data.source

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
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

    private val sourcesMap = createSources()

    open fun get(sourceKey: Int): Source? {
        return sourcesMap[sourceKey]
    }

    fun getOnlineSources() = sourcesMap.values.filterIsInstance(OnlineSource::class.java)

    private fun createOnlineSourceList(): List<Source> = listOf(
            Batoto(1),
            Mangahere(2),
            Mangafox(3),
            Kissmanga(4),
            Readmanga(5),
            Mintmanga(6),
            Mangachan(7),
            Readmangatoday(8),
            Mangasee(9),
            WieManga(10)
    )

    private fun createSources(): Map<Int, Source> = hashMapOf<Int, Source>().apply {
        createOnlineSourceList().forEach { put(it.id, it) }

        val parsersDir = File(Environment.getExternalStorageDirectory().absolutePath +
                File.separator + context.getString(R.string.app_name), "parsers")

        if (parsersDir.exists() && context.hasPermission(READ_EXTERNAL_STORAGE)) {
            val yaml = Yaml()
            for (file in parsersDir.listFiles().filter { it.extension == "yml" }) {
                try {
                    val map = file.inputStream().use { yaml.loadAs(it, Map::class.java) }
                    YamlOnlineSource(map).let { put(it.id, it) }
                } catch (e: Exception) {
                    Timber.e("Error loading source from file. Bad format?")
                }
            }
        }

        createExtensionSources().forEach { put(it.id, it) }
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

            val ext = Extension(extName, pkgInfo, appInfo, version, sourceClass)


            val instance = loadExtension(ext, pkgManager) ?: continue
            sources.add(instance)
        }
        return sources
    }

    private fun loadExtension(ext: Extension, pkgManager: PackageManager): OnlineSource? {
        return try {
            val classLoader = PathClassLoader(ext.applicationInfo.sourceDir, null, context.classLoader)
            val resources = pkgManager.getResourcesForApplication(ext.applicationInfo)

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

    class Extension(val extensionName: String,
                    val packageInfo: PackageInfo,
                    val applicationInfo: ApplicationInfo,
                    val version: Int,
                    val sourceClass: String)

    companion object {
        private val FEATURE = "tachiyomi.extension"
        private val NAME = "tachiyomi.extension.name"
        private val VERSION = "tachiyomi.extension.version"
        private val SOURCE = "tachiyomi.extension.source"
    }

}
