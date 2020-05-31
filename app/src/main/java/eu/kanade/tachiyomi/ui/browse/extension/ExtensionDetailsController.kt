package eu.kanade.tachiyomi.ui.browse.extension

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.preference.DialogPreference
import androidx.preference.EditTextPreference
import androidx.preference.EditTextPreferenceDialogController
import androidx.preference.ListPreference
import androidx.preference.ListPreferenceDialogController
import androidx.preference.MultiSelectListPreference
import androidx.preference.MultiSelectListPreferenceDialogController
import androidx.preference.Preference
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import androidx.recyclerview.widget.LinearLayoutManager
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.EmptyPreferenceDataStore
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.SharedPreferencesDataStore
import eu.kanade.tachiyomi.databinding.ExtensionDetailControllerBinding
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.util.preference.checkBoxPreference
import eu.kanade.tachiyomi.util.preference.onChange
import eu.kanade.tachiyomi.util.preference.preference
import eu.kanade.tachiyomi.util.preference.preferenceCategory
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.view.visible
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.android.view.clicks
import uy.kohesive.injekt.injectLazy

@SuppressLint("RestrictedApi")
class ExtensionDetailsController(bundle: Bundle? = null) :
    NucleusController<ExtensionDetailControllerBinding, ExtensionDetailsPresenter>(bundle),
    PreferenceManager.OnDisplayPreferenceDialogListener,
    DialogPreference.TargetFragment {

    private val preferences: PreferencesHelper by injectLazy()

    private var preferenceScreen: PreferenceScreen? = null
    private var lastOpenPreferencePosition: Int? = null

    constructor(pkgName: String) : this(
        Bundle().apply {
            putString(PKGNAME_KEY, pkgName)
        }
    )

    init {
        setHasOptionsMenu(true)
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        val themedInflater = inflater.cloneInContext(getPreferenceThemeContext())
        binding = ExtensionDetailControllerBinding.inflate(themedInflater)
        return binding.root
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

        val extension = presenter.extension ?: return
        val context = view.context

        extension.getApplicationIcon(context)?.let { binding.extensionIcon.setImageDrawable(it) }
        binding.extensionTitle.text = extension.name
        binding.extensionVersion.text = context.getString(R.string.ext_version_info, extension.versionName)
        binding.extensionLang.text = context.getString(R.string.ext_language_info, LocaleHelper.getSourceDisplayName(extension.lang, context))
        binding.extensionPkg.text = extension.pkgName

        binding.extensionUninstallButton.clicks()
            .onEach { presenter.uninstallExtension() }
            .launchIn(scope)

        if (extension.isObsolete) {
            binding.extensionWarningBanner.visible()
            binding.extensionWarningBanner.setText(R.string.obsolete_extension_message)
        }

        if (extension.isUnofficial) {
            binding.extensionWarningBanner.visible()
            binding.extensionWarningBanner.setText(R.string.unofficial_extension_message)
        }

        initPreferences(context, extension)
    }

    private fun initPreferences(context: Context, extension: Extension.Installed) {
        val themedContext by lazy { getPreferenceThemeContext() }
        val manager = PreferenceManager(themedContext)
        manager.preferenceDataStore = EmptyPreferenceDataStore()
        manager.onDisplayPreferenceDialogListener = this
        val screen = manager.createPreferenceScreen(themedContext)
        preferenceScreen = screen

        with(screen) {
            extension.sources
                .groupBy { (it as CatalogueSource).lang }
                .toSortedMap(compareBy { LocaleHelper.getSourceDisplayName(it, context) })
                .forEach {
                    preferenceCategory {
                        title = LocaleHelper.getSourceDisplayName(it.key, context)
                        it.value
                            .sortedWith(compareBy({ !it.isEnabled() }, { it.name }))
                            .forEach { source ->
                                val sourcePrefs = mutableListOf<Preference>()

                                // Source enable/disable
                                checkBoxPreference {
                                    key = getSourceKey(source.id)
                                    title = source.toString()
                                    isPersistent = false
                                    isChecked = source.isEnabled()

                                    onChange { newValue ->
                                        val checked = newValue as Boolean
                                        toggleSource(source, checked)
                                        true
                                    }

                                    // React to enable/disable all changes
                                    preferences.hiddenCatalogues().asFlow()
                                        .onEach {
                                            val enabled = source.isEnabled()
                                            isChecked = enabled
                                            sourcePrefs.forEach { pref -> pref.isVisible = enabled }
                                        }
                                        .launchIn(scope)
                                }

                                // Source preferences
                                if (source is ConfigurableSource) {
                                    // TODO
                                    val dataStore = SharedPreferencesDataStore(/*if (source is HttpSource) {
                                        source.preferences
                                    } else {*/
                                        context.getSharedPreferences(getSourceKey(source.id), Context.MODE_PRIVATE)
                                        /*}*/
                                    )

                                    val newScreen = screen.preferenceManager.createPreferenceScreen(context)
                                    source.setupPreferenceScreen(newScreen)

                                    // Reparent the preferences
                                    while (newScreen.preferenceCount != 0) {
                                        val pref = newScreen.getPreference(0)
                                        sourcePrefs.add(pref)

                                        pref.preferenceDataStore = dataStore
                                        pref.order = Int.MAX_VALUE // reset to default order
                                        pref.isVisible = source.isEnabled()

                                        newScreen.removePreference(pref)
                                        screen.addPreference(pref)
                                    }
                                }
                            }
                    }
                }
        }

        binding.extensionPrefsRecycler.layoutManager = LinearLayoutManager(context)
        binding.extensionPrefsRecycler.adapter = PreferenceGroupAdapter(screen)
        binding.extensionPrefsRecycler.addItemDecoration(DividerItemDecoration(context, VERTICAL))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        lastOpenPreferencePosition?.let { outState.putInt(LASTOPENPREFERENCE_KEY, it) }
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        lastOpenPreferencePosition = savedInstanceState.get(LASTOPENPREFERENCE_KEY) as? Int
    }

    override fun onDestroyView(view: View) {
        preferenceScreen = null
        super.onDestroyView(view)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.extension_details, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_enable_all -> toggleAllSources(true)
            R.id.action_disable_all -> toggleAllSources(false)
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
        val current = preferences.hiddenCatalogues().get()

        preferences.hiddenCatalogues().set(
            if (enable) {
                current - source.id.toString()
            } else {
                current + source.id.toString()
            }
        )
    }

    private fun Source.isEnabled(): Boolean {
        return id.toString() !in preferences.hiddenCatalogues().get()
    }

    private fun getSourceKey(sourceId: Long): String {
        return "source_$sourceId"
    }

    private fun getPreferenceThemeContext(): Context {
        val tv = TypedValue()
        activity!!.theme.resolveAttribute(R.attr.preferenceTheme, tv, true)
        return ContextThemeWrapper(activity, tv.resourceId)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (!isAttached) return

        val screen = preference.parent!!

        lastOpenPreferencePosition = (0 until screen.preferenceCount).indexOfFirst {
            screen.getPreference(it) === preference
        }

        val f = when (preference) {
            is EditTextPreference ->
                EditTextPreferenceDialogController
                    .newInstance(preference.getKey())
            is ListPreference ->
                ListPreferenceDialogController
                    .newInstance(preference.getKey())
            is MultiSelectListPreference ->
                MultiSelectListPreferenceDialogController
                    .newInstance(preference.getKey())
            else -> throw IllegalArgumentException(
                "Tried to display dialog for unknown " +
                    "preference type. Did you forget to override onDisplayPreferenceDialog()?"
            )
        }
        f.targetController = this
        f.showDialog(router)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Preference> findPreference(key: CharSequence): T? {
        // We track [lastOpenPreferencePosition] when displaying the dialog
        // [key] isn't useful since there may be duplicates
        return preferenceScreen!!.getPreference(lastOpenPreferencePosition!!) as T
    }

    private companion object {
        const val PKGNAME_KEY = "pkg_name"
        const val LASTOPENPREFERENCE_KEY = "last_open_preference"
    }
}
