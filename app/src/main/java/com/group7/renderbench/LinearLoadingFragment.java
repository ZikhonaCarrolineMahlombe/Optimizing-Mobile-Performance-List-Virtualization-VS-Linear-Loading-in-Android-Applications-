package com.group7.renderbench;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.group7.renderbench.entities.Item;
import com.group7.renderbench.generators.DataGenerator;

import java.util.List;

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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int datasetSize = requireArguments().getInt(ARG_DATASET_SIZE);
        ScrollView scrollView = view.findViewById(R.id.linear_scroll_view);
        LinearLayout container = view.findViewById(R.id.linear_items_container);

        // Generate the data pre-build
        List<Item> items = new DataGenerator().generateDataset(datasetSize);

        ((TestActivity) requireActivity()).showLoading(datasetSize);

        // post() runs after the screen is drawn, so the overlay shows before the build freezes it
        view.post(() -> buildRows(scrollView, container, items));
    }

    private void buildRows(ScrollView scrollView, LinearLayout container, List<Item> items) {
        if (!isAdded()) {
            return;
        }
        TestActivity activity = (TestActivity) requireActivity();

        try {
            LayoutInflater inflater = getLayoutInflater();

            for (Item item : items) {
                View row = inflater.inflate(R.layout.item_row, container, false);
                TextView title = row.findViewById(R.id.item_row_title);
                TextView description = row.findViewById(R.id.item_row_description);
                title.setText(item.getTitle());
                description.setText(item.getDescription());
                container.addView(row);
            }
        } catch (OutOfMemoryError e) {
            // Can occur at larger dataset sizes.
            int rowsBuilt = container.getChildCount();
            container.removeAllViews();
            activity.onBuildFailed(rowsBuilt);
            return;
        }

        // Start scrolling after the new rows have been laid out and drawn
        scrollView.post(() -> activity.onRowsReady(scrollView));
    }
}
