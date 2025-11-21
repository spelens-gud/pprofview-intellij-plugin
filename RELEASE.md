# Quick Release Guide

English | [简体中文](RELEASE_ZH.md)

This document provides step-by-step instructions for releasing a new version.

## Prerequisites

✅ Complete [GitHub Secrets Setup](.github/SECRETS_SETUP.md)  
✅ All tests pass  
✅ Code merged to main branch

## Release Steps

### 1. Update Version Number

Edit `gradle.properties`:

```properties
pluginVersion = 1.0.1  # Update to new version
```

### 2. Update CHANGELOG

Edit `CHANGELOG.md`, move `[Unreleased]` section to new version:

```markdown
## [1.0.1] - 2025-11-22

### Added
- New feature description

### Fixed
- Bug fix description

### Changed
- Change description

## [Unreleased]
```

### 3. Commit Changes

```bash
git add gradle.properties CHANGELOG.md
git commit -m "chore: prepare release 1.0.1"
git push origin main
```

### 4. Create and Push Tag

```bash
git tag -a v1.0.1 -m "Release version 1.0.1"
git push origin v1.0.1
```

### 5. Wait for Automated Build

1. Visit [GitHub Actions](https://github.com/spelens-gud/pprofview-intellij-plugin/actions)
2. Wait for Build workflow to complete
3. Check if Draft Release is created

### 6. Publish Release

1. Visit [Releases page](https://github.com/spelens-gud/pprofview-intellij-plugin/releases)
2. Find the auto-created Draft Release
3. Review Release Notes
4. Click **Publish release**

### 7. Wait for Marketplace Publication

1. GitHub Actions will automatically publish to JetBrains Marketplace
2. Visit [Actions](https://github.com/spelens-gud/pprofview-intellij-plugin/actions) to monitor progress
3. Wait for JetBrains review (usually hours to days)

### 8. Verify Release

1. Visit [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/XXXXX-pprof-plus)
2. Confirm new version is published
3. Test installation in IDE

## Version Numbering

Follow [Semantic Versioning](https://semver.org/):

- **Major version**: Incompatible API changes
  - Example: 1.0.0 → 2.0.0
  
- **Minor version**: Backward-compatible new features
  - Example: 1.0.0 → 1.1.0
  
- **Patch version**: Backward-compatible bug fixes
  - Example: 1.0.0 → 1.0.1

## Pre-release Versions

To release pre-release versions (alpha, beta, rc):

```bash
# Update version
pluginVersion = 1.1.0-beta.1

# Create tag
git tag -a v1.1.0-beta.1 -m "Release version 1.1.0-beta.1"
git push origin v1.1.0-beta.1
```

Pre-release versions are automatically published to corresponding Release Channels.

## Rolling Back a Release

If you discover critical issues:

### Option 1: Hide Version

1. Login to [JetBrains Marketplace](https://plugins.jetbrains.com/)
2. Go to plugin management page
3. Find the problematic version, click **Hide**

### Option 2: Quick Fix Release

```bash
# Fix the issue
git commit -am "fix: critical issue"

# Release fix version
pluginVersion = 1.0.2
git tag -a v1.0.2 -m "Release version 1.0.2"
git push origin v1.0.2
```

## Common Issues

### Q: Release failed with authentication error

**A**: Check if `PUBLISH_TOKEN` in GitHub Secrets is correctly configured. See [Secrets Setup Guide](.github/SECRETS_SETUP.md).

### Q: Plugin signing failed

**A**: Check if `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, and `PRIVATE_KEY_PASSWORD` are correctly configured.

### Q: How to revoke a published version

**A**: Cannot fully revoke, but you can hide the version in Marketplace and quickly release a fix version.

### Q: How long until visible in Marketplace

**A**: Usually hours to days, depending on JetBrains review speed. First releases may take longer.

### Q: How to publish to specific Release Channel

**A**: Use pre-release identifiers in version number:
- `1.0.0-alpha.1` → alpha channel
- `1.0.0-beta.1` → beta channel
- `1.0.0-rc.1` → rc channel
- `1.0.0` → default channel

## Release Checklist

Use the [Release Checklist](.github/RELEASE_CHECKLIST.md) to ensure no steps are missed.

## Automation Script

You can create a script to automate the release process:

```bash
#!/bin/bash
# release.sh - Automated release script

VERSION=$1

if [ -z "$VERSION" ]; then
    echo "Usage: ./release.sh <version>"
    echo "Example: ./release.sh 1.0.1"
    exit 1
fi

echo "Preparing release version $VERSION"

# 1. Update version
sed -i '' "s/pluginVersion = .*/pluginVersion = $VERSION/" gradle.properties

# 2. Commit changes
git add gradle.properties CHANGELOG.md
git commit -m "chore: prepare release $VERSION"

# 3. Create tag
git tag -a "v$VERSION" -m "Release version $VERSION"

# 4. Push
git push origin main
git push origin "v$VERSION"

echo "✅ Release process initiated"
echo "📝 Visit GitHub Actions to monitor build progress"
echo "🔗 https://github.com/spelens-gud/pprofview-intellij-plugin/actions"
```

Usage:

```bash
chmod +x release.sh
./release.sh 1.0.1
```

## Related Documentation

- [Release Checklist](.github/RELEASE_CHECKLIST.md)
- [Secrets Setup Guide](.github/SECRETS_SETUP.md)
- [Contributing Guide](CONTRIBUTING_EN.md)
- [Changelog](CHANGELOG_EN.md)
