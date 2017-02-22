package com.stasbar.knowyourself.data;

/**
 * Created by admin1 on 25.01.2017.
 */

import android.content.Context;
import android.content.SharedPreferences;

import com.stasbar.knowyourself.Utils;


/**
 * This class encapsulates the transfer of data between widget objects and their permanent storage
 * in {@link SharedPreferences}.
 */
public final class WidgetDAO {

    /** Suffix for a key to a preference that stores the instance count for a given widget type. */
    private static final String WIDGET_COUNT = "_widget_count";

    private WidgetDAO() {}

    /**
     * @param widgetProviderClass indicates the type of widget being counted
     * @param count the number of widgets of the given type
     * @return the delta between the new count and the old count
     */
    static int updateWidgetCount(Context context, Class widgetProviderClass, int count) {
        final SharedPreferences prefs = Utils.getDefaultSharedPreferences(context);
        final String key = widgetProviderClass.getSimpleName() + WIDGET_COUNT;
        final int oldCount = prefs.getInt(key, 0);
        if (count == 0) {
            prefs.edit().remove(key).apply();
        } else {
            prefs.edit().putInt(key, count).apply();
        }
        return count - oldCount;
    }
}
