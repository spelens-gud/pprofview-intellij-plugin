# GitHub Configuration Files

English | [简体中文](README_ZH.md)

This directory contains GitHub-related configuration files and documentation.

## 📁 Directory Structure

```
.github/
├── ISSUE_TEMPLATE/          # Issue templates
│   ├── bug_report.md        # Bug report template
│   └── feature_request.md   # Feature request template
├── workflows/               # GitHub Actions workflows
│   ├── build.yml           # Build and test workflow
│   ├── release.yml         # Release workflow
│   └── run-ui-tests.yml    # UI test workflow
├── dependabot.yml          # Dependabot configuration
├── PULL_REQUEST_TEMPLATE.md # PR template
├── RELEASE_CHECKLIST.md    # Release checklist
└── SECRETS_SETUP.md        # Secrets setup guide
```

## 📝 File Descriptions

### Issue Templates

- **bug_report.md**: Template for users to report bugs
- **feature_request.md**: Template for users to request new features

### Workflows

- **build.yml**: Automatically runs build, test, and verification on every push and PR
- **release.yml**: Automatically publishes plugin to JetBrains Marketplace on release
- **run-ui-tests.yml**: Runs UI tests

### Configuration Guides

- **RELEASE_CHECKLIST.md**: Complete checklist before releasing
- **SECRETS_SETUP.md**: Detailed guide for configuring GitHub Secrets
- **PULL_REQUEST_TEMPLATE.md**: Template for creating PRs

## 🚀 Quick Start

### Before First Release

1. Read [SECRETS_SETUP.md](SECRETS_SETUP.md) to configure required GitHub Secrets
2. Read [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) to understand the release process
3. Ensure all CI checks pass

### Releasing a New Version

Refer to [RELEASE.md](../RELEASE.md) in the project root.

## 🔧 Customization

To modify templates or workflows:

1. **Issue Templates**: Edit files in `ISSUE_TEMPLATE/` directory
2. **PR Template**: Edit `PULL_REQUEST_TEMPLATE.md`
3. **Workflows**: Edit YAML files in `workflows/` directory

## 📚 Related Documentation

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Issue Templates Documentation](https://docs.github.com/en/communities/using-templates-to-encourage-useful-issues-and-pull-requests)
- [IntelliJ Platform Plugin Publishing Documentation](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html)
