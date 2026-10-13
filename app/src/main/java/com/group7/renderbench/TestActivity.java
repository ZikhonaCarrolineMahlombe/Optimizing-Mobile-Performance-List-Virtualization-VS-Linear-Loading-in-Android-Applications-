package com.group7.renderbench;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.group7.renderbench.TestController.TestResult;
import com.group7.renderbench.TestController.TestStatus;
import com.group7.renderbench.entities.Item;

import java.text.NumberFormat;
import java.util.List;

/** Test Run screen: shows the selected engine's list for one test run. TestController runs the test itself. */
public class TestActivity extends AppCompatActivity implements AutoScroller.Listener {

    // Intent extra keys
    public static final String EXTRA_ENGINE = "com.group7.renderbench.EXTRA_ENGINE";
    public static final String EXTRA_DATASET_SIZE = "com.group7.renderbench.EXTRA_DATASET_SIZE";
    public static final String EXTRA_ITERATION = "com.group7.renderbench.EXTRA_ITERATION";
    public static final String EXTRA_TOTAL_ITERATIONS = "com.group7.renderbench.EXTRA_TOTAL_ITERATIONS";

    private TestController controller;
    private List<Item> items;
    private AutoScroller autoScroller;
    private AlertDialog abortDialog;

    /** Builds the Intent that opens this screen with the given run settings. */
    public static Intent newIntent(Context context, String engine, int datasetSize,
                                   int iteration, int totalIterations) {
        Intent intent = runIntent(context, engine, datasetSize, iteration, totalIterations);
        // Prevent double tap runs
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    private static Intent runIntent(Context context, String engine, int datasetSize,
                                    int iteration, int totalIterations) {
        Intent intent = new Intent(context, TestActivity.class);
        intent.putExtra(EXTRA_ENGINE, engine);
        intent.putExtra(EXTRA_DATASET_SIZE, datasetSize);
        intent.putExtra(EXTRA_ITERATION, iteration);
        intent.putExtra(EXTRA_TOTAL_ITERATIONS, totalIterations);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_test);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.test_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (savedInstanceState != null) {
            Toast.makeText(this, R.string.test_run_interrupted, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!configureRun()) {
            Toast.makeText(this, R.string.test_invalid_settings, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setUpBackNavigation();
        showRunDetails();
        showEngine();
    }

    /** TestController validates run settings. */
    private boolean configureRun() {
        Intent intent = getIntent();
        controller = new TestController(this);

        try {
            controller.configureTest(
                    intent.getStringExtra(EXTRA_ENGINE),
                    intent.getIntExtra(EXTRA_DATASET_SIZE, -1),
                    intent.getIntExtra(EXTRA_ITERATION, 1),
                    intent.getIntExtra(EXTRA_TOTAL_ITERATIONS, 1));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Leaving the app mid-run makes the run invalid.
        if (controller != null && !isRunOver()) {
            stopScroller();
            controller.markIncomplete();
            Toast.makeText(this, R.string.test_run_left, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /** Back Navigation in case of leaving the test */
    private void setUpBackNavigation() {
        MaterialToolbar toolbar = findViewById(R.id.test_toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackRequested());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onBackRequested();
            }
        });
    }

    private void onBackRequested() {

        if (isRunOver()) {
            finish();
        } else {
            confirmAbort();
        }
    }

    private void confirmAbort() {
        abortDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.test_abort_title)
                .setMessage(R.string.test_abort_message)
                .setPositiveButton(R.string.test_abort_confirm, (dialog, which) -> {
                    stopScroller();
                    controller.markIncomplete();
                    finish();
                })
                .setNegativeButton(R.string.test_abort_cancel, null)
                .show();
    }

    /** True once TestController has given this run its final status. */
    private boolean isRunOver() {
        TestStatus status = controller.getTestStatus();
        return status == TestStatus.COMPLETED
                || status == TestStatus.FAILED
                || status == TestStatus.INCOMPLETE;
    }

    private void stopScroller() {

        if (autoScroller != null) {
            autoScroller.stop();
        }
    }

    /** Fills in the header and the Rows cell. */
    private void showRunDetails() {
        TextView engineLabel = findViewById(R.id.test_engine_label);
        TextView runDetails = findViewById(R.id.test_run_details);
        TextView rowsValue = findViewById(R.id.test_metric_rows_value);

        String sizeText = formatNumber(controller.getDatasetSize());

        if (TestConfig.ENGINE_SCROLLVIEW.equals(controller.getRenderingEngine())) {
            engineLabel.setText(R.string.test_engine_scrollview);
        } else {
            engineLabel.setText(R.string.test_engine_recyclerview);
        }
        runDetails.setText(getString(R.string.test_run_details, sizeText,
                controller.getIteration(), controller.getTotalIterations()));
        rowsValue.setText(sizeText);
    }

    /** Gets the dataset from TestController and adds the selected engine's fragment to the list area. */
    private void showEngine() {
        items = controller.prepareDataset();

        // RecyclerView fetching here
        if (TestConfig.ENGINE_RECYCLERVIEW.equals(controller.getRenderingEngine())) {
            //TO-DO: replace with the RecyclerView fragment
            Toast.makeText(this, R.string.test_engine_not_ready, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // ScrollView fetching here
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.test_list_container, new LinearLoadingFragment())
                .commit();
    }

    /** This run's dataset, generated by TestController. */
    public List<Item> getItems() {
        return items;
    }

    /** Shows the loading overlay while the engine builds its rows. Called by the engine fragment. */
    public void showLoading(int rowCount) {
        TextView loadingText = findViewById(R.id.test_loading_text);
        loadingText.setText(getString(R.string.test_loading_rows, formatNumber(rowCount)));
        findViewById(R.id.test_loading_overlay).setVisibility(View.VISIBLE);
        setStatus(R.string.test_status_building);
    }

    /** Starts measuring right before the rows are built, so the build cost is included. Called by the engine fragment. */
    public void onBuildStarted() {
        controller.startMonitoring();
    }

    /** Hides the loading overlay and starts auto-scrolling. Called by the engine fragment once its rows are on screen. */
    public void onRowsReady(View list) {
        if (isFinishing()) {
            return; // Run was aborted before scrolling started
        }
        findViewById(R.id.test_loading_overlay).setVisibility(View.GONE);
        setStatus(R.string.test_status_scrolling);
        autoScroller = new AutoScroller(list, controller.getDatasetSize(), this);
        autoScroller.start();
    }

    @Override
    public void onScrollProgress(long elapsedMs, int percent) {
        showScrollProgress(elapsedMs, percent);
    }

    @Override
    public void onScrollFinished(long elapsedMs, int percent) {
        showScrollProgress(elapsedMs, percent);
        setStatus(R.string.test_status_done);

        // Reaching the end of the list or the 30 seconds limit both complete the run
        TestResult result = controller.stopMonitoring(true);

        if (abortDialog != null && abortDialog.isShowing()) {
            abortDialog.dismiss(); // Run finished while the abort dialog was open
        }
        openResults(result);
    }

    /**
     * Shows the current iteration's results. If iterations remain, the next one is placed
     * under the Results screen and starts when Results is closed.
     */
    private void openResults(TestResult result) {
        Intent results = new Intent(this, ResultsActivity.class)
                .putExtra(ResultsActivity.EXTRA_RESULT, result);

        if (controller.getIteration() < controller.getTotalIterations()) {
            // runIntent has no single-top flag, so a new screen is created instead of reusing this one
            Intent next = runIntent(this, controller.getRenderingEngine(), controller.getDatasetSize(),
                    controller.getIteration() + 1, controller.getTotalIterations());
            startActivities(new Intent[]{next, results});
        } else {
            startActivity(results);
        }
        finish();
    }

    private void showScrollProgress(long elapsedMs, int percent) {
        TextView elapsedValue = findViewById(R.id.test_metric_elapsed_value);
        ProgressBar progress = findViewById(R.id.test_progress);
        elapsedValue.setText(getString(R.string.test_elapsed_seconds, elapsedMs / 1000f));
        progress.setProgress(percent);
    }

    /** Shows that the build ran out of memory, then closes the run. Called by the engine fragment. */
    public void onBuildFailed(int rowsBuilt) {
        controller.markFailed();
        findViewById(R.id.test_loading_overlay).setVisibility(View.GONE);
        setStatus(R.string.test_status_failed);
        new AlertDialog.Builder(this)
                .setTitle(R.string.test_oom_title)
                .setMessage(getString(R.string.test_oom_message,
                        formatNumber(rowsBuilt), formatNumber(controller.getDatasetSize())))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                .show();
    }

    private void setStatus(int statusResId) {
        TextView statusValue = findViewById(R.id.test_metric_status_value);
        statusValue.setText(statusResId);
    }

    // e.g. 10000 -> "10 000"
    private static String formatNumber(long value) {
        return NumberFormat.getIntegerInstance().format(value);
    }
}
