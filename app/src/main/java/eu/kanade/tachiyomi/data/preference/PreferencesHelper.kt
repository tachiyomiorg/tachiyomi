package eu.kanade.tachiyomi.data.preference

import android.content.Context
import android.os.Environment
import android.preference.PreferenceManager
import com.f2prateek.rx.preferences.Preference
import com.f2prateek.rx.preferences.RxSharedPreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.mangasync.base.MangaSyncService
import eu.kanade.tachiyomi.data.source.base.Source
import java.io.File
import java.io.IOException

fun <T> Preference<T>.getOrDefault(): T = get() ?: defaultValue()!!

class PreferencesHelper(private val context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val rxPrefs = RxSharedPreferences.create(prefs)

    private val defaultDownloadsDir: File

    init {
        defaultDownloadsDir = File(Environment.getExternalStorageDirectory().absolutePath +
                File.separator + context.getString(R.string.app_name), "downloads")

        // Create default directory
        if (downloadsDirectory == defaultDownloadsDir.absolutePath && !defaultDownloadsDir.exists()) {
            defaultDownloadsDir.mkdirs()
        }

        // Don't display downloaded chapters in gallery apps creating a ".nomedia" file
        try {
            File(downloadsDirectory, ".nomedia").createNewFile()
        } catch (e: IOException) {
            /* Ignore */
        }

    }

    companion object {

        const val SOURCE_ACCOUNT_USERNAME = "pref_source_username_"
        const val SOURCE_ACCOUNT_PASSWORD = "pref_source_password_"
        const val MANGASYNC_ACCOUNT_USERNAME = "pref_mangasync_username_"
        const val MANGASYNC_ACCOUNT_PASSWORD = "pref_mangasync_password_"

        fun getLibraryUpdateInterval(context: Context): Int {
            return PreferenceManager.getDefaultSharedPreferences(context).getInt(
                    context.getString(R.string.pref_library_update_interval_key), 0)
        }
    }

    private fun getKey(keyResource: Int): String {
        return context.getString(keyResource)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun rotation(): Preference<Int> {
        return rxPrefs.getInteger(getKey(R.string.pref_rotation_type_key), 1)
    }

    fun enableTransitions(): Preference<Boolean> {
        return rxPrefs.getBoolean(getKey(R.string.pref_enable_transitions_key), true)
    }

    fun showPageNumber(): Preference<Boolean> {
        return rxPrefs.getBoolean(getKey(R.string.pref_show_page_number_key), true)
    }

    fun hideStatusBar(): Preference<Boolean> {
        return rxPrefs.getBoolean(getKey(R.string.pref_hide_status_bar_key), true)
    }

    fun keepScreenOn(): Preference<Boolean> {
        return rxPrefs.getBoolean(getKey(R.string.pref_keep_screen_on_key), true)
    }

    fun customBrightness(): Preference<Boolean> {
        return rxPrefs.getBoolean(getKey(R.string.pref_custom_brightness_key), false)
    }

    fun customBrightnessValue(): Preference<Float> {
        return rxPrefs.getFloat(getKey(R.string.pref_custom_brightness_value_key), 0f)
    }

    val defaultViewer: Int
        get() = prefs.getInt(getKey(R.string.pref_default_viewer_key), 1)

    fun imageScaleType(): Preference<Int> {
        return rxPrefs.getInteger(getKey(R.string.pref_image_scale_type_key), 1)
    }

    fun imageDecoder(): Preference<Int> {
        return rxPrefs.getInteger(getKey(R.string.pref_image_decoder_key), 0)
    }

    fun zoomStart(): Preference<Int> {
        return rxPrefs.getInteger(getKey(R.string.pref_zoom_start_key), 1)
    }

    fun readerTheme(): Preference<Int> {
        return rxPrefs.getInteger(getKey(R.string.pref_reader_theme_key), 0)
    }

    fun portraitColumns(): Preference<Int> {
        return rxPrefs.getInteger(getKey(R.string.pref_library_columns_portrait_key), 0)
    }

    fun landscapeColumns(): Preference<Int> {
        return rxPrefs.getInteger(getKey(R.string.pref_library_columns_landscape_key), 0)
    }

    fun updateOnlyNonCompleted(): Boolean {
        return prefs.getBoolean(getKey(R.string.pref_update_only_non_completed_key), false)
    }

    fun autoUpdateMangaSync(): Boolean {
        return prefs.getBoolean(getKey(R.string.pref_auto_update_manga_sync_key), true)
    }

    fun askUpdateMangaSync(): Boolean {
        return prefs.getBoolean(getKey(R.string.pref_ask_update_manga_sync_key), false)
    }

    fun lastUsedCatalogueSource(): Preference<Int> {
        return rxPrefs.getInteger(getKey(R.string.pref_last_catalogue_source_key), -1)
    }

    fun seamlessMode(): Boolean {
        return prefs.getBoolean(getKey(R.string.pref_seamless_mode_key), true)
    }

    fun catalogueAsList(): Preference<Boolean> {
        return rxPrefs.getBoolean(getKey(R.string.pref_display_catalogue_as_list), false)
    }

    fun enabledLanguages(): Preference<MutableSet<String>> {
        return rxPrefs.getStringSet(getKey(R.string.pref_source_languages), setOf("EN"))
    }

    fun getSourceUsername(source: Source): String {
        return prefs.getString(SOURCE_ACCOUNT_USERNAME + source.id, "")
    }

    fun getSourcePassword(source: Source): String {
        return prefs.getString(SOURCE_ACCOUNT_PASSWORD + source.id, "")
    }

    fun setSourceCredentials(source: Source, username: String, password: String) {
        prefs.edit()
                .putString(SOURCE_ACCOUNT_USERNAME + source.id, username)
                .putString(SOURCE_ACCOUNT_PASSWORD + source.id, password)
                .apply()
    }

    fun getMangaSyncUsername(sync: MangaSyncService): String {
        return prefs.getString(MANGASYNC_ACCOUNT_USERNAME + sync.id, "")
    }

    fun getMangaSyncPassword(sync: MangaSyncService): String {
        return prefs.getString(MANGASYNC_ACCOUNT_PASSWORD + sync.id, "")
    }

    fun setMangaSyncCredentials(sync: MangaSyncService, username: String, password: String) {
        prefs.edit()
                .putString(MANGASYNC_ACCOUNT_USERNAME + sync.id, username)
                .putString(MANGASYNC_ACCOUNT_PASSWORD + sync.id, password)
                .apply()
    }

    var downloadsDirectory: String
        get() = prefs.getString(getKey(R.string.pref_download_directory_key), defaultDownloadsDir.absolutePath)
        set(path) = prefs.edit().putString(getKey(R.string.pref_download_directory_key), path).apply()

    fun downloadThreads(): Preference<Int> {
        return rxPrefs.getInteger(getKey(R.string.pref_download_slots_key), 1)
    }

    fun downloadOnlyOverWifi(): Boolean {
        return prefs.getBoolean(getKey(R.string.pref_download_only_over_wifi_key), true)
    }

    fun libraryUpdateInterval(): Preference<Int> {
        return rxPrefs.getInteger(getKey(R.string.pref_library_update_interval_key), 0)
    }

    fun filterDownloaded(): Preference<Boolean> {
        return rxPrefs.getBoolean(getKey(R.string.pref_filter_downloaded), false)
    }

    fun filterUnread(): Preference<Boolean> {
        return rxPrefs.getBoolean(getKey(R.string.pref_filter_unread), false)
    }

}
