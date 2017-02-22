/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.stasbar.knowyourself;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import com.stasbar.knowyourself.actionbarmenu.MenuItemControllerFactory;
import com.stasbar.knowyourself.actionbarmenu.OptionsMenuManager;
import com.stasbar.knowyourself.actionbarmenu.SettingsMenuItemController;
import com.stasbar.knowyourself.alarms.AlarmStateManager;
import com.stasbar.knowyourself.data.DataModel;
import com.stasbar.knowyourself.widget.toast.SnackbarManager;

import static android.app.NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED;
import static android.app.NotificationManager.INTERRUPTION_FILTER_NONE;
import static android.media.AudioManager.FLAG_SHOW_UI;
import static android.media.AudioManager.STREAM_ALARM;
import static android.media.RingtoneManager.TYPE_ALARM;
import static android.provider.Settings.System.CONTENT_URI;
import static android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;

/**
 * The main activity of the application which displays 4 different tabs contains alarms, world
 * clocks, timers and a stopwatch.
 */
public class DeskClock extends BaseActivity {

    /**
     * The Uri to the settings entry that stores alarm stream volume.
     */
    private static final Uri VOLUME_URI = Uri.withAppendedPath(CONTENT_URI, "volume_alarm_speaker");

    /**
     * The intent filter that identifies do-not-disturb change broadcasts.
     */
    @SuppressLint("NewApi")
    private static final IntentFilter DND_CHANGE_FILTER
            = new IntentFilter(ACTION_INTERRUPTION_FILTER_CHANGED);
    private static final String TAG = DeskClock.class.getName();


    /**
     * Coordinates handling of context menu items.
     */
    private final OptionsMenuManager mOptionsMenuManager = new OptionsMenuManager();

    /**
     * Displays a snackbar explaining that the system default alarm ringtone is silent.
     */
    private final Runnable mShowSilentAlarmSnackbarRunnable = new ShowSilentAlarmSnackbarRunnable();

    /**
     * Observes default alarm ringtone changes while the app is in the foreground.
     */
    private final ContentObserver mAlarmRingtoneChangeObserver = new AlarmRingtoneChangeObserver();

    /**
     * Displays a snackbar explaining that the alarm volume is muted.
     */
    private final Runnable mShowMutedVolumeSnackbarRunnable = new ShowMutedVolumeSnackbarRunnable();

    /**
     * Observes alarm volume changes while the app is in the foreground.
     */
    private final ContentObserver mAlarmVolumeChangeObserver = new AlarmVolumeChangeObserver();

    /**
     * Displays a snackbar explaining that do-not-disturb is blocking alarms.
     */
    private final Runnable mShowDNDBlockingSnackbarRunnable = new ShowDNDBlockingSnackbarRunnable();

    /**
     * Observes do-not-disturb changes while the app is in the foreground.
     */
    private final BroadcastReceiver mDoNotDisturbChangeReceiver = new DoNotDisturbChangeReceiver();

    /**
     * Used to query the alarm volume and display the system control to change the alarm volume.
     */
    private AudioManager mAudioManager;

    /**
     * Used to query the do-not-disturb setting value, also called "interruption filter".
     */
    private NotificationManager mNotificationManager;

    /**
     * {@code true} permits the muted alarm volume snackbar to show when starting this activity.
     */
    private boolean mShowSilencedAlarmsSnackbar;

    /**
     * The view to which snackbar items are anchored.
     */
    private View mSnackbarAnchor;

    /**
     * {@code true} when a settings change necessitates recreating this activity.
     */
    private boolean mRecreateActivity;

