package eu.kanade.tachiyomi.ui.reader

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.util.plusAssign
import eu.kanade.tachiyomi.util.visibleIf
import eu.kanade.tachiyomi.widget.IgnoreFirstSpinnerListener
import kotlinx.android.synthetic.main.reader_settings_dialog.view.*
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.TimeUnit.MILLISECONDS

class ReaderSettingsDialog : DialogFragment() {

    private val preferences by injectLazy<PreferencesHelper>()

    private lateinit var subscriptions: CompositeSubscription

    override fun onCreateDialog(savedState: Bundle?): Dialog {
        val dialog = MaterialDialog.Builder(activity!!)
                .title(R.string.label_settings)
                .customView(R.layout.reader_settings_dialog, true)
                .positiveText(android.R.string.ok)
                .build()

        subscriptions = CompositeSubscription()
        onViewCreated(dialog.view, savedState)

        return dialog
    }

    override fun onViewCreated(view: View, savedState: Bundle?) = with(view) {
        viewer.onItemSelectedListener = IgnoreFirstSpinnerListener { position ->
            subscriptions += Observable.timer(250, MILLISECONDS, AndroidSchedulers.mainThread())
                    .subscribe {
                        val readerActivity = activity as? ReaderActivity
                        if (readerActivity != null) {
                            readerActivity.presenter.updateMangaViewer(position)
                            readerActivity.recreate()
                        }
                    }
        }
        viewer.setSelection((activity as ReaderActivity).presenter.manga.viewer, false)

        rotation_mode.onItemSelectedListener = IgnoreFirstSpinnerListener { position ->
            subscriptions += Observable.timer(250, MILLISECONDS)
                    .subscribe {
                        preferences.rotation().set(position + 1)
                    }
        }
        rotation_mode.setSelection(preferences.rotation().getOrDefault() - 1, false)

        scale_type.onItemSelectedListener = IgnoreFirstSpinnerListener { position ->
            preferences.imageScaleType().set(position + 1)
        }
        scale_type.setSelection(preferences.imageScaleType().getOrDefault() - 1, false)

        zoom_start.onItemSelectedListener = IgnoreFirstSpinnerListener { position ->
            preferences.zoomStart().set(position + 1)
        }
        zoom_start.setSelection(preferences.zoomStart().getOrDefault() - 1, false)

        image_decoder.onItemSelectedListener = IgnoreFirstSpinnerListener { position ->
            preferences.imageDecoder().set(position)
        }
        image_decoder.setSelection(preferences.imageDecoder().getOrDefault(), false)

        background_color.onItemSelectedListener = IgnoreFirstSpinnerListener { position ->
            preferences.readerTheme().set(position)
        }
        background_color.setSelection(preferences.readerTheme().getOrDefault(), false)

        show_page_number.isChecked = preferences.showPageNumber().getOrDefault()
        show_page_number.setOnCheckedChangeListener { _, isChecked ->
            preferences.showPageNumber().set(isChecked)
        }

        fullscreen.isChecked = preferences.fullscreen().getOrDefault()
        fullscreen.setOnCheckedChangeListener { _, isChecked ->
            preferences.fullscreen().set(isChecked)
        }

        crop_borders.isChecked = preferences.cropBorders().getOrDefault()
        crop_borders.setOnCheckedChangeListener { _, isChecked ->
            preferences.cropBorders().set(isChecked)
        }

        crop_borders_webtoon.isChecked = preferences.cropBordersWebtoon().getOrDefault()
        crop_borders_webtoon.setOnCheckedChangeListener { _, isChecked ->
            preferences.cropBordersWebtoon().set(isChecked)
        }

        val readerActivity = activity as? ReaderActivity
        val isWebtoonViewer = if (readerActivity != null) {
            val mangaViewer = readerActivity.presenter.manga.viewer
            val viewer = if (mangaViewer == 0) preferences.defaultViewer() else mangaViewer
            viewer == ReaderActivity.WEBTOON
        } else {
            false
        }

        crop_borders.visibleIf { !isWebtoonViewer }
        crop_borders_webtoon.visibleIf { isWebtoonViewer }
    }

    override fun onDestroyView() {
        subscriptions.unsubscribe()
        super.onDestroyView()
    }

}
