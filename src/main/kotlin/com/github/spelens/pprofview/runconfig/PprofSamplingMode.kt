package com.github.spelens.pprofview.runconfig

/**
 * pprof 采样模式
 */
enum class PprofSamplingMode(val displayName: String) {
    SINGLE("单次采样"),
    LOOP("循环采样");

    companion object {
        fun fromString(value: String?): PprofSamplingMode {
            return entries.find { it.name == value } ?: SINGLE
        }
    }
}
