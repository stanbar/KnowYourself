package com.stasbar.knowyourself.fragments;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
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
import android.widget.TextView;
import android.widget.Toast;

import com.stasbar.knowyourself.MainActivity;
import com.stasbar.knowyourself.OnSelectionChanged;
import com.stasbar.knowyourself.R;
import com.stasbar.knowyourself.adapters.FeelingAdapter;
import com.stasbar.knowyourself.data.ActivityItem;
import com.stasbar.knowyourself.data.Feeling;
import com.stasbar.knowyourself.data.Timer;
import com.stasbar.knowyourself.databinding.FragmentFeelingListBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class FeelingsFragment extends Fragment implements OnSelectionChanged, TextWatcher, TextView.OnEditorActionListener {

    private static final String ACTIVITI_ITEM = "activityItem";
    private static final String TIMER = "timer";
    private static final String STARTED_TIME = "startedTimeMillis";
    private static final String FINISHED_TIME = "finishedTimeMillis";

    RecyclerView recyclerView;
    EditText etHowYouFeel;
    Button btnDone;

    Timer timer;
    ActivityItem activityItem;
    private FeelingAdapter adapter;
    private MenuItem btnMenuDone;
    RecyclerView.LayoutManager linearLayout;
    private List<Feeling> feelingList;

    public FeelingsFragment() {
    }

    final Comparator<Feeling> alphabeticalComparator = new Comparator<Feeling>() {
        @Override
        public int compare(Feeling a, Feeling b) {
            if (!a.selected && b.selected) {
                return 1;
            } else if (a.selected && !b.selected) {
                return -1;
            } else
                return a.label.compareTo(b.label);
        }
    };

    public static FeelingsFragment newInstance(ActivityItem activityItem, long startedTimeMillis, long finishedTimeMillis) {
        FeelingsFragment fragment = new FeelingsFragment();

        Bundle bundle = new Bundle();
        bundle.putLong(STARTED_TIME, startedTimeMillis);
        bundle.putLong(FINISHED_TIME, finishedTimeMillis);
        bundle.putParcelable(ACTIVITI_ITEM, activityItem);

        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() == null)
            throw new IllegalStateException("Arguments can not be null");

        timer = getArguments().getParcelable(TIMER);
        activityItem = getArguments().getParcelable(ACTIVITI_ITEM);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FragmentFeelingListBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_feeling_list, container, false);
        setHasOptionsMenu(true);
        View view = binding.getRoot();
        btnDone = binding.buttonDone;
        etHowYouFeel = binding.editTextHowDoYouFeel;
        etHowYouFeel.setOnEditorActionListener(this);
        recyclerView = binding.recyclerViewFeelings;
        fillList();

        adapter = new FeelingAdapter(getActivity(), alphabeticalComparator, new FeelingAdapter.FeelingClickListener() {
            @Override
            public void onFeelingClick(Feeling feeling) {
                adapter.edit().remove(feeling).commit();
                feeling.selected = !feeling.selected;
                adapter.edit().add(feeling).commit();
                recyclerView.scrollToPosition(0);
                btnMenuDone.setVisible(!adapter.getSelectedItems().isEmpty());
                btnDone.setVisibility(!adapter.getSelectedItems().isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
        recyclerView.setAdapter(adapter);
        linearLayout = recyclerView.getLayoutManager();

        etHowYouFeel.addTextChangedListener(this);

        adapter.edit()
                .replaceAll(feelingList)
                .commit();
        return view;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_feeling_fragment, menu);
        btnMenuDone = menu.findItem(R.id.action_done);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_done:
                onDoneClick();
                return true;

        }

        return false;
    }

    public void onDoneClick() {
        List<Feeling> feelingList = adapter.getSelectedItems();
        Toast.makeText(getActivity(), "Done", Toast.LENGTH_SHORT).show();
        // TODO: 16.02.2017 add entity to database
        ((MainActivity) getActivity()).showActivities();
    }

    private void fillList() {
        List<String> feelingsPositive = Arrays.asList(getResources().getStringArray(R.array.feelings_positive));
        List<String> feelingsNegative = Arrays.asList(getResources().getStringArray(R.array.feelings_negative));
        feelingList = new ArrayList<>();
        addToList(feelingList, feelingsPositive);
        addToList(feelingList, feelingsNegative);

    }


    public void addToList(List<Feeling> feelings, List<String> feelingsString) {
        for (String string : feelingsString) {
            String[] s = string.split(";");
            String label = Character.toUpperCase(s[0].charAt(0)) + s[0].substring(1).toLowerCase();
            feelings.add(new Feeling(label));
        }
    }

    @Override
    public void onSelectionChanged() {
        for (Feeling feeling : feelingList)
            if (feeling.selected) {
                btnMenuDone.setVisible(true);
                return;
            }

        btnMenuDone.setVisible(false);
    }

    public void onAddFeelingClick() {
        if (etHowYouFeel.getText().toString().isEmpty())
            return;
        if (!selectSimillarOnList(etHowYouFeel.getText().toString())) {
            Feeling feeling = new Feeling(etHowYouFeel.getText().toString());
            feeling.selected = true;
            feelingList.add(feeling);
            // TODO: 10.02.2017 add to local database
            adapter.edit().add(feeling).commit();

        }
        etHowYouFeel.setText("");

    }

    private boolean selectSimillarOnList(String s) {
        final List<Feeling> duplicates = findDuplicates(feelingList, s);
        if (duplicates.size() == 1) {
            duplicates.get(0).selected = true;
            adapter.edit().replaceAll(duplicates).commit();
            return true;
        } else
            return false;
    }


    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE && v == etHowYouFeel) {
            onAddFeelingClick();
            return true;
        }
        return false;
    }


    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        final List<Feeling> filteredModelList = filter(feelingList, s.toString());
        adapter.edit()
                .replaceAll(filteredModelList)
                .commit();
        recyclerView.scrollToPosition(0);
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    private static List<Feeling> filter(List<Feeling> models, String query) {
        final String lowerCaseQuery = query.toLowerCase().replaceAll("[^a-zA-Z]", "");

        final List<Feeling> filteredModelList = new ArrayList<>();
        for (Feeling model : models) {
            final String text = model.label.toLowerCase().replaceAll("[^a-zA-Z]", "");
            if (text.contains(lowerCaseQuery)) {
                filteredModelList.add(model);
            }
        }
        return filteredModelList;
    }

    private List<Feeling> findDuplicates(List<Feeling> models, String feeling) {
        final String lowerCaseQuery = feeling.toLowerCase().replaceAll("[^a-zA-Z]", "");

        final List<Feeling> filteredModelList = new ArrayList<>();
        for (Feeling model : models) {
            final String text = model.label.toLowerCase().replaceAll("[^a-zA-Z]", "");
            if (text.equals(lowerCaseQuery)) {
                filteredModelList.add(model);
            }
        }
        return filteredModelList;

    }

}
