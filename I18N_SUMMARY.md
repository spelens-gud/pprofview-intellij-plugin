# Internationalization Summary / 国际化总结

[English](#english) | [简体中文](#简体中文)

---

## English

### Overview

The Pprof Plus plugin has been fully internationalized to support both English and Chinese languages. This document summarizes all internationalization work completed.

### Completed Work

#### 1. Documentation Internationalization

All major documentation has been internationalized with both English and Chinese versions:

**Root Directory:**
- `README.md` (English) / `README_ZH.md` (Chinese)
- `CHANGELOG_EN.md` (English) / `CHANGELOG.md` (Chinese)
- `CONTRIBUTING_EN.md` (English) / `CONTRIBUTING.md` (Chinese)
- `RELEASE.md` (English) / `RELEASE_ZH.md` (Chinese)
- `PRE_RELEASE_SUMMARY.md` (English) / `PRE_RELEASE_SUMMARY_ZH.md` (Chinese)

**GitHub Configuration (.github/):**
- `README.md` (English) / `README_ZH.md` (Chinese)
- `RELEASE_CHECKLIST.md` (English) / `RELEASE_CHECKLIST_ZH.md` (Chinese)
- `SECRETS_SETUP.md` (English) / `SECRETS_SETUP_ZH.md` (Chinese)
- Issue Templates: `bug_report.md` / `bug_report_zh.md`
- Issue Templates: `feature_request.md` / `feature_request_zh.md`
- PR Template: `PULL_REQUEST_TEMPLATE.md` / `PULL_REQUEST_TEMPLATE_ZH.md`

**Documentation (docs/):**
- `i18n-guide.md` - Bilingual internationalization guide

#### 2. Code Internationalization

**Resource Files:**
- `src/main/resources/messages/PprofViewBundle.properties` - English (default)
- `src/main/resources/messages/PprofViewBundle_zh_CN.properties` - Simplified Chinese

**Code Comments:**
- All KDoc comments in English
- All inline comments in English
- User-facing strings internationalized through resource files

#### 3. Plugin Configuration

**plugin.xml:**
- Configured resource bundle: `messages.PprofViewBundle`
- All UI elements use internationalized strings

### File Structure

```
pprofview-intellij-plugin/
├── README.md (English)
├── README_ZH.md (Chinese)
├── CHANGELOG_EN.md (English)
├── CHANGELOG.md (Chinese)
├── CONTRIBUTING_EN.md (English)
├── CONTRIBUTING.md (Chinese)
├── RELEASE.md (English)
├── RELEASE_ZH.md (Chinese)
├── PRE_RELEASE_SUMMARY.md (English)
├── PRE_RELEASE_SUMMARY_ZH.md (Chinese)
├── I18N_SUMMARY.md (This file)
├── .github/
│   ├── README.md (English)
│   ├── README_ZH.md (Chinese)
│   ├── RELEASE_CHECKLIST.md (English)
│   ├── RELEASE_CHECKLIST_ZH.md (Chinese)
│   ├── SECRETS_SETUP.md (English)
│   ├── SECRETS_SETUP_ZH.md (Chinese)
│   ├── PULL_REQUEST_TEMPLATE.md (English)
│   ├── PULL_REQUEST_TEMPLATE_ZH.md (Chinese)
│   └── ISSUE_TEMPLATE/
│       ├── bug_report.md (English)
│       ├── bug_report_zh.md (Chinese)
│       ├── feature_request.md (English)
│       └── feature_request_zh.md (Chinese)
├── docs/
│   └── i18n-guide.md (Bilingual)
└── src/main/resources/messages/
    ├── PprofViewBundle.properties (English)
    └── PprofViewBundle_zh_CN.properties (Chinese)
```

### Language Switching

The plugin automatically detects the system language:
- **Chinese System**: Uses Chinese resources (`PprofViewBundle_zh_CN.properties`)
- **Other Systems**: Uses English resources (`PprofViewBundle.properties`)

Users can manually change the IDE language in:
`File → Settings → Appearance & Behavior → System Settings → Language`

### Developer Guidelines

When adding new features:

1. **User-Facing Strings**: Always use `PprofViewBundle.message("key")`
2. **Add to Both Files**: Update both English and Chinese resource files
3. **Code Comments**: Write in English
4. **Test Both Languages**: Verify display in both English and Chinese

For detailed guidelines, see [docs/i18n-guide.md](docs/i18n-guide.md).

### Maintenance

To maintain internationalization:

1. **New Strings**: Add to both `PprofViewBundle.properties` and `PprofViewBundle_zh_CN.properties`
2. **Documentation Updates**: Update both English and Chinese versions
3. **Code Reviews**: Ensure no hardcoded user-facing strings
4. **Testing**: Test in both languages before release

---

## 简体中文

### 概述

Pprof Plus 插件已完成全面国际化，支持英文和中文两种语言。本文档总结了所有已完成的国际化工作。

### 已完成工作

#### 1. 文档国际化

所有主要文档都已国际化，提供英文和中文版本：

**根目录：**
- `README.md`（英文）/ `README_ZH.md`（中文）
- `CHANGELOG_EN.md`（英文）/ `CHANGELOG.md`（中文）
- `CONTRIBUTING_EN.md`（英文）/ `CONTRIBUTING.md`（中文）
- `RELEASE.md`（英文）/ `RELEASE_ZH.md`（中文）
- `PRE_RELEASE_SUMMARY.md`（英文）/ `PRE_RELEASE_SUMMARY_ZH.md`（中文）

**GitHub 配置 (.github/)：**
- `README.md`（英文）/ `README_ZH.md`（中文）
- `RELEASE_CHECKLIST.md`（英文）/ `RELEASE_CHECKLIST_ZH.md`（中文）
- `SECRETS_SETUP.md`（英文）/ `SECRETS_SETUP_ZH.md`（中文）
- Issue 模板：`bug_report.md` / `bug_report_zh.md`
- Issue 模板：`feature_request.md` / `feature_request_zh.md`
- PR 模板：`PULL_REQUEST_TEMPLATE.md` / `PULL_REQUEST_TEMPLATE_ZH.md`

**文档 (docs/)：**
- `i18n-guide.md` - 双语国际化指南

#### 2. 代码国际化

**资源文件：**
- `src/main/resources/messages/PprofViewBundle.properties` - 英文（默认）
- `src/main/resources/messages/PprofViewBundle_zh_CN.properties` - 简体中文

**代码注释：**
- 所有 KDoc 注释使用英文
- 所有行内注释使用英文
- 用户可见字符串通过资源文件国际化

#### 3. 插件配置

**plugin.xml：**
- 配置资源包：`messages.PprofViewBundle`
- 所有 UI 元素使用国际化字符串

### 文件结构

```
pprofview-intellij-plugin/
├── README.md（英文）
├── README_ZH.md（中文）
├── CHANGELOG_EN.md（英文）
├── CHANGELOG.md（中文）
├── CONTRIBUTING_EN.md（英文）
├── CONTRIBUTING.md（中文）
├── RELEASE.md（英文）
├── RELEASE_ZH.md（中文）
├── PRE_RELEASE_SUMMARY.md（英文）
├── PRE_RELEASE_SUMMARY_ZH.md（中文）
├── I18N_SUMMARY.md（本文件）
├── .github/
│   ├── README.md（英文）
│   ├── README_ZH.md（中文）
│   ├── RELEASE_CHECKLIST.md（英文）
│   ├── RELEASE_CHECKLIST_ZH.md（中文）
│   ├── SECRETS_SETUP.md（英文）
│   ├── SECRETS_SETUP_ZH.md（中文）
│   ├── PULL_REQUEST_TEMPLATE.md（英文）
│   ├── PULL_REQUEST_TEMPLATE_ZH.md（中文）
│   └── ISSUE_TEMPLATE/
│       ├── bug_report.md（英文）
│       ├── bug_report_zh.md（中文）
│       ├── feature_request.md（英文）
│       └── feature_request_zh.md（中文）
├── docs/
│   └── i18n-guide.md（双语）
└── src/main/resources/messages/
    ├── PprofViewBundle.properties（英文）
    └── PprofViewBundle_zh_CN.properties（中文）
```

### 语言切换

插件会自动检测系统语言：
- **中文系统**：使用中文资源（`PprofViewBundle_zh_CN.properties`）
- **其他系统**：使用英文资源（`PprofViewBundle.properties`）

用户可以在以下位置手动更改 IDE 语言：
`File → Settings → Appearance & Behavior → System Settings → Language`

### 开发者指南

添加新功能时：

1. **用户可见字符串**：始终使用 `PprofViewBundle.message("key")`
2. **添加到两个文件**：同时更新英文和中文资源文件
3. **代码注释**：使用英文编写
4. **测试两种语言**：验证英文和中文显示

详细指南请参见 [docs/i18n-guide.md](docs/i18n-guide.md)。

### 维护

维护国际化：

1. **新字符串**：添加到 `PprofViewBundle.properties` 和 `PprofViewBundle_zh_CN.properties`
2. **文档更新**：同时更新英文和中文版本
3. **代码审查**：确保没有硬编码的用户可见字符串
4. **测试**：发布前测试两种语言

---

**Internationalization completed! / 国际化完成！** 🎉
