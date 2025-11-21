package com.github.spelens.pprofview.runconfig

/**
 * pprof 运行种类
 * 
 * 参考 Go 插件的运行配置类型
 */
enum class PprofRunKind(val displayName: String, val description: String) {
    /**
     * 目录 - 运行指定目录下的 main 包
     */
    DIRECTORY(
        "目录",
        "运行指定目录下的 main 包"
    ),
    
    /**
     * 文件 - 运行指定的 Go 文件
     */
    FILE(
        "文件",
        "运行指定的 Go 文件"
    ),
    
    /**
     * 软件包 - 运行指定的 Go 包
     */
    PACKAGE(
        "软件包",
        "运行指定的 Go 包（如 github.com/user/project）"
    );
    
    companion object {
        fun fromString(value: String?): PprofRunKind {
            return entries.find { it.name == value } ?: FILE
        }
    }
}
