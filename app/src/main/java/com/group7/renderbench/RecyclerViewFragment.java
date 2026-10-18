package com.group7.renderbench;


import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.group7.renderbench.entities.Item;

import java.util.List;

/**
 * List virtualization engine (FR3): displays the test dataset in a {@link RecyclerView}.
 * <p>
 *     Only the rows on screen are inflated, and they are recycled while scrolling, so memory use
 *     stays roughly constant regardless of dataset size. This is the counterpart to
 *     {@link LinearLoadingFragment}, which builds every row up front.
 * <p>
 * The dataset comes from the host {@link TestActivity}, which is also told when the build
 * starts and when the rows are ready so it can measure performance and start auto-scrolling.
 *
 * @see ListAdapter
 * @see LinearLoadingFragment
 */

public class RecyclerViewFragment extends Fragment {
    public RecyclerViewFragment() {
        super(R.layout.fragment_recycler_loading);
    }

    /**
     * Sets up the RecyclerView and schedules the rows to be bound once the screen is drawn.
     * Does nothing if the run was cancelled (the host has no dataset).
     *
     * @param view the fragment's root view
     * @param savedInstanceState previously saved, or {@code null} (unused)
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TestActivity activity = (TestActivity) requireActivity();

        // Data comes from the TestController via the host screen; null means the run was cancelled
        List<Item> items = activity.getItems();

        if (items == null) {
            return;
        }

        RecyclerView recyclerView = view.findViewById(R.id.recycler_list_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));

        // Rows have a fixed height, so per-item measuring is skipped
        recyclerView.setHasFixedSize(true);

        activity.showLoading(items.size());

        // post() runs after the screen is drawn, so the overlay shows before the rows are bound
        view.post(() -> bindRows(recyclerView, items));
    }

    /**
     * Attaches the adapter to the RecyclerView and notifies the host activity so it can
     * start measuring and then auto-scrolling. Skipped if the fragment is no longer attached
     * or the activity is finishing.
     *
     * @param recyclerView the list to attach the adapter to
     * @param items the dataset to display
     */
    private void bindRows(RecyclerView recyclerView, List<Item> items) {
        if (!isAdded() || requireActivity().isFinishing()) {
            return;
        }
        TestActivity activity = (TestActivity) requireActivity();

        // Measurement starts here so the inflate or bind cost is included, like the ScrollView run
        activity.onBuildStarted();

        // Only the visible rows are inflated, the rest are created on demand while scrolling and
        // recycled afterward, so the linear builds OutOfMemoryError path cannot occur here.
        // That constant memory behaviour is what FR3 measures
        recyclerView.setAdapter(new ListAdapter(items));

        // Start scrolling after the new rows have been laid out and drawn
        recyclerView.post(() -> activity.onRowsReady(recyclerView));
    }
}