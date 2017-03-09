package eu.kanade.tachiyomi.data.preference

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.preference.PreferenceManager
import com.f2prateek.rx.preferences.Preference
import com.f2prateek.rx.preferences.RxSharedPreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.source.Source
import java.io.File

fun <T> Preference<T>.getOrDefault(): T = get() ?: defaultValue()!!

fun Preference<Boolean>.invert(): Boolean = getOrDefault().let { set(!it); !it }

class PreferencesHelper(val context: Context) {

    val keys = PreferenceKeys(context)

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val rxPrefs = RxSharedPreferences.create(prefs)

    private val defaultDownloadsDir = Uri.fromFile(
            File(Environment.getExternalStorageDirectory().absolutePath + File.separator +
                    context.getString(R.string.app_name), "downloads"))

    fun startScreen() = prefs.getInt(keys.startScreen, 1)

    fun clear() = prefs.edit().clear().apply()

    fun theme() = prefs.getInt(keys.theme, 1)

    fun rotation() = rxPrefs.getInteger(keys.rotation, 1)

    fun pageTransitions() = rxPrefs.getBoolean(keys.enableTransitions, true)

    fun showPageNumber() = rxPrefs.getBoolean(keys.showPageNumber, true)

    fun fullscreen() = rxPrefs.getBoolean(keys.fullscreen, true)

    fun keepScreenOn() = rxPrefs.getBoolean(keys.keepScreenOn, true)

    fun customBrightness() = rxPrefs.getBoolean(keys.customBrightness, false)

    fun customBrightnessValue() = rxPrefs.getInteger(keys.customBrightnessValue, 0)

    fun colorFilter() = rxPrefs.getBoolean(keys.colorFilter, false)

    fun colorFilterValue() = rxPrefs.getInteger(keys.colorFilterValue, 0)

    fun defaultViewer() = prefs.getInt(keys.defaultViewer, 1)

    fun imageScaleType() = rxPrefs.getInteger(keys.imageScaleType, 1)

    fun imageDecoder() = rxPrefs.getInteger(keys.imageDecoder, 0)

    fun zoomStart() = rxPrefs.getInteger(keys.zoomStart, 1)

    fun readerTheme() = rxPrefs.getInteger(keys.readerTheme, 0)

    fun cropBorders() = rxPrefs.getBoolean(keys.cropBorders, false)

    fun readWithTapping() = rxPrefs.getBoolean(keys.readWithTapping, true)

    fun readWithVolumeKeys() = rxPrefs.getBoolean(keys.readWithVolumeKeys, false)

    fun portraitColumns() = rxPrefs.getInteger(keys.portraitColumns, 0)

    fun landscapeColumns() = rxPrefs.getInteger(keys.landscapeColumns, 0)

    fun updateOnlyNonCompleted() = prefs.getBoolean(keys.updateOnlyNonCompleted, false)

    fun autoUpdateTrack() = prefs.getBoolean(keys.autoUpdateTrack, true)

    fun askUpdateTrack() = prefs.getBoolean(keys.askUpdateTrack, false)

    fun lastUsedCatalogueSource() = rxPrefs.getLong(keys.lastUsedCatalogueSource, -1)

    fun lastUsedCategory() = rxPrefs.getInteger(keys.lastUsedCategory, 0)

    fun lastVersionCode() = rxPrefs.getInteger("last_version_code", 0)

    fun catalogueAsList() = rxPrefs.getBoolean(keys.catalogueAsList, false)

    fun enabledLanguages() = rxPrefs.getStringSet(keys.enabledLanguages, setOf("en"))

    fun sourceUsername(source: Source) = prefs.getString(keys.sourceUsername(source.id), "")

    fun sourcePassword(source: Source) = prefs.getString(keys.sourcePassword(source.id), "")

    fun setSourceCredentials(source: Source, username: String, password: String) {
        prefs.edit()
                .putString(keys.sourceUsername(source.id), username)
                .putString(keys.sourcePassword(source.id), password)
                .apply()
    }

    fun trackUsername(sync: TrackService) = prefs.getString(keys.trackUsername(sync.id), "")

    fun trackPassword(sync: TrackService) = prefs.getString(keys.trackPassword(sync.id), "")

    fun setTrackCredentials(sync: TrackService, username: String, password: String) {
        prefs.edit()
                .putString(keys.trackUsername(sync.id), username)
                .putString(keys.trackPassword(sync.id), password)
                .apply()
    }

    fun trackToken(sync: TrackService) = rxPrefs.getString(keys.trackToken(sync.id), "")

    fun anilistScoreType() = rxPrefs.getInteger("anilist_score_type", 0)

    fun downloadsDirectory() = rxPrefs.getString(keys.downloadsDirectory, defaultDownloadsDir.toString())

    fun downloadThreads() = rxPrefs.getInteger(keys.downloadThreads, 1)

    fun downloadOnlyOverWifi() = prefs.getBoolean(keys.downloadOnlyOverWifi, true)

    fun removeAfterReadSlots() = prefs.getInt(keys.removeAfterReadSlots, -1)

    fun removeAfterMarkedAsRead() = prefs.getBoolean(keys.removeAfterMarkedAsRead, false)

    fun libraryUpdateInterval() = rxPrefs.getInteger(keys.libraryUpdateInterval, 0)

    fun libraryUpdateRestriction() = prefs.getStringSet(keys.libraryUpdateRestriction, emptySet())

    fun libraryUpdateCategories() = rxPrefs.getStringSet(keys.libraryUpdateCategories, emptySet())

    fun libraryAsList() = rxPrefs.getBoolean(keys.libraryAsList, false)

    fun filterDownloaded() = rxPrefs.getBoolean(keys.filterDownloaded, false)

    fun filterUnread() = rxPrefs.getBoolean(keys.filterUnread, false)

    fun librarySortingMode() = rxPrefs.getInteger(keys.librarySortingMode, 0)

    fun librarySortingAscending() = rxPrefs.getBoolean("library_sorting_ascending", true)

    fun automaticUpdates() = prefs.getBoolean(keys.automaticUpdates, false)

    fun hiddenCatalogues() = rxPrefs.getStringSet("hidden_catalogues", emptySet())

    fun downloadNew() = rxPrefs.getBoolean(keys.downloadNew, false)

    fun downloadNewCategories() = rxPrefs.getStringSet(keys.downloadNewCategories, emptySet())

    fun lang() = prefs.getString(keys.lang, "")

}
