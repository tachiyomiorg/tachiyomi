package eu.kanade.tachiyomi.widget;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import rx.functions.Action0;

public class EndlessGridScrollListener extends RecyclerView.OnScrollListener {

    private int previousTotal = 0; // The total number of items in the dataset after the last load
    private boolean loading = true; // True if we are still waiting for the last set of data to load.
    private int visibleThreshold = 5; // The minimum amount of items to have below your current scroll position before loading more.
    int firstVisibleItem, visibleItemCount, totalItemCount;

    private GridLayoutManager layoutManager;

    private Action0 requestNext;

    public EndlessGridScrollListener(GridLayoutManager layoutManager, Action0 requestNext) {
        this.layoutManager = layoutManager;
        this.requestNext = requestNext;
    }

    public void resetScroll() {
        previousTotal = 0;
        loading = true;
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        visibleItemCount = recyclerView.getChildCount();
        totalItemCount = layoutManager.getItemCount();
        firstVisibleItem = layoutManager.findFirstVisibleItemPosition();

        if (loading && (totalItemCount > previousTotal)) {
            loading = false;
            previousTotal = totalItemCount;
        }
        if (!loading && (totalItemCount - visibleItemCount)
                <= (firstVisibleItem + visibleThreshold)) {
            // End has been reached
            requestNext.call();
            loading = true;
        }
    }

}