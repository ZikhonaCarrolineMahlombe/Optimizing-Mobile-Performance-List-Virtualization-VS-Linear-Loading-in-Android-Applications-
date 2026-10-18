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
 * List virtualisation engine: RecyclerView + ListAdapter.
 * Displays the test dataset in RecyclerView where only the rows on
 * screen are inflated and, they are recycled while scrolling, so memory
 * use stays roughly constant at any dataset size.
 * The dataset comes from the host TestActivity, which is also told when
 * the build starts and when the rows are ready so it can measure
 * performance and start auto-scrolling.
 */
public class RecyclerViewFragment extends Fragment {
    /** Creates this fragment using the fragment_recycler_loading layout. */
    public RecyclerViewFragment() {
        super(R.layout.fragment_recycler_loading);
    }

    /**
     * Sets up the RecyclerView and schedules its rows to be bound once
     * the screen has been drawn.
     * Does nothing if the run was cancelled, meaning the host activity
     * has no dataset.
     *
     * @param view the root view of this fragment
     * @param savedInstanceState the previously saved state, or null if
     *                           there is none (unused)
     */

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TestActivity activity = (TestActivity) requireActivity();

        //Data comes from the TestController via the host screen; null means the run was cancelled
        List<Item> items = activity.getItems();

        if (items == null) {
            return;
        }

        RecyclerView recyclerView = view.findViewById(R.id.recycler_list_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));

        //Rows have a fixed height, so per-item measuring is skipped
        recyclerView.setHasFixedSize(true);

        activity.showLoading(items.size());

        //post() runs after the screen is drawn, so the overlay shows before the rows are bound
        view.post(() -> bindRows(recyclerView, items));
    }

    /**
     * Attaches a new ListAdapter to the list, then notifies the host
     * activity so it can start measuring and, once the rows are laid
     * out, start auto-scrolling.
     * Does nothing if this fragment is no longer attached or the host
     * activity is finishing.
     *
     * @param recyclerView the RecyclerView to attach the adapter to
     * @param items the list of items to display
     */
    private void bindRows(RecyclerView recyclerView, List<Item> items) {
        if (!isAdded() || requireActivity().isFinishing()) {
            return;
        }
        TestActivity activity = (TestActivity) requireActivity();

        // Measurement starts here so the inflate or bind cost is included, like the ScrollView run
        activity.onBuildStarted();

        // Only the visible rows are inflated; the rest are created on demand while scrolling and
        // recycled afterward, so the OutOfMemoryerror path of the linear build can not occur here.
        // That constant memory behaviour is what FR3 measures.
        recyclerView.setAdapter(new ListAdapter(items));

        // Start scrolling after the new rows have been laid out and drawn
        recyclerView.post(() -> activity.onRowsReady(recyclerView));
    }
}