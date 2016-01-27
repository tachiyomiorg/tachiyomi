package eu.kanade.tachiyomi.ui.catalogue;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.ViewSwitcher;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.ButterKnife;
import eu.kanade.tachiyomi.R;
import eu.kanade.tachiyomi.data.database.models.Manga;
import eu.kanade.tachiyomi.data.source.base.Source;
import eu.kanade.tachiyomi.ui.base.adapter.FlexibleViewHolder;
import eu.kanade.tachiyomi.ui.base.fragment.BaseRxFragment;
import eu.kanade.tachiyomi.ui.decoration.DividerItemDecoration;
import eu.kanade.tachiyomi.ui.main.MainActivity;
import eu.kanade.tachiyomi.ui.manga.MangaActivity;
import eu.kanade.tachiyomi.util.ToastUtil;
import eu.kanade.tachiyomi.widget.AutofitRecyclerView;
import eu.kanade.tachiyomi.widget.EndlessGridScrollListener;
import eu.kanade.tachiyomi.widget.EndlessListScrollListener;
import icepick.State;
import nucleus.factory.RequiresPresenter;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;

@RequiresPresenter(CataloguePresenter.class)
public class CatalogueFragment extends BaseRxFragment<CataloguePresenter>
        implements FlexibleViewHolder.OnListItemClickListener {

    @Bind(R.id.switcher) ViewSwitcher switcher;
    @Bind(R.id.catalogue_grid) AutofitRecyclerView catalogueGrid;
    @Bind(R.id.catalogue_list) RecyclerView catalogueList;
    @Bind(R.id.progress) ProgressBar progress;
    @Bind(R.id.progress_grid) ProgressBar progressGrid;

    private Toolbar toolbar;
    private Spinner spinner;
    private CatalogueAdapter adapter;
    private EndlessGridScrollListener gridScrollListener;
    private EndlessListScrollListener listScrollListener;

    @State String query = "";
    @State int selectedIndex = -1;
    private final int SEARCH_TIMEOUT = 1000;

    private PublishSubject<String> queryDebouncerSubject;
    private Subscription queryDebouncerSubscription;

    private MenuItem displayMode;
    private MenuItem searchItem;

    public static CatalogueFragment newInstance() {
        return new CatalogueFragment();
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_catalogue, container, false);
        ButterKnife.bind(this, view);

        // Initialize adapter, scroll listener and recycler views
        adapter = new CatalogueAdapter(this);

        GridLayoutManager glm = (GridLayoutManager) catalogueGrid.getLayoutManager();
        gridScrollListener = new EndlessGridScrollListener(glm, this::requestNextPage);
        catalogueGrid.setHasFixedSize(true);
        catalogueGrid.setAdapter(adapter);
        catalogueGrid.addOnScrollListener(gridScrollListener);

        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        listScrollListener = new EndlessListScrollListener(llm, this::requestNextPage);
        catalogueList.setHasFixedSize(true);
        catalogueList.setAdapter(adapter);
        catalogueList.setLayoutManager(llm);
        catalogueList.addOnScrollListener(listScrollListener);
        catalogueList.addItemDecoration(new DividerItemDecoration(
                ContextCompat.getDrawable(getContext(), R.drawable.line_divider)));

        if (getPresenter().isListMode()) {
            switcher.showNext();
        }

        Animation inAnim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
        Animation outAnim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
        switcher.setInAnimation(inAnim);
        switcher.setOutAnimation(outAnim);

        // Create toolbar spinner
        Context themedContext = getBaseActivity().getSupportActionBar() != null ?
                getBaseActivity().getSupportActionBar().getThemedContext() : getActivity();
        spinner = new Spinner(themedContext);
        CatalogueSpinnerAdapter spinnerAdapter = new CatalogueSpinnerAdapter(themedContext,
                android.R.layout.simple_spinner_item, getPresenter().getEnabledSources());
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (savedState == null) selectedIndex = spinnerAdapter.getEmptyIndex();
        spinner.setAdapter(spinnerAdapter);
        spinner.setSelection(selectedIndex);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Source source = spinnerAdapter.getItem(position);
                // We add an empty source with id -1 that acts as a placeholder to show a hint
                // that asks to select a source
                if (source.getId() != -1 && (selectedIndex != position || adapter.isEmpty())) {
                    // Set previous selection if it's not a valid source and notify the user
                    if (!getPresenter().isValidSource(source)) {
                        spinner.setSelection(spinnerAdapter.getEmptyIndex());
                        ToastUtil.showShort(getActivity(), R.string.source_requires_login);
                    } else {
                        selectedIndex = position;
                        showProgressBar();
                        glm.scrollToPositionWithOffset(0, 0);
                        llm.scrollToPositionWithOffset(0, 0);
                        getPresenter().startRequesting(source);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        setToolbarTitle("");
        toolbar = ((MainActivity)getActivity()).getToolbar();
        toolbar.addView(spinner);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.catalogue_list, menu);

        // Initialize search menu
        searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        if (!TextUtils.isEmpty(query)) {
            searchItem.expandActionView();
            searchView.setQuery(query, true);
            searchView.clearFocus();
        }
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                onSearchEvent(query, true);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                onSearchEvent(newText, false);
                return true;
            }
        });

        // Show next display mode
        displayMode = menu.findItem(R.id.action_display_mode);
        int icon = getPresenter().isListMode() ?
                R.drawable.ic_view_module_white_24dp : R.drawable.ic_view_list_white_24dp;
        displayMode.setIcon(icon);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_display_mode:
                swapDisplayMode();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        initializeSearchSubscription();
    }

    @Override
    public void onStop() {
        destroySearchSubscription();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if (searchItem != null && searchItem.isActionViewExpanded()) {
            searchItem.collapseActionView();
        }
        toolbar.removeView(spinner);
        super.onDestroyView();
    }

    private void initializeSearchSubscription() {
        queryDebouncerSubject = PublishSubject.create();
        queryDebouncerSubscription = queryDebouncerSubject
                .debounce(SEARCH_TIMEOUT, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::restartRequest);
    }

    private void destroySearchSubscription() {
        queryDebouncerSubscription.unsubscribe();
    }

    private void onSearchEvent(String query, boolean now) {
        // If the query is not debounced, resolve it instantly
        if (now)
            restartRequest(query);
        else if (queryDebouncerSubject != null)
            queryDebouncerSubject.onNext(query);
    }

    private void restartRequest(String newQuery) {
        // If text didn't change, do nothing
        if (query.equals(newQuery) || getPresenter().getSource() == null)
            return;

        query = newQuery;
        showProgressBar();
        catalogueGrid.getLayoutManager().scrollToPosition(0);
        catalogueList.getLayoutManager().scrollToPosition(0);

        getPresenter().restartRequest(query);
    }

    private void requestNextPage() {
        if (getPresenter().hasNextPage()) {
            showGridProgressBar();
            getPresenter().requestNext();
        }
    }

    public void onAddPage(int page, List<Manga> mangas) {
        hideProgressBar();
        if (page == 0) {
            adapter.clear();
            gridScrollListener.resetScroll();
            listScrollListener.resetScroll();
        }
        adapter.addItems(mangas);
    }

    public void onAddPageError() {
        hideProgressBar();
    }

    public void updateImage(Manga manga) {
        CatalogueGridHolder holder = getHolder(manga);
        if (holder != null) {
            holder.setImage(manga, getPresenter());
        }
    }

    public void swapDisplayMode() {
        getPresenter().swapDisplayMode();
        boolean isListMode = getPresenter().isListMode();
        int icon = isListMode ?
                R.drawable.ic_view_module_white_24dp : R.drawable.ic_view_list_white_24dp;
        displayMode.setIcon(icon);
        switcher.showNext();
        if (!isListMode) {
            // Initialize mangas if going to grid view
            getPresenter().initializeMangas(adapter.getItems());
        }
    }

    @Nullable
    private CatalogueGridHolder getHolder(Manga manga) {
        return (CatalogueGridHolder) catalogueGrid.findViewHolderForItemId(manga.id);
    }

    private void showProgressBar() {
        progress.setVisibility(ProgressBar.VISIBLE);
    }

    private void showGridProgressBar() {
        progressGrid.setVisibility(ProgressBar.VISIBLE);
    }

    private void hideProgressBar() {
        progress.setVisibility(ProgressBar.GONE);
        progressGrid.setVisibility(ProgressBar.GONE);
    }

    @Override
    public boolean onListItemClick(int position) {
        final Manga selectedManga = adapter.getItem(position);

        Intent intent = MangaActivity.newIntent(getActivity(), selectedManga);
        intent.putExtra(MangaActivity.MANGA_ONLINE, true);
        startActivity(intent);
        return false;
    }

    @Override
    public void onListItemLongClick(int position) {
        final Manga selectedManga = adapter.getItem(position);

        int textRes = selectedManga.favorite ? R.string.remove_from_library : R.string.add_to_library;

        new MaterialDialog.Builder(getActivity())
                .items(getString(textRes))
                .itemsCallback((dialog, itemView, which, text) -> {
                    switch (which) {
                        case 0:
                            getPresenter().changeMangaFavorite(selectedManga);
                            adapter.notifyItemChanged(position);
                            break;
                    }
                })
                .show();
    }
}
