package eu.kanade.tachiyomi.ui.base.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

interface ActivityMixin {

    var resumed: Boolean

    fun setupToolbar(toolbar: Toolbar, backNavigation: Boolean = true) {
        setSupportActionBar(toolbar)
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)
        if (backNavigation) {
            toolbar.setNavigationOnClickListener {
                if (resumed) {
                    onBackPressed()
                }
            }
        }
    }

    fun setAppTheme() {
        setTheme(when (Injekt.get<PreferencesHelper>().theme()) {
            2 -> R.style.Theme_Tachiyomi_Dark
            else -> R.style.Theme_Tachiyomi
        })
    }

    fun setToolbarTitle(title: String) {
        getSupportActionBar()?.title = title
    }

    fun setToolbarTitle(titleResource: Int) {
        getSupportActionBar()?.title = getString(titleResource)
    }

    fun setToolbarSubtitle(title: String) {
        getSupportActionBar()?.subtitle = title
    }

    fun setToolbarSubtitle(titleResource: Int) {
        getSupportActionBar()?.subtitle = getString(titleResource)
    }

    /**
     * Requests read and write permissions on Android M and higher.
     */
    fun requestPermissionsOnMarshmallow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(getActivity(),
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                        1)

            }
        }
    }

    fun getActivity(): AppCompatActivity

    fun onBackPressed()

    fun getSupportActionBar(): ActionBar?

    fun setSupportActionBar(toolbar: Toolbar?)

    fun setTheme(resource: Int)

    fun getString(resource: Int): String

}