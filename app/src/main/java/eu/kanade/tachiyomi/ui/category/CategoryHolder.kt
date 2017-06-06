package eu.kanade.tachiyomi.ui.category

import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.data.database.models.Category
import kotlinx.android.synthetic.main.categories_item.view.*

/**
 * Holder used to display category items.
 *
 * @param view The view used by category items.
 * @param adapter The adapter containing this holder.
 */
class CategoryHolder(view: View, val adapter: CategoryAdapter) : FlexibleViewHolder(view, adapter) {

    init {
        // Create round letter image onclick to simulate long click
        itemView.image.setOnClickListener {
            // Simulate long click on this view to enter selection mode
            onLongClick(view)
        }

        setDragHandleView(itemView.reorder)
    }

    /**
     * Binds this holder with the given category.
     *
     * @param category The category to bind.
     */
    fun bind(category: Category) {
        // Set capitalized title.
        itemView.title.text = category.name.capitalize()

        // Update circle letter image.
        itemView.post {
            itemView.image.setImageDrawable(getRound(category.name.take(1).toUpperCase()))
        }
    }

    /**
     * Returns circle letter image.
     *
     * @param text The first letter of string.
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

    /**
     * Called when an item is released.
     *
     * @param position The position of the released item.
     */
    override fun onItemReleased(position: Int) {
        super.onItemReleased(position)
        adapter.onItemReleaseListener.onItemReleased(position)
    }

}