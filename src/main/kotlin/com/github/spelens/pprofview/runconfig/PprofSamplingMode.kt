package com.github.spelens.pprofview.runconfig

import com.github.spelens.pprofview.PprofViewBundle

/**
 * pprof sampling mode
 */
enum class PprofSamplingMode(val messageKey: String) {
    SINGLE("pprof.samplingMode.single"),
    LOOP("pprof.samplingMode.loop");
    
    val displayName: String
        get() = PprofViewBundle.message(messageKey)

    companion object {
        fun fromString(value: String?): PprofSamplingMode {
            return entries.find { it.name == value } ?: SINGLE
        }
    }
}
