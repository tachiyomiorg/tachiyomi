package eu.kanade.tachiyomi.ui.base.presenter;

import android.os.Bundle;
import android.support.annotation.Nullable;

import nucleus.factory.PresenterFactory;
import nucleus.presenter.Presenter;

public class NucleusConductorDelegate<P extends Presenter> {

    @Nullable private P presenter;
    @Nullable private Bundle bundle;
    private boolean presenterHasView = false;

    private PresenterFactory<P> factory;

    public NucleusConductorDelegate(PresenterFactory<P> creator) {
        this.factory = creator;
    }

    public P getPresenter() {
        if (presenter == null) {
            presenter = factory.createPresenter();
            presenter.create(bundle);
        }
        bundle = null;
        return presenter;
    }

    Bundle onSaveInstanceState() {
        Bundle bundle = new Bundle();
        getPresenter();
        if (presenter != null) {
            presenter.save(bundle);
        }
        return bundle;
    }

    void onRestoreInstanceState(Bundle presenterState) {
        if (presenter != null)
            throw new IllegalArgumentException("onRestoreInstanceState() should be called before onResume()");
        bundle = presenterState;
    }

    void onTakeView(Object view) {
        getPresenter();
        if (presenter != null && !presenterHasView) {
            //noinspection unchecked
            presenter.takeView(view);
            presenterHasView = true;
        }
    }

    void onDropView() {
        if (presenter != null && presenterHasView) {
            presenter.dropView();
            presenterHasView = false;
        }
    }

    void onDestroy() {
        if (presenter != null) {
            presenter.destroy();
            presenter = null;
        }
    }
}
