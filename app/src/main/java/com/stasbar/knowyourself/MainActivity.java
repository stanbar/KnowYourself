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
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;

import com.stasbar.knowyourself.alarms.AlarmStateManager;
import com.stasbar.knowyourself.data.ActivityItem;
import com.stasbar.knowyourself.data.DataModel;
import com.stasbar.knowyourself.data.Timer;
import com.stasbar.knowyourself.databinding.ActivityMainBinding;
import com.stasbar.knowyourself.fragments.ActivitiesFragment;
import com.stasbar.knowyourself.fragments.FeelingsFragment;
import com.stasbar.knowyourself.fragments.PickTimeCounterFragment;
import com.stasbar.knowyourself.stopwatch.StopwatchFragment;
import com.stasbar.knowyourself.timer.TimerFragment;
import com.stasbar.knowyourself.widget.toast.SnackbarManager;


import static android.app.NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED;
import static android.app.NotificationManager.INTERRUPTION_FILTER_NONE;
import static android.media.AudioManager.FLAG_SHOW_UI;
import static android.media.AudioManager.STREAM_ALARM;
import static android.media.RingtoneManager.TYPE_ALARM;
import static android.provider.Settings.System.CONTENT_URI;
import static android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();
    public static final String ACTIVITIES_FRAGMENT = ActivitiesFragment.class.getName();
    public static final String FEELINGS_FRAGMENT = FeelingsFragment.class.getName();
    public static final String STOPWATCH_FRAGMENT = StopwatchFragment.class.getName();
    public static final String TIMER_FRAGMENT = TimerFragment.class.getName();
    private static final String PICK_TIME_COUNTER = PickTimeCounterFragment.class.getName();

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


    /**
     * Displays a snackbar explaining that the system default alarm ringtone is silent.
     */
    private final Runnable mShowSilentAlarmSnackbarRunnable = new ShowSilentAlarmSnackbarRunnable();


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
     * Observes default alarm ringtone changes while the app is in the foreground.
     */
    private final ContentObserver mAlarmRingtoneChangeObserver = new AlarmRingtoneChangeObserver();

    RelativeLayout fragmentPlaceholder;

    Toolbar toolbar;
    private FragmentManager supportFragmentManager;

    /**
     * {@code true} permits the muted alarm volume snackbar to show when starting this activity.
     */
    private boolean mShowSilencedAlarmsSnackbar;

    /**
     * The view to which snackbar items are anchored.
     */

    private View mSnackbarAnchor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding viewDataBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        mSnackbarAnchor = viewDataBinding.coordinatorMainActivity;
        toolbar = viewDataBinding.toolbar;
        fragmentPlaceholder = viewDataBinding.fragmentPlaceholder;
        supportFragmentManager = getSupportFragmentManager();
        setSupportActionBar(toolbar);

        // Don't show the volume muted snackbar on rotations.
        mShowSilencedAlarmsSnackbar = savedInstanceState == null;
        // Update the next alarm time on app startup because the user might have altered the data.
        //AlarmStateManager.updateNextAlarm(this);
        showActivities();
        //stopActivities(new ArrayList<ActivityItem>(),null);
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
    protected void onStop() {
        LogUtils.d(TAG, "onStop");
        if (!isChangingConfigurations()) {
            LogUtils.d(TAG, "onStop setApplicationInForeground -> FALSE");
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

    private void replaceFragment(Fragment fragment, String fragmentTag, boolean addToBackStack) {
        FragmentTransaction fragmentTransaction = supportFragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        fragmentTransaction.replace(R.id.fragment_placeholder, fragment, fragmentTag);
        if (addToBackStack) fragmentTransaction.addToBackStack(fragmentTag);
        fragmentTransaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    public void showActivities() {
        LogUtils.d(TAG, "showActivities");
        ActivitiesFragment fragment = new ActivitiesFragment();
        replaceFragment(fragment, ACTIVITIES_FRAGMENT, true);
    }

    public void beginActivity(ActivityItem activityItem) {
        LogUtils.d(TAG, "beginActivity");
        PickTimeCounterFragment pickTimeCounterFragment = PickTimeCounterFragment.newInstance(activityItem);

        replaceFragment(pickTimeCounterFragment, MainActivity.PICK_TIME_COUNTER, true);
    }

    public void startTimer(ActivityItem activityItem) {
        TimerFragment timerFragment = TimerFragment.newInstance(activityItem);
        replaceFragment(timerFragment, MainActivity.TIMER_FRAGMENT, true);
    }

    public void startStopwatch(ActivityItem activityItem) {
        StopwatchFragment stopwatchFragment = StopwatchFragment.newInstance(activityItem);
        replaceFragment(stopwatchFragment, MainActivity.STOPWATCH_FRAGMENT, true);
    }


    public void stopActivity(ActivityItem activityItem, Timer timer) {
        LogUtils.d(TAG, "stopActivities and show feeling fragment");
        supportFragmentManager.popBackStack(0, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        FeelingsFragment feelingsFragment = FeelingsFragment.newInstance(activityItem, timer.getTotalLength()
                //TODO: 21.02.2017 change it
                , timer.getElapsedTime());
        replaceFragment(feelingsFragment, MainActivity.FEELINGS_FRAGMENT, false);
    }


    private boolean isSystemAlarmRingtoneSilent() {
        try {
            return RingtoneManager.getActualDefaultRingtoneUri(this, TYPE_ALARM) == null;
        } catch (Exception e) {
            // Since this is purely informational, avoid crashing the app.
            return false;
        }
    }

    private void showSilentRingtoneSnackbar() {
        final View.OnClickListener changeClickListener = new View.OnClickListener() {
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
        try {
            return mAudioManager.getStreamVolume(STREAM_ALARM) <= 0;
        } catch (Exception e) {
            // Since this is purely informational, avoid crashing the app.
            return false;
        }
    }

    private void showAlarmVolumeMutedSnackbar() {
        final View.OnClickListener unmuteClickListener = new View.OnClickListener() {
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

        try {
            return mNotificationManager.getCurrentInterruptionFilter() == INTERRUPTION_FILTER_NONE;
        } catch (Exception e) {
            // Since this is purely informational, avoid crashing the app.
            return false;
        }
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
