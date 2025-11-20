package main

import (
	"fmt"
	"sync"
	"time"
)

// 这个示例程序演示如何使用 pprofview 插件分析协程
// 使用方法：
// 1. 创建 pprof 运行配置
// 2. 勾选"协程分析"
// 3. 运行此程序
// 4. 查看 pprof Output 窗口中的协程分析结果

func main() {
	fmt.Println("协程分析示例程序启动...")
	
	// 创建一个 WaitGroup 来等待所有 goroutine 完成
	var wg sync.WaitGroup
	
	// 启动 100 个 goroutine 执行计算任务
	for i := 0; i < 100; i++ {
		wg.Add(1)
		go worker(i, &wg)
	}
	
	// 启动一些长期运行的 goroutine
	for i := 0; i < 10; i++ {
		wg.Add(1)
		go longRunningWorker(i, &wg)
	}
	
	fmt.Println("等待所有 goroutine 完成...")
	wg.Wait()
	
	fmt.Println("程序执行完成")
}

// worker 执行短期计算任务
func worker(id int, wg *sync.WaitGroup) {
	defer wg.Done()
	
	// 模拟一些计算工作
	sum := 0
	for i := 0; i < 1000000; i++ {
		sum += i
	}
	
	if id%10 == 0 {
		fmt.Printf("Worker %d 完成，结果: %d\n", id, sum)
	}
}

// longRunningWorker 执行长期任务
func longRunningWorker(id int, wg *sync.WaitGroup) {
	defer wg.Done()
	
	fmt.Printf("长期 Worker %d 启动\n", id)
	
	// 模拟长期运行的任务
	for i := 0; i < 10; i++ {
		time.Sleep(1 * time.Second)
		
		// 执行一些计算
		sum := 0
		for j := 0; j < 1000000; j++ {
			sum += j
		}
	}
	
	fmt.Printf("长期 Worker %d 完成\n", id)
}
