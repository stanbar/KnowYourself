package com.stasbar.knowyourself.data;

import android.content.Context;
import android.net.Uri;

import java.util.TimeZone;

import android.provider.Settings;

import com.stasbar.knowyourself.R;
import com.stasbar.knowyourself.Utils;
import com.stasbar.knowyourself.data.DataModel.ClockStyle;
/**
 * Created by admin1 on 25.01.2017.
 */

public final class SettingsModel {

    private final Context mContext;

    /** The uri of the default ringtone to use for timers until the user explicitly chooses one. */
    private Uri mDefaultTimerRingtoneUri;

    SettingsModel(Context context) {
        mContext = context;

        // Set the user's default home timezone if one has not yet been chosen.
        SettingsDAO.setDefaultHomeTimeZone(mContext, TimeZone.getDefault());
    }


    TimeZone getHomeTimeZone() {
        return SettingsDAO.getHomeTimeZone(mContext);
    }

    ClockStyle getClockStyle() {
        return SettingsDAO.getClockStyle(mContext);
    }

    ClockStyle getScreensaverClockStyle() {
        return SettingsDAO.getScreensaverClockStyle(mContext);
    }

    boolean getScreensaverNightModeOn() {
        return SettingsDAO.getScreensaverNightModeOn(mContext);
    }

    boolean getShowHomeClock() {
        if (!SettingsDAO.getAutoShowHomeClock(mContext)) {
            return false;
        }

        // Show the home clock if the current time and home time differ.
        // (By using UTC offset for this comparison the various DST rules are considered)
        final TimeZone homeTimeZone = SettingsDAO.getHomeTimeZone(mContext);
        final long now = System.currentTimeMillis();
        return homeTimeZone.getOffset(now) != TimeZone.getDefault().getOffset(now);
    }

    Uri getDefaultTimerRingtoneUri() {
        if (mDefaultTimerRingtoneUri == null) {
            mDefaultTimerRingtoneUri = Utils.getResourceUri(mContext, R.raw.timer_expire);
        }

        return mDefaultTimerRingtoneUri;
    }

    void setTimerRingtoneUri(Uri uri) {
        SettingsDAO.setTimerRingtoneUri(mContext, uri);
    }

    Uri getTimerRingtoneUri() {
        return SettingsDAO.getTimerRingtoneUri(mContext, getDefaultTimerRingtoneUri());
    }

    Uri getDefaultAlarmRingtoneUri() {
        return SettingsDAO.getDefaultAlarmRingtoneUri(mContext,
                Settings.System.DEFAULT_ALARM_ALERT_URI);
    }

    void setDefaultAlarmRingtoneUri(Uri uri) {
        SettingsDAO.setDefaultAlarmRingtoneUri(mContext, uri);
    }

    boolean getTimerVibrate() {
        return SettingsDAO.getTimerVibrate(mContext);
    }

    void setTimerVibrate(boolean enabled) {
        SettingsDAO.setTimerVibrate(mContext, enabled);
    }
}
