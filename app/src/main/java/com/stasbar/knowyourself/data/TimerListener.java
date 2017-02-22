package com.stasbar.knowyourself.data;

/**
 * Created by admin1 on 24.01.2017.
 */


/**
 * The interface through which interested parties are notified of changes to one of the timers.
 */
public interface TimerListener {

    /**
     * @param timer the timer that was added
     */
    void timerAdded(Timer timer);

    /**
     * @param before the timer state before the update
     * @param after the timer state after the update
     */
    void timerUpdated(Timer before, Timer after);

    /**
     * @param timer the timer that was removed
     */
    void timerRemoved(Timer timer);
}
