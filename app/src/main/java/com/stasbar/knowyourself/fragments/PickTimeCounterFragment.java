package com.stasbar.knowyourself.fragments;


import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.stasbar.knowyourself.MainActivity;
import com.stasbar.knowyourself.R;
import com.stasbar.knowyourself.data.ActivityItem;
import com.stasbar.knowyourself.databinding.FragmentPickTimerBinding;

public class PickTimeCounterFragment extends Fragment {
    private static final String ACTIVITY_ITEM = "activity_item";
    ProgressBar btnTimer;
    ProgressBar btnStopwatch;


    private ActivityItem activityItem;
    private Animatable2.AnimationCallback animationCallback;

    public PickTimeCounterFragment() {
        // Required empty public constructor
    }

    public static PickTimeCounterFragment newInstance(ActivityItem activityItem) {
        PickTimeCounterFragment fragment = new PickTimeCounterFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ACTIVITY_ITEM, activityItem);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            activityItem = getArguments().getParcelable(ACTIVITY_ITEM);
        } else {
            throw new IllegalArgumentException("Arguments can not be null");
        }
    }
    AnimatedVectorDrawable animatedVectorDrawable;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        FragmentPickTimerBinding binding = FragmentPickTimerBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        btnStopwatch = binding.progressBarStopwatch;
        btnTimer = binding.progressBarTimer;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            animatedVectorDrawable = ((AnimatedVectorDrawable)btnTimer.getIndeterminateDrawable());
            animationCallback = new Animatable2.AnimationCallback() {
                @Override
                public void onAnimationEnd(Drawable drawable) {
                    super.onAnimationEnd(drawable);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        animatedVectorDrawable.start();
                    }
                }

                @Override
                public void onAnimationStart(Drawable drawable) {
                    super.onAnimationStart(drawable);
                }
            };
            animatedVectorDrawable.registerAnimationCallback(animationCallback);
            animatedVectorDrawable.start();
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            animatedVectorDrawable.unregisterAnimationCallback(animationCallback);
        }
    }



    public void startTimeCounter(View view) {
        if (getActivity() instanceof MainActivity) {
            if (view.getId() == R.id.progress_bar_timer)
                ((MainActivity) getActivity()).startTimer(activityItem);
            else if (view.getId() == R.id.progress_bar_stopwatch)
                ((MainActivity) getActivity()).startStopwatch(activityItem);
        } else
            throw new IllegalStateException("Fragment attached to wrong activity");
    }

}
