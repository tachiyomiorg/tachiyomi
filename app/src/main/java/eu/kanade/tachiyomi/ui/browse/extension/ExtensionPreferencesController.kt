package eu.kanade.tachiyomi.ui.browse.extension

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import eu.kanade.tachiyomi.data.preference.SharedPreferencesDataStore
import eu.kanade.tachiyomi.databinding.ExtensionPreferencesControllerBinding
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.util.preference.preferenceCategory
import timber.log.Timber

@SuppressLint("RestrictedApi")
class ExtensionPreferencesController(bundle: Bundle? = null) :
    NucleusController<ExtensionPreferencesControllerBinding, ExtensionPreferencesPresenter>(bundle),
    PreferenceManager.OnDisplayPreferenceDialogListener,
    DialogPreference.TargetFragment {

    private var lastOpenPreferencePosition: Int? = null

    private var preferenceScreen: PreferenceScreen? = null

    constructor(pkgName: String) : this(
        Bundle().apply {
            putString(PKGNAME_KEY, pkgName)
        }
    )

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        val themedInflater = inflater.cloneInContext(getPreferenceThemeContext())
        binding = ExtensionPreferencesControllerBinding.inflate(themedInflater)
        return binding.root
    }

    override fun createPresenter(): ExtensionPreferencesPresenter {
        return ExtensionPreferencesPresenter(args.getString(PKGNAME_KEY)!!)
    }

    override fun getTitle(): String? {
        return resources?.getString(R.string.label_extension_info)
    }

    @SuppressLint("PrivateResource")
    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        val extension = presenter.extension ?: return
        val context = view.context

        val themedContext by lazy { getPreferenceThemeContext() }
        val manager = PreferenceManager(themedContext)
        manager.preferenceDataStore = EmptyPreferenceDataStore()
        manager.onDisplayPreferenceDialogListener = this
        val screen = manager.createPreferenceScreen(themedContext)
        preferenceScreen = screen

        val multiSource = extension.sources.size > 1

        extension.sources
            .filterIsInstance<ConfigurableSource>()
            .forEach { source ->
                try {
                    addPreferencesForSource(screen, source, multiSource)
                } catch (e: AbstractMethodError) {
                    Timber.e("Source did not implement [addPreferencesForSource]: ${source.name}")
                }
            }

        manager.setPreferences(screen)

        binding.extensionPrefsRecycler.layoutManager = LinearLayoutManager(context)
        binding.extensionPrefsRecycler.adapter = PreferenceGroupAdapter(screen)
        binding.extensionPrefsRecycler.addItemDecoration(DividerItemDecoration(context, VERTICAL))

        if (screen.preferenceCount == 0) {
            binding.extensionPrefsEmptyView.show(R.string.ext_empty_preferences)
        }
    }

    override fun onDestroyView(view: View) {
        preferenceScreen = null
        super.onDestroyView(view)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        lastOpenPreferencePosition?.let { outState.putInt(LASTOPENPREFERENCE_KEY, it) }
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        lastOpenPreferencePosition = savedInstanceState.get(LASTOPENPREFERENCE_KEY) as? Int
    }

    private fun addPreferencesForSource(screen: PreferenceScreen, source: Source, multiSource: Boolean) {
        val context = screen.context

        // TODO
        val dataStore = SharedPreferencesDataStore(/*if (source is HttpSource) {
            source.preferences
        } else {*/
            context.getSharedPreferences("source_${source.id}", Context.MODE_PRIVATE)
            /*}*/
        )

        if (source is ConfigurableSource) {
            if (multiSource) {
                screen.preferenceCategory {
                    title = source.toString()
                }
            }

            val newScreen = screen.preferenceManager.createPreferenceScreen(context)
            source.setupPreferenceScreen(newScreen)

            // Reparent the preferences
            while (newScreen.preferenceCount != 0) {
                val pref = newScreen.getPreference(0)
                pref.isIconSpaceReserved = false
                pref.preferenceDataStore = dataStore
                pref.order = Int.MAX_VALUE // reset to default order

                newScreen.removePreference(pref)
                screen.addPreference(pref)
            }
        }
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
