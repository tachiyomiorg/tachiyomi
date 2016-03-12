package eu.kanade.tachiyomi.ui.base.activity

import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import eu.kanade.tachiyomi.App
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.injection.component.AppComponent
import icepick.Icepick
import org.greenrobot.eventbus.EventBus

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        Icepick.restoreInstanceState(this, savedState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Icepick.saveInstanceState(this, outState)
    }

    protected fun setupToolbar(toolbar: Toolbar) {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun registerForEvents() {
        EventBus.getDefault().register(this)
    }

    fun unregisterForEvents() {
        EventBus.getDefault().unregister(this)
    }

    fun snack(text: String?, duration: Int = Snackbar.LENGTH_LONG) {
        val snack = Snackbar.make(findViewById(android.R.id.content)!!, text ?: getString(R.string.unknown_error), duration)
        val textView = snack.view.findViewById(android.support.design.R.id.snackbar_text) as TextView
        textView.setTextColor(Color.WHITE)
        snack.show()
    }

    fun snack(text: String?, actionRes: Int, actionFunc: () -> Unit,
              duration: Int = Snackbar.LENGTH_LONG, view: View = findViewById(android.R.id.content)!!) {

        val snack = Snackbar.make(view, text ?: getString(R.string.unknown_error), duration)
                .setAction(actionRes, { actionFunc() })

        val textView = snack.view.findViewById(android.support.design.R.id.snackbar_text) as TextView
        textView.setTextColor(Color.WHITE)
        snack.show()
    }

    protected val applicationComponent: AppComponent
        get() = App.get(this).component

}