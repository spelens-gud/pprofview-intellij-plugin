# Pprof Plus Changelog

All notable changes to this project will be documented in this file.

## [1.0.0] - 2025-11-21

### Initial Release

Pprof Plus 1.0.0 is officially released! This is a fully-featured Go performance analysis plugin that provides powerful pprof visualization and analysis capabilities for GoLand and IntelliJ IDEA users.

### Core Features

#### go tool pprof Visualization Integration
  - Context menu actions: Visualize pprof files directly
  - Support for 7 visualization types:
    - Web Browser (Interactive UI)
    - Text Report
    - Call Graph SVG
    - Flame Graph SVG
    - Top Functions
    - Function List
    - Brief Info
  - pprof Output tool window: Display text output results
  - Auto-open feature: Automatically opens visualization after run configuration completes
  - Notification alerts: Real-time feedback on operation status

#### Pprof Run Configuration
  - Support for three run kinds: File, Directory, Package
  - Responsive smart auto-fill:
    - File mode: Automatically finds main.go or files containing main function
    - Directory mode: Automatically uses working directory
    - Package mode: Automatically reads go.mod and scans all sub-packages
  - Dynamic updates: Automatically updates options when switching run kind or changing working directory
  - Support for multiple collection modes: Runtime Sampling, HTTP Service, Manual Collection, Compile-time Instrumentation
  - Support for multiple profile types: CPU, Heap, Goroutine, Block, Mutex, Allocs
  - Configurable working directory, program arguments, environment variables, Go build flags
  - Automatically sets pprof-related environment variables
  - Configurable output directory and sampling parameters
  - Support for test mode: Can profile Go test files

#### Code Navigation Features
  - Click function names in charts to jump directly to source code locations
  - Smart function name matching: Supports various Go function name formats
    - Package path functions: `github.com/user/repo/pkg.FuncName`
    - Method calls: `(*Type).Method` and `Type.Method`
    - Generic functions: `Func[T]` and `Func[T1, T2]`
    - Closure functions: `Func.func1`, `Func.func2`, etc.
  - Support for click-to-jump in tables, bar charts, pie charts, and heatmaps
  - Function names highlighted as clickable link style
  - Detailed debug logs and error messages

#### Inlay Hints Performance Tips
  - Display function performance data in source code
  - Real-time display of CPU usage, memory consumption, and other metrics
  - Visual performance hotspot markers

#### Enhanced Chart Visualization
  - Four chart types: Detailed data table, bar chart, pie chart, heatmap
  - Interactive hover tooltips: Display detailed performance data
  - Tab refresh functionality: Can reload chart data
  - Responsive design: Supports narrow window adaptive layout
  - Streamlined visual style: Removes excessive decoration, focuses on data presentation

#### Example Code and Documentation
  - HTTP Service mode example
  - Runtime Sampling mode example (runtime_sampling_example.go)
  - Manual Collection mode example
  - Test mode example (test_sampling_example_test.go)
  - Compile-time Instrumentation mode description
  - Detailed usage guides and documentation
  - Complete example code covering all collection modes

### System Requirements

- GoLand 2025.2+ or IntelliJ IDEA 2025.2+ (with Go plugin)
- Go 1.16+
- JVM 21+

### Supported Platforms

- Windows
- macOS
- Linux

---

## [Unreleased]

### Planned Features

- More visualization type support
- Performance comparison functionality
- History management
- Export report functionality
