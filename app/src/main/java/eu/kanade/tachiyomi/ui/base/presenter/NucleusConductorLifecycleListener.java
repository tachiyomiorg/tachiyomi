package eu.kanade.tachiyomi.ui.base.presenter;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import com.bluelinelabs.conductor.Controller;

public class NucleusConductorLifecycleListener extends Controller.LifecycleListener {

    private static final String PRESENTER_STATE_KEY = "presenter_state";

    private NucleusConductorDelegate delegate;

    public NucleusConductorLifecycleListener(NucleusConductorDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void postCreateView(@NonNull Controller controller, @NonNull View view) {
        delegate.onTakeView(controller);
    }

    @Override
    public void preDestroyView(@NonNull Controller controller, @NonNull View view) {
        delegate.onDropView();
    }

    @Override
    public void preDestroy(@NonNull Controller controller) {
        delegate.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Controller controller, @NonNull Bundle outState) {
        outState.putBundle(PRESENTER_STATE_KEY, delegate.onSaveInstanceState());
    }

    @Override
    public void onRestoreInstanceState(@NonNull Controller controller, @NonNull Bundle savedInstanceState) {
        delegate.onRestoreInstanceState(savedInstanceState.getBundle(PRESENTER_STATE_KEY));
    }

}
