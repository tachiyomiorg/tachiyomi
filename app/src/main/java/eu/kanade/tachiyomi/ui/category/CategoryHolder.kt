package eu.kanade.tachiyomi.ui.category

import android.graphics.Color
import android.graphics.Typeface
import android.support.v4.view.MotionEventCompat
import android.view.MotionEvent
import android.view.View
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.ui.base.adapter.FlexibleViewHolder
import eu.kanade.tachiyomi.ui.base.adapter.OnStartDragListener
import kotlinx.android.synthetic.main.item_edit_categories.view.*

/**
 * Holder that contains category item.
 * Uses R.layout.item_edit_categories.
 * UI related actions should be called from here.
 *
 * @param view view of category item.
 * @param adapter adapter belonging to holder.
 * @param listener called when item clicked.
 * @param dragListener called when item dragged.
 *
 * @constructor Create CategoryHolder object
 */
class CategoryHolder(
        view: View,
        adapter: CategoryAdapter,
        listener: FlexibleViewHolder.OnListItemClickListener,
        dragListener: OnStartDragListener
) : FlexibleViewHolder(view, adapter, listener) {

    init {
        // Create round letter image onclick to simulate long click
        itemView.image.setOnClickListener {
            // Simulate long click on this view to enter selection mode
            onLongClick(view)
        }

        // Set on touch listener for reorder image
        itemView.reorder.setOnTouchListener { v, event ->
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                dragListener.onStartDrag(this)
            }
            false
        }
    }

    /**
     * Update category item values.
     *
     * @param category category of item.
     */
    fun onSetValues(category: Category) {
        // Set capitalized title.
        itemView.title.text = category.name.capitalize()

        // Update circle letter image.
        itemView.post {
            itemView.image.setImageDrawable(getRound(category.name.take(1).toUpperCase()))
        }
    }

    /**
     * Returns circle letter image
     *
     * @param text first letter of string
     */
    private fun getRound(text: String): TextDrawable {
        val size = Math.min(itemView.image.width, itemView.image.height)
        return TextDrawable.builder()
                .beginConfig()
                .width(size)
                .height(size)
                .textColor(Color.WHITE)
                .useFont(Typeface.DEFAULT)
                .endConfig()
                .buildRound(text, ColorGenerator.MATERIAL.getColor(text))
    }
}