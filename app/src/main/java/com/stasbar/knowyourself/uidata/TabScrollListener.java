package com.stasbar.knowyourself.uidata;

/**
 * Created by admin1 on 17.02.2017.
 */
import com.stasbar.knowyourself.uidata.UiDataModel.Tab;

/**
 * The interface through which interested parties are notified of changes to the vertical scroll
 * position of the selected tab. Callbacks to listener occur when any of these events occur:
 *
 * <ul>
 *     <li>the vertical scroll position of the selected tab is now scrolled to the top</li>
 *     <li>the vertical scroll position of the selected tab is no longer scrolled to the top</li>
 *     <li>the selected tab changed and the new tab scroll state does not match the prior tab</li>
 * </ul>
 */
public interface TabScrollListener {

    /**
     * @param selectedTab an enumerated value indicating the current selected tab
     * @param scrolledToTop indicates whether the current selected tab is scrolled to its top
     */
    void selectedTabScrollToTopChanged(Tab selectedTab, boolean scrolledToTop);
}