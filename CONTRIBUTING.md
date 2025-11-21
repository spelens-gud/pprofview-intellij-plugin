# 贡献指南

感谢你对 Pprof Plus 的关注！我们欢迎任何形式的贡献。

## 如何贡献

### 报告问题

如果你发现了 bug 或有功能建议，请：

1. 在 [Issues](https://github.com/spelens-gud/pprofview-intellij-plugin/issues) 中搜索是否已有相关问题
2. 如果没有，创建一个新的 issue，并提供：
   - 清晰的标题和描述
   - 复现步骤（如果是 bug）
   - 预期行为和实际行为
   - 环境信息（IDE 版本、Go 版本、操作系统等）
   - 相关的日志或截图

### 提交代码

1. **Fork 仓库**
   ```bash
   git clone https://github.com/your-username/pprofview-intellij-plugin.git
   cd pprofview-intellij-plugin
   ```

2. **创建分支**
   ```bash
   git checkout -b feature/your-feature-name
   # 或
   git checkout -b fix/your-bug-fix
   ```

3. **开发**
   - 遵循项目的代码规范（参考 `.kiro/steering/project-standards.md`）
   - 编写清晰的代码注释（使用中文）
   - 确保代码通过所有测试
   - 添加必要的测试用例

4. **提交**
   ```bash
   git add .
   git commit -m "feat: 添加新功能描述"
   # 或
   git commit -m "fix: 修复问题描述"
   ```
   
   提交信息格式遵循 [Conventional Commits](https://www.conventionalcommits.org/)：
   - `feat:` 新功能
   - `fix:` Bug 修复
   - `docs:` 文档更新
   - `style:` 代码格式调整
   - `refactor:` 重构
   - `test:` 测试相关
   - `chore:` 构建/工具相关

5. **推送并创建 Pull Request**
   ```bash
   git push origin feature/your-feature-name
   ```
   
   然后在 GitHub 上创建 Pull Request，并：
   - 提供清晰的 PR 标题和描述
   - 关联相关的 issue（如 `Closes #123`）
   - 等待代码审查

## 开发环境设置

### 前置要求

- JDK 21+
- IntelliJ IDEA 2025.2+ 或 GoLand 2025.2+
- Go 1.16+

### 构建项目

```bash
# 构建插件
./gradlew buildPlugin

# 运行测试
./gradlew test

# 运行 IDE（用于调试）
./gradlew runIde

# 代码检查
./gradlew verifyPlugin
```

### 项目结构

```
src/main/kotlin/com/github/spelens/pprofview/
├── actions/         # IDE 操作
├── model/           # 数据模型
├── parser/          # pprof 文件解析
├── runconfig/       # 运行配置
├── services/        # 服务层
├── startup/         # 启动活动
├── toolWindow/      # 工具窗口
├── ui/              # UI 组件
└── utils/           # 工具类
```

## 代码规范

### Kotlin 代码风格

- 使用 4 空格缩进
- 每行最大 120 字符
- 优先使用 Kotlin 特性（data class、sealed class、extension functions 等）
- 避免使用 `!!` 操作符

### 注释规范

- 所有公共 API 必须有 KDoc 注释
- 注释使用中文
- 复杂逻辑需要添加行内注释

示例：
```kotlin
/**
 * 解析 pprof 格式的性能分析文件
 *
 * @param file 要解析的文件
 * @return 解析后的性能分析数据
 * @throws PprofParseException 当文件格式不正确时
 */
fun parseProfile(file: VirtualFile): Profile {
    // 实现代码
}
```

## 测试

- 为新功能添加单元测试
- 确保所有测试通过：`./gradlew test`
- 测试数据放在 `src/test/testData/` 目录

## 文档

- 更新相关文档（README.md、CHANGELOG.md 等）
- 如果添加了新功能，在 README 中添加使用说明
- 在 CHANGELOG.md 的 `[Unreleased]` 部分记录变更

## 发布流程

发布由维护者负责：

1. 更新 `gradle.properties` 中的版本号
2. 更新 `CHANGELOG.md`
3. 创建 Git tag
4. GitHub Actions 自动构建并发布到 JetBrains Marketplace

## 行为准则

- 尊重所有贡献者
- 保持友好和专业的交流
- 接受建设性的批评
- 关注对项目最有利的事情

## 获取帮助

如果你有任何问题：

- 查看 [文档](README.md)
- 搜索或创建 [Issue](https://github.com/spelens-gud/pprofview-intellij-plugin/issues)
- 查看 [IntelliJ Platform SDK 文档](https://plugins.jetbrains.com/docs/intellij/)

## 许可证

通过贡献代码，你同意你的贡献将在 [MIT License](LICENSE) 下授权。

---

再次感谢你的贡献！🎉
