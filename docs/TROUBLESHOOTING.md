# 故障排查指南

## 点击函数名没有反应

### 可能的原因

1. **项目或文件信息缺失**
2. **pprof 命令执行失败**
3. **文件路径解析失败**
4. **源文件不在项目中**

### 排查步骤

#### 1. 检查控制台输出

点击函数名后，查看 IDE 控制台（View > Tool Windows > Run）是否有输出：

```
========================================
用户点击函数: github.com/wolfogre/go-pprof-practice/animal/canidae/wolf.(*Wolf).Drink.func1
时间: 2024-11-20T21:30:45.123
项目: myproject
pprof 文件: /path/to/profile.pprof
========================================
```

如果没有任何输出，说明点击事件没有触发，可能是：
- 表格没有正确初始化
- project 或 pprofFile 为 null

#### 2. 检查通知

IDE 右下角应该会显示通知，说明具体的错误原因：

- **"项目或 pprof 文件信息缺失"**：需要重新打开 pprof 文件
- **"无法获取函数的源代码信息"**：pprof 命令执行失败
- **"无法解析函数的代码位置"**：pprof 输出格式不正确
- **"文件未找到"**：源文件不在项目中

#### 3. 查看详细日志

打开 IDE 日志文件：

**macOS**:
```bash
tail -f ~/Library/Logs/JetBrains/IntelliJIdea2024.3/idea.log
```

**Linux**:
```bash
tail -f ~/.cache/JetBrains/IntelliJIdea2024.3/log/idea.log
```

**Windows**:
```powershell
Get-Content $env:USERPROFILE\AppData\Local\JetBrains\IntelliJIdea2024.3\log\idea.log -Wait -Tail 50
```

查找关键信息：

```
开始导航到函数: github.com/wolfogre/go-pprof-practice/animal/canidae/wolf.(*Wolf).Drink.func1
执行命令: go tool pprof -list=github.com/wolfogre/go-pprof-practice/animal/canidae/wolf.(*Wolf).Drink.func1 /path/to/profile.pprof
进程退出码: 0
获取到输出长度: 1234 字符
```

#### 4. 手动测试 pprof 命令

在终端中手动执行 pprof 命令，查看输出：

```bash
# 方法 1: 使用完整函数名（可能失败）
go tool pprof -list='github.com/wolfogre/go-pprof-practice/animal/canidae/wolf.(*Wolf).Drink.func1' /path/to/profile.pprof

# 方法 2: 使用简化的函数名（推荐）
go tool pprof -list='Drink' /path/to/profile.pprof

# 方法 3: 使用正则表达式
go tool pprof -list='Wolf.*Drink' /path/to/profile.pprof

# 方法 4: 查看所有函数，找到正确的名称
go tool pprof -top /path/to/profile.pprof
```

**使用测试脚本**：

项目提供了测试脚本来自动尝试多种模式：

```bash
./test_function_patterns.sh /path/to/profile.pprof 'github.com/wolfogre/go-pprof-practice/animal/canidae/wolf.(*Wolf).Drink.func1'
```

**预期输出**：
```
Total: 10.50s
ROUTINE ======================== github.com/wolfogre/go-pprof-practice/animal/canidae/wolf.(*Wolf).Drink.func1 in /path/to/wolf.go
      10ms      10ms (flat, cum)  0.10% of Total
         .          .     45:func (w *Wolf) Drink() {
         .          .     46:    go func() {
      10ms       10ms     47:        fmt.Println("wolf drink")
         .          .     48:    }()
         .          .     49:}
```

**常见问题**：

- **命令不存在**：需要安装 Go 工具链
- **文件不存在**：pprof 文件路径错误
- **函数名不匹配**：函数名拼写错误或格式不正确
- **无输出**：该函数在 pprof 中没有数据

#### 5. 检查函数名格式

pprof 中的函数名可能包含特殊字符，需要正确转义：

**正确的格式**：
- `main.main`
- `github.com/user/project/pkg.Function`
- `github.com/user/project/pkg.(*Type).Method`
- `github.com/user/project/pkg.(*Type).Method.func1` (匿名函数)

**注意**：
- 包名使用完整路径
- 方法接收者使用 `(*Type)` 格式
- 匿名函数使用 `.func1`, `.func2` 等后缀

#### 6. 验证源文件存在

确认源文件在项目中：

