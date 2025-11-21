#!/bin/bash

# 测试函数名模式匹配
# 用于验证不同的函数名格式是否能被 pprof -list 识别

PPROF_FILE="${1:-cpu.pprof}"
FUNCTION_NAME="${2:-github.com/wolfogre/go-pprof-practice/animal/canidae/wolf.(*Wolf).Drink.func1}"

echo "========================================="
echo "测试 pprof -list 函数名匹配"
echo "========================================="
echo "pprof 文件: $PPROF_FILE"
echo "函数名: $FUNCTION_NAME"
echo ""

if [ ! -f "$PPROF_FILE" ]; then
    echo "错误: pprof 文件不存在: $PPROF_FILE"
    exit 1
fi

# 提取函数名的各个部分
LAST_PART=$(echo "$FUNCTION_NAME" | awk -F'.' '{print $NF}')
PKG_AND_FUNC=$(echo "$FUNCTION_NAME" | awk -F'/' '{print $NF}')

echo "提取的部分:"
echo "  - 最后一部分: $LAST_PART"
echo "  - 包名和函数: $PKG_AND_FUNC"
echo ""

# 测试不同的模式
patterns=(
    "$FUNCTION_NAME"
    "$LAST_PART"
    "$PKG_AND_FUNC"
    "Drink"
    "Wolf"
    "wolf"
)

for i in "${!patterns[@]}"; do
    pattern="${patterns[$i]}"
    echo "----------------------------------------"
    echo "模式 $((i+1)): $pattern"
    echo "----------------------------------------"
    
    output=$(go tool pprof -list="$pattern" "$PPROF_FILE" 2>&1)
    exit_code=$?
    
    if [ $exit_code -eq 0 ] && [ -n "$output" ] && ! echo "$output" | grep -q "no matches"; then
        echo "✓ 成功"
        echo "$output" | head -20
        echo ""
        echo "找到匹配！使用模式: $pattern"
        exit 0
    else
        echo "✗ 失败 (退出码: $exit_code)"
        if [ -n "$output" ]; then
            echo "$output" | head -5
        fi
    fi
    echo ""
done

echo "========================================="
echo "所有模式都失败了"
echo "========================================="
echo ""
echo "建议:"
echo "1. 检查函数名是否正确"
echo "2. 使用 'go tool pprof -top $PPROF_FILE' 查看所有函数"
echo "3. 尝试使用正则表达式: 'go tool pprof -list=Drink $PPROF_FILE'"
exit 1
