package com.group7.renderbench;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import com.group7.renderbench.monitoring.PerformanceMonitor;

import java.lang.reflect.Field;

public class PerformanceMonitorTest {

    /**
     * Test subclass that stubs the Activity dependency.
     */
    private static class TestableMonitor extends PerformanceMonitor {
        TestableMonitor() {
            super(null);
        }

        /**
         * Sets the private frameOverrunMs field via reflection.
         *
         * @param overrunMs the simulated overrun value in milliseconds
         */
        public void simulateFrameOverrun(double overrunMs) {
            try {
                Field field = PerformanceMonitor.class.getDeclaredField("frameOverrunMs");
                field.setAccessible(true);
                field.setDouble(this, overrunMs);
            } catch (Exception e) {
                throw new RuntimeException("Failed to inject overrun for testing", e);
            }
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
        assertFalse("Fresh monitor should not be janky", monitor.isLastFrameJanky());
    }

    @Test
    public void lastFrameIsJankyWhenOverrunIsPositive() {
        TestableMonitor monitor = new TestableMonitor();
        monitor.simulateFrameOverrun(5.0);
        assertTrue("A positive overrun should trigger jank detection", monitor.isLastFrameJanky());
    }
}