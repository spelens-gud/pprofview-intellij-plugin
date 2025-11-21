# Pprof Plus: Visual Analytics

<!-- Plugin description -->
<h2>🚀 Pprof Plus - Go Performance Analysis Made Easy</h2>

<p>
A powerful performance analysis plugin that brings <code>go tool pprof</code> directly into your IDE.
Visualize, analyze, and optimize your Go applications without leaving GoLand or IntelliJ IDEA.
</p>

<h3>✨ Key Features</h3>
<ul>
<li><strong>🎯 Smart Run Configurations</strong> - One-click profiling with multiple collection modes (Runtime, HTTP, Manual, Instrumentation)</li>
<li><strong>📊 Rich Visualizations</strong> - 7 visualization types including interactive web UI, flame graphs, call graphs, and more</li>
<li><strong>🔍 Code Navigation</strong> - Jump from performance data to source code with inlay hints and heatmap visualization</li>
<li><strong>🛠️ Integrated Tool Windows</strong> - View analysis results directly in your IDE</li>
<li><strong>⚡ Multiple Profile Types</strong> - CPU, Memory, Goroutine, Block, Mutex, and Allocs profiling</li>
</ul>

<h3>🎨 Highlights</h3>
<ul>
<li><strong>Inlay Hints</strong> - Display performance metrics (flat/cum) directly in your code editor</li>
<li><strong>Heatmap</strong> - Visual treemap of Top 20 functions with color-coded hotspots</li>
<li><strong>Context Menu</strong> - Right-click any pprof file to visualize instantly</li>
<li><strong>Auto-open Results</strong> - Visualization opens automatically after profiling completes</li>
</ul>

<h3>🚀 Quick Start</h3>
<ol>
<li>Create a new <strong>pprof</strong> run configuration</li>
<li>Select your collection mode and profile type</li>
<li>Click Run and watch the magic happen</li>
<li>Explore interactive visualizations and jump to code</li>
</ol>

<h3>📋 Requirements</h3>
<ul>
<li>GoLand 2025.2+ or IntelliJ IDEA 2025.2+ (with Go plugin)</li>
<li>Go 1.16+</li>
<li>JVM 21+</li>
</ul>

<p><strong>Made with ❤️ for Go developers</strong></p>

<p>
<a href="https://github.com/spelens-gud/pprofview-intellij-plugin">GitHub</a> |
<a href="https://github.com/spelens-gud/pprofview-intellij-plugin/issues">Report Issues</a> |
<a href="https://github.com/spelens-gud/pprofview-intellij-plugin/blob/main/CHANGELOG.md">Changelog</a>
</p>
<!-- Plugin description end -->

English | [简体中文](README_ZH.md)

## ✨ Key Features

### 🎯 Run Configuration
- **Smart Run Configuration**: Supports file, directory, and package run modes
- **Responsive Auto-fill**: Automatically fills configuration options based on project structure
- **Multiple Collection Modes**:
  - Runtime Sampling
  - HTTP Server
  - Manual Collection
  - Compile-time Instrumentation
- **Multiple Profile Types**: CPU, Heap, Goroutine, Block, Mutex, Allocs
- **Flexible Configuration**: Working directory, program arguments, environment variables, Go build flags

### 📊 Visualization
- **7 Visualization Types**:
  - 🌐 Web Browser (Interactive UI)
  - 📝 Text Report
  - 📋 Function List
  - ℹ️ Brief Info
- **Context Menu Integration**: Visualize pprof files directly from context menu
- **Auto-open**: Automatically opens visualization results after run configuration completes
- **Real-time Notifications**: Instant feedback on operation status

### 🔍 Code Navigation
- **Inlay Hints**: Display flat and cum data at the end of code lines
- **Heatmap**: Treemap visualization of Top 20 functions' performance distribution
- **Smart Jump**: Jump directly from charts to source code
- **Color Coding**: Automatically selects colors based on hotspot intensity

### 🛠️ Tool Windows
- **pprof Output**: Displays text output results
- **Integrated Terminal**: View detailed performance analysis data

## 📦 Installation

### From JetBrains Marketplace
1. Open GoLand/IntelliJ IDEA
2. Go to `Settings/Preferences` → `Plugins` → `Marketplace`
3. Search for "pprofview"
4. Click `Install`

### Manual Installation
1. Download the latest release from [Releases](https://github.com/spelens-gud/pprofview-intellij-plugin/releases)
2. Go to `Settings/Preferences` → `Plugins` → `⚙️` → `Install Plugin from Disk...`
3. Select the downloaded ZIP file

## 🚀 Quick Start

### 1. Create Run Configuration
1. Click `Run` → `Edit Configurations...`
2. Click `+` → `pprof`
3. Configure run parameters:
   - Select run kind (File/Directory/Package)
   - Select collection mode
   - Select profile type
   - Configure sampling parameters

### 2. Run Performance Analysis
1. Click the run button
2. Wait for program execution and data collection
3. Visualization results open automatically

### 3. Visualization Analysis
- **Context Menu**: Right-click on pprof file in project view or editor → `Visualize with go tool pprof`
- **Select Visualization Type**: Web, Text, Call Graph, Flame Graph, etc.
- **View Results**: View in browser or tool window

### 4. Code Navigation
- Click on function name in chart
- Automatically jump to source code
- View inlay hints with performance data

## 📖 Usage Examples

### Runtime Sampling Mode
```go
package main

import (
    "fmt"
    "runtime/pprof"
    "os"
)

func main() {
    // CPU profiling
    f, _ := os.Create("cpu.pprof")
    pprof.StartCPUProfile(f)
    defer pprof.StopCPUProfile()
    
    // Your code here
    fibonacci(40)
}

func fibonacci(n int) int {
    if n <= 1 {
        return n
    }
    return fibonacci(n-1) + fibonacci(n-2)
}
```

### HTTP Server Mode
```go
package main

import (
    _ "net/http/pprof"
    "net/http"
)

func main() {
    go func() {
        http.ListenAndServe("localhost:6060", nil)
    }()
    
    // Your application code
}
```

## 🎨 Feature Highlights

### Inlay Hints
- Display flat and cum data at the end of code lines
- Use different colors based on hotspot intensity
- Support for light and dark themes

### Heatmap
- Rectangle area represents performance ratio
- Color intensity represents hotspot level
- Display Top 20 functions
- Hover to show detailed information
- Click to jump to code

## 🔧 Requirements

- **IDE**: GoLand 2025.2+ or IntelliJ IDEA 2025.2+ (with Go plugin)
- **Go**: 1.16+
- **JVM**: 21+
- **OS**: Windows, macOS, Linux

## 📝 Development

### Build Plugin
```bash
./gradlew buildPlugin
```

### Run Tests
```bash
./gradlew test
```

### Run IDE
```bash
./gradlew runIde
```

## 🤝 Contributing

Contributions are welcome! Please check the [Contributing Guide](CONTRIBUTING_EN.md).

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🔗 Links

- [GitHub Repository](https://github.com/spelens-gud/pprofview-intellij-plugin)
- [Issue Tracker](https://github.com/spelens-gud/pprofview-intellij-plugin/issues)
- [Changelog](CHANGELOG_EN.md)
- [Go pprof Documentation](https://pkg.go.dev/runtime/pprof)

## 📧 Contact

For questions or suggestions, please contact us through [GitHub Issues](https://github.com/spelens-gud/pprofview-intellij-plugin/issues).

---

**Made with ❤️ for Go developers**
