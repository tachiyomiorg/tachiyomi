package eu.kanade.tachiyomi.ui.setting

import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v4.content.ContextCompat
import android.support.v7.preference.XpPreferenceFragment
import android.view.View
import eu.kanade.tachiyomi.R
import net.xpece.android.support.preference.PreferenceIconHelper
import net.xpece.android.support.preference.PreferenceScreenNavigationStrategy
import net.xpece.android.support.preference.Util
import rx.subscriptions.CompositeSubscription

open class SettingsFragment : XpPreferenceFragment() {

    companion object {
        fun newInstance(rootKey: String?): SettingsFragment {
            val args = Bundle()
            args.putString(XpPreferenceFragment.ARG_PREFERENCE_ROOT, rootKey)
            return SettingsFragment().apply { arguments = args }
        }
    }

    lateinit var subscriptions: CompositeSubscription

    private val iconTint by lazy { ContextCompat.getColorStateList(
            context, Util.resolveResourceId(context, R.attr.colorAccent, 0))
    }

    override final fun onCreatePreferences2(savedState: Bundle?, rootKey: String?) {
        subscriptions = CompositeSubscription()

        addPreferencesFromResource(R.xml.pref_general)
        addPreferencesFromResource(R.xml.pref_reader)
        addPreferencesFromResource(R.xml.pref_downloads)
        addPreferencesFromResource(R.xml.pref_sources)
        addPreferencesFromResource(R.xml.pref_sync)
        addPreferencesFromResource(R.xml.pref_library_sync)
        addPreferencesFromResource(R.xml.pref_advanced)
        addPreferencesFromResource(R.xml.pref_about)

        // Add an icon to each subscreen
        for ((screen, drawable) in getSubscreenIcons()) {
            val icon = VectorDrawableCompat.create(resources, drawable, context.theme) ?: continue

            PreferenceIconHelper(findPreference(screen)).apply {
                isIconPaddingEnabled = true
                setIcon(icon)
                tintList = iconTint
                isIconTintEnabled = true
            }
        }

        // Setup root preference title.
        preferenceScreen.title = activity.title

        PreferenceScreenNavigationStrategy.ReplaceFragment.onCreatePreferences(this, rootKey)
    }

    @CallSuper
    override fun onViewCreated(view: View, savedState: Bundle?) {
        listView.isFocusable = false
    }

    override fun onStart() {
        super.onStart()
        activity.title = preferenceScreen.title
    }

    override fun onDestroyView() {
        subscriptions.unsubscribe()
        super.onDestroyView()
    }

    private fun getSubscreenIcons() = listOf(
            "general_screen" to R.drawable.ic_tune_black_24dp,
            "reader_screen" to R.drawable.ic_chrome_reader_mode_black_24dp,
            "downloads_screen" to R.drawable.ic_file_download_black_24dp,
            "sources_screen" to R.drawable.ic_language_black_24dp,
            "sync_screen" to R.drawable.ic_sync_black_24dp,
            "library_sync_screen" to R.drawable.ic_cloud_white_24dp,
            "advanced_screen" to R.drawable.ic_code_black_24dp,
            "about_screen" to R.drawable.ic_help_black_24dp
    )

}