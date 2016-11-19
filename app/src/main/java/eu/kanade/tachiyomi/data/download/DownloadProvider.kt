package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.net.Uri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.source.Source
import uy.kohesive.injekt.injectLazy

/**
 * This class is used to provide the directories where the downloads should be saved.
 * It uses the following path scheme: /<root downloads dir>/<source name>/<manga>/<chapter>
 *
 * @param context the application context.
 */
class DownloadProvider(private val context: Context) {

    /**
     * Preferences helper.
     */
    private val preferences: PreferencesHelper by injectLazy()

    /**
     * The root directory for downloads.
     */
    private lateinit var downloadsDir: UniFile

    init {
        preferences.downloadsDirectory().asObservable()
                .subscribe { downloadsDir = UniFile.fromUri(context, Uri.parse(it)) }
    }

    /**
     * Returns the download directory for a manga. For internal use only.
     *
     * @param source the source of the manga.
     * @param manga the manga to query.
     */
    internal fun getMangaDir(source: Source, manga: Manga): UniFile {
        return downloadsDir
                .subFile(getSourceDirName(source))!!
                .subFile(getMangaDirName(manga))!!
    }

    /**
     * Returns the download directory for a manga if it exists.
     *
     * @param source the source of the manga.
     * @param manga the manga to query.
     */
    fun findMangaDir(source: Source, manga: Manga): UniFile? {
        val sourceDir = downloadsDir.findFile(getSourceDirName(source))
        return sourceDir?.findFile(getMangaDirName(manga))
    }

    /**
     * Returns the download directory for a chapter if it exists.
     *
     * @param source the source of the chapter.
     * @param manga the manga of the chapter.
     * @param chapter the chapter to query.
     */
    fun findChapterDir(source: Source, manga: Manga, chapter: Chapter): UniFile? {
        val mangaDir = findMangaDir(source, manga)
        return mangaDir?.findFile(getChapterDirName(chapter))
    }

    /**
     * Returns the download directory name for a source.
     *
     * @param source the source to query.
     */
    fun getSourceDirName(source: Source): String {
        return source.toString()
    }

    /**
     * Returns the download directory name for a manga.
     *
     * @param manga the manga to query.
     */
    fun getMangaDirName(manga: Manga): String {
        return buildValidFatFilename(manga.title.trim('.', ' '))
    }

    /**
     * Returns the chapter directory name for a chapter.
     *
     * @param chapter the chapter to query.
     */
    fun getChapterDirName(chapter: Chapter): String {
        return buildValidFatFilename(chapter.name.trim('.', ' '))
    }

    /**
     * Mutate the given filename to make it valid for a FAT filesystem,
     * replacing any invalid characters with "_".
     */
    private fun buildValidFatFilename(name: String): String {
        if (name.isNullOrEmpty()) {
            return "(invalid)"
        }
        val res = StringBuilder(name.length)
        name.forEach { c ->
            if (isValidFatFilenameChar(c)) {
                res.append(c)
            } else {
                res.append('_')
            }
        }
        // Even though vfat allows 255 UCS-2 chars, we might eventually write to
        // ext4 through a FUSE layer, so use that limit minus 5 reserved characters.
        return res.toString().take(250)
    }

    /**
     * Returns true if the given character is a valid filename character, false otherwise.
     */
    private fun isValidFatFilenameChar(c: Char): Boolean {
        if (0x00.toChar() <= c && c <= 0x1f.toChar()) {
            return false
        }
        when (c) {
            '"', '*', '/', ':', '<', '>', '?', '\\', '|', 0x7f.toChar() -> return false
            else -> return true
        }
    }
}