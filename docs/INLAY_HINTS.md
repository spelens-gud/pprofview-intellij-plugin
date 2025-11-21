# pprof 性能数据内嵌显示

## 功能说明

当从 pprof 图表跳转到代码时，编辑器会在代码行尾内嵌显示性能分析数据，包括：

- **flat**: 函数自身执行时间
- **cum**: 函数及其调用的所有函数的总时间

## 显示效果

性能数据会以带颜色的标签形式显示在代码行尾：

```go
func fibonacci(n int) int {
    if n <= 1 {
        return n  // flat: 10ms, cum: 10ms  ← 这里会显示彩色标签
    }
    return fibonacci(n-1) + fibonacci(n-2)
}
```

标签特点：
- 圆角背景和边框
- 斜体字体
- 根据性能数据自动选择颜色

## 颜色标识

性能数据会根据热点强度使用不同颜色显示：

- 🔴 **红色**: 高热点 (值 >= 100)
- 🟠 **橙色**: 中热点 (值 >= 10)
- 🟡 **黄色**: 低热点 (值 > 0)
- ⚪ **灰色**: 正常

## 实现细节

### 核心组件

1. **PprofInlayRenderer**: 自定义渲染器，负责在代码行尾绘制性能数据
2. **PprofCodeNavigationService**: 导航服务，负责添加和管理 inlay hints
3. **PprofInlayHintsProvider**: Inlay hints 提供者（预留扩展）

### 技术特点

- 使用 IntelliJ Platform 的 Inlay API
- 圆角背景和边框，美观易读
- 根据性能数据自动选择颜色
- 支持浅色和深色主题
- 自动清理旧的 inlay hints

## 使用方式

1. 打开 pprof 文件
2. 在图表中点击函数名
3. 自动跳转到代码并显示性能数据
4. 性能数据以注释形式显示在代码行尾

## 注意事项

- Inlay hints 仅在跳转时显示，不会持久化
- 切换到其他文件或再次跳转时，旧的 hints 会自动清除
- 只显示有性能数据的代码行（flat 或 cum 不为 "."）
