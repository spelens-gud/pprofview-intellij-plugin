package main

import (
	"fmt"
	"log"
	"net/http"
	_ "net/http/pprof" // HTTP 服务模式
	"os"
	"runtime"
	"runtime/pprof"
	"strconv"
	"time"
)

func main() {
	// 示例 1: HTTP 服务模式
	// 在运行配置中选择 "HTTP 服务" 模式，程序会自动启动 pprof HTTP 服务器
	// 访问 http://localhost:6060/debug/pprof/ 查看性能数据
	go func() {
		port := os.Getenv("PPROF_HTTP_PORT")
		if port == "" {
			port = "6060"
		}
		log.Println("pprof HTTP 服务器启动在端口:", port)
		log.Println(http.ListenAndServe(":"+port, nil))
	}()

	// 示例 2: 运行时采样模式
	// 在运行配置中选择 "运行时采样" 模式，环境变量会自动设置
	// 插件会设置以下环境变量：
	// - PPROF_OUTPUT_DIR: 输出目录
	// - PPROF_ENABLE_CPU: 是否启用 CPU 分析
	// - PPROF_ENABLE_HEAP: 是否启用堆内存分析
	// - PPROF_CPU_DURATION: CPU 采样持续时间（秒）
	// - PPROF_MEM_RATE: 内存采样率
	// - PPROF_BLOCK_RATE: 阻塞采样率
	// - PPROF_MUTEX_FRACTION: 互斥锁采样率
	
	outputDir := os.Getenv("PPROF_OUTPUT_DIR")
	if outputDir == "" {
		outputDir = "." // 默认当前目录
	}
	
	// CPU 分析
	if os.Getenv("PPROF_ENABLE_CPU") == "true" {
		cpuFile := outputDir + "/cpu.pprof"
		f, err := os.Create(cpuFile)
		if err != nil {
			log.Fatal("无法创建 CPU profile 文件:", err)
		}
		defer f.Close()
		
		if err := pprof.StartCPUProfile(f); err != nil {
			log.Fatal("无法启动 CPU profiling:", err)
		}
		defer pprof.StopCPUProfile()
		
		// 根据配置的持续时间自动停止
		if durationStr := os.Getenv("PPROF_CPU_DURATION"); durationStr != "" {
			if duration, err := time.ParseDuration(durationStr + "s"); err == nil {
				go func() {
					time.Sleep(duration)
					pprof.StopCPUProfile()
					f.Close()
					log.Println("CPU profiling 已完成")
				}()
			}
		}
	}

	// 堆内存分析
	if os.Getenv("PPROF_ENABLE_HEAP") == "true" {
		// 设置内存采样率
		if memRateStr := os.Getenv("PPROF_MEM_RATE"); memRateStr != "" {
			if memRate, err := strconv.Atoi(memRateStr); err == nil {
				runtime.MemProfileRate = memRate
				log.Printf("内存采样率设置为: %d", memRate)
			}
		}
		
		defer func() {
			heapFile := outputDir + "/heap.pprof"
			f, err := os.Create(heapFile)
			if err != nil {
				log.Fatal("无法创建堆内存 profile 文件:", err)
			}
			defer f.Close()
			
			runtime.GC() // 触发 GC 以获取最新的内存统计
			if err := pprof.WriteHeapProfile(f); err != nil {
				log.Fatal("无法写入堆内存 profile:", err)
			}
			
			log.Println("堆内存 profiling 已完成，输出到:", heapFile)
		}()
	}
	
	// 协程分析
	if os.Getenv("PPROF_ENABLE_GOROUTINE") == "true" {
		defer func() {
			goroutineFile := outputDir + "/goroutine.pprof"
			f, err := os.Create(goroutineFile)
			if err != nil {
				log.Fatal("无法创建协程 profile 文件:", err)
			}
			defer f.Close()
			
			if err := pprof.Lookup("goroutine").WriteTo(f, 0); err != nil {
				log.Fatal("无法写入协程 profile:", err)
			}
			
			log.Println("协程 profiling 已完成，输出到:", goroutineFile)
		}()
	}
	
	// 阻塞分析
	if os.Getenv("PPROF_ENABLE_BLOCK") == "true" {
		if blockRateStr := os.Getenv("PPROF_BLOCK_RATE"); blockRateStr != "" {
			if blockRate, err := strconv.Atoi(blockRateStr); err == nil {
				runtime.SetBlockProfileRate(blockRate)
				log.Printf("阻塞采样率设置为: %d", blockRate)
			}
		}
		
		defer func() {
			blockFile := outputDir + "/block.pprof"
			f, err := os.Create(blockFile)
			if err != nil {
				log.Fatal("无法创建阻塞 profile 文件:", err)
			}
			defer f.Close()
			
			if err := pprof.Lookup("block").WriteTo(f, 0); err != nil {
				log.Fatal("无法写入阻塞 profile:", err)
			}
			
			log.Println("阻塞 profiling 已完成，输出到:", blockFile)
		}()
	}
	
	// 互斥锁分析
	if os.Getenv("PPROF_ENABLE_MUTEX") == "true" {
		if mutexFractionStr := os.Getenv("PPROF_MUTEX_FRACTION"); mutexFractionStr != "" {
			if mutexFraction, err := strconv.Atoi(mutexFractionStr); err == nil {
				runtime.SetMutexProfileFraction(mutexFraction)
				log.Printf("互斥锁采样率设置为: %d", mutexFraction)
			}
		}
		
		defer func() {
			mutexFile := outputDir + "/mutex.pprof"
			f, err := os.Create(mutexFile)
			if err != nil {
				log.Fatal("无法创建互斥锁 profile 文件:", err)
			}
			defer f.Close()
			
			if err := pprof.Lookup("mutex").WriteTo(f, 0); err != nil {
				log.Fatal("无法写入互斥锁 profile:", err)
			}
			
			log.Println("互斥锁 profiling 已完成，输出到:", mutexFile)
		}()
	}
	
	// 内存分配分析
	if os.Getenv("PPROF_ENABLE_ALLOCS") == "true" {
		defer func() {
			allocsFile := outputDir + "/allocs.pprof"
			f, err := os.Create(allocsFile)
			if err != nil {
				log.Fatal("无法创建内存分配 profile 文件:", err)
			}
			defer f.Close()
			
			runtime.GC()
			if err := pprof.Lookup("allocs").WriteTo(f, 0); err != nil {
				log.Fatal("无法写入内存分配 profile:", err)
			}
			
			log.Println("内存分配 profiling 已完成，输出到:", allocsFile)
		}()
	}

	// 示例 3: 手动采集模式
	// 在运行配置中选择 "手动采集" 模式，在代码中手动控制采集
	manualProfiling := false
	if manualProfiling {
		// CPU 分析
		cpuFile, _ := os.Create("manual_cpu.pprof")
		pprof.StartCPUProfile(cpuFile)
		defer func() {
			pprof.StopCPUProfile()
			cpuFile.Close()
		}()

		// 在需要分析的代码段前后手动控制
		// ... 你的代码 ...

		// 堆内存快照
		heapFile, _ := os.Create("manual_heap.pprof")
		pprof.WriteHeapProfile(heapFile)
		heapFile.Close()

		// 协程快照
		goroutineFile, _ := os.Create("manual_goroutine.pprof")
		pprof.Lookup("goroutine").WriteTo(goroutineFile, 0)
		goroutineFile.Close()

		// 阻塞分析
		runtime.SetBlockProfileRate(1)
		blockFile, _ := os.Create("manual_block.pprof")
		pprof.Lookup("block").WriteTo(blockFile, 0)
		blockFile.Close()

		// 互斥锁分析
		runtime.SetMutexProfileFraction(1)
		mutexFile, _ := os.Create("manual_mutex.pprof")
		pprof.Lookup("mutex").WriteTo(mutexFile, 0)
		mutexFile.Close()
	}

	// 示例 4: 编译时插桩模式
	// 在运行配置中选择 "编译时插桩" 模式，并在自定义编译参数中添加:
	// -race (竞态检测)
	// -cover (代码覆盖率)
	// -gcflags="-m" (逃逸分析)

	// 模拟一些工作负载
	fmt.Println("开始执行工作负载...")
	doWork()
	fmt.Println("工作负载执行完成")

	// 保持程序运行一段时间以便采集数据
	time.Sleep(5 * time.Second)
}

func doWork() {
	// 模拟 CPU 密集型操作
	for i := 0; i < 1000000; i++ {
		_ = fibonacci(20)
	}

	// 模拟内存分配
	data := make([][]byte, 100)
	for i := range data {
		data[i] = make([]byte, 1024*1024) // 1MB
	}

	// 模拟并发操作
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
