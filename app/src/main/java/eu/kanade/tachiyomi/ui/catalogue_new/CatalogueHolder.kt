package eu.kanade.tachiyomi.ui.catalogue_new

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.LoginSource
import kotlinx.android.synthetic.main.catalogue_item.view.*

@SuppressLint("ViewConstructor")
class CatalogueHolder(view: View, val adapter: CatalogueAdapter) : FlexibleViewHolder(view, adapter) {
    fun bind(source: Source) {
        with(itemView) {


            title.text = source.name
            // Update circle letter image.
            post {
                image.setImageDrawable(getRound(source.name.take(1).toUpperCase()))
            }

            if (source is LoginSource && !source.isLogged()) {
                source_option.text = context.getString(R.string.login)
            }
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
                .withBorder(25)
                .textColor(Color.WHITE)
                .useFont(Typeface.DEFAULT)
                .endConfig()
                .buildRound(text, ColorGenerator.MATERIAL.getColor(text[0]))
    }
}