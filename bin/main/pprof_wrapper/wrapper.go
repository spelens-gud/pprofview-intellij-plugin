package main

import (
	"log"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"runtime/pprof"
	"strconv"
	"time"
)

// pprof wrapper - 自动为 Go 程序添加性能分析
// 由 pprofview 插件自动生成和使用

func main() {
	// 初始化 pprof
	cleanup := initPprof()
	defer cleanup()
	
	// 执行原始程序
	if len(os.Args) < 2 {
		log.Fatal("用法: wrapper <program> [args...]")
	}
	
	cmd := exec.Command(os.Args[1], os.Args[2:]...)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	cmd.Stdin = os.Stdin
	
	if err := cmd.Run(); err != nil {
		log.Printf("程序执行失败: %v", err)
		os.Exit(1)
	}
}

// initPprof 初始化 pprof 性能分析
func initPprof() func() {
	outputDir := os.Getenv("PPROF_OUTPUT_DIR")
	if outputDir == "" {
		return func() {}
	}
	
	log.Printf("[pprofview] 输出目录: %s", outputDir)
	
	// 设置采样率
	if memRateStr := os.Getenv("PPROF_MEM_RATE"); memRateStr != "" {
		if memRate, err := strconv.Atoi(memRateStr); err == nil && memRate > 0 {
			runtime.MemProfileRate = memRate
			log.Printf("[pprofview] 内存采样率: %d", memRate)
		}
	}
	
	if blockRateStr := os.Getenv("PPROF_BLOCK_RATE"); blockRateStr != "" {
		if blockRate, err := strconv.Atoi(blockRateStr); err == nil && blockRate > 0 {
			runtime.SetBlockProfileRate(blockRate)
			log.Printf("[pprofview] 阻塞采样率: %d", blockRate)
		}
	}
	
	if mutexFractionStr := os.Getenv("PPROF_MUTEX_FRACTION"); mutexFractionStr != "" {
		if mutexFraction, err := strconv.Atoi(mutexFractionStr); err == nil && mutexFraction > 0 {
			runtime.SetMutexProfileFraction(mutexFraction)
			log.Printf("[pprofview] 互斥锁采样率: %d", mutexFraction)
		}
	}
	
	// CPU 分析
	var cpuFile *os.File
	if os.Getenv("PPROF_ENABLE_CPU") == "true" {
		cpuFile = startCPUProfiling(outputDir)
	}
	
	// 返回清理函数
	return func() {
		if cpuFile != nil {
			pprof.StopCPUProfile()
			cpuFile.Close()
			log.Printf("[pprofview] CPU profiling 已完成")
		}
		writePprofProfiles(outputDir)
	}
}

// startCPUProfiling 启动 CPU 分析
func startCPUProfiling(outputDir string) *os.File {
	cpuFile := filepath.Join(outputDir, "cpu.pprof")
	f, err := os.Create(cpuFile)
	if err != nil {
		log.Printf("[pprofview] 无法创建 CPU profile 文件: %v", err)
		return nil
	}
	
	if err := pprof.StartCPUProfile(f); err != nil {
		f.Close()
		return nil
	}
	
	return f
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
				log.Printf("[pprofview] 堆内存 profiling 已完成: %s", heapFile)
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
				log.Printf("[pprofview] 协程 profiling 已完成: %s", goroutineFile)
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
				log.Printf("[pprofview] 阻塞 profiling 已完成: %s", blockFile)
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
				log.Printf("[pprofview] 互斥锁 profiling 已完成: %s", mutexFile)
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
				log.Printf("[pprofview] 内存分配 profiling 已完成: %s", allocsFile)
			}
			f.Close()
		}
	}
}
