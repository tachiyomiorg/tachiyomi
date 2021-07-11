package eu.kanade.tachiyomi.ui.setting

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.preference.PreferenceScreen
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.preference.CHARGING
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.UNMETERED_NETWORK
import eu.kanade.tachiyomi.data.preference.asImmediateFlow
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.category.CategoryController
import eu.kanade.tachiyomi.util.preference.defaultValue
import eu.kanade.tachiyomi.util.preference.entriesRes
import eu.kanade.tachiyomi.util.preference.intListPreference
import eu.kanade.tachiyomi.util.preference.multiSelectListPreference
import eu.kanade.tachiyomi.util.preference.onChange
import eu.kanade.tachiyomi.util.preference.onClick
import eu.kanade.tachiyomi.util.preference.preference
import eu.kanade.tachiyomi.util.preference.preferenceCategory
import eu.kanade.tachiyomi.util.preference.summaryRes
import eu.kanade.tachiyomi.util.preference.switchPreference
import eu.kanade.tachiyomi.util.preference.titleRes
import eu.kanade.tachiyomi.util.system.isTablet
import eu.kanade.tachiyomi.widget.MinMaxNumberPicker
import eu.kanade.tachiyomi.widget.materialdialogs.QuadStateCheckBox
import eu.kanade.tachiyomi.widget.materialdialogs.listItemsQuadStateMultiChoice
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

class SettingsLibraryController : SettingsController() {

    private val db: DatabaseHelper = Injekt.get()
    private val trackManager: TrackManager by injectLazy()

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.pref_category_library

        val dbCategories = db.getCategories().executeAsBlocking()
        val categories = listOf(Category.createDefault()) + dbCategories

        preferenceCategory {
            titleRes = R.string.pref_category_display

            preference {
                key = "pref_library_columns"
                titleRes = R.string.pref_library_columns
                onClick {
                    LibraryColumnsDialog().showDialog(router)
                }

                fun getColumnValue(value: Int): String {
                    return if (value == 0) {
                        context.getString(R.string.default_columns)
                    } else {
                        value.toString()
                    }
                }

                preferences.portraitColumns().asFlow().combine(preferences.landscapeColumns().asFlow()) { portraitCols, landscapeCols -> Pair(portraitCols, landscapeCols) }
                    .onEach { (portraitCols, landscapeCols) ->
                        val portrait = getColumnValue(portraitCols)
                        val landscape = getColumnValue(landscapeCols)
                        summary = "${context.getString(R.string.portrait)}: $portrait, " +
                            "${context.getString(R.string.landscape)}: $landscape"
                    }
                    .launchIn(viewScope)
            }
            if (!context.isTablet()) {
                switchPreference {
                    key = Keys.jumpToChapters
                    titleRes = R.string.pref_jump_to_chapters
                    defaultValue = false
                }
            }
        }

        preferenceCategory {
            titleRes = R.string.categories

            preference {
                key = "pref_action_edit_categories"
                titleRes = R.string.action_edit_categories

                val catCount = dbCategories.size
                summary = context.resources.getQuantityString(R.plurals.num_categories, catCount, catCount)

                onClick {
                    router.pushController(CategoryController().withFadeTransaction())
                }
            }

            intListPreference {
                key = Keys.defaultCategory
                titleRes = R.string.default_category

                entries = arrayOf(context.getString(R.string.default_category_summary)) +
                    categories.map { it.name }.toTypedArray()
                entryValues = arrayOf("-1") + categories.map { it.id.toString() }.toTypedArray()
                defaultValue = "-1"

                val selectedCategory = categories.find { it.id == preferences.defaultCategory() }
                summary = selectedCategory?.name
                    ?: context.getString(R.string.default_category_summary)
                onChange { newValue ->
                    summary = categories.find {
                        it.id == (newValue as String).toInt()
                    }?.name ?: context.getString(R.string.default_category_summary)
                    true
                }
            }

            switchPreference {
                key = Keys.categorizedDisplay
                titleRes = R.string.categorized_display_settings
                defaultValue = false
            }
        }

