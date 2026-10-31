package com.group7.renderbench.controllers;

import android.app.Activity;

import com.group7.renderbench.TestConfig;
import com.group7.renderbench.entities.Item;
import com.group7.renderbench.generators.DataGenerator;
import com.group7.renderbench.monitoring.PerformanceMonitor;

import java.util.List;

/**
 * Manages the benchmark testing process.
 * Handles test configuration, dataset preparation,
 * test state changes, and performance monitoring.
 */
public class TestController {

    /**
     * Represents the current execution status of a benchmark test.
     */
    public enum TestStatus {
        NOT_STARTED,
        PREPARING,
        READY,
        RUNNING,
        COMPLETED,
        FAILED,
        INCOMPLETE
    }

    private static final int[] DATASET_SIZES = TestConfig.DATASET_SIZES;

    private static final int MAX_ITERATIONS = TestConfig.ITERATIONS;

    private final DataGenerator dataGenerator;
    private final PerformanceMonitor performanceMonitor;

    private String renderingEngine;
    private int datasetSize;
    private int iteration;
    private int totalIterations;

    private TestStatus testStatus = TestStatus.NOT_STARTED;
    private boolean configured;
    private long startingBatteryCurrentUa = 0;

    /**
     * Creates a new test controller.
     *
     * @param activity the activity used for performance monitoring
     * @throws IllegalArgumentException if activity is null
     */
    public TestController(Activity activity) {
        if (activity == null) {
            throw new IllegalArgumentException(
                    "Activity cannot be null."
            );
        }

        dataGenerator = new DataGenerator();
        performanceMonitor = new PerformanceMonitor(activity);
    }

    /**
     * Configures the benchmark parameters for a specific test run.
     *
     * @param engine the rendering engine to test (ScrollView or RecyclerView)
     * @param size the dataset item count (must be a valid size from TestConfig)
     * @param iterationNumber the current iteration index being executed
     * @param iterationsToRun the total number of iterations scheduled for this test batch
     * @throws IllegalArgumentException if parameters fall outside valid constraints
     * @throws IllegalStateException if called while a test is currently running or preparing
     */
    public void configureTest(
            String engine,
            int size,
            int iterationNumber,
            int iterationsToRun) {

        if (TestConfig.ENGINE_SCROLLVIEW.equals(engine)) {
            renderingEngine = TestConfig.ENGINE_SCROLLVIEW;
        } else if (TestConfig.ENGINE_RECYCLERVIEW.equals(engine)) {
            renderingEngine = TestConfig.ENGINE_RECYCLERVIEW;
        } else {
            throw new IllegalArgumentException(
                    "Invalid rendering engine."
            );
        }

        if (!isValidDatasetSize(size)) {
            throw new IllegalArgumentException(
                    "Invalid dataset size: " + size
            );
        }

        if (iterationsToRun < 1
                || iterationsToRun > MAX_ITERATIONS) {
            throw new IllegalArgumentException(
                    "Total iterations must be between 1 and " + MAX_ITERATIONS + "."
            );
        }

        if (iterationNumber < 1
                || iterationNumber > iterationsToRun) {
            throw new IllegalArgumentException(
                    "Invalid iteration number."
            );
        }

        // Prevent modifying state while a test is actively executing.
        if (testStatus == TestStatus.RUNNING
                || testStatus == TestStatus.PREPARING) {
            throw new IllegalStateException(
                    "Cannot reconfigure a running test."
            );
        }

        datasetSize = size;
        iteration = iterationNumber;
        totalIterations = iterationsToRun;
        testStatus = TestStatus.NOT_STARTED;
        configured = true;
    }

    /**
     * Generates the dataset for the current test configuration.
     *
     * @return the generated dataset
     * @throws IllegalStateException if the test has not been configured
     */
    public List<Item> prepareDataset() {
        ensureConfigured();

        if (testStatus == TestStatus.RUNNING) {
            throw new IllegalStateException(
                    "Cannot generate data during a test."
            );
        }

        testStatus = TestStatus.PREPARING;

        try {
            List<Item> items =
                    dataGenerator.generateDataset(datasetSize);

            if (items == null || items.size() != datasetSize) {
                throw new IllegalStateException(
                        "Generated dataset has an unexpected size."
                );
            }

            testStatus = TestStatus.READY;
            return items;

        } catch (RuntimeException exception) {
            testStatus = TestStatus.FAILED;
            throw exception;
        }
    }

    /**
     * Starts collecting performance data for the current test.
     *
     * @throws IllegalStateException if monitoring cannot be started
     */
    public void startMonitoring() {
        ensureConfigured();

        if (testStatus == TestStatus.RUNNING) {
            throw new IllegalStateException(
                    "Monitoring is already running."
            );
        }

        if (testStatus == TestStatus.COMPLETED
                || testStatus == TestStatus.FAILED
                || testStatus == TestStatus.INCOMPLETE) {
            throw new IllegalStateException(
                    "Configure a new test before starting again."
            );
        }

        // Capture starting baseline metrics before tracking begins.
        startingBatteryCurrentUa =
                performanceMonitor.getBatteryCurrentNow();

        performanceMonitor.startTracking();
        testStatus = TestStatus.RUNNING;
    }

