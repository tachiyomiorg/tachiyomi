package eu.kanade.tachiyomi.data.database.models

class LibraryManga : MangaImpl() {

    var unread: Int = 0

    var category: Int = 0

    var latest_upload: Long = 0

    var latest_read: Boolean = false

}