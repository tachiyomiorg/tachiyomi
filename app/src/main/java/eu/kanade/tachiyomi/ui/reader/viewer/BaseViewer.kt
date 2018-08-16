package eu.kanade.tachiyomi.ui.reader.viewer

import android.view.KeyEvent
import android.view.View
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters

abstract class BaseViewer(val activity: ReaderActivity) {

    abstract fun getView(): View

    open fun destroy() {}

    abstract fun setChapters(chapters: ViewerChapters)

    abstract fun moveToPage(page: ReaderPage)

    abstract fun moveLeft()

    abstract fun moveRight()

    abstract fun moveUp()

    abstract fun moveDown()

    open fun handleKeyEvent(event: KeyEvent): Boolean {
        return false
    }

}
