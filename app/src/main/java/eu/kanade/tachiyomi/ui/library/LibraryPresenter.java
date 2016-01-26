package eu.kanade.tachiyomi.ui.library;

import android.os.Bundle;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import eu.kanade.tachiyomi.data.cache.CoverCache;
import eu.kanade.tachiyomi.data.database.DatabaseHelper;
import eu.kanade.tachiyomi.data.database.models.Category;
import eu.kanade.tachiyomi.data.database.models.Manga;
import eu.kanade.tachiyomi.data.database.models.MangaCategory;
import eu.kanade.tachiyomi.data.preference.PreferencesHelper;
import eu.kanade.tachiyomi.data.source.SourceManager;
import eu.kanade.tachiyomi.event.LibraryMangasEvent;
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

public class LibraryPresenter extends BasePresenter<LibraryFragment> {

    @Inject DatabaseHelper db;
    @Inject PreferencesHelper preferences;
    @Inject CoverCache coverCache;
    @Inject SourceManager sourceManager;

    protected List<Category> categories;
    protected List<Manga> selectedMangas;

    private static final int GET_LIBRARY = 1;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        selectedMangas = new ArrayList<>();

        restartableLatestCache(GET_LIBRARY,
                this::getLibraryObservable,
                (view, pair) -> view.onNextLibraryUpdate(pair.first, pair.second));

        if (savedState == null) {
            start(GET_LIBRARY);
        }
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().removeStickyEvent(LibraryMangasEvent.class);
        super.onDestroy();
    }

    @Override
    protected void onTakeView(LibraryFragment libraryFragment) {
        super.onTakeView(libraryFragment);
        if (isUnsubscribed(GET_LIBRARY)) {
            start(GET_LIBRARY);
        }
    }

    private Observable<Pair<List<Category>, Map<Integer, List<Manga>>>> getLibraryObservable() {
        return Observable.combineLatest(getCategoriesObservable(), getLibraryMangasObservable(),
                Pair::create)
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<List<Category>> getCategoriesObservable() {
        return db.getCategories().asRxObservable()
                .doOnNext(categories -> this.categories = categories);
    }

    private Observable<Map<Integer, List<Manga>>> getLibraryMangasObservable() {
        return db.getLibraryMangas().asRxObservable()
                .flatMap(mangas -> Observable.from(mangas)
                        .groupBy(manga -> manga.category)
                        .flatMap(group -> group.toList()
                                .map(list -> Pair.create(group.getKey(), list)))
                        .toMap(pair -> pair.first, pair -> pair.second));
    }

    public void onOpenManga(Manga manga) {
        // Avoid further db updates for the library when it's not needed
        stop(GET_LIBRARY);
    }

    public void setSelection(Manga manga, boolean selected) {
        if (selected) {
            selectedMangas.add(manga);
        } else {
            selectedMangas.remove(manga);
        }
    }

    public String[] getCategoriesNames() {
        int count = categories.size();
        String[] names = new String[count];

        for (int i = 0; i < count; i++) {
            names[i] = categories.get(i).name;
        }

        return names;
    }

    public void deleteMangas() {
        for (Manga manga : selectedMangas) {
            manga.favorite = false;
        }

        db.insertMangas(selectedMangas).executeAsBlocking();
    }

    public void moveMangasToCategories(Integer[] positions, List<Manga> mangas) {
        List<Category> categoriesToAdd = new ArrayList<>();
        for (Integer index : positions) {
            categoriesToAdd.add(categories.get(index));
        }

        moveMangasToCategories(categoriesToAdd, mangas);
    }

    public void moveMangasToCategories(List<Category> categories, List<Manga> mangas) {
        List<MangaCategory> mc = new ArrayList<>();

        for (Manga manga : mangas) {
            for (Category cat : categories) {
                mc.add(MangaCategory.create(manga, cat));
            }
        }

        db.setMangaCategories(mc, mangas);
    }
}
