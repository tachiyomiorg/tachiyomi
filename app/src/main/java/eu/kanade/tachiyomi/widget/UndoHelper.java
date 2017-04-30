/*
 * Copyright 2016 Davide Steduto
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.kanade.tachiyomi.widget;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;

/**
 * Helper to simplify the Undo operation with FlexibleAdapter.
 *
 * @author Davide Steduto
 * @since 30/04/2016
 */
@SuppressWarnings("WeakerAccess")
public class UndoHelper extends Snackbar.Callback {

    /**
     * Default undo-timeout of 5''.
     */
    public static final int UNDO_TIMEOUT = 5000;
    /**
     * Indicates that the Confirmation Listener (Undo and Delete) will perform a deletion.
     */
    public static final int ACTION_REMOVE = 0;
    /**
     * Indicates that the Confirmation Listener (Undo and Delete) will perform an update.
     */
    public static final int ACTION_UPDATE = 1;

    /**
     * Annotation interface for Undo actions.
     */
    @IntDef({ACTION_REMOVE, ACTION_UPDATE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Action {
    }

    @Action
    private int mAction = ACTION_REMOVE;
    private List<Integer> mPositions = null;
    private Object mPayload = null;
    private FlexibleAdapter mAdapter;
    private Snackbar mSnackbar = null;
    private OnActionListener mActionListener;
    private OnUndoListener mUndoListener;
    private @ColorInt int mActionTextColor = Color.TRANSPARENT;


    /**
     * Default constructor.
     * <p>By calling this constructor, {@link FlexibleAdapter#setPermanentDelete(boolean)}
     * is set {@code false} automatically.
     *
     * @param adapter      the instance of {@code FlexibleAdapter}
     * @param undoListener the callback for the Undo and Delete confirmation
     */
    public UndoHelper(FlexibleAdapter adapter, OnUndoListener undoListener) {
        this.mAdapter = adapter;
        this.mUndoListener = undoListener;
        adapter.setPermanentDelete(false);
    }

    /**
     * Sets the payload to inform other linked items about the change in action.
     *
     * @param payload any non-null user object to notify the parent (the payload will be
     *                therefore passed to the bind method of the parent ViewHolder),
     *                pass null to <u>not</u> notify the parent
     * @return this object, so it can be chained
     */
    public UndoHelper withPayload(Object payload) {
        this.mPayload = payload;
        return this;
    }

    /**
     * By default {@link UndoHelper#ACTION_REMOVE} is performed.
     *
     * @param action         the action, one of {@link UndoHelper#ACTION_REMOVE}, {@link UndoHelper#ACTION_UPDATE}
     * @param actionListener the listener for the custom action to perform before the deletion
     * @return this object, so it can be chained
     */
    public UndoHelper withAction(@Action int action, @NonNull OnActionListener actionListener) {
        this.mAction = action;
        this.mActionListener = actionListener;
        return this;
    }

    /**
     * Sets the text color of the action.
     *
     * @param color the color for the action button
     * @return this object, so it can be chained
     */
    public UndoHelper withActionTextColor(@ColorInt int color) {
        this.mActionTextColor = color;
        return this;
    }

    /**
     * As {@link #remove(List, View, CharSequence, CharSequence, int)} but with String
     * resources instead of CharSequence.
     */
    public void remove(List<Integer> positions, @NonNull View mainView,
                       @StringRes int messageStringResId, @StringRes int actionStringResId,
                       @IntRange(from = -1) int undoTime) {
        Context context = mainView.getContext();
        remove(positions, mainView, context.getString(messageStringResId),
                context.getString(actionStringResId), undoTime);
    }

    /**
     * Performs the action on the specified positions and displays a SnackBar to Undo
     * the operation. To customize the UPDATE event, please set a custom listener with
     * {@link #withAction(int, OnActionListener)} method.
     * <p>By default the DELETE action will be performed.</p>
     *
     * @param positions  the position to delete or update
     * @param mainView   the view to find a parent from
     * @param message    the text to show. Can be formatted text
     * @param actionText the action text to display
     * @param undoTime   How long to display the message. Either {@link Snackbar#LENGTH_SHORT} or
     *                   {@link Snackbar#LENGTH_LONG} or any custom Integer.
     * @see #remove(List, View, int, int, int)
     */
    @SuppressWarnings("WrongConstant")
    public void remove(List<Integer> positions, @NonNull View mainView,
                       CharSequence message, CharSequence actionText,
                       @IntRange(from = -1) int undoTime) {
        this.mPositions = positions;
        Snackbar snackbar;
        if (!mAdapter.isPermanentDelete()) {
            snackbar = Snackbar.make(mainView, message, undoTime > 0 ? undoTime + 400 : undoTime)
                    .setAction(actionText, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mUndoListener != null)
                                mUndoListener.onUndoConfirmed(mAction);
                        }
                    });
        } else {
            snackbar = Snackbar.make(mainView, message, undoTime);
        }
        if (mActionTextColor != Color.TRANSPARENT) {
            snackbar.setActionTextColor(mActionTextColor);
        }
        mSnackbar = snackbar;
        snackbar.addCallback(this);
        snackbar.show();
    }

    public void dismissNow() {
        if (mSnackbar != null) {
            mSnackbar.removeCallback(this);
            mSnackbar.dismiss();
            onDismissed(mSnackbar, Snackbar.Callback.DISMISS_EVENT_MANUAL);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDismissed(Snackbar snackbar, int event) {
        if (mAdapter.isPermanentDelete()) return;
        switch (event) {
            case DISMISS_EVENT_SWIPE:
            case DISMISS_EVENT_MANUAL:
            case DISMISS_EVENT_TIMEOUT:
                if (mUndoListener != null)
                    mUndoListener.onDeleteConfirmed(mAction);
                mAdapter.emptyBin();
                mSnackbar = null;
            case DISMISS_EVENT_CONSECUTIVE:
            case DISMISS_EVENT_ACTION:
            default:
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onShown(Snackbar snackbar) {
        boolean consumed = false;
        // Perform the action before deletion
        if (mActionListener != null) consumed = mActionListener.onPreAction();
        // Remove selected items from Adapter list after SnackBar is shown
        if (!consumed) mAdapter.removeItems(mPositions, mPayload);
        // Perform the action after the deletion
        if (mActionListener != null) mActionListener.onPostAction();
        // Here, we can notify the callback only in case of permanent deletion
        if (mAdapter.isPermanentDelete() && mUndoListener != null)
            mUndoListener.onDeleteConfirmed(mAction);
    }

    /**
     * Basic implementation of {@link OnActionListener} interface.
     * <p>Override the methods as your convenience.</p>
     */
    public static class SimpleActionListener implements OnActionListener {
        @Override
        public boolean onPreAction() {
            return false;
        }

        @Override
        public void onPostAction() {

        }
    }

    public interface OnActionListener {
        /**
         * Performs the custom action before item deletion.
         *
         * @return true if action has been consumed and should stop the deletion, false to
         * continue with the deletion
         */
        boolean onPreAction();

        /**
         * Performs custom action After items deletion. Useful to finish the action mode and perform
         * secondary custom actions.
         */
        void onPostAction();
    }

    /**
     * @since 30/04/2016
     */
    public interface OnUndoListener {
        /**
         * Called when Undo event is triggered. Perform custom action after restoration.
         * <p>Usually for a delete restoration you should call
         * {@link FlexibleAdapter#restoreDeletedItems()}.</p>
         *
         * @param action one of {@link UndoHelper#ACTION_REMOVE}, {@link UndoHelper#ACTION_UPDATE}
         */
        void onUndoConfirmed(int action);

        /**
         * Called when Undo timeout is over and action must be committed in the user Database.
         * <p>Due to Java Generic, it's too complicated and not well manageable if we pass the
         * List&lt;T&gt; object.<br/>
         * So, to get deleted items, use {@link FlexibleAdapter#getDeletedItems()} from the
         * implementation of this method.</p>
         *
         * @param action one of {@link UndoHelper#ACTION_REMOVE}, {@link UndoHelper#ACTION_UPDATE}
         */
        void onDeleteConfirmed(int action);
    }

}