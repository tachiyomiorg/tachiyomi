package eu.kanade.tachiyomi.ui.browse.extension.details

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.os.bundleOf
import androidx.preference.Preference
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import dev.chrisbanes.insetter.applyInsetter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.EmptyPreferenceDataStore
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.minusAssign
import eu.kanade.tachiyomi.data.preference.plusAssign
import eu.kanade.tachiyomi.databinding.ExtensionDetailControllerBinding
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.getPreferenceKey
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.ToolbarLiftOnScrollController
import eu.kanade.tachiyomi.ui.base.controller.openInBrowser
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.util.preference.DSL
import eu.kanade.tachiyomi.util.preference.onChange
import eu.kanade.tachiyomi.util.preference.preferenceCategory
import eu.kanade.tachiyomi.util.preference.switchPreference
import eu.kanade.tachiyomi.util.preference.switchSettingsPreference
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import uy.kohesive.injekt.injectLazy

@SuppressLint("RestrictedApi")
class ExtensionDetailsController(bundle: Bundle? = null) :
    NucleusController<ExtensionDetailControllerBinding, ExtensionDetailsPresenter>(bundle),
    ToolbarLiftOnScrollController {

    private val preferences: PreferencesHelper by injectLazy()

    private var preferenceScreen: PreferenceScreen? = null

    constructor(pkgName: String) : this(
        bundleOf(PKGNAME_KEY to pkgName)
    )

    init {
        setHasOptionsMenu(true)
    }

    override fun createBinding(inflater: LayoutInflater): ExtensionDetailControllerBinding {
        val themedInflater = inflater.cloneInContext(getPreferenceThemeContext())
        return ExtensionDetailControllerBinding.inflate(themedInflater)
    }

    override fun createPresenter(): ExtensionDetailsPresenter {
        return ExtensionDetailsPresenter(args.getString(PKGNAME_KEY)!!)
    }

    override fun getTitle(): String? {
        return resources?.getString(R.string.label_extension_info)
    }

    @SuppressLint("PrivateResource")
    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        binding.extensionPrefsRecycler.applyInsetter {
            type(navigationBars = true) {
                padding()
            }
        }

        val extension = presenter.extension ?: return
        val context = view.context

        binding.extensionPrefsRecycler.layoutManager = LinearLayoutManager(context)
        binding.extensionPrefsRecycler.adapter = ConcatAdapter(
            ExtensionDetailsHeaderAdapter(presenter),
            initPreferencesAdapter(context, extension)
        )
    }

    private fun initPreferencesAdapter(context: Context, extension: Extension.Installed): PreferenceGroupAdapter {
        val themedContext = getPreferenceThemeContext()
        val manager = PreferenceManager(themedContext)
        manager.preferenceDataStore = EmptyPreferenceDataStore()
        val screen = manager.createPreferenceScreen(themedContext)
        preferenceScreen = screen

        val isMultiSource = extension.sources.size > 1
        val isMultiLangSingleSource = isMultiSource && extension.sources.map { it.name }.distinct().size == 1

        with(screen) {
            extension.sources
                .groupBy { (it as CatalogueSource).lang }
                .toSortedMap(compareBy { LocaleHelper.getSourceDisplayName(it, context) })
                .forEach {
                    val preferenceBlock = {
                        it.value
                            .sortedWith(compareBy({ !it.isEnabled() }, { it.name.lowercase() }))
                            .forEach { source ->
                                val sourcePrefs = mutableListOf<Preference>()

                                val block: (@DSL SwitchPreferenceCompat).() -> Unit = {
                                    key = source.getPreferenceKey()
                                    title = when {
                                        isMultiSource && !isMultiLangSingleSource -> source.toString()
                                        else -> LocaleHelper.getSourceDisplayName(it.key, context)
                                    }
                                    isPersistent = false
                                    isChecked = source.isEnabled()

                                    onChange { newValue ->
                                        val checked = newValue as Boolean
                                        toggleSource(source, checked)
                                        true
                                    }

                                    // React to enable/disable all changes
                                    preferences.disabledSources().asFlow()
                                        .onEach {
                                            val enabled = source.isEnabled()
                                            isChecked = enabled
                                            sourcePrefs.forEach { pref -> pref.isVisible = enabled }
                                        }
                                        .launchIn(viewScope)
                                }

                                // Source enable/disable
                                if (source is ConfigurableSource) {
                                    switchSettingsPreference {
                                        block()
                                        onSettingsClick = View.OnClickListener {
                                            router.pushController(
                                                SourcePreferencesController(source.id).withFadeTransaction()
                                            )
                                        }
                                    }
                                } else {
                                    switchPreference(block)
                                }
                            }
                    }

                    if (isMultiSource && !isMultiLangSingleSource) {
                        preferenceCategory {
                            title = LocaleHelper.getSourceDisplayName(it.key, context)

                            preferenceBlock()
                        }
                    } else {
                        preferenceBlock()
                    }
                }
        }

        return PreferenceGroupAdapter(screen)
    }

    override fun onDestroyView(view: View) {
        preferenceScreen = null
        super.onDestroyView(view)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.extension_details, menu)

        menu.findItem(R.id.action_history).isVisible = presenter.extension?.isUnofficial == false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_history -> openCommitHistory()
            R.id.action_enable_all -> toggleAllSources(true)
            R.id.action_disable_all -> toggleAllSources(false)
            R.id.action_open_in_settings -> openInSettings()
        }
        return super.onOptionsItemSelected(item)
    }

    fun onExtensionUninstalled() {
        router.popCurrentController()
    }

    private fun toggleAllSources(enable: Boolean) {
        presenter.extension?.sources?.forEach { toggleSource(it, enable) }
    }

    private fun toggleSource(source: Source, enable: Boolean) {
        if (enable) {
            preferences.disabledSources() -= source.id.toString()
        } else {
            preferences.disabledSources() += source.id.toString()
        }
    }

    private fun openCommitHistory() {
        val pkgName = presenter.extension!!.pkgName.substringAfter("eu.kanade.tachiyomi.extension.")
        val pkgFactory = presenter.extension!!.pkgFactory
        val url = when {
            !pkgFactory.isNullOrEmpty() -> "$URL_EXTENSION_COMMITS/multisrc/src/main/java/eu/kanade/tachiyomi/multisrc/$pkgFactory"
            else -> "$URL_EXTENSION_COMMITS/src/${pkgName.replace(".", "/")}"
        }
        openInBrowser(url)
    }

    private fun openInSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", presenter.pkgName, null)
        }
        startActivity(intent)
    }

    private fun Source.isEnabled(): Boolean {
        return id.toString() !in preferences.disabledSources().get()
    }

    private fun getPreferenceThemeContext(): Context {
        val tv = TypedValue()
        activity!!.theme.resolveAttribute(R.attr.preferenceTheme, tv, true)
        return ContextThemeWrapper(activity, tv.resourceId)
    }

    private companion object {
        const val PKGNAME_KEY = "pkg_name"

        private const val URL_EXTENSION_COMMITS = "https://github.com/tachiyomiorg/tachiyomi-extensions/commits/master"
    }
}
