package eu.kanade.tachiyomi.data.download

import android.content.Context
import androidx.core.content.edit
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.injectLazy

/**
 * Class used to keep a list of chapters for future deletion.
 *
 * @param context the application context.
 */
class DownloadPendingDeleter(context: Context) {

    private val json: Json by injectLazy()

    /**
     * Preferences used to store the list of chapters to delete.
     */
    private val preferences = context.getSharedPreferences("chapters_to_delete", Context.MODE_PRIVATE)

    /**
     * Last added chapter, used to avoid decoding from the preference too often.
     */
    private var lastAddedEntry: Entry? = null

    /**
     * Adds a list of chapters for future deletion.
     *
     * @param chapters the chapters to be deleted.
     * @param manga the manga of the chapters.
     */
    @Synchronized
    fun addChapters(chapters: List<Chapter>, manga: Manga) {
        val lastEntry = lastAddedEntry

        val newEntry = if (lastEntry != null && lastEntry.manga.id == manga.id) {
            // Append new chapters
            val newChapters = lastEntry.chapters.addUniqueById(chapters)

            // If no chapters were added, do nothing
            if (newChapters.size == lastEntry.chapters.size) return

            // Last entry matches the manga, reuse it to avoid decoding json from preferences
            lastEntry.copy(chapters = newChapters)
        } else {
            val existingEntry = preferences.getString(manga.id!!.toString(), null)
            if (existingEntry != null) {
                // Existing entry found on preferences, decode json and add the new chapter
                val savedEntry = json.decodeFromString<Entry>(existingEntry)

                // Append new chapters
                val newChapters = savedEntry.chapters.addUniqueById(chapters)

                // If no chapters were added, do nothing
                if (newChapters.size == savedEntry.chapters.size) return

                savedEntry.copy(chapters = newChapters)
            } else {
                // No entry has been found yet, create a new one
                Entry(chapters.map { it.toEntry() }, manga.toEntry())
            }
        }

        // Save current state
        val json = json.encodeToString(newEntry)
        preferences.edit {
            putString(newEntry.manga.id.toString(), json)
        }
        lastAddedEntry = newEntry
    }

    /**
     * Returns the list of chapters to be deleted grouped by its manga.
     *
     * Note: the returned list of manga and chapters only contain basic information needed by the
     * downloader, so don't use them for anything else.
     */
    @Synchronized
    fun getPendingChapters(): Map<Manga, List<Chapter>> {
        val entries = decodeAll()
        preferences.edit {
            clear()
        }
        lastAddedEntry = null

        return entries.associate { (chapters, manga) ->
            manga.toModel() to chapters.map { it.toModel() }
        }
    }

    /**
     * Decodes all the chapters from preferences.
     */
    private fun decodeAll(): List<Entry> {
        return preferences.all.values.mapNotNull { rawEntry ->
            try {
                (rawEntry as? String)?.let { json.decodeFromString<Entry>(it) }
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Returns a copy of chapter entries ensuring no duplicates by chapter id.
     */
    private fun List<ChapterEntry>.addUniqueById(chapters: List<Chapter>): List<ChapterEntry> {
        val newList = toMutableList()
        for (chapter in chapters) {
            if (none { it.id == chapter.id }) {
                newList.add(chapter.toEntry())
            }
        }
        return newList
    }

    /**
     * Class used to save an entry of chapters with their manga into preferences.
     */
    @Serializable
    private data class Entry(
        val chapters: List<ChapterEntry>,
        val manga: MangaEntry
    )

    /**
     * Class used to save an entry for a chapter into preferences.
     */
    @Serializable
    private data class ChapterEntry(
        val id: Long,
        val url: String,
        val name: String,
        val scanlator: String? = null
    )

    /**
     * Class used to save an entry for a manga into preferences.
     */
    @Serializable
    private data class MangaEntry(
        val id: Long,
        val url: String,
        val title: String,
        val source: Long
    )

    /**
     * Returns a manga entry from a manga model.
     */
    private fun Manga.toEntry(): MangaEntry {
        return MangaEntry(id!!, url, title, source)
    }

    /**
     * Returns a chapter entry from a chapter model.
     */
    private fun Chapter.toEntry(): ChapterEntry {
        return ChapterEntry(id!!, url, name, scanlator)
    }

    /**
     * Returns a manga model from a manga entry.
     */
    private fun MangaEntry.toModel(): Manga {
        return Manga.create(url, title, source).also {
            it.id = id
        }
    }

    /**
     * Returns a chapter model from a chapter entry.
     */
    private fun ChapterEntry.toModel(): Chapter {
        return Chapter.create().also {
            it.id = id
            it.url = url
            it.name = name
            it.scanlator = scanlator
        }
    }
}
