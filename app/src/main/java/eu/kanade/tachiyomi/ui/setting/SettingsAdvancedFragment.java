package eu.kanade.tachiyomi.ui.setting;

import android.os.Bundle;
import android.preference.Preference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import eu.kanade.tachiyomi.R;
import eu.kanade.tachiyomi.data.cache.ChapterCache;
import eu.kanade.tachiyomi.data.database.DatabaseHelper;
import eu.kanade.tachiyomi.util.ToastUtil;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class SettingsAdvancedFragment extends SettingsNestedFragment {

    private CompositeSubscription subscriptions;

    public static SettingsNestedFragment newInstance(int resourcePreference, int resourceTitle) {
        SettingsNestedFragment fragment = new SettingsAdvancedFragment();
        fragment.setArgs(resourcePreference, resourceTitle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View view = super.onCreateView(inflater, container, savedState);
        subscriptions = new CompositeSubscription();

        Preference clearCache = findPreference(getString(R.string.pref_clear_chapter_cache_key));
        clearCache.setOnPreferenceClickListener(preference -> {
            clearChapterCache(preference);
            return true;
        });
        clearCache.setSummary(getString(R.string.used_cache, getChapterCache().getReadableSize()));

        Preference clearDatabase = findPreference(getString(R.string.pref_clear_database_key));
        clearDatabase.setOnPreferenceClickListener(preference -> {
            clearDatabase();
            return true;
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        subscriptions.unsubscribe();
        super.onDestroyView();
    }

    private void clearChapterCache(Preference preference) {
        final ChapterCache chapterCache = getChapterCache();
        final AtomicInteger deletedFiles = new AtomicInteger();

        File[] files = chapterCache.getCacheDir().listFiles();

        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.deleting)
                .progress(false, files.length, true)
                .cancelable(false)
                .show();

        subscriptions.add(Observable.defer(() -> Observable.from(files))
                .concatMap(file -> {
                    if (chapterCache.removeFileFromCache(file.getName())) {
                        deletedFiles.incrementAndGet();
                    }
                    return Observable.just(file);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(file -> dialog.incrementProgress(1),
                        error -> {
                            dialog.dismiss();
                            ToastUtil.showShort(getActivity(), getString(R.string.cache_delete_error));
                        }, () -> {
                            dialog.dismiss();
                            ToastUtil.showShort(getActivity(), getString(R.string.cache_deleted, deletedFiles.get()));
                            preference.setSummary(getString(R.string.used_cache, chapterCache.getReadableSize()));
                        }));
    }

    private void clearDatabase() {
        final DatabaseHelper db = getSettingsActivity().db;

        new MaterialDialog.Builder(getActivity())
                .content(R.string.clear_database_confirmation)
                .positiveText(R.string.button_yes)
                .negativeText(R.string.button_no)
                .onPositive((dialog1, which) -> {
                    db.deleteMangasNotInLibrary().executeAsBlocking();
                })
                .show();
    }

    private ChapterCache getChapterCache() {
        return getSettingsActivity().chapterCache;
    }

}
