package eu.kanade.tachiyomi.widget

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nononsenseapps.filepicker.AbstractFilePickerFragment
import com.nononsenseapps.filepicker.FilePickerActivity
import com.nononsenseapps.filepicker.FilePickerFragment
import com.nononsenseapps.filepicker.LogicHandler
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.view.inflate
import java.io.File

class CustomLayoutPickerActivity : FilePickerActivity() {

    override fun getFragment(startPath: String?, mode: Int, allowMultiple: Boolean, allowCreateDir: Boolean):
        AbstractFilePickerFragment<File> {
            val fragment = CustomLayoutFilePickerFragment()
            fragment.setArgs(startPath, mode, allowMultiple, allowCreateDir)
            return fragment
        }
}

class CustomLayoutFilePickerFragment : FilePickerFragment() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            LogicHandler.VIEWTYPE_DIR -> {
                val view = parent.inflate(R.layout.common_listitem_dir)
                DirViewHolder(view)
            }
            else -> super.onCreateViewHolder(parent, viewType)
        }
    }
}
