package eu.kanade.tachiyomi.ui.setting

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.animation.doOnEnd
import androidx.core.view.updatePadding
import androidx.preference.PreferenceController
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import dev.chrisbanes.insetter.applyInsetter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.controller.BaseController
import eu.kanade.tachiyomi.ui.base.controller.RootController
import eu.kanade.tachiyomi.util.system.getResourceColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

abstract class SettingsController : PreferenceController() {

    var preferenceKey: String? = null
    val preferences: PreferencesHelper = Injekt.get()
    val viewScope: CoroutineScope = MainScope()
    private var themedContext: Context? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        if (this is RootController) {
            listView.clipToPadding = false
            listView.updatePadding(bottom = view.context.resources.getDimensionPixelSize(R.dimen.action_toolbar_list_padding))
        }

        listView.applyInsetter {
            type(navigationBars = true) {
                padding()
            }
        }

        return view
    }

    override fun onAttach(view: View) {
        super.onAttach(view)

        preferenceKey?.let { prefKey ->
            val adapter = listView.adapter
            scrollToPreference(prefKey)

            listView.post {
                if (adapter is PreferenceGroup.PreferencePositionCallback) {
                    val pos = adapter.getPreferenceAdapterPosition(prefKey)
                    listView.findViewHolderForAdapterPosition(pos)?.let {
                        animatePreferenceHighlight(it.itemView)
                    }
                }
            }
        }
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        if (type.isEnter) {
            setTitle()
        }
        setHasOptionsMenu(type.isEnter)
        super.onChangeStarted(handler, type)
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        viewScope.cancel()
        themedContext = null
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val tv = TypedValue()
        activity!!.theme.resolveAttribute(R.attr.preferenceTheme, tv, true)
        themedContext = ContextThemeWrapper(activity, tv.resourceId)

        val screen = preferenceManager.createPreferenceScreen(themedContext)
        preferenceScreen = screen
        setupPreferenceScreen(screen)
    }

    abstract fun setupPreferenceScreen(screen: PreferenceScreen): PreferenceScreen

    private fun animatePreferenceHighlight(view: View) {
        val origBackground = view.background
        ValueAnimator
            .ofObject(ArgbEvaluator(), Color.TRANSPARENT, view.context.getResourceColor(R.attr.colorControlHighlight))
            .apply {
                duration = 200L
                repeatCount = 5
                repeatMode = ValueAnimator.REVERSE
                addUpdateListener { animator -> view.setBackgroundColor(animator.animatedValue as Int) }
                start()
            }
            .doOnEnd {
                // Restore original ripple
                view.background = origBackground
            }
    }

    open fun getTitle(): String? {
        return preferenceScreen?.title?.toString()
    }

    private fun setTitle() {
        var parentController = parentController
        while (parentController != null) {
            if (parentController is BaseController<*> && parentController.getTitle() != null) {
                return
            }
            parentController = parentController.parentController
        }

        (activity as? AppCompatActivity)?.supportActionBar?.title = getTitle()
    }
}
