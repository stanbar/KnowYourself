package com.stasbar.knowyourself.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.stasbar.knowyourself.R;
import com.stasbar.knowyourself.data.ActivityItem;
import com.stasbar.knowyourself.databinding.FragmentDuringActivityBinding;

import java.util.ArrayList;
import java.util.List;

public class DuringActivityFragment extends Fragment {
    private static final String ACTIVITIES = "activities";

    long startTime = 0;

    private List<ActivityItem> activities;

    TextView tvActivities;

    TextView tvTimer;

    Button btnStartStop;

    public DuringActivityFragment() {
    }

    public static DuringActivityFragment newInstance(ArrayList<ActivityItem> activities) {
        DuringActivityFragment fragment = new DuringActivityFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ACTIVITIES, activities);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            activities = getArguments().getParcelableArrayList(ACTIVITIES);
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FragmentDuringActivityBinding binder = FragmentDuringActivityBinding.inflate(inflater, container, false);
        View view = binder.getRoot();
        btnStartStop = binder.buttonStartStop;
        tvTimer = binder.textViewTimer;
        tvActivities = binder.textViewActivityLabel;

        StringBuilder stringBuilder = new StringBuilder();
        for (ActivityItem activity : activities) {
            stringBuilder.append(activity.label).append("\n");
        }
        tvActivities.setText(stringBuilder.toString());

        return view;
    }

    public void onStartStop(View view) {

    }

}
