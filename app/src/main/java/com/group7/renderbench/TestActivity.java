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
import com.group7.renderbench.controllers.TestController;
import com.group7.renderbench.controllers.TestController.TestResult;
import com.group7.renderbench.controllers.TestController.TestStatus;
import com.group7.renderbench.entities.Item;

import java.text.NumberFormat;
import java.util.List;

/**
 * Test Run screen that shows the selected engine's list for one test run.
 * <code>TestController</code> runs the test itself.
 */
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

    /**
     * Builds the <code>Intent</code> that opens this screen with the given run
     * settings.
     *
     * @param context the context to create the <code>Intent</code> from
     * @param engine the rendering engine to test
     * @param datasetSize the number of rows to build
     * @param iteration the number of this run, starting at 1
     * @param totalIterations the number of runs in this test
     * @return the <code>Intent</code> that opens this screen
     */
    public static Intent newIntent(Context context, String engine, int datasetSize,
                                   int iteration, int totalIterations) {
        Intent intent = runIntent(context, engine, datasetSize, iteration, totalIterations);
        // Prevent double tap runs
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    /**
     * Builds the run <code>Intent</code> without the single-top flag, so it can
     * queue the next iteration.
     *
     * @param context the context to create the <code>Intent</code> from
     * @param engine the rendering engine to test
     * @param datasetSize the number of rows to build
     * @param iteration the number of this run, starting at 1
     * @param totalIterations the number of runs in this test
     * @return the <code>Intent</code> for the run
     */
    private static Intent runIntent(Context context, String engine, int datasetSize,
                                    int iteration, int totalIterations) {
        Intent intent = new Intent(context, TestActivity.class);
        intent.putExtra(EXTRA_ENGINE, engine);
        intent.putExtra(EXTRA_DATASET_SIZE, datasetSize);
        intent.putExtra(EXTRA_ITERATION, iteration);
        intent.putExtra(EXTRA_TOTAL_ITERATIONS, totalIterations);
        return intent;
    }

    /**
     * Sets up this screen and starts the run. Closes this screen instead if the
     * settings are invalid or the screen was recreated mid-run.
     *
     * @param savedInstanceState the saved state, or <code>null</code> if this
     *        screen was not recreated
     */
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

    /**
     * Passes the run settings from the <code>Intent</code> to
     * <code>TestController</code> to validate.
     *
     * @return <code>true</code> if the settings are valid, otherwise
     *         <code>false</code>
     */
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

    /** Aborts the run if the user leaves the app before it has finished. */
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

    /** Routes the toolbar arrow and system Back button to the abort check. */
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

    /**
     * Closes this screen if the run is over, otherwise asks the user to confirm
     * aborting it.
     */
    private void onBackRequested() {

        if (isRunOver()) {
            finish();
        } else {
            confirmAbort();
        }
    }

    /**
     * Shows the Abort run dialog. Confirming stops the scroller and marks the
     * run incomplete.
     */
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

    /**
     * Checks whether <code>TestController</code> has given this run its final
     * status.
     *
     * @return <code>true</code> if the run completed, failed or is incomplete
     */
    private boolean isRunOver() {
        TestStatus status = controller.getTestStatus();
        return status == TestStatus.COMPLETED
                || status == TestStatus.FAILED
                || status == TestStatus.INCOMPLETE;
    }

    /** Stops the <code>AutoScroller</code> if it has been started. */
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

    /**
     * Gets the dataset from <code>TestController</code> and adds the selected
     * engine's fragment to the list area.
     */
    private void showEngine() {
        items = controller.prepareDataset();

        // RecyclerView fetching here
        if (TestConfig.ENGINE_RECYCLERVIEW.equals(controller.getRenderingEngine())) {
            getSupportFragmentManager().beginTransaction().replace(R.id.test_list_container, new RecyclerViewFragment()).commit();
            return;
        }

        // ScrollView fetching here
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.test_list_container, new LinearLoadingFragment())
                .commit();
    }

    /**
     * Returns this run's dataset, generated by <code>TestController</code>.
     *
     * @return the items for the list, or <code>null</code> if the run was
     *         cancelled
     */
    public List<Item> getItems() {
        return items;
    }

    /**
     * Shows the loading overlay while the engine builds its rows. Called by the
     * engine fragment.
     *
     * @param rowCount the number of rows being built
     */
    public void showLoading(int rowCount) {
        TextView loadingText = findViewById(R.id.test_loading_text);
        loadingText.setText(getString(R.string.test_loading_rows, formatNumber(rowCount)));
        findViewById(R.id.test_loading_overlay).setVisibility(View.VISIBLE);
        setStatus(R.string.test_status_building);
    }

    /**
     * Starts measuring right before the rows are built, so the build cost is
     * included. Called by the engine fragment.
     */
    public void onBuildStarted() {
        controller.startMonitoring();
    }

    /**
     * Hides the loading overlay and starts auto-scrolling. Called by the engine
     * fragment once its rows are on screen.
     *
     * @param list the scrolling view that holds the rows
     */
    public void onRowsReady(View list) {
        if (isFinishing()) {
            return; // Run was aborted before scrolling started
        }
        findViewById(R.id.test_loading_overlay).setVisibility(View.GONE);
        setStatus(R.string.test_status_scrolling);
        autoScroller = new AutoScroller(list, controller.getDatasetSize(), this);
        autoScroller.start();
    }

    /**
     * Updates the elapsed time and progress bar while scrolling. Called by
     * <code>AutoScroller</code>.
     *
     * @param elapsedMs the time since scrolling started, in milliseconds
     * @param percent the percentage of the list scrolled so far
     */
    @Override
    public void onScrollProgress(long elapsedMs, int percent) {
        showScrollProgress(elapsedMs, percent);
    }

    /**
     * Completes the run, stops measuring and opens the Results screen. Called
     * by <code>AutoScroller</code>.
     *
     * @param elapsedMs the total scrolling time, in milliseconds
     * @param percent the percentage of the list that was scrolled
     */
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
     * Shows the current iteration's results. If iterations remain, the next one
     * is placed under the Results screen and starts when Results is closed.
     *
     * @param result the status and measurements of the run that just finished
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

    /**
     * Shows the elapsed time in the metrics strip and the scroll percentage on
     * the progress bar.
     *
     * @param elapsedMs the time since scrolling started, in milliseconds
     * @param percent the percentage of the list scrolled so far
     */
    private void showScrollProgress(long elapsedMs, int percent) {
        TextView elapsedValue = findViewById(R.id.test_metric_elapsed_value);
        ProgressBar progress = findViewById(R.id.test_progress);
        elapsedValue.setText(getString(R.string.test_elapsed_seconds, elapsedMs / 1000f));
        progress.setProgress(percent);
    }

    /**
     * Shows that the build ran out of memory, then closes the run. Called by
     * the engine fragment.
     *
     * @param rowsBuilt the number of rows built before memory ran out
     */
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

    /**
     * Sets the text of the Status cell in the metrics strip.
     *
     * @param statusResId the string resource ID of the status to show
     */
    private void setStatus(int statusResId) {
        TextView statusValue = findViewById(R.id.test_metric_status_value);
        statusValue.setText(statusResId);
    }

    /**
     * Formats a number with the device's digit grouping, for example 10000
     * becomes "10 000".
     *
     * @param value the number to format
     * @return the formatted number
     */
    private static String formatNumber(long value) {
        return NumberFormat.getIntegerInstance().format(value);
    }
}
