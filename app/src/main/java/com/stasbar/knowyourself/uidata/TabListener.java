package com.stasbar.knowyourself.uidata;

/**
 * Created by admin1 on 17.02.2017.
 */

import com.stasbar.knowyourself.uidata.UiDataModel.Tab;

/**
 * The interface through which interested parties are notified of changes to the selected tab.
 */
public interface TabListener {

    /**
     * @param oldSelectedTab an enumerated value indicating the prior selected tab
     * @param newSelectedTab an enumerated value indicating the newly selected tab
     */
    void selectedTabChanged(Tab oldSelectedTab, Tab newSelectedTab);
}