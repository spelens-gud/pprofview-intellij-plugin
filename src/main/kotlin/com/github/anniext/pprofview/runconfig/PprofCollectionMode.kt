package com.github.anniext.pprofview.runconfig

/**
 * pprof 数据采集模式
 */
enum class PprofCollectionMode(val displayName: String, val description: String) {
    /**
     * 编译时插桩 - 使用 -race 或 -cover 等编译选项
     */
    COMPILE_TIME_INSTRUMENTATION(
        "编译时插桩",
        "在编译时插入性能分析代码（如 -race, -cover）"
    ),

    /**
     * 运行时采样 - 使用 runtime/pprof 包自动采样
     */
    RUNTIME_SAMPLING(
        "运行时采样",
        "程序运行时自动采样性能数据"
    ),

    /**
     * 手动采集 - 在代码中手动调用 pprof API
     */
    MANUAL_COLLECTION(
        "手动采集",
        "通过代码手动控制性能数据采集"
    ),

    /**
     * HTTP 服务 - 启动 pprof HTTP 服务器
     */
    HTTP_SERVER(
        "HTTP 服务",
        "启动 HTTP 服务器提供实时性能数据访问"
    ),

    companion object {
        fun fromString(value: String?): PprofCollectionMode {
            return entries.find { it.name == value } ?: NONE
        }
    }
}
