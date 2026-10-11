package com.group7.renderbench;

import android.view.Choreographer;
import android.view.View;

/** Scrolls a list down at a fixed speed until the end or the time limit. Shared by both engines. */
public class AutoScroller implements Choreographer.FrameCallback {

    // Same speed and time limit for every run
    public static final int SCROLL_SPEED_DP_PER_SECOND = 2000;
    public static final long MAX_DURATION_MS = 30_000;

    private static final long PROGRESS_INTERVAL_MS = 1000;

    /** Receives scroll updates on the UI thread. */
    public interface Listener {
        void onScrollProgress(long elapsedMs, int percent);

        void onScrollFinished(long elapsedMs, int percent);
    }

    private final View list;
    private final Listener listener;
    private final float pixelsPerMs;
    private final int totalDistancePx;

    private boolean running;
    private long startTimeNanos = -1;
    private long lastFrameNanos;
    private long lastProgressMs;
    private float pendingPx;
    private int scrolledPx;

    /** The list must already be laid out, so its height is known. */
    public AutoScroller(View list, int rowCount, Listener listener) {
        this.list = list;
        this.listener = listener;
        float density = list.getResources().getDisplayMetrics().density;
        pixelsPerMs = SCROLL_SPEED_DP_PER_SECOND * density / 1000f;
        int rowHeightPx = list.getResources().getDimensionPixelSize(R.dimen.item_row_height);
        totalDistancePx = Math.max(1, rowCount * rowHeightPx - list.getHeight());
    }

    public void start() {
        if (!running) {
            running = true;
            Choreographer.getInstance().postFrameCallback(this);
        }
    }

    public void stop() {
        running = false;
        Choreographer.getInstance().removeFrameCallback(this);
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!running) {
            return;
        }
        if (startTimeNanos < 0) {
            startTimeNanos = frameTimeNanos;
            lastFrameNanos = frameTimeNanos;
        }
        long elapsedMs = (frameTimeNanos - startTimeNanos) / 1_000_000;

        // Distance is based on time passed, so dropped frames don't slow the scroll down
        pendingPx += (frameTimeNanos - lastFrameNanos) / 1_000_000f * pixelsPerMs;
        lastFrameNanos = frameTimeNanos;
        int dy = (int) pendingPx;
        if (dy > 0) {
            pendingPx -= dy;
            list.scrollBy(0, dy);
            scrolledPx += dy;
        }

        boolean reachedEnd = !list.canScrollVertically(1);
        if (reachedEnd || elapsedMs >= MAX_DURATION_MS) {
            running = false;
            listener.onScrollFinished(elapsedMs, reachedEnd ? 100 : percentScrolled());
            return;
        }

        if (elapsedMs - lastProgressMs >= PROGRESS_INTERVAL_MS) {
            lastProgressMs = elapsedMs;
            listener.onScrollProgress(elapsedMs, percentScrolled());
        }
        Choreographer.getInstance().postFrameCallback(this);
    }

    private int percentScrolled() {
        return (int) Math.min(100, scrolledPx * 100L / totalDistancePx);
    }
}
