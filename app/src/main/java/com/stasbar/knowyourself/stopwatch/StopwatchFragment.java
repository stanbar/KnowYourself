/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stasbar.knowyourself.stopwatch;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;


import com.stasbar.knowyourself.LogUtils;
import com.stasbar.knowyourself.R;
import com.stasbar.knowyourself.data.Stopwatch;
import com.stasbar.knowyourself.Utils;
import com.stasbar.knowyourself.data.ActivityItem;
import com.stasbar.knowyourself.data.DataModel;
import com.stasbar.knowyourself.data.Lap;
import com.stasbar.knowyourself.data.StopwatchListener;
import com.stasbar.knowyourself.timer.CountingTimerView;
import com.stasbar.knowyourself.uidata.UiDataModel;

import static android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

/**
 * Fragment that shows the stopwatch and recorded laps.
 */
public final class StopwatchFragment extends Fragment {
    /** Milliseconds between redraws. */
    private static final int REDRAW_PERIOD = 25;
    private static final String ACTIVITY_ITEM = "activity_item";


    /** Scheduled to update the stopwatch time and current lap time while stopwatch is running. */
    private final Runnable mTimeUpdateRunnable = new TimeUpdateRunnable();

    /** Updates the user interface in response to stopwatch changes. */
    private final StopwatchListener mStopwatchWatcher = new StopwatchWatcher();


    /** Draws the reference lap while the stopwatch is running. */
    private StopwatchCircleView mTime;


    /** Displays the current stopwatch time. */
    private CountingTimerView mTimeText;

    /** Number of laps the stopwatch is tracking. */
    private int mLapCount;

    private ActivityItem activityItem;

    /** The public no-arg constructor required by all fragments. */
    public StopwatchFragment() {
    }

