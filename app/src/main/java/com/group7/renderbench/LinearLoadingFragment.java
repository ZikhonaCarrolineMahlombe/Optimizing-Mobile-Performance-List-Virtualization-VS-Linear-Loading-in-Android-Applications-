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

import java.util.List;

/**
 * Linear loading engine that builds every row into a <code>LinearLayout</code>
 * inside a <code>ScrollView</code> up front, with no view recycling.
 */
public class LinearLoadingFragment extends Fragment {

    /**
     * Creates this fragment with <code>fragment_linear_loading</code> as its
     * layout.
     */
    public LinearLoadingFragment() {
        super(R.layout.fragment_linear_loading);
    }

    /**
     * Gets the dataset from <code>TestActivity</code>, shows the loading
     * overlay and schedules the row build.
     *
     * @param view this fragment's root view
     * @param savedInstanceState the saved state, or <code>null</code> if this
     *        fragment was not recreated
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TestActivity activity = (TestActivity) requireActivity();

        // Data comes from TestController via the host screen; null means the run was cancelled
        List<Item> items = activity.getItems();

        if (items == null) {
            return;
        }

        ScrollView scrollView = view.findViewById(R.id.linear_scroll_view);
        LinearLayout container = view.findViewById(R.id.linear_items_container);

        activity.showLoading(items.size());

        // post() runs after the screen is drawn, so the overlay shows before the build freezes it
        view.post(() -> buildRows(scrollView, container, items));
    }

    /**
     * Inflates one row per item, then reports either an out-of-memory failure
     * or that the rows are ready to scroll.
     *
     * @param scrollView the scrolling container shown on screen
     * @param container the layout that holds the rows
     * @param items the dataset, built as one row per item
     */
    private void buildRows(ScrollView scrollView, LinearLayout container, List<Item> items) {

        if (!isAdded() || requireActivity().isFinishing()) {
            return;
        }
        TestActivity activity = (TestActivity) requireActivity();
        activity.onBuildStarted();

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
