package com.group7.renderbench;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

/** Linear loading engine: ScrollView + LinearLayout, all rows created up front. */
public class LinearLoadingFragment extends Fragment {

    private static final String ARG_DATASET_SIZE = "dataset_size";

    public LinearLoadingFragment() {
        super(R.layout.fragment_linear_loading);
    }

    /** Creates the fragment for the given dataset size. */
    public static LinearLoadingFragment newInstance(int datasetSize) {
        LinearLoadingFragment fragment = new LinearLoadingFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_DATASET_SIZE, datasetSize);
        fragment.setArguments(args);
        return fragment;
    }
}
