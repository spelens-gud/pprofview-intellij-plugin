# GitHub Secrets Setup Guide

English | [简体中文](SECRETS_SETUP_ZH.md)

This document explains how to configure GitHub Secrets required for publishing the plugin.

## Required Secrets

### 1. PUBLISH_TOKEN

JetBrains Marketplace publishing token for publishing the plugin to the marketplace.

**Steps to Obtain:**

1. Visit [JetBrains Marketplace](https://plugins.jetbrains.com/)
2. Login with your JetBrains account
3. Click your avatar in top right → **Profile**
4. Select **API Tokens** in left menu
5. Click **Generate New Token**
6. Enter token name (e.g., `pprofview-plugin-publish`)
7. Select permissions: **Marketplace** → **Plugin Upload**
8. Click **Generate**
9. Copy the generated token (only shown once!)

**Add to GitHub:**

1. Go to GitHub repository
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Name: `PUBLISH_TOKEN`
5. Secret: Paste the copied token
6. Click **Add secret**

---

### 2. CERTIFICATE_CHAIN

Plugin signing certificate chain for verifying plugin authenticity.

**Generation Steps:**

```bash
# 1. Generate private key (4096-bit RSA)
openssl genrsa -out private.pem 4096

# 2. Generate Certificate Signing Request (CSR)
openssl req -new -key private.pem -out cert.csr

# Fill in certificate information:
# Country Name (2 letter code): CN
# State or Province Name: Beijing
# Locality Name: Beijing
# Organization Name: Your Organization
# Organizational Unit Name: Development
# Common Name: your-name
# Email Address: your-email@example.com

# 3. Generate self-signed certificate (valid for 10 years)
openssl x509 -req -days 3650 -in cert.csr -signkey private.pem -out cert.pem

# 4. View certificate content
cat cert.pem
```

**Add to GitHub:**

1. Copy complete content of `cert.pem` file (including `-----BEGIN CERTIFICATE-----` and `-----END CERTIFICATE-----`)
2. Go to GitHub repository → **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Name: `CERTIFICATE_CHAIN`
5. Secret: Paste certificate content
6. Click **Add secret**

---

### 3. PRIVATE_KEY

Plugin signing private key.

**Add to GitHub:**

1. Copy complete content of `private.pem` file (including `-----BEGIN RSA PRIVATE KEY-----` and `-----END RSA PRIVATE KEY-----`)
2. Go to GitHub repository → **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Name: `PRIVATE_KEY`
5. Secret: Paste private key content
6. Click **Add secret**

⚠️ **Important**: Keep private key secure and never expose it!

---

### 4. PRIVATE_KEY_PASSWORD

Private key password (if private key is password-protected).

**If your private key has no password:**

You can set an empty string or skip this secret.

**If you want to add password to private key:**

```bash
# Add password to existing private key
openssl rsa -aes256 -in private.pem -out private_encrypted.pem

# Enter and confirm password
```

**Add to GitHub:**

1. Go to GitHub repository → **Settings** → **Secrets and variables** → **Actions**
2. Click **New repository secret**
3. Name: `PRIVATE_KEY_PASSWORD`
4. Secret: Enter password
5. Click **Add secret**

---

## Optional Secrets

### CODECOV_TOKEN

For uploading code coverage reports to CodeCov.

**Steps to Obtain:**

1. Visit [CodeCov](https://codecov.io/)
2. Login with GitHub account
3. Add your repository
4. Copy Upload Token

**Add to GitHub:**

1. Go to GitHub repository → **Settings** → **Secrets and variables** → **Actions**
2. Click **New repository secret**
3. Name: `CODECOV_TOKEN`
4. Secret: Paste token
5. Click **Add secret**

---

## Verify Configuration

After configuration, verify with:

### 1. Check Secrets List

In **Settings** → **Secrets and variables** → **Actions**, you should see:

- ✅ PUBLISH_TOKEN
- ✅ CERTIFICATE_CHAIN
- ✅ PRIVATE_KEY
- ✅ PRIVATE_KEY_PASSWORD
- ✅ CODECOV_TOKEN (optional)

### 2. Trigger Build

Push code to main branch, GitHub Actions will automatically run build process.

### 3. Test Release Process

Create a test tag:

```bash
git tag -a v0.0.1-test -m "Test release"
git push origin v0.0.1-test
```

Check GitHub Actions logs for authentication errors.

---

## Security Recommendations

1. **Rotate Tokens Regularly**
   - Recommend updating PUBLISH_TOKEN every 6-12 months
   - If token leak is suspected, revoke and regenerate immediately

2. **Protect Private Key**
   - Never commit private key to code repository
   - Never share private key publicly
   - Recommend password-protecting private key

3. **Limit Access Permissions**
   - Only give necessary personnel Settings access to repository
   - Use GitHub branch protection rules

4. **Backup Certificate and Private Key**
   - Securely backup certificate and private key to multiple locations
   - If lost, need to regenerate and update all configurations

---

## Troubleshooting

### Release failed: Authentication failed

**Cause**: PUBLISH_TOKEN is invalid or expired

**Solution**:
1. Check if token was copied correctly
2. Regenerate token in JetBrains Marketplace
3. Update GitHub Secret

### Signing failed: Invalid certificate

**Cause**: Certificate format incorrect or private key doesn't match

**Solution**:
1. Ensure certificate and private key are paired
2. Check certificate content is complete (including begin and end markers)
3. Regenerate certificate and private key

### Build failed: Secret not found

**Cause**: Secret name incorrect or not configured

**Solution**:
1. Check Secret name exactly matches workflow name (case-sensitive)
2. Ensure all required Secrets are configured

---

## References

- [JetBrains Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html)
- [GitHub Encrypted Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [OpenSSL Documentation](https://www.openssl.org/docs/)
