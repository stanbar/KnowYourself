package com.stasbar.knowyourself.data;

/**
 * Created by admin1 on 25.01.2017.
 */


import android.content.Context;

/**
 * All widget data is accessed via this model.
 */
public final class WidgetModel {

    private final Context mContext;

    WidgetModel(Context context) {
        mContext = context;
    }

    /**
     * @param widgetClass indicates the type of widget being counted
     * @param count the number of widgets of the given type
     */
    void updateWidgetCount(Class widgetClass, int count) {
        WidgetDAO.updateWidgetCount(mContext, widgetClass, count);
    }
}
