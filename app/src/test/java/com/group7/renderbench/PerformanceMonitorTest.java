package com.group7.renderbench;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.group7.renderbench.monitoring.PerformanceMonitor;

public class PerformanceMonitorTest {

    /**
     * Creates a test-only subclass that stubs the Activity dependency.
     * This lets us test the pure-Java methods without an Android runtime.
     */
    private static class TestableMonitor extends PerformanceMonitor {
        TestableMonitor() {
            super(null);
        }
    }

    @Test
    public void memoryUsageIsPositive() {
        TestableMonitor monitor = new TestableMonitor();
        long usedMemory = monitor.getMemoryUsageMb();
        assertTrue("Memory usage should be zero or greater", usedMemory >= 0);
    }

    @Test
    public void frameTimeStartsAtZero() {
        TestableMonitor monitor = new TestableMonitor();
        assertEquals(0.0, monitor.getFrameTimeMs(), 0.001);
    }

    @Test
    public void frameOverrunStartsAtZero() {
        TestableMonitor monitor = new TestableMonitor();
        assertEquals(0.0, monitor.getFrameOverrunMs(), 0.001);
    }

    @Test
    public void lastFrameIsNotJankyBeforeTracking() {
        TestableMonitor monitor = new TestableMonitor();
        // No frames captured yet, so overrun is 0 and this should be false
        assertEquals(false, monitor.isLastFrameJanky());
    }
}