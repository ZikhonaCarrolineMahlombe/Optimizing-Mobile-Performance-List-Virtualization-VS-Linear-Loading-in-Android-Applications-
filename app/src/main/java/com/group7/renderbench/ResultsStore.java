package com.group7.renderbench;

import android.content.Context;
import android.content.SharedPreferences;

import com.group7.renderbench.controllers.TestController.TestResult;
import com.group7.renderbench.controllers.TestController.TestStatus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Saves finished benchmark runs on the device so the Results screen can show
 * them after the app is restarted. Runs are stored as one JSON array in
 * SharedPreferences, newest run first.
 */
public class ResultsStore {
    private static final String PREFERENCES_NAME = "renderbench_results";
    private static final String KEY_RUNS = "runs";

    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_ENGINE = "engine";
    private static final String KEY_DATASET_SIZE = "datasetSize";
    private static final String KEY_ITERATION = "iteration";
    private static final String KEY_TOTAL_ITERATIONS = "totalIterations";
    private static final String KEY_STATUS = "status";
    private static final String KEY_CPU_MS = "cpuTimeMs";
    private static final String KEY_MEMORY_MB = "memoryUsageMb";
    private static final String KEY_FRAME_MS = "frameTimeMs";
    private static final String KEY_BATTERY_START = "startingBatteryCurrentUa";
    private static final String KEY_BATTERY_END = "endingBatteryCurrentUa";

    private ResultsStore() {}

    /** One saved run: the benchmark result plus when it was recorded. */
    public static final class SavedRun {
        /** The finished benchmark run. */
        public final TestResult result;
        /** Wall clock time in milliseconds when the run was saved. */
        public final long timestampMs;

        /**
         * Pairs a result with its recording time.
         *
         * @param result the finished benchmark run
         * @param timestampMs wall clock time in milliseconds
         */
        SavedRun(TestResult result, long timestampMs) {
            this.result = result;
            this.timestampMs = timestampMs;
        }
    }

    /**
     * Appends a finished run to the saved history.
     *
     * @param context any context used to reach SharedPreferences
     * @param result the run to store, ignored when null
     */
    public static void save(Context context, TestResult result) {
        if (result == null) {
            return;
        }

        try {
            JSONArray runs = readRuns(context);
            runs.put(toJson(new SavedRun(result, System.currentTimeMillis())));

            writeRuns(context, runs);
        } catch (JSONException exception) {
            // A corrupt record must never crash a benchmark run
        }
    }

    /**
     * All saved runs, newest first
     *
     * @param context any context used to reach SharedPreferences
     * @return an unmodifiable list of saved runs
     */
    public static List<SavedRun> load(Context context) {
        List<SavedRun> savedRuns = new ArrayList<>();

        try {
            JSONArray runs = readRuns(context);

            for (int i = 0; i < runs.length(); i++) {
                JSONObject json = runs.optJSONObject(i);

                if (json == null) {
                    continue;
                }

                SavedRun run = fromJson(json);

                if (run != null) {
                    savedRuns.add(run);
                }
            }
        } catch (JSONException exception) {
            // Unreadable history behaves like an empty history.
        }

        // Newest runs first, so the latest measurement is at the top.
        savedRuns.sort(Comparator.comparingLong((SavedRun run) -> run.timestampMs).reversed());

        return Collections.unmodifiableList(savedRuns);
    }

    /**
     * Deletes every saved run.
     *
     * @param context any context used to reach SharedPreferences
     */
    public static void clear(Context context) {
        writeRuns(context, new JSONArray());
    }

    /**
     * Reads the stored JSON array, falling back to an empty one when the
     * stored text is missing or no longer parses.
     *
     * @param context any context used to reach SharedPreferences
     * @return the stored runs as a JSON array
     * @throws JSONException when the stored text is not valid JSON
     */
    private static JSONArray readRuns(Context context) throws JSONException {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);

        String stored = preferences.getString(KEY_RUNS, "[]");

        // A history that no longer parses is replaced instead of reused.
        try {
            return new JSONArray(stored);
        } catch (JSONException exception) {
            return new JSONArray();
        }
    }

    /**
     * Replaces the stored history with the given array.
     *
     * @param context any context used to reach SharedPreferences
     * @param runs the array to persist
     */
    private static void writeRuns(Context context, JSONArray runs) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);

        preferences.edit().putString(KEY_RUNS, runs.toString()).apply();
    }

    /**
     * Converts a saved run into its JSON record.
     *
     * @param run the run to encode
     * @return the JSON object stored in the history
     * @throws JSONException when a value cannot be written
     */
    private static JSONObject toJson(SavedRun run) throws JSONException {
        TestResult result = run.result;

        JSONObject json = new JSONObject();
        json.put(KEY_TIMESTAMP, run.timestampMs);
        json.put(KEY_ENGINE, result.renderingEngine);
        json.put(KEY_DATASET_SIZE, result.datasetSize);
        json.put(KEY_ITERATION, result.iteration);
        json.put(KEY_TOTAL_ITERATIONS, result.totalIterations);
        json.put(KEY_STATUS, result.status.toString());
        json.put(KEY_CPU_MS, result.cpuTimeMs);
        json.put(KEY_MEMORY_MB, result.memoryUsageMb);
        json.put(KEY_FRAME_MS, result.frameTimeMs);
        json.put(KEY_BATTERY_START, result.startingBatteryCurrentUa);
        json.put(KEY_BATTERY_END, result.endingBatteryCurrentUa);

        return json;
    }

    /**
     * Rebuilds a saved run from its JSON record.
     *
     * @param json the record to decode
     * @return the decoded run, or null when the record is damaged
     */
    private static SavedRun fromJson(JSONObject json) {
        try {
            TestResult result = new TestResult(
                    json.getString(KEY_ENGINE),
                    json.getInt(KEY_DATASET_SIZE),
                    json.getInt(KEY_ITERATION),
                    json.getInt(KEY_TOTAL_ITERATIONS),
                    TestStatus.valueOf(json.getString(KEY_STATUS)),
                    json.getLong(KEY_CPU_MS),
                    json.getLong(KEY_MEMORY_MB),
                    json.getDouble(KEY_FRAME_MS),
                    json.getLong(KEY_BATTERY_START),
                    json.getLong(KEY_BATTERY_END)
            );

            return new SavedRun(result, json.getLong(KEY_TIMESTAMP));
        } catch (JSONException | IllegalArgumentException exception) {
            return null; // Skip the damaged record, keep the rest of the history
        }
    }
}
