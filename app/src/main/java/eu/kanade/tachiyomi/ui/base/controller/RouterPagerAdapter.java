package eu.kanade.tachiyomi.ui.base.controller;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import com.bluelinelabs.conductor.Controller;
import com.bluelinelabs.conductor.Router;
import com.bluelinelabs.conductor.RouterTransaction;

import java.util.ArrayList;
import java.util.List;

/**
 * An adapter for ViewPagers that uses Routers as pages
 */
public abstract class RouterPagerAdapter extends PagerAdapter {

    private static final String KEY_SAVED_PAGES = "RouterPagerAdapter.savedStates";
    private static final String KEY_MAX_PAGES_TO_STATE_SAVE = "RouterPagerAdapter.maxPagesToStateSave";
    private static final String KEY_SAVE_PAGE_HISTORY = "RouterPagerAdapter.savedPageHistory";

    private final Controller host;
    private int maxPagesToStateSave = Integer.MAX_VALUE;
    private SparseArray<Bundle> savedPages = new SparseArray<>();
    private SparseArray<Router> visibleRouters = new SparseArray<>();
    private ArrayList<Integer> savedPageHistory = new ArrayList<>();
    private Router primaryRouter;

    /**
     * Creates a new RouterPagerAdapter using the passed host.
     */
    public RouterPagerAdapter(@NonNull Controller host) {
        this.host = host;
    }

    /**
     * Called when a router is instantiated. Here the router's root should be set if needed.
     *
     * @param router   The router used for the page
     * @param position The page position to be instantiated.
     */
    public abstract void configureRouter(@NonNull Router router, int position);

    /**
     * Sets the maximum number of pages that will have their states saved. When this number is exceeded,
     * the page that was state saved least recently will have its state removed from the save data.
     */
    public void setMaxPagesToStateSave(int maxPagesToStateSave) {
        if (maxPagesToStateSave < 0) {
            throw new IllegalArgumentException("Only positive integers may be passed for maxPagesToStateSave.");
        }

        this.maxPagesToStateSave = maxPagesToStateSave;

        ensurePagesSaved();
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        final String name = makeRouterName(container.getId(), getItemId(position));

        Router router = host.getChildRouter(container, name);
        if (!router.hasRootController()) {
            Bundle routerSavedState = savedPages.get(position);

            if (routerSavedState != null) {
                router.restoreInstanceState(routerSavedState);
                savedPages.remove(position);
            }
        }

        router.rebindIfNeeded();
        configureRouter(router, position);

        if (router != primaryRouter) {
            for (RouterTransaction transaction : router.getBackstack()) {
                transaction.controller().setOptionsMenuHidden(true);
            }
        }

        visibleRouters.put(position, router);
        return router;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        Router router = (Router)object;

        Bundle savedState = new Bundle();
        router.saveInstanceState(savedState);
        savedPages.put(position, savedState);

        savedPageHistory.remove((Integer)position);
        savedPageHistory.add(position);

        ensurePagesSaved();

        host.removeChildRouter(router);

        visibleRouters.remove(position);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        Router router = (Router)object;
        if (router != primaryRouter) {
            if (primaryRouter != null) {
                for (RouterTransaction transaction : primaryRouter.getBackstack()) {
                    transaction.controller().setOptionsMenuHidden(true);
                }
            }
            if (router != null) {
                for (RouterTransaction transaction : router.getBackstack()) {
                    transaction.controller().setOptionsMenuHidden(false);
                }
            }
            primaryRouter = router;
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        Router router = (Router)object;
        final List<RouterTransaction> backstack = router.getBackstack();
        for (RouterTransaction transaction : backstack) {
            if (transaction.controller().getView() == view) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Parcelable saveState() {
        Bundle bundle = new Bundle();
        bundle.putSparseParcelableArray(KEY_SAVED_PAGES, savedPages);
        bundle.putInt(KEY_MAX_PAGES_TO_STATE_SAVE, maxPagesToStateSave);
        bundle.putIntegerArrayList(KEY_SAVE_PAGE_HISTORY, savedPageHistory);
        return bundle;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
        Bundle bundle = (Bundle)state;
        if (state != null) {
            savedPages = bundle.getSparseParcelableArray(KEY_SAVED_PAGES);
            maxPagesToStateSave = bundle.getInt(KEY_MAX_PAGES_TO_STATE_SAVE);
            savedPageHistory = bundle.getIntegerArrayList(KEY_SAVE_PAGE_HISTORY);
        }
    }

    /**
     * Returns the already instantiated Router in the specified position or {@code null} if there
     * is no router associated with this position.
     */
    @Nullable
    public Router getRouter(int position) {
        return visibleRouters.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    SparseArray<Bundle> getSavedPages() {
        return savedPages;
    }

    private void ensurePagesSaved() {
        while (savedPages.size() > maxPagesToStateSave) {
            int positionToRemove = savedPageHistory.remove(0);
            savedPages.remove(positionToRemove);
        }
    }

    private static String makeRouterName(int viewId, long id) {
        return viewId + ":" + id;
    }

}