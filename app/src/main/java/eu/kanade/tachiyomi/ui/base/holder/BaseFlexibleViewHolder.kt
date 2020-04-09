package eu.kanade.tachiyomi.ui.base.holder

import android.view.View
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.viewholders.FlexibleViewHolder
import kotlinx.android.extensions.LayoutContainer

abstract class BaseFlexibleViewHolder(
    view: View,
    adapter: FlexibleAdapter<*>,
    stickyHeader: Boolean = false
) : FlexibleViewHolder(view, adapter, stickyHeader), LayoutContainer {

    override val containerView: View?
        get() = itemView
}
