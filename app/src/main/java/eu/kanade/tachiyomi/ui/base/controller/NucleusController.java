package eu.kanade.tachiyomi.ui.base.controller;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.bluelinelabs.conductor.Controller;

import eu.kanade.tachiyomi.ui.base.presenter.ConductorPresenter;

public abstract class NucleusController<P extends ConductorPresenter> extends BaseController {

    private static final String PRESENTER_STATE_KEY = "presenter_state";

    private PresenterDelegate delegate = new PresenterDelegate();

    public NucleusController() {
        super();
        init();
    }

    public NucleusController(Bundle args) {
        super(args);
        init();
    }

    private void init() {
        addLifecycleListener(new Lifecycle());
    }

    public P getPresenter() {
        return delegate.getPresenter();
    }

    @NonNull
    protected abstract P createPresenter();

    private class PresenterDelegate {

        @Nullable private P presenter;
        @Nullable private Bundle bundle;
        private boolean presenterHasView = false;

        public P getPresenter() {
            if (presenter == null) {
                presenter = createPresenter();
                presenter.create(bundle == null ? null : bundle.getBundle(PRESENTER_STATE_KEY));
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
            this.bundle = presenterState;
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

    private class Lifecycle extends Controller.LifecycleListener {

        @Override
        public void postAttach(@NonNull Controller controller, @NonNull View view) {
            //noinspection unchecked
            getPresenter().takeView(NucleusController.this);
            delegate.onTakeView(NucleusController.this);
        }

        @Override
        public void preDetach(@NonNull Controller controller, @NonNull View view) {
            getPresenter().dropView();
            delegate.onDropView();
        }

        @Override
        public void preDestroy(@NonNull Controller controller) {
            getPresenter().destroy();
            delegate.onDestroy();
        }

        @Override
        public void onSaveInstanceState(@NonNull Controller controller, @NonNull Bundle outState) {
            Bundle presenterBundle = new Bundle();
            getPresenter().save(presenterBundle);
            outState.putBundle(PRESENTER_STATE_KEY, delegate.onSaveInstanceState());
        }

        @Override
        public void onRestoreInstanceState(@NonNull Controller controller, @NonNull Bundle savedInstanceState) {
            delegate.onRestoreInstanceState(savedInstanceState.getBundle(PRESENTER_STATE_KEY));
        }
    }

}
