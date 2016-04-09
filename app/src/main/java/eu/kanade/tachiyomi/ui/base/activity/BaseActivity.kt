package eu.kanade.tachiyomi.ui.base.activity

import android.graphics.Color
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.TextView
import eu.kanade.tachiyomi.App
import eu.kanade.tachiyomi.R

open class BaseActivity : AppCompatActivity() {

    protected fun setupToolbar(toolbar: Toolbar, backNavigation: Boolean = true) {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (backNavigation) {
            toolbar.setNavigationOnClickListener { onBackPressed() }
        }
    }

    fun setAppTheme() {
        when (app.appTheme) {
            2 -> setTheme(R.style.Theme_Tachiyomi_Dark)
            else -> setTheme(R.style.Theme_Tachiyomi)
        }
    }

    fun setToolbarTitle(title: String) {
        supportActionBar?.title = title
    }

    fun setToolbarTitle(titleResource: Int) {
        supportActionBar?.title = getString(titleResource)
    }

    fun setToolbarSubtitle(title: String) {
        supportActionBar?.subtitle = title
    }

    fun setToolbarSubtitle(titleResource: Int) {
        supportActionBar?.subtitle = getString(titleResource)
    }

    protected val app: App
        get() = App.get(this)

    inline fun View.snack(message: String, length: Int = Snackbar.LENGTH_LONG, f: Snackbar.() -> Unit) {
        val snack = Snackbar.make(this, message, length)
        val textView = snack.view.findViewById(android.support.design.R.id.snackbar_text) as TextView
        textView.setTextColor(Color.WHITE)
        snack.f()
        snack.show()
    }

}