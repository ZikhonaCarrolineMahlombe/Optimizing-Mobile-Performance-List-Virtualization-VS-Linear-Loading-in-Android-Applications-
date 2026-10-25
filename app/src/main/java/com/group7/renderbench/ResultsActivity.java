package com.group7.renderbench;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.group7.renderbench.controllers.TestController;
import com.group7.renderbench.monitoring.PerformanceMonitor;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Displays the results of a benchmark test.
 * Shows the collected performance metrics and
 * allows the results to be exported as a CSV file.
 */

public class ResultsActivity extends AppCompatActivity {

    public static final String EXTRA_RESULT =
            "com.group7.renderbench.EXTRA_RESULT";

    private TestController.TestResult result;

    // Handles the system file picker flow for saving the CSV export.
    private final ActivityResultLauncher<String> createCsvLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.CreateDocument("text/csv"),
                    this::writeCsvFile
            );

    /**
     * Initializes the activity and loads the test results.
     *
     * @param savedInstanceState the saved activity state
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        result = (TestController.TestResult)
                getIntent().getSerializableExtra(EXTRA_RESULT);

        // Terminate early if the benchmark payload is missing or invalid.
        if (result == null) {
            showMissingResult();
            return;
        }

        displayResults();
        setUpButtons();
    }
    private void displayResults() {
        TextView status = findViewById(R.id.results_status);
        TextView engine = findViewById(R.id.results_engine);
        TextView datasetSize = findViewById(R.id.results_dataset_size);
        TextView iteration = findViewById(R.id.results_iteration);
        TextView cpu = findViewById(R.id.results_cpu);
        TextView memory = findViewById(R.id.results_memory);
        TextView frameTime = findViewById(R.id.results_frame_time);
        TextView battery = findViewById(R.id.results_battery);

        status.setText("Status: " + result.status);
        engine.setText("Rendering engine: " + result.renderingEngine);

        datasetSize.setText(
                "Dataset size: " + formatNumber(result.datasetSize));

        iteration.setText(
                "Iteration: " + result.iteration
                        + " of " + result.totalIterations);

        cpu.setText("CPU thread time: "
                + result.cpuTimeMs + " ms");

        memory.setText("Java heap memory: "
                + result.memoryUsageMb + " MB");

        frameTime.setText(String.format(
                Locale.getDefault(),
                "Latest frame time: %.2f ms",
                result.frameTimeMs));

        battery.setText(String.format(
                Locale.getDefault(),
                "Battery current: start %s, end %s",
                formatBatteryCurrent(result.startingBatteryCurrentUa),
                formatBatteryCurrent(result.endingBatteryCurrentUa)
        ));
    }

    /**
     * Sets up the button actions for exporting results
     * and closing the screen.
     */
    private void setUpButtons() {
        Button exportButton =
                findViewById(R.id.results_export_button);

        Button doneButton =
                findViewById(R.id.results_done_button);

        // Ask the user where to save the CSV file.
        exportButton.setOnClickListener(v -> {
            if (result != null) {
                createCsvLauncher.launch("renderbench_results.csv");
            }
        });

        doneButton.setOnClickListener(v -> finish());
    }

    /**
     * Writes the benchmark results to the selected CSV file.
     *
     * @param uri the destination URI, or null if the user cancels the file picker
     */
    private void writeCsvFile(Uri uri) {
        // The user may cancel the file picker.
        if (uri == null || result == null) {
            return;
        }

        String csv = buildCsvContent();

        try (OutputStream outputStream =
                     getContentResolver().openOutputStream(uri)) {

            if (outputStream == null) {
                throw new IOException(
                        "Unable to open the selected file.");
            }

            outputStream.write(
                    csv.getBytes(StandardCharsets.UTF_8));

            Toast.makeText(
                    this,
                    "Results exported successfully.",
                    Toast.LENGTH_LONG
            ).show();

        } catch (IOException exception) {
            Toast.makeText(
                    this,
                    "Could not export results: "
                            + exception.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }
    private String buildCsvContent() {
        StringBuilder csv = new StringBuilder();

        csv.append("Metric,Value\n");

        csv.append("Rendering Engine,")
                .append(csvEscape(result.renderingEngine))
                .append("\n");

        csv.append("Dataset Size,")
                .append(result.datasetSize)
                .append("\n");

        csv.append("Iteration,")
                .append(result.iteration)
                .append("\n");

        csv.append("Total Iterations,")
                .append(result.totalIterations)
                .append("\n");

        csv.append("Status,")
                .append(csvEscape(result.status.toString()))
                .append("\n");

        csv.append("CPU Thread Time (ms),")
                .append(result.cpuTimeMs)
                .append("\n");

        csv.append("Java Heap Memory (MB),")
                .append(result.memoryUsageMb)
                .append("\n");

        csv.append("Latest Frame Time (ms),")
                .append(result.frameTimeMs)
                .append("\n");

        csv.append("Starting Battery Current (uA),")
                .append(batteryCsvValue(result.startingBatteryCurrentUa))
                .append("\n");

        csv.append("Ending Battery Current (uA),")
                .append(batteryCsvValue(result.endingBatteryCurrentUa))
                .append("\n");

        csv.append("Device,")
                .append(csvEscape(Build.MANUFACTURER + " " + Build.MODEL))
                .append("\n");

        return csv.toString();
    }

    /**
     * Escapes a value before it is written to the CSV file.
     *
     * @param value the value to escape
     * @return the escaped value
     */
    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    /**
     * Shows a message when no test results are available
     * and closes the activity.
     */
    private void showMissingResult() {
        Toast.makeText(
                this,
                "No test results were supplied.",
                Toast.LENGTH_LONG
        ).show();

        finish();
    }
    private String formatNumber(int value) {
        return NumberFormat.getIntegerInstance().format(value);
    }

    private String formatBatteryCurrent(long currentUa) {
        if (!PerformanceMonitor.isBatteryCurrentAvailable(currentUa)) {
            return "N/A";
        }

        return String.format(Locale.getDefault(), "%d µA", currentUa);
    }

    private String batteryCsvValue(long currentUa) {
        return PerformanceMonitor.isBatteryCurrentAvailable(currentUa) ? String.valueOf(currentUa) : "";
    }
}
