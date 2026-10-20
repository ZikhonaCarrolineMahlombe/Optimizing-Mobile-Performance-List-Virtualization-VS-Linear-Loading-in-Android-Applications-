# RenderBench

## Optimizing Mobile Performance: List Virtualization VS Linear Loading in Android Applications

## Project Overview

RenderBench is an Android performance testing application that investigates the performance impact of two list-rendering approaches in native Android applications:

- **Linear Loading**: ScrollView with LinearLayout – all item views are inflated and held in memory at once.
- **List Virtualization**: RecyclerView – only visible views are maintained and recycled as the user scrolls.

The goal is to measure and compare CPU time, Java heap memory, frame time, frame overrun (stutter), and battery current across both approaches using different dataset sizes (50 to 10,000 rows).

## Research Context

This project is part of the ITMDA3-34 module at Eduvos. It builds on the research proposal and literature review submitted in previous deliverables (ITDMA3-22 and ITMDA3-34).

The findings will provide practical recommendations for Android developers, especially those working with resource-constrained devices common in South Africa (e.g., entry-level phones with low RAM).

## Current Status

### Completed (D1 - D4)
- Research proposal and literature review (D1, D2)
- System architecture design, wireframes, and data model (D3)
- Android application implementation (D4)

### In Progress (D5: Tests and Results)
- Running the benchmark on physical devices (Huawei nova 12i, Mobicel P12)
- Collecting performance data and analyzing the results
- Documenting hardware limitations (e.g., simulated emulator batteries, broken sensors on budget devices)

## Core Components

| Component | Description |
|-----------|-------------|
| **Item** | Data model representing a single list row (id, title, description, imageUrl). Immutable. |
| **DataGenerator** | Creates test datasets of sizes: 50, 100, 500, 1,000, 5,000, 10,000. |
| **PerformanceMonitor** | Captures CPU thread time, Java heap memory, frame time, frame overrun, and battery current (µA) using native Android APIs. |
| **TestController** | Manages the test flow, validates parameters, and coordinates the components. |
| **MainActivity** | Setup screen for selecting dataset size and rendering engine. |
| **TestActivity** | Hosts the ScrollView and RecyclerView engines. Runs the auto-scroller and measures metrics. |
| **ResultsActivity** | Displays metrics for a single run and allows CSV export. |
| **ResultHistoryActivity** | Shows an engine comparison dashboard (averages) and a history of all saved runs. |
| **ResultsStore** | Saves benchmark runs locally to SharedPreferences (JSON) so results persist across app restarts. |

## Technologies Used

| Component | Technology |
|-----------|------------|
| **Language** | Java |
| **UI Framework** | Android XML Layouts (Material 3) |
| **Linear Loading** | ScrollView + LinearLayout |
| **List Virtualization** | RecyclerView + Adapter |
| **Performance Monitoring** | Native Android APIs (`Runtime`, `FrameMetrics`, `BatteryManager`, `Debug`) |
| **Automated Testing** | UI Automator (via `Choreographer` for scrolling) |
| **Local Storage** | SharedPreferences (JSON) |
| **Version Control** | Git + GitHub |

## Team Members

| Name | Student Number | Role |
|------|---------------|------|
| Ammaarah Jacobs | EDUV4930538 | Group Leader, User Stories, Use Case Diagram (D3), RecyclerView Engine, Results History (D4) |
| Kamogelo Phale | EDUV4810351 | Architecture Design (D3), DataGenerator, PerformanceMonitor, Item (D4) |
| Ruan de Wit | EDUV4863263 | Wireframes (D3), TestActivity ScrollView, AutoScroller, Iteration Flow (D4) |
| Troy James Bower | EDUV4865433 | Topic Selection, Requirements (D3), TestController, ResultsActivity, CSV Export (D4) |
| Zikhona Carroline Mahlombe | EDUV4956645 | Data Design (D3), MainActivity, Project Setup, Theme (D4) |

## Project Structure

The codebase is organized into logical packages to separate concerns:

    app/src/main/java/com/group7/renderbench/
    ├── controllers/
    │   └── TestController.java       # Test lifecycle and state management
    ├── entities/
    │   └── Item.java                 # Data model (POJO)
    ├── generators/
    │   └── DataGenerator.java        # Creates the test datasets
    ├── monitoring/
    │   └── PerformanceMonitor.java   # Captures native Android metrics
    ├── ListAdapter.java              # RecyclerView adapter
    ├── LinearLoadingFragment.java    # ScrollView engine
    ├── RecyclerViewFragment.java     # RecyclerView engine
    ├── AutoScroller.java             # Automated scrolling logic
    ├── TestConfig.java               # Shared constants
    ├── MainActivity.java             # Setup screen
    ├── TestActivity.java             # Test run host screen
    ├── ResultsActivity.java          # Single-run results
    ├── ResultHistoryActivity.java    # Results dashboard/history
    └── ResultsStore.java             # Local JSON persistence

## Branching Strategy

- `main` – Stable branch. All tested code merges here via pull requests.
- `feature/...` – Individual feature branches for each developer.

## References

- ITDMA3-22 Group 7 (2026) *Research Proposal*
- ITMDA3-34 Group 7 (2026) *Deliverables 1-4*
- Android Developers (2026) *API Reference for Runtime, FrameMetrics, BatteryManager, and Debug*.

## Module Information

- **Module**: ITMDA3-34
- **Faculty**: Information Technology
- **Campus**: Eduvos Online
- **Lecturer**: Marcus Lamola
- **Group**: 7
