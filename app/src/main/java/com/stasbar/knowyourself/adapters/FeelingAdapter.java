package com.stasbar.knowyourself.adapters;

import android.content.Context;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.github.wrdlbrnft.sortedlistadapter.SortedListAdapter;
import com.stasbar.knowyourself.data.Feeling;
import com.stasbar.knowyourself.databinding.FeelingLayoutBinding;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class FeelingAdapter extends SortedListAdapter<Feeling> {



    public interface FeelingClickListener{
        void onFeelingClick(Feeling feeling);
    }
    private FeelingClickListener listener;
    public FeelingAdapter(Context context, Comparator<Feeling> comparator, FeelingClickListener listener) {
        super(context, Feeling.class, comparator);

        this.listener = listener;
    }

    @Override
    protected ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        final FeelingLayoutBinding binding = FeelingLayoutBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding);
    }


    @Override
    protected boolean areItemsTheSame(Feeling item1, Feeling item2) {
        return Objects.equals(item1.label, item2.label);
    }

    @Override
    protected boolean areItemContentsTheSame(Feeling oldItem, Feeling newItem) {
        return oldItem.equals(newItem);
    }

    public List<Feeling> getSelectedItems() {
        List<Feeling> list = new ArrayList<>();
        for (int i = 0; i < getItemCount(); i++) {
            Feeling currentFeeling = getItem(i);
            if (currentFeeling.selected)
                list.add(currentFeeling);

        }
        return list;
    }

    public List<Feeling> getAllItems() {
        List<Feeling> list = new ArrayList<>();
        for (int i = 0; i < getItemCount(); i++)
            list.add(getItem(i));
        return list;
    }

    public class ViewHolder extends SortedListAdapter.ViewHolder<Feeling>{
        private final FeelingLayoutBinding mBinding;


        public ViewHolder(FeelingLayoutBinding mBinding) {
            super(mBinding.getRoot());
            this.mBinding = mBinding;

        }

        @Override
        protected void performBind(Feeling feeling) {
            mBinding.setFeeling(feeling);
            mBinding.setHolder(this);
            mBinding.setListener(listener);
        }


    }
}
