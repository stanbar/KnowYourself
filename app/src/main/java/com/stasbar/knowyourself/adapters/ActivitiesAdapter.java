package com.stasbar.knowyourself.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.github.wrdlbrnft.sortedlistadapter.SortedListAdapter;

import com.stasbar.knowyourself.Utils;
import com.stasbar.knowyourself.data.ActivityItem;
import com.stasbar.knowyourself.databinding.ActivityItemBinding;

import java.util.Comparator;
import java.util.Objects;

public class ActivitiesAdapter extends SortedListAdapter<ActivityItem> {

    public interface OnActivityClickListener {
        void onActivityClick(ActivityItem activityItem);
    }

    private OnActivityClickListener listener;

    public ActivitiesAdapter(Context context, Comparator<ActivityItem> comparator, OnActivityClickListener listener) {
        super(context, ActivityItem.class, comparator);

        this.listener = listener;
    }

    @Override
    protected ActivityViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        final ActivityItemBinding binding = ActivityItemBinding.inflate(inflater, parent, false);
        return new ActivityViewHolder(binding);
    }

    @Override
    protected boolean areItemsTheSame(ActivityItem item1, ActivityItem item2) {
        return Objects.equals(item1.label, item2.label);
    }

    @Override
    protected boolean areItemContentsTheSame(ActivityItem oldItem, ActivityItem newItem) {

        return oldItem.equals(newItem);
    }


    private class ActivityViewHolder extends SortedListAdapter.ViewHolder<ActivityItem> {


        private ActivityItemBinding binding;

        ActivityViewHolder(ActivityItemBinding binding) {
            super(binding.getRoot());

            this.binding = binding;
        }

        @Override
        protected void performBind(ActivityItem activityItem) {
            binding.setActivityItem(activityItem);
            binding.setHandler(listener);
        }
    }
}