    @Override
    public void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);

        // Fragments may query the latest intent for information, so update the intent.
        setIntent(newIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.desk_clock);

        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Don't show the volume muted snackbar on rotations.
        mShowSilencedAlarmsSnackbar = savedInstanceState == null;
        mSnackbarAnchor = findViewById(R.id.coordinator);

        // Configure the toolbar.
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Configure the menu item controllers add behavior to the toolbar.
        mOptionsMenuManager
                .addMenuItemController(new SettingsMenuItemController(this))
                .addMenuItemController(MenuItemControllerFactory.getInstance()
                        .buildMenuItemControllers(this));

        // Inflate the menu during creation to avoid a double layout pass. Otherwise, the menu
        // inflation occurs *after* the initial draw and a second layout pass adds in the menu.
        onCreateOptionsMenu(toolbar.getMenu());


        // Update the next alarm time on app startup because the user might have altered the data.
        AlarmStateManager.updateNextAlarm(this);

        if (savedInstanceState == null) {
            // Set the background color to initially match the theme value so that we can
            // smoothly transition to the dynamic color.
            final int backgroundColor = ContextCompat.getColor(this, R.color.default_background);
            adjustAppColor(backgroundColor, false /* animate */);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mShowSilencedAlarmsSnackbar) {
            if (isDoNotDisturbBlockingAlarms()) {
                mSnackbarAnchor.postDelayed(mShowDNDBlockingSnackbarRunnable, SECOND_IN_MILLIS);
            } else if (isAlarmStreamMuted()) {
                mSnackbarAnchor.postDelayed(mShowMutedVolumeSnackbarRunnable, SECOND_IN_MILLIS);
            } else if (isSystemAlarmRingtoneSilent()) {
                mSnackbarAnchor.postDelayed(mShowSilentAlarmSnackbarRunnable, SECOND_IN_MILLIS);
            }
        }

        // Subsequent starts of this activity should show the snackbar by default.
        mShowSilencedAlarmsSnackbar = true;

        final ContentResolver cr = getContentResolver();
        // Watch for system alarm ringtone changes while the app is in the foreground.
        cr.registerContentObserver(DEFAULT_ALARM_ALERT_URI, false, mAlarmRingtoneChangeObserver);

        // Watch for alarm volume changes while the app is in the foreground.
        cr.registerContentObserver(VOLUME_URI, false, mAlarmVolumeChangeObserver);

        if (Utils.isMOrLater()) {
            // Watch for do-not-disturb changes while the app is in the foreground.
            registerReceiver(mDoNotDisturbChangeReceiver, DND_CHANGE_FILTER);
        }

        DataModel.getDataModel().setApplicationInForeground(true);
    }


    @Override
    protected void onPostResume() {
        super.onPostResume();

        if (mRecreateActivity) {
            mRecreateActivity = false;
        }
    }


    @Override
    protected void onStop() {
        LogUtils.d(TAG,"onStop");
        if (!isChangingConfigurations()) {
            LogUtils.d(TAG,"onStop setApplicationInForeground -> FALSE");
            DataModel.getDataModel().setApplicationInForeground(false);
        }


        // Stop watching for system alarm ringtone changes while the app is in the background.
        getContentResolver().unregisterContentObserver(mAlarmRingtoneChangeObserver);

        // Stop watching for alarm volume changes while the app is in the background.
        getContentResolver().unregisterContentObserver(mAlarmVolumeChangeObserver);

        if (Utils.isMOrLater()) {
            // Stop watching for do-not-disturb changes while the app is in the background.
            unregisterReceiver(mDoNotDisturbChangeReceiver);
        }

        // Remove any scheduled work to show snackbars; it is no longer relevant.
        mSnackbarAnchor.removeCallbacks(mShowSilentAlarmSnackbarRunnable);
        mSnackbarAnchor.removeCallbacks(mShowDNDBlockingSnackbarRunnable);
        mSnackbarAnchor.removeCallbacks(mShowMutedVolumeSnackbarRunnable);
        super.onStop();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mOptionsMenuManager.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        mOptionsMenuManager.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mOptionsMenuManager.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Recreate the activity if any settings have been changed
        if (requestCode == SettingsMenuItemController.REQUEST_CHANGE_SETTINGS
                && resultCode == RESULT_OK) {
            mRecreateActivity = true;
        }
    }


    private boolean isSystemAlarmRingtoneSilent() {
        return RingtoneManager.getActualDefaultRingtoneUri(this, TYPE_ALARM) == null;
    }

    private void showSilentRingtoneSnackbar() {
        final OnClickListener changeClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        };

        SnackbarManager.show(
                createSnackbar(R.string.silent_default_alarm_ringtone)
                        .setAction(R.string.change_default_alarm_ringtone, changeClickListener)
        );
    }

    private boolean isAlarmStreamMuted() {
        return mAudioManager.getStreamVolume(STREAM_ALARM) <= 0;
    }

    private void showAlarmVolumeMutedSnackbar() {
        final OnClickListener unmuteClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set the alarm volume to ~30% of max and show the slider UI.
                final int index = mAudioManager.getStreamMaxVolume(STREAM_ALARM) / 3;
                mAudioManager.setStreamVolume(STREAM_ALARM, index, FLAG_SHOW_UI);
            }
        };

        SnackbarManager.show(
                createSnackbar(R.string.alarm_volume_muted)
                        .setAction(R.string.unmute_alarm_volume, unmuteClickListener)
        );
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean isDoNotDisturbBlockingAlarms() {
        if (!Utils.isMOrLater()) {
            return false;
        }
        return mNotificationManager.getCurrentInterruptionFilter() == INTERRUPTION_FILTER_NONE;
    }

    private void showDoNotDisturbIsBlockingAlarmsSnackbar() {
        SnackbarManager.show(createSnackbar(R.string.alarms_blocked_by_dnd));
    }

    /**
     * @return a Snackbar that displays the message with the given id for 5 seconds
     */
    private Snackbar createSnackbar(@StringRes int messageId) {
        return Snackbar.make(mSnackbarAnchor, messageId, 5000 /* duration */);
    }


    /**
     * Displays a snackbar that indicates the system default alarm ringtone currently silent and
     * offers an action that displays the system alarm ringtone setting to adjust it.
     */
    private final class ShowSilentAlarmSnackbarRunnable implements Runnable {
        @Override
        public void run() {
            showSilentRingtoneSnackbar();
        }
    }

    /**
     * Displays a snackbar that indicates the alarm volume is currently muted and offers an action
     * that displays the system volume control to adjust it.
     */
    private final class ShowMutedVolumeSnackbarRunnable implements Runnable {
        @Override
        public void run() {
            showAlarmVolumeMutedSnackbar();
        }
    }

    /**
     * Displays a snackbar that indicates the do-not-disturb setting is currently blocking alarms.
     */
    private final class ShowDNDBlockingSnackbarRunnable implements Runnable {
        @Override
        public void run() {
            showDoNotDisturbIsBlockingAlarmsSnackbar();
        }
    }

    /**
     * Observe changes to the system default alarm ringtone while the application is in the
     * foreground and show/hide the snackbar that warns when the ringtone is silent.
     */
    private final class AlarmRingtoneChangeObserver extends ContentObserver {
        private AlarmRingtoneChangeObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            if (isSystemAlarmRingtoneSilent()) {
                showSilentRingtoneSnackbar();
            } else {
                SnackbarManager.dismiss();
            }
        }
    }

    /**
     * Observe changes to the alarm stream volume while the application is in the foreground and
     * show/hide the snackbar that warns when the alarm volume is muted.
     */
    private final class AlarmVolumeChangeObserver extends ContentObserver {
        private AlarmVolumeChangeObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            if (isAlarmStreamMuted()) {
                showAlarmVolumeMutedSnackbar();
            } else {
                SnackbarManager.dismiss();
            }
        }
    }

    /**
     * Observe changes to the do-not-disturb setting while the application is in the foreground
     * and show/hide the snackbar that warns when the setting is blocking alarms.
     */
    private final class DoNotDisturbChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isDoNotDisturbBlockingAlarms()) {
                showDoNotDisturbIsBlockingAlarmsSnackbar();
            } else {
                SnackbarManager.dismiss();
            }
        }
    }


}