    public static StopwatchFragment newInstance(ActivityItem activityItem) {
        StopwatchFragment fragment = new StopwatchFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ACTIVITY_ITEM, activityItem);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            activityItem = getArguments().getParcelable(ACTIVITY_ITEM);
        } else {
            throw new IllegalArgumentException("Arguments can not be null");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {

        final View v = inflater.inflate(R.layout.stopwatch_fragment, container, false);
        mTime = (StopwatchCircleView) v.findViewById(R.id.stopwatch_time);


        // Timer text serves as a virtual start/stop button.
        mTimeText = (CountingTimerView) v.findViewById(R.id.stopwatch_time_text);
        mTimeText.setShowBoundingCircle(mTime != null);
        mTimeText.setVirtualButtonEnabled(true);
        mTimeText.registerVirtualButtonAction(new ToggleStopwatchRunnable());

        DataModel.getDataModel().addStopwatchListener(mStopwatchWatcher);

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Synchronize the user interface with the data model.
        updateUI();
    }

    @Override
    public void onStop() {
        super.onStop();

        // Stop all updates while the fragment is not visible.
        stopUpdatingTime();
        mTimeText.blinkTimeStr(false);


        // Release the wake lock if it is currently held.
        releaseWakeLock();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        DataModel.getDataModel().removeStopwatchListener(mStopwatchWatcher);
    }

    public void onFabClick(@NonNull ImageView fab) {
        toggleStopwatchState();
    }



    public void onUpdateFab(@NonNull ImageView fab) {
        if (getStopwatch().isRunning()) {
            fab.setImageResource(R.drawable.ic_pause_white_24dp);
            fab.setContentDescription(fab.getResources().getString(R.string.sw_pause_button));
        } else {
            fab.setImageResource(R.drawable.ic_start_white_24dp);
            fab.setContentDescription(fab.getResources().getString(R.string.sw_start_button));
        }
        fab.setVisibility(VISIBLE);
    }

    public void onUpdateFabButtons(@NonNull ImageButton left, @NonNull ImageButton right) {
        right.setImageResource(R.drawable.ic_share);
        right.setContentDescription(right.getResources().getString(R.string.sw_share_button));

        switch (getStopwatch().getState()) {
            case RESET:
                left.setEnabled(false);
                left.setVisibility(INVISIBLE);
                right.setVisibility(INVISIBLE);
                break;
            case RUNNING:
                final boolean canRecordLaps = canRecordMoreLaps();
                left.setImageResource(R.drawable.ic_lap);
                left.setContentDescription(left.getResources().getString(R.string.sw_lap_button));
                left.setEnabled(canRecordLaps);
                left.setVisibility(canRecordLaps ? VISIBLE : INVISIBLE);
                right.setVisibility(INVISIBLE);
                break;
            case PAUSED:
                left.setEnabled(true);
                left.setImageResource(R.drawable.ic_reset);
                left.setContentDescription(left.getResources().getString(R.string.sw_reset_button));
                left.setVisibility(VISIBLE);
                right.setVisibility(VISIBLE);
                break;
        }
    }

    public void onMorphFabButtons(@NonNull ImageButton left, @NonNull ImageButton right) {
        right.setImageResource(R.drawable.ic_share);
        right.setContentDescription(right.getResources().getString(R.string.sw_share_button));

        switch (getStopwatch().getState()) {
            case RESET:
                left.setEnabled(false);
                left.setVisibility(INVISIBLE);
                right.setVisibility(INVISIBLE);
                break;
            case RUNNING: {
                final boolean canRecordLaps = canRecordMoreLaps();
                left.setContentDescription(left.getResources().getString(R.string.sw_lap_button));
                left.setEnabled(canRecordLaps);
                left.setVisibility(canRecordLaps ? VISIBLE : INVISIBLE);
                right.setVisibility(INVISIBLE);
                final Drawable icon = left.getDrawable();
                if (icon instanceof Animatable) {
                    ((Animatable) icon).start();
                }
                break;
            }
            case PAUSED: {
                left.setEnabled(true);
                updateResetIcon(left);
                left.setContentDescription(left.getResources().getString(R.string.sw_reset_button));
                left.setVisibility(VISIBLE);
                right.setVisibility(VISIBLE);
                final Drawable icon = left.getDrawable();
                if (icon instanceof Animatable) {
                    ((Animatable) icon).start();
                }
                break;
            }
        }
    }





    private void updateResetIcon(ImageButton button) {
        if (Utils.isLMR1OrLater()) {
            button.setImageResource(R.drawable.ic_lap_reset_animation);
        } else {
            button.setImageResource(R.drawable.ic_reset);
        }
    }

    /**
     * Start the stopwatch.
     */
    private void doStart() {
        DataModel.getDataModel().startStopwatch();
    }

    /**
     * Pause the stopwatch.
     */
    private void doPause() {
        DataModel.getDataModel().pauseStopwatch();
    }

    /**
     * Reset the stopwatch.
     */
    private void doReset() {
        DataModel.getDataModel().resetStopwatch();
    }






    private void adjustWakeLock() {
        final boolean appInForeground = DataModel.getDataModel().isApplicationInForeground();
        if (getStopwatch().isRunning() && appInForeground) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            releaseWakeLock();
        }
    }

    private void releaseWakeLock() {
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * Either pause or start the stopwatch based on its current state.
     */
    private void toggleStopwatchState() {
        if (getStopwatch().isRunning()) {
            doPause();
        } else {
            doStart();
        }
    }

    private Stopwatch getStopwatch() {
        return DataModel.getDataModel().getStopwatch();
    }

    private boolean canRecordMoreLaps() {
        return DataModel.getDataModel().canAddMoreLaps();
    }

    /**
     * Post the first runnable to update times within the UI. It will reschedule itself as needed.
     */
    private void startUpdatingTime() {
        // Ensure only one copy of the runnable is ever scheduled by first stopping updates.
        stopUpdatingTime();
        mTimeText.post(mTimeUpdateRunnable);
    }

    /**
     * Remove the runnable that updates times within the UI.
     */
    private void stopUpdatingTime() {
        mTimeText.removeCallbacks(mTimeUpdateRunnable);
    }

    /**
     * Update all time displays based on a single snapshot of the stopwatch progress. This includes
     * the stopwatch time drawn in the circle, the current lap time and the total elapsed time in
     * the list of laps.
     */
    private void updateTime() {
        // Compute the total time of the stopwatch.
        final Stopwatch stopwatch = getStopwatch();
        final long totalTime = stopwatch.getTotalTime();

        // Update the total time display.
        mTimeText.setTime(totalTime, true);

    }


    /**
     * Synchronize the UI state with the model data.
     */
    private void updateUI() {
        LogUtils.d("updateUI");
        adjustWakeLock();

        // Draw the latest stopwatch and current lap times.
        updateTime();

        if (mTime != null) {
            mTime.update();
        }

        // Start updates if the stopwatch is running.
        final Stopwatch stopwatch = getStopwatch();
        if (stopwatch.isRunning()) {
            startUpdatingTime();
        }

        // Blink text iff the stopwatch is paused.
        mTimeText.blinkTimeStr(stopwatch.isPaused());

    }

    /**
     * This runnable periodically updates times throughout the UI. It stops these updates when the
     * stopwatch is no longer running.
     */
    private final class TimeUpdateRunnable implements Runnable {
        @Override
        public void run() {
            final long startTime = SystemClock.elapsedRealtime();

            updateTime();

            if (getStopwatch().isRunning()) {
                // Try to maintain a consistent period of time between redraws.
                final long endTime = SystemClock.elapsedRealtime();
                final long delay = Math.max(0, startTime + REDRAW_PERIOD - endTime);

                mTimeText.postDelayed(this, delay);
            }
        }
    }

    /**
     * Tapping the stopwatch text also toggles the stopwatch state, just like the fab.
     */
    private final class ToggleStopwatchRunnable implements Runnable {
        @Override
        public void run() {
            toggleStopwatchState();
        }
    }


    /**
     * Update the user interface in response to a stopwatch change.
     */
    private class StopwatchWatcher implements StopwatchListener {
        @Override
        public void stopwatchUpdated(Stopwatch before, Stopwatch after) {
            LogUtils.d("stopwatchUpdated");
            if (DataModel.getDataModel().isApplicationInForeground()) {

                updateUI();
            }
        }

        @Override
        public void lapAdded(Lap lap) {
        }
    }


    /**
     * Draws a tinting gradient over the bottom of the stopwatch laps list. This reduces the
     * contrast between floating buttons and the laps list content.
     */
    private static final class GradientItemDecoration extends RecyclerView.ItemDecoration {

        //  0% -  25% of gradient length -> opacity changes from 0% to 50%
        // 25% -  90% of gradient length -> opacity changes from 50% to 100%
        // 90% - 100% of gradient length -> opacity remains at 100%
        private static final int[] ALPHAS = {
                0x00, // 0%
                0x1A, // 10%
                0x33, // 20%
                0x4D, // 30%
                0x66, // 40%
                0x80, // 50%
                0x89, // 53.8%
                0x93, // 57.6%
                0x9D, // 61.5%
                0xA7, // 65.3%
                0xB1, // 69.2%
                0xBA, // 73.0%
                0xC4, // 76.9%
                0xCE, // 80.7%
                0xD8, // 84.6%
                0xE2, // 88.4%
                0xEB, // 92.3%
                0xF5, // 96.1%
                0xFF, // 100%
                0xFF, // 100%
                0xFF, // 100%
        };

        /**
         * A reusable array of control point colors that define the gradient. It is based on the
         * background color of the window and thus recomputed each time that color is changed.
         */
        private final int[] mGradientColors = new int[ALPHAS.length];

        /** The drawable that produces the tinting gradient effect of this decoration. */
        private final GradientDrawable mGradient = new GradientDrawable();

        /** The height of the gradient; sized relative to the fab height. */
        private final int mGradientHeight;

        public GradientItemDecoration(Context context) {
            mGradient.setOrientation(TOP_BOTTOM);
            updateGradientColors(UiDataModel.getUiDataModel().getWindowBackgroundColor());

            final Resources resources = context.getResources();
            final float fabHeight = resources.getDimensionPixelSize(R.dimen.fab_height);
            mGradientHeight = Math.round(fabHeight * 1.2f);
        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
            super.onDrawOver(c, parent, state);

            final int w = parent.getWidth();
            final int h = parent.getHeight();

            mGradient.setBounds(0, h - mGradientHeight, w, h);
            mGradient.draw(c);
        }

        /**
         * Given a {@code baseColor}, compute a gradient of tinted colors that define the fade
         * effect to apply to the bottom of the lap list.
         *
         * @param baseColor a base color to which the gradient tint should be applied
         */
        public void updateGradientColors(@ColorInt int baseColor) {
            // Compute the tinted colors that form the gradient.
            for (int i = 0; i < mGradientColors.length; i++) {
                mGradientColors[i] = ColorUtils.setAlphaComponent(baseColor, ALPHAS[i]);
            }

            // Set the gradient colors into the drawable.
            mGradient.setColors(mGradientColors);
        }
    }
}