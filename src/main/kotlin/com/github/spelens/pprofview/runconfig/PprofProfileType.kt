package com.github.spelens.pprofview.runconfig

/**
 * pprof 性能分析类型
 */
enum class PprofProfileType(val displayName: String, val fileName: String) {
    CPU("CPU 分析", "cpu.pprof"),
    HEAP("堆内存分析", "heap.pprof"),
    GOROUTINE("协程分析", "goroutine.pprof"),
    THREAD_CREATE("线程创建分析", "threadcreate.pprof"),
    BLOCK("阻塞分析", "block.pprof"),
    MUTEX("互斥锁分析", "mutex.pprof"),
    ALLOCS("内存分配分析", "allocs.pprof"),
    TRACE("执行追踪", "trace.out");

    companion object {
        fun fromString(value: String?): PprofProfileType? {
            return entries.find { it.name == value }
        }
    }
}
