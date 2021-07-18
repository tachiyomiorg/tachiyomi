package eu.kanade.tachiyomi.ui.base.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import timber.log.Timber

abstract class BaseController<VB : ViewBinding>(bundle: Bundle? = null) : Controller(bundle) {

    protected lateinit var binding: VB
        private set

    lateinit var viewScope: CoroutineScope

    init {
        addLifecycleListener(
            object : LifecycleListener() {
                override fun postCreateView(controller: Controller, view: View) {
                    onViewCreated(view)
                }

                override fun preCreateView(controller: Controller) {
                    viewScope = MainScope()
                    Timber.d("Create view for ${controller.instance()}")
                }

                override fun preAttach(controller: Controller, view: View) {
                    Timber.d("Attach view for ${controller.instance()}")
                }

                override fun preDetach(controller: Controller, view: View) {
                    Timber.d("Detach view for ${controller.instance()}")
                }

                override fun preDestroyView(controller: Controller, view: View) {
                    viewScope.cancel()
                    Timber.d("Destroy view for ${controller.instance()}")
                }
            }
        )
    }

    abstract fun createBinding(inflater: LayoutInflater): VB

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
        binding = createBinding(inflater)
        return binding.root
    }

    open fun onViewCreated(view: View) {}

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        if (type.isEnter) {
            setTitle()
            setHasOptionsMenu(true)
        }

        super.onChangeStarted(handler, type)
    }

    open fun getTitle(): String? {
        return null
    }

    fun setTitle(title: String? = null) {
        var parentController = parentController
        while (parentController != null) {
            if (parentController is BaseController<*> && parentController.getTitle() != null) {
                return
            }
            parentController = parentController.parentController
        }

        (activity as? AppCompatActivity)?.supportActionBar?.title = title ?: getTitle()
    }

    private fun Controller.instance(): String {
        return "${javaClass.simpleName}@${Integer.toHexString(hashCode())}"
    }

    /**
     * Workaround for buggy menu item layout after expanding/collapsing an expandable item like a SearchView.
     * This method should be removed when fixed upstream.
     * Issue link: https://issuetracker.google.com/issues/37657375
     */
    var expandActionViewFromInteraction = false

    fun MenuItem.fixExpand(onExpand: ((MenuItem) -> Boolean)? = null, onCollapse: ((MenuItem) -> Boolean)? = null) {
        setOnActionExpandListener(
            object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                    return onExpand?.invoke(item) ?: true
                }

                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    activity?.invalidateOptionsMenu()

                    return onCollapse?.invoke(item) ?: true
                }
            }
        )

        if (expandActionViewFromInteraction) {
            expandActionViewFromInteraction = false
            expandActionView()
        }
    }

    /**
     * Workaround for menu items not disappearing when expanding an expandable item like a SearchView.
     * [expandActionViewFromInteraction] should be set to true in [onOptionsItemSelected] when the expandable item is selected
     * This method should be called as part of [MenuItem.OnActionExpandListener.onMenuItemActionExpand]
     */
    open fun invalidateMenuOnExpand(): Boolean {
        return if (expandActionViewFromInteraction) {
            activity?.invalidateOptionsMenu()
            false
        } else {
            true
        }
    }
}
