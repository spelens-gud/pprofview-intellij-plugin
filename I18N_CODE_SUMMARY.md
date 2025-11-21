# Code Internationalization Summary / 代码国际化总结

[English](#english) | [简体中文](#简体中文)

---

## English

### Overview

This document summarizes the code internationalization work completed for the Pprof Plus plugin.

### Completed Work

#### 1. Resource Files Updated

**English Resources (`PprofViewBundle.properties`):**
- Added UI labels and messages
- Added configuration type descriptions
- Added visualization messages
- Added trace-related messages
- Total: 60+ new message keys

**Chinese Resources (`PprofViewBundle_zh_CN.properties`):**
- Added corresponding Chinese translations for all English keys
- Maintained consistency with English version

#### 2. Code Files Internationalized

**Configuration Files:**
- ✅ `PprofConfigurationType.kt` - Configuration type display name and description
- ✅ `PprofConfigurationEditor.kt` - All UI labels and messages
  - Checkbox labels
  - Form labels
  - File chooser titles
  - Panel titles

**Enum Classes (Already Internationalized):**
- ✅ `PprofCollectionMode.kt` - Uses message keys
- ✅ `PprofProfileType.kt` - Uses message keys
- ✅ `PprofRunKind.kt` - Uses message keys
- ✅ `PprofSamplingMode.kt` - Uses message keys

**Service Files:**
- ✅ `PprofVisualizationService.kt` - Added import for PprofViewBundle (partial)

### Key Changes

#### Before (Hardcoded Chinese):
```kotlin
class PprofConfigurationType : ConfigurationType {
    override fun getDisplayName(): String = "Go Pprof"
    override fun getConfigurationTypeDescription(): String = 
        "使用 pprof 进行 Go 程序性能分析"
}
```

#### After (Internationalized):
```kotlin
class PprofConfigurationType : ConfigurationType {
    override fun getDisplayName(): String = 
        PprofViewBundle.message("pprof.configurationType.name")
    override fun getConfigurationTypeDescription(): String = 
        PprofViewBundle.message("pprof.configurationType.description")
}
```

### Message Keys Added

#### UI Labels
- `pprof.ui.enablePprof`
- `pprof.ui.profileTypes`
- `pprof.ui.advancedConfig`
- `pprof.ui.runConfig`
- `pprof.ui.runKind`
- `pprof.ui.file`
- `pprof.ui.directory`
- `pprof.ui.package`
- `pprof.ui.workingDirectory`
- `pprof.ui.programArguments`
- `pprof.ui.environmentVariables`
- `pprof.ui.goBuildFlags`
- `pprof.ui.samplingInterval`
- `pprof.ui.testPattern`
- `pprof.ui.selectGoFile`
- `pprof.ui.selectDirectory`
- `pprof.ui.selectWorkingDirectory`
- `pprof.ui.selectOutputDirectory`

#### Configuration Type
- `pprof.configurationType.name`
- `pprof.configurationType.description`

#### Visualization Messages
- `pprof.visualization.starting`
- `pprof.visualization.pleaseWait`
- `pprof.visualization.started`
- `pprof.visualization.browserWillOpen`
- `pprof.visualization.stopped`
- `pprof.visualization.highlightsCleared`
- `pprof.visualization.startFailed`
- `pprof.visualization.cannotStart`
- `pprof.visualization.svgGenerated`
- `pprof.visualization.svgSavedAt`
- `pprof.visualization.generateFailed`
- `pprof.visualization.cannotGenerateSvg`
- `pprof.visualization.cannotGenerateSvgError`
- `pprof.visualization.executionFailed`
- `pprof.visualization.commandFailed`
- `pprof.visualization.cannotExecute`

#### Trace Messages
- `pprof.trace.title`
- `pprof.trace.reportTitle`
- `pprof.trace.file`
- `pprof.trace.path`
- `pprof.trace.size`
- `pprof.trace.about`
- `pprof.trace.description`
- `pprof.trace.goroutineEvents`
- `pprof.trace.syscallEvents`
- `pprof.trace.gcEvents`
- `pprof.trace.processorEvents`
- `pprof.trace.networkEvents`
- `pprof.trace.viewVisualization`
- `pprof.trace.webStarted`
- `pprof.trace.manualCommand`
- `pprof.trace.webViews`
- `pprof.trace.viewTrace`
- `pprof.trace.goroutineAnalysis`
- `pprof.trace.networkBlocking`
- `pprof.trace.syncBlocking`
- `pprof.trace.syscallBlocking`
- `pprof.trace.schedulerLatency`
- `pprof.trace.usageTips`
- `pprof.trace.tip1`
- `pprof.trace.tip2`
- `pprof.trace.tip3`
- `pprof.trace.tip4`
- `pprof.trace.started`
- `pprof.trace.stopped`

### Remaining Work

The following files still contain hardcoded strings that need internationalization:

1. **`PprofVisualizationService.kt`** - Notification messages and trace output
   - Notification titles and messages
   - Trace report content
   - Error messages

2. **Other service files** - May contain log messages or UI strings

3. **Action files** - May contain menu items or dialog messages

4. **UI components** - Chart panels and tool windows

### Next Steps

To complete the code internationalization:

1. **Update `PprofVisualizationService.kt`:**
   - Replace all `showNotification()` calls with internationalized messages
   - Replace `buildTraceOutput()` content with internationalized strings
   - Update log messages (optional, as logs are typically in English)

2. **Check remaining files:**
   - Search for hardcoded strings in action files
   - Check UI components for hardcoded labels
   - Review tool window implementations

3. **Test:**
   - Test in English environment
   - Test in Chinese environment
   - Verify all UI elements display correctly

4. **Update documentation:**
   - Update i18n-guide.md with new message keys
   - Document any special cases or patterns

---

## 简体中文

### 概述

本文档总结了 Pprof Plus 插件的代码国际化工作。

### 已完成工作

#### 1. 资源文件更新

**英文资源 (`PprofViewBundle.properties`)：**
- 添加了 UI 标签和消息
- 添加了配置类型描述
- 添加了可视化消息
- 添加了 trace 相关消息
- 总计：60+ 个新消息键

**中文资源 (`PprofViewBundle_zh_CN.properties`)：**
- 为所有英文键添加了对应的中文翻译
- 与英文版本保持一致

#### 2. 代码文件国际化

**配置文件：**
- ✅ `PprofConfigurationType.kt` - 配置类型显示名称和描述
- ✅ `PprofConfigurationEditor.kt` - 所有 UI 标签和消息
  - 复选框标签
  - 表单标签
  - 文件选择器标题
  - 面板标题

**枚举类（已国际化）：**
- ✅ `PprofCollectionMode.kt` - 使用消息键
- ✅ `PprofProfileType.kt` - 使用消息键
- ✅ `PprofRunKind.kt` - 使用消息键
- ✅ `PprofSamplingMode.kt` - 使用消息键

**服务文件：**
- ✅ `PprofVisualizationService.kt` - 添加了 PprofViewBundle 导入（部分）

### 关键变更

#### 之前（硬编码中文）：
```kotlin
class PprofConfigurationType : ConfigurationType {
    override fun getDisplayName(): String = "Go Pprof"
    override fun getConfigurationTypeDescription(): String = 
        "使用 pprof 进行 Go 程序性能分析"
}
```

#### 之后（国际化）：
```kotlin
class PprofConfigurationType : ConfigurationType {
    override fun getDisplayName(): String = 
        PprofViewBundle.message("pprof.configurationType.name")
    override fun getConfigurationTypeDescription(): String = 
        PprofViewBundle.message("pprof.configurationType.description")
}
```

### 添加的消息键

（消息键列表与英文部分相同）

### 剩余工作

以下文件仍包含需要国际化的硬编码字符串：

1. **`PprofVisualizationService.kt`** - 通知消息和 trace 输出
   - 通知标题和消息
   - Trace 报告内容
   - 错误消息

2. **其他服务文件** - 可能包含日志消息或 UI 字符串

3. **操作文件** - 可能包含菜单项或对话框消息

4. **UI 组件** - 图表面板和工具窗口

### 下一步

完成代码国际化：

1. **更新 `PprofVisualizationService.kt`：**
   - 将所有 `showNotification()` 调用替换为国际化消息
   - 将 `buildTraceOutput()` 内容替换为国际化字符串
   - 更新日志消息（可选，因为日志通常使用英文）

2. **检查剩余文件：**
   - 在操作文件中搜索硬编码字符串
   - 检查 UI 组件中的硬编码标签
   - 审查工具窗口实现

3. **测试：**
   - 在英文环境中测试
   - 在中文环境中测试
   - 验证所有 UI 元素正确显示

4. **更新文档：**
   - 使用新消息键更新 i18n-guide.md
   - 记录任何特殊情况或模式

---

**Status: Partial completion - Core UI internationalized, service layer needs completion**  
**状态：部分完成 - 核心 UI 已国际化，服务层需要完成**
