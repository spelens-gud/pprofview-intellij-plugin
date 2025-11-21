# 发布检查清单

[English](RELEASE_CHECKLIST.md) | 简体中文

在将插件发布到 JetBrains Marketplace 之前，请确保完成以下所有项目。

## 📋 必需项目

### 代码和构建

- [ ] 所有测试通过：`./gradlew test`
- [ ] 插件验证通过：`./gradlew verifyPlugin`
- [ ] 代码检查无严重问题：`./gradlew verifyPlugin`
- [ ] 构建成功：`./gradlew buildPlugin`
- [ ] 在本地 IDE 中测试插件：`./gradlew runIde`

### 文档

- [ ] README.md 内容完整准确
- [ ] README_ZH.md 与英文版本同步
- [ ] CHANGELOG.md 更新了当前版本的变更
- [ ] LICENSE 文件存在
- [ ] CONTRIBUTING.md 存在

### 配置文件

- [ ] `gradle.properties` 中的版本号已更新
- [ ] `plugin.xml` 中的插件描述准确
- [ ] `plugin.xml` 中的 `since-build` 版本正确
- [ ] 插件名称和供应商信息正确

### GitHub 配置

- [ ] GitHub Secrets 已配置：
  - [ ] `PUBLISH_TOKEN` - JetBrains Marketplace 令牌
  - [ ] `CERTIFICATE_CHAIN` - 插件签名证书链
  - [ ] `PRIVATE_KEY` - 插件签名私钥
  - [ ] `PRIVATE_KEY_PASSWORD` - 私钥密码
- [ ] GitHub Actions 工作流正常运行
- [ ] 所有 CI 检查通过

### 插件内容

- [ ] 插件图标存在且美观（`pluginIcon.svg`）
- [ ] 所有功能正常工作
- [ ] 没有已知的严重 Bug
- [ ] 性能可接受

## 🔍 可选项目

### 质量保证

- [ ] 代码覆盖率达到合理水平
- [ ] Qodana 代码检查通过
- [ ] 在多个 IDE 版本中测试
- [ ] 在不同操作系统上测试（Windows、macOS、Linux）

### 文档和示例

- [ ] 提供使用示例
- [ ] 截图和 GIF 演示（如适用）
- [ ] API 文档完整（如果提供 API）

### 社区

- [ ] 准备发布公告
- [ ] 社交媒体推广内容（如适用）

## 📝 发布步骤

### 1. 准备发布

```bash
# 1. 确保在 main 分支
git checkout main
git pull origin main

# 2. 更新版本号（在 gradle.properties 中）
# pluginVersion = x.y.z

# 3. 更新 CHANGELOG.md
# 将 [Unreleased] 部分移至新版本

# 4. 提交变更
git add .
git commit -m "chore: prepare release x.y.z"
git push origin main
```

### 2. 创建标签

```bash
# 创建并推送标签
git tag -a vx.y.z -m "Release version x.y.z"
git push origin vx.y.z
```

### 3. 等待 CI 构建

- GitHub Actions 将自动构建并创建草稿 Release
- 检查构建日志是否有错误
- 下载并测试构建的插件

### 4. 发布到 Marketplace

- 在 GitHub Releases 页面找到草稿 Release
- 验证 Release Notes 正确
- 点击 "Publish release"
- GitHub Actions 将自动发布到 JetBrains Marketplace

### 5. 验证发布

- 等待 JetBrains Marketplace 审核（通常需要几小时到几天）
- 在 Marketplace 页面确认插件已发布
- 从 Marketplace 测试安装插件

## 🔐 首次发布额外步骤

如果这是首次发布，还需要：

### 获取 JetBrains Marketplace 令牌

1. 访问 [JetBrains Marketplace](https://plugins.jetbrains.com/)
2. 使用 JetBrains 账号登录
3. 进入 Profile → API Tokens
4. 创建新令牌
5. 将令牌添加到 GitHub Secrets（`PUBLISH_TOKEN`）

### 生成插件签名证书（可选但推荐）

```bash
# 生成私钥
openssl genrsa -out private.pem 4096

# 生成证书请求
openssl req -new -key private.pem -out cert.csr

# 生成自签名证书
openssl x509 -req -days 3650 -in cert.csr -signkey private.pem -out cert.pem

# 将证书和私钥添加到 GitHub Secrets
# CERTIFICATE_CHAIN: cert.pem 的内容
# PRIVATE_KEY: private.pem 的内容
# PRIVATE_KEY_PASSWORD: 私钥密码（如果设置）
```

### 配置 GitHub Secrets

在 GitHub 仓库设置中：
1. 进入 Settings → Secrets and variables → Actions
2. 添加以下 secrets：
   - `PUBLISH_TOKEN`
   - `CERTIFICATE_CHAIN`
   - `PRIVATE_KEY`
   - `PRIVATE_KEY_PASSWORD`

## ⚠️ 重要注意事项

1. **版本号规则**：遵循语义化版本（SemVer）
   - 主版本号：不兼容的 API 变更
   - 次版本号：向后兼容的新功能
   - 修订号：向后兼容的 Bug 修复

2. **CHANGELOG 格式**：保持一致的格式
   - 使用 `### Added`、`### Changed`、`### Fixed` 标题
   - 每个变更一行，简洁明了

3. **充分测试**：发布前充分测试
   - 在不同 IDE 版本中测试
   - 测试所有主要功能
   - 检查性能和内存使用

4. **回滚计划**：如果发现严重问题
   - 可以在 Marketplace 中隐藏版本
   - 快速发布修复版本

## 📞 获取帮助

如果遇到问题：
- 查看 [IntelliJ Platform 插件发布文档](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html)
- 检查 [GitHub Actions 日志](https://github.com/spelens-gud/pprofview-intellij-plugin/actions)
- 在项目 Issues 中寻求帮助
