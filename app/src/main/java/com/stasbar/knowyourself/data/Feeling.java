package com.stasbar.knowyourself.data;

import com.github.wrdlbrnft.sortedlistadapter.SortedListAdapter;


import io.realm.RealmObject;
import io.realm.annotations.Ignore;

/**
 * Created by admin1 on 26.11.2016.
 */
public class Feeling extends RealmObject implements SortedListAdapter.ViewModel {
    public String label;

    @Ignore
    public boolean selected = false;

    public Feeling() {

    }

    public Feeling(String label) {
        this.label = label;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Feeling)) return false;

        Feeling feeling = (Feeling) o;

        return label.equals(feeling.label);

    }

    @Override
    public int hashCode() {
        return label.hashCode();
    }
}
