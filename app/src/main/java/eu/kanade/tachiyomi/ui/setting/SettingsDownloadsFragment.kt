package eu.kanade.tachiyomi.ui.setting

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.v4.content.ContextCompat
import android.support.v7.preference.Preference
import android.support.v7.preference.XpPreferenceFragment
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.afollestad.materialdialogs.MaterialDialog
import com.hippo.unifile.UniFile
import com.nononsenseapps.filepicker.AbstractFilePickerFragment
import com.nononsenseapps.filepicker.FilePickerActivity
import com.nononsenseapps.filepicker.FilePickerFragment
import com.nononsenseapps.filepicker.LogicHandler
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.util.inflate
import eu.kanade.tachiyomi.util.plusAssign
import uy.kohesive.injekt.injectLazy
import java.io.File

class SettingsDownloadsFragment : SettingsFragment() {

    companion object {
        val DOWNLOAD_DIR_CODE = 103

        fun newInstance(rootKey: String): SettingsDownloadsFragment {
            val args = Bundle()
            args.putString(XpPreferenceFragment.ARG_PREFERENCE_ROOT, rootKey)
            return SettingsDownloadsFragment().apply { arguments = args }
        }
    }

    private val preferences: PreferencesHelper by injectLazy()

    val downloadDirPref: Preference by bindPref(R.string.pref_download_directory_key)

    override fun onViewCreated(view: View, savedState: Bundle?) {
        super.onViewCreated(view, savedState)

        downloadDirPref.setOnPreferenceClickListener {

            val currentDir = preferences.downloadsDirectory().getOrDefault()
            val externalDirs = getExternalFilesDirs() + getString(R.string.custom_dir)
            val selectedIndex = externalDirs.indexOf(File(currentDir))

            MaterialDialog.Builder(activity)
                    .items(externalDirs)
                    .itemsCallbackSingleChoice(selectedIndex, { dialog, view, which, text ->
                        if (which == externalDirs.lastIndex) {
                            if (Build.VERSION.SDK_INT < 21) {
                                // Custom dir selected, open directory selector
                                val i = Intent(activity, CustomLayoutPickerActivity::class.java)
                                i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)
                                i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true)
                                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR)
                                i.putExtra(FilePickerActivity.EXTRA_START_PATH, currentDir)

                                startActivityForResult(i, DOWNLOAD_DIR_CODE)
                            } else {
                                val i = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                                startActivityForResult(i, DOWNLOAD_DIR_CODE)
                            }
                        } else {
                            // One of the predefined folders was selected
                            val path = Uri.parse(text.toString()).toString()
                            // FIXME find a better approach
                            preferences.downloadsDirectory().set("file://$path")
                        }
                        true
                    })
                    .show()

            true
        }

        subscriptions += preferences.downloadsDirectory().asObservable()
                .subscribe { downloadDirPref.summary = it }
    }

    fun getExternalFilesDirs(): List<File> {
        val defaultDir = Environment.getExternalStorageDirectory().absolutePath +
                File.separator + getString(R.string.app_name) +
                File.separator + "downloads"

        return mutableListOf(File(defaultDir)) +
                ContextCompat.getExternalFilesDirs(activity, "").filterNotNull()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && requestCode == DOWNLOAD_DIR_CODE && resultCode == Activity.RESULT_OK) {
            val file = UniFile.fromTreeUri(context, data.data)
            preferences.downloadsDirectory().set(file.uri.toString())
        }
    }

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
            when (viewType) {
                LogicHandler.VIEWTYPE_DIR -> {
                    val view = parent.inflate(R.layout.listitem_dir)
                    return DirViewHolder(view)
                }
                else -> return super.onCreateViewHolder(parent, viewType)
            }
        }
    }

}
