# pprofview 插件调试指南

## 问题诊断

如果在 GoLand 中看不到 pprof 配置选项,请按以下步骤排查:

### 1. 检查插件是否正确安装

1. 打开 GoLand 2025.2.4
2. 进入 `Settings/Preferences → Plugins`
3. 在 `Installed` 标签页中查找 `pprofview` 插件
4. 确保插件已启用（复选框已勾选）

### 2. 检查日志

1. 打开 `Help → Show Log in Finder/Explorer`
2. 查看 `idea.log` 文件
3. 搜索关键词:
   - `PprofRunConfigurationExtension`
   - `isApplicableFor`
   - `createEditor`
4. 查看是否有错误或警告信息

### 3. 验证 Go 运行配置

1. 创建或打开一个 Go 项目
2. 创建一个 Go 运行配置:
   - `Run → Edit Configurations...`
   - 点击 `+` → `Go Build` 或 `Go Application`
3. 在运行配置编辑器中,应该能看到一个 `pprof` 或类似的标签页

### 4. 查看配置类型

在日志中查找类似这样的信息:
```
isApplicableFor: com.goide.execution.GoApplicationConfiguration -> true
```

如果看到 `false`,说明配置类型不匹配。

### 5. 手动测试

运行以下命令启动带插件的 IDE 实例进行测试:

```bash
./gradlew runIde
```

这会启动一个新的 GoLand 实例,其中已安装了你的插件。

## 常见问题

### Q: 看不到 pprof 配置选项

**可能原因:**
1. 插件未正确加载
2. Go 插件版本不兼容
3. 运行配置类型不匹配

**解决方法:**
1. 检查 `idea.log` 中的错误信息
2. 确认 Go 插件版本为 252.27397.103
3. 尝试重启 IDE

### Q: 插件安装后 IDE 无法启动

**可能原因:**
1. 插件与 IDE 版本不兼容
2. 依赖的 Go 插件版本不匹配

**解决方法:**
1. 在安全模式下启动 IDE
2. 禁用 pprofview 插件
3. 检查 `gradle.properties` 中的版本配置

### Q: 配置选项显示但不生效

**可能原因:**
1. 配置未正确保存
2. 命令行参数未正确应用

**解决方法:**
1. 检查运行配置的 XML 文件
2. 查看日志中的 `patchCommandLine` 调用
3. 验证环境变量是否正确设置

## 调试技巧

### 启用详细日志

在 `Help → Diagnostic Tools → Debug Log Settings` 中添加:

```
#com.github.anniext.pprofview
```

这会启用插件的详细日志输出。

### 检查扩展点注册

在 IDE 中打开 `Tools → Internal Actions → Registry`，搜索 `runConfigurationExtension`，查看是否有我们的扩展。

### 验证类加载

在日志中搜索:
```
PprofRunConfigurationExtension
```

如果找不到,说明类未被加载。

## 重新构建插件

如果修改了代码,重新构建插件:

```bash
./gradlew clean buildPlugin
```

插件文件位于: `build/distributions/pprofview-0.1.0.zip`

## 联系支持

如果问题仍然存在,请提供:
1. GoLand 版本号
2. `idea.log` 相关日志
3. 运行配置的 XML 内容
4. 插件安装截图
