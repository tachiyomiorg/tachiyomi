package eu.kanade.tachiyomi.data.glide

import eu.kanade.tachiyomi.data.database.models.Manga

data class MangaThumbnail(val manga: Manga, val url: String?)

fun Manga.toMangaThumbnail() = MangaThumbnail(this, this.thumbnail_url)
