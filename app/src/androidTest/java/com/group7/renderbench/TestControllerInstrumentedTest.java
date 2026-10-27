package com.group7.renderbench;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.group7.renderbench.controllers.TestController;
import com.group7.renderbench.entities.Item;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * Instrumentation tests for validating TestController behavior.
 *
 * These tests run in an Android environment because TestController
 * requires an Activity to initialize its PerformanceMonitor.
 *
 * The tests verify that invalid test configurations are rejected
 * and that dataset preparation returns the requested number of items.
 */
@RunWith(AndroidJUnit4.class)
public class TestControllerInstrumentedTest {

    /**
     * V1: Verifies that the controller rejects an invalid rendering engine.
     *
     * Expected result: configureTest() throws IllegalArgumentException.
     */
    @Test
    public void V1_invalidEngineIsRejected() {
        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(MainActivity.class)) {

            scenario.onActivity(activity -> {
                TestController controller =
                        new TestController(activity);

                try {
                    controller.configureTest(
                            "INVALID_ENGINE", 50, 1, 5);
                    fail("Expected IllegalArgumentException");
                } catch (IllegalArgumentException expected) {
                    // Expected: the invalid engine is rejected.
                }
            });
        }
    }

    /**
     * V2: Verifies that unsupported dataset sizes are rejected.
     *
     * Expected result: configureTest() throws IllegalArgumentException
     * for dataset sizes that are not supported by TestConfig.
     */
    @Test
    public void V2_invalidDatasetSizeIsRejected() {
        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(MainActivity.class)) {

            scenario.onActivity(activity -> {
                TestController controller =
                        new TestController(activity);

                int[] invalidSizes = {0, 250};

                for (int size : invalidSizes) {
                    try {
                        controller.configureTest(
                                TestConfig.ENGINE_SCROLLVIEW,
                                size, 1, 5);
                        fail("Expected rejection for size " + size);
                    } catch (IllegalArgumentException expected) {
                        // Expected: the unsupported size is rejected.
                    }
                }
            });
        }
    }

    /**
     * V3: Verifies that a running test cannot be reconfigured.
     *
     * The controller is configured and started before a second
     * configuration is attempted.
     *
     * Expected result: configureTest() throws IllegalStateException.
     */
    @Test
    public void V3_reconfigurationWhileRunningIsRejected() {
        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(MainActivity.class)) {

            scenario.onActivity(activity -> {
                TestController controller =
                        new TestController(activity);

                controller.configureTest(
                        TestConfig.ENGINE_SCROLLVIEW, 50, 1, 5);

                controller.startMonitoring();

                assertEquals(
                        TestController.TestStatus.RUNNING,
                        controller.getTestStatus());

                try {
                    controller.configureTest(
                            TestConfig.ENGINE_RECYCLERVIEW,
                            100, 1, 5);

                    fail("Expected IllegalStateException");
                } catch (IllegalStateException expected) {
                    // Expected: reconfiguration is rejected.
                } finally {
                    // Stop monitoring to clean up the running test.
                    if (controller.getTestStatus()
                            == TestController.TestStatus.RUNNING) {
                        controller.stopMonitoring(false);
                    }
                }
            });
        }
    }

    /**
     * V4: Verifies that an iteration number outside the allowed range
     * is rejected.
     *
     * Expected result: configureTest() throws IllegalArgumentException
     * when iteration 6 is specified for a five-iteration batch.
     */
    @Test
    public void V4_invalidIterationIsRejected() {
        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(MainActivity.class)) {

            scenario.onActivity(activity -> {
                TestController controller =
                        new TestController(activity);

                try {
                    controller.configureTest(
                            TestConfig.ENGINE_SCROLLVIEW,
                            50, 6, 5);

                    fail("Expected IllegalArgumentException");
                } catch (IllegalArgumentException expected) {
                    // Expected: the out-of-range iteration is rejected.
                }
            });
        }
    }

    /**
     * V5: Verifies that dataset preparation returns the requested
     * number of items and updates the controller state correctly.
     *
     * Expected result: 50 items are returned and the controller
     * status becomes READY.
     */
    @Test
    public void V5_preparedDatasetHasRequestedSize() {
        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(MainActivity.class)) {

            scenario.onActivity(activity -> {
                TestController controller =
                        new TestController(activity);

                controller.configureTest(
                        TestConfig.ENGINE_SCROLLVIEW, 50, 1, 5);

                List<Item> items = controller.prepareDataset();

                assertNotNull(items);
                assertEquals(50, items.size());

                assertEquals(
                        TestController.TestStatus.READY,
                        controller.getTestStatus());
            });
        }
    }
}