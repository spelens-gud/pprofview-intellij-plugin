# GitHub Secrets 配置指南

本文档说明如何配置发布插件所需的 GitHub Secrets。

## 必需的 Secrets

### 1. PUBLISH_TOKEN

JetBrains Marketplace 发布令牌，用于将插件发布到市场。

**获取步骤：**

1. 访问 [JetBrains Marketplace](https://plugins.jetbrains.com/)
2. 使用你的 JetBrains 账号登录
3. 点击右上角的头像 → **Profile**
4. 在左侧菜单中选择 **API Tokens**
5. 点击 **Generate New Token**
6. 输入 Token 名称（例如：`pprofview-plugin-publish`）
7. 选择权限：**Marketplace** → **Plugin Upload**
8. 点击 **Generate**
9. 复制生成的 token（只会显示一次！）

**添加到 GitHub：**

1. 进入 GitHub 仓库
2. 点击 **Settings** → **Secrets and variables** → **Actions**
3. 点击 **New repository secret**
4. Name: `PUBLISH_TOKEN`
5. Secret: 粘贴刚才复制的 token
6. 点击 **Add secret**

---

### 2. CERTIFICATE_CHAIN

插件签名证书链，用于验证插件的真实性。

**生成步骤：**

```bash
# 1. 生成私钥（4096 位 RSA）
openssl genrsa -out private.pem 4096

# 2. 生成证书签名请求（CSR）
openssl req -new -key private.pem -out cert.csr

# 填写证书信息：
# Country Name (2 letter code): CN
# State or Province Name: Beijing
# Locality Name: Beijing
# Organization Name: Your Organization
# Organizational Unit Name: Development
# Common Name: your-name
# Email Address: your-email@example.com

# 3. 生成自签名证书（有效期 10 年）
openssl x509 -req -days 3650 -in cert.csr -signkey private.pem -out cert.pem

# 4. 查看证书内容
cat cert.pem
```

**添加到 GitHub：**

1. 复制 `cert.pem` 文件的完整内容（包括 `-----BEGIN CERTIFICATE-----` 和 `-----END CERTIFICATE-----`）
2. 进入 GitHub 仓库 → **Settings** → **Secrets and variables** → **Actions**
3. 点击 **New repository secret**
4. Name: `CERTIFICATE_CHAIN`
5. Secret: 粘贴证书内容
6. 点击 **Add secret**

---

### 3. PRIVATE_KEY

插件签名私钥。

**添加到 GitHub：**

1. 复制 `private.pem` 文件的完整内容（包括 `-----BEGIN RSA PRIVATE KEY-----` 和 `-----END RSA PRIVATE KEY-----`）
2. 进入 GitHub 仓库 → **Settings** → **Secrets and variables** → **Actions**
3. 点击 **New repository secret**
4. Name: `PRIVATE_KEY`
5. Secret: 粘贴私钥内容
6. 点击 **Add secret**

⚠️ **重要**：私钥必须妥善保管，不要泄露！

---

### 4. PRIVATE_KEY_PASSWORD

私钥密码（如果私钥有密码保护）。

**如果你的私钥没有密码：**

可以设置一个空字符串或跳过此 secret。

**如果你想为私钥设置密码：**

```bash
# 为现有私钥添加密码
openssl rsa -aes256 -in private.pem -out private_encrypted.pem

# 输入并确认密码
```

**添加到 GitHub：**

1. 进入 GitHub 仓库 → **Settings** → **Secrets and variables** → **Actions**
2. 点击 **New repository secret**
3. Name: `PRIVATE_KEY_PASSWORD`
4. Secret: 输入密码
5. 点击 **Add secret**

---

## 可选的 Secrets

### CODECOV_TOKEN

用于上传代码覆盖率报告到 CodeCov。

**获取步骤：**

1. 访问 [CodeCov](https://codecov.io/)
2. 使用 GitHub 账号登录
3. 添加你的仓库
4. 复制 Upload Token

**添加到 GitHub：**

1. 进入 GitHub 仓库 → **Settings** → **Secrets and variables** → **Actions**
2. 点击 **New repository secret**
3. Name: `CODECOV_TOKEN`
4. Secret: 粘贴 token
5. 点击 **Add secret**

---

## 验证配置

配置完成后，可以通过以下方式验证：

### 1. 检查 Secrets 列表

在 **Settings** → **Secrets and variables** → **Actions** 中，应该看到：

- ✅ PUBLISH_TOKEN
- ✅ CERTIFICATE_CHAIN
- ✅ PRIVATE_KEY
- ✅ PRIVATE_KEY_PASSWORD
- ✅ CODECOV_TOKEN（可选）

### 2. 触发构建

推送代码到 main 分支，GitHub Actions 会自动运行构建流程。

### 3. 测试发布流程

创建一个测试 tag：

```bash
git tag -a v0.0.1-test -m "Test release"
git push origin v0.0.1-test
```

检查 GitHub Actions 日志，确保没有认证错误。

---

## 安全建议

1. **定期轮换 Token**
   - 建议每 6-12 个月更新一次 PUBLISH_TOKEN
   - 如果怀疑 token 泄露，立即撤销并重新生成

2. **保护私钥**
   - 不要将私钥提交到代码仓库
   - 不要在公开场合分享私钥
   - 建议使用密码保护私钥

3. **限制访问权限**
   - 只给必要的人员提供仓库的 Settings 访问权限
   - 使用 GitHub 的分支保护规则

4. **备份证书和私钥**
   - 将证书和私钥安全地备份到多个位置
   - 如果丢失，需要重新生成并更新所有配置

---

## 故障排查

### 发布失败：Authentication failed

**原因**：PUBLISH_TOKEN 无效或过期

**解决方案**：
1. 检查 token 是否正确复制
2. 在 JetBrains Marketplace 中重新生成 token
3. 更新 GitHub Secret

### 签名失败：Invalid certificate

**原因**：证书格式不正确或私钥不匹配

**解决方案**：
1. 确保证书和私钥是配对的
2. 检查证书内容是否完整（包括开始和结束标记）
3. 重新生成证书和私钥

### 构建失败：Secret not found

**原因**：Secret 名称不正确或未配置

**解决方案**：
1. 检查 Secret 名称是否与工作流中的名称完全一致（区分大小写）
2. 确保所有必需的 Secrets 都已配置

---

## 参考资料

- [JetBrains Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html)
- [GitHub Encrypted Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [OpenSSL Documentation](https://www.openssl.org/docs/)
