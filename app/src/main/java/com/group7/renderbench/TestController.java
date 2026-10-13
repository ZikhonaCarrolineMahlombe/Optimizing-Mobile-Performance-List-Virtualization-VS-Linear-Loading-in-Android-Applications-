package com.group7.renderbench;

import android.app.Activity;

import com.group7.renderbench.entities.Item;
import com.group7.renderbench.generators.DataGenerator;
import com.group7.renderbench.monitoring.PerformanceMonitor;

import java.util.List;

public class TestController {

    public enum TestStatus {
        NOT_STARTED,
        PREPARING,
        READY,
        RUNNING,
        COMPLETED,
        FAILED,
        INCOMPLETE
    }

    private static final int[] DATASET_SIZES = {
            50, 100, 500, 1000, 5000, 10000
    };

    private static final int MAX_ITERATIONS = 5;

    private final DataGenerator dataGenerator;
    private final PerformanceMonitor performanceMonitor;

    private String renderingEngine;
    private int datasetSize;
    private int iteration;
    private int totalIterations;

    private TestStatus testStatus = TestStatus.NOT_STARTED;
    private boolean configured;
    private int startingBatteryLevel = -1;

    public TestController(Activity activity) {
        if (activity == null) {
            throw new IllegalArgumentException(
                    "Activity cannot be null."
            );
        }

        dataGenerator = new DataGenerator();
        performanceMonitor = new PerformanceMonitor(activity);
    }

    public void configureTest(
            String engine,
            int size,
            int iterationNumber,
            int iterationsToRun) {

        if (TestActivity.ENGINE_SCROLLVIEW.equals(engine)) {
            renderingEngine = TestActivity.ENGINE_SCROLLVIEW;
        } else if (TestActivity.ENGINE_RECYCLERVIEW.equals(engine)) {
            renderingEngine = TestActivity.ENGINE_RECYCLERVIEW;
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
                    "Total iterations must be between 1 and 5."
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
        startingBatteryLevel =
                performanceMonitor.getBatteryLevel();

        performanceMonitor.startTracking();
        testStatus = TestStatus.RUNNING;
    }

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

        int endingBatteryLevel =
                performanceMonitor.getBatteryLevel();

        int batteryDrain = -1;

        // Calculate battery percentage drop if both levels are valid.
        if (startingBatteryLevel >= 0
                && endingBatteryLevel >= 0) {
            batteryDrain = Math.max(
                    0,
                    startingBatteryLevel - endingBatteryLevel
            );
        }

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
                startingBatteryLevel,
                endingBatteryLevel,
                batteryDrain
        );
    }

    public void markFailed() {
        if (testStatus == TestStatus.RUNNING) {
            performanceMonitor.stopTracking();
        }

        testStatus = TestStatus.FAILED;
    }

    public void markIncomplete() {
        if (testStatus == TestStatus.RUNNING) {
            performanceMonitor.stopTracking();
        }

        testStatus = TestStatus.INCOMPLETE;
    }

    private void ensureConfigured() {
        if (!configured) {
            throw new IllegalStateException(
                    "Configure the test before running it."
            );
        }
    }

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
        public final int startingBatteryLevel;
        public final int endingBatteryLevel;
        public final int batteryDrainPercent;

        public TestResult(
                String renderingEngine,
                int datasetSize,
                int iteration,
                int totalIterations,
                TestStatus status,
                long cpuTimeMs,
                long memoryUsageMb,
                double frameTimeMs,
                int startingBatteryLevel,
                int endingBatteryLevel,
                int batteryDrainPercent) {

            this.renderingEngine = renderingEngine;
            this.datasetSize = datasetSize;
            this.iteration = iteration;
            this.totalIterations = totalIterations;
            this.status = status;
            this.cpuTimeMs = cpuTimeMs;
            this.memoryUsageMb = memoryUsageMb;
            this.frameTimeMs = frameTimeMs;
            this.startingBatteryLevel = startingBatteryLevel;
            this.endingBatteryLevel = endingBatteryLevel;
            this.batteryDrainPercent = batteryDrainPercent;
        }
    }
}
