package eu.kanade.tachiyomi.ui.reader.model

data class ViewerChapters(
        val currChapter: ReaderChapter,
        val prevChapter: ReaderChapter?,
        val nextChapter: ReaderChapter?
) {

    fun ref() {
        currChapter.ref()
        prevChapter?.ref()
        nextChapter?.ref()
    }

    fun unref() {
        currChapter.unref()
        prevChapter?.unref()
        nextChapter?.unref()
    }

}