        preferenceCategory {
            titleRes = R.string.pref_category_library_update

            intListPreference {
                key = Keys.libraryUpdateInterval
                titleRes = R.string.pref_library_update_interval
                entriesRes = arrayOf(
                    R.string.update_never,
                    R.string.update_3hour,
                    R.string.update_4hour,
                    R.string.update_6hour,
                    R.string.update_8hour,
                    R.string.update_12hour,
                    R.string.update_24hour,
                    R.string.update_48hour,
                    R.string.update_weekly
                )
                entryValues = arrayOf("0", "3", "4", "6", "8", "12", "24", "48", "168")
                defaultValue = "24"
                summary = "%s"

                onChange { newValue ->
                    val interval = (newValue as String).toInt()
                    LibraryUpdateJob.setupTask(context, interval)
                    true
                }
            }
            multiSelectListPreference {
                key = Keys.libraryUpdateRestriction
                titleRes = R.string.pref_library_update_restriction
                entriesRes = arrayOf(R.string.network_unmetered, R.string.charging)
                entryValues = arrayOf(UNMETERED_NETWORK, CHARGING)
                defaultValue = setOf(UNMETERED_NETWORK)

                preferences.libraryUpdateInterval().asImmediateFlow { isVisible = it > 0 }
                    .launchIn(viewScope)

                onChange {
                    // Post to event looper to allow the preference to be updated.
                    ContextCompat.getMainExecutor(context).execute { LibraryUpdateJob.setupTask(context) }
                    true
                }

                fun updateSummary() {
                    val restrictions = preferences.libraryUpdateRestriction().get()
                        .sorted()
                        .map {
                            when (it) {
                                UNMETERED_NETWORK -> context.getString(R.string.network_unmetered)
                                CHARGING -> context.getString(R.string.charging)
                                else -> it
                            }
                        }
                    val restrictionsText = if (restrictions.isEmpty()) {
                        context.getString(R.string.none)
                    } else {
                        restrictions.joinToString()
                    }

                    summary = context.getString(R.string.restrictions, restrictionsText)
                }

                preferences.libraryUpdateRestriction().asFlow()
                    .onEach { updateSummary() }
                    .launchIn(viewScope)
            }
            switchPreference {
                key = Keys.updateOnlyNonCompleted
                titleRes = R.string.pref_update_only_non_completed
                defaultValue = false
            }
            preference {
                key = Keys.libraryUpdateCategories
                titleRes = R.string.categories
                onClick {
                    LibraryGlobalUpdateCategoriesDialog().showDialog(router)
                }

                fun updateSummary() {
                    val selectedCategories = preferences.libraryUpdateCategories().get()
                        .mapNotNull { id -> categories.find { it.id == id.toInt() } }
                        .sortedBy { it.order }
                    val includedItemsText = if (selectedCategories.isEmpty()) {
                        context.getString(R.string.all)
                    } else {
                        selectedCategories.joinToString { it.name }
                    }

                    val excludedCategories = preferences.libraryUpdateCategoriesExclude().get()
                        .mapNotNull { id -> categories.find { it.id == id.toInt() } }
                        .sortedBy { it.order }
                    val excludedItemsText = if (excludedCategories.isEmpty()) {
                        context.getString(R.string.none)
                    } else {
                        excludedCategories.joinToString { it.name }
                    }

                    summary = buildSpannedString {
                        append(context.getString(R.string.include, includedItemsText))
                        appendLine()
                        append(context.getString(R.string.exclude, excludedItemsText))
                    }
                }

                preferences.libraryUpdateCategories().asFlow()
                    .onEach { updateSummary() }
                    .launchIn(viewScope)
                preferences.libraryUpdateCategoriesExclude().asFlow()
                    .onEach { updateSummary() }
                    .launchIn(viewScope)
            }
            intListPreference {
                key = Keys.libraryUpdatePrioritization
                titleRes = R.string.pref_library_update_prioritization

                // The following array lines up with the list rankingScheme in:
                // ../../data/library/LibraryUpdateRanker.kt
                val priorities = arrayOf(
                    Pair("0", R.string.action_sort_alpha),
                    Pair("1", R.string.action_sort_last_checked),
                    Pair("2", R.string.action_sort_next_updated)
                )
                val defaultPriority = priorities[0]

                entriesRes = priorities.map { it.second }.toTypedArray()
                entryValues = priorities.map { it.first }.toTypedArray()
                defaultValue = defaultPriority.first

                val selectedPriority = priorities.find { it.first.toInt() == preferences.libraryUpdatePrioritization().get() }
                summaryRes = selectedPriority?.second ?: defaultPriority.second
                onChange { newValue ->
                    summaryRes = priorities.find {
                        it.first == (newValue as String)
                    }?.second ?: defaultPriority.second
                    true
                }
            }
            switchPreference {
                key = Keys.autoUpdateMetadata
                titleRes = R.string.pref_library_update_refresh_metadata
                summaryRes = R.string.pref_library_update_refresh_metadata_summary
                defaultValue = false
            }
            if (trackManager.hasLoggedServices()) {
                switchPreference {
                    key = Keys.autoUpdateTrackers
                    titleRes = R.string.pref_library_update_refresh_trackers
                    summaryRes = R.string.pref_library_update_refresh_trackers_summary
                    defaultValue = false
                }
            }
            switchPreference {
                key = Keys.showLibraryUpdateErrors
                titleRes = R.string.pref_library_update_error_notification
                defaultValue = true
            }
        }
    }

    class LibraryColumnsDialog : DialogController() {

        private val preferences: PreferencesHelper = Injekt.get()

        private var portrait = preferences.portraitColumns().get()
        private var landscape = preferences.landscapeColumns().get()

        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            val dialog = MaterialDialog(activity!!)
                .title(R.string.pref_library_columns)
                .customView(R.layout.pref_library_columns, horizontalPadding = true)
                .positiveButton(android.R.string.ok) {
                    preferences.portraitColumns().set(portrait)
                    preferences.landscapeColumns().set(landscape)
                }
                .negativeButton(android.R.string.cancel)

            onViewCreated(dialog.view)
            return dialog
        }

        fun onViewCreated(view: View) {
            with(view.findViewById(R.id.portrait_columns) as MinMaxNumberPicker) {
                displayedValues = arrayOf(context.getString(R.string.default_columns)) +
                    IntRange(1, 10).map(Int::toString)
                value = portrait

                setOnValueChangedListener { _, _, newValue ->
                    portrait = newValue
                }
            }
            with(view.findViewById(R.id.landscape_columns) as MinMaxNumberPicker) {
                displayedValues = arrayOf(context.getString(R.string.default_columns)) +
                    IntRange(1, 10).map(Int::toString)
                value = landscape

                setOnValueChangedListener { _, _, newValue ->
                    landscape = newValue
                }
            }
        }
    }

    class LibraryGlobalUpdateCategoriesDialog : DialogController() {

        private val preferences: PreferencesHelper = Injekt.get()
        private val db: DatabaseHelper = Injekt.get()

        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            val dbCategories = db.getCategories().executeAsBlocking()
            val categories = listOf(Category.createDefault()) + dbCategories

            val items = categories.map { it.name }
            val preselected = categories
                .map {
                    when (it.id.toString()) {
                        in preferences.libraryUpdateCategories().get() -> QuadStateCheckBox.State.CHECKED.ordinal
                        in preferences.libraryUpdateCategoriesExclude().get() -> QuadStateCheckBox.State.INVERSED.ordinal
                        else -> QuadStateCheckBox.State.UNCHECKED.ordinal
                    }
                }
                .toIntArray()

            return MaterialDialog(activity!!)
                .title(R.string.categories)
                .message(R.string.pref_library_update_categories_details)
                .listItemsQuadStateMultiChoice(
                    items = items,
                    initialSelected = preselected
                ) { selections ->
                    val included = selections
                        .mapIndexed { index, value -> if (value == QuadStateCheckBox.State.CHECKED.ordinal) index else null }
                        .filterNotNull()
                        .map { categories[it].id.toString() }
                        .toSet()
                    val excluded = selections
                        .mapIndexed { index, value -> if (value == QuadStateCheckBox.State.INVERSED.ordinal) index else null }
                        .filterNotNull()
                        .map { categories[it].id.toString() }
                        .toSet()

                    preferences.libraryUpdateCategories().set(included)
                    preferences.libraryUpdateCategoriesExclude().set(excluded)
                }
                .positiveButton(android.R.string.ok)
                .negativeButton(android.R.string.cancel)
        }
    }
}
