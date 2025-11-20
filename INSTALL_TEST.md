# 安装测试步骤

## 1. 完全卸载旧版本

1. 打开 GoLand
2. `Settings → Plugins → Installed`
3. 找到 `pprofview`，点击右侧的下拉菜单
4. 选择 `Uninstall`
5. **完全退出 GoLand** (不是重启,是完全关闭)

## 2. 清理缓存(可选但推荐)

```bash
# 删除插件缓存
rm -rf ~/Library/Caches/JetBrains/GoLand2025.2/plugins/pprofview*

# 删除插件配置
rm -rf ~/Library/Application\ Support/JetBrains/GoLand2025.2/plugins/pprofview*
```

## 3. 安装新版本

1. 重新打开 GoLand
2. `Settings → Plugins → ⚙️ → Install Plugin from Disk...`
3. 选择: `build/distributions/pprofview-0.1.0.zip`
4. 点击 `OK`
5. 点击 `Restart IDE` 按钮

## 4. 验证安装

重启后:

1. `Settings → Plugins → Installed`
2. 确认 `pprofview (0.1.0)` 已安装且已启用(复选框已勾选)

## 5. 测试配置显示

### 方法 1: 创建新的 Go 运行配置

1. 打开一个 Go 项目
2. `Run → Edit Configurations...`
3. 点击 `+` 按钮
4. 选择 `Go Build` 或 `Go Application`
5. 填写基本配置(名称、文件等)
6. 在配置编辑器中查找 pprof 相关的选项

**期望结果**: 应该能看到一个 "pprof" 或 "性能分析" 的标签页或区域

### 方法 2: 编辑现有配置

1. `Run → Edit Configurations...`
2. 选择一个现有的 Go 运行配置
3. 查看配置编辑器

## 6. 查看日志

如果还是看不到配置选项:

1. `Help → Show Log in Finder`
2. 打开 `idea.log`
3. 搜索以下关键词:
   - `PprofRunConfigurationExtension`
   - `isApplicableFor`
   - `createEditor`

### 期望的日志输出

```
INFO - PprofRunConfigurationExtension - isApplicableFor: com.goide.execution.GoApplicationConfiguration -> true
INFO - PprofRunConfigurationExtension - createEditor called for configuration: GoApplicationConfiguration
INFO - PprofRunConfigurationExtension - Creating PprofSettingsEditor
```

### 如果看到错误

如果日志中有错误信息,请复制完整的错误堆栈并反馈。

## 7. 启用调试日志

如果日志中没有任何 pprof 相关信息:

1. `Help → Diagnostic Tools → Debug Log Settings`
2. 添加一行: `#com.github.anniext.pprofview`
3. 点击 `OK`
4. 重新打开运行配置编辑器
5. 再次查看日志

## 常见问题

### Q: 插件显示已安装但没有任何效果

**A**: 尝试以下步骤:
1. 完全退出 GoLand
2. 删除缓存: `rm -rf ~/Library/Caches/JetBrains/GoLand2025.2`
3. 重新启动 GoLand

### Q: 日志中显示 "unresolved extension"

**A**: 这是一个警告,不是错误。插件应该仍然可以工作。

### Q: 日志中没有任何 pprof 相关信息

**A**: 这说明扩展没有被调用。可能的原因:
1. 插件没有正确加载
2. 扩展点注册有问题
3. Go 插件版本不兼容

请提供完整的日志文件以便进一步诊断。

## 反馈信息

如果测试失败,请提供:

1. **GoLand 版本**: `Help → About` 中的完整版本信息
2. **Go 插件版本**: `Settings → Plugins → Installed → Go`
3. **日志文件**: `idea.log` 中包含 "pprof" 或 "Plugin" 的所有行
4. **截图**: 
   - 插件列表截图
   - 运行配置编辑器截图
5. **运行配置类型**: 你尝试的是哪种 Go 运行配置(Go Build/Go Application/etc.)
