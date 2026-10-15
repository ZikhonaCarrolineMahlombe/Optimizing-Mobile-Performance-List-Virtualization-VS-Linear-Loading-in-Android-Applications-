package com.group7.renderbench.monitoring;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.view.FrameMetrics;
import android.view.Window;

/**
 * Captures performance metrics during a test run.
 * Uses native Android APIs to measure RAM usage, frame time,
 * frame overrun, battery level, and CPU time.
 */
public class PerformanceMonitor {

    private final Activity activity;

    private Window.OnFrameMetricsAvailableListener frameListener;
    private double frameTimeMs = 0;
    private double frameOverrunMs = 0;
    private boolean isTracking = false;
    private long threadCpuTime = 0;

    /**
     * Creates a monitor tied to the given Activity.
     *
     * @param activity the Activity running the test
     */
    public PerformanceMonitor(Activity activity) {
        this.activity = activity;
    }

    /**
     * Starts tracking frame time, frame overrun, and CPU time.
     * Registers a frame metrics listener on the Activity's window
     * and records the current CPU time as a baseline.
     */
    public void startTracking() {
        if (isTracking) return;

        frameListener = (window, frameMetrics, dropCount) -> {
            long totalNs = frameMetrics.getMetric(FrameMetrics.TOTAL_DURATION);
            long deadlineNs = frameMetrics.getMetric(FrameMetrics.DEADLINE);

            frameTimeMs = totalNs / 1_000_000.0;

            // Positive value = frame missed its deadline = stutter
            // Negative value = frame finished early
            frameOverrunMs = (totalNs - deadlineNs) / 1_000_000.0;
        };

        activity.getWindow().addOnFrameMetricsAvailableListener(
                frameListener,
                new Handler(Looper.getMainLooper())
        );

        threadCpuTime = Debug.threadCpuTimeNanos();
        isTracking = true;
    }

    /**
     * Stops tracking and removes the frame metrics listener.
     */
    public void stopTracking() {
        if (!isTracking) return;

        if (frameListener != null) {
            activity.getWindow().removeOnFrameMetricsAvailableListener(frameListener);
        }

        isTracking = false;
    }

    /**
     * Returns the most recent frame time in milliseconds.
     *
     * @return the latest frame time in milliseconds
     */
    public double getFrameTimeMs() {
        return frameTimeMs;
    }

    /**
     * Returns the most recent frame overrun in milliseconds.
     * A positive value means the frame missed its deadline and
     * caused a visible stutter.
     *
     * @return the latest frame overrun in milliseconds
     */
    public double getFrameOverrunMs() {
        return frameOverrunMs;
    }

    /**
     * Returns true if the last captured frame caused a stutter.
     *
     * @return true if the frame overran its deadline
     */
    public boolean isLastFrameJanky() {
        return frameOverrunMs > 0;
    }

    /**
     * Returns the current Java heap memory usage in megabytes.
     *
     * @return used memory in MB
     */
    public long getMemoryUsageMb() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return usedMemory / (1024 * 1024);
    }

    /**
     * Returns the current battery level as a percentage.
     *
     * @return the battery percentage between 0 and 100,
     *         or -1 if unavailable
     */
    public int getBatteryLevel() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = activity.registerReceiver(null, filter);
        if (batteryStatus == null) return -1;

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        if (level < 0 || scale <= 0) return -1;
        return (int) ((level / (float) scale) * 100);
    }

    /**
     * Returns the CPU time used by the current thread since the last call.
     *
     * @return CPU time in milliseconds
     */
    public long getCpuTimeMs() {
        long now = Debug.threadCpuTimeNanos();
        long used = (now - threadCpuTime) / 1_000_000;
        threadCpuTime = now;
        return used;
    }
}