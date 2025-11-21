package com.github.spelens.pprofview.runconfig

/**
 * pprof 数据采集模式
 */
enum class PprofCollectionMode(val displayName: String, val description: String) {
    /**
     * 运行时采样 - 使用 runtime/pprof 包自动采样
     */
    RUNTIME_SAMPLING(
        "运行时采样",
        "程序运行时自动采样性能数据"
    ),

    /**
     * HTTP 服务 - 启动 pprof HTTP 服务器
     */
    HTTP_SERVER(
        "HTTP 服务",
        "启动 HTTP 服务器提供实时性能数据访问"
    ),

    /**
     * 测试时采样 - 在 go test 时采样
     */
    TEST_SAMPLING(
        "测试时采样",
        "运行 go test 时自动采样性能数据"
    );

    companion object {
        fun fromString(value: String?): PprofCollectionMode {
            return entries.find { it.name == value } ?: RUNTIME_SAMPLING
        }
    }
}
