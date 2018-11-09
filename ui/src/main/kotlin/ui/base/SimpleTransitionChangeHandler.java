package ui.base;

import android.transition.Transition;
import android.transition.TransitionSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.bluelinelabs.conductor.changehandler.TransitionChangeHandler;
import com.bluelinelabs.conductor.internal.TransitionUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A TransitionChangeHandler that facilitates using different Transitions for the entering view, the exiting view,
 * and shared elements between the two.
 */
// Much of this class is based on FragmentTransition.java and FragmentTransitionCompat21.java from the Android support library
public abstract class SimpleTransitionChangeHandler extends TransitionChangeHandler {

  @Nullable Transition exitTransition;
  @Nullable Transition enterTransition;

  @NonNull
  @Override
  protected final Transition getTransition(@NonNull ViewGroup container, @Nullable View from, @Nullable View to, boolean isPush) {
    exitTransition = getExitTransition(container, from, to, isPush);
    enterTransition = getEnterTransition(container, from, to, isPush);

    if (enterTransition == null && exitTransition == null) {
      throw new IllegalStateException("SimpleTransitionChangeHandler must have at least one transaction.");
    }

    return mergeTransitions(isPush);
  }

  @Override
  public void prepareForTransition(@NonNull final ViewGroup container, @Nullable final View from, @Nullable final View to, @NonNull final Transition transition, final boolean isPush, @NonNull final OnTransitionPreparedListener onTransitionPreparedListener) {
    OnTransitionPreparedListener listener = new OnTransitionPreparedListener() {
      @Override
      public void onPrepared() {
        configureTransition(container, from, to, transition, isPush);
        onTransitionPreparedListener.onPrepared();
      }
    };

    listener.onPrepared();
  }

  void configureTransition(@NonNull final ViewGroup container, @Nullable View from, @Nullable View to, @NonNull final Transition transition, boolean isPush) {
    final View nonExistentView = new View(container.getContext());

    List<View> exitingViews = exitTransition != null ? configureEnteringExitingViews(exitTransition, from, nonExistentView) : null;
    if (exitingViews == null || exitingViews.isEmpty()) {
      exitTransition = null;
    }

    if (enterTransition != null) {
      enterTransition.addTarget(nonExistentView);
    }

    final List<View> enteringViews = new ArrayList<>();
    scheduleRemoveTargets(transition, enterTransition, enteringViews, exitTransition, exitingViews);
    scheduleTargetChange(container, to, nonExistentView, enteringViews, exitingViews);
  }

  private void scheduleTargetChange(@NonNull final ViewGroup container, @Nullable final View to, @NonNull final View nonExistentView,
                                    @NonNull final List<View> enteringViews, @Nullable final List<View> exitingViews) {
    OneShotPreDrawListener.add(true, container, new Runnable() {
      @Override
      public void run() {
        if (enterTransition != null) {
          enterTransition.removeTarget(nonExistentView);
          List<View> views = configureEnteringExitingViews(enterTransition, to, nonExistentView);
          enteringViews.addAll(views);
        }

        if (exitingViews != null) {
          if (exitTransition != null) {
            List<View> tempExiting = new ArrayList<>();
            tempExiting.add(nonExistentView);
            TransitionUtils.replaceTargets(exitTransition, exitingViews, tempExiting);
          }
          exitingViews.clear();
          exitingViews.add(nonExistentView);
        }
      }
    });
  }

  private Transition mergeTransitions(boolean isPush) {
    boolean overlap = enterTransition == null || exitTransition == null || allowTransitionOverlap(isPush);

    if (overlap) {
      return TransitionUtils.mergeTransitions(TransitionSet.ORDERING_TOGETHER, exitTransition, enterTransition);
    } else {
      Transition staggered = TransitionUtils.mergeTransitions(TransitionSet.ORDERING_SEQUENTIAL, exitTransition, enterTransition);
      return TransitionUtils.mergeTransitions(TransitionSet.ORDERING_TOGETHER, staggered);
    }
  }

  @NonNull List<View> configureEnteringExitingViews(@NonNull Transition transition, @Nullable View view, @NonNull View nonExistentView) {
    List<View> viewList = new ArrayList<>();
    if (view != null) {
      captureTransitioningViews(viewList, view);
    }
    if (!viewList.isEmpty()) {
      viewList.add(nonExistentView);
      TransitionUtils.addTargets(transition, viewList);
    }
    return viewList;
  }

