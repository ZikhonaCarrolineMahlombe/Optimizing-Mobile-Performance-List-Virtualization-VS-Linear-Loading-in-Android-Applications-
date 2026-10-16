package com.group7.renderbench.monitoring;

import android.app.Activity;
import android.os.BatteryManager;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.view.FrameMetrics;
import android.view.Window;

/**
 * Captures performance metrics during a test run.
 * Uses native Android APIs to measure RAM usage, frame time,
 * frame overrun, battery current, and CPU time.
 * ADB shell commands are not used because Android blocks normal
 * apps from running them.
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
     * The Activity is needed for the frame metrics listener and
     * the battery service.
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
     * Calling this method more than once while tracking is active
     * has no effect.
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
     * This must be called to avoid leaking the listener when the
     * Activity is destroyed.
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
     * Returns the instantaneous battery current in microamperes.
     * Negative values mean the battery is discharging (power being
     * consumed). Positive values mean the battery is charging.
     *
     * @return battery current in microamperes, or 0 if unavailable
     */
    public long getBatteryCurrentNow() {
        BatteryManager batteryManager =
                (BatteryManager) activity.getSystemService(Activity.BATTERY_SERVICE);
        if (batteryManager == null) {
            return 0;
        }
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
    }

    /**
     * Returns the CPU time used by the current thread since the last call.
     * This is not the total app CPU usage, only the CPU time spent on
     * the calling thread.
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