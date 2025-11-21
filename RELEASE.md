# 快速发布指南

本文档提供快速发布新版本的步骤说明。

## 前置条件

✅ 确保已完成 [GitHub Secrets 配置](.github/SECRETS_SETUP.md)  
✅ 确保所有测试通过  
✅ 确保代码已合并到 main 分支

## 发布步骤

### 1. 更新版本号

编辑 `gradle.properties`：

```properties
pluginVersion = 1.0.1  # 更新为新版本号
```

### 2. 更新 CHANGELOG

编辑 `CHANGELOG.md`，将 `[Unreleased]` 部分移到新版本下：

```markdown
## [1.0.1] - 2025-11-22

### Added
- 新功能描述

### Fixed
- Bug 修复描述

### Changed
- 变更描述

## [Unreleased]
```

### 3. 提交变更

```bash
git add gradle.properties CHANGELOG.md
git commit -m "chore: prepare release 1.0.1"
git push origin main
```

### 4. 创建并推送 Tag

```bash
git tag -a v1.0.1 -m "Release version 1.0.1"
git push origin v1.0.1
```

### 5. 等待自动构建

1. 访问 [GitHub Actions](https://github.com/spelens-gud/pprofview-intellij-plugin/actions)
2. 等待 Build 工作流完成
3. 检查是否创建了 Draft Release

### 6. 发布 Release

1. 访问 [Releases 页面](https://github.com/spelens-gud/pprofview-intellij-plugin/releases)
2. 找到自动创建的 Draft Release
3. 检查 Release Notes 是否正确
4. 点击 **Publish release**

### 7. 等待发布到 Marketplace

1. GitHub Actions 会自动将插件发布到 JetBrains Marketplace
2. 访问 [Actions](https://github.com/spelens-gud/pprofview-intellij-plugin/actions) 查看发布进度
3. 等待 JetBrains 审核（通常几小时到几天）

### 8. 验证发布

1. 访问 [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/XXXXX-pprof-plus)
2. 确认新版本已发布
3. 在 IDE 中测试安装新版本

## 版本号规范

遵循 [语义化版本](https://semver.org/lang/zh-CN/)：

- **主版本号**（Major）：不兼容的 API 变更
  - 例如：1.0.0 → 2.0.0
  
- **次版本号**（Minor）：向后兼容的功能新增
  - 例如：1.0.0 → 1.1.0
  
- **修订号**（Patch）：向后兼容的问题修正
  - 例如：1.0.0 → 1.0.1

## 预发布版本

如果需要发布预发布版本（alpha、beta、rc）：

```bash
# 更新版本号
pluginVersion = 1.1.0-beta.1

# 创建 tag
git tag -a v1.1.0-beta.1 -m "Release version 1.1.0-beta.1"
git push origin v1.1.0-beta.1
```

预发布版本会自动发布到对应的 Release Channel。

## 回滚发布

如果发现严重问题需要回滚：

### 方案 1：隐藏版本

1. 登录 [JetBrains Marketplace](https://plugins.jetbrains.com/)
2. 进入插件管理页面
3. 找到问题版本，点击 **Hide**

### 方案 2：快速发布修复版本

```bash
# 修复问题
git commit -am "fix: 修复严重问题"

# 发布修复版本
pluginVersion = 1.0.2
git tag -a v1.0.2 -m "Release version 1.0.2"
git push origin v1.0.2
```

## 常见问题

### Q: 发布失败，提示认证错误

**A**: 检查 GitHub Secrets 中的 `PUBLISH_TOKEN` 是否正确配置。参考 [Secrets 配置指南](.github/SECRETS_SETUP.md)。

### Q: 插件签名失败

**A**: 检查 `CERTIFICATE_CHAIN`、`PRIVATE_KEY` 和 `PRIVATE_KEY_PASSWORD` 是否正确配置。

### Q: 如何撤销已发布的版本

**A**: 无法完全撤销，但可以在 Marketplace 中隐藏版本，并快速发布修复版本。

### Q: 发布后多久能在 Marketplace 看到

**A**: 通常几小时到几天，取决于 JetBrains 的审核速度。首次发布可能需要更长时间。

### Q: 如何发布到特定的 Release Channel

**A**: 在版本号中使用预发布标识：
- `1.0.0-alpha.1` → alpha channel
- `1.0.0-beta.1` → beta channel
- `1.0.0-rc.1` → rc channel
- `1.0.0` → default channel

## 发布检查清单

使用 [发布检查清单](.github/RELEASE_CHECKLIST.md) 确保不遗漏任何步骤。

## 自动化脚本

可以创建一个脚本来自动化发布流程：

```bash
#!/bin/bash
# release.sh - 自动化发布脚本

VERSION=$1

if [ -z "$VERSION" ]; then
    echo "Usage: ./release.sh <version>"
    echo "Example: ./release.sh 1.0.1"
    exit 1
fi

echo "准备发布版本 $VERSION"

# 1. 更新版本号
sed -i '' "s/pluginVersion = .*/pluginVersion = $VERSION/" gradle.properties

# 2. 提交变更
git add gradle.properties CHANGELOG.md
git commit -m "chore: prepare release $VERSION"

# 3. 创建 tag
git tag -a "v$VERSION" -m "Release version $VERSION"

# 4. 推送
git push origin main
git push origin "v$VERSION"

echo "✅ 发布流程已启动"
echo "📝 请访问 GitHub Actions 查看构建进度"
echo "🔗 https://github.com/spelens-gud/pprofview-intellij-plugin/actions"
```

使用方法：

```bash
chmod +x release.sh
./release.sh 1.0.1
```

## 相关文档

- [发布检查清单](.github/RELEASE_CHECKLIST.md)
- [Secrets 配置指南](.github/SECRETS_SETUP.md)
- [贡献指南](CONTRIBUTING.md)
- [更新日志](CHANGELOG.md)
