# pprofview

![Build](https://github.com/Anniext/pprofview/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

<!-- Plugin description -->
一个用于在 JetBrains IDE 中可视化 pprof 性能分析数据的插件。

支持解析和展示 Go 语言 pprof 格式的性能分析文件,包括 CPU、内存、goroutine 等性能数据的可视化展示。

主要功能:
- 解析 pprof 格式文件 (protobuf 和文本格式)
- 火焰图可视化展示
- 调用图展示
- 性能数据统计分析
- 支持多种性能分析类型 (CPU、Heap、Goroutine 等)
- Go 运行配置集成，支持多种 pprof 数据采集方式
<!-- Plugin description end -->

## 安装

- 使用 IDE 内置插件系统:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>搜索 "pprofview"</kbd> >
  <kbd>Install</kbd>
  
- 手动安装:

  从 [最新版本](https://github.com/Anniext/pprofview/releases/latest) 下载插件并手动安装:
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## 使用

### 查看 pprof 文件

1. 在项目中右键点击 pprof 文件 (`.pb.gz`, `.pprof` 等格式)
2. 选择 "Open with pprofview" 打开可视化界面
3. 查看火焰图、调用图等性能分析数据

### 在运行配置中启用 pprof

插件为 Go 运行配置添加了 pprof 支持，可以在程序运行时自动采集性能数据。

#### 配置步骤

1. 打开 Go 运行配置 (Run > Edit Configurations...)
2. 在配置页面中找到 "pprof 性能分析" 选项卡
3. 勾选 "启用 pprof 性能分析"
4. 选择采集模式和性能分析类型

#### 采集模式

**1. 编译时插桩**
- 在编译时插入性能分析代码
- 适用场景：竞态检测、代码覆盖率分析
- 配置示例：
  - 自定义编译参数：`-race` (竞态检测)
  - 自定义编译参数：`-cover` (代码覆盖率)

**2. 运行时采样**
- 程序运行时自动采样性能数据
- 适用场景：CPU、内存、协程等常规性能分析
- 配置选项：
  - CPU 采样持续时间：默认 30 秒
  - 内存采样率：默认 512KB
  - 支持的分析类型：CPU、堆内存、协程、线程创建、阻塞、互斥锁等

**3. 手动采集**
- 在代码中手动调用 pprof API 控制采集
- 适用场景：需要精确控制采集时机和范围
- 示例代码：参见 `src/main/resources/examples/pprof_example.go`

**4. HTTP 服务**
- 启动 pprof HTTP 服务器，提供实时性能数据访问
- 适用场景：长期运行的服务、实时监控
- 默认端口：6060
- 访问地址：`http://localhost:6060/debug/pprof/`

#### 性能分析类型

- **CPU 分析**：分析 CPU 使用情况，找出热点函数
- **堆内存分析**：分析内存分配情况，发现内存泄漏
- **协程分析**：查看所有协程的状态和调用栈
- **线程创建分析**：分析线程创建情况
- **阻塞分析**：分析阻塞操作（channel、锁等）
- **互斥锁分析**：分析锁竞争情况
- **内存分配分析**：分析所有内存分配（包括已释放的）
- **执行追踪**：记录程序执行的详细追踪信息

#### 输出配置

- **输出目录**：指定 pprof 文件保存位置（默认为系统临时目录）
- **自动打开结果**：程序结束后自动在 IDE 中打开生成的 pprof 文件

#### 使用示例

1. 创建或编辑 Go 运行配置
2. 启用 pprof，选择 "运行时采样" 模式
3. 勾选 "CPU 分析" 和 "堆内存分析"
4. 设置输出目录为项目的 `pprof` 文件夹
5. 运行程序
6. 程序结束后，IDE 会自动打开生成的 `cpu.pprof` 和 `heap.pprof` 文件

## 开发

本项目基于 [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template) 构建。

### 构建

```bash
./gradlew buildPlugin
```

### 运行

```bash
./gradlew runIde
```

### 测试

```bash
./gradlew test
```
