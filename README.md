# RenderBench

## Optimizing Mobile Performance: List Virtualization VS Linear Loading in Android Applications

## Project Overview

RenderBench is an Android performance testing application that investigates the performance impact of two list-rendering approaches in native Android applications:

- **Linear Loading**: ScrollView with LinearLayout – all item views are inflated and held in memory at once.
- **List Virtualization**: RecyclerView – only visible views are maintained and recycled as the user scrolls.

The goal is to measure and compare CPU usage, RAM consumption, frame rate, and battery drain across both approaches using different dataset sizes.

## Research Context

This project is part of the ITMDA3-34 module at Eduvos. It builds on the research proposal and literature review submitted in previous deliverables (ITDMA3-22 and ITMDA3-34).

The findings will provide practical recommendations for Android developers, especially those working with resource-constrained devices common in South Africa.

## Current Status

### Completed
- Research proposal and literature review (D1, D2)
- System architecture design (D3)
- Wireframes and use case diagrams (D3)
- Data model design (D3)

### In Progress (D4: Implementation)
- Android application development
- Core classes: Item, DataGenerator, PerformanceMonitor
- UI screens: MainActivity, TestActivity, ResultsActivity
- Performance monitoring using Android Debug Bridge (ADB)

## Components

| Component | Description |
|-----------|-------------|
| Item | Data model representing a single list row (id, title, description, imageUrl) |
| DataGenerator | Creates test datasets of sizes: 50, 100, 500, 1,000, 5,000, 10,000 |
| PerformanceMonitor | Captures CPU, RAM, frame rate, and battery metrics during test runs |
| TestController | Manages the test flow and coordinates all components |
| MainActivity | Setup screen for selecting dataset size and rendering engine |
| TestActivity | Renders the list using ScrollView or RecyclerView |
| ResultsActivity | Displays performance metrics and exports results to CSV |

## Technologies Used

| Component | Technology |
|-----------|------------|
| Language | Java |
| UI Framework | Android XML Layouts |
| Linear Loading | ScrollView + LinearLayout |
| List Virtualization | RecyclerView |
| Performance Monitoring | Android Studio Profiler + ADB |
| Automated Testing | UI Automator |
| Version Control | Git + GitHub |

## Team Members

| Name | Student Number | Role |
|------|---------------|------|
| Ammaarah Jacobs | EDUV4930538 | Group Leader, User Stories, Use Case Diagram |
| Kamogelo Phale | EDUV4810351 | DataGenerator, PerformanceMonitor, Architecture Design |
| Ruan de Wit | EDUV4863263 | TestActivity (ScrollView) |
| Troy James Bower | EDUV4865433 | TestController, ResultsActivity |
| Zikhona Carroline Mahlombe | EDUV4956645 | MainActivity, Project Setup |

## Project Structure

```
app/
├── src/main/
│ ├── java/com/group7/renderbench/
│ │ ├── MainActivity.java
│ │ ├── TestActivity.java
│ │ ├── ResultsActivity.java
│ │ ├── Item.java
│ │ ├── DataGenerator.java
│ │ ├── PerformanceMonitor.java
│ │ └── TestController.java
│ ├── res/
│ │ ├── layout/
│ │ │ ├── activity_main.xml
│ │ │ ├── activity_test.xml
│ │ │ └── activity_results.xml
│ │ └── values/
│ └── AndroidManifest.xml
├── build.gradle.kts
└── gradle/
```

## Branching Strategy

- `main` – stable branch, all tested code merges here
- `feature/...` – individual feature branches for each developer

## References

- ITDMA3-22 Group 7 (2026) Research Proposal
- ITMDA3-34 Group 7 (2026) Deliverables 1-3
- Android Developers documentation
- Academic sources cited in the literature review

## Module Information

- **Module**: ITMDA3-34
- **Faculty**: Information Technology
- **Campus**: Eduvos Online
- **Lecturer**: Marcus Lamola
- **Group**: 7
