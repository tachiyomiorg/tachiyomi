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
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.util.inflate
import eu.kanade.tachiyomi.util.plusAssign
import net.xpece.android.support.preference.MultiSelectListPreference
import uy.kohesive.injekt.injectLazy
import java.io.File

class SettingsDownloadsFragment : SettingsFragment() {

    companion object {
        const val DOWNLOAD_DIR_PRE_L = 103
        const val DOWNLOAD_DIR_L = 104

        fun newInstance(rootKey: String): SettingsDownloadsFragment {
            val args = Bundle()
            args.putString(XpPreferenceFragment.ARG_PREFERENCE_ROOT, rootKey)
            return SettingsDownloadsFragment().apply { arguments = args }
        }
    }

    private val preferences: PreferencesHelper by injectLazy()

    private val db: DatabaseHelper by injectLazy()

    val downloadDirPref: Preference by bindPref(R.string.pref_download_directory_key)

    val downloadCategory: MultiSelectListPreference by bindPref(R.string.pref_download_new_categories_key)

    override fun onViewCreated(view: View, savedState: Bundle?) {
        super.onViewCreated(view, savedState)

        downloadDirPref.setOnPreferenceClickListener {

            val currentDir = preferences.downloadsDirectory().getOrDefault()
            val externalDirs = getExternalFilesDirs() + File(getString(R.string.custom_dir))
            val selectedIndex = externalDirs.map(File::toString).indexOfFirst { it in currentDir }

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

                                startActivityForResult(i, DOWNLOAD_DIR_PRE_L)
                            } else {
                                val i = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                                startActivityForResult(i, DOWNLOAD_DIR_L)
                            }
                        } else {
                            // One of the predefined folders was selected
                            val path = Uri.fromFile(File(text.toString()))
                            preferences.downloadsDirectory().set(path.toString())
                        }
                        true
                    })
                    .show()

            true
        }

        subscriptions += preferences.downloadsDirectory().asObservable()
                .subscribe { path ->
                    val dir = UniFile.fromUri(context, Uri.parse(path))

                    downloadDirPref.summary = dir.filePath ?: path

                    // Don't display downloaded chapters in gallery apps creating a ".nomedia" file.
                    if (dir != null && dir.exists()) {
                        dir.createFile(".nomedia")
                    }
                }

        subscriptions += preferences.downloadNew().asObservable()
                .subscribe { downloadCategory.isVisible = it }

        val dbCategories = db.getCategories().executeAsBlocking()
        downloadCategory.apply {
            entries = dbCategories.map { it.name }.toTypedArray()
            entryValues = dbCategories.map { it.id.toString() }.toTypedArray()
        }

        subscriptions += preferences.downloadNewCategories().asObservable()
                .subscribe {
                    val selectedCategories = it
                            .mapNotNull { id -> dbCategories.find { it.id == id.toInt() } }
                            .sortedBy { it.order }

                    val summary = if (selectedCategories.isEmpty())
                        getString(R.string.all)
                    else
                        selectedCategories.joinToString { it.name }

                    downloadCategory.summary = summary
                }
    }

    fun getExternalFilesDirs(): List<File> {
        val defaultDir = Environment.getExternalStorageDirectory().absolutePath +
                File.separator + getString(R.string.app_name) +
                File.separator + "downloads"

        return mutableListOf(File(defaultDir)) +
                ContextCompat.getExternalFilesDirs(activity, "").filterNotNull()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            DOWNLOAD_DIR_PRE_L -> if (data != null && resultCode == Activity.RESULT_OK) {
                val uri = Uri.fromFile(File(data.data.path))
                preferences.downloadsDirectory().set(uri.toString())
            }
            DOWNLOAD_DIR_L -> if (data != null && resultCode == Activity.RESULT_OK) {
                val uri = data.data
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                @Suppress("NewApi")
                context.contentResolver.takePersistableUriPermission(uri, flags)

                val file = UniFile.fromUri(context, uri)
                preferences.downloadsDirectory().set(file.uri.toString())
            }
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
