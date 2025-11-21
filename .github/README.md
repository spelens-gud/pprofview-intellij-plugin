# GitHub 配置文件说明

本目录包含 GitHub 相关的配置文件和文档。

## 📁 目录结构

```
.github/
├── ISSUE_TEMPLATE/          # Issue 模板
│   ├── bug_report.md        # Bug 报告模板
│   └── feature_request.md   # 功能请求模板
├── workflows/               # GitHub Actions 工作流
│   ├── build.yml           # 构建和测试工作流
│   ├── release.yml         # 发布工作流
│   └── run-ui-tests.yml    # UI 测试工作流
├── dependabot.yml          # Dependabot 配置
├── PULL_REQUEST_TEMPLATE.md # PR 模板
├── RELEASE_CHECKLIST.md    # 发布检查清单
└── SECRETS_SETUP.md        # Secrets 配置指南
```

## 📝 文件说明

### Issue 模板

- **bug_report.md**: 用户报告 bug 时使用的模板
- **feature_request.md**: 用户请求新功能时使用的模板

### 工作流

- **build.yml**: 在每次推送和 PR 时自动运行构建、测试和验证
- **release.yml**: 在发布 Release 时自动发布插件到 JetBrains Marketplace
- **run-ui-tests.yml**: 运行 UI 测试

### 配置指南

- **RELEASE_CHECKLIST.md**: 发布前的完整检查清单
- **SECRETS_SETUP.md**: 配置 GitHub Secrets 的详细指南
- **PULL_REQUEST_TEMPLATE.md**: 创建 PR 时的模板

## 🚀 快速开始

### 首次发布前的准备

1. 阅读 [SECRETS_SETUP.md](SECRETS_SETUP.md) 配置必需的 GitHub Secrets
2. 阅读 [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) 了解发布流程
3. 确保所有 CI 检查通过

### 发布新版本

参考项目根目录的 [RELEASE.md](../RELEASE.md) 文档。

## 🔧 自定义

如果需要修改模板或工作流：

1. **Issue 模板**: 编辑 `ISSUE_TEMPLATE/` 目录下的文件
2. **PR 模板**: 编辑 `PULL_REQUEST_TEMPLATE.md`
3. **工作流**: 编辑 `workflows/` 目录下的 YAML 文件

## 📚 相关文档

- [GitHub Actions 文档](https://docs.github.com/en/actions)
- [Issue 模板文档](https://docs.github.com/en/communities/using-templates-to-encourage-useful-issues-and-pull-requests)
- [IntelliJ Platform Plugin 发布文档](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html)
