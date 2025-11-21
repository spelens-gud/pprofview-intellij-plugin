# Release Checklist

English | [简体中文](RELEASE_CHECKLIST_ZH.md)

Before releasing the plugin to JetBrains Marketplace, ensure all items below are completed.

## 📋 Required Items

### Code and Build

- [ ] All tests pass: `./gradlew test`
- [ ] Plugin verification passes: `./gradlew verifyPlugin`
- [ ] Code inspection has no critical issues: `./gradlew verifyPlugin`
- [ ] Build succeeds: `./gradlew buildPlugin`
- [ ] Plugin tested in local IDE: `./gradlew runIde`

### Documentation

- [ ] README.md content is complete and accurate
- [ ] README_ZH.md is synchronized with English version
- [ ] CHANGELOG.md updated with current version changes
- [ ] LICENSE file exists
- [ ] CONTRIBUTING.md exists

### Configuration Files

- [ ] Version number updated in `gradle.properties`
- [ ] Plugin description in `plugin.xml` is accurate
- [ ] `since-build` version in `plugin.xml` is correct
- [ ] Plugin name and vendor information are correct

### GitHub Configuration

- [ ] GitHub Secrets configured:
  - [ ] `PUBLISH_TOKEN` - JetBrains Marketplace token
  - [ ] `CERTIFICATE_CHAIN` - Plugin signing certificate chain
  - [ ] `PRIVATE_KEY` - Plugin signing private key
  - [ ] `PRIVATE_KEY_PASSWORD` - Private key password
- [ ] GitHub Actions workflows running normally
- [ ] All CI checks pass

### Plugin Content

- [ ] Plugin icon exists and looks good (`pluginIcon.svg`)
- [ ] All features work correctly
- [ ] No known critical bugs
- [ ] Performance is acceptable

## 🔍 Optional Items

### Quality Assurance

- [ ] Code coverage reaches reasonable level
- [ ] Qodana code inspection passes
- [ ] Tested in multiple IDE versions
- [ ] Tested on different operating systems (Windows, macOS, Linux)

### Documentation and Examples

- [ ] Usage examples provided
- [ ] Screenshots and GIF demos (if applicable)
- [ ] API documentation complete (if providing API)

### Community

- [ ] Release announcement prepared
- [ ] Social media promotion content (if applicable)

## 📝 Release Steps

### 1. Prepare Release

```bash
# 1. Ensure on main branch
git checkout main
git pull origin main

# 2. Update version number (in gradle.properties)
# pluginVersion = x.y.z

# 3. Update CHANGELOG.md
# Move [Unreleased] section to new version

# 4. Commit changes
git add .
git commit -m "chore: prepare release x.y.z"
git push origin main
```

### 2. Create Tag

```bash
# Create and push tag
git tag -a vx.y.z -m "Release version x.y.z"
git push origin vx.y.z
```

### 3. Wait for CI Build

- GitHub Actions will automatically build and create Draft Release
- Check build logs for errors
- Download and test the built plugin

### 4. Publish to Marketplace

- Find Draft Release on GitHub Releases page
- Verify Release Notes are correct
- Click "Publish release"
- GitHub Actions will automatically publish to JetBrains Marketplace

### 5. Verify Release

- Wait for JetBrains Marketplace review (usually hours to days)
- Confirm plugin is published on Marketplace page
- Test installing plugin from Marketplace

## 🔐 First Release Additional Steps

If this is the first release, you also need to:

### Get JetBrains Marketplace Token

1. Visit [JetBrains Marketplace](https://plugins.jetbrains.com/)
2. Login with your JetBrains account
3. Go to Profile → API Tokens
4. Create new token
5. Add token to GitHub Secrets (`PUBLISH_TOKEN`)

### Generate Plugin Signing Certificate (Optional but Recommended)

```bash
# Generate private key
openssl genrsa -out private.pem 4096

# Generate certificate request
openssl req -new -key private.pem -out cert.csr

# Generate self-signed certificate
openssl x509 -req -days 3650 -in cert.csr -signkey private.pem -out cert.pem

# Add certificate and private key to GitHub Secrets
# CERTIFICATE_CHAIN: content of cert.pem
# PRIVATE_KEY: content of private.pem
# PRIVATE_KEY_PASSWORD: private key password (if set)
```

### Configure GitHub Secrets

In GitHub repository settings:
1. Go to Settings → Secrets and variables → Actions
2. Add the following secrets:
   - `PUBLISH_TOKEN`
   - `CERTIFICATE_CHAIN`
   - `PRIVATE_KEY`
   - `PRIVATE_KEY_PASSWORD`

## ⚠️ Important Notes

1. **Version Numbering**: Follow Semantic Versioning (SemVer)
   - Major version: Incompatible API changes
   - Minor version: Backward-compatible new features
   - Patch version: Backward-compatible bug fixes

2. **CHANGELOG Format**: Maintain consistent format
   - Use `### Added`, `### Changed`, `### Fixed` headings
   - One change per line, concise and clear

3. **Test Thoroughly**: Test adequately before release
   - Test in different IDE versions
   - Test all major features
   - Check performance and memory usage

4. **Rollback Plan**: If critical issues are found
   - Can hide version in Marketplace
   - Quickly release fix version

## 📞 Getting Help

If you encounter issues:
- Check [IntelliJ Platform Plugin Publishing Documentation](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html)
- Check [GitHub Actions Logs](https://github.com/spelens-gud/pprofview-intellij-plugin/actions)
- Seek help in project Issues
