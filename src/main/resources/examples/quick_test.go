package main

import (
	"fmt"
	"time"
)

// 这是一个快速测试程序，用于验证 pprof 在短时间运行的程序中是否能正常工作
// 程序会运行 5 秒钟，进行一些计算

func main() {
	fmt.Println("快速测试程序启动...")
	fmt.Println("程序将运行 5 秒钟")
	
	// 执行一些计算任务，确保有 CPU 活动
	for i := 0; i < 5; i++ {
		fmt.Printf("第 %d 秒...\n", i+1)
		
		// 执行一些计算
		sum := 0
		for j := 0; j < 10000000; j++ {
			sum += j
		}
		
		time.Sleep(1 * time.Second)
	}
	
	fmt.Println("程序执行完成，等待 pprof 数据保存...")
	// 给 pprof 一些时间来保存数据
	time.Sleep(2 * time.Second)
	
	fmt.Println("程序退出")
}
