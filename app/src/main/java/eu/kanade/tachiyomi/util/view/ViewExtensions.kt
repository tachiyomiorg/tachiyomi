@file:Suppress("NOTHING_TO_INLINE")

package eu.kanade.tachiyomi.util.view

import android.annotation.SuppressLint
import android.graphics.Point
import android.text.TextUtils
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.elevation.ElevationOverlayProvider
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.isNightMode

/**
 * Returns coordinates of view.
 * Used for animation
 *
 * @return coordinates of view
 */
fun View.getCoordinates() = Point((left + right) / 2, (top + bottom) / 2)

/**
 * Shows a snackbar in this view.
 *
 * @param message the message to show.
 * @param length the duration of the snack.
 * @param f a function to execute in the snack, allowing for example to define a custom action.
 */
inline fun View.snack(
    message: String,
    length: Int = Snackbar.LENGTH_LONG,
    f: Snackbar.() -> Unit = {}
): Snackbar {
    val snack = Snackbar.make(this, message, length)
    snack.f()
    snack.show()
    return snack
}

/**
 * Adds a tooltip shown on long press.
 *
 * @param stringRes String resource for tooltip.
 */
inline fun View.setTooltip(@StringRes stringRes: Int) {
    setTooltip(context.getString(stringRes))
}

/**
 * Adds a tooltip shown on long press.
 *
 * @param text Text for tooltip.
 */
inline fun View.setTooltip(text: String) {
    TooltipCompat.setTooltipText(this, text)
}

/**
 * Shows a popup menu on top of this view.
 *
 * @param menuRes menu items to inflate the menu with.
 * @param initMenu function to execute when the menu after is inflated.
 * @param onMenuItemClick function to execute when a menu item is clicked.
 */
inline fun View.popupMenu(
    @MenuRes menuRes: Int,
    noinline initMenu: (Menu.() -> Unit)? = null,
    noinline onMenuItemClick: MenuItem.() -> Unit
): PopupMenu {
    val popup = PopupMenu(context, this, Gravity.NO_GRAVITY, R.attr.actionOverflowMenuStyle, 0)
    popup.menuInflater.inflate(menuRes, popup.menu)

    if (initMenu != null) {
        popup.menu.initMenu()
    }
    popup.setOnMenuItemClickListener {
        it.onMenuItemClick()
        true
    }

    popup.show()
    return popup
}

/**
 * Shows a popup menu on top of this view.
 *
 * @param items menu item names to inflate the menu with. List of itemId to stringRes pairs.
 * @param selectedItemId optionally show a checkmark beside an item with this itemId.
 * @param onMenuItemClick function to execute when a menu item is clicked.
 */
@SuppressLint("RestrictedApi")
inline fun View.popupMenu(
    items: List<Pair<Int, Int>>,
    selectedItemId: Int? = null,
    noinline onMenuItemClick: MenuItem.() -> Unit
): PopupMenu {
    val popup = PopupMenu(context, this, Gravity.NO_GRAVITY, R.attr.actionOverflowMenuStyle, 0)
    items.forEach { (id, stringRes) ->
        popup.menu.add(0, id, 0, stringRes)
    }

    if (selectedItemId != null) {
        (popup.menu as? MenuBuilder)?.setOptionalIconsVisible(true)
        val emptyIcon = ContextCompat.getDrawable(context, R.drawable.ic_blank_24dp)
        popup.menu.forEach { item ->
            item.icon = when (item.itemId) {
                selectedItemId -> ContextCompat.getDrawable(context, R.drawable.ic_check_24dp)?.mutate()?.apply {
                    setTint(context.getResourceColor(android.R.attr.textColorPrimary))
                }
                else -> emptyIcon
            }
        }
    }

    popup.setOnMenuItemClickListener {
        it.onMenuItemClick()
        true
    }

    popup.show()
    return popup
}

/**
 * Shrink an ExtendedFloatingActionButton when the associated RecyclerView is scrolled down.
 *
 * @param recycler [RecyclerView] that the FAB should shrink/extend in response to.
 */
inline fun ExtendedFloatingActionButton.shrinkOnScroll(recycler: RecyclerView): RecyclerView.OnScrollListener {
    val listener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (dy <= 0) {
                extend()
            } else {
                shrink()
            }
        }
    }
    recycler.addOnScrollListener(listener)
    return listener
}

/**
 * Replaces chips in a ChipGroup.
 *
 * @param items List of strings that are shown as individual chips.
 * @param onClick Optional on click listener for each chip.
 */
inline fun ChipGroup.setChips(
    items: List<String>?,
    noinline onClick: (item: String) -> Unit = {}
) {
    removeAllViews()

    items?.forEach { item ->
        val chip = Chip(context).apply {
            text = item
            setOnClickListener { onClick(item) }
        }

        addView(chip)
    }
}

/**
 * Applies elevation overlay to a MaterialCardView
 */
inline fun MaterialCardView.applyElevationOverlay() {
    if (context.isNightMode()) {
        val provider = ElevationOverlayProvider(context)
        setCardBackgroundColor(provider.compositeOverlay(cardBackgroundColor.defaultColor, cardElevation))
    }
}

/**
 * Sets TextView max lines dynamically. Can only be called when the view is already laid out.
 */
inline fun TextView.setMaxLinesAndEllipsize(_ellipsize: TextUtils.TruncateAt = TextUtils.TruncateAt.END) = post {
    maxLines = (measuredHeight - paddingTop - paddingBottom) / lineHeight
    ellipsize = _ellipsize
}
