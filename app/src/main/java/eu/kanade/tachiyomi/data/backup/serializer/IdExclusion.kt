package eu.kanade.tachiyomi.data.backup.serializer

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import eu.kanade.tachiyomi.data.database.models.CategoryImpl
import eu.kanade.tachiyomi.data.database.models.ChapterImpl
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.database.models.TrackImpl

class IdExclusion : ExclusionStrategy {

    private val categoryExclusions = listOf("id")
    private val mangaExclusions = listOf("id")
    private val chapterExclusions = listOf("id", "manga_id")
    private val syncExclusions = listOf("id", "manga_id", "update")

    override fun shouldSkipField(f: FieldAttributes) = when (f.declaringClass) {
        MangaImpl::class.java -> mangaExclusions.contains(f.name)
        ChapterImpl::class.java -> chapterExclusions.contains(f.name)
        TrackImpl::class.java -> syncExclusions.contains(f.name)
        CategoryImpl::class.java -> categoryExclusions.contains(f.name)
        else -> false
    }

    override fun shouldSkipClass(clazz: Class<*>) = false

}
