# 发布准备总结

本文档总结了为发布 Pprof Plus 插件所做的准备工作。

## ✅ 已完成的工作

### 1. 核心文档

- ✅ **LICENSE** - MIT 许可证文件
- ✅ **CONTRIBUTING.md** - 贡献指南，包含开发规范和提交流程
- ✅ **RELEASE.md** - 快速发布指南，提供简化的发布步骤
- ✅ **README.md** - 英文项目说明（已存在）
- ✅ **README_ZH.md** - 中文项目说明（已存在）
- ✅ **CHANGELOG.md** - 更新日志（已存在）

### 2. GitHub 配置

#### Issue 和 PR 模板
- ✅ `.github/ISSUE_TEMPLATE/bug_report.md` - Bug 报告模板
- ✅ `.github/ISSUE_TEMPLATE/feature_request.md` - 功能请求模板
- ✅ `.github/PULL_REQUEST_TEMPLATE.md` - Pull Request 模板

#### 发布文档
- ✅ `.github/RELEASE_CHECKLIST.md` - 详细的发布检查清单
- ✅ `.github/SECRETS_SETUP.md` - GitHub Secrets 配置指南
- ✅ `.github/README.md` - GitHub 配置文件说明

#### 工作流
- ✅ `.github/workflows/build.yml` - 构建和测试工作流（已存在）
- ✅ `.github/workflows/release.yml` - 发布工作流（已存在）
- ✅ `.github/workflows/run-ui-tests.yml` - UI 测试工作流（已存在）

### 3. 自动化脚本

- ✅ `scripts/release.sh` - 自动化发布脚本
  - 版本号验证
  - 自动运行测试
  - 自动更新版本号
  - 自动创建 tag 并推送

### 4. 构建配置优化

- ✅ 禁用 `buildSearchableOptions` 任务，消除构建警告

## 📋 发布前检查清单

### 必需完成的配置

#### 1. GitHub Secrets（⚠️ 必需）

在 GitHub 仓库设置中配置以下 Secrets：

- [ ] **PUBLISH_TOKEN** - JetBrains Marketplace 发布令牌
  - 📖 参考：`.github/SECRETS_SETUP.md`
  
- [ ] **CERTIFICATE_CHAIN** - 插件签名证书链
  - 📖 参考：`.github/SECRETS_SETUP.md`
  
- [ ] **PRIVATE_KEY** - 插件签名私钥
  - 📖 参考：`.github/SECRETS_SETUP.md`
  
- [ ] **PRIVATE_KEY_PASSWORD** - 私钥密码
  - 📖 参考：`.github/SECRETS_SETUP.md`

#### 2. 代码质量检查

- [ ] 运行测试：`./gradlew test`
- [ ] 运行插件验证：`./gradlew verifyPlugin`
- [ ] 构建插件：`./gradlew buildPlugin`
- [ ] 本地测试：`./gradlew runIde`

#### 3. 文档检查

- [ ] README.md 内容准确完整
- [ ] README_ZH.md 与英文版本同步
- [ ] CHANGELOG.md 已更新当前版本的变更
- [ ] 所有链接可访问

#### 4. 插件配置

- [ ] `plugin.xml` 中的插件描述准确
- [ ] `gradle.properties` 中的版本号正确
- [ ] 插件图标存在（`pluginIcon.svg`）

## 🚀 发布流程

### 方式 1: 使用自动化脚本（推荐）

```bash
# 运行发布脚本
./scripts/release.sh 1.0.0

# 脚本会自动：
# 1. 验证版本号格式
# 2. 运行测试和验证
# 3. 更新版本号
# 4. 提交变更
# 5. 创建并推送 tag
```

### 方式 2: 手动发布

```bash
# 1. 更新版本号（在 gradle.properties 中）
pluginVersion = 1.0.0

# 2. 更新 CHANGELOG.md

# 3. 提交变更
git add gradle.properties CHANGELOG.md
git commit -m "chore: prepare release 1.0.0"
git push origin main

# 4. 创建并推送 tag
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

### 发布后的步骤

1. **等待 CI 构建**
   - 访问 [GitHub Actions](https://github.com/spelens-gud/pprofview-intellij-plugin/actions)
   - 等待 Build 工作流完成（约 10-20 分钟）

2. **发布 Draft Release**
   - 访问 [Releases](https://github.com/spelens-gud/pprofview-intellij-plugin/releases)
   - 检查自动创建的 Draft Release
   - 点击 "Publish release"

3. **等待 Marketplace 发布**
   - GitHub Actions 会自动发布到 JetBrains Marketplace
   - 等待 JetBrains 审核（几小时到几天）

4. **验证发布**
   - 在 Marketplace 中确认插件已发布
   - 在 IDE 中测试安装

## 📚 相关文档

### 快速参考
- 🚀 [快速发布指南](RELEASE.md) - 简化的发布步骤
- 📋 [详细检查清单](.github/RELEASE_CHECKLIST.md) - 完整的发布检查项
- 🔐 [Secrets 配置](.github/SECRETS_SETUP.md) - GitHub Secrets 配置指南

### 开发文档
- 🤝 [贡献指南](CONTRIBUTING.md) - 如何贡献代码
- 📖 [项目规范](.kiro/steering/project-standards.md) - 代码规范和项目结构
- 📝 [更新日志](CHANGELOG.md) - 版本历史

### GitHub 配置
- 🐛 [Bug 报告模板](.github/ISSUE_TEMPLATE/bug_report.md)
- ✨ [功能请求模板](.github/ISSUE_TEMPLATE/feature_request.md)
- 🔀 [PR 模板](.github/PULL_REQUEST_TEMPLATE.md)

## 🎯 下一步行动

### 立即执行

1. **配置 GitHub Secrets**（最重要！）
   - 按照 `.github/SECRETS_SETUP.md` 配置所有必需的 Secrets
   - 没有这些配置，无法发布到 Marketplace

2. **运行完整测试**
   ```bash
   ./gradlew test
   ./gradlew verifyPlugin
   ./gradlew buildPlugin
   ```

3. **本地测试插件**
   ```bash
   ./gradlew runIde
   ```

### 准备发布

1. **最终检查**
   - 使用 `.github/RELEASE_CHECKLIST.md` 进行完整检查
   - 确保所有文档准确无误

2. **执行发布**
   ```bash
   ./scripts/release.sh 1.0.0
   ```

3. **监控发布流程**
   - 关注 GitHub Actions 构建状态
   - 及时处理任何错误

## ⚠️ 重要提醒

1. **首次发布**
   - 首次发布需要更长的审核时间
   - 确保插件描述清晰、准确
   - 提供足够的文档和示例

2. **版本号规范**
   - 遵循语义化版本（SemVer）
   - 主版本号.次版本号.修订号
   - 例如：1.0.0, 1.1.0, 1.0.1

3. **安全性**
   - 妥善保管私钥和证书
   - 不要将 Secrets 提交到代码仓库
   - 定期轮换 API Token

4. **测试充分**
   - 在多个 IDE 版本中测试
   - 测试所有主要功能
   - 确保没有严重 bug

## 📞 获取帮助

如果遇到问题：

1. 查看相关文档（见上方"相关文档"部分）
2. 检查 [GitHub Actions 日志](https://github.com/spelens-gud/pprofview-intellij-plugin/actions)
3. 查看 [IntelliJ Platform 文档](https://plugins.jetbrains.com/docs/intellij/)
4. 在项目中创建 Issue

---

**祝发布顺利！🎉**
