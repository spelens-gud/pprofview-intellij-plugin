# Pre-Release Summary

English | [简体中文](PRE_RELEASE_SUMMARY_ZH.md)

This document summarizes the preparation work done for releasing Pprof Plus plugin.

## ✅ Completed Work

### 1. Core Documentation

- ✅ **LICENSE** - MIT License file
- ✅ **CONTRIBUTING.md** - Contributing guide with development standards and submission process
- ✅ **RELEASE.md** - Quick release guide with simplified release steps
- ✅ **README.md** - English project documentation (existing)
- ✅ **README_ZH.md** - Chinese project documentation (existing)
- ✅ **CHANGELOG.md** - Changelog (existing)

### 2. GitHub Configuration

#### Issue and PR Templates
- ✅ `.github/ISSUE_TEMPLATE/bug_report.md` - Bug report template
- ✅ `.github/ISSUE_TEMPLATE/feature_request.md` - Feature request template
- ✅ `.github/PULL_REQUEST_TEMPLATE.md` - Pull Request template

#### Release Documentation
- ✅ `.github/RELEASE_CHECKLIST.md` - Detailed release checklist
- ✅ `.github/SECRETS_SETUP.md` - GitHub Secrets setup guide
- ✅ `.github/README.md` - GitHub configuration files documentation

#### Workflows
- ✅ `.github/workflows/build.yml` - Build and test workflow (existing)
- ✅ `.github/workflows/release.yml` - Release workflow (existing)
- ✅ `.github/workflows/run-ui-tests.yml` - UI test workflow (existing)

### 3. Automation Scripts

- ✅ `scripts/release.sh` - Automated release script
  - Version number validation
  - Automatic test execution
  - Automatic version number update
  - Automatic tag creation and push

### 4. Build Configuration Optimization

- ✅ Disabled `buildSearchableOptions` task to eliminate build warnings

## 📋 Pre-Release Checklist

### Required Configuration

#### 1. GitHub Secrets (⚠️ Required)

Configure the following Secrets in GitHub repository settings:

- [ ] **PUBLISH_TOKEN** - JetBrains Marketplace publishing token
  - 📖 Reference: `.github/SECRETS_SETUP.md`
  
- [ ] **CERTIFICATE_CHAIN** - Plugin signing certificate chain
  - 📖 Reference: `.github/SECRETS_SETUP.md`
  
- [ ] **PRIVATE_KEY** - Plugin signing private key
  - 📖 Reference: `.github/SECRETS_SETUP.md`
  
- [ ] **PRIVATE_KEY_PASSWORD** - Private key password
  - 📖 Reference: `.github/SECRETS_SETUP.md`

#### 2. Code Quality Checks

- [ ] Run tests: `./gradlew test`
- [ ] Run plugin verification: `./gradlew verifyPlugin`
- [ ] Build plugin: `./gradlew buildPlugin`
- [ ] Local testing: `./gradlew runIde`

#### 3. Documentation Review

- [ ] README.md content accurate and complete
- [ ] README_ZH.md synchronized with English version
- [ ] CHANGELOG.md updated with current version changes
- [ ] All links accessible

#### 4. Plugin Configuration

- [ ] Plugin description in `plugin.xml` is accurate
- [ ] Version number in `gradle.properties` is correct
- [ ] Plugin icon exists (`pluginIcon.svg`)

## 🚀 Release Process

### Method 1: Using Automation Script (Recommended)

```bash
# Run release script
./scripts/release.sh 1.0.0

# Script will automatically:
# 1. Validate version number format
# 2. Run tests and verification
# 3. Update version number
# 4. Commit changes
# 5. Create and push tag
```

### Method 2: Manual Release

```bash
# 1. Update version number (in gradle.properties)
pluginVersion = 1.0.0

# 2. Update CHANGELOG.md

# 3. Commit changes
git add gradle.properties CHANGELOG.md
git commit -m "chore: prepare release 1.0.0"
git push origin main

# 4. Create and push tag
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

### Post-Release Steps

1. **Wait for CI Build**
   - Visit [GitHub Actions](https://github.com/spelens-gud/pprofview-intellij-plugin/actions)
   - Wait for Build workflow to complete (about 10-20 minutes)

2. **Publish Draft Release**
   - Visit [Releases](https://github.com/spelens-gud/pprofview-intellij-plugin/releases)
   - Check auto-created Draft Release
   - Click "Publish release"

3. **Wait for Marketplace Publication**
   - GitHub Actions will automatically publish to JetBrains Marketplace
   - Wait for JetBrains review (hours to days)

4. **Verify Release**
   - Confirm plugin is published in Marketplace
   - Test installation in IDE

## 📚 Related Documentation

### Quick Reference
- 🚀 [Quick Release Guide](RELEASE.md) - Simplified release steps
- 📋 [Detailed Checklist](.github/RELEASE_CHECKLIST.md) - Complete release checklist
- 🔐 [Secrets Setup](.github/SECRETS_SETUP.md) - GitHub Secrets setup guide

### Development Documentation
- 🤝 [Contributing Guide](CONTRIBUTING_EN.md) - How to contribute code
- 📝 [Changelog](CHANGELOG_EN.md) - Version history

### GitHub Configuration
- 🐛 [Bug Report Template](.github/ISSUE_TEMPLATE/bug_report.md)
- ✨ [Feature Request Template](.github/ISSUE_TEMPLATE/feature_request.md)
- 🔀 [PR Template](.github/PULL_REQUEST_TEMPLATE.md)

## 🎯 Next Steps

### Immediate Actions

1. **Configure GitHub Secrets** (Most Important!)
   - Follow `.github/SECRETS_SETUP.md` to configure all required Secrets
   - Cannot publish to Marketplace without these configurations

2. **Run Complete Tests**
   ```bash
   ./gradlew test
   ./gradlew verifyPlugin
   ./gradlew buildPlugin
   ```

3. **Local Plugin Testing**
   ```bash
   ./gradlew runIde
   ```

### Prepare for Release

1. **Final Check**
   - Use `.github/RELEASE_CHECKLIST.md` for complete check
   - Ensure all documentation is accurate

2. **Execute Release**
   ```bash
   ./scripts/release.sh 1.0.0
   ```

3. **Monitor Release Process**
   - Watch GitHub Actions build status
   - Handle any errors promptly

## ⚠️ Important Reminders

1. **First Release**
   - First release requires longer review time
   - Ensure plugin description is clear and accurate
   - Provide sufficient documentation and examples

2. **Version Numbering**
   - Follow Semantic Versioning (SemVer)
   - Major.Minor.Patch
   - Example: 1.0.0, 1.1.0, 1.0.1

3. **Security**
   - Keep private keys and certificates secure
   - Never commit Secrets to code repository
   - Rotate API Tokens regularly

4. **Test Thoroughly**
   - Test in multiple IDE versions
   - Test all major features
   - Ensure no critical bugs

## 📞 Getting Help

If you encounter issues:

1. Check relevant documentation (see "Related Documentation" section above)
2. Check [GitHub Actions Logs](https://github.com/spelens-gud/pprofview-intellij-plugin/actions)
3. Refer to [IntelliJ Platform Documentation](https://plugins.jetbrains.com/docs/intellij/)
4. Create an Issue in the project

---

**Good luck with the release! 🎉**
