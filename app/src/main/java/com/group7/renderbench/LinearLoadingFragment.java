package com.group7.renderbench;

import android.os.Bundle;
import android.os.Trace;
import android.view.Choreographer;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
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
        LinearLayout container = view.findViewById(R.id.linear_items_container);

        // Generate the data before the build starts
        List<Item> items = new DataGenerator().generateDataset(datasetSize);

        ((TestActivity) requireActivity()).showLoading(datasetSize);

        // Wait one frame so the loading overlay is shown before the build blocks the UI thread
        runAfterNextFrame(() -> buildRows(container, items));
    }

    /** Inflates and adds a row for every item at once (no recycling). */
    private void buildRows(LinearLayout container, List<Item> items) {
        // Labels the build in the Android Studio CPU profiler
        Trace.beginSection("LinearLoading.buildRows");

        LayoutInflater inflater = getLayoutInflater();
        for (Item item : items) {
            View row = inflater.inflate(R.layout.item_row, container, false);
            TextView title = row.findViewById(R.id.item_row_title);
            TextView description = row.findViewById(R.id.item_row_description);
            title.setText(item.getTitle());
            description.setText(item.getDescription());
            container.addView(row);
        }

        Trace.endSection();

        ((TestActivity) requireActivity()).onRowsReady();
    }

    /** Runs the task after the next frame has been laid out and drawn. Skipped if the screen has closed. */
    private void runAfterNextFrame(Runnable task) {
        Choreographer.getInstance().postFrameCallback(frameTimeNanos -> {
            View view = getView();
            if (view != null) {
                view.post(task);
            }
        });
    }
}
