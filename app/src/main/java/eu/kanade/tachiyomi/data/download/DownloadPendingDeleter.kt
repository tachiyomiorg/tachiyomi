package eu.kanade.tachiyomi.data.download

import android.content.Context
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import uy.kohesive.injekt.injectLazy

/**
 * Class used to keep a list of chapters for future deletion.
 *
 * @param context the application context.
 */
class DownloadPendingDeleter(context: Context) {

    /**
     * Gson instance to encode and decode chapters.
     */
    private val gson by injectLazy<Gson>()

    /**
     * Preferences used to store the list of chapters to delete.
     */
    private val prefs = context.getSharedPreferences("chapters_to_delete", Context.MODE_PRIVATE)

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
            val existingEntry = prefs.getString(manga.id!!.toString(), null)
            if (existingEntry != null) {
                // Existing entry found on preferences, decode json and add the new chapter
                val savedEntry = gson.fromJson<Entry>(existingEntry)

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
        val json = gson.toJson(newEntry)
        prefs.edit().putString(newEntry.manga.id.toString(), json).apply()
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
        prefs.edit().clear().apply()
        lastAddedEntry = null

        return entries.associate { entry ->
            entry.manga.toModel() to entry.chapters.map { it.toModel() }
        }
    }

    /**
     * Decodes all the chapters from preferences.
     */
    private fun decodeAll(): List<Entry> {
        return prefs.all.values.mapNotNull { rawEntry ->
            try {
                (rawEntry as? String)?.let { gson.fromJson<Entry>(it) }
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
    private data class Entry(
            val chapters: List<ChapterEntry>,
            val manga: MangaEntry
    )

    /**
     * Class used to save an entry for a chapter into preferences.
     */
    private data class ChapterEntry(
            val id: Long,
            val url: String,
            val name: String
    )

    /**
     * Class used to save an entry for a manga into preferences.
     */
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
        return ChapterEntry(id!!, url, name)
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
        }
    }

}
