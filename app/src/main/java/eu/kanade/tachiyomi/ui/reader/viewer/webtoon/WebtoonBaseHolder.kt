package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams
import eu.kanade.tachiyomi.ui.base.holder.BaseViewHolder
import rx.Subscription

abstract class WebtoonBaseHolder(
        view: View,
        protected val viewer: WebtoonViewer
) : BaseViewHolder(view) {

    /**
     * Context getter because it's used often.
     */
    val context: Context get() = itemView.context

    /**
     * Called when the view is recycled and being added to the view pool.
     */
    open fun recycle() {}

    /**
     * Adds a subscription to a list of subscriptions that will automatically unsubscribe when the
     * activity or the reader is destroyed.
     */
    protected fun addSubscription(subscription: Subscription?) {
        viewer.subscriptions.add(subscription)
    }

    /**
     * Removes a subscription from the list of subscriptions.
     */
    protected fun removeSubscription(subscription: Subscription?) {
        subscription?.let { viewer.subscriptions.remove(it) }
    }

    /**
     * Extension method to set layout params to wrap content on this view.
     */
    protected fun View.wrapContent() {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

}
