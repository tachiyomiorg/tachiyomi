package eu.kanade.tachiyomi.ui.manga.chapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.MangaChaptersHeaderBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class MangaChaptersHeaderAdapter :
    RecyclerView.Adapter<MangaChaptersHeaderAdapter.HeaderViewHolder>() {

    private var numChapters: Int? = null

    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private lateinit var binding: MangaChaptersHeaderBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        binding = MangaChaptersHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HeaderViewHolder(binding.root)
    }

    override fun getItemCount(): Int = 1

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.bind()
    }

    fun setNumChapters(numChapters: Int) {
        this.numChapters = numChapters

        notifyDataSetChanged()
    }

    inner class HeaderViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        fun bind() {
            binding.chaptersLabel.text = if (numChapters == null) {
                view.context.getString(R.string.chapters)
            } else {
                view.context.resources.getQuantityString(R.plurals.manga_num_chapters, numChapters!!, numChapters)
            }
        }
    }
}
