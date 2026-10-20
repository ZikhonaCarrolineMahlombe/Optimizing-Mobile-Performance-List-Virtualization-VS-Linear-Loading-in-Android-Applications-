package com.group7.renderbench;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.group7.renderbench.ResultsStore.SavedRun;
import com.group7.renderbench.controllers.TestController.TestResult;
import com.group7.renderbench.controllers.TestController.TestStatus;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Results screen: shows every  run saved in ResyltsStore, compares the two
 * rendering engines on average, and opens the existing single-run detail screen
 * ResultActivity when a run is tapped
 */
public class ResultHistoryActivity extends AppCompatActivity {
    private final List<SavedRun> runs = new java.util.ArrayList<>();

    private ResultsAdapter adapter;
    private View emptyState;
    private View contentScroll;
    private TextView scrollViewSummary;
    private TextView recyclerViewSummary;

    // Handles the system file picker flow for saving the combined CSV export.
    private final ActivityResultLauncher<String> createCsvLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/csv"),
            this::writeCsvFile
    );

    /**
     * Inflates the screen, applies the system bar insets and wires up the
     * toolbar, the run list and the export/clear buttons.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_results_history);
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.results_history_root), (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                });
        setUpToolbar();
        setUpList();
        setUpButtons();
    }

    /** Reloads the saved runs whenever the screen becomes visible again. */
    @Override
    protected void onResume() {
        super.onResume();
        //Refresh so runs finished since this screen was opened are included.
        refresh();
    }

    /** Makes the toolbars up arrow close the screen and return to the setup. */
    private void setUpToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.results_history_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    /** Looks up the summary and list views and attaches the run adapter. */
    private void setUpList() {
        emptyState = findViewById(R.id.results_history_empty);
        contentScroll = findViewById(R.id.results_history_scroll);
        scrollViewSummary = findViewById(R.id.results_scrollview_summary);
        recyclerViewSummary = findViewById(R.id.results_recyclerview_summary);

        RecyclerView list = findViewById(R.id.results_history_list);
        list.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ResultsAdapter();
        list.setAdapter(adapter);
    }

    /** Attaches the CSV export, clear-all and empty-state shortcuts. */
    private void setUpButtons() {
        // Ask the user where to save the combined CSV file
        findViewById(R.id.results_export_all_button).setOnClickListener(v -> {
            if (!runs.isEmpty()) {
                createCsvLauncher.launch(getString(R.string.results_export_all_name));
            }
        });

        findViewById(R.id.results_empty_run_button).setOnClickListener(v -> {
            finish();
        });
    }

    /** Reloads the saved runs and redraws the list, summaries and empty state. */
    private void refresh() {
        runs.clear();
        runs.addAll(ResultsStore.load(this));

        adapter.notifyDataSetChanged();
        showComparison();

        boolean isEmpty = runs.isEmpty();
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        contentScroll.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    /** Fills both engine cards with the average of that engines completed runs. */
    private void showComparison() {
        scrollViewSummary.setText(summarizeEngine(TestConfig.ENGINE_SCROLLVIEW));
        recyclerViewSummary.setText(summarizeEngine(TestConfig.ENGINE_RECYCLERVIEW));
    }

    /**
     * Builds the average summary line for one rendering engine.
     *
     * @param engine the engine constant to average over
     * @return the summary text, or a dash when the engine has no completed runs
     */
    private String summarizeEngine(String engine) {
        int completedRuns = 0;
        long totalCpuMs = 0;
        long totalMemoryMb = 0;
        double totalFrameMs = 0;

        for (SavedRun run : runs) {
            TestResult result = run.result;

            if (!engine.equals(result.renderingEngine) || result.status != TestStatus.COMPLETED) {
                continue;
            }

            completedRuns++;
            totalCpuMs += result.cpuTimeMs;
            totalMemoryMb += result.memoryUsageMb;
            totalFrameMs += result.frameTimeMs;
        }

        if (completedRuns == 0) {
            return getString(R.string.test_metric_empty);
        }

        return getString(
                R.string.results_run_summary,
                completedRuns,
                formatNumber(totalCpuMs / completedRuns),
                formatNumber(totalMemoryMb / completedRuns),
                totalFrameMs / completedRuns
        );
    }

    /** Asks for confirmation, then wipes the saved history. */
    private void confirmClear() {
        new AlertDialog.Builder(this).setTitle(R.string.results_clear_title).setMessage(R.string.results_clear_message).setPositiveButton(R.string.results_clear_confirm, (dialog, which) -> {
            ResultsStore.clear(this);
            refresh();
            Toast.makeText(this, R.string.results_cleared, Toast.LENGTH_SHORT).show();
        }).setNegativeButton(R.string.results_clear_cancel, null).show();
    }

    /** Writes every saved run as one CSV file, newest run first. */
    private void writeCsvFile(Uri uri) {
        // The user may cancel the file picker.
        if (uri == null) {
            return;
        }

        String csv = buildCsvContent();

        try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
            if (outputStream == null) {
                throw new IOException("Unable to open the selected file.");
            }

            outputStream.write(csv.getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, R.string.results_export_done, Toast.LENGTH_LONG).show();
        } catch (IOException exception) {
            Toast.makeText(this, getString(R.string.results_export_failed, exception.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    /** @return the CSV document holding every saved run, newest first. */
    private String buildCsvContent() {
        StringBuilder csv = new StringBuilder();

        csv.append("Timestamp,Renderung Engine,Dataset Size,Iteration," + "Total Iterations,Status,CPU Thread Time (ms)," + "Java Heap Memory (MB),Latest Frame Time (ms)," + "Starting Battery Current (uA),Ending Battery Current (uA)\n");

        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

        for (SavedRun run : runs) {
            TestResult result = run.result;

            csv.append(csvEscape(dateFormat.format(new java.util.Date(run.timestampMs))))
                    .append(",").append(result.datasetSize)
                    .append(",").append(result.iteration)
                    .append(",").append(result.totalIterations)
                    .append(",").append(csvEscape(result.status.toString()))
                    .append(",").append(result.cpuTimeMs)
                    .append(",").append(result.memoryUsageMb)
                    .append(",").append(result.frameTimeMs)
                    .append(",").append(result.startingBatteryCurrentUa)
                    .append(",").append(result.endingBatteryCurrentUa)
                    .append("\n");
        }

        return csv.toString();
    }

    /**
     * Sanitizes strings to prevent malformed CSV structure.
     *
     * @param value the raw cell text, may be null
     * @return the quoted, escaped cell text
     */
    private String csvEscape(String value) {
        if( value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    /**
     * Human-readable engine name, matching the labels used on the test screen.
     *
     * @param engine the engine constant stored on the result
     * @return the display name of the engine
     */
    private String engineLabel(String engine) {
        return TestConfig.ENGINE_RECYCLERVIEW.equals(engine) ? getString(R.string.test_engine_recyclerview) : getString(R.string.test_engine_scrollview);
    }

    /**
     * Groups a member with the devices locale, e.g. 10000 -> "10 000".
     *
     * @param value the number to format
     * @return the grouped number text
     */
    private static String formatNumber(long value) {
        return NumberFormat.getNumberInstance().format(value);
    }

    /** Adapter that renders one card per saved run, newest first. */
    private class ResultsAdapter extends RecyclerView.Adapter<ResultsAdapter.RunViewHolder> {
        /**
         * Inflates R.layout.item_result_history and wraps it in a new view holder.
         *
         * @param parent the list the new row will be attached to
         * @param viewType the view type, unused because all rows share one layout
         * @return a new holder for one saved run
         */
        @NonNull
        @Override
        public RunViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_result_history, parent, false);
            return new RunViewHolder(row);
        }

        /**
         * Binds the saved run at the given position to the holder's views and
         * attaches the tap that opens the detail screen.
         *
         * @param holder the holder to update, may be a recycled one
         * @param position the position of the run in the list
         * @return
         */
        @Override
        public void onBindViewHolder(@NonNull RunViewHolder holder, int position) {
            SavedRun run = runs.get(position);
            TestResult result = run.result;

            holder.engine.setText(engineLabel(result.renderingEngine));

            holder.details.setText(getString(R.string.test_run_details, formatNumber(result.datasetSize), result.iteration, result.totalIterations));

            holder.metrics.setText(String.format(Locale.getDefault(), "CPU %d ms • Memory %d MB • Frame %.2f ms", result.cpuTimeMs, result.memoryUsageMb, result.frameTimeMs));

            holder.status.setText(formatStatus(result.status));

            holder.time.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new java.util.Date(run.timestampMs)));

            // Open the existing single-run detail screen for this run.
            holder.itemView.setOnClickListener(v -> startActivity(new Intent(ResultHistoryActivity.this, ResultsActivity.class).putExtra(ResultsActivity.EXTRA_RESULT, result)));
        }

        /**
         * Returns the number of saved runs. Newest and oldest runs alike are
         * counted, as they all have a row.
         *
         * @return the total number of saved runs
         */
        @Override
        public int getItemCount() {
            return runs.size();
        }

        /**
         * "COMPLETED" -> "Completed"
         *
         * @param status the raw enum status
         * @return the status text shown on the row
         */
        private String formatStatus(TestStatus status) {
            String text = status.toString().toLowerCase(Locale.getDefault());
            return text.substring(0, 1).toUpperCase(Locale.getDefault()) + text.substring(1);
        }

        /** Caches one rows views, looked up once when the row is created. */
        class RunViewHolder extends RecyclerView.ViewHolder {
            final TextView engine;
            final TextView details;
            final TextView metrics;
            final TextView status;
            final TextView time;

            RunViewHolder(@NonNull View itemView) {
                super(itemView);
                engine = itemView.findViewById(R.id.item_result_engine);
                details = itemView.findViewById(R.id.item_result_details);
                metrics = itemView.findViewById(R.id.item_result_metrics);
                status = itemView.findViewById(R.id.item_result_status);
                time = itemView.findViewById(R.id.item_result_time);
            }
        }
    }
}
