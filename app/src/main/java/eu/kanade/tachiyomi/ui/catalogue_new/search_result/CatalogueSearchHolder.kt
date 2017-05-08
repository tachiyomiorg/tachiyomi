package eu.kanade.tachiyomi.ui.catalogue_new.search_result

import android.annotation.SuppressLint
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.manga.MangaController
import kotlinx.android.synthetic.main.catalogue_search_item.view.*

@SuppressLint("ViewConstructor")
class CatalogueSearchHolder(view: View, val adapter: CatalogueSearchAdapter) : FlexibleViewHolder(view, adapter), FlexibleAdapter.OnItemClickListener {

    private var mangaAdapter: CatalogueSearchSingleAdapter? = null

    override fun onItemClick(position: Int): Boolean {
        val item = mangaAdapter?.getItem(position) as? CatalogueSearchSingleItem ?: return false
        adapter.controller.router.pushController(RouterTransaction.with(MangaController(item.manga, true))
                .pushChangeHandler(FadeChangeHandler())
                .popChangeHandler(FadeChangeHandler()))
        return true
    }

    fun setImage(manga: Manga){
        getHolder(manga)?.setImage(manga)
    }

    /**
     * Returns the view holder for the given manga.
     *
     * @param manga the manga to find.
     * @return the holder of the manga or null if it's not bound.
     */
    private fun getHolder(manga: Manga): CatalogueSearchSingleHolder? {
        val adapter = mangaAdapter ?: return null

        adapter.allBoundViewHolders.forEach { holder ->
            val item = adapter.getItem(holder.adapterPosition)
            if (item != null && item.manga.id!! == manga.id!!) {
                return holder as CatalogueSearchSingleHolder
            }
        }

        return null
    }



    fun bind(searchResult: Pair<List<Manga>, Source>) {
        val source = searchResult.second
        val mangas = searchResult.first

        with(itemView) {
            title.text = source.name

            if (!mangas.isEmpty()) {
                itemView.nothing_found.visibility = View.GONE
                itemView.recycler.visibility = View.VISIBLE
                recycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                recycler.isNestedScrollingEnabled = false
                mangaAdapter = CatalogueSearchSingleAdapter(this@CatalogueSearchHolder)
                recycler.adapter = mangaAdapter
                mangaAdapter?.updateDataSet(searchResult.first.map(::CatalogueSearchSingleItem))
            }else{
                itemView.nothing_found.visibility = View.VISIBLE
                itemView.recycler.visibility = View.GONE
            }
        }

        // Update circle letter image.
//        itemView.post {
//            itemView.image.setImageDrawable(getRound(source.name.take(1).toUpperCase()))
//        }
    }
}