# 故障排查指南

## 问题: 在运行配置中看不到 pprof 选项

### 检查清单

#### 1. 确认插件已安装并启用

```
Settings → Plugins → Installed → 查找 "pprofview"
```

确保插件旁边的复选框已勾选。

#### 2. 确认 Go 插件版本

本插件需要 Go 插件版本 252.27397.103 或更高版本。

检查方法:
```
Settings → Plugins → Installed → 查找 "Go"
```

查看版本号是否匹配。

#### 3. 确认 GoLand 版本

本插件支持 GoLand 2025.2.4 (build 252.27397 或更高)。

检查方法:
```
Help → About
```

查看 Build 号。

#### 4. 检查运行配置类型

pprof 选项只在 Go 运行配置中显示,包括:
- Go Build
- Go Application  
- Go Test
- Go Remote

如果你使用的是其他类型的运行配置,将不会显示 pprof 选项。

#### 5. 查看 IDE 日志

1. 打开日志文件:
   ```
   Help → Show Log in Finder/Explorer
   ```

2. 搜索以下关键词:
   ```
   PprofRunConfigurationExtension
   isApplicableFor
   createEditor
   ```

3. 查找错误信息,例如:
   ```
   ERROR - PprofRunConfigurationExtension - ...
   ```

#### 6. 启用调试日志

1. 打开调试日志设置:
   ```
   Help → Diagnostic Tools → Debug Log Settings
   ```

2. 添加以下行:
   ```
   #com.github.anniext.pprofview
   ```

3. 点击 OK

4. 重启 IDE 或重新打开运行配置

5. 再次查看日志文件,应该能看到详细的调试信息:
   ```
   INFO - PprofRunConfigurationExtension - isApplicableFor: com.goide.execution.GoApplicationConfiguration -> true
   INFO - PprofRunConfigurationExtension - Creating PprofSettingsEditor
   ```

### 常见错误及解决方法

#### 错误: "Plugin 'pprofview' cannot be loaded"

**原因**: 依赖的 Go 插件未安装或版本不匹配

**解决方法**:
1. 确保 Go 插件已安装
2. 更新 Go 插件到兼容版本
3. 重启 IDE

#### 错误: "Class not found: GoRunConfigurationBase"

**原因**: Go 插件版本过旧

**解决方法**:
1. 更新 Go 插件到 252.27397.103 或更高版本
2. 重新安装 pprofview 插件

#### 日志显示: "isApplicableFor: ... -> false"

**原因**: 运行配置类型不匹配

**解决方法**:
1. 确认你打开的是 Go 运行配置
2. 尝试创建一个新的 Go Application 配置
3. 查看日志中的完整类名,确认是否为 Go 相关配置

### 手动测试

如果以上方法都无法解决问题,可以尝试在开发模式下运行插件:

```bash
./gradlew runIde
```

这会启动一个新的 GoLand 实例,其中已加载插件。在这个实例中:

1. 创建或打开一个 Go 项目
2. 创建一个 Go 运行配置
3. 查看是否显示 pprof 选项
4. 查看控制台输出的日志信息

### 获取帮助

如果问题仍未解决,请提供以下信息:

1. **GoLand 版本**:
   ```
   Help → About → 复制版本信息
   ```

2. **Go 插件版本**:
   ```
   Settings → Plugins → Installed → Go → 版本号
   ```

3. **相关日志**:
   从 `idea.log` 中复制包含 `PprofRunConfigurationExtension` 的所有行

4. **运行配置信息**:
   - 运行配置类型 (Go Build / Go Application / etc.)
   - 运行配置的 XML 内容 (可选)

5. **截图**:
   - 运行配置编辑器的截图
   - 插件列表的截图

## 已知问题

### 问题 1: 配置选项不显示

**状态**: 调查中

**临时解决方法**: 
1. 重启 IDE
2. 重新创建运行配置
3. 检查日志中的错误信息

### 问题 2: 配置保存后丢失

**状态**: 已修复 (v0.1.0)

**解决方法**: 更新到最新版本

## 反馈渠道

- GitHub Issues: https://github.com/Anniext/pprofview/issues
- 邮件: (待添加)
