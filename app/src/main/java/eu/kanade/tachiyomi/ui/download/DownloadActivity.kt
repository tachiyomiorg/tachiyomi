package eu.kanade.tachiyomi.ui.download

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.MenuItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar.*

/**
 * TODO
 */
class DownloadActivity : BaseActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        setAppTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download_manager)
        setFragment(DownloadFragment.newInstance())
        setupToolbar(toolbar, backNavigation = false)
    }

    private fun setFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.frame_container, fragment)
                .commit()
    }

    fun updateEmptyView(show: Boolean, textResource: Int, drawable: Int) {
        if (show) empty_view.show(drawable, textResource) else empty_view.hide()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}