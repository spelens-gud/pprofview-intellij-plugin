# Inlay Hints 调试指南

## 如何查看日志

1. 打开 IntelliJ IDEA 的日志窗口：
   - 菜单：Help → Show Log in Finder (macOS) / Show Log in Explorer (Windows)
   - 或者：Help → Diagnostic Tools → Debug Log Settings

2. 启用详细日志：
   - 在 Debug Log Settings 中添加：`#com.github.anniext.pprofview`
   - 这会启用插件的所有日志输出

3. 查看日志文件：
   - 日志文件位置：`~/Library/Logs/JetBrains/IntelliJIdea2024.3/idea.log` (macOS)
   - 搜索关键字：`inlay hint`、`添加 inlay`

## 预期的日志输出

当你点击函数跳转到代码时，应该看到类似的日志：

```
开始处理 5 个热点行
处理行 12: isHot=true, flat=10ms, cum=10ms
行 12 offset: start=245, end=268
准备添加 inlay hint: 行 12, offset: 268, 文本:   // flat: 10ms, cum: 10ms
✅ 成功添加 inlay hint: 行 12
处理完成: 高亮 3 行, 尝试添加 3 个 inlay hints
```

## 常见问题

### 1. Inlay hints 没有显示

可能的原因：
- IntelliJ Platform API 版本不兼容
- 编辑器设置禁用了 inlay hints
- offset 计算错误

解决方法：
- 检查日志中是否有 "成功添加 inlay hint" 消息
- 检查是否有异常信息
- 确认 IDE 版本是 2024.3+

### 2. 显示位置不正确

可能的原因：
- 使用了错误的 offset
- 应该使用 `addAfterLineEndElement` 而不是 `addInlineElement`

当前实现：
- 使用 `addAfterLineEndElement` 在行尾添加
- offset 使用 `getLineEndOffset(lineNumber)`

### 3. 颜色不正确

检查：
- `parsePerformanceValue` 方法是否正确解析了性能数据
- `getColors` 方法是否返回了正确的颜色

## 测试步骤

1. 打开一个 pprof 文件
2. 在图表中点击一个函数名
3. 观察编辑器中的代码行
4. 检查行尾是否有彩色标签显示性能数据
5. 查看日志确认 inlay hints 是否成功添加

## API 参考

使用的 IntelliJ Platform API：
- `Editor.inlayModel.addAfterLineEndElement()` - 在行尾添加 inlay
- `EditorCustomElementRenderer` - 自定义渲染器
- `Inlay.dispose()` - 清除 inlay

## 备选方案

如果 inlay hints 不工作，可以考虑：
1. 使用 `addInlineElement` 在行内添加
2. 使用 `addBlockElement` 在行下方添加
3. 只使用高亮和 tooltip（当前已实现）
