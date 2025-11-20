package main

import (
	"testing"
	"time"
)

// 测试模式采集示例
// 使用 go test 运行测试时进行性能分析

// TestIndexHandler 测试索引处理器
// 使用测试模式选项: ^\QTestIndexHandler\E$
func TestIndexHandler(t *testing.T) {
	t.Log("开始测试 IndexHandler...")
	
	// 模拟一些计算密集型操作
	result := 0
	for i := 0; i < 10000000; i++ {
		result += i * i
	}
	
	t.Logf("计算结果: %d", result)
}

// TestDataProcessor 测试数据处理器
// 使用测试模式选项: ^\QTestDataProcessor\E$
func TestDataProcessor(t *testing.T) {
	t.Log("开始测试 DataProcessor...")
	
	// 模拟内存分配
	data := make([][]byte, 1000)
	for i := 0; i < 1000; i++ {
		data[i] = make([]byte, 1024*10) // 每次分配 10KB
	}
	
	t.Logf("分配了 %d 个数据块", len(data))
}

// TestConcurrentWorker 测试并发工作器
// 使用测试模式选项: ^\QTestConcurrentWorker\E$
func TestConcurrentWorker(t *testing.T) {
	t.Log("开始测试 ConcurrentWorker...")
	
	// 模拟并发操作
	done := make(chan bool)
	
	for i := 0; i < 10; i++ {
		go func(id int) {
			// 模拟一些工作
			sum := 0
			for j := 0; j < 1000000; j++ {
				sum += j
			}
			done <- true
		}(i)
	}
	
	// 等待所有 goroutine 完成
	for i := 0; i < 10; i++ {
		<-done
	}
	
	t.Log("所有并发任务完成")
}

// TestSlowOperation 测试慢操作
// 使用测试模式选项: ^\QTestSlowOperation\E$
func TestSlowOperation(t *testing.T) {
	t.Log("开始测试 SlowOperation...")
	
	// 模拟一个耗时操作
	time.Sleep(2 * time.Second)
	
	// 执行一些计算
	result := fibonacci(30)
	
	t.Logf("斐波那契数列第 30 项: %d", result)
}

// fibonacci 计算斐波那契数列
func fibonacci(n int) int {
	if n <= 1 {
		return n
	}
	return fibonacci(n-1) + fibonacci(n-2)
}

// BenchmarkStringConcat 基准测试：字符串拼接
// 使用测试模式选项: ^BenchmarkStringConcat$
func BenchmarkStringConcat(b *testing.B) {
	for i := 0; i < b.N; i++ {
		s := ""
		for j := 0; j < 100; j++ {
			s += "a"
		}
	}
}

// BenchmarkMapAccess 基准测试：Map 访问
// 使用测试模式选项: ^BenchmarkMapAccess$
func BenchmarkMapAccess(b *testing.B) {
	m := make(map[int]int)
	for i := 0; i < 1000; i++ {
		m[i] = i
	}
	
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		_ = m[i%1000]
	}
}

/*
go test 支持的性能分析参数：
┌─────────────────┬──────────────────┬────────────────────────────┐
│ 性能分析类型    │ go test 参数     │ 说明                       │
├─────────────────┼──────────────────┼────────────────────────────┤
│ CPU 分析        │ -cpuprofile      │ ✓ 支持                     │
│ 堆内存分析      │ -memprofile      │ ✓ 支持                     │
│ 阻塞分析        │ -blockprofile    │ ✓ 支持                     │
│ 互斥锁分析      │ -mutexprofile    │ ✓ 支持                     │
│ 协程分析        │ -                │ ✗ 不支持                   │
│ 线程创建分析    │ -                │ ✗ 不支持                   │
│ 内存分配分析    │ -                │ ✗ 不支持                   │
│ 执行追踪        │ -trace           │ ✗ 不支持（需单独使用）     │
└─────────────────┴──────────────────┴────────────────────────────┘

使用说明：

1. 运行单个测试函数：
   测试模式选项: ^\QTestIndexHandler\E$
   这将只运行 TestIndexHandler 测试

2. 运行多个测试函数（使用正则表达式）：
   测试模式选项: ^Test.*Handler$
   这将运行所有以 Test 开头、以 Handler 结尾的测试

3. 运行所有测试：
   测试模式选项留空或使用: .*

4. 运行基准测试：
   测试模式选项: ^Benchmark
   注意：需要在 Go 构建标志中添加 -bench=.

5. 性能分析类型选择（测试模式支持）：
   - CPU 分析：分析测试函数的 CPU 使用情况（-cpuprofile）
   - 堆内存分析：分析测试过程中的内存分配（-memprofile）
   - 阻塞分析：分析 channel 和锁的阻塞情况（-blockprofile）
   - 互斥锁分析：分析锁竞争情况（-mutexprofile）
   
   注意：go test 不支持以下类型：
   - 协程分析、线程创建分析、内存分配分析、执行追踪
   这些类型在测试模式下会被自动禁用

6. 配置示例：
   运行种类: 目录
   目录: 包含此测试文件的目录
   采集模式: 测试时采样
   测试模式选项: ^\QTestIndexHandler\E$
   性能分析类型: 勾选 CPU 分析、堆内存分析
   自动打开结果: 勾选

7. 正则表达式说明：
   - ^\Q...\E$ 表示精确匹配（\Q 和 \E 之间的内容不会被解释为正则表达式）
   - ^Test 表示以 Test 开头
   - Handler$ 表示以 Handler 结尾
   - .* 表示匹配任意字符

执行命令示例：
go test -run=^\QTestIndexHandler\E$ -cpuprofile=cpu.pprof -memprofile=mem.pprof
*/
