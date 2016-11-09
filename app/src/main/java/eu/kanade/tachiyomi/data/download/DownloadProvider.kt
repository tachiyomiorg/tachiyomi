package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.net.Uri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.source.Source
import uy.kohesive.injekt.injectLazy

class DownloadProvider(private val context: Context) {

    private val preferences: PreferencesHelper by injectLazy()

    private lateinit var downloadsDir: UniFile

    init {
        preferences.downloadsDirectory().asObservable()
                .subscribe { setDownloadsDir(it) }
    }

    private fun setDownloadsDir(path: String) {
        downloadsDir = UniFile.fromUri(context, Uri.parse(path))
    }

    fun getMangaDir(source: Source, manga: Manga): UniFile {
        val uri = downloadsDir.uri.buildUpon()
                .appendPath(source.toString())
                .appendPath(buildValidFatFilename(manga.title))
                .build()
        return UniFile.fromUri(context, uri)
    }

    fun getChapterDir(mangaDir: UniFile, chapter: Chapter): UniFile {
        val uri = mangaDir.uri.buildUpon()
                .appendPath(buildValidFatFilename(chapter.name))
                .build()
        return UniFile.fromUri(context, uri)
    }

    fun getTmpChapterDir(chapterDir: UniFile): UniFile {
        val file = chapterDir.uri.buildUpon().encodedPath("${chapterDir.uri.encodedPath}_tmp").build()
        return UniFile.fromUri(context, file)
    }

    /**
     * Mutate the given filename to make it valid for a FAT filesystem,
     * replacing any invalid characters with "_".
     */
    private fun buildValidFatFilename(name: String): String {
        if (name.isNullOrBlank() || "." == name || ".." == name) {
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
        // ext4 through a FUSE layer, so use that limit.
        return res.toString().take(255)
    }

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