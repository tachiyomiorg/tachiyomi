package eu.kanade.tachiyomi.ui.reader.model

class InsertPage(val parent: ReaderPage, index: Int) : ReaderPage(index, parent.url, parent.imageUrl) {

    override var chapter: ReaderChapter = parent.chapter

    init {
        stream = parent.stream
    }
}
