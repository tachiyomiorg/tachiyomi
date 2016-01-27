package eu.kanade.tachiyomi.ui.manga.chapter;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import eu.kanade.tachiyomi.R;
import eu.kanade.tachiyomi.data.database.models.Chapter;
import eu.kanade.tachiyomi.data.download.DownloadService;
import eu.kanade.tachiyomi.data.download.model.Download;
import eu.kanade.tachiyomi.ui.base.adapter.FlexibleViewHolder;
import eu.kanade.tachiyomi.ui.base.fragment.BaseRxFragment;
import eu.kanade.tachiyomi.ui.decoration.DividerItemDecoration;
import eu.kanade.tachiyomi.ui.manga.MangaActivity;
import eu.kanade.tachiyomi.ui.reader.ReaderActivity;
import eu.kanade.tachiyomi.util.ToastUtil;
import nucleus.factory.RequiresPresenter;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

@RequiresPresenter(ChaptersPresenter.class)
public class ChaptersFragment extends BaseRxFragment<ChaptersPresenter> implements
        ActionMode.Callback, FlexibleViewHolder.OnListItemClickListener {

    @Bind(R.id.chapter_list) RecyclerView recyclerView;
    @Bind(R.id.swipe_refresh) SwipeRefreshLayout swipeRefresh;
    @Bind(R.id.toolbar_bottom) ViewGroup toolbarBottom;

    @Bind(R.id.action_sort) ImageView sortBtn;
    @Bind(R.id.action_next_unread) ImageView nextUnreadBtn;
    @Bind(R.id.action_show_unread) CheckBox readCb;
    @Bind(R.id.action_show_downloaded) CheckBox downloadedCb;

    private ChaptersAdapter adapter;
    private LinearLayoutManager linearLayout;
    private ActionMode actionMode;

    private Subscription downloadProgressSubscription;

    public static ChaptersFragment newInstance() {
        return new ChaptersFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_manga_chapters, container, false);
        ButterKnife.bind(this, view);

        // Init RecyclerView and adapter
        linearLayout = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayout);
        recyclerView.addItemDecoration(new DividerItemDecoration(ContextCompat.getDrawable(getContext(), R.drawable.line_divider)));
        recyclerView.setHasFixedSize(true);
        adapter = new ChaptersAdapter(this);
        recyclerView.setAdapter(adapter);

        // Set initial values
        setReadFilter();
        setDownloadedFilter();
        setSortIcon();

        // Init listeners
        swipeRefresh.setOnRefreshListener(this::fetchChapters);
        readCb.setOnCheckedChangeListener((arg, isChecked) ->
                getPresenter().setReadFilter(isChecked));
        downloadedCb.setOnCheckedChangeListener((v, isChecked) ->
                getPresenter().setDownloadedFilter(isChecked));
        sortBtn.setOnClickListener(v -> {
            getPresenter().revertSortOrder();
            setSortIcon();
        });
        nextUnreadBtn.setOnClickListener(v -> {
            Chapter chapter = getPresenter().getNextUnreadChapter();
            if (chapter != null) {
                openChapter(chapter);
            } else {
                ToastUtil.showShort(getContext(), R.string.no_next_chapter);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        observeChapterDownloadProgress();
    }

    @Override
    public void onPause() {
        unsubscribeChapterDownloadProgress();
        super.onPause();
    }

    public void onNextChapters(List<Chapter> chapters) {
        // If the list is empty, fetch chapters from source if the conditions are met
        // We use presenter chapters instead because they are always unfiltered
        if (getPresenter().getChapters().isEmpty())
            initialFetchChapters();

        destroyActionModeIfNeeded();
        adapter.setItems(chapters);
    }

    private void initialFetchChapters() {
        // Only fetch if this view is from the catalog and it hasn't requested previously
        if (isCatalogueManga() && !getPresenter().hasRequested()) {
            fetchChapters();
        }
    }

    public void fetchChapters() {
        if (getPresenter().getManga() != null) {
            swipeRefresh.setRefreshing(true);
            getPresenter().fetchChaptersFromSource();
        }
    }

    public void onFetchChaptersDone() {
        swipeRefresh.setRefreshing(false);
    }

    public void onFetchChaptersError() {
        swipeRefresh.setRefreshing(false);
        ToastUtil.showShort(getContext(), R.string.fetch_chapters_error);
    }

    public boolean isCatalogueManga() {
        return ((MangaActivity) getActivity()).isCatalogueManga();
    }

    protected void openChapter(Chapter chapter) {
        getPresenter().onOpenChapter(chapter);
        Intent intent = ReaderActivity.newIntent(getActivity());
        startActivity(intent);
    }

    private void observeChapterDownloadProgress() {
        downloadProgressSubscription = getPresenter().getDownloadProgressObs()
                .subscribe(this::onDownloadProgressChange,
                        error -> { /* TODO getting a NPE sometimes on 'manga' from presenter */ });
    }

    private void unsubscribeChapterDownloadProgress() {
        if (downloadProgressSubscription != null)
            downloadProgressSubscription.unsubscribe();
    }

    private void onDownloadProgressChange(Download download) {
        ChaptersHolder holder = getHolder(download.chapter);
        if (holder != null)
            holder.onProgressChange(getContext(), download.downloadedImages, download.pages.size());
    }

    public void onChapterStatusChange(Chapter chapter) {
        ChaptersHolder holder = getHolder(chapter);
        if (holder != null)
            holder.onStatusChange(chapter.status);
    }

    @Nullable
    private ChaptersHolder getHolder(Chapter chapter) {
        return (ChaptersHolder) recyclerView.findViewHolderForItemId(chapter.id);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.chapter_selection, menu);
        adapter.setMode(ChaptersAdapter.MODE_MULTI);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_select_all:
                return onSelectAll();
            case R.id.action_mark_as_read:
                return onMarkAsRead(getSelectedChapters());
            case R.id.action_mark_as_unread:
                return onMarkAsUnread(getSelectedChapters());
            case R.id.action_download:
                return onDownload(getSelectedChapters());
            case R.id.action_delete:
                return onDelete(getSelectedChapters());
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        adapter.setMode(ChaptersAdapter.MODE_SINGLE);
        adapter.clearSelection();
        actionMode = null;
    }

    private Observable<Chapter> getSelectedChapters() {
        // Create a blocking copy of the selected chapters.
        // When the action mode is closed the list is cleared. If we use background
        // threads with this observable, some emissions could be lost.
        List<Chapter> chapters = Observable.from(adapter.getSelectedItems())
                .map(adapter::getItem).toList().toBlocking().single();

        return Observable.from(chapters);
    }

    public void destroyActionModeIfNeeded() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    protected boolean onSelectAll() {
        adapter.selectAll();
        setContextTitle(adapter.getSelectedItemCount());
        return true;
    }

    protected boolean onMarkAsRead(Observable<Chapter> chapters) {
        getPresenter().markChaptersRead(chapters, true);
        return true;
    }

    protected boolean onMarkAsUnread(Observable<Chapter> chapters) {
        getPresenter().markChaptersRead(chapters, false);
        return true;
    }

    public boolean onMarkPreviousAsRead(Chapter chapter) {
        getPresenter().markPreviousChaptersAsRead(chapter);
        return true;
    }

    protected boolean onDownload(Observable<Chapter> chapters) {
        DownloadService.start(getActivity());

        Observable<Chapter> observable = chapters
                .doOnCompleted(adapter::notifyDataSetChanged);

        getPresenter().downloadChapters(observable);
        destroyActionModeIfNeeded();
        return true;
    }

    protected boolean onDelete(Observable<Chapter> chapters) {
        int size = adapter.getSelectedItemCount();

        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.deleting)
                .progress(false, size, true)
                .cancelable(false)
                .show();

        Observable<Chapter> observable = chapters
                .concatMap(chapter -> {
                    getPresenter().deleteChapter(chapter);
                    return Observable.just(chapter);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(chapter -> {
                    dialog.incrementProgress(1);
                    chapter.status = Download.NOT_DOWNLOADED;
                })
                .doOnCompleted(adapter::notifyDataSetChanged)
                .finallyDo(dialog::dismiss);

        getPresenter().deleteChapters(observable);
        destroyActionModeIfNeeded();
        return true;
    }

    @Override
    public boolean onListItemClick(int position) {
        if (actionMode != null && adapter.getMode() == ChaptersAdapter.MODE_MULTI) {
            toggleSelection(position);
            return true;
        } else {
            openChapter(adapter.getItem(position));
            return false;
        }
    }

    @Override
    public void onListItemLongClick(int position) {
        if (actionMode == null)
            actionMode = getBaseActivity().startSupportActionMode(this);

        toggleSelection(position);
    }

    private void toggleSelection(int position) {
        adapter.toggleSelection(position, false);

        int count = adapter.getSelectedItemCount();
        if (count == 0) {
            actionMode.finish();
        } else {
            setContextTitle(count);
            actionMode.invalidate();
        }
    }

    private void setContextTitle(int count) {
        actionMode.setTitle(getString(R.string.label_selected, count));
    }

    public void setSortIcon() {
        if (sortBtn != null) {
            boolean aToZ = getPresenter().getSortOrder();
            sortBtn.setImageResource(!aToZ ? R.drawable.ic_expand_less_white_36dp : R.drawable.ic_expand_more_white_36dp);
        }
    }

    public void setReadFilter() {
        if (readCb != null) {
            readCb.setChecked(getPresenter().getReadFilter());
        }
    }

    public void setDownloadedFilter() {
        if (downloadedCb != null) {
            downloadedCb.setChecked(getPresenter().getDownloadedFilter());
        }
    }

}