  private void captureTransitioningViews(@NonNull List<View> transitioningViews, @NonNull View view) {
    if (view.getVisibility() == View.VISIBLE) {
      if (view instanceof ViewGroup) {
        ViewGroup viewGroup = (ViewGroup) view;
        if (viewGroup.isTransitionGroup()) {
          transitioningViews.add(viewGroup);
        } else {
          int count = viewGroup.getChildCount();
          for (int i = 0; i < count; i++) {
            View child = viewGroup.getChildAt(i);
            captureTransitioningViews(transitioningViews, child);
          }
        }
      } else {
        transitioningViews.add(view);
      }
    }
  }

  private void scheduleRemoveTargets(@NonNull final Transition overallTransition,
                                     @Nullable final Transition enterTransition, @Nullable final List<View> enteringViews,
                                     @Nullable final Transition exitTransition, @Nullable final List<View> exitingViews) {
    overallTransition.addListener(new Transition.TransitionListener() {
      @Override
      public void onTransitionStart(Transition transition) {
        if (enterTransition != null && enteringViews != null) {
          TransitionUtils.replaceTargets(enterTransition, enteringViews, null);
        }
        if (exitTransition != null && exitingViews != null) {
          TransitionUtils.replaceTargets(exitTransition, exitingViews, null);
        }
      }

      @Override
      public void onTransitionEnd(Transition transition) { }

      @Override
      public void onTransitionCancel(Transition transition) { }

      @Override
      public void onTransitionPause(Transition transition) { }

      @Override
      public void onTransitionResume(Transition transition) { }
    });
  }

  /**
   * Should return the transition that will be used on the exiting ("from") view, if one is desired.
   */
  @Nullable
  public abstract Transition getExitTransition(@NonNull ViewGroup container, @Nullable View from, @Nullable View to, boolean isPush);

  /**
   * Should return the transition that will be used on the entering ("to") view, if one is desired.
   */
  @Nullable
  public abstract Transition getEnterTransition(@NonNull ViewGroup container, @Nullable View from, @Nullable View to, boolean isPush);

  /**
   * Should return whether or not the the exit transition and enter transition should overlap. If true,
   * the enter transition will start as soon as possible. Otherwise, the enter transition will wait until the
   * completion of the exit transition. Defaults to true.
   */
  public boolean allowTransitionOverlap(boolean isPush) {
    return true;
  }

  private static class OneShotPreDrawListener implements ViewTreeObserver.OnPreDrawListener, View.OnAttachStateChangeListener {

    private final View view;
    private ViewTreeObserver viewTreeObserver;
    private final Runnable runnable;
    private final boolean preDrawReturnValue;

    private OneShotPreDrawListener(boolean preDrawReturnValue, @NonNull View view, @NonNull Runnable runnable) {
      this.preDrawReturnValue = preDrawReturnValue;
      this.view = view;
      viewTreeObserver = view.getViewTreeObserver();
      this.runnable = runnable;
    }

    @NonNull
    public static OneShotPreDrawListener add(boolean preDrawReturnValue, @NonNull View view, @NonNull Runnable runnable) {
      OneShotPreDrawListener listener = new OneShotPreDrawListener(preDrawReturnValue, view, runnable);
      view.getViewTreeObserver().addOnPreDrawListener(listener);
      view.addOnAttachStateChangeListener(listener);
      return listener;
    }

    @Override
    public boolean onPreDraw() {
      removeListener();
      runnable.run();
      return preDrawReturnValue;
    }

    private void removeListener() {
      if (viewTreeObserver.isAlive()) {
        viewTreeObserver.removeOnPreDrawListener(this);
      } else {
        view.getViewTreeObserver().removeOnPreDrawListener(this);
      }
      view.removeOnAttachStateChangeListener(this);
    }

    @Override
    public void onViewAttachedToWindow(View v) {
      viewTreeObserver = v.getViewTreeObserver();
    }

    @Override
    public void onViewDetachedFromWindow(View v) {
      removeListener();
    }

  }

}
