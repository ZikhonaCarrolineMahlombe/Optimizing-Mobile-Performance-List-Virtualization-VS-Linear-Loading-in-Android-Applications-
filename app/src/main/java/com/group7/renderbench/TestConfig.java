package com.group7.renderbench;

/** Shared test settings (engine names, dataset sizes and iteration count) used by the Setup screen, TestActivity and TestController. */
public final class TestConfig {

    public static final String ENGINE_SCROLLVIEW = "SCROLLVIEW";
    public static final String ENGINE_RECYCLERVIEW = "RECYCLERVIEW";

    public static final int[] DATASET_SIZES = {50, 100, 500, 1000, 5000, 10000};

    public static final int ITERATIONS = 5;

    /** Holds constants only, so it cannot be instantiated. */
    private TestConfig() {
    }
}
