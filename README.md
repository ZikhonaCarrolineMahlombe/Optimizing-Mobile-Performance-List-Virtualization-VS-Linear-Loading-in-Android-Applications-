# RenderBench

## Optimizing Mobile Performance: List Virtualization vs Linear Loading in Android Applications

---

## Project Overview

**RenderBench** is a native Android performance testing tool designed to scientifically compare two list-rendering approaches:

1.  **Linear Loading**: `ScrollView` + `LinearLayout` — all item views are inflated and held in memory at once.
2.  **List Virtualization**: `RecyclerView` — only visible views are maintained and recycled as the user scrolls.

The application measures **CPU thread time, Java heap memory, frame time, frame overrun (stutter), and battery current** across dataset sizes ranging from 50 to 10,000 rows. The goal is to provide measurable, empirical evidence for which engine is optimal for resource-constrained devices common in South Africa (e.g., entry-level phones with limited RAM).

## Research Context

This project is part of the **ITMDA3-34** module at Eduvos. It builds on the research proposal and literature review submitted in previous deliverables (ITDMA3-22 and ITMDA3-34). The findings provide practical recommendations for Android developers building for the African market.

---

## Features

*   **Dual Rendering Engines:** Side-by-side comparison of ScrollView and RecyclerView under identical conditions.
*   **Native Metric Capture:** Measures CPU, RAM, frame time, and battery natively without needing a laptop connected via ADB.
*   **Results History Dashboard:** Saves every benchmark iteration locally using `SharedPreferences` (JSON) so data persists across app restarts.
*   **Filter Chips:** Quickly separate data on the dashboard by "All Runs", "ScrollView", or "RecyclerView".
*   **Dynamic CSV Export:** Exports data with a filename that automatically updates based on your active filter (e.g., `renderbench_scrollview_results.csv`).
*   **Device Auto-Detection:** Automatically detects the phone model and includes a "Device" column in the CSV export.
*   **Graceful Hardware Fallbacks:** Handles broken hardware sensors by displaying "N/A" for battery metrics if the device lacks the physical chip.
*   **Forced Light Mode:** Ensures a consistent UI experience across all devices, fixing previous dark-mode color bugs and status bar visibility issues.

---

## Metrics Captured

RenderBench avoids ADB shell commands (which are blocked by Android security) and instead uses native Android APIs inside the app itself:

*   **Java Heap Memory:** Captured via `Runtime.getRuntime()`.
*   **Frame Time & Overrun:** Captured via `Window.OnFrameMetricsAvailableListener`.
*   **Battery Current:** Captured via `BatteryManager.BATTERY_PROPERTY_CURRENT_NOW`.
*   **CPU Thread Time:** Captured via `Debug.threadCpuTimeNanos()`.

---

## Screenshots

| Setup Screen | Test Run (RecyclerView) |
| :---: | :---: |
| <img src="https://github.com/user-attachments/assets/b495f26f-0cf5-4476-87c8-16293410035b" width="300" /> | <img src="https://github.com/user-attachments/assets/d665604d-f26c-41f4-93db-0292661b776e" width="300" /> |
| *Select engine and dataset size* | *Live metrics during automated scroll* |

| Results Screen | History Dashboard |
| :---: | :---: |
| <img src="https://github.com/user-attachments/assets/7ebd7571-7f4e-414f-b72c-a69971b568a4" width="300" /> | <img src="https://github.com/user-attachments/assets/af8543fa-643a-4ac3-9f80-0e63dadee9eb" width="300" /> |
| *Detailed metrics & CSV export* | *Filterable history & engine comparison* |

---

## Architecture

The app follows a strict **3-Tier Layered Architecture** to ensure separation of concerns:

1.  **Presentation Layer:** Activities, Fragments, Adapters, and XML Layouts.
2.  **Business Logic Layer:** `TestController`, `PerformanceMonitor`, `AutoScroller`, `DataGenerator`.
3.  **Data Layer:** `Item` (POJO) and `ResultsStore` (Local Storage).

**Key Engineering Decisions:**
*   **No Database:** We deliberately avoided SQLite/Firebase. Disk I/O introduces latency which would skew the CPU and memory metrics. All test data is generated purely in-memory.
*   **Self-Measuring App:** Instead of relying on external scripts, the app measures its own performance natively.

