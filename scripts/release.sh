#!/bin/bash

# 自动化发布脚本
# 用法: ./scripts/release.sh <version>
# 示例: ./scripts/release.sh 1.0.1

set -e

VERSION=$1

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# 检查参数
if [ -z "$VERSION" ]; then
    print_error "请提供版本号"
    echo "用法: ./scripts/release.sh <version>"
    echo "示例: ./scripts/release.sh 1.0.1"
    exit 1
fi

# 验证版本号格式
if ! [[ $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.]+)?$ ]]; then
    print_error "版本号格式不正确"
    echo "正确格式: x.y.z 或 x.y.z-alpha.1"
    exit 1
fi

print_info "准备发布版本 $VERSION"

# 检查是否在 main 分支
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "main" ]; then
    print_warning "当前不在 main 分支 (当前: $CURRENT_BRANCH)"
    read -p "是否继续? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_info "已取消"
        exit 0
    fi
fi

# 检查是否有未提交的变更
if ! git diff-index --quiet HEAD --; then
    print_error "存在未提交的变更，请先提交或暂存"
    git status --short
    exit 1
fi

# 拉取最新代码
print_info "拉取最新代码..."
git pull origin main

# 运行测试
print_info "运行测试..."
if ./gradlew test; then
    print_success "测试通过"
else
    print_error "测试失败，请修复后再发布"
    exit 1
fi

# 运行插件验证
print_info "运行插件验证..."
if ./gradlew verifyPlugin; then
    print_success "插件验证通过"
else
    print_error "插件验证失败，请修复后再发布"
    exit 1
fi

# 更新版本号
print_info "更新版本号到 $VERSION..."
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    sed -i '' "s/pluginVersion = .*/pluginVersion = $VERSION/" gradle.properties
else
    # Linux
    sed -i "s/pluginVersion = .*/pluginVersion = $VERSION/" gradle.properties
fi
print_success "版本号已更新"

# 检查 CHANGELOG
print_warning "请确保已更新 CHANGELOG.md"
read -p "是否已更新 CHANGELOG.md? (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    print_info "请先更新 CHANGELOG.md，然后重新运行此脚本"
    # 恢复版本号变更
    git checkout gradle.properties
    exit 0
fi

# 显示变更
print_info "将要提交的变更:"
git diff gradle.properties

# 确认发布
print_warning "即将发布版本 $VERSION"
read -p "确认发布? (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    print_info "已取消发布"
    # 恢复版本号变更
    git checkout gradle.properties
    exit 0
fi

# 提交变更
print_info "提交变更..."
git add gradle.properties CHANGELOG.md
git commit -m "chore: prepare release $VERSION"
print_success "变更已提交"

# 创建 tag
print_info "创建 tag v$VERSION..."
git tag -a "v$VERSION" -m "Release version $VERSION"
print_success "Tag 已创建"

# 推送到远程
print_info "推送到远程仓库..."
git push origin main
git push origin "v$VERSION"
print_success "已推送到远程仓库"

# 完成
echo ""
print_success "发布流程已启动！"
echo ""
print_info "接下来的步骤:"
echo "  1. 访问 GitHub Actions 查看构建进度"
echo "     https://github.com/spelens-gud/pprofview-intellij-plugin/actions"
echo ""
echo "  2. 等待 Draft Release 创建完成"
echo "     https://github.com/spelens-gud/pprofview-intellij-plugin/releases"
echo ""
echo "  3. 检查并发布 Release"
echo ""
echo "  4. 等待插件发布到 JetBrains Marketplace"
echo ""
print_info "预计 10-30 分钟后可以看到 Draft Release"
