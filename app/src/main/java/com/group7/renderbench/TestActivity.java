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

import java.text.NumberFormat;

/** Test Run screen: shows the selected engine's list for one test run. */
public class TestActivity extends AppCompatActivity implements AutoScroller.Listener {

    // Intent extra keys
    public static final String EXTRA_ENGINE = "com.group7.renderbench.EXTRA_ENGINE";
    public static final String EXTRA_DATASET_SIZE = "com.group7.renderbench.EXTRA_DATASET_SIZE";
    public static final String EXTRA_ITERATION = "com.group7.renderbench.EXTRA_ITERATION";
    public static final String EXTRA_TOTAL_ITERATIONS = "com.group7.renderbench.EXTRA_TOTAL_ITERATIONS";

    // Rendering engines
    public static final String ENGINE_SCROLLVIEW = "SCROLLVIEW";
    public static final String ENGINE_RECYCLERVIEW = "RECYCLERVIEW";

    // Allowed dataset sizes
    public static final int[] DATASET_SIZES = {50, 100, 500, 1000, 5000, 10000};

    private String engine;
    private int datasetSize;
    private int iteration;
    private int totalIterations;

    private AutoScroller autoScroller;
    private boolean runFinished;

    /** Builds the Intent that opens this screen with the given run settings. */
    public static Intent newIntent(Context context, String engine, int datasetSize,
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

        if (!readRunSettings()) {
            Toast.makeText(this, R.string.test_invalid_settings, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setUpBackNavigation();
        showRunDetails();

        if (savedInstanceState == null) {
            showEngine();
        }
    }

    private boolean readRunSettings() {
        Intent intent = getIntent();
        engine = intent.getStringExtra(EXTRA_ENGINE);
        datasetSize = intent.getIntExtra(EXTRA_DATASET_SIZE, -1);
        iteration = intent.getIntExtra(EXTRA_ITERATION, 1);
        totalIterations = intent.getIntExtra(EXTRA_TOTAL_ITERATIONS, 1);

        boolean validEngine = ENGINE_SCROLLVIEW.equals(engine) || ENGINE_RECYCLERVIEW.equals(engine);
        boolean validIteration = iteration >= 1 && iteration <= totalIterations;
        return validEngine && isValidDatasetSize(datasetSize) && validIteration;
    }

    private static boolean isValidDatasetSize(int size) {
        for (int allowed : DATASET_SIZES) {
            if (allowed == size) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Leaving the app mid-run makes the run invalid.
        if (autoScroller != null && autoScroller.isRunning()) {
            autoScroller.stop();
            Toast.makeText(this, R.string.test_run_left, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /** Toolbar arrow and back button: ask before leaving, unless the run has finished. */
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

        if (runFinished) {
            finish();
        } else {
            confirmAbort();
        }
    }

    private void confirmAbort() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.test_abort_title)
                .setMessage(R.string.test_abort_message)
                .setPositiveButton(R.string.test_abort_confirm, (dialog, which) -> {
                    // Stop first so onStop doesn't treat this as leaving the app
                    if (autoScroller != null) {
                        autoScroller.stop();
                    }
                    finish();
                })
                .setNegativeButton(R.string.test_abort_cancel, null)
                .show();
    }

    /** Fills in the header and the Rows cell. */
    private void showRunDetails() {
        TextView engineLabel = findViewById(R.id.test_engine_label);
        TextView runDetails = findViewById(R.id.test_run_details);
        TextView rowsValue = findViewById(R.id.test_metric_rows_value);

        String sizeText = formatNumber(datasetSize);

        if (ENGINE_SCROLLVIEW.equals(engine)) {
            engineLabel.setText(R.string.test_engine_scrollview);
        } else {
            engineLabel.setText(R.string.test_engine_recyclerview);
        }
        runDetails.setText(getString(R.string.test_run_details, sizeText, iteration, totalIterations));
        rowsValue.setText(sizeText);
    }

    /** Adds the selected engine's fragment to the list area. */
    private void showEngine() {
        if (ENGINE_RECYCLERVIEW.equals(engine)) {
            // RecyclerView fragment goes here once it is added
            Toast.makeText(this, R.string.test_engine_not_ready, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.test_list_container, LinearLoadingFragment.newInstance(datasetSize))
                .commit();
    }

    /** Shows the loading overlay while the engine builds its rows. Called by the engine fragment. */
    public void showLoading(int rowCount) {
        TextView loadingText = findViewById(R.id.test_loading_text);
        loadingText.setText(getString(R.string.test_loading_rows, formatNumber(rowCount)));
        findViewById(R.id.test_loading_overlay).setVisibility(View.VISIBLE);
        setStatus(R.string.test_status_building);
    }

    /** Hides the loading overlay and starts auto-scrolling. Called by the engine fragment once its rows are on screen. */
    public void onRowsReady(View list) {
        if (isFinishing()) {
            return; // Run was aborted before scrolling started
        }
        findViewById(R.id.test_loading_overlay).setVisibility(View.GONE);
        setStatus(R.string.test_status_scrolling);
        autoScroller = new AutoScroller(list, datasetSize, this);
        autoScroller.start();
    }

    @Override
    public void onScrollProgress(long elapsedMs, int percent) {
        showScrollProgress(elapsedMs, percent);
    }

    @Override
    public void onScrollFinished(long elapsedMs, int percent) {
        runFinished = true;
        showScrollProgress(elapsedMs, percent);
        setStatus(R.string.test_status_done);
    }

    private void showScrollProgress(long elapsedMs, int percent) {
        TextView elapsedValue = findViewById(R.id.test_metric_elapsed_value);
        ProgressBar progress = findViewById(R.id.test_progress);
        elapsedValue.setText(getString(R.string.test_elapsed_seconds, elapsedMs / 1000f));
        progress.setProgress(percent);
    }

    /** Shows that the build ran out of memory, then closes the run. Called by the engine fragment. */
    public void onBuildFailed(int rowsBuilt) {
        findViewById(R.id.test_loading_overlay).setVisibility(View.GONE);
        setStatus(R.string.test_status_failed);
        new AlertDialog.Builder(this)
                .setTitle(R.string.test_oom_title)
                .setMessage(getString(R.string.test_oom_message,
                        formatNumber(rowsBuilt), formatNumber(datasetSize)))
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
