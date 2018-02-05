package eu.kanade.tachiyomi.source

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import dalvik.system.PathClassLoader
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.LoginSource
import eu.kanade.tachiyomi.source.online.YamlHttpSource
import eu.kanade.tachiyomi.source.online.english.*
import eu.kanade.tachiyomi.source.online.german.WieManga
import eu.kanade.tachiyomi.source.online.russian.Mangachan
import eu.kanade.tachiyomi.source.online.russian.Mintmanga
import eu.kanade.tachiyomi.source.online.russian.Readmanga
import eu.kanade.tachiyomi.util.hasPermission
import org.yaml.snakeyaml.Yaml
import timber.log.Timber
import java.io.File

open class SourceManager(private val context: Context) {

    private val sourcesMap = mutableMapOf<Long, Source>()

    private var prefs = PreferencesHelper(context)

    init {
        createSources()
    }

    open fun get(sourceKey: Long): Source? {
        return sourcesMap[sourceKey]
    }

    fun getOnlineSources() = sourcesMap.values.filterIsInstance<HttpSource>()

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
            LocalSource(context),
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
                    sources.add(YamlHttpSource(map))
                } catch (e: Exception) {
                    Timber.e("Error loading source from file. Bad format?", e)
                }
            }
        }
        return sources
    }

    private fun createExtensionSources(): List<Source> {
        val pkgManager = context.packageManager
        val flags = PackageManager.GET_CONFIGURATIONS or PackageManager.GET_SIGNATURES
        val installedPkgs = pkgManager.getInstalledPackages(flags)
        val extPkgs = installedPkgs.filter { it.reqFeatures.orEmpty().any { it.name == EXTENSION_FEATURE } }

        val sources = mutableListOf<Source>()
        for (pkgInfo in extPkgs) {
            val appInfo = pkgManager.getApplicationInfo(pkgInfo.packageName,
                    PackageManager.GET_META_DATA) ?: continue

            val extName = pkgManager.getApplicationLabel(appInfo).toString()
                    .substringAfter("Tachiyomi: ")
            val version = pkgInfo.versionName
            val sourceClasses = appInfo.metaData.getString(METADATA_SOURCE_CLASS)
                    .split(";")
                    .map {
                        val sourceClass = it.trim()
                        if(sourceClass.startsWith("."))
                            pkgInfo.packageName + sourceClass
                        else
                            sourceClass
                    }

            val extension = Extension(extName, appInfo, version, sourceClasses)
            try {
                sources += loadExtension(extension)
            } catch (e: Exception) {
                Timber.e("Extension load error: $extName.", e)
            } catch (e: LinkageError) {
                Timber.e("Extension load error: $extName.", e)
            }
        }
        for (source in sources) {
            if (source is LoginSource) {
                val username = prefs.sourceUsername(source)
                val password = prefs.sourcePassword(source)
                if (username.length > 0 || password.length > 0) {
                    source.login(username, password)
                }
            }
        }
        return sources
    }

    private fun loadExtension(ext: Extension): List<Source> {
        // Validate lib version
        val majorLibVersion = ext.version.substringBefore('.').toInt()
        if (majorLibVersion < LIB_VERSION_MIN || majorLibVersion > LIB_VERSION_MAX) {
            throw Exception("Lib version is $majorLibVersion, while only versions "
                    + "$LIB_VERSION_MIN to $LIB_VERSION_MAX are allowed")
        }

        val classLoader = PathClassLoader(ext.appInfo.sourceDir, null, context.classLoader)
        return ext.sourceClasses.flatMap {
            val obj = Class.forName(it, false, classLoader).newInstance()
            when(obj) {
                is Source -> listOf(obj)
                is SourceFactory -> obj.createSources()
                else -> throw Exception("Unknown source class type!")
            }
        }
    }

    class Extension(val name: String,
                    val appInfo: ApplicationInfo,
                    val version: String,
                    val sourceClasses: List<String>)

    private companion object {
        const val EXTENSION_FEATURE = "tachiyomi.extension"
        const val METADATA_SOURCE_CLASS = "tachiyomi.extension.class"
        const val LIB_VERSION_MIN = 1
        const val LIB_VERSION_MAX = 1
    }

}
