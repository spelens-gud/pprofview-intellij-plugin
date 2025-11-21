package com.github.spelens.pprofview.runconfig

import com.github.spelens.pprofview.PprofViewBundle

/**
 * pprof profile type
 */
enum class PprofProfileType(val messageKey: String, val fileName: String) {
    CPU("pprof.type.cpu", "cpu.pprof"),
    HEAP("pprof.type.heap", "heap.pprof"),
    GOROUTINE("pprof.type.goroutine", "goroutine.pprof"),
    THREAD_CREATE("pprof.type.threadCreate", "threadcreate.pprof"),
    BLOCK("pprof.type.block", "block.pprof"),
    MUTEX("pprof.type.mutex", "mutex.pprof"),
    ALLOCS("pprof.type.allocs", "allocs.pprof"),
    TRACE("pprof.type.trace", "trace.out");
    
    val displayName: String
        get() = PprofViewBundle.message(messageKey)

    companion object {
        fun fromString(value: String?): PprofProfileType? {
            return entries.find { it.name == value }
        }
    }
}