    /**
     * Stops performance metric collection and packages the captured data into a results object.
     *
     * @param completed true if the test finished successfully; false if interrupted
     * @return a {@link TestResult} containing all captured performance metrics for this iteration
     * @throws IllegalStateException if no test is currently running
     */
    public TestResult stopMonitoring(boolean completed) {
        if (testStatus != TestStatus.RUNNING) {
            throw new IllegalStateException(
                    "No test is currently running."
            );
        }

        // Stop collection before reading the final measurements.
        performanceMonitor.stopTracking();

        long cpuTimeMs = performanceMonitor.getCpuTimeMs();
        long memoryUsageMb = performanceMonitor.getMemoryUsageMb();
        double frameTimeMs = performanceMonitor.getFrameTimeMs();

        long endingBatteryCurrentUa =
                performanceMonitor.getBatteryCurrentNow();

        testStatus = completed
                ? TestStatus.COMPLETED
                : TestStatus.INCOMPLETE;

        return new TestResult(
                renderingEngine,
                datasetSize,
                iteration,
                totalIterations,
                testStatus,
                cpuTimeMs,
                memoryUsageMb,
                frameTimeMs,
                startingBatteryCurrentUa,
                endingBatteryCurrentUa
        );
    }

    /**
     * Marks the current test as failed
     */
    public void markFailed() {
        if (testStatus == TestStatus.RUNNING) {
            performanceMonitor.stopTracking();
        }

        testStatus = TestStatus.FAILED;
    }

    /**
     * Marks the current tests as incomplete
     */
    public void markIncomplete() {
        if (testStatus == TestStatus.RUNNING) {
            performanceMonitor.stopTracking();
        }

        testStatus = TestStatus.INCOMPLETE;
    }

    /**
     * Ensures that the test has been configured
     *
     * @throws IllegalStateException if the controller is not configured
     */
    private void ensureConfigured() {
        if (!configured) {
            throw new IllegalStateException(
                    "Configure the test before running it."
            );
        }
    }

    /**
     * Checks whether a dataset size is valid
     *
     * @param size the dataset size to check
     * @return true if the size is valid; otherwise false
     */
    private boolean isValidDatasetSize(int size) {
        for (int allowedSize : DATASET_SIZES) {
            if (allowedSize == size) {
                return true;
            }
        }

        return false;
    }

    public String getRenderingEngine() {
        return renderingEngine;
    }

    public int getDatasetSize() {
        return datasetSize;
    }

    public int getIteration() {
        return iteration;
    }

    public int getTotalIterations() {
        return totalIterations;
    }

    public TestStatus getTestStatus() {
        return testStatus;
    }

    public int[] getAvailableDatasetSizes() {
        return DATASET_SIZES.clone();
    }

    /**
     * Stores the performance data collected during a test run
     */
    public static class TestResult implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        public final String renderingEngine;
        public final int datasetSize;
        public final int iteration;
        public final int totalIterations;
        public final TestStatus status;
        public final long cpuTimeMs;
        public final long memoryUsageMb;
        public final double frameTimeMs;
        public final long startingBatteryCurrentUa;
        public final long endingBatteryCurrentUa;

        /**
         * Creates a test result containing the data collected during a test
         *
         * @param renderingEngine the rendering engine that was tested
         * @param datasetSize the item count used in the dataset
         * @param iteration the current iteration index
         * @param totalIterations the total iterations run
         * @param status the final completion status
         * @param cpuTimeMs total thread CPU time consumed in milliseconds
         * @param memoryUsageMb Java heap memory consumed in megabytes
         * @param frameTimeMs captured frame time metric
         * @param startingBatteryCurrentUa battery current draw at test start (microamperes)
         * @param endingBatteryCurrentUa battery current draw at test completion (microamperes)
         */
        public TestResult(
                String renderingEngine,
                int datasetSize,
                int iteration,
                int totalIterations,
                TestStatus status,
                long cpuTimeMs,
                long memoryUsageMb,
                double frameTimeMs,
                long startingBatteryCurrentUa,
                long endingBatteryCurrentUa) {

            this.renderingEngine = renderingEngine;
            this.datasetSize = datasetSize;
            this.iteration = iteration;
            this.totalIterations = totalIterations;
            this.status = status;
            this.cpuTimeMs = cpuTimeMs;
            this.memoryUsageMb = memoryUsageMb;
            this.frameTimeMs = frameTimeMs;
            this.startingBatteryCurrentUa = startingBatteryCurrentUa;
            this.endingBatteryCurrentUa = endingBatteryCurrentUa;
        }
    }
}