```bash
# 在项目根目录执行
find . -name "wolf.go"
```

如果文件不存在，需要：
1. 克隆或下载源代码到项目中
2. 确保项目结构正确
3. 刷新项目（File > Invalidate Caches / Restart）

## 常见错误及解决方案

### 错误 1: "项目或 pprof 文件信息缺失"

**原因**：表格创建时没有传入 project 或 pprofFile 参数。

**解决方案**：
1. 关闭当前的 pprof Output 工具窗口
2. 重新右键点击 pprof 文件
3. 选择"使用 go tool pprof 可视化"

### 错误 2: "无法执行 go tool pprof 命令"

**原因**：Go 工具链未安装或不在 PATH 中。

**解决方案**：
```bash
# 检查 Go 是否安装
go version

# 检查 pprof 工具
go tool pprof -h
```

如果未安装，访问 https://golang.org/dl/ 下载安装。

### 错误 3: "无法解析函数的代码位置"

**原因**：pprof 输出格式不符合预期。

**解决方案**：
1. 查看日志中的 pprof 输出内容
2. 检查是否是文本格式的 pprof 文件
3. 尝试使用 `go tool pprof -text` 重新生成报告

### 错误 4: "文件未找到"

**原因**：源文件不在项目中，或路径解析失败。

**解决方案**：

1. **检查文件是否存在**：
   ```bash
   ls -la /path/to/source/file.go
   ```

2. **检查项目结构**：
   确保项目包含源代码，而不仅仅是 pprof 文件。

3. **查看路径解析日志**：
   ```
   开始查找源文件: /Users/user/go/src/github.com/wolfogre/go-pprof-practice/animal/canidae/wolf/wolf.go
     - 策略 1 失败: 绝对路径不存在
     - 策略 2 失败: 项目根 + 路径不存在
     - 检测到 GOPATH 结构，提取包路径: github.com/wolfogre/go-pprof-practice/animal/canidae/wolf/wolf.go
     - 提取文件名: wolf.go
     - 找到 1 个同名文件
     - 策略 5 成功: 唯一匹配
   ```

4. **手动验证路径**：
   如果日志显示找到了文件，但仍然无法打开，可能是权限问题：
   ```bash
   ls -la /path/to/found/file.go
   ```

## 调试技巧

### 启用详细日志

在 `Help > Diagnostic Tools > Debug Log Settings` 中添加：

```
#com.github.anniext.pprofview:trace
```

这将启用更详细的跟踪日志。

### 使用 println 调试

代码中已经添加了 `println` 输出，可以在 IDE 控制台中查看：

```kotlin
println("用户点击函数: $functionName")
println("项目: ${project.name}")
println("pprof 文件: ${pprofFile.path}")
```

### 断点调试

在以下位置设置断点：

1. `PprofChartPanel.navigateToCode()` - 点击事件入口
2. `PprofCodeNavigationService.navigateToFunction()` - 导航服务入口
3. `PprofCodeNavigationService.executeListCommand()` - 命令执行
4. `PprofCodeNavigationService.findSourceFile()` - 文件查找

### 测试用例

运行测试用例验证功能：

```bash
./gradlew test --tests "PprofCodeNavigationServiceTest"
```

## 性能问题

### 点击响应慢

**正常耗时**：200-500ms

**如果超过 1 秒**：

1. **检查 pprof 命令耗时**：
   ```bash
   time go tool pprof -list='function.name' profile.pprof
   ```

2. **检查文件系统**：
   - 避免在网络驱动器上运行
   - 检查防病毒软件是否扫描

3. **检查 IDE 负载**：
   - 等待索引完成
   - 关闭不必要的插件

### 内存占用高

如果处理大型 pprof 文件（>100MB），可能会占用较多内存。

**解决方案**：
- 增加 IDE 内存限制（Help > Edit Custom VM Options）
- 使用 `-top` 或 `-list` 参数只查看部分数据

## 获取帮助

如果以上方法都无法解决问题，请提供以下信息：

1. **IDE 版本**：Help > About
2. **Go 版本**：`go version`
3. **插件版本**：Settings > Plugins > pprofview
4. **错误日志**：从 idea.log 中复制相关日志
5. **pprof 文件**：如果可能，提供示例 pprof 文件
6. **重现步骤**：详细描述操作步骤

提交 Issue：https://github.com/anniext/pprofview/issues
