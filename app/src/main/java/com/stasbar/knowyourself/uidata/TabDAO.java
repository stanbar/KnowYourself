package com.stasbar.knowyourself.uidata;

/**
 * Created by admin1 on 17.02.2017.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.stasbar.knowyourself.Utils;

import static com.stasbar.knowyourself.uidata.UiDataModel.Tab;

/**
 * This class encapsulates the storage of tab data in {@link SharedPreferences}.
 */
final class TabDAO {

    private static final String KEY_SELECTED_TAB = "selected_tab";

    // Lazily instantiated and cached for the life of the application.
    private static SharedPreferences sPrefs;

    private TabDAO() {}

    /**
     * @return an enumerated value indicating the currently selected primary tab
     */
    static Tab getSelectedTab(Context context) {
        final SharedPreferences prefs = getSharedPreferences(context);
        final int selectedTabOrdinal = prefs.getInt(KEY_SELECTED_TAB, Tab.ACTIVITIES.ordinal());
        return Tab.values()[selectedTabOrdinal];
    }

    /**
     * @param tab an enumerated value indicating the newly selected primary tab
     */
    static void setSelectedTab(Context context, Tab tab) {
        getSharedPreferences(context).edit().putInt(KEY_SELECTED_TAB, tab.ordinal()).apply();
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        if (sPrefs == null) {
            sPrefs = Utils.getDefaultSharedPreferences(context.getApplicationContext());
        }

        return sPrefs;
    }
}
