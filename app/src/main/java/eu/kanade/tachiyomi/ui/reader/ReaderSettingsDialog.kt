package eu.kanade.tachiyomi.ui.reader

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import android.widget.SeekBar
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.util.plusAssign
import eu.kanade.tachiyomi.widget.SimpleItemSelectedListener
import eu.kanade.tachiyomi.widget.SimpleSeekBarListener
import kotlinx.android.synthetic.main.dialog_reader_settings.view.*
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.TimeUnit.MILLISECONDS

class ReaderSettingsDialog : DialogFragment() {

    private val preferences by injectLazy<PreferencesHelper>()

    private lateinit var subscriptions: CompositeSubscription

    override fun onCreateDialog(savedState: Bundle?): Dialog {
        val dialog = MaterialDialog.Builder(activity)
                .title(R.string.label_settings)
                .customView(R.layout.dialog_reader_settings, true)
                .positiveText(android.R.string.ok)
                .build()

        subscriptions = CompositeSubscription()
        onViewCreated(dialog.view, savedState)

        return dialog
    }

    override fun onViewCreated(view: View, savedState: Bundle?) = with(view) {
        viewer.setSelection((activity as ReaderActivity).presenter.manga.viewer, false)
        viewer.onItemSelectedListener = SimpleItemSelectedListener { position ->
            subscriptions += Observable.timer(250, MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        (activity as ReaderActivity).presenter.updateMangaViewer(position)
                        activity.recreate()
                    }
        }

        rotation_mode.setSelection(preferences.rotation().getOrDefault() - 1, false)
        rotation_mode.onItemSelectedListener = SimpleItemSelectedListener { position ->
            subscriptions += Observable.timer(250, MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        preferences.rotation().set(position + 1)
                    }
        }

        scale_type.setSelection(preferences.imageScaleType().getOrDefault() - 1, false)
        scale_type.onItemSelectedListener = SimpleItemSelectedListener { position ->
            preferences.imageScaleType().set(position + 1)
        }

        zoom_start.setSelection(preferences.zoomStart().getOrDefault() - 1, false)
        zoom_start.onItemSelectedListener = SimpleItemSelectedListener { position ->
            preferences.zoomStart().set(position + 1)
        }

        image_decoder.setSelection(preferences.imageDecoder().getOrDefault(), false)
        image_decoder.onItemSelectedListener = SimpleItemSelectedListener { position ->
            preferences.imageDecoder().set(position)
        }

        background_color.setSelection(preferences.readerTheme().getOrDefault(), false)
        background_color.onItemSelectedListener = SimpleItemSelectedListener { position ->
            preferences.readerTheme().set(position)
        }

        enable_transitions.isChecked = preferences.enableTransitions().getOrDefault()
        enable_transitions.setOnCheckedChangeListener { v, isChecked ->
            preferences.enableTransitions().set(isChecked)
        }

        show_page_number.isChecked = preferences.showPageNumber().getOrDefault()
        show_page_number.setOnCheckedChangeListener { v, isChecked ->
            preferences.showPageNumber().set(isChecked)
        }

        fullscreen.isChecked = preferences.fullscreen().getOrDefault()
        fullscreen.setOnCheckedChangeListener { v, isChecked ->
            preferences.fullscreen().set(isChecked)
        }

        keep_screen_on.isChecked = preferences.keepScreenOn().getOrDefault()
        keep_screen_on.setOnCheckedChangeListener { v, isChecked ->
            preferences.keepScreenOn().set(isChecked)
        }

        subscriptions += preferences.customBrightness().asObservable()
                .subscribe { isEnabled ->
                    custom_brightness.isChecked = isEnabled
                    brightness_seekbar.isEnabled = isEnabled
                }

        custom_brightness.setOnCheckedChangeListener { v, isChecked ->
            preferences.customBrightness().set(isChecked)
        }

        brightness_seekbar.max = 100
        brightness_seekbar.progress = Math.round(
                preferences.customBrightnessValue().getOrDefault() * brightness_seekbar.max)
        brightness_seekbar.setOnSeekBarChangeListener(object : SimpleSeekBarListener() {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    preferences.customBrightnessValue().set(progress.toFloat() / seekBar.max)
                }
            }
        })

    }

    override fun onDestroyView() {
        subscriptions.unsubscribe()
        super.onDestroyView()
    }

}