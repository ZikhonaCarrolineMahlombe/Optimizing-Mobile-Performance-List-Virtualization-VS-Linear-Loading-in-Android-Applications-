package com.group7.renderbench.monitoring;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.FrameMetrics;
import android.view.Window;

public class PerformanceMonitor {

    private final Activity activity;
    private double frameTimeMs = 0;
    private boolean isTracking = false;

    public PerformanceMonitor(Activity activity) {
        this.activity = activity;
    }

    public void startTracking() {
        if (isTracking) return;
        activity.getWindow().addOnFrameMetricsAvailableListener(
                (window, frameMetrics, dropCount) -> {
                    long totalNs = frameMetrics.getMetric(FrameMetrics.TOTAL_DURATION);
                    frameTimeMs = totalNs / 1_000_000.0;
                },
                new Handler(Looper.getMainLooper())
        );
        isTracking = true;
    }

    public void stopTracking() {
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
}