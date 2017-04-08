package eu.kanade.tachiyomi.ui.category

import android.os.Bundle
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import kotlinx.android.synthetic.main.activity_edit_categories.*
import kotlinx.android.synthetic.main.toolbar.*

/**
 * Activity that shows categories.
 * Uses R.layout.activity_edit_categories.
 * UI related actions should be called from here.
 */
class CategoryActivity : BaseActivity() {

    private lateinit var router: Router

    override fun onCreate(savedInstanceState: Bundle?) {
        setAppTheme()
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_edit_categories)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            if (!isFinishing) {
                onBackPressed()
            }
        }

        router = Conductor.attachRouter(this, controller_container, savedInstanceState)
        if (!router.hasRootController()) {
            router.setRoot(RouterTransaction.with(CategoryController()))
        }
    }

    override fun onBackPressed() {
        if (!router.handleBack()) {
            super.onBackPressed()
        }
    }


}