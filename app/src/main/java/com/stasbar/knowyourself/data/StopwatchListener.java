package com.stasbar.knowyourself.data;

/**
 * Created by admin1 on 24.01.2017.
 */

public interface StopwatchListener {

    /**
     * @param before the stopwatch state before the update
     * @param after the stopwatch state after the update
     */
    void stopwatchUpdated(Stopwatch before, Stopwatch after);

    /**
     * @param lap the lap that was added
     */
    void lapAdded(Lap lap);
}