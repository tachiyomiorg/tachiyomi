package eu.kanade.tachiyomi.data.preference

import android.content.Context
import eu.kanade.tachiyomi.R

/**
 * This class stores the keys for the preferences in the application. Most of them are defined
 * in the file "keys.xml". By using this class we can define preferences in one place and get them
 * referenced here.
 */
@Suppress("HasPlatformType")
class PreferenceKeys(context: Context) {

    val theme = context.getString(R.string.pref_theme_key)

    val rotation = context.getString(R.string.pref_rotation_type_key)

    val enableTransitions = context.getString(R.string.pref_enable_transitions_key)

    val showPageNumber = context.getString(R.string.pref_show_page_number_key)

    val fullscreen = context.getString(R.string.pref_fullscreen_key)

    val keepScreenOn = context.getString(R.string.pref_keep_screen_on_key)

    val customBrightness = context.getString(R.string.pref_custom_brightness_key)

    val customBrightnessValue = context.getString(R.string.pref_custom_brightness_value_key)

    val colorFilter = context.getString(R.string.pref_color_filter_key)

    val colorFilterValue = context.getString(R.string.pref_color_filter_value_key)

    val defaultViewer = context.getString(R.string.pref_default_viewer_key)

    val imageScaleType = context.getString(R.string.pref_image_scale_type_key)

    val imageDecoder = context.getString(R.string.pref_image_decoder_key)

    val zoomStart = context.getString(R.string.pref_zoom_start_key)

    val readerTheme = context.getString(R.string.pref_reader_theme_key)

    val readWithTapping = context.getString(R.string.pref_read_with_tapping_key)

    val readWithVolumeKeys = context.getString(R.string.pref_read_with_volume_keys_key)

    val portraitColumns = context.getString(R.string.pref_library_columns_portrait_key)

    val landscapeColumns = context.getString(R.string.pref_library_columns_landscape_key)

    val updateOnlyNonCompleted = context.getString(R.string.pref_update_only_non_completed_key)

    val autoUpdateTrack = context.getString(R.string.pref_auto_update_manga_sync_key)

    val askUpdateTrack = context.getString(R.string.pref_ask_update_manga_sync_key)

    val lastUsedCatalogueSource = context.getString(R.string.pref_last_catalogue_source_key)

    val lastUsedCategory = context.getString(R.string.pref_last_used_category_key)

    val catalogueAsList = context.getString(R.string.pref_display_catalogue_as_list)

    val enabledLanguages = context.getString(R.string.pref_source_languages)

    val downloadsDirectory = context.getString(R.string.pref_download_directory_key)

    val downloadThreads = context.getString(R.string.pref_download_slots_key)

    val downloadOnlyOverWifi = context.getString(R.string.pref_download_only_over_wifi_key)

    val removeAfterReadSlots = context.getString(R.string.pref_remove_after_read_slots_key)

    val removeAfterMarkedAsRead = context.getString(R.string.pref_remove_after_marked_as_read_key)

    val libraryUpdateInterval = context.getString(R.string.pref_library_update_interval_key)

    val libraryUpdateRestriction = context.getString(R.string.pref_library_update_restriction_key)

    val libraryUpdateCategories = context.getString(R.string.pref_library_update_categories_key)

    val filterDownloaded = context.getString(R.string.pref_filter_downloaded_key)

    val filterUnread = context.getString(R.string.pref_filter_unread_key)

    val librarySortingMode = context.getString(R.string.pref_library_sorting_mode_key)

    val automaticUpdates = context.getString(R.string.pref_enable_automatic_updates_key)

    val startScreen = context.getString(R.string.pref_start_screen_key)

    val downloadNew = context.getString(R.string.pref_download_new_key)

    fun sourceUsername(sourceId: Long) = "pref_source_username_$sourceId"

    fun sourcePassword(sourceId: Long) = "pref_source_password_$sourceId"

    fun trackUsername(syncId: Int) = "pref_mangasync_username_$syncId"

    fun trackPassword(syncId: Int) = "pref_mangasync_password_$syncId"

    fun trackToken(syncId: Int) = "track_token_$syncId"

    val libraryAsList = context.getString(R.string.pref_display_library_as_list)

    val lang = context.getString(R.string.pref_language_key)

}
