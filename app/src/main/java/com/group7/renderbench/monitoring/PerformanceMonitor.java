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

public class PerformanceMonitor {

    private final Activity activity;

    private Window.OnFrameMetricsAvailableListener frameListener;
    private double frameTimeMs = 0;
    private boolean isTracking = false;
    private long threadCpuTime = 0;

    public PerformanceMonitor(Activity activity) {
        this.activity = activity;
    }

    public void startTracking() {
        if (isTracking) return;

        frameListener = (window, frameMetrics, dropCount) -> {
            long totalNs = frameMetrics.getMetric(FrameMetrics.TOTAL_DURATION);
            frameTimeMs = totalNs / 1_000_000.0;
        };

        activity.getWindow().addOnFrameMetricsAvailableListener(
                frameListener,
                new Handler(Looper.getMainLooper())
        );

        threadCpuTime = Debug.threadCpuTimeNanos();
        isTracking = true;
    }

    public void stopTracking() {
        if (!isTracking) return;

        if (frameListener != null) {
            activity.getWindow().removeOnFrameMetricsAvailableListener(frameListener);
        }

        isTracking = false;
    }

    public double getFrameTimeMs() {
        return frameTimeMs;
    }

    public long getMemoryUsageMb() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return usedMemory / (1024 * 1024);
    }

    public int getBatteryLevel() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = activity.registerReceiver(null, filter);
        if (batteryStatus == null) return -1;

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        if (level < 0 || scale <= 0) return -1;
        return (int) ((level / (float) scale) * 100);
    }

    public long getCpuTimeMs() {
        long now = Debug.threadCpuTimeNanos();
        long used = (now - threadCpuTime) / 1_000_000;
        threadCpuTime = now;
        return used;
    }
}