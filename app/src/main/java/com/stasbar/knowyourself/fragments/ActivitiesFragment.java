package com.stasbar.knowyourself.fragments;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.stasbar.knowyourself.MainActivity;
import com.stasbar.knowyourself.R;
import com.stasbar.knowyourself.adapters.ActivitiesAdapter;
import com.stasbar.knowyourself.data.ActivityItem;
import com.stasbar.knowyourself.databinding.FragmentActivitiesListBinding;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;



public class ActivitiesFragment extends Fragment implements TextWatcher, TextView.OnEditorActionListener {


    RecyclerView recyclerView;

    ImageButton btnStart;

    EditText etActivity;

    private int mColumnCount = 1;

    ActivitiesAdapter adapter;
    List<ActivityItem> activityItemList;

    final Comparator<ActivityItem> alphabeticalComparator = new Comparator<ActivityItem>() {
        @Override
        public int compare(ActivityItem a, ActivityItem b) {
            return a.label.compareTo(b.label);
        }
    };

    public ActivitiesFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FragmentActivitiesListBinding binding = DataBindingUtil.inflate(inflater,R.layout.fragment_activities_list, container, false);
        binding.setFragment(this);
        setHasOptionsMenu(true);
        View view = binding.getRoot();

        etActivity = binding.editTextActivityName;
        btnStart = binding.buttonAddActivity;
        recyclerView = binding.recyclerViewActivities;
        etActivity.setOnEditorActionListener(this);
        activityItemList = new ArrayList<>();
        for (String name : getResources().getStringArray(R.array.activities)) {
            activityItemList.add(new ActivityItem(Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase()));
        }

        adapter = new ActivitiesAdapter(getActivity(), alphabeticalComparator, new ActivitiesAdapter.OnActivityClickListener() {
            @Override
            public void onActivityClick(ActivityItem activityItem) {
                startActivity(activityItem);
            }
        });

        etActivity.addTextChangedListener(this);
        recyclerView.setAdapter(adapter);

        adapter.edit()
                .replaceAll(activityItemList)
                .commit();


        return view;
    }



    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE && v == etActivity) {
            onStartClick();
            return true;
        }
        return false;
    }

    public void onStartClick() {
        startActivity(new ActivityItem(etActivity.getText().toString()));

    }

    public void startActivity(ActivityItem activityItem) {
        ((MainActivity) getActivity()).beginActivity(activityItem);
    }


    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        final List<ActivityItem> filteredModelList = filter(activityItemList, s.toString());
        adapter.edit()
                .replaceAll(filteredModelList)
                .commit();
        recyclerView.scrollToPosition(0);
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    private static List<ActivityItem> filter(List<ActivityItem> models, String query) {
        final String lowerCaseQuery = query.toLowerCase().replaceAll("[^a-zA-Z]","");

        final List<ActivityItem> filteredModelList = new ArrayList<>();
        for (ActivityItem model : models) {
            final String text = model.label.toLowerCase().replaceAll("[^a-zA-Z]","");

            if (text.contains(lowerCaseQuery)) {
                filteredModelList.add(model);
            }
        }
        return filteredModelList;
    }
}
