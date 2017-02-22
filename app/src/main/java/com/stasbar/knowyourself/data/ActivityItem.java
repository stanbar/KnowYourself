package com.stasbar.knowyourself.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.github.wrdlbrnft.sortedlistadapter.SortedListAdapter;

import java.util.ArrayList;
import java.util.List;

import io.realm.RealmObject;

/**
 * Created by admin1 on 26.11.2016.
 */

public class ActivityItem extends RealmObject implements Parcelable, SortedListAdapter.ViewModel{
    public String label;

    public ActivityItem() {
    }

    public ActivityItem(String label) {
        this.label = label;
    }


    public ActivityItem(Parcel parcel) {
        this.label = parcel.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(label);

    }

    public static final Creator<ActivityItem> CREATOR = new Creator<ActivityItem>() {
        @Override
        public ActivityItem createFromParcel(Parcel in) {
            return new ActivityItem(in);
        }

        @Override
        public ActivityItem[] newArray(int size) {
            return new ActivityItem[size];
        }
    };



}
