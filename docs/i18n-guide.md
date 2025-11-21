# Internationalization Guide / 国际化使用指南

[English](#english) | [简体中文](#简体中文)

---

## English

### Overview

The Pprof Plus plugin has been fully internationalized and supports both Chinese and English languages.

### Resource Files

- `src/main/resources/messages/PprofViewBundle.properties` - English resources (default)
- `src/main/resources/messages/PprofViewBundle_zh_CN.properties` - Simplified Chinese resources

### Usage in Kotlin Code

#### 1. Get Bundle Instance

```kotlin
import com.github.spelens.pprofview.PprofViewBundle

// Get localized string
val message = PprofViewBundle.message("pprof.log.started")
```

#### 2. Messages with Parameters

```kotlin
// Use placeholders {0}, {1}, etc.
val message = PprofViewBundle.message("pprof.log.fileOpened", fileName)
val error = PprofViewBundle.message("pprof.log.error", errorMessage)
```

#### 3. Common Message Keys

##### Configuration
- `pprof.config.enable` - Enable pprof profiling
- `pprof.config.collectionMode` - Collection mode
- `pprof.config.profileTypes` - Profile types
- `pprof.config.outputDirectory` - Output directory

##### Collection Modes
- `pprof.mode.compileTime` - Compile-time instrumentation
- `pprof.mode.runtime` - Runtime sampling
- `pprof.mode.manual` - Manual collection
- `pprof.mode.http` - HTTP service

##### Profile Types
- `pprof.type.cpu` - CPU profiling
- `pprof.type.heap` - Heap profiling
- `pprof.type.goroutine` - Goroutine profiling
- `pprof.type.block` - Block profiling
- `pprof.type.mutex` - Mutex profiling

##### Log Messages
- `pprof.log.started` - pprof profiling started
- `pprof.log.fileOpened` - Opened profile file: {0}
- `pprof.log.fileNotFound` - Profile file not found or empty: {0}
- `pprof.log.error` - pprof configuration failed: {0}

### Adding New Internationalized Strings

#### Steps

1. Add English key-value pair in `PprofViewBundle.properties`:
```properties
pprof.new.message=New message in English
pprof.new.messageWithParam=Message with parameter: {0}
```

2. Add corresponding Chinese translation in `PprofViewBundle_zh_CN.properties`:
```properties
pprof.new.message=新消息（中文）
pprof.new.messageWithParam=带参数的消息: {0}
```

3. Use in code:
```kotlin
val message = PprofViewBundle.message("pprof.new.message")
val messageWithParam = PprofViewBundle.message("pprof.new.messageWithParam", value)
```

### Code Comment Standards

According to project standards, code comments are now in English to meet internationalization standards:

- KDoc comments for classes and methods use English
- Inline comments use English
- User-visible text is internationalized through resource files

### Go Runtime Logs

Log messages in the Go runtime injection code (`pprof_init.go`) are in English for consistency.

### Testing Internationalization

#### Switching Languages

IntelliJ IDEA automatically selects resource files based on system language:
- Simplified Chinese system → Uses `PprofViewBundle_zh_CN.properties`
- Other language systems → Uses `PprofViewBundle.properties` (English)

#### Manual Testing

You can test different languages by modifying IDE language settings:
1. File → Settings → Appearance & Behavior → System Settings → Language
2. Select language and restart IDE

### Best Practices

1. **Never hardcode user-facing strings**
   ```kotlin
   // ❌ Wrong
   val message = "Enable pprof profiling"
   
   // ✅ Correct
   val message = PprofViewBundle.message("pprof.config.enable")
   ```

2. **Use meaningful key names**
   ```properties
   # ✅ Good key name
   pprof.config.cpuDuration=CPU sampling duration (seconds)
   
   # ❌ Bad key name
   msg1=CPU sampling duration (seconds)
   ```

3. **Maintain key name consistency**
   - Use dot notation for namespaces: `pprof.category.item`
   - Use same prefix for related keys

4. **Parameterize messages**
   ```properties
   # Use {0}, {1}, etc. placeholders
   pprof.log.fileOpened=Opened profile file: {0}
   ```

### Maintenance Checklist

When adding new features, ensure:
- [ ] All user-visible strings are internationalized
- [ ] Both English and Chinese resource files are updated
- [ ] Code comments use English
- [ ] Tested display in both languages

---

## 简体中文

### 概述

Pprof Plus 插件已完成国际化处理，支持中文和英文两种语言。

### 资源文件

- `src/main/resources/messages/PprofViewBundle.properties` - 英文资源（默认）
- `src/main/resources/messages/PprofViewBundle_zh_CN.properties` - 简体中文资源

### 在 Kotlin 代码中使用

#### 1. 获取 Bundle 实例

```kotlin
import com.github.spelens.pprofview.PprofViewBundle

// 获取本地化字符串
val message = PprofViewBundle.message("pprof.log.started")
```

#### 2. 带参数的消息

```kotlin
// 使用占位符 {0}, {1} 等
val message = PprofViewBundle.message("pprof.log.fileOpened", fileName)
val error = PprofViewBundle.message("pprof.log.error", errorMessage)
```

#### 3. 常用消息键

##### 配置相关
- `pprof.config.enable` - 启用 pprof 性能分析
- `pprof.config.collectionMode` - 采集模式
- `pprof.config.profileTypes` - 性能分析类型
- `pprof.config.outputDirectory` - 输出目录

##### 采集模式
- `pprof.mode.compileTime` - 编译时插桩
- `pprof.mode.runtime` - 运行时采样
- `pprof.mode.manual` - 手动采集
- `pprof.mode.http` - HTTP 服务

##### 性能分析类型
- `pprof.type.cpu` - CPU 分析
- `pprof.type.heap` - 堆内存分析
- `pprof.type.goroutine` - 协程分析
- `pprof.type.block` - 阻塞分析
- `pprof.type.mutex` - 互斥锁分析

##### 日志消息
- `pprof.log.started` - pprof 性能分析已启动
- `pprof.log.fileOpened` - 已打开性能分析文件: {0}
- `pprof.log.fileNotFound` - 性能分析文件不存在或为空: {0}
- `pprof.log.error` - pprof 配置失败: {0}

### 添加新的国际化字符串

#### 步骤

1. 在 `PprofViewBundle.properties` 中添加英文键值对：
```properties
pprof.new.message=New message in English
pprof.new.messageWithParam=Message with parameter: {0}
```

2. 在 `PprofViewBundle_zh_CN.properties` 中添加对应的中文翻译：
```properties
pprof.new.message=新消息（中文）
pprof.new.messageWithParam=带参数的消息: {0}
```

3. 在代码中使用：
```kotlin
val message = PprofViewBundle.message("pprof.new.message")
val messageWithParam = PprofViewBundle.message("pprof.new.messageWithParam", value)
```

### 代码注释规范

根据项目规范，代码注释已统一使用英文，以符合国际化标准：

- 类和方法的 KDoc 注释使用英文
- 行内注释使用英文
- 用户可见的文本通过资源文件国际化

### Go 运行时日志

Go 运行时注入代码 (`pprof_init.go`) 中的日志消息已改为英文，保持一致性。

### 测试国际化

#### 切换语言

IntelliJ IDEA 会根据系统语言自动选择资源文件：
- 简体中文系统 → 使用 `PprofViewBundle_zh_CN.properties`
- 其他语言系统 → 使用 `PprofViewBundle.properties`（英文）

#### 手动测试

可以通过修改 IDE 的语言设置来测试不同语言：
1. File → Settings → Appearance & Behavior → System Settings → Language
2. 选择语言后重启 IDE

### 最佳实践

1. **不要硬编码字符串**
   ```kotlin
   // ❌ 错误
   val message = "启用 pprof 性能分析"
   
   // ✅ 正确
   val message = PprofViewBundle.message("pprof.config.enable")
   ```

2. **使用有意义的键名**
   ```properties
   # ✅ 好的键名
   pprof.config.cpuDuration=CPU 采样持续时间（秒）
   
   # ❌ 不好的键名
   msg1=CPU 采样持续时间（秒）
   ```

3. **保持键名一致性**
   - 使用点号分隔命名空间：`pprof.category.item`
   - 相关的键使用相同的前缀

4. **参数化消息**
   ```properties
   # 使用 {0}, {1} 等占位符
   pprof.log.fileOpened=已打开性能分析文件: {0}
   ```

### 维护清单

添加新功能时，确保：
- [ ] 所有用户可见的字符串都已国际化
- [ ] 英文和中文资源文件都已更新
- [ ] 代码注释使用英文
- [ ] 测试了两种语言的显示效果
