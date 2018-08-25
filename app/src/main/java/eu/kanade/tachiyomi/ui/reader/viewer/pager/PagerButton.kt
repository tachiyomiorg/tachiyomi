package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.annotation.SuppressLint
import android.content.Context
import android.support.v7.widget.AppCompatButton
import android.view.MotionEvent

/**
 * A button class to be used by child views of the pager viewer. All tap gestures are handled by
 * the pager, but this class disables that behavior to allow clickable buttons.
 */
@SuppressLint("ViewConstructor")
class PagerButton(context: Context, viewer: PagerViewer) : AppCompatButton(context) {

    init {
        setOnTouchListener { _, event ->
            viewer.pager.setGestureDetectorEnabled(false)
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                viewer.pager.setGestureDetectorEnabled(true)
            }
            false
        }
    }

}
