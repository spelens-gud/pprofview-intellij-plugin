# 函数名匹配策略

## 问题背景

pprof 中的函数名可能包含特殊字符，直接使用 `-list` 参数可能会失败：

```bash
# 这个命令可能失败（退出码 2）
go tool pprof -list='github.com/wolfogre/go-pprof-practice/animal/canidae/wolf.(*Wolf).Drink.func1' profile.pprof
```

原因：
- 函数名包含特殊字符：`(`, `)`, `*`, `.`
- pprof 的 `-list` 参数使用正则表达式匹配
- 特殊字符在正则表达式中有特殊含义

## 解决方案

插件使用多种模式依次尝试，直到找到匹配的函数。

### 模式 1: 原始函数名

直接使用完整的函数名：

```
github.com/wolfogre/go-pprof-practice/animal/canidae/wolf.(*Wolf).Drink.func1
```

**适用场景**：简单的函数名，不包含特殊字符。

### 模式 2: 提取方法名

提取最后一个 `.` 之后的部分：

```
输入: github.com/wolfogre/go-pprof-practice/animal/canidae/wolf.(*Wolf).Drink.func1
提取: func1 (跳过，因为是匿名函数)

输入: github.com/wolfogre/go-pprof-practice/animal/canidae/wolf.(*Wolf).Drink
提取: Drink
```

**适用场景**：方法名在项目中唯一。

### 模式 3: 提取类型和方法

提取 `(*Type).Method` 部分：

```
输入: github.com/wolfogre/go-pprof-practice/animal/canidae/wolf.(*Wolf).Drink.func1
提取: (*Wolf).Drink
```

**适用场景**：类型和方法的组合在项目中唯一。

### 模式 4: 转义特殊字符

将特殊字符转义为正则表达式字面量：

```
输入: github.com/wolfogre/go-pprof-practice/animal/canidae/wolf.(*Wolf).Drink.func1
转义: github\.com/wolfogre/go-pprof-practice/animal/canidae/wolf\.\(\*Wolf\)\.Drink\.func1
```

**适用场景**：需要精确匹配完整函数名。

### 模式 5: 简化路径

只使用路径的最后两部分：

```
输入: github.com/wolfogre/go-pprof-practice/animal/canidae/wolf.(*Wolf).Drink.func1
提取: wolf.(*Wolf).Drink.func1
```

**适用场景**：包名的最后部分足以区分。

### 模式 6: 包名和函数名

提取最后一个 `/` 之后的部分：

```
输入: github.com/wolfogre/go-pprof-practice/animal/canidae/wolf.(*Wolf).Drink.func1
提取: wolf.(*Wolf).Drink.func1
```

**适用场景**：包名和函数名的组合唯一。

## 实际案例

### 案例 1: 方法调用

**函数名**：
```
github.com/wolfogre/go-pprof-practice/animal/canidae/wolf.(*Wolf).Drink.func1
```

**尝试顺序**：
1. ❌ 完整名称（特殊字符导致失败）
2. ❌ `func1`（太通用，可能匹配多个）
3. ✅ `(*Wolf).Drink`（成功匹配）

**pprof 输出**：
```
ROUTINE ======================== github.com/wolfogre/go-pprof-practice/animal/canidae/wolf.(*Wolf).Drink.func1 in /path/to/wolf.go
      10ms      10ms (flat, cum)  0.10% of Total
         .          .     45:func (w *Wolf) Drink() {
         .          .     46:    go func() {
      10ms       10ms     47:        fmt.Println("wolf drink")
         .          .     48:    }()
         .          .     49:}
```

### 案例 2: 包级函数

**函数名**：
```
github.com/wolfogre/go-pprof-practice/main.main
```

**尝试顺序**：
1. ✅ 完整名称（成功匹配）

**pprof 输出**：
```
ROUTINE ======================== github.com/wolfogre/go-pprof-practice/main.main in /path/to/main.go
     100ms     100ms (flat, cum)  1.00% of Total
         .          .     10:func main() {
     100ms     100ms     11:    // ...
         .          .     12:}
```

### 案例 3: 泛型函数

