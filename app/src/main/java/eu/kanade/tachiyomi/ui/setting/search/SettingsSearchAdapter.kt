package eu.kanade.tachiyomi.ui.setting.search

import android.os.Bundle
import android.os.Parcelable
import android.util.SparseArray
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.ui.setting.SettingsController

/**
 * Adapter that holds the search cards.
 *
 * @param controller instance of [SettingsSearchController].
 */
class SettingsSearchAdapter(val controller: SettingsSearchController) :
    FlexibleAdapter<SettingsSearchItem>(null, controller, true) {

    val titleClickListener: OnTitleClickListener = controller

    /**
     * Bundle where the view state of the holders is saved.
     */
    private var bundle = Bundle()

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: List<Any?>
    ) {
        super.onBindViewHolder(holder, position, payloads)
        restoreHolderState(holder)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        saveHolderState(holder, bundle)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val holdersBundle = Bundle()
        allBoundViewHolders.forEach { saveHolderState(it, holdersBundle) }
        outState.putBundle(HOLDER_BUNDLE_KEY, holdersBundle)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        bundle = savedInstanceState.getBundle(HOLDER_BUNDLE_KEY)!!
    }

    /**
     * Saves the view state of the given holder.
     *
     * @param holder The holder to save.
     * @param outState The bundle where the state is saved.
     */
    private fun saveHolderState(holder: RecyclerView.ViewHolder, outState: Bundle) {
        val key = "holder_${holder.bindingAdapterPosition}"
        val holderState = SparseArray<Parcelable>()
        holder.itemView.saveHierarchyState(holderState)
        outState.putSparseParcelableArray(key, holderState)
    }

    /**
     * Restores the view state of the given holder.
     *
     * @param holder The holder to restore.
     */
    private fun restoreHolderState(holder: RecyclerView.ViewHolder) {
        val key = "holder_${holder.bindingAdapterPosition}"
        bundle.getSparseParcelableArray<Parcelable>(key)?.let {
            holder.itemView.restoreHierarchyState(it)
            bundle.remove(key)
        }
    }

    interface OnTitleClickListener {
        fun onTitleClick(ctrl: SettingsController)
    }

    private companion object {
        const val HOLDER_BUNDLE_KEY = "holder_bundle"
    }
}
