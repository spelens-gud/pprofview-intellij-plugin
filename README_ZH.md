# Pprof Plus

一个强大的 Go 性能分析插件。Pprof Plus 将 `go tool pprof` 无缝集成到 GoLand 和 IntelliJ IDEA 中，为 Go 开发者提供全面的性能分析可视化能力。

**核心特性：**
- 🎯 智能运行配置，支持多种采集模式（运行时、HTTP、手动、插桩）
- 📊 7 种可视化类型，包括交互式 Web UI、火焰图和调用图
- 🔍 代码导航，支持性能数据内嵌显示和热力图可视化
- 🛠️ 集成工具窗口，实时性能分析
- 🚀 一键分析 CPU、内存、协程、阻塞和互斥锁性能

[English](README.md) | 简体中文

## ✨ 核心功能

### 🎯 运行配置
- **智能运行配置**：支持文件、目录、软件包三种运行模式
- **响应式填充**：根据项目结构自动填充配置选项
- **多种采集模式**：
  - 运行时采样（Runtime Sampling）
  - HTTP 服务（HTTP Server）
  - 手动采集（Manual Collection）
  - 编译时插桩（Compile-time Instrumentation）
- **多种性能分析类型**：CPU、堆内存、协程、阻塞、互斥锁、内存分配
- **灵活配置**：工作目录、程序参数、环境变量、Go 构建标志

### 📊 可视化分析
- **7 种可视化类型**：
  - 🌐 Web 浏览器（交互式界面）
  - 📝 文本报告
  - 📋 函数列表
  - ℹ️ 简要信息
- **右键菜单集成**：直接对 pprof 文件进行可视化
- **自动打开**：运行配置完成后自动打开可视化结果
- **实时通知**：操作状态实时反馈

### 🔍 代码导航
- **性能数据内嵌显示**：在代码行尾显示 flat 和 cum 数据
- **热力图**：使用矩形树图展示 Top 20 函数的性能分布
- **智能跳转**：从图表直接跳转到源代码
- **颜色标识**：根据热点强度自动选择颜色

### 🛠️ 工具窗口
- **pprof Output**：显示文本输出结果
- **集成终端**：查看详细的性能分析数据

## 📦 安装

### 从 JetBrains Marketplace 安装
1. 打开 GoLand/IntelliJ IDEA
2. 进入 `Settings/Preferences` → `Plugins` → `Marketplace`
3. 搜索 "Pprof Plus"
4. 点击 `Install`

### 手动安装
1. 从 [Releases](https://github.com/spelens-gud/pprofview-intellij-plugin/releases) 下载最新版本
2. 进入 `Settings/Preferences` → `Plugins` → `⚙️` → `Install Plugin from Disk...`
3. 选择下载的 ZIP 文件

## 🚀 快速开始

### 1. 创建运行配置
1. 点击 `Run` → `Edit Configurations...`
2. 点击 `+` → `pprof`
3. 配置运行参数：
   - 选择运行种类（文件/目录/软件包）
   - 选择采集模式
   - 选择性能分析类型
   - 配置采样参数

### 2. 运行性能分析
1. 点击运行按钮
2. 等待程序执行和数据采集
3. 自动打开可视化结果

### 3. 可视化分析
- **右键菜单**：在项目视图或编辑器中右键点击 pprof 文件 → `使用 go tool pprof 可视化`
- **选择可视化类型**：Web、文本、调用图、火焰图等
- **查看结果**：在浏览器或工具窗口中查看

### 4. 代码导航
- 在图表中点击函数名
- 自动跳转到源代码
- 查看性能数据内嵌显示

## 📖 使用示例

### 运行时采样模式
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

### HTTP 服务模式
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

## 🎨 功能特性

### 性能数据内嵌显示
- 在代码行尾显示 flat 和 cum 数据
- 根据热点强度使用不同颜色
- 支持浅色和深色主题

### 热力图
- 矩形面积代表性能占比
- 颜色深浅代表热点程度
- 显示 Top 20 函数
- 支持悬停显示详细信息
- 点击跳转到代码

## 🔧 系统要求

- **IDE**: GoLand 2025.2+ 或 IntelliJ IDEA 2025.2+ (带 Go 插件)
- **Go**: 1.16+
- **JVM**: 21+
- **操作系统**: Windows, macOS, Linux

## 📝 开发

### 构建项目
```bash
./gradlew buildPlugin
```

### 运行测试
```bash
./gradlew test
```

### 运行 IDE
```bash
./gradlew runIde
```

## 🤝 贡献

欢迎贡献！请查看 [贡献指南](CONTRIBUTING.md)。

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🔗 相关链接

- [GitHub 仓库](https://github.com/spelens-gud/pprofview-intellij-plugin)
- [问题反馈](https://github.com/spelens-gud/pprofview-intellij-plugin/issues)
- [更新日志](CHANGELOG.md)
- [Go pprof 文档](https://pkg.go.dev/runtime/pprof)

## 📧 联系方式

如有问题或建议，请通过 [GitHub Issues](https://github.com/spelens-gud/pprofview-intellij-plugin/issues) 联系我们。

---

**Made with ❤️ for Go developers**
