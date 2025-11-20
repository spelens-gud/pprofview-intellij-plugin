package main

import (
	"log"
	"time"
)

// 运行时采样示例
// 
// 使用 pprofview 插件的运行时采样功能时，你的代码不需要做任何修改！
// 插件会自动注入 pprof 初始化代码。
//
// 只需：
// 1. 创建一个 pprof 运行配置
// 2. 选择"运行时采样"模式
// 3. 选择要采集的性能数据类型（CPU、Heap 等）
// 4. 运行你的程序
//
// pprof 文件会自动生成在指定的输出目录中。

func main() {
	// 你的程序逻辑
	log.Println("程序开始执行...")
	doWork()
	log.Println("程序执行完成")
}

// initPprof 初始化 pprof 性能分析
// 读取 pprofview 插件设置的环境变量并启用相应的分析
func initPprof() {
	outputDir := os.Getenv("PPROF_OUTPUT_DIR")
	if outputDir == "" {
		// 如果没有设置输出目录，则不启用 pprof
		return
	}
	
	log.Printf("[pprof] 输出目录: %s", outputDir)
	
	// 设置采样率
	if memRateStr := os.Getenv("PPROF_MEM_RATE"); memRateStr != "" {
		if memRate, err := strconv.Atoi(memRateStr); err == nil && memRate > 0 {
			runtime.MemProfileRate = memRate
			log.Printf("[pprof] 内存采样率: %d", memRate)
		}
	}
	
	if blockRateStr := os.Getenv("PPROF_BLOCK_RATE"); blockRateStr != "" {
		if blockRate, err := strconv.Atoi(blockRateStr); err == nil && blockRate > 0 {
			runtime.SetBlockProfileRate(blockRate)
			log.Printf("[pprof] 阻塞采样率: %d", blockRate)
		}
	}
	
	if mutexFractionStr := os.Getenv("PPROF_MUTEX_FRACTION"); mutexFractionStr != "" {
		if mutexFraction, err := strconv.Atoi(mutexFractionStr); err == nil && mutexFraction > 0 {
			runtime.SetMutexProfileFraction(mutexFraction)
			log.Printf("[pprof] 互斥锁采样率: %d", mutexFraction)
		}
	}
	
	// CPU 分析
	if os.Getenv("PPROF_ENABLE_CPU") == "true" {
		startCPUProfiling(outputDir)
	}
	
	// 注册程序退出时的清理函数
	// 注意：这里使用 defer 在 main 函数返回时执行
	// 对于更复杂的程序，可能需要使用 signal 处理
	defer func() {
		writePprofProfiles(outputDir)
	}()
}

// startCPUProfiling 启动 CPU 分析
func startCPUProfiling(outputDir string) {
	cpuFile := filepath.Join(outputDir, "cpu.pprof")
	f, err := os.Create(cpuFile)
	if err != nil {
		log.Printf("[pprof] 无法创建 CPU profile 文件: %v", err)
		return
	}
	
	if err := pprof.StartCPUProfile(f); err != nil {
		f.Close()
		return
	}
	
	// 获取持续时间
	duration := 30
	if durationStr := os.Getenv("PPROF_CPU_DURATION"); durationStr != "" {
		if d, err := strconv.Atoi(durationStr); err == nil {
			duration = d
		}
	}
	
	// 定时停止
	go func() {
		time.Sleep(time.Duration(duration) * time.Second)
		pprof.StopCPUProfile()
		f.Close()
		log.Printf("[pprof] CPU profiling 已完成: %s", cpuFile)
	}()
}

// writePprofProfiles 写入其他类型的 profile
func writePprofProfiles(outputDir string) {
	// 堆内存分析
	if os.Getenv("PPROF_ENABLE_HEAP") == "true" {
		heapFile := filepath.Join(outputDir, "heap.pprof")
		f, err := os.Create(heapFile)
		if err == nil {
			runtime.GC()
			if err := pprof.WriteHeapProfile(f); err == nil {
				log.Printf("[pprof] 堆内存 profiling 已完成: %s", heapFile)
			}
			f.Close()
		}
	}
	
	// 协程分析
	if os.Getenv("PPROF_ENABLE_GOROUTINE") == "true" {
		goroutineFile := filepath.Join(outputDir, "goroutine.pprof")
		f, err := os.Create(goroutineFile)
		if err == nil {
			if err := pprof.Lookup("goroutine").WriteTo(f, 0); err == nil {
				log.Printf("[pprof] 协程 profiling 已完成: %s", goroutineFile)
			}
			f.Close()
		}
	}
	
	// 阻塞分析
	if os.Getenv("PPROF_ENABLE_BLOCK") == "true" {
		blockFile := filepath.Join(outputDir, "block.pprof")
		f, err := os.Create(blockFile)
		if err == nil {
			if err := pprof.Lookup("block").WriteTo(f, 0); err == nil {
				log.Printf("[pprof] 阻塞 profiling 已完成: %s", blockFile)
			}
			f.Close()
		}
	}
	
	// 互斥锁分析
	if os.Getenv("PPROF_ENABLE_MUTEX") == "true" {
		mutexFile := filepath.Join(outputDir, "mutex.pprof")
		f, err := os.Create(mutexFile)
		if err == nil {
			if err := pprof.Lookup("mutex").WriteTo(f, 0); err == nil {
				log.Printf("[pprof] 互斥锁 profiling 已完成: %s", mutexFile)
			}
			f.Close()
		}
	}
	
	// 内存分配分析
	if os.Getenv("PPROF_ENABLE_ALLOCS") == "true" {
		allocsFile := filepath.Join(outputDir, "allocs.pprof")
		f, err := os.Create(allocsFile)
		if err == nil {
			runtime.GC()
			if err := pprof.Lookup("allocs").WriteTo(f, 0); err == nil {
				log.Printf("[pprof] 内存分配 profiling 已完成: %s", allocsFile)
			}
			f.Close()
		}
	}
}

// doWork 模拟一些工作负载
func doWork() {
	// CPU 密集型操作
	for i := 0; i < 1000000; i++ {
		_ = fibonacci(20)
	}
	
	// 内存分配
	data := make([][]byte, 100)
	for i := range data {
		data[i] = make([]byte, 1024*1024) // 1MB
	}
	
	// 并发操作
	done := make(chan bool)
	for i := 0; i < 10; i++ {
		go func() {
			time.Sleep(100 * time.Millisecond)
			done <- true
		}()
	}
	for i := 0; i < 10; i++ {
		<-done
	}
}

func fibonacci(n int) int {
	if n <= 1 {
		return n
	}
	return fibonacci(n-1) + fibonacci(n-2)
}
