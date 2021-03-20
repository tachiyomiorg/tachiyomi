package eu.kanade.tachiyomi.ui.reader

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewPropertyAnimator
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import kotlin.math.abs

class ReaderNavigationOverlayView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    private var viewPropertyAnimator: ViewPropertyAnimator? = null

    private var navigation: ViewerNavigation? = null

    fun setNavigation(navigation: ViewerNavigation, showOnStart: Boolean) {
        if (!showOnStart && this.navigation == null) {
            this.navigation = navigation
            isVisible = false
            return
        }

        this.navigation = navigation
        invalidate()

        if (!isVisible) {
            viewPropertyAnimator = animate()
                .alpha(1f)
                .setDuration(1000L)
                .withStartAction {
                    isVisible = true
                    viewPropertyAnimator = null
                }
            viewPropertyAnimator?.start()
        }
    }

    private val textPaint = Paint().apply {
        textAlign = Paint.Align.CENTER
        color = Color.WHITE
        textSize = 64f
    }

    private val textBorderPaint = Paint().apply {
        textAlign = Paint.Align.CENTER
        color = Color.BLACK
        textSize = 64f
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    override fun onDraw(canvas: Canvas?) {
        if (navigation == null) return

        navigation?.regions?.forEach { region ->

            val paint = paintForRegion(region.type)

            val rect = region.rectF

            canvas?.save()

            // Scale rect from 1f,1f to screen width and height
            canvas?.scale(width.toFloat(), height.toFloat())
            canvas?.drawRect(rect, paint)

            canvas?.restore()
            // Don't want scale anymore because it messes with drawText
            canvas?.save()

            // Translate origin to rect start (left, top)
            canvas?.translate((width * rect.left), (height * rect.top))

            // Calculate center of rect width on screen
            val x = width * (abs(rect.left - rect.right) / 2)

            // Calculate center of rect height on screen
            val y = height * (abs(rect.top - rect.bottom) / 2)

            canvas?.drawText(region.type.name, x, y, textBorderPaint)
            canvas?.drawText(region.type.name, x, y, textPaint)

            canvas?.restore()
        }
    }

    private fun paintForRegion(type: ViewerNavigation.NavigationRegion): Paint {
        return Paint().apply {
            when (type) {
                ViewerNavigation.NavigationRegion.NEXT -> {
                    color = ContextCompat.getColor(context, R.color.navigation_next)
                }
                ViewerNavigation.NavigationRegion.PREV -> {
                    color = ContextCompat.getColor(context, R.color.navigation_prev)
                }
                ViewerNavigation.NavigationRegion.MENU -> {
                    color = ContextCompat.getColor(context, R.color.navigation_menu)
                }
                ViewerNavigation.NavigationRegion.RIGHT -> {
                    color = ContextCompat.getColor(context, R.color.navigation_right)
                }
                ViewerNavigation.NavigationRegion.LEFT -> {
                    color = ContextCompat.getColor(context, R.color.navigation_left)
                }
            }
        }
    }

    override fun performClick(): Boolean {
        super.performClick()

        if (viewPropertyAnimator == null && isVisible) {
            viewPropertyAnimator = animate()
                .alpha(0f)
                .setDuration(1000L)
                .withEndAction {
                    isVisible = false
                    viewPropertyAnimator = null
                }
            viewPropertyAnimator?.start()
        }

        return true
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // Hide overlay if user start tapping or swiping
        performClick()
        return super.onTouchEvent(event)
    }
}