**函数名**：
```
github.com/user/project/pkg.GenericFunc[go.shape.int]
```

**尝试顺序**：
1. ❌ 完整名称（`[` 和 `]` 是特殊字符）
2. ✅ `GenericFunc`（成功匹配）

## 性能考虑

### 尝试次数

- 最多尝试 6 种模式
- 每次尝试需要执行一次 pprof 命令
- 典型耗时：50-200ms/次

### 优化策略

1. **成功模式缓存**（未实现）
   - 记录成功的模式
   - 下次优先使用

2. **并行尝试**（未实现）
   - 同时执行多个模式
   - 使用第一个成功的结果

3. **智能排序**
   - 将最可能成功的模式放在前面
   - 减少平均尝试次数

## 调试

### 查看尝试过程

日志中会记录每个模式的尝试：

```
尝试模式 1/6: github.com/wolfogre/go-pprof-practice/animal/canidae/wolf.(*Wolf).Drink.func1
pprof 命令执行失败，退出码: 2

尝试模式 2/6: func1
pprof 命令执行失败，退出码: 2

尝试模式 3/6: (*Wolf).Drink
模式 3 成功，获取到输出
```

### 手动测试

使用测试脚本验证：

```bash
./test_function_patterns.sh profile.pprof 'github.com/wolfogre/go-pprof-practice/animal/canidae/wolf.(*Wolf).Drink.func1'
```

输出：
```
=========================================
测试 pprof -list 函数名匹配
=========================================
pprof 文件: profile.pprof
函数名: github.com/wolfogre/go-pprof-practice/animal/canidae/wolf.(*Wolf).Drink.func1

提取的部分:
  - 最后一部分: func1
  - 包名和函数: wolf.(*Wolf).Drink.func1

----------------------------------------
模式 1: github.com/wolfogre/go-pprof-practice/animal/canidae/wolf.(*Wolf).Drink.func1
----------------------------------------
✗ 失败 (退出码: 2)

----------------------------------------
模式 2: func1
----------------------------------------
✗ 失败 (退出码: 2)

----------------------------------------
模式 3: wolf.(*Wolf).Drink.func1
----------------------------------------
✓ 成功
ROUTINE ======================== github.com/wolfogre/go-pprof-practice/animal/canidae/wolf.(*Wolf).Drink.func1 in /path/to/wolf.go
...

找到匹配！使用模式: wolf.(*Wolf).Drink.func1
```

## 已知限制

### 1. 多个匹配

如果简化的模式匹配到多个函数，pprof 会返回所有匹配的函数：

```bash
go tool pprof -list='Drink' profile.pprof
```

可能返回：
- `(*Wolf).Drink`
- `(*Dog).Drink`
- `(*Cat).Drink`

**解决方案**：使用更具体的模式，如 `Wolf.*Drink`。

### 2. 特殊字符

某些特殊字符可能无法正确处理：
- Unicode 字符
- 空格
- 制表符

**解决方案**：避免在函数名中使用这些字符。

### 3. 性能开销

多次尝试会增加响应时间：
- 1 次尝试：~100ms
- 6 次尝试：~600ms

**解决方案**：实现缓存机制。

## 改进建议

### 1. 用户自定义模式

允许用户配置自定义的匹配模式：

```json
{
  "functionNamePatterns": [
    "{methodName}",
    "{typeName}.{methodName}",
    "{packageName}.{typeName}.{methodName}"
  ]
}
```

### 2. 学习机制

记录成功的模式，自动调整优先级：

```kotlin
private val successfulPatterns = mutableMapOf<String, String>()

fun getPattern(functionName: String): String {
    return successfulPatterns[functionName] ?: buildDefaultPattern(functionName)
}
```

### 3. 正则表达式优化

使用更智能的正则表达式：

```kotlin
// 匹配任意包路径 + 类型 + 方法
val pattern = ".*${typeName}.*${methodName}"
```

## 参考资料

- [pprof 文档](https://github.com/google/pprof/blob/main/doc/README.md)
- [Go 函数命名规范](https://golang.org/doc/effective_go#names)
- [正则表达式语法](https://pkg.go.dev/regexp/syntax)
