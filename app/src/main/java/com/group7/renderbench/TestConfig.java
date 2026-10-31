package com.group7.renderbench;

/**
 * Shared test settings (engine names, dataset sizes and iteration count)
 * used by the Setup screen, <code>TestActivity</code> and
 * <code>TestController</code>.
 */
public final class TestConfig {

    public static final String ENGINE_SCROLLVIEW = "SCROLLVIEW";
    public static final String ENGINE_RECYCLERVIEW = "RECYCLERVIEW";

    public static final int[] DATASET_SIZES = {50, 100, 500, 1000, 5000, 10000};

    public static final int ITERATIONS = 5;

    /** Prevents instantiation, because this class only holds constants. */
    private TestConfig() {
    }
}
