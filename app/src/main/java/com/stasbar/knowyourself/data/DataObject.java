package com.stasbar.knowyourself.data;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by admin1 on 08.02.2017.
 */

public class DataObject extends RealmObject {

    @PrimaryKey
    String id;

    private ActivityItem activity;
    private RealmList<Feeling> feelingsBefore;
    private RealmList<Feeling> feelingsInProgress;
    private RealmList<Feeling> feelingsAfter;
    private long startedTimeMillis;
    private long finishedTimeMillis;
    //private Timer timer;

    public DataObject() {
    }

    public DataObject(String id, ActivityItem activity, RealmList<Feeling> feelingsBefore, RealmList<Feeling> feelingsInProgress, RealmList<Feeling> feelingsAfter, long startedTimeMillis, long finishedTimeMillis) {
        this.id = id;
        this.activity = activity;
        this.feelingsBefore = feelingsBefore;
        this.feelingsInProgress = feelingsInProgress;
        this.feelingsAfter = feelingsAfter;
        this.startedTimeMillis = startedTimeMillis;
        this.finishedTimeMillis = finishedTimeMillis;
    }
}
