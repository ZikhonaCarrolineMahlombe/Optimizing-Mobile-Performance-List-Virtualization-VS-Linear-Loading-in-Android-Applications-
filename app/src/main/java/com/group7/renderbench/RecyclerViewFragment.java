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

public class RecyclerViewFragment extends Fragment {
    public RecyclerViewFragment() {
        super(R.layout.fragment_recycler_loading);
    }

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

    private void bindRows(RecyclerView recyclerView, List<Item> items) {
        if (!isAdded() || requireActivity().isFinishing()) {
            return;
        }
        TestActivity activity = (TestActivity) requireActivity();

        //Measurement starts here so the inflate or bind cost is included, like the ScrollView run
        activity.onBuildStarted();

        //Only the visible rows are inflated, the rest are created on demand while scrolling and recycled afterwareds, so the linear builds OutOfMemoryError path cannot occur here - that constant memory behaviour is what FR3 measures
        recyclerView.setAdapter(new ListAdapter(items));

        //Start scrolling after the new rows have been laid out and drawn
        recyclerView.post(() -> activity.onRowsReady(recyclerView));
    }
}
