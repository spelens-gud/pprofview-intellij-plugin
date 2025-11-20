package main

import (
	"fmt"
	"log"
	"net/http"
	_ "net/http/pprof" // HTTP 服务模式
	"os"
	"runtime"
	"runtime/pprof"
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
	// CPU 分析
	if cpuProfile := os.Getenv("CPUPROFILE"); cpuProfile != "" {
		f, err := os.Create(cpuProfile)
		if err != nil {
			log.Fatal("无法创建 CPU profile 文件:", err)
		}
		defer f.Close()
		
		if err := pprof.StartCPUProfile(f); err != nil {
			log.Fatal("无法启动 CPU profiling:", err)
		}
		defer pprof.StopCPUProfile()
		
		log.Println("CPU profiling 已启动，输出到:", cpuProfile)
	}

	// 内存分析
	if memProfile := os.Getenv("MEMPROFILE"); memProfile != "" {
		defer func() {
			f, err := os.Create(memProfile)
			if err != nil {
				log.Fatal("无法创建内存 profile 文件:", err)
			}
			defer f.Close()
			
			runtime.GC() // 触发 GC 以获取最新的内存统计
			if err := pprof.WriteHeapProfile(f); err != nil {
				log.Fatal("无法写入内存 profile:", err)
			}
			
			log.Println("内存 profiling 已完成，输出到:", memProfile)
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
