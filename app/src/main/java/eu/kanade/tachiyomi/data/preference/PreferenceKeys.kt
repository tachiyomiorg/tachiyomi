package eu.kanade.tachiyomi.data.preference

/**
 * This class stores the keys for the preferences in the application.
 */
object PreferenceKeys {

    const val themeMode = "pref_theme_mode_key"

    const val appTheme = "pref_app_theme"

    const val themeDarkAmoled = "pref_theme_dark_amoled_key"

    const val confirmExit = "pref_confirm_exit"

    const val hideBottomBarOnScroll = "pref_hide_bottom_bar_on_scroll"

    const val sideNavIconAlignment = "pref_side_nav_icon_alignment"

    const val enableTransitions = "pref_enable_transitions_key"

    const val doubleTapAnimationSpeed = "pref_double_tap_anim_speed"

    const val showPageNumber = "pref_show_page_number_key"

    const val dualPageSplitPaged = "pref_dual_page_split"

    const val dualPageSplitWebtoon = "pref_dual_page_split_webtoon"

    const val dualPageInvertPaged = "pref_dual_page_invert"

    const val dualPageInvertWebtoon = "pref_dual_page_invert_webtoon"

    const val showReadingMode = "pref_show_reading_mode"

    const val trueColor = "pref_true_color_key"

    const val fullscreen = "fullscreen"

    const val cutoutShort = "cutout_short"

    const val keepScreenOn = "pref_keep_screen_on_key"

    const val customBrightness = "pref_custom_brightness_key"

    const val customBrightnessValue = "custom_brightness_value"

    const val colorFilter = "pref_color_filter_key"

    const val colorFilterValue = "color_filter_value"

    const val colorFilterMode = "color_filter_mode"

    const val grayscale = "pref_grayscale"

    const val defaultReadingMode = "pref_default_reading_mode_key"

    const val defaultOrientationType = "pref_default_orientation_type_key"

    const val imageScaleType = "pref_image_scale_type_key"

    const val zoomStart = "pref_zoom_start_key"

    const val readerTheme = "pref_reader_theme_key"

    const val cropBorders = "crop_borders"

    const val cropBordersWebtoon = "crop_borders_webtoon"

    const val readWithTapping = "reader_tap"

    const val pagerNavInverted = "reader_tapping_inverted"

    const val webtoonNavInverted = "reader_tapping_inverted_webtoon"

    const val readWithLongTap = "reader_long_tap"

    const val readWithVolumeKeys = "reader_volume_keys"

    const val readWithVolumeKeysInverted = "reader_volume_keys_inverted"

    const val navigationModePager = "reader_navigation_mode_pager"

    const val navigationModeWebtoon = "reader_navigation_mode_webtoon"

    const val showNavigationOverlayNewUser = "reader_navigation_overlay_new_user"

    const val showNavigationOverlayOnStart = "reader_navigation_overlay_on_start"

    const val webtoonSidePadding = "webtoon_side_padding"

    const val portraitColumns = "pref_library_columns_portrait_key"

    const val landscapeColumns = "pref_library_columns_landscape_key"

    const val jumpToChapters = "jump_to_chapters"

    const val updateOnlyNonCompleted = "pref_update_only_non_completed_key"

    const val autoUpdateTrack = "pref_auto_update_manga_sync_key"

    const val autoAddTrack = "pref_auto_add_track_key"

    const val lastUsedSource = "last_catalogue_source"

    const val lastUsedCategory = "last_used_category"

    const val sourceDisplayMode = "pref_display_mode_catalogue"

    const val enabledLanguages = "source_languages"

    const val backupDirectory = "backup_directory"

    const val downloadsDirectory = "download_directory"

    const val downloadOnlyOverWifi = "pref_download_only_over_wifi_key"

    const val folderPerManga = "create_folder_per_manga"

    const val numberOfBackups = "backup_slots"

    const val backupInterval = "backup_interval"

    const val removeAfterReadSlots = "remove_after_read_slots"

    const val removeAfterMarkedAsRead = "pref_remove_after_marked_as_read_key"

    const val removeBookmarkedChapters = "pref_remove_bookmarked"

    const val libraryUpdateInterval = "pref_library_update_interval_key"

    const val libraryUpdateRestriction = "library_update_restriction"

    const val libraryUpdateCategories = "library_update_categories"
    const val libraryUpdateCategoriesExclude = "library_update_categories_exclude"

    const val libraryUpdatePrioritization = "library_update_prioritization"

    const val downloadedOnly = "pref_downloaded_only"

    const val filterDownloaded = "pref_filter_library_downloaded"

    const val filterUnread = "pref_filter_library_unread"

    const val filterCompleted = "pref_filter_library_completed"

    const val filterTracked = "pref_filter_library_tracked"

    const val librarySortingMode = "library_sorting_mode"
    const val librarySortingDirection = "library_sorting_ascending"

    const val automaticExtUpdates = "automatic_ext_updates"

    const val showNsfwSource = "show_nsfw_source"
    const val showNsfwExtension = "show_nsfw_extension"
    const val labelNsfwExtension = "label_nsfw_extension"

    const val startScreen = "start_screen"

    const val useAuthenticator = "use_biometric_lock"

    const val lockAppAfter = "lock_app_after"

    const val lastAppUnlock = "last_app_unlock"

    const val secureScreen = "secure_screen"

    const val hideNotificationContent = "hide_notification_content"

    const val autoUpdateMetadata = "auto_update_metadata"

    const val autoUpdateTrackers = "auto_update_trackers"

    const val showLibraryUpdateErrors = "show_library_update_errors"

    const val downloadNew = "download_new"

    const val downloadNewCategories = "download_new_categories"
    const val downloadNewCategoriesExclude = "download_new_categories_exclude"

    const val libraryDisplayMode = "pref_display_mode_library"

    const val lang = "app_language"

    const val dateFormat = "app_date_format"

    const val defaultCategory = "default_category"

    const val categorizedDisplay = "categorized_display"

    const val skipRead = "skip_read"

    const val skipFiltered = "skip_filtered"

    const val downloadBadge = "display_download_badge"

    const val unreadBadge = "display_unread_badge"

    const val localBadge = "display_local_badge"

    const val categoryTabs = "display_category_tabs"

    const val categoryNumberOfItems = "display_number_of_items"

    const val alwaysShowChapterTransition = "always_show_chapter_transition"

    const val searchPinnedSourcesOnly = "search_pinned_sources_only"

    const val dohProvider = "doh_provider"

    const val defaultChapterFilterByRead = "default_chapter_filter_by_read"

    const val defaultChapterFilterByDownloaded = "default_chapter_filter_by_downloaded"

    const val defaultChapterFilterByBookmarked = "default_chapter_filter_by_bookmarked"

    const val defaultChapterSortBySourceOrNumber = "default_chapter_sort_by_source_or_number" // and upload date

    const val defaultChapterSortByAscendingOrDescending = "default_chapter_sort_by_ascending_or_descending"

    const val defaultChapterDisplayByNameOrNumber = "default_chapter_display_by_name_or_number"

    const val incognitoMode = "incognito_mode"

    fun trackUsername(syncId: Int) = "pref_mangasync_username_$syncId"

    fun trackPassword(syncId: Int) = "pref_mangasync_password_$syncId"

    fun trackToken(syncId: Int) = "track_token_$syncId"
}
