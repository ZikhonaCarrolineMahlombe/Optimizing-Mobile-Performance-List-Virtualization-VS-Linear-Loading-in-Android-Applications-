package com.group7.renderbench;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PerformanceMonitorTest {

    @Test
    public void memoryUsageIsPositive() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        assertTrue(usedMemory >= 0);
    }
}