---

## Testing and Validation

RenderBench has been rigorously tested using a combination of automated and manual methods:

*   **Unit Testing:** JUnit tests cover the `DataGenerator` and the pure-Java logic of the `PerformanceMonitor`.
*   **Instrumentation Testing:** Native Android Instrumentation Tests (using `ActivityScenario`) validate the `TestController` state machine (V1-V5).
*   **Physical Device Testing:** The app was stress-tested on three distinct hardware tiers:
    *   **Samsung Galaxy S9+** (Flagship, 6GB RAM)
    *   **Huawei nova 12i** (Mid-range, 8GB RAM, 90Hz display)
    *   **Mobicel P12** (Entry-level, budget hardware)
*   **Data Validity:** Emulator data and broken hardware sensor data (e.g., the Mobicel's faulty battery sensor) were explicitly excluded from the final statistical analysis.

---

## How to Build

1.  Clone the repository:
    `git clone https://github.com/your-username/RenderBench.git`
2.  Open the project in **Android Studio** (Ladybug or newer recommended).
3.  Allow Gradle to sync and download dependencies.
4.  Connect a physical Android device (recommended for accurate metrics) or launch an emulator.
5.  Click **Run 'app'**.

*Note: For accurate performance benchmarking, a physical device is highly recommended. Emulators will produce synthetic battery and CPU metrics.*

---

## Technologies Used

| Component | Technology |
| :--- | :--- |
| **Language** | Java |
| **UI Framework** | Android XML Layouts (Material 3) |
| **Performance Monitoring** | Native Android APIs (`Runtime`, `FrameMetrics`, `BatteryManager`, `Debug`) |
| **Automated Testing** | UI Automator (via `Choreographer`), JUnit, AndroidX Test |
| **Local Storage** | SharedPreferences (JSON) |
| **Version Control** | Git + GitHub |

---

## Project Structure

The codebase is organized into logical packages to separate concerns:

*   `controllers/TestController.java` — Test lifecycle and state management
*   `entities/Item.java` — Data model (POJO)
*   `generators/DataGenerator.java` — Creates the test datasets in memory
*   `monitoring/PerformanceMonitor.java` — Captures native Android metrics
*   `ListAdapter.java` — RecyclerView adapter
*   `LinearLoadingFragment.java` — ScrollView engine
*   `RecyclerViewFragment.java` — RecyclerView engine
*   `AutoScroller.java` — Automated scrolling logic
*   `TestConfig.java` — Shared constants
*   `MainActivity.java` — Setup screen
*   `TestActivity.java` — Test run host screen
*   `ResultsActivity.java` — Single-run results
*   `ResultHistoryActivity.java` — Results dashboard/history
*   `ResultsStore.java` — Local JSON persistence

---

## Team Members (Group 7)

| Name | Student Number | Role |
| :--- | :--- | :--- |
| **Ammaarah Jacobs** | EDUV4930538 | Group Leader, RecyclerView Engine, Results History Dashboard |
| **Kamogelo Phale** | EDUV4810351 | System Architecture, DataGenerator, PerformanceMonitor |
| **Ruan de Wit** | EDUV4863263 | ScrollView Engine, AutoScroller, QA Bug Fixes |
| **Troy James Bower** | EDUV4865433 | TestController, CSV Export, Instrumentation Tests |
| **Zikhona Carroline Mahlombe** | EDUV4956645 | MainActivity Setup Screen, App Theme, Project Setup |

---

## Branching Strategy

*   `main` – Stable branch. All tested code merges here via reviewed pull requests.
*   `feature/...` – Individual feature branches for each developer.

---

## References

*   ITDMA3-22 Group 7 (2026) *Research Proposal: Evaluating the Impact of List Virtualisation vs. Linear Loading on Mobile Device Resource Consumption in Native Android Applications.* Eduvos.
*   ITMDA3-34 Group 7 (2026) *Deliverables 1-5.* Eduvos.
*   Android Developers (2026) *API Reference for Runtime, FrameMetrics, BatteryManager, and Debug.*

---

**Module:** ITMDA3-34 | **Faculty:** Information Technology | **Campus:** Eduvos Online | **Lecturer:** Marcus Lamola
