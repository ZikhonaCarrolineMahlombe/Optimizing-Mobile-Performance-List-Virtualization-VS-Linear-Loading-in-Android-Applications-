package com.group7.renderbench;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
 * Results screen: shows every  run saved in ResultsStore, compares the two
 * rendering engines on average, and opens the existing single-run detail screen
 * ResultActivity when a run is tapped
 */
public class ResultHistoryActivity extends AppCompatActivity {
    private final List<SavedRun> runs = new java.util.ArrayList<>();

    private LinearLayout historyList;
    private View emptyState;
    private View contentScroll;
    private TextView scrollViewSummary;
    private TextView recyclerViewSummary;
    private String activeFilter = "ALL"; // "ALL", "SCROLLVIEW", or "RECYCLERVIEW"
    private com.google.android.material.button.MaterialButton exportButton;
    private String exportFilterName = "all";

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

        historyList = findViewById(R.id.results_history_list); // Updated

        // ChipGroup filtering logic
        com.google.android.material.chip.ChipGroup filterGroup = findViewById(R.id.results_filter_group);
        filterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);

            if (id == R.id.filter_scrollview) {
                activeFilter = TestConfig.ENGINE_SCROLLVIEW;
                exportButton.setText(R.string.results_export_scrollview);
                exportFilterName = "scrollview";
            } else if (id == R.id.filter_recyclerview) {
                activeFilter = TestConfig.ENGINE_RECYCLERVIEW;
                exportButton.setText(R.string.results_export_recyclerview);
                exportFilterName = "recyclerview";
            } else {
                activeFilter = "ALL";
                exportButton.setText(R.string.results_export_all);
                exportFilterName = "all";
            }
            refresh();
        });
    }

    /** Attaches the CSV export, clear-all and empty-state shortcuts. */
    private void setUpButtons() {
        exportButton = findViewById(R.id.results_export_all_button);

        exportButton.setOnClickListener(v -> {
            if (!runs.isEmpty()) {
                // Generate the filename dynamically
                String filename = getString(R.string.results_export_filename, exportFilterName);
                createCsvLauncher.launch(filename);
            }
        });

        findViewById(R.id.results_empty_run_button).setOnClickListener(v -> {
            finish();
        });
    }

    /** Reloads the saved runs and redraws the list, summaries and empty state. */
    private void refresh() {
        runs.clear();
        List<SavedRun> allRuns = ResultsStore.load(this);

        for (SavedRun run : allRuns) {
            if (activeFilter.equals("ALL") || run.result.renderingEngine.equals(activeFilter)) {
                runs.add(run);
            }
        }

        // Build the UI manually
        historyList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (SavedRun run : runs) {
            View row = inflater.inflate(R.layout.item_result_history, historyList, false);

            TextView engine = row.findViewById(R.id.item_result_engine);
            TextView details = row.findViewById(R.id.item_result_details);
            TextView metrics = row.findViewById(R.id.item_result_metrics);
            TextView status = row.findViewById(R.id.item_result_status);
            TextView time = row.findViewById(R.id.item_result_time);

            TestResult result = run.result;

            engine.setText(engineLabel(result.renderingEngine));
            details.setText(getString(R.string.test_run_details, formatNumber(result.datasetSize), result.iteration, result.totalIterations));
            metrics.setText(String.format(Locale.getDefault(), "CPU %d ms • Memory %d MB • Frame %.2f ms", result.cpuTimeMs, result.memoryUsageMb, result.frameTimeMs));

            String statusText = result.status.toString().toLowerCase(Locale.getDefault());
            status.setText(statusText.substring(0, 1).toUpperCase(Locale.getDefault()) + statusText.substring(1));

            time.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new java.util.Date(run.timestampMs)));

            row.setOnClickListener(v -> startActivity(new Intent(ResultHistoryActivity.this, ResultsActivity.class).putExtra(ResultsActivity.EXTRA_RESULT, result)));

            historyList.addView(row);
        }

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

        csv.append("Timestamp,Rendering Engine,Dataset Size,Iteration," + "Total Iterations,Status,CPU Thread Time (ms)," + "Java Heap Memory (MB),Latest Frame Time (ms)," + "Starting Battery Current (uA),Ending Battery Current (uA)\n");

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
